package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.security.SecureStorageHelper
import com.example.security.SecurityManager
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Data model for Duty Officers, Operators, and Field Responders
 * at Pusdalops BPBD Kabupaten Mukomuko / Tim Reaksi Cepat (TRC).
 */
data class OfficerProfile(
    val id: String = UUID.randomUUID().toString(),
    val fullName: String,       // Nama Lengkap beserta Gelar ASN / TRC
    val nip: String,            // NIP Pegawai ASN (18 digit) atau No. Registrasi Personil TRC BPBD
    val role: String,           // "Operator Pusdalops", "Petugas Jaga Siaga", "Petugas Lapangan TRC"
    val unit: String = "Pusdalops BPBD Kab. Mukomuko",
    val shift: String = "Shift Siaga 24 Jam",
    val phone: String = ""
) {
    /**
     * Standard formatted display label combining Nama and NIP ASN/TRC
     */
    fun formattedLabel(): String {
        val nipPrefix = if (nip.startsWith("TRC", ignoreCase = true)) "Reg TRC" else "NIP"
        return "$fullName ($nipPrefix: $nip)"
    }

    fun formattedSummary(): String {
        return "$fullName • $nip • $role"
    }
}

object OfficerRoles {
    const val SUPER_ADMIN = "SuperAdmin"
    const val RBAC_OPERATOR = "Operator"
    const val PENGAWAS = "Pengawas BPBD"
    const val MANAJER = "Manajer Pusdalops-PB"
    const val SUPERVISOR = "Supervisor BPBD"
    const val KOORD_ADMINISTRASI = "Koord. Administrasi"
    const val OPERATOR = "Operator Pusdalops"
    const val PETUGAS_JAGA = "Petugas Jaga Siaga / Piket"
    const val PETUGAS_LAPANGAN = "Petugas Lapangan TRC / Logistik"

    val ALL_ROLES = listOf(SUPER_ADMIN, RBAC_OPERATOR, PENGAWAS, MANAJER, SUPERVISOR, KOORD_ADMINISTRASI, OPERATOR, PETUGAS_JAGA, PETUGAS_LAPANGAN)

    /**
     * Official hierarchy rank for duty personnel list.
     * Manajer Pusdalops is positioned immediately below Pengawas.
     */
    fun getRoleRank(role: String): Int = when {
        role.equals(SUPER_ADMIN, ignoreCase = true) -> 0
        role.contains("Pengawas", ignoreCase = true) -> 1
        role.contains("Manajer", ignoreCase = true) || role.contains("Manager", ignoreCase = true) -> 2
        role.contains("Supervisor", ignoreCase = true) -> 3
        role.contains("Koord", ignoreCase = true) || role.contains("Administrasi", ignoreCase = true) -> 4
        role.contains("Operator", ignoreCase = true) -> 5
        role.contains("Piket", ignoreCase = true) || role.contains("Jaga", ignoreCase = true) -> 6
        role.contains("Lapangan", ignoreCase = true) || role.contains("Logistik", ignoreCase = true) || role.contains("TRC", ignoreCase = true) -> 7
        else -> 8
    }
}

/**
 * Manages authentication, duty personnel roster, and verification for Petugas Jaga (Duty Officers),
 * Operator Pusdalops, and Petugas Lapangan TRC BPBD Kabupaten Mukomuko.
 */
class OfficerAuthManager(private val context: Context) {

    private val sharedPrefs: SharedPreferences = context.getSharedPreferences(
        "bpbd_officer_auth_prefs",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val PREF_IS_OFFICER_LOGGED_IN = "is_officer_logged_in"
        private const val PREF_OFFICER_NAME = "officer_name"
        private const val PREF_OFFICER_BADGE_ID = "officer_badge_id"
        private const val PREF_OFFICER_ROLE = "officer_role"
        private const val PREF_OFFICER_SHIFT = "officer_shift"
        private const val PREF_SAVED_NIP = "saved_nip"
        private const val PREF_HAS_CLOUD_CUSTOM_PIN = "has_cloud_custom_pin"
        private const val PREF_STORAGE_SCHEMA_VERSION = "officer_storage_schema_version"
        private const val CURRENT_STORAGE_SCHEMA_VERSION = 3

        // Legacy local PIN & obsolete startup-reset keys: NEVER written anymore; purged non-destructively on upgrade
        private const val PREF_OFFICER_PIN = "officer_pin"
        private const val PREF_OFFICER_PIN_HASH = "officer_pin_hash"
        private const val PREF_OFFICER_PIN_SALT = "officer_pin_salt"
        private const val PREF_LEGACY_SIM_CLEAN_LOCAL = "hitatun_arazak_normalized_v1_clean"
        private const val PREF_LEGACY_SIM_CLEAN_CLOUD = "hitatun_arazak_normalized_v1_cloud"
        private const val PREF_OFFICER_ROSTER_JSON = "officer_roster_json"

        private const val PREF_IS_ADMIN_LOGGED_IN = "is_admin_logged_in"
        private const val PREF_ADMIN_PASSWORD = "admin_password"
        private const val PREF_ADMIN_PASSWORD_HASH = "admin_password_hash"
        private const val PREF_ADMIN_PASSWORD_SALT = "admin_password_salt"
        private const val PREF_ADMIN_FAILED_ATTEMPTS = "admin_failed_attempts"
        private const val PREF_ADMIN_LOCKOUT_UNTIL = "admin_lockout_until"
        const val DEFAULT_ADMIN_USERNAME = "admin"
        private const val MAX_ADMIN_ATTEMPTS = 5
        private const val ADMIN_LOCKOUT_MS = 5 * 60 * 1000L

        private val FORBIDDEN_ADMIN_PASSWORDS = setOf(
            "sipagarkita2026", "admin", "admin123", "123456", "12345678", "password", "bpbd117"
        )

        // Forbidden default PINs that must NEVER be accepted or auto-seeded
        val FORBIDDEN_DEFAULT_PINS = listOf("117", "BPBD117", "ADMIN117", "TRC2026", "PUSDALOPS117", "0000", "1234")

        // Roster is pre-populated with official ASN BPBD Mukomuko Pusdalops operators
        val DEFAULT_ROSTER: List<OfficerProfile> = OfficialAsnBpbdPersonnel.toOfficerProfiles()
    }

