package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mukomuko_app_update_prefs")

/**
 * Persistent manager for App Update state and 'Last Checked Version'.
 * Ensures that previously installed devices can detect, receive, and permanently
 * install the latest version online without requiring offline APK updates,
 * and prevents recurring update notification dialogs once updated or dismissed for a release.
 */
class AppUpdatePreferences(private val context: Context) {

    private val sharedPrefs: SharedPreferences = context.getSharedPreferences(
        "mukomuko_update_persistence",
        Context.MODE_PRIVATE
    )

    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        val KEY_INSTALLED_VERSION = stringPreferencesKey("installed_version")
        val KEY_LAST_CHECKED_VERSION = stringPreferencesKey("last_checked_version")
        val KEY_LATEST_SERVER_VERSION = stringPreferencesKey("latest_server_version")
        val KEY_IS_PERMANENTLY_INSTALLED = booleanPreferencesKey("is_permanently_installed")
        val KEY_DISMISSED_DIALOG_VERSION = stringPreferencesKey("dismissed_dialog_version")
        val KEY_IS_ONBOARDING_COMPLETED = booleanPreferencesKey("is_onboarding_completed")

        private const val PREF_INSTALLED_VERSION = "installed_version"
        private const val PREF_LAST_CHECKED_VERSION = "last_checked_version"
        private const val PREF_LATEST_SERVER_VERSION = "latest_server_version"
        private const val PREF_IS_PERMANENTLY_INSTALLED = "is_permanently_installed"
        private const val PREF_DISMISSED_DIALOG_VERSION = "dismissed_dialog_version"
        private const val PREF_IS_ONBOARDING_COMPLETED = "is_onboarding_completed"

