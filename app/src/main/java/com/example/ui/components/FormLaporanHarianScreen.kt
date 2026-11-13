package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AppDatabase
import com.example.data.local.LaporanHarian
import com.example.data.local.LogKomunikasi
import com.example.data.local.OfficialAsnBpbdPersonnel
import com.example.util.SessionManager
import com.example.ui.theme.OrangeOnPrimary
import com.example.ui.theme.OrangePrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Komponen Jetpack Compose Form Laporan Harian Pusdalops BPBD Mukomuko
 * Mengimplementasikan arsitektur layout multi-tahap (ViewPager/Tab) sesuai blueprint XML.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormLaporanHarianScreen(
    database: AppDatabase,
    onClose: () -> Unit,
    onSavedSuccess: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var currentStep by remember { mutableIntStateOf(0) }
    val stepTitles = listOf(
        "1. Kejadian",
        "2. Dampak & Korban",
        "3. Upaya & Kondisi",
        "4. Administrasi & Log"
    )

    val todayFormatted = remember {
        SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")).format(Date())
    }
    val timeFormatted = remember {
        SimpleDateFormat("HH:mm 'WIB'", Locale.getDefault()).format(Date())
    }

    // --- State Step 1: Data Kejadian ---
    var tanggalLaporan by remember { mutableStateOf(todayFormatted) }
    var jenisBencana by remember { mutableStateOf("Gempa Bumi Tektonik") }
    var tanggalKejadian by remember { mutableStateOf(todayFormatted) }
    var waktuKejadian by remember { mutableStateOf(timeFormatted) }
    var kedalaman by remember { mutableStateOf("10 km") }
    var lokasi by remember { mutableStateOf("Kecamatan Kota Mukomuko, Pesisir Samudra Hindia") }
    var koordinat by remember { mutableStateOf("-2.5830, 101.1240") }
    var kondisiCuaca by remember { mutableStateOf("Berawan") }

    // --- State Step 2: Dampak & Korban ---
    var cakupanDampak by remember { mutableStateOf("3 Desa pesisir terdampak getaran dan genangan pasang air laut") }
    var korbanLuka by remember { mutableIntStateOf(0) }
    var korbanMeninggal by remember { mutableIntStateOf(0) }
    var kerusakan by remember { mutableStateOf("Nihil korban jiwa. Retak rambut pada 1 pos ronda dan 2 perahu katir terseret ombak.") }
    var penyebab by remember { mutableStateOf("Aktivitas subduksi megathrust lempeng tektonik Samudra Hindia dan pasang purnama laut") }

    // --- State Step 3: Upaya & Kondisi ---
    var upayaDilakukan by remember { mutableStateOf("Tim Reaksi Cepat (TRC) Pusdalops BPBD Mukomuko melakukan kaji cepat (rapid assessment), monitoring pesisir Muara Manjunto, dan koordinasi dengan pos ronda desa.") }
    var kondisiMutakhir by remember { mutableStateOf("Situasi kondusif, air pasang telah surut, warga kembali beraktivitas melaut dengan waspada.") }
    var sumberInformasi by remember { mutableStateOf("BMKG Stasiun Geofisika Kepahiang & Laporan Camat Kota Mukomuko") }

    // --- State Step 4: Administrasi & Log Komunikasi ---
    val context = LocalContext.current
    val session = remember { SessionManager(context) }
    var tanggalSurat by remember { mutableStateOf(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())) }
    var tanggalTerima by remember { mutableStateOf(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())) }
    var operatorName by remember { mutableStateOf(session.getNamaPetugas().ifBlank { "Rahmat Hidayat (Operator Utama)" }) }
    var stationId by remember { mutableStateOf("PUSDALOPS-MM-01") }

    // Temporary list for Log Komunikasi (Relasi One-to-Many)
    val logsList = remember {
        mutableStateListOf(
            LogKomunikasi(
                id = 0,
                laporanId = 0,
                waktu = timeFormatted,
                stationIdFrom = "Radio VHF Ch.1",
                stationIdTo = "Kalaksa BPBD Mukomuko",
                subjek = "Laporan awal kejadian disampaikan. Tidak terdeteksi potensi tsunami.",
                operatorNama = session.getNamaPetugas(),
                operatorNip = session.getNipPetugas()
            )
        )
    }

    var isAddingLogDialog by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var saveSuccessDialog by remember { mutableStateOf(false) }
    var savedId by remember { mutableIntStateOf(0) }

    val commonDisasterTypes = listOf(
        "Gempa Bumi Tektonik",
        "Banjir Luapan Sungai",
        "Gelombang Pasang / Rob",
        "Cuaca Ekstrem / Badai",
        "Tanah Longsor",
        "Kebakaran Hutan & Lahan"
    )

    val weatherOptions = listOf("Cerah", "Cerah Berawan", "Berawan", "Hujan Ringan", "Hujan Lebat", "Angin Kencang")

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("form_laporan_harian_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Form Laporan Harian",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.testTag("btn_close_form")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1976D2)
                )
            )
        },
        bottomBar = {
            // Tombol Navigasi Bawah sesuai Layout XML
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStep > 0) {
                        OutlinedButton(
                            onClick = { currentStep -= 1 },
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                                .testTag("btnSebelumnya"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Kembali", fontSize = 14.sp)
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f).padding(end = 8.dp))
                    }

                    Button(
                        onClick = {
                            if (currentStep < 3) {
                                currentStep += 1
                            } else {
                                // Save to Room Database
                                isSaving = true
                                scope.launch(Dispatchers.IO) {
                                    val newLaporan = LaporanHarian(
                                        tanggalLaporan = tanggalLaporan,
                                        jenisBencana = jenisBencana,
                                        tanggalKejadian = tanggalKejadian,
                                        waktuKejadian = waktuKejadian,
                                        kedalaman = kedalaman,
                                        lokasi = lokasi,
                                        koordinat = koordinat,
                                        cakupanDampak = cakupanDampak,
                                        kondisiCuaca = kondisiCuaca,
                                        upayaDilakukan = upayaDilakukan,
                                        kondisiMutakhir = kondisiMutakhir,
                                        korbanLuka = korbanLuka,
                                        korbanMeninggal = korbanMeninggal,
                                        kerusakan = kerusakan,
                                        penyebab = penyebab,
                                        sumberInformasi = sumberInformasi,
                                        tanggalSurat = tanggalSurat,
                                        tanggalTerima = tanggalTerima,
                                        operatorName = operatorName,
                                        stationId = stationId
                                    )
                                    val insertedLaporanId = database.laporanHarianDao().insertLaporan(newLaporan).toInt()

                                    // Insert associated communication logs
                                    val logEntities = logsList.map {
                                        it.copy(id = 0, laporanId = insertedLaporanId)
                                    }
                                    if (logEntities.isNotEmpty()) {
                                        database.laporanHarianDao().insertLogKomunikasiList(logEntities)
                                    }

                                    withContext(Dispatchers.Main) {
                                        isSaving = false
                                        savedId = insertedLaporanId
                                        saveSuccessDialog = true
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp)
                            .testTag("btnSelanjutnya"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (currentStep == 3) "Simpan Laporan" else "Selanjutnya",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = if (currentStep == 3) Icons.Default.Save else Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF5F5F5))
        ) {
            // Tab Menu TabLayout
            ScrollableTabRow(
                selectedTabIndex = currentStep,
                containerColor = Color.White,
                contentColor = Color(0xFFFF9800),
                edgePadding = 12.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[currentStep]),
                        color = Color(0xFFFF9800),
                        height = 3.dp
                    )
                }
            ) {
                stepTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = currentStep == index,
                        onClick = { currentStep = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = if (currentStep == index) FontWeight.Bold else FontWeight.Medium,
                                color = if (currentStep == index) Color(0xFFFF9800) else Color(0xFF757575),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    )
                }
            }

            // Body Form Container
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when (currentStep) {
                    0 -> Step1DataKejadian(
                        tanggalLaporan = tanggalLaporan,
                        onTanggalLaporanChange = { tanggalLaporan = it },
                        jenisBencana = jenisBencana,
                        onJenisBencanaChange = { jenisBencana = it },
                        commonDisasterTypes = commonDisasterTypes,
                        tanggalKejadian = tanggalKejadian,
                        onTanggalKejadianChange = { tanggalKejadian = it },
                        waktuKejadian = waktuKejadian,
                        onWaktuKejadianChange = { waktuKejadian = it },
                        kedalaman = kedalaman,
                        onKedalamanChange = { kedalaman = it },
                        lokasi = lokasi,
                        onLokasiChange = { lokasi = it },
                        koordinat = koordinat,
                        onKoordinatChange = { koordinat = it },
                        kondisiCuaca = kondisiCuaca,
                        onKondisiCuacaChange = { kondisiCuaca = it },
                        weatherOptions = weatherOptions
                    )
                    1 -> Step2DampakDanKorban(
                        cakupanDampak = cakupanDampak,
                        onCakupanDampakChange = { cakupanDampak = it },
                        korbanMeninggal = korbanMeninggal,
                        onKorbanMeninggalChange = { korbanMeninggal = it },
                        korbanLuka = korbanLuka,
                        onKorbanLukaChange = { korbanLuka = it },
                        kerusakan = kerusakan,
                        onKerusakanChange = { kerusakan = it },
                        penyebab = penyebab,
                        onPenyebabChange = { penyebab = it }
                    )
                    2 -> Step3UpayaDanKondisi(
                        upayaDilakukan = upayaDilakukan,
                        onUpayaDilakukanChange = { upayaDilakukan = it },
                        kondisiMutakhir = kondisiMutakhir,
                        onKondisiMutakhirChange = { kondisiMutakhir = it },
                        sumberInformasi = sumberInformasi,
                        onSumberInformasiChange = { sumberInformasi = it }
                    )
                    3 -> Step4AdministrasiDanKomunikasi(
                        tanggalSurat = tanggalSurat,
                        onTanggalSuratChange = { tanggalSurat = it },
                        tanggalTerima = tanggalTerima,
                        onTanggalTerimaChange = { tanggalTerima = it },
                        operatorName = operatorName,
                        onOperatorNameChange = { operatorName = it },
                        stationId = stationId,
                        onStationIdChange = { stationId = it },
                        logsList = logsList,
                        onAddLogClick = { isAddingLogDialog = true },
                        onDeleteLog = { logsList.remove(it) }
                    )
                }
            }
        }
    }

    // Modal Tambah Log Komunikasi
    if (isAddingLogDialog) {
        DialogTambahLogKomunikasi(
            onDismiss = { isAddingLogDialog = false },
            onAddLog = { log ->
                logsList.add(log)
                isAddingLogDialog = false
            }
        )
    }

    // Success Dialog
    if (saveSuccessDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = {
                saveSuccessDialog = false
                onSavedSuccess(savedId)
                onClose()
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Laporan Harian Tersimpan", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Laporan harian berhasil disimpan ke Room Database lokal Pusdalops dengan ID #$savedId, beserta ${logsList.size} log komunikasi kedaruratan.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        saveSuccessDialog = false
                        onSavedSuccess(savedId)
                        onClose()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                ) {
                    Text("OK", color = Color.White)
                }
            }
        )
    }
}

