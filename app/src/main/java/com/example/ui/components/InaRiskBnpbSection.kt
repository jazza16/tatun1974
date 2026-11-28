package com.example.ui.components

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Flood
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Water
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.District
import com.example.model.KrbDistrictRiskProfile
import com.example.model.MukomukoDistricts
import com.example.model.MukomukoKrb2026Catalog
import com.example.model.MukomukoRtrw2025Catalog
import com.example.model.RtrwDistrictSpatialProfile
import com.example.ui.theme.OrangeOnPrimary
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.OrangeSecondary
import com.example.ui.theme.WeatherGoodGreen
import com.example.ui.theme.WeatherRainBlue
import java.util.Locale

/**
 * Data model for inaRISK BNPB Hazard profile, 100% disinkronkan dengan:
 * 1. Dokumen Kajian Risiko Bencana (KRB) Kabupaten Mukomuko Tahun 2026–2030 (BPBD & PS2DL UGM, Januari 2026)
 * 2. Perda Kabupaten Mukomuko Nomor 12 Tahun 2025 tentang RTRW Kabupaten Mukomuko Tahun 2025–2045 (Pasal 92 & 93)
 */
data class InaRiskHazard(
    val id: String,
    val number: Int,
    val name: String,
    val priorityLevel: Int, // 1 = Prioritas 1, 2 = Prioritas 2, 3 = Prioritas 3 (Tabel 3.113 KRB 2026-2030)
    val hazardClass: String, // Kelas Bahaya KRB
    val vulnerabilityClass: String, // Kelas Kerentanan KRB
    val capacityIndex: Double, // Indeks Kapasitas KRB
    val capacityClass: String, // Kelas Kapasitas KRB
    val hazardLevel: String, // Kelas Risiko KRB ("TINGGI", "SEDANG", "RENDAH")
    val isDominantInSelectedDistrict: Boolean,
    val districtSpecificMetric: String,
    val rtrwArticleReference: String,
    val icon: ImageVector,
    val color: Color,
    val affectedArea: String,
    val exposedPopulation: String,
    val description: String,
    val mitigationSteps: List<String>
)

object InaRiskMukomukoDataProvider {

