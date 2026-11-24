package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import com.example.data.api.FirebaseStoragePhotoUploader
import com.example.data.api.PhotoUploadOutcome
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlin.math.max

/**
 * Image compression, persistent storage, and cloud-portable encoding utility
 * for Citizen Disaster Reports.
 *
 * Ensures GPS camera evidence photos are an inseparable part of every citizen report:
 * - Downscales high-resolution camera captures safely without OOM and corrects EXIF rotation.
 * - Stores local copies inside persistent app storage (`filesDir/citizen_report_photos`) instead of volatile `cacheDir`.
 * - Encodes photos into compact, high-clarity JPEG Data URIs (`data:image/jpeg;base64,...`, <= 350 KB)
 *   whenever external object storage (Supabase) is unconfigured or offline, guaranteeing cross-device
 *   sync directly via Cloud Firestore well within Firestore's 1 MiB document limit.
 */
object ImageUtils {

    const val OPTIMAL_MAX_DIMENSION = 1280
    const val OPTIMAL_JPEG_QUALITY = 82

    /**
     * Conservative cap for inline Base64 Data URI stored in Firestore (`fotoUrl` / `photoUri`).
     * Kept <= 300,000 chars so storing both fields stays safely under Firestore's 1 MiB document limit.
     */
    const val MAX_CLOUD_PHOTO_DATA_URI_CHARS = 300_000

    /**
     * Hard schema ceiling mirrored in `firestore.rules` (`data.photoUri.size() <= 400000` & `data.fotoUrl.size() <= 400000`).
     */
    const val FIRESTORE_PHOTO_URI_MAX_CHARS = 400_000

    private const val REPORT_PHOTOS_DIR = "citizen_report_photos"

    private val inlineDataUriByReportId = java.util.concurrent.ConcurrentHashMap<String, String>()
    private val inlineDataUriByUrl = java.util.concurrent.ConcurrentHashMap<String, String>()

    @Volatile
    var lastSelectedCitizenReportPhotoUri: String? = null

    fun rememberInlineDataUriForReport(reportId: String?, dataUri: String?) {
        val id = reportId?.trim()?.takeIf { it.isNotBlank() } ?: return
        val uri = dataUri?.trim()?.takeIf { it.startsWith("data:image", ignoreCase = true) } ?: return
        inlineDataUriByReportId[id] = uri
    }

    fun getInlineDataUriForReport(reportId: String?): String? {
        val id = reportId?.trim()?.takeIf { it.isNotBlank() } ?: return null
        return inlineDataUriByReportId[id]
    }

    fun rememberInlineDataUriForUrl(url: String?, dataUri: String?) {
        val cleanUrl = url?.trim()?.takeIf { it.isNotBlank() } ?: return
        val uri = dataUri?.trim()?.takeIf { it.startsWith("data:image", ignoreCase = true) } ?: return
        inlineDataUriByUrl[cleanUrl] = uri
    }

    fun getInlineDataUriForUrl(url: String?): String? {
        val cleanUrl = url?.trim()?.takeIf { it.isNotBlank() } ?: return null
        return inlineDataUriByUrl[cleanUrl]
    }

    /**
     * Result of resolving a citizen report photo prior to Firestore sync.
     * Guarantees strict atomicity: if a citizen attached a photo, the report is NEVER
     * pushed to Firestore without its uploaded photo URL/payload.
     */
    sealed class CloudPhotoResolution {
        data object NoPhoto : CloudPhotoResolution()
        data class ReadyForCloud(val cloudPhotoUrl: String) : CloudPhotoResolution()
        data class DeferredOffline(val reason: String) : CloudPhotoResolution()
    }

    /**
     * Permanently renders the official BPBD Mukomuko GPS watermark directly onto the Bitmap pixels
     * using Canvas/Paint so the uploaded JPEG file itself carries the verified GPS coordinates,
     * accuracy, timestamp, and village/district stamp (not just a UI overlay).
     */
    fun renderGpsWatermarkOnBitmap(
        source: Bitmap,
        latitude: Double,
        longitude: Double,
        incidentTime: String,
        locationLabel: String = "",
        accuracyMeters: Float? = null
    ): Bitmap {
        val optimized = optimizeReportBitmap(source)
        val width = optimized.width.coerceAtLeast(1)
        val height = optimized.height.coerceAtLeast(1)
        val mutable = optimized.copy(Bitmap.Config.ARGB_8888, true)
            ?: Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bmp ->
                Canvas(bmp).drawBitmap(optimized, 0f, 0f, null)
            }

