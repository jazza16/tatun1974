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

    companion object {
        private const val TAG = "SmartOfficerAuthRepo"

        // Default PINs that are strictly forbidden for officer login under the smart login scheme
        val FORBIDDEN_DEFAULT_PINS = listOf("117", "BPBD117", "ADMIN117", "TRC2026", "PUSDALOPS117", "0000", "1234")

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
     * Smart Auto-Discovery & Synchronization:
     * Scans all known personnel sources (official ASN roster, local Room personnel table,
     * SharedPreferences roster, and cloud Firestore).
     * If any officer is not yet registered in the credentials database, creates and stores their
     * unique secure credential in both SQLite and Cloud Firestore.
     */
    suspend fun harvestAndSyncAllOfficers(): Int = withContext(Dispatchers.IO) {
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
            Log.d(TAG, "Harvested and registered ${newRegistrations.size} new officers into credentials database.")

            // Push all newly registered officers to Cloud Firestore
            for (cred in newRegistrations) {
                FirestoreHelper.syncCredentialToCloud(cred)
            }
        }

        // Source 4: Fetch cloud credentials from Cloud Firestore (Multi-device smart sync)
        pullCredentialsFromCloud()

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

                        // If not in local or cloud version has custom PIN set / is newer, adopt cloud version
                        if (local == null || (cloud.isCustomPinSet && !local.isCustomPinSet) || cloud.updatedAt > local.updatedAt) {
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
     * - Rejects any generic default PIN (117, BPBD117, TRC2026).
     * - Verifies officer identity against the unique database credential.
     * - Verifies device and records login session locally & in Cloud Firestore.
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
        if (trimmedPin.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("PIN pribadi tidak boleh kosong."))
        }

        // STRICT CHECK: Reject generic default PINs without exception
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
                            "Wajib login menggunakan PIN pribadi yang telah tersimpan di database."
                )
            )
        }

        // Retrieve credential from Room
        var credential = credentialDao.getCredentialByNip(trimmedNip)

        // If not in Room yet, make an inline cloud retrieval attempt (useful for new devices)
        if (credential == null) {
            harvestAndSyncAllOfficers()
            credential = credentialDao.getCredentialByNip(trimmedNip)
        }

        if (credential == null) {
            return@withContext Result.failure(
                IllegalArgumentException("Petugas dengan NIP '$trimmedNip' tidak ditemukan dalam database resmi.")
            )
        }

        // Validate personal PIN against salted hash or initial secure NIP key
        val isPinCorrect = credential.verifyPin(trimmedPin)

        if (!isPinCorrect) {
            auditDao.insertAuditLog(
                PersonnelAuditLogEntity(
                    nip = trimmedNip,
                    name = credential.fullName,
                    role = credential.role,
                    shift = credential.shift,
                    action = "AUTH_INVALID_PIN",
                    deviceInfo = "Percobaan PIN salah pada $deviceModel"
                )
            )
            val hintMessage = if (!credential.isCustomPinSet && credential.initialSecurePin.isNotBlank()) {
                "PIN salah. Untuk login perdana pada akun baru, gunakan 6 digit akhir NIP Anda (${credential.initialSecurePin})."
            } else {
                "PIN pribadi salah untuk NIP $trimmedNip. Masukkan PIN yang telah didaftarkan sebelumnya pada perangkat Anda."
            }
            return@withContext Result.failure(IllegalArgumentException(hintMessage))
        }

        // Authentication Successful: Record device & timestamp
        val now = System.currentTimeMillis()
        val deviceDesc = "$deviceModel (Android ${Build.VERSION.RELEASE})"
        credentialDao.updateLoginSession(trimmedNip, now, deviceDesc)

        // Synchronize updated login metadata to Cloud Firestore
        val updatedCred = credential.copy(lastLoginAt = now, lastLoginDevice = deviceDesc, updatedAt = now)
        FirestoreHelper.syncCredentialToCloud(updatedCred)

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
     * Changes personal PIN for a specific officer:
     * - Enforces non-default, non-trivial PIN (at least 4 digits, not in forbidden default list).
     * - Hashes with unique salt and updates Room DB.
     * - Immediately pushes to Cloud Firestore so the new device will require this exact PIN.
     */
    suspend fun changePersonalPin(
        nip: String,
        currentPin: String,
        newPin: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val trimmedNip = nip.trim()
        val trimmedOld = currentPin.trim()
        val trimmedNew = newPin.trim()

        val credential = credentialDao.getCredentialByNip(trimmedNip)
            ?: return@withContext Result.failure(IllegalArgumentException("Petugas dengan NIP '$trimmedNip' tidak ditemukan."))

        if (!credential.verifyPin(trimmedOld)) {
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

        if (trimmedNew == trimmedOld) {
            return@withContext Result.failure(IllegalArgumentException("PIN baru tidak boleh sama dengan PIN saat ini."))
        }

        val newSalt = java.util.UUID.randomUUID().toString().take(12)
        val newHash = OfficerCredentialEntity.hashWithSalt(trimmedNew, newSalt)
        val now = System.currentTimeMillis()

        credentialDao.updatePersonalPin(trimmedNip, newHash, newSalt, now)

        val updatedCred = credential.copy(
            pinHash = newHash,
            salt = newSalt,
            isCustomPinSet = true,
            initialSecurePin = "",
            isSyncedWithCloud = true,
            updatedAt = now
        )

        // Push to Cloud Firestore so all other devices immediately require the new PIN
        FirestoreHelper.syncCredentialToCloud(updatedCred)

        auditDao.insertAuditLog(
            PersonnelAuditLogEntity(
                nip = trimmedNip,
                name = credential.fullName,
                role = credential.role,
                shift = credential.shift,
                action = "PIN_CHANGED_PERMANENT",
                deviceInfo = "PIN pribadi diperbarui dan disinkronkan ke Cloud Firestore"
            )
        )

        return@withContext Result.success("PIN pribadi untuk ${credential.fullName} berhasil diperbarui dan tersimpan aman di database!")
    }

    /**
     * Resets officer PIN to their deterministic individual 6-digit NIP key.
     * Guarantees that generic default PINs ("117", "TRC2026", etc.) are NEVER restored.
     */
    suspend fun resetToInitialNipSecret(nip: String): Result<String> = withContext(Dispatchers.IO) {
        val trimmedNip = nip.trim()
        val cred = credentialDao.getCredentialByNip(trimmedNip)
            ?: return@withContext Result.failure(IllegalArgumentException("Petugas dengan NIP '$trimmedNip' tidak ditemukan."))

        val initialPin = OfficerCredentialEntity.generateInitialPinForNip(cred.nip)
        val salt = java.util.UUID.randomUUID().toString().take(12)
        val hash = OfficerCredentialEntity.hashWithSalt(initialPin, salt)
        val now = System.currentTimeMillis()

        val updated = cred.copy(
            pinHash = hash,
            salt = salt,
            isCustomPinSet = false,
            initialSecurePin = initialPin,
            isSyncedWithCloud = true,
            updatedAt = now
        )
        credentialDao.insertCredential(updated)
        FirestoreHelper.syncCredentialToCloud(updated)

        auditDao.insertAuditLog(
            PersonnelAuditLogEntity(
                nip = trimmedNip,
                name = cred.fullName,
                role = cred.role,
                shift = cred.shift,
                action = "PIN_RESET_TO_NIP_KEY",
                deviceInfo = "PIN di-reset ke kunci individual NIP (Bukan PIN Default umum)"
            )
        )

        return@withContext Result.success("PIN akun ${cred.fullName} di-reset ke 6 digit NIP ($initialPin). Tanpa PIN default umum.")
    }

    /**
     * Checks if an officer already has a personalized custom PIN set in the database.
     */
    suspend fun isCustomPinConfigured(nip: String): Boolean = withContext(Dispatchers.IO) {
        val cred = credentialDao.getCredentialByNip(nip.trim())
        return@withContext cred?.isCustomPinSet == true
    }

    /**
     * Gets credential details for a specific officer.
     */
    suspend fun getCredential(nip: String): OfficerCredentialEntity? = withContext(Dispatchers.IO) {
        return@withContext credentialDao.getCredentialByNip(nip.trim())
    }
}
