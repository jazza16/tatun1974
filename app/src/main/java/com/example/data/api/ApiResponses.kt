package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OpenMeteoForecastResponse(
    val current: CurrentUnitsDto?,
    val hourly: HourlyDto?,
    val daily: DailyDto?
)

@JsonClass(generateAdapter = true)
data class CurrentUnitsDto(
    @param:Json(name = "temperature_2m") val temperature2m: Double?,
    @param:Json(name = "relative_humidity_2m") val relativeHumidity2m: Int?,
    @param:Json(name = "apparent_temperature") val apparentTemperature: Double?,
    val precipitation: Double?,
    @param:Json(name = "weather_code") val weatherCode: Int?,
    @param:Json(name = "surface_pressure") val surfacePressure: Double?,
    @param:Json(name = "wind_speed_10m") val windSpeed10m: Double?,
    @param:Json(name = "wind_direction_10m") val windDirection10m: Int?,
    val time: String?
)

@JsonClass(generateAdapter = true)
data class HourlyDto(
    val time: List<String>?,
    @param:Json(name = "temperature_2m") val temperature2m: List<Double>?,
    @param:Json(name = "relative_humidity_2m") val relativeHumidity2m: List<Int>?,
    @param:Json(name = "precipitation_probability") val precipitationProbability: List<Int>?,
    @param:Json(name = "weather_code") val weatherCode: List<Int>?,
    @param:Json(name = "wind_speed_10m") val windSpeed10m: List<Double>?,
    @param:Json(name = "uv_index") val uvIndex: List<Double>?
)

@JsonClass(generateAdapter = true)
data class DailyDto(
    val time: List<String>?,
    @param:Json(name = "weather_code") val weatherCode: List<Int>?,
    @param:Json(name = "temperature_2m_max") val temperature2mMax: List<Double>?,
    @param:Json(name = "temperature_2m_min") val temperature2mMin: List<Double>?,
    @param:Json(name = "precipitation_probability_max") val precipitationProbabilityMax: List<Int>?,
    @param:Json(name = "precipitation_sum") val precipitationSum: List<Double>? = null,
    @param:Json(name = "uv_index_max") val uvIndexMax: List<Double>?,
    @param:Json(name = "wind_speed_10m_max") val windSpeed10mMax: List<Double>?,
    val sunrise: List<String>?,
    val sunset: List<String>?
)

@JsonClass(generateAdapter = true)
data class OpenMeteoAirQualityResponse(
    val current: AirQualityCurrentDto?
)

@JsonClass(generateAdapter = true)
data class AirQualityCurrentDto(
    val pm10: Double?,
    val pm2_5: Double?,
    @param:Json(name = "us_aqi") val usAqi: Int?,
    @param:Json(name = "european_aqi") val europeanAqi: Int?
)

@JsonClass(generateAdapter = true)
data class OpenMeteoArchiveResponse(
    val daily: ArchiveDailyDto?
)

@JsonClass(generateAdapter = true)
data class ArchiveDailyDto(
    val time: List<String>?,
    @param:Json(name = "weather_code") val weatherCode: List<Int>?,
    @param:Json(name = "temperature_2m_max") val temperature2mMax: List<Double>?,
    @param:Json(name = "temperature_2m_min") val temperature2mMin: List<Double>?,
    @param:Json(name = "temperature_2m_mean") val temperature2mMean: List<Double>?,
    @param:Json(name = "precipitation_sum") val precipitationSum: List<Double>?,
    @param:Json(name = "wind_speed_10m_max") val windSpeed10mMax: List<Double>?
)