    fun getHazardsForDistrict(district: District): List<InaRiskHazard> {
        val districtProfile = MukomukoKrb2026Catalog.getDistrictProfile(district)
        val isCoastTsunami = districtProfile.isCoastalTsunamiZone // 8 Kecamatan Pesisir resmi Tabel 3.87 KRB 2026-2030
        val isFlashFlood = districtProfile.isFlashFloodZone // 7 Kecamatan koridor sungai Bukit Barisan KRB 2026-2030

        fun findDistrictBadgeMetric(keyword: String): String? {
            return districtProfile.primaryHazardBadges.firstOrNull {
                it.hazardName.contains(keyword, ignoreCase = true)
            }?.let { "${it.detailMetric} (Risiko Kecamatan: ${it.riskLevel})" }
        }

        val allTwelveHazards = MukomukoKrb2026Catalog.twelveHazardsMatrix.map { krb ->
            val (id, icon, rtrwRef, badgeKeyword) = when (krb.number) {
                1 -> Quad("banjir", Icons.Default.Flood, "Pasal 92 ayat (2) & Pasal 93 ayat (3)", "Banjir")
                2 -> Quad("banjir_bandang", Icons.Default.Waves, "Pasal 92 ayat (3) & Pasal 93 ayat (3)", "Banjir Bandang")
                3 -> Quad("cuaca_ekstrem", Icons.Default.Thunderstorm, "Pasal 92 ayat (1) & Mitigasi Cuaca", "Cuaca Ekstrem")
                4 -> Quad("epidemi", Icons.Default.LocalHospital, "Mitigasi Kesehatan & Sanitasi Wilayah", "Epidemi")
                5 -> Quad("abrasi", Icons.Default.Water, "Pasal 92 ayat (9) & Pasal 93 ayat (2)", "Gelombang")
                6 -> Quad("gempa", Icons.Default.Warning, "Pasal 92 ayat (6) Konstruksi Tahan Gempa", "Gempa")
                7 -> Quad("karhutla", Icons.Default.LocalFireDepartment, "Pasal 92 ayat (5) & Pasal 83 ayat (4)", "Karhutla")
                8 -> Quad("kegagalan_teknologi", Icons.Default.Build, "Pasal 42 Kawasan Peruntukan Industri", "Teknologi")
                9 -> Quad("kekeringan", Icons.Default.WbSunny, "Pasal 21 Jaringan Irigasi & Sumber Daya Air", "Kekeringan")
                10 -> Quad("likuefaksi", Icons.Default.Landscape, "Pasal 92 ayat (4) Zona Rawan Likuefaksi", "Likuefaksi")
                11 -> Quad("longsor", Icons.Default.Landscape, "Pasal 92 ayat (7) & Pasal 94 ayat (1)", "Longsor")
                else -> Quad("tsunami", Icons.Default.Waves, "Pasal 92 ayat (8) & Pasal 93 ayat (2)", "Tsunami")
            }

            val specificMetric = when (krb.number) {
                2 -> findDistrictBadgeMetric("Banjir Bandang")
                    ?: if (isFlashFlood) {
                        "Termasuk 7 Kecamatan Zona Bahaya Banjir Bandang KRB 2026–2030 (Koridor Sungai Hulu Bukit Barisan)"
                    } else {
                        "Tidak termasuk 7 kecamatan bahaya langsung banjir bandang (tetap waspada kiriman debit DAS hulu)"
                    }
                5 -> findDistrictBadgeMetric("Gelombang")
                    ?: if (isCoastTsunami && districtProfile.districtId != "xiv_koto") {
                        "Termasuk 7 Kecamatan Pesisir Zona Bahaya Gelombang Ekstrem & Abrasi Pantai KRB 2026–2030"
                    } else {
                        "Wilayah daratan/non-garis pantai langsung (tidak terpapar langsung abrasi pantai)"
                    }
                12 -> findDistrictBadgeMetric("Tsunami")
                    ?: if (isCoastTsunami) {
                        "Termasuk 8 Kecamatan Pesisir Zona Bahaya Tsunami KRB 2026–2030 (Tabel 3.87 • Ketinggian 10–11 m)"
                    } else {
                        "Zona Daratan/Hulu di luar 8 Kecamatan Pesisir Tsunami (Berfungsi sebagai Zona Penyangga Evakuasi)"
                    }
                else -> findDistrictBadgeMetric(badgeKeyword)
                    ?: "Sebaran Kabupaten: ${formatIdNumber(krb.hazardAreaHa)} Ha di ${krb.affectedDistrictsCount} Kecamatan"
            }

            val isDominant = districtProfile.primaryHazardBadges.any {
                it.hazardName.contains(badgeKeyword, ignoreCase = true)
            } || (krb.number == 12 && isCoastTsunami) || (krb.number == 2 && isFlashFlood)

            // Kelas risiko spesifik kecamatan: jika tsunami/abrasi/banjir bandang tidak berada di kecamatan tsb, beri catatan Konteks Kecamatan
            val effectiveRiskLevel = when {
                krb.number == 12 && !isCoastTsunami -> "ZONA PENYANGGA (KAB: TINGGI)"
                krb.number == 5 && (!isCoastTsunami || districtProfile.districtId == "xiv_koto") -> "NON-PESISIR (KAB: TINGGI)"
                krb.number == 2 && !isFlashFlood -> "NON-KORIDOR (KAB: TINGGI)"
                else -> krb.riskClass.uppercase(Locale.getDefault())
            }

            val badgeColor = when {
                effectiveRiskLevel.startsWith("ZONA PENYANGGA") ||
                    effectiveRiskLevel.startsWith("NON-") -> Color(0xFF475569)
                krb.riskClass.equals("Tinggi", ignoreCase = true) -> Color(0xFFDC2626)
                krb.riskClass.equals("Sedang", ignoreCase = true) -> Color(0xFFD97706)
                else -> Color(0xFF16A34A)
            }

            val explanationNote = if (krb.number == 6) {
                // Penjelasan khusus untuk mencegah salah paham mengenai Gempa Bumi di KRB 2026-2030 vs Megathrust
                "${krb.keyHighlight} Catatan Sinkronisasi KRB 2026–2030: Berdasarkan Tabel 3.105 & 3.113, meskipun seluruh 15 kecamatan berada di zona subduksi Mentawai-Bengkulu (potensi rugi tertinggi Rp 3,182 Triliun & 205.602 jiwa terpapar), perhitungan PGA menghasilkan Kelas Bahaya Sedang, Kerentanan Tinggi, dan Kapasitas Rendah (0,287) sehingga Gempa Bumi ditetapkan sebagai Kelas Risiko SEDANG (Prioritas 3), sedangkan bencana ikutannya yaitu Tsunami (di 8 Kecamatan Pesisir) dan Likuefaksi (145.653,75 Ha) berada pada Kelas Risiko TINGGI (Prioritas 1 & Prioritas 2)."
            } else {
                krb.keyHighlight
            }

            InaRiskHazard(
                id = id,
                number = krb.number,
                name = krb.hazardName,
                priorityLevel = krb.priorityLevel,
                hazardClass = krb.hazardClass,
                vulnerabilityClass = krb.vulnerabilityClass,
                capacityIndex = krb.capacityIndex,
                capacityClass = krb.capacityClass,
                hazardLevel = effectiveRiskLevel,
                isDominantInSelectedDistrict = isDominant,
                districtSpecificMetric = specificMetric,
                rtrwArticleReference = rtrwRef,
                icon = icon,
                color = badgeColor,
                affectedArea = "Luas Bahaya Kab: ${formatIdNumber(krb.hazardAreaHa)} Ha (${krb.affectedDistrictsCount} Kec) • Tren 2015–2024: ${krb.historicalTrend}",
                exposedPopulation = "${formatIdInt(krb.exposedPopulation)} Jiwa Terpapar • Potensi Rugi: ${formatRupiahMillions(krb.totalLossMillionRp)} • Lingkungan: ${formatIdNumber(krb.envDamageHa)} Ha",
                description = explanationNote,
                mitigationSteps = listOf(
                    "Mitigasi Struktural (KRB 2026–2030): ${krb.structuralMitigation}",
                    "Mitigasi Non-Struktural (KRB 2026–2030): ${krb.nonStructuralMitigation}",
                    "Ketentuan Tata Ruang (Perda RTRW No. 12 Tahun 2025): Wajib mematuhi ${rtrwRef} dalam setiap perizinan KKPR dan pembangunan fisik."
                )
            )
        }

        // Urutkan: Ancaman yang dominan di kecamatan terpilih & Prioritas 1 tampil paling atas, diikuti Prioritas 2 dan Prioritas 3
        return allTwelveHazards.sortedWith(
            compareByDescending<InaRiskHazard> { it.isDominantInSelectedDistrict }
                .thenBy { it.priorityLevel }
                .thenBy { it.number }
        )
    }

