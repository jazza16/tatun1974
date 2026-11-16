package com.example.data.repository

import android.content.Context
import android.os.Build
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.FirestoreHelper
import com.example.data.local.OfficerAuthManager
import com.example.data.local.OfficerCredentialEntity
import com.example.data.local.OfficerProfile
import com.example.data.local.OfficerRoles
import com.example.data.local.OfficialAsnBpbdPersonnel
import com.example.data.local.Personnel
import com.example.data.local.PersonnelAuditLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Smart Authentication Repository for Pusdalops BPBD Kabupaten Mukomuko.
 *
 * Implements a smart, zero-default-PIN security scheme that:
 * 1. Automatically inventories and records all official personnel (past and present, synced or un-synced).
 * 2. Persists unique individual login credentials for every officer in both local Room DB and Cloud Firestore.
 * 3. Enforces multi-device synchronization: logging in on a new device requires the previously configured
 *    personal PIN/credentials, entirely eliminating default PIN vulnerabilities (such as "117" or "TRC2026").
 */
class SmartOfficerAuthRepository(private val context: Context) {

    private val database = AppDatabase.getInstance(context)
    private val credentialDao = database.credentialDao()
    private val personnelDao = database.personnelDao()
    private val auditDao = database.auditLogDao()
    private val officerAuthManager = OfficerAuthManager(context)
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    @Volatile
    var activeCredential: OfficerCredentialEntity? = null
        private set

