package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.District
import com.example.model.MukomukoRpb2025Catalog
import com.example.ui.theme.OrangePrimary
import java.util.Locale

/**
 * Modul UI resmi Dokumen Rencana Penanggulangan Bencana (RPB / RPBD)
 * Kabupaten Mukomuko Tahun 2025–2029 (Pemerintah Daerah Kabupaten Mukomuko, 2026).
 *
 * Ditampilkan sebagai Sub-Tab khusus pada Tab "KRB, RPB & RTRW" agar struktur kode
 * tetap modular dan terpisah dari KrbMukomuko2026Section.kt.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RpbMukomuko2025Section(
    currentDistrict: District,
    isDark: Boolean,
    subCardBg: Color,
    cardBorder: Color,
    accentNavy: Color,
    accentTeal: Color,
    highRiskRed: Color,
    medRiskAmber: Color,
    lowRiskGreen: Color,
    modifier: Modifier = Modifier
) {
    // 0 = Tujuan, Isu & 5 Target 2029, 1 = 31 Strategi, 2 = Matriks OPD & 11 Shelter,
    // 3 = SPM, Sejarah 184 & Fiskal, 4 = Kelembagaan & Monev
    var selectedSection by rememberSaveable { mutableIntStateOf(0) }
    var selectedSasaranFilter by rememberSaveable { mutableIntStateOf(0) } // 0=Semua, 1, 2, 3
    var selectedOpdFilter by rememberSaveable { mutableStateOf("Semua") }

    val districtShelters = remember(currentDistrict.id, currentDistrict.name) {
        MukomukoRpb2025Catalog.getSheltersForDistrict(currentDistrict)
    }

    val accentPurple = if (isDark) Color(0xFFC084FC) else Color(0xFF6A1B9A)
    val surfaceOpaque = MaterialTheme.colorScheme.surface

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("rpb_2025_2029_section"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header Ringkas Dokumen RPB 2025–2029
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = surfaceOpaque),
            border = BorderStroke(1.5.dp, OrangePrimary),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    RpbBadgePill(
                        text = "DOKUMEN RPB 2025–2029",
                        bgColor = OrangePrimary,
                        textColor = Color.White
                    )
                    RpbBadgePill(
                        text = "31 STRATEGI • 5 TARGET 2029",
                        bgColor = accentPurple.copy(alpha = 0.16f),
                        textColor = accentPurple
                    )
                    RpbBadgePill(
                        text = "11 SHELTER RESMI",
                        bgColor = accentTeal.copy(alpha = 0.16f),
                        textColor = accentTeal
                    )
                }

                Text(
                    text = MukomukoRpb2025Catalog.DOCUMENT_TITLE,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 19.sp
                )

                Text(
                    text = "${MukomukoRpb2025Catalog.DOCUMENT_PUBLISHER} • ${MukomukoRpb2025Catalog.DOCUMENT_OFFICIAL}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 15.sp
                )

                Surface(
                    color = subCardBg,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Tujuan Strategis RPB 2025–2029:",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = accentNavy
                        )
                        Text(
                            text = "\"${MukomukoRpb2025Catalog.GOAL_STATEMENT}\"",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 16.sp
                        )
                        Text(
                            text = MukomukoRpb2025Catalog.LEGAL_STATUS,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 14.sp
                        )
                    }
                }

                // Highlight Shelter di Kecamatan Terpilih
                Surface(
                    color = if (districtShelters.isNotEmpty()) accentTeal.copy(alpha = 0.12f) else subCardBg,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(
                        1.dp,
                        if (districtShelters.isNotEmpty()) accentTeal.copy(alpha = 0.45f) else cardBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = if (districtShelters.isNotEmpty()) accentTeal else accentNavy,
                            modifier = Modifier
                                .padding(top = 1.dp)
                                .size(16.dp)
                        )
                        Text(
                            text = if (districtShelters.isNotEmpty()) {
                                "Titik Penampungan Pengungsi Resmi RPB di Kec. ${currentDistrict.name}: " +
                                    districtShelters.joinToString("; ") { "${it.siteName} (${it.facilityType})" } +
                                    " [Kode 1.06.06.2.01.0007]."
                            } else {
                                "Kec. ${currentDistrict.name} terhubung ke 11 titik penampungan pengungsi resmi kabupaten & jaringan 681,37 Km jalan evakuasi RPB 2025–2029."
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }

        // Navigasi 5 Sub-Bagian Internal RPB 2025–2029
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val sections = listOf(
                0 to "Isu & 5 Target 2029",
                1 to "31 Strategi RPB",
                2 to "Nomenklatur & 11 Shelter",
                3 to "SPM, Sejarah & Fiskal",
                4 to "Kelembagaan & Monev"
            )
            sections.forEach { (idx, label) ->
                FilterChip(
                    selected = selectedSection == idx,
                    onClick = { selectedSection = idx },
                    label = {
                        Text(
                            text = label,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = OrangePrimary.copy(alpha = 0.16f),
                        selectedLabelColor = if (isDark) Color(0xFFFFAB40) else OrangePrimary
                    ),
                    modifier = Modifier.testTag("rpb_section_chip_$idx")
                )
            }
        }

        when (selectedSection) {
            0 -> RpbIssuesAndTargetsContent(
                subCardBg = subCardBg,
                cardBorder = cardBorder,
                accentNavy = accentNavy,
                accentTeal = accentTeal,
                lowRiskGreen = lowRiskGreen
            )

            1 -> RpbStrategiesContent(
                selectedSasaran = selectedSasaranFilter,
                onSelectSasaran = { selectedSasaranFilter = it },
                isDark = isDark,
                subCardBg = subCardBg,
                cardBorder = cardBorder,
                accentNavy = accentNavy,
                accentTeal = accentTeal,
                accentPurple = accentPurple
            )

            2 -> RpbProgramsAndSheltersContent(
                currentDistrict = currentDistrict,
                selectedOpd = selectedOpdFilter,
                onSelectOpd = { selectedOpdFilter = it },
                isDark = isDark,
                subCardBg = subCardBg,
                cardBorder = cardBorder,
                accentNavy = accentNavy,
                accentTeal = accentTeal
            )

            3 -> RpbSpmHistoryAndFiscalContent(
                subCardBg = subCardBg,
                cardBorder = cardBorder,
                accentNavy = accentNavy,
                accentTeal = accentTeal,
                highRiskRed = highRiskRed,
                medRiskAmber = medRiskAmber,
                lowRiskGreen = lowRiskGreen
            )

            4 -> RpbGovernanceAndMonevContent(
                subCardBg = subCardBg,
                cardBorder = cardBorder,
                accentNavy = accentNavy,
                accentTeal = accentTeal,
                accentPurple = accentPurple
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RpbIssuesAndTargetsContent(
    subCardBg: Color,
    cardBorder: Color,
    accentNavy: Color,
    accentTeal: Color,
    lowRiskGreen: Color
) {
    var isLegalExpanded by rememberSaveable { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // 3 Isu Strategis Prioritas (Subbab 3.6)
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, cardBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = null,
                        tint = OrangePrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "3 Isu Strategis Prioritas RPB (Subbab 3.6)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                MukomukoRpb2025Catalog.strategicIssues.forEach { issue ->
                    Surface(
                        color = subCardBg,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, cardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                RpbBadgePill(
                                    text = "ISU ${issue.number}",
                                    bgColor = OrangePrimary,
                                    textColor = Color.White
                                )
                                RpbBadgePill(
                                    text = "SKOR PRIORITAS ${issue.priorityScore}",
                                    bgColor = accentNavy.copy(alpha = 0.14f),
                                    textColor = accentNavy
                                )
                                RpbBadgePill(
                                    text = "SASARAN ${issue.relatedSasaran}",
                                    bgColor = accentTeal.copy(alpha = 0.14f),
                                    textColor = accentTeal
                                )
                            }
                            Text(
                                text = issue.title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 16.sp
                            )
                            Text(
                                text = issue.summary,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }
        }

        // 5 Indikator Dampak 2025–2029 (Tabel 4.1)
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, cardBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Assessment,
                        contentDescription = null,
                        tint = lowRiskGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "5 Indikator Dampak RPBD 2025–2029 (Tabel 4.1)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                MukomukoRpb2025Catalog.impactIndicators.forEach { ind ->
                    Surface(
                        color = subCardBg,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, cardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                RpbBadgePill(
                                    text = "INDIKATOR ${ind.code}",
                                    bgColor = accentNavy,
                                    textColor = Color.White
                                )
                                RpbBadgePill(
                                    text = ind.unit,
                                    bgColor = lowRiskGreen.copy(alpha = 0.15f),
                                    textColor = lowRiskGreen
                                )
                            }
                            Text(
                                text = ind.indicatorName,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = ind.sasaranTitle,
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 14.sp
                            )

                            // Timeline Baseline -> 2025..2029
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                RpbYearTargetChip("Base", ind.baseline, cardBorder, MaterialTheme.colorScheme.onSurface)
                                RpbYearTargetChip("2025", ind.target2025, cardBorder, accentNavy)
                                RpbYearTargetChip("2026", ind.target2026, cardBorder, accentNavy)
                                RpbYearTargetChip("2027", ind.target2027, cardBorder, accentTeal)
                                RpbYearTargetChip("2028", ind.target2028, cardBorder, accentTeal)
                                RpbYearTargetChip("2029", ind.target2029, OrangePrimary, OrangePrimary, isHighlight = true)
                            }

                            if (ind.methodologyNote.isNotBlank()) {
                                Text(
                                    text = ind.methodologyNote,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = OrangePrimary,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Landasan Hukum & Regulasi RPB (Collapsible)
        Card(
            onClick = { isLegalExpanded = !isLegalExpanded },
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(if (isLegalExpanded) 1.5.dp else 1.dp, if (isLegalExpanded) OrangePrimary else cardBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("rpb_toggle_legal_basis")
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Gavel,
                            contentDescription = null,
                            tint = accentTeal,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = "Landasan Hukum & Regulasi RPB 2025–2029",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "7 pilar regulasi nasional & daerah (termasuk Permendagri 18/2025 & Perka BNPB 3/2026)",
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    RpbTogglePill(isExpanded = isLegalExpanded)
                }

                AnimatedVisibility(visible = isLegalExpanded) {
                    Column(
                        modifier = Modifier.padding(top = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        HorizontalDivider(color = cardBorder)
                        MukomukoRpb2025Catalog.legalBasisList.forEach { law ->
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = null,
                                    tint = accentTeal,
                                    modifier = Modifier
                                        .padding(top = 2.dp)
                                        .size(14.dp)
                                )
                                Text(
                                    text = law,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RpbStrategiesContent(
    selectedSasaran: Int,
    onSelectSasaran: (Int) -> Unit,
    isDark: Boolean,
    subCardBg: Color,
    cardBorder: Color,
    accentNavy: Color,
    accentTeal: Color,
    accentPurple: Color
) {
    val filtered = remember(selectedSasaran) {
        if (selectedSasaran == 0) {
            MukomukoRpb2025Catalog.strategies
        } else {
            MukomukoRpb2025Catalog.strategies.filter { it.sasaranNumber == selectedSasaran }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Filter Sasaran 1 / 2 / 3
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                0 to "Semua (31)",
                1 to "Sasaran 1 (5)",
                2 to "Sasaran 2 (14)",
                3 to "Sasaran 3 (12)"
            ).forEach { (id, label) ->
                FilterChip(
                    selected = selectedSasaran == id,
                    onClick = { onSelectSasaran(id) },
                    label = {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = accentNavy.copy(alpha = 0.16f),
                        selectedLabelColor = if (isDark) Color(0xFF93C5FD) else accentNavy
                    ),
                    modifier = Modifier.testTag("rpb_strategy_filter_$id")
                )
            }
        }

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, cardBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ListAlt,
                        contentDescription = null,
                        tint = accentNavy,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Daftar Strategi RPB 2025–2029 (Tabel 4.2 • ${filtered.size} Strategi)",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                filtered.forEach { strat ->
                    val badgeColor = when (strat.sasaranNumber) {
                        1 -> OrangePrimary
                        2 -> accentNavy
                        else -> accentTeal
                    }
                    Surface(
                        color = subCardBg,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, cardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                RpbBadgePill(
                                    text = "STRATEGI ${strat.code}",
                                    bgColor = badgeColor,
                                    textColor = Color.White
                                )
                                RpbBadgePill(
                                    text = strat.focusTag,
                                    bgColor = accentPurple.copy(alpha = 0.14f),
                                    textColor = accentPurple
                                )
                            }
                            Text(
                                text = strat.title,
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RpbProgramsAndSheltersContent(
    currentDistrict: District,
    selectedOpd: String,
    onSelectOpd: (String) -> Unit,
    isDark: Boolean,
    subCardBg: Color,
    cardBorder: Color,
    accentNavy: Color,
    accentTeal: Color
) {
    val opdOptions = listOf("Semua", "BPBD", "DPUPR", "Disdamkarmat", "Dinsos", "DLH", "Lainnya")
    val filteredPrograms = remember(selectedOpd) {
        when (selectedOpd) {
            "Semua" -> MukomukoRpb2025Catalog.prioritySubActivities
            "Lainnya" -> MukomukoRpb2025Catalog.prioritySubActivities.filter {
                it.opdLead !in listOf("BPBD", "DPUPR", "Disdamkarmat", "Dinsos", "DLH")
            }
            else -> MukomukoRpb2025Catalog.prioritySubActivities.filter { it.opdLead == selectedOpd }
        }
    }

    var isSheltersExpanded by rememberSaveable { mutableStateOf(true) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // 11 Lokasi Resmi Tempat Penampungan Pengungsi (Kode 1.06.06.2.01.0007)
        Card(
            onClick = { isSheltersExpanded = !isSheltersExpanded },
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(if (isSheltersExpanded) 1.5.dp else 1.dp, if (isSheltersExpanded) OrangePrimary else cardBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("rpb_toggle_11_shelters")
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.HomeWork,
                            contentDescription = null,
                            tint = accentTeal,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = "11 Lokasi Resmi Penampungan Pengungsi",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Kode 1.06.06.2.01.0007 (BPBD • Tabel 5.1 RPB 2025–2029)",
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    RpbTogglePill(isExpanded = isSheltersExpanded)
                }

                AnimatedVisibility(visible = isSheltersExpanded) {
                    Column(
                        modifier = Modifier.padding(top = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        HorizontalDivider(color = cardBorder)
                        MukomukoRpb2025Catalog.officialEvacuationShelters.forEach { shelter ->
                            val isCurrent = shelter.districtId.equals(currentDistrict.id, ignoreCase = true)
                            Surface(
                                color = if (isCurrent) OrangePrimary.copy(alpha = 0.12f) else subCardBg,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(
                                    width = if (isCurrent) 1.5.dp else 1.dp,
                                    color = if (isCurrent) OrangePrimary else cardBorder
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        RpbBadgePill(
                                            text = "#${shelter.number} • KEC. ${shelter.districtName.uppercase(Locale.getDefault())}",
                                            bgColor = if (isCurrent) OrangePrimary else accentTeal,
                                            textColor = Color.White
                                        )
                                        if (isCurrent) {
                                            RpbBadgePill(
                                                text = "KECAMATAN AKTIF",
                                                bgColor = accentNavy,
                                                textColor = Color.White
                                            )
                                        }
                                    }
                                    Text(
                                        text = shelter.siteName,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = shelter.facilityType,
                                        fontSize = 10.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Filter OPD untuk Matriks Nomenklatur Kepmendagri 900.1-861/2026
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            opdOptions.forEach { opd ->
                FilterChip(
                    selected = selectedOpd == opd,
                    onClick = { onSelectOpd(opd) },
                    label = {
                        Text(
                            text = opd,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = accentTeal.copy(alpha = 0.16f),
                        selectedLabelColor = if (isDark) Color(0xFF80CBC4) else accentTeal
                    ),
                    modifier = Modifier.testTag("rpb_opd_filter_$opd")
                )
            }
        }

        // Matriks Program & Nomenklatur Sub-Kegiatan Prioritas
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, cardBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Matriks Nomenklatur Sub-Kegiatan 2025–2029 (Tabel 5.1 • Kepmendagri 900.1-861/2026)",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 17.sp
                )

                filteredPrograms.forEach { prog ->
                    Surface(
                        color = subCardBg,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, cardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                RpbBadgePill(
                                    text = prog.opdLead,
                                    bgColor = if (prog.opdLead == "BPBD") OrangePrimary else accentNavy,
                                    textColor = Color.White
                                )
                                RpbBadgePill(
                                    text = prog.nomenklaturCode,
                                    bgColor = accentTeal.copy(alpha = 0.15f),
                                    textColor = accentTeal
                                )
                                RpbBadgePill(
                                    text = "SASARAN ${prog.sasaranNumber}",
                                    bgColor = cardBorder,
                                    textColor = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = prog.subActivityName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 16.sp
                            )
                            Text(
                                text = "• Target/Pagu: ${prog.targetAndBudgetHighlight}",
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 14.5.sp
                            )
                            Text(
                                text = "• Sinkronisasi SIPAGAR KITA: ${prog.sipagarSyncNote}",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = accentNavy,
                                lineHeight = 14.5.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RpbSpmHistoryAndFiscalContent(
    subCardBg: Color,
    cardBorder: Color,
    accentNavy: Color,
    accentTeal: Color,
    highRiskRed: Color,
    medRiskAmber: Color,
    lowRiskGreen: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // 1. Capaian SPM Sub-Urusan Bencana (Subbab 3.4.1)
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, cardBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = OrangePrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Kinerja SPM Sub-Urusan Bencana (Permendagri 101/2018)",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                MukomukoRpb2025Catalog.spmAchievements.forEach { spm ->
                    val barColor = when {
                        spm.isCriticalGap -> highRiskRed
                        spm.latestPercent >= 90.0 -> lowRiskGreen
                        else -> medRiskAmber
                    }
                    Surface(
                        color = subCardBg,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(
                            width = if (spm.isCriticalGap) 1.2.dp else 1.dp,
                            color = if (spm.isCriticalGap) highRiskRed.copy(alpha = 0.55f) else cardBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                RpbBadgePill(
                                    text = spm.spmCategory,
                                    bgColor = accentNavy.copy(alpha = 0.14f),
                                    textColor = accentNavy
                                )
                                RpbBadgePill(
                                    text = "${String.format(Locale.GERMANY, "%.2f", spm.latestPercent)}% (${spm.latestYear})",
                                    bgColor = barColor,
                                    textColor = Color.White
                                )
                                if (spm.isCriticalGap) {
                                    RpbBadgePill(
                                        text = "PRIORITAS INTERVENSI",
                                        bgColor = highRiskRed.copy(alpha = 0.14f),
                                        textColor = highRiskRed
                                    )
                                }
                            }
                            Text(
                                text = spm.serviceName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            LinearProgressIndicator(
                                progress = { (spm.latestPercent / 100.0).toFloat().coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp),
                                color = barColor,
                                trackColor = cardBorder
                            )
                            Text(
                                text = spm.previousTrend,
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
        }

        // 2. Analisis Pendanaan Kebencanaan BPKAD 2020–2025 (Tabel 3.9)
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.5.dp, OrangePrimary),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = null,
                        tint = OrangePrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Analisis Fiskal Kebencanaan BPKAD 2020–2025 (Tabel 3.9)",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = MukomukoRpb2025Catalog.FISCAL_SUMMARY_NOTE,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 15.5.sp
                )

                MukomukoRpb2025Catalog.fiscalHistory.forEach { fy ->
                    Surface(
                        color = subCardBg,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, cardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                            ) {
                                Text(
                                    text = "Tahun ${fy.year} • Fiskal ${fy.fiscalCapacityClass}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Belanja Bencana: Rp ${String.format(Locale.GERMANY, "%.2f", fy.disasterSpendingBillionRp)} M" +
                                        if (fy.bttMillionRp > 0) " • BTT Rp ${fy.bttMillionRp.toInt()} Jt" else "",
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            RpbBadgePill(
                                text = "${String.format(Locale.GERMANY, "%.2f", fy.apbdSharePercent)}% APBD",
                                bgColor = if (fy.year == 2025) OrangePrimary else accentNavy.copy(alpha = 0.14f),
                                textColor = if (fy.year == 2025) Color.White else accentNavy
                            )
                        }
                    }
                }
            }
        }

        // 3. Rekapitulasi Sejarah 184 Bencana (2014–2024) & Tren IRBI/IKD 2021–2025
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, cardBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = highRiskRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Sejarah ${MukomukoRpb2025Catalog.HISTORICAL_TOTAL_EVENTS} Kejadian Bencana 2014–2024 (Tabel 3.10–3.12)",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "• Dampak Sosial: ${MukomukoRpb2025Catalog.HISTORICAL_SOCIAL_IMPACT}\n• Dampak Fisik & Kesehatan: ${MukomukoRpb2025Catalog.HISTORICAL_PHYSICAL_IMPACT}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 15.5.sp
                )

                MukomukoRpb2025Catalog.historicalDisasters.forEach { stat ->
                    Surface(
                        color = subCardBg,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, cardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                            ) {
                                Text(
                                    text = stat.hazardName,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stat.highlightNote,
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 14.sp
                                )
                            }
                            RpbBadgePill(
                                text = "${stat.eventCount} Kejadian",
                                bgColor = accentTeal.copy(alpha = 0.16f),
                                textColor = accentTeal
                            )
                        }
                    }
                }

                HorizontalDivider(color = cardBorder, modifier = Modifier.padding(vertical = 4.dp))

                Text(
                    text = "Tren Resmi IRBI & IKD 2021–2025 (Gambar 3.15–3.16 RPB)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = accentNavy
                )
                MukomukoRpb2025Catalog.irbiIkdTrend.forEach { yr ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 6.dp)
                        ) {
                            Text(
                                text = "${yr.year}: IRBI ${String.format(Locale.GERMANY, "%.2f", yr.irbiScore)} • IKD ${String.format(Locale.GERMANY, "%.2f", yr.ikdScore)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (yr.year == 2025) OrangePrimary else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = yr.note,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RpbGovernanceAndMonevContent(
    subCardBg: Color,
    cardBorder: Color,
    accentNavy: Color,
    accentTeal: Color,
    accentPurple: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Kerangka Kelembagaan Multipihak (Hexahelix & GEDSI)
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, cardBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Groups,
                        contentDescription = null,
                        tint = accentPurple,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Kelembagaan Multipihak Hexahelix & Inklusi GEDSI (Bab VI)",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    RpbBadgePill("HEXAHELIX", accentPurple, Color.White)
                    RpbBadgePill("INKLUSI GEDSI", accentTeal, Color.White)
                    RpbBadgePill("PRIORITAS: BENTUK FPRB", OrangePrimary, Color.White)
                }

                Text(
                    text = MukomukoRpb2025Catalog.INSTITUTIONAL_NOTE,
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 16.sp
                )
            }
        }

        // Kerangka Pemantauan & Evaluasi (Tabel 6.1)
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, cardBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = accentTeal,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Kerangka Pemantauan & Evaluasi RPBD (Tabel 6.1)",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                MukomukoRpb2025Catalog.monevPhases.forEach { phase ->
                    Surface(
                        color = subCardBg,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, cardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                RpbBadgePill(
                                    text = phase.phaseName.uppercase(Locale.getDefault()),
                                    bgColor = accentNavy,
                                    textColor = Color.White
                                )
                                RpbBadgePill(
                                    text = phase.frequency,
                                    bgColor = accentTeal.copy(alpha = 0.16f),
                                    textColor = accentTeal
                                )
                            }
                            Text(
                                text = "• Pelaksana: ${phase.actors}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 15.sp
                            )
                            Text(
                                text = "• Keluaran: ${phase.outputDeliverable}",
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RpbBadgePill(
    text: String,
    bgColor: Color,
    textColor: Color
) {
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = text,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.ExtraBold,
            color = textColor,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun RpbYearTargetChip(
    yearLabel: String,
    value: String,
    borderColor: Color,
    valueColor: Color,
    isHighlight: Boolean = false
) {
    Surface(
        color = if (isHighlight) OrangePrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(7.dp),
        border = BorderStroke(if (isHighlight) 1.2.dp else 1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = yearLabel,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = value,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = valueColor,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun RpbTogglePill(isExpanded: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Surface(
            color = OrangePrimary.copy(alpha = 0.12f),
            shape = RoundedCornerShape(6.dp)
        ) {
            Text(
                text = if (isExpanded) "Tutup" else "Buka",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = OrangePrimary,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
            )
        }
        Icon(
            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = if (isExpanded) "Tutup" else "Buka",
            tint = OrangePrimary,
            modifier = Modifier.size(18.dp)
        )
    }
}
