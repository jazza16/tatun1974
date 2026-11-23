package com.example.data.local

import android.content.Context
import android.util.Log
import com.example.R
import com.example.model.CitizenDisasterReport
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Hasil verifikasi/pembacaan dokumen kredensial petugas `officer_credentials/{nip}`
 * langsung dari server Cloud Firestore (`Source.SERVER`).
 */
sealed class CloudCredentialLookupResult {
    data class Found(
        val credential: OfficerCredentialEntity,
        val serverPlainPin: String? = null,
        val ownerUid: String = ""
    ) : CloudCredentialLookupResult()

    data object NotFound : CloudCredentialLookupResult()

    data class Error(
        val userMessage: String,
        val errorCode: String = "UNKNOWN",
        val cause: Throwable? = null
    ) : CloudCredentialLookupResult()
}

/**
 * Cloud Firestore Helper for real-time bidirectional synchronization
 * of Personnel (Officers), Officer Credentials, Backups, and Citizen Disaster Reports.
 * Always targets the project's Named Firestore Database ID (`firestore_database_id`).
 */
object FirestoreHelper {
    private const val TAG = "FirestoreHelper"
    private const val DATABASE_ID = "ai-studio-android-cuacamuk-a1e6cb6f-11e7-41ba-90f4-82c9adb67961"
    private const val COLLECTION_OFFICERS = "officers"
    private const val COLLECTION_CITIZEN_REPORTS = "citizen_disaster_reports"
    private const val COLLECTION_BACKUPS = "database_backups"
    private const val COLLECTION_OFFICER_CREDENTIALS = "officer_credentials"
    private const val COLLECTION_APP_SETTINGS = "app_settings"
    private const val DOC_DOCUMENT_SETTINGS = "document_settings"

    /**
     * Pesan standar Zero-Friction saat laporan warga disimpan ketika perangkat sedang offline.
     */
    const val OFFLINE_REPORT_SAVED_MESSAGE =
        "Laporan tersimpan di perangkat. Akan dikirim otomatis saat sinyal tersedia."

    @Volatile
    private var cachedFirestore: FirebaseFirestore? = null

    @Volatile
    var isOfflinePersistenceConfigured: Boolean = false
        private set

    /**
     * Optional test hooks allowing JVM/Robolectric tests to inject deterministic Cloud Firestore
     * credential verification & persistence behavior without network flakes.
     */
    @Volatile
    var overrideFirestoreForTesting: FirebaseFirestore? = null

    @Volatile
    var overrideCloudCredentialFetcherForTesting: (suspend (String) -> CloudCredentialLookupResult)? = null

    @Volatile
    var overrideCloudCredentialWriterForTesting: (suspend (OfficerCredentialEntity) -> Result<Unit>)? = null

    @Volatile
    var overrideCitizenReportFetcherForTesting: (suspend () -> Result<List<CitizenDisasterReport>>)? = null

    @Volatile
    var overrideCitizenReportUploaderForTesting: (suspend (CitizenDisasterReport) -> Result<Unit>)? = null

    @Volatile
    var overrideCitizenReportStatusUpdaterForTesting: (suspend (String, String, String?, Long, String, String, String) -> Result<Unit>)? = null

    @Volatile
    var overrideOfficersCollectionRoleFetcherForTesting: (suspend (String) -> String?)? = null

    @Volatile
    var overrideOfficersDirectoryFetcherForTesting: (suspend () -> Result<List<OfficerProfile>>)? = null

    fun resetTestHooks() {
        overrideFirestoreForTesting = null
        overrideCloudCredentialFetcherForTesting = null
        overrideCloudCredentialWriterForTesting = null
        overrideCitizenReportFetcherForTesting = null
        overrideCitizenReportUploaderForTesting = null
        overrideCitizenReportStatusUpdaterForTesting = null
        overrideOfficersCollectionRoleFetcherForTesting = null
        overrideOfficersDirectoryFetcherForTesting = null
    }

    private fun currentUid(): String? = try {
        FirebaseAuth.getInstance().currentUser?.uid
    } catch (_: Exception) {
        null
    }

    @Volatile
    private var lastAppCheckWarmupMs: Long = 0L

    private const val APP_CHECK_WARMUP_INTERVAL_MS = 60_000L

    private fun ensureAppCheckTokenAttached() {
        val now = System.currentTimeMillis()
        if (now - lastAppCheckWarmupMs < APP_CHECK_WARMUP_INTERVAL_MS) {
            return
        }
        lastAppCheckWarmupMs = now
        try {
            FirebaseAppCheck.getInstance().getAppCheckToken(false)
                .addOnFailureListener { e ->
                    Log.w(TAG, "[APP_CHECK_WARNING] Gagal memuat token App Check untuk request Firestore: ${e.message}")
                }
        } catch (e: Exception) {
            Log.w(TAG, "[APP_CHECK_WARNING] FirebaseAppCheck belum siap: ${e.message}")
        }
    }

    private fun logFirestoreError(operation: String, error: Throwable) {
        val fsEx = error as? FirebaseFirestoreException
            ?: (error.cause as? FirebaseFirestoreException)
        if (fsEx != null) {
            Log.e(
                TAG,
                "[FIRESTORE_ERROR] Operasi '$operation' gagal — Code=${fsEx.code} (${fsEx.code.name}), " +
                    "Pesan=${fsEx.message}. Periksa Firestore Rules & status App Check!",
                fsEx
            )
        } else {
            Log.e(TAG, "[FIRESTORE_ERROR] Operasi '$operation' gagal: ${error.localizedMessage}", error)
        }
    }

    /**
     * Mengaktifkan Offline Persistence (`persistenceEnabled = true` / [PersistentCacheSettings])
     * pada instance [FirebaseFirestore] agar setiap dokumen yang ditulis saat offline
     * otomatis disimpan di cache lokal HP dan tersinkron saat koneksi internet tersedia.
     */
    fun configureOfflinePersistence(firestoreInstance: FirebaseFirestore): Boolean {
        return try {
            val persistentCache = PersistentCacheSettings.newBuilder()
                .setSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build()
            val settings = FirebaseFirestoreSettings.Builder(firestoreInstance.firestoreSettings)
                .setLocalCacheSettings(persistentCache)
                .build()
            firestoreInstance.firestoreSettings = settings
            isOfflinePersistenceConfigured = true
            Log.i(TAG, "Firestore Offline Persistence (PersistentCacheSettings / persistenceEnabled=true) berhasil diaktifkan.")
            true
        } catch (e: Throwable) {
            // Jika instance Firestore sudah mulai melakukan operasi sebelumnya, persistence bawaan Android tetap aktif
            isOfflinePersistenceConfigured = true
            Log.d(TAG, "Firestore Offline Persistence sudah aktif pada instance ini: ${e.message}")
            true
        }
    }

    /**
     * Dipanggil saat inisialisasi Firebase di [com.example.MainActivity] untuk memastikan
     * Offline Cache Firestore aktif sebelum operasi pembacaan/penulisan pertama dijalankan.
     */
    fun ensureOfflinePersistenceEnabled(context: Context? = null): Boolean {
        return try {
            val db = getFirestoreOrThrow(context).getOrNull()
            if (db != null) {
                configureOfflinePersistence(db)
            } else {
                isOfflinePersistenceConfigured = true
                true
            }
        } catch (e: Throwable) {
            isOfflinePersistenceConfigured = true
            Log.w(TAG, "Inisialisasi awal Offline Persistence Firestore ditunda: ${e.message}")
            false
        }
    }

    private val firestore: FirebaseFirestore?
        get() = getFirestoreOrThrow(null).getOrNull()

    fun getFirestoreOrThrow(context: Context? = null): Result<FirebaseFirestore> {
        overrideFirestoreForTesting?.let {
            configureOfflinePersistence(it)
            return Result.success(it)
        }
        cachedFirestore?.let { return Result.success(it) }
        return try {
            val appContext = context?.applicationContext
            val firebaseApp = if (appContext != null) {
                if (FirebaseApp.getApps(appContext).isEmpty()) {
                    FirebaseApp.initializeApp(appContext)
                        ?: throw IllegalStateException("FirebaseApp gagal diinisialisasi. Periksa google-services.json.")
                } else {
                    FirebaseApp.getInstance()
                }
            } else {
                FirebaseApp.getInstance()
            }
            val resolvedDbId = try {
                appContext?.getString(R.string.firestore_database_id)?.trim()?.ifBlank { DATABASE_ID }
                    ?: DATABASE_ID
            } catch (_: Exception) {
                DATABASE_ID
            }
            val instance = FirebaseFirestore.getInstance(firebaseApp, resolvedDbId)
            requireNotNull(instance) { "FirebaseFirestore.getInstance($resolvedDbId) mengembalikan null" }
            configureOfflinePersistence(instance)
            cachedFirestore = instance
            Result.success(instance)
        } catch (e: Exception) {
            Log.e(TAG, "FirebaseFirestore instance unavailable: ${e.localizedMessage}", e)
            Result.failure(e)
        }
    }

