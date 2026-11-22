package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.LaporanHarianEntity
import com.example.data.repository.WeatherRepository
import com.example.model.CitizenDisasterReport
import com.example.model.CitizenReportStatusHelper
import com.example.training.SandboxTrainingManager
import com.example.ui.form.CitizenIncomingReportsProvider
import com.example.util.PdfGenerator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SandboxTrainingModeTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var repository: WeatherRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = WeatherRepository(database.weatherDao())
        SandboxTrainingManager.setSandboxModeEnabled(context, false)
        SandboxTrainingManager.resetSandboxData(context, reseedDefaults = false)
    }

    @After
    fun tearDown() {
        SandboxTrainingManager.setSandboxModeEnabled(context, false)
        SandboxTrainingManager.resetSandboxData(context, reseedDefaults = false)
        database.close()
    }

    @Test
    fun `verify Sandbox Training Mode isolates reporting validation autofill and PDF generation from production database`() = runBlocking {
        // 1. Pastikan database produksi awal bersih (0 laporan)
        assertEquals(0, database.weatherDao().getCitizenReportCount())
        assertEquals(0, database.laporanHarianDao().getAllLaporanHarianSync().size)
        assertFalse(SandboxTrainingManager.isSandboxModeActive())

        // 2. Aktifkan Mode Latihan Petugas (Sandbox)
        SandboxTrainingManager.setSandboxModeEnabled(context, true)
        assertTrue(SandboxTrainingManager.isSandboxModeActive())

        // Skenario awal otomatis terisi di Sandbox, namun database Room produksi tetap 0
        val initialSandboxReports = repository.getCitizenDisasterReports().first()
        assertEquals(2, initialSandboxReports.size)
        assertEquals(0, database.weatherDao().getCitizenReportCount())

        // 3.Langkah 1: Suntikkan simulasi laporan warga baru & kirim laporan dari Mode Warga
        val injected = SandboxTrainingManager.injectSimulatedCitizenReport(
            context = context,
            scenarioIndex = 2,
            triggerRealtimeAlertBanner = true
        )
        assertTrue(injected.id.startsWith(SandboxTrainingManager.SANDBOX_ID_PREFIX))

        val citizenDrillReport = CitizenDisasterReport(
            id = "LAP-WARGA-UJI-01",
            disasterType = "Banjir",
            incidentTime = "21 Nov 2026, 15:00 WIB",
            incidentLocation = "Desa Pasar Mukomuko",
            district = "Kota Mukomuko",
            village = "Pasar Mukomuko",
            latitude = -2.5562,
            longitude = 101.1184,
            reporterName = "Warga Latihan",
            status = CitizenReportStatusHelper.STATUS_DIPROSES
        )
        val submitResult = repository.submitCitizenDisasterReportWithResult(citizenDrillReport, context)
        assertTrue(submitResult.reportId.startsWith(SandboxTrainingManager.SANDBOX_ID_PREFIX))
        assertTrue(
            SandboxTrainingManager.completedSteps.value.contains(
                SandboxTrainingManager.STEP_1_INJECT_OR_REPORT
            )
        )

        // Pastikan TIDAK ADA satupun baris latihan yang bocor ke Room database produksi
        assertEquals(0, database.weatherDao().getCitizenReportCount())
        assertEquals(4, repository.getCitizenDisasterReports().first().size)

        // 4. Langkah 2: Petugas melakukan validasi status & catatan tindak lanjut TRC
        val updatedOk = repository.updateCitizenReportStatusAndNotes(
            reportId = injected.id,
            newStatus = CitizenReportStatusHelper.STATUS_DIVALIDASI,
            notes = "[Latihan TRC] Regu piket telah memverifikasi lokasi simulasi.",
            officerName = "Operator Latihan Pusdalops",
            officerNip = "199001012020121001",
            officerRole = "Operator Pusdalops"
        )
        assertTrue(updatedOk)
        val updatedInSandbox = SandboxTrainingManager.sandboxReports.value.first { it.id == injected.id }
        assertEquals(CitizenReportStatusHelper.STATUS_DIVALIDASI, updatedInSandbox.status)
        assertTrue(
            SandboxTrainingManager.completedSteps.value.contains(
                SandboxTrainingManager.STEP_2_VALIDATE_STATUS
            )
        )
        assertEquals(0, database.weatherDao().getCitizenReportCount())

        // 5. Langkah 3: Tarik ke "DARI WARGA" & Simpan Form Laporan Harian di Sandbox
        val incomingForDailyForm = CitizenIncomingReportsProvider.loadIncomingCitizenReports(context, database)
        assertEquals(4, incomingForDailyForm.size)
        val chosenIncoming = incomingForDailyForm.first { it.id == injected.id }
        CitizenIncomingReportsProvider.markReportAsUsedByOfficer(context, database, chosenIncoming)

        val trainingDailyReport = LaporanHarianEntity(
            tanggalLaporan = "21-11-2026",
            jenisBencana = chosenIncoming.disasterType,
            tanggalKejadian = chosenIncoming.dateText,
            waktuKejadian = chosenIncoming.timeText,
            lokasi = chosenIncoming.fullFormattedLocation(),
            koordinat = chosenIncoming.coordinates,
            upayaDilakukan = "Simulasi Evakuasi & Pendataan TRC",
            kondisiMutakhir = "Terkendali (Latihan)",
            operatorName = "Operator Latihan Pusdalops",
            stationId = "PUSDALOPS-MM-01"
        )
        val sandboxDailyId = SandboxTrainingManager.recordSandboxDailyReportSaved(trainingDailyReport)
        assertTrue(sandboxDailyId >= 9000)
        assertEquals(1, SandboxTrainingManager.sandboxDailyReports.value.size)
        assertEquals(0, database.laporanHarianDao().getAllLaporanHarianSync().size)
        assertTrue(
            SandboxTrainingManager.completedSteps.value.contains(
                SandboxTrainingManager.STEP_3_AUTOFILL_DAILY_FORM
            )
        )

        // 6. Langkah 4: Generate PDF Laporan Harian dengan Watermark Latihan
        val pdfBytes = PdfGenerator.generateLaporanPdfBytes(
            context = context,
            laporan = trainingDailyReport,
            listLog = emptyList(),
            listLogistik = emptyList()
        )
        assertNotNull(pdfBytes)
        assertTrue(pdfBytes.isNotEmpty())
        assertTrue(String(pdfBytes).startsWith("%PDF"))
        assertTrue(
            SandboxTrainingManager.completedSteps.value.contains(
                SandboxTrainingManager.STEP_4_PRINT_PDF
            )
        )

        // 7. Matikan Mode Latihan -> Aplikasi langsung kembali ke database produksi yang bersih 100%
        SandboxTrainingManager.setSandboxModeEnabled(context, false)
        assertFalse(SandboxTrainingManager.isSandboxModeActive())
        assertEquals(0, repository.getCitizenDisasterReports().first().size)
        assertEquals(0, database.weatherDao().getCitizenReportCount())
        assertEquals(0, database.laporanHarianDao().getAllLaporanHarianSync().size)
    }
}
