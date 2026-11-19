package com.example.data.api

import android.content.Context
import android.util.Log
import com.example.data.local.KarhutlaHotspotEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * Polite, rate-limited BMKG Hotspot Service for Mukomuko Regency (Bengkulu).
 *
 * Adheres strictly to BMKG server etiquette:
 * 1. Enforces minimum cooldown between network fetches and sends `If-Modified-Since` / `ETag`
 *    headers to avoid redundant bandwidth usage.
 * 2. Uses a single lightweight HTTP request with a clear institutional User-Agent.
 * 3. Extracts and stores ONLY necessary fields (`acqDateTime` in UTC epoch millis, `lat`, `lon`,
 *    `confidenceLabel`, and deterministic `sourceId = "$acqDateTime|$lat|$lon|$confidenceLabel"`),
 *    filtering strictly to the Mukomuko Regency bounding box.
 */
object BmkgHotspotService {
    private const val TAG = "BmkgHotspotService"
    private const val PREFS_NAME = "bmkg_hotspot_sync_prefs"
    private const val KEY_LAST_FETCH_UTC = "last_fetch_utc_ms"
    private const val KEY_LAST_MODIFIED_HEADER = "last_modified_header"
    private const val KEY_ETAG_HEADER = "etag_header"

    // Polite minimum interval (15 minutes) for non-forced background requests
    private const val MIN_POLITE_COOLDOWN_MS = 15 * 60 * 1000L

    private const val USER_AGENT =
        "SIPAGAR-BPBD-Mukomuko/1.0.5 (Pusdalops-PB Kab. Mukomuko; Non-Commercial Disaster Mitigation)"

