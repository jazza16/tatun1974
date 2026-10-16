package com.example.data.api

import retrofit2.http.GET
import retrofit2.http.Query

interface OpenMeteoApi {
    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,weather_code,surface_pressure,wind_speed_10m,wind_direction_10m",
        @Query("hourly") hourly: String = "temperature_2m,relative_humidity_2m,precipitation_probability,weather_code,wind_speed_10m,uv_index",
        @Query("daily") daily: String = "weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,uv_index_max,wind_speed_10m_max,sunrise,sunset",
        @Query("timezone") timezone: String = "Asia/Jakarta",
        @Query("forecast_days") forecastDays: Int = 7
    ): OpenMeteoForecastResponse
}

interface AirQualityApi {
    @GET("v1/air-quality")
    suspend fun getAirQuality(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "pm10,pm2_5,european_aqi,us_aqi",
        @Query("timezone") timezone: String = "Asia/Jakarta"
    ): OpenMeteoAirQualityResponse
}

interface ArchiveWeatherApi {
    @GET("v1/archive")
    suspend fun getHistoricalWeather(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("start_date") startDate: String,
        @Query("end_date") endDate: String,
        @Query("daily") daily: String = "weather_code,temperature_2m_max,temperature_2m_min,temperature_2m_mean,precipitation_sum,wind_speed_10m_max",
        @Query("timezone") timezone: String = "Asia/Jakarta"
    ): OpenMeteoArchiveResponse
}
