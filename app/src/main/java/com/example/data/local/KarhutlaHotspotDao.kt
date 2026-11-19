package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * DAO for KarhutlaHotspotEntity supporting:
 * - Deterministic duplicate prevention via `@Insert(onConflict = OnConflictStrategy.IGNORE)`
 * - Atomic cross-source spatio-temporal matching (`@Transaction`) between FIRMS (VIIRS/MODIS) and BMKG
 * - Periodic retention pruning for records older than 30–90 days using UTC epoch milliseconds.
 */
@Dao
interface KarhutlaHotspotDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(hotspot: KarhutlaHotspotEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnoreAll(hotspots: List<KarhutlaHotspotEntity>): List<Long>

    @Query(
        """
        SELECT * FROM karhutla_hotspots
        WHERE acqDateTime >= :sinceEpochMillisUtc
        ORDER BY isMatchedWithCrossSource DESC, acqDateTime DESC
        """
    )
    fun observeHotspotsSince(sinceEpochMillisUtc: Long): Flow<List<KarhutlaHotspotEntity>>

    @Query(
        """
        SELECT * FROM karhutla_hotspots
        ORDER BY isMatchedWithCrossSource DESC, acqDateTime DESC
        """
    )
    fun observeAllHotspots(): Flow<List<KarhutlaHotspotEntity>>

    @Query(
        """
        SELECT * FROM karhutla_hotspots
        WHERE sourceType = :sourceType AND acqDateTime >= :sinceEpochMillisUtc
        ORDER BY acqDateTime DESC
        """
    )
    suspend fun getHotspotsBySourceSince(
        sourceType: String,
        sinceEpochMillisUtc: Long
    ): List<KarhutlaHotspotEntity>

    @Query(
        """
        UPDATE karhutla_hotspots
        SET isMatchedWithCrossSource = 1,
            matchedPartnerSourceId = :partnerSourceId,
            matchedDistanceKm = :distanceKm,
            verificationStatus = :status
        WHERE sourceId = :sourceId
        """
    )
    suspend fun updateMatchState(
        sourceId: String,
        partnerSourceId: String,
        distanceKm: Double,
        status: String = KarhutlaHotspotEntity.STATUS_VERIFIED_MATCH
    )

    /**
     * Data retention query: deletes hotspot records older than the specified UTC cutoff timestamp
     * (30–90 days retention window).
     */
    @Query("DELETE FROM karhutla_hotspots WHERE acqDateTime < :cutoffEpochMillisUtc")
    suspend fun deleteHotspotsOlderThan(cutoffEpochMillisUtc: Long): Int

    @Query("DELETE FROM karhutla_hotspots WHERE isSimulation = 1")
    suspend fun deleteSimulationHotspots(): Int

    @Query("SELECT COUNT(*) FROM karhutla_hotspots")
    suspend fun getTotalCount(): Int

    @Query("SELECT COUNT(*) FROM karhutla_hotspots WHERE isMatchedWithCrossSource = 1")
    suspend fun getMatchedCount(): Int

    /**
     * Runs insertion, cross-source FIRMS <-> BMKG matching, and 30–90 day data retention cleanup
     * atomically inside a single Room `@Transaction`.
     *
     * Matching criteria:
     * - One record from `FIRMS` and one from `BMKG`
     * - Temporal difference `abs(firms.acqDateTime - bmkg.acqDateTime) <= maxTimeDiffMillis` (default 6 hours)
     * - Spatial Haversine distance `<= maxDistanceKm` (default 2.5 km)
     */
    @Transaction
    suspend fun syncAndMatchHotspotsTransaction(
        newFirmsHotspots: List<KarhutlaHotspotEntity>,
        newBmkgHotspots: List<KarhutlaHotspotEntity>,
        matchingLookbackMillis: Long,
        retentionCutoffMillisUtc: Long,
        maxTimeDiffMillis: Long = 6 * 3600_000L,
        maxDistanceKm: Double = 2.5
    ): Int {
        if (newFirmsHotspots.isNotEmpty()) {
            insertIgnoreAll(newFirmsHotspots)
        }
        if (newBmkgHotspots.isNotEmpty()) {
            insertIgnoreAll(newBmkgHotspots)
        }

        // Prune records older than 30-90 days retention window
        deleteHotspotsOlderThan(retentionCutoffMillisUtc)

        // Query active FIRMS & BMKG hotspots within lookback window for cross-matching
        val firmsList = getHotspotsBySourceSince(
            sourceType = KarhutlaHotspotEntity.SOURCE_FIRMS,
            sinceEpochMillisUtc = matchingLookbackMillis
        )
        val bmkgList = getHotspotsBySourceSince(
            sourceType = KarhutlaHotspotEntity.SOURCE_BMKG,
            sinceEpochMillisUtc = matchingLookbackMillis
        )

        var matchedPairs = 0
        for (firms in firmsList) {
            var bestBmkg: KarhutlaHotspotEntity? = null
            var bestDistKm = Double.MAX_VALUE

            for (bmkg in bmkgList) {
                val timeDiff = abs(firms.acqDateTime - bmkg.acqDateTime)
                if (timeDiff <= maxTimeDiffMillis) {
                    val distKm = haversineDistanceKm(firms.lat, firms.lon, bmkg.lat, bmkg.lon)
                    if (distKm <= maxDistanceKm && distKm < bestDistKm) {
                        bestDistKm = distKm
                        bestBmkg = bmkg
                    }
                }
            }

            if (bestBmkg != null) {
                updateMatchState(
                    sourceId = firms.sourceId,
                    partnerSourceId = bestBmkg.sourceId,
                    distanceKm = bestDistKm,
                    status = KarhutlaHotspotEntity.STATUS_VERIFIED_MATCH
                )
                updateMatchState(
                    sourceId = bestBmkg.sourceId,
                    partnerSourceId = firms.sourceId,
                    distanceKm = bestDistKm,
                    status = KarhutlaHotspotEntity.STATUS_VERIFIED_MATCH
                )
                matchedPairs++
            }
        }

        return matchedPairs
    }

    companion object {
        private fun haversineDistanceKm(
            lat1: Double,
            lon1: Double,
            lat2: Double,
            lon2: Double
        ): Double {
            val r = 6371.0
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2.0)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            return r * c
        }
    }
}
