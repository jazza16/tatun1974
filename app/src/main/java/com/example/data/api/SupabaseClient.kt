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
    private const val BUCKET_REPORTS = "reports"

    private fun isConfigured(): Boolean {
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
     * Mengompres Bitmap ke format JPG ringan, mengunggahnya ke bucket publik `reports`
     * pada Supabase Storage dengan nama file acak anti-overwrite (`upsert = false`),
     * lalu mengembalikan URL publik (`https://...`).
     */
    suspend fun uploadPhotoToSupabase(bitmap: Bitmap, reportId: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                if (!isConfigured()) {
                    return@withContext Result.failure(
                        IllegalStateException("Konfigurasi Supabase Storage (SUPABASE_URL / SUPABASE_KEY) belum diatur di panel Secrets.")
                    )
                }
                val safeReportId = reportId.replace(Regex("[^A-Za-z0-9_-]"), "_").ifBlank { "report" }
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

                val uniqueSuffix = UUID.randomUUID().toString().replace("-", "").take(12)
                val filePath = "${safeReportId}_$uniqueSuffix.jpg"
                val bucket = client.storage.from(BUCKET_REPORTS)
                bucket.upload(path = filePath, data = imageBytes) {
                    contentType = ContentType.Image.JPEG
                    upsert = false
                }

                val publicUrl = bucket.publicUrl(filePath)
                Log.d(TAG, "Uploaded photo to Supabase for $safeReportId")
                Result.success(publicUrl)
            } catch (e: Exception) {
                Log.e(TAG, "Failed uploading photo to Supabase: ${e.localizedMessage}", e)
                Result.failure(e)
            }
        }
}
