package com.example.ui.components

import android.content.Context
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import com.example.util.EmergencyTonePlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.sync.OfficerPushNotificationHelper
import com.example.model.CitizenDisasterReport
import com.example.model.CitizenReportStatusHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Detektor Real-Time Laporan Warga Baru untuk Dashboard Petugas / Pusdalops.
 * Menyimpan `lastReadReportId` (variabel penampung ID laporan terakhir yang dibaca)
 * dan mendeteksi setiap ada ID laporan baru berstatus Pending yang masuk melalui stream
 * yang diurutkan berdasarkan `timestamp` descending.
 */
class OfficerRealtimeNewReportDetector {
    var lastReadReportId: String? = null
        private set

    private val seenReportIds = mutableSetOf<String>()
    private val unacknowledgedQueue = java.util.concurrent.CopyOnWriteArrayList<CitizenDisasterReport>()

    var isBaselineInitialized: Boolean = false
        private set

    companion object {
        /**
         * Detektor sesi global yang bertahan lintas pergantian tab (Mode Warga <-> Ruang Kerja Petugas)
         * sehingga laporan warga yang baru dikirim saat sesi petugas aktif tidak pernah tertelan sebagai baseline lama.
         */
        val sharedSessionDetector = OfficerRealtimeNewReportDetector()
    }

    @Synchronized
    fun resetBaselineAndClearAlerts() {
        lastReadReportId = null
        seenReportIds.clear()
        unacknowledgedQueue.clear()
        isBaselineInitialized = false
    }

    @Synchronized
    fun initializeBaselineIfNeeded(existingReports: List<CitizenDisasterReport>) {
        if (!isBaselineInitialized) {
            val pendingSortedDesc = existingReports
                .filter {
                    CitizenReportStatusHelper.isPending(it.status) ||
                        it.status.equals("Pending", ignoreCase = true)
                }
                .sortedByDescending { it.timestamp }
            isBaselineInitialized = true
            pendingSortedDesc.forEach { seenReportIds.add(it.id) }
            lastReadReportId = pendingSortedDesc.firstOrNull()?.id
        }
    }

    @Synchronized
    fun recordNewCitizenReport(report: CitizenDisasterReport, context: Context? = null) {
        if (!CitizenReportStatusHelper.isPending(report.status) &&
            !report.status.equals("Pending", ignoreCase = true)
        ) {
            return
        }
        isBaselineInitialized = true
        seenReportIds.add(report.id)
        lastReadReportId = report.id
        unacknowledgedQueue.removeAll { it.id == report.id }
        unacknowledgedQueue.add(0, report)
        val effectiveContext = context?.applicationContext ?: OfficerPushNotificationHelper.getAppContext()
        if (effectiveContext != null) {
            try {
                OfficerPushNotificationHelper.dispatchRoleAwareNewReportNotification(effectiveContext, report)
            } catch (_: Throwable) {
            }
        }
    }

    @Synchronized
    fun getUnacknowledgedAlerts(): List<CitizenDisasterReport> = unacknowledgedQueue.toList()

    /**
     * Memproses daftar laporan dari stream `citizen_disaster_reports`
     * (filter: status == Pending/Diproses, orderBy: timestamp descending).
     * Mengembalikan [CitizenDisasterReport] baru apabila terdeteksi ID laporan baru yang belum dibaca.
     */
    @Synchronized
    fun onIncomingPendingReports(incomingReports: List<CitizenDisasterReport>): CitizenDisasterReport? {
        val pendingSortedDesc = incomingReports
            .filter {
                CitizenReportStatusHelper.isPending(it.status) ||
                    it.status.equals("Pending", ignoreCase = true)
            }
            .sortedByDescending { it.timestamp }

        if (!isBaselineInitialized) {
            isBaselineInitialized = true
            pendingSortedDesc.forEach { seenReportIds.add(it.id) }
            lastReadReportId = pendingSortedDesc.firstOrNull()?.id
            return unacknowledgedQueue.firstOrNull()
        }

        if (pendingSortedDesc.isEmpty()) {
            return unacknowledgedQueue.firstOrNull()
        }

        val topNewest = pendingSortedDesc.first()
        val newestUnseen = if (topNewest.id != lastReadReportId && !seenReportIds.contains(topNewest.id)) {
            topNewest
        } else {
            pendingSortedDesc.firstOrNull { !seenReportIds.contains(it.id) }
        }

        // Tangkap seluruh laporan baru yang belum pernah dilihat dalam snapshot ini
        val allNewUnseen = pendingSortedDesc.filter { !seenReportIds.contains(it.id) }
        pendingSortedDesc.forEach { seenReportIds.add(it.id) }

        if (allNewUnseen.isNotEmpty()) {
            val appCtx = OfficerPushNotificationHelper.getAppContext()
            for (newRep in allNewUnseen.asReversed()) {
                unacknowledgedQueue.removeAll { it.id == newRep.id }
                unacknowledgedQueue.add(0, newRep)
                if (appCtx != null) {
                    try {
                        OfficerPushNotificationHelper.notifyOfficerForNewReport(appCtx, newRep)
                    } catch (_: Throwable) {
                    }
                }
            }
        }

        return if (newestUnseen != null) {
            lastReadReportId = newestUnseen.id
            newestUnseen
        } else {
            null
        }
    }

