package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.launch
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.local.AppDatabase
import com.example.data.local.OfficialAsnBpbdPersonnel
import com.example.data.local.OfficerAuthManager
import com.example.data.local.OfficerProfile
import com.example.data.local.OfficerRoles
import com.example.data.repository.PersonnelRepository
import com.example.model.CitizenDisasterReport
import com.example.model.District
import com.example.model.MukomukoDistricts
import com.example.ui.theme.OrangePrimary
import com.example.util.GoogleMapsHelper
import com.example.util.LocationHelper
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private val DISASTER_TYPES = listOf(
    "Banjir / Genangan Luapan Air",
    "Gempa Bumi",
    "Gelombang Pasang & Abrasi Pantai",
    "Angin Puting Beliung & Badai",
    "Tanah Longsor",
    "Pohon Tumbang",
    "Kebakaran Hutan & Lahan (Karhutla)",
    "Kekeringan / Krisis Air Bersih",
    "Lainnya"
)

private val DISTRICT_NAMES = listOf(
    "Kota Mukomuko",
    "Ipuh",
    "Teramang Jaya",
    "Penarik",
    "Lubuk Pinang",
    "Pondok Suguh",
    "Air Dikit",
    "Selagan Raya",
    "XIV Koto",
    "Malin Deman",
    "V Koto",
    "Teras Terunjam",
    "Sungai Rumbai",
    "Air Rami",
    "Air Manjunto"
)

private val VILLAGES_MAP = mapOf(
    "Kota Mukomuko" to listOf("Kelurahan Bandar Ratu", "Kelurahan Koto Jaya", "Kelurahan Pasar Mukomuko", "Kelurahan Ujung Padang", "Desa Pondok Batu", "Desa Selagan Jaya", "Desa Tanah Rekah", "Desa Pasar Sebelah"),
    "Ipuh" to listOf("Desa Medan Jaya", "Desa Pasar Ipuh", "Desa Tanjung Harapan", "Desa Pulai Payung", "Desa Pulau Baru", "Desa Sibak", "Desa Tanjung Medan"),
    "Teramang Jaya" to listOf("Desa Pasar Teramang", "Desa Teramang", "Desa Bunga Tanjung", "Desa Nelan Indah", "Desa Batu Ejung", "Desa Mandi Angin"),
    "Penarik" to listOf("Desa Penarik", "Desa Lubuk Mukti", "Desa Maju Makmur", "Desa Mekar Mulya", "Desa Marga Mukti", "Desa Sidodadi"),
    "Lubuk Pinang" to listOf("Desa Lubuk Pinang", "Desa Lubuk Gedang", "Desa Aie Manjunto", "Desa Ranah Karya", "Desa Suka Pindah"),
    "Pondok Suguh" to listOf("Desa Pondok Suguh", "Desa Tunggang", "Desa Teluk Bakung", "Desa Air Bikuk", "Desa Karya Mulya"),
    "Air Dikit" to listOf("Desa Sari Bulan", "Desa Dusun Baru V Koto", "Desa Air Dikit", "Desa Air Kasai", "Desa Pondok Lunang"),
    "Selagan Raya" to listOf("Desa Sungai Jerinjing", "Desa Surian Bungkal", "Desa Lubuk Sahung", "Desa Pondok Baru", "Desa Sungai Ipuh"),
    "XIV Koto" to listOf("Desa Lubuk Sanai", "Desa Rawa Bangun", "Desa Rawa Mulya", "Desa Pauh Terenja", "Desa Tanjung Mulya"),
    "Malin Deman" to listOf("Desa Talang Baru", "Desa Serami Baru", "Desa Talang Arah", "Desa Air Merah"),
    "V Koto" to listOf("Desa Lalang Luas", "Desa Pondok Tengah", "Desa Lubuk Cabau", "Desa Sungai Rengas"),
    "Teras Terunjam" to listOf("Desa Teras Terunjam", "Desa Karang Jaya", "Desa Karang Anyar", "Desa Mekar Jaya"),
    "Sungai Rumbai" to listOf("Desa Retak Mudik", "Desa Gajah Mati", "Desa Retak Hilir", "Desa Padang Gading"),
    "Air Rami" to listOf("Desa Air Rami", "Desa Bukit Harapan", "Desa Rami Mulya", "Desa Talang Rio"),
    "Air Manjunto" to listOf("Desa Manjunto Jaya", "Desa Tirta Makmur", "Desa Agung Jaya", "Desa Sinar Jaya")
)

/**
 * Tab 9: Layanan Terpadu Laporan Warga Bencana Kabupaten Mukomuko
 * Reproduces the official incident reporting form + GIS Spatial Map + Status Tracking + Emergency Contacts
 */
