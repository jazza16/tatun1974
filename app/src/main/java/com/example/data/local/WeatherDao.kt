package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WeatherDao {
    @Query("SELECT * FROM weather_cache WHERE districtId = :districtId LIMIT 1")
    suspend fun getCachedWeather(districtId: String): WeatherCacheEntity?

    @Query("SELECT * FROM weather_cache WHERE districtId = :districtId LIMIT 1")
    fun observeCachedWeather(districtId: String): Flow<WeatherCacheEntity?>

    @Query("SELECT * FROM weather_cache ORDER BY districtId ASC")
    suspend fun getAllCached(): List<WeatherCacheEntity>

    @Query("SELECT * FROM weather_cache ORDER BY districtId ASC")
    fun observeAllCachedWeather(): Flow<List<WeatherCacheEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCache(entity: WeatherCacheEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllCache(entities: List<WeatherCacheEntity>)

    @Query("SELECT districtId FROM favorite_districts WHERE isFavorite = 1")
    fun getFavoriteDistrictIds(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setFavorite(favorite: FavoriteDistrictEntity)

    @Query("DELETE FROM favorite_districts WHERE districtId = :districtId")
    suspend fun removeFavorite(districtId: String)

    // Community Reports
    @Query("SELECT * FROM community_reports ORDER BY timestamp DESC")
    fun getAllCommunityReports(): Flow<List<CommunityReportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommunityReport(report: CommunityReportEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllCommunityReports(reports: List<CommunityReportEntity>)

    @Query("UPDATE community_reports SET upvotes = upvotes + 1 WHERE id = :reportId")
    suspend fun upvoteReport(reportId: String)

    // Citizen Disaster Reports
    @Query("SELECT * FROM citizen_disaster_reports ORDER BY timestamp DESC")
    fun getAllCitizenDisasterReports(): Flow<List<CitizenDisasterReportEntity>>

    @Query("SELECT * FROM citizen_disaster_reports ORDER BY timestamp DESC")
    suspend fun getAllCitizenDisasterReportsSync(): List<CitizenDisasterReportEntity>

    @Query("SELECT * FROM citizen_disaster_reports WHERE id = :id LIMIT 1")
    suspend fun getCitizenReportById(id: String): CitizenDisasterReportEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCitizenDisasterReport(report: CitizenDisasterReportEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllCitizenDisasterReports(reports: List<CitizenDisasterReportEntity>)

    @Query("UPDATE citizen_disaster_reports SET status = :newStatus WHERE id = :reportId")
    suspend fun updateCitizenReportStatus(reportId: String, newStatus: String)

    @Query("UPDATE citizen_disaster_reports SET status = :newStatus, additionalNotes = :newNotes WHERE id = :reportId")
    suspend fun updateCitizenReportStatusAndNotes(reportId: String, newStatus: String, newNotes: String)

    @Query("SELECT * FROM citizen_disaster_reports WHERE status = :status ORDER BY timestamp DESC")
    fun getCitizenReportsByStatus(status: String): Flow<List<CitizenDisasterReportEntity>>

    @Query("SELECT * FROM citizen_disaster_reports WHERE district = :district ORDER BY timestamp DESC")
    fun getCitizenReportsByDistrict(district: String): Flow<List<CitizenDisasterReportEntity>>

    @Query("SELECT * FROM citizen_disaster_reports WHERE disasterType = :disasterType ORDER BY timestamp DESC")
    fun getCitizenReportsByType(disasterType: String): Flow<List<CitizenDisasterReportEntity>>

    @Query("SELECT * FROM citizen_disaster_reports WHERE district = :district AND status = :status ORDER BY timestamp DESC")
    fun getCitizenReportsByDistrictAndStatus(district: String, status: String): Flow<List<CitizenDisasterReportEntity>>

    @Query("SELECT COUNT(*) FROM citizen_disaster_reports")
    suspend fun getCitizenReportCount(): Int

    @Query("SELECT COUNT(*) FROM citizen_disaster_reports WHERE status = :status")
    suspend fun getCitizenReportCountByStatus(status: String): Int

    @Query("DELETE FROM citizen_disaster_reports WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteReportsOlderThan(cutoffTimestamp: Long): Int

    // Historical Cache
    @Query("SELECT * FROM historical_cache WHERE districtId = :districtId AND date = :date LIMIT 1")
    suspend fun getHistoricalCache(districtId: String, date: String): HistoricalWeatherEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoricalCache(entity: HistoricalWeatherEntity)

    // Hazard Risk Zones & Evacuation Waypoints (100% Offline Persistence)
    @Query("SELECT * FROM hazard_risk_zones")
    fun getAllHazardZones(): Flow<List<HazardRiskZoneEntity>>

    @Query("SELECT * FROM hazard_risk_zones")
    suspend fun getAllHazardZonesSync(): List<HazardRiskZoneEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllHazardZones(zones: List<HazardRiskZoneEntity>)
}
