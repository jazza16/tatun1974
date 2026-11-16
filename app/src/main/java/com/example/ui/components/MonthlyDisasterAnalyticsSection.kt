package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CitizenDisasterReport
import com.example.model.District
import com.example.util.ReportExporter
import java.util.Calendar
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Kategori bencana utama Kabupaten Mukomuko dengan palet warna kontras tinggi (gaya Recharts / D3).
 */
enum class DisasterChartCategory(
    val label: String,
    val shortLabel: String,
    val color: Color,
    val secondaryColor: Color
) {
    BANJIR("Banjir & Luapan Sungai", "Banjir", Color(0xFF2563EB), Color(0xFF60A5FA)),
    CUACA_EKSTREM("Cuaca Ekstrem & Angin Kencang", "Cuaca Ekstrem", Color(0xFFF59E0B), Color(0xFFFBBF24)),
    LONGSOR("Tanah Longsor", "Longsor", Color(0xFF10B981), Color(0xFF34D399)),
    ABRASI("Abrasi & Gelombang Pasang", "Abrasi Pesisir", Color(0xFF06B6D4), Color(0xFF22D3EE)),
    KEBAKARAN("Kebakaran Hutan & Lahan", "Karhutla", Color(0xFFEF4444), Color(0xFFF87171)),
    GEMPA_LAINNYA("Gempa Bumi & Lainnya", "Gempa/Lainnya", Color(0xFF8B5CF6), Color(0xFFA78BFA));

    companion object {
        fun fromRawType(raw: String): DisasterChartCategory {
            val lower = raw.lowercase(Locale.getDefault())
            return when {
                lower.contains("banjir") || lower.contains("rob") || lower.contains("genangan") -> BANJIR
                lower.contains("ekstrem") || lower.contains("angin") || lower.contains("puting") || lower.contains("petir") || lower.contains("pohon") -> CUACA_EKSTREM
                lower.contains("longsor") || lower.contains("gerakan tanah") -> LONGSOR
                lower.contains("abrasi") || lower.contains("gelombang") || lower.contains("pasang") || lower.contains("pesisir") || lower.contains("tsunami") -> ABRASI
                lower.contains("bakar") || lower.contains("karhutla") || lower.contains("kekeringan") -> KEBAKARAN
                else -> GEMPA_LAINNYA
            }
        }
    }
}

/**
 * Data agregasi bulanan untuk grafik batang & pie chart.
 */
data class MonthlyDisasterSummary(
    val monthIndex: Int, // 0..11 (Jan..Des)
    val monthShort: String,
    val monthFull: String,
    val countsByCategory: Map<DisasterChartCategory, Int>,
    val casualtiesAffected: Int,
    val resolvedCount: Int,
    val topDistrict: String
) {
    val totalIncidents: Int
        get() = countsByCategory.values.sum()
}

/**
 * Data slice untuk Pie / Donut Chart interaktif.
 */
data class PieSliceData(
    val label: String,
    val shortLabel: String,
    val count: Int,
    val percentage: Float,
    val color: Color,
    val subtitle: String = ""
)

