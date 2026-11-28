package com.example.ui.components

import android.media.AudioAttributes
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.OfflineEvacuationSopEntity
import com.example.data.repository.CitizenPreparednessRepository
import com.example.ui.theme.OrangePrimary
import com.example.util.EmergencyTonePlayer
import com.example.util.IndonesianTtsHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Model data statis untuk setiap item bencana pada SOP Evakuasi Mandiri (100% Offline).
 */
data class PanduanEvakuasiBencana(
    val id: String,
    val namaBencana: String,
    val subJudul: String,
    val rumusUtama: String,
    val icon: ImageVector,
    val warnaAksen: Color,
    val langkahEvakuasi: List<LangkahEvakuasi>,
    val catatanPenting: String
)

data class LangkahEvakuasi(
    val nomor: Int,
    val judulLangkah: String,
    val deskripsi: String
)

/**
 * ============================================================================
 * PUSAT DATA TEKS PANDUAN EVAKUASI MANDIRI (100% OFFLINE)
 * ============================================================================
 * Anda dapat mengubah teks atau menambahkan jenis bencana baru langsung di dalam
 * list `DAFTAR_PANDUAN_EVAKUASI` di bawah ini tanpa memerlukan koneksi internet.
 */
val DAFTAR_PANDUAN_EVAKUASI: List<PanduanEvakuasiBencana> = listOf(
    PanduanEvakuasiBencana(
        id = "gempa_bumi",
        namaBencana = "Gempa Bumi",
        subJudul = "Prosedur Penyelamatan Diri Saat Guncangan",
        rumusUtama = "Prinsip Utama: DROP • COVER • HOLD ON",
        icon = Icons.Default.Public,
        warnaAksen = Color(0xFFEA580C), // Oranye BPBD
        langkahEvakuasi = listOf(
            LangkahEvakuasi(
                nomor = 1,
                judulLangkah = "DROP (Merunduk)",
                deskripsi = "Segera jatuhkan tubuh ke posisi merangkak/bertumpu pada tangan dan lutut sebelum guncangan gempa menjatuhkan Anda."
            ),
            LangkahEvakuasi(
                nomor = 2,
                judulLangkah = "COVER (Lindungi Kepala & Leher)",
                deskripsi = "Berlindung di bawah meja yang kokoh. Jika tidak ada meja, lindungi kepala dan leher dengan kedua tangan/bantal serta merapat ke dinding bagian dalam yang jauh dari kaca."
            ),
            LangkahEvakuasi(
                nomor = 3,
                judulLangkah = "HOLD ON (Bertahan)",
                deskripsi = "Pegang erat kaki meja atau pelindung Anda hingga guncangan benar-benar berhenti. Jangan berlari keluar saat bangunan masih berguncang."
            ),
            LangkahEvakuasi(
                nomor = 4,
                judulLangkah = "Evakuasi Setelah Guncangan Berhenti",
                deskripsi = "Keluar dengan tertib melalui jalur evakuasi menuju Titik Kumpul terbuka. Jauhi kaca, tiang listrik, pohon tinggi, dan papan reklame. Gunakan tangga darurat, jangan gunakan lift."
            )
        ),
        catatanPenting = "Waspadai gempa susulan. Jika berada di wilayah pesisir Kabupaten Mukomuko dan gempa terasa kuat atau berlangsung lama, segera bersiap melakukan evakuasi tsunami."
    ),
    PanduanEvakuasiBencana(
        id = "tsunami",
        namaBencana = "Tsunami",
        subJudul = "Evakuasi Cepat Wilayah Pesisir Pantai Mukomuko",
        rumusUtama = "Rumus BNPB/BPBD: 20 - 20 - 20",
        icon = Icons.Default.Tsunami,
        warnaAksen = Color(0xFFD97706), // Oranye Amber
        langkahEvakuasi = listOf(
            LangkahEvakuasi(
                nomor = 1,
                judulLangkah = "20 Detik Pertama (Kenali Guncangan)",
                deskripsi = "Jika merasakan gempa bumi yang berlangsung selama 20 detik atau lebih (atau guncangan sangat kuat hingga sulit berdiri), JANGAN tunggu peringatan sirine resmi."
            ),
            LangkahEvakuasi(
                nomor = 2,
                judulLangkah = "20 Menit Waktu Evakuasi",
                deskripsi = "Anda memiliki waktu sekitar 20 menit sebelum gelombang tsunami berpotensi tiba di garis pantai. Segera tinggalkan pantai dan lari menuju tempat tinggi dengan membawa Tas Siaga Bencana."
            ),
            LangkahEvakuasi(
                nomor = 3,
                judulLangkah = "20 Meter Ketinggian Minimal",
                deskripsi = "Evakuasi diri dan keluarga ke dataran tinggi atau bukit evakuasi dengan ketinggian minimal 20 meter di atas permukaan laut (MDPL), atau menjauh minimal 2–3 km dari bibir pantai."
            ),
            LangkahEvakuasi(
                nomor = 4,
                judulLangkah = "Waspadai Tanda Alam Air Laut Surut",
                deskripsi = "Jika melihat air laut surut secara tiba-tiba atau terdengar suara gemuruh keras dari arah laut, segera berlari ke dataran tinggi dan jauhi aliran sungai yang bermuara ke laut."
            )
        ),
        catatanPenting = "Utamakan evakuasi berjalan kaki atau berlari mengikuti rambu jalur evakuasi agar tidak terjebak kemacetan kendaraan. Tetap di tempat tinggi hingga ada pernyataan aman resmi dari BMKG/BPBD."
    ),
    PanduanEvakuasiBencana(
        id = "banjir",
        namaBencana = "Banjir",
        subJudul = "Langkah Penyelamatan Saat Luapan Sungai & Genangan",
        rumusUtama = "Prioritas: Matikan Listrik • Amankan Dokumen • Evakuasi",
        icon = Icons.Default.Flood,
        warnaAksen = Color(0xFFEA580C), // Oranye BPBD
        langkahEvakuasi = listOf(
            LangkahEvakuasi(
                nomor = 1,
                judulLangkah = "Matikan Aliran Listrik (MCB) & Gas",
                deskripsi = "Segera matikan sakelar utama listrik (MCB) di rumah dan lepaskan regulator tabung gas LPG untuk mencegah risiko tersengat listrik atau kebakaran saat air mulai naik."
            ),
            LangkahEvakuasi(
                nomor = 2,
                judulLangkah = "Amankan Dokumen & Tas Siaga Bencana",
                deskripsi = "Masukkan dokumen penting (KTP, KK, Ijazah, Surat Tanah), obat-obatan pribadi, pakaian ganti, dan senter ke dalam wadah plastik kedap air di Tas Siaga Bencana."
            ),
            LangkahEvakuasi(
                nomor = 3,
                judulLangkah = "Evakuasi Sebelum Air Terlalu Tinggi",
                deskripsi = "Segera mengungsi ke posko evakuasi atau dataran yang lebih tinggi sebelum ketinggian air menyulitkan pergerakan. Prioritaskan lansia, anak-anak, ibu hamil, dan penyandang disabilitas."
            ),
            LangkahEvakuasi(
                nomor = 4,
                judulLangkah = "Hindari Arus Deras & Genangan Dalam",
                deskripsi = "Jangan nekat menerobos jalanan atau jembatan yang tergenang arus deras (baik berjalan kaki maupun berkendara). Gunakan tongkat untuk mengecek kedalaman jalan di depan Anda."
            )
        ),
        catatanPenting = "Pantau terus kenaikan debit air sungai (seperti DAS Selagan, Manjuto, dan Air Dikit) saat hujan lebat berlangsung lebih dari 2 jam di wilayah hulu dan hilir."
    ),
    PanduanEvakuasiBencana(
        id = "tanah_longsor",
        namaBencana = "Tanah Longsor",
        subJudul = "Kewaspadaan Daerah Tebing & Perbukitan",
        rumusUtama = "Prinsip: Kenali Retakan • Menjauh Tegak Lurus Jalur Longsor",
        icon = Icons.Default.Landscape,
        warnaAksen = Color(0xFFC2410C), // Oranye Tua
        langkahEvakuasi = listOf(
            LangkahEvakuasi(
                nomor = 1,
                judulLangkah = "Kenali Tanda Awal Longsoran",
                deskripsi = "Waspadai munculnya retakan baru di tanah/dinding rumah, pohon atau tiang listrik yang mulai miring, mata air yang mendadak keruh, atau suara gemuruh dari atas tebing saat/setelah hujan lebat."
            ),
            LangkahEvakuasi(
                nomor = 2,
                judulLangkah = "Segera Keluar & Menjauh Secara Tegak Lurus",
                deskripsi = "Jika terdengar gemuruh longsor, segera keluar dari bangunan dan berlari ke arah samping (tegak lurus) menjauhi jalur luncuran material longsor, bukan searah dengan turunan tebing."
            ),
            LangkahEvakuasi(
                nomor = 3,
                judulLangkah = "Posisi Melingkar (Bila Terjebak)",
                deskripsi = "Apabila tidak sempat menghindar dari jangkauan material longsor, segera gulung tubuh seperti bola dengan melindungi kepala menggunakan kedua tangan."
            ),
            LangkahEvakuasi(
                nomor = 4,
                judulLangkah = "Jangan Kembali ke Lokasi Longsoran",
                deskripsi = "Setelah longsor pertama berhenti, jangan langsung masuk kembali ke area longsoran karena tanah masih labil dan sangat berisiko terjadi longsor susulan."
            )
        ),
        catatanPenting = "Warga di kawasan perbukitan atau dekat lereng tebing diimbau melakukan evakuasi mandiri lebih awal apabila turun hujan deras terus-menerus pada malam hari."
    )
)

