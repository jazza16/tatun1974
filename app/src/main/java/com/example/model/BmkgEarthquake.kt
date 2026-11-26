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
    val urgencyLevel: EarthquakeUrgency,
    val isFallbackData: Boolean = false
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

    const val MAX_EARTHQUAKE_NOTIFICATION_WINDOW_MS = 2L * 60L * 60_000L // 2 jam sejak waktu kejadian
    const val MIN_EARTHQUAKE_DISPLAY_TIMEOUT_MS = 1L * 60L * 60_000L // 1 jam durasi tampil minimum
    private const val MAX_FUTURE_CLOCK_SKEW_MS = 10L * 60_000L

    private val INDONESIAN_MONTH_TO_NUMBER = mapOf(
        "jan" to "01", "januari" to "01",
        "feb" to "02", "peb" to "02", "februari" to "02", "pebruari" to "02",
        "mar" to "03", "maret" to "03",
        "apr" to "04", "april" to "04",
        "mei" to "05", "may" to "05",
        "jun" to "06", "juni" to "06",
        "jul" to "07", "juli" to "07",
        "agu" to "08", "ags" to "08", "agustus" to "08", "aug" to "08",
        "sep" to "09", "sept" to "09", "september" to "09",
        "okt" to "10", "oktober" to "10", "oct" to "10",
        "nov" to "11", "nop" to "11", "november" to "11", "nopember" to "11",
        "des" to "12", "desember" to "12", "dec" to "12"
    )

    /**
     * Mengonversi stempel waktu BMKG (`DateTime` ISO-8601 atau pasangan `Tanggal` + `Jam` WIB)
     * menjadi epoch milidetik UTC. Mengembalikan `null` jika format waktu tidak dapat dikenali.
     */
    fun parseEventTimestampMillis(
        dateTimeIso: String,
        date: String,
        timeWib: String
    ): Long? {
        val rawIso = dateTimeIso.trim()
        if (rawIso.isNotBlank() && rawIso.contains("T")) {
            val cleanedIso = rawIso
                .replace(Regex("\\.\\d+"), "")
                .let { if (it.endsWith("Z")) it.dropLast(1) + "+00:00" else it }

            val isoFormats = listOf(
                "yyyy-MM-dd'T'HH:mm:ssXXX",
                "yyyy-MM-dd'T'HH:mm:ssZ",
                "yyyy-MM-dd'T'HH:mm:ss"
            )
            for (pattern in isoFormats) {
                try {
                    val sdf = SimpleDateFormat(pattern, Locale.US).apply {
                        isLenient = false
                        if (!pattern.endsWith("XXX") && !pattern.endsWith("Z")) {
                            timeZone = java.util.TimeZone.getTimeZone("GMT+7")
                        }
                    }
                    val targetStr = if (pattern == "yyyy-MM-dd'T'HH:mm:ssZ") {
                        // Convert +07:00 -> +0700 for SimpleDateFormat Z compatibility
                        cleanedIso.replace(Regex("([+-]\\d{2}):(\\d{2})$"), "$1$2")
                    } else {
                        cleanedIso
                    }
                    val parsed = sdf.parse(targetStr)
                    if (parsed != null) return parsed.time
                } catch (_: Exception) {
                }
            }
        }

        // Fallback parsing format Indonesia BMKG: "16 Okt 2026" + "17:42:10 WIB"
        val dateTokens = date.trim().split(Regex("\\s+"))
        val timeToken = timeWib.trim().uppercase(Locale.ROOT)
        if (dateTokens.size >= 3) {
            val day = dateTokens[0].toIntOrNull()
            val monthNum = INDONESIAN_MONTH_TO_NUMBER[dateTokens[1].lowercase(Locale.ROOT)]
            val year = dateTokens[2].toIntOrNull()
            val timeMatch = Regex("(\\d{1,2}):(\\d{2})(?::(\\d{2}))?").find(timeToken)
            if (day != null && monthNum != null && year != null && timeMatch != null) {
                val hh = timeMatch.groupValues[1].padStart(2, '0')
                val mm = timeMatch.groupValues[2].padStart(2, '0')
                val ss = timeMatch.groupValues[3].ifBlank { "00" }.padStart(2, '0')
                val tzId = when {
                    timeToken.contains("WITA") -> "GMT+8"
                    timeToken.contains("WIT") && !timeToken.contains("WITA") -> "GMT+9"
                    else -> "GMT+7"
                }
                val combined = "${year.toString().padStart(4, '0')}-$monthNum-${day.toString().padStart(2, '0')} $hh:$mm:$ss"
                return try {
                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
                        isLenient = false
                        timeZone = java.util.TimeZone.getTimeZone(tzId)
                    }.parse(combined)?.time
                } catch (_: Exception) {
                    null
                }
            }
        }
        return null
    }

    /**
     * Memastikan notifikasi gempa HANYA aktif untuk kejadian gempa nyata yang baru terjadi
     * dalam jendela waktu 1–2 jam sejak waktu kejadian (menolak data fallback & kejadian lama).
     */
    fun isWithinNotificationWindow(
        earthquake: BmkgEarthquake,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        if (earthquake.isFallbackData) return false
        val eventMillis = parseEventTimestampMillis(
            dateTimeIso = earthquake.dateTimeIso,
            date = earthquake.date,
            timeWib = earthquake.timeWib
        ) ?: return false
        val ageMillis = nowMillis - eventMillis
        return ageMillis in -MAX_FUTURE_CLOCK_SKEW_MS..MAX_EARTHQUAKE_NOTIFICATION_WINDOW_MS
    }

    /**
     * Menghitung durasi tampil notifikasi (1–2 jam) sebelum sistem Android menutupnya otomatis.
     */
    fun computeNotificationDisplayTimeoutMs(
        earthquake: BmkgEarthquake,
        nowMillis: Long = System.currentTimeMillis()
    ): Long {
        val eventMillis = parseEventTimestampMillis(
            dateTimeIso = earthquake.dateTimeIso,
            date = earthquake.date,
            timeWib = earthquake.timeWib
        ) ?: return MAX_EARTHQUAKE_NOTIFICATION_WINDOW_MS
        val elapsedMillis = (nowMillis - eventMillis).coerceAtLeast(0L)
        val remainingMillis = MAX_EARTHQUAKE_NOTIFICATION_WINDOW_MS - elapsedMillis
        return remainingMillis.coerceIn(MIN_EARTHQUAKE_DISPLAY_TIMEOUT_MS, MAX_EARTHQUAKE_NOTIFICATION_WINDOW_MS)
    }

    /**
     * Identitas stabil per kejadian gempa BMKG untuk mencegah pengulangan notifikasi
     * pada event yang sama di berbagai refresh/restart aplikasi.
     */
    fun buildEarthquakeEventIdentity(earthquake: BmkgEarthquake): String {
        val eventMs = parseEventTimestampMillis(earthquake.dateTimeIso, earthquake.date, earthquake.timeWib)
        val normalizedCoords = String.format(Locale.US, "%.2f_%.2f", earthquake.latitude, earthquake.longitude)
        return if (eventMs != null) {
            "eq_${eventMs}_$normalizedCoords"
        } else {
            "eq_${earthquake.date.trim()}_${earthquake.timeWib.trim()}_$normalizedCoords"
        }
    }
}
