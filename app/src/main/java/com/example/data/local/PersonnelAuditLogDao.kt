package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Pusdalops Personnel Audit Logs.
 */
@Dao
interface PersonnelAuditLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: PersonnelAuditLogEntity): Long

    @Query("SELECT * FROM personnel_audit_logs ORDER BY timestamp DESC")
    fun getAllAuditLogs(): Flow<List<PersonnelAuditLogEntity>>

    @Query("SELECT * FROM personnel_audit_logs ORDER BY timestamp DESC")
    suspend fun getAllAuditLogsList(): List<PersonnelAuditLogEntity>

    @Query("SELECT * FROM personnel_audit_logs WHERE nip = :nip ORDER BY timestamp DESC")
    fun getAuditLogsByNip(nip: String): Flow<List<PersonnelAuditLogEntity>>

    @Query("SELECT * FROM personnel_audit_logs WHERE action = :action ORDER BY timestamp DESC")
    fun getAuditLogsByAction(action: String): Flow<List<PersonnelAuditLogEntity>>

    @Query("SELECT * FROM personnel_audit_logs ORDER BY timestamp DESC LIMIT 50")
    suspend fun getRecentAuditLogs(): List<PersonnelAuditLogEntity>

    @Query("SELECT * FROM personnel_audit_logs WHERE nip = :nip AND action = 'LOGIN' ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastLogin(nip: String): PersonnelAuditLogEntity?

    @Query("DELETE FROM personnel_audit_logs WHERE LOWER(TRIM(nip)) = LOWER(TRIM(:nip)) OR LOWER(name) LIKE '%' || LOWER(TRIM(:nameKeyword)) || '%'")
    suspend fun deleteAuditLogsByNipOrName(nip: String, nameKeyword: String)

    @Query("DELETE FROM personnel_audit_logs WHERE LOWER(TRIM(nip)) = LOWER(TRIM(:nip))")
    suspend fun deleteAuditLogsByNip(nip: String)

    @Query("DELETE FROM personnel_audit_logs")
    suspend fun clearAuditLogs()
}
