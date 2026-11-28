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
import com.example.util.AppVersionHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mukomuko_app_update_prefs")

/**
 * Persistent manager for App Update state, APK installation synchronization, and OTA checks.
 * Ensures that previously installed devices running older versions (e.g. v1.0.0, v1.3.0, v1.4.0)
 * accurately recognize, notify, and install newer versions (e.g. v1.5.0) both via in-app OTA
 * and when upgrading APK binaries.
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

        private const val PREF_RECORDED_BUILD_VERSION = "recorded_build_version"
        private const val PREF_INSTALLED_VERSION = "installed_version"
        private const val PREF_LAST_CHECKED_VERSION = "last_checked_version"
        private const val PREF_LATEST_SERVER_VERSION = "latest_server_version"
        private const val PREF_IS_PERMANENTLY_INSTALLED = "is_permanently_installed"
        private const val PREF_DISMISSED_DIALOG_VERSION = "dismissed_dialog_version"
        private const val PREF_IS_ONBOARDING_COMPLETED = "is_onboarding_completed"

        const val DEFAULT_LATEST_VERSION = "1.3.1"
        const val PREVIOUS_VERSION_MOCK = "0.9.0"
    }

    init {
        syncVersionsOnStartup()
    }

    /**
     * Inspects the binary version from [BuildConfig.VERSION_NAME] against persistent preferences.
     * Automatically handles:
     * 1. Fresh app install: initializes preferences to the binary version and latest release.
     * 2. APK Upgrade (e.g. installing v1.0.7 APK over v1.0.6): detects new binary version,
     *    automatically migrates stored version, marks update permanent, and clears dismissal flags.
     * 3. Previous versions running on device: ensures server version is recognized as at least
     *    [DEFAULT_LATEST_VERSION] so update dialogs and banners trigger promptly.
     */
    fun syncVersionsOnStartup() {
        val currentBuildVersion = try {
            BuildConfig.VERSION_NAME
        } catch (_: Exception) {
            DEFAULT_LATEST_VERSION
        }

        try {
            val recordedBuild = try { sharedPrefs.getString(PREF_RECORDED_BUILD_VERSION, null) } catch (_: Exception) { null }
            val savedInstalled = try { sharedPrefs.getString(PREF_INSTALLED_VERSION, null) } catch (_: Exception) { null }
            val savedServer = try { sharedPrefs.getString(PREF_LATEST_SERVER_VERSION, null) } catch (_: Exception) { null }

            val editor = sharedPrefs.edit()

            if (recordedBuild == null && savedInstalled == null) {
                // Brand new installation
                val targetLatest = maxVersion(currentBuildVersion, DEFAULT_LATEST_VERSION)
                val isCurrent = (currentBuildVersion == targetLatest)
                editor.putString(PREF_RECORDED_BUILD_VERSION, currentBuildVersion)
                editor.putString(PREF_INSTALLED_VERSION, currentBuildVersion)
                editor.putString(PREF_LATEST_SERVER_VERSION, targetLatest)
                editor.putBoolean(PREF_IS_PERMANENTLY_INSTALLED, isCurrent)
                editor.putString(PREF_LAST_CHECKED_VERSION, targetLatest)
                editor.apply()
                syncStateToDataStore(currentBuildVersion, targetLatest, targetLatest, isCurrent, clearDismissed = false)
            } else if (recordedBuild != currentBuildVersion) {
                // An APK update was installed onto the phone!
                editor.putString(PREF_RECORDED_BUILD_VERSION, currentBuildVersion)
                editor.putString(PREF_INSTALLED_VERSION, currentBuildVersion)
                val targetLatest = maxVersion(currentBuildVersion, DEFAULT_LATEST_VERSION)
                editor.putString(PREF_LATEST_SERVER_VERSION, targetLatest)
                editor.putBoolean(PREF_IS_PERMANENTLY_INSTALLED, true)
                editor.remove(PREF_DISMISSED_DIALOG_VERSION)
                editor.putString(PREF_LAST_CHECKED_VERSION, currentBuildVersion)
                editor.apply()
                syncStateToDataStore(currentBuildVersion, currentBuildVersion, targetLatest, true, clearDismissed = true)
            } else {
                // Same binary build is running. Ensure server version is at least currentBuildVersion.
                val currentServer = savedServer ?: currentBuildVersion
                val baselineLatest = maxVersion(currentBuildVersion, DEFAULT_LATEST_VERSION)
                if (AppVersionHelper.isVersionNewer(currentServer, baselineLatest)) {
                    editor.putString(PREF_LATEST_SERVER_VERSION, baselineLatest)
                    val installed = savedInstalled ?: currentBuildVersion
                    val hasNewer = AppVersionHelper.isVersionNewer(installed, baselineLatest)
                    if (hasNewer) {
                        editor.remove(PREF_DISMISSED_DIALOG_VERSION)
                        editor.putBoolean(PREF_IS_PERMANENTLY_INSTALLED, false)
                    }
                    editor.apply()
                    syncStateToDataStore(
                        installedVersion = installed,
                        lastCheckedVersion = try { sharedPrefs.getString(PREF_LAST_CHECKED_VERSION, baselineLatest) } catch (_: Exception) { baselineLatest } ?: baselineLatest,
                        latestServerVersion = baselineLatest,
                        isPermanent = !hasNewer,
                        clearDismissed = hasNewer
                    )
                }
            }
        } catch (_: Exception) {
            try {
                sharedPrefs.edit()
                    .clear()
                    .putString(PREF_RECORDED_BUILD_VERSION, currentBuildVersion)
                    .putString(PREF_INSTALLED_VERSION, currentBuildVersion)
                    .putString(PREF_LATEST_SERVER_VERSION, currentBuildVersion)
                    .putBoolean(PREF_IS_PERMANENTLY_INSTALLED, true)
                    .putString(PREF_LAST_CHECKED_VERSION, currentBuildVersion)
                    .apply()
            } catch (_: Exception) {
            }
        }
    }

    private fun syncStateToDataStore(
        installedVersion: String,
        lastCheckedVersion: String,
        latestServerVersion: String,
        isPermanent: Boolean,
        clearDismissed: Boolean
    ) {
        scope.launch {
            try {
                context.dataStore.edit { prefs ->
                    prefs[KEY_INSTALLED_VERSION] = installedVersion
                    prefs[KEY_LAST_CHECKED_VERSION] = lastCheckedVersion
                    prefs[KEY_LATEST_SERVER_VERSION] = latestServerVersion
                    prefs[KEY_IS_PERMANENTLY_INSTALLED] = isPermanent
                    if (clearDismissed) {
                        prefs.remove(KEY_DISMISSED_DIALOG_VERSION)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun maxVersion(v1: String, v2: String): String {
        return if (AppVersionHelper.isVersionNewer(v1, v2)) v2 else v1
    }

    /**
     * DataStore Flow for the last checked version from remote update server.
     */
    val lastCheckedVersionFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_LAST_CHECKED_VERSION] ?: try { sharedPrefs.getString(PREF_LAST_CHECKED_VERSION, DEFAULT_LATEST_VERSION) } catch (_: Exception) { DEFAULT_LATEST_VERSION } ?: DEFAULT_LATEST_VERSION
    }

    /**
     * DataStore Flow for the currently recognized installed version.
     */
    val installedVersionFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_INSTALLED_VERSION] ?: try { sharedPrefs.getString(PREF_INSTALLED_VERSION, DEFAULT_LATEST_VERSION) } catch (_: Exception) { DEFAULT_LATEST_VERSION } ?: DEFAULT_LATEST_VERSION
    }

    /**
     * Synchronous getter for current recognized installed version on this device.
     */
    fun getInstalledVersion(): String {
        val buildVersion = try { BuildConfig.VERSION_NAME } catch (_: Exception) { DEFAULT_LATEST_VERSION }
        return try { sharedPrefs.getString(PREF_INSTALLED_VERSION, buildVersion) } catch (_: Exception) { buildVersion } ?: buildVersion
    }

    /**
     * Synchronous getter for latest version detected on remote update server or bundled release.
     */
    fun getLatestServerVersion(): String {
        val saved = try { sharedPrefs.getString(PREF_LATEST_SERVER_VERSION, null) } catch (_: Exception) { null }
        return when {
            saved == null -> DEFAULT_LATEST_VERSION
            AppVersionHelper.isVersionNewer(saved, DEFAULT_LATEST_VERSION) -> {
                try { sharedPrefs.edit().putString(PREF_LATEST_SERVER_VERSION, DEFAULT_LATEST_VERSION).apply() } catch (_: Exception) {}
                DEFAULT_LATEST_VERSION
            }
            else -> saved
        }
    }

    /**
     * Synchronous getter for last checked version.
     */
    fun getLastCheckedVersion(): String {
        return try { sharedPrefs.getString(PREF_LAST_CHECKED_VERSION, "") } catch (_: Exception) { "" } ?: ""
    }

    /**
     * Whether the update has been installed permanently into persistent storage.
     * Returns false if a newer server version is available.
     */
    fun isUpdatePermanentlyInstalled(): Boolean {
        val installed = getInstalledVersion()
        val server = getLatestServerVersion()
        val hasNewer = AppVersionHelper.isVersionNewer(installed, server)
        val permanentFlag = try { sharedPrefs.getBoolean(PREF_IS_PERMANENTLY_INSTALLED, true) } catch (_: Exception) { true }
        return !hasNewer && permanentFlag
    }

    /**
     * Publishes a new version release by the developer (e.g. v1.5.0, v1.6.0, etc.).
     * Sets latest server version and resets dismissal status so update notifications
     * will be triggered for all users on earlier versions.
     */
    fun publishNewVersionRelease(newVersion: String) {
        sharedPrefs.edit()
            .putString(PREF_LATEST_SERVER_VERSION, newVersion)
            .putBoolean(PREF_IS_PERMANENTLY_INSTALLED, false)
            .remove(PREF_DISMISSED_DIALOG_VERSION)
            .remove(PREF_LAST_CHECKED_VERSION)
            .apply()

        scope.launch {
            try {
                context.dataStore.edit { prefs ->
                    prefs[KEY_LATEST_SERVER_VERSION] = newVersion
                    prefs[KEY_IS_PERMANENTLY_INSTALLED] = false
                    prefs.remove(KEY_DISMISSED_DIALOG_VERSION)
                    prefs.remove(KEY_LAST_CHECKED_VERSION)
                }
            } catch (_: Exception) {}
        }
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
     * Developer / QA Simulation: sets the device to simulate an earlier installed version (e.g. 1.4.0 or 1.3.0).
     * This allows full end-to-end verification that a device previously on an earlier version receives,
     * recognizes, downloads, and installs 1.5.0 permanently without offline updates.
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
     * Reset to latest official release state (v1.5.0 permanently applied).
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
