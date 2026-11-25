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
import android.text.TextUtils
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.local.AppDatabase
import com.example.data.local.OfficerAuthManager
import com.example.data.repository.DocumentSettingsRepository
import com.example.model.AirQuality
import com.example.model.CitizenDisasterReport
import com.example.model.CurrentWeather
import com.example.model.DocumentSettings
import com.example.model.MarineWeather
import com.example.model.MukomukoDistricts
import com.example.model.WeatherFullReport
import com.example.ui.components.DisasterChartCategory
import com.example.ui.components.MonthlyDisasterSummary
import com.example.ui.components.PieSliceData
import com.example.ui.components.buildBmkgForecastRows
import com.example.ui.components.buildMonthlyDisasterSummaries
import com.example.ui.components.buildPieSlices
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
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

    private const val A4_PAGE_WIDTH = 595
    private const val A4_PAGE_HEIGHT = 842
    private const val STANDARD_MARGIN = 36f

    @Volatile
    private var cachedMukomukoLogo: Bitmap? = null

    @Volatile
    private var cachedBpbdLogo: Bitmap? = null

    private fun getExportCacheDir(context: Context): File {
        return File(context.cacheDir, "exported_reports").apply {
            if (!exists()) mkdirs()
        }
    }

    private fun sanitizeCsvCell(raw: String?): String {
        val cleaned = (raw ?: "").replace("\r", " ").replace("\n", " ").trim()
        val formulaSafe = if (cleaned.isNotEmpty() && cleaned[0] in charArrayOf('=', '+', '-', '@', '\t')) {
            "'$cleaned"
        } else {
            cleaned
        }
        val escaped = formulaSafe.replace("\"", "\"\"")
        return "\"$escaped\""
    }

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
     * Mengembalikan 4 baris teks Kop Surat Standar Resmi SIPAGAR KITA
     * (mengacu pada format baku Kop Surat Tugas Relawan).
     */
    fun buildStandardKopLines(documentSettings: DocumentSettings? = null): List<String> {
        val settings = (documentSettings ?: DocumentSettings()).clamped()
        return listOf(
            settings.kopHeaderLine1,
            settings.kopHeaderLine2,
            settings.kopHeaderLine3,
            settings.kopAddressLine
        )
    }

    /**
     * Membentuk Kop Surat Standar dalam format teks biasa (untuk ekspor teks / berbagi dokumen non-PDF).
     */
    fun buildStandardKopPlainText(documentSettings: DocumentSettings? = null): String {
        val lines = buildStandardKopLines(documentSettings)
        return buildString {
            lines.forEach { appendLine(it) }
            appendLine("================================================================")
        }
    }

    /**
     * Menulis 4 baris Kop Surat Standar Resmi ke bagian paling atas berkas CSV/Excel.
     */
    fun writeStandardKopSuratCsvHeader(
        writer: java.io.Writer,
        documentSettings: DocumentSettings? = null
    ) {
        buildStandardKopLines(documentSettings).forEach { line ->
            writer.write("${sanitizeCsvCell(line)}\n")
        }
    }

    /**
     * Menggambar **Kop Surat Standar Resmi Aplikasi SIPAGAR KITA** (mengacu pada format Kop Surat
     * Tugas Relawan) secara terstandarisasi untuk seluruh dokumen cetak PDF:
     * - Sebelah KIRI  : Logo Kabupaten Mukomuko (tinggi 44 pt proporsional perisai sesuai Laporan Harian Pusdalops, transparan & tajam)
     * - Bagian TENGAH : 4 baris teks resmi seragam hitam (Serif Bold untuk instansi & Sans-Serif untuk alamat)
     *                   dengan auto-shrink presisi dan batas aman 14 pt dari kedua logo
     * - Sebelah KANAN : Logo BPBD Kabupaten Mukomuko (tinggi 44 pt proporsional lingkaran sejajar, transparan & tajam)
     * - Bawah Kop     : Garis ganda resmi warna hitam pekat (2.2 pt tebal + 0.8 pt tipis)
     *
     * @return Posisi koordinat Y berikutnya setelah garis ganda kop surat + `spacingAfterLine`.
     */
    fun drawKopSuratResmi(
        context: Context,
        canvas: Canvas,
        pageWidth: Int,
        margin: Float = 36f,
        pageNumber: Int = 1,
        documentSettings: DocumentSettings? = null,
        spacingAfterLine: Float = 15f
    ): Float {
        // Kop surat hanya dicetak pada Halaman 1; halaman 2 dst langsung dimulai dari konten.
        if (pageNumber > 1) {
            return margin
        }
        val settings = (documentSettings ?: DocumentSettingsRepository.getCachedOrDefaultSettings(context)).clamped()
        val isSandbox = com.example.training.SandboxTrainingManager.isSandboxModeActive()

        val centerX = pageWidth / 2f
        // Gunakan ukuran (tinggi 44 pt) dan proporsi asli logo Laporan Harian Pusdalops
        // agar bentuk perisai Kabupaten Mukomuko & lingkaran BPBD tajam, proporsional, dan tidak terdistorsi.
        val targetLogoHeight = 44f
        val topTextBaseY = if (isSandbox) 50f else 48f
        val logoTop = topTextBaseY - 6f
        val logoBottom = logoTop + targetLogoHeight

        val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            isDither = true
        }

        // 1. Logo KIRI: Kabupaten Mukomuko (Perisai proporsional sesuai Laporan Harian Pusdalops, transparan & tajam)
        var leftLogoWidth = 36f
        try {
            val leftLogo = getCleanTransparentLogo(context, isMukomukoShield = true)
            if (leftLogo != null && leftLogo.height > 0) {
                val aspect = leftLogo.width.toFloat() / leftLogo.height.toFloat()
                leftLogoWidth = targetLogoHeight * aspect
                val dstLeft = RectF(margin, logoTop, margin + leftLogoWidth, logoBottom)
                canvas.drawBitmap(leftLogo, null, dstLeft, bitmapPaint)
            }
        } catch (_: Exception) {
        }

        // 2. Logo KANAN: BPBD Kabupaten Mukomuko (Lingkaran proporsional sejajar tinggi dengan Logo Kab. Mukomuko)
        var rightLogoWidth = 44f
        try {
            val rightLogo = getCleanTransparentLogo(context, isMukomukoShield = false)
            if (rightLogo != null && rightLogo.height > 0) {
                val aspect = rightLogo.width.toFloat() / rightLogo.height.toFloat()
                rightLogoWidth = targetLogoHeight * aspect
                val dstRight = RectF(
                    pageWidth - margin - rightLogoWidth,
                    logoTop,
                    pageWidth - margin,
                    logoBottom
                )
                canvas.drawBitmap(rightLogo, null, dstRight, bitmapPaint)
            }
        } catch (_: Exception) {
        }

        val logoTextClearance = 14f
        val maxLogoReserve = max(leftLogoWidth, rightLogoWidth) + logoTextClearance
        val maxKopTextWidth = (pageWidth - (margin + maxLogoReserve) * 2f).coerceAtLeast(180f)

        // 2. Teks Kop Surat Seragam Hitam Resmi (Serif Bold untuk Instansi, Sans-Serif Normal untuk Alamat)
        val kopTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val kopAddressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }

        fun drawCenteredFitKopLine(
            text: String,
            yPos: Float,
            basePaint: Paint,
            defaultSize: Float,
            minSize: Float = 6.8f
        ) {
            val workingPaint = Paint(basePaint).apply {
                textSize = defaultSize
            }
            val measured = workingPaint.measureText(text)
            if (measured > maxKopTextWidth && measured > 0f) {
                workingPaint.textSize = (defaultSize * (maxKopTextWidth / measured)).coerceAtLeast(minSize)
            }
            canvas.drawText(text, centerX, yPos, workingPaint)
        }

        var y = topTextBaseY
        drawCenteredFitKopLine(
            text = settings.kopHeaderLine1,
            yPos = y,
            basePaint = kopTitlePaint,
            defaultSize = 12.5f,
            minSize = settings.minFontSize.coerceAtLeast(7.0f)
        )
        y += 16f
        drawCenteredFitKopLine(
            text = settings.kopHeaderLine2,
            yPos = y,
            basePaint = kopTitlePaint,
            defaultSize = 13.5f,
            minSize = settings.minFontSize.coerceAtLeast(7.0f)
        )
        y += 14f
        drawCenteredFitKopLine(
            text = settings.kopHeaderLine3,
            yPos = y,
            basePaint = kopTitlePaint,
            defaultSize = 9.2f,
            minSize = settings.minFontSize.coerceAtLeast(6.5f)
        )
        y += 12f
        drawCenteredFitKopLine(
            text = settings.kopAddressLine,
            yPos = y,
            basePaint = kopAddressPaint,
            defaultSize = 8.4f,
            minSize = settings.minFontSize.coerceAtLeast(6.4f)
        )

        // 3. Garis Penutup Kop Surat Ganda Resmi Warna Hitam (diletakkan aman di bawah batas bawah logo)
        y = maxOf(y + 10f, logoBottom + 10f)
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            strokeWidth = 2.2f
        }
        canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
        y += 3f
        linePaint.strokeWidth = 0.8f
        canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)

        if (isSandbox) {
            drawSandboxTrainingWatermark(canvas, pageWidth, A4_PAGE_HEIGHT, margin)
        }

        return y + spacingAfterLine
    }

    /**
     * Menggambar pita peringatan atas & watermark diagonal transparan pada halaman PDF
     * selama Mode Latihan Petugas (Sandbox) aktif agar dokumen latihan tidak pernah tertukar
     * dengan rilis resmi Pusdalops-PB BPBD Kabupaten Mukomuko.
     */
    fun drawSandboxTrainingWatermark(
        canvas: Canvas,
        pageWidth: Int,
        pageHeight: Int,
        margin: Float = 30f
    ) {
        // 1. Pita merah/oranye di bagian paling atas halaman
        val bannerRect = RectF(margin, 14f, pageWidth - margin, 32f)
        val bannerFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(254, 243, 199)
            style = Paint.Style.FILL
        }
        val bannerStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(217, 119, 6)
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
        }
        val bannerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(180, 83, 9)
            textSize = 8.5f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawRoundRect(bannerRect, 4f, 4f, bannerFill)
        canvas.drawRoundRect(bannerRect, 4f, 4f, bannerStroke)
        canvas.drawText(
            "★ ${com.example.training.SandboxTrainingManager.WATERMARK_HEADER_TEXT} ★",
            pageWidth / 2f,
            26f,
            bannerTextPaint
        )

        // 2. Watermark diagonal tipis di tengah halaman
        canvas.save()
        canvas.rotate(-32f, pageWidth / 2f, pageHeight / 2f)
        val watermarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(34, 217, 119, 6)
            textSize = 26f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            "SIMULASI LATIHAN PETUGAS - NON PRODUKSI",
            pageWidth / 2f,
            pageHeight / 2f,
            watermarkPaint
        )
        canvas.restore()
    }

    /**
     * Menggambar atribusi footer resmi di bagian paling bawah setiap halaman PDF (Data-Driven).
     */
    fun drawFooterAtribusiResmi(
        canvas: Canvas,
        pageWidth: Int,
        pageHeight: Int,
        margin: Float = 30f,
        documentSettings: DocumentSettings? = null
    ) {
        val settings = (documentSettings ?: DocumentSettingsRepository.getCachedOrDefaultSettings(null)).clamped()
        val footerLineY = pageHeight - 28f
        val footerTextY = pageHeight - 16f

        val linePaint = Paint().apply {
            color = Color.rgb(203, 213, 225)
            strokeWidth = 0.6f
            isAntiAlias = true
        }
        val isSandbox = com.example.training.SandboxTrainingManager.isSandboxModeActive()
        val footerPaint = Paint().apply {
            color = if (isSandbox) Color.rgb(180, 83, 9) else Color.rgb(100, 116, 139)
            textSize = 7.2f
            isFakeBoldText = isSandbox
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        val effectiveFooterText = if (isSandbox) {
            "${com.example.training.SandboxTrainingManager.WATERMARK_SHORT_BADGE} • ${settings.footerText}"
        } else {
            settings.footerText
        }

        canvas.drawLine(margin, footerLineY, pageWidth - margin, footerLineY, linePaint)
        PdfTextAutoShrinkHelper.drawAutoShrinkText(
            canvas = canvas,
            text = effectiveFooterText,
            x = pageWidth / 2f,
            y = footerTextY,
            maxWidth = pageWidth - (margin * 2f),
            basePaint = footerPaint,
            initialFontSize = 7.2f,
            minFontSize = settings.minFontSize
        )
    }

    /**
     * Menggambar blok tanda tangan Tata Naskah Dinas dengan formasi Piramida Terbalik (3 Kolom)
     * secara 100% Data-Driven (mengambil nama, gelar, jabatan, NIP dari `DocumentSettings` / API)
     * disertai Auto-Shrink Text agar teks nama/gelar panjang mengecil otomatis di dalam batas kolom.
     *
     * @return Posisi Y akhir setelah blok tanda tangan.
     */
    fun drawBlokTandaTanganPiramidaTerbalik(
        canvas: Canvas,
        pageWidth: Int,
        startY: Float,
        margin: Float = 36f,
        context: Context? = null,
        documentSettings: DocumentSettings? = null
    ): Float {
        val settings = (documentSettings ?: DocumentSettingsRepository.getCachedOrDefaultSettings(context)).clamped()
        val signatories = DocumentSettingsRepository.resolveSignatories(context, settings)
        val koordSig = signatories[DocumentSettings.SLOT_KOORD_ADMIN]
        val manajerSig = signatories[DocumentSettings.SLOT_MANAJER_PUSDALOPS]
        val pengawasSig = signatories[DocumentSettings.SLOT_PENGAWAS]

        val usableWidth = pageWidth - (margin * 2f)
        val colWidth = usableWidth / 3f
        val sideBoxMaxW = (colWidth - 8f).coerceAtLeast(60f)
        val centerBoxMaxW = (usableWidth * 0.56f).coerceAtLeast(sideBoxMaxW)

        // Titik tengah masing-masing kolom (3 kolom: Kiri, Tengah, Kanan)
        val leftColCenterX = margin + (colWidth * 0.5f)
        val centerColCenterX = pageWidth / 2f
        val rightColCenterX = pageWidth - margin - (colWidth * 0.5f)

        val roleFontSize = (settings.fontSize - 0.5f).coerceAtLeast(settings.minFontSize)
        val nameFontSize = settings.fontSize
        val nipFontSize = (settings.fontSize - 0.8f).coerceAtLeast(settings.minFontSize)

        val rolePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 41, 59)
            textSize = roleFontSize
            textAlign = Paint.Align.CENTER
        }
        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = nameFontSize
            isFakeBoldText = true
            isUnderlineText = true
            textAlign = Paint.Align.CENTER
        }
        val nipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(51, 65, 85)
            textSize = nipFontSize
            textAlign = Paint.Align.CENTER
        }

        // --- BARIS 1: Kiri (Koord. Administrasi) & Kanan (Manajer Pusdalops PB) ---
        var yRow1 = startY
        koordSig?.let {
            PdfTextAutoShrinkHelper.drawAutoShrinkText(canvas, it.roleLine1, leftColCenterX, yRow1, sideBoxMaxW, rolePaint, roleFontSize, settings.minFontSize)
        }
        manajerSig?.let {
            PdfTextAutoShrinkHelper.drawAutoShrinkText(canvas, it.roleLine1, rightColCenterX, yRow1, sideBoxMaxW, rolePaint, roleFontSize, settings.minFontSize)
        }

        yRow1 += 11f
        koordSig?.let {
            PdfTextAutoShrinkHelper.drawAutoShrinkText(canvas, it.roleLine2, leftColCenterX, yRow1, sideBoxMaxW, rolePaint, roleFontSize, settings.minFontSize)
        }
        manajerSig?.let {
            PdfTextAutoShrinkHelper.drawAutoShrinkText(canvas, it.roleLine2, rightColCenterX, yRow1, sideBoxMaxW, rolePaint, roleFontSize, settings.minFontSize)
        }

        // Ruang tanda tangan basah/stempel
        yRow1 += 38f
        koordSig?.let {
            PdfTextAutoShrinkHelper.drawAutoShrinkText(canvas, it.formattedName, leftColCenterX, yRow1, sideBoxMaxW, namePaint, nameFontSize, settings.minFontSize)
        }
        manajerSig?.let {
            PdfTextAutoShrinkHelper.drawAutoShrinkText(canvas, it.formattedName, rightColCenterX, yRow1, sideBoxMaxW, namePaint, nameFontSize, settings.minFontSize)
        }

        yRow1 += 11f
        koordSig?.let {
            PdfTextAutoShrinkHelper.drawAutoShrinkText(canvas, it.nipLine, leftColCenterX, yRow1, sideBoxMaxW, nipPaint, nipFontSize, settings.minFontSize)
        }
        manajerSig?.let {
            PdfTextAutoShrinkHelper.drawAutoShrinkText(canvas, it.nipLine, rightColCenterX, yRow1, sideBoxMaxW, nipPaint, nipFontSize, settings.minFontSize)
        }

        // --- BARIS 2 (Merge 3 Kolom - Bawah Tengah): Pengawas ---
        var yRow2 = yRow1 + 22f
        pengawasSig?.let {
            PdfTextAutoShrinkHelper.drawAutoShrinkText(canvas, it.roleLine1, centerColCenterX, yRow2, centerBoxMaxW, rolePaint, roleFontSize, settings.minFontSize)
        }
        yRow2 += 11f
        pengawasSig?.let {
            PdfTextAutoShrinkHelper.drawAutoShrinkText(canvas, it.roleLine2, centerColCenterX, yRow2, centerBoxMaxW, rolePaint, roleFontSize, settings.minFontSize)
        }

        // Ruang tanda tangan Pengawas
        yRow2 += 38f
        pengawasSig?.let {
            PdfTextAutoShrinkHelper.drawAutoShrinkText(canvas, it.formattedName, centerColCenterX, yRow2, centerBoxMaxW, namePaint, nameFontSize, settings.minFontSize)
        }
        yRow2 += 11f
        pengawasSig?.let {
            PdfTextAutoShrinkHelper.drawAutoShrinkText(canvas, it.nipLine, centerColCenterX, yRow2, centerBoxMaxW, nipPaint, nipFontSize, settings.minFontSize)
        }

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
        val staticLayout = buildAutoFitStaticLayout(
            text = text,
            maxWidth = maxWidth,
            paint = paint,
            maxLines = maxLines,
            minTextSize = paint.textSize
        )

        canvas.save()
        canvas.translate(x, topY)
        staticLayout.draw(canvas)
        canvas.restore()

        return staticLayout.height.toFloat()
    }

    /**
     * Membangun StaticLayout dengan word-wrap, jarak spasi antar-kata yang tegas, dan pengecilan
     * textSize otomatis hingga minTextSize agar teks panjang tetap muat rapi di dalam lebar kolom
     * tanpa kata-kata tampak menempel pada hasil PDF.
     */
    private fun buildAutoFitStaticLayout(
        text: String,
        maxWidth: Int,
        paint: Paint,
        maxLines: Int = 2,
        minTextSize: Float = 5.8f,
        lineSpacingExtra: Float = 2.0f,
        lineSpacingMultiplier: Float = 1.2f
    ): StaticLayout {
        val rawSafe = text.ifBlank { "-" }
        val safeText = PdfTextAutoShrinkHelper.normalizePdfWordSpacing(rawSafe)
        val safeWidth = maxWidth.coerceAtLeast(20)
        var currentSize = paint.textSize
        var layout: StaticLayout
        while (true) {
            val tp = PdfTextAutoShrinkHelper.configurePdfPaint(
                TextPaint(paint).apply {
                    isAntiAlias = true
                    textSize = currentSize
                },
                letterSpacingEm = 0.032f
            )
            val fullLayout = StaticLayout.Builder
                .obtain(safeText, 0, safeText.length, tp, safeWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(lineSpacingExtra, lineSpacingMultiplier)
                .setIncludePad(false)
                .build()
            val longestLineWidth = (0 until fullLayout.lineCount).maxOfOrNull { fullLayout.getLineWidth(it) } ?: 0f
            val fitsLinesAndWidth = fullLayout.lineCount <= maxLines && longestLineWidth <= safeWidth + 0.5f
            if (fitsLinesAndWidth || currentSize <= minTextSize) {
                layout = StaticLayout.Builder
                    .obtain(safeText, 0, safeText.length, tp, safeWidth)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(lineSpacingExtra, lineSpacingMultiplier)
                    .setIncludePad(false)
                    .setMaxLines(maxLines)
                    .setEllipsize(TextUtils.TruncateAt.END)
                    .build()
                break
            }
            currentSize = (currentSize - 0.35f).coerceAtLeast(minTextSize)
        }
        return layout
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
        if (com.example.training.SandboxTrainingManager.isSandboxModeActive() ||
            reports.any { com.example.training.SandboxTrainingManager.isSandboxReportId(it.id) }
        ) {
            com.example.training.SandboxTrainingManager.markTrainingStepCompleted(
                com.example.training.SandboxTrainingManager.STEP_4_PRINT_PDF
            )
        }

        val pdfDoc = PdfDocument()
        try {
            val pageWidth = A4_PAGE_WIDTH
            val pageHeight = A4_PAGE_HEIGHT
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

            val settings = DocumentSettingsRepository.getCachedOrDefaultSettings(context).clamped()
            var y = drawKopSuratResmi(context, canvas, pageWidth, margin = 36f, documentSettings = settings)
            val resolvedOfficer = DocumentSettingsRepository.resolveOfficerIdentity(
                context = context,
                rawName = officerName,
                rawNip = officerNip,
                settings = settings
            )

            // Document Title
            canvas.drawText("REKAPITULASI RESMI LAPORAN BENCANA WARGA TERVALIDASI", (pageWidth / 2).toFloat(), y, titlePaint)
            y += 14f
            textPaint.textAlign = Paint.Align.LEFT
            PdfTextAutoShrinkHelper.drawAutoShrinkText(
                canvas = canvas,
                text = "Waktu Cetak: $printDate",
                x = 36f,
                y = y,
                maxWidth = (pageWidth - 72).toFloat(),
                basePaint = textPaint,
                initialFontSize = settings.bodyFontSize,
                minFontSize = settings.minFontSize
            )
            y += 10.5f
            PdfTextAutoShrinkHelper.drawAutoShrinkText(
                canvas = canvas,
                text = "Petugas Validasi: ${resolvedOfficer.formattedName} (${resolvedOfficer.nipLine})",
                x = 36f,
                y = y,
                maxWidth = (pageWidth - 72).toFloat(),
                basePaint = textPaint,
                initialFontSize = settings.bodyFontSize,
                minFontSize = settings.minFontSize
            )
            y += 10f

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
                    drawFooterAtribusiResmi(currentCanvas, pageWidth, pageHeight, margin = 36f, documentSettings = settings)
                    pdfDoc.finishPage(currentPage)

                    currentPageNumber += 1
                    val nextPageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
                    currentPage = pdfDoc.startPage(nextPageInfo)
                    currentCanvas = currentPage.canvas

                    // Halaman 2+ langsung dimulai dari konten tabel tanpa Kop Surat
                    y = drawKopSuratResmi(context, currentCanvas, pageWidth, margin = 36f, pageNumber = currentPageNumber, documentSettings = settings)
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
                val noteText = settings.truncateIfNeeded(rep.additionalNotes.ifBlank { "-" })

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

            // Lampiran Bukti Foto Dokumentasi Lapangan untuk Pelaporan Berjenjang
            val reportsWithPhotos = reports.mapNotNull { rep ->
                val bmp = ImageUtils.decodePhotoSourceToBitmap(rep.photoUri, context)
                    ?: ImageUtils.getInlineDataUriForReport(rep.id)?.let { ImageUtils.decodePhotoSourceToBitmap(it, context) }
                if (bmp != null && !bmp.isRecycled && bmp.width > 1 && bmp.height > 1) {
                    rep to bmp
                } else {
                    null
                }
            }

            if (reportsWithPhotos.isNotEmpty()) {
                val photoCardHeight = 132f
                val photoGap = 14f
                val cardWidth = ((pageWidth - 72f) - photoGap) / 2f
                val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(148, 163, 184)
                    style = Paint.Style.STROKE
                    strokeWidth = 0.9f
                }
                val photoBitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

                if (y + 32f + photoCardHeight > maxRowBottomY) {
                    drawFooterAtribusiResmi(currentCanvas, pageWidth, pageHeight, margin = 36f, documentSettings = settings)
                    pdfDoc.finishPage(currentPage)
                    currentPageNumber += 1
                    val nextPageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
                    currentPage = pdfDoc.startPage(nextPageInfo)
                    currentCanvas = currentPage.canvas
                    y = drawKopSuratResmi(context, currentCanvas, pageWidth, margin = 36f, pageNumber = currentPageNumber, documentSettings = settings)
                } else {
                    y += 12f
                }

                currentCanvas.drawRect(36f, y, (pageWidth - 36).toFloat(), y + 18f, headerBoxPaint)
                currentCanvas.drawText(
                    "LAMPIRAN BUKTI FOTO DOKUMENTASI LAPANGAN (BUKTI DUKUNG PELAPORAN BERJENJANG)",
                    42f,
                    y + 12f,
                    boldPaint
                )
                y += 24f

                val chunkedPhotos = reportsWithPhotos.chunked(2)
                for (pair in chunkedPhotos) {
                    if (y + photoCardHeight > maxRowBottomY) {
                        drawFooterAtribusiResmi(currentCanvas, pageWidth, pageHeight, margin = 36f, documentSettings = settings)
                        pdfDoc.finishPage(currentPage)
                        currentPageNumber += 1
                        val nextPageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
                        currentPage = pdfDoc.startPage(nextPageInfo)
                        currentCanvas = currentPage.canvas
                        y = drawKopSuratResmi(context, currentCanvas, pageWidth, margin = 36f, pageNumber = currentPageNumber, documentSettings = settings)
                    }

                    pair.forEachIndexed { colIdx, (rep, bmp) ->
                        val leftX = 36f + colIdx * (cardWidth + photoGap)
                        val cardRect = RectF(leftX, y, leftX + cardWidth, y + photoCardHeight)
                        val imgRect = RectF(leftX + 4f, y + 4f, leftX + cardWidth - 4f, y + 96f)
                        currentCanvas.drawBitmap(bmp, null, imgRect, photoBitmapPaint)
                        currentCanvas.drawRect(cardRect, framePaint)
                        val captionMaxW = (cardWidth - 12f).coerceAtLeast(80f)
                        PdfTextAutoShrinkHelper.drawAutoShrinkText(
                            canvas = currentCanvas,
                            text = "#${rep.id.takeLast(7)} • ${rep.disasterType}",
                            x = leftX + 6f,
                            y = y + 108f,
                            maxWidth = captionMaxW,
                            basePaint = boldPaint,
                            initialFontSize = boldPaint.textSize,
                            minFontSize = settings.minFontSize
                        )
                        PdfTextAutoShrinkHelper.drawAutoShrinkText(
                            canvas = currentCanvas,
                            text = "${rep.village}, ${rep.district} (GPS: %.4f, %.4f)".format(rep.latitude, rep.longitude),
                            x = leftX + 6f,
                            y = y + 119f,
                            maxWidth = captionMaxW,
                            basePaint = textPaint,
                            initialFontSize = textPaint.textSize,
                            minFontSize = settings.minFontSize
                        )
                        PdfTextAutoShrinkHelper.drawAutoShrinkText(
                            canvas = currentCanvas,
                            text = "Pelapor: ${rep.reporterName} • Status: ${rep.status}",
                            x = leftX + 6f,
                            y = y + 128f,
                            maxWidth = captionMaxW,
                            basePaint = textPaint,
                            initialFontSize = textPaint.textSize,
                            minFontSize = settings.minFontSize
                        )
                    }
                    y += photoCardHeight + 10f
                }
            }

            // Tanpa blok tanda tangan pejabat (khusus Rekapitulasi Laporan Warga)
            drawFooterAtribusiResmi(currentCanvas, pageWidth, pageHeight, margin = 36f, documentSettings = settings)

            pdfDoc.finishPage(currentPage)

            // Save PDF to dedicated exported_reports cache subdirectory
            val pdfFile = File(getExportCacheDir(context), "Laporan_Bencana_Pusdalops_BPBD_Mukomuko.pdf")
            FileOutputStream(pdfFile).use { out ->
                pdfDoc.writeTo(out)
            }

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
        } finally {
            try {
                pdfDoc.close()
            } catch (_: Exception) {}
        }
    }

    /**
     * Menghasilkan berkas CSV agar data laporan warga dapat diolah di Microsoft Excel / Google Sheets.
     */
    fun exportToCsv(
        context: Context,
        reports: List<CitizenDisasterReport>,
        officerName: String = "",
        officerNip: String = ""
    ) {
        if (reports.isEmpty()) {
            Toast.makeText(context, "Tidak ada data laporan untuk diekspor ke CSV", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            if (com.example.training.SandboxTrainingManager.isSandboxModeActive() ||
                reports.any { com.example.training.SandboxTrainingManager.isSandboxReportId(it.id) }
            ) {
                com.example.training.SandboxTrainingManager.markTrainingStepCompleted(
                    com.example.training.SandboxTrainingManager.STEP_4_PRINT_PDF
                )
            }
            val settings = DocumentSettingsRepository.getCachedOrDefaultSettings(context).clamped()
            val resolvedOfficer = DocumentSettingsRepository.resolveOfficerIdentity(
                context = context,
                rawName = officerName,
                rawNip = officerNip,
                settings = settings
            )
            val csvFile = File(getExportCacheDir(context), "Laporan_Bencana_Pusdalops_Mukomuko.csv")
            csvFile.bufferedWriter(Charsets.UTF_8).use { writer ->
                writer.write("\uFEFF")
                writeStandardKopSuratCsvHeader(writer, settings)
                writer.write("\"DOKUMEN RESMI: REKAPITULASI LAPORAN BENCANA WARGA TERVALIDASI\"\n")
                if (com.example.training.SandboxTrainingManager.isSandboxModeActive()) {
                    writer.write("\"# ${com.example.training.SandboxTrainingManager.WATERMARK_HEADER_TEXT}\"\n")
                }
                writer.write("\n")
                // CSV Header (Dilengkapi Nama & NIP Petugas Validasi yang tersinkronisasi & Data-Driven)
                writer.write("No,ID_Laporan,Jenis_Bencana,Waktu_Kejadian,Kecamatan,Desa,Detail_Lokasi,Latitude,Longitude,Akurasi_GPS_m,Korban_Meninggal,Korban_Luka,Korban_Hilang,Status,Catatan_Tindak_Lanjut_TRC,Nama_Pelapor,Nomor_Telepon,Petugas_Validasi,NIP_Petugas_Validasi\n")

                // CSV Data Rows (sanitized against CSV formula injection & RFC-4180 quoted)
                reports.forEachIndexed { idx, rep ->
                    val row = listOf(
                        "${idx + 1}",
                        sanitizeCsvCell(rep.id),
                        sanitizeCsvCell(rep.disasterType),
                        sanitizeCsvCell(rep.incidentTime),
                        sanitizeCsvCell(rep.district),
                        sanitizeCsvCell(rep.village),
                        sanitizeCsvCell(rep.incidentLocation),
                        "${rep.latitude}",
                        "${rep.longitude}",
                        "${rep.gpsAccuracyMeters ?: 0f}",
                        "${rep.casualtiesDead}",
                        "${rep.casualtiesInjured}",
                        "${rep.casualtiesMissing}",
                        sanitizeCsvCell(rep.status),
                        sanitizeCsvCell(rep.additionalNotes),
                        sanitizeCsvCell(rep.reporterName),
                        sanitizeCsvCell(rep.reporterPhone),
                        sanitizeCsvCell(resolvedOfficer.formattedName),
                        sanitizeCsvCell(resolvedOfficer.nip)
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
        val outputFile = File(getExportCacheDir(context), "Peta_Cuaca_15_Kecamatan_Mukomuko_${System.currentTimeMillis()}.pdf")
        try {
            val pageWidth = A4_PAGE_WIDTH
            val pageHeight = A4_PAGE_HEIGHT
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

        val margin = STANDARD_MARGIN
        val settings = DocumentSettingsRepository.getCachedOrDefaultSettings(context).clamped()
        var y = drawKopSuratResmi(context, canvas, pageWidth, margin, documentSettings = settings)

        // Judul Dokumen
        val docTitlePaint = Paint().apply {
            color = Color.rgb(194, 65, 12)
            textSize = 11f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        PdfTextAutoShrinkHelper.drawAutoShrinkText(
            canvas = canvas,
            text = "BULETIN PETA CUACA & PERINGATAN DINI 15 KECAMATAN",
            x = pageWidth / 2f,
            y = y,
            maxWidth = pageWidth - (margin * 2f),
            basePaint = docTitlePaint,
            initialFontSize = 11f,
            minFontSize = 8.5f
        )
        y += 14f

        val sdf = SimpleDateFormat("EEEE, dd MMMM yyyy - HH:mm 'WIB'", Locale.forLanguageTag("id-ID"))
        val currentTimeStr = sdf.format(Date())
        val resolvedOfficer = DocumentSettingsRepository.resolveOfficerIdentity(
            context = context,
            settings = settings
        )
        val metaPaint = Paint().apply {
            color = Color.rgb(71, 85, 105)
            textSize = 8.0f
            textAlign = Paint.Align.CENTER
        }
        PdfTextAutoShrinkHelper.drawAutoShrinkText(
            canvas = canvas,
            text = "Waktu Penerbitan: $currentTimeStr  |  Mode Overlay: $activeLayerName",
            x = pageWidth / 2f,
            y = y,
            maxWidth = pageWidth - (margin * 2f),
            basePaint = metaPaint,
            initialFontSize = settings.bodyFontSize,
            minFontSize = settings.minFontSize
        )
        y += 10.5f
        PdfTextAutoShrinkHelper.drawAutoShrinkText(
            canvas = canvas,
            text = "Petugas: ${resolvedOfficer.formattedName} (${resolvedOfficer.nipLine})",
            x = pageWidth / 2f,
            y = y,
            maxWidth = pageWidth - (margin * 2f),
            basePaint = metaPaint,
            initialFontSize = settings.bodyFontSize,
            minFontSize = settings.minFontSize
        )
        y += 14f

        // Kotak Ringkasan Ekstrem Cuaca (Presisi dalam batas margin halaman A4)
        val maxRain = items.maxByOrNull { it.precipitation }
        val maxTemp = items.maxByOrNull { it.temperature }
        val minTemp = items.minByOrNull { it.temperature }
        val maxWind = items.maxByOrNull { it.windSpeed }

        val tableLeft = margin
        val tableRight = pageWidth - margin
        val tableWidth = pageWidth - (margin * 2f) // pageWidth - 72f = 523f untuk A4 (595 - 72)

        val summaryTitlePaint = Paint(boldTextPaint).apply {
            color = Color.rgb(153, 27, 27)
            textSize = 7.8f
            textAlign = Paint.Align.LEFT
        }
        val summaryBodyPaint = Paint(textPaint).apply {
            color = Color.rgb(51, 65, 85)
            textSize = 7.4f
            textAlign = Paint.Align.LEFT
        }

        val summaryColWidth = ((tableWidth - 28f) / 2f).toInt()
        val leftSummaryText =
            "• Curah Hujan Tertinggi: ${maxRain?.districtName ?: "-"} (${maxRain?.precipitation ?: 0.0} mm/j)\n" +
            "• Suhu Ekstrem: Maks ${maxTemp?.districtName ?: "-"} (${maxTemp?.temperature ?: 0.0}°C) | Min ${minTemp?.districtName ?: "-"} (${minTemp?.temperature ?: 0.0}°C)"
        val rightSummaryText =
            "• Angin Maksimal: ${maxWind?.districtName ?: "-"} (${maxWind?.windSpeed?.toInt() ?: 0} km/j - ${maxWind?.windDirection ?: "-"})\n" +
            "• Pemantauan: Siaga Sensor Telemetri & Stasiun BMKG"

        val leftSummaryLayout = buildAutoFitStaticLayout(
            text = leftSummaryText,
            maxWidth = summaryColWidth,
            paint = summaryBodyPaint,
            maxLines = 4,
            minTextSize = 6.5f
        )
        val rightSummaryLayout = buildAutoFitStaticLayout(
            text = rightSummaryText,
            maxWidth = summaryColWidth,
            paint = summaryBodyPaint,
            maxLines = 4,
            minTextSize = 6.5f
        )

        val summaryContentH = max(leftSummaryLayout.height, rightSummaryLayout.height).toFloat()
        val summaryBoxHeight = 20f + summaryContentH + 7f
        val summaryRect = RectF(tableLeft, y, tableRight, y + summaryBoxHeight)
        canvas.drawRoundRect(summaryRect, 6f, 6f, Paint().apply { color = Color.rgb(254, 242, 242) })
        canvas.drawRoundRect(summaryRect, 6f, 6f, Paint().apply { color = Color.rgb(252, 165, 165); style = Paint.Style.STROKE; strokeWidth = 1f })

        PdfTextAutoShrinkHelper.drawAutoShrinkText(
            canvas = canvas,
            text = "RINGKASAN PARAMETER METEOROLOGI KABUPATEN MUKOMUKO",
            x = tableLeft + 10f,
            y = y + 13f,
            maxWidth = tableWidth - 20f,
            basePaint = summaryTitlePaint,
            initialFontSize = 7.8f,
            minFontSize = 6.5f
        )

        canvas.save()
        canvas.translate(tableLeft + 10f, y + 18f)
        leftSummaryLayout.draw(canvas)
        canvas.restore()

        canvas.save()
        canvas.translate(tableLeft + 18f + summaryColWidth, y + 18f)
        rightSummaryLayout.draw(canvas)
        canvas.restore()

        y += summaryBoxHeight + 10f

        // Tabel 15 Kecamatan - Pembagian Lebar Kolom Dinamis 8 Kolom (Total = 100% dari pageWidth - 72f)
        val colWeights = floatArrayOf(
            0.15f, // 0: Kecamatan (15%)
            0.18f, // 1: Karakter Wilayah (18%)
            0.10f, // 2: Hujan (10%)
            0.10f, // 3: Suhu (10%)
            0.10f, // 4: Terasa (10%)
            0.10f, // 5: Angin (10%)
            0.08f, // 6: RH (8%)
            0.19f  // 7: Kondisi (19%)
        )
        val colBounds = FloatArray(colWeights.size + 1)
        colBounds[0] = tableLeft
        for (i in colWeights.indices) {
            colBounds[i + 1] = colBounds[i] + (tableWidth * colWeights[i])
        }
        colBounds[colWeights.size] = tableRight // Pastikan batas paling kanan tepat di pageWidth - margin

        fun colStart(idx: Int) = colBounds[idx]
        fun colWidth(idx: Int) = colBounds[idx + 1] - colBounds[idx]
        fun colCenter(idx: Int) = (colBounds[idx] + colBounds[idx + 1]) / 2f

        val headerTextLeftPaint = Paint(boldTextPaint).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 7.3f
            textAlign = Paint.Align.LEFT
        }
        val headerTextCenterPaint = Paint(boldTextPaint).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 7.3f
            textAlign = Paint.Align.CENTER
        }
        val cellTextLeftPaint = Paint(textPaint).apply {
            color = Color.rgb(51, 65, 85)
            textSize = 7.2f
            textAlign = Paint.Align.LEFT
        }
        val cellBoldLeftPaint = Paint(boldTextPaint).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 7.2f
            textAlign = Paint.Align.LEFT
        }
        val cellTextCenterPaint = Paint(textPaint).apply {
            color = Color.rgb(51, 65, 85)
            textSize = 7.3f
            textAlign = Paint.Align.CENTER
        }

        // Table Header
        val tableStartY = y
        val headerHeight = 23f
        val thRect = RectF(tableLeft, y, tableRight, y + headerHeight)
        canvas.drawRect(thRect, headerBgPaint)
        canvas.drawLine(tableLeft, y, tableRight, y, linePaint)
        canvas.drawLine(tableLeft, y + headerHeight, tableRight, y + headerHeight, linePaint)

        val headerBaselineY = y + 14f
        PdfTextAutoShrinkHelper.drawPdfText(canvas, "Kecamatan", colStart(0) + 4f, headerBaselineY, headerTextLeftPaint)
        PdfTextAutoShrinkHelper.drawPdfText(canvas, "Karakter Wilayah", colStart(1) + 4f, headerBaselineY, headerTextLeftPaint)
        PdfTextAutoShrinkHelper.drawPdfText(canvas, "Hujan", colCenter(2), headerBaselineY - 3f, headerTextCenterPaint)
        PdfTextAutoShrinkHelper.drawPdfText(canvas, "(mm/j)", colCenter(2), headerBaselineY + 5f, Paint(headerTextCenterPaint).apply { textSize = 6.2f })
        PdfTextAutoShrinkHelper.drawPdfText(canvas, "Suhu", colCenter(3), headerBaselineY - 3f, headerTextCenterPaint)
        PdfTextAutoShrinkHelper.drawPdfText(canvas, "(°C)", colCenter(3), headerBaselineY + 5f, Paint(headerTextCenterPaint).apply { textSize = 6.2f })
        PdfTextAutoShrinkHelper.drawPdfText(canvas, "Terasa", colCenter(4), headerBaselineY - 3f, headerTextCenterPaint)
        PdfTextAutoShrinkHelper.drawPdfText(canvas, "(°C)", colCenter(4), headerBaselineY + 5f, Paint(headerTextCenterPaint).apply { textSize = 6.2f })
        PdfTextAutoShrinkHelper.drawPdfText(canvas, "Angin", colCenter(5), headerBaselineY - 3f, headerTextCenterPaint)
        PdfTextAutoShrinkHelper.drawPdfText(canvas, "(km/j)", colCenter(5), headerBaselineY + 5f, Paint(headerTextCenterPaint).apply { textSize = 6.2f })
        PdfTextAutoShrinkHelper.drawPdfText(canvas, "RH (%)", colCenter(6), headerBaselineY, headerTextCenterPaint)
        PdfTextAutoShrinkHelper.drawPdfText(canvas, "Kondisi", colStart(7) + 4f, headerBaselineY, headerTextLeftPaint)
        y += headerHeight

        // Table Rows (15 Kecamatan) dengan tinggi baris dinamis & StaticLayout auto-fit
        items.forEachIndexed { index, item ->
            val kecLayout = buildAutoFitStaticLayout(
                text = "${index + 1}. ${item.districtName}",
                maxWidth = (colWidth(0) - 8f).toInt(),
                paint = cellBoldLeftPaint,
                maxLines = 2,
                minTextSize = 5.8f
            )
            val katLayout = buildAutoFitStaticLayout(
                text = item.category,
                maxWidth = (colWidth(1) - 8f).toInt(),
                paint = cellTextLeftPaint,
                maxLines = 2,
                minTextSize = 5.8f
            )
            val kondisiLayout = buildAutoFitStaticLayout(
                text = item.weatherDesc,
                maxWidth = (colWidth(7) - 8f).toInt(),
                paint = cellTextLeftPaint,
                maxLines = 2,
                minTextSize = 5.8f
            )

            val maxTextHeight = max(kecLayout.height, max(katLayout.height, kondisiLayout.height)).toFloat()
            val rowHeight = max(20f, maxTextHeight + 8f)
            val rowBaselineY = y + (rowHeight / 2f) + 2.5f

            if (index % 2 == 1) {
                canvas.drawRect(RectF(tableLeft, y, tableRight, y + rowHeight), Paint().apply { color = Color.rgb(248, 250, 252) })
            }

            // Kolom 0: Kecamatan (StaticLayout vertikal rata tengah)
            canvas.save()
            canvas.translate(colStart(0) + 4f, y + (rowHeight - kecLayout.height) / 2f)
            kecLayout.draw(canvas)
            canvas.restore()

            // Kolom 1: Karakter Wilayah (StaticLayout vertikal rata tengah)
            canvas.save()
            canvas.translate(colStart(1) + 4f, y + (rowHeight - katLayout.height) / 2f)
            katLayout.draw(canvas)
            canvas.restore()

            // Kolom 2: Hujan highlight (Rata Tengah)
            val rainPaint = if (item.precipitation >= 5.0) {
                Paint(cellTextCenterPaint).apply { color = Color.rgb(220, 38, 38); isFakeBoldText = true }
            } else {
                Paint(cellTextCenterPaint).apply { color = Color.rgb(3, 105, 161) }
            }
            canvas.drawText("%.1f".format(item.precipitation), colCenter(2), rowBaselineY, rainPaint)

            // Kolom 3 & 4: Suhu & Terasa (Rata Tengah)
            canvas.drawText("%.1f°C".format(item.temperature), colCenter(3), rowBaselineY, cellTextCenterPaint)
            canvas.drawText("%.1f°C".format(item.feelsLike), colCenter(4), rowBaselineY, cellTextCenterPaint)

            // Kolom 5 & 6: Angin & Kelembapan (Rata Tengah)
            canvas.drawText("${item.windSpeed.toInt()}", colCenter(5), rowBaselineY, cellTextCenterPaint)
            canvas.drawText("${item.humidity}%", colCenter(6), rowBaselineY, cellTextCenterPaint)

            // Kolom 7: Kondisi (StaticLayout vertikal rata tengah, berakhir presisi di dalam margin kanan)
            canvas.save()
            canvas.translate(colStart(7) + 4f, y + (rowHeight - kondisiLayout.height) / 2f)
            kondisiLayout.draw(canvas)
            canvas.restore()

            y += rowHeight
            canvas.drawLine(tableLeft, y, tableRight, y, linePaint)
        }

        // Garis Vertikal Pemisah Kolom & Bingkai Tabel
        for (boundX in colBounds) {
            canvas.drawLine(boundX, tableStartY, boundX, y, linePaint)
        }
        canvas.drawRect(
            RectF(tableLeft, tableStartY, tableRight, y),
            Paint().apply { color = Color.rgb(148, 163, 184); style = Paint.Style.STROKE; strokeWidth = 0.9f }
        )

        y += 12f

        // Catatan SOP Peringatan Dini (menggunakan StaticLayout & tinggi kotak dinamis)
        val sopTextWidth = (tableWidth - 20f).toInt()
        val sopNoteText =
            "1. Wilayah pesisir (Kota Mukomuko, Teramang Jaya, Ipuh, Air Rami) mewaspadai pasang surut air laut dan angin kencang.\n" +
            "2. Wilayah perbukitan (V Koto, Malin Deman, Selagan Raya) mewaspadai potensi longsor bila curah hujan >15 mm/jam berturut-turut."
        val sopLayout = buildAutoFitStaticLayout(
            text = sopNoteText,
            maxWidth = sopTextWidth,
            paint = Paint(textPaint).apply { color = Color.rgb(120, 53, 15); textSize = 7.4f; textAlign = Paint.Align.LEFT },
            maxLines = 4,
            minTextSize = 6.5f
        )
        val noteBoxHeight = 20f + sopLayout.height + 8f
        val noteRect = RectF(tableLeft, y, tableRight, y + noteBoxHeight)
        canvas.drawRoundRect(noteRect, 6f, 6f, Paint().apply { color = Color.rgb(255, 251, 235) })
        canvas.drawRoundRect(noteRect, 6f, 6f, Paint().apply { color = Color.rgb(253, 230, 138); style = Paint.Style.STROKE; strokeWidth = 1f })

        PdfTextAutoShrinkHelper.drawAutoShrinkText(
            canvas = canvas,
            text = "PETUNJUK KESIAPSIAGAAN & DISPOSISI KEBENCANAAN:",
            x = tableLeft + 10f,
            y = y + 13f,
            maxWidth = tableWidth - 20f,
            basePaint = Paint(boldTextPaint).apply { color = Color.rgb(180, 83, 9); textAlign = Paint.Align.LEFT },
            initialFontSize = 7.6f,
            minFontSize = 6.5f
        )
        canvas.save()
        canvas.translate(tableLeft + 10f, y + 18f)
        sopLayout.draw(canvas)
        canvas.restore()

        y += noteBoxHeight + 10f

        // Sumber Informasi di bagian bawah konten laporan (di-wrap agar aman dalam margin A4)
        drawWrappedText(
            canvas = canvas,
            text = "Sumber Data & Informasi: Stasiun Meteorologi BMKG Fatmawati Bengkulu & Jaringan Sensor Telemetri BPBD Mukomuko",
            x = tableLeft,
            topY = y,
            maxWidth = tableWidth.toInt(),
            paint = Paint(textPaint).apply { color = Color.rgb(100, 116, 139); textSize = 7.4f; textAlign = Paint.Align.LEFT },
            maxLines = 2
        )

        // Tanpa blok tanda tangan pejabat (khusus Peta Cuaca 15 Kecamatan)
        drawFooterAtribusiResmi(canvas, pageWidth, pageHeight, margin, documentSettings = settings)

        pdfDoc.finishPage(page)

        FileOutputStream(outputFile).use { out ->
            pdfDoc.writeTo(out)
        }
        } finally {
            try {
                pdfDoc.close()
            } catch (_: Exception) {}
        }

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
     * Mengambil data ringkasan bencana bulanan (Januari s/d bulan berjalan) dan proporsi Pie Chart
     * dari database lokal + baseline musiman BPBD Mukomuko untuk dilampirkan otomatis pada PDF Laporan Harian.
     */
    suspend fun buildDefaultMonthlyAnalyticsForAttachment(
        context: Context,
        selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR),
        districtFilter: String? = null
    ): Pair<List<MonthlyDisasterSummary>, List<PieSliceData>> {
        val reports = try {
            AppDatabase.getDatabase(context).weatherDao().getAllCitizenDisasterReportsSync().map { entity ->
                CitizenDisasterReport(
                    id = entity.id,
                    disasterType = entity.disasterType,
                    incidentTime = entity.incidentTime,
                    incidentLocation = entity.incidentLocation,
                    district = entity.district,
                    village = entity.village,
                    latitude = entity.latitude,
                    longitude = entity.longitude,
                    gpsAccuracyMeters = entity.gpsAccuracyMeters,
                    isGpsVerified = entity.isGpsVerified,
                    casualtiesDead = entity.casualtiesDead,
                    casualtiesInjured = entity.casualtiesInjured,
                    casualtiesMissing = entity.casualtiesMissing,
                    photoUri = entity.photoUri,
                    reporterName = entity.reporterName,
                    reporterPhone = entity.reporterPhone,
                    additionalNotes = entity.additionalNotes,
                    timestamp = entity.timestamp,
                    status = entity.status
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
        val currentMonthIndex = Calendar.getInstance().get(Calendar.MONTH)
        val allMonths = buildMonthlyDisasterSummaries(reports, selectedYear, districtFilter)
        val activeMonths = allMonths.filter { it.monthIndex <= currentMonthIndex }
            .ifEmpty { allMonths.take(currentMonthIndex + 1) }
        val pieSlices = buildPieSlices(activeMonths, reports, pieMode = 0)
        return Pair(activeMonths, pieSlices)
    }

    /**
     * Menghasilkan dokumen PDF resmi A4 berisi Ringkasan & Statistik Data Bencana Bulanan
     * (lengkap dengan visualisasi Grafik Batang, Pie Chart proporsi bencana, KPI Eksekutif,
     * dan Tabel Matriks Januari s/d Bulan Berjalan) untuk lampiran laporan resmi harian Pusdalops BPBD.
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
            val safeScope = scopeLabel.replace(Regex("[^A-Za-z0-9]"), "_").take(20)
            val fileName = "Ringkasan_Statistik_Bencana_Bulanan_BPBD_Mukomuko_${selectedYear}_$safeScope.pdf"
            val cachePdfFile = File(getExportCacheDir(context), fileName)

            val pdfDoc = PdfDocument()
            try {
                val pageWidth = A4_PAGE_WIDTH // Standar A4 (points)
                val pageHeight = A4_PAGE_HEIGHT // Standar A4 (points)
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
                val page = pdfDoc.startPage(pageInfo)
                drawMonthlyDisasterAnalyticsPage(
                    context = context,
                    canvas = page.canvas,
                    monthlyData = monthlyData,
                    pieSlices = pieSlices,
                    selectedYear = selectedYear,
                    scopeLabel = scopeLabel
                )
                pdfDoc.finishPage(page)

                // Simpan ke subfolder cacheDir yang sesuai file_paths.xml untuk FileProvider Share/View
                FileOutputStream(cachePdfFile).use { out ->
                    pdfDoc.writeTo(out)
                }
            } finally {
                try {
                    pdfDoc.close()
                } catch (_: Exception) {}
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

    /**
     * Menggambar 1 halaman penuh Laporan Resmi Ringkasan & Statistik Data Bencana Bulanan
     * ke atas Canvas A4 (baik saat diunduh mandiri maupun sebagai halaman lampiran Laporan Harian).
     *
     * Hanya menampilkan baris dari Januari hingga bulan berjalan saat ini (`Calendar.MONTH`).
     */
    fun drawMonthlyDisasterAnalyticsPage(
        context: Context,
        canvas: Canvas,
        monthlyData: List<MonthlyDisasterSummary>,
        pieSlices: List<PieSliceData>,
        selectedYear: Int,
        scopeLabel: String,
        documentSettings: DocumentSettings? = null
    ) {
        val currentMonthIndex = Calendar.getInstance().get(Calendar.MONTH)
        val activeMonthlyData = monthlyData
            .filter { it.monthIndex <= currentMonthIndex }
            .ifEmpty { monthlyData.take(currentMonthIndex + 1) }
        if (activeMonthlyData.isEmpty()) return

        val lastMonthNameUpper = activeMonthlyData.last().monthFull.uppercase(Locale.getDefault())
        val lastMonthShort = activeMonthlyData.last().monthShort

        val settings = (documentSettings ?: DocumentSettingsRepository.getCachedOrDefaultSettings(context)).clamped()
        val resolvedOfficer = DocumentSettingsRepository.resolveOfficerIdentity(
            context = context,
            settings = settings
        )
        val rawOfficerName = "${resolvedOfficer.formattedName} (${resolvedOfficer.nipLine})"

        val pageWidth = A4_PAGE_WIDTH
        val pageHeight = A4_PAGE_HEIGHT
        val margin = STANDARD_MARGIN

        val titlePaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 11.5f
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
        var y = drawKopSuratResmi(context, canvas, pageWidth, margin, documentSettings = settings)

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
        PdfTextAutoShrinkHelper.drawAutoShrinkText(
            canvas = canvas,
            text = "Cakupan Wilayah: $scopeLabel  |  Waktu Unduh: $printDateStr",
            x = pageWidth / 2f,
            y = y,
            maxWidth = pageWidth - (margin * 2f),
            basePaint = metaPaint,
            initialFontSize = 7.8f,
            minFontSize = settings.minFontSize
        )
        y += 10.5f
        PdfTextAutoShrinkHelper.drawAutoShrinkText(
            canvas = canvas,
            text = "Petugas: $rawOfficerName",
            x = pageWidth / 2f,
            y = y,
            maxWidth = pageWidth - (margin * 2f),
            basePaint = metaPaint,
            initialFontSize = 7.8f,
            minFontSize = settings.minFontSize
        )
        y += 15f

        // 3. Kotak Ringkasan Eksekutif (KPI hingga Bulan Berjalan)
        val totalAnnual = activeMonthlyData.sumOf { it.totalIncidents }
        val totalAffected = activeMonthlyData.sumOf { it.casualtiesAffected }
        val totalResolved = activeMonthlyData.sumOf { it.resolvedCount }
        val resolvedPct = if (totalAnnual > 0) ((totalResolved * 100f) / totalAnnual).roundToInt().coerceIn(0, 100) else 100
        val avgMonthly = totalAnnual / activeMonthlyData.size.coerceAtLeast(1).toFloat()
        val peakMonth = activeMonthlyData.maxByOrNull { it.totalIncidents } ?: activeMonthlyData.first()
        val domCatEntry = DisasterChartCategory.entries.associateWith { cat ->
            activeMonthlyData.sumOf { it.countsByCategory[cat] ?: 0 }
        }.maxByOrNull { it.value }

        val kpiColWidth = ((pageWidth - 2f * margin - 28f) / 2f).toInt()
        val kpiBodyPaint = Paint(textPaint).apply {
            textSize = 7.0f
        }
        val leftKpiText =
            "• Total Kejadian: $totalAnnual kejadian (Rata-rata ${String.format(Locale.getDefault(), "%.1f", avgMonthly)}/bln)\n" +
            "• Bulan Puncak Kejadian: ${peakMonth.monthFull} (${peakMonth.totalIncidents} kejadian - Dominan Kec. ${peakMonth.topDistrict})"
        val rightKpiText =
            "• Bencana Dominan: ${domCatEntry?.key?.shortLabel ?: "Banjir"} (${domCatEntry?.value ?: 0} kejadian)\n" +
            "• Penanganan Tuntas TRC: $totalResolved/$totalAnnual selesai ($resolvedPct%) • Terdampak: $totalAffected jiwa"

        val leftKpiLayout = buildAutoFitStaticLayout(
            text = leftKpiText,
            maxWidth = kpiColWidth,
            paint = kpiBodyPaint,
            maxLines = 4,
            minTextSize = 5.6f,
            lineSpacingExtra = 1.5f,
            lineSpacingMultiplier = 1.12f
        )
        val rightKpiLayout = buildAutoFitStaticLayout(
            text = rightKpiText,
            maxWidth = kpiColWidth,
            paint = kpiBodyPaint,
            maxLines = 4,
            minTextSize = 5.6f,
            lineSpacingExtra = 1.5f,
            lineSpacingMultiplier = 1.12f
        )

        val kpiContentHeight = max(leftKpiLayout.height, rightKpiLayout.height).toFloat()
        val kpiBoxHeight = 18f + kpiContentHeight + 7f
        val kpiRect = RectF(margin, y, pageWidth - margin, y + kpiBoxHeight)
        canvas.drawRoundRect(kpiRect, 6f, 6f, Paint().apply { color = Color.rgb(239, 246, 255) })
        canvas.drawRoundRect(kpiRect, 6f, 6f, Paint().apply {
            color = Color.rgb(147, 197, 253)
            style = Paint.Style.STROKE
            strokeWidth = 1f
            isAntiAlias = true
        })

        PdfTextAutoShrinkHelper.drawAutoShrinkText(
            canvas = canvas,
            text = "I. RINGKASAN EKSEKUTIF STATISTIK KEBENCANAAN TAHUN $selectedYear (JANUARI – $lastMonthNameUpper)",
            x = margin + 10f,
            y = y + 12f,
            maxWidth = (pageWidth - margin * 2 - 20f),
            basePaint = Paint(boldTextPaint).apply { color = Color.rgb(30, 64, 175); textSize = 7.4f },
            initialFontSize = 7.4f,
            minFontSize = 6.2f
        )

        canvas.save()
        canvas.translate(margin + 10f, y + 17f)
        leftKpiLayout.draw(canvas)
        canvas.restore()

        canvas.save()
        canvas.translate(margin + 18f + kpiColWidth, y + 17f)
        rightKpiLayout.draw(canvas)
        canvas.restore()

        y += kpiBoxHeight + 18f

        // 4. Visualisasi Grafik Batang & Pie Chart Proporsi Bencana
        PdfTextAutoShrinkHelper.drawAutoShrinkText(
            canvas = canvas,
            text = "II. VISUALISASI GRAFIK BATANG TREN BULANAN & PIE CHART PROPORSI BENCANA",
            x = margin,
            y = y,
            maxWidth = (pageWidth - margin * 2),
            basePaint = sectionHeaderPaint,
            initialFontSize = 9.0f,
            minFontSize = 7.2f
        )
        y += 8f

        val chartBoxHeight = 142f
        val barBoxRect = RectF(margin, y, margin + 306f, y + chartBoxHeight)
        val pieBoxRect = RectF(margin + 314f, y, pageWidth - margin, y + chartBoxHeight)

        // Kotak Kiri: Grafik Batang Bulanan (Jan s/d Bulan Berjalan)
        canvas.drawRoundRect(barBoxRect, 6f, 6f, Paint().apply { color = Color.rgb(248, 250, 252) })
        canvas.drawRoundRect(barBoxRect, 6f, 6f, Paint(linePaint).apply { style = Paint.Style.STROKE })
        PdfTextAutoShrinkHelper.drawAutoShrinkText(
            canvas = canvas,
            text = "Grafik Batang Kejadian Bulanan (Jan–$lastMonthShort $selectedYear)",
            x = barBoxRect.left + 8f,
            y = barBoxRect.top + 12f,
            maxWidth = barBoxRect.width() - 16f,
            basePaint = boldTextPaint,
            initialFontSize = 7.8f,
            minFontSize = 6.4f
        )

        val chartLeft = barBoxRect.left + 24f
        val chartRight = barBoxRect.right - 8f
        val chartTop = barBoxRect.top + 24f
        val chartBottom = barBoxRect.bottom - 28f
        val cWidth = chartRight - chartLeft
        val cHeight = chartBottom - chartTop
        val maxMonthInc = max(activeMonthlyData.maxOfOrNull { it.totalIncidents } ?: 10, 6)

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

        val slotW = cWidth / activeMonthlyData.size.coerceAtLeast(1).toFloat()
        val barW = slotW * 0.56f
        val barMonthPaint = Paint(textPaint).apply {
            textSize = 6.5f
            textAlign = Paint.Align.CENTER
        }
        val barValPaint = Paint(boldTextPaint).apply {
            textSize = 6.5f
            textAlign = Paint.Align.CENTER
        }

        activeMonthlyData.forEachIndexed { idx, m ->
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
        val legPaint = PdfTextAutoShrinkHelper.configurePdfPaint(Paint(textPaint).apply { textSize = 6.3f }, 0.025f)
        DisasterChartCategory.entries.forEach { cat ->
            val labelText = PdfTextAutoShrinkHelper.normalizePdfWordSpacing(cat.shortLabel)
            canvas.drawRect(legX, legY - 5f, legX + 5f, legY, Paint().apply { color = getCategoryRgbColor(cat) })
            canvas.drawText(labelText, legX + 7f, legY - 0.5f, legPaint)
            legX += legPaint.measureText(labelText) + 14f
        }

        // Kotak Kanan: Pie / Donut Chart Proporsi Bencana
        canvas.drawRoundRect(pieBoxRect, 6f, 6f, Paint().apply { color = Color.rgb(248, 250, 252) })
        canvas.drawRoundRect(pieBoxRect, 6f, 6f, Paint(linePaint).apply { style = Paint.Style.STROKE })
        PdfTextAutoShrinkHelper.drawAutoShrinkText(
            canvas = canvas,
            text = "Pie Chart Proporsi Bencana",
            x = pieBoxRect.left + 8f,
            y = pieBoxRect.top + 12f,
            maxWidth = pieBoxRect.width() - 16f,
            basePaint = boldTextPaint,
            initialFontSize = 7.8f,
            minFontSize = 6.4f
        )

        val pieOval = RectF(
            pieBoxRect.left + 8f,
            pieBoxRect.top + 28f,
            pieBoxRect.left + 76f,
            pieBoxRect.top + 96f
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
            17f,
            Paint().apply { color = Color.rgb(248, 250, 252); isAntiAlias = true }
        )
        canvas.drawText(
            "$totalAnnual",
            pieOval.centerX(),
            pieOval.centerY() + 3f,
            Paint(boldTextPaint).apply { textAlign = Paint.Align.CENTER; textSize = 8f }
        )

        // Legenda Persentase Pie Chart di sebelah kanan Donut
        var pLegY = pieBoxRect.top + 26f
        val pLegX = pieBoxRect.left + 82f
        val maxLegendTextWidth = (pieBoxRect.right - (pLegX + 8f) - 5f).toInt()
        val smallLegPaint = Paint(textPaint).apply { textSize = 6.2f }
        val smallBoldPaint = Paint(boldTextPaint).apply { textSize = 6.0f }
        pieSlices.take(6).forEachIndexed { idx, slice ->
            val cRgb = getSliceRgbColor(slice.shortLabel, idx)
            canvas.drawRect(pLegX, pLegY + 1f, pLegX + 5.5f, pLegY + 6.5f, Paint().apply { color = cRgb })
            val pctStr = "${String.format(Locale.getDefault(), "%.1f", slice.percentage)}%"
            val legendItemLayout = buildAutoFitStaticLayout(
                text = "${slice.shortLabel.take(12)}: ${slice.count} ($pctStr)",
                maxWidth = maxLegendTextWidth,
                paint = smallBoldPaint,
                maxLines = 1,
                minTextSize = 5.0f
            )
            canvas.save()
            canvas.translate(pLegX + 8f, pLegY)
            legendItemLayout.draw(canvas)
            canvas.restore()
            pLegY += 15f
        }
        PdfTextAutoShrinkHelper.drawPdfText(
            canvas,
            "Total: $totalAnnual Kejadian ($selectedYear)",
            pieBoxRect.left + 10f,
            pieBoxRect.bottom - 8f,
            smallLegPaint
        )

        y += chartBoxHeight + 28f

        // 5. Tabel Rekapitulasi Bulanan (Januari s/d Bulan Berjalan)
        PdfTextAutoShrinkHelper.drawAutoShrinkText(
            canvas = canvas,
            text = "III. TABEL REKAPITULASI DATA KEJADIAN BENCANA BULANAN (JANUARI – $lastMonthNameUpper $selectedYear)",
            x = margin,
            y = y,
            maxWidth = (pageWidth - margin * 2),
            basePaint = sectionHeaderPaint,
            initialFontSize = 9.0f,
            minFontSize = 7.2f
        )
        y += 8f

        val tableTopY = y
        val tableRight = pageWidth - margin
        // Koordinat batas vertikal tiap kolom (13 kolom, total tepat 523pt dari x=36 hingga x=559)
        val xNo = margin                  // 36..50   (14pt)
        val xBulan = margin + 14f         // 50..92   (42pt)
        val xBanjir = margin + 56f        // 92..119  (27pt)
        val xEkstrem = margin + 83f       // 119..149 (30pt)
        val xLongsor = margin + 113f      // 149..179 (30pt)
        val xAbrasi = margin + 143f       // 179..206 (27pt)
        val xKarhutla = margin + 170f     // 206..237 (31pt)
        val xLain = margin + 201f         // 237..265 (28pt)
        val xTotal = margin + 229f        // 265..290 (25pt)
        val xJiwa = margin + 254f         // 290..334 (44pt - Dampak)
        val xSelesai = margin + 298f      // 334..374 (40pt - Selesai)
        val xKec = margin + 338f          // 374..489 (115pt - Kec. Dominan)
        val xStatus = margin + 453f       // 489..559 (70pt - Status, pas di tableRight=559)

        val colNoCenter = (xNo + xBulan) / 2f
        val colBulanText = xBulan + 3f
        val colBanjirCenter = (xBanjir + xEkstrem) / 2f
        val colEkstremCenter = (xEkstrem + xLongsor) / 2f
        val colLongsorCenter = (xLongsor + xAbrasi) / 2f
        val colAbrasiCenter = (xAbrasi + xKarhutla) / 2f
        val colKarhutlaCenter = (xKarhutla + xLain) / 2f
        val colLainCenter = (xLain + xTotal) / 2f
        val colTotalCenter = (xTotal + xJiwa) / 2f
        val colJiwaCenter = (xJiwa + xSelesai) / 2f
        val colSelesaiCenter = (xSelesai + xKec) / 2f
        val colKecText = xKec + 4f
        val colStatusCenter = (xStatus + tableRight) / 2f

        val thHeight = 18f
        canvas.drawRect(RectF(margin, y, tableRight, y + thHeight), Paint().apply { color = Color.rgb(226, 232, 240) })
        val thPaintLeft = Paint(boldTextPaint).apply { textSize = 6.3f; textAlign = Paint.Align.LEFT }
        val thPaintCenter = Paint(boldTextPaint).apply { textSize = 6.3f; textAlign = Paint.Align.CENTER }
        PdfTextAutoShrinkHelper.drawPdfText(canvas, "No", colNoCenter, y + 12f, thPaintCenter)
        PdfTextAutoShrinkHelper.drawPdfText(canvas, "Bulan", colBulanText, y + 12f, thPaintLeft)
        PdfTextAutoShrinkHelper.drawPdfText(canvas, "Banjir", colBanjirCenter, y + 12f, thPaintCenter)
        PdfTextAutoShrinkHelper.drawPdfText(canvas, "Ekstrem", colEkstremCenter, y + 12f, thPaintCenter)
        PdfTextAutoShrinkHelper.drawPdfText(canvas, "Longsor", colLongsorCenter, y + 12f, thPaintCenter)
        PdfTextAutoShrinkHelper.drawPdfText(canvas, "Abrasi", colAbrasiCenter, y + 12f, thPaintCenter)
        PdfTextAutoShrinkHelper.drawPdfText(canvas, "Karhutla", colKarhutlaCenter, y + 12f, thPaintCenter)
        PdfTextAutoShrinkHelper.drawPdfText(canvas, "Lainnya", colLainCenter, y + 12f, thPaintCenter)
        PdfTextAutoShrinkHelper.drawPdfText(canvas, "Total", colTotalCenter, y + 12f, thPaintCenter)
        PdfTextAutoShrinkHelper.drawPdfText(canvas, "Dampak", colJiwaCenter, y + 12f, thPaintCenter)
        PdfTextAutoShrinkHelper.drawPdfText(canvas, "Selesai", colSelesaiCenter, y + 12f, thPaintCenter)
        PdfTextAutoShrinkHelper.drawPdfText(canvas, "Kec. Dominan", colKecText, y + 12f, thPaintLeft)
        PdfTextAutoShrinkHelper.drawPdfText(canvas, "Status", colStatusCenter, y + 12f, thPaintCenter)
        y += thHeight
        canvas.drawLine(margin, y, tableRight, y, linePaint)

        val rowH = 17.5f
        val cellPaint = Paint(textPaint).apply { textSize = 6.4f; textAlign = Paint.Align.LEFT }
        val cellCenterPaint = Paint(textPaint).apply { textSize = 6.4f; textAlign = Paint.Align.CENTER }
        val cellBoldPaint = Paint(boldTextPaint).apply { textSize = 6.4f; textAlign = Paint.Align.LEFT }
        val cellBoldCenterPaint = Paint(boldTextPaint).apply { textSize = 6.4f; textAlign = Paint.Align.CENTER }
        val rightColPaint = Paint(textPaint).apply { textSize = 6.1f; textAlign = Paint.Align.LEFT }
        val kecCellPaint = Paint(textPaint).apply { textSize = 6.1f; textAlign = Paint.Align.LEFT }

        val wJiwa = (xSelesai - xJiwa - 6f).toInt()
        val wSelesai = (xKec - xSelesai - 6f).toInt()
        val wKec = (xStatus - xKec - 8f).toInt()
        val wStatus = (tableRight - xStatus - 6f).toInt()

        activeMonthlyData.forEachIndexed { idx, m ->
            if (idx % 2 == 1) {
                canvas.drawRect(
                    RectF(margin, y, tableRight, y + rowH),
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

            canvas.drawText("${idx + 1}", colNoCenter, ry, cellCenterPaint)
            canvas.drawText(m.monthFull, colBulanText, ry, cellBoldPaint)
            canvas.drawText("$cBanjir", colBanjirCenter, ry, cellCenterPaint)
            canvas.drawText("$cEkstrem", colEkstremCenter, ry, cellCenterPaint)
            canvas.drawText("$cLongsor", colLongsorCenter, ry, cellCenterPaint)
            canvas.drawText("$cAbrasi", colAbrasiCenter, ry, cellCenterPaint)
            canvas.drawText("$cKarhutla", colKarhutlaCenter, ry, cellCenterPaint)
            canvas.drawText("$cLain", colLainCenter, ry, cellCenterPaint)
            canvas.drawText("${m.totalIncidents}", colTotalCenter, ry, cellBoldCenterPaint)

            // Gunakan StaticLayout (buildAutoFitStaticLayout) pada kolom kanan agar tidak pernah meluber/terpotong
            val jiwaLayout = buildAutoFitStaticLayout(
                text = "${m.casualtiesAffected} jw",
                maxWidth = wJiwa,
                paint = rightColPaint,
                maxLines = 1,
                minTextSize = 5.2f
            )
            canvas.save()
            canvas.translate(xJiwa + (xSelesai - xJiwa - jiwaLayout.getLineWidth(0)) / 2f, y + (rowH - jiwaLayout.height) / 2f)
            jiwaLayout.draw(canvas)
            canvas.restore()

            val selesaiLayout = buildAutoFitStaticLayout(
                text = "${m.resolvedCount}/${m.totalIncidents}",
                maxWidth = wSelesai,
                paint = rightColPaint,
                maxLines = 1,
                minTextSize = 5.2f
            )
            canvas.save()
            canvas.translate(xSelesai + (xKec - xSelesai - selesaiLayout.getLineWidth(0)) / 2f, y + (rowH - selesaiLayout.height) / 2f)
            selesaiLayout.draw(canvas)
            canvas.restore()

            val kecLayout = buildAutoFitStaticLayout(
                text = m.topDistrict,
                maxWidth = wKec,
                paint = kecCellPaint,
                maxLines = 1,
                minTextSize = 5.2f
            )
            canvas.save()
            canvas.translate(colKecText, y + (rowH - kecLayout.height) / 2f)
            kecLayout.draw(canvas)
            canvas.restore()

            val statusLayout = buildAutoFitStaticLayout(
                text = statusText,
                maxWidth = wStatus,
                paint = Paint(boldTextPaint).apply { color = statusColor; textSize = 5.9f },
                maxLines = 1,
                minTextSize = 5.0f
            )
            canvas.save()
            canvas.translate(xStatus + (tableRight - xStatus - statusLayout.getLineWidth(0)) / 2f, y + (rowH - statusLayout.height) / 2f)
            statusLayout.draw(canvas)
            canvas.restore()

            y += rowH
            canvas.drawLine(margin, y, tableRight, y, linePaint)
        }

        // Baris Total (Januari s/d Bulan Berjalan)
        val totalRowH = 19f
        canvas.drawRect(
            RectF(margin, y, tableRight, y + totalRowH),
            Paint().apply { color = Color.rgb(219, 234, 254) }
        )
        val totY = y + 13f
        val sumBanjir = activeMonthlyData.sumOf { it.countsByCategory[DisasterChartCategory.BANJIR] ?: 0 }
        val sumEkstrem = activeMonthlyData.sumOf { it.countsByCategory[DisasterChartCategory.CUACA_EKSTREM] ?: 0 }
        val sumLongsor = activeMonthlyData.sumOf { it.countsByCategory[DisasterChartCategory.LONGSOR] ?: 0 }
        val sumAbrasi = activeMonthlyData.sumOf { it.countsByCategory[DisasterChartCategory.ABRASI] ?: 0 }
        val sumKarhutla = activeMonthlyData.sumOf { it.countsByCategory[DisasterChartCategory.KEBAKARAN] ?: 0 }
        val sumLain = activeMonthlyData.sumOf { it.countsByCategory[DisasterChartCategory.GEMPA_LAINNYA] ?: 0 }

        canvas.drawText("TOTAL", colBulanText, totY, cellBoldPaint)
        canvas.drawText("$sumBanjir", colBanjirCenter, totY, cellBoldCenterPaint)
        canvas.drawText("$sumEkstrem", colEkstremCenter, totY, cellBoldCenterPaint)
        canvas.drawText("$sumLongsor", colLongsorCenter, totY, cellBoldCenterPaint)
        canvas.drawText("$sumAbrasi", colAbrasiCenter, totY, cellBoldCenterPaint)
        canvas.drawText("$sumKarhutla", colKarhutlaCenter, totY, cellBoldCenterPaint)
        canvas.drawText("$sumLain", colLainCenter, totY, cellBoldCenterPaint)
        canvas.drawText("$totalAnnual", colTotalCenter, totY, cellBoldCenterPaint)

        val totJiwaLayout = buildAutoFitStaticLayout(
            text = "$totalAffected jw",
            maxWidth = wJiwa,
            paint = Paint(boldTextPaint).apply { textSize = 6.1f },
            maxLines = 1,
            minTextSize = 5.2f
        )
        canvas.save()
        canvas.translate(xJiwa + (xSelesai - xJiwa - totJiwaLayout.getLineWidth(0)) / 2f, y + (totalRowH - totJiwaLayout.height) / 2f)
        totJiwaLayout.draw(canvas)
        canvas.restore()

        val totSelesaiLayout = buildAutoFitStaticLayout(
            text = "$totalResolved/$totalAnnual",
            maxWidth = wSelesai,
            paint = Paint(boldTextPaint).apply { textSize = 6.1f },
            maxLines = 1,
            minTextSize = 5.2f
        )
        canvas.save()
        canvas.translate(xSelesai + (xKec - xSelesai - totSelesaiLayout.getLineWidth(0)) / 2f, y + (totalRowH - totSelesaiLayout.height) / 2f)
        totSelesaiLayout.draw(canvas)
        canvas.restore()

        val totKecLayout = buildAutoFitStaticLayout(
            text = "15 Kecamatan",
            maxWidth = wKec,
            paint = Paint(boldTextPaint).apply { textSize = 6.1f },
            maxLines = 1,
            minTextSize = 5.2f
        )
        canvas.save()
        canvas.translate(colKecText, y + (totalRowH - totKecLayout.height) / 2f)
        totKecLayout.draw(canvas)
        canvas.restore()

        val totStatusLayout = buildAutoFitStaticLayout(
            text = "$resolvedPct% TUNTAS",
            maxWidth = wStatus,
            paint = Paint(boldTextPaint).apply { color = Color.rgb(21, 128, 61); textSize = 5.9f },
            maxLines = 1,
            minTextSize = 5.0f
        )
        canvas.save()
        canvas.translate(xStatus + (tableRight - xStatus - totStatusLayout.getLineWidth(0)) / 2f, y + (totalRowH - totStatusLayout.height) / 2f)
        totStatusLayout.draw(canvas)
        canvas.restore()

        val tableBottomY = y + totalRowH
        // Garis pemisah vertikal (grid lines) dan bingkai luar tabel yang sejajar dengan batas kolom baru
        val verticalGridPaint = Paint(linePaint).apply { strokeWidth = 0.6f }
        val colSeparators = floatArrayOf(
            xBulan, xBanjir, xEkstrem, xLongsor, xAbrasi,
            xKarhutla, xLain, xTotal, xJiwa, xSelesai, xKec, xStatus
        )
        for (vx in colSeparators) {
            canvas.drawLine(vx, tableTopY, vx, tableBottomY, verticalGridPaint)
        }
        canvas.drawRect(
            RectF(margin, tableTopY, tableRight, tableBottomY),
            Paint(linePaint).apply { style = Paint.Style.STROKE; strokeWidth = 0.8f }
        )

        y = tableBottomY + 14f

        // 6. Catatan Evaluasi Musiman & Tanda Tangan Pengesahan Laporan Harian
        val notePaddingHorizontal = 12f
        val notePaddingTop = 10f
        val notePaddingBottom = 10f
        val noteHeaderHeight = 12f
        val noteGapAfterHeader = 6f
        val noteBodyPaint = Paint(cellPaint).apply {
            color = Color.rgb(30, 41, 59)
            textSize = 7.4f
        }
        val noteBodyLayout = buildAutoFitStaticLayout(
            text = "Dokumen statistik bulanan ini dihasilkan otomatis dari basis data terpadu SIPAGAR KITA BPBD Mukomuko sebagai lampiran resmi laporan harian Pusdalops-PB untuk evaluasi kesiapsiagaan personil TRC dan distribusi logistik kebencanaan.",
            maxWidth = (pageWidth - margin * 2 - notePaddingHorizontal * 2).toInt(),
            paint = noteBodyPaint,
            maxLines = 4,
            minTextSize = 6.8f,
            lineSpacingExtra = 3.0f,
            lineSpacingMultiplier = 1.3f
        )
        val noteBoxHeight = notePaddingTop + noteHeaderHeight + noteGapAfterHeader + noteBodyLayout.height + notePaddingBottom
        val noteBox = RectF(margin, y, pageWidth - margin, y + noteBoxHeight)
        canvas.drawRoundRect(noteBox, 5f, 5f, Paint().apply { color = Color.rgb(255, 251, 235) })
        canvas.drawRoundRect(noteBox, 5f, 5f, Paint().apply { color = Color.rgb(253, 230, 138); style = Paint.Style.STROKE; strokeWidth = 0.9f })
        PdfTextAutoShrinkHelper.drawAutoShrinkText(
            canvas = canvas,
            text = "CATATAN LAMPIRAN LAPORAN HARIAN PUSDALOPS-PB BPBD KABUPATEN MUKOMUKO:",
            x = margin + notePaddingHorizontal,
            y = y + notePaddingTop + 8.5f,
            maxWidth = pageWidth - margin * 2 - notePaddingHorizontal * 2,
            basePaint = Paint(boldTextPaint).apply { color = Color.rgb(180, 83, 9); textSize = 7.6f },
            initialFontSize = 7.6f,
            minFontSize = 6.6f
        )
        canvas.save()
        canvas.translate(margin + notePaddingHorizontal, y + notePaddingTop + noteHeaderHeight + noteGapAfterHeader)
        noteBodyLayout.draw(canvas)
        canvas.restore()

        y += noteBoxHeight + 8f

        drawFooterAtribusiResmi(canvas, pageWidth, pageHeight, margin, documentSettings = settings)
    }

    /**
     * Mengambil data cuaca 15 Kecamatan dari Room Cache (`weather_cache`) untuk digunakan
     * sebagai lampiran Halaman 4 (Tabel Prakiraan Cuaca BMKG) pada PDF Laporan Harian Petugas.
     */
    suspend fun buildDefaultDistrictsForecastForAttachment(
        context: Context
    ): List<WeatherFullReport> {
        val cachedMap = try {
            AppDatabase.getDatabase(context).weatherDao().getAllCached().associateBy { it.districtId }
        } catch (_: Exception) {
            emptyMap()
        }
        return MukomukoDistricts.list.mapIndexed { idx, district ->
            val cached = cachedMap[district.id]
            WeatherFullReport(
                district = district,
                current = CurrentWeather(
                    temperature = cached?.temperature ?: (29.0 + (idx % 3)),
                    feelsLike = cached?.feelsLike ?: (31.0 + (idx % 3)),
                    humidity = cached?.humidity ?: (78 + (idx % 10)),
                    precipitation = cached?.precipitation ?: 0.0,
                    weatherCode = cached?.weatherCode ?: 2,
                    surfacePressure = cached?.surfacePressure ?: 1010.0,
                    windSpeed = cached?.windSpeed ?: (12.0 + (idx % 10)),
                    windDirection = cached?.windDirection ?: (210 + (idx * 15) % 90),
                    time = cached?.time ?: ""
                ),
                hourly = emptyList(),
                daily = emptyList(),
                marine = MarineWeather(
                    waveHeight = 1.2,
                    seaCondition = "Sedang",
                    windSpeedKnots = 10.0,
                    tideStatus = "Pasang Normal",
                    fishermanSafety = "Waspada Perahu Kecil",
                    fishermanAdvice = "Perhatikan angin barat daya"
                ),
                airQuality = AirQuality(
                    aqi = 32,
                    pm25 = 8.5,
                    pm10 = 18.0,
                    status = "Baik",
                    recommendation = "Udara bersih dan sehat"
                ),
                alert = null
            )
        }
    }

    /**
     * Menghasilkan dokumen PDF mandiri Tabel Prakiraan Cuaca BMKG 15 Kecamatan Kabupaten Mukomuko.
     */
    fun exportBmkgForecastToPdf(
        context: Context,
        allDistrictsSummary: List<WeatherFullReport>
    ): File? {
        return try {
            val fileName = "Prakiraan_Cuaca_BMKG_15_Kecamatan_Mukomuko_${System.currentTimeMillis()}.pdf"
            val cachePdfFile = File(getExportCacheDir(context), fileName)

            val pdfDoc = PdfDocument()
            try {
                val pageInfo = PdfDocument.PageInfo.Builder(A4_PAGE_WIDTH, A4_PAGE_HEIGHT, 1).create()
                val page = pdfDoc.startPage(pageInfo)
                drawBmkgForecastPageToCanvas(
                    context = context,
                    canvas = page.canvas,
                    pageWidth = A4_PAGE_WIDTH,
                    pageHeight = A4_PAGE_HEIGHT,
                    allDistrictsSummary = allDistrictsSummary
                )
                pdfDoc.finishPage(page)

                FileOutputStream(cachePdfFile).use { out ->
                    pdfDoc.writeTo(out)
                }
            } finally {
                try {
                    pdfDoc.close()
                } catch (_: Exception) {}
            }

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

            try {
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", cachePdfFile)
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(
                        Intent.EXTRA_SUBJECT,
                        "Tabel Prakiraan Cuaca BMKG 15 Kecamatan Kabupaten Mukomuko"
                    )
                    putExtra(
                        Intent.EXTRA_TEXT,
                        "Terlampir dokumen PDF resmi Tabel Prakiraan Cuaca 15 Kecamatan Kabupaten Mukomuko (Pagi, Siang, Malam, Dini Hari, Suhu & Angin)."
                    )
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                val chooser = Intent.createChooser(shareIntent, "Unduh / Bagikan PDF Prakiraan Cuaca BMKG").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            } catch (_: Exception) {}

            val toastMsg = if (savedToDownloads) {
                "PDF Prakiraan Cuaca tersimpan di Downloads/$fileName!"
            } else {
                "PDF Prakiraan Cuaca berhasil dibuat & siap dibagikan!"
            }
            Toast.makeText(context, toastMsg, Toast.LENGTH_LONG).show()
            cachePdfFile
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal mengunduh PDF Prakiraan: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            null
        }
    }

    /**
     * Menggambar Tabel Prakiraan Cuaca BMKG 15 Kecamatan (8 Kolom: No, Kecamatan, Pagi, Siang,
     * Malam, Dini Hari, Suhu, Angin) ke atas Canvas A4 Portrait.
     */
    fun drawBmkgForecastPageToCanvas(
        context: Context,
        canvas: Canvas,
        pageWidth: Int,
        pageHeight: Int,
        allDistrictsSummary: List<WeatherFullReport>,
        documentSettings: DocumentSettings? = null
    ) {
        val margin = STANDARD_MARGIN
        val tableLeft = margin
        val tableRight = pageWidth - margin
        val tableWidth = tableRight - tableLeft // 523pt pada A4

        val rows = buildBmkgForecastRows(allDistrictsSummary)
        val settings = (documentSettings ?: DocumentSettingsRepository.getCachedOrDefaultSettings(context)).clamped()
        val resolvedOfficer = DocumentSettingsRepository.resolveOfficerIdentity(
            context = context,
            settings = settings
        )
        val rawOfficerName = "${resolvedOfficer.formattedName} (${resolvedOfficer.nipLine})"

        var y = drawKopSuratResmi(context, canvas, pageWidth, margin, documentSettings = settings)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 58, 138)
            textSize = 11f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        PdfTextAutoShrinkHelper.drawAutoShrinkText(
            canvas = canvas,
            text = "TABEL PRAKIRAAN CUACA HARIAN BMKG 15 KECAMATAN KABUPATEN MUKOMUKO",
            x = pageWidth / 2f,
            y = y,
            maxWidth = tableWidth,
            basePaint = titlePaint,
            initialFontSize = 11f,
            minFontSize = 8.5f
        )
        y += 13f

        val sdf = SimpleDateFormat("EEEE, dd MMMM yyyy - HH:mm 'WIB'", Locale.forLanguageTag("id-ID"))
        val printDateStr = sdf.format(Date())
        val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(71, 85, 105)
            textSize = 8f
            textAlign = Paint.Align.CENTER
        }
        PdfTextAutoShrinkHelper.drawAutoShrinkText(
            canvas = canvas,
            text = "Waktu Penerbitan: $printDateStr  |  Sumber: Stasiun Meteorologi BMKG & Pusdalops BPBD",
            x = pageWidth / 2f,
            y = y,
            maxWidth = tableWidth,
            basePaint = metaPaint,
            initialFontSize = settings.bodyFontSize,
            minFontSize = settings.minFontSize
        )
        y += 10.5f
        PdfTextAutoShrinkHelper.drawAutoShrinkText(
            canvas = canvas,
            text = "Petugas: $rawOfficerName",
            x = pageWidth / 2f,
            y = y,
            maxWidth = tableWidth,
            basePaint = metaPaint,
            initialFontSize = settings.bodyFontSize,
            minFontSize = settings.minFontSize
        )
        y += 14f

        // Pembagian 8 Kolom Presisi di dalam tableWidth (523pt):
        // 0: No (4.5%), 1: Kecamatan (21.5%), 2: Pagi (13%), 3: Siang (13%),
        // 4: Malam (13%), 5: Dini Hari (13%), 6: Suhu (10%), 7: Angin (12%)
        val colWeights = floatArrayOf(0.045f, 0.215f, 0.13f, 0.13f, 0.13f, 0.13f, 0.10f, 0.12f)
        val colBounds = FloatArray(colWeights.size + 1)
        colBounds[0] = tableLeft
        for (i in colWeights.indices) {
            colBounds[i + 1] = colBounds[i] + (tableWidth * colWeights[i])
        }
        colBounds[colWeights.size] = tableRight

        fun colStart(idx: Int) = colBounds[idx]
        fun colWidth(idx: Int) = colBounds[idx + 1] - colBounds[idx]
        fun colCenter(idx: Int) = (colBounds[idx] + colBounds[idx + 1]) / 2f

        val headerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 58, 138)
            style = Paint.Style.FILL
        }
        val headerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 7.4f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        val headerLeftPaint = Paint(headerTextPaint).apply {
            textAlign = Paint.Align.LEFT
        }
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(203, 213, 225)
            strokeWidth = 0.8f
        }
        val cellBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 7.2f
            isFakeBoldText = true
        }
        val cellSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(71, 85, 105)
            textSize = 6.3f
        }
        val cellTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 41, 59)
            textSize = 7.0f
        }
        val cellCenterPaint = Paint(cellTextPaint).apply {
            textAlign = Paint.Align.CENTER
        }

        val tableTopY = y
        val thHeight = 22f
        canvas.drawRect(RectF(tableLeft, y, tableRight, y + thHeight), headerBgPaint)

        val thBaseline = y + 14f
        PdfTextAutoShrinkHelper.drawPdfText(canvas, "No", colCenter(0), thBaseline, headerTextPaint)
        PdfTextAutoShrinkHelper.drawPdfText(canvas, "Kecamatan", colStart(1) + 5f, thBaseline, headerLeftPaint)
        PdfTextAutoShrinkHelper.drawPdfText(canvas, "Pagi", colCenter(2), thBaseline, headerTextPaint)
        PdfTextAutoShrinkHelper.drawPdfText(canvas, "Siang", colCenter(3), thBaseline, headerTextPaint)
        PdfTextAutoShrinkHelper.drawPdfText(canvas, "Malam", colCenter(4), thBaseline, headerTextPaint)
        PdfTextAutoShrinkHelper.drawPdfText(canvas, "Dini Hari", colCenter(5), thBaseline, headerTextPaint)
        PdfTextAutoShrinkHelper.drawPdfText(canvas, "Suhu", colCenter(6), thBaseline, headerTextPaint)
        PdfTextAutoShrinkHelper.drawPdfText(canvas, "Angin", colCenter(7), thBaseline, headerTextPaint)
        y += thHeight

        val rowH = 31f
        rows.forEachIndexed { idx, row ->
            if (idx % 2 == 1) {
                canvas.drawRect(
                    RectF(tableLeft, y, tableRight, y + rowH),
                    Paint().apply { color = Color.rgb(248, 250, 252) }
                )
            }

            // Kolom 0: No
            PdfTextAutoShrinkHelper.drawPdfText(canvas, "${idx + 1}", colCenter(0), y + 18f, cellCenterPaint)

            // Kolom 1: Kecamatan + Kategori Wilayah
            val kecLayout = buildAutoFitStaticLayout(
                text = row.district.name,
                maxWidth = (colWidth(1) - 8f).toInt(),
                paint = cellBoldPaint,
                maxLines = 1,
                minTextSize = 5.8f
            )
            val catLayout = buildAutoFitStaticLayout(
                text = row.district.category,
                maxWidth = (colWidth(1) - 8f).toInt(),
                paint = cellSubPaint,
                maxLines = 1,
                minTextSize = 5.4f
            )
            canvas.save()
            canvas.translate(colStart(1) + 5f, y + 5f)
            kecLayout.draw(canvas)
            canvas.translate(0f, kecLayout.height + 1.5f)
            catLayout.draw(canvas)
            canvas.restore()

            // Kolom 2-5: Pagi, Siang, Malam, Dini Hari
            val slots = listOf(
                row.pagiCondition to row.pagiCode,
                row.siangCondition to row.siangCode,
                row.malamCondition to row.malamCode,
                row.diniHariCondition to row.diniHariCode
            )
            slots.forEachIndexed { sIdx, (condText, code) ->
                val colIdx = 2 + sIdx
                val badgeColor = when (code) {
                    0 -> Color.rgb(217, 119, 6)
                    1 -> Color.rgb(2, 132, 199)
                    51, 53, 55, 61, 80 -> Color.rgb(37, 99, 235)
                    63, 65, 81, 82 -> Color.rgb(29, 78, 216)
                    95, 96, 99 -> Color.rgb(220, 38, 38)
                    else -> Color.rgb(51, 65, 85)
                }
                val condLayout = buildAutoFitStaticLayout(
                    text = condText,
                    maxWidth = (colWidth(colIdx) - 4f).toInt(),
                    paint = Paint(cellBoldPaint).apply { color = badgeColor; textSize = 6.7f },
                    maxLines = 2,
                    minTextSize = 5.5f
                )
                canvas.save()
                val textWidth = (0 until condLayout.lineCount).maxOfOrNull { condLayout.getLineWidth(it) } ?: 0f
                canvas.translate(
                    colStart(colIdx) + (colWidth(colIdx) - textWidth) / 2f,
                    y + (rowH - condLayout.height) / 2f
                )
                condLayout.draw(canvas)
                canvas.restore()
            }

            // Kolom 6: Suhu & RH
            val tempLayout = buildAutoFitStaticLayout(
                text = "${row.tempMin} - ${row.tempMax} °C",
                maxWidth = (colWidth(6) - 4f).toInt(),
                paint = cellBoldPaint,
                maxLines = 1,
                minTextSize = 5.6f
            )
            val rhLayout = buildAutoFitStaticLayout(
                text = "RH ${row.humidityMin}-${row.humidityMax}%",
                maxWidth = (colWidth(6) - 4f).toInt(),
                paint = cellSubPaint,
                maxLines = 1,
                minTextSize = 5.2f
            )
            canvas.save()
            canvas.translate(colStart(6) + (colWidth(6) - tempLayout.getLineWidth(0)) / 2f, y + 6f)
            tempLayout.draw(canvas)
            canvas.restore()
            canvas.save()
            canvas.translate(colStart(6) + (colWidth(6) - rhLayout.getLineWidth(0)) / 2f, y + 6f + tempLayout.height + 2f)
            rhLayout.draw(canvas)
            canvas.restore()

            // Kolom 7: Angin (Arah + Kecepatan)
            val windDirLayout = buildAutoFitStaticLayout(
                text = "➤ ${row.windDirectionLabel}",
                maxWidth = (colWidth(7) - 4f).toInt(),
                paint = Paint(cellBoldPaint).apply { color = Color.rgb(3, 105, 161); textSize = 6.6f },
                maxLines = 1,
                minTextSize = 5.4f
            )
            val windSpdLayout = buildAutoFitStaticLayout(
                text = "${row.windSpeedKmh} km/jam",
                maxWidth = (colWidth(7) - 4f).toInt(),
                paint = cellSubPaint,
                maxLines = 1,
                minTextSize = 5.2f
            )
            canvas.save()
            canvas.translate(colStart(7) + (colWidth(7) - windDirLayout.getLineWidth(0)) / 2f, y + 6f)
            windDirLayout.draw(canvas)
            canvas.restore()
            canvas.save()
            canvas.translate(colStart(7) + (colWidth(7) - windSpdLayout.getLineWidth(0)) / 2f, y + 6f + windDirLayout.height + 2f)
            windSpdLayout.draw(canvas)
            canvas.restore()

            y += rowH
            canvas.drawLine(tableLeft, y, tableRight, y, linePaint)
        }

        // Garis Vertikal & Bingkai Tabel
        for (boundX in colBounds) {
            canvas.drawLine(boundX, tableTopY, boundX, y, linePaint)
        }
        canvas.drawRect(
            RectF(tableLeft, tableTopY, tableRight, y),
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(148, 163, 184)
                style = Paint.Style.STROKE
                strokeWidth = 0.9f
            }
        )

        y += 12f

        // Kotak Catatan Kesiapsiagaan Hidrometeorologi BMKG (Spasi antar-kata dipertegas & kalimat lebih jelas)
        val notePaddingH = 12f
        val notePaddingV = 10f
        val noteHeaderH = 12f
        val noteGap = 6f
        val noteBodyLayout = buildAutoFitStaticLayout(
            text = "Prakiraan cuaca harian 15 Kecamatan Kabupaten Mukomuko ini digunakan sebagai acuan kesiapsiagaan personil TRC Pusdalops-PB BPBD terhadap potensi hujan lebat, angin kencang pesisir Samudra Hindia, serta kenaikan muka air sungai.",
            maxWidth = (tableWidth - notePaddingH * 2).toInt(),
            paint = Paint(cellTextPaint).apply {
                color = Color.rgb(30, 41, 59)
                textSize = 7.4f
            },
            maxLines = 4,
            minTextSize = 6.8f,
            lineSpacingExtra = 3.2f,
            lineSpacingMultiplier = 1.32f
        )
        val noteBoxH = notePaddingV + noteHeaderH + noteGap + noteBodyLayout.height + notePaddingV
        val noteRect = RectF(tableLeft, y, tableRight, y + noteBoxH)
        canvas.drawRoundRect(noteRect, 6f, 6f, Paint().apply { color = Color.rgb(239, 246, 255) })
        canvas.drawRoundRect(noteRect, 6f, 6f, Paint().apply {
            color = Color.rgb(96, 165, 250)
            style = Paint.Style.STROKE
            strokeWidth = 1.0f
        })
        PdfTextAutoShrinkHelper.drawAutoShrinkText(
            canvas = canvas,
            text = "CATATAN LAMPIRAN PRAKIRAAN CUACA HARIAN BMKG - PUSDALOPS BPBD MUKOMUKO:",
            x = tableLeft + notePaddingH,
            y = y + notePaddingV + 8.5f,
            maxWidth = tableWidth - notePaddingH * 2,
            basePaint = Paint(cellBoldPaint).apply { color = Color.rgb(30, 64, 175); textSize = 7.6f },
            initialFontSize = 7.6f,
            minFontSize = 6.6f
        )
        canvas.save()
        canvas.translate(tableLeft + notePaddingH, y + notePaddingV + noteHeaderH + noteGap)
        noteBodyLayout.draw(canvas)
        canvas.restore()

        drawFooterAtribusiResmi(canvas, pageWidth, pageHeight, margin, documentSettings = settings)
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
