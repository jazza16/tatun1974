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
    private const val CHANNEL_NAME = "Peringatan Dini Gempa Lokal Provinsi Bengkulu"
    private const val CHANNEL_DESC = "Notifikasi real-time khusus gempa bumi lokal yang terjadi dan dirasakan di Wilayah Provinsi Bengkulu (BMKG)"

    private const val PREFS_NAME = "sipagar_eq_direct_notif_prefs"
    private const val KEY_PREFIX_NOTIFIED = "notified_eq_"
    private const val RETENTION_MS = 7L * 24L * 60L * 60_000L

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

    fun clearNotifiedHistory(context: Context) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }

    private fun wasEventAlreadyNotified(context: Context, eventIdentity: String): Boolean {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.contains(KEY_PREFIX_NOTIFIED + eventIdentity.hashCode())
    }

    private fun markEventNotified(context: Context, eventIdentity: String, nowMillis: Long) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        // Prune stale history older than 7 days to keep prefs lightweight
        prefs.all.forEach { (key, value) ->
            if (key.startsWith(KEY_PREFIX_NOTIFIED) && value is Long && (nowMillis - value) > RETENTION_MS) {
                editor.remove(key)
            }
        }
        editor.putLong(KEY_PREFIX_NOTIFIED + eventIdentity.hashCode(), nowMillis)
        editor.apply()
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

    fun postEarthquakeNotification(
        context: Context,
        earthquake: BmkgEarthquake,
        allowOutsideBengkuluForManualTest: Boolean = false,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        // GUARD WILAYAH LOKAL PROVINSI BENGKULU & WAKTU KEJADIAN (MAX 2 JAM):
        // 1. Blokir gempa global di luar Provinsi Bengkulu.
        // 2. Blokir data fallback offline dan kejadian lampau (> 2 jam) agar tidak muncul berulang.
        // 3. Blokir duplikasi persisten untuk event gempa yang sama.
        val eventIdentity = com.example.model.BengkuluEarthquakeScope.buildEarthquakeEventIdentity(earthquake)
        if (!allowOutsideBengkuluForManualTest) {
            if (!earthquake.isBengkuluProvinceAffected) {
                return false
            }
            if (!com.example.model.BengkuluEarthquakeScope.isWithinNotificationWindow(earthquake, nowMillis)) {
                return false
            }
            if (wasEventAlreadyNotified(context, eventIdentity)) {
                return false
            }
        }

        initNotificationChannel(context)

        if (!hasNotificationPermission(context)) {
            return false
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_NAVIGATE_TAB", 5) // Tab 5: Peringatan & Gempa WRS
            putExtra("EXTRA_EARTHQUAKE_ID", earthquake.id)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            eventIdentity.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val urgencyEmoji = when (earthquake.urgencyLevel) {
            EarthquakeUrgency.CRITICAL -> "🔴 TRIASE MERAH • AWAS"
            EarthquakeUrgency.WARNING -> "🟠 TRIASE ORANYE • SIAGA"
            EarthquakeUrgency.ADVISORY -> "🟡 TRIASE KUNING • WASPADA"
        }

        val dateTimeLabel = listOf(earthquake.date.trim(), earthquake.timeWib.trim())
            .filter { it.isNotBlank() }
            .joinToString(" • ")

        val title = "$urgencyEmoji Gempa M ${earthquake.magnitude} - BMKG"
        val subtitle = "$dateTimeLabel • ${earthquake.region} (Kedalaman ${earthquake.depth})"
        val detailedContent = buildString {
            append("Tanggal & Waktu Kejadian: ${earthquake.date} • ${earthquake.timeWib}\n")
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
            EarthquakeUrgency.CRITICAL -> DisasterTriageLevel.MERAH_AWAS.colorInt
            EarthquakeUrgency.WARNING -> DisasterTriageLevel.ORANYE_SIAGA.colorInt
            EarthquakeUrgency.ADVISORY -> DisasterTriageLevel.KUNING_WASPADA.colorInt
        }

        val displayTimeoutMs = if (allowOutsideBengkuluForManualTest) {
            com.example.model.BengkuluEarthquakeScope.MAX_EARTHQUAKE_NOTIFICATION_WINDOW_MS
        } else {
            com.example.model.BengkuluEarthquakeScope.computeNotificationDisplayTimeoutMs(earthquake, nowMillis)
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
            .setTimeoutAfter(displayTimeoutMs)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 400, 200, 400, 200, 600))
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(eventIdentity.hashCode(), notification)
            if (!allowOutsideBengkuluForManualTest) {
                markEventNotified(context, eventIdentity, nowMillis)
            }
            true
        } catch (_: SecurityException) {
            false
        }
    }

    fun postTsunamiWarningNotification(
        context: Context,
        earthquake: BmkgEarthquake,
        allowOutsideBengkuluForManualTest: Boolean = false,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        // GUARD WILAYAH LOKAL PROVINSI BENGKULU & WAKTU KEJADIAN:
        val eventIdentity = "tsunami_${com.example.model.BengkuluEarthquakeScope.buildEarthquakeEventIdentity(earthquake)}"
        if (!allowOutsideBengkuluForManualTest) {
            if (!earthquake.isBengkuluProvinceAffected) {
                return false
            }
            if (!com.example.model.BengkuluEarthquakeScope.isWithinNotificationWindow(earthquake, nowMillis)) {
                return false
            }
            if (wasEventAlreadyNotified(context, eventIdentity)) {
                return false
            }
        }

        initNotificationChannel(context)

        if (!hasNotificationPermission(context)) {
            return false
        }

        // Navigate directly to Tab 7 (Peta & SOP Evakuasi)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_NAVIGATE_TAB", 7)
            putExtra("EXTRA_EARTHQUAKE_ID", earthquake.id)
            putExtra("EXTRA_SHOW_TSUNAMI_GUIDE", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            eventIdentity.hashCode(),
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
        val subtitle = "${earthquake.date} • ${earthquake.timeWib} • Gempa M ${earthquake.magnitude} • Evakuasi Mandiri!"
        val detailedContent = buildString {
            append("PERINGATAN DINI TSUNAMI RESMI PUSDALOPS BPBD MUKOMUKO\n")
            append("Tanggal & Waktu Kejadian: ${earthquake.date} • ${earthquake.timeWib}\n")
            append("Kekuatan Gempa: M ${earthquake.magnitude} | Pusat: ${earthquake.region}\n")
            append("Kedalaman: ${earthquake.depth}\n\n")
            append("SOP EVAKUASI MANDIRI PESISIR MUKOMUKO (RUMUS 20-20-20):\n")
            append("1. Jangan tunggu sirene atau air surut!\n")
            append("2. Lari mandiri menuju tempat berketinggian minimal 20 meter dpl atau minimal 2 km dari bibir pantai Samudera Hindia!\n")
            append("3. Jauhi muara sungai dan jembatan.\n")
            append("4. Hubungi Call Center Darurat Bebas Pulsa: 117")
        }

        val displayTimeoutMs = if (allowOutsideBengkuluForManualTest) {
            com.example.model.BengkuluEarthquakeScope.MAX_EARTHQUAKE_NOTIFICATION_WINDOW_MS
        } else {
            com.example.model.BengkuluEarthquakeScope.computeNotificationDisplayTimeoutMs(earthquake, nowMillis)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setStyle(NotificationCompat.BigTextStyle().bigText(detailedContent))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setColor(DisasterTriageLevel.MERAH_AWAS.colorInt)
            .setColorized(true)
            .setAutoCancel(true)
            .setTimeoutAfter(displayTimeoutMs)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_directions, "Panduan Evakuasi", pendingIntent)
            .addAction(android.R.drawable.ic_menu_call, "Panggil 117", dial117PendingIntent)
            .setVibrate(longArrayOf(0, 500, 200, 500, 200, 800))
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(eventIdentity.hashCode(), notification)
            if (!allowOutsideBengkuluForManualTest) {
                markEventNotified(context, eventIdentity, nowMillis)
            }
            true
        } catch (_: SecurityException) {
            false
        }
    }
}

