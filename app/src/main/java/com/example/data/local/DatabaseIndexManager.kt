package com.example.data.local

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Automated Database Index Manager for Room SQLite.
 * Manages runtime index verification and execution of maintenance commands (VACUUM, ANALYZE)
 * on frequently queried tables: 'citizen_disaster_reports' and 'personnel'.
 */
object DatabaseIndexManager {
    private const val TAG = "DatabaseIndexManager"

    // Queries to ensure indexes are present even if schema evolves
    private val RECOMMENDED_INDEXES = listOf(
        "CREATE INDEX IF NOT EXISTS idx_cdr_status ON citizen_disaster_reports(status)",
        "CREATE INDEX IF NOT EXISTS idx_cdr_district ON citizen_disaster_reports(district)",
        "CREATE INDEX IF NOT EXISTS idx_cdr_timestamp ON citizen_disaster_reports(timestamp DESC)",
        "CREATE INDEX IF NOT EXISTS idx_cdr_disaster_type ON citizen_disaster_reports(disasterType)",
        "CREATE INDEX IF NOT EXISTS idx_cdr_status_time ON citizen_disaster_reports(status, timestamp DESC)",
        "CREATE INDEX IF NOT EXISTS idx_cdr_district_status ON citizen_disaster_reports(district, status)",
        "CREATE INDEX IF NOT EXISTS idx_personnel_nip ON personnel(nip)",
        "CREATE INDEX IF NOT EXISTS idx_personnel_role ON personnel(role)",
        "CREATE INDEX IF NOT EXISTS idx_personnel_shift ON personnel(shift)",
        "CREATE INDEX IF NOT EXISTS idx_personnel_name ON personnel(name)",
        "CREATE INDEX IF NOT EXISTS idx_personnel_role_shift ON personnel(role, shift)"
    )

    /**
     * Verifies and applies indexes across tables to guarantee fast querying.
     */
    suspend fun ensureIndexes(database: AppDatabase): Boolean = withContext(Dispatchers.IO) {
        try {
            val db = database.openHelper.writableDatabase
            for (sql in RECOMMENDED_INDEXES) {
                db.execSQL(sql)
            }
            Log.d(TAG, "All Room indexes verified and ensured successfully.")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error ensuring database indexes: ${e.message}", e)
            false
        }
    }

    /**
     * Executes SQLite VACUUM operation to reclaim unused storage, defragment the database,
     * and optimize query planning with ANALYZE.
     */
    suspend fun performVacuum(database: AppDatabase): Long = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            val db = database.openHelper.writableDatabase
            db.execSQL("VACUUM")
            db.execSQL("ANALYZE")
            val duration = System.currentTimeMillis() - startTime
            Log.i(TAG, "SQLite VACUUM & ANALYZE completed in ${duration}ms")
            duration
        } catch (e: Exception) {
            Log.e(TAG, "Failed to perform SQLite VACUUM: ${e.message}", e)
            -1L
        }
    }

    /**
     * Gets the database file size in kilobytes for storage health reporting.
     */
    fun getDatabaseSizeBytes(context: Context, dbName: String = "cuaca_mukomuko.db"): Long {
        return try {
            val dbFile = context.getDatabasePath(dbName)
            if (dbFile.exists()) dbFile.length() else 0L
        } catch (_: Exception) {
            0L
        }
    }
}
