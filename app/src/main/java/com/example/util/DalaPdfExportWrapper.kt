package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.repository.DocumentSettingsRepository
import com.example.model.DalaAssessmentState
import com.example.model.DocumentSettings
import com.example.model.UrgentNeedStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

/**
 * Wrapper Ekspor PDF Standar BNPB/BPBD untuk Modul Pasca Bencana / DALA (Data dan Informasi Bencana).
 *
 * Terintegrasi langsung dengan mesin PDF Data-Driven (`DocumentSettingsRepository`, `PdfTextAutoShrinkHelper`,
 * Kop Surat Resmi, Grafik Batang Kerusakan Rumah RB/RS/RR, Tabel Kebutuhan Mendesak, dan Blok Tanda Tangan
 * Piramida Terbalik).
 */
object DalaPdfExportWrapper {

    private const val A4_WIDTH = ReportExporter.A4_PAGE_WIDTH
    private const val A4_HEIGHT = ReportExporter.A4_PAGE_HEIGHT
    private const val MARGIN = ReportExporter.STANDARD_MARGIN

    suspend fun exportAndSharePdf(
        context: Context,
        assessment: DalaAssessmentState
    ): File? = withContext(Dispatchers.IO) {
        val settings = DocumentSettingsRepository.fetchDocumentSettings(context).clamped()
        val exportDir = File(context.cacheDir, "exported_reports").apply { if (!exists()) mkdirs() }
        val safeId = assessment.assessmentId.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val fileName = "Laporan_DALA_BNPB_${safeId}.pdf"
        val outFile = File(exportDir, fileName)

        val pdfDoc = PdfDocument()
        try {
            val pageInfo = PdfDocument.PageInfo.Builder(A4_WIDTH, A4_HEIGHT, 1).create()
            val page = pdfDoc.startPage(pageInfo)
            drawDalaBnpbReportPage(
                context = context,
                canvas = page.canvas,
                assessment = assessment,
                settings = settings
            )
            pdfDoc.finishPage(page)

            FileOutputStream(outFile).use { stream ->
                pdfDoc.writeTo(stream)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Gagal mengekspor PDF DALA: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
            return@withContext null
        } finally {
            try {
                pdfDoc.close()
            } catch (_: Exception) {}
        }

        // Simpan salinan ke folder Downloads pada Android 10+ (MediaStore zero-permission)
        var savedToDownloads = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        outFile.inputStream().use { ins -> ins.copyTo(os) }
                    }
                    savedToDownloads = true
                }
            } catch (_: Exception) {}
        }

        withContext(Dispatchers.Main) {
            try {
                val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    putExtra(Intent.EXTRA_SUBJECT, "Laporan Kaji Cepat DALA BPBD (${assessment.assessmentId})")
                    putExtra(
                        Intent.EXTRA_TEXT,
                        "Terlampir Laporan Pasca Bencana / DALA Standar BNPB untuk kejadian ${assessment.disasterTitle} (${assessment.district})."
                    )
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(Intent.createChooser(shareIntent, "Buka / Bagikan PDF DALA (Standar BNPB)"))
                val msg = if (savedToDownloads) {
                    "PDF DALA tersimpan di Downloads/$fileName & siap dibagikan!"
                } else {
                    "PDF DALA berhasil dibuat & siap dibagikan!"
                }
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "PDF dibuat, namun gagal membuka lembar berbagi: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
        outFile
    }

    /**
     * Merender halaman tunggal Laporan Kaji Cepat Pasca Bencana (DALA) standar BNPB ke Canvas A4.
     * Dapat dipanggil langsung untuk pengujian Robolectric / rendering preview.
     */
    fun drawDalaBnpbReportPage(
        context: Context,
        canvas: Canvas,
        assessment: DalaAssessmentState,
        settings: DocumentSettings = DocumentSettingsRepository.getCachedOrDefaultSettings(context)
    ) {
        val contentWidth = A4_WIDTH - (MARGIN * 2f)
        val resolvedOfficer = DocumentSettingsRepository.resolveOfficerIdentity(
            context = context,
            rawName = assessment.officerName,
            rawNip = assessment.officerNip,
            settings = settings
        )

        var y = ReportExporter.drawKopSuratResmi(
            context = context,
            canvas = canvas,
            pageWidth = A4_WIDTH,
            margin = MARGIN,
            pageNumber = 1,
            documentSettings = settings
        )

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(194, 65, 12) // BPBD Orange Dark
            textSize = 11.2f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(71, 85, 105)
            textSize = 7.8f
            textAlign = Paint.Align.CENTER
        }
        val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 9.0f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 41, 59)
            textSize = settings.bodyFontSize
        }
        val boldBodyPaint = Paint(bodyPaint).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(203, 213, 225)
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
        }
        val headerFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(255, 247, 237) // Warm Orange 50
            style = Paint.Style.FILL
        }

        // Judul & Metadata
        PdfTextAutoShrinkHelper.drawAutoShrinkText(
            canvas = canvas,
            text = "LAPORAN KAJI CEPAT PASCA BENCANA (DALA) — STANDAR BNPB",
            x = A4_WIDTH / 2f,
            y = y,
            maxWidth = contentWidth,
            basePaint = titlePaint,
            initialFontSize = titlePaint.textSize,
            minFontSize = 7.5f
        )
        y += 12f
        PdfTextAutoShrinkHelper.drawAutoShrinkText(
            canvas = canvas,
            text = "ID: ${assessment.assessmentId}  |  Kejadian: ${assessment.disasterTitle.ifBlank { "-" }}  |  Wilayah: ${assessment.district}",
            x = A4_WIDTH / 2f,
            y = y,
            maxWidth = contentWidth,
            basePaint = metaPaint,
            initialFontSize = 7.8f,
            minFontSize = settings.minFontSize
        )
        y += 10.5f
        PdfTextAutoShrinkHelper.drawAutoShrinkText(
            canvas = canvas,
            text = "Petugas: ${resolvedOfficer.formattedName} (${resolvedOfficer.nipLine})",
            x = A4_WIDTH / 2f,
            y = y,
            maxWidth = contentWidth,
            basePaint = metaPaint,
            initialFontSize = 7.8f,
            minFontSize = settings.minFontSize
        )
        y += 12f

        // BAGIAN I: KORBAN & PENGUNGSI
        PdfTextAutoShrinkHelper.drawPdfText(canvas, "I. KORBAN JIWA, PENGUNGSI & KELOMPOK RENTAN", MARGIN, y, sectionPaint)
        y += 6f
        val kpiBoxH = 34f
        val kpiRect = RectF(MARGIN, y, MARGIN + contentWidth, y + kpiBoxH)
        canvas.drawRoundRect(kpiRect, 5f, 5f, headerFillPaint)
        canvas.drawRoundRect(kpiRect, 5f, 5f, borderPaint)

        val kpiItems = listOf(
            "Meninggal" to "${assessment.casualties.dead} Jiwa",
            "Hilang" to "${assessment.casualties.missing} Jiwa",
            "Luka Berat" to "${assessment.casualties.heavyInjured} Jiwa",
            "Luka Ringan" to "${assessment.casualties.lightInjured} Jiwa",
            "Mengungsi" to "${assessment.casualties.totalEvacuees} Jiwa"
        )
        val colW = contentWidth / kpiItems.size
        kpiItems.forEachIndexed { i, (label, value) ->
            val cx = MARGIN + colW * (i + 0.5f)
            val lPaint = Paint(bodyPaint).apply { textAlign = Paint.Align.CENTER; textSize = 7.2f; color = Color.rgb(100, 116, 139) }
            val vPaint = Paint(boldBodyPaint).apply { textAlign = Paint.Align.CENTER; textSize = 9.0f; color = Color.rgb(194, 65, 12) }
            PdfTextAutoShrinkHelper.drawPdfText(canvas, label, cx, y + 12f, lPaint)
            PdfTextAutoShrinkHelper.drawPdfText(canvas, value, cx, y + 25f, vPaint)
        }
        y += kpiBoxH + 10f

        val sitesSummary = if (assessment.casualties.evacuationSites.isEmpty()) {
            "Belum ada titik pengungsian tercatat."
        } else {
            assessment.casualties.evacuationSites.joinToString(" • ") { "${it.locationName} (${it.evacueeCount} jiwa)" }
        }
        val vulnerableSummary = if (assessment.casualties.vulnerableGroups.isEmpty()) {
            "Tidak ada data kelompok rentan."
        } else {
            assessment.casualties.vulnerableGroups.joinToString(" • ") { "${it.category.label}: ${it.count}" }
        }
        PdfTextAutoShrinkHelper.drawAutoShrinkText(
            canvas = canvas,
            text = "Titik Pengungsian: $sitesSummary",
            x = MARGIN,
            y = y,
            maxWidth = contentWidth,
            basePaint = bodyPaint,
            initialFontSize = settings.bodyFontSize,
            minFontSize = settings.minFontSize,
            maxLengthBeforeTruncate = settings.maxLengthBeforeTruncate
        )
        y += 12f
        PdfTextAutoShrinkHelper.drawAutoShrinkText(
            canvas = canvas,
            text = "Kelompok Rentan: $vulnerableSummary",
            x = MARGIN,
            y = y,
            maxWidth = contentWidth,
            basePaint = bodyPaint,
            initialFontSize = settings.bodyFontSize,
            minFontSize = settings.minFontSize,
            maxLengthBeforeTruncate = settings.maxLengthBeforeTruncate
        )
        y += 16f

        // BAGIAN II: KERUSAKAN, KERUGIAN & GRAFIK BATANG RUMAH (RB / RS / RR)
        PdfTextAutoShrinkHelper.drawPdfText(canvas, "II. KERUSAKAN INFRASTRUKTUR, RUMAH & ESTIMASI KERUGIAN", MARGIN, y, sectionPaint)
        y += 8f

        val leftW = contentWidth * 0.56f
        val rightW = contentWidth - leftW - 10f
        val dmgBoxH = 98f

        // Kiri: Rincian Kerusakan
        val leftRect = RectF(MARGIN, y, MARGIN + leftW, y + dmgBoxH)
        canvas.drawRoundRect(leftRect, 5f, 5f, borderPaint)
        var ly = y + 13f
        val dmgLines = listOf(
            "Rumah: RB ${assessment.damages.houseHeavy} Unit | RS ${assessment.damages.houseModerate} Unit | RR ${assessment.damages.houseLight} Unit",
            "Fasilitas Umum: Sekolah ${assessment.damages.schoolUnits} | Puskesmas ${assessment.damages.puskesmasUnits} | Kantor ${assessment.damages.officeUnits} Unit",
            "Infrastruktur: Jalan ${assessment.damages.roadMeters} m | Jembatan ${assessment.damages.bridgeUnits} Unit | Irigasi ${assessment.damages.irrigationMeters} m",
            "Estimasi Kerugian: ${assessment.damages.formatEstimatedLossRupiah()}",
            "Catatan: ${settings.truncateIfNeeded(assessment.damages.damageDescription.ifBlank { "-" }, 90)}"
        )
        dmgLines.forEachIndexed { idx, line ->
            val p = if (idx == 3) boldBodyPaint else bodyPaint
            PdfTextAutoShrinkHelper.drawAutoShrinkText(
                canvas = canvas,
                text = line,
                x = MARGIN + 7f,
                y = ly,
                maxWidth = leftW - 14f,
                basePaint = p,
                initialFontSize = 7.6f,
                minFontSize = settings.minFontSize
            )
            ly += 17f
        }

        // Kanan: Grafik Batang Kerusakan Rumah (RB, RS, RR)
        val chartLeft = MARGIN + leftW + 10f
        val chartRect = RectF(chartLeft, y, MARGIN + contentWidth, y + dmgBoxH)
        canvas.drawRoundRect(chartRect, 5f, 5f, borderPaint)
        val chartTitlePaint = Paint(boldBodyPaint).apply { textAlign = Paint.Align.CENTER; textSize = 7.4f }
        PdfTextAutoShrinkHelper.drawAutoShrinkText(
            canvas = canvas,
            text = "GRAFIK KERUSAKAN RUMAH (UNIT)",
            x = chartRect.centerX(),
            y = y + 12f,
            maxWidth = rightW - 12f,
            basePaint = chartTitlePaint,
            initialFontSize = 7.4f,
            minFontSize = 5.8f
        )

        val bars = listOf(
            Triple("RB", assessment.damages.houseHeavy, Color.rgb(220, 38, 38)),
            Triple("RS", assessment.damages.houseModerate, Color.rgb(234, 88, 12)),
            Triple("RR", assessment.damages.houseLight, Color.rgb(217, 119, 6))
        )
        val maxVal = max(1, bars.maxOf { it.second })
        val barAreaBottom = y + dmgBoxH - 18f
        val barAreaMaxH = 52f
        val slotW = rightW / 3f
        bars.forEachIndexed { idx, (label, count, colorInt) ->
            val barCx = chartLeft + slotW * (idx + 0.5f)
            val barW = 28f
            val h = (count.toFloat() / maxVal.toFloat()) * barAreaMaxH
            val barRect = RectF(barCx - barW / 2f, barAreaBottom - h, barCx + barW / 2f, barAreaBottom)
            canvas.drawRoundRect(barRect, 3f, 3f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colorInt })
            val valPaint = Paint(boldBodyPaint).apply { textAlign = Paint.Align.CENTER; textSize = 7.5f; color = colorInt }
            PdfTextAutoShrinkHelper.drawPdfText(canvas, "$count", barCx, barRect.top - 3f, valPaint)
            val lblPaint = Paint(bodyPaint).apply { textAlign = Paint.Align.CENTER; textSize = 7.2f }
            PdfTextAutoShrinkHelper.drawPdfText(canvas, label, barCx, barAreaBottom + 11f, lblPaint)
        }
        y += dmgBoxH + 16f

        // BAGIAN III: KEBUTUHAN MENDESAK
        PdfTextAutoShrinkHelper.drawPdfText(canvas, "III. DAFTAR KEBUTUHAN MENDESAK LAPANGAN", MARGIN, y, sectionPaint)
        y += 7f
        val needCols = floatArrayOf(0.20f, 0.14f, 0.14f, 0.16f, 0.36f)
        val needHeaders = arrayOf("JENIS", "JUMLAH", "SATUAN", "STATUS", "KETERANGAN")
        var cx = MARGIN
        val hdrH = 16f
        for (i in needHeaders.indices) {
            val w = contentWidth * needCols[i]
            canvas.drawRect(cx, y, cx + w, y + hdrH, headerFillPaint)
            canvas.drawRect(cx, y, cx + w, y + hdrH, borderPaint)
            val hp = Paint(boldBodyPaint).apply { textAlign = Paint.Align.CENTER; textSize = 7.4f }
            PdfTextAutoShrinkHelper.drawPdfText(canvas, needHeaders[i], cx + w / 2f, y + 11f, hp)
            cx += w
        }
        y += hdrH

        if (assessment.urgentNeeds.isEmpty()) {
            canvas.drawRect(MARGIN, y, MARGIN + contentWidth, y + 18f, borderPaint)
            PdfTextAutoShrinkHelper.drawPdfText(
                canvas,
                "Belum ada kebutuhan mendesak tercatat.",
                A4_WIDTH / 2f,
                y + 12f,
                Paint(bodyPaint).apply { textAlign = Paint.Align.CENTER }
            )
            y += 18f
        } else {
            for (need in assessment.urgentNeeds.take(7)) {
                val rowH = 16f
                var rx = MARGIN
                val vals = arrayOf(
                    need.category.label,
                    "${need.quantity}",
                    need.unit,
                    need.status.label,
                    settings.truncateIfNeeded(need.notes.ifBlank { "-" }, 48)
                )
                for (i in vals.indices) {
                    val w = contentWidth * needCols[i]
                    canvas.drawRect(rx, y, rx + w, y + rowH, borderPaint)
                    val cellP = Paint(bodyPaint).apply {
                        textSize = 7.4f
                        textAlign = if (i in 1..3) Paint.Align.CENTER else Paint.Align.LEFT
                        if (i == 3) {
                            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                            color = if (need.status == UrgentNeedStatus.MENDESAK) Color.rgb(220, 38, 38) else Color.rgb(21, 128, 61)
                        }
                    }
                    val drawX = if (i in 1..3) rx + w / 2f else rx + 4f
                    PdfTextAutoShrinkHelper.drawAutoShrinkText(
                        canvas = canvas,
                        text = vals[i],
                        x = drawX,
                        y = y + 11f,
                        maxWidth = w - 8f,
                        basePaint = cellP,
                        initialFontSize = 7.4f,
                        minFontSize = settings.minFontSize
                    )
                    rx += w
                }
                y += rowH
            }
        }

        // BAGIAN IV: BLOK TANDA TANGAN TATA NASKAH DINAS (DATA-DRIVEN + AUTO-SHRINK)
        val sigStartY = (y + 20f).coerceAtMost(A4_HEIGHT - 195f)
        ReportExporter.drawBlokTandaTanganPiramidaTerbalik(
            canvas = canvas,
            pageWidth = A4_WIDTH,
            startY = sigStartY,
            margin = MARGIN,
            context = context,
            documentSettings = settings
        )

        ReportExporter.drawFooterAtribusiResmi(
            canvas = canvas,
            pageWidth = A4_WIDTH,
            pageHeight = A4_HEIGHT,
            margin = MARGIN,
            documentSettings = settings
        )
    }
}
