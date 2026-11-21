package com.example.data.local

import android.content.Context
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Room Database Entity for Official BPBD Mukomuko Personnel.
 * Optimized with high-performance indices on NIP (unique), role, shift, and name.
 */
@Entity(
    tableName = "personnel",
    indices = [
        Index(value = ["nip"], unique = true),
        Index(value = ["role"]),
        Index(value = ["shift"]),
        Index(value = ["name"]),
        Index(value = ["role", "shift"])
    ]
)
data class Personnel(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val nip: String,
    val role: String,
    val unit: String = "Pusdalops BPBD Kab. Mukomuko",
    val shift: String = "Shift Siaga 24 Jam",
    val phone: String = ""
) {
    fun formattedLabel(): String {
        val nipPrefix = if (nip.startsWith("TRC", ignoreCase = true)) "Reg TRC" else "NIP"
        return "$name ($nipPrefix: $nip)"
    }
}

/**
 * Official ASN BPBD Kabupaten Mukomuko assigned as Operator Pusdalops.
 * Pre-populated into the database credentials system with separate columns for Name and NIP.
 * This facilitates officer name selection via dropdown/picker on the login screen,
 * preventing unauthorized parties from manually typing spoofed names and NIPs.
 */
object OfficialAsnBpbdPersonnel {
    const val ROLE_PENGAWAS = "Pengawas BPBD"
    const val ROLE_SUPERVISOR = "Supervisor BPBD"
    const val ROLE_KOORD_ADMINISTRASI = "Koord. Administrasi"
    const val ROLE_MANAJER = "Manajer Pusdalops-PB"
    const val ROLE_OPERATOR = "Operator Pusdalops"
    const val ROLE_STAF_LOGISTIK_TRC = "Staf Logistik/TRC"

    val ASN_MUKOMUKO_2026_LIST: List<Pair<String, String>> = listOf(
        "A. RAHMAN" to "197906132005021002",
        "AAN FITRADINATA, SE" to "197903122006041007",
        "AHMAD HIDAYAT SYAH" to "197302182006041005",
        "AINUN MARDIATI CANIAGO, S.Pd" to "198812302025212141",
        "AMRI KURNIADI" to "197608122011011003",
        "ANDIKA JAYA PUTRA" to "199311282025211115",
        "ARIF WIJOYO, S.E" to "198606282025211024",
        "DAVID HARIYANTO, S.I.P" to "199303302025211135",
        "DESTRI GANDALIA, S.STP, M.A.P" to "198812242010102001",
        "DHUHAN PUTRA YUNARDI, S.M." to "200005282025211058",
        "DORI FRANCISKO, S.Pd." to "199512162025061002",
        "ELI SAHWATI" to "199604122025212113",
        "ERI YANTO" to "198912102025211038",
        "FEBRIZA RAHMADI" to "199502082024211009",
        "FERAHANDAYANI" to "197806182025212030",
        "FIRMANSYAH" to "199305102025211171",
        "HARI KURNIAWAN" to "198607092015021001",
        "HERMANDAHLI" to "197106092025211033",
        "HITATUN ARAZAK" to "197401282006041006",
        "LISMIE NOVTRIANTI" to "199711282025212144",
        "MECCA RIZKA, S.M." to "199908042025212096",
        "MEI SAHPUTRA" to "199405252019031005",
        "M. RIGAN JASWARI, S.Akun." to "200201252025211014",
        "NITA KUSWANI" to "197809192010012010",
        "NOKI PRADINATA, S.Agr" to "199211292025211127",
        "NOVRANDA EKA PUTRA" to "199011042019031006",
        "PUTRI MAYANG SARI" to "199906302025212010",
        "ROMI YANTI, S.Pd" to "199108092025212036",
        "RURI IRWANDI, ST, MT" to "197906072006041011",
        "SAMSUL BAHRI" to "198306102009041001",
        "SATRIA AMANDA PUTRA, S.M." to "200003112025211067",
        "SUMARNI" to "197502102009042001",
        "YESSY TRIYANI" to "198005042011012004",
        "YUDHA SOFRIANTO, S.A.P." to "199807202025061002"
    )

