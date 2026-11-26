package com.example.model

import androidx.annotation.Keep
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Keep
@JsonClass(generateAdapter = true)
data class BmkgAutoGempaResponse(
    @param:Json(name = "Infogempa")
    val infoGempa: BmkgInfoGempaContainer?
)

@Keep
@JsonClass(generateAdapter = true)
data class BmkgRecentGempaResponse(
    @param:Json(name = "Infogempa")
    val infoGempa: BmkgRecentGempaContainer?
)

@Keep
@JsonClass(generateAdapter = true)
data class BmkgInfoGempaContainer(
    @param:Json(name = "gempa")
    val gempa: BmkgGempaRaw?
)

@Keep
@JsonClass(generateAdapter = true)
data class BmkgRecentGempaContainer(
    @param:Json(name = "gempa")
    val gempaList: List<BmkgGempaRaw>?
)

@Keep
@JsonClass(generateAdapter = true)
data class BmkgGempaRaw(
    @param:Json(name = "Tanggal")
    val tanggal: String? = null,
    @param:Json(name = "Jam")
    val jam: String? = null,
    @param:Json(name = "DateTime")
    val dateTime: String? = null,
    @param:Json(name = "Coordinates")
    val coordinates: String? = null,
    @param:Json(name = "Lintang")
    val lintang: String? = null,
    @param:Json(name = "Bujur")
    val bujur: String? = null,
    @param:Json(name = "Magnitude")
    val magnitude: String? = null,
    @param:Json(name = "Kedalaman")
    val kedalaman: String? = null,
    @param:Json(name = "Wilayah")
    val wilayah: String? = null,
    @param:Json(name = "Potensi")
    val potensi: String? = null,
    @param:Json(name = "Dirasakan")
    val dirasakan: String? = null,
    @param:Json(name = "Shakemap")
    val shakemap: String? = null
)

@Keep
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
) {
    /**
     * Bernilai `true` HANYA apabila gempa terjadi di wilayah Provinsi Bengkulu (darat maupun perairan
     * pesisir Bengkulu) ATAU tercatat dirasakan oleh masyarakat di Provinsi Bengkulu (kolom `Dirasakan` BMKG).
     * Gempa global Indonesia lainnya (Papua, Maluku, Sulawesi, Jawa, NTT, Aceh, dll.) yang tidak
     * berdampak/dirasakan di Provinsi Bengkulu akan bernilai `false` sehingga tidak memicu notifikasi.
     */
    val isBengkuluProvinceAffected: Boolean
        get() = BengkuluEarthquakeScope.isBengkuluLocalOrFelt(
            region = region,
            feltDescription = feltDescription,
            latitude = latitude,
            longitude = longitude
        )
}

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

/**
 * Penyaring Wilayah Lokal Provinsi Bengkulu untuk Notifikasi Gempa Bumi SIPAGAR KITA.
 *
 * Memastikan notifikasi Gempa Bumi HANYA dipicu apabila kejadian gempa:
 * 1. Berpusat di salah satu dari 10 Kabupaten/Kota atau perairan pesisir Provinsi Bengkulu, ATAU
 * 2. Tercatat dirasakan oleh masyarakat di wilayah Provinsi Bengkulu (kolom `Dirasakan` / MMI BMKG), ATAU
 * 3. Koordinat episenter berada langsung di dalam kotak geografis Provinsi Bengkulu & pesisirnya
 *    (selama teks wilayah BMKG tidak secara eksplisit menandakan provinsi luar tanpa rasa di Bengkulu).
 */
object BengkuluEarthquakeScope {
    // 10 Kabupaten/Kota, ibu kota kabupaten, pulau, dan kecamatan utama Provinsi Bengkulu
    private val BENGKULU_TERRITORY_KEYWORDS = listOf(
        "bengkulu",
        "mukomuko",
        "muko-muko",
        "muko muko",
        "ipuh",
        "air rami",
        "pondok suguh",
        "teramang jaya",
        "lubuk pinang",
        "penarik",
        "selagan",
        "enggano",
        "arga makmur",
        "argamakmur",
        "ketahun",
        "lais",
        "putri hijau",
        "manna",
        "seluma",
        "tais",
        "kaur",
        "bintuhan",
        "rejang lebong",
        "curup",
        "kepahiang",
        "lebong",
        "muara aman",
        "tubei",
        "karang tinggi"
    )

