package com.example.util

import java.util.Locale

/**
 * Hasil pemisahan nama utama pegawai dan gelar akademik di bagian akhir.
 */
data class ParsedOfficerName(
    val mainName: String,
    val degrees: String,
    val hasCommaBeforeDegrees: Boolean
)

/**
 * Utilitas dinamis untuk memformat nama panjang pegawai menjadi inisial pada bagian
 * tengah/akhir sambil mempertahankan gelar akademik di belakang secara utuh.
 *
 * Contoh (`maxWords = 2`):
 * - "AINUN MARDIATI CANIAGO SPd"       -> "AINUN MARDIATI C SPd"
 * - "MUHAMMAD RIZKY PRATAMA, S.Kom"    -> "MUHAMMAD RIZKY P, S.Kom"
 * - "BUDI SANTOSO"                     -> "BUDI SANTOSO"
 *
 * Contoh (`maxWords = 1`):
 * - "AINUN MARDIATI CANIAGO SPd"       -> "AINUN M C SPd"
 */
object OfficerNameFormatter {

    private val DOTTED_DEGREE_REGEX = Regex("^[A-Za-z]{1,4}(\\.[A-Za-z]{1,5})+\\.?$")

    /**
     * Daftar gelar akademik/profesi Indonesia (tanpa titik, huruf kecil) untuk mendeteksi
     * gelar yang ditulis tanpa koma maupun tanpa titik (mis. SPd, SKom, MSi, SE, ST, MM, AMd).
     */
    private val KNOWN_TRAILING_DEGREES: Set<String> = setOf(
        // Diploma & Sarjana Terapan
        "amd", "amdkep", "amdkeb", "amdkom", "amdak", "amdfarm", "amdt", "amdpar",
        "amdgz", "amdpel", "amdab", "amdkl", "amdro", "amdfis", "ama", "amapd",
        "str", "strkom", "strkep", "strkeb", "strt", "strak", "strgz", "strs", "strip",
        // Sarjana (S1)
        "spd", "spdi", "spdsd", "spdh", "spdgr", "skom", "sikom", "st", "se", "sh", "shi",
        "ssos", "sip", "skm", "skep", "sfarm", "spsi", "sag", "shut", "spt", "spi", "sgz",
        "ssi", "smn", "sak", "sm", "sp", "sstat", "sars", "sgeo", "sfis", "sbio", "skim",
        "smat", "sfil", "ss", "shum", "skg", "skh", "sked", "sadm", "spar", "sabi",
        "sakt", "sdes", "sds", "skes", "sorg", "sost", "spw", "spsi", "ssy", "sth", "sthi",
        "ba", "bsc", "beng", "bba", "bed", "llb",
        // Magister (S2) & Doktor (S3)
        "mpd", "mpdi", "mkom", "mikom", "mt", "mm", "mh", "mhi", "msi", "mkes", "map",
        "mba", "msc", "meng", "mhum", "mkn", "mak", "mag", "mec", "mstat", "mpsi",
        "mfarm", "mkep", "mars", "mfil", "msos", "mip", "mp", "mhut", "mpt", "mpi",
        "mbiomed", "mepid", "mkm", "macc", "mfin", "ma", "med", "mth", "llm", "phd",
        "edd", "dba", "drhc",
        // Profesi & Sertifikasi ASN
        "ns", "apt", "ak", "akt", "ca", "cpa", "gr", "ipu", "ipm", "ipp", "aseaneng",
        "drg", "drh", "bd", "bdn", "psikolog"
    )

    /**
     * Memeriksa apakah satu token (kata) merupakan gelar akademik/profesi bagian belakang.
     */
    fun isAcademicDegreeToken(rawToken: String): Boolean {
        val cleaned = rawToken.trim().trimEnd(',', ';')
        if (cleaned.isBlank()) return false

        // Pola bertitik seperti S.Pd, S.Kom., M.Si, A.Md.Kep, Ph.D., S.Tr.Kom
        if (DOTTED_DEGREE_REGEX.matches(cleaned)) return true

        // Normalisasi tanpa titik dan cek terhadap daftar gelar resmi
        val normalized = cleaned.replace(".", "").lowercase(Locale.ROOT)
        if (normalized in KNOWN_TRAILING_DEGREES) return true

        // Kasus penulisan khusus dengan huruf campuran klasik seperti "SPd", "SKom", "MSi", "AMd"
        if (Regex("^[SMAB][A-Z][a-z]{1,4}$").matches(cleaned)) return true

        return false
    }

