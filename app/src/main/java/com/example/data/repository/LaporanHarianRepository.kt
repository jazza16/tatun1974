package com.example.data.repository

import com.example.data.local.LaporanHarian
import com.example.data.local.LaporanHarianDao
import com.example.data.local.LaporanHarianWithLogKomunikasi
import com.example.data.local.LogKomunikasi
import kotlinx.coroutines.flow.Flow

/**
 * Repository pattern untuk mengabstraksi akses data Laporan Harian dan Log Komunikasi.
 */
class LaporanHarianRepository(private val dao: LaporanHarianDao) {

    val allLaporan: Flow<List<LaporanHarian>> = dao.getAllLaporanHarian()
    val allLaporanWithLogs: Flow<List<LaporanHarianWithLogKomunikasi>> = dao.getAllLaporanWithLogs()

    fun getLaporanById(id: Int): Flow<LaporanHarian?> = dao.getLaporanHarianById(id)

    suspend fun getLaporanByIdSync(id: Int): LaporanHarian? = dao.getLaporanHarianByIdSync(id)

    fun getLaporanWithLogsById(id: Int): Flow<LaporanHarianWithLogKomunikasi?> = dao.getLaporanWithLogsById(id)

    fun searchLaporan(query: String): Flow<List<LaporanHarian>> = dao.searchLaporan(query)

    suspend fun insertLaporan(laporan: LaporanHarian): Long = dao.insertLaporan(laporan)

    suspend fun updateLaporan(laporan: LaporanHarian) = dao.updateLaporan(laporan)

    suspend fun deleteLaporan(laporan: LaporanHarian) = dao.deleteLaporan(laporan)

    suspend fun deleteLaporanById(id: Int) = dao.deleteLaporanById(id)

    suspend fun addLogKomunikasi(log: LogKomunikasi): Long = dao.insertLogKomunikasi(log)

    suspend fun addLogs(logs: List<LogKomunikasi>) = dao.insertLogKomunikasiList(logs)

    fun getLogsForLaporan(laporanId: Int): Flow<List<LogKomunikasi>> = dao.getLogsForLaporan(laporanId)

    suspend fun deleteLogById(logId: Int) = dao.deleteLogKomunikasiById(logId)
}