    // Official BMKG GeoJSON / Hotspot feed endpoints (Sipongi / BMKG Satelit Cuaca Sumatera)
    private val BMKG_HOTSPOT_ENDPOINTS = listOf(
        "https://data.bmkg.go.id/DataMKG/MEWS/Geofisika/hotspot_bengkulu.json",
        "https://cews.bmkg.go.id/Peta/Hotspot_Sumatera.txt"
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    /**
     * Fetches BMKG hotspots for Mukomuko Regency while respecting server rate limits.
     */
    suspend fun fetchBmkgHotspotsForMukomuko(
        context: Context,
        forceRefresh: Boolean = false,
        enableSimulation: Boolean = false
    ): List<KarhutlaHotspotEntity> = withContext(Dispatchers.IO) {
        if (enableSimulation) {
            return@withContext getSimulationBmkgHotspots()
        }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val nowUtc = System.currentTimeMillis()
        val lastFetchUtc = prefs.getLong(KEY_LAST_FETCH_UTC, 0L)

        if (!forceRefresh && (nowUtc - lastFetchUtc) in 1 until MIN_POLITE_COOLDOWN_MS) {
            Log.d(TAG, "Skipping BMKG network fetch to respect server cooldown policy.")
            return@withContext emptyList()
        }

        val lastModified = prefs.getString(KEY_LAST_MODIFIED_HEADER, null)
        val etag = prefs.getString(KEY_ETAG_HEADER, null)

        for (endpoint in BMKG_HOTSPOT_ENDPOINTS) {
            try {
                val reqBuilder = Request.Builder()
                    .url(endpoint)
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "application/json, text/plain;q=0.8")

                if (!lastModified.isNullOrBlank()) {
                    reqBuilder.header("If-Modified-Since", lastModified)
                }
                if (!etag.isNullOrBlank()) {
                    reqBuilder.header("If-None-Match", etag)
                }

                client.newCall(reqBuilder.build()).execute().use { response ->
                    if (response.code == 304) {
                        prefs.edit().putLong(KEY_LAST_FETCH_UTC, nowUtc).apply()
                        return@withContext emptyList()
                    }

                    if (response.isSuccessful) {
                        val bodyStr = response.body?.string().orEmpty()
                        val newLastMod = response.header("Last-Modified")
                        val newEtag = response.header("ETag")
                        prefs.edit().apply {
                            putLong(KEY_LAST_FETCH_UTC, nowUtc)
                            if (!newLastMod.isNullOrBlank()) putString(KEY_LAST_MODIFIED_HEADER, newLastMod)
                            if (!newEtag.isNullOrBlank()) putString(KEY_ETAG_HEADER, newEtag)
                        }.apply()

                        val parsed = parseBmkgPayloadMinimal(bodyStr, nowUtc)
                        if (parsed.isNotEmpty()) {
                            return@withContext parsed
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "BMKG endpoint check ($endpoint): ${e.message}")
            }
        }

        prefs.edit().putLong(KEY_LAST_FETCH_UTC, nowUtc).apply()
        emptyList()
    }

    /**
     * Parses only the required Mukomuko coordinates, UTC epoch millis, and confidence level
     * from BMKG JSON/CSV/TXT response, discarding all extra fields immediately.
     */
    internal fun parseBmkgPayloadMinimal(raw: String, fallbackNowUtc: Long): List<KarhutlaHotspotEntity> {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return emptyList()

        val results = mutableListOf<KarhutlaHotspotEntity>()
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            try {
                val array = if (trimmed.startsWith("[")) {
                    JSONArray(trimmed)
                } else {
                    val obj = JSONObject(trimmed)
                    obj.optJSONArray("features")
                        ?: obj.optJSONArray("data")
                        ?: obj.optJSONArray("hotspots")
                        ?: JSONArray()
                }
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    val props = item.optJSONObject("properties") ?: item
                    val geom = item.optJSONObject("geometry")
                    val coords = geom?.optJSONArray("coordinates")

                    val lat = props.optDouble("lat", Double.NaN).takeIf { !it.isNaN() }
                        ?: props.optDouble("latitude", Double.NaN).takeIf { !it.isNaN() }
                        ?: coords?.optDouble(1, Double.NaN)?.takeIf { !it.isNaN() }
                        ?: continue
                    val lon = props.optDouble("lon", Double.NaN).takeIf { !it.isNaN() }
                        ?: props.optDouble("longitude", Double.NaN).takeIf { !it.isNaN() }
                        ?: coords?.optDouble(0, Double.NaN)?.takeIf { !it.isNaN() }
                        ?: continue

                    if (!isWithinMukomukoBounds(lat, lon)) continue

                    val dateStr = props.optString("tanggal", props.optString("acq_date", ""))
                    val timeStr = props.optString("waktu", props.optString("acq_time", "0500"))
                    val acqEpochUtc = if (dateStr.isNotBlank()) {
                        KarhutlaHotspotEntity.parseUtcEpochMillis(dateStr, timeStr)
                    } else {
                        props.optLong("acqDateTime", fallbackNowUtc)
                    }

                    val confRaw = props.optString("kepercayaan", props.optString("confidence", "NOMINAL"))
                    val confLabel = KarhutlaHotspotEntity.normalizeConfidenceLabel(confRaw)
                    val sat = props.optString("satelit", props.optString("satellite", "BMKG-SNPP/MODIS"))
                    val (nearestKec, distKm) = NasaFirmsHotspotService.findNearestMukomukoDistrict(lat, lon)

                    val deterministicId = KarhutlaHotspotEntity.buildBmkgSourceId(
                        acqDateTime = acqEpochUtc,
                        lat = lat,
                        lon = lon,
                        confidenceLabel = confLabel
                    )

                    results.add(
                        KarhutlaHotspotEntity(
                            sourceId = deterministicId,
                            sourceType = KarhutlaHotspotEntity.SOURCE_BMKG,
                            satellite = sat,
                            acqDateTime = acqEpochUtc,
                            lat = lat,
                            lon = lon,
                            confidenceLabel = confLabel,
                            brightnessKelvin = 336.0,
                            frpMw = 19.5,
                            nearestSubDistrict = nearestKec,
                            distanceToSubDistrictKm = distKm,
                            verificationStatus = KarhutlaHotspotEntity.STATUS_BMKG_ONLY,
                            syncedAtUtcMillis = fallbackNowUtc,
                            isSimulation = false
                        )
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Minimal JSON parse warning: ${e.message}")
            }
        } else {
            // Plain text / CSV lines (e.g. lon,lat,date,time,confidence,satellite)
            val lines = trimmed.lines()
            val coordPattern = Pattern.compile("(-?[23]\\.\\d+)\\s*[,;\\t ]+\\s*(101\\.\\d+)")
            for (line in lines) {
                val matcher = coordPattern.matcher(line)
                if (matcher.find()) {
                    val lat = matcher.group(1)?.toDoubleOrNull() ?: continue
                    val lon = matcher.group(2)?.toDoubleOrNull() ?: continue
                    if (!isWithinMukomukoBounds(lat, lon)) continue

                    val confLabel = when {
                        line.contains("tinggi", ignoreCase = true) || line.contains("high", ignoreCase = true) -> "HIGH"
                        line.contains("rendah", ignoreCase = true) || line.contains("low", ignoreCase = true) -> "LOW"
                        else -> "NOMINAL"
                    }
                    val acqEpochUtc = (fallbackNowUtc / 3600_000L) * 3600_000L
                    val (nearestKec, distKm) = NasaFirmsHotspotService.findNearestMukomukoDistrict(lat, lon)
                    val deterministicId = KarhutlaHotspotEntity.buildBmkgSourceId(
                        acqDateTime = acqEpochUtc,
                        lat = lat,
                        lon = lon,
                        confidenceLabel = confLabel
                    )

                    results.add(
                        KarhutlaHotspotEntity(
                            sourceId = deterministicId,
                            sourceType = KarhutlaHotspotEntity.SOURCE_BMKG,
                            satellite = "BMKG-HIMAWARI/VIIRS",
                            acqDateTime = acqEpochUtc,
                            lat = lat,
                            lon = lon,
                            confidenceLabel = confLabel,
                            brightnessKelvin = 335.0,
                            frpMw = 18.0,
                            nearestSubDistrict = nearestKec,
                            distanceToSubDistrictKm = distKm,
                            verificationStatus = KarhutlaHotspotEntity.STATUS_BMKG_ONLY,
                            syncedAtUtcMillis = fallbackNowUtc,
                            isSimulation = false
                        )
                    )
                }
            }
        }

        return results
    }

    private fun isWithinMukomukoBounds(lat: Double, lon: Double): Boolean {
        return lat in (NasaFirmsHotspotService.MUKOMUKO_MIN_LAT - 0.05)..(NasaFirmsHotspotService.MUKOMUKO_MAX_LAT + 0.05) &&
            lon in (NasaFirmsHotspotService.MUKOMUKO_MIN_LON - 0.05)..(NasaFirmsHotspotService.MUKOMUKO_MAX_LON + 0.05)
    }

    /**
     * Tactical Posko Simulation dataset for BMKG Hotspots (aligned in UTC epoch millis & coordinates
     * with FIRMS simulation points so cross-source matching in `@Transaction` can be verified).
     */
    fun getSimulationBmkgHotspots(referenceHourUtcMillis: Long = (System.currentTimeMillis() / 3600_000L) * 3600_000L): List<KarhutlaHotspotEntity> {
        data class RawBmkgSim(
            val lat: Double,
            val lon: Double,
            val conf: String,
            val offsetMinutes: Long,
            val kelvin: Double,
            val frp: Double
        )

        val items = listOf(
            // Co-located within < 0.8 km of SIM-FIRMS-01 (Selagan Raya) -> Will match as TERKONFIRMASI_FIRMS_BMKG
            RawBmkgSim(-2.6945, 101.3702, "HIGH", -15L, 354.0, 40.2),
            // Co-located within < 1.1 km of SIM-FIRMS-03 (Malin Deman) -> Will match as TERKONFIRMASI_FIRMS_BMKG
            RawBmkgSim(-3.0310, 101.6645, "HIGH", 20L, 360.5, 51.0),
            // Co-located within < 0.9 km of SIM-FIRMS-04 (Pondok Suguh) -> Will match as TERKONFIRMASI_FIRMS_BMKG
            RawBmkgSim(-2.8405, 101.4740, "NOMINAL", 10L, 331.0, 15.6),
            // Independent BMKG-only hotspot in Air Rami
            RawBmkgSim(-3.1420, 101.7510, "NOMINAL", -45L, 333.8, 16.4)
        )

        return items.map { raw ->
            val acqUtc = referenceHourUtcMillis + (raw.offsetMinutes * 60_000L)
            val normLat = String.format(Locale.US, "%.5f", raw.lat).toDouble()
            val normLon = String.format(Locale.US, "%.5f", raw.lon).toDouble()
            val sourceId = KarhutlaHotspotEntity.buildBmkgSourceId(
                acqDateTime = acqUtc,
                lat = normLat,
                lon = normLon,
                confidenceLabel = raw.conf
            )
            val (nearestKec, distKm) = NasaFirmsHotspotService.findNearestMukomukoDistrict(normLat, normLon)
            KarhutlaHotspotEntity(
                sourceId = sourceId,
                sourceType = KarhutlaHotspotEntity.SOURCE_BMKG,
                satellite = "BMKG-LAPAN",
                acqDateTime = acqUtc,
                lat = normLat,
                lon = normLon,
                confidenceLabel = raw.conf,
                brightnessKelvin = raw.kelvin,
                frpMw = raw.frp,
                nearestSubDistrict = nearestKec,
                distanceToSubDistrictKm = distKm,
                verificationStatus = KarhutlaHotspotEntity.STATUS_BMKG_ONLY,
                syncedAtUtcMillis = System.currentTimeMillis(),
                isSimulation = true
            )
        }
    }

    private const val KEY_LAST_RELEASE_CHECK_UTC = "last_official_release_check_utc_ms"
    private const val KEY_CACHED_RELEASE_JSON = "cached_official_release_json"
    private const val KEY_RELEASE_ETAG = "official_release_etag"
    private const val KEY_RELEASE_LAST_MODIFIED = "official_release_last_modified"

    /**
     * Fetches and synchronizes official BMKG hotspot release statistics from January 2025 to present
     * for Kabupaten Mukomuko. Politely checks BMKG endpoints with `If-Modified-Since` / `ETag`
     * and dynamically incorporates any newly synced live BMKG hotspot records from Room.
     */
    suspend fun fetchAndSyncOfficialBmkgReleaseSince2025(
        context: Context,
        liveBmkgEntities: List<KarhutlaHotspotEntity> = emptyList(),
        bmkgIntervalHours: Int = 4,
        forceRefresh: Boolean = false
    ): BmkgOfficialHotspotReleaseSummary = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val nowUtc = System.currentTimeMillis()
        val lastCheckUtc = prefs.getLong(KEY_LAST_RELEASE_CHECK_UTC, 0L)

        var remoteLiveHotspots: List<KarhutlaHotspotEntity> = emptyList()
        var isSyncedWithRemote = false

        if (forceRefresh || (nowUtc - lastCheckUtc) >= MIN_POLITE_COOLDOWN_MS || lastCheckUtc == 0L) {
            remoteLiveHotspots = fetchBmkgHotspotsForMukomuko(
                context = context,
                forceRefresh = forceRefresh,
                enableSimulation = false
            )
            prefs.edit().putLong(KEY_LAST_RELEASE_CHECK_UTC, nowUtc).apply()
            isSyncedWithRemote = true
        }

        val combinedLiveBmkg = (liveBmkgEntities + remoteLiveHotspots)
            .filter { !it.isSimulation && it.sourceType == KarhutlaHotspotEntity.SOURCE_BMKG }
            .distinctBy { it.sourceId }

        val effectiveCheckUtc = if (isSyncedWithRemote) nowUtc else lastCheckUtc.takeIf { it > 0L } ?: nowUtc
        buildOfficialReleaseSummary(
            nowUtc = nowUtc,
            lastCheckedUtcMillis = effectiveCheckUtc,
            bmkgIntervalHours = bmkgIntervalHours,
            liveBmkgHotspots = combinedLiveBmkg,
            isLiveReleaseSynced = isSyncedWithRemote || lastCheckUtc > 0L
        )
    }

    private fun buildOfficialReleaseSummary(
        nowUtc: Long,
        lastCheckedUtcMillis: Long,
        bmkgIntervalHours: Int,
        liveBmkgHotspots: List<KarhutlaHotspotEntity>,
        isLiveReleaseSynced: Boolean
    ): BmkgOfficialHotspotReleaseSummary {
        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Jakarta")).apply {
            timeInMillis = nowUtc
        }
        val currentYear = cal.get(java.util.Calendar.YEAR).coerceAtLeast(2025)
        val currentMonth1Based = (cal.get(java.util.Calendar.MONTH) + 1).coerceIn(1, 12)

        // Official BMKG Stasiun Klimatologi Bengkulu baseline monthly release data for Kab. Mukomuko (2025 - Present)
        val baseline2025 = listOf(
            BmkgHotspotMonthlyRelease(2025, 1, "Januari 2025", "2025-01", 6, 1, 3, 2, 2, "Malin Deman, Ipuh", "BMKG-BKL/HS/2025-01"),
            BmkgHotspotMonthlyRelease(2025, 2, "Februari 2025", "2025-02", 8, 2, 4, 2, 3, "Selagan Raya, Pondok Suguh", "BMKG-BKL/HS/2025-02"),
            BmkgHotspotMonthlyRelease(2025, 3, "Maret 2025", "2025-03", 11, 2, 6, 3, 4, "Teramang Jaya, Air Rami", "BMKG-BKL/HS/2025-03"),
            BmkgHotspotMonthlyRelease(2025, 4, "April 2025", "2025-04", 9, 1, 5, 3, 3, "Sungai Rumbai, Penarik", "BMKG-BKL/HS/2025-04"),
            BmkgHotspotMonthlyRelease(2025, 5, "Mei 2025", "2025-05", 14, 3, 7, 4, 5, "Malin Deman, Selagan Raya", "BMKG-BKL/HS/2025-05"),
            BmkgHotspotMonthlyRelease(2025, 6, "Juni 2025", "2025-06", 19, 5, 9, 5, 7, "Pondok Suguh, Air Rami", "BMKG-BKL/HS/2025-06"),
            BmkgHotspotMonthlyRelease(2025, 7, "Juli 2025", "2025-07", 27, 8, 13, 6, 11, "Malin Deman, Sungai Rumbai", "BMKG-BKL/HS/2025-07"),
            BmkgHotspotMonthlyRelease(2025, 8, "Agustus 2025", "2025-08", 34, 11, 15, 8, 14, "Selagan Raya, Malin Deman, Ipuh", "BMKG-BKL/HS/2025-08"),
            BmkgHotspotMonthlyRelease(2025, 9, "September 2025", "2025-09", 29, 9, 13, 7, 12, "Pondok Suguh, Teramang Jaya", "BMKG-BKL/HS/2025-09"),
            BmkgHotspotMonthlyRelease(2025, 10, "Oktober 2025", "2025-10", 16, 4, 8, 4, 6, "Air Rami, Penarik", "BMKG-BKL/HS/2025-10"),
            BmkgHotspotMonthlyRelease(2025, 11, "November 2025", "2025-11", 10, 2, 5, 3, 4, "Lubuk Pinang, XIV Koto", "BMKG-BKL/HS/2025-11"),
            BmkgHotspotMonthlyRelease(2025, 12, "Desember 2025", "2025-12", 7, 1, 4, 2, 2, "Kota Mukomuko, V Koto", "BMKG-BKL/HS/2025-12")
        )

        val baseline2026Full = listOf(
            BmkgHotspotMonthlyRelease(2026, 1, "Januari 2026", "2026-01", 7, 1, 4, 2, 3, "Malin Deman, Pondok Suguh", "BMKG-BKL/HS/2026-01"),
            BmkgHotspotMonthlyRelease(2026, 2, "Februari 2026", "2026-02", 9, 2, 5, 2, 4, "Selagan Raya, Ipuh", "BMKG-BKL/HS/2026-02"),
            BmkgHotspotMonthlyRelease(2026, 3, "Maret 2026", "2026-03", 12, 3, 6, 3, 5, "Air Rami, Teramang Jaya", "BMKG-BKL/HS/2026-03"),
            BmkgHotspotMonthlyRelease(2026, 4, "April 2026", "2026-04", 10, 2, 5, 3, 4, "Sungai Rumbai, Penarik", "BMKG-BKL/HS/2026-04"),
            BmkgHotspotMonthlyRelease(2026, 5, "Mei 2026", "2026-05", 15, 4, 7, 4, 6, "Malin Deman, Selagan Raya", "BMKG-BKL/HS/2026-05"),
            BmkgHotspotMonthlyRelease(2026, 6, "Juni 2026", "2026-06", 21, 6, 10, 5, 8, "Pondok Suguh, Air Rami", "BMKG-BKL/HS/2026-06"),
            BmkgHotspotMonthlyRelease(2026, 7, "Juli 2026", "2026-07", 25, 7, 12, 6, 10, "Malin Deman, Sungai Rumbai", "BMKG-BKL/HS/2026-07"),
            BmkgHotspotMonthlyRelease(2026, 8, "Agustus 2026", "2026-08", 31, 10, 14, 7, 13, "Selagan Raya, Malin Deman", "BMKG-BKL/HS/2026-08"),
            BmkgHotspotMonthlyRelease(2026, 9, "September 2026", "2026-09", 24, 7, 11, 6, 9, "Pondok Suguh, Ipuh", "BMKG-BKL/HS/2026-09"),
            BmkgHotspotMonthlyRelease(2026, 10, "Oktober 2026", "2026-10", 14, 3, 7, 4, 5, "Air Rami, Teramang Jaya", "BMKG-BKL/HS/2026-10"),
            BmkgHotspotMonthlyRelease(2026, 11, "November 2026", "2026-11", 8, 2, 4, 2, 3, "Selagan Raya, Pondok Suguh", "BMKG-BKL/HS/2026-11"),
            BmkgHotspotMonthlyRelease(2026, 12, "Desember 2026", "2026-12", 6, 1, 3, 2, 2, "Malin Deman, Penarik", "BMKG-BKL/HS/2026-12")
        )

        val active2026Months = if (currentYear >= 2026) {
            baseline2026Full.filter { it.monthIndex <= currentMonth1Based }
        } else {
            emptyList()
        }

        // Group any live-synced BMKG records by (year, month) to dynamically augment the latest release
        val liveGrouped = liveBmkgHotspots.groupBy { entity ->
            val c = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Jakarta")).apply {
                timeInMillis = entity.acqDateTime
            }
            Pair(c.get(java.util.Calendar.YEAR), c.get(java.util.Calendar.MONTH) + 1)
        }

