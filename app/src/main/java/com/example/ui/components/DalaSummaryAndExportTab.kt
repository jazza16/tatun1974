package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DalaAssessmentState
import com.example.model.UrgentNeedStatus
import kotlin.math.max

private val SummaryOrange = Color(0xFFEA580C)
private val SummaryOrangeSoft = Color(0xFFFFF7ED)
private val SummaryOrangeBorder = Color(0xFFFED7AA)
private val SummaryRed = Color(0xFFDC2626)
private val SummaryAmber = Color(0xFFD97706)
private val SummaryGreen = Color(0xFF15803D)
private val SummaryGreenSoft = Color(0xFFF0FDF4)
private val SummarySlate = Color(0xFF475569)

/**
 * TAB 4: LAPORAN & REKAP (SUMMARY & EXPORT)
 * Menampilkan ringkasan Read-Only dari Tab 1–3, Grafik Batang Kerusakan Rumah (RB, RS, RR),
 * serta aksi Simpan Draft, Kirim ke Pusdalops, dan Ekspor PDF standar BNPB.
 */
@Composable
fun DalaSummaryTab(
    state: DalaAssessmentState,
    isExportingPdf: Boolean,
    onSaveDraft: () -> Unit,
    onSubmitToPusdalops: () -> Unit,
    onExportPdf: () -> Unit
) {
    val isSubmitted = state.submissionStatus == DalaAssessmentState.STATUS_SUBMITTED

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dala_tab_summary_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Banner Header Read-Only
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SummaryOrangeSoft),
                border = BorderStroke(1.dp, SummaryOrangeBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "REKAP KAJI CEPAT (READ-ONLY)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = SummaryOrange,
                            letterSpacing = 0.6.sp
                        )
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = if (isSubmitted) SummaryGreenSoft else Color.White,
                            border = BorderStroke(
                                1.dp,
                                if (isSubmitted) SummaryGreen.copy(alpha = 0.4f) else SummaryOrangeBorder
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isSubmitted) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = SummaryGreen,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = if (isSubmitted) "TERKIRIM PUSDALOPS" else "DRAFT LOKAL",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSubmitted) SummaryGreen else SummaryOrange
                                )
                            }
                        }
                    }

                    Text(
                        text = state.disasterTitle.ifBlank { "Kejadian Pasca Bencana" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${state.district} • ID ${state.assessmentId} • ${state.assessmentDate}",
                        fontSize = 12.sp,
                        color = SummarySlate
                    )
                    if (state.officerName.isNotBlank()) {
                        Text(
                            text = "Petugas: ${state.officerName}" + if (state.officerNip.isNotBlank()) " (NIP: ${state.officerNip})" else "",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = SummarySlate
                        )
                    }
                }
            }
        }

        // Kartu Statistik Read-Only (Ringkasan Tab 1, 2, 3)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Ringkasan Eksekutif Dampak Bencana",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ReadOnlyStatCard(
                        title = "Total Korban",
                        value = "${state.casualties.totalCasualties}",
                        subtitle = "MD ${state.casualties.dead} • Hilang ${state.casualties.missing} • LB ${state.casualties.heavyInjured} • LR ${state.casualties.lightInjured}",
                        accentColor = SummaryRed,
                        modifier = Modifier.weight(1f)
                    )
                    ReadOnlyStatCard(
                        title = "Pengungsi",
                        value = "${state.casualties.totalEvacuees}",
                        subtitle = "${state.casualties.evacuationSites.size} Titik • ${state.casualties.totalVulnerable} Rentan",
                        accentColor = SummaryOrange,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ReadOnlyStatCard(
                        title = "Rumah Rusak",
                        value = "${state.damages.totalHousesDamaged} Unit",
                        subtitle = "Fasum ${state.damages.totalPublicFacilitiesDamaged} Unit • Jalan ${state.damages.roadMeters} m",
                        accentColor = SummaryAmber,
                        modifier = Modifier.weight(1f)
                    )
                    ReadOnlyStatCard(
                        title = "Kebutuhan",
                        value = "${state.urgentNeeds.size} Item",
                        subtitle = "${state.urgentNeedsPendingCount} Mendesak • ${state.urgentNeeds.size - state.urgentNeedsPendingCount} Tersedia",
                        accentColor = SummaryGreen,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Estimasi Kerugian Ekonomi
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Estimasi Total Kerugian Ekonomi",
                                fontSize = 12.sp,
                                color = SummarySlate
                            )
                            Text(
                                text = state.damages.formatEstimatedLossRupiah(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = SummaryOrange
                            )
                        }
                        Text(
                            text = "Irigasi ${state.damages.irrigationMeters}m\nJembatan ${state.damages.bridgeUnits} Unit",
                            fontSize = 11.sp,
                            color = SummarySlate,
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }

        // GRAFIK BATANG (BAR CHART) KERUSAKAN RUMAH: RB, RS, RR
        item {
            DalaHouseDamageBarChart(
                heavy = state.damages.houseHeavy,
                moderate = state.damages.houseModerate,
                light = state.damages.houseLight
            )
        }

        // Ringkasan Read-Only Titik Pengungsian, Kelompok Rentan & Kebutuhan
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Rincian Lapangan Tercatat",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    SummaryDetailRow(
                        label = "Titik Pengungsian",
                        value = if (state.casualties.evacuationSites.isEmpty()) "-"
                        else state.casualties.evacuationSites.joinToString("\n") { "• ${it.locationName} (${it.evacueeCount} jiwa)" }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    SummaryDetailRow(
                        label = "Kelompok Rentan",
                        value = if (state.casualties.vulnerableGroups.isEmpty()) "-"
                        else state.casualties.vulnerableGroups.joinToString(" • ") { "${it.category.label}: ${it.count}" }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    SummaryDetailRow(
                        label = "Prioritas Kebutuhan Mendesak",
                        value = if (state.urgentNeeds.isEmpty()) "-"
                        else state.urgentNeeds.joinToString("\n") { item ->
                            val tag = if (item.status == UrgentNeedStatus.MENDESAK) "[MENDESAK]" else "[TERSEDIA]"
                            "• ${item.category.label} ${item.quantity} ${item.unit} $tag"
                        }
                    )

                    if (state.damages.damageDescription.isNotBlank()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        SummaryDetailRow(
                            label = "Deskripsi Kerusakan",
                            value = state.damages.damageDescription
                        )
                    }
                }
            }
        }

        // TOMBOL AKSI: SIMPAN DRAFT, KIRIM KE PUSDALOPS, EKSPOR PDF (STANDAR BNPB)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, SummaryOrangeBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Aksi Laporan Kaji Cepat DALA",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onSaveDraft,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, SummaryOrange),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("btn_dala_save_draft")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = null,
                                tint = SummaryOrange,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Simpan Draft",
                                fontWeight = FontWeight.Bold,
                                color = SummaryOrange,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Button(
                            onClick = onSubmitToPusdalops,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SummaryGreen),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("btn_dala_submit_pusdalops")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = null,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Kirim Pusdalops",
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Button(
                        onClick = onExportPdf,
                        enabled = !isExportingPdf,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SummaryOrange),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 50.dp)
                            .testTag("btn_dala_export_pdf")
                    ) {
                        if (isExportingPdf) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Menyusun PDF Standar BNPB...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Ekspor PDF (Standar BNPB)", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadOnlyStatCard(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(accentColor, CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SummarySlate,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                fontSize = 10.5.sp,
                lineHeight = 14.sp,
                color = SummarySlate
            )
        }
    }
}

