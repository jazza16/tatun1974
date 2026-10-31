package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncConflictLogDao {
    @Query("SELECT * FROM sync_conflict_logs ORDER BY timestamp DESC")
    fun getAllConflictLogs(): Flow<List<SyncConflictLogEntity>>

    @Query("SELECT * FROM sync_conflict_logs ORDER BY timestamp DESC")
    suspend fun getAllConflictLogsList(): List<SyncConflictLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConflictLog(log: SyncConflictLogEntity): Long

    @Query("SELECT COUNT(*) FROM sync_conflict_logs")
    suspend fun getConflictLogsCount(): Int

    @Query("DELETE FROM sync_conflict_logs")
    suspend fun clearConflictLogs()
}
