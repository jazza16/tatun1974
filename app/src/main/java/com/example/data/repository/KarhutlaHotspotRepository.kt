package com.example.data.repository

import android.content.Context
import com.example.data.api.BmkgHotspotService
import com.example.data.api.FirmsSensorSource
import com.example.data.api.NasaFirmsHotspotService
import com.example.data.local.AppDatabase
import com.example.data.local.KarhutlaHotspotDao
import com.example.data.local.KarhutlaHotspotEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

data class KarhutlaSyncSummary(
    val firmsFetchedCount: Int,
    val bmkgFetchedCount: Int,
    val matchedPairsCount: Int,
    val totalActiveCount: Int,
    val retentionDays: Int,
    val firmsIntervalHours: Int,
    val bmkgIntervalHours: Int,
    val lastFirmsSyncUtcMillis: Long,
    val lastBmkgSyncUtcMillis: Long,
    val statusMessage: String
)

/**
 * Repository coordinating NASA FIRMS (VIIRS/MODIS) and BMKG Hotspot synchronization,
 * deterministic `INSERT IGNORE` storage, atomic `@Transaction` cross-source matching,
 * and 30–90 day periodic data retention cleanup.
 */
class KarhutlaHotspotRepository(context: Context) {
    private val appContext = context.applicationContext
    private val dao: KarhutlaHotspotDao = AppDatabase.getInstance(appContext).karhutlaHotspotDao()
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun observeHotspotsSince(sinceEpochMillisUtc: Long): Flow<List<KarhutlaHotspotEntity>> {
        return dao.observeHotspotsSince(sinceEpochMillisUtc)
    }

    fun getFirmsIntervalHours(): Int {
        return prefs.getInt(KEY_FIRMS_INTERVAL_HOURS, 2).coerceIn(1, 3)
    }

    fun setFirmsIntervalHours(hours: Int) {
        prefs.edit().putInt(KEY_FIRMS_INTERVAL_HOURS, hours.coerceIn(1, 3)).apply()
    }

    fun getBmkgIntervalHours(): Int {
        return prefs.getInt(KEY_BMKG_INTERVAL_HOURS, 4).coerceIn(3, 6)
    }

    fun setBmkgIntervalHours(hours: Int) {
        prefs.edit().putInt(KEY_BMKG_INTERVAL_HOURS, hours.coerceIn(3, 6)).apply()
    }

    fun getRetentionDays(): Int {
        return prefs.getInt(KEY_RETENTION_DAYS, 60).coerceIn(30, 90)
    }

    fun setRetentionDays(days: Int) {
        prefs.edit().putInt(KEY_RETENTION_DAYS, days.coerceIn(30, 90)).apply()
    }

    fun getLastFirmsSyncUtcMillis(): Long = prefs.getLong(KEY_LAST_FIRMS_SYNC_UTC, 0L)

    fun getLastBmkgSyncUtcMillis(): Long = prefs.getLong(KEY_LAST_BMKG_SYNC_UTC, 0L)

    /**
     * Synchronizes NASA FIRMS (VIIRS/MODIS) hotspots, stores with `INSERT IGNORE` using deterministic
     * `sourceId = "$satellite|$acqDateTime|$lat|$lon"`, runs cross-source matching with BMKG, and
     * prunes records older than 30–90 days inside a single `@Transaction`.
     */
    suspend fun syncFirmsAndMatch(
        sensor: FirmsSensorSource = FirmsSensorSource.VIIRS_SNPP,
        dayRange: Int = 2,
        enableSimulation: Boolean = false
    ): Int = withContext(Dispatchers.IO) {
        val nowUtc = System.currentTimeMillis()
        val retentionDays = getRetentionDays()
        val retentionCutoffUtc = nowUtc - (retentionDays.toLong() * 24L * 3600_000L)
        val lookbackUtc = nowUtc - (dayRange.coerceIn(1, 7).toLong() * 24L * 3600_000L)

        val firmsEntities = NasaFirmsHotspotService.fetchFirmsEntitiesForMukomuko(
            sensor = sensor,
            dayRange = dayRange,
            enableSimulation = enableSimulation
        )

        val matchedCount = dao.syncAndMatchHotspotsTransaction(
            newFirmsHotspots = firmsEntities,
            newBmkgHotspots = emptyList(),
            matchingLookbackMillis = lookbackUtc,
            retentionCutoffMillisUtc = retentionCutoffUtc
        )

        prefs.edit().putLong(KEY_LAST_FIRMS_SYNC_UTC, nowUtc).apply()
        matchedCount
    }