    init {
        ensureUpdateSafeStorageMigration()
    }

    /**
     * Update-safe non-destructive migration for SharedPreferences (`bpbd_officer_auth_prefs`):
     * - NEVER calls `clear()` and NEVER overwrites/erases `saved_nip`, `is_officer_logged_in`,
     *   or officer profile metadata during an app update.
     * - Backfills `saved_nip` from `officer_badge_id` or `PusdalopsSession` if upgrading from an older version.
     * - Purges ONLY legacy local PIN storage keys (`officer_pin`, `officer_pin_hash`, `officer_pin_salt`)
     *   and obsolete simulation flags so raw/hashed PINs are NEVER stored on the device.
     */
    private fun ensureUpdateSafeStorageMigration() {
        val currentVersion = sharedPrefs.getInt(PREF_STORAGE_SCHEMA_VERSION, 0)
        val hasLegacyPinKeys = sharedPrefs.contains(PREF_OFFICER_PIN) ||
            sharedPrefs.contains(PREF_OFFICER_PIN_HASH) ||
            sharedPrefs.contains(PREF_OFFICER_PIN_SALT) ||
            sharedPrefs.contains(PREF_LEGACY_SIM_CLEAN_LOCAL) ||
            sharedPrefs.contains(PREF_LEGACY_SIM_CLEAN_CLOUD)

        if (currentVersion >= CURRENT_STORAGE_SCHEMA_VERSION && !hasLegacyPinKeys) {
            return
        }

        val editor = sharedPrefs.edit()
        // If older build stored a custom PIN hash locally, preserve only the non-secret boolean status
        if (sharedPrefs.contains(PREF_OFFICER_PIN_HASH) && !sharedPrefs.contains(PREF_HAS_CLOUD_CUSTOM_PIN)) {
            editor.putBoolean(PREF_HAS_CLOUD_CUSTOM_PIN, true)
        }
        // Purge ONLY legacy PIN secrets & obsolete flags — strictly preserve saved_nip and officer session!
        editor.remove(PREF_OFFICER_PIN)
            .remove(PREF_OFFICER_PIN_HASH)
            .remove(PREF_OFFICER_PIN_SALT)
            .remove(PREF_LEGACY_SIM_CLEAN_LOCAL)
            .remove(PREF_LEGACY_SIM_CLEAN_CLOUD)

        // Backfill saved_nip non-destructively if upgrading from older version that only saved officer_badge_id
        // or PusdalopsSession NIP
        val existingSavedNip = SecureStorageHelper.getDecrypted(sharedPrefs, PREF_SAVED_NIP)?.trim().orEmpty()
        if (existingSavedNip.isBlank()) {
            val legacyBadgeNip = SecureStorageHelper.getDecrypted(sharedPrefs, PREF_OFFICER_BADGE_ID)?.trim().orEmpty()
            val candidateNip = if (legacyBadgeNip.isNotBlank() && !legacyBadgeNip.equals("PUSDALOPS-117", ignoreCase = true)) {
                legacyBadgeNip
            } else {
                val sessionPrefs = context.getSharedPreferences("PusdalopsSession", Context.MODE_PRIVATE)
                val sessionNip = SecureStorageHelper.getDecrypted(sessionPrefs, "nip_petugas")?.trim().orEmpty()
                if (sessionNip.isNotBlank() && !sessionNip.equals("PUSDALOPS-117", ignoreCase = true)) sessionNip else ""
            }
            if (candidateNip.isNotBlank()) {
                SecureStorageHelper.putEncrypted(editor, PREF_SAVED_NIP, candidateNip)
            }
        }

        editor.putInt(PREF_STORAGE_SCHEMA_VERSION, CURRENT_STORAGE_SCHEMA_VERSION)
        editor.apply()
    }

    fun isOfficerLoggedIn(): Boolean {
        return sharedPrefs.getBoolean(PREF_IS_OFFICER_LOGGED_IN, false)
    }

    /**
     * Reads `saved_nip` from encrypted Secure Storage (Android Keystore AES-256-GCM).
     */
    fun getSavedNip(): String {
        val secureNip = SecureStorageHelper.getDecrypted(sharedPrefs, PREF_SAVED_NIP)?.trim().orEmpty()
        if (secureNip.isNotBlank()) return secureNip
        val badgeNip = SecureStorageHelper.getDecrypted(sharedPrefs, PREF_OFFICER_BADGE_ID)?.trim().orEmpty()
        if (badgeNip.isNotBlank()) return badgeNip
        val storedName = SecureStorageHelper.getDecrypted(sharedPrefs, PREF_OFFICER_NAME)?.trim().orEmpty()
        if (storedName.isNotBlank()) {
            val matched = findOfficerInRoster(rawName = storedName, rawNip = "")
            if (matched != null) return matched.nip
        }
        return ""
    }

    /**
     * Formats the saved officer identity for Skenario B ("Masuk sebagai: [Nama Petugas/NIP]").
     */
    fun getSavedOfficerDisplay(): String {
        val nip = getSavedNip()
        val name = getOfficerName().trim()
        return when {
            name.isNotBlank() && nip.isNotBlank() -> "$name / $nip"
            nip.isNotBlank() -> nip
            name.isNotBlank() -> name
            else -> ""
        }
    }

