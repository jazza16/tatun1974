package com.example.data.sync

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.data.auth.AuthManager
import com.example.data.local.OfficerAuthManager
import com.example.model.CitizenDisasterReport
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Layanan Firebase Cloud Messaging (FCM) resmi untuk Pusdalops BPBD Kabupaten Mukomuko.
 *
 * Setiap kali ada dokumen baru di koleksi `citizen_disaster_reports` (baik yang dikirim melalui
 * payload topik FCM `bpbd_officer_alerts` maupun terdeteksi real-time dari koleksi Firestore),
 * notifikasi darurat prioritas tinggi dikirimkan KHUSUS ke perangkat yang sedang login sebagai
 * Petugas / Pusdalops.
 */
class OfficerFcmMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i(TAG, "FCM Token baru diterima untuk perangkat BPBD: ${token.take(12)}...")
        OfficerPushNotificationHelper.saveFcmToken(applicationContext, token)
        OfficerPushNotificationHelper.syncOfficerTopicSubscription(applicationContext)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.i(TAG, "Pesan FCM diterima dari: ${message.from}, data=${message.data}")

        if (!OfficerPushNotificationHelper.isDeviceLoggedInAsOfficer(applicationContext)) {
            Log.d(TAG, "Mengabaikan push notification FCM petugas karena perangkat tidak dalam sesi login Petugas/Pusdalops.")
            return
        }

        val data = message.data
        val reportId = data["reportId"]
            ?: data["id"]
            ?: "LAP-MMK-FCM-${System.currentTimeMillis()}"
        val disasterType = data["disasterType"]
            ?: message.notification?.title
            ?: "Laporan Bencana Warga"
        val district = data["district"] ?: "Kota Mukomuko"
        val village = data["village"] ?: "-"
        val incidentLocation = data["incidentLocation"]
            ?: message.notification?.body
            ?: "Lokasi dilaporkan warga"
        val reporterName = data["reporterName"] ?: "Warga Mukomuko"
        val reporterPhone = data["reporterPhone"] ?: "-"
        val photoUrl = data["fotoUrl"] ?: data["fotoLampiran"] ?: data["photoUri"]
        val timestamp = data["timestamp"]?.toLongOrNull() ?: System.currentTimeMillis()

        val syntheticReport = CitizenDisasterReport(
            id = reportId,
            disasterType = disasterType,
            incidentTime = data["incidentTime"] ?: "Baru saja",
            incidentLocation = incidentLocation,
            district = district,
            village = village,
            latitude = data["latitude"]?.toDoubleOrNull() ?: -2.5833,
            longitude = data["longitude"]?.toDoubleOrNull() ?: 101.1167,
            gpsAccuracyMeters = data["gpsAccuracyMeters"]?.toFloatOrNull(),
            isGpsVerified = data["isGpsVerified"]?.toBooleanStrictOrNull() ?: true,
            casualtiesDead = data["casualtiesDead"]?.toIntOrNull() ?: 0,
            casualtiesInjured = data["casualtiesInjured"]?.toIntOrNull() ?: 0,
            casualtiesMissing = data["casualtiesMissing"]?.toIntOrNull() ?: 0,
            photoUri = photoUrl,
            reporterName = reporterName,
            reporterPhone = reporterPhone,
            additionalNotes = data["additionalNotes"] ?: "",
            timestamp = timestamp,
            status = data["status"] ?: "Diproses",
            updatedAt = timestamp,
            isSyncedToCloud = true
        )

        OfficerPushNotificationHelper.notifyOfficerForNewReport(
            context = applicationContext,
            report = syntheticReport
        )
    }

    companion object {
        private const val TAG = "OfficerFcmService"
    }
}

/**
 * Helper terpusat untuk manajemen langganan topik FCM Petugas (`bpbd_officer_alerts`),
 * validasi sesi login Petugas/Pusdalops, deduplikasi ID laporan, dan pengiriman
 * Push Notification Darurat ke bilah status perangkat petugas.
 */
object OfficerPushNotificationHelper {
    private const val TAG = "OfficerPushNotif"
    const val OFFICER_ALERT_TOPIC = "bpbd_officer_alerts"
    const val OFFICER_ALERT_CHANNEL_ID = "bpbd_pusdalops_officer_alerts"
    private const val PREFS_NAME = "bpbd_officer_fcm_alerts"
    private const val KEY_FCM_TOKEN = "fcm_registration_token"
    private const val KEY_NOTIFIED_REPORT_IDS = "notified_report_ids"
    private const val KEY_BASELINE_INITIALIZED = "baseline_initialized"

