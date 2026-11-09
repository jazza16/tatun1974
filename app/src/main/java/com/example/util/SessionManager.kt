package com.example.util

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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
 * Menyimpan dan menyinkronkan data petugas login (Nama, NIP, Jabatan) menggunakan
 * Jetpack DataStore ("user_session") serta SharedPreferences ("PusdalopsSession").
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
        preferences[NAMA_PETUGAS]
            ?: prefs.getString("NAMA_PETUGAS", null)
            ?: DEFAULT_NAMA_PETUGAS
    }

    /**
     * Flow DataStore untuk memantau perubahan NIP Petugas secara reaktif.
     */
    val nipPetugasFlow: Flow<String> = appContext.dataStore.data.map { preferences ->
        preferences[NIP_PETUGAS]
            ?: prefs.getString("NIP_PETUGAS", null)
            ?: DEFAULT_NIP_PETUGAS
    }

    /**
     * Flow DataStore untuk memantau perubahan Jabatan Petugas secara reaktif.
     */
    val jabatanPetugasFlow: Flow<String> = appContext.dataStore.data.map { preferences ->
        preferences[JABATAN_PETUGAS]
            ?: prefs.getString("JABATAN_PETUGAS", null)
            ?: DEFAULT_JABATAN_PETUGAS
    }

    /**
     * Menyimpan sesi petugas ke DataStore ("user_session") dan SharedPreferences ("PusdalopsSession").
     */
    fun setPetugasLogin(nama: String, nip: String, jabatan: String) {
        prefs.edit()
            .putString("NAMA_PETUGAS", nama)
            .putString("NIP_PETUGAS", nip)
            .putString("JABATAN_PETUGAS", jabatan)
            .apply()

        scope.launch {
            savePetugasToDataStore(nama, nip, jabatan)
        }
    }

    /**
     * Suspend function untuk menyimpan sesi langsung ke DataStore ("user_session").
     */
    suspend fun savePetugasToDataStore(nama: String, nip: String, jabatan: String = DEFAULT_JABATAN_PETUGAS) {
        appContext.dataStore.edit { preferences ->
            preferences[NAMA_PETUGAS] = nama
            preferences[NIP_PETUGAS] = nip
            preferences[JABATAN_PETUGAS] = jabatan
        }
        prefs.edit()
            .putString("NAMA_PETUGAS", nama)
            .putString("NIP_PETUGAS", nip)
            .putString("JABATAN_PETUGAS", jabatan)
            .apply()
    }

    /**
     * Mengambil Nama Petugas dari DataStore / SharedPreferences.
     */
    fun getNamaPetugas(): String {
        val fromPrefs = prefs.getString("NAMA_PETUGAS", null)
        if (!fromPrefs.isNullOrBlank()) return fromPrefs
        return try {
            runBlocking(Dispatchers.IO) {
                appContext.dataStore.data.first()[NAMA_PETUGAS]
            }?.takeIf { it.isNotBlank() } ?: DEFAULT_NAMA_PETUGAS
        } catch (_: Exception) {
            DEFAULT_NAMA_PETUGAS
        }
    }

    /**
     * Mengambil NIP Petugas dari DataStore / SharedPreferences.
     */
    fun getNipPetugas(): String {
        val fromPrefs = prefs.getString("NIP_PETUGAS", null)
        if (!fromPrefs.isNullOrBlank()) return fromPrefs
        return try {
            runBlocking(Dispatchers.IO) {
                appContext.dataStore.data.first()[NIP_PETUGAS]
            }?.takeIf { it.isNotBlank() } ?: DEFAULT_NIP_PETUGAS
        } catch (_: Exception) {
            DEFAULT_NIP_PETUGAS
        }
    }

    /**
     * Mengambil Jabatan Petugas dari DataStore / SharedPreferences.
     */
    fun getJabatanPetugas(): String {
        val fromPrefs = prefs.getString("JABATAN_PETUGAS", null)
        if (!fromPrefs.isNullOrBlank()) return fromPrefs
        return try {
            runBlocking(Dispatchers.IO) {
                appContext.dataStore.data.first()[JABATAN_PETUGAS]
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
        val nama = prefs.getString("NAMA_PETUGAS", null)
        return !nama.isNullOrBlank()
    }
}
