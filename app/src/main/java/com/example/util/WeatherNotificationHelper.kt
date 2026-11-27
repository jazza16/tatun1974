package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.model.DetailedEarlyWarning

data class WeatherAlertSettings(
    val isEnabled: Boolean = true,
    val alertThunderstorm: Boolean = true,
    val alertStrongWind: Boolean = true,
    val alertFloodHeavyRain: Boolean = true,
    val alertHighWaves: Boolean = true,
    val onlySelectedDistrict: Boolean = true
)

object WeatherNotificationHelper {

    const val CHANNEL_ID = "mukomuko_severe_weather_channel"
    private const val CHANNEL_NAME = "Peringatan Cuaca Buruk Mukomuko"
    private const val CHANNEL_DESC = "Notifikasi peringatan dini badai petir, angin kencang, banjir, dan cuaca ekstrem Kabupaten Mukomuko"

    // Signature Orange Color matching the app theme (#EA580C)
    val ORANGE_NOTIFICATION_COLOR: Int = Color.rgb(234, 88, 12)

    fun initNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableLights(true)
                lightColor = ORANGE_NOTIFICATION_COLOR
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 350, 150, 350, 200, 500)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun postWeatherAlertNotification(
        context: Context,
        warning: DetailedEarlyWarning,
        targetDistrictName: String
    ): Boolean {
        initNotificationChannel(context)

        if (!hasNotificationPermission(context)) {
            return false
        }

        // Intent to open app directly on Peringatan & Gempa WRS tab (index 5)
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_NAVIGATE_TAB", 5)
            putExtra("EXTRA_WARNING_ID", warning.id)
        }

        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            warning.id.hashCode(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent to call emergency 117
        val dial117Intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:117")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val dial117PendingIntent = PendingIntent.getActivity(
            context,
            117,
            dial117Intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triageLevel = DisasterTriageNotificationManager.severityToTriageLevel(warning.severity)
        val severityEmoji = when (triageLevel) {
            DisasterTriageLevel.MERAH_AWAS -> "🔴 TRIASE MERAH • AWAS"
            DisasterTriageLevel.ORANYE_SIAGA -> "🟠 TRIASE ORANYE • SIAGA"
            DisasterTriageLevel.KUNING_WASPADA -> "🟡 TRIASE KUNING • WASPADA"
            DisasterTriageLevel.HIJAU_AMAN -> "🟢 AMAN"
        }

        val title = "$severityEmoji: ${warning.title} • $targetDistrictName"
        val subtitle = "${warning.category} • Berlaku s/d ${warning.validUntil}"

        val bigText = buildString {
            append("Wilayah Terdampak: ${warning.affectedDistricts.joinToString(", ")}\n")
            append("Status: ${triageLevel.badgeLabel} • Berlaku s/d ${warning.validUntil}\n\n")
            append("${warning.description}\n\n")
            append("Panduan BPBD Mukomuko:\n")
            warning.safetyInstructions.take(2).forEach { instruction ->
                append("• $instruction\n")
            }
            append("\nCall Center Darurat Bebas Pulsa: 117")
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setColor(triageLevel.colorInt)
            .setColorized(true)
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(android.R.drawable.ic_menu_view, "Lihat Rincian", openAppPendingIntent)
            .addAction(android.R.drawable.ic_menu_call, "Panggil 117", dial117PendingIntent)
            .setVibrate(longArrayOf(0, 350, 150, 350, 200, 500))
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(warning.id.hashCode(), notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    /**
     * Push notification for sudden, localized severe weather alerts in Mukomuko Regency
     */
    fun postLocationBasedSuddenSevereWeatherPushNotification(
        context: Context,
        districtName: String,
        alertType: String = "Badai Petir & Angin Kencang Mendadak",
        windSpeedKmH: Double = 52.0,
        rainProbability: Int = 95
    ): Boolean {
        initNotificationChannel(context)

        if (!hasNotificationPermission(context)) {
            return false
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_NAVIGATE_TAB", 5)
        }

        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            "sudden_weather_$districtName".hashCode(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dial117Intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:117")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val dial117PendingIntent = PendingIntent.getActivity(
            context,
            117,
            dial117Intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "⚡ PERINGATAN CUACA BURUK MENDADAK: $districtName"
        val subtitle = "$alertType • Kecepatan Angin ${windSpeedKmH.toInt()} km/j"

        val bigText = buildString {
            append("⚠️ PERINGATAN DINI CUACA EKSTREM BERBASIS LOKASI\n")
            append("Lokasi Terdeteksi: Kecamatan $districtName, Kabupaten Mukomuko\n\n")
            append("BMKG Stasiun Meteorologi Fatmawati Soekarno mendeteksi pertumbuhan awan Cumulonimbus (Cb) pekat di atas wilayah Anda.\n\n")
            append("Dampak Potensial:\n")
            append("• Hujan lebat disertai kilat/petir (Peluang: $rainProbability%)\n")
            append("• Angin kencang sesaat hingga ${windSpeedKmH.toInt()} km/jam\n")
            append("• Potensi pohon tumbang dan genangan banjir kilat di titik cekung\n\n")
            append("Arahan Kesiapsiagaan Pusdalops BPBD Mukomuko:\n")
            append("1. Hindari berteduh di bawah pohon besar, tiang listrik, atau baliho reklame.\n")
            append("2. Nelayan pesisir tunda pelayaran dan amankan perahu ke tambatan aman.\n")
            append("3. Pengendara jalan lintas kurangi kecepatan dan nyalakan lampu kabut.\n\n")
            append("Layanan Bantuan Darurat BPBD Mukomuko Bebas Pulsa: 117")
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setColor(DisasterTriageLevel.MERAH_AWAS.colorInt)
            .setColorized(true)
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(android.R.drawable.ic_menu_view, "Buka Radar & Panduan", openAppPendingIntent)
            .addAction(android.R.drawable.ic_menu_call, "Panggil 117", dial117PendingIntent)
            .setVibrate(longArrayOf(0, 450, 150, 450, 200, 600))
            .build()

        return try {
            NotificationManagerCompat.from(context).notify("sudden_weather_$districtName".hashCode(), notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    const val APP_UPDATE_CHANNEL_ID = "mukomuko_app_update_channel"

    fun initAppUpdateNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                APP_UPDATE_CHANNEL_ID,
                "Pembaruan Daring Aplikasi SIPAGAR KITA",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Pemberitahuan rilis versi terbaru dan instalasi pembaruan daring"
                enableLights(true)
                lightColor = ORANGE_NOTIFICATION_COLOR
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 150, 300)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    fun postAppUpdateNotification(
        context: Context,
        latestVersion: String,
        highlightText: String
    ): Boolean {
        initAppUpdateNotificationChannel(context)
        if (!hasNotificationPermission(context)) return false

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_OPEN_UPDATE_SHEET", true)
            putExtra("EXTRA_AUTO_START_UPDATE", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            2026,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, APP_UPDATE_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("🚀 Pembaruan Aplikasi Tersedia: v$latestVersion")
            .setContentText("Versi terbaru siap dipasang langsung secara daring (tanpa update offline).")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Pusdalops BPBD Mukomuko merilis SIPAGAR KITA v$latestVersion!\n\n" +
                    "Fitur Baru:\n$highlightText\n\n" +
                    "Ketuk untuk langsung menginstal secara daring tanpa perlu unduh berkas offline."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVibrate(longArrayOf(0, 300, 150, 300))
            .setColor(ORANGE_NOTIFICATION_COLOR)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(
                android.R.drawable.stat_sys_download,
                "Perbarui Daring (OTA)",
                pendingIntent
            )
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(2026, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }
}

