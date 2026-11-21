package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Archive
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import coil.compose.AsyncImage
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.ImageNotSupported
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.OrangeOnPrimary
import com.example.ui.theme.OrangePrimary
import com.example.data.local.AppDatabase
import com.example.data.local.Personnel
import com.example.data.local.OfficerAuthManager
import com.example.data.local.OfficerCredentialEntity
import com.example.data.local.OfficerProfile
import com.example.data.local.OfficerRoles
import com.example.data.local.OfficialAsnBpbdPersonnel
import com.example.data.repository.PersonnelRepository
import com.example.data.repository.SmartOfficerAuthRepository
import com.example.model.CitizenDisasterReport
import com.example.model.District
import com.example.security.DeviceSecuritySession
import com.example.security.OfficerLoginActivity
import com.example.security.PlayIntegritySecurityManager
import com.example.security.SecurityManager
import com.example.security.UserRole
import androidx.compose.material.icons.filled.Settings
import com.example.util.GoogleMapsHelper
import com.example.util.ReportExporter
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Modul Petugas Dashboard Aman (Khusus Staf Pusdalops BPBD & Tim Reaksi Cepat TRC).
 * Fitur:
 * 1. Filter status, kecamatan, dan multi-kategori bencana (Banjir, Cuaca Ekstrem, Keamanan Pesisir, dll.)
 * 2. Auto-Pin lokasi tepat di peta mini interaktif saat meninjau laporan
 * 3. Grafik batang visualisasi statistik tren bulanan kategori bencana
 * 4. Ekspor dokumen resmi ke PDF (arsip digital) dan CSV (analisis Excel)
 * 5. Modul 'Arsip' untuk memisahkan laporan berstatus 'Selesai'
 * 6. Validasi status ('Diproses', 'Divalidasi', 'Selesai') dengan toast notifikasi
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OfficerDashboardConsoleView(
    reports: List<CitizenDisasterReport>,
    currentDistrict: District,
    isOfficerLoggedIn: Boolean,
    officerName: String,
    officerBadgeId: String,
    officerShift: String,
    officerRole: String = OfficerRoles.PETUGAS_JAGA,
    onLoginClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onUpdateReportStatus: (String, String) -> Unit,
    onUpdateReportStatusAndNotes: ((String, String, String) -> Unit)? = null,
    onDeleteReport: ((String) -> Unit)? = null,
    onViewPhoto: (String, CitizenDisasterReport) -> Unit,
    onSyncReports: (((Int) -> Unit) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Multi-Tier RBAC BPBD Kabupaten Mukomuko (Tier 1 Manajer, Tier 2 Pengawas/Koordinator, Tier 3 Operator/Lapangan)
    val currentUserRole = remember(officerRole) { UserRole.fromRoleString(officerRole) }
    val isManajerRole = UserRole.isManajer(officerRole)
    val canAccessSystemSettings = UserRole.canAccessSystemSettings(officerRole)
    val canValidateReports = UserRole.canValidateReports(officerRole)
    val canViewStatistics = UserRole.canViewStatistics(officerRole)
    val canDeleteReports = UserRole.canDeleteReports(officerRole)

    // View Mode: 0 = Laporan/Antrean Aktif, 1 = Modul Arsip (Selesai), 2 = Grafik Statistik, 3 = Pengaturan Sistem/Sync, 4 = Catatan
    var dashboardSubTab by remember { mutableStateOf(0) }
    var reportPendingDeletion by remember { mutableStateOf<CitizenDisasterReport?>(null) }

    LaunchedEffect(currentUserRole) {
        if (dashboardSubTab == 3 && !canAccessSystemSettings) {
            dashboardSubTab = 0
        } else if ((dashboardSubTab == 1 || dashboardSubTab == 2) && !canViewStatistics) {
            dashboardSubTab = 0
        }
    }

    // Filters State
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf("Semua") } // "Semua", "Diproses", "Divalidasi", "Selesai"
    var selectedCategoryFilter by remember { mutableStateOf("Semua Kategori") }
    var isCategoryDropdownExpanded by remember { mutableStateOf(false) }
    var selectedDistrictFilter by remember { mutableStateOf("Semua Kecamatan") }
    var isDistrictDropdownExpanded by remember { mutableStateOf(false) }
    var onlyWithCasualties by remember { mutableStateOf(false) }

    // Real-Time Alert (Notifikasi Tanpa Henti) State & Detector untuk Laporan Warga Baru (Pending)
    val realtimeNewReportDetector = remember { OfficerRealtimeNewReportDetector.sharedSessionDetector }
    var unacknowledgedAlertQueue by remember {
        mutableStateOf(realtimeNewReportDetector.getUnacknowledgedAlerts())
    }
    var activeRealtimeAlertReport by remember {
        mutableStateOf(unacknowledgedAlertQueue.firstOrNull())
    }
    var realtimeCloudReports by remember {
        mutableStateOf<List<CitizenDisasterReport>>(emptyList())
    }

    val liveReports = remember(reports, realtimeCloudReports) {
        if (realtimeCloudReports.isEmpty()) {
            reports.sortedByDescending { it.timestamp }
        } else {
            val combinedMap = LinkedHashMap<String, CitizenDisasterReport>()
            for (local in reports) {
                combinedMap[local.id] = local
            }
            for (remote in realtimeCloudReports) {
                val existing = combinedMap[remote.id]
                if (existing == null) {
                    combinedMap[remote.id] = remote
                } else {
                    val bestPhoto = com.example.util.ImageUtils.selectBestPhotoUri(remote.photoUri, existing.photoUri)
                    val remoteRank = com.example.model.CitizenReportStatusHelper.statusRank(remote.status)
                    val localRank = com.example.model.CitizenReportStatusHelper.statusRank(existing.status)
                    val winner = if (remote.effectiveUpdatedAt() >= existing.effectiveUpdatedAt() || remoteRank > localRank) {
                        remote
                    } else {
                        existing
                    }
                    combinedMap[remote.id] = winner.copy(
                        photoUri = bestPhoto,
                        isSyncedToCloud = true
                    )
                }
            }
            combinedMap.values.sortedByDescending { it.timestamp }
        }
    }

    val handleIncomingPendingReportsStream: (List<CitizenDisasterReport>) -> Unit = { incomingList ->
        val newlyDetected = realtimeNewReportDetector.onIncomingPendingReports(incomingList)
        val currentQueue = realtimeNewReportDetector.getUnacknowledgedAlerts()
        if (currentQueue.isNotEmpty()) {
            unacknowledgedAlertQueue = currentQueue
            if (activeRealtimeAlertReport == null) {
                activeRealtimeAlertReport = currentQueue.firstOrNull()
            }
        }
        if (newlyDetected != null) {
            activeRealtimeAlertReport = newlyDetected
            if (unacknowledgedAlertQueue.none { it.id == newlyDetected.id }) {
                unacknowledgedAlertQueue = listOf(newlyDetected) + unacknowledgedAlertQueue
            }
        }
    }

    // StreamBuilder equivalent: Real-Time Firestore SnapshotListener on citizen_disaster_reports
    // filtered by status Pending and ordered by timestamp descending, plus full collection stream
    DisposableEffect(isOfficerLoggedIn) {
        val pendingReportsListener = com.example.data.local.FirestoreHelper.listenPendingCitizenReportsRealtime(
            onUpdate = { pendingReports ->
                if (pendingReports.isNotEmpty()) {
                    val existingById = realtimeCloudReports.associateBy { it.id }.toMutableMap()
                    pendingReports.forEach { r -> existingById[r.id] = r }
                    realtimeCloudReports = existingById.values.sortedByDescending { it.timestamp }
                }
                handleIncomingPendingReportsStream(pendingReports)
                onSyncReports?.invoke { _ -> }
            }
        )
        val allReportsListener = com.example.data.local.FirestoreHelper.listenCitizenReportsRealtime(
            onUpdate = { allRemote ->
                realtimeCloudReports = allRemote
                handleIncomingPendingReportsStream(allRemote)
                onSyncReports?.invoke { _ -> }
            }
        )
        onDispose {
            pendingReportsListener?.remove()
            allReportsListener?.remove()
            OfficerAlarmPlayer.stopActiveRingtone()
        }
    }

    // Active Officer Session Fast Heartbeat Sync (every 4 seconds while Ruang Kerja is open)
    LaunchedEffect(isOfficerLoggedIn) {
        if (isOfficerLoggedIn) {
            while (isActive) {
                onSyncReports?.invoke { _ -> }
                delay(4_000L)
            }
        }
    }

    // Auto-Pin Location on Mini Map State & Sinkronisasi Detektor Laporan Baru dari Flow Repository
    var pinnedReportForMap by remember {
        mutableStateOf<CitizenDisasterReport?>(
            liveReports.firstOrNull { !com.example.model.CitizenReportStatusHelper.isResolved(it.status) }
                ?: liveReports.firstOrNull()
        )
    }
    LaunchedEffect(liveReports) {
        handleIncomingPendingReportsStream(liveReports)
        val currentPinnedId = pinnedReportForMap?.id
        pinnedReportForMap = if (currentPinnedId != null) {
            liveReports.find { it.id == currentPinnedId }
                ?: liveReports.firstOrNull { !com.example.model.CitizenReportStatusHelper.isResolved(it.status) }
                ?: liveReports.firstOrNull()
        } else {
            liveReports.firstOrNull { !com.example.model.CitizenReportStatusHelper.isResolved(it.status) }
                ?: liveReports.firstOrNull()
        }
    }

    // Sync State
    var isSyncing by remember { mutableStateOf(false) }

    // Officer Notes Dialog State
    var noteEditingReport by remember { mutableStateOf<CitizenDisasterReport?>(null) }
    var followUpNoteText by remember { mutableStateOf("") }
    var followUpStatusTarget by remember { mutableStateOf("") }

    // Change Officer PIN State (Security Guard against unauthorized ID/NIP usage)
    val officerAuthManager = remember { OfficerAuthManager(context) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var hasCustomPin by remember { mutableStateOf(officerAuthManager.hasCustomPin()) }

    // Server-Side Admin Auth Dialog & Temporary Access Token State (protects Kelola Personil, Database Explorer, Cabut Akses, & Sync)
    var isAdminAuthDialogVisible by remember { mutableStateOf(false) }
    var adminAuthActionTitle by remember { mutableStateOf("") }
    var pendingAdminAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var activeAdminToken by remember {
        mutableStateOf(com.example.security.AdminAccessSecurityManager.getActiveTokenForOfficer(officerBadgeId))
    }

    // Room Database & Personnel Repository
    val coroutineScope = rememberCoroutineScope()

    val requestAdminProtectedAction: (String, () -> Unit) -> Unit = { title, action ->
        val validToken = com.example.security.AdminAccessSecurityManager.getActiveTokenForOfficer(officerBadgeId)
        activeAdminToken = validToken
        if (validToken != null) {
            // Token Akses Sementara (5–15 menit) masih aktif: catat Audit Log pemakaian ulang & langsung buka
            coroutineScope.launch {
                com.example.security.AdminAccessSecurityManager.recordTokenReuseAttempt(
                    context = context,
                    officerNip = officerBadgeId,
                    officerName = officerName,
                    officerRole = officerRole,
                    actionTitle = title,
                    token = validToken
                )
            }
            action()
        } else {
            adminAuthActionTitle = title
            pendingAdminAction = action
            isAdminAuthDialogVisible = true
        }
    }
    val database = remember { AppDatabase.getInstance(context) }
    val personnelRepository = remember { PersonnelRepository(database.personnelDao()) }
    val dbPersonnelList by personnelRepository.allPersonnel.collectAsStateWithLifecycle(initialValue = emptyList())

    // Central SyncController across 5 devices
    val syncController = remember { com.example.data.sync.SyncController.getInstance(context, database) }
    val latencyMs by syncController.latencyMs.collectAsStateWithLifecycle()
    val isSlowNetwork by syncController.isSlowNetwork.collectAsStateWithLifecycle()
    val activeDevices by syncController.activeDevices.collectAsStateWithLifecycle()
    val syncState by syncController.syncState.collectAsStateWithLifecycle()
    val conflictLogs by database.syncConflictLogDao().getAllConflictLogs().collectAsStateWithLifecycle(initialValue = emptyList())

    var showDatabaseExplorer by remember { mutableStateOf(false) }
    var showFormLaporanHarian by remember { mutableStateOf(false) }
    var showDalaModule by remember { mutableStateOf(false) }
    var integrityCheckResult by remember { mutableStateOf<com.example.data.sync.IntegrityCheckResult?>(null) }
    var isCheckingIntegrity by remember { mutableStateOf(false) }
    var editingPersonnel by remember { mutableStateOf<com.example.data.local.Personnel?>(null) }

    // Enterprise Play Integrity SecurityManager & Backend Firestore Sessions + Login Activities
    val securityManager = remember { SecurityManager.getInstance(context) }
    var securitySessionsList by remember { mutableStateOf<List<DeviceSecuritySession>>(emptyList()) }
    var loginActivitiesList by remember { mutableStateOf<List<OfficerLoginActivity>>(emptyList()) }
    var isSyncingSecurityActivities by remember { mutableStateOf(false) }
    var showSecuritySessionsDialog by remember { mutableStateOf(false) }
    var revokedNipsInSession by remember { mutableStateOf<Set<String>>(emptySet()) }
    var currentFirebaseUid by remember {
        mutableStateOf(
            try { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid } catch (_: Exception) { null }
        )
    }

    val knownOfficialNips = remember {
        OfficialAsnBpbdPersonnel.ALL_OFFICIAL_PERSONNEL.map { it.nip.trim() }.filter { it.isNotBlank() }
    }

    suspend fun runFullSecurityActivitySync() {
        isSyncingSecurityActivities = true
        try {
            if (isOfficerLoggedIn && officerBadgeId.isNotBlank()) {
                val normalizedNip = officerBadgeId.trim().lowercase()
                if (!revokedNipsInSession.contains(normalizedNip)) {
                    securityManager.syncPendingSessionToFirestore(
                        officerNip = officerBadgeId,
                        officerName = officerName,
                        officerRole = officerRole
                    )
                }
            }
            val auditLogs = try {
                database.auditLogDao().getAllAuditLogsList()
            } catch (_: Exception) {
                emptyList()
            }
            val (sessions, activities) = securityManager.syncAllOfficerLoginActivities(
                auditLogs = auditLogs,
                knownOfficialNips = knownOfficialNips,
                officerNipFilter = "",
                activeDeviceHeartbeats = activeDevices
            )
            securitySessionsList = sessions.filterNot {
                revokedNipsInSession.contains(it.officerNip.trim().lowercase())
            }
            loginActivitiesList = activities
        } finally {
            isSyncingSecurityActivities = false
        }
    }

    DisposableEffect(Unit) {
        val auth = try { com.google.firebase.auth.FirebaseAuth.getInstance() } catch (_: Exception) { null }
        val authListener = com.google.firebase.auth.FirebaseAuth.AuthStateListener { firebaseAuth ->
            currentFirebaseUid = firebaseAuth.currentUser?.uid
        }
        auth?.addAuthStateListener(authListener)
        onDispose {
            auth?.removeAuthStateListener(authListener)
        }
    }

    DisposableEffect(currentFirebaseUid, isOfficerLoggedIn, officerBadgeId) {
        val sessionListener = securityManager.listenSecuritySessionsRealtime(
            officerNipFilter = "",
            activeOfficerNipForRevocation = officerBadgeId,
            onActiveSessionRevoked = {
                if (isOfficerLoggedIn) {
                    Toast.makeText(
                        context,
                        "Akses sesi perangkat Anda telah dicabut dari server. Logout paksa dijalankan.",
                        Toast.LENGTH_LONG
                    ).show()
                    securityManager.clearLocalSession()
                    com.example.data.auth.AuthManager.getInstance(context).signOut()
                    officerAuthManager.logoutOfficer()
                    onLogoutClick()
                }
            }
        ) { list ->
            securitySessionsList = securityManager.reconcileWithActiveHeartbeats(
                sessions = list,
                heartbeats = activeDevices
            ).filterNot {
                revokedNipsInSession.contains(it.officerNip.trim().lowercase())
            }
        }
        val activityListener = securityManager.listenLoginActivitiesRealtime { activities ->
            loginActivitiesList = activities
        }
        onDispose {
            sessionListener?.remove()
            activityListener?.remove()
        }
    }

    LaunchedEffect(activeDevices) {
        if (activeDevices.isNotEmpty()) {
            securitySessionsList = securityManager.reconcileWithActiveHeartbeats(
                sessions = securitySessionsList,
                heartbeats = activeDevices
            ).filterNot {
                revokedNipsInSession.contains(it.officerNip.trim().lowercase())
            }
        }
    }

    LaunchedEffect(currentFirebaseUid, isOfficerLoggedIn, officerBadgeId, dashboardSubTab) {
        onSyncReports?.invoke { _ -> }
        if (isOfficerLoggedIn && officerBadgeId.isNotBlank()) {
            syncController.sendHeartbeatNow()
        }
        runFullSecurityActivitySync()
    }

    LaunchedEffect(Unit) {
        personnelRepository.seedDefaultAsnPersonnelIfNeeded()
        personnelRepository.startAutoSyncFromCloud(this)
    }

    // 30-Minute Inactivity Auto-Logout Timer for Officer Security
    var lastActiveTimeMs by remember { mutableStateOf(System.currentTimeMillis()) }
    val autoLogoutThresholdMs = 30 * 60 * 1000L // 30 minutes

    LaunchedEffect(isOfficerLoggedIn) {
        if (isOfficerLoggedIn) {
            while (isActive) {
                delay(15_000L) // Poll every 15 seconds
                val idleDuration = System.currentTimeMillis() - lastActiveTimeMs
                if (idleDuration >= autoLogoutThresholdMs) {
                    Toast.makeText(
                        context,
                        "Sesi konsol petugas berakhir otomatis setelah 30 menit tidak aktif demi keamanan data.",
                        Toast.LENGTH_LONG
                    ).show()
                    onLogoutClick()
                    break
                }
            }
        }
    }

    // Pusdalops Officer Roster Management State backed directly by Room Database & Session Manager
    var showRosterDialog by remember { mutableStateOf(false) }
    var rosterList by remember { mutableStateOf(officerAuthManager.getOfficerRoster()) }
    val activeRosterList = remember(dbPersonnelList) {
        dbPersonnelList.map { p ->
            OfficerProfile(
                id = p.id.toString(),
                fullName = p.name,
                nip = p.nip,
                role = p.role,
                unit = p.unit,
                shift = p.shift,
                phone = p.phone
            )
        }
    }

    LaunchedEffect(dbPersonnelList) {
        if (dbPersonnelList.isNotEmpty()) {
            dbPersonnelList.forEach { p ->
                officerAuthManager.addOfficer(p.name, p.nip, p.role, p.shift, p.unit, p.phone)
            }
        }
        rosterList = officerAuthManager.getOfficerRoster()
    }

    // Quick Stats Calculation (Canonical Normalized Status)
    val totalCount = liveReports.size
    val pendingCount = liveReports.count { com.example.model.CitizenReportStatusHelper.isPending(it.status) }
    val validatedCount = liveReports.count { com.example.model.CitizenReportStatusHelper.isValidated(it.status) }
    val resolvedCount = liveReports.count { com.example.model.CitizenReportStatusHelper.isResolved(it.status) }
    val totalDead = liveReports.sumOf { it.casualtiesDead }
    val totalInjured = liveReports.sumOf { it.casualtiesInjured }
    val totalMissing = liveReports.sumOf { it.casualtiesMissing }

    val categoryOptions = listOf(
        "Semua Kategori",
        "Banjir",
        "Cuaca Ekstrem",
        "Keamanan Pesisir",
        "Gempa Bumi",
        "Tanah Longsor",
        "Pohon Tumbang",
        "Kebakaran Hutan/Lahan",
        "Lainnya"
    )

    val districtOptions = listOf("Semua Kecamatan") + listOf(
        "Kota Mukomuko", "Ipuh", "Teramang Jaya", "Penarik", "Lubuk Pinang",
        "Pondok Suguh", "Air Dikit", "Selagan Raya", "XIV Koto", "Malin Deman",
        "V Koto", "Teras Terunjam", "Sungai Rumbai", "Air Rami", "Air Manjunto"
    )

    // Filtered Reports
    val baseReports = when (dashboardSubTab) {
        1 -> liveReports.filter { com.example.model.CitizenReportStatusHelper.isResolved(it.status) } // Modul Arsip
        else -> liveReports.filter { !com.example.model.CitizenReportStatusHelper.isResolved(it.status) } // Antrean Aktif
    }

    val filteredReports = baseReports.filter { rep ->
        val matchesStatus = com.example.model.CitizenReportStatusHelper.matchesFilter(rep.status, selectedStatusFilter)

        val matchesCategory = if (selectedCategoryFilter == "Semua Kategori") true else {
            rep.disasterType.contains(selectedCategoryFilter, ignoreCase = true) ||
            (selectedCategoryFilter == "Keamanan Pesisir" && (rep.disasterType.contains("Abrasi", ignoreCase = true) || rep.disasterType.contains("Gelombang", ignoreCase = true))) ||
            (selectedCategoryFilter == "Cuaca Ekstrem" && (rep.disasterType.contains("Angin", ignoreCase = true) || rep.disasterType.contains("Badai", ignoreCase = true)))
        }

        val matchesDistrict = if (selectedDistrictFilter == "Semua Kecamatan") true else rep.district.equals(selectedDistrictFilter, ignoreCase = true)

        val matchesCasualties = if (onlyWithCasualties) {
            (rep.casualtiesDead > 0 || rep.casualtiesInjured > 0 || rep.casualtiesMissing > 0)
        } else true

        val matchesSearch = if (searchQuery.isBlank()) true else {
            val q = searchQuery.trim().lowercase()
            rep.id.lowercase().contains(q) ||
            rep.disasterType.lowercase().contains(q) ||
            rep.district.lowercase().contains(q) ||
            rep.village.lowercase().contains(q) ||
            rep.incidentLocation.lowercase().contains(q) ||
            rep.reporterName.lowercase().contains(q) ||
            rep.additionalNotes.lowercase().contains(q)
        }

        matchesStatus && matchesCategory && matchesDistrict && matchesCasualties && matchesSearch
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent()
                        lastActiveTimeMs = System.currentTimeMillis()
                    }
                }
            }
            .testTag("officer_dashboard_console_view"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 0. REAL-TIME RED ALERT BANNER ("🚨 LAPORAN WARGA BARU! Segera Cek!")
        activeRealtimeAlertReport?.let { alertReport ->
            item(key = "realtime_red_alert_${alertReport.id}") {
                OfficerRealtimeRedAlertBanner(
                    alertReport = alertReport,
                    unacknowledgedCount = unacknowledgedAlertQueue.size.coerceAtLeast(1),
                    onInspectAndAcknowledge = { targetReport ->
                        realtimeNewReportDetector.markReportAsRead(targetReport.id)
                        pinnedReportForMap = targetReport
                        dashboardSubTab = 0
                        selectedStatusFilter = "Semua"
                        selectedCategoryFilter = "Semua Kategori"
                        selectedDistrictFilter = "Semua Kecamatan"
                        onlyWithCasualties = false
                        val remaining = unacknowledgedAlertQueue.filterNot { it.id == targetReport.id }
                        unacknowledgedAlertQueue = remaining
                        activeRealtimeAlertReport = remaining.firstOrNull()
                        Toast.makeText(
                            context,
                            "Membuka & memfokuskan laporan baru ${targetReport.id} di Peta & Antrean Aktif.",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    onDismissAlarm = { targetReport ->
                        realtimeNewReportDetector.markReportAsRead(targetReport.id)
                        val remaining = unacknowledgedAlertQueue.filterNot { it.id == targetReport.id }
                        unacknowledgedAlertQueue = remaining
                        activeRealtimeAlertReport = remaining.firstOrNull()
                    }
                )
            }
        }

        // 1. OFFICER COMMAND BAR WITH EXPORT PDF & CSV BUTTONS
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isOfficerLoggedIn) Color(0xFF0F172A) else Color(0xFF1E293B)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("officer_command_bar")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Profile Header: Title RUANG KERJA sejajar dan presisi dengan Badge Sesi Resmi
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = "RUANG KERJA",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFFBBF24),
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "KONSOL PUSDALOPS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF64748B),
                                letterSpacing = 0.3.sp
                            )
                        }

                        if (isOfficerLoggedIn) {
                            // Badge Sesi Resmi sejajar dan presisi dengan tulisan Ruang Kerja
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF166534).copy(alpha = 0.85f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF22C55E)),
                                modifier = Modifier.heightIn(min = 32.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(Color(0xFF4ADE80), CircleShape)
                                        )
                                        Text(
                                            text = "Sesi Resmi",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF7C2D12).copy(alpha = 0.85f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDC2626)),
                                modifier = Modifier.heightIn(min = 32.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(Color(0xFFEF4444), CircleShape)
                                        )
                                        Text(
                                            text = "Terkunci",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Baris Badge Aksi: Mengisi kekosongan secara presisi dan terbagi sejajar dalam Ruang Kerja
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isOfficerLoggedIn) {
                            // TIER 1 KHUSUS MANAJER: Menu Rahasia "Kelola Personil" HANYA tampil jika role == Manajer
                            if (isManajerRole) {
                                OutlinedButton(
                                    onClick = {
                                        requestAdminProtectedAction("Kelola Direktori Personil BPBD") {
                                            rosterList = officerAuthManager.getOfficerRoster()
                                            showRosterDialog = true
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF60A5FA)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2563EB)),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 36.dp)
                                        .testTag("btn_rbac_kelola_personil")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Badge,
                                        contentDescription = "Kelola Personil BPBD",
                                        tint = Color(0xFF60A5FA),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Kelola Personil",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // TIER 1 KHUSUS MANAJER: Menu "Pengaturan Sistem" & Sinkronisasi Sistem
                            if (canAccessSystemSettings) {
                                OutlinedButton(
                                    onClick = {
                                        requestAdminProtectedAction("Pengaturan Sistem & Keamanan Perangkat") {
                                            dashboardSubTab = 3
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0284C7)),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 36.dp)
                                        .testTag("btn_rbac_pengaturan_sistem")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Pengaturan Sistem",
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Pengaturan Sistem",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // Ubah PIN
                            OutlinedButton(
                                onClick = { showChangePinDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFBBF24)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD97706)),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Ganti PIN",
                                    tint = Color(0xFFFBBF24),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Ubah PIN",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Keluar
                            OutlinedButton(
                                onClick = onLogoutClick,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFCA5A5)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF991B1B)),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LockOpen,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Keluar",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        } else {
                            Button(
                                onClick = onLoginClick,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Badge,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "Verifikasi PIN", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                        }
                    }

                    // Posisi Nama Petugas sejajar di bawah badge dalam card ruang kerja
                    if (isOfficerLoggedIn) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF0F172A).copy(alpha = 0.55f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF0284C7).copy(alpha = 0.2f),
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = Color(0xFF38BDF8),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "Petugas Jaga Aktif:",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF64748B)
                                        )
                                        Text(
                                            text = officerName,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    val nipPrefix = if (officerBadgeId.startsWith("TRC", ignoreCase = true)) "Reg TRC" else "NIP"
                                    Text(
                                        text = "$nipPrefix: $officerBadgeId • $officerRole • $officerShift • BPBD Kab. Mukomuko",
                                        fontSize = 11.sp,
                                        color = Color(0xFF94A3B8),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(currentUserRole.badgeColorHex).copy(alpha = 0.22f),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            Color(currentUserRole.badgeColorHex)
                                        ),
                                        modifier = Modifier.testTag("badge_rbac_tier")
                                    ) {
                                        Text(
                                            text = "${currentUserRole.tierCode} — ${currentUserRole.tierDisplayName}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Autentikasi Petugas Diperlukan • Akses Konsol Khusus Petugas BPBD",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }

                    if (isOfficerLoggedIn) {
                        // Security banner if officer is still using initial NIP key without custom personal PIN
                        if (!hasCustomPin) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF78350F).copy(alpha = 0.6f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight()
                                    .clickable { showChangePinDialog = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = Color(0xFFFBBF24),
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "PIN Anda masih menggunakan kunci awal 6-digit NIP. Buat PIN pribadi sekarang agar tersimpan aman di database & Cloud Firestore untuk login multi-perangkat.",
                                            fontSize = 11.sp,
                                            color = Color(0xFFFEF3C7),
                                            lineHeight = 15.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Ubah ➜",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFBBF24)
                                    )
                                }
                            }
                        }

                        // MENU OPERASIONAL (DALA & FORM HARIAN) + EKSPOR (TIER 1 & TIER 2)
                        val isExportAuthorized = isOfficerLoggedIn && UserRole.canExportReports(officerRole)
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
                        Spacer(modifier = Modifier.height(12.dp))

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Modul Pasca Bencana / DALA (Tersedia untuk Tier 1, Tier 2, Tier 3)
                            Button(
                                onClick = { showDalaModule = true },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .heightIn(min = 40.dp)
                                    .testTag("btn_open_dala_module")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Assignment,
                                    contentDescription = "Buka Modul Pasca Bencana DALA",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "Pasca Bencana / DALA", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            // Input Form Laporan Harian (Operasional untuk Tier 1, Tier 2, Tier 3)
                            Button(
                                onClick = { showFormLaporanHarian = true },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .heightIn(min = 40.dp)
                                    .testTag("btn_open_form_harian")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Buka Form Laporan Harian",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "Form Harian", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            if (isExportAuthorized) {
                                // PDF Export Button (Tier 1 & Tier 2)
                                Button(
                                    onClick = {
                                        val exportList = reports.filter { it.status.equals("Divalidasi", ignoreCase = true) || it.status.equals("Selesai", ignoreCase = true) }
                                        val finalExport = if (exportList.isNotEmpty()) exportList else reports
                                        ReportExporter.exportToPdf(context, finalExport, officerName, officerBadgeId)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.heightIn(min = 40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PictureAsPdf,
                                        contentDescription = "Ekspor PDF",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "Ekspor PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                // CSV Export Button (Tier 1 & Tier 2)
                                Button(
                                    onClick = {
                                        ReportExporter.exportToCsv(context, reports, officerName, officerBadgeId)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.heightIn(min = 40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.TableChart,
                                        contentDescription = "Ekspor CSV",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "Ekspor CSV (Excel)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // SLOW NETWORK WARNING BANNER
        if (isSlowNetwork) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFEF3C7),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("alert_slow_network")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFB45309),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "⚠️ PERINGATAN: JARINGAN LAMBAT (${latencyMs}ms)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF92400E)
                            )
                            Text(
                                text = "Waktu respon API Firestore melebihi batas 3000ms. Sinkronisasi antar 5 perangkat dialihkan ke antrean retry otomatis berlatar belakang.",
                                fontSize = 11.sp,
                                color = Color(0xFFB45309)
                            )
                        }
                    }
                }
            }
        }

        // 2. SUB-TABS BERDASARKAN TIER RBAC BPBD MUKOMUKO:
        // - Tier 1 (Manajer): Laporan, Arsip, Statistik, Pengaturan Sistem, Catatan
        // - Tier 2 (Pengawas/Supervisor/Koord. Administrasi): Laporan, Arsip, Statistik, Catatan (Pengaturan Sistem disembunyikan)
        // - Tier 3 (Operator/Logistik/TRC/null): Laporan, Catatan (Statistik, Validasi, Kelola Personil, Pengaturan Sistem disembunyikan)
        item {
            val visibleSubTabs = buildList {
                add(0 to "🚨 Laporan (${pendingCount + validatedCount})")
                if (canViewStatistics) {
                    add(1 to "🗄️ Arsip ($resolvedCount)")
                    add(2 to "📊 Statistik")
                }
                if (canAccessSystemSettings) {
                    add(3 to "⚙️ Sistem (${activeDevices.size})")
                }
                add(4 to "📝 Catatan")
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                visibleSubTabs.forEach { (idx, label) ->
                    val isSel = dashboardSubTab == idx
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSel) Color.White else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                if (idx == 3) {
                                    if (canAccessSystemSettings && dashboardSubTab != 3) {
                                        requestAdminProtectedAction("Pengaturan Sistem & Sync Multi-Perangkat") {
                                            dashboardSubTab = 3
                                        }
                                    }
                                } else {
                                    dashboardSubTab = idx
                                }
                            }
                            .testTag("dashboard_subtab_$idx")
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSel) Color(0xFF0F172A) else Color(0xFF64748B),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(vertical = 7.dp, horizontal = 2.dp)
                        )
                    }
                }
            }
        }

        // 3. STATISTICAL BAR CHART VIEW (TIER 1 & TIER 2 ONLY)
        if (dashboardSubTab == 2 && canViewStatistics) {
            item {
                DisasterMonthlyBarChartCard(reports = reports)
            }
        }

        // 3B. CATATAN OPERASIONAL PUSDALOPS (IF SELECTED)
        if (dashboardSubTab == 4) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(680.dp)
                        .testTag("card_pusdalops_notes_container")
                ) {
                    PusdalopsNotesSection(
                        officerName = if (officerBadgeId.isNotBlank()) "$officerName (NIP. $officerBadgeId)" else officerName,
                        officerRole = officerRole,
                        currentDistrict = currentDistrict.name,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // 3C. PENGATURAN SISTEM & DIAGNOSTIK SINKRONISASI (TIER 1 MANAJER ONLY)
        if (dashboardSubTab == 3 && canAccessSystemSettings) {
            item {
                SyncDiagnosticsSection(
                    latencyMs = latencyMs,
                    isSlowNetwork = isSlowNetwork,
                    activeDevices = activeDevices,
                    conflictLogs = conflictLogs,
                    integrityResult = integrityCheckResult,
                    isCheckingIntegrity = isCheckingIntegrity,
                    securitySessions = securitySessionsList,
                    loginActivities = loginActivitiesList,
                    isCloudLinked = !currentFirebaseUid.isNullOrBlank(),
                    isSyncingSecurity = isSyncingSecurityActivities,
                    onSyncSecurityNow = {
                        coroutineScope.launch {
                            runFullSecurityActivitySync()
                            Toast.makeText(
                                context,
                                "Sinkronisasi ${securitySessionsList.size} sesi perangkat & ${loginActivitiesList.size} aktivitas login selesai.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    onLinkGoogleCloud = {
                        coroutineScope.launch {
                            val authRes = com.example.data.auth.AuthManager.getInstance(context).signInWithGoogle(context)
                            if (authRes.isSuccess) {
                                currentFirebaseUid = authRes.getOrNull()?.uid
                                runFullSecurityActivitySync()
                                Toast.makeText(
                                    context,
                                    "Google Cloud terhubung. Stream real-time seluruh aktivitas login petugas aktif.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    context,
                                    authRes.exceptionOrNull()?.message ?: "Gagal menghubungkan Google Cloud.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    },
                    onVerifySecuritySession = { session, approved, note ->
                        val actionLabel = if (approved) {
                            "Verifikasi Hardware & Izinkan (${session.officerName})"
                        } else {
                            "Blokir Akses Perangkat (${session.officerName})"
                        }
                        requestAdminProtectedAction(actionLabel) {
                            coroutineScope.launch {
                                val res = securityManager.verifyDeviceSessionByAdmin(
                                    session = session,
                                    adminName = officerName.ifBlank { "Admin Pusdalops" },
                                    approved = approved,
                                    note = note
                                )
                                res.getOrNull()?.let { updated ->
                                    securitySessionsList = securitySessionsList.map {
                                        if (it.officerNip.equals(updated.officerNip, ignoreCase = true)) updated else it
                                    }
                                    loginActivitiesList = securityManager.fetchLoginActivities()
                                    Toast.makeText(
                                        context,
                                        if (approved) {
                                            "Hardware & token ${updated.officerName} diverifikasi SAH oleh Admin."
                                        } else {
                                            "Akses perangkat ${updated.officerName} berhasil diblokir oleh Admin."
                                        },
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    },
                    onRevokeSecuritySession = { session ->
                        val actionLabel = "Cabut Akses Perangkat (${session.officerName.ifBlank { session.officerNip }})"
                        requestAdminProtectedAction(actionLabel) {
                            val normalizedNip = session.officerNip.trim().lowercase()
                            val isSelfSession = officerBadgeId.trim().equals(session.officerNip.trim(), ignoreCase = true)
                            revokedNipsInSession = revokedNipsInSession + normalizedNip
                            securitySessionsList = securitySessionsList.filterNot {
                                it.officerNip.trim().equals(session.officerNip.trim(), ignoreCase = true)
                            }
                            coroutineScope.launch {
                                securityManager.deleteSecuritySession(
                                    officerNip = session.officerNip,
                                    purgeActivities = false,
                                    revokedByAdmin = officerName.ifBlank { "Admin Pusdalops" }
                                )
                                loginActivitiesList = securityManager.fetchLoginActivities()
                                if (isSelfSession) {
                                    securityManager.clearLocalSession()
                                    com.example.data.auth.AuthManager.getInstance(context).signOut()
                                    officerAuthManager.logoutOfficer()
                                    Toast.makeText(
                                        context,
                                        "Akses sesi Anda (${session.officerName}) dicabut. Logout dijalankan.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    onLogoutClick()
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Akses sesi ${session.officerName.ifBlank { "Petugas" }} (NIP ${session.officerNip}) berhasil dicabut dari server.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    },
                    onRunIntegrityCheck = {
                        isCheckingIntegrity = true
                        coroutineScope.launch {
                            val res = com.example.data.sync.IntegrityCheckWorker.performCheck(context, database)
                            integrityCheckResult = res
                            isCheckingIntegrity = false
                            Toast.makeText(context, "Pengecekan Integritas selesai. Konsisten: ${res.isConsistent}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onTestPing = {
                        coroutineScope.launch {
                            val lat = syncController.measureLatency()
                            Toast.makeText(context, "Latensi saat ini: ${lat}ms", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onExportConflictLogsCsv = {
                        coroutineScope.launch {
                            val csv = com.example.data.sync.SyncConflictHandler(context, database).exportConflictLogsToCsv()
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Log_Konflik_Sync_BPBD.csv")
                                putExtra(Intent.EXTRA_TEXT, csv)
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Bagikan Log Konflik CSV"))
                        }
                    },
                    onOpenDatabaseExplorer = {
                        requestAdminProtectedAction("Database Explorer (Room SQLite)") {
                            showDatabaseExplorer = true
                        }
                    },
                    onOpenFormLaporanHarian = {
                        showFormLaporanHarian = true
                    }
                )
            }
        }

        // 4. REAL-TIME KPI CARDS
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Metrik Triage & Validasi Laporan",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = "$totalCount Total Aduan",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF64748B)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    KpiMetricCard(
                        title = "Diproses",
                        count = pendingCount,
                        badgeColor = Color(0xFFD97706),
                        bgColor = Color(0xFFFFFBEB),
                        borderColor = Color(0xFFFDE68A),
                        subtitle = "Butuh Validasi",
                        modifier = Modifier.weight(1f).wrapContentHeight()
                    )
                    KpiMetricCard(
                        title = "Divalidasi",
                        count = validatedCount,
                        badgeColor = Color(0xFF16A34A),
                        bgColor = Color(0xFFF0FDF4),
                        borderColor = Color(0xFFBBF7D0),
                        subtitle = "Tim TRC Meluncur",
                        modifier = Modifier.weight(1f).wrapContentHeight()
                    )
                    KpiMetricCard(
                        title = "Selesai",
                        count = resolvedCount,
                        badgeColor = Color(0xFF2563EB),
                        bgColor = Color(0xFFEFF6FF),
                        borderColor = Color(0xFFBFDBFE),
                        subtitle = "Tuntas / Arsip",
                        modifier = Modifier.weight(1f).wrapContentHeight()
                    )
                }
            }
        }

        // 5. AUTO-PIN MINI-MAP CARD FOR INSPECTING INCIDENTS
        if (pinnedReportForMap != null) {
            val rep = pinnedReportForMap!!
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Box(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(end = 76.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MyLocation,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Auto-Pin Peta Lokasi Tinjauan Laporan",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF0284C7),
                                modifier = Modifier.align(Alignment.TopEnd)
                            ) {
                                Text(
                                    text = "#${rep.id.takeLast(7)}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Interactive Mini Map Canvas with Pin & Radar Pulse
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E293B))
                                .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                        ) {
                            MiniMapWithPinCanvas(
                                latitude = rep.latitude,
                                longitude = rep.longitude,
                                disasterType = rep.disasterType
                            )

                            // Coords & District Overlay
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color.Black.copy(alpha = 0.6f)
                                ) {
                                    Text(
                                        text = "${rep.village}, Kec. ${rep.district} • ${rep.disasterType}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color.Black.copy(alpha = 0.65f)
                                    ) {
                                        Text(
                                            text = "GPS: %.4f, %.4f (±%.0fm)".format(rep.latitude, rep.longitude, rep.gpsAccuracyMeters ?: 5f),
                                            fontSize = 11.sp,
                                            color = Color(0xFF86EFAC),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            GoogleMapsHelper.openNavigationRoute(
                                                context = context,
                                                destLat = rep.latitude,
                                                destLon = rep.longitude,
                                                destName = "Bencana ${rep.disasterType} - ${rep.village}"
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.heightIn(min = 28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Navigation,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("Rute Maps", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. SEARCH & MULTI-CATEGORY FILTERS
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Search box
                    val searchKeyboardActions = rememberFormKeyboardActions()
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .formScrollPadding()
                            .testTag("officer_search_input"),
                        placeholder = { Text("Cari ID, jenis bencana, desa, atau pelapor...", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Hapus pencarian",
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = formDoneKeyboardOptions(),
                        keyboardActions = searchKeyboardActions,
                        shape = RoundedCornerShape(10.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedIndicatorColor = Color(0xFF15803D),
                            unfocusedIndicatorColor = Color(0xFFCBD5E1)
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Disaster Category Filter Chips (Scrollable)
                    Text(
                        text = "Kategori Bencana:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF475569)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(categoryOptions) { cat ->
                            val isSel = selectedCategoryFilter == cat
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSel) Color(0xFF0F172A) else Color.White,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSel) Color(0xFF0F172A) else Color(0xFFCBD5E1)
                                ),
                                modifier = Modifier.clickable { selectedCategoryFilter = cat }
                            ) {
                                Text(
                                    text = cat,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) Color.White else Color(0xFF334155),
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // District Selector Dropdown & Casualties Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White,
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                modifier = Modifier.clickable { isDistrictDropdownExpanded = !isDistrictDropdownExpanded }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = Color(0xFF15803D),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = selectedDistrictFilter,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF1E293B)
                                    )
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (onlyWithCasualties) Color(0xFFFEF2F2) else Color.White,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (onlyWithCasualties) Color(0xFFDC2626) else Color(0xFFCBD5E1)
                            ),
                            modifier = Modifier.clickable { onlyWithCasualties = !onlyWithCasualties }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (onlyWithCasualties) Color(0xFFDC2626) else Color(0xFF64748B),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Ada Korban",
                                    fontSize = 11.sp,
                                    fontWeight = if (onlyWithCasualties) FontWeight.Bold else FontWeight.Normal,
                                    color = if (onlyWithCasualties) Color(0xFF991B1B) else Color(0xFF475569)
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = isDistrictDropdownExpanded) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 3.dp,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 200.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                districtOptions.forEach { dist ->
                                    DropdownMenuItem(
                                        text = { Text(dist, fontSize = 11.sp) },
                                        onClick = {
                                            selectedDistrictFilter = dist
                                            isDistrictDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 7. REPORTS LIST WITH ACTION BUTTONS & STATUS TOASTS
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (dashboardSubTab == 1) "Daftar Arsip Laporan Selesai" else "Antrean Penanganan Bencana Aktif",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "${filteredReports.size} laporan",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
            }
        }

        if (filteredReports.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 32.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (dashboardSubTab == 1) "Belum Ada Laporan yang Diarsipkan" else "Tidak Ada Laporan Aktif",
                            fontWeight = FontWeight.Normal,
                            fontSize = 13.sp,
                            color = Color(0xFF1E293B)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Semua laporan dalam kriteria ini telah ditangani atau periksa kata kunci pencarian.",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(filteredReports, key = { it.id }) { report ->
                OfficerIncidentReportCard(
                    report = report,
                    isOfficerLoggedIn = isOfficerLoggedIn,
                    officerName = officerName,
                    officerRole = officerRole,
                    onInspectMapPin = {
                        pinnedReportForMap = report
                        Toast.makeText(context, "Pin peta dipusatkan ke #${report.id.takeLast(7)} (${report.village})", Toast.LENGTH_SHORT).show()
                    },
                    onUpdateStatus = { targetStatus ->
                        if (isOfficerLoggedIn) {
                            if (targetStatus.equals("Divalidasi", ignoreCase = true)) {
                                if (!UserRole.canValidateReports(officerRole)) {
                                    Toast.makeText(
                                        context,
                                        "Akses Ditolak (RBAC): Tombol Validasi Laporan hanya untuk Manajer, Pengawas, Supervisor, atau Koord. Administrasi.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                } else {
                                    onUpdateReportStatus(report.id, targetStatus)
                                    Toast.makeText(
                                        context,
                                        "Status laporan #${report.id.takeLast(7)} divalidasi oleh $officerName ($officerRole)",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } else if (targetStatus.equals("Selesai", ignoreCase = true)) {
                                if (!UserRole.canCompleteReports(officerRole)) {
                                    Toast.makeText(
                                        context,
                                        "Akses Ditolak (RBAC): Penyelesaian laporan hanya untuk Tier 1 (Manajer) atau Tier 2 (Pengawas/Supervisor/Koord. Administrasi).",
                                        Toast.LENGTH_LONG
                                    ).show()
                                } else {
                                    onUpdateReportStatus(report.id, targetStatus)
                                    Toast.makeText(
                                        context,
                                        "Status laporan #${report.id.takeLast(7)} diperbarui menjadi '$targetStatus' oleh $officerName ($officerRole)",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } else {
                                onUpdateReportStatus(report.id, targetStatus)
                                Toast.makeText(
                                    context,
                                    "Status laporan #${report.id.takeLast(7)} diperbarui menjadi '$targetStatus' oleh $officerName",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else {
                            onLoginClick()
                        }
                    },
                    onDeleteReport = if (canDeleteReports && onDeleteReport != null) {
                        {
                            reportPendingDeletion = report
                        }
                    } else null,
                    onOpenNoteEditor = {
                        if (isOfficerLoggedIn) {
                            noteEditingReport = report
                            followUpNoteText = report.additionalNotes
                            followUpStatusTarget = report.status
                        } else {
                            onLoginClick()
                        }
                    },
                    onViewPhoto = { photoUri ->
                        onViewPhoto(photoUri, report)
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // 7. DIALOG GANTI & RESET PIN PETUGAS PUSDALOPS (DILENGKAPI PLAY INTEGRITY CLASSIC)
    if (showChangePinDialog) {
        var pinDialogMode by remember { mutableIntStateOf(0) } // 0 = Ganti PIN, 1 = Reset PIN
        var oldPinInput by remember { mutableStateOf("") }
        var newPinInput by remember { mutableStateOf("") }
        var confirmPinInput by remember { mutableStateOf("") }
        var resetAdminInput by remember { mutableStateOf("") }
        var isOldPinVisible by remember { mutableStateOf(false) }
        var isNewPinVisible by remember { mutableStateOf(false) }
        var isConfirmPinVisible by remember { mutableStateOf(false) }
        var isResetAdminVisible by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        var isProcessingSecurity by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isProcessingSecurity) showChangePinDialog = false },
            icon = {
                Surface(
                    shape = CircleShape,
                    color = if (pinDialogMode == 0) Color(0xFFFEF3C7) else Color(0xFFEFF6FF),
                    modifier = Modifier.size(50.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (pinDialogMode == 0) Icons.Default.Lock else Icons.Default.Shield,
                            contentDescription = null,
                            tint = if (pinDialogMode == 0) Color(0xFFD97706) else Color(0xFF2563EB),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            },
            title = {
                Text(
                    text = if (pinDialogMode == 0) "Ganti PIN / Sandi Petugas" else "Reset PIN Petugas BPBD",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                val pinKeyboardActions = rememberFormKeyboardActions()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Petugas: $officerName (ID/NIP: $officerBadgeId)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Dilindungi Play Integrity API (Mode Classic) untuk mencegah pembajakan sesi dan pemalsuan kredensial akun dinas.",
                        fontSize = 11.sp,
                        color = Color(0xFF475569),
                        lineHeight = 14.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Tab Selector: Ganti PIN vs Reset PIN
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (pinDialogMode == 0) Color.White else Color.Transparent,
                            shadowElevation = if (pinDialogMode == 0) 2.dp else 0.dp,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    pinDialogMode = 0
                                    errorMessage = null
                                }
                        ) {
                            Text(
                                text = "Ganti PIN",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (pinDialogMode == 0) Color(0xFF0F172A) else Color(0xFF64748B),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (pinDialogMode == 1) Color.White else Color.Transparent,
                            shadowElevation = if (pinDialogMode == 1) 2.dp else 0.dp,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    pinDialogMode = 1
                                    errorMessage = null
                                }
                        ) {
                            Text(
                                text = "Reset PIN",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (pinDialogMode == 1) Color(0xFFDC2626) else Color(0xFF64748B),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (pinDialogMode == 0) {
                        // MODE GANTI PIN
                        OutlinedTextField(
                            value = oldPinInput,
                            onValueChange = {
                                oldPinInput = it
                                errorMessage = null
                            },
                            label = { Text("PIN / Sandi Lama", fontSize = 11.sp) },
                            placeholder = { Text("Masukkan PIN lama Anda", fontSize = 11.sp) },
                            visualTransformation = if (isOldPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                            keyboardActions = pinKeyboardActions,
                            trailingIcon = {
                                IconButton(onClick = { isOldPinVisible = !isOldPinVisible }) {
                                    Icon(
                                        imageVector = if (isOldPinVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .formScrollPadding()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = newPinInput,
                            onValueChange = {
                                newPinInput = it
                                errorMessage = null
                            },
                            label = { Text("PIN / Sandi Baru (Min. 4 Digit)", fontSize = 11.sp) },
                            placeholder = { Text("Buat PIN baru yang aman", fontSize = 11.sp) },
                            visualTransformation = if (isNewPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                            keyboardActions = pinKeyboardActions,
                            trailingIcon = {
                                IconButton(onClick = { isNewPinVisible = !isNewPinVisible }) {
                                    Icon(
                                        imageVector = if (isNewPinVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .formScrollPadding()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = confirmPinInput,
                            onValueChange = {
                                confirmPinInput = it
                                errorMessage = null
                            },
                            label = { Text("Konfirmasi PIN Baru", fontSize = 11.sp) },
                            placeholder = { Text("Ulangi PIN baru", fontSize = 11.sp) },
                            visualTransformation = if (isConfirmPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                            keyboardActions = pinKeyboardActions,
                            trailingIcon = {
                                IconButton(onClick = { isConfirmPinVisible = !isConfirmPinVisible }) {
                                    Icon(
                                        imageVector = if (isConfirmPinVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .formScrollPadding()
                        )
                    } else {
                        // MODE RESET PIN
                        Text(
                            text = "Reset PIN mengembalikan sandi otorisasi ke kunci individual awal 6-digit NIP Anda (tanpa melalui PIN default umum '117'). Masukkan Sandi Administrator atau PIN lama untuk konfirmasi.",
                            fontSize = 11.sp,
                            color = Color(0xFF475569),
                            lineHeight = 15.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = resetAdminInput,
                            onValueChange = {
                                resetAdminInput = it
                                errorMessage = null
                            },
                            label = { Text("Sandi Administrator / PIN Lama", fontSize = 11.sp) },
                            placeholder = { Text("Masukkan sandi admin / PIN lama", fontSize = 11.sp) },
                            visualTransformation = if (isResetAdminVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                            keyboardActions = pinKeyboardActions,
                            trailingIcon = {
                                IconButton(onClick = { isResetAdminVisible = !isResetAdminVisible }) {
                                    Icon(
                                        imageVector = if (isResetAdminVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .formScrollPadding()
                        )
                    }

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = errorMessage.orEmpty(),
                            color = Color(0xFFDC2626),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isProcessingSecurity) return@Button
                        if (pinDialogMode == 0) {
                            if (oldPinInput.isBlank()) {
                                errorMessage = "Mohon isi PIN / sandi lama Anda saat ini."
                                return@Button
                            }
                            if (newPinInput.length < 4) {
                                errorMessage = "PIN baru harus minimal 4 karakter / angka."
                                return@Button
                            }
                            if (newPinInput != confirmPinInput) {
                                errorMessage = "Konfirmasi PIN baru tidak sama."
                                return@Button
                            }
                            if (SmartOfficerAuthRepository.FORBIDDEN_DEFAULT_PINS.any { it.equals(newPinInput.trim(), ignoreCase = true) }) {
                                errorMessage = "PIN baru dilarang menggunakan PIN default umum (117 / TRC2026 / dsb)."
                                return@Button
                            }
                            isProcessingSecurity = true
                            coroutineScope.launch {
                                val integrityResult = PlayIntegritySecurityManager.getInstance(context)
                                    .verifyCriticalAction(officerBadgeId.ifBlank { "OFFICER" }, "CHANGE_PIN")
                                val smartRepo = SmartOfficerAuthRepository.getInstance(context)
                                val result = smartRepo.changePersonalPin(officerBadgeId, oldPinInput, newPinInput)
                                isProcessingSecurity = false
                                result.onSuccess { msg ->
                                    officerAuthManager.markCloudCustomPinStatus(true)
                                    hasCustomPin = true
                                    showChangePinDialog = false
                                    Toast.makeText(context, "$msg (${integrityResult.verdict})", Toast.LENGTH_LONG).show()
                                }.onFailure { err ->
                                    errorMessage = err.message ?: "Gagal memperbarui PIN."
                                }
                            }
                        } else {
                            val trimmedResetInput = resetAdminInput.trim()
                            if (trimmedResetInput.isBlank()) {
                                errorMessage = "Ketikkan Sandi Administrator atau PIN lama untuk reset."
                                return@Button
                            }
                            isProcessingSecurity = true
                            coroutineScope.launch {
                                val integrityResult = PlayIntegritySecurityManager.getInstance(context)
                                    .verifyCriticalAction(officerBadgeId.ifBlank { "OFFICER" }, "RESET_PIN")
                                val smartRepo = SmartOfficerAuthRepository.getInstance(context)
                                val isAdminAuthorized = securityManager.verifyMasterAdminPin(
                                    inputPin = trimmedResetInput,
                                    officerNip = officerBadgeId,
                                    officerName = officerName,
                                    officerRole = officerRole,
                                    actionTitle = "Reset PIN Petugas ($officerBadgeId)"
                                ).getOrDefault(false)
                                val isSelfPinAuthorized = if (!isAdminAuthorized) {
                                    smartRepo.verifyOfficerPinAgainstCloud(officerBadgeId, trimmedResetInput)
                                } else {
                                    false
                                }
                                if (!isAdminAuthorized && !isSelfPinAuthorized) {
                                    isProcessingSecurity = false
                                    errorMessage = "Verifikasi Sandi Administrator ke server atau PIN pribadi lama ditolak. Percobaan dicatat di Audit Log."
                                    return@launch
                                }
                                val result = smartRepo.resetToInitialNipSecret(officerBadgeId)
                                isProcessingSecurity = false
                                result.onSuccess { msg ->
                                    officerAuthManager.markCloudCustomPinStatus(false)
                                    hasCustomPin = false
                                    showChangePinDialog = false
                                    Toast.makeText(context, "$msg (${integrityResult.verdict})", Toast.LENGTH_LONG).show()
                                }.onFailure { err ->
                                    errorMessage = err.message ?: "Otorisasi reset PIN gagal."
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (pinDialogMode == 0) Color(0xFF15803D) else Color(0xFFDC2626)),
                    enabled = !isProcessingSecurity
                ) {
                    if (isProcessingSecurity) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Memverifikasi...")
                    } else {
                        Text(if (pinDialogMode == 0) "Simpan PIN Baru" else "Reset ke Kunci NIP")
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showChangePinDialog = false },
                    enabled = !isProcessingSecurity
                ) {
                    Text("Batal")
                }
            }
        )
    }

    // 7A. DIALOG AUTENTIKASI ADMIN SERVER-SIDE (POST /api/verify-admin-pass + TOKEN SEMENTARA + AUDIT LOG)
    if (isAdminAuthDialogVisible) {
        AdminAuthDialog(
            actionTitle = adminAuthActionTitle,
            officerNip = officerBadgeId,
            officerName = officerName,
            officerRole = officerRole,
            onDismiss = {
                isAdminAuthDialogVisible = false
                pendingAdminAction = null
            },
            onAuthorized = { issuedToken ->
                activeAdminToken = issuedToken
                isAdminAuthDialogVisible = false
                val action = pendingAdminAction
                pendingAdminAction = null
                action?.invoke()
            }
        )
    }

    // 7B. DIALOG MANAJEMEN PERSONIL PUSDALOPS & TRC BPBD (WAJIB TEMPORARY ACCESS TOKEN AKTIF)
    if (showRosterDialog && isManajerRole) {
        val currentToken = com.example.security.AdminAccessSecurityManager.getActiveTokenForOfficer(officerBadgeId)
        if (currentToken == null) {
            LaunchedEffect(Unit) {
                activeAdminToken = null
                showRosterDialog = false
                Toast.makeText(
                    context,
                    "Token akses sementara telah kedaluwarsa. Silakan verifikasi ulang ke server.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        var rosterMode by remember { mutableStateOf(0) } // 0 = Daftar Personil, 1 = Form Input Personil Baru
        var selectedRoleTab by remember { mutableStateOf("Semua") }
        var rosterSearchQuery by remember { mutableStateOf("") }

        // Form Input Personil Baru State
        var inputFullName by remember { mutableStateOf("") }
        var inputNip by remember { mutableStateOf("") }
        var inputRole by remember { mutableStateOf(OfficerRoles.PETUGAS_JAGA) }
        var inputShift by remember { mutableStateOf("Shift Siaga 24 Jam") }
        var inputUnit by remember { mutableStateOf("Pusdalops BPBD Kab. Mukomuko") }
        var inputPhone by remember { mutableStateOf("") }
        var formError by remember { mutableStateOf<String?>(null) }

        val filteredList = rosterList.filter { off ->
            val matchesRole = if (selectedRoleTab == "Semua") true else off.role.contains(selectedRoleTab, ignoreCase = true)
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
            onDismissRequest = { showRosterDialog = false },
            icon = {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFEFF6FF),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (rosterMode == 1) Icons.Default.PersonAdd else Icons.Default.Badge,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            },
            title = {
                Text(
                    text = if (rosterMode == 1) "Input Personil Pusdalops Baru" else "Direktori Personil Pusdalops & TRC",
                    fontWeight = FontWeight.Normal,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                val rosterKeyboardActions = rememberFormKeyboardActions()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 460.dp)
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                ) {
                    if (rosterMode == 0) {
                        val validTokenBanner = com.example.security.AdminAccessSecurityManager.getActiveTokenForOfficer(officerBadgeId)
                        if (validTokenBanner != null) {
                            val remMin = (validTokenBanner.remainingSeconds() / 60L).coerceAtLeast(0L)
                            val remSec = validTokenBanner.remainingSeconds() % 60L
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF0FDF4),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                                    .testTag("banner_admin_temp_token")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 7.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Token Sementara Aktif • Sisa ${remMin}m ${remSec}d",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF15803D)
                                    )
                                    Text(
                                        text = "Kunci Sesi",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFDC2626),
                                        modifier = Modifier
                                            .clickable {
                                                com.example.security.AdminAccessSecurityManager.revokeToken()
                                                activeAdminToken = null
                                                showRosterDialog = false
                                                Toast.makeText(context, "Token akses sementara dikunci.", Toast.LENGTH_SHORT).show()
                                            }
                                            .testTag("btn_revoke_admin_token")
                                    )
                                }
                            }
                        }

                        Text(
                            text = "Daftar resmi nama personil ASN & TRC BPBD Kabupaten Mukomuko yang bertugas sebagai Operator, Petugas Jaga, dan Personil Lapangan di Pusdalops.",
                            fontSize = 11.sp,
                            color = Color(0xFF4B5563),
                            lineHeight = 15.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Search and Add Button Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedTextField(
                                value = rosterSearchQuery,
                                onValueChange = { rosterSearchQuery = it },
                                placeholder = { Text("Cari nama/NIP personil...", fontSize = 11.sp) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                singleLine = true,
                                keyboardOptions = formDoneKeyboardOptions(),
                                keyboardActions = rosterKeyboardActions,
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .formScrollPadding()
                            )

                            Button(
                                onClick = {
                                    rosterMode = 1
                                    formError = null
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PersonAdd,
                                    contentDescription = "Tambah Petugas",
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+ Input", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Category Filter Chips
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val roleTabs = listOf(
                                "Semua" to "Semua (${rosterList.size})",
                                "Pengawas" to "Pengawas (${rosterList.count { it.role.contains("Pengawas", ignoreCase = true) }})",
                                "Manajer" to "Manajer Pusdalops (${rosterList.count { it.role.contains("Manajer", ignoreCase = true) }})",
                                "Supervisor" to "Supervisor (${rosterList.count { it.role.contains("Supervisor", ignoreCase = true) }})",
                                OfficerRoles.OPERATOR to "Operator (${rosterList.count { it.role.contains("Operator", ignoreCase = true) }})",
                                OfficerRoles.PETUGAS_JAGA to "Piket (${rosterList.count { it.role.contains("Piket", ignoreCase = true) || it.role.contains("Jaga", ignoreCase = true) }})",
                                OfficerRoles.PETUGAS_LAPANGAN to "Staff Logistik (${rosterList.count { it.role.contains("Lapangan", ignoreCase = true) || it.role.contains("Logistik", ignoreCase = true) || it.role.contains("TRC", ignoreCase = true) }})"
                            )
                            items(roleTabs) { (roleKey, label) ->
                                val isSelected = selectedRoleTab == roleKey
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isSelected) Color(0xFF2563EB) else Color(0xFFF1F5F9),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) Color(0xFF1D4ED8) else Color(0xFFCBD5E1)
                                    ),
                                    modifier = Modifier.clickable { selectedRoleTab = roleKey }
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

                        Spacer(modifier = Modifier.height(10.dp))

                        // Personil Cards
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (filteredList.isEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "Belum Ada Personil Terdaftar",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color(0xFF334155)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Daftar personil Room Database masih bersih. Tekan tombol '+ Input' di atas untuk mendaftarkan Operator, Piket Siaga, Staff Logistik, atau Manajer Pusdalops PB.",
                                            fontSize = 11.sp,
                                            color = Color(0xFF64748B),
                                            textAlign = TextAlign.Center,
                                            lineHeight = 15.sp
                                        )
                                    }
                                }
                            } else {
                                filteredList.forEach { off ->
                                    val (roleBg, roleColor) = com.example.data.local.OfficialAsnBpbdPersonnel.getRoleBadgeColor(off.role)
                                    val roleBorder = roleColor.copy(alpha = 0.35f)
                                    val roleIcon = when {
                                        off.role.contains("Pengawas", ignoreCase = true) || off.role.contains("Kalaksa", ignoreCase = true) -> Icons.Default.AdminPanelSettings
                                        off.role.contains("Supervisor", ignoreCase = true) || off.role.contains("Kabid", ignoreCase = true) -> Icons.Default.Shield
                                        off.role.contains("Koord", ignoreCase = true) || off.role.contains("Administrasi", ignoreCase = true) -> Icons.Default.Badge
                                        off.role.contains("Manajer", ignoreCase = true) -> Icons.Default.AdminPanelSettings
                                        off.role.contains("Operator", ignoreCase = true) -> Icons.Default.HeadsetMic
                                        off.role.contains("Logistik", ignoreCase = true) || off.role.contains("TRC", ignoreCase = true) -> Icons.Default.DirectionsCar
                                        else -> Icons.Default.Badge
                                    }
                                    val roleBadgeLabel = off.role

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color.White,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = roleBg,
                                                border = androidx.compose.foundation.BorderStroke(1.dp, roleBorder),
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = roleIcon,
                                                        contentDescription = null,
                                                        tint = roleColor,
                                                        modifier = Modifier.size(19.dp)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(10.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = off.fullName,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF0F172A)
                                                )
                                                val nipLabel = if (off.nip.startsWith("TRC", ignoreCase = true)) "Reg TRC" else "NIP"
                                                Text(
                                                    text = "$nipLabel: ${off.nip}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFF334155)
                                                )
                                                Text(
                                                    text = "${off.unit} • ${off.shift}",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF64748B)
                                                )
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(top = 3.dp)
                                                ) {
                                                    Surface(
                                                        shape = RoundedCornerShape(20.dp),
                                                        color = roleBg,
                                                        border = androidx.compose.foundation.BorderStroke(0.8.dp, roleBorder)
                                                    ) {
                                                        Text(
                                                            text = roleBadgeLabel.uppercase(),
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = roleColor,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.5.dp),
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                            softWrap = false
                                                        )
                                                    }
                                                    if (off.phone.isNotBlank()) {
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            text = "📞 ${off.phone}",
                                                            fontSize = 11.sp,
                                                            color = Color(0xFF475569)
                                                        )
                                                    }
                                                }
                                            }

                                            // Edit button for Admin: allows editing personnel details
                                            IconButton(
                                                onClick = {
                                                    val matchInDb = dbPersonnelList.firstOrNull { it.nip.equals(off.nip, ignoreCase = true) }
                                                    editingPersonnel = matchInDb ?: com.example.data.local.Personnel(
                                                        name = off.fullName,
                                                        nip = off.nip,
                                                        role = off.role,
                                                        unit = off.unit,
                                                        shift = off.shift,
                                                        phone = off.phone
                                                    )
                                                },
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .testTag("edit_officer_btn")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Edit Personil",
                                                    tint = Color(0xFF0284C7),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }

                                            // Delete button for Admin: allows removing any personnel from Room & session cache
                                            IconButton(
                                                onClick = {
                                                    coroutineScope.launch {
                                                        val matchInDb = dbPersonnelList.firstOrNull { it.nip.equals(off.nip, ignoreCase = true) }
                                                        if (matchInDb != null) {
                                                            personnelRepository.deleteById(matchInDb.id)
                                                        }
                                                        officerAuthManager.deleteOfficer(off.id)
                                                        rosterList = officerAuthManager.getOfficerRoster()
                                                        Toast.makeText(
                                                            context,
                                                            "Personil '${off.fullName}' berhasil dihapus dari direktori.",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                },
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .testTag("delete_officer_btn")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Hapus Personil",
                                                    tint = Color(0xFFDC2626),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Reset / Bersihkan direktori personil
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    personnelRepository.clearAll()
                                    officerAuthManager.resetRosterToDefault()
                                    rosterList = officerAuthManager.getOfficerRoster()
                                    Toast.makeText(context, "Direktori personil telah dibersihkan.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Bersihkan Semua Data Direktori Personil", fontSize = 11.sp)
                        }

                    } else {
                        // MODE 1: FORM INPUT PERSONIL BARU KE ROOM DATABASE
                        Text(
                            text = "Input data personil baru Pusdalops BPBD Kabupaten Mukomuko ke Room Database lokal. Wajib mengisi Nama Lengkap dan NIP ASN atau No. Registrasi TRC.",
                            fontSize = 11.sp,
                            color = Color(0xFF4B5563),
                            lineHeight = 15.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = inputFullName,
                            onValueChange = {
                                inputFullName = it
                                formError = null
                            },
                            label = { Text("Nama Petugas & Gelar *", fontSize = 11.sp) },
                            placeholder = { Text("Contoh: Ramdani Pratama, S.T.", fontSize = 11.sp) },
                            singleLine = true,
                            keyboardOptions = formNextKeyboardOptions(),
                            keyboardActions = rosterKeyboardActions,
                            modifier = Modifier
                                .fillMaxWidth()
                                .formScrollPadding()
                                .testTag("personnel_input_name")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = inputNip,
                            onValueChange = {
                                inputNip = it
                                formError = null
                            },
                            label = { Text("NIP Pegawai ASN / No. Reg TRC *", fontSize = 11.sp) },
                            placeholder = { Text("Contoh: 19850101 201001 1 001 atau TRC-MMK-2024-001", fontSize = 11.sp) },
                            singleLine = true,
                            keyboardOptions = formNextKeyboardOptions(),
                            keyboardActions = rosterKeyboardActions,
                            modifier = Modifier
                                .fillMaxWidth()
                                .formScrollPadding()
                                .testTag("personnel_input_nip")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Penugasan Kedinasan di Pusdalops:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF334155)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            val roleChoices = listOf(
                                Triple(OfficerRoles.PENGAWAS, "🏛️ Pengawas BPBD", "Pimpinan pengawasan siaga & koordinasi komando kebijakan bencana"),
                                Triple(OfficerRoles.MANAJER, "⭐ Manajer Pusdalops PB", "Penanggung jawab koordinasi operasional komando penanggulangan bencana"),
                                Triple(OfficerRoles.SUPERVISOR, "🛡️ Supervisor BPBD", "Supervisi kesiapsiagaan posko darurat dan pemantauan personil"),
                                Triple(OfficerRoles.KOORD_ADMINISTRASI, "📋 Koord. Administrasi", "Pengelolaan administrasi data posko dan logistik kebencanaan"),
                                Triple(OfficerRoles.OPERATOR, "🎧 Operator Pusdalops", "Pusat panggilan darurat 117, radio komunikasi, & data spasial"),
                                Triple(OfficerRoles.PETUGAS_JAGA, "🛡️ Petugas Jaga Siaga / Piket", "Piket Posko Tanggap Darurat Bencana 24 Jam"),
                                Triple(OfficerRoles.PETUGAS_LAPANGAN, "🚜 Staff Logistik / TRC", "Tim Reaksi Cepat evakuasi, penanganan lapangan & logistik")
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
                                keyboardOptions = formNextKeyboardOptions(),
                                keyboardActions = rosterKeyboardActions,
                                modifier = Modifier
                                    .weight(1f)
                                    .formScrollPadding()
                            )
                            OutlinedTextField(
                                value = inputUnit,
                                onValueChange = { inputUnit = it },
                                label = { Text("Unit / Satuan", fontSize = 11.sp) },
                                singleLine = true,
                                keyboardOptions = formNextKeyboardOptions(),
                                keyboardActions = rosterKeyboardActions,
                                modifier = Modifier
                                    .weight(1f)
                                    .formScrollPadding()
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = inputPhone,
                            onValueChange = { inputPhone = it },
                            label = { Text("No. Kontak / Radio HT (Opsional)", fontSize = 11.sp) },
                            placeholder = { Text("Contoh: 0812-3456-7890 / Ch-117", fontSize = 11.sp) },
                            singleLine = true,
                            keyboardOptions = formDoneKeyboardOptions(),
                            keyboardActions = rosterKeyboardActions,
                            modifier = Modifier
                                .fillMaxWidth()
                                .formScrollPadding()
                        )

                        if (formError != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = formError!!,
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
                                onClick = { rosterMode = 0 },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Batal", fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    // Validation using PersonnelRepository validation function
                                    val validationError = personnelRepository.validatePersonnelInput(inputFullName, inputNip)
                                    if (validationError != null) {
                                        formError = validationError
                                        return@Button
                                    }

                                    coroutineScope.launch {
                                        // 1. Otomatis simpan data petugas ke Room dan sinkron ke Cloud Firestore untuk seluruh perangkat
                                        personnelRepository.autoSaveAndSyncNewPersonnel(
                                            name = inputFullName.trim(),
                                            nip = inputNip.trim(),
                                            role = inputRole,
                                            unit = inputUnit.trim(),
                                            shift = inputShift.trim(),
                                            phone = inputPhone.trim()
                                        )

                                        // 2. Daftarkan ke officer auth manager
                                        val res = officerAuthManager.addOfficer(
                                            fullName = inputFullName.trim(),
                                            nip = inputNip.trim(),
                                            role = inputRole,
                                            shift = inputShift.trim(),
                                            unit = inputUnit.trim(),
                                            phone = inputPhone.trim()
                                        )

                                        if (res.isSuccess) {
                                            rosterList = officerAuthManager.getOfficerRoster()
                                            rosterMode = 0
                                            formError = null
                                            val savedName = inputFullName.trim()
                                            val savedNip = inputNip.trim()
                                            inputFullName = ""
                                            inputNip = ""
                                            inputPhone = ""
                                            Toast.makeText(
                                                context,
                                                "Petugas '$savedName' (NIP: $savedNip) otomatis tersimpan & tersinkron ke semua perangkat baru!",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            formError = res.exceptionOrNull()?.message ?: "Gagal mendaftarkan personil."
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                                modifier = Modifier.weight(1.3f)
                            ) {
                                Text("Simpan Personil", fontSize = 11.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showRosterDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Text("Selesai")
                }
            }
        )
    }

    // DIALOG EDIT PERSONIL OLEH ADMIN
    if (editingPersonnel != null) {
        val p = editingPersonnel!!
        var editName by remember(p) { mutableStateOf(p.name) }
        var editNip by remember(p) { mutableStateOf(p.nip) }
        var editRole by remember(p) { mutableStateOf(p.role) }
        var editShift by remember(p) { mutableStateOf(p.shift) }
        var editPhone by remember(p) { mutableStateOf(p.phone) }

        AlertDialog(
            onDismissRequest = { editingPersonnel = null },
            title = { Text("Edit Data Personil BPBD", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                val editPersonnelKeyboardActions = rememberFormKeyboardActions()
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Nama Lengkap") },
                        singleLine = true,
                        keyboardOptions = formNextKeyboardOptions(),
                        keyboardActions = editPersonnelKeyboardActions,
                        modifier = Modifier
                            .fillMaxWidth()
                            .formScrollPadding()
                            .testTag("input_edit_personnel_name")
                    )
                    OutlinedTextField(
                        value = editNip,
                        onValueChange = { editNip = it },
                        label = { Text("NIP / Reg TRC") },
                        singleLine = true,
                        keyboardOptions = formNextKeyboardOptions(),
                        keyboardActions = editPersonnelKeyboardActions,
                        modifier = Modifier
                            .fillMaxWidth()
                            .formScrollPadding()
                            .testTag("input_edit_personnel_nip")
                    )
                    OutlinedTextField(
                        value = editRole,
                        onValueChange = { editRole = it },
                        label = { Text("Jabatan / Peran") },
                        singleLine = true,
                        keyboardOptions = formNextKeyboardOptions(),
                        keyboardActions = editPersonnelKeyboardActions,
                        modifier = Modifier
                            .fillMaxWidth()
                            .formScrollPadding()
                            .testTag("input_edit_personnel_role")
                    )
                    OutlinedTextField(
                        value = editShift,
                        onValueChange = { editShift = it },
                        label = { Text("Shift Penugasan") },
                        singleLine = true,
                        keyboardOptions = formNextKeyboardOptions(),
                        keyboardActions = editPersonnelKeyboardActions,
                        modifier = Modifier
                            .fillMaxWidth()
                            .formScrollPadding()
                            .testTag("input_edit_personnel_shift")
                    )
                    OutlinedTextField(
                        value = editPhone,
                        onValueChange = { editPhone = it },
                        label = { Text("Nomor HP / WhatsApp") },
                        singleLine = true,
                        keyboardOptions = formDoneKeyboardOptions(),
                        keyboardActions = editPersonnelKeyboardActions,
                        modifier = Modifier
                            .fillMaxWidth()
                            .formScrollPadding()
                            .testTag("input_edit_personnel_phone")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val updated = p.copy(
                                name = editName.trim(),
                                nip = editNip.trim(),
                                role = editRole.trim(),
                                shift = editShift.trim(),
                                phone = editPhone.trim()
                            )
                            personnelRepository.update(updated)
                            officerAuthManager.updateOfficer(
                                OfficerProfile(
                                    id = "asn-${updated.nip}",
                                    fullName = updated.name,
                                    nip = updated.nip,
                                    role = updated.role,
                                    unit = updated.unit,
                                    shift = updated.shift,
                                    phone = updated.phone
                                )
                            )
                            editingPersonnel = null
                            Toast.makeText(context, "Data personil '${updated.name}' berhasil diperbarui.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                    modifier = Modifier.testTag("btn_save_edit_personnel")
                ) {
                    Text("Simpan Perubahan")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { editingPersonnel = null }) {
                    Text("Batal")
                }
            }
        )
    }

    // FULL SCREEN DIALOG DATABASE EXPLORER (PENGATURAN SISTEM - KHUSUS TIER 1 MANAJER)
    if (showDatabaseExplorer && canAccessSystemSettings) {
        Dialog(
            onDismissRequest = { showDatabaseExplorer = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            DatabaseExplorerScreen(
                database = database,
                onClose = { showDatabaseExplorer = false }
            )
        }
    }

    // DIALOG KONFIRMASI HAPUS LAPORAN (KHUSUS TIER 1 MANAJER - FULL ACCESS)
    if (reportPendingDeletion != null && canDeleteReports) {
        val targetDelete = reportPendingDeletion!!
        AlertDialog(
            onDismissRequest = { reportPendingDeletion = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Hapus Laporan",
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Hapus Laporan Warga?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            },
            text = {
                Text(
                    text = "Akses Tier 1 (Manajer): Laporan #${targetDelete.id.takeLast(7)} (${targetDelete.disasterType} - Desa ${targetDelete.village}) akan dihapus permanen dari database lokal dan Cloud Firestore.",
                    fontSize = 12.sp,
                    color = Color(0xFF334155)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteReport?.invoke(targetDelete.id)
                        Toast.makeText(
                            context,
                            "Laporan #${targetDelete.id.takeLast(7)} berhasil dihapus oleh Manajer $officerName.",
                            Toast.LENGTH_SHORT
                        ).show()
                        reportPendingDeletion = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    modifier = Modifier.testTag("btn_confirm_delete_report")
                ) {
                    Text("Ya, Hapus Permanen", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { reportPendingDeletion = null }) {
                    Text("Batal", fontSize = 11.sp)
                }
            }
        )
    }

    // FULL SCREEN DIALOG FORM LAPORAN HARIAN
    if (showFormLaporanHarian) {
        Dialog(
            onDismissRequest = { showFormLaporanHarian = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            FormLaporanHarianScreen(
                database = database,
                onClose = { showFormLaporanHarian = false }
            )
        }
    }

    // FULL SCREEN DIALOG MODUL PASCA BENCANA / DALA (KAJI CEPAT)
    if (showDalaModule) {
        Dialog(
            onDismissRequest = { showDalaModule = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            DalaAssessmentScreen(
                officerName = officerName,
                officerNip = officerBadgeId,
                onClose = { showDalaModule = false }
            )
        }
    }

    // 8. OFFICIAL OFFICER FOLLOW-UP NOTE EDITOR DIALOG
    if (noteEditingReport != null) {
        val activeRep = noteEditingReport!!
        AlertDialog(
            onDismissRequest = { noteEditingReport = null },
            icon = {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF15803D),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Assignment,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            },
            title = {
                Text(
                    text = "Log Tindak Lanjut TRC BPBD",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                val followUpKeyboardActions = rememberFormKeyboardActions()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Laporan: #${activeRep.id.takeLast(7)} • ${activeRep.disasterType}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF15803D)
                    )
                    Text(
                        text = "Lokasi: ${activeRep.village}, Kec. ${activeRep.district}",
                        fontSize = 11.sp,
                        color = Color(0xFF475569)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Ubah Status Operasional:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val allowedStatuses = if (canValidateReports) {
                        listOf("Diproses", "Divalidasi", "Selesai")
                    } else {
                        listOf("Diproses")
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        allowedStatuses.forEach { st ->
                            val isSel = followUpStatusTarget.equals(st, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) Color(0xFF15803D) else Color(0xFFF1F5F9),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSel) Color(0xFF15803D) else Color(0xFFCBD5E1)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { followUpStatusTarget = st }
                            ) {
                                Text(
                                    text = st,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) Color.White else Color(0xFF334155),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Catatan Tindak Lanjut Personel TRC:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = followUpNoteText,
                        onValueChange = { followUpNoteText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .formScrollPadding(),
                        keyboardOptions = formDoneKeyboardOptions(),
                        keyboardActions = followUpKeyboardActions,
                        placeholder = {
                            Text(
                                "Contoh: Regu TRC Posko Ipuh diterjunkan dengan 2 perahu karet & 1 unit genset. Logistik sembako disalurkan ke posko darurat balai desa.",
                                fontSize = 11.sp
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalStatus = com.example.model.CitizenReportStatusHelper.normalizeStatus(
                            followUpStatusTarget.ifBlank { activeRep.status }
                        )
                        val finalNote = followUpNoteText.trim()
                        if (onUpdateReportStatusAndNotes != null) {
                            onUpdateReportStatusAndNotes(activeRep.id, finalStatus, finalNote)
                        } else {
                            onUpdateReportStatus(activeRep.id, finalStatus)
                        }
                        Toast.makeText(
                            context,
                            "Log tindak lanjut tersimpan resmi oleh $officerName (NIP. $officerBadgeId)",
                            Toast.LENGTH_SHORT
                        ).show()
                        noteEditingReport = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D))
                ) {
                    Text("Simpan Tindak Lanjut", fontSize = 11.sp)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { noteEditingReport = null }) {
                    Text("Batal", fontSize = 11.sp)
                }
            }
        )
    }
}

/**
 * Visualisasi Grafik Batang Tren Bulanan Kategori Bencana
 */
@Composable
private fun DisasterMonthlyBarChartCard(
    reports: List<CitizenDisasterReport>,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = null,
                        tint = Color(0xFF15803D),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Grafik Tren Kategori Bencana Bulanan",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                }
                Text(
                    text = "Tahun 2026",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF64748B)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Categories tally
            val categories = listOf(
                Triple("Banjir", reports.count { it.disasterType.contains("Banjir", true) }, Color(0xFF2563EB)),
                Triple("Badai", reports.count { it.disasterType.contains("Angin", true) || it.disasterType.contains("Badai", true) }, Color(0xFFD97706)),
                Triple("Abrasi", reports.count { it.disasterType.contains("Gelombang", true) || it.disasterType.contains("Abrasi", true) }, Color(0xFF0284C7)),
                Triple("Longsor", reports.count { it.disasterType.contains("Longsor", true) }, Color(0xFFB45309)),
                Triple("Gempa", reports.count { it.disasterType.contains("Gempa", true) }, Color(0xFFDC2626)),
                Triple("Lainnya", reports.count { !it.disasterType.contains("Banjir", true) && !it.disasterType.contains("Angin", true) && !it.disasterType.contains("Gelombang", true) && !it.disasterType.contains("Longsor", true) && !it.disasterType.contains("Gempa", true) }, Color(0xFF64748B))
            )

            val maxCount = (categories.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)

            // Canvas Bar Chart
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .padding(horizontal = 8.dp)
            ) {
                val barWidth = 24.dp.toPx()
                val spacing = (size.width - (categories.size * barWidth)) / (categories.size + 1)
                val chartHeight = size.height - 20f

                categories.forEachIndexed { i, (_, count, color) ->
                    val x = spacing + i * (barWidth + spacing)
                    val barHeight = (count.toFloat() / maxCount) * chartHeight
                    val y = size.height - barHeight

                    // Draw bar
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Legend labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                categories.forEach { (name, count, color) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = color,
                            modifier = Modifier.size(8.dp)
                        ) {}
                        Text(
                            text = name,
                            fontSize = 11.sp,
                            color = Color(0xFF475569),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        Text(
                            text = "$count",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Mini Map Canvas with Pin and Pulsing Radar Circle
 */
@Composable
private fun MiniMapWithPinCanvas(
    latitude: Double,
    longitude: Double,
    disasterType: String
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pin_radar_anim")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 10f,
        targetValue = 35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_radius"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f

        // Grid lines (GIS Map aesthetic)
        val gridStep = 30f
        var gx = 0f
        while (gx <= size.width) {
            drawLine(
                color = Color(0xFF334155).copy(alpha = 0.5f),
                start = Offset(gx, 0f),
                end = Offset(gx, size.height),
                strokeWidth = 1f
            )
            gx += gridStep
        }
        var gy = 0f
        while (gy <= size.height) {
            drawLine(
                color = Color(0xFF334155).copy(alpha = 0.5f),
                start = Offset(0f, gy),
                end = Offset(size.width, gy),
                strokeWidth = 1f
            )
            gy += gridStep
        }

        // Pulsing radar circles
        drawCircle(
            color = Color(0xFFEF4444).copy(alpha = (1f - (pulseRadius / 35f)).coerceIn(0f, 0.7f)),
            radius = pulseRadius,
            center = Offset(centerX, centerY),
            style = Stroke(width = 2f)
        )

        // Center Pin
        drawCircle(
            color = Color(0xFFDC2626),
            radius = 7f,
            center = Offset(centerX, centerY)
        )
        drawCircle(
            color = Color.White,
            radius = 3f,
            center = Offset(centerX, centerY)
        )
    }
}

/**
 * KPI Metric Card Component
 */
@Composable
private fun KpiMetricCard(
    title: String,
    count: Int,
    badgeColor: Color,
    bgColor: Color,
    borderColor: Color,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        modifier = modifier.wrapContentHeight()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$count",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = badgeColor
            )
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B),
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Kartu Insiden Petugas Responsif dengan Aksi Validasi & Auto-Pin
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OfficerIncidentReportCard(
    report: CitizenDisasterReport,
    isOfficerLoggedIn: Boolean,
    officerName: String,
    officerRole: String = OfficerRoles.PETUGAS_JAGA,
    onInspectMapPin: () -> Unit,
    onUpdateStatus: (String) -> Unit,
    onDeleteReport: (() -> Unit)? = null,
    onOpenNoteEditor: () -> Unit,
    onViewPhoto: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val canValidate = UserRole.canValidateReports(officerRole)
    val canComplete = UserRole.canCompleteReports(officerRole)
    val canDelete = UserRole.canDeleteReports(officerRole) && onDeleteReport != null

    val normalizedStatus = com.example.model.CitizenReportStatusHelper.normalizeStatus(report.status)
    val statusColor = when (normalizedStatus) {
        com.example.model.CitizenReportStatusHelper.STATUS_DIVALIDASI -> Color(0xFF16A34A)
        com.example.model.CitizenReportStatusHelper.STATUS_SELESAI -> Color(0xFF2563EB)
        else -> Color(0xFFD97706) // "Diproses"
    }

    val statusBg = when (normalizedStatus) {
        com.example.model.CitizenReportStatusHelper.STATUS_DIVALIDASI -> Color(0xFFF0FDF4)
        com.example.model.CitizenReportStatusHelper.STATUS_SELESAI -> Color(0xFFEFF6FF)
        else -> Color(0xFFFFFBEB)
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("officer_incident_card_${report.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Title with Badges in a single horizontal line below
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF0F172A),
                        modifier = Modifier
                            .heightIn(min = 24.dp)
                            .wrapContentWidth()
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 6.dp)) {
                            Text(
                                text = "#${report.id.takeLast(7)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                softWrap = false
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = report.disasterType,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Badges in a single horizontal row below the title - Locked height 24.dp
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = statusBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .heightIn(min = 24.dp)
                            .wrapContentWidth()
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
                            Text(
                                text = normalizedStatus.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = statusColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                softWrap = false
                            )
                        }
                    }

                    // Status Sinkronisasi: "Synced" (Hijau) apabila laporan sudah ada di server Firestore
                    val isSyncedServer = isReportSyncedWithServer(report)
                    val syncIcon = if (isSyncedServer) Icons.Default.CheckCircle else Icons.Default.Schedule
                    val syncColor = if (isSyncedServer) Color(0xFF16A34A) else Color(0xFFD97706)
                    val syncText = if (isSyncedServer) "Synced" else "Menunggu Sync"

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = syncColor.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, syncColor.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .heightIn(min = 24.dp)
                            .wrapContentWidth()
                            .testTag("officer_sync_status_badge")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        ) {
                            Icon(
                                imageVector = syncIcon,
                                contentDescription = syncText,
                                tint = syncColor,
                                modifier = Modifier.size(10.dp)
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

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF1F5F9),
                        modifier = Modifier
                            .heightIn(min = 24.dp)
                            .wrapContentWidth()
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 7.dp)) {
                            Text(
                                text = report.district,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF475569),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Location & Date
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color(0xFFDC2626),
                    modifier = Modifier
                        .size(15.dp)
                        .padding(top = 1.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = "${report.village}, Kecamatan ${report.district}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0F172A)
                    )
                    if (report.incidentLocation.isNotBlank()) {
                        Text(
                            text = "Patokan: ${report.incidentLocation}",
                            fontSize = 11.sp,
                            color = Color(0xFF475569)
                        )
                    }
                    Text(
                        text = "Waktu Kejadian: ${report.incidentTime}",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }

            // Casualties banner if any
            if (report.casualtiesDead > 0 || report.casualtiesInjured > 0 || report.casualtiesMissing > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFFEF2F2),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Korban: ${report.casualtiesDead} Meninggal • ${report.casualtiesInjured} Luka • ${report.casualtiesMissing} Hilang",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF991B1B)
                        )
                    }
                }
            }

            // Dedicated Photo Evidence Area (Wajib tampil jelas di Mode Petugas)
            Spacer(modifier = Modifier.height(8.dp))
            if (!report.photoUri.isNullOrBlank()) {
                val thumbContext = LocalContext.current
                val resolvedPhotoModel = remember(report.photoUri) {
                    decodeCitizenPhotoModel(report.photoUri)
                }
                val fallbackOfficerBitmap = rememberCitizenReportPhotoBitmap(report.photoUri, report.id)
                var isThumbError by remember(report.photoUri) {
                    mutableStateOf(resolvedPhotoModel == null && fallbackOfficerBitmap == null)
                }
                if (fallbackOfficerBitmap != null && isThumbError) {
                    isThumbError = false
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isThumbError && fallbackOfficerBitmap == null) Color(0xFFE5E7EB) else Color(0xFF0F172A),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .testTag("officer_report_photo_thumbnail")
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(enabled = !isThumbError || fallbackOfficerBitmap != null) { onViewPhoto(report.photoUri) }
                    ) {
                        if (isThumbError && fallbackOfficerBitmap == null) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFFE5E7EB))
                                    .testTag("officer_report_photo_error")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BrokenImage,
                                    contentDescription = "Foto tidak dapat dimuat",
                                    tint = Color(0xFF6B7280),
                                    modifier = Modifier.size(36.dp)
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
                            val activeBitmap = fallbackOfficerBitmap ?: (resolvedPhotoModel as? android.graphics.Bitmap)
                            if (activeBitmap != null) {
                                androidx.compose.foundation.Image(
                                    bitmap = activeBitmap.asImageBitmap(),
                                    contentDescription = "Foto Bukti Kejadian dari Warga",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                AsyncImage(
                                    model = coil.request.ImageRequest.Builder(thumbContext)
                                        .data(resolvedPhotoModel)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Foto Bukti Kejadian dari Warga",
                                    onSuccess = { isThumbError = false },
                                    onError = {
                                        if (fallbackOfficerBitmap == null) {
                                            isThumbError = true
                                        }
                                    },
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Surface(
                                color = Color.Black.copy(alpha = 0.72f),
                                shape = RoundedCornerShape(topStart = 8.dp),
                                modifier = Modifier.align(Alignment.BottomEnd)
                            ) {
                                Text(
                                    text = "🔍 Ketuk untuk layar penuh",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF86EFAC),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            IconButton(
                                onClick = { onViewPhoto(report.photoUri) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(48.dp)
                                    .testTag("officer_btn_zoom_report_photo")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(Color.Black.copy(alpha = 0.68f), CircleShape)
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
            } else {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF8FAFC),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("officer_placeholder_no_photo")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.ImageNotSupported,
                            contentDescription = "Warga tidak melampirkan foto",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(18.dp)
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

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (!report.photoUri.isNullOrBlank()) "📷 Lampiran foto bukti tersedia" else "Tanpa lampiran foto",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (!report.photoUri.isNullOrBlank()) Color(0xFF15803D) else Color(0xFF64748B)
                )

                // Tap to pin on map
                OutlinedButton(
                    onClick = onInspectMapPin,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.heightIn(min = 36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = null,
                        tint = Color(0xFF0284C7),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Tinjau Peta", fontSize = 11.sp, color = Color(0xFF0284C7))
                }
            }

            // Official notes if any
            if (report.additionalNotes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF0FDF4),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Assignment,
                                contentDescription = null,
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Catatan Tindak Lanjut TRC:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF166534)
                            )
                        }
                        Text(
                            text = report.additionalNotes,
                            fontSize = 11.sp,
                            color = Color(0xFF14532D)
                        )
                    }
                }
            }

            // Active officer handling attribution banner
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
                            imageVector = Icons.Default.Badge,
                            contentDescription = "Petugas Penangan",
                            tint = Color(0xFF1D4ED8),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        val handlerText = buildString {
                            append("Petugas Penangan: ")
                            append(report.handledByOfficerName.ifBlank { "Petugas Pusdalops" })
                            if (report.handledByOfficerRole.isNotBlank()) {
                                append(" (${report.handledByOfficerRole})")
                            }
                            if (report.handledByOfficerNip.isNotBlank()) {
                                append(" • NIP ${maskNipLast6(report.handledByOfficerNip)}")
                            }
                        }
                        Text(
                            text = handlerText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1E40AF),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons (Multi-Tier RBAC BPBD Mukomuko):
            // - Validasi Laporan & Selesai: HANYA Tier 1 (Manajer) & Tier 2 (Pengawas/Supervisor/Koord. Administrasi)
            // - Hapus Laporan: HANYA Tier 1 (Manajer)
            // - Catatan & Rute Maps: Operasional untuk semua Tier (1, 2, 3)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Tombol Validasi Laporan (Tier 1 & Tier 2 ONLY; disembunyikan untuk Tier 3)
                if (canValidate && !report.status.equals("Divalidasi", ignoreCase = true)) {
                    Button(
                        onClick = { onUpdateStatus("Divalidasi") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .heightIn(min = 40.dp)
                            .testTag("btn_validate_report_${report.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Validasi Laporan",
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(text = "Validasi Laporan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Tombol Selesai / Buka Kembali (Tier 1 & Tier 2 ONLY; disembunyikan untuk Tier 3)
                if (canComplete) {
                    if (!report.status.equals("Selesai", ignoreCase = true)) {
                        Button(
                            onClick = { onUpdateStatus("Selesai") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier
                                .heightIn(min = 40.dp)
                                .testTag("btn_complete_report_${report.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Tandai Selesai",
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(text = "Selesai", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        OutlinedButton(
                            onClick = { onUpdateStatus("Diproses") },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.heightIn(min = 40.dp)
                        ) {
                            Text(text = "Buka Kembali", fontSize = 11.sp)
                        }
                    }
                }

                // Tombol Hapus Laporan (KHUSUS Tier 1 Manajer - Full Access)
                if (canDelete) {
                    Button(
                        onClick = { onDeleteReport?.invoke() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .heightIn(min = 40.dp)
                            .testTag("btn_delete_report_${report.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Hapus Laporan",
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(text = "Hapus Laporan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Note editor (Operasional untuk Tier 1, 2, 3)
                OutlinedButton(
                    onClick = onOpenNoteEditor,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.heightIn(min = 40.dp)
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(text = "Catatan", fontSize = 11.sp)
                }

                // Google Maps route (Operasional untuk Tier 1, 2, 3)
                Button(
                    onClick = {
                        GoogleMapsHelper.openNavigationRoute(
                            context = context,
                            destLat = report.latitude,
                            destLon = report.longitude,
                            destName = "Bencana ${report.disasterType} - ${report.village}"
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.heightIn(min = 40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = "Rute Google Maps",
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

/**
 * Diagnostik Sinkronisasi Multi-Perangkat BPBD:
 * Memantau latensi API Firestore, status detak jantung (heartbeat) 5 unit HP pengujian,
 * pemeriksaan integritas checksum (SHA-256), log resolusi konflik, dan akses Database Explorer.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SyncDiagnosticsSection(
    latencyMs: Long,
    isSlowNetwork: Boolean,
    activeDevices: List<com.example.data.sync.DeviceHeartbeat>,
    conflictLogs: List<com.example.data.local.SyncConflictLogEntity>,
    integrityResult: com.example.data.sync.IntegrityCheckResult?,
    isCheckingIntegrity: Boolean,
    securitySessions: List<DeviceSecuritySession> = emptyList(),
    loginActivities: List<OfficerLoginActivity> = emptyList(),
    isCloudLinked: Boolean = false,
    isSyncingSecurity: Boolean = false,
    onSyncSecurityNow: () -> Unit = {},
    onLinkGoogleCloud: () -> Unit = {},
    onVerifySecuritySession: (DeviceSecuritySession, Boolean, String) -> Unit = { _, _, _ -> },
    onRevokeSecuritySession: (DeviceSecuritySession) -> Unit = {},
    onRunIntegrityCheck: () -> Unit,
    onTestPing: () -> Unit,
    onExportConflictLogsCsv: () -> Unit,
    onOpenDatabaseExplorer: () -> Unit,
    onOpenFormLaporanHarian: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 0. PLAY INTEGRITY CLASSIC BACKEND SECURITY SESSIONS & ALL OFFICER LOGIN ACTIVITIES CARD
        PlayIntegritySecuritySessionsCard(
            securitySessions = securitySessions,
            loginActivities = loginActivities,
            isCloudLinked = isCloudLinked,
            isSyncing = isSyncingSecurity,
            onSyncNow = onSyncSecurityNow,
            onLinkGoogleCloud = onLinkGoogleCloud,
            onVerifySecuritySession = onVerifySecuritySession,
            onRevokeSecuritySession = onRevokeSecuritySession
        )

        // 0B. SUPABASE STORAGE IMAGE UPLOAD PATH & BUCKET POLICY DIAGNOSTIC CARD
        SupabaseStorageDiagnosticCard()

        // 1. LATENCY METRIC CARD
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            modifier = Modifier.fillMaxWidth().testTag("card_sync_latency")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = (if (isSlowNetwork) Color(0xFFEF4444) else Color(0xFF10B981)).copy(alpha = 0.2f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.NetworkCheck,
                                    contentDescription = null,
                                    tint = if (isSlowNetwork) Color(0xFFEF4444) else Color(0xFF10B981),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Pemantauan Latensi Firestore", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Waktu bolak-balik respon API cloud", fontSize = 11.sp, color = Color(0xFF64748B))
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSlowNetwork) Color(0xFFDC2626) else Color(0xFF059669),
                        modifier = Modifier
                            .heightIn(min = 24.dp)
                            .wrapContentWidth()
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
                            Text(
                                text = if (isSlowNetwork) "JARINGAN LAMBAT" else "NORMAL",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                softWrap = false
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Response Time", fontSize = 11.sp, color = Color(0xFF64748B))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = if (latencyMs > 0) "$latencyMs" else "-",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isSlowNetwork) Color(0xFFEF4444) else Color(0xFF10B981),
                                modifier = Modifier.weight(1f, fill = false),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "ms",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8),
                                modifier = Modifier.padding(bottom = 4.dp).weight(0.5f, fill = false),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Button(
                        onClick = onTestPing,
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.heightIn(min = 36.dp).testTag("btn_test_latency_ping")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Uji Ping Sekarang",
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

        // 2. ACTIVE CONNECTED DEVICES (HEARTBEAT TIMELINE)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth().testTag("card_active_devices_heartbeat")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.Devices, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Detak Jantung Perangkat (${activeDevices.size} HP Aktif)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "5 HP Pengujian",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF64748B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (activeDevices.isEmpty()) {
                    Text(
                        text = "Belum ada heartbeat perangkat lain terdeteksi dalam 5 menit terakhir.",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        activeDevices.forEach { dev ->
                            val isMe = dev.deviceId.length > 5 && dev.status == "ONLINE"
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF8FAFC),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = dev.deviceModel,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = "Petugas: ${dev.officerName} • ID: ${dev.deviceId.take(8)}",
                                            fontSize = 11.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (dev.status == "SLOW_NET") Color(0xFFFEF3C7) else Color(0xFFDCFCE7)
                                        ) {
                                            Text(
                                                text = if (dev.status == "SLOW_NET") "SLOW NET" else "ONLINE",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (dev.status == "SLOW_NET") Color(0xFFB45309) else Color(0xFF15803D),
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(
                                            text = "${dev.latencyMs} ms",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF334155)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. INTEGRITY CHECK (SHA-256 CHECKSUM COMPARISON)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth().testTag("card_checksum_integrity")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Pemeriksaan Integritas Data (Checksum)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Bandingkan hashing SHA-256 antara Room Database SQLite lokal dan Cloud Firestore untuk mendeteksi rekaman yang terlewat atau tidak konsisten.",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (integrityResult != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (integrityResult.isConsistent) Color(0xFFF0FDF4) else Color(0xFFFFFBEB),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (integrityResult.isConsistent) Color(0xFF86EFAC) else Color(0xFFFDE68A)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (integrityResult.isConsistent) "✅ KONSISTEN (100% SINKRON)" else "⚠️ DITEMUKAN PERBEDAAN",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (integrityResult.isConsistent) Color(0xFF15803D) else Color(0xFFB45309)
                                )
                                Text(
                                    text = "Lokal: ${integrityResult.localCount} | Cloud: ${integrityResult.remoteCount}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF334155)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Checksum Lokal: ${integrityResult.localChecksum}\nChecksum Cloud: ${integrityResult.remoteChecksum}",
                                fontSize = 11.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                color = Color(0xFF475569)
                            )
                            if (integrityResult.discrepancies.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Tindakan Resolusi Otomatis (${integrityResult.resolvedDiscrepancyCount} laporan diselaraskan):",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color(0xFFB45309)
                                )
                                integrityResult.discrepancies.take(3).forEach { d ->
                                    Text("• $d", fontSize = 11.sp, color = Color(0xFF78350F))
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Button(
                    onClick = onRunIntegrityCheck,
                    enabled = !isCheckingIntegrity,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("btn_run_integrity_check")
                ) {
                    if (isCheckingIntegrity) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Memeriksa Checksum...", fontSize = 11.sp)
                    } else {
                        Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Jalankan Pengecekan Integritas (Checksum)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 4. CONFLICT RESOLUTION LOGS & TOOLS
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth().testTag("card_conflict_logs_section")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SyncProblem, contentDescription = null, tint = Color(0xFFEA580C), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Log Resolusi Konflik (${conflictLogs.size})",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Riwayat resolusi otomatis saat dua perangkat petugas mengubah laporan yang sama secara bersamaan.",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onExportConflictLogsCsv,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).testTag("btn_export_conflict_csv")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ekspor CSV Log", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onOpenDatabaseExplorer,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).testTag("btn_open_database_explorer")
                    ) {
                        Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Database Explorer", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Card Buat Laporan Harian (Navigasi ke FormLaporanActivity)
                val localContext = LocalContext.current
                val officerAuth = remember(localContext) { OfficerAuthManager(localContext) }
                val isOfficerLoggedIn = officerAuth.isOfficerLoggedIn()
                val officerRole = officerAuth.getOfficerRole()
                val hasSpecialAssignment = officerRole.contains("Penugasan Khusus", ignoreCase = true)
                if (
                    isOfficerLoggedIn && (
                        officerRole.contains("Manajer", ignoreCase = true) ||
                        officerRole.contains("Administrasi", ignoreCase = true) ||
                        officerRole.contains("Operator", ignoreCase = true) ||
                        hasSpecialAssignment
                    )
                ) {
                    Card(
                        onClick = {
                            val intent = Intent(localContext, com.example.FormLaporanActivity::class.java)
                            localContext.startActivity(intent)
                        },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1976D2)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("cardBuatLaporan")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Assignment,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    "Buat Laporan Harian (Form Resmi)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    "Buka FormLaporanActivity dengan 4 Tab (Kejadian, Dampak, Log, Simpan)",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.85f)
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
fun AdminAuthDialog(
    actionTitle: String,
    officerNip: String,
    officerName: String,
    officerRole: String,
    onDismiss: () -> Unit,
    onAuthorized: (com.example.security.AdminTemporaryAccessToken) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var inputPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isVerifying by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isRetryableError by remember { mutableStateOf(false) }

    val executeVerification: () -> Unit = {
        val trimmed = inputPassword.trim()
        if (trimmed.isEmpty()) {
            errorMessage = "Masukkan PIN Master Admin terlebih dahulu."
            isRetryableError = false
        } else {
            isVerifying = true
            errorMessage = null
            isRetryableError = false
            coroutineScope.launch {
                val result = com.example.security.AdminAccessSecurityManager.verifyMasterPasswordAndIssueToken(
                    context = context,
                    masterPassword = trimmed,
                    officerNip = officerNip,
                    officerName = officerName,
                    officerRole = officerRole,
                    actionTitle = actionTitle
                )
                isVerifying = false
                when (result) {
                    is com.example.security.AdminVerificationResult.Authorized -> {
                        Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                        onAuthorized(result.accessToken)
                    }
                    is com.example.security.AdminVerificationResult.Denied -> {
                        errorMessage = result.message
                        isRetryableError = result.canRetry
                    }
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isVerifying) onDismiss() },
        icon = {
            Surface(
                shape = CircleShape,
                color = Color(0xFFFEF2F2),
                modifier = Modifier.size(50.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = "Verifikasi Admin Server-Side",
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        },
        title = {
            Text(
                text = "Otorisasi Master Admin",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )
        },
        text = {
            val adminPinKeyboardActions = rememberFormKeyboardActions(onDoneCallback = executeVerification)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Akses ke \"$actionTitle\" diverifikasi langsung ke server Cloud Firestore (app_settings/admin_config). " +
                        "Jika PIN valid, Token Akses Sementara (10 menit) diterbitkan dan setiap percobaan dicatat di Audit Log.",
                    fontSize = 11.5.sp,
                    color = Color(0xFF475569),
                    lineHeight = 16.sp
                )
                if (officerNip.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Petugas: ${officerName.ifBlank { "-" }} (NIP: $officerNip)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E293B)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = inputPassword,
                    onValueChange = {
                        inputPassword = it
                        errorMessage = null
                        isRetryableError = false
                    },
                    label = { Text("PIN Master Admin", fontSize = 11.sp) },
                    placeholder = { Text("Masukkan PIN Master Admin", fontSize = 11.sp) },
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = adminPinKeyboardActions,
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (isPasswordVisible) "Sembunyikan PIN" else "Tampilkan PIN",
                                tint = Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    singleLine = true,
                    enabled = !isVerifying,
                    modifier = Modifier
                        .fillMaxWidth()
                        .formScrollPadding()
                        .testTag("input_master_admin_pin")
                )
                if (!errorMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isRetryableError) Color(0xFFFFFBEB) else Color(0xFFFEF2F2),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isRetryableError) Color(0xFFF59E0B) else Color(0xFFFECACA)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("card_admin_auth_error")
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = errorMessage.orEmpty(),
                                color = if (isRetryableError) Color(0xFF92400E) else Color(0xFFDC2626),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 15.sp
                            )
                            if (isRetryableError) {
                                OutlinedButton(
                                    onClick = executeVerification,
                                    enabled = !isVerifying,
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD97706)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 44.dp)
                                        .testTag("btn_retry_admin_pin")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Coba Lagi verifikasi server",
                                        tint = Color(0xFFB45309),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Coba Lagi",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFB45309)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = executeVerification,
                enabled = !isVerifying,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRetryableError) Color(0xFFEA580C) else Color(0xFF15803D)
                ),
                modifier = Modifier.testTag("btn_verify_master_admin_pin")
            ) {
                if (isVerifying) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Memverifikasi...")
                } else {
                    Text(if (isRetryableError) "Coba Lagi" else "Verifikasi Server")
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !isVerifying
            ) {
                Text("Batal")
            }
        }
    )
}

