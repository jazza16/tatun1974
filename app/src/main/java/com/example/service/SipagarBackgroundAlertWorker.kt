package com.example.service

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.local.AppDatabase
import com.example.data.local.CitizenDisasterReportEntity
import com.example.data.repository.KarhutlaHotspotRepository
import com.example.data.repository.WeatherRepository
import com.example.data.sync.OfficerPushNotificationHelper
import com.example.model.BengkuluEarthquakeScope
import com.example.model.CitizenDisasterReport
import com.example.model.CitizenReportStatusHelper
import com.example.model.MukomukoDistricts
import com.example.model.TidalDataHelper
import com.example.util.DisasterTriageNotificationManager
import com.example.util.EarthquakeNotificationHelper
import com.example.util.TriageDispatchResult
import com.example.util.WeatherNotificationHelper
import java.util.concurrent.TimeUnit

/**
 * Worker Latar Belakang 24 Jam SIPAGAR KITA (BPBD Kabupaten Mukomuko).
 *
 * Memastikan seluruh notifikasi terkait:
 * 1. Laporan warga yang masuk (`citizen_disaster_reports` dari Cloud Firestore),
 * 2. Kejadian bencana nyata (`Gempa Bumi & Tsunami WRS BMKG`),
 * 3. Potensi ancaman (`Peringatan Dini Cuaca, Cuaca Ekstrem, Gelombang Laut Maritim, Pasang Rob, Kualitas Udara, dan Hotspot Karhutla`),
 * tetap muncul langsung di HP pengguna MESKIPUN APLIKASI DITUTUP / TIDAK DIJALANKAN DI FOREGROUND,
 * serta TANPA HARUS LOGIN baik pada Mode Warga maupun Mode Petugas (dengan porsi kegunaan berbeda).
 */
class SipagarBackgroundAlertWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val dispatchedCount = SipagarBackgroundAlertEngine.runBackgroundAlertCheckNow(applicationContext)
            Log.d(TAG, "SipagarBackgroundAlertWorker selesai. Notifikasi baru dikirim: $dispatchedCount")
            Result.success()
        } catch (e: Exception) {
            Log.w(TAG, "SipagarBackgroundAlertWorker kendala jaringan sementara: ${e.message}")
            Result.retry()
        }
    }

    companion object {
        const val TAG = "SipagarBgAlertWorker"
        const val WORK_NAME_PERIODIC = "sipagar_background_alert_periodic_15m"
        const val WORK_NAME_IMMEDIATE = "sipagar_background_alert_immediate_once"
    }
}

/**
 * Mesin eksekusi pengecekan latar belakang terpadu yang dapat dipanggil oleh WorkManager
 * maupun pengujian terisolasi.
 */
object SipagarBackgroundAlertEngine {
    private const val TAG = "SipagarBgAlertEngine"
    private const val FRESH_REPORT_WINDOW_MS = 30L * 60_000L // 30 menit