    val LEADERSHIP = listOf(
        Personnel(
            name = "RURI IRWANDI, ST, MT",
            nip = "197906072006041011",
            role = ROLE_PENGAWAS,
            unit = "BPBD Kab. Mukomuko",
            shift = "Pimpinan Siaga"
        ),
        Personnel(
            name = "HITATUN ARAZAK",
            nip = "197401282006041006",
            role = ROLE_MANAJER,
            unit = "Pusdalops BPBD Kab. Mukomuko",
            shift = "Manajerial 24 Jam"
        ),
        Personnel(
            name = "DESTRI GANDALIA, S.STP, M.A.P",
            nip = "198812242010102001",
            role = ROLE_SUPERVISOR,
            unit = "BPBD Kab. Mukomuko",
            shift = "Supervisi Posko"
        ),
        Personnel(
            name = "AAN FITRADINATA, SE",
            nip = "197903122006041007",
            role = ROLE_KOORD_ADMINISTRASI,
            unit = "BPBD Kab. Mukomuko",
            shift = "Administrasi & Logistik"
        )
    )

    val OPERATORS = listOf(
        Personnel(
            name = "YUDHA SOFRIANTO, S.A.P.",
            nip = "199807202025061002",
            role = ROLE_OPERATOR,
            unit = "Pusdalops BPBD Kab. Mukomuko",
            shift = "Shift Siaga 24 Jam"
        ),
        Personnel(
            name = "ERI YANTO",
            nip = "198912102025211038",
            role = ROLE_OPERATOR,
            unit = "Pusdalops BPBD Kab. Mukomuko",
            shift = "Shift Siaga 24 Jam"
        ),
        Personnel(
            name = "M. RIGAN JASWARI, S.Akun.",
            nip = "200201252025211014",
            role = ROLE_OPERATOR,
            unit = "Pusdalops BPBD Kab. Mukomuko",
            shift = "Shift Siaga 24 Jam"
        ),
        Personnel(
            name = "AINUN MARDIATI CANIAGO, S.Pd",
            nip = "198812302025212141",
            role = ROLE_OPERATOR,
            unit = "Pusdalops BPBD Kab. Mukomuko",
            shift = "Shift Siaga 24 Jam"
        ),
        Personnel(
            name = "SATRIA AMANDA PUTRA, S.M.",
            nip = "200003112025211067",
            role = ROLE_OPERATOR,
            unit = "Pusdalops BPBD Kab. Mukomuko",
            shift = "Shift Siaga 24 Jam"
        ),
        Personnel(
            name = "FIRMANSYAH",
            nip = "199305102025211171",
            role = ROLE_OPERATOR,
            unit = "Pusdalops BPBD Kab. Mukomuko",
            shift = "Shift Siaga 24 Jam"
        ),
        Personnel(
            name = "ARIF WIJOYO, S.E",
            nip = "198606282025211024",
            role = ROLE_OPERATOR,
            unit = "Pusdalops BPBD Kab. Mukomuko",
            shift = "Shift Siaga 24 Jam"
        ),
        Personnel(
            name = "DAVID HARIYANTO, S.I.P",
            nip = "199303302025211135",
            role = ROLE_OPERATOR,
            unit = "Pusdalops BPBD Kab. Mukomuko",
            shift = "Shift Siaga 24 Jam"
        )
    )

    val STAF_LOGISTIK_TRC: List<Personnel> = ASN_MUKOMUKO_2026_LIST
        .filter { (_, nip) ->
            (LEADERSHIP + OPERATORS).none { it.nip.equals(nip, ignoreCase = true) }
        }
        .map { (name, nip) ->
            Personnel(
                name = name,
                nip = nip,
                role = ROLE_STAF_LOGISTIK_TRC,
                unit = "BPBD Kab. Mukomuko",
                shift = "Shift Siaga 24 Jam"
            )
        }

    val ALL_OFFICIAL_PERSONNEL = LEADERSHIP + OPERATORS + STAF_LOGISTIK_TRC

    /**
     * Normalizes an officer name/NIP string for fuzzy lookup (e.g. "HITATUN" -> "HITATUN ARAZAK",
     * "RURI IRWANDI, ST.MT" -> "RURI IRWANDI, ST, MT", "AINUN MARDIATI.S.Pd.SD" -> "AINUN MARDIATI CANIAGO, S.Pd").
     */
    private fun normalizeAlphaNum(value: String): String {
        return value.uppercase().replace(Regex("[^A-Z0-9]"), "")
    }

