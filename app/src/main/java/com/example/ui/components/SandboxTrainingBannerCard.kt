package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.training.SandboxTrainingManager

/**
 * Panel Kendali Utama Sandbox Training Mode (Mode Latihan Petugas) di Ruang Kerja Pusdalops & TRC.
 * Memungkinkan operator mengaktifkan lingkungan simulasi 100% terisolasi, menyuntikkan skenario bencana
 * Kabupaten Mukomuko, memantau progres 4 langkah latihan SOP, dan mereset data latihan.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SandboxTrainingControlCard(
    onOpenDailyReportForm: (() -> Unit)? = null,
    onScenarioInjected: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isSandboxEnabled by SandboxTrainingManager.isSandboxEnabled.collectAsState()
    val sandboxReports by SandboxTrainingManager.sandboxReports.collectAsState()
    val sandboxDailyReports by SandboxTrainingManager.sandboxDailyReports.collectAsState()
    val completedSteps by SandboxTrainingManager.completedSteps.collectAsState()
    val lastActionSummary by SandboxTrainingManager.lastActionSummary.collectAsState()

    var showScenarioPicker by remember { mutableStateOf(false) }
    var selectedScenarioIndex by remember { mutableIntStateOf(0) }

    val cardBgColor = if (isSandboxEnabled) {
        Color(0xFFFFF8E1)
    } else {
        MaterialTheme.colorScheme.surface
    }
    val borderColor = if (isSandboxEnabled) {
        Color(0xFFF59E0B)
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("sandbox_training_control_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        border = BorderStroke(if (isSandboxEnabled) 2.dp else 1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSandboxEnabled) 4.dp else 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = if (isSandboxEnabled) {
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFFB45309), Color(0xFFD97706))
                            )
                        } else {
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFF1E3A8A), Color(0xFF2563EB))
                            )
                        }
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = "Ikon Mode Latihan Petugas",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "MODE LATIHAN PETUGAS (SANDBOX)",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp
                                )
                            }
                            Text(
                                text = if (isSandboxEnabled) {
                                    "AKTIF • Simulasi Terisolasi (Database Produksi 100% Aman)"
                                } else {
                                    "NONAKTIF • Aktifkan untuk latihan pelaporan, validasi & cetak PDF"
                                },
                                color = Color.White.copy(alpha = 0.92f),
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                    SipagarSliderSwitch(
                        checked = isSandboxEnabled,
                        onCheckedChange = { enabled ->
                            SandboxTrainingManager.setSandboxModeEnabled(context, enabled)
                        },
                        modifier = Modifier.testTag("sandbox_training_toggle_switch"),
                        checkedThumbColor = Color(0xFFFDE68A),
                        checkedTrackColor = Color(0xFFFDE68A).copy(alpha = 0.45f),
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color.White.copy(alpha = 0.35f)
                    )
                }
            }

            // Body Content
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = if (isSandboxEnabled) Color(0xFFFEF3C7) else Color(0xFFEFF6FF),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isSandboxEnabled) Color(0xFFF59E0B) else Color(0xFFBFDBFE)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Status Isolasi Database",
                                tint = if (isSandboxEnabled) Color(0xFFB45309) else Color(0xFF1D4ED8),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isSandboxEnabled) {
                                    "ISOLASI AKTIF: Zero Write Firestore & Room"
                                } else {
                                    "Lingkungan Orientasi Internal Pusdalops & TRC BPBD"
                                },
                                fontSize = 10.5.sp,
                                lineHeight = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSandboxEnabled) Color(0xFF92400E) else Color(0xFF1E40AF),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (isSandboxEnabled) {
                        Surface(
                            color = Color(0xFFFDE68A),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFD97706))
                        ) {
                            Text(
                                text = "${completedSteps.size}/4 Modul",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF92400E),
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                if (!isSandboxEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Gunakan Mode Latihan untuk membimbing petugas Pusdalops & TRC mempraktikkan alur penerimaan laporan warga, validasi status lapangan, tarik data ke Form Harian, hingga cetak PDF ber-watermark tanpa mencemari data bencana asli.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            SandboxTrainingManager.setSandboxModeEnabled(context, true)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_activate_sandbox_training"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD97706),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Mulai Mode Latihan",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Mulai Sesi Latihan Petugas (Sandbox Terisolasi)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(10.dp))

                    // Stats summary pills in sandbox
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SandboxMetricChip(
                            label = "Laporan Simulasi",
                            value = "${sandboxReports.size} Data",
                            modifier = Modifier.weight(1f)
                        )
                        SandboxMetricChip(
                            label = "Siap Divalidasi",
                            value = "${sandboxReports.count { it.status == "Diproses" }} Pending",
                            modifier = Modifier.weight(1f)
                        )
                        SandboxMetricChip(
                            label = "Rekap Harian",
                            value = "${sandboxDailyReports.size} Draft",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (!lastActionSummary.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = Color(0xFFECFDF5),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFF10B981)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "✓ $lastActionSummary",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF065F46),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 4-Step Guided Curriculum Checklist
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "PANDUAN 4 LANGKAH LATIHAN SOP PETUGAS:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF92400E)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            TrainingStepRow(
                                stepNumber = 1,
                                title = "Suntikkan / Kirim Simulasi Laporan Bencana Warga",
                                isDone = completedSteps.contains(SandboxTrainingManager.STEP_1_INJECT_OR_REPORT)
                            )
                            TrainingStepRow(
                                stepNumber = 2,
                                title = "Ubah Status ('Divalidasi' / 'Selesai') & Isi Catatan TRC di bawah",
                                isDone = completedSteps.contains(SandboxTrainingManager.STEP_2_VALIDATE_STATUS)
                            )
                            TrainingStepRow(
                                stepNumber = 3,
                                title = "Buka Form Laporan Harian -> Klik 'DARI WARGA' -> Simpan",
                                isDone = completedSteps.contains(SandboxTrainingManager.STEP_3_AUTOFILL_DAILY_FORM)
                            )
                            TrainingStepRow(
                                stepNumber = 4,
                                title = "Cetak / Ekspor PDF Laporan Harian (Watermark Simulasi)",
                                isDone = completedSteps.contains(SandboxTrainingManager.STEP_4_PRINT_PDF)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Scenario Selector Accordion
                    Surface(
                        color = Color(0xFFFFFBEB),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFFCD34D)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showScenarioPicker = !showScenarioPicker }
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val currentScenario = SandboxTrainingManager.predefinedScenarios[selectedScenarioIndex]
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Skenario Bencana Siap Suntik:",
                                        fontSize = 10.sp,
                                        color = Color(0xFF92400E),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = currentScenario.title,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF78350F)
                                    )
                                }
                                Icon(
                                    imageVector = if (showScenarioPicker) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = "Pilih Skenario Latihan",
                                    tint = Color(0xFF92400E)
                                )
                            }

                            AnimatedVisibility(visible = showScenarioPicker) {
                                Column(modifier = Modifier.padding(top = 8.dp)) {
                                    HorizontalDivider(color = Color(0xFFFDE68A))
                                    Spacer(modifier = Modifier.height(6.dp))
                                    SandboxTrainingManager.predefinedScenarios.forEach { scenario ->
                                        val isSelected = scenario.index == selectedScenarioIndex
                                        Surface(
                                            color = if (isSelected) Color(0xFFFEF3C7) else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    selectedScenarioIndex = scenario.index
                                                    showScenarioPicker = false
                                                }
                                                .padding(vertical = 2.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                                    contentDescription = scenario.title,
                                                    tint = if (isSelected) Color(0xFFD97706) else Color(0xFF9CA3AF),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(
                                                        text = scenario.title,
                                                        fontSize = 12.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                        color = Color(0xFF78350F)
                                                    )
                                                    Text(
                                                        text = "${scenario.village}, Kec. ${scenario.district} • Pelapor: ${scenario.reporterName}",
                                                        fontSize = 10.sp,
                                                        color = Color(0xFF92400E)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Action Buttons Row
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val injected = SandboxTrainingManager.injectSimulatedCitizenReport(
                                    context = context,
                                    scenarioIndex = selectedScenarioIndex,
                                    triggerRealtimeAlertBanner = true
                                )
                                selectedScenarioIndex = (selectedScenarioIndex + 1) % SandboxTrainingManager.predefinedScenarios.size
                                onScenarioInjected?.invoke(injected.id)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_inject_sandbox_report"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFB45309),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddAlert,
                                contentDescription = "Suntikkan Laporan Simulasi",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "+ Suntikkan Laporan Simulasi",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        if (onOpenDailyReportForm != null) {
                            OutlinedButton(
                                onClick = onOpenDailyReportForm,
                                modifier = Modifier.testTag("btn_sandbox_open_daily_form"),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.5.dp, Color(0xFFB45309)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFF92400E)
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = "Latihan Form Harian",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Latihan Form Harian",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        TextButton(
                            onClick = {
                                SandboxTrainingManager.resetSandboxData(context, reseedDefaults = true)
                            },
                            modifier = Modifier.testTag("btn_reset_sandbox_data"),
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = Color(0xFF92400E)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset Latihan",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Reset Skenario",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SandboxMetricChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.White,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFFFDE68A))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF78350F),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Text(
                text = label,
                fontSize = 9.5.sp,
                color = Color(0xFF92400E),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun TrainingStepRow(
    stepNumber: Int,
    title: String,
    isDone: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = "Langkah $stepNumber",
            tint = if (isDone) Color(0xFF10B981) else Color(0xFFD97706),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$stepNumber. $title",
            fontSize = 11.sp,
            fontWeight = if (isDone) FontWeight.Bold else FontWeight.Medium,
            color = if (isDone) Color(0xFF065F46) else Color(0xFF78350F)
        )
    }
}

/**
 * Pita Peringatan Ringkas (Compact Strip Banner) yang tampil di bagian atas Mode Warga dan
 * Formulir Laporan Harian ketika Mode Latihan Petugas (Sandbox) sedang aktif.
 */