@Composable
fun CitizenReportScreen(
    reports: List<CitizenDisasterReport>,
    currentDistrict: District,
    onSubmitReport: (CitizenDisasterReport) -> Unit,
    onUpdateReportStatus: (String, String) -> Unit,
    onBackClick: () -> Unit,
    onSyncReports: (((Int) -> Unit) -> Unit)? = null,
    onUpdateReportStatusAndNotes: ((String, String, String) -> Unit)? = null,
    initialShowOfficerLogin: Boolean = false,
    onOfficerLoginDismissed: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Formulir, 1 = Peta Sebaran, 2 = Tabel Riwayat, 3 = Kontak Penting, 4 = Konsol Petugas

    // Officer Authentication State (BPBD Mukomuko Petugas Jaga)
    val officerAuthManager = remember { OfficerAuthManager(context) }
    var isOfficerLoggedIn by remember { mutableStateOf(officerAuthManager.isOfficerLoggedIn()) }
    var officerName by remember { mutableStateOf(officerAuthManager.getOfficerName()) }
    var officerBadgeId by remember { mutableStateOf(officerAuthManager.getOfficerBadgeId()) }
    var officerShift by remember { mutableStateOf(officerAuthManager.getOfficerShift()) }
    var officerRole by remember { mutableStateOf(officerAuthManager.getOfficerRole()) }

    val coroutineScope = rememberCoroutineScope()
    val database = remember { AppDatabase.getInstance(context) }
    val personnelRepository = remember { PersonnelRepository(database.personnelDao()) }
    LaunchedEffect(Unit) {
        personnelRepository.seedDefaultAsnPersonnelIfNeeded()
    }

    // Officer Login & Status Gating Modal State
    var showOfficerLoginDialog by remember { mutableStateOf(initialShowOfficerLogin) }
    LaunchedEffect(initialShowOfficerLogin) {
        if (initialShowOfficerLogin) {
            showOfficerLoginDialog = true
        }
    }
    var pendingStatusReportId by remember { mutableStateOf<String?>(null) }
    var pendingStatusTarget by remember { mutableStateOf<String?>(null) }

    // Photo Fullscreen Zoom Dialog State
    var previewPhotoUri by remember { mutableStateOf<String?>(null) }
    var previewPhotoReport by remember { mutableStateOf<CitizenDisasterReport?>(null) }

    // Cloud Online Sync State
    var isSyncingOnline by remember { mutableStateOf(false) }
    var lastSyncStatusMessage by remember { mutableStateOf<String?>(null) }

    // Trigger auto-sync once on screen open
    LaunchedEffect(Unit) {
        onSyncReports?.invoke { count ->
            if (count > 0) {
                lastSyncStatusMessage = "$count laporan baru disinkronkan"
            }
        }
    }

    // Form inputs state
    var selectedDisasterType by remember { mutableStateOf(DISASTER_TYPES[0]) }
    var isDisasterDropdownExpanded by remember { mutableStateOf(false) }

    val initialTime = remember {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm 'WIB'", Locale("id", "ID"))
        sdf.format(Date())
    }
    var incidentTime by remember { mutableStateOf(initialTime) }
    var incidentLocation by remember { mutableStateOf("") }

    var selectedDistrict by remember { mutableStateOf(currentDistrict.name) }
    var isDistrictDropdownExpanded by remember { mutableStateOf(false) }

    var selectedVillage by remember {
        mutableStateOf(VILLAGES_MAP[currentDistrict.name]?.firstOrNull() ?: "Desa Pasar")
    }
    var isVillageDropdownExpanded by remember { mutableStateOf(false) }

    // Coordinates & GPS Verification
    var latitude by remember { mutableDoubleStateOf(currentDistrict.latitude) }
    var longitude by remember { mutableDoubleStateOf(currentDistrict.longitude) }
    var gpsAccuracy by remember { mutableStateOf<Float?>(null) }
    var isGpsLocked by remember { mutableStateOf(false) }
    var isFetchingGps by remember { mutableStateOf(false) }
    var gpsStatusMessage by remember { mutableStateOf("Sentuh pin GPS untuk mengunci koordinat ponsel") }

    // Casualties data
    var deadCount by remember { mutableIntStateOf(0) }
    var injuredCount by remember { mutableIntStateOf(0) }
    var missingCount by remember { mutableIntStateOf(0) }

    // Photo & Geotag Verification (Anti-Hoax: Live camera only, gallery upload disabled)
    var photoUriString by remember { mutableStateOf<String?>(null) }
    var cameraCapturedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var reporterName by remember { mutableStateOf("") }
    var reporterPhone by remember { mutableStateOf("") }
    var additionalNotes by remember { mutableStateOf("") }

    // Dialog state
    var showSuccessDialog by remember { mutableStateOf(false) }
    var submittedReportId by remember { mutableStateOf("") }

    // Table Filter by Status: "Semua", "Diproses", "Divalidasi", "Selesai"
    var tableStatusFilter by remember { mutableStateOf("Semua") }

    // Permission Rationale / Guidance Dialog state
    var permissionDeniedDialogMessage by remember { mutableStateOf<String?>(null) }
    var permissionDeniedDialogTitle by remember { mutableStateOf<String?>(null) }

    // Camera launcher (TakePicturePreview returns Bitmap thumbnail)
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            cameraCapturedBitmap = bitmap
            photoUriString = null
            // Save bitmap to cache directory for persistent URI
            try {
                val file = File(context.cacheDir, "report_photo_${System.currentTimeMillis()}.jpg")
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                photoUriString = Uri.fromFile(file).toString()
            } catch (_: Exception) {
                // Keep bitmap in memory
            }
            Toast.makeText(context, "Foto kamera langsung berhasil diambil dengan stempel GPS!", Toast.LENGTH_SHORT).show()
        }
    }

    // Camera Runtime Permission Launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch(null)
        } else {
            permissionDeniedDialogTitle = "Izin Kamera Diperlukan"
            permissionDeniedDialogMessage = "Izin kamera diperlukan untuk mengambil foto bukti langsung kejadian bencana di lapangan dengan stempel digital anti-hoax Pusdalops BPBD."
            Toast.makeText(context, "Izin kamera diperlukan untuk mengambil foto bukti", Toast.LENGTH_SHORT).show()
        }
    }

    // Location fetcher internal helper
    val fetchGpsLocationInternal: () -> Unit = {
        isFetchingGps = true
        gpsStatusMessage = "Mencari sinyal GPS satelit ponsel..."
        LocationHelper.fetchCurrentLocation(
            context = context,
            onSuccess = { lat, lon, nearestDistrict, _, accuracy ->
                latitude = lat
                longitude = lon
                gpsAccuracy = accuracy
                isGpsLocked = true
                isFetchingGps = false
                selectedDistrict = nearestDistrict.name
                val villageList = VILLAGES_MAP[nearestDistrict.name]
                if (villageList != null && villageList.isNotEmpty()) {
                    selectedVillage = villageList.first()
                }
                gpsStatusMessage = "Koordinat GPS Terkunci: %.5f, %.5f (Akurasi: ±%.0fm)".format(lat, lon, accuracy ?: 5f)
                Toast.makeText(context, "Titik GPS ponsel berhasil dikunci!", Toast.LENGTH_SHORT).show()
            },
            onError = { err ->
                isFetchingGps = false
                gpsStatusMessage = "GPS belum aktif: $err"
                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
            }
        )
    }

    // Location Runtime Permission Launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            fetchGpsLocationInternal()
        } else {
            isFetchingGps = false
            gpsStatusMessage = "Izin GPS ditolak. Sentuh tombol pin untuk mencoba lagi."
            permissionDeniedDialogTitle = "Izin Lokasi GPS Diperlukan"
            permissionDeniedDialogMessage = "Izin lokasi presisi (GPS) diperlukan untuk memvalidasi posisi akurat bencana warga dan mengarahkan armada penyelamat TRC BPBD."
            Toast.makeText(context, "Izin lokasi diperlukan untuk mengunci koordinat bencana", Toast.LENGTH_SHORT).show()
        }
    }

    // Function to trigger GPS with runtime permission check
    val fetchGpsLocation = {
        val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (fineGranted || coarseGranted) {
            fetchGpsLocationInternal()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Function to trigger Camera with runtime permission check & auto-geotag
    val launchCameraWithPermission = {
        // Automatically request GPS locking if not yet locked to ensure geotagged photo
        if (!isGpsLocked) {
            val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val coarseGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            if (fineGranted || coarseGranted) {
                fetchGpsLocationInternal()
            }
        }

        val cameraGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (cameraGranted) {
            cameraLauncher.launch(null)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF15803D)) // Signature Mukomuko green
            .testTag("citizen_report_screen")
    ) {
        // 1. Top Bar Header (Orange, matching screenshot)
        Surface(
            color = Color(0xFFEA580C),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("citizen_report_header")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Kembali",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = "Laporan warga",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "PUSDALOPS BPBD Kab. Mukomuko",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Fast Call 117 Header Button
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White.copy(alpha = 0.20f),
                    modifier = Modifier.clickable {
                        EmergencyDialer.dial(context, "117")
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneInTalk,
                            contentDescription = "Call 117",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "117 BPBD",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Officer BPBD Access Lock/Status Button
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isOfficerLoggedIn) Color(0xFF16A34A) else Color.White.copy(alpha = 0.20f),
                    modifier = Modifier.clickable {
                        if (isOfficerLoggedIn) {
                            selectedTab = 4
                        } else {
                            Toast.makeText(context, "Login dengan ID petugas untuk akses ini", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isOfficerLoggedIn) Icons.Default.Shield else Icons.Default.Lock,
                            contentDescription = "Akses Petugas",
                            tint = if (isOfficerLoggedIn) Color.White else Color(0xFFFBBF24),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isOfficerLoggedIn) "TRC ON" else "Petugas",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Sub Navigation Tabs: 0: Formulir, 1: Peta Sebaran, 2: Tabel Riwayat, 3: Kontak Penting
        Surface(
            color = Color(0xFF15803D),
            modifier = Modifier.fillMaxWidth()
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF15803D),
                contentColor = Color.White,
                edgePadding = 8.dp,
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            height = 3.dp,
                            color = Color(0xFFFBBF24)
                        )
                    }
                },
                divider = {}
            ) {
                // Tab 0: Formulir
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = "Formulir Laporan",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp,
                            color = if (selectedTab == 0) Color.White else Color.White.copy(alpha = 0.7f)
                        )
                    }
                )

                // Tab 1: Peta Sebaran (Google Maps / GIS)
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Map,
                                contentDescription = null,
                                tint = if (selectedTab == 1) Color(0xFFFBBF24) else Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Peta Sebaran",
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp,
                                color = if (selectedTab == 1) Color.White else Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                )

                // Tab 2: Tabel Riwayat
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Tabel Riwayat",
                                fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp,
                                color = if (selectedTab == 2) Color.White else Color.White.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFFBBF24)
                            ) {
                                Text(
                                    text = "${reports.size}",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                )

                // Tab 3: Kontak Penting
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = if (selectedTab == 3) Color(0xFFFBBF24) else Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Kontak Penting",
                                fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp,
                                color = if (selectedTab == 3) Color.White else Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                )

                // Tab 4: Modul Khusus Petugas (Konsol Validasi TRC)
                Tab(
                    selected = selectedTab == 4,
                    onClick = {
                        if (isOfficerLoggedIn) {
                            selectedTab = 4
                        } else {
                            Toast.makeText(context, "Login dengan ID petugas untuk akses ini", Toast.LENGTH_SHORT).show()
                        }
                    },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isOfficerLoggedIn) Icons.Default.Shield else Icons.Default.Lock,
                                contentDescription = null,
                                tint = if (selectedTab == 4) Color(0xFFFBBF24) else Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Modul Petugas",
                                fontWeight = if (selectedTab == 4) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp,
                                color = if (selectedTab == 4) Color.White else Color.White.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(
                                shape = CircleShape,
                                color = if (isOfficerLoggedIn) Color(0xFFFBBF24) else Color.White.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = if (isOfficerLoggedIn) "AKTIF" else "KHUSUS",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOfficerLoggedIn) Color.Black else Color.White,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                )
            }
        }

        // Main White Rounded Card Body
        Surface(
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            when (selectedTab) {
                0 -> {
                    // TAB 0: FORMULIR INPUT (Matches Screenshot)
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // SECTION 1: DATA BENCANA (as in screenshot)
                        item {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "DATA BENCANA",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4B5563),
                                    letterSpacing = 1.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }

                        // Field 1: Jenis Bencana (Dropdown)
                        item {
                            ReportDropdownField(
                                label = "Jenis Bencana",
                                value = selectedDisasterType,
                                isExpanded = isDisasterDropdownExpanded,
                                onToggleExpand = { isDisasterDropdownExpanded = !isDisasterDropdownExpanded },
                                onDismiss = { isDisasterDropdownExpanded = false }
                            ) {
                                DISASTER_TYPES.forEach { disaster ->
                                    DropdownMenuItem(
                                        text = { Text(disaster, fontSize = 13.sp) },
                                        onClick = {
                                            selectedDisasterType = disaster
                                            isDisasterDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                            HorizontalDivider(color = Color(0xFFE5E7EB), thickness = 1.dp, modifier = Modifier.padding(top = 10.dp))
                        }

                        // Field 2: Waktu Kejadian
                        item {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Waktu Kejadian",
                                            fontSize = 11.sp,
                                            color = Color(0xFF6B7280)
                                        )
                                        Text(
                                            text = incidentTime,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF111827)
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFF3F4F6),
                                        modifier = Modifier.clickable {
                                            val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm 'WIB'", Locale("id", "ID"))
                                            incidentTime = sdf.format(Date())
                                            Toast.makeText(context, "Waktu disinkronkan ke jam sekarang", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Text(
                                            text = "Set Sekarang",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF15803D),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                            HorizontalDivider(color = Color(0xFFE5E7EB), thickness = 1.dp, modifier = Modifier.padding(top = 10.dp))
                        }

                        // Field 3: Lokasi Kejadian (Input Text)
                        item {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Lokasi Kejadian",
                                    fontSize = 11.sp,
                                    color = Color(0xFF6B7280)
                                )
                                OutlinedTextField(
                                    value = incidentLocation,
                                    onValueChange = { incidentLocation = it },
                                    placeholder = { Text("Nama jalan, dusun, atau patokan lokasi...", fontSize = 13.sp, color = Color(0xFF9CA3AF)) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    singleLine = true,
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color(0xFF15803D),
                                        unfocusedIndicatorColor = Color.Transparent
                                    )
                                )
                            }
                            HorizontalDivider(color = Color(0xFFE5E7EB), thickness = 1.dp, modifier = Modifier.padding(top = 2.dp))
                        }

                        // Field 4: Kecamatan (Dropdown 15 Kecamatan)
                        item {
                            ReportDropdownField(
                                label = "Kecamatan",
                                value = selectedDistrict,
                                isExpanded = isDistrictDropdownExpanded,
                                onToggleExpand = { isDistrictDropdownExpanded = !isDistrictDropdownExpanded },
                                onDismiss = { isDistrictDropdownExpanded = false }
                            ) {
                                DISTRICT_NAMES.forEach { district ->
                                    DropdownMenuItem(
                                        text = { Text(district, fontSize = 13.sp) },
                                        onClick = {
                                            selectedDistrict = district
                                            isDistrictDropdownExpanded = false
                                            val villages = VILLAGES_MAP[district]
                                            if (villages != null && villages.isNotEmpty()) {
                                                selectedVillage = villages.first()
                                            }
                                        }
                                    )
                                }
                            }
                            HorizontalDivider(color = Color(0xFFE5E7EB), thickness = 1.dp, modifier = Modifier.padding(top = 10.dp))
                        }

                        // Field 5: Desa/Kelurahan (Dropdown)
                        item {
                            val villages = VILLAGES_MAP[selectedDistrict] ?: listOf("Desa Pasar", "Desa Pusat")
                            ReportDropdownField(
                                label = "Desa/Kelurahan",
                                value = selectedVillage,
                                isExpanded = isVillageDropdownExpanded,
                                onToggleExpand = { isVillageDropdownExpanded = !isVillageDropdownExpanded },
                                onDismiss = { isVillageDropdownExpanded = false }
                            ) {
                                villages.forEach { village ->
                                    DropdownMenuItem(
                                        text = { Text(village, fontSize = 13.sp) },
                                        onClick = {
                                            selectedVillage = village
                                            isVillageDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                            HorizontalDivider(color = Color(0xFFE5E7EB), thickness = 1.dp, modifier = Modifier.padding(top = 10.dp))
                        }

                        // Field 6: Titik Lokasi with circular blue GPS pin button (matches screenshot exactly!)
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Titik Lokasi",
                                        fontSize = 11.sp,
                                        color = Color(0xFF6B7280)
                                    )
                                    Text(
                                        text = if (isGpsLocked) "%.5f, %.5f".format(latitude, longitude) else "Belum terkunci (Sentuh tombol biru)",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isGpsLocked) Color(0xFF15803D) else Color(0xFF374151)
                                    )
                                    if (isGpsLocked) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = Color(0xFF16A34A),
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Akurasi: ±%.0fm • Anti-Hoax Aktif".format(gpsAccuracy ?: 5f),
                                                fontSize = 10.sp,
                                                color = Color(0xFF16A34A),
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = gpsStatusMessage,
                                            fontSize = 10.sp,
                                            color = Color(0xFF9CA3AF)
                                        )
                                    }
                                }

                                // Circular Blue GPS Button matching the screenshot
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF2563EB),
                                    shadowElevation = 4.dp,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clickable { fetchGpsLocation() }
                                        .testTag("btn_get_gps_coordinates")
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        if (isFetchingGps) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(22.dp),
                                                color = Color.White,
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.LocationOn,
                                                contentDescription = "Ambil Titik Lokasi GPS",
                                                tint = Color.White,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            HorizontalDivider(color = Color(0xFFE5E7EB), thickness = 1.dp, modifier = Modifier.padding(top = 10.dp))
                        }

                        // SECTION 2: DATA KORBAN JIWA (as in screenshot)
                        item {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            ) {
                                Text(
                                    text = "DATA KORBAN JIWA",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4B5563),
                                    letterSpacing = 1.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        // Counter 1: Meninggal Dunia
                        item {
                            CasualtyCounterRow(
                                label = "Meninggal Dunia",
                                count = deadCount,
                                onIncrement = { deadCount++ },
                                onDecrement = { if (deadCount > 0) deadCount-- }
                            )
                            HorizontalDivider(color = Color(0xFFE5E7EB), thickness = 1.dp, modifier = Modifier.padding(top = 10.dp))
                        }

                        // Counter 2: Luka
                        item {
                            CasualtyCounterRow(
                                label = "Luka",
                                count = injuredCount,
                                onIncrement = { injuredCount++ },
                                onDecrement = { if (injuredCount > 0) injuredCount-- }
                            )
                            HorizontalDivider(color = Color(0xFFE5E7EB), thickness = 1.dp, modifier = Modifier.padding(top = 10.dp))
                        }

                        // Counter 3: Hilang
                        item {
                            CasualtyCounterRow(
                                label = "Hilang",
                                count = missingCount,
                                onIncrement = { missingCount++ },
                                onDecrement = { if (missingCount > 0) missingCount-- }
                            )
                            HorizontalDivider(color = Color(0xFFE5E7EB), thickness = 1.dp, modifier = Modifier.padding(top = 10.dp))
                        }

                        // SECTION 3: FOTO KEJADIAN DENGAN KAMERA & GEOTAG ANTI-PALSU
                        item {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isGpsLocked) Color(0xFFF0FDF4) else Color(0xFFFFFBEB)
                                ),
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isGpsLocked) Color(0xFF86EFAC) else Color(0xFFFCD34D)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("card_photo_geotag_verification")
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (isGpsLocked) Icons.Default.CheckCircle else Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = if (isGpsLocked) Color(0xFF16A34A) else Color(0xFFD97706),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Kamera Langsung & Geotag Anti-Palsu",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isGpsLocked) Color(0xFF15803D) else Color(0xFF92400E)
                                        )
                                    }

                                    Text(
                                        text = "Ambil foto langsung melalui kamera ponsel di lokasi kejadian. Koordinat GPS dan waktu saat ini akan dicap secara digital untuk memverifikasi keabsahan laporan.",
                                        fontSize = 11.sp,
                                        color = Color(0xFF4B5563),
                                        lineHeight = 15.sp,
                                        modifier = Modifier.padding(vertical = 6.dp)
                                    )

                                    // Photo Preview with Geotag Watermark
                                    if (cameraCapturedBitmap != null || photoUriString != null) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(180.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .border(1.dp, Color(0xFFD1D5DB), RoundedCornerShape(12.dp))
                                        ) {
                                            if (cameraCapturedBitmap != null) {
                                                Image(
                                                    bitmap = cameraCapturedBitmap!!.asImageBitmap(),
                                                    contentDescription = "Foto Kamera Kejadian",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                AsyncImage(
                                                    model = photoUriString,
                                                    contentDescription = "Foto Galeri Kejadian",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }

                                            // Digital Geotag Watermark Overlay
                                            Surface(
                                                color = Color.Black.copy(alpha = 0.75f),
                                                shape = RoundedCornerShape(topStart = 8.dp),
                                                modifier = Modifier.align(Alignment.BottomEnd)
                                            ) {
                                                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                                                    Text(
                                                        text = "📍 %.5f, %.5f".format(latitude, longitude),
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFFFBBF24)
                                                    )
                                                    Text(
                                                        text = "🕒 $incidentTime",
                                                        fontSize = 9.sp,
                                                        color = Color.White
                                                    )
                                                    Text(
                                                        text = "🛡️ KAMERA & GPS TERVERIFIKASI BPBD",
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = Color(0xFF86EFAC)
                                                    )
                                                }
                                            }

                                            // Close/Remove photo button
                                            IconButton(
                                                onClick = {
                                                    cameraCapturedBitmap = null
                                                    photoUriString = null
                                                },
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(4.dp)
                                                    .size(28.dp)
                                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Hapus Foto",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Action Button: Buka Kamera Langsung Saja (Pilihan galeri dinonaktifkan demi integritas anti-palsu)
                                    Button(
                                        onClick = { launchCameraWithPermission() },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("btn_launch_camera"),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CameraAlt,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Buka Kamera Langsung (Geotag Anti-Palsu)",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Surface(
                                        color = Color(0xFFFEF2F2),
                                        shape = RoundedCornerShape(8.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Shield,
                                                contentDescription = null,
                                                tint = Color(0xFFDC2626),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Pilihan galeri dinonaktifkan. Demi integritas anti-palsu, hanya foto kamera langsung di tempat kejadian dengan stempel satelit GPS yang diterima sistem BPBD.",
                                                fontSize = 9.sp,
                                                color = Color(0xFF991B1B),
                                                lineHeight = 12.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Dedicated Card: Deskripsi Kejadian / Jenis Bencana Lainnya (Wajib Diisi)
                        item {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFFFFBEB)
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.2.dp,
                                    if (additionalNotes.isBlank()) Color(0xFFF59E0B) else Color(0xFF10B981)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("card_deskripsi_kejadian_lainnya")
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Info,
                                                contentDescription = null,
                                                tint = Color(0xFFD97706),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Deskripsi Kejadian / Bencana Lainnya",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF92400E)
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (additionalNotes.isBlank()) Color(0xFFDC2626) else Color(0xFF15803D)
                                        ) {
                                            Text(
                                                text = if (additionalNotes.isBlank()) "Wajib Diisi *" else "Terisi ✓",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = "Wajib diisi: Jelaskan kronologi kejadian, dampak kerusakan, kebutuhan mendesak, atau rincian jika jenis bencana tidak tercantum pada pilihan di atas.",
                                        fontSize = 10.sp,
                                        color = Color(0xFF78350F),
                                        lineHeight = 14.sp
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    OutlinedTextField(
                                        value = additionalNotes,
                                        onValueChange = { additionalNotes = it },
                                        placeholder = {
                                            Text(
                                                "Tuliskan rincian kejadian, kondisi terkini, atau jenis bencana lainnya di sini...",
                                                fontSize = 12.sp,
                                                color = Color(0xFF9CA3AF)
                                            )
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("input_deskripsi_kejadian_lainnya"),
                                        minLines = 3,
                                        maxLines = 6,
                                        shape = RoundedCornerShape(10.dp),
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = Color.White,
                                            unfocusedContainerColor = Color.White,
                                            focusedIndicatorColor = Color(0xFFD97706),
                                            unfocusedIndicatorColor = Color(0xFFE5E7EB)
                                        )
                                    )
                                }
                            }
                        }

                        // Optional Pelapor Info (Name & Notes)
                        item {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Nama Pelapor (Opsional)",
                                    fontSize = 11.sp,
                                    color = Color(0xFF6B7280)
                                )
                                OutlinedTextField(
                                    value = reporterName,
                                    onValueChange = { reporterName = it },
                                    placeholder = { Text("Contoh: Budi (Warga RT 02)", fontSize = 13.sp) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    singleLine = true
                                )
                            }
                        }

                        // Submit Button
                        item {
                            Button(
                                onClick = {
                                    if (incidentLocation.isBlank()) {
                                        Toast.makeText(context, "Mohon isi rincian Lokasi Kejadian", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    if (additionalNotes.isBlank()) {
                                        Toast.makeText(context, "Kolom deskripsi kejadian / jenis bencana lainnya wajib diisi!", Toast.LENGTH_LONG).show()
                                        return@Button
                                    }
                                    if (!isGpsLocked) {
                                        Toast.makeText(context, "Mohon sentuh tombol biru untuk mengunci koordinat GPS ponsel agar aduan terverifikasi anti-hoax", Toast.LENGTH_LONG).show()
                                        fetchGpsLocation()
                                        return@Button
                                    }

                                    val reportId = "LAP-MMK-" + SimpleDateFormat("yyyyMMdd", Locale.US).format(Date()) + "-" + (100..999).random()
                                    val report = CitizenDisasterReport(
                                        id = reportId,
                                        disasterType = selectedDisasterType,
                                        incidentTime = incidentTime,
                                        incidentLocation = incidentLocation,
                                        district = selectedDistrict,
                                        village = selectedVillage,
                                        latitude = latitude,
                                        longitude = longitude,
                                        gpsAccuracyMeters = gpsAccuracy,
                                        isGpsVerified = isGpsLocked,
                                        casualtiesDead = deadCount,
                                        casualtiesInjured = injuredCount,
                                        casualtiesMissing = missingCount,
                                        photoUri = photoUriString,
                                        reporterName = reporterName.ifBlank { "Warga $selectedDistrict" },
                                        reporterPhone = reporterPhone,
                                        additionalNotes = additionalNotes,
                                        timestamp = System.currentTimeMillis(),
                                        status = "Diproses" // Standard initial status: 'Diproses'
                                    )
                                    onSubmitReport(report)
                                    submittedReportId = reportId
                                    showSuccessDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("btn_submit_citizen_report")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "KIRIM LAPORAN RESMI KE BPBD",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(40.dp))
                        }
                    }
                }

                1 -> {
                    // TAB 1: PETA SEBARAN LAPORAN (Google Maps / GIS Spatial Map)
                    CitizenReportMapView(
                        reports = reports,
                        onUpdateReportStatus = onUpdateReportStatus
                    )
                }

                2 -> {
                    // TAB 2: TABEL RIWAYAT LAPORAN WARGA (With Status Filters, Cloud Sync, & Officer Role-Gating)
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 1. Officer Role & Authentication Status Card
                        item {
                            Surface(
                                color = if (isOfficerLoggedIn) Color(0xFFF0FDF4) else Color(0xFFEFF6FF),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isOfficerLoggedIn) Color(0xFFBBF7D0) else Color(0xFFBFDBFE)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (!isOfficerLoggedIn) {
                                            Toast.makeText(context, "Login dengan ID petugas untuk akses ini", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (isOfficerLoggedIn) Icons.Default.Badge else Icons.Default.Shield,
                                            contentDescription = null,
                                            tint = if (isOfficerLoggedIn) Color(0xFF15803D) else Color(0xFF2563EB),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = if (isOfficerLoggedIn) {
                                                    "PETUGAS JAGA AKTIF: $officerName"
                                                } else {
                                                    "Akses Respon: Khusus Petugas BPBD"
                                                },
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isOfficerLoggedIn) Color(0xFF14532D) else Color(0xFF1E40AF)
                                            )
                                            Text(
                                                text = if (isOfficerLoggedIn) {
                                                    "ID/NIP: $officerBadgeId • $officerShift"
                                                } else {
                                                    "Hanya Petugas Jaga terdaftar yang dapat memvalidasi & mengubah status laporan"
                                                },
                                                fontSize = 10.sp,
                                                color = if (isOfficerLoggedIn) Color(0xFF166534) else Color(0xFF3B82F6)
                                            )
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Button(
                                            onClick = {
                                                if (isOfficerLoggedIn) {
                                                    selectedTab = 4
                                                } else {
                                                    Toast.makeText(context, "Login dengan ID petugas untuk akses ini", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isOfficerLoggedIn) Color(0xFF15803D) else Color(0xFF475569)
                                            ),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                            modifier = Modifier.height(34.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isOfficerLoggedIn) Icons.Default.Shield else Icons.Default.Lock,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "Konsol Petugas",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                if (isOfficerLoggedIn) {
                                                    officerAuthManager.logoutOfficer()
                                                    isOfficerLoggedIn = false
                                                    officerName = officerAuthManager.getOfficerName()
                                                    officerBadgeId = officerAuthManager.getOfficerBadgeId()
                                                    officerShift = officerAuthManager.getOfficerShift()
                                                    officerRole = officerAuthManager.getOfficerRole()
                                                    Toast.makeText(context, "Sesi Petugas Jaga diakhiri (Mode Warga Aktif)", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, "Login dengan ID petugas untuk akses ini", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isOfficerLoggedIn) Color(0xFFDC2626) else Color(0xFF2563EB)
                                            ),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                            modifier = Modifier.height(34.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isOfficerLoggedIn) Icons.Default.LockOpen else Icons.Default.Lock,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (isOfficerLoggedIn) "Keluar" else "PIN",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 2. Cloud Synchronization Card across Android phones
                        item {
                            Surface(
                                color = Color(0xFFFFFBEB),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CloudSync,
                                            contentDescription = null,
                                            tint = Color(0xFFD97706),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "Sinkronisasi Cloud Antar-Ponsel",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF92400E)
                                            )
                                            Text(
                                                text = lastSyncStatusMessage ?: "Sinkronkan agar laporan warga dari ponsel lain tampil otomatis.",
                                                fontSize = 9.sp,
                                                color = Color(0xFFB45309)
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            if (!isSyncingOnline) {
                                                isSyncingOnline = true
                                                onSyncReports?.invoke { count ->
                                                    isSyncingOnline = false
                                                    lastSyncStatusMessage = "Tersinkron: $count laporan diperbarui dari server"
                                                    Toast.makeText(
                                                        context,
                                                        "Sinkronisasi selesai! $count laporan warga terunduh dari server Pusdalops.",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                } ?: run {
                                                    isSyncingOnline = false
                                                }
                                            }
                                        },
                                        enabled = !isSyncingOnline,
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        if (isSyncingOnline) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(14.dp),
                                                strokeWidth = 2.dp,
                                                color = Color.White
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Sync,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isSyncingOnline) "Menyinkronkan..." else "Sinkronkan",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        // 3. Filter by status chips
                        item {
                            Surface(
                                color = Color(0xFFF3F4F6),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = Color(0xFF15803D),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Filter Riwayat Laporan Bencana",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF111827)
                                        )
                                    }
                                    Text(
                                        text = "Indikator status: 'Diproses' (kuning), 'Divalidasi' (hijau), 'Selesai' (biru). Terkoneksi langsung dengan Google Maps & GPS Kamera.",
                                        fontSize = 10.sp,
                                        color = Color(0xFF4B5563),
                                        modifier = Modifier.padding(top = 2.dp)
                                    )

                                    // Filter by status chips
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf("Semua", "Diproses", "Divalidasi", "Selesai").forEach { st ->
                                            val isSel = tableStatusFilter == st
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = if (isSel) Color(0xFF15803D) else Color.White,
                                                border = androidx.compose.foundation.BorderStroke(
                                                    1.dp,
                                                    if (isSel) Color(0xFF15803D) else Color(0xFFD1D5DB)
                                                ),
                                                modifier = Modifier.clickable { tableStatusFilter = st }
                                            ) {
                                                Text(
                                                    text = st,
                                                    fontSize = 10.sp,
                                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSel) Color.White else Color(0xFF4B5563),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        val displayedReports = if (tableStatusFilter == "Semua") {
                            reports
                        } else {
                            reports.filter { it.status.equals(tableStatusFilter, ignoreCase = true) }
                        }

                        if (displayedReports.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Tidak ada laporan dengan status '$tableStatusFilter'.",
                                        fontSize = 13.sp,
                                        color = Color(0xFF6B7280)
                                    )
                                }
                            }
                        } else {
                            items(displayedReports) { report ->
                                CitizenReportItemCard(
                                    report = report,
                                    isOfficerLoggedIn = isOfficerLoggedIn,
                                    onUpdateStatus = { newStatus ->
                                        if (isOfficerLoggedIn) {
                                            onUpdateReportStatus(report.id, newStatus)
                                            Toast.makeText(
                                                context,
                                                "Status laporan #${report.id.takeLast(7)} diubah menjadi '$newStatus' oleh Petugas $officerName",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "Login dengan ID petugas untuk akses ini",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    },
                                    onViewPhoto = { photoUri ->
                                        previewPhotoUri = photoUri
                                        previewPhotoReport = report
                                    }
                                )
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(40.dp))
                        }
                    }
                }

                3 -> {
                    // TAB 3: KONTAK PENTING DARURAT MUKOMUKO
                    CitizenEmergencyContactsView()
                }

                4 -> {
                    // TAB 4: KONSOL KHUSUS PETUGAS PUSDALOPS BPBD & TIM TRC
                    if (isOfficerLoggedIn) {
                        OfficerDashboardConsoleView(
                            reports = reports,
                            currentDistrict = currentDistrict,
                            isOfficerLoggedIn = isOfficerLoggedIn,
                            officerName = officerName,
                            officerBadgeId = officerBadgeId,
                            officerShift = officerShift,
                            officerRole = officerRole,
                            onLoginClick = { showOfficerLoginDialog = true },
                            onLogoutClick = {
                                officerAuthManager.logoutOfficer()
                                isOfficerLoggedIn = false
                                officerName = officerAuthManager.getOfficerName()
                                officerBadgeId = officerAuthManager.getOfficerBadgeId()
                                officerShift = officerAuthManager.getOfficerShift()
                                officerRole = officerAuthManager.getOfficerRole()
                                selectedTab = 0
                                Toast.makeText(context, "Sesi Petugas Jaga diakhiri (Mode Warga Aktif)", Toast.LENGTH_SHORT).show()
                            },
                            onUpdateReportStatus = onUpdateReportStatus,
                            onUpdateReportStatusAndNotes = onUpdateReportStatusAndNotes,
                            onViewPhoto = { photoUri, rep ->
                                previewPhotoUri = photoUri
                                previewPhotoReport = rep
                            },
                            onSyncReports = onSyncReports
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable {
                                    Toast.makeText(context, "Login dengan ID petugas untuk akses ini", Toast.LENGTH_SHORT).show()
                                }
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFFEF2F2),
                                modifier = Modifier.size(64.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Terkunci",
                                        tint = Color(0xFFDC2626),
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Modul Khusus Petugas BPBD",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Konsol validasi status dan triage laporan hanya dapat diakses oleh petugas jaga resmi Pusdalops / TRC dengan PIN keamanan.",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    Toast.makeText(context, "Login dengan ID petugas untuk akses ini", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D))
                            ) {
                                Icon(imageVector = Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Masuk dengan PIN Petugas")
                            }
                        }
                    }
                }
            }
        }
    }

    // Permission Guidance Dialog for CAMERA & LOCATION
    if (permissionDeniedDialogMessage != null) {
        AlertDialog(
            onDismissRequest = { permissionDeniedDialogMessage = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = permissionDeniedDialogTitle ?: "Izin Akses Diperlukan",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = permissionDeniedDialogMessage.orEmpty(),
                    fontSize = 12.sp,
                    color = Color(0xFF374151),
                    lineHeight = 16.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val isCamera = permissionDeniedDialogTitle?.contains("Kamera") == true
                        permissionDeniedDialogMessage = null
                        if (isCamera) {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        } else {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D))
                ) {
                    Text("Beri Izin Sekarang")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { permissionDeniedDialogMessage = null }) {
                    Text("Nanti Saja")
                }
            }
        )
    }

    // Success Confirmation Dialog
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF16A34A),
                    modifier = Modifier.size(44.dp)
                )
            },
            title = {
                Text(
                    text = "Laporan Bencana Terkirim!",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Laporan Anda telah berhasil dicatat oleh sistem PUSDALOPS BPBD Kabupaten Mukomuko.",
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        color = Color(0xFF374151)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF3F4F6),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(text = "Nomor Aduan: $submittedReportId", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(text = "Jenis: $selectedDisasterType", fontSize = 11.sp)
                            Text(text = "Kecamatan: $selectedDistrict", fontSize = 11.sp)
                            Text(text = "Status: 🟡 Diproses (Menunggu Verifikasi Lapangan)", fontSize = 11.sp, color = Color(0xFFEA580C), fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Untuk situasi darurat penyelamatan jiwa mendesak, segera hubungi Call Center 117.",
                        fontSize = 11.sp,
                        color = Color(0xFFDC2626),
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        selectedTab = 2 // Switch to table view to see the report
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D))
                ) {
                    Text("Lihat Riwayat Laporan")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showSuccessDialog = false
                        EmergencyDialer.dial(context, "117")
                    }
                ) {
                    Text("Panggil 117")
                }
            }
        )
    }

    // Officer Login & PIN Verification Dialog
    if (showOfficerLoginDialog) {
        var roster by remember { mutableStateOf(officerAuthManager.getOfficerRoster()) }
        var selectedRoleFilter by remember { mutableStateOf(OfficerRoles.OPERATOR) }
        var selectedOfficer by remember {
            mutableStateOf(
                roster.firstOrNull { it.nip == officerBadgeId }
                    ?: roster.firstOrNull { it.role == OfficerRoles.OPERATOR }
                    ?: roster.firstOrNull()
            )
        }
        var dialogMode by remember { mutableIntStateOf(0) } // 0 = Pilih dari Roster / Dropdown, 1 = Form Input Petugas Baru (Admin Only)
        var isNameDropdownOpen by remember { mutableStateOf(false) }
        var rosterSearchQuery by remember { mutableStateOf("") }

        // PIN state
        var inputPin by remember { mutableStateOf("") }
        var isPinVisible by remember { mutableStateOf(false) }
        var isError by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf("") }

        // Admin Security Prompt for manual input protection
        var showAdminPromptForAdd by remember { mutableStateOf(false) }
        var adminPinAttempt by remember { mutableStateOf("") }
        var adminPinErrorMsg by remember { mutableStateOf("") }

        // Form Input Petugas Baru (Format: Nama Petugas & NIP selaku ASN/TRC BPBD)
        var inputFullName by remember { mutableStateOf("") }
        var inputNip by remember { mutableStateOf("") }
        var inputRole by remember { mutableStateOf(OfficerRoles.OPERATOR) }
        var inputShift by remember { mutableStateOf("Shift Siaga 24 Jam") }
        var inputUnit by remember { mutableStateOf("Pusdalops BPBD Kab. Mukomuko") }
        var formValidationError by remember { mutableStateOf("") }

        val filteredRoster = roster.filter { off ->
            val matchesRole = if (selectedRoleFilter == "Semua") true else off.role == selectedRoleFilter
            val matchesSearch = if (rosterSearchQuery.isBlank()) true else {
                val q = rosterSearchQuery.trim().lowercase()
                off.fullName.lowercase().contains(q) || off.nip.lowercase().contains(q) || off.unit.lowercase().contains(q)
            }
            matchesRole && matchesSearch
        }

        AlertDialog(
            onDismissRequest = {
                showOfficerLoginDialog = false
                onOfficerLoginDismissed?.invoke()
                pendingStatusReportId = null
                pendingStatusTarget = null
            },
            icon = {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFEFF6FF),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (dialogMode == 1) Icons.Default.PersonAdd else Icons.Default.Badge,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            },
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (dialogMode == 1) "Input Petugas Baru (Admin)" else "Login Kredensial Petugas Pusdalops",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "BPBD Kabupaten Mukomuko",
                        fontSize = 11.sp,
                        color = Color(0xFF2563EB),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (dialogMode == 0) {
                        // MODE 0: PILIH DARI DATABASE RESMI PUSDALOPS (ANTI-SPOOFING)
                        Text(
                            text = "Pilih nama personil ASN BPBD dari daftar resmi kredensial. Nama dan NIP ditampilkan terpisah dan terkunci guna mencegah manipulasi identitas oleh pihak tidak berwenang.",
                            fontSize = 11.sp,
                            color = Color(0xFF4B5563),
                            lineHeight = 15.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Category Tabs: Operator Pusdalops (Default), Petugas Jaga, Lapangan TRC, Semua
                        Text(
                            text = "PILIHAN TUGAS KEDINASAN:",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B),
                            letterSpacing = 0.5.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val roleFilters = listOf(
                                OfficerRoles.OPERATOR to "🎧 Operator (${roster.count { it.role == OfficerRoles.OPERATOR }})",
                                OfficerRoles.PETUGAS_JAGA to "🛡️ Petugas Jaga (${roster.count { it.role == OfficerRoles.PETUGAS_JAGA }})",
                                OfficerRoles.PETUGAS_LAPANGAN to "🚜 TRC (${roster.count { it.role == OfficerRoles.PETUGAS_LAPANGAN }})",
                                "Semua" to "Semua (${roster.size})"
                            )
                            items(roleFilters) { (roleKey, label) ->
                                val isSelected = selectedRoleFilter == roleKey
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isSelected) Color(0xFF2563EB) else Color(0xFFF1F5F9),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) Color(0xFF1D4ED8) else Color(0xFFCBD5E1)
                                    ),
                                    modifier = Modifier.clickable {
                                        selectedRoleFilter = roleKey
                                        // Auto pick first officer in selected role
                                        val firstInRole = roster.firstOrNull { if (roleKey == "Semua") true else it.role == roleKey }
                                        if (firstInRole != null) {
                                            selectedOfficer = firstInRole
                                        }
                                    }
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else Color(0xFF475569),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // DROPDOWN MENU PEMILIHAN NAMA PETUGAS ASN (OPERATOR PUSDALOPS)
                        Text(
                            text = "PILIH NAMA PETUGAS ASN (OPERATOR PUSDALOPS):",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E40AF),
                            letterSpacing = 0.5.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Box(modifier = Modifier.fillMaxWidth()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFEFF6FF),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF2563EB)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isNameDropdownOpen = true }
                                    .testTag("officer_name_dropdown_selector")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = Color(0xFF1D4ED8),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = selectedOfficer?.fullName ?: "Klik untuk memilih nama petugas...",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "NIP: ${selectedOfficer?.nip ?: "-"} • ${selectedOfficer?.role ?: "-"}",
                                            fontSize = 10.sp,
                                            color = Color(0xFF1E40AF)
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Buka Pilihan Nama",
                                        tint = Color(0xFF1D4ED8),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = isNameDropdownOpen,
                                onDismissRequest = { isNameDropdownOpen = false },
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .heightIn(max = 280.dp)
                            ) {
                                val availableInRole = filteredRoster.ifEmpty { roster }
                                availableInRole.forEach { off ->
                                    val isCurrent = selectedOfficer?.nip == off.nip
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    text = off.fullName,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                                                    color = if (isCurrent) Color(0xFF1D4ED8) else Color(0xFF0F172A)
                                                )
                                                Text(
                                                    text = "NIP: ${off.nip} • ${off.role}",
                                                    fontSize = 9.sp,
                                                    color = Color(0xFF64748B)
                                                )
                                            }
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Badge,
                                                contentDescription = null,
                                                tint = if (isCurrent) Color(0xFF2563EB) else Color.Gray,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        trailingIcon = {
                                            if (isCurrent) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color(0xFF16A34A),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        },
                                        onClick = {
                                            selectedOfficer = off
                                            isNameDropdownOpen = false
                                            isError = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // KREDENSIAL TERPISAH: KOLOM NAMA & KOLOM NIP (TERKUNCI / ANTI MANUAL INPUT)
                        Text(
                            text = "KREDENSIAL RESMI DARI DATABASE BPBD (TERPISAH & TERPROTEKSI):",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF475569),
                            letterSpacing = 0.5.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Kolom Terpisah 1: Nama Lengkap ASN BPBD (Read-Only)
                        OutlinedTextField(
                            value = selectedOfficer?.fullName ?: "",
                            onValueChange = {}, // Read-only: mencegah input manual pihak tak berwenang
                            readOnly = true,
                            enabled = false,
                            label = { Text("Kolom Nama Pegawai ASN BPBD", fontSize = 11.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color(0xFF1D4ED8),
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Terkunci Aman",
                                    tint = Color(0xFF059669),
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = TextFieldDefaults.colors(
                                disabledContainerColor = Color(0xFFF8FAFC),
                                disabledTextColor = Color(0xFF0F172A),
                                disabledLabelColor = Color(0xFF475569),
                                disabledIndicatorColor = Color(0xFFCBD5E1)
                            ),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("officer_credential_name_field")
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Kolom Terpisah 2: NIP Pegawai ASN (Read-Only)
                        OutlinedTextField(
                            value = selectedOfficer?.nip ?: "",
                            onValueChange = {}, // Read-only: mencegah input manual pihak tak berwenang
                            readOnly = true,
                            enabled = false,
                            label = { Text("Kolom NIP Pegawai ASN (18 Digit)", fontSize = 11.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Badge,
                                    contentDescription = null,
                                    tint = Color(0xFF1D4ED8),
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Terkunci Aman",
                                    tint = Color(0xFF059669),
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = TextFieldDefaults.colors(
                                disabledContainerColor = Color(0xFFF8FAFC),
                                disabledTextColor = Color(0xFF0F172A),
                                disabledLabelColor = Color(0xFF475569),
                                disabledIndicatorColor = Color(0xFFCBD5E1)
                            ),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("officer_credential_nip_field")
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Penugasan & Satuan Kerja Card
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF0FDF4),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HeadsetMic,
                                    contentDescription = null,
                                    tint = Color(0xFF15803D),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Tugas: ${selectedOfficer?.role ?: OfficerRoles.OPERATOR}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF166534)
                                    )
                                    Text(
                                        text = "${selectedOfficer?.unit ?: "Pusdalops BPBD Kab. Mukomuko"} • ${selectedOfficer?.shift ?: "Shift Siaga 24 Jam"}",
                                        fontSize = 9.sp,
                                        color = Color(0xFF15803D)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFFDCFCE7)
                                ) {
                                    Text(
                                        text = "TERVERIFIKASI",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF166534),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Anti-Spoofing & Security Indicator
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFFEF3C7),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = Color(0xFFB45309),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Proteksi Kredensial: Kolom Nama & NIP dikunci otomatis dari database resmi BPBD. Input manual dinonaktifkan guna menghindari pemalsuan identitas oleh pihak tidak berwenang.",
                                    fontSize = 9.sp,
                                    color = Color(0xFF92400E),
                                    lineHeight = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Tombol Tambah Petugas (Khusus Admin dengan PIN Master)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (officerAuthManager.isAdminLoggedIn()) {
                                        dialogMode = 1
                                        formValidationError = ""
                                    } else {
                                        showAdminPromptForAdd = true
                                        adminPinAttempt = ""
                                        adminPinErrorMsg = ""
                                    }
                                },
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Color(0xFF0F766E),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Kelola Personil (Admin)", fontSize = 10.sp, color = Color(0xFF0F766E))
                            }
                        }

                        if (showAdminPromptForAdd) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF1F5F9),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        text = "Verifikasi Hak Akses Administrator:",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E293B)
                                    )
                                    Text(
                                        text = "Pendaftaran atau modifikasi personil baru hanya dapat dilakukan oleh Admin Pusdalops PB.",
                                        fontSize = 9.sp,
                                        color = Color(0xFF475569)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    OutlinedTextField(
                                        value = adminPinAttempt,
                                        onValueChange = { adminPinAttempt = it },
                                        placeholder = { Text("Masukkan Sandi/PIN Admin...", fontSize = 10.sp) },
                                        singleLine = true,
                                        visualTransformation = PasswordVisualTransformation(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    if (adminPinErrorMsg.isNotBlank()) {
                                        Text(
                                            text = adminPinErrorMsg,
                                            fontSize = 9.sp,
                                            color = Color.Red,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        OutlinedButton(
                                            onClick = { showAdminPromptForAdd = false },
                                            modifier = Modifier.weight(1f).height(32.dp),
                                            contentPadding = PaddingValues(2.dp)
                                        ) {
                                            Text("Batal", fontSize = 10.sp)
                                        }
                                        Button(
                                            onClick = {
                                                if (officerAuthManager.verifyPin(adminPinAttempt) || officerAuthManager.loginAdmin("admin", adminPinAttempt)) {
                                                    showAdminPromptForAdd = false
                                                    dialogMode = 1
                                                    formValidationError = ""
                                                } else {
                                                    adminPinErrorMsg = "PIN/Sandi Admin salah. Akses ditolak demi keamanan."
                                                }
                                            },
                                            modifier = Modifier.weight(1f).height(32.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                                            contentPadding = PaddingValues(2.dp)
                                        ) {
                                            Text("Verifikasi", fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // PIN Input Field
                        OutlinedTextField(
                            value = inputPin,
                            onValueChange = {
                                inputPin = it
                                isError = false
                            },
                            label = { Text("PIN Keamanan Petugas", fontSize = 11.sp) },
                            placeholder = { Text("PIN Default: 117 / BPBD117 / TRC2026", fontSize = 11.sp) },
                            visualTransformation = if (isPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                IconButton(onClick = { isPinVisible = !isPinVisible }) {
                                    Icon(
                                        imageVector = if (isPinVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            isError = isError,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (isError) {
                            Text(
                                text = errorMessage.ifBlank { "PIN salah. Gunakan PIN resmi BPBD (117 / BPBD117 / TRC2026)." },
                                color = Color(0xFFDC2626),
                                fontSize = 10.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        } else {
                            Text(
                                text = "Petunjuk: PIN standar Pusdalops adalah 117 atau BPBD117",
                                color = Color(0xFF6B7280),
                                fontSize = 10.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                    } else {
                        // MODE 1: FORM INPUT NAMA PETUGAS BESERTA NIP ASN/TRC BPBD
                        Text(
                            text = "Formulir pendaftaran nama personil Pusdalops BPBD Kabupaten Mukomuko (Operator, Petugas Jaga, dan Personil Lapangan TRC) dengan format identitas resmi ASN/TRC.",
                            fontSize = 11.sp,
                            color = Color(0xFF4B5563),
                            lineHeight = 15.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = inputFullName,
                            onValueChange = {
                                inputFullName = it
                                formValidationError = ""
                            },
                            label = { Text("Nama Petugas & Gelar", fontSize = 11.sp) },
                            placeholder = { Text("Contoh: Budi Santoso, S.Sos / Rian Hidayat", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = inputNip,
                            onValueChange = {
                                inputNip = it
                                formValidationError = ""
                            },
                            label = { Text("NIP Pegawai ASN / No. Reg TRC", fontSize = 11.sp) },
                            placeholder = { Text("Contoh: 19850101 201001 1 001 atau TRC-MMK-2024-025", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Penugasan di Pusdalops BPBD:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF334155)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // 3 Role choices: Operator, Petugas Jaga, Lapangan TRC
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            val roleChoices = listOf(
                                Triple(OfficerRoles.OPERATOR, "🎧 Operator Pusdalops", "Pusat panggilan 117, radio HT, & data spasial"),
                                Triple(OfficerRoles.PETUGAS_JAGA, "🛡️ Petugas Jaga Siaga", "Piket Posko Siaga Bencana 24 Jam Pusdalops"),
                                Triple(OfficerRoles.PETUGAS_LAPANGAN, "🚜 Petugas Lapangan TRC", "Tim Reaksi Cepat evakuasi & kaji cepat darurat")
                            )

                            roleChoices.forEach { (roleVal, roleTitle, roleDesc) ->
                                val isSelected = inputRole == roleVal
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) Color(0xFFEFF6FF) else Color(0xFFF8FAFC),
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) Color(0xFF2563EB) else Color(0xFFE2E8F0)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { inputRole = roleVal }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = roleTitle,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color(0xFF1D4ED8) else Color(0xFF1E293B)
                                            )
                                            Text(
                                                text = roleDesc,
                                                fontSize = 9.sp,
                                                color = Color(0xFF64748B)
                                            )
                                        }
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = Color(0xFF2563EB),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = inputShift,
                                onValueChange = { inputShift = it },
                                label = { Text("Regu / Shift", fontSize = 11.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = inputUnit,
                                onValueChange = { inputUnit = it },
                                label = { Text("Unit / Satuan", fontSize = 11.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (formValidationError.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = formValidationError,
                                color = Color(0xFFDC2626),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { dialogMode = 0 },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Batal / Kembali", fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    if (inputFullName.trim().length < 3) {
                                        formValidationError = "Nama petugas minimal 3 karakter."
                                        return@Button
                                    }
                                    if (inputNip.trim().length < 4) {
                                        formValidationError = "NIP ASN / No. Reg TRC minimal 4 karakter."
                                        return@Button
                                    }
                                    val addResult = officerAuthManager.addOfficer(
                                        fullName = inputFullName,
                                        nip = inputNip,
                                        role = inputRole,
                                        shift = inputShift,
                                        unit = inputUnit
                                    )
                                    if (addResult.isSuccess) {
                                        val newOff = addResult.getOrNull()
                                        // Also persist to Room Database
                                        coroutineScope.launch {
                                            personnelRepository.insert(
                                                com.example.data.local.Personnel(
                                                    name = inputFullName.trim(),
                                                    nip = inputNip.trim(),
                                                    role = inputRole,
                                                    shift = inputShift.trim(),
                                                    unit = inputUnit.trim()
                                                )
                                            )
                                        }
                                        roster = officerAuthManager.getOfficerRoster()
                                        selectedOfficer = newOff
                                        dialogMode = 0
                                        formValidationError = ""
                                        Toast.makeText(
                                            context,
                                            "Petugas '${newOff?.fullName}' berhasil didaftarkan ke sistem Pusdalops!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        formValidationError = addResult.exceptionOrNull()?.message ?: "Gagal mendaftarkan petugas."
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                                modifier = Modifier.weight(1.3f)
                            ) {
                                Text("Simpan & Pilih Petugas", fontSize = 11.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (dialogMode == 0) {
                    Button(
                        onClick = {
                            val officer = selectedOfficer
                            if (officer == null) {
                                if (inputPin.isNotBlank() && officerAuthManager.verifyPin(inputPin)) {
                                    val success = officerAuthManager.loginOfficer(
                                        name = "Admin Pusdalops PB",
                                        badgeId = "ADMIN-BPBD-MMK-117",
                                        shift = "Komando Utama (24 Jam)",
                                        pin = inputPin,
                                        role = OfficerRoles.MANAJER
                                    )
                                    if (success) {
                                        isOfficerLoggedIn = true
                                        officerName = officerAuthManager.getOfficerName()
                                        officerBadgeId = officerAuthManager.getOfficerBadgeId()
                                        officerShift = officerAuthManager.getOfficerShift()
                                        officerRole = officerAuthManager.getOfficerRole()
                                        showOfficerLoginDialog = false
                                        selectedTab = 4 // Direct to Konsol Petugas
                                        Toast.makeText(
                                            context,
                                            "Masuk sebagai Administrator Pusdalops PB. Silakan kelola personil di Konsol Petugas.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        return@Button
                                    } else {
                                        isError = true
                                        errorMessage = "PIN Master tidak valid."
                                        return@Button
                                    }
                                } else {
                                    isError = true
                                    errorMessage = if (officerAuthManager.getOfficerRoster().isEmpty()) {
                                        "Masukkan PIN Master (117 / BPBD117 / ADMIN117) untuk masuk sebagai Administrator."
                                    } else {
                                        "Pilih petugas dari daftar terlebih dahulu atau masukkan PIN Master Admin."
                                    }
                                    return@Button
                                }
                            }

                            val nonNullOfficer = officer
                            if (inputPin.isBlank()) {
                                isError = true
                                errorMessage = "Masukkan PIN keamanan petugas dinas."
                                return@Button
                            }

                            val success = officerAuthManager.loginOfficer(
                                name = nonNullOfficer.fullName,
                                badgeId = nonNullOfficer.nip,
                                shift = nonNullOfficer.shift,
                                pin = inputPin,
                                role = nonNullOfficer.role
                            )

                            if (success) {
                                isOfficerLoggedIn = true
                                officerName = officerAuthManager.getOfficerName()
                                officerBadgeId = officerAuthManager.getOfficerBadgeId()
                                officerShift = officerAuthManager.getOfficerShift()
                                officerRole = officerAuthManager.getOfficerRole()
                                showOfficerLoginDialog = false
                                onOfficerLoginDismissed?.invoke()
                                selectedTab = 4 // Direct to Konsol Petugas

                                val targetId = pendingStatusReportId
                                val targetStatus = pendingStatusTarget
                                if (targetId != null && targetStatus != null) {
                                    onUpdateReportStatus(targetId, targetStatus)
                                    Toast.makeText(
                                        context,
                                        "Terverifikasi sebagai ${nonNullOfficer.formattedLabel()}. Status laporan diperbarui menjadi '$targetStatus'",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Selamat bertugas, ${nonNullOfficer.formattedLabel()}!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                pendingStatusReportId = null
                                pendingStatusTarget = null
                            } else {
                                isError = true
                                errorMessage = "PIN salah. Gunakan PIN resmi Pusdalops (117 / BPBD117 / TRC2026) atau PIN baru yang telah diubah."
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D))
                    ) {
                        Text("Verifikasi & Masuk")
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showOfficerLoginDialog = false
                        pendingStatusReportId = null
                        pendingStatusTarget = null
                    }
                ) {
                    Text("Tutup")
                }
            }
        )
    }

    // Enlarged Fullscreen Photo Dialog
    if (previewPhotoUri != null && previewPhotoReport != null) {
        val activeRep = previewPhotoReport!!
        Dialog(
            onDismissRequest = {
                previewPhotoUri = null
                previewPhotoReport = null
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.95f)),
                color = Color.Black.copy(alpha = 0.95f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Top Bar with Close Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Foto Kejadian Bencana Lapangan",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "ID: ${activeRep.id} • ${activeRep.village}, ${activeRep.district}",
                                color = Color(0xFFD1D5DB),
                                fontSize = 11.sp
                            )
                        }

                        IconButton(
                            onClick = {
                                previewPhotoUri = null
                                previewPhotoReport = null
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color.White.copy(alpha = 0.2f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Tutup",
                                tint = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Full Photo Container
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF111827)),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = previewPhotoUri,
                            contentDescription = "Foto Bencana Resolusi Penuh",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )

                        // GPS Satellite Geotag Watermark Overlay (Bottom)
                        Surface(
                            color = Color.Black.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
                            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = Color(0xFF22C55E),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "STEMPEL DIGITAL BPBD MUKOMUKO (KAMERA SATELLITE GPS)",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF86EFAC)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "📍 Koordinat: %.5f, %.5f (Akurasi: ${activeRep.gpsAccuracyMeters?.let { "%.1f m".format(it) } ?: "Terkunci Satelit"})".format(activeRep.latitude, activeRep.longitude),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFBBF24)
                                )
                                Text(
                                    text = "🕒 Waktu Kejadian: ${activeRep.incidentTime} • Pelapor: ${activeRep.reporterName}",
                                    fontSize = 10.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Bottom Navigation Button: Open route in Google Maps
                    Button(
                        onClick = {
                            GoogleMapsHelper.openNavigationRoute(
                                context = context,
                                destLat = activeRep.latitude,
                                destLon = activeRep.longitude,
                                destName = "${activeRep.disasterType} - ${activeRep.village}"
                            )
                        },
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Navigation,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Buka Rute Evakuasi di Google Maps",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * Reusable Dropdown Row matching the clean underlined layout of the screenshot
 */
@Composable
private fun ReportDropdownField(
    label: String,
    value: String,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color(0xFF6B7280)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleExpand() }
                .padding(vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = value,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF111827),
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = Color(0xFF9CA3AF),
                    modifier = Modifier.size(24.dp)
                )
            }

            DropdownMenu(
                expanded = isExpanded,
                onDismissRequest = onDismiss
            ) {
                content()
            }
        }
    }
}

/**
 * Casualty counter row with [-] and [+]
 */
@Composable
private fun CasualtyCounterRow(
    label: String,
    count: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color(0xFF374151),
            fontWeight = FontWeight.Medium
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = if (count > 0) Color(0xFFEF4444).copy(alpha = 0.15f) else Color(0xFFF3F4F6),
                modifier = Modifier
                    .size(32.dp)
                    .clickable(enabled = count > 0, onClick = onDecrement)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Kurang",
                        tint = if (count > 0) Color(0xFFDC2626) else Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Text(
                text = "$count Orang",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (count > 0) Color(0xFFDC2626) else Color(0xFF111827),
                modifier = Modifier.width(68.dp),
                textAlign = TextAlign.Center
            )

            Surface(
                shape = CircleShape,
                color = Color(0xFF15803D).copy(alpha = 0.15f),
                modifier = Modifier
                    .size(32.dp)
                    .clickable(onClick = onIncrement)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Tambah",
                        tint = Color(0xFF15803D),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Report Card Item inside the History Table view with interactive status buttons,
 * GPS Photo display with watermark, Google Maps navigation, and officer-gated actions.
 */
@Composable
private fun CitizenReportItemCard(
    report: CitizenDisasterReport,
    isOfficerLoggedIn: Boolean,
    onUpdateStatus: (String) -> Unit,
    onViewPhoto: (String) -> Unit
) {
    val context = LocalContext.current

    // Indicator Color based on Status
    val (statusContainer, statusContent, statusLabel) = when (report.status) {
        "Divalidasi" -> Triple(Color(0xFFDCFCE7), Color(0xFF15803D), "Divalidasi")
        "Selesai" -> Triple(Color(0xFFDBEAFE), Color(0xFF2563EB), "Selesai")
        else -> Triple(Color(0xFFFEF3C7), Color(0xFFD97706), "Diproses")
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: ID, Time & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = report.id,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6B7280)
                    )
                    Text(
                        text = report.incidentTime,
                        fontSize = 10.sp,
                        color = Color(0xFF9CA3AF)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(6.dp).background(statusContent, CircleShape))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = statusLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusContent
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Disaster Type
            Text(
                text = report.disasterType,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFEA580C)
            )

            // Location
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color(0xFF2563EB),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${report.village}, Kec. ${report.district}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF374151)
                )
            }

            Text(
                text = "Detail: ${report.incidentLocation}",
                fontSize = 11.sp,
                color = Color(0xFF6B7280),
                modifier = Modifier.padding(top = 2.dp)
            )

            // PHOTO DISPLAY: Captured Photo with BPBD GPS Watermark
            if (!report.photoUri.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD1D5DB)),
                    color = Color(0xFF1E293B),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clickable { onViewPhoto(report.photoUri) }
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = report.photoUri,
                            contentDescription = "Foto Bencana di Lokasi",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // GPS Satellite Watermark Banner (Bottom)
                        Surface(
                            color = Color.Black.copy(alpha = 0.78f),
                            shape = RoundedCornerShape(topStart = 8.dp),
                            modifier = Modifier.align(Alignment.BottomEnd)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                                Text(
                                    text = "📍 %.5f, %.5f".format(report.latitude, report.longitude),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFBBF24)
                                )
                                Text(
                                    text = "🛡️ FOTO ASLI BPBD MUKOMUKO",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF86EFAC)
                                )
                            }
                        }

                        // Zoom indicator badge (Top End)
                        Surface(
                            color = Color.Black.copy(alpha = 0.6f),
                            shape = CircleShape,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.ZoomIn,
                                    contentDescription = "Perbesar Foto",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
                Text(
                    text = "🔍 Ketuk foto untuk melihat dalam resolusi penuh & stempel digital",
                    fontSize = 9.sp,
                    color = Color(0xFF6B7280),
                    modifier = Modifier.padding(top = 3.dp)
                )
            }

            // GPS Coordinates Banner
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFFF3F4F6),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📍 Lat: %.5f, Lon: %.5f".format(report.latitude, report.longitude),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1F2937),
                        modifier = Modifier.weight(1f)
                    )
                    if (report.isGpsVerified) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF16A34A)
                        ) {
                            Text(
                                text = "GPS ASLI",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }

            // Casualties Breakdown
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CasualtyBadge(label = "Meninggal", count = report.casualtiesDead, color = Color(0xFFDC2626))
                CasualtyBadge(label = "Luka", count = report.casualtiesInjured, color = Color(0xFFD97706))
                CasualtyBadge(label = "Hilang", count = report.casualtiesMissing, color = Color(0xFF4B5563))
            }

            if (report.additionalNotes.isNotBlank()) {
                Text(
                    text = "Catatan: ${report.additionalNotes}",
                    fontSize = 11.sp,
                    color = Color(0xFF4B5563),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Text(
                text = "Dilaporkan oleh: ${report.reporterName}",
                fontSize = 10.sp,
                color = Color(0xFF9CA3AF),
                modifier = Modifier.padding(top = 4.dp)
            )

            // GOOGLE MAPS CONNECTION BUTTONS (Universal Route & Navigation)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = {
                        GoogleMapsHelper.openNavigationRoute(
                            context = context,
                            destLat = report.latitude,
                            destLon = report.longitude,
                            destName = "${report.disasterType} - ${report.village}"
                        )
                    },
                    modifier = Modifier.weight(1.3f).height(36.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Rute Google Maps",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                OutlinedButton(
                    onClick = {
                        GoogleMapsHelper.openLocation(
                            context = context,
                            latitude = report.latitude,
                            longitude = report.longitude,
                            label = "${report.disasterType} (${report.village})"
                        )
                    },
                    modifier = Modifier.weight(1f).height(36.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1D4ED8)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF93C5FD)),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Pin Maps",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            HorizontalDivider(
                color = Color(0xFFF3F4F6),
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Status Management Row with Buttons (Officer role-gated)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!isOfficerLoggedIn) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Terkunci",
                            tint = Color(0xFF9CA3AF),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                    }
                    Text(
                        text = if (isOfficerLoggedIn) "Update Status (Petugas):" else "Status (Khusus Petugas):",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isOfficerLoggedIn) Color(0xFF15803D) else Color(0xFF4B5563)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    ReportStatusChip(
                        label = "Diproses",
                        isSelected = report.status == "Diproses",
                        color = Color(0xFFEA580C)
                    ) {
                        onUpdateStatus("Diproses")
                    }
                    ReportStatusChip(
                        label = "Divalidasi",
                        isSelected = report.status == "Divalidasi",
                        color = Color(0xFF16A34A)
                    ) {
                        onUpdateStatus("Divalidasi")
                    }
                    ReportStatusChip(
                        label = "Selesai",
                        isSelected = report.status == "Selesai",
                        color = Color(0xFF2563EB)
                    ) {
                        onUpdateStatus("Selesai")
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportStatusChip(
    label: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (isSelected) color else Color(0xFFF3F4F6),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) color else Color(0xFFE5E7EB)
        ),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color.White else Color(0xFF6B7280),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun CasualtyBadge(label: String, count: Int, color: Color) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (count > 0) color.copy(alpha = 0.12f) else Color(0xFFF3F4F6)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$label: ",
                fontSize = 10.sp,
                color = Color(0xFF4B5563)
            )
            Text(
                text = "$count",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (count > 0) color else Color(0xFF6B7280)
            )
        }
    }
}