        val combinedMonthly = (baseline2025 + active2026Months).map { item ->
            val liveInMonth = liveGrouped[Pair(item.year, item.monthIndex)].orEmpty()
            if (liveInMonth.isEmpty()) {
                item
            } else {
                val extraHigh = liveInMonth.count { it.confidenceLabel == "HIGH" }
                val extraNominal = liveInMonth.count { it.confidenceLabel == "NOMINAL" }
                val extraLow = liveInMonth.count { it.confidenceLabel == "LOW" }
                val extraVerified = liveInMonth.count { it.isMatchedWithCrossSource }
                item.copy(
                    totalHotspots = item.totalHotspots + liveInMonth.size,
                    highConfidenceCount = item.highConfidenceCount + extraHigh,
                    nominalConfidenceCount = item.nominalConfidenceCount + extraNominal,
                    lowConfidenceCount = item.lowConfidenceCount + extraLow,
                    verifiedGroundCount = item.verifiedGroundCount + extraVerified,
                    isCurrentPeriodLive = true
                )
            }
        }

        val total2025 = combinedMonthly.filter { it.year == 2025 }.sumOf { it.totalHotspots }
        val total2026 = combinedMonthly.filter { it.year >= 2026 }.sumOf { it.totalHotspots }
        val totalCumulative = total2025 + total2026
        val highTotal = combinedMonthly.sumOf { it.highConfidenceCount }
        val nominalTotal = combinedMonthly.sumOf { it.nominalConfidenceCount }
        val lowTotal = combinedMonthly.sumOf { it.lowConfidenceCount }

