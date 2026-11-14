package com.example.data.api

import android.graphics.Bitmap
import android.util.Log
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

/**
 * Inisialisasi Supabase Client dan utilitas unggah foto bukti laporan warga ke Supabase Storage.
 */
object SupabaseClient {

    private const val TAG = "SupabaseClient"
    private const val SUPABASE_URL = "https://xuohnibmkuvgvxkvpjaz.supabase.co"
    private const val SUPABASE_KEY = "sb_publishable_KceriqqRoIEkQYGqjiBK0w_wAlqLh_z"
    private const val BUCKET_REPORTS = "reports"

    val client: SupabaseSdkClient by lazy {
        createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_KEY
        ) {
            install(Storage)
            install(Postgrest)
        }
    }

    /**
     * Mengompres Bitmap ke format JPG ringan, mengunggahnya ke bucket publik `reports`
     * pada Supabase Storage, lalu mengembalikan URL publik (`https://...`).
     */
    suspend fun uploadPhotoToSupabase(bitmap: Bitmap, reportId: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val optimized = ImageUtils.optimizeReportBitmap(bitmap)
                val baos = ByteArrayOutputStream()
                optimized.compress(
                    Bitmap.CompressFormat.JPEG,
                    ImageUtils.OPTIMAL_JPEG_QUALITY,
                    baos
                )
                val imageBytes = baos.toByteArray()

                val filePath = "$reportId.jpg"
                val bucket = client.storage.from(BUCKET_REPORTS)
                bucket.upload(path = filePath, data = imageBytes) {
                    contentType = ContentType.Image.JPEG
                    upsert = true
                }

                val publicUrl = bucket.publicUrl(filePath)
                Log.d(TAG, "Uploaded photo to Supabase for $reportId: $publicUrl")
                Result.success(publicUrl)
            } catch (e: Exception) {
                Log.e(TAG, "Failed uploading photo to Supabase for $reportId: ${e.localizedMessage}", e)
                Result.failure(e)
            }
        }
}
