package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity untuk Log Komunikasi Kedaruratan (Relasi One-to-Many ke LaporanHarian).
 * Mencatat koordinasi antar stasiun dan menyinkronkan identitas petugas dari SessionManager / DataStore.
 */
@Entity(
    tableName = "log_komunikasi",
    foreignKeys = [
        ForeignKey(
            entity = LaporanHarianEntity::class,
            parentColumns = ["id"],
            childColumns = ["laporanId"],
            onDelete = CASCADE
        )
    ],
    indices = [
        Index(value = ["laporanId"])
    ]
)
data class LogKomunikasiEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val laporanId: Int,
    val waktu: String,
    val stationIdFrom: String,
    val stationIdTo: String,
    val subjek: String,

    // Tambahan Sinkronisasi
    val operatorName: String = "HITATUN ARAZAK" // Akan diisi otomatis "HITATUN ARAZAK"
) {
    // Secondary constructor untuk pemanggilan dengan parameter `operator`
    @Ignore
    constructor(
        laporanId: Int,
        waktu: String,
        stationIdFrom: String,
        stationIdTo: String,
        subjek: String,
        operator: String
    ) : this(
        id = 0,
        laporanId = laporanId,
        waktu = waktu,
        stationIdFrom = stationIdFrom,
        stationIdTo = stationIdTo,
        subjek = subjek,
        operatorName = operator.ifBlank { "HITATUN ARAZAK" }
    )

    // Secondary constructor untuk pemanggilan dengan operatorNama & operatorNip
    @Ignore
    constructor(
        id: Int = 0,
        laporanId: Int,
        waktu: String,
        stationIdFrom: String,
        stationIdTo: String,
        subjek: String,
        operatorNama: String,
        operatorNip: String = "197401282006041006"
    ) : this(
        id = id,
        laporanId = laporanId,
        waktu = waktu,
        stationIdFrom = stationIdFrom,
        stationIdTo = stationIdTo,
        subjek = subjek,
        operatorName = operatorNama.ifBlank { "HITATUN ARAZAK" }
    )

    // Secondary constructor untuk kompatibilitas pemanggilan sebelumnya
    @Ignore
    constructor(
        laporanId: Int,
        waktuKomunikasi: String,
        saluran: String,
        pihakDihubungi: String,
        pesan: String,
        arahanTindakan: String = "",
        operatorLog: String = "HITATUN ARAZAK",
        timestamp: Long = System.currentTimeMillis()
    ) : this(
        id = 0,
        laporanId = laporanId,
        waktu = waktuKomunikasi,
        stationIdFrom = saluran,
        stationIdTo = pihakDihubungi,
        subjek = pesan,
        operatorName = operatorLog.ifBlank { "HITATUN ARAZAK" }
    )

    // Getter kompatibilitas agar kode UI tetap berjalan mulus
    val logId: Int get() = id
    val waktuKomunikasi: String get() = waktu
    val saluran: String get() = stationIdFrom
    val pihakDihubungi: String get() = stationIdTo
    val pesan: String get() = subjek
    val arahanTindakan: String get() = ""
    val operator: String get() = operatorName
    val operatorLog: String get() = operatorName
    val operatorNama: String get() = operatorName
    val operatorNip: String get() = "197401282006041006"
    val timestamp: Long get() = System.currentTimeMillis()
}

typealias LogKomunikasi = LogKomunikasiEntity
