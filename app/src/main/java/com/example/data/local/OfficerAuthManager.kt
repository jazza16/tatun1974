package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
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
    const val PENGAWAS = "Pengawas BPBD"
    const val MANAJER = "Manajer Pusdalops-PB"
    const val SUPERVISOR = "Supervisor BPBD"
    const val KOORD_ADMINISTRASI = "Koord. Administrasi"
    const val OPERATOR = "Operator Pusdalops"
    const val PETUGAS_JAGA = "Petugas Jaga Siaga / Piket"
    const val PETUGAS_LAPANGAN = "Petugas Lapangan TRC / Logistik"

    val ALL_ROLES = listOf(PENGAWAS, MANAJER, SUPERVISOR, KOORD_ADMINISTRASI, OPERATOR, PETUGAS_JAGA, PETUGAS_LAPANGAN)

    /**
     * Official hierarchy rank for duty personnel list.
     * Manajer Pusdalops is positioned immediately below Pengawas.
     */
    fun getRoleRank(role: String): Int = when {
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
        private const val PREF_OFFICER_PIN = "officer_pin"
        private const val PREF_OFFICER_ROSTER_JSON = "officer_roster_json"

        private const val PREF_IS_ADMIN_LOGGED_IN = "is_admin_logged_in"
        private const val PREF_ADMIN_PASSWORD = "admin_password"
        const val DEFAULT_ADMIN_USERNAME = "admin"
        const val DEFAULT_ADMIN_PASSWORD = "sipagarkita2026"

        // Default valid PINs for BPBD Mukomuko Duty Officers and Admin Master
        val VALID_PINS = listOf("117", "BPBD117", "ADMIN117", "TRC2026", "PUSDALOPS117")

        // Roster is pre-populated with official ASN BPBD Mukomuko Pusdalops operators
        val DEFAULT_ROSTER: List<OfficerProfile> = OfficialAsnBpbdPersonnel.toOfficerProfiles()
    }

    fun isOfficerLoggedIn(): Boolean {
        return sharedPrefs.getBoolean(PREF_IS_OFFICER_LOGGED_IN, false)
    }

    fun getOfficerName(): String {
        return sharedPrefs.getString(PREF_OFFICER_NAME, "") ?: ""
    }

    fun getOfficerBadgeId(): String {
        return sharedPrefs.getString(PREF_OFFICER_BADGE_ID, "") ?: ""
    }

    fun getOfficerRole(): String {
        return sharedPrefs.getString(PREF_OFFICER_ROLE, OfficerRoles.OPERATOR) ?: OfficerRoles.OPERATOR
    }

    fun getOfficerShift(): String {
        return sharedPrefs.getString(PREF_OFFICER_SHIFT, "Shift Siaga 24 Jam") ?: "Shift Siaga 24 Jam"
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
     * Gets the full roster of officers, operators, and field responders.
     * Guarantees all 8 ASN BPBD Pusdalops operators are present with separate name and NIP.
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
                list.add(
                    OfficerProfile(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        fullName = obj.getString("fullName"),
                        nip = obj.getString("nip"),
                        role = obj.getString("role"),
                        unit = obj.optString("unit", "Pusdalops BPBD Kab. Mukomuko"),
                        shift = obj.optString("shift", "Shift Siaga 24 Jam"),
                        phone = obj.optString("phone", "")
                    )
                )
            }
            // Ensure all 8 official ASN BPBD operators exist in the list
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
     * Validates PIN and registers/logs in the duty officer.
     */
    fun loginOfficer(
        name: String,
        badgeId: String,
        shift: String,
        pin: String,
        role: String = OfficerRoles.PETUGAS_JAGA
    ): Boolean {
        val trimmedPin = pin.trim()
        val customPin = sharedPrefs.getString(PREF_OFFICER_PIN, null)
        val isPinValid = VALID_PINS.any { it.equals(trimmedPin, ignoreCase = true) } ||
                (customPin != null && customPin == trimmedPin)

        if (isPinValid) {
            val resolvedName = name.ifBlank { "Petugas Pusdalops PB" }
            val resolvedBadge = badgeId.ifBlank { "PUSDALOPS-117" }
            val resolvedRole = role.ifBlank { OfficerRoles.PETUGAS_JAGA }
            sharedPrefs.edit()
                .putBoolean(PREF_IS_OFFICER_LOGGED_IN, true)
                .putString(PREF_OFFICER_NAME, resolvedName)
                .putString(PREF_OFFICER_BADGE_ID, resolvedBadge)
                .putString(PREF_OFFICER_ROLE, resolvedRole)
                .putString(PREF_OFFICER_SHIFT, shift.ifBlank { "Shift Siaga 24 Jam" })
                .apply()
            com.example.util.SessionManager(context).setPetugasLogin(resolvedName, resolvedBadge, resolvedRole)
            return true
        }
        return false
    }

    /**
     * Directly establishes an authenticated officer session after smart database verification.
     */
    fun loginOfficerDirectly(
        name: String,
        badgeId: String,
        shift: String,
        role: String = OfficerRoles.PETUGAS_JAGA
    ) {
        val resolvedName = name.ifBlank { "Petugas Pusdalops PB" }
        val resolvedBadge = badgeId.ifBlank { "PUSDALOPS-117" }
        val resolvedRole = role.ifBlank { OfficerRoles.PETUGAS_JAGA }
        sharedPrefs.edit()
            .putBoolean(PREF_IS_OFFICER_LOGGED_IN, true)
            .putString(PREF_OFFICER_NAME, resolvedName)
            .putString(PREF_OFFICER_BADGE_ID, resolvedBadge)
            .putString(PREF_OFFICER_ROLE, resolvedRole)
            .putString(PREF_OFFICER_SHIFT, shift.ifBlank { "Shift Siaga 24 Jam" })
            .apply()
        com.example.util.SessionManager(context).setPetugasLogin(resolvedName, resolvedBadge, resolvedRole)
    }

    /**
     * Verifies if a given PIN matches the active officer PIN or fallback default PINs.
     */
    fun verifyPin(pin: String): Boolean {
        val trimmedPin = pin.trim()
        val customPin = sharedPrefs.getString(PREF_OFFICER_PIN, null)
        return (customPin != null && customPin == trimmedPin) ||
                VALID_PINS.any { it.equals(trimmedPin, ignoreCase = true) }
    }

    fun hasCustomPin(): Boolean {
        return sharedPrefs.contains(PREF_OFFICER_PIN)
    }

    /**
     * Changes the officer PIN/password to protect against unauthorized ID/NIP usage.
     */
    fun changeOfficerPin(oldPin: String, newPin: String): Result<String> {
        val trimmedOld = oldPin.trim()
        val trimmedNew = newPin.trim()

        if (!verifyPin(trimmedOld)) {
            return Result.failure(IllegalArgumentException("PIN / Sandi lama tidak valid. Silakan periksa kembali."))
        }

        if (trimmedNew.length < 4) {
            return Result.failure(IllegalArgumentException("PIN baru minimal terdiri dari 4 karakter atau digit."))
        }

        if (trimmedNew == trimmedOld) {
            return Result.failure(IllegalArgumentException("PIN baru tidak boleh sama dengan PIN saat ini."))
        }

        sharedPrefs.edit()
            .putString(PREF_OFFICER_PIN, trimmedNew)
            .apply()

        return Result.success("PIN keamanan petugas berhasil diperbarui secara permanen!")
    }

    /**
     * Resets the PIN back to standard Pusdalops default PINs.
     */
    fun resetPinToDefault(currentPin: String): Boolean {
        if (verifyPin(currentPin)) {
            sharedPrefs.edit()
                .remove(PREF_OFFICER_PIN)
                .apply()
            return true
        }
        return false
    }

    /**
     * Resets the PIN back to standard Pusdalops default PINs using Administrator password/PIN.
     */
    fun resetPinWithAdmin(adminPasswordOrPin: String): Result<String> {
        val trimmed = adminPasswordOrPin.trim()
        val storedAdminPass = sharedPrefs.getString(PREF_ADMIN_PASSWORD, DEFAULT_ADMIN_PASSWORD) ?: DEFAULT_ADMIN_PASSWORD
        val isAdminValid = (trimmed == storedAdminPass || trimmed == DEFAULT_ADMIN_PASSWORD || verifyPin(trimmed))
        if (!isAdminValid) {
            return Result.failure(IllegalArgumentException("Sandi / PIN Administrator tidak valid. Otorisasi reset PIN ditolak."))
        }
        sharedPrefs.edit()
            .remove(PREF_OFFICER_PIN)
            .apply()
        return Result.success("PIN keamanan petugas berhasil di-reset ke PIN resmi bawaan Pusdalops (117 / BPBD117 / TRC2026).")
    }

    fun logoutOfficer() {
        sharedPrefs.edit()
            .putBoolean(PREF_IS_OFFICER_LOGGED_IN, false)
            .apply()
        com.example.util.SessionManager(context).clearSession()
    }

    /**
     * Checks if Application Administrator is currently logged in.
     */
    fun isAdminLoggedIn(): Boolean {
        return sharedPrefs.getBoolean(PREF_IS_ADMIN_LOGGED_IN, false)
    }

    /**
     * Authenticates Application Admin.
     * Default username: admin, Default password: sipagarkita2026.
     */
    fun loginAdmin(username: String, password: String): Boolean {
        val trimmedUser = username.trim()
        val trimmedPass = password.trim()
        val currentPass = sharedPrefs.getString(PREF_ADMIN_PASSWORD, DEFAULT_ADMIN_PASSWORD) ?: DEFAULT_ADMIN_PASSWORD

        if (trimmedUser.equals(DEFAULT_ADMIN_USERNAME, ignoreCase = true) && trimmedPass == currentPass) {
            sharedPrefs.edit().putBoolean(PREF_IS_ADMIN_LOGGED_IN, true).apply()
            return true
        }
        return false
    }

    /**
     * Logs out Application Admin.
     */
    fun logoutAdmin() {
        sharedPrefs.edit().putBoolean(PREF_IS_ADMIN_LOGGED_IN, false).apply()
    }

    /**
     * Allows Admin to change the master admin password/PIN after successful login.
     */
    fun changeAdminPassword(oldPass: String, newPass: String): Result<String> {
        val trimmedOld = oldPass.trim()
        val trimmedNew = newPass.trim()
        val currentPass = sharedPrefs.getString(PREF_ADMIN_PASSWORD, DEFAULT_ADMIN_PASSWORD) ?: DEFAULT_ADMIN_PASSWORD

        if (trimmedOld != currentPass) {
            return Result.failure(IllegalArgumentException("Sandi admin lama tidak sesuai."))
        }
        if (trimmedNew.length < 6) {
            return Result.failure(IllegalArgumentException("Sandi baru minimal 6 karakter/digit."))
        }
        if (trimmedNew == trimmedOld) {
            return Result.failure(IllegalArgumentException("Sandi baru tidak boleh sama dengan sandi saat ini."))
        }

        sharedPrefs.edit().putString(PREF_ADMIN_PASSWORD, trimmedNew).apply()
        return Result.success("Sandi Admin SIPAGAR KITA berhasil diperbarui secara permanen!")
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
