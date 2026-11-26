package com.example.service

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.api.NetworkClient
import com.example.data.local.AppDatabase
import com.example.data.local.WeatherCacheEntity
import com.example.model.DetailedEarlyWarning
import com.example.model.MukomukoDistricts
import com.example.util.WeatherNotificationHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * Background WorkManager Service running periodically every 3 hours.
 * Fetches BMKG / Open-Meteo meteorological updates for Kabupaten Mukomuko,
 * compares current readings against Room SQLite historical cache,
 * and broadcasts immediate emergency alert notifications upon detecting significant anomalies.
 */
class WeatherReportAggregator(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val database = AppDatabase.getInstance(appContext)
            val weatherDao = database.weatherDao()
            val api = NetworkClient.weatherApi

            val primaryDistrict = MukomukoDistricts.list.firstOrNull() ?: return Result.success()

            // Fetch live weather data
            val response = api.getForecast(
                latitude = primaryDistrict.latitude,
                longitude = primaryDistrict.longitude
            )

            val current = response.current ?: return Result.success()
            val cached = weatherDao.getCachedWeather(primaryDistrict.id)

            val currentTemp = current.temperature2m ?: 28.0
            val currentWind = current.windSpeed10m ?: 10.0
            val currentPrecip = current.precipitation ?: 0.0
            val currentPressure = current.surfacePressure ?: 1012.0

            var anomalyDetected = false
            var anomalyTitle = ""
            var anomalyDesc = ""
            var severity = "WASPADA"

            if (cached != null) {
                val tempDiff = abs(currentTemp - cached.temperature)
                val windDiff = currentWind - cached.windSpeed
                val pressureDrop = cached.surfacePressure - currentPressure

                if (currentPrecip >= 15.0) {
                    anomalyDetected = true
                    severity = if (currentPrecip >= 30.0) "AWAS" else "SIAGA"
                    anomalyTitle = "Peringatan Curah Hujan Ekstrem"
                    anomalyDesc = "Terdeteksi lonjakan curah hujan sebesar $currentPrecip mm/jam di ${primaryDistrict.name}. Waspadai luapan sungai dan genangan air."
                } else if (windDiff >= 18.0 || currentWind >= 45.0) {
                    anomalyDetected = true
                    severity = "SIAGA"
                    anomalyTitle = "Peringatan Anomali Angin Kencang"
                    anomalyDesc = "Kecepatan angin meningkat signifikan menjadi ${currentWind.toInt()} km/jam. Waspada pohon tumbang dan gelombang laut."
                } else if (pressureDrop >= 3.5) {
                    anomalyDetected = true
                    severity = "SIAGA"
                    anomalyTitle = "Penurunan Tekanan Atmosfer Cepat"
                    anomalyDesc = "Tekanan udara turun ${"%.1f".format(pressureDrop)} hPa secara tiba-tiba, mengindikasikan potensi terbentuknya awan badai konvektif."
                } else if (tempDiff >= 5.0) {
                    anomalyDetected = true
                    severity = "WASPADA"
                    anomalyTitle = "Fluktuasi Suhu Signifikan"
                    anomalyDesc = "Suhu udara berubah drastis ${"%.1f".format(tempDiff)}°C dari pencatatan sebelumnya menjadi $currentTemp°C."
                }
            } else if (currentPrecip >= 20.0 || currentWind >= 40.0) {
                anomalyDetected = true
                severity = "SIAGA"
                anomalyTitle = "Deteksi Cuaca Ekstrem Mukomuko"
                anomalyDesc = "Sensor cuaca mendeteksi intensitas tinggi: Hujan $currentPrecip mm/j, Angin ${currentWind.toInt()} km/j."
            }

            // Save updated cache to Room
            val newCache = WeatherCacheEntity(
                districtId = primaryDistrict.id,
                temperature = currentTemp,
                feelsLike = current.apparentTemperature ?: currentTemp,
                weatherCode = current.weatherCode ?: 1,
                humidity = current.relativeHumidity2m ?: 80,
                windSpeed = currentWind,
                windDirection = current.windDirection10m ?: 180,
                surfacePressure = currentPressure,
                time = current.time ?: "",
                cachedAt = System.currentTimeMillis()
            )
            weatherDao.insertCache(newCache)

            // Dispatch color-triage notification if an anomaly was detected
            if (anomalyDetected) {
                val timeStr = SimpleDateFormat("HH:mm 'WIB'", Locale.forLanguageTag("id-ID")).format(Date(System.currentTimeMillis() + 3 * 3600 * 1000L))
                val warning = DetailedEarlyWarning(
                    id = "ANOMALY-${System.currentTimeMillis()}",
                    title = anomalyTitle,
                    category = "Anomali Meteorologi",
                    severity = severity,
                    validUntil = timeStr,
                    description = anomalyDesc,
                    affectedDistricts = listOf(primaryDistrict.name, "Pesisir Mukomuko"),
                    safetyInstructions = listOf(
                        "Hindari berteduh di bawah pohon rindang atau tiang listrik.",
                        "Nelayan perahu motor tradisional diimbau menunda melaut saat badai."
                    )
                )
                val triageSignal = com.example.util.DisasterTriageNotificationManager.evaluateEarlyWarnings(
                    warnings = listOf(warning),
                    districtName = primaryDistrict.name
                )
                val dispatched = com.example.util.DisasterTriageNotificationManager.evaluateAndNotify(
                    context = appContext,
                    signal = triageSignal
                )
                if (dispatched != com.example.util.TriageDispatchResult.POSTED &&
                    dispatched != com.example.util.TriageDispatchResult.SKIPPED_COOLDOWN_DUPLICATE &&
                    dispatched != com.example.util.TriageDispatchResult.SKIPPED_BELOW_THRESHOLD &&
                    dispatched != com.example.util.TriageDispatchResult.SKIPPED_DISABLED
                ) {
                    WeatherNotificationHelper.postWeatherAlertNotification(
                        context = appContext,
                        warning = warning,
                        targetDistrictName = primaryDistrict.name
                    )
                }
            }

            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "MukomukoWeatherReportAggregatorWork"

        fun schedulePeriodic(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val periodicRequest = PeriodicWorkRequestBuilder<WeatherReportAggregator>(
                3, TimeUnit.HOURS,
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )
        }
    }
}