    companion object {
        private const val TAG = "SmartOfficerAuthRepo"
        private const val MAX_FAILED_ATTEMPTS = 5
        private const val LOCKOUT_DURATION_MS = 5 * 60 * 1000L

        private data class FailedAttemptState(val count: Int, val lockedUntilMs: Long)
        private val failedAttemptMap = java.util.concurrent.ConcurrentHashMap<String, FailedAttemptState>()

        // Default PINs that are strictly forbidden for officer login under the smart login scheme
        val FORBIDDEN_DEFAULT_PINS = listOf("117", "BPBD117", "ADMIN117", "TRC2026", "PUSDALOPS117", "0000", "1234")

        private fun maskNip(nip: String): String {
            val t = nip.trim()
            return if (t.length <= 4) "***" else "***${t.takeLast(4)}"
        }

        @Volatile
        private var INSTANCE: SmartOfficerAuthRepository? = null

        fun getInstance(context: Context): SmartOfficerAuthRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SmartOfficerAuthRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    /**
     * Observe live list of all officer credentials from Room DB.
     */
    fun observeAllCredentials(): Flow<List<OfficerCredentialEntity>> {
        return credentialDao.getAllCredentials()
    }

    /**
     * Ensures all official BPBD Mukomuko personnel exist in local Room SQLite database
     * with their deterministic 6-digit NIP initial PIN, even when offline.
     */
    private suspend fun ensureLocalOfficersSeeded(): List<OfficerCredentialEntity> = withContext(Dispatchers.IO) {
        val existingCreds = credentialDao.getAllCredentialsList().associateBy { it.nip.lowercase().trim() }
        val newRegistrations = mutableListOf<OfficerCredentialEntity>()

        // Source 1: Official ASN BPBD Personnel (Leadership & Operators)
        for (asn in OfficialAsnBpbdPersonnel.ALL_OFFICIAL_PERSONNEL) {
            val key = asn.nip.lowercase().trim()
            if (!existingCreds.containsKey(key)) {
                val cred = OfficerCredentialEntity.createDefault(
                    nip = asn.nip,
                    fullName = asn.name,
                    role = asn.role,
                    unit = asn.unit,
                    shift = asn.shift,
                    phone = asn.phone
                )
                newRegistrations.add(cred)
            }
        }

        // Source 2: Local Personnel Table in Room
        val dbPersonnel = personnelDao.getAllPersonnelList()
        for (p in dbPersonnel) {
            val key = p.nip.lowercase().trim()
            if (!existingCreds.containsKey(key) && newRegistrations.none { it.nip.lowercase().trim() == key }) {
                val cred = OfficerCredentialEntity.createDefault(
                    nip = p.nip,
                    fullName = p.name,
                    role = p.role,
                    unit = p.unit,
                    shift = p.shift,
                    phone = p.phone
                )
                newRegistrations.add(cred)
            }
        }

        // Source 3: OfficerAuthManager Roster
        val roster = officerAuthManager.getOfficerRoster()
        for (r in roster) {
            val key = r.nip.lowercase().trim()
            if (!existingCreds.containsKey(key) && newRegistrations.none { it.nip.lowercase().trim() == key }) {
                val cred = OfficerCredentialEntity.createDefault(
                    nip = r.nip,
                    fullName = r.fullName,
                    role = r.role,
                    unit = r.unit,
                    shift = r.shift,
                    phone = r.phone
                )
                newRegistrations.add(cred)
            }
        }

        if (newRegistrations.isNotEmpty()) {
            credentialDao.insertAll(newRegistrations)
            Log.d(TAG, "Harvested and registered ${newRegistrations.size} new officers into local credentials database.")
        }
        return@withContext newRegistrations
    }

    /**
     * Smart Auto-Discovery & Synchronization:
     * 1. FIRST pulls all existing credentials from Cloud Firestore so a newly installed app on a new phone
     *    receives the latest custom PINs (isPinChanged / isCustomPinSet) and NEVER overwrites Cloud Firestore
     *    with default credentials.
     * 2. Only creates default credentials for officers who do not yet exist in Cloud Firestore or local SQLite.
     */
    suspend fun harvestAndSyncAllOfficers(): Int = withContext(Dispatchers.IO) {
        // STEP 1: Fetch latest credentials from Cloud Firestore FIRST
        val cloudFetchResult = FirestoreHelper.fetchAllCredentialsFromCloudSuspend()
        val cloudNipSet = mutableSetOf<String>()

        if (cloudFetchResult.isSuccess) {
            val cloudCreds = cloudFetchResult.getOrNull().orEmpty()
            val localMap = credentialDao.getAllCredentialsList().associateBy { it.nip.lowercase().trim() }
            for (cloud in cloudCreds) {
                val key = cloud.nip.lowercase().trim()
                cloudNipSet.add(key)
                val local = localMap[key]
                if (local == null || (cloud.isCustomPinSet && !local.isCustomPinSet) || cloud.updatedAt >= local.updatedAt) {
                    credentialDao.insertCredential(cloud.copy(isSyncedWithCloud = true))
                }
            }
        }

        val newRegistrations = ensureLocalOfficersSeeded()

        if (newRegistrations.isNotEmpty() && cloudFetchResult.isSuccess) {
            // CRITICAL: Only push newly registered default credentials to Cloud Firestore if the cloud fetch
            // succeeded AND confirmed that the officer does not already exist in Cloud Firestore.
            for (cred in newRegistrations) {
                val key = cred.nip.lowercase().trim()
                if (!cloudNipSet.contains(key)) {
                    FirestoreHelper.syncCredentialToCloud(cred)
                }
            }
        }

        return@withContext newRegistrations.size
    }

    /**
     * Pulls credentials from Cloud Firestore and updates local Room DB.
     * Crucial for a newly installed app on a new device to recognize previously registered officers.
     */
    fun pullCredentialsFromCloud(onComplete: ((Boolean, Int) -> Unit)? = null) {
        FirestoreHelper.fetchAllCredentialsFromCloud { cloudCreds ->
            if (cloudCreds.isNotEmpty()) {
                coroutineScope.launch {
                    val localMap = credentialDao.getAllCredentialsList().associateBy { it.nip.lowercase().trim() }
                    var updatedCount = 0

                    for (cloud in cloudCreds) {
                        val key = cloud.nip.lowercase().trim()
                        val local = localMap[key]

                        // Always adopt cloud version if not in local, or if cloud has custom PIN set, or is newer/equal
                        if (local == null || (cloud.isCustomPinSet && !local.isCustomPinSet) || cloud.updatedAt >= local.updatedAt) {
                            credentialDao.insertCredential(cloud.copy(isSyncedWithCloud = true))
                            updatedCount++
                        }
                    }
                    Log.d(TAG, "Pulled $updatedCount credentials from Cloud Firestore into local database.")
                    withContext(Dispatchers.Main) {
                        onComplete?.invoke(true, updatedCount)
                    }
                }
            } else {
                onComplete?.invoke(false, 0)
            }
        }
    }

    /**
     * Smart Officer Authentication:
     * 1. Rejects any generic default PIN (117, BPBD117, TRC2026) unconditionally.
     * 2. Always fetches the latest credential from Cloud Firestore (Source.SERVER) first when
     *    'Verifikasi & Masuk' is pressed, updating local SQLite if found.
     * 3. Falls back cleanly to local SQLite official credential database if Cloud Firestore is unreachable,
     *    still strictly enforcing the 6-digit NIP initial PIN rule and custom PIN lock.
     */
    suspend fun authenticateOfficer(
        nip: String,
        inputPin: String,
        deviceModel: String = "${Build.MANUFACTURER} ${Build.MODEL}"
    ): Result<OfficerProfile> = withContext(Dispatchers.IO) {
        val trimmedNip = nip.trim()
        val trimmedPin = inputPin.trim()

        if (trimmedNip.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("NIP / Nomor Registrasi tidak boleh kosong."))
        }
        val lockoutKey = trimmedNip.lowercase()
        val nowCheck = System.currentTimeMillis()
        val existingLockState = failedAttemptMap[lockoutKey]
        if (existingLockState != null && existingLockState.lockedUntilMs > nowCheck) {
            val remSec = (existingLockState.lockedUntilMs - nowCheck + 999L) / 1000L
            return@withContext Result.failure(
                SecurityException("Akun terkunci sementara selama $remSec detik akibat terlalu banyak percobaan PIN salah.")
            )
        }
        if (trimmedPin.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("PIN pribadi tidak boleh kosong."))
        }

