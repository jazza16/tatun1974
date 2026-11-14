package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Flood
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.Water
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.District
import com.example.model.MukomukoDistricts
import com.example.ui.theme.OrangeOnPrimary
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.OrangeSecondary
import com.example.ui.theme.WeatherGoodGreen
import com.example.ui.theme.WeatherRainBlue
import com.example.ui.theme.WeatherSeaCyan
import com.example.ui.theme.WeatherSunGold

/**
 * Data model for inaRISK BNPB Hazard profile.
 */
data class InaRiskHazard(
    val id: String,
    val name: String,
    val hazardLevel: String, // "TINGGI", "SEDANG", "RENDAH"
    val riskScore: Int,      // 0 - 100
    val icon: ImageVector,
    val color: Color,
    val affectedArea: String,
    val exposedPopulation: String,
    val description: String,
    val mitigationSteps: List<String>
)

object InaRiskMukomukoDataProvider {

    fun getHazardsForDistrict(district: District): List<InaRiskHazard> {
        val isCoast = district.category.contains("Pesisir", ignoreCase = true) ||
                district.id in listOf("kota_mukomuko", "ipuh", "pondok_suguh", "teramang_jaya", "air_rami", "sungai_rumbai", "air_dikit")

        return listOf(
            InaRiskHazard(
                id = "gempa",
                name = "Gempa Bumi Megathrust",
                hazardLevel = "TINGGI",
                riskScore = 94,
                icon = Icons.Default.Warning,
                color = Color(0xFFDC2626),
                affectedArea = "Seluruh 15 Kecamatan (${district.name})",
                exposedPopulation = "100% Penduduk Terpapar (Zona Sesar Mentawai)",
                description = "Kabupaten Mukomuko berada langsung di atas zona subduksi lempeng Indo-Australia terhadap Eurasia dengan potensi gempa bumi M 7.5 - M 8.9.",
                mitigationSteps = listOf(
                    "Penerapan konstruksi bangunan tahan gempa (retrofitting struktur rumah).",
                    "Penyediaan Tas Siaga Bencana (TSB) keluarga di dekat pintu keluar.",
                    "Simulasi evakuasi mandiri berkala (Prinsip Menunduk, Lindungi Kepala, Berpegangan)."
                )
            ),
            InaRiskHazard(
                id = "tsunami",
                name = "Tsunami Samudra Hindia",
                hazardLevel = if (isCoast) "TINGGI" else "SEDANG",
                riskScore = if (isCoast) 90 else 60,
                icon = Icons.Default.Waves,
                color = if (isCoast) Color(0xFFDC2626) else Color(0xFFD97706),
                affectedArea = if (isCoast) "Kawasan Pantai 0-12 m DPL (${district.name})" else "Wilayah Hilir & Muara Sungai",
                exposedPopulation = if (isCoast) "Sangat Tinggi di zona pemukiman pesisir" else "Rendah - Sedang (Pengungsi pesisir)",
                description = "Garis pantai Mukomuko sepanjang 100+ km menghadap langsung palung Samudra Hindia. Gelombang tsunami diproyeksikan tiba 20-30 menit setelah gempa besar bawah laut dangkal.",
                mitigationSteps = listOf(
                    "Evakuasi segera ke dataran tinggi (>15 m DPL) tanpa menunggu sirene resmi jika gempa terasa kuat >20 detik.",
                    "Hafalkan jalur evakuasi resmi BPBD Mukomuko menuju Tempat Evakuasi Sementara (TES).",
                    "Hindari mendekati bibir pantai atau muara sungai saat air laut surut mendadak."
                )
            ),
            InaRiskHazard(
                id = "banjir",
                name = "Banjir & Luapan DAS",
                hazardLevel = "TINGGI",
                riskScore = 86,
                icon = Icons.Default.Flood,
                color = Color(0xFF2563EB),
                affectedArea = "Daerah Aliran Sungai (DAS) Selagan, Manjunto, Teramang & Air Dikit",
                exposedPopulation = "Tinggi pada dataran rendah & bantaran sungai",
                description = "Curah hujan tinggi di hulu perbukitan Taman Nasional Kerinci Seblat memicu luapan sungai besar yang melintasi pemukiman dan perkebunan di Mukomuko.",
                mitigationSteps = listOf(
                    "Normalisasi dan pembersihan sampah di saluran drainase serta sempadan sungai.",
                    "Peninggian pondasi rumah dan pembuatan tanggul pelindung di pemukiman rawan.",
                    "Pantau peringatan dini cuaca ekstrem BMKG dan status debit pintu air."
                )
            ),
            InaRiskHazard(
                id = "cuaca_ekstrem",
                name = "Cuaca Ekstrem & Angin Kencang",
                hazardLevel = "SEDANG - TINGGI",
                riskScore = 75,
                icon = Icons.Default.Thunderstorm,
                color = Color(0xFFEA580C),
                affectedArea = "Kawasan Terbuka & Pesisir Seluruh Mukomuko",
                exposedPopulation = "Sedang - Tinggi (Pengguna jalan & nelayan)",
                description = "Angin kencang konvektif dan badai tropis sering melanda wilayah perairan dan daratan Mukomuko, menyebabkan pohon tumbang dan atap rumah rusak.",
                mitigationSteps = listOf(
                    "Pemangkasan dahan pohon rimbun di dekat rumah dan kabel listrik.",
                    "Perkuatan konstruksi atap seng dengan paku ulir dan reng yang kokoh.",
                    "Larangan melaut bagi kapal nelayan kecil saat BMKG menerbitkan peringatan gelombang tinggi."
                )
            ),
            InaRiskHazard(
                id = "abrasi",
                name = "Abrasi Pantai Pesisir",
                hazardLevel = if (isCoast) "TINGGI" else "RENDAH",
                riskScore = if (isCoast) 82 else 30,
                icon = Icons.Default.Water,
                color = if (isCoast) Color(0xFFD97706) else Color(0xFF16A34A),
                affectedArea = if (isCoast) "Garis Pantai Abrasi Mukomuko & Jalur Lintas Barat" else "Tidak Terpapar Langsung",
                exposedPopulation = if (isCoast) "Tinggi pada fasilitas jalan nasional & rumah tepi pantai" else "Nihil",
                description = "Hantaman gelombang pasang Samudra Hindia terus mengikis garis pantai dan mengancam infrastruktur vital Jalur Lintas Barat Sumatera.",
                mitigationSteps = listOf(
                    "Pembangunan revetment batu gajah dan pemecah gelombang (breakwater).",
                    "Penanaman vegetasi pantai seperti pohon cemara laut dan mangrove peredam ombak.",
                    "Penetapan garis sempadan pantai minimal 100 meter dari titik pasang tertinggi."
                )
            ),
            InaRiskHazard(
                id = "longsor",
                name = "Tanah Longsor Perbukitan",
                hazardLevel = if (district.category.contains("Perkebunan") || district.id in listOf("malin_deman", "selagan_raya", "teras_terunjam")) "SEDANG" else "RENDAH",
                riskScore = 62,
                icon = Icons.Default.Landscape,
                color = Color(0xFF78350F),
                affectedArea = "Lereng Tebing Kecamatan Malin Deman & Selagan Hulu",
                exposedPopulation = "Sedang di sepanjang jalan perbukitan dan pemukiman lereng",
                description = "Struktur tanah labil di lereng perbukitan rentan mengalami pergerakan massa tanah ketika jenuh air hujan deras.",
                mitigationSteps = listOf(
                    "Pembuatan terasering dan drainase lereng untuk mencegah akumulasi air tanah.",
                    "Hindari penggalian tebing curam tanpa dinding penahan tanah (turap).",
                    "Waspada retakan tanah setelah hujan deras berdurasi lebih dari 3 jam berturut-turut."
                )
            )
        )
    }
}

