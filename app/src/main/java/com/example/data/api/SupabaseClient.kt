package com.example.data.api

import android.graphics.Bitmap
import android.util.Log
import com.example.BuildConfig
import com.example.util.ImageUtils
import io.github.jan.supabase.SupabaseClient as SupabaseSdkClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.UUID

/**
 * Inisialisasi Supabase Client dan utilitas unggah foto bukti laporan warga ke Supabase Storage.
 */
object SupabaseClient {

    private const val TAG = "SupabaseClient"
    const val BUCKET_REPORTS = "reports"

    fun isConfigured(): Boolean {
        val url = BuildConfig.SUPABASE_URL.trim()
        val key = BuildConfig.SUPABASE_KEY.trim()
        return url.startsWith("https://") &&
            url != "YOUR_SUPABASE_URL" &&
            !url.contains("YOUR_PROJECT") &&
            key.isNotBlank() &&
            key != "YOUR_SUPABASE_KEY"
    }

    val client: SupabaseSdkClient by lazy {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL.trim(),
            supabaseKey = BuildConfig.SUPABASE_KEY.trim()
        ) {
            install(Storage)
            install(Postgrest)
        }
    }

    /**
     * Builds a sanitized, collision-resistant JPEG object path for Supabase Storage bucket `reports`.
     */
    fun buildReportPhotoPath(reportId: String, prefix: String = ""): String {
        val safeReportId = reportId.replace(Regex("[^A-Za-z0-9_-]"), "_").ifBlank { "report" }
        val uniqueSuffix = UUID.randomUUID().toString().replace("-", "").take(12)
        val cleanPrefix = prefix.trim().trim('/').replace(Regex("[^A-Za-z0-9_\\-/]"), "")
        return if (cleanPrefix.isNotBlank()) {
            "$cleanPrefix/${safeReportId}_$uniqueSuffix.jpg"
        } else {
            "${safeReportId}_$uniqueSuffix.jpg"
        }
    }

    /**
     * Uploads raw JPEG bytes to a specific bucket/path in Supabase Storage and returns its public URL.
     */
    suspend fun uploadRawJpegBytes(
        bucket: String = BUCKET_REPORTS,
        filePath: String,
        imageBytes: ByteArray
    ): Result<String> = withContext(Dispatchers.IO) {
        val startMs = System.currentTimeMillis()
        try {
            if (!isConfigured()) {
                return@withContext Result.failure(
                    IllegalStateException("Konfigurasi Supabase Storage (SUPABASE_URL / SUPABASE_KEY) belum diatur di panel Secrets.")
                )
            }
            if (imageBytes.isEmpty()) {
                return@withContext Result.failure(
                    IllegalArgumentException("Byte gambar JPEG kosong (0 bytes); batal unggah ke '$bucket/$filePath'.")
                )
            }
            val validMagic = SupabaseStorageDiagnostics.hasJpegMagicBytes(imageBytes)
            val shaPrefix = SupabaseStorageDiagnostics.sha256Hex(imageBytes).take(16)
            val storageBucket = client.storage.from(bucket)
            storageBucket.upload(path = filePath, data = imageBytes) {
                contentType = ContentType.Image.JPEG
                upsert = false
            }
            val publicUrl = storageBucket.publicUrl(filePath)
            val elapsedMs = System.currentTimeMillis() - startMs
            Log.i(
                TAG,
                "Supabase upload OK | bucket=$bucket | path=$filePath | bytes=${imageBytes.size} | " +
                    "jpegHeader=$validMagic | sha256=$shaPrefix | duration=${elapsedMs}ms | url=$publicUrl"
            )
            Result.success(publicUrl)
        } catch (e: Exception) {
            val elapsedMs = System.currentTimeMillis() - startMs
            Log.e(
                TAG,
                "Supabase upload FAILED | bucket=$bucket | path=$filePath | bytes=${imageBytes.size} | " +
                    "duration=${elapsedMs}ms | error=${e.localizedMessage}",
                e
            )
            Result.failure(e)
        }
    }

    /**
     * Deletes a specific object from a Supabase Storage bucket (used for diagnostic probe cleanup).
     */
    suspend fun deleteObjectFromBucket(
        bucket: String = BUCKET_REPORTS,
        filePath: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!isConfigured()) {
                return@withContext Result.failure(IllegalStateException("Supabase Storage belum dikonfigurasi."))
            }
            client.storage.from(bucket).delete(filePath)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Mengompres Bitmap ke format JPG ringan, mengunggahnya ke bucket publik `reports`
     * pada Supabase Storage dengan nama file acak anti-overwrite (`upsert = false`),
     * lalu mengembalikan URL publik (`https://...`).
     */
    suspend fun uploadPhotoToSupabase(bitmap: Bitmap, reportId: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                if (!isConfigured()) {
                    Log.w(TAG, "Skip Supabase photo upload for $reportId: SUPABASE_URL/SUPABASE_KEY belum diatur di Secrets.")
                    return@withContext Result.failure(
                        IllegalStateException("Konfigurasi Supabase Storage (SUPABASE_URL / SUPABASE_KEY) belum diatur di panel Secrets.")
                    )
                }
                val optimized = ImageUtils.optimizeReportBitmap(bitmap)
                val imageBytes = try {
                    ByteArrayOutputStream().use { baos ->
                        optimized.compress(
                            Bitmap.CompressFormat.JPEG,
                            ImageUtils.OPTIMAL_JPEG_QUALITY,
                            baos
                        )
                        baos.toByteArray()
                    }
                } finally {
                    if (optimized !== bitmap && !optimized.isRecycled) {
                        optimized.recycle()
                    }
                }

                val filePath = buildReportPhotoPath(reportId)
                uploadRawJpegBytes(
                    bucket = BUCKET_REPORTS,
                    filePath = filePath,
                    imageBytes = imageBytes
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed preparing/uploading photo to Supabase for $reportId: ${e.localizedMessage}", e)
                Result.failure(e)
            }
        }

    /**
     * Diagnostic log function to verify the Supabase image upload path, transmitted JPEG byte integrity
     * (size + JPEG magic header + SHA-256), and bucket access policy (`public` vs `authenticated`).
     */
    suspend fun diagnoseImageUploadPath(
        sampleBitmap: Bitmap? = null,
        reportId: String = "diag_probe",
        authBearerToken: String? = null,
        cleanupProbeObject: Boolean = true
    ): SupabaseUploadDiagnosticReport = SupabaseStorageDiagnostics.diagnoseImageUploadPath(
        sampleBitmap = sampleBitmap,
        reportId = reportId,
        bucketName = BUCKET_REPORTS,
        authBearerToken = authBearerToken,
        cleanupProbeObject = cleanupProbeObject
    )

    /**
     * Diagnostic log function to inspect retrieval policy (public vs authenticated) for an existing report photo URL.
     */
    suspend fun diagnoseExistingReportImageUrl(
        imageUrlOrPath: String,
        authBearerToken: String? = null
    ): SupabaseUploadDiagnosticReport = SupabaseStorageDiagnostics.diagnoseExistingReportImageUrl(
        imageUrlOrPath = imageUrlOrPath,
        bucketName = BUCKET_REPORTS,
        authBearerToken = authBearerToken
    )
}