        // STRICT CHECK: Reject generic default PINs ('117', 'BPBD117', 'TRC2026', etc.) without exception
        if (FORBIDDEN_DEFAULT_PINS.any { it.equals(trimmedPin, ignoreCase = true) }) {
            auditDao.insertAuditLog(
                PersonnelAuditLogEntity(
                    nip = trimmedNip,
                    name = "Percobaan Akses PIN Default",
                    role = "Tidak Sah",
                    shift = "-",
                    action = "AUTH_BLOCKED_DEFAULT_PIN_REJECTED",
                    deviceInfo = "PIN default '$trimmedPin' ditolak pada perangkat $deviceModel"
                )
            )
            return@withContext Result.failure(
                SecurityException(
                    "PIN default '$trimmedPin' dinonaktifkan oleh sistem keamanan. " +
                            "Wajib login menggunakan PIN pribadi yang telah tersimpan di Cloud Firestore."
                )
            )
        }

        // 1. Panggil fetchCredentialByNipFromCloud(nip) terlebih dahulu saat 'Verifikasi & Masuk' ditekan
        val cloudFetchResult = FirestoreHelper.fetchCredentialByNipFromCloud(trimmedNip)
        var credential: OfficerCredentialEntity? = null

        if (cloudFetchResult.isSuccess) {
            // 2. Jika berhasil ditarik dari server, simpan/update ke SQLite lokal (Room)
            val cloudCred = cloudFetchResult.getOrNull()
            if (cloudCred != null) {
                val syncedCred = cloudCred.copy(isSyncedWithCloud = true)
                credentialDao.insertCredential(syncedCred)
                credential = syncedCred
            } else {
                var localCred = credentialDao.getCredentialByNip(trimmedNip)
                if (localCred == null) {
                    ensureLocalOfficersSeeded()
                    localCred = credentialDao.getCredentialByNip(trimmedNip)
                }
                if (localCred != null) {
                    FirestoreHelper.syncCredentialToCloud(localCred)
                    credential = localCred.copy(isSyncedWithCloud = true)
                    credentialDao.insertCredential(credential)
                }
            }
        } else {
            // 3. Jika sinkronisasi Cloud Firestore gagal (offline / error server):
            // Pastikan data personil resmi lokal sudah ter-seed di SQLite (Room), lalu validasi terhadap SQLite lokal
            ensureLocalOfficersSeeded()
            val localCred = credentialDao.getCredentialByNip(trimmedNip)

            if (localCred == null) {
                val originalErrorMsg = cloudFetchResult.exceptionOrNull()?.message
                    ?: "Gagal sinkronisasi, periksa koneksi internet"
                auditDao.insertAuditLog(
                    PersonnelAuditLogEntity(
                        nip = trimmedNip,
                        name = "Petugas",
                        role = "-",
                        shift = "-",
                        action = "AUTH_BLOCKED_CLOUD_SYNC_FAILED",
                        deviceInfo = "$originalErrorMsg pada $deviceModel"
                    )
                )
                return@withContext Result.failure(
                    IllegalStateException(originalErrorMsg, cloudFetchResult.exceptionOrNull())
                )
            }

            Log.w(TAG, "Cloud sync unavailable (${cloudFetchResult.exceptionOrNull()?.message}); validating NIP ${maskNip(trimmedNip)} against local SQLite.")
            credential = localCred
        }

