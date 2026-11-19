package com.example.util

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.security.SecureStorageHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Alur DataStore Sesi Petugas Pusdalops BPBD Kabupaten Mukomuko.
 */
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_session")
val NAMA_PETUGAS = stringPreferencesKey("nama_petugas")
val NIP_PETUGAS = stringPreferencesKey("nip_petugas")
val JABATAN_PETUGAS = stringPreferencesKey("jabatan_petugas")

/**
 * Pengelola Sesi Petugas Piket Pusdalops BPBD Kabupaten Mukomuko.
 * Menyimpan dan menyinkronkan data petugas login (Nama, NIP, Jabatan) terenkripsi AES-256-GCM
 * menggunakan Jetpack DataStore ("user_session") serta SharedPreferences ("PusdalopsSession").
 */
class SessionManager(private val context: Context) {
    private val appContext: Context = context.applicationContext ?: context
    private val prefs: SharedPreferences = appContext.getSharedPreferences("PusdalopsSession", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        const val DEFAULT_NAMA_PETUGAS = "HITATUN ARAZAK"
        const val DEFAULT_NIP_PETUGAS = "197401282006041006"
        const val DEFAULT_JABATAN_PETUGAS = "Manajer Pusdalops PB"
        const val DEFAULT_STATION_ID = "PUSDALOPS-MM-01"
    }

    /**
     * Flow DataStore untuk memantau perubahan Nama Petugas secara reaktif.
     */
    val namaPetugasFlow: Flow<String> = appContext.dataStore.data.map { preferences ->
        SecureStorageHelper.decryptString(preferences[NAMA_PETUGAS])?.takeIf { it.isNotBlank() }
            ?: SecureStorageHelper.getDecrypted(prefs, "NAMA_PETUGAS")?.takeIf { it.isNotBlank() }
            ?: DEFAULT_NAMA_PETUGAS
    }

    /**
     * Flow DataStore untuk memantau perubahan NIP Petugas secara reaktif.
     */
    val nipPetugasFlow: Flow<String> = appContext.dataStore.data.map { preferences ->
        SecureStorageHelper.decryptString(preferences[NIP_PETUGAS])?.takeIf { it.isNotBlank() }
            ?: SecureStorageHelper.getDecrypted(prefs, "NIP_PETUGAS")?.takeIf { it.isNotBlank() }
            ?: DEFAULT_NIP_PETUGAS
    }

    /**
     * Flow DataStore untuk memantau perubahan Jabatan Petugas secara reaktif.
     */
    val jabatanPetugasFlow: Flow<String> = appContext.dataStore.data.map { preferences ->
        SecureStorageHelper.decryptString(preferences[JABATAN_PETUGAS])?.takeIf { it.isNotBlank() }
            ?: SecureStorageHelper.getDecrypted(prefs, "JABATAN_PETUGAS")?.takeIf { it.isNotBlank() }
            ?: DEFAULT_JABATAN_PETUGAS
    }

    /**
     * Menyimpan sesi petugas terenkripsi ke DataStore ("user_session") dan SharedPreferences ("PusdalopsSession").
     * Otomatis menyinkronkan Nama & NIP resmi dari daftar kontak personil Pusdalops BPBD.
     */
    fun setPetugasLogin(nama: String, nip: String, jabatan: String) {
        val canonical = com.example.data.local.OfficialAsnBpbdPersonnel.resolveCanonicalPersonnel(
            rawName = nama,
            rawNip = nip,
            defaultRole = jabatan.ifBlank { DEFAULT_JABATAN_PETUGAS }
        )
        val syncedNama = canonical.name.ifBlank { nama.ifBlank { DEFAULT_NAMA_PETUGAS } }
        val syncedNip = canonical.nip.takeIf { it.isNotBlank() && it != "-" }
            ?: nip.ifBlank { DEFAULT_NIP_PETUGAS }
        val syncedJabatan = jabatan.ifBlank { canonical.role.ifBlank { DEFAULT_JABATAN_PETUGAS } }

        val editor = prefs.edit()
        SecureStorageHelper.putEncrypted(editor, "NAMA_PETUGAS", syncedNama)
        SecureStorageHelper.putEncrypted(editor, "NIP_PETUGAS", syncedNip)
        SecureStorageHelper.putEncrypted(editor, "JABATAN_PETUGAS", syncedJabatan)
        editor.apply()

        scope.launch {
            savePetugasToDataStore(syncedNama, syncedNip, syncedJabatan)
        }
    }

