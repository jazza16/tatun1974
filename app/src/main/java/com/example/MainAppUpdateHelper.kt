package com.example

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class GitHubReleaseUpdate(
    val tagName: String,
    val latestVersion: String,
    val htmlUrl: String,
    val releaseNotes: String = ""
)

private val githubUpdateHttpClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .build()
}

/**
 * Mengambil versi aplikasi yang sedang terinstal menggunakan PackageManager (setara PackageInfo.fromPlatform()).
 */
fun getInstalledAppVersion(context: Context): String {
    return try {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        packageInfo.versionName?.trim()?.removePrefix("v")?.removePrefix("V")
            ?.takeIf { it.isNotBlank() }
            ?: BuildConfig.VERSION_NAME
    } catch (e: Exception) {
        Log.d("GitHubUpdateChecker", "Fallback menggunakan BuildConfig.VERSION_NAME: ${e.message}")
        BuildConfig.VERSION_NAME
    }
}

/**
 * Mem-parsing JSON respons dari https://api.github.com/repos/jazza16/tatun1974/releases/latest
 * untuk mengekstrak `tag_name` (misal: v1.0.7 -> 1.0.7) dan `html_url`.
 */
fun parseGitHubLatestReleaseJson(bodyStr: String): GitHubReleaseUpdate? {
    return try {
        if (bodyStr.isBlank()) return null
        val json = JSONObject(bodyStr)
        val rawTag = json.optString("tag_name", "").trim()
        val cleanVersion = rawTag.removePrefix("v").removePrefix("V").trim()
        if (cleanVersion.isBlank()) return null

        val htmlUrl = json.optString(
            "html_url",
            "https://github.com/jazza16/tatun1974/releases/latest"
        ).trim().ifBlank { "https://github.com/jazza16/tatun1974/releases/latest" }
        val notes = json.optString("body", "").trim()

        GitHubReleaseUpdate(
            tagName = rawTag,
            latestVersion = cleanVersion,
            htmlUrl = htmlUrl,
            releaseNotes = notes
        )
    } catch (e: Exception) {
        Log.d("GitHubUpdateChecker", "Gagal mem-parsing respons JSON GitHub Releases: ${e.message}")
        null
    }
}

/**
 * Memeriksa pembaruan aplikasi secara otomatis (auto-check update) melalui GitHub Releases API
 * khusus untuk repositori `jazza16/tatun1974` (`https://api.github.com/repos/jazza16/tatun1974/releases/latest`).
 * Jika versi GitHub lebih baru dari versi aplikasi yang terinstal saat ini, menampilkan AlertDialog
 * "Versi baru tersedia" dan membuka `html_url` di browser default HP tanpa menginstal APK secara otomatis.
 */
fun checkForAppUpdates(context: Context) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val currentVersion = getInstalledAppVersion(context)
            val request = Request.Builder()
                .url("https://api.github.com/repos/jazza16/tatun1974/releases/latest")
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "SIPAGAR-KITA-Android/$currentVersion")
                .get()
                .build()

            val releaseUpdate = githubUpdateHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val bodyStr = response.body?.string().orEmpty()
                parseGitHubLatestReleaseJson(bodyStr)
            } ?: return@launch

            val isUpdateAvailable = com.example.util.AppVersionHelper.isVersionNewer(
                currentVersion = currentVersion,
                targetVersion = releaseUpdate.latestVersion
            )

            if (isUpdateAvailable) {
                withContext(Dispatchers.Main) {
                    showUpdateDialog(context, releaseUpdate)
                }
            }
        } catch (t: Throwable) {
            // Jangan tampilkan pesan error yang mengganggu jika tidak ada koneksi; cukup log di console.
            Log.d("GitHubUpdateChecker", "Pemeriksaan pembaruan GitHub dilewati: ${t.message}")
        }
    }
}

/**
 * Menampilkan AlertDialog dengan pesan "Versi baru tersedia".
 * Jika pengguna menekan "Unduh", membuka `html_url` (link halaman rilis GitHub) di browser default HP.
 */
fun showUpdateDialog(context: Context, update: GitHubReleaseUpdate? = null) {
    try {
        if (context is android.app.Activity && (context.isFinishing || context.isDestroyed)) return
        val latestVer = update?.latestVersion?.takeIf { it.isNotBlank() } ?: "terbaru"
        val htmlUrl = update?.htmlUrl?.takeIf { it.isNotBlank() }
            ?: "https://github.com/jazza16/tatun1974/releases/latest"
        val notes = update?.releaseNotes?.takeIf { it.isNotBlank() }
        val messageText = if (notes != null) {
            "Versi baru tersedia (v$latestVer).\n\n$notes"
        } else {
            "Versi baru tersedia (v$latestVer). Silakan unduh pembaruan melalui halaman rilis resmi GitHub."
        }

        AlertDialog.Builder(context)
            .setTitle("Versi baru tersedia")
            .setMessage(messageText)
            .setPositiveButton("Unduh") { dialog, _ ->
                dialog.dismiss()
                openGitHubReleaseUrlInBrowser(context, htmlUrl)
            }
            .setNegativeButton("Nanti") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    } catch (e: Exception) {
        Log.d("GitHubUpdateChecker", "Gagal menampilkan dialog pembaruan: ${e.message}")
    }
}

fun openGitHubReleaseUrlInBrowser(context: Context, htmlUrl: String) {
    try {
        val safeUrl = htmlUrl.trim().ifBlank { "https://github.com/jazza16/tatun1974/releases/latest" }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(safeUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Log.d("GitHubUpdateChecker", "Gagal membuka browser untuk URL rilis ($htmlUrl): ${e.message}")
    }
}
