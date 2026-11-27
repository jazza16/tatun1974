package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.sync.OfficerPushNotificationHelper
import com.example.util.DisasterTriageNotificationManager
import com.example.util.EarthquakeNotificationHelper
import com.example.util.WeatherNotificationHelper

/**
 * BroadcastReceiver otomatis saat perangkat dinyalakan ulang (`BOOT_COMPLETED`) atau
 * aplikasi diperbarui (`MY_PACKAGE_REPLACED`).
 *
 * Memastikan kanal notifikasi, langganan topik FCM (Mode Warga & Mode Petugas tanpa login),
 * serta WorkManager pemantauan latar belakang 24 jam langsung berjalan otomatis tanpa
 * mengharuskan pengguna membuka aplikasi terlebih dahulu.
 */
class SipagarBootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "android.intent.action.LOCKED_BOOT_COMPLETED"
        ) {
            val appCtx = context.applicationContext
            Log.i(TAG, "Menerima sinyal sistem [$action] -> Mengaktifkan pemantauan latar belakang 24 jam SIPAGAR KITA tanpa login.")
            try {
                DisasterTriageNotificationManager.initNotificationChannels(appCtx)
                OfficerPushNotificationHelper.ensureNotificationChannels(appCtx)
                EarthquakeNotificationHelper.initNotificationChannel(appCtx)
                WeatherNotificationHelper.initNotificationChannel(appCtx)
                OfficerPushNotificationHelper.registerAppContext(appCtx)
                OfficerPushNotificationHelper.syncOfficerTopicSubscription(appCtx)
                SipagarBackgroundAlertScheduler.scheduleBackgroundMonitoring(
                    context = appCtx,
                    triggerImmediateCheck = true
                )
                KarhutlaSyncScheduler.schedulePeriodicSync(appCtx)
            } catch (e: Throwable) {
                Log.w(TAG, "Inisialisasi pasca-boot selesai sebagian: ${e.message}")
            }
        }
    }

    companion object {
        private const val TAG = "SipagarBootReceiver"
    }
}
