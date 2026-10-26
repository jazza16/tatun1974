package com.example.data.local

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages authentication and verification for Petugas Jaga (Duty Officers)
 * from Pusdalops BPBD Kabupaten Mukomuko / Tim Reaksi Cepat (TRC).
 * Only registered/verified duty officers can change disaster report statuses
 * (Diproses -> Divalidasi -> Selesai) and submit official response notes.
 */
class OfficerAuthManager(context: Context) {

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

        // Default valid PINs for BPBD Mukomuko Duty Officers
        val VALID_PINS = listOf("117", "BPBD117", "TRC2026", "PUSDALOPS117")
    }

    fun isOfficerLoggedIn(): Boolean {
        return sharedPrefs.getBoolean(PREF_IS_OFFICER_LOGGED_IN, false)
    }

    fun getOfficerName(): String {
        return sharedPrefs.getString(PREF_OFFICER_NAME, "Petugas Pusdalops BPBD") ?: "Petugas Pusdalops BPBD"
    }

    fun getOfficerBadgeId(): String {
        return sharedPrefs.getString(PREF_OFFICER_BADGE_ID, "TRC-MMK-117") ?: "TRC-MMK-117"
    }

    fun getOfficerRole(): String {
        return sharedPrefs.getString(PREF_OFFICER_ROLE, "Petugas Jaga Siaga Bencana") ?: "Petugas Jaga Siaga Bencana"
    }

    fun getOfficerShift(): String {
        return sharedPrefs.getString(PREF_OFFICER_SHIFT, "Shift Siaga 24 Jam") ?: "Shift Siaga 24 Jam"
    }

    /**
     * Validates PIN and registers/logs in the duty officer.
     */
    fun loginOfficer(
        name: String,
        badgeId: String,
        shift: String,
        pin: String
    ): Boolean {
        val trimmedPin = pin.trim()
        val customPin = sharedPrefs.getString(PREF_OFFICER_PIN, null)
        val isPinValid = VALID_PINS.any { it.equals(trimmedPin, ignoreCase = true) } ||
                (customPin != null && customPin == trimmedPin)

        if (isPinValid) {
            sharedPrefs.edit()
                .putBoolean(PREF_IS_OFFICER_LOGGED_IN, true)
                .putString(PREF_OFFICER_NAME, name.ifBlank { "Petugas Pusdalops BPBD Mukomuko" })
                .putString(PREF_OFFICER_BADGE_ID, badgeId.ifBlank { "TRC-MMK-117" })
                .putString(PREF_OFFICER_ROLE, "Petugas Jaga Resmi Pusdalops BPBD")
                .putString(PREF_OFFICER_SHIFT, shift.ifBlank { "Regu Siaga TRC" })
                .apply()
            return true
        }
        return false
    }

    fun logoutOfficer() {
        sharedPrefs.edit()
            .putBoolean(PREF_IS_OFFICER_LOGGED_IN, false)
            .apply()
    }
}
