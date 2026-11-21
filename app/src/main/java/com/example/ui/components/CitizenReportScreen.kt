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
import androidx.biometric.BiometricManager
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
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.ImageNotSupported
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
import androidx.compose.material.icons.filled.VerifiedUser
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
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
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
import com.example.security.UserRole
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
    onSubmitReportWithResult: ((CitizenDisasterReport, (com.example.data.repository.WeatherRepository.CitizenReportSubmitResult) -> Unit) -> Unit)? = null,
    onDeleteReport: ((String) -> Unit)? = null,
    initialShowOfficerLogin: Boolean = false,
    initialOpenRuangKerja: Boolean = false,
    onOfficerLoginDismissed: (() -> Unit)? = null,
    hasSpecialAssignment: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by rememberSaveable { mutableIntStateOf(if (initialOpenRuangKerja) 4 else 0) } // 0 = Formulir, 1 = Peta Sebaran, 2 = Tabel Riwayat, 3 = Kontak Penting, 4 = Konsol Petugas

    // Officer & Firebase Authentication Manager (SIPAGAR KITA)
    val authManager = remember { com.example.data.auth.AuthManager.getInstance(context) }
    val officerAuthManager = remember { OfficerAuthManager(context) }
    var isOfficerLoggedIn by rememberSaveable { mutableStateOf(authManager.isSessionActive() || officerAuthManager.isOfficerLoggedIn()) }
    var officerName by rememberSaveable { mutableStateOf(officerAuthManager.getOfficerName().ifBlank { authManager.getDisplayName() }) }
    var officerBadgeId by rememberSaveable { mutableStateOf(officerAuthManager.getOfficerBadgeId().ifBlank { authManager.getBadgeOrUid() }) }
    var officerShift by rememberSaveable { mutableStateOf(officerAuthManager.getOfficerShift()) }
    var officerRole by rememberSaveable { mutableStateOf(officerAuthManager.getOfficerRole()) }
    var historyReportPendingDeletion by remember { mutableStateOf<CitizenDisasterReport?>(null) }

    // RBAC Multi-Tier: Tier 3 (Operator/Logistik/TRC/null) hanya akses menu operasional (tanpa Statistik)
    val canShowStatisticsTab = !isOfficerLoggedIn || UserRole.canViewStatistics(officerRole)
    LaunchedEffect(canShowStatisticsTab, selectedTab) {
        if (!canShowStatisticsTab && selectedTab == 5) {
            selectedTab = 0
        }
    }

    val coroutineScope = rememberCoroutineScope()
    val database = remember { AppDatabase.getInstance(context) }
    val personnelRepository = remember { PersonnelRepository(database.personnelDao()) }

    // Firestore Session Management (officer_sessions)
    val securityManager = remember { SecurityManager.getInstance(context) }
    var securityAccessResult by remember { mutableStateOf<SecurityAccessResult?>(null) }
    var isVerifyingSecurity by remember { mutableStateOf(false) }

    DisposableEffect(isOfficerLoggedIn, officerBadgeId) {
        val activeNip = officerBadgeId.ifBlank { authManager.getBadgeOrUid() }.trim()
        val registration = if (isOfficerLoggedIn && activeNip.isNotBlank()) {
            securityManager.listenSecuritySessionsRealtime(
                officerNipFilter = activeNip,
                onActiveSessionRevoked = {
                    securityManager.clearLocalSession()
                    authManager.signOut()
                    officerAuthManager.logoutOfficer()
                    isOfficerLoggedIn = false
                    securityAccessResult = null
                    if (selectedTab == 4) {
                        selectedTab = 0
                    }
                    Toast.makeText(
                        context,
                        "Akses sesi perangkat Anda telah dicabut dari server. Logout paksa dijalankan.",
                        Toast.LENGTH_LONG
                    ).show()
                },
                onUpdate = { _ -> }
            )
        } else {
            null
        }
        onDispose {
            registration?.remove()
        }
    }

    LaunchedEffect(selectedTab, isOfficerLoggedIn, officerBadgeId) {
        if (selectedTab == 4 && (authManager.isSessionActive() || officerAuthManager.isOfficerLoggedIn())) {
            val targetNip = officerBadgeId.ifBlank { authManager.getBadgeOrUid() }
            if (targetNip.isNotBlank()) {
                isVerifyingSecurity = true
                val result = securityManager.verifySessionBeforeGrantingRuangKerja(targetNip)
                securityAccessResult = result
                isVerifyingSecurity = false
                if (!result.isAuthorized && result.session != null && !result.session.accessGranted) {
                    securityManager.clearLocalSession()
                    authManager.signOut()
                    officerAuthManager.logoutOfficer()
                    isOfficerLoggedIn = false
                    selectedTab = 0
                }
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
    LaunchedEffect(initialOpenRuangKerja) {
        if (initialOpenRuangKerja && (authManager.isSessionActive() || officerAuthManager.isOfficerLoggedIn())) {
            isOfficerLoggedIn = true
            officerName = officerAuthManager.getOfficerName().ifBlank { authManager.getDisplayName() }
            officerBadgeId = officerAuthManager.getOfficerBadgeId().ifBlank { authManager.getBadgeOrUid() }
            officerShift = officerAuthManager.getOfficerShift()
            officerRole = officerAuthManager.getOfficerRole()
            selectedTab = 4
        }
    }
    var pendingStatusReportId by remember { mutableStateOf<String?>(null) }
    var pendingStatusTarget by remember { mutableStateOf<String?>(null) }

    // Photo Fullscreen Zoom Dialog State
    var previewPhotoUri by remember { mutableStateOf<String?>(null) }
    var previewPhotoReport by remember { mutableStateOf<CitizenDisasterReport?>(null) }

    // Cloud Online Sync & Zero-Friction Offline Persistence State
    var isSyncingOnline by remember { mutableStateOf(false) }
    var lastSyncStatusMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    val isDeviceOnline by com.example.data.sync.OfflineReportConnectivityMonitor.isOnlineFlow.collectAsStateWithLifecycle()

    LaunchedEffect(context) {
        FirestoreHelper.ensureOfflinePersistenceEnabled(context)
        com.example.data.sync.OfflineReportConnectivityMonitor.ensureRegistered(context)
        com.example.data.sync.OfflineReportConnectivityMonitor.refreshConnectivityState(context)
    }

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
    var pendingCameraFilePath by rememberSaveable { mutableStateOf<String?>(null) }
    var rawCameraBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var cameraCapturedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(photoUriString) {
        if (cameraCapturedBitmap == null && !photoUriString.isNullOrBlank()) {
            cameraCapturedBitmap = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                com.example.util.ImageUtils.decodePhotoSourceToBitmap(photoUriString, context)
            }
        }
    }

    var reporterName by rememberSaveable { mutableStateOf("") }
    var reporterPhone by rememberSaveable { mutableStateOf("") }
    var additionalNotes by rememberSaveable { mutableStateOf("") }

    // Submit progress & Dialog state (WAJIB menunggu upload foto selesai 100%)
    var isSubmittingReport by remember { mutableStateOf(false) }
    var submitStatusLabel by remember { mutableStateOf("") }
    var lastSubmitResult by remember {
        mutableStateOf<com.example.data.repository.WeatherRepository.CitizenReportSubmitResult?>(null)
    }
    var showSuccessDialog by rememberSaveable { mutableStateOf(false) }
    var submittedReportId by rememberSaveable { mutableStateOf("") }
    var trackingReportIdQuery by rememberSaveable { mutableStateOf("") }

    // Table Filter by Status: "Semua", "Diproses", "Divalidasi", "Selesai"
    var tableStatusFilter by rememberSaveable { mutableStateOf("Semua") }

    // Permission Rationale / Guidance Dialog state
    var permissionDeniedDialogMessage by remember { mutableStateOf<String?>(null) }
    var permissionDeniedDialogTitle by remember { mutableStateOf<String?>(null) }

    // Fallback Camera launcher (TakePicturePreview) jika perangkat tidak mendukung FileProvider URI
    val cameraPreviewFallbackLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            val optimizedBitmap = com.example.util.ImageUtils.optimizeReportBitmap(bitmap)
            rawCameraBitmap = optimizedBitmap
            // Render watermark GPS langsung ke piksel Bitmap agar gambar yang di-upload membawa stempel GPS resmi
            val watermarkedBitmap = com.example.util.ImageUtils.renderGpsWatermarkOnBitmap(
                source = optimizedBitmap,
                latitude = latitude,
                longitude = longitude,
                incidentTime = incidentTime,
                locationLabel = "$selectedVillage, Kec. $selectedDistrict",
                accuracyMeters = gpsAccuracy
            )
            cameraCapturedBitmap = watermarkedBitmap
            val savedPersistentUri = com.example.util.ImageUtils.saveBitmapToPersistentReportFile(context, watermarkedBitmap)
                ?: com.example.util.ImageUtils.encodeBitmapToCloudDataUri(watermarkedBitmap)
            photoUriString = savedPersistentUri
            Toast.makeText(context, "Foto bukti kamera + watermark GPS tersimpan siap unggah ke Pusdalops!", Toast.LENGTH_SHORT).show()
        }
    }

    // Primary Full-Resolution Camera launcher (TakePicture via FileProvider -> persistent filesDir)
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        val capturedPath = pendingCameraFilePath
        if (success && !capturedPath.isNullOrBlank()) {
            val optimizedBitmap = com.example.util.ImageUtils.decodeCapturedFileSafely(capturedPath)
            if (optimizedBitmap != null) {
                rawCameraBitmap = optimizedBitmap
                // Render watermark GPS langsung ke piksel Bitmap agar file yang di-upload adalah hasil render watermark GPS
                val watermarkedBitmap = com.example.util.ImageUtils.renderGpsWatermarkOnBitmap(
                    source = optimizedBitmap,
                    latitude = latitude,
                    longitude = longitude,
                    incidentTime = incidentTime,
                    locationLabel = "$selectedVillage, Kec. $selectedDistrict",
                    accuracyMeters = gpsAccuracy
                )
                cameraCapturedBitmap = watermarkedBitmap
                // Simpan hasil render watermark & koreksi orientasi EXIF ke file persisten agar tajam dan hemat kuota
                val savedUri = try {
                    val file = File(capturedPath)
                    FileOutputStream(file).use { out ->
                        watermarkedBitmap.compress(
                            Bitmap.CompressFormat.JPEG,
                            com.example.util.ImageUtils.OPTIMAL_JPEG_QUALITY,
                            out
                        )
                    }
                    if (file.exists() && file.length() > 0L) Uri.fromFile(file).toString() else null
                } catch (_: Exception) {
                    com.example.util.ImageUtils.saveBitmapToPersistentReportFile(context, watermarkedBitmap)
                } ?: com.example.util.ImageUtils.encodeBitmapToCloudDataUri(watermarkedBitmap)

                photoUriString = savedUri
                Toast.makeText(context, "Foto bukti + watermark GPS tersimpan siap unggah ke Pusdalops!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Gagal membaca hasil foto kamera, silakan ulangi.", Toast.LENGTH_SHORT).show()
            }
        }
        pendingCameraFilePath = null
    }

    val launchLiveCamera: () -> Unit = {
        try {
            val photoFile = com.example.util.ImageUtils.createPersistentReportPhotoFile(context)
            val contentUri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                photoFile
            )
            pendingCameraFilePath = photoFile.absolutePath
            cameraLauncher.launch(contentUri)
        } catch (_: Exception) {
            pendingCameraFilePath = null
            cameraPreviewFallbackLauncher.launch(null)
        }
    }

    // Camera Runtime Permission Launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchLiveCamera()
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
            launchLiveCamera()
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
            val effectiveTabIndex = if (canShowStatisticsTab) selectedTab else selectedTab.coerceAtMost(4)
            ScrollableTabRow(
                selectedTabIndex = effectiveTabIndex,
                containerColor = Color(0xFF15803D),
                contentColor = Color.White,
                edgePadding = 8.dp,
                indicator = { tabPositions ->
                    if (effectiveTabIndex < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[effectiveTabIndex]),
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

                // Tab 2: Tabel Riwayat & Lacak Status
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Riwayat & Lacak",
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
                            selectedTab = 4
                            showOfficerLoginDialog = true
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

                // Tab 5: Statistik & Tren Bulanan (RBAC: Disembunyikan untuk petugas Tier 3 Operator/Logistik/TRC)
                if (canShowStatisticsTab) {
                    Tab(
                        selected = selectedTab == 5,
                        onClick = { selectedTab = 5 },
                        modifier = Modifier.testTag("tab_tren_bulanan_statistik"),
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
                    val citizenFormKeyboardActions = rememberFormKeyboardActions()
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .imePadding()
                            .padding(horizontal = 20.dp),
                        contentPadding = PaddingValues(top = 12.dp, bottom = 110.dp),
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
                        // QUICK ACCESS: PELACAKAN STATUS LAPORAN (TRACKING ID)
                        item {
                            Surface(
                                color = Color(0xFFF0FDF4),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF86EFAC)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedTab = 2 }
                                    .testTag("banner_open_status_tracker")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = Color(0xFF15803D),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Sudah Mengirim Laporan? Lacak Status Penanganan",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF14532D)
                                        )
                                        Text(
                                            text = "Cek ID Laporan Anda (Pending / Proses / Selesai) secara real-time",
                                            fontSize = 10.sp,
                                            color = Color(0xFF166534)
                                        )
                                    }
                                    Surface(
                                        color = Color(0xFF15803D),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "LACAK ID",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }

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
                                    keyboardOptions = formNextKeyboardOptions(),
                                    keyboardActions = citizenFormKeyboardActions,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp)
                                        .formScrollPadding(),
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
                                                    val targetUri = photoUriString?.takeIf { it.isNotBlank() }
                                                        ?: cameraCapturedBitmap?.let {
                                                            com.example.util.ImageUtils.encodeBitmapToCloudDataUri(it)
                                                        }
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
                                            if (previewModel is Bitmap) {
                                                Image(
                                                    bitmap = previewModel.asImageBitmap(),
                                                    contentDescription = "Foto Bukti Kejadian",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                AsyncImage(
                                                    model = previewModel,
                                                    contentDescription = "Foto Bukti Kejadian",
                                                    placeholder = fallbackPainter,
                                                    error = fallbackPainter,
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
                                        keyboardOptions = formNextKeyboardOptions(),
                                        keyboardActions = citizenFormKeyboardActions,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .formScrollPadding()
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
                                    keyboardOptions = formDoneKeyboardOptions(),
                                    keyboardActions = citizenFormKeyboardActions,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp)
                                        .formScrollPadding(),
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

                                    val coordValidation = com.example.util.CoordinateValidator.validate(latitude, longitude)
                                    if (coordValidation.isFailure) {
                                        val errMsg = coordValidation.exceptionOrNull()?.message
                                            ?: com.example.util.CoordinateValidator.ERROR_INVALID_RANGE
                                        Toast.makeText(context, errMsg, Toast.LENGTH_LONG).show()
                                        return@Button
                                    }

                                    if (isSubmittingReport) return@Button
                                    val nowMs = System.currentTimeMillis()
                                    val reportId = "LAP-MMK-" + SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date(nowMs)) + "-" + (100..999).random()
                                     val isOnlineNow = com.example.data.sync.OfflineReportConnectivityMonitor.refreshConnectivityState(context)
                                     isSubmittingReport = true
                                     submitStatusLabel = if (!isOnlineNow) {
                                         FirestoreHelper.OFFLINE_REPORT_SAVED_MESSAGE
                                     } else if (!photoUriString.isNullOrBlank() || rawCameraBitmap != null || cameraCapturedBitmap != null) {
                                         "Mengunggah foto bukti GPS ke server (0-100%)..."
                                     } else {
                                         "Mengirim laporan resmi ke PUSDALOPS BPBD..."
                                     }

                                     coroutineScope.launch {
                                         // Pastikan jika ada foto kamera, gambar yang di-upload adalah hasil render watermark GPS final
                                         val finalWatermarkedPhotoUri = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                             val sourceRaw = rawCameraBitmap
                                             if (sourceRaw != null) {
                                                 val finalStamped = com.example.util.ImageUtils.renderGpsWatermarkOnBitmap(
                                                     source = sourceRaw,
                                                     latitude = latitude,
                                                     longitude = longitude,
                                                     incidentTime = incidentTime,
                                                     locationLabel = "$selectedVillage, Kec. $selectedDistrict",
                                                     accuracyMeters = gpsAccuracy
                                                 )
                                                 cameraCapturedBitmap = finalStamped
                                                 com.example.util.ImageUtils.saveBitmapToPersistentReportFile(context, finalStamped)
                                                     ?: com.example.util.ImageUtils.encodeBitmapToCloudDataUri(finalStamped)
                                             } else {
                                                 photoUriString?.trim()?.takeIf { it.isNotBlank() }
                                                     ?: cameraCapturedBitmap?.let {
                                                         com.example.util.ImageUtils.saveBitmapToPersistentReportFile(context, it)
                                                             ?: com.example.util.ImageUtils.encodeBitmapToCloudDataUri(it)
                                                     }
                                             }
                                         }

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
                                             photoUri = finalWatermarkedPhotoUri,
                                             reporterName = reporterName.ifBlank { "Warga $selectedDistrict" },
                                             reporterPhone = reporterPhone,
                                             additionalNotes = additionalNotes,
                                             timestamp = nowMs,
                                             status = com.example.model.CitizenReportStatusHelper.STATUS_DIPROSES,
                                             updatedAt = nowMs,
                                             isSyncedToCloud = false
                                         )

                                         if (onSubmitReportWithResult != null) {
                                             onSubmitReportWithResult(report) { result ->
                                                 isSubmittingReport = false
                                                 submitStatusLabel = ""
                                                 lastSubmitResult = result
                                                 submittedReportId = reportId
                                                 trackingReportIdQuery = reportId
                                                 if (result.deferredOffline || !result.syncedToCloud || !isOnlineNow) {
                                                     Toast.makeText(
                                                         context,
                                                         FirestoreHelper.OFFLINE_REPORT_SAVED_MESSAGE,
                                                         Toast.LENGTH_LONG
                                                     ).show()
                                                     coroutineScope.launch {
                                                         snackbarHostState.showSnackbar(
                                                             message = FirestoreHelper.OFFLINE_REPORT_SAVED_MESSAGE
                                                         )
                                                     }
                                                 }
                                                 showSuccessDialog = true
                                             }
                                         } else {
                                             onSubmitReport(report)
                                             isSubmittingReport = false
                                             submitStatusLabel = ""
                                             lastSubmitResult = null
                                             submittedReportId = reportId
                                             trackingReportIdQuery = reportId
                                             if (!isOnlineNow) {
                                                 Toast.makeText(
                                                     context,
                                                     FirestoreHelper.OFFLINE_REPORT_SAVED_MESSAGE,
                                                     Toast.LENGTH_LONG
                                                 ).show()
                                                 coroutineScope.launch {
                                                     snackbarHostState.showSnackbar(
                                                         message = FirestoreHelper.OFFLINE_REPORT_SAVED_MESSAGE
                                                     )
                                                 }
                                             }
                                             showSuccessDialog = true
                                         }
                                     }
                                 },
                                 enabled = !isSubmittingReport,
                                 colors = ButtonDefaults.buttonColors(
                                     containerColor = Color(0xFFDC2626),
                                     contentColor = Color.White,
                                     disabledContainerColor = Color(0xFFEA580C).copy(alpha = 0.75f),
                                     disabledContentColor = Color.White
                                 ),
                                 border = androidx.compose.foundation.BorderStroke(2.5.dp, Color(0xFFFED7AA)),
                                 elevation = ButtonDefaults.buttonElevation(
                                     defaultElevation = 10.dp,
                                     pressedElevation = 4.dp
                                 ),
                                 shape = RoundedCornerShape(18.dp),
                                 contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
                                 modifier = Modifier
                                     .fillMaxWidth()
                                     .heightIn(min = 82.dp)
                                     .testTag("btn_submit_citizen_report")
                             ) {
                                 if (isSubmittingReport) {
                                     CircularProgressIndicator(
                                         color = Color.White,
                                         strokeWidth = 3.dp,
                                         modifier = Modifier.size(26.dp)
                                     )
                                     Spacer(modifier = Modifier.width(12.dp))
                                     Column {
                                         Text(
                                             text = if (!isDeviceOnline) {
                                                 "MENYIMPAN LAPORAN DI PERANGKAT..."
                                             } else {
                                                 "MENGIRIM LAPORAN DARURAT..."
                                             },
                                             fontSize = 14.sp,
                                             fontWeight = FontWeight.ExtraBold,
                                             color = Color.White
                                         )
                                         Text(
                                             text = submitStatusLabel.ifBlank {
                                                 if (!isDeviceOnline) {
                                                     FirestoreHelper.OFFLINE_REPORT_SAVED_MESSAGE
                                                 } else {
                                                     "Mengunggah foto GPS & menyimpan..."
                                                 }
                                             },
                                             fontSize = 11.sp,
                                             fontWeight = FontWeight.Medium,
                                             color = Color(0xFFFFEDD5)
                                         )
                                     }
                                 } else {
                                     Surface(
                                         shape = CircleShape,
                                         color = Color(0xFFEA580C),
                                         border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.White),
                                         modifier = Modifier.size(44.dp)
                                     ) {
                                         Box(contentAlignment = Alignment.Center) {
                                             Icon(
                                                 imageVector = Icons.AutoMirrored.Filled.Send,
                                                 contentDescription = "Kirim Laporan Darurat",
                                                 tint = Color.White,
                                                 modifier = Modifier.size(24.dp)
                                             )
                                         }
                                     }
                                     Spacer(modifier = Modifier.width(14.dp))
                                     Column(
                                         horizontalAlignment = Alignment.Start,
                                         modifier = Modifier.weight(1f)
                                     ) {
                                         Text(
                                             text = if (isDeviceOnline) {
                                                 "🚨 KIRIM LAPORAN"
                                             } else {
                                                 "🚨 KIRIM LAPORAN (SIMPAN OFFLINE)"
                                             },
                                             fontSize = 17.sp,
                                             fontWeight = FontWeight.Black,
                                             letterSpacing = 0.5.sp,
                                             color = Color.White
                                         )
                                         Text(
                                             text = if (isDeviceOnline) {
                                                 "TOMBOL DARURAT • Kirim Lokasi GPS & Foto Bukti ke Pusdalops BPBD"
                                             } else {
                                                 FirestoreHelper.OFFLINE_REPORT_SAVED_MESSAGE
                                             },
                                             fontSize = 11.sp,
                                             fontWeight = FontWeight.Bold,
                                             color = Color(0xFFFFEDD5),
                                             lineHeight = 14.sp
                                         )
                                     }
                                 }
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
                                                    showOfficerLoginDialog = true
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
                                                    showOfficerLoginDialog = true
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

                        // 2. Pelacakan Status Laporan (Tracking ID Laporan — Pending / Proses / Selesai)
                        item {
                            CitizenReportStatusTrackerCard(
                                reports = reports,
                                initialTrackingId = trackingReportIdQuery,
                                onTrackingIdChange = { trackingReportIdQuery = it },
                                onViewPhoto = { photoUri, rep ->
                                    previewPhotoUri = photoUri
                                    previewPhotoReport = rep
                                }
                            )
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

                        val displayedReports = reports.filter {
                            com.example.model.CitizenReportStatusHelper.matchesFilter(it.status, tableStatusFilter)
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
                                    officerRole = officerRole,
                                    onUpdateStatus = { newStatus ->
                                        if (isOfficerLoggedIn) {
                                            if (newStatus.equals("Divalidasi", ignoreCase = true)) {
                                                if (!UserRole.canValidateReports(officerRole)) {
                                                    Toast.makeText(
                                                        context,
                                                        "Akses Ditolak (RBAC): Tombol Validasi Laporan hanya untuk Manajer, Pengawas, Supervisor, atau Koord. Administrasi.",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                } else {
                                                    onUpdateReportStatus(report.id, newStatus)
                                                    Toast.makeText(
                                                        context,
                                                        "Status laporan #${report.id.takeLast(7)} divalidasi oleh $officerName ($officerRole)",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            } else if (newStatus.equals("Selesai", ignoreCase = true)) {
                                                if (!UserRole.canCompleteReports(officerRole)) {
                                                    Toast.makeText(
                                                        context,
                                                        "Akses Ditolak (RBAC): Status Selesai hanya dapat dieksekusi oleh Tier 1 (Manajer) atau Tier 2 (Pengawas/Supervisor/Koord. Administrasi).",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                } else {
                                                    onUpdateReportStatus(report.id, newStatus)
                                                    Toast.makeText(
                                                        context,
                                                        "Status laporan #${report.id.takeLast(7)} diubah menjadi '$newStatus' oleh $officerName",
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
                                    },
                                    onDeleteReport = if (isOfficerLoggedIn && UserRole.canDeleteReports(officerRole) && onDeleteReport != null) {
                                        { historyReportPendingDeletion = report }
                                    } else {
                                        null
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
                                    officerName = officerAuthManager.getOfficerName()
                                    officerBadgeId = officerAuthManager.getOfficerBadgeId()
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
                                onSyncReports = onSyncReports,
                                onDeleteReport = onDeleteReport
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
                                    showOfficerLoginDialog = true
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
                    // TAB 5: TREN & STATISTIK BENCANA BULANAN (RBAC: Tier 1 & Tier 2, atau publik non-petugas)
                    if (canShowStatisticsTab) {
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
            androidx.compose.material3.SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(12.dp)
            )
        }
    }
}

    // Konfirmasi Hapus Laporan (RBAC Tier 1: HANYA untuk Manajer)
    val reportToDelete = historyReportPendingDeletion
    if (reportToDelete != null && isOfficerLoggedIn && UserRole.canDeleteReports(officerRole)) {
        AlertDialog(
            onDismissRequest = { historyReportPendingDeletion = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Hapus Laporan",
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(34.dp)
                )
            },
            title = {
                Text(
                    text = "Hapus Laporan Warga?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = "Laporan #${reportToDelete.id} (${reportToDelete.disasterType} - ${reportToDelete.village}) akan dihapus permanen dari perangkat dan server Cloud Firestore. Aksi eksklusif Tier 1 Manajer.",
                    fontSize = 12.sp,
                    color = Color(0xFF374151),
                    lineHeight = 17.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = reportToDelete.id
                        historyReportPendingDeletion = null
                        onDeleteReport?.invoke(id)
                        Toast.makeText(context, "Laporan #$id dihapus oleh Manajer $officerName", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    modifier = Modifier.testTag("btn_confirm_history_delete_report")
                ) {
                    Text("Ya, Hapus Permanen")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { historyReportPendingDeletion = null }) {
                    Text("Batal")
                }
            }
        )
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

    // Success Confirmation Dialog (Zero-Friction Offline Persistence & Cloud status)
    if (showSuccessDialog) {
        val isDeferredOffline = lastSubmitResult?.deferredOffline == true || lastSubmitResult?.syncedToCloud == false
        val hasUploadedPhoto = lastSubmitResult?.hasPhoto == true && lastSubmitResult?.syncedToCloud == true
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
                    text = if (isDeferredOffline) "Laporan Tersimpan di Perangkat" else "Laporan Bencana Terkirim!",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (isDeferredOffline) {
                            FirestoreHelper.OFFLINE_REPORT_SAVED_MESSAGE
                        } else {
                            lastSubmitResult?.message?.takeIf { it.isNotBlank() }
                                ?: "Laporan Anda telah berhasil dicatat oleh sistem PUSDALOPS BPBD Kabupaten Mukomuko."
                        },
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
                            Text(
                                text = when {
                                    isDeferredOffline -> "Status: 🟢 Tersimpan di Perangkat (Dikirim Otomatis Saat Sinyal Tersedia)"
                                    hasUploadedPhoto -> "Status Kirim: 🟢 Foto GPS & Laporan Tersinkron 100%"
                                    else -> "Status: 🟡 Diproses (Menunggu Verifikasi Lapangan)"
                                },
                                fontSize = 11.sp,
                                color = Color(0xFF15803D),
                                fontWeight = FontWeight.Bold
                            )
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
                        trackingReportIdQuery = submittedReportId
                        selectedTab = 2 // Switch to tracking & history view
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D))
                ) {
                    Text("Lacak Status & Riwayat")
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

    // Officer Login & PIN Verification Dialog ("Sekali Input NIP, Seterusnya Cukup PIN")
    if (showOfficerLoginDialog) {
        val smartAuthRepo = remember { SmartOfficerAuthRepository.getInstance(context) }

        // 1. Cek Penyimpanan (Secure Storage via Android Keystore AES-256-GCM)
        var savedNip by remember { mutableStateOf(officerAuthManager.getSavedNip()) }
        var savedName by remember { mutableStateOf(officerAuthManager.getOfficerName().trim()) }
        val hasSavedNip = savedNip.isNotBlank()

        var inputNipOrName by remember { mutableStateOf("") }
        var inputPin by remember { mutableStateOf("") }
        var isPinVisible by remember { mutableStateOf(false) }
        var isSubmitting by remember { mutableStateOf(false) }
        var isSyncingCloud by remember { mutableStateOf(false) }
        var isBiometricScanning by remember { mutableStateOf(false) }
        var pendingBiometricNip by remember { mutableStateOf("") }
        var pendingBiometricLabel by remember { mutableStateOf("") }
        var isError by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf("") }
        var syncStatusMessage by remember { mutableStateOf("") }

        val biometricStatus = remember(context) {
            try {
                BiometricManager.from(context).canAuthenticate(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.BIOMETRIC_WEAK
                )
            } catch (_: Exception) {
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE
            }
        }
        val isBiometricAvailable = biometricStatus == BiometricManager.BIOMETRIC_SUCCESS
        val isBiometricNotEnrolled = biometricStatus == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED

        val savedOfficerDisplay = remember(savedNip, savedName) {
            when {
                savedName.isNotBlank() && savedNip.isNotBlank() -> "$savedName ($savedNip)"
                savedNip.isNotBlank() -> savedNip
                else -> ""
            }
        }

        val biometricAuthLauncher = rememberAuthenticationLauncher { authResult ->
            when (authResult) {
                is AuthenticationResult.Success -> {
                    isBiometricScanning = false
                    triggerBiometricVibration(context, "SUCCESS")
                    val targetIdentity = pendingBiometricNip.ifBlank {
                        savedNip.ifBlank { inputNipOrName.trim() }
                    }
                    coroutineScope.launch {
                        isSubmitting = true
                        isError = false
                        errorMessage = ""
                        try {
                            val bioResult = smartAuthRepo.authenticateWithBiometric(targetIdentity)
                            if (bioResult.isSuccess) {
                                val cred = bioResult.getOrThrow()
                                val sessionRes = securityManager.verifyDeviceAuthenticityDuringLogin(
                                    officerNip = cred.nip,
                                    officerName = cred.fullName,
                                    officerRole = cred.role,
                                    loginMethod = "BIOMETRIK",
                                    loginAction = "LOGIN_SUCCESSFUL_BIOMETRIC_AUTH"
                                )
                                val verifiedSession = sessionRes.getOrNull()
                                val isDeviceAuthorized = sessionRes.isSuccess && (verifiedSession?.accessGranted == true)
                                if (!isDeviceAuthorized) {
                                    val denyReason = sessionRes.exceptionOrNull()?.message
                                        ?: "Integritas perangkat tidak memenuhi syarat keamanan untuk mengakses Ruang Kerja."
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
                                    message = "Perangkat & Biometrik terverifikasi valid.",
                                    session = verifiedSession
                                )

                                officerAuthManager.saveNipToSecureStorage(
                                    nip = cred.nip,
                                    fullName = cred.fullName,
                                    role = cred.role,
                                    shift = cred.shift
                                )
                                officerAuthManager.loginOfficerDirectly(
                                    name = cred.fullName,
                                    badgeId = cred.nip,
                                    shift = cred.shift,
                                    role = cred.role
                                )
                                savedNip = cred.nip
                                savedName = cred.fullName
                                isOfficerLoggedIn = true
                                officerName = cred.fullName
                                officerBadgeId = cred.nip
                                officerShift = cred.shift
                                officerRole = cred.role
                                showOfficerLoginDialog = false
                                onOfficerLoginDismissed?.invoke()
                                selectedTab = 4 // Masuk langsung ke Ruang Kerja Petugas

                                val targetId = pendingStatusReportId
                                val targetStatus = pendingStatusTarget
                                if (targetId != null && targetStatus != null) {
                                    onUpdateReportStatus(targetId, targetStatus)
                                    Toast.makeText(
                                        context,
                                        "Terverifikasi Biometrik sebagai ${cred.fullName}. Status diperbarui.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Selamat bertugas, ${cred.fullName}! (Biometrik Terverifikasi)",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                pendingStatusReportId = null
                                pendingStatusTarget = null
                            } else {
                                triggerBiometricVibration(context, "ERROR")
                                isError = true
                                errorMessage = bioResult.exceptionOrNull()?.message
                                    ?: "Verifikasi kredensial biometrik gagal."
                            }
                        } catch (e: Exception) {
                            triggerBiometricVibration(context, "ERROR")
                            isError = true
                            errorMessage = e.localizedMessage
                                ?: "Terjadi kendala saat autentikasi biometrik. Silakan coba lagi."
                        } finally {
                            isSubmitting = false
                        }
                    }
                }
                is AuthenticationResult.Error -> {
                    isBiometricScanning = false
                    triggerBiometricVibration(context, "ERROR")
                    isError = true
                    errorMessage = "Autentikasi biometrik gagal/dibatalkan: ${authResult.errString}"
                }
                is AuthenticationResult.CustomFallbackSelected -> {
                    isBiometricScanning = false
                    isError = false
                    errorMessage = ""
                }
            }
        }

        val launchBiometricLogin: () -> Unit = triggerBio@{
            if (!isBiometricAvailable) {
                isError = true
                errorMessage = if (isBiometricNotEnrolled) {
                    "Sidik jari / pengenalan wajah belum didaftarkan di Pengaturan perangkat Anda."
                } else {
                    "Perangkat ini tidak mendukung autentikasi biometrik."
                }
                return@triggerBio
            }

            isError = false
            errorMessage = ""
            syncStatusMessage = ""

            if (hasSavedNip) {
                pendingBiometricNip = savedNip
                pendingBiometricLabel = savedOfficerDisplay.ifBlank { savedNip }
                isBiometricScanning = true
                triggerBiometricVibration(context, "SCAN_START")
                val fallbackOption = AuthenticationRequest.Biometric.Fallback.CustomOption(
                    "Gunakan PIN",
                    AuthenticationRequest.Biometric.Fallback.ICON_TYPE_PASSWORD
                )
                val biometricRequest = AuthenticationRequest.Biometric.Builder(
                    "Login Biometrik Petugas Pusdalops",
                    fallbackOption
                )
                    .setSubtitle(pendingBiometricLabel)
                    .build()
                biometricAuthLauncher.launch(biometricRequest)
            } else {
                val typedIdentity = inputNipOrName.trim()
                if (typedIdentity.isBlank()) {
                    isError = true
                    errorMessage = "Masukkan Nama/NIP terlebih dahulu atau login dengan PIN sekali untuk mengaktifkan Login Biometrik."
                    return@triggerBio
                }
                coroutineScope.launch {
                    isSubmitting = true
                    try {
                        val resolvedNip = smartAuthRepo.resolveNipFromDirectoryOrInput(typedIdentity)
                        if (resolvedNip.isNullOrBlank()) {
                            isError = true
                            errorMessage = "NIP atau Nama '$typedIdentity' tidak dikenali. Periksa kembali Nama/NIP Anda."
                            return@launch
                        }
                        pendingBiometricNip = resolvedNip
                        pendingBiometricLabel = "$typedIdentity ($resolvedNip)"
                        isBiometricScanning = true
                        triggerBiometricVibration(context, "SCAN_START")
                        val fallbackOption = AuthenticationRequest.Biometric.Fallback.CustomOption(
                            "Gunakan PIN",
                            AuthenticationRequest.Biometric.Fallback.ICON_TYPE_PASSWORD
                        )
                        val biometricRequest = AuthenticationRequest.Biometric.Builder(
                            "Login Biometrik Petugas Pusdalops",
                            fallbackOption
                        )
                            .setSubtitle(pendingBiometricLabel)
                            .build()
                        biometricAuthLauncher.launch(biometricRequest)
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        isError = true
                        errorMessage = e.message ?: "Gagal memverifikasi identitas biometrik."
                        isBiometricScanning = false
                    } finally {
                        isSubmitting = false
                    }
                }
            }
        }

        val submitOfficerLogin: () -> Unit = submitLogin@{
            val trimmedPin = inputPin.trim()
            if (hasSavedNip) {
                // Skenario B: saved_nip ADA -> Hanya validasi PIN yang diketik + saved_nip dari Secure Storage
                if (trimmedPin.isEmpty()) {
                    isError = true
                    errorMessage = "Silakan masukkan PIN Anda"
                    return@submitLogin
                }
            } else {
                // Skenario A: saved_nip KOSONG -> Validasi Kolom 1 (NIP/Nama) & Kolom 2 (PIN)
                if (inputNipOrName.trim().isEmpty()) {
                    isError = true
                    errorMessage = "Silakan masukkan NIP atau Nama"
                    return@submitLogin
                }
                if (trimmedPin.isEmpty()) {
                    isError = true
                    errorMessage = "Silakan masukkan PIN Anda"
                    return@submitLogin
                }
            }

            isError = false
            errorMessage = ""
            syncStatusMessage = ""

            coroutineScope.launch {
                isSubmitting = true
                try {
                    val authResult = if (hasSavedNip) {
                        // Skenario B: Ambil saved_nip dari Secure Storage, gabungkan dengan PIN, verifikasi ke API/Repo
                        smartAuthRepo.authenticateOfficer(
                            nip = savedNip,
                            inputPin = trimmedPin
                        )
                    } else {
                        // Skenario A: Verifikasi NIP atau Nama beserta PIN ke API/Repo
                        smartAuthRepo.authenticateByNipOrNameAndPin(
                            nipOrName = inputNipOrName.trim(),
                            inputPin = trimmedPin
                        )
                    }

                    if (authResult.isSuccess) {
                        val profile = authResult.getOrThrow()
                        val sessionRes = securityManager.verifyDeviceAuthenticityDuringLogin(
                            officerNip = profile.nip,
                            officerName = profile.fullName,
                            officerRole = profile.role,
                            loginMethod = "PIN_CLOUD",
                            loginAction = "LOGIN_SUCCESSFUL_CLOUD_VERIFIED"
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

                        // Simpan NIP & identitas petugas ke Secure Storage (AES-256-GCM)
                        officerAuthManager.saveNipToSecureStorage(
                            nip = profile.nip,
                            fullName = profile.fullName,
                            role = profile.role,
                            shift = profile.shift
                        )
                        officerAuthManager.loginOfficerDirectly(
                            name = profile.fullName,
                            badgeId = profile.nip,
                            shift = profile.shift,
                            role = profile.role
                        )
                        savedNip = profile.nip
                        savedName = profile.fullName
                        isOfficerLoggedIn = true
                        officerName = profile.fullName
                        officerBadgeId = profile.nip
                        officerShift = profile.shift
                        officerRole = profile.role
                        showOfficerLoginDialog = false
                        onOfficerLoginDismissed?.invoke()
                        selectedTab = 4 // Arahkan ke halaman utama Ruang Kerja Petugas

                        val targetId = pendingStatusReportId
                        val targetStatus = pendingStatusTarget
                        if (targetId != null && targetStatus != null) {
                            onUpdateReportStatus(targetId, targetStatus)
                            Toast.makeText(
                                context,
                                "Terverifikasi sebagai ${profile.fullName}. Status laporan diperbarui menjadi '$targetStatus'",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                context,
                                "Selamat bertugas, ${profile.fullName}!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        pendingStatusReportId = null
                        pendingStatusTarget = null
                    } else {
                        isError = true
                        val errMsg = authResult.exceptionOrNull()?.message
                            ?: if (hasSavedNip) "PIN tidak sesuai." else "NIP/Nama atau PIN tidak sesuai."
                        errorMessage = errMsg
                    }
                } catch (e: Exception) {
                    isError = true
                    errorMessage = e.localizedMessage
                        ?: "Terjadi kesalahan saat memproses login. Silakan coba lagi."
                } finally {
                    isSubmitting = false
                }
            }
        }

        Dialog(
            onDismissRequest = {
                if (!isSubmitting && !isSyncingCloud) {
                    showOfficerLoginDialog = false
                    onOfficerLoginDismissed?.invoke()
                    pendingStatusReportId = null
                    pendingStatusTarget = null
                }
            }
        ) {
            val loginKeyboardActions = rememberFormKeyboardActions(
                onDoneCallback = {
                    if (!isSubmitting) {
                        submitOfficerLogin()
                    }
                }
            )
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                tonalElevation = 8.dp,
                shadowElevation = 12.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .padding(horizontal = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 1. Logo
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFDCFCE7),
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Badge,
                                contentDescription = "Logo Pusdalops",
                                tint = Color(0xFF15803D),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 2. Judul
                    Text(
                        text = "Login Kredensial Petugas Pusdalops",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center,
                        color = Color(0xFF0F172A)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Skenario B: Tampilkan info "Masuk sebagai: [Nama Petugas/NIP]" di atas kolom PIN
                    AnimatedVisibility(visible = hasSavedNip) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF0FDF4),
                            border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 14.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = Color(0xFF15803D),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Masuk sebagai: $savedOfficerDisplay",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF166534),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // Skenario A: Jika saved_nip KOSONG -> Tampilkan Kolom 1 (Masukkan NIP atau Nama)
                    AnimatedVisibility(visible = !hasSavedNip) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = inputNipOrName,
                                onValueChange = {
                                    inputNipOrName = it
                                    isError = false
                                    errorMessage = ""
                                },
                                placeholder = {
                                    Text(
                                        text = "Masukkan Nama, NIP, atau Nama/NIP",
                                        fontSize = 14.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                },
                                singleLine = true,
                                isError = isError && inputNipOrName.isBlank(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = loginKeyboardActions,
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "NIP atau Nama",
                                        tint = Color(0xFF15803D)
                                    )
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF15803D),
                                    unfocusedBorderColor = Color(0xFFCBD5E1),
                                    focusedContainerColor = Color(0xFFF8FAFC),
                                    unfocusedContainerColor = Color(0xFFF8FAFC)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .formScrollPadding()
                                    .testTag("officer_nip_input")
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    // Kolom PIN (Selalu tampil di Skenario A sebagai Kolom 2, dan satu-satunya kolom di Skenario B)
                    OutlinedTextField(
                        value = inputPin,
                        onValueChange = {
                            if (it.length <= 20) {
                                inputPin = it
                                isError = false
                                errorMessage = ""
                            }
                        },
                        placeholder = {
                            Text(
                                text = "Masukkan PIN",
                                fontSize = 14.sp,
                                color = Color(0xFF94A3B8)
                            )
                        },
                        singleLine = true,
                        isError = isError,
                        visualTransformation = if (isPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = loginKeyboardActions,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Ikon PIN",
                                tint = Color(0xFF15803D)
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { isPinVisible = !isPinVisible }) {
                                Icon(
                                    imageVector = if (isPinVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (isPinVisible) "Sembunyikan PIN" else "Tampilkan PIN",
                                    tint = Color(0xFF64748B)
                                )
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF15803D),
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .formScrollPadding()
                            .testTag("officer_pin_input")
                    )

                    if (isError && errorMessage.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage,
                            color = Color(0xFFDC2626),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (syncStatusMessage.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = syncStatusMessage,
                            color = Color(0xFF15803D),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Animated Biometric Scanner Banner saat sensor aktif memindai
                    AnimatedVisibility(
                        visible = isBiometricScanning,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            BiometricScanningVisualBanner(
                                officerName = pendingBiometricLabel.ifBlank {
                                    savedOfficerDisplay.ifBlank {
                                        inputNipOrName.trim().ifBlank { "Petugas Pusdalops" }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                            )
                        }
                    }

                    // Tombol "MASUK" berwarna solid
                    Button(
                        onClick = { submitOfficerLogin() },
                        enabled = !isSubmitting && !isSyncingCloud && !isBiometricScanning,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF15803D),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("officer_login_submit_button")
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "MEMVERIFIKASI...",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        } else {
                            Text(
                                text = "MASUK",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    // Fitur Login Biometrik (Sidik Jari / Pengenalan Wajah)
                    if (isBiometricAvailable) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { launchBiometricLogin() },
                            enabled = !isSubmitting && !isSyncingCloud && !isBiometricScanning,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.5.dp, Color(0xFF15803D)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color(0xFFF0FDF4),
                                contentColor = Color(0xFF15803D)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("officer_biometric_login_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = "Login Biometrik",
                                tint = Color(0xFF15803D),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isBiometricScanning) "MEMINDAI BIOMETRIK..." else "Masuk dengan Biometrik",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    } else if (isBiometricNotEnrolled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Daftarkan sidik jari/wajah di Pengaturan HP untuk Login Biometrik",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // 4. Opsi Ganti Akun (Penting): Muncul pada Skenario B untuk menghapus saved_nip dari Secure Storage
                    // dan kembali ke tampilan Dua Kolom Input (Skenario A) secara seamless tanpa restart aplikasi.
                    AnimatedVisibility(visible = hasSavedNip) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Bukan Anda? Ganti Akun",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFDC2626),
                                textDecoration = TextDecoration.Underline,
                                modifier = Modifier
                                    .testTag("officer_switch_account_button")
                                    .clickable(enabled = !isSubmitting && !isSyncingCloud) {
                                        officerAuthManager.clearSavedNipFromSecureStorage()
                                        savedNip = ""
                                        savedName = ""
                                        inputNipOrName = ""
                                        inputPin = ""
                                        isError = false
                                        errorMessage = ""
                                        syncStatusMessage = ""
                                    }
                                    .padding(vertical = 6.dp, horizontal = 12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Tautan teks kecil "Sinkronisasi Cloud" di paling bawah layar
                    Text(
                        text = if (isSyncingCloud) "Menyinkronkan Cloud..." else "Sinkronisasi Cloud",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF64748B),
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier
                            .clickable(enabled = !isSyncingCloud && !isSubmitting) {
                                isError = false
                                errorMessage = ""
                                syncStatusMessage = ""
                                coroutineScope.launch {
                                    isSyncingCloud = true
                                    try {
                                        smartAuthRepo.harvestAndSyncAllOfficers()
                                        smartAuthRepo.pullCredentialsFromCloud { success, count ->
                                            isSyncingCloud = false
                                            syncStatusMessage = if (success) {
                                                "Sinkronisasi berhasil ($count kredensial diperbarui)"
                                            } else {
                                                "Kredensial database lokal sudah mutakhir"
                                            }
                                            Toast.makeText(
                                                context,
                                                syncStatusMessage,
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    } catch (e: Exception) {
                                        isSyncingCloud = false
                                        isError = true
                                        errorMessage = "Gagal sinkronisasi: ${e.message ?: "Periksa koneksi internet"}"
                                    }
                                }
                            }
                            .padding(vertical = 6.dp, horizontal = 12.dp)
                    )
                }
            }
        }
    }

    // Dialog Info Lanskap & Wallpaper HD (diakses dari info aplikasi setelah login)
    if (showDisasterWallpaperDialog) {
        MukomukoDisasterWallpaperDialog(
            onDismiss = { showDisasterWallpaperDialog = false }
        )
    }

    // Enlarged Fullscreen Photo Dialog (with interactive pinch-to-zoom & +/- zoom controls)
    if (previewPhotoUri != null && previewPhotoReport != null) {
        val activeRep = previewPhotoReport!!
        var zoomScale by remember(previewPhotoUri) { mutableStateOf(1f) }
        var panOffset by remember(previewPhotoUri) { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
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
                    .background(Color.Black.copy(alpha = 0.96f))
                    .testTag("fullscreen_photo_dialog"),
                color = Color.Black.copy(alpha = 0.96f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Top Bar with Zoom Controls & Close Button (48dp touch target)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = "Foto Kejadian Bencana Lapangan",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "ID: ${activeRep.id} • ${activeRep.village}, ${activeRep.district}",
                                color = Color(0xFFD1D5DB),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    zoomScale = if (zoomScale > 1.05f) 1f else 2.2f
                                    panOffset = androidx.compose.ui.geometry.Offset.Zero
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("btn_fullscreen_toggle_zoom")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color.White.copy(alpha = 0.18f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ZoomIn,
                                        contentDescription = "Perbesar atau Reset Zoom",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    previewPhotoUri = null
                                    previewPhotoReport = null
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("btn_close_fullscreen_photo")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color.White.copy(alpha = 0.22f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Tutup Layar Penuh",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Full Photo Container
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF111827))
                            .pointerInput(previewPhotoUri) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    val newScale = (zoomScale * zoom).coerceIn(1f, 4f)
                                    zoomScale = newScale
                                    panOffset = if (newScale > 1f) {
                                        panOffset + pan
                                    } else {
                                        Offset.Zero
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        val zoomContext = LocalContext.current
                        val fullPhotoModel = remember(previewPhotoUri) {
                            decodeCitizenPhotoModel(previewPhotoUri)
                        }
                        val fallbackFullBitmap = rememberCitizenReportPhotoBitmap(previewPhotoUri, activeRep.id)
                        var isZoomPhotoError by remember(previewPhotoUri) {
                            mutableStateOf(fullPhotoModel == null && fallbackFullBitmap == null)
                        }
                        if (fallbackFullBitmap != null && isZoomPhotoError) {
                            isZoomPhotoError = false
                        }
                        val zoomModifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = zoomScale,
                                scaleY = zoomScale,
                                translationX = panOffset.x,
                                translationY = panOffset.y
                            )

                        if (fallbackFullBitmap != null) {
                            Image(
                                bitmap = fallbackFullBitmap.asImageBitmap(),
                                contentDescription = "Foto Bencana Resolusi Penuh",
                                contentScale = ContentScale.Fit,
                                modifier = zoomModifier
                            )
                        } else if (isZoomPhotoError || fullPhotoModel == null) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFFE5E7EB))
                                    .testTag("fullscreen_photo_error_placeholder")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BrokenImage,
                                    contentDescription = "Foto tidak dapat dimuat",
                                    tint = Color(0xFF6B7280),
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Foto tidak dapat dimuat",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF374151)
                                )
                            }
                        } else if (fullPhotoModel is Bitmap) {
                            Image(
                                bitmap = fullPhotoModel.asImageBitmap(),
                                contentDescription = "Foto Bencana Resolusi Penuh",
                                contentScale = ContentScale.Fit,
                                modifier = zoomModifier
                            )
                        } else {
                            AsyncImage(
                                model = coil.request.ImageRequest.Builder(zoomContext)
                                    .data(fullPhotoModel)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Foto Bencana Resolusi Penuh",
                                onSuccess = { isZoomPhotoError = false },
                                onError = {
                                    if (fallbackFullBitmap == null) {
                                        isZoomPhotoError = true
                                    }
                                },
                                contentScale = ContentScale.Fit,
                                modifier = zoomModifier
                            )
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
    officerRole: String = OfficerRoles.PETUGAS_JAGA,
    onUpdateStatus: (String) -> Unit,
    onViewPhoto: (String) -> Unit,
    onDeleteReport: (() -> Unit)? = null
) {
    val context = LocalContext.current

    // Indicator Color based on Canonical Normalized Status
    val normalizedStatus = com.example.model.CitizenReportStatusHelper.normalizeStatus(report.status)
    val (statusContainer, statusContent, statusLabel) = when (normalizedStatus) {
        com.example.model.CitizenReportStatusHelper.STATUS_DIVALIDASI -> Triple(Color(0xFFDCFCE7), Color(0xFF15803D), "Divalidasi")
        com.example.model.CitizenReportStatusHelper.STATUS_SELESAI -> Triple(Color(0xFFDBEAFE), Color(0xFF2563EB), "Selesai")
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
                    // Status Sinkronisasi: "Synced" (Hijau) apabila data laporan sudah ada di server Firestore.
                    // Jangan pernah menampilkan "Menunggu Sync" untuk laporan yang sudah berhasil diambil dari server pusat.
                    val isSyncedServer = isReportSyncedWithServer(report)
                    val syncIcon = if (isSyncedServer) Icons.Default.CloudDone else Icons.Default.CloudSync
                    val syncColor = if (isSyncedServer) Color(0xFF16A34A) else Color(0xFFD97706)
                    val syncText = if (isSyncedServer) "Synced" else "Menunggu Sync"

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = syncColor.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, syncColor.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .heightIn(min = 24.dp)
                            .wrapContentWidth()
                            .testTag("sync_status_badge")
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

            // PHOTO DISPLAY AREA (Wajib selalu tampil tepat di bawah "Detail: ..." baik ada foto maupun kosong)
            Spacer(modifier = Modifier.height(8.dp))
            if (!report.photoUri.isNullOrBlank()) {
                val cardContext = LocalContext.current
                val resolvedPhotoModel = remember(report.photoUri) {
                    decodeCitizenPhotoModel(report.photoUri)
                }
                val fallbackCardBitmap = rememberCitizenReportPhotoBitmap(report.photoUri, report.id)
                var isPhotoLoadError by remember(report.photoUri) {
                    mutableStateOf(resolvedPhotoModel == null && fallbackCardBitmap == null)
                }
                if (fallbackCardBitmap != null && isPhotoLoadError) {
                    isPhotoLoadError = false
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD1D5DB)),
                    color = if (isPhotoLoadError && fallbackCardBitmap == null) Color(0xFFE5E7EB) else Color(0xFF1E293B),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .testTag("citizen_report_photo_thumbnail")
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(enabled = !isPhotoLoadError || fallbackCardBitmap != null) { onViewPhoto(report.photoUri) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isPhotoLoadError && fallbackCardBitmap == null) {
                            // Placeholder default abu-abu agar gambar rusak/gagal dimuat tidak pernah tampil glitch
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFFE5E7EB))
                                    .testTag("photo_error_placeholder")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BrokenImage,
                                    contentDescription = "Foto tidak dapat dimuat",
                                    tint = Color(0xFF6B7280),
                                    modifier = Modifier.size(38.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Foto tidak dapat dimuat",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF374151)
                                )
                            }
                        } else {
                            val activeBitmap = fallbackCardBitmap ?: (resolvedPhotoModel as? Bitmap)
                            if (activeBitmap != null) {
                                Image(
                                    bitmap = activeBitmap.asImageBitmap(),
                                    contentDescription = "Foto Bukti Laporan Bencana",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                AsyncImage(
                                    model = coil.request.ImageRequest.Builder(cardContext)
                                        .data(resolvedPhotoModel)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Foto Bukti Laporan Bencana",
                                    onSuccess = { isPhotoLoadError = false },
                                    onError = {
                                        if (fallbackCardBitmap == null) {
                                            isPhotoLoadError = true
                                        }
                                    },
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
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

                            // Tombol Zoom Interaktif (Pojok Kanan Atas - minimum 48dp touch target) -> Buka Layar Penuh
                            IconButton(
                                onClick = { onViewPhoto(report.photoUri) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(48.dp)
                                    .testTag("btn_zoom_report_photo")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(Color.Black.copy(alpha = 0.68f), CircleShape)
                                        .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ZoomIn,
                                        contentDescription = "Perbesar Foto Layar Penuh",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                if (!isPhotoLoadError) {
                    Text(
                        text = "🔍 Sentuh foto atau ikon kaca pembesar untuk mode layar penuh",
                        fontSize = 11.sp,
                        color = Color(0xFF6B7280),
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
            } else {
                // Error / Empty Photo Handling (Wajib tampil, jangan pernah kosong melompong!)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF8FAFC),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("placeholder_no_photo")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.ImageNotSupported,
                            contentDescription = "Warga tidak melampirkan foto",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Warga tidak melampirkan foto",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF475569)
                        )
                    }
                }
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

            if (report.handledByOfficerName.isNotBlank() || report.handledByOfficerNip.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFEFF6FF),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = "Petugas Penangan Aktif",
                            tint = Color(0xFF1D4ED8),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        val officerLabel = buildString {
                            append("Ditangani: ")
                            append(report.handledByOfficerName.ifBlank { "Petugas Pusdalops BPBD" })
                            if (report.handledByOfficerRole.isNotBlank()) {
                                append(" (${report.handledByOfficerRole})")
                            }
                            if (report.handledByOfficerNip.isNotBlank()) {
                                append(" • NIP ${maskNipLast6(report.handledByOfficerNip)}")
                            }
                        }
                        Text(
                            text = officerLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1E40AF),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // FEEDBACK LOOP (PELACAKAN STATUS REAL-TIME):
            // StreamBuilder Pelacakan pada dokumen spesifik (LAP-MMK-...) + UI Stepper Vertikal 4 Tahapan
            // + Sinkronisasi Warna Otomatis + Waktu Penanganan dari Timestamp Firestore
            Spacer(modifier = Modifier.height(10.dp))
            CitizenReportVerticalFeedbackStepper(
                report = report
            )

            // GOOGLE MAPS CONNECTION BUTTONS (Universal Route & Navigation)
            Spacer(modifier = Modifier.height(8.dp))
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

            // Status Management & RBAC Action Row (Multi-Tier BPBD Mukomuko)
            val activeUserRole = UserRole.fromRoleString(officerRole)
            val canValidate = isOfficerLoggedIn && UserRole.canValidateReports(officerRole)
            val canComplete = isOfficerLoggedIn && UserRole.canCompleteReports(officerRole)
            val canDelete = isOfficerLoggedIn && UserRole.canDeleteReports(officerRole) && onDeleteReport != null

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
                        text = if (isOfficerLoggedIn) {
                            "Aksi (${activeUserRole.tierCode}):"
                        } else {
                            "Status (Khusus Petugas):"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isOfficerLoggedIn) Color(activeUserRole.badgeColorHex) else Color(0xFF4B5563)
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Operasional (Diproses): Tersedia untuk semua tier petugas (Tier 1, 2, 3) atau menampilkan dialog login untuk warga
                    ReportStatusChip(
                        label = "Diproses",
                        isSelected = report.status == "Diproses",
                        color = Color(0xFFEA580C)
                    ) {
                        onUpdateStatus("Diproses")
                    }

                    // Validasi Laporan: HANYA tampil untuk Tier 1 (Manajer) & Tier 2 (Pengawas/Supervisor/Koord. Administrasi)
                    // Disembunyikan total untuk Tier 3 (Operator/Logistik/TRC/null)
                    if (canValidate) {
                        ReportStatusChip(
                            label = "Validasi Laporan",
                            isSelected = report.status == "Divalidasi",
                            color = Color(0xFF16A34A),
                            testTag = "btn_history_validate_report_${report.id}"
                        ) {
                            onUpdateStatus("Divalidasi")
                        }
                    }

                    // Selesai: HANYA tampil untuk Tier 1 & Tier 2 (disembunyikan untuk Tier 3)
                    if (canComplete) {
                        ReportStatusChip(
                            label = "Selesai",
                            isSelected = report.status == "Selesai",
                            color = Color(0xFF2563EB),
                            testTag = "btn_history_complete_report_${report.id}"
                        ) {
                            onUpdateStatus("Selesai")
                        }
                    }

                    // Hapus Laporan: HANYA tampil untuk Tier 1 (Manajer)
                    if (canDelete) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFFEE2E2),
                            border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                            modifier = Modifier
                                .heightIn(min = 40.dp)
                                .testTag("btn_history_delete_report_${report.id}")
                                .clickable { onDeleteReport?.invoke() }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Hapus Laporan",
                                    tint = Color(0xFFB91C1C),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Hapus Laporan",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB91C1C),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    softWrap = false
                                )
                            }
                        }
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
    testTag: String? = null,
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
            .let { base -> if (testTag != null) base.testTag(testTag) else base }
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
 * Menentukan apakah sebuah laporan warga sudah terkonfirmasi tersinkronisasi dengan database pusat (Firestore).
 * Laporan yang berasal dari server Firestore, membawa URL cloud (`https://`), atau sudah memiliki
 * penanganan petugas dari server WAJIB menampilkan label "Synced" (Hijau), bukan "Menunggu Sync".
 */
internal fun isReportSyncedWithServer(report: CitizenDisasterReport): Boolean {
    if (report.isSyncedToCloud) return true
    val photo = report.photoUri?.trim().orEmpty()
    val hasRemoteStorageUrl = photo.startsWith("https://", ignoreCase = true) ||
        photo.startsWith("http://", ignoreCase = true)
    val hasServerOfficerHandling = report.handledByOfficerName.isNotBlank() ||
        report.handledByOfficerNip.isNotBlank()
    return hasRemoteStorageUrl || hasServerOfficerHandling
}

/**
 * Mendekode sumber gambar laporan warga:
 * - Jika berupa string Base64 (dengan prefix `data:image/...;base64,` maupun raw Base64),
 *   didekode menggunakan `Base64.decode` menjadi `Bitmap`.
 * - Jika berupa URI `file://` atau path file lokal, memverifikasi keberadaan file.
 * - Jika berupa URL `http(s)://`, memeriksa cache `Bitmap` / backup Data URI terlebih dahulu,
 *   atau meneruskan URL HTTPS ke Coil `AsyncImage`.
 */
internal fun decodeCitizenPhotoModel(photoSource: String?): Any? {
    if (photoSource.isNullOrBlank()) return null
    val trimmed = photoSource.trim()

    // 1. Cek format Data URI Base64 (mis. data:image/jpeg;base64,...) -> langsung dekode ke Bitmap
    if (trimmed.startsWith("data:image", ignoreCase = true) || trimmed.contains("base64,", ignoreCase = true)) {
        return com.example.util.ImageUtils.decodePhotoSourceToBitmap(trimmed)
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
            return com.example.util.ImageUtils.decodePhotoSourceToBitmap(trimmed)
        }
        val normalizedUrl = if (trimmed.startsWith("http://", ignoreCase = true)) {
            "https://" + trimmed.substring(7)
        } else {
            trimmed
        }
        if (normalizedUrl.startsWith("https://", ignoreCase = true)) {
            com.example.data.api.SupabaseClient.getCachedPhotoBitmap(normalizedUrl)?.let { return it }
            com.example.util.ImageUtils.getInlineDataUriForUrl(normalizedUrl)?.let { inlineDataUri ->
                com.example.util.ImageUtils.decodePhotoSourceToBitmap(inlineDataUri)?.let { bmp ->
                    com.example.data.api.SupabaseClient.cachePhotoBitmap(normalizedUrl, bmp)
                    return bmp
                }
            }
        }
        return normalizedUrl
    }

    // 3. Cek path file absolut lokal (/data/..., /storage/...)
    if (trimmed.startsWith("/")) {
        return com.example.util.ImageUtils.decodePhotoSourceToBitmap(trimmed)
    }

    // 4. Fallback: raw Base64 string tanpa prefix
    return com.example.util.ImageUtils.decodePhotoSourceToBitmap(trimmed)
}

/**
 * Mengambil Bitmap foto bukti laporan warga secara reaktif:
 * - Mendukung Data URI Base64, file lokal, serta mengunduh secara terautentikasi dari Supabase Storage
 *   (baik bucket `reports` berstatus Public maupun Authenticated/Private) agar foto laporan warga
 *   selalu tampil di Halaman Petugas & Laporan Warga.
 */
@Composable
internal fun rememberCitizenReportPhotoBitmap(
    photoSource: String?,
    reportId: String = ""
): Bitmap? {
    val trimmed = photoSource?.trim().orEmpty()
    var resolvedBitmap by remember(trimmed, reportId) {
        val initialModel = decodeCitizenPhotoModel(trimmed)
        val fromModel = initialModel as? Bitmap
        val fromReportCache = if (fromModel == null && reportId.isNotBlank()) {
            com.example.util.ImageUtils.getInlineDataUriForReport(reportId)?.let {
                com.example.util.ImageUtils.decodePhotoSourceToBitmap(it)
            }
        } else {
            null
        }
        mutableStateOf(fromModel ?: fromReportCache)
    }

    LaunchedEffect(trimmed, reportId) {
        if (resolvedBitmap == null &&
            (trimmed.startsWith("https://", ignoreCase = true) || trimmed.startsWith("http://", ignoreCase = true))
        ) {
            val downloaded = com.example.data.api.SupabaseClient.downloadReportPhotoBitmap(trimmed)
            if (downloaded != null) {
                resolvedBitmap = downloaded
            } else if (reportId.isNotBlank()) {
                val backupUri = com.example.util.ImageUtils.getInlineDataUriForReport(reportId)
                if (!backupUri.isNullOrBlank()) {
                    resolvedBitmap = com.example.util.ImageUtils.decodePhotoSourceToBitmap(backupUri)
                }
            }
        }
    }

    return resolvedBitmap
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


