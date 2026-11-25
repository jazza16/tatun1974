package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) untuk tabel 'laporan_warga'.
 */
@Dao
interface LaporanWargaDao {

    @Query("SELECT * FROM laporan_warga WHERE tanggal = :tanggalHariIni")
    fun getLaporanHariIni(tanggalHariIni: String): List<LaporanWargaEntity>

    // Ambil laporan warga berdasarkan tanggal (misal format "09-11-2026")
    @Query("SELECT * FROM laporan_warga WHERE tanggal LIKE :tanggal || '%' ORDER BY id DESC")
    fun getLaporanWargaByDate(tanggal: String): List<LaporanWargaEntity>

    @Query("SELECT * FROM laporan_warga WHERE tanggal = :tanggalHariIni ORDER BY id DESC")
    fun getLaporanHariIniFlow(tanggalHariIni: String): Flow<List<LaporanWargaEntity>>

    @Query("SELECT * FROM laporan_warga ORDER BY id DESC")
    fun getAllLaporanWarga(): Flow<List<LaporanWargaEntity>>

    @Query("SELECT * FROM laporan_warga ORDER BY id DESC")
    fun getAllLaporanWargaSync(): List<LaporanWargaEntity>

    @Query("SELECT * FROM laporan_warga WHERE id = :id")
    fun getLaporanWargaById(id: Int): LaporanWargaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLaporanWarga(laporan: LaporanWargaEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLaporanWargaList(list: List<LaporanWargaEntity>)

    @Update
    suspend fun updateLaporanWarga(laporan: LaporanWargaEntity)

    @Delete
    suspend fun deleteLaporanWarga(laporan: LaporanWargaEntity)

    @Query("DELETE FROM laporan_warga WHERE id = :id")
    suspend fun deleteLaporanWargaById(id: Int)

    @Query("DELETE FROM laporan_warga WHERE id > 1")
    suspend fun deleteNonDefaultLaporanWarga(): Int
}