    /**
     * Politely synchronizes BMKG hotspots, stores with `INSERT IGNORE` using deterministic
     * `sourceId = "$acqDateTime|$lat|$lon|$confidenceLabel"`, runs cross-source matching with FIRMS,
     * and prunes records older than 30–90 days inside a single `@Transaction`.
     */
    suspend fun syncBmkgAndMatch(
        forceRefresh: Boolean = false,
        dayRange: Int = 2,
        enableSimulation: Boolean = false
    ): Int = withContext(Dispatchers.IO) {
        val nowUtc = System.currentTimeMillis()
        val retentionDays = getRetentionDays()
        val retentionCutoffUtc = nowUtc - (retentionDays.toLong() * 24L * 3600_000L)
        val lookbackUtc = nowUtc - (dayRange.coerceIn(1, 7).toLong() * 24L * 3600_000L)

        val bmkgEntities = BmkgHotspotService.fetchBmkgHotspotsForMukomuko(
            context = appContext,
            forceRefresh = forceRefresh,
            enableSimulation = enableSimulation
        )

        val matchedCount = dao.syncAndMatchHotspotsTransaction(
            newFirmsHotspots = emptyList(),
            newBmkgHotspots = bmkgEntities,
            matchingLookbackMillis = lookbackUtc,
            retentionCutoffMillisUtc = retentionCutoffUtc
        )

        // Also update the official BMKG 2025-present release recap during periodic WorkManager runs
        BmkgHotspotService.fetchAndSyncOfficialBmkgReleaseSince2025(
            context = appContext,
            liveBmkgEntities = bmkgEntities,
            bmkgIntervalHours = getBmkgIntervalHours(),
            forceRefresh = false
        )

        prefs.edit().putLong(KEY_LAST_BMKG_SYNC_UTC, nowUtc).apply()
        matchedCount
    }

    /**
     * Loads and synchronizes the Official BMKG Hotspot Release Summary from January 2025 to the present,
     * merging stored BMKG detections in Room with the latest BMKG release bulletin.
     */
    suspend fun getOfficialBmkgReleaseSummarySince2025(
        forceRefresh: Boolean = false
    ): com.example.data.api.BmkgOfficialHotspotReleaseSummary = withContext(Dispatchers.IO) {
        val jan2025UtcMillis = 1735689600_000L // 2025-01-01T00:00:00Z
        val storedBmkgSince2025 = dao.getHotspotsBySourceSince(
            sourceType = KarhutlaHotspotEntity.SOURCE_BMKG,
            sinceEpochMillisUtc = jan2025UtcMillis
        )
        BmkgHotspotService.fetchAndSyncOfficialBmkgReleaseSince2025(
            context = appContext,
            liveBmkgEntities = storedBmkgSince2025,
            bmkgIntervalHours = getBmkgIntervalHours(),
            forceRefresh = forceRefresh
        )
    }

