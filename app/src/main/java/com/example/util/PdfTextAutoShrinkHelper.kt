package com.example.util

import android.graphics.Canvas
import android.graphics.Paint
import android.text.TextPaint
import android.text.TextUtils

/**
 * Utilitas Auto-Shrink Text untuk dokumen PDF berbasis Android Canvas/Paint.
 *
 * Jika teks Nama + Gelar Petugas/Pejabat (atau metadata lain) melebihi lebar kotak (`maxWidth`),
 * ukuran font otomatis diperkecil secara bertahap hingga muat dengan rapi tanpa terpotong atau menabrak
 * batas kolom. Jika pada ukuran font minimum (`minFontSize`) teks masih melebihi lebar, diterapkan
 * pemotongan ellipsis yang bersih.
 */
object PdfTextAutoShrinkHelper {

    /**
     * Menghitung ukuran font optimal (pt) agar `text` muat dalam `maxWidth` pada `paint`.
     * Tidak memutasi `paint.textSize` milik pemanggil.
     */
    fun computeAutoShrinkFontSize(
        text: String,
        paint: Paint,
        maxWidth: Float,
        initialFontSize: Float = paint.textSize,
        minFontSize: Float = 5.5f,
        step: Float = 0.25f
    ): Float {
        val measurePaint = Paint(paint)
        return computeAutoShrinkFontSize(
            text = text,
            maxWidth = maxWidth,
            initialFontSize = initialFontSize,
            minFontSize = minFontSize,
            step = step
        ) { candidate, candidateSize ->
            measurePaint.textSize = candidateSize
            measurePaint.measureText(candidate)
        }
    }

    /**
     * Overload murni (platform-independent) untuk menghitung ukuran font auto-shrink
     * dengan fungsi pengukur lebar teks kustom.
     */
    fun computeAutoShrinkFontSize(
        text: String,
        maxWidth: Float,
        initialFontSize: Float,
        minFontSize: Float = 5.5f,
        step: Float = 0.25f,
        widthMeasurer: (text: String, fontSize: Float) -> Float
    ): Float {
        val safeInitial = initialFontSize.coerceAtLeast(4.5f)
        val safeMin = minFontSize.coerceIn(4.0f, safeInitial)
        if (text.isEmpty() || maxWidth <= 0f) return safeInitial

        var currentSize = safeInitial
        val safeStep = step.coerceAtLeast(0.1f)

        while (currentSize > safeMin) {
            if (widthMeasurer(text, currentSize) <= maxWidth) {
                return currentSize
            }
            currentSize = (currentSize - safeStep).coerceAtLeast(safeMin)
        }
        return safeMin
    }

    /**
     * Menyiapkan teks agar tidak melebihi `maxLengthBeforeTruncate` dan tetap muat dalam `maxWidth`
     * pada `paint` (dengan ellipsis bila masih meluap setelah mencapai `minFontSize`).
     */
    fun fitTextToWidth(
        text: String,
        paint: Paint,
        maxWidth: Float,
        maxLengthBeforeTruncate: Int = Int.MAX_VALUE
    ): String {
        val trimmed = text.replace("\r", " ").replace("\n", " ").trim()
        val lengthCapped = if (maxLengthBeforeTruncate in 4 until trimmed.length) {
            trimmed.take(maxLengthBeforeTruncate - 3).trimEnd() + "..."
        } else {
            trimmed
        }
        if (maxWidth <= 0f || paint.measureText(lengthCapped) <= maxWidth) {
            return lengthCapped
        }
        return TextUtils.ellipsize(
            lengthCapped,
            TextPaint(paint),
            maxWidth,
            TextUtils.TruncateAt.END
        ).toString()
    }

    /**
     * Memperlebar spasi antar-kata dan mengaktifkan subpixel/linear text metrics khusus untuk
     * rendering Android `PdfDocument` (SkPDF).
     *
     * Pada mesin PDF native Android (`PdfDocument`), lebar glyph spasi tunggal (`' '`) pada ukuran
     * font kecil (6pt–9pt) dan huruf tebal (`FakeBoldText` / `Typeface.BOLD`) sering terkompresi
     * sehingga kata-kata tampak menempel ("HujanRintik", "BerawanTebal", "CATATANLAMPIRAN...").
     * Fungsi ini menggandakan jarak visual spasi antar-kata secara presisi tanpa mengubah baris baru (`\n`).
     */
    fun normalizePdfWordSpacing(text: String): String {
        if (text.isEmpty()) return text
        // Pertahankan baris baru (\n), namun ganti setiap spasi tunggal/berurutan antar-kata
        // dengan dua spasi penuh ("  ") agar di PDF terlihat memiliki jarak pemisah kata yang tegas.
        return text.lines().joinToString("\n") { line ->
            val trimmedLine = line.trim()
            if (trimmedLine.isEmpty()) {
                ""
            } else {
                trimmedLine.replace(Regex("[ \\t]+"), "  ")
            }
        }
    }

    /**
     * Mengonfigurasi `Paint` / `TextPaint` agar memiliki `letterSpacing` dan `isSubpixelText`
     * yang optimal saat dirender ke Canvas `PdfDocument`.
     */
    fun <T : Paint> configurePdfPaint(paint: T, letterSpacingEm: Float = 0.035f): T {
        paint.isAntiAlias = true
        paint.isSubpixelText = true
        paint.isLinearText = true
        if (paint.letterSpacing < letterSpacingEm) {
            paint.letterSpacing = letterSpacingEm
        }
        return paint
    }

    /**
     * Menggambar teks satu baris pada `canvas` dengan penegasan spasi antar-kata PDF.
     */
    fun drawPdfText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        paint: Paint,
        letterSpacingEm: Float = 0.035f
    ) {
        val pdfPaint = configurePdfPaint(Paint(paint), letterSpacingEm)
        val spacedText = normalizePdfWordSpacing(text)
        canvas.drawText(spacedText, x, y, pdfPaint)
    }

    /**
     * Menggambar teks satu baris pada `canvas` dengan logika Auto-Shrink otomatis
     * serta jarak spasi antar-kata yang tegas untuk dokumen PDF.
     */
    fun drawAutoShrinkText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        maxWidth: Float,
        basePaint: Paint,
        initialFontSize: Float = basePaint.textSize,
        minFontSize: Float = 5.5f,
        maxLengthBeforeTruncate: Int = Int.MAX_VALUE
    ): Float {
        val cleanText = text.replace("\r", " ").replace("\n", " ").trim()
        val lengthCapped = if (maxLengthBeforeTruncate in 4 until cleanText.length) {
            cleanText.take(maxLengthBeforeTruncate - 3).trimEnd() + "..."
        } else {
            cleanText
        }
        val spacedText = normalizePdfWordSpacing(lengthCapped)
        val configuredBase = configurePdfPaint(Paint(basePaint), 0.03f)

        val resolvedSize = computeAutoShrinkFontSize(
            text = spacedText,
            paint = configuredBase,
            maxWidth = maxWidth,
            initialFontSize = initialFontSize,
            minFontSize = minFontSize
        )

        val drawPaint = Paint(configuredBase).apply {
            textSize = resolvedSize
        }

        val finalText = fitTextToWidth(
            text = spacedText,
            paint = drawPaint,
            maxWidth = maxWidth,
            maxLengthBeforeTruncate = Int.MAX_VALUE
        )

        canvas.drawText(finalText, x, y, drawPaint)
        return resolvedSize
    }
}
