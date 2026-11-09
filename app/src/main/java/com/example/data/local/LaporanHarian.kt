package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room Database Entity untuk Laporan Harian Pusdalops PB BPBD Kabupaten Mukomuko.
 * Menyimpan rekapitulasi data bencana, dampak kerusakan, korban, upaya penanganan,
 * dan identitas stasiun/operator pelapor.
 */
@Entity(
    tableName = "laporan_harian",
    indices = [
        Index(value = ["tanggalLaporan"]),
        Index(value = ["jenisBencana"]),
        Index(value = ["stationId"]),
        Index(value = ["tanggalLaporan", "jenisBencana"])
    ]
)
data class LaporanHarian(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val tanggalLaporan: String,
    val jenisBencana: String,
    val tanggalKejadian: String,
    val waktuKejadian: String,
    val kedalaman: String,
    val lokasi: String,
    val koordinat: String,
    val cakupanDampak: String,
    val kondisiCuaca: String,
    val upayaDilakukan: String,
    val kondisiMutakhir: String,
    val korbanLuka: Int,
    val korbanMeninggal: Int,
    val kerusakan: String,
    val penyebab: String,
    val sumberInformasi: String,
    val tanggalSurat: String,
    val tanggalTerima: String,
    val operatorName: String,
    val stationId: String
    // Log komunikasi dihubungkan melalui tabel relasi One-to-Many 'log_komunikasi' (ForeignKey cascade)
)
