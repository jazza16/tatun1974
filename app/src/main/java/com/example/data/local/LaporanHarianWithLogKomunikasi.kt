package com.example.data.local

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Data model relasi One-to-Many antara LaporanHarian dan LogKomunikasi.
 */
data class LaporanHarianWithLogKomunikasi(
    @Embedded val laporan: LaporanHarian,
    @Relation(
        parentColumn = "id",
        entityColumn = "laporanId"
    )
    val logs: List<LogKomunikasi> = emptyList()
)
