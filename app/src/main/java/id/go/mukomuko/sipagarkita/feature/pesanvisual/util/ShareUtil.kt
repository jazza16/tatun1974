// FILE: feature/pesanvisual/util/ShareUtil.kt
package id.go.mukomuko.sipagarkita.feature.pesanvisual.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object ShareUtil {
    private const val AUTHORITY_SUFFIX = ".fileprovider"

    fun sharePoster(
        context: Context,
        bitmap: Bitmap,
        caption: String,
        subject: String,
        fileName: String
    ): Boolean = runCatching {
        val sharedDir = File(context.cacheDir, "shared_images").apply {
            if (!exists()) mkdirs()
        }

        // Cleanup file lama > 1 hari
        val cutoff = System.currentTimeMillis() - (24 * 60 * 60 * 1000L)
        sharedDir.listFiles()?.forEach { file ->
            if (file.lastModified() < cutoff) {
                file.delete()
            }
        }

        // Tulis bitmap ke shared_images/[fileName] JPEG quality 90
        val imageFile = File(sharedDir, fileName)
        FileOutputStream(imageFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            out.flush()
        }

        val authority = "${context.packageName}$AUTHORITY_SUFFIX"
        val uri = FileProvider.getUriForFile(context, authority, imageFile)

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, caption)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val resInfoList = context.packageManager.queryIntentActivities(shareIntent, 0)
        if (resInfoList.isEmpty()) {
            return false
        }

        // Grant read URI permission to target apps
        for (resolveInfo in resInfoList) {
            val pkg = resolveInfo.activityInfo.packageName
            context.grantUriPermission(pkg, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(shareIntent, "Bagikan pesan kesiapsiagaan").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(chooser)
        true
    }.getOrDefault(false)

    fun shareTextOnly(
        context: Context,
        caption: String,
        subject: String
    ): Boolean = runCatching {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, caption)
            putExtra(Intent.EXTRA_SUBJECT, subject)
        }

        val chooser = Intent.createChooser(shareIntent, "Bagikan pesan kesiapsiagaan").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(chooser)
        true
    }.getOrDefault(false)
}
