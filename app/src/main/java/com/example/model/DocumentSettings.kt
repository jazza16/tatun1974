package com.example.model

import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/**
 * Mode pemformatan Nama dan Gelar Petugas/Pejabat yang dapat diubah dinamis
 * oleh Admin BPBD dari Dashboard Web melalui endpoint `GET /document-settings`
 * atau Cloud Firestore `app_settings/document_settings` tanpa rilis ulang aplikasi.
 */
object GelarFormat {
    /** Tampilkan lengkap sesuai data server: "Dr. Ruri Irwandi, S.T., M.T." */
    const val FULL = "FULL"

    /** Huruf kapital seluruhnya sesuai tata naskah dinas tradisional: "RURI IRWANDI, ST, MT" */
    const val UPPERCASE = "UPPERCASE"

    /** Title Case untuk nama pokok, gelar dipertahankan sesuai kaidah: "Ruri Irwandi, ST, MT" */
    const val TITLE_CASE = "TITLE_CASE"

    /** Hanya nama pokok tanpa gelar depan/belakang (ringkas untuk kotak sempit) */
    const val NO_TITLE = "NO_TITLE"

    /** Pastikan pemisah koma baku antara nama dan gelar belakang */
    const val COMMA_SEPARATED = "COMMA_SEPARATED"

    /** Hapus titik pada gelar belakang agar lebih ringkas: "RURI IRWANDI, ST, MT" */
    const val SHORT_DOT = "SHORT_DOT"

    /** Singkat kata nama setelah `max_words` menjadi inisial tanpa titik: "AINUN MARDIATI C SPd" */
    const val ABBREVIATE_NAME = "ABBREVIATE_NAME"

    fun normalize(raw: String?): String {
        val candidate = raw?.trim()?.uppercase(Locale.ROOT).orEmpty()
        return when (candidate) {
            FULL, UPPERCASE, TITLE_CASE, NO_TITLE, COMMA_SEPARATED, SHORT_DOT, ABBREVIATE_NAME -> candidate
            else -> FULL
        }
    }
}

/**
 * Konfigurasi 1 slot penanda tangan pada blok tanda tangan Tata Naskah Dinas (Piramida Terbalik).
 * Data sepenuhnya bersumber dari Server/API (`GET /document-settings` / Firestore).
 */
data class RemoteSignatoryConfig(
    val slotKey: String,           // "koord_admin", "manajer_pusdalops", "pengawas"
    val roleLine1: String,
    val roleLine2: String,
    val nama: String = "",
    val gelarDepan: String = "",
    val gelarBelakang: String = "",
    val nip: String = ""
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("slotKey", slotKey)
        put("roleLine1", roleLine1)
        put("roleLine2", roleLine2)
        put("nama", nama)
        put("gelarDepan", gelarDepan)
        put("gelarBelakang", gelarBelakang)
        put("nip", nip)
    }

    companion object {
        fun fromJson(obj: JSONObject): RemoteSignatoryConfig? {
            val slotKey = obj.optString("slotKey").ifBlank { obj.optString("slot_key") }.trim()
            if (slotKey.isBlank()) return null
            return RemoteSignatoryConfig(
                slotKey = slotKey,
                roleLine1 = obj.optString("roleLine1").ifBlank { obj.optString("role_line_1") }.trim(),
                roleLine2 = obj.optString("roleLine2").ifBlank { obj.optString("role_line_2") }.trim(),
                nama = obj.optString("nama").ifBlank { obj.optString("name") }.trim(),
                gelarDepan = obj.optString("gelarDepan").ifBlank { obj.optString("gelar_depan") }.trim(),
                gelarBelakang = obj.optString("gelarBelakang").ifBlank { obj.optString("gelar_belakang") }.trim(),
                nip = obj.optString("nip").trim()
            )
        }
    }
}

/**
 * Data personil/petugas murni dari API/Server (`GET /officers` atau tertanam di `GET /document-settings`).
 */
