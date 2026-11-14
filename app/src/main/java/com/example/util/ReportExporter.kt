package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.util.Base64
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.local.OfficerAuthManager
import com.example.model.CitizenDisasterReport
import com.example.ui.components.DisasterChartCategory
import com.example.ui.components.MonthlyDisasterSummary
import com.example.ui.components.PieSliceData
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Helper untuk mengekspor laporan bencana warga dan statistik bulanan ke format PDF dan CSV.
 * Digunakan oleh Petugas Pusdalops BPBD Kabupaten Mukomuko untuk arsip digital,
 * lampiran laporan resmi harian, dan analisis data spreadsheet (Excel).
 */
object ReportExporter {

    private const val FOOTER_ATRIBUSI_TEXT =
        "Dicetak melalui Aplikasi SIPAGAR KITA - Pusdalops PB BPBD Kabupaten Mukomuko"

    @Volatile
    private var cachedMukomukoLogo: Bitmap? = null

    @Volatile
    private var cachedBpbdLogo: Bitmap? = null

    /**
     * Memuat dan membersihkan background pada Logo Kabupaten Mukomuko (perisai) atau Logo BPBD (lingkaran)
     * sehingga 100% transparan (ARGB_8888 tanpa background warna/kotak apapun) serta di-crop tepat
     * pada batas tepi logo agar ukurannya proporsional dan sejajar saat digambar di kop surat.
     */
    fun getCleanTransparentLogo(context: Context, isMukomukoShield: Boolean): Bitmap? {
        if (isMukomukoShield) {
            cachedMukomukoLogo?.let { return it }
        } else {
            cachedBpbdLogo?.let { return it }
        }

        return try {
            val resId = if (isMukomukoShield) {
                com.example.R.drawable.ic_mukomuko_logo
            } else {
                com.example.R.drawable.ic_bpbd_mukomuko_circle
            }
            val opts = BitmapFactory.Options().apply {
                inScaled = false
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val src = BitmapFactory.decodeResource(context.resources, resId, opts) ?: return null
            val w = src.width
            val h = src.height
            val pixels = IntArray(w * h)
            src.getPixels(pixels, 0, w, 0, 0, w, h)

            val visited = BooleanArray(w * h)
            val queue = IntArray(w * h)
            var head = 0
            var tail = 0

            fun enqueue(x: Int, y: Int) {
                val idx = y * w + x
                if (!visited[idx]) {
                    visited[idx] = true
                    queue[tail++] = idx
                }
            }

            for (x in 0 until w) {
                enqueue(x, 0)
                enqueue(x, h - 1)
            }
            for (y in 0 until h) {
                enqueue(0, y)
                enqueue(w - 1, y)
            }

            fun isBackgroundPixel(color: Int): Boolean {
                val a = (color ushr 24) and 0xFF
                if (a < 30) return true
                val r = (color ushr 16) and 0xFF
                val g = (color ushr 8) and 0xFF
                val b = color and 0xFF
                return if (isMukomukoShield) {
                    // Menghapus background luar di sekitar garis tepi hitam perisai Kab. Mukomuko
                    (r + g + b) > 260 && abs(r - g) < 28 && abs(g - b) < 28
                } else {
                    // Menghapus background luar di sekitar cincin gelap luar lingkaran BPBD
                    r > 195 && g > 195 && b > 195
                }
            }

            while (head < tail) {
                val idx = queue[head++]
                val color = pixels[idx]
                if (isBackgroundPixel(color)) {
                    pixels[idx] = Color.TRANSPARENT
                    val x = idx % w
                    val y = idx / w
                    if (x > 0) enqueue(x - 1, y)
                    if (x < w - 1) enqueue(x + 1, y)
                    if (y > 0) enqueue(x, y - 1)
                    if (y < h - 1) enqueue(x, y + 1)
                }
            }

            // Haluskan piksel tepi luar yang berbatasan langsung dengan area transparan
            for (y in 1 until h - 1) {
                for (x in 1 until w - 1) {
                    val idx = y * w + x
                    if (pixels[idx] != Color.TRANSPARENT && isBackgroundPixel(pixels[idx])) {
                        if (pixels[idx - 1] == Color.TRANSPARENT ||
                            pixels[idx + 1] == Color.TRANSPARENT ||
                            pixels[idx - w] == Color.TRANSPARENT ||
                            pixels[idx + w] == Color.TRANSPARENT
                        ) {
                            pixels[idx] = Color.TRANSPARENT
                        }
                    }
                }
            }

            // Cari bounding box ketat dari piksel non-transparan agar rasio aspek logo akurat
            var minX = w
            var minY = h
            var maxX = 0
            var maxY = 0
            for (y in 0 until h) {
                for (x in 0 until w) {
                    val alpha = (pixels[y * w + x] ushr 24) and 0xFF
                    if (alpha > 16) {
                        if (x < minX) minX = x
                        if (x > maxX) maxX = x
                        if (y < minY) minY = y
                        if (y > maxY) maxY = y
                    }
                }
            }

            val cleanedFull = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            cleanedFull.setPixels(pixels, 0, w, 0, 0, w, h)

            val trimmed = if (maxX > minX && maxY > minY) {
                Bitmap.createBitmap(
                    cleanedFull,
                    minX,
                    minY,
                    (maxX - minX + 1).coerceAtLeast(1),
                    (maxY - minY + 1).coerceAtLeast(1)
                )
            } else {
                cleanedFull
            }

            if (isMukomukoShield) {
                cachedMukomukoLogo = trimmed
            } else {
                cachedBpbdLogo = trimmed
            }
            trimmed
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Menghasilkan Data URI Base64 PNG transparan (`data:image/png;base64,...`)
     * untuk digunakan pada kop surat HTML cetak PDF (`PdfGenerator`).
     */
    fun getCleanLogoDataUri(context: Context, isMukomukoShield: Boolean): String {
        return try {
            val bmp = getCleanTransparentLogo(context, isMukomukoShield) ?: return ""
            val baos = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.PNG, 100, baos)
            val base64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
            "data:image/png;base64,$base64"
        } catch (_: Exception) {
            ""
        }
    }

    /**
     * Menggambar Kop Surat Resmi Pemerintah Kabupaten Mukomuko secara terstandarisasi:
     * - Sebelah KIRI  : Logo Kabupaten Mukomuko (tanpa background warna, proporsional perisai)
     * - Bagian TENGAH : Teks Instansi Pemkab Mukomuko, BPBD, Pusdalops-PB & Alamat Resmi
     * - Sebelah KANAN : Logo BPBD Kabupaten Mukomuko (tanpa background warna, proporsional lingkaran)
     *
     * @return Posisi koordinat Y berikutnya setelah garis ganda kop surat.
     */
    fun drawKopSuratResmi(
        context: Context,
        canvas: Canvas,
        pageWidth: Int,
        margin: Float = 30f,
        pageNumber: Int = 1
    ): Float {
        // Kop surat hanya dicetak pada Halaman 1; halaman 2 dst langsung dimulai dari konten.
        if (pageNumber > 1) {
            return margin
        }
        val targetLogoHeight = 44f
        val logoCenterY = 41f
        val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        // 1. Logo KIRI: Kabupaten Mukomuko (Perisai proporsional, transparan tanpa background)
        try {
            val leftLogo = getCleanTransparentLogo(context, isMukomukoShield = true)
            if (leftLogo != null && leftLogo.height > 0) {
                val aspect = leftLogo.width.toFloat() / leftLogo.height.toFloat()
                val leftWidth = targetLogoHeight * aspect
                val top = logoCenterY - (targetLogoHeight / 2f)
                val dstLeft = RectF(margin, top, margin + leftWidth, top + targetLogoHeight)
                canvas.drawBitmap(leftLogo, null, dstLeft, bitmapPaint)
            }
        } catch (_: Exception) {}

        // 2. Logo KANAN: BPBD Kabupaten Mukomuko (Lingkaran proporsional, sejajar & transparan tanpa background)
        try {
            val rightLogo = getCleanTransparentLogo(context, isMukomukoShield = false)
            if (rightLogo != null && rightLogo.height > 0) {
                val aspect = rightLogo.width.toFloat() / rightLogo.height.toFloat()
                val rightWidth = targetLogoHeight * aspect
                val top = logoCenterY - (targetLogoHeight / 2f)
                val dstRight = RectF(
                    pageWidth - margin - rightWidth,
                    top,
                    pageWidth - margin,
                    top + targetLogoHeight
                )
                canvas.drawBitmap(rightLogo, null, dstRight, bitmapPaint)
            }
        } catch (_: Exception) {}

        // 3. Teks Instansi di Tengah (sejajar secara vertikal dengan titik tengah kedua logo)
        val headerTitlePaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 11.5f
            isFakeBoldText = true
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        val headerSubtitlePaint = Paint().apply {
            color = Color.rgb(194, 65, 12)
            textSize = 9f
            isFakeBoldText = true
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        val headerAddressPaint = Paint().apply {
            color = Color.rgb(51, 65, 85)
            textSize = 7.4f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        var y = 29f
        val centerX = pageWidth / 2f
        canvas.drawText("PEMERINTAH KABUPATEN MUKOMUKO", centerX, y, headerTitlePaint)
        y += 13f
        canvas.drawText("BADAN PENANGGULANGAN BENCANA DAERAH (BPBD)", centerX, y, headerTitlePaint)
        y += 12f
        canvas.drawText(
            "PUSAT PENGENDALIAN OPERASI PENANGGULANGAN BENCANA (PUSDALOPS-PB)",
            centerX,
            y,
            headerSubtitlePaint
        )
        y += 11f
        canvas.drawText(
            "Jl. Danau Nibung, Kompleks Perkantoran Pemkab Mukomuko • Call Center Darurat 117 • Sistem SIPAGAR KITA",
            centerX,
            y,
            headerAddressPaint
        )

        // 4. Garis Penutup Kop Surat Ganda Resmi
        y += 9f
        val thickLinePaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            strokeWidth = 1.8f
            isAntiAlias = true
        }
        val thinLinePaint = Paint().apply {
            color = Color.rgb(100, 116, 139)
            strokeWidth = 0.7f
            isAntiAlias = true
        }
        canvas.drawLine(margin, y, pageWidth - margin, y, thickLinePaint)
        y += 2.5f
        canvas.drawLine(margin, y, pageWidth - margin, y, thinLinePaint)

        return y + 15f
    }

    /**
     * Menggambar atribusi footer resmi di bagian paling bawah setiap halaman PDF.
     */
    fun drawFooterAtribusiResmi(
        canvas: Canvas,
        pageWidth: Int,
        pageHeight: Int,
        margin: Float = 30f
    ) {
        val footerLineY = pageHeight - 28f
        val footerTextY = pageHeight - 16f

        val linePaint = Paint().apply {
            color = Color.rgb(203, 213, 225)
            strokeWidth = 0.6f
            isAntiAlias = true
        }
        val footerPaint = Paint().apply {
            color = Color.rgb(100, 116, 139)
            textSize = 7.2f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        canvas.drawLine(margin, footerLineY, pageWidth - margin, footerLineY, linePaint)
        canvas.drawText(FOOTER_ATRIBUSI_TEXT, pageWidth / 2f, footerTextY, footerPaint)
    }

    /**
     * Menggambar blok tanda tangan Tata Naskah Dinas dengan formasi Piramida Terbalik (3 Kolom):
     * - Baris 1 Kolom 1 (Kiri)  : Koord. Administrasi Pusdalops-PB (Rata Tengah)
     * - Baris 1 Kolom 3 (Kanan) : Manajer Pusdalops PB (Rata Tengah)
     * - Baris 2 (Merge 3 Kolom) : Pengawas di bawah tengah (Rata Tengah)
     *
     * @return Posisi Y akhir setelah blok tanda tangan.
     */
    fun drawBlokTandaTanganPiramidaTerbalik(
        canvas: Canvas,
        pageWidth: Int,
        startY: Float,
        margin: Float = 36f
    ): Float {
        val usableWidth = pageWidth - (margin * 2f)
        val colWidth = usableWidth / 3f

        // Titik tengah masing-masing kolom (3 kolom: Kiri, Tengah, Kanan)
        val leftColCenterX = margin + (colWidth * 0.5f)
        val centerColCenterX = pageWidth / 2f
        val rightColCenterX = pageWidth - margin - (colWidth * 0.5f)

        val rolePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 41, 59)
            textSize = 8.5f
            textAlign = Paint.Align.CENTER
        }
        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 8.8f
            isFakeBoldText = true
            isUnderlineText = true
            textAlign = Paint.Align.CENTER
        }
        val nipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(51, 65, 85)
            textSize = 8f
            textAlign = Paint.Align.CENTER
        }

        // --- BARIS 1: Kiri (Koord. Administrasi) & Kanan (Manajer Pusdalops PB) ---
        var yRow1 = startY
        canvas.drawText("Koord. Administrasi", leftColCenterX, yRow1, rolePaint)
        canvas.drawText("Manajer Pusdalops PB", rightColCenterX, yRow1, rolePaint)

        yRow1 += 11f
        canvas.drawText("Pusdalops-PB", leftColCenterX, yRow1, rolePaint)
        canvas.drawText("BPBD Mukomuko", rightColCenterX, yRow1, rolePaint)

        // Ruang tanda tangan basah/stempel
        yRow1 += 38f
        canvas.drawText("AAN FITRADINATA, SE", leftColCenterX, yRow1, namePaint)
        canvas.drawText("HITATUN", rightColCenterX, yRow1, namePaint)

        yRow1 += 11f
        canvas.drawText("NIP. 197903122006041007", leftColCenterX, yRow1, nipPaint)
        canvas.drawText("NIP. 197401282006041006", rightColCenterX, yRow1, nipPaint)

        // --- BARIS 2 (Merge 3 Kolom - Bawah Tengah): Pengawas ---
        var yRow2 = yRow1 + 22f
        canvas.drawText("Pengawas", centerColCenterX, yRow2, rolePaint)
        yRow2 += 11f
        canvas.drawText("Kepala Pelaksana BPBD Mukomuko", centerColCenterX, yRow2, rolePaint)

        // Ruang tanda tangan Pengawas
        yRow2 += 38f
        canvas.drawText("RURI IRWANDI, ST.MT", centerColCenterX, yRow2, namePaint)
        yRow2 += 11f
        canvas.drawText("NIP. 197906072006041011", centerColCenterX, yRow2, nipPaint)

        return yRow2 + 8f
    }

