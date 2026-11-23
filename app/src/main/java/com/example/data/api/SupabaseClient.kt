package com.example.data.api

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.util.LruCache
import com.example.BuildConfig
import com.example.util.ImageUtils
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import io.github.jan.supabase.SupabaseClient as SupabaseSdkClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Inisialisasi Supabase Client, sinkronisasi konfigurasi Secrets lintas-build,
 * dan utilitas unggah/unduh foto bukti laporan warga pada Supabase Storage bucket `reports`.
 */
object SupabaseClient {

    private const val TAG = "SupabaseClient"
    const val BUCKET_REPORTS = "reports"

    private const val PREFS_NAME = "bpbd_supabase_config_cache"
    private const val KEY_CACHED_URL = "supabase_url"
    private const val KEY_CACHED_KEY = "supabase_key"
    private const val FIRESTORE_SETTINGS_COLLECTION = "app_settings"
    private const val FIRESTORE_SUPABASE_DOC = "supabase_storage_config"

    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var cachedRuntimeUrl: String = ""

    @Volatile
    private var cachedRuntimeKey: String = ""

    @Volatile
    private var activeSdkClient: SupabaseSdkClient? = null

    @Volatile
    private var activeSdkClientSignature: String = ""

    private val photoBitmapCache = object : LruCache<String, Bitmap>(32) {}

    private val storageHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(25, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    internal fun cleanUrlValue(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        var cleaned = raw.trim()
            .trim('"', '\'', '`', ' ')
            .trimEnd('/')
        val storageIdx = cleaned.indexOf("/storage/v1", ignoreCase = true)
        if (storageIdx > 0) {
            cleaned = cleaned.substring(0, storageIdx).trimEnd('/')
        }
        val restIdx = cleaned.indexOf("/rest/v1", ignoreCase = true)
        if (restIdx > 0) {
            cleaned = cleaned.substring(0, restIdx).trimEnd('/')
        }
        return cleaned
    }

    internal fun cleanKeyValue(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        return raw.trim().trim('"', '\'', '`', ' ')
    }

    internal fun isValidSupabaseUrl(url: String): Boolean {
        val cleaned = cleanUrlValue(url)
        return cleaned.startsWith("https://", ignoreCase = true) &&
            !cleaned.equals("YOUR_SUPABASE_URL", ignoreCase = true) &&
            !cleaned.contains("YOUR_PROJECT", ignoreCase = true) &&
            !cleaned.contains("YOUR_SUPABASE", ignoreCase = true) &&
            cleaned.contains(".")
    }

    internal fun isValidSupabaseKey(key: String): Boolean {
        val cleaned = cleanKeyValue(key)
        return cleaned.length >= 16 &&
            !cleaned.equals("YOUR_SUPABASE_KEY", ignoreCase = true) &&
            !cleaned.contains("YOUR_SUPABASE", ignoreCase = true)
    }

    fun getEffectiveSupabaseUrl(): String {
        val buildUrl = cleanUrlValue(BuildConfig.SUPABASE_URL)
        if (isValidSupabaseUrl(buildUrl)) return buildUrl
        val runtimeUrl = cleanUrlValue(cachedRuntimeUrl)
        if (isValidSupabaseUrl(runtimeUrl)) return runtimeUrl
        return buildUrl.ifBlank { BuildConfig.SUPABASE_URL.trim() }
    }

    fun getEffectiveSupabaseKey(): String {
        val buildKey = cleanKeyValue(BuildConfig.SUPABASE_KEY)
        if (isValidSupabaseKey(buildKey)) return buildKey
        val runtimeKey = cleanKeyValue(cachedRuntimeKey)
        if (isValidSupabaseKey(runtimeKey)) return runtimeKey
        return buildKey.ifBlank { BuildConfig.SUPABASE_KEY.trim() }
    }

    fun isConfigured(): Boolean {
        return isValidSupabaseUrl(getEffectiveSupabaseUrl()) &&
            isValidSupabaseKey(getEffectiveSupabaseKey())
    }

    /**
     * Memperbarui cache konfigurasi Supabase di memori & SharedPreferences.
     */
    fun updateRuntimeConfig(url: String, key: String, context: Context? = null) {
        val cleanUrl = cleanUrlValue(url)
        val cleanKey = cleanKeyValue(key)
        if (!isValidSupabaseUrl(cleanUrl) || !isValidSupabaseKey(cleanKey)) return
        cachedRuntimeUrl = cleanUrl
        cachedRuntimeKey = cleanKey
        context?.applicationContext?.let { appCtx ->
            runCatching {
                appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY_CACHED_URL, cleanUrl)
                    .putString(KEY_CACHED_KEY, cleanKey)
                    .apply()
            }
        }
    }

