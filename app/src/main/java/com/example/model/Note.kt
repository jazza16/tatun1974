package com.example.model

import androidx.annotation.Keep
import java.util.UUID

/**
 * Data Model Note untuk Catatan Operasional Pusdalops BPBD Kabupaten Mukomuko.
 * Dirancang responsif dan terintegrasi untuk Phone, Tablet, & Foldable.
 */
@Keep
data class Note(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val content: String,
    val category: String = "Operasional", // Operasional, Insiden Bencana, Logistik & Sarpras, Instruksi Pimpinan, Cuaca & BMKG
    val priority: String = "Sedang", // Tinggi, Sedang, Rendah
    val authorName: String = "Petugas Pusdalops",
    val authorRole: String = "OPERATOR",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val tags: String = "",
    val district: String = "Mukomuko"
)