    suspend fun runBackgroundAlertCheckNow(context: Context): Int {
        val appCtx = context.applicationContext
        DisasterTriageNotificationManager.initNotificationChannels(appCtx)
        OfficerPushNotificationHelper.ensureNotificationChannels(appCtx)
        EarthquakeNotificationHelper.initNotificationChannel(appCtx)
        WeatherNotificationHelper.initNotificationChannel(appCtx)
        OfficerPushNotificationHelper.registerAppContext(appCtx)

        val settings = DisasterTriageNotificationManager.getSettings(appCtx)
        if (!settings.isEnabled) {
            return 0
        }

        var dispatchedNotifications = 0
        val database = AppDatabase.getInstance(appCtx)
        val repository = WeatherRepository(database.weatherDao())

        val selectedDistrictId = DisasterTriageNotificationManager.getSelectedDistrictIdForBackground(appCtx)
        val selectedDistrict = MukomukoDistricts.list.firstOrNull { it.id == selectedDistrictId }
            ?: MukomukoDistricts.defaultDistrict

        // =====================================================================
        // 1. PEMANTAUAN LAPORAN WARGA YANG MASUK (TANPA HARUS LOGIN)
        // =====================================================================
        try {
            repository.syncCitizenDisasterReportsOnline()
        } catch (e: Exception) {
            Log.d(TAG, "Sinkronisasi laporan online latar belakang dilewati: ${e.message}")
        }

        val allReportEntities = try {
            database.weatherDao().getAllCitizenDisasterReportsSync()
        } catch (_: Exception) {
            emptyList()
        }
        val allReports = allReportEntities.map { it.toDomainModel() }

        val nowMs = System.currentTimeMillis()
        if (!OfficerPushNotificationHelper.isBaselineInitialized(appCtx)) {
            // Daftarkan laporan lama sebagai baseline, kecuali laporan baru (< 30 menit terakhir)
            val olderIds = allReports
                .filter { (nowMs - it.timestamp) > FRESH_REPORT_WINDOW_MS }
                .map { it.id }
            OfficerPushNotificationHelper.registerInitialBaselineReports(appCtx, olderIds)
        }

        val pendingNewReports = allReports
            .filter { rep ->
                val isPending = CitizenReportStatusHelper.isPending(rep.status) ||
                    rep.status.equals("Pending", ignoreCase = true)
                isPending && !OfficerPushNotificationHelper.hasReportBeenNotified(appCtx, rep.id)
            }
            .sortedBy { it.timestamp }

        for (newReport in pendingNewReports) {
            val posted = OfficerPushNotificationHelper.dispatchRoleAwareNewReportNotification(
                context = appCtx,
                report = newReport
            )
            if (posted) {
                dispatchedNotifications++
            }
        }

        // Evaluasi sinyal eskalasi laporan warga (Triase Kuning / Oranye / Merah)
        if (allReports.isNotEmpty()) {
            val surgeSignal = DisasterTriageNotificationManager.evaluateDisasterReportSurge(
                reports = allReports,
                fallbackDistrict = selectedDistrict.name,
                onlySelectedDistrict = settings.onlySelectedDistrict
            )
            if (DisasterTriageNotificationManager.evaluateAndNotify(appCtx, surgeSignal) == TriageDispatchResult.POSTED) {
                dispatchedNotifications++
            }
        }

        // =====================================================================
        // 2. PEMANTAUAN KEJADIAN GEMPA BUMI & TSUNAMI WRS BMKG (PROV. BENGKULU)
        // =====================================================================
        try {
            val latestEq = repository.fetchLatestEarthquake()
            val feltEqs = repository.fetchFeltEarthquakes()
            val eqCandidates = (listOf(latestEq) + feltEqs)
                .distinctBy { it.id }
                .filter {
                    !it.isFallbackData &&
                        it.isBengkuluProvinceAffected &&
                        BengkuluEarthquakeScope.isWithinNotificationWindow(it, nowMs)
                }
            if (eqCandidates.isNotEmpty()) {
                val eqSignal = DisasterTriageNotificationManager.evaluateEarthquakesForBengkulu(
                    earthquakes = eqCandidates,
                    fallbackLocation = selectedDistrict.name,
                    nowMillis = nowMs
                )
                if (DisasterTriageNotificationManager.evaluateAndNotify(appCtx, eqSignal, nowMillis = nowMs) == TriageDispatchResult.POSTED) {
                    dispatchedNotifications++
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Pemeriksaan gempa BMKG latar belakang dilewati: ${e.message}")
        }

        // =====================================================================
        // 3. PEMANTAUAN POTENSI ANCAMAN (PERINGATAN DINI, CUACA EKSTREM, MARITIM, ROB, UDARA)
        // =====================================================================
        try {
            val warnings = repository.getDetailedEarlyWarnings(selectedDistrict)
            val warningSignal = DisasterTriageNotificationManager.evaluateEarlyWarnings(
                warnings = warnings,
                districtName = selectedDistrict.name,
                onlySelectedDistrict = settings.onlySelectedDistrict
            )
            if (DisasterTriageNotificationManager.evaluateAndNotify(appCtx, warningSignal) == TriageDispatchResult.POSTED) {
                dispatchedNotifications++
            }
        } catch (e: Exception) {
            Log.d(TAG, "Evaluasi peringatan dini latar belakang dilewati: ${e.message}")
        }

        try {
            val weatherReport = repository.fetchWeatherReport(selectedDistrict)
            val weatherSignal = DisasterTriageNotificationManager.evaluateWeatherTelemetry(weatherReport)
            val marineSignal = DisasterTriageNotificationManager.evaluateMarineWave(weatherReport.marine, selectedDistrict.name)
            val aqiSignal = DisasterTriageNotificationManager.evaluateAirQuality(weatherReport.airQuality, selectedDistrict.name)

            listOf(weatherSignal, marineSignal, aqiSignal).forEach { sig ->
                if (DisasterTriageNotificationManager.evaluateAndNotify(appCtx, sig) == TriageDispatchResult.POSTED) {
                    dispatchedNotifications++
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Fetch telemetri cuaca latar belakang dilewati: ${e.message}")
        }

        try {
            val tidal = TidalDataHelper.getTidalForecastForDistrict(selectedDistrict)
            val tidalSignal = DisasterTriageNotificationManager.evaluateTidalRob(tidal, selectedDistrict.name)
            if (DisasterTriageNotificationManager.evaluateAndNotify(appCtx, tidalSignal) == TriageDispatchResult.POSTED) {
                dispatchedNotifications++
            }
        } catch (e: Exception) {
            Log.d(TAG, "Evaluasi pasang rob latar belakang dilewati: ${e.message}")
        }

        // =====================================================================
        // 4. PEMANTAUAN TITIK PANAS KARHUTLA (NASA FIRMS & BMKG)
        // =====================================================================
        try {
            val karhutlaRepo = KarhutlaHotspotRepository(appCtx)
            val karhutlaSignal = karhutlaRepo.evaluateAndNotifyKarhutlaTriage(dayRange = 2)
            if (karhutlaSignal.level != com.example.util.DisasterTriageLevel.HIJAU_AMAN) {
                dispatchedNotifications++
            }
        } catch (e: Exception) {
            Log.d(TAG, "Evaluasi hotspot Karhutla latar belakang dilewati: ${e.message}")
        }

        return dispatchedNotifications
    }

    private fun CitizenDisasterReportEntity.toDomainModel(): CitizenDisasterReport {
        return CitizenDisasterReport(
            id = id,
            disasterType = disasterType,
            incidentTime = incidentTime,
            incidentLocation = incidentLocation,
            district = district,
            village = village,
            latitude = latitude,
            longitude = longitude,
            gpsAccuracyMeters = gpsAccuracyMeters,
            isGpsVerified = isGpsVerified,
            casualtiesDead = casualtiesDead,
            casualtiesInjured = casualtiesInjured,
            casualtiesMissing = casualtiesMissing,
            photoUri = photoUri,
            reporterName = reporterName,
            reporterPhone = reporterPhone,
            additionalNotes = additionalNotes,
            timestamp = timestamp,
            status = status,
            updatedAt = updatedAt,
            handledByOfficerName = handledByOfficerName,
            handledByOfficerNip = handledByOfficerNip,
            handledByOfficerRole = handledByOfficerRole,
            isSyncedToCloud = isSyncedToCloud
        )
    }
}

/**
 * Penjadwal resmi pemantauan latar belakang 24 jam SIPAGAR KITA.
 * Menjamin perangkat tetap menerima notifikasi laporan warga, potensi ancaman, dan kejadian
 * bencana meskipun aplikasi ditutup atau perangkat baru saja dinyalakan ulang (reboot).
 */
object SipagarBackgroundAlertScheduler {
    private const val TAG = "SipagarBgScheduler"

    fun scheduleBackgroundMonitoring(
        context: Context,
        triggerImmediateCheck: Boolean = false,
        replaceExisting: Boolean = false
    ) {
        val appCtx = context.applicationContext
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val periodicRequest = PeriodicWorkRequestBuilder<SipagarBackgroundAlertWorker>(
                15L, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5L, TimeUnit.MINUTES)
                .addTag(SipagarBackgroundAlertWorker.WORK_NAME_PERIODIC)
                .build()

            val workManager = WorkManager.getInstance(appCtx)
            workManager.enqueueUniquePeriodicWork(
                SipagarBackgroundAlertWorker.WORK_NAME_PERIODIC,
                if (replaceExisting) ExistingPeriodicWorkPolicy.UPDATE else ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )

            if (triggerImmediateCheck) {
                val immediateRequest = OneTimeWorkRequestBuilder<SipagarBackgroundAlertWorker>()
                    .setConstraints(constraints)
                    .addTag(SipagarBackgroundAlertWorker.WORK_NAME_IMMEDIATE)
                    .build()
                workManager.enqueueUniqueWork(
                    SipagarBackgroundAlertWorker.WORK_NAME_IMMEDIATE,
                    ExistingWorkPolicy.REPLACE,
                    immediateRequest
                )
            }
            Log.i(TAG, "Pemantauan latar belakang 24 jam SIPAGAR KITA aktif (interval 15 menit, tanpa perlu login).")
        } catch (e: Exception) {
            Log.w(TAG, "WorkManager belum diinisialisasi di environment ini: ${e.message}")
        }
    }
}