    /**
     * Menggambar teks multi-baris secara otomatis (word-wrap) menggunakan StaticLayout
     * agar teks deskripsi/catatan yang panjang turun ke baris berikutnya dan tidak keluar batas.
     *
     * @return Tinggi total (height) dari blok teks yang digambar dalam satuan point.
     */
    private fun drawWrappedText(
        canvas: Canvas,
        text: String,
        x: Float,
        topY: Float,
        maxWidth: Int,
        paint: Paint,
        maxLines: Int = Int.MAX_VALUE
    ): Float {
        val safeText = text.ifBlank { "-" }
        val textPaint = TextPaint(paint).apply { isAntiAlias = true }
        val staticLayout = StaticLayout.Builder
            .obtain(safeText, 0, safeText.length, textPaint, maxWidth.coerceAtLeast(20))
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(1.5f, 1.0f)
            .setIncludePad(false)
            .setMaxLines(maxLines)
            .build()

        canvas.save()
        canvas.translate(x, topY)
        staticLayout.draw(canvas)
        canvas.restore()

        return staticLayout.height.toFloat()
    }

    /**
     * Menghasilkan dokumen PDF resmi A4 berisi rekapitulasi laporan bencana tervalidasi.
     */
    fun exportToPdf(
        context: Context,
        reports: List<CitizenDisasterReport>,
        officerName: String,
        officerNip: String = ""
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

            val sdf = SimpleDateFormat("dd MMMM yyyy, HH:mm 'WIB'", Locale.forLanguageTag("id-ID"))
            val printDate = sdf.format(Date())

            var y = drawKopSuratResmi(context, canvas, pageWidth, margin = 36f)

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
            y += 20f

            var currentPageNumber = 1
            var currentPage = page
            var currentCanvas = canvas

            // Render Rows dengan word-wrap otomatis & multi-halaman (Halaman 2+ tanpa kop surat)
            val maxRowBottomY = pageHeight - 52f
            reports.forEachIndexed { idx, rep ->
                if (y > maxRowBottomY - 40f) {
                    drawFooterAtribusiResmi(currentCanvas, pageWidth, pageHeight, margin = 36f)
                    pdfDoc.finishPage(currentPage)

                    currentPageNumber += 1
                    val nextPageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
                    currentPage = pdfDoc.startPage(nextPageInfo)
                    currentCanvas = currentPage.canvas

                    // Halaman 2+ langsung dimulai dari konten tabel tanpa Kop Surat
                    y = drawKopSuratResmi(context, currentCanvas, pageWidth, margin = 36f, pageNumber = currentPageNumber)
                    currentCanvas.drawRect(36f, y, (pageWidth - 36).toFloat(), y + 18f, headerBoxPaint)
                    currentCanvas.drawText("No", 42f, y + 12f, boldPaint)
                    currentCanvas.drawText("ID Laporan", 68f, y + 12f, boldPaint)
                    currentCanvas.drawText("Bencana", 145f, y + 12f, boldPaint)
                    currentCanvas.drawText("Wilayah", 245f, y + 12f, boldPaint)
                    currentCanvas.drawText("Status", 370f, y + 12f, boldPaint)
                    currentCanvas.drawText("Catatan Tindak Lanjut TRC", 445f, y + 12f, boldPaint)
                    y += 20f
                }

                val rowTopY = y + 2f
                val wilayahText = buildString {
                    append("${rep.village}, ${rep.district}")
                    if (rep.incidentLocation.isNotBlank()) {
                        append(" (${rep.incidentLocation})")
                    }
                }
                val noteText = rep.additionalNotes.ifBlank { "-" }

                currentCanvas.drawText("${idx + 1}.", 42f, rowTopY + 8f, textPaint)
                currentCanvas.drawText("#${rep.id.takeLast(7)}", 68f, rowTopY + 8f, boldPaint)

                val hBencana = drawWrappedText(
                    canvas = currentCanvas,
                    text = rep.disasterType,
                    x = 145f,
                    topY = rowTopY,
                    maxWidth = 94,
                    paint = textPaint,
                    maxLines = 3
                )
                val hWilayah = drawWrappedText(
                    canvas = currentCanvas,
                    text = wilayahText,
                    x = 245f,
                    topY = rowTopY,
                    maxWidth = 118,
                    paint = textPaint,
                    maxLines = 3
                )
                val hStatus = drawWrappedText(
                    canvas = currentCanvas,
                    text = rep.status,
                    x = 370f,
                    topY = rowTopY,
                    maxWidth = 68,
                    paint = boldPaint,
                    maxLines = 2
                )
                val hNote = drawWrappedText(
                    canvas = currentCanvas,
                    text = noteText,
                    x = 445f,
                    topY = rowTopY,
                    maxWidth = (pageWidth - 36) - 445,
                    paint = textPaint,
                    maxLines = 4
                )

                val rowHeight = max(max(hBencana, hWilayah), max(hStatus, hNote)).coerceAtLeast(12f)
                y = rowTopY + rowHeight + 5f
                currentCanvas.drawLine(36f, y, (pageWidth - 36).toFloat(), y, linePaint)
                y += 4f
            }

            // Tanpa blok tanda tangan pejabat (khusus Rekapitulasi Laporan Warga)
            drawFooterAtribusiResmi(currentCanvas, pageWidth, pageHeight, margin = 36f)

            pdfDoc.finishPage(currentPage)

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

        val margin = 30f
        var y = drawKopSuratResmi(context, canvas, pageWidth, margin)

        // Judul Dokumen
        val docTitlePaint = Paint().apply {
            color = Color.rgb(194, 65, 12)
            textSize = 11f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("BULETIN PETA CUACA & PERINGATAN DINI 15 KECAMATAN", pageWidth / 2f, y, docTitlePaint)
        y += 14f

        val sdf = SimpleDateFormat("EEEE, dd MMMM yyyy - HH:mm 'WIB'", Locale.forLanguageTag("id-ID"))
        val currentTimeStr = sdf.format(Date())
        val metaPaint = Paint().apply {
            color = Color.rgb(71, 85, 105)
            textSize = 8.5f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Waktu Penerbitan: $currentTimeStr | Mode Overlay Aktif: $activeLayerName", pageWidth / 2f, y, metaPaint)
        y += 18f

        // Kotak Ringkasan Ekstrem Cuaca (Presisi dalam batas margin halaman A4)
        val maxRain = items.maxByOrNull { it.precipitation }
        val maxTemp = items.maxByOrNull { it.temperature }
        val minTemp = items.minByOrNull { it.temperature }
        val maxWind = items.maxByOrNull { it.windSpeed }

        val summaryRect = RectF(margin, y, pageWidth - margin, y + 42f)
        canvas.drawRoundRect(summaryRect, 6f, 6f, Paint().apply { color = Color.rgb(254, 242, 242) })
        canvas.drawRoundRect(summaryRect, 6f, 6f, Paint().apply { color = Color.rgb(252, 165, 165); style = Paint.Style.STROKE; strokeWidth = 1f })

        canvas.drawText("RINGKASAN PARAMETER METEOROLOGI KABUPATEN MUKOMUKO", margin + 10f, y + 13f, boldTextPaint.apply { color = Color.rgb(153, 27, 27) })
        canvas.drawText("• Curah Hujan Tertinggi: ${maxRain?.districtName ?: "-"} (${maxRain?.precipitation ?: 0.0} mm/j)", margin + 10f, y + 25f, textPaint)
        canvas.drawText("• Suhu Ekstrem: Max ${maxTemp?.districtName} (${maxTemp?.temperature ?: 0.0}°C) | Min ${minTemp?.districtName} (${minTemp?.temperature ?: 0.0}°C)", margin + 10f, y + 36f, textPaint)
        canvas.drawText("• Kecepatan Angin Maksimal: ${maxWind?.districtName} (${maxWind?.windSpeed?.toInt() ?: 0} km/j - ${maxWind?.windDirection})", margin + 270f, y + 25f, textPaint)
        canvas.drawText("• Pemantauan: Siaga Sensor Telemetri & Stasiun BMKG", margin + 270f, y + 36f, textPaint)

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

        // Catatan SOP Peringatan Dini (menggunakan StaticLayout agar teks otomatis wrap)
        val sopTextWidth = (pageWidth - margin * 2 - 20f).toInt()
        val sopNoteText =
            "1. Wilayah pesisir (Kota Mukomuko, Teramang Jaya, Ipuh, Air Rami) mewaspadai pasang surut air laut dan angin kencang.\n" +
            "2. Wilayah perbukitan (V Koto, Malin Deman, Selagan Raya) mewaspadai potensi longsor bila curah hujan >15 mm/jam berturut-turut."
        val noteRect = android.graphics.RectF(margin, y, pageWidth - margin, y + 48f)
        canvas.drawRoundRect(noteRect, 6f, 6f, Paint().apply { color = Color.rgb(255, 251, 235) })
        canvas.drawRoundRect(noteRect, 6f, 6f, Paint().apply { color = Color.rgb(253, 230, 138); style = Paint.Style.STROKE; strokeWidth = 1f })

        canvas.drawText("PETUNJUK KESIAPSIAGAAN & DISPOSISI KEBENCANAAN:", margin + 10f, y + 13f, boldTextPaint.apply { color = Color.rgb(180, 83, 9) })
        drawWrappedText(
            canvas = canvas,
            text = sopNoteText,
            x = margin + 10f,
            topY = y + 17f,
            maxWidth = sopTextWidth,
            paint = Paint(textPaint).apply { color = Color.rgb(120, 53, 15) },
            maxLines = 4
        )

        y += 54f

        // Sumber Informasi di bagian bawah konten laporan
        canvas.drawText("Sumber Data & Informasi: Stasiun Meteorologi BMKG Fatmawati Bengkulu & Jaringan Sensor Telemetri BPBD Mukomuko", margin, y, textPaint.apply { color = Color.rgb(100, 116, 139) })

        // Tanpa blok tanda tangan pejabat (khusus Peta Cuaca 15 Kecamatan)
        drawFooterAtribusiResmi(canvas, pageWidth, pageHeight, margin)

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

    /**
     * Menghasilkan dokumen PDF resmi A4 berisi Ringkasan & Statistik Data Bencana Bulanan
     * (lengkap dengan visualisasi Grafik Batang, Pie Chart proporsi bencana, KPI Eksekutif,
     * dan Tabel Matriks 12 Bulan Januari–Desember) untuk lampiran laporan resmi harian Pusdalops BPBD.
     *
     * Menyimpan berkas langsung ke folder Downloads perangkat (via MediaStore pada Android 10+)
     * serta membuka lembar berbagi/pratinjau PDF melalui FileProvider.
     */
    fun exportMonthlyDisasterAnalyticsToPdf(
        context: Context,
        monthlyData: List<MonthlyDisasterSummary>,
        pieSlices: List<PieSliceData>,
        selectedYear: Int,
        scopeLabel: String
    ): File? {
        if (monthlyData.isEmpty()) {
            Toast.makeText(context, "Tidak ada data ringkasan bencana bulanan untuk diunduh", Toast.LENGTH_SHORT).show()
            return null
        }

        return try {
            val officerAuth = OfficerAuthManager(context)
            val rawOfficerName = officerAuth.getOfficerName().ifBlank { "Petugas Operator Siaga Pusdalops-PB" }
            val rawOfficerNip = officerAuth.getOfficerBadgeId().ifBlank { "197401282006041006" }

            val pdfDoc = PdfDocument()
            val pageWidth = 595 // Standar A4 (points)
            val pageHeight = 842 // Standar A4 (points)
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdfDoc.startPage(pageInfo)
            val canvas = page.canvas
            val margin = 30f

            val titlePaint = Paint().apply {
                color = Color.rgb(15, 23, 42)
                textSize = 11.5f
                isFakeBoldText = true
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }

            val subtitlePaint = Paint().apply {
                color = Color.rgb(37, 99, 235)
                textSize = 9f
                isFakeBoldText = true
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }

            val sectionHeaderPaint = Paint().apply {
                color = Color.rgb(15, 23, 42)
                textSize = 9f
                isFakeBoldText = true
                isAntiAlias = true
            }

            val textPaint = Paint().apply {
                color = Color.rgb(51, 65, 85)
                textSize = 7.8f
                isAntiAlias = true
            }

            val boldTextPaint = Paint().apply {
                color = Color.rgb(15, 23, 42)
                textSize = 7.8f
                isFakeBoldText = true
                isAntiAlias = true
            }

            val linePaint = Paint().apply {
                color = Color.rgb(203, 213, 225)
                strokeWidth = 0.8f
                isAntiAlias = true
            }

            // 1. Kop Surat Resmi Terstandarisasi (Kiri: Kab. Mukomuko, Kanan: BPBD)
            var y = drawKopSuratResmi(context, canvas, pageWidth, margin)

            // 2. Judul Dokumen & Metadata
            val docTitlePaint = Paint(titlePaint).apply {
                color = Color.rgb(30, 58, 138)
                textSize = 10.5f
            }
            canvas.drawText(
                "LAPORAN RESMI RINGKASAN & STATISTIK DATA BENCANA BULANAN TAHUN $selectedYear",
                pageWidth / 2f,
                y,
                docTitlePaint
            )
            y += 12f

            val sdf = SimpleDateFormat("dd MMMM yyyy, HH:mm 'WIB'", Locale.forLanguageTag("id-ID"))
            val printDateStr = sdf.format(Date())
            val metaPaint = Paint(textPaint).apply {
                textAlign = Paint.Align.CENTER
                color = Color.rgb(71, 85, 105)
            }
            canvas.drawText(
                "Cakupan Wilayah: $scopeLabel  |  Waktu Unduh: $printDateStr  |  Petugas: $rawOfficerName",
                pageWidth / 2f,
                y,
                metaPaint
            )
            y += 10f

            // 3. Kotak Ringkasan Eksekutif (KPI Tahunan)
            val totalAnnual = monthlyData.sumOf { it.totalIncidents }
            val totalAffected = monthlyData.sumOf { it.casualtiesAffected }
            val totalResolved = monthlyData.sumOf { it.resolvedCount }
            val resolvedPct = if (totalAnnual > 0) ((totalResolved * 100f) / totalAnnual).roundToInt().coerceIn(0, 100) else 100
            val avgMonthly = totalAnnual / 12f
            val peakMonth = monthlyData.maxByOrNull { it.totalIncidents } ?: monthlyData.first()
            val domCatEntry = DisasterChartCategory.entries.associateWith { cat ->
                monthlyData.sumOf { it.countsByCategory[cat] ?: 0 }
            }.maxByOrNull { it.value }

            val kpiRect = RectF(margin, y, pageWidth - margin, y + 40f)
            canvas.drawRoundRect(kpiRect, 6f, 6f, Paint().apply { color = Color.rgb(239, 246, 255) })
            canvas.drawRoundRect(kpiRect, 6f, 6f, Paint().apply {
                color = Color.rgb(147, 197, 253)
                style = Paint.Style.STROKE
                strokeWidth = 1f
                isAntiAlias = true
            })

            canvas.drawText(
                "I. RINGKASAN EKSEKUTIF STATISTIK KEBENCANAAN TAHUN $selectedYear",
                margin + 10f,
                y + 12f,
                Paint(boldTextPaint).apply { color = Color.rgb(30, 64, 175) }
            )
            canvas.drawText(
                "• Total Kejadian Tahunan: $totalAnnual kejadian (Rata-rata ${String.format(Locale.getDefault(), "%.1f", avgMonthly)}/bln)",
                margin + 10f,
                y + 24f,
                textPaint
            )
            canvas.drawText(
                "• Bulan Puncak Kejadian: ${peakMonth.monthFull} (${peakMonth.totalIncidents} kejadian - Dominan Kec. ${peakMonth.topDistrict})",
                margin + 10f,
                y + 34f,
                textPaint
            )
            canvas.drawText(
                "• Bencana Dominan: ${domCatEntry?.key?.shortLabel ?: "Banjir"} (${domCatEntry?.value ?: 0} kejadian)",
                margin + 295f,
                y + 24f,
                textPaint
            )
            canvas.drawText(
                "• Penanganan Tuntas TRC: $totalResolved/$totalAnnual selesai ($resolvedPct%) • Terdampak: $totalAffected jiwa",
                margin + 295f,
                y + 34f,
                textPaint
            )

            y += 48f

            // 4. Visualisasi Grafik Batang (Jan-Des) & Pie Chart Proporsi Bencana
            canvas.drawText(
                "II. VISUALISASI GRAFIK BATANG TREN BULANAN & PIE CHART PROPORSI BENCANA",
                margin,
                y,
                sectionHeaderPaint
            )
            y += 6f

            val chartBoxHeight = 142f
            val barBoxRect = RectF(margin, y, margin + 332f, y + chartBoxHeight)
            val pieBoxRect = RectF(margin + 340f, y, pageWidth - margin, y + chartBoxHeight)

            // Kotak Kiri: Grafik Batang Bulanan (Jan-Des)
            canvas.drawRoundRect(barBoxRect, 6f, 6f, Paint().apply { color = Color.rgb(248, 250, 252) })
            canvas.drawRoundRect(barBoxRect, 6f, 6f, Paint(linePaint).apply { style = Paint.Style.STROKE })
            canvas.drawText(
                "Grafik Batang Kejadian Bulanan (Jan–Des $selectedYear)",
                barBoxRect.left + 8f,
                barBoxRect.top + 12f,
                boldTextPaint
            )

            val chartLeft = barBoxRect.left + 24f
            val chartRight = barBoxRect.right - 8f
            val chartTop = barBoxRect.top + 24f
            val chartBottom = barBoxRect.bottom - 28f
            val cWidth = chartRight - chartLeft
            val cHeight = chartBottom - chartTop
            val maxMonthInc = max(monthlyData.maxOfOrNull { it.totalIncidents } ?: 10, 6)

            val axisLabelPaint = Paint(textPaint).apply {
                textSize = 6.5f
                textAlign = Paint.Align.RIGHT
                color = Color.rgb(100, 116, 139)
            }
            for (step in 0..3) {
                val frac = step / 3f
                val gy = chartBottom - cHeight * frac
                val gVal = (maxMonthInc * frac).roundToInt()
                canvas.drawLine(chartLeft, gy, chartRight, gy, Paint(linePaint).apply { strokeWidth = 0.5f })
                canvas.drawText("$gVal", chartLeft - 3f, gy + 2f, axisLabelPaint)
            }

            val slotW = cWidth / 12f
            val barW = slotW * 0.56f
            val barMonthPaint = Paint(textPaint).apply {
                textSize = 6.5f
                textAlign = Paint.Align.CENTER
            }
            val barValPaint = Paint(boldTextPaint).apply {
                textSize = 6.5f
                textAlign = Paint.Align.CENTER
            }

            monthlyData.forEachIndexed { idx, m ->
                val cx = chartLeft + slotW * idx + slotW / 2f
                val bLeft = cx - barW / 2f
                var currBottom = chartBottom

                DisasterChartCategory.entries.forEach { cat ->
                    val count = m.countsByCategory[cat] ?: 0
                    if (count > 0) {
                        val segH = (count.toFloat() / maxMonthInc) * cHeight
                        val segTop = (currBottom - segH).coerceAtLeast(chartTop)
                        val catPaint = Paint().apply {
                            color = getCategoryRgbColor(cat)
                            isAntiAlias = true
                        }
                        canvas.drawRect(bLeft, segTop, bLeft + barW, currBottom, catPaint)
                        currBottom = segTop
                    }
                }

                canvas.drawText("${m.totalIncidents}", cx, (currBottom - 2.5f).coerceAtLeast(chartTop - 2f), barValPaint)
                canvas.drawText(m.monthShort, cx, chartBottom + 9f, barMonthPaint)
            }

            // Legenda ringkas di bawah grafik batang
            var legX = barBoxRect.left + 8f
            val legY = barBoxRect.bottom - 7f
            val legPaint = Paint(textPaint).apply { textSize = 6.3f }
            DisasterChartCategory.entries.forEach { cat ->
                canvas.drawRect(legX, legY - 5f, legX + 5f, legY, Paint().apply { color = getCategoryRgbColor(cat) })
                canvas.drawText(cat.shortLabel, legX + 7f, legY - 0.5f, legPaint)
                legX += legPaint.measureText(cat.shortLabel) + 14f
            }

            // Kotak Kanan: Pie / Donut Chart Proporsi Bencana
            canvas.drawRoundRect(pieBoxRect, 6f, 6f, Paint().apply { color = Color.rgb(248, 250, 252) })
            canvas.drawRoundRect(pieBoxRect, 6f, 6f, Paint(linePaint).apply { style = Paint.Style.STROKE })
            canvas.drawText(
                "Pie Chart Proporsi Bencana",
                pieBoxRect.left + 8f,
                pieBoxRect.top + 12f,
                boldTextPaint
            )

            val pieOval = RectF(
                pieBoxRect.left + 10f,
                pieBoxRect.top + 26f,
                pieBoxRect.left + 86f,
                pieBoxRect.top + 102f
            )
            var startAng = -90f
            pieSlices.forEachIndexed { idx, slice ->
                val sweep = (slice.percentage / 100f) * 360f
                val slicePaint = Paint().apply {
                    color = getSliceRgbColor(slice.shortLabel, idx)
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }
                canvas.drawArc(pieOval, startAng, sweep, true, slicePaint)
                startAng += sweep
            }
            // Lubang tengah Donut Chart
            canvas.drawCircle(
                pieOval.centerX(),
                pieOval.centerY(),
                19f,
                Paint().apply { color = Color.rgb(248, 250, 252); isAntiAlias = true }
            )
            canvas.drawText(
                "$totalAnnual",
                pieOval.centerX(),
                pieOval.centerY() + 3f,
                Paint(boldTextPaint).apply { textAlign = Paint.Align.CENTER; textSize = 8f }
            )

            // Legenda Persentase Pie Chart di sebelah kanan Donut
            var pLegY = pieBoxRect.top + 30f
            val pLegX = pieBoxRect.left + 94f
            val smallLegPaint = Paint(textPaint).apply { textSize = 6.6f }
            val smallBoldPaint = Paint(boldTextPaint).apply { textSize = 6.6f }
            pieSlices.take(6).forEachIndexed { idx, slice ->
                val cRgb = getSliceRgbColor(slice.shortLabel, idx)
                canvas.drawRect(pLegX, pLegY - 5f, pLegX + 6f, pLegY + 1f, Paint().apply { color = cRgb })
                val pctStr = "${String.format(Locale.getDefault(), "%.1f", slice.percentage)}%"
                canvas.drawText(
                    "${slice.shortLabel.take(12)}: ${slice.count} ($pctStr)",
                    pLegX + 9f,
                    pLegY,
                    smallBoldPaint
                )
                pLegY += 15f
            }
            canvas.drawText(
                "Total: $totalAnnual Kejadian ($selectedYear)",
                pieBoxRect.left + 10f,
                pieBoxRect.bottom - 8f,
                smallLegPaint
            )

            y += chartBoxHeight + 14f

            // 5. Tabel Rekapitulasi 12 Bulan (Januari - Desember)
            canvas.drawText(
                "III. TABEL REKAPITULASI DATA KEJADIAN BENCANA BULANAN (JANUARI – DESEMBER $selectedYear)",
                margin,
                y,
                sectionHeaderPaint
            )
            y += 6f

            val colNo = margin + 4f
            val colBulan = margin + 22f
            val colBanjir = margin + 82f
            val colEkstrem = margin + 118f
            val colLongsor = margin + 158f
            val colAbrasi = margin + 196f
            val colKarhutla = margin + 232f
            val colLain = margin + 272f
            val colTotal = margin + 308f
            val colJiwa = margin + 342f
            val colSelesai = margin + 386f
            val colKec = margin + 422f
            val colStatus = margin + 488f

            val thHeight = 18f
            canvas.drawRect(RectF(margin, y, pageWidth - margin, y + thHeight), Paint().apply { color = Color.rgb(226, 232, 240) })
            val thPaint = Paint(boldTextPaint).apply { textSize = 7.2f }
            canvas.drawText("No", colNo, y + 12f, thPaint)
            canvas.drawText("Bulan", colBulan, y + 12f, thPaint)
            canvas.drawText("Banjir", colBanjir, y + 12f, thPaint)
            canvas.drawText("Ekstrem", colEkstrem, y + 12f, thPaint)
            canvas.drawText("Longsor", colLongsor, y + 12f, thPaint)
            canvas.drawText("Abrasi", colAbrasi, y + 12f, thPaint)
            canvas.drawText("Karhutla", colKarhutla, y + 12f, thPaint)
            canvas.drawText("Lainnya", colLain, y + 12f, thPaint)
            canvas.drawText("Total", colTotal, y + 12f, thPaint)
            canvas.drawText("Dampak", colJiwa, y + 12f, thPaint)
            canvas.drawText("Selesai", colSelesai, y + 12f, thPaint)
            canvas.drawText("Kec. Dominan", colKec, y + 12f, thPaint)
            canvas.drawText("Status", colStatus, y + 12f, thPaint)
            y += thHeight

            val rowH = 17.5f
            val cellPaint = Paint(textPaint).apply { textSize = 7.2f }
            val cellBoldPaint = Paint(boldTextPaint).apply { textSize = 7.2f }

            monthlyData.forEachIndexed { idx, m ->
                if (idx % 2 == 1) {
                    canvas.drawRect(
                        RectF(margin, y, pageWidth - margin, y + rowH),
                        Paint().apply { color = Color.rgb(248, 250, 252) }
                    )
                }
                val ry = y + 12f
                val cBanjir = m.countsByCategory[DisasterChartCategory.BANJIR] ?: 0
                val cEkstrem = m.countsByCategory[DisasterChartCategory.CUACA_EKSTREM] ?: 0
                val cLongsor = m.countsByCategory[DisasterChartCategory.LONGSOR] ?: 0
                val cAbrasi = m.countsByCategory[DisasterChartCategory.ABRASI] ?: 0
                val cKarhutla = m.countsByCategory[DisasterChartCategory.KEBAKARAN] ?: 0
                val cLain = m.countsByCategory[DisasterChartCategory.GEMPA_LAINNYA] ?: 0

                val statusText = when {
                    m.totalIncidents >= 10 -> "SIAGA TINGGI"
                    m.totalIncidents >= 6 -> "WASPADA"
                    else -> "TERKENDALI"
                }
                val statusColor = when {
                    m.totalIncidents >= 10 -> Color.rgb(220, 38, 38)
                    m.totalIncidents >= 6 -> Color.rgb(217, 119, 6)
                    else -> Color.rgb(22, 163, 74)
                }

                canvas.drawText("${idx + 1}", colNo, ry, cellPaint)
                canvas.drawText(m.monthFull, colBulan, ry, cellBoldPaint)
                canvas.drawText("$cBanjir", colBanjir + 6f, ry, cellPaint)
                canvas.drawText("$cEkstrem", colEkstrem + 8f, ry, cellPaint)
                canvas.drawText("$cLongsor", colLongsor + 8f, ry, cellPaint)
                canvas.drawText("$cAbrasi", colAbrasi + 6f, ry, cellPaint)
                canvas.drawText("$cKarhutla", colKarhutla + 8f, ry, cellPaint)
                canvas.drawText("$cLain", colLain + 6f, ry, cellPaint)
                canvas.drawText("${m.totalIncidents}", colTotal + 4f, ry, cellBoldPaint)
                canvas.drawText("${m.casualtiesAffected} jw", colJiwa, ry, cellPaint)
                canvas.drawText("${m.resolvedCount}/${m.totalIncidents}", colSelesai, ry, cellPaint)
                canvas.drawText(m.topDistrict.take(13), colKec, ry, cellPaint)
                canvas.drawText(
                    statusText,
                    colStatus,
                    ry,
                    Paint(cellBoldPaint).apply { color = statusColor; textSize = 6.5f }
                )

                y += rowH
                canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
            }

            // Baris Total Tahunan
            canvas.drawRect(
                RectF(margin, y, pageWidth - margin, y + 19f),
                Paint().apply { color = Color.rgb(219, 234, 254) }
            )
            val totY = y + 13f
            val sumBanjir = monthlyData.sumOf { it.countsByCategory[DisasterChartCategory.BANJIR] ?: 0 }
            val sumEkstrem = monthlyData.sumOf { it.countsByCategory[DisasterChartCategory.CUACA_EKSTREM] ?: 0 }
            val sumLongsor = monthlyData.sumOf { it.countsByCategory[DisasterChartCategory.LONGSOR] ?: 0 }
            val sumAbrasi = monthlyData.sumOf { it.countsByCategory[DisasterChartCategory.ABRASI] ?: 0 }
            val sumKarhutla = monthlyData.sumOf { it.countsByCategory[DisasterChartCategory.KEBAKARAN] ?: 0 }
            val sumLain = monthlyData.sumOf { it.countsByCategory[DisasterChartCategory.GEMPA_LAINNYA] ?: 0 }

            canvas.drawText("TOTAL", colBulan, totY, cellBoldPaint)
            canvas.drawText("$sumBanjir", colBanjir + 6f, totY, cellBoldPaint)
            canvas.drawText("$sumEkstrem", colEkstrem + 8f, totY, cellBoldPaint)
            canvas.drawText("$sumLongsor", colLongsor + 8f, totY, cellBoldPaint)
            canvas.drawText("$sumAbrasi", colAbrasi + 6f, totY, cellBoldPaint)
            canvas.drawText("$sumKarhutla", colKarhutla + 8f, totY, cellBoldPaint)
            canvas.drawText("$sumLain", colLain + 6f, totY, cellBoldPaint)
            canvas.drawText("$totalAnnual", colTotal + 4f, totY, cellBoldPaint)
            canvas.drawText("$totalAffected jw", colJiwa, totY, cellBoldPaint)
            canvas.drawText("$totalResolved/$totalAnnual", colSelesai, totY, cellBoldPaint)
            canvas.drawText("15 Kecamatan", colKec, totY, cellBoldPaint)
            canvas.drawText("$resolvedPct% TUNTAS", colStatus, totY, Paint(cellBoldPaint).apply { color = Color.rgb(21, 128, 61); textSize = 6.5f })
            y += 27f

            // 6. Catatan Evaluasi Musiman & Tanda Tangan Pengesahan Laporan Harian
            val noteBox = RectF(margin, y, pageWidth - margin, y + 34f)
            canvas.drawRoundRect(noteBox, 5f, 5f, Paint().apply { color = Color.rgb(255, 251, 235) })
            canvas.drawRoundRect(noteBox, 5f, 5f, Paint().apply { color = Color.rgb(253, 230, 138); style = Paint.Style.STROKE; strokeWidth = 0.8f })
            canvas.drawText(
                "CATATAN LAMPIRAN LAPORAN HARIAN PUSDALOPS-PB BPBD KABUPATEN MUKOMUKO:",
                margin + 8f,
                y + 11f,
                Paint(boldTextPaint).apply { color = Color.rgb(180, 83, 9); textSize = 7.2f }
            )
            drawWrappedText(
                canvas = canvas,
                text = "Dokumen statistik bulanan ini dihasilkan otomatis dari basis data terpadu SiPagarKita BPBD Mukomuko sebagai lampiran resmi laporan harian Pusdalops-PB untuk evaluasi kesiapsiagaan personil TRC dan distribusi logistik kebencanaan.",
                x = margin + 8f,
                topY = y + 14f,
                maxWidth = (pageWidth - margin * 2 - 16f).toInt(),
                paint = cellPaint,
                maxLines = 3
            )

            y += 42f

            // Keterangan Arsip Digital (tanpa blok tanda tangan pejabat)
            canvas.drawText("Dokumen Resmi Terverifikasi Sistem Informasi SiPagarKita • Pusdalops-PB BPBD Kabupaten Mukomuko", margin, y, cellBoldPaint)

            drawFooterAtribusiResmi(canvas, pageWidth, pageHeight, margin)

            pdfDoc.finishPage(page)

            val safeScope = scopeLabel.replace(Regex("[^A-Za-z0-9]"), "_").take(20)
            val fileName = "Ringkasan_Statistik_Bencana_Bulanan_BPBD_Mukomuko_${selectedYear}_$safeScope.pdf"

            // Simpan ke cacheDir untuk FileProvider Share/View
            val cachePdfFile = File(context.cacheDir, fileName)
            FileOutputStream(cachePdfFile).use { out ->
                pdfDoc.writeTo(out)
            }

            // Simpan juga langsung ke folder Downloads perangkat (Android 10+ / API 29+) tanpa izin penyimpanan
            var savedToDownloads = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    val resolver = context.contentResolver
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    }
                    val downloadUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    if (downloadUri != null) {
                        resolver.openOutputStream(downloadUri)?.use { outStream ->
                            cachePdfFile.inputStream().use { inStream ->
                                inStream.copyTo(outStream)
                            }
                        }
                        savedToDownloads = true
                    }
                } catch (_: Exception) {}
            }

            pdfDoc.close()

            // Buka dialog bagikan / lihat PDF via FileProvider
            try {
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", cachePdfFile)
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(
                        Intent.EXTRA_SUBJECT,
                        "Laporan PDF Statistik Bencana Bulanan BPBD Mukomuko ($selectedYear)"
                    )
                    putExtra(
                        Intent.EXTRA_TEXT,
                        "Terlampir dokumen PDF resmi Ringkasan & Statistik Data Bencana Bulanan ($selectedYear - $scopeLabel) untuk lampiran laporan harian Pusdalops BPBD Kabupaten Mukomuko."
                    )
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                val chooser = Intent.createChooser(shareIntent, "Unduh / Lampirkan PDF Statistik Bulanan").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            } catch (_: Exception) {}

            val toastMsg = if (savedToDownloads) {
                "PDF tersimpan di folder Downloads/$fileName & siap dilampirkan!"
            } else {
                "Dokumen PDF berhasil dibuat & siap dilampirkan!"
            }
            Toast.makeText(context, toastMsg, Toast.LENGTH_LONG).show()

            cachePdfFile
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal mengunduh PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            null
        }
    }

