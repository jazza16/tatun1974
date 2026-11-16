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
    val status: String = "Terverifikasi GPS" // "Terverifikasi GPS", "Diterima PUSDALOPS", "Tim TRC Meluncur"
)
