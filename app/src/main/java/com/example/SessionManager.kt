package com.example

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.example.util.NAMA_PETUGAS as UTIL_NAMA_PETUGAS
import com.example.util.NIP_PETUGAS as UTIL_NIP_PETUGAS
import com.example.util.JABATAN_PETUGAS as UTIL_JABATAN_PETUGAS
import com.example.util.dataStore as utilDataStore

typealias SessionManager = com.example.util.SessionManager

val Context.dataStore: DataStore<Preferences>
    get() = this.utilDataStore

val NAMA_PETUGAS: Preferences.Key<String> = UTIL_NAMA_PETUGAS
val NIP_PETUGAS: Preferences.Key<String> = UTIL_NIP_PETUGAS
val JABATAN_PETUGAS: Preferences.Key<String> = UTIL_JABATAN_PETUGAS
