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
 * Layanan Firebase Cloud Messaging (FCM) resmi untuk SIPAGAR KITA BPBD Kabupaten Mukomuko.
 *
 * Mendukung penerimaan notifikasi langsung di HP pengguna TANPA HARUS LOGIN, baik pada
 * Mode Warga (topik `bpbd_public_alerts` & `bpbd_citizen_incidents`) maupun Mode Petugas/TRC
 * (topik `bpbd_officer_alerts`), dengan membedakan porsi kegunaan informasi masing-masing.
 */
class OfficerFcmMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i(TAG, "FCM Token baru diterima untuk perangkat SIPAGAR KITA: ${token.take(12)}...")
        OfficerPushNotificationHelper.saveFcmToken(applicationContext, token)
        OfficerPushNotificationHelper.syncOfficerTopicSubscription(applicationContext)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.i(TAG, "Pesan FCM diterima dari: ${message.from}, data=${message.data}")

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

        // Kirim notifikasi langsung tanpa harus login sesuai porsi kegunaan (Mode Warga vs Mode Petugas)
        OfficerPushNotificationHelper.dispatchRoleAwareNewReportNotification(
            context = applicationContext,
            report = syntheticReport
        )
    }

    companion object {
        private const val TAG = "OfficerFcmService"
    }
}

/**
 * Helper terpusat untuk manajemen langganan topik FCM Publik & Petugas, deduplikasi ID laporan,
 * serta pengiriman Push Notification Laporan Warga Masuk tanpa harus login dengan porsi kegunaan
 * berbeda antara Mode Warga (Kewaspadaan & Evakuasi Publik) dan Mode Petugas (Respon Operasional TRC).
 */
object OfficerPushNotificationHelper {
    private const val TAG = "OfficerPushNotif"
    const val OFFICER_ALERT_TOPIC = "bpbd_officer_alerts"
    const val PUBLIC_ALERT_TOPIC = "bpbd_public_alerts"
    const val CITIZEN_INCIDENT_TOPIC = "bpbd_citizen_incidents"

