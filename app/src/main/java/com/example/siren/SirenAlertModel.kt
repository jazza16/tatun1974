package com.example.siren

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioManager
import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.Locale

/**
 * Jenis bahaya yang memicu Sirene Digital & TTS Bahasa Indonesia.
 */
enum class SirenAlertType(val displayTitle: String, val baseSeverityRank: Int) {
    EXTREME_WEATHER("PERINGATAN CUACA EKSTREM", 100),
    TSUNAMI("PERINGATAN DINI TSUNAMI", 200)
}

/**
 * Sumber asal peringatan dini.
 */
enum class SirenSourceType(val displayLabel: String) {
    BMKG_WRS_NTWC("BMKG Resmi (WRS-NTWC)"),
    LAPORAN_LAPANGAN("Laporan Lapangan / Non-BMKG"),
    PETUGAS_PUSDALOPS("Operator Pusdalops BPBD"),
    SIMULASI_LOKAL("Mode Uji Coba Lokal (SIMULASI)")
}

/**
 * Status verifikasi sumber peringatan secara eksplisit.
 * - Memiliki status terpisah untuk rilis utama gempa/tsunami BMKG (`VERIFIED_BMKG_PRIMARY`),
 *   riwayat resmi gempa/tsunami BMKG (`VERIFIED_BMKG_HISTORY`), serta telemetri cuaca ekstrem
 *   (`VERIFIED_WEATHER_TELEMETRY`).
 * - Kasus jaringan gagal, timeout, atau tidak ditemukan di feed resmi TIDAK pernah
 *   diam-diam diturunkan menjadi non-BMKG biasa, melainkan diberi status "BELUM TERVERIFIKASI" yang jelas.
 */
enum class SirenVerificationStatus(
    val badgeLabel: String,
    val isVerifiedBmkg: Boolean,
    val isUnverifiedWarning: Boolean,
    val verificationRank: Int
) {
    VERIFIED_BMKG_PRIMARY(
        badgeLabel = "TERVERIFIKASI BMKG (RILIS UTAMA)",
        isVerifiedBmkg = true,
        isUnverifiedWarning = false,
        verificationRank = 30
    ),
    VERIFIED_WEATHER_TELEMETRY(
        badgeLabel = "TERVERIFIKASI TELEMETRI CUACA EKSTREM",
        isVerifiedBmkg = true,
        isUnverifiedWarning = false,
        verificationRank = 28
    ),
    VERIFIED_BMKG_HISTORY(
        badgeLabel = "TERVERIFIKASI BMKG (RIWAYAT RESMI - TERGESER GEMPA BARU)",
        isVerifiedBmkg = true,
        isUnverifiedWarning = false,
        verificationRank = 25
    ),
    CONFIRMED_BY_OFFICER(
        badgeLabel = "DIKONFIRMASI PETUGAS PUSDALOPS",
        isVerifiedBmkg = false,
        isUnverifiedWarning = false,
        verificationRank = 20
    ),
    LOCAL_SIMULATION(
        badgeLabel = "SIMULASI LOKAL PETUGAS",
        isVerifiedBmkg = false,
        isUnverifiedWarning = false,
        verificationRank = 15
    ),
    UNVERIFIED_TIMEOUT(
        badgeLabel = "BELUM TERVERIFIKASI (TIMEOUT SERVER BMKG)",
        isVerifiedBmkg = false,
        isUnverifiedWarning = true,
        verificationRank = 5
    ),
    UNVERIFIED_NETWORK_ERROR(
        badgeLabel = "BELUM TERVERIFIKASI (JARINGAN GAGAL / DATA TIDAK VALID)",
        isVerifiedBmkg = false,
        isUnverifiedWarning = true,
        verificationRank = 5
    ),
    UNVERIFIED_NOT_IN_BMKG_FEED(
        badgeLabel = "BELUM TERVERIFIKASI (TIDAK ADA DI FEED RESMI BMKG)",
        isVerifiedBmkg = false,
        isUnverifiedWarning = true,
        verificationRank = 1
    ),
    PENDING_OFFICER_CONFIRMATION(
        badgeLabel = "MENUNGGU KONFIRMASI PETUGAS PUSDALOPS",
        isVerifiedBmkg = false,
        isUnverifiedWarning = false,
        verificationRank = 1
    )
}

/**
 * Kebijakan yang dapat dikonfigurasi (via flag pengaturan/SharedPreferences, bukan hardcode)
 * ketika verifikasi silang ke server BMKG mengalami kegagalan jaringan, timeout, atau format waktu rusak.
 */
