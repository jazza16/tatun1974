package com.example.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Log
import com.example.data.local.InitialDummyVolunteers
import com.example.data.local.VolunteerDao
import com.example.data.local.VolunteerDatabase
import com.example.data.local.VolunteerEntity
import com.example.data.local.VolunteerPrivacyHelper
import com.example.data.local.VolunteerStatus
import com.example.training.SandboxTrainingManager
import com.example.util.VolunteerAssignmentPdfGenerator
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.UUID

/**
 * Hasil Validasi & Pendaftaran Relawan (Anti Data Ganda dengan NIK 16 Digit sebagai Primary Key).
 */
sealed class VolunteerRegistrationResult {
    data class Success(val volunteer: VolunteerEntity) : VolunteerRegistrationResult()
    data class DuplicateNik(
        val message: String = VolunteerPrivacyHelper.DUPLICATE_NIK_ALERT_MESSAGE
    ) : VolunteerRegistrationResult()
    data class ValidationError(val message: String) : VolunteerRegistrationResult()
}

/**
 * Repository Offline-First untuk Modul "Pendataan & Koordinasi Relawan" (SIPAGAR KITA BPBD Mukomuko).
 * - NIK (16 digit) digunakan sebagai Primary Key di SQLite (`bpbd_volunteers`) dan dokumen Cloud Firestore.
 * - Mencegah pendaftaran NIK ganda dengan alert: "NIK sudah terdaftar, silakan hubungi admin."
 * - Mencatat timestamp Check-in (`waktu_checkin`) & Check-out (`waktu_checkout`) kehadiran Posko secara real-time.
 * - Menghasilkan file PDF "SURAT TUGAS RELAWAN" secara otomatis saat Admin menyimpan penugasan.
 */
