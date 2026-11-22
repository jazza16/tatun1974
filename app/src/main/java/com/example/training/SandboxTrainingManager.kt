package com.example.training

import android.content.Context
import com.example.R
import com.example.data.local.LaporanHarianEntity
import com.example.model.CitizenDisasterReport
import com.example.model.CitizenReportStatusHelper
import com.example.ui.components.OfficerRealtimeNewReportDetector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

/**
 * Manajer Mode Latihan Petugas (Sandbox Training Mode) SIPAGAR KITA.
 *
 * Menyediakan lingkungan simulasi 100% terisolasi bagi operator Pusdalops-PB dan TRC BPBD
 * Kabupaten Mukomuko agar latihan:
 * 1. Penerimaan & Pengiriman Laporan Bencana Warga (beserta simulasi sirine/alert darurat)
 * 2. Validasi Status & Catatan Tindak Lanjut Lapangan (Diproses -> Divalidasi -> Selesai)
 * 3. Tarik Otomatis ("DARI WARGA") ke Formulir Laporan Harian Pusdalops
 * 4. Cetak & Ekspor Dokumen PDF / CSV Laporan Harian (dengan watermark latihan)
 *
 * JAMINAN ISOLASI PRODUKSI:
 * Selama Mode Latihan aktif, SELURUH operasi baca/tulis dialihkan ke memori Sandbox ini
 * dan DILARANG menulis ke database Room produksi, Cloud Firestore produksi, maupun bucket Supabase produksi.
 */
object SandboxTrainingManager {

    const val SANDBOX_ID_PREFIX = "SIM-LAT-"
    const val WATERMARK_HEADER_TEXT = "SIMULASI LATIHAN PETUGAS - BUKAN DATA PRODUKSI RESMI"
    const val WATERMARK_SHORT_BADGE = "[MODE LATIHAN / SANDBOX - NON PRODUKSI]"

    const val STEP_1_INJECT_OR_REPORT = "STEP_1_INJECT_OR_REPORT"
    const val STEP_2_VALIDATE_STATUS = "STEP_2_VALIDATE_STATUS"
    const val STEP_3_AUTOFILL_DAILY_FORM = "STEP_3_AUTOFILL_DAILY_FORM"
    const val STEP_4_PRINT_PDF = "STEP_4_PRINT_PDF"

    data class TrainingScenario(
        val index: Int,
        val code: String,
        val title: String,
        val disasterType: String,
        val district: String,
        val village: String,
        val incidentLocation: String,
        val latitude: Double,
        val longitude: Double,
        val casualtiesDead: Int,
        val casualtiesInjured: Int,
        val casualtiesMissing: Int,
        val reporterName: String,
        val reporterPhone: String,
        val additionalNotes: String
    )

