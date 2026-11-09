package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room Database Entity untuk tabel 'laporan_warga'.
 * Menyimpan data laporan bencana dari warga berdasarkan tanggal pelaporan.
 */
@Entity(
    tableName = "laporan_warga",
    indices = [
        Index(value = ["tanggal"]),
        Index(value = ["jenisBencana"]),
        Index(value = ["kecamatan"]),
        Index(value = ["tanggal", "status"])
    ]
)
data class LaporanWargaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val tanggal: String,
    val waktu: String = "",
    val namaPelapor: String = "Warga Mukomuko",
    val nomorKontak: String = "",
    val jenisBencana: String = "Banjir",
    val lokasi: String = "",
    val koordinat: String = "-2.5830, 101.1240",
    val kecamatan: String = "Kota Mukomuko",
    val desa: String = "",
    val deskripsi: String = "",
    val status: String = "Menunggu Verifikasi",
    val fotoUri: String? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
)
