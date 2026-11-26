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
import androidx.compose.material.icons.filled.Download
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
    val officerAuthManager = remember { OfficerAuthManager(context) }
    val hasVerifiedOfficerSession = isOfficerLoggedIn && officerAuthManager.isOfficerLoggedIn()

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
    val isSandboxModeActive by com.example.training.SandboxTrainingManager.isSandboxEnabled.collectAsStateWithLifecycle()
    val sandboxReportsList by com.example.training.SandboxTrainingManager.sandboxReports.collectAsStateWithLifecycle()

    val liveReports = remember(reports, realtimeCloudReports, isSandboxModeActive, sandboxReportsList) {
        if (isSandboxModeActive) {
            sandboxReportsList.sortedByDescending { it.timestamp }
        } else if (realtimeCloudReports.isEmpty()) {
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
    DisposableEffect(hasVerifiedOfficerSession) {
        if (!hasVerifiedOfficerSession) {
            realtimeCloudReports = emptyList()
            onDispose {
                OfficerAlarmPlayer.stopActiveRingtone()
            }
        } else {
            val pendingReportsListener = com.example.data.local.FirestoreHelper.listenPendingCitizenReportsRealtime(
                onUpdate = { pendingReports ->
                    if (pendingReports.isNotEmpty()) {
                        val existingById = realtimeCloudReports.associateBy { it.id }.toMutableMap()
                        pendingReports.forEach { r -> existingById[r.id] = r }
                        realtimeCloudReports = existingById.values.sortedByDescending { it.timestamp }
                    }
                    handleIncomingPendingReportsStream(pendingReports)
                }
            )
            val allReportsListener = com.example.data.local.FirestoreHelper.listenCitizenReportsRealtime(
                onUpdate = { allRemote ->
                    realtimeCloudReports = allRemote
                    handleIncomingPendingReportsStream(allRemote)
                }
            )
            onDispose {
                pendingReportsListener?.remove()
                allReportsListener?.remove()
                OfficerAlarmPlayer.stopActiveRingtone()
            }
        }
    }

    // Gentle fallback sync (every 60 seconds; real-time Firestore listeners already push updates instantly)
    LaunchedEffect(isOfficerLoggedIn) {
        if (isOfficerLoggedIn) {
            while (isActive) {
                delay(60_000L)
                onSyncReports?.invoke { _ -> }
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
        if (!hasVerifiedOfficerSession || !officerAuthManager.isOfficerLoggedIn()) {
            com.example.security.AdminAccessSecurityManager.revokeToken()
            activeAdminToken = null
            Toast.makeText(
                context,
                "Akses ditolak: Sesi login Petugas aktif diperlukan.",
                Toast.LENGTH_SHORT
            ).show()
            onLogoutClick()
        } else {
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
    }
    val database = remember { AppDatabase.getInstance(context) }
    val personnelRepository = remember { PersonnelRepository(database.personnelDao()) }
    val smartOfficerAuthRepo = remember(context) { com.example.data.repository.SmartOfficerAuthRepository(context) }
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
    var showVolunteerModule by remember { mutableStateOf(false) }
    var showPoskoLogisticsModule by remember { mutableStateOf(false) }
    var showDownloadVolunteerArchiveDialog by remember { mutableStateOf(false) }
    var showSandboxPanel by remember { mutableStateOf(false) }
    var integrityCheckResult by remember { mutableStateOf<com.example.data.sync.IntegrityCheckResult?>(null) }
    var isCheckingIntegrity by remember { mutableStateOf(false) }
    // State untuk manajemen penghapusan dan edit personil di kelola direktori
    var officerPendingDeletion by remember { mutableStateOf<OfficerProfile?>(null) }
    var showResetRosterConfirm by remember { mutableStateOf(false) }
    var restoringPersonnel by remember { mutableStateOf<com.example.data.local.OfficerProfile?>(null) }

    // Enterprise Play Integrity SecurityManager & Backend Firestore Sessions + Login Activities
    val securityManager = remember { SecurityManager.getInstance(context) }
    var securitySessionsList by remember { mutableStateOf<List<DeviceSecuritySession>>(emptyList()) }
    var loginActivitiesList by remember { mutableStateOf<List<OfficerLoginActivity>>(emptyList()) }
    var isSyncingSecurityActivities by remember { mutableStateOf(false) }
    var showSecuritySessionsDialog by remember { mutableStateOf(false) }
    var revokedNipsInSession by remember { mutableStateOf<Set<String>>(emptySet()) }
    val lastSecuritySyncMs = remember { java.util.concurrent.atomic.AtomicLong(0L) }
    var currentFirebaseUid by remember {
        mutableStateOf(
            try { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid } catch (_: Exception) { null }
        )
    }

    val knownOfficialNips = remember {
        OfficialAsnBpbdPersonnel.ALL_OFFICIAL_PERSONNEL.map { it.nip.trim() }.filter { it.isNotBlank() }
    }

    suspend fun runFullSecurityActivitySync() {
        if (!hasVerifiedOfficerSession || !officerAuthManager.isOfficerLoggedIn()) {
            securitySessionsList = emptyList()
            loginActivitiesList = emptyList()
            return
        }
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

    DisposableEffect(currentFirebaseUid, hasVerifiedOfficerSession, officerBadgeId) {
        if (!hasVerifiedOfficerSession) {
            securitySessionsList = emptyList()
            loginActivitiesList = emptyList()
            onDispose { }
        } else {
            val sessionListener = securityManager.listenSecuritySessionsRealtime(
                officerNipFilter = "",
                activeOfficerNipForRevocation = officerBadgeId,
                onActiveSessionRevoked = {
                    if (hasVerifiedOfficerSession && !SecurityManager.isPinResetSuppressed(officerBadgeId)) {
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
                val currentHeartbeats = activeDevices
                val currentRevoked = revokedNipsInSession
                coroutineScope.launch(kotlinx.coroutines.Dispatchers.Default) {
                    val reconciled = securityManager.reconcileWithActiveHeartbeats(
                        sessions = list,
                        heartbeats = currentHeartbeats,
                        persistToCache = false
                    ).filterNot {
                        currentRevoked.contains(it.officerNip.trim().lowercase())
                    }
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        securitySessionsList = reconciled
                    }
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
    }

    LaunchedEffect(activeDevices, hasVerifiedOfficerSession) {
        if (hasVerifiedOfficerSession && activeDevices.isNotEmpty()) {
            val currentSessions = securitySessionsList
            val currentRevoked = revokedNipsInSession
            val reconciled = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                securityManager.reconcileWithActiveHeartbeats(
                    sessions = currentSessions,
                    heartbeats = activeDevices,
                    persistToCache = false
                ).filterNot {
                    currentRevoked.contains(it.officerNip.trim().lowercase())
                }
            }
            securitySessionsList = reconciled
        }
    }

    // Initial sync on Ruang Kerja entry (does NOT re-run heavy Firestore queries on every sub-tab tap)
    LaunchedEffect(currentFirebaseUid, hasVerifiedOfficerSession, officerBadgeId) {
        if (hasVerifiedOfficerSession && officerBadgeId.isNotBlank()) {
            onSyncReports?.invoke { _ -> }
            syncController.sendHeartbeatNow()
        }
    }

    // Only run full security activity sync when Admin explicitly opens Pengaturan Sistem tab (with 30s cooldown)
    LaunchedEffect(dashboardSubTab, currentFirebaseUid, hasVerifiedOfficerSession) {
        if (dashboardSubTab == 3 && hasVerifiedOfficerSession && canAccessSystemSettings) {
            val now = System.currentTimeMillis()
            if (now - lastSecuritySyncMs.get() >= 30_000L) {
                lastSecuritySyncMs.set(now)
                runFullSecurityActivitySync()
            }
        }
    }

    DisposableEffect(hasVerifiedOfficerSession) {
        val officersReg = if (hasVerifiedOfficerSession) {
            coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                personnelRepository.seedDefaultAsnPersonnelIfNeeded(context)
            }
            personnelRepository.startAutoSyncFromCloud(coroutineScope, context)
        } else {
            null
        }
        onDispose {
            officersReg?.remove()
        }
    }

    // Pusdalops Officer Roster Management State backed directly by Room Database & Session Manager
    var showRosterDialog by remember { mutableStateOf(false) }
    var rosterList by remember { mutableStateOf<List<OfficerProfile>>(emptyList()) }
    val activeRosterList = remember(dbPersonnelList) {
        dbPersonnelList.map { p ->
            val cleanNip = p.nip.trim().replace(Regex("[\\s\\-]+"), "")
            OfficerProfile(
                id = "officer_$cleanNip",
                fullName = p.name,
                nip = cleanNip.ifBlank { p.nip.trim() },
                role = p.role,
                unit = p.unit,
                shift = p.shift,
                phone = p.phone
            )
        }
    }

    // Sync Room personnel into local cache on Dispatchers.IO WITHOUT writing back to Firestore or resurrecting deleted NIPs
    LaunchedEffect(activeRosterList) {
        val synced = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            if (activeRosterList.isNotEmpty()) {
                val filteredActive = activeRosterList.filterNot { officerAuthManager.isNipDeleted(it.nip) }
                if (filteredActive.isNotEmpty()) {
                    officerAuthManager.syncRosterLocallyWithoutCloudWrite(filteredActive)
                }
                filteredActive
            } else {
                officerAuthManager.getOfficerRoster()
            }
        }
        rosterList = synced
    }

    // Quick Stats Calculation (Canonical Normalized Status) — memoized to avoid recomputing on every recomposition
    val totalCount = liveReports.size
    val pendingCount = remember(liveReports) {
        liveReports.count { com.example.model.CitizenReportStatusHelper.isPending(it.status) }
    }
    val validatedCount = remember(liveReports) {
        liveReports.count { com.example.model.CitizenReportStatusHelper.isValidated(it.status) }
    }
    val resolvedCount = remember(liveReports) {
        liveReports.count { com.example.model.CitizenReportStatusHelper.isResolved(it.status) }
    }
    val totalDead = remember(liveReports) { liveReports.sumOf { it.casualtiesDead } }
    val totalInjured = remember(liveReports) { liveReports.sumOf { it.casualtiesInjured } }
    val totalMissing = remember(liveReports) { liveReports.sumOf { it.casualtiesMissing } }

    val categoryOptions = remember {
        listOf(
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
    }

    val districtOptions = remember {
        listOf("Semua Kecamatan") + listOf(
            "Kota Mukomuko", "Ipuh", "Teramang Jaya", "Penarik", "Lubuk Pinang",
            "Pondok Suguh", "Air Dikit", "Selagan Raya", "XIV Koto", "Malin Deman",
            "V Koto", "Teras Terunjam", "Sungai Rumbai", "Air Rami", "Air Manjunto"
        )
    }

    // Filtered Reports — memoized by actual filter keys
    val baseReports = remember(liveReports, dashboardSubTab) {
        when (dashboardSubTab) {
            1 -> liveReports.filter { com.example.model.CitizenReportStatusHelper.isResolved(it.status) } // Modul Arsip
            else -> liveReports.filter { !com.example.model.CitizenReportStatusHelper.isResolved(it.status) } // Antrean Aktif
        }
    }

    val filteredReports = remember(
        baseReports,
        selectedStatusFilter,
        selectedCategoryFilter,
        selectedDistrictFilter,
        onlyWithCasualties,
        searchQuery
    ) {
        baseReports.filter { rep ->
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
    }

    if (!hasVerifiedOfficerSession) {
        InternalModuleLockedScreen(
            moduleTitle = "Ruang Kerja Konsol Pusdalops",
            testTag = "officer_dashboard_locked",
            onClose = onLoginClick,
            modifier = modifier
        )
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("officer_dashboard_console_view"),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
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

                    // Baris Badge Aksi: Mengisi kekosongan secara presisi dan terbagi sejajar 2 kolom dalam Ruang Kerja
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        maxItemsInEachRow = 2
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

                        // MENU OPERASIONAL (DALA & FORM HARIAN) + EKSPOR (TIER 1 & TIER 2, ATAU MODE LATIHAN SANDBOX)
                        val isExportAuthorized = isOfficerLoggedIn && (isSandboxModeActive || UserRole.canExportReports(officerRole))
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
                        Spacer(modifier = Modifier.height(12.dp))

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // BARIS 1: Modul Operasional Bencana (2 Kolom Simetris 50% : 50%)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Modul Pasca Bencana / DALA (Tersedia untuk Tier 1, Tier 2, Tier 3)
                                Button(
                                    onClick = { showDalaModule = true },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .testTag("btn_open_dala_module")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Assignment,
                                        contentDescription = "Buka Modul Pasca Bencana DALA",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "Pasca Bencana / DALA",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // Input Form Laporan Harian (Operasional untuk Tier 1, Tier 2, Tier 3)
                                Button(
                                    onClick = { showFormLaporanHarian = true },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .testTag("btn_open_form_harian")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Buka Form Laporan Harian",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "Form Harian",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // BARIS 2: Modul Koordinasi & Unduh Data Relawan (2 Kolom Simetris 50% : 50%)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Modul Pendataan & Koordinasi Relawan (Dashboard, Registrasi Offline-First, Verifikasi & Penugasan)
                                Button(
                                    onClick = { showVolunteerModule = true },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .testTag("btn_open_volunteer_module")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Buka Modul Pendataan & Koordinasi Relawan",
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "Koordinasi Relawan",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // Tombol Unduh Daftar Data Seluruh Relawan (Khusus Sesi Aktif: Arsip, Pelaporan Berjenjang, & Pengakhiran Penanggulangan Bencana)
                                Button(
                                    onClick = { showDownloadVolunteerArchiveDialog = true },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .testTag("btn_command_bar_download_all_volunteers")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = "Unduh Daftar Data Seluruh Relawan",
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "Unduh Data Relawan",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // BARIS 2B: Modul Manajemen Pengungsi & Logistik Posko (Indikator Kritis < 10, FEFO, & Anti-Duplikasi NIK)
                            val poskoRepo = remember { com.example.data.repository.PoskoLogisticsRepository.getInstance(context) }
                            val poskoLogisticStocks by poskoRepo.logisticStocks.collectAsStateWithLifecycle()
                            val criticalLogisticCount = remember(poskoLogisticStocks) {
                                poskoLogisticStocks.count { it.isCritical }
                            }
                            Button(
                                onClick = { showPoskoLogisticsModule = true },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (criticalLogisticCount > 0) Color(0xFFB91C1C) else Color(0xFF1D4ED8)
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                                    .testTag("btn_open_posko_logistics_module")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Archive,
                                    contentDescription = "Buka Modul Manajemen Posko & Logistik",
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (criticalLogisticCount > 0) {
                                        "Manajemen Posko & Logistik ($criticalLogisticCount Stok Kritis <10)"
                                    } else {
                                        "Manajemen Posko & Logistik (FEFO & NIK)"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // BARIS 3: Ekspor Dokumen Resmi PDF & CSV (2 Kolom Simetris 50% : 50% untuk Tier 1 & Tier 2 atau Mode Latihan)
                            if (isExportAuthorized) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // PDF Export Button (Tier 1 & Tier 2, atau Mode Latihan)
                                    Button(
                                        onClick = {
                                            val exportList = liveReports.filter {
                                                it.status.equals("Divalidasi", ignoreCase = true) ||
                                                    it.status.equals("Selesai", ignoreCase = true)
                                            }
                                            val finalExport = if (exportList.isNotEmpty()) exportList else liveReports
                                            ReportExporter.exportToPdf(context, finalExport, officerName, officerBadgeId)
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PictureAsPdf,
                                            contentDescription = "Ekspor PDF",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = if (isSandboxModeActive) "Ekspor PDF (Latihan)" else "Ekspor PDF",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    // CSV Export Button (Tier 1 & Tier 2, atau Mode Latihan)
                                    Button(
                                        onClick = {
                                            ReportExporter.exportToCsv(context, liveReports, officerName, officerBadgeId)
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D)),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.TableChart,
                                            contentDescription = "Ekspor CSV",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = if (isSandboxModeActive) "Ekspor CSV (Latihan)" else "Ekspor CSV (Excel)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            // BARIS 4: Mode Latihan Petugas (Sandbox Simulasi Terisolasi) — Full Width Presisi Sejajar Kiri-Kanan
                            Button(
                                onClick = { showSandboxPanel = !showSandboxPanel },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSandboxModeActive) Color(0xFFD97706) else Color(0xFF334155)
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSandboxModeActive) Color(0xFFFBBF24) else Color(0xFF475569)
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                                    .testTag("btn_toggle_sandbox_panel")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = "Mode Latihan Sandbox",
                                    tint = if (isSandboxModeActive) Color.White else Color(0xFF94A3B8),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isSandboxModeActive) "Mode Latihan (Sandbox): AKTIF" else "Mode Latihan (Sandbox Simulasi)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // PANEL MODE LATIHAN PETUGAS (SANDBOX TERISOLASI) - Hanya ditampilkan saat Mode Latihan aktif atau dibuka manual agar tidak menggeser Core Respon Petugas
        if (isOfficerLoggedIn && (isSandboxModeActive || showSandboxPanel)) {
            item {
                SandboxTrainingControlCard(
                    onOpenDailyReportForm = { showFormLaporanHarian = true },
                    onScenarioInjected = { _ ->
                        dashboardSubTab = 0
                    }
                )
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
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                maxItemsInEachRow = 3
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
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
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
        if (hasVerifiedOfficerSession && dashboardSubTab == 3 && canAccessSystemSettings) {
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
                        color = Color(0xFF1E293B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$totalCount Total Aduan",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF64748B),
                        maxLines = 1,
                        softWrap = false
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
                                        modifier = Modifier.weight(1f, fill = false),
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color.Black.copy(alpha = 0.65f)
                                    ) {
                                        Text(
                                            text = "GPS: %.4f, %.4f (±%.0fm)".format(rep.latitude, rep.longitude, rep.gpsAccuracyMeters ?: 5f),
                                            fontSize = 11.sp,
                                            color = Color(0xFF86EFAC),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))

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
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF0F172A),
                            cursorColor = Color(0xFF15803D),
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
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${filteredReports.size} laporan",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B),
                    maxLines = 1,
                    softWrap = false
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

    // 7. DIALOG GANTI PIN PRIBADI PETUGAS PUSDALOPS (TANPA OPSI RESET KE PIN DEFAULT)
    if (showChangePinDialog) {
        var oldPinInput by remember { mutableStateOf("") }
        var newPinInput by remember { mutableStateOf("") }
        var confirmPinInput by remember { mutableStateOf("") }
        var isOldPinVisible by remember { mutableStateOf(false) }
        var isNewPinVisible by remember { mutableStateOf(false) }
        var isConfirmPinVisible by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        var isProcessingSecurity by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isProcessingSecurity) showChangePinDialog = false },
            icon = {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFFEF3C7),
                    modifier = Modifier.size(50.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            },
            title = {
                Text(
                    text = "Ganti PIN / Sandi Petugas",
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
                        text = "PIN pribadi tersimpan terenkripsi di server Cloud Firestore dan tidak pernah dikembalikan ke PIN default.",
                        fontSize = 11.sp,
                        color = Color(0xFF475569),
                        lineHeight = 14.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

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
                                    contentDescription = if (isOldPinVisible) "Sembunyikan PIN lama" else "Tampilkan PIN lama",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .formScrollPadding()
                            .testTag("input_old_pin")
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
                                    contentDescription = if (isNewPinVisible) "Sembunyikan PIN baru" else "Tampilkan PIN baru",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .formScrollPadding()
                            .testTag("input_new_pin")
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
                                    contentDescription = if (isConfirmPinVisible) "Sembunyikan konfirmasi PIN" else "Tampilkan konfirmasi PIN",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .formScrollPadding()
                            .testTag("input_confirm_new_pin")
                    )

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
                        val trimmedNew = newPinInput.trim()
                        val defaultNipPin = com.example.data.local.OfficerCredentialEntity.generateInitialPinForNip(officerBadgeId)
                        if (oldPinInput.isBlank()) {
                            errorMessage = "Mohon isi PIN / sandi lama Anda saat ini."
                            return@Button
                        }
                        if (trimmedNew.length < 4) {
                            errorMessage = "PIN baru harus minimal 4 karakter / angka."
                            return@Button
                        }
                        if (newPinInput != confirmPinInput) {
                            errorMessage = "Konfirmasi PIN baru tidak sama."
                            return@Button
                        }
                        if (SmartOfficerAuthRepository.FORBIDDEN_DEFAULT_PINS.any { it.equals(trimmedNew, ignoreCase = true) }) {
                            errorMessage = "PIN baru dilarang menggunakan PIN default umum (117 / TRC2026 / dsb)."
                            return@Button
                        }
                        if (trimmedNew.equals(defaultNipPin, ignoreCase = true) ||
                            trimmedNew.equals(officerBadgeId.trim().takeLast(6), ignoreCase = true)
                        ) {
                            errorMessage = "PIN baru dilarang menggunakan 6 digit terakhir NIP (PIN default)."
                            return@Button
                        }
                        isProcessingSecurity = true
                        coroutineScope.launch {
                            try {
                                val (integrityResult, result) = kotlinx.coroutines.withTimeout(25_000L) {
                                    val integrity = PlayIntegritySecurityManager.getInstance(context)
                                        .verifyCriticalAction(officerBadgeId.ifBlank { "OFFICER" }, "CHANGE_PIN")
                                    val smartRepo = SmartOfficerAuthRepository.getInstance(context)
                                    val res = smartRepo.changePersonalPin(officerBadgeId, oldPinInput, trimmedNew)
                                    integrity to res
                                }
                                result.onSuccess { msg ->
                                    officerAuthManager.markCloudCustomPinStatus(true)
                                    hasCustomPin = true
                                    showChangePinDialog = false
                                    Toast.makeText(context, "$msg (${integrityResult.verdict})", Toast.LENGTH_LONG).show()
                                }.onFailure { err ->
                                    errorMessage = err.message ?: "Gagal memperbarui PIN."
                                }
                            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                                errorMessage = "Waktu koneksi ke server Firebase habis. Periksa sinyal internet lalu coba lagi."
                            } catch (e: kotlinx.coroutines.CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                errorMessage = e.localizedMessage ?: "Gagal memperbarui PIN."
                            } finally {
                                isProcessingSecurity = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D)),
                    enabled = !isProcessingSecurity,
                    modifier = Modifier.testTag("btn_save_changed_pin")
                ) {
                    if (isProcessingSecurity) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Memverifikasi...")
                    } else {
                        Text("Simpan PIN Baru")
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
    if (hasVerifiedOfficerSession && isAdminAuthDialogVisible) {
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
    if (hasVerifiedOfficerSession && showRosterDialog && isManajerRole) {
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
        var rosterMode by remember { mutableStateOf(0) } // 0 = Daftar Personil, 1 = Form Input Personil Baru, 2 = Form Edit Personil
        var selectedRoleTab by remember { mutableStateOf("Semua") }
        var rosterSearchQuery by remember { mutableStateOf("") }

        // Form Input Personil Baru State
        var inputFullName by remember { mutableStateOf("") }
        var inputNip by remember { mutableStateOf("") }
        var inputInitialPin by remember { mutableStateOf("") }
        var inputRole by remember { mutableStateOf(OfficerRoles.PETUGAS_JAGA) }
        var inputShift by remember { mutableStateOf("Shift Siaga 24 Jam") }
        var inputUnit by remember { mutableStateOf("Pusdalops BPBD Kab. Mukomuko") }
        var inputPhone by remember { mutableStateOf("") }
        var formError by remember { mutableStateOf<String?>(null) }
        var isSavingPersonnel by remember { mutableStateOf(false) }

        // Form Edit Personil State (inline di dalam modal yang sama agar bebas bug double-dialog & tanpa celah keyboard)
        var editOriginalNip by remember { mutableStateOf("") }
        var editOfficerId by remember { mutableStateOf("") }
        var editRoomId by remember { mutableStateOf<Long?>(null) }
        var editFullName by remember { mutableStateOf("") }
        var editNip by remember { mutableStateOf("") }
        var editCustomPin by remember { mutableStateOf("") }
        var editRole by remember { mutableStateOf(OfficerRoles.PETUGAS_JAGA) }
        var editShift by remember { mutableStateOf("Shift Siaga 24 Jam") }
        var editUnit by remember { mutableStateOf("Pusdalops BPBD Kab. Mukomuko") }
        var editPhone by remember { mutableStateOf("") }
        var editError by remember { mutableStateOf<String?>(null) }
        var isSavingEdit by remember { mutableStateOf(false) }

        val cleanPreviewNip = inputNip.trim().replace(Regex("[\\s\\-]+"), "")
        val autoGeneratedPinPreview = remember(cleanPreviewNip) {
            if (cleanPreviewNip.length < 4) {
                "-"
            } else {
                val rawLastSix = cleanPreviewNip.takeLast(6)
                if (rawLastSix.length in 4..6 && rawLastSix.all { it.isDigit() }) {
                    rawLastSix
                } else {
                    com.example.data.local.OfficerCredentialEntity.generateInitialPinForNip(cleanPreviewNip)
                }
            }
        }

        val submitManualPersonnel: () -> Unit = {
            if (!isSavingPersonnel) {
                val validationError = personnelRepository.validatePersonnelInput(inputFullName, inputNip)
                if (validationError != null) {
                    formError = validationError
                } else {
                    isSavingPersonnel = true
                    coroutineScope.launch {
                        val regRes = smartOfficerAuthRepo.registerOrUpdateManualOfficer(
                            fullName = inputFullName.trim(),
                            nip = inputNip.trim(),
                            role = inputRole,
                            unit = inputUnit.trim(),
                            shift = inputShift.trim(),
                            phone = inputPhone.trim(),
                            customInitialPin = inputInitialPin.trim().ifBlank { null }
                        )
                        isSavingPersonnel = false
                        regRes.fold(
                            onSuccess = { regInfo ->
                                rosterList = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                    officerAuthManager.getOfficerRoster()
                                }
                                rosterMode = 0
                                formError = null
                                val savedName = regInfo.profile.fullName
                                val savedNip = regInfo.profile.nip
                                val effectivePin = regInfo.effectiveInitialPin
                                inputFullName = ""
                                inputNip = ""
                                inputInitialPin = ""
                                inputPhone = ""
                                val syncLabel = if (regInfo.isCloudCredentialSynced) "tersinkron Cloud" else "tersimpan lokal"
                                Toast.makeText(
                                    context,
                                    "Petugas '$savedName' (NIP: $savedNip) berhasil didaftarkan ($syncLabel)! PIN Login: $effectivePin",
                                    Toast.LENGTH_LONG
                                ).show()
                            },
                            onFailure = { err ->
                                formError = err.message ?: "Gagal mendaftarkan personil."
                            }
                        )
                    }
                }
            }
        }

        val submitEditPersonnel: () -> Unit = {
            if (!isSavingEdit) {
                val validationError = personnelRepository.validatePersonnelInput(editFullName, editNip)
                val trimmedCustomPin = editCustomPin.trim()
                val cleanTargetNip = editNip.trim().replace(Regex("[\\s\\-]+"), "")
                val defaultNipPin = com.example.data.local.OfficerCredentialEntity.generateInitialPinForNip(cleanTargetNip)
                val pinError = when {
                    trimmedCustomPin.isBlank() -> null
                    trimmedCustomPin.length < 4 -> "PIN khusus baru minimal 4 digit/karakter."
                    SmartOfficerAuthRepository.FORBIDDEN_DEFAULT_PINS.any { it.equals(trimmedCustomPin, ignoreCase = true) } ->
                        "PIN khusus tidak boleh menggunakan PIN default umum (117 / TRC2026 / dsb)."
                    trimmedCustomPin.equals(defaultNipPin, ignoreCase = true) ||
                        trimmedCustomPin.equals(cleanTargetNip.takeLast(6), ignoreCase = true) ->
                        "PIN petugas dilarang dikembalikan ke 6 digit terakhir NIP (PIN default)."
                    else -> null
                }
                if (validationError != null) {
                    editError = validationError
                } else if (pinError != null) {
                    editError = pinError
                } else {
                    isSavingEdit = true
                    coroutineScope.launch {
                        val regRes = smartOfficerAuthRepo.registerOrUpdateManualOfficer(
                            fullName = editFullName.trim(),
                            nip = editNip.trim(),
                            role = editRole.trim(),
                            unit = editUnit.trim(),
                            shift = editShift.trim(),
                            phone = editPhone.trim(),
                            customInitialPin = editCustomPin.trim().ifBlank { null },
                            existingId = editOfficerId.ifBlank { "officer_${editOriginalNip.trim()}" },
                            previousNip = editOriginalNip.trim().ifBlank { null },
                            existingRoomId = editRoomId
                        )
                        isSavingEdit = false
                        regRes.fold(
                            onSuccess = { regInfo ->
                                rosterList = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                    officerAuthManager.getOfficerRoster()
                                }
                                rosterMode = 0
                                editError = null
                                val syncLabel = if (regInfo.isCloudCredentialSynced) "tersinkron Cloud" else "tersimpan lokal"
                                Toast.makeText(
                                    context,
                                    "Data personil '${regInfo.profile.fullName}' berhasil diperbarui ($syncLabel).",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            onFailure = { err ->
                                editError = err.message ?: "Gagal memperbarui data personil."
                            }
                        )
                    }
                }
            }
        }

        val filteredList = rosterList.filter { off ->
            val matchesRole = when (selectedRoleTab) {
                "Semua" -> true
                "Pengawas" -> off.role.contains("Pengawas", ignoreCase = true)
                "Sekretaris" -> off.role.contains("Sekretaris", ignoreCase = true)
                "Kabid KL" -> off.role.contains("Kabid KL", ignoreCase = true)
                "Kabid RR" -> off.role.contains("Kabid RR", ignoreCase = true)
                "Manajer" -> off.role.contains("Manajer", ignoreCase = true)
                "Supervisor" -> off.role.contains("Supervisor", ignoreCase = true)
                OfficerRoles.OPERATOR -> off.role.contains("Operator", ignoreCase = true)
                OfficerRoles.PETUGAS_JAGA -> off.role.contains("Piket", ignoreCase = true) || off.role.contains("Jaga", ignoreCase = true)
                OfficerRoles.PETUGAS_LAPANGAN -> off.role.contains("Lapangan", ignoreCase = true) || off.role.contains("Logistik", ignoreCase = true) || off.role.contains("TRC", ignoreCase = true)
                else -> off.role.contains(selectedRoleTab, ignoreCase = true)
            }
            val matchesSearch = if (rosterSearchQuery.isBlank()) true else {
                val q = rosterSearchQuery.trim().lowercase()
                off.fullName.lowercase().contains(q) ||
                    off.nip.lowercase().contains(q) ||
                    off.unit.lowercase().contains(q) ||
                    off.role.lowercase().contains(q)
            }
            matchesRole && matchesSearch
        }.sortedWith(
            compareBy<OfficerProfile> { OfficerRoles.getRoleRank(it.role) }
                .thenBy { it.fullName }
        )

        BalancedModalDialog(
            onDismissRequest = {
                if (rosterMode != 0) {
                    rosterMode = 0
                    formError = null
                    editError = null
                } else {
                    showRosterDialog = false
                }
            },
            horizontalMargin = 14.dp,
            extraVerticalMargin = 14.dp
        ) {
            val rosterKeyboardActions = rememberFormKeyboardActions()
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    // Compact Dialog Header (selalu terlihat rapi meski keyboard terbuka)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = when (rosterMode) {
                                1 -> Color(0xFFCCFBF1)
                                2 -> Color(0xFFFFEDD5)
                                else -> Color(0xFFEFF6FF)
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = when (rosterMode) {
                                        1 -> Icons.Default.PersonAdd
                                        2 -> Icons.Default.Edit
                                        else -> Icons.Default.Badge
                                    },
                                    contentDescription = null,
                                    tint = when (rosterMode) {
                                        1 -> Color(0xFF0F766E)
                                        2 -> OrangePrimary
                                        else -> Color(0xFF2563EB)
                                    },
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Text(
                            text = when (rosterMode) {
                                1 -> "Input Personil Pusdalops Baru"
                                2 -> "Edit Data Personil BPBD"
                                else -> "Direktori Personil Pusdalops & TRC"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { showRosterDialog = false },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Tutup Direktori Personil",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Scrollable Body: menyesuaikan tinggi otomatis tanpa celah kosong saat keyboard tampil
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
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
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Token Sementara Aktif • Sisa ${remMin}m ${remSec}d",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF15803D),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = "Kunci Sesi",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFDC2626),
                                            maxLines = 1,
                                            softWrap = false,
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
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Search and Add Button Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = rosterSearchQuery,
                                    onValueChange = { rosterSearchQuery = it },
                                    placeholder = {
                                        Text(
                                            text = "Cari nama/NIP personil...",
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    trailingIcon = if (rosterSearchQuery.isNotEmpty()) {
                                        {
                                            IconButton(
                                                onClick = { rosterSearchQuery = "" },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Hapus Pencarian",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    } else null,
                                    singleLine = true,
                                    keyboardOptions = formDoneKeyboardOptions(),
                                    keyboardActions = rosterKeyboardActions,
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 50.dp)
                                )

                                Button(
                                    onClick = {
                                        rosterMode = 1
                                        formError = null
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                    modifier = Modifier.heightIn(min = 50.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PersonAdd,
                                        contentDescription = "Tambah Petugas",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "+ Input",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Category Filter Chips
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val roleTabs = listOf(
                                    "Semua" to "Semua (${rosterList.size})",
                                    "Pengawas" to "Pengawas (${rosterList.count { it.role.contains("Pengawas", ignoreCase = true) }})",
                                    "Sekretaris" to "Sekretaris (${rosterList.count { it.role.contains("Sekretaris", ignoreCase = true) }})",
                                    "Kabid KL" to "Kabid KL (${rosterList.count { it.role.contains("Kabid KL", ignoreCase = true) }})",
                                    "Kabid RR" to "Kabid RR (${rosterList.count { it.role.contains("Kabid RR", ignoreCase = true) }})",
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
                                            fontSize = 11.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else Color(0xFF475569),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Personil Cards
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (filteredList.isEmpty()) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFFF8FAFC),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            if (rosterList.isEmpty()) {
                                                Text(
                                                    text = "Belum Ada Personil Terdaftar",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.5.sp,
                                                    color = Color(0xFF334155)
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "Daftar personil Room Database masih bersih. Tekan tombol '+ Input' di atas untuk mendaftarkan Operator, Piket Siaga, Staff Logistik, atau Manajer Pusdalops PB.",
                                                    fontSize = 11.5.sp,
                                                    color = Color(0xFF64748B),
                                                    textAlign = TextAlign.Center,
                                                    lineHeight = 16.sp
                                                )
                                            } else {
                                                Text(
                                                    text = "Tidak Ada Personil yang Cocok",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.5.sp,
                                                    color = Color(0xFF334155)
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "Tidak ditemukan personil pada filter/pencarian ini dari total ${rosterList.size} personil terdaftar.",
                                                    fontSize = 11.5.sp,
                                                    color = Color(0xFF64748B),
                                                    textAlign = TextAlign.Center,
                                                    lineHeight = 16.sp
                                                )
                                                Spacer(modifier = Modifier.height(10.dp))
                                                OutlinedButton(
                                                    onClick = {
                                                        selectedRoleTab = "Semua"
                                                        rosterSearchQuery = ""
                                                    },
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                                ) {
                                                    Text(
                                                        text = "Tampilkan Semua (${rosterList.size})",
                                                        fontSize = 11.5.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    filteredList.forEach { off ->
                                        val (roleBg, roleColor) = com.example.data.local.OfficialAsnBpbdPersonnel.getRoleBadgeColor(off.role)
                                        val roleBorder = roleColor.copy(alpha = 0.35f)
                                        val roleIcon = when {
                                            off.role.contains("Pengawas", ignoreCase = true) || off.role.contains("Kalaksa", ignoreCase = true) -> Icons.Default.AdminPanelSettings
                                            off.role.contains("Sekretaris", ignoreCase = true) -> Icons.Default.AdminPanelSettings
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
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
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

                                                    // Edit button for Admin: membuka form Edit inline (mode 2) tanpa double-dialog
                                                    IconButton(
                                                        onClick = {
                                                            val cleanOffNip = off.nip.trim().replace(Regex("[\\s\\-]+"), "")
                                                            val matchInDb = dbPersonnelList.firstOrNull {
                                                                it.nip.trim().replace(Regex("[\\s\\-]+"), "").equals(cleanOffNip, ignoreCase = true)
                                                            }
                                                            editOriginalNip = cleanOffNip
                                                            editOfficerId = off.id
                                                            editRoomId = matchInDb?.id
                                                            editFullName = matchInDb?.name ?: off.fullName
                                                            editNip = matchInDb?.nip ?: off.nip
                                                            editRole = matchInDb?.role ?: off.role
                                                            editUnit = matchInDb?.unit ?: off.unit
                                                            editShift = matchInDb?.shift ?: off.shift
                                                            editPhone = matchInDb?.phone ?: off.phone
                                                            editCustomPin = ""
                                                            editError = null
                                                            rosterMode = 2
                                                        },
                                                        modifier = Modifier
                                                            .size(40.dp)
                                                            .testTag("edit_officer_btn")
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Edit,
                                                            contentDescription = "Edit Personil",
                                                            tint = Color(0xFF0284C7),
                                                            modifier = Modifier.size(19.dp)
                                                        )
                                                    }

                                                    // Delete button for Admin: membuka konfirmasi hapus permanen (mutasi/pensiun/typo/maintenance)
                                                    IconButton(
                                                        onClick = {
                                                            officerPendingDeletion = off
                                                        },
                                                        modifier = Modifier
                                                            .size(40.dp)
                                                            .testTag("delete_officer_btn")
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Delete,
                                                            contentDescription = "Hapus Personil",
                                                            tint = Color(0xFFDC2626),
                                                            modifier = Modifier.size(19.dp)
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(8.dp))

                                                // One-Click Admin Recovery Button: "Pulihkan Akses & Reset Sesi"
                                                val isCurrentlyRevokedOrBlocked = remember(off.nip, revokedNipsInSession, securitySessionsList) {
                                                    val cleanTarget = off.nip.trim().lowercase()
                                                    revokedNipsInSession.contains(cleanTarget) ||
                                                        securitySessionsList.any {
                                                            it.officerNip.trim().equals(cleanTarget, ignoreCase = true) && !it.accessGranted
                                                        }
                                                }
                                                OutlinedButton(
                                                    onClick = { restoringPersonnel = off },
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = ButtonDefaults.outlinedButtonColors(
                                                        containerColor = if (isCurrentlyRevokedOrBlocked) Color(0xFFFEF2F2) else Color(0xFFF0FDF4),
                                                        contentColor = if (isCurrentlyRevokedOrBlocked) Color(0xFFDC2626) else Color(0xFF15803D)
                                                    ),
                                                    border = androidx.compose.foundation.BorderStroke(
                                                        1.dp,
                                                        if (isCurrentlyRevokedOrBlocked) Color(0xFFFCA5A5) else Color(0xFF86EFAC)
                                                    ),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .heightIn(min = 36.dp)
                                                        .testTag("restore_access_officer_btn_${off.nip}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.VerifiedUser,
                                                        contentDescription = "Pulihkan Akses & Reset Sesi",
                                                        tint = if (isCurrentlyRevokedOrBlocked) Color(0xFFDC2626) else Color(0xFF15803D),
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = if (isCurrentlyRevokedOrBlocked) {
                                                            "Akses Dicabut/Diblokir • Pulihkan Akses & Reset Sesi"
                                                        } else {
                                                            "Pulihkan Akses & Reset Sesi"
                                                        },
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Maintenance: Pulihkan Daftar Default ASN BPBD (dengan dialog konfirmasi)
                            OutlinedButton(
                                onClick = { showResetRosterConfirm = true },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 44.dp)
                                    .testTag("btn_reset_default_roster")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Pulihkan Daftar Default ASN BPBD (Maintenance)",
                                    fontSize = 11.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                        } else if (rosterMode == 1) {
                            // MODE 1: FORM INPUT PERSONIL BARU KE ROOM DATABASE & CLOUD
                            Text(
                                text = "Input data personil baru Pusdalops BPBD Kabupaten Mukomuko ke Room Database lokal. Wajib mengisi Nama Lengkap dan NIP ASN atau No. Registrasi TRC.",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
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
                                placeholder = { Text("Contoh: 19850101 201001 1 001 atau 123412", fontSize = 11.sp) },
                                singleLine = true,
                                keyboardOptions = formNextKeyboardOptions(),
                                keyboardActions = rosterKeyboardActions,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .formScrollPadding()
                                    .testTag("personnel_input_nip")
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = inputInitialPin,
                                onValueChange = {
                                    inputInitialPin = it
                                    formError = null
                                },
                                label = { Text("PIN Login Awal (Opsional - Otomatis 6 Digit Terakhir NIP)", fontSize = 11.sp) },
                                placeholder = {
                                    Text(
                                        text = if (autoGeneratedPinPreview != "-") {
                                            "Otomatis: $autoGeneratedPinPreview (atau ketik PIN khusus min. 4 digit)"
                                        } else {
                                            "Kosongkan agar otomatis 6 digit terakhir NIP"
                                        },
                                        fontSize = 11.sp
                                    )
                                },
                                singleLine = true,
                                keyboardOptions = formNextKeyboardOptions(),
                                keyboardActions = rosterKeyboardActions,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .formScrollPadding()
                                    .testTag("personnel_input_initial_pin")
                            )

                            if (cleanPreviewNip.length >= 4) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFF0FDF4),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "🔑 Kredensial Login Petugas: Nama '${inputFullName.trim().ifBlank { "..." }}' atau NIP '$cleanPreviewNip' • PIN Login: ${inputInitialPin.trim().ifBlank { autoGeneratedPinPreview }}",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF166534),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Penugasan Kedinasan di Pusdalops:",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                val roleChoices = listOf(
                                    Triple(OfficerRoles.PENGAWAS, "🏛️ Pengawas BPBD", "Pimpinan pengawasan siaga & koordinasi komando kebijakan bencana"),
                                    Triple(OfficerRoles.SEKRETARIS, "📑 Sekretaris", "Koordinasi kesekretariatan, administrasi umum, & tata kelola BPBD"),
                                    Triple(OfficerRoles.KABID_KL, "🚨 Kabid KL", "Kepala Bidang Kedaruratan dan Logistik penanggulangan bencana"),
                                    Triple(OfficerRoles.KABID_RR, "🏗️ Kabid RR", "Kepala Bidang Rehabilitasi dan Rekonstruksi pasca bencana"),
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
                                        shape = RoundedCornerShape(10.dp),
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
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = roleTitle,
                                                    fontSize = 11.5.sp,
                                                    lineHeight = 16.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) Color(0xFF1D4ED8) else Color(0xFF1E293B)
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = roleDesc,
                                                    fontSize = 11.sp,
                                                    lineHeight = 15.sp,
                                                    color = Color(0xFF64748B)
                                                )
                                            }
                                            if (isSelected) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = Color(0xFF2563EB),
                                                    modifier = Modifier.size(18.dp)
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
                        } else {
                            // MODE 2: FORM EDIT DATA PERSONIL BPBD (KOREKSI TYPO NAMA/NIP, JABATAN, UNIT, SHIFT, PIN KHUSUS)
                            Text(
                                text = "Perbarui nama, NIP/No. Reg TRC, jabatan, unit, shift, kontak, atau tetapkan PIN khusus baru. Kosongkan kolom PIN agar PIN pribadi petugas tetap aman dan tidak berubah.",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = editFullName,
                                onValueChange = {
                                    editFullName = it
                                    editError = null
                                },
                                label = { Text("Nama Lengkap & Gelar *", fontSize = 11.sp) },
                                singleLine = true,
                                keyboardOptions = formNextKeyboardOptions(),
                                keyboardActions = rosterKeyboardActions,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .formScrollPadding()
                                    .testTag("input_edit_personnel_name")
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = editNip,
                                onValueChange = {
                                    editNip = it
                                    editError = null
                                },
                                label = { Text("NIP Pegawai ASN / No. Reg TRC *", fontSize = 11.sp) },
                                singleLine = true,
                                keyboardOptions = formNextKeyboardOptions(),
                                keyboardActions = rosterKeyboardActions,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .formScrollPadding()
                                    .testTag("input_edit_personnel_nip")
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = editCustomPin,
                                onValueChange = {
                                    editCustomPin = it
                                    editError = null
                                },
                                label = { Text("PIN Khusus Baru (Opsional)", fontSize = 11.sp) },
                                placeholder = { Text("Kosongkan agar PIN petugas tetap aman (tidak berubah)", fontSize = 11.sp) },
                                singleLine = true,
                                keyboardOptions = formNextKeyboardOptions(),
                                keyboardActions = rosterKeyboardActions,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .formScrollPadding()
                                    .testTag("input_edit_personnel_pin")
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Jabatan / Penugasan Kedinasan:",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            val editRolePresets = listOf(
                                OfficerRoles.PENGAWAS,
                                OfficerRoles.SEKRETARIS,
                                OfficerRoles.KABID_KL,
                                OfficerRoles.KABID_RR,
                                OfficerRoles.MANAJER,
                                OfficerRoles.SUPERVISOR,
                                OfficerRoles.KOORD_ADMINISTRASI,
                                OfficerRoles.OPERATOR,
                                OfficerRoles.PETUGAS_JAGA,
                                OfficerRoles.PETUGAS_LAPANGAN
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(editRolePresets) { preset ->
                                    val selected = editRole.equals(preset, ignoreCase = true)
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = if (selected) OrangePrimary else Color(0xFFF1F5F9),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (selected) OrangePrimary else Color(0xFFCBD5E1)
                                        ),
                                        modifier = Modifier.clickable {
                                            editRole = preset
                                            editError = null
                                        }
                                    ) {
                                        Text(
                                            text = preset,
                                            fontSize = 11.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (selected) Color.White else Color(0xFF334155),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = editRole,
                                onValueChange = { editRole = it },
                                label = { Text("Jabatan / Peran", fontSize = 11.sp) },
                                singleLine = true,
                                keyboardOptions = formNextKeyboardOptions(),
                                keyboardActions = rosterKeyboardActions,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .formScrollPadding()
                                    .testTag("input_edit_personnel_role")
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = editShift,
                                    onValueChange = { editShift = it },
                                    label = { Text("Regu / Shift", fontSize = 11.sp) },
                                    singleLine = true,
                                    keyboardOptions = formNextKeyboardOptions(),
                                    keyboardActions = rosterKeyboardActions,
                                    modifier = Modifier
                                        .weight(1f)
                                        .formScrollPadding()
                                        .testTag("input_edit_personnel_shift")
                                )
                                OutlinedTextField(
                                    value = editUnit,
                                    onValueChange = { editUnit = it },
                                    label = { Text("Unit / Satuan", fontSize = 11.sp) },
                                    singleLine = true,
                                    keyboardOptions = formNextKeyboardOptions(),
                                    keyboardActions = rosterKeyboardActions,
                                    modifier = Modifier
                                        .weight(1f)
                                        .formScrollPadding()
                                        .testTag("input_edit_personnel_unit")
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = editPhone,
                                onValueChange = { editPhone = it },
                                label = { Text("Nomor HP / WhatsApp / Radio HT", fontSize = 11.sp) },
                                singleLine = true,
                                keyboardOptions = formDoneKeyboardOptions(),
                                keyboardActions = rosterKeyboardActions,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .formScrollPadding()
                                    .testTag("input_edit_personnel_phone")
                            )

                            if (editError != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = editError!!,
                                    color = Color(0xFFDC2626),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Pinned Footer Actions (selalu tampil utuh, tidak ganda, dan seimbang)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        when (rosterMode) {
                            1 -> {
                                OutlinedButton(
                                    onClick = {
                                        rosterMode = 0
                                        formError = null
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 44.dp)
                                ) {
                                    Text(
                                        text = "Kembali ke Daftar",
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Button(
                                    onClick = submitManualPersonnel,
                                    enabled = !isSavingPersonnel,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                                    modifier = Modifier
                                        .weight(1.15f)
                                        .heightIn(min = 44.dp)
                                        .testTag("btn_save_manual_personnel")
                                ) {
                                    Text(
                                        text = if (isSavingPersonnel) "Menyimpan..." else "Simpan Personil",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            2 -> {
                                OutlinedButton(
                                    onClick = {
                                        rosterMode = 0
                                        editError = null
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 44.dp)
                                ) {
                                    Text(
                                        text = "Batal",
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Button(
                                    onClick = submitEditPersonnel,
                                    enabled = !isSavingEdit,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                                    modifier = Modifier
                                        .weight(1.25f)
                                        .heightIn(min = 44.dp)
                                        .testTag("btn_save_edit_personnel")
                                ) {
                                    Text(
                                        text = if (isSavingEdit) "Menyimpan..." else "Simpan Perubahan",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            else -> {
                                Button(
                                    onClick = { showRosterDialog = false },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                    modifier = Modifier.heightIn(min = 44.dp)
                                ) {
                                    Text("Selesai", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // DIALOG KONFIRMASI HAPUS PERSONIL PERMANEN OLEH ADMIN (MUTASI / PENSIUN / TYPO / MAINTENANCE)
    if (officerPendingDeletion != null) {
        val targetOff = officerPendingDeletion!!
        val deleteReasons = remember {
            listOf(
                "ASN Pindah Tugas / Mutasi",
                "Pensiun Tugas BPBD",
                "Koreksi Data / Typo",
                "Maintenance Aplikasi"
            )
        }
        var selectedDeleteReason by remember(targetOff) { mutableStateOf(deleteReasons.first()) }
        var isDeletingNow by remember(targetOff) { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isDeletingNow) officerPendingDeletion = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = Color(0xFFDC2626)
                )
            },
            title = {
                Text(
                    text = "Hapus Personil Permanen?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Data personil berikut akan dihapus secara permanen dari direktori lokal & Cloud (tidak akan muncul kembali setelah sinkronisasi):",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 17.sp
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFFEF2F2),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = targetOff.fullName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF991B1B)
                            )
                            Text(
                                text = "NIP: ${targetOff.nip} • ${targetOff.role}",
                                fontSize = 11.5.sp,
                                color = Color(0xFF7F1D1D)
                            )
                        }
                    }
                    Text(
                        text = "Alasan Penghapusan (Audit Log):",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        deleteReasons.forEach { reason ->
                            val isSelected = selectedDeleteReason == reason
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) Color(0xFFFEE2E2) else Color(0xFFF8FAFC),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) Color(0xFFDC2626) else Color(0xFFE2E8F0)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedDeleteReason = reason }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = reason,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color(0xFF991B1B) else Color(0xFF334155),
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFFDC2626),
                                            modifier = Modifier.size(16.dp)
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
                    onClick = {
                        if (!isDeletingNow) {
                            isDeletingNow = true
                            coroutineScope.launch {
                                val cleanTargetNip = targetOff.nip.trim().replace(Regex("[\\s\\-]+"), "")
                                val matchInDb = dbPersonnelList.firstOrNull {
                                    it.nip.trim().replace(Regex("[\\s\\-]+"), "").equals(cleanTargetNip, ignoreCase = true)
                                }
                                val delRes = smartOfficerAuthRepo.deleteOfficerPermanently(
                                    nip = cleanTargetNip,
                                    officerId = targetOff.id,
                                    roomId = matchInDb?.id,
                                    fullName = targetOff.fullName,
                                    role = targetOff.role,
                                    shift = targetOff.shift,
                                    reason = selectedDeleteReason
                                )
                                isDeletingNow = false
                                officerPendingDeletion = null
                                delRes.fold(
                                    onSuccess = { cloudSynced ->
                                        rosterList = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                            officerAuthManager.getOfficerRoster()
                                        }
                                        val syncSuffix = if (cloudSynced) " & Cloud Firestore" else " (lokal)"
                                        Toast.makeText(
                                            context,
                                            "Personil '${targetOff.fullName}' berhasil dihapus permanen$syncSuffix.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    onFailure = { err ->
                                        Toast.makeText(
                                            context,
                                            err.message ?: "Gagal menghapus data personil.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                )
                            }
                        }
                    },
                    enabled = !isDeletingNow,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    modifier = Modifier.testTag("btn_confirm_delete_officer")
                ) {
                    Text(if (isDeletingNow) "Menghapus..." else "Hapus Permanen")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { officerPendingDeletion = null },
                    enabled = !isDeletingNow
                ) {
                    Text("Batal")
                }
            }
        )
    }

    // DIALOG KONFIRMASI PULIHKAN DAFTAR DEFAULT ASN BPBD (MAINTENANCE)
    if (showResetRosterConfirm) {
        var isResettingRoster by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { if (!isResettingRoster) showResetRosterConfirm = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = Color(0xFF2563EB)
                )
            },
            title = {
                Text(
                    text = "Pulihkan Daftar Default ASN?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = "Tindakan maintenance ini akan memulihkan seluruh daftar resmi 37 ASN BPBD Kabupaten Mukomuko (termasuk ASN yang sebelumnya dihapus) ke direktori lokal.",
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!isResettingRoster) {
                            isResettingRoster = true
                            coroutineScope.launch {
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                    officerAuthManager.resetRosterToDefault()
                                    personnelRepository.clearAll()
                                    personnelRepository.seedDefaultAsnPersonnelIfNeeded(context)
                                }
                                rosterList = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                    officerAuthManager.getOfficerRoster()
                                }
                                isResettingRoster = false
                                showResetRosterConfirm = false
                                Toast.makeText(
                                    context,
                                    "Daftar default ASN BPBD berhasil dipulihkan (${rosterList.size} personil).",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    },
                    enabled = !isResettingRoster,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    modifier = Modifier.testTag("btn_confirm_reset_default_roster")
                ) {
                    Text(if (isResettingRoster) "Memulihkan..." else "Pulihkan Default")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showResetRosterConfirm = false },
                    enabled = !isResettingRoster
                ) {
                    Text("Batal")
                }
            }
        )
    }

    // DIALOG PULIHKAN AKSES SESI SATU KLIK DARI DIREKTORI PERSONIL (TANPA MERESET PIN)
    if (restoringPersonnel != null) {
        val targetOfficer = restoringPersonnel!!
        val cleanTargetNip = targetOfficer.nip.trim().replace(Regex("[\\s\\-]+"), "")
        var isRestoringSession by remember(targetOfficer) { mutableStateOf(false) }
        var restoreError by remember(targetOfficer) { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = {
                if (!isRestoringSession) {
                    restoringPersonnel = null
                }
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.VerifiedUser,
                    contentDescription = "Pulihkan Akses Sesi",
                    tint = Color(0xFF15803D),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Pulihkan Akses Sesi Petugas",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF0FDF4),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = targetOfficer.fullName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "NIP: ${targetOfficer.nip} • ${targetOfficer.role}",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF166534)
                            )
                        }
                    }

                    Text(
                        text = "Pemulihan ini membuka kembali akses perangkat/sesi yang diblokir atau dicabut tanpa mengubah maupun mereset PIN pribadi petugas.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )

                    Button(
                        onClick = {
                            if (isRestoringSession) return@Button
                            isRestoringSession = true
                            restoreError = null
                            coroutineScope.launch {
                                try {
                                    val normalizedTarget = cleanTargetNip.lowercase()
                                    revokedNipsInSession = revokedNipsInSession - normalizedTarget
                                    val res = securityManager.restoreAndVerifyOfficerSessionByAdmin(
                                        officerNip = cleanTargetNip,
                                        officerName = targetOfficer.fullName,
                                        officerRole = targetOfficer.role,
                                        adminName = officerName.ifBlank { "Admin Pusdalops" }
                                    )
                                    res.fold(
                                        onSuccess = { restoredSess ->
                                            securitySessionsList = listOf(restoredSess) + securitySessionsList.filterNot {
                                                it.officerNip.trim().equals(cleanTargetNip, ignoreCase = true)
                                            }
                                            loginActivitiesList = securityManager.fetchLoginActivities()
                                            isRestoringSession = false
                                            restoringPersonnel = null
                                            Toast.makeText(
                                                context,
                                                "Akses & sesi ${targetOfficer.fullName} (NIP: $cleanTargetNip) berhasil dipulihkan (PIN tetap aman). Petugas dapat langsung login kembali.",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        },
                                        onFailure = { err ->
                                            isRestoringSession = false
                                            restoreError = err.message ?: "Gagal memulihkan akses sesi petugas."
                                        }
                                    )
                                } catch (e: kotlinx.coroutines.CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    isRestoringSession = false
                                    restoreError = e.localizedMessage ?: "Gagal memulihkan akses sesi."
                                }
                            }
                        },
                        enabled = !isRestoringSession,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("btn_confirm_restore_session_only")
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "Pulihkan Akses Sesi (PIN Tetap)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Aktifkan kembali sesi yang tercabut tanpa menyentuh PIN pribadi",
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }

                    if (isRestoringSession) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF15803D)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Menyinkronkan pemulihan akses ke Cloud Firestore...",
                                fontSize = 11.sp,
                                color = Color(0xFF15803D),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (restoreError != null) {
                        Text(
                            text = restoreError!!,
                            color = Color(0xFFDC2626),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                OutlinedButton(
                    onClick = { restoringPersonnel = null },
                    enabled = !isRestoringSession
                ) {
                    Text("Batal")
                }
            }
        )
    }

    // FULL SCREEN DIALOG DATABASE EXPLORER (PENGATURAN SISTEM - KHUSUS TIER 1 MANAJER)
    if (hasVerifiedOfficerSession && showDatabaseExplorer && canAccessSystemSettings) {
        Dialog(
            onDismissRequest = { showDatabaseExplorer = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
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
    if (hasVerifiedOfficerSession && showFormLaporanHarian) {
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
    if (hasVerifiedOfficerSession && showDalaModule) {
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

    // FULL SCREEN DIALOG MODUL PENDATAAN & KOORDINASI RELAWAN
    if (hasVerifiedOfficerSession && showVolunteerModule) {
        Dialog(
            onDismissRequest = { showVolunteerModule = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            VolunteerCoordinationScreen(
                isOfficerLoggedIn = isOfficerLoggedIn,
                officerName = officerName,
                officerNip = officerBadgeId,
                officerRole = officerRole,
                onClose = { showVolunteerModule = false }
            )
        }
    }

    // FULL SCREEN DIALOG MODUL MANAJEMEN POSKO, PENGUNGSI, & LOGISTIK DARURAT (MASTER PRD)
    if (hasVerifiedOfficerSession && showPoskoLogisticsModule) {
        Dialog(
            onDismissRequest = { showPoskoLogisticsModule = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            PoskoLogisticsManagementScreen(
                officerName = officerName,
                officerNip = officerBadgeId,
                officerRole = officerRole,
                onClose = { showPoskoLogisticsModule = false }
            )
        }
    }

    // DIALOG UNDUH DAFTAR DATA SELURUH RELAWAN (SESI AKTIF: ARSIP, PELAPORAN BERJENJANG & PENGAKHIRAN PENANGGULANGAN BENCANA)
    if (showDownloadVolunteerArchiveDialog) {
        val archivePurposeOptions = remember {
            listOf(
                "Arsip & Pelaporan Berjenjang kepada Pihak Berkompeten",
                "Dokumentasi Kegiatan Pengakhiran Waktu Penanggulangan Bencana",
                "Arsip Gabungan Pelaporan Berjenjang & Dokumentasi Pasca Tanggap Darurat"
            )
        }
        var selectedArchivePurpose by remember { mutableStateOf(archivePurposeOptions.first()) }

        AlertDialog(
            onDismissRequest = { showDownloadVolunteerArchiveDialog = false },
            modifier = Modifier.testTag("dialog_download_volunteer_archive"),
            icon = {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF0D9488),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            },
            title = {
                Text(
                    text = "Unduh Daftar Data Seluruh Relawan",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A),
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Dokumen ini memuat daftar lengkap seluruh relawan kebencanaan terdaftar (NIK KTP 16 digit, Organisasi, Keahlian, Lokasi Posko, Waktu Check-in/Check-out, & Nomor Surat Tugas) untuk keperluan arsip resmi, pelaporan berjenjang kepada pihak berkompeten, serta dokumentasi pengakhiran waktu penanggulangan bencana.",
                        fontSize = 11.5.sp,
                        color = Color(0xFF334155),
                        lineHeight = 16.sp
                    )

                    Text(
                        text = "Pilih Peruntukan Dokumen:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A)
                    )

                    archivePurposeOptions.forEachIndexed { idx, purpose ->
                        val isSelected = selectedArchivePurpose == purpose
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) Color(0xFFF0FDFA) else Color(0xFFF8FAFC),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) Color(0xFF0D9488) else Color(0xFFCBD5E1)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedArchivePurpose = purpose }
                                .testTag("option_volunteer_archive_purpose_$idx")
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.AutoMirrored.Filled.Assignment,
                                    contentDescription = null,
                                    tint = if (isSelected) Color(0xFF0D9488) else Color(0xFF64748B),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = purpose,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color(0xFF0F766E) else Color(0xFF334155)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val chosenPurpose = selectedArchivePurpose
                                showDownloadVolunteerArchiveDialog = false
                                coroutineScope.launch {
                                    com.example.util.VolunteerDirectoryExporter.exportAllVolunteersFromDbToPdf(
                                        context = context,
                                        officerName = officerName,
                                        officerNip = officerBadgeId,
                                        officerRole = officerRole,
                                        documentPurpose = chosenPurpose
                                    )
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp)
                                .testTag("btn_dialog_export_all_volunteers_pdf")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Unduh PDF",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        Button(
                            onClick = {
                                val chosenPurpose = selectedArchivePurpose
                                showDownloadVolunteerArchiveDialog = false
                                coroutineScope.launch {
                                    com.example.util.VolunteerDirectoryExporter.exportAllVolunteersFromDbToCsv(
                                        context = context,
                                        officerName = officerName,
                                        officerNip = officerBadgeId,
                                        officerRole = officerRole,
                                        documentPurpose = chosenPurpose
                                    )
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp)
                                .testTag("btn_dialog_export_all_volunteers_csv")
                        ) {
                            Icon(
                                imageVector = Icons.Default.TableChart,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Unduh CSV (Excel)",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                OutlinedButton(onClick = { showDownloadVolunteerArchiveDialog = false }) {
                    Text("Tutup", fontSize = 11.5.sp)
                }
            }
        )
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
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF0F172A),
                            cursorColor = Color(0xFF15803D),
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







