package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.FirestoreHelper
import com.example.model.CitizenDisasterReport
import com.example.model.CitizenReportStatusHelper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Representasi terstruktur status pelacakan laporan warga (Pending / Proses / Selesai).
 */
data class CitizenReportTrackingSummary(
    val reportId: String,
    val stageIndex: Int, // 0 = Pending, 1 = Proses (Diproses/Divalidasi), 2 = Selesai
    val stageBadgeLabel: String, // "PENDING", "PROSES", "SELESAI"
    val statusHeadline: String,
    val statusDescription: String,
    val isSyncedToServer: Boolean,
    val handledByOfficerDisplay: String
)

/**
 * Status visual setiap tahapan pada UI Stepper Vertikal 4 Tahapan (Feedback Loop Pelacakan Status).
 */
enum class StepperVisualState {
    COMPLETED, // Tahap sudah dilewati (Hijau)
    ACTIVE,    // Tahap yang sedang berlangsung/aktif (Oranye / Hijau)
    UNREACHED  // Tahap yang belum tercapai (Abu-abu)
}

/**
 * Model 1 tahapan pada UI Stepper Vertikal 4 Tahapan:
 * - [1] Laporan Diterima (Status: Pending)
 * - [2] Tim Menuju Lokasi (Status: Diproses)
 * - [3] Penanganan (Status: Divalidasi)
 * - [4] Selesai (Status: Selesai)
 */
data class CitizenVerticalStepperStep(
    val stepNumber: Int, // 1..4
    val stepTitle: String, // "[1] Laporan Diterima", "[2] Tim Menuju Lokasi", "[3] Penanganan", "[4] Selesai"
    val statusKey: String, // "Pending", "Diproses", "Divalidasi", "Selesai"
    val statusSubtitle: String, // "Status: Pending", "Status: Diproses", "Status: Divalidasi", "Status: Selesai"
    val description: String,
    val visualState: StepperVisualState,
    val iconColorHex: Long,
    val textColorHex: Long,
    val handlingTimeDisplay: String?, // Jam berapa status diubah oleh petugas (dari timestamp Firestore)
    val responseSpeedDisplay: String? // Kecepatan respons pemerintah
)

const val STEPPER_COLOR_GRAY_UNREACHED_HEX: Long = 0xFF94A3B8
const val STEPPER_COLOR_ORANGE_ACTIVE_HEX: Long = 0xFFEA580C
const val STEPPER_COLOR_GREEN_ACTIVE_OR_COMPLETED_HEX: Long = 0xFF16A34A
const val STEPPER_TEXT_GREEN_HEX: Long = 0xFF15803D

/**
 * Memformat epoch millisecond dari Firestore (`timestamp` / `updatedAt`) menjadi jam & tanggal WIB
 * agar warga mengetahui jam berapa status laporan diubah oleh petugas.
 */
fun formatFirestoreHandlingTime(timestampMillis: Long): String {
    if (timestampMillis <= 0L) return "-"
    return try {
        val sdf = SimpleDateFormat("HH:mm 'WIB' • dd MMM yyyy", Locale.forLanguageTag("id-ID"))
        sdf.format(Date(timestampMillis))
    } catch (_: Exception) {
        val fallback = SimpleDateFormat("HH:mm 'WIB' • dd/MM/yyyy", Locale.US)
        fallback.format(Date(timestampMillis))
    }
}

/**
 * Menghitung kecepatan respons pemerintah berdasarkan selisih waktu laporan diterima (`createdAtMillis`)
 * dan waktu perubahan status oleh petugas (`updatedAtMillis`) di Firestore.
 */
