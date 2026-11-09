package com.example.data.api

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Model representing active fire / hotspot detected by NASA FIRMS satellite sensors (VIIRS / MODIS).
 */
data class NasaFirmsHotspot(
    val latitude: Double,
    val longitude: Double,
    val brightness: Double,
    val scan: Double,
    val track: Double,
    val acqDate: String,
    val acqTime: String,
    val satellite: String,
    val confidence: String,
    val frp: Double,
    val dayNight: String
)

/**
 * Service to query NASA FIRMS (Fire Information for Resource Management System) API for real-time
 * Karhutla (forest & plantation fire) hotspot detection across Mukomuko Regency coordinates.
 */
object NasaFirmsHotspotService {
    private const val TAG = "NasaFirmsService"
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    /**
     * Mukomuko bounding box:
     * Min Lon: 101.0, Min Lat: -3.3, Max Lon: 101.8, Max Lat: -2.0
     */
    private const val MUKOMUKO_AREA = "101.0,-3.3,101.8,-2.0"

    fun getFirmsMapKey(): String {
        return try {
            BuildConfig.FIRMS_MAP_KEY
        } catch (_: Exception) {
            ""
        }
    }

    suspend fun fetchActiveHotspots(): List<NasaFirmsHotspot> = withContext(Dispatchers.IO) {
        val key = getFirmsMapKey()
        if (key.isBlank() || key == "YOUR_FIRMS_MAP_KEY") {
            Log.d(TAG, "NASA FIRMS map key is not configured or placeholder.")
            return@withContext emptyList()
        }

        try {
            // NASA FIRMS API returns CSV format for area queries
            // Source: VIIRS_SNPP_NRT (Suomi NPP 375m active thermal hotspots)
            val url = "https://firms.modaps.eosdis.nasa.gov/api/area/csv/$key/VIIRS_SNPP_NRT/$MUKOMUKO_AREA/1"
            val request = Request.Builder()
                .url(url)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "NASA FIRMS API returned code: ${response.code}")
                    return@withContext emptyList()
                }

                val csv = response.body?.string() ?: return@withContext emptyList()
                parseCsv(csv)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch NASA FIRMS active hotspots: ${e.message}")
            emptyList()
        }
    }

    private fun parseCsv(csv: String): List<NasaFirmsHotspot> {
        val lines = csv.lines().filter { it.isNotBlank() }
        if (lines.size <= 1) return emptyList()

        val results = mutableListOf<NasaFirmsHotspot>()
        for (i in 1 until lines.size) {
            val tokens = lines[i].split(",")
            if (tokens.size >= 10) {
                try {
                    val lat = tokens[0].trim().toDoubleOrNull() ?: continue
                    val lon = tokens[1].trim().toDoubleOrNull() ?: continue
                    val brightness = tokens.getOrNull(2)?.trim()?.toDoubleOrNull() ?: 0.0
                    val scan = tokens.getOrNull(3)?.trim()?.toDoubleOrNull() ?: 0.0
                    val track = tokens.getOrNull(4)?.trim()?.toDoubleOrNull() ?: 0.0
                    val acqDate = tokens.getOrNull(5)?.trim() ?: ""
                    val acqTime = tokens.getOrNull(6)?.trim() ?: ""
                    val satellite = tokens.getOrNull(7)?.trim() ?: "VIIRS"
                    val confidence = tokens.getOrNull(8)?.trim() ?: "nominal"
                    val frp = tokens.getOrNull(10)?.trim()?.toDoubleOrNull() ?: 0.0
                    val dayNight = tokens.getOrNull(11)?.trim() ?: "D"

                    results.add(
                        NasaFirmsHotspot(
                            latitude = lat,
                            longitude = lon,
                            brightness = brightness,
                            scan = scan,
                            track = track,
                            acqDate = acqDate,
                            acqTime = acqTime,
                            satellite = satellite,
                            confidence = confidence,
                            frp = frp,
                            dayNight = dayNight
                        )
                    )
                } catch (_: Exception) {
                    // skip malformed line
                }
            }
        }
        return results
    }
}
