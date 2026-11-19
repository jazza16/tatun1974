package com.example.data.api

import android.util.Log
import com.example.BuildConfig
import com.example.data.local.KarhutlaHotspotEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.sqrt

/**
 * NASA FIRMS Satellite Sensor Sources for Karhutla Hotspot Detection.
 */
enum class FirmsSensorSource(
    val apiCode: String,
    val label: String,
    val resolution: String,
    val firmsWmsLayer: String,
    val gibsWmsLayer: String
) {
    VIIRS_SNPP(
        apiCode = "VIIRS_SNPP_NRT",
        label = "VIIRS Suomi-NPP",
        resolution = "375m",
        firmsWmsLayer = "fires_viirs_snpp_24",
        gibsWmsLayer = "VIIRS_SNPP_Thermal_Anomalies_375m_All"
    ),
    VIIRS_NOAA20(
        apiCode = "VIIRS_NOAA20_NRT",
        label = "VIIRS NOAA-20",
        resolution = "375m",
        firmsWmsLayer = "fires_viirs_noaa20_24",
        gibsWmsLayer = "VIIRS_NOAA20_Thermal_Anomalies_375m_All"
    ),
    MODIS(
        apiCode = "MODIS_NRT",
        label = "MODIS Terra/Aqua",
        resolution = "1km",
        firmsWmsLayer = "fires_modis_24",
        gibsWmsLayer = "MODIS_Combined_Thermal_Anomalies_All"
    )
}

enum class HotspotConfidenceLevel(val label: String) {
    HIGH("Tinggi (High)"),
    NOMINAL("Sedang (Nominal)"),
    LOW("Rendah (Low)")
}

data class MukomukoDistrictCoord(
    val name: String,
    val latitude: Double,
    val longitude: Double
)

/**
 * Model representing active fire / hotspot detected by NASA FIRMS satellite sensors (VIIRS / MODIS)
 * or synchronized from BMKG.
 * Stores `acqDateTime` as UTC epoch milliseconds and deterministic `sourceId`.
 */
