package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "citizen_disaster_reports")
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
    val status: String
)