    /**
     * Stores `saved_nip` and officer identity encrypted inside Secure Storage (AES-256-GCM),
     * automatically synchronizing canonical Name and NIP from the officer roster.
     */
    fun saveNipToSecureStorage(
        nip: String,
        fullName: String = "",
        role: String = OfficerRoles.RBAC_OPERATOR,
        shift: String = "Shift Siaga 24 Jam"
    ) {
        val matched = findOfficerInRoster(rawName = fullName, rawNip = nip)
        val cleanNip = matched?.nip ?: nip.trim()
        val cleanName = fullName.trim().ifBlank { matched?.fullName.orEmpty() }
        val cleanRole = role.trim().ifBlank { OfficerRoles.RBAC_OPERATOR }
        val cleanShift = shift.trim().ifBlank { matched?.shift ?: "Shift Siaga 24 Jam" }
        if (cleanNip.isBlank()) return
        val editor = sharedPrefs.edit()
        SecureStorageHelper.putEncrypted(editor, PREF_SAVED_NIP, cleanNip)
        SecureStorageHelper.putEncrypted(editor, PREF_OFFICER_BADGE_ID, cleanNip)
        if (cleanName.isNotBlank()) {
            SecureStorageHelper.putEncrypted(editor, PREF_OFFICER_NAME, cleanName)
        }
        SecureStorageHelper.putEncrypted(editor, PREF_OFFICER_ROLE, cleanRole)
        if (cleanShift.isNotBlank()) {
            SecureStorageHelper.putEncrypted(editor, PREF_OFFICER_SHIFT, cleanShift)
        }
        editor.apply()
    }

    /**
     * Clears `saved_nip` from Secure Storage when "Bukan Anda? Ganti Akun" is clicked.
     */
    fun clearSavedNipFromSecureStorage() {
        clearSavedOfficerIdentity()
    }

    fun getOfficerName(): String {
        val rawName = SecureStorageHelper.getDecrypted(sharedPrefs, PREF_OFFICER_NAME)?.trim().orEmpty()
        if (rawName.isNotBlank()) return rawName
        val rawNip = SecureStorageHelper.getDecrypted(sharedPrefs, PREF_SAVED_NIP)?.trim().orEmpty()
            .ifBlank { SecureStorageHelper.getDecrypted(sharedPrefs, PREF_OFFICER_BADGE_ID)?.trim().orEmpty() }
        val matched = findOfficerInRoster(rawName = rawName, rawNip = rawNip)
        return matched?.fullName ?: rawName
    }

    fun getOfficerBadgeId(): String {
        val rawNip = SecureStorageHelper.getDecrypted(sharedPrefs, PREF_OFFICER_BADGE_ID)?.trim().orEmpty()
            .ifBlank { SecureStorageHelper.getDecrypted(sharedPrefs, PREF_SAVED_NIP)?.trim().orEmpty() }
        if (rawNip.isNotBlank()) return rawNip
        val rawName = SecureStorageHelper.getDecrypted(sharedPrefs, PREF_OFFICER_NAME)?.trim().orEmpty()
        val matched = findOfficerInRoster(rawName = rawName, rawNip = rawNip)
        return matched?.nip ?: rawNip
    }

    /**
     * Reads the raw stored `role` string directly from SharedPreferences / SecureStorage
     * without consulting any hardcoded local roster.
     */
    fun getRawStoredRoleFromPrefs(): String {
        return SecureStorageHelper.getDecrypted(sharedPrefs, PREF_OFFICER_ROLE)?.trim().orEmpty()
    }

    /**
     * Returns the stored officer role from SharedPreferences / SecureStorage.
     * Defaults to 'Operator' if no role is stored. Never looks up hardcoded NIP/Name roles.
     */
    fun getOfficerRole(): String {
        val rawRole = getRawStoredRoleFromPrefs()
        return rawRole.ifBlank { OfficerRoles.RBAC_OPERATOR }
    }

    /**
     * Returns the canonical RBAC role ('SuperAdmin' or 'Operator') based dynamically on the stored role.
     */
    fun getRbacRole(): String {
        return com.example.security.OfficerRbacManager.normalizeDynamicRole(getRawStoredRoleFromPrefs())
    }

    /**
     * Returns true if the stored officer role is 'SuperAdmin'.
     */
    fun isSuperAdmin(): Boolean {
        return com.example.security.OfficerRbacManager.isSuperAdminRole(getRawStoredRoleFromPrefs())
    }

    fun getOfficerShift(): String {
        return SecureStorageHelper.getDecrypted(sharedPrefs, PREF_OFFICER_SHIFT) ?: "Shift Siaga 24 Jam"
    }

    fun getOfficerFormattedTitle(): String {
        val name = getOfficerName()
        val nip = getOfficerBadgeId()
        val role = getOfficerRole()
        if (name.isBlank()) return "Petugas Pusdalops Terverifikasi"
        val nipPrefix = if (nip.startsWith("TRC", ignoreCase = true)) "Reg TRC" else "NIP"
        return if (nip.isNotBlank()) "$name ($nipPrefix: $nip) • $role" else "$name • $role"
    }

