package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entitas Room Database untuk menyimpan titik rawan bencana dan rute/titik evakuasi
 * di Kabupaten Mukomuko secara lokal (100% offline persistence).
 */
@Entity(tableName = "hazard_risk_zones")
data class HazardRiskZoneEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val type: String, // Gelombang & Abrasi, Banjir DAS, Tanah Longsor, Tsunami, Titik Kumpul (TES)
    val riskLevel: String, // SANGAT TINGGI, TINGGI, SEDANG, ZONA AMAN
    val centerLat: Double,
    val centerLon: Double,
    val radiusKm: Float,
    val colorHex: String,
    val description: String,
    val evacuationSop: String
)
