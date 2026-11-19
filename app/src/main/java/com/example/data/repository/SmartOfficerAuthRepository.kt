package com.example.data.repository

import android.content.Context
import android.os.Build
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.CloudCredentialLookupResult
import com.example.data.local.FirestoreHelper
import com.example.data.local.OfficerAuthManager
import com.example.data.local.OfficerCredentialEntity
import com.example.data.local.OfficerProfile
import com.example.data.local.OfficialAsnBpbdPersonnel
import com.example.data.local.PersonnelAuditLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Smart Authentication Repository for Pusdalops BPBD Kabupaten Mukomuko.
 *
 * Security & Persistence Invariants (Anti-Default-PIN-Reset Architecture):
 * 1. ZERO HARDCODED / DEFAULT PIN: Never seeds or accepts '117', 'BPBD117', 'TRC2026', '0000', '1234',
 *    or local auto-generated default PINs when local storage is empty (fresh install or app update).
 * 2. MANDATORY CLOUD VERIFICATION: Every PIN login (both routine login with `saved_nip` and fresh install
 *    with NIP + PIN) MUST verify strictly against Firebase Cloud Firestore (`officer_credentials/{nip}`)
 *    using `Source.SERVER`. If Firebase is unreachable or the NIP does not exist on the server, login fails
 *    cleanly without ever falling back to any local default PIN.
 * 3. SAVE ONLY NIP LOCALLY AFTER CLOUD SUCCESS: Only after Firebase confirms the PIN is valid does the app
 *    store `saved_nip` (and non-secret officer display profile) in encrypted SharedPreferences. The raw PIN
 *    (and its local hash/salt) is NEVER stored on the phone!
 * 4. UPDATE-SAFE SESSION STORAGE: App updates preserve `saved_nip` and officer session without wiping
 *    `SharedPreferences` or resetting credentials to defaults.
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

        // Default PINs that are strictly forbidden for officer login under all circumstances
        val FORBIDDEN_DEFAULT_PINS = listOf("117", "BPBD117", "ADMIN117", "TRC2026", "PUSDALOPS117", "0000", "1234")

        private fun maskNip(nip: String): String {
            val t = nip.trim()
            return if (t.length <= 4) "***" else "***${t.takeLast(4)}"
        }

        fun clearFailedAttemptsForTesting() {
            failedAttemptMap.clear()
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
     * Observe live list of non-secret synced officer metadata from Room DB.
     */
    fun observeAllCredentials(): Flow<List<OfficerCredentialEntity>> {
        return credentialDao.getAllCredentials()
    }

    /**
     * Strips secret PIN fields before caching non-secret profile metadata in Room so that
     * NO PIN or PIN hash is ever persisted on the device.
     */
    private fun sanitizeForLocalMetadataCache(credential: OfficerCredentialEntity): OfficerCredentialEntity {
        return credential.copy(
            pinHash = "",
            salt = "",
            initialSecurePin = "",
            isSyncedWithCloud = true
        )
    }

    /**
     * Verifies a candidate PIN strictly against the Cloud Firestore record (`pinHash` + `salt`,
     * or optional server-side `pin` field if configured by Admin in Firebase Console).
     * Never accepts forbidden default PINs ('117', etc.) or unverified local defaults.
     */
    private fun verifyAgainstCloudRecord(
        inputPin: String,
        cloudRecord: CloudCredentialLookupResult.Found
    ): Boolean {
        val trimmedPin = inputPin.trim()
        if (trimmedPin.isBlank()) return false
        if (FORBIDDEN_DEFAULT_PINS.any { it.equals(trimmedPin, ignoreCase = true) }) {
            return false
        }

        val cred = cloudRecord.credential
        // 1. Primary path: salted cryptographic hash stored in Cloud Firestore
        if (cred.pinHash.isNotBlank() && cred.salt.isNotBlank()) {
            return cred.verifyPin(trimmedPin)
        }

        // 2. Secondary path: server-managed plaintext `pin` field in Firestore Console (never mutated)
        val serverPlainPin = cloudRecord.serverPlainPin?.trim().orEmpty()
        if (serverPlainPin.isNotBlank()) {
            if (FORBIDDEN_DEFAULT_PINS.any { it.equals(serverPlainPin, ignoreCase = true) }) {
                return false
            }
            return OfficerCredentialEntity.constantTimeEqualsPlain(trimmedPin, serverPlainPin)
        }

        return false
    }

    /**
     * Resolves an officer's NIP from non-secret directory sources (direct NIP input, Official ASN list,
     * OfficerAuthManager roster, or Room Personnel directory). NEVER creates or seeds PINs.
     */
    private suspend fun resolveNipFromDirectoryOrInput(nipOrName: String): String? = withContext(Dispatchers.IO) {
        val rawQuery = nipOrName.trim()
        if (rawQuery.isBlank()) return@withContext null
        val normalizedQuery = rawQuery.replace(" ", "")

        // 1. Check Official ASN BPBD directory
        OfficialAsnBpbdPersonnel.ALL_OFFICIAL_PERSONNEL.firstOrNull { asn ->
            asn.nip.replace(" ", "").equals(normalizedQuery, ignoreCase = true) ||
                asn.name.equals(rawQuery, ignoreCase = true) ||
                asn.name.contains(rawQuery, ignoreCase = true) ||
                (normalizedQuery.length >= 4 && asn.nip.replace(" ", "").contains(normalizedQuery, ignoreCase = true))
        }?.let { return@withContext it.nip.trim() }

        // 2. Check OfficerAuthManager roster (non-secret profile directory)
        officerAuthManager.getOfficerRoster().firstOrNull { r ->
            r.nip.replace(" ", "").equals(normalizedQuery, ignoreCase = true) ||
                r.fullName.equals(rawQuery, ignoreCase = true) ||
                r.fullName.contains(rawQuery, ignoreCase = true) ||
                (normalizedQuery.length >= 4 && r.nip.replace(" ", "").contains(normalizedQuery, ignoreCase = true))
        }?.let { return@withContext it.nip.trim() }

        // 3. Check local Room Personnel directory (non-secret)
        personnelDao.getAllPersonnelList().firstOrNull { p ->
            p.nip.replace(" ", "").equals(normalizedQuery, ignoreCase = true) ||
                p.name.equals(rawQuery, ignoreCase = true) ||
                p.name.contains(rawQuery, ignoreCase = true) ||
                (normalizedQuery.length >= 4 && p.nip.replace(" ", "").contains(normalizedQuery, ignoreCase = true))
        }?.let { return@withContext it.nip.trim() }

        // 4. Direct NIP/Registration ID input (allows Cloud-only officers added in Firebase Console to log in)
        val looksLikeNipOrBadge = normalizedQuery.length >= 4 &&
            normalizedQuery.matches(Regex("^[a-zA-Z0-9_\\-]+$")) &&
            normalizedQuery.any { it.isDigit() }
        if (looksLikeNipOrBadge) {
            return@withContext normalizedQuery
        }

        return@withContext null
    }

    /**
     * Pulls non-secret officer metadata from Cloud Firestore WITHOUT ever seeding local default PINs
     * or pushing client-generated default credentials to Cloud Firestore.
     */
    suspend fun harvestAndSyncAllOfficers(): Int = withContext(Dispatchers.IO) {
        val cloudFetchResult = FirestoreHelper.fetchAllCredentialsFromCloudSuspend()
        var syncedCount = 0
        if (cloudFetchResult.isSuccess) {
            val cloudCreds = cloudFetchResult.getOrNull().orEmpty()
            for (cloud in cloudCreds) {
                credentialDao.insertCredential(sanitizeForLocalMetadataCache(cloud))
                syncedCount++
            }
        }
        return@withContext syncedCount
    }

    /**
     * Pulls non-secret officer metadata from Cloud Firestore into Room cache (NEVER caches raw/hashed PINs).
     */
    fun pullCredentialsFromCloud(onComplete: ((Boolean, Int) -> Unit)? = null) {
        FirestoreHelper.fetchAllCredentialsFromCloud { cloudCreds ->
            if (cloudCreds.isNotEmpty()) {
                coroutineScope.launch {
                    var updatedCount = 0
                    for (cloud in cloudCreds) {
                        credentialDao.insertCredential(sanitizeForLocalMetadataCache(cloud))
                        updatedCount++
                    }
                    Log.d(TAG, "Pulled $updatedCount sanitized officer metadata records from Cloud Firestore.")
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
     * PIN-Only Login (Skenario B when `saved_nip` is already saved in Secure Storage):
     * Requires a known `preferredNip` (`saved_nip`) and verifies strictly against Firebase Cloud Firestore.
     * NEVER checks local defaults or auto-seeds PINs.
     */
    suspend fun authenticateOfficerByPinOnly(
        inputPin: String,
        preferredNip: String? = null
    ): Result<OfficerProfile> = withContext(Dispatchers.IO) {
        val trimmedPin = inputPin.trim()
        if (trimmedPin.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Masukkan PIN petugas."))
        }
        if (FORBIDDEN_DEFAULT_PINS.any { it.equals(trimmedPin, ignoreCase = true) }) {
            return@withContext Result.failure(
                SecurityException("PIN default '$trimmedPin' tidak diizinkan. Gunakan PIN pribadi yang terdaftar di Firebase.")
            )
        }

        val targetNip = preferredNip?.trim().orEmpty().ifBlank { officerAuthManager.getSavedNip() }
        if (targetNip.isBlank()) {
            return@withContext Result.failure(
                IllegalArgumentException("NIP belum tersimpan di perangkat. Silakan masukkan NIP dan PIN Anda untuk verifikasi ke Firebase.")
            )
        }

        return@withContext authenticateOfficer(
            nip = targetNip,
            inputPin = trimmedPin
        )
    }

    /**
     * Skenario A (Fresh Install / `saved_nip` Kosong):
     * Resolves NIP from input (NIP or Name) WITHOUT creating local default PINs,
     * then verifies strictly against Cloud Firestore (`officer_credentials/{nip}`).
     * Only on Firebase verification success does it persist `saved_nip` locally (NEVER the PIN!).
     */
    suspend fun authenticateByNipOrNameAndPin(
        nipOrName: String,
        inputPin: String
    ): Result<OfficerProfile> = withContext(Dispatchers.IO) {
        val rawQuery = nipOrName.trim()
        val trimmedPin = inputPin.trim()

        if (rawQuery.isBlank() || trimmedPin.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Silakan masukkan NIP/Nama dan PIN Anda."))
        }

        if (FORBIDDEN_DEFAULT_PINS.any { it.equals(trimmedPin, ignoreCase = true) }) {
            return@withContext Result.failure(
                SecurityException("PIN default '$trimmedPin' tidak diizinkan. Wajib gunakan PIN pribadi Anda di Firebase.")
            )
        }

        val resolvedNip = resolveNipFromDirectoryOrInput(rawQuery)
            ?: return@withContext Result.failure(
                IllegalArgumentException("NIP atau Nama '$rawQuery' tidak dikenali. Masukkan NIP resmi Anda.")
            )

        return@withContext authenticateOfficer(
            nip = resolvedNip,
            inputPin = trimmedPin
        )
    }

    /**
     * Mandatory Cloud Officer Authentication (`officer_credentials/{nip}` via `Source.SERVER`):
     * 1. Rejects all hardcoded/default PINs (`117`, `BPBD117`, `TRC2026`, `0000`, `1234`) unconditionally.
     * 2. Sends NIP to Firebase Cloud Firestore (`Source.SERVER`) and matches PIN strictly against server record.
     *    NEVER falls back to local default PIN seeding if offline or missing.
     * 3. ONLY AFTER Firebase verifies the PIN is valid, saves ONLY `saved_nip` + non-secret display metadata
     *    to encrypted SharedPreferences. NEVER saves the raw PIN or local PIN hash on the device!
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

        // 1. STRICT ZERO-HARDCODE CHECK: Reject generic default PINs unconditionally
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
                        "Wajib login menggunakan PIN pribadi yang diverifikasi oleh server Firebase."
                )
            )
        }

        // 2. MANDATORY CLOUD VERIFICATION (Source.SERVER on `officer_credentials/{nip}`)
        val cloudLookup = FirestoreHelper.fetchOfficerCredentialStrictFromServer(trimmedNip, context)
        val foundCloudRecord = when (cloudLookup) {
            is CloudCredentialLookupResult.Found -> cloudLookup
            is CloudCredentialLookupResult.NotFound -> {
                auditDao.insertAuditLog(
                    PersonnelAuditLogEntity(
                        nip = trimmedNip,
                        name = "Petugas Tidak Terdaftar",
                        role = "-",
                        shift = "-",
                        action = "AUTH_REJECTED_NOT_FOUND_IN_CLOUD",
                        deviceInfo = "NIP ${maskNip(trimmedNip)} tidak ditemukan di koleksi officer_credentials Firebase ($deviceModel)"
                    )
                )
                return@withContext Result.failure(
                    IllegalArgumentException(
                        "NIP tidak ditemukan pada server Firebase (officer_credentials). Hubungi Admin Pusdalops untuk aktivasi akun."
                    )
                )
            }
            is CloudCredentialLookupResult.Error -> {
                auditDao.insertAuditLog(
                    PersonnelAuditLogEntity(
                        nip = trimmedNip,
                        name = "Petugas",
                        role = "-",
                        shift = "-",
                        action = "AUTH_BLOCKED_CLOUD_UNAVAILABLE",
                        deviceInfo = "${cloudLookup.errorCode}: ${cloudLookup.userMessage} ($deviceModel)"
                    )
                )
                return@withContext Result.failure(
                    IllegalStateException(cloudLookup.userMessage, cloudLookup.cause)
                )
            }
        }

        val credential = foundCloudRecord.credential

        // Block old 6-digit NIP initial PIN if officer already changed PIN in Cloud
        val sixDigitNip = OfficerCredentialEntity.generateInitialPinForNip(credential.nip)
        val rawLastSixNip = credential.nip.trim().takeLast(6)
        val isInputSixDigitNip = trimmedPin.equals(sixDigitNip, ignoreCase = true) ||
            (rawLastSixNip.length == 6 && trimmedPin.equals(rawLastSixNip, ignoreCase = true))
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
                SecurityException("PIN awal (6 digit terakhir NIP) sudah tidak berlaku karena Anda telah mengganti PIN di Firebase. Gunakan PIN pribadi terbaru Anda.")
            )
        }

        // 3. STRICT SERVER CREDENTIAL MATCHING (Zero local default fallback)
        val isPinCorrect = verifyAgainstCloudRecord(trimmedPin, foundCloudRecord)
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
            } else {
                "PIN salah untuk NIP ${maskNip(trimmedNip)}. Pastikan Anda menggunakan PIN terbaru yang tersimpan di server Firebase. (Sisa percobaan: ${MAX_FAILED_ATTEMPTS - currentFailures})"
            }
            return@withContext Result.failure(IllegalArgumentException(hintMessage))
        }

        // 4. CLOUD VERIFICATION SUCCEEDED:
        // Save ONLY NIP + non-secret profile locally (NEVER store raw PIN or local PIN hash on device!)
        failedAttemptMap.remove(lockoutKey)
        val now = System.currentTimeMillis()
        val deviceDesc = "$deviceModel (Android ${Build.VERSION.RELEASE})"

        val sanitizedMetadata = sanitizeForLocalMetadataCache(
            credential.copy(
                lastLoginAt = now,
                lastLoginDevice = deviceDesc,
                updatedAt = now
            )
        )
        credentialDao.insertCredential(sanitizedMetadata)
        activeCredential = sanitizedMetadata

        // Sync non-secret login timestamp/device metadata back to Cloud (preserving server pinHash & salt)
        if (credential.pinHash.isNotBlank() && credential.salt.isNotBlank()) {
            val cloudMetadataUpdate = credential.copy(
                lastLoginAt = now,
                lastLoginDevice = deviceDesc,
                initialSecurePin = "",
                isSyncedWithCloud = true,
                updatedAt = now
            )
            FirestoreHelper.syncCredentialToCloud(cloudMetadataUpdate, context = context)
        }

        // Record ONLY non-secret custom PIN status flag (NEVER writes PIN or PIN hash to SharedPreferences)
        officerAuthManager.markCloudCustomPinStatus(credential.isCustomPinSet)

        // Save ONLY NIP & non-secret display metadata to encrypted SharedPreferences AFTER Cloud success
        officerAuthManager.saveNipToSecureStorage(
            nip = credential.nip,
            fullName = credential.fullName,
            role = credential.role,
            shift = credential.shift
        )
        officerAuthManager.loginOfficerDirectly(
            name = credential.fullName,
            badgeId = credential.nip,
            shift = credential.shift,
            role = credential.role
        )

        auditDao.insertAuditLog(
            PersonnelAuditLogEntity(
                nip = trimmedNip,
                name = credential.fullName,
                role = credential.role,
                shift = credential.shift,
                action = "LOGIN_SUCCESSFUL_CLOUD_VERIFIED",
                deviceInfo = "Berhasil masuk di $deviceDesc (PIN Terverifikasi Server Firebase, NIP disimpan lokal)"
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
     * Verifies the officer NIP exists in Cloud Firestore (`Source.SERVER`) and establishes session
     * without ever storing or resetting PINs locally.
     */
    suspend fun authenticateWithBiometric(nip: String): Result<OfficerCredentialEntity> = withContext(Dispatchers.IO) {
        val trimmedNip = nip.trim()
        if (trimmedNip.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Petugas tidak ditemukan"))
        }

        val cloudLookup = FirestoreHelper.fetchOfficerCredentialStrictFromServer(trimmedNip, context)
        val activeCred = when (cloudLookup) {
            is CloudCredentialLookupResult.Found -> cloudLookup.credential
            is CloudCredentialLookupResult.NotFound -> {
                return@withContext Result.failure(IllegalArgumentException("Petugas tidak ditemukan di server Firebase."))
            }
            is CloudCredentialLookupResult.Error -> {
                return@withContext Result.failure(IllegalStateException(cloudLookup.userMessage, cloudLookup.cause))
            }
        }

        val now = System.currentTimeMillis()
        val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
        val deviceDesc = "$deviceModel (Android ${Build.VERSION.RELEASE} • Biometrik)"

        val sanitizedMetadata = sanitizeForLocalMetadataCache(
            activeCred.copy(
                lastLoginAt = now,
                lastLoginDevice = deviceDesc,
                updatedAt = now
            )
        )
        credentialDao.insertCredential(sanitizedMetadata)
        activeCredential = sanitizedMetadata

        if (activeCred.pinHash.isNotBlank() && activeCred.salt.isNotBlank()) {
            FirestoreHelper.syncCredentialToCloud(
                activeCred.copy(
                    lastLoginAt = now,
                    lastLoginDevice = deviceDesc,
                    initialSecurePin = "",
                    isSyncedWithCloud = true,
                    updatedAt = now
                ),
                context = context
            )
        }

        officerAuthManager.markCloudCustomPinStatus(activeCred.isCustomPinSet)
        officerAuthManager.saveNipToSecureStorage(
            nip = activeCred.nip,
            fullName = activeCred.fullName,
            role = activeCred.role,
            shift = activeCred.shift
        )
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
                deviceInfo = "Login Biometrik berhasil di $deviceDesc (Terverifikasi Server Firebase)"
            )
        )

        return@withContext Result.success(sanitizedMetadata)
    }

    /**
     * Changes personal PIN for a specific officer strictly via Firebase Cloud Firestore:
     * 1. Fetches current credential strictly from Cloud Firestore (`Source.SERVER`).
     * 2. Verifies `currentPin` against Cloud credential.
     * 3. Validates `newPin` (min 4 chars, not forbidden default, not 6-digit NIP, not identical).
     * 4. Persists salted PBKDF2 hash to Cloud Firestore `officer_credentials/{nip}` and REQUIRES Cloud write success
     *    so the PIN change is permanent across app updates and reinstalls.
     * 5. NEVER stores the raw PIN or hash in SharedPreferences or Room!
     */
    suspend fun changePersonalPin(
        nip: String,
        currentPin: String,
        newPin: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val trimmedNip = nip.trim()
        val trimmedOld = currentPin.trim()
        val trimmedNew = newPin.trim()

        if (trimmedNip.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("NIP petugas tidak boleh kosong."))
        }

        val cloudLookup = FirestoreHelper.fetchOfficerCredentialStrictFromServer(trimmedNip, context)
        val foundCloudRecord = when (cloudLookup) {
            is CloudCredentialLookupResult.Found -> cloudLookup
            is CloudCredentialLookupResult.NotFound -> {
                return@withContext Result.failure(
                    IllegalArgumentException("Akun petugas dengan NIP '$trimmedNip' tidak ditemukan di server Firebase.")
                )
            }
            is CloudCredentialLookupResult.Error -> {
                return@withContext Result.failure(
                    IllegalStateException(cloudLookup.userMessage, cloudLookup.cause)
                )
            }
        }
        val activeCred = foundCloudRecord.credential

        // Verify currentPin strictly against Cloud Firestore credential
        val isOldPinValid = verifyAgainstCloudRecord(trimmedOld, foundCloudRecord)
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
                IllegalArgumentException("PIN pribadi baru tidak boleh menggunakan 6 digit terakhir NIP.")
            )
        }

        if (trimmedNew == trimmedOld) {
            return@withContext Result.failure(IllegalArgumentException("PIN baru tidak boleh sama dengan PIN saat ini."))
        }

        val newSalt = OfficerCredentialEntity.generateCryptographicSalt()
        val newHash = OfficerCredentialEntity.hashWithSalt(trimmedNew, newSalt)
        val now = System.currentTimeMillis()

        val cloudPayload = activeCred.copy(
            pinHash = newHash,
            salt = newSalt,
            isCustomPinSet = true,
            initialSecurePin = "",
            isSyncedWithCloud = true,
            updatedAt = now
        )

        // REQUIRE Cloud Firestore persistence to succeed so PIN never reverts after update/reinstall!
        val cloudSyncResult = FirestoreHelper.syncCredentialToCloudSuspend(cloudPayload, context = context)
        if (cloudSyncResult.isFailure) {
            return@withContext Result.failure(
                IllegalStateException(
                    cloudSyncResult.exceptionOrNull()?.message
                        ?: "Gagal menyimpan PIN baru ke server Firebase. Periksa koneksi internet Anda lalu coba lagi.",
                    cloudSyncResult.exceptionOrNull()
                )
            )
        }

        // Update ONLY non-secret metadata locally (NEVER store PIN or PIN hash on device!)
        val sanitizedLocal = sanitizeForLocalMetadataCache(cloudPayload)
        credentialDao.insertCredential(sanitizedLocal)
        activeCredential = sanitizedLocal
        officerAuthManager.markCloudCustomPinStatus(true)

        auditDao.insertAuditLog(
            PersonnelAuditLogEntity(
                nip = trimmedNip,
                name = activeCred.fullName,
                role = activeCred.role,
                shift = activeCred.shift,
                action = "PIN_CHANGED_PERMANENT_CLOUD",
                deviceInfo = "PIN pribadi diperbarui permanen di Cloud Firestore (tidak disimpan di HP)"
            )
        )

        return@withContext Result.success(
            "PIN pribadi untuk ${activeCred.fullName} berhasil diperbarui secara permanen di server Firebase!"
        )
    }

    /**
     * Admin-only PIN reset via Cloud Firestore.
     * Strictly requires Cloud Firestore persistence and NEVER stores plaintext PIN locally or in Cloud.
     */
    suspend fun resetToInitialNipSecret(nip: String): Result<String> = withContext(Dispatchers.IO) {
        val trimmedNip = nip.trim()
        val cloudLookup = FirestoreHelper.fetchOfficerCredentialStrictFromServer(trimmedNip, context)
        val cred = when (cloudLookup) {
            is CloudCredentialLookupResult.Found -> cloudLookup.credential
            is CloudCredentialLookupResult.NotFound -> {
                return@withContext Result.failure(
                    IllegalArgumentException("Petugas dengan NIP '$trimmedNip' tidak ditemukan di server Firebase.")
                )
            }
            is CloudCredentialLookupResult.Error -> {
                return@withContext Result.failure(
                    IllegalStateException(cloudLookup.userMessage, cloudLookup.cause)
                )
            }
        }

        val initialPin = OfficerCredentialEntity.generateInitialPinForNip(cred.nip)
        val salt = OfficerCredentialEntity.generateCryptographicSalt()
        val hash = OfficerCredentialEntity.hashWithSalt(initialPin, salt)
        val now = System.currentTimeMillis()

        val syncCandidate = cred.copy(
            pinHash = hash,
            salt = salt,
            isCustomPinSet = false,
            initialSecurePin = "",
            isSyncedWithCloud = true,
            updatedAt = now
        )

        val syncResult = FirestoreHelper.syncCredentialToCloudSuspend(syncCandidate, context = context)
        if (syncResult.isFailure) {
            return@withContext Result.failure(
                IllegalStateException(
                    syncResult.exceptionOrNull()?.message
                        ?: "Gagal melakukan reset PIN di server Firebase. Periksa koneksi internet.",
                    syncResult.exceptionOrNull()
                )
            )
        }

        val sanitizedLocal = sanitizeForLocalMetadataCache(syncCandidate)
        credentialDao.insertCredential(sanitizedLocal)
        officerAuthManager.markCloudCustomPinStatus(false)

        auditDao.insertAuditLog(
            PersonnelAuditLogEntity(
                nip = trimmedNip,
                name = cred.fullName,
                role = cred.role,
                shift = cred.shift,
                action = "PIN_RESET_TO_NIP_KEY_CLOUD",
                deviceInfo = "PIN di-reset di Cloud Firestore oleh Admin (Bukan PIN default 117)"
            )
        )

        return@withContext Result.success(
            "PIN akun ${cred.fullName} berhasil di-reset di server Firebase ke 6 digit terakhir NIP."
        )
    }

    /**
     * Verifies a candidate PIN strictly against Cloud Firestore (`Source.SERVER`) without creating
     * a login session or storing any PIN locally. Used for self-service authorization (e.g., PIN reset).
     */
    suspend fun verifyOfficerPinAgainstCloud(nip: String, inputPin: String): Boolean = withContext(Dispatchers.IO) {
        val trimmedNip = nip.trim()
        val trimmedPin = inputPin.trim()
        if (trimmedNip.isBlank() || trimmedPin.isBlank()) return@withContext false
        if (FORBIDDEN_DEFAULT_PINS.any { it.equals(trimmedPin, ignoreCase = true) }) return@withContext false
        val lookup = FirestoreHelper.fetchOfficerCredentialStrictFromServer(trimmedNip, context)
        return@withContext if (lookup is CloudCredentialLookupResult.Found) {
            verifyAgainstCloudRecord(trimmedPin, lookup)
        } else {
            false
        }
    }

    /**
     * Checks if an officer already has a personalized custom PIN set on Cloud Firestore.
     */
    suspend fun isCustomPinConfigured(nip: String): Boolean = withContext(Dispatchers.IO) {
        val cred = getCredential(nip)
        return@withContext cred?.isCustomPinSet == true
    }

    /**
     * Gets non-secret officer credential metadata from Cloud Firestore first (`Source.SERVER`).
     */
    suspend fun getCredential(nip: String): OfficerCredentialEntity? = withContext(Dispatchers.IO) {
        val trimmedNip = nip.trim()
        val cloudResult = FirestoreHelper.fetchCredentialByNipFromCloud(trimmedNip, context)
        if (cloudResult.isSuccess) {
            val cloudCred = cloudResult.getOrNull()
            if (cloudCred != null) {
                val sanitized = sanitizeForLocalMetadataCache(cloudCred)
                credentialDao.insertCredential(sanitized)
                return@withContext sanitized
            }
        }
        return@withContext credentialDao.getCredentialByNip(trimmedNip)
    }
}
