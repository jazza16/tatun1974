package com.example.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@JsonClass(generateAdapter = true)
data class BmkgAutoGempaResponse(
    @Json(name = "Infogempa")
    val infoGempa: BmkgInfoGempaContainer?
)

@JsonClass(generateAdapter = true)
data class BmkgRecentGempaResponse(
    @Json(name = "Infogempa")
    val infoGempa: BmkgRecentGempaContainer?
)

@JsonClass(generateAdapter = true)
data class BmkgInfoGempaContainer(
    @Json(name = "gempa")
    val gempa: BmkgGempaRaw?
)

@JsonClass(generateAdapter = true)
data class BmkgRecentGempaContainer(
    @Json(name = "gempa")
    val gempaList: List<BmkgGempaRaw>?
)

@JsonClass(generateAdapter = true)
data class BmkgGempaRaw(
    @Json(name = "Tanggal")
    val tanggal: String? = null,
    @Json(name = "Jam")
    val jam: String? = null,
    @Json(name = "DateTime")
    val dateTime: String? = null,
    @Json(name = "Coordinates")
    val coordinates: String? = null,
    @Json(name = "Lintang")
    val lintang: String? = null,
    @Json(name = "Bujur")
    val bujur: String? = null,
    @Json(name = "Magnitude")
    val magnitude: String? = null,
    @Json(name = "Kedalaman")
    val kedalaman: String? = null,
    @Json(name = "Wilayah")
    val wilayah: String? = null,
    @Json(name = "Potensi")
    val potensi: String? = null,
    @Json(name = "Dirasakan")
    val dirasakan: String? = null,
    @Json(name = "Shakemap")
    val shakemap: String? = null
)

data class BmkgEarthquake(
    val id: String,
    val date: String,
    val timeWib: String,
    val dateTimeIso: String,
    val latitude: Double,
    val longitude: Double,
    val magnitude: Double,
    val depth: String,
    val region: String,
    val potentialTsunami: String,
    val feltDescription: String,
    val shakemapUrl: String?,
    val distanceKmToMukomuko: Int,
    val isNearMukomuko: Boolean,
    val urgencyLevel: EarthquakeUrgency
)

enum class EarthquakeUrgency {
    CRITICAL, // >= 6.0 or close & > 5.0
    WARNING,  // >= 5.0
    ADVISORY  // < 5.0 or felt
}

object BmkgEarthquakeMapper {
    // Reference coordinates for Kota Mukomuko center
    private const val MUKOMUKO_LAT = -2.5833
    private const val MUKOMUKO_LON = 101.1167

    fun fromRaw(raw: BmkgGempaRaw): BmkgEarthquake {
        val date = raw.tanggal ?: "Hari Ini"
        val timeWib = raw.jam ?: "Terkini"
        val dateTimeIso = raw.dateTime ?: "${date}_${timeWib}"
        val mag = raw.magnitude?.toDoubleOrNull() ?: 5.0
        val depth = raw.kedalaman ?: "10 km"
        val region = raw.wilayah ?: "Wilayah Indonesia"
        val potential = raw.potensi ?: "Tidak berpotensi tsunami"
        val felt = raw.dirasakan ?: "-"

        var lat = 0.0
        var lon = 0.0
        val coords = raw.coordinates?.split(",")
        if (coords != null && coords.size == 2) {
            lat = coords[0].trim().toDoubleOrNull() ?: 0.0
            lon = coords[1].trim().toDoubleOrNull() ?: 0.0
        }

        val distanceKm = calculateDistanceKm(lat, lon, MUKOMUKO_LAT, MUKOMUKO_LON)
        val isNear = region.contains("mukomuko", ignoreCase = true) ||
                region.contains("bengkulu", ignoreCase = true) ||
                region.contains("pesisir barat", ignoreCase = true) ||
                distanceKm <= 250

        val urgency = when {
            potential.contains("berpotensi tsunami", ignoreCase = true) && !potential.contains("tidak", ignoreCase = true) -> EarthquakeUrgency.CRITICAL
            mag >= 6.0 -> EarthquakeUrgency.CRITICAL
            isNear && mag >= 5.0 -> EarthquakeUrgency.CRITICAL
            mag >= 5.0 -> EarthquakeUrgency.WARNING
            else -> EarthquakeUrgency.ADVISORY
        }

        val shakemapUrl = if (!raw.shakemap.isNullOrBlank()) {
            "https://data.bmkg.go.id/DataMKG/TEWS/${raw.shakemap.trim()}"
        } else null

        val id = "${dateTimeIso}_${raw.coordinates ?: region}"

        return BmkgEarthquake(
            id = id,
            date = date,
            timeWib = timeWib,
            dateTimeIso = dateTimeIso,
            latitude = lat,
            longitude = lon,
            magnitude = mag,
            depth = depth,
            region = region,
            potentialTsunami = potential,
            feltDescription = felt,
            shakemapUrl = shakemapUrl,
            distanceKmToMukomuko = distanceKm,
            isNearMukomuko = isNear,
            urgencyLevel = urgency
        )
    }

    private fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Int {
        if (lat1 == 0.0 && lon1 == 0.0) return 999
        val r = 6371.0 // Earth radius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return (r * c).toInt()
    }
}