    /**
     * Menandai ID laporan sebagai sudah dibaca/direspons oleh petugas.
     */
    @Synchronized
    fun markReportAsRead(reportId: String) {
        val trimmed = reportId.trim()
        if (trimmed.isNotBlank()) {
            seenReportIds.add(trimmed)
            lastReadReportId = trimmed
            unacknowledgedQueue.removeAll { it.id == trimmed }
        }
    }

    @Synchronized
    fun resetForTesting() {
        lastReadReportId = null
        seenReportIds.clear()
        unacknowledgedQueue.clear()
        isBaselineInitialized = false
    }
}

/**
 * Pemutar Suara Alarm/Sirene Singkat & Getaran Kuat (Real-Time Alert Engine) untuk Petugas Pusdalops.
 */
object OfficerAlarmPlayer {
    private const val TAG = "OfficerAlarmPlayer"
    const val RED_BANNER_HEADLINE = "🚨 LAPORAN WARGA BARU! Segera Cek!"

    @Volatile
    var lastAlarmTriggeredReportId: String? = null
        private set

    @Volatile
    var alarmTriggerCount: Int = 0
        private set

    @Volatile
    var vibrationTriggerCount: Int = 0
        private set

    @Volatile
    private var activeRingtone: Ringtone? = null

    /**
     * Memicu suara alarm/sirene singkat dan getaran kuat pada perangkat petugas.
     */
    fun triggerSirenAndVibration(
        context: Context,
        report: CitizenDisasterReport? = null,
        dispatchSystemNotification: Boolean = false
    ) {
        if (report != null) {
            lastAlarmTriggeredReportId = report.id
        }
        alarmTriggerCount += 1

        // 1. Getaran Kuat (Strong Hardware Vibration Waveform)
        triggerStrongVibration(context)

        // 2. Suara Alarm / Sirene Singkat (RingtoneManager TYPE_ALARM + ToneGenerator Sirene Darurat)
        playShortAlarmSiren(context)

        // 3. Notifikasi Push Sistem (High-Importance Channel) saat laporan baru pertama kali terdeteksi
        if (dispatchSystemNotification && report != null) {
            try {
                OfficerPushNotificationHelper.notifyOfficerForNewReport(context, report)
            } catch (t: Throwable) {
                Log.w(TAG, "Gagal mengirim notifikasi sistem untuk laporan ${report.id}: ${t.message}")
            }
        }
    }