data class RemoteOfficerRecord(
    val id: String = "",
    val nama: String,
    val gelarDepan: String = "",
    val gelarBelakang: String = "",
    val fullName: String = "",
    val nip: String = "",
    val role: String = "",
    val unit: String = "",
    val shift: String = "",
    val phone: String = ""
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("nama", nama)
        put("gelarDepan", gelarDepan)
        put("gelarBelakang", gelarBelakang)
        put("fullName", fullName)
        put("nip", nip)
        put("role", role)
        put("unit", unit)
        put("shift", shift)
        put("phone", phone)
    }

    companion object {
        fun fromJson(obj: JSONObject): RemoteOfficerRecord? {
            val fullName = obj.optString("fullName").ifBlank { obj.optString("full_name") }.trim()
            val nama = obj.optString("nama").ifBlank { obj.optString("name") }.trim()
            val nip = obj.optString("nip").trim()
            if (nama.isBlank() && fullName.isBlank() && nip.isBlank()) return null
            return RemoteOfficerRecord(
                id = obj.optString("id").ifBlank { nip }.trim(),
                nama = nama.ifBlank { fullName },
                gelarDepan = obj.optString("gelarDepan").ifBlank { obj.optString("gelar_depan") }.trim(),
                gelarBelakang = obj.optString("gelarBelakang").ifBlank { obj.optString("gelar_belakang") }.trim(),
                fullName = fullName,
                nip = nip,
                role = obj.optString("role").ifBlank { obj.optString("jabatan") }.trim(),
                unit = obj.optString("unit").trim(),
                shift = obj.optString("shift").trim(),
                phone = obj.optString("phone").ifBlank { obj.optString("telepon") }.trim()
            )
        }
    }
}

/**
 * Hasil akhir blok tanda tangan yang siap dirender ke Canvas PDF tanpa logika hardcode di UI.
 */
data class ResolvedSignatory(
    val slotKey: String,
    val roleLine1: String,
    val roleLine2: String,
    val formattedName: String,
    val nip: String,
    val nipLine: String
)

/**
 * Konfigurasi dokumen PDF yang bersifat Data-Driven (dikontrol oleh Admin BPBD dari Backend / Web Dashboard).
 * Diambil dari `GET /document-settings` (utama), Firestore `app_settings/document_settings` (sekunder),
 * atau cache lokal saat perangkat petugas di lapangan sedang offline.
 */
