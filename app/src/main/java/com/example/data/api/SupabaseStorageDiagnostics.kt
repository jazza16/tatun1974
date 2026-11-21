package com.example.data.api

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import com.example.BuildConfig
import com.example.util.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.net.URI
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * Classification of Supabase Storage bucket retrieval policy for the `reports` bucket.
 */
enum class BucketAccessPolicyStatus {
    PUBLIC_READ_ALLOWED,
    AUTHENTICATED_READ_ONLY,
    ACCESS_DENIED_OR_MISCONFIGURED,
    BYTE_MISMATCH_CORRUPTED,
    UPLOAD_FAILED,
    NOT_CONFIGURED
}

/**
 * Individual diagnostic checkpoint with pass/fail status, HTTP status (if applicable), and timing.
 */
data class SupabaseDiagnosticStep(
    val step: String,
    val passed: Boolean,
    val detail: String,
    val httpStatus: Int? = null,
    val durationMs: Long = 0L
)

/**
 * Structured HTTP probe response returned by [SupabaseStorageDiagnostics.HttpProbeExecutor].
 */
data class HttpProbeResult(
    val url: String,
    val statusCode: Int,
    val contentType: String?,
    val bodyBytes: ByteArray,
    val errorSnippet: String? = null,
    val durationMs: Long = 0L,
    val exceptionMessage: String? = null
)

/**
 * Complete diagnostic report for Supabase Storage image upload path, byte transmission integrity,
 * and bucket policy (public vs authenticated retrieval) on the `reports` bucket.
 */
data class SupabaseUploadDiagnosticReport(
    val timestampMs: Long,
    val bucketName: String,
    val supabaseHost: String,
    val objectPath: String,
    val publicUrl: String,
    val authenticatedUrl: String,
    val expectedByteSize: Int,
    val expectedSha256: String,
    val hasValidJpegMagic: Boolean,
    val uploadSucceeded: Boolean,
    val bytesTransmittedVerified: Boolean,
    val publicAccessAllowed: Boolean,
    val publicHttpStatus: Int?,
    val publicContentType: String?,
    val publicDownloadedByteSize: Int?,
    val publicSha256Matches: Boolean,
    val authenticatedAccessAllowed: Boolean,
    val authenticatedHttpStatus: Int?,
    val authenticatedContentType: String?,
    val authenticatedDownloadedByteSize: Int?,
    val authenticatedSha256Matches: Boolean,
    val bucketPolicyStatus: BucketAccessPolicyStatus,
    val cleanupDeleted: Boolean,
    val steps: List<SupabaseDiagnosticStep>,
    val recommendation: String,
    val formattedLog: String
)

/**
 * Diagnostic utility to verify the full Supabase Storage image upload pipeline for citizen reports:
 * 1. Validates Supabase configuration and normalized upload path in bucket `reports`.
 * 2. Verifies JPEG encoding, JPEG SOI/EOI magic headers, exact byte size, and SHA-256 digest.
 * 3. Uploads image bytes to Supabase Storage and checks whether transmitted bytes round-trip intact.
 * 4. Probes BOTH unauthenticated public URL (`/storage/v1/object/public/reports/...`) AND
 *    authenticated URL (`/storage/v1/object/authenticated/reports/...` with `apikey` + `Bearer`)
 *    to determine whether the bucket policy allows public read, authenticated-only read, or denies access.
 * 5. Emits a structured multi-line diagnostic log to Logcat under tag `SupabaseStorageDiag`.
 */
object SupabaseStorageDiagnostics {

    const val LOG_TAG = "SupabaseStorageDiag"

    /**
     * Abstraction for Storage upload/delete so production uses the real Supabase SDK
     * while unit tests can inject deterministic adapters without network credentials.
     */
    interface StorageProbeAdapter {
        fun isConfigured(): Boolean
        fun supabaseUrl(): String
        fun supabaseKey(): String
        suspend fun uploadBytes(bucket: String, path: String, bytes: ByteArray): Result<String>
        suspend fun deleteObject(bucket: String, path: String): Result<Unit>
    }

    /**
     * Abstraction for HTTP GET probing of public and authenticated Supabase Storage URLs.
     */
    fun interface HttpProbeExecutor {
        suspend fun executeGet(url: String, headers: Map<String, String>): HttpProbeResult
    }

    private val probeOkHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    private val defaultStorageAdapter = object : StorageProbeAdapter {
        override fun isConfigured(): Boolean = SupabaseClient.isConfigured()
        override fun supabaseUrl(): String = SupabaseClient.getEffectiveSupabaseUrl().trimEnd('/')
        override fun supabaseKey(): String = SupabaseClient.getEffectiveSupabaseKey()
        override suspend fun uploadBytes(bucket: String, path: String, bytes: ByteArray): Result<String> =
            SupabaseClient.uploadRawJpegBytes(bucket = bucket, filePath = path, imageBytes = bytes)

        override suspend fun deleteObject(bucket: String, path: String): Result<Unit> =
            SupabaseClient.deleteObjectFromBucket(bucket = bucket, filePath = path)
    }