    // Penanda wilayah luar Provinsi Bengkulu agar episenter perbatasan darat (Sumsel/Jambi/Lampung/Sumbar)
    // tidak lolos hanya karena bounding box bila tidak benar-benar dirasakan di Bengkulu.
    private val OUTSIDE_PROVINCE_MARKERS = listOf(
        "sumsel", "sumatera selatan", "lubuklinggau", "lubuk linggau", "lahat", "pagar alam", "pagaralam", "empat lawang", "musi rawas", "palembang",
        "jambi", "kerinci", "sungai penuh", "merangin", "sarolangun",
        "sumbar", "sumatera barat", "padang", "painan", "pesisir selatan", "mentawai", "solok",
        "lampung", "pesisir barat", "liwa", "tanggamus", "bandar lampung",
        "aceh", "sumut", "sumatera utara", "nias", "riau", "kepri", "babel", "bangka",
        "banten", "jabar", "jawa barat", "jateng", "jawa tengah", "jatim", "jawa timur", "jakarta", "diy", "yogyakarta",
        "bali", "ntb", "ntt", "lombok", "sumbawa", "flores", "kupang", "timor",
        "kalimantan", "sulawesi", "palu", "manado", "gorontalo", "makassar", "kendari", "mamuju", "halmahera",
        "maluku", "ambon", "ternate", "tobelo", "banda", "tanimbar",
        "papua", "jayapura", "sorong", "manokwari", "biak", "nabire", "merauke", "sarmi", "wamena"
    )

    // Bounding box Provinsi Bengkulu & perairan pesisir Samudra Hindia Bengkulu (termasuk Enggano & Mukomuko)
    private const val MIN_LAT_BENGKULU = -5.65
    private const val MAX_LAT_BENGKULU = -2.25
    private const val MIN_LON_BENGKULU = 100.40
    private const val MAX_LON_BENGKULU = 103.65

    fun containsBengkuluKeyword(text: String?): Boolean {
        if (text.isNullOrBlank() || text.trim() == "-") return false
        val normalized = text.lowercase(Locale.ROOT)
        return BENGKULU_TERRITORY_KEYWORDS.any { keyword ->
            normalized.contains(keyword)
        }
    }

    private fun explicitlyMentionsOutsideProvince(region: String?): Boolean {
        if (region.isNullOrBlank()) return false
        val normalized = region.lowercase(Locale.ROOT)
        if (containsBengkuluKeyword(normalized)) return false
        return OUTSIDE_PROVINCE_MARKERS.any { marker -> normalized.contains(marker) }
    }

    fun isInsideBengkuluGeoBox(latitude: Double, longitude: Double): Boolean {
        if (latitude == 0.0 && longitude == 0.0) return false
        return latitude in MIN_LAT_BENGKULU..MAX_LAT_BENGKULU &&
            longitude in MIN_LON_BENGKULU..MAX_LON_BENGKULU
    }

    /**
     * Menentukan apakah suatu kejadian gempa bumi merupakan event lokal Provinsi Bengkulu
     * (terjadi di Provinsi Bengkulu ATAU dirasakan masyarakat di Provinsi Bengkulu).
     */
    fun isBengkuluLocalOrFelt(
        region: String?,
        feltDescription: String?,
        latitude: Double,
        longitude: Double
    ): Boolean {
        // 1. Episenter secara eksplisit berada di wilayah Provinsi Bengkulu (10 Kab/Kota & Enggano)
        if (containsBengkuluKeyword(region)) return true

        // 2. Gempa tercatat dirasakan masyarakat di Provinsi Bengkulu pada laporan MMI BMKG
        if (containsBengkuluKeyword(feltDescription)) return true

        // 3. Episenter di luar nama wilayah Bengkulu tetapi berada di dalam koordinat Provinsi Bengkulu
        //    (selama teks wilayah BMKG tidak secara eksplisit menyatakan provinsi lain).
        if (isInsideBengkuluGeoBox(latitude, longitude) && !explicitlyMentionsOutsideProvince(region)) {
            return true
        }

        return false
    }
}