/**
 * Section displaying official BNPB inaRISK Disaster Risk Assessment
 * replacing the citizen report tab with comprehensive BPBD Mukomuko disaster hazard analytics.
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
    var selectedDistrict by remember { mutableStateOf(currentDistrict) }
    var selectedHazardId by remember { mutableStateOf<String?>("gempa") }

    val hazards = remember(selectedDistrict) {
        InaRiskMukomukoDataProvider.getHazardsForDistrict(selectedDistrict)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("inarisk_bnpb_section")
            .padding(bottom = 24.dp)
    ) {

        // 1. OFFICIAL HERO HEADER CARD
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("inarisk_hero_card"),
            shape = RoundedCornerShape(22.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {

                // Top Badge Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = OrangePrimary,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.size(38.dp)
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

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "inaRISK BNPB",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = Color(0xFFDC2626),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "RISIKO TINGGI",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Kajian Risiko Bencana Indonesia • Dokumen KRB Mukomuko",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // IRBI Score Banner
                Surface(
                    color = Color(0xFFFEF2F2),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Indeks Risiko Bencana Indonesia (IRBI)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF991B1B)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Skor Indeks: 168.40 (Kelas Risiko TINGGI)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFDC2626)
                            )
                            Text(
                                text = "Mukomuko termasuk kawasan prioritas nasional pengurangan risiko bencana multi-ancaman gempa, tsunami, banjir, dan abrasi.",
                                fontSize = 11.sp,
                                color = Color(0xFF7F1D1D),
                                lineHeight = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // District Filter Selector
                Text(
                    text = "Pilih Kecamatan untuk Analisis Risiko:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MukomukoDistricts.list.forEach { dist ->
                        val isSelected = dist.id == selectedDistrict.id
                        Surface(
                            color = if (isSelected) OrangePrimary else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .clickable { selectedDistrict = dist }
                                .testTag("inarisk_district_${dist.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = if (isSelected) OrangeOnPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = dist.name,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) OrangeOnPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. MULTI-HAZARD RISK MATRIX
        Text(
            text = "Matriks Bahaya & Risiko di ${selectedDistrict.name}",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        hazards.forEach { hazard ->
            val isExpanded = selectedHazardId == hazard.id

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        selectedHazardId = if (isExpanded) null else hazard.id
                    }
                    .testTag("inarisk_hazard_${hazard.id}"),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {

                    // Header item
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
                                    .size(36.dp)
                                    .background(hazard.color.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = hazard.icon,
                                    contentDescription = hazard.name,
                                    tint = hazard.color,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Text(
                                text = hazard.name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = hazard.color,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = hazard.hazardLevel,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Detail",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Sub-judul wilayah terdampak diletakkan tepat di atas garis pemisah (divider) agar tidak menabrak badge
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = hazard.affectedArea,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Expanded Details
                    AnimatedVisibility(visible = isExpanded) {
                        Column(modifier = Modifier.padding(top = 8.dp)) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                modifier = Modifier.padding(vertical = 8.dp)
                            )

                            // Description
                            Text(
                                text = hazard.description,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                lineHeight = 16.sp
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Exposed population tag
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = OrangeSecondary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Paparan: ${hazard.exposedPopulation}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Actionable mitigation steps
                            Text(
                                text = "Rekomendasi Aksi & Mitigasi BNPB:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            hazard.mitigationSteps.forEachIndexed { sIdx, step ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = WeatherGoodGreen,
                                        modifier = Modifier
                                            .size(14.dp)
                                            .padding(top = 2.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = step,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3. VULNERABILITY INDEX CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Dimensi Kerentanan Wilayah (Vulnerability Profile)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Analisis kapasitas dan kerentanan Kabupaten Mukomuko menurut inaRISK",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VulnerabilityMetric(
                        label = "Sosial",
                        value = "Tinggi",
                        desc = "Kepadatan pesisir & lansia",
                        color = Color(0xFFEF4444),
                        modifier = Modifier.weight(1f)
                    )
                    VulnerabilityMetric(
                        label = "Fisik",
                        value = "Tinggi",
                        desc = "Jalan lintas & rumah kayu",
                        color = Color(0xFFEA580C),
                        modifier = Modifier.weight(1f)
                    )
                    VulnerabilityMetric(
                        label = "Ekonomi",
                        value = "Sedang",
                        desc = "Perkebunan sawit & tambak",
                        color = Color(0xFFF59E0B),
                        modifier = Modifier.weight(1f)
                    )
                    VulnerabilityMetric(
                        label = "Lingkungan",
                        value = "Sedang",
                        desc = "Sempadan DAS & pesisir",
                        color = Color(0xFF10B981),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. ACTION BUTTONS & EMERGENCY DIALER
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
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
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                softWrap = false
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = desc,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                maxLines = 1
            )
        }
    }
}