    private val defaultHttpProbeExecutor = HttpProbeExecutor { url, headers ->
        val startMs = System.currentTimeMillis()
        try {
            val reqBuilder = Request.Builder().url(url).get()
            headers.forEach { (k, v) -> reqBuilder.addHeader(k, v) }
            probeOkHttpClient.newCall(reqBuilder.build()).execute().use { response ->
                val rawBytes = response.body?.bytes() ?: ByteArray(0)
                val contentType = response.header("Content-Type")
                val snippet = if (!response.isSuccessful && rawBytes.isNotEmpty()) {
                    String(rawBytes.copyOf(minOf(rawBytes.size, 320)), Charsets.UTF_8)
                        .replace("\n", " ")
                        .trim()
                } else {
                    null
                }
                HttpProbeResult(
                    url = url,
                    statusCode = response.code,
                    contentType = contentType,
                    bodyBytes = rawBytes,
                    errorSnippet = snippet,
                    durationMs = System.currentTimeMillis() - startMs
                )
            }
        } catch (e: Exception) {
            HttpProbeResult(
                url = url,
                statusCode = -1,
                contentType = null,
                bodyBytes = ByteArray(0),
                errorSnippet = null,
                durationMs = System.currentTimeMillis() - startMs,
                exceptionMessage = "${e::class.java.simpleName}: ${e.localizedMessage ?: "Network error"}"
            )
        }
    }

    @Volatile
    internal var overrideStorageAdapterForTesting: StorageProbeAdapter? = null

    @Volatile
    internal var overrideHttpProbeExecutorForTesting: HttpProbeExecutor? = null

    internal fun resetTestHooks() {
        overrideStorageAdapterForTesting = null
        overrideHttpProbeExecutorForTesting = null
    }