    @Volatile
    var overrideOfficerSessionForTesting: Boolean? = null

    @Volatile
    private var cachedAppContext: Context? = null

    fun registerAppContext(context: Context) {
        cachedAppContext = context.applicationContext
    }

    fun getAppContext(): Context? = cachedAppContext

    /**
     * Memeriksa apakah perangkat saat ini merupakan HP Asli Petugas ASN / Pusdalops BPBD
     * (baik yang sedang membuka konsol Sesi Resmi aktif maupun perangkat resmi petugas
     * yang identitas NIP/kredensialnya sudah terdaftar di perangkat melalui PIN / Biometrik).
     * Dengan demikian notifikasi darurat diterima real-time tanpa harus menunggu petugas
     * membuka sesi login Resmi.
     */
    fun isDeviceLoggedInAsOfficer(context: Context): Boolean {
        overrideOfficerSessionForTesting?.let { return it }
        val appCtx = context.applicationContext
        cachedAppContext = appCtx
        val isRegisteredOrActiveOfficerDevice = try {
            val officerAuth = OfficerAuthManager(appCtx)
            officerAuth.isOfficerLoggedIn() ||
                officerAuth.hasSavedOfficerIdentity() ||
                officerAuth.getOfficerBadgeId().isNotBlank()
        } catch (_: Exception) {
            false
        }
        if (isRegisteredOrActiveOfficerDevice) return true

        val pusdalopsSessionActive = try {
            AuthManager.getInstance(appCtx).isSessionActive()
        } catch (_: Exception) {
            false
        }
        return pusdalopsSessionActive
    }

