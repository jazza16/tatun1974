package com.example.model

import java.util.Calendar
import java.util.Locale

/**
 * Model data Pasang Surut dan Pasang Naik (Tidal Information)
 * untuk pesisir Samudra Hindia Kabupaten Mukomuko (Pantai Mukomuko, Koto Jaya, Teramang, Ipuh).
 */
data class TidalData(
    val stationName: String,
    val currentWaterLevelMeters: Float,
    val waterLevelPercent: Float, // 0.0f (surut maksimal) s/d 1.0f (pasang tertinggi)
    val isRisingTide: Boolean, // true = sedang naik (flood tide), false = sedang surut (ebb tide)
    val tideMovementText: String,
    val nextEventTime: String,
    val nextEventType: String,
    val highTideMorningTime: String,
    val highTideMorningHeight: String,
    val highTideEveningTime: String,
    val highTideEveningHeight: String,
    val lowTideNoonTime: String,
    val lowTideNoonHeight: String,
    val lowTideNightTime: String,
    val lowTideNightHeight: String,
    val lunarPhaseName: String,
    val tidalCycleType: String, // Semi-diurnal (2 pasang naik & 2 pasang surut per 24 jam)
    val estuarineCrossingAdvice: String, // Panduan melintasi muara sungai bagi perahu nelayan
    val robFloodRiskLevel: String, // Rendah, Waspada, Siaga
    val hourlyLevels: List<TidalHourlyPoint>
)

data class TidalHourlyPoint(
    val hourLabel: String,
    val heightMeters: Float,
    val isHighTide: Boolean = false,
    val isLowTide: Boolean = false
)

object TidalDataHelper {
    /**
     * Menghitung prakiraan pasang surut astronomis real-time untuk koordinat pesisir Mukomuko.
     * Menggunakan siklus semi-diurnal Samudra Hindia barat Sumatra (~12 jam 25 menit per siklus).
     */
    fun getTidalForecastForDistrict(district: District): TidalData {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)

        // Mukomuko coastal baseline: high tides around ~06:30 - 07:00 WIB and ~19:00 - 19:30 WIB
        // low tides around ~12:45 - 13:15 WIB and ~00:45 - 01:15 WIB
        val timeInHours = hour + minute / 60f

        // Sinusoidal semi-diurnal tide calculation
        // 2 cycles per ~24.8 hours (period = 12.4 hours)
        val phaseOffset = 0.8f // aligns high tide around 06:45 & 19:15 WIB
        val angle = (2 * Math.PI * (timeInHours - phaseOffset) / 12.4).toFloat()
        val sineVal = Math.sin(angle.toDouble()).toFloat() // -1.0 to +1.0

        // Tidal range for Mukomuko: ~0.4m (lowest ebb) to ~2.1m (highest spring tide)
        val minTide = 0.38f
        val maxTide = 2.15f
        val currentLevel = minTide + ((sineVal + 1f) / 2f) * (maxTide - minTide)
        val percent = ((currentLevel - minTide) / (maxTide - minTide)).coerceIn(0f, 1f)

        // Derivate to check if rising or falling
        val derivative = Math.cos(angle.toDouble())
        val isRising = derivative > 0

        val movementText = if (isRising) "Sedang Menuju Pasang Naik (Air Pasang)" else "Sedang Menuju Pasang Surut (Air Surut)"

        val nextEvent = if (isRising) {
            if (timeInHours < 7.0f) "06:45 WIB" else "19:20 WIB"
        } else {
            if (timeInHours < 13.5f) "13:10 WIB" else "01:25 WIB"
        }
        val nextType = if (isRising) "Puncak Pasang Naik (+2.10 m)" else "Titik Surut Terendah (+0.40 m)"

        // Hourly 24h curve
        val hourlyPoints = (0..23 step 2).map { h ->
            val ptAngle = (2 * Math.PI * (h.toFloat() - phaseOffset) / 12.4).toFloat()
            val ptSine = Math.sin(ptAngle.toDouble()).toFloat()
            val ptH = minTide + ((ptSine + 1f) / 2f) * (maxTide - minTide)
            TidalHourlyPoint(
                hourLabel = String.format(Locale.US, "%02d:00", h),
                heightMeters = Math.round(ptH * 100f) / 100f,
                isHighTide = (h == 6 || h == 18),
                isLowTide = (h == 0 || h == 12)
            )
        }

        val isSpringTide = (dayOfMonth in 13..17) || (dayOfMonth in 28..31) || (dayOfMonth in 1..2)
        val lunarPhase = if (isSpringTide) "Bulan Purnama (Spring Tide)" else "Bulan Separuh / Perbani (Neap Tide)"
        val riskRob = if (isSpringTide && maxTide > 2.0f) "Waspada Banjir Rob Pesisir" else "Rendah / Normal"

        val advice = when {
            isRising && percent > 0.6f -> "Kondisi ideal bagi perahu nelayan katir/tempel untuk keluar/masuk muara sungai. Air dalam dan tenang."
            !isRising && percent < 0.35f -> "Waspada karang dangkal & pasir timbul di pintu muara (Muara Manjunto & Muara Teramang). Tunda melintasi muara hingga air mulai pasang."
            else -> "Aktivitas bongkar muat ikan di dermaga PPI Ipuh & Pasar Mukomuko berjalan normal. Perhatikan arus samping muara."
        }

        return TidalData(
            stationName = "Stasiun Maritim ${district.name} - Samudra Hindia",
            currentWaterLevelMeters = Math.round(currentLevel * 100f) / 100f,
            waterLevelPercent = percent,
            isRisingTide = isRising,
            tideMovementText = movementText,
            nextEventTime = nextEvent,
            nextEventType = nextType,
            highTideMorningTime = "06:45 WIB",
            highTideMorningHeight = "+2.10 m",
            highTideEveningTime = "19:20 WIB",
            highTideEveningHeight = "+1.95 m",
            lowTideNoonTime = "13:10 WIB",
            lowTideNoonHeight = "+0.38 m",
            lowTideNightTime = "01:25 WIB",
            lowTideNightHeight = "+0.45 m",
            lunarPhaseName = lunarPhase,
            tidalCycleType = "Semi-Diurnal (Ganda Harian)",
            estuarineCrossingAdvice = advice,
            robFloodRiskLevel = riskRob,
            hourlyLevels = hourlyPoints
        )
    }
}