fun calculateGovernmentResponseSpeed(
    createdAtMillis: Long,
    updatedAtMillis: Long
): String {
    val diffMs = (updatedAtMillis - createdAtMillis).coerceAtLeast(0L)
    val diffSec = diffMs / 1000L
    val diffMin = diffSec / 60L
    val diffHours = diffMin / 60L
    return when {
        diffSec < 60L -> "Kecepatan Respons: < 1 menit (Respons Instan)"
        diffMin < 60L -> "Kecepatan Respons: $diffMin menit sejak laporan diterima"
        else -> {
            val remMin = diffMin % 60L
            if (remMin > 0L) {
                "Kecepatan Respons: $diffHours jam $remMin menit"
            } else {
                "Kecepatan Respons: $diffHours jam"
            }
        }
    }
}

/**
 * Menentukan indeks tahap aktif (0..3) pada UI Stepper Vertikal 4 Tahapan berdasarkan status dokumen laporan:
 * - 0 -> [1] Laporan Diterima (Status: Pending)
 * - 1 -> [2] Tim Menuju Lokasi (Status: Diproses)
 * - 2 -> [3] Penanganan (Status: Divalidasi)
 * - 3 -> [4] Selesai (Status: Selesai)
 */
fun resolveFourStageVerticalStepperIndex(report: CitizenDisasterReport): Int {
    val rawStatus = report.status.trim()
    return when {
        rawStatus.equals("Selesai", ignoreCase = true) ||
            CitizenReportStatusHelper.isResolved(rawStatus) -> 3
        rawStatus.equals("Divalidasi", ignoreCase = true) ||
            CitizenReportStatusHelper.isValidated(rawStatus) -> 2
        rawStatus.equals("Pending", ignoreCase = true) ||
            rawStatus.equals("Menunggu", ignoreCase = true) ||
            rawStatus.equals("Baru", ignoreCase = true) ||
            !isReportSyncedWithServer(report) -> 0
        else -> 1 // "Diproses" -> [2] Tim Menuju Lokasi (Status: Diproses)
    }
}

/**
 * Membangun 4 tahapan UI Stepper Vertikal lengkap dengan sinkronisasi warna otomatis
 * (Abu-abu untuk tahap belum tercapai, Oranye/Hijau untuk tahap aktif/tercapai)
 * dan informasi Waktu Penanganan dari timestamp Firestore.
 */
