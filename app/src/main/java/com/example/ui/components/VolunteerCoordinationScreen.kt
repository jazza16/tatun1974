package com.example.ui.components

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.local.MukomukoVolunteerPoskoCatalog
import com.example.data.local.VolunteerEntity
import com.example.data.local.VolunteerPrivacyHelper
import com.example.data.local.VolunteerSkills
import com.example.data.local.VolunteerStatus
import com.example.data.repository.VolunteerRegistrationResult
import com.example.data.repository.VolunteerRepository
import com.example.util.VolunteerAssignmentPdfGenerator
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.hypot

/**
 * Modul "Pendataan & Koordinasi Relawan" Pusdalops-PB BPBD Kabupaten Mukomuko.
 *
 * Fitur Lengkap:
 * 1. Tab "Dashboard Relawan" (Untuk Pusdalops & Publik):
 *    - 4 Kartu Statistik: Total Terdaftar, Terverifikasi, Sedang Bertugas, Butuh Bantuan.
 *    - Peta Sebaran Relawan & Posko (mirip Peta Karhutla) di 15 Kecamatan Kabupaten Mukomuko.
 *    - Filter Dropdown Keahlian & Status.
 *    - Klik item relawan untuk membuka Halaman Detail Relawan (lengkap dengan tombol
 *      [ CHECK-IN POSKO ], [ CHECK-OUT POSKO ], status kehadiran real-time, dan "Unduh Surat Tugas").
 * 2. Tab "Registrasi Relawan" (Form Input Offline-First SQLite -> Auto-Sync Firestore):
 *    - Validasi NIK (Anti Data Ganda - Primary Key 16 digit): Jika NIK sudah terdaftar di database/dummy data,
 *      menampilkan alert error: "NIK sudah terdaftar, silakan hubungi admin."
 * 3. Tab "Verifikasi & Penugasan" (Khusus Admin/Pusdalops):
 *    - Tombol Aksi per Item: "Verifikasi", "Tolak", "Tugaskan".
 *    - Modal Penugasan: Saat admin memilih lokasi posko & shift lalu menekan "Simpan Penugasan",
 *      sistem otomatis membuat file PDF "SURAT TUGAS RELAWAN" dan menyediakan tombol "Unduh Surat Tugas".
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VolunteerCoordinationScreen(
    isOfficerLoggedIn: Boolean,
    officerName: String,
    officerNip: String,
    officerRole: String,
    onClose: () -> Unit,
    onRequestOfficerLogin: (() -> Unit)? = null,
    initialTab: Int = 0
) {
    BackHandler { onClose() }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val repository = remember { VolunteerRepository.getInstance(context) }

    val volunteers by repository.allVolunteers.collectAsStateWithLifecycle(initialValue = emptyList())
    val isOnline by repository.isOnline.collectAsStateWithLifecycle()
    val isSyncing by repository.isSyncing.collectAsStateWithLifecycle()
    val lastSyncStatus by repository.lastSyncStatus.collectAsStateWithLifecycle()

    var selectedTab by rememberSaveable { mutableIntStateOf(initialTab.coerceIn(0, 2)) }

    // Jika Admin sudah login, default tampilan membuka NIK/No. HP penuh.
    var simulatePublicPrivacyMask by rememberSaveable { mutableStateOf(false) }
    val isEffectiveAdminView = isOfficerLoggedIn && !simulatePublicPrivacyMask

    // State Modal Penugasan Relawan
    var assigningVolunteer by remember { mutableStateOf<VolunteerEntity?>(null) }

    // State Halaman / Modal Detail Relawan (untuk Check-in / Check-out Posko & Unduh Surat Tugas)
    var selectedDetailVolunteerNik by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedDetailVolunteer = remember(volunteers, selectedDetailVolunteerNik) {
        volunteers.firstOrNull { it.nik == selectedDetailVolunteerNik || it.id == selectedDetailVolunteerNik }
    }

    // State Dialog Pratinjau & Unduh Surat Tugas Digital (PDF)
    var previewSuratTugasVolunteer by remember { mutableStateOf<VolunteerEntity?>(null) }

    // State Alert Error NIK Ganda ("NIK sudah terdaftar, silakan hubungi admin.")
    var duplicateNikAlertMessage by remember { mutableStateOf<String?>(null) }

    // Statistik Utama (4 Kartu KPI)
    val totalTerdaftar = volunteers.size
    val totalTerverifikasi = volunteers.count {
        val st = it.statusEnum()
        st == VolunteerStatus.VERIFIED || st == VolunteerStatus.ASSIGNED || st == VolunteerStatus.DONE
    }
    val totalSedangBertugas = volunteers.count {
        it.statusEnum() == VolunteerStatus.ASSIGNED && it.isActive
    }
    val totalButuhBantuan = volunteers.count { it.needsAssistance }
    val pendingVerificationCount = volunteers.count { it.statusEnum() == VolunteerStatus.PENDING }
    val unsyncedLocalCount = volunteers.count { !it.isSyncedToCloud }

    val handleCheckInPosko: (VolunteerEntity) -> Unit = { volunteer ->
        coroutineScope.launch {
            val now = System.currentTimeMillis()
            val updated = repository.checkInPosko(volunteer.nik, now)
            val timeStr = VolunteerPrivacyHelper.formatWibTimeOnly(now)
            Toast.makeText(
                context,
                "CHECK-IN POSKO Berhasil: ${volunteer.fullName} sedang bertugas di ${updated?.effectivePoskoName() ?: volunteer.effectivePoskoName()} sejak $timeStr",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    val handleCheckOutPosko: (VolunteerEntity) -> Unit = { volunteer ->
        coroutineScope.launch {
            val now = System.currentTimeMillis()
            val updated = repository.checkOutPosko(volunteer.nik, now)
            val timeStr = VolunteerPrivacyHelper.formatWibTimeOnly(now)
            Toast.makeText(
                context,
                "CHECK-OUT POSKO Tercatat: ${volunteer.fullName} keluar dari ${updated?.effectivePoskoName() ?: volunteer.effectivePoskoName()} pada $timeStr",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    val handleDownloadSuratTugas: (VolunteerEntity) -> Unit = { volunteer ->
        coroutineScope.launch {
            repository.ensureAndGetSuratTugasPdf(
                nikOrId = volunteer.nik,
                officerName = officerName.ifBlank { volunteer.verifiedByOfficer.ifBlank { "HITATUN ARAZAK" } }
            )
            VolunteerAssignmentPdfGenerator.downloadAndShareAssignmentPdf(
                context = context,
                volunteer = volunteer,
                officerName = officerName.ifBlank { volunteer.verifiedByOfficer.ifBlank { "HITATUN ARAZAK" } }
            )
            previewSuratTugasVolunteer = volunteer
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .safeDrawingPadding()
            .testTag("volunteer_coordination_screen"),
        containerColor = Color(0xFFF1F5F9),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B))
                        )
                    )
            ) {
                // Baris Judul Command Center (Ringkas & Efisien)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("btn_close_volunteer_module")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Tutup Modul Relawan",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "PENDATAAN & KOORDINASI RELAWAN",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isOfficerLoggedIn) {
                                "Pusdalops-PB • Admin: ${officerName.ifBlank { "Petugas" }} (Akses Penuh)"
                            } else {
                                "Pusdalops-PB • Mode Publik (NIK Disamarkan 1234********)"
                            },
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    if (isOfficerLoggedIn) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (simulatePublicPrivacyMask) Color(0xFFF59E0B) else Color(0xFF334155),
                            modifier = Modifier
                                .clickable { simulatePublicPrivacyMask = !simulatePublicPrivacyMask }
                                .testTag("btn_toggle_privacy_mask_preview")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (simulatePublicPrivacyMask) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Uji Samaran NIK",
                                    tint = if (simulatePublicPrivacyMask) Color.Black else Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = if (simulatePublicPrivacyMask) "Publik" else "Admin",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (simulatePublicPrivacyMask) Color.Black else Color.White,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    // Tombol Sinkronisasi Offline SQLite <-> Cloud Firestore
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isOnline) Color(0xFF065F46) else Color(0xFF78350F),
                        border = BorderStroke(
                            1.dp,
                            if (isOnline) Color(0xFF10B981) else Color(0xFFF59E0B)
                        ),
                        modifier = Modifier
                            .clickable(enabled = !isSyncing) {
                                coroutineScope.launch {
                                    val count = repository.syncPendingOfflineVolunteersToFirestore()
                                    Toast.makeText(
                                        context,
                                        if (count > 0) "Berhasil menyinkronkan $count data relawan lokal ke Cloud Firestore"
                                        else "Database SQLite & Cloud Firestore sudah sinkron",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                            .testTag("btn_sync_volunteers_cloud")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            } else {
                                Icon(
                                    imageVector = if (isOnline) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                    contentDescription = "Status Sinkronisasi",
                                    tint = if (isOnline) Color(0xFF34D399) else Color(0xFFFBBF24),
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (unsyncedLocalCount > 0) "SQLite ($unsyncedLocalCount)" else "Sinkron",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                // Strip Status Offline-First SQLite + Kontrol Privasi NIK (1 Baris Ramping)
                Surface(
                    color = Color(0xFF0F172A).copy(alpha = 0.65f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isEffectiveAdminView) Icons.Default.VerifiedUser else Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (isEffectiveAdminView) Color(0xFF38BDF8) else Color(0xFFFBBF24),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (isEffectiveAdminView) {
                                "Primary Key: NIK 16 Digit • Admin Login (NIK & No. HP Utuh) • $lastSyncStatus"
                            } else {
                                "Primary Key: NIK 16 Digit • Mode Publik (NIK Disamarkan 1234********) • $lastSyncStatus"
                            },
                            fontSize = 9.5.sp,
                            color = Color(0xFFE2E8F0),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // TabBar Utama (3 Tab Tunggal yang Rapi Tanpa Duplikasi Bottom Navigation)
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFF1E293B),
                    contentColor = Color.White,
                    indicator = { tabPositions ->
                        if (selectedTab < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                height = 3.dp,
                                color = Color(0xFFF97316)
                            )
                        }
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        modifier = Modifier
                            .heightIn(min = 42.dp)
                            .testTag("tab_volunteer_dashboard"),
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Dashboard,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = if (selectedTab == 0) Color(0xFFF97316) else Color(0xFF94A3B8)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Dashboard",
                                    fontSize = 11.sp,
                                    fontWeight = if (selectedTab == 0) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (selectedTab == 0) Color.White else Color(0xFF94A3B8),
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    )

                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        modifier = Modifier
                            .heightIn(min = 42.dp)
                            .testTag("tab_volunteer_registration"),
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.PersonAdd,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = if (selectedTab == 1) Color(0xFFF97316) else Color(0xFF94A3B8)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Registrasi",
                                    fontSize = 11.sp,
                                    fontWeight = if (selectedTab == 1) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (selectedTab == 1) Color.White else Color(0xFF94A3B8),
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    )

                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        modifier = Modifier
                            .heightIn(min = 42.dp)
                            .testTag("tab_volunteer_verification"),
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.HowToReg,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = if (selectedTab == 2) Color(0xFFF97316) else Color(0xFF94A3B8)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Verifikasi",
                                    fontSize = 11.sp,
                                    fontWeight = if (selectedTab == 2) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (selectedTab == 2) Color.White else Color(0xFF94A3B8),
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (pendingVerificationCount > 0) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Badge(
                                        containerColor = Color(0xFFEF4444),
                                        contentColor = Color.White
                                    ) {
                                        Text(
                                            text = "$pendingVerificationCount",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> VolunteerDashboardTabContent(
                    volunteers = volunteers,
                    totalTerdaftar = totalTerdaftar,
                    totalTerverifikasi = totalTerverifikasi,
                    totalSedangBertugas = totalSedangBertugas,
                    totalButuhBantuan = totalButuhBantuan,
                    isAdminView = isEffectiveAdminView,
                    onNavigateToRegister = { selectedTab = 1 },
                    onNavigateToVerification = { selectedTab = 2 },
                    onOpenDetailVolunteer = { volunteer ->
                        selectedDetailVolunteerNik = volunteer.nik
                    },
                    onCheckInPosko = handleCheckInPosko,
                    onCheckOutPosko = handleCheckOutPosko,
                    onDownloadSuratTugas = handleDownloadSuratTugas
                )

                1 -> VolunteerRegistrationTabContent(
                    context = context,
                    existingVolunteers = volunteers,
                    isOnline = isOnline,
                    unsyncedLocalCount = unsyncedLocalCount,
                    onDuplicateNikDetected = { alertMsg ->
                        duplicateNikAlertMessage = alertMsg
                    },
                    onSubmitRegistration = { fullName, nik, phone, org, skills, district, selfieUri, certUri ->
                        coroutineScope.launch {
                            when (
                                val result = repository.registerVolunteerValidated(
                                    fullName = fullName,
                                    nik = nik,
                                    phoneNumber = phone,
                                    organization = org,
                                    selectedSkills = skills,
                                    homeDistrict = district,
                                    selfiePhotoUri = selfieUri,
                                    certificatePhotoUri = certUri
                                )
                            ) {
                                is VolunteerRegistrationResult.Success -> {
                                    Toast.makeText(
                                        context,
                                        "Pendaftaran ${result.volunteer.fullName} (NIK PK: ${result.volunteer.nik}) berhasil disimpan di SQLite!",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    selectedTab = 0
                                }
                                is VolunteerRegistrationResult.DuplicateNik -> {
                                    duplicateNikAlertMessage = result.message
                                }
                                is VolunteerRegistrationResult.ValidationError -> {
                                    Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                )

                2 -> VolunteerVerificationTabContent(
                    volunteers = volunteers,
                    isOfficerLoggedIn = isOfficerLoggedIn,
                    isEffectiveAdminView = isEffectiveAdminView,
                    officerName = officerName,
                    onRequestOfficerLogin = onRequestOfficerLogin,
                    onDisablePublicSimulation = { simulatePublicPrivacyMask = false },
                    onVerifyVolunteer = { volunteer ->
                        coroutineScope.launch {
                            repository.verifyVolunteer(volunteer.nik, officerName)
                            Toast.makeText(
                                context,
                                "Relawan ${volunteer.fullName} berhasil diverifikasi (Verified)",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    onRejectVolunteer = { volunteer ->
                        coroutineScope.launch {
                            repository.rejectVolunteer(volunteer.nik, officerName)
                            Toast.makeText(
                                context,
                                "Pendaftaran ${volunteer.fullName} ditolak / dinonaktifkan",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    onOpenAssignModal = { volunteer ->
                        assigningVolunteer = volunteer
                    },
                    onOpenDetailVolunteer = { volunteer ->
                        selectedDetailVolunteerNik = volunteer.nik
                    },
                    onDownloadSuratTugas = handleDownloadSuratTugas,
                    onCheckInPosko = handleCheckInPosko,
                    onCheckOutPosko = handleCheckOutPosko,
                    onMarkAssignmentDone = { volunteer ->
                        coroutineScope.launch {
                            repository.markAssignmentDone(volunteer.nik, officerName)
                            Toast.makeText(
                                context,
                                "Penugasan ${volunteer.fullName} ditandai Selesai (Done)",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    onToggleNeedsAssistance = { volunteer ->
                        coroutineScope.launch {
                            val nextFlag = !volunteer.needsAssistance
                            repository.toggleNeedsAssistance(
                                nikOrId = volunteer.nik,
                                needsAssistance = nextFlag,
                                note = if (nextFlag) {
                                    "Darurat Posko ${volunteer.effectivePoskoDistrict()}: Butuh dukungan logistik/evakuasi segera"
                                } else {
                                    ""
                                }
                            )
                            Toast.makeText(
                                context,
                                if (nextFlag) "Status BUTUH BANTUAN diaktifkan untuk ${volunteer.fullName}"
                                else "Status darurat ${volunteer.fullName} telah diselesaikan",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
            }
        }
    }

    // 1. Alert Error Validasi NIK Ganda ("NIK sudah terdaftar, silakan hubungi admin.")
    if (duplicateNikAlertMessage != null) {
        AlertDialog(
            onDismissRequest = { duplicateNikAlertMessage = null },
            modifier = Modifier.testTag("alert_duplicate_nik"),
            icon = {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Validasi NIK (Anti Data Ganda)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF991B1B)
                )
            },
            text = {
                Text(
                    text = duplicateNikAlertMessage ?: VolunteerPrivacyHelper.DUPLICATE_NIK_ALERT_MESSAGE,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
            },
            confirmButton = {
                Button(
                    onClick = { duplicateNikAlertMessage = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    modifier = Modifier.testTag("btn_dismiss_duplicate_nik_alert")
                ) {
                    Text("Mengerti", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // 2. Modal Penugasan Relawan ("Simpan Penugasan" otomatis men-generate PDF Surat Tugas Digital)
    if (assigningVolunteer != null) {
        val targetVolunteer = assigningVolunteer!!
        VolunteerAssignmentModalDialog(
            volunteer = targetVolunteer,
            onDismiss = { assigningVolunteer = null },
            onConfirmAssign = { selectedDistrict, selectedPoskoName, selectedShift ->
                coroutineScope.launch {
                    val pdfFile = repository.assignVolunteerToPosko(
                        volunteerId = targetVolunteer.nik,
                        district = selectedDistrict,
                        poskoName = selectedPoskoName,
                        shift = selectedShift,
                        officerName = officerName
                    )
                    val letterNo = VolunteerAssignmentPdfGenerator.buildLetterNumber(targetVolunteer.nik)
                    val updatedVolunteer = targetVolunteer.copy(
                        assignedDistrict = selectedDistrict,
                        assignedPoskoName = selectedPoskoName,
                        assignedShift = selectedShift,
                        status = VolunteerStatus.ASSIGNED.code,
                        suratTugasNumber = letterNo,
                        suratTugasPdfPath = pdfFile?.absolutePath,
                        verifiedByOfficer = officerName.ifBlank { "HITATUN ARAZAK" }
                    )
                    Toast.makeText(
                        context,
                        "Penugasan disimpan & PDF SURAT TUGAS RELAWAN otomatis dibuat!",
                        Toast.LENGTH_LONG
                    ).show()
                    assigningVolunteer = null
                    previewSuratTugasVolunteer = updatedVolunteer
                }
            }
        )
    }

    // 3. Halaman / Modal Detail Relawan (Tombol Besar [ CHECK-IN POSKO ] Hijau & [ CHECK-OUT POSKO ] Merah + Unduh Surat Tugas)
    if (selectedDetailVolunteer != null) {
        VolunteerDetailModalDialog(
            volunteer = selectedDetailVolunteer,
            isAdminView = isEffectiveAdminView,
            onDismiss = { selectedDetailVolunteerNik = null },
            onCheckInPosko = { handleCheckInPosko(selectedDetailVolunteer) },
            onCheckOutPosko = { handleCheckOutPosko(selectedDetailVolunteer) },
            onDownloadSuratTugas = { handleDownloadSuratTugas(selectedDetailVolunteer) }
        )
    }

    // 4. Dialog Pratinjau & Unduh Surat Tugas Digital (PDF)
    if (previewSuratTugasVolunteer != null) {
        val target = previewSuratTugasVolunteer!!
        SuratTugasDigitalPreviewDialog(
            volunteer = target,
            officerName = officerName.ifBlank { target.verifiedByOfficer.ifBlank { "HITATUN ARAZAK" } },
            onDismiss = { previewSuratTugasVolunteer = null },
            onDownloadPdfFile = {
                VolunteerAssignmentPdfGenerator.downloadAndShareAssignmentPdf(
                    context = context,
                    volunteer = target,
                    officerName = officerName.ifBlank { target.verifiedByOfficer.ifBlank { "HITATUN ARAZAK" } }
                )
            }
        )
    }
}

/**
 * TAB 1: DASHBOARD RELAWAN (Untuk Pusdalops)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VolunteerDashboardTabContent(
    volunteers: List<VolunteerEntity>,
    totalTerdaftar: Int,
    totalTerverifikasi: Int,
    totalSedangBertugas: Int,
    totalButuhBantuan: Int,
    isAdminView: Boolean,
    onNavigateToRegister: () -> Unit,
    onNavigateToVerification: () -> Unit,
    onOpenDetailVolunteer: (VolunteerEntity) -> Unit,
    onCheckInPosko: (VolunteerEntity) -> Unit,
    onCheckOutPosko: (VolunteerEntity) -> Unit,
    onDownloadSuratTugas: (VolunteerEntity) -> Unit
) {
    var selectedSkillFilter by rememberSaveable { mutableStateOf("Semua") }
    var selectedStatusFilter by rememberSaveable { mutableStateOf("Semua") }
    var selectedDistrictOnMap by rememberSaveable { mutableStateOf("Kota Mukomuko") }

    var skillDropdownExpanded by remember { mutableStateOf(false) }
    var statusDropdownExpanded by remember { mutableStateOf(false) }

    val statusFilterOptions = remember {
        listOf(
            "Semua",
            "Aktif",
            "Non-aktif",
            "Pending",
            "Verified",
            "Assigned",
            "Done"
        )
    }

    val filteredVolunteers = remember(volunteers, selectedSkillFilter, selectedStatusFilter) {
        volunteers.filter { v ->
            val matchesSkill = VolunteerSkills.matchesSkillFilter(v.skillsCsv, selectedSkillFilter)
            val matchesStatus = when (selectedStatusFilter) {
                "Semua" -> true
                "Aktif" -> v.isActive && v.statusEnum() != VolunteerStatus.REJECTED
                "Non-aktif" -> !v.isActive || v.statusEnum() == VolunteerStatus.REJECTED
                else -> v.status.equals(selectedStatusFilter, ignoreCase = true)
            }
            matchesSkill && matchesStatus
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("volunteer_dashboard_list"),
        contentPadding = PaddingValues(start = 12.dp, top = 10.dp, end = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. 4 Kartu Statistik Utama (KPI Cards - Ringkas & Proporsional)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VolunteerStatKpiCard(
                        title = "Total Terdaftar",
                        count = totalTerdaftar,
                        subtitle = "Primary Key NIK Unik",
                        accentColor = Color(0xFF0284C7),
                        bgColor = Color(0xFFE0F2FE),
                        testTag = "kpi_total_terdaftar",
                        modifier = Modifier.weight(1f)
                    )
                    VolunteerStatKpiCard(
                        title = "Terverifikasi",
                        count = totalTerverifikasi,
                        subtitle = "Personel Valid Pusdalops",
                        accentColor = Color(0xFF15803D),
                        bgColor = Color(0xFFDCFCE7),
                        testTag = "kpi_terverifikasi",
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VolunteerStatKpiCard(
                        title = "Sedang Bertugas",
                        count = totalSedangBertugas,
                        subtitle = "Aktif di Titik Posko",
                        accentColor = Color(0xFFEA580C),
                        bgColor = Color(0xFFFFEDD5),
                        testTag = "kpi_sedang_bertugas",
                        modifier = Modifier.weight(1f)
                    )
                    VolunteerStatKpiCard(
                        title = "Butuh Bantuan",
                        count = totalButuhBantuan,
                        subtitle = if (totalButuhBantuan > 0) "Darurat Posko Lapangan!" else "Situasi Posko Terkendali",
                        accentColor = Color(0xFFDC2626),
                        bgColor = Color(0xFFFEE2E2),
                        testTag = "kpi_butuh_bantuan",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 2. Panel Keselarasan Roadmap SIPAGAR KITA & Pemerataan Penempatan Personil Pusdalops
        item {
            VolunteerRoadmapAlignmentCard(
                volunteers = volunteers,
                onSelectSkillFilter = { skill ->
                    selectedSkillFilter = skill
                },
                onNavigateToVerification = onNavigateToVerification
            )
        }

        // 3. Peta Sebaran Relawan & Titik Posko 15 Kecamatan (Mirip Peta Karhutla + TES/TEA Roadmap)
        item {
            VolunteerPoskoDistributionMapCard(
                volunteers = volunteers,
                selectedDistrict = selectedDistrictOnMap,
                onSelectDistrict = { selectedDistrictOnMap = it }
            )
        }

        // 4. Filter Dropdown Berdasarkan Keahlian & Status (Aktif/Non-aktif)
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = null,
                                tint = Color(0xFFEA580C),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Filter Sebaran Keahlian & Status Relawan",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (selectedSkillFilter != "Semua" || selectedStatusFilter != "Semua") {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Reset Filter",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626),
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.clickable {
                                    selectedSkillFilter = "Semua"
                                    selectedStatusFilter = "Semua"
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Dropdown Filter Keahlian
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { skillDropdownExpanded = true },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 44.dp)
                                    .testTag("dropdown_filter_skill")
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.Start
                                ) {
                                    Text(
                                        text = "Keahlian",
                                        fontSize = 9.sp,
                                        color = Color(0xFF64748B),
                                        maxLines = 1
                                    )
                                    Text(
                                        text = selectedSkillFilter,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = skillDropdownExpanded,
                                onDismissRequest = { skillDropdownExpanded = false }
                            ) {
                                VolunteerSkills.FILTER_SKILLS.forEach { skillOption ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = skillOption,
                                                fontSize = 13.sp,
                                                fontWeight = if (skillOption == selectedSkillFilter) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            selectedSkillFilter = skillOption
                                            skillDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Dropdown Filter Status (Aktif / Non-aktif / Siklus Status)
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { statusDropdownExpanded = true },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 44.dp)
                                    .testTag("dropdown_filter_status")
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.Start
                                ) {
                                    Text(
                                        text = "Status Relawan",
                                        fontSize = 9.sp,
                                        color = Color(0xFF64748B),
                                        maxLines = 1
                                    )
                                    Text(
                                        text = selectedStatusFilter,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = statusDropdownExpanded,
                                onDismissRequest = { statusDropdownExpanded = false }
                            ) {
                                statusFilterOptions.forEach { statusOption ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = statusOption,
                                                fontSize = 13.sp,
                                                fontWeight = if (statusOption == selectedStatusFilter) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            selectedStatusFilter = statusOption
                                            statusDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Chip Filter Cepat Keahlian
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        VolunteerSkills.FILTER_SKILLS.forEach { skillChip ->
                            val isSelected = selectedSkillFilter.equals(skillChip, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedSkillFilter = skillChip },
                                label = {
                                    Text(
                                        text = skillChip,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFEA580C),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }
        }

        // 5. Header Daftar Relawan & Info Privasi NIK
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Direktori Personel Relawan (${filteredVolunteers.size} Orang)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Ketuk relawan untuk Check-in / Check-out Posko & Unduh Surat Tugas PDF",
                        fontSize = 10.5.sp,
                        color = Color(0xFF475569),
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onNavigateToRegister,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.heightIn(min = 36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Daftar Baru",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        // 6. Kartu Item Relawan (dengan Check-in / Check-out Posko & Unduh Surat Tugas)
        items(filteredVolunteers, key = { it.nik }) { volunteer ->
            VolunteerDirectoryCard(
                volunteer = volunteer,
                isAdminView = isAdminView,
                onOpenDetailClick = { onOpenDetailVolunteer(volunteer) },
                onCheckInPosko = { onCheckInPosko(volunteer) },
                onCheckOutPosko = { onCheckOutPosko(volunteer) },
                onDownloadSuratTugas = { onDownloadSuratTugas(volunteer) }
            )
        }
    }
}

@Composable
private fun VolunteerStatKpiCard(
    title: String,
    count: Int,
    subtitle: String,
    accentColor: Color,
    bgColor: Color,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.28f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.testTag(testTag)
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF475569),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = bgColor
                ) {
                    Text(
                        text = if (count > 0 && title == "Butuh Bantuan") "SIAGA" else "LIVE",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = accentColor,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "$count",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = accentColor,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = subtitle,
                    fontSize = 9.5.sp,
                    color = Color(0xFF64748B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = 2.dp)
                )
            }
        }
    }
}

/**
 * Peta Sebaran Relawan & Posko Taktis 15 Kecamatan Kabupaten Mukomuko (Mirip Peta Karhutla).
 */
