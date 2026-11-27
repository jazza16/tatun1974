package com.example.ui.components

import android.content.Context
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Surfing
import androidx.compose.material.icons.filled.Vibration
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BengkuluEarthquakeScope
import com.example.model.BmkgEarthquake
import com.example.model.DetailedEarlyWarning
import com.example.model.MarineWeather
import com.example.model.WeatherAlert
import com.example.ui.theme.OrangePrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Kebijakan Validasi Tanggal Event, Durasi Tampil (1–2 Jam), dan Deduplikasi Persisten
 * untuk Popup & Kartu Peringatan Dini Darurat SIPAGAR KITA.
 */
object EmergencyWarningPopupPolicy {

    const val ACTIVE_WARNING_WINDOW_HOURS = 2
    const val ACTIVE_WARNING_WINDOW_MS = ACTIVE_WARNING_WINDOW_HOURS * 60L * 60_000L
    private const val PREFS_NAME = "sipagar_emergency_popup_ledger_prefs"
    private const val KEY_PREFIX_DISMISSED_SIG = "dismissed_popup_sig_"
    private const val LEDGER_RETENTION_MS = 7L * 24L * 60L * 60_000L

    private val INDONESIAN_MONTHS = arrayOf(
        "Jan", "Feb", "Mar", "Apr", "Mei", "Jun",
        "Jul", "Agu", "Sep", "Okt", "Nov", "Des"
    )

    /**
     * Menormalkan label waktu agar tidak pernah terjadi duplikasi `"WIB WIB"`.
     */
    fun normalizeTimeWib(rawTimeWib: String): String {
        val cleaned = rawTimeWib
            .trim()
            .replace(Regex("(?i)\\bWIB(\\s+WIB)+\\b"), "WIB")
            .trim()
        if (cleaned.isBlank()) return "Terkini WIB"
        val upper = cleaned.uppercase(Locale.ROOT)
        return if (upper.endsWith("WIB") || upper.endsWith("WITA") || upper.endsWith("WIT")) {
            cleaned
        } else {
            "$cleaned WIB"
        }
    }

    /**
     * Memastikan teks potensi tsunami hanya bernilai true jika benar-benar berpotensi tsunami
     * (bukan kalimat `"Tidak berpotensi tsunami"`).
     */
    fun hasRealTsunamiPotential(potentialTsunami: String): Boolean {
        val lower = potentialTsunami.trim().lowercase(Locale.ROOT)
        if (lower.isBlank()) return false
        if (lower.contains("tidak berpotensi") || lower.contains("tidak ada potensi")) {
            return false
        }
        return lower.contains("tsunami")
    }

    /**
     * Memeriksa apakah objek gempa memenuhi syarat sebagai peringatan darurat aktif:
     * 1. Bukan data fallback statis (`!isFallbackData`)
     * 2. Berada di / dirasakan di wilayah lokal Provinsi Bengkulu (`isBengkuluProvinceAffected`)
     * 3. Masih berada dalam jendela waktu aktif 1–2 jam sejak waktu kejadian (`isWithinNotificationWindow`)
     * 4. Memiliki signifikansi bahaya (`magnitude >= 5.0 || isNearMukomuko || hasRealTsunamiPotential`)
     */
    fun isEligibleLiveEarthquakeAlert(
        earthquake: BmkgEarthquake?,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        if (earthquake == null) return false
        if (earthquake.isFallbackData) return false
        if (!earthquake.isBengkuluProvinceAffected) return false
        if (!BengkuluEarthquakeScope.isWithinNotificationWindow(earthquake, nowMillis)) return false
        return earthquake.magnitude >= 5.0 ||
            earthquake.isNearMukomuko ||
            hasRealTsunamiPotential(earthquake.potentialTsunami)
    }

    /**
     * Memeriksa apakah kondisi gelombang laut saat ini termasuk gelombang tinggi darurat (>= 2.0 m).
     */
    fun isEligibleHighWaveAlert(marineWeather: MarineWeather?): Boolean {
        if (marineWeather == null) return false
        return marineWeather.waveHeight >= 2.0 ||
            marineWeather.seaCondition.contains("Tinggi", ignoreCase = true) ||
            marineWeather.fishermanSafety.contains("Bahaya", ignoreCase = true) ||
            marineWeather.fishermanSafety.contains("Tunda", ignoreCase = true)
    }