    /**
     * Extracts clean name and optional embedded NIP from formatted strings like:
     * "HITATUN ARAZAK (NIP: 197401282006041006)" or "HITATUN ARAZAK / 197401282006041006"
     */
    fun extractCleanNameAndNip(rawText: String): Pair<String, String> {
        val trimmed = rawText.trim()
        if (trimmed.isEmpty()) return "" to ""
        val nipInParens = Regex("""\((?:NIP|Reg\s*TRC|ID/NIP)[.:\s]*([A-Za-z0-9\-_]+)\)""", RegexOption.IGNORE_CASE)
            .find(trimmed)
        if (nipInParens != null) {
            val extractedNip = nipInParens.groupValues[1].trim()
            val cleanName = trimmed.removeRange(nipInParens.range).trim().trimEnd('-', '•', '/')
            return cleanName to extractedNip
        }
        if (trimmed.contains(" / ")) {
            val parts = trimmed.split(" / ", limit = 2)
            val left = parts[0].trim()
            val right = parts[1].trim()
            if (right.any { it.isDigit() }) {
                return left to right
            }
        }
        val eighteenDigits = Regex("""\b(\d{18})\b""").find(trimmed)
        if (eighteenDigits != null && trimmed.length > 18) {
            val extractedNip = eighteenDigits.groupValues[1]
            val cleanName = trimmed
                .replace(extractedNip, "")
                .replace(Regex("""\(\s*(?:NIP|ID)?[.:\s]*\)""", RegexOption.IGNORE_CASE), "")
                .trim()
                .trimEnd('-', '•', '/', ',')
                .trim()
            return cleanName to extractedNip
        }
        return trimmed to ""
    }

    /**
     * Finds an official ASN/BPBD personnel record by NIP or Name (exact or prefix/base-name match).
     */
    fun findByNipOrName(query: String): Personnel? {
        val raw = query.trim()
        if (raw.isBlank() || raw == "-") return null
        val (cleanName, embeddedNip) = extractCleanNameAndNip(raw)
        val candidates = listOf(embeddedNip, raw, cleanName).filter { it.isNotBlank() && it != "-" }

        // 1. Exact NIP match
        for (c in candidates) {
            val byNip = ALL_OFFICIAL_PERSONNEL.firstOrNull { it.nip.equals(c, ignoreCase = true) }
            if (byNip != null) return byNip
        }

        // 2. Exact full name match
        for (c in candidates) {
            val byExactName = ALL_OFFICIAL_PERSONNEL.firstOrNull { it.name.equals(c, ignoreCase = true) }
            if (byExactName != null) return byExactName
        }

        // 3. Normalized alphanumeric or base-name match (without degree suffix)
        for (c in candidates) {
            val normQuery = normalizeAlphaNum(c)
            val baseQuery = normalizeAlphaNum(c.substringBefore(","))
            if (normQuery.length < 3) continue
            val match = ALL_OFFICIAL_PERSONNEL.firstOrNull { p ->
                val normTarget = normalizeAlphaNum(p.name)
                val baseTarget = normalizeAlphaNum(p.name.substringBefore(","))
                normTarget == normQuery ||
                    (baseQuery.length >= 4 && baseTarget == baseQuery) ||
                    (baseQuery.length >= 5 && (normTarget.startsWith(baseQuery) || baseTarget.startsWith(baseQuery)))
            }
            if (match != null) return match
        }
        return null
    }

    /**
     * Resolves any raw name & NIP combination against the official BPBD roster.
     * Guarantees both canonical `name` and `nip` are returned in sync.
     */
    fun resolveCanonicalPersonnel(
        rawName: String,
        rawNip: String = "",
        defaultRole: String = ROLE_OPERATOR
    ): Personnel {
        val (extractedName, extractedNip) = extractCleanNameAndNip(rawName)
        val effectiveNip = rawNip.trim().takeIf { it.isNotBlank() && it != "-" } ?: extractedNip
        val effectiveName = extractedName.ifBlank { rawName.trim() }

        val byNip = if (effectiveNip.isNotBlank()) findByNipOrName(effectiveNip) else null
        if (byNip != null) return byNip

        val byName = if (effectiveName.isNotBlank()) findByNipOrName(effectiveName) else null
        if (byName != null) return byName

        // Fallback if custom officer not in default 34 ASN list
        val fallbackDefault = LEADERSHIP.first { it.role == ROLE_MANAJER } // HITATUN ARAZAK (197401282006041006)
        val finalName = effectiveName.ifBlank { fallbackDefault.name }
        val finalNip = effectiveNip.ifBlank {
            if (finalName.contains("HITATUN", ignoreCase = true)) fallbackDefault.nip else "-"
        }
        return Personnel(
            name = finalName,
            nip = finalNip,
            role = defaultRole
        )
    }

