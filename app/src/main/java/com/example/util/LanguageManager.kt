package com.example.util

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppLanguage(val code: String, val label: String, val flag: String) {
    ID("id", "Bahasa Indonesia", "🇮🇩"),
    EN("en", "English", "🇬🇧")
}

/**
 * Manages active language state (Indonesian & English) for SIPAGAR KITA.
 * Persists selection in SharedPreferences and provides reactive state for UI.
 */
object LanguageManager {
    private const val PREFS_NAME = "sipagar_language_prefs"
    private const val KEY_LANGUAGE = "selected_language"

    private val _currentLanguage = MutableStateFlow(AppLanguage.ID)
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    fun init(context: Context) {
        val prefs = getPrefs(context)
        val savedCode = prefs.getString(KEY_LANGUAGE, AppLanguage.ID.code) ?: AppLanguage.ID.code
        val lang = if (savedCode == AppLanguage.EN.code) AppLanguage.EN else AppLanguage.ID
        _currentLanguage.value = lang
    }

    fun setLanguage(context: Context, language: AppLanguage) {
        _currentLanguage.value = language
        getPrefs(context).edit().putString(KEY_LANGUAGE, language.code).apply()
    }

    fun toggleLanguage(context: Context) {
        val next = if (_currentLanguage.value == AppLanguage.ID) AppLanguage.EN else AppLanguage.ID
        setLanguage(context, next)
    }

    fun isIndonesian(): Boolean = _currentLanguage.value == AppLanguage.ID

    fun getString(idText: String, enText: String): String {
        return if (_currentLanguage.value == AppLanguage.ID) idText else enText
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}