enum class VerificationFailurePolicy(
    val code: String,
    val title: String,
    val description: String
) {
    HOLD_FOR_OFFICER_CONFIRMATION(
        code = "HOLD_FOR_OFFICER_CONFIRMATION",
        title = "Tahan Alarm & Tunggu Konfirmasi Petugas (Rekomendasi Aman)",
        description = "Jika server BMKG timeout/offline, tampilkan status 'BELUM TERVERIFIKASI' secara jelas dan tunggu konfirmasi petugas sebelum membunyikan sirene."
    ),
    NOTIFY_WITH_UNVERIFIED_BADGE_NO_SIREN(
        code = "NOTIFY_WITH_UNVERIFIED_BADGE_NO_SIREN",
        title = "Tampilkan Notifikasi 'BELUM TERVERIFIKASI' Tanpa Sirene",
        description = "Tampilkan notifikasi peringatan dengan label tegas 'BELUM TERVERIFIKASI' di layar tanpa memutar suara sirene otomatis."
    ),
    SOUND_SIREN_WITH_UNVERIFIED_BANNER(
        code = "SOUND_SIREN_WITH_UNVERIFIED_BANNER",
        title = "Fail-Safe Darurat: Bunyikan Sirene dengan Label 'BELUM TERVERIFIKASI'",
        description = "Tetap bunyikan sirene dan TTS yang menyertakan pengumuman bahwa verifikasi server BMKG sedang terkendala jaringan."
    );

    companion object {
        fun fromCode(code: String?): VerificationFailurePolicy {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) }
                ?: HOLD_FOR_OFFICER_CONFIRMATION
        }
    }
}

/**
 * Konfigurasi runtime Sirene Digital yang dapat diatur (termasuk ambang cuaca ekstrem,
 * toleransi numerik, serta validasi/clamp agar nilai selalu berada dalam batas aman).
 */