        val canvas = Canvas(mutable)
        val baseSize = (width.coerceAtMost(height) * 0.030f).coerceIn(12f, 32f)
        val padH = (width * 0.028f).coerceIn(10f, 28f)
        val padV = (height * 0.018f).coerceIn(8f, 22f)
        val lineGap = baseSize * 1.32f

        val accText = accuracyMeters?.takeIf { !it.isNaN() && !it.isInfinite() && it > 0f }
            ?.let { String.format(Locale.US, " (±%.0fm)", it) }
            .orEmpty()
        val line1 = "BPBD MUKOMUKO • FOTO BUKTI GPS TERVERIFIKASI"
        val line2 = String.format(Locale.US, "GPS: %.5f, %.5f%s", latitude, longitude, accText)
        val cleanTime = incidentTime.trim().ifBlank { "Waktu Tersinkronisasi" }
        val cleanLoc = locationLabel.trim()
        val line3 = if (cleanLoc.isNotBlank()) {
            "$cleanTime • $cleanLoc".take(76)
        } else {
            cleanTime.take(76)
        }

        val barHeight = padV * 2f + lineGap * 3f
        val topY = (height - barHeight).coerceAtLeast(0f)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(195, 15, 23, 42) // Slate-900 translucent stamp bar
            style = Paint.Style.FILL
        }
        canvas.drawRect(RectF(0f, topY, width.toFloat(), height.toFloat()), bgPaint)

        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(34, 197, 94) // Emerald-500 verification stripe
            style = Paint.Style.FILL
        }
        canvas.drawRect(RectF(0f, topY, width.toFloat(), topY + (baseSize * 0.16f).coerceAtLeast(2f)), accentPaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(134, 239, 172) // Emerald-300
            textSize = baseSize * 0.92f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(2f, 1f, 1f, Color.BLACK)
        }
        val coordPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(251, 191, 36) // Amber-400
            textSize = baseSize * 0.98f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(2f, 1f, 1f, Color.BLACK)
        }
        val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = baseSize * 0.86f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            setShadowLayer(2f, 1f, 1f, Color.BLACK)
        }

        var baselineY = topY + padV + baseSize
        canvas.drawText(line1, padH, baselineY, titlePaint)
        baselineY += lineGap
        canvas.drawText(line2, padH, baselineY, coordPaint)
        baselineY += lineGap
        canvas.drawText(line3, padH, baselineY, metaPaint)

        return mutable
    }

    /**
     * Proportionally downscales high-resolution camera bitmaps to optimal disaster report resolution.
     */
    fun optimizeReportBitmap(source: Bitmap, maxDimension: Int = OPTIMAL_MAX_DIMENSION): Bitmap {
        val width = source.width
        val height = source.height
        val maxSide = max(width, height)

        if (maxSide <= maxDimension || width <= 0 || height <= 0) {
            return source
        }

        val scale = maxDimension.toFloat() / maxSide
        val matrix = Matrix().apply {
            postScale(scale, scale)
        }

        return Bitmap.createBitmap(
            source,
            0,
            0,
            width,
            height,
            matrix,
            true
        )
    }

    /**
     * Creates a persistent file in `filesDir/citizen_report_photos` for live camera capture.
     * Unlike `cacheDir`, Android will not spontaneously evict files in `filesDir`.
     */
    fun createPersistentReportPhotoFile(context: Context): File {
        val dir = File(context.filesDir, REPORT_PHOTOS_DIR).apply {
            if (!exists()) mkdirs()
        }
        return File(dir, "report_photo_${System.currentTimeMillis()}.jpg")
    }

    /**
     * Persists an optimized report bitmap into `filesDir/citizen_report_photos` and returns its `file://` URI string.
     */
    fun saveBitmapToPersistentReportFile(context: Context, bitmap: Bitmap): String? {
        return try {
            val file = createPersistentReportPhotoFile(context)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, OPTIMAL_JPEG_QUALITY, out)
            }
            if (file.exists() && file.length() > 0L) {
                Uri.fromFile(file).toString()
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Validates that raw encoded image bytes represent a complete, non-truncated JPEG, PNG, or WebP
     * payload so Android's BitmapFactory does not silently decode a partially cut-off/glitched bitmap.
     */
    fun isValidEncodedImageBytes(bytes: ByteArray): Boolean {
        if (bytes.size < 32) return false
        val b0 = bytes[0].toInt() and 0xFF
        val b1 = bytes[1].toInt() and 0xFF

        // JPEG: must start with SOI (FF D8) and contain EOI (FF D9) near the end
        if (b0 == 0xFF && b1 == 0xD8) {
            val scanStart = (bytes.size - 128).coerceAtLeast(2)
            for (i in bytes.size - 2 downTo scanStart) {
                if ((bytes[i].toInt() and 0xFF) == 0xFF && (bytes[i + 1].toInt() and 0xFF) == 0xD9) {
                    return true
                }
            }
            return false
        }

        // PNG: must start with 89 50 4E 47 and contain IEND trailer
        if (bytes.size >= 24 &&
            b0 == 0x89 && b1 == 0x50 &&
            (bytes[2].toInt() and 0xFF) == 0x4E &&
            (bytes[3].toInt() and 0xFF) == 0x47
        ) {
            val scanStart = (bytes.size - 64).coerceAtLeast(8)
            for (i in bytes.size - 4 downTo scanStart) {
                if (bytes[i] == 'I'.code.toByte() &&
                    bytes[i + 1] == 'E'.code.toByte() &&
                    bytes[i + 2] == 'N'.code.toByte() &&
                    bytes[i + 3] == 'D'.code.toByte()
                ) {
                    return true
                }
            }
            return false
        }

        // WebP: RIFF....WEBP and full declared payload length present
        if (bytes.size >= 16 &&
            bytes[0] == 'R'.code.toByte() && bytes[1] == 'I'.code.toByte() &&
            bytes[2] == 'F'.code.toByte() && bytes[3] == 'F'.code.toByte() &&
            bytes[8] == 'W'.code.toByte() && bytes[9] == 'E'.code.toByte() &&
            bytes[10] == 'B'.code.toByte() && bytes[11] == 'P'.code.toByte()
        ) {
            val riffSize = (bytes[4].toInt() and 0xFF) or
                ((bytes[5].toInt() and 0xFF) shl 8) or
                ((bytes[6].toInt() and 0xFF) shl 16) or
                ((bytes[7].toInt() and 0xFF) shl 24)
            return bytes.size >= riffSize + 8
        }

        return false
    }

    private fun decodeValidatedImageBytes(bytes: ByteArray): Bitmap? {
        if (!isValidEncodedImageBytes(bytes)) return null
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
        if (decoded.width < 16 || decoded.height < 16) return null
        return decoded
    }

    /**
     * Decodes a captured photo file safely with subsampling (preventing OOM on high-MP sensors),
     * validates encoded byte completeness to prevent truncated glitch renders,
     * and applies EXIF orientation correction so portrait/landscape photos appear upright.
     */
    fun decodeCapturedFileSafely(
        filePath: String,
        maxDimension: Int = OPTIMAL_MAX_DIMENSION
    ): Bitmap? {
        val file = File(filePath)
        if (!file.exists() || file.length() < 32L) return null

        return try {
            val fileBytes = file.readBytes()
            if (!isValidEncodedImageBytes(fileBytes)) return null

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(fileBytes, 0, fileBytes.size, bounds)
            val srcW = bounds.outWidth
            val srcH = bounds.outHeight
            if (srcW < 16 || srcH < 16) return null

            var sampleSize = 1
            val target = maxDimension * 2
            while ((srcW / sampleSize) > target || (srcH / sampleSize) > target) {
                sampleSize *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val decoded = BitmapFactory.decodeByteArray(fileBytes, 0, fileBytes.size, decodeOpts) ?: return null
            if (decoded.width < 16 || decoded.height < 16) return null
            val rotated = applyExifRotationIfNeeded(file.absolutePath, decoded)
            optimizeReportBitmap(rotated, maxDimension)
        } catch (_: Exception) {
            null
        }
    }

    private fun applyExifRotationIfNeeded(filePath: String, bitmap: Bitmap): Bitmap {
        return try {
            val exif = ExifInterface(filePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            val degrees = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
            if (degrees == 0f) {
                bitmap
            } else {
                val matrix = Matrix().apply { postRotate(degrees) }
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            }
        } catch (_: Exception) {
            bitmap
        }
    }

    /**
     * Checks whether a URI points strictly to local device storage (`file://`, `/path`, `content://`)
     * that cannot be accessed by other officers' devices across Cloud Firestore.
     */
    fun isDeviceLocalPhotoPath(uri: String?): Boolean {
        val trimmed = uri?.trim().orEmpty()
        if (trimmed.isBlank()) return false
        return trimmed.startsWith("file://", ignoreCase = true) ||
            trimmed.startsWith("/") ||
            trimmed.startsWith("content://", ignoreCase = true)
    }

    /**
     * Checks whether a photo URI is portable across devices (HTTPS URL, inline Base64 Data URI, or bundled drawable resource).
     */
    fun isCloudPortablePhotoUri(uri: String?): Boolean {
        val trimmed = uri?.trim().orEmpty()
        if (trimmed.isBlank()) return false
        return trimmed.startsWith("https://", ignoreCase = true) ||
            trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("data:image", ignoreCase = true) ||
            trimmed.startsWith("android.resource://", ignoreCase = true)
    }

    /**
     * Decodes a local file path, `content://` URI, or `data:image/...;base64,...` Data URI into a `Bitmap`.
     * Returns null for remote `http(s)://` URLs (which Coil fetches directly) or unreadable files.
     */
    fun decodePhotoSourceToBitmap(photoSource: String?, context: Context? = null): Bitmap? {
        if (photoSource.isNullOrBlank()) return null
        val trimmed = photoSource.trim()
        if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            com.example.data.api.SupabaseClient.getCachedPhotoBitmap(trimmed)?.let { return it }
            val fallbackDataUri = getInlineDataUriForUrl(trimmed)
            if (!fallbackDataUri.isNullOrBlank()) {
                return decodePhotoSourceToBitmap(fallbackDataUri, context)
            }
            return null
        }
        if (trimmed.startsWith("android.resource://", ignoreCase = true)) {
            if (context == null) return null
            return try {
                val parsed = Uri.parse(trimmed)
                context.contentResolver.openInputStream(parsed)?.use { input ->
                    val bytes = input.readBytes()
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.let { optimizeReportBitmap(it) }
                }
            } catch (_: Exception) {
                null
            }
        }

        return try {
            when {
                trimmed.startsWith("data:image", ignoreCase = true) ||
                    trimmed.contains("base64,", ignoreCase = true) -> {
                    val base64Part = trimmed.substringAfter("base64,", trimmed)
                        .replace("\n", "")
                        .replace("\r", "")
                        .trim()
                    if (base64Part.isBlank()) return null
                    val bytes = Base64.decode(base64Part, Base64.DEFAULT)
                    decodeValidatedImageBytes(bytes)
                }
                trimmed.startsWith("file://", ignoreCase = true) -> {
                    val path = Uri.parse(trimmed).path ?: return null
                    decodeCapturedFileSafely(path)
                }
                trimmed.startsWith("/") -> {
                    decodeCapturedFileSafely(trimmed)
                }
                trimmed.startsWith("content://", ignoreCase = true) && context != null -> {
                    val parsed = Uri.parse(trimmed)
                    context.contentResolver.openInputStream(parsed)?.use { input ->
                        val bytes = input.readBytes()
                        decodeValidatedImageBytes(bytes)?.let { optimizeReportBitmap(it) }
                    }
                }
                trimmed.length >= 64 && !trimmed.contains("://") -> {
                    val bytes = Base64.decode(trimmed, Base64.DEFAULT)
                    decodeValidatedImageBytes(bytes)
                }
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Compresses a `Bitmap` into an inline JPEG Data URI (`data:image/jpeg;base64,...`)
     * guaranteed to fit inside [maxChars] so it syncs reliably inside a Cloud Firestore document.
     */
    fun encodeBitmapToCloudDataUri(
        source: Bitmap,
        maxChars: Int = MAX_CLOUD_PHOTO_DATA_URI_CHARS
    ): String? {
        if (source.width <= 0 || source.height <= 0) return null

        val ladder = listOf(
            960 to 78,
            960 to 68,
            840 to 62,
            720 to 56,
            640 to 50,
            512 to 44,
            384 to 38
        )

        val prefix = "data:image/jpeg;base64,"
        for ((maxDim, quality) in ladder) {
            val scaled = optimizeReportBitmap(source, maxDimension = maxDim)
            val baos = ByteArrayOutputStream()
            val ok = scaled.compress(Bitmap.CompressFormat.JPEG, quality, baos)
            if (!ok) continue
            val bytes = baos.toByteArray()
            if (bytes.isEmpty()) continue
            val candidate = prefix + Base64.encodeToString(bytes, Base64.NO_WRAP)
            if (candidate.length <= maxChars) {
                return candidate
            }
        }
        return null
    }

    /**
     * Converts any raw photo URI into a cloud-portable format suitable for multi-device Firestore sync:
     * 1. Existing `https://` / `http://` -> normalized `https://`.
     * 2. Existing `data:image/...;base64,...` -> kept if within limit, otherwise recompressed to fit.
     * 3. Local `file://` or `/path` -> attempts [supabaseUploader] first; if unavailable or fails,
     *    falls back immediately to an inline compressed JPEG Data URI (`data:image/jpeg;base64,...`)
     *    so photo evidence is NEVER stripped from the citizen report.
     */
    suspend fun prepareCloudPortablePhotoUri(
        rawPhotoUri: String?,
        supabaseUploader: suspend (Bitmap) -> String? = { null }
    ): String? {
        val trimmed = rawPhotoUri?.trim().orEmpty()
        if (trimmed.isBlank()) return null

        if (trimmed.startsWith("https://", ignoreCase = true)) {
            return trimmed.takeIf { it.length <= FIRESTORE_PHOTO_URI_MAX_CHARS }
        }
        if (trimmed.startsWith("http://", ignoreCase = true)) {
            val upgraded = "https://" + trimmed.substring(7)
            return upgraded.takeIf { it.length <= FIRESTORE_PHOTO_URI_MAX_CHARS }
        }
        if (trimmed.startsWith("android.resource://", ignoreCase = true)) {
            return trimmed.takeIf { it.length <= 1024 }
        }

        if (trimmed.startsWith("data:image", ignoreCase = true) ||
            trimmed.contains("base64,", ignoreCase = true)
        ) {
            val header = trimmed.substringBefore("base64,", "data:image/jpeg;")
            val cleanBase64 = trimmed.substringAfter("base64,", "")
                .replace("\n", "")
                .replace("\r", "")
                .replace(" ", "")
                .trim()
            if (cleanBase64.isBlank()) return null
            val normalizedDataUri = "${header}base64,$cleanBase64"
            if (normalizedDataUri.length <= MAX_CLOUD_PHOTO_DATA_URI_CHARS) {
                return normalizedDataUri
            }
            val decoded = decodePhotoSourceToBitmap(normalizedDataUri) ?: return null
            return encodeBitmapToCloudDataUri(decoded)
        }

        if (isDeviceLocalPhotoPath(trimmed)) {
            val bitmap = decodePhotoSourceToBitmap(trimmed) ?: return null
            val inlineBackup = encodeBitmapToCloudDataUri(bitmap)
            val remoteUrl = try {
                supabaseUploader(bitmap)?.trim()?.takeIf {
                    it.startsWith("https://", ignoreCase = true) ||
                        it.startsWith("http://", ignoreCase = true)
                }
            } catch (_: Exception) {
                null
            }
            if (!remoteUrl.isNullOrBlank()) {
                val normalized = if (remoteUrl.startsWith("http://", ignoreCase = true)) {
                    "https://" + remoteUrl.substring(7)
                } else {
                    remoteUrl
                }
                rememberInlineDataUriForUrl(normalized, inlineBackup)
                com.example.data.api.SupabaseClient.cachePhotoBitmap(normalized, bitmap)
                return normalized
            }
            return inlineBackup
        }

        return null
    }

    /**
     * Resolves a citizen report photo for Cloud Firestore upload with strict offline & atomicity guarantees:
     * 1. No photo attached -> [CloudPhotoResolution.NoPhoto]
     * 2. Already an HTTPS URL or portable Data URI -> [CloudPhotoResolution.ReadyForCloud]
     * 3. Device-local watermarked photo:
     *    - Checks network connectivity first; if offline -> [CloudPhotoResolution.DeferredOffline] (NEVER sends text-only!)
     *    - Uploads 100% to Cloud Storage (Supabase / Firebase) and awaits `downloadUrl`
     *    - Caches an inline JPEG Data URI backup for cross-device Firestore fallback
     *    - If external bucket is unavailable/unconfigured while online -> tries Supabase Storage,
     *      then falls back to compact inline JPEG Data URI (<= 300 KB) so photo is STILL guaranteed attached.
     *    - If all attempts fail -> [CloudPhotoResolution.DeferredOffline] (NEVER drops photo!)
     */
    suspend fun resolveCloudPhotoForUpload(
        rawPhotoUri: String?,
        reportId: String,
        context: Context? = null,
        firebaseUploader: suspend (Bitmap) -> PhotoUploadOutcome = { bmp ->
            FirebaseStoragePhotoUploader.uploadReportPhoto(bmp, reportId, context)
        },
        supabaseUploader: suspend (Bitmap) -> String? = { null }
    ): CloudPhotoResolution {
        val trimmed = rawPhotoUri?.trim().orEmpty()
        if (trimmed.isBlank()) return CloudPhotoResolution.NoPhoto

        if (trimmed.startsWith("https://", ignoreCase = true)) {
            return if (trimmed.length <= FIRESTORE_PHOTO_URI_MAX_CHARS) {
                CloudPhotoResolution.ReadyForCloud(trimmed)
            } else {
                CloudPhotoResolution.DeferredOffline("URL foto melebihi batas karakter Firestore")
            }
        }
        if (trimmed.startsWith("http://", ignoreCase = true)) {
            val upgraded = "https://" + trimmed.substring(7)
            return if (upgraded.length <= FIRESTORE_PHOTO_URI_MAX_CHARS) {
                CloudPhotoResolution.ReadyForCloud(upgraded)
            } else {
                CloudPhotoResolution.DeferredOffline("URL foto melebihi batas karakter Firestore")
            }
        }
        if (trimmed.startsWith("android.resource://", ignoreCase = true)) {
            return if (trimmed.length <= 1024) {
                CloudPhotoResolution.ReadyForCloud(trimmed)
            } else {
                CloudPhotoResolution.DeferredOffline("URI resource foto terlalu panjang")
            }
        }

        if (trimmed.startsWith("data:image", ignoreCase = true) ||
            trimmed.contains("base64,", ignoreCase = true)
        ) {
            if (!FirebaseStoragePhotoUploader.isNetworkOnline(context)) {
                return CloudPhotoResolution.DeferredOffline("Perangkat sedang offline")
            }
            val decoded = decodePhotoSourceToBitmap(trimmed, context)
            val recompressedInline = decoded?.let { encodeBitmapToCloudDataUri(it, MAX_CLOUD_PHOTO_DATA_URI_CHARS) }
            rememberInlineDataUriForReport(reportId, recompressedInline ?: trimmed)
            if (decoded != null) {
                when (val fbOutcome = firebaseUploader(decoded)) {
                    is PhotoUploadOutcome.Success -> {
                        rememberInlineDataUriForUrl(fbOutcome.downloadUrl, recompressedInline ?: trimmed)
                        com.example.data.api.SupabaseClient.cachePhotoBitmap(fbOutcome.downloadUrl, decoded)
                        return CloudPhotoResolution.ReadyForCloud(fbOutcome.downloadUrl)
                    }
                    is PhotoUploadOutcome.NetworkOffline -> return CloudPhotoResolution.DeferredOffline(fbOutcome.reason)
                    is PhotoUploadOutcome.StorageUnavailable -> {
                        val sbUrl = runCatching { supabaseUploader(decoded) }.getOrNull()?.trim()
                        if (!sbUrl.isNullOrBlank() && (sbUrl.startsWith("https://", true) || sbUrl.startsWith("http://", true))) {
                            val normalizedUrl = if (sbUrl.startsWith("http://", true)) "https://" + sbUrl.substring(7) else sbUrl
                            rememberInlineDataUriForUrl(normalizedUrl, recompressedInline ?: trimmed)
                            com.example.data.api.SupabaseClient.cachePhotoBitmap(normalizedUrl, decoded)
                            return CloudPhotoResolution.ReadyForCloud(normalizedUrl)
                        }
                    }
                }
            }
            val header = trimmed.substringBefore("base64,", "data:image/jpeg;")
            val cleanBase64 = trimmed.substringAfter("base64,", "")
                .replace("\n", "")
                .replace("\r", "")
                .replace(" ", "")
                .trim()
            if (cleanBase64.isBlank()) return CloudPhotoResolution.DeferredOffline("Data foto kosong")
            val normalizedDataUri = "${header}base64,$cleanBase64"
            if (normalizedDataUri.length <= MAX_CLOUD_PHOTO_DATA_URI_CHARS) {
                rememberInlineDataUriForReport(reportId, normalizedDataUri)
                return CloudPhotoResolution.ReadyForCloud(normalizedDataUri)
            }
            return if (!recompressedInline.isNullOrBlank()) {
                CloudPhotoResolution.ReadyForCloud(recompressedInline)
            } else {
                CloudPhotoResolution.DeferredOffline("Gagal mengompresi foto untuk unggahan cloud")
            }
        }

        if (isDeviceLocalPhotoPath(trimmed)) {
            if (!FirebaseStoragePhotoUploader.isNetworkOnline(context)) {
                return CloudPhotoResolution.DeferredOffline(
                    com.example.data.local.FirestoreHelper.OFFLINE_REPORT_SAVED_MESSAGE
                )
            }
            val bitmap = decodePhotoSourceToBitmap(trimmed, context)
                ?: return CloudPhotoResolution.DeferredOffline("File foto lokal tidak ditemukan atau rusak")

            val inlineDataUri = encodeBitmapToCloudDataUri(bitmap, MAX_CLOUD_PHOTO_DATA_URI_CHARS)
            rememberInlineDataUriForReport(reportId, inlineDataUri)

            val fbOutcome = try {
                firebaseUploader(bitmap)
            } catch (e: Throwable) {
                FirebaseStoragePhotoUploader.classifyUploadException(reportId, e)
            }

            when (fbOutcome) {
                is PhotoUploadOutcome.Success -> {
                    rememberInlineDataUriForUrl(fbOutcome.downloadUrl, inlineDataUri)
                    com.example.data.api.SupabaseClient.cachePhotoBitmap(fbOutcome.downloadUrl, bitmap)
                    return CloudPhotoResolution.ReadyForCloud(fbOutcome.downloadUrl)
                }
                is PhotoUploadOutcome.NetworkOffline -> {
                    return CloudPhotoResolution.DeferredOffline(fbOutcome.reason)
                }
                is PhotoUploadOutcome.StorageUnavailable -> {
                    val sbUrl = try {
                        supabaseUploader(bitmap)?.trim()?.takeIf {
                            it.startsWith("https://", ignoreCase = true) ||
                                it.startsWith("http://", ignoreCase = true)
                        }
                    } catch (_: Exception) {
                        null
                    }
                    if (!sbUrl.isNullOrBlank()) {
                        val normalized = if (sbUrl.startsWith("http://", ignoreCase = true)) {
                            "https://" + sbUrl.substring(7)
                        } else {
                            sbUrl
                        }
                        rememberInlineDataUriForUrl(normalized, inlineDataUri)
                        com.example.data.api.SupabaseClient.cachePhotoBitmap(normalized, bitmap)
                        return CloudPhotoResolution.ReadyForCloud(normalized)
                    }
                    return if (!inlineDataUri.isNullOrBlank()) {
                        CloudPhotoResolution.ReadyForCloud(inlineDataUri)
                    } else {
                        CloudPhotoResolution.DeferredOffline("Gagal memproses foto bukti untuk sinkronisasi cloud")
                    }
                }
            }
        }

        return CloudPhotoResolution.DeferredOffline("Format URI foto tidak dikenali")
    }

    /**
     * Selects the highest-reliability photo URI when merging remote and local records:
     * prefers cloud-portable URIs (`https://`, `data:image/...`) over device-only paths,
     * and preserves an existing local readable photo if the remote record still lacks one.
     */
    fun selectBestPhotoUri(primary: String?, fallback: String?): String? {
        val p = primary?.trim()?.takeIf { it.isNotBlank() }
        val f = fallback?.trim()?.takeIf { it.isNotBlank() }

        if (p != null && isCloudPortablePhotoUri(p)) return p
        if (f != null && isCloudPortablePhotoUri(f)) return f

        if (p != null && isReadableLocalPhotoFile(p)) return p
        if (f != null && isReadableLocalPhotoFile(f)) return f

        return p ?: f
    }

    fun isReadableLocalPhotoFile(uri: String?): Boolean {
        val trimmed = uri?.trim().orEmpty()
        if (trimmed.isBlank()) return false
        return try {
            val path = when {
                trimmed.startsWith("file://", ignoreCase = true) -> Uri.parse(trimmed).path
                trimmed.startsWith("/") -> trimmed
                else -> null
            } ?: return false
            val f = File(path)
            f.exists() && f.length() > 0L
        } catch (_: Exception) {
            false
        }
    }
}