fun buildCitizenReportVerticalStepperSteps(report: CitizenDisasterReport): List<CitizenVerticalStepperStep> {
    val activeIdx = resolveFourStageVerticalStepperIndex(report)
    val createdTimeStr = formatFirestoreHandlingTime(report.timestamp)
    val updatedTimeStr = formatFirestoreHandlingTime(report.effectiveUpdatedAt())
    val speedStr = calculateGovernmentResponseSpeed(report.timestamp, report.effectiveUpdatedAt())
    val officerNameDisplay = report.handledByOfficerName.trim().ifBlank { "Petugas Pusdalops BPBD" }

    val stageDefinitions = listOf(
        Triple(
            "[1] Laporan Diterima",
            "Pending",
            "Laporan warga beserta titik koordinat GPS dan foto bukti telah diterima oleh sistem Pusdalops BPBD."
        ),
        Triple(
            "[2] Tim Menuju Lokasi",
            "Diproses",
            "Petugas Pusdalops ($officerNameDisplay) memproses laporan dan mengerahkan Tim Reaksi Cepat (TRC) menuju titik lokasi."
        ),
        Triple(
            "[3] Penanganan",
            "Divalidasi",
            "Laporan telah divalidasi di lapangan oleh petugas ($officerNameDisplay) dan sedang dalam tindakan penanganan darurat."
        ),
        Triple(
            "[4] Selesai",
            "Selesai",
            "Penanganan dampak kejadian bencana di lokasi telah tuntas dilaksanakan oleh Tim BPBD Kabupaten Mukomuko."
        )
    )

    return stageDefinitions.mapIndexed { idx, (title, statusKey, desc) ->
        val state = when {
            idx < activeIdx -> StepperVisualState.COMPLETED
            idx == activeIdx -> StepperVisualState.ACTIVE
            else -> StepperVisualState.UNREACHED
        }

        // Sinkronisasi warna otomatis:
        // - Abu-abu (0xFF94A3B8) untuk tahap belum tercapai (UNREACHED)
        // - Oranye (0xFFEA580C) untuk tahap sedang aktif pada proses berjalan (idx 0..2), Hijau (0xFF16A34A) saat aktif di tahap Selesai (idx 3)
        // - Hijau (0xFF16A34A) untuk tahap yang sudah terlewati/tuntas (COMPLETED)
        val (iconHex, textHex) = when (state) {
            StepperVisualState.UNREACHED -> Pair(
                STEPPER_COLOR_GRAY_UNREACHED_HEX,
                STEPPER_COLOR_GRAY_UNREACHED_HEX
            )
            StepperVisualState.ACTIVE -> {
                if (idx == 3) {
                    Pair(STEPPER_COLOR_GREEN_ACTIVE_OR_COMPLETED_HEX, STEPPER_TEXT_GREEN_HEX)
                } else {
                    Pair(STEPPER_COLOR_ORANGE_ACTIVE_HEX, STEPPER_COLOR_ORANGE_ACTIVE_HEX)
                }
            }
            StepperVisualState.COMPLETED -> Pair(
                STEPPER_COLOR_GREEN_ACTIVE_OR_COMPLETED_HEX,
                STEPPER_TEXT_GREEN_HEX
            )
        }

        val timeDisplay: String? = when {
            state == StepperVisualState.UNREACHED -> null
            idx == 0 -> "Waktu Masuk: $createdTimeStr"
            idx == activeIdx -> "Diubah Petugas: $updatedTimeStr"
            else -> "Tercatat: $updatedTimeStr"
        }

        val responseDisplay: String? = when {
            state == StepperVisualState.UNREACHED -> null
            idx == 0 -> if (isReportSyncedWithServer(report)) "Tersinkron ke Server Pusdalops" else "Tersimpan di perangkat (Auto-Sync)"
            idx == activeIdx -> speedStr
            else -> null
        }

        CitizenVerticalStepperStep(
            stepNumber = idx + 1,
            stepTitle = title,
            statusKey = statusKey,
            statusSubtitle = "Status: $statusKey",
            description = desc,
            visualState = state,
            iconColorHex = iconHex,
            textColorHex = textHex,
            handlingTimeDisplay = timeDisplay,
            responseSpeedDisplay = responseDisplay
        )
    }
}

/**
 * Mencari laporan warga berdasarkan ID Laporan (baik ID lengkap `LAP-MMK-...` maupun potongan kode akhir ID).
 */
fun resolveTrackedCitizenReport(
    query: String,
    reports: List<CitizenDisasterReport>
): CitizenDisasterReport? {
    val cleanQuery = query.trim().removePrefix("#").trim()
    if (cleanQuery.isBlank()) return null

    // 1. Pencocokan ID persis (case-insensitive)
    reports.firstOrNull { it.id.equals(cleanQuery, ignoreCase = true) }?.let { return it }

    // 2. Pencocokan akhiran kode ID (mis. 3-7 karakter terakhir setelah tanda '-')
    reports.firstOrNull { it.id.endsWith(cleanQuery, ignoreCase = true) }?.let { return it }

    // 3. Pencocokan substring ID
    reports.firstOrNull { it.id.contains(cleanQuery, ignoreCase = true) }?.let { return it }

    return null
}

/**
 * Memetakan [CitizenDisasterReport] ke dalam tahapan pelacakan ringkas (Pending / Proses / Selesai).
 */
