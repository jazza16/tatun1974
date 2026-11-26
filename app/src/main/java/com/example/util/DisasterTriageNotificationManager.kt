package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.data.local.KarhutlaHotspotEntity
import com.example.data.local.OfficerAuthManager
import com.example.data.repository.PoskoLogisticCommodity
import com.example.model.AirQuality
import com.example.model.BmkgEarthquake
import com.example.model.CitizenDisasterReport
import com.example.model.DetailedEarlyWarning
import com.example.model.EarthquakeUrgency
import com.example.model.MarineWeather
import com.example.model.TidalData
import com.example.model.WeatherFullReport
import java.util.Locale

/**
 * Standar Triase Pewarnaan Siaga Bencana BPBD Kabupaten Mukomuko (SIPAGAR KITA):
 * - HIJAU (Aman / Normal): Hening, tidak mengirim notifikasi.
 * - KUNING (Waspada): Pemberitahuan kesiapsiagaan tingkat menengah.
 * - ORANYE (Siaga): Peringatan prioritas tinggi dengan getaran & tombol darurat 117.
 * - MERAH (Awas / Bahaya / Kritis): Alarm darurat prioritas maksimal + aksi panggil 117.
 */
enum class DisasterTriageLevel(
    val code: String,
    val statusLabel: String,
    val badgeLabel: String,
    val colorInt: Int,
    val colorHex: Long,
    val emoji: String,
    val priorityScore: Int,
    val cooldownMillis: Long
) {
    HIJAU_AMAN(
        code = "HIJAU",
        statusLabel = "Aman / Normal",
        badgeLabel = "TRIASE HIJAU • AMAN",
        colorInt = 0xFF16A34A.toInt(),
        colorHex = 0xFF16A34A,
        emoji = "🟢",
        priorityScore = 0,
        cooldownMillis = Long.MAX_VALUE
    ),
    KUNING_WASPADA(
        code = "KUNING",
        statusLabel = "Waspada",
        badgeLabel = "TRIASE KUNING • WASPADA",
        colorInt = 0xFFD97706.toInt(),
        colorHex = 0xFFD97706,
        emoji = "🟡",
        priorityScore = 1,
        cooldownMillis = 60L * 60_000L // 60 menit
    ),
    ORANYE_SIAGA(
        code = "ORANYE",
        statusLabel = "Siaga",
        badgeLabel = "TRIASE ORANYE • SIAGA",
        colorInt = 0xFFEA580C.toInt(),
        colorHex = 0xFFEA580C,
        emoji = "🟠",
        priorityScore = 2,
        cooldownMillis = 30L * 60_000L // 30 menit
    ),
    MERAH_AWAS(
        code = "MERAH",
        statusLabel = "Awas / Bahaya",
        badgeLabel = "TRIASE MERAH • AWAS",
        colorInt = 0xFFDC2626.toInt(),
        colorHex = 0xFFDC2626,
        emoji = "🔴",
        priorityScore = 3,
        cooldownMillis = 15L * 60_000L // 15 menit
    );

    companion object {
        fun fromCode(code: String?): DisasterTriageLevel {
            return entries.firstOrNull { it.code.equals(code?.trim(), ignoreCase = true) }
                ?: ORANYE_SIAGA
        }
    }
}

/**
 * Modul/Fitur sumber evaluasi tingkat Siaga Bencana di dalam aplikasi SIPAGAR KITA.
 * `targetTabIndex` diselaraskan dengan daftar tab di `WeatherTabs.kt`.
 */
enum class TriageFeatureSource(
    val key: String,
    val displayName: String,
    val targetTabIndex: Int,
    val requiresOfficerSession: Boolean = false
) {
    PERINGATAN_CUACA("peringatan_cuaca", "Peringatan Dini Cuaca", targetTabIndex = 4),
    CUACA_EKSTREM("cuaca_ekstrem", "Telemetri Cuaca Ekstrem", targetTabIndex = 4),
    GEMPA_TSUNAMI("gempa_tsunami", "Gempa Bumi & Tsunami WRS", targetTabIndex = 4),
    MARITIM_GELOMBANG("maritim_gelombang", "Maritim & Gelombang Laut", targetTabIndex = 9),
    PASANG_ROB("pasang_rob", "Pasang Surut & Banjir Rob", targetTabIndex = 9),
    KARHUTLA_HOTSPOT("karhutla_hotspot", "Monitoring Karhutla (FIRMS/BMKG)", targetTabIndex = 5),
    KUALITAS_UDARA("kualitas_udara", "Kualitas Udara & Kabut Asap", targetTabIndex = 10),
    LAPORAN_DARURAT_WARGA("laporan_darurat_warga", "Eskalasi Laporan Bencana", targetTabIndex = 2),
    POSKO_LOGISTIK_KRITIS(
        key = "posko_logistik_kritis",
        displayName = "Logistik & FEFO Posko (Petugas)",
        targetTabIndex = 1,
        requiresOfficerSession = true
    )
}

/**
 * Hasil evaluasi triase pewarnaan dari satu modul/fitur aplikasi.
 */
data class DisasterTriageSignal(
    val source: TriageFeatureSource,
    val level: DisasterTriageLevel,
    val districtOrLocation: String,
    val headline: String,
    val metricSummary: String,
    val safetyAdvice: String,
    val signatureKey: String,
    val targetTabOverride: Int? = null
) {
    val resolvedTargetTab: Int
        get() = targetTabOverride ?: source.targetTabIndex
}

/**
 * Pengaturan preferensi notifikasi berbasis Triase Pewarnaan Siaga Bencana.
 */
data class TriageNotificationSettings(
    val isEnabled: Boolean = true,
    val minimumLevel: DisasterTriageLevel = DisasterTriageLevel.ORANYE_SIAGA,
    val enableWeatherAndAlerts: Boolean = true,
    val enableEarthquakeTsunami: Boolean = true,
    val enableMarineAndRob: Boolean = true,
    val enableKarhutlaAndAirQuality: Boolean = true,
    val enableDisasterReportEscalation: Boolean = true,
    val enableOfficerPoskoAlerts: Boolean = true
) {
    fun isSourceEnabled(source: TriageFeatureSource): Boolean {
        if (!isEnabled) return false
        return when (source) {
            TriageFeatureSource.PERINGATAN_CUACA,
            TriageFeatureSource.CUACA_EKSTREM -> enableWeatherAndAlerts
            TriageFeatureSource.GEMPA_TSUNAMI -> enableEarthquakeTsunami
            TriageFeatureSource.MARITIM_GELOMBANG,
            TriageFeatureSource.PASANG_ROB -> enableMarineAndRob
            TriageFeatureSource.KARHUTLA_HOTSPOT,
            TriageFeatureSource.KUALITAS_UDARA -> enableKarhutlaAndAirQuality
            TriageFeatureSource.LAPORAN_DARURAT_WARGA -> enableDisasterReportEscalation
            TriageFeatureSource.POSKO_LOGISTIK_KRITIS -> enableOfficerPoskoAlerts
        }
    }
}