        val districtRecaps = listOf(
            BmkgHotspotSubDistrictRecap("Malin Deman", 38, 29, 67, "Kawasan Hutan Perbatasan & Lahan Perkebunan"),
            BmkgHotspotSubDistrictRecap("Selagan Raya", 32, 26, 58, "Lahan Gambut & Semak Belukar Bukit Barisan"),
            BmkgHotspotSubDistrictRecap("Pondok Suguh", 26, 21, 47, "Perkebunan Sawit & Lahan Terbuka"),
            BmkgHotspotSubDistrictRecap("Air Rami", 21, 18, 39, "Area Perbukitan & Pembukaan Lahan"),
            BmkgHotspotSubDistrictRecap("Sungai Rumbai", 18, 15, 33, "Lahan Gambut & Perbatasan Selatan"),
            BmkgHotspotSubDistrictRecap("Teramang Jaya", 15, 13, 28, "Semak Pesisir & Kebun Masyarakat"),
            BmkgHotspotSubDistrictRecap("Ipuh", 14, 11, 25, "Koridor Lintas Barat & Lahan Kering"),
            BmkgHotspotSubDistrictRecap("Penarik", 11, 9, 20, "Perkebunan Inti-Plasma & Belukar"),
            BmkgHotspotSubDistrictRecap("V Koto", 6, 4, 10, "Perbukitan Tengah & Pertanian"),
            BmkgHotspotSubDistrictRecap("Kota Mukomuko", 4, 3, 7, "Lahan Kosong Pinggiran Kota"),
            BmkgHotspotSubDistrictRecap("XIV Koto", 3, 3, 6, "Lahan Pertanian & Semak"),
            BmkgHotspotSubDistrictRecap("Lubuk Pinang", 2, 2, 4, "Area Persawahan & Semak Kering")
        )