        if (credential == null) {
            return@withContext Result.failure(
                IllegalArgumentException("Petugas dengan NIP '$trimmedNip' tidak ditemukan dalam database resmi.")
            )
        }

        val sixDigitNip = OfficerCredentialEntity.generateInitialPinForNip(credential.nip)
        val rawLastSixNip = credential.nip.trim().takeLast(6)
        val isInputSixDigitNip = trimmedPin.equals(sixDigitNip, ignoreCase = true) ||
            (rawLastSixNip.length == 6 && trimmedPin.equals(rawLastSixNip, ignoreCase = true)) ||
            (credential.initialSecurePin.isNotBlank() && trimmedPin.equals(credential.initialSecurePin, ignoreCase = true))

        // Jika Petugas sudah mengganti PIN (isCustomPinSet == true), tolak penggunaan 6 digit NIP
        if ((credential.isPinChanged || credential.isCustomPinSet) && isInputSixDigitNip) {
            auditDao.insertAuditLog(
                PersonnelAuditLogEntity(
                    nip = trimmedNip,
                    name = credential.fullName,
                    role = credential.role,
                    shift = credential.shift,
                    action = "AUTH_BLOCKED_INITIAL_NIP_AFTER_PIN_CHANGED",
                    deviceInfo = "Percobaan login memakai 6 digit NIP setelah PIN diubah pada $deviceModel"
                )
            )
            return@withContext Result.failure(
                SecurityException("PIN awal (6 digit terakhir NIP) sudah tidak berlaku karena Anda telah mengganti PIN. Wajib gunakan PIN pribadi terbaru Anda.")
            )
        }

        // STEP 3: Validate PIN against Cloud Firestore status (isPinChanged / isCustomPinSet & pinHash)
        val isPinCorrect = credential.verifyPin(trimmedPin)

        if (!isPinCorrect) {
            val currentFailures = (failedAttemptMap[lockoutKey]?.takeIf { it.lockedUntilMs <= System.currentTimeMillis() }?.count ?: 0) + 1
            val lockUntil = if (currentFailures >= MAX_FAILED_ATTEMPTS) System.currentTimeMillis() + LOCKOUT_DURATION_MS else 0L
            failedAttemptMap[lockoutKey] = FailedAttemptState(
                count = if (currentFailures >= MAX_FAILED_ATTEMPTS) 0 else currentFailures,
                lockedUntilMs = lockUntil
            )
            auditDao.insertAuditLog(
                PersonnelAuditLogEntity(
                    nip = trimmedNip,
                    name = credential.fullName,
                    role = credential.role,
                    shift = credential.shift,
                    action = if (lockUntil > 0L) "AUTH_LOCKED_OUT_BRUTE_FORCE" else "AUTH_INVALID_PIN",
                    deviceInfo = "Percobaan PIN salah ($currentFailures/$MAX_FAILED_ATTEMPTS) pada $deviceModel"
                )
            )
            val hintMessage = if (lockUntil > 0L) {
                "Terlalu banyak percobaan gagal. Akun dikunci sementara selama 5 menit demi keamanan."
            } else if (!credential.isCustomPinSet) {
                "PIN salah. Untuk aktivasi awal akun yang belum diganti PIN-nya, masukkan 6 digit terakhir NIP Anda. (Sisa percobaan: ${MAX_FAILED_ATTEMPTS - currentFailures})"
            } else {
                "PIN pribadi salah untuk NIP ${maskNip(trimmedNip)}. Wajib gunakan PIN pribadi terbaru Anda. (Sisa percobaan: ${MAX_FAILED_ATTEMPTS - currentFailures})"
            }
            return@withContext Result.failure(IllegalArgumentException(hintMessage))
        }