fun mapReportToTrackingProgress(report: CitizenDisasterReport): CitizenReportTrackingSummary {
    val isSynced = isReportSyncedWithServer(report)
    val normStatus = CitizenReportStatusHelper.normalizeStatus(report.status)
    val hasOfficerAssigned = report.handledByOfficerName.isNotBlank()

    val (stageIdx, badgeLabel, headline, desc) = when {
        !isSynced -> listOf(
            0,
            "PENDING",
            "Pending • Menunggu Koneksi Internet (Auto-Sync WorkManager)",
            "Laporan & foto bukti GPS tersimpan aman di perangkat dan dijadwalkan terkirim otomatis ke server Pusdalops saat sinyal tersedia."
        )
        normStatus == CitizenReportStatusHelper.STATUS_SELESAI -> listOf(
            2,
            "SELESAI",
            "Selesai • Penanganan Lapangan Tuntas",
            "Laporan bencana telah selesai diverifikasi dan ditangani oleh Tim Pusdalops & TRC BPBD Kabupaten Mukomuko."
        )
        normStatus == CitizenReportStatusHelper.STATUS_DIVALIDASI -> listOf(
            1,
            "PROSES",
            "Proses (Divalidasi) • Tim TRC / Pusdalops Sedang Menangani",
            "Laporan telah tervalidasi resmi oleh Operator Pusdalops dan sedang dalam penanganan lapangan."
        )
        hasOfficerAssigned -> listOf(
            1,
            "PROSES",
            "Proses • Sedang Ditindaklanjuti Petugas Jaga",
            "Laporan telah diterima di server Pusdalops dan sedang diperiksa oleh petugas piket aktif."
        )
        else -> listOf(
            0,
            "PENDING",
            "Pending / Diproses • Menunggu Verifikasi Operator Pusdalops",
            "Laporan dan bukti foto GPS telah masuk ke database pusat Pusdalops BPBD dan menunggu giliran verifikasi petugas."
        )
    }

    val officerDisplay = if (report.handledByOfficerName.isNotBlank()) {
        buildString {
            append(report.handledByOfficerName)
            if (report.handledByOfficerRole.isNotBlank()) {
                append(" (${report.handledByOfficerRole})")
            }
        }
    } else {
        "Antrean Operator Pusdalops BPBD Mukomuko"
    }

    return CitizenReportTrackingSummary(
        reportId = report.id,
        stageIndex = stageIdx as Int,
        stageBadgeLabel = badgeLabel as String,
        statusHeadline = headline as String,
        statusDescription = desc as String,
        isSyncedToServer = isSynced,
        handledByOfficerDisplay = officerDisplay
    )
}

/**
 * Komponen StreamBuilder Pelacakan + UI Stepper Vertikal 4 Tahapan (Feedback Loop).
 * Mendengarkan dokumen spesifik `citizen_disaster_reports/{report.id}` (`LAP-MMK-...`) secara real-time
 * dan otomatis memperbarui warna tahapan serta jam perubahan status oleh petugas.
 */
