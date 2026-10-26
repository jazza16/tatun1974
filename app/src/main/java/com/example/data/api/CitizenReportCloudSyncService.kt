package com.example.data.api

import android.util.Log
import com.example.model.CitizenDisasterReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Cloud Synchronization Service for Citizen Disaster Reports in Mukomuko.
 * Syncs reports across all Android devices running SIPAGAR KITA using a shared
 * cloud object container, ensuring that reports submitted from one device
 * appear on all other citizen and BPBD officer devices.
 */
object CitizenReportCloudSyncService {
    private const val TAG = "CitizenReportSync"
    private const val SYNC_ENDPOINT = "https://api.restful-api.dev/objects/ff808181a09d98f701a1760160d80ce5"

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Fetches all cloud-synced citizen disaster reports.
     * Returns null if network fails or device is offline.
     */
    suspend fun fetchReportsFromCloud(): List<CitizenDisasterReport>? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(SYNC_ENDPOINT)
                .header("Accept", "application/json")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        val root = JSONObject(body)
                        val dataObj = root.optJSONObject("data")
                        val reportsArray = dataObj?.optJSONArray("reports")
                        if (reportsArray != null) {
                            val result = mutableListOf<CitizenDisasterReport>()
                            for (i in 0 until reportsArray.length()) {
                                val item = reportsArray.getJSONObject(i)
                                val r = CitizenDisasterReport(
                                    id = item.optString("id"),
                                    disasterType = item.optString("disasterType", "Bencana"),
                                    incidentTime = item.optString("incidentTime", ""),
                                    incidentLocation = item.optString("incidentLocation", ""),
                                    district = item.optString("district", "Kota Mukomuko"),
                                    village = item.optString("village", ""),
                                    latitude = item.optDouble("latitude", -2.5833),
                                    longitude = item.optDouble("longitude", 101.1167),
                                    gpsAccuracyMeters = item.optDouble("gpsAccuracyMeters", 5.0).toFloat(),
                                    isGpsVerified = item.optBoolean("isGpsVerified", true),
                                    casualtiesDead = item.optInt("casualtiesDead", 0),
                                    casualtiesInjured = item.optInt("casualtiesInjured", 0),
                                    casualtiesMissing = item.optInt("casualtiesMissing", 0),
                                    photoUri = item.optString("photoUri").takeIf { it.isNotBlank() },
                                    reporterName = item.optString("reporterName", "Warga"),
                                    reporterPhone = item.optString("reporterPhone", ""),
                                    additionalNotes = item.optString("additionalNotes", ""),
                                    timestamp = item.optLong("timestamp", System.currentTimeMillis()),
                                    status = item.optString("status", "Diproses")
                                )
                                if (r.id.isNotBlank()) {
                                    result.add(r)
                                }
                            }
                            Log.i(TAG, "Successfully synced ${result.size} reports from cloud.")
                            return@withContext result
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Cloud sync fetch error: ${e.message}")
        }
        null
    }

    /**
     * Pushes the merged list of citizen reports to the cloud container so all devices stay in sync.
     */
    suspend fun pushReportsToCloud(reports: List<CitizenDisasterReport>): Boolean = withContext(Dispatchers.IO) {
        try {
            val jsonArray = JSONArray()
            reports.take(100).forEach { r ->
                val obj = JSONObject().apply {
                    put("id", r.id)
                    put("disasterType", r.disasterType)
                    put("incidentTime", r.incidentTime)
                    put("incidentLocation", r.incidentLocation)
                    put("district", r.district)
                    put("village", r.village)
                    put("latitude", r.latitude)
                    put("longitude", r.longitude)
                    put("gpsAccuracyMeters", r.gpsAccuracyMeters)
                    put("isGpsVerified", r.isGpsVerified)
                    put("casualtiesDead", r.casualtiesDead)
                    put("casualtiesInjured", r.casualtiesInjured)
                    put("casualtiesMissing", r.casualtiesMissing)
                    put("photoUri", r.photoUri ?: "")
                    put("reporterName", r.reporterName)
                    put("reporterPhone", r.reporterPhone)
                    put("additionalNotes", r.additionalNotes)
                    put("timestamp", r.timestamp)
                    put("status", r.status)
                }
                jsonArray.put(obj)
            }

            val payload = JSONObject().apply {
                put("name", "SIPAGAR_KITA_MUKOMUKO_REPORTS_V1")
                put("data", JSONObject().apply {
                    put("updatedAt", System.currentTimeMillis())
                    put("reports", jsonArray)
                })
            }

            val body = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(SYNC_ENDPOINT)
                .put(body)
                .build()

            httpClient.newCall(request).execute().use { response ->
                val ok = response.isSuccessful
                Log.i(TAG, "Pushed ${reports.size} reports to cloud container: success=$ok")
                return@withContext ok
            }
        } catch (e: Exception) {
            Log.d(TAG, "Cloud sync push error: ${e.message}")
            false
        }
    }
}