    private data class Quad(
        val id: String,
        val icon: ImageVector,
        val rtrwRef: String,
        val keyword: String
    )
}

/**
 * Tab inaRISK BNPB yang telah disinkronkan 100% dengan:
 * - Dokumen Kajian Risiko Bencana (KRB) Kabupaten Mukomuko Tahun 2026–2030
 * - Peraturan Daerah Kabupaten Mukomuko Nomor 12 Tahun 2025 tentang RTRW Tahun 2025–2045
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InaRiskBnpbSection(
    currentDistrict: District,
    onOpenEvacuationGuide: () -> Unit,
    onCall117: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    // Otomatis tersinkron ketika pengguna mengganti kecamatan di header atas, namun tetap bisa dipilih per chip
    var selectedDistrict by remember(currentDistrict.id) { mutableStateOf(currentDistrict) }
    var selectedPriorityFilter by remember { mutableIntStateOf(0) } // 0 = Semua (12), 1 = Prioritas 1 (8), 2 = Prioritas 2 (2), 3 = Prioritas 3 (2)

    val districtKrbProfile: KrbDistrictRiskProfile = remember(selectedDistrict.id, selectedDistrict.name) {
        MukomukoKrb2026Catalog.getDistrictProfile(selectedDistrict)
    }
    val districtRtrwProfile: RtrwDistrictSpatialProfile = remember(selectedDistrict.id, selectedDistrict.name) {
        MukomukoRtrw2025Catalog.getDistrictSpatialProfile(selectedDistrict)
    }

    val allHazards = remember(selectedDistrict.id) {
        InaRiskMukomukoDataProvider.getHazardsForDistrict(selectedDistrict)
    }
    val filteredHazards = remember(allHazards, selectedPriorityFilter) {
        if (selectedPriorityFilter == 0) {
            allHazards
        } else {
            allHazards.filter { it.priorityLevel == selectedPriorityFilter }
        }
    }

    var selectedHazardId by remember(selectedDistrict.id) {
        mutableStateOf<String?>(allHazards.firstOrNull()?.id ?: "banjir")
    }
    var isHeroKrbRtrwExpanded by remember { mutableStateOf(false) }
    var isDistrictVulnerabilityExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("inarisk_bnpb_section")
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        // 1. OFFICIAL HERO HEADER CARD (SINKRON KRB 2026–2030 & PERDA RTRW NO. 12 TAHUN 2025)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("inarisk_hero_card"),
            shape = RoundedCornerShape(22.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Top Badge Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Surface(
                        color = OrangePrimary,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = OrangeOnPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "inaRISK BNPB & KRB 2026–2030",
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                color = Color(0xFFDC2626),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "RISIKO TINGGI",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Text(
                            text = "Sinkronisasi Resmi Trilogi Kebijakan: KRB 2026–2030, RPB 2025–2029 (Target IRB 136,87 & IKD 0,57) & Perda RTRW No. 12 Tahun 2025",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
                            lineHeight = 15.sp
                        )
                    }
                }

                // Collapsible Toggle Bar: Ringkasan Kebijakan KRB 2026–2030, RPB 2025–2029 & Perda RTRW No. 12/2025 (Buka/Tutup seperti Catatan Rilis)
                Surface(
                    onClick = { isHeroKrbRtrwExpanded = !isHeroKrbRtrwExpanded },
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        width = if (isHeroKrbRtrwExpanded) 1.5.dp else 1.dp,
                        color = if (isHeroKrbRtrwExpanded) OrangePrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("inarisk_toggle_hero_krb_rtrw")
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "Profil KRB, RPB & RTRW Kec. ${districtKrbProfile.districtName}",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isHeroKrbRtrwExpanded) {
                                        "IRBI 2025 (RPB): 228,28 (2023–24: 164,32) • Target IKD 2029: 0,57 • ${districtRtrwProfile.settlementHierarchyCode}"
                                    } else {
                                        "IRBI 228,28 (Target 136,87) • IKD 0,29→0,57 • Ketuk untuk buka"
                                    },
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Surface(
                                    color = OrangePrimary.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = if (isHeroKrbRtrwExpanded) "Tutup" else "Buka",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = OrangePrimary,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                    )
                                }
                                Icon(
                                    imageVector = if (isHeroKrbRtrwExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = if (isHeroKrbRtrwExpanded) "Tutup rincian" else "Buka rincian",
                                    tint = OrangePrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        AnimatedVisibility(visible = isHeroKrbRtrwExpanded) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                // IRBI 2025 & IKD 2025 Official Score Banner
                                Surface(
                                    color = Color(0xFFFEF2F2),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        FlowRow(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "Indeks Risiko Bencana Indonesia (IRBI 2025)",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF991B1B)
                                            )
                                            Surface(
                                                color = Color(0xFF16A34A).copy(alpha = 0.14f),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = "Turun 26,88 Poin dari 2015",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color(0xFF15803D),
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Text(
                                            text = "Skor IRBI 2025: 164,32 (Kelas Risiko TINGGI)",
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFFDC2626)
                                        )

                                        Text(
                                            text = "• Indeks Ketahanan Daerah (IKD 2025): 0,29 (IKD_T = 0,242 • Kelas RENDAH)\n" +
                                                "• Indeks Kesiapsiagaan Masyarakat (IKM): 0,348 • Indeks Kapasitas Multibahaya: 0,295 (RENDAH)\n" +
                                                "• Cakupan Paparan: Seluruh 15 Kecamatan (148 Desa & 3 Kelurahan • 205.602 jiwa • Luas 414.320,37 Ha) berada pada Kelas Risiko Multibahaya TINGGI dengan total potensi kerugian Rp 6,74 Triliun.",
                                            fontSize = 11.sp,
                                            color = Color(0xFF7F1D1D),
                                            lineHeight = 15.sp
                                        )
                                    }
                                }

                                // Ringkasan Cepat Profil Kecamatan Terpilih (KRB 2026-2030 + Perda RTRW No. 12/2025)
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        FlowRow(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "Profil Resmi Kec. ${districtKrbProfile.districtName}",
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Surface(
                                                color = Color(0xFF00695C),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = "RTRW: ${districtRtrwProfile.settlementHierarchyCode}",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color.White,
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                        val desaKel = if (districtKrbProfile.wardsCount > 0) {
                                            "${districtKrbProfile.villagesCount} Desa & ${districtKrbProfile.wardsCount} Kelurahan"
                                        } else {
                                            "${districtKrbProfile.villagesCount} Desa"
                                        }
                                        Text(
                                            text = "Ibu Kota: ${districtKrbProfile.capitalName} • $desaKel • Luas KRB: ${formatIdNumber(districtKrbProfile.areaHa)} Ha",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight = 15.sp,
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Text(
                                            text = "• Pusat Permukiman (Pasal 10 RTRW): ${districtRtrwProfile.urbanOrRuralCenterName} (${districtRtrwProfile.settlementHierarchyTitle})\n" +
                                                "• Distribusi Luas Risiko Multibahaya (Tabel 3.111 KRB): Risiko Tinggi ${formatIdNumber(districtKrbProfile.riskHighAreaHa)} Ha | Risiko Sedang ${formatIdNumber(districtKrbProfile.riskMediumAreaHa)} Ha | Risiko Rendah ${formatIdNumber(districtKrbProfile.riskLowAreaHa)} Ha",
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

        // 2. MATRIKS 12 BAHAYA & PRIORITAS KRB 2026–2030 DI KECAMATAN TERPILIH
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Matriks 12 Bahaya & Risiko KRB 2026–2030 di Kec. ${selectedDistrict.name}",
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            Text(
                text = "Disusun sesuai urutan Prioritas Penanganan Tabel 3.105 & Tabel 3.113 Dokumen KRB 2026–2030 serta Pasal 92 & 93 Perda RTRW No. 12 Tahun 2025:",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
                lineHeight = 15.sp
            )

            // Filter Chips Prioritas 1 / 2 / 3
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val prioFilters = listOf(
                    0 to "Semua (12 Ancaman KRB)",
                    1 to "Prioritas 1 (8 Risiko Tinggi)",
                    2 to "Prioritas 2 (Likuefaksi & Wabah)",
                    3 to "Prioritas 3 (Gempa & Kegagalan Tek.)"
                )
                prioFilters.forEach { (prio, label) ->
                    FilterChip(
                        selected = selectedPriorityFilter == prio,
                        onClick = { selectedPriorityFilter = prio },
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
                val isExpanded = selectedHazardId == hazard.id
                val prioBadgeColor = when (hazard.priorityLevel) {
                    1 -> Color(0xFFB91C1C)
                    2 -> Color(0xFFD97706)
                    else -> Color(0xFF1D4ED8)
                }

                Card(
                    onClick = {
                        selectedHazardId = if (isExpanded) null else hazard.id
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("inarisk_hazard_${hazard.id}"),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        width = if (isExpanded) 1.5.dp else 1.dp,
                        color = if (isExpanded) hazard.color else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Top Row: Badge Prioritas KRB + Badge Dominan Kecamatan + Badge Kelas Risiko (FlowRow agar semua badge muncul penuh) + Expand Icon
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
                                    color = prioBadgeColor,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "PRIORITAS ${hazard.priorityLevel}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                    )
                                }

                                Surface(
                                    color = hazard.color,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "RISIKO ${hazard.hazardLevel}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                    )
                                }

                                if (hazard.isDominantInSelectedDistrict) {
                                    Surface(
                                        color = Color(0xFFFFF3E0),
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, Color(0xFFFB8C00))
                                    ) {
                                        Text(
                                            text = "Dominan di ${selectedDistrict.name}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFFE65100),
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Detail",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Row Kedua: Ikon & Nama Bencana Lebar Penuh
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(hazard.color.copy(alpha = 0.14f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = hazard.icon,
                                    contentDescription = hazard.name,
                                    tint = hazard.color,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "${hazard.number}. ${hazard.name}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Profil Kec. ${selectedDistrict.name}: ${hazard.districtSpecificMetric}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = hazard.color,
                                    lineHeight = 15.sp
                                )
                            }
                        }

                        // Ringkasan 3 Komponen KRB (Bahaya, Kerentanan, Kapasitas) & Referensi Pasal RTRW
                        Text(
                            text = "• KRB 2026–2030: Bahaya ${hazard.hazardClass} | Kerentanan ${hazard.vulnerabilityClass} | Kapasitas ${String.format(Locale.US, "%.3f", hazard.capacityIndex)} (${hazard.capacityClass})\n" +
                                "• ${hazard.affectedArea}\n" +
                                "• Regulasi Tata Ruang: ${hazard.rtrwArticleReference} (Perda RTRW No. 12 Tahun 2025)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
                            lineHeight = 15.sp,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Expanded Details
                        AnimatedVisibility(visible = isExpanded) {
                            Column(
                                modifier = Modifier.padding(top = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )

                                // Description
                                Text(
                                    text = hazard.description,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                                    lineHeight = 16.sp
                                )

                                // Exposed population & loss tag
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(9.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = OrangeSecondary,
                                            modifier = Modifier
                                                .padding(top = 1.dp)
                                                .size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Rekapitulasi Dampak Kabupaten (Tabel 3.105 KRB): ${hazard.exposedPopulation}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight = 15.sp
                                        )
                                    }
                                }

                                // Actionable mitigation steps
                                Text(
                                    text = "Rekomendasi Mitigasi KRB 2026–2030 & Ketentuan Perda RTRW No. 12/2025:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                hazard.mitigationSteps.forEach { step ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = WeatherGoodGreen,
                                            modifier = Modifier
                                                .padding(top = 2.dp)
                                                .size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = step,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
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

        // 3. DIMENSI KERENTANAN & KAPASITAS RESMI KECAMATAN TERPILIH (TABEL 3.107, 3.108, 3.110 KRB 2026–2030) — COLLAPSIBLE SEPERTI CATATAN RILIS
        Card(
            onClick = { isDistrictVulnerabilityExpanded = !isDistrictVulnerabilityExpanded },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("inarisk_card_district_vulnerability"),
            shape = RoundedCornerShape(18.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(
                width = if (isDistrictVulnerabilityExpanded) 1.5.dp else 1.dp,
                color = if (isDistrictVulnerabilityExpanded) {
                    OrangePrimary
                } else {
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                }
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Dimensi Kerentanan & Kapasitas Kec. ${districtKrbProfile.districtName} (KRB, RPB & RTRW)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isDistrictVulnerabilityExpanded) {
                                "Tabel 3.107–3.110 KRB, Target IKD RPB 2029 (0,57) & Arahan Perda RTRW 12/2025"
                            } else {
                                "4 dimensi kerentanan, target RPB 2025–2029 & arahan RTRW • Ketuk untuk membuka"
                            },
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 15.sp
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            color = OrangePrimary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (isDistrictVulnerabilityExpanded) "Tutup" else "Buka",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = OrangePrimary,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                        Icon(
                            imageVector = if (isDistrictVulnerabilityExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isDistrictVulnerabilityExpanded) "Tutup rincian" else "Buka rincian",
                            tint = OrangePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                AnimatedVisibility(visible = isDistrictVulnerabilityExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // Grid 2x2 agar tidak terpotong di layar sempit
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            VulnerabilityMetric(
                                label = "Kerentanan Sosial",
                                value = "${formatIdInt(districtKrbProfile.exposedPopulation)} Jiwa",
                                desc = "Umur Rentan: ${formatIdInt(districtKrbProfile.vulnerableAgePopulation)} • Miskin P3KE: ${formatIdInt(districtKrbProfile.poorPopulationP3KE)} • Disabilitas: ${districtKrbProfile.disabilityPopulation}",
                                color = Color(0xFFEF4444),
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                            VulnerabilityMetric(
                                label = "Kerentanan Fisik",
                                value = formatRupiahMillions(districtKrbProfile.physicalLossMillionRp),
                                desc = "Potensi kerusakan rumah, fasilitas umum & kritis (${districtKrbProfile.healthFacilitiesCount} Faskes)",
                                color = Color(0xFFEA580C),
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
                            VulnerabilityMetric(
                                label = "Kerentanan Ekonomi",
                                value = formatRupiahMillions(districtKrbProfile.economicLossMillionRp),
                                desc = "Total Rugi Fisik + Ekonomi: ${formatRupiahMillions(districtKrbProfile.totalLossMillionRp)}",
                                color = Color(0xFFD97706),
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                            VulnerabilityMetric(
                                label = "Lingkungan & Kapasitas",
                                value = "${formatIdNumber(districtKrbProfile.envDamageHa)} Ha",
                                desc = "Indeks Kapasitas: ${String.format(Locale.US, "%.3f", districtKrbProfile.multiHazardCapacityIndex)} (${districtKrbProfile.multiHazardCapacityClass}) • IKM: ${String.format(Locale.US, "%.3f", districtKrbProfile.ikmScore)}",
                                color = Color(0xFF10B981),
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                        }

                        // Rekomendasi Fokus Penanganan KRB & Arahan Tata Ruang RTRW Kecamatan Terpilih
                        Surface(
                            color = OrangePrimary.copy(alpha = 0.06f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, OrangePrimary.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = null,
                                        tint = OrangePrimary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "Rekomendasi KRB 2026–2030 & Arahan RTRW 2025–2045 (${districtKrbProfile.districtName}):",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = "• Fokus KRB 2026–2030: ${districtKrbProfile.recommendationSummary}\n" +
                                        "• Arahan Perda RTRW No. 12/2025: ${districtRtrwProfile.spatialControlDirective}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. ACTION BUTTONS & EMERGENCY DIALER
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Layanan & Kesiapsiagaan Bencana Resmi",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Button: Buka Panduan Evakuasi Mandiri BPBD
                Button(
                    onClick = onOpenEvacuationGuide,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("inarisk_open_evacuation_guide"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = OrangeOnPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Panduan & Jalur Evakuasi Mandiri BPBD",
                        fontWeight = FontWeight.Bold,
                        color = OrangeOnPrimary,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Button: Hubungi Call Center 117
                OutlinedButton(
                    onClick = onCall117,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("inarisk_call_117"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = null,
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Hubungi Call Center Bencana 117 / Posko BPBD",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Official Link info
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://inarisk.bnpb.go.id"))
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInBrowser,
                        contentDescription = null,
                        tint = WeatherRainBlue,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Kunjungi Portal Resmi inaRisk: inarisk.bnpb.go.id",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = WeatherRainBlue
                    )
                }
            }
        }
    }
}

@Composable
private fun VulnerabilityMetric(
    label: String,
    value: String,
    desc: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            Text(
                text = desc,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
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