    fun triggerStrongVibration(context: Context) {
        vibrationTriggerCount += 1
        try {
            val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (vibrator != null && vibrator.hasVibrator()) {
                val timings = longArrayOf(0L, 450L, 120L, 450L, 120L, 650L)
                val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                    vibrator.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(timings, -1)
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Getaran perangkat gagal dipicu: ${t.message}")
        }
    }

    fun playShortAlarmSiren(context: Context) {
        try {
            stopActiveRingtone()
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

            if (alarmUri != null) {
                val ringtone = RingtoneManager.getRingtone(context.applicationContext, alarmUri)
                if (ringtone != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        ringtone.audioAttributes = AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    }
                    activeRingtone = ringtone
                    ringtone.play()
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Ringtone alarm gagal diputar, beralih ke ToneGenerator: ${t.message}")
        }

        // Sirine akustik singkat berbasis PCM AudioTrack (bebas deadlock/timeout ToneGenerator)
        try {
            EmergencyTonePlayer.playToneAsync(
                freqHz1 = 880.0,
                freqHz2 = 1100.0,
                durationMs = 850,
                usage = AudioAttributes.USAGE_ALARM,
                volume = 0.85f
            )
        } catch (t: Throwable) {
            Log.d(TAG, "EmergencyTonePlayer tidak tersedia pada lingkungan ini: ${t.message}")
        }
    }

    fun stopActiveRingtone() {
        try {
            activeRingtone?.let {
                if (it.isPlaying) {
                    it.stop()
                }
            }
        } catch (_: Throwable) {
        } finally {
            activeRingtone = null
        }
        try {
            EmergencyTonePlayer.stop()
        } catch (_: Throwable) {
        }
    }

    fun resetCountersForTesting() {
        stopActiveRingtone()
        lastAlarmTriggeredReportId = null
        alarmTriggerCount = 0
        vibrationTriggerCount = 0
    }
}

/**
 * UI Banner Merah Darurat ("🚨 LAPORAN WARGA BARU! Segera Cek!") di bagian atas layar Dashboard Petugas.
 * Memutar alarm berulang (Notifikasi Tanpa Henti) & getaran kuat hingga petugas menekan tombol cek/konfirmasi.
 */
@Composable
fun OfficerRealtimeRedAlertBanner(
    alertReport: CitizenDisasterReport,
    unacknowledgedCount: Int = 1,
    onInspectAndAcknowledge: (CitizenDisasterReport) -> Unit,
    onDismissAlarm: (CitizenDisasterReport) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current

    // Siklus Alarm & Getaran Tanpa Henti selama Banner Merah aktif dan belum direspons petugas
    LaunchedEffect(alertReport.id) {
        var pulseIndex = 0
        while (isActive) {
            try {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            } catch (_: Throwable) {
            }
            OfficerAlarmPlayer.triggerSirenAndVibration(
                context = context,
                report = alertReport,
                dispatchSystemNotification = (pulseIndex == 0)
            )
            pulseIndex += 1
            delay(4_500L)
            OfficerAlarmPlayer.stopActiveRingtone()
            delay(500L)
        }
    }

    DisposableEffect(alertReport.id) {
        onDispose {
            OfficerAlarmPlayer.stopActiveRingtone()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "realtime_red_alert_pulse")
    val bannerBgColor by infiniteTransition.animateColor(
        initialValue = Color(0xFFDC2626),
        targetValue = Color(0xFF991B1B),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "banner_bg_pulse"
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bannerBgColor),
        border = BorderStroke(2.5.dp, Color(0xFFFEE2E2)),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("officer_realtime_red_alert_banner")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Sirene Laporan Warga Baru",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Surface(
                            color = Color(0xFFFEF08A),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (unacknowledgedCount > 1) {
                                    "REAL-TIME ALERT • $unacknowledgedCount LAPORAN PENDING BARU"
                                } else {
                                    "REAL-TIME ALERT PUSDALOPS • DARURAT"
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF7F1D1D),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = OfficerAlarmPlayer.RED_BANNER_HEADLINE,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            lineHeight = 22.sp,
                            modifier = Modifier.testTag("officer_realtime_red_alert_headline")
                        )
                    }
                }

                IconButton(
                    onClick = {
                        OfficerAlarmPlayer.stopActiveRingtone()
                        onDismissAlarm(alertReport)
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("btn_dismiss_realtime_red_alert")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Matikan Alarm",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Ringkasan Laporan Warga Baru yang Masuk
            Surface(
                color = Color(0xFF450A0A).copy(alpha = 0.65f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ID: ${alertReport.id}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFFDE047)
                        )
                        Text(
                            text = "Status: ${alertReport.status.ifBlank { "Pending" }}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFECACA)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFFDE047),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${alertReport.disasterType} — Pelapor: ${alertReport.reporterName}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color(0xFFFCA5A5),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Desa ${alertReport.village.ifBlank { "-" }}, Kec. ${alertReport.district} (${alertReport.incidentLocation.ifBlank { "Koordinat GPS" }})",
                            fontSize = 12.sp,
                            color = Color(0xFFFEE2E2)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        OfficerAlarmPlayer.stopActiveRingtone()
                        onInspectAndAcknowledge(alertReport)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF991B1B)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1.35f)
                        .fillMaxHeight()
                        .heightIn(min = 48.dp)
                        .testTag("btn_inspect_realtime_alert_report")
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SEGERA CEK &\nMATIKAN ALARM",
                        fontSize = 11.sp,
                        lineHeight = 13.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedButton(
                    onClick = {
                        OfficerAlarmPlayer.stopActiveRingtone()
                        onDismissAlarm(alertReport)
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    ),
                    border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.8f)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(0.85f)
                        .fillMaxHeight()
                        .heightIn(min = 48.dp)
                        .testTag("btn_silence_realtime_alarm")
                ) {
                    Text(
                        text = "MATIKAN\nSIRENE",
                        fontSize = 11.sp,
                        lineHeight = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
