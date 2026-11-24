package com.example.data.local

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pengelola status persetujuan Disklaimer & Kebijakan Privasi SIPAGAR KITA (UU PDP).
 *
 * Menyimpan versi disklaimer yang disetujui beserta stempel waktu secara persisten agar:
 * - Pop-up Disklaimer Singkat hanya muncul satu kali saat pertama kali pengguna membuka aplikasi
 *   (atau ketika versi Disklaimer/Kebijakan Privasi diperbarui), sehingga tidak menghambat
 *   pengguna dalam situasi darurat bencana.
 * - Pengguna tetap dapat membuka kembali Disklaimer Lengkap & Kebijakan Privasi kapan saja
 *   melalui menu resmi di dalam aplikasi.
 */
class DisclaimerConsentManager(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun hasAcceptedCurrentDisclaimer(): Boolean {
        val accepted = prefs.getBoolean(KEY_ACCEPTED, false)
        val version = prefs.getString(KEY_ACCEPTED_VERSION, "").orEmpty()
        return accepted && version == CURRENT_DISCLAIMER_VERSION
    }

    fun getAcceptedVersion(): String {
        return prefs.getString(KEY_ACCEPTED_VERSION, "").orEmpty()
    }

    fun getAcceptedTimestamp(): Long {
        return prefs.getLong(KEY_ACCEPTED_TIMESTAMP, 0L)
    }

    fun getFormattedAcceptedDate(): String? {
        val ts = getAcceptedTimestamp()
        if (ts <= 0L) return null
        return try {
            SimpleDateFormat("dd MMM yyyy, HH:mm 'WIB'", Locale.forLanguageTag("id-ID"))
                .format(Date(ts))
        } catch (_: Exception) {
            SimpleDateFormat("dd/MM/yyyy HH:mm 'WIB'", Locale.US).format(Date(ts))
        }
    }

    fun recordConsentAccepted(version: String = CURRENT_DISCLAIMER_VERSION) {
        prefs.edit()
            .putBoolean(KEY_ACCEPTED, true)
            .putString(KEY_ACCEPTED_VERSION, version)
            .putLong(KEY_ACCEPTED_TIMESTAMP, System.currentTimeMillis())
            .commit()
    }

    fun resetConsentForTesting() {
        prefs.edit().clear().commit()
    }

    companion object {
        private const val PREFS_NAME = "bpbd_disclaimer_consent_prefs"
        private const val KEY_ACCEPTED = "disclaimer_accepted"
        private const val KEY_ACCEPTED_VERSION = "disclaimer_accepted_version"
        private const val KEY_ACCEPTED_TIMESTAMP = "disclaimer_accepted_timestamp"

        const val CURRENT_DISCLAIMER_VERSION = "2026.1"
        const val LAST_UPDATED_DISPLAY = "24 November 2026"
    }
}
