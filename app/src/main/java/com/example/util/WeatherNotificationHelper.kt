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

        // Intent to open app directly on Peringatan Dini tab (index 3)
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_NAVIGATE_TAB", 3)
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

        val severityEmoji = when (warning.severity) {
            "AWAS" -> "🚨 AWAS"
            "SIAGA" -> "⚠️ SIAGA"
            else -> "⚡ WASPADA"
        }

        val title = "$severityEmoji: ${warning.title} • $targetDistrictName"
        val subtitle = "${warning.category} • Berlaku s/d ${warning.validUntil}"

        val bigText = buildString {
            append("Wilayah Terdampak: ${warning.affectedDistricts.joinToString(", ")}\n")
            append("Status: ${warning.severity} • Berlaku s/d ${warning.validUntil}\n\n")
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
            .setColor(ORANGE_NOTIFICATION_COLOR)
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
}