    /**
     * Searches the active roster (and official 34 ASN BPBD database) by NIP or Name.
     */
    fun findOfficerInRoster(rawName: String, rawNip: String = ""): OfficerProfile? {
        val (cleanName, embeddedNip) = OfficialAsnBpbdPersonnel.extractCleanNameAndNip(rawName)
        val candidateNip = rawNip.trim().takeIf { it.isNotBlank() && it != "-" } ?: embeddedNip
        val candidateName = cleanName.ifBlank { rawName.trim() }

        if (candidateNip.isBlank() && (candidateName.isBlank() || candidateName == "-")) {
            return null
        }

        val roster = getOfficerRoster()
        if (candidateNip.isNotBlank()) {
            val byNip = roster.firstOrNull { it.nip.equals(candidateNip, ignoreCase = true) }
            if (byNip != null) return byNip
        }
        if (candidateName.isNotBlank()) {
            val byExactName = roster.firstOrNull { it.fullName.equals(candidateName, ignoreCase = true) }
            if (byExactName != null) return byExactName

            val baseCandidate = candidateName.substringBefore(",").trim()
            if (baseCandidate.length >= 4) {
                val byBaseName = roster.firstOrNull {
                    it.fullName.substringBefore(",").trim().equals(baseCandidate, ignoreCase = true) ||
                        it.fullName.startsWith(baseCandidate, ignoreCase = true)
                }
                if (byBaseName != null) return byBaseName
            }
        }

        val officialMatch = OfficialAsnBpbdPersonnel.findByNipOrName(candidateNip.ifBlank { candidateName })
            ?: OfficialAsnBpbdPersonnel.findByNipOrName(candidateName)
        return officialMatch?.let {
            OfficerProfile(
                id = "asn-${it.nip}",
                fullName = it.name,
                nip = it.nip,
                role = it.role,
                unit = it.unit,
                shift = it.shift,
                phone = it.phone
            )
        }
    }

    /**
     * Resolves an officer profile for any report, log, or PDF export:
     * 1. Attempts to resolve `rawName` / `rawNip` from the roster.
     * 2. Falls back to the currently logged-in / saved officer in Secure Storage & SessionManager.
     * 3. Falls back to canonical default Manajer Pusdalops-PB ("HITATUN ARAZAK", NIP "197401282006041006").
     */
    fun resolveSyncedOfficer(rawName: String = "", rawNip: String = ""): OfficerProfile {
        val directMatch = findOfficerInRoster(rawName = rawName, rawNip = rawNip)
        if (directMatch != null) return directMatch

        val activeName = getOfficerName()
        val activeNip = getOfficerBadgeId()
        val activeMatch = findOfficerInRoster(rawName = activeName, rawNip = activeNip)
        if (activeMatch != null && (rawName.isBlank() || rawName.equals("Petugas Pusdalops", ignoreCase = true))) {
            return activeMatch
        }

        val session = com.example.util.SessionManager(context)
        val sessionName = session.getNamaPetugas()
        val sessionNip = session.getNipPetugas()
        val sessionMatch = findOfficerInRoster(rawName = sessionName, rawNip = sessionNip)

        val (cleanPassedName, embeddedNip) = OfficialAsnBpbdPersonnel.extractCleanNameAndNip(rawName)
        val effectivePassedNip = rawNip.trim().takeIf { it.isNotBlank() && it != "-" } ?: embeddedNip

        if (cleanPassedName.isNotBlank() && !cleanPassedName.equals("Petugas Pusdalops", ignoreCase = true)) {
            val resolvedOfficial = OfficialAsnBpbdPersonnel.resolveCanonicalPersonnel(cleanPassedName, effectivePassedNip)
            return OfficerProfile(
                id = "asn-${resolvedOfficial.nip}",
                fullName = resolvedOfficial.name,
                nip = resolvedOfficial.nip.takeIf { it.isNotBlank() && it != "-" }
                    ?: activeNip.takeIf { it.isNotBlank() && activeName.equals(cleanPassedName, ignoreCase = true) }
                    ?: sessionNip.takeIf { it.isNotBlank() && sessionName.equals(cleanPassedName, ignoreCase = true) }
                    ?: "197401282006041006",
                role = resolvedOfficial.role
            )
        }

        if (activeMatch != null) return activeMatch
        if (sessionMatch != null) return sessionMatch

        val defaultPersonnel = OfficialAsnBpbdPersonnel.resolveCanonicalPersonnel("HITATUN ARAZAK", "197401282006041006")
        return OfficerProfile(
            id = "asn-${defaultPersonnel.nip}",
            fullName = defaultPersonnel.name,
            nip = defaultPersonnel.nip,
            role = defaultPersonnel.role,
            unit = defaultPersonnel.unit,
            shift = defaultPersonnel.shift
        )
    }

    /**
     * Resolves official leadership personnel for signature blocks (Koord. Administrasi, Manajer Pusdalops-PB, Pengawas / Kalaksa)
     * from the live roster so any updates to personnel names or NIPs automatically reflect in PDF signatures.
     */
    fun resolveLeadershipForSignature(
        roleKeyword: String,
        fallbackName: String,
        fallbackNip: String
    ): OfficerProfile {
        val roster = getOfficerRoster()
        val byRole = roster.firstOrNull { it.role.contains(roleKeyword, ignoreCase = true) }
        if (byRole != null) return byRole
        val byNip = roster.firstOrNull { it.nip.equals(fallbackNip, ignoreCase = true) }
        if (byNip != null) return byNip
        val canonical = OfficialAsnBpbdPersonnel.resolveCanonicalPersonnel(fallbackName, fallbackNip)
        return OfficerProfile(
            id = "asn-${canonical.nip}",
            fullName = canonical.name,
            nip = canonical.nip,
            role = canonical.role
        )
    }

