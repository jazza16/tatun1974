package com.example

import com.example.data.local.OfficialAsnBpbdPersonnel
import com.example.data.local.OfficerRoles
import com.example.data.repository.PoskoLogisticCommodity
import com.example.data.repository.PoskoLogisticsRepository
import com.example.security.UserRole
import com.example.util.PoskoStockOpnameExporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Mandatory Regression Test Suite: Officer Login Session & Post-Login Modules.
 * Wajib dijalankan setiap kali ada penambahan modul, fitur, atau fungsi baru pada SIPAGAR KITA
 * untuk memastikan 100% sesi login Petugas Pusdalops/TRC/Posko bebas dari Force Close.
 */
class PoskoLogisticsAndOfficerSessionTest {

    @Test
    fun mandatoryOfficerLoginSessionSmokeTest_allRolesAndPostLoginModules_100PercentSuccess() {
        // 1. Verifikasi seluruh Role RBAC Petugas (Tier 1 Manajer, Tier 2 Koordinator/Pengawas, Tier 3 Operator/TRC/Posko)
        val allRolesToVerify = listOf(
            "Manajer Pusdalops-PB",
            "Pengawas BPBD",
            "Supervisor BPBD",
            "Koord. Administrasi",
            "Operator",
            "Logistik",
            "TRC",
            OfficerRoles.PETUGAS_JAGA,
            "petugas_posko",
            "operator_pusdalops",
            "kepala_pusdalops",
            "kalak"
        )
        for (roleName in allRolesToVerify) {
            val resolvedRole = UserRole.fromRoleString(roleName)
            assertNotNull("Role $roleName wajib terpetakan dengan aman tanpa exception", resolvedRole)
            assertTrue(resolvedRole.tierCode.isNotBlank())
            assertTrue(resolvedRole.tierDisplayName.isNotBlank())
        }

        // 2. Verifikasi Roster Personil Resmi ASN/TRC BPBD Mukomuko saat sesi Login Petugas
        val officialRoster = OfficialAsnBpbdPersonnel.ALL_OFFICIAL_PERSONNEL
        assertTrue("Roster personil resmi BPBD tidak boleh kosong", officialRoster.isNotEmpty())
        val firstOfficer = officialRoster.first()
        assertTrue(firstOfficer.nip.isNotBlank())
        assertTrue(firstOfficer.name.isNotBlank())

        // 3. Verifikasi Inisialisasi Modul Posko, Pengungsi, & Logistik tepat saat isOfficerLoggedIn == true
        val repository = PoskoLogisticsRepository.createForInMemoryTest()
        assertTrue(repository.poskoUnits.value.isNotEmpty())
        assertTrue(repository.logisticStocks.value.isNotEmpty())
        assertTrue(repository.distributionLedger.value.isNotEmpty())

        val firstPosko = repository.poskoUnits.value.first()

        // 4. Verifikasi Validasi Ketat Pengungsi: (Lansia + Ibu Hamil + Balita + Disabilitas) <= Total Jiwa
        val invalidResult = repository.validateAndUpdatePoskoRefugees(
            poskoId = firstPosko.poskoId,
            totalJiwa = 50,
            lansia = 20,
            ibuHamil = 10,
            balita = 15,
            disabilitas = 10, // Total rentan = 55 > 50 (Wajib ditolak)
            officerName = firstOfficer.name
        )
        assertTrue(invalidResult.isFailure)

        val validResult = repository.validateAndUpdatePoskoRefugees(
            poskoId = firstPosko.poskoId,
            totalJiwa = 100,
            lansia = 20,
            ibuHamil = 5,
            balita = 15,
            disabilitas = 5, // Total rentan = 45 <= 100 (Wajib diterima)
            officerName = firstOfficer.name
        )
        assertTrue(validResult.isSuccess)

        // 5. Verifikasi Indikator Stok Kritis (< 10) & FEFO
        val criticalItems = repository.logisticStocks.value.filter { it.isCritical }
        assertTrue(criticalItems.isNotEmpty())
        assertFalse(
            PoskoLogisticCommodity(
                code = "TEST-10",
                name = "Beras Cadangan",
                category = "PANGAN",
                unit = "Paket",
                currentStock = 10,
                minThreshold = 10,
                expiryDateIso = "2027-01-01",
                poskoCode = firstPosko.poskoCode
            ).isCritical
        )

        // 6. Verifikasi Distribusi Bantuan Berbasis NIK 16 Digit (Anti-Data Ganda)
        val targetItem = repository.logisticStocks.value.first { it.currentStock >= 5 }
        val distributeSuccess = repository.distributeLogisticToNik(
            recipientNik = "1706019988776655",
            recipientNoKk = "1706010000111122",
            recipientName = "Warga Uji",
            vulnerabilityGroup = "Lansia",
            commodityCode = targetItem.code,
            quantity = 2,
            poskoCode = targetItem.poskoCode,
            officerName = firstOfficer.name
        )
        assertTrue(distributeSuccess.isSuccess)

        val distributeDuplicate = repository.distributeLogisticToNik(
            recipientNik = "1706019988776655",
            recipientNoKk = "1706010000111122",
            recipientName = "Warga Uji",
            vulnerabilityGroup = "Lansia",
            commodityCode = targetItem.code,
            quantity = 1,
            poskoCode = targetItem.poskoCode,
            officerName = firstOfficer.name
        )
        assertTrue(distributeDuplicate.isFailure)

        // 7. Verifikasi Auto-Populate & Kunci Snapshot Posko ke DaLA
        val snapshot = repository.captureAndLockDalaSnapshot(
            officerName = firstOfficer.name
        )
        assertNotNull(snapshot)
        assertTrue(snapshot.isLocked)
        assertEquals(snapshot.totalJiwa, repository.dalaLockedSnapshot.value?.totalJiwa)

        // 8. Verifikasi Fungsi Unduh Dokumen Resmi (BA Stock Opname, LPJ Mutasi, & BA Rekonsiliasi Aset BMN/BMD)
        // beserta Kop Surat Standar Resmi dan Footer Standar
        val csvBaStockOpname = PoskoStockOpnameExporter.buildStockOpnameCsvString(
            stocks = repository.logisticStocks.value,
            poskoUnits = repository.poskoUnits.value,
            distributionLedger = repository.distributionLedger.value,
            officerName = firstOfficer.name,
            officerNip = firstOfficer.nip,
            officerRole = firstOfficer.role,
            documentPurpose = PoskoStockOpnameExporter.OFFICIAL_REPORT_PURPOSES[0]
        )
        assertTrue(csvBaStockOpname.contains(com.example.model.DocumentSettings.DEFAULT_KOP_LINE_1))
        assertTrue(csvBaStockOpname.contains(com.example.model.DocumentSettings.DEFAULT_KOP_LINE_2))
        assertTrue(csvBaStockOpname.contains("BERITA ACARA STOCK OPNAME LOGISTIK & TATA KELOLA BARANG MILIK NEGARA / DAERAH (BMN/BMD)"))
        assertTrue(csvBaStockOpname.contains(com.example.model.DocumentSettings.DEFAULT_FOOTER_TEXT))
        assertTrue(csvBaStockOpname.contains("LOG-SEMBAKO"))
        assertTrue(csvBaStockOpname.contains("1706019988776655"))

        val csvLpjMutasi = PoskoStockOpnameExporter.buildStockOpnameCsvString(
            stocks = repository.logisticStocks.value,
            poskoUnits = repository.poskoUnits.value,
            distributionLedger = repository.distributionLedger.value,
            officerName = firstOfficer.name,
            officerNip = firstOfficer.nip,
            officerRole = firstOfficer.role,
            documentPurpose = PoskoStockOpnameExporter.OFFICIAL_REPORT_PURPOSES[1]
        )
        assertTrue(csvLpjMutasi.contains("LAPORAN PERTANGGUNGJAWABAN MUTASI PERSEDIAAN & DISTRIBUSI BANTUAN (BMN/BMD)"))
        assertTrue(csvLpjMutasi.contains(com.example.model.DocumentSettings.DEFAULT_FOOTER_TEXT))

        val csvRekonsiliasi = PoskoStockOpnameExporter.buildStockOpnameCsvString(
            stocks = repository.logisticStocks.value,
            poskoUnits = repository.poskoUnits.value,
            distributionLedger = repository.distributionLedger.value,
            officerName = firstOfficer.name,
            officerNip = firstOfficer.nip,
            officerRole = firstOfficer.role,
            documentPurpose = PoskoStockOpnameExporter.OFFICIAL_REPORT_PURPOSES[2]
        )
        assertTrue(csvRekonsiliasi.contains("BERITA ACARA REKONSILIASI ASET KEBENCANAAN & TATA KELOLA BMN / BMD"))
        assertTrue(csvRekonsiliasi.contains(com.example.model.DocumentSettings.DEFAULT_FOOTER_TEXT))

        // 9. Verifikasi Presisi Jarak Tabel ke Blok Pejabat Penanda Tangan (tanpa pemborosan ruang kosong)
        assertEquals(456f, PoskoStockOpnameExporter.computeSignatureBlockTopY(440f), 0.1f)
        assertEquals(710f, PoskoStockOpnameExporter.computeSignatureBlockTopY(760f), 0.1f)

        // 10. Verifikasi Standarisasi Kertas Kerja ISO A4 & Margin Simetris pada Seluruh Output PDF
        assertEquals(595, com.example.util.ReportExporter.A4_PAGE_WIDTH)
        assertEquals(842, com.example.util.ReportExporter.A4_PAGE_HEIGHT)
        assertEquals(36f, com.example.util.ReportExporter.STANDARD_MARGIN, 0.01f)
        assertEquals(523f, com.example.util.ReportExporter.STANDARD_CONTENT_WIDTH, 0.01f)
        assertEquals(297.5f, com.example.util.ReportExporter.A4_CENTER_X, 0.01f)
    }
}
