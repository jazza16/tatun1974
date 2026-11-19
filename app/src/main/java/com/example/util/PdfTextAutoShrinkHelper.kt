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
     * Menggambar teks satu baris pada `canvas` dengan logika Auto-Shrink otomatis.
     *
     * @param canvas Canvas halaman PDF.
     * @param text Teks target (misalnya Nama + Gelar atau baris NIP).
     * @param x Koordinat horizontal (mengikuti `basePaint.textAlign`: LEFT, CENTER, atau RIGHT).
     * @param y Baseline vertikal teks.
     * @param maxWidth Lebar maksimum kotak/kolom yang tersedia (dalam pt).
     * @param basePaint Gaya Paint awal (warna, typeface, underline, alignment) — tidak dimutasi.
     * @param initialFontSize Ukuran font awal dari `DocumentSettings.fontSize`.
     * @param minFontSize Batas minimum ukuran font dari `DocumentSettings.minFontSize`.
     * @param maxLengthBeforeTruncate Batas panjang karakter maksimum sebelum pemotongan dari `DocumentSettings`.
     * @return Ukuran font akhir (pt) yang digunakan saat menggambar teks.
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

        val resolvedSize = computeAutoShrinkFontSize(
            text = lengthCapped,
            paint = basePaint,
            maxWidth = maxWidth,
            initialFontSize = initialFontSize,
            minFontSize = minFontSize
        )

        val drawPaint = Paint(basePaint).apply {
            textSize = resolvedSize
        }

        val finalText = fitTextToWidth(
            text = lengthCapped,
            paint = drawPaint,
            maxWidth = maxWidth,
            maxLengthBeforeTruncate = maxLengthBeforeTruncate
        )

        canvas.drawText(finalText, x, y, drawPaint)
        return resolvedSize
    }
}