    /**
     * Real-time listener for BPBD Officer / Personnel roster across Pusdalops & TRC devices.
     */
    fun listenOfficersRealtime(
        onUpdate: (List<OfficerProfile>) -> Unit,
        onError: ((Exception) -> Unit)? = null
    ): ListenerRegistration? {
        val db = firestore ?: return null
        ensureAppCheckTokenAttached()
        return try {
            db.collection(COLLECTION_OFFICERS)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Error listening to officers snapshot", error)
                        onError?.invoke(error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null && !snapshot.isEmpty) {
                        val list = mutableListOf<OfficerProfile>()
                        for (doc in snapshot.documents) {
                            val id = doc.getString("id") ?: doc.id
                            val fullName = doc.getString("fullName")?.trim() ?: continue
                            if (fullName.isBlank()) continue
                            val nip = (doc.getString("nip") ?: doc.id).trim()
                            val role = doc.getString("role") ?: OfficerRoles.PETUGAS_JAGA
                            val unit = doc.getString("unit") ?: "Pusdalops BPBD Kab. Mukomuko"
                            val shift = doc.getString("shift") ?: "Shift Siaga 24 Jam"
                            val phone = doc.getString("phone") ?: ""
                            list.add(
                                OfficerProfile(
                                    id = id,
                                    fullName = fullName,
                                    nip = nip,
                                    role = role,
                                    unit = unit,
                                    shift = shift,
                                    phone = phone
                                )
                            )
                        }
                        onUpdate(list)
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach officers listener", e)
            null
        }
    }

    /**
     * Pushes officer profile data to Firestore so all Pusdalops & TRC devices stay in sync.
     * Supports both Google-authenticated sessions and NIP-authenticated officer sessions.
     */
    fun syncOfficerToCloud(officer: OfficerProfile, context: Context? = null) {
        val db = getFirestoreOrThrow(context).getOrNull() ?: firestore ?: return
        ensureAppCheckTokenAttached()
        try {
            val cleanNip = officer.nip.trim().replace(Regex("[\\s\\-]+"), "")
            val effectiveNip = cleanNip.ifBlank { officer.nip.trim() }
            if (effectiveNip.isBlank()) return
            val docId = effectiveNip
            val effectiveOwnerUid = currentUid() ?: "officer_nip_$effectiveNip"
            val data = mapOf(
                "id" to officer.id.ifBlank { "officer_$effectiveNip" },
                "ownerUid" to effectiveOwnerUid,
                "fullName" to officer.fullName.trim(),
                "nip" to effectiveNip,
                "role" to officer.role.ifBlank { OfficerRoles.PETUGAS_JAGA },
                "unit" to officer.unit.ifBlank { "Pusdalops BPBD Kab. Mukomuko" },
                "shift" to officer.shift.ifBlank { "Shift Siaga 24 Jam" },
                "phone" to officer.phone.trim(),
                "updatedAt" to System.currentTimeMillis()
            )
            db.collection(COLLECTION_OFFICERS)
                .document(docId)
                .set(data, SetOptions.merge())
                .addOnSuccessListener {
                    Log.d(TAG, "Officer successfully synced to Firestore: ${officer.fullName} ($effectiveNip)")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Failed to sync officer to Firestore", e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during officer cloud sync", e)
        }
    }

    /**
     * Suspend version of [syncOfficerToCloud] for deterministic registration of new manual officers.
     */
    suspend fun syncOfficerToCloudSuspend(
        officer: OfficerProfile,
        context: Context? = null
    ): Result<Unit> {
        if (overrideCloudCredentialWriterForTesting != null && overrideFirestoreForTesting == null) {
            return Result.success(Unit)
        }
        val dbResult = getFirestoreOrThrow(context)
        val db = dbResult.getOrNull()
            ?: return Result.failure(dbResult.exceptionOrNull() ?: IllegalStateException("Firestore tidak tersedia"))
        ensureAppCheckTokenAttached()
        val cleanNip = officer.nip.trim().replace(Regex("[\\s\\-]+"), "")
        val effectiveNip = cleanNip.ifBlank { officer.nip.trim() }
        if (effectiveNip.isBlank()) {
            return Result.failure(IllegalArgumentException("NIP petugas tidak boleh kosong"))
        }
        val effectiveOwnerUid = currentUid() ?: "officer_nip_$effectiveNip"
        val data = mapOf(
            "id" to officer.id.ifBlank { "officer_$effectiveNip" },
            "ownerUid" to effectiveOwnerUid,
            "fullName" to officer.fullName.trim(),
            "nip" to effectiveNip,
            "role" to officer.role.ifBlank { OfficerRoles.PETUGAS_JAGA },
            "unit" to officer.unit.ifBlank { "Pusdalops BPBD Kab. Mukomuko" },
            "shift" to officer.shift.ifBlank { "Shift Siaga 24 Jam" },
            "phone" to officer.phone.trim(),
            "updatedAt" to System.currentTimeMillis()
        )
        return withTimeoutOrNull(8_000L) {
            suspendCancellableCoroutine { cont ->
                try {
                    db.collection(COLLECTION_OFFICERS)
                        .document(effectiveNip)
                        .set(data, SetOptions.merge())
                        .addOnSuccessListener {
                            if (cont.isActive) cont.resume(Result.success(Unit))
                        }
                        .addOnFailureListener { e ->
                            logFirestoreError("syncOfficerToCloudSuspend($effectiveNip)", e)
                            if (cont.isActive) cont.resume(Result.failure(e))
                        }
                } catch (e: Exception) {
                    logFirestoreError("syncOfficerToCloudSuspend(exception)", e)
                    if (cont.isActive) cont.resume(Result.failure(e))
                }
            }
        } ?: Result.failure(IllegalStateException("Waktu sinkronisasi profil petugas ke server Firebase habis (timeout 8 detik)."))
    }

    /**
     * Removes officer profile from Firestore.
     */
    fun deleteOfficerFromCloud(officerId: String) {
        val db = firestore ?: return
        ensureAppCheckTokenAttached()
        try {
            val cleanId = officerId.trim()
            if (cleanId.isBlank()) return
            db.collection(COLLECTION_OFFICERS)
                .document(cleanId)
                .delete()
                .addOnSuccessListener {
                    Log.d(TAG, "Officer $cleanId deleted from Firestore")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Failed to delete officer from Firestore", e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during officer cloud delete", e)
        }
    }

    /**
     * Parses a Firestore DocumentSnapshot into CitizenDisasterReport.
     */
    private fun parseCitizenReportDocument(doc: com.google.firebase.firestore.DocumentSnapshot): CitizenDisasterReport? {
        if (!doc.exists()) return null
        val id = doc.getString("id") ?: doc.id
        val disasterType = doc.getString("disasterType") ?: "Lainnya"
        val incidentTime = doc.getString("incidentTime") ?: ""
        val incidentLocation = doc.getString("incidentLocation") ?: ""
        val district = doc.getString("district") ?: "Mukomuko"
        val village = doc.getString("village") ?: ""
        val latitude = doc.getDouble("latitude") ?: -2.58
        val longitude = doc.getDouble("longitude") ?: 101.12
        val gpsAccuracy = doc.getDouble("gpsAccuracyMeters")?.toFloat()
        val isGpsVerified = doc.getBoolean("isGpsVerified") ?: true
        val dead = doc.getLong("casualtiesDead")?.toInt() ?: 0
        val injured = doc.getLong("casualtiesInjured")?.toInt() ?: 0
        val missing = doc.getLong("casualtiesMissing")?.toInt() ?: 0
        val rawFotoUrl = doc.getString("fotoUrl")?.trim()?.takeIf { it.isNotBlank() }
        val rawFotoLampiran = doc.getString("fotoLampiran")?.trim()?.takeIf { it.isNotBlank() }
        val rawPhotoUri = doc.getString("photoUri")?.trim()?.takeIf { it.isNotBlank() }
        val allPhotoCandidates = listOfNotNull(rawFotoUrl, rawFotoLampiran, rawPhotoUri)
        val inlineCandidate = allPhotoCandidates.firstOrNull {
            it.startsWith("data:image", ignoreCase = true) || it.contains("base64,", ignoreCase = true)
        }
        val httpsCandidate = allPhotoCandidates.firstOrNull {
            it.startsWith("https://", ignoreCase = true) || it.startsWith("http://", ignoreCase = true)
        }
        if (inlineCandidate != null) {
            com.example.util.ImageUtils.rememberInlineDataUriForReport(id, inlineCandidate)
            if (httpsCandidate != null) {
                com.example.util.ImageUtils.rememberInlineDataUriForUrl(httpsCandidate, inlineCandidate)
            }
        }
        val photoUri = rawFotoUrl ?: rawFotoLampiran ?: rawPhotoUri ?: com.example.util.ImageUtils.getInlineDataUriForReport(id)
        val reporterName = doc.getString("reporterName") ?: "Warga"
        val reporterPhone = doc.getString("reporterPhone") ?: ""
        val notes = doc.getString("additionalNotes") ?: ""
        val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
        val updatedAt = doc.getLong("updatedAt") ?: timestamp
        val rawStatus = doc.getString("status")?.trim().orEmpty()
        val status = if (rawStatus.equals("Pending", ignoreCase = true)) {
            "Pending"
        } else {
            com.example.model.CitizenReportStatusHelper.normalizeStatus(
                rawStatus.ifBlank { com.example.model.CitizenReportStatusHelper.STATUS_DIPROSES }
            )
        }
        val handledByOfficerName = doc.getString("handledByOfficerName") ?: ""
        val handledByOfficerNip = doc.getString("handledByOfficerNip") ?: ""
        val handledByOfficerRole = doc.getString("handledByOfficerRole") ?: ""

        return CitizenDisasterReport(
            id = id,
            disasterType = disasterType,
            incidentTime = incidentTime,
            incidentLocation = incidentLocation,
            district = district,
            village = village,
            latitude = latitude,
            longitude = longitude,
            gpsAccuracyMeters = gpsAccuracy,
            isGpsVerified = isGpsVerified,
            casualtiesDead = dead,
            casualtiesInjured = injured,
            casualtiesMissing = missing,
            photoUri = photoUri,
            reporterName = reporterName,
            reporterPhone = reporterPhone,
            additionalNotes = notes,
            timestamp = timestamp,
            status = status,
            updatedAt = maxOf(updatedAt, timestamp),
            handledByOfficerName = handledByOfficerName,
            handledByOfficerNip = handledByOfficerNip,
            handledByOfficerRole = handledByOfficerRole,
            isSyncedToCloud = true
        )
    }

    private fun sanitizeCitizenReportId(rawId: String): String {
        val cleaned = rawId.trim().replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(128)
        return cleaned.ifBlank { "LAP-MMK-${System.currentTimeMillis()}" }
    }

    internal fun sanitizeCloudPhotoUri(rawUri: String?): String? {
        val trimmed = rawUri?.trim().orEmpty()
        if (trimmed.isBlank()) return null

        // 1. Remote HTTPS/HTTP URLs (upgrade HTTP -> HTTPS)
        if (trimmed.startsWith("https://", ignoreCase = true)) {
            return trimmed.takeIf { it.length <= com.example.util.ImageUtils.FIRESTORE_PHOTO_URI_MAX_CHARS }
        }
        if (trimmed.startsWith("http://", ignoreCase = true)) {
            val upgraded = "https://" + trimmed.substring(7)
            return upgraded.takeIf { it.length <= com.example.util.ImageUtils.FIRESTORE_PHOTO_URI_MAX_CHARS }
        }

        // 2. Bundled Android drawable resource URIs
        if (trimmed.startsWith("android.resource://", ignoreCase = true)) {
            return trimmed.takeIf { it.length <= 1024 }
        }

        // 3. Inline compressed Base64 Data URI (data:image/jpeg;base64,...)
        if (trimmed.startsWith("data:image", ignoreCase = true) ||
            trimmed.contains("base64,", ignoreCase = true)
        ) {
            val header = trimmed.substringBefore("base64,", "data:image/jpeg;")
            val cleanBase64 = trimmed.substringAfter("base64,", "")
                .replace("\n", "")
                .replace("\r", "")
                .replace(" ", "")
                .trim()
            if (cleanBase64.isBlank()) return null
            val normalized = "${header}base64,$cleanBase64"
            if (normalized.length <= com.example.util.ImageUtils.FIRESTORE_PHOTO_URI_MAX_CHARS) {
                return normalized
            }
            // Recompress if slightly over cap so photo evidence is preserved
            val bmp = com.example.util.ImageUtils.decodePhotoSourceToBitmap(normalized) ?: return null
            return com.example.util.ImageUtils.encodeBitmapToCloudDataUri(bmp)
        }

        // 4. Safety net: never push a device-only file:// or /path string to Firestore;
        // encode it directly into a portable compressed JPEG Data URI instead.
        if (com.example.util.ImageUtils.isDeviceLocalPhotoPath(trimmed)) {
            val bmp = com.example.util.ImageUtils.decodePhotoSourceToBitmap(trimmed) ?: return null
            return com.example.util.ImageUtils.encodeBitmapToCloudDataUri(bmp)
        }

        return null
    }

    internal fun resolvePhotoUriFromDocumentFields(fields: Map<String, Any?>): String? {
        val fotoUrl = (fields["fotoUrl"] as? String)?.trim()?.takeIf { it.isNotBlank() }
        if (fotoUrl != null) return fotoUrl
        val fotoLampiran = (fields["fotoLampiran"] as? String)?.trim()?.takeIf { it.isNotBlank() }
        if (fotoLampiran != null) return fotoLampiran
        return (fields["photoUri"] as? String)?.trim()?.takeIf { it.isNotBlank() }
    }

    internal fun buildMutableCitizenReportFields(report: CitizenDisasterReport): Map<String, Any> {
        val effectiveUpdated = report.effectiveUpdatedAt()
        val normalizedStatus = com.example.model.CitizenReportStatusHelper.normalizeStatus(report.status)
        val map = mutableMapOf<String, Any>(
            "disasterType" to report.disasterType.trim().ifBlank { "Lainnya" }.take(120),
            "incidentTime" to report.incidentTime.trim().take(80),
            "incidentLocation" to report.incidentLocation.trim().take(300),
            "district" to report.district.trim().ifBlank { "Kota Mukomuko" }.take(100),
            "village" to report.village.trim().take(120),
            "latitude" to report.latitude.coerceIn(-90.0, 90.0),
            "longitude" to report.longitude.coerceIn(-180.0, 180.0),
            "isGpsVerified" to report.isGpsVerified,
            "casualtiesDead" to report.casualtiesDead.coerceIn(0, 100000),
            "casualtiesInjured" to report.casualtiesInjured.coerceIn(0, 100000),
            "casualtiesMissing" to report.casualtiesMissing.coerceIn(0, 100000),
            "reporterName" to report.reporterName.trim().ifBlank { "Warga Mukomuko" }.take(120),
            "reporterPhone" to report.reporterPhone.trim().take(40),
            "additionalNotes" to report.additionalNotes.trim().take(2000),
            "timestamp" to report.timestamp,
            "updatedAt" to effectiveUpdated,
            "status" to normalizedStatus,
            "handledByOfficerName" to report.handledByOfficerName.trim().take(150),
            "handledByOfficerNip" to report.handledByOfficerNip.trim().take(64),
            "handledByOfficerRole" to report.handledByOfficerRole.trim().take(80)
        )
        report.gpsAccuracyMeters?.let { acc ->
            if (!acc.isNaN() && !acc.isInfinite()) {
                map["gpsAccuracyMeters"] = acc.coerceIn(0f, 100000f)
            }
        }
        sanitizeCloudPhotoUri(report.photoUri)?.let { safePhoto ->
            map["photoUri"] = safePhoto
            map["fotoUrl"] = safePhoto
            if (safePhoto.startsWith("https://", ignoreCase = true) ||
                safePhoto.startsWith("http://", ignoreCase = true)
            ) {
                map["fotoLampiran"] = safePhoto
            }
        }
        return map
    }

    private val localAllReportsListeners =
        java.util.concurrent.CopyOnWriteArrayList<(List<CitizenDisasterReport>) -> Unit>()
    private val localPendingReportsListeners =
        java.util.concurrent.CopyOnWriteArrayList<(List<CitizenDisasterReport>) -> Unit>()
    private val inMemoryLatestReportsById =
        java.util.concurrent.ConcurrentHashMap<String, CitizenDisasterReport>()

    private fun recordAndBroadcastInMemoryReport(report: CitizenDisasterReport) {
        val safeId = sanitizeCitizenReportId(report.id)
        val syncedCopy = report.copy(id = safeId, isSyncedToCloud = true)
        inMemoryLatestReportsById[safeId] = syncedCopy
        realtimeDocumentStreamListeners[safeId]?.forEach { cb ->
            runCatching { cb(syncedCopy) }
        }
        val allSorted = inMemoryLatestReportsById.values.sortedByDescending { it.timestamp }
        if (allSorted.isNotEmpty()) {
            localAllReportsListeners.forEach { cb ->
                runCatching { cb(allSorted) }
            }
            val pendingSorted = allSorted.filter {
                com.example.model.CitizenReportStatusHelper.isPending(it.status) ||
                    it.status.equals("Pending", ignoreCase = true)
            }
            localPendingReportsListeners.forEach { cb ->
                runCatching { cb(pendingSorted) }
            }
        }
    }

    /**
     * Real-time listener for all Citizen Disaster Reports across citizen & officer devices.
     */
    fun listenCitizenReportsRealtime(
        onUpdate: (List<CitizenDisasterReport>) -> Unit,
        onError: ((Exception) -> Unit)? = null
    ): ListenerRegistration? {
        localAllReportsListeners.add(onUpdate)
        val db = firestore
        if (db == null) {
            return ListenerRegistration { localAllReportsListeners.remove(onUpdate) }
        }
        ensureAppCheckTokenAttached()
        val firestoreReg = try {
            db.collection(COLLECTION_CITIZEN_REPORTS)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        logFirestoreError("listenCitizenReportsRealtime", error)
                        onError?.invoke(error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val reports = snapshot.documents.mapNotNull { doc ->
                            parseCitizenReportDocument(doc)
                        }.sortedByDescending { it.timestamp }
                        reports.forEach { r -> inMemoryLatestReportsById[sanitizeCitizenReportId(r.id)] = r }
                        onUpdate(reports)
                    }
                }
        } catch (e: Exception) {
            logFirestoreError("attachCitizenReportsRealtimeListener", e)
            null
        }
        return ListenerRegistration {
            localAllReportsListeners.remove(onUpdate)
            runCatching { firestoreReg?.remove() }
        }
    }