    /**
     * Menyimpan token FCM perangkat untuk keperluan diagnostik dan pengiriman terarah.
     */
    fun saveFcmToken(context: Context, token: String) {
        if (token.isBlank()) return
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_FCM_TOKEN, token.trim())
            .apply()
    }

    fun getSavedFcmToken(context: Context): String? {
        return context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_FCM_TOKEN, null)
    }

    /**
     * Menyinkronkan langganan topik FCM `bpbd_officer_alerts`.
     * - Selama perangkat terdaftar sebagai HP Petugas/Pusdalops: otomatis `subscribeToTopic("bpbd_officer_alerts")`.
     * - Hanya saat identitas akun petugas dihapus dari perangkat: `unsubscribeFromTopic("bpbd_officer_alerts")`.
     */
    fun syncOfficerTopicSubscription(context: Context, forceOfficerActive: Boolean? = null) {
        registerAppContext(context)
        val isOfficer = forceOfficerActive ?: isDeviceLoggedInAsOfficer(context)
        try {
            val messaging = FirebaseMessaging.getInstance()
            if (isOfficer) {
                messaging.subscribeToTopic(OFFICER_ALERT_TOPIC)
                    .addOnSuccessListener {
                        Log.i(TAG, "Perangkat Petugas terdaftar pada topik FCM [$OFFICER_ALERT_TOPIC].")
                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Gagal subscribe topik FCM [$OFFICER_ALERT_TOPIC]: ${e.message}")
                    }
            } else {
                messaging.unsubscribeFromTopic(OFFICER_ALERT_TOPIC)
                    .addOnSuccessListener {
                        Log.i(TAG, "Perangkat keluar dari topik FCM [$OFFICER_ALERT_TOPIC] (mode Warga).")
                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Gagal unsubscribe topik FCM [$OFFICER_ALERT_TOPIC]: ${e.message}")
                    }
            }
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseMessaging belum tersedia di environment ini: ${e.message}")
        }
    }

    /**
     * Mencatat kumpulan ID laporan awal (baseline) yang sudah ada di server saat aplikasi pertama kali dipasang
     * agar hanya dokumen laporan BARU di `citizen_disaster_reports` yang membunyikan push notification.
     */
    fun registerInitialBaselineReports(context: Context, existingReportIds: Collection<String>) {
        registerAppContext(context)
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentSet = prefs.getStringSet(KEY_NOTIFIED_REPORT_IDS, emptySet())?.toMutableSet() ?: mutableSetOf()
        currentSet.addAll(existingReportIds.filter { it.isNotBlank() })
        prefs.edit()
            .putStringSet(KEY_NOTIFIED_REPORT_IDS, currentSet)
            .putBoolean(KEY_BASELINE_INITIALIZED, true)
            .apply()
    }

    fun isBaselineInitialized(context: Context): Boolean {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_BASELINE_INITIALIZED, false)
    }

    fun hasReportBeenNotified(context: Context, reportId: String): Boolean {
        if (reportId.isBlank()) return true
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val notified = prefs.getStringSet(KEY_NOTIFIED_REPORT_IDS, emptySet()).orEmpty()
        return notified.contains(reportId)
    }

    fun markReportNotified(context: Context, reportId: String) {
        if (reportId.isBlank()) return
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val updated = prefs.getStringSet(KEY_NOTIFIED_REPORT_IDS, emptySet())?.toMutableSet() ?: mutableSetOf()
        updated.add(reportId)
        // Jaga ukuran set tetap ringkas (maks 400 ID terbaru)
        val trimmed = if (updated.size > 400) updated.toList().takeLast(400).toSet() else updated
        prefs.edit().putStringSet(KEY_NOTIFIED_REPORT_IDS, trimmed).apply()
    }

    fun clearNotificationHistoryForTesting(context: Context) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }

    /**
     * Mengirim Push Notification Darurat secara real-time ke HP Asli Petugas / Pusdalops
     * setiap kali ada dokumen baru di koleksi `citizen_disaster_reports` tanpa menunggu
     * petugas membuka sesi login Resmi.
     *
     * @return `true` jika push notification berhasil diterbitkan ke perangkat petugas,
     *         `false` jika perangkat bukan HP petugas atau laporan sudah pernah dinotifikasi.
     */
    fun notifyOfficerForNewReport(
        context: Context,
        report: CitizenDisasterReport
    ): Boolean {
        val appCtx = context.applicationContext
        cachedAppContext = appCtx
        if (!isDeviceLoggedInAsOfficer(appCtx)) {
            return false
        }
        if (report.id.isBlank() || report.id.startsWith("LAP-MMK-20261025-00")) {
            // Abaikan data seed contoh statis
            return false
        }
        if (com.example.data.local.FirestoreHelper.isLegacyPreV110SimulationReport(report.id, report.status)) {
            return false
        }
        if (hasReportBeenNotified(appCtx, report.id)) {
            return false
        }

        ensureNotificationChannel(appCtx)
        markReportNotified(appCtx, report.id)

        val intent = Intent(appCtx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("extra_open_report_id", report.id)
            putExtra("extra_open_officer_reports", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            appCtx,
            report.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val hasPhotoLabel = if (!report.photoUri.isNullOrBlank()) "📸 Foto GPS Terlampir" else "Tanpa Foto"
        val title = "🚨 LAPORAN DARURAT WARGA: ${report.disasterType.uppercase()}"
        val shortBody = "[${report.id}] Desa ${report.village.ifBlank { "-" }}, Kec. ${report.district} • $hasPhotoLabel"
        val detailedBody = buildString {
            appendLine("ID Laporan: ${report.id}")
            appendLine("Wilayah: Desa ${report.village.ifBlank { "-" }}, Kec. ${report.district}")
            appendLine("Titik Lokasi: ${report.incidentLocation}")
            appendLine("Pelapor: ${report.reporterName} (${report.reporterPhone.ifBlank { "Tanpa No. HP" }})")
            append("Bukti Foto: $hasPhotoLabel • Ketuk untuk tindak lanjut Pusdalops.")
        }

        val notification = NotificationCompat.Builder(appCtx, OFFICER_ALERT_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(shortBody)
            .setStyle(NotificationCompat.BigTextStyle().bigText(detailedBody))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 500, 200, 500, 200, 650))
            .build()

        try {
            val hasPerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    appCtx,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
            if (hasPerm) {
                NotificationManagerCompat.from(appCtx).notify(report.id.hashCode(), notification)
            } else {
                val nm = appCtx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                nm?.notify(report.id.hashCode(), notification)
            }
            try {
                com.example.ui.components.OfficerAlarmPlayer.triggerSirenAndVibration(
                    context = appCtx,
                    report = report,
                    dispatchSystemNotification = false
                )
            } catch (_: Throwable) {
            }
            Log.i(TAG, "Push notification laporan warga baru [${report.id}] terkirim real-time ke HP Asli Petugas/Pusdalops.")
        } catch (e: Exception) {
            Log.w(TAG, "Gagal menampilkan push notification sistem untuk [${report.id}]: ${e.message}")
        }
        return true
    }

    private fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val existing = nm.getNotificationChannel(OFFICER_ALERT_CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    OFFICER_ALERT_CHANNEL_ID,
                    "Peringatan Laporan Darurat Warga (Petugas Pusdalops)",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifikasi real-time FCM & Firestore khusus HP Asli Petugas/Pusdalops saat laporan bencana warga baru masuk"
                    enableLights(true)
                    lightColor = android.graphics.Color.RED
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 650)
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                }
                nm.createNotificationChannel(channel)
            }
        }
    }
}