    /**
     * Suspend function untuk menyimpan sesi terenkripsi langsung ke DataStore ("user_session").
     */
    suspend fun savePetugasToDataStore(nama: String, nip: String, jabatan: String = DEFAULT_JABATAN_PETUGAS) {
        val canonical = com.example.data.local.OfficialAsnBpbdPersonnel.resolveCanonicalPersonnel(
            rawName = nama,
            rawNip = nip,
            defaultRole = jabatan.ifBlank { DEFAULT_JABATAN_PETUGAS }
        )
        val syncedNama = canonical.name.ifBlank { nama.ifBlank { DEFAULT_NAMA_PETUGAS } }
        val syncedNip = canonical.nip.takeIf { it.isNotBlank() && it != "-" }
            ?: nip.ifBlank { DEFAULT_NIP_PETUGAS }
        val syncedJabatan = jabatan.ifBlank { canonical.role.ifBlank { DEFAULT_JABATAN_PETUGAS } }

        val encNama = SecureStorageHelper.encryptString(syncedNama)
        val encNip = SecureStorageHelper.encryptString(syncedNip)
        val encJabatan = SecureStorageHelper.encryptString(syncedJabatan)
        appContext.dataStore.edit { preferences ->
            preferences[NAMA_PETUGAS] = encNama
            preferences[NIP_PETUGAS] = encNip
            preferences[JABATAN_PETUGAS] = encJabatan
        }
        val editor = prefs.edit()
        SecureStorageHelper.putEncrypted(editor, "NAMA_PETUGAS", syncedNama)
        SecureStorageHelper.putEncrypted(editor, "NIP_PETUGAS", syncedNip)
        SecureStorageHelper.putEncrypted(editor, "JABATAN_PETUGAS", syncedJabatan)
        editor.apply()
    }

    /**
     * Mengambil Nama Petugas dari OfficerAuthManager / DataStore / SharedPreferences
     * yang sudah disinkronkan dengan daftar kontak personil resmi.
     */
    fun getNamaPetugas(): String {
        val officerAuth = com.example.data.local.OfficerAuthManager(appContext)
        val authName = officerAuth.getOfficerName()
        if (authName.isNotBlank()) return authName

        val fromPrefs = SecureStorageHelper.getDecrypted(prefs, "NAMA_PETUGAS")
        val rawNip = SecureStorageHelper.getDecrypted(prefs, "NIP_PETUGAS").orEmpty()
        if (!fromPrefs.isNullOrBlank()) {
            return com.example.data.local.OfficialAsnBpbdPersonnel
                .resolveCanonicalPersonnel(fromPrefs, rawNip).name
        }
        return try {
            val dsNama = runBlocking(Dispatchers.IO) {
                SecureStorageHelper.decryptString(appContext.dataStore.data.first()[NAMA_PETUGAS])
            }?.takeIf { it.isNotBlank() } ?: DEFAULT_NAMA_PETUGAS
            com.example.data.local.OfficialAsnBpbdPersonnel
                .resolveCanonicalPersonnel(dsNama, rawNip).name
        } catch (_: Exception) {
            DEFAULT_NAMA_PETUGAS
        }
    }

    /**
     * Mengambil NIP Petugas dari OfficerAuthManager / DataStore / SharedPreferences
     * yang sudah disinkronkan dengan daftar kontak personil resmi.
     */
    fun getNipPetugas(): String {
        val officerAuth = com.example.data.local.OfficerAuthManager(appContext)
        val authNip = officerAuth.getOfficerBadgeId()
        if (authNip.isNotBlank()) return authNip

        val fromPrefs = SecureStorageHelper.getDecrypted(prefs, "NIP_PETUGAS")
        val rawName = SecureStorageHelper.getDecrypted(prefs, "NAMA_PETUGAS").orEmpty()
        if (!fromPrefs.isNullOrBlank()) {
            val resolved = com.example.data.local.OfficialAsnBpbdPersonnel
                .resolveCanonicalPersonnel(rawName, fromPrefs).nip
            return if (resolved.isNotBlank() && resolved != "-") resolved else fromPrefs
        }
        return try {
            val dsNip = runBlocking(Dispatchers.IO) {
                SecureStorageHelper.decryptString(appContext.dataStore.data.first()[NIP_PETUGAS])
            }?.takeIf { it.isNotBlank() }
            if (!dsNip.isNullOrBlank()) {
                val resolved = com.example.data.local.OfficialAsnBpbdPersonnel
                    .resolveCanonicalPersonnel(rawName, dsNip).nip
                if (resolved.isNotBlank() && resolved != "-") resolved else dsNip
            } else {
                val byName = com.example.data.local.OfficialAsnBpbdPersonnel.findByNipOrName(rawName)
                byName?.nip ?: DEFAULT_NIP_PETUGAS
            }
        } catch (_: Exception) {
            DEFAULT_NIP_PETUGAS
        }
    }