        // Authentication Successful: Reset failure tracker and record device & timestamp
        failedAttemptMap.remove(lockoutKey)
        val now = System.currentTimeMillis()
        val deviceDesc = "$deviceModel (Android ${Build.VERSION.RELEASE})"
        credentialDao.updateLoginSession(trimmedNip, now, deviceDesc)

        // Synchronize updated login metadata to Cloud Firestore
        val updatedCred = credential.copy(
            lastLoginAt = now,
            lastLoginDevice = deviceDesc,
            isSyncedWithCloud = true,
            updatedAt = now
        )
        FirestoreHelper.syncCredentialToCloud(updatedCred)
        activeCredential = updatedCred
        if (credential.isCustomPinSet) {
            officerAuthManager.setCustomOfficerPin(trimmedPin)
        }

        // Update local session in OfficerAuthManager
        officerAuthManager.loginOfficerDirectly(
            name = credential.fullName,
            badgeId = credential.nip,
            shift = credential.shift,
            role = credential.role
        )

        // Record successful login audit
        auditDao.insertAuditLog(
            PersonnelAuditLogEntity(
                nip = trimmedNip,
                name = credential.fullName,
                role = credential.role,
                shift = credential.shift,
                action = "LOGIN_SUCCESSFUL_SMART_AUTH",
                deviceInfo = "Berhasil masuk di $deviceDesc (PIN Pribadi Terverifikasi)"
            )
        )

        val profile = OfficerProfile(
            id = "cred-${credential.nip}",
            fullName = credential.fullName,
            nip = credential.nip,
            role = credential.role,
            unit = credential.unit,
            shift = credential.shift,
            phone = credential.phone
        )