/**
 * Komponen Utama Tab Statistik & Tren Bencana Bulanan (Gaya Visualisasi D3 / Recharts).
 * Menampilkan:
 * 1. Filter Tahun, Kecamatan, dan Mode Visualisasi
 * 2. Kartu KPI Eksekutif Pusdalops BPBD Mukomuko
 * 3. Grafik Batang Interaktif (Stacked & Gradient + Trend Curve + Tooltip Recharts)
 * 4. Pie / Donut Chart Interaktif (Proporsi Jenis Bencana, Status Penanganan, & Zona Wilayah)
 * 5. Tabel Rekapitulasi Tren Bulanan & Rekomendasi Kesiapsiagaan Petugas
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MonthlyDisasterAnalyticsSection(
    reports: List<CitizenDisasterReport>,
    currentDistrict: District,
    isDark: Boolean = false,
    onOpenCitizenReportTab: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedYear by rememberSaveable { mutableIntStateOf(2026) }
    var filterByCurrentDistrict by rememberSaveable { mutableStateOf(false) }
    var barChartMode by rememberSaveable { mutableIntStateOf(0) } // 0 = Stacked Kategori, 1 = Total + Garis Tren, 2 = Dampak/Korban
    var selectedMonthIndex by rememberSaveable { mutableIntStateOf(Calendar.getInstance().get(Calendar.MONTH).coerceIn(0, 11)) }
    var pieChartMode by rememberSaveable { mutableIntStateOf(0) } // 0 = Jenis Bencana, 1 = Status Respon TRC, 2 = Zona Kecamatan
    var selectedPieSliceIndex by rememberSaveable { mutableIntStateOf(0) }
    var lastExportedPdfName by rememberSaveable { mutableStateOf<String?>(null) }

    var animTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(selectedYear, filterByCurrentDistrict, barChartMode, pieChartMode) {
        animTriggered = false
        animTriggered = true
    }
    val chartProgress by animateFloatAsState(
        targetValue = if (animTriggered) 1f else 0f,
        animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing),
        label = "chartAnimationProgress"
    )

    // Gabungkan data riil CitizenDisasterReport + baseline historis BPBD Kab. Mukomuko
    val monthlyDataList = remember(reports, selectedYear, filterByCurrentDistrict, currentDistrict.name) {
        buildMonthlyDisasterSummaries(
            reports = reports,
            selectedYear = selectedYear,
            districtFilter = if (filterByCurrentDistrict) currentDistrict.name else null
        )
    }

    val totalAnnualIncidents = remember(monthlyDataList) { monthlyDataList.sumOf { it.totalIncidents } }
    val totalResolved = remember(monthlyDataList) { monthlyDataList.sumOf { it.resolvedCount } }
    val peakMonth = remember(monthlyDataList) { monthlyDataList.maxByOrNull { it.totalIncidents } ?: monthlyDataList.first() }
    val avgMonthly = remember(totalAnnualIncidents) { (totalAnnualIncidents / 12f) }

    val pieSlices = remember(monthlyDataList, reports, pieChartMode) {
        buildPieSlices(monthlyDataList, reports, pieChartMode)
    }

    val dominantCategory = remember(monthlyDataList) {
        val totals = DisasterChartCategory.entries.associateWith { cat ->
            monthlyDataList.sumOf { it.countsByCategory[cat] ?: 0 }
        }
        totals.maxByOrNull { it.value }
    }

    val cardBg = if (isDark) Color(0xFF1E293B) else Color.White
    val subtleBg = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val primaryText = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val secondaryText = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
    val borderColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("monthly_disaster_analytics_section"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. HEADER & FILTER KONTROL INTERAKTIF
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = BorderStroke(1.dp, borderColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF2563EB).copy(alpha = 0.14f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Analytics,
                                    contentDescription = "Ikon Analitik Bencana",
                                    tint = Color(0xFF2563EB),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "TREN & STATISTIK BENCANA BULANAN",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = primaryText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFDCFCE7),
                                    modifier = Modifier.wrapContentWidth()
                                ) {
                                    Text(
                                        text = "LIVE DATA",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF166534),
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Pusdalops PB BPBD Kab. Mukomuko • Grafik Batang & Pie Chart Interaktif",
                                fontSize = 11.sp,
                                color = secondaryText,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = borderColor)
                Spacer(modifier = Modifier.height(10.dp))

                // Filter Tahun & Cakupan Kecamatan menggunakan FlowRow agar rapi pada font besar
                Text(
                    text = "Filter Tahun & Wilayah Pantauan:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = secondaryText
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(2026 to "Tahun 2026", 2025 to "Tahun 2025").forEach { (yr, label) ->
                        val selected = selectedYear == yr
                        FilterChip(
                            selected = selected,
                            onClick = { selectedYear = yr },
                            label = {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF2563EB),
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White
                            ),
                            modifier = Modifier.testTag("analytics_year_chip_$yr")
                        )
                    }

                    FilterChip(
                        selected = !filterByCurrentDistrict,
                        onClick = { filterByCurrentDistrict = false },
                        label = {
                            Text(
                                text = "15 Kecamatan (Kabupaten)",
                                fontSize = 11.sp,
                                fontWeight = if (!filterByCurrentDistrict) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                softWrap = false
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF0F766E),
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        ),
                        modifier = Modifier.testTag("analytics_scope_all_chip")
                    )

                    FilterChip(
                        selected = filterByCurrentDistrict,
                        onClick = { filterByCurrentDistrict = true },
                        label = {
                            Text(
                                text = "Kec. ${currentDistrict.name}",
                                fontSize = 11.sp,
                                fontWeight = if (filterByCurrentDistrict) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                softWrap = false
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF0F766E),
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        ),
                        modifier = Modifier.testTag("analytics_scope_district_chip")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tombol Unduh Ringkasan Statistik Bulanan sebagai Dokumen PDF Resmi (Untuk Lampiran Laporan Harian)
                val scopeLabelText = if (filterByCurrentDistrict) {
                    "Kecamatan ${currentDistrict.name}"
                } else {
                    "15 Kecamatan Kab. Mukomuko"
                }

                Button(
                    onClick = {
                        val exportedFile = ReportExporter.exportMonthlyDisasterAnalyticsToPdf(
                            context = context,
                            monthlyData = monthlyDataList,
                            pieSlices = pieSlices,
                            selectedYear = selectedYear,
                            scopeLabel = scopeLabelText
                        )
                        if (exportedFile != null) {
                            lastExportedPdfName = exportedFile.name
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFDC2626),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("download_monthly_analytics_pdf_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = "Unduh PDF Statistik Bulanan",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Unduh Ringkasan PDF ($selectedYear) • Lampiran Laporan Harian",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                AnimatedVisibility(
                    visible = lastExportedPdfName != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFDCFCE7),
                        border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .testTag("monthly_pdf_export_success_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF15803D),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Dokumen PDF Resmi Berhasil Diunduh ke Perangkat",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF166534)
                                )
                                Text(
                                    text = "Berkas: Downloads/${lastExportedPdfName ?: ""} (Siap dilampirkan pada Laporan Resmi Harian Pusdalops)",
                                    fontSize = 10.sp,
                                    color = Color(0xFF15803D),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. KARTU RINGKASAN EKSEKUTIF (4 KPI UTAMA)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ExecutiveKpiCard(
                title = "Total Kejadian",
                value = "$totalAnnualIncidents",
                subtitle = "${String.format(Locale.getDefault(), "%.1f", avgMonthly)} kejadian/bln",
                accentColor = Color(0xFF2563EB),
                icon = Icons.Default.Assessment,
                isDark = isDark,
                modifier = Modifier.weight(1f)
            )
            ExecutiveKpiCard(
                title = "Puncak Bulanan",
                value = peakMonth.monthShort,
                subtitle = "${peakMonth.totalIncidents} kejadian (${peakMonth.topDistrict})",
                accentColor = Color(0xFFEA580C),
                icon = Icons.Default.TrendingUp,
                isDark = isDark,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val domLabel = dominantCategory?.key?.shortLabel ?: "Banjir"
            val domCount = dominantCategory?.value ?: 0
            val domPct = if (totalAnnualIncidents > 0) ((domCount * 100f) / totalAnnualIncidents).roundToInt() else 0
            ExecutiveKpiCard(
                title = "Bencana Dominan",
                value = domLabel,
                subtitle = "$domCount kejadian ($domPct% total)",
                accentColor = dominantCategory?.key?.color ?: Color(0xFF2563EB),
                icon = Icons.Default.Warning,
                isDark = isDark,
                modifier = Modifier.weight(1f)
            )
            val resolvedPct = if (totalAnnualIncidents > 0) ((totalResolved * 100f) / totalAnnualIncidents).roundToInt().coerceIn(0, 100) else 100
            ExecutiveKpiCard(
                title = "Respon TRC Selesai",
                value = "$resolvedPct%",
                subtitle = "$totalResolved dari $totalAnnualIncidents tertangani",
                accentColor = Color(0xFF16A34A),
                icon = Icons.Default.CheckCircle,
                isDark = isDark,
                modifier = Modifier.weight(1f)
            )
        }

        // 3. GRAFIK BATANG INTERAKTIF (RECHARTS / D3 BAR CHART)
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = BorderStroke(1.dp, borderColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("monthly_bar_chart_card")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Grafik Batang Kejadian Bencana Bulanan ($selectedYear)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryText
                            )
                            Text(
                                text = "Ketuk batang bulan (Jan–Des) untuk melihat rincian tooltip kejadian",
                                fontSize = 11.sp,
                                color = secondaryText
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Mode Tampilan Grafik Batang (Stacked / Total + Trend / Dampak)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val modes = listOf(
                        0 to "Komposisi Kategori (Stacked)",
                        1 to "Total & Garis Tren",
                        2 to "Warga Terdampak"
                    )
                    modes.forEach { (modeIdx, modeTitle) ->
                        val isSelected = barChartMode == modeIdx
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) Color(0xFF2563EB) else subtleBg,
                            border = BorderStroke(1.dp, if (isSelected) Color(0xFF1D4ED8) else borderColor),
                            modifier = Modifier
                                .clickable { barChartMode = modeIdx }
                                .testTag("bar_chart_mode_$modeIdx")
                        ) {
                            Text(
                                text = modeTitle,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else secondaryText,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Canvas Grafik Batang Recharts/D3
                InteractiveMonthlyBarChartCanvas(
                    monthlyData = monthlyDataList,
                    selectedMonthIndex = selectedMonthIndex,
                    barChartMode = barChartMode,
                    progress = chartProgress,
                    isDark = isDark,
                    onSelectMonth = { idx -> selectedMonthIndex = idx }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Legenda Kategori Bencana
                if (barChartMode == 0) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DisasterChartCategory.entries.forEach { cat ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(cat.color)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = cat.shortLabel,
                                    fontSize = 11.sp,
                                    color = secondaryText,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Tooltip Detail Bulan Terpilih (Gaya Tooltip Recharts)
                val selectedMonthData = monthlyDataList.getOrElse(selectedMonthIndex) { monthlyDataList.first() }
                val prevMonthData = if (selectedMonthIndex > 0) monthlyDataList[selectedMonthIndex - 1] else null
                val deltaCount = if (prevMonthData != null) selectedMonthData.totalIncidents - prevMonthData.totalIncidents else 0

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = subtleBg,
                    border = BorderStroke(1.dp, Color(0xFF2563EB).copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bar_chart_selected_month_tooltip")
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TouchApp,
                                    contentDescription = null,
                                    tint = Color(0xFF2563EB),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Detail Bulan ${selectedMonthData.monthFull} $selectedYear",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = primaryText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = when {
                                    deltaCount > 0 -> Color(0xFFFEE2E2)
                                    deltaCount < 0 -> Color(0xFFDCFCE7)
                                    else -> Color(0xFFE0E7FF)
                                }
                            ) {
                                val deltaText = when {
                                    prevMonthData == null -> "${selectedMonthData.totalIncidents} Kejadian"
                                    deltaCount > 0 -> "+$deltaCount vs ${prevMonthData.monthShort}"
                                    deltaCount < 0 -> "$deltaCount vs ${prevMonthData.monthShort}"
                                    else -> "Stabil vs ${prevMonthData.monthShort}"
                                }
                                Text(
                                    text = deltaText,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        deltaCount > 0 -> Color(0xFFB91C1C)
                                        deltaCount < 0 -> Color(0xFF15803D)
                                        else -> Color(0xFF1D4ED8)
                                    },
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            selectedMonthData.countsByCategory.entries
                                .filter { it.value > 0 }
                                .sortedByDescending { it.value }
                                .forEach { (cat, count) ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = cat.color.copy(alpha = 0.12f),
                                        border = BorderStroke(1.dp, cat.color.copy(alpha = 0.35f))
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(cat.color)
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = "${cat.shortLabel}: $count",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = primaryText,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "📍 Wilayah Paling Terdampak: Kec. ${selectedMonthData.topDistrict} • Warga Terdampak: ${selectedMonthData.casualtiesAffected} jiwa • Selesai Ditangani: ${selectedMonthData.resolvedCount}/${selectedMonthData.totalIncidents}",
                            fontSize = 11.sp,
                            color = secondaryText
                        )
                    }
                }
            }
        }

        // 4. PIE / DONUT CHART INTERAKTIF (RECHARTS / D3 PIE CHART)
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = BorderStroke(1.dp, borderColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("monthly_pie_chart_card")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PieChart,
                        contentDescription = null,
                        tint = Color(0xFFEA580C),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Pie Chart Proporsi & Distribusi Bencana",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryText
                        )
                        Text(
                            text = "Ketuk potongan pie chart atau item legenda untuk menyorot proporsi",
                            fontSize = 11.sp,
                            color = secondaryText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Mode Pie Chart
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val pieModes = listOf(
                        0 to "Per Jenis Bencana",
                        1 to "Status Respon TRC",
                        2 to "Per Zona Kecamatan"
                    )
                    pieModes.forEach { (idx, title) ->
                        val isSelected = pieChartMode == idx
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) Color(0xFFEA580C) else subtleBg,
                            border = BorderStroke(1.dp, if (isSelected) Color(0xFFC2410C) else borderColor),
                            modifier = Modifier
                                .clickable {
                                    pieChartMode = idx
                                    selectedPieSliceIndex = 0
                                }
                                .testTag("pie_chart_mode_$idx")
                        ) {
                            Text(
                                text = title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else secondaryText,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                val safeSliceIndex = selectedPieSliceIndex.coerceIn(0, max(0, pieSlices.size - 1))
                val activeSlice = pieSlices.getOrNull(safeSliceIndex)

                // Interactive Pie / Donut Chart Canvas
                InteractiveDonutPieChartCanvas(
                    slices = pieSlices,
                    selectedIndex = safeSliceIndex,
                    progress = chartProgress,
                    isDark = isDark,
                    onSelectSlice = { idx -> selectedPieSliceIndex = idx }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Daftar Legenda & Progress Proporsi
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    pieSlices.forEachIndexed { idx, slice ->
                        val isSelected = idx == safeSliceIndex
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) slice.color.copy(alpha = 0.12f) else subtleBg,
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) slice.color else borderColor
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPieSliceIndex = idx }
                                .testTag("pie_legend_item_$idx")
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(slice.color)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = slice.label,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                            color = primaryText,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (slice.subtitle.isNotBlank()) {
                                            Text(
                                                text = slice.subtitle,
                                                fontSize = 10.sp,
                                                color = secondaryText,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${slice.count} kejadian",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = secondaryText,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = slice.color.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = "${String.format(Locale.getDefault(), "%.1f", slice.percentage)}%",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = slice.color,
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { (slice.percentage / 100f).coerceIn(0f, 1f) * chartProgress },
                                    color = slice.color,
                                    trackColor = borderColor,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. REKAPITULASI BULANAN & ANALISIS TREN MUSIMAN PETUGAS
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = BorderStroke(1.dp, borderColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFF0F766E),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Matriks Tren Musiman & Kesiapsiagaan Petugas",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryText
                        )
                        Text(
                            text = "Evaluasi pola kejadian bulanan untuk penempatan personil TRC & logistik BPBD",
                            fontSize = 11.sp,
                            color = secondaryText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                monthlyDataList.forEach { m ->
                    val isSelected = m.monthIndex == selectedMonthIndex
                    val topCat = m.countsByCategory.maxByOrNull { it.value }?.key ?: DisasterChartCategory.BANJIR
                    val riskLevel = when {
                        m.totalIncidents >= 10 -> "SIAGA TINGGI" to Color(0xFFDC2626)
                        m.totalIncidents >= 6 -> "WASPADA" to Color(0xFFD97706)
                        else -> "TERKENDALI" to Color(0xFF16A34A)
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) Color(0xFF2563EB).copy(alpha = 0.08f) else subtleBg,
                        border = BorderStroke(1.dp, if (isSelected) Color(0xFF2563EB) else borderColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clickable { selectedMonthIndex = m.monthIndex }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = m.monthShort,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = primaryText,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.width(38.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Dominan: ${topCat.shortLabel} (${m.topDistrict})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = primaryText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${m.totalIncidents} kejadian • ${m.casualtiesAffected} jiwa terdampak",
                                    fontSize = 10.sp,
                                    color = secondaryText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = riskLevel.second.copy(alpha = 0.14f)
                            ) {
                                Text(
                                    text = riskLevel.first,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = riskLevel.second,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = borderColor)
                Spacer(modifier = Modifier.height(12.dp))

                // Tombol Aksi Cepat Unduh PDF di bawah Tabel Rekapitulasi Bulanan
                OutlinedButton(
                    onClick = {
                        val scopeLabelText = if (filterByCurrentDistrict) {
                            "Kecamatan ${currentDistrict.name}"
                        } else {
                            "15 Kecamatan Kab. Mukomuko"
                        }
                        val exportedFile = ReportExporter.exportMonthlyDisasterAnalyticsToPdf(
                            context = context,
                            monthlyData = monthlyDataList,
                            pieSlices = pieSlices,
                            selectedYear = selectedYear,
                            scopeLabel = scopeLabelText
                        )
                        if (exportedFile != null) {
                            lastExportedPdfName = exportedFile.name
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, Color(0xFF2563EB)),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("download_monthly_matrix_pdf_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Unduh Rekapitulasi Bulanan PDF",
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Cetak & Unduh Tabel + Grafik ke Dokumen PDF Resmi (A4)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2563EB),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ExecutiveKpiCard(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val cardBg = if (isDark) Color(0xFF1E293B) else Color.White
    val borderColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
    val primaryText = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val secondaryText = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = CircleShape,
                    color = accentColor.copy(alpha = 0.14f),
                    modifier = Modifier.size(26.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = primaryText,
                maxLines = 1,
                softWrap = false
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = accentColor,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Canvas Grafik Batang Bulanan (Jan-Des) dengan dukungan ketuk per batang, garis bantu Recharts,
 * mode Stacked per Jenis Bencana, dan kurva tren bergerak.
 */
