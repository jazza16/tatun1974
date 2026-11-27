package com.example.ui.components

import android.content.Context
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CrisisAlert
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Surfing
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.District
import com.example.model.MukomukoDistricts
import com.example.ui.theme.OrangeOnPrimary
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.OrangeSecondary
import com.example.ui.theme.WeatherGoodGreen
import kotlinx.coroutines.delay

data class EvacuationAssemblyPoint(
    val districtName: String,
    val primaryLocation: String,
    val alternativeLocation: String,
    val verticalEvacuationShelter: String,
    val safeElevationMeters: Int,
    val keyTips: String
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EarthquakeEvacuationPreparednessSection(
    currentDistrict: District,
    onCall117: () -> Unit,
    onTestTsunamiAlert: (Context) -> Unit,
    onTestSevereWeatherAlert: (Context) -> Unit,
    onOpenEvacuationGuide: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Simulation drill timer state
    var isDrillRunning by remember { mutableStateOf(false) }
    var drillSecondsLeft by remember { mutableIntStateOf(60) }
    var drillCompleted by remember { mutableStateOf(false) }

    LaunchedEffect(isDrillRunning, drillSecondsLeft) {
        if (isDrillRunning && drillSecondsLeft > 0) {
            delay(1000L)
            drillSecondsLeft -= 1
        } else if (isDrillRunning && drillSecondsLeft == 0) {
            isDrillRunning = false
            drillCompleted = true
        }
    }

    // Interactive Emergency Bag Checklist (10 Essential Items)
    val checkedItems = remember {
        mutableStateListOf(
            true, // Air minum
            true, // P3K
            true, // Dokumen penting
            false, // Senter LED
            false, // Peluit darurat
            false, // Makanan darurat
            false, // Power bank
            false, // Uang tunai
            false, // Pakaian ganti
            false  // Radio saku
        )
    }

    val bagItemsList = listOf(
        "Air minum kemasan (minimal 3 liter per anggota keluarga)",
        "Kotak P3K, obat rutin keluarga, antiseptik & perban",
        "Dokumen penting (KTP, KK, Ijazah, Akta, Surat Tanah) dalam map anti air",
        "Senter LED terang beserta baterai cadangan",
        "Peluit darurat (untuk sinyal lokasi ke tim SAR jika terjebak)",
        "Makanan tahan lama berenergi tinggi (biskuit, kornet, kurma, abon)",
        "Power bank handphone terisi penuh 100% dan kabel data",
        "Uang tunai secukupnya dalam pecahan kecil (ATM dapat padam listrik)",
        "Pakaian ganti tebal, jaket hangat, jas hujan, dan selimut termal",
        "Radio saku baterai (untuk memantau instruksi resmi BMKG / BPBD Mukomuko)"
    )

    val readyCount = checkedItems.count { it }
    val readyPercentage = (readyCount * 100) / bagItemsList.size

    // Assembly point selection per district
    var selectedDistrictName by remember { mutableStateOf(currentDistrict.name) }

    val assemblyPoints = remember {
        listOf(
            EvacuationAssemblyPoint(
                districtName = "Kota Mukomuko",
                primaryLocation = "Lapangan Merdeka Kota Mukomuko & Kompleks Perkantoran Pemkab",
                alternativeLocation = "Halaman Masjid Agung Mukomuko (Zona Bebas Bangunan Tinggi)",
                verticalEvacuationShelter = "Gedung Evakuasi Sementara (TES) / Kantor Bupati Mukomuko",
                safeElevationMeters = 25,
                keyTips = "Jauhi kawasan Muara Pantai Indah Mukomuko. Segera bergerak ke arah Jalan Lintas Barat Bukit Senja."
            ),
            EvacuationAssemblyPoint(
                districtName = "Ipuh",
                primaryLocation = "Lapangan Sepak Bola Desa Medan Jaya & Halaman Kantor Camat Ipuh",
                alternativeLocation = "Puskesmas Perawatan Ipuh Lama (Lahan Terbuka Dataran Tinggi)",
                verticalEvacuationShelter = "Gedung Bertingkat SMA/SMK Negeri Ipuh & Bukit Terbuka Pulai Payung",
                safeElevationMeters = 22,
                keyTips = "Warga pesisir Pantai Abrasi Ipuh wajib segera evakuasi ke arah daratan timur Jalan Negara."
            ),
            EvacuationAssemblyPoint(
                districtName = "Teramang Jaya",
                primaryLocation = "Lapangan Sepak Bola Pasar Teramang & Kompleks Kantor Camat",
                alternativeLocation = "Halaman SMPN Teramang Jaya",
                verticalEvacuationShelter = "Dataran Tinggi Perkebunan Bunga Tanjung",
                safeElevationMeters = 28,
                keyTips = "Jalur evakuasi utama mengikuti jalan poros desa menuju perbukitan sawit arah pedalaman."
            ),
            EvacuationAssemblyPoint(
                districtName = "Penarik",
                primaryLocation = "Lapangan Desa Penarik & Stadion Sepak Bola Penarik",
                alternativeLocation = "Kompleks Perguruan Muhammadiyah / Pasar Penarik Baru",
                verticalEvacuationShelter = "Gedung Kantor Camat Penarik",
                safeElevationMeters = 40,
                keyTips = "Kawasan Penarik berada di dataran tinggi aman tsunami, siap menjadi zona penyangga evakuasi pengungsi pesisir."
            ),
            EvacuationAssemblyPoint(
                districtName = "Lubuk Pinang",
                primaryLocation = "Lapangan Bola Lubuk Pinang & Halaman Kantor Camat",
                alternativeLocation = "Kompleks Bendung Manjunto / Area Irigasi Atas",
                verticalEvacuationShelter = "Perbukitan Desa Arah Perbatasan Sumbar",
                safeElevationMeters = 65,
                keyTips = "Waspadai retakan tanah dan potensi longsor dari lereng bukit jika gempa dangkal sangat kuat."
            ),
            EvacuationAssemblyPoint(
                districtName = "Air Dikit",
                primaryLocation = "Lapangan Terbuka Desa Dusun Baru Air Dikit",
                alternativeLocation = "Halaman Kantor Camat Air Dikit",
                verticalEvacuationShelter = "Area Bukit Sawit Dataran Tinggi Sari Bulan",
                safeElevationMeters = 26,
                keyTips = "Pesisir Air Dikit sangat dekat bibir pantai. Terapkan rumus 20-20-20 segera setelah gempa reda."
            ),
            EvacuationAssemblyPoint(
                districtName = "Pondok Suguh",
                primaryLocation = "Lapangan Merdeka Pondok Suguh & Halaman SMPN 1",
                alternativeLocation = "Area Dataran Tinggi Desa Karya Mulya",
                verticalEvacuationShelter = "Gedung Serbaguna Kecamatan Pondok Suguh",
                safeElevationMeters = 30,
                keyTips = "Segera jauhi muara Sungai Air Berau dan pantai nelayan menuju arah perbukitan."
            ),
            EvacuationAssemblyPoint(
                districtName = "XIV Koto",
                primaryLocation = "Lapangan Sepak Bola Lubuk Sanai & Halaman Kantor Camat",
                alternativeLocation = "Area Terbuka Kompleks Agrowisata Lubuk Sanai",
                verticalEvacuationShelter = "Gedung Bertulang Kokoh Kantor Desa Lubuk Sanai Tiga",
                safeElevationMeters = 38,
                keyTips = "Jalur evakuasi bebas genangan banjir, aman dari risiko gelombang laut Samudra Hindia."
            ),
            EvacuationAssemblyPoint(
                districtName = "Sungai Rumbai",
                primaryLocation = "Lapangan Desa Retak Mudik & Kantor Camat Sungai Rumbai",
                alternativeLocation = "Halaman Terbuka Pabrik Sawit Dataran Tinggi",
                verticalEvacuationShelter = "Perbukitan Blok Perkebunan Arah Gajah Mati",
                safeElevationMeters = 32,
                keyTips = "Nelayan Pantai Gajah Mati dan Retak Ilir segera bergerak lurus ke timur melintasi Jalinsum."
            ),
            EvacuationAssemblyPoint(
                districtName = "Malin Deman",
                primaryLocation = "Lapangan Terbuka Desa Talang Baru & Halaman Kantor Camat",
                alternativeLocation = "Kompleks Terbuka Kantor Desa Serami Baru",
                verticalEvacuationShelter = "Kawasan Dataran Tinggi Perbukitan Malin Deman",
                safeElevationMeters = 55,
                keyTips = "Zona dataran tinggi aman gelombang tsunami, waspadai jalan retak di jembatan penghubung."
            ),
            EvacuationAssemblyPoint(
                districtName = "Air Rami",
                primaryLocation = "Lapangan Terbuka Desa Air Rami & Lapangan Sepak Bola",
                alternativeLocation = "Halaman Puskesmas Perawatan Air Rami",
                verticalEvacuationShelter = "Area Perbukitan Sawit Desa Rami Mulya",
                safeElevationMeters = 24,
                keyTips = "Desa pesisir Air Rami wajib lari ke arah daratan tinggi sebelum menit ke-20 setelah gempa."
            ),
            EvacuationAssemblyPoint(
                districtName = "Air Manjunto",
                primaryLocation = "Lapangan Desa Manjunto Jaya & Kantor Camat Air Manjunto",
                alternativeLocation = "Halaman Terbuka Balai Benih Ikan / Agro Mukomuko",
                verticalEvacuationShelter = "Kompleks Gedung Terpadu Air Manjunto",
                safeElevationMeters = 42,
                keyTips = "Zona penyangga darurat bagi pengungsi pesisir Kota Mukomuko dengan akses jalan lebar."
            ),
            EvacuationAssemblyPoint(
                districtName = "Selagan Raya",
                primaryLocation = "Lapangan Terbuka Desa Surian Rekang & Kantor Camat",
                alternativeLocation = "Halaman Luas Masjid Jami' Selagan Raya",
                verticalEvacuationShelter = "Perbukitan Hulu Sungai Selagan",
                safeElevationMeters = 60,
                keyTips = "Waspadai material pohon tumbang di jalan desa dan longsor tebing sungai."
            ),
            EvacuationAssemblyPoint(
                districtName = "Teras Terunjam",
                primaryLocation = "Lapangan Sepak Bola Teras Terunjam & Kantor Camat",
                alternativeLocation = "Halaman SMKN Teras Terunjam",
                verticalEvacuationShelter = "Area Terbuka Dataran Tinggi Desa Pondok Kopi",
                safeElevationMeters = 35,
                keyTips = "Jalur evakuasi lancar menuju pusat koordinasi logistik logis di Kabupaten Mukomuko."
            ),
            EvacuationAssemblyPoint(
                districtName = "V Koto",
                primaryLocation = "Lapangan Terbuka Desa Lalang Luas & Kantor Camat V Koto",
                alternativeLocation = "Halaman Terbuka SMPN V Koto",
                verticalEvacuationShelter = "Kawasan Ketinggian Arah Hulu Manjunto",
                safeElevationMeters = 48,
                keyTips = "Hindari berteduh di dekat lereng tanah labil sepanjang bantaran aliran sungai."
            )
        )
    }

    val currentAssemblyPoint = assemblyPoints.find { it.districtName.equals(selectedDistrictName, ignoreCase = true) }
        ?: assemblyPoints.first()

    // Location scenario guidance tabs
    var selectedScenarioIndex by remember { mutableIntStateOf(0) }
    val scenarioTitles = listOf(
        "🏠 Dalam Rumah",
        "🏖️ Pesisir Pantai",
        "🚗 Dalam Mobil",
        "🏢 Gedung / Kantor",
        "⛰️ Lereng Bukit"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("earthquake_evacuation_preparedness_section"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. HERO OFFICIAL HEADER BANNER ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .clickable(onClick = onOpenEvacuationGuide)
                .testTag("card_evacuation_hero_header"),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF7F1D1D) // Deep Emergency Crimson
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_bpbd_mukomuko_official),
                            contentDescription = "Logo BPBD Mukomuko",
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, Color.White, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Surface(
                                color = Color.White.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "SOP RESMI PUSDALOPS-PB BPBD",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    softWrap = false
                                )
                            }
                            Text(
                                text = "Kesiapsiagaan Evakuasi Mandiri Gempa",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                    // Direct Call 117 Button
                    Button(
                        onClick = onCall117,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.heightIn(min = 32.dp).testTag("btn_hero_call_117")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneInTalk,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "117",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFDC2626),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            softWrap = false
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Pedoman praktis dan langkah mandiri keselamatan warga saat menghadapi guncangan gempa bumi bumi dan ancaman tsunami di Kabupaten Mukomuko, Bengkulu.",
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Preparedness badge
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Panduan Kesiapsiagaan Mandiri Bencana Gempa & Tsunami",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Buka Panduan SOP Evakuasi",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // --- 2. REFLEKS EMAS SAAT GUNCANGAN: DROP - COVER - HOLD ON ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("card_drop_cover_holdon"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(OrangePrimary.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Vibration,
                            contentDescription = null,
                            tint = OrangePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Refleks 3 Langkah Utama",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                color = OrangePrimary,
                                shape = RoundedCornerShape(5.dp)
                            ) {
                                Text(
                                    text = "WAJIB HAFAL",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    softWrap = false
                                )
                            }
                            Text(
                                text = "Tindakan seketika saat bumi mulai berguncang",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 3 Steps Grid
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    StepActionItem(
                        number = "1",
                        badge = "DROP",
                        title = "Berlutut / Tiarap Seketika",
                        desc = "Segera jatuhkan tubuh ke lantai sebelum gempa menjatuhkan Anda. Posisi merangkak melindungi organ vital dan memudahkan bergerak jika runtuhan terjadi.",
                        accentColor = Color(0xFFDC2626)
                    )

                    StepActionItem(
                        number = "2",
                        badge = "COVER",
                        title = "Lindungi Kepala & Leher",
                        desc = "Masuk ke bawah meja kokoh. Jika tidak ada meja, jongkok di sudut bagian dalam dinding dan lindungi kepala memakai tas, bantal tebal, helm, atau kedua tangan.",
                        accentColor = OrangePrimary
                    )

                    StepActionItem(
                        number = "3",
                        badge = "HOLD ON",
                        title = "Bertahan & Pegang Erat",
                        desc = "Pegang kuat kaki meja tempat berlindung. Ikuti pergerakan meja jika bergeser sampai seluruh guncangan gempa BMKG benar-benar berhenti sempurna.",
                        accentColor = Color(0xFF16A34A)
                    )
                }
            }
        }

        // --- 3. SIMULASI / LATIHAN EVAKUASI MANDIRI 60 DETIK ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("card_evacuation_drill_simulator"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(
                1.dp,
                if (isDrillRunning) OrangePrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(OrangePrimary.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = OrangePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Latihan Mandiri 60 Detik (Drill)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Surface(
                            color = if (isDrillRunning) Color(0xFFDC2626) else OrangePrimary,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (isDrillRunning) "00:${drillSecondsLeft.toString().padStart(2, '0')}" else "60 Detik",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(3.dp))

                        Text(
                            text = "Latih refleks evakuasi bersama anggota keluarga",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Drill Stage Guide based on countdown
                val stageInstruction = when {
                    !isDrillRunning && !drillCompleted -> "Tekan 'Mulai Latihan' untuk menguji kesiapsiagaan refleks 3 detik Anda dan keluarga."
                    isDrillRunning && drillSecondsLeft > 40 -> "🚨 FASE 1 (Detik 0-20): GEMPA KUAT! Lakukan DROP & COVER segera! Masuk bawah meja, lindungi kepala!"
                    isDrillRunning && drillSecondsLeft > 20 -> "⚠️ FASE 2 (Detik 21-40): GEMPA SUSULAN! Lakukan HOLD ON! Pegang kaki meja erat-erat, jangan panik berlari!"
                    isDrillRunning -> "🟢 FASE 3 (Detik 41-60): GUNCANGAN BERHENTI! Periksa kondisi sekitar, matikan kompor/listrik, keluar tertib menuju titik kumpul terbuka!"
                    else -> "✅ SIMULASI SELESAI! Refleks evakuasi mandiri Anda teruji siap menghadapi situasi darurat nyata."
                }

                Surface(
                    color = if (isDrillRunning) Color(0xFFFFE4E6) else MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stageInstruction,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDrillRunning) Color(0xFF991B1B) else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Drill progress bar
                LinearProgressIndicator(
                    progress = { if (isDrillRunning) (60 - drillSecondsLeft) / 60f else if (drillCompleted) 1f else 0f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (isDrillRunning) Color(0xFFDC2626) else Color(0xFF16A34A),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Start/Stop Drill Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!isDrillRunning) {
                        Button(
                            onClick = {
                                drillSecondsLeft = 60
                                drillCompleted = false
                                isDrillRunning = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_start_drill")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Mulai Latihan Evakuasi", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                        }
                    } else {
                        Button(
                            onClick = { isDrillRunning = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_stop_drill")
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Hentikan Latihan", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            isDrillRunning = false
                            drillSecondsLeft = 60
                            drillCompleted = false
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("btn_reset_drill")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset", fontSize = 12.sp, maxLines = 1, softWrap = false)
                    }
                }
            }
        }

        // --- 4. RUMUS 20-20-20 TSUNAMI MANDIRI PESISIR MUKOMUKO ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("card_rumus_20_20_20_tsunami"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF0F172A) // Dark Oceanic Blue
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color(0xFF38BDF8).copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Surfing,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Rumus 20-20-20 Tsunami",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                color = Color(0xFFEF4444),
                                shape = RoundedCornerShape(5.dp)
                            ) {
                                Text(
                                    text = "KRUSIAL",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    softWrap = false
                                )
                            }
                            Text(
                                text = "Khas Zona Pesisir Samudra Hindia Mukomuko",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8),
                                lineHeight = 15.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Kabupaten Mukomuko membentang di pesisir Samudra Hindia dengan potensi gempa megathrust. Hafalkan rumus penyelamatan mandiri 20-20-20:",
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = Color(0xFFE2E8F0)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 20-20-20 Triplet Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TsunamiTripletCard(
                        value = "20",
                        unit = "DETIK",
                        desc = "Gempa bumi kuat berlangsung lebih dari 20 detik",
                        color = Color(0xFFF97316),
                        modifier = Modifier.weight(1f)
                    )

                    TsunamiTripletCard(
                        value = "20",
                        unit = "MENIT",
                        desc = "Waktu maksimal Anda untuk evakuasi menjauhi pantai",
                        color = Color(0xFFEF4444),
                        modifier = Modifier.weight(1f)
                    )

                    TsunamiTripletCard(
                        value = "20",
                        unit = "METER",
                        desc = "Ketinggian aman tempat evakuasi / menjauh 2 km",
                        color = Color(0xFF10B981),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "JANGAN MENUNGGU SIRENE! JANGAN MENUNGGU AIR SURUT! Lari seketika setelah getaran gempa selesai.",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFDE68A),
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }

        // --- 5. PANDUAN EVAKUASI MENURUT SITUASI & LOKASI ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("card_evacuation_scenario_guidance"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "SOP Evakuasi Menurut Lokasi",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Langkah spesifik penyelamatan di berbagai tempat",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Scenario Selector Chips
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    scenarioTitles.forEachIndexed { index, title ->
                        FilterChip(
                            selected = selectedScenarioIndex == index,
                            onClick = { selectedScenarioIndex = index },
                            label = { Text(title, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = OrangePrimary.copy(alpha = 0.2f),
                                selectedLabelColor = OrangePrimary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scenario content card
                when (selectedScenarioIndex) {
                    0 -> ScenarioContentItem(
                        title = "Di Dalam Rumah atau Bangunan Tempat Tinggal",
                        doPoints = listOf(
                            "Jangan panik dan jangan berebut lari saat getaran masih berlangsung kencang.",
                            "Tiarap dan lindungi kepala di bawah meja kokoh atau sudut ruangan yang kuat.",
                            "Jauhi cermin, lemari kaca, jendela kaca, dan lampu gantung yang rawan runtuh.",
                            "Setelah guncangan reda, matikan kompor gas dan sakelar listrik MCB utama.",
                            "Buka pintu rumah perlahan dan keluar tertib menuju ruang terbuka."
                        ),
                        dontPoints = listOf(
                            "Jangan menyalakan korek api atau lilin (waspadai kebocoran tabung gas).",
                            "Jangan kembali masuk ke dalam rumah untuk mengambil barang berharga sebelum ada info aman BPBD."
                        )
                    )
                    1 -> ScenarioContentItem(
                        title = "Di Daerah Pesisir Pantai atau Muara Sungai Mukomuko",
                        doPoints = listOf(
                            "Jika merasakan guncangan gempa kuat atau berlangsung lama (> 20 detik), segera tinggalkan pantai!",
                            "Lari mandiri menjauhi garis pantai menuju tempat yang tinggi (> 20 meter dpl) atau minimal 2 km ke darat.",
                            "Waspadai bahwa muara sungai dapat mengalirkan air tsunami dengan kecepatan sangat tinggi.",
                            "Bantu lansia, anak-anak, dan tetangga sekitar untuk segera lari ke titik kumpul aman."
                        ),
                        dontPoints = listOf(
                            "Jangan menunggu sirene peringatan atau rilis media sosial sebelum bergerak evakuasi.",
                            "Jangan menonton pantai jika melihat air laut surut tiba-tiba; tsunami segera datang dalam hitungan menit!"
                        )
                    )
                    2 -> ScenarioContentItem(
                        title = "Di Dalam Kendaraan (Mobil / Motor)",
                        doPoints = listOf(
                            "Kurangi kecepatan kendaraan secara bertahap dan tepikan di sisi kiri jalan yang lapang.",
                            "Hindari berhenti di bawah jembatan, tiang listrik, papan baliho, atau pohon tinggi.",
                            "Tarik rem tangan dan tetap berada di dalam kendaraan hingga gempa berhenti.",
                            "Jika berada di jalan pesisir pantai Mukomuko, segera tinggalkan kendaraan dan lari ke bukit terdekat jika terasa gempa besar."
                        ),
                        dontPoints = listOf(
                            "Jangan mengerem mendadak yang dapat memicu tabrakan beruntun di jalan raya.",
                            "Jangan melintasi jembatan yang tampak mengalami retakan akibat gempa."
                        )
                    )
                    3 -> ScenarioContentItem(
                        title = "Di Sekolah, Kantor Pemerintahan, atau Tempat Kerja",
                        doPoints = listOf(
                            "Ikuti arahan koordinator evakuasi bencana dan guru/pimpinan regu.",
                            "Gunakan tangga darurat dengan tertib sambil melindungi kepala dengan buku/tas.",
                            "Berjalan cepat tapi jangan saling mendorong atau berlari histeris.",
                            "Berkumpul di lapangan upacara atau area terbuka yang telah ditentukan."
                        ),
                        dontPoints = listOf(
                            "JANGAN PERNAH menggunakan lift/elevator selama dan sesaat setelah gempa.",
                            "Jangan membawa barang bawaan berat yang memperlambat laju evakuasi Anda."
                        )
                    )
                    4 -> ScenarioContentItem(
                        title = "Di Daerah Perbukitan, Lereng, atau Tebing",
                        doPoints = listOf(
                            "Waspadai potensi tanah longsor dan runtuhan bebatuan dari tebing bukit.",
                            "Segera menjauhi lereng yang curam menuju punggungan bukit atau lembah datar yang aman.",
                            "Perhatikan tanda-tanda air sungai mendadak keruh atau bercampur lumpur tebal."
                        ),
                        dontPoints = listOf(
                            "Jangan berdiri tepat di bawah tebing terjal yang memiliki retakan tanah.",
                            "Jangan melintasi alur sungai yang berpotensi banjir lahar dingin atau bandang pasca gempa."
                        )
                    )
                }
            }
        }

        // --- 6. TITIK KUMPUL EVAKUASI RESMI 15 KECAMATAN MUKOMUKO ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("card_assembly_points_district"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF16A34A).copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Titik Kumpul Evakuasi Resmi",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                color = Color(0xFF16A34A).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(5.dp)
                            ) {
                                Text(
                                    text = "15 KECAMATAN",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF16A34A),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    softWrap = false
                                )
                            }
                            Text(
                                text = "Lokasi kumpul aman per kecamatan di Mukomuko",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // District selector chips
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    assemblyPoints.forEach { ap ->
                        FilterChip(
                            selected = selectedDistrictName.equals(ap.districtName, ignoreCase = true),
                            onClick = { selectedDistrictName = ap.districtName },
                            label = { Text(ap.districtName, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF16A34A).copy(alpha = 0.2f),
                                selectedLabelColor = Color(0xFF16A34A)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Detailed Assembly Point Info
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Kecamatan ${currentAssemblyPoint.districtName}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        InfoRowItem(label = "Titik Kumpul Utama:", value = currentAssemblyPoint.primaryLocation)
                        InfoRowItem(label = "Lokasi Alternatif:", value = currentAssemblyPoint.alternativeLocation)
                        InfoRowItem(label = "Shelter Evakuasi Vertikal (TES):", value = currentAssemblyPoint.verticalEvacuationShelter)
                        InfoRowItem(label = "Ketinggian Aman Wilayah:", value = "± ${currentAssemblyPoint.safeElevationMeters} meter dpl")

                        Spacer(modifier = Modifier.height(6.dp))

                        Surface(
                            color = Color(0xFFFEF3C7),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Panduan Khusus: ${currentAssemblyPoint.keyTips}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF92400E),
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }
        }

        // --- 7. TAS SIAGA BENCANA (EMERGENCY GO-BAG 72 JAM) CHECKLIST ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("card_emergency_bag_checklist"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(OrangeSecondary.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.HealthAndSafety,
                            contentDescription = null,
                            tint = OrangeSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Tas Siaga Bencana (72 Jam)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                color = if (readyPercentage >= 80) Color(0xFF16A34A) else OrangePrimary,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "$readyPercentage% Siaga",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                            Text(
                                text = "Cek perlengkapan wajib penyelamatan keluarga",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                lineHeight = 15.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Progress Bar
                LinearProgressIndicator(
                    progress = { readyPercentage / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (readyPercentage >= 80) Color(0xFF16A34A) else OrangePrimary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "$readyCount dari ${bagItemsList.size} perlengkapan siaga telah disiapkan:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Checklist Items
                bagItemsList.forEachIndexed { index, itemText ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { checkedItems[index] = !checkedItems[index] }
                            .padding(vertical = 4.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = checkedItems[index],
                            onCheckedChange = { checkedItems[index] = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = OrangePrimary,
                                checkmarkColor = Color.White
                            ),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = itemText,
                            fontSize = 12.sp,
                            color = if (checkedItems[index]) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                            fontWeight = if (checkedItems[index]) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        // --- 8. TEST NOTIFIKASI DARURAT SISTEM (TSUNAMI & CUACA EKSTREM) ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("card_test_emergency_notifications"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, OrangePrimary.copy(alpha = 0.35f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(OrangePrimary.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = OrangePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Pengujian Sistem Notifikasi Darurat",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Pastikan ponsel Anda dapat menerima getaran & suara sirine",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Test Tsunami Alert Button
                    Button(
                        onClick = { onTestTsunamiAlert(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_test_tsunami_notification")
                    ) {
                        Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Tes Alarm Tsunami", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                    }

                    // Test Severe Weather Alert (Orange Theme)
                    Button(
                        onClick = { onTestSevereWeatherAlert(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_test_orange_weather_alert")
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Tes Notifikasi Oranye", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                    }
                }
            }
        }

        // --- 9. NOMOR KONTAK DARURAT RESMI MUKOMUKO ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("card_emergency_contacts_mukomuko"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
            border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFFDC2626), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneInTalk,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Posko & Kontak Darurat 24 Jam",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF991B1B)
                        )
                        Text(
                            text = "Hubungi segera jika terjadi korban atau kerusakan gempa",
                            fontSize = 11.sp,
                            color = Color(0xFF7F1D1D)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                EmergencyContactItem(
                    name = "Call Center 117 Pusdalops BPBD",
                    number = "117",
                    desc = "Bebas Pulsa 24 Jam Non-Stop",
                    onDial = onCall117
                )

                Spacer(modifier = Modifier.height(8.dp))

                EmergencyContactItem(
                    name = "Nomor Tunggal Panggilan Darurat 112",
                    number = "112",
                    desc = "Dikelola Dinas Kominfo Kab. Mukomuko (Bebas Pulsa 24 Jam)",
                    onDial = {
                        val dialIntent = android.content.Intent(android.content.Intent.ACTION_DIAL, android.net.Uri.parse("tel:112"))
                        dialIntent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                        context.startActivity(dialIntent)
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                EmergencyContactItem(
                    name = "Basarnas / SAR Bengkulu Pos Mukomuko",
                    number = "115",
                    desc = "Pencarian & Penyelamatan Korban Terjebak",
                    onDial = {
                        val dialIntent = android.content.Intent(android.content.Intent.ACTION_DIAL, android.net.Uri.parse("tel:115"))
                        dialIntent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                        context.startActivity(dialIntent)
                    }
                )
            }
        }
    }
}

@Composable
private fun StepActionItem(
    number: String,
    badge: String,
    title: String,
    desc: String,
    accentColor: Color
) {
    Surface(
        color = accentColor.copy(alpha = 0.08f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(accentColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = number,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = accentColor,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = badge,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = desc,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@Composable
private fun TsunamiTripletCard(
    value: String,
    unit: String,
    desc: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF1E293B),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = color
            )
            Text(
                text = unit,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = desc,
                fontSize = 11.sp,
                color = Color(0xFFCBD5E1),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 13.sp
            )
        }
    }
}

@Composable
private fun ScenarioContentItem(
    title: String,
    doPoints: List<String>,
    dontPoints: List<String>
) {
    Column {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = OrangePrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "LAKUKAN (Tindakan Dianjurkan):",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF16A34A)
        )

        Spacer(modifier = Modifier.height(4.dp))

        doPoints.forEach { point ->
            Row(
                modifier = Modifier.padding(vertical = 2.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(text = "✓ ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                Text(
                    text = point,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "HINDARI (Jangan Dilakukan):",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFDC2626)
        )

        Spacer(modifier = Modifier.height(4.dp))

        dontPoints.forEach { point ->
            Row(
                modifier = Modifier.padding(vertical = 2.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(text = "✕ ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                Text(
                    text = point,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@Composable
private fun InfoRowItem(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 3.dp)) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun EmergencyContactItem(
    name: String,
    number: String,
    desc: String,
    onDial: () -> Unit
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF991B1B)
                )
                Text(
                    text = "$number • $desc",
                    fontSize = 11.sp,
                    color = Color(0xFF7F1D1D)
                )
            }

            Button(
                onClick = onDial,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.heightIn(min = 30.dp)
            ) {
                Icon(Icons.Default.PhoneInTalk, contentDescription = null, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Panggil",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false
                )
            }
        }
    }
}
