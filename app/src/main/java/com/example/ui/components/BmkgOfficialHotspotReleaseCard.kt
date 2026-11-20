package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.api.BmkgHotspotMonthlyRelease
import com.example.data.api.BmkgOfficialHotspotReleaseSummary
import com.example.data.repository.KarhutlaHotspotRepository
import com.example.model.District
import com.example.ui.theme.OrangePrimary

// Official BMKG Signature Palette (Navy Atmosphere Blue, Ocean Blue, Emerald Climatology Green, Solar Gold)
private val BmkgNavyBlue = Color(0xFF003B73)
private val BmkgOceanBlue = Color(0xFF0077B6)
private val BmkgEmeraldGreen = Color(0xFF00994C)
private val BmkgSolarGold = Color(0xFFFACC15)

private enum class BmkgReleaseYearFilter(val label: String) {
    ALL_SINCE_2025("Semua (2025–Kini)"),
    YEAR_2025("Thn 2025"),
    YEAR_CURRENT("2026–Kini")
}

private enum class BmkgReleaseDetailMode(val label: String) {
    MONTHLY_BULLETIN("Rincian Bulanan"),
    DISTRICT_DISTRIBUTION("Sebaran Kecamatan")
}

/**
 * Compact & Illustrated BMKG Official Hotspot Release Card (2025 – Saat Ini):
 * - Uses BMKG signature blue-green colors & custom atmospheric/climatology wave illustration
 *   on the "BMKG Stasiun Klimatologi Bengkulu & Satelit Cuaca Sipongi" strip with minimal footprint.
 * - Eliminates excess empty white space while displaying 2025–present hotspot counts and periodic sync status.
 */
