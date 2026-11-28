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
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.repository.DocumentSettingsRepository
import com.example.model.DalaAssessmentState
import com.example.model.DocumentSettings
import com.example.model.UrgentNeedItem
import com.example.model.UrgentNeedStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.max

/**
 * Wrapper Ekspor PDF Standar BNPB/BPBD untuk Modul Pasca Bencana / DALA (Data dan Informasi Bencana).
 *
 * Terintegrasi langsung dengan mesin PDF Data-Driven (`DocumentSettingsRepository`, `PdfTextAutoShrinkHelper`,
 * Kop Surat Resmi, Grafik Batang Kerusakan Rumah RB/RS/RR, Tabel Kebutuhan Mendesak dengan kolom KETERANGAN
 * diperlebar + pembungkusan baris penuh tanpa pemotongan/ellipsis, dukungan multi-halaman otomatis, dan
 * Blok Tanda Tangan Piramida Terbalik).
 */
object DalaPdfExportWrapper {

    private const val A4_WIDTH = ReportExporter.A4_PAGE_WIDTH
    private const val A4_HEIGHT = ReportExporter.A4_PAGE_HEIGHT
    private const val MARGIN = ReportExporter.STANDARD_MARGIN
    private const val FOOTER_LIMIT_Y = A4_HEIGHT - 36f
    private const val SIGNATURE_BLOCK_RESERVE_H = 165f

    /**
     * Proporsi kolom tabel III. DAFTAR KEBUTUHAN MENDESAK LAPANGAN.
     * Kolom KETERANGAN diperlebar menjadi 51% agar informasi tindak lanjut pemerintah tampil utuh dan mudah dibaca.
     */
    val NEED_COLUMN_WEIGHTS: FloatArray = floatArrayOf(0.17f, 0.09f, 0.10f, 0.13f, 0.51f)
    val NEED_COLUMN_HEADERS: Array<String> = arrayOf("JENIS", "JUMLAH", "SATUAN", "STATUS", "KETERANGAN")

    data class DalaFormattedReportText(
        val sitesSummary: String,
        val vulnerableSummary: String,
        val damageNotes: String,
        val urgentNeedRows: List<List<String>>
    )

    /**
     * Menyiapkan seluruh teks laporan DALA secara UTUH (tanpa pemotongan karakter / ellipsis `...`)
     * agar dapat dijadikan acuan resmi pengambilan kebijakan dan tindak lanjut Pemerintah Daerah.
     */
    fun buildCompleteReportText(assessment: DalaAssessmentState): DalaFormattedReportText {
        val numberFormat = NumberFormat.getIntegerInstance(Locale("id", "ID"))

        val sitesSummary = if (assessment.casualties.evacuationSites.isEmpty()) {
            "Belum ada titik pengungsian tercatat."
        } else {
            assessment.casualties.evacuationSites.joinToString("; ") { site ->
                val notePart = site.notes.trim().takeIf { it.isNotEmpty() }?.let { " — $it" }.orEmpty()
                "${site.locationName.trim()} (${numberFormat.format(site.evacueeCount)} jiwa$notePart)"
            }
        }

        val vulnerableSummary = if (assessment.casualties.vulnerableGroups.isEmpty()) {
            "Tidak ada data kelompok rentan."
        } else {
            assessment.casualties.vulnerableGroups.joinToString("; ") { group ->
                val notePart = group.notes.trim().takeIf { it.isNotEmpty() }?.let { " — $it" }.orEmpty()
                "${group.category.label}: ${numberFormat.format(group.count)} jiwa$notePart"
            }
        }

        val damageNotes = assessment.damages.damageDescription.trim().ifBlank { "-" }

        val urgentNeedRows = assessment.urgentNeeds.map { need ->
            formatUrgentNeedRowValues(need, numberFormat)
        }

        return DalaFormattedReportText(
            sitesSummary = sitesSummary,
            vulnerableSummary = vulnerableSummary,
            damageNotes = damageNotes,
            urgentNeedRows = urgentNeedRows
        )
    }

