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

    @Query("SELECT * FROM weather_cache")
    suspend fun getAllCached(): List<WeatherCacheEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCache(entity: WeatherCacheEntity)

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

    // Historical Cache
    @Query("SELECT * FROM historical_cache WHERE districtId = :districtId AND date = :date LIMIT 1")
    suspend fun getHistoricalCache(districtId: String, date: String): HistoricalWeatherEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoricalCache(entity: HistoricalWeatherEntity)
}
