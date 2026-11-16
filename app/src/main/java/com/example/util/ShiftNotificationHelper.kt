package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity

/**
 * Local notification system alerting Pusdalops BPBD staff when their designated
 * shift hours are approaching, based on roster records and audit logs.
 */
object ShiftNotificationHelper {

    const val CHANNEL_ID = "bpbd_shift_reminder_channel"
    private const val CHANNEL_NAME = "Pengingat Shift Dinas Pusdalops PB"
    private const val NOTIFICATION_ID_BASE = 8800

    fun initNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                importance
            ).apply {
                description = "Notifikasi pengingat pergantian tugas piket dan shift dinas personil Pusdalops BPBD Mukomuko"
                enableVibration(true)
                setShowBadge(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Sends local push alert to staff about upcoming shift duty.
     */
    fun sendShiftReminderNotification(
        context: Context,
        officerName: String,
        shiftName: String,
        minutesBefore: Int = 30
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        initNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_NAVIGATE_TAB", 4) // Tab Konsol Petugas
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_BASE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("⏰ Pengingat Tugas: $shiftName")
            .setContentText("Yth. $officerName, jadwal shift dinas Anda akan dimulai dalam $minutesBefore menit. Mohon bersiap serah terima piket Pusdalops.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Pusdalops-PB BPBD Kabupaten Mukomuko\n" +
                    "Nama Petugas: $officerName\n" +
                    "Shift: $shiftName\n" +
                    "Status: Harap segera melakukan login absensi pada konsol petugas di Posko Pusdalops PB."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            val manager = NotificationManagerCompat.from(context)
            manager.notify(NOTIFICATION_ID_BASE + (System.currentTimeMillis() % 100).toInt(), notification)
        } catch (_: SecurityException) {
            // Permission POST_NOTIFICATIONS may not be granted
        }
    }
}