    fun formatUrgentNeedRowValues(
        need: UrgentNeedItem,
        numberFormat: NumberFormat = NumberFormat.getIntegerInstance(Locale("id", "ID"))
    ): List<String> {
        return listOf(
            need.category.label.trim().ifBlank { "-" },
            numberFormat.format(need.quantity),
            need.unit.trim().ifBlank { "-" },
            need.status.label.trim().ifBlank { "-" },
            need.notes.trim().ifBlank { "-" }
        )
    }

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
            renderDalaBnpbDocument(
                context = context,
                pdfDoc = pdfDoc,
                assessment = assessment,
                settings = settings
            )

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
     * Merender dokumen Laporan Kaji Cepat Pasca Bencana (DALA) standar BNPB ke [PdfDocument]
     * dengan dukungan otomatis multi-halaman agar seluruh isi berita, catatan kerusakan,
     * daftar kebutuhan mendesak, dan blok tanda tangan tercetak UTUH tanpa terpotong.
     *
     * @return Jumlah total halaman A4 yang dihasilkan.
     */
    fun renderDalaBnpbDocument(
        context: Context,
        pdfDoc: PdfDocument,
        assessment: DalaAssessmentState,
        settings: DocumentSettings = DocumentSettingsRepository.getCachedOrDefaultSettings(context)
    ): Int {
        var activePage: PdfDocument.Page? = null
        return renderDalaBnpbPages(
            context = context,
            assessment = assessment,
            settings = settings,
            pageFactory = { pageNumber ->
                val page = pdfDoc.startPage(
                    PdfDocument.PageInfo.Builder(A4_WIDTH, A4_HEIGHT, pageNumber).create()
                )
                activePage = page
                page.canvas
            },
            onFinishPage = { _, _ ->
                activePage?.let { pdfDoc.finishPage(it) }
                activePage = null
            }
        )
    }

    /**
     * Mesin perenderan multi-halaman generik yang digunakan oleh [renderDalaBnpbDocument]
     * dan dapat diverifikasi langsung pada lingkungan pengujian JVM/Robolectric dengan Canvas Bitmap.
     *
     * @return Jumlah total halaman A4 yang dirender.
     */
    fun renderDalaBnpbPages(
        context: Context,
        assessment: DalaAssessmentState,
        settings: DocumentSettings = DocumentSettingsRepository.getCachedOrDefaultSettings(context),
        pageFactory: (pageNumber: Int) -> Canvas,
        onFinishPage: (pageNumber: Int, canvas: Canvas) -> Unit = { _, _ -> }
    ): Int {
        var pageNumber = 1
        var currentCanvas = pageFactory(pageNumber)

        val startNextPage: () -> Canvas = {
            ReportExporter.drawFooterAtribusiResmi(
                canvas = currentCanvas,
                pageWidth = A4_WIDTH,
                pageHeight = A4_HEIGHT,
                margin = MARGIN,
                documentSettings = settings
            )
            onFinishPage(pageNumber, currentCanvas)
            pageNumber += 1
            currentCanvas = pageFactory(pageNumber)
            currentCanvas
        }

        renderDalaContent(
            context = context,
            initialCanvas = currentCanvas,
            assessment = assessment,
            settings = settings,
            onRequestNewPage = {
                val nextCanvas = startNextPage()
                val nextStartY = ReportExporter.drawKopSuratResmi(
                    context = context,
                    canvas = nextCanvas,
                    pageWidth = A4_WIDTH,
                    margin = MARGIN,
                    pageNumber = pageNumber,
                    documentSettings = settings
                )
                nextCanvas to nextStartY
            }
        )

        ReportExporter.drawFooterAtribusiResmi(
            canvas = currentCanvas,
            pageWidth = A4_WIDTH,
            pageHeight = A4_HEIGHT,
            margin = MARGIN,
            documentSettings = settings
        )
        onFinishPage(pageNumber, currentCanvas)
        return pageNumber
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
        renderDalaContent(
            context = context,
            initialCanvas = canvas,
            assessment = assessment,
            settings = settings,
            onRequestNewPage = null
        )
        ReportExporter.drawFooterAtribusiResmi(
            canvas = canvas,
            pageWidth = A4_WIDTH,
            pageHeight = A4_HEIGHT,
            margin = MARGIN,
            documentSettings = settings
        )
    }