    /**
     * Mengambil Jabatan Petugas dari DataStore / SharedPreferences.
     */
    fun getJabatanPetugas(): String {
        val officerAuth = com.example.data.local.OfficerAuthManager(appContext)
        val authRole = officerAuth.getOfficerRole()
        if (authRole.isNotBlank()) return authRole

        val fromPrefs = SecureStorageHelper.getDecrypted(prefs, "JABATAN_PETUGAS")
        if (!fromPrefs.isNullOrBlank()) return fromPrefs
        return try {
            runBlocking(Dispatchers.IO) {
                SecureStorageHelper.decryptString(appContext.dataStore.data.first()[JABATAN_PETUGAS])
            }?.takeIf { it.isNotBlank() } ?: DEFAULT_JABATAN_PETUGAS
        } catch (_: Exception) {
            DEFAULT_JABATAN_PETUGAS
        }
    }

    fun clearSession() {
        prefs.edit().clear().apply()
        scope.launch {
            appContext.dataStore.edit { it.clear() }
        }
    }

    fun isPetugasLoggedIn(): Boolean {
        val nama = SecureStorageHelper.getDecrypted(prefs, "NAMA_PETUGAS")
        return !nama.isNullOrBlank()
    }

    /**
     * Representasi klaim identitas operator yang terverifikasi dari sesi/token keamanan
     * (setara dengan `req.user` hasil middleware `authenticateToken`).
     */
    data class VerifiedOperatorUser(
        val name: String,
        val nip: String,
        val role: String,
        val stationId: String,
        val integrityToken: String
    ) {
        fun formattedNameWithNip(): String = "$name (NIP. $nip)"
    }

    /**
     * Memverifikasi sesi/token keamanan aktif (AES-256-GCM Keystore + Play Integrity Session)
     * dan mengembalikan identitas operator (`name`, `nip`, `role`, & `stationId`) yang sudah
     * disinkronkan dengan daftar kontak personil Pusdalops BPBD.
     */
    fun authenticateToken(): VerifiedOperatorUser {
        val officerAuth = com.example.data.local.OfficerAuthManager(appContext)
        val securityManager = com.example.security.SecurityManager.getInstance(appContext)

        val candidateNip = officerAuth.getOfficerBadgeId().ifBlank { getNipPetugas() }
        val activeSecuritySession = securityManager.getLocalCachedSessions().firstOrNull {
            it.officerNip.equals(candidateNip, ignoreCase = true)
        } ?: securityManager.getLocalCachedSessions().firstOrNull()

        val candidateName = activeSecuritySession?.officerName?.takeIf { it.isNotBlank() }
            ?: officerAuth.getOfficerName().takeIf { it.isNotBlank() }
            ?: getNamaPetugas().ifBlank { DEFAULT_NAMA_PETUGAS }

        val rawNip = activeSecuritySession?.officerNip?.takeIf { it.isNotBlank() }
            ?: candidateNip

        val syncedProfile = officerAuth.resolveSyncedOfficer(rawName = candidateName, rawNip = rawNip)

        val verifiedRole = activeSecuritySession?.officerRole?.takeIf { it.isNotBlank() }
            ?: syncedProfile.role.takeIf { it.isNotBlank() }
            ?: officerAuth.getOfficerRole().takeIf { it.isNotBlank() }
            ?: getJabatanPetugas().ifBlank { DEFAULT_JABATAN_PETUGAS }

        val verifiedStationId = SecureStorageHelper.getDecrypted(prefs, "STATION_ID_PETUGAS")
            ?.takeIf { it.isNotBlank() }
            ?: DEFAULT_STATION_ID

        return VerifiedOperatorUser(
            name = syncedProfile.fullName.ifBlank { DEFAULT_NAMA_PETUGAS },
            nip = syncedProfile.nip.ifBlank { DEFAULT_NIP_PETUGAS },
            role = verifiedRole,
            stationId = verifiedStationId,
            integrityToken = activeSecuritySession?.integrityToken.orEmpty()
        )
    }
}