    /**
     * Synchronizes both NASA FIRMS and BMKG sources, then executes deterministic insertion,
     * spatio-temporal cross-matching, and 30–90 day retention cleanup inside a single `@Transaction`.
     */
    suspend fun syncBothSourcesAndMatchInTransaction(
        sensor: FirmsSensorSource = FirmsSensorSource.VIIRS_SNPP,
        dayRange: Int = 2,
        forceBmkgCheck: Boolean = false,
        enableSimulation: Boolean = false
    ): KarhutlaSyncSummary = withContext(Dispatchers.IO) {
        val nowUtc = System.currentTimeMillis()
        val refHourUtc = (nowUtc / 3600_000L) * 3600_000L
        val retentionDays = getRetentionDays()
        val retentionCutoffUtc = nowUtc - (retentionDays.toLong() * 24L * 3600_000L)
        val lookbackUtc = nowUtc - (dayRange.coerceIn(1, 7).toLong() * 24L * 3600_000L)

        if (!enableSimulation) {
            dao.deleteSimulationHotspots()
        }

        val firmsEntities = if (enableSimulation) {
            NasaFirmsHotspotService.getTrainingSimulationHotspots(sensor, refHourUtc).map { it.toEntity() }
        } else {
            NasaFirmsHotspotService.fetchFirmsEntitiesForMukomuko(
                sensor = sensor,
                dayRange = dayRange,
                enableSimulation = false
            )
        }

        val bmkgEntities = if (enableSimulation) {
            BmkgHotspotService.getSimulationBmkgHotspots(refHourUtc)
        } else {
            BmkgHotspotService.fetchBmkgHotspotsForMukomuko(
                context = appContext,
                forceRefresh = forceBmkgCheck,
                enableSimulation = false
            )
        }

        val matchedPairs = dao.syncAndMatchHotspotsTransaction(
            newFirmsHotspots = firmsEntities,
            newBmkgHotspots = bmkgEntities,
            matchingLookbackMillis = lookbackUtc,
            retentionCutoffMillisUtc = retentionCutoffUtc
        )

        prefs.edit()
            .putLong(KEY_LAST_FIRMS_SYNC_UTC, nowUtc)
            .putLong(KEY_LAST_BMKG_SYNC_UTC, nowUtc)
            .apply()

        val totalActive = dao.getTotalCount()
        val firmsInterval = getFirmsIntervalHours()
        val bmkgInterval = getBmkgIntervalHours()

        val msg = if (enableSimulation) {
            "Simulasi Posko: ${firmsEntities.size} titik FIRMS & ${bmkgEntities.size} titik BMKG disinkronkan • $matchedPairs pasang titik TERKONFIRMASI GANDA (@Transaction)."
        } else {
            "WorkManager Aktif (FIRMS tiap ${firmsInterval}j, BMKG tiap ${bmkgInterval}j) • $matchedPairs titik Terkonfirmasi Ganda • Retensi $retentionDays hari."
        }

        KarhutlaSyncSummary(
            firmsFetchedCount = firmsEntities.size,
            bmkgFetchedCount = bmkgEntities.size,
            matchedPairsCount = matchedPairs,
            totalActiveCount = totalActive,
            retentionDays = retentionDays,
            firmsIntervalHours = firmsInterval,
            bmkgIntervalHours = bmkgInterval,
            lastFirmsSyncUtcMillis = nowUtc,
            lastBmkgSyncUtcMillis = nowUtc,
            statusMessage = msg
        )
    }

    /**
     * Runs only the cross-source matching and retention cleanup inside a `@Transaction`
     * (used by the final step of chained WorkManager tasks after FIRMS and BMKG workers finish).
     */
    suspend fun runCrossSourceMatchingAndRetentionOnly(
        dayRange: Int = 2
    ): Int = withContext(Dispatchers.IO) {
        val nowUtc = System.currentTimeMillis()
        val retentionDays = getRetentionDays()
        val retentionCutoffUtc = nowUtc - (retentionDays.toLong() * 24L * 3600_000L)
        val lookbackUtc = nowUtc - (dayRange.coerceIn(1, 7).toLong() * 24L * 3600_000L)

        dao.syncAndMatchHotspotsTransaction(
            newFirmsHotspots = emptyList(),
            newBmkgHotspots = emptyList(),
            matchingLookbackMillis = lookbackUtc,
            retentionCutoffMillisUtc = retentionCutoffUtc
        )
    }

    companion object {
        private const val PREFS_NAME = "karhutla_hotspot_repo_prefs"
        private const val KEY_FIRMS_INTERVAL_HOURS = "firms_interval_hours"
        private const val KEY_BMKG_INTERVAL_HOURS = "bmkg_interval_hours"
        private const val KEY_RETENTION_DAYS = "retention_days"
        private const val KEY_LAST_FIRMS_SYNC_UTC = "last_firms_sync_utc"
        private const val KEY_LAST_BMKG_SYNC_UTC = "last_bmkg_sync_utc"
    }
}