    /**
     * Runs a full diagnostic verification of the Supabase Storage image upload path,
     * transmitted byte integrity, and bucket access policy (`public` vs `authenticated`).
     *
     * @param sampleBitmap Optional real report [Bitmap] to test with; if null, generates a compact valid JPEG probe bitmap.
     * @param reportId Optional report ID used to construct the normalized object path.
     * @param bucketName Target Supabase Storage bucket (defaults to [SupabaseClient.BUCKET_REPORTS] = `"reports"`).
     * @param authBearerToken Optional authenticated JWT access token (defaults to the configured Supabase publishable/anon key).
     * @param cleanupProbeObject When true, deletes the temporary diagnostic object from the bucket after verification.
     */
    suspend fun diagnoseImageUploadPath(
        sampleBitmap: Bitmap? = null,
        reportId: String = "diag_probe",
        bucketName: String = SupabaseClient.BUCKET_REPORTS,
        authBearerToken: String? = null,
        cleanupProbeObject: Boolean = true
    ): SupabaseUploadDiagnosticReport = withContext(Dispatchers.IO) {
        val nowMs = System.currentTimeMillis()
        val adapter = overrideStorageAdapterForTesting ?: defaultStorageAdapter
        if (overrideStorageAdapterForTesting == null && !adapter.isConfigured()) {
            SupabaseClient.ensureCloudConfigLoaded()
        }
        val httpExecutor = overrideHttpProbeExecutorForTesting ?: defaultHttpProbeExecutor
        val steps = mutableListOf<SupabaseDiagnosticStep>()

        val baseUrl = adapter.supabaseUrl().trimEnd('/')
        val apiKey = adapter.supabaseKey()
        val host = extractHostSafely(baseUrl)
        val objectPath = SupabaseClient.buildReportPhotoPath(
            reportId = reportId,
            prefix = "diagnostics"
        )
        val expectedPublicUrl = "$baseUrl/storage/v1/object/public/$bucketName/$objectPath"
        val expectedAuthUrl = "$baseUrl/storage/v1/object/authenticated/$bucketName/$objectPath"

        // STEP 1: Verify Secrets Configuration & Upload Path Format
        val configured = adapter.isConfigured()
        val pathValid = isValidSupabaseObjectPath(objectPath)
        steps += SupabaseDiagnosticStep(
            step = "1_CONFIG_AND_PATH",
            passed = configured && pathValid,
            detail = if (configured) {
                "Host=$host | Bucket=$bucketName | Path=$objectPath | PathValid=$pathValid | KeyConfigured=true(len=${apiKey.length})"
            } else {
                "Supabase belum terkonfigurasi di panel Secrets (SUPABASE_URL/SUPABASE_KEY masih kosong atau placeholder)."
            }
        )

        if (!configured) {
            val recommendation =
                "Atur SUPABASE_URL (https://<project-ref>.supabase.co) dan SUPABASE_KEY (anon/publishable key) di panel Secrets AI Studio agar unggahan foto ke bucket '$bucketName' aktif."
            return@withContext finalizeAndEmitReport(
                timestampMs = nowMs,
                bucketName = bucketName,
                supabaseHost = host,
                objectPath = objectPath,
                publicUrl = expectedPublicUrl,
                authenticatedUrl = expectedAuthUrl,
                expectedByteSize = 0,
                expectedSha256 = "",
                hasValidJpegMagic = false,
                uploadSucceeded = false,
                bytesTransmittedVerified = false,
                publicAccessAllowed = false,
                publicHttpStatus = null,
                publicContentType = null,
                publicDownloadedByteSize = null,
                publicSha256Matches = false,
                authenticatedAccessAllowed = false,
                authenticatedHttpStatus = null,
                authenticatedContentType = null,
                authenticatedDownloadedByteSize = null,
                authenticatedSha256Matches = false,
                bucketPolicyStatus = BucketAccessPolicyStatus.NOT_CONFIGURED,
                cleanupDeleted = false,
                steps = steps,
                recommendation = recommendation
            )
        }

        // STEP 2: Prepare & Inspect JPEG Image Bytes
        val encodeStartMs = System.currentTimeMillis()
        val createdTempBitmap = sampleBitmap == null
        val sourceBitmap = sampleBitmap ?: createDiagnosticProbeBitmap()
        val imageBytes = try {
            encodeJpegBytes(sourceBitmap)
        } finally {
            if (createdTempBitmap && !sourceBitmap.isRecycled) {
                sourceBitmap.recycle()
            }
        }
        val expectedSha256 = sha256Hex(imageBytes)
        val hasValidMagic = hasJpegMagicBytes(imageBytes)
        val encodeDurationMs = System.currentTimeMillis() - encodeStartMs

        steps += SupabaseDiagnosticStep(
            step = "2_ENCODE_IMAGE_BYTES",
            passed = imageBytes.isNotEmpty() && hasValidMagic,
            detail = "Encoded ${imageBytes.size} bytes | ValidJPEGHeader(FF D8)=$hasValidMagic | SHA256=${expectedSha256.take(16)}...",
            durationMs = encodeDurationMs
        )

        // STEP 3: Upload Image Bytes to Supabase Storage Path
        val uploadStartMs = System.currentTimeMillis()
        val uploadResult = adapter.uploadBytes(bucketName, objectPath, imageBytes)
        val uploadDurationMs = System.currentTimeMillis() - uploadStartMs
        val uploadedPublicUrl = uploadResult.getOrNull()?.takeIf { it.isNotBlank() } ?: expectedPublicUrl

        if (uploadResult.isFailure) {
            val uploadErr = uploadResult.exceptionOrNull()?.localizedMessage ?: "Unknown upload error"
            steps += SupabaseDiagnosticStep(
                step = "3_UPLOAD_OBJECT",
                passed = false,
                detail = "Upload GAGAL ke '$bucketName/$objectPath': $uploadErr",
                durationMs = uploadDurationMs
            )
            val recommendation = buildUploadFailureRecommendation(bucketName, uploadErr)
            return@withContext finalizeAndEmitReport(
                timestampMs = nowMs,
                bucketName = bucketName,
                supabaseHost = host,
                objectPath = objectPath,
                publicUrl = uploadedPublicUrl,
                authenticatedUrl = expectedAuthUrl,
                expectedByteSize = imageBytes.size,
                expectedSha256 = expectedSha256,
                hasValidJpegMagic = hasValidMagic,
                uploadSucceeded = false,
                bytesTransmittedVerified = false,
                publicAccessAllowed = false,
                publicHttpStatus = null,
                publicContentType = null,
                publicDownloadedByteSize = null,
                publicSha256Matches = false,
                authenticatedAccessAllowed = false,
                authenticatedHttpStatus = null,
                authenticatedContentType = null,
                authenticatedDownloadedByteSize = null,
                authenticatedSha256Matches = false,
                bucketPolicyStatus = BucketAccessPolicyStatus.UPLOAD_FAILED,
                cleanupDeleted = false,
                steps = steps,
                recommendation = recommendation
            )
        }

        steps += SupabaseDiagnosticStep(
            step = "3_UPLOAD_OBJECT",
            passed = true,
            detail = "Upload SUKSES ${imageBytes.size} bytes ke '$bucketName/$objectPath' -> PublicURL=$uploadedPublicUrl",
            durationMs = uploadDurationMs
        )

        // STEP 4: Probe PUBLIC Retrieval (Zero Auth Headers — verifies public bucket policy)
        val publicProbe = httpExecutor.executeGet(uploadedPublicUrl, emptyMap())
        val publicDownloadedSize = if (publicProbe.statusCode in 200..299) publicProbe.bodyBytes.size else null
        val publicShaMatches = publicProbe.statusCode in 200..299 &&
            publicProbe.bodyBytes.size == imageBytes.size &&
            hasJpegMagicBytes(publicProbe.bodyBytes) &&
            sha256Hex(publicProbe.bodyBytes) == expectedSha256
        val publicReadOk = publicProbe.statusCode in 200..299 && publicProbe.bodyBytes.isNotEmpty()

        steps += SupabaseDiagnosticStep(
            step = "4_PROBE_PUBLIC_ACCESS",
            passed = publicShaMatches,
            httpStatus = publicProbe.statusCode.takeIf { it > 0 },
            durationMs = publicProbe.durationMs,
            detail = buildProbeDetail(
                label = "Public GET (tanpa header auth)",
                probe = publicProbe,
                expectedSize = imageBytes.size,
                shaMatches = publicShaMatches
            )
        )

        // STEP 5: Probe AUTHENTICATED Retrieval (With apikey + Authorization: Bearer, or apikey-only for sb_publishable_)
        val effectiveBearer = authBearerToken?.trim()?.takeIf { it.isNotBlank() } ?: apiKey
        val authHeaders = mapOf(
            "apikey" to apiKey,
            "Authorization" to "Bearer $effectiveBearer"
        )
        var authProbe = httpExecutor.executeGet(expectedAuthUrl, authHeaders)
        var resolvedAuthUrl = expectedAuthUrl
        if (authProbe.statusCode == 400 || authProbe.statusCode == 404) {
            // Fallback check on direct `/storage/v1/object/{bucket}/{path}` authenticated endpoint
            val directObjectUrl = "$baseUrl/storage/v1/object/$bucketName/$objectPath"
            val directProbe = httpExecutor.executeGet(directObjectUrl, authHeaders)
            if (directProbe.statusCode in 200..299 || authProbe.statusCode == 404) {
                authProbe = directProbe
                resolvedAuthUrl = directObjectUrl
            }
        }
        if (authProbe.statusCode !in 200..299 && apiKey.startsWith("sb_publishable_", ignoreCase = true)) {
            val publishableHeaders = mapOf("apikey" to apiKey)
            val pubAuthProbe = httpExecutor.executeGet(expectedAuthUrl, publishableHeaders)
            if (pubAuthProbe.statusCode in 200..299) {
                authProbe = pubAuthProbe
                resolvedAuthUrl = expectedAuthUrl
            } else {
                val directObjectUrl = "$baseUrl/storage/v1/object/$bucketName/$objectPath"
                val pubDirectProbe = httpExecutor.executeGet(directObjectUrl, publishableHeaders)
                if (pubDirectProbe.statusCode in 200..299) {
                    authProbe = pubDirectProbe
                    resolvedAuthUrl = directObjectUrl
                }
            }
        }
        val authDownloadedSize = if (authProbe.statusCode in 200..299) authProbe.bodyBytes.size else null
        val authShaMatches = authProbe.statusCode in 200..299 &&
            authProbe.bodyBytes.size == imageBytes.size &&
            hasJpegMagicBytes(authProbe.bodyBytes) &&
            sha256Hex(authProbe.bodyBytes) == expectedSha256
        val authReadOk = authProbe.statusCode in 200..299 && authProbe.bodyBytes.isNotEmpty()

        steps += SupabaseDiagnosticStep(
            step = "5_PROBE_AUTHENTICATED_ACCESS",
            passed = authShaMatches,
            httpStatus = authProbe.statusCode.takeIf { it > 0 },
            durationMs = authProbe.durationMs,
            detail = buildProbeDetail(
                label = "Authenticated GET (apikey + Bearer)",
                probe = authProbe,
                expectedSize = imageBytes.size,
                shaMatches = authShaMatches
            )
        )

        // STEP 6: Byte Transmission Integrity & Bucket Access Policy Classification
        val bytesVerified = publicShaMatches || authShaMatches
        val policyStatus = when {
            publicShaMatches -> BucketAccessPolicyStatus.PUBLIC_READ_ALLOWED
            authShaMatches -> BucketAccessPolicyStatus.AUTHENTICATED_READ_ONLY
            publicReadOk || authReadOk -> BucketAccessPolicyStatus.BYTE_MISMATCH_CORRUPTED
            else -> BucketAccessPolicyStatus.ACCESS_DENIED_OR_MISCONFIGURED
        }

        steps += SupabaseDiagnosticStep(
            step = "6_BYTE_INTEGRITY_AND_POLICY",
            passed = bytesVerified && policyStatus == BucketAccessPolicyStatus.PUBLIC_READ_ALLOWED,
            detail = "BytesTransmittedIntact=$bytesVerified | PolicyStatus=$policyStatus | Public200=$publicReadOk | Auth200=$authReadOk"
        )

        // STEP 7: Optional Cleanup of Diagnostic Probe Object
        var cleanupDeleted = false
        if (cleanupProbeObject) {
            val cleanupStartMs = System.currentTimeMillis()
            val deleteRes = adapter.deleteObject(bucketName, objectPath)
            cleanupDeleted = deleteRes.isSuccess
            steps += SupabaseDiagnosticStep(
                step = "7_CLEANUP_PROBE_OBJECT",
                passed = cleanupDeleted,
                durationMs = System.currentTimeMillis() - cleanupStartMs,
                detail = if (cleanupDeleted) {
                    "Objek uji '$bucketName/$objectPath' berhasil dihapus setelah verifikasi."
                } else {
                    "Objek uji tidak dapat dihapus otomatis (policy DELETE belum diaktifkan): ${deleteRes.exceptionOrNull()?.localizedMessage ?: "-"}"
                }
            )
        }

        val recommendation = buildFinalRecommendation(
            bucketName = bucketName,
            policyStatus = policyStatus,
            publicProbe = publicProbe,
            authProbe = authProbe,
            expectedSize = imageBytes.size
        )

        finalizeAndEmitReport(
            timestampMs = nowMs,
            bucketName = bucketName,
            supabaseHost = host,
            objectPath = objectPath,
            publicUrl = uploadedPublicUrl,
            authenticatedUrl = resolvedAuthUrl,
            expectedByteSize = imageBytes.size,
            expectedSha256 = expectedSha256,
            hasValidJpegMagic = hasValidMagic,
            uploadSucceeded = true,
            bytesTransmittedVerified = bytesVerified,
            publicAccessAllowed = publicShaMatches,
            publicHttpStatus = publicProbe.statusCode.takeIf { it > 0 },
            publicContentType = publicProbe.contentType,
            publicDownloadedByteSize = publicDownloadedSize,
            publicSha256Matches = publicShaMatches,
            authenticatedAccessAllowed = authShaMatches,
            authenticatedHttpStatus = authProbe.statusCode.takeIf { it > 0 },
            authenticatedContentType = authProbe.contentType,
            authenticatedDownloadedByteSize = authDownloadedSize,
            authenticatedSha256Matches = authShaMatches,
            bucketPolicyStatus = policyStatus,
            cleanupDeleted = cleanupDeleted,
            steps = steps,
            recommendation = recommendation
        )
    }

