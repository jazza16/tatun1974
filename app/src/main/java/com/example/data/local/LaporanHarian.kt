package com.example.data.local

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room Database Entity untuk Laporan Harian Pusdalops PB BPBD Kabupaten Mukomuko.
 * Menyimpan rekapitulasi data bencana, dampak kerusakan, korban, upaya penanganan,
 * dan identitas stasiun/operator pelapor.
 */
@Entity(
    tableName = "laporan_harian",
    indices = [
        Index(value = ["tanggalLaporan"]),
        Index(value = ["jenisBencana"]),
        Index(value = ["stationId"]),
        Index(value = ["tanggalLaporan", "jenisBencana"])
    ]
)
class LaporanHarianEntity(
    @PrimaryKey(autoGenerate = true) var id: Int = 0,
    var tanggalLaporan: String = "",
    var jenisBencana: String = "",
    var tanggalKejadian: String = "",
    var waktuKejadian: String = "",
    var kedalaman: String = "",
    var lokasi: String = "",
    var koordinat: String = "",
    var cakupanDampak: String = "",
    var kondisiCuaca: String = "",
    var cuacaSaatKejadian: String = "Data cuaca tidak tersedia",
    var upayaDilakukan: String = "",
    var kondisiMutakhir: String = "",
    var korbanLuka: Int = 0,
    var korbanMeninggal: Int = 0,
    var kerusakan: String = "",
    var penyebab: String = "",
    var sumberInformasi: String = "",
    var tanggalSurat: String = "",
    var tanggalTerima: String = "",
    var operatorName: String = "HITATUN ARAZAK",
    var stationId: String = "PUSDALOPS-MM-01"
) {
    @Ignore
    constructor(
        id: Int = 0,
        jenisBencana: String = "",
        tanggalKejadian: String = "",
        waktuKejadian: String = "",
        kedalaman: String = "",
        lokasiBencana: String = "",
        koordinat: String = "",
        cakupanDampak: String = "",
        kondisiCuaca: String = "",
        cuacaSaatKejadian: String = "Data cuaca tidak tersedia",
        upayaDilakukan: String = "",
        kondisiMutakhir: String = "",
        korbanLuka: Int = 0,
        korbanMeninggal: Int = 0,
        kerusakan: String = "",
        penyebab: String = "",
        sumberInformasi: String = "",
        tanggalSurat: String = "",
        tanggalTerima: String = "",
        petugasJaga: String = "HITATUN ARAZAK",
        tanggalLaporanDibuat: String = "",
        stationId: String = "PUSDALOPS-MM-01"
    ) : this(
        id = id,
        tanggalLaporan = tanggalLaporanDibuat,
        jenisBencana = jenisBencana,
        tanggalKejadian = tanggalKejadian,
        waktuKejadian = waktuKejadian,
        kedalaman = kedalaman,
        lokasi = lokasiBencana,
        koordinat = koordinat,
        cakupanDampak = cakupanDampak,
        kondisiCuaca = kondisiCuaca,
        cuacaSaatKejadian = cuacaSaatKejadian,
        upayaDilakukan = upayaDilakukan,
        kondisiMutakhir = kondisiMutakhir,
        korbanLuka = korbanLuka,
        korbanMeninggal = korbanMeninggal,
        kerusakan = kerusakan,
        penyebab = penyebab,
        sumberInformasi = sumberInformasi,
        tanggalSurat = tanggalSurat,
        tanggalTerima = tanggalTerima,
        operatorName = petugasJaga,
        stationId = stationId
    )

    @get:Ignore
    var lokasiBencana: String
        get() = lokasi
        set(value) {
            lokasi = value
        }

    @get:Ignore
    var petugasJaga: String
        get() = operatorName
        set(value) {
            operatorName = value
        }

    @get:Ignore
    var tanggalLaporanDibuat: String
        get() = tanggalLaporan
        set(value) {
            tanggalLaporan = value
        }

    @get:Ignore
    val operatorCanonicalName: String
        get() = OfficialAsnBpbdPersonnel.resolveCanonicalPersonnel(operatorName).name

    @get:Ignore
    val operatorNip: String
        get() = OfficialAsnBpbdPersonnel.resolveCanonicalPersonnel(operatorName).nip

    fun formattedOperatorWithNip(): String {
        val canonical = OfficialAsnBpbdPersonnel.resolveCanonicalPersonnel(operatorName)
        return if (canonical.nip.isNotBlank() && canonical.nip != "-") {
            "${canonical.name} (NIP. ${canonical.nip})"
        } else {
            canonical.name
        }
    }

    fun copy(
        id: Int = this.id,
        jenisBencana: String = this.jenisBencana,
        tanggalKejadian: String = this.tanggalKejadian,
        waktuKejadian: String = this.waktuKejadian,
        kedalaman: String = this.kedalaman,
        lokasiBencana: String = this.lokasi,
        lokasi: String = lokasiBencana,
        koordinat: String = this.koordinat,
        cakupanDampak: String = this.cakupanDampak,
        kondisiCuaca: String = this.kondisiCuaca,
        cuacaSaatKejadian: String = this.cuacaSaatKejadian,
        upayaDilakukan: String = this.upayaDilakukan,
        kondisiMutakhir: String = this.kondisiMutakhir,
        korbanLuka: Int = this.korbanLuka,
        korbanMeninggal: Int = this.korbanMeninggal,
        kerusakan: String = this.kerusakan,
        penyebab: String = this.penyebab,
        sumberInformasi: String = this.sumberInformasi,
        tanggalSurat: String = this.tanggalSurat,
        tanggalTerima: String = this.tanggalTerima,
        petugasJaga: String = this.operatorName,
        operatorName: String = petugasJaga,
        tanggalLaporanDibuat: String = this.tanggalLaporan,
        tanggalLaporan: String = tanggalLaporanDibuat,
        stationId: String = this.stationId
    ): LaporanHarianEntity {
        return LaporanHarianEntity(
            id = id,
            tanggalLaporan = tanggalLaporan,
            jenisBencana = jenisBencana,
            tanggalKejadian = tanggalKejadian,
            waktuKejadian = waktuKejadian,
            kedalaman = kedalaman,
            lokasi = lokasi,
            koordinat = koordinat,
            cakupanDampak = cakupanDampak,
            kondisiCuaca = kondisiCuaca,
            cuacaSaatKejadian = cuacaSaatKejadian,
            upayaDilakukan = upayaDilakukan,
            kondisiMutakhir = kondisiMutakhir,
            korbanLuka = korbanLuka,
            korbanMeninggal = korbanMeninggal,
            kerusakan = kerusakan,
            penyebab = penyebab,
            sumberInformasi = sumberInformasi,
            tanggalSurat = tanggalSurat,
            tanggalTerima = tanggalTerima,
            operatorName = operatorName,
            stationId = stationId
        )
    }
}

typealias LaporanHarian = LaporanHarianEntity