    /**
     * Memformat tanggal kejadian dan durasi aktif (1–2 jam) untuk kartu Gempa Bumi BMKG.
     */
    fun formatEarthquakeDateAndDuration(
        earthquake: BmkgEarthquake,
        nowMillis: Long = System.currentTimeMillis()
    ): Pair<String, String> {
        val cleanTime = normalizeTimeWib(earthquake.timeWib)
        val cleanDate = earthquake.date.trim().ifBlank { formatWibDateOnly(nowMillis) }
        val eventDateLabel = "Tanggal Kejadian: $cleanDate • Pukul $cleanTime"

        val parsedEventMs = BengkuluEarthquakeScope.parseEventTimestampMillis(
            dateTimeIso = earthquake.dateTimeIso,
            date = earthquake.date,
            timeWib = earthquake.timeWib
        )
        val durationLabel = if (parsedEventMs != null) {
            val startHm = formatWibHourMinute(parsedEventMs)
            val endHm = formatWibHourMinute(parsedEventMs + ACTIVE_WARNING_WINDOW_MS)
            "Durasi Peringatan: Aktif 1–2 Jam ($startHm – $endHm WIB • $cleanDate)"
        } else {
            "Durasi Peringatan: Aktif 1–2 Jam sejak kejadian ($cleanDate)"
        }
        return eventDateLabel to durationLabel
    }

    /**
     * Memformat tanggal event hari ini beserta jendela durasi berlaku (1–2 jam) untuk peringatan
     * cuaca ekstrem, gelombang tinggi maritim, atau peringatan dini sektoral.
     */
    fun formatWeatherOrMarineDateAndDuration(
        rawValidUntil: String? = null,
        nowMillis: Long = System.currentTimeMillis(),
        windowHours: Int = ACTIVE_WARNING_WINDOW_HOURS
    ): Pair<String, String> {
        val safeHours = windowHours.coerceIn(1, 3)
        val dateStr = formatWibDateOnly(nowMillis)
        val startHm = formatWibHourMinute(nowMillis)
        val endHm = formatWibHourMinute(nowMillis + safeHours * 60L * 60_000L)

        val eventDateLabel = "Tanggal Event: $dateStr • Pembaruan $startHm WIB"
        val cleanedValidUntil = rawValidUntil?.trim().orEmpty()
        val durationLabel = when {
            cleanedValidUntil.isBlank() ||
                cleanedValidUntil.contains("3 hari", ignoreCase = true) ||
                cleanedValidUntil.contains("normal kembali", ignoreCase = true) -> {
                "Durasi Berlaku: Aktif 1–$safeHours Jam ($startHm – $endHm WIB • $dateStr)"
            }
            cleanedValidUntil.contains(dateStr, ignoreCase = true) -> {
                "Durasi Berlaku: $cleanedValidUntil"
            }
            else -> {
                "Durasi Berlaku: $dateStr • Aktif 1–$safeHours Jam ($startHm – $endHm WIB)"
            }
        }
        return eventDateLabel to durationLabel
    }

    fun formatWibDateOnly(epochMillis: Long = System.currentTimeMillis()): String {
        val cal = java.util.Calendar.getInstance(TimeZone.getTimeZone("GMT+7")).apply {
            timeInMillis = epochMillis
        }
        val day = String.format(Locale.US, "%02d", cal.get(java.util.Calendar.DAY_OF_MONTH))
        val month = INDONESIAN_MONTHS[cal.get(java.util.Calendar.MONTH).coerceIn(0, 11)]
        val year = cal.get(java.util.Calendar.YEAR)
        return "$day $month $year"
    }