    /**
     * Sinkronisasi dua arah antara BuildConfig (panel Secrets AI Studio), SharedPreferences lokal,
     * dan dokumen Firestore `app_settings/supabase_storage_config` agar build lokal Android Studio
     * yang diekspor tanpa file `.env` tetap otomatis mendapatkan konfigurasi Supabase yang valid.
     */
    fun syncConfigWithFirestore(context: Context? = null) {
        context?.applicationContext?.let { appCtx ->
            runCatching {
                val prefs = appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val savedUrl = cleanUrlValue(prefs.getString(KEY_CACHED_URL, null))
                val savedKey = cleanKeyValue(prefs.getString(KEY_CACHED_KEY, null))
                if (isValidSupabaseUrl(savedUrl) && isValidSupabaseKey(savedKey)) {
                    cachedRuntimeUrl = savedUrl
                    cachedRuntimeKey = savedKey
                }
            }
        }

        val buildUrl = cleanUrlValue(BuildConfig.SUPABASE_URL)
        val buildKey = cleanKeyValue(BuildConfig.SUPABASE_KEY)
        if (isValidSupabaseUrl(buildUrl) && isValidSupabaseKey(buildKey)) {
            updateRuntimeConfig(buildUrl, buildKey, context)
            backgroundScope.launch {
                runCatching {
                    val payload = mapOf(
                        "supabaseUrl" to buildUrl,
                        "supabaseKey" to buildKey,
                        "bucketName" to BUCKET_REPORTS,
                        "updatedAt" to System.currentTimeMillis()
                    )
                    val db = com.example.data.local.FirestoreHelper.getFirestoreOrThrow(context).getOrNull()
                    if (db != null) {
                        withTimeoutOrNull(4500L) {
                            db.collection(FIRESTORE_SETTINGS_COLLECTION)
                                .document(FIRESTORE_SUPABASE_DOC)
                                .set(payload, SetOptions.merge())
                                .await()
                        }
                        Log.i(TAG, "Synced valid BuildConfig Supabase configuration to Firestore app_settings.")
                    }
                }.onFailure { e ->
                    Log.w(TAG, "Skipped syncing Supabase config to Firestore: ${e.message}")
                }
            }
        } else {
            backgroundScope.launch {
                ensureCloudConfigLoaded(context)
            }
        }
    }