private fun OfflineEvacuationSopEntity.toPanduanEvakuasiBencana(): PanduanEvakuasiBencana {
    val (icon, accentColor) = when (id) {
        "gempa_bumi" -> Icons.Default.Public to Color(0xFFEA580C)
        "tsunami" -> Icons.Default.Tsunami to Color(0xFFD97706)
        "banjir" -> Icons.Default.Flood to Color(0xFFEA580C)
        "tanah_longsor" -> Icons.Default.Landscape to Color(0xFFC2410C)
        else -> Icons.Default.HealthAndSafety to OrangePrimary
    }
    val parsedSteps = CitizenPreparednessRepository.parseSopSteps(langkahJson).map {
        LangkahEvakuasi(
            nomor = it.nomor,
            judulLangkah = it.judulLangkah,
            deskripsi = it.deskripsi
        )
    }
    return PanduanEvakuasiBencana(
        id = id,
        namaBencana = namaBencana,
        subJudul = subJudul,
        rumusUtama = rumusUtama,
        icon = icon,
        warnaAksen = accentColor,
        langkahEvakuasi = parsedSteps,
        catatanPenting = catatanPenting
    )
}

/**
 * Layar Panduan SOP Evakuasi Mandiri (100% Offline) — Fase Pra-Bencana.
 * Terintegrasi penuh dengan SQLite Room (`offline_evacuation_sop`), Text-to-Speech Bahasa Indonesia,
 * dan tombol Tes Alarm Sirene Lokal.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanduanEvakuasiScreen(
    onBackClick: () -> Unit,
    onCall117: () -> Unit = {}
) {
    // Pastikan tombol Back sistem Android menutup layar panduan ini
    BackHandler(onBack = onBackClick)

    val context = LocalContext.current
    val preparednessRepo = remember(context) { CitizenPreparednessRepository.fromContext(context) }
    val dbSops by preparednessRepo.evacuationSopsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    LaunchedEffect(preparednessRepo) {
        preparednessRepo.ensureSeeded()
    }

    val daftarPanduan = remember(dbSops) {
        if (dbSops.isNotEmpty()) {
            dbSops.map { it.toPanduanEvakuasiBencana() }
        } else {
            DAFTAR_PANDUAN_EVAKUASI
        }
    }

    // Text-to-Speech Bahasa Indonesia 100% Offline
    val ttsHelper = remember(context) { IndonesianTtsHelper(context) }
    val activeSpeakingKey by ttsHelper.activeSpeakingKey.collectAsStateWithLifecycle()

    // Tes Alarm Sirene Lokal
    val coroutineScope = rememberCoroutineScope()
    var isTestingLocalSiren by remember { mutableStateOf(false) }
    var sirenTestJob by remember { mutableStateOf<Job?>(null) }

    DisposableEffect(ttsHelper) {
        onDispose {
            sirenTestJob?.cancel()
            EmergencyTonePlayer.stop()
            ttsHelper.shutdown()
        }
    }

    // Menyimpan ID bencana yang sedang dibuka (default: buka item pertama agar pengguna langsung melihat contohnya)
    var expandedIds by remember { mutableStateOf(setOf("gempa_bumi")) }
    val adaptiveInsets = rememberAdaptiveSystemBarInsets()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = Modifier
            .fillMaxSize()
            .testTag("panduan_evakuasi_screen"),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(top = adaptiveInsets.topBarInset),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "SOP Evakuasi Mandiri",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = "Panduan Resmi BNPB/BPBD • 100% Offline",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("btn_back_panduan_evakuasi")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali ke Halaman Utama",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = OrangePrimary
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 24.dp + adaptiveInsets.effectiveBottomInset),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Banner Informasi Offline
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Transparent
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFFEA580C),
                                        Color(0xFFF97316)
                                    )
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Surface(
                                    color = Color.White,
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.OfflinePin,
                                            contentDescription = null,
                                            tint = OrangePrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "TERSEDIA TANPA INTERNET",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = OrangePrimary,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }

                                Text(
                                    text = "Fase Pra-Bencana",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Buku Saku Keselamatan Warga Mukomuko",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tersimpan di basis data SQLite lokal (100% offline). Ketuk kartu bencana untuk membaca atau tekan ikon Speaker untuk mendengarkan panduan suara Bahasa Indonesia.",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.95f),
                                lineHeight = 17.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        if (isTestingLocalSiren) {
                                            isTestingLocalSiren = false
                                            sirenTestJob?.cancel()
                                            sirenTestJob = null
                                            EmergencyTonePlayer.stop()
                                        } else {
                                            ttsHelper.stop()
                                            isTestingLocalSiren = true
                                            sirenTestJob?.cancel()
                                            sirenTestJob = coroutineScope.launch {
                                                try {
                                                    while (isActive) {
                                                        EmergencyTonePlayer.playToneSuspend(
                                                            freqHz1 = 920.0,
                                                            freqHz2 = 1240.0,
                                                            durationMs = 550,
                                                            usage = AudioAttributes.USAGE_ALARM,
                                                            volume = 0.85f
                                                        )
                                                        delay(180L)
                                                        EmergencyTonePlayer.playToneSuspend(
                                                            freqHz1 = 680.0,
                                                            freqHz2 = 960.0,
                                                            durationMs = 450,
                                                            usage = AudioAttributes.USAGE_ALARM,
                                                            volume = 0.85f
                                                        )
                                                        delay(220L)
                                                    }
                                                } finally {
                                                    EmergencyTonePlayer.stop()
                                                }
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isTestingLocalSiren) Color(0xFFB91C1C) else Color.White,
                                        contentColor = if (isTestingLocalSiren) Color.White else OrangePrimary
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("btn_test_local_siren_sop")
                                ) {
                                    Icon(
                                        imageVector = if (isTestingLocalSiren) Icons.Default.StopCircle else Icons.Default.Campaign,
                                        contentDescription = if (isTestingLocalSiren) "Hentikan Tes Alarm Sirene" else "Tes Alarm Sirene Lokal",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isTestingLocalSiren) "Hentikan Tes Sirene" else "Tes Alarm Sirene",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }

                                if (activeSpeakingKey != null) {
                                    OutlinedButton(
                                        onClick = { ttsHelper.stop() },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.8f)),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                        modifier = Modifier
                                            .heightIn(min = 48.dp)
                                            .testTag("btn_stop_tts_global")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.VolumeOff,
                                            contentDescription = "Hentikan Pembacaan Suara",
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Stop Suara",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Daftar Accordion / ExpandableCard Bencana (Bersumber dari SQLite offline_evacuation_sop)
            items(daftarPanduan, key = { it.id }) { itemBencana ->
                val isExpanded = expandedIds.contains(itemBencana.id)
                ExpandablePanduanBencanaCard(
                    bencana = itemBencana,
                    isExpanded = isExpanded,
                    activeSpeakingKey = activeSpeakingKey,
                    onSpeakToggle = { key, text ->
                        if (isTestingLocalSiren) {
                            isTestingLocalSiren = false
                            sirenTestJob?.cancel()
                            sirenTestJob = null
                            EmergencyTonePlayer.stop()
                        }
                        ttsHelper.toggleSpeak(key, text)
                    },
                    onToggle = {
                        expandedIds = if (isExpanded) {
                            expandedIds - itemBencana.id
                        } else {
                            expandedIds + itemBencana.id
                        }
                    }
                )
            }

            // Tombol Bantuan Darurat Pusdalops di bagian bawah
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = OrangePrimary.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, OrangePrimary.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Butuh Pertolongan Darurat?",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Hubungi Call Center Bebas Pulsa 117 / Pusdalops BPBD",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onCall117,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = OrangePrimary,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("btn_call_117_panduan")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhoneInTalk,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "117",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpandablePanduanBencanaCard(
    bencana: PanduanEvakuasiBencana,
    isExpanded: Boolean,
    activeSpeakingKey: String?,
    onSpeakToggle: (key: String, text: String) -> Unit,
    onToggle: () -> Unit
) {
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "accordion_arrow_rotation"
    )
    val fullSpeechKey = "sop_all_${bencana.id}"
    val isReadingAll = activeSpeakingKey == fullSpeechKey

    Card(
        onClick = onToggle,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("accordion_bencana_${bencana.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (isExpanded) 1.5.dp else 1.dp,
            color = if (isExpanded) OrangePrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Kartu (Selalu Tampil)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isExpanded) OrangePrimary.copy(alpha = 0.08f)
                        else Color.Transparent
                    )
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Ikon Bencana dengan Latar Oranye/Putih
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isExpanded) OrangePrimary else OrangePrimary.copy(alpha = 0.12f),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = bencana.icon,
                                contentDescription = bencana.namaBencana,
                                tint = if (isExpanded) Color.White else OrangePrimary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = bencana.namaBencana,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = bencana.subJudul,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Ikon Panah Buka/Tutup
                    Surface(
                        shape = CircleShape,
                        color = OrangePrimary.copy(alpha = 0.1f),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.ExpandMore,
                                contentDescription = if (isExpanded) "Tutup panduan" else "Buka panduan",
                                tint = OrangePrimary,
                                modifier = Modifier
                                    .size(22.dp)
                                    .rotate(rotationAngle)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Badge Rumus / Prinsip Utama Full-Width agar tidak terhimpit di antara ikon & tombol panah
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = OrangePrimary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, OrangePrimary.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = OrangePrimary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = bencana.rumusUtama,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = OrangePrimary,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            // Isi Langkah-Langkah Evakuasi (Muncul Saat Diklik)
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HorizontalDivider(color = OrangePrimary.copy(alpha = 0.2f))

                    // Tombol Bacakan Seluruh SOP Bencana via TTS Bahasa Indonesia
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Panduan Audio Offline (TTS Bahasa Indonesia)",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 15.sp,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 10.dp)
                        )
                        FilledTonalButton(
                            onClick = {
                                val fullScript = buildString {
                                    append("Panduan Evakuasi ")
                                    append(bencana.namaBencana)
                                    append(". ")
                                    append(bencana.rumusUtama)
                                    append(". ")
                                    bencana.langkahEvakuasi.forEach { step ->
                                        append("Langkah ${step.nomor}: ${step.judulLangkah}. ${step.deskripsi} ")
                                    }
                                    append("Catatan penting: ${bencana.catatanPenting}")
                                }
                                onSpeakToggle(fullSpeechKey, fullScript)
                            },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (isReadingAll) Color(0xFFB91C1C) else OrangePrimary.copy(alpha = 0.15f),
                                contentColor = if (isReadingAll) Color.White else OrangePrimary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .heightIn(min = 44.dp)
                                .testTag("btn_tts_all_${bencana.id}")
                        ) {
                            Icon(
                                imageVector = if (isReadingAll) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = if (isReadingAll) "Hentikan Pembacaan ${bencana.namaBencana}" else "Bacakan Semua Langkah ${bencana.namaBencana}",
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isReadingAll) "Hentikan" else "Bacakan Semua",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    bencana.langkahEvakuasi.forEach { langkah ->
                        val stepSpeechKey = "sop_step_${bencana.id}_${langkah.nomor}"
                        val isStepSpeaking = activeSpeakingKey == stepSpeechKey
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            // Badge Nomor Langkah Berwarna Oranye
                            Surface(
                                shape = CircleShape,
                                color = OrangePrimary,
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .size(26.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = langkah.nomor.toString(),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = langkah.judulLangkah,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = langkah.deskripsi,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 18.sp
                                )
                            }

                            IconButton(
                                onClick = {
                                    onSpeakToggle(
                                        stepSpeechKey,
                                        "Langkah ${langkah.nomor}: ${langkah.judulLangkah}. ${langkah.deskripsi}"
                                    )
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("btn_tts_step_${bencana.id}_${langkah.nomor}")
                            ) {
                                Icon(
                                    imageVector = if (isStepSpeaking) Icons.Default.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = if (isStepSpeaking) {
                                        "Hentikan suara langkah ${langkah.nomor}"
                                    } else {
                                        "Bacakan langkah ${langkah.nomor}: ${langkah.judulLangkah}"
                                    },
                                    tint = if (isStepSpeaking) Color(0xFFB91C1C) else OrangePrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    // Kotak Catatan Penting BPBD
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = OrangePrimary.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, OrangePrimary.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = OrangePrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = bencana.catatanPenting,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
