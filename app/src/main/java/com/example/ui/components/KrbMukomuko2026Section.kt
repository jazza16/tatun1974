package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Water
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.District
import com.example.model.KrbDistrictRiskProfile
import com.example.model.MukomukoKrb2026Catalog
import com.example.model.MukomukoRtrw2025Catalog
import com.example.model.RtrwDistrictSpatialProfile
import java.util.Locale

@Composable
fun KrbMukomuko2026Section(
    currentDistrict: District,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    // 0 = Profil KRB & RTRW Kecamatan, 1 = Matriks 12 Bencana KRB, 2 = 7 Fokus IKD & IRBI,
    // 3 = Perda RTRW No. 12/2025 Pasal 92 & 93 (KRB & Sempadan), 4 = Pola Ruang, KDB/KDH, Program BPBD & Sanksi KKPR
    var selectedSubTab by remember { mutableIntStateOf(0) }
    var selectedPriorityFilter by remember { mutableIntStateOf(0) } // 0 = Semua, 1 = Prioritas 1, 2 = Prioritas 2, 3 = Prioritas 3
    var expandedHazardNumber by remember { mutableIntStateOf(1) }
    var expandedRtrwRuleId by remember { mutableStateOf("krb_tsunami") }
    var selectedPatternFilter by remember { mutableStateOf("Semua") }

    val districtProfile = remember(currentDistrict.id, currentDistrict.name) {
        MukomukoKrb2026Catalog.getDistrictProfile(currentDistrict)
    }
    val districtRtrwProfile = remember(currentDistrict.id, currentDistrict.name) {
        MukomukoRtrw2025Catalog.getDistrictSpatialProfile(currentDistrict)
    }

    val cardBg = if (isDark) Color(0xFF131F33) else Color(0xFFFFFFFF)
    val cardBorder = if (isDark) Color(0xFF283B5B) else Color(0xFFDCE4F2)
    val subCardBg = if (isDark) Color(0xFF1A2942) else Color(0xFFF5F8FD)
    val accentOrange = Color(0xFFE65100)
    val accentNavy = if (isDark) Color(0xFF60A5FA) else Color(0xFF0D47A1)
    val accentTeal = if (isDark) Color(0xFF4DB6AC) else Color(0xFF00695C)
    val highRiskRed = Color(0xFFD32F2F)
    val medRiskAmber = Color(0xFFF57C00)
    val lowRiskGreen = Color(0xFF2E7D32)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("krb_2026_2030_section_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.5.dp, cardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Banner Resmi Sinergi Dokumen KRB 2026-2030 & Perda RTRW No. 12 Tahun 2025 (2025-2045)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF0F2547),
                                Color(0xFF1B3A6B),
                                Color(0xFF004D40),
                                Color(0xFFB71C1C)
                            )
                        )
                    )
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = Color(0xFFFF6D00),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = "Dokumen Resmi KRB",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "DOKUMEN KRB 2026–2030",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }

                        Surface(
                            color = Color(0xFF00897B),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Gavel,
                                    contentDescription = "Perda RTRW No. 12 Tahun 2025",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "PERDA RTRW NO. 12 TAHUN 2025 (2025–2045)",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }

                    Text(
                        text = "Integrasi Kajian Risiko Bencana (KRB 2026–2030) & Perda RTRW Kab. Mukomuko No. 12 Tahun 2025",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 20.sp
                    )

                    Text(
                        text = "${MukomukoKrb2026Catalog.DOCUMENT_PUBLISHER} (${MukomukoKrb2026Catalog.DOCUMENT_RESPONSIBLE}) • ${MukomukoRtrw2025Catalog.ENACTMENT_INFO}",
                        color = Color.White.copy(alpha = 0.88f),
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )

                    // 4 Indikator Makro KRB & RTRW Kabupaten Mukomuko
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        KrbMacroMetricPill(
                            label = "IRBI & IKD 2025",
                            value = "164,32 / 0,29",
                            badge = "Tinggi / Rendah",
                            modifier = Modifier.weight(1f)
                        )
                        KrbMacroMetricPill(
                            label = "Luas RTRW 2025",
                            value = "413.752 Ha",
                            badge = "Pola: 413.866 Ha",
                            modifier = Modifier.weight(1f)
                        )
                        KrbMacroMetricPill(
                            label = "Jiwa Terpapar",
                            value = "205.602",
                            badge = "15 Kec • 151 Desa",
                            modifier = Modifier.weight(1f)
                        )
                        KrbMacroMetricPill(
                            label = "Potensi Rugi",
                            value = "Rp 6,74 T",
                            badge = "12 Ancaman KRB",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Sub-Tab Selector (5 Tab Terpadu KRB 2026-2030 & Perda RTRW No. 12/2025)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedSubTab == 0,
                    onClick = { selectedSubTab = 0 },
                    label = {
                        Text(
                            text = "Profil KRB & RTRW Kec. ${districtProfile.districtName}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.LocationCity,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = accentOrange.copy(alpha = 0.16f),
                        selectedLabelColor = if (isDark) Color(0xFFFFAB40) else accentOrange,
                        selectedLeadingIconColor = if (isDark) Color(0xFFFFAB40) else accentOrange
                    ),
                    modifier = Modifier.testTag("krb_subtab_district_profile")
                )

                FilterChip(
                    selected = selectedSubTab == 1,
                    onClick = { selectedSubTab = 1 },
                    label = {
                        Text(
                            text = "Matriks 12 Bencana KRB",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Assessment,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = accentNavy.copy(alpha = 0.16f),
                        selectedLabelColor = accentNavy,
                        selectedLeadingIconColor = accentNavy
                    ),
                    modifier = Modifier.testTag("krb_subtab_12_hazards")
                )

                FilterChip(
                    selected = selectedSubTab == 2,
                    onClick = { selectedSubTab = 2 },
                    label = {
                        Text(
                            text = "7 Fokus IKD & Tren IRBI",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = lowRiskGreen.copy(alpha = 0.16f),
                        selectedLabelColor = if (isDark) Color(0xFF81C784) else lowRiskGreen,
                        selectedLeadingIconColor = if (isDark) Color(0xFF81C784) else lowRiskGreen
                    ),
                    modifier = Modifier.testTag("krb_subtab_ikd_irbi")
                )

                FilterChip(
                    selected = selectedSubTab == 3,
                    onClick = { selectedSubTab = 3 },
                    label = {
                        Text(
                            text = "Perda RTRW 12/2025: Pasal 92 & 93 KRB",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Gavel,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = accentTeal.copy(alpha = 0.18f),
                        selectedLabelColor = accentTeal,
                        selectedLeadingIconColor = accentTeal
                    ),
                    modifier = Modifier.testTag("krb_subtab_rtrw_pasal92_93")
                )

                FilterChip(
                    selected = selectedSubTab == 4,
                    onClick = { selectedSubTab = 4 },
                    label = {
                        Text(
                            text = "Pola Ruang, KDB/KDH, Program BPBD & Sanksi KKPR",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = accentNavy.copy(alpha = 0.16f),
                        selectedLabelColor = accentNavy,
                        selectedLeadingIconColor = accentNavy
                    ),
                    modifier = Modifier.testTag("krb_subtab_rtrw_patterns_bpbd")
                )
            }

            HorizontalDivider(color = cardBorder)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (selectedSubTab) {
                    0 -> KrbDistrictProfileContent(
                        profile = districtProfile,
                        rtrwProfile = districtRtrwProfile,
                        isDark = isDark,
                        subCardBg = subCardBg,
                        cardBorder = cardBorder,
                        accentTeal = accentTeal,
                        highRiskRed = highRiskRed,
                        medRiskAmber = medRiskAmber,
                        lowRiskGreen = lowRiskGreen
                    )

                    1 -> KrbTwelveHazardsMatrixContent(
                        selectedPriorityFilter = selectedPriorityFilter,
                        onSelectPriorityFilter = { selectedPriorityFilter = it },
                        expandedHazardNumber = expandedHazardNumber,
                        onToggleExpand = { num ->
                            expandedHazardNumber = if (expandedHazardNumber == num) -1 else num
                        },
                        isDark = isDark,
                        subCardBg = subCardBg,
                        cardBorder = cardBorder,
                        highRiskRed = highRiskRed,
                        medRiskAmber = medRiskAmber,
                        lowRiskGreen = lowRiskGreen
                    )

                    2 -> KrbIkdAndIrbiContent(
                        isDark = isDark,
                        subCardBg = subCardBg,
                        cardBorder = cardBorder,
                        accentNavy = accentNavy,
                        highRiskRed = highRiskRed,
                        lowRiskGreen = lowRiskGreen
                    )

                    3 -> RtrwPasal92And93Content(
                        expandedRuleId = expandedRtrwRuleId,
                        onToggleRule = { id ->
                            expandedRtrwRuleId = if (expandedRtrwRuleId == id) "" else id
                        },
                        isDark = isDark,
                        subCardBg = subCardBg,
                        cardBorder = cardBorder,
                        accentTeal = accentTeal,
                        highRiskRed = highRiskRed,
                        medRiskAmber = medRiskAmber,
                        lowRiskGreen = lowRiskGreen
                    )

                    4 -> RtrwPatternsBpbdAndSanctionsContent(
                        selectedPatternFilter = selectedPatternFilter,
                        onSelectPatternFilter = { selectedPatternFilter = it },
                        isDark = isDark,
                        subCardBg = subCardBg,
                        cardBorder = cardBorder,
                        accentNavy = accentNavy,
                        accentTeal = accentTeal,
                        highRiskRed = highRiskRed,
                        lowRiskGreen = lowRiskGreen
                    )
                }
            }
        }
    }
}

