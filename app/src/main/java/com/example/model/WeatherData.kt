package com.example.model

data class CurrentWeather(
    val temperature: Double,
    val feelsLike: Double,
    val humidity: Int,
    val precipitation: Double,
    val weatherCode: Int,
    val surfacePressure: Double,
    val windSpeed: Double,
    val windDirection: Int,
    val time: String
) {
    val weatherInfo: WeatherInfo
        get() = WeatherCodeHelper.getWeatherInfo(weatherCode)

    val windDirectionCompass: String
        get() = when (windDirection) {
            in 23..67 -> "Timur Laut (NE)"
            in 68..112 -> "Timur (E)"
            in 113..157 -> "Tenggara (SE)"
            in 158..202 -> "Selatan (S)"
            in 203..247 -> "Barat Daya (SW)"
            in 248..292 -> "Barat (W)"
            in 293..337 -> "Barat Laut (NW)"
            else -> "Utara (N)"
        }
}

data class HourlyItem(
    val time: String,
    val hourLabel: String,
    val temperature: Double,
    val weatherCode: Int,
    val rainProbability: Int,
    val humidity: Int,
    val windSpeed: Double
) {
    val weatherInfo: WeatherInfo
        get() = WeatherCodeHelper.getWeatherInfo(weatherCode)
}

data class DailyItem(
    val date: String,
    val dayName: String,
    val weatherCode: Int,
    val maxTemp: Double,
    val minTemp: Double,
    val rainProbabilityMax: Int,
    val uvIndexMax: Double,
    val windSpeedMax: Double,
    val sunrise: String,
    val sunset: String
) {
    val weatherInfo: WeatherInfo
        get() = WeatherCodeHelper.getWeatherInfo(weatherCode)
}

data class MarineWeather(
    val waveHeight: Double, // in meters
    val seaCondition: String, // "Tenang", "Sedang", "Tinggi", "Sangat Tinggi"
    val windSpeedKnots: Double,
    val tideStatus: String, // "Pasang Pagi", "Surut Siang", dll.
    val fishermanSafety: String, // "Aman Melaut", "Waspada Perahu Kecil", "Bahaya Gelombang"
    val fishermanAdvice: String,
    val waterTemp: Double = 29.0
)

data class AirQuality(
    val aqi: Int,
    val pm25: Double,
    val pm10: Double,
    val status: String, // "Baik", "Sedang", "Tidak Sehat"
    val recommendation: String
)

data class WeatherAlert(
    val id: String,
    val title: String,
    val severity: String, // "Waspada", "Siaga", "Awas"
    val message: String,
    val source: String = "Prakiraan BMKG & Maritim Stasiun Bengkulu",
    val validUntil: String
)

data class WeatherFullReport(
    val district: District,
    val current: CurrentWeather,
    val hourly: List<HourlyItem>,
    val daily: List<DailyItem>,
    val marine: MarineWeather,
    val airQuality: AirQuality,
    val alert: WeatherAlert?,
    val lastUpdated: Long = System.currentTimeMillis(),
    val isFromCache: Boolean = false
)

data class HistoricalWeatherRecord(
    val date: String, // YYYY-MM-DD
    val districtName: String,
    val tempMean: Double,
    val tempMax: Double,
    val tempMin: Double,
    val precipitationSum: Double, // mm
    val weatherCode: Int,
    val windSpeedMax: Double,
    val summaryText: String,
    val comparisonWithAverage: String
) {
    val weatherInfo: WeatherInfo
        get() = WeatherCodeHelper.getWeatherInfo(weatherCode)
}

data class CommunityWeatherReport(
    val id: String,
    val districtId: String,
    val districtName: String,
    val reporterName: String,
    val weatherCondition: String,
    val notes: String,
    val timestamp: Long,
    val upvotes: Int = 0
)

data class DetailedEarlyWarning(
    val id: String,
    val title: String,
    val severity: String, // "AWAS", "SIAGA", "WASPADA"
    val category: String, // "Gelombang Tinggi", "Hujan Lebat", "Petir & Angin Kencang"
    val affectedDistricts: List<String>,
    val validUntil: String,
    val description: String,
    val safetyInstructions: List<String>,
    val bpbdContact: String = "Posko BPBD Mukomuko: 0737-71112 / Call Center 112"
)