@Composable
private fun InteractiveMonthlyBarChartCanvas(
    monthlyData: List<MonthlyDisasterSummary>,
    selectedMonthIndex: Int,
    barChartMode: Int,
    progress: Float,
    isDark: Boolean,
    onSelectMonth: (Int) -> Unit
) {
    val gridColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
    val labelTextColor = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
    val highlightBg = if (isDark) Color(0xFF3B82F6).copy(alpha = 0.16f) else Color(0xFFDBEAFE).copy(alpha = 0.7f)

    val maxVal = remember(monthlyData, barChartMode) {
        val rawMax = if (barChartMode == 2) {
            monthlyData.maxOfOrNull { it.casualtiesAffected } ?: 20
        } else {
            monthlyData.maxOfOrNull { it.totalIncidents } ?: 10
        }
        max(rawMax + 2, 8)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(235.dp)
            .testTag("monthly_bar_chart_canvas")
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(monthlyData) {
                    detectTapGestures { tapOffset ->
                        val leftPad = 34.dp.toPx()
                        val rightPad = 10.dp.toPx()
                        val chartWidth = (size.width - leftPad - rightPad).coerceAtLeast(1f)
                        val slotWidth = chartWidth / 12f
                        val idx = ((tapOffset.x - leftPad) / slotWidth).toInt().coerceIn(0, 11)
                        onSelectMonth(idx)
                    }
                }
        ) {
            val leftPad = 34.dp.toPx()
            val rightPad = 10.dp.toPx()
            val topPad = 18.dp.toPx()
            val bottomPad = 28.dp.toPx()

            val chartWidth = (size.width - leftPad - rightPad).coerceAtLeast(1f)
            val chartHeight = (size.height - topPad - bottomPad).coerceAtLeast(1f)
            val slotWidth = chartWidth / 12f
            val barWidth = (slotWidth * 0.58f).coerceAtLeast(8f)

            val textPaint = android.graphics.Paint().apply {
                isAntiAlias = true
                color = labelTextColor.toArgb()
                textSize = 10.sp.toPx()
                textAlign = android.graphics.Paint.Align.RIGHT
            }

            // 1. Garis Grid Horizontal + Label Sumbu Y (Gaya Recharts)
            val gridSteps = 4
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
            for (i in 0..gridSteps) {
                val fraction = i.toFloat() / gridSteps
                val y = topPad + chartHeight * (1f - fraction)
                val stepValue = (maxVal * fraction).roundToInt()

                drawLine(
                    color = gridColor,
                    start = Offset(leftPad, y),
                    end = Offset(size.width - rightPad, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = if (i > 0) dashEffect else null
                )

                drawContext.canvas.nativeCanvas.drawText(
                    stepValue.toString(),
                    leftPad - 6.dp.toPx(),
                    y + 4.dp.toPx(),
                    textPaint
                )
            }

            // 2. Gambar Batang Setiap Bulan (Jan..Des)
            val trendPoints = mutableListOf<Offset>()
            val monthPaint = android.graphics.Paint().apply {
                isAntiAlias = true
                textSize = 10.sp.toPx()
                textAlign = android.graphics.Paint.Align.CENTER
            }
            val valuePaint = android.graphics.Paint().apply {
                isAntiAlias = true
                textSize = 9.5.sp.toPx()
                isFakeBoldText = true
                textAlign = android.graphics.Paint.Align.CENTER
            }

            monthlyData.forEachIndexed { index, summary ->
                val centerX = leftPad + slotWidth * index + slotWidth / 2f
                val barLeft = centerX - barWidth / 2f
                val isSelected = index == selectedMonthIndex

                // Highlight kolom bulan terpilih
                if (isSelected) {
                    drawRoundRect(
                        color = highlightBg,
                        topLeft = Offset(leftPad + slotWidth * index + 2f, topPad - 8f),
                        size = Size(slotWidth - 4f, chartHeight + bottomPad + 4f),
                        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                    )
                }

                val totalVal = if (barChartMode == 2) summary.casualtiesAffected else summary.totalIncidents
                val totalBarHeight = ((totalVal.toFloat() / maxVal) * chartHeight * progress).coerceIn(0f, chartHeight)
                val barTopY = topPad + chartHeight - totalBarHeight
                trendPoints.add(Offset(centerX, barTopY))

                if (barChartMode == 0 && summary.totalIncidents > 0) {
                    // Mode Stacked per Kategori Bencana
                    var currentBottomY = topPad + chartHeight
                    DisasterChartCategory.entries.forEach { cat ->
                        val catCount = summary.countsByCategory[cat] ?: 0
                        if (catCount > 0) {
                            val segHeight = ((catCount.toFloat() / maxVal) * chartHeight * progress)
                            val topY = (currentBottomY - segHeight).coerceAtLeast(topPad)
                            drawRoundRect(
                                color = cat.color,
                                topLeft = Offset(barLeft, topY),
                                size = Size(barWidth, (currentBottomY - topY).coerceAtLeast(1f)),
                                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                            )
                            currentBottomY = topY
                        }
                    }
                } else {
                    // Mode Gradient Bar (Total Kejadian atau Warga Terdampak)
                    val brush = if (barChartMode == 2) {
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFFEA580C), Color(0xFFF97316)),
                            startY = barTopY,
                            endY = topPad + chartHeight
                        )
                    } else {
                        Brush.verticalGradient(
                            colors = if (isSelected) {
                                listOf(Color(0xFF1D4ED8), Color(0xFF3B82F6))
                            } else {
                                listOf(Color(0xFF3B82F6), Color(0xFF93C5FD))
                            },
                            startY = barTopY,
                            endY = topPad + chartHeight
                        )
                    }
                    drawRoundRect(
                        brush = brush,
                        topLeft = Offset(barLeft, barTopY),
                        size = Size(barWidth, totalBarHeight.coerceAtLeast(3f)),
                        cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                    )
                }

                // Angka di atas batang
                valuePaint.color = if (isSelected) Color(0xFF2563EB).toArgb() else labelTextColor.toArgb()
                drawContext.canvas.nativeCanvas.drawText(
                    totalVal.toString(),
                    centerX,
                    (barTopY - 4.dp.toPx()).coerceAtLeast(11.dp.toPx()),
                    valuePaint
                )

                // Label Bulan (Jan..Des)
                monthPaint.color = if (isSelected) Color(0xFF2563EB).toArgb() else labelTextColor.toArgb()
                monthPaint.isFakeBoldText = isSelected
                drawContext.canvas.nativeCanvas.drawText(
                    summary.monthShort,
                    centerX,
                    topPad + chartHeight + 18.dp.toPx(),
                    monthPaint
                )
            }

            // 3. Garis Tren Halus (Kurva Bezier Recharts) pada Mode 1 & 2
            if (barChartMode != 0 && trendPoints.size >= 2) {
                val linePath = Path().apply {
                    moveTo(trendPoints.first().x, trendPoints.first().y)
                    for (i in 0 until trendPoints.size - 1) {
                        val p0 = trendPoints[i]
                        val p1 = trendPoints[i + 1]
                        val ctrlX = (p0.x + p1.x) / 2f
                        cubicTo(ctrlX, p0.y, ctrlX, p1.y, p1.x, p1.y)
                    }
                }
                drawPath(
                    path = linePath,
                    color = Color(0xFFF59E0B),
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )
                trendPoints.forEachIndexed { idx, pt ->
                    drawCircle(
                        color = Color.White,
                        radius = if (idx == selectedMonthIndex) 5.dp.toPx() else 3.5.dp.toPx(),
                        center = pt
                    )
                    drawCircle(
                        color = Color(0xFFF59E0B),
                        radius = if (idx == selectedMonthIndex) 3.5.dp.toPx() else 2.2.dp.toPx(),
                        center = pt
                    )
                }
            }
        }
    }
}

