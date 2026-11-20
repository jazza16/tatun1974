package com.example.model

import androidx.annotation.Keep

/**
 * Model data laporan kejadian bencana dari warga Mukomuko.
 * Dilengkapi koordinat GPS ponsel dan foto kejadian untuk verifikasi anti-laporan palsu.
 */
@Keep
data class CitizenDisasterReport(
    val id: String, // e.g. "LAP-MMK-20261025-001"
    val disasterType: String, // Jenis Bencana
    val incidentTime: String, // Waktu Kejadian
    val incidentLocation: String, // Lokasi Kejadian (detail alamat / patokan)
    val district: String, // Kecamatan (15 Kecamatan di Kab. Mukomuko)
    val village: String, // Desa / Kelurahan
    val latitude: Double, // Titik Lokasi Lat dari GPS ponsel
    val longitude: Double, // Titik Lokasi Lon dari GPS ponsel
    val gpsAccuracyMeters: Float? = null, // Akurasi GPS ponsel dalam meter
    val isGpsVerified: Boolean = false, // Status verifikasi geolokasi ponsel pelapor
    val casualtiesDead: Int = 0, // Korban Meninggal Dunia
    val casualtiesInjured: Int = 0, // Korban Luka
    val casualtiesMissing: Int = 0, // Korban Hilang
    val photoUri: String? = null, // URI atau Path Foto Bukti Kejadian
    val reporterName: String = "Warga Mukomuko",
    val reporterPhone: String = "",
    val additionalNotes: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "Diproses", // Kanonik: "Diproses", "Divalidasi", "Selesai"
    val updatedAt: Long = timestamp,
    val handledByOfficerName: String = "",
    val handledByOfficerNip: String = "",
    val handledByOfficerRole: String = "",
    val isSyncedToCloud: Boolean = true
) {
    fun effectiveUpdatedAt(): Long = maxOf(updatedAt, timestamp)
}

/**
 * Single source of truth for canonical citizen disaster report status normalization,
 * progression ranking, and UI filtering across Citizen View, Officer Console,
 * Form Laporan Harian ("DARI WARGA"), and SyncConflictHandler.
 */
object CitizenReportStatusHelper {
    const val STATUS_DIPROSES = "Diproses"
    const val STATUS_DIVALIDASI = "Divalidasi"
    const val STATUS_SELESAI = "Selesai"

    fun normalizeStatus(rawStatus: String?): String {
        val trimmed = rawStatus?.trim().orEmpty()
        return when {
            trimmed.equals(STATUS_SELESAI, ignoreCase = true) ||
                trimmed.contains("Selesai", ignoreCase = true) -> STATUS_SELESAI
            trimmed.equals(STATUS_DIVALIDASI, ignoreCase = true) ||
                trimmed.contains("Valid", ignoreCase = true) ||
                trimmed.contains("TRC", ignoreCase = true) ||
                trimmed.contains("Dalam Penanganan", ignoreCase = true) -> STATUS_DIVALIDASI
            else -> STATUS_DIPROSES
        }
    }

    fun statusRank(rawStatus: String?): Int = when (normalizeStatus(rawStatus)) {
        STATUS_SELESAI -> 3
        STATUS_DIVALIDASI -> 2
        else -> 1
    }

    fun isPending(rawStatus: String?): Boolean = normalizeStatus(rawStatus) == STATUS_DIPROSES
    fun isValidated(rawStatus: String?): Boolean = normalizeStatus(rawStatus) == STATUS_DIVALIDASI
    fun isResolved(rawStatus: String?): Boolean = normalizeStatus(rawStatus) == STATUS_SELESAI

    fun matchesFilter(rawStatus: String?, selectedFilter: String): Boolean {
        return when (selectedFilter.trim()) {
            "", "Semua" -> true
            STATUS_DIPROSES -> isPending(rawStatus)
            STATUS_DIVALIDASI -> isValidated(rawStatus)
            STATUS_SELESAI -> isResolved(rawStatus)
            else -> normalizeStatus(rawStatus).equals(normalizeStatus(selectedFilter), ignoreCase = true)
        }
    }
}

