package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room Entity to record every synchronization conflict resolution event
 * between local SQLite database and Firestore across all multi-device testing units.
 */
@Entity(
    tableName = "sync_conflict_logs",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["reportId"]),
        Index(value = ["conflictType"]),
        Index(value = ["resolution"])
    ]
)
data class SyncConflictLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val reportId: String,
    val conflictType: String,      // e.g., "TIMESTAMP_VERSION_DRIFT", "CONCURRENT_STATUS_UPDATE", "OFFLINE_MERGE"
    val resolution: String,        // e.g., "LATEST_TIMESTAMP_PRESERVED", "OFFICER_TRIAGE_MERGED", "REMOTE_ACCEPTED"
    val localTimestamp: Long,
    val remoteTimestamp: Long,
    val localStatus: String,
    val remoteStatus: String,
    val details: String,
    val officerBadge: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
