package com.example.util

import java.util.Locale

/**
 * Validator Geofence Cakupan Wilayah Administrasi Kabupaten Mukomuko (Anti-Spam Pelaporan Warga).
 *
 * Berdasarkan batas astronomis resmi BPS & Kemendagri Kabupaten Mukomuko:
 * - Lintang Selatan (LS): 02°16'32" LS (-2.2756) s.d. 03°07'46" LS (-3.1294)
 * - Bujur Timur (BT): 101°01'15" BT (101.0209) s.d. 101°51'29" BT (101.8582)
 * - Utara: Kab. Pesisir Selatan (Sumbar)
 * - Selatan: Kab. Bengkulu Utara (Putri Hijau)
 * - Timur: Kab. Kerinci & Kab. Merangin (Jambi)
 * - Barat: Samudra Hindia
 *
 * Menggunakan poligon batas administrasi diagonal + toleransi GPS perbatasan tipis (~2-4 km)
 * agar seluruh warga di 15 kecamatan (termasuk desa perbatasan & pesisir nelayan) tetap dapat
 * melapor, sementara perangkat di luar Kabupaten Mukomuko ditolak otomatis untuk mencegah spam/hoaks.
 */
object MukomukoGeofenceValidator {

    const val MIN_LAT = -3.1450
    const val MAX_LAT = -2.2700
    const val MIN_LON = 101.0150
    const val MAX_LON = 101.8700

    const val DEFAULT_REJECTION_TOAST =
        "Pengiriman laporan ditolak: Lokasi perangkat Anda berada di luar Wilayah Administrasi Kabupaten Mukomuko. Pelaporan warga hanya berlaku di dalam cakupan wilayah Kabupaten Mukomuko."

    // Poligon searah jarum jam (lat, lon) mengikuti bentuk diagonal wilayah administrasi Kab. Mukomuko
    private val MUKOMUKO_ADMIN_POLYGON: List<Pair<Double, Double>> = listOf(
        -2.2700 to 101.0150, // Perbatasan Utara-Barat (Lubuk Pinang / Silaut Pesisir Selatan)
        -2.2700 to 101.4200, // Perbatasan Utara-Timur (V Koto / Hulu Manjuto - Pesisir Selatan)
        -2.5800 to 101.6200, // Batas Timur Tengah (Selagan Raya / Penarik - TNKS Kerinci)
        -2.8500 to 101.8700, // Batas Timur Selatan (Malin Deman - Merangin Jambi)
        -3.1450 to 101.8700, // Perbatasan Selatan-Timur (Malin Deman / Air Rami - Bengkulu Utara)
        -3.1450 to 101.4900, // Perbatasan Selatan-Barat (Air Rami - Putri Hijau Bengkulu Utara)
        -3.0300 to 101.4200, // Koridor Pesisir Selatan (Ipuh + buffer nelayan pesisir)
        -2.8200 to 101.2800, // Koridor Pesisir Tengah (Pondok Suguh & Teramang Jaya)
        -2.6000 to 101.0300, // Koridor Pesisir Kota Mukomuko & Air Dikit
        -2.4500 to 101.0150  // Koridor Pesisir Utara (Bandara Mukomuko & Lubuk Pinang)
    )

    data class GeofenceCheckResult(
        val isInsideMukomuko: Boolean,
        val nearestDistrictName: String,
        val distanceToNearestDistrictKm: Double,
        val rejectionMessage: String = ""
    )

    /**
     * Memeriksa apakah koordinat GPS berada di dalam cakupan Wilayah Administrasi Kabupaten Mukomuko.
     */
    fun isInsideMukomukoCoverage(latitude: Double, longitude: Double): Boolean {
        if (CoordinateValidator.validate(latitude, longitude).isFailure) return false
        if (latitude == 0.0 && longitude == 0.0) return false
        if (latitude !in MIN_LAT..MAX_LAT || longitude !in MIN_LON..MAX_LON) return false
        return isPointInPolygon(latitude, longitude, MUKOMUKO_ADMIN_POLYGON)
    }

    /**
     * Mengevaluasi posisi perangkat terhadap batas administrasi Kabupaten Mukomuko sekaligus
     * menghasilkan pesan penolakan Toast yang informatif bila berada di luar cakupan.
     */
    fun evaluate(latitude: Double, longitude: Double): GeofenceCheckResult {
        if (CoordinateValidator.validate(latitude, longitude).isFailure || (latitude == 0.0 && longitude == 0.0)) {
            return GeofenceCheckResult(
                isInsideMukomuko = false,
                nearestDistrictName = "Kabupaten Mukomuko",
                distanceToNearestDistrictKm = Double.POSITIVE_INFINITY,
                rejectionMessage = "Pengiriman laporan ditolak: Koordinat GPS perangkat tidak valid atau belum terdeteksi di dalam Wilayah Administrasi Kabupaten Mukomuko."
            )
        }

        val (nearestDistrict, distanceKm) = LocationHelper.findNearestDistrict(latitude, longitude)
        val inside = isInsideMukomukoCoverage(latitude, longitude)

        return if (inside) {
            GeofenceCheckResult(
                isInsideMukomuko = true,
                nearestDistrictName = nearestDistrict.name,
                distanceToNearestDistrictKm = distanceKm,
                rejectionMessage = ""
            )
        } else {
            val formattedDistance = String.format(Locale.US, "%.1f", distanceKm)
            GeofenceCheckResult(
                isInsideMukomuko = false,
                nearestDistrictName = nearestDistrict.name,
                distanceToNearestDistrictKm = distanceKm,
                rejectionMessage = "Pengiriman laporan ditolak: Perangkat Anda terdeteksi di luar Wilayah Administrasi Kabupaten Mukomuko (~$formattedDistance km dari Kec. ${nearestDistrict.name}). Pelaporan warga hanya diterima dari dalam cakupan Kabupaten Mukomuko."
            )
        }
    }

    /**
     * Ray-casting algorithm (even-odd rule) dengan penanganan aman pada titik tepi poligon.
     */
    private fun isPointInPolygon(
        lat: Double,
        lon: Double,
        polygon: List<Pair<Double, Double>>
    ): Boolean {
        var inside = false
        var j = polygon.lastIndex
        for (i in polygon.indices) {
            val (latI, lonI) = polygon[i]
            val (latJ, lonJ) = polygon[j]

            val intersects = ((lonI > lon) != (lonJ > lon)) &&
                (lat < (latJ - latI) * (lon - lonI) / ((lonJ - lonI).takeIf { it != 0.0 } ?: 1e-12) + latI)
            if (intersects) {
                inside = !inside
            }
            j = i
        }
        return inside
    }
}
