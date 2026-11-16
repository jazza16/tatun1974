package com.example.data.api

import android.util.Log
import com.example.BuildConfig
import com.example.data.local.AppUpdatePreferences
import com.example.model.VersionChangelog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Remote Data Source for Over-The-Air (OTA) and APK update checks.
 * Queries online endpoints with multi-tier failover and graceful fallback to
 * embedded release catalog when network is offline or unreachable.
 */
object AppUpdateRemoteService {
    private const val TAG = "AppUpdateRemoteService"

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(6, TimeUnit.SECONDS)
            .build()
    }

    data class RemoteReleaseInfo(
        val latestVersion: String,
        val versionCode: Int,
        val releaseDate: String,
        val highlights: List<String>,
        val downloadUrl: String,
        val packageSizeMb: Float
    )

    // Remote endpoints with failover
    private val REMOTE_MANIFEST_URLS = listOf(
        "https://raw.githubusercontent.com/takajuikgue/cuaca-mukomuko/main/version.json",
        "https://raw.githubusercontent.com/tathoenz/sipagar-kita/main/app-version.json"
    )

    private const val FALLBACK_TRUSTED_DOWNLOAD_URL = "https://t.me/takajuikgue"
    private val TRUSTED_DOWNLOAD_HOSTS = setOf(
        "github.com",
        "raw.githubusercontent.com",
        "objects.githubusercontent.com",
        "release-assets.githubusercontent.com",
        "t.me",
        "play.google.com"
    )

    fun validateDownloadUrl(rawUrl: String?): String {
        val candidate = rawUrl?.trim().orEmpty()
        if (candidate.isBlank()) return FALLBACK_TRUSTED_DOWNLOAD_URL
        return try {
            val uri = java.net.URI(candidate)
            val scheme = uri.scheme?.lowercase()
            val host = uri.host?.lowercase()
            if (scheme == "https" && !host.isNullOrBlank() && TRUSTED_DOWNLOAD_HOSTS.any { host == it || host.endsWith(".$it") }) {
                candidate
            } else {
                Log.w(TAG, "Untrusted OTA downloadUrl rejected ($host); falling back to official channel.")
                FALLBACK_TRUSTED_DOWNLOAD_URL
            }
        } catch (_: Exception) {
            FALLBACK_TRUSTED_DOWNLOAD_URL
        }
    }

    /**
     * Attempts to fetch the latest release manifest from remote server endpoints.
     * Returns null if all endpoints fail or device is offline (calling code then falls back
     * to the app's compiled catalog).
     */
    suspend fun fetchLatestRelease(): RemoteReleaseInfo? = withContext(Dispatchers.IO) {
        for (url in REMOTE_MANIFEST_URLS) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("Accept", "application/json")
                    .header("User-Agent", "SIPAGAR-KITA-Android/${BuildConfig.VERSION_NAME}")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        if (!body.isNullOrBlank()) {
                            val json = JSONObject(body)
                            val ver = json.optString("latestVersion", json.optString("version", ""))
                            if (ver.isNotBlank()) {
                                val code = json.optInt("versionCode", 5)
                                val date = json.optString("releaseDate", "Oktober 2026")
                                val dlUrl = validateDownloadUrl(json.optString("downloadUrl", FALLBACK_TRUSTED_DOWNLOAD_URL))
                                val size = json.optDouble("packageSizeMb", 16.2).toFloat()
                                val highlightsList = mutableListOf<String>()
                                val highlightsArr = json.optJSONArray("highlights")
                                if (highlightsArr != null) {
                                    for (i in 0 until highlightsArr.length()) {
                                        highlightsList.add(highlightsArr.getString(i))
                                    }
                                }
                                Log.i(TAG, "Successfully fetched remote release manifest: v$ver from $url")
                                return@withContext RemoteReleaseInfo(
                                    latestVersion = ver,
                                    versionCode = code,
                                    releaseDate = date,
                                    highlights = highlightsList.ifEmpty { com.example.model.INITIAL_RELEASE_CHANGELOG.highlights },
                                    downloadUrl = dlUrl,
                                    packageSizeMb = size
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "Failed to fetch update manifest from $url: ${e.message}")
            }
        }
        null
    }
}