    fun toOfficerProfiles(): List<OfficerProfile> {
        return ALL_OFFICIAL_PERSONNEL.map {
            OfficerProfile(
                id = "asn-${it.nip}",
                fullName = it.name,
                nip = it.nip,
                role = it.role,
                unit = it.unit,
                shift = it.shift,
                phone = it.phone
            )
        }
    }

    fun getRoleBadgeColor(role: String): Pair<androidx.compose.ui.graphics.Color, androidx.compose.ui.graphics.Color> {
        return when {
            role.contains("Pengawas", ignoreCase = true) ->
                Pair(androidx.compose.ui.graphics.Color(0xFFEDE9FE), androidx.compose.ui.graphics.Color(0xFF6D28D9)) // Ungu
            role.contains("Supervisor", ignoreCase = true) ->
                Pair(androidx.compose.ui.graphics.Color(0xFFE0F2FE), androidx.compose.ui.graphics.Color(0xFF0369A1)) // Biru Muda
            role.contains("Koord", ignoreCase = true) || role.contains("Administrasi", ignoreCase = true) ->
                Pair(androidx.compose.ui.graphics.Color(0xFFCCFBF1), androidx.compose.ui.graphics.Color(0xFF0F766E)) // Hijau Teal
            role.contains("Manajer", ignoreCase = true) ->
                Pair(androidx.compose.ui.graphics.Color(0xFFFEF3C7), androidx.compose.ui.graphics.Color(0xFFB45309)) // Amber Emas
            role.contains("Operator", ignoreCase = true) ->
                Pair(androidx.compose.ui.graphics.Color(0xFFFFEDD5), androidx.compose.ui.graphics.Color(0xFFC2410C)) // Oranye BPBD
            role.contains("TRC", ignoreCase = true) || role.contains("Lapangan", ignoreCase = true) ->
                Pair(androidx.compose.ui.graphics.Color(0xFFFEE2E2), androidx.compose.ui.graphics.Color(0xFFB91C1C)) // Merah Siaga
            else ->
                Pair(androidx.compose.ui.graphics.Color(0xFFF1F5F9), androidx.compose.ui.graphics.Color(0xFF475569)) // Netral Slate
        }
    }
}

/**
 * Seeding 34 data direktori ASN BPBD Kabupaten Mukomuko ke dalam tabel lokal `personnel` (non-rahasia).
 * - Hanya mengisi nama, NIP, jabatan, unit, shift, dan telepon ke tabel `personnel` jika NIP belum ada.
 * - TIDAK PERNAH mengisi PIN default ('117' atau lainnya) ke lokal maupun ke Firebase.
 * - TIDAK PERNAH menimpa atau mereset `officer_credentials` di Cloud maupun `saved_nip` di SharedPreferences
 *   saat aplikasi di-update atau di-install ulang.
 */
fun seedDataAsnMukomuko2026(context: Context) {
    val appContext = context.applicationContext
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val db = AppDatabase.getInstance(appContext)
            val personnelDao = db.personnelDao()

            for (official in OfficialAsnBpbdPersonnel.ALL_OFFICIAL_PERSONNEL) {
                val name = official.name.trim()
                val nip = official.nip.trim()
                if (name.isEmpty() || nip.isEmpty()) continue

                // Hanya masukkan data direktori non-rahasia ke tabel `personnel` jika NIP belum terdaftar
                val existingPersonnel = personnelDao.getPersonnelByNip(nip)
                if (existingPersonnel == null) {
                    personnelDao.insertPersonnel(
                        Personnel(
                            name = name,
                            nip = nip,
                            role = official.role,
                            unit = official.unit,
                            shift = official.shift,
                            phone = official.phone
                        )
                    )
                }
            }
        } catch (e: Throwable) {
            android.util.Log.w("PersonnelSeed", "Gagal melakukan seed data ASN Mukomuko (non-fatal): ${e.message}")
        }
    }
}
