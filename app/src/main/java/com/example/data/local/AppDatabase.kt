package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        LaporanHarianEntity::class,
        LogKomunikasiEntity::class,
        LaporanWargaEntity::class,
        WeatherCacheEntity::class,
        FavoriteDistrictEntity::class,
        CommunityReportEntity::class,
        HistoricalWeatherEntity::class,
        CitizenDisasterReportEntity::class,
        HazardRiskZoneEntity::class,
        Personnel::class,
        PersonnelAuditLogEntity::class,
        SyncConflictLogEntity::class,
        OfficerNoteEntity::class,
        OfficerCredentialEntity::class,
        DistribusiLogistikEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun laporanHarianDao(): LaporanHarianDao
    abstract fun laporanWargaDao(): LaporanWargaDao
    abstract fun weatherDao(): WeatherDao
    abstract fun personnelDao(): PersonnelDao
    abstract fun auditLogDao(): PersonnelAuditLogDao
    abstract fun syncConflictLogDao(): SyncConflictLogDao
    abstract fun noteDao(): NoteDao
    abstract fun credentialDao(): OfficerCredentialDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sipagar_database"
                ).fallbackToDestructiveMigration(true)
                .fallbackToDestructiveMigrationOnDowngrade(true)
                .allowMainThreadQueries()
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Pre-populate official ASN BPBD personnel (leadership & operators)
                        for (op in OfficialAsnBpbdPersonnel.ALL_OFFICIAL_PERSONNEL) {
                            val sql = "INSERT OR IGNORE INTO personnel (name, nip, role, unit, shift, phone) VALUES (?, ?, ?, ?, ?, ?)"
                            db.execSQL(sql, arrayOf(op.name, op.nip, op.role, op.unit, op.shift, op.phone))
                        }

                        // Pre-populate sample official Laporan Harian & Log Komunikasi
                        val sqlLaporan = """
                            INSERT OR IGNORE INTO laporan_harian (
                                id, tanggalLaporan, jenisBencana, tanggalKejadian, waktuKejadian, kedalaman, lokasi, koordinat,
                                cakupanDampak, kondisiCuaca, cuacaSaatKejadian, upayaDilakukan, kondisiMutakhir, korbanLuka, korbanMeninggal,
                                kerusakan, penyebab, sumberInformasi, tanggalSurat, tanggalTerima, operatorName, stationId
                            ) VALUES (
                                1, '09 November 2026', 'Gempa Bumi Tektonik', '09 November 2026', '06:15:20 WIB', '15 km',
                                'Pesisir Samudra Hindia, Kec. Kota Mukomuko', '-2.5830, 101.1240',
                                '3 Desa pesisir merasakan guncangan skala III MMI', '29°C, Cerah Berawan', '29°C, Cerah Berawan',
                                'Pusdalops BPBD Mukomuko melakukan diseminasi informasi dan patroli TRC ke bibir pantai',
                                'Kondisi kondusif, tidak ada potensi tsunami, warga tetap beraktivitas normal',
                                0, 0, 'Nihil kerusakan berat (retak rambut pada 1 pos ronda)',
                                'Aktivitas Subduksi Lempeng Indo-Australia terhadap Eurasia', 'BMKG Stasiun Geofisika Kepahiang',
                                '09/11/2026', '09/11/2026', 'HITATUN ARAZAK', 'PUSDALOPS-MM-01'
                            )
                        """.trimIndent()
                        db.execSQL(sqlLaporan)

                        val sqlLog = """
                            INSERT OR IGNORE INTO log_komunikasi (
                                id, laporanId, waktu, stationIdFrom, stationIdTo, subjek, operatorName
                            ) VALUES (
                                1, 1, '06:18 WIB', 'Radio VHF Ch.1 - Pusdalops', 'Kalaksa BPBD Mukomuko',
                                'Laporan gempa M 4.8 kedalaman 15 km di pesisir Kota Mukomuko. Tidak berpotensi tsunami.',
                                'HITATUN ARAZAK'
                            )
                        """.trimIndent()
                        db.execSQL(sqlLog)

                        // Pre-populate sample Laporan Warga Hari Ini
                        val sqlWarga = """
                            INSERT OR IGNORE INTO laporan_warga (
                                id, tanggal, waktu, namaPelapor, nomorKontak, jenisBencana, lokasi, koordinat, kecamatan, desa, deskripsi, status, timestamp
                            ) VALUES (
                                1, '09-11-2026', '08:30 WIB', 'Budi Santoso', '081273910022', 'Genangan Pasang Air Laut (Rob)',
                                'RT 03 Pasar Bantal', '-2.7123, 101.2345', 'Teramang Jaya', 'Pasar Bantal', 'Air laut pasang naik ke jalan desa setinggi 20 cm, lalu lintas agak terhambat.',
                                'Terverifikasi', 1794281400000
                            )
                        """.trimIndent()
                        db.execSQL(sqlWarga)
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }

        fun getInstance(context: Context): AppDatabase = getDatabase(context)
    }
}
