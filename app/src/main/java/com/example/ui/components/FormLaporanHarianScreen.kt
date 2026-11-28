package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AppDatabase
import com.example.data.local.DistribusiLogistikEntity
import com.example.data.local.LaporanHarian
import com.example.data.local.LogKomunikasi
import com.example.data.local.OfficialAsnBpbdPersonnel
import com.example.data.repository.WeatherRepository
import com.example.model.MukomukoDistricts
import com.example.model.WeatherCodeHelper
import com.example.util.PdfGenerator
import com.example.util.SessionManager
import com.example.ui.form.CitizenIncomingReportsModalBottomSheet
import com.example.ui.form.CitizenIncomingReportsProvider
import com.example.ui.form.DariWargaHeaderCard
import com.example.ui.form.IncomingCitizenReportItem
import com.example.ui.theme.OrangeOnPrimary
import com.example.ui.theme.OrangePrimary
import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

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
    val guardContext = LocalContext.current
    val isOfficerSessionActive = com.example.data.local.OfficerAuthManager(guardContext).isOfficerLoggedIn()
    if (!isOfficerSessionActive) {
        InternalModuleLockedScreen(
            moduleTitle = "Form Laporan Harian",
            testTag = "form_laporan_screen_locked",
            onClose = onClose,
            modifier = modifier
        )
        return
    }
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
    var selectedKecamatan by remember { mutableStateOf("Kota Mukomuko") }
    var lokasi by remember { mutableStateOf("Kecamatan Kota Mukomuko, Pesisir Samudra Hindia") }
    var koordinat by remember { mutableStateOf("-2.5830, 101.1240") }
    var cuacaSaatKejadian by remember { mutableStateOf("Data cuaca tidak tersedia") }
    var kondisiCuaca by remember { mutableStateOf("Data cuaca tidak tersedia") }

    // Ambil cuaca terkini otomatis saat Kecamatan dipilih dari modul cuaca 15 Kecamatan
    val weatherRepository = remember { WeatherRepository(database.weatherDao()) }
    LaunchedEffect(selectedKecamatan) {
        val district = MukomukoDistricts.list.firstOrNull {
            it.name.equals(selectedKecamatan.trim(), ignoreCase = true)
        }
        val weatherText = if (district == null) {
            "Data cuaca tidak tersedia"
        } else {
            withContext(Dispatchers.IO) {
                try {
                    val cached = database.weatherDao().getCachedWeather(district.id)
                    if (cached != null) {
                        val tempInt = cached.temperature.roundToInt()
                        val condition = WeatherCodeHelper.getWeatherInfo(cached.weatherCode).conditionText
                        "${tempInt}°C, $condition"
                    } else {
                        val report = weatherRepository.fetchWeatherReport(district)
                        val tempInt = report.current.temperature.roundToInt()
                        val condition = WeatherCodeHelper.getWeatherInfo(report.current.weatherCode).conditionText
                        "${tempInt}°C, $condition"
                    }
                } catch (_: Exception) {
                    "Data cuaca tidak tersedia"
                }
            }
        }
        cuacaSaatKejadian = weatherText
        kondisiCuaca = weatherText
    }

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
    val officerAuthManager = remember { com.example.data.local.OfficerAuthManager(context) }
    val session = remember { SessionManager(context) }
    val initialVerifiedUser = remember { session.authenticateToken() }
    var tanggalSurat by remember { mutableStateOf(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())) }
    var tanggalTerima by remember { mutableStateOf(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())) }
    var operatorName by remember { mutableStateOf(initialVerifiedUser.name) }
    var operatorNip by remember { mutableStateOf(initialVerifiedUser.nip) }
    var stationId by remember { mutableStateOf(initialVerifiedUser.stationId) }

    LaunchedEffect(Unit) {
        session.namaPetugasFlow.collect {
            val verified = session.authenticateToken()
            operatorName = verified.name
            operatorNip = verified.nip
            stationId = verified.stationId
        }
    }

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
                operatorNama = initialVerifiedUser.name,
                operatorNip = initialVerifiedUser.nip
            )
        )
    }

    // Temporary list for Distribusi Logistik / Aset Bantuan (Relasi One-to-Many)
    val logistikList = remember {
        mutableStateListOf<DistribusiLogistikEntity>()
    }

    var isAddingLogDialog by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var saveSuccessDialog by remember { mutableStateOf(false) }
    var savedId by remember { mutableIntStateOf(0) }
    var pendingPdfBytesToSave by remember { mutableStateOf<ByteArray?>(null) }

    // --- State Daftar Laporan Warga ("DARI WARGA") ---
    var incomingCitizenReports by remember { mutableStateOf<List<IncomingCitizenReportItem>>(emptyList()) }
    var isLoadingCitizenReports by remember { mutableStateOf(false) }
    var showDariWargaModalSheet by remember { mutableStateOf(false) }
    var attachedCitizenReport by remember { mutableStateOf<IncomingCitizenReportItem?>(null) }
    val isSandboxModeActive by com.example.training.SandboxTrainingManager.isSandboxEnabled.collectAsState()
    val sandboxReportsSnapshot by com.example.training.SandboxTrainingManager.sandboxReports.collectAsState()
    val unreadCitizenCount = remember(incomingCitizenReports) {
        incomingCitizenReports.count { it.isUnreadOrUnprocessed }
    }

    LaunchedEffect(isSandboxModeActive, sandboxReportsSnapshot) {
        isLoadingCitizenReports = true
        incomingCitizenReports = CitizenIncomingReportsProvider.loadIncomingCitizenReports(
            context = context,
            database = database
        )
        isLoadingCitizenReports = false
    }

    val savePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.data
        val bytes = pendingPdfBytesToSave
        if (result.resultCode == Activity.RESULT_OK && uri != null && bytes != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(bytes)
                    outputStream.flush()
                } ?: throw IllegalStateException("OutputStream null")
                Toast.makeText(context, "Laporan berhasil disimpan", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {
                Toast.makeText(context, "Gagal menyimpan laporan", Toast.LENGTH_SHORT).show()
            }
        } else if (result.resultCode != Activity.RESULT_CANCELED) {
            Toast.makeText(context, "Gagal menyimpan laporan", Toast.LENGTH_SHORT).show()
        }
        pendingPdfBytesToSave = null
    }

    fun buildCurrentLaporan(): LaporanHarian {
        // Ambil namaOperator dan idStasiun langsung dari user yang terverifikasi di token/sesi (bukan dari state input UI)
        val verifiedUser = session.authenticateToken()
        val safeCuaca = cuacaSaatKejadian.ifBlank { "Data cuaca tidak tersedia" }
        return LaporanHarian(
            id = savedId,
            tanggalLaporan = tanggalLaporan,
            jenisBencana = jenisBencana,
            tanggalKejadian = tanggalKejadian,
            waktuKejadian = waktuKejadian,
            kedalaman = kedalaman,
            lokasi = lokasi,
            koordinat = koordinat,
            cakupanDampak = cakupanDampak,
            kondisiCuaca = safeCuaca,
            cuacaSaatKejadian = safeCuaca,
            upayaDilakukan = upayaDilakukan,
            kondisiMutakhir = kondisiMutakhir,
            korbanLuka = korbanLuka,
            korbanMeninggal = korbanMeninggal,
            kerusakan = kerusakan,
            penyebab = penyebab,
            sumberInformasi = sumberInformasi,
            tanggalSurat = tanggalSurat,
            tanggalTerima = tanggalTerima,
            operatorName = verifiedUser.name,
            stationId = verifiedUser.stationId
        ).also {
            it.fotoDokumentasiUri = attachedCitizenReport?.photoUrl.orEmpty()
        }
    }

    fun savePdfToDevice() {
        val currentLaporan = buildCurrentLaporan()
        val currentLogs = logsList.toList()
        val currentLogistik = logistikList.toList()
        val cleanDate = tanggalKejadian
            .replace("/", "-")
            .replace(" ", "_")
            .ifBlank { SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date()) }
        val defaultFileName = "Laporan_BPBD_$cleanDate.pdf"

        scope.launch {
            try {
                val pdfBytes = PdfGenerator.generateLaporanPdfBytes(
                    context = context,
                    laporan = currentLaporan,
                    listLog = currentLogs,
                    listLogistik = currentLogistik
                )
                pendingPdfBytesToSave = pdfBytes
                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_TITLE, defaultFileName)
                }
                savePdfLauncher.launch(intent)
            } catch (_: Exception) {
                Toast.makeText(context, "Gagal menyimpan laporan", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val commonDisasterTypes = listOf(
        "Gempa Bumi Tektonik",
        "Banjir Luapan Sungai",
        "Gelombang Pasang / Rob",
        "Cuaca Ekstrem / Badai",
        "Tanah Longsor",
        "Kebakaran Hutan & Lahan"
    )

    val weatherOptions = listOf("Cerah", "Cerah Berawan", "Berawan", "Hujan Ringan", "Hujan Lebat", "Angin Kencang")
    val adaptiveInsets = rememberAdaptiveSystemBarInsets()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier
            .fillMaxSize()
            .testTag("form_laporan_harian_screen"),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(top = adaptiveInsets.topBarInset),
                title = {
                    Text(
                        text = "Form Laporan Harian",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
            // Tombol Navigasi Bawah dengan Safe Area Adaptif agar tidak tertutup navigation bar / area gestur HP
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = adaptiveInsets.effectiveBottomInset)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStep > 0) {
                        OutlinedButton(
                            onClick = { currentStep -= 1 },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp)
                                .padding(end = 8.dp)
                                .testTag("btnSebelumnya"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Kembali",
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                softWrap = false
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f).padding(end = 8.dp))
                    }

                    Button(
                        onClick = {
                            val isKondusif = jenisBencana.contains("Kondusif", ignoreCase = true) ||
                                jenisBencana.contains("Tidak Ada", ignoreCase = true)
                            val coordValidation = com.example.util.CoordinateValidator.validateKoordinatString(
                                koordinat = koordinat,
                                allowKondusifDash = isKondusif
                            )
                            if (coordValidation.isFailure) {
                                val errMsg = coordValidation.exceptionOrNull()?.message
                                    ?: com.example.util.CoordinateValidator.ERROR_INVALID_RANGE
                                Toast.makeText(context, errMsg, Toast.LENGTH_SHORT).show()
                                currentStep = 0
                                return@Button
                            }

                            if (currentStep < 3) {
                                currentStep += 1
                            } else {
                                // Jika lolos validasi koordinat, simpan ke Room Database (atau ke Sandbox saat Mode Latihan)
                                // BENAR: Ambil namaOperator & idStasiun dari user yang terverifikasi di token sesi (bukan dari UI state)
                                isSaving = true
                                scope.launch(Dispatchers.IO) {
                                    val verifiedUser = session.authenticateToken()
                                    val namaOperator = verifiedUser.name
                                    val idStasiun = verifiedUser.stationId
                                    val safeCuaca = cuacaSaatKejadian.ifBlank { "Data cuaca tidak tersedia" }
                                    val newLaporan = LaporanHarian(
                                        tanggalLaporan = tanggalLaporan,
                                        jenisBencana = jenisBencana,
                                        tanggalKejadian = tanggalKejadian,
                                        waktuKejadian = waktuKejadian,
                                        kedalaman = kedalaman,
                                        lokasi = lokasi,
                                        koordinat = koordinat,
                                        cakupanDampak = cakupanDampak,
                                        kondisiCuaca = safeCuaca,
                                        cuacaSaatKejadian = safeCuaca,
                                        upayaDilakukan = upayaDilakukan,
                                        kondisiMutakhir = kondisiMutakhir,
                                        korbanLuka = korbanLuka,
                                        korbanMeninggal = korbanMeninggal,
                                        kerusakan = kerusakan,
                                        penyebab = penyebab,
                                        sumberInformasi = sumberInformasi,
                                        tanggalSurat = tanggalSurat,
                                        tanggalTerima = tanggalTerima,
                                        operatorName = namaOperator,
                                        stationId = idStasiun
                                    ).also {
                                        it.fotoDokumentasiUri = attachedCitizenReport?.photoUrl.orEmpty()
                                    }
                                    val insertedLaporanId = if (com.example.training.SandboxTrainingManager.isSandboxModeActive()) {
                                        com.example.training.SandboxTrainingManager.recordSandboxDailyReportSaved(newLaporan)
                                    } else {
                                        val id = database.laporanHarianDao().insertLaporan(newLaporan).toInt()
                                        // Insert associated communication logs with verified operator identity
                                        val logEntities = logsList.map {
                                            it.copy(
                                                id = 0,
                                                laporanId = id,
                                                operatorName = namaOperator
                                            )
                                        }
                                        if (logEntities.isNotEmpty()) {
                                            database.laporanHarianDao().insertLogKomunikasiList(logEntities)
                                        }

                                        // Insert associated logistics distribution items
                                        val logistikEntities = logistikList.map {
                                            it.copy(id = 0, laporanId = id)
                                        }
                                        if (logistikEntities.isNotEmpty()) {
                                            database.laporanHarianDao().insertLogistikList(logistikEntities)
                                        }
                                        id
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
                            .heightIn(min = 44.dp)
                            .padding(start = 8.dp)
                            .testTag("btnSelanjutnya"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (currentStep == 3) {
                                if (isSandboxModeActive) "Simpan (Latihan)" else "Simpan Laporan"
                            } else {
                                "Selanjutnya"
                            },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            softWrap = false
                        )
                        Spacer(modifier = Modifier.width(8.dp))
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
                .consumeWindowInsets(innerPadding)
                .background(Color(0xFFF5F5F5))
        ) {
            SandboxTrainingCompactNoticeBanner(
                screenContextLabel = "Form Laporan Harian",
                onInjectDrillClick = {
                    com.example.training.SandboxTrainingManager.injectSimulatedCitizenReport(
                        context = context,
                        triggerRealtimeAlertBanner = false
                    )
                    Toast.makeText(
                        context,
                        "Laporan simulasi baru ditambahkan! Klik tombol 'DARI WARGA' untuk menarik data.",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )

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
                        officerName = "${operatorName.ifBlank { "HITATUN ARAZAK" }} (NIP. ${operatorNip.ifBlank { "197401282006041006" }})",
                        unreadCitizenCount = unreadCitizenCount,
                        attachedCitizenReport = attachedCitizenReport,
                        onClearAttachedCitizenReport = {
                            attachedCitizenReport = null
                            com.example.util.ImageUtils.lastSelectedCitizenReportPhotoUri = null
                        },
                        onOpenDariWargaModal = {
                            showDariWargaModalSheet = true
                            scope.launch {
                                isLoadingCitizenReports = true
                                incomingCitizenReports = CitizenIncomingReportsProvider.loadIncomingCitizenReports(
                                    context = context,
                                    database = database
                                )
                                isLoadingCitizenReports = false
                            }
                        },
                        onTarikLaporanKemarin = {
                            scope.launch(Dispatchers.IO) {
                                val laporanKemarin = if (com.example.training.SandboxTrainingManager.isSandboxModeActive()) {
                                    com.example.training.SandboxTrainingManager.sandboxDailyReports.value.firstOrNull()
                                } else {
                                    database.laporanHarianDao().getLaporanTerakhir()
                                }
                                withContext(Dispatchers.Main) {
                                    if (laporanKemarin != null) {
                                        jenisBencana = laporanKemarin.jenisBencana.ifBlank { jenisBencana }
                                        tanggalKejadian = laporanKemarin.tanggalKejadian.ifBlank { tanggalKejadian }
                                        waktuKejadian = laporanKemarin.waktuKejadian.ifBlank { waktuKejadian }
                                        kedalaman = laporanKemarin.kedalaman.ifBlank { kedalaman }
                                        lokasi = laporanKemarin.lokasiBencana.ifBlank { lokasi }
                                        koordinat = laporanKemarin.koordinat.ifBlank { koordinat }
                                        cakupanDampak = laporanKemarin.cakupanDampak.ifBlank { cakupanDampak }
                                        upayaDilakukan = laporanKemarin.upayaDilakukan.ifBlank { upayaDilakukan }
                                        kondisiMutakhir = laporanKemarin.kondisiMutakhir.ifBlank { kondisiMutakhir }
                                        kerusakan = laporanKemarin.kerusakan.ifBlank { kerusakan }
                                        penyebab = laporanKemarin.penyebab.ifBlank { penyebab }
                                        sumberInformasi = laporanKemarin.sumberInformasi.ifBlank { sumberInformasi }
                                        val matchedDist = MukomukoDistricts.list.firstOrNull {
                                            laporanKemarin.lokasiBencana.contains(it.name, ignoreCase = true)
                                        }
                                        if (matchedDist != null) {
                                            selectedKecamatan = matchedDist.name
                                        }
                                        Toast.makeText(
                                            context,
                                            "Data kejadian sebelumnya berhasil dimuat.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Belum ada data laporan sebelumnya.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        },
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
                        selectedKecamatan = selectedKecamatan,
                        onSelectedKecamatanChange = { kec ->
                            selectedKecamatan = kec
                            val dist = MukomukoDistricts.list.firstOrNull { it.name == kec }
                            if (dist != null) {
                                lokasi = "Kecamatan ${dist.name}, ${dist.landmark}"
                                koordinat = "${dist.latitude}, ${dist.longitude}"
                            }
                        },
                        lokasi = lokasi,
                        onLokasiChange = { lokasi = it },
                        koordinat = koordinat,
                        onKoordinatChange = { koordinat = it },
                        cuacaSaatKejadian = cuacaSaatKejadian,
                        cakupanDampak = cakupanDampak,
                        onCakupanDampakChange = { cakupanDampak = it },
                        upayaDilakukan = upayaDilakukan,
                        onUpayaDilakukanChange = { upayaDilakukan = it },
                        kondisiMutakhir = kondisiMutakhir,
                        onKondisiMutakhirChange = { kondisiMutakhir = it },
                        kerusakan = kerusakan,
                        onKerusakanChange = { kerusakan = it },
                        penyebab = penyebab,
                        onPenyebabChange = { penyebab = it },
                        sumberInformasi = sumberInformasi,
                        onSumberInformasiChange = { sumberInformasi = it }
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
                        onDeleteLog = { logsList.remove(it) },
                        logistikList = logistikList,
                        onAddLogistik = { logistikList.add(it) },
                        onDeleteLogistik = { logistikList.remove(it) },
                        onCetakPdfClick = {
                            val currentLaporan = buildCurrentLaporan()
                            PdfGenerator.cetakLaporanKePdf(
                                context = context,
                                laporan = currentLaporan,
                                listLog = logsList.toList(),
                                listLogistik = logistikList.toList()
                            )
                        },
                        onSimpanPdfClick = {
                            savePdfToDevice()
                        }
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

    // Modal Bottom Sheet Daftar Laporan Warga Masuk ("DARI WARGA")
    if (showDariWargaModalSheet) {
        CitizenIncomingReportsModalBottomSheet(
            reports = incomingCitizenReports,
            isLoading = isLoadingCitizenReports,
            onDismiss = { showDariWargaModalSheet = false },
            onRefresh = {
                scope.launch {
                    isLoadingCitizenReports = true
                    incomingCitizenReports = CitizenIncomingReportsProvider.loadIncomingCitizenReports(
                        context = context,
                        database = database
                    )
                    isLoadingCitizenReports = false
                }
            },
            onUseReportData = { selectedItem: IncomingCitizenReportItem ->
                showDariWargaModalSheet = false
                attachedCitizenReport = selectedItem.copy(
                    isUnreadOrUnprocessed = false,
                    status = "Sudah Dipakai"
                )
                com.example.util.ImageUtils.lastSelectedCitizenReportPhotoUri = selectedItem.photoUrl

                // Autofill Kolom-kolom di "Informasi Dasar Kejadian" & Dampak
                jenisBencana = selectedItem.disasterType
                if (selectedItem.dateText.isNotBlank()) {
                    tanggalKejadian = selectedItem.dateText
                }
                if (selectedItem.timeText.isNotBlank()) {
                    waktuKejadian = selectedItem.timeText
                }
                val matchedKecamatan = MukomukoDistricts.list.firstOrNull {
                    it.name.equals(selectedItem.district.trim(), ignoreCase = true) ||
                        selectedItem.locationDetail.contains(it.name, ignoreCase = true)
                }?.name
                if (!matchedKecamatan.isNullOrBlank()) {
                    selectedKecamatan = matchedKecamatan
                }
                lokasi = selectedItem.fullFormattedLocation()
                if (selectedItem.coordinates.isNotBlank()) {
                    koordinat = selectedItem.coordinates
                }
                cakupanDampak = com.example.util.CakupanDampakFormatter.formatStructured(
                    lokasiKejadian = selectedItem.fullFormattedLocation(),
                    deskripsiKejadian = selectedItem.description
                )
                kondisiMutakhir = selectedItem.description
                sumberInformasi = "Laporan Warga (${selectedItem.reporterName} - ${selectedItem.reporterPhone})"
                korbanMeninggal = selectedItem.casualtiesDead
                korbanLuka = selectedItem.casualtiesInjured

                scope.launch {
                    CitizenIncomingReportsProvider.markReportAsUsedByOfficer(
                        context = context,
                        database = database,
                        report = selectedItem
                    )
                    incomingCitizenReports = CitizenIncomingReportsProvider.loadIncomingCitizenReports(
                        context = context,
                        database = database
                    )
                }

                Toast.makeText(
                    context,
                    "Data laporan warga (${selectedItem.disasterType} - ${selectedItem.reporterName}) berhasil diisi ke formulir!",
                    Toast.LENGTH_SHORT
                ).show()
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
                        text = "Laporan harian berhasil disimpan ke Room Database lokal Pusdalops dengan ID #$savedId, beserta ${logsList.size} log komunikasi dan ${logistikList.size} item distribusi logistik.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                    )
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = {
                            val currentLaporan = buildCurrentLaporan()
                            PdfGenerator.cetakLaporanKePdf(
                                context = context,
                                laporan = currentLaporan,
                                listLog = logsList.toList(),
                                listLogistik = logistikList.toList()
                            )
                        }
                    ) {
                        Text("Cetak PDF", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = { savePdfToDevice() }
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Simpan PDF", fontSize = 12.sp)
                    }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Step1DataKejadian(
    officerName: String,
    unreadCitizenCount: Int,
    attachedCitizenReport: IncomingCitizenReportItem?,
    onClearAttachedCitizenReport: () -> Unit,
    onOpenDariWargaModal: () -> Unit,
    onTarikLaporanKemarin: () -> Unit,
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
    selectedKecamatan: String,
    onSelectedKecamatanChange: (String) -> Unit,
    lokasi: String,
    onLokasiChange: (String) -> Unit,
    koordinat: String,
    onKoordinatChange: (String) -> Unit,
    cuacaSaatKejadian: String,
    cakupanDampak: String,
    onCakupanDampakChange: (String) -> Unit,
    upayaDilakukan: String,
    onUpayaDilakukanChange: (String) -> Unit,
    kondisiMutakhir: String,
    onKondisiMutakhirChange: (String) -> Unit,
    kerusakan: String,
    onKerusakanChange: (String) -> Unit,
    penyebab: String,
    onPenyebabChange: (String) -> Unit,
    sumberInformasi: String,
    onSumberInformasiChange: (String) -> Unit
) {
    val keyboardActions = rememberFormKeyboardActions()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // KARTU 1: INFO PETUGAS & TOMBOL "DARI WARGA" DENGAN BADGE NOTIFIKASI MERAH
        DariWargaHeaderCard(
            officerName = officerName,
            unreadCitizenCount = unreadCitizenCount,
            attachedCitizenReport = attachedCitizenReport,
            onOpenDariWargaSheet = onOpenDariWargaModal,
            onUpdateKejadianKemarin = onTarikLaporanKemarin,
            onClearAttachedCitizenReport = onClearAttachedCitizenReport
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "Informasi Dasar Kejadian", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1976D2))

                OutlinedTextField(
                    value = tanggalLaporan,
                    onValueChange = onTanggalLaporanChange,
                    label = { Text("Tanggal Laporan") },
                    keyboardOptions = formNextKeyboardOptions(),
                    keyboardActions = keyboardActions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .formScrollPadding(),
                    singleLine = true
                )

                Text(text = "Jenis Bencana:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF4B5563))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    commonDisasterTypes.forEach { type ->
                        FilterChip(
                            selected = jenisBencana == type,
                            onClick = { onJenisBencanaChange(type) },
                            label = {
                                Text(
                                    text = type,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    softWrap = false
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFFF9800),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CompactOutlinedTextField(
                        value = tanggalKejadian,
                        onValueChange = onTanggalKejadianChange,
                        label = "Tanggal",
                        modifier = Modifier.weight(1f)
                    )
                    CompactOutlinedTextField(
                        value = waktuKejadian,
                        onValueChange = onWaktuKejadianChange,
                        label = "Waktu",
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(text = "Pilih Kecamatan (15 Kecamatan Mukomuko):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF4B5563))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    MukomukoDistricts.list.forEach { dist ->
                        FilterChip(
                            selected = selectedKecamatan == dist.name,
                            onClick = { onSelectedKecamatanChange(dist.name) },
                            label = {
                                Text(
                                    text = dist.name,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    softWrap = false
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF0284C7),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = lokasi,
                    onValueChange = onLokasiChange,
                    label = { Text("Detail Lokasi Kejadian (Kecamatan / Desa)") },
                    keyboardOptions = formNextKeyboardOptions(),
                    keyboardActions = keyboardActions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .formScrollPadding(),
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFFDC2626)) }
                )

                val isKondusif = jenisBencana.contains("Kondusif", ignoreCase = true) ||
                    jenisBencana.contains("Tidak Ada", ignoreCase = true)
                val koordinatError = remember(koordinat, isKondusif) {
                    com.example.util.CoordinateValidator.validateKoordinatString(
                        koordinat = koordinat,
                        allowKondusifDash = isKondusif
                    ).exceptionOrNull()?.message
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = koordinat,
                        onValueChange = onKoordinatChange,
                        isError = koordinatError != null,
                        supportingText = if (koordinatError != null) {
                            { Text(text = koordinatError, fontSize = 10.sp) }
                        } else null,
                        label = { Text("Koordinat GPS", maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false) },
                        keyboardOptions = formNextKeyboardOptions(),
                        keyboardActions = keyboardActions,
                        modifier = Modifier
                            .weight(1f)
                            .formScrollPadding()
                            .testTag("input_koordinat_gps"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = kedalaman,
                        onValueChange = onKedalamanChange,
                        label = { Text("Kedalaman", maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false) },
                        keyboardOptions = formNextKeyboardOptions(),
                        keyboardActions = keyboardActions,
                        modifier = Modifier
                            .weight(0.7f)
                            .formScrollPadding(),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = cuacaSaatKejadian.ifBlank { "Data cuaca tidak tersedia" },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Kondisi Cuaca Saat Kejadian (Read-Only Otomatis)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF0F9FF),
                        unfocusedContainerColor = Color(0xFFF0F9FF),
                        focusedBorderColor = Color(0xFF0284C7),
                        unfocusedBorderColor = Color(0xFF7DD3FC)
                    )
                )

                OutlinedTextField(
                    value = cakupanDampak,
                    onValueChange = onCakupanDampakChange,
                    label = { Text("Cakupan Dampak (1. Lokasi Kejadian & 2. Deskripsi Bencana)") },
                    keyboardOptions = formNextKeyboardOptions(),
                    keyboardActions = keyboardActions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .formScrollPadding(),
                    minLines = 3
                )

                OutlinedTextField(
                    value = upayaDilakukan,
                    onValueChange = onUpayaDilakukanChange,
                    label = { Text("Upaya Dilakukan (Penanganan BPBD / TRC)") },
                    keyboardOptions = formNextKeyboardOptions(),
                    keyboardActions = keyboardActions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .formScrollPadding(),
                    minLines = 2
                )

                OutlinedTextField(
                    value = kondisiMutakhir,
                    onValueChange = onKondisiMutakhirChange,
                    label = { Text("Kondisi Mutakhir Lapangan") },
                    keyboardOptions = formNextKeyboardOptions(),
                    keyboardActions = keyboardActions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .formScrollPadding(),
                    minLines = 2
                )

                OutlinedTextField(
                    value = kerusakan,
                    onValueChange = onKerusakanChange,
                    label = { Text("Kerusakan (Rumah / Fasilitas / Infrastruktur)") },
                    keyboardOptions = formNextKeyboardOptions(),
                    keyboardActions = keyboardActions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .formScrollPadding(),
                    minLines = 2
                )

                OutlinedTextField(
                    value = penyebab,
                    onValueChange = onPenyebabChange,
                    label = { Text("Penyebab Terjadinya Bencana") },
                    keyboardOptions = formNextKeyboardOptions(),
                    keyboardActions = keyboardActions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .formScrollPadding(),
                    minLines = 2
                )

                OutlinedTextField(
                    value = sumberInformasi,
                    onValueChange = onSumberInformasiChange,
                    label = { Text("Sumber Informasi") },
                    keyboardOptions = formDoneKeyboardOptions(),
                    keyboardActions = keyboardActions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .formScrollPadding(),
                    singleLine = true
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val colors = OutlinedTextFieldDefaults.colors()
    val keyboardActions = rememberFormKeyboardActions()
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .formScrollPadding(),
        singleLine = true,
        keyboardOptions = formNextKeyboardOptions(),
        keyboardActions = keyboardActions,
        textStyle = TextStyle(
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF1E293B)
        ),
        interactionSource = interactionSource,
        decorationBox = { innerTextField ->
            OutlinedTextFieldDefaults.DecorationBox(
                value = value,
                innerTextField = innerTextField,
                enabled = true,
                singleLine = true,
                visualTransformation = VisualTransformation.None,
                interactionSource = interactionSource,
                label = {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false
                    )
                },
                colors = colors,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
                container = {
                    OutlinedTextFieldDefaults.Container(
                        enabled = true,
                        isError = false,
                        interactionSource = interactionSource,
                        colors = colors
                    )
                }
            )
        }
    )
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
    val keyboardActions = rememberFormKeyboardActions()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "Cakupan Dampak & Korban Jiwa", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))

                val parsedImpact = remember(cakupanDampak) {
                    com.example.util.CakupanDampakFormatter.parse(cakupanDampak)
                }

                Text(
                    text = "Dipisahkan ke 2 baris terstruktur pada formulir & hasil cetak PDF:",
                    fontSize = 11.sp,
                    color = Color(0xFF475569)
                )

                OutlinedTextField(
                    value = parsedImpact.lokasiKejadian,
                    onValueChange = { newLokasi ->
                        onCakupanDampakChange(
                            com.example.util.CakupanDampakFormatter.formatStructured(
                                lokasiKejadian = newLokasi,
                                deskripsiKejadian = parsedImpact.deskripsiKejadian
                            )
                        )
                    },
                    label = { Text("1. Lokasi Kejadian (Wilayah / Desa Terdampak)") },
                    keyboardOptions = formNextKeyboardOptions(),
                    keyboardActions = keyboardActions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .formScrollPadding()
                        .testTag("input_step2_cakupan_lokasi"),
                    minLines = 2
                )

                OutlinedTextField(
                    value = parsedImpact.deskripsiKejadian,
                    onValueChange = { newDeskripsi ->
                        onCakupanDampakChange(
                            com.example.util.CakupanDampakFormatter.formatStructured(
                                lokasiKejadian = parsedImpact.lokasiKejadian,
                                deskripsiKejadian = newDeskripsi
                            )
                        )
                    },
                    label = { Text("2. Deskripsi Kejadian / Bencana Lainnya") },
                    keyboardOptions = formNextKeyboardOptions(),
                    keyboardActions = keyboardActions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .formScrollPadding()
                        .testTag("input_step2_cakupan_deskripsi"),
                    minLines = 3
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Korban Meninggal",
                                fontSize = 11.sp,
                                color = Color(0xFF991B1B),
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                softWrap = false
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { if (korbanMeninggal > 0) onKorbanMeninggalChange(korbanMeninggal - 1) }) {
                                    Text("-", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                }
                                Text("$korbanMeninggal", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626), maxLines = 1, softWrap = false)
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
                        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Korban Luka-Luka",
                                fontSize = 11.sp,
                                color = Color(0xFF92400E),
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                softWrap = false
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { if (korbanLuka > 0) onKorbanLukaChange(korbanLuka - 1) }) {
                                    Text("-", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                }
                                Text("$korbanLuka", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706), maxLines = 1, softWrap = false)
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
                    keyboardOptions = formNextKeyboardOptions(),
                    keyboardActions = keyboardActions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .formScrollPadding(),
                    minLines = 3
                )

                OutlinedTextField(
                    value = penyebab,
                    onValueChange = onPenyebabChange,
                    label = { Text("Penyebab Terjadinya Bencana") },
                    keyboardOptions = formDoneKeyboardOptions(),
                    keyboardActions = keyboardActions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .formScrollPadding(),
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
    val keyboardActions = rememberFormKeyboardActions()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 24.dp),
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
                    keyboardOptions = formNextKeyboardOptions(),
                    keyboardActions = keyboardActions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .formScrollPadding(),
                    minLines = 4
                )

                OutlinedTextField(
                    value = kondisiMutakhir,
                    onValueChange = onKondisiMutakhirChange,
                    label = { Text("Kondisi Mutakhir Lapangan") },
                    keyboardOptions = formNextKeyboardOptions(),
                    keyboardActions = keyboardActions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .formScrollPadding(),
                    minLines = 3
                )

                OutlinedTextField(
                    value = sumberInformasi,
                    onValueChange = onSumberInformasiChange,
                    label = { Text("Sumber Informasi Awal") },
                    keyboardOptions = formDoneKeyboardOptions(),
                    keyboardActions = keyboardActions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .formScrollPadding(),
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
    onDeleteLog: (LogKomunikasi) -> Unit,
    logistikList: List<DistribusiLogistikEntity> = emptyList(),
    onAddLogistik: (DistribusiLogistikEntity) -> Unit = {},
    onDeleteLogistik: (DistribusiLogistikEntity) -> Unit = {},
    onCetakPdfClick: () -> Unit = {},
    onSimpanPdfClick: () -> Unit = {}
) {
    val keyboardActions = rememberFormKeyboardActions()
    var namaBarangInput by remember { mutableStateOf("") }
    var jumlahInput by remember { mutableStateOf("") }
    var satuanInput by remember { mutableStateOf("") }
    var keteranganInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "Administrasi Stasiun & Operator", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = tanggalSurat,
                        onValueChange = onTanggalSuratChange,
                        label = { Text("Tanggal Surat", maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false) },
                        keyboardOptions = formNextKeyboardOptions(),
                        keyboardActions = keyboardActions,
                        modifier = Modifier
                            .weight(1f)
                            .formScrollPadding(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = tanggalTerima,
                        onValueChange = onTanggalTerimaChange,
                        label = { Text("Tanggal Terima", maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false) },
                        keyboardOptions = formNextKeyboardOptions(),
                        keyboardActions = keyboardActions,
                        modifier = Modifier
                            .weight(1f)
                            .formScrollPadding(),
                        singleLine = true
                    )
                }

                val resolvedOperatorProfile = remember(operatorName) {
                    com.example.data.local.OfficialAsnBpbdPersonnel.resolveCanonicalPersonnel(operatorName)
                }
                val displayOperatorWithNip = if (resolvedOperatorProfile.nip.isNotBlank() && resolvedOperatorProfile.nip != "-") {
                    "${resolvedOperatorProfile.name} (NIP. ${resolvedOperatorProfile.nip})"
                } else {
                    operatorName
                }
                OutlinedTextField(
                    value = displayOperatorWithNip,
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    label = { Text("Nama & NIP Operator Pusdalops") },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Terkunci otomatis dari sesi login",
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF3F4F6),
                        unfocusedContainerColor = Color(0xFFF3F4F6),
                        disabledContainerColor = Color(0xFFF3F4F6),
                        disabledTextColor = Color(0xFF374151),
                        disabledLabelColor = Color(0xFF6B7280),
                        disabledBorderColor = Color(0xFFD1D5DB),
                        disabledTrailingIconColor = Color(0xFF9CA3AF)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_operator_pusdalops"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = stationId,
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    label = { Text("ID Stasiun / Pos Komando") },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Terkunci otomatis dari sesi stasiun",
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF3F4F6),
                        unfocusedContainerColor = Color(0xFFF3F4F6),
                        disabledContainerColor = Color(0xFFF3F4F6),
                        disabledTextColor = Color(0xFF374151),
                        disabledLabelColor = Color(0xFF6B7280),
                        disabledBorderColor = Color(0xFFD1D5DB),
                        disabledTrailingIconColor = Color(0xFF9CA3AF)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_station_id"),
                    singleLine = true
                )
            }
        }

        // Seksi Distribusi Logistik / Aset Bantuan (Form Dinamis)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Distribusi Logistik / Aset Bantuan",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF047857)
                )
                Text(
                    text = "Daftar bantuan darurat yang terikat dengan ID Laporan (${logistikList.size} item)",
                    fontSize = 11.sp,
                    color = Color(0xFF6B7280)
                )

                OutlinedTextField(
                    value = namaBarangInput,
                    onValueChange = { namaBarangInput = it },
                    label = { Text("Nama Barang (cth: Tenda Gulung, Beras, Air Mineral)") },
                    keyboardOptions = formNextKeyboardOptions(),
                    keyboardActions = keyboardActions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .formScrollPadding(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = jumlahInput,
                        onValueChange = { jumlahInput = it },
                        label = { Text("Jumlah", maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false) },
                        keyboardOptions = formNextKeyboardOptions(),
                        keyboardActions = keyboardActions,
                        modifier = Modifier
                            .weight(1f)
                            .formScrollPadding(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = satuanInput,
                        onValueChange = { satuanInput = it },
                        label = { Text("Satuan (Unit/Karung/Dus)", maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false) },
                        keyboardOptions = formNextKeyboardOptions(),
                        keyboardActions = keyboardActions,
                        modifier = Modifier
                            .weight(1f)
                            .formScrollPadding(),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = keteranganInput,
                    onValueChange = { keteranganInput = it },
                    label = { Text("Keterangan (cth: Diserahkan ke Kecamatan X)") },
                    keyboardOptions = formDoneKeyboardOptions(),
                    keyboardActions = keyboardActions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .formScrollPadding(),
                    singleLine = true
                )

                Button(
                    onClick = {
                        if (namaBarangInput.isNotBlank()) {
                            onAddLogistik(
                                DistribusiLogistikEntity(
                                    namaBarang = namaBarangInput.trim(),
                                    jumlah = jumlahInput.trim().ifBlank { "1" },
                                    satuan = satuanInput.trim().ifBlank { "Unit" },
                                    keterangan = keteranganInput.trim().ifBlank { "-" }
                                )
                            )
                            namaBarangInput = ""
                            jumlahInput = ""
                            satuanInput = ""
                            keteranganInput = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tambah Barang Logistik", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                if (logistikList.isNotEmpty()) {
                    logistikList.forEachIndexed { idx, item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA7F3D0))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${idx + 1}. ${item.namaBarang} (${item.jumlah} ${item.satuan})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color(0xFF065F46)
                                    )
                                    Text(
                                        text = "Keterangan: ${item.keterangan}",
                                        fontSize = 11.sp,
                                        color = Color(0xFF334155)
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteLogistik(item) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Hapus Barang",
                                        tint = Color(0xFFDC2626),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Log Komunikasi Kedaruratan",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD97706),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Relasi One-to-Many (${logsList.size} Riwayat)",
                            fontSize = 11.sp,
                            color = Color(0xFF6B7280),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            softWrap = false
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onAddLogClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.heightIn(min = 36.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Tambah Log",
                            fontSize = 12.sp,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            softWrap = false
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

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
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${log.waktuKomunikasi} • ${log.saluran}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = Color(0xFFB45309),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { onDeleteLog(log) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                                    }
                                }
                                Text(
                                    text = "Tujuan: ${log.pihakDihubungi}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1E293B),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Subjek: \"${log.pesan}\"",
                                    fontSize = 11.sp,
                                    color = Color(0xFF334155),
                                    lineHeight = 16.sp,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    text = "Petugas: ${log.operatorNama} (NIP: ${log.operatorNip})",
                                    fontSize = 11.sp,
                                    color = Color(0xFF047857),
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onCetakPdfClick,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_cetak_pdf"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Cetak PDF",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Button(
                        onClick = onSimpanPdfClick,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_simpan_pdf"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF047857)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Simpan PDF",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Simpan PDF",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
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
    val keyboardActions = rememberFormKeyboardActions()
    val officerAuthManager = remember { com.example.data.local.OfficerAuthManager(context) }
    val session = remember { SessionManager(context) }
    var waktu by remember {
        mutableStateOf(SimpleDateFormat("HH:mm 'WIB'", Locale.getDefault()).format(Date()))
    }
    var saluran by remember { mutableStateOf("Radio VHF Ch.1") }
    var pihakDihubungi by remember { mutableStateOf("Kalaksa BPBD Mukomuko") }
    var pesan by remember { mutableStateOf("") }
    var arahanTindakan by remember { mutableStateOf("") }
    var operatorLog by remember {
        mutableStateOf(officerAuthManager.getOfficerName().ifBlank { session.getNamaPetugas() })
    }

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
                    keyboardOptions = formNextKeyboardOptions(),
                    keyboardActions = keyboardActions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .formScrollPadding(),
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
                    keyboardOptions = formNextKeyboardOptions(),
                    keyboardActions = keyboardActions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .formScrollPadding(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = pesan,
                    onValueChange = { pesan = it },
                    label = { Text("Isi Pesan Koordinasi") },
                    keyboardOptions = formNextKeyboardOptions(),
                    keyboardActions = keyboardActions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .formScrollPadding(),
                    minLines = 2
                )

                OutlinedTextField(
                    value = arahanTindakan,
                    onValueChange = { arahanTindakan = it },
                    label = { Text("Arahan / Tindakan Hasil Komunikasi") },
                    keyboardOptions = formDoneKeyboardOptions(),
                    keyboardActions = keyboardActions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .formScrollPadding(),
                    minLines = 2
                )

                OutlinedTextField(
                    value = operatorLog,
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    label = { Text("Nama Operator") },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Terkunci otomatis dari sesi login",
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF3F4F6),
                        unfocusedContainerColor = Color(0xFFF3F4F6),
                        disabledContainerColor = Color(0xFFF3F4F6),
                        disabledTextColor = Color(0xFF374151),
                        disabledLabelColor = Color(0xFF6B7280),
                        disabledBorderColor = Color(0xFFD1D5DB),
                        disabledTrailingIconColor = Color(0xFF9CA3AF)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (pesan.isNotBlank()) {
                        val verifiedUser = session.authenticateToken()
                        onAddLog(
                            LogKomunikasi(
                                id = 0,
                                laporanId = 0,
                                waktu = waktu,
                                stationIdFrom = saluran,
                                stationIdTo = pihakDihubungi,
                                subjek = pesan,
                                operatorNama = verifiedUser.name,
                                operatorNip = verifiedUser.nip
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
