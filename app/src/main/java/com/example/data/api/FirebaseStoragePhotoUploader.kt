package com.example.data.api

import android.content.Context
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.util.ImageUtils
import com.google.firebase.Firebase
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.UUID

/**
 * Outcome of uploading a citizen disaster report photo to Firebase Storage.
 * Distinguishes network/offline failures (which MUST defer the entire report offline
 * so text is never sent without its photo) from storage bucket/config unavailability.
 */
sealed class PhotoUploadOutcome {
    data class Success(val downloadUrl: String) : PhotoUploadOutcome()
    data class NetworkOffline(val reason: String) : PhotoUploadOutcome()
    data class StorageUnavailable(val reason: String) : PhotoUploadOutcome()
}

/**
 * Uploads GPS-watermarked citizen disaster report evidence photos to Firebase Storage
 * and awaits 100% completion to obtain the official HTTPS download URL.
 */
object FirebaseStoragePhotoUploader {

    private const val TAG = "FirebaseStorageUploader"
    private const val FOLDER_CITIZEN_REPORTS = "citizen_reports"

    @Volatile
    internal var overrideUploaderForTesting: (suspend (Bitmap, String) -> PhotoUploadOutcome)? = null

    @Volatile
    internal var overrideOnlineCheckForTesting: (() -> Boolean)? = null

    /**
     * Checks whether the device currently has an active internet connection.
     */
    fun isNetworkOnline(context: Context?): Boolean {
        overrideOnlineCheckForTesting?.let { return it() }
        if (context == null) return true
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return true
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            Log.w(TAG, "Connectivity check fallback to true: ${e.message}")
            true
        }
    }

    /**
     * Compresses the GPS-watermarked Bitmap to JPEG and uploads it to Firebase Storage,
     * awaiting 100% upload completion before fetching the public download URL.
     */
    suspend fun uploadReportPhoto(
        bitmap: Bitmap,
        reportId: String,
        context: Context? = null
    ): PhotoUploadOutcome = withContext(Dispatchers.IO) {
        if (!isNetworkOnline(context)) {
            Log.w(TAG, "Device offline — deferring photo upload & report $reportId to local SQLite queue.")
            return@withContext PhotoUploadOutcome.NetworkOffline(
                com.example.data.local.FirestoreHelper.OFFLINE_REPORT_SAVED_MESSAGE
            )
        }

        overrideUploaderForTesting?.let { customUploader ->
            return@withContext try {
                customUploader(bitmap, reportId)
            } catch (e: Throwable) {
                classifyUploadException(reportId, e)
            }
        }

        // Priority 1 on production devices: Upload directly to Supabase Storage bucket `reports` when configured
        if (SupabaseClient.isConfigured() || SupabaseClient.ensureCloudConfigLoaded(context)) {
            val sbResult = runCatching { SupabaseClient.uploadPhotoToSupabase(bitmap, reportId).getOrNull() }.getOrNull()
            if (!sbResult.isNullOrBlank() &&
                (sbResult.startsWith("https://", ignoreCase = true) || sbResult.startsWith("http://", ignoreCase = true))
            ) {
                val normalizedSbUrl = if (sbResult.startsWith("http://", ignoreCase = true)) {
                    "https://" + sbResult.substring(7)
                } else {
                    sbResult
                }
                Log.i(TAG, "Supabase Storage primary upload 100% complete for $reportId -> $normalizedSbUrl")
                return@withContext PhotoUploadOutcome.Success(normalizedSbUrl)
            }
        }

        try {
            val optimized = ImageUtils.optimizeReportBitmap(bitmap)
            val baos = ByteArrayOutputStream()
            optimized.compress(Bitmap.CompressFormat.JPEG, ImageUtils.OPTIMAL_JPEG_QUALITY, baos)
            val jpegBytes = baos.toByteArray()
            if (jpegBytes.isEmpty()) {
                return@withContext PhotoUploadOutcome.StorageUnavailable("Byte JPEG foto kosong")
            }

            val safeId = reportId.trim()
                .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
                .ifBlank { "LAP_${System.currentTimeMillis()}" }
                .take(80)
            val fileName = "${safeId}_${UUID.randomUUID().toString().take(8)}.jpg"
            val storageInstance = Firebase.storage.apply {
                maxUploadRetryTimeMillis = 3500L
                maxOperationRetryTimeMillis = 3500L
            }
            val storageRef = storageInstance.reference
                .child(FOLDER_CITIZEN_REPORTS)
                .child(fileName)

            val metadata = StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .setCustomMetadata("reportId", safeId)
                .setCustomMetadata("source", "bpbd_mukomuko_gps_camera")
                .build()

            Log.i(TAG, "Uploading $fileName (${jpegBytes.size} bytes) to Firebase Storage...")
            kotlinx.coroutines.withTimeout(5000L) {
                storageRef.putBytes(jpegBytes, metadata).await()
            }
            val downloadUrl = kotlinx.coroutines.withTimeout(4000L) {
                storageRef.downloadUrl.await().toString().trim()
            }

            if (downloadUrl.startsWith("https://", ignoreCase = true) ||
                downloadUrl.startsWith("http://", ignoreCase = true)
            ) {
                val normalized = if (downloadUrl.startsWith("http://", ignoreCase = true)) {
                    "https://" + downloadUrl.substring(7)
                } else {
                    downloadUrl
                }
                Log.i(TAG, "Firebase Storage upload 100% complete for $safeId -> $normalized")
                PhotoUploadOutcome.Success(normalized)
            } else {
                PhotoUploadOutcome.StorageUnavailable("Download URL Firebase Storage tidak valid")
            }
        } catch (e: Throwable) {
            if (isNetworkOnline(context)) {
                Log.w(TAG, "Firebase Storage bucket unavailable while device is online for $reportId (${e.message}) — falling back to Supabase/inline Cloud Data URI.")
                PhotoUploadOutcome.StorageUnavailable(
                    "Firebase Storage tidak tersedia: ${e.localizedMessage ?: e.javaClass.simpleName}"
                )
            } else {
                classifyUploadException(reportId, e)
            }
        }
    }

    internal fun classifyUploadException(reportId: String, error: Throwable): PhotoUploadOutcome {
        val msg = (error.message ?: "").lowercase()
        val cause = error.cause
        val isNetworkError = error is IOException ||
            error is UnknownHostException ||
            error is SocketTimeoutException ||
            error is ConnectException ||
            cause is IOException ||
            cause is UnknownHostException ||
            cause is SocketTimeoutException ||
            cause is ConnectException ||
            (error is StorageException && (
                error.errorCode == StorageException.ERROR_RETRY_LIMIT_EXCEEDED ||
                    error.errorCode == StorageException.ERROR_CANCELED
                )) ||
            msg.contains("network") ||
            msg.contains("unable to resolve host") ||
            msg.contains("timeout") ||
            msg.contains("timed out") ||
            msg.contains("failed to connect") ||
            msg.contains("offline")

        return if (isNetworkError) {
            Log.w(TAG, "Network error uploading photo for $reportId: ${error.message}")
            PhotoUploadOutcome.NetworkOffline(
                com.example.data.local.FirestoreHelper.OFFLINE_REPORT_SAVED_MESSAGE
            )
        } else {
            Log.w(TAG, "Firebase Storage unavailable for $reportId: ${error.message}")
            PhotoUploadOutcome.StorageUnavailable(
                "Firebase Storage tidak tersedia: ${error.localizedMessage ?: error.javaClass.simpleName}"
            )
        }
    }
}