@Composable
fun BmkgOfficialHotspotReleaseCard(
    currentDistrict: District,
    isDark: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { KarhutlaHotspotRepository(context) }

    var releaseSummary by remember { mutableStateOf<BmkgOfficialHotspotReleaseSummary?>(null) }
    var selectedYearFilter by remember { mutableStateOf(BmkgReleaseYearFilter.ALL_SINCE_2025) }
    var selectedDetailMode by remember { mutableStateOf(BmkgReleaseDetailMode.MONTHLY_BULLETIN) }
    var selectedMonthIndex by remember { mutableIntStateOf(-1) }
    var showAllRows by remember { mutableStateOf(false) }

    val jan2025EpochUtc = 1735689600_000L
    val liveHotspotsSince2025 by remember {
        repository.observeHotspotsSince(jan2025EpochUtc)
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    LaunchedEffect(liveHotspotsSince2025.size) {
        releaseSummary = repository.getOfficialBmkgReleaseSummarySince2025(forceRefresh = false)
    }

    val summary = releaseSummary
    // Subtle BMKG blue-green tinted card surface instead of stark plain white
    val containerBg = if (isDark) Color(0xFF0B1727) else Color(0xFFF2F9F9)
    val borderColor = if (isDark) BmkgEmeraldGreen.copy(alpha = 0.45f) else BmkgOceanBlue.copy(alpha = 0.35f)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("bmkg_official_hotspot_release_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = containerBg),
        border = BorderStroke(1.2.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // 1. COMPACT HEADER: Full Title & Subtitle (No refresh button blocking the text)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(BmkgNavyBlue, BmkgEmeraldGreen)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Radar,
                        contentDescription = "Rilis Resmi Hotspot BMKG",
                        tint = Color.White,
                        modifier = Modifier.size(19.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Rilis Resmi Hotspot BMKG (2025 – Saat Ini)",
                        fontSize = 13.5.sp,
                        lineHeight = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Rekapitulasi Temuan Titik Panas Kab. Mukomuko • Update Berkala",
                        fontSize = 10.sp,
                        lineHeight = 13.sp,
                        color = if (isDark) Color(0xFF38BDF8) else BmkgOceanBlue,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(7.dp))

            if (summary != null) {
                // 2. COMPACT ILLUSTRATED BMKG STATION BANNER ("BMKG Stasiun Klimatologi Bengkulu & Satelit Cuaca Sipongi")
                // Uses BMKG signature Blue-Emerald gradient + atmospheric/earth wave illustration, without logo
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    BmkgNavyBlue,
                                    BmkgOceanBlue,
                                    BmkgEmeraldGreen
                                )
                            )
                        )
                        .drawBehind {
                            // Subtle BMKG decorative atmospheric & climatology wave arcs + satellite orbit lines
                            val w = size.width
                            val h = size.height
                            val wavePath1 = Path().apply {
                                moveTo(w * 0.55f, h)
                                cubicTo(w * 0.68f, h * 0.15f, w * 0.82f, h * 0.85f, w, h * 0.1f)
                            }
                            drawPath(
                                path = wavePath1,
                                color = Color.White.copy(alpha = 0.14f),
                                style = Stroke(width = 2.dp.toPx())
                            )
                            val wavePath2 = Path().apply {
                                moveTo(w * 0.40f, 0f)
                                cubicTo(w * 0.60f, h * 0.75f, w * 0.78f, h * 0.20f, w, h * 0.90f)
                            }
                            drawPath(
                                path = wavePath2,
                                color = BmkgSolarGold.copy(alpha = 0.22f),
                                style = Stroke(width = 1.5.dp.toPx())
                            )
                            drawCircle(
                                color = Color.White.copy(alpha = 0.08f),
                                radius = h * 0.85f,
                                center = Offset(w * 0.92f, h * 0.2f)
                            )
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("bmkg_station_illustrated_banner")
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = summary.issuingStation,
                            fontSize = 10.5.sp,
                            lineHeight = 13.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                text = summary.bulletinReferenceCode,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = BmkgSolarGold,
                                maxLines = 1
                            )
                            Text(
                                text = "•",
                                fontSize = 9.sp,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                            Text(
                                text = "Rilis: ${summary.releaseFormattedWib}",
                                fontSize = 9.sp,
                                color = Color.White.copy(alpha = 0.92f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 3. COMPACT CUMULATIVE SUMMARY METRICS (2025 - PRESENT)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BmkgReleaseMetricBox(
                        title = "Total 2025–Kini",
                        value = "${summary.cumulativeTotalSince2025}",
                        subtitle = "Akumulasi Titik",
                        accentColor = BmkgOceanBlue,
                        isDark = isDark,
                        modifier = Modifier.weight(1f)
                    )
                    BmkgReleaseMetricBox(
                        title = "Rilis Thn 2025",
                        value = "${summary.total2025Count}",
                        subtitle = "Jan–Des 2025",
                        accentColor = BmkgEmeraldGreen,
                        isDark = isDark,
                        modifier = Modifier.weight(1f)
                    )
                    BmkgReleaseMetricBox(
                        title = "Thn 2026–Kini",
                        value = "${summary.total2026ToDateCount}",
                        subtitle = "Rilis Berjalan",
                        accentColor = OrangePrimary,
                        isDark = isDark,
                        modifier = Modifier.weight(1f)
                    )
                    BmkgReleaseMetricBox(
                        title = "Kategori Tinggi",
                        value = "${summary.highConfidenceTotal}",
                        subtitle = "High Conf.",
                        accentColor = Color(0xFFDC2626),
                        isDark = isDark,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 4. COMPACT YEAR FILTER PILLS & CHART HEADER IN ONE ROW
                val filteredMonths = remember(summary.monthlyReleases, selectedYearFilter) {
                    when (selectedYearFilter) {
                        BmkgReleaseYearFilter.ALL_SINCE_2025 -> summary.monthlyReleases
                        BmkgReleaseYearFilter.YEAR_2025 -> summary.monthlyReleases.filter { it.year == 2025 }
                        BmkgReleaseYearFilter.YEAR_CURRENT -> summary.monthlyReleases.filter { it.year >= 2026 }
                    }
                }

                Surface(
                    color = if (isDark) Color(0xFF132338) else Color(0xFFE6F4F1).copy(alpha = 0.65f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF1E3A5F) else BmkgOceanBlue.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bmkg_release_monthly_chart_box")
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        // Baris 1: Judul Grafik & Indikator Skala Puncak (Sejajar & Tidak Bertabrakan)
                        val maxMonthlyCount = (filteredMonths.maxOfOrNull { it.totalHotspots } ?: 1).coerceAtLeast(1)
                        val midMonthlyCount = (maxMonthlyCount / 2).coerceAtLeast(1)

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
                                    imageVector = Icons.Default.BarChart,
                                    contentDescription = null,
                                    tint = BmkgOceanBlue,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Tren Bulanan (${filteredMonths.size} Bln)",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    softWrap = false
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Surface(
                                color = BmkgOceanBlue.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(0.8.dp, BmkgOceanBlue.copy(alpha = 0.35f))
                            ) {
                                Text(
                                    text = "Maks: $maxMonthlyCount Titik",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFF38BDF8) else BmkgNavyBlue,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Baris 2: Filter Periode Tahun (Horizontal Rata & Single-Line tanpa Terpotong "Ki"/"ni")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BmkgReleaseYearFilter.entries.forEach { filter ->
                                val selected = selectedYearFilter == filter
                                Surface(
                                    color = if (selected) BmkgOceanBlue else Color.Transparent,
                                    shape = RoundedCornerShape(7.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (selected) BmkgOceanBlue else BmkgOceanBlue.copy(alpha = 0.45f)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            selectedYearFilter = filter
                                            selectedMonthIndex = -1
                                        }
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = filter.label,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Baris 3: Area Grafik Batang + Sumbu Y yang Sejajar dan Rapi
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            // Sumbu Y (Kiri): Label Skala Sejajar (Maks, Tengah, 0) dengan lebar tetap agar tidak menumpuk
                            Column(
                                modifier = Modifier
                                    .widthIn(min = 26.dp)
                                    .fillMaxHeight()
                                    .padding(top = 4.dp, bottom = 16.dp, end = 6.dp),
                                verticalArrangement = Arrangement.SpaceBetween,
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = "$maxMonthlyCount",
                                    fontSize = 8.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(
                                    text = "$midMonthlyCount",
                                    fontSize = 8.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(
                                    text = "0",
                                    fontSize = 8.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            // Garis pembatas vertikal Sumbu Y
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .fillMaxHeight()
                                    .padding(top = 4.dp, bottom = 16.dp)
                                    .background(BmkgOceanBlue.copy(alpha = 0.28f))
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            // Deretan Batang Bulanan (Scrollable dengan Padding Kanan Cukup)
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(end = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                filteredMonths.forEachIndexed { idx, monthItem ->
                                    val isSelectedBar = selectedMonthIndex == idx
                                    val ratio = (monthItem.totalHotspots.toFloat() / maxMonthlyCount.toFloat()).coerceIn(0.15f, 1f)
                                    val barBrush = when {
                                        isSelectedBar -> Brush.verticalGradient(listOf(OrangePrimary, Color(0xFFDC2626)))
                                        monthItem.year >= 2026 -> Brush.verticalGradient(listOf(BmkgEmeraldGreen, BmkgOceanBlue))
                                        else -> Brush.verticalGradient(listOf(BmkgOceanBlue, BmkgNavyBlue))
                                    }

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Bottom,
                                        modifier = Modifier
                                            .width(36.dp)
                                            .fillMaxHeight()
                                            .clickable {
                                                selectedMonthIndex = if (selectedMonthIndex == idx) -1 else idx
                                            }
                                    ) {
                                        Text(
                                            text = "${monthItem.totalHotspots}",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isSelectedBar) OrangePrimary else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Box(
                                            modifier = Modifier
                                                .width(20.dp)
                                                .fillMaxHeight(ratio * 0.66f)
                                                .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                                                .background(barBrush)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = monthItem.periodCode.substringAfter("-").let { m ->
                                                "$m/'${monthItem.year.toString().takeLast(2)}"
                                            },
                                            fontSize = 8.5.sp,
                                            fontWeight = if (isSelectedBar) FontWeight.ExtraBold else FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }

                        // Highlighted Month Compact Strip
                        val highlightedMonth: BmkgHotspotMonthlyRelease? =
                            filteredMonths.getOrNull(selectedMonthIndex) ?: filteredMonths.lastOrNull()

                        if (highlightedMonth != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                color = if (isDark) Color(0xFF0B1727) else Color.White.copy(alpha = 0.9f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(0.8.dp, BmkgEmeraldGreen.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${highlightedMonth.monthName} (${highlightedMonth.releaseBulletinNumber})",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Dominan: Kec. ${highlightedMonth.dominantSubDistricts} • Verifikasi Darat: ${highlightedMonth.verifiedGroundCount}",
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                        ConfidenceMiniPill("H:${highlightedMonth.highConfidenceCount}", Color(0xFFDC2626))
                                        ConfidenceMiniPill("N:${highlightedMonth.nominalConfidenceCount}", Color(0xFFEA580C))
                                        ConfidenceMiniPill("L:${highlightedMonth.lowConfidenceCount}", Color(0xFFD97706))
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 5. COMPACT VIEW MODE TOGGLE & EXPANDABLE LIST
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BmkgReleaseDetailMode.entries.forEach { mode ->
                        val isSelected = selectedDetailMode == mode
                        Surface(
                            color = if (isSelected) BmkgNavyBlue else Color.Transparent,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) BmkgNavyBlue else BmkgOceanBlue.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedDetailMode = mode }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (mode == BmkgReleaseDetailMode.MONTHLY_BULLETIN) {
                                        Icons.Default.Analytics
                                    } else {
                                        Icons.Default.LocationOn
                                    },
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = mode.label,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Surface(
                        color = BmkgEmeraldGreen.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(0.8.dp, BmkgEmeraldGreen.copy(alpha = 0.5f)),
                        modifier = Modifier.clickable { showAllRows = !showAllRows }
                    ) {
                        Text(
                            text = if (showAllRows) "Ringkas" else "Semua",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = BmkgEmeraldGreen,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                val rowLimit = if (showAllRows) 12 else 4
                if (selectedDetailMode == BmkgReleaseDetailMode.MONTHLY_BULLETIN) {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        filteredMonths.reversed().take(rowLimit).forEach { item ->
                            Surface(
                                color = if (isDark) Color(0xFF132338) else Color.White.copy(alpha = 0.85f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(
                                    0.8.dp,
                                    if (item.isCurrentPeriodLive) BmkgEmeraldGreen
                                    else if (isDark) Color(0xFF1E3A5F) else Color(0xFFCBD5E1)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = item.monthName,
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = item.releaseBulletinNumber,
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = BmkgOceanBlue,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Text(
                                            text = "Dominan: Kec. ${item.dominantSubDistricts}",
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        ConfidenceMiniPill("H:${item.highConfidenceCount}", Color(0xFFDC2626))
                                        ConfidenceMiniPill("N:${item.nominalConfidenceCount}", Color(0xFFEA580C))
                                        Surface(
                                            color = BmkgOceanBlue,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "${item.totalHotspots} Titik",
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        summary.districtRecaps.take(rowLimit).forEach { distRecap ->
                            val isCurrentDistrict = distRecap.subDistrictName.equals(currentDistrict.name, ignoreCase = true)
                            Surface(
                                color = if (isCurrentDistrict) BmkgEmeraldGreen.copy(alpha = 0.14f)
                                else if (isDark) Color(0xFF132338) else Color.White.copy(alpha = 0.85f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(
                                    0.8.dp,
                                    if (isCurrentDistrict) BmkgEmeraldGreen
                                    else if (isDark) Color(0xFF1E3A5F) else Color(0xFFCBD5E1)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Kec. ${distRecap.subDistrictName}" +
                                                if (isCurrentDistrict) " (Aktif)" else "",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = distRecap.highRiskZone,
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        ConfidenceMiniPill("'25:${distRecap.count2025}", BmkgNavyBlue)
                                        ConfidenceMiniPill("'26:${distRecap.count2026ToDate}", BmkgEmeraldGreen)
                                        Surface(
                                            color = BmkgOceanBlue,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "Σ ${distRecap.totalCount}",
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BmkgReleaseMetricBox(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (isDark) Color(0xFF132338) else Color.White.copy(alpha = 0.88f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.38f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                softWrap = false
            )
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor,
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = subtitle,
                fontSize = 8.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                softWrap = false
            )
        }
    }
}

@Composable
private fun ConfidenceMiniPill(
    text: String,
    color: Color
) {
    Surface(
        color = color.copy(alpha = 0.14f),
        shape = RoundedCornerShape(5.dp),
        border = BorderStroke(0.8.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            text = text,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.ExtraBold,
            color = color,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
        )
    }
}
