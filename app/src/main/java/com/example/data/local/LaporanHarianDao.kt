package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) untuk Laporan Harian Pusdalops & Log Komunikasi.
 */
@Dao
interface LaporanHarianDao {

    // === Laporan Harian Queries ===

    @Query("SELECT * FROM laporan_harian ORDER BY id DESC")
    fun getAllLaporanHarian(): Flow<List<LaporanHarian>>

    @Query("SELECT * FROM laporan_harian ORDER BY id DESC")
    suspend fun getAllLaporanHarianSync(): List<LaporanHarian>

    @Query("SELECT * FROM laporan_harian WHERE id = :id")
    fun getLaporanHarianById(id: Int): Flow<LaporanHarian?>

    @Query("SELECT * FROM laporan_harian WHERE id = :id")
    suspend fun getLaporanHarianByIdSync(id: Int): LaporanHarian?

    @Query("SELECT * FROM laporan_harian WHERE tanggalLaporan = :tanggal ORDER BY id DESC")
    fun getLaporanByTanggal(tanggal: String): Flow<List<LaporanHarian>>

    @Query("SELECT * FROM laporan_harian WHERE jenisBencana LIKE '%' || :query || '%' OR lokasi LIKE '%' || :query || '%' OR operatorName LIKE '%' || :query || '%' ORDER BY id DESC")
    fun searchLaporan(query: String): Flow<List<LaporanHarian>>

    @Transaction
    @Query("SELECT * FROM laporan_harian ORDER BY id DESC")
    fun getAllLaporanWithLogs(): Flow<List<LaporanHarianWithLogKomunikasi>>

    @Transaction
    @Query("SELECT * FROM laporan_harian WHERE id = :id")
    fun getLaporanWithLogsById(id: Int): Flow<LaporanHarianWithLogKomunikasi?>

    @Transaction
    @Query("SELECT * FROM laporan_harian WHERE id = :id")
    suspend fun getLaporanWithLogsByIdSync(id: Int): LaporanHarianWithLogKomunikasi?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLaporan(laporan: LaporanHarian): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLaporanList(list: List<LaporanHarian>)

    @Update
    suspend fun updateLaporan(laporan: LaporanHarian)

    @Delete
    suspend fun deleteLaporan(laporan: LaporanHarian)

    @Query("DELETE FROM laporan_harian WHERE id = :id")
    suspend fun deleteLaporanById(id: Int)

    @Query("DELETE FROM laporan_harian")
    suspend fun clearAllLaporan()

    // === Log Komunikasi Queries ===

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogKomunikasi(log: LogKomunikasi): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogKomunikasiList(logs: List<LogKomunikasi>)

    @Query("SELECT * FROM log_komunikasi WHERE laporanId = :laporanId ORDER BY id ASC")
    fun getLogsForLaporan(laporanId: Int): Flow<List<LogKomunikasi>>

    @Query("SELECT * FROM log_komunikasi WHERE laporanId = :laporanId ORDER BY id ASC")
    suspend fun getLogsForLaporanSync(laporanId: Int): List<LogKomunikasi>

    @Query("DELETE FROM log_komunikasi WHERE id = :logId")
    suspend fun deleteLogKomunikasiById(logId: Int)

    @Query("DELETE FROM log_komunikasi WHERE laporanId = :laporanId")
    suspend fun deleteLogsForLaporan(laporanId: Int)
}
