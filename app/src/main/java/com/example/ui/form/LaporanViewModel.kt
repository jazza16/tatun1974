package com.example.ui.form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.LaporanHarianDao
import com.example.data.local.LaporanHarianEntity
import com.example.data.local.LogKomunikasiEntity
import kotlinx.coroutines.launch

class LaporanViewModel : ViewModel() {

    // 1. Wadah data sementara (default kosong)
    var laporanSementara = LaporanHarianEntity(
        jenisBencana = "",
        tanggalKejadian = "",
        waktuKejadian = "",
        kedalaman = "",
        lokasiBencana = "",
        koordinat = "",
        cakupanDampak = "",
        kondisiCuaca = "",
        upayaDilakukan = "",
        kondisiMutakhir = "",
        korbanLuka = 0,
        korbanMeninggal = 0,
        kerusakan = "",
        penyebab = "",
        sumberInformasi = "",
        tanggalSurat = "",
        tanggalTerima = "",
        petugasJaga = "HITATUN ARAZAK", // Bisa diisi dari DataStore nanti
        tanggalLaporanDibuat = ""
    )

    // 2. Wadah untuk Log BPBD / BNPB (bisa lebih dari 1)
    var listLogSementara = mutableListOf<LogKomunikasiEntity>()

    // 3. Fungsi untuk menyimpan ke Database Room
    fun simpanKeDatabase(dao: LaporanHarianDao, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                // Simpan Laporan Utama
                val laporanId = dao.insertLaporan(laporanSementara).toInt()

                // Simpan semua Log yang ada di dalam list
                for (log in listLogSementara) {
                    val logDenganId = log.copy(laporanId = laporanId)
                    dao.insertLog(logDenganId)
                }

                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Gagal menyimpan data")
            }
        }
    }
}
