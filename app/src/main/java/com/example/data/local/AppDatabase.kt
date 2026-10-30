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
        Personnel::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun weatherDao(): WeatherDao
    abstract fun personnelDao(): PersonnelDao

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
                        // Pre-populate the 8 official ASN BPBD operators with separate name and NIP columns
                        for (op in OfficialAsnBpbdPersonnel.OPERATORS) {
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
