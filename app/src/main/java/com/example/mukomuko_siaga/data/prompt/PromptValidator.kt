package com.example.mukomuko_siaga.data.prompt

object PromptValidator {

    sealed interface Result {
        data object Valid : Result
        data class Invalid(val reasons: List<String>) : Result
    }

    fun validate(name: String, prompt: String): Result {
        val reasons = mutableListOf<String>()

        if (!prompt.contains(PromptMarkers.SAFE_AREA)) {
            reasons += "Marker SAFE_AREA tidak ditemukan"
        }
        if (!prompt.contains(PromptMarkers.NO_TEXT)) {
            reasons += "Marker NO_TEXT tidak ditemukan"
        }
        if (!prompt.contains(PromptMarkers.REGION, ignoreCase = true)) {
            reasons += "Konteks wilayah '${PromptMarkers.REGION}' tidak ditemukan"
        }
        PromptMarkers.FORBIDDEN_TOKENS.forEach { token ->
            if (prompt.contains(token, ignoreCase = true)) {
                reasons += "Mengandung token terlarang: '$token'"
            }
        }
        if (prompt.length < 200) {
            reasons += "Terlalu pendek (<200 karakter), kemungkinan terpotong"
        }
        if (prompt.contains("  ")) {
            reasons += "Mengandung spasi ganda (indentasi trimIndent bermasalah)"
        }

        return if (reasons.isEmpty()) Result.Valid else Result.Invalid(reasons)
    }

    /** Validasi massal; lempar exception jika ada yang gagal. */
    fun validateAll(entries: Map<String, String>) {
        val failures = entries.mapNotNull { (name, prompt) ->
            when (val result = validate(name, prompt)) {
                is Result.Invalid -> "$name:\n  - ${result.reasons.joinToString("\n  - ")}"
                Result.Valid -> null
            }
        }
        check(failures.isEmpty()) {
            "Template prompt gagal validasi:\n\n${failures.joinToString("\n\n")}"
        }
    }
}