    fun formatWibHourMinute(epochMillis: Long = System.currentTimeMillis()): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("GMT+7")
        }
        return sdf.format(Date(epochMillis))
    }

    /**
     * Membangun identitas unik (signature) untuk kombinasi event darurat aktif saat ini.
     * Jika tidak ada event darurat nyata yang memenuhi syarat, mengembalikan string kosong `""`.
     */
    fun buildActivePopupSignature(
        weatherAlert: WeatherAlert?,
        marineWeather: MarineWeather?,
        latestEarthquake: BmkgEarthquake?,
        nowMillis: Long = System.currentTimeMillis()
    ): String {
        val parts = mutableListOf<String>()
        val wibDate = formatWibDateOnly(nowMillis)
        // Bucket 2 jam untuk peringatan cuaca/gelombang agar tidak berulang terus pada event yang sama
        val twoHourSlot = (nowMillis / ACTIVE_WARNING_WINDOW_MS)

        if (isEligibleLiveEarthquakeAlert(latestEarthquake, nowMillis) && latestEarthquake != null) {
            val eqId = BengkuluEarthquakeScope.buildEarthquakeEventIdentity(latestEarthquake)
            parts.add("eq:$eqId")
        }
        if (weatherAlert != null) {
            parts.add("wa:${weatherAlert.id}:${weatherAlert.severity}:$wibDate:$twoHourSlot")
        }
        if (isEligibleHighWaveAlert(marineWeather) && marineWeather != null) {
            val waveBucket = String.format(Locale.US, "%.1f", marineWeather.waveHeight)
            parts.add("mw:$waveBucket:$wibDate:$twoHourSlot")
        }
        return parts.joinToString("|")
    }

    /**
     * Memeriksa apakah event darurat dengan signature tersebut sudah pernah ditampilkan & ditutup
     * oleh pengguna sehingga tidak muncul berulang-ulang.
     */
    fun isPopupSignatureAlreadyDismissed(
        context: Context,
        signature: String,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        if (signature.isBlank()) return true
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = KEY_PREFIX_DISMISSED_SIG + signature.hashCode()
        val dismissedAt = prefs.getLong(key, 0L)
        if (dismissedAt <= 0L) return false
        return (nowMillis - dismissedAt) <= LEDGER_RETENTION_MS
    }

    /**
     * Mencatat bahwa pengguna telah melihat dan menutup popup untuk signature event ini.
     */
    fun markPopupSignatureDismissed(
        context: Context,
        signature: String,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        if (signature.isBlank()) return
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        prefs.all.forEach { (k, v) ->
            if (k.startsWith(KEY_PREFIX_DISMISSED_SIG) && v is Long) {
                if ((nowMillis - v) > LEDGER_RETENTION_MS) {
                    editor.remove(k)
                }
            }
        }
        editor.putLong(KEY_PREFIX_DISMISSED_SIG + signature.hashCode(), nowMillis)
        editor.apply()
    }

    fun clearDismissedPopupLedger(context: Context) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
}

/**
 * Dialog Popup Notifikasi Peringatan Dini Kebencanaan SIPAGAR KITA.
 * Muncul secara proaktif ketika terdeteksi peringatan gempa bumi lokal Provinsi Bengkulu (1–2 jam terakhir),
 * cuaca buruk ekstrem, atau gelombang tinggi Samudra Hindia di Kabupaten Mukomuko.
 */
