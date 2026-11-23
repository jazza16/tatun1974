package com.example.util

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.work.WorkManager
import com.example.BuildConfig
import com.example.data.local.AppDatabase
import com.example.security.SecureStorageHelper
import java.io.File

/**
 * Menangani pembersihan cache otomatis, migrasi database SQLite/Room yang aman,
 * serta sanitasi SharedPreferences & WorkManager saat aplikasi pertama kali dijalankan
 * setelah instalasi pembaruan (update APK).
 *
 * Mencegah Force Close pasca-update tanpa mengharuskan pengguna melakukan
 * "Hapus Cache" dan "Hapus Data" secara manual di pengaturan HP.
 */
object AppUpgradeSanitizer {
    private const val TAG = "AppUpgradeSanitizer"
    private const val PREFS_UPGRADE_GUARD = "sipagar_app_upgrade_guard_prefs"
    private const val KEY_LAST_VERSION_CODE = "last_sanitized_version_code"
    private const val KEY_LAST_VERSION_NAME = "last_sanitized_version_name"
    private const val UPDATE_NOTIFICATION_ID = 2026

    /**
     * Wajib dipanggil paling awal di `MainActivity.onCreate()` sebelum inisialisasi
     * Room Database, WorkManager, Firebase, atau ViewModel.
     */
    fun runOnStartupIfUpdated(context: Context, launchIntent: Intent? = null) {
        val appContext = context.applicationContext
        try {
            val guardPrefs = appContext.getSharedPreferences(PREFS_UPGRADE_GUARD, Context.MODE_PRIVATE)
            val currentVersionCode = BuildConfig.VERSION_CODE
            val currentVersionName = BuildConfig.VERSION_NAME

            val recordedVersionCode = try {
                guardPrefs.getInt(KEY_LAST_VERSION_CODE, -1)
            } catch (_: Exception) {
                -1
            }
            val recordedVersionName = try {
                guardPrefs.getString(KEY_LAST_VERSION_NAME, null)
            } catch (_: Exception) {
                null
            }

            val isUpgradeOrFirstSanitize =
                recordedVersionCode != currentVersionCode || recordedVersionName != currentVersionName

            if (isUpgradeOrFirstSanitize) {
                Log.i(
                    TAG,
                    "Deteksi pembaruan aplikasi ($recordedVersionName [$recordedVersionCode] -> $currentVersionName [$currentVersionCode]). Menjalankan auto-sanitasi..."
                )

                // 1. Bersihkan direktori cache aplikasi (setara dengan "Hapus Cache" otomatis)
                clearApplicationCaches(appContext)

                // 2. Bersihkan notifikasi update yang masih menggantung di status bar
                try {
                    NotificationManagerCompat.from(appContext).cancel(UPDATE_NOTIFICATION_ID)
                } catch (_: Exception) {
                }

                // 3. Hapus extra intent update lama agar tidak memicu loop update sesaat setelah APK baru terbuka
                try {
                    launchIntent?.removeExtra("EXTRA_OPEN_UPDATE_SHEET")
                    launchIntent?.removeExtra("EXTRA_AUTO_START_UPDATE")
                } catch (_: Exception) {
                }

                // 4. Sanitasi SharedPreferences agar bebas dari ClassCastException atau token Play Integrity usang
                sanitizeSharedPreferencesOnUpgrade(appContext, currentVersionName)

                // 5. Reset & bersihkan antrean WorkManager lama yang mungkin menyimpan nama kelas ter-obfuscate R8 dari versi lama
                resetStaleWorkManagerJobs(appContext)

                // 6. Pastikan database Room SQLite lama yang berbeda hash skema di-reset dengan aman jika berasal dari versi < 8
                if (recordedVersionCode < 8) {
                    safelyResetLegacyDatabaseFiles(appContext)
                }

                // 7. Simpan penanda bahwa versi ini sudah disanitasi secara sinkron
                guardPrefs.edit()
                    .putInt(KEY_LAST_VERSION_CODE, currentVersionCode)
                    .putString(KEY_LAST_VERSION_NAME, currentVersionName)
                    .commit()

                Log.i(TAG, "Auto-sanitasi pasca-update ke v$currentVersionName ($currentVersionCode) selesai dengan sukses.")
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Gagal menjalankan AppUpgradeSanitizer: ${t.message}", t)
        }
    }

    private fun clearApplicationCaches(context: Context) {
        try {
            deleteChildrenSafely(context.cacheDir)
        } catch (_: Exception) {
        }
        try {
            deleteChildrenSafely(context.codeCacheDir)
        } catch (_: Exception) {
        }
        try {
            context.externalCacheDir?.let { deleteChildrenSafely(it) }
        } catch (_: Exception) {
        }
    }

    private fun deleteChildrenSafely(dir: File?) {
        if (dir == null || !dir.exists() || !dir.isDirectory) return
        val children = dir.listFiles() ?: return
        for (child in children) {
            try {
                // Jangan hapus cache internal Firestore yang sedang aktif bila ada
                if (child.name.contains("firestore", ignoreCase = true)) continue
                child.deleteRecursively()
            } catch (_: Exception) {
            }
        }
    }

    private fun sanitizeSharedPreferencesOnUpgrade(context: Context, currentVersionName: String) {
        // A. Sanitasi mukomuko_update_persistence agar langsung mengenali versi baru yang sudah terinstal
        try {
            val updatePrefs = context.getSharedPreferences("mukomuko_update_persistence", Context.MODE_PRIVATE)
            updatePrefs.edit()
                .clear()
                .putString("recorded_build_version", currentVersionName)
                .putString("installed_version", currentVersionName)
                .putString("latest_server_version", currentVersionName)
                .putString("last_checked_version", currentVersionName)
                .putBoolean("is_permanently_installed", true)
                .putString("dismissed_dialog_version", currentVersionName)
                .commit()
        } catch (e: Exception) {
            Log.w(TAG, "Gagal mereset mukomuko_update_persistence: ${e.message}")
        }

        // B. Hapus file DataStore update lama jika korup atau menyimpan state versi lama yang memicu konflik
        try {
            val dataStoreFile = File(context.filesDir, "datastore/mukomuko_app_update_prefs.preferences_pb")
            if (dataStoreFile.exists()) {
                dataStoreFile.delete()
            }
        } catch (_: Exception) {
        }

        // C. Bersihkan cache sesi Play Integrity lama di bpbd_security_manager_prefs
        try {
            val securityPrefs = context.getSharedPreferences("bpbd_security_manager_prefs", Context.MODE_PRIVATE)
            securityPrefs.edit().clear().commit()
        } catch (_: Exception) {
        }

        // D. Validasi bpbd_officer_auth_prefs tanpa menghapus roster personil, status PIN custom Cloud, atau identitas petugas
        try {
            val authPrefs = context.getSharedPreferences("bpbd_officer_auth_prefs", Context.MODE_PRIVATE)
            val savedNip = try {
                SecureStorageHelper.getDecrypted(authPrefs, "saved_nip")?.trim().orEmpty()
                    .ifBlank { SecureStorageHelper.getDecrypted(authPrefs, "officer_badge_id")?.trim().orEmpty() }
            } catch (_: Exception) {
                ""
            }
            val officerName = try {
                SecureStorageHelper.getDecrypted(authPrefs, "officer_name")?.trim().orEmpty()
            } catch (_: Exception) {
                ""
            }
            val officerRole = try {
                SecureStorageHelper.getDecrypted(authPrefs, "officer_role")?.trim().orEmpty()
            } catch (_: Exception) {
                ""
            }
            val officerShift = try {
                SecureStorageHelper.getDecrypted(authPrefs, "officer_shift")?.trim().orEmpty()
            } catch (_: Exception) {
                ""
            }
            val isLoggedIn = try {
                authPrefs.getBoolean("is_officer_logged_in", false)
            } catch (_: Exception) {
                false
            }
            val hasCloudCustomPin = try {
                authPrefs.getBoolean("has_cloud_custom_pin", false)
            } catch (_: Exception) {
                false
            }

            // Hapus HANYA key PIN lokal usang/plaintext (JANGAN clear() seluruh prefs agar roster & status PIN custom Cloud tetap utuh)
            val editor = authPrefs.edit()
                .remove("officer_pin")
                .remove("officer_pin_hash")
                .remove("officer_pin_salt")
                .remove("hitatun_arazak_normalized_v1_clean")
                .remove("hitatun_arazak_normalized_v1_cloud")
                .putBoolean("is_officer_logged_in", isLoggedIn)
                .putBoolean("has_cloud_custom_pin", hasCloudCustomPin)
                .putInt("officer_storage_schema_version", 3)

            if (savedNip.isNotBlank()) {
                SecureStorageHelper.putEncrypted(editor, "saved_nip", savedNip)
                SecureStorageHelper.putEncrypted(editor, "officer_badge_id", savedNip)
            }
            if (officerName.isNotBlank()) {
                SecureStorageHelper.putEncrypted(editor, "officer_name", officerName)
            }
            if (officerRole.isNotBlank()) {
                SecureStorageHelper.putEncrypted(editor, "officer_role", officerRole)
            }
            if (officerShift.isNotBlank()) {
                SecureStorageHelper.putEncrypted(editor, "officer_shift", officerShift)
            }
            editor.commit()
        } catch (e: Exception) {
            Log.w(TAG, "Sanitasi bpbd_officer_auth_prefs fallback: ${e.message}")
        }
    }

    private fun resetStaleWorkManagerJobs(context: Context) {
        try {
            val wm = WorkManager.getInstance(context)
            wm.cancelAllWork()
            wm.pruneWork()
        } catch (e: Exception) {
            Log.w(TAG, "WorkManager prune dilewati: ${e.message}")
        }
    }

    private fun safelyResetLegacyDatabaseFiles(context: Context) {
        try {
            AppDatabase.resetAndDeleteDatabase(context)
        } catch (e: Exception) {
            Log.w(TAG, "Reset database legacy dilewati: ${e.message}")
        }
    }
}
