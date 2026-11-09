package id.go.mukomuko.sipagarkita.feature.pesanvisual.config

object PesanVisualConfig {

    // ── Identitas instansi ──
    const val BPBD_NAME = "BPBD Kabupaten Mukomuko"
    const val PUSDALOPS_NAME = "Pusdalops-PB Kabupaten Mukomuko"

    // ── Sumber aplikasi ──
    const val APP_SOURCE = "SIPAGAR KITA"
    const val APP_SOURCE_FONT_SP = 10

    // ── Jalur darurat ──
    const val CALL_CENTER = "117"
    const val CALL_CENTER_LABEL = "Call Center 117 (Bebas Pulsa)"

    // ── Logo (overlay di Compose) ──
    const val SHOW_LAMBANG_MUKOMUKO = false
    const val LOGO_SIZE_DP = 48

    // ── Footer gambar (overlay) ──
    const val FOOTER_IMAGE_LINE1 = "BPBD Kabupaten Mukomuko"
    const val FOOTER_IMAGE_LINE2 = "Pusdalops-PB · 117 (Bebas Pulsa)"

    // ── Baris caption ──
    fun callCenterCaptionLine(): String =
        "📞 Darurat? Hubungi *Pusdalops-PB Kabupaten Mukomuko* " +
        "via Call Center 117 (Bebas Pulsa)"

    fun callCenterShortLine(): String = "Pusdalops-PB · 117 (Bebas Pulsa)"

    fun appSourceCaptionLine(): String =
        "📱 *SIPAGAR KITA* · BPBD Kabupaten Mukomuko"
}