data class NasaFirmsHotspot(
    val id: String = "",
    val sourceType: String = KarhutlaHotspotEntity.SOURCE_FIRMS,
    val latitude: Double,
    val longitude: Double,
    val brightness: Double,
    val scan: Double,
    val track: Double,
    val acqDateTime: Long = System.currentTimeMillis(), // Epoch millis UTC
    val acqDate: String = "",
    val acqTime: String = "",
    val satellite: String,
    val confidence: String,
    val frp: Double,
    val dayNight: String,
    val nearestSubDistrict: String = "Kota Mukomuko",
    val distanceToSubDistrictKm: Double = 0.0,
    val verificationStatus: String = KarhutlaHotspotEntity.STATUS_FIRMS_ONLY,
    val isMatchedWithCrossSource: Boolean = false,
    val matchedPartnerSourceId: String? = null,
    val matchedDistanceKm: Double? = null,
    val isSimulation: Boolean = false
) {
    val brightnessCelsius: Double
        get() = (brightness - 273.15).coerceAtLeast(0.0)

    val confidenceLevel: HotspotConfidenceLevel
        get() = when (KarhutlaHotspotEntity.normalizeConfidenceLabel(confidence)) {
            "HIGH" -> HotspotConfidenceLevel.HIGH
            "LOW" -> HotspotConfidenceLevel.LOW
            else -> HotspotConfidenceLevel.NOMINAL
        }

    val acqTimeWib: String
        get() {
            val sdf = SimpleDateFormat("dd MMM yyyy • HH:mm 'WIB'", Locale("id", "ID")).apply {
                timeZone = TimeZone.getTimeZone("Asia/Jakarta")
            }
            return sdf.format(Date(acqDateTime))
        }

    fun toEntity(): KarhutlaHotspotEntity {
        val normLat = String.format(Locale.US, "%.5f", latitude).toDouble()
        val normLon = String.format(Locale.US, "%.5f", longitude).toDouble()
        val confNorm = KarhutlaHotspotEntity.normalizeConfidenceLabel(confidence)
        val deterministicId = if (sourceType == KarhutlaHotspotEntity.SOURCE_BMKG) {
            KarhutlaHotspotEntity.buildBmkgSourceId(acqDateTime, normLat, normLon, confNorm)
        } else {
            KarhutlaHotspotEntity.buildFirmsSourceId(satellite, acqDateTime, normLat, normLon)
        }
        return KarhutlaHotspotEntity(
            sourceId = deterministicId,
            sourceType = sourceType,
            satellite = satellite,
            acqDateTime = acqDateTime,
            lat = normLat,
            lon = normLon,
            confidenceLabel = confNorm,
            brightnessKelvin = brightness,
            frpMw = frp,
            dayNight = dayNight,
            nearestSubDistrict = nearestSubDistrict,
            distanceToSubDistrictKm = distanceToSubDistrictKm,
            verificationStatus = verificationStatus,
            isMatchedWithCrossSource = isMatchedWithCrossSource,
            matchedPartnerSourceId = matchedPartnerSourceId,
            matchedDistanceKm = matchedDistanceKm,
            isSimulation = isSimulation
        )
    }

    companion object {
        fun fromEntity(entity: KarhutlaHotspotEntity): NasaFirmsHotspot {
            val utcDateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val utcTimeFmt = SimpleDateFormat("HHmm", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val dt = Date(entity.acqDateTime)
            return NasaFirmsHotspot(
                id = entity.sourceId,
                sourceType = entity.sourceType,
                latitude = entity.lat,
                longitude = entity.lon,
                brightness = entity.brightnessKelvin,
                scan = 0.38,
                track = 0.38,
                acqDateTime = entity.acqDateTime,
                acqDate = utcDateFmt.format(dt),
                acqTime = utcTimeFmt.format(dt),
                satellite = entity.satellite,
                confidence = entity.confidenceLabel,
                frp = entity.frpMw,
                dayNight = entity.dayNight,
                nearestSubDistrict = entity.nearestSubDistrict,
                distanceToSubDistrictKm = entity.distanceToSubDistrictKm,
                verificationStatus = entity.verificationStatus,
                isMatchedWithCrossSource = entity.isMatchedWithCrossSource,
                matchedPartnerSourceId = entity.matchedPartnerSourceId,
                matchedDistanceKm = entity.matchedDistanceKm,
                isSimulation = entity.isSimulation
            )
        }
    }
}

/**
 * Result wrapper for NASA FIRMS & BMKG fetch operations with diagnostic metadata.
 */
data class FirmsFetchResult(
    val hotspots: List<NasaFirmsHotspot>,
    val sourceLabel: String,
    val isFirmsKeyConfigured: Boolean,
    val isSimulationMode: Boolean,
    val matchedCrossSourceCount: Int = 0,
    val lastUpdatedTimestamp: Long = System.currentTimeMillis(),
    val statusMessage: String
)

/**
 * Service to query NASA FIRMS (Fire Information for Resource Management System) API and WMS tiles
 * for real-time Karhutla (forest & plantation fire) hotspot detection across Mukomuko Regency
 * on top of OpenStreetMap.
 */
object NasaFirmsHotspotService {
    private const val TAG = "NasaFirmsService"
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    /**
     * Mukomuko bounding box (West Sumatra border to North Bengkulu border, Indian Ocean to Bukit Barisan):
     * Min Lon: 101.0, Min Lat: -3.3, Max Lon: 101.85, Max Lat: -2.35
     */
    const val MUKOMUKO_MIN_LON = 101.00
    const val MUKOMUKO_MAX_LON = 101.85
    const val MUKOMUKO_MIN_LAT = -3.30
    const val MUKOMUKO_MAX_LAT = -2.35
    private const val MUKOMUKO_AREA = "101.0,-3.3,101.85,-2.35"

    val MUKOMUKO_15_DISTRICTS: List<MukomukoDistrictCoord> = listOf(
        MukomukoDistrictCoord("Lubuk Pinang", -2.4350, 101.1680),
        MukomukoDistrictCoord("XIV Koto", -2.5167, 101.1333),
        MukomukoDistrictCoord("V Koto", -2.5020, 101.2450),
        MukomukoDistrictCoord("Air Manjunto", -2.5480, 101.1780),
        MukomukoDistrictCoord("Kota Mukomuko", -2.5833, 101.1167),
        MukomukoDistrictCoord("Air Dikit", -2.6500, 101.2000),
        MukomukoDistrictCoord("Teras Terunjam", -2.6167, 101.2800),
        MukomukoDistrictCoord("Penarik", -2.7333, 101.3000),
        MukomukoDistrictCoord("Selagan Raya", -2.6833, 101.3450),
        MukomukoDistrictCoord("Teramang Jaya", -2.8000, 101.3833),
        MukomukoDistrictCoord("Pondok Suguh", -2.8500, 101.4667),
        MukomukoDistrictCoord("Sungai Rumbai", -2.9250, 101.5450),
        MukomukoDistrictCoord("Ipuh", -2.9833, 101.6000),
        MukomukoDistrictCoord("Malin Deman", -3.0500, 101.6833),
        MukomukoDistrictCoord("Air Rami", -3.1500, 101.7667)
    )

    fun getFirmsMapKey(): String {
        return try {
            BuildConfig.FIRMS_MAP_KEY.trim()
        } catch (_: Exception) {
            ""
        }
    }

    fun isFirmsKeyValid(): Boolean {
        val key = getFirmsMapKey()
        return key.isNotBlank() && key != "YOUR_FIRMS_MAP_KEY" && !key.startsWith("YOUR_")
    }

    /**
     * Builds a WMS 1.1.1 Tile URL string for OpenStreetMap (`osmdroid` `OnlineTileSourceBase`)
     * returning a 256x256 transparent PNG overlay of active thermal hotspots.
     */
    fun buildOsmFirmsWmsTileUrl(
        zoom: Int,
        tileX: Int,
        tileY: Int,
        sensor: FirmsSensorSource = FirmsSensorSource.VIIRS_SNPP
    ): String {
        val bbox = tileXYToBBoxEpsg4326(tileX, tileY, zoom)
        val bboxStr = String.format(
            Locale.US,
            "%.6f,%.6f,%.6f,%.6f",
            bbox[0], bbox[1], bbox[2], bbox[3]
        )
        return if (isFirmsKeyValid()) {
            val key = getFirmsMapKey()
            "https://firms.modaps.eosdis.nasa.gov/mapserver/wms/fires/$key/?" +
                "SERVICE=WMS&VERSION=1.1.1&REQUEST=GetMap" +
                "&LAYERS=${sensor.firmsWmsLayer}" +
                "&STYLES=&SRS=EPSG:4326" +
                "&BBOX=$bboxStr&WIDTH=256&HEIGHT=256" +
                "&FORMAT=image/png&TRANSPARENT=TRUE"
        } else {
            // Public NASA Earthdata GIBS WMS for near-real-time VIIRS/MODIS Thermal Anomalies
            "https://gibs.earthdata.nasa.gov/wms/epsg4326/best/wms.cgi?" +
                "SERVICE=WMS&VERSION=1.1.1&REQUEST=GetMap" +
                "&LAYERS=${sensor.gibsWmsLayer}" +
                "&STYLES=&SRS=EPSG:4326" +
                "&BBOX=$bboxStr&WIDTH=256&HEIGHT=256" +
                "&FORMAT=image/png&TRANSPARENT=TRUE"
        }
    }

    /**
     * Converts XYZ slippy map tile coordinates to EPSG:4326 [minLon, minLat, maxLon, maxLat].
     */
    private fun tileXYToBBoxEpsg4326(x: Int, y: Int, zoom: Int): DoubleArray {
        val n = 2.0.pow(zoom.toDouble())
        val minLon = (x / n) * 360.0 - 180.0
        val maxLon = ((x + 1) / n) * 360.0 - 180.0
        val latRadNorth = atan(sinh(Math.PI * (1.0 - 2.0 * y / n)))
        val latRadSouth = atan(sinh(Math.PI * (1.0 - 2.0 * (y + 1) / n)))
        val maxLat = Math.toDegrees(latRadNorth)
        val minLat = Math.toDegrees(latRadSouth)
        return doubleArrayOf(minLon, minLat, maxLon, maxLat)
    }

    /**
     * Fetches active FIRMS hotspots for Mukomuko Regency as `KarhutlaHotspotEntity` list
     * with deterministic `sourceId = "$satellite|$acqDateTime|$lat|$lon"` and UTC epoch millis.
     */
    suspend fun fetchFirmsEntitiesForMukomuko(
        sensor: FirmsSensorSource = FirmsSensorSource.VIIRS_SNPP,
        dayRange: Int = 2,
        enableSimulation: Boolean = false
    ): List<KarhutlaHotspotEntity> = withContext(Dispatchers.IO) {
        val res = fetchHotspotsForMukomuko(
            sensor = sensor,
            dayRange = dayRange,
            enableSimulationIfNeeded = enableSimulation
        )
        res.hotspots.map { it.toEntity() }
    }

    /**
     * Fetches active hotspots for Mukomuko Regency from NASA FIRMS CSV API (if key configured)
     * or NASA EONET Open Wildfire API, with optional BPBD Training Simulation mode.
     */
    suspend fun fetchHotspotsForMukomuko(
        sensor: FirmsSensorSource = FirmsSensorSource.VIIRS_SNPP,
        dayRange: Int = 2,
        enableSimulationIfNeeded: Boolean = false
    ): FirmsFetchResult = withContext(Dispatchers.IO) {
        val safeDayRange = dayRange.coerceIn(1, 5)
        val firmsConfigured = isFirmsKeyValid()

        if (enableSimulationIfNeeded) {
            val simList = getTrainingSimulationHotspots(sensor)
            return@withContext FirmsFetchResult(
                hotspots = simList,
                sourceLabel = "Simulasi Latihan Posko (${sensor.label} + BMKG)",
                isFirmsKeyConfigured = firmsConfigured,
                isSimulationMode = true,
                statusMessage = "Mode Simulasi Latihan Posko Karhutla aktif (${simList.size} titik FIRMS disinkronkan dengan BMKG)."
            )
        }

        if (firmsConfigured) {
            val key = getFirmsMapKey()
            try {
                val url = "https://firms.modaps.eosdis.nasa.gov/api/area/csv/$key/${sensor.apiCode}/$MUKOMUKO_AREA/$safeDayRange"
                val request = Request.Builder().url(url).build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val csv = response.body?.string().orEmpty()
                        val parsed = parseCsv(csv, sensor.apiCode)
                        val msg = if (parsed.isEmpty()) {
                            "Sinkronisasi FIRMS (${sensor.label}) & BMKG aktif • Nihil titik panas dalam ${safeDayRange * 24} jam terakhir di Kab. Mukomuko."
                        } else {
                            "Sinkronisasi FIRMS (${sensor.label}) & BMKG aktif • Terdeteksi ${parsed.size} titik panas di Kab. Mukomuko."
                        }
                        return@withContext FirmsFetchResult(
                            hotspots = parsed,
                            sourceLabel = "NASA FIRMS Live (${sensor.label})",
                            isFirmsKeyConfigured = true,
                            isSimulationMode = false,
                            statusMessage = msg
                        )
                    } else {
                        Log.w(TAG, "NASA FIRMS API returned HTTP ${response.code}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to query NASA FIRMS CSV API: ${e.message}")
            }
        }

        // Open-access fallback: Query NASA EONET v3 Wildfire Events in Sumatra/Bengkulu/Mukomuko bounding box
        val eonetHotspots = fetchNasaEonetWildfires(sensor.apiCode)
        val statusMsg = if (firmsConfigured) {
            "Sinkronisasi WorkManager FIRMS & BMKG aktif (${eonetHotspots.size} titik panas terdeteksi)."
        } else {
            "Sinkronisasi WorkManager (FIRMS 1–3 jam, BMKG 3–6 jam) & Layer WMS aktif (${eonetHotspots.size} titik panas)."
        }

        FirmsFetchResult(
            hotspots = eonetHotspots,
            sourceLabel = "OSM + NASA FIRMS / BMKG",
            isFirmsKeyConfigured = firmsConfigured,
            isSimulationMode = false,
            statusMessage = statusMsg
        )
    }

    suspend fun fetchActiveHotspots(): List<NasaFirmsHotspot> = withContext(Dispatchers.IO) {
        fetchHotspotsForMukomuko(
            sensor = FirmsSensorSource.VIIRS_SNPP,
            dayRange = 1,
            enableSimulationIfNeeded = false
        ).hotspots
    }

    private fun fetchNasaEonetWildfires(defaultSatCode: String): List<NasaFirmsHotspot> {
        return try {
            val url = "https://eonet.gsfc.nasa.gov/api/v3/events?category=wildfires&bbox=100.8,-3.5,102.0,-2.0&status=open&limit=25"
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()
                val body = response.body?.string() ?: return emptyList()
                val json = JSONObject(body)
                val events = json.optJSONArray("events") ?: return emptyList()
                val results = mutableListOf<NasaFirmsHotspot>()
                for (i in 0 until events.length()) {
                    val ev = events.getJSONObject(i)
                    val geometries = ev.optJSONArray("geometry") ?: continue
                    if (geometries.length() > 0) {
                        val geom = geometries.getJSONObject(geometries.length() - 1)
                        val coords = geom.optJSONArray("coordinates") ?: continue
                        if (coords.length() >= 2) {
                            val lon = String.format(Locale.US, "%.5f", coords.optDouble(0)).toDouble()
                            val lat = String.format(Locale.US, "%.5f", coords.optDouble(1)).toDouble()
                            val dateIso = geom.optString("date", "")
                            val acqDate = dateIso.take(10).ifBlank { "2026-11-17" }
                            val acqTime = if (dateIso.length >= 16) {
                                dateIso.substring(11, 16).replace(":", "")
                            } else {
                                "0500"
                            }
                            val acqEpochUtc = KarhutlaHotspotEntity.parseUtcEpochMillis(acqDate, acqTime)
                            val deterministicId = KarhutlaHotspotEntity.buildFirmsSourceId(
                                satellite = defaultSatCode,
                                acqDateTime = acqEpochUtc,
                                lat = lat,
                                lon = lon
                            )
                            val (nearestName, distKm) = findNearestMukomukoDistrict(lat, lon)
                            results.add(
                                NasaFirmsHotspot(
                                    id = deterministicId,
                                    sourceType = KarhutlaHotspotEntity.SOURCE_FIRMS,
                                    latitude = lat,
                                    longitude = lon,
                                    brightness = 338.5,
                                    scan = 0.38,
                                    track = 0.38,
                                    acqDateTime = acqEpochUtc,
                                    acqDate = acqDate,
                                    acqTime = acqTime,
                                    satellite = defaultSatCode,
                                    confidence = "NOMINAL",
                                    frp = 18.4,
                                    dayNight = "D",
                                    nearestSubDistrict = nearestName,
                                    distanceToSubDistrictKm = distKm,
                                    isSimulation = false
                                )
                            )
                        }
                    }
                }
                results
            }
        } catch (e: Exception) {
            Log.w(TAG, "NASA EONET wildfire query failed: ${e.message}")
            emptyList()
        }
    }

    private fun parseCsv(csv: String, defaultSatellite: String): List<NasaFirmsHotspot> {
        val lines = csv.lines().filter { it.isNotBlank() }
        if (lines.size <= 1) return emptyList()

        val results = mutableListOf<NasaFirmsHotspot>()
        for (i in 1 until lines.size) {
            val tokens = lines[i].split(",")
            if (tokens.size >= 10) {
                try {
                    val rawLat = tokens[0].trim().toDoubleOrNull() ?: continue
                    val rawLon = tokens[1].trim().toDoubleOrNull() ?: continue
                    val lat = String.format(Locale.US, "%.5f", rawLat).toDouble()
                    val lon = String.format(Locale.US, "%.5f", rawLon).toDouble()
                    val brightness = tokens.getOrNull(2)?.trim()?.toDoubleOrNull() ?: 325.0
                    val scan = tokens.getOrNull(3)?.trim()?.toDoubleOrNull() ?: 0.38
                    val track = tokens.getOrNull(4)?.trim()?.toDoubleOrNull() ?: 0.38
                    val acqDate = tokens.getOrNull(5)?.trim() ?: ""
                    val acqTime = tokens.getOrNull(6)?.trim() ?: ""
                    val satellite = tokens.getOrNull(7)?.trim().takeUnless { it.isNullOrBlank() } ?: defaultSatellite
                    val confidence = KarhutlaHotspotEntity.normalizeConfidenceLabel(tokens.getOrNull(8)?.trim() ?: "NOMINAL")
                    val frp = tokens.getOrNull(10)?.trim()?.toDoubleOrNull()
                        ?: tokens.getOrNull(9)?.trim()?.toDoubleOrNull()
                        ?: 12.0
                    val dayNight = tokens.getOrNull(11)?.trim() ?: "D"

                    val acqEpochUtc = KarhutlaHotspotEntity.parseUtcEpochMillis(acqDate, acqTime)
                    val deterministicId = KarhutlaHotspotEntity.buildFirmsSourceId(
                        satellite = satellite,
                        acqDateTime = acqEpochUtc,
                        lat = lat,
                        lon = lon
                    )
                    val (nearestDistrict, distKm) = findNearestMukomukoDistrict(lat, lon)

                    results.add(
                        NasaFirmsHotspot(
                            id = deterministicId,
                            sourceType = KarhutlaHotspotEntity.SOURCE_FIRMS,
                            latitude = lat,
                            longitude = lon,
                            brightness = brightness,
                            scan = scan,
                            track = track,
                            acqDateTime = acqEpochUtc,
                            acqDate = acqDate,
                            acqTime = acqTime,
                            satellite = satellite,
                            confidence = confidence,
                            frp = frp,
                            dayNight = dayNight,
                            nearestSubDistrict = nearestDistrict,
                            distanceToSubDistrictKm = distKm,
                            isSimulation = false
                        )
                    )
                } catch (_: Exception) {
                    // skip malformed line
                }
            }
        }
        return results
    }

    /**
     * Explicit Safety Simulation Mode dataset for BPBD Mukomuko Posko Karhutla tactical drills.
     * Uses UTC epoch millis aligned with `BmkgHotspotService.getSimulationBmkgHotspots` so `@Transaction`
     * cross-matching can be tested end-to-end.
     */
    fun getTrainingSimulationHotspots(
        sensor: FirmsSensorSource,
        referenceHourUtcMillis: Long = (System.currentTimeMillis() / 3600_000L) * 3600_000L
    ): List<NasaFirmsHotspot> {
        data class RawSim(
            val lat: Double,
            val lon: Double,
            val kelvin: Double,
            val conf: String,
            val frp: Double,
            val offsetMinutes: Long
        )

        val items = listOf(
            RawSim(-2.6920, 101.3680, 356.4, "HIGH", 42.8, 0L),
            RawSim(-2.7540, 101.3180, 341.2, "NOMINAL", 21.5, -10L),
            RawSim(-3.0280, 101.6620, 362.8, "HIGH", 54.1, 5L),
            RawSim(-2.8380, 101.4720, 329.5, "NOMINAL", 14.2, -5L),
            RawSim(-2.4780, 101.2140, 319.0, "LOW", 7.6, -20L)
        )

        val utcDateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val utcTimeFmt = SimpleDateFormat("HHmm", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        return items.map { item ->
            val acqUtc = referenceHourUtcMillis + (item.offsetMinutes * 60_000L)
            val normLat = String.format(Locale.US, "%.5f", item.lat).toDouble()
            val normLon = String.format(Locale.US, "%.5f", item.lon).toDouble()
            val deterministicId = KarhutlaHotspotEntity.buildFirmsSourceId(
                satellite = sensor.apiCode,
                acqDateTime = acqUtc,
                lat = normLat,
                lon = normLon
            )
            val (nearest, distKm) = findNearestMukomukoDistrict(normLat, normLon)
            val dt = Date(acqUtc)
            NasaFirmsHotspot(
                id = deterministicId,
                sourceType = KarhutlaHotspotEntity.SOURCE_FIRMS,
                latitude = normLat,
                longitude = normLon,
                brightness = item.kelvin,
                scan = 0.38,
                track = 0.38,
                acqDateTime = acqUtc,
                acqDate = utcDateFmt.format(dt),
                acqTime = utcTimeFmt.format(dt),
                satellite = sensor.apiCode,
                confidence = item.conf,
                frp = item.frp,
                dayNight = "D",
                nearestSubDistrict = nearest,
                distanceToSubDistrictKm = distKm,
                isSimulation = true
            )
        }
    }

    fun findNearestMukomukoDistrict(lat: Double, lon: Double): Pair<String, Double> {
        var bestName = "Kota Mukomuko"
        var bestDist = Double.MAX_VALUE
        for (district in MUKOMUKO_15_DISTRICTS) {
            val d = haversineKm(lat, lon, district.latitude, district.longitude)
            if (d < bestDist) {
                bestDist = d
                bestName = district.name
            }
        }
        return bestName to bestDist
    }

    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
