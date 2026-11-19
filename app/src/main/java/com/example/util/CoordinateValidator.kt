package com.example.util

/**
 * Validator koordinat geografis (Latitude & Longitude) sebelum data disimpan ke database.
 *
 * Aturan validasi:
 * 1. `lat` dan `lng` wajib berupa angka valid (`"Koordinat harus berupa angka"`).
 * 2. Rentang nilai wajib `-90 <= lat <= 90` dan `-180 <= lng <= 180` (`"Nilai koordinat tidak valid"`).
 */
object CoordinateValidator {

    const val ERROR_NOT_NUMBER = "Koordinat harus berupa angka"
    const val ERROR_INVALID_RANGE = "Nilai koordinat tidak valid"

    /**
     * Memvalidasi objek `lat` dan `lng` (Number atau String numerik).
     */
    fun validate(lat: Any?, lng: Any?): Result<Pair<Double, Double>> {
        val latNum = parseFiniteDouble(lat)
        val lngNum = parseFiniteDouble(lng)

        if (latNum == null || lngNum == null) {
            return Result.failure(IllegalArgumentException(ERROR_NOT_NUMBER))
        }

        if (latNum < -90.0 || latNum > 90.0 || lngNum < -180.0 || lngNum > 180.0) {
            return Result.failure(IllegalArgumentException(ERROR_INVALID_RANGE))
        }

        return Result.success(Pair(latNum, lngNum))
    }

    /**
     * Memvalidasi string koordinat berformat `"lat, lng"` (contoh: `"-2.5830, 101.1240"`).
     *
     * @param koordinat String koordinat dari input form atau body request.
     * @param allowKondusifDash Jika true, mengizinkan nilai `"-"` khusus saat Mode Kondusif (Tidak Ada Bencana).
     */
    fun validateKoordinatString(
        koordinat: String?,
        allowKondusifDash: Boolean = false
    ): Result<Pair<Double, Double>> {
        val raw = koordinat?.trim().orEmpty()
        if (allowKondusifDash && raw == "-") {
            return Result.success(Pair(0.0, 0.0))
        }
        if (raw.isEmpty()) {
            return Result.failure(IllegalArgumentException(ERROR_NOT_NUMBER))
        }

        val parts = if (raw.contains(",")) {
            raw.split(",")
        } else {
            raw.split(Regex("\\s+"))
        }

        if (parts.size != 2) {
            return Result.failure(IllegalArgumentException(ERROR_NOT_NUMBER))
        }

        return validate(parts[0].trim(), parts[1].trim())
    }

    private fun parseFiniteDouble(value: Any?): Double? {
        val parsed = when (value) {
            is Number -> value.toDouble()
            is String -> {
                val trimmed = value.trim()
                if (trimmed.isEmpty()) null else trimmed.toDoubleOrNull()
            }
            else -> null
        } ?: return null

        return if (parsed.isNaN() || parsed.isInfinite()) null else parsed
    }
}