        return@withContext Result.success(profile)
    }

    /**
     * Biometric Officer Authentication (Fingerprint / Face Unlock):
     * - TIDAK memanggil verifyPin() karena autentikasi telah divalidasi oleh sensor biometrik perangkat.
     * - Mengecek keberadaan NIP di Cloud Firestore terlebih dahulu (Cloud-First) atau SQLite lokal.
     * - Jika NIP ditemukan, mengatur sesi aktif (activeCredential), memperbarui lastLoginAt dan lastLoginDevice
     *   di database lokal, lalu menyinkronkan ke Cloud Firestore.
     * - Jika NIP tidak ditemukan di database, mengembalikan Result.failure("Petugas tidak ditemukan").
     */
    suspend fun authenticateWithBiometric(nip: String): Result<OfficerCredentialEntity> = withContext(Dispatchers.IO) {
        val trimmedNip = nip.trim()
        if (trimmedNip.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Petugas tidak ditemukan"))
        }

        val cloudFetchResult = FirestoreHelper.fetchCredentialByNipFromCloud(trimmedNip)
        var credential: OfficerCredentialEntity? = null

        if (cloudFetchResult.isSuccess) {
            val cloudCred = cloudFetchResult.getOrNull()
            if (cloudCred != null) {
                val syncedCred = cloudCred.copy(isSyncedWithCloud = true)
                credentialDao.insertCredential(syncedCred)
                credential = syncedCred
            } else {
                ensureLocalOfficersSeeded()
                credential = credentialDao.getCredentialByNip(trimmedNip)
            }
        } else {
            ensureLocalOfficersSeeded()
            val localCred = credentialDao.getCredentialByNip(trimmedNip)
            if (localCred == null) {
                val originalErrorMsg = cloudFetchResult.exceptionOrNull()?.message
                    ?: "Petugas tidak ditemukan"
                return@withContext Result.failure(
                    IllegalStateException(originalErrorMsg, cloudFetchResult.exceptionOrNull())
                )
            }
            credential = localCred
        }

        val activeCred = credential
            ?: return@withContext Result.failure(
                IllegalArgumentException("Petugas tidak ditemukan")
            )

        val now = System.currentTimeMillis()
        val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
        val deviceDesc = "$deviceModel (Android ${Build.VERSION.RELEASE} • Biometrik)"
        credentialDao.updateLoginSession(trimmedNip, now, deviceDesc)

        val updatedCred = activeCred.copy(
            lastLoginAt = now,
            lastLoginDevice = deviceDesc,
            isSyncedWithCloud = true,
            updatedAt = now
        )
        credentialDao.insertCredential(updatedCred)
        activeCredential = updatedCred
        FirestoreHelper.syncCredentialToCloud(updatedCred)

        officerAuthManager.loginOfficerDirectly(
            name = activeCred.fullName,
            badgeId = activeCred.nip,
            shift = activeCred.shift,
            role = activeCred.role
        )

        auditDao.insertAuditLog(
            PersonnelAuditLogEntity(
                nip = trimmedNip,
                name = activeCred.fullName,
                role = activeCred.role,
                shift = activeCred.shift,
                action = "LOGIN_SUCCESSFUL_BIOMETRIC_AUTH",
                deviceInfo = "Login Biometrik berhasil di $deviceDesc"
            )
        )

        return@withContext Result.success(updatedCred)
    }

    /**
     * Changes personal PIN for a specific officer:
     * - Fetches latest credential from Cloud Firestore first.
     * - Enforces non-default, non-trivial PIN (at least 4 digits, not in forbidden default list).
     * - Hashes with unique salt, updates local SQLite Room DB and syncs with Cloud Firestore (`isPinChanged = true`, `isCustomPinSet = true`).
     */
    suspend fun changePersonalPin(
        nip: String,
        currentPin: String,
        newPin: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val trimmedNip = nip.trim()
        val trimmedOld = currentPin.trim()
        val trimmedNew = newPin.trim()

        // Pull latest credential from Cloud Firestore first if online
        val cloudFetch = FirestoreHelper.fetchCredentialByNipFromCloud(trimmedNip)
        val cloudCred = cloudFetch.getOrNull()
        if (cloudCred != null) {
            credentialDao.insertCredential(cloudCred.copy(isSyncedWithCloud = true))
        }

        var credential = cloudCred ?: credentialDao.getCredentialByNip(trimmedNip)
        if (credential == null) {
            ensureLocalOfficersSeeded()
            credential = credentialDao.getCredentialByNip(trimmedNip)
        }
        val activeCred = credential
            ?: return@withContext Result.failure(IllegalArgumentException("Petugas dengan NIP '$trimmedNip' tidak ditemukan."))

        // Verify currentPin against activeCred strictly (legacy '117' never accepted)
        val isOldPinValid = if (activeCred.isCustomPinSet) {
            OfficerCredentialEntity.verifyHash(trimmedOld, activeCred.salt, activeCred.pinHash)
        } else {
            activeCred.verifyPin(trimmedOld)
        }

        if (!isOldPinValid) {
            return@withContext Result.failure(IllegalArgumentException("PIN saat ini salah."))
        }

        if (trimmedNew.length < 4) {
            return@withContext Result.failure(IllegalArgumentException("PIN baru minimal terdiri dari 4 digit/karakter."))
        }

        if (FORBIDDEN_DEFAULT_PINS.any { it.equals(trimmedNew, ignoreCase = true) }) {
            return@withContext Result.failure(
                SecurityException("PIN baru tidak boleh menggunakan PIN default umum (117 / TRC2026 / dsb).")
            )
        }

        val sixDigitNip = OfficerCredentialEntity.generateInitialPinForNip(activeCred.nip)
        if (trimmedNew.equals(sixDigitNip, ignoreCase = true) ||
            trimmedNew.equals(activeCred.nip.trim().takeLast(6), ignoreCase = true)
        ) {
            return@withContext Result.failure(
                IllegalArgumentException("PIN pribadi baru tidak boleh menggunakan 6 digit terakhir NIP (PIN awal).")
            )
        }

        if (trimmedNew == trimmedOld) {
            return@withContext Result.failure(IllegalArgumentException("PIN baru tidak boleh sama dengan PIN saat ini."))
        }

        val newSalt = OfficerCredentialEntity.generateCryptographicSalt()
        val newHash = OfficerCredentialEntity.hashWithSalt(trimmedNew, newSalt)
        val now = System.currentTimeMillis()

        val cloudSyncResult = FirestoreHelper.syncCredentialToCloudSuspend(
            activeCred.copy(
                pinHash = newHash,
                salt = newSalt,
                isCustomPinSet = true,
                initialSecurePin = "",
                isSyncedWithCloud = true,
                updatedAt = now
            )
        )

        val updatedCred = activeCred.copy(
            pinHash = newHash,
            salt = newSalt,
            isCustomPinSet = true,
            initialSecurePin = "",
            isSyncedWithCloud = cloudSyncResult.isSuccess,
            updatedAt = now
        )

        // Update local SQLite Room DB & SharedPreferences
        credentialDao.insertCredential(updatedCred)
        activeCredential = updatedCred
        officerAuthManager.setCustomOfficerPin(trimmedNew)

        if (cloudSyncResult.isFailure) {
            FirestoreHelper.syncCredentialToCloud(updatedCred)
        }

        auditDao.insertAuditLog(
            PersonnelAuditLogEntity(
                nip = trimmedNip,
                name = activeCred.fullName,
                role = activeCred.role,
                shift = activeCred.shift,
                action = "PIN_CHANGED_PERMANENT",
                deviceInfo = "PIN pribadi diperbarui dan disinkronkan ke Cloud Firestore"
            )
        )

        return@withContext Result.success("PIN pribadi untuk ${activeCred.fullName} berhasil diperbarui dan disinkronkan ke Cloud Firestore!")
    }

    /**
     * Resets officer PIN to their deterministic individual 6-digit NIP key.
     * Guarantees that generic default PINs ("117", "TRC2026", etc.) are NEVER restored.
     */
    suspend fun resetToInitialNipSecret(nip: String): Result<String> = withContext(Dispatchers.IO) {
        val trimmedNip = nip.trim()
        ensureLocalOfficersSeeded()
        val cred = credentialDao.getCredentialByNip(trimmedNip)
            ?: return@withContext Result.failure(IllegalArgumentException("Petugas dengan NIP '$trimmedNip' tidak ditemukan."))

        val initialPin = OfficerCredentialEntity.generateInitialPinForNip(cred.nip)
        val salt = OfficerCredentialEntity.generateCryptographicSalt()
        val hash = OfficerCredentialEntity.hashWithSalt(initialPin, salt)
        val now = System.currentTimeMillis()

        val syncCandidate = cred.copy(
            pinHash = hash,
            salt = salt,
            isCustomPinSet = false,
            initialSecurePin = initialPin,
            isSyncedWithCloud = true,
            updatedAt = now
        )

        val syncResult = FirestoreHelper.syncCredentialToCloudSuspend(syncCandidate)
        val updated = syncCandidate.copy(isSyncedWithCloud = syncResult.isSuccess)

        credentialDao.insertCredential(updated)
        if (syncResult.isFailure) {
            FirestoreHelper.syncCredentialToCloud(updated)
        }

        auditDao.insertAuditLog(
            PersonnelAuditLogEntity(
                nip = trimmedNip,
                name = cred.fullName,
                role = cred.role,
                shift = cred.shift,
                action = "PIN_RESET_TO_NIP_KEY",
                deviceInfo = "PIN di-reset ke kunci individual NIP di Cloud Firestore (Bukan PIN Default umum)"
            )
        )

        return@withContext Result.success("PIN akun ${cred.fullName} berhasil di-reset ke kunci individual 6 digit terakhir NIP.")
    }

    /**
     * Checks if an officer already has a personalized custom PIN set in the database.
     */
    suspend fun isCustomPinConfigured(nip: String): Boolean = withContext(Dispatchers.IO) {
        val cred = getCredential(nip)
        return@withContext cred?.isCustomPinSet == true
    }

    /**
     * Gets credential details for a specific officer, fetching the latest version from Cloud Firestore first.
     */
    suspend fun getCredential(nip: String): OfficerCredentialEntity? = withContext(Dispatchers.IO) {
        val trimmedNip = nip.trim()
        val cloudResult = FirestoreHelper.fetchCredentialByNipFromCloud(trimmedNip)
        if (cloudResult.isSuccess) {
            val cloudCred = cloudResult.getOrNull()
            if (cloudCred != null) {
                val synced = cloudCred.copy(isSyncedWithCloud = true)
                credentialDao.insertCredential(synced)
                return@withContext synced
            }
        }
        return@withContext credentialDao.getCredentialByNip(trimmedNip)
    }
}
