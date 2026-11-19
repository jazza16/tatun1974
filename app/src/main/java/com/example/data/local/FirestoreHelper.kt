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
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Hasil verifikasi/pembacaan dokumen kredensial petugas `officer_credentials/{nip}`
 * langsung dari server Cloud Firestore (`Source.SERVER`).
 */
sealed class CloudCredentialLookupResult {
    data class Found(
        val credential: OfficerCredentialEntity,
        val serverPlainPin: String? = null
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

    @Volatile
    private var cachedFirestore: FirebaseFirestore? = null

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

    fun resetTestHooks() {
        overrideFirestoreForTesting = null
        overrideCloudCredentialFetcherForTesting = null
        overrideCloudCredentialWriterForTesting = null
    }

    private fun currentUid(): String? = try {
        FirebaseAuth.getInstance().currentUser?.uid
    } catch (_: Exception) {
        null
    }

    private fun ensureAppCheckTokenAttached() {
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

    private val firestore: FirebaseFirestore?
        get() = getFirestoreOrThrow(null).getOrNull()

    private fun getFirestoreOrThrow(context: Context? = null): Result<FirebaseFirestore> {
        overrideFirestoreForTesting?.let { return Result.success(it) }
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
            cachedFirestore = instance
            Result.success(instance)
        } catch (e: Exception) {
            Log.e(TAG, "FirebaseFirestore instance unavailable: ${e.localizedMessage}", e)
            Result.failure(e)
        }
    }

    /**
     * Real-time listener for BPBD Officer / Personnel roster scoped to the authenticated account.
     */
    fun listenOfficersRealtime(
        onUpdate: (List<OfficerProfile>) -> Unit,
        onError: ((Exception) -> Unit)? = null
    ): ListenerRegistration? {
        val uid = currentUid()
        if (uid == null) {
            Log.d(TAG, "listenOfficersRealtime skipped: user not authenticated with Google.")
            return null
        }
        val db = firestore ?: return null
        return try {
            db.collection(COLLECTION_OFFICERS)
                .whereEqualTo("ownerUid", uid)
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
                            val fullName = doc.getString("fullName") ?: continue
                            val nip = doc.getString("nip") ?: ""
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
     * Pushes officer profile data to Firestore so authorized devices under this account stay in sync.
     */
    fun syncOfficerToCloud(officer: OfficerProfile) {
        val uid = currentUid() ?: return
        val db = firestore ?: return
        try {
            val docId = if (officer.id.isNotBlank()) officer.id else officer.nip
            val data = mapOf(
                "id" to officer.id,
                "ownerUid" to uid,
                "fullName" to officer.fullName,
                "nip" to officer.nip,
                "role" to officer.role,
                "unit" to officer.unit,
                "shift" to officer.shift,
                "phone" to officer.phone,
                "updatedAt" to System.currentTimeMillis()
            )
            db.collection(COLLECTION_OFFICERS)
                .document(docId)
                .set(data, SetOptions.merge())
                .addOnSuccessListener {
                    Log.d(TAG, "Officer successfully synced to Firestore: ${officer.fullName}")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Failed to sync officer to Firestore", e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during officer cloud sync", e)
        }
    }

    /**
     * Removes officer profile from Firestore.
     */
    fun deleteOfficerFromCloud(officerId: String) {
        if (currentUid() == null) return
        val db = firestore ?: return
        try {
            db.collection(COLLECTION_OFFICERS)
                .document(officerId)
                .delete()
                .addOnSuccessListener {
                    Log.d(TAG, "Officer $officerId deleted from Firestore")
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
        val photoUri = doc.getString("photoUri")
        val reporterName = doc.getString("reporterName") ?: "Warga"
        val reporterPhone = doc.getString("reporterPhone") ?: ""
        val notes = doc.getString("additionalNotes") ?: ""
        val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
        val status = doc.getString("status") ?: "Menunggu Verifikasi"

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
            status = status
        )
    }

    /**
     * Real-time listener for all Citizen Disaster Reports across citizen & officer devices.
     */
    fun listenCitizenReportsRealtime(
        onUpdate: (List<CitizenDisasterReport>) -> Unit,
        onError: ((Exception) -> Unit)? = null
    ): ListenerRegistration? {
        val db = firestore ?: return null
        ensureAppCheckTokenAttached()
        return try {
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
                        onUpdate(reports)
                    }
                }
        } catch (e: Exception) {
            logFirestoreError("attachCitizenReportsRealtimeListener", e)
            null
        }
    }

    /**
     * Actively fetches the latest Citizen Disaster Reports directly from Firestore SERVER
     * (.get(Source.SERVER)) so Officer devices always display up-to-date citizen reports.
     */
    suspend fun fetchCitizenReportsFromServerSuspend(): Result<List<CitizenDisasterReport>> =
        suspendCancellableCoroutine { cont ->
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

    /**
     * Uploads or updates a citizen disaster report in Firestore.
     */
    fun syncReportToCloud(report: CitizenDisasterReport) {
        val uid = currentUid() ?: "PUBLIC_CITIZEN_MUKOMUKO"
        val db = firestore ?: return
        ensureAppCheckTokenAttached()
        try {
            val data = mapOf(
                "id" to report.id,
                "authorUid" to uid,
                "disasterType" to report.disasterType,
                "incidentTime" to report.incidentTime,
                "incidentLocation" to report.incidentLocation,
                "district" to report.district,
                "village" to report.village,
                "latitude" to report.latitude,
                "longitude" to report.longitude,
                "gpsAccuracyMeters" to report.gpsAccuracyMeters,
                "isGpsVerified" to report.isGpsVerified,
                "casualtiesDead" to report.casualtiesDead,
                "casualtiesInjured" to report.casualtiesInjured,
                "casualtiesMissing" to report.casualtiesMissing,
                "photoUri" to report.photoUri,
                "reporterName" to report.reporterName,
                "reporterPhone" to report.reporterPhone,
                "additionalNotes" to report.additionalNotes,
                "timestamp" to report.timestamp,
                "status" to report.status
            ).filterValues { it != null }
            db.collection(COLLECTION_CITIZEN_REPORTS)
                .document(report.id)
                .set(data, SetOptions.merge())
                .addOnSuccessListener {
                    Log.d(TAG, "Report ${report.id} synced to Firestore")
                }
                .addOnFailureListener { e ->
                    logFirestoreError("syncReportToCloud(${report.id})", e)
                }
        } catch (e: Exception) {
            logFirestoreError("syncReportToCloud(${report.id})", e)
        }
    }

    /**
     * Updates only the report status and optional response notes in Firestore.
     */
    fun updateReportStatusInCloud(reportId: String, newStatus: String, notes: String? = null) {
        val db = firestore ?: return
        ensureAppCheckTokenAttached()
        try {
            val updateMap = mutableMapOf<String, Any>(
                "status" to newStatus,
                "updatedAt" to System.currentTimeMillis()
            )
            if (notes != null) {
                updateMap["additionalNotes"] = notes
            }
            db.collection(COLLECTION_CITIZEN_REPORTS)
                .document(reportId)
                .update(updateMap)
                .addOnSuccessListener {
                    Log.d(TAG, "Report status updated to $newStatus in Firestore")
                }
                .addOnFailureListener { e ->
                    logFirestoreError("updateReportStatusInCloud($reportId)", e)
                }
        } catch (e: Exception) {
            logFirestoreError("updateReportStatusInCloud($reportId)", e)
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

    private fun parseCredentialDocument(doc: com.google.firebase.firestore.DocumentSnapshot): OfficerCredentialEntity? {
        if (!doc.exists()) return null
        val docNip = doc.getString("nip") ?: doc.id
        val fullName = doc.getString("fullName") ?: ""
        if (fullName.isBlank() && docNip.isBlank()) return null
        val role = doc.getString("role") ?: OfficerRoles.OPERATOR
        val unit = doc.getString("unit") ?: "Pusdalops BPBD Kab. Mukomuko"
        val shift = doc.getString("shift") ?: "Shift Siaga 24 Jam"
        val phone = doc.getString("phone") ?: ""
        val pinHash = doc.getString("pinHash") ?: ""
        val salt = doc.getString("salt") ?: ""
        val isCustom = (doc.getBoolean("isPinChanged") == true) ||
                (doc.getBoolean("isCustomPinSet") == true)
        val lastLoginAt = doc.getLong("lastLoginAt") ?: 0L
        val lastLoginDevice = doc.getString("lastLoginDevice") ?: ""
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
        onSuccess: (() -> Unit)? = null,
        onFailure: ((Exception) -> Unit)? = null
    ) {
        val trimmedNip = credential.nip.trim()
        if (trimmedNip.isBlank()) {
            onFailure?.invoke(IllegalArgumentException("NIP tidak boleh kosong saat sinkronisasi kredensial."))
            return
        }
        val effectiveOwnerUid = currentUid() ?: "officer_nip_$trimmedNip"
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
            db.collection(COLLECTION_OFFICER_CREDENTIALS)
                .document(trimmedNip)
                .set(data, SetOptions.merge())
                .addOnSuccessListener {
                    Log.d(TAG, "Credential synced to Firestore (officer_credentials/$trimmedNip)")
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
        context: Context? = null
    ): Result<Unit> {
        overrideCloudCredentialWriterForTesting?.let { writer ->
            return writer(credential)
        }
        return suspendCancellableCoroutine { cont ->
            syncCredentialToCloud(
                credential = credential,
                context = context,
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
        return suspendCancellableCoroutine { cont ->
            try {
                try {
                    db.enableNetwork()
                } catch (_: Exception) {
                }

                db.collection(COLLECTION_OFFICER_CREDENTIALS)
                    .document(trimmedNip)
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
                        // Support optional server-side PIN field if configured directly by Admin in Firestore Console
                        val plainPinField = doc.getString("pin")?.trim()?.takeIf { it.isNotBlank() }
                            ?: doc.getString("personalPin")?.trim()?.takeIf { it.isNotBlank() }
                        cont.resume(
                            CloudCredentialLookupResult.Found(
                                credential = parsed,
                                serverPlainPin = plainPinField
                            )
                        )
                    }
                    .addOnFailureListener { e ->
                        logFirestoreError("fetchOfficerCredentialStrictFromServer(officer_credentials/$trimmedNip)", e)
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
     */
    suspend fun fetchOfficersDirectoryFromCloudSuspend(): Result<List<OfficerProfile>> =
        suspendCancellableCoroutine { cont ->
            val uid = currentUid()
            if (uid == null) {
                if (cont.isActive) cont.resume(Result.success(emptyList()))
                return@suspendCancellableCoroutine
            }
            val dbResult = getFirestoreOrThrow()
            val db = dbResult.getOrNull()
            if (db == null) {
                val err = dbResult.exceptionOrNull() ?: IllegalStateException("Firestore tidak tersedia")
                if (cont.isActive) cont.resume(Result.failure(err))
                return@suspendCancellableCoroutine
            }
            ensureAppCheckTokenAttached()
            try {
                db.collection(COLLECTION_OFFICERS)
                    .whereEqualTo("ownerUid", uid)
                    .get(Source.SERVER)
                    .addOnSuccessListener { snapshot ->
                        if (!cont.isActive) return@addOnSuccessListener
                        val list = mutableListOf<OfficerProfile>()
                        if (snapshot != null) {
                            for (doc in snapshot.documents) {
                                val id = doc.getString("id") ?: doc.id
                                val fullName = doc.getString("fullName") ?: continue
                                list.add(
                                    OfficerProfile(
                                        id = id,
                                        fullName = fullName,
                                        nip = doc.getString("nip") ?: "",
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

    private fun isGpsVerified(value: Boolean): Boolean = value
}