data class SirenRuntimeConfig(
    val cooldownWindowMillis: Long = DEFAULT_COOLDOWN_MILLIS,
    val simulationCooldownMillis: Long = DEFAULT_SIMULATION_COOLDOWN_MILLIS,
    val maxAlertAgeMillis: Long = DEFAULT_MAX_ALERT_AGE_MILLIS,
    val bmkgVerifyTimeoutMillis: Long = DEFAULT_BMKG_VERIFY_TIMEOUT_MILLIS,
    val verificationFailurePolicy: VerificationFailurePolicy = VerificationFailurePolicy.HOLD_FOR_OFFICER_CONFIRMATION,
    val extremeWeatherWmoCodes: Set<Int> = DEFAULT_EXTREME_WMO_CODES,
    val extremePrecipitationMmThreshold: Double = DEFAULT_EXTREME_PRECIPITATION_MM_THRESHOLD,
    val extremeWindKmhThreshold: Double = DEFAULT_EXTREME_WIND_KMH_THRESHOLD,
    val coordinateToleranceDegrees: Double = DEFAULT_COORDINATE_TOLERANCE_DEGREES,
    val magnitudeTolerance: Double = DEFAULT_MAGNITUDE_TOLERANCE
) {
    /**
     * Mengembalikan salinan konfigurasi yang telah di-clamp ke batas aman.
     */
    fun clamped(): SirenRuntimeConfig = SirenRuntimeConfig(
        cooldownWindowMillis = clampCooldownMillis(cooldownWindowMillis),
        simulationCooldownMillis = clampSimulationCooldownMillis(simulationCooldownMillis),
        maxAlertAgeMillis = clampMaxAlertAgeMillis(maxAlertAgeMillis),
        bmkgVerifyTimeoutMillis = clampVerifyTimeoutMillis(bmkgVerifyTimeoutMillis),
        verificationFailurePolicy = verificationFailurePolicy,
        extremeWeatherWmoCodes = sanitizeWmoCodes(extremeWeatherWmoCodes),
        extremePrecipitationMmThreshold = clampPrecipitationMm(extremePrecipitationMmThreshold),
        extremeWindKmhThreshold = clampWindKmh(extremeWindKmhThreshold),
        coordinateToleranceDegrees = clampCoordinateTolerance(coordinateToleranceDegrees),
        magnitudeTolerance = clampMagnitudeTolerance(magnitudeTolerance)
    )

    companion object {
        // Nilai Default
        const val DEFAULT_COOLDOWN_MILLIS = 3 * 60 * 1000L // 3 menit jeda untuk kejadian yang sama
        const val DEFAULT_SIMULATION_COOLDOWN_MILLIS = 5 * 1000L // 5 detik untuk mode SIMULASI lokal
        const val DEFAULT_MAX_ALERT_AGE_MILLIS = 30 * 60 * 1000L // 30 menit batas kedaluwarsa peringatan (TTL)
        const val DEFAULT_BMKG_VERIFY_TIMEOUT_MILLIS = 8 * 1000L // 8 detik batas tunggu verifikasi BMKG
        val DEFAULT_EXTREME_WMO_CODES: Set<Int> = setOf(65, 82, 95, 96, 99)
        const val DEFAULT_EXTREME_PRECIPITATION_MM_THRESHOLD = 15.0
        const val DEFAULT_EXTREME_WIND_KMH_THRESHOLD = 40.0
        const val DEFAULT_COORDINATE_TOLERANCE_DEGREES = 0.05
        const val DEFAULT_MAGNITUDE_TOLERANCE = 0.25

        // Batas Wajar (Clamp Bounds)
        const val MIN_COOLDOWN_MILLIS = 10 * 1000L // 10 detik
        const val MAX_COOLDOWN_MILLIS = 60 * 60 * 1000L // 60 menit

        const val MIN_SIMULATION_COOLDOWN_MILLIS = 1 * 1000L // 1 detik
        const val MAX_SIMULATION_COOLDOWN_MILLIS = 10 * 60 * 1000L // 10 menit

        const val MIN_MAX_ALERT_AGE_MILLIS = 60 * 1000L // 1 menit
        const val MAX_MAX_ALERT_AGE_MILLIS = 6 * 60 * 60 * 1000L // 6 jam

        const val MIN_BMKG_VERIFY_TIMEOUT_MILLIS = 2 * 1000L // 2 detik
        const val MAX_BMKG_VERIFY_TIMEOUT_MILLIS = 30 * 1000L // 30 detik

        const val MIN_EXTREME_PRECIPITATION_MM = 1.0
        const val MAX_EXTREME_PRECIPITATION_MM = 300.0

        const val MIN_EXTREME_WIND_KMH = 15.0
        const val MAX_EXTREME_WIND_KMH = 250.0

        const val MIN_COORDINATE_TOLERANCE_DEGREES = 0.001
        const val MAX_COORDINATE_TOLERANCE_DEGREES = 0.5

        const val MIN_MAGNITUDE_TOLERANCE = 0.01
        const val MAX_MAGNITUDE_TOLERANCE = 1.0

        private const val PREFS_NAME = "sipagar_siren_config_prefs"
        private const val KEY_FAILURE_POLICY = "verification_failure_policy"
        private const val KEY_COOLDOWN_MS = "cooldown_window_millis"
        private const val KEY_SIM_COOLDOWN_MS = "simulation_cooldown_millis"
        private const val KEY_MAX_AGE_MS = "max_alert_age_millis"
        private const val KEY_VERIFY_TIMEOUT_MS = "bmkg_verify_timeout_millis"
        private const val KEY_EXTREME_WMO_CODES = "extreme_wmo_codes"
        private const val KEY_EXTREME_PRECIP_MM = "extreme_precipitation_mm_threshold"
        private const val KEY_EXTREME_WIND_KMH = "extreme_wind_kmh_threshold"
        private const val KEY_COORD_TOLERANCE_DEG = "coordinate_tolerance_degrees"
        private const val KEY_MAG_TOLERANCE = "magnitude_tolerance"

        internal fun clampCooldownMillis(raw: Long): Long =
            raw.coerceIn(MIN_COOLDOWN_MILLIS, MAX_COOLDOWN_MILLIS)

        internal fun clampSimulationCooldownMillis(raw: Long): Long =
            raw.coerceIn(MIN_SIMULATION_COOLDOWN_MILLIS, MAX_SIMULATION_COOLDOWN_MILLIS)

        internal fun clampMaxAlertAgeMillis(raw: Long): Long =
            raw.coerceIn(MIN_MAX_ALERT_AGE_MILLIS, MAX_MAX_ALERT_AGE_MILLIS)

        internal fun clampVerifyTimeoutMillis(raw: Long): Long =
            raw.coerceIn(MIN_BMKG_VERIFY_TIMEOUT_MILLIS, MAX_BMKG_VERIFY_TIMEOUT_MILLIS)

        internal fun clampPrecipitationMm(raw: Double): Double =
            if (raw.isFinite()) raw.coerceIn(MIN_EXTREME_PRECIPITATION_MM, MAX_EXTREME_PRECIPITATION_MM)
            else DEFAULT_EXTREME_PRECIPITATION_MM_THRESHOLD

        internal fun clampWindKmh(raw: Double): Double =
            if (raw.isFinite()) raw.coerceIn(MIN_EXTREME_WIND_KMH, MAX_EXTREME_WIND_KMH)
            else DEFAULT_EXTREME_WIND_KMH_THRESHOLD

        internal fun clampCoordinateTolerance(raw: Double): Double =
            if (raw.isFinite()) raw.coerceIn(MIN_COORDINATE_TOLERANCE_DEGREES, MAX_COORDINATE_TOLERANCE_DEGREES)
            else DEFAULT_COORDINATE_TOLERANCE_DEGREES

        internal fun clampMagnitudeTolerance(raw: Double): Double =
            if (raw.isFinite()) raw.coerceIn(MIN_MAGNITUDE_TOLERANCE, MAX_MAGNITUDE_TOLERANCE)
            else DEFAULT_MAGNITUDE_TOLERANCE

        internal fun sanitizeWmoCodes(rawCodes: Set<Int>?): Set<Int> {
            val valid = rawCodes?.filter { it in 0..99 }?.toSet().orEmpty()
            return valid.ifEmpty { DEFAULT_EXTREME_WMO_CODES }
        }

        fun load(context: Context): SirenRuntimeConfig {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return loadFromPreferences(prefs)
        }

        internal fun loadFromPreferences(prefs: SharedPreferences): SirenRuntimeConfig {
            val policyCode = prefs.getString(
                KEY_FAILURE_POLICY,
                VerificationFailurePolicy.HOLD_FOR_OFFICER_CONFIRMATION.code
            )
            val cooldown = clampCooldownMillis(
                prefs.getLong(KEY_COOLDOWN_MS, DEFAULT_COOLDOWN_MILLIS)
            )
            val simCooldown = clampSimulationCooldownMillis(
                prefs.getLong(KEY_SIM_COOLDOWN_MS, DEFAULT_SIMULATION_COOLDOWN_MILLIS)
            )
            val maxAge = clampMaxAlertAgeMillis(
                prefs.getLong(KEY_MAX_AGE_MS, DEFAULT_MAX_ALERT_AGE_MILLIS)
            )
            val verifyTimeout = clampVerifyTimeoutMillis(
                prefs.getLong(KEY_VERIFY_TIMEOUT_MS, DEFAULT_BMKG_VERIFY_TIMEOUT_MILLIS)
            )

            val savedWmoCsv = prefs.getString(KEY_EXTREME_WMO_CODES, null)
            val parsedWmoCodes = if (!savedWmoCsv.isNullOrBlank()) {
                savedWmoCsv.split(",")
                    .mapNotNull { it.trim().toIntOrNull() }
                    .toSet()
            } else {
                null
            }
            val wmoCodes = sanitizeWmoCodes(parsedWmoCodes)

            val precipThreshold = clampPrecipitationMm(
                prefs.getFloat(
                    KEY_EXTREME_PRECIP_MM,
                    DEFAULT_EXTREME_PRECIPITATION_MM_THRESHOLD.toFloat()
                ).toDouble()
            )
            val windThreshold = clampWindKmh(
                prefs.getFloat(
                    KEY_EXTREME_WIND_KMH,
                    DEFAULT_EXTREME_WIND_KMH_THRESHOLD.toFloat()
                ).toDouble()
            )
            val coordTolerance = clampCoordinateTolerance(
                prefs.getFloat(
                    KEY_COORD_TOLERANCE_DEG,
                    DEFAULT_COORDINATE_TOLERANCE_DEGREES.toFloat()
                ).toDouble()
            )
            val magTolerance = clampMagnitudeTolerance(
                prefs.getFloat(
                    KEY_MAG_TOLERANCE,
                    DEFAULT_MAGNITUDE_TOLERANCE.toFloat()
                ).toDouble()
            )

            return SirenRuntimeConfig(
                cooldownWindowMillis = cooldown,
                simulationCooldownMillis = simCooldown,
                maxAlertAgeMillis = maxAge,
                bmkgVerifyTimeoutMillis = verifyTimeout,
                verificationFailurePolicy = VerificationFailurePolicy.fromCode(policyCode),
                extremeWeatherWmoCodes = wmoCodes,
                extremePrecipitationMmThreshold = precipThreshold,
                extremeWindKmhThreshold = windThreshold,
                coordinateToleranceDegrees = coordTolerance,
                magnitudeTolerance = magTolerance
            )
        }

        fun saveVerificationFailurePolicy(context: Context, policy: VerificationFailurePolicy) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_FAILURE_POLICY, policy.code)
                .apply()
        }

        /**
         * Menyimpan parameter runtime ke SharedPreferences secara selektif:
         * - Hanya menulis parameter yang TIDAK null (tidak menimpa nilai lama yang tidak diisi).
         * - Ikut menyimpan `simulationCooldownMillis` (dan `verificationFailurePolicy` bila diberikan).
         * - Seluruh nilai di-clamp terlebih dahulu ke batas aman sebelum disimpan.
         */
        fun saveRuntimeParameters(
            context: Context,
            cooldownWindowMillis: Long? = null,
            simulationCooldownMillis: Long? = null,
            maxAlertAgeMillis: Long? = null,
            bmkgVerifyTimeoutMillis: Long? = null,
            verificationFailurePolicy: VerificationFailurePolicy? = null,
            extremeWeatherWmoCodes: Set<Int>? = null,
            extremePrecipitationMmThreshold: Double? = null,
            extremeWindKmhThreshold: Double? = null,
            coordinateToleranceDegrees: Double? = null,
            magnitudeTolerance: Double? = null
        ) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            saveRuntimeParametersToPreferences(
                prefs = prefs,
                cooldownWindowMillis = cooldownWindowMillis,
                simulationCooldownMillis = simulationCooldownMillis,
                maxAlertAgeMillis = maxAlertAgeMillis,
                bmkgVerifyTimeoutMillis = bmkgVerifyTimeoutMillis,
                verificationFailurePolicy = verificationFailurePolicy,
                extremeWeatherWmoCodes = extremeWeatherWmoCodes,
                extremePrecipitationMmThreshold = extremePrecipitationMmThreshold,
                extremeWindKmhThreshold = extremeWindKmhThreshold,
                coordinateToleranceDegrees = coordinateToleranceDegrees,
                magnitudeTolerance = magnitudeTolerance
            )
        }

        internal fun saveRuntimeParametersToPreferences(
            prefs: SharedPreferences,
            cooldownWindowMillis: Long? = null,
            simulationCooldownMillis: Long? = null,
            maxAlertAgeMillis: Long? = null,
            bmkgVerifyTimeoutMillis: Long? = null,
            verificationFailurePolicy: VerificationFailurePolicy? = null,
            extremeWeatherWmoCodes: Set<Int>? = null,
            extremePrecipitationMmThreshold: Double? = null,
            extremeWindKmhThreshold: Double? = null,
            coordinateToleranceDegrees: Double? = null,
            magnitudeTolerance: Double? = null
        ) {
            val editor = prefs.edit()
            if (cooldownWindowMillis != null) {
                editor.putLong(KEY_COOLDOWN_MS, clampCooldownMillis(cooldownWindowMillis))
            }
            if (simulationCooldownMillis != null) {
                editor.putLong(KEY_SIM_COOLDOWN_MS, clampSimulationCooldownMillis(simulationCooldownMillis))
            }
            if (maxAlertAgeMillis != null) {
                editor.putLong(KEY_MAX_AGE_MS, clampMaxAlertAgeMillis(maxAlertAgeMillis))
            }
            if (bmkgVerifyTimeoutMillis != null) {
                editor.putLong(KEY_VERIFY_TIMEOUT_MS, clampVerifyTimeoutMillis(bmkgVerifyTimeoutMillis))
            }
            if (verificationFailurePolicy != null) {
                editor.putString(KEY_FAILURE_POLICY, verificationFailurePolicy.code)
            }
            if (extremeWeatherWmoCodes != null) {
                val sanitized = sanitizeWmoCodes(extremeWeatherWmoCodes)
                editor.putString(KEY_EXTREME_WMO_CODES, sanitized.joinToString(","))
            }
            if (extremePrecipitationMmThreshold != null) {
                editor.putFloat(KEY_EXTREME_PRECIP_MM, clampPrecipitationMm(extremePrecipitationMmThreshold).toFloat())
            }
            if (extremeWindKmhThreshold != null) {
                editor.putFloat(KEY_EXTREME_WIND_KMH, clampWindKmh(extremeWindKmhThreshold).toFloat())
            }
            if (coordinateToleranceDegrees != null) {
                editor.putFloat(KEY_COORD_TOLERANCE_DEG, clampCoordinateTolerance(coordinateToleranceDegrees).toFloat())
            }
            if (magnitudeTolerance != null) {
                editor.putFloat(KEY_MAG_TOLERANCE, clampMagnitudeTolerance(magnitudeTolerance).toFloat())
            }
            editor.apply()
        }
    }
}

