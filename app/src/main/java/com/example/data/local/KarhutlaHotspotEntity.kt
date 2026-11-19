package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Room Entity for storing synchronized VIIRS (NASA FIRMS) and BMKG Karhutla Hotspots.
 *
 * Design rules enforced:
 * 1. Stores acquisition time strictly as UTC epoch milliseconds (`acqDateTime: Long`), never raw strings,
 *    for fast temporal queries, retention pruning, and cross-source spatio-temporal matching.
 * 2. Uses a deterministic `sourceId` as primary key so `@Insert(onConflict = OnConflictStrategy.IGNORE)`
 *    prevents duplicate records:
 *    - FIRMS: "$satellite|$acqDateTime|$lat|$lon"
 *    - BMKG:  "$acqDateTime|$lat|$lon|$confidenceLabel"
 * 3. Stores only primitive coordinates and telemetry/status fields (never UI or Map Marker objects);
 *    map markers are rendered dynamically only when the map is opened.
 */
@Entity(
    tableName = "karhutla_hotspots",
    indices = [
        Index(value = ["sourceId"], unique = true),
        Index(value = ["acqDateTime"]),
        Index(value = ["sourceType"]),
        Index(value = ["verificationStatus"]),
        Index(value = ["isMatchedWithCrossSource"])
    ]
)
data class KarhutlaHotspotEntity(
    @PrimaryKey
    val sourceId: String,
    val sourceType: String, // SOURCE_FIRMS or SOURCE_BMKG
    val satellite: String,
    val acqDateTime: Long, // Epoch millis UTC
    val lat: Double,
    val lon: Double,
    val confidenceLabel: String, // HIGH, NOMINAL, LOW
    val brightnessKelvin: Double,
    val frpMw: Double,
    val dayNight: String = "D",
    val nearestSubDistrict: String = "Kota Mukomuko",
    val distanceToSubDistrictKm: Double = 0.0,
    val verificationStatus: String = STATUS_FIRMS_ONLY,
    val isMatchedWithCrossSource: Boolean = false,
    val matchedPartnerSourceId: String? = null,
    val matchedDistanceKm: Double? = null,
    val syncedAtUtcMillis: Long = System.currentTimeMillis(),
    val isSimulation: Boolean = false
) {
    val brightnessCelsius: Double
        get() = (brightnessKelvin - 273.15).coerceAtLeast(0.0)

    fun formattedAcqTimeWib(): String {
        val sdf = SimpleDateFormat("dd MMM yyyy • HH:mm 'WIB'", Locale("id", "ID")).apply {
            timeZone = TimeZone.getTimeZone("Asia/Jakarta")
        }
        return sdf.format(Date(acqDateTime))
    }

    fun formattedAcqTimeUtc(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm 'UTC'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return sdf.format(Date(acqDateTime))
    }

    companion object {
        const val SOURCE_FIRMS = "FIRMS"
        const val SOURCE_BMKG = "BMKG"

        const val STATUS_VERIFIED_MATCH = "TERKONFIRMASI_FIRMS_BMKG"
        const val STATUS_FIRMS_ONLY = "PANTAUAN_FIRMS"
        const val STATUS_BMKG_ONLY = "PANTAUAN_BMKG"

        /**
         * Deterministic sourceId for NASA FIRMS:
         * "$satellite|$acqDateTime|$lat|$lon"
         */
        fun buildFirmsSourceId(
            satellite: String,
            acqDateTime: Long,
            lat: Double,
            lon: Double
        ): String {
            val cleanSat = satellite.trim().uppercase(Locale.US)
            val normLat = String.format(Locale.US, "%.5f", lat).toDouble()
            val normLon = String.format(Locale.US, "%.5f", lon).toDouble()
            return "$cleanSat|$acqDateTime|$normLat|$normLon"
        }

        /**
         * Deterministic sourceId for BMKG:
         * "$acqDateTime|$lat|$lon|$confidenceLabel"
         */
        fun buildBmkgSourceId(
            acqDateTime: Long,
            lat: Double,
            lon: Double,
            confidenceLabel: String
        ): String {
            val cleanConf = normalizeConfidenceLabel(confidenceLabel)
            val normLat = String.format(Locale.US, "%.5f", lat).toDouble()
            val normLon = String.format(Locale.US, "%.5f", lon).toDouble()
            return "$acqDateTime|$normLat|$normLon|$cleanConf"
        }

        fun normalizeConfidenceLabel(raw: String): String {
            val trimmed = raw.trim().lowercase(Locale.US)
            val numeric = trimmed.toIntOrNull()
            return when {
                trimmed == "h" || trimmed == "high" || trimmed.contains("tinggi") || (numeric != null && numeric >= 80) || numeric == 9 -> "HIGH"
                trimmed == "l" || trimmed == "low" || trimmed.contains("rendah") || (numeric != null && numeric in 1..39) || numeric == 7 -> "LOW"
                else -> "NOMINAL"
            }
        }

        /**
         * Parses NASA FIRMS / BMKG UTC date ("YYYY-MM-DD") and UTC time ("HHMM" or "HH:MM")
         * into exact UTC epoch milliseconds.
         */
        fun parseUtcEpochMillis(acqDate: String, acqTime: String): Long {
            val cleanDate = acqDate.trim()
            val digitsTime = acqTime.trim().replace(":", "").padStart(4, '0').take(4)
            return try {
                val sdf = SimpleDateFormat("yyyy-MM-dd HHmm", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                    isLenient = false
                }
                sdf.parse("$cleanDate $digitsTime")?.time ?: System.currentTimeMillis()
            } catch (_: Exception) {
                System.currentTimeMillis()
            }
        }
    }
}