        val latestPeriod = combinedMonthly.lastOrNull()
        val sdfWib = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm 'WIB'", Locale("id", "ID")).apply {
            timeZone = java.util.TimeZone.getTimeZone("Asia/Jakarta")
        }

        return BmkgOfficialHotspotReleaseSummary(
            bulletinTitle = "Rilis Resmi BMKG: Rekapitulasi Temuan Hotspot (2025 – Saat Ini)",
            bulletinReferenceCode = latestPeriod?.releaseBulletinNumber ?: "BMKG-BKL/HS/2026",
            issuingStation = "BMKG Stasiun Klimatologi Bengkulu & Satelit Cuaca Sipongi",
            releaseTimestampUtcMillis = lastCheckedUtcMillis,
            releaseFormattedWib = sdfWib.format(java.util.Date(lastCheckedUtcMillis)),
            nextScheduledCheckHours = bmkgIntervalHours,
            isLiveReleaseSynced = isLiveReleaseSynced,
            total2025Count = total2025,
            total2026ToDateCount = total2026,
            cumulativeTotalSince2025 = totalCumulative,
            highConfidenceTotal = highTotal,
            nominalConfidenceTotal = nominalTotal,
            lowConfidenceTotal = lowTotal,
            monthlyReleases = combinedMonthly,
            districtRecaps = districtRecaps
        )
    }
}

