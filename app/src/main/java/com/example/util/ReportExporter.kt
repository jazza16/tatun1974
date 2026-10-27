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
}
