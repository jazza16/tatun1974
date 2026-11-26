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
import com.example.data.api.FirmsSensorSource
import com.example.data.repository.KarhutlaHotspotRepository
import java.util.concurrent.TimeUnit

/**
 * WorkManager Periodic Worker for NASA FIRMS (VIIRS/MODIS) Hotspot Synchronization.
 * Runs periodically every 1–3 hours (default: 2 hours).
 */
class FirmsHotspotSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val repo = KarhutlaHotspotRepository(applicationContext)
            val matched = repo.syncFirmsAndMatch(
                sensor = FirmsSensorSource.VIIRS_SNPP,
                dayRange = 2,
                enableSimulation = false
            )
            repo.evaluateAndNotifyKarhutlaTriage(dayRange = 2)
            Log.d(TAG, "FIRMS periodic sync completed. Cross-matched pairs: $matched")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "FIRMS periodic sync error: ${e.message}")
            Result.retry()
        }
    }

    companion object {
        const val TAG = "FirmsHotspotSyncWorker"
        const val WORK_NAME_PERIODIC = "karhutla_firms_periodic_sync"
    }
}

/**
 * WorkManager Periodic Worker for BMKG Hotspot Synchronization.
 * Runs periodically every 3–6 hours (default: 4 hours) while respecting BMKG server etiquette.
 */
class BmkgHotspotSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val repo = KarhutlaHotspotRepository(applicationContext)
            val matched = repo.syncBmkgAndMatch(
                forceRefresh = false,
                dayRange = 2,
                enableSimulation = false
            )
            repo.evaluateAndNotifyKarhutlaTriage(dayRange = 2)
            Log.d(TAG, "BMKG periodic sync completed. Cross-matched pairs: $matched")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "BMKG periodic sync error: ${e.message}")
            Result.retry()
        }
    }

    companion object {
        const val TAG = "BmkgHotspotSyncWorker"
        const val WORK_NAME_PERIODIC = "karhutla_bmkg_periodic_sync"
    }
}

/**
 * Final chained Worker that runs atomic `@Transaction` matching & 30–90 day retention cleanup
 * immediately after both `FirmsHotspotSyncWorker` and `BmkgHotspotSyncWorker` complete.
 */
class KarhutlaCrossMatchWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val repo = KarhutlaHotspotRepository(applicationContext)
            val matched = repo.runCrossSourceMatchingAndRetentionOnly(dayRange = 2)
            Log.d(TAG, "Chained @Transaction matching & retention completed. Matched pairs: $matched")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Chained matching error: ${e.message}")
            Result.retry()
        }
    }

    companion object {
        const val TAG = "KarhutlaCrossMatchWorker"
        const val WORK_NAME_CHAINED = "karhutla_chained_sync_and_match"
    }
}

/**
 * Scheduler utility for configuring periodic WorkManager jobs:
 * - FIRMS every 1–3 hours
 * - BMKG every 3–6 hours
 * and chained one-time sync (FIRMS + BMKG -> @Transaction Matching).
 */
object KarhutlaSyncScheduler {

    fun schedulePeriodicSync(
        context: Context,
        firmsIntervalHours: Int? = null,
        bmkgIntervalHours: Int? = null,
        replaceExisting: Boolean = false
    ) {
        val appContext = context.applicationContext
        val repo = KarhutlaHotspotRepository(appContext)

        val resolvedFirmsHours = (firmsIntervalHours ?: repo.getFirmsIntervalHours()).coerceIn(1, 3)
        val resolvedBmkgHours = (bmkgIntervalHours ?: repo.getBmkgIntervalHours()).coerceIn(3, 6)

        repo.setFirmsIntervalHours(resolvedFirmsHours)
        repo.setBmkgIntervalHours(resolvedBmkgHours)

        val networkConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // 1. Periodic FIRMS Sync (1–3 hours)
        val firmsPeriodicRequest = PeriodicWorkRequestBuilder<FirmsHotspotSyncWorker>(
            resolvedFirmsHours.toLong(),
            TimeUnit.HOURS
        )
            .setConstraints(networkConstraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
            .addTag(FirmsHotspotSyncWorker.WORK_NAME_PERIODIC)
            .build()

        // 2. Periodic BMKG Sync (3–6 hours)
        val bmkgPeriodicRequest = PeriodicWorkRequestBuilder<BmkgHotspotSyncWorker>(
            resolvedBmkgHours.toLong(),
            TimeUnit.HOURS
        )
            .setConstraints(networkConstraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
            .addTag(BmkgHotspotSyncWorker.WORK_NAME_PERIODIC)
            .build()

        val policy = if (replaceExisting) {
            ExistingPeriodicWorkPolicy.UPDATE
        } else {
            ExistingPeriodicWorkPolicy.KEEP
        }

        val workManager = WorkManager.getInstance(appContext)
        workManager.enqueueUniquePeriodicWork(
            FirmsHotspotSyncWorker.WORK_NAME_PERIODIC,
            policy,
            firmsPeriodicRequest
        )
        workManager.enqueueUniquePeriodicWork(
            BmkgHotspotSyncWorker.WORK_NAME_PERIODIC,
            policy,
            bmkgPeriodicRequest
        )
    }

    /**
     * Triggers an immediate chained WorkManager flow:
     * [FirmsHotspotSyncWorker, BmkgHotspotSyncWorker] -> KarhutlaCrossMatchWorker (@Transaction)
     */
    fun triggerChainedSyncNow(context: Context) {
        val appContext = context.applicationContext
        val networkConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val firmsOneTime = OneTimeWorkRequestBuilder<FirmsHotspotSyncWorker>()
            .setConstraints(networkConstraints)
            .build()

        val bmkgOneTime = OneTimeWorkRequestBuilder<BmkgHotspotSyncWorker>()
            .setConstraints(networkConstraints)
            .build()

        val crossMatchOneTime = OneTimeWorkRequestBuilder<KarhutlaCrossMatchWorker>()
            .build()

        WorkManager.getInstance(appContext)
            .beginUniqueWork(
                KarhutlaCrossMatchWorker.WORK_NAME_CHAINED,
                ExistingWorkPolicy.REPLACE,
                listOf(firmsOneTime, bmkgOneTime)
            )
            .then(crossMatchOneTime)
            .enqueue()
    }
}