    val predefinedScenarios: List<TrainingScenario> = listOf(
        TrainingScenario(
            index = 0,
            code = "DRILL-BANJIR-01",
            title = "Skenario 1: Banjir Luapan Sungai Selagan",
            disasterType = "Banjir",
            district = "Kota Mukomuko",
            village = "Pasar Mukomuko",
            incidentLocation = "[SIMULASI LATIHAN] Bantaran Sungai Selagan RT 03 Pasar Mukomuko (Tinggi Muka Air 85 cm)",
            latitude = -2.5562,
            longitude = 101.1184,
            casualtiesDead = 0,
            casualtiesInjured = 2,
            casualtiesMissing = 0,
            reporterName = "Warga Simulasi (Bpk. Hendra - Ketua RT)",
            reporterPhone = "081278001101",
            additionalNotes = "[DATA LATIHAN SANDBOX] Air Sungai Selagan meluap sejak hujan lebat 3 jam terakhir. Sekitar 18 rumah warga tergenang 60-85 cm, warga lansia butuh bantuan evakuasi perahu karet TRC."
        ),
        TrainingScenario(
            index = 1,
            code = "DRILL-LONGSOR-02",
            title = "Skenario 2: Tanah Longsor Jalan Lintas Barat Ipuh",
            disasterType = "Tanah Longsor",
            district = "Ipuh",
            village = "Medan Jaya",
            incidentLocation = "[SIMULASI LATIHAN] Tanjakan Tebing Jalinbar KM 42 Desa Medan Jaya, Kec. Ipuh",
            latitude = -3.1492,
            longitude = 101.5123,
            casualtiesDead = 0,
            casualtiesInjured = 1,
            casualtiesMissing = 0,
            reporterName = "Warga Simulasi (Sdr. Riko - Pengemudi Lintas)",
            reporterPhone = "085267002202",
            additionalNotes = "[DATA LATIHAN SANDBOX] Material longsoran tanah tebing dan pohon tumbang menutup separuh badan Jalan Lintas Barat Sumatera. Arus lalu lintas sistem buka-tutup, butuh gergaji mesin dan koordinasi alat berat."
        ),
        TrainingScenario(
            index = 2,
            code = "DRILL-GEMPA-03",
            title = "Skenario 3: Gempa Bumi Pesisir & Retakan Dinding",
            disasterType = "Gempa Bumi",
            district = "Air Rami",
            village = "Rami Mulia",
            incidentLocation = "[SIMULASI LATIHAN] Dusun II Desa Rami Mulia, Kec. Air Rami",
            latitude = -3.0218,
            longitude = 101.5941,
            casualtiesDead = 0,
            casualtiesInjured = 3,
            casualtiesMissing = 0,
            reporterName = "Warga Simulasi (Ibu Suryani - Perangkat Desa)",
            reporterPhone = "082179003303",
            additionalNotes = "[DATA LATIHAN SANDBOX] Guncangan gempa terasa kuat selama 12 detik. Dinding bangunan Pustu dan 4 rumah warga mengalami retak sedang, 3 warga luka ringan akibat tertimpa plafon."
        ),
        TrainingScenario(
            index = 3,
            code = "DRILL-KARHUTLA-04",
            title = "Skenario 4: Kebakaran Lahan Gambut Penarik",
            disasterType = "Kebakaran Hutan dan Lahan",
            district = "Penarik",
            village = "Penarik",
            incidentLocation = "[SIMULASI LATIHAN] Area Lahan Semak & Gambut Blok C Desa Penarik",
            latitude = -2.6540,
            longitude = 101.2890,
            casualtiesDead = 0,
            casualtiesInjured = 0,
            casualtiesMissing = 0,
            reporterName = "Warga Simulasi (Bpk. Darmawan - Pokmas)",
            reporterPhone = "081366004404",
            additionalNotes = "[DATA LATIHAN SANDBOX] Titik asap dan api terpantau meluas sekitar 1,5 hektar akibat angin kencang siang hari. Jarak ke permukiman warga sekitar 400 meter, butuh unit pompa pemadam portable."
        )
    )

    private val sequenceCounter = AtomicInteger(100)
    private val dailyReportIdCounter = AtomicInteger(9001)
    private var nextScenarioCursor = 0

    private val _isSandboxEnabled = MutableStateFlow(false)
    val isSandboxEnabled: StateFlow<Boolean> = _isSandboxEnabled.asStateFlow()

    private val _sandboxReports = MutableStateFlow<List<CitizenDisasterReport>>(emptyList())
    val sandboxReports: StateFlow<List<CitizenDisasterReport>> = _sandboxReports.asStateFlow()

    private val _sandboxDailyReports = MutableStateFlow<List<LaporanHarianEntity>>(emptyList())
    val sandboxDailyReports: StateFlow<List<LaporanHarianEntity>> = _sandboxDailyReports.asStateFlow()

    private val _completedSteps = MutableStateFlow<Set<String>>(emptySet())
    val completedSteps: StateFlow<Set<String>> = _completedSteps.asStateFlow()

    private val _lastActionSummary = MutableStateFlow<String?>(null)
    val lastActionSummary: StateFlow<String?> = _lastActionSummary.asStateFlow()

    fun isSandboxModeActive(): Boolean = _isSandboxEnabled.value

    fun isSandboxReportId(reportId: String?): Boolean {
        return !reportId.isNullOrBlank() && reportId.trim().startsWith(SANDBOX_ID_PREFIX, ignoreCase = true)
    }

    private fun defaultBundledTrainingPhotoUri(context: Context? = null): String {
        val pkg = context?.packageName ?: "com.example"
        return "android.resource://$pkg/${R.drawable.img_mukomuko_disaster_wallpaper}"
    }

    private fun formatTrainingTime(timestampMs: Long = System.currentTimeMillis()): String {
        val formatter = SimpleDateFormat("dd MMM yyyy, HH:mm 'WIB'", Locale("id", "ID"))
        return formatter.format(Date(timestampMs))
    }