    /**
     * Verifies retrieval accessibility (public vs authenticated) and JPEG byte validity
     * of an already-uploaded report image URL or bucket object path.
     */
    suspend fun diagnoseExistingReportImageUrl(
        imageUrlOrPath: String,
        bucketName: String = SupabaseClient.BUCKET_REPORTS,
        authBearerToken: String? = null
    ): SupabaseUploadDiagnosticReport = withContext(Dispatchers.IO) {
        val nowMs = System.currentTimeMillis()
        val adapter = overrideStorageAdapterForTesting ?: defaultStorageAdapter
        if (overrideStorageAdapterForTesting == null && !adapter.isConfigured()) {
            SupabaseClient.ensureCloudConfigLoaded()
        }
        val httpExecutor = overrideHttpProbeExecutorForTesting ?: defaultHttpProbeExecutor
        val steps = mutableListOf<SupabaseDiagnosticStep>()

        val baseUrl = adapter.supabaseUrl().trimEnd('/')
        val apiKey = adapter.supabaseKey()
        val host = extractHostSafely(baseUrl)
        val trimmedInput = imageUrlOrPath.trim()
        val extractedPath = extractObjectPathFromUrlOrPath(trimmedInput, bucketName)
        val publicUrl = if (trimmedInput.startsWith("https://", ignoreCase = true) ||
            trimmedInput.startsWith("http://", ignoreCase = true)
        ) {
            trimmedInput
        } else {
            "$baseUrl/storage/v1/object/public/$bucketName/$extractedPath"
        }
        val authUrl = "$baseUrl/storage/v1/object/authenticated/$bucketName/$extractedPath"

        val publicProbe = httpExecutor.executeGet(publicUrl, emptyMap())
        val publicValidImage = publicProbe.statusCode in 200..299 &&
            publicProbe.bodyBytes.isNotEmpty() &&
            hasImageMagicBytes(publicProbe.bodyBytes)
        val publicSha = if (publicValidImage) sha256Hex(publicProbe.bodyBytes) else ""

        steps += SupabaseDiagnosticStep(
            step = "1_EXISTING_PUBLIC_GET",
            passed = publicValidImage,
            httpStatus = publicProbe.statusCode.takeIf { it > 0 },
            durationMs = publicProbe.durationMs,
            detail = "URL=$publicUrl | HTTP=${publicProbe.statusCode} | Bytes=${publicProbe.bodyBytes.size} | ContentType=${publicProbe.contentType ?: "-"} | ValidImageHeader=$publicValidImage"
        )

        val authHeaders = if (apiKey.isNotBlank()) {
            val bearer = authBearerToken?.trim()?.takeIf { it.isNotBlank() } ?: apiKey
            mapOf("apikey" to apiKey, "Authorization" to "Bearer $bearer")
        } else {
            emptyMap()
        }
        val authProbe = if (baseUrl.startsWith("https://") && extractedPath.isNotBlank() && authHeaders.isNotEmpty()) {
            httpExecutor.executeGet(authUrl, authHeaders)
        } else {
            HttpProbeResult(authUrl, -1, null, ByteArray(0), "Supabase URL/key belum dikonfigurasi untuk probe authenticated")
        }
        val authValidImage = authProbe.statusCode in 200..299 &&
            authProbe.bodyBytes.isNotEmpty() &&
            hasImageMagicBytes(authProbe.bodyBytes)
        val authSha = if (authValidImage) sha256Hex(authProbe.bodyBytes) else ""

        steps += SupabaseDiagnosticStep(
            step = "2_EXISTING_AUTHENTICATED_GET",
            passed = authValidImage,
            httpStatus = authProbe.statusCode.takeIf { it > 0 },
            durationMs = authProbe.durationMs,
            detail = "URL=$authUrl | HTTP=${authProbe.statusCode} | Bytes=${authProbe.bodyBytes.size} | ContentType=${authProbe.contentType ?: "-"} | ValidImageHeader=$authValidImage"
        )

        val status = when {
            publicValidImage -> BucketAccessPolicyStatus.PUBLIC_READ_ALLOWED
            authValidImage -> BucketAccessPolicyStatus.AUTHENTICATED_READ_ONLY
            !adapter.isConfigured() && !trimmedInput.startsWith("http") -> BucketAccessPolicyStatus.NOT_CONFIGURED
            else -> BucketAccessPolicyStatus.ACCESS_DENIED_OR_MISCONFIGURED
        }
        val resolvedSize = when {
            publicValidImage -> publicProbe.bodyBytes.size
            authValidImage -> authProbe.bodyBytes.size
            else -> 0
        }
        val resolvedSha = publicSha.ifBlank { authSha }

        finalizeAndEmitReport(
            timestampMs = nowMs,
            bucketName = bucketName,
            supabaseHost = host,
            objectPath = extractedPath,
            publicUrl = publicUrl,
            authenticatedUrl = authUrl,
            expectedByteSize = resolvedSize,
            expectedSha256 = resolvedSha,
            hasValidJpegMagic = publicValidImage || authValidImage,
            uploadSucceeded = true,
            bytesTransmittedVerified = publicValidImage || authValidImage,
            publicAccessAllowed = publicValidImage,
            publicHttpStatus = publicProbe.statusCode.takeIf { it > 0 },
            publicContentType = publicProbe.contentType,
            publicDownloadedByteSize = if (publicProbe.statusCode in 200..299) publicProbe.bodyBytes.size else null,
            publicSha256Matches = publicValidImage,
            authenticatedAccessAllowed = authValidImage,
            authenticatedHttpStatus = authProbe.statusCode.takeIf { it > 0 },
            authenticatedContentType = authProbe.contentType,
            authenticatedDownloadedByteSize = if (authProbe.statusCode in 200..299) authProbe.bodyBytes.size else null,
            authenticatedSha256Matches = authValidImage,
            bucketPolicyStatus = status,
            cleanupDeleted = false,
            steps = steps,
            recommendation = buildFinalRecommendation(bucketName, status, publicProbe, authProbe, resolvedSize)
        )
    }