/**
 * Payload terstruktur untuk satu kejadian peringatan Sirene Digital + TTS Bahasa Indonesia.
 */
data class SirenAlertPayload(
    val alertId: String,
    val alertType: SirenAlertType,
    val title: String,
    val spokenMessageId: String,
    val affectedDistrict: String,
    val claimedSource: SirenSourceType,
    val verificationStatus: SirenVerificationStatus,
    val verificationDetailMessage: String = "",
    val confirmedByOfficer: Boolean = false,
    val confirmedByOfficerName: String = "",
    val simulationMode: Boolean = false,
    val eventTimestampMillis: Long = System.currentTimeMillis(),
    val expiresAtMillis: Long = eventTimestampMillis + SirenRuntimeConfig.DEFAULT_MAX_ALERT_AGE_MILLIS,
    val magnitude: Double? = null,
    val depthKm: String? = null,
    val coordinates: String? = null,
    val weatherCode: Int? = null,
    val windSpeedKmh: Double? = null,
    val precipitationMm: Double? = null
) {
    /**
     * Memeriksa apakah peringatan sudah melewati batas waktu kedaluwarsa (expired).
     */
    fun isExpired(
        nowMillis: Long = System.currentTimeMillis(),
        maxAgeMillis: Long = SirenRuntimeConfig.DEFAULT_MAX_ALERT_AGE_MILLIS
    ): Boolean {
        val effectiveExpiresAt = minOf(expiresAtMillis, eventTimestampMillis + maxAgeMillis)
        return nowMillis > effectiveExpiresAt
    }

    /**
     * Kunci insiden untuk membedakan antara kejadian yang sama vs kejadian berbeda:
     * - Merapikan spasi berlebih pada koordinat maupun nama wilayah/kecamatan.
     * - Membulatkan koordinat lintang & bujur ke 1 angka di belakang koma (menggunakan Locale.US)
     *   agar revisi kecil koordinat dari BMKG (misal "-2.58, 101.11" menjadi "-2.61, 101.13" -> "-2.6,101.1")
     *   tidak dianggap sebagai insiden baru yang membunyikan sirene dua kali.
     * - Memisahkan prefix "SIM_" dan "REAL_" agar alarm SIMULASI tidak pernah menghalangi alarm REAL.
     */
    fun incidentKey(): String {
        val locationPart = normalizeIncidentLocation(coordinates, affectedDistrict)
        return if (simulationMode) {
            "SIM_${alertType.name}_$locationPart"
        } else {
            "REAL_${alertType.name}_$locationPart"
        }
    }

    /**
     * Mengecek apakah telah melampaui masing-masing dimensi ambang cuaca ekstrem berdasarkan konfigurasi runtime.
     */
    fun exceedsExtremeWmoThreshold(config: SirenRuntimeConfig = SirenRuntimeConfig()): Boolean {
        val code = weatherCode ?: return false
        return code in config.clamped().extremeWeatherWmoCodes
    }

    fun exceedsExtremePrecipitationThreshold(config: SirenRuntimeConfig = SirenRuntimeConfig()): Boolean {
        val precip = precipitationMm ?: return false
        return precip >= config.clamped().extremePrecipitationMmThreshold
    }

    fun exceedsExtremeWindThreshold(config: SirenRuntimeConfig = SirenRuntimeConfig()): Boolean {
        val wind = windSpeedKmh ?: return false
        return wind >= config.clamped().extremeWindKmhThreshold
    }

    fun extremeWeatherThresholdBitmask(config: SirenRuntimeConfig = SirenRuntimeConfig()): Int {
        var mask = 0
        if (exceedsExtremeWmoThreshold(config)) mask = mask or 1
        if (exceedsExtremePrecipitationThreshold(config)) mask = mask or 2
        if (exceedsExtremeWindThreshold(config)) mask = mask or 4
        return mask
    }

    /**
     * Menentukan apakah pembaruan peringatan ini naik tingkat secara bermakna dibandingkan
     * rekaman peringatan sebelumnya:
     * 1. Status verifikasi naik (`verificationStatus.verificationRank > prevVerificationRank`),
     *    misalnya dari `PENDING_OFFICER_CONFIRMATION` (1) atau `UNVERIFIED_*` (1..5) menjadi `CONFIRMED_BY_OFFICER` (20) atau `VERIFIED_BMKG_*` (25..30).
     * 2. Tipe bahaya naik (`alertType.baseSeverityRank > prevTypeRank`), misalnya dari `EXTREME_WEATHER` (100) menjadi `TSUNAMI` (200).
     * 3. Magnitudo gempa naik minimal `0.5` (`incomingMag - prevMaxMag >= 0.5 - 1e-9`).
     * 4. Ambang cuaca ekstrem baru terlampaui (bitmask dimensi WMO / curah hujan / angin yang melampaui ambang memiliki bit baru yang sebelumnya belum terlampaui).
     *
     * Kenaikan angka kecil (misal angin 41 km/jam menjadi 42 km/jam yang sama-sama sudah melampaui ambang 40 km/jam,
     * atau kenaikan magnitudo < 0.5) TIDAK dianggap sebagai pembaruan yang lebih berat.
     */
    fun isMeaningfullyMoreSevereThan(
        prevTypeRank: Int,
        prevVerificationRank: Int,
        prevMaxMagnitude: Double?,
        prevWeatherThresholdMask: Int,
        config: SirenRuntimeConfig = SirenRuntimeConfig()
    ): Boolean {
        if (alertType.baseSeverityRank > prevTypeRank) return true
        if (verificationStatus.verificationRank > prevVerificationRank) return true

        val currentMag = magnitude
        if (currentMag != null) {
            if (prevMaxMagnitude == null || (currentMag - prevMaxMagnitude) >= (MIN_MEANINGFUL_MAGNITUDE_INCREASE - 1e-9)) {
                return true
            }
        }

        val currentMask = extremeWeatherThresholdBitmask(config)
        val newlyExceededBits = currentMask and prevWeatherThresholdMask.inv()
        if (newlyExceededBits != 0) return true

        return false
    }

    /**
     * Skor tingkat keparahan terkuantisasi (berdasarkan tangga kenaikan bermakna, bukan setiap kenaikan angka kecil):
     * - Tipe bahaya (`baseSeverityRank`)
     * - Status verifikasi (`verificationRank`)
     * - Pita magnitudo per kelipatan 0.5 (`floor(magnitude / 0.5)`)
     * - Bitmask ambang cuaca ekstrem yang terlampaui (WMO, hujan ekstrem, angin ekstrem)
     */
    fun severityScore(config: SirenRuntimeConfig = SirenRuntimeConfig()): Int {
        val typeScore = alertType.baseSeverityRank * 1000
        val verifScore = verificationStatus.verificationRank * 10
        val magBandScore = if (magnitude != null) {
            kotlin.math.floor((magnitude + 1e-9) / MIN_MEANINGFUL_MAGNITUDE_INCREASE).toInt() * 50
        } else {
            0
        }
        val weatherThresholdScore = extremeWeatherThresholdBitmask(config)
        return typeScore + magBandScore + verifScore + weatherThresholdScore
    }

    /**
     * Menghasilkan naskah suara TTS Bahasa Indonesia (id-ID) dan teks layar untuk tunarungu.
     */
    fun buildAccessibleTranscript(): String {
        val prefix = when {
            simulationMode -> "[MODE SIMULASI UJI COBA PETUGAS - BUKAN KEJADIAN NYATA]. "
            verificationStatus.isUnverifiedWarning -> "[STATUS: ${verificationStatus.badgeLabel}]. "
            else -> ""
        }
        return "$prefix$spokenMessageId".trim()
    }

    companion object {
        const val MIN_MEANINGFUL_MAGNITUDE_INCREASE = 0.5

        /**
         * Merapikan spasi dan membulatkan koordinat lintang,bujur ke 1 angka desimal (misal "-2.58, 101.11" -> "-2.6,101.1").
         * Jika koordinat kosong atau tidak valid, merapikan spasi berlebih pada nama kecamatan/wilayah.
         */
        fun normalizeIncidentLocation(rawCoordinates: String?, rawDistrict: String): String {
            val cleanedCoords = rawCoordinates?.trim().orEmpty()
            if (cleanedCoords.isNotEmpty()) {
                val parts = cleanedCoords.split(",")
                if (parts.size == 2) {
                    val lat = parts[0].trim().toDoubleOrNull()
                    val lon = parts[1].trim().toDoubleOrNull()
                    if (lat != null && lon != null && lat.isFinite() && lon.isFinite()) {
                        val roundedLat = kotlin.math.round(lat * 10.0) / 10.0
                        val roundedLon = kotlin.math.round(lon * 10.0) / 10.0
                        val normLat = if (roundedLat == 0.0) 0.0 else roundedLat
                        val normLon = if (roundedLon == 0.0) 0.0 else roundedLon
                        return String.format(Locale.US, "%.1f,%.1f", normLat, normLon)
                    }
                }
                return cleanedCoords.replace(Regex("\\s+"), "").lowercase(Locale.ROOT)
            }
            return rawDistrict.trim()
                .replace(Regex("\\s+"), " ")
                .lowercase(Locale.ROOT)
        }
    }
}