enum class TriageDispatchResult {
    POSTED,
    SKIPPED_SAFE_GREEN,
    SKIPPED_DISABLED,
    SKIPPED_BELOW_THRESHOLD,
    SKIPPED_OFFICER_SESSION_REQUIRED,
    SKIPPED_COOLDOWN_DUPLICATE,
    SKIPPED_NO_PERMISSION
}

/**
 * Mesin Terpadu Evaluasi & Notifikasi Siaga Bencana Berbasis Triase Pewarnaan SIPAGAR KITA.
 *
 * Fitur utama:
 * 1. Menyatukan seluruh modul pemantauan (Cuaca, Peringatan Dini, Gempa/Tsunami, Maritim,
 *    Pasang Rob, Karhutla FIRMS/BMKG, Kualitas Udara, Lonjakan Laporan, dan Logistik Posko Petugas).
 * 2. Menggunakan 3 NotificationChannel khusus sesuai warna triase (Merah, Oranye, Kuning) dan
 *    hening otomatis pada kondisi Hijau/Aman.
 * 3. Deduplikasi & Cooldown pintar berbasis SharedPreferences agar tidak spam, tetapi langsung
 *    menembus cooldown saat terjadi eskalasi tingkat bahaya (mis. Kuning -> Oranye -> Merah).
 * 4. Isolasi Mode Warga ketat: sinyal internal petugas (`POSKO_LOGISTIK_KRITIS`) tidak akan pernah
 *    dipicu jika sesi login petugas tidak aktif.
 */
object DisasterTriageNotificationManager {

    const val CHANNEL_TRIAGE_MERAH = "sipagar_triage_merah_awas"
    const val CHANNEL_TRIAGE_ORANYE = "sipagar_triage_oranye_siaga"
    const val CHANNEL_TRIAGE_KUNING = "sipagar_triage_kuning_waspada"

    const val EXTRA_NAVIGATE_TAB = "EXTRA_NAVIGATE_TAB"
    const val EXTRA_TRIAGE_LEVEL = "EXTRA_TRIAGE_LEVEL"
    const val EXTRA_TRIAGE_SOURCE = "EXTRA_TRIAGE_SOURCE"

    private const val PREFS_NAME = "sipagar_disaster_triage_notif_prefs"
    private const val KEY_ENABLED = "triage_enabled"
    private const val KEY_MIN_LEVEL = "triage_min_level"
    private const val KEY_ENABLE_WEATHER = "triage_enable_weather"
    private const val KEY_ENABLE_EARTHQUAKE = "triage_enable_earthquake"
    private const val KEY_ENABLE_MARINE = "triage_enable_marine"
    private const val KEY_ENABLE_KARHUTLA = "triage_enable_karhutla"
    private const val KEY_ENABLE_SURGE = "triage_enable_surge"
    private const val KEY_ENABLE_POSKO = "triage_enable_posko"

    private const val NOTIFICATION_ID_BASE = 74000

