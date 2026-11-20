package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room Database Entity for Citizen Disaster Reports.
 * Optimized with high-performance indices for timestamp, status, district, and composite queries.
 */
@Entity(
    tableName = "citizen_disaster_reports",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["status"]),
        Index(value = ["district"]),
        Index(value = ["disasterType"]),
        Index(value = ["status", "timestamp"]),
        Index(value = ["district", "timestamp"]),
        Index(value = ["district", "status"])
    ]
)
data class CitizenDisasterReportEntity(
    @PrimaryKey
    val id: String,
    val disasterType: String,
    val incidentTime: String,
    val incidentLocation: String,
    val district: String,
    val village: String,
    val latitude: Double,
    val longitude: Double,
    val gpsAccuracyMeters: Float?,
    val isGpsVerified: Boolean,
    val casualtiesDead: Int,
    val casualtiesInjured: Int,
    val casualtiesMissing: Int,
    val photoUri: String?,
    val reporterName: String,
    val reporterPhone: String,
    val additionalNotes: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String,
    val updatedAt: Long = timestamp,
    val handledByOfficerName: String = "",
    val handledByOfficerNip: String = "",
    val handledByOfficerRole: String = "",
    val isSyncedToCloud: Boolean = true
) {
    fun effectiveUpdatedAt(): Long = maxOf(updatedAt, timestamp)
}