@Composable
fun EmergencyWarningPopupDialog(
    weatherAlert: WeatherAlert? = null,
    marineWeather: MarineWeather? = null,
    latestEarthquake: BmkgEarthquake? = null,
    earlyWarnings: List<DetailedEarlyWarning> = emptyList(),
    nowMillis: Long = System.currentTimeMillis(),
    isDark: Boolean = false,
    onDismiss: () -> Unit,
    onCall117: () -> Unit,
    onOpenEvacuationMap: () -> Unit
) {
    // Pulsing animation for the emergency badge beacon
    val infiniteTransition = rememberInfiniteTransition(label = "pulseAlert")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val isHighWave = EmergencyWarningPopupPolicy.isEligibleHighWaveAlert(marineWeather)
    val isCriticalEq = EmergencyWarningPopupPolicy.isEligibleLiveEarthquakeAlert(latestEarthquake, nowMillis)

    // Kartu suplemen earlyWarnings hanya ditampilkan apabila ada kondisi darurat cuaca/maritim aktif
    // atau jika dialog dipanggil khusus untuk pratinjau earlyWarnings tanpa pemicu lain.
    val showEarlyWarningCard = earlyWarnings.isNotEmpty() &&
        weatherAlert == null &&
        (isHighWave || !isCriticalEq)

    val hasActiveAlerts = weatherAlert != null || isHighWave || isCriticalEq || showEarlyWarningCard

    if (!hasActiveAlerts) return

    BalancedModalDialog(
        onDismissRequest = onDismiss,
        dismissOnBackPress = true,
        dismissOnClickOutside = false,
        horizontalMargin = 14.dp,
        extraVerticalMargin = 14.dp
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("emergency_warning_popup_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) Color(0xFF1C130D) else Color(0xFFFFFFFF)
            ),
            border = BorderStroke(1.5.dp, Color(0xFFDC2626)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Top Header Row: Beacon & Dismiss Icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .scale(pulseScale)
                                    .clip(CircleShape)
                                    .background(Color(0xFFDC2626).copy(alpha = 0.25f))
                            )
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFFDC2626), Color(0xFF991B1B))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Campaign,
                                    contentDescription = "Peringatan",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFDC2626)
                            ) {
                                Text(
                                    text = "PERINGATAN DINI AKTIF",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "SIPAGAR KITA • BPBD MUKOMUKO",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = OrangePrimary,
                                maxLines = 1
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF2C1E16) else Color(0xFFFEE2E2))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup Dialog",
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Perhatian Masyarakat Kabupaten Mukomuko!",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDark) Color(0xFFFFF7ED) else Color(0xFF1F2937)
                )

                Text(
                    text = "Terdeteksi potensi bahaya cuaca buruk, gelombang tinggi, atau aktivitas seismik gempa bumi lokal Provinsi Bengkulu (jendela kejadian 1–2 jam):",
                    fontSize = 12.sp,
                    color = if (isDark) Color(0xFFFED7AA) else Color(0xFF4B5563),
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                // 1. Gempa Bumi BMKG Section (Hanya event nyata lokal Provinsi Bengkulu dalam 1-2 jam terakhir)
                if (isCriticalEq) {
                    latestEarthquake?.let { eq ->
                        val (eqDateLabel, eqDurationLabel) = EmergencyWarningPopupPolicy
                            .formatEarthquakeDateAndDuration(eq, nowMillis)
                        val cleanTimeWib = EmergencyWarningPopupPolicy.normalizeTimeWib(eq.timeWib)
                        val cleanRegion = formatEarthquakeRegion(eq.region)
                        WarningCardItem(
                            icon = Icons.Default.Vibration,
                            iconTint = Color(0xFFDC2626),
                            category = "GEMPA BUMI WRS-NTWC BMKG",
                            headline = "M ${eq.magnitude} • Kedalaman ${eq.depth}",
                            eventDateText = eqDateLabel,
                            durationText = eqDurationLabel,
                            detail = "$cleanRegion (${eq.date} • $cleanTimeWib). ${eq.potentialTsunami}",
                            badgeText = if (eq.isNearMukomuko) "DEKAT MUKOMUKO" else "LOKAL BENGKULU",
                            isDark = isDark
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }

                // 2. Cuaca Buruk / Ekstrem Section
                if (weatherAlert != null) {
                    val (waDateLabel, waDurationLabel) = EmergencyWarningPopupPolicy
                        .formatWeatherOrMarineDateAndDuration(weatherAlert.validUntil, nowMillis)
                    WarningCardItem(
                        icon = Icons.Default.Warning,
                        iconTint = Color(0xFFEA580C),
                        category = "PERINGATAN CUACA BMKG",
                        headline = weatherAlert.title,
                        eventDateText = waDateLabel,
                        durationText = waDurationLabel,
                        detail = weatherAlert.message,
                        badgeText = weatherAlert.severity.uppercase(),
                        isDark = isDark
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // 3. Gelombang Tinggi Samudra Hindia Section
                if (isHighWave) {
                    marineWeather?.let { wave ->
                        val (waveDateLabel, waveDurationLabel) = EmergencyWarningPopupPolicy
                            .formatWeatherOrMarineDateAndDuration(null, nowMillis)
                        WarningCardItem(
                            icon = Icons.Default.Surfing,
                            iconTint = Color(0xFF0284C7),
                            category = "MARITIM & PESISIR PANTAI",
                            headline = "Tinggi Gelombang ${wave.waveHeight} Meter (${wave.seaCondition})",
                            eventDateText = waveDateLabel,
                            durationText = waveDurationLabel,
                            detail = "${wave.fishermanSafety}. Himbauan nelayan: ${wave.fishermanAdvice}",
                            badgeText = "WASPADA GELOMBANG",
                            isDark = isDark
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }

                // 4. Detailed Early Warnings list
                if (showEarlyWarningCard) {
                    val firstWarning = earlyWarnings.first()
                    val (ewDateLabel, ewDurationLabel) = EmergencyWarningPopupPolicy
                        .formatWeatherOrMarineDateAndDuration(firstWarning.validUntil, nowMillis)
                    WarningCardItem(
                        icon = Icons.Default.Warning,
                        iconTint = Color(0xFFDC2626),
                        category = firstWarning.category.uppercase(),
                        headline = firstWarning.title,
                        eventDateText = ewDateLabel,
                        durationText = ewDurationLabel,
                        detail = firstWarning.description,
                        badgeText = firstWarning.severity,
                        isDark = isDark
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Himbauan Keamanan
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) Color(0xFF2B1612) else Color(0xFFFEF2F2),
                    border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Himbauan Kesiapsiagaan BPBD:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB91C1C)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "1. Hindari aktivitas di bibir pantai dan muara saat gelombang pasang tinggi.\n2. Waspadai pohon tumbang, banjir luapan sungai, dan tanah longsor.\n3. Tetap tenang dan ikuti arahan resmi Posko PB BPBD Kabupaten Mukomuko.",
                            fontSize = 11.sp,
                            color = if (isDark) Color(0xFFFEE2E2) else Color(0xFF7F1D1D),
                            lineHeight = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons: Call 117 & SOP Evakuasi
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            onCall117()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("popup_btn_call_117")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneInTalk,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Telepon 117",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = {
                            onOpenEvacuationMap()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("popup_btn_sop_evacuation")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SOP Evakuasi",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF573E32) else Color(0xFFD1D5DB)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("popup_btn_dismiss")
                ) {
                    Text(
                        text = "Mengerti & Tetap Siaga",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) Color(0xFFFED7AA) else Color(0xFF4B5563)
                    )
                }
            }
        }
    }
}

@Composable
private fun WarningCardItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    category: String,
    headline: String,
    eventDateText: String,
    durationText: String,
    detail: String,
    badgeText: String,
    isDark: Boolean
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isDark) Color(0xFF261814) else Color(0xFFFFF7ED),
        border = BorderStroke(1.dp, iconTint.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = category,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = iconTint,
                        letterSpacing = 0.4.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = iconTint,
                        border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.4f)),
                        modifier = Modifier.wrapContentWidth()
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = headline,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFFFFF7ED) else Color(0xFF1F2937)
                )

                Spacer(modifier = Modifier.height(5.dp))

                // Informasi Tanggal Kejadian & Durasi Berlaku (1-2 Jam) agar tidak membingungkan pengguna
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDark) Color(0xFF1A110E) else Color(0xFFFFEDD5).copy(alpha = 0.7f),
                    border = BorderStroke(0.8.dp, iconTint.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Event,
                                contentDescription = null,
                                tint = iconTint,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = eventDateText,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) Color(0xFFFED7AA) else Color(0xFF7C2D12)
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = iconTint,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = durationText,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isDark) Color(0xFFFDBA74) else Color(0xFF9A3412)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    text = detail,
                    fontSize = 11.sp,
                    color = if (isDark) Color(0xFFE2D6CF) else Color(0xFF4B5563),
                    lineHeight = 15.sp
                )
            }
        }
    }
}