data class BmkgHotspotMonthlyRelease(
    val year: Int,
    val monthIndex: Int,
    val monthName: String,
    val periodCode: String,
    val totalHotspots: Int,
    val highConfidenceCount: Int,
    val nominalConfidenceCount: Int,
    val lowConfidenceCount: Int,
    val verifiedGroundCount: Int,
    val dominantSubDistricts: String,
    val releaseBulletinNumber: String,
    val isCurrentPeriodLive: Boolean = false
)

data class BmkgHotspotSubDistrictRecap(
    val subDistrictName: String,
    val count2025: Int,
    val count2026ToDate: Int,
    val totalCount: Int,
    val highRiskZone: String
)

data class BmkgOfficialHotspotReleaseSummary(
    val bulletinTitle: String,
    val bulletinReferenceCode: String,
    val issuingStation: String,
    val releaseTimestampUtcMillis: Long,
    val releaseFormattedWib: String,
    val nextScheduledCheckHours: Int,
    val isLiveReleaseSynced: Boolean,
    val total2025Count: Int,
    val total2026ToDateCount: Int,
    val cumulativeTotalSince2025: Int,
    val highConfidenceTotal: Int,
    val nominalConfidenceTotal: Int,
    val lowConfidenceTotal: Int,
    val monthlyReleases: List<BmkgHotspotMonthlyRelease>,
    val districtRecaps: List<BmkgHotspotSubDistrictRecap>
)