@Composable
fun SandboxTrainingCompactNoticeBanner(
    screenContextLabel: String,
    onInjectDrillClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isSandboxEnabled by SandboxTrainingManager.isSandboxEnabled.collectAsState()

    AnimatedVisibility(visible = isSandboxEnabled) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .testTag("sandbox_compact_notice_banner"),
            color = Color(0xFFFEF3C7),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.5.dp, Color(0xFFF59E0B))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = "Mode Latihan Aktif",
                        tint = Color(0xFFB45309),
                        modifier = Modifier
                            .padding(top = 1.dp)
                            .size(18.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "SANDBOX LATIHAN PETUGAS AKTIF ($screenContextLabel)",
                            fontSize = 11.5.sp,
                            lineHeight = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF78350F),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Seluruh input, validasi & cetak PDF terisolasi 100% — Tidak masuk database produksi.",
                            fontSize = 10.5.sp,
                            lineHeight = 14.sp,
                            color = Color(0xFF92400E)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    if (onInjectDrillClick != null) {
                        OutlinedButton(
                            onClick = onInjectDrillClick,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFB45309)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.heightIn(min = 34.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF92400E)
                            )
                        ) {
                            Text(
                                text = "+ Simulasi",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            SandboxTrainingManager.setSandboxModeEnabled(context, false)
                        },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .heightIn(min = 34.dp)
                            .testTag("btn_exit_sandbox_compact"),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0xFFFEF2F2),
                            contentColor = Color(0xFFB91C1C)
                        )
                    ) {
                        Text(
                            text = "Keluar Latihan",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}
