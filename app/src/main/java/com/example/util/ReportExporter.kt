package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.model.CitizenDisasterReport
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Helper untuk mengekspor laporan bencana warga ke format PDF dan CSV.
 * Digunakan oleh Petugas Pusdalops BPBD Kabupaten Mukomuko untuk arsip digital
 * dan analisis data spreadsheet (Excel).
 */
object ReportExporter {

    /**
     * Menghasilkan dokumen PDF resmi A4 berisi rekapitulasi laporan bencana tervalidasi.
     */
    fun exportToPdf(
        context: Context,
        reports: List<CitizenDisasterReport>,
        officerName: String
    ) {
        if (reports.isEmpty()) {
            Toast.makeText(context, "Tidak ada data laporan untuk diekspor ke PDF", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val pdfDoc = PdfDocument()
            val pageWidth = 595 // A4 standard width in points
            val pageHeight = 842 // A4 standard height in points
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdfDoc.startPage(pageInfo)
            val canvas = page.canvas

            val titlePaint = Paint().apply {
                color = Color.rgb(15, 23, 42) // Slate 900
                textSize = 13f
                isFakeBoldText = true
                textAlign = Paint.Align.CENTER
            }

            val subtitlePaint = Paint().apply {
                color = Color.rgb(21, 128, 61) // Green 700
                textSize = 10f
                isFakeBoldText = true
                textAlign = Paint.Align.CENTER
            }

            val textPaint = Paint().apply {
                color = Color.rgb(30, 41, 59)
                textSize = 8.5f
            }

            val boldPaint = Paint().apply {
                color = Color.rgb(15, 23, 42)
                textSize = 8.5f
                isFakeBoldText = true
            }

            val linePaint = Paint().apply {
                color = Color.rgb(203, 213, 225)
                strokeWidth = 1f
            }

            val headerBoxPaint = Paint().apply {
                color = Color.rgb(241, 245, 249)
            }

            val sdf = SimpleDateFormat("dd MMMM yyyy, HH:mm 'WIB'", Locale("id", "ID"))
            val printDate = sdf.format(Date())

            var y = 45f

            // Official Header
            canvas.drawText("PEMERINTAH KABUPATEN MUKOMUKO", (pageWidth / 2).toFloat(), y, titlePaint)
            y += 15f
            canvas.drawText("BADAN PENANGGULANGAN BENCANA DAERAH (BPBD)", (pageWidth / 2).toFloat(), y, titlePaint)
            y += 14f
            canvas.drawText("Pusat Pengendalian Operasi (Pusdalops) & Tim Reaksi Cepat (TRC)", (pageWidth / 2).toFloat(), y, subtitlePaint)
            y += 12f
            canvas.drawLine(36f, y, (pageWidth - 36).toFloat(), y, linePaint)
            y += 15f

            // Document Title
            canvas.drawText("REKAPITULASI RESMI LAPORAN BENCANA WARGA TERVALIDASI", (pageWidth / 2).toFloat(), y, titlePaint)
            y += 14f
            textPaint.textAlign = Paint.Align.LEFT
            canvas.drawText("Waktu Cetak: $printDate  |  Petugas Validasi: $officerName", 36f, y, textPaint)
            y += 12f

            // Summary KPI Box
            canvas.drawRect(36f, y, (pageWidth - 36).toFloat(), y + 26f, headerBoxPaint)
            val totalReports = reports.size
            val totalDead = reports.sumOf { it.casualtiesDead }
            val totalInjured = reports.sumOf { it.casualtiesInjured }
            val totalMissing = reports.sumOf { it.casualtiesMissing }
            canvas.drawText(
                "Total: $totalReports Laporan Tervalidasi  |  Korban: $totalDead Meninggal, $totalInjured Luka-Luka, $totalMissing Hilang",
                46f,
                y + 17f,
                boldPaint
            )
            y += 36f

            // Table Columns: No (25), ID (75), Bencana (105), Kecamatan/Desa (115), Status (65), Catatan (135)
            canvas.drawRect(36f, y, (pageWidth - 36).toFloat(), y + 18f, headerBoxPaint)
            canvas.drawText("No", 42f, y + 12f, boldPaint)
            canvas.drawText("ID Laporan", 68f, y + 12f, boldPaint)
            canvas.drawText("Bencana", 145f, y + 12f, boldPaint)
            canvas.drawText("Wilayah", 245f, y + 12f, boldPaint)
            canvas.drawText("Status", 370f, y + 12f, boldPaint)
            canvas.drawText("Catatan Tindak Lanjut TRC", 445f, y + 12f, boldPaint)
            y += 24f

            // Render Rows (up to fit A4 page cleanly)
            reports.take(18).forEachIndexed { idx, rep ->
                canvas.drawText("${idx + 1}.", 42f, y, textPaint)
                canvas.drawText("#${rep.id.takeLast(7)}", 68f, y, boldPaint)
                canvas.drawText(rep.disasterType.take(18), 145f, y, textPaint)
                canvas.drawText("${rep.village.take(12)}, ${rep.district.take(10)}", 245f, y, textPaint)
                canvas.drawText(rep.status, 370f, y, boldPaint)
                val note = if (rep.additionalNotes.isNotBlank()) rep.additionalNotes.take(24) else "-"
                canvas.drawText(note, 445f, y, textPaint)

                y += 6f
                canvas.drawLine(36f, y, (pageWidth - 36).toFloat(), y, linePaint)
                y += 16f
            }

            // Signature Footer
            y = (pageHeight - 90).toFloat()
            canvas.drawText("Mukomuko, $printDate", (pageWidth - 190).toFloat(), y, textPaint)
            y += 12f
            canvas.drawText("Petugas Jaga Siaga Pusdalops BPBD,", (pageWidth - 190).toFloat(), y, textPaint)
            y += 36f
            canvas.drawText(officerName, (pageWidth - 190).toFloat(), y, boldPaint)
            y += 10f
            canvas.drawText("NIP/ID: TRC-MMK-117", (pageWidth - 190).toFloat(), y, textPaint)

            pdfDoc.finishPage(page)

            // Save PDF to cache directory
            val pdfFile = File(context.cacheDir, "Laporan_Bencana_Pusdalops_BPBD_Mukomuko.pdf")
            FileOutputStream(pdfFile).use { out ->
                pdfDoc.writeTo(out)
            }
            pdfDoc.close()

            // Open / Share PDF via Intent
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Laporan Resmi Bencana Pusdalops BPBD Mukomuko")
                putExtra(Intent.EXTRA_TEXT, "Terlampir dokumen PDF rekapitulasi resmi laporan bencana warga tervalidasi Pusdalops BPBD Kabupaten Mukomuko.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(Intent.createChooser(shareIntent, "Buka / Bagikan Berkas PDF"))
            Toast.makeText(context, "Dokumen PDF berhasil dibuat & siap dibagikan!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal membuat PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Menghasilkan berkas CSV agar data laporan warga dapat diolah di Microsoft Excel / Google Sheets.
     */
    fun exportToCsv(
        context: Context,
        reports: List<CitizenDisasterReport>
    ) {
        if (reports.isEmpty()) {
            Toast.makeText(context, "Tidak ada data laporan untuk diekspor ke CSV", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val csvFile = File(context.cacheDir, "Laporan_Bencana_Pusdalops_Mukomuko.csv")
            csvFile.bufferedWriter().use { writer ->
                // CSV Header
                writer.write("No,ID_Laporan,Jenis_Bencana,Waktu_Kejadian,Kecamatan,Desa,Detail_Lokasi,Latitude,Longitude,Akurasi_GPS_m,Korban_Meninggal,Korban_Luka,Korban_Hilang,Status,Catatan_Tindak_Lanjut_TRC,Nama_Pelapor,Nomor_Telepon\n")

                // CSV Data Rows
                reports.forEachIndexed { idx, rep ->
                    val cleanNotes = rep.additionalNotes.replace(",", ";").replace("\n", " ")
                    val cleanLocation = rep.incidentLocation.replace(",", ";").replace("\n", " ")
                    val row = listOf(
                        "${idx + 1}",
                        rep.id,
                        rep.disasterType,
                        rep.incidentTime,
                        rep.district,
                        rep.village,
                        cleanLocation,
                        "${rep.latitude}",
                        "${rep.longitude}",
                        "${rep.gpsAccuracyMeters ?: 0f}",
                        "${rep.casualtiesDead}",
                        "${rep.casualtiesInjured}",
                        "${rep.casualtiesMissing}",
                        rep.status,
                        cleanNotes,
                        rep.reporterName,
                        rep.reporterPhone
                    ).joinToString(",")
                    writer.write(row + "\n")
                }
            }

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", csvFile)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Data CSV Laporan Bencana Pusdalops Mukomuko")
                putExtra(Intent.EXTRA_TEXT, "Terlampir berkas data CSV laporan bencana warga Kabupaten Mukomuko untuk pengolahan spreadsheet / Excel.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(Intent.createChooser(shareIntent, "Buka / Bagikan Berkas CSV"))
            Toast.makeText(context, "Berkas CSV berhasil dibuat & siap diolah di Excel!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal membuat CSV: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    data class DistrictWeatherExportItem(
        val districtName: String,
        val category: String,
        val temperature: Double,
        val feelsLike: Double,
        val humidity: Int,
        val windSpeed: Double,
        val windDirection: String = "Barat Daya (SW)",
        val precipitation: Double = 0.0,
        val weatherDesc: String
    )

    /**
     * Menghasilkan dokumen PDF resmi A4 berisi Peta Cuaca & Iklim 15 Kecamatan Kabupaten Mukomuko,
     * mencakup overlay Curah Hujan Radar, Suhu Udara, dan Kecepatan Angin untuk diseminasi informasi publik.
     */
    fun exportWeatherMapToPdf(
        context: Context,
        items: List<DistrictWeatherExportItem>,
        activeLayerName: String
    ): File {
        val pdfDoc = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDoc.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 12f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }

        val subtitlePaint = Paint().apply {
            color = Color.rgb(194, 65, 12)
            textSize = 9.5f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }

        val textPaint = Paint().apply {
            color = Color.rgb(51, 65, 85)
            textSize = 8f
            isAntiAlias = true
        }

        val boldTextPaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 8f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val linePaint = Paint().apply {
            color = Color.rgb(203, 213, 225)
            strokeWidth = 0.8f
        }

        val headerBgPaint = Paint().apply {
            color = Color.rgb(241, 245, 249)
        }

        var y = 35f
        val margin = 30f

        // Kop Lembaga
        canvas.drawText("PEMERINTAH KABUPATEN MUKOMUKO", pageWidth / 2f, y, titlePaint)
        y += 14f
        canvas.drawText("BADAN PENANGGULANGAN BENCANA DAERAH (BPBD)", pageWidth / 2f, y, titlePaint)
        y += 13f
        canvas.drawText("PUSAT PENGENDALIAN OPERASI PENANGGULANGAN BENCANA (PUSDALOPS-PB)", pageWidth / 2f, y, subtitlePaint)
        y += 11f
        canvas.drawText("Jl. Danau Nibung, Kompleks Perkantoran Pemkab Mukomuko - Call Center 117 (Bebas Pulsa)", pageWidth / 2f, y, textPaint.apply { textAlign = Paint.Align.CENTER })
        textPaint.textAlign = Paint.Align.LEFT

        y += 12f
        canvas.drawLine(margin, y, pageWidth - margin, y, linePaint.apply { strokeWidth = 2f; color = Color.rgb(15, 23, 42) })
        y += 3f
        canvas.drawLine(margin, y, pageWidth - margin, y, linePaint.apply { strokeWidth = 0.8f; color = Color.rgb(100, 116, 139) })
        y += 18f

        // Judul Dokumen
        val docTitlePaint = Paint().apply {
            color = Color.rgb(194, 65, 12)
            textSize = 11f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("BULETIN PETA CUACA & PERINGATAN DINI 15 KECAMATAN", pageWidth / 2f, y, docTitlePaint)
        y += 14f

        val sdf = SimpleDateFormat("EEEE, dd MMMM yyyy - HH:mm 'WIB'", Locale("id", "ID"))
        val currentTimeStr = sdf.format(Date())
        val metaPaint = Paint().apply {
            color = Color.rgb(71, 85, 105)
            textSize = 8.5f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Waktu Penerbitan: $currentTimeStr | Mode Overlay Aktif: $activeLayerName", pageWidth / 2f, y, metaPaint)
        y += 18f

        // Kotak Ringkasan Ekstrem Cuaca
        val maxRain = items.maxByOrNull { it.precipitation }
        val maxTemp = items.maxByOrNull { it.temperature }
        val minTemp = items.minByOrNull { it.temperature }
        val maxWind = items.maxByOrNull { it.windSpeed }

        val summaryRect = android.graphics.RectF(margin, y, pageWidth - margin, y + 42f)
        canvas.drawRoundRect(summaryRect, 6f, 6f, Paint().apply { color = Color.rgb(254, 242, 242) })
        canvas.drawRoundRect(summaryRect, 6f, 6f, Paint().apply { color = Color.rgb(252, 165, 165); style = Paint.Style.STROKE; strokeWidth = 1f })

        canvas.drawText("RINGKASAN PARAMETER METEOROLOGI KABUPATEN MUKOMUKO", margin + 10f, y + 13f, boldTextPaint.apply { color = Color.rgb(153, 27, 27) })
        canvas.drawText("• Curah Hujan Tertinggi: ${maxRain?.districtName ?: "-"} (${maxRain?.precipitation ?: 0.0} mm/j)", margin + 10f, y + 25f, textPaint)
        canvas.drawText("• Suhu Ekstrem: Tertinggi ${maxTemp?.districtName} (${maxTemp?.temperature ?: 0.0}°C) | Tersejuk ${minTemp?.districtName} (${minTemp?.temperature ?: 0.0}°C)", margin + 10f, y + 36f, textPaint)
        canvas.drawText("• Kecepatan Angin Maksimal: ${maxWind?.districtName} (${maxWind?.windSpeed?.toInt() ?: 0} km/j - ${maxWind?.windDirection})", margin + 280f, y + 25f, textPaint)
        canvas.drawText("• Sumber Data: Radar BMKG Fatmawati Bengkulu & Jaringan Sensor Telemetri", margin + 280f, y + 36f, textPaint)

        y += 54f

        // Tabel 15 Kecamatan
        val colNo = margin + 5f
        val colKec = margin + 24f
        val colKat = margin + 130f
        val colHujan = margin + 205f
        val colSuhu = margin + 275f
        val colTerasa = margin + 335f
        val colAngin = margin + 395f
        val colLembap = margin + 465f
        val colKondisi = margin + 505f

        // Table Header
        val thRect = android.graphics.RectF(margin, y, pageWidth - margin, y + 20f)
        canvas.drawRect(thRect, headerBgPaint)
        canvas.drawLine(margin, y + 20f, pageWidth - margin, y + 20f, linePaint)

        boldTextPaint.color = Color.rgb(15, 23, 42)
        canvas.drawText("No", colNo, y + 13f, boldTextPaint)
        canvas.drawText("Kecamatan", colKec, y + 13f, boldTextPaint)
        canvas.drawText("Karakter Wilayah", colKat, y + 13f, boldTextPaint)
        canvas.drawText("Hujan (mm/j)", colHujan, y + 13f, boldTextPaint)
        canvas.drawText("Suhu (°C)", colSuhu, y + 13f, boldTextPaint)
        canvas.drawText("Terasa", colTerasa, y + 13f, boldTextPaint)
        canvas.drawText("Angin (km/j)", colAngin, y + 13f, boldTextPaint)
        canvas.drawText("RH (%)", colLembap, y + 13f, boldTextPaint)
        canvas.drawText("Kondisi", colKondisi, y + 13f, boldTextPaint)
        y += 20f

        // Table Rows (15 Kecamatan)
        items.forEachIndexed { index, item ->
            val rowY = y + 14f
            if (index % 2 == 1) {
                canvas.drawRect(android.graphics.RectF(margin, y, pageWidth - margin, y + 18f), Paint().apply { color = Color.rgb(248, 250, 252) })
            }

            canvas.drawText("${index + 1}", colNo, rowY, textPaint)
            canvas.drawText(item.districtName, colKec, rowY, boldTextPaint)
            canvas.drawText(item.category.take(16), colKat, rowY, textPaint)

            // Hujan highlight
            val rainPaint = if (item.precipitation >= 5.0) boldTextPaint.apply { color = Color.rgb(220, 38, 38) } else textPaint.apply { color = Color.rgb(3, 105, 161) }
            canvas.drawText("%.1f".format(item.precipitation), colHujan, rowY, rainPaint)

            // Suhu
            canvas.drawText("%.1f°C".format(item.temperature), colSuhu, rowY, textPaint.apply { color = Color.rgb(51, 65, 85) })
            canvas.drawText("%.1f°".format(item.feelsLike), colTerasa, rowY, textPaint)

            // Angin
            canvas.drawText("${item.windSpeed.toInt()}", colAngin, rowY, textPaint)
            // Kelembapan
            canvas.drawText("${item.humidity}%", colLembap, rowY, textPaint)
            // Kondisi
            canvas.drawText(item.weatherDesc.take(12), colKondisi, rowY, textPaint)

            y += 18f
            canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
        }

        y += 16f

        // Catatan SOP Peringatan Dini
        val noteRect = android.graphics.RectF(margin, y, pageWidth - margin, y + 42f)
        canvas.drawRoundRect(noteRect, 6f, 6f, Paint().apply { color = Color.rgb(255, 251, 235) })
        canvas.drawRoundRect(noteRect, 6f, 6f, Paint().apply { color = Color.rgb(253, 230, 138); style = Paint.Style.STROKE; strokeWidth = 1f })

        canvas.drawText("PETUNJUK KESIAPSIAGAAN & DISPOSISI KEBENCANAAN:", margin + 10f, y + 13f, boldTextPaint.apply { color = Color.rgb(180, 83, 9) })
        canvas.drawText("1. Wilayah pesisir (Kota Mukomuko, Teramang Jaya, Ipuh, Air Rami) mewaspadai pasang surut air laut dan angin kencang.", margin + 10f, y + 24f, textPaint.apply { color = Color.rgb(120, 53, 15) })
        canvas.drawText("2. Wilayah perbukitan (V Koto, Malin Deman, Selagan Raya) mewaspadai potensi longsor bila curah hujan >15 mm/jam berturut-turut.", margin + 10f, y + 35f, textPaint)

        y += 56f

        // Tanda tangan Pengesahan Petugas Pusdalops
        val sigX = pageWidth - margin - 180f
        canvas.drawText("Mukomuko, ${SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")).format(Date())}", sigX, y, textPaint.apply { color = Color.rgb(15, 23, 42) })
        y += 12f
        canvas.drawText("Petugas Operator Siaga Pusdalops-PB,", sigX, y, boldTextPaint)
        y += 42f
        canvas.drawText("( PUSDALOPS-PB BPBD MUKOMUKO )", sigX, y, boldTextPaint)
        y += 11f
        canvas.drawText("Sistem Informasi Cuaca SiPagarKita Terintegrasi", sigX, y, textPaint)

        pdfDoc.finishPage(page)

        val outputFile = File(context.cacheDir, "Peta_Cuaca_15_Kecamatan_Mukomuko_${System.currentTimeMillis()}.pdf")
        FileOutputStream(outputFile).use { out ->
            pdfDoc.writeTo(out)
        }
        pdfDoc.close()

        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outputFile)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Peta Cuaca 15 Kecamatan Mukomuko")
                putExtra(Intent.EXTRA_TEXT, "Terlampir dokumen PDF Buletin Peta Cuaca & Radar BMKG 15 Kecamatan Kabupaten Mukomuko.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(Intent.createChooser(shareIntent, "Buka / Bagikan Peta Cuaca PDF"))
            Toast.makeText(context, "Dokumen PDF Peta Cuaca berhasil diterbitkan!", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {}

        return outputFile
    }
}
