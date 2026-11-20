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

    private suspend fun safeInsertAuditLog(entry: PersonnelAuditLogEntity) {
        try {
            auditDao.insertAuditLog(entry)
        } catch (e: Exception) {
            Log.w(TAG, "Non-fatal audit log write skipped: ${e.message}")
        }
    }

    private suspend fun safeCacheMetadata(sanitizedMetadata: OfficerCredentialEntity) {
        try {
            credentialDao.insertCredential(sanitizedMetadata)
        } catch (e: Exception) {
            Log.w(TAG, "Non-fatal local metadata cache write skipped: ${e.message}")
        }
    }

    /**
     * Verifies a candidate PIN strictly against the Cloud Firestore record (`pinHash` + `salt`,
     * and/or server-side `pin` field if configured by Admin in Firebase Console).
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
        // 1. Primary path: cryptographic hash stored in Cloud Firestore (PBKDF2+salt or legacy SHA-256)
        if (cred.pinHash.isNotBlank() && cred.verifyPin(trimmedPin)) {
            return true
        }

        // 2. Secondary path: server-managed plaintext `pin` field in Firestore Console (never mutated),
        //    also honored if an Admin updated `pin` directly in Console on a record that still had an older hash.
        val serverPlainPin = cloudRecord.serverPlainPin?.trim().orEmpty()
        if (serverPlainPin.isNotBlank()) {
            if (FORBIDDEN_DEFAULT_PINS.any { it.equals(serverPlainPin, ignoreCase = true) }) {
                return false
            }
            if (OfficerCredentialEntity.constantTimeEqualsPlain(trimmedPin, serverPlainPin)) {
                return true
            }
        }

        return false
    }

    private fun normalizeNameForMatch(value: String): String {
        return value
            .lowercase()
            .replace(Regex("[.,\\-_/()\\s]+"), " ")
            .trim()
    }

    private fun levenshteinDistance(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        var prev = IntArray(b.length + 1) { it }
        var curr = IntArray(b.length + 1)
        for (i in 1..a.length) {
            curr[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                curr[j] = minOf(
                    curr[j - 1] + 1,
                    prev[j] + 1,
                    prev[j - 1] + cost
                )
            }
            val tmp = prev
            prev = curr
            curr = tmp
        }
        return prev[b.length]
    }

    /**
     * Extracts an embedded NIP/registration token from combined input like:
     * - "Ruri Irwandi, 197906072006041011"
     * - "Yudha Sofrianto (199807202025061002)"
     * - "19790607 200604 1 011"
     * - "TRC-01" / "BPBD-001"
     */
    private fun extractEmbeddedNipToken(rawQuery: String): String? {
        val compact = rawQuery.replace(Regex("[\\s\\-]+"), "")
        // 1. Standard Indonesian ASN NIP (18 digits) or long numeric registration ID (8..22 digits)
        val longDigits = Regex("\\d{8,22}").find(compact)?.value
        if (!longDigits.isNullOrBlank()) return longDigits

        // 2. Spaced ASN NIP inside mixed text e.g. "Nama, 19790607 200604 1 011"
        val spacedDigitsMatch = Regex("(?:\\d[\\s\\-]*){8,22}").find(rawQuery)?.value
        if (!spacedDigitsMatch.isNullOrBlank()) {
            val cleaned = spacedDigitsMatch.replace(Regex("[^0-9]"), "")
            if (cleaned.length >= 8) return cleaned
        }

        // 3. TRC / Pusdalops alphanumeric badge code (e.g. TRC-001, BPBD-12)
        val badgeMatch = Regex("\\b(?:TRC|BPBD|PUSDALOPS)[-_]?\\d{2,10}\\b", RegexOption.IGNORE_CASE)
            .find(rawQuery)?.value
        if (!badgeMatch.isNullOrBlank()) return badgeMatch.uppercase()

        return null
    }

    /**
     * Resolves an officer's NIP from non-secret directory sources (direct NIP input, combined "Nama, NIP",
     * Official ASN list, OfficerAuthManager roster, or Room Personnel directory). NEVER creates or seeds PINs.
     */
    suspend fun resolveNipFromDirectoryOrInput(nipOrName: String): String? = withContext(Dispatchers.IO) {
        val rawQuery = nipOrName.trim()
        if (rawQuery.isBlank()) return@withContext null

        val embeddedNip = extractEmbeddedNipToken(rawQuery)
        val compactQuery = rawQuery.replace(Regex("[\\s\\-]+"), "")
        val normalizedQueryName = normalizeNameForMatch(rawQuery)
        val querySegments = rawQuery
            .split(",", "|", "/", ";", "\n", "(", ")")
            .map { normalizeNameForMatch(it) }
            .filter { it.length >= 3 }

        fun matchesOfficer(candidateNip: String, candidateName: String): Boolean {
            val cleanNip = candidateNip.trim().replace(Regex("[\\s\\-]+"), "")
            val cleanName = normalizeNameForMatch(candidateName)
            if (embeddedNip != null && cleanNip.equals(embeddedNip, ignoreCase = true)) return true
            if (cleanNip.equals(compactQuery, ignoreCase = true)) return true
            if (embeddedNip != null && embeddedNip.length >= 6 && cleanNip.contains(embeddedNip, ignoreCase = true)) return true
            if (compactQuery.length >= 6 && compactQuery.all { it.isDigit() } && cleanNip.contains(compactQuery, ignoreCase = true)) return true
            if (cleanName == normalizedQueryName) return true
            if (normalizedQueryName.length >= 3 && (cleanName.contains(normalizedQueryName) || normalizedQueryName.contains(cleanName))) return true
            if (querySegments.any { seg ->
                    cleanName == seg || cleanName.contains(seg) ||
                        (cleanNip.length >= 6 && cleanNip.equals(seg.replace(" ", ""), ignoreCase = true))
                }
            ) return true
            return false
        }

        // 1. Check Official ASN BPBD directory
        OfficialAsnBpbdPersonnel.ALL_OFFICIAL_PERSONNEL.firstOrNull { asn ->
            matchesOfficer(asn.nip, asn.name)
        }?.let { return@withContext it.nip.trim().replace(" ", "") }

        // 2. Check OfficerAuthManager roster (non-secret profile directory)
        officerAuthManager.getOfficerRoster().firstOrNull { r ->
            matchesOfficer(r.nip, r.fullName)
        }?.let { return@withContext it.nip.trim().replace(" ", "") }

        // 3. Check local Room Personnel directory (non-secret)
        val roomPersonnel = try {
            personnelDao.getAllPersonnelList()
        } catch (_: Exception) {
            emptyList()
        }
        roomPersonnel.firstOrNull { p ->
            matchesOfficer(p.nip, p.name)
        }?.let { return@withContext it.nip.trim().replace(" ", "") }

        // 4. Embedded NIP token from combined "Nama, NIP" input (allows Cloud-only officers to log in seamlessly)
        if (!embeddedNip.isNullOrBlank()) {
            return@withContext embeddedNip
        }

        // 5. Unambiguous minor-typo fuzzy match on full/base name (e.g., "Hitatur Arazak" -> "HITATUN ARAZAK")
        val primaryQueryName = querySegments.firstOrNull { seg -> seg.any { it.isLetter() } } ?: normalizedQueryName
        if (primaryQueryName.length >= 6 && primaryQueryName.any { it.isLetter() }) {
            val allCandidates = (
                OfficialAsnBpbdPersonnel.ALL_OFFICIAL_PERSONNEL.map { it.nip to it.name } +
                    officerAuthManager.getOfficerRoster().map { it.nip to it.fullName } +
                    roomPersonnel.map { it.nip to it.name }
                ).distinctBy { it.first.trim().replace(Regex("[\\s\\-]+"), "") }

            val scored = allCandidates.mapNotNull { (candNip, candName) ->
                val cleanCandFull = normalizeNameForMatch(candName)
                val cleanCandBase = normalizeNameForMatch(candName.substringBefore(","))
                val distFull = levenshteinDistance(primaryQueryName, cleanCandFull)
                val distBase = levenshteinDistance(primaryQueryName, cleanCandBase)
                val bestDist = minOf(distFull, distBase)
                if (bestDist in 1..2) candNip.trim().replace(Regex("[\\s\\-]+"), "") to bestDist else null
            }.sortedBy { it.second }

            if (scored.isNotEmpty()) {
                val minDist = scored.first().second
                val bestMatches = scored.filter { it.second == minDist }
                if (bestMatches.size == 1) {
                    return@withContext bestMatches.first().first
                }
            }
        }

        // 6. Direct NIP/Registration ID input (allows Cloud-only officers added in Firebase Console to log in)
        val looksLikeNipOrBadge = compactQuery.length >= 4 &&
            compactQuery.matches(Regex("^[a-zA-Z0-9_\\-]+$")) &&
            compactQuery.any { it.isDigit() }
        if (looksLikeNipOrBadge) {
            return@withContext compactQuery
        }

        return@withContext null
    }

    /**
     * Resolves an official registered officer profile from local non-secret directories (for first-time
     * Cloud activation or biometric display label). NEVER returns or generates a hardcoded default PIN.
     */
    private suspend fun findRegisteredOfficerMetadata(nip: String): OfficerProfile? = withContext(Dispatchers.IO) {
        val cleanNip = nip.trim().replace(Regex("[\\s\\-]+"), "")
        if (cleanNip.isBlank()) return@withContext null

        OfficialAsnBpbdPersonnel.ALL_OFFICIAL_PERSONNEL.firstOrNull {
            it.nip.trim().replace(Regex("[\\s\\-]+"), "").equals(cleanNip, ignoreCase = true)
        }?.let { asn ->
            return@withContext OfficerProfile(
                id = "asn-${asn.nip.trim()}",
                fullName = asn.name,
                nip = asn.nip.trim(),
                role = asn.role,
                unit = "BPBD Kab. Mukomuko",
                shift = "Shift Siaga 24 Jam",
                phone = ""
            )
        }

        officerAuthManager.getOfficerRoster().firstOrNull {
            it.nip.trim().replace(Regex("[\\s\\-]+"), "").equals(cleanNip, ignoreCase = true)
        }?.let { return@withContext it }

        personnelDao.getAllPersonnelList().firstOrNull {
            it.nip.trim().replace(Regex("[\\s\\-]+"), "").equals(cleanNip, ignoreCase = true)
        }?.let { p ->
            return@withContext OfficerProfile(
                id = "room-${p.nip.trim()}",
                fullName = p.name,
                nip = p.nip.trim(),
                role = p.role,
                unit = "Pusdalops BPBD Kab. Mukomuko",
                shift = "Shift Siaga 24 Jam",
                phone = p.phone
            )
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
            safeInsertAuditLog(
                PersonnelAuditLogEntity(
                    nip = trimmedNip,
                    name = "Percobaan Akses PIN Default",
                    role = "Tidak Sah",
                    shift = "-",
                    action = "AUTH_BLOCKED_DEFAULT_PIN_REJECTED",
                    deviceInfo = "PIN default '$trimmedPin' ditolak pada perangkat $deviceModel"
                )
            )
            try {
                com.example.security.SecurityManager.getInstance(context).recordLoginActivityEvent(
                    officerNip = trimmedNip,
                    officerName = "Percobaan Akses PIN Default",
                    officerRole = "Tidak Sah",
                    loginMethod = "PIN_CLOUD",
                    loginStatus = "BLOCKED",
                    action = "AUTH_BLOCKED_DEFAULT_PIN_REJECTED",
                    detail = "PIN default '$trimmedPin' ditolak pada perangkat $deviceModel",
                    accessGranted = false
                )
            } catch (_: Exception) {}
            return@withContext Result.failure(
                SecurityException(
                    "PIN default '$trimmedPin' dinonaktifkan oleh sistem keamanan. " +
                        "Wajib login menggunakan PIN pribadi yang diverifikasi oleh server Firebase."
                )
            )
        }

        // 2. MANDATORY CLOUD VERIFICATION (Source.SERVER on `officer_credentials/{nip}`)
        val cleanLookupNip = trimmedNip.replace(Regex("[\\s\\-]+"), "")
        val cloudLookup = FirestoreHelper.fetchOfficerCredentialStrictFromServer(cleanLookupNip, context)
        val foundCloudRecord = when (cloudLookup) {
            is CloudCredentialLookupResult.Found -> {
                val existingCred = cloudLookup.credential
                val needsHashActivation = existingCred.pinHash.isBlank() ||
                    (existingCred.salt.isBlank() && existingCred.pinHash.trim().length != 64)
                val hasServerPlainPin = !cloudLookup.serverPlainPin.isNullOrBlank()
                if (needsHashActivation && !hasServerPlainPin && !existingCred.isCustomPinSet && !existingCred.isPinChanged) {
                    // Existing Firestore doc without cryptographic hash (e.g. seeded via createDefault() or partial Console doc):
                    // Activate securely if NIP is an official registered officer and PIN matches unique 6-digit NIP key.
                    val registeredOfficer = findRegisteredOfficerMetadata(cleanLookupNip)
                    val uniqueActivationKey = OfficerCredentialEntity.generateInitialPinForNip(cleanLookupNip)
                    val rawLastSix = cleanLookupNip.takeLast(6)
                    val matchesUniqueNipKey = uniqueActivationKey.isNotBlank() && (
                        OfficerCredentialEntity.constantTimeEqualsPlain(trimmedPin, uniqueActivationKey) ||
                            (rawLastSix.length == 6 && OfficerCredentialEntity.constantTimeEqualsPlain(trimmedPin, rawLastSix))
                        )
                    if (registeredOfficer != null && matchesUniqueNipKey) {
                        val activationSalt = OfficerCredentialEntity.generateCryptographicSalt()
                        val activationHash = OfficerCredentialEntity.hashWithSalt(trimmedPin, activationSalt)
                        val nowActivation = System.currentTimeMillis()
                        val activatedCloudCredential = existingCred.copy(
                            nip = registeredOfficer.nip.trim().replace(Regex("[\\s\\-]+"), ""),
                            fullName = existingCred.fullName.ifBlank { registeredOfficer.fullName },
                            role = existingCred.role.ifBlank { registeredOfficer.role },
                            unit = existingCred.unit.ifBlank { registeredOfficer.unit },
                            shift = existingCred.shift.ifBlank { registeredOfficer.shift },
                            phone = existingCred.phone.ifBlank { registeredOfficer.phone },
                            pinHash = activationHash,
                            salt = activationSalt,
                            isCustomPinSet = false,
                            initialSecurePin = "",
                            lastLoginAt = nowActivation,
                            lastLoginDevice = "$deviceModel (Android ${Build.VERSION.RELEASE})",
                            isSyncedWithCloud = true,
                            updatedAt = nowActivation
                        )
                        val cloudWriteRes = FirestoreHelper.syncCredentialToCloudSuspend(
                            credential = activatedCloudCredential,
                            context = context,
                            preserveOwnerUid = cloudLookup.ownerUid
                        )
                        if (cloudWriteRes.isSuccess) {
                            Log.i(TAG, "Uninitialized Cloud credential activated on Firestore for official NIP ${maskNip(cleanLookupNip)}")
                            cloudLookup.copy(credential = activatedCloudCredential)
                        } else {
                            val writeErr = cloudWriteRes.exceptionOrNull()?.message
                                ?: "Gagal mengaktifkan kredensial di server Firebase."
                            safeInsertAuditLog(
                                PersonnelAuditLogEntity(
                                    nip = cleanLookupNip,
                                    name = existingCred.fullName,
                                    role = existingCred.role,
                                    shift = existingCred.shift,
                                    action = "AUTH_REJECTED_CLOUD_ACTIVATION_FAILED",
                                    deviceInfo = "$writeErr ($deviceModel)"
                                )
                            )
                            return@withContext Result.failure(IllegalStateException(writeErr, cloudWriteRes.exceptionOrNull()))
                        }
                    } else {
                        cloudLookup
                    }
                } else {
                    cloudLookup
                }
            }
            is CloudCredentialLookupResult.NotFound -> {
                // First-time Cloud Activation for official registered ASN/Pusdalops officers who do not yet
                // have a document in Cloud Firestore:
                // - ONLY when Cloud explicitly responded NotFound (online, no existing Cloud PIN)
                // - NEVER accepts forbidden default PINs ('117', 'BPBD117', etc.)
                // - Accepts the officer's unique 6-digit NIP activation key ONLY if Cloud Firestore write succeeds!
                val registeredOfficer = findRegisteredOfficerMetadata(cleanLookupNip)
                val uniqueActivationKey = OfficerCredentialEntity.generateInitialPinForNip(cleanLookupNip)
                val rawLastSix = cleanLookupNip.takeLast(6)
                val matchesUniqueNipKey = uniqueActivationKey.isNotBlank() && (
                    OfficerCredentialEntity.constantTimeEqualsPlain(trimmedPin, uniqueActivationKey) ||
                        (rawLastSix.length == 6 && OfficerCredentialEntity.constantTimeEqualsPlain(trimmedPin, rawLastSix))
                    )

                if (registeredOfficer != null && matchesUniqueNipKey) {
                    val activationSalt = OfficerCredentialEntity.generateCryptographicSalt()
                    val activationHash = OfficerCredentialEntity.hashWithSalt(trimmedPin, activationSalt)
                    val nowActivation = System.currentTimeMillis()
                    val activatedCloudCredential = OfficerCredentialEntity(
                        nip = registeredOfficer.nip.trim().replace(Regex("[\\s\\-]+"), ""),
                        fullName = registeredOfficer.fullName,
                        role = registeredOfficer.role,
                        unit = registeredOfficer.unit,
                        shift = registeredOfficer.shift,
                        phone = registeredOfficer.phone,
                        pinHash = activationHash,
                        salt = activationSalt,
                        isCustomPinSet = false,
                        initialSecurePin = "",
                        lastLoginAt = nowActivation,
                        lastLoginDevice = "$deviceModel (Android ${Build.VERSION.RELEASE})",
                        isSyncedWithCloud = true,
                        updatedAt = nowActivation
                    )
                    val cloudWriteRes = FirestoreHelper.syncCredentialToCloudSuspend(activatedCloudCredential, context = context)
                    if (cloudWriteRes.isSuccess) {
                        Log.i(TAG, "First-time Cloud credential activated on Firestore for official NIP ${maskNip(cleanLookupNip)}")
                        CloudCredentialLookupResult.Found(credential = activatedCloudCredential)
                    } else {
                        val writeErr = cloudWriteRes.exceptionOrNull()?.message
                            ?: "Gagal mengaktifkan kredensial di server Firebase."
                        safeInsertAuditLog(
                            PersonnelAuditLogEntity(
                                nip = cleanLookupNip,
                                name = registeredOfficer.fullName,
                                role = registeredOfficer.role,
                                shift = registeredOfficer.shift,
                                action = "AUTH_REJECTED_CLOUD_ACTIVATION_FAILED",
                                deviceInfo = "$writeErr ($deviceModel)"
                            )
                        )
                        return@withContext Result.failure(IllegalStateException(writeErr, cloudWriteRes.exceptionOrNull()))
                    }
                } else {
                    safeInsertAuditLog(
                        PersonnelAuditLogEntity(
                            nip = cleanLookupNip,
                            name = registeredOfficer?.fullName ?: "Petugas Tidak Terdaftar",
                            role = registeredOfficer?.role ?: "-",
                            shift = "-",
                            action = "AUTH_REJECTED_NOT_FOUND_IN_CLOUD",
                            deviceInfo = "NIP ${maskNip(cleanLookupNip)} belum teraktivasi di koleksi officer_credentials Firebase ($deviceModel)"
                        )
                    )
                    try {
                        com.example.security.SecurityManager.getInstance(context).recordLoginActivityEvent(
                            officerNip = cleanLookupNip,
                            officerName = registeredOfficer?.fullName ?: "Petugas Tidak Terdaftar",
                            officerRole = registeredOfficer?.role ?: "-",
                            loginMethod = "PIN_CLOUD",
                            loginStatus = "FAILED",
                            action = "AUTH_REJECTED_NOT_FOUND_IN_CLOUD",
                            detail = "NIP ${maskNip(cleanLookupNip)} belum teraktivasi di koleksi officer_credentials Firebase ($deviceModel)",
                            accessGranted = false
                        )
                    } catch (_: Exception) {}
                    val notFoundMsg = if (registeredOfficer != null) {
                        "Akun ${registeredOfficer.fullName} belum teraktivasi di server Firebase. Masukkan 6 digit terakhir NIP sebagai PIN aktivasi perdana (bukan 117), lalu ganti ke PIN pribadi Anda."
                    } else {
                        "NIP tidak ditemukan pada server Firebase (officer_credentials). Hubungi Admin Pusdalops untuk aktivasi akun."
                    }
                    return@withContext Result.failure(IllegalArgumentException(notFoundMsg))
                }
            }
            is CloudCredentialLookupResult.Error -> {
                safeInsertAuditLog(
                    PersonnelAuditLogEntity(
                        nip = cleanLookupNip,
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
        // (unless Admin explicitly configured matching serverPlainPin in Firebase Console)
        val sixDigitNip = OfficerCredentialEntity.generateInitialPinForNip(credential.nip)
        val rawLastSixNip = credential.nip.trim().takeLast(6)
        val isInputSixDigitNip = trimmedPin.equals(sixDigitNip, ignoreCase = true) ||
            (rawLastSixNip.length == 6 && trimmedPin.equals(rawLastSixNip, ignoreCase = true))
        val adminConfiguredSamePlainPin = foundCloudRecord.serverPlainPin?.trim()?.let { pinVal ->
            !FORBIDDEN_DEFAULT_PINS.any { it.equals(pinVal, ignoreCase = true) } &&
                OfficerCredentialEntity.constantTimeEqualsPlain(trimmedPin, pinVal)
        } == true
        if ((credential.isPinChanged || credential.isCustomPinSet) && isInputSixDigitNip && !adminConfiguredSamePlainPin) {
            safeInsertAuditLog(
                PersonnelAuditLogEntity(
                    nip = trimmedNip,
                    name = credential.fullName,
                    role = credential.role,
                    shift = credential.shift,
                    action = "AUTH_BLOCKED_INITIAL_NIP_AFTER_PIN_CHANGED",
                    deviceInfo = "Percobaan login memakai 6 digit NIP setelah PIN diubah pada $deviceModel"
                )
            )
            try {
                com.example.security.SecurityManager.getInstance(context).recordLoginActivityEvent(
                    officerNip = trimmedNip,
                    officerName = credential.fullName,
                    officerRole = credential.role,
                    loginMethod = "PIN_CLOUD",
                    loginStatus = "BLOCKED",
                    action = "AUTH_BLOCKED_INITIAL_NIP_AFTER_PIN_CHANGED",
                    detail = "Percobaan login memakai 6 digit NIP setelah PIN diubah pada $deviceModel",
                    accessGranted = false
                )
            } catch (_: Exception) {}
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
            val failAction = if (lockUntil > 0L) "AUTH_LOCKED_OUT_BRUTE_FORCE" else "AUTH_INVALID_PIN"
            val failDetail = "Percobaan PIN salah ($currentFailures/$MAX_FAILED_ATTEMPTS) pada $deviceModel"
            safeInsertAuditLog(
                PersonnelAuditLogEntity(
                    nip = trimmedNip,
                    name = credential.fullName,
                    role = credential.role,
                    shift = credential.shift,
                    action = failAction,
                    deviceInfo = failDetail
                )
            )
            try {
                com.example.security.SecurityManager.getInstance(context).recordLoginActivityEvent(
                    officerNip = trimmedNip,
                    officerName = credential.fullName,
                    officerRole = credential.role,
                    loginMethod = "PIN_CLOUD",
                    loginStatus = if (lockUntil > 0L) "BLOCKED" else "FAILED",
                    action = failAction,
                    detail = failDetail,
                    accessGranted = false
                )
            } catch (_: Exception) {}
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
        safeCacheMetadata(sanitizedMetadata)
        activeCredential = sanitizedMetadata

        // Sync non-secret login timestamp/device metadata back to Cloud (preserving server pinHash, salt & ownerUid)
        if (credential.pinHash.isNotBlank() && credential.salt.isNotBlank()) {
            val cloudMetadataUpdate = credential.copy(
                lastLoginAt = now,
                lastLoginDevice = deviceDesc,
                initialSecurePin = "",
                isSyncedWithCloud = true,
                updatedAt = now
            )
            FirestoreHelper.syncCredentialToCloud(
                credential = cloudMetadataUpdate,
                context = context,
                preserveOwnerUid = foundCloudRecord.ownerUid
            )
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

        safeInsertAuditLog(
            PersonnelAuditLogEntity(
                nip = trimmedNip,
                name = credential.fullName,
                role = credential.role,
                shift = credential.shift,
                action = "LOGIN_SUCCESSFUL_CLOUD_VERIFIED",
                deviceInfo = "Berhasil masuk di $deviceDesc (PIN Terverifikasi Server Firebase, NIP disimpan lokal)"
            )
        )

        // Sinkronkan aktivitas login PIN Cloud ke daftar Keamanan Perangkat & Sesi (Play Integrity Classic)
        try {
            com.example.security.SecurityManager.getInstance(context).verifyDeviceAuthenticityDuringLogin(
                officerNip = credential.nip,
                officerName = credential.fullName,
                officerRole = credential.role,
                loginMethod = "PIN_CLOUD",
                loginAction = "LOGIN_SUCCESSFUL_CLOUD_VERIFIED",
                detail = "Login PIN Cloud terverifikasi di $deviceDesc"
            )
        } catch (e: Exception) {
            Log.w(TAG, "Non-fatal Play Integrity session sync warning after PIN login: ${e.message}")
        }

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
     * Resolves officer identity (supports NIP, Name, or combined "Nama, NIP"), checks Cloud Firestore
     * (`Source.SERVER`), and establishes the Ruang Kerja session without ever storing or resetting PINs locally.
     * If this device already has a previously verified `saved_nip` matching the officer, hardware-verified
     * biometrics still unlock gracefully when field internet signal is momentarily unavailable.
     */
    suspend fun authenticateWithBiometric(nipOrName: String): Result<OfficerCredentialEntity> = withContext(Dispatchers.IO) {
        val rawInput = nipOrName.trim()
        if (rawInput.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Identitas petugas tidak ditemukan. Masukkan Nama/NIP atau login dengan PIN terlebih dahulu."))
        }

        val resolvedNip = resolveNipFromDirectoryOrInput(rawInput) ?: rawInput.replace(Regex("[\\s\\-]+"), "")
        val savedNipOnDevice = officerAuthManager.getSavedNip().trim().replace(Regex("[\\s\\-]+"), "")
        val isPreviouslyVerifiedOnThisDevice = savedNipOnDevice.isNotBlank() &&
            savedNipOnDevice.equals(resolvedNip, ignoreCase = true)

        val cloudLookup = FirestoreHelper.fetchOfficerCredentialStrictFromServer(resolvedNip, context)
        val foundOwnerUid = (cloudLookup as? CloudCredentialLookupResult.Found)?.ownerUid
        val activeCred = when (cloudLookup) {
            is CloudCredentialLookupResult.Found -> cloudLookup.credential
            is CloudCredentialLookupResult.NotFound -> {
                val registered = findRegisteredOfficerMetadata(resolvedNip)
                if (registered != null) {
                    OfficerCredentialEntity(
                        nip = registered.nip,
                        fullName = registered.fullName,
                        role = registered.role,
                        unit = registered.unit,
                        shift = registered.shift,
                        phone = registered.phone,
                        pinHash = "",
                        salt = "",
                        isCustomPinSet = officerAuthManager.hasCustomPin(),
                        initialSecurePin = "",
                        isSyncedWithCloud = false
                    )
                } else {
                    return@withContext Result.failure(
                        IllegalArgumentException("Petugas dengan NIP '$resolvedNip' tidak ditemukan di server Firebase.")
                    )
                }
            }
            is CloudCredentialLookupResult.Error -> {
                if (isPreviouslyVerifiedOnThisDevice) {
                    val registered = findRegisteredOfficerMetadata(resolvedNip)
                    val fallbackName = officerAuthManager.getOfficerName().ifBlank { registered?.fullName ?: "Petugas Pusdalops" }
                    val fallbackRole = officerAuthManager.getOfficerRole().ifBlank { registered?.role ?: "Operator Pusdalops" }
                    val fallbackShift = officerAuthManager.getOfficerShift().ifBlank { registered?.shift ?: "Shift Siaga 24 Jam" }
                    OfficerCredentialEntity(
                        nip = resolvedNip,
                        fullName = fallbackName,
                        role = fallbackRole,
                        unit = registered?.unit ?: "Pusdalops BPBD Kab. Mukomuko",
                        shift = fallbackShift,
                        phone = registered?.phone ?: "",
                        pinHash = "",
                        salt = "",
                        isCustomPinSet = officerAuthManager.hasCustomPin(),
                        initialSecurePin = "",
                        isSyncedWithCloud = false
                    )
                } else {
                    return@withContext Result.failure(IllegalStateException(cloudLookup.userMessage, cloudLookup.cause))
                }
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
        safeCacheMetadata(sanitizedMetadata)
        activeCredential = sanitizedMetadata

        if (activeCred.pinHash.isNotBlank() && activeCred.salt.isNotBlank()) {
            FirestoreHelper.syncCredentialToCloud(
                credential = activeCred.copy(
                    lastLoginAt = now,
                    lastLoginDevice = deviceDesc,
                    initialSecurePin = "",
                    isSyncedWithCloud = true,
                    updatedAt = now
                ),
                context = context,
                preserveOwnerUid = foundOwnerUid
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

        safeInsertAuditLog(
            PersonnelAuditLogEntity(
                nip = activeCred.nip,
                name = activeCred.fullName,
                role = activeCred.role,
                shift = activeCred.shift,
                action = "LOGIN_SUCCESSFUL_BIOMETRIC_AUTH",
                deviceInfo = "Login Biometrik berhasil di $deviceDesc"
            )
        )

        // Sinkronkan aktivitas login Biometrik ke daftar Keamanan Perangkat & Sesi (Play Integrity Classic)
        try {
            com.example.security.SecurityManager.getInstance(context).verifyDeviceAuthenticityDuringLogin(
                officerNip = activeCred.nip,
                officerName = activeCred.fullName,
                officerRole = activeCred.role,
                loginMethod = "BIOMETRIK",
                loginAction = "LOGIN_SUCCESSFUL_BIOMETRIC_AUTH",
                detail = "Login Biometrik berhasil di $deviceDesc"
            )
        } catch (e: Exception) {
            Log.w(TAG, "Non-fatal Play Integrity session sync warning after biometric login: ${e.message}")
        }

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

        // REQUIRE Cloud Firestore persistence to succeed (full document write removes any legacy plaintext `pin` field)
        // so the custom PIN is permanent across app updates and reinstalls!
        val cloudSyncResult = FirestoreHelper.syncCredentialToCloudSuspend(
            credential = cloudPayload,
            context = context,
            preserveOwnerUid = foundCloudRecord.ownerUid,
            overwriteFullDocument = true
        )
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
        safeCacheMetadata(sanitizedLocal)
        activeCredential = sanitizedLocal
        officerAuthManager.markCloudCustomPinStatus(true)

        safeInsertAuditLog(
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
     * Normalizes an official officer account ("HITATUN ARAZAK" / NIP 197401282006041006 or any official ASN)
     * back to a brand-new, never-logged-in state:
     * 1. Purges all login & failed-attempt audit logs in local Room (`personnel_audit_logs`) and clears in-memory lockout state.
     * 2. Purges local cached credential row (`officer_credentials`), Play Integrity device binding, and `device_security_sessions` in Room/SharedPreferences & Cloud Firestore.
     * 3. Clears `saved_nip` and active session if currently set to this NIP so the officer is treated as never having logged in.
     * 4. Resets the PIN in Cloud Firestore (`officer_credentials/{nip}`) to the salted PBKDF2 hash of the official initial 6-digit NIP code (e.g. `041006` for HITATUN ARAZAK) with `isCustomPinSet = false`, `isPinChanged = false`, `lastLoginAt = 0L`, `lastLoginDevice = ""` (overwriting any stale Console fields).
     * 5. Does NOT insert any new audit log entry so the account remains completely clean.
     */
    suspend fun normalizeAccountToBrandNewInitialState(
        nip: String = "197401282006041006",
        syncToCloud: Boolean = true
    ): Result<String> = withContext(Dispatchers.IO) {
        val cleanNip = nip.trim().replace(Regex("[\\s\\-]+"), "")
        if (cleanNip.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("NIP petugas tidak boleh kosong."))
        }

        val officialProfile = findRegisteredOfficerMetadata(cleanNip)
        val canonicalName = officialProfile?.fullName ?: if (cleanNip == "197401282006041006") "HITATUN ARAZAK" else "Petugas $cleanNip"
        val canonicalRole = officialProfile?.role ?: if (cleanNip == "197401282006041006") com.example.data.local.OfficerRoles.MANAJER else com.example.data.local.OfficerRoles.OPERATOR
        val canonicalUnit = officialProfile?.unit ?: "Pusdalops BPBD Kab. Mukomuko"
        val canonicalShift = officialProfile?.shift ?: if (cleanNip == "197401282006041006") "Manajerial 24 Jam" else "Shift Siaga 24 Jam"
        val canonicalPhone = officialProfile?.phone.orEmpty()

        // 1. Clear in-memory failed login attempts / lockout state
        failedAttemptMap.remove(cleanNip.lowercase())
        if (activeCredential?.nip?.equals(cleanNip, ignoreCase = true) == true) {
            activeCredential = null
        }

        // 2. Purge all local Room audit logs & cached credential metadata for this officer (treated as never logged in)
        try {
            val nameKeyword = canonicalName.substringBefore(",").trim().ifBlank { cleanNip }
            auditDao.deleteAuditLogsByNipOrName(cleanNip, nameKeyword)
        } catch (e: Exception) {
            Log.w(TAG, "Non-fatal warning clearing local audit logs for $cleanNip: ${e.message}")
        }
        try {
            credentialDao.deleteByNip(cleanNip)
        } catch (e: Exception) {
            Log.w(TAG, "Non-fatal warning deleting local cached credential for $cleanNip: ${e.message}")
        }

        // 3. Clear local Play Integrity device binding, SecurityManager session & login activities, and saved_nip if belonging to this officer
        try {
            com.example.security.PlayIntegritySecurityManager.getInstance(context)
                .clearDeviceBindingForOfficer(cleanNip)
            com.example.security.SecurityManager.getInstance(context)
                .deleteSecuritySession(cleanNip, purgeActivities = true)
        } catch (e: Exception) {
            Log.w(TAG, "Non-fatal warning clearing security session for $cleanNip: ${e.message}")
        }

        val currentSavedNip = officerAuthManager.getSavedNip()
        if (currentSavedNip.equals(cleanNip, ignoreCase = true)) {
            officerAuthManager.clearSavedOfficerIdentity()
            officerAuthManager.markCloudCustomPinStatus(false)
        }

        // 4. Reset Cloud Firestore credential to brand-new initial 6-digit NIP PIN hash with zero login history
        val initialPin = OfficerCredentialEntity.generateInitialPinForNip(cleanNip)
        val salt = OfficerCredentialEntity.generateCryptographicSalt()
        val hash = OfficerCredentialEntity.hashWithSalt(initialPin, salt)
        val now = System.currentTimeMillis()

        if (syncToCloud) {
            val cloudLookup = FirestoreHelper.fetchOfficerCredentialStrictFromServer(cleanNip, context)
            val existingCred = (cloudLookup as? CloudCredentialLookupResult.Found)?.credential
            val existingOwnerUid = (cloudLookup as? CloudCredentialLookupResult.Found)?.ownerUid

            if (cloudLookup is CloudCredentialLookupResult.Error) {
                return@withContext Result.failure(
                    IllegalStateException(cloudLookup.userMessage, cloudLookup.cause)
                )
            }

            val freshCloudCredential = OfficerCredentialEntity(
                nip = cleanNip,
                fullName = existingCred?.fullName?.takeIf { it.isNotBlank() } ?: canonicalName,
                role = existingCred?.role?.takeIf { it.isNotBlank() } ?: canonicalRole,
                unit = existingCred?.unit?.takeIf { it.isNotBlank() } ?: canonicalUnit,
                shift = existingCred?.shift?.takeIf { it.isNotBlank() } ?: canonicalShift,
                phone = existingCred?.phone?.takeIf { it.isNotBlank() } ?: canonicalPhone,
                pinHash = hash,
                salt = salt,
                isCustomPinSet = false,
                initialSecurePin = "",
                lastLoginAt = 0L,
                lastLoginDevice = "",
                isSyncedWithCloud = true,
                createdAt = now,
                updatedAt = now
            )

            val syncResult = FirestoreHelper.syncCredentialToCloudSuspend(
                credential = freshCloudCredential,
                context = context,
                preserveOwnerUid = existingOwnerUid,
                overwriteFullDocument = true
            )
            if (syncResult.isFailure) {
                return@withContext Result.failure(
                    IllegalStateException(
                        syncResult.exceptionOrNull()?.message
                            ?: "Gagal menormalkan kredensial di server Firebase. Periksa koneksi internet.",
                        syncResult.exceptionOrNull()
                    )
                )
            }

            // 5. Purge Cloud simulation traces (device_security_sessions & admin_access_audit_logs)
            FirestoreHelper.purgeOfficerCloudLoginTracesSuspend(cleanNip, context)
        }

        return@withContext Result.success(
            "Akun $canonicalName (NIP: $cleanNip) berhasil dinormalkan seperti baru (belum pernah login), seluruh riwayat upaya login dihapus, dan PIN di-reset ke PIN awal resmi ($initialPin)."
        )
    }

    // Catatan Keamanan: Normalisasi / reset PIN otomatis saat startup atau fresh install
    // DIHAPUS secara permanen agar aplikasi yang di-update atau di-install ulang TIDAK PERNAH
    // mengembalikan PIN petugas di Cloud Firestore ke PIN default atau menghapus `saved_nip`.

    /**
     * Admin-only PIN reset & full account normalization via Cloud Firestore.
     * Purges all login attempts/history in local & Cloud databases, resets the account to a brand-new
     * never-logged-in state (`lastLoginAt = 0L`, `lastLoginDevice = ""`, `isCustomPinSet = false`),
     * and sets the salted PBKDF2 hash of the officer's initial 6-digit NIP PIN in Cloud Firestore.
     */
    suspend fun resetToInitialNipSecret(nip: String): Result<String> = withContext(Dispatchers.IO) {
        return@withContext normalizeAccountToBrandNewInitialState(
            nip = nip.trim(),
            syncToCloud = true
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