/**
 * Parser Zero-Trust untuk payload data FCM yang masuk.
 * Mengabaikan sepenuhnya field `verificationStatus`, `confirmedByOfficer`, `confirmed`,
 * `isVerifiedBmkg`, dan `simulationMode` dari payload eksternal agar tidak dapat di-spoof.
 */
object SirenFcmPayloadParser {
    fun parseUntrustedFcmData(
        data: Map<String, String>,
        receivedAtMillis: Long = System.currentTimeMillis(),
        config: SirenRuntimeConfig = SirenRuntimeConfig()
    ): SirenAlertPayload {
        val rawAlertId = data["alertId"]?.trim()?.takeIf { it.isNotEmpty() }
            ?: data["eventId"]?.trim()?.takeIf { it.isNotEmpty() }
            ?: "FCM_${receivedAtMillis}"

        val rawType = data["alertType"]?.trim()?.uppercase(Locale.ROOT)
        val alertType = if (rawType == SirenAlertType.EXTREME_WEATHER.name) {
            SirenAlertType.EXTREME_WEATHER
        } else {
            SirenAlertType.TSUNAMI
        }

        val rawSource = data["claimedSource"] ?: data["source"] ?: ""
        val claimedSource = if (rawSource.contains("BMKG", ignoreCase = true)) {
            SirenSourceType.BMKG_WRS_NTWC
        } else {
            SirenSourceType.LAPORAN_LAPANGAN
        }

        val district = data["affectedDistrict"]?.trim()?.takeIf { it.isNotEmpty() }
            ?: data["district"]?.trim()?.takeIf { it.isNotEmpty() }
            ?: "Kabupaten Mukomuko"

        val defaultTitle = when (alertType) {
            SirenAlertType.TSUNAMI -> "PERINGATAN DINI TSUNAMI - $district"
            SirenAlertType.EXTREME_WEATHER -> "PERINGATAN CUACA EKSTREM - $district"
        }
        val title = data["title"]?.trim()?.takeIf { it.isNotEmpty() } ?: defaultTitle

        val spoken = data["spokenMessageId"]?.trim()?.takeIf { it.isNotEmpty() }
            ?: data["message"]?.trim()?.takeIf { it.isNotEmpty() }
            ?: "$defaultTitle. Harap waspada dan ikuti arahan evakuasi BPBD Kabupaten Mukomuko."

        return SirenAlertPayload(
            alertId = rawAlertId,
            alertType = alertType,
            title = title,
            spokenMessageId = spoken,
            affectedDistrict = district,
            claimedSource = claimedSource,
            verificationStatus = SirenVerificationStatus.PENDING_OFFICER_CONFIRMATION,
            verificationDetailMessage = "Menunggu verifikasi sumber independen.",
            confirmedByOfficer = false,
            confirmedByOfficerName = "",
            simulationMode = false,
            eventTimestampMillis = receivedAtMillis,
            expiresAtMillis = receivedAtMillis + config.maxAlertAgeMillis,
            magnitude = data["magnitude"]?.toDoubleOrNull(),
            depthKm = data["depthKm"] ?: data["depth"],
            coordinates = data["coordinates"]?.trim(),
            weatherCode = data["weatherCode"]?.toIntOrNull(),
            windSpeedKmh = data["windSpeedKmh"]?.toDoubleOrNull(),
            precipitationMm = data["precipitationMm"]?.toDoubleOrNull()
        )
    }
}