/**
 * Grafik Batang (Bar Chart) Jetpack Compose Canvas untuk visualisasi jumlah kerusakan rumah (RB, RS, RR).
 */
@Composable
fun DalaHouseDamageBarChart(
    heavy: Int,
    moderate: Int,
    light: Int
) {
    val maxCount = max(1, maxOf(heavy, moderate, light))
    val heavyRatio by animateFloatAsState(
        targetValue = heavy.toFloat() / maxCount.toFloat(),
        animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing),
        label = "bar_rb"
    )
    val moderateRatio by animateFloatAsState(
        targetValue = moderate.toFloat() / maxCount.toFloat(),
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "bar_rs"
    )
    val lightRatio by animateFloatAsState(
        targetValue = light.toFloat() / maxCount.toFloat(),
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "bar_rr"
    )

    val bars = listOf(
        Triple("Rusak Berat (RB)", heavy, Pair(heavyRatio, SummaryRed)),
        Triple("Rusak Sedang (RS)", moderate, Pair(moderateRatio, SummaryOrange)),
        Triple("Rusak Ringan (RR)", light, Pair(lightRatio, SummaryAmber))
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("chart_dala_house_damage")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.BarChart,
                    contentDescription = "Grafik kerusakan rumah",
                    tint = SummaryOrange,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Grafik Kerusakan Rumah (RB / RS / RR)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Total ${heavy + moderate + light} unit rumah terdampak",
                        fontSize = 11.sp,
                        color = SummarySlate
                    )
                }
            }

            // Label angka tepat di atas setiap kolom
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                bars.forEach { (_, count, meta) ->
                    Text(
                        text = "$count Unit",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = meta.second,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Canvas Batang Responsif (menggunakan DrawScope size)
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                val gridLineColor = Color(0xFFE2E8F0)
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                val gridSteps = 4
                for (i in 0..gridSteps) {
                    val y = size.height * (i.toFloat() / gridSteps.toFloat())
                    drawLine(
                        color = gridLineColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = if (i == gridSteps) 2f else 1.2f,
                        pathEffect = if (i == gridSteps) null else dashEffect
                    )
                }

                val slotWidth = size.width / 3f
                val barWidth = (slotWidth * 0.44f).coerceIn(28f, 96f)
                val maxBarHeight = size.height - 12f

                bars.forEachIndexed { index, (_, count, meta) ->
                    val ratio = meta.first
                    val color = meta.second
                    val barHeight = if (count == 0) 6f else max(12f, maxBarHeight * ratio)
                    val left = slotWidth * index + (slotWidth - barWidth) / 2f
                    val top = size.height - barHeight
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(left, top),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(14f, 14f)
                    )
                }
            }

            // Label kategori RB / RS / RR di bawah sumbu X
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                bars.forEach { (label, _, meta) ->
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(meta.second, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = label.substringBefore(" "),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SummarySlate,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryDetailRow(
    label: String,
    value: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = SummaryOrange
        )
        Text(
            text = value,
            fontSize = 12.5.sp,
            lineHeight = 18.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
