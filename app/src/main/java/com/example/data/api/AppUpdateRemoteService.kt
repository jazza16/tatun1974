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

    const val GITHUB_RELEASES_LATEST_API_URL = "https://api.github.com/repos/jazza16/tatun1974/releases/latest"
    const val DEFAULT_GITHUB_RELEASE_PAGE_URL = "https://github.com/jazza16/tatun1974/releases/latest"

    // Remote endpoints with failover
    private val REMOTE_MANIFEST_URLS = listOf(
        GITHUB_RELEASES_LATEST_API_URL,
        "https://raw.githubusercontent.com/takajuikgue/cuaca-mukomuko/main/version.json",
        "https://raw.githubusercontent.com/tathoenz/sipagar-kita/main/app-version.json"
    )

    private const val FALLBACK_TRUSTED_DOWNLOAD_URL = "https://t.me/takajuikgue"
    private val TRUSTED_DOWNLOAD_HOSTS = setOf(
        "github.com",
        "api.github.com",
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
     * Parses a GitHub Releases `/releases/latest` JSON payload (`tag_name` and `html_url`).
     */
    fun parseGitHubReleaseJson(jsonBody: String): RemoteReleaseInfo? {
        return try {
            if (jsonBody.isBlank()) return null
            val json = JSONObject(jsonBody)
            val rawTag = json.optString("tag_name", "").trim()
            val cleanVer = rawTag.removePrefix("v").removePrefix("V").trim()
            if (cleanVer.isBlank()) return null

            val htmlUrl = json.optString("html_url", DEFAULT_GITHUB_RELEASE_PAGE_URL)
                .trim()
                .ifBlank { DEFAULT_GITHUB_RELEASE_PAGE_URL }
            val publishedAt = json.optString("published_at", "").take(10).ifBlank { "Rilis GitHub Terbaru" }
            val bodyNotes = json.optString("body", "").trim()
            val highlights = bodyNotes
                .lines()
                .map { it.trim().removePrefix("-").removePrefix("*").removePrefix("•").trim() }
                .filter { it.isNotBlank() }
                .ifEmpty { listOf("Pembaruan aplikasi versi v$cleanVer tersedia di GitHub Releases.") }

            RemoteReleaseInfo(
                latestVersion = cleanVer,
                versionCode = 0,
                releaseDate = publishedAt,
                highlights = highlights,
                downloadUrl = validateDownloadUrl(htmlUrl),
                packageSizeMb = 16.2f
            )
        } catch (e: Exception) {
            Log.d(TAG, "Failed to parse GitHub release JSON: ${e.message}")
            null
        }
    }

    /**
     * Attempts to fetch the latest release manifest from GitHub Releases API (jazza16/tatun1974)
     * or fallback remote endpoints. Returns null if all endpoints fail or device is offline.
     */
    suspend fun fetchLatestRelease(): RemoteReleaseInfo? = withContext(Dispatchers.IO) {
        for (url in REMOTE_MANIFEST_URLS) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("Accept", "application/vnd.github+json, application/json")
                    .header("User-Agent", "SIPAGAR-KITA-Android/${BuildConfig.VERSION_NAME}")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        if (!body.isNullOrBlank()) {
                            // 1. Check if response is GitHub Releases API format (contains tag_name)
                            val githubInfo = parseGitHubReleaseJson(body)
                            if (githubInfo != null) {
                                Log.i(TAG, "Successfully fetched GitHub release: v${githubInfo.latestVersion} from $url")
                                return@withContext githubInfo
                            }

                            // 2. Fallback manifest JSON format
                            val json = JSONObject(body)
                            val ver = json.optString("latestVersion", json.optString("version", ""))
                                .removePrefix("v")
                                .removePrefix("V")
                                .trim()
                            if (ver.isNotBlank()) {
                                val code = json.optInt("versionCode", 5)
                                val date = json.optString("releaseDate", "Oktober 2026")
                                val dlUrl = validateDownloadUrl(json.optString("downloadUrl", DEFAULT_GITHUB_RELEASE_PAGE_URL))
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
                                    highlights = highlightsList.ifEmpty { com.example.model.LATEST_RELEASE_CHANGELOG.highlights },
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