class VolunteerRepository private constructor(
    private val appContext: Context,
    private val dao: VolunteerDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val allVolunteers: Flow<List<VolunteerEntity>> = dao.observeAllVolunteers()

    private val _isOnline = MutableStateFlow(checkNetworkOnline(appContext))
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastSyncStatus = MutableStateFlow("SQLite Lokal Siap • Auto-Sync Firestore Aktif")
    val lastSyncStatus: StateFlow<String> = _lastSyncStatus.asStateFlow()

    private var firestoreListener: ListenerRegistration? = null
    private var networkCallbackRegistered = false

    init {
        scope.launch {
            ensureDummyDataSeeded()
            registerAutoSyncNetworkCallback()
            if (_isOnline.value && !SandboxTrainingManager.isSandboxModeActive()) {
                syncPendingOfflineVolunteersToFirestore()
                startRealtimeFirestoreListener()
            }
        }
    }

    suspend fun ensureDummyDataSeeded() {
        try {
            if (dao.getCount() == 0) {
                dao.insertIgnoreAll(InitialDummyVolunteers.getDummyVolunteers())
            }
        } catch (e: Exception) {
            Log.w(TAG, "Gagal memastikan seed awal relawan: ${e.message}")
        }
    }

    /**
     * Mengecek apakah NIK (16 digit) sudah terdaftar di SQLite atau data dummy (Anti Data Ganda).
     */
    suspend fun isNikAlreadyRegistered(nik: String): Boolean {
        val cleanedNik = nik.trim()
        if (cleanedNik.isEmpty()) return false
        ensureDummyDataSeeded()
        if (dao.existsByNik(cleanedNik)) return true
        return InitialDummyVolunteers.getDummyVolunteers().any {
            it.nik.equals(cleanedNik, ignoreCase = true)
        }
    }

    /**
     * Registrasi Relawan Baru dengan Validasi NIK (Anti Data Ganda - Primary Key 16 Digit).
     * Jika NIK sudah ada di database/dummy data, mengembalikan DuplicateNik("NIK sudah terdaftar, silakan hubungi admin.").
     */
    suspend fun registerVolunteerValidated(
        fullName: String,
        nik: String,
        phoneNumber: String,
        organization: String,
        selectedSkills: List<String>,
        homeDistrict: String,
        selfiePhotoUri: String?,
        certificatePhotoUri: String?
    ): VolunteerRegistrationResult {
        val cleanedNik = nik.trim()
        if (cleanedNik.length != 16 || !cleanedNik.all { it.isDigit() }) {
            return VolunteerRegistrationResult.ValidationError(
                "NIK wajib terdiri dari tepat 16 digit angka sesuai KTP."
            )
        }

        // Validasi Anti Data Ganda terhadap SQLite & Dummy Data
        if (isNikAlreadyRegistered(cleanedNik)) {
            return VolunteerRegistrationResult.DuplicateNik(
                VolunteerPrivacyHelper.DUPLICATE_NIK_ALERT_MESSAGE
            )
        }

        val now = System.currentTimeMillis()
        val shortCode = UUID.randomUUID().toString().replace("-", "").take(5).uppercase()
        val id = "REL-MMK-$shortCode"
        val onlineNow = checkNetworkOnline(appContext)
        _isOnline.value = onlineNow

        val entity = VolunteerEntity(
            nik = cleanedNik, // Primary Key
            id = id,
            fullName = fullName.trim(),
            phoneNumber = phoneNumber.trim(),
            organization = organization.trim().ifBlank { "Relawan Individu" },
            skillsCsv = selectedSkills.joinToString("|"),
            homeDistrict = homeDistrict.trim().ifBlank { "Kota Mukomuko" },
            assignedDistrict = "",
            assignedPoskoName = "",
            assignedShift = "",
            status = VolunteerStatus.PENDING.code,
            isActive = true,
            needsAssistance = false,
            assistanceNote = "",
            selfiePhotoUri = selfiePhotoUri,
            certificatePhotoUri = certificatePhotoUri,
            waktuCheckIn = null,
            waktuCheckOut = null,
            suratTugasNumber = "",
            suratTugasPdfPath = null,
            registeredAt = now,
            updatedAt = now,
            verifiedByOfficer = "",
            isSyncedToCloud = false
        )

        // 1. Insert ke SQLite dengan OnConflictStrategy.ABORT (menjamin Primary Key NIK tidak ganda)
        try {
            dao.insertNewVolunteer(entity)
        } catch (_: Exception) {
            return VolunteerRegistrationResult.DuplicateNik(
                VolunteerPrivacyHelper.DUPLICATE_NIK_ALERT_MESSAGE
            )
        }

        // 2. Sinkronisasi otomatis ke Cloud Firestore jika ada sinyal internet & bukan Mode Sandbox
        if (onlineNow && !SandboxTrainingManager.isSandboxModeActive()) {
            scope.launch {
                pushSingleVolunteerToFirestore(entity)
            }
            _lastSyncStatus.value = "Tersimpan di SQLite (PK: NIK) & disinkronkan ke Cloud Firestore"
        } else if (SandboxTrainingManager.isSandboxModeActive()) {
            _lastSyncStatus.value = "Tersimpan di Sandbox Lokal (Terisolasi dari Cloud Produksi)"
        } else {
            _lastSyncStatus.value = "Mode Offline: Tersimpan di SQLite • Menunggu sinyal untuk Auto-Sync"
        }

        return VolunteerRegistrationResult.Success(entity)
    }

    /**
     * Wrapper registrasi langsung yang melempar IllegalStateException jika NIK sudah terdaftar.
     */
    suspend fun registerVolunteer(
        fullName: String,
        nik: String,
        phoneNumber: String,
        organization: String,
        selectedSkills: List<String>,
        homeDistrict: String,
        selfiePhotoUri: String?,
        certificatePhotoUri: String?
    ): VolunteerEntity {
        return when (
            val result = registerVolunteerValidated(
                fullName = fullName,
                nik = nik,
                phoneNumber = phoneNumber,
                organization = organization,
                selectedSkills = selectedSkills,
                homeDistrict = homeDistrict,
                selfiePhotoUri = selfiePhotoUri,
                certificatePhotoUri = certificatePhotoUri
            )
        ) {
            is VolunteerRegistrationResult.Success -> result.volunteer
            is VolunteerRegistrationResult.DuplicateNik -> throw IllegalStateException(result.message)
            is VolunteerRegistrationResult.ValidationError -> throw IllegalArgumentException(result.message)
        }
    }

    /**
     * Ubah Status Relawan (Pending -> Verified -> Assigned -> Done / Rejected).
     */
    suspend fun verifyVolunteer(nikOrId: String, officerName: String) {
        val now = System.currentTimeMillis()
        dao.updateVerificationStatus(
            nik = nikOrId,
            newStatus = VolunteerStatus.VERIFIED.code,
            isActive = true,
            officerName = officerName.ifBlank { "Admin Pusdalops" },
            updatedAt = now
        )
        triggerSyncForKey(nikOrId)
    }

    suspend fun rejectVolunteer(nikOrId: String, officerName: String) {
        val now = System.currentTimeMillis()
        dao.updateVerificationStatus(
            nik = nikOrId,
            newStatus = VolunteerStatus.REJECTED.code,
            isActive = false,
            officerName = officerName.ifBlank { "Admin Pusdalops" },
            updatedAt = now
        )
        triggerSyncForKey(nikOrId)
    }

    /**
     * Penugasan Relawan ke Kecamatan / Posko & Shift Jam Bertugas (Verified -> Assigned)
     * sekaligus otomatis memicu pembuatan file PDF "SURAT TUGAS RELAWAN".
     */
    suspend fun assignVolunteerToPosko(
        volunteerId: String,
        district: String,
        poskoName: String,
        shift: String,
        officerName: String
    ): File? {
        val now = System.currentTimeMillis()
        val existing = dao.getVolunteerByIdOrNik(volunteerId)
        val targetNik = existing?.nik ?: volunteerId
        val letterNumber = VolunteerAssignmentPdfGenerator.buildLetterNumber(targetNik, now)

        val previewEntity = (existing ?: InitialDummyVolunteers.getDummyVolunteers().first()).copy(
            assignedDistrict = district,
            assignedPoskoName = poskoName,
            assignedShift = shift,
            status = VolunteerStatus.ASSIGNED.code,
            suratTugasNumber = letterNumber,
            verifiedByOfficer = officerName.ifBlank { "HITATUN ARAZAK" },
            updatedAt = now
        )

        val generatedPdfFile = try {
            VolunteerAssignmentPdfGenerator.generateAssignmentPdfFile(
                context = appContext,
                volunteer = previewEntity,
                assignedDistrict = district,
                assignedPoskoName = poskoName,
                assignedShift = shift,
                letterNumber = letterNumber,
                officerName = officerName.ifBlank { "HITATUN ARAZAK" }
            )
        } catch (e: Exception) {
            Log.w(TAG, "Gagal membuat PDF Surat Tugas: ${e.message}")
            null
        }

        dao.updateAssignmentWithPdf(
            nik = volunteerId,
            newStatus = VolunteerStatus.ASSIGNED.code,
            district = district,
            poskoName = poskoName,
            shift = shift,
            suratTugasNumber = letterNumber,
            suratTugasPdfPath = generatedPdfFile?.absolutePath,
            officerName = officerName.ifBlank { "Admin Pusdalops" },
            updatedAt = now
        )
        triggerSyncForKey(volunteerId)
        return generatedPdfFile
    }

    /**
     * Mencatat Check-in Kehadiran Relawan di Posko (`waktu_checkin` = timestamp server saat itu).
     */
    suspend fun checkInPosko(
        nikOrId: String,
        timestampMs: Long = System.currentTimeMillis()
    ): VolunteerEntity? {
        dao.updateCheckInPosko(
            nik = nikOrId,
            waktuCheckIn = timestampMs,
            updatedAt = timestampMs
        )
        triggerSyncForKey(nikOrId)
        return dao.getVolunteerByIdOrNik(nikOrId)
    }

    /**
     * Mencatat Check-out Kehadiran Relawan dari Posko (`waktu_checkout` = timestamp server saat itu).
     */
    suspend fun checkOutPosko(
        nikOrId: String,
        timestampMs: Long = System.currentTimeMillis()
    ): VolunteerEntity? {
        dao.updateCheckOutPosko(
            nik = nikOrId,
            waktuCheckOut = timestampMs,
            updatedAt = timestampMs
        )
        triggerSyncForKey(nikOrId)
        return dao.getVolunteerByIdOrNik(nikOrId)
    }

    /**
     * Memastikan file PDF Surat Tugas Digital tersedia dan mengembalikannya untuk diunduh/dibuka.
     */
    suspend fun ensureAndGetSuratTugasPdf(
        nikOrId: String,
        officerName: String = "HITATUN ARAZAK"
    ): File? {
        val entity = dao.getVolunteerByIdOrNik(nikOrId) ?: return null
        val letterNo = entity.suratTugasNumber.ifBlank {
            VolunteerAssignmentPdfGenerator.buildLetterNumber(entity.nik)
        }
        val pdfFile = VolunteerAssignmentPdfGenerator.generateAssignmentPdfFile(
            context = appContext,
            volunteer = entity,
            assignedDistrict = entity.effectivePoskoDistrict(),
            assignedPoskoName = entity.effectivePoskoName(),
            assignedShift = entity.assignedShift.ifBlank { "Shift Pagi (08:00 - 16:00 WIB)" },
            letterNumber = letterNo,
            officerName = officerName.ifBlank { entity.verifiedByOfficer.ifBlank { "HITATUN ARAZAK" } }
        )
        dao.updateSuratTugasPdfPath(
            nik = entity.nik,
            pdfPath = pdfFile.absolutePath,
            suratTugasNumber = letterNo
        )
        return pdfFile
    }

    /**
     * Menyelesaikan Penugasan Relawan (Assigned -> Done).
     */
    suspend fun markAssignmentDone(nikOrId: String, officerName: String) {
        val now = System.currentTimeMillis()
        dao.updateVerificationStatus(
            nik = nikOrId,
            newStatus = VolunteerStatus.DONE.code,
            isActive = true,
            officerName = officerName.ifBlank { "Admin Pusdalops" },
            updatedAt = now
        )
        dao.updateNeedsAssistance(
            nik = nikOrId,
            needsAssistance = false,
            note = "",
            updatedAt = now
        )
        triggerSyncForKey(nikOrId)
    }

    /**
     * Toggle Status Darurat "Butuh Bantuan" untuk Relawan yang sedang bertugas di lapangan.
     */
    suspend fun toggleNeedsAssistance(
        nikOrId: String,
        needsAssistance: Boolean,
        note: String = "Membutuhkan dukungan logistik/evakuasi tambahan di titik posko"
    ) {
        val now = System.currentTimeMillis()
        dao.updateNeedsAssistance(
            nik = nikOrId,
            needsAssistance = needsAssistance,
            note = if (needsAssistance) note else "",
            updatedAt = now
        )
        triggerSyncForKey(nikOrId)
    }

    private fun triggerSyncForKey(nikOrId: String) {
        val onlineNow = checkNetworkOnline(appContext)
        _isOnline.value = onlineNow
        if (!onlineNow || SandboxTrainingManager.isSandboxModeActive()) return
        scope.launch {
            val updated = dao.getVolunteerByIdOrNik(nikOrId) ?: return@launch
            pushSingleVolunteerToFirestore(updated)
        }
    }

    /**
     * Sinkronisasi Otomatis Seluruh Antrean SQLite Offline ke Cloud Firestore.
     */
    suspend fun syncPendingOfflineVolunteersToFirestore(): Int {
        val onlineNow = checkNetworkOnline(appContext)
        _isOnline.value = onlineNow
        if (!onlineNow) {
            _lastSyncStatus.value = "Tidak ada koneksi internet • Data aman di SQLite Lokal"
            return 0
        }
        if (SandboxTrainingManager.isSandboxModeActive()) {
            _lastSyncStatus.value = "Mode Latihan (Sandbox) Aktif • Sinkronisasi Cloud ditangguhkan"
            return 0
        }

        _isSyncing.value = true
        var syncedCount = 0
        try {
            val unsyncedList = dao.getUnsyncedVolunteers()
            for (volunteer in unsyncedList) {
                val ok = pushSingleVolunteerToFirestore(volunteer)
                if (ok) syncedCount++
            }
            _lastSyncStatus.value = if (syncedCount > 0) {
                "Auto-Sync Berhasil: $syncedCount data relawan lokal tersinkron ke Cloud Firestore"
            } else {
                "100% Sinkron (SQLite Lokal <-> Cloud Firestore)"
            }
        } catch (e: Exception) {
            Log.w(TAG, "Auto-sync relawan tertunda: ${e.message}")
        } finally {
            _isSyncing.value = false
        }
        return syncedCount
    }

    private suspend fun pushSingleVolunteerToFirestore(entity: VolunteerEntity): Boolean {
        if (SandboxTrainingManager.isSandboxModeActive()) return false
        return try {
            val firestore = FirebaseFirestore.getInstance()
            val payload = mapOf(
                "nik" to entity.nik,
                "id" to entity.id,
                "fullName" to entity.fullName,
                "phoneNumber" to entity.phoneNumber,
                "organization" to entity.organization,
                "skillsCsv" to entity.skillsCsv,
                "homeDistrict" to entity.homeDistrict,
                "assignedDistrict" to entity.assignedDistrict,
                "assignedPoskoName" to entity.assignedPoskoName,
                "assignedShift" to entity.assignedShift,
                "status" to entity.status,
                "isActive" to entity.isActive,
                "needsAssistance" to entity.needsAssistance,
                "assistanceNote" to entity.assistanceNote,
                "selfiePhotoUri" to entity.selfiePhotoUri,
                "certificatePhotoUri" to entity.certificatePhotoUri,
                "waktu_checkin" to entity.waktuCheckIn,
                "waktu_checkout" to entity.waktuCheckOut,
                "suratTugasNumber" to entity.suratTugasNumber,
                "suratTugasPdfPath" to entity.suratTugasPdfPath,
                "registeredAt" to entity.registeredAt,
                "updatedAt" to entity.updatedAt,
                "verifiedByOfficer" to entity.verifiedByOfficer,
                "isSyncedToCloud" to true
            )
            // Gunakan NIK sebagai Document Primary Key di Cloud Firestore
            firestore.collection(COLLECTION_VOLUNTEERS)
                .document(entity.nik)
                .set(payload, SetOptions.merge())
                .await()
            dao.markSyncedToCloud(entity.nik, System.currentTimeMillis())
            true
        } catch (e: Exception) {
            Log.w(TAG, "Penyimpanan Cloud Firestore tertunda untuk NIK ${entity.nik} (tetap aman di SQLite): ${e.message}")
            false
        }
    }

    private fun startRealtimeFirestoreListener() {
        if (firestoreListener != null || SandboxTrainingManager.isSandboxModeActive()) return
        try {
            val firestore = FirebaseFirestore.getInstance()
            firestoreListener = firestore.collection(COLLECTION_VOLUNTEERS)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    if (SandboxTrainingManager.isSandboxModeActive()) return@addSnapshotListener
                    scope.launch {
                        for (doc in snapshot.documents) {
                            try {
                                val nik = doc.getString("nik") ?: continue
                                val id = doc.getString("id") ?: doc.id
                                val fullName = doc.getString("fullName") ?: continue
                                val phoneNumber = doc.getString("phoneNumber") ?: ""
                                val organization = doc.getString("organization") ?: "Relawan Individu"
                                val skillsCsv = doc.getString("skillsCsv") ?: ""
                                val homeDistrict = doc.getString("homeDistrict") ?: "Kota Mukomuko"
                                val assignedDistrict = doc.getString("assignedDistrict") ?: ""
                                val assignedPoskoName = doc.getString("assignedPoskoName") ?: ""
                                val assignedShift = doc.getString("assignedShift") ?: ""
                                val status = doc.getString("status") ?: VolunteerStatus.PENDING.code
                                val isActive = doc.getBoolean("isActive") ?: true
                                val needsAssistance = doc.getBoolean("needsAssistance") ?: false
                                val assistanceNote = doc.getString("assistanceNote") ?: ""
                                val selfiePhotoUri = doc.getString("selfiePhotoUri")
                                val certificatePhotoUri = doc.getString("certificatePhotoUri")
                                val waktuCheckIn = doc.getLong("waktu_checkin") ?: doc.getLong("waktuCheckIn")
                                val waktuCheckOut = doc.getLong("waktu_checkout") ?: doc.getLong("waktuCheckOut")
                                val suratTugasNumber = doc.getString("suratTugasNumber") ?: ""
                                val suratTugasPdfPath = doc.getString("suratTugasPdfPath")
                                val registeredAt = doc.getLong("registeredAt") ?: System.currentTimeMillis()
                                val remoteUpdatedAt = doc.getLong("updatedAt") ?: registeredAt
                                val verifiedByOfficer = doc.getString("verifiedByOfficer") ?: ""

                                val existingLocal = dao.getVolunteerByNik(nik)
                                if (existingLocal == null ||
                                    (existingLocal.isSyncedToCloud && remoteUpdatedAt >= existingLocal.updatedAt)
                                ) {
                                    dao.upsertVolunteer(
                                        VolunteerEntity(
                                            nik = nik,
                                            id = id,
                                            fullName = fullName,
                                            phoneNumber = phoneNumber,
                                            organization = organization,
                                            skillsCsv = skillsCsv,
                                            homeDistrict = homeDistrict,
                                            assignedDistrict = assignedDistrict,
                                            assignedPoskoName = assignedPoskoName,
                                            assignedShift = assignedShift,
                                            status = status,
                                            isActive = isActive,
                                            needsAssistance = needsAssistance,
                                            assistanceNote = assistanceNote,
                                            selfiePhotoUri = selfiePhotoUri,
                                            certificatePhotoUri = certificatePhotoUri,
                                            waktuCheckIn = waktuCheckIn,
                                            waktuCheckOut = waktuCheckOut,
                                            suratTugasNumber = suratTugasNumber,
                                            suratTugasPdfPath = existingLocal?.suratTugasPdfPath ?: suratTugasPdfPath,
                                            registeredAt = registeredAt,
                                            updatedAt = remoteUpdatedAt,
                                            verifiedByOfficer = verifiedByOfficer,
                                            isSyncedToCloud = true
                                        )
                                    )
                                }
                            } catch (_: Exception) {
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Realtime listener Firestore bpbd_volunteers dilewati: ${e.message}")
        }
    }

    private fun registerAutoSyncNetworkCallback() {
        if (networkCallbackRegistered) return
        try {
            val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
            cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    _isOnline.value = true
                    scope.launch {
                        syncPendingOfflineVolunteersToFirestore()
                        startRealtimeFirestoreListener()
                    }
                }

                override fun onLost(network: Network) {
                    _isOnline.value = checkNetworkOnline(appContext)
                    if (!_isOnline.value) {
                        _lastSyncStatus.value = "Sinyal Terputus • Mode Offline SQLite Aktif"
                    }
                }
            })
            networkCallbackRegistered = true
        } catch (e: Exception) {
            Log.w(TAG, "NetworkCallback tidak dapat didaftarkan: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "VolunteerRepository"
        const val COLLECTION_VOLUNTEERS = "bpbd_volunteers"

        @Volatile
        private var INSTANCE: VolunteerRepository? = null

        fun getInstance(context: Context): VolunteerRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: VolunteerRepository(
                    context.applicationContext,
                    VolunteerDatabase.getInstance(context.applicationContext).volunteerDao()
                ).also { INSTANCE = it }
            }
        }

        fun checkNetworkOnline(context: Context): Boolean {
            return try {
                val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                    ?: return false
                val activeNetwork = cm.activeNetwork ?: return false
                val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            } catch (_: Exception) {
                false
            }
        }
    }
}