@Composable
private fun KrbMacroMetricPill(
    label: String,
    value: String,
    badge: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.13f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = badge,
                color = Color(0xFFFFD54F),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun KrbDistrictProfileContent(
    profile: KrbDistrictRiskProfile,
    rtrwProfile: RtrwDistrictSpatialProfile,
    isDark: Boolean,
    subCardBg: Color,
    cardBorder: Color,
    accentTeal: Color,
    highRiskRed: Color,
    medRiskAmber: Color,
    lowRiskGreen: Color
) {
    // 1. Identitas Wilayah & Status Risiko Multibahaya Kecamatan
    Surface(
        color = subCardBg,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "KECAMATAN ${profile.districtName.uppercase(Locale.getDefault())}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    val adminDesc = if (profile.wardsCount > 0) {
                        "Ibu Kota: ${profile.capitalName} • ${profile.villagesCount} Desa & ${profile.wardsCount} Kelurahan"
                    } else {
                        "Ibu Kota: ${profile.capitalName} • ${profile.villagesCount} Desa"
                    }
                    Text(
                        text = "$adminDesc • Luas: ${formatIdNumber(profile.areaHa)} Ha",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = highRiskRed,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "RISIKO ${profile.multiHazardRiskClass.uppercase(Locale.getDefault())}",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            // Badges Zona Khusus (Tsunami Pesisir & Banjir Bandang)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = accentTeal.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, accentTeal.copy(alpha = 0.45f))
                ) {
                    Text(
                        text = "${rtrwProfile.settlementHierarchyCode}: ${rtrwProfile.urbanOrRuralCenterName} (${rtrwProfile.article10Reference})",
                        color = accentTeal,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                if (profile.isCoastalTsunamiZone) {
                    Surface(
                        color = Color(0xFFD32F2F).copy(alpha = 0.14f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFD32F2F).copy(alpha = 0.45f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Water,
                                contentDescription = null,
                                tint = Color(0xFFD32F2F),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Zona Tsunami Pesisir (KRB & Pasal 92 ayat 8)",
                                color = if (isDark) Color(0xFFFF8A80) else Color(0xFFB71C1C),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (profile.isFlashFloodZone) {
                    Surface(
                        color = Color(0xFFE65100).copy(alpha = 0.14f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFE65100).copy(alpha = 0.45f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFE65100),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Zona Banjir Bandang (KRB & Pasal 92 ayat 3)",
                                color = if (isDark) Color(0xFFFFB74D) else Color(0xFFE65100),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Grid 2x2 Kerentanan Sosial & Demografi Terpapar (Tabel 3.107)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                KrbDetailStatBox(
                    title = "Penduduk Terpapar",
                    value = "${formatIdInt(profile.exposedPopulation)} Jiwa",
                    subtitle = "Umur Rentan: ${formatIdInt(profile.vulnerableAgePopulation)} jiwa",
                    accentColor = Color(0xFF1565C0),
                    modifier = Modifier.weight(1f)
                )
                KrbDetailStatBox(
                    title = "Kelompok Rentan P3KE",
                    value = "${formatIdInt(profile.poorPopulationP3KE)} Miskin",
                    subtitle = "Disabilitas: ${profile.disabilityPopulation} jiwa • ${profile.healthFacilitiesCount} Faskes",
                    accentColor = medRiskAmber,
                    modifier = Modifier.weight(1f)
                )
            }

            // Grid 2x2 Kapasitas & Potensi Kerugian Multibahaya (Tabel 3.108 & 3.110)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val capColor = if (profile.multiHazardCapacityClass == "Sedang") lowRiskGreen else highRiskRed
                KrbDetailStatBox(
                    title = "Indeks Kapasitas (Tabel 3.110)",
                    value = "${String.format(Locale.US, "%.3f", profile.multiHazardCapacityIndex)} (${profile.multiHazardCapacityClass})",
                    subtitle = "IKM: ${String.format(Locale.US, "%.3f", profile.ikmScore)} • IKD_T: ${String.format(Locale.US, "%.3f", profile.ikdTransformed)}",
                    accentColor = capColor,
                    modifier = Modifier.weight(1f)
                )
                KrbDetailStatBox(
                    title = "Potensi Rugi Multibahaya",
                    value = formatRupiahMillions(profile.totalLossMillionRp),
                    subtitle = "Fisik: ${formatRupiahMillions(profile.physicalLossMillionRp)} • Ekon: ${formatRupiahMillions(profile.economicLossMillionRp)}",
                    accentColor = highRiskRed,
                    modifier = Modifier.weight(1f)
                )
            }

            // Distribusi Luas Risiko Multibahaya & Kerusakan Lingkungan (Tabel 3.108 & 3.111)
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, cardBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Distribusi Luas Risiko Multibahaya (Tabel 3.111)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Lingkungan Terdampak: ${formatIdNumber(profile.envDamageHa)} Ha",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (profile.envDamageHa > 1000.0) highRiskRed else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "• Risiko Tinggi: ${formatIdNumber(profile.riskHighAreaHa)} Ha  |  • Risiko Sedang: ${formatIdNumber(profile.riskMediumAreaHa)} Ha  |  • Risiko Rendah: ${formatIdNumber(profile.riskLowAreaHa)} Ha",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // 2. Profil Ancaman Utama Spesifik Kecamatan (KRB 2026-2030)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Profil Ancaman Bencana Dominan di Kec. ${profile.districtName} (KRB 2026–2030)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.ExtraBold
        )

        profile.primaryHazardBadges.forEach { badge ->
            val badgeColor = when (badge.riskLevel) {
                "Tinggi" -> highRiskRed
                "Sedang" -> medRiskAmber
                else -> lowRiskGreen
            }
            Surface(
                color = subCardBg,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.38f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = badge.hazardName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = badge.detailMetric,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        color = badgeColor.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Risiko ${badge.riskLevel}",
                            color = badgeColor,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }

    // 3. Profil Tata Ruang & Zonasi Kecamatan menurut Perda RTRW No. 12 Tahun 2025 (2025–2045)
    Surface(
        color = subCardBg,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.2.dp, accentTeal.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("rtrw_district_spatial_profile_card")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
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
                            text = "Profil Tata Ruang Kec. ${rtrwProfile.districtName} (Perda RTRW No. 12/2025)",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            color = accentTeal
                        )
                        Text(
                            text = "${rtrwProfile.settlementHierarchyTitle} • ${rtrwProfile.urbanOrRuralCenterName} (${rtrwProfile.article10Reference})",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Surface(
                    color = accentTeal,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = rtrwProfile.settlementHierarchyCode,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Status Kawasan Strategis Kabupaten (Pasal 44-45)
            Surface(
                color = accentTeal.copy(alpha = 0.1f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, accentTeal.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Status Kawasan Strategis Kabupaten / KSK (Pasal 44–45):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = accentTeal
                    )
                    Text(
                        text = rtrwProfile.strategicAreaStatus,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }

            // Kawasan Lindung & Kawasan Budi Daya
            RtrwBulletGroupBox(
                title = "Alokasi Kawasan Lindung (Pasal 30–35):",
                items = rtrwProfile.protectedZones,
                bulletColor = lowRiskGreen
            )
            RtrwBulletGroupBox(
                title = "Alokasi Kawasan Budi Daya Unggulan (Pasal 36–43):",
                items = rtrwProfile.cultivationZones,
                bulletColor = Color(0xFF1565C0)
            )
            RtrwBulletGroupBox(
                title = "Sistem Jaringan Prasarana & Mitigasi Bencana (Pasal 13–28):",
                items = rtrwProfile.vitalInfrastructureAndMitigation,
                bulletColor = medRiskAmber
            )
            RtrwBulletGroupBox(
                title = "Ketentuan Khusus KRB & Sempadan Berlaku (Pasal 92 & Pasal 93):",
                items = rtrwProfile.applicablePasal92And93Rules,
                bulletColor = highRiskRed
            )

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, cardBorder)
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = "Arahan Pengendalian Pemanfaatan Ruang & KKPR Kecamatan:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = rtrwProfile.spatialControlDirective,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }

    // 4. Rekomendasi Strategis KRB 2026-2030 untuk Kecamatan Terpilih
    Surface(
        color = Color(0xFF0D47A1).copy(alpha = if (isDark) 0.22f else 0.08f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(0xFF1976D2).copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = if (isDark) Color(0xFF64B5F6) else Color(0xFF0D47A1),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Rekomendasi Fokus Penanganan KRB 2026–2030 (${profile.districtName})",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    color = if (isDark) Color(0xFF90CAF9) else Color(0xFF0D47A1)
                )
            }
            Text(
                text = profile.recommendationSummary,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
        }
    }
}

@Composable
private fun RtrwBulletGroupBox(
    title: String,
    items: List<String>,
    bulletColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            color = bulletColor
        )
        items.forEach { item ->
            Text(
                text = "• $item",
                fontSize = 11.sp,
                lineHeight = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun KrbDetailStatBox(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.32f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 13.sp
            )
        }
    }
}

@Composable
private fun KrbTwelveHazardsMatrixContent(
    selectedPriorityFilter: Int,
    onSelectPriorityFilter: (Int) -> Unit,
    expandedHazardNumber: Int,
    onToggleExpand: (Int) -> Unit,
    isDark: Boolean,
    subCardBg: Color,
    cardBorder: Color,
    highRiskRed: Color,
    medRiskAmber: Color,
    lowRiskGreen: Color
) {
    val filteredHazards = remember(selectedPriorityFilter) {
        if (selectedPriorityFilter == 0) {
            MukomukoKrb2026Catalog.twelveHazardsMatrix
        } else {
            MukomukoKrb2026Catalog.twelveHazardsMatrix.filter { it.priorityLevel == selectedPriorityFilter }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Matriks 12 Ancaman Bencana & Prioritas Penanganan (Tabel 3.105 & 3.113)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.ExtraBold
        )

        // Filter Chips Prioritas 1 / 2 / 3
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filters = listOf(
                0 to "Semua (12 Bencana)",
                1 to "Prioritas 1 (8 Bencana Utama)",
                2 to "Prioritas 2 (Likuefaksi & KLB)",
                3 to "Prioritas 3 (Gempa & Kegagalan Tek.)"
            )
            filters.forEach { (prio, label) ->
                FilterChip(
                    selected = selectedPriorityFilter == prio,
                    onClick = { onSelectPriorityFilter(prio) },
                    label = {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
            }
        }

        filteredHazards.forEach { hazard ->
            val isExpanded = expandedHazardNumber == hazard.number
            val riskColor = when (hazard.riskClass) {
                "Tinggi" -> highRiskRed
                "Sedang" -> medRiskAmber
                else -> lowRiskGreen
            }
            val prioColor = when (hazard.priorityLevel) {
                1 -> highRiskRed
                2 -> medRiskAmber
                else -> Color(0xFF1565C0)
            }

            Surface(
                color = subCardBg,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, if (isExpanded) riskColor else cardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand(hazard.number) }
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                color = prioColor,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "P${hazard.priorityLevel}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "${hazard.number}. ${hazard.hazardName}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Luas Bahaya: ${formatIdNumber(hazard.hazardAreaHa)} Ha (${hazard.affectedDistrictsCount} Kec) • Tren: ${hazard.historicalTrend}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                color = riskColor.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Risiko ${hazard.riskClass}",
                                    color = riskColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Detail",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Ringkasan Cepat 4 Pilar KRB
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        KrbMiniTag("Bahaya: ${hazard.hazardClass}", Modifier.weight(1f))
                        KrbMiniTag("Kerentanan: ${hazard.vulnerabilityClass}", Modifier.weight(1f))
                        KrbMiniTag(
                            "Kapasitas: ${String.format(Locale.US, "%.3f", hazard.capacityIndex)} (${hazard.capacityClass})",
                            Modifier.weight(1.3f)
                        )
                    }

                    AnimatedVisibility(visible = isExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            HorizontalDivider(color = cardBorder)
                            Text(
                                text = "• Jiwa Terpapar: ${formatIdInt(hazard.exposedPopulation)} jiwa  |  • Potensi Kerugian: ${formatRupiahMillions(hazard.totalLossMillionRp)}  |  • Lingkungan: ${formatIdNumber(hazard.envDamageHa)} Ha",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Temuan Kunci KRB: ${hazard.keyHighlight}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Mitigasi Struktural: ${hazard.structuralMitigation}",
                                fontSize = 11.sp,
                                color = if (isDark) Color(0xFF90CAF9) else Color(0xFF0D47A1)
                            )
                            Text(
                                text = "Mitigasi Non-Struktural: ${hazard.nonStructuralMitigation}",
                                fontSize = 11.sp,
                                color = if (isDark) Color(0xFFA5D6A7) else Color(0xFF1B5E20)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KrbMiniTag(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun KrbIkdAndIrbiContent(
    isDark: Boolean,
    subCardBg: Color,
    cardBorder: Color,
    accentNavy: Color,
    highRiskRed: Color,
    lowRiskGreen: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // 1. Kartu 7 Fokus Prioritas IKD 2025 (Tabel 3.102)
        Surface(
            color = subCardBg,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "7 Fokus Prioritas Indeks Ketahanan Daerah (IKD 2025)",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Tabel 3.102 KRB 2026–2030 • Skor IKD = 0,29 (IKD_T = 0,242 • Kelas Rendah)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        color = highRiskRed.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "IKD: 0,29",
                            color = highRiskRed,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                MukomukoKrb2026Catalog.ikdPriorities.forEach { item ->
                    val barColor = when {
                        item.focusNumber == 3 -> lowRiskGreen // Tertinggi 0.46
                        item.focusNumber == 4 || item.focusNumber == 5 -> highRiskRed // Terendah 0.20 & 0.22
                        else -> accentNavy
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Fokus ${item.focusNumber}: ${item.focusTitle}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${String.format(Locale.US, "%.2f", item.score)} (${item.categoryLabel})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = barColor
                            )
                        }
                        LinearProgressIndicator(
                            progress = { item.score.toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = barColor,
                            trackColor = barColor.copy(alpha = 0.18f)
                        )
                        Text(
                            text = "Catatan KRB: ${item.strategicNote}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }

        // 2. Kartu Tren Historis IRBI Kabupaten Mukomuko 2015-2025 (Gambar 1.1)
        Surface(
            color = subCardBg,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Tren Indeks Risiko Bencana Indonesia (IRBI) 2015–2025",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Gambar 1.1 KRB 2026–2030 • Turun dari 191,20 (2015) ke 164,32 (2025)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        color = lowRiskGreen.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingDown,
                                contentDescription = null,
                                tint = lowRiskGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "-26,88 Poin",
                                color = lowRiskGreen,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MukomukoKrb2026Catalog.irbiHistoricalTrend.forEach { (year, score) ->
                        val isLatest = year == "2025"
                        Surface(
                            color = if (isLatest) highRiskRed.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (isLatest) highRiskRed else cardBorder)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = year,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = String.format(Locale.GERMANY, "%.2f", score),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isLatest) highRiskRed else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Ringkasan Tren Kejadian Historis 2015-2024 (Tabel 4.1)
                HorizontalDivider(color = cardBorder)
                Text(
                    text = "Tren Historis Kejadian Bencana 2015–2024 (Tabel 4.1):",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    KrbTrendBadge(
                        icon = Icons.Default.TrendingUp,
                        label = "Meningkat: Banjir, Longsor, Cuaca Ekstrem, Abrasi",
                        color = highRiskRed,
                        modifier = Modifier.weight(1f)
                    )
                    KrbTrendBadge(
                        icon = Icons.Default.TrendingFlat,
                        label = "Stabil: Karhutla & Kekeringan",
                        color = Color(0xFFF57C00),
                        modifier = Modifier.weight(0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun RtrwPasal92And93Content(
    expandedRuleId: String,
    onToggleRule: (String) -> Unit,
    isDark: Boolean,
    subCardBg: Color,
    cardBorder: Color,
    accentTeal: Color,
    highRiskRed: Color,
    medRiskAmber: Color,
    lowRiskGreen: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(
            color = accentTeal.copy(alpha = if (isDark) 0.2f else 0.1f),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, accentTeal.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "${MukomukoRtrw2025Catalog.PERDA_TITLE} • Pasal 92 & Pasal 93",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    color = accentTeal
                )
                Text(
                    text = "Ketentuan Khusus Kawasan Rawan Bencana (8 Ancaman KRB pada Pasal 92 ayat 1–9) & Kawasan Sempadan Pantai, Sungai, dan Danau/Waduk (Pasal 93 ayat 1–4). Ketuk setiap pasal untuk melihat kegiatan yang Diperbolehkan, Bersyarat, dan Dilarang.",
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }

        MukomukoRtrw2025Catalog.specialDisasterAndBufferRules.forEach { rule ->
            val isExpanded = expandedRuleId == rule.id
            Surface(
                color = subCardBg,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.2.dp, if (isExpanded) accentTeal else cardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleRule(rule.id) }
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    color = accentTeal,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = rule.articleReference,
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                    )
                                }
                                Text(
                                    text = rule.zoneTitle,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp
                                )
                            }
                            Text(
                                text = rule.krbAlignmentNote,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 3.dp)
                            )
                        }
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Buka Ketentuan Pasal",
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    AnimatedVisibility(visible = isExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            HorizontalDivider(color = cardBorder)

                            // 1. Kegiatan Diperbolehkan (Hijau)
                            Surface(
                                color = lowRiskGreen.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, lowRiskGreen.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = lowRiskGreen,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "Kegiatan Diperbolehkan (Huruf a):",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = lowRiskGreen
                                        )
                                    }
                                    rule.allowedActivities.forEach { act ->
                                        Text(
                                            text = "• $act",
                                            fontSize = 11.sp,
                                            lineHeight = 15.sp
                                        )
                                    }
                                }
                            }

                            // 2. Kegiatan Diperbolehkan dengan Syarat (Kuning/Oranye)
                            Surface(
                                color = medRiskAmber.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, medRiskAmber.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = medRiskAmber,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "Diperbolehkan dengan Syarat Wajib Mitigasi (Huruf b):",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = medRiskAmber
                                        )
                                    }
                                    rule.conditionalActivities.forEach { act ->
                                        Text(
                                            text = "• $act",
                                            fontSize = 11.sp,
                                            lineHeight = 15.sp
                                        )
                                    }
                                }
                            }

                            // 3. Kegiatan Tidak Diperbolehkan / Larangan (Merah)
                            Surface(
                                color = highRiskRed.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, highRiskRed.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Gavel,
                                            contentDescription = null,
                                            tint = highRiskRed,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "Kegiatan Tidak Diperbolehkan / Dilarang (Huruf c):",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = highRiskRed
                                        )
                                    }
                                    rule.prohibitedActivities.forEach { act ->
                                        Text(
                                            text = "• $act",
                                            fontSize = 11.sp,
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
    }
}

@Composable
private fun RtrwPatternsBpbdAndSanctionsContent(
    selectedPatternFilter: String,
    onSelectPatternFilter: (String) -> Unit,
    isDark: Boolean,
    subCardBg: Color,
    cardBorder: Color,
    accentNavy: Color,
    accentTeal: Color,
    highRiskRed: Color,
    lowRiskGreen: Color
) {
    val filteredPatterns = remember(selectedPatternFilter) {
        if (selectedPatternFilter == "Semua") {
            MukomukoRtrw2025Catalog.spatialPatternSummaries
        } else {
            MukomukoRtrw2025Catalog.spatialPatternSummaries.filter { it.category == selectedPatternFilter }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // 1. Tujuan Penataan Ruang (Pasal 4) & 3 Kawasan Strategis Kabupaten (Pasal 44-45)
        Surface(
            color = subCardBg,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Tujuan Penataan Ruang (Pasal 4) & 3 Kawasan Strategis Kabupaten (Pasal 44–45)",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    color = accentTeal
                )
                Text(
                    text = "\"${MukomukoRtrw2025Catalog.RTRW_VISION_PASAL_4}\"",
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
                HorizontalDivider(color = cardBorder)
                Text(
                    text = "• KSK 1 (Pertumbuhan Ekonomi): Kawasan Agropolitan Lubuk Pinang – Air Manjunto (Pasal 44 ayat 5a).\n" +
                        "• KSK 2 (Pertumbuhan Ekonomi): Kawasan Sentra Kelautan dan Perikanan Teramang Jaya & KSP Kampung Sejahtera Bantal (Pasal 44 ayat 5b & Pasal 45 ayat 1).\n" +
                        "• KSK 3 (Fungsi & Daya Dukung Lingkungan Adaptif Bencana): Kawasan Perkotaan Mukomuko di 5 Kecamatan (Air Dikit, Air Manjunto, Kota Mukomuko, Teras Terunjam, XIV Koto) sebagai Kawasan Permukiman Perkotaan yang adaptif terhadap ancaman bencana alam (Pasal 44 ayat 6b & Pasal 45 ayat 2).",
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 2. Rencana Pola Ruang Wilayah & Ketentuan Intensitas KDB/KDH/KLB (Pasal 29–43 & Pasal 79–88)
        Surface(
            color = subCardBg,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Rencana Pola Ruang Wilayah (413.866 Ha) & Aturan KDB / KDH / KLB (Pasal 29–43 & 79–88)",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp
                )

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Semua", "Kawasan Lindung", "Kawasan Budi Daya").forEach { cat ->
                        FilterChip(
                            selected = selectedPatternFilter == cat,
                            onClick = { onSelectPatternFilter(cat) },
                            label = {
                                Text(
                                    text = cat,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        )
                    }
                }

                filteredPatterns.forEach { pattern ->
                    val isProtected = pattern.category == "Kawasan Lindung"
                    val badgeColor = if (isProtected) lowRiskGreen else accentNavy
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${pattern.zoneName} (${pattern.articleReference})",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Surface(
                                    color = badgeColor.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "${formatIdInt(pattern.areaHa)} Ha",
                                        color = badgeColor,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Sebaran: ${pattern.distributionSummary}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Ketentuan Intensitas: ${pattern.intensityRule}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = badgeColor
                            )
                        }
                    }
                }
            }
        }

        // 3. Indikasi Program Utama Jangka Menengah BPBD (Lampiran XII Program A.2.5.5)
        Surface(
            color = subCardBg,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Indikasi Program Utama Sistem Evakuasi Bencana BPBD (Lampiran XII Perda RTRW)",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    color = accentNavy
                )
                MukomukoRtrw2025Catalog.bpbdEvacuationPrograms.forEach { prog ->
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, cardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "${prog.code} • ${prog.programName}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "Lokasi: ${prog.location}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Pelaksana: ${prog.executor}  |  Sumber Dana: ${prog.fundingSource}  |  Periode: ${prog.period}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = accentTeal
                            )
                        }
                    }
                }
            }
        }

        // 4. Penilaian Pelaksanaan KKPR & 9 Sanksi Administratif (Pasal 95–108)
        Surface(
            color = highRiskRed.copy(alpha = if (isDark) 0.18f else 0.08f),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, highRiskRed.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Pengendalian Pemanfaatan Ruang, KKPR & 9 Sanksi Administratif (Pasal 95–108)",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    color = highRiskRed
                )
                Text(
                    text = "• Setiap kegiatan pemanfaatan ruang wajib memiliki Kesesuaian Kegiatan Pemanfaatan Ruang / KKPR (KKKPR, PKKPR, atau RKKPR) dan mematuhi kajian risiko bencana (Pasal 95–96).\n" +
                        "• Pelanggaran rencana tata ruang atau penghalangan akses jalur evakuasi/milik umum dikenai 9 Sanksi Administratif (Pasal 103 ayat 3): (1) Peringatan tertulis maks 3x, (2) Denda administratif, (3) Penghentian sementara kegiatan, (4) Penghentian sementara pelayanan umum, (5) Penutupan lokasi, (6) Pencabutan KKPR, (7) Pembatalan KKPR, (8) Pembongkaran bangunan, dan/atau (9) Pemulihan fungsi ruang (Pasal 104–108).",
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

@Composable
private fun KrbTrendBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                lineHeight = 13.sp
            )
        }
    }
}

private fun formatIdNumber(value: Double): String {
    return String.format(Locale.GERMANY, "%,.2f", value)
}

private fun formatIdInt(value: Int): String {
    return String.format(Locale.GERMANY, "%,d", value)
}

private fun formatRupiahMillions(millionRp: Double): String {
    return when {
        millionRp >= 1_000_000.0 -> {
            val triliun = millionRp / 1_000_000.0
            "Rp ${String.format(Locale.GERMANY, "%,.2f", triliun)} T"
        }
        millionRp >= 1_000.0 -> {
            val miliar = millionRp / 1_000.0
            "Rp ${String.format(Locale.GERMANY, "%,.2f", miliar)} M"
        }
        else -> {
            "Rp ${String.format(Locale.GERMANY, "%,.2f", millionRp)} Jt"
        }
    }
}