    /**
     * Mengaktifkan atau menonaktifkan Mode Latihan Petugas (Sandbox).
     * Saat diaktifkan, langsung menyiapkan paket skenario latihan awal di memori terisolasi.
     * Saat dinonaktifkan, seluruh tampilan otomatis kembali ke data produksi asli tanpa jejak latihan.
     */
    @Synchronized
    fun setSandboxModeEnabled(context: Context? = null, enabled: Boolean) {
        if (_isSandboxEnabled.value == enabled) return
        _isSandboxEnabled.value = enabled
        if (enabled) {
            if (_sandboxReports.value.isEmpty()) {
                seedInitialTrainingReports(context)
            }
            _lastActionSummary.value = "Mode Latihan Petugas Aktif — Database Produksi 100% Terlindungi."
        } else {
            _lastActionSummary.value = null
        }
    }

    /**
     * Menyiapkan 2 laporan simulasi awal (1 berstatus Diproses, 1 berstatus Divalidasi)
     * agar petugas langsung memiliki bahan latihan begitu mengaktifkan Sandbox.
     */
    @Synchronized
    fun seedInitialTrainingReports(context: Context? = null) {
        val now = System.currentTimeMillis()
        val photoUri = defaultBundledTrainingPhotoUri(context)
        val s1 = predefinedScenarios[0]
        val s2 = predefinedScenarios[1]

        val initial1 = CitizenDisasterReport(
            id = "${SANDBOX_ID_PREFIX}DRILL-001",
            disasterType = s1.disasterType,
            incidentTime = formatTrainingTime(now - 10 * 60_000L),
            incidentLocation = s1.incidentLocation,
            district = s1.district,
            village = s1.village,
            latitude = s1.latitude,
            longitude = s1.longitude,
            gpsAccuracyMeters = 6.5f,
            isGpsVerified = true,
            casualtiesDead = s1.casualtiesDead,
            casualtiesInjured = s1.casualtiesInjured,
            casualtiesMissing = s1.casualtiesMissing,
            photoUri = photoUri,
            reporterName = s1.reporterName,
            reporterPhone = s1.reporterPhone,
            additionalNotes = s1.additionalNotes,
            timestamp = now - 10 * 60_000L,
            status = CitizenReportStatusHelper.STATUS_DIPROSES,
            updatedAt = now - 10 * 60_000L,
            handledByOfficerName = "",
            handledByOfficerNip = "",
            handledByOfficerRole = "",
            isSyncedToCloud = true
        )

        val initial2 = CitizenDisasterReport(
            id = "${SANDBOX_ID_PREFIX}DRILL-002",
            disasterType = s2.disasterType,
            incidentTime = formatTrainingTime(now - 35 * 60_000L),
            incidentLocation = s2.incidentLocation,
            district = s2.district,
            village = s2.village,
            latitude = s2.latitude,
            longitude = s2.longitude,
            gpsAccuracyMeters = 8.0f,
            isGpsVerified = true,
            casualtiesDead = s2.casualtiesDead,
            casualtiesInjured = s2.casualtiesInjured,
            casualtiesMissing = s2.casualtiesMissing,
            photoUri = photoUri,
            reporterName = s2.reporterName,
            reporterPhone = s2.reporterPhone,
            additionalNotes = s2.additionalNotes + "\n\n[Tindak Lanjut Petugas]: Tim TRC Regu B sedang menuju KM 42 membawa chainsaw.",
            timestamp = now - 35 * 60_000L,
            status = CitizenReportStatusHelper.STATUS_DIVALIDASI,
            updatedAt = now - 20 * 60_000L,
            handledByOfficerName = "PETUGAS SIMULASI TRC",
            handledByOfficerNip = "SIMULASI-TRC-01",
            handledByOfficerRole = "Tim Reaksi Cepat (TRC)",
            isSyncedToCloud = true
        )

        nextScenarioCursor = 2 % predefinedScenarios.size
        _sandboxReports.value = listOf(initial1, initial2)
    }