// ==========================================
// Sub-Komponen Form Setiap Tahapan
// ==========================================

@Composable
private fun Step1DataKejadian(
    tanggalLaporan: String,
    onTanggalLaporanChange: (String) -> Unit,
    jenisBencana: String,
    onJenisBencanaChange: (String) -> Unit,
    commonDisasterTypes: List<String>,
    tanggalKejadian: String,
    onTanggalKejadianChange: (String) -> Unit,
    waktuKejadian: String,
    onWaktuKejadianChange: (String) -> Unit,
    kedalaman: String,
    onKedalamanChange: (String) -> Unit,
    lokasi: String,
    onLokasiChange: (String) -> Unit,
    koordinat: String,
    onKoordinatChange: (String) -> Unit,
    kondisiCuaca: String,
    onKondisiCuacaChange: (String) -> Unit,
    weatherOptions: List<String>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = "Informasi Dasar Bencana", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1976D2))

                OutlinedTextField(
                    value = tanggalLaporan,
                    onValueChange = onTanggalLaporanChange,
                    label = { Text("Tanggal Laporan") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text(text = "Jenis Bencana:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF4B5563))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(commonDisasterTypes) { type ->
                        FilterChip(
                            selected = jenisBencana == type,
                            onClick = { onJenisBencanaChange(type) },
                            label = { Text(type, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFFF9800),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = tanggalKejadian,
                        onValueChange = onTanggalKejadianChange,
                        label = { Text("Tanggal", maxLines = 1) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = waktuKejadian,
                        onValueChange = onWaktuKejadianChange,
                        label = { Text("Waktu", maxLines = 1) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = lokasi,
                    onValueChange = onLokasiChange,
                    label = { Text("Lokasi Kejadian (Kecamatan / Desa)") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFFDC2626)) }
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = koordinat,
                        onValueChange = onKoordinatChange,
                        label = { Text("Koordinat GPS") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = kedalaman,
                        onValueChange = onKedalamanChange,
                        label = { Text("Kedalaman") },
                        modifier = Modifier.weight(0.7f),
                        singleLine = true
                    )
                }

                Text(text = "Kondisi Cuaca Saat Kejadian:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF4B5563))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(weatherOptions) { cuaca ->
                        FilterChip(
                            selected = kondisiCuaca == cuaca,
                            onClick = { onKondisiCuacaChange(cuaca) },
                            label = { Text(cuaca, fontSize = 11.sp) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Step2DampakDanKorban(
    cakupanDampak: String,
    onCakupanDampakChange: (String) -> Unit,
    korbanMeninggal: Int,
    onKorbanMeninggalChange: (Int) -> Unit,
    korbanLuka: Int,
    onKorbanLukaChange: (Int) -> Unit,
    kerusakan: String,
    onKerusakanChange: (String) -> Unit,
    penyebab: String,
    onPenyebabChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "Cakupan Dampak & Korban Jiwa", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))

                OutlinedTextField(
                    value = cakupanDampak,
                    onValueChange = onCakupanDampakChange,
                    label = { Text("Cakupan Wilayah Terdampak & KK") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Korban Meninggal", fontSize = 11.sp, color = Color(0xFF991B1B), fontWeight = FontWeight.SemiBold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { if (korbanMeninggal > 0) onKorbanMeninggalChange(korbanMeninggal - 1) }) {
                                    Text("-", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                }
                                Text("$korbanMeninggal", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                                IconButton(onClick = { onKorbanMeninggalChange(korbanMeninggal + 1) }) {
                                    Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Korban Luka-Luka", fontSize = 11.sp, color = Color(0xFF92400E), fontWeight = FontWeight.SemiBold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { if (korbanLuka > 0) onKorbanLukaChange(korbanLuka - 1) }) {
                                    Text("-", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                }
                                Text("$korbanLuka", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                                IconButton(onClick = { onKorbanLukaChange(korbanLuka + 1) }) {
                                    Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = kerusakan,
                    onValueChange = onKerusakanChange,
                    label = { Text("Kerusakan Fasilitas / Rumah / Infrastruktur") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                OutlinedTextField(
                    value = penyebab,
                    onValueChange = onPenyebabChange,
                    label = { Text("Penyebab Terjadinya Bencana") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        }
    }
}

@Composable
private fun Step3UpayaDanKondisi(
    upayaDilakukan: String,
    onUpayaDilakukanChange: (String) -> Unit,
    kondisiMutakhir: String,
    onKondisiMutakhirChange: (String) -> Unit,
    sumberInformasi: String,
    onSumberInformasiChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "Upaya Penanganan & Kondisi Terkini", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))

                OutlinedTextField(
                    value = upayaDilakukan,
                    onValueChange = onUpayaDilakukanChange,
                    label = { Text("Upaya Penanganan BPBD / TNI / Polri / Warga") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4
                )

                OutlinedTextField(
                    value = kondisiMutakhir,
                    onValueChange = onKondisiMutakhirChange,
                    label = { Text("Kondisi Mutakhir Lapangan") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                OutlinedTextField(
                    value = sumberInformasi,
                    onValueChange = onSumberInformasiChange,
                    label = { Text("Sumber Informasi Awal") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }
    }
}

@Composable
private fun Step4AdministrasiDanKomunikasi(
    tanggalSurat: String,
    onTanggalSuratChange: (String) -> Unit,
    tanggalTerima: String,
    onTanggalTerimaChange: (String) -> Unit,
    operatorName: String,
    onOperatorNameChange: (String) -> Unit,
    stationId: String,
    onStationIdChange: (String) -> Unit,
    logsList: List<LogKomunikasi>,
    onAddLogClick: () -> Unit,
    onDeleteLog: (LogKomunikasi) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = "Administrasi Stasiun & Operator", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = tanggalSurat,
                        onValueChange = onTanggalSuratChange,
                        label = { Text("Tanggal Surat") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = tanggalTerima,
                        onValueChange = onTanggalTerimaChange,
                        label = { Text("Tanggal Terima") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = operatorName,
                    onValueChange = onOperatorNameChange,
                    label = { Text("Nama Operator Pusdalops") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = stationId,
                    onValueChange = onStationIdChange,
                    label = { Text("ID Stasiun / Pos Komando") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }

        // Seksi Log Komunikasi (One-to-Many)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Log Komunikasi Kedaruratan", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                        Text(text = "Relasi One-to-Many (${logsList.size} Riwayat)", fontSize = 11.sp, color = Color(0xFF6B7280))
                    }
                    Button(
                        onClick = onAddLogClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tambah Log", fontSize = 12.sp, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (logsList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF9FAFB), RoundedCornerShape(8.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Belum ada log komunikasi ditambahkan. Tekan tombol Tambah Log.", fontSize = 11.sp, color = Color.Gray)
                    }
                } else {
                    logsList.forEachIndexed { idx, log ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${log.waktuKomunikasi} • ${log.saluran}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = Color(0xFFB45309)
                                    )
                                    IconButton(
                                        onClick = { onDeleteLog(log) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                                    }
                                }
                                Text(text = "Tujuan: ${log.pihakDihubungi}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
                                Text(text = "Subjek: \"${log.pesan}\"", fontSize = 11.sp, color = Color(0xFF334155))
                                Text(text = "Petugas: ${log.operatorNama} (NIP: ${log.operatorNip})", fontSize = 11.sp, color = Color(0xFF047857), fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogTambahLogKomunikasi(
    onDismiss: () -> Unit,
    onAddLog: (LogKomunikasi) -> Unit
) {
    val context = LocalContext.current
    val session = remember { SessionManager(context) }
    var waktu by remember {
        mutableStateOf(SimpleDateFormat("HH:mm 'WIB'", Locale.getDefault()).format(Date()))
    }
    var saluran by remember { mutableStateOf("Radio VHF Ch.1") }
    var pihakDihubungi by remember { mutableStateOf("Kalaksa BPBD Mukomuko") }
    var pesan by remember { mutableStateOf("") }
    var arahanTindakan by remember { mutableStateOf("") }
    var operatorLog by remember { mutableStateOf(session.getNamaPetugas()) }

    val saluranList = listOf("Radio VHF Ch.1", "Radio SSB", "Call Center 117", "WhatsApp Siaga", "Telepon Satelit")

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Tambah Log Komunikasi", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1976D2))
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = waktu,
                    onValueChange = { waktu = it },
                    label = { Text("Waktu Komunikasi") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text(text = "Saluran Komunikasi:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(saluranList) { s ->
                        FilterChip(
                            selected = saluran == s,
                            onClick = { saluran = s },
                            label = { Text(s, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFFF9800),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = pihakDihubungi,
                    onValueChange = { pihakDihubungi = it },
                    label = { Text("Pihak yang Dihubungi / Menerima") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = pesan,
                    onValueChange = { pesan = it },
                    label = { Text("Isi Pesan Koordinasi") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                OutlinedTextField(
                    value = arahanTindakan,
                    onValueChange = { arahanTindakan = it },
                    label = { Text("Arahan / Tindakan Hasil Komunikasi") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                OutlinedTextField(
                    value = operatorLog,
                    onValueChange = { operatorLog = it },
                    label = { Text("Nama Operator") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (pesan.isNotBlank()) {
                        onAddLog(
                            LogKomunikasi(
                                id = 0,
                                laporanId = 0,
                                waktu = waktu,
                                stationIdFrom = saluran,
                                stationIdTo = pihakDihubungi,
                                subjek = pesan,
                                operatorNama = operatorLog,
                                operatorNip = session.getNipPetugas()
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
            ) {
                Text("Simpan Log", color = Color.White)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