    /**
     * Gets the full roster of officers, operators, and field responders.
     * Guarantees all official ASN BPBD Pusdalops personnel are present with synchronized name and NIP.
     */
    fun getOfficerRoster(): List<OfficerProfile> {
        val jsonString = sharedPrefs.getString(PREF_OFFICER_ROSTER_JSON, null)
        val defaultList = OfficialAsnBpbdPersonnel.toOfficerProfiles()
        if (jsonString.isNullOrBlank()) {
            return defaultList
        }
        return try {
            val array = JSONArray(jsonString)
            val list = mutableListOf<OfficerProfile>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val rawFullName = obj.getString("fullName")
                val rawNip = obj.getString("nip")
                val official = OfficialAsnBpbdPersonnel.findByNipOrName(rawNip)
                    ?: OfficialAsnBpbdPersonnel.findByNipOrName(rawFullName)
                list.add(
                    OfficerProfile(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        fullName = official?.name ?: rawFullName,
                        nip = official?.nip ?: rawNip,
                        role = obj.getString("role").ifBlank { official?.role ?: OfficerRoles.OPERATOR },
                        unit = obj.optString("unit", official?.unit ?: "Pusdalops BPBD Kab. Mukomuko"),
                        shift = obj.optString("shift", official?.shift ?: "Shift Siaga 24 Jam"),
                        phone = obj.optString("phone", "")
                    )
                )
            }
            for (asn in defaultList) {
                if (list.none { it.nip.equals(asn.nip, ignoreCase = true) }) {
                    list.add(asn)
                }
            }
            if (list.isEmpty()) defaultList else list
        } catch (e: Exception) {
            defaultList
        }
    }

    private fun saveOfficerRoster(roster: List<OfficerProfile>) {
        val array = JSONArray()
        for (officer in roster) {
            val obj = JSONObject().apply {
                put("id", officer.id)
                put("fullName", officer.fullName)
                put("nip", officer.nip)
                put("role", officer.role)
                put("unit", officer.unit)
                put("shift", officer.shift)
                put("phone", officer.phone)
            }
            array.put(obj)
        }
        sharedPrefs.edit().putString(PREF_OFFICER_ROSTER_JSON, array.toString()).apply()
    }

    /**
     * Adds a new officer with ASN/TRC format into the system roster.
     */
    fun addOfficer(
        fullName: String,
        nip: String,
        role: String,
        shift: String = "Shift Siaga 24 Jam",
        unit: String = "Pusdalops BPBD Kab. Mukomuko",
        phone: String = ""
    ): Result<OfficerProfile> {
        val trimmedName = fullName.trim()
        val trimmedNip = nip.trim()

        if (trimmedName.length < 3) {
            return Result.failure(IllegalArgumentException("Nama petugas minimal 3 karakter."))
        }
        if (trimmedNip.length < 4) {
            return Result.failure(IllegalArgumentException("NIP / No. Registrasi TRC minimal 4 karakter."))
        }

        val currentRoster = getOfficerRoster().toMutableList()
        val existingIndex = currentRoster.indexOfFirst { it.nip.equals(trimmedNip, ignoreCase = true) }
        val newOfficer = OfficerProfile(
            id = if (existingIndex >= 0) currentRoster[existingIndex].id else UUID.randomUUID().toString(),
            fullName = trimmedName,
            nip = trimmedNip,
            role = role.ifBlank { OfficerRoles.PETUGAS_JAGA },
            unit = unit.ifBlank { "Pusdalops BPBD Kab. Mukomuko" },
            shift = shift.ifBlank { "Shift Siaga 24 Jam" },
            phone = phone.trim()
        )

        if (existingIndex >= 0) {
            currentRoster[existingIndex] = newOfficer
        } else {
            currentRoster.add(0, newOfficer)
        }
        saveOfficerRoster(currentRoster)
        FirestoreHelper.syncOfficerToCloud(newOfficer)
        return Result.success(newOfficer)
    }

    /**
     * Updates an existing officer profile (name, nip, role/classification, shift, etc.)
     * and synchronizes to Firestore in real-time.
     */
    fun updateOfficer(profile: OfficerProfile): Result<OfficerProfile> {
        return updateOfficer(
            id = profile.id,
            fullName = profile.fullName,
            nip = profile.nip,
            role = profile.role,
            shift = profile.shift,
            unit = profile.unit,
            phone = profile.phone
        )
    }

    fun updateOfficer(
        id: String,
        fullName: String,
        nip: String,
        role: String,
        shift: String,
        unit: String = "Pusdalops BPBD Kab. Mukomuko",
        phone: String = ""
    ): Result<OfficerProfile> {
        val trimmedName = fullName.trim()
        val trimmedNip = nip.trim()

        if (trimmedName.length < 3) {
            return Result.failure(IllegalArgumentException("Nama petugas minimal 3 karakter."))
        }
        if (trimmedNip.length < 4) {
            return Result.failure(IllegalArgumentException("NIP / No. Registrasi TRC minimal 4 karakter."))
        }

        val currentRoster = getOfficerRoster().toMutableList()
        val existingIndex = currentRoster.indexOfFirst { it.id == id || it.nip.equals(trimmedNip, ignoreCase = true) }
        val updated = OfficerProfile(
            id = if (existingIndex >= 0) currentRoster[existingIndex].id else id.ifBlank { UUID.randomUUID().toString() },
            fullName = trimmedName,
            nip = trimmedNip,
            role = role.ifBlank { OfficerRoles.PETUGAS_JAGA },
            unit = unit.ifBlank { "Pusdalops BPBD Kab. Mukomuko" },
            shift = shift.ifBlank { "Shift Siaga 24 Jam" },
            phone = phone.trim()
        )

        if (existingIndex >= 0) {
            currentRoster[existingIndex] = updated
        } else {
            currentRoster.add(0, updated)
        }
        saveOfficerRoster(currentRoster)
        FirestoreHelper.syncOfficerToCloud(updated)
        return Result.success(updated)
    }

    /**
     * Deletes an officer by ID from the roster and removes from Firestore.
     */
    fun deleteOfficer(id: String): Boolean {
        val currentRoster = getOfficerRoster().toMutableList()
        val removed = currentRoster.removeAll { it.id == id || it.nip.equals(id, ignoreCase = true) }
        if (removed) {
            saveOfficerRoster(currentRoster)
            FirestoreHelper.deleteOfficerFromCloud(id)
            return true
        }
        return false
    }

    /**
     * Resets the roster back to the standard official BPBD personnel.
     */
    fun resetRosterToDefault() {
        sharedPrefs.edit().remove(PREF_OFFICER_ROSTER_JSON).apply()
    }

    /**
     * Local offline PIN login is DISABLED.
     * All ASN officer PIN verification MUST go through Cloud Firestore via
     * [com.example.data.repository.SmartOfficerAuthRepository.authenticateOfficer].
     */
    @Deprecated("Verifikasi PIN petugas wajib melalui Cloud Firestore (SmartOfficerAuthRepository.authenticateOfficer)")
    fun loginOfficer(
        name: String,
        badgeId: String,
        shift: String,
        pin: String,
        role: String = OfficerRoles.PETUGAS_JAGA
    ): Boolean {
        return false
    }

    /**
     * Directly establishes an authenticated officer session AFTER Cloud Firestore PIN verification succeeds.
     * Stores ONLY `saved_nip` and non-secret display metadata — NEVER stores the officer's PIN on the device.
     */
    fun loginOfficerDirectly(
        name: String,
        badgeId: String,
        shift: String,
        role: String = OfficerRoles.RBAC_OPERATOR
    ) {
        val cleanNip = badgeId.trim()
        if (cleanNip.isBlank()) return
        val resolvedName = name.ifBlank { "Petugas Pusdalops PB" }
        val resolvedRole = role.trim().ifBlank { OfficerRoles.RBAC_OPERATOR }
        val resolvedShift = shift.ifBlank { "Shift Siaga 24 Jam" }
        val editor = sharedPrefs.edit()
            .putBoolean(PREF_IS_OFFICER_LOGGED_IN, true)
            .remove(PREF_OFFICER_PIN)
            .remove(PREF_OFFICER_PIN_HASH)
            .remove(PREF_OFFICER_PIN_SALT)
        SecureStorageHelper.putEncrypted(editor, PREF_SAVED_NIP, cleanNip)
        SecureStorageHelper.putEncrypted(editor, PREF_OFFICER_NAME, resolvedName)
        SecureStorageHelper.putEncrypted(editor, PREF_OFFICER_BADGE_ID, cleanNip)
        SecureStorageHelper.putEncrypted(editor, PREF_OFFICER_ROLE, resolvedRole)
        SecureStorageHelper.putEncrypted(editor, PREF_OFFICER_SHIFT, resolvedShift)
        editor.apply()
        com.example.util.SessionManager(context).setPetugasLogin(resolvedName, cleanNip, resolvedRole)
        com.example.data.sync.OfficerPushNotificationHelper.syncOfficerTopicSubscription(
            context = context,
            forceOfficerActive = true
        )
    }

    /**
     * Local PIN verification is DISABLED; PINs are NEVER stored locally on the device.
     * Always verify NIP + PIN via Cloud Firestore (`SmartOfficerAuthRepository.authenticateOfficer`).
     */
    @Deprecated("PIN tidak pernah disimpan di perangkat; verifikasi wajib ke Cloud Firestore")
    fun verifyPin(pin: String): Boolean {
        return false
    }

    /**
     * Returns whether the last Cloud Firestore login/PIN update confirmed a personalized Cloud PIN.
     * Does NOT read or store any local PIN or PIN hash.
     */
    fun hasCustomPin(): Boolean {
        return sharedPrefs.getBoolean(PREF_HAS_CLOUD_CUSTOM_PIN, false)
    }

    /**
     * Records ONLY a non-secret boolean status from Cloud Firestore (whether the officer's PIN is custom).
     * NEVER stores the actual PIN or PIN hash on the device.
     */
    fun markCloudCustomPinStatus(isCustomPinSet: Boolean) {
        sharedPrefs.edit()
            .putBoolean(PREF_HAS_CLOUD_CUSTOM_PIN, isCustomPinSet)
            .remove(PREF_OFFICER_PIN)
            .remove(PREF_OFFICER_PIN_HASH)
            .remove(PREF_OFFICER_PIN_SALT)
            .apply()
    }

    /**
     * Legacy compatibility shim: NEVER persists the PIN value to local storage.
     * Only records the non-secret custom PIN status flag and purges any legacy local PIN keys.
     */
    @Deprecated("Jangan pernah menyimpan PIN di perangkat; gunakan markCloudCustomPinStatus(true)")
    fun setCustomOfficerPin(newPin: String) {
        if (newPin.trim().length >= 4) {
            markCloudCustomPinStatus(true)
        }
    }

    /**
     * Local-only PIN change is disabled; PIN updates must be verified and persisted to Cloud Firestore
     * via [com.example.data.repository.SmartOfficerAuthRepository.changePersonalPin].
     */
    @Deprecated("Gunakan SmartOfficerAuthRepository.changePersonalPin() untuk memperbarui PIN di Cloud Firestore")
    fun changeOfficerPin(oldPin: String, newPin: String): Result<String> {
        return Result.failure(
            UnsupportedOperationException(
                "Perubahan PIN wajib dilakukan melalui sinkronisasi Cloud Firestore (SmartOfficerAuthRepository.changePersonalPin)."
            )
        )
    }

    /**
     * Local default PIN reset is permanently disabled so PINs can never revert to default.
     */
    @Deprecated("Reset PIN default lokal dinonaktifkan")
    fun resetPinToDefault(currentPin: String): Boolean {
        return false
    }

    /**
     * Local default PIN reset is permanently disabled; admin PIN management must update Cloud Firestore.
     */
    @Deprecated("Gunakan SmartOfficerAuthRepository untuk pengelolaan kredensial di Cloud Firestore")
    fun resetPinWithAdmin(adminPassword: String): Result<String> {
        return Result.failure(
            UnsupportedOperationException("Pengelolaan PIN petugas wajib dilakukan di Cloud Firestore.")
        )
    }

    fun hasSavedOfficerIdentity(): Boolean {
        return getSavedNip().isNotBlank()
    }

    fun clearSavedOfficerIdentity() {
        sharedPrefs.edit()
            .putBoolean(PREF_IS_OFFICER_LOGGED_IN, false)
            .remove(PREF_SAVED_NIP)
            .remove(PREF_OFFICER_NAME)
            .remove(PREF_OFFICER_BADGE_ID)
            .remove(PREF_OFFICER_ROLE)
            .remove(PREF_OFFICER_SHIFT)
            .apply()
        com.example.util.SessionManager(context).clearSession()
        SecurityManager.getInstance(context).clearLocalSession()
        com.example.data.sync.OfficerPushNotificationHelper.syncOfficerTopicSubscription(
            context = context,
            forceOfficerActive = false
        )
    }

    fun logoutOfficer() {
        sharedPrefs.edit()
            .putBoolean(PREF_IS_OFFICER_LOGGED_IN, false)
            .apply()
        com.example.util.SessionManager(context).clearSession()
        SecurityManager.getInstance(context).clearLocalSession()
        com.example.security.AdminAccessSecurityManager.revokeToken()
        com.example.data.sync.OfficerPushNotificationHelper.syncOfficerTopicSubscription(
            context = context,
            forceOfficerActive = false
        )
    }

    /**
     * Checks if Application Administrator has configured a custom master password.
     */
    fun isAdminPasswordConfigured(): Boolean {
        migrateLegacySecretsIfNeeded()
        val storedHash = SecureStorageHelper.getDecrypted(sharedPrefs, PREF_ADMIN_PASSWORD_HASH)
        val storedSalt = SecureStorageHelper.getDecrypted(sharedPrefs, PREF_ADMIN_PASSWORD_SALT)
        return !storedHash.isNullOrBlank() && !storedSalt.isNullOrBlank()
    }

    /**
     * First-time setup for the Application Administrator master password.
     * Rejects trivial defaults and stores only a PBKDF2 hash encrypted via AndroidKeyStore.
     */
    fun configureInitialAdminPassword(newPassword: String): Result<String> {
        if (isAdminPasswordConfigured()) {
            return Result.failure(IllegalStateException("Sandi Administrator sudah dikonfigurasi. Gunakan menu ubah sandi admin."))
        }
        val trimmed = newPassword.trim()
        if (trimmed.length < 8) {
            return Result.failure(IllegalArgumentException("Sandi Administrator minimal 8 karakter."))
        }
        if (FORBIDDEN_ADMIN_PASSWORDS.contains(trimmed.lowercase())) {
            return Result.failure(IllegalArgumentException("Sandi Administrator terlalu lemah atau menggunakan kata sandi default yang dilarang."))
        }
        val salt = OfficerCredentialEntity.generateCryptographicSalt()
        val hash = OfficerCredentialEntity.hashWithSalt(trimmed, salt)
        val editor = sharedPrefs.edit()
            .remove(PREF_ADMIN_PASSWORD)
            .putInt(PREF_ADMIN_FAILED_ATTEMPTS, 0)
            .putLong(PREF_ADMIN_LOCKOUT_UNTIL, 0L)
        SecureStorageHelper.putEncrypted(editor, PREF_ADMIN_PASSWORD_SALT, salt)
        SecureStorageHelper.putEncrypted(editor, PREF_ADMIN_PASSWORD_HASH, hash)
        editor.apply()
        return Result.success("Sandi Administrator berhasil dikonfigurasi secara aman.")
    }

    fun getAdminLockoutRemainingSeconds(): Long {
        val lockoutUntil = sharedPrefs.getLong(PREF_ADMIN_LOCKOUT_UNTIL, 0L)
        val remaining = lockoutUntil - System.currentTimeMillis()
        return if (remaining > 0L) (remaining + 999L) / 1000L else 0L
    }

    /**
     * Checks if Application Administrator is currently logged in.
     */
    fun isAdminLoggedIn(): Boolean {
        return sharedPrefs.getBoolean(PREF_IS_ADMIN_LOGGED_IN, false)
    }

    /**
     * Authenticates Application Admin using server-side verification via
     * [com.example.security.AdminAccessSecurityManager.verifyMasterPasswordAndIssueToken].
     */
    suspend fun loginAdminServerSide(username: String, password: String): Boolean {
        val trimmedUser = username.trim()
        val trimmedPass = password.trim()
        if (!trimmedUser.equals(DEFAULT_ADMIN_USERNAME, ignoreCase = true) || trimmedPass.isEmpty()) {
            return false
        }
        val result = com.example.security.AdminAccessSecurityManager.verifyMasterPasswordAndIssueToken(
            context = context,
            masterPassword = trimmedPass,
            officerNip = getSavedNip().ifBlank { DEFAULT_ADMIN_USERNAME },
            officerName = getOfficerName().ifBlank { "Administrator SIPAGAR KITA" },
            officerRole = getOfficerRole().ifBlank { "Admin" },
            actionTitle = "Login Administrator Aplikasi"
        )
        if (result is com.example.security.AdminVerificationResult.Authorized) {
            sharedPrefs.edit().putBoolean(PREF_IS_ADMIN_LOGGED_IN, true).apply()
            return true
        }
        return false
    }

    /**
     * Synchronous local admin login is disabled; use [loginAdminServerSide] or
     * [com.example.security.AdminAccessSecurityManager.verifyMasterPasswordAndIssueToken].
     */
    @Deprecated(
        message = "Gunakan AdminAccessSecurityManager.verifyMasterPasswordAndIssueToken() atau loginAdminServerSide() (server-side)"
    )
    fun loginAdmin(username: String, password: String): Boolean {
        return kotlinx.coroutines.runBlocking {
            loginAdminServerSide(username, password)
        }
    }

    /**
     * Verifikasi sandi master admin lokal DINONAKTIFKAN demi keamanan anti reverse-engineering.
     * Seluruh verifikasi master admin wajib melalui endpoint server-side
     * [com.example.security.AdminAccessSecurityManager.verifyMasterPasswordAndIssueToken].
     */
    @Deprecated(
        message = "Gunakan AdminAccessSecurityManager.verifyMasterPasswordAndIssueToken() (server-side)",
        replaceWith = ReplaceWith(
            "AdminAccessSecurityManager.verifyMasterPasswordAndIssueToken(context, password, officerNip, officerName, officerRole, actionTitle)",
            "com.example.security.AdminAccessSecurityManager"
        )
    )
    fun verifyAdminPassword(password: String): Boolean {
        return false
    }

    /**
     * Logs out Application Admin and revokes any active Temporary Access Token.
     */
    fun logoutAdmin() {
        com.example.security.AdminAccessSecurityManager.revokeToken()
        sharedPrefs.edit().putBoolean(PREF_IS_ADMIN_LOGGED_IN, false).apply()
    }

    /**
     * Allows Admin to change the master admin password after successful server-side verification via
     * [com.example.security.AdminAccessSecurityManager.verifyMasterPasswordAndIssueToken].
     */
    suspend fun changeAdminPasswordServerSide(oldPass: String, newPass: String): Result<String> {
        if (getAdminLockoutRemainingSeconds() > 0L) {
            return Result.failure(SecurityException("Akses admin dikunci sementara (${getAdminLockoutRemainingSeconds()} detik) karena terlalu banyak percobaan gagal."))
        }
        val trimmedOld = oldPass.trim()
        val trimmedNew = newPass.trim()
        val verifyResult = com.example.security.AdminAccessSecurityManager.verifyMasterPasswordAndIssueToken(
            context = context,
            masterPassword = trimmedOld,
            officerNip = getSavedNip().ifBlank { DEFAULT_ADMIN_USERNAME },
            officerName = getOfficerName().ifBlank { "Administrator SIPAGAR KITA" },
            officerRole = getOfficerRole().ifBlank { "Admin" },
            actionTitle = "Ubah Sandi Master Admin"
        )
        if (verifyResult !is com.example.security.AdminVerificationResult.Authorized) {
            return Result.failure(IllegalArgumentException("Sandi admin lama tidak sesuai."))
        }
        if (trimmedNew.length < 8) {
            return Result.failure(IllegalArgumentException("Sandi admin baru minimal 8 karakter."))
        }
        if (FORBIDDEN_ADMIN_PASSWORDS.contains(trimmedNew.lowercase())) {
            return Result.failure(IllegalArgumentException("Sandi baru terlalu lemah atau menggunakan kata sandi default yang dilarang."))
        }
        if (trimmedNew == trimmedOld) {
            return Result.failure(IllegalArgumentException("Sandi baru tidak boleh sama dengan sandi saat ini."))
        }

        val salt = OfficerCredentialEntity.generateCryptographicSalt()
        val hash = OfficerCredentialEntity.hashWithSalt(trimmedNew, salt)
        val editor = sharedPrefs.edit().remove(PREF_ADMIN_PASSWORD)
        SecureStorageHelper.putEncrypted(editor, PREF_ADMIN_PASSWORD_SALT, salt)
        SecureStorageHelper.putEncrypted(editor, PREF_ADMIN_PASSWORD_HASH, hash)
        editor.apply()
        return Result.success("Sandi Admin SIPAGAR KITA berhasil diperbarui secara permanen!")
    }

    /**
     * Synchronous wrapper for changing admin password using server-side verification via
     * [com.example.security.AdminAccessSecurityManager.verifyMasterPasswordAndIssueToken].
     */
    fun changeAdminPassword(oldPass: String, newPass: String): Result<String> {
        return kotlinx.coroutines.runBlocking {
            changeAdminPasswordServerSide(oldPass, newPass)
        }
    }

    private fun migrateLegacySecretsIfNeeded() {
        val editor = sharedPrefs.edit()
        var changed = false
        // Always purge any legacy local officer PIN keys without touching saved_nip or session
        if (sharedPrefs.contains(PREF_OFFICER_PIN) ||
            sharedPrefs.contains(PREF_OFFICER_PIN_HASH) ||
            sharedPrefs.contains(PREF_OFFICER_PIN_SALT)
        ) {
            editor.remove(PREF_OFFICER_PIN)
                .remove(PREF_OFFICER_PIN_HASH)
                .remove(PREF_OFFICER_PIN_SALT)
            changed = true
        }
        val legacyAdmin = sharedPrefs.getString(PREF_ADMIN_PASSWORD, null)
        if (!legacyAdmin.isNullOrBlank()) {
            val salt = OfficerCredentialEntity.generateCryptographicSalt()
            val hash = OfficerCredentialEntity.hashWithSalt(legacyAdmin.trim(), salt)
            SecureStorageHelper.putEncrypted(editor, PREF_ADMIN_PASSWORD_SALT, salt)
            SecureStorageHelper.putEncrypted(editor, PREF_ADMIN_PASSWORD_HASH, hash)
            editor.remove(PREF_ADMIN_PASSWORD)
            changed = true
        }
        if (changed) {
            editor.apply()
        }
    }

    /**
     * Merges officers received from Firestore real-time listener into the local roster.
     */
    fun syncRosterFromCloud(cloudOfficers: List<OfficerProfile>) {
        if (cloudOfficers.isEmpty()) return
        val currentRoster = getOfficerRoster().toMutableList()
        var changed = false
        for (cloud in cloudOfficers) {
            val idx = currentRoster.indexOfFirst { it.id == cloud.id || it.nip.equals(cloud.nip, ignoreCase = true) }
            if (idx >= 0) {
                currentRoster[idx] = cloud
                changed = true
            } else {
                currentRoster.add(cloud)
                changed = true
            }
        }
        if (changed) {
            saveOfficerRoster(currentRoster)
        }
    }
}