    /**
     * Menyuntikkan laporan bencana simulasi baru seolah-olah warga baru saja mengirim laporan darurat.
     * Sekaligus memicu banner peringatan real-time di Ruang Kerja Petugas tanpa menyentuh server produksi.
     */
    @Synchronized
    fun injectSimulatedCitizenReport(
        context: Context? = null,
        scenarioIndex: Int? = null,
        triggerRealtimeAlertBanner: Boolean = true
    ): CitizenDisasterReport {
        if (!_isSandboxEnabled.value) {
            _isSandboxEnabled.value = true
        }
        val chosenIndex = scenarioIndex ?: (nextScenarioCursor % predefinedScenarios.size)
        nextScenarioCursor = (chosenIndex + 1) % predefinedScenarios.size
        val scenario = predefinedScenarios[chosenIndex.coerceIn(0, predefinedScenarios.lastIndex)]

        val now = System.currentTimeMillis()
        val seq = sequenceCounter.incrementAndGet()
        val reportId = "${SANDBOX_ID_PREFIX}${scenario.code}-$seq"
        val simulatedReport = CitizenDisasterReport(
            id = reportId,
            disasterType = scenario.disasterType,
            incidentTime = formatTrainingTime(now),
            incidentLocation = scenario.incidentLocation,
            district = scenario.district,
            village = scenario.village,
            latitude = scenario.latitude,
            longitude = scenario.longitude,
            gpsAccuracyMeters = 5.4f,
            isGpsVerified = true,
            casualtiesDead = scenario.casualtiesDead,
            casualtiesInjured = scenario.casualtiesInjured,
            casualtiesMissing = scenario.casualtiesMissing,
            photoUri = defaultBundledTrainingPhotoUri(context),
            reporterName = scenario.reporterName,
            reporterPhone = scenario.reporterPhone,
            additionalNotes = scenario.additionalNotes,
            timestamp = now,
            status = CitizenReportStatusHelper.STATUS_DIPROSES,
            updatedAt = now,
            handledByOfficerName = "",
            handledByOfficerNip = "",
            handledByOfficerRole = "",
            isSyncedToCloud = true
        )

        _sandboxReports.value = listOf(simulatedReport) + _sandboxReports.value.filterNot { it.id == reportId }
        markTrainingStepCompleted(STEP_1_INJECT_OR_REPORT)
        _lastActionSummary.value = "Simulasi masuk: ${scenario.disasterType} (${scenario.village}, ${scenario.district}) [$reportId]"

        if (triggerRealtimeAlertBanner) {
            OfficerRealtimeNewReportDetector.sharedSessionDetector.recordNewCitizenReport(
                report = simulatedReport,
                context = context
            )
        }
        return simulatedReport
    }

    /**
     * Menangkap laporan yang dikirim dari form "Mode Warga" saat Mode Latihan aktif
     * agar hanya tersimpan di Sandbox lokal dan tidak masuk ke Firestore/Supabase/Room produksi.
     */
    @Synchronized
    fun submitSandboxCitizenReport(
        context: Context? = null,
        report: CitizenDisasterReport,
        triggerRealtimeAlertBanner: Boolean = true
    ): CitizenDisasterReport {
        val now = System.currentTimeMillis()
        val rawId = report.id.trim()
        val sandboxId = if (rawId.startsWith(SANDBOX_ID_PREFIX, ignoreCase = true)) {
            rawId
        } else {
            "$SANDBOX_ID_PREFIX${rawId.ifBlank { "WARGA-${sequenceCounter.incrementAndGet()}" }}"
        }

        val resolvedPhotoUri = report.photoUri?.takeIf { it.isNotBlank() }
            ?: defaultBundledTrainingPhotoUri(context)

        val prefixedLocation = if (report.incidentLocation.contains("[SIMULASI", ignoreCase = true)) {
            report.incidentLocation
        } else {
            "[SIMULASI LATIHAN] ${report.incidentLocation}"
        }

        val sandboxCopy = report.copy(
            id = sandboxId,
            incidentLocation = prefixedLocation,
            photoUri = resolvedPhotoUri,
            status = CitizenReportStatusHelper.normalizeStatus(report.status),
            timestamp = if (report.timestamp > 0L) report.timestamp else now,
            updatedAt = now,
            isSyncedToCloud = true
        )

        _sandboxReports.value = listOf(sandboxCopy) + _sandboxReports.value.filterNot { it.id == sandboxId }
        markTrainingStepCompleted(STEP_1_INJECT_OR_REPORT)
        _lastActionSummary.value = "Laporan latihan Mode Warga diterima di Sandbox: ${sandboxCopy.disasterType} ($sandboxId)"

        if (triggerRealtimeAlertBanner) {
            OfficerRealtimeNewReportDetector.sharedSessionDetector.recordNewCitizenReport(
                report = sandboxCopy,
                context = context
            )
        }
        return sandboxCopy
    }