@Composable
fun CitizenReportVerticalFeedbackStepper(
    report: CitizenDisasterReport,
    modifier: Modifier = Modifier
) {
    // State yang disinkronkan secara real-time dari dokumen spesifik Firestore (StreamBuilder Pelacakan)
    var liveReport by remember(report.id) { mutableStateOf(report) }

    // Update apabila prop parent berubah dengan timestamp lebih baru
    LaunchedEffect(report) {
        if (report.effectiveUpdatedAt() >= liveReport.effectiveUpdatedAt() ||
            report.status != liveReport.status
        ) {
            liveReport = report
        }
    }

    // StreamBuilder Pelacakan: mendengarkan dokumen spesifik berdasarkan ID Laporan (LAP-MMK-...)
    DisposableEffect(report.id) {
        val registration = FirestoreHelper.listenCitizenReportByIdRealtime(
            reportId = report.id,
            onUpdate = { remoteDoc ->
                if (remoteDoc != null) {
                    liveReport = remoteDoc
                }
            }
        )
        onDispose {
            registration?.remove()
        }
    }

    val steps = remember(liveReport) {
        buildCitizenReportVerticalStepperSteps(liveReport)
    }
    val updatedTimeFormatted = remember(liveReport) {
        formatFirestoreHandlingTime(liveReport.effectiveUpdatedAt())
    }
    val responseSpeedText = remember(liveReport) {
        calculateGovernmentResponseSpeed(liveReport.timestamp, liveReport.effectiveUpdatedAt())
    }

    Surface(
        color = Color(0xFFF8FAFC),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("vertical_feedback_stepper_${liveReport.id}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Feedback Loop Real-Time & Waktu Penanganan
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF16A34A))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "PROGRES PENANGANAN REAL-TIME",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A)
                    )
                }

                Surface(
                    color = Color(0xFFE0F2FE),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "LIVE STREAM",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF0369A1),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Banner Waktu Penanganan & Kecepatan Respons Pemerintah (dari timestamp Firestore)
            Surface(
                color = Color(0xFFFFFBEB),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("stepper_handling_timestamp_banner")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Waktu Penanganan Petugas",
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Waktu Update Status: $updatedTimeFormatted",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF92400E)
                        )
                        Text(
                            text = responseSpeedText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFB45309)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // UI Stepper Vertikal 4 Tahapan:
            // [1] Laporan Diterima (Status: Pending)
            // [2] Tim Menuju Lokasi (Status: Diproses)
            // [3] Penanganan (Status: Divalidasi)
            // [4] Selesai (Status: Selesai)
            steps.forEachIndexed { index, step ->
                val isLastStep = index == steps.lastIndex
                val stepColor = Color(step.iconColorHex)
                val titleTextColor = Color(step.textColorHex)
                val isUnreached = step.visualState == StepperVisualState.UNREACHED
                val isActive = step.visualState == StepperVisualState.ACTIVE
                val isCompleted = step.visualState == StepperVisualState.COMPLETED

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("vertical_step_row_${step.stepNumber}"),
                    verticalAlignment = Alignment.Top
                ) {
                    // Kolom Kiri: Lingkaran Ikon Tahap + Garis Penghubung Vertikal
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(32.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isUnreached) Color(0xFFE2E8F0) else stepColor
                                )
                                .border(
                                    width = if (isActive) 2.dp else 1.dp,
                                    color = if (isActive) Color(0xFFFED7AA) else stepColor,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                isCompleted -> Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Tahap Selesai",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                isActive -> Icon(
                                    imageVector = if (step.stepNumber == 4) {
                                        Icons.Default.CheckCircle
                                    } else {
                                        Icons.Default.Sync
                                    },
                                    contentDescription = "Tahap Aktif",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                else -> Text(
                                    text = step.stepNumber.toString(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        if (!isLastStep) {
                            val connectorColor = if (isCompleted) {
                                Color(STEPPER_COLOR_GREEN_ACTIVE_OR_COMPLETED_HEX)
                            } else {
                                Color(0xFFCBD5E1)
                            }
                            Box(
                                modifier = Modifier
                                    .width(2.5.dp)
                                    .height(38.dp)
                                    .background(connectorColor)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Kolom Kanan: Judul Tahap, Badge Status, Deskripsi, & Jam Perubahan Status
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(bottom = if (isLastStep) 2.dp else 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${step.stepTitle} (${step.statusSubtitle})",
                                fontSize = 12.sp,
                                fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Bold,
                                color = titleTextColor,
                                modifier = Modifier.weight(1f)
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = when (step.visualState) {
                                    StepperVisualState.ACTIVE -> stepColor.copy(alpha = 0.14f)
                                    StepperVisualState.COMPLETED -> Color(0xFFDCFCE7)
                                    StepperVisualState.UNREACHED -> Color(0xFFF1F5F9)
                                },
                                border = BorderStroke(
                                    0.8.dp,
                                    when (step.visualState) {
                                        StepperVisualState.ACTIVE -> stepColor
                                        StepperVisualState.COMPLETED -> Color(0xFF86EFAC)
                                        StepperVisualState.UNREACHED -> Color(0xFFCBD5E1)
                                    }
                                )
                            ) {
                                Text(
                                    text = when (step.visualState) {
                                        StepperVisualState.ACTIVE -> if (step.stepNumber == 4) "SELESAI" else "AKTIF"
                                        StepperVisualState.COMPLETED -> "TERCAPAI"
                                        StepperVisualState.UNREACHED -> "MENUNGGU"
                                    },
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = when (step.visualState) {
                                        StepperVisualState.ACTIVE -> stepColor
                                        StepperVisualState.COMPLETED -> Color(0xFF15803D)
                                        StepperVisualState.UNREACHED -> Color(0xFF94A3B8)
                                    },
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = step.description,
                            fontSize = 11.sp,
                            color = if (isUnreached) Color(0xFF94A3B8) else Color(0xFF475569),
                            lineHeight = 15.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )

                        if (!step.handlingTimeDisplay.isNullOrBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = stepColor,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = step.handlingTimeDisplay,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = titleTextColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * UI Pelacakan Status Laporan Warga (Tracking ID Laporan) — Standar Aplikasi Darurat Profesional.
 * Warga dapat memasukkan ID Laporan (atau memilih dari riwayat terbaru di perangkat) untuk
 * memeriksa status penanganan real-time melalui UI Stepper Vertikal 4 Tahapan.
 */
@Composable
fun CitizenReportStatusTrackerCard(
    reports: List<CitizenDisasterReport>,
    initialTrackingId: String = "",
    onTrackingIdChange: ((String) -> Unit)? = null,
    onViewPhoto: ((String, CitizenDisasterReport) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var trackingQuery by remember(initialTrackingId) {
        mutableStateOf(initialTrackingId.ifBlank { reports.firstOrNull()?.id.orEmpty() })
    }
    var cloudLookedUpReport by remember { mutableStateOf<CitizenDisasterReport?>(null) }
    var isSearchingCloud by remember { mutableStateOf(false) }
    var searchFeedbackMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val matchedLocal = remember(trackingQuery, reports) {
        resolveTrackedCitizenReport(trackingQuery, reports)
    }
    val activeTrackedReport = matchedLocal ?: cloudLookedUpReport?.takeIf {
        resolveTrackedCitizenReport(trackingQuery, listOf(it)) != null
    }

    // StreamBuilder Pelacakan pada dokumen spesifik yang sedang dilacak (LAP-MMK-...)
    DisposableEffect(activeTrackedReport?.id) {
        val targetId = activeTrackedReport?.id
        val registration = if (!targetId.isNullOrBlank()) {
            FirestoreHelper.listenCitizenReportByIdRealtime(
                reportId = targetId,
                onUpdate = { liveReport ->
                    if (liveReport != null) {
                        cloudLookedUpReport = liveReport
                    }
                }
            )
        } else {
            null
        }
        onDispose {
            registration?.remove()
        }
    }

    // Gabungkan data terbaru antara stream real-time dokumen spesifik dan list lokal
    val effectiveReport = remember(activeTrackedReport, cloudLookedUpReport) {
        val local = activeTrackedReport
        val cloud = cloudLookedUpReport
        if (local != null && cloud != null && local.id.equals(cloud.id, ignoreCase = true)) {
            if (cloud.effectiveUpdatedAt() >= local.effectiveUpdatedAt() || cloud.status != local.status) {
                cloud
            } else {
                local
            }
        } else {
            local
        }
    }

    Surface(
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, Color(0xFF15803D).copy(alpha = 0.35f)),
        shadowElevation = 3.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("citizen_report_status_tracker_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFDCFCE7),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Pelacakan Status Laporan",
                            tint = Color(0xFF15803D),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "FEEDBACK LOOP & PELACAKAN STATUS REAL-TIME",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF14532D)
                    )
                    Text(
                        text = "Pantau progres penanganan laporan (LAP-MMK-...) melalui 4 tahapan vertikal secara langsung",
                        fontSize = 11.sp,
                        color = Color(0xFF4B5563)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Input ID Laporan & Tombol Lacak
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = trackingQuery,
                    onValueChange = {
                        trackingQuery = it
                        searchFeedbackMessage = null
                        onTrackingIdChange?.invoke(it)
                    },
                    placeholder = {
                        Text(
                            text = "Contoh: LAP-MMK-2026...",
                            fontSize = 12.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Color(0xFF15803D),
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (trackingQuery.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    trackingQuery = ""
                                    cloudLookedUpReport = null
                                    searchFeedbackMessage = null
                                    onTrackingIdChange?.invoke("")
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Bersihkan ID",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_track_report_id")
                )

                Button(
                    onClick = {
                        val q = trackingQuery.trim()
                        if (q.isBlank()) {
                            searchFeedbackMessage = "Masukkan ID Laporan terlebih dahulu."
                            return@Button
                        }
                        val localHit = resolveTrackedCitizenReport(q, reports)
                        if (localHit != null) {
                            searchFeedbackMessage = "Memantau dokumen ${localHit.id} secara real-time."
                        } else {
                            isSearchingCloud = true
                            searchFeedbackMessage = "Mencari ID $q di server Pusdalops..."
                            coroutineScope.launch {
                                val res = FirestoreHelper.fetchCitizenReportByIdFromServerSuspend(q)
                                isSearchingCloud = false
                                val found = res.getOrNull()
                                if (found != null) {
                                    cloudLookedUpReport = found
                                    searchFeedbackMessage = "Laporan ditemukan langsung dari Cloud Server Pusdalops."
                                } else {
                                    searchFeedbackMessage = "ID Laporan '$q' belum ditemukan. Pastikan kode ID benar."
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    modifier = Modifier
                        .heightIn(min = 50.dp)
                        .testTag("btn_track_report_id")
                ) {
                    if (isSearchingCloud) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "LACAK",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }
            }

            // Quick-select chips dari riwayat laporan terbaru
            if (reports.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Pilih Cepat ID Laporan Terbaru:",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF6B7280)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    reports.take(6).forEach { item ->
                        val isSelected = effectiveReport?.id == item.id
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) Color(0xFF15803D) else Color(0xFFF3F4F6),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) Color(0xFF15803D) else Color(0xFFD1D5DB)
                            ),
                            modifier = Modifier
                                .clickable {
                                    trackingQuery = item.id
                                    searchFeedbackMessage = null
                                    onTrackingIdChange?.invoke(item.id)
                                }
                                .testTag("chip_track_report_${item.id}")
                        ) {
                            Text(
                                text = item.id,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else Color(0xFF374151),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            if (!searchFeedbackMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = searchFeedbackMessage.orEmpty(),
                    fontSize = 11.sp,
                    color = if (effectiveReport != null) Color(0xFF15803D) else Color(0xFFB45309),
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Kartu Hasil Pelacakan dengan UI Stepper Vertikal 4 Tahapan
            if (effectiveReport != null) {
                val summary = remember(effectiveReport) {
                    mapReportToTrackingProgress(effectiveReport)
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tracked_report_result_box")
                ) {
                    // Header ID Laporan & Lokasi
                    Surface(
                        color = Color(0xFFF0FDF4),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "ID Dokumen: ${effectiveReport.id}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF14532D)
                                )
                                Text(
                                    text = "${effectiveReport.disasterType} • Desa ${effectiveReport.village}, Kec. ${effectiveReport.district}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF166534),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (!effectiveReport.photoUri.isNullOrBlank() && onViewPhoto != null) {
                                OutlinedButton(
                                    onClick = {
                                        onViewPhoto(effectiveReport.photoUri, effectiveReport)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.heightIn(min = 34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ZoomIn,
                                        contentDescription = "Lihat Foto Bukti",
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Foto Bukti", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // UI Stepper Vertikal 4 Tahapan Real-Time
                    CitizenReportVerticalFeedbackStepper(
                        report = effectiveReport
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Petugas Penangan: ${summary.handledByOfficerDisplay}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E293B)
                    )
                }
            } else if (trackingQuery.isNotBlank()) {
                Surface(
                    color = Color(0xFFFFFBEB),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Tekan tombol 'LACAK' untuk memeriksa ID Laporan '$trackingQuery' di database pusat Pusdalops.",
                            fontSize = 11.sp,
                            color = Color(0xFF92400E)
                        )
                    }
                }
            }
        }
    }
}