@Composable
private fun VolunteerPoskoDistributionMapCard(
    volunteers: List<VolunteerEntity>,
    selectedDistrict: String,
    onSelectDistrict: (String) -> Unit
) {
    val districtCounts = remember(volunteers) {
        MukomukoVolunteerPoskoCatalog.POSKO_LIST.associate { posko ->
            val count = volunteers.count {
                it.effectivePoskoDistrict().equals(posko.districtName, ignoreCase = true) &&
                    it.statusEnum() != VolunteerStatus.REJECTED
            }
            posko.districtName to count
        }
    }

    val districtEmergencyMap = remember(volunteers) {
        MukomukoVolunteerPoskoCatalog.POSKO_LIST.associate { posko ->
            val hasEmergency = volunteers.any {
                it.effectivePoskoDistrict().equals(posko.districtName, ignoreCase = true) &&
                    it.needsAssistance
            }
            posko.districtName to hasEmergency
        }
    }

    val selectedPoskoPoint = remember(selectedDistrict) {
        MukomukoVolunteerPoskoCatalog.POSKO_LIST.firstOrNull {
            it.districtName.equals(selectedDistrict, ignoreCase = true)
        } ?: MukomukoVolunteerPoskoCatalog.POSKO_LIST.first()
    }

    val selectedPoskoVolunteers = remember(volunteers, selectedPoskoPoint) {
        volunteers.filter {
            it.effectivePoskoDistrict().equals(selectedPoskoPoint.districtName, ignoreCase = true) &&
                it.statusEnum() != VolunteerStatus.REJECTED
        }
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = BorderStroke(1.dp, Color(0xFF334155)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("volunteer_posko_map_card")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "PETA SEBARAN POSKO & RELAWAN (15 KECAMATAN)",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Ketuk titik Posko untuk melihat jumlah personel & keahlian di tiap kecamatan",
                        fontSize = 9.5.sp,
                        color = Color(0xFF94A3B8),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF091326))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures { tapOffset ->
                                val nearest = MukomukoVolunteerPoskoCatalog.POSKO_LIST.minByOrNull { pt ->
                                    val px = pt.mapNormalizedX * size.width
                                    val py = pt.mapNormalizedY * size.height
                                    hypot((tapOffset.x - px).toDouble(), (tapOffset.y - py).toDouble())
                                }
                                if (nearest != null) {
                                    onSelectDistrict(nearest.districtName)
                                }
                            }
                        }
                ) {
                    val w = size.width
                    val h = size.height

                    val gridColor = Color(0xFF1E293B)
                    for (i in 1..4) {
                        val y = h * (i / 5f)
                        drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
                    }
                    for (i in 1..6) {
                        val x = w * (i / 7f)
                        drawLine(gridColor, Offset(x, 0f), Offset(x, h), strokeWidth = 1f)
                    }

                    val coastPath = Path().apply {
                        moveTo(w * 0.14f, h * 0.10f)
                        quadraticTo(w * 0.30f, h * 0.42f, w * 0.50f, h * 0.62f)
                        quadraticTo(w * 0.65f, h * 0.78f, w * 0.80f, h * 0.92f)
                    }
                    drawPath(
                        path = coastPath,
                        color = Color(0xFF0284C7).copy(alpha = 0.55f),
                        style = Stroke(
                            width = 3.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                        )
                    )

                    MukomukoVolunteerPoskoCatalog.POSKO_LIST.forEach { posko ->
                        val cx = posko.mapNormalizedX * w
                        val cy = posko.mapNormalizedY * h
                        val count = districtCounts[posko.districtName] ?: 0
                        val hasEmergency = districtEmergencyMap[posko.districtName] == true
                        val isSelected = posko.districtName.equals(selectedPoskoPoint.districtName, ignoreCase = true)

                        val markerColor = when {
                            hasEmergency -> Color(0xFFEF4444)
                            count > 0 -> Color(0xFF22C55E)
                            else -> Color(0xFF64748B)
                        }

                        if (isSelected || hasEmergency || count > 0) {
                            drawCircle(
                                color = markerColor.copy(alpha = if (isSelected) 0.35f else 0.20f),
                                radius = if (isSelected) 24f else 17f,
                                center = Offset(cx, cy)
                            )
                        }

                        if (isSelected) {
                            drawCircle(
                                color = Color(0xFFFBBF24),
                                radius = 14f,
                                center = Offset(cx, cy),
                                style = Stroke(width = 2.5f)
                            )
                        }

                        drawCircle(
                            color = markerColor,
                            radius = if (count > 0) 9.5f else 6f,
                            center = Offset(cx, cy)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF0F172A).copy(alpha = 0.88f),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LegendDot(color = Color(0xFF22C55E), label = "Posko Aktif")
                        LegendDot(color = Color(0xFFEF4444), label = "Darurat")
                        LegendDot(color = Color(0xFF64748B), label = "Siaga (0)")
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF1E293B).copy(alpha = 0.9f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                ) {
                    Text(
                        text = "${selectedPoskoPoint.districtName} (${selectedPoskoVolunteers.size} Relawan)",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFFBBF24),
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MukomukoVolunteerPoskoCatalog.POSKO_LIST.forEach { posko ->
                    val count = districtCounts[posko.districtName] ?: 0
                    val hasEmergency = districtEmergencyMap[posko.districtName] == true
                    val isSelected = posko.districtName.equals(selectedPoskoPoint.districtName, ignoreCase = true)

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when {
                            isSelected -> Color(0xFFEA580C)
                            hasEmergency -> Color(0xFF7F1D1D)
                            count > 0 -> Color(0xFF1E293B)
                            else -> Color(0xFF0F172A)
                        },
                        border = BorderStroke(
                            1.dp,
                            when {
                                isSelected -> Color(0xFFFBBF24)
                                hasEmergency -> Color(0xFFEF4444)
                                count > 0 -> Color(0xFF22C55E).copy(alpha = 0.5f)
                                else -> Color(0xFF334155)
                            }
                        ),
                        modifier = Modifier.clickable { onSelectDistrict(posko.districtName) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = posko.districtName,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Surface(
                                shape = CircleShape,
                                color = if (count > 0) Color(0xFF22C55E) else Color(0xFF475569)
                            ) {
                                Text(
                                    text = "$count",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val poskoActiveSkills = remember(selectedPoskoVolunteers) {
                selectedPoskoVolunteers.flatMap { it.skillsList() }.distinct()
            }
            val missingCoreRoadmapSkills = remember(poskoActiveSkills) {
                listOf(
                    VolunteerSkills.MEDIS,
                    VolunteerSkills.DAPUR_UMUM,
                    "SAR",
                    VolunteerSkills.PSIKOSOSIAL
                ).filter { requiredSkill ->
                    poskoActiveSkills.none { it.contains(requiredSkill, ignoreCase = true) }
                }
            }

            // Box Detail Posko Terpilih (Anti-Vertical-Squish & Proporsional di Layar Ponsel)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF1E293B),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selectedPoskoPoint.poskoName,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Kec. ${selectedPoskoPoint.districtName} • TES/TEA (${selectedPoskoPoint.latitude}, ${selectedPoskoPoint.longitude})",
                                fontSize = 9.5.sp,
                                color = Color(0xFF94A3B8),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF0F172A)
                        ) {
                            Text(
                                text = "${selectedPoskoVolunteers.size} Relawan",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF4ADE80),
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }

                    if (selectedPoskoVolunteers.isNotEmpty()) {
                        Text(
                            text = "Personel Posko: " + selectedPoskoVolunteers.joinToString(", ") { "${it.fullName} (${it.status})" },
                            fontSize = 10.sp,
                            color = Color(0xFF38BDF8),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Keahlian Aktif (Standar Roadmap): ${poskoActiveSkills.joinToString(", ")}",
                            fontSize = 9.5.sp,
                            color = Color(0xFFA7F3D0),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Indikator Pemerataan Personil Posko sesuai ROADMAP.md
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF0F172A).copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (missingCoreRoadmapSkills.isEmpty()) {
                                "✓ Formasi Posko Lengkap & Merata: Medis, Dapur Umum, SAR, dan Psikososial telah terisi."
                            } else {
                                "Rekomendasi Pemerataan Pusdalops (Roadmap): Posko Kec. ${selectedPoskoPoint.districtName} masih membutuhkan keahlian [${missingCoreRoadmapSkills.joinToString(", ")}] untuk layanan Pengungsi Terpilah, Respon SOS & Kaji Cepat DALA."
                            },
                            fontSize = 9.5.sp,
                            lineHeight = 13.sp,
                            color = if (missingCoreRoadmapSkills.isEmpty()) Color(0xFF4ADE80) else Color(0xFFFBBF24),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * KARTU KESELARASAN PETA JALAN (ROADMAP.MD SIPAGAR KITA) UNTUK KOMANDO PUSDALOPS-PB:
 * Menyelaraskan Tab "Dashboard Relawan" dengan 4 Fase di dokumen ROADMAP.md:
 * - Fase 1 (Tanggap Darurat): Manajemen Pengungsi Terpilah (Lansia, Ibu Hamil, Anak/Balita, Disabilitas),
 *   Stok Logistik Darurat (Sembako, Tenda, Obat, Air Bersih), & Respon Sinyal SOS TRC.
 * - Fase 2 (Pra-Bencana): SOP Evakuasi Offline (Tsunami 20-20-20), Edukasi 15 Kecamatan, & Tas Siaga.
 * - Fase 3 (Pasca-Bencana): Pendataan & Koordinasi Relawan (Penempatan Personil Bantuan Secara Merata & Terukur)
 *   serta Sinergi Formulir Kaji Cepat DALA (Damage and Loss Assessment).
 * - Fase 4 (Peningkatan Teknis): Titik Evakuasi TES/TEA, Mode Offline, & Sandbox Training Mode.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VolunteerRoadmapAlignmentCard(
    volunteers: List<VolunteerEntity>,
    onSelectSkillFilter: (String) -> Unit,
    onNavigateToVerification: () -> Unit
) {
    val isSandboxActive = com.example.training.SandboxTrainingManager.isSandboxModeActive()
    val activeVolunteers = remember(volunteers) {
        volunteers.filter { it.statusEnum() != VolunteerStatus.REJECTED }
    }

    // Hitung pemerataan wilayah di 15 Kecamatan Kabupaten Mukomuko
    val totalDistricts = MukomukoVolunteerPoskoCatalog.POSKO_LIST.size
    val coveredDistrictsCount = remember(activeVolunteers) {
        MukomukoVolunteerPoskoCatalog.POSKO_LIST.count { posko ->
            activeVolunteers.any {
                it.effectivePoskoDistrict().equals(posko.districtName, ignoreCase = true)
            }
        }
    }
    val uncoveredDistrictNames = remember(activeVolunteers) {
        MukomukoVolunteerPoskoCatalog.POSKO_LIST
            .filter { posko ->
                activeVolunteers.none {
                    it.effectivePoskoDistrict().equals(posko.districtName, ignoreCase = true)
                }
            }
            .map { it.districtName }
    }

    // Hitung keterukuran 4 Keahlian Utama Roadmap (+ Logistik & Alat Berat)
    val medisCount = remember(activeVolunteers) {
        activeVolunteers.count { VolunteerSkills.matchesSkillFilter(it.skillsCsv, VolunteerSkills.MEDIS) }
    }
    val dapurLogistikCount = remember(activeVolunteers) {
        activeVolunteers.count {
            VolunteerSkills.matchesSkillFilter(it.skillsCsv, VolunteerSkills.DAPUR_UMUM) ||
                VolunteerSkills.matchesSkillFilter(it.skillsCsv, VolunteerSkills.LOGISTIK)
        }
    }
    val sarCount = remember(activeVolunteers) {
        activeVolunteers.count { VolunteerSkills.matchesSkillFilter(it.skillsCsv, "SAR") }
    }
    val psikososialCount = remember(activeVolunteers) {
        activeVolunteers.count { VolunteerSkills.matchesSkillFilter(it.skillsCsv, VolunteerSkills.PSIKOSOSIAL) }
    }
    val alatBeratCount = remember(activeVolunteers) {
        activeVolunteers.count { VolunteerSkills.matchesSkillFilter(it.skillsCsv, VolunteerSkills.ALAT_BERAT) }
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("volunteer_roadmap_alignment_card")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header Keselarasan Roadmap SIPAGAR KITA
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF0F172A)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Assignment,
                            contentDescription = null,
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier
                                .padding(6.dp)
                                .size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "KOMANDO PUSDALOPS — KESELARASAN ROADMAP SIPAGAR KITA",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Penempatan Personil Bantuan Secara Merata & Terukur di 15 Kecamatan",
                            fontSize = 9.5.sp,
                            color = Color(0xFF475569),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isSandboxActive) Color(0xFFFEF3C7) else Color(0xFFDCFCE7),
                    border = BorderStroke(
                        1.dp,
                        if (isSandboxActive) Color(0xFFF59E0B) else Color(0xFF16A34A)
                    )
                ) {
                    Text(
                        text = if (isSandboxActive) "SANDBOX LATIHAN" else "OPERASI AKTIF",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isSandboxActive) Color(0xFF92400E) else Color(0xFF166534),
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // 1. Matriks Sinergi Keahlian Relawan terhadap Fase Roadmap (Klik untuk Filter Cepat)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                RoadmapSkillPillarBox(
                    skillTitle = "Medis ($medisCount)",
                    roadmapRole = "Pengungsi Rentan (Lansia, Ibu Hamil, Balita, Disabilitas)",
                    phaseBadge = "Fase 1",
                    accentColor = Color(0xFF0284C7),
                    onClick = { onSelectSkillFilter(VolunteerSkills.MEDIS) },
                    modifier = Modifier.weight(1f)
                )
                RoadmapSkillPillarBox(
                    skillTitle = "Dapur & Logistik ($dapurLogistikCount)",
                    roadmapRole = "Distribusi Sembako, Tenda, Obat & Air Bersih Posko",
                    phaseBadge = "Fase 1",
                    accentColor = Color(0xFF15803D),
                    onClick = { onSelectSkillFilter(VolunteerSkills.DAPUR_UMUM) },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                RoadmapSkillPillarBox(
                    skillTitle = "SAR & Evakuasi ($sarCount)",
                    roadmapRole = "Respon Sinyal SOS Darurat, Evakuasi TES/TEA & TRC",
                    phaseBadge = "Fase 1 & 2",
                    accentColor = Color(0xFFEA580C),
                    onClick = { onSelectSkillFilter("SAR") },
                    modifier = Modifier.weight(1f)
                )
                RoadmapSkillPillarBox(
                    skillTitle = "Psikososial & DALA (${psikososialCount + alatBeratCount})",
                    roadmapRole = "Pemulihan Pasca-Bencana & Pendataan Kerusakan DALA",
                    phaseBadge = "Fase 3",
                    accentColor = Color(0xFF4F46E5),
                    onClick = { onSelectSkillFilter(VolunteerSkills.PSIKOSOSIAL) },
                    modifier = Modifier.weight(1f)
                )
            }

            // 2. Indeks Pemerataan Pembagian Wilayah Tugas (15 Kecamatan Kab. Mukomuko)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Indeks Pemerataan Wilayah Tugas Posko (TES / TEA):",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$coveredDistrictsCount / $totalDistricts Kecamatan Terisi",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF15803D),
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    if (uncoveredDistrictNames.isNotEmpty()) {
                        Text(
                            text = "Prioritas Pemerataan Pusdalops (Posko Belum Terisi): ${uncoveredDistrictNames.take(5).joinToString(", ")}" +
                                if (uncoveredDistrictNames.size > 5) " (+${uncoveredDistrictNames.size - 5} kec. lainnya)" else "",
                            fontSize = 9.5.sp,
                            color = Color(0xFFB45309),
                            lineHeight = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Seluruh Organisasi & Komunitas Mitra Terdaftar di BPBD Kab. Mukomuko",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF166534),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Atur Penempatan ➜",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFEA580C),
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier
                                .clickable { onNavigateToVerification() }
                                .testTag("btn_roadmap_distribute_volunteers")
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RoadmapSkillPillarBox(
    skillTitle: String,
    roadmapRole: String,
    phaseBadge: String,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = accentColor.copy(alpha = 0.07f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = skillTitle,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = accentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = accentColor.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = phaseBadge,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = accentColor,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
            Text(
                text = roadmapRole,
                fontSize = 9.sp,
                color = Color(0xFF334155),
                lineHeight = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 8.5.sp,
            color = Color(0xFFCBD5E1),
            maxLines = 1,
            softWrap = false
        )
    }
}

/**
 * Kartu Direktori Relawan dengan Masking Privasi NIK, Status Kehadiran Real-time,
 * Tombol [ CHECK-IN POSKO ] & [ CHECK-OUT POSKO ], serta Tombol "Unduh Surat Tugas".
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VolunteerDirectoryCard(
    volunteer: VolunteerEntity,
    isAdminView: Boolean,
    onOpenDetailClick: () -> Unit,
    onCheckInPosko: () -> Unit,
    onCheckOutPosko: () -> Unit,
    onDownloadSuratTugas: () -> Unit
) {
    val statusEnum = volunteer.statusEnum()
    val statusColor = when (statusEnum) {
        VolunteerStatus.PENDING -> Color(0xFFD97706)
        VolunteerStatus.VERIFIED -> Color(0xFF0284C7)
        VolunteerStatus.ASSIGNED -> Color(0xFF15803D)
        VolunteerStatus.DONE -> Color(0xFF4F46E5)
        VolunteerStatus.REJECTED -> Color(0xFFDC2626)
    }

    val displayedNik = VolunteerPrivacyHelper.maskNik(volunteer.nik, isAdminView)
    val displayedPhone = VolunteerPrivacyHelper.maskPhone(volunteer.phoneNumber, isAdminView)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (volunteer.needsAssistance) Color(0xFFFEF2F2) else Color.White
        ),
        border = BorderStroke(
            width = if (volunteer.needsAssistance) 1.5.dp else 1.dp,
            color = if (volunteer.needsAssistance) Color(0xFFEF4444) else Color(0xFFE2E8F0)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenDetailClick() }
            .testTag("volunteer_card_${volunteer.nik}")
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            if (volunteer.needsAssistance) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFDC2626),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "BUTUH BANTUAN DARURAT: ${volunteer.assistanceNote.ifBlank { "Membutuhkan dukungan di titik posko" }}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Foto Diri Relawan
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFE2E8F0),
                    border = BorderStroke(1.5.dp, statusColor),
                    modifier = Modifier.size(44.dp)
                ) {
                    if (!volunteer.selfiePhotoUri.isNullOrBlank()) {
                        AsyncImage(
                            model = volunteer.selfiePhotoUri,
                            contentDescription = "Foto ${volunteer.fullName}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = null,
                                tint = Color(0xFF475569),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = volunteer.fullName,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (volunteer.isSyncedToCloud) Color(0xFFDCFCE7) else Color(0xFFFEF3C7)
                        ) {
                            Text(
                                text = if (volunteer.isSyncedToCloud) "CLOUD" else "SQLITE OFFLINE",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (volunteer.isSyncedToCloud) Color(0xFF166534) else Color(0xFF92400E),
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }

                    Text(
                        text = "${volunteer.organization} • ID: ${volunteer.id}",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF475569),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            imageVector = if (isAdminView) Icons.Default.Badge else Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (isAdminView) Color(0xFF0284C7) else Color(0xFF64748B),
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "NIK: $displayedNik • HP: $displayedPhone",
                            fontSize = 10.5.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isAdminView) Color(0xFF0F172A) else Color(0xFF64748B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("volunteer_nik_${volunteer.nik}")
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = statusColor.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, statusColor)
                    ) {
                        Text(
                            text = "${statusEnum.code} (${statusEnum.displayLabel})",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = statusColor,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Detail & Absensi ➜",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFEA580C),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Daftar Chips Keahlian
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                volunteer.skillsList().forEach { skill ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.MedicalServices,
                                contentDescription = null,
                                tint = Color(0xFFEA580C),
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = skill,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B),
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // KHUSUS STATUS "ASSIGNED" (DITUGASKAN):
            // 1. Status Kehadiran Real-time ("Sedang Bertugas di Posko ... sejak 08.00 WIB")
            // 2. Dua Tombol Besar: [ CHECK-IN POSKO ] (Hijau) & [ CHECK-OUT POSKO ] (Merah)
            // 3. Tombol "Unduh Surat Tugas" (PDF)
            if (statusEnum == VolunteerStatus.ASSIGNED) {
                Spacer(modifier = Modifier.height(8.dp))
                val isCheckedIn = volunteer.isCurrentlyCheckedInAtPosko()

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isCheckedIn) Color(0xFFECFDF5) else Color(0xFFF8FAFC),
                    border = BorderStroke(
                        1.dp,
                        if (isCheckedIn) Color(0xFF10B981) else Color(0xFFCBD5E1)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isCheckedIn) Color(0xFF16A34A) else Color(0xFFD97706))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = volunteer.realtimeAttendanceStatusText(),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isCheckedIn) Color(0xFF065F46) else Color(0xFF334155),
                                modifier = Modifier.testTag("realtime_attendance_${volunteer.nik}")
                            )
                        }

                        if (volunteer.assignedShift.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Shift: ${volunteer.assignedShift} • Kec. ${volunteer.effectivePoskoDistrict()}",
                                fontSize = 9.5.sp,
                                color = Color(0xFF475569),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Dua Tombol Besar: [ CHECK-IN POSKO ] (Hijau) dan [ CHECK-OUT POSKO ] (Merah)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onCheckInPosko,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 40.dp)
                                    .testTag("btn_checkin_${volunteer.nik}")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Login,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "CHECK-IN POSKO",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            Button(
                                onClick = onCheckOutPosko,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 40.dp)
                                    .testTag("btn_checkout_${volunteer.nik}")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Logout,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "CHECK-OUT POSKO",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Tombol "Unduh Surat Tugas" (PDF)
                        OutlinedButton(
                            onClick = onDownloadSuratTugas,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFF0284C7)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 38.dp)
                                .testTag("btn_download_surat_tugas_${volunteer.nik}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                tint = Color(0xFF0284C7),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Unduh Surat Tugas (PDF)",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0284C7),
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

/**
 * TAB 2: REGISTRASI RELAWAN (Form Input Offline-First SQLite + Validasi NIK Anti Data Ganda)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VolunteerRegistrationTabContent(
    context: Context,
    existingVolunteers: List<VolunteerEntity>,
    isOnline: Boolean,
    unsyncedLocalCount: Int,
    onDuplicateNikDetected: (String) -> Unit,
    onSubmitRegistration: (
        fullName: String,
        nik: String,
        phone: String,
        organization: String,
        skills: List<String>,
        district: String,
        selfieUri: String?,
        certUri: String?
    ) -> Unit
) {
    var fullName by rememberSaveable { mutableStateOf("") }
    var nik by rememberSaveable { mutableStateOf("") }
    var phoneNumber by rememberSaveable { mutableStateOf("") }
    var organization by rememberSaveable { mutableStateOf("Relawan Individu") }
    var homeDistrict by rememberSaveable { mutableStateOf("Kota Mukomuko") }
    var districtDropdownExpanded by remember { mutableStateOf(false) }

    val selectedSkills = remember {
        mutableStateListOf(VolunteerSkills.SAR)
    }

    var selfiePhotoPath by rememberSaveable { mutableStateOf<String?>(null) }
    var certificatePhotoPath by rememberSaveable { mutableStateOf<String?>(null) }
    var validationError by remember { mutableStateOf<String?>(null) }

    // Cek secara reaktif apakah NIK (16 digit) sudah terdaftar di database / dummy data
    val isDuplicateNikInDb = remember(nik, existingVolunteers) {
        val cleaned = nik.trim()
        cleaned.length == 16 && existingVolunteers.any { it.nik.equals(cleaned, ignoreCase = true) }
    }

    val selfiePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selfiePhotoPath = copyVolunteerPhotoToInternalStorage(context, uri, "selfie")
        }
    }

    val certificatePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            certificatePhotoPath = copyVolunteerPhotoToInternalStorage(context, uri, "cert")
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("volunteer_registration_form"),
        contentPadding = PaddingValues(start = 12.dp, top = 10.dp, end = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Banner Jaminan Offline-First SQLite & Primary Key NIK
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isOnline) Color(0xFFECFDF5) else Color(0xFFFFFBEB),
                border = BorderStroke(
                    1.dp,
                    if (isOnline) Color(0xFF10B981) else Color(0xFFF59E0B)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isOnline) Icons.Default.CloudDone else Icons.Default.CloudOff,
                        contentDescription = null,
                        tint = if (isOnline) Color(0xFF059669) else Color(0xFFD97706),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Validasi NIK Primary Key (Anti Data Ganda) + Offline SQLite",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isOnline) Color(0xFF065F46) else Color(0xFF92400E),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "NIK 16 digit berfungsi sebagai Primary Key di SQLite & Cloud Firestore. Pendaftaran NIK ganda otomatis ditolak." +
                                if (unsyncedLocalCount > 0) " ($unsyncedLocalCount antrean lokal siap sinkron)" else "",
                            fontSize = 10.sp,
                            color = if (isOnline) Color(0xFF047857) else Color(0xFFB45309),
                            lineHeight = 14.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Card Form Input Utama
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Formulir Registrasi Relawan Kebencanaan",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Data NIK (Primary Key) & No. HP Anda dilindungi enkripsi privasi dan hanya dapat dilihat oleh Admin Pusdalops BPBD.",
                            fontSize = 10.5.sp,
                            color = Color(0xFF64748B),
                            lineHeight = 14.sp
                        )
                    }

                    HorizontalDivider(color = Color(0xFFE2E8F0))

                    // 1. Nama Lengkap
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = {
                            fullName = it
                            validationError = null
                        },
                        label = { Text("Nama Lengkap Relawan *") },
                        placeholder = { Text("Contoh: Ahmad Fauzi Pratama") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_volunteer_name")
                    )

                    // 2. NIK (Wajib 16 Digit - Primary Key - Anti Data Ganda)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(
                            value = nik,
                            onValueChange = { input ->
                                val digitsOnly = input.filter { it.isDigit() }.take(16)
                                nik = digitsOnly
                                if (digitsOnly.length == 16 &&
                                    existingVolunteers.any { it.nik.equals(digitsOnly, ignoreCase = true) }
                                ) {
                                    validationError = VolunteerPrivacyHelper.DUPLICATE_NIK_ALERT_MESSAGE
                                    onDuplicateNikDetected(VolunteerPrivacyHelper.DUPLICATE_NIK_ALERT_MESSAGE)
                                } else {
                                    validationError = null
                                }
                            },
                            isError = isDuplicateNikInDb,
                            label = { Text("NIK (16 Digit - Primary Key Wajib) *") },
                            placeholder = { Text("Masukkan 16 Digit NIK KTP") },
                            supportingText = {
                                if (isDuplicateNikInDb) {
                                    Text(
                                        text = VolunteerPrivacyHelper.DUPLICATE_NIK_ALERT_MESSAGE,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFDC2626)
                                    )
                                } else {
                                    Text(
                                        text = "Terkumpul: ${nik.length}/16 digit • Primary Key Anti Data Ganda • Samaran Publik: ${VolunteerPrivacyHelper.maskNik(nik.ifBlank { "1706000000000000" }, false)}",
                                        fontSize = 10.sp
                                    )
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_volunteer_nik")
                        )

                        // Tombol bantu uji cepat Validasi Anti Data Ganda
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Uji Validasi Anti Data Ganda:",
                                fontSize = 10.sp,
                                color = Color(0xFF64748B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Coba NIK Terdaftar (1706011408910001)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEA580C),
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier
                                    .clickable {
                                        nik = "1706011408910001"
                                        validationError = VolunteerPrivacyHelper.DUPLICATE_NIK_ALERT_MESSAGE
                                        onDuplicateNikDetected(VolunteerPrivacyHelper.DUPLICATE_NIK_ALERT_MESSAGE)
                                    }
                                    .testTag("btn_test_duplicate_nik")
                            )
                        }
                    }

                    // 3. No. HP / WhatsApp
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { input ->
                            phoneNumber = input.filter { it.isDigit() || it == '+' || it == '-' }.take(16)
                            validationError = null
                        },
                        label = { Text("No. HP / WhatsApp Darurat *") },
                        placeholder = { Text("Contoh: 081278901234") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_volunteer_phone")
                    )

                    // 4. Asal Organisasi / Komunitas
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        val isRegisteredPresetOrg = MukomukoVolunteerPoskoCatalog.ORGANIZATION_PRESETS.any {
                            it.equals(organization.trim(), ignoreCase = true)
                        }
                        OutlinedTextField(
                            value = organization,
                            onValueChange = { organization = it },
                            label = { Text("Asal Organisasi / Komunitas *") },
                            placeholder = {
                                Text("Relawan Individu, YAKI INDONESIA, KPS Selagan-Manjunto, FPL (Forum Peduli Lingkungan), PMI, Tagana, dll")
                            },
                            supportingText = {
                                Text(
                                    text = if (isRegisteredPresetOrg) {
                                        "✓ Organisasi / Komunitas Terdaftar di BPBD Kabupaten Mukomuko"
                                    } else {
                                        "Pilih organisasi/komunitas terdaftar di BPBD Mukomuko atau ketik nama komunitas Anda."
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = if (isRegisteredPresetOrg) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isRegisteredPresetOrg) Color(0xFF15803D) else Color(0xFF64748B)
                                )
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_volunteer_organization")
                        )

                        Text(
                            text = "Pilihan Cepat Organisasi / Komunitas (Terdaftar di BPBD Mukomuko):",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF475569)
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            MukomukoVolunteerPoskoCatalog.ORGANIZATION_PRESETS.forEach { presetOrg ->
                                val selected = organization.equals(presetOrg, ignoreCase = true)
                                FilterChip(
                                    selected = selected,
                                    onClick = { organization = presetOrg },
                                    label = {
                                        Text(
                                            text = presetOrg,
                                            fontSize = 10.5.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF0F172A),
                                        selectedLabelColor = Color.White
                                    ),
                                    modifier = Modifier.testTag("chip_org_$presetOrg")
                                )
                            }
                        }
                    }

                    // 5. Kecamatan Domisili / Posko Terdekat
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Kecamatan Domisili / Posko Terdekat:",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { districtDropdownExpanded = true },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 44.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Kecamatan $homeDistrict",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = Color(0xFFEA580C)
                                    )
                                }
                            }
                            DropdownMenu(
                                expanded = districtDropdownExpanded,
                                onDismissRequest = { districtDropdownExpanded = false }
                            ) {
                                MukomukoVolunteerPoskoCatalog.POSKO_LIST.forEach { posko ->
                                    DropdownMenuItem(
                                        text = { Text("${posko.districtName} (${posko.poskoName})", fontSize = 12.sp) },
                                        onClick = {
                                            homeDistrict = posko.districtName
                                            districtDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // 6. Pilihan Keahlian (Multi-Select Chips)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Pilihan Keahlian (Multi-select chips - Pilih minimal 1) *",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A)
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            VolunteerSkills.ALL_SKILLS.forEach { skill ->
                                val isSelected = selectedSkills.contains(skill)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        if (isSelected) {
                                            selectedSkills.remove(skill)
                                        } else {
                                            selectedSkills.add(skill)
                                        }
                                        validationError = null
                                    },
                                    label = {
                                        Text(
                                            text = skill,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    leadingIcon = if (isSelected) {
                                        {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    } else null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFEA580C),
                                        selectedLabelColor = Color.White,
                                        selectedLeadingIconColor = Color.White
                                    ),
                                    modifier = Modifier.testTag("chip_skill_$skill")
                                )
                            }
                        }
                    }

                    // 7. Upload Foto Diri & Sertifikat Keahlian (Jika Ada)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Lampiran Dokumen Identitas & Sertifikasi",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            VolunteerPhotoUploadCard(
                                title = "Foto Diri Relawan",
                                subtitle = "Wajib / Identitas",
                                photoPath = selfiePhotoPath,
                                onPickPhoto = {
                                    selfiePickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                onClearPhoto = { selfiePhotoPath = null },
                                testTag = "btn_upload_volunteer_selfie",
                                modifier = Modifier.weight(1f)
                            )

                            VolunteerPhotoUploadCard(
                                title = "Sertifikat Keahlian",
                                subtitle = "Opsional (Jika Ada)",
                                photoPath = certificatePhotoPath,
                                onPickPhoto = {
                                    certificatePickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                onClearPhoto = { certificatePhotoPath = null },
                                testTag = "btn_upload_volunteer_certificate",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Pesan Validasi Error
                    AnimatedVisibility(visible = !validationError.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFEF2F2),
                            border = BorderStroke(1.dp, Color(0xFFF87171)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("banner_registration_validation_error")
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = validationError.orEmpty(),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB91C1C)
                                )
                            }
                        }
                    }

                    // 8. Tombol "Kirim Pendaftaran"
                    Button(
                        onClick = {
                            val cleanedNik = nik.trim()
                            when {
                                cleanedNik.length != 16 || !cleanedNik.all { it.isDigit() } -> {
                                    validationError = "NIK wajib diisi tepat 16 digit angka sesuai KTP."
                                }
                                existingVolunteers.any { it.nik.equals(cleanedNik, ignoreCase = true) } -> {
                                    validationError = VolunteerPrivacyHelper.DUPLICATE_NIK_ALERT_MESSAGE
                                    onDuplicateNikDetected(VolunteerPrivacyHelper.DUPLICATE_NIK_ALERT_MESSAGE)
                                }
                                fullName.trim().length < 3 -> {
                                    validationError = "Nama Lengkap wajib diisi minimal 3 karakter."
                                }
                                phoneNumber.trim().length < 8 -> {
                                    validationError = "Nomor HP / WhatsApp darurat wajib diisi."
                                }
                                selectedSkills.isEmpty() -> {
                                    validationError = "Pilih minimal 1 Keahlian penanganan bencana."
                                }
                                else -> {
                                    onSubmitRegistration(
                                        fullName.trim(),
                                        cleanedNik,
                                        phoneNumber.trim(),
                                        organization.trim().ifBlank { "Relawan Individu" },
                                        selectedSkills.toList(),
                                        homeDistrict,
                                        selfiePhotoPath,
                                        certificatePhotoPath
                                    )
                                }
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("btn_submit_volunteer_registration")
                    ) {
                        Icon(
                            imageVector = Icons.Default.HowToReg,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Kirim Pendaftaran",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VolunteerPhotoUploadCard(
    title: String,
    subtitle: String,
    photoPath: String?,
    onPickPhoto: () -> Unit,
    onClearPhoto: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(
            1.dp,
            if (photoPath != null) Color(0xFF15803D) else Color(0xFFCBD5E1)
        ),
        modifier = modifier
            .clickable { onPickPhoto() }
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!photoPath.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    AsyncImage(
                        model = photoPath,
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    IconButton(
                        onClick = onClearPhoto,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(24.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Hapus Foto",
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE2E8F0).copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddAPhoto,
                            contentDescription = null,
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Pilih Foto",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF334155),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF0F172A),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (photoPath != null) "Lampiran Siap ✓" else subtitle,
                fontSize = 9.5.sp,
                color = if (photoPath != null) Color(0xFF15803D) else Color(0xFF64748B),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * TAB 3: VERIFIKASI & PENUGASAN (Khusus Admin / Pusdalops)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VolunteerVerificationTabContent(
    volunteers: List<VolunteerEntity>,
    isOfficerLoggedIn: Boolean,
    isEffectiveAdminView: Boolean,
    officerName: String,
    onRequestOfficerLogin: (() -> Unit)?,
    onDisablePublicSimulation: () -> Unit,
    onVerifyVolunteer: (VolunteerEntity) -> Unit,
    onRejectVolunteer: (VolunteerEntity) -> Unit,
    onOpenAssignModal: (VolunteerEntity) -> Unit,
    onOpenDetailVolunteer: (VolunteerEntity) -> Unit,
    onDownloadSuratTugas: (VolunteerEntity) -> Unit,
    onCheckInPosko: (VolunteerEntity) -> Unit,
    onCheckOutPosko: (VolunteerEntity) -> Unit,
    onMarkAssignmentDone: (VolunteerEntity) -> Unit,
    onToggleNeedsAssistance: (VolunteerEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("volunteer_verification_list"),
        contentPadding = PaddingValues(start = 12.dp, top = 10.dp, end = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Banner Status Otorisasi Admin Pusdalops
        item {
            if (!isOfficerLoggedIn) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                    border = BorderStroke(1.dp, Color(0xFFEF4444)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Akses Terbatas: Khusus Admin / Operator Pusdalops-PB",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF991B1B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Sesuai ketentuan keamanan Data Privacy, NIK penuh, No. HP, serta tombol aksi Verifikasi, Tolak, dan Tugaskan hanya aktif bagi Admin Pusdalops yang sudah login di Ruang Kerja.",
                            fontSize = 10.5.sp,
                            color = Color(0xFF7F1D1D),
                            lineHeight = 14.sp
                        )
                        if (onRequestOfficerLogin != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = onRequestOfficerLogin,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.heightIn(min = 38.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Login Petugas Pusdalops Sekarang", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else if (!isEffectiveAdminView) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFFFBEB),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Simulasi Tampilan Publik Sedang Aktif",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF92400E),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "NIK disamarkan menjadi 1706********. Kembalikan ke Mode Admin untuk melihat NIK utuh.",
                                fontSize = 10.sp,
                                color = Color(0xFFB45309)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onDisablePublicSimulation,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Buka NIK Admin",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFECFDF5),
                    border = BorderStroke(1.dp, Color(0xFF10B981)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = Color(0xFF059669),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Konsol Verifikasi, Penugasan & Surat Tugas PDF (${officerName.ifBlank { "Petugas Resmi" }})",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF065F46),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Alur State Management: Pending ➔ Verified ➔ Assigned (Surat Tugas PDF + Check-in/Out Posko) ➔ Done.",
                                fontSize = 10.sp,
                                color = Color(0xFF047857),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Daftar Pendaftar Masuk & Penugasan
        items(volunteers, key = { "verify_${it.nik}" }) { volunteer ->
            val statusEnum = volunteer.statusEnum()
            val displayedNik = VolunteerPrivacyHelper.maskNik(volunteer.nik, isEffectiveAdminView)
            val displayedPhone = VolunteerPrivacyHelper.maskPhone(volunteer.phoneNumber, isEffectiveAdminView)

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(
                    1.dp,
                    when (statusEnum) {
                        VolunteerStatus.PENDING -> Color(0xFFF59E0B)
                        VolunteerStatus.VERIFIED -> Color(0xFF38BDF8)
                        VolunteerStatus.ASSIGNED -> Color(0xFF22C55E)
                        VolunteerStatus.DONE -> Color(0xFF818CF8)
                        VolunteerStatus.REJECTED -> Color(0xFFF87171)
                    }
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenDetailVolunteer(volunteer) }
                    .testTag("verify_item_${volunteer.nik}")
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = volunteer.fullName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0F172A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${volunteer.organization} • Domisili: Kec. ${volunteer.homeDistrict}",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF475569),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        VolunteerStatusPipelineBadge(statusEnum = statusEnum)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Box Data Privasi NIK (Primary Key) & No. HP
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isEffectiveAdminView) Color(0xFFF0F9FF) else Color(0xFFF8FAFC),
                        border = BorderStroke(
                            1.dp,
                            if (isEffectiveAdminView) Color(0xFFBAE6FD) else Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Badge,
                                    contentDescription = null,
                                    tint = Color(0xFF0284C7),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "NIK KTP (Primary Key): $displayedNik",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF0F172A),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = Color(0xFF15803D),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "No. HP/WA: $displayedPhone",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Keahlian & Sertifikat
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        volunteer.skillsList().forEach { skill ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFFFF7ED),
                                border = BorderStroke(1.dp, Color(0xFFFDBA74))
                            ) {
                                Text(
                                    text = skill,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF9A3412),
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                                )
                            }
                        }

                        if (!volunteer.certificatePhotoUri.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFECFDF5),
                                border = BorderStroke(1.dp, Color(0xFF6EE7B7))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.WorkspacePremium,
                                        contentDescription = null,
                                        tint = Color(0xFF059669),
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Sertifikat Terlampir",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF065F46),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }

                    // Informasi Penugasan Aktif & Kehadiran Real-time (Jika Sudah Ditugaskan)
                    if (statusEnum == VolunteerStatus.ASSIGNED) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF0FDF4),
                            border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = volunteer.realtimeAttendanceStatusText(),
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF166534)
                                )
                                Text(
                                    text = "Lokasi: ${volunteer.effectivePoskoName()} (Kec. ${volunteer.effectivePoskoDistrict()}) • ${volunteer.assignedShift}",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF15803D),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { onCheckInPosko(volunteer) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D)),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = "CHECK-IN POSKO",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                    Button(
                                        onClick = { onCheckOutPosko(volunteer) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = "CHECK-OUT POSKO",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                    Spacer(modifier = Modifier.height(8.dp))

                    // Tombol Aksi per Item: "Verifikasi", "Tolak", "Tugaskan", "Unduh Surat Tugas", "Selesai", & "Butuh Bantuan"
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // 1. Tombol "Verifikasi"
                        Button(
                            onClick = { onVerifyVolunteer(volunteer) },
                            enabled = isOfficerLoggedIn && statusEnum != VolunteerStatus.VERIFIED && statusEnum != VolunteerStatus.ASSIGNED,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier
                                .heightIn(min = 38.dp)
                                .testTag("btn_verify_${volunteer.nik}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Verifikasi",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        // 2. Tombol "Tolak"
                        OutlinedButton(
                            onClick = { onRejectVolunteer(volunteer) },
                            enabled = isOfficerLoggedIn && statusEnum != VolunteerStatus.REJECTED,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFDC2626)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier
                                .heightIn(min = 38.dp)
                                .testTag("btn_reject_${volunteer.nik}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Tolak",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626),
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        // 3. Tombol "Tugaskan"
                        Button(
                            onClick = { onOpenAssignModal(volunteer) },
                            enabled = isOfficerLoggedIn && statusEnum != VolunteerStatus.REJECTED,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier
                                .heightIn(min = 38.dp)
                                .testTag("btn_assign_${volunteer.nik}")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Assignment,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Tugaskan",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        // 4. Tombol "Unduh Surat Tugas" (PDF) untuk relawan yang sudah ditugaskan
                        if (statusEnum == VolunteerStatus.ASSIGNED || volunteer.suratTugasNumber.isNotBlank()) {
                            OutlinedButton(
                                onClick = { onDownloadSuratTugas(volunteer) },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFF0284C7)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .heightIn(min = 38.dp)
                                    .testTag("btn_verify_download_pdf_${volunteer.nik}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    tint = Color(0xFF0284C7),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Unduh Surat Tugas",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF0284C7),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        // 5. Tombol "Selesai" (Assigned -> Done)
                        if (statusEnum == VolunteerStatus.ASSIGNED) {
                            Button(
                                onClick = { onMarkAssignmentDone(volunteer) },
                                enabled = isOfficerLoggedIn,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .heightIn(min = 38.dp)
                                    .testTag("btn_done_${volunteer.nik}")
                            ) {
                                Text(
                                    text = "Selesai (Done)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        // 6. Toggle Darurat "Butuh Bantuan"
                        OutlinedButton(
                            onClick = { onToggleNeedsAssistance(volunteer) },
                            enabled = isOfficerLoggedIn,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(
                                1.dp,
                                if (volunteer.needsAssistance) Color(0xFF15803D) else Color(0xFFB91C1C)
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier
                                .heightIn(min = 38.dp)
                                .testTag("btn_assistance_${volunteer.nik}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (volunteer.needsAssistance) Color(0xFF15803D) else Color(0xFFB91C1C),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (volunteer.needsAssistance) "Bantuan Terpenuhi" else "Butuh Bantuan",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (volunteer.needsAssistance) Color(0xFF15803D) else Color(0xFFB91C1C),
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
private fun VolunteerStatusPipelineBadge(statusEnum: VolunteerStatus) {
    val steps = listOf(
        VolunteerStatus.PENDING,
        VolunteerStatus.VERIFIED,
        VolunteerStatus.ASSIGNED,
        VolunteerStatus.DONE
    )
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color(0xFFF1F5F9),
        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            if (statusEnum == VolunteerStatus.REJECTED) {
                Text(
                    text = "DITOLAK",
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFDC2626),
                    maxLines = 1,
                    softWrap = false
                )
            } else {
                steps.forEachIndexed { index, step ->
                    val isCurrent = step == statusEnum
                    Text(
                        text = step.code,
                        fontSize = 8.5.sp,
                        fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Normal,
                        color = if (isCurrent) Color(0xFFEA580C) else Color(0xFF94A3B8),
                        maxLines = 1,
                        softWrap = false
                    )
                    if (index < steps.lastIndex) {
                        Text(
                            text = "→",
                            fontSize = 8.5.sp,
                            color = Color(0xFFCBD5E1),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

/**
 * MODAL PENUGASAN RELAWAN:
 * Saat Admin selesai memasukkan data penugasan (lokasi & shift) dan menekan tombol "Simpan Penugasan",
 * sistem otomatis memicu fungsi pembuatan file PDF "SURAT TUGAS RELAWAN".
 */
@Composable
private fun VolunteerAssignmentModalDialog(
    volunteer: VolunteerEntity,
    onDismiss: () -> Unit,
    onConfirmAssign: (district: String, poskoName: String, shift: String) -> Unit
) {
    val initialPosko = remember(volunteer) {
        MukomukoVolunteerPoskoCatalog.POSKO_LIST.firstOrNull {
            it.districtName.equals(volunteer.assignedDistrict.ifBlank { volunteer.homeDistrict }, ignoreCase = true)
        } ?: MukomukoVolunteerPoskoCatalog.POSKO_LIST.first()
    }

    var selectedPosko by remember { mutableStateOf(initialPosko) }
    var customPoskoName by remember {
        mutableStateOf(volunteer.assignedPoskoName.ifBlank { initialPosko.poskoName })
    }
    var selectedShift by remember {
        mutableStateOf(
            volunteer.assignedShift.ifBlank { MukomukoVolunteerPoskoCatalog.SHIFT_OPTIONS.first() }
        )
    }
    var poskoDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("modal_assign_volunteer"),
        title = {
            Column {
                Text(
                    text = "Penugasan Relawan & Cetak Surat Tugas PDF",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "${volunteer.fullName} (NIK: ${volunteer.nik}) • ${volunteer.skillsList().joinToString(", ")}",
                    fontSize = 11.sp,
                    color = Color(0xFF475569)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "1. Pilih Kecamatan & Titik Posko BPBD:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { poskoDropdownExpanded = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("dropdown_assign_posko_district")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Kec. ${selectedPosko.districtName}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = Color(0xFFEA580C)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = poskoDropdownExpanded,
                            onDismissRequest = { poskoDropdownExpanded = false }
                        ) {
                            MukomukoVolunteerPoskoCatalog.POSKO_LIST.forEach { posko ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = "Kec. ${posko.districtName}",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = posko.poskoName,
                                                fontSize = 11.sp,
                                                color = Color(0xFF64748B)
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedPosko = posko
                                        customPoskoName = posko.poskoName
                                        poskoDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = customPoskoName,
                    onValueChange = { customPoskoName = it },
                    label = { Text("Lokasi / Nama Posko Penugasan") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_assign_posko_name")
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "2. Atur Shift (Jam Bertugas):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    MukomukoVolunteerPoskoCatalog.SHIFT_OPTIONS.forEach { shiftOption ->
                        val isSelected = selectedShift == shiftOption
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) Color(0xFFFFEDD5) else Color(0xFFF8FAFC),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) Color(0xFFEA580C) else Color(0xFFCBD5E1)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedShift = shiftOption }
                                .testTag("shift_option_$shiftOption")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = if (isSelected) Color(0xFFEA580C) else Color(0xFF64748B),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = shiftOption,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (isSelected) Color(0xFF9A3412) else Color(0xFF1E293B)
                                )
                            }
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF0F9FF),
                    border = BorderStroke(1.dp, Color(0xFFBAE6FD)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Menekan 'Simpan Penugasan' otomatis men-generate PDF SURAT TUGAS RELAWAN resmi BPBD Mukomuko.",
                            fontSize = 10.sp,
                            color = Color(0xFF0369A1)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirmAssign(
                        selectedPosko.districtName,
                        customPoskoName.trim().ifBlank { selectedPosko.poskoName },
                        selectedShift
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                modifier = Modifier.testTag("btn_confirm_volunteer_assignment")
            ) {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Simpan Penugasan", fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

/**
 * HALAMAN / MODAL DETAIL RELAWAN:
 * Menampilkan dua tombol besar [ CHECK-IN POSKO ] (Hijau) & [ CHECK-OUT POSKO ] (Merah),
 * status kehadiran real-time ("Sedang Bertugas di Posko ... sejak 08.00 WIB"),
 * serta tombol "Unduh Surat Tugas" (PDF).
 */
@Composable
private fun VolunteerDetailModalDialog(
    volunteer: VolunteerEntity,
    isAdminView: Boolean,
    onDismiss: () -> Unit,
    onCheckInPosko: () -> Unit,
    onCheckOutPosko: () -> Unit,
    onDownloadSuratTugas: () -> Unit
) {
    val statusEnum = volunteer.statusEnum()
    val displayedNik = VolunteerPrivacyHelper.maskNik(volunteer.nik, isAdminView)
    val displayedPhone = VolunteerPrivacyHelper.maskPhone(volunteer.phoneNumber, isAdminView)
    val isCheckedIn = volunteer.isCurrentlyCheckedInAtPosko()

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("modal_detail_volunteer"),
        title = {
            Column {
                Text(
                    text = "Detail & Kehadiran Posko Relawan",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "${volunteer.fullName} • ${volunteer.organization}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF475569)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "NIK (Primary Key): $displayedNik",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "ID Registrasi: ${volunteer.id} • HP: $displayedPhone",
                            fontSize = 11.sp,
                            color = Color(0xFF334155)
                        )
                        Text(
                            text = "Keahlian: ${volunteer.skillsList().joinToString(", ")}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEA580C)
                        )
                        Text(
                            text = "Status: ${statusEnum.code} (${statusEnum.displayLabel})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF15803D)
                        )
                    }
                }

                if (statusEnum == VolunteerStatus.ASSIGNED) {
                    // Status Kehadiran Real-time
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isCheckedIn) Color(0xFFECFDF5) else Color(0xFFFFFBEB),
                        border = BorderStroke(
                            1.5.dp,
                            if (isCheckedIn) Color(0xFF10B981) else Color(0xFFF59E0B)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "STATUS KEHADIRAN REAL-TIME:",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isCheckedIn) Color(0xFF047857) else Color(0xFFB45309)
                            )
                            Text(
                                text = volunteer.realtimeAttendanceStatusText(),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isCheckedIn) Color(0xFF065F46) else Color(0xFF92400E),
                                modifier = Modifier.testTag("detail_realtime_attendance_text")
                            )
                            if (volunteer.waktuCheckIn != null) {
                                Text(
                                    text = "Timestamp waktu_checkin: ${VolunteerPrivacyHelper.formatWibDateTime(volunteer.waktuCheckIn)}",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF475569)
                                )
                            }
                            if (volunteer.waktuCheckOut != null) {
                                Text(
                                    text = "Timestamp waktu_checkout: ${VolunteerPrivacyHelper.formatWibDateTime(volunteer.waktuCheckOut)}",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF475569)
                                )
                            }
                        }
                    }

                    // Dua Tombol Besar: [ CHECK-IN POSKO ] (Hijau) & [ CHECK-OUT POSKO ] (Merah)
                    Button(
                        onClick = onCheckInPosko,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 50.dp)
                            .testTag("btn_detail_checkin_posko")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Login,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "[ CHECK-IN POSKO ]",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }

                    Button(
                        onClick = onCheckOutPosko,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 50.dp)
                            .testTag("btn_detail_checkout_posko")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "[ CHECK-OUT POSKO ]",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }

                    // Tombol "Unduh Surat Tugas" (PDF)
                    Button(
                        onClick = onDownloadSuratTugas,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("btn_detail_download_surat_tugas")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Unduh Surat Tugas (PDF)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }
            }
        },
        confirmButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}

