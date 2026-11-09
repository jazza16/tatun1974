package com.example.mukomuko_siaga.data.prompt

object PromptMarkers {

    /** Wajib ada di setiap template. Menjamin sudut kanan atas kosong. */
    const val SAFE_AREA =
        "Leave the entire top-right corner empty and uncluttered " +
        "(no subject, no object, no text) as a safe area for institutional logos."

    /** Wajib ada di setiap template. Mencegah AI menulis teks/logo. */
    const val NO_TEXT =
        "Do not include any text, logos, or watermarks in the image."

    /** Konteks wilayah yang wajib muncul agar nuansa lokal terjaga. */
    const val REGION = "Mukomuko"

    /** Kata yang TIDAK boleh muncul (karena logo ditempel di Compose). */
    val FORBIDDEN_TOKENS = listOf("BPBD", "Lambang", "logo BPBD")
}
