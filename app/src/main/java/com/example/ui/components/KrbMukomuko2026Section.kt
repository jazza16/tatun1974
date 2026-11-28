package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KrbMukomuko2026Section(
    currentDistrict: District,
    isDark: Boolean,
    onNavigateToTab: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // 0 = Profil KRB & RTRW Kecamatan, 5 = Dokumen RPB 2025–2029 (31 Strategi, Nomenklatur & 11 Shelter),
    // 1 = Matriks 12 Bencana KRB, 2 = 7 Fokus IKD & IRBI,
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
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
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
                                    text = "KRB 2026–2030",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        Surface(
                            color = Color(0xFF7B1FA2),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = "Dokumen Resmi RPB 2025–2029",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "RPB 2025–2029",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    softWrap = false
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
                                    text = "PERDA RTRW NO. 12/2025",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    Text(
                        text = "Trilogi Kebijakan Kebencanaan Daerah: KRB 2026–2030, RPB 2025–2029 & Perda RTRW No. 12 Tahun 2025",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 20.sp
                    )

                    Text(
                        text = "${MukomukoKrb2026Catalog.DOCUMENT_PUBLISHER} • RPB Kab. Mukomuko 2025–2029 (31 Strategi & 11 Shelter) • ${MukomukoRtrw2025Catalog.ENACTMENT_INFO}",
                        color = Color.White.copy(alpha = 0.88f),
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )

                    // 4 Indikator Makro KRB, RPB & RTRW Kabupaten Mukomuko dalam Grid 2x2 agar tidak terpotong di layar sempit
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            KrbMacroMetricPill(
                                label = "IRBI & IKD 2025 (RPB)",
                                value = "228,28 / 0,29",
                                badge = "Target 2029: 136,87 / 0,57",
                                modifier = Modifier.weight(1f)
                            )
                            KrbMacroMetricPill(
                                label = "Strategi & Shelter RPB",
                                value = "31 Strategi",
                                badge = "11 Titik Pengungsi Resmi",
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            KrbMacroMetricPill(
                                label = "Penduduk Terpapar",
                                value = "205.602 Jiwa",
                                badge = "15 Kecamatan • 151 Desa/Kel",
                                modifier = Modifier.weight(1f)
                            )
                            KrbMacroMetricPill(
                                label = "Potensi Kerugian",
                                value = "Rp 6,74 Triliun",
                                badge = "APBD Bencana 2025: 0,59%",
                                modifier = Modifier.weight(1f)
                            )
                        }
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
                    selected = selectedSubTab == 5,
                    onClick = { selectedSubTab = 5 },
                    label = {
                        Text(
                            text = "RPB 2025–2029 (31 Strategi & 11 Shelter)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF7B1FA2).copy(alpha = 0.16f),
                        selectedLabelColor = if (isDark) Color(0xFFD8B4FE) else Color(0xFF6A1B9A),
                        selectedLeadingIconColor = if (isDark) Color(0xFFD8B4FE) else Color(0xFF6A1B9A)
                    ),
                    modifier = Modifier.testTag("krb_subtab_rpb_2025_2029")
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

            // Bar Sinkronisasi Lintas Tab & Modul Operasional SIPAGAR KITA (Buka-Tutup / Collapsible)
            var isCrossModuleBarExpanded by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
            Surface(
                color = subCardBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { isCrossModuleBarExpanded = !isCrossModuleBarExpanded }
                            .testTag("krb_toggle_cross_module_bar")
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        ) {
                            Text(
                                text = "Sinkronisasi Modul Operasional Terkait (Kec. ${districtProfile.districtName})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = accentNavy,
                                lineHeight = 14.sp
                            )
                            Text(
                                text = if (isCrossModuleBarExpanded) {
                                    "Pilih modul untuk berpindah tab • Ketuk untuk menutup"
                                } else {
                                    "Pintasan ke 8 Tab Operasional selaras KRB, RPB & RTRW • Ketuk untuk buka"
                                },
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Surface(
                            color = accentTeal.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = if (isCrossModuleBarExpanded) "Tutup" else "Buka",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accentTeal,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Icon(
                                    imageVector = if (isCrossModuleBarExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = if (isCrossModuleBarExpanded) "Tutup" else "Buka",
                                    tint = accentTeal,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = isCrossModuleBarExpanded) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val linkedModules = listOf(
                                Triple(1, "Laporan Warga (Verifikasi Zona KRB)", accentOrange),
                                Triple(3, "Statistik Bencana vs Baseline KRB", accentNavy),
                                Triple(5, "Peringatan & Gempa WRS (PGA & Tsunami)", highRiskRed),
                                Triple(6, "Monitoring Karhutla (Pasal 92 ayat 5)", medRiskAmber),
                                Triple(7, "Peta & SOP Evakuasi (TES/TEA Pasal 42)", accentTeal),
                                Triple(8, "inaRISK BNPB (IRBI 164,32 • IKD 0,29)", accentNavy),
                                Triple(10, "Maritim & Pesisir (Sempadan 100m Pasal 93)", accentTeal),
                                Triple(12, "15 Kecamatan (Profil Risiko & Peran RTRW)", lowRiskGreen)
                            )
                            linkedModules.forEach { (tabIdx, label, chipColor) ->
                                Surface(
                                    color = chipColor.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, chipColor.copy(alpha = 0.35f)),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onNavigateToTab(tabIdx) }
                                        .testTag("krb_jump_module_tab_$tabIdx")
                                ) {
                                    Text(
                                        text = "↗ $label",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = chipColor,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
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

                    5 -> RpbMukomuko2025Section(
                        currentDistrict = currentDistrict,
                        isDark = isDark,
                        subCardBg = subCardBg,
                        cardBorder = cardBorder,
                        accentNavy = accentNavy,
                        accentTeal = accentTeal,
                        highRiskRed = highRiskRed,
                        medRiskAmber = medRiskAmber,
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
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = badge,
                color = Color(0xFFFFD54F),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = "KECAMATAN ${profile.districtName.uppercase(Locale.getDefault())}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Surface(
                        color = highRiskRed,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "RISIKO ${profile.multiHazardRiskClass.uppercase(Locale.getDefault())}",
                            color = Color.White,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                }
                val adminDesc = if (profile.wardsCount > 0) {
                    "Ibu Kota: ${profile.capitalName} • ${profile.villagesCount} Desa & ${profile.wardsCount} Kelurahan"
                } else {
                    "Ibu Kota: ${profile.capitalName} • ${profile.villagesCount} Desa"
                }
                Text(
                    text = "$adminDesc • Luas: ${formatIdNumber(profile.areaHa)} Ha",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Badges Zona Khusus (Hierarki RTRW, Tsunami Pesisir & Banjir Bandang) — Ditumpuk vertikal lebar penuh agar tidak pernah terpotong di tepi kanan
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    color = accentTeal.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, accentTeal.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "${rtrwProfile.settlementHierarchyCode}: ${rtrwProfile.urbanOrRuralCenterName} (${rtrwProfile.article10Reference})",
                        color = accentTeal,
                        fontSize = 10.5.sp,
                        lineHeight = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                if (profile.isCoastalTsunamiZone) {
                    Surface(
                        color = Color(0xFFD32F2F).copy(alpha = 0.14f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFD32F2F).copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Water,
                                contentDescription = null,
                                tint = Color(0xFFD32F2F),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Zona Tsunami Pesisir (KRB & Pasal 92 ayat 8)",
                                color = if (isDark) Color(0xFFFF8A80) else Color(0xFFB71C1C),
                                fontSize = 10.5.sp,
                                lineHeight = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (profile.isFlashFloodZone) {
                    Surface(
                        color = Color(0xFFE65100).copy(alpha = 0.14f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFE65100).copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFE65100),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Zona Banjir Bandang (KRB & Pasal 92 ayat 3)",
                                color = if (isDark) Color(0xFFFFB74D) else Color(0xFFE65100),
                                fontSize = 10.5.sp,
                                lineHeight = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Grid 2x2 Kerentanan Sosial & Demografi Terpapar (Tabel 3.107) — Tinggi Kartu Seragam
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                KrbDetailStatBox(
                    title = "Penduduk Terpapar",
                    value = "${formatIdInt(profile.exposedPopulation)} Jiwa",
                    subtitle = "Umur Rentan: ${formatIdInt(profile.vulnerableAgePopulation)} jiwa",
                    accentColor = Color(0xFF1565C0),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
                KrbDetailStatBox(
                    title = "Kelompok Rentan P3KE",
                    value = "${formatIdInt(profile.poorPopulationP3KE)} Miskin",
                    subtitle = "Disabilitas: ${profile.disabilityPopulation} jiwa • ${profile.healthFacilitiesCount} Faskes",
                    accentColor = medRiskAmber,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }

            // Grid 2x2 Kapasitas & Potensi Kerugian Multibahaya (Tabel 3.108 & 3.110) — Tinggi Kartu Seragam
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val capColor = if (profile.multiHazardCapacityClass == "Sedang") lowRiskGreen else highRiskRed
                KrbDetailStatBox(
                    title = "Indeks Kapasitas (KRB)",
                    value = "${String.format(Locale.US, "%.3f", profile.multiHazardCapacityIndex)} (${profile.multiHazardCapacityClass})",
                    subtitle = "IKM: ${String.format(Locale.US, "%.3f", profile.ikmScore)} • IKD_T: ${String.format(Locale.US, "%.3f", profile.ikdTransformed)}",
                    accentColor = capColor,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
                KrbDetailStatBox(
                    title = "Potensi Rugi Multibahaya",
                    value = formatRupiahMillions(profile.totalLossMillionRp),
                    subtitle = "Fisik: ${formatRupiahMillions(profile.physicalLossMillionRp)} • Ekon: ${formatRupiahMillions(profile.economicLossMillionRp)}",
                    accentColor = highRiskRed,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }

            // Distribusi Luas Risiko Multibahaya & Kerusakan Lingkungan (Tabel 3.108 & 3.111)
            // Ditata dalam Grid 2x2 bersih agar tidak pernah terhimpit secara vertikal
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Distribusi Luas Risiko Multibahaya & Lingkungan (Tabel 3.108 & 3.111)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        KrbAreaDistributionPill(
                            label = "Area Risiko Tinggi",
                            value = "${formatIdNumber(profile.riskHighAreaHa)} Ha",
                            accentColor = highRiskRed,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                        KrbAreaDistributionPill(
                            label = "Area Risiko Sedang",
                            value = "${formatIdNumber(profile.riskMediumAreaHa)} Ha",
                            accentColor = medRiskAmber,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        KrbAreaDistributionPill(
                            label = "Area Risiko Rendah",
                            value = "${formatIdNumber(profile.riskLowAreaHa)} Ha",
                            accentColor = lowRiskGreen,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                        KrbAreaDistributionPill(
                            label = "Lingkungan Terdampak",
                            value = "${formatIdNumber(profile.envDamageHa)} Ha",
                            accentColor = if (profile.envDamageHa > 1000.0) highRiskRed else accentTeal,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }
            }
        }
    }

    // 2. Profil Ancaman Utama Spesifik Kecamatan (KRB 2026-2030) dalam Container Rapi
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
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Profil Ancaman Bencana Dominan di Kec. ${profile.districtName}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Berdasarkan Dokumen KRB Kabupaten Mukomuko Tahun 2026–2030 (${profile.primaryHazardBadges.size} Ancaman Prioritas Kecamatan)",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            profile.primaryHazardBadges.forEach { badge ->
                val badgeColor = when (badge.riskLevel) {
                    "Tinggi" -> highRiskRed
                    "Sedang" -> medRiskAmber
                    else -> lowRiskGreen
                }
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.38f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = badge.hazardName,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = badge.detailMetric,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 15.sp
                            )
                        }
                        Surface(
                            color = badgeColor.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = "Risiko ${badge.riskLevel}",
                                color = badgeColor,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                            )
                        }
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
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Surface(
                        color = accentTeal,
                        shape = RoundedCornerShape(7.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Gavel,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "RTRW: ${rtrwProfile.settlementHierarchyCode}",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                    Surface(
                        color = accentTeal.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(7.dp)
                    ) {
                        Text(
                            text = "PERDA RTRW NO. 12/2025",
                            color = accentTeal,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 10.sp,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.5.dp)
                        )
                    }
                }
                Text(
                    text = "Profil Tata Ruang Kec. ${rtrwProfile.districtName}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.5.sp,
                    color = accentTeal,
                    lineHeight = 18.sp,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "${rtrwProfile.settlementHierarchyTitle} • ${rtrwProfile.urbanOrRuralCenterName} (${rtrwProfile.article10Reference})",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 15.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Status Kawasan Strategis Kabupaten (Pasal 44-45)
            Surface(
                color = accentTeal.copy(alpha = 0.1f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, accentTeal.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
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
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier.fillMaxWidth()
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
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = if (isDark) Color(0xFF64B5F6) else Color(0xFF0D47A1),
                    modifier = Modifier
                        .padding(top = 1.dp)
                        .size(16.dp)
                )
                Text(
                    text = "Rekomendasi Fokus Penanganan KRB 2026–2030 (${profile.districtName})",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = if (isDark) Color(0xFF90CAF9) else Color(0xFF0D47A1),
                    modifier = Modifier.weight(1f)
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
private fun KrbAreaDistributionPill(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = accentColor.copy(alpha = 0.08f),
        shape = RoundedCornerShape(9.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = title,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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

@OptIn(ExperimentalLayoutApi::class)
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
                    // Baris Badge Prioritas & Risiko di atas, diikuti Judul penuh di bawahnya agar judul bencana panjang tidak pernah terhimpit
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        FlowRow(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                color = prioColor,
                                shape = RoundedCornerShape(7.dp)
                            ) {
                                Text(
                                    text = "Prioritas ${hazard.priorityLevel}",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Surface(
                                color = riskColor.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(7.dp),
                                border = BorderStroke(1.dp, riskColor.copy(alpha = 0.35f))
                            ) {
                                Text(
                                    text = "Risiko ${hazard.riskClass}",
                                    color = riskColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Detail",
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "${hazard.number}. ${hazard.hazardName}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Luas Bahaya: ${formatIdNumber(hazard.hazardAreaHa)} Ha (${hazard.affectedDistrictsCount} Kecamatan) • Tren Historis: ${hazard.historicalTrend}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 15.sp
                        )
                    }

                    // Ringkasan Cepat 3 Pilar KRB — Label di atas Nilai agar tidak terpotong di layar sempit
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        KrbMiniMetricTag(
                            label = "Kelas Bahaya",
                            value = hazard.hazardClass,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                        KrbMiniMetricTag(
                            label = "Kerentanan",
                            value = hazard.vulnerabilityClass,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                        KrbMiniMetricTag(
                            label = "Kapasitas",
                            value = String.format(Locale.US, "%.3f", hazard.capacityIndex),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
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
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Max),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                KrbMiniMetricTag(
                                    label = "Jiwa Terpapar",
                                    value = "${formatIdInt(hazard.exposedPopulation)} jiwa",
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                )
                                KrbMiniMetricTag(
                                    label = "Potensi Rugi",
                                    value = formatRupiahMillions(hazard.totalLossMillionRp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                )
                                KrbMiniMetricTag(
                                    label = "Lingk. Rusak",
                                    value = "${formatIdNumber(hazard.envDamageHa)} Ha",
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                )
                            }
                            Text(
                                text = "Temuan Kunci KRB: ${hazard.keyHighlight}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 15.sp
                            )
                            Text(
                                text = "Mitigasi Struktural: ${hazard.structuralMitigation}",
                                fontSize = 11.sp,
                                color = if (isDark) Color(0xFF90CAF9) else Color(0xFF0D47A1),
                                lineHeight = 15.sp
                            )
                            Text(
                                text = "Mitigasi Non-Struktural: ${hazard.nonStructuralMitigation}",
                                fontSize = 11.sp,
                                color = if (isDark) Color(0xFFA5D6A7) else Color(0xFF1B5E20),
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
private fun KrbMiniMetricTag(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(7.dp),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = label,
                fontSize = 9.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            color = highRiskRed.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(7.dp)
                        ) {
                            Text(
                                text = "IKD 2025: 0,29 (RENDAH)",
                                color = highRiskRed,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.5.sp,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Surface(
                            color = lowRiskGreen.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(7.dp)
                        ) {
                            Text(
                                text = "TARGET RPB 2029: 0,57",
                                color = lowRiskGreen,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.5.sp,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Text(
                        text = "7 Fokus Prioritas Indeks Ketahanan Daerah (IKD 2025)",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.5.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Tabel 3.102 KRB 2026–2030 • Skor IKD = 0,29 (IKD_T = 0,242 • Kelas Rendah)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 15.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                MukomukoKrb2026Catalog.ikdPriorities.forEach { item ->
                    val barColor = when {
                        item.focusNumber == 3 -> lowRiskGreen // Tertinggi 0.46
                        item.focusNumber == 4 || item.focusNumber == 5 -> highRiskRed // Terendah 0.20 & 0.22
                        else -> accentNavy
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, barColor.copy(alpha = 0.30f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 9.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Surface(
                                    color = barColor,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "FOKUS ${item.focusNumber}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                                    )
                                }
                                Surface(
                                    color = barColor.copy(alpha = 0.13f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "Skor ${String.format(Locale.US, "%.2f", item.score)} • ${item.categoryLabel}",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = barColor,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                                    )
                                }
                            }
                            Text(
                                text = item.focusTitle,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 16.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
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
                                lineHeight = 14.5.sp
                            )
                        }
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
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            color = lowRiskGreen.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(7.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.5.dp),
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
                                    text = "-26,88 Poin (2015–2025)",
                                    color = lowRiskGreen,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 10.5.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                    Text(
                        text = "Tren Indeks Risiko Bencana Indonesia (IRBI) 2015–2025",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.5.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Gambar 1.1 KRB 2026–2030 • Turun dari 191,20 (2015) ke 164,32 (2025)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 15.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
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

                // Ringkasan Tren Kejadian Historis 2015-2024 (Tabel 4.1) — Ditumpuk vertikal agar rapi & penuh
                HorizontalDivider(color = cardBorder)
                Text(
                    text = "Tren Historis Kejadian Bencana 2015–2024 (Tabel 4.1):",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    KrbTrendBadge(
                        icon = Icons.Default.TrendingUp,
                        label = "Tren Meningkat: Banjir, Tanah Longsor, Cuaca Ekstrem, serta Gelombang Ekstrem & Abrasi",
                        color = highRiskRed,
                        modifier = Modifier.fillMaxWidth()
                    )
                    KrbTrendBadge(
                        icon = Icons.Default.TrendingFlat,
                        label = "Tren Stabil / Berulang Tahunan: Kebakaran Hutan & Lahan (Karhutla) serta Kekeringan",
                        color = Color(0xFFF57C00),
                        modifier = Modifier.fillMaxWidth()
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
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Baris Atas: Badge Pasal di kiri & Ikon Expand/Collapse di kanan
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        ) {
                            Surface(
                                color = accentTeal,
                                shape = RoundedCornerShape(7.dp)
                            ) {
                                Text(
                                    text = rule.articleReference,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Buka Ketentuan Pasal",
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Judul Ketentuan Khusus dengan Lebar Penuh (100% Full Width) agar tidak pernah terhimpit per kata
                    Text(
                        text = rule.zoneTitle,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        lineHeight = 19.sp,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = rule.krbAlignmentNote,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 15.sp,
                        modifier = Modifier.fillMaxWidth()
                    )

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
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.Top,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = lowRiskGreen,
                                            modifier = Modifier
                                                .padding(top = 1.dp)
                                                .size(14.dp)
                                        )
                                        Text(
                                            text = "Kegiatan Diperbolehkan (Huruf a):",
                                            fontSize = 11.sp,
                                            lineHeight = 15.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = lowRiskGreen,
                                            modifier = Modifier.weight(1f)
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
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.Top,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = medRiskAmber,
                                            modifier = Modifier
                                                .padding(top = 1.dp)
                                                .size(14.dp)
                                        )
                                        Text(
                                            text = "Diperbolehkan Bersyarat Wajib Mitigasi (Huruf b):",
                                            fontSize = 11.sp,
                                            lineHeight = 15.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = medRiskAmber,
                                            modifier = Modifier.weight(1f)
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
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.Top,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Gavel,
                                            contentDescription = null,
                                            tint = highRiskRed,
                                            modifier = Modifier
                                                .padding(top = 1.dp)
                                                .size(14.dp)
                                        )
                                        Text(
                                            text = "Kegiatan Tidak Diperbolehkan / Dilarang (Huruf c):",
                                            fontSize = 11.sp,
                                            lineHeight = 15.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = highRiskRed,
                                            modifier = Modifier.weight(1f)
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
                            modifier = Modifier.padding(11.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            // Baris Atas: Kategori di kiri & Luas Ha di kanan (tanpa teks Pasal panjang agar badge Luas Ha tidak pernah tergencet vertikal)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = badgeColor.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = pattern.category,
                                        color = badgeColor,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Surface(
                                    color = badgeColor,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "${formatIdInt(pattern.areaHa)} Ha",
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            Text(
                                text = pattern.zoneName,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = "Dasar Hukum Perda RTRW: ${pattern.articleReference}",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = badgeColor,
                                lineHeight = 14.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = "Sebaran: ${pattern.distributionSummary}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 15.sp
                            )
                            Text(
                                text = "Ketentuan Intensitas: ${pattern.intensityRule}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = badgeColor,
                                lineHeight = 15.sp
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
                            modifier = Modifier.padding(11.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            // Badge Kode Program dan Periode ditata bertingkat lebar penuh agar tidak pernah bertabrakan
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = accentNavy.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "Kode Program ${prog.code}",
                                        color = accentNavy,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            Surface(
                                color = accentTeal.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(0.5.dp, accentTeal.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Periode & Dasar: ${prog.period}",
                                    fontSize = 10.5.sp,
                                    lineHeight = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accentTeal,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Text(
                                text = prog.programName,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.5.sp,
                                lineHeight = 17.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = "Lokasi: ${prog.location}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 15.sp
                            )
                            Text(
                                text = "Pelaksana: ${prog.executor}  •  Sumber Dana: ${prog.fundingSource}",
                                fontSize = 10.5.sp,
                                lineHeight = 14.sp,
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
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                lineHeight = 15.sp
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

/**
 * Banner ringkas penyelarasan KRB 2026–2030 & Perda RTRW No. 12 Tahun 2025
 * untuk ditempatkan pada Tab/Modul operasional terkait (Beranda Hari Ini, Laporan Warga, Statistik Bencana,
 * Prakiraan Cuaca, Peringatan & Gempa WRS, Monitoring Karhutla, Peta & SOP Evakuasi, inaRISK BNPB,
 * Cuaca Historis, Maritim & Pesisir, Kualitas Udara, dan 15 Kecamatan).
 * Dirancang menggunakan pola akordeon buka-tutup (collapsible) seperti Catatan Rilis Aplikasi
 * agar ringkas, sesuai preferensi pengguna, dan tidak mendominasi tampilan utama setiap Tab.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KrbRtrwModuleContextBanner(
    currentDistrict: District,
    moduleTag: String,
    moduleTitle: String,
    krbAlignmentNote: String,
    rtrwRegulationNote: String,
    isDark: Boolean,
    onOpenKrbRtrwTab: () -> Unit,
    rpbAlignmentNote: String = "",
    initiallyExpanded: Boolean = false,
    modifier: Modifier = Modifier
) {
    var isExpanded by androidx.compose.runtime.saveable.rememberSaveable(moduleTag) {
        mutableStateOf(initiallyExpanded)
    }

    val districtProfile = remember(currentDistrict.id, currentDistrict.name) {
        MukomukoKrb2026Catalog.getDistrictProfile(currentDistrict)
    }
    val districtRtrw = remember(currentDistrict.id, currentDistrict.name) {
        MukomukoRtrw2025Catalog.getDistrictSpatialProfile(currentDistrict)
    }

    val accentNavy = if (isDark) Color(0xFF60A5FA) else Color(0xFF0D47A1)
    val accentTeal = if (isDark) Color(0xFF4DB6AC) else Color(0xFF00695C)
    val accentOrange = Color(0xFFE65100)
    val borderCol = if (isExpanded) {
        accentOrange
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
    }

    Card(
        onClick = { isExpanded = !isExpanded },
        modifier = modifier
            .fillMaxWidth()
            .testTag("krb_rtrw_context_banner_$moduleTag"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(if (isExpanded) 1.5.dp else 1.dp, borderCol),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Surface(
                        color = accentOrange.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = accentOrange,
                            modifier = Modifier
                                .padding(6.dp)
                                .size(16.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = "Kebijakan KRB, RPB & RTRW",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = accentNavy
                            )
                            Surface(
                                color = accentTeal.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = districtRtrw.settlementHierarchyCode,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = accentTeal,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                )
                            }
                        }
                        Text(
                            text = if (isExpanded) {
                                "$moduleTitle • Kec. ${districtProfile.districtName}"
                            } else {
                                "$moduleTitle (${districtProfile.districtName}) • Ketuk untuk buka rincian"
                            },
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        color = accentOrange.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (isExpanded) "Tutup" else "Buka",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentOrange,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Tutup rincian KRB, RPB & RTRW" else "Buka rincian KRB, RPB & RTRW",
                        tint = accentOrange,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(color = borderCol.copy(alpha = 0.6f))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Surface(
                            color = accentNavy.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Kec. ${districtProfile.districtName}: Multibahaya ${districtProfile.multiHazardRiskClass} (${formatIdNumber(districtProfile.areaHa)} Ha)",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentNavy,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Surface(
                            color = accentTeal.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "IKD: ${String.format(Locale.US, "%.3f", districtProfile.multiHazardCapacityIndex)} (${districtProfile.multiHazardCapacityClass}) • ${districtRtrw.settlementHierarchyCode}: ${districtRtrw.urbanOrRuralCenterName}",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentTeal,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = "• Kajian Risiko (KRB 2026–2030): $krbAlignmentNote",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 15.sp
                    )
                    if (rpbAlignmentNote.isNotBlank()) {
                        Text(
                            text = "• Rencana Aksi (RPB 2025–2029): $rpbAlignmentNote",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 15.sp
                        )
                    }
                    Text(
                        text = "• Regulasi Tata Ruang (Perda RTRW No. 12/2025): $rtrwRegulationNote (Hierarki Kec. ${districtProfile.districtName}: ${districtRtrw.settlementHierarchyTitle} • ${districtRtrw.article10Reference})",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 15.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Surface(
                            color = accentOrange,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onOpenKrbRtrwTab() }
                                .testTag("btn_open_krb_rtrw_from_$moduleTag")
                        ) {
                            Text(
                                text = "Buka Tab Khusus KRB, RPB & RTRW ↗",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