/**
 * DIALOG PRATINJAU & UNDUH SURAT TUGAS DIGITAL (PDF)
 * Menampilkan desain minimalis Surat Tugas Relawan (Kop Surat BPBD Mukomuko,
 * Judul "SURAT TUGAS RELAWAN", Data Relawan, Lokasi Penugasan, Tanggal, dan Footer Tanda Tangan Digital).
 */
@Composable
private fun SuratTugasDigitalPreviewDialog(
    volunteer: VolunteerEntity,
    officerName: String,
    onDismiss: () -> Unit,
    onDownloadPdfFile: () -> Unit
) {
    val letterNumber = remember(volunteer) {
        volunteer.suratTugasNumber.ifBlank {
            VolunteerAssignmentPdfGenerator.buildLetterNumber(volunteer.nik)
        }
    }
    val dateStr = remember {
        SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")).format(Date())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("dialog_preview_surat_tugas_pdf"),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Surat Tugas Digital (PDF)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A)
                )
            }
        },
        text = {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color.White,
                border = BorderStroke(1.5.dp, Color(0xFF0F172A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .padding(14.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Kop Surat BPBD Mukomuko
                    Text(
                        text = "PEMERINTAH KABUPATEN MUKOMUKO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "BADAN PENANGGULANGAN BENCANA DAERAH",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "PUSDALOPS-PB KABUPATEN MUKOMUKO",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFEA580C),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    HorizontalDivider(thickness = 2.dp, color = Color(0xFF0F172A))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Judul "SURAT TUGAS RELAWAN"
                    Text(
                        text = "SURAT TUGAS RELAWAN",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Nomor: $letterNumber",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF475569)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Data Relawan
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Nama Lengkap : ${volunteer.fullName}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("NIK : ${volunteer.nik}", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        Text("Organisasi : ${volunteer.organization}", fontSize = 11.sp)
                        Text("Keahlian : ${volunteer.skillsList().joinToString(", ")}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEA580C))
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text("Lokasi Posko : ${volunteer.effectivePoskoName()}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("Kecamatan : Kec. ${volunteer.effectivePoskoDistrict()}, Kab. Mukomuko", fontSize = 11.sp)
                        Text("Shift Bertugas : ${volunteer.assignedShift.ifBlank { "Shift Pagi (08:00 - 16:00 WIB)" }}", fontSize = 11.sp)
                        Text("Tanggal : $dateStr", fontSize = 11.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Footer Tanda Tangan Digital Pusdalops
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFECFDF5),
                        border = BorderStroke(1.dp, Color(0xFF10B981)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "DITANDATANGANI SECARA DIGITAL — PUSDALOPS-PB BPBD MUKOMUKO",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF065F46),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Pejabat Penugas: ${officerName.uppercase()} • ID: ${volunteer.id}",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF047857)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDownloadPdfFile,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                modifier = Modifier.testTag("btn_confirm_download_surat_tugas_pdf")
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Unduh Surat Tugas (PDF)", fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}

private fun copyVolunteerPhotoToInternalStorage(
    context: Context,
    sourceUri: Uri,
    prefix: String
): String? {
    return try {
        val dir = File(context.filesDir, "volunteer_attachments").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val targetFile = File(dir, "${prefix}_${stamp}.jpg")
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            FileOutputStream(targetFile).use { output ->
                input.copyTo(output)
            }
        }
        targetFile.absolutePath
    } catch (_: Exception) {
        sourceUri.toString()
    }
}
