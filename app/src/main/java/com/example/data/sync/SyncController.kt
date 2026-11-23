package com.example.data.sync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.data.local.AppDatabase
import com.example.data.local.OfficerAuthManager
import com.example.model.CitizenDisasterReport
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

enum class SyncState(val label: String) {
    SYNCED("Sinkronisasi Selesai"),
    SYNCING("Sedang Sinkronisasi..."),
    SLOW_NETWORK("Jaringan Lambat (>3000ms)"),
    PENDING_RETRY("Menunggu Pemulihan Jaringan"),
    OFFLINE("Mode Offline (Tersimpan Lokal)")
}

data class DeviceHeartbeat(
    val deviceId: String = "",
    val deviceModel: String = "",
    val officerName: String = "",
    val lastSeen: Long = System.currentTimeMillis(),
    val latencyMs: Long = 0L,
    val status: String = "ONLINE"
)

/**
 * Central orchestrator for real-time multi-device database synchronization,
 * latency measurement, retry progress indicators, and instant notification alerts.
 */
class SyncController(
    private val context: Context,
    private val database: AppDatabase
) {
    companion object {
        private const val TAG = "SyncController"
        const val SLOW_NETWORK_THRESHOLD_MS = 3000L
        const val NOTIFICATION_CHANNEL_ID = "pusdalops_live_sync_alerts"
        const val NOTIFICATION_CHANNEL_NAME = "Pemberitahuan Darurat Pusdalops Multi-Perangkat"

        @Volatile
        private var INSTANCE: SyncController? = null

        fun getInstance(context: Context, database: AppDatabase): SyncController {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SyncController(context.applicationContext, database).also { INSTANCE = it }
            }
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val firestore: FirebaseFirestore? by lazy {
        try {
            val dbId = try {
                context.getString(com.example.R.string.firestore_database_id)
            } catch (_: Exception) {
                "ai-studio-android-cuacamuk-a1e6cb6f-11e7-41ba-90f4-82c9adb67961"
            }
            FirebaseFirestore.getInstance(dbId)
        } catch (_: Exception) { null }
    }

    private fun currentUid(): String? = try {
        com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
    } catch (_: Exception) {
        null
    }

    private fun resolveOwnerUid(): String {
        val uid = currentUid()
        if (!uid.isNullOrBlank()) return uid
        val officerNip = try {
            OfficerAuthManager(context).getOfficerBadgeId().trim()
        } catch (_: Exception) {
            ""
        }
        return officerNip.ifBlank { deviceId }
    }

    private val conflictHandler = SyncConflictHandler(context, database)
    private val weatherDao = database.weatherDao()

    // Unique ID for this device
    val deviceId: String by lazy {
        val rawId = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
                ?: UUID.randomUUID().toString().take(12)
        } catch (_: Exception) {
            UUID.randomUUID().toString().take(12)
        }
        rawId.trim().replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(128).ifBlank { "device_local" }
    }

    val deviceModel: String = "${Build.MANUFACTURER} ${Build.MODEL}"

    // State Flows for UI binding
    private val _syncState = MutableStateFlow(SyncState.SYNCED)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _latencyMs = MutableStateFlow<Long>(120L)
    val latencyMs: StateFlow<Long> = _latencyMs.asStateFlow()

    private val _isSlowNetwork = MutableStateFlow(false)
    val isSlowNetwork: StateFlow<Boolean> = _isSlowNetwork.asStateFlow()

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _retryProgress = MutableStateFlow(0f)
    val retryProgress: StateFlow<Float> = _retryProgress.asStateFlow()

    private val _isRetrying = MutableStateFlow(false)
    val isRetrying: StateFlow<Boolean> = _isRetrying.asStateFlow()

    private val _activeDevices = MutableStateFlow<List<DeviceHeartbeat>>(emptyList())
    val activeDevices: StateFlow<List<DeviceHeartbeat>> = _activeDevices.asStateFlow()

    private var reportsListener: ListenerRegistration? = null
    private var heartbeatsListener: ListenerRegistration? = null
    private var heartbeatJob: Job? = null
    private var latencyJob: Job? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    // Track known report IDs thread-safely to only trigger notifications on new/updated external reports
    private val knownReportIds = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    @Volatile
    private var isFirstSnapshot = true

    private var authStateListener: com.google.firebase.auth.FirebaseAuth.AuthStateListener? = null

    init {
        createNotificationChannel()
        OfficerPushNotificationHelper.syncOfficerTopicSubscription(context)
        scope.launch {
            runCatching {
                if (weatherDao.getUnsyncedCitizenReports().isNotEmpty()) {
                    CitizenReportSyncWorker.enqueueWhenConnected(context)
                }
            }
        }
        setupNetworkCallback()
        attachRealtimeReportsListener()
        startHeartbeatAndLatencyMonitoring()
        attachRealtimeHeartbeatsListener()
        try {
            val firebaseAuth = com.google.firebase.auth.FirebaseAuth.getInstance()
            val listener = com.google.firebase.auth.FirebaseAuth.AuthStateListener { auth ->
                OfficerPushNotificationHelper.syncOfficerTopicSubscription(context)
                startHeartbeatAndLatencyMonitoring()
                attachRealtimeReportsListener()
                attachRealtimeHeartbeatsListener()
                sendHeartbeatNow()
            }
            authStateListener = listener
            firebaseAuth.addAuthStateListener(listener)
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseAuth listener skipped: ${e.message}")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .build()

            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi serentak semua perangkat petugas saat ada laporan kejadian bencana baru masuk dari warga Mukomuko"
                enableLights(true)
                lightColor = android.graphics.Color.RED
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
                setSound(soundUri, audioAttributes)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun setupNetworkCallback() {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        val builder = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _isOnline.value = true
                scope.launch {
                    measureLatency()
                    processPendingSyncWithRetry()
                }
            }

            override fun onLost(network: Network) {
                _isOnline.value = false
                _syncState.value = SyncState.OFFLINE
                CitizenReportSyncWorker.enqueueWhenConnected(context)
            }
        }
        networkCallback = callback
        cm.registerNetworkCallback(builder.build(), callback)
    }

    /**
     * Measures network latency towards Cloud Firestore and triggers Slow Network warning if > 3000ms.
     */
    suspend fun measureLatency(): Long = withContext(Dispatchers.IO) {
        val uid = resolveOwnerUid()
        val db = firestore ?: return@withContext -1L
        val startTime = System.currentTimeMillis()
        try {
            val pingDoc = mapOf(
                "deviceId" to deviceId,
                "ownerUid" to uid,
                "pingTimestamp" to startTime
            )
            // Perform write roundtrip test on ping document
            val task = db.collection("network_pings").document(deviceId).set(pingDoc)
            com.google.android.gms.tasks.Tasks.await(task, 5L, java.util.concurrent.TimeUnit.SECONDS)
            val duration = System.currentTimeMillis() - startTime
            _latencyMs.value = duration

            val slow = duration > SLOW_NETWORK_THRESHOLD_MS
            _isSlowNetwork.value = slow
            if (slow && _syncState.value != SyncState.SYNCING) {
                _syncState.value = SyncState.SLOW_NETWORK
            } else if (!slow && _syncState.value == SyncState.SLOW_NETWORK) {
                _syncState.value = SyncState.SYNCED
            }
            duration
        } catch (e: Exception) {
            Log.w(TAG, "Latency check failed: ${e.message}")
            _latencyMs.value = 9999L
            _isSlowNetwork.value = true
            -1L
        }
    }

    @Volatile
    private var lastSessionSyncMs: Long = 0L

    private fun startHeartbeatAndLatencyMonitoring() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (true) {
                if (_isOnline.value) {
                    sendHeartbeat()
                    measureLatency()
                }
                delay(60_000L) // every 60 seconds for battery efficiency
            }
        }
    }

    /**
     * Immediately dispatches a device heartbeat (used on officer login & Sync tab open).
     */
    fun sendHeartbeatNow() {
        scope.launch {
            if (heartbeatsListener == null) {
                attachRealtimeHeartbeatsListener()
            }
            sendHeartbeat()
        }
    }

    private suspend fun sendHeartbeat() = withContext(Dispatchers.IO) {
        val officerAuth = OfficerAuthManager(context)
        val isOfficerActive = officerAuth.isOfficerLoggedIn()
        val officerName = if (isOfficerActive) officerAuth.getOfficerName().ifBlank { "Petugas Pusdalops" } else "Piket Siaga"
        val officerNip = if (isOfficerActive) officerAuth.getOfficerBadgeId().ifBlank { officerAuth.getSavedNip() } else ""
        val officerRole = if (isOfficerActive) officerAuth.getOfficerRole().ifBlank { "Operator Pusdalops" } else ""
        val now = System.currentTimeMillis()
        val currentStatus = if (_isSlowNetwork.value) "SLOW_NET" else "ONLINE"

        if (isOfficerActive && officerNip.isNotBlank() && (now - lastSessionSyncMs >= 5 * 60 * 1000L)) {
            lastSessionSyncMs = now
            try {
                com.example.security.SecurityManager.getInstance(context).syncPendingSessionToFirestore(
                    officerNip = officerNip,
                    officerName = officerName,
                    officerRole = officerRole
                )
            } catch (_: Exception) {
            }
        }

        val localHeartbeat = DeviceHeartbeat(
            deviceId = deviceId,
            deviceModel = deviceModel,
            officerName = officerName,
            lastSeen = now,
            latencyMs = _latencyMs.value.coerceAtLeast(0L),
            status = currentStatus
        )
        val merged = _activeDevices.value
            .filterNot { it.deviceId == deviceId }
            .toMutableList()
        merged.add(0, localHeartbeat)
        _activeDevices.value = merged.sortedByDescending { it.lastSeen }

        val uid = resolveOwnerUid()
        val db = firestore ?: return@withContext
        try {
            val heartbeat = mapOf(
                "deviceId" to deviceId,
                "ownerUid" to uid,
                "deviceModel" to deviceModel,
                "officerName" to officerName,
                "lastSeen" to now,
                "latencyMs" to _latencyMs.value.coerceAtLeast(0L),
                "status" to currentStatus
            )
            val task = db.collection("device_heartbeats")
                .document(deviceId)
                .set(heartbeat, SetOptions.merge())
            com.google.android.gms.tasks.Tasks.await(task, 5L, java.util.concurrent.TimeUnit.SECONDS)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to send heartbeat: ${e.message}")
        }
    }

    private fun attachRealtimeHeartbeatsListener() {
        val db = firestore ?: return
        heartbeatsListener?.remove()
        heartbeatsListener = db.collection("device_heartbeats")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val list = mutableListOf<DeviceHeartbeat>()
                val now = System.currentTimeMillis()
                for (doc in snapshot.documents) {
                    val lastSeen = doc.getLong("lastSeen") ?: 0L
                    // Filter devices active in the last 5 minutes
                    if (now - lastSeen < 5 * 60 * 1000) {
                        list.add(
                            DeviceHeartbeat(
                                deviceId = doc.getString("deviceId") ?: doc.id,
                                deviceModel = doc.getString("deviceModel") ?: "Perangkat Android",
                                officerName = doc.getString("officerName") ?: "Petugas",
                                lastSeen = lastSeen,
                                latencyMs = doc.getLong("latencyMs") ?: 0L,
                                status = doc.getString("status") ?: "ONLINE"
                            )
                        )
                    }
                }
                _activeDevices.value = list.sortedByDescending { it.lastSeen }
            }
    }

    /**
     * Real-time Firestore snapshot listener that triggers local Room reconcile
     * AND fires notification alert on ALL 5 phones simultaneously when a new disaster report arrives.
     */
    private fun attachRealtimeReportsListener() {
        val db = firestore ?: return
        reportsListener?.remove()
        reportsListener = db.collection("citizen_disaster_reports")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(
                        TAG,
                        "[FIRESTORE_SYNC_ERROR] Gagal membaca citizen_disaster_reports (Code=${error.code}): ${error.message}",
                        error
                    )
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                scope.launch {
                    val incomingReports = mutableListOf<CitizenDisasterReport>()
                    for (doc in snapshot.documents) {
                        val id = doc.getString("id") ?: doc.id
                        val ts = doc.getLong("timestamp") ?: System.currentTimeMillis()
                        val updatedAt = doc.getLong("updatedAt") ?: ts
                        val r = CitizenDisasterReport(
                            id = id,
                            disasterType = doc.getString("disasterType") ?: "Kejadian Darurat",
                            incidentTime = doc.getString("incidentTime") ?: "",
                            incidentLocation = doc.getString("incidentLocation") ?: "",
                            district = doc.getString("district") ?: "Mukomuko",
                            village = doc.getString("village") ?: "",
                            latitude = doc.getDouble("latitude") ?: -2.58,
                            longitude = doc.getDouble("longitude") ?: 101.12,
                            gpsAccuracyMeters = doc.getDouble("gpsAccuracyMeters")?.toFloat(),
                            isGpsVerified = doc.getBoolean("isGpsVerified") ?: true,
                            casualtiesDead = doc.getLong("casualtiesDead")?.toInt() ?: 0,
                            casualtiesInjured = doc.getLong("casualtiesInjured")?.toInt() ?: 0,
                            casualtiesMissing = doc.getLong("casualtiesMissing")?.toInt() ?: 0,
                            photoUri = doc.getString("fotoUrl")?.trim()?.takeIf { it.isNotBlank() }
                                ?: doc.getString("fotoLampiran")?.trim()?.takeIf { it.isNotBlank() }
                                ?: doc.getString("photoUri")?.trim()?.takeIf { it.isNotBlank() },
                            reporterName = doc.getString("reporterName") ?: "Warga",
                            reporterPhone = doc.getString("reporterPhone") ?: "",
                            additionalNotes = doc.getString("additionalNotes") ?: "",
                            timestamp = ts,
                            status = com.example.model.CitizenReportStatusHelper.normalizeStatus(
                                doc.getString("status") ?: com.example.model.CitizenReportStatusHelper.STATUS_DIPROSES
                            ),
                            updatedAt = maxOf(updatedAt, ts),
                            handledByOfficerName = doc.getString("handledByOfficerName") ?: "",
                            handledByOfficerNip = doc.getString("handledByOfficerNip") ?: "",
                            handledByOfficerRole = doc.getString("handledByOfficerRole") ?: "",
                            isSyncedToCloud = true
                        )
                        incomingReports.add(r)
                    }

                    // Reconcile each report with Room using SyncConflictHandler
                    for (remoteReport in incomingReports) {
                        val localEntity = weatherDao.getCitizenReportById(remoteReport.id)
                        val resolved = conflictHandler.resolveReportConflict(localEntity, remoteReport)

                        // Insert or update resolved into Room
                        weatherDao.insertCitizenDisasterReport(
                            com.example.data.local.CitizenDisasterReportEntity(
                                id = resolved.id,
                                disasterType = resolved.disasterType,
                                incidentTime = resolved.incidentTime,
                                incidentLocation = resolved.incidentLocation,
                                district = resolved.district,
                                village = resolved.village,
                                latitude = resolved.latitude,
                                longitude = resolved.longitude,
                                gpsAccuracyMeters = resolved.gpsAccuracyMeters,
                                isGpsVerified = resolved.isGpsVerified,
                                casualtiesDead = resolved.casualtiesDead,
                                casualtiesInjured = resolved.casualtiesInjured,
                                casualtiesMissing = resolved.casualtiesMissing,
                                photoUri = resolved.photoUri,
                                reporterName = resolved.reporterName,
                                reporterPhone = resolved.reporterPhone,
                                additionalNotes = resolved.additionalNotes,
                                timestamp = resolved.timestamp,
                                status = resolved.status,
                                updatedAt = resolved.effectiveUpdatedAt(),
                                handledByOfficerName = resolved.handledByOfficerName,
                                handledByOfficerNip = resolved.handledByOfficerNip,
                                handledByOfficerRole = resolved.handledByOfficerRole,
                                isSyncedToCloud = resolved.isSyncedToCloud
                            )
                        )

                        // Trigger push notification to devices logged in as Officer/Pusdalops for brand-new reports
                        if (!isFirstSnapshot && !knownReportIds.contains(remoteReport.id) && localEntity == null) {
                            triggerSimultaneousDisasterAlert(resolved)
                        }
                        knownReportIds.add(remoteReport.id)
                    }
                    if (isFirstSnapshot) {
                        OfficerPushNotificationHelper.registerInitialBaselineReports(context, knownReportIds)
                    }
                    isFirstSnapshot = false
                }
            }
    }

    /**
     * Dispatches high-priority push notification with alert sound and screen wake-up to devices logged in as Officer/Pusdalops.
     * Compatible across Android 7.0 (Nougat, API 24) to Android 16 (API 36).
     */
    private fun triggerSimultaneousDisasterAlert(report: CitizenDisasterReport) {
        try {
            if (!OfficerPushNotificationHelper.isDeviceLoggedInAsOfficer(context)) {
                Log.d(TAG, "Skipping officer push alert on citizen-only device for report ${report.id}")
                return
            }
            val dispatched = OfficerPushNotificationHelper.notifyOfficerForNewReport(context, report)
            if (!dispatched) return

            // Wake device screen momentarily so field officers/Pusdalops immediately notice incoming alert
            try {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                @Suppress("DEPRECATION")
                val wakeLock = powerManager?.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                    "SipagarKita:DisasterAlertWakeLock"
                )
                wakeLock?.acquire(3000L)
            } catch (e: Exception) {
                Log.d(TAG, "WakeLock acquisition non-critical note: ${e.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to dispatch disaster notification: ${e.message}")
        }
    }

    /**
     * Executes retry loop with exponential backoff and updates retryProgress for the UI status bar.
     */
    suspend fun processPendingSyncWithRetry() = withContext(Dispatchers.IO) {
        if (!_isOnline.value) return@withContext
        _isRetrying.value = true
        _syncState.value = SyncState.SYNCING

        val maxRetries = 4
        var attempt = 0
        var success = false

        while (attempt < maxRetries && !success) {
            attempt++
            _retryProgress.value = attempt / maxRetries.toFloat()
            try {
                // Only push unsynced local reports (avoid blind overwrites of already-synced server docs)
                val pendingReports = weatherDao.getUnsyncedCitizenReports()
                var allSucceeded = true
                for (r in pendingReports) {
                    val photoResolution = com.example.util.ImageUtils.resolveCloudPhotoForUpload(
                        rawPhotoUri = r.photoUri,
                        reportId = r.id,
                        context = context,
                        supabaseUploader = { bmp ->
                            com.example.data.api.SupabaseClient.uploadPhotoToSupabase(bmp, r.id).getOrNull()
                        }
                    )
                    val portablePhotoUri = when (photoResolution) {
                        is com.example.util.ImageUtils.CloudPhotoResolution.NoPhoto -> null
                        is com.example.util.ImageUtils.CloudPhotoResolution.ReadyForCloud -> photoResolution.cloudPhotoUrl
                        is com.example.util.ImageUtils.CloudPhotoResolution.DeferredOffline -> {
                            Log.w(TAG, "Pending report ${r.id} held in offline queue (photo upload incomplete): ${photoResolution.reason}")
                            allSucceeded = false
                            continue
                        }
                    }
                    val reportModel = CitizenDisasterReport(
                        id = r.id,
                        disasterType = r.disasterType,
                        incidentTime = r.incidentTime,
                        incidentLocation = r.incidentLocation,
                        district = r.district,
                        village = r.village,
                        latitude = r.latitude,
                        longitude = r.longitude,
                        gpsAccuracyMeters = r.gpsAccuracyMeters,
                        isGpsVerified = r.isGpsVerified,
                        casualtiesDead = r.casualtiesDead,
                        casualtiesInjured = r.casualtiesInjured,
                        casualtiesMissing = r.casualtiesMissing,
                        photoUri = portablePhotoUri,
                        reporterName = r.reporterName,
                        reporterPhone = r.reporterPhone,
                        additionalNotes = r.additionalNotes,
                        timestamp = r.timestamp,
                        status = com.example.model.CitizenReportStatusHelper.normalizeStatus(r.status),
                        updatedAt = r.effectiveUpdatedAt(),
                        handledByOfficerName = r.handledByOfficerName,
                        handledByOfficerNip = r.handledByOfficerNip,
                        handledByOfficerRole = r.handledByOfficerRole,
                        isSyncedToCloud = r.isSyncedToCloud
                    )
                    val res = com.example.data.local.FirestoreHelper.syncReportToCloudSuspend(reportModel)
                    if (res.isSuccess) {
                        if (portablePhotoUri != null && portablePhotoUri != r.photoUri) {
                            weatherDao.insertCitizenDisasterReport(
                                r.copy(photoUri = portablePhotoUri, isSyncedToCloud = true)
                            )
                        } else {
                            weatherDao.updateCitizenReportSyncState(r.id, true)
                        }
                    } else {
                        allSucceeded = false
                    }
                }
                success = allSucceeded
                if (!success) {
                    delay(1000L * (1 shl (attempt - 1)))
                }
            } catch (e: Exception) {
                Log.w(TAG, "Retry attempt $attempt failed: ${e.message}")
                delay(1000L * (1 shl (attempt - 1))) // Exponential backoff: 1s, 2s, 4s...
            }
        }

        _retryProgress.value = 1f
        delay(500)
        _isRetrying.value = false
        _syncState.value = if (success) {
            if (_isSlowNetwork.value) SyncState.SLOW_NETWORK else SyncState.SYNCED
        } else {
            SyncState.PENDING_RETRY
        }
    }

    fun cleanup() {
        authStateListener?.let { listener ->
            try {
                com.google.firebase.auth.FirebaseAuth.getInstance().removeAuthStateListener(listener)
            } catch (_: Exception) {}
            authStateListener = null
        }
        reportsListener?.remove()
        heartbeatsListener?.remove()
        heartbeatJob?.cancel()
        latencyJob?.cancel()
        networkCallback?.let { cb ->
            try {
                val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                cm?.unregisterNetworkCallback(cb)
            } catch (_: Exception) {}
            networkCallback = null
        }
    }
}