    /**
     * Memastikan konfigurasi Supabase diambil dari Firestore `app_settings/supabase_storage_config`
     * apabila APK dibangun secara lokal tanpa `.env`.
     */
    suspend fun ensureCloudConfigLoaded(context: Context? = null): Boolean = withContext(Dispatchers.IO) {
        if (isConfigured()) return@withContext true
        context?.applicationContext?.let { appCtx ->
            runCatching {
                val prefs = appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val savedUrl = cleanUrlValue(prefs.getString(KEY_CACHED_URL, null))
                val savedKey = cleanKeyValue(prefs.getString(KEY_CACHED_KEY, null))
                if (isValidSupabaseUrl(savedUrl) && isValidSupabaseKey(savedKey)) {
                    cachedRuntimeUrl = savedUrl
                    cachedRuntimeKey = savedKey
                    return@withContext true
                }
            }
        }
        try {
            val db = com.example.data.local.FirestoreHelper.getFirestoreOrThrow(context).getOrNull()
            val snap = if (db != null) {
                withTimeoutOrNull(4500L) {
                    db.collection(FIRESTORE_SETTINGS_COLLECTION)
                        .document(FIRESTORE_SUPABASE_DOC)
                        .get()
                        .await()
                }
            } else {
                null
            }
            if (snap != null && snap.exists()) {
                val remoteUrl = cleanUrlValue(snap.getString("supabaseUrl"))
                val remoteKey = cleanKeyValue(snap.getString("supabaseKey"))
                if (isValidSupabaseUrl(remoteUrl) && isValidSupabaseKey(remoteKey)) {
                    updateRuntimeConfig(remoteUrl, remoteKey, context)
                    Log.i(TAG, "Loaded Supabase Storage config from Firestore app_settings (host=$remoteUrl)")
                    return@withContext true
                }
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Unable to fetch Supabase config from Firestore: ${e.message}")
        }
        isConfigured()
    }

    private fun getOrCreateSdkClient(): SupabaseSdkClient {
        val url = getEffectiveSupabaseUrl()
        val key = getEffectiveSupabaseKey()
        val signature = "$url|$key"
        val existing = activeSdkClient
        if (existing != null && activeSdkClientSignature == signature) {
            return existing
        }
        val created = createSupabaseClient(
            supabaseUrl = url,
            supabaseKey = key
        ) {
            install(Storage)
            install(Postgrest)
        }
        activeSdkClient = created
        activeSdkClientSignature = signature
        return created
    }

    val client: SupabaseSdkClient
        get() = getOrCreateSdkClient()

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

    fun getCachedPhotoBitmap(urlOrPath: String?): Bitmap? {
        val key = urlOrPath?.trim()?.takeIf { it.isNotBlank() } ?: return null
        return synchronized(photoBitmapCache) {
            photoBitmapCache.get(key)?.takeIf { !it.isRecycled }
        }
    }

    fun cachePhotoBitmap(urlOrPath: String?, bitmap: Bitmap?) {
        val key = urlOrPath?.trim()?.takeIf { it.isNotBlank() } ?: return
        val bmp = bitmap?.takeIf { !it.isRecycled && it.width > 0 && it.height > 0 } ?: return
        synchronized(photoBitmapCache) {
            photoBitmapCache.put(key, bmp)
        }
    }

    /**
     * Uploads raw JPEG bytes to a specific bucket/path in Supabase Storage and returns an accessible HTTPS URL.
     * Supports both legacy anon JWT keys (`eyJ...`) and modern Supabase publishable keys (`sb_publishable_...`),
     * automatically ensures bucket `reports` exists/public if possible, and falls back to Signed URL when private.
     */
    suspend fun uploadRawJpegBytes(
        bucket: String = BUCKET_REPORTS,
        filePath: String,
        imageBytes: ByteArray
    ): Result<String> = withContext(Dispatchers.IO) {
        val startMs = System.currentTimeMillis()
        try {
            if (!isConfigured()) {
                ensureCloudConfigLoaded()
            }
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

            val baseUrl = getEffectiveSupabaseUrl().trimEnd('/')
            val apiKey = getEffectiveSupabaseKey()
            val cleanPath = filePath.trimStart('/')
            val validMagic = SupabaseStorageDiagnostics.hasJpegMagicBytes(imageBytes)
            val shaPrefix = SupabaseStorageDiagnostics.sha256Hex(imageBytes).take(16)

            // 1. Try direct HTTP REST upload (compatible with both JWT and sb_publishable_ keys)
            var directUploadOk = executeDirectRestUpload(baseUrl, apiKey, bucket, cleanPath, imageBytes)
            if (!directUploadOk.first && directUploadOk.second.contains("Bucket not found", ignoreCase = true)) {
                ensurePublicBucketExistsRest(baseUrl, apiKey, bucket)
                directUploadOk = executeDirectRestUpload(baseUrl, apiKey, bucket, cleanPath, imageBytes)
            }

            // 2. Fallback to Supabase Kotlin SDK if direct REST did not succeed
            if (!directUploadOk.first) {
                try {
                    val storageBucket = getOrCreateSdkClient().storage.from(bucket)
                    storageBucket.upload(path = cleanPath, data = imageBytes) {
                        contentType = ContentType.Image.JPEG
                        upsert = true
                    }
                    directUploadOk = true to "SDK upload OK"
                } catch (sdkErr: Exception) {
                    val combinedMsg = "REST: ${directUploadOk.second} | SDK: ${sdkErr.localizedMessage ?: sdkErr.javaClass.simpleName}"
                    return@withContext Result.failure(IllegalStateException(combinedMsg, sdkErr))
                }
            }

            val publicUrl = "$baseUrl/storage/v1/object/public/$bucket/$cleanPath"
            val resolvedUrl = resolveBestAccessiblePhotoUrl(
                baseUrl = baseUrl,
                apiKey = apiKey,
                bucket = bucket,
                objectPath = cleanPath,
                defaultPublicUrl = publicUrl
            )

            // Cache decoded bitmap in memory for instant local & officer display
            runCatching {
                BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)?.let { bmp ->
                    cachePhotoBitmap(publicUrl, bmp)
                    cachePhotoBitmap(resolvedUrl, bmp)
                }
            }

            val elapsedMs = System.currentTimeMillis() - startMs
            Log.i(
                TAG,
                "Supabase upload OK | bucket=$bucket | path=$cleanPath | bytes=${imageBytes.size} | " +
                    "jpegHeader=$validMagic | sha256=$shaPrefix | duration=${elapsedMs}ms | url=$resolvedUrl"
            )
            Result.success(resolvedUrl)
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

    private fun buildAuthHeaderAttempts(apiKey: String): List<Map<String, String>> {
        val isPublishableKey = apiKey.startsWith("sb_publishable_", ignoreCase = true) ||
            apiKey.startsWith("sb_secret_", ignoreCase = true)
        val withBearer = mapOf(
            "apikey" to apiKey,
            "Authorization" to "Bearer $apiKey"
        )
        val apiKeyOnly = mapOf(
            "apikey" to apiKey
        )
        return if (isPublishableKey) {
            listOf(withBearer, apiKeyOnly)
        } else {
            listOf(withBearer)
        }
    }

    private fun executeDirectRestUpload(
        baseUrl: String,
        apiKey: String,
        bucket: String,
        cleanPath: String,
        imageBytes: ByteArray
    ): Pair<Boolean, String> {
        val uploadUrl = "$baseUrl/storage/v1/object/$bucket/$cleanPath"
        val body = imageBytes.toRequestBody("image/jpeg".toMediaType())
        var lastError = "HTTP upload error"

        for (headers in buildAuthHeaderAttempts(apiKey)) {
            try {
                val reqBuilder = Request.Builder()
                    .url(uploadUrl)
                    .post(body)
                    .header("Content-Type", "image/jpeg")
                    .header("x-upsert", "true")
                headers.forEach { (k, v) -> reqBuilder.header(k, v) }

                storageHttpClient.newCall(reqBuilder.build()).execute().use { resp ->
                    if (resp.isSuccessful) {
                        return true to "HTTP ${resp.code}"
                    }
                    val errBody = resp.body?.string()?.take(300).orEmpty()
                    lastError = "HTTP ${resp.code}: $errBody"
                    if (!errBody.contains("Invalid JWT", ignoreCase = true) &&
                        !errBody.contains("compact JWS", ignoreCase = true) &&
                        resp.code != 401
                    ) {
                        return false to lastError
                    }
                }
            } catch (e: Exception) {
                lastError = "${e.javaClass.simpleName}: ${e.localizedMessage}"
            }
        }
        return false to lastError
    }

    private fun ensurePublicBucketExistsRest(baseUrl: String, apiKey: String, bucket: String) {
        val jsonPayload = JSONObject().apply {
            put("id", bucket)
            put("name", bucket)
            put("public", true)
        }.toString().toRequestBody("application/json".toMediaType())

        for (headers in buildAuthHeaderAttempts(apiKey)) {
            runCatching {
                val createReq = Request.Builder()
                    .url("$baseUrl/storage/v1/bucket")
                    .post(jsonPayload)
                    .apply { headers.forEach { (k, v) -> header(k, v) } }
                    .build()
                storageHttpClient.newCall(createReq).execute().close()

                val updateReq = Request.Builder()
                    .url("$baseUrl/storage/v1/bucket/$bucket")
                    .put(jsonPayload)
                    .apply { headers.forEach { (k, v) -> header(k, v) } }
                    .build()
                storageHttpClient.newCall(updateReq).execute().close()
            }
        }
    }

    private fun resolveBestAccessiblePhotoUrl(
        baseUrl: String,
        apiKey: String,
        bucket: String,
        objectPath: String,
        defaultPublicUrl: String
    ): String {
        // Check if public URL is directly readable without headers
        try {
            val probeReq = Request.Builder().url(defaultPublicUrl).get().build()
            storageHttpClient.newCall(probeReq).execute().use { resp ->
                if (resp.isSuccessful) {
                    return defaultPublicUrl
                }
            }
        } catch (_: Exception) {
        }

        // If public URL returned 400/403/404 (e.g., bucket is private), try make bucket public & create long-lived signed URL
        ensurePublicBucketExistsRest(baseUrl, apiKey, bucket)

        for (headers in buildAuthHeaderAttempts(apiKey)) {
            try {
                val signBody = JSONObject().apply {
                    put("expiresIn", 315360000) // 10 years
                }.toString().toRequestBody("application/json".toMediaType())
                val signReq = Request.Builder()
                    .url("$baseUrl/storage/v1/object/sign/$bucket/$objectPath")
                    .post(signBody)
                    .apply { headers.forEach { (k, v) -> header(k, v) } }
                    .build()
                storageHttpClient.newCall(signReq).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val rawJson = resp.body?.string().orEmpty()
                        val signedPath = JSONObject(rawJson).optString("signedURL", "").trim()
                        if (signedPath.isNotBlank()) {
                            return when {
                                signedPath.startsWith("https://", ignoreCase = true) -> signedPath
                                signedPath.startsWith("/storage/v1") -> "$baseUrl$signedPath"
                                signedPath.startsWith("/") -> "$baseUrl/storage/v1$signedPath"
                                else -> "$baseUrl/storage/v1/$signedPath"
                            }
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }

        return defaultPublicUrl
    }

    /**
     * Mengunduh foto laporan warga dari URL Supabase (baik bucket Public, Authenticated, maupun Signed URL)
     * menggunakan kredensial `SUPABASE_KEY` bila akses publik memerlukan otorisasi, serta memeriksa fallback Data URI.
     */
    suspend fun downloadReportPhotoBitmap(urlOrPath: String?): Bitmap? = withContext(Dispatchers.IO) {
        val trimmed = urlOrPath?.trim().orEmpty()
        if (trimmed.isBlank()) return@withContext null

        getCachedPhotoBitmap(trimmed)?.let { return@withContext it }

        // Check inline Data URI fallback cache in ImageUtils
        ImageUtils.getInlineDataUriForUrl(trimmed)?.let { inlineUri ->
            ImageUtils.decodePhotoSourceToBitmap(inlineUri)?.let { bmp ->
                cachePhotoBitmap(trimmed, bmp)
                return@withContext bmp
            }
        }

        if (!trimmed.startsWith("https://", ignoreCase = true) &&
            !trimmed.startsWith("http://", ignoreCase = true)
        ) {
            return@withContext ImageUtils.decodePhotoSourceToBitmap(trimmed)
        }

        if (!isConfigured()) {
            ensureCloudConfigLoaded()
        }

        val baseUrl = getEffectiveSupabaseUrl().trimEnd('/')
        val apiKey = getEffectiveSupabaseKey()
        val candidateUrls = LinkedHashSet<String>()
        candidateUrls.add(trimmed)

        val extractedPath = SupabaseStorageDiagnostics.extractObjectPathFromUrlOrPath(trimmed, BUCKET_REPORTS)
        if (baseUrl.startsWith("https://") && extractedPath.isNotBlank() && !extractedPath.startsWith("http")) {
            candidateUrls.add("$baseUrl/storage/v1/object/public/$BUCKET_REPORTS/$extractedPath")
            candidateUrls.add("$baseUrl/storage/v1/object/authenticated/$BUCKET_REPORTS/$extractedPath")
            candidateUrls.add("$baseUrl/storage/v1/object/$BUCKET_REPORTS/$extractedPath")
        }

        val headerAttempts = mutableListOf<Map<String, String>>(emptyMap())
        if (isValidSupabaseKey(apiKey)) {
            headerAttempts.addAll(buildAuthHeaderAttempts(apiKey))
        }

        for (candidateUrl in candidateUrls) {
            for (headers in headerAttempts) {
                try {
                    val reqBuilder = Request.Builder().url(candidateUrl).get()
                    headers.forEach { (k, v) -> reqBuilder.header(k, v) }
                    storageHttpClient.newCall(reqBuilder.build()).execute().use { resp ->
                        if (resp.isSuccessful) {
                            val bytes = resp.body?.bytes() ?: ByteArray(0)
                            if (bytes.isNotEmpty() && SupabaseStorageDiagnostics.hasImageMagicBytes(bytes)) {
                                val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                if (decoded != null && decoded.width > 0 && decoded.height > 0) {
                                    cachePhotoBitmap(trimmed, decoded)
                                    cachePhotoBitmap(candidateUrl, decoded)
                                    return@withContext decoded
                                }
                            }
                        }
                    }
                } catch (_: Exception) {
                }
            }
        }
        null
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
                ensureCloudConfigLoaded()
            }
            if (!isConfigured()) {
                return@withContext Result.failure(IllegalStateException("Supabase Storage belum dikonfigurasi."))
            }
            val baseUrl = getEffectiveSupabaseUrl().trimEnd('/')
            val apiKey = getEffectiveSupabaseKey()
            val cleanPath = filePath.trimStart('/')
            for (headers in buildAuthHeaderAttempts(apiKey)) {
                try {
                    val req = Request.Builder()
                        .url("$baseUrl/storage/v1/object/$bucket/$cleanPath")
                        .delete()
                        .apply { headers.forEach { (k, v) -> header(k, v) } }
                        .build()
                    storageHttpClient.newCall(req).execute().use { resp ->
                        if (resp.isSuccessful) {
                            return@withContext Result.success(Unit)
                        }
                    }
                } catch (_: Exception) {
                }
            }
            getOrCreateSdkClient().storage.from(bucket).delete(cleanPath)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Mengompres Bitmap ke format JPG ringan, mengunggahnya ke bucket `reports`
     * pada Supabase Storage dengan nama file acak anti-overwrite,
     * lalu mengembalikan URL HTTPS yang dapat diakses lintas perangkat.
     */
    suspend fun uploadPhotoToSupabase(bitmap: Bitmap, reportId: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                if (!isConfigured()) {
                    ensureCloudConfigLoaded()
                }
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