data class DocumentSettings(
    val fontSize: Float = DEFAULT_FONT_SIZE,
    val minFontSize: Float = DEFAULT_MIN_FONT_SIZE,
    val bodyFontSize: Float = DEFAULT_BODY_FONT_SIZE,
    val maxLengthBeforeTruncate: Int = DEFAULT_MAX_LENGTH_BEFORE_TRUNCATE,
    val maxWords: Int = DEFAULT_MAX_WORDS,
    val abbreviateLongNames: Boolean = false,
    val gelarFormat: String = GelarFormat.FULL,
    val nipPrefix: String = DEFAULT_NIP_PREFIX,
    val useRemotePdfTemplate: Boolean = true,
    val remotePdfTimeoutMs: Long = DEFAULT_REMOTE_PDF_TIMEOUT_MS,
    val kopHeaderLine1: String = DEFAULT_KOP_LINE_1,
    val kopHeaderLine2: String = DEFAULT_KOP_LINE_2,
    val kopHeaderLine3: String = DEFAULT_KOP_LINE_3,
    val kopAddressLine: String = DEFAULT_KOP_ADDRESS,
    val footerText: String = DEFAULT_FOOTER_TEXT,
    val signatories: List<RemoteSignatoryConfig> = defaultSignatorySlots(),
    val officers: List<RemoteOfficerRecord> = emptyList(),
    val version: Int = 1,
    val updatedAt: Long = 0L,
    val source: String = SOURCE_DEFAULT
) {
    /**
     * Memastikan seluruh parameter numerik berada pada rentang aman sehingga konfigurasi JSON
     * yang keliru dari backend tidak merusak tata letak PDF.
     */
    fun clamped(): DocumentSettings {
        val safeFontSize = fontSize.coerceIn(MIN_ALLOWED_FONT_SIZE, MAX_ALLOWED_FONT_SIZE)
        val safeMinFontSize = minFontSize.coerceIn(4.5f, safeFontSize)
        val safeBodyFontSize = bodyFontSize.coerceIn(6.0f, 12.0f)
        val safeMaxTruncate = maxLengthBeforeTruncate.coerceIn(32, 500)
        val safeMaxWords = maxWords.coerceIn(1, 6)
        val safeTimeout = remotePdfTimeoutMs.coerceIn(1_500L, 30_000L)
        val normalizedKopLine2 = kopHeaderLine2.trim().let {
            if (it.isBlank() || it.equals(LEGACY_DEFAULT_KOP_LINE_2, ignoreCase = true)) {
                DEFAULT_KOP_LINE_2
            } else {
                it
            }
        }
        val normalizedKopAddress = kopAddressLine.trim().let {
            if (it.isBlank() || it.equals(LEGACY_DEFAULT_KOP_ADDRESS, ignoreCase = true)) {
                DEFAULT_KOP_ADDRESS
            } else {
                it
            }
        }
        return copy(
            fontSize = safeFontSize,
            minFontSize = safeMinFontSize,
            bodyFontSize = safeBodyFontSize,
            maxLengthBeforeTruncate = safeMaxTruncate,
            maxWords = safeMaxWords,
            gelarFormat = GelarFormat.normalize(gelarFormat),
            nipPrefix = nipPrefix.trim().ifBlank { DEFAULT_NIP_PREFIX },
            remotePdfTimeoutMs = safeTimeout,
            kopHeaderLine1 = kopHeaderLine1.trim().ifBlank { DEFAULT_KOP_LINE_1 },
            kopHeaderLine2 = normalizedKopLine2,
            kopHeaderLine3 = kopHeaderLine3.trim().ifBlank { DEFAULT_KOP_LINE_3 },
            kopAddressLine = normalizedKopAddress,
            footerText = footerText.trim().ifBlank { DEFAULT_FOOTER_TEXT }
        )
    }

    /**
     * Memformat Nama + Gelar Depan + Gelar Belakang sesuai `gelarFormat` yang ditetapkan server.
     */
    fun formatNameWithGelar(
        nama: String,
        gelarDepan: String = "",
        gelarBelakang: String = "",
        formatOverride: String? = null
    ): String {
        val activeFormat = GelarFormat.normalize(formatOverride ?: gelarFormat)
        val cleanName = nama.trim().replace(Regex("\\s+"), " ")
        val cleanDepan = gelarDepan.trim().replace(Regex("\\s+"), " ")
        val cleanBelakang = gelarBelakang.trim().replace(Regex("\\s+"), " ").trimStart(',').trim()

        if (cleanName.isBlank()) return "-"

        // Jika nama sudah mengandung gelar belakang (mis. "RURI IRWANDI, ST, MT") dan field gelar terpisah kosong,
        // delegasikan ke parser nama mentah agar mode NO_TITLE / SHORT_DOT / TITLE_CASE tetap bekerja konsisten.
        if (cleanDepan.isBlank() && cleanBelakang.isBlank()) {
            return formatRawFullName(cleanName, activeFormat)
        }

        val normalizedDepan = when (activeFormat) {
            GelarFormat.NO_TITLE -> ""
            GelarFormat.UPPERCASE -> cleanDepan.uppercase(Locale.ROOT)
            GelarFormat.SHORT_DOT -> cleanDepan.replace(".", "").trim()
            else -> cleanDepan
        }

        val normalizedCore = when (activeFormat) {
            GelarFormat.UPPERCASE -> cleanName.uppercase(Locale.ROOT)
            GelarFormat.TITLE_CASE -> toTitleCase(cleanName)
            else -> cleanName
        }

        val normalizedBelakang = when (activeFormat) {
            GelarFormat.NO_TITLE -> ""
            GelarFormat.UPPERCASE -> cleanBelakang.uppercase(Locale.ROOT)
            GelarFormat.SHORT_DOT -> cleanBelakang.replace(".", "").replace(Regex("\\s*,\\s*"), ", ").trim()
            GelarFormat.COMMA_SEPARATED -> cleanBelakang.replace(Regex("\\s*,\\s*"), ", ").trim()
            else -> cleanBelakang
        }

        val withPrefix = if (normalizedDepan.isNotBlank()) "$normalizedDepan $normalizedCore" else normalizedCore
        val combined = if (normalizedBelakang.isNotBlank()) {
            "$withPrefix, $normalizedBelakang"
        } else {
            withPrefix
        }
        return if (activeFormat == GelarFormat.ABBREVIATE_NAME || abbreviateLongNames) {
            com.example.util.OfficerNameFormatter.formatLongOfficerName(combined, maxWords = maxWords)
        } else {
            combined
        }
    }

    /**
     * Memformat string nama lengkap yang mungkin sudah berisi gelar depan/belakang
     * (contoh: "Dr. RURI IRWANDI, S.T., M.T." atau "AINUN MARDIATI CANIAGO SPd")
     * mengikuti `gelarFormat` dan `maxWords` dari server.
     */
    fun formatRawFullName(rawFullName: String, formatOverride: String? = null): String {
        val activeFormat = GelarFormat.normalize(formatOverride ?: gelarFormat)
        val trimmed = rawFullName.trim().replace(Regex("\\s+"), " ")
        if (trimmed.isBlank() || trimmed == "-") return "-"

        if (activeFormat == GelarFormat.ABBREVIATE_NAME) {
            return com.example.util.OfficerNameFormatter.formatLongOfficerName(trimmed, maxWords = maxWords)
        }

        val parts = trimmed.split(",", limit = 2)
        val leftPart = parts[0].trim()
        val suffixPart = parts.getOrNull(1)?.trim().orEmpty()

        val (prefixTitles, coreName) = splitPrefixTitles(leftPart)

        val formatted = when (activeFormat) {
            GelarFormat.NO_TITLE -> coreName.ifBlank { leftPart }
            GelarFormat.UPPERCASE -> trimmed.uppercase(Locale.ROOT)
            GelarFormat.TITLE_CASE -> {
                val titledCore = toTitleCase(coreName.ifBlank { leftPart })
                val withPrefix = if (prefixTitles.isNotBlank()) "$prefixTitles $titledCore" else titledCore
                if (suffixPart.isNotBlank()) "$withPrefix, $suffixPart" else withPrefix
            }
            GelarFormat.SHORT_DOT -> {
                val compactPrefix = prefixTitles.replace(".", "").trim()
                val compactSuffix = suffixPart.replace(".", "").replace(Regex("\\s*,\\s*"), ", ").trim()
                val withPrefix = if (compactPrefix.isNotBlank()) "$compactPrefix ${coreName.ifBlank { leftPart }}" else coreName.ifBlank { leftPart }
                if (compactSuffix.isNotBlank()) "$withPrefix, $compactSuffix" else withPrefix
            }
            GelarFormat.COMMA_SEPARATED -> {
                val normalizedSuffix = suffixPart.replace(Regex("\\s*,\\s*"), ", ").trim()
                if (normalizedSuffix.isNotBlank()) "$leftPart, $normalizedSuffix" else leftPart
            }
            else -> trimmed
        }

        return if (abbreviateLongNames) {
            com.example.util.OfficerNameFormatter.formatLongOfficerName(formatted, maxWords = maxWords)
        } else {
            formatted
        }
    }

    /**
     * Memformat baris NIP dengan prefiks dinamis dari server (mis. "NIP. 1979...").
     */
    fun formatNipLine(nip: String): String {
        val cleanNip = nip.trim()
        if (cleanNip.isBlank() || cleanNip == "-") return "$nipPrefix -"
        val prefix = if (cleanNip.startsWith("TRC", ignoreCase = true)) "Reg TRC." else nipPrefix
        return "$prefix $cleanNip"
    }

    /**
     * Memotong teks panjang menggunakan batas `maxLengthBeforeTruncate` dari konfigurasi server.
     */
    fun truncateIfNeeded(rawText: String, overrideMaxLength: Int? = null): String {
        val clean = rawText.replace("\r", " ").replace("\n", " ").trim()
        val limit = (overrideMaxLength ?: maxLengthBeforeTruncate).coerceAtLeast(8)
        return if (clean.length > limit) {
            clean.take((limit - 3).coerceAtLeast(1)).trimEnd() + "..."
        } else {
            clean
        }
    }

    fun toJson(): String {
        val root = JSONObject().apply {
            put("fontSize", fontSize.toDouble())
            put("minFontSize", minFontSize.toDouble())
            put("bodyFontSize", bodyFontSize.toDouble())
            put("maxLengthBeforeTruncate", maxLengthBeforeTruncate)
            put("maxWords", maxWords)
            put("abbreviateLongNames", abbreviateLongNames)
            put("gelarFormat", gelarFormat)
            put("nipPrefix", nipPrefix)
            put("useRemotePdfTemplate", useRemotePdfTemplate)
            put("remotePdfTimeoutMs", remotePdfTimeoutMs)
            put("kopHeaderLine1", kopHeaderLine1)
            put("kopHeaderLine2", kopHeaderLine2)
            put("kopHeaderLine3", kopHeaderLine3)
            put("kopAddressLine", kopAddressLine)
            put("footerText", footerText)
            put("version", version)
            put("updatedAt", updatedAt)
            put("source", source)

            val sigArr = JSONArray()
            signatories.forEach { sigArr.put(it.toJson()) }
            put("signatories", sigArr)

            val offArr = JSONArray()
            officers.forEach { offArr.put(it.toJson()) }
            put("officers", offArr)
        }
        return root.toString()
    }

    companion object {
        const val SOURCE_API = "API"
        const val SOURCE_FIRESTORE = "FIRESTORE"
        const val SOURCE_CACHE = "CACHE"
        const val SOURCE_DEFAULT = "DEFAULT"

        const val SLOT_KOORD_ADMIN = "koord_admin"
        const val SLOT_MANAJER_PUSDALOPS = "manajer_pusdalops"
        const val SLOT_PENGAWAS = "pengawas"

        const val DEFAULT_FONT_SIZE = 9.0f
        const val DEFAULT_MIN_FONT_SIZE = 5.5f
        const val DEFAULT_BODY_FONT_SIZE = 8.0f
        const val MIN_ALLOWED_FONT_SIZE = 6.5f
        const val MAX_ALLOWED_FONT_SIZE = 14.0f
        const val DEFAULT_MAX_LENGTH_BEFORE_TRUNCATE = 135
        const val DEFAULT_MAX_WORDS = 2
        const val DEFAULT_NIP_PREFIX = "NIP."
        const val DEFAULT_REMOTE_PDF_TIMEOUT_MS = 8_000L

        // Kop Surat Standar Resmi Aplikasi SIPAGAR KITA (mengacu pada standar Kop Surat Tugas Relawan)
        const val DEFAULT_KOP_LINE_1 = "PEMERINTAH KABUPATEN MUKOMUKO"
        const val DEFAULT_KOP_LINE_2 = "BADAN PENANGGULANGAN BENCANA DAERAH"
        const val DEFAULT_KOP_LINE_3 = "PUSAT PENGENDALIAN OPERASI PENANGGULANGAN BENCANA (PUSDALOPS-PB)"
        const val DEFAULT_KOP_ADDRESS =
            "Jl. Imam Bonjol Komplek Perkantoran Pemda Kab. Mukomuko, Prov. Bengkulu 38365"
        const val DEFAULT_FOOTER_TEXT =
            "Dicetak melalui Aplikasi SIPAGAR KITA - Pusdalops PB BPBD Kabupaten Mukomuko"

        private const val LEGACY_DEFAULT_KOP_LINE_2 = "BADAN PENANGGULANGAN BENCANA DAERAH (BPBD)"
        private const val LEGACY_DEFAULT_KOP_ADDRESS =
            "Jl. Danau Nibung, Kompleks Perkantoran Pemkab Mukomuko • Call Center Darurat 117 • Sistem SIPAGAR KITA"

        private val PREFIX_TITLE_TOKENS = setOf(
            "dr", "dr.", "drs", "drs.", "dra", "dra.", "ir", "ir.",
            "prof", "prof.", "h", "h.", "hj", "hj.", "kh", "kh.",
            "akbp", "kompol", "iptu", "ipda", "kolonel", "letkol", "mayor", "kapten"
        )

        /**
         * Slot struktur jabatan standar (tanpa hardcode nama personil — nama & NIP diisi dari data server/roster).
         */
        fun defaultSignatorySlots(): List<RemoteSignatoryConfig> = listOf(
            RemoteSignatoryConfig(
                slotKey = SLOT_KOORD_ADMIN,
                roleLine1 = "Koord. Administrasi",
                roleLine2 = "Pusdalops-PB"
            ),
            RemoteSignatoryConfig(
                slotKey = SLOT_MANAJER_PUSDALOPS,
                roleLine1 = "Manajer Pusdalops PB",
                roleLine2 = "BPBD Mukomuko"
            ),
            RemoteSignatoryConfig(
                slotKey = SLOT_PENGAWAS,
                roleLine1 = "Pengawas",
                roleLine2 = "Kepala Pelaksana BPBD Mukomuko"
            )
        )

        private fun splitPrefixTitles(leftPart: String): Pair<String, String> {
            val tokens = leftPart.split(" ").filter { it.isNotBlank() }
            if (tokens.size <= 1) return "" to leftPart
            val prefixList = mutableListOf<String>()
            var idx = 0
            while (idx < tokens.size - 1) {
                val lower = tokens[idx].lowercase(Locale.ROOT)
                if (lower in PREFIX_TITLE_TOKENS) {
                    prefixList.add(tokens[idx])
                    idx++
                } else {
                    break
                }
            }
            val core = tokens.subList(idx, tokens.size).joinToString(" ")
            return prefixList.joinToString(" ") to core
        }

        private fun toTitleCase(input: String): String {
            return input.lowercase(Locale.ROOT)
                .split(" ")
                .filter { it.isNotBlank() }
                .joinToString(" ") { word ->
                    word.replaceFirstChar { ch ->
                        if (ch.isLowerCase()) ch.titlecase(Locale.ROOT) else ch.toString()
                    }
                }
        }

        /**
         * Memparsing payload JSON dari `GET /document-settings` atau cache lokal.
         * Mendukung baik camelCase (`fontSize`, `maxLengthBeforeTruncate`, `gelarFormat`)
         * maupun snake_case (`font_size`, `max_length_before_truncate`, `gelar_format`)
         * serta bungkus `{ "data": { ... } }`.
         */
        fun fromJson(rawJson: String, source: String = SOURCE_API): DocumentSettings? {
            if (rawJson.isBlank()) return null
            return try {
                val parsed = JSONObject(rawJson)
                val obj = parsed.optJSONObject("data") ?: parsed

                fun optFloat(camel: String, snake: String, defaultValue: Float): Float {
                    return when {
                        obj.has(camel) && !obj.isNull(camel) -> obj.optDouble(camel, defaultValue.toDouble()).toFloat()
                        obj.has(snake) && !obj.isNull(snake) -> obj.optDouble(snake, defaultValue.toDouble()).toFloat()
                        else -> defaultValue
                    }
                }

                fun optInt(camel: String, snake: String, defaultValue: Int): Int {
                    return when {
                        obj.has(camel) && !obj.isNull(camel) -> obj.optInt(camel, defaultValue)
                        obj.has(snake) && !obj.isNull(snake) -> obj.optInt(snake, defaultValue)
                        else -> defaultValue
                    }
                }

                fun optLong(camel: String, snake: String, defaultValue: Long): Long {
                    return when {
                        obj.has(camel) && !obj.isNull(camel) -> obj.optLong(camel, defaultValue)
                        obj.has(snake) && !obj.isNull(snake) -> obj.optLong(snake, defaultValue)
                        else -> defaultValue
                    }
                }

                fun optBool(camel: String, snake: String, defaultValue: Boolean): Boolean {
                    return when {
                        obj.has(camel) && !obj.isNull(camel) -> obj.optBoolean(camel, defaultValue)
                        obj.has(snake) && !obj.isNull(snake) -> obj.optBoolean(snake, defaultValue)
                        else -> defaultValue
                    }
                }

                fun optStr(camel: String, snake: String, defaultValue: String): String {
                    val v = obj.optString(camel).ifBlank { obj.optString(snake) }.trim()
                    return v.ifBlank { defaultValue }
                }

                val sigList = mutableListOf<RemoteSignatoryConfig>()
                val sigArray = obj.optJSONArray("signatories")
                if (sigArray != null) {
                    for (i in 0 until sigArray.length()) {
                        val item = sigArray.optJSONObject(i) ?: continue
                        RemoteSignatoryConfig.fromJson(item)?.let { sigList.add(it) }
                    }
                }

                val officerList = mutableListOf<RemoteOfficerRecord>()
                val offArray = obj.optJSONArray("officers")
                if (offArray != null) {
                    for (i in 0 until offArray.length()) {
                        val item = offArray.optJSONObject(i) ?: continue
                        RemoteOfficerRecord.fromJson(item)?.let { officerList.add(it) }
                    }
                }

                DocumentSettings(
                    fontSize = optFloat("fontSize", "font_size", DEFAULT_FONT_SIZE),
                    minFontSize = optFloat("minFontSize", "min_font_size", DEFAULT_MIN_FONT_SIZE),
                    bodyFontSize = optFloat("bodyFontSize", "body_font_size", DEFAULT_BODY_FONT_SIZE),
                    maxLengthBeforeTruncate = optInt(
                        "maxLengthBeforeTruncate",
                        "max_length_before_truncate",
                        DEFAULT_MAX_LENGTH_BEFORE_TRUNCATE
                    ),
                    maxWords = optInt("maxWords", "max_words", DEFAULT_MAX_WORDS),
                    abbreviateLongNames = optBool("abbreviateLongNames", "abbreviate_long_names", false),
                    gelarFormat = optStr("gelarFormat", "gelar_format", GelarFormat.FULL),
                    nipPrefix = optStr("nipPrefix", "nip_prefix", DEFAULT_NIP_PREFIX),
                    useRemotePdfTemplate = optBool("useRemotePdfTemplate", "use_remote_pdf_template", true),
                    remotePdfTimeoutMs = optLong("remotePdfTimeoutMs", "remote_pdf_timeout_ms", DEFAULT_REMOTE_PDF_TIMEOUT_MS),
                    kopHeaderLine1 = optStr("kopHeaderLine1", "kop_header_line_1", DEFAULT_KOP_LINE_1),
                    kopHeaderLine2 = optStr("kopHeaderLine2", "kop_header_line_2", DEFAULT_KOP_LINE_2),
                    kopHeaderLine3 = optStr("kopHeaderLine3", "kop_header_line_3", DEFAULT_KOP_LINE_3),
                    kopAddressLine = optStr("kopAddressLine", "kop_address_line", DEFAULT_KOP_ADDRESS),
                    footerText = optStr("footerText", "footer_text", DEFAULT_FOOTER_TEXT),
                    signatories = sigList.ifEmpty { defaultSignatorySlots() },
                    officers = officerList,
                    version = optInt("version", "version", 1),
                    updatedAt = optLong("updatedAt", "updated_at", System.currentTimeMillis()),
                    source = source
                ).clamped()
            } catch (_: Exception) {
                null
            }
        }

        /**
         * Memparsing map dokumen Firestore `app_settings/document_settings` menjadi `DocumentSettings`.
         */
        fun fromFirestoreMap(map: Map<String, Any>): DocumentSettings? {
            return try {
                val jsonObject = JSONObject(map)
                fromJson(jsonObject.toString(), source = SOURCE_FIRESTORE)
            } catch (_: Exception) {
                null
            }
        }

        /**
         * Memparsing respons JSON dari endpoint `GET /officers` (array langsung atau `{ "data": [...] }` / `{ "officers": [...] }`).
         */
        fun parseOfficersJson(rawJson: String): List<RemoteOfficerRecord> {
            if (rawJson.isBlank()) return emptyList()
            return try {
                val trimmed = rawJson.trim()
                val arr: JSONArray = when {
                    trimmed.startsWith("[") -> JSONArray(trimmed)
                    else -> {
                        val root = JSONObject(trimmed)
                        root.optJSONArray("officers")
                            ?: root.optJSONArray("data")
                            ?: JSONArray()
                    }
                }
                val result = mutableListOf<RemoteOfficerRecord>()
                for (i in 0 until arr.length()) {
                    val item = arr.optJSONObject(i) ?: continue
                    RemoteOfficerRecord.fromJson(item)?.let { result.add(it) }
                }
                result
            } catch (_: Exception) {
                emptyList()
            }
        }
    }
}