/**
 * Status volume STREAM_ALARM pada perangkat untuk memperingatkan pengguna/petugas jika volume alarm = 0.
 */
data class AlarmVolumeState(
    val currentVolume: Int,
    val maxVolume: Int
) {
    val isVolumeZero: Boolean
        get() = currentVolume <= 0

    val volumePercentage: Int
        get() = if (maxVolume > 0) ((currentVolume * 100) / maxVolume).coerceIn(0, 100) else 0

    companion object {
        fun inspect(context: Context): AlarmVolumeState {
            return try {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                val current = audioManager?.getStreamVolume(AudioManager.STREAM_ALARM) ?: 0
                val max = audioManager?.getStreamMaxVolume(AudioManager.STREAM_ALARM) ?: 7
                AlarmVolumeState(currentVolume = current, maxVolume = max)
            } catch (_: Exception) {
                AlarmVolumeState(currentVolume = 0, maxVolume = 7)
            }
        }
    }
}

/**
 * Model dokumen Firestore untuk antrean konfirmasi peringatan non-BMKG (/siren_alerts/{alertId}).
 */
@Keep
@IgnoreExtraProperties
data class SirenFirestoreAlertDoc(
    @DocumentId val id: String = "",
    val alertId: String = "",
    val authorUid: String = "",
    val alertType: String = SirenAlertType.TSUNAMI.name,
    val title: String = "",
    val spokenMessageId: String = "",
    val affectedDistrict: String = "Kota Mukomuko",
    val sourceType: String = SirenSourceType.LAPORAN_LAPANGAN.name,
    val verificationStatus: String = SirenVerificationStatus.PENDING_OFFICER_CONFIRMATION.name,
    val verificationDetail: String = "",
    val confirmed: Boolean = false,
    val confirmedByName: String = "",
    val confirmedByNip: String = "",
    val eventTimestampMillis: Long = 0L,
    val expiresAtMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

/**
 * Model dokumen Firestore untuk pencatatan audit log sirene (/siren_audit_logs/{logId}).
 */
@Keep
@IgnoreExtraProperties
data class SirenAuditLogDoc(
    @DocumentId val id: String = "",
    val alertId: String = "",
    val logId: String = "",
    val ownerUid: String = "",
    val actionType: String = "",
    val alertType: String = "",
    val verificationStatus: String = "",
    val simulationMode: Boolean = false,
    val officerName: String = "",
    val officerNip: String = "",
    val officerRole: String = "",
    val stationId: String = "",
    val notes: String = "",
    val timestampMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)
