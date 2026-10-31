package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room Database Entity for Pusdalops Personnel Audit Logs.
 * Records login and logoff activities for staff accountability, shift monitoring,
 * and automated shift reminders.
 * Indexed for fast querying by NIP, timestamp, and action.
 */
@Entity(
    tableName = "personnel_audit_logs",
    indices = [
        Index(value = ["nip"]),
        Index(value = ["timestamp"]),
        Index(value = ["action"]),
        Index(value = ["nip", "timestamp"])
    ]
)
data class PersonnelAuditLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nip: String,
    val name: String,
    val role: String,
    val shift: String,
    val action: String, // "LOGIN" or "LOGOFF"
    val timestamp: Long = System.currentTimeMillis(),
    val deviceInfo: String = "Pusdalops Terminal Mobile"
)