    /**
     * Memisahkan nama utama pegawai dari gelar akademik di bagian akhir.
     * Mendukung format berkoma ("AINUN MARDIATI CANIAGO, S.Pd., M.Si.") maupun tanpa koma ("AINUN MARDIATI CANIAGO SPd").
     */
    fun splitNameAndDegrees(rawInput: String): ParsedOfficerName {
        val normalized = rawInput.trim().replace(Regex("\\s+"), " ")
        if (normalized.isBlank()) {
            return ParsedOfficerName(mainName = "", degrees = "", hasCommaBeforeDegrees = false)
        }

        // 1. Jika ada koma, bagian setelah koma pertama adalah blok gelar belakang
        val commaIndex = normalized.indexOf(',')
        if (commaIndex > 0) {
            val leftPart = normalized.substring(0, commaIndex).trim()
            val rightPart = normalized.substring(commaIndex + 1).trim()
            return ParsedOfficerName(
                mainName = leftPart,
                degrees = rightPart,
                hasCommaBeforeDegrees = rightPart.isNotBlank()
            )
        }

        // 2. Tanpa koma: pindai token dari kanan ke kiri selama cocok pola gelar akademik
        val tokens = normalized.split(" ").filter { it.isNotBlank() }
        if (tokens.size <= 1) {
            return ParsedOfficerName(mainName = normalized, degrees = "", hasCommaBeforeDegrees = false)
        }

        var splitIdx = tokens.size
        while (splitIdx > 1 && isAcademicDegreeToken(tokens[splitIdx - 1])) {
            splitIdx--
        }

        val mainName = tokens.subList(0, splitIdx).joinToString(" ")
        val degrees = if (splitIdx < tokens.size) {
            tokens.subList(splitIdx, tokens.size).joinToString(" ")
        } else {
            ""
        }

        return ParsedOfficerName(
            mainName = mainName,
            degrees = degrees,
            hasCommaBeforeDegrees = false
        )
    }

    /**
     * Memformat nama panjang pegawai menjadi inisial dinamis sesuai `maxWords` (default: 2).
     *
     * Aturan:
     * 1. Gelar akademik di bagian akhir dipisahkan lebih dulu.
     * 2. Jika jumlah kata nama utama <= `maxWords`, nama dibiarkan utuh.
     * 3. Jika lebih dari `maxWords`, ambil `maxWords` kata pertama secara utuh, lalu ubah kata
     *    berikutnya menjadi inisial (1 huruf pertama kapital tanpa titik).
     * 4. Gabungkan kembali nama yang sudah disingkat dengan gelarnya.
     */
    fun formatLongOfficerName(
        fullNameWithTitle: String,
        maxWords: Int = 2
    ): String {
        val safeMaxWords = maxWords.coerceAtLeast(1)
        val parsed = splitNameAndDegrees(fullNameWithTitle)
        if (parsed.mainName.isBlank()) return fullNameWithTitle.trim()

        val nameWords = parsed.mainName.split(Regex("\\s+")).filter { it.isNotBlank() }
        val formattedMainName = if (nameWords.size <= safeMaxWords) {
            nameWords.joinToString(" ")
        } else {
            val keptWords = nameWords.take(safeMaxWords)
            val initials = nameWords.drop(safeMaxWords).mapNotNull { word ->
                val firstAlpha = word.firstOrNull { it.isLetterOrDigit() }
                firstAlpha?.uppercaseChar()?.toString()
            }
            (keptWords + initials).joinToString(" ")
        }

        return when {
            parsed.degrees.isBlank() -> formattedMainName
            parsed.hasCommaBeforeDegrees -> "$formattedMainName, ${parsed.degrees}"
            else -> "$formattedMainName ${parsed.degrees}"
        }
    }
}