    /**
     * Real-time stream listener for Pending Citizen Disaster Reports (setara StreamBuilder Flutter):
     * Mendengarkan koleksi `citizen_disaster_reports` secara real-time, memfilter status Pending/Diproses,
     * dan mengurutkan berdasarkan `timestamp` descending tanpa bergantung pada composite index yang rawan gagal.
     */
    fun listenPendingCitizenReportsRealtime(
        onUpdate: (List<CitizenDisasterReport>) -> Unit,
        onError: ((Exception) -> Unit)? = null
    ): ListenerRegistration? {
        localPendingReportsListeners.add(onUpdate)
        val db = firestore
        if (db == null) {
            return ListenerRegistration { localPendingReportsListeners.remove(onUpdate) }
        }
        ensureAppCheckTokenAttached()
        val firestoreReg = try {
            db.collection(COLLECTION_CITIZEN_REPORTS)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        logFirestoreError("listenPendingCitizenReportsRealtime", error)
                        onError?.invoke(error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val allReports = snapshot.documents.mapNotNull { doc ->
                            parseCitizenReportDocument(doc)
                        }
                        allReports.forEach { r -> inMemoryLatestReportsById[sanitizeCitizenReportId(r.id)] = r }
                        val pendingReports = allReports.filter {
                            com.example.model.CitizenReportStatusHelper.isPending(it.status) ||
                                it.status.equals("Pending", ignoreCase = true)
                        }.sortedByDescending { it.timestamp }
                        onUpdate(pendingReports)
                    }
                }
        } catch (e: Exception) {
            logFirestoreError("attachPendingCitizenReportsRealtimeListener", e)
            null
        }
        return ListenerRegistration {
            localPendingReportsListeners.remove(onUpdate)
            runCatching { firestoreReg?.remove() }
        }
    }

    // In-memory stream listener registry for single-document real-time tracking (StreamBuilder Pelacakan by ID)
    private val realtimeDocumentStreamListeners =
        java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.CopyOnWriteArrayList<(CitizenDisasterReport?) -> Unit>>()

    @androidx.annotation.VisibleForTesting
    fun emitTestRealtimeReportDocumentUpdate(report: CitizenDisasterReport) {
        val safeId = sanitizeCitizenReportId(report.id)
        realtimeDocumentStreamListeners[safeId]?.forEach { callback ->
            callback(report)
        }
    }

    @androidx.annotation.VisibleForTesting
    fun getActiveDocumentStreamListenerCountForTesting(reportId: String): Int {
        val safeId = sanitizeCitizenReportId(reportId)
        return realtimeDocumentStreamListeners[safeId]?.size ?: 0
    }

    /**
     * StreamBuilder Pelacakan (Real-Time Specific Document Stream Listener):
     * Mendengarkan dokumen spesifik `citizen_disaster_reports/{reportId}` (mis. `LAP-MMK-...`)
     * secara real-time agar halaman "Riwayat Laporan" warga langsung memperbarui UI Stepper Vertikal,
     * warna tahapan, dan jam perubahan status oleh petugas begitu data di Firestore berubah.
     */
    fun listenCitizenReportByIdRealtime(
        reportId: String,
        onUpdate: (CitizenDisasterReport?) -> Unit,
        onError: ((Exception) -> Unit)? = null
    ): ListenerRegistration? {
        val trimmedId = reportId.trim()
        if (trimmedId.isBlank()) return null
        val safeId = sanitizeCitizenReportId(trimmedId)

        val callbackList = realtimeDocumentStreamListeners.getOrPut(safeId) {
            java.util.concurrent.CopyOnWriteArrayList()
        }
        callbackList.add(onUpdate)

        val firestoreReg: ListenerRegistration? = try {
            val db = firestore
            if (db != null) {
                ensureAppCheckTokenAttached()
                db.collection(COLLECTION_CITIZEN_REPORTS)
                    .document(safeId)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            logFirestoreError("listenCitizenReportByIdRealtime($safeId)", error)
                            onError?.invoke(error)
                            return@addSnapshotListener
                        }
                        if (snapshot != null && snapshot.exists()) {
                            val parsed = parseCitizenReportDocument(snapshot)
                            onUpdate(parsed)
                        }
                    }
            } else {
                null
            }
        } catch (e: Exception) {
            logFirestoreError("attachCitizenReportByIdRealtimeListener($safeId)", e)
            null
        }

        return ListenerRegistration {
            callbackList.remove(onUpdate)
            try {
                firestoreReg?.remove()
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Actively fetches the latest Citizen Disaster Reports directly from Firestore SERVER
     * (.get(Source.SERVER)) so Officer devices always display up-to-date citizen reports.
     */
    suspend fun fetchCitizenReportsFromServerSuspend(): Result<List<CitizenDisasterReport>> {
        overrideCitizenReportFetcherForTesting?.let { return it() }
        return suspendCancellableCoroutine { cont ->
            val dbResult = getFirestoreOrThrow()
            val db = dbResult.getOrNull()
            if (db == null) {
                val err = dbResult.exceptionOrNull() ?: IllegalStateException("Firestore tidak tersedia")
                logFirestoreError("fetchCitizenReportsFromServerSuspend(init)", err)
                if (cont.isActive) cont.resume(Result.failure(err))
                return@suspendCancellableCoroutine
            }
            ensureAppCheckTokenAttached()
            try {
                db.collection(COLLECTION_CITIZEN_REPORTS)
                    .get(Source.SERVER)
                    .addOnSuccessListener { snapshot ->
                        if (!cont.isActive) return@addOnSuccessListener
                        val reports = snapshot?.documents?.mapNotNull { doc ->
                            parseCitizenReportDocument(doc)
                        }?.sortedByDescending { it.timestamp }.orEmpty()
                        Log.i(TAG, "Berhasil menarik ${reports.size} laporan warga dari Firestore SERVER.")
                        cont.resume(Result.success(reports))
                    }
                    .addOnFailureListener { e ->
                        logFirestoreError("fetchCitizenReportsFromServerSuspend(Source.SERVER)", e)
                        if (cont.isActive) {
                            cont.resume(Result.failure(e))
                        }
                    }
            } catch (e: Exception) {
                logFirestoreError("fetchCitizenReportsFromServerSuspend(exception)", e)
                if (cont.isActive) {
                    cont.resume(Result.failure(e))
                }
            }
        }
    }

    /**
     * Mengambil satu dokumen laporan warga berdasarkan ID Laporan langsung dari Cloud Firestore SERVER
     * untuk fitur Pelacakan Status Laporan (Tracking ID).
     */
    suspend fun fetchCitizenReportByIdFromServerSuspend(reportId: String): Result<CitizenDisasterReport?> {
        val trimmed = reportId.trim()
        if (trimmed.isBlank()) return Result.success(null)
        overrideCitizenReportFetcherForTesting?.let { fetcher ->
            return fetcher().map { list ->
                list.firstOrNull {
                    it.id.equals(trimmed, ignoreCase = true) ||
                        it.id.endsWith(trimmed, ignoreCase = true)
                }
            }
        }
        return suspendCancellableCoroutine { cont ->
            val dbResult = getFirestoreOrThrow()
            val db = dbResult.getOrNull()
            if (db == null) {
                val err = dbResult.exceptionOrNull() ?: IllegalStateException("Firestore tidak tersedia")
                if (cont.isActive) cont.resume(Result.failure(err))
                return@suspendCancellableCoroutine
            }
            ensureAppCheckTokenAttached()
            val safeId = sanitizeCitizenReportId(trimmed)
            try {
                db.collection(COLLECTION_CITIZEN_REPORTS)
                    .document(safeId)
                    .get(Source.SERVER)
                    .addOnSuccessListener { doc ->
                        if (!cont.isActive) return@addOnSuccessListener
                        val parsed = if (doc != null && doc.exists()) parseCitizenReportDocument(doc) else null
                        cont.resume(Result.success(parsed))
                    }
                    .addOnFailureListener { e ->
                        if (cont.isActive) cont.resume(Result.failure(e))
                    }
            } catch (e: Exception) {
                if (cont.isActive) cont.resume(Result.failure(e))
            }
        }
    }

    /**
     * AuthorUid-safe suspend upload/sync of a CitizenDisasterReport to Cloud Firestore.
     * Attempts `.set(fullCreateData)` first so brand-new citizen reports evaluate `allow create`
     * in a single round-trip (avoiding `allow update` PERMISSION_DENIED when `resource == null`).
     * If the document already exists with a different `authorUid`, falls back to `.update(mutableFields)`
     * which preserves `id` and `authorUid`.
     */
    suspend fun syncReportToCloudSuspend(report: CitizenDisasterReport): Result<Unit> {
        recordAndBroadcastInMemoryReport(report)
        overrideCitizenReportUploaderForTesting?.let { return it(report) }
        val dbResult = getFirestoreOrThrow()
        val db = dbResult.getOrNull()
            ?: return Result.failure(dbResult.exceptionOrNull() ?: IllegalStateException("Firestore tidak tersedia"))
        ensureAppCheckTokenAttached()
        val safeId = sanitizeCitizenReportId(report.id)
        val mutableFields = buildMutableCitizenReportFields(report)
        val uid = currentUid() ?: "PUBLIC_CITIZEN_MUKOMUKO"
        val fullCreateData = mutableMapOf<String, Any>(
            "id" to safeId,
            "authorUid" to uid
        ).apply { putAll(mutableFields) }

        // 1. Primary path: .set(fullCreateData) — triggers `allow create` for new reports (or `allow update` if authorUid matches)
        val createOrSetResult = suspendCancellableCoroutine<Result<Unit>> { cont ->
            try {
                db.collection(COLLECTION_CITIZEN_REPORTS)
                    .document(safeId)
                    .set(fullCreateData)
                    .addOnSuccessListener {
                        Log.d(TAG, "Report $safeId written (create/set) in Firestore")
                        if (cont.isActive) cont.resume(Result.success(Unit))
                    }
                    .addOnFailureListener { e ->
                        if (cont.isActive) cont.resume(Result.failure(e))
                    }
            } catch (e: Exception) {
                if (cont.isActive) cont.resume(Result.failure(e))
            }
        }
        if (createOrSetResult.isSuccess) {
            return Result.success(Unit)
        }

        // 2. Fallback path: document already exists in Firestore with a different authorUid -> in-place .update(mutableFields)
        return suspendCancellableCoroutine { cont ->
            try {
                db.collection(COLLECTION_CITIZEN_REPORTS)
                    .document(safeId)
                    .update(mutableFields)
                    .addOnSuccessListener {
                        Log.d(TAG, "Report $safeId updated in Firestore (authorUid preserved)")
                        if (cont.isActive) cont.resume(Result.success(Unit))
                    }
                    .addOnFailureListener { e ->
                        logFirestoreError("syncReportToCloudSuspend(update $safeId)", e)
                        if (cont.isActive) cont.resume(Result.failure(e))
                    }
            } catch (e: Exception) {
                logFirestoreError("syncReportToCloudSuspend(update $safeId)", e)
                if (cont.isActive) cont.resume(Result.failure(e))
            }
        }
    }

    /**
     * Backward-compatible non-suspend wrapper for uploading or updating a citizen disaster report.
     */
    fun syncReportToCloud(report: CitizenDisasterReport) {
        recordAndBroadcastInMemoryReport(report)
        val db = firestore ?: return
        ensureAppCheckTokenAttached()
        val safeId = sanitizeCitizenReportId(report.id)
        val mutableFields = buildMutableCitizenReportFields(report)
        val uid = currentUid() ?: "PUBLIC_CITIZEN_MUKOMUKO"
        val createData = mutableMapOf<String, Any>(
            "id" to safeId,
            "authorUid" to uid
        ).apply { putAll(mutableFields) }
        try {
            db.collection(COLLECTION_CITIZEN_REPORTS)
                .document(safeId)
                .set(createData)
                .addOnSuccessListener {
                    Log.d(TAG, "Report $safeId created/synced to Firestore")
                }
                .addOnFailureListener { _ ->
                    db.collection(COLLECTION_CITIZEN_REPORTS)
                        .document(safeId)
                        .update(mutableFields)
                        .addOnSuccessListener {
                            Log.d(TAG, "Report $safeId updated in Firestore")
                        }
                        .addOnFailureListener { updateErr ->
                            logFirestoreError("syncReportToCloud($safeId)", updateErr)
                        }
                }
        } catch (e: Exception) {
            logFirestoreError("syncReportToCloud($safeId)", e)
        }
    }

    /**
     * Updates report status, optional follow-up notes, timestamp (`updatedAt`), and active officer attribution
     * in Firestore. If the report document has not reached Firestore yet (`NOT_FOUND` or `PERMISSION_DENIED` on missing doc)
     * and `fallbackReport` is provided, creates the complete report document immediately so no officer follow-up is lost.
     */
    suspend fun updateReportStatusInCloudSuspend(
        reportId: String,
        newStatus: String,
        notes: String? = null,
        updatedAt: Long = System.currentTimeMillis(),
        officerName: String = "",
        officerNip: String = "",
        officerRole: String = "",
        fallbackReport: CitizenDisasterReport? = null
    ): Result<Unit> {
        val normalizedStatus = com.example.model.CitizenReportStatusHelper.normalizeStatus(newStatus)
        val safeId = sanitizeCitizenReportId(reportId)
        val existingMem = inMemoryLatestReportsById[safeId] ?: fallbackReport
        if (existingMem != null) {
            recordAndBroadcastInMemoryReport(
                existingMem.copy(
                    id = safeId,
                    status = normalizedStatus,
                    additionalNotes = notes ?: existingMem.additionalNotes,
                    updatedAt = updatedAt,
                    handledByOfficerName = officerName.ifBlank { existingMem.handledByOfficerName },
                    handledByOfficerNip = officerNip.ifBlank { existingMem.handledByOfficerNip },
                    handledByOfficerRole = officerRole.ifBlank { existingMem.handledByOfficerRole }
                )
            )
        }
        overrideCitizenReportStatusUpdaterForTesting?.let {
            return it(reportId, normalizedStatus, notes, updatedAt, officerName, officerNip, officerRole)
        }
        val dbResult = getFirestoreOrThrow()
        val db = dbResult.getOrNull()
            ?: return Result.failure(dbResult.exceptionOrNull() ?: IllegalStateException("Firestore tidak tersedia"))
        ensureAppCheckTokenAttached()

        val updateMap = mutableMapOf<String, Any>(
            "status" to normalizedStatus,
            "updatedAt" to updatedAt
        )
        if (notes != null) {
            updateMap["additionalNotes"] = notes.trim().take(2000)
        }
        if (officerName.isNotBlank()) {
            updateMap["handledByOfficerName"] = officerName.trim().take(150)
        }
        if (officerNip.isNotBlank()) {
            updateMap["handledByOfficerNip"] = officerNip.trim().take(64)
        }
        if (officerRole.isNotBlank()) {
            updateMap["handledByOfficerRole"] = officerRole.trim().take(80)
        }

        val updateResult = suspendCancellableCoroutine<Result<Unit>> { cont ->
            try {
                db.collection(COLLECTION_CITIZEN_REPORTS)
                    .document(safeId)
                    .update(updateMap)
                    .addOnSuccessListener {
                        Log.d(TAG, "Report $safeId status updated to $normalizedStatus in Firestore")
                        if (cont.isActive) cont.resume(Result.success(Unit))
                    }
                    .addOnFailureListener { e ->
                        if (cont.isActive) cont.resume(Result.failure(e))
                    }
            } catch (e: Exception) {
                if (cont.isActive) cont.resume(Result.failure(e))
            }
        }
        if (updateResult.isSuccess) return Result.success(Unit)

        val err = updateResult.exceptionOrNull()
        val fsEx = err as? FirebaseFirestoreException ?: (err?.cause as? FirebaseFirestoreException)
        if ((fsEx?.code == FirebaseFirestoreException.Code.NOT_FOUND ||
                fsEx?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) && fallbackReport != null
        ) {
            val patched = fallbackReport.copy(
                id = safeId,
                status = normalizedStatus,
                additionalNotes = notes ?: fallbackReport.additionalNotes,
                updatedAt = updatedAt,
                handledByOfficerName = officerName.ifBlank { fallbackReport.handledByOfficerName },
                handledByOfficerNip = officerNip.ifBlank { fallbackReport.handledByOfficerNip },
                handledByOfficerRole = officerRole.ifBlank { fallbackReport.handledByOfficerRole }
            )
            return syncReportToCloudSuspend(patched)
        }

        if (err != null) logFirestoreError("updateReportStatusInCloudSuspend($safeId)", err)
        return updateResult
    }

    /**
     * Backward-compatible wrapper for updating report status and optional notes in Firestore.
     */
    fun updateReportStatusInCloud(
        reportId: String,
        newStatus: String,
        notes: String? = null,
        officerName: String = "",
        officerNip: String = "",
        officerRole: String = ""
    ) {
        val db = firestore ?: return
        ensureAppCheckTokenAttached()
        val safeId = sanitizeCitizenReportId(reportId)
        val normalizedStatus = com.example.model.CitizenReportStatusHelper.normalizeStatus(newStatus)
        try {
            val updateMap = mutableMapOf<String, Any>(
                "status" to normalizedStatus,
                "updatedAt" to System.currentTimeMillis()
            )
            if (notes != null) {
                updateMap["additionalNotes"] = notes.trim().take(2000)
            }
            if (officerName.isNotBlank()) updateMap["handledByOfficerName"] = officerName.trim().take(150)
            if (officerNip.isNotBlank()) updateMap["handledByOfficerNip"] = officerNip.trim().take(64)
            if (officerRole.isNotBlank()) updateMap["handledByOfficerRole"] = officerRole.trim().take(80)

            db.collection(COLLECTION_CITIZEN_REPORTS)
                .document(safeId)
                .update(updateMap)
                .addOnSuccessListener {
                    Log.d(TAG, "Report status updated to $normalizedStatus in Firestore")
                }
                .addOnFailureListener { e ->
                    logFirestoreError("updateReportStatusInCloud($safeId)", e)
                }
        } catch (e: Exception) {
            logFirestoreError("updateReportStatusInCloud($safeId)", e)
        }
    }

    /**
     * Menghapus dokumen laporan warga di Cloud Firestore (khusus Tier 1 Manajer / Full Access).
     */
    suspend fun deleteReportFromCloudSuspend(reportId: String): Result<Unit> {
        val dbResult = getFirestoreOrThrow()
        val db = dbResult.getOrNull()
            ?: return Result.failure(dbResult.exceptionOrNull() ?: IllegalStateException("Firestore tidak tersedia"))
        ensureAppCheckTokenAttached()
        val safeId = sanitizeCitizenReportId(reportId)
        return suspendCancellableCoroutine { cont ->
            try {
                db.collection(COLLECTION_CITIZEN_REPORTS)
                    .document(safeId)
                    .delete()
                    .addOnSuccessListener {
                        Log.d(TAG, "Report $safeId deleted from Firestore by Tier 1 Manajer")
                        if (cont.isActive) cont.resume(Result.success(Unit))
                    }
                    .addOnFailureListener { e ->
                        logFirestoreError("deleteReportFromCloudSuspend($safeId)", e)
                        if (cont.isActive) cont.resume(Result.failure(e))
                    }
            } catch (e: Exception) {
                logFirestoreError("deleteReportFromCloudSuspend($safeId)", e)
                if (cont.isActive) cont.resume(Result.failure(e))
            }
        }
    }

    /**
     * Backs up the disaster report database to user-isolated Cloud Firestore backup storage.
     */
    fun backupReportsToCloud(
        reports: List<CitizenDisasterReport>,
        officerName: String,
        onComplete: (Boolean, String) -> Unit
    ) {
        val uid = currentUid()
        if (uid == null) {
            onComplete(false, "Autentikasi Google diperlukan untuk mencadangkan data ke Cloud Firestore.")
            return
        }
        val db = firestore
        if (db == null) {
            onComplete(false, "Firestore tidak tersedia. Pastikan koneksi internet aktif.")
            return
        }

        try {
            val backupId = "backup_reports_${System.currentTimeMillis()}"
            val serializedReports = reports.map { r ->
                mapOf(
                    "id" to r.id,
                    "disasterType" to r.disasterType,
                    "incidentTime" to r.incidentTime,
                    "incidentLocation" to r.incidentLocation,
                    "district" to r.district,
                    "village" to r.village,
                    "latitude" to r.latitude,
                    "longitude" to r.longitude,
                    "gpsAccuracyMeters" to r.gpsAccuracyMeters,
                    "isGpsVerified" to r.isGpsVerified,
                    "casualtiesDead" to r.casualtiesDead,
                    "casualtiesInjured" to r.casualtiesInjured,
                    "casualtiesMissing" to r.casualtiesMissing,
                    "photoUri" to r.photoUri,
                    "reporterName" to r.reporterName,
                    "reporterPhone" to r.reporterPhone,
                    "additionalNotes" to r.additionalNotes,
                    "timestamp" to r.timestamp,
                    "status" to r.status
                ).filterValues { it != null }
            }

            val backupPayload = mapOf(
                "backupId" to backupId,
                "ownerUid" to uid,
                "backupDate" to System.currentTimeMillis(),
                "officerName" to officerName,
                "totalReports" to reports.size,
                "reports" to serializedReports
            )

            db.collection(COLLECTION_BACKUPS)
                .document(backupId)
                .set(backupPayload)
                .addOnSuccessListener {
                    db.collection(COLLECTION_BACKUPS)
                        .document("latest_backup_$uid")
                        .set(backupPayload)
                    onComplete(true, "Berhasil mencadangkan ${reports.size} laporan ke cloud ($backupId).")
                }
                .addOnFailureListener { e ->
                    onComplete(false, "Gagal mencadangkan ke cloud: ${e.localizedMessage}")
                }
        } catch (e: Exception) {
            onComplete(false, "Terjadi kesalahan: ${e.localizedMessage}")
        }
    }

    /**
     * Restores the latest disaster report database backup for the signed-in account.
     */
    fun restoreReportsFromCloud(
        onComplete: (Boolean, List<CitizenDisasterReport>, String) -> Unit
    ) {
        val uid = currentUid()
        if (uid == null) {
            onComplete(false, emptyList(), "Autentikasi Google diperlukan untuk memulihkan cadangan Cloud Firestore.")
            return
        }
        val db = firestore
        if (db == null) {
            onComplete(false, emptyList(), "Firestore tidak tersedia.")
            return
        }

        try {
            db.collection(COLLECTION_BACKUPS)
                .document("latest_backup_$uid")
                .get()
                .addOnSuccessListener { doc ->
                    if (doc != null && doc.exists()) {
                        @Suppress("UNCHECKED_CAST")
                        val rawList = doc.get("reports") as? List<Map<String, Any?>> ?: emptyList()
                        val restoredReports = rawList.mapNotNull { map ->
                            val id = map["id"] as? String ?: return@mapNotNull null
                            val disasterType = map["disasterType"] as? String ?: "Lainnya"
                            val incidentTime = map["incidentTime"] as? String ?: ""
                            val incidentLocation = map["incidentLocation"] as? String ?: ""
                            val district = map["district"] as? String ?: "Mukomuko"
                            val village = map["village"] as? String ?: ""
                            val latitude = (map["latitude"] as? Number)?.toDouble() ?: -2.58
                            val longitude = (map["longitude"] as? Number)?.toDouble() ?: 101.12
                            val gpsAccuracy = (map["gpsAccuracyMeters"] as? Number)?.toFloat()
                            val isGps = map["isGpsVerified"] as? Boolean ?: true
                            val dead = (map["casualtiesDead"] as? Number)?.toInt() ?: 0
                            val injured = (map["casualtiesInjured"] as? Number)?.toInt() ?: 0
                            val missing = (map["casualtiesMissing"] as? Number)?.toInt() ?: 0
                            val photoUri = map["photoUri"] as? String
                            val repName = map["reporterName"] as? String ?: "Warga"
                            val repPhone = map["reporterPhone"] as? String ?: ""
                            val notes = map["additionalNotes"] as? String ?: ""
                            val timestamp = (map["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
                            val status = map["status"] as? String ?: "Menunggu Verifikasi"

                            CitizenDisasterReport(
                                id = id,
                                disasterType = disasterType,
                                incidentTime = incidentTime,
                                incidentLocation = incidentLocation,
                                district = district,
                                village = village,
                                latitude = latitude,
                                longitude = longitude,
                                gpsAccuracyMeters = gpsAccuracy,
                                isGpsVerified = isGpsVerified(isGps),
                                casualtiesDead = dead,
                                casualtiesInjured = injured,
                                casualtiesMissing = missing,
                                photoUri = photoUri,
                                reporterName = repName,
                                reporterPhone = repPhone,
                                additionalNotes = notes,
                                timestamp = timestamp,
                                status = status
                            )
                        }
                        val officerName = doc.getString("officerName") ?: "Pusdalops"
                        onComplete(true, restoredReports, "Berhasil memulihkan ${restoredReports.size} laporan dari cadangan cloud oleh $officerName.")
                    } else {
                        onComplete(false, emptyList(), "Tidak ditemukan berkas cadangan untuk akun ini di cloud.")
                    }
                }
                .addOnFailureListener { e ->
                    onComplete(false, emptyList(), "Gagal mengunduh cadangan: ${e.localizedMessage}")
                }
        } catch (e: Exception) {
            onComplete(false, emptyList(), "Terjadi kesalahan: ${e.localizedMessage}")
        }
    }

    private fun safeReadStringField(
        doc: com.google.firebase.firestore.DocumentSnapshot,
        vararg keys: String
    ): String {
        for (key in keys) {
            val raw = try {
                doc.get(key)
            } catch (_: Exception) {
                null
            }
            val text = when (raw) {
                is String -> raw.trim()
                is Number -> raw.toString().trim()
                else -> raw?.toString()?.trim().orEmpty()
            }
            if (text.isNotBlank()) return text
        }
        return ""
    }

    private fun parseCredentialDocument(doc: com.google.firebase.firestore.DocumentSnapshot): OfficerCredentialEntity? {
        if (!doc.exists()) return null
        val docNip = safeReadStringField(doc, "nip", "badgeId").ifBlank { doc.id.trim() }
        val officialFallback = OfficialAsnBpbdPersonnel.findByNipOrName(docNip)
        val fullName = safeReadStringField(doc, "fullName", "name", "nama")
            .ifBlank { officialFallback?.name.orEmpty() }
            .ifBlank { if (docNip.isNotBlank()) "Petugas $docNip" else "" }
        if (fullName.isBlank() && docNip.isBlank()) return null
        val role = safeReadStringField(doc, "role", "jabatan")
            .ifBlank { officialFallback?.role ?: OfficerRoles.OPERATOR }
        val unit = safeReadStringField(doc, "unit").ifBlank { "Pusdalops BPBD Kab. Mukomuko" }
        val shift = safeReadStringField(doc, "shift").ifBlank { "Shift Siaga 24 Jam" }
        val phone = safeReadStringField(doc, "phone", "telepon")
        val pinHash = safeReadStringField(doc, "pinHash")
        val salt = safeReadStringField(doc, "salt")
        val isCustom = (doc.getBoolean("isPinChanged") == true) ||
                (doc.getBoolean("isCustomPinSet") == true)
        val lastLoginAt = doc.getLong("lastLoginAt") ?: 0L
        val lastLoginDevice = safeReadStringField(doc, "lastLoginDevice")
        val updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()

        return OfficerCredentialEntity(
            nip = docNip,
            fullName = fullName,
            role = role,
            unit = unit,
            shift = shift,
            phone = phone,
            pinHash = pinHash,
            salt = salt,
            isCustomPinSet = isCustom,
            initialSecurePin = "",
            lastLoginAt = lastLoginAt,
            lastLoginDevice = lastLoginDevice,
            isSyncedWithCloud = true,
            updatedAt = updatedAt
        )
    }

    /**
     * Synchronizes a single officer credential to Cloud Firestore without storing plaintext initialSecurePin.
     * Supports both Google-authenticated UID and self-service officer NIP ownership (`officer_nip_{nip}`).
     */
    fun syncCredentialToCloud(
        credential: OfficerCredentialEntity,
        context: Context? = null,
        preserveOwnerUid: String? = null,
        overwriteFullDocument: Boolean = false,
        onSuccess: (() -> Unit)? = null,
        onFailure: ((Exception) -> Unit)? = null
    ) {
        val trimmedNip = credential.nip.trim()
        if (trimmedNip.isBlank()) {
            onFailure?.invoke(IllegalArgumentException("NIP tidak boleh kosong saat sinkronisasi kredensial."))
            return
        }
        val effectiveOwnerUid = preserveOwnerUid?.trim()?.takeIf { it.isNotBlank() }
            ?: currentUid()
            ?: "officer_nip_$trimmedNip"
        val dbResult = getFirestoreOrThrow(context)
        val db = dbResult.getOrNull()
        if (db == null) {
            val initErr = dbResult.exceptionOrNull()
            onFailure?.invoke(
                IllegalStateException(
                    "Gagal sinkronisasi ke Firebase: ${initErr?.localizedMessage ?: "FirebaseFirestore instance null"}",
                    initErr
                )
            )
            return
        }
        ensureAppCheckTokenAttached()
        try {
            val data = mapOf(
                "nip" to trimmedNip,
                "ownerUid" to effectiveOwnerUid,
                "fullName" to credential.fullName,
                "role" to credential.role,
                "unit" to credential.unit,
                "shift" to credential.shift,
                "phone" to credential.phone,
                "pinHash" to credential.pinHash,
                "salt" to credential.salt,
                "isCustomPinSet" to credential.isCustomPinSet,
                "isPinChanged" to credential.isCustomPinSet,
                "initialSecurePin" to "",
                "lastLoginAt" to credential.lastLoginAt,
                "lastLoginDevice" to credential.lastLoginDevice,
                "updatedAt" to credential.updatedAt
            )
            val docRef = db.collection(COLLECTION_OFFICER_CREDENTIALS).document(trimmedNip)
            val writeTask = if (overwriteFullDocument) {
                docRef.set(data)
            } else {
                docRef.set(data, SetOptions.merge())
            }
            writeTask
                .addOnSuccessListener {
                    Log.d(TAG, "Credential synced to Firestore (officer_credentials/$trimmedNip, overwrite=$overwriteFullDocument)")
                    onSuccess?.invoke()
                }
                .addOnFailureListener { e ->
                    logFirestoreError("syncCredentialToCloud(officer_credentials/$trimmedNip)", e)
                    onFailure?.invoke(e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during credential cloud sync", e)
            onFailure?.invoke(e)
        }
    }

    suspend fun syncCredentialToCloudSuspend(
        credential: OfficerCredentialEntity,
        context: Context? = null,
        preserveOwnerUid: String? = null,
        overwriteFullDocument: Boolean = false
    ): Result<Unit> {
        overrideCloudCredentialWriterForTesting?.let { writer ->
            return writer(credential)
        }
        return withTimeoutOrNull(12_000L) {
            suspendCancellableCoroutine { cont ->
                syncCredentialToCloud(
                    credential = credential,
                    context = context,
                    preserveOwnerUid = preserveOwnerUid,
                    overwriteFullDocument = overwriteFullDocument,
                    onSuccess = {
                        if (cont.isActive) cont.resume(Result.success(Unit))
                    },
                    onFailure = { e ->
                        if (cont.isActive) {
                            val fsEx = e as? FirebaseFirestoreException
                            val msg = when (fsEx?.code) {
                                FirebaseFirestoreException.Code.UNAVAILABLE,
                                FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ->
                                    "Koneksi internet bermasalah. Gagal menyimpan PIN baru ke server Firebase."
                                FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                                    "Izin tulis ke koleksi officer_credentials ditolak oleh Firestore Rules."
                                else -> "Gagal menyimpan kredensial ke Firebase: ${e.localizedMessage ?: "Kesalahan tidak diketahui"}"
                            }
                            cont.resume(Result.failure(IllegalStateException(msg, e)))
                        }
                    }
                )
            }
        } ?: Result.failure(
            IllegalStateException("Waktu penyimpanan kredensial ke server Firebase habis (DEADLINE_EXCEEDED). Periksa koneksi internet lalu coba lagi.")
        )
    }

    /**
     * Purges any simulation login session and admin audit trace documents in Cloud Firestore for the target NIP.
     */
    suspend fun purgeOfficerCloudLoginTracesSuspend(
        nip: String,
        context: Context? = null
    ) {
        val trimmedNip = nip.trim()
        if (trimmedNip.isBlank()) return
        if (overrideCloudCredentialWriterForTesting != null || overrideCloudCredentialFetcherForTesting != null) {
            return
        }
        val db = getFirestoreOrThrow(context).getOrNull() ?: return
        ensureAppCheckTokenAttached()
        withTimeoutOrNull(8_000L) {
            try {
                suspendCancellableCoroutine<Unit> { cont ->
                    db.collection("officer_sessions")
                        .document(trimmedNip)
                        .delete()
                        .addOnCompleteListener {
                            if (cont.isActive) cont.resume(Unit)
                        }
                }
            } catch (_: Exception) {
            }
            try {
                suspendCancellableCoroutine<Unit> { cont ->
                    db.collection("device_security_sessions")
                        .document(trimmedNip)
                        .delete()
                        .addOnCompleteListener {
                            if (cont.isActive) cont.resume(Unit)
                        }
                }
            } catch (_: Exception) {
            }
            val uid = currentUid()
            if (!uid.isNullOrBlank()) {
                try {
                    suspendCancellableCoroutine<Unit> { cont ->
                        db.collection("admin_access_audit_logs")
                            .whereEqualTo("officerNip", trimmedNip)
                            .get(Source.SERVER)
                            .addOnSuccessListener { snap ->
                                snap?.documents?.forEach { doc ->
                                    doc.reference.delete()
                                }
                                if (cont.isActive) cont.resume(Unit)
                            }
                            .addOnFailureListener {
                                if (cont.isActive) cont.resume(Unit)
                            }
                    }
                } catch (_: Exception) {
                }
                try {
                    suspendCancellableCoroutine<Unit> { cont ->
                        db.collection("device_login_activities")
                            .whereEqualTo("officerNip", trimmedNip)
                            .whereEqualTo("ownerUid", uid)
                            .get(Source.SERVER)
                            .addOnSuccessListener { snap ->
                                snap?.documents?.forEach { doc ->
                                    doc.reference.delete()
                                }
                                if (cont.isActive) cont.resume(Unit)
                            }
                            .addOnFailureListener {
                                if (cont.isActive) cont.resume(Unit)
                            }
                    }
                } catch (_: Exception) {
                }
            }
        }
    }

    private suspend fun fetchSingleCredentialDocFromServer(
        db: FirebaseFirestore,
        documentId: String
    ): CloudCredentialLookupResult {
        return withTimeoutOrNull(15_000L) {
            suspendCancellableCoroutine { cont ->
                try {
                    db.collection(COLLECTION_OFFICER_CREDENTIALS)
                        .document(documentId)
                        .get(Source.SERVER)
                        .addOnSuccessListener { doc ->
                            if (!cont.isActive) return@addOnSuccessListener
                            if (doc == null || !doc.exists()) {
                                cont.resume(CloudCredentialLookupResult.NotFound)
                                return@addOnSuccessListener
                            }
                            val parsed = parseCredentialDocument(doc)
                            if (parsed == null) {
                                cont.resume(CloudCredentialLookupResult.NotFound)
                                return@addOnSuccessListener
                            }
                            val existingOwnerUid = safeReadStringField(doc, "ownerUid")
                            // Support optional server-side PIN field (String or numeric) if configured by Admin in Firestore Console
                            val plainPinField = safeReadStringField(
                                doc,
                                "pin",
                                "personalPin",
                                "pinCode",
                                "customPin",
                                "kodePin",
                                "pinPetugas",
                                "pin_petugas",
                                "pinLogin"
                            ).takeIf { it.isNotBlank() }
                            cont.resume(
                                CloudCredentialLookupResult.Found(
                                    credential = parsed,
                                    serverPlainPin = plainPinField,
                                    ownerUid = existingOwnerUid
                                )
                            )
                        }
                        .addOnFailureListener { e ->
                            logFirestoreError("fetchOfficerCredentialStrictFromServer(officer_credentials/$documentId)", e)
                            if (!cont.isActive) return@addOnFailureListener
                            val fsEx = e as? FirebaseFirestoreException
                            val errorResult = when (fsEx?.code) {
                                FirebaseFirestoreException.Code.UNAVAILABLE -> CloudCredentialLookupResult.Error(
                                    userMessage = "Koneksi internet bermasalah atau server Firebase tidak terjangkau (UNAVAILABLE). Verifikasi PIN wajib online.",
                                    errorCode = "UNAVAILABLE",
                                    cause = e
                                )
                                FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> CloudCredentialLookupResult.Error(
                                    userMessage = "Waktu verifikasi ke server Firebase habis (DEADLINE_EXCEEDED). Periksa sinyal lalu coba lagi.",
                                    errorCode = "DEADLINE_EXCEEDED",
                                    cause = e
                                )
                                FirebaseFirestoreException.Code.PERMISSION_DENIED -> CloudCredentialLookupResult.Error(
                                    userMessage = "Akses verifikasi ke koleksi officer_credentials ditolak server (PERMISSION_DENIED).",
                                    errorCode = "PERMISSION_DENIED",
                                    cause = e
                                )
                                else -> CloudCredentialLookupResult.Error(
                                    userMessage = "Gagal terhubung ke server Firebase untuk verifikasi PIN: ${e.localizedMessage ?: "Periksa koneksi internet."}",
                                    errorCode = fsEx?.code?.name ?: "NETWORK_ERROR",
                                    cause = e
                                )
                            }
                            cont.resume(errorResult)
                        }
                } catch (e: Exception) {
                    logFirestoreError("fetchOfficerCredentialStrictFromServer(exception)", e)
                    if (cont.isActive) {
                        cont.resume(
                            CloudCredentialLookupResult.Error(
                                userMessage = "Terjadi kesalahan saat memverifikasi PIN ke Firebase: ${e.localizedMessage ?: "Coba lagi."}",
                                errorCode = "EXCEPTION",
                                cause = e
                            )
                        )
                    }
                }
            }
        } ?: CloudCredentialLookupResult.Error(
            userMessage = "Waktu verifikasi ke server Firebase habis (DEADLINE_EXCEEDED). Periksa koneksi internet lalu coba lagi.",
            errorCode = "DEADLINE_EXCEEDED"
        )
    }

    /**
     * Mandatory Cloud Verification read (`Source.SERVER`) on `officer_credentials/{nip}`.
     * Never falls back to offline cache or hardcoded default PINs.
     */
    suspend fun fetchOfficerCredentialStrictFromServer(
        nip: String,
        context: Context? = null
    ): CloudCredentialLookupResult {
        val trimmedNip = nip.trim()
        if (trimmedNip.isBlank()) {
            return CloudCredentialLookupResult.NotFound
        }

        overrideCloudCredentialFetcherForTesting?.let { fetcher ->
            return fetcher(trimmedNip)
        }

        val dbResult = getFirestoreOrThrow(context)
        val db = dbResult.getOrNull()
        if (db == null) {
            val initErr = dbResult.exceptionOrNull()
            return CloudCredentialLookupResult.Error(
                userMessage = "Firebase belum siap untuk verifikasi PIN. Pastikan koneksi internet aktif lalu coba lagi.",
                errorCode = "FIRESTORE_INIT_ERROR",
                cause = initErr
            )
        }

        ensureAppCheckTokenAttached()

        var primaryResult = fetchSingleCredentialDocFromServer(db, trimmedNip)
        // Resilient 1x retry ONLY on transient transport/handshake errors (cold-start gRPC / App Check warmup).
        // Never falls back to local cache or default PINs; strictly queries Source.SERVER.
        if (primaryResult is CloudCredentialLookupResult.Error &&
            (primaryResult.errorCode == "UNAVAILABLE" || primaryResult.errorCode == "DEADLINE_EXCEEDED")
        ) {
            Log.w(TAG, "Cloud PIN lookup transient ${primaryResult.errorCode}; retrying once after bounded network reconnect...")
            delay(600L)
            withTimeoutOrNull(2_000L) {
                suspendCancellableCoroutine<Unit> { cont ->
                    try {
                        db.enableNetwork()
                            .addOnCompleteListener {
                                if (cont.isActive) cont.resume(Unit)
                            }
                    } catch (_: Exception) {
                        if (cont.isActive) cont.resume(Unit)
                    }
                }
            }
            primaryResult = fetchSingleCredentialDocFromServer(db, trimmedNip)
        }
        if (primaryResult !is CloudCredentialLookupResult.NotFound) {
            return primaryResult
        }

        // Fallback alternate valid document IDs if Admin stored underscored NIP or sanitized official name in Firestore
        val fallbackIds = buildList {
            if (trimmedNip.length == 18 && trimmedNip.all { it.isDigit() }) {
                add("${trimmedNip.substring(0, 8)}_${trimmedNip.substring(8, 14)}_${trimmedNip.substring(14, 15)}_${trimmedNip.substring(15, 18)}")
            }
            val officialName = OfficialAsnBpbdPersonnel.findByNipOrName(trimmedNip)?.name.orEmpty()
            if (officialName.isNotBlank()) {
                val sanitizedUpper = officialName.trim().replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
                val sanitizedLower = sanitizedUpper.lowercase()
                if (sanitizedUpper.isNotBlank()) add(sanitizedUpper)
                if (sanitizedLower.isNotBlank() && sanitizedLower != sanitizedUpper) add(sanitizedLower)
            }
        }.distinct().filter { it != trimmedNip && it.matches(Regex("^[a-zA-Z0-9_\\-]{1,128}$")) }

        for (candidateId in fallbackIds) {
            val candidateResult = fetchSingleCredentialDocFromServer(db, candidateId)
            if (candidateResult is CloudCredentialLookupResult.Found) {
                return candidateResult.copy(
                    credential = candidateResult.credential.copy(nip = trimmedNip)
                )
            }
        }

        return CloudCredentialLookupResult.NotFound
    }

    suspend fun fetchCredentialByNipFromCloud(
        nip: String,
        context: Context? = null
    ): Result<OfficerCredentialEntity?> {
        return when (val lookup = fetchOfficerCredentialStrictFromServer(nip, context)) {
            is CloudCredentialLookupResult.Found -> Result.success(lookup.credential)
            is CloudCredentialLookupResult.NotFound -> Result.success(null)
            is CloudCredentialLookupResult.Error -> Result.failure(
                IllegalStateException(lookup.userMessage, lookup.cause)
            )
        }
    }

    suspend fun fetchAllCredentialsFromCloudSuspend(): Result<List<OfficerCredentialEntity>> =
        withTimeoutOrNull(8_000L) {
            suspendCancellableCoroutine { cont ->
                val uid = currentUid()
                if (uid == null) {
                    if (cont.isActive) {
                        cont.resume(Result.failure(SecurityException("Autentikasi Google diperlukan untuk sinkronisasi cloud.")))
                    }
                    return@suspendCancellableCoroutine
                }
                val dbResult = getFirestoreOrThrow()
                val db = dbResult.getOrNull()
                if (db == null) {
                    val initErr = dbResult.exceptionOrNull()
                    if (cont.isActive) {
                        cont.resume(
                            Result.failure(
                                IllegalStateException(
                                    "Gagal sinkronisasi: ${initErr?.localizedMessage ?: "FirebaseFirestore instance null"}",
                                    initErr
                                )
                            )
                        )
                    }
                    return@suspendCancellableCoroutine
                }
                try {
                    db.collection(COLLECTION_OFFICER_CREDENTIALS)
                        .whereEqualTo("ownerUid", uid)
                        .get(Source.SERVER)
                        .addOnSuccessListener { snapshot ->
                            if (!cont.isActive) return@addOnSuccessListener
                            val list = mutableListOf<OfficerCredentialEntity>()
                            if (snapshot != null) {
                                for (doc in snapshot.documents) {
                                    parseCredentialDocument(doc)?.let { list.add(it) }
                                }
                            }
                            cont.resume(Result.success(list))
                        }
                        .addOnFailureListener { e ->
                            Log.w(TAG, "Failed to fetch all credentials from Firestore server", e)
                            if (cont.isActive) {
                                cont.resume(
                                    Result.failure(
                                        IllegalStateException("Gagal sinkronisasi: ${e.localizedMessage}", e)
                                    )
                                )
                            }
                        }
                } catch (e: Exception) {
                    if (cont.isActive) {
                        cont.resume(
                            Result.failure(
                                IllegalStateException("Gagal sinkronisasi: ${e.localizedMessage}", e)
                            )
                        )
                    }
                }
            }
        } ?: Result.failure(IllegalStateException("Waktu sinkronisasi kredensial dari server Firebase habis (timeout 8 detik)."))

    fun fetchAllCredentialsFromCloud(onResult: (List<OfficerCredentialEntity>) -> Unit) {
        val uid = currentUid()
        if (uid == null) {
            onResult(emptyList())
            return
        }
        val db = firestore
        if (db == null) {
            onResult(emptyList())
            return
        }
        try {
            db.collection(COLLECTION_OFFICER_CREDENTIALS)
                .whereEqualTo("ownerUid", uid)
                .get(Source.SERVER)
                .addOnSuccessListener { snapshot ->
                    val list = mutableListOf<OfficerCredentialEntity>()
                    if (snapshot != null) {
                        for (doc in snapshot.documents) {
                            parseCredentialDocument(doc)?.let { list.add(it) }
                        }
                    }
                    onResult(list)
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Failed to fetch credentials from Firestore", e)
                    onResult(emptyList())
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception fetching credentials from cloud", e)
            onResult(emptyList())
        }
    }

    /**
     * Mengambil konfigurasi dokumen PDF dinamis (`fontSize`, `maxLengthBeforeTruncate`, `gelarFormat`,
     * `signatories`, `officers`, kop surat, footer) dari Firestore `app_settings/document_settings`.
     */
    suspend fun fetchDocumentSettingsFromCloudSuspend(): Result<Map<String, Any>?> =
        suspendCancellableCoroutine { cont ->
            val dbResult = getFirestoreOrThrow()
            val db = dbResult.getOrNull()
            if (db == null) {
                val err = dbResult.exceptionOrNull() ?: IllegalStateException("Firestore tidak tersedia")
                if (cont.isActive) cont.resume(Result.failure(err))
                return@suspendCancellableCoroutine
            }
            ensureAppCheckTokenAttached()
            try {
                db.collection(COLLECTION_APP_SETTINGS)
                    .document(DOC_DOCUMENT_SETTINGS)
                    .get(Source.SERVER)
                    .addOnSuccessListener { doc ->
                        if (!cont.isActive) return@addOnSuccessListener
                        val data = if (doc != null && doc.exists()) doc.data else null
                        cont.resume(Result.success(data))
                    }
                    .addOnFailureListener { e ->
                        logFirestoreError("fetchDocumentSettingsFromCloudSuspend", e)
                        if (cont.isActive) cont.resume(Result.failure(e))
                    }
            } catch (e: Exception) {
                logFirestoreError("fetchDocumentSettingsFromCloudSuspend(exception)", e)
                if (cont.isActive) cont.resume(Result.failure(e))
            }
        }

    /**
     * Mengambil direktori personil (Nama, Gelar, NIP, Jabatan) terbaru dari koleksi Firestore `officers`.
     * Tidak mewajibkan sesi Google Sign-In agar perangkat baru dapat mengenali petugas yang baru diinput manual.
     */
    suspend fun fetchOfficersDirectoryFromCloudSuspend(
        context: Context? = null
    ): Result<List<OfficerProfile>> {
        overrideOfficersDirectoryFetcherForTesting?.let { fetcher ->
            return fetcher()
        }
        if (overrideCloudCredentialFetcherForTesting != null && overrideFirestoreForTesting == null) {
            return Result.success(emptyList())
        }
        return withTimeoutOrNull(8_000L) {
            suspendCancellableCoroutine { cont ->
                val dbResult = getFirestoreOrThrow(context)
                val db = dbResult.getOrNull()
                if (db == null) {
                    val err = dbResult.exceptionOrNull() ?: IllegalStateException("Firestore tidak tersedia")
                    if (cont.isActive) cont.resume(Result.failure(err))
                    return@suspendCancellableCoroutine
                }
                ensureAppCheckTokenAttached()
                try {
                    db.collection(COLLECTION_OFFICERS)
                        .get(Source.SERVER)
                        .addOnSuccessListener { snapshot ->
                            if (!cont.isActive) return@addOnSuccessListener
                            val list = mutableListOf<OfficerProfile>()
                            if (snapshot != null) {
                                for (doc in snapshot.documents) {
                                    val id = doc.getString("id") ?: doc.id
                                    val fullName = doc.getString("fullName")?.trim() ?: continue
                                    if (fullName.isBlank()) continue
                                    val nip = (doc.getString("nip") ?: doc.id).trim()
                                    list.add(
                                        OfficerProfile(
                                            id = id,
                                            fullName = fullName,
                                            nip = nip,
                                            role = doc.getString("role") ?: OfficerRoles.PETUGAS_JAGA,
                                            unit = doc.getString("unit") ?: "Pusdalops BPBD Kab. Mukomuko",
                                            shift = doc.getString("shift") ?: "Shift Siaga 24 Jam",
                                            phone = doc.getString("phone") ?: ""
                                        )
                                    )
                                }
                            }
                            cont.resume(Result.success(list))
                        }
                        .addOnFailureListener { e ->
                            logFirestoreError("fetchOfficersDirectoryFromCloudSuspend", e)
                            if (cont.isActive) cont.resume(Result.failure(e))
                        }
                } catch (e: Exception) {
                    logFirestoreError("fetchOfficersDirectoryFromCloudSuspend(exception)", e)
                    if (cont.isActive) cont.resume(Result.failure(e))
                }
            }
        } ?: Result.failure(IllegalStateException("Waktu memuat direktori petugas dari server Firebase habis (timeout 8 detik)."))
    }

    /**
     * Mengambil profil petugas tunggal berdasarkan NIP dari koleksi Firestore `officers/{nip}`.
     */
    suspend fun fetchOfficerProfileFromOfficersCollectionSuspend(
        nip: String,
        context: Context? = null
    ): OfficerProfile? {
        val cleanNip = nip.trim().replace(Regex("[\\s\\-]+"), "")
        if (cleanNip.isBlank()) return null

        overrideOfficersDirectoryFetcherForTesting?.let { fetcher ->
            return fetcher().getOrNull()?.firstOrNull {
                it.nip.trim().replace(Regex("[\\s\\-]+"), "").equals(cleanNip, ignoreCase = true)
            }
        }

        if (overrideCloudCredentialFetcherForTesting != null && overrideFirestoreForTesting == null) {
            return null
        }

        val db = getFirestoreOrThrow(context).getOrNull() ?: return null
        ensureAppCheckTokenAttached()
        return withTimeoutOrNull(2500L) {
            suspendCancellableCoroutine { cont ->
                try {
                    db.collection(COLLECTION_OFFICERS)
                        .document(cleanNip)
                        .get(Source.SERVER)
                        .addOnSuccessListener { doc ->
                            if (!cont.isActive) return@addOnSuccessListener
                            if (doc != null && doc.exists()) {
                                val fullName = doc.getString("fullName")?.trim().orEmpty()
                                if (fullName.isNotBlank()) {
                                    cont.resume(
                                        OfficerProfile(
                                            id = doc.getString("id") ?: doc.id,
                                            fullName = fullName,
                                            nip = (doc.getString("nip") ?: cleanNip).trim(),
                                            role = doc.getString("role") ?: OfficerRoles.PETUGAS_JAGA,
                                            unit = doc.getString("unit") ?: "Pusdalops BPBD Kab. Mukomuko",
                                            shift = doc.getString("shift") ?: "Shift Siaga 24 Jam",
                                            phone = doc.getString("phone") ?: ""
                                        )
                                    )
                                    return@addOnSuccessListener
                                }
                            }
                            cont.resume(null)
                        }
                        .addOnFailureListener {
                            if (cont.isActive) cont.resume(null)
                        }
                } catch (_: Exception) {
                    if (cont.isActive) cont.resume(null)
                }
            }
        }
    }

    /**
     * Mengambil field `role` untuk NIP petugas tertentu dari koleksi Firestore `officers`
     * (dengan timeout singkat agar tidak menghambat proses login jika koleksi `officers` belum diisi).
     */
    suspend fun fetchOfficerRoleFromOfficersCollectionSuspend(
        nip: String,
        context: Context? = null
    ): String? {
        val cleanNip = nip.trim()
        if (cleanNip.isBlank()) return null

        overrideOfficersCollectionRoleFetcherForTesting?.let { fetcher ->
            return fetcher(cleanNip)
        }

        // Jika dalam mode unit test mock credential fetcher, lewati jaringan Firestore langsung
        if (overrideCloudCredentialFetcherForTesting != null && overrideFirestoreForTesting == null) {
            return null
        }

        val db = getFirestoreOrThrow(context).getOrNull() ?: return null
        return withTimeoutOrNull(2500L) {
            suspendCancellableCoroutine { cont ->
                try {
                    db.collection(COLLECTION_OFFICERS)
                        .document(cleanNip)
                        .get(Source.SERVER)
                        .addOnSuccessListener { doc ->
                            if (!cont.isActive) return@addOnSuccessListener
                            val directRole = doc?.takeIf { it.exists() }?.getString("role")?.trim()
                            if (!directRole.isNullOrBlank()) {
                                cont.resume(directRole)
                            } else {
                                cont.resume(null)
                            }
                        }
                        .addOnFailureListener {
                            if (cont.isActive) cont.resume(null)
                        }
                } catch (_: Exception) {
                    if (cont.isActive) cont.resume(null)
                }
            }
        }
    }

    private fun isGpsVerified(value: Boolean): Boolean = value
}