    const val OFFICER_ALERT_CHANNEL_ID = "bpbd_pusdalops_officer_alerts"
    const val CITIZEN_ALERT_CHANNEL_ID = "bpbd_citizen_public_incident_alerts"

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
     * Memeriksa apakah perangkat saat ini merupakan HP Petugas ASN / Pusdalops BPBD
     * (baik yang sedang membuka konsol Sesi Resmi aktif maupun perangkat resmi petugas
     * yang identitas NIP/kredensialnya sudah terdaftar di perangkat melalui PIN / Biometrik).
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
     * Menyinkronkan langganan topik FCM tanpa mewajibkan login:
     * - Seluruh perangkat otomatis berlangganan topik publik (`bpbd_public_alerts` & `bpbd_citizen_incidents`).
     * - Perangkat yang memilih Porsi Mode Petugas/Gabungan atau terdaftar identitas petugas juga
     *   otomatis berlangganan topik operasional (`bpbd_officer_alerts`) tanpa harus login aktif.
     */
    fun syncOfficerTopicSubscription(context: Context, forceOfficerActive: Boolean? = null) {
        registerAppContext(context)
        val appCtx = context.applicationContext
        val effectivePortion = try {
            com.example.util.DisasterTriageNotificationManager.resolveEffectiveRolePortion(appCtx)
        } catch (_: Throwable) {
            com.example.util.NotificationRolePortion.CITIZEN_PUBLIC
        }
        val shouldSubscribeOfficerTopic = forceOfficerActive
            ?: (isDeviceLoggedInAsOfficer(appCtx) ||
                effectivePortion == com.example.util.NotificationRolePortion.OFFICER_OPERATIONAL ||
                effectivePortion == com.example.util.NotificationRolePortion.DUAL_COMBINED)
        try {
            val messaging = FirebaseMessaging.getInstance()
            // Topik publik warga selalu aktif tanpa login
            messaging.subscribeToTopic(PUBLIC_ALERT_TOPIC)
            messaging.subscribeToTopic(CITIZEN_INCIDENT_TOPIC)

            if (shouldSubscribeOfficerTopic) {
                messaging.subscribeToTopic(OFFICER_ALERT_TOPIC)
                    .addOnSuccessListener {
                        Log.i(TAG, "Perangkat terdaftar pada topik FCM [$OFFICER_ALERT_TOPIC, $PUBLIC_ALERT_TOPIC].")
                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Gagal subscribe topik FCM [$OFFICER_ALERT_TOPIC]: ${e.message}")
                    }
            } else {
                messaging.unsubscribeFromTopic(OFFICER_ALERT_TOPIC)
                    .addOnSuccessListener {
                        Log.i(TAG, "Perangkat aktif pada topik FCM Publik Warga [$PUBLIC_ALERT_TOPIC, $CITIZEN_INCIDENT_TOPIC].")
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
     * Dispatcher cerdas untuk laporan warga baru yang masuk (baik dari real-time Firestore,
     * WorkManager latar belakang saat aplikasi ditutup, maupun FCM) TANPA HARUS LOGIN.
     *
     * Membedakan porsi kegunaan secara tegas:
     * - **Porsi Mode Warga (`CITIZEN_PUBLIC`)**: Fokus informasi kewaspadaan masyarakat sekitar lokasi
     *   kejadian, imbauan menjauhi titik rawan, panduan evakuasi mandiri, dan perlindungan privasi pelapor.
     * - **Porsi Mode Petugas / TRC (`OFFICER_OPERATIONAL`)**: Fokus komando operasional Pusdalops & TRC,
     *   ID tiket laporan, koordinat GPS presisi, rincian korban (Meninggal/Luka/Hilang), kontak pelapor,
     *   dan tombol tindak lanjut verifikasi lapangan tanpa harus membuka sesi login terlebih dahulu.
     * - **Porsi Gabungan (`DUAL_COMBINED`)**: Menampilkan kedua porsi sekaligus.
     */
    fun dispatchRoleAwareNewReportNotification(
        context: Context,
        report: CitizenDisasterReport,
        bypassDuplicateCheckForTest: Boolean = false
    ): Boolean {
        val appCtx = context.applicationContext
        cachedAppContext = appCtx

        // Bila dalam mode unit test spesifik officer session override, delegasikan ke notifyOfficerForNewReport
        if (overrideOfficerSessionForTesting != null) {
            return notifyOfficerForNewReport(appCtx, report)
        }

        val settings = com.example.util.DisasterTriageNotificationManager.getSettings(appCtx)
        if (!settings.isEnabled && !bypassDuplicateCheckForTest) {
            return false
        }

        val effectivePortion = com.example.util.DisasterTriageNotificationManager.resolveEffectiveRolePortion(appCtx, settings)
        return when (effectivePortion) {
            com.example.util.NotificationRolePortion.OFFICER_OPERATIONAL ->
                notifyOfficerStandbyForNewReport(
                    context = appCtx,
                    report = report,
                    includeCitizenSection = false,
                    bypassDuplicateCheck = bypassDuplicateCheckForTest
                )
            com.example.util.NotificationRolePortion.DUAL_COMBINED ->
                notifyOfficerStandbyForNewReport(
                    context = appCtx,
                    report = report,
                    includeCitizenSection = true,
                    bypassDuplicateCheck = bypassDuplicateCheckForTest
                )
            else ->
                notifyCitizenPublicForNewReport(
                    context = appCtx,
                    report = report,
                    bypassDuplicateCheck = bypassDuplicateCheckForTest
                )
        }
    }

    /**
     * Mengirim Push Notification Laporan Warga Masuk dengan **PORSI MODE WARGA (PUBLIK)** tanpa login.
     * Melindungi data pribadi/nomor HP pelapor, namun memberikan peringatan kewaspadaan lokasi kejadian,
     * panduan keselamatan diri, dan akses cepat ke Peta/Pantauan Warga & Call Center 117.
     */
    fun notifyCitizenPublicForNewReport(
        context: Context,
        report: CitizenDisasterReport,
        bypassDuplicateCheck: Boolean = false
    ): Boolean {
        val appCtx = context.applicationContext
        cachedAppContext = appCtx
        if (isInvalidOrSeedReport(report)) return false
        if (!bypassDuplicateCheck && hasReportBeenNotified(appCtx, report.id)) {
            return false
        }

        ensureNotificationChannels(appCtx)
        if (!bypassDuplicateCheck) {
            markReportNotified(appCtx, report.id)
        }

        val openTabIntent = Intent(appCtx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(com.example.util.DisasterTriageNotificationManager.EXTRA_NAVIGATE_TAB, 1)
            putExtra("extra_open_report_id", report.id)
        }
        val openPendingIntent = PendingIntent.getActivity(
            appCtx,
            report.id.hashCode() + 11,
            openTabIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dial117Intent = Intent(Intent.ACTION_DIAL, android.net.Uri.parse("tel:117")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val dial117PendingIntent = PendingIntent.getActivity(
            appCtx,
            report.id.hashCode() + 19,
            dial117Intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val gpsBadge = if (report.isGpsVerified) "Terverifikasi GPS" else "Laporan Warga"
        val title = "📢 [PORSI WARGA] Laporan Kejadian ${report.disasterType} • Kec. ${report.district}"
        val shortBody = "Desa ${report.village.ifBlank { "-" }}, Kec. ${report.district} (${report.incidentLocation}) • Hindari titik rawan & tetap waspada."
        val detailedBody = buildString {
            appendLine("Porsi Notifikasi: PORSI MODE WARGA (KEWASPADAAN & EVAKUASI PUBLIK)")
            appendLine("Jenis Kejadian: ${report.disasterType} ($gpsBadge)")
            appendLine("Wilayah Terdampak: Desa ${report.village.ifBlank { "-" }}, Kec. ${report.district}")
            appendLine("Titik Lokasi: ${report.incidentLocation}")
            if (report.additionalNotes.isNotBlank()) {
                appendLine("Situasi Lapangan: ${report.additionalNotes}")
            }
            appendLine()
            appendLine("Panduan Keselamatan Warga (Tanpa Perlu Login):")
            appendLine("• Hindari mendekati titik lokasi kejadian agar aman dari bahaya susulan & tidak menghambat jalur evakuasi TRC BPBD.")
            appendLine("• Siapkan Tas Siaga Bencana (TSB) bila berada di radius rawan sekitar Desa ${report.village.ifBlank { report.district }}.")
            append("• Hubungi Call Center Darurat 117 (Bebas Pulsa) jika membutuhkan bantuan evakuasi.")
        }

        val notification = NotificationCompat.Builder(appCtx, CITIZEN_ALERT_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(shortBody)
            .setStyle(NotificationCompat.BigTextStyle().bigText(detailedBody))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setColor(0xFFEA580C.toInt())
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .addAction(android.R.drawable.ic_menu_mapmode, "Pantauan & Peta Warga", openPendingIntent)
            .addAction(android.R.drawable.ic_menu_call, "Panggil 117", dial117PendingIntent)
            .setVibrate(longArrayOf(0, 300, 150, 300))
            .build()

        postSystemNotificationSafely(appCtx, report.id.hashCode(), notification)
        Log.i(TAG, "Notifikasi Porsi Mode Warga laporan baru [${report.id}] terkirim tanpa login.")
        return true
    }

    /**
     * Mengirim Push Notification Laporan Warga Masuk dengan **PORSI MODE PETUGAS / TRC SIAGA**
     * tanpa mengharuskan petugas sedang membuka sesi login di aplikasi.
     */
    fun notifyOfficerStandbyForNewReport(
        context: Context,
        report: CitizenDisasterReport,
        includeCitizenSection: Boolean = false,
        bypassDuplicateCheck: Boolean = false
    ): Boolean {
        val appCtx = context.applicationContext
        cachedAppContext = appCtx
        if (isInvalidOrSeedReport(report)) return false
        if (!bypassDuplicateCheck && hasReportBeenNotified(appCtx, report.id)) {
            return false
        }

        ensureNotificationChannels(appCtx)
        if (!bypassDuplicateCheck) {
            markReportNotified(appCtx, report.id)
        }

        val intent = Intent(appCtx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(com.example.util.DisasterTriageNotificationManager.EXTRA_NAVIGATE_TAB, 1)
            putExtra("extra_open_report_id", report.id)
            putExtra("extra_open_officer_reports", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            appCtx,
            report.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dial117Intent = Intent(Intent.ACTION_DIAL, android.net.Uri.parse("tel:117")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val dial117PendingIntent = PendingIntent.getActivity(
            appCtx,
            report.id.hashCode() + 29,
            dial117Intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val hasPhotoLabel = if (!report.photoUri.isNullOrBlank()) "📸 Foto GPS Terlampir" else "Tanpa Foto"
        val coordsText = String.format(java.util.Locale.US, "%.4f, %.4f", report.latitude, report.longitude)
        val casualtySummary = "MD: ${report.casualtiesDead} • Luka: ${report.casualtiesInjured} • Hilang: ${report.casualtiesMissing}"
        val prefix = if (includeCitizenSection) "🚨 [WARGA & TRC]" else "🚨 [PORSI PETUGAS/TRC]"
        val title = "$prefix LAPORAN DARURAT: ${report.disasterType.uppercase()}"
        val shortBody = "[${report.id}] Desa ${report.village.ifBlank { "-" }}, Kec. ${report.district} • $casualtySummary • $hasPhotoLabel"
        val detailedBody = buildString {
            appendLine(
                if (includeCitizenSection) {
                    "Porsi Notifikasi: GABUNGAN (OPERASIONAL PETUGAS/TRC + KEWASPADAAN WARGA)"
                } else {
                    "Porsi Notifikasi: PORSI MODE PETUGAS / TRC BPBD (SIAGA TANPA LOGIN)"
                }
            )
            appendLine("ID Tiket: ${report.id} • Status: ${report.status}")
            appendLine("Wilayah: Desa ${report.village.ifBlank { "-" }}, Kec. ${report.district}")
            appendLine("Titik Lokasi: ${report.incidentLocation} (GPS: $coordsText)")
            appendLine("Triase Dampak Korban: $casualtySummary")
            appendLine("Pelapor: ${report.reporterName} (${report.reporterPhone.ifBlank { "Tanpa No. HP" }})")
            appendLine("Bukti Lampiran: $hasPhotoLabel")
            appendLine()
            appendLine("Instruksi Operasional Petugas / TRC:")
            append("• Lakukan verifikasi lapangan & validasi bukti foto GPS, siagakan personel TRC menuju titik koordinat, dan perbarui status penanganan Pusdalops.")
            if (includeCitizenSection) {
                appendLine()
                appendLine()
                appendLine("Imbauan Keselamatan Warga Sekitar:")
                append("• Jauhi radius bahaya di Desa ${report.village.ifBlank { report.district }}, amankan keluarga, dan beri akses prioritas bagi kendaraan operasional BPBD.")
            }
        }

        val notification = NotificationCompat.Builder(appCtx, OFFICER_ALERT_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(shortBody)
            .setStyle(NotificationCompat.BigTextStyle().bigText(detailedBody))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setColor(0xFFDC2626.toInt())
            .setColorized(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_view, "Verifikasi & Tindak Lanjut", pendingIntent)
            .addAction(android.R.drawable.ic_menu_call, "Hubungi Posko 117", dial117PendingIntent)
            .setVibrate(longArrayOf(0, 500, 200, 500, 200, 650))
            .build()

        postSystemNotificationSafely(appCtx, report.id.hashCode(), notification)
        try {
            com.example.ui.components.OfficerAlarmPlayer.triggerSirenAndVibration(
                context = appCtx,
                report = report,
                dispatchSystemNotification = false
            )
        } catch (_: Throwable) {
        }
        Log.i(TAG, "Notifikasi Porsi Mode Petugas/TRC laporan warga baru [${report.id}] terkirim real-time.")
        return true
    }

    /**
     * Mengirim Push Notification Darurat secara real-time khusus untuk perangkat yang sedang
     * dalam sesi/identitas Petugas Pusdalops. Dipertahankan kompatibilitasnya untuk pengujian
     * isolasi sesi petugas (`CitizenReportPhotoUploadAndDisplayTest`).
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
        return notifyOfficerStandbyForNewReport(
            context = appCtx,
            report = report,
            includeCitizenSection = false,
            bypassDuplicateCheck = false
        )
    }

    private fun isInvalidOrSeedReport(report: CitizenDisasterReport): Boolean {
        if (report.id.isBlank() || report.id.startsWith("LAP-MMK-20261025-00")) {
            return true
        }
        if (com.example.data.local.FirestoreHelper.isLegacyPreV110SimulationReport(report.id, report.status)) {
            return true
        }
        return false
    }

    private fun postSystemNotificationSafely(
        appCtx: Context,
        notificationId: Int,
        notification: android.app.Notification
    ) {
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
                NotificationManagerCompat.from(appCtx).notify(notificationId, notification)
            } else {
                val nm = appCtx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                nm?.notify(notificationId, notification)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Gagal menampilkan push notification sistem: ${e.message}")
        }
    }

    fun ensureNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            if (nm.getNotificationChannel(OFFICER_ALERT_CHANNEL_ID) == null) {
                val officerChannel = NotificationChannel(
                    OFFICER_ALERT_CHANNEL_ID,
                    "Porsi Petugas/TRC • Laporan Darurat & Respon Lapangan",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifikasi real-time Porsi Mode Petugas/TRC saat laporan bencana warga baru masuk (tanpa harus membuka sesi login)"
                    enableLights(true)
                    lightColor = android.graphics.Color.RED
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 650)
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                }
                nm.createNotificationChannel(officerChannel)
            }
            if (nm.getNotificationChannel(CITIZEN_ALERT_CHANNEL_ID) == null) {
                val citizenChannel = NotificationChannel(
                    CITIZEN_ALERT_CHANNEL_ID,
                    "Porsi Warga • Info Kejadian & Kewaspadaan Publik",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifikasi real-time Porsi Mode Warga saat terdapat laporan kejadian bencana di sekitar wilayah Mukomuko tanpa harus login"
                    enableLights(true)
                    lightColor = 0xFFEA580C.toInt()
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 300, 150, 300)
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                }
                nm.createNotificationChannel(citizenChannel)
            }
        }
    }
}