    private fun renderDalaContent(
        context: Context,
        initialCanvas: Canvas,
        assessment: DalaAssessmentState,
        settings: DocumentSettings,
        onRequestNewPage: (() -> Pair<Canvas, Float>)?
    ) {
        var canvas = initialCanvas
        val contentWidth = A4_WIDTH - (MARGIN * 2f)
        val formattedText = buildCompleteReportText(assessment)
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
        val metaTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(71, 85, 105)
            textSize = 7.8f
        }
        val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 9.0f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 41, 59)
            textSize = settings.bodyFontSize.coerceIn(7.4f, 8.2f)
        }
        val boldBodyPaint = Paint(bodyPaint).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyTextPaint = TextPaint(bodyPaint)
        val boldBodyTextPaint = TextPaint(boldBodyPaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(203, 213, 225)
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
        }
        val headerFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(255, 247, 237) // Warm Orange 50
            style = Paint.Style.FILL
        }
        val zebraFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(248, 250, 252) // Slate 50
            style = Paint.Style.FILL
        }

        // Judul & Metadata (multi-baris penuh tanpa pemotongan)
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
        y += 5f

        val metaLine1 = "ID: ${assessment.assessmentId}  |  Kejadian: ${assessment.disasterTitle.ifBlank { "-" }}  |  Wilayah: ${assessment.district}"
        val metaLayout1 = buildFullWrappedLayout(
            text = metaLine1,
            paint = metaTextPaint,
            maxWidth = contentWidth,
            alignment = Layout.Alignment.ALIGN_CENTER,
            lineSpacingAdd = 1.0f
        )
        drawStaticLayout(canvas, metaLayout1, MARGIN, y)
        y += metaLayout1.height + 2.5f

        val metaLine2 = "Petugas: ${resolvedOfficer.formattedName} (${resolvedOfficer.nipLine})"
        val metaLayout2 = buildFullWrappedLayout(
            text = metaLine2,
            paint = metaTextPaint,
            maxWidth = contentWidth,
            alignment = Layout.Alignment.ALIGN_CENTER,
            lineSpacingAdd = 1.0f
        )
        drawStaticLayout(canvas, metaLayout2, MARGIN, y)
        y += metaLayout2.height + 10f

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
        y += kpiBoxH + 8f

        // Titik Pengungsian & Kelompok Rentan (multi-baris penuh, ZERO ellipsis)
        val sitesLayout = buildFullWrappedLayout(
            text = "Titik Pengungsian: ${formattedText.sitesSummary}",
            paint = bodyTextPaint,
            maxWidth = contentWidth
        )
        drawStaticLayout(canvas, sitesLayout, MARGIN, y)
        y += sitesLayout.height + 4.5f

        val vulnerableLayout = buildFullWrappedLayout(
            text = "Kelompok Rentan: ${formattedText.vulnerableSummary}",
            paint = bodyTextPaint,
            maxWidth = contentWidth
        )
        drawStaticLayout(canvas, vulnerableLayout, MARGIN, y)
        y += vulnerableLayout.height + 12f

        // BAGIAN II: KERUSAKAN, KERUGIAN & GRAFIK BATANG RUMAH (RB / RS / RR)
        PdfTextAutoShrinkHelper.drawPdfText(canvas, "II. KERUSAKAN INFRASTRUKTUR, RUMAH & ESTIMASI KERUGIAN", MARGIN, y, sectionPaint)
        y += 7f

        val leftW = contentWidth * 0.58f
        val rightW = contentWidth - leftW - 10f
        val dmgInnerW = leftW - 14f
        val dmgBodyPaint = TextPaint(bodyPaint).apply { textSize = 7.5f }
        val dmgBoldPaint = TextPaint(boldBodyPaint).apply { textSize = 7.6f }

        val dmgRows = listOf(
            "Rumah: RB ${assessment.damages.houseHeavy} Unit | RS ${assessment.damages.houseModerate} Unit | RR ${assessment.damages.houseLight} Unit" to false,
            "Fasilitas Umum: Sekolah ${assessment.damages.schoolUnits} | Puskesmas ${assessment.damages.puskesmasUnits} | Kantor ${assessment.damages.officeUnits} Unit" to false,
            "Infrastruktur: Jalan ${assessment.damages.roadMeters} m | Jembatan ${assessment.damages.bridgeUnits} Unit | Irigasi ${assessment.damages.irrigationMeters} m" to false,
            "Estimasi Kerugian: ${assessment.damages.formatEstimatedLossRupiah()}" to true,
            "Catatan Kerusakan: ${formattedText.damageNotes}" to false
        )
        val dmgLayouts = dmgRows.map { (text, isBold) ->
            buildFullWrappedLayout(
                text = text,
                paint = if (isBold) dmgBoldPaint else dmgBodyPaint,
                maxWidth = dmgInnerW,
                lineSpacingAdd = 1.2f
            )
        }
        val dmgLineGap = 4.5f
        val leftContentHeight = dmgLayouts.sumOf { it.height }.toFloat() +
            (if (dmgLayouts.size > 1) dmgLineGap * (dmgLayouts.size - 1) else 0f)
        val dmgBoxH = max(102f, leftContentHeight + 16f)

        // Jika kotak Bagian II tidak muat di sisa halaman saat ini, pindah ke halaman baru
        if (onRequestNewPage != null && y + dmgBoxH > FOOTER_LIMIT_Y) {
            val (nextCanvas, nextY) = onRequestNewPage()
            canvas = nextCanvas
            y = nextY
            PdfTextAutoShrinkHelper.drawPdfText(
                canvas,
                "II. KERUSAKAN INFRASTRUKTUR, RUMAH & ESTIMASI KERUGIAN (Lanjutan)",
                MARGIN,
                y,
                sectionPaint
            )
            y += 7f
        }

        // Kiri: Rincian Kerusakan (tinggi dinamis, seluruh catatan tercetak utuh)
        val leftRect = RectF(MARGIN, y, MARGIN + leftW, y + dmgBoxH)
        canvas.drawRoundRect(leftRect, 5f, 5f, borderPaint)
        var ly = y + 8f
        dmgLayouts.forEachIndexed { idx, layout ->
            drawStaticLayout(canvas, layout, MARGIN + 7f, ly)
            ly += layout.height + if (idx < dmgLayouts.lastIndex) dmgLineGap else 0f
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
            y = y + 13f,
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
        val barAreaTop = y + 28f
        val barAreaBottom = y + dmgBoxH - 18f
        val barAreaMaxH = (barAreaBottom - barAreaTop).coerceIn(40f, 78f)
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
        y += dmgBoxH + 14f

        // BAGIAN III: KEBUTUHAN MENDESAK (Kolom KETERANGAN diperlebar 51%, baris dinamis & utuh tanpa ellipsis)
        val hdrH = 18f
        val drawNeedsSectionHeader: (Canvas, Float, Boolean) -> Float = { targetCanvas, startY, isContinuation ->
            var curY = startY
            val heading = if (isContinuation) {
                "III. DAFTAR KEBUTUHAN MENDESAK LAPANGAN (Lanjutan)"
            } else {
                "III. DAFTAR KEBUTUHAN MENDESAK LAPANGAN"
            }
            PdfTextAutoShrinkHelper.drawPdfText(targetCanvas, heading, MARGIN, curY, sectionPaint)
            curY += 7f
            var cx = MARGIN
            for (i in NEED_COLUMN_HEADERS.indices) {
                val w = contentWidth * NEED_COLUMN_WEIGHTS[i]
                targetCanvas.drawRect(cx, curY, cx + w, curY + hdrH, headerFillPaint)
                targetCanvas.drawRect(cx, curY, cx + w, curY + hdrH, borderPaint)
                val hp = Paint(boldBodyPaint).apply { textAlign = Paint.Align.CENTER; textSize = 7.4f }
                PdfTextAutoShrinkHelper.drawPdfText(targetCanvas, NEED_COLUMN_HEADERS[i], cx + w / 2f, curY + 12f, hp)
                cx += w
            }
            curY + hdrH
        }

        // Pastikan judul + header tabel + minimal 1 baris muat sebelum footer
        if (onRequestNewPage != null && y + 48f > FOOTER_LIMIT_Y) {
            val (nextCanvas, nextY) = onRequestNewPage()
            canvas = nextCanvas
            y = nextY
        }
        y = drawNeedsSectionHeader(canvas, y, false)

        if (assessment.urgentNeeds.isEmpty()) {
            canvas.drawRect(MARGIN, y, MARGIN + contentWidth, y + 20f, borderPaint)
            PdfTextAutoShrinkHelper.drawPdfText(
                canvas,
                "Belum ada kebutuhan mendesak tercatat.",
                A4_WIDTH / 2f,
                y + 13f,
                Paint(bodyPaint).apply { textAlign = Paint.Align.CENTER }
            )
            y += 20f
        } else {
            val cellPadH = 5f
            val cellPadV = 4.5f
            val cellBaseTextSize = 7.5f

            assessment.urgentNeeds.forEachIndexed { rowIndex, need ->
                val rowValues = formattedText.urgentNeedRows[rowIndex]
                val cellLayouts = rowValues.mapIndexed { colIdx, cellValue ->
                    val colWidth = contentWidth * NEED_COLUMN_WEIGHTS[colIdx]
                    val innerW = (colWidth - (cellPadH * 2f)).coerceAtLeast(20f)
                    val cellPaint = TextPaint(bodyTextPaint).apply {
                        textSize = cellBaseTextSize
                        if (colIdx == 3) {
                            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                            color = if (need.status == UrgentNeedStatus.MENDESAK) {
                                Color.rgb(220, 38, 38)
                            } else {
                                Color.rgb(21, 128, 61)
                            }
                        } else if (colIdx == 0) {
                            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        }
                    }
                    val align = if (colIdx in 1..3) Layout.Alignment.ALIGN_CENTER else Layout.Alignment.ALIGN_NORMAL
                    buildFullWrappedLayout(
                        text = cellValue,
                        paint = cellPaint,
                        maxWidth = innerW,
                        alignment = align,
                        lineSpacingAdd = 1.4f
                    )
                }

                val maxTextH = cellLayouts.maxOf { it.height }.toFloat()
                val rowH = max(20f, maxTextH + (cellPadV * 2f))

                // Jika baris tidak muat di halaman saat ini, buka halaman baru otomatis & ulangi header tabel
                if (onRequestNewPage != null && y + rowH > FOOTER_LIMIT_Y) {
                    val (nextCanvas, nextY) = onRequestNewPage()
                    canvas = nextCanvas
                    y = drawNeedsSectionHeader(canvas, nextY, true)
                }

                var rx = MARGIN
                val isZebra = rowIndex % 2 == 1
                for (colIdx in cellLayouts.indices) {
                    val w = contentWidth * NEED_COLUMN_WEIGHTS[colIdx]
                    if (isZebra) {
                        canvas.drawRect(rx, y, rx + w, y + rowH, zebraFillPaint)
                    }
                    canvas.drawRect(rx, y, rx + w, y + rowH, borderPaint)

                    val layout = cellLayouts[colIdx]
                    // Kolom pendek (Jenis/Jumlah/Satuan/Status) diratakan tengah vertikal, Keterangan tetap rapi
                    val verticalOffset = ((rowH - layout.height) / 2f).coerceAtLeast(cellPadV)
                    drawStaticLayout(canvas, layout, rx + cellPadH, y + verticalOffset)
                    rx += w
                }
                y += rowH
            }
        }

        // BAGIAN IV: BLOK TANDA TANGAN TATA NASKAH DINAS (DATA-DRIVEN + AUTO-SHRINK)
        val desiredSigStartY = y + 18f
        val sigStartY = if (onRequestNewPage != null && desiredSigStartY + SIGNATURE_BLOCK_RESERVE_H > FOOTER_LIMIT_Y) {
            val (nextCanvas, nextY) = onRequestNewPage()
            canvas = nextCanvas
            nextY + 10f
        } else {
            desiredSigStartY.coerceAtMost(A4_HEIGHT - 195f)
        }

        ReportExporter.drawBlokTandaTanganPiramidaTerbalik(
            canvas = canvas,
            pageWidth = A4_WIDTH,
            startY = sigStartY,
            margin = MARGIN,
            context = context,
            documentSettings = settings
        )
    }

    private fun buildFullWrappedLayout(
        text: String,
        paint: TextPaint,
        maxWidth: Float,
        alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL,
        lineSpacingAdd: Float = 1.2f,
        lineSpacingMult: Float = 1.05f
    ): StaticLayout {
        val cleaned = text.replace("\r\n", "\n").replace("\r", "\n").trim().ifEmpty { "-" }
        val spaced = PdfTextAutoShrinkHelper.normalizePdfWordSpacing(cleaned)
        val configuredPaint = PdfTextAutoShrinkHelper.configurePdfPaint(TextPaint(paint), 0.025f)
        val widthPx = maxWidth.toInt().coerceAtLeast(24)
        return StaticLayout.Builder
            .obtain(spaced, 0, spaced.length, configuredPaint, widthPx)
            .setAlignment(alignment)
            .setLineSpacing(lineSpacingAdd, lineSpacingMult)
            .setIncludePad(false)
            .build()
    }

    private fun drawStaticLayout(canvas: Canvas, layout: StaticLayout, x: Float, y: Float) {
        canvas.save()
        canvas.translate(x, y)
        layout.draw(canvas)
        canvas.restore()
    }
}