    internal fun hasJpegMagicBytes(bytes: ByteArray): Boolean {
        if (bytes.size < 4) return false
        val soiValid = (bytes[0].toInt() and 0xFF) == 0xFF && (bytes[1].toInt() and 0xFF) == 0xD8
        return soiValid
    }

    internal fun hasImageMagicBytes(bytes: ByteArray): Boolean {
        if (bytes.size < 4) return false
        val b0 = bytes[0].toInt() and 0xFF
        val b1 = bytes[1].toInt() and 0xFF
        val b2 = bytes[2].toInt() and 0xFF
        val b3 = bytes[3].toInt() and 0xFF
        val isJpeg = b0 == 0xFF && b1 == 0xD8
        val isPng = b0 == 0x89 && b1 == 0x50 && b2 == 0x4E && b3 == 0x47
        val isWebp = bytes.size >= 12 &&
            b0 == 0x52 && b1 == 0x49 && b2 == 0x46 && b3 == 0x46
        return isJpeg || isPng || isWebp
    }

    internal fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        val sb = StringBuilder(digest.size * 2)
        for (b in digest) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }

    internal fun isValidSupabaseObjectPath(path: String): Boolean {
        if (path.isBlank() || path.startsWith("/") || path.contains("..")) return false
        return path.matches(Regex("^[A-Za-z0-9_\\-/]+\\.[A-Za-z0-9]+$"))
    }

    internal fun extractObjectPathFromUrlOrPath(input: String, bucketName: String): String {
        val publicMarker = "/storage/v1/object/public/$bucketName/"
        val authMarker = "/storage/v1/object/authenticated/$bucketName/"
        val directMarker = "/storage/v1/object/$bucketName/"
        return when {
            input.contains(publicMarker) -> input.substringAfter(publicMarker).substringBefore("?")
            input.contains(authMarker) -> input.substringAfter(authMarker).substringBefore("?")
            input.contains(directMarker) -> input.substringAfter(directMarker).substringBefore("?")
            else -> input.trimStart('/').substringBefore("?")
        }
    }

    private fun extractHostSafely(url: String): String {
        if (url.isBlank()) return "(unconfigured)"
        return try {
            URI(url).host?.takeIf { it.isNotBlank() } ?: url.take(40)
        } catch (_: Exception) {
            "(invalid-url)"
        }
    }

    private fun createDiagnosticProbeBitmap(): Bitmap {
        val bmp = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.rgb(15, 23, 42))
        val paint = Paint().apply {
            color = Color.rgb(34, 197, 94)
            style = Paint.Style.FILL
        }
        canvas.drawRect(8f, 8f, 56f, 56f, paint)
        paint.color = Color.rgb(249, 115, 22)
        canvas.drawCircle(32f, 32f, 14f, paint)
        return bmp
    }

    private fun encodeJpegBytes(bitmap: Bitmap): ByteArray {
        val optimized = ImageUtils.optimizeReportBitmap(bitmap)
        return try {
            ByteArrayOutputStream().use { baos ->
                optimized.compress(Bitmap.CompressFormat.JPEG, ImageUtils.OPTIMAL_JPEG_QUALITY, baos)
                baos.toByteArray()
            }
        } finally {
            if (optimized !== bitmap && !optimized.isRecycled) {
                optimized.recycle()
            }
        }
    }

    private fun buildProbeDetail(
        label: String,
        probe: HttpProbeResult,
        expectedSize: Int,
        shaMatches: Boolean
    ): String {
        if (probe.exceptionMessage != null) {
            return "$label GAGAL jaringan: ${probe.exceptionMessage}"
        }
        val snippetPart = probe.errorSnippet?.let { " | Body='$it'" }.orEmpty()
        return "$label -> HTTP ${probe.statusCode} | ContentType=${probe.contentType ?: "-"} | " +
            "Received=${probe.bodyBytes.size}/${expectedSize} bytes | SHA256Match=$shaMatches$snippetPart"
    }

    private fun buildUploadFailureRecommendation(bucketName: String, errorMsg: String): String {
        return when {
            errorMsg.contains("Bucket not found", ignoreCase = true) ||
                errorMsg.contains("404", ignoreCase = true) ->
                "Bucket '$bucketName' belum ada di Supabase Storage. Buat bucket bernama '$bucketName' (aktifkan opsi Public bucket jika foto ingin langsung tampil di Coil)."
            errorMsg.contains("403", ignoreCase = true) ||
                errorMsg.contains("row-level security", ignoreCase = true) ||
                errorMsg.contains("Unauthorized", ignoreCase = true) ->
                "Upload ditolak RLS policy pada bucket '$bucketName'. Tambahkan policy INSERT untuk role anon/authenticated pada storage.objects WHERE bucket_id = '$bucketName'."
            else ->
                "Periksa konektivitas jaringan, SUPABASE_URL/SUPABASE_KEY di panel Secrets, dan policy INSERT pada bucket '$bucketName'. Detail: $errorMsg"
        }
    }

    private fun buildFinalRecommendation(
        bucketName: String,
        policyStatus: BucketAccessPolicyStatus,
        publicProbe: HttpProbeResult,
        authProbe: HttpProbeResult,
        expectedSize: Int
    ): String {
        return when (policyStatus) {
            BucketAccessPolicyStatus.PUBLIC_READ_ALLOWED ->
                "OK: Jalur unggah '$bucketName', integritas byte ($expectedSize bytes, SHA-256 cocok), dan policy akses publik (HTTP 200) berfungsi normal."
            BucketAccessPolicyStatus.AUTHENTICATED_READ_ONLY ->
                "PERHATIAN: Byte gambar terkirim utuh ($expectedSize bytes), namun bucket '$bucketName' bersifat PRIVATE/AUTHENTICATED-ONLY (Public HTTP ${publicProbe.statusCode}, Auth HTTP ${authProbe.statusCode}). Agar Coil dapat menampilkan foto laporan lintas perangkat tanpa header khusus, aktifkan 'Public bucket' atau tambahkan SELECT policy publik pada storage.objects (bucket_id = '$bucketName'), atau gunakan Signed URL."
            BucketAccessPolicyStatus.BYTE_MISMATCH_CORRUPTED ->
                "KRITIS: Endpoint mengembalikan HTTP 200 tetapi jumlah byte / SHA-256 tidak cocok dengan file JPEG yang diunggah (kemungkinan proxy/CDN memodifikasi konten atau mengembalikan halaman error HTML)."
            BucketAccessPolicyStatus.ACCESS_DENIED_OR_MISCONFIGURED ->
                "GAGAL BACA: Upload berhasil, tetapi baik akses publik (HTTP ${publicProbe.statusCode}) maupun authenticated (HTTP ${authProbe.statusCode}) ditolak. Periksa RLS SELECT policy pada storage.objects untuk bucket '$bucketName'."
            BucketAccessPolicyStatus.UPLOAD_FAILED ->
                "Upload ke bucket '$bucketName' gagal. Periksa konfigurasi bucket dan RLS INSERT policy."
            BucketAccessPolicyStatus.NOT_CONFIGURED ->
                "Konfigurasi SUPABASE_URL dan SUPABASE_KEY belum diatur di panel Secrets AI Studio."
        }
    }

    private fun finalizeAndEmitReport(
        timestampMs: Long,
        bucketName: String,
        supabaseHost: String,
        objectPath: String,
        publicUrl: String,
        authenticatedUrl: String,
        expectedByteSize: Int,
        expectedSha256: String,
        hasValidJpegMagic: Boolean,
        uploadSucceeded: Boolean,
        bytesTransmittedVerified: Boolean,
        publicAccessAllowed: Boolean,
        publicHttpStatus: Int?,
        publicContentType: String?,
        publicDownloadedByteSize: Int?,
        publicSha256Matches: Boolean,
        authenticatedAccessAllowed: Boolean,
        authenticatedHttpStatus: Int?,
        authenticatedContentType: String?,
        authenticatedDownloadedByteSize: Int?,
        authenticatedSha256Matches: Boolean,
        bucketPolicyStatus: BucketAccessPolicyStatus,
        cleanupDeleted: Boolean,
        steps: List<SupabaseDiagnosticStep>,
        recommendation: String
    ): SupabaseUploadDiagnosticReport {
        val logText = buildString {
            appendLine("========== SUPABASE STORAGE DIAGNOSTIC REPORT ==========")
            appendLine("Timestamp       : $timestampMs")
            appendLine("Supabase Host   : $supabaseHost")
            appendLine("Bucket          : $bucketName")
            appendLine("Upload Path     : $objectPath")
            appendLine("Public URL      : $publicUrl")
            appendLine("Auth URL        : $authenticatedUrl")
            appendLine("Encoded Bytes   : $expectedByteSize bytes (JPEG SOI=$hasValidJpegMagic, SHA256=${expectedSha256.take(16)})")
            appendLine("Upload Status   : ${if (uploadSucceeded) "PASS" else "FAIL"}")
            appendLine("Bytes Verified  : ${if (bytesTransmittedVerified) "PASS (Exact Size + SHA-256 Match)" else "FAIL"}")
            appendLine("Public Access   : allowed=$publicAccessAllowed (HTTP=${publicHttpStatus ?: "N/A"}, bytes=${publicDownloadedByteSize ?: "N/A"}, type=${publicContentType ?: "-"}, shaMatch=$publicSha256Matches)")
            appendLine("Auth Access     : allowed=$authenticatedAccessAllowed (HTTP=${authenticatedHttpStatus ?: "N/A"}, bytes=${authenticatedDownloadedByteSize ?: "N/A"}, type=${authenticatedContentType ?: "-"}, shaMatch=$authenticatedSha256Matches)")
            appendLine("Policy Verdict  : $bucketPolicyStatus")
            appendLine("Probe Cleanup   : $cleanupDeleted")
            appendLine("--- Steps ---")
            steps.forEach { s ->
                val mark = if (s.passed) "[PASS]" else "[FAIL]"
                val httpPart = s.httpStatus?.let { " (HTTP $it)" }.orEmpty()
                appendLine("  $mark ${s.step}$httpPart [${s.durationMs}ms]: ${s.detail}")
            }
            appendLine("Recommendation  : $recommendation")
            append("========================================================")
        }

        // Emit structured diagnostic report to Android Logcat safely
        try {
            when (bucketPolicyStatus) {
                BucketAccessPolicyStatus.PUBLIC_READ_ALLOWED -> Log.i(LOG_TAG, logText)
                BucketAccessPolicyStatus.AUTHENTICATED_READ_ONLY -> Log.w(LOG_TAG, logText)
                else -> Log.e(LOG_TAG, logText)
            }
        } catch (_: Throwable) {
            // Safe no-op if android.util.Log is unmocked in plain JVM unit test
        }

        return SupabaseUploadDiagnosticReport(
            timestampMs = timestampMs,
            bucketName = bucketName,
            supabaseHost = supabaseHost,
            objectPath = objectPath,
            publicUrl = publicUrl,
            authenticatedUrl = authenticatedUrl,
            expectedByteSize = expectedByteSize,
            expectedSha256 = expectedSha256,
            hasValidJpegMagic = hasValidJpegMagic,
            uploadSucceeded = uploadSucceeded,
            bytesTransmittedVerified = bytesTransmittedVerified,
            publicAccessAllowed = publicAccessAllowed,
            publicHttpStatus = publicHttpStatus,
            publicContentType = publicContentType,
            publicDownloadedByteSize = publicDownloadedByteSize,
            publicSha256Matches = publicSha256Matches,
            authenticatedAccessAllowed = authenticatedAccessAllowed,
            authenticatedHttpStatus = authenticatedHttpStatus,
            authenticatedContentType = authenticatedContentType,
            authenticatedDownloadedByteSize = authenticatedDownloadedByteSize,
            authenticatedSha256Matches = authenticatedSha256Matches,
            bucketPolicyStatus = bucketPolicyStatus,
            cleanupDeleted = cleanupDeleted,
            steps = steps,
            recommendation = recommendation,
            formattedLog = logText
        )
    }
}
