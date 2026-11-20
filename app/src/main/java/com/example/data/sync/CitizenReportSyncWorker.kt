package com.example.data.sync

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.local.AppDatabase
import com.example.data.repository.WeatherRepository
import java.util.concurrent.TimeUnit

/**
 * Worker Latar Belakang (WorkManager) untuk Standar Aplikasi Darurat Profesional:
 * - Ketika pengiriman laporan warga gagal karena tidak ada sinyal internet, laporan (teks + foto GPS)
 *   disimpan utuh di Room SQLite lokal (`isSyncedToCloud = false`).
 * - [CitizenReportSyncWorker] dijadwalkan dengan constraint [NetworkType.CONNECTED] dan akan
 *   dijalankan otomatis oleh sistem Android segera saat perangkat kembali terhubung ke internet,
 *   bahkan jika aplikasi sedang ditutup.
 * - Mengunggah foto bukti ber-watermark GPS ke Firebase Storage hingga 100% selesai, mengambil
 *   download URL (`fotoUrl` / `fotoLampiran`), lalu menyimpan dokumen laporan ke koleksi
 *   `citizen_disaster_reports` di Cloud Firestore secara atomik.
 */
class CitizenReportSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val db = AppDatabase.getDatabase(applicationContext)
            val dao = db.weatherDao()
            val pendingBefore = dao.getUnsyncedCitizenReports()
            if (pendingBefore.isEmpty()) {
                Log.d(TAG, "Tidak ada laporan warga offline yang tertunda. Background sync selesai.")
                return Result.success()
            }

            Log.i(TAG, "Memulai WorkManager background sync untuk ${pendingBefore.size} laporan warga tertunda...")
            val repository = WeatherRepository(dao)
            val syncedCount = repository.syncCitizenDisasterReportsOnline()

            val pendingAfter = dao.getUnsyncedCitizenReports()
            if (pendingAfter.isEmpty()) {
                Log.i(TAG, "Seluruh laporan warga offline ($syncedCount item) berhasil disinkronkan ke Cloud Firestore & Firebase Storage.")
                Result.success()
            } else {
                Log.w(TAG, "Masih ada ${pendingAfter.size} laporan warga yang menunggu koneksi stabil (attempt=$runAttemptCount).")
                if (runAttemptCount < MAX_RETRY_ATTEMPTS) {
                    Result.retry()
                } else {
                    Result.failure()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "WorkManager gagal menyinkronkan laporan warga: ${e.message}", e)
            if (runAttemptCount < MAX_RETRY_ATTEMPTS) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        private const val TAG = "CitizenReportSyncWorker"
        const val UNIQUE_WORK_NAME = "citizen_disaster_report_background_sync"
        const val WORK_TAG = "bpbd_citizen_report_offline_sync"
        private const val MAX_RETRY_ATTEMPTS = 8

        /**
         * Menjadwalkan pengiriman ulang otomatis di latar belakang melalui Jetpack WorkManager
         * dengan syarat jaringan [NetworkType.CONNECTED] dan Exponential Backoff.
         */
        fun enqueueWhenConnected(context: Context?): Boolean {
            val appCtx = context?.applicationContext ?: return false
            return try {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val syncRequest = OneTimeWorkRequestBuilder<CitizenReportSyncWorker>()
                    .setConstraints(constraints)
                    .addTag(WORK_TAG)
                    .setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        15L,
                        TimeUnit.SECONDS
                    )
                    .build()

                WorkManager.getInstance(appCtx).enqueueUniqueWork(
                    UNIQUE_WORK_NAME,
                    ExistingWorkPolicy.APPEND_OR_REPLACE,
                    syncRequest
                )
                Log.i(TAG, "WorkManager [$UNIQUE_WORK_NAME] dijadwalkan (menunggu NetworkType.CONNECTED).")
                true
            } catch (e: Exception) {
                Log.w(TAG, "WorkManager belum diinisialisasi di environment ini: ${e.message}")
                false
            }
        }
    }
}
