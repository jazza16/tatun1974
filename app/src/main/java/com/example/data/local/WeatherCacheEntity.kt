package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "weather_cache")
data class WeatherCacheEntity(
    @PrimaryKey
    val districtId: String,
    val temperature: Double,
    val feelsLike: Double,
    val weatherCode: Int,
    val humidity: Int,
    val windSpeed: Double,
    val windDirection: Int,
    val surfacePressure: Double,
    val time: String,
    val cachedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "favorite_districts")
data class FavoriteDistrictEntity(
    @PrimaryKey
    val districtId: String,
    val isFavorite: Boolean = true
)

@Entity(
    tableName = "community_reports",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["districtId"]),
        Index(value = ["districtId", "timestamp"])
    ]
)
data class CommunityReportEntity(
    @PrimaryKey
    val id: String,
    val districtId: String,
    val districtName: String,
    val reporterName: String,
    val weatherCondition: String, // e.g. "Hujan Deras", "Cerah Berawan", "Angin Kencang", "Genangan Air"
    val notes: String,
    val timestamp: Long = System.currentTimeMillis(),
    val upvotes: Int = 0
)

@Entity(tableName = "historical_cache")
data class HistoricalWeatherEntity(
    @PrimaryKey
    val id: String, // districtId_date
    val districtId: String,
    val date: String,
    val tempMean: Double,
    val tempMax: Double,
    val tempMin: Double,
    val precipitationSum: Double,
    val weatherCode: Int,
    val windSpeedMax: Double
)
