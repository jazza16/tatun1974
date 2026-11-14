package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Entitas Distribusi Logistik / Aset Bantuan yang terikat dengan ID Laporan Harian.
 */
@Serializable
@Entity(
    tableName = "distribusi_logistik",
    indices = [Index(value = ["laporanId"])]
)
data class DistribusiLogistikEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val laporanId: Int = 0,
    val namaBarang: String = "",
    val jumlah: String = "",
    val satuan: String = "",
    val keterangan: String = ""
)

typealias DistribusiLogistik = DistribusiLogistikEntity