    fun initNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val redChannel = NotificationChannel(
            CHANNEL_TRIAGE_MERAH,
            "Triase Merah • Siaga Awas / Darurat Bencana",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alarm darurat prioritas maksimal saat fitur aplikasi mencatat status AWAS / MERAH (Tsunami, Gempa Kuat, Cuaca Ekstrem, Karhutla Kritis)."
            enableLights(true)
            lightColor = DisasterTriageLevel.MERAH_AWAS.colorInt
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 500, 150, 500, 150, 700)
        }

        val orangeChannel = NotificationChannel(
            CHANNEL_TRIAGE_ORANYE,
            "Triase Oranye • Siaga Bencana",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Peringatan prioritas tinggi saat modul pemantauan mencatat status SIAGA / ORANYE (Gelombang Tinggi, Banjir Rob, Hujan Lebat, Titik Panas)."
            enableLights(true)
            lightColor = DisasterTriageLevel.ORANYE_SIAGA.colorInt
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 350, 150, 350, 150, 450)
        }

        val yellowChannel = NotificationChannel(
            CHANNEL_TRIAGE_KUNING,
            "Triase Kuning • Waspada Bencana",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Pemberitahuan kesiapsiagaan saat modul aplikasi mencatat tingkat WASPADA / KUNING."
            enableLights(true)
            lightColor = DisasterTriageLevel.KUNING_WASPADA.colorInt
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 220, 140, 220)
        }

        manager.createNotificationChannel(redChannel)
        manager.createNotificationChannel(orangeChannel)
        manager.createNotificationChannel(yellowChannel)
    }

    fun getSettings(context: Context): TriageNotificationSettings {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val minLevelCode = prefs.getString(KEY_MIN_LEVEL, DisasterTriageLevel.ORANYE_SIAGA.code)
        val parsedLevel = DisasterTriageLevel.fromCode(minLevelCode).let {
            if (it == DisasterTriageLevel.HIJAU_AMAN) DisasterTriageLevel.KUNING_WASPADA else it
        }
        return TriageNotificationSettings(
            isEnabled = prefs.getBoolean(KEY_ENABLED, true),
            minimumLevel = parsedLevel,
            enableWeatherAndAlerts = prefs.getBoolean(KEY_ENABLE_WEATHER, true),
            enableEarthquakeTsunami = prefs.getBoolean(KEY_ENABLE_EARTHQUAKE, true),
            enableMarineAndRob = prefs.getBoolean(KEY_ENABLE_MARINE, true),
            enableKarhutlaAndAirQuality = prefs.getBoolean(KEY_ENABLE_KARHUTLA, true),
            enableDisasterReportEscalation = prefs.getBoolean(KEY_ENABLE_SURGE, true),
            enableOfficerPoskoAlerts = prefs.getBoolean(KEY_ENABLE_POSKO, true)
        )
    }

    fun saveSettings(context: Context, settings: TriageNotificationSettings) {
        val normalizedMin = if (settings.minimumLevel == DisasterTriageLevel.HIJAU_AMAN) {
            DisasterTriageLevel.KUNING_WASPADA
        } else {
            settings.minimumLevel
        }
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_ENABLED, settings.isEnabled)
            .putString(KEY_MIN_LEVEL, normalizedMin.code)
            .putBoolean(KEY_ENABLE_WEATHER, settings.enableWeatherAndAlerts)
            .putBoolean(KEY_ENABLE_EARTHQUAKE, settings.enableEarthquakeTsunami)
            .putBoolean(KEY_ENABLE_MARINE, settings.enableMarineAndRob)
            .putBoolean(KEY_ENABLE_KARHUTLA, settings.enableKarhutlaAndAirQuality)
            .putBoolean(KEY_ENABLE_SURGE, settings.enableDisasterReportEscalation)
            .putBoolean(KEY_ENABLE_POSKO, settings.enableOfficerPoskoAlerts)
            .apply()
    }

    fun clearCooldownHistory(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        TriageFeatureSource.entries.forEach { src ->
            editor.remove("last_time_${src.key}")
            editor.remove("last_score_${src.key}")
            editor.remove("last_sig_${src.key}")
        }
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

    fun channelIdForLevel(level: DisasterTriageLevel): String = when (level) {
        DisasterTriageLevel.MERAH_AWAS -> CHANNEL_TRIAGE_MERAH
        DisasterTriageLevel.ORANYE_SIAGA -> CHANNEL_TRIAGE_ORANYE
        DisasterTriageLevel.KUNING_WASPADA -> CHANNEL_TRIAGE_KUNING
        DisasterTriageLevel.HIJAU_AMAN -> CHANNEL_TRIAGE_KUNING
    }

    fun notificationIdForSource(source: TriageFeatureSource): Int =
        NOTIFICATION_ID_BASE + source.ordinal

    // =========================================================================
    // EVALUATOR TRIASE PEWARNAAN PER FITUR / MODUL APLIKASI
    // =========================================================================

    fun evaluateEarlyWarnings(
        warnings: List<DetailedEarlyWarning>,
        districtName: String
    ): DisasterTriageSignal {
        val prioritized = warnings.maxByOrNull { severityToTriageLevel(it.severity).priorityScore }
        if (prioritized == null) {
            return safeSignal(
                source = TriageFeatureSource.PERINGATAN_CUACA,
                districtOrLocation = districtName,
                summary = "Tidak ada peringatan dini cuaca aktif."
            )
        }
        val level = severityToTriageLevel(prioritized.severity)
        if (level == DisasterTriageLevel.HIJAU_AMAN) {
            return safeSignal(TriageFeatureSource.PERINGATAN_CUACA, districtName, "Kondisi cuaca terpantau normal.")
        }
        val advice = prioritized.safetyInstructions.firstOrNull()
            ?: "Pantau pembaruan resmi Pusdalops BPBD Mukomuko & BMKG."
        return DisasterTriageSignal(
            source = TriageFeatureSource.PERINGATAN_CUACA,
            level = level,
            districtOrLocation = districtName,
            headline = prioritized.title,
            metricSummary = "${prioritized.category} • Berlaku s/d ${prioritized.validUntil}",
            safetyAdvice = advice,
            signatureKey = "${prioritized.id}|${level.code}|$districtName"
        )
    }

    fun evaluateWeatherTelemetry(report: WeatherFullReport): DisasterTriageSignal {
        val districtName = report.district.name
        val current = report.current
        val wind = current.windSpeed
        val precip = current.precipitation
        val code = current.weatherCode
        val conditionText = current.weatherInfo.conditionText
        val uv = report.daily.firstOrNull()?.uvIndexMax ?: 0.0

        val (level, headline, metric, advice) = when {
            wind >= 60.0 || precip >= 30.0 || code in setOf(96, 99) || (code >= 95 && wind >= 45.0) -> Quad(
                DisasterTriageLevel.MERAH_AWAS,
                "Cuaca Ekstrem & Badai Petir Berbahaya",
                "Angin ${wind.toInt()} km/jam • Hujan ${String.format(Locale.US, "%.1f", precip)} mm • $conditionText • ${current.temperature.toInt()}°C",
                "Segera berlindung di dalam bangunan kokoh, jauhi pohon rindang, tiang listrik, dan papan reklame."
            )
            wind >= 40.0 || precip >= 15.0 || code in setOf(65, 82, 95) || uv >= 11.0 -> Quad(
                DisasterTriageLevel.ORANYE_SIAGA,
                "Siaga Hujan Lebat / Angin Kencang",
                "Angin ${wind.toInt()} km/jam • Hujan ${String.format(Locale.US, "%.1f", precip)} mm • $conditionText",
                "Waspadai genangan air, pohon tumbang, dan kurangi aktivitas luar ruangan serta pelayaran."
            )
            wind >= 28.0 || precip >= 7.0 || code in setOf(63, 81) || uv >= 8.0 -> Quad(
                DisasterTriageLevel.KUNING_WASPADA,
                "Waspada Perubahan Cuaca Signifikan",
                "Angin ${wind.toInt()} km/jam • $conditionText • UV ${String.format(Locale.US, "%.1f", uv)}",
                "Siapkan perlengkapan pelindung hujan/panas dan pantau perkembangan cuaca kecamatan."
            )
            else -> return safeSignal(
                source = TriageFeatureSource.CUACA_EKSTREM,
                districtOrLocation = districtName,
                summary = "Cuaca $conditionText, angin ${wind.toInt()} km/jam (Normal)."
            )
        }

        return DisasterTriageSignal(
            source = TriageFeatureSource.CUACA_EKSTREM,
            level = level,
            districtOrLocation = districtName,
            headline = headline,
            metricSummary = metric,
            safetyAdvice = advice,
            signatureKey = "weather_${report.district.id}_${level.code}_$code"
        )
    }

    fun evaluateMarineWave(
        marine: MarineWeather,
        districtName: String
    ): DisasterTriageSignal {
        val h = marine.waveHeight
        val condLower = marine.seaCondition.lowercase(Locale.ROOT)
        val safetyLower = marine.fishermanSafety.lowercase(Locale.ROOT)

        val (level, headline, advice) = when {
            h >= 4.0 || condLower.contains("ekstrem") || condLower.contains("sangat tinggi") || safetyLower.contains("bahaya") -> Triple(
                DisasterTriageLevel.MERAH_AWAS,
                "Gelombang Laut Sangat Tinggi / Berbahaya",
                "LARANGAN MELAUT untuk perahu nelayan & kapal kecil. Amankan perahu ke tambatan terlindung."
            )
            h >= 2.5 || condLower.contains("tinggi") || safetyLower.contains("tunda") || safetyLower.contains("siaga") -> Triple(
                DisasterTriageLevel.ORANYE_SIAGA,
                "Siaga Gelombang Tinggi Perairan Mukomuko",
                "Nelayan perahu katir & kapal motor tradisional diimbau menunda pelayaran sementara."
            )
            h >= 1.25 || condLower.contains("sedang") || safetyLower.contains("waspada") -> Triple(
                DisasterTriageLevel.KUNING_WASPADA,
                "Waspada Gelombang Sedang Samudra Hindia",
                "Tingkatkan kewaspadaan saat melaut dan gunakan jaket pelampung serta alat komunikasi aktif."
            )
            else -> return safeSignal(
                source = TriageFeatureSource.MARITIM_GELOMBANG,
                districtOrLocation = districtName,
                summary = "Tinggi gelombang ${String.format(Locale.US, "%.1f", h)} m (${marine.seaCondition})."
            )
        }

        return DisasterTriageSignal(
            source = TriageFeatureSource.MARITIM_GELOMBANG,
            level = level,
            districtOrLocation = districtName,
            headline = headline,
            metricSummary = "Gelombang ${String.format(Locale.US, "%.1f", h)} m • ${marine.seaCondition} • ${marine.fishermanSafety}",
            safetyAdvice = advice,
            signatureKey = "marine_${districtName}_${level.code}_${String.format(Locale.US, "%.1f", h)}"
        )
    }

    fun evaluateTidalRob(
        tidalData: TidalData,
        districtName: String
    ): DisasterTriageSignal {
        val riskLower = tidalData.robFloodRiskLevel.lowercase(Locale.ROOT)
        val wl = tidalData.currentWaterLevelMeters.toDouble()

        val (level, headline, advice) = when {
            riskLower.contains("awas") || riskLower.contains("bahaya") || riskLower.contains("kritis") || wl >= 2.6 -> Triple(
                DisasterTriageLevel.MERAH_AWAS,
                "Awas Pasang Maksimum & Banjir Rob Pesisir",
                "Segera amankan barang berharga, matikan instalasi listrik area tergenang, dan jauhi bibir pantai."
            )
            riskLower.contains("siaga") || riskLower.contains("tinggi") || wl >= 2.1 -> Triple(
                DisasterTriageLevel.ORANYE_SIAGA,
                "Siaga Potensi Banjir Rob Pesisir",
                "Warga pesisir & tambak waspada luapan air pasang menuju permukiman dataran rendah."
            )
            riskLower.contains("waspada") || riskLower.contains("sedang") || wl >= 1.7 -> Triple(
                DisasterTriageLevel.KUNING_WASPADA,
                "Waspada Kenaikan Pasang Air Laut",
                "Pantau tinggi muka air laut saat ${tidalData.nextEventType} pukul ${tidalData.nextEventTime} (${tidalData.highTideEveningHeight})."
            )
            else -> return safeSignal(
                source = TriageFeatureSource.PASANG_ROB,
                districtOrLocation = districtName,
                summary = "Muka air laut ${String.format(Locale.US, "%.2f", wl)} m • Risiko Rob: ${tidalData.robFloodRiskLevel}."
            )
        }

        return DisasterTriageSignal(
            source = TriageFeatureSource.PASANG_ROB,
            level = level,
            districtOrLocation = districtName,
            headline = headline,
            metricSummary = "Muka Air ${String.format(Locale.US, "%.2f", wl)} m (${tidalData.tideMovementText}) • Risiko Rob: ${tidalData.robFloodRiskLevel}",
            safetyAdvice = advice,
            signatureKey = "tidal_${districtName}_${level.code}_${tidalData.robFloodRiskLevel}"
        )
    }

    fun evaluateAirQuality(
        airQuality: AirQuality,
        districtName: String
    ): DisasterTriageSignal {
        val statusLower = airQuality.status.lowercase(Locale.ROOT)
        val aqi = airQuality.aqi
        val pm25 = airQuality.pm25

        val (level, headline, advice) = when {
            aqi >= 201 || pm25 >= 150.5 || statusLower.contains("berbahaya") || statusLower.contains("sangat tidak sehat") -> Triple(
                DisasterTriageLevel.MERAH_AWAS,
                "Kualitas Udara Berbahaya / Kabut Asap Pekat",
                "Hentikan aktivitas luar ruangan, tutup ventilasi rumah, gunakan masker N95, lindungi balita & lansia."
            )
            aqi >= 151 || pm25 >= 55.5 || statusLower.contains("tidak sehat") -> Triple(
                DisasterTriageLevel.ORANYE_SIAGA,
                "Siaga Kualitas Udara Tidak Sehat",
                "Kurangi aktivitas fisik di luar ruangan dan gunakan masker pelindung pernapasan."
            )
            aqi >= 101 || pm25 >= 35.5 || statusLower.contains("sensitif") -> Triple(
                DisasterTriageLevel.KUNING_WASPADA,
                "Waspada Penurunan Kualitas Udara",
                "Kelompok rentan (balita, lansia, penderita asma) disarankan membatasi paparan udara luar."
            )
            else -> return safeSignal(
                source = TriageFeatureSource.KUALITAS_UDARA,
                districtOrLocation = districtName,
                summary = "AQI $aqi • PM2.5 ${String.format(Locale.US, "%.1f", pm25)} µg/m³ (${airQuality.status})."
            )
        }

        return DisasterTriageSignal(
            source = TriageFeatureSource.KUALITAS_UDARA,
            level = level,
            districtOrLocation = districtName,
            headline = headline,
            metricSummary = "AQI $aqi (${airQuality.status}) • PM2.5 ${String.format(Locale.US, "%.1f", pm25)} µg/m³",
            safetyAdvice = advice,
            signatureKey = "aqi_${districtName}_${level.code}"
        )
    }

    fun evaluateEarthquake(earthquake: BmkgEarthquake): DisasterTriageSignal {
        // FILTER WILAYAH LOKAL PROVINSI BENGKULU:
        // Notifikasi Gempa Bumi HANYA berlaku untuk gempa lokal yang terjadi di Provinsi Bengkulu
        // atau tercatat dirasakan oleh masyarakat Provinsi Bengkulu. Gempa luar Bengkulu (global Indonesia)
        // selalu dikembalikan sebagai HIJAU_AMAN agar tidak mengganggu pengguna.
        if (!earthquake.isBengkuluProvinceAffected) {
            return safeSignal(
                source = TriageFeatureSource.GEMPA_TSUNAMI,
                districtOrLocation = earthquake.region,
                summary = "Gempa M ${earthquake.magnitude} di luar Provinsi Bengkulu (${earthquake.region}) — notifikasi dibisukan (filter lokal Provinsi Bengkulu)."
            )
        }

        val hasTsunamiPotential = earthquake.potentialTsunami.contains("BERPOTENSI TSUNAMI", ignoreCase = true) &&
            !earthquake.potentialTsunami.contains("tidak berpotensi", ignoreCase = true)
        val hasFeltInBengkulu = com.example.model.BengkuluEarthquakeScope.containsBengkuluKeyword(earthquake.feltDescription)

        val (level, headline, advice, targetTab) = when {
            hasTsunamiPotential -> QuadTab(
                DisasterTriageLevel.MERAH_AWAS,
                "AWAS TSUNAMI BENGKULU! Gempa M ${earthquake.magnitude}",
                "Segera evakuasi mandiri ke dataran tinggi (>= 20m dpl / >= 2 km dari pantai). Rumus 20-20-20!",
                6
            )
            earthquake.magnitude >= 6.0 || (earthquake.isNearMukomuko && earthquake.magnitude >= 5.0) -> QuadTab(
                DisasterTriageLevel.MERAH_AWAS,
                "Gempa Kuat M ${earthquake.magnitude} Wilayah Bengkulu",
                "Lakukan Drop-Cover-Hold On, keluar bangunan secara tertib setelah guncangan reda, waspada gempa susulan.",
                4
            )
            earthquake.magnitude >= 5.0 || (earthquake.isNearMukomuko && earthquake.magnitude >= 4.5) -> QuadTab(
                DisasterTriageLevel.ORANYE_SIAGA,
                "Siaga Gempa Lokal Bengkulu M ${earthquake.magnitude} (${earthquake.depth})",
                "Periksa kondisi bangunan sekitar dan pantau rilis resmi BMKG serta arahan Pusdalops BPBD Bengkulu/Mukomuko.",
                4
            )
            earthquake.magnitude >= 3.0 || hasFeltInBengkulu -> QuadTab(
                DisasterTriageLevel.KUNING_WASPADA,
                "Waspada Gempa Lokal Terasa M ${earthquake.magnitude} (Bengkulu)",
                "Tetap tenang, pantau informasi resmi BMKG dan hindari bangunan retak.",
                4
            )
            else -> return safeSignal(
                source = TriageFeatureSource.GEMPA_TSUNAMI,
                districtOrLocation = earthquake.region,
                summary = "Gempa mikro lokal M ${earthquake.magnitude} (${earthquake.region}) — tidak berdampak signifikan."
            )
        }

        val feltSuffix = if (earthquake.feltDescription.isNotBlank() && earthquake.feltDescription != "-") {
            " • Dirasakan: ${earthquake.feltDescription}"
        } else {
            ""
        }

        return DisasterTriageSignal(
            source = TriageFeatureSource.GEMPA_TSUNAMI,
            level = level,
            districtOrLocation = earthquake.region,
            headline = headline,
            metricSummary = "M ${earthquake.magnitude} • Kedalaman ${earthquake.depth} • ${earthquake.timeWib} • Jarak ~${earthquake.distanceKmToMukomuko} km$feltSuffix",
            safetyAdvice = advice,
            signatureKey = "eq_${earthquake.id}_${level.code}",
            targetTabOverride = targetTab
        )
    }

    /**
     * Memilih kejadian gempa lokal Provinsi Bengkulu dengan tingkat triase tertinggi dari daftar gempa
     * BMKG (gabungan autogempa + gempadirasakan). Mengembalikan HIJAU_AMAN bila tidak ada gempa
     * yang terjadi atau dirasakan di Wilayah Provinsi Bengkulu.
     */
    fun evaluateEarthquakesForBengkulu(
        earthquakes: List<BmkgEarthquake>,
        fallbackLocation: String = "Provinsi Bengkulu"
    ): DisasterTriageSignal {
        val localSignals = earthquakes
            .filter { it.isBengkuluProvinceAffected }
            .map { evaluateEarthquake(it) }
        return localSignals.maxByOrNull { it.level.priorityScore }
            ?: safeSignal(
                source = TriageFeatureSource.GEMPA_TSUNAMI,
                districtOrLocation = fallbackLocation,
                summary = "Tidak ada gempa lokal atau dirasakan di Wilayah Provinsi Bengkulu saat ini."
            )
    }

    fun evaluateKarhutlaHotspots(
        hotspots: List<KarhutlaHotspotEntity>,
        fallbackDistrict: String = "Kab. Mukomuko",
        includeSimulation: Boolean = false
    ): DisasterTriageSignal {
        val active = if (includeSimulation) hotspots else hotspots.filter { !it.isSimulation }
        if (active.isEmpty()) {
            return safeSignal(
                source = TriageFeatureSource.KARHUTLA_HOTSPOT,
                districtOrLocation = fallbackDistrict,
                summary = "Nihil titik panas (hotspot) terdeteksi di Kabupaten Mukomuko."
            )
        }

        val highCount = active.count { it.confidenceLabel.equals("HIGH", ignoreCase = true) }
        val nominalCount = active.count { it.confidenceLabel.equals("NOMINAL", ignoreCase = true) }
        val matchedCount = active.count { it.isMatchedWithCrossSource }
        val primaryDistrict = active
            .groupingBy { it.nearestSubDistrict.ifBlank { fallbackDistrict } }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key ?: fallbackDistrict

        val (level, headline, advice) = when {
            highCount >= 2 || (highCount >= 1 && matchedCount >= 1) || active.size >= 10 -> Triple(
                DisasterTriageLevel.MERAH_AWAS,
                "Awas Klaster Titik Panas Karhutla Kritis",
                "Segera laporkan asap/api ke Pusdalops 117, hentikan pembakaran lahan, dan siagakan TRC pemadaman."
            )
            highCount >= 1 || matchedCount >= 1 || nominalCount >= 3 -> Triple(
                DisasterTriageLevel.ORANYE_SIAGA,
                "Siaga Titik Panas Karhutla Terdeteksi Satelit",
                "Tingkatkan patroli darat di sekitar Kec. $primaryDistrict dan verifikasi lapangan segera."
            )
            nominalCount >= 1 || active.size >= 2 -> Triple(
                DisasterTriageLevel.KUNING_WASPADA,
                "Waspada Deteksi Awal Hotspot Karhutla",
                "Pantau perkembangan satelit NASA FIRMS & BMKG serta hindari aktivitas pemicu percikan api."
            )
            else -> return safeSignal(
                source = TriageFeatureSource.KARHUTLA_HOTSPOT,
                districtOrLocation = primaryDistrict,
                summary = "${active.size} titik confidence rendah — dalam pemantauan normal."
            )
        }

        return DisasterTriageSignal(
            source = TriageFeatureSource.KARHUTLA_HOTSPOT,
            level = level,
            districtOrLocation = primaryDistrict,
            headline = headline,
            metricSummary = "Total ${active.size} titik panas • High: $highCount • Terkonfirmasi Ganda: $matchedCount",
            safetyAdvice = advice,
            signatureKey = "karhutla_${level.code}_${primaryDistrict}_${active.size}_$highCount"
        )
    }

    fun evaluateDisasterReportSurge(
        reports: List<CitizenDisasterReport>,
        fallbackDistrict: String = "Kab. Mukomuko"
    ): DisasterTriageSignal {
        val activeReports = reports.filter { rep ->
            !com.example.model.CitizenReportStatusHelper.isResolved(rep.status)
        }
        if (activeReports.isEmpty()) {
            return safeSignal(
                source = TriageFeatureSource.LAPORAN_DARURAT_WARGA,
                districtOrLocation = fallbackDistrict,
                summary = "Tidak ada laporan kejadian bencana aktif yang menunggu penanganan."
            )
        }

        val criticalCount = activeReports.count { rep ->
            val typeLower = rep.disasterType.lowercase(Locale.ROOT)
            val notesLower = rep.additionalNotes.lowercase(Locale.ROOT)
            rep.casualtiesDead > 0 ||
                rep.casualtiesMissing > 0 ||
                rep.casualtiesInjured >= 3 ||
                typeLower.contains("tsunami") ||
                typeLower.contains("banjir bandang") ||
                typeLower.contains("tanah longsor") ||
                notesLower.contains("darurat") ||
                notesLower.contains("kritis")
        }
        val topDistrict = activeReports
            .groupingBy { rep -> rep.district.ifBlank { fallbackDistrict } }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key ?: fallbackDistrict

        val (level, headline, advice) = when {
            criticalCount >= 3 || activeReports.size >= 8 -> Triple(
                DisasterTriageLevel.MERAH_AWAS,
                "Awas Eskalasi Kejadian Bencana Aktif",
                "Terdapat $criticalCount laporan berstatus darurat/berdampak korban. Ikuti arahan evakuasi BPBD & hubungi 117 bila terancam."
            )
            criticalCount >= 1 || activeReports.size >= 4 -> Triple(
                DisasterTriageLevel.ORANYE_SIAGA,
                "Siaga Laporan Kejadian Bencana Aktif",
                "Waspadai kondisi lingkungan sekitar Kec. $topDistrict dan pantau status penanganan Pusdalops."
            )
            activeReports.size >= 2 -> Triple(
                DisasterTriageLevel.KUNING_WASPADA,
                "Waspada Peningkatan Laporan Bencana Warga",
                "Tetap waspada terhadap potensi dampak lanjutan di wilayah terdampak."
            )
            else -> return safeSignal(
                source = TriageFeatureSource.LAPORAN_DARURAT_WARGA,
                districtOrLocation = topDistrict,
                summary = "${activeReports.size} laporan aktif dalam pemantauan standar."
            )
        }

        return DisasterTriageSignal(
            source = TriageFeatureSource.LAPORAN_DARURAT_WARGA,
            level = level,
            districtOrLocation = topDistrict,
            headline = headline,
            metricSummary = "${activeReports.size} laporan aktif ($criticalCount prioritas darurat) • Fokus: $topDistrict",
            safetyAdvice = advice,
            signatureKey = "surge_${level.code}_${activeReports.size}_$criticalCount"
        )
    }

    /**
     * Evaluator KHUSUS PETUGAS (Posko & Logistik).
     * Isolasi Mode Warga dijaga ketat pada `evaluateAndNotify(...)`: sinyal ini tidak pernah
     * dikirimkan apabila sesi login petugas tidak aktif.
     */
    fun evaluatePoskoLogistics(
        commodities: List<PoskoLogisticCommodity>
    ): DisasterTriageSignal {
        if (commodities.isEmpty()) {
            return safeSignal(
                source = TriageFeatureSource.POSKO_LOGISTIK_KRITIS,
                districtOrLocation = "Gudang & Posko BPBD",
                summary = "Belum ada data komoditas logistik posko."
            )
        }

        val criticalItems = commodities.filter { it.isCritical }
        val expiredItems = commodities.filter { it.isExpired }
        val expiringSoonItems = commodities.filter { it.isExpiringSoonFefo }
        val lowBufferItems = commodities.filter { !it.isCritical && it.currentStock in it.minThreshold..(it.minThreshold + 4) }

        val (level, headline, metric, advice) = when {
            criticalItems.isNotEmpty() || expiredItems.isNotEmpty() -> {
                val sampleNames = (criticalItems + expiredItems).distinctBy { it.code }.take(2).joinToString(", ") {
                    "${it.name} (${it.currentStock} ${it.unit})"
                }
                Quad(
                    DisasterTriageLevel.MERAH_AWAS,
                    "Stok Logistik Posko KRITIS (< 10) / Kedaluwarsa",
                    "Kritis: ${criticalItems.size} item • Kedaluwarsa: ${expiredItems.size} item ($sampleNames)",
                    "Segera lakukan pengadaan/droping logistik darurat dan tarik item kedaluwarsa dari distribusi."
                )
            }
            expiringSoonItems.size >= 2 || lowBufferItems.isNotEmpty() -> Quad(
                DisasterTriageLevel.ORANYE_SIAGA,
                "Siaga Penipisan Stok / Peringatan FEFO Posko",
                "Menipis: ${lowBufferItems.size} item • FEFO <=30 hari: ${expiringSoonItems.size} item",
                "Prioritaskan penyaluran batch FEFO terdekat dan siapkan pengisian ulang gudang posko."
            )
            expiringSoonItems.size == 1 -> Quad(
                DisasterTriageLevel.KUNING_WASPADA,
                "Waspada 1 Komoditas Mendekati Batas FEFO",
                "Komoditas ${expiringSoonItems.first().name} mendekati kedaluwarsa (${expiringSoonItems.first().expiryDateIso ?: "-"}).",
                "Jadwalkan rotasi stok FEFO saat distribusi posko berikutnya."
            )
            else -> return safeSignal(
                source = TriageFeatureSource.POSKO_LOGISTIK_KRITIS,
                districtOrLocation = "Gudang & Posko BPBD",
                summary = "Seluruh ${commodities.size} komoditas logistik posko dalam batas aman."
            )
        }

        val poskoLoc = (criticalItems.firstOrNull() ?: expiringSoonItems.firstOrNull() ?: commodities.first()).poskoCode
        return DisasterTriageSignal(
            source = TriageFeatureSource.POSKO_LOGISTIK_KRITIS,
            level = level,
            districtOrLocation = poskoLoc,
            headline = headline,
            metricSummary = metric,
            safetyAdvice = advice,
            signatureKey = "posko_${level.code}_${criticalItems.size}_${expiredItems.size}_${expiringSoonItems.size}"
        )
    }

    // =========================================================================
    // DISPATCHER NOTIFIKASI DENGAN DEDUPLIKASI, ESKALASI & ISOLASI SESI PETUGAS
    // =========================================================================

    fun evaluateAndNotify(
        context: Context,
        signal: DisasterTriageSignal,
        forceBypassCooldown: Boolean = false,
        nowMillis: Long = System.currentTimeMillis()
    ): TriageDispatchResult {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val timeKey = "last_time_${signal.source.key}"
        val scoreKey = "last_score_${signal.source.key}"
        val sigKey = "last_sig_${signal.source.key}"

        // 1. HIJAU (Aman) tidak pernah memicu notifikasi & mereset status eskalasi sebelumnya
        if (signal.level == DisasterTriageLevel.HIJAU_AMAN) {
            prefs.edit()
                .putInt(scoreKey, DisasterTriageLevel.HIJAU_AMAN.priorityScore)
                .remove(sigKey)
                .apply()
            return TriageDispatchResult.SKIPPED_SAFE_GREEN
        }

        // 2. Isolasi Mode Warga: sinyal khusus petugas HANYA boleh muncul jika sesi petugas aktif
        if (signal.source.requiresOfficerSession && !OfficerAuthManager(appContext).isOfficerLoggedIn()) {
            return TriageDispatchResult.SKIPPED_OFFICER_SESSION_REQUIRED
        }

        // 3. Cek preferensi pengguna
        val settings = getSettings(appContext)
        if (!forceBypassCooldown && !settings.isSourceEnabled(signal.source)) {
            return TriageDispatchResult.SKIPPED_DISABLED
        }

        // 4. Cek ambang batas warna triase minimum
        if (!forceBypassCooldown && signal.level.priorityScore < settings.minimumLevel.priorityScore) {
            return TriageDispatchResult.SKIPPED_BELOW_THRESHOLD
        }

        // 5. Cek Deduplikasi & Eskalasi (naik level Kuning->Oranye->Merah selalu lolos cooldown!)
        val lastTime = prefs.getLong(timeKey, 0L)
        val lastScore = prefs.getInt(scoreKey, 0)
        val lastSig = prefs.getString(sigKey, null)
        val isEscalation = signal.level.priorityScore > lastScore

        if (!forceBypassCooldown &&
            !isEscalation &&
            lastSig == signal.signatureKey &&
            (nowMillis - lastTime) < signal.level.cooldownMillis
        ) {
            return TriageDispatchResult.SKIPPED_COOLDOWN_DUPLICATE
        }

        // 6. Cek izin POST_NOTIFICATIONS Android 13+
        initNotificationChannels(appContext)
        if (!hasNotificationPermission(appContext)) {
            return TriageDispatchResult.SKIPPED_NO_PERMISSION
        }

        // 7. Bangun dan tampilkan notifikasi sesuai warna triase
        val posted = postTriageNotificationInternal(appContext, signal)
        if (!posted) {
            return TriageDispatchResult.SKIPPED_NO_PERMISSION
        }

        // 8. Simpan jejak deduplikasi
        prefs.edit()
            .putLong(timeKey, nowMillis)
            .putInt(scoreKey, signal.level.priorityScore)
            .putString(sigKey, signal.signatureKey)
            .apply()

        return TriageDispatchResult.POSTED
    }

    /**
     * Memicu notifikasi uji coba untuk pratinjau visual & suara tiap warna triase (Kuning, Oranye, Merah).
     */
    fun postTestTriageNotification(
        context: Context,
        level: DisasterTriageLevel,
        districtName: String
    ): Boolean {
        if (level == DisasterTriageLevel.HIJAU_AMAN) return false
        val sampleSignal = when (level) {
            DisasterTriageLevel.MERAH_AWAS -> DisasterTriageSignal(
                source = TriageFeatureSource.PERINGATAN_CUACA,
                level = DisasterTriageLevel.MERAH_AWAS,
                districtOrLocation = districtName,
                headline = "Simulasi Status AWAS (Triase Merah)",
                metricSummary = "Contoh indikasi darurat: Cuaca ekstrem / Gempa kuat / Gelombang >= 4.0 m / Karhutla kritis.",
                safetyAdvice = "Segera lakukan langkah evakuasi aman sesuai SOP BPBD Mukomuko dan hubungi Call Center 117.",
                signatureKey = "test_merah_${System.currentTimeMillis()}"
            )
            DisasterTriageLevel.ORANYE_SIAGA -> DisasterTriageSignal(
                source = TriageFeatureSource.MARITIM_GELOMBANG,
                level = DisasterTriageLevel.ORANYE_SIAGA,
                districtOrLocation = districtName,
                headline = "Simulasi Status SIAGA (Triase Oranye)",
                metricSummary = "Contoh indikasi siaga: Hujan lebat / Gelombang >= 2.5 m / Rob Siaga / AQI Tidak Sehat.",
                safetyAdvice = "Tingkatkan kesiapsiagaan keluarga, tunda aktivitas berisiko, dan pantau rilis Pusdalops BPBD.",
                signatureKey = "test_oranye_${System.currentTimeMillis()}"
            )
            DisasterTriageLevel.KUNING_WASPADA -> DisasterTriageSignal(
                source = TriageFeatureSource.KUALITAS_UDARA,
                level = DisasterTriageLevel.KUNING_WASPADA,
                districtOrLocation = districtName,
                headline = "Simulasi Status WASPADA (Triase Kuning)",
                metricSummary = "Contoh indikasi waspada: Perubahan cuaca signifikan / Gelombang sedang / Hotspot awal.",
                safetyAdvice = "Pantau perkembangan informasi secara berkala pada aplikasi SIPAGAR KITA.",
                signatureKey = "test_kuning_${System.currentTimeMillis()}"
            )
            DisasterTriageLevel.HIJAU_AMAN -> return false
        }
        return evaluateAndNotify(
            context = context,
            signal = sampleSignal,
            forceBypassCooldown = true
        ) == TriageDispatchResult.POSTED
    }

    private fun postTriageNotificationInternal(
        context: Context,
        signal: DisasterTriageSignal
    ): Boolean {
        val channelId = channelIdForLevel(signal.level)
        val notifId = notificationIdForSource(signal.source)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NAVIGATE_TAB, signal.resolvedTargetTab)
            putExtra(EXTRA_TRIAGE_LEVEL, signal.level.code)
            putExtra(EXTRA_TRIAGE_SOURCE, signal.source.key)
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            notifId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "${signal.level.emoji} [${signal.level.badgeLabel}] ${signal.headline}"
        val subtitle = "${signal.source.displayName} • ${signal.districtOrLocation}"
        val detailedText = buildString {
            append("Status Triase: ${signal.level.badgeLabel}\n")
            append("Fitur Sumber: ${signal.source.displayName}\n")
            append("Wilayah / Lokasi: ${signal.districtOrLocation}\n\n")
            append("Indikator Tercatat:\n${signal.metricSummary}\n\n")
            append("Arahan Kesiapsiagaan BPBD Mukomuko:\n${signal.safetyAdvice}")
            if (signal.level.priorityScore >= DisasterTriageLevel.ORANYE_SIAGA.priorityScore) {
                append("\n\nCall Center Darurat BPBD Mukomuko (Bebas Pulsa): 117")
            }
        }

        val priority = when (signal.level) {
            DisasterTriageLevel.MERAH_AWAS -> NotificationCompat.PRIORITY_MAX
            DisasterTriageLevel.ORANYE_SIAGA -> NotificationCompat.PRIORITY_HIGH
            DisasterTriageLevel.KUNING_WASPADA -> NotificationCompat.PRIORITY_DEFAULT
            DisasterTriageLevel.HIJAU_AMAN -> NotificationCompat.PRIORITY_LOW
        }

        val vibration = when (signal.level) {
            DisasterTriageLevel.MERAH_AWAS -> longArrayOf(0, 500, 150, 500, 150, 700)
            DisasterTriageLevel.ORANYE_SIAGA -> longArrayOf(0, 350, 150, 350, 150, 450)
            else -> longArrayOf(0, 220, 140, 220)
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setStyle(NotificationCompat.BigTextStyle().bigText(detailedText))
            .setPriority(priority)
            .setCategory(
                if (signal.level.priorityScore >= DisasterTriageLevel.ORANYE_SIAGA.priorityScore) {
                    NotificationCompat.CATEGORY_ALARM
                } else {
                    NotificationCompat.CATEGORY_STATUS
                }
            )
            .setColor(signal.level.colorInt)
            .setColorized(signal.level.priorityScore >= DisasterTriageLevel.ORANYE_SIAGA.priorityScore)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .addAction(android.R.drawable.ic_menu_view, "Buka Modul", openPendingIntent)
            .setVibrate(vibration)

        if (signal.level.priorityScore >= DisasterTriageLevel.ORANYE_SIAGA.priorityScore) {
            val dial117Intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:117")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val dial117PendingIntent = PendingIntent.getActivity(
                context,
                notifId + 500,
                dial117Intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_menu_call, "Panggil 117", dial117PendingIntent)
        }

        return try {
            NotificationManagerCompat.from(context).notify(notifId, builder.build())
            true
        } catch (_: SecurityException) {
            false
        }
    }

    fun severityToTriageLevel(rawSeverity: String): DisasterTriageLevel {
        val norm = rawSeverity.trim().uppercase(Locale.ROOT)
        return when {
            norm.contains("AWAS") || norm.contains("MERAH") || norm.contains("BAHAYA") || norm.contains("KRITIS") || norm.contains("DARURAT") ->
                DisasterTriageLevel.MERAH_AWAS
            norm.contains("SIAGA") || norm.contains("ORANYE") || norm.contains("TINGGI") ->
                DisasterTriageLevel.ORANYE_SIAGA
            norm.contains("WASPADA") || norm.contains("KUNING") || norm.contains("SEDANG") ->
                DisasterTriageLevel.KUNING_WASPADA
            else -> DisasterTriageLevel.HIJAU_AMAN
        }
    }

    private fun safeSignal(
        source: TriageFeatureSource,
        districtOrLocation: String,
        summary: String
    ): DisasterTriageSignal = DisasterTriageSignal(
        source = source,
        level = DisasterTriageLevel.HIJAU_AMAN,
        districtOrLocation = districtOrLocation,
        headline = "Status Normal / Aman",
        metricSummary = summary,
        safetyAdvice = "Kondisi dalam batas aman. Tetap pantau informasi resmi BPBD Mukomuko.",
        signatureKey = "safe_${source.key}"
    )

    private data class Quad(
        val level: DisasterTriageLevel,
        val headline: String,
        val metric: String,
        val advice: String
    )

    private data class QuadTab(
        val level: DisasterTriageLevel,
        val headline: String,
        val advice: String,
        val targetTab: Int
    )
}