        const val DEFAULT_LATEST_VERSION = "1.3.0"
        const val PREVIOUS_VERSION_MOCK = "1.2.0"
    }

    init {
        // First-run initialization: if no version is saved yet, initialize from actual BuildConfig
        if (!sharedPrefs.contains(PREF_INSTALLED_VERSION)) {
            val buildVersion = try {
                BuildConfig.VERSION_NAME
            } catch (_: Exception) {
                DEFAULT_LATEST_VERSION
            }
            sharedPrefs.edit()
                .putString(PREF_INSTALLED_VERSION, buildVersion)
                .putString(PREF_LATEST_SERVER_VERSION, DEFAULT_LATEST_VERSION)
                .putBoolean(PREF_IS_PERMANENTLY_INSTALLED, true)
                .putString(PREF_LAST_CHECKED_VERSION, DEFAULT_LATEST_VERSION)
                .apply()
        }
    }

    /**
     * DataStore Flow for the last checked version from remote update server.
     */
    val lastCheckedVersionFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_LAST_CHECKED_VERSION] ?: sharedPrefs.getString(PREF_LAST_CHECKED_VERSION, DEFAULT_LATEST_VERSION) ?: DEFAULT_LATEST_VERSION
    }

    /**
     * DataStore Flow for the currently recognized installed version.
     */
    val installedVersionFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_INSTALLED_VERSION] ?: sharedPrefs.getString(PREF_INSTALLED_VERSION, DEFAULT_LATEST_VERSION) ?: DEFAULT_LATEST_VERSION
    }

    /**
     * Synchronous getter for current recognized installed version on this device.
     */
    fun getInstalledVersion(): String {
        return sharedPrefs.getString(PREF_INSTALLED_VERSION, DEFAULT_LATEST_VERSION) ?: DEFAULT_LATEST_VERSION
    }

    /**
     * Synchronous getter for latest version detected on remote update server.
     */
    fun getLatestServerVersion(): String {
        return sharedPrefs.getString(PREF_LATEST_SERVER_VERSION, DEFAULT_LATEST_VERSION) ?: DEFAULT_LATEST_VERSION
    }

    /**
     * Synchronous getter for last checked version.
     */
    fun getLastCheckedVersion(): String {
        return sharedPrefs.getString(PREF_LAST_CHECKED_VERSION, "") ?: ""
    }

    /**
     * Whether the update has been installed permanently into persistent storage.
     */
    fun isUpdatePermanentlyInstalled(): Boolean {
        return sharedPrefs.getBoolean(PREF_IS_PERMANENTLY_INSTALLED, true)
    }

    /**
     * Check if the user already dismissed the update notification dialog for a specific version.
     */
    fun isDialogDismissedForVersion(version: String): Boolean {
        val dismissed = sharedPrefs.getString(PREF_DISMISSED_DIALOG_VERSION, "")
        return dismissed == version
    }

    /**
     * Permanently record that the device has installed the update to [newVersion].
     * Updates both SharedPreferences and DataStore.
     */
    fun setUpdateInstalledPermanently(newVersion: String) {
        sharedPrefs.edit()
            .putString(PREF_INSTALLED_VERSION, newVersion)
            .putString(PREF_LAST_CHECKED_VERSION, newVersion)
            .putString(PREF_LATEST_SERVER_VERSION, newVersion)
            .putBoolean(PREF_IS_PERMANENTLY_INSTALLED, true)
            .putString(PREF_DISMISSED_DIALOG_VERSION, newVersion)
            .apply()

        scope.launch {
            try {
                context.dataStore.edit { prefs ->
                    prefs[KEY_INSTALLED_VERSION] = newVersion
                    prefs[KEY_LAST_CHECKED_VERSION] = newVersion
                    prefs[KEY_LATEST_SERVER_VERSION] = newVersion
                    prefs[KEY_IS_PERMANENTLY_INSTALLED] = true
                    prefs[KEY_DISMISSED_DIALOG_VERSION] = newVersion
                }
            } catch (_: Exception) {}
        }
    }

    /**
     * Records that the user dismissed the update dialog for [version], ensuring
     * they are not prompted again until a higher version is published on the server.
     */
    fun setDialogDismissedForVersion(version: String) {
        sharedPrefs.edit()
            .putString(PREF_DISMISSED_DIALOG_VERSION, version)
            .putString(PREF_LAST_CHECKED_VERSION, version)
            .apply()

        scope.launch {
            try {
                context.dataStore.edit { prefs ->
                    prefs[KEY_DISMISSED_DIALOG_VERSION] = version
                    prefs[KEY_LAST_CHECKED_VERSION] = version
                }
            } catch (_: Exception) {}
        }
    }

    /**
     * Records the latest checked version from server check.
     */
    fun setLastCheckedVersion(version: String) {
        sharedPrefs.edit()
            .putString(PREF_LAST_CHECKED_VERSION, version)
            .apply()

        scope.launch {
            try {
                context.dataStore.edit { prefs ->
                    prefs[KEY_LAST_CHECKED_VERSION] = version
                }
            } catch (_: Exception) {}
        }
    }

    /**
     * Developer / QA Simulation: sets the device to simulate an earlier installed version (e.g. 1.2.0 or 1.0.0).
     * This allows full end-to-end verification that a device previously on an earlier version receives,
     * recognizes, downloads, and installs 1.3.0 permanently without offline updates.
     */
    fun simulateEarlierInstalledVersion(earlierVersion: String = PREVIOUS_VERSION_MOCK) {
        sharedPrefs.edit()
            .putString(PREF_INSTALLED_VERSION, earlierVersion)
            .putString(PREF_LATEST_SERVER_VERSION, DEFAULT_LATEST_VERSION)
            .putBoolean(PREF_IS_PERMANENTLY_INSTALLED, false)
            .remove(PREF_DISMISSED_DIALOG_VERSION)
            .remove(PREF_LAST_CHECKED_VERSION)
            .apply()

        scope.launch {
            try {
                context.dataStore.edit { prefs ->
                    prefs[KEY_INSTALLED_VERSION] = earlierVersion
                    prefs[KEY_LATEST_SERVER_VERSION] = DEFAULT_LATEST_VERSION
                    prefs[KEY_IS_PERMANENTLY_INSTALLED] = false
                    prefs.remove(KEY_DISMISSED_DIALOG_VERSION)
                    prefs.remove(KEY_LAST_CHECKED_VERSION)
                }
            } catch (_: Exception) {}
        }
    }

    /**
     * Reset to latest official release state (v1.3.0 permanently applied).
     */
    fun resetToLatestReleaseState() {
        setUpdateInstalledPermanently(DEFAULT_LATEST_VERSION)
    }

    /**
     * Whether the user has completed or dismissed the onboarding interactive guide.
     */
    fun isOnboardingCompleted(): Boolean {
        return sharedPrefs.getBoolean(PREF_IS_ONBOARDING_COMPLETED, false)
    }

    /**
     * Permanently marks that the user has completed or dismissed the onboarding interactive guide.
     */
    fun setOnboardingCompleted(completed: Boolean = true) {
        sharedPrefs.edit()
            .putBoolean(PREF_IS_ONBOARDING_COMPLETED, completed)
            .apply()
        scope.launch {
            try {
                context.dataStore.edit { prefs ->
                    prefs[KEY_IS_ONBOARDING_COMPLETED] = completed
                }
            } catch (_: Exception) {}
        }
    }

    /**
     * Resets onboarding state so user can experience the interactive guide again.
     */
    fun resetOnboarding() {
        sharedPrefs.edit()
            .putBoolean(PREF_IS_ONBOARDING_COMPLETED, false)
            .apply()
        scope.launch {
            try {
                context.dataStore.edit { prefs ->
                    prefs[KEY_IS_ONBOARDING_COMPLETED] = false
                }
            } catch (_: Exception) {}
        }
    }
}
