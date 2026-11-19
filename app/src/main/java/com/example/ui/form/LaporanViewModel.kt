package com.example.ui.form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.DistribusiLogistikEntity
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
        kondisiCuaca = "Data cuaca tidak tersedia",
        cuacaSaatKejadian = "Data cuaca tidak tersedia",
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

    // 3. Wadah untuk Distribusi Logistik / Aset Bantuan (bisa lebih dari 1)
    var listLogistikSementara = mutableListOf<DistribusiLogistikEntity>()

    // 4. Fungsi untuk menyimpan ke Database Room
    // Mengambil namaOperator dan idStasiun langsung dari token sesi terverifikasi (bukan dari input form body)
    fun simpanKeDatabase(
        dao: LaporanHarianDao,
        verifiedUser: com.example.util.SessionManager.VerifiedOperatorUser? = null,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val isKondusif = laporanSementara.jenisBencana.contains("Kondusif", ignoreCase = true) ||
                    laporanSementara.jenisBencana.contains("Tidak Ada", ignoreCase = true)

                val coordCheck = com.example.util.CoordinateValidator.validateKoordinatString(
                    koordinat = laporanSementara.koordinat,
                    allowKondusifDash = isKondusif
                )
                if (coordCheck.isFailure) {
                    onError(coordCheck.exceptionOrNull()?.message ?: com.example.util.CoordinateValidator.ERROR_INVALID_RANGE)
                    return@launch
                }

                val namaOperator = verifiedUser?.name?.takeIf { it.isNotBlank() }
                    ?: laporanSementara.petugasJaga.ifBlank { com.example.util.SessionManager.DEFAULT_NAMA_PETUGAS }
                val idStasiun = verifiedUser?.stationId?.takeIf { it.isNotBlank() }
                    ?: com.example.util.SessionManager.DEFAULT_STATION_ID
                val nipOperator = verifiedUser?.nip?.takeIf { it.isNotBlank() }
                    ?: com.example.util.SessionManager.DEFAULT_NIP_PETUGAS

                val verifiedLaporan = laporanSementara.copy(
                    petugasJaga = namaOperator,
                    stationId = idStasiun
                )
                laporanSementara = verifiedLaporan

                // Jika lolos validasi koordinat & token, simpan Laporan Utama ke database
                val laporanId = dao.insertLaporan(verifiedLaporan).toInt()

                // Simpan semua Log yang ada di dalam list dengan identitas operator terverifikasi
                for (log in listLogSementara) {
                    val logDenganId = log.copy(
                        laporanId = laporanId,
                        operatorName = namaOperator
                    )
                    dao.insertLog(logDenganId)
                }

                // Simpan semua Distribusi Logistik yang ada di dalam list
                for (item in listLogistikSementara) {
                    val itemDenganId = item.copy(laporanId = laporanId)
                    dao.insertLogistik(itemDenganId)
                }

                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Gagal menyimpan data")
            }
        }
    }
}

