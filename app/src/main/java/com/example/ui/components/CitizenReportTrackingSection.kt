package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.FirestoreHelper
import com.example.model.CitizenDisasterReport
import com.example.model.CitizenReportStatusHelper
import kotlinx.coroutines.launch

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
 * Memetakan [CitizenDisasterReport] ke dalam tahapan pelacakan standar aplikasi darurat:
 * - Stage 0 (Pending): Laporan masih menunggu sinkronisasi internet (WorkManager) atau baru masuk antrean awal tanpa petugas.
 * - Stage 1 (Proses): Laporan sudah masuk server (`Diproses` atau `Divalidasi`) dan sedang ditindaklanjuti Petugas Pusdalops / TRC.
 * - Stage 2 (Selesai): Penanganan laporan bencana telah diselesaikan oleh BPBD (`Selesai`).
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
 * UI Pelacakan Status Laporan Warga (Tracking ID Laporan) — Standar Aplikasi Darurat Profesional.
 * Warga dapat memasukkan ID Laporan (atau memilih dari riwayat terbaru di perangkat) untuk
 * memeriksa status penanganan real-time: Pending -> Proses -> Selesai.
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
                        text = "PELACAKAN STATUS LAPORAN (TRACKING ID)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF14532D)
                    )
                    Text(
                        text = "Masukkan ID Laporan atau pilih riwayat untuk cek status (Pending / Proses / Selesai)",
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
                            searchFeedbackMessage = "Laporan ditemukan (${localHit.id})."
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
                        val isSelected = activeTrackedReport?.id == item.id
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
                    color = if (activeTrackedReport != null) Color(0xFF15803D) else Color(0xFFB45309),
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Kartu Hasil Pelacakan 3 Tahap (Pending -> Proses -> Selesai)
            if (activeTrackedReport != null) {
                val summary = remember(activeTrackedReport) {
                    mapReportToTrackingProgress(activeTrackedReport)
                }
                val badgeBgColor = when (summary.stageIndex) {
                    2 -> Color(0xFFDBEAFE)
                    1 -> Color(0xFFDCFCE7)
                    else -> Color(0xFFFEF3C7)
                }
                val badgeTextColor = when (summary.stageIndex) {
                    2 -> Color(0xFF1D4ED8)
                    1 -> Color(0xFF15803D)
                    else -> Color(0xFFB45309)
                }

                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tracked_report_result_box")
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "ID: ${activeTrackedReport.id}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "${activeTrackedReport.disasterType} • Desa ${activeTrackedReport.village}, Kec. ${activeTrackedReport.district}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF475569),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = badgeBgColor,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = summary.stageBadgeLabel,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = badgeTextColor,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Visual 3-Step Timeline: 1. PENDING -> 2. PROSES -> 3. SELESAI
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TrackingStepNode(
                                stepNumber = 1,
                                title = "Pending",
                                isCompleted = summary.stageIndex >= 0,
                                isActive = summary.stageIndex == 0,
                                activeColor = Color(0xFFD97706),
                                modifier = Modifier.weight(1f)
                            )
                            HorizontalDivider(
                                modifier = Modifier.width(24.dp),
                                thickness = 2.dp,
                                color = if (summary.stageIndex >= 1) Color(0xFF16A34A) else Color(0xFFCBD5E1)
                            )
                            TrackingStepNode(
                                stepNumber = 2,
                                title = "Proses",
                                isCompleted = summary.stageIndex >= 1,
                                isActive = summary.stageIndex == 1,
                                activeColor = Color(0xFF16A34A),
                                modifier = Modifier.weight(1f)
                            )
                            HorizontalDivider(
                                modifier = Modifier.width(24.dp),
                                thickness = 2.dp,
                                color = if (summary.stageIndex >= 2) Color(0xFF2563EB) else Color(0xFFCBD5E1)
                            )
                            TrackingStepNode(
                                stepNumber = 3,
                                title = "Selesai",
                                isCompleted = summary.stageIndex >= 2,
                                isActive = summary.stageIndex == 2,
                                activeColor = Color(0xFF2563EB),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = summary.statusHeadline,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeTextColor
                        )
                        Text(
                            text = summary.statusDescription,
                            fontSize = 11.sp,
                            color = Color(0xFF334155),
                            lineHeight = 15.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = Color(0xFFE2E8F0))
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Petugas Penangan: ${summary.handledByOfficerDisplay}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1E293B)
                                )
                                Text(
                                    text = "Waktu Laporan: ${activeTrackedReport.incidentTime} • Sync: ${if (summary.isSyncedToServer) "Synced (Server)" else "Antre WorkManager"}",
                                    fontSize = 10.sp,
                                    color = Color(0xFF64748B)
                                )
                            }

                            if (!activeTrackedReport.photoUri.isNullOrBlank() && onViewPhoto != null) {
                                OutlinedButton(
                                    onClick = {
                                        onViewPhoto(activeTrackedReport.photoUri, activeTrackedReport)
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

                        if (activeTrackedReport.additionalNotes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                color = Color.White,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Catatan / Tindak Lanjut: ${activeTrackedReport.additionalNotes}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF334155),
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
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

@Composable
private fun TrackingStepNode(
    stepNumber: Int,
    title: String,
    isCompleted: Boolean,
    isActive: Boolean,
    activeColor: Color,
    modifier: Modifier = Modifier
) {
    val circleColor = when {
        isActive -> activeColor
        isCompleted -> Color(0xFF16A34A)
        else -> Color(0xFFE2E8F0)
    }
    val textColor = when {
        isActive -> activeColor
        isCompleted -> Color(0xFF0F172A)
        else -> Color(0xFF94A3B8)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(circleColor),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted && !isActive && stepNumber < 3) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(15.dp)
                )
            } else {
                Text(
                    text = stepNumber.toString(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isCompleted || isActive) Color.White else Color(0xFF64748B)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = textColor,
            textAlign = TextAlign.Center
        )
    }
}
