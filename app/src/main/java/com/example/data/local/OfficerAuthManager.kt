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
    const val SEKRETARIS = "Sekretaris"
    const val KABID_KL = "Kabid KL"
    const val KABID_RR = "Kabid RR"
    const val MANAJER = "Manajer Pusdalops-PB"
    const val SUPERVISOR = "Supervisor BPBD"
    const val KOORD_ADMINISTRASI = "Koord. Administrasi"
    const val OPERATOR = "Operator Pusdalops"
    const val PETUGAS_JAGA = "Petugas Jaga Siaga / Piket"
    const val PETUGAS_LAPANGAN = "Petugas Lapangan TRC / Logistik"

    val ALL_ROLES = listOf(
        SUPER_ADMIN,
        RBAC_OPERATOR,
        PENGAWAS,
        SEKRETARIS,
        KABID_KL,
        KABID_RR,
        MANAJER,
        SUPERVISOR,
        KOORD_ADMINISTRASI,
        OPERATOR,
        PETUGAS_JAGA,
        PETUGAS_LAPANGAN
    )

    /**
     * Official hierarchy rank for duty personnel list.
     * Manajer Pusdalops is positioned immediately below Pengawas.
     */
    fun getRoleRank(role: String): Int = when {
        role.equals(SUPER_ADMIN, ignoreCase = true) -> 0
        role.contains("Pengawas", ignoreCase = true) -> 1
        role.contains("Manajer", ignoreCase = true) || role.contains("Manager", ignoreCase = true) -> 2
        role.contains("Sekretaris", ignoreCase = true) -> 3
        role.contains("Kabid KL", ignoreCase = true) -> 4
        role.contains("Kabid RR", ignoreCase = true) -> 5
        role.contains("Supervisor", ignoreCase = true) -> 6
        role.contains("Koord", ignoreCase = true) || role.contains("Administrasi", ignoreCase = true) -> 7
        role.contains("Operator", ignoreCase = true) -> 8
        role.contains("Piket", ignoreCase = true) || role.contains("Jaga", ignoreCase = true) -> 9
        role.contains("Lapangan", ignoreCase = true) || role.contains("Logistik", ignoreCase = true) || role.contains("TRC", ignoreCase = true) -> 10
        else -> 11
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
        private const val PREF_DELETED_OFFICER_NIPS_JSON = "deleted_officer_nips_json"

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
        try {
            val currentVersion = try {
                sharedPrefs.getInt(PREF_STORAGE_SCHEMA_VERSION, 0)
            } catch (_: Exception) {
                0
            }
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
        } catch (e: Throwable) {
            android.util.Log.w("OfficerAuthManager", "Non-fatal error in ensureUpdateSafeStorageMigration: ${e.message}")
        }
    }

    fun isOfficerLoggedIn(): Boolean {
        return try {
            sharedPrefs.getBoolean(PREF_IS_OFFICER_LOGGED_IN, false)
        } catch (_: Exception) {
            false
        }
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
        editor.commit()
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

    private fun normalizeNip(nip: String): String =
        nip.trim().replace(Regex("[\\s\\-]+"), "")

    /**
     * Returns map of deleted/archived officer NIPs (lowercase normalized NIP -> deleted timestamp ms).
     * Ensures retired/transferred/typo-corrected official ASN records never resurrect automatically.
     */
    fun getDeletedOfficerNips(): Map<String, Long> {
        val raw = sharedPrefs.getString(PREF_DELETED_OFFICER_NIPS_JSON, null)
        if (raw.isNullOrBlank()) return emptyMap()
        return try {
            val obj = JSONObject(raw)
            val result = mutableMapOf<String, Long>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val clean = normalizeNip(k).lowercase()
                if (clean.isNotBlank()) {
                    result[clean] = obj.optLong(k, 1L).coerceAtLeast(1L)
                }
            }
            result
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun saveDeletedOfficerNips(map: Map<String, Long>) {
        val obj = JSONObject()
        for ((k, ts) in map) {
            val clean = normalizeNip(k).lowercase()
            if (clean.isNotBlank()) {
                obj.put(clean, ts)
            }
        }
        sharedPrefs.edit().putString(PREF_DELETED_OFFICER_NIPS_JSON, obj.toString()).commit()
    }

    fun isNipDeleted(nip: String): Boolean {
        val clean = normalizeNip(nip).lowercase()
        if (clean.isBlank()) return false
        return getDeletedOfficerNips().containsKey(clean)
    }

    fun getDeletedTimestamp(nip: String): Long? {
        val clean = normalizeNip(nip).lowercase()
        if (clean.isBlank()) return null
        return getDeletedOfficerNips()[clean]
    }

    fun markNipDeleted(nip: String, deletedAtMs: Long = System.currentTimeMillis()) {
        val clean = normalizeNip(nip).lowercase()
        if (clean.isBlank()) return
        val current = getDeletedOfficerNips().toMutableMap()
        val prev = current[clean] ?: 0L
        current[clean] = maxOf(prev, deletedAtMs)
        saveDeletedOfficerNips(current)
    }

    fun unmarkNipDeleted(nip: String) {
        val clean = normalizeNip(nip).lowercase()
        if (clean.isBlank()) return
        val current = getDeletedOfficerNips().toMutableMap()
        if (current.remove(clean) != null) {
            saveDeletedOfficerNips(current)
        }
    }

    fun clearAllDeletedNips() {
        sharedPrefs.edit().remove(PREF_DELETED_OFFICER_NIPS_JSON).commit()
    }

    /**
     * Gets the full roster of officers, operators, and field responders.
     * Preserves manually added/edited profiles and strictly respects Admin deletions
     * (tombstoned NIPs for retired/transferred/typo-corrected ASN are never resurrected).
     */
    fun getOfficerRoster(): List<OfficerProfile> {
        val deletedMap = getDeletedOfficerNips()
        val jsonString = sharedPrefs.getString(PREF_OFFICER_ROSTER_JSON, null)
        val defaultList = OfficialAsnBpbdPersonnel.toOfficerProfiles().filterNot {
            deletedMap.containsKey(normalizeNip(it.nip).lowercase())
        }
        if (jsonString == null) {
            return defaultList
        }
        if (jsonString.isBlank()) {
            return if (deletedMap.isEmpty()) defaultList else emptyList()
        }
        return try {
            val array = JSONArray(jsonString)
            val list = mutableListOf<OfficerProfile>()
            val seenNips = mutableSetOf<String>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val rawFullName = obj.optString("fullName", "").trim()
                val rawNip = normalizeNip(obj.optString("nip", ""))
                if (rawFullName.isBlank() && rawNip.isBlank()) continue
                val exactOfficial = OfficialAsnBpbdPersonnel.ALL_OFFICIAL_PERSONNEL.firstOrNull {
                    normalizeNip(it.nip).equals(rawNip, ignoreCase = true)
                }
                val resolvedName = rawFullName.ifBlank { exactOfficial?.name ?: "" }
                val resolvedNip = rawNip.ifBlank { exactOfficial?.nip?.let { normalizeNip(it) } ?: "" }
                if (resolvedName.isBlank() || resolvedNip.isBlank()) continue
                val nipKey = resolvedNip.lowercase()
                if (deletedMap.containsKey(nipKey) || !seenNips.add(nipKey)) continue
                list.add(
                    OfficerProfile(
                        id = obj.optString("id", "").ifBlank { "officer_$resolvedNip" },
                        fullName = resolvedName,
                        nip = resolvedNip,
                        role = obj.optString("role", "").ifBlank { exactOfficial?.role ?: OfficerRoles.OPERATOR },
                        unit = obj.optString("unit", "").ifBlank { exactOfficial?.unit ?: "Pusdalops BPBD Kab. Mukomuko" },
                        shift = obj.optString("shift", "").ifBlank { exactOfficial?.shift ?: "Shift Siaga 24 Jam" },
                        phone = obj.optString("phone", "")
                    )
                )
            }
            for (asn in defaultList) {
                val asnKey = normalizeNip(asn.nip).lowercase()
                if (!deletedMap.containsKey(asnKey) && seenNips.add(asnKey)) {
                    list.add(asn)
                }
            }
            list
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
        sharedPrefs.edit().putString(PREF_OFFICER_ROSTER_JSON, array.toString()).commit()
    }

    /**
     * Synchronizes Room personnel roster into local SharedPreferences cache in a single batch
     * without triggering Firestore writes or resurrecting Admin-deleted NIPs.
     */
    fun syncRosterLocallyWithoutCloudWrite(profiles: List<OfficerProfile>) {
        if (profiles.isEmpty()) return
        val deletedMap = getDeletedOfficerNips()
        val existingByNip = getOfficerRoster().associateBy {
            normalizeNip(it.nip).lowercase()
        }.toMutableMap()
        var changed = false
        for (profile in profiles) {
            val cleanNip = normalizeNip(profile.nip)
            val cleanName = profile.fullName.trim()
            if (cleanNip.length < 4 || cleanName.length < 3) continue
            val key = cleanNip.lowercase()
            if (deletedMap.containsKey(key)) continue
            val prev = existingByNip[key]
            val stableId = prev?.id?.takeIf { it.isNotBlank() && !it.all { ch -> ch.isDigit() } }
                ?: profile.id.takeIf { it.isNotBlank() && !it.all { ch -> ch.isDigit() } }
                ?: "officer_$cleanNip"
            val normalized = OfficerProfile(
                id = stableId,
                fullName = cleanName,
                nip = cleanNip,
                role = profile.role.ifBlank { OfficerRoles.PETUGAS_JAGA },
                unit = profile.unit.ifBlank { "Pusdalops BPBD Kab. Mukomuko" },
                shift = profile.shift.ifBlank { "Shift Siaga 24 Jam" },
                phone = profile.phone.trim()
            )
            if (prev != normalized) {
                existingByNip[key] = normalized
                changed = true
            }
        }
        if (!changed) return
        val array = JSONArray()
        for (officer in existingByNip.values) {
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
     * Automatically clears any prior deletion tombstone for this NIP so re-onboarding works seamlessly.
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
        val trimmedNip = normalizeNip(nip)

        if (trimmedName.length < 3) {
            return Result.failure(IllegalArgumentException("Nama petugas minimal 3 karakter."))
        }
        if (trimmedNip.length < 4) {
            return Result.failure(IllegalArgumentException("NIP / No. Registrasi TRC minimal 4 karakter."))
        }

        unmarkNipDeleted(trimmedNip)

        val currentRoster = getOfficerRoster().toMutableList()
        val existingIndex = currentRoster.indexOfFirst {
            normalizeNip(it.nip).equals(trimmedNip, ignoreCase = true)
        }
        val existingStableId = if (existingIndex >= 0) {
            currentRoster[existingIndex].id.takeIf { it.isNotBlank() && !it.all { ch -> ch.isDigit() } }
        } else null
        val newOfficer = OfficerProfile(
            id = existingStableId ?: "officer_$trimmedNip",
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
        FirestoreHelper.syncOfficerToCloud(newOfficer, context)
        return Result.success(newOfficer)
    }

    /**
     * Updates an existing officer profile (name, nip, role/classification, shift, unit, phone)
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
        phone: String = "",
        previousNip: String? = null
    ): Result<OfficerProfile> {
        val trimmedName = fullName.trim()
        val trimmedNip = normalizeNip(nip)
        val cleanPrevNip = previousNip?.let { normalizeNip(it) }?.takeIf { it.isNotBlank() }

        if (trimmedName.length < 3) {
            return Result.failure(IllegalArgumentException("Nama petugas minimal 3 karakter."))
        }
        if (trimmedNip.length < 4) {
            return Result.failure(IllegalArgumentException("NIP / No. Registrasi TRC minimal 4 karakter."))
        }

        // Un-tombstone target NIP if it was previously deleted
        unmarkNipDeleted(trimmedNip)

        val currentRoster = getOfficerRoster().toMutableList()

        val nipChanged = cleanPrevNip != null && !cleanPrevNip.equals(trimmedNip, ignoreCase = true)
        if (nipChanged && cleanPrevNip != null) {
            // Tombstone the old typo NIP so a hardcoded default ASN with the old NIP never resurrects
            markNipDeleted(cleanPrevNip)
        }

        val existingIndex = currentRoster.indexOfFirst { entry ->
            val entryNip = normalizeNip(entry.nip)
            (id.isNotBlank() && entry.id == id) ||
                (cleanPrevNip != null && entryNip.equals(cleanPrevNip, ignoreCase = true)) ||
                entryNip.equals(trimmedNip, ignoreCase = true)
        }

        val resolvedId = when {
            nipChanged -> "officer_$trimmedNip"
            existingIndex >= 0 && currentRoster[existingIndex].id.isNotBlank() &&
                !currentRoster[existingIndex].id.all { it.isDigit() } -> currentRoster[existingIndex].id
            id.isNotBlank() && !id.all { it.isDigit() } -> id
            else -> "officer_$trimmedNip"
        }

        val updated = OfficerProfile(
            id = resolvedId,
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

        // Deduplicate: remove any extra entries matching either old typo NIP or new NIP (keep first occurrence)
        val deduped = mutableListOf<OfficerProfile>()
        val seen = mutableSetOf<String>()
        for (entry in currentRoster) {
            val eNip = normalizeNip(entry.nip).lowercase()
            if (nipChanged && cleanPrevNip != null && eNip == cleanPrevNip.lowercase()) continue
            if (seen.add(eNip)) {
                deduped.add(entry)
            }
        }

        saveOfficerRoster(deduped)
        FirestoreHelper.syncOfficerToCloud(updated, context)
        return Result.success(updated)
    }

    /**
     * Permanently removes an officer by NIP (and optional legacy ID) from the local roster,
     * tombstones the NIP so hardcoded default ASN entries never resurrect, and propagates deletion to Cloud.
     */
    fun deleteOfficerByNip(nip: String, legacyId: String? = null, syncCloud: Boolean = true): Boolean {
        val cleanNip = normalizeNip(nip)
        val cleanLegacy = legacyId?.trim().orEmpty()
        if (cleanNip.isBlank() && cleanLegacy.isBlank()) return false

        val currentRoster = getOfficerRoster().toMutableList()
        val matched = currentRoster.firstOrNull { entry ->
            (cleanNip.isNotBlank() && normalizeNip(entry.nip).equals(cleanNip, ignoreCase = true)) ||
                (cleanLegacy.isNotBlank() && (entry.id == cleanLegacy || normalizeNip(entry.nip).equals(normalizeNip(cleanLegacy), ignoreCase = true)))
        }
        val effectiveNip = cleanNip.ifBlank { matched?.let { normalizeNip(it.nip) } ?: "" }

        if (effectiveNip.isNotBlank()) {
            markNipDeleted(effectiveNip)
        }

        val removed = currentRoster.removeAll { entry ->
            (effectiveNip.isNotBlank() && normalizeNip(entry.nip).equals(effectiveNip, ignoreCase = true)) ||
                (cleanLegacy.isNotBlank() && entry.id == cleanLegacy)
        }
        saveOfficerRoster(currentRoster)

        if (syncCloud) {
            if (effectiveNip.isNotBlank()) {
                FirestoreHelper.deleteOfficerFromCloud(effectiveNip)
            }
            if (cleanLegacy.isNotBlank() && !cleanLegacy.equals(effectiveNip, ignoreCase = true) && !cleanLegacy.all { it.isDigit() }) {
                FirestoreHelper.deleteOfficerFromCloud(cleanLegacy)
            }
        }
        return removed || effectiveNip.isNotBlank()
    }

    /**
     * Deletes an officer by ID or NIP from the roster, tombstones their NIP, and removes from Firestore.
     */
    fun deleteOfficer(id: String): Boolean {
        val raw = id.trim()
        if (raw.isBlank()) return false
        val extractedNip = when {
            raw.startsWith("asn-", ignoreCase = true) -> normalizeNip(raw.removePrefix("asn-"))
            raw.startsWith("officer_", ignoreCase = true) -> normalizeNip(raw.removePrefix("officer_"))
            else -> ""
        }
        val currentRoster = getOfficerRoster()
        val matched = currentRoster.firstOrNull {
            it.id == raw ||
                normalizeNip(it.nip).equals(normalizeNip(raw), ignoreCase = true) ||
                (extractedNip.isNotBlank() && normalizeNip(it.nip).equals(extractedNip, ignoreCase = true))
        }
        val targetNip = matched?.let { normalizeNip(it.nip) }
            ?: extractedNip.ifBlank { normalizeNip(raw).takeIf { it.length >= 4 } ?: "" }
        return deleteOfficerByNip(nip = targetNip, legacyId = raw, syncCloud = true)
    }

    /**
     * Resets the roster back to the standard official BPBD personnel and clears deleted tombstones.
     */
    fun resetRosterToDefault() {
        sharedPrefs.edit()
            .remove(PREF_OFFICER_ROSTER_JSON)
            .remove(PREF_DELETED_OFFICER_NIPS_JSON)
            .commit()
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
        editor.commit()
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
            .commit()
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
            .commit()
        com.example.util.SessionManager(context).clearSession()
        SecurityManager.getInstance(context).clearLocalSession()
        com.example.security.AdminAccessSecurityManager.revokeToken()
        // Pertahankan langganan topik FCM bpbd_officer_alerts jika identitas petugas masih terdaftar di HP Asli Petugas
        com.example.data.sync.OfficerPushNotificationHelper.syncOfficerTopicSubscription(
            context = context
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
     * Merges officers received from Firestore real-time listener into the local roster,
     * respecting deleted tombstones and propagating cloud soft-deletes.
     */
    fun syncRosterFromCloud(
        cloudOfficers: List<OfficerProfile>,
        deletedCloudNips: Map<String, Long> = emptyMap(),
        cloudUpdatedAtByNip: Map<String, Long> = emptyMap()
    ) {
        var changed = false
        if (deletedCloudNips.isNotEmpty()) {
            for ((delNip, delTs) in deletedCloudNips) {
                markNipDeleted(delNip, delTs)
            }
        }

        val currentRoster = getOfficerRoster().toMutableList()
        val deletedMap = getDeletedOfficerNips()

        if (currentRoster.removeAll { deletedMap.containsKey(normalizeNip(it.nip).lowercase()) }) {
            changed = true
        }

        for (cloud in cloudOfficers) {
            val cleanNip = normalizeNip(cloud.nip)
            if (cleanNip.isBlank()) continue
            val key = cleanNip.lowercase()
            val localDeletedAt = deletedMap[key]
            val cloudUpdatedAt = cloudUpdatedAtByNip[key] ?: 0L
            if (localDeletedAt != null) {
                if (cloudUpdatedAt > localDeletedAt) {
                    // Re-added on another device AFTER local deletion -> restore
                    unmarkNipDeleted(cleanNip)
                } else {
                    // Keep deleted locally
                    continue
                }
            }
            val idx = currentRoster.indexOfFirst {
                normalizeNip(it.nip).equals(cleanNip, ignoreCase = true) ||
                    (cloud.id.isNotBlank() && it.id == cloud.id)
            }
            val normalizedCloud = cloud.copy(
                id = cloud.id.ifBlank { "officer_$cleanNip" },
                nip = cleanNip
            )
            if (idx >= 0) {
                if (currentRoster[idx] != normalizedCloud) {
                    currentRoster[idx] = normalizedCloud
                    changed = true
                }
            } else {
                currentRoster.add(normalizedCloud)
                changed = true
            }
        }
        if (changed) {
            saveOfficerRoster(currentRoster)
        }
    }
}
