package id.go.mukomuko.sipagarkita.feature.pesanvisual.util

import id.go.mukomuko.sipagarkita.feature.pesanvisual.config.PesanVisualConfig
import id.go.mukomuko.sipagarkita.feature.pesanvisual.context.EarlyWarning
import id.go.mukomuko.sipagarkita.feature.pesanvisual.context.SituationalContext
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object ContextualCaptionBuilder {

    private fun severityRank(severity: String): Int = when (severity.trim().uppercase(Locale.ROOT)) {
        "AWAS" -> 1
        "SIAGA" -> 2
        "WASPADA" -> 3
        else -> 4
    }

    fun build(
        disasterLabel: String,
        context: SituationalContext,
        now: Instant = Instant.now()
    ): String {
        val sb = StringBuilder()

        // Judul utama
        sb.appendLine("📢 *KESIAPSIAGAAN BENCANA — ${disasterLabel.uppercase(Locale.ROOT)}*")
        sb.appendLine("Kabupaten Mukomuko, Provinsi Bengkulu")
        sb.appendLine()

        if (context.isEmpty()) {
            // Fallback caption generik sederhana
            sb.appendLine("Tetap waspada, kenali potensi bahaya di lingkungan sekitar Anda, dan ikuti panduan kesiapsiagaan dari BPBD Kabupaten Mukomuko.")
            sb.appendLine("Pastikan jalur evakuasi keluarga telah disepakati bersama dan simpan kontak darurat pada gawai Anda.")
            sb.appendLine()
        } else {
            // 1. Peringatan Dini Aktif (isFresh, sort by severity)
            val freshWarnings = context.earlyWarnings
                .filter { it.isFresh(now) }
                .sortedBy { severityRank(it.severity) }

            if (freshWarnings.isNotEmpty()) {
                sb.appendLine("⚠️ *PERINGATAN DINI AKTIF*")
                freshWarnings.forEach { warning ->
                    sb.appendLine("• [${warning.severity.uppercase(Locale.ROOT)}] ${warning.type}: ${warning.summary} (Sumber: ${warning.source})")
                }
                sb.appendLine()
            }

            // 2. Cuaca
            context.weather?.let { weather ->
                sb.appendLine("🌧️ *KONDISI CUACA*")
                sb.appendLine("Prakiraan: ${weather.summary}")
                if (weather.affectedAreas.isNotEmpty()) {
                    sb.appendLine("Wilayah terdampak: ${weather.affectedAreas.joinToString(", ")}")
                }
                sb.appendLine("Sumber data: ${weather.source}")
                sb.appendLine()
            }

            // 3. Maritim (hanya jika bukan "Aman")
            context.maritime?.let { maritime ->
                if (!maritime.warningLevel.equals("Aman", ignoreCase = true)) {
                    sb.appendLine("🌊 *STATUS MARITIM & GELOMBANG*")
                    sb.appendLine("Tinggi gelombang: ${maritime.waveHeight}")
                    sb.appendLine("Tingkat peringatan: ${maritime.warningLevel}")
                    sb.appendLine("Sumber data: ${maritime.source}")
                    sb.appendLine()
                }
            }

            // 4. Gempa Terkini (<24 jam)
            context.earthquake?.let { eq ->
                if (eq.isRecent(now)) {
                    sb.appendLine("📍 *GEMPA BUMI TERKINI*")
                    sb.appendLine("Magnitudo: M ${eq.magnitude} | Kedalaman: ${eq.depthKm} km")
                    sb.appendLine("Lokasi: ${eq.location}")
                    sb.appendLine("Sumber data: ${eq.source}")
                    sb.appendLine()
                }
            }

            // 5. InaRISK Wilayah
            context.risk?.let { risk ->
                sb.appendLine("🗺️ *RISIKO WILAYAH (InaRISK BNPB)*")
                if (risk.highRiskAreas.isNotEmpty()) {
                    sb.appendLine("Risiko Tinggi: ${risk.highRiskAreas.joinToString(", ")}")
                }
                if (risk.mediumRiskAreas.isNotEmpty()) {
                    sb.appendLine("Risiko Sedang: ${risk.mediumRiskAreas.joinToString(", ")}")
                }
                sb.appendLine()
            }

            // 6. Jalur Evakuasi
            context.evacuation?.routes?.let { routes ->
                if (routes.isNotEmpty()) {
                    sb.appendLine("🚪 *JALUR EVAKUASI DIREKOMENDASIKAN*")
                    routes.forEach { route ->
                        val noteText = route.notes?.let { " ($it)" } ?: ""
                        sb.appendLine("• Dari ${route.fromArea} ➔ Titik Kumpul: ${route.toLocation}$noteText")
                    }
                    sb.appendLine()
                }
            }

            // Catatan mitigasi tambahan bila ada
            if (context.mitigationNotes.isNotEmpty()) {
                sb.appendLine("💡 *HIMBAUAN MITIGASI*")
                context.mitigationNotes.forEach { note ->
                    sb.appendLine("• $note")
                }
                sb.appendLine()
            }
        }

        // SELALU: Call Center
        sb.appendLine(PesanVisualConfig.callCenterCaptionLine())
        sb.appendLine()

        // SELALU: Sumber Aplikasi
        sb.appendLine(PesanVisualConfig.appSourceCaptionLine())

        // Timestamp
        val formatter = SimpleDateFormat("dd MMM yyyy, HH:mm 'WIB'", Locale("id", "ID")).apply {
            timeZone = TimeZone.getTimeZone("Asia/Jakarta")
        }
        val dateStr = formatter.format(Date.from(context.fetchedAt))
        sb.append("_Diperbarui: ${dateStr}_")

        return sb.toString()
    }
}
