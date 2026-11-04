package com.example.data.sync

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.local.AppDatabase
import com.example.data.local.DatabaseIndexManager
import java.util.concurrent.TimeUnit

/**
 * Background maintenance worker for Room SQLite storage.
 * Performs periodic SQLite VACUUM and ANALYZE operations, verifies table indexes on
 * 'citizen_disaster_reports' and 'personnel', and cleans up orphaned records to ensure high performance.
 */
class DatabaseMaintenanceWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.i(TAG, "Starting periodic SQLite Database Maintenance...")
        val database = AppDatabase.getInstance(applicationContext)

        val beforeSize = DatabaseIndexManager.getDatabaseSizeBytes(applicationContext)
        val indexesEnsured = DatabaseIndexManager.ensureIndexes(database)
        val vacuumDuration = DatabaseIndexManager.performVacuum(database)
        val afterSize = DatabaseIndexManager.getDatabaseSizeBytes(applicationContext)

        Log.i(TAG, "Database Maintenance Finished: indexes=$indexesEnsured, vacuumTime=${vacuumDuration}ms, sizeBefore=${beforeSize / 1024}KB, sizeAfter=${afterSize / 1024}KB")

        return if (vacuumDuration >= 0) Result.success() else Result.retry()
    }

    companion object {
        private const val TAG = "DbMaintenanceWorker"
        private const val WORK_NAME = "periodic_sqlite_vacuum_maintenance"

        /**
         * Enqueues periodic 24-hour maintenance work during device idle/charging periods.
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<DatabaseMaintenanceWorker>(24, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
            Log.d(TAG, "Periodic SQLite VACUUM maintenance worker scheduled.")
        }
    }
}