    /**
     * Memperbarui status dan catatan tindak lanjut laporan di dalam Sandbox.
     */
    @Synchronized
    fun updateSandboxReportStatus(
        reportId: String,
        newStatus: String,
        notes: String? = null,
        officerName: String = "",
        officerNip: String = "",
        officerRole: String = ""
    ): CitizenDisasterReport? {
        val canonicalStatus = CitizenReportStatusHelper.normalizeStatus(newStatus)
        val now = System.currentTimeMillis()
        var updatedItem: CitizenDisasterReport? = null

        _sandboxReports.value = _sandboxReports.value.map { existing ->
            if (existing.id == reportId) {
                val mergedNotes = if (!notes.isNullOrBlank()) {
                    notes.trim()
                } else {
                    existing.additionalNotes
                }
                val modified = existing.copy(
                    status = canonicalStatus,
                    additionalNotes = mergedNotes,
                    updatedAt = now,
                    handledByOfficerName = officerName.ifBlank { existing.handledByOfficerName.ifBlank { "Petugas Latihan" } },
                    handledByOfficerNip = officerNip.ifBlank { existing.handledByOfficerNip },
                    handledByOfficerRole = officerRole.ifBlank { existing.handledByOfficerRole.ifBlank { "Operator Latihan" } },
                    isSyncedToCloud = true
                )
                updatedItem = modified
                modified
            } else {
                existing
            }
        }

        if (updatedItem != null) {
            markTrainingStepCompleted(STEP_2_VALIDATE_STATUS)
            _lastActionSummary.value = "Status laporan $reportId diperbarui ke '$canonicalStatus' (Terisolasi di Sandbox)."
        }
        return updatedItem
    }

    /**
     * Menghapus satu laporan simulasi dari daftar Sandbox.
     */
    @Synchronized
    fun deleteSandboxReport(reportId: String) {
        _sandboxReports.value = _sandboxReports.value.filterNot { it.id == reportId }
        _lastActionSummary.value = "Laporan simulasi $reportId dihapus dari Sandbox."
    }

    /**
     * Menyimpan Laporan Harian Pusdalops hasil latihan ke memori Sandbox
     * (tidak menyentuh tabel `laporan_harian` Room produksi).
     */
    @Synchronized
    fun recordSandboxDailyReportSaved(laporan: LaporanHarianEntity): Int {
        val assignedId = if (laporan.id > 0) laporan.id else dailyReportIdCounter.incrementAndGet()
        laporan.id = assignedId
        if (!laporan.lokasi.contains("[SIMULASI", ignoreCase = true)) {
            laporan.lokasi = "[SIMULASI LATIHAN] ${laporan.lokasi}"
        }
        _sandboxDailyReports.value = listOf(laporan) + _sandboxDailyReports.value.filterNot { it.id == assignedId }
        markTrainingStepCompleted(STEP_3_AUTOFILL_DAILY_FORM)
        _lastActionSummary.value = "Laporan Harian Latihan (#$assignedId - ${laporan.jenisBencana}) tersimpan di Sandbox."
        return assignedId
    }

    @Synchronized
    fun markTrainingStepCompleted(stepKey: String) {
        _completedSteps.value = _completedSteps.value + stepKey
    }

    /**
     * Mereset seluruh data latihan kembali ke kondisi awal yang bersih.
     */
    @Synchronized
    fun resetSandboxData(context: Context? = null, reseedDefaults: Boolean = true) {
        _sandboxDailyReports.value = emptyList()
        _completedSteps.value = emptySet()
        if (reseedDefaults && _isSandboxEnabled.value) {
            seedInitialTrainingReports(context)
            _lastActionSummary.value = "Skenario Latihan direset ke kondisi awal (2 laporan simulasi siap dilatih)."
        } else {
            _sandboxReports.value = emptyList()
            _lastActionSummary.value = "Seluruh data latihan Sandbox telah dibersihkan."
        }
    }
}
