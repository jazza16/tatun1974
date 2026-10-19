package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.model.BmkgEarthquake
import com.example.model.EarthquakeUrgency

object EarthquakeNotificationHelper {

    const val CHANNEL_ID = "bmkg_earthquake_channel"
    private const val CHANNEL_NAME = "Peringatan Dini Gempa Bumi BMKG"
    private const val CHANNEL_DESC = "Notifikasi real-time rilis gempa bumi resmi dari BMKG Indonesia"

    fun initNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 200, 400, 200, 600)
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

    fun postEarthquakeNotification(context: Context, earthquake: BmkgEarthquake): Boolean {
        initNotificationChannel(context)

        if (!hasNotificationPermission(context)) {
            return false
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_NAVIGATE_TAB", 3) // Tab 3: Peringatan Dini
            putExtra("EXTRA_EARTHQUAKE_ID", earthquake.id)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            earthquake.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val urgencyEmoji = when (earthquake.urgencyLevel) {
            EarthquakeUrgency.CRITICAL -> "🚨 BAHAYA"
            EarthquakeUrgency.WARNING -> "⚠️ WASPADA"
            EarthquakeUrgency.ADVISORY -> "ℹ️ INFO"
        }

        val title = "$urgencyEmoji Gempa M ${earthquake.magnitude} - BMKG"
        val subtitle = "${earthquake.region} (Kedalaman ${earthquake.depth})"
        val detailedContent = buildString {
            append("Waktu: ${earthquake.date} pukul ${earthquake.timeWib}\n")
            append("Pusat: ${earthquake.region}\n")
            append("Kedalaman: ${earthquake.depth} • Magnitudo: M ${earthquake.magnitude}\n")
            append("Status: ${earthquake.potentialTsunami}\n")
            if (earthquake.distanceKmToMukomuko < 900) {
                append("Jarak dari Mukomuko: ~${earthquake.distanceKmToMukomuko} km")
            }
            if (earthquake.feltDescription != "-") {
                append("\nDirasakan: ${earthquake.feltDescription}")
            }
        }

        val notificationColor = when (earthquake.urgencyLevel) {
            EarthquakeUrgency.CRITICAL -> Color.RED
            EarthquakeUrgency.WARNING -> Color.rgb(234, 88, 12) // Orange
            EarthquakeUrgency.ADVISORY -> Color.rgb(14, 165, 233) // Blue
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setStyle(NotificationCompat.BigTextStyle().bigText(detailedContent))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setColor(notificationColor)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 400, 200, 400, 200, 600))
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(earthquake.id.hashCode(), notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    fun postTsunamiWarningNotification(context: Context, earthquake: BmkgEarthquake): Boolean {
        initNotificationChannel(context)

        if (!hasNotificationPermission(context)) {
            return false
        }

        // Navigate directly to Tab 4 (Evakuasi Gempa & Kesiapsiagaan)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_NAVIGATE_TAB", 4)
            putExtra("EXTRA_EARTHQUAKE_ID", earthquake.id)
            putExtra("EXTRA_SHOW_TSUNAMI_GUIDE", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            ("tsunami_" + earthquake.id).hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dial117Intent = Intent(Intent.ACTION_DIAL, android.net.Uri.parse("tel:117")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val dial117PendingIntent = PendingIntent.getActivity(
            context,
            1170,
            dial117Intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "🚨 PERINGATAN DINI POTENSI TSUNAMI - BPBD MUKOMUKO"
        val subtitle = "Gempa M ${earthquake.magnitude} • Segera Evakuasi Mandiri ke Tempat Tinggi!"
        val detailedContent = buildString {
            append("PERINGATAN DINI TSUNAMI RESMI PUSDALOPS BPBD MUKOMUKO\n")
            append("Kekuatan Gempa: M ${earthquake.magnitude} | Pusat: ${earthquake.region}\n")
            append("Kedalaman: ${earthquake.depth} | Waktu: ${earthquake.timeWib}\n\n")
            append("SOP EVAKUASI MANDIRI PESISIR MUKOMUKO (RUMUS 20-20-20):\n")
            append("1. Jangan tunggu sirene atau air surut!\n")
            append("2. Lari mandiri menuju tempat berketinggian minimal 20 meter dpl atau minimal 2 km dari bibir pantai Samudera Hindia!\n")
            append("3. Jauhi muara sungai dan jembatan.\n")
            append("4. Hubungi Call Center Darurat Bebas Pulsa: 117")
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setStyle(NotificationCompat.BigTextStyle().bigText(detailedContent))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setColor(Color.RED)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_directions, "Panduan Evakuasi", pendingIntent)
            .addAction(android.R.drawable.ic_menu_call, "Panggil 117", dial117PendingIntent)
            .setVibrate(longArrayOf(0, 500, 200, 500, 200, 800))
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(("tsunami_" + earthquake.id).hashCode(), notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }
}

