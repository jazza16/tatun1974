package com.example.util

/**
 * Formatter & parser standar untuk kolom "Cakupan Dampak" pada Form Laporan Harian
 * dan Output PDF Print Out Pusdalops-PB BPBD Kabupaten Mukomuko.
 *
 * Memastikan butir:
 *   1. Lokasi Kejadian
 *   2. Deskripsi Kejadian/Bencana
 * selalu terpisah pada baris tersendiri (minimal 2 baris) dan tidak pernah menyatu
 * menjadi satu baris dengan tanda " — " di form maupun hasil cetak PDF.
 */
object CakupanDampakFormatter {

    const val PREFIX_LOKASI = "1. Lokasi Kejadian:"
    const val PREFIX_DESKRIPSI = "2. Deskripsi Kejadian/Bencana:"

    data class StructuredImpact(
        val lokasiKejadian: String,
        val deskripsiKejadian: String
    ) {
        fun isEmpty(): Boolean = lokasiKejadian.isBlank() && deskripsiKejadian.isBlank()
    }

    /**
     * Membentuk teks terstruktur 2 baris bernomor untuk penyimpanan di LaporanHarian.cakupanDampak
     * dan tampilan input multiline.
     */
    fun formatStructured(lokasiKejadian: String, deskripsiKejadian: String): String {
        val cleanLokasi = stripItemPrefix(lokasiKejadian.trim())
        val cleanDeskripsi = stripItemPrefix(deskripsiKejadian.trim())
        return when {
            cleanLokasi.isNotBlank() && cleanDeskripsi.isNotBlank() ->
                "$PREFIX_LOKASI $cleanLokasi\n$PREFIX_DESKRIPSI $cleanDeskripsi"
            cleanLokasi.isNotBlank() ->
                "$PREFIX_LOKASI $cleanLokasi"
            cleanDeskripsi.isNotBlank() ->
                "$PREFIX_DESKRIPSI $cleanDeskripsi"
            else -> ""
        }
    }

    /**
     * Memecah teks `cakupanDampak` (baik format baru 2 baris bernomor maupun data lama
     * yang masih menyatu dengan pemisah " — " / " - ") menjadi 2 bagian terpisah:
     * (1) Lokasi Kejadian dan (2) Deskripsi Kejadian/Bencana.
     */
    fun parse(rawCakupanDampak: String, fallbackLokasi: String = ""): StructuredImpact {
        val trimmed = rawCakupanDampak.trim()
        if (trimmed.isBlank() || trimmed == "-") {
            return StructuredImpact(
                lokasiKejadian = fallbackLokasi.trim(),
                deskripsiKejadian = ""
            )
        }

        // 1. Cek format terstruktur multiline dengan penomoran "1." dan "2."
        val lines = trimmed.lines().map { it.trim() }.filter { it.isNotBlank() }
        val idxLine1 = lines.indexOfFirst { it.startsWith("1.") }
        val idxLine2 = lines.indexOfFirst { it.startsWith("2.") }
        if (idxLine1 >= 0 && idxLine2 > idxLine1) {
            val part1 = lines.subList(idxLine1, idxLine2).joinToString(" ").let(::stripItemPrefix)
            val part2 = lines.subList(idxLine2, lines.size).joinToString("\n").let(::stripItemPrefix)
            return StructuredImpact(
                lokasiKejadian = part1.ifBlank { fallbackLokasi.trim() },
                deskripsiKejadian = part2
            )
        }

        // 2. Cek pemisah lama " — " (em-dash) atau " – " (en-dash) yang menyatukan Lokasi dan Deskripsi
        val dashSeparators = listOf(" — ", " – ", " -- ", " —", "— ")
        for (sep in dashSeparators) {
            val idx = trimmed.indexOf(sep)
            if (idx > 0) {
                val left = stripItemPrefix(trimmed.substring(0, idx).trim())
                val right = stripItemPrefix(trimmed.substring(idx + sep.length).trim())
                if (left.isNotBlank() && right.isNotBlank()) {
                    return StructuredImpact(
                        lokasiKejadian = left,
                        deskripsiKejadian = right
                    )
                }
            }
        }

        // 3. Jika sudah berisi >= 2 baris manual tanpa penomoran
        if (lines.size >= 2) {
            val first = stripItemPrefix(lines.first())
            val rest = lines.drop(1).joinToString("\n").let(::stripItemPrefix)
            return StructuredImpact(
                lokasiKejadian = first,
                deskripsiKejadian = rest
            )
        }

        // 4. Satu baris teks bebas: pisahkan fallbackLokasi sebagai poin 1 jika berbeda
        val single = stripItemPrefix(trimmed)
        val cleanFallback = fallbackLokasi.trim()
        return if (
            cleanFallback.isNotBlank() &&
            !cleanFallback.equals("Wilayah Kabupaten Mukomuko", ignoreCase = true) &&
            !single.equals(cleanFallback, ignoreCase = true) &&
            !single.startsWith(cleanFallback, ignoreCase = true)
        ) {
            StructuredImpact(
                lokasiKejadian = cleanFallback,
                deskripsiKejadian = single
            )
        } else {
            StructuredImpact(
                lokasiKejadian = single,
                deskripsiKejadian = ""
            )
        }
    }

    /**
     * Menghasilkan daftar baris cetak resmi (1..N baris terpisah) untuk sel tabel "Cakupan Dampak"
     * pada output file .PDF agar poin 1 (Lokasi Kejadian) dan poin 2 (Deskripsi Kejadian/Bencana)
     * selalu terpisah tegas dan tidak pernah menyatu dalam 1 baris.
     */
    fun toPdfLines(rawCakupanDampak: String, fallbackLokasi: String = ""): List<String> {
        val trimmed = rawCakupanDampak.trim()
        if (trimmed.isBlank() || trimmed == "-") return listOf("-")

        val parsed = parse(trimmed, fallbackLokasi)
        val result = mutableListOf<String>()
        if (parsed.lokasiKejadian.isNotBlank()) {
            result.add("$PREFIX_LOKASI ${parsed.lokasiKejadian}")
        }
        if (parsed.deskripsiKejadian.isNotBlank()) {
            result.add("$PREFIX_DESKRIPSI ${parsed.deskripsiKejadian}")
        }
        return if (result.isNotEmpty()) result else listOf(trimmed)
    }

    /**
     * Menghasilkan teks gabungan multi-baris untuk preview/PDF non-blok.
     */
    fun formatForPdfPrint(rawCakupanDampak: String, fallbackLokasi: String = ""): String {
        return toPdfLines(rawCakupanDampak, fallbackLokasi).joinToString("\n")
    }

    private fun stripItemPrefix(text: String): String {
        return text
            .replace(Regex("^1\\.\\s*(Lokasi\\s*Kejadian\\s*:?)?\\s*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^2\\.\\s*(Deskripsi\\s*Kejadian(?:/Bencana(?:\\s*Lain(?:nya)?)?)?\\s*:?)?\\s*", RegexOption.IGNORE_CASE), "")
            .trim()
    }
}
