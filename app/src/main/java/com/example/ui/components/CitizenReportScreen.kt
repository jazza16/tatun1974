package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.AuthenticationRequest
import androidx.biometric.AuthenticationResult
import androidx.biometric.compose.rememberAuthenticationLauncher
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Fingerprint
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.security.PlayIntegritySecurityManager
import com.example.security.SecurityManager
import com.example.security.SecurityAccessResult
import kotlinx.coroutines.launch
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.local.AppDatabase
import com.example.data.local.OfficialAsnBpbdPersonnel
import com.example.data.local.OfficerAuthManager
import com.example.data.local.OfficerProfile
import com.example.data.local.OfficerRoles
import com.example.data.local.OfficerCredentialEntity
import com.example.data.local.FirestoreHelper
import com.example.data.repository.PersonnelRepository
import com.example.data.repository.SmartOfficerAuthRepository
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
 * Memasking 6 digit terakhir NIP untuk menjaga privasi pada tampilan UI Login Petugas.
 * Menampilkan 12 digit pertama dan mengganti 6 digit terakhir dengan "******".
 * Contoh: "197401282006041006" -> "197401282006******"
 */
fun maskNipLast6(nip: String): String {
    val trimmed = nip.trim()
    if (trimmed.isEmpty()) return "-"
    return when {
        trimmed.length >= 12 -> trimmed.take(12) + "******"
        trimmed.length > 6 -> trimmed.dropLast(6) + "******"
        else -> "******"
    }
}

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
    hasSpecialAssignment: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by rememberSaveable { mutableIntStateOf(0) } // 0 = Formulir, 1 = Peta Sebaran, 2 = Tabel Riwayat, 3 = Kontak Penting, 4 = Konsol Petugas

    // Officer & Firebase Authentication Manager (SIPAGAR KITA)
    val authManager = remember { com.example.data.auth.AuthManager.getInstance(context) }
    val officerAuthManager = remember { OfficerAuthManager(context) }
    var isOfficerLoggedIn by rememberSaveable { mutableStateOf(authManager.isSessionActive() || officerAuthManager.isOfficerLoggedIn()) }
    var officerName by rememberSaveable { mutableStateOf(officerAuthManager.getOfficerName().ifBlank { authManager.getDisplayName() }) }
    var officerBadgeId by rememberSaveable { mutableStateOf(officerAuthManager.getOfficerBadgeId().ifBlank { authManager.getBadgeOrUid() }) }
    var officerShift by rememberSaveable { mutableStateOf(officerAuthManager.getOfficerShift()) }
    var officerRole by rememberSaveable { mutableStateOf(officerAuthManager.getOfficerRole()) }

    val coroutineScope = rememberCoroutineScope()
    val database = remember { AppDatabase.getInstance(context) }
    val personnelRepository = remember { PersonnelRepository(database.personnelDao()) }

    // Enterprise Play Integrity SecurityManager
    val securityManager = remember { SecurityManager.getInstance(context) }
    var securityAccessResult by remember { mutableStateOf<SecurityAccessResult?>(null) }
    var isVerifyingSecurity by remember { mutableStateOf(false) }

    LaunchedEffect(selectedTab, isOfficerLoggedIn, officerBadgeId) {
        if (selectedTab == 4 && (authManager.isSessionActive() || officerAuthManager.isOfficerLoggedIn())) {
            val targetNip = officerBadgeId.ifBlank { authManager.getBadgeOrUid() }
            if (targetNip.isNotBlank()) {
                isVerifyingSecurity = true
                val result = securityManager.verifySessionBeforeGrantingRuangKerja(targetNip)
                securityAccessResult = result
                isVerifyingSecurity = false
            }
        }
    }

    LaunchedEffect(Unit) {
        personnelRepository.seedDefaultAsnPersonnelIfNeeded()
    }

    // Officer Login & Status Gating Modal State
    var showOfficerLoginDialog by remember { mutableStateOf(initialShowOfficerLogin) }
    var showDisasterWallpaperDialog by remember { mutableStateOf(false) }
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

    // Trigger auto-sync on screen open and sub-tab / officer login state changes
    LaunchedEffect(selectedTab, isOfficerLoggedIn) {
        onSyncReports?.invoke { count ->
            if (count > 0) {
                lastSyncStatusMessage = "$count laporan baru disinkronkan"
            }
        }
    }

    // Form inputs state
    var selectedDisasterType by rememberSaveable { mutableStateOf(DISASTER_TYPES[0]) }
    var isDisasterDropdownExpanded by remember { mutableStateOf(false) }

    val initialTime = remember {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm 'WIB'", Locale.forLanguageTag("id-ID"))
        sdf.format(Date())
    }
    var incidentTime by rememberSaveable { mutableStateOf(initialTime) }
    var incidentLocation by rememberSaveable { mutableStateOf("") }

    var selectedDistrict by rememberSaveable { mutableStateOf(currentDistrict.name) }
    var isDistrictDropdownExpanded by remember { mutableStateOf(false) }

    var selectedVillage by rememberSaveable {
        mutableStateOf(VILLAGES_MAP[currentDistrict.name]?.firstOrNull() ?: "Desa Pasar")
    }
    var isVillageDropdownExpanded by remember { mutableStateOf(false) }

    // Coordinates & GPS Verification
    var latitude by rememberSaveable { mutableDoubleStateOf(currentDistrict.latitude) }
    var longitude by rememberSaveable { mutableDoubleStateOf(currentDistrict.longitude) }
    var gpsAccuracy by rememberSaveable { mutableStateOf<Float?>(null) }
    var isGpsLocked by rememberSaveable { mutableStateOf(false) }
    var isFetchingGps by remember { mutableStateOf(false) }
    var gpsStatusMessage by rememberSaveable { mutableStateOf("Sentuh pin GPS untuk mengunci koordinat ponsel") }

    // Casualties data
    var deadCount by rememberSaveable { mutableIntStateOf(0) }
    var injuredCount by rememberSaveable { mutableIntStateOf(0) }
    var missingCount by rememberSaveable { mutableIntStateOf(0) }

    // Photo & Geotag Verification (Anti-Hoax: Live camera only, gallery upload disabled)
    var photoUriString by rememberSaveable { mutableStateOf<String?>(null) }
    var cameraCapturedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(photoUriString) {
        if (cameraCapturedBitmap == null && !photoUriString.isNullOrBlank()) {
            runCatching {
                val parsedUri = Uri.parse(photoUriString)
                context.contentResolver.openInputStream(parsedUri)?.use { input ->
                    cameraCapturedBitmap = BitmapFactory.decodeStream(input)
                }
            }
        }
    }

    var reporterName by rememberSaveable { mutableStateOf("") }
    var reporterPhone by rememberSaveable { mutableStateOf("") }
    var additionalNotes by rememberSaveable { mutableStateOf("") }

    // Dialog state
    var showSuccessDialog by rememberSaveable { mutableStateOf(false) }
    var submittedReportId by rememberSaveable { mutableStateOf("") }

    // Table Filter by Status: "Semua", "Diproses", "Divalidasi", "Selesai"
    var tableStatusFilter by rememberSaveable { mutableStateOf("Semua") }

    // Permission Rationale / Guidance Dialog state
    var permissionDeniedDialogMessage by remember { mutableStateOf<String?>(null) }
    var permissionDeniedDialogTitle by remember { mutableStateOf<String?>(null) }

    // Camera launcher (TakePicturePreview returns Bitmap thumbnail)
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            val optimizedBitmap = com.example.util.ImageUtils.optimizeReportBitmap(bitmap)
            cameraCapturedBitmap = optimizedBitmap
            photoUriString = null
            // Save optimized bitmap to cache directory for persistent URI
            try {
                val file = File(context.cacheDir, "report_photo_${System.currentTimeMillis()}.jpg")
                FileOutputStream(file).use { out ->
                    optimizedBitmap.compress(Bitmap.CompressFormat.JPEG, com.example.util.ImageUtils.OPTIMAL_JPEG_QUALITY, out)
                }
                photoUriString = Uri.fromFile(file).toString()
            } catch (_: Exception) {
                // Keep bitmap in memory
            }
            Toast.makeText(context, "Foto resolusi optimal tersimpan siap verifikasi Pusdalops!", Toast.LENGTH_SHORT).show()
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
        // 1. Top Bar Header (Signature Mukomuko Green, clean)
        Surface(
            color = Color(0xFF15803D),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("citizen_report_header")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Laporan Warga",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = { showDisasterWallpaperDialog = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info Lanskap & Wallpaper HD",
                        tint = Color.White
                    )
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
                                    fontSize = 11.sp,
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

                // Tab 4: Modul Khusus Petugas (Ruang Kerja Pusdalops & TRC)
                Tab(
                    selected = selectedTab == 4,
                    onClick = {
                        if (authManager.isSessionActive() || officerAuthManager.isOfficerLoggedIn()) {
                            isOfficerLoggedIn = true
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
                                text = "Ruang Kerja",
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
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOfficerLoggedIn) Color.Black else Color.White,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                )

                // Tab 5: Statistik & Tren Bulanan (Grafik Batang & Pie Chart)
                Tab(
                    selected = selectedTab == 5,
                    onClick = { selectedTab = 5 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Analytics,
                                contentDescription = null,
                                tint = if (selectedTab == 5) Color(0xFFFBBF24) else Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Tren Bulanan",
                                fontWeight = if (selectedTab == 5) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp,
                                color = if (selectedTab == 5) Color.White else Color.White.copy(alpha = 0.7f)
                            )
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
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isWideScreen = maxWidth >= 600.dp
                Box(
                    modifier = if (isWideScreen) {
                        Modifier
                            .fillMaxSize()
                            .widthIn(max = 840.dp)
                            .align(Alignment.TopCenter)
                    } else {
                        Modifier.fillMaxSize()
                    }
                ) {
                    when (selectedTab) {
                0 -> {
                    // TAB 0: FORMULIR INPUT (Matches Screenshot)
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .imePadding()
                            .padding(horizontal = 20.dp),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        if (isOfficerLoggedIn) {
                            // Mode Petugas Aktif: Sembunyikan form laporan warga, tampilkan Kartu Pengarah Besar ke Form Laporan Harian Pusdalops
                            item {
                                Card(
                                    onClick = {
                                        val intent = Intent(context, com.example.FormLaporanActivity::class.java)
                                        context.startActivity(intent)
                                    },
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                                    shape = RoundedCornerShape(18.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF93C5FD)),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("cardBuatLaporan")
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(20.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = Color(0xFF1976D2),
                                            modifier = Modifier.size(56.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.AutoMirrored.Filled.Assignment,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(30.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(14.dp))
                                        Surface(
                                            color = Color(0xFFDBEAFE),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "MODE PETUGAS PUSDALOPS AKTIF",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFF1E40AF),
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = "Form Laporan Harian Pusdalops",
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF1E3A8A),
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Saat sesi Petugas aktif ($officerName), formulir aduan warga dinonaktifkan agar data pelaporan masyarakat dan laporan resmi petugas tetap terpisah.",
                                            fontSize = 12.sp,
                                            color = Color(0xFF334155),
                                            textAlign = TextAlign.Center,
                                            lineHeight = 18.sp
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(
                                            onClick = {
                                                val intent = Intent(context, com.example.FormLaporanActivity::class.java)
                                                context.startActivity(intent)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(min = 48.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.Assignment,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "BUKA FORM LAPORAN HARIAN (MULTI-TAB)",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedButton(
                                            onClick = { selectedTab = 4 },
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(min = 44.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Shield,
                                                contentDescription = null,
                                                tint = Color(0xFF15803D),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Ke Ruang Kerja & Konsol Petugas",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF15803D)
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
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
                                            val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm 'WIB'", Locale.forLanguageTag("id-ID"))
                                            incidentTime = sdf.format(Date())
                                            Toast.makeText(context, "Waktu disinkronkan ke jam sekarang", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Text(
                                            text = "Set Sekarang",
                                            fontSize = 11.sp,
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
                                    placeholder = { Text("Nama jalan, dusun, atau patokan lokasi...", fontSize = 13.sp, color = Color(0xFF6B7280)) },
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
                                                fontSize = 11.sp,
                                                color = Color(0xFF16A34A),
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = gpsStatusMessage,
                                            fontSize = 11.sp,
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
                                        val previewModel = remember(cameraCapturedBitmap, photoUriString) {
                                            cameraCapturedBitmap ?: decodeCitizenPhotoModel(photoUriString)
                                        }
                                        val fallbackPainter = rememberVectorPainter(Icons.Default.BrokenImage)
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(180.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .border(1.dp, Color(0xFFD1D5DB), RoundedCornerShape(12.dp))
                                                .clickable {
                                                    val targetUri = photoUriString
                                                    if (!targetUri.isNullOrBlank()) {
                                                        previewPhotoUri = targetUri
                                                        previewPhotoReport = CitizenDisasterReport(
                                                            id = "PRATINJAU",
                                                            disasterType = selectedDisasterType,
                                                            incidentTime = incidentTime,
                                                            incidentLocation = incidentLocation.ifBlank { selectedVillage },
                                                            district = selectedDistrict,
                                                            village = selectedVillage,
                                                            latitude = latitude,
                                                            longitude = longitude,
                                                            gpsAccuracyMeters = gpsAccuracy,
                                                            isGpsVerified = isGpsLocked,
                                                            photoUri = targetUri,
                                                            reporterName = reporterName.ifBlank { "Pelapor Warga" },
                                                            reporterPhone = reporterPhone
                                                        )
                                                    }
                                                }
                                        ) {
                                            AsyncImage(
                                                model = previewModel,
                                                contentDescription = "Foto Bukti Kejadian",
                                                placeholder = fallbackPainter,
                                                error = fallbackPainter,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )

                                            // Digital Geotag Watermark Overlay
                                            Surface(
                                                color = Color.Black.copy(alpha = 0.75f),
                                                shape = RoundedCornerShape(topStart = 8.dp),
                                                modifier = Modifier.align(Alignment.BottomEnd)
                                            ) {
                                                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                                                    Text(
                                                        text = "📍 %.5f, %.5f".format(latitude, longitude),
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFFFBBF24)
                                                    )
                                                    Text(
                                                        text = "🕒 $incidentTime",
                                                        fontSize = 11.sp,
                                                        color = Color.White
                                                    )
                                                    Text(
                                                        text = "🛡️ KAMERA & GPS TERVERIFIKASI BPBD",
                                                        fontSize = 10.sp,
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
                                                fontSize = 11.sp,
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
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                softWrap = false
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = "Wajib diisi: Jelaskan kronologi kejadian, dampak kerusakan, kebutuhan mendesak, atau rincian jika jenis bencana tidak tercantum pada pilihan di atas.",
                                        fontSize = 11.sp,
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
                                                color = Color(0xFF6B7280)
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
                                    .heightIn(min = 50.dp)
                                    .testTag("btn_submit_citizen_report")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
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
                        }
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
                                Column(modifier = Modifier.padding(12.dp)) {
                                    // Dua Tampilan Berbeda: Sesi Petugas Resmi vs Mode Warga/Tamu
                                    if (isOfficerLoggedIn) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Badge,
                                                contentDescription = null,
                                                tint = Color(0xFF15803D),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Petugas Jaga Aktif: $officerName",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF14532D),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    } else {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = null,
                                                tint = Color(0xFF2563EB),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Mode Warga / Tamu",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1E40AF),
                                                maxLines = 1
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Tombol Konsol Petugas dan Tombol Keluar / PIN
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
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
                                            modifier = Modifier
                                                .weight(1f)
                                                .heightIn(min = 36.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isOfficerLoggedIn) Icons.Default.Shield else Icons.Default.Lock,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Konsol Petugas",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                maxLines = 1
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
                                            modifier = Modifier
                                                .weight(1f)
                                                .heightIn(min = 36.dp)
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
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                maxLines = 1
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Tulisan Akses Respon : Khusus Petugas BPBD berada di bawah tombol
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Shield,
                                            contentDescription = null,
                                            tint = Color(0xFF64748B),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Akses Respon : Khusus Petugas BPBD",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF64748B),
                                            textAlign = TextAlign.Center
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
                                        fontSize = 11.sp,
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
                                                    fontSize = 11.sp,
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
                                            if (newStatus.equals("Divalidasi", ignoreCase = true)) {
                                                val isOperator = officerRole.contains("Operator", ignoreCase = true)
                                                if (!isOperator) {
                                                    Toast.makeText(
                                                        context,
                                                        "Akses Ditolak: Tombol Validasi Laporan hanya dapat dieksekusi oleh Operator Pusdalops sebagai wujud tanggung jawab petugas.",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                } else {
                                                    onUpdateReportStatus(report.id, newStatus)
                                                    Toast.makeText(
                                                        context,
                                                        "Status laporan #${report.id.takeLast(7)} diubah menjadi '$newStatus' oleh Petugas $officerName",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            } else if (newStatus.equals("Selesai", ignoreCase = true)) {
                                                val isAuthorizedLeader = officerRole.contains("Manajer", ignoreCase = true) ||
                                                        officerRole.contains("Administrasi", ignoreCase = true)
                                                if (!isAuthorizedLeader) {
                                                    Toast.makeText(
                                                        context,
                                                        "Akses Ditolak: Tombol Selesai hanya dapat dieksekusi oleh Manajer Pusdalops atau Koord. Administrasi sebagai wujud tanggung jawab petugas.",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                } else {
                                                    onUpdateReportStatus(report.id, newStatus)
                                                    Toast.makeText(
                                                        context,
                                                        "Status laporan #${report.id.takeLast(7)} diubah menjadi '$newStatus' oleh Petugas $officerName",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            } else {
                                                onUpdateReportStatus(report.id, newStatus)
                                                Toast.makeText(
                                                    context,
                                                    "Status laporan #${report.id.takeLast(7)} diubah menjadi '$newStatus' oleh Petugas $officerName",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
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
                    }
                }

                3 -> {
                    // TAB 3: KONTAK PENTING DARURAT MUKOMUKO
                    CitizenEmergencyContactsView()
                }

                4 -> {
                    // TAB 4: RUANG KERJA PETUGAS PUSDALOPS BPBD & TIM TRC
                    val hasActiveSession = authManager.isSessionActive() || officerAuthManager.isOfficerLoggedIn()
                    if (hasActiveSession) {
                        if (isVerifyingSecurity) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(color = Color(0xFF15803D), modifier = Modifier.size(44.dp))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Memverifikasi Keamanan Perangkat & Sesi",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color(0xFF0F172A)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Memeriksa token Play Integrity Classic dan otorisasi Cloud Firestore backend...",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B),
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else if (securityAccessResult != null && !securityAccessResult!!.isAuthorized) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
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
                                            imageVector = Icons.Default.Shield,
                                            contentDescription = "Akses Dibatasi",
                                            tint = Color(0xFFDC2626),
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "Otorisasi Ruang Kerja Ditolak",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color(0xFF991B1B)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = securityAccessResult?.message ?: "Perangkat atau sesi tidak memenuhi standar Play Integrity Classic.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF475569),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                val targetNip = officerBadgeId.ifBlank { authManager.getBadgeOrUid() }
                                                isVerifyingSecurity = true
                                                securityAccessResult = securityManager.verifySessionBeforeGrantingRuangKerja(targetNip)
                                                isVerifyingSecurity = false
                                            }
                                        }
                                    ) {
                                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Cek Ulang")
                                    }
                                    Button(
                                        onClick = {
                                            showOfficerLoginDialog = true
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D))
                                    ) {
                                        Icon(imageVector = Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Login Ulang")
                                    }
                                }
                            }
                        } else {
                            OfficerDashboardConsoleView(
                                reports = reports,
                                currentDistrict = currentDistrict,
                                isOfficerLoggedIn = true,
                                officerName = officerName.ifBlank { authManager.getDisplayName() },
                                officerBadgeId = officerBadgeId.ifBlank { authManager.getBadgeOrUid() },
                                officerShift = officerShift,
                                officerRole = officerRole,
                                onLoginClick = { showOfficerLoginDialog = true },
                                onLogoutClick = {
                                    authManager.signOut()
                                    officerAuthManager.logoutOfficer()
                                    isOfficerLoggedIn = false
                                    officerName = ""
                                    officerBadgeId = ""
                                    securityAccessResult = null
                                    selectedTab = 0
                                    Toast.makeText(context, "Sesi Petugas Ruang Kerja diakhiri", Toast.LENGTH_SHORT).show()
                                },
                                onUpdateReportStatus = onUpdateReportStatus,
                                onUpdateReportStatusAndNotes = onUpdateReportStatusAndNotes,
                                onViewPhoto = { photoUri, rep ->
                                    previewPhotoUri = photoUri
                                    previewPhotoReport = rep
                                },
                                onSyncReports = onSyncReports
                            )
                        }
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
                                text = "Ruang Kerja Petugas Pusdalops",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Konsol validasi status dan penanganan bencana hanya dapat diakses oleh petugas resmi yang terautentikasi.",
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
                                Text("Masuk Ruang Kerja")
                            }
                        }
                    }
                }
                5 -> {
                    // TAB 5: TREN & STATISTIK BENCANA BULANAN (GRAFIK BATANG & PIE CHART)
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            MonthlyDisasterAnalyticsSection(
                                reports = reports,
                                currentDistrict = currentDistrict,
                                isDark = false
                            )
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
        val smartAuthRepo = remember { SmartOfficerAuthRepository.getInstance(context) }
        var roster by remember { mutableStateOf(officerAuthManager.getOfficerRoster()) }
        var selectedRoleFilter by remember { mutableStateOf(OfficerRoles.OPERATOR) }
        var selectedOfficer by remember {
            mutableStateOf(
                roster.firstOrNull { it.nip == officerBadgeId }
                    ?: roster.firstOrNull { it.role == OfficerRoles.OPERATOR }
                    ?: roster.firstOrNull()
            )
        }
        var selectedCredential by remember { mutableStateOf<OfficerCredentialEntity?>(null) }
        var isHarvestingSync by remember { mutableStateOf(false) }
        var dialogMode by remember { mutableIntStateOf(0) } // 0 = Pilih dari Roster / Dropdown, 1 = Form Input Petugas Baru (Admin Only)
        var isNameDropdownOpen by remember { mutableStateOf(false) }
        var rosterSearchQuery by remember { mutableStateOf("") }

        // PIN & Biometric state
        var inputPin by remember { mutableStateOf("") }
        var isPinVisible by remember { mutableStateOf(false) }
        var isError by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf("") }
        var authResultText by rememberSaveable { mutableStateOf("") }
        var isBiometricScanning by rememberSaveable { mutableStateOf(false) }
        val hapticFeedback = LocalHapticFeedback.current

        val authLauncher = rememberAuthenticationLauncher { authResult ->
            when (authResult) {
                is AuthenticationResult.Success -> {
                    // Autentikasi biometrik BERHASIL
                    triggerBiometricVibration(context, "SUCCESS")
                    authResultText = "Login Biometrik Berhasil! Memverifikasi sesi..."
                    val savedNip = officerAuthManager.getOfficerBadgeId()
                        .ifBlank { selectedOfficer?.nip.orEmpty() }

                    if (savedNip.isBlank()) {
                        isBiometricScanning = false
                        isError = true
                        errorMessage = "NIP petugas belum tersimpan di sesi perangkat. Silakan pilih petugas atau login dengan PIN terlebih dahulu."
                        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                    } else {
                        coroutineScope.launch {
                            isHarvestingSync = true
                            val bioResult = smartAuthRepo.authenticateWithBiometric(savedNip)
                            isHarvestingSync = false
                            isBiometricScanning = false

                            if (bioResult.isSuccess) {
                                val cred = bioResult.getOrThrow()
                                selectedCredential = cred
                                val nipPrefix = if (cred.nip.startsWith("TRC", ignoreCase = true)) "Reg TRC" else "NIP"
                                val formattedLabel = "${cred.fullName} ($nipPrefix: ${maskNipLast6(cred.nip)})"

                                val sessionRes = securityManager.verifyDeviceAuthenticityDuringLogin(
                                    officerNip = cred.nip,
                                    officerName = cred.fullName,
                                    officerRole = cred.role
                                )
                                val verifiedSession = sessionRes.getOrNull()
                                val isDeviceAuthorized = sessionRes.isSuccess && (verifiedSession?.accessGranted == true)
                                if (!isDeviceAuthorized) {
                                    val denyReason = sessionRes.exceptionOrNull()?.message
                                        ?: "Integritas perangkat tidak memenuhi syarat keamanan (${verifiedSession?.verdict ?: "UNVERIFIED"}) untuk mengakses Ruang Kerja."
                                    securityAccessResult = SecurityAccessResult(
                                        isAuthorized = false,
                                        message = denyReason,
                                        session = verifiedSession
                                    )
                                    officerAuthManager.logoutOfficer()
                                    isOfficerLoggedIn = false
                                    isError = true
                                    errorMessage = denyReason
                                    Toast.makeText(context, denyReason, Toast.LENGTH_LONG).show()
                                    return@launch
                                }
                                securityAccessResult = SecurityAccessResult(
                                    isAuthorized = true,
                                    message = "Perangkat terverifikasi valid.",
                                    session = verifiedSession
                                )
                                isOfficerLoggedIn = true
                                officerName = cred.fullName
                                officerBadgeId = cred.nip
                                officerShift = cred.shift
                                officerRole = cred.role
                                showOfficerLoginDialog = false
                                onOfficerLoginDismissed?.invoke()
                                selectedTab = 4 // Direct to Konsol Petugas

                                val targetId = pendingStatusReportId
                                val targetStatus = pendingStatusTarget
                                if (targetId != null && targetStatus != null) {
                                    onUpdateReportStatus(targetId, targetStatus)
                                    Toast.makeText(
                                        context,
                                        "Terverifikasi sebagai $formattedLabel. Status laporan diperbarui menjadi '$targetStatus'",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Selamat bertugas, $formattedLabel! (Biometrik Terverifikasi)",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                pendingStatusReportId = null
                                pendingStatusTarget = null
                            } else {
                                triggerBiometricVibration(context, "ERROR")
                                isError = true
                                val errMsg = bioResult.exceptionOrNull()?.message
                                    ?: "Petugas tidak ditemukan"
                                errorMessage = errMsg
                                authResultText = "Error: $errMsg"
                                Toast.makeText(context, errMsg, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
                is AuthenticationResult.Error -> {
                    // Autentikasi GAGAL (misalnya, pengguna membatalkan, sensor error)
                    isBiometricScanning = false
                    triggerBiometricVibration(context, "ERROR")
                    authResultText = "Error: ${authResult.errString}"
                    isError = true
                    errorMessage = "Biometrik gagal: ${authResult.errString}. Silakan gunakan PIN pribadi."
                }
                is AuthenticationResult.CustomFallbackSelected -> {
                    // Pengguna memilih opsi fallback (misalnya, "Gunakan PIN")
                    isBiometricScanning = false
                    triggerBiometricVibration(context, "SCAN_START")
                    authResultText = "Fallback dipilih: ${authResult.fallback.text}"
                    isError = false
                }
            }
        }

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

        // Automatically harvest un-synced official officers and pull credentials from cloud
        LaunchedEffect(Unit) {
            isHarvestingSync = true
            try {
                smartAuthRepo.harvestAndSyncAllOfficers()
                smartAuthRepo.pullCredentialsFromCloud { _, _ -> }
                roster = officerAuthManager.getOfficerRoster()
                val nip = selectedOfficer?.nip ?: officerBadgeId
                if (!nip.isNullOrBlank()) {
                    selectedCredential = smartAuthRepo.getCredential(nip)
                }
            } catch (_: Exception) {
            } finally {
                isHarvestingSync = false
            }
        }

        LaunchedEffect(selectedOfficer) {
            val nip = selectedOfficer?.nip
            if (!nip.isNullOrBlank()) {
                selectedCredential = smartAuthRepo.getCredential(nip)
            }
        }

        val filteredRoster = roster.filter { off ->
            val matchesRole = if (selectedRoleFilter == "Semua") true else off.role.contains(selectedRoleFilter, ignoreCase = true)
            val matchesSearch = if (rosterSearchQuery.isBlank()) true else {
                val q = rosterSearchQuery.trim().lowercase()
                off.fullName.lowercase().contains(q) || off.nip.lowercase().contains(q) || off.unit.lowercase().contains(q)
            }
            matchesRole && matchesSearch
        }.sortedWith(
            compareBy<OfficerProfile> { OfficerRoles.getRoleRank(it.role) }
                .thenBy { it.fullName }
        )

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
                            fontSize = 11.sp,
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
                                "Semua" to "Semua (${roster.size})",
                                "Pengawas" to "🏛️ Pengawas (${roster.count { it.role.contains("Pengawas", ignoreCase = true) }})",
                                "Manajer" to "⭐ Manajer Pusdalops (${roster.count { it.role.contains("Manajer", ignoreCase = true) }})",
                                "Supervisor" to "🛡️ Supervisor (${roster.count { it.role.contains("Supervisor", ignoreCase = true) }})",
                                OfficerRoles.OPERATOR to "🎧 Operator (${roster.count { it.role.contains("Operator", ignoreCase = true) }})",
                                OfficerRoles.PETUGAS_JAGA to "🛡️ Petugas Jaga (${roster.count { it.role.contains("Piket", ignoreCase = true) || it.role.contains("Jaga", ignoreCase = true) }})",
                                OfficerRoles.PETUGAS_LAPANGAN to "🚜 TRC (${roster.count { it.role.contains("Lapangan", ignoreCase = true) || it.role.contains("Logistik", ignoreCase = true) || it.role.contains("TRC", ignoreCase = true) }})"
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
                                        fontSize = 11.sp,
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
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E40AF),
                            letterSpacing = 0.5.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFEFF6FF),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF2563EB)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isNameDropdownOpen = !isNameDropdownOpen }
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
                                            text = "NIP: ${selectedOfficer?.nip?.let { maskNipLast6(it) } ?: "-"} • ${selectedOfficer?.role ?: "-"}",
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
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

                            AnimatedVisibility(visible = isNameDropdownOpen) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    shadowElevation = 4.dp,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 220.dp)
                                            .verticalScroll(rememberScrollState())
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
                                                            text = "NIP: ${maskNipLast6(off.nip)} • ${off.role}",
                                                            fontSize = 11.sp,
                                                            fontFamily = FontFamily.Monospace,
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
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // KREDENSIAL TERPISAH: KOLOM NAMA & KOLOM NIP (TERKUNCI / ANTI MANUAL INPUT)
                        Text(
                            text = "KREDENSIAL RESMI DARI DATABASE BPBD (TERPISAH & TERPROTEKSI):",
                            fontSize = 11.sp,
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
                            value = selectedOfficer?.nip?.let { maskNipLast6(it) } ?: "",
                            onValueChange = {}, // Read-only: mencegah input manual pihak tak berwenang
                            readOnly = true,
                            enabled = false,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp
                            ),
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
                                        fontSize = 11.sp,
                                        color = Color(0xFF15803D)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFFDCFCE7)
                                ) {
                                    Text(
                                        text = "TERVERIFIKASI",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF166534),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        softWrap = false
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Smart Database Credentials & Multi-Device Sync Card
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedCredential?.isCustomPinSet == true) Color(0xFFF0FDF4) else Color(0xFFFFFBEB),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (selectedCredential?.isCustomPinSet == true) Color(0xFF86EFAC) else Color(0xFFFDE68A)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (selectedCredential?.isCustomPinSet == true) Icons.Default.CheckCircle else Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = if (selectedCredential?.isCustomPinSet == true) Color(0xFF15803D) else Color(0xFFB45309),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (selectedCredential?.isCustomPinSet == true) "Kredensial Aktif: PIN Pribadi Terdaftar" else "Aktivasi Akun Baru: Kunci Individual NIP",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedCredential?.isCustomPinSet == true) Color(0xFF166534) else Color(0xFF92400E)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (selectedCredential?.isCustomPinSet == true) {
                                        "Kredensial tersimpan di SQLite & Cloud Firestore. Wajib login dengan PIN pribadi Anda pada perangkat ini maupun perangkat baru. PIN default '117' dinonaktifkan."
                                    } else {
                                        "Akun personil belum mengganti PIN pribadi. Untuk aktivasi perdana, gunakan 6 digit terakhir NIP Anda secara pribadi. Akses melalui PIN default '117' dilarang."
                                    },
                                    fontSize = 11.sp,
                                    color = if (selectedCredential?.isCustomPinSet == true) Color(0xFF166534) else Color(0xFF78350F),
                                    lineHeight = 13.sp
                                )
                                if (selectedCredential?.lastLoginDevice?.isNotBlank() == true) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "📱 Perangkat Terakhir: ${selectedCredential?.lastLoginDevice}",
                                        fontSize = 10.sp,
                                        color = Color(0xFF475569)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Google Account Sign-In Status (Zero-Trust Cloud Firestore Gate)
                        val authMgr = remember { com.example.data.auth.AuthManager.getInstance(context) }
                        val googleUserEmail by authMgr.currentUserEmail.collectAsStateWithLifecycle()
                        val isFirebaseSignedIn = googleUserEmail != null || authMgr.getCurrentUser() != null
                        var isGoogleSigningIn by remember { mutableStateOf(false) }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isFirebaseSignedIn) Color(0xFFEFF6FF) else Color(0xFFF8FAFC),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isFirebaseSignedIn) Color(0xFF93C5FD) else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isFirebaseSignedIn) "Akun Cloud Google Aktif" else "Sinkronisasi Cloud Membutuhkan Google",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isFirebaseSignedIn) Color(0xFF1D4ED8) else Color(0xFF334155)
                                    )
                                    Text(
                                        text = googleUserEmail ?: "Masuk Google untuk sinkronisasi Firestore & backup lintas perangkat.",
                                        fontSize = 10.sp,
                                        color = Color(0xFF475569),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                OutlinedButton(
                                    onClick = {
                                        if (isGoogleSigningIn) return@OutlinedButton
                                        if (isFirebaseSignedIn) {
                                            authMgr.signOut()
                                            Toast.makeText(context, "Sesi Google Cloud ditutup", Toast.LENGTH_SHORT).show()
                                        } else {
                                            coroutineScope.launch {
                                                isGoogleSigningIn = true
                                                val signInRes = authMgr.signInWithGoogle(context)
                                                isGoogleSigningIn = false
                                                signInRes.onSuccess {
                                                    Toast.makeText(context, "Akun Google terhubung untuk Cloud Firestore", Toast.LENGTH_SHORT).show()
                                                }.onFailure { err ->
                                                    Toast.makeText(context, err.message ?: "Gagal masuk Google", Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.heightIn(min = 32.dp)
                                ) {
                                    Text(
                                        text = when {
                                            isGoogleSigningIn -> "Memuat..."
                                            isFirebaseSignedIn -> "Keluar"
                                            else -> "Masuk Google"
                                        },
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF1D4ED8)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Multi-device Cloud Sync & Admin Management Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch {
                                        isHarvestingSync = true
                                        try {
                                            smartAuthRepo.harvestAndSyncAllOfficers()
                                            smartAuthRepo.pullCredentialsFromCloud { success, count ->
                                                roster = officerAuthManager.getOfficerRoster()
                                                val nip = selectedOfficer?.nip
                                                if (!nip.isNullOrBlank()) {
                                                    coroutineScope.launch {
                                                        selectedCredential = smartAuthRepo.getCredential(nip)
                                                    }
                                                }
                                                isHarvestingSync = false
                                                Toast.makeText(
                                                    context,
                                                    if (success) "Sinkronisasi Cloud Berhasil: $count kredensial multi-perangkat diperbarui." else "Kredensial database lokal sudah mutakhir.",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        } catch (e: Exception) {
                                            isHarvestingSync = false
                                            Toast.makeText(context, "Sinkronisasi cloud: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.heightIn(min = 36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudSync,
                                    contentDescription = null,
                                    tint = Color(0xFF2563EB),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isHarvestingSync) "Menyinkronkan..." else "Sinkronkan Cloud",
                                    fontSize = 11.sp,
                                    color = Color(0xFF2563EB)
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    showAdminPromptForAdd = true
                                    adminPinAttempt = ""
                                    adminPinErrorMsg = ""
                                },
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.heightIn(min = 36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Color(0xFF0F766E),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Kelola Personil", fontSize = 11.sp, color = Color(0xFF0F766E))
                            }
                        }

                        if (showAdminPromptForAdd) {
                            AdminAuthDialog(
                                actionTitle = "Kelola Personil Pusdalops BPBD",
                                securityManager = remember { com.example.security.SecurityManager.getInstance(context) },
                                onDismiss = { showAdminPromptForAdd = false },
                                onAuthorized = {
                                    showAdminPromptForAdd = false
                                    dialogMode = 1
                                    formValidationError = ""
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Smart PIN Input Field (Dedicated Personal PIN, No Default PIN Allowed)
                        OutlinedTextField(
                            value = inputPin,
                            onValueChange = {
                                inputPin = it
                                isError = false
                            },
                            label = { Text("PIN Pribadi Petugas Pusdalops", fontSize = 11.sp) },
                            placeholder = {
                                Text(
                                    if (selectedCredential?.isCustomPinSet == true) "Masukkan PIN pribadi tersimpan"
                                    else "Masukkan 6 digit akhir NIP",
                                    fontSize = 11.sp
                                )
                            },
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
                                text = errorMessage.ifBlank { "PIN salah. Gunakan PIN pribadi yang tersimpan di database." },
                                color = Color(0xFFDC2626),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        } else {
                            Text(
                                text = "🔒 Skema Login Pintar: PIN default '117 / BPBD117 / TRC2026' ditolak. Masukkan PIN pribadi tersimpan atau 6 digit akhir NIP.",
                                color = Color(0xFF0F766E),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        if (authResultText.isNotBlank()) {
                            Text(
                                text = authResultText,
                                color = if (authResultText.startsWith("Login Biometrik Berhasil")) Color(0xFF15803D) else Color(0xFF1E40AF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        // Visual Biometric Scanner Animation Banner when active
                        AnimatedVisibility(
                            visible = isBiometricScanning,
                            enter = fadeIn(tween(220)) + expandVertically(tween(220)),
                            exit = fadeOut(tween(180)) + shrinkVertically(tween(180))
                        ) {
                            BiometricScanningVisualBanner(
                                officerName = selectedOfficer?.fullName ?: "Petugas Pusdalops",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                                    .testTag("biometric_scanning_animation_banner")
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val buttonScanTransition = rememberInfiniteTransition(label = "biometricButtonPulse")
                        val buttonIconScale by buttonScanTransition.animateFloat(
                            initialValue = 1f,
                            targetValue = if (isBiometricScanning) 1.24f else 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(650, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "buttonIconScale"
                        )

                        OutlinedButton(
                            onClick = {
                                val officer = selectedOfficer
                                if (officer == null) {
                                    isError = true
                                    errorMessage = "Pilih petugas dari daftar terlebih dahulu."
                                    triggerBiometricVibration(context, "ERROR")
                                    return@OutlinedButton
                                }
                                isError = false
                                isBiometricScanning = true
                                authResultText = "Memindai sensor sidik jari / biometrik..."
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                triggerBiometricVibration(context, "SCAN_START")

                                val fallbackOption = AuthenticationRequest.Biometric.Fallback.CustomOption(
                                    "Gunakan PIN",
                                    AuthenticationRequest.Biometric.Fallback.ICON_TYPE_PASSWORD
                                )
                                val biometricRequest = AuthenticationRequest.Biometric.Builder(
                                    "Login Biometrik Petugas Pusdalops",
                                    fallbackOption
                                )
                                    .setSubtitle("${officer.fullName} (${maskNipLast6(officer.nip)})")
                                    .build()
                                authLauncher.launch(biometricRequest)
                            },
                            enabled = !isHarvestingSync,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isBiometricScanning) 1.5.dp else 1.dp,
                                color = if (isBiometricScanning) Color(0xFF0284C7) else Color(0xFF2563EB)
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isBiometricScanning) Color(0xFFE0F2FE) else Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 44.dp)
                                .testTag("officer_biometric_login_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = "Login Biometrik",
                                tint = if (isBiometricScanning) Color(0xFF0284C7) else Color(0xFF2563EB),
                                modifier = Modifier
                                    .size(20.dp)
                                    .graphicsLayer {
                                        scaleX = if (isBiometricScanning) buttonIconScale else 1f
                                        scaleY = if (isBiometricScanning) buttonIconScale else 1f
                                    }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isBiometricScanning) "Memindai Sidik Jari..." else "Gunakan Sidik Jari / Biometrik",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isBiometricScanning) Color(0xFF0369A1) else Color(0xFF2563EB)
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
                                Triple(OfficerRoles.PENGAWAS, "🏛️ Pengawas BPBD", "Pimpinan pengawasan siaga & koordinasi komando kebijakan bencana"),
                                Triple(OfficerRoles.MANAJER, "⭐ Manajer Pusdalops PB", "Penanggung jawab koordinasi operasional komando penanggulangan bencana"),
                                Triple(OfficerRoles.SUPERVISOR, "🛡️ Supervisor BPBD", "Supervisi kesiapsiagaan posko darurat dan pemantauan personil"),
                                Triple(OfficerRoles.KOORD_ADMINISTRASI, "📋 Koord. Administrasi", "Pengelolaan administrasi data posko dan logistik kebencanaan"),
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
                                                fontSize = 11.sp,
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
                                fontSize = 11.sp,
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
                                            val newCred = OfficerCredentialEntity.createDefault(
                                                nip = inputNip.trim(),
                                                fullName = inputFullName.trim(),
                                                role = inputRole,
                                                unit = inputUnit.trim(),
                                                shift = inputShift.trim()
                                            )
                                            AppDatabase.getInstance(context).credentialDao().insertCredential(newCred)
                                            FirestoreHelper.syncCredentialToCloud(newCred)
                                        }
                                        roster = officerAuthManager.getOfficerRoster()
                                        selectedOfficer = newOff
                                        dialogMode = 0
                                        formValidationError = ""
                                        Toast.makeText(
                                            context,
                                            "Petugas '${newOff?.fullName}' berhasil didaftarkan ke sistem & kredensial tersimpan di database!",
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
                                isError = true
                                errorMessage = "Pilih petugas dari daftar terlebih dahulu."
                                return@Button
                            }

                            val nonNullOfficer = officer
                            val trimmedPin = inputPin.trim()
                            if (trimmedPin.isBlank()) {
                                isError = true
                                errorMessage = "Masukkan PIN pribadi petugas dinas."
                                return@Button
                            }

                            // STRICT CHECK: Reject generic default PINs without exception
                            if (SmartOfficerAuthRepository.FORBIDDEN_DEFAULT_PINS.any { it.equals(trimmedPin, ignoreCase = true) }) {
                                isError = true
                                errorMessage = "PIN default '$trimmedPin' dinonaktifkan demi keamanan. Wajib login menggunakan PIN pribadi yang telah tersimpan di database."
                                return@Button
                            }

                            coroutineScope.launch {
                                isHarvestingSync = true
                                val authResult = smartAuthRepo.authenticateOfficer(
                                    nip = nonNullOfficer.nip,
                                    inputPin = trimmedPin
                                )
                                selectedCredential = smartAuthRepo.getCredential(nonNullOfficer.nip)
                                isHarvestingSync = false

                                if (authResult.isSuccess) {
                                    val profile = authResult.getOrNull() ?: nonNullOfficer
                                    val sessionRes = securityManager.verifyDeviceAuthenticityDuringLogin(
                                        officerNip = profile.nip,
                                        officerName = profile.fullName,
                                        officerRole = profile.role
                                    )
                                    val verifiedSession = sessionRes.getOrNull()
                                    val isDeviceAuthorized = sessionRes.isSuccess && (verifiedSession?.accessGranted == true)
                                    if (!isDeviceAuthorized) {
                                        val denyReason = sessionRes.exceptionOrNull()?.message
                                            ?: "Integritas perangkat tidak memenuhi syarat keamanan (${verifiedSession?.verdict ?: "UNVERIFIED"}) untuk mengakses Ruang Kerja."
                                        securityAccessResult = SecurityAccessResult(
                                            isAuthorized = false,
                                            message = denyReason,
                                            session = verifiedSession
                                        )
                                        officerAuthManager.logoutOfficer()
                                        isOfficerLoggedIn = false
                                        isError = true
                                        errorMessage = denyReason
                                        Toast.makeText(context, denyReason, Toast.LENGTH_LONG).show()
                                        return@launch
                                    }
                                    securityAccessResult = SecurityAccessResult(
                                        isAuthorized = true,
                                        message = "Perangkat terverifikasi valid.",
                                        session = verifiedSession
                                    )
                                    isOfficerLoggedIn = true
                                    officerName = profile.fullName
                                    officerBadgeId = profile.nip
                                    officerShift = profile.shift
                                    officerRole = profile.role
                                    showOfficerLoginDialog = false
                                    onOfficerLoginDismissed?.invoke()
                                    selectedTab = 4 // Direct to Konsol Petugas

                                    val targetId = pendingStatusReportId
                                    val targetStatus = pendingStatusTarget
                                    if (targetId != null && targetStatus != null) {
                                        onUpdateReportStatus(targetId, targetStatus)
                                        Toast.makeText(
                                            context,
                                            "Terverifikasi sebagai ${profile.fullName} (NIP: ${maskNipLast6(profile.nip)}). Status laporan diperbarui menjadi '$targetStatus'",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Selamat bertugas, ${profile.fullName} (NIP: ${maskNipLast6(profile.nip)})! (Kredensial Database Terverifikasi)",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    pendingStatusReportId = null
                                    pendingStatusTarget = null
                                } else {
                                    isError = true
                                    val errMsg = authResult.exceptionOrNull()?.message
                                        ?: "Gagal sinkronisasi, periksa koneksi internet"
                                    errorMessage = errMsg
                                    Toast.makeText(context, errMsg, Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        enabled = !isHarvestingSync,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D))
                    ) {
                        if (isHarvestingSync) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Memverifikasi...")
                        } else {
                            Text("Verifikasi & Masuk")
                        }
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

    // Dialog Info Lanskap & Wallpaper HD (diakses dari info aplikasi setelah login)
    if (showDisasterWallpaperDialog) {
        MukomukoDisasterWallpaperDialog(
            onDismiss = { showDisasterWallpaperDialog = false }
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
                        val zoomContext = LocalContext.current
                        val fullPhotoModel = remember(previewPhotoUri) {
                            decodeCitizenPhotoModel(previewPhotoUri)
                        }
                        val brokenImagePainter = rememberVectorPainter(Icons.Default.BrokenImage)
                        var isZoomPhotoError by remember(previewPhotoUri) {
                            mutableStateOf(fullPhotoModel == null)
                        }

                        AsyncImage(
                            model = coil.request.ImageRequest.Builder(zoomContext)
                                .data(fullPhotoModel)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Foto Bencana Resolusi Penuh",
                            placeholder = brokenImagePainter,
                            error = brokenImagePainter,
                            onSuccess = { isZoomPhotoError = false },
                            onError = { isZoomPhotoError = true },
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )

                        if (isZoomPhotoError) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BrokenImage,
                                    contentDescription = "Foto tidak tersedia",
                                    tint = Color(0xFF9CA3AF),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Foto tidak tersedia",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFD1D5DB)
                                )
                            }
                        }

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
                                        fontSize = 11.sp,
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
                                    fontSize = 11.sp,
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
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
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
        }

        AnimatedVisibility(visible = isExpanded) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 3.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    content()
                }
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
@OptIn(ExperimentalLayoutApi::class)
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
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: ID, Time & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = report.id,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6B7280),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false
                    )
                    Text(
                        text = report.incidentTime,
                        fontSize = 11.sp,
                        color = Color(0xFF6B7280),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Sync Status Indicator: Green check = Synced Cloud, Amber sync = Pending
                    val isPending = report.additionalNotes.contains("Tertunda", ignoreCase = true) || !report.isGpsVerified
                    val syncIcon = if (isPending) Icons.Default.CloudSync else Icons.Default.CloudDone
                    val syncColor = if (isPending) Color(0xFFD97706) else Color(0xFF16A34A)
                    val syncText = if (isPending) "Pending" else "Synced"

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = syncColor.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, syncColor.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .heightIn(min = 24.dp)
                            .wrapContentWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 7.dp)
                        ) {
                            Icon(
                                imageVector = syncIcon,
                                contentDescription = syncText,
                                tint = syncColor,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = syncText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = syncColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                softWrap = false
                            )
                        }
                    }

                    // Main Status Badge
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = statusContainer,
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, statusContent.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .heightIn(min = 24.dp)
                            .wrapContentWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(6.dp).background(statusContent, CircleShape))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = statusLabel.uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = statusContent,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                softWrap = false
                            )
                        }
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
                    color = Color(0xFF374151),
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = "Detail: ${report.incidentLocation}",
                fontSize = 11.sp,
                color = Color(0xFF6B7280),
                lineHeight = 16.sp,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            )

            // PHOTO DISPLAY: Captured Photo with BPBD GPS Watermark
            if (!report.photoUri.isNullOrBlank()) {
                val cardContext = LocalContext.current
                val resolvedPhotoModel = remember(report.photoUri) {
                    decodeCitizenPhotoModel(report.photoUri)
                }
                val brokenImagePainter = rememberVectorPainter(Icons.Default.BrokenImage)
                var isPhotoLoadError by remember(report.photoUri) {
                    mutableStateOf(resolvedPhotoModel == null)
                }

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
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { onViewPhoto(report.photoUri) },
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = coil.request.ImageRequest.Builder(cardContext)
                                .data(resolvedPhotoModel)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Foto Bencana di Lokasi",
                            placeholder = brokenImagePainter,
                            error = brokenImagePainter,
                            onSuccess = { isPhotoLoadError = false },
                            onError = { isPhotoLoadError = true },
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        if (isPhotoLoadError) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BrokenImage,
                                    contentDescription = "Foto tidak tersedia",
                                    tint = Color(0xFF9CA3AF),
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Foto tidak tersedia",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFE5E7EB)
                                )
                            }
                        }

                        // GPS Satellite Watermark Banner (Bottom)
                        Surface(
                            color = Color.Black.copy(alpha = 0.78f),
                            shape = RoundedCornerShape(topStart = 8.dp),
                            modifier = Modifier.align(Alignment.BottomEnd)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                                Text(
                                    text = "📍 %.5f, %.5f".format(report.latitude, report.longitude),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFBBF24)
                                )
                                Text(
                                    text = "🛡️ FOTO ASLI BPBD MUKOMUKO",
                                    fontSize = 10.sp,
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
                    text = "🔍 Sentuh untuk zoom • Lihat resolusi penuh & stempel digital",
                    fontSize = 11.sp,
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
                        fontSize = 11.sp,
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
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            // Casualties Breakdown
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                fontSize = 11.sp,
                color = Color(0xFF6B7280),
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
                    modifier = Modifier.weight(1.3f).heightIn(min = 40.dp),
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
                    modifier = Modifier.weight(1f).heightIn(min = 40.dp),
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
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.align(Alignment.CenterVertically)
                ) {
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
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isOfficerLoggedIn) Color(0xFF15803D) else Color(0xFF4B5563)
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
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
        modifier = Modifier
            .heightIn(min = 40.dp)
            .clickable { onClick() }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else Color(0xFF6B7280),
                maxLines = 1,
                softWrap = false
            )
        }
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
                fontSize = 11.sp,
                color = Color(0xFF4B5563),
                modifier = Modifier.weight(1f, fill = false),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$count",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (count > 0) color else Color(0xFF6B7280),
                modifier = Modifier.weight(0.5f, fill = false),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Mendekode sumber gambar laporan warga:
 * - Jika berupa string Base64 (dengan prefix `data:image/...;base64,` maupun raw Base64),
 *   didekode menggunakan `Base64.decode` menjadi `Bitmap`.
 * - Jika berupa URI `file://` atau path file lokal, memverifikasi keberadaan file.
 * - Jika berupa URI `content://` atau URL `http(s)://`, diteruskan langsung ke Coil `AsyncImage`.
 */
internal fun decodeCitizenPhotoModel(photoSource: String?): Any? {
    if (photoSource.isNullOrBlank()) return null
    val trimmed = photoSource.trim()

    // 1. Cek format Data URI Base64 (mis. data:image/jpeg;base64,...)
    if (trimmed.startsWith("data:image", ignoreCase = true) || trimmed.contains("base64,", ignoreCase = true)) {
        return try {
            val base64Part = trimmed.substringAfter("base64,", trimmed)
            val decodedBytes = Base64.decode(base64Part, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (_: Exception) {
            null
        }
    }

    // 2. Cek URI standar (file://, content://, http://, https://, android.resource://)
    if (
        trimmed.startsWith("file://", ignoreCase = true) ||
        trimmed.startsWith("content://", ignoreCase = true) ||
        trimmed.startsWith("http://", ignoreCase = true) ||
        trimmed.startsWith("https://", ignoreCase = true) ||
        trimmed.startsWith("android.resource://", ignoreCase = true)
    ) {
        if (trimmed.startsWith("file://", ignoreCase = true)) {
            val path = Uri.parse(trimmed).path
            if (path.isNullOrBlank()) return null
            val file = File(path)
            return if (file.exists() && file.length() > 0L) file else null
        }
        if (trimmed.startsWith("http://", ignoreCase = true)) {
            return "https://" + trimmed.substring(7)
        }
        return trimmed
    }

    // 3. Cek path file absolut lokal (/data/..., /storage/...)
    if (trimmed.startsWith("/")) {
        val file = File(trimmed)
        return if (file.exists() && file.length() > 0L) file else null
    }

    // 4. Fallback: raw Base64 string tanpa prefix
    return try {
        val decodedBytes = Base64.decode(trimmed, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    } catch (_: Exception) {
        null
    }
}

/**
 * Triggers tactile vibration feedback during biometric authentication stages:
 * - "SCAN_START": Double light pulse when biometric prompt is initiated.
 * - "SUCCESS": Crisp confirmation pulse when fingerprint/face is verified.
 * - "ERROR": Triple warning pulse when authentication fails or is cancelled.
 */
@Suppress("DEPRECATION")
internal fun triggerBiometricVibration(context: Context, mode: String) {
    try {
        val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        if (vibrator == null || !vibrator.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = when (mode) {
                "SUCCESS" -> VibrationEffect.createWaveform(
                    longArrayOf(0L, 40L, 60L, 90L),
                    intArrayOf(0, 180, 0, 255),
                    -1
                )
                "ERROR" -> VibrationEffect.createWaveform(
                    longArrayOf(0L, 65L, 50L, 65L, 50L, 95L),
                    intArrayOf(0, 220, 0, 220, 0, 255),
                    -1
                )
                else -> VibrationEffect.createWaveform(
                    longArrayOf(0L, 35L, 55L, 45L),
                    intArrayOf(0, 150, 0, 200),
                    -1
                )
            }
            vibrator.vibrate(effect)
        } else {
            when (mode) {
                "SUCCESS" -> vibrator.vibrate(longArrayOf(0L, 40L, 60L, 90L), -1)
                "ERROR" -> vibrator.vibrate(longArrayOf(0L, 65L, 50L, 65L, 50L, 95L), -1)
                else -> vibrator.vibrate(longArrayOf(0L, 35L, 55L, 45L), -1)
            }
        }
    } catch (_: Exception) {
        // Ignore if device lacks vibrator hardware or permission is restricted
    }
}

/**
 * Animated visual banner displayed while the biometric sensor is actively scanning.
 * Features concentric pulsing radar rings, a vertical laser scan sweep across the fingerprint icon,
 * and live status instructions for the duty officer.
 */
@Composable
private fun BiometricScanningVisualBanner(
    officerName: String,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "biometricScannerBanner")

    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1350, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarPulse"
    )

    val scanLineY by infiniteTransition.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.88f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laserScanLine"
    )

    val iconGlowScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 675, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iconGlowScale"
    )

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFF0F9FF),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7DD3FC)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(48.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val maxRadius = size.minDimension / 2f

                    // Outer expanding radar ring
                    drawCircle(
                        color = Color(0xFF0284C7).copy(alpha = (1f - pulseProgress) * 0.45f),
                        radius = maxRadius * (0.45f + 0.55f * pulseProgress),
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )

                    // Secondary staggered radar ring
                    val secondaryProgress = (pulseProgress + 0.5f) % 1f
                    drawCircle(
                        color = Color(0xFF2563EB).copy(alpha = (1f - secondaryProgress) * 0.35f),
                        radius = maxRadius * (0.45f + 0.55f * secondaryProgress),
                        center = center,
                        style = Stroke(width = 1.5.dp.toPx())
                    )

                    // Inner soft glow circle
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF38BDF8).copy(alpha = 0.25f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = maxRadius * 0.78f
                        ),
                        radius = maxRadius * 0.78f,
                        center = center
                    )

                    // Horizontal laser scan line sweeping over the fingerprint sensor area
                    val yPos = size.height * scanLineY
                    drawLine(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xFF0284C7),
                                Color(0xFF10B981),
                                Color(0xFF0284C7),
                                Color.Transparent
                            )
                        ),
                        start = Offset(size.width * 0.14f, yPos),
                        end = Offset(size.width * 0.86f, yPos),
                        strokeWidth = 2.2.dp.toPx()
                    )
                }

                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = "Memindai Sidik Jari",
                    tint = Color(0xFF0284C7),
                    modifier = Modifier
                        .size(26.dp)
                        .graphicsLayer {
                            scaleX = iconGlowScale
                            scaleY = iconGlowScale
                        }
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Memindai Sensor Biometrik...",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0369A1)
                )
                Text(
                    text = "Verifikasi identitas: $officerName",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Tempelkan jari pada sensor sidik jari perangkat Anda.",
                    fontSize = 10.sp,
                    color = Color(0xFF475569),
                    lineHeight = 13.sp
                )
            }
        }
    }
}