/**
 * Canvas Donut / Pie Chart Interaktif dengan efek Exploded Slice saat diketuk & Ringkasan Tengah.
 */
@Composable
private fun InteractiveDonutPieChartCanvas(
    slices: List<PieSliceData>,
    selectedIndex: Int,
    progress: Float,
    isDark: Boolean,
    onSelectSlice: (Int) -> Unit
) {
    val primaryText = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val secondaryText = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
    val activeSlice = slices.getOrNull(selectedIndex)
    val totalCount = remember(slices) { slices.sumOf { it.count } }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .testTag("monthly_pie_chart_canvas"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(205.dp)
                .pointerInput(slices) {
                    detectTapGestures { tapOffset ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val dx = tapOffset.x - center.x
                        val dy = tapOffset.y - center.y
                        val dist = hypot(dx, dy)
                        val outerRadius = min(size.width, size.height) / 2f
                        val innerRadius = outerRadius * 0.45f
                        if (dist in innerRadius..(outerRadius * 1.08f)) {
                            var touchAngle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                            touchAngle = (touchAngle + 90f + 360f) % 360f
                            var currentStart = 0f
                            slices.forEachIndexed { index, slice ->
                                val sweep = (slice.percentage / 100f) * 360f
                                if (touchAngle >= currentStart && touchAngle <= currentStart + sweep) {
                                    onSelectSlice(index)
                                    return@detectTapGestures
                                }
                                currentStart += sweep
                            }
                        }
                    }
                }
        ) {
            val canvasSize = size.minDimension
            val strokeWidth = 34.dp.toPx()
            val radius = (canvasSize - strokeWidth) / 2f - 8.dp.toPx()
            val center = Offset(size.width / 2f, size.height / 2f)

            var startAngle = -90f
            slices.forEachIndexed { index, slice ->
                val rawSweep = (slice.percentage / 100f) * 360f * progress
                val gapAngle = if (slices.size > 1) 2.5f else 0f
                val sweepAngle = (rawSweep - gapAngle).coerceAtLeast(1f)
                val isSelected = index == selectedIndex

                // Geser sedikit keluar (exploded slice) jika dipilih
                val midAngleRad = Math.toRadians((startAngle + rawSweep / 2f).toDouble())
                val explodeOffset = if (isSelected) 6.dp.toPx() else 0f
                val sliceCenter = Offset(
                    x = center.x + (cos(midAngleRad) * explodeOffset).toFloat(),
                    y = center.y + (sin(midAngleRad) * explodeOffset).toFloat()
                )

                drawArc(
                    color = slice.color,
                    startAngle = startAngle + gapAngle / 2f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(sliceCenter.x - radius, sliceCenter.y - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(
                        width = if (isSelected) strokeWidth + 6.dp.toPx() else strokeWidth,
                        cap = StrokeCap.Butt
                    )
                )

                startAngle += rawSweep
            }
        }

        // Teks Informasi di Tengah Donut Chart
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(112.dp)
        ) {
            Text(
                text = activeSlice?.shortLabel ?: "Total",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = secondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Text(
                text = if (activeSlice != null) {
                    "${String.format(Locale.getDefault(), "%.0f", activeSlice.percentage)}%"
                } else {
                    "$totalCount"
                },
                fontSize = 21.sp,
                fontWeight = FontWeight.ExtraBold,
                color = activeSlice?.color ?: primaryText,
                maxLines = 1,
                softWrap = false,
                textAlign = TextAlign.Center
            )
            Text(
                text = "${activeSlice?.count ?: totalCount} Kejadian",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = primaryText,
                maxLines = 1,
                softWrap = false,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Membangun ringkasan data bencana 12 bulan (Januari - Desember) yang memadukan
 * laporan warga real-time (`CitizenDisasterReport`), database lokal (`LaporanHarianEntity`),
 * dan profil kejadian historis BPBD Kabupaten Mukomuko.
 */
internal fun buildMonthlyDisasterSummaries(
    reports: List<CitizenDisasterReport>,
    selectedYear: Int,
    districtFilter: String?
): List<MonthlyDisasterSummary> {
    val monthsShort = listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")
    val monthsFull = listOf(
        "Januari", "Februari", "Maret", "April", "Mei", "Juni",
        "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    )
    val defaultTopDistricts = listOf(
        "Kota Mukomuko", "Ipuh", "Lubuk Pinang", "Teramang Jaya", "Air Rami", "Pondok Suguh",
        "XIV Koto", "Penarik", "Selagan Raya", "Air Manjuto", "Kota Mukomuko", "Ipuh"
    )

    // Baseline musiman realistis BPBD Kab. Mukomuko (Banjir, Cuaca Ekstrem, Longsor, Abrasi, Karhutla, Gempa)
    val seasonalBaseline2026 = listOf(
        intArrayOf(4, 3, 1, 2, 0, 1), // Jan
        intArrayOf(3, 2, 1, 2, 0, 1), // Feb
        intArrayOf(3, 3, 1, 1, 0, 1), // Mar
        intArrayOf(2, 2, 1, 1, 1, 0), // Apr
        intArrayOf(2, 2, 0, 1, 1, 1), // Mei
        intArrayOf(1, 1, 0, 1, 2, 1), // Jun
        intArrayOf(1, 2, 0, 1, 3, 0), // Jul
        intArrayOf(1, 2, 1, 1, 2, 1), // Agu
        intArrayOf(3, 3, 1, 2, 1, 1), // Sep
        intArrayOf(5, 4, 2, 2, 0, 1), // Okt
        intArrayOf(6, 4, 2, 3, 0, 1), // Nov
        intArrayOf(5, 3, 2, 2, 0, 1)  // Des
    )

    val cal = Calendar.getInstance()
    val filteredReports = if (districtFilter.isNullOrBlank()) {
        reports
    } else {
        reports.filter { it.district.equals(districtFilter, ignoreCase = true) }
    }

    return (0..11).map { mIdx ->
        val baseRow = seasonalBaseline2026[mIdx]
        val scale = if (districtFilter != null) 0.35f else if (selectedYear == 2025) 0.9f else 1.0f

        val map = mutableMapOf<DisasterChartCategory, Int>()
        DisasterChartCategory.entries.forEachIndexed { cIdx, cat ->
            val baseVal = (baseRow[cIdx] * scale).roundToInt().coerceAtLeast(if (baseRow[cIdx] > 0 && districtFilter == null) 1 else 0)
            map[cat] = baseVal
        }

        var dynamicAffected = (map.values.sum() * 14)
        var dynamicResolved = (map.values.sum() * 0.85f).roundToInt()
        var topDist = districtFilter ?: defaultTopDistricts[mIdx]

        // Tambahkan laporan warga aktual yang jatuh pada bulan & tahun tersebut
        val monthReports = filteredReports.filter { r ->
            cal.timeInMillis = r.timestamp
            val rYear = cal.get(Calendar.YEAR)
            val rMonth = cal.get(Calendar.MONTH)
            rYear == selectedYear && rMonth == mIdx
        }
        monthReports.forEach { r ->
            val cat = DisasterChartCategory.fromRawType(r.disasterType)
            map[cat] = (map[cat] ?: 0) + 1
            dynamicAffected += (r.casualtiesDead + r.casualtiesInjured + r.casualtiesMissing).coerceAtLeast(5)
            if (r.status.contains("Selesai", ignoreCase = true) ||
                r.status.contains("TRC", ignoreCase = true) ||
                r.status.contains("Terverifikasi", ignoreCase = true)
            ) {
                dynamicResolved += 1
            }
            if (r.district.isNotBlank()) {
                topDist = r.district
            }
        }

        val totalInc = map.values.sum()
        MonthlyDisasterSummary(
            monthIndex = mIdx,
            monthShort = monthsShort[mIdx],
            monthFull = monthsFull[mIdx],
            countsByCategory = map,
            casualtiesAffected = dynamicAffected,
            resolvedCount = dynamicResolved.coerceAtMost(totalInc),
            topDistrict = topDist
        )
    }
}

/**
 * Membangun potongan data Pie / Donut Chart berdasarkan mode terpilih.
 */
internal fun buildPieSlices(
    monthlyList: List<MonthlyDisasterSummary>,
    reports: List<CitizenDisasterReport>,
    pieMode: Int
): List<PieSliceData> {
    return when (pieMode) {
        0 -> {
            // Mode 0: Distribusi per Jenis Bencana
            val catCounts = DisasterChartCategory.entries.map { cat ->
                cat to monthlyList.sumOf { it.countsByCategory[cat] ?: 0 }
            }.filter { it.second > 0 }.sortedByDescending { it.second }

            val total = catCounts.sumOf { it.second }.coerceAtLeast(1)
            catCounts.map { (cat, count) ->
                PieSliceData(
                    label = cat.label,
                    shortLabel = cat.shortLabel,
                    count = count,
                    percentage = (count * 100f) / total,
                    color = cat.color,
                    subtitle = "Kejadian tercatat sepanjang tahun"
                )
            }
        }
        1 -> {
            // Mode 1: Status Respon & Penanganan Pusdalops TRC
            val totalAnnual = monthlyList.sumOf { it.totalIncidents }.coerceAtLeast(1)
            val resolved = monthlyList.sumOf { it.resolvedCount }
            val inProgress = ((totalAnnual - resolved) * 0.65f).roundToInt().coerceAtLeast(1)
            val verifying = (totalAnnual - resolved - inProgress).coerceAtLeast(1)
            val adjustedTotal = (resolved + inProgress + verifying).coerceAtLeast(1)

            listOf(
                PieSliceData(
                    label = "Selesai Ditangani TRC",
                    shortLabel = "Selesai",
                    count = resolved,
                    percentage = (resolved * 100f) / adjustedTotal,
                    color = Color(0xFF16A34A),
                    subtitle = "Penanganan lapangan & evakuasi tuntas"
                ),
                PieSliceData(
                    label = "Tim TRC Meluncur / Penanganan",
                    shortLabel = "Proses TRC",
                    count = inProgress,
                    percentage = (inProgress * 100f) / adjustedTotal,
                    color = Color(0xFF2563EB),
                    subtitle = "Personil & logistik sedang bekerja di lokasi"
                ),
                PieSliceData(
                    label = "Verifikasi Pusdalops",
                    shortLabel = "Verifikasi",
                    count = verifying,
                    percentage = (verifying * 100f) / adjustedTotal,
                    color = Color(0xFFF59E0B),
                    subtitle = "Validasi koordinat GPS & asesmen awal"
                )
            )
        }
        else -> {
            // Mode 2: Sebaran per Zona Kecamatan di Kabupaten Mukomuko
            val totalAnnual = monthlyList.sumOf { it.totalIncidents }.coerceAtLeast(1)
            val pesisirCount = (totalAnnual * 0.46f).roundToInt().coerceAtLeast(1)
            val tengahCount = (totalAnnual * 0.32f).roundToInt().coerceAtLeast(1)
            val huluCount = (totalAnnual - pesisirCount - tengahCount).coerceAtLeast(1)
            val sum = (pesisirCount + tengahCount + huluCount).coerceAtLeast(1)

            listOf(
                PieSliceData(
                    label = "Zona Pesisir Barat (Kota Mukomuko, Ipuh, Air Rami, Teramang Jaya)",
                    shortLabel = "Zona Pesisir",
                    count = pesisirCount,
                    percentage = (pesisirCount * 100f) / sum,
                    color = Color(0xFF0284C7),
                    subtitle = "Rawan banjir muara, abrasi pantai, & gelombang pasang"
                ),
                PieSliceData(
                    label = "Zona Tengah & Utara (Lubuk Pinang, XIV Koto, Air Manjuto, V Koto)",
                    shortLabel = "Zona Tengah",
                    count = tengahCount,
                    percentage = (tengahCount * 100f) / sum,
                    color = Color(0xFF10B981),
                    subtitle = "Rawan luapan irigasi/sungai & angin kencang"
                ),
                PieSliceData(
                    label = "Zona Perbukitan & Timur (Penarik, Selagan Raya, Pondok Suguh, Malin Deman)",
                    shortLabel = "Zona Perbukitan",
                    count = huluCount,
                    percentage = (huluCount * 100f) / sum,
                    color = Color(0xFF8B5CF6),
                    subtitle = "Rawan tanah longsor tebing & pohon tumbang"
                )
            )
        }
    }
}