    private fun getCategoryRgbColor(cat: DisasterChartCategory): Int {
        return when (cat) {
            DisasterChartCategory.BANJIR -> Color.rgb(37, 99, 235)
            DisasterChartCategory.CUACA_EKSTREM -> Color.rgb(245, 158, 11)
            DisasterChartCategory.LONGSOR -> Color.rgb(16, 185, 129)
            DisasterChartCategory.ABRASI -> Color.rgb(6, 182, 212)
            DisasterChartCategory.KEBAKARAN -> Color.rgb(239, 68, 68)
            DisasterChartCategory.GEMPA_LAINNYA -> Color.rgb(139, 92, 246)
        }
    }

    private fun getSliceRgbColor(shortLabel: String, fallbackIdx: Int): Int {
        val lower = shortLabel.lowercase(Locale.getDefault())
        return when {
            lower.contains("banjir") -> Color.rgb(37, 99, 235)
            lower.contains("ekstrem") -> Color.rgb(245, 158, 11)
            lower.contains("longsor") || lower.contains("tengah") -> Color.rgb(16, 185, 129)
            lower.contains("abrasi") || lower.contains("pesisir") -> Color.rgb(6, 182, 212)
            lower.contains("karhutla") -> Color.rgb(239, 68, 68)
            lower.contains("selesai") -> Color.rgb(22, 163, 74)
            lower.contains("proses") -> Color.rgb(37, 99, 235)
            lower.contains("verifikasi") -> Color.rgb(245, 158, 11)
            else -> {
                val palette = listOf(
                    Color.rgb(139, 92, 246),
                    Color.rgb(37, 99, 235),
                    Color.rgb(16, 185, 129),
                    Color.rgb(245, 158, 11),
                    Color.rgb(239, 68, 68),
                    Color.rgb(6, 182, 212)
                )
                palette[fallbackIdx % palette.size]
            }
        }
    }
}
