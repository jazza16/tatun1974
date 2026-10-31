package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        WeatherCacheEntity::class,
        FavoriteDistrictEntity::class,
        CommunityReportEntity::class,
        HistoricalWeatherEntity::class,
        CitizenDisasterReportEntity::class,
        HazardRiskZoneEntity::class,
        Personnel::class,
        PersonnelAuditLogEntity::class,
        SyncConflictLogEntity::class
    ],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun weatherDao(): WeatherDao
    abstract fun personnelDao(): PersonnelDao
    abstract fun auditLogDao(): PersonnelAuditLogDao
    abstract fun syncConflictLogDao(): SyncConflictLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cuaca_mukomuko.db"
                ).fallbackToDestructiveMigration()
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Pre-populate official ASN BPBD personnel (leadership & operators)
                        for (op in OfficialAsnBpbdPersonnel.ALL_OFFICIAL_PERSONNEL) {
                            val sql = "INSERT OR IGNORE INTO personnel (name, nip, role, unit, shift, phone) VALUES (?, ?, ?, ?, ?, ?)"
                            db.execSQL(sql, arrayOf(op.name, op.nip, op.role, op.unit, op.shift, op.phone))
                        }
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
