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
     */
    fun setPetugasLogin(nama: String, nip: String, jabatan: String) {
        val editor = prefs.edit()
        SecureStorageHelper.putEncrypted(editor, "NAMA_PETUGAS", nama)
        SecureStorageHelper.putEncrypted(editor, "NIP_PETUGAS", nip)
        SecureStorageHelper.putEncrypted(editor, "JABATAN_PETUGAS", jabatan)
        editor.apply()

        scope.launch {
            savePetugasToDataStore(nama, nip, jabatan)
        }
    }

    /**
     * Suspend function untuk menyimpan sesi terenkripsi langsung ke DataStore ("user_session").
     */
    suspend fun savePetugasToDataStore(nama: String, nip: String, jabatan: String = DEFAULT_JABATAN_PETUGAS) {
        val encNama = SecureStorageHelper.encryptString(nama)
        val encNip = SecureStorageHelper.encryptString(nip)
        val encJabatan = SecureStorageHelper.encryptString(jabatan)
        appContext.dataStore.edit { preferences ->
            preferences[NAMA_PETUGAS] = encNama
            preferences[NIP_PETUGAS] = encNip
            preferences[JABATAN_PETUGAS] = encJabatan
        }
        val editor = prefs.edit()
        SecureStorageHelper.putEncrypted(editor, "NAMA_PETUGAS", nama)
        SecureStorageHelper.putEncrypted(editor, "NIP_PETUGAS", nip)
        SecureStorageHelper.putEncrypted(editor, "JABATAN_PETUGAS", jabatan)
        editor.apply()
    }

    /**
     * Mengambil Nama Petugas dari DataStore / SharedPreferences.
     */
    fun getNamaPetugas(): String {
        val fromPrefs = SecureStorageHelper.getDecrypted(prefs, "NAMA_PETUGAS")
        if (!fromPrefs.isNullOrBlank()) return fromPrefs
        return try {
            runBlocking(Dispatchers.IO) {
                SecureStorageHelper.decryptString(appContext.dataStore.data.first()[NAMA_PETUGAS])
            }?.takeIf { it.isNotBlank() } ?: DEFAULT_NAMA_PETUGAS
        } catch (_: Exception) {
            DEFAULT_NAMA_PETUGAS
        }
    }

    /**
     * Mengambil NIP Petugas dari DataStore / SharedPreferences.
     */
    fun getNipPetugas(): String {
        val fromPrefs = SecureStorageHelper.getDecrypted(prefs, "NIP_PETUGAS")
        if (!fromPrefs.isNullOrBlank()) return fromPrefs
        return try {
            runBlocking(Dispatchers.IO) {
                SecureStorageHelper.decryptString(appContext.dataStore.data.first()[NIP_PETUGAS])
            }?.takeIf { it.isNotBlank() } ?: DEFAULT_NIP_PETUGAS
        } catch (_: Exception) {
            DEFAULT_NIP_PETUGAS
        }
    }

    /**
     * Mengambil Jabatan Petugas dari DataStore / SharedPreferences.
     */
    fun getJabatanPetugas(): String {
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
}
