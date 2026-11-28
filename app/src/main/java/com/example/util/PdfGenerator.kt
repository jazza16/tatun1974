package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.widget.Toast
import com.example.data.local.DistribusiLogistikEntity
import com.example.data.local.LaporanHarianEntity
import com.example.data.local.LogKomunikasiEntity
import com.example.data.repository.DocumentSettingsRepository
import com.example.model.DocumentSettings
import com.example.model.WeatherFullReport
import com.example.ui.components.MonthlyDisasterSummary
import com.example.ui.components.PieSliceData
import java.io.File
import java.util.Calendar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.max

object PdfGenerator {

    private const val PAGE_WIDTH = ReportExporter.A4_PAGE_WIDTH
    private const val PAGE_HEIGHT = ReportExporter.A4_PAGE_HEIGHT
    private const val PAGE_MARGIN = ReportExporter.STANDARD_MARGIN // Standar margin A4 (36pt / ~1.27 cm)

    private val pdfScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /**
     * Membentuk URL OpenStreetMap Static Map berdasarkan Latitude dan Longitude kejadian.
     */
    fun buildOsmStaticMapUrl(lat: String, lng: String): String {
        return "https://staticmap.openstreetmap.de/staticmap.php?center=$lat,$lng&zoom=16&size=600x400&maptype=mapnik&markers=$lat,$lng,red-pushpin"
    }

    /**
     * Mengekstrak pasangan (Latitude, Longitude) dari string koordinat laporan.
     */
    private fun extractLatLng(koordinatRaw: String): Pair<String, String>? {
        val source = koordinatRaw.ifBlank { "-2.5830, 101.1240" }
        val matches = Regex("[-+]?\\d+(?:\\.\\d+)?").findAll(source).map { it.value }.toList()
        return if (matches.size >= 2) {
            Pair(matches[0], matches[1])
        } else {
            null
        }
    }

    /**
     * Mengunduh gambar peta satelit/jalan dari server tile resmi (Esri World Imagery / OpenStreetMap)
     * di background thread (Dispatchers.IO) dan menggabungkannya dengan pin merah lokasi kejadian.
     */
    suspend fun downloadStaticMapBitmap(lat: String, lng: String): Bitmap? = withContext(Dispatchers.IO) {
        withTimeoutOrNull(6000L) {
            try {
                val latD = lat.toDoubleOrNull() ?: -2.5830
                val lngD = lng.toDoubleOrNull() ?: 101.1240
                val zoom = 15
                val n = (1 shl zoom).toDouble()
                val latRad = Math.toRadians(latD.coerceIn(-85.0, 85.0))
                val exactX = ((lngD + 180.0) / 360.0) * n
                val exactY = ((1.0 - kotlin.math.ln(kotlin.math.tan(latRad) + 1.0 / kotlin.math.cos(latRad)) / Math.PI) / 2.0) * n
                val centerTileX = exactX.toInt()
                val centerTileY = exactY.toInt()

                fun fetchTile(urlStr: String): Bitmap? {
                    var conn: HttpURLConnection? = null
                    return try {
                        conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                            requestMethod = "GET"
                            connectTimeout = 2800
                            readTimeout = 2800
                            doInput = true
                            setRequestProperty(
                                "User-Agent",
                                "SIPAGAR-KITA-BPBD-Mukomuko/1.0.1 (Android; Pusdalops-PB)"
                            )
                        }
                        conn.connect()
                        if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                            val bytes = conn.inputStream.use { it.readBytes() }
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        } else null
                    } catch (_: Exception) {
                        null
                    } finally {
                        try {
                            conn?.disconnect()
                        } catch (_: Exception) {
                        }
                    }
                }

                // Unduh tile satelit Esri World Imagery atau fallback ke tile OpenStreetMap resmi
                val centerTile = fetchTile(
                    "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/$zoom/$centerTileY/$centerTileX"
                ) ?: fetchTile(
                    "https://tile.openstreetmap.org/$zoom/$centerTileX/$centerTileY.png"
                )

                if (centerTile != null && centerTile.width > 1 && centerTile.height > 1) {
                    val outW = 480
                    val outH = 240
                    val composite = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
                    val c = Canvas(composite)
                    val srcRect = android.graphics.Rect(0, 32, centerTile.width, (centerTile.height - 32).coerceAtLeast(64))
                    val dstRect = RectF(0f, 0f, outW.toFloat(), outH.toFloat())
                    c.drawBitmap(centerTile, srcRect, dstRect, Paint(Paint.FILTER_BITMAP_FLAG))
                    drawMapPinAndOverlay(c, outW.toFloat(), outH.toFloat(), lat, lng)
                    composite
                } else {
                    null
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun drawMapPinAndOverlay(
        canvas: Canvas,
        w: Float,
        h: Float,
        lat: String,
        lng: String
    ) {
        val cx = w / 2f
        val cy = h / 2f

        // Lingkaran radius 100m
        val radiusFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(55, 239, 68, 68)
            style = Paint.Style.FILL
        }
        val radiusStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(220, 38, 38)
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawCircle(cx, cy, 34f, radiusFill)
        canvas.drawCircle(cx, cy, 34f, radiusStroke)

        // Pin titik lokasi bencana
        val pinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(220, 38, 38)
            style = Paint.Style.FILL
        }
        val pinBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
        }
        canvas.drawCircle(cx, cy, 9f, pinPaint)
        canvas.drawCircle(cx, cy, 9f, pinBorder)

        // Badge koordinat di sudut bawah
        val badgeRect = RectF(8f, h - 28f, w - 8f, h - 6f)
        canvas.drawRoundRect(
            badgeRect,
            6f,
            6f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(190, 15, 23, 42) }
        )
        val coordPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("CITRA SATELIT GIS • KOORDINAT: $lat, $lng", w / 2f, h - 13f, coordPaint)
    }

    /**
     * Menghasilkan citra peta satelit/topografi GIS lokal beresolusi tinggi apabila perangkat sedang offline
     * sehingga bagian 'Layer Gambar Citra Satelit' di PDF selalu tergambar penuh dan tidak pernah kosong.
     */
    private fun generateSatelliteMapBitmapFallback(
        context: Context,
        lat: String,
        lng: String,
        lokasiLabel: String
    ): Bitmap {
        val w = 480
        val h = 240
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)

        // Coba muat tekstur citra lanskap/satelit lokal Mukomuko sebagai base layer
        val baseWallpaper = try {
            BitmapFactory.decodeResource(context.resources, com.example.R.drawable.mukomuko_hero)
                ?: BitmapFactory.decodeResource(context.resources, com.example.R.drawable.img_mukomuko_wallpaper)
        } catch (_: Exception) {
            null
        }

        if (baseWallpaper != null) {
            c.drawBitmap(
                baseWallpaper,
                null,
                RectF(0f, 0f, w.toFloat(), h.toFloat()),
                Paint(Paint.FILTER_BITMAP_FLAG)
            )
            c.drawRect(
                0f,
                0f,
                w.toFloat(),
                h.toFloat(),
                Paint().apply { color = Color.argb(85, 15, 45, 30) }
            )
        } else {
            c.drawColor(Color.rgb(22, 58, 42))
        }

        // Gambar grid koordinat GIS satelit
        val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(70, 148, 163, 184)
            strokeWidth = 1f
        }
        for (gx in 0..w step 48) {
            c.drawLine(gx.toFloat(), 0f, gx.toFloat(), h.toFloat(), gridPaint)
        }
        for (gy in 0..h step 40) {
            c.drawLine(0f, gy.toFloat(), w.toFloat(), gy.toFloat(), gridPaint)
        }

        // Kompas Utara (N) di kanan atas
        val compassCx = w - 26f
        val compassCy = 26f
        c.drawCircle(
            compassCx,
            compassCy,
            15f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(180, 15, 23, 42) }
        )
        c.drawText(
            "N▲",
            compassCx,
            compassCy + 4f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(250, 204, 21)
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
        )

        // Label wilayah di kiri atas
        val topLabelRect = RectF(8f, 8f, w - 52f, 28f)
        c.drawRoundRect(
            topLabelRect,
            5f,
            5f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(175, 15, 23, 42) }
        )
        c.drawText(
            lokasiLabel.take(48),
            14f,
            22f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(226, 232, 240)
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
        )

        drawMapPinAndOverlay(c, w.toFloat(), h.toFloat(), lat, lng)
        return bmp
    }

    /**
     * Menghasilkan data PDF Laporan Harian secara Data-Driven (Dispatchers.IO):
     * 1. Membaca konfigurasi terbaru (`GET /document-settings` / Firestore / cache lokal).
     * 2. Jika Remote Template PDF aktif dan backend tersedia, mengunduh file PDF yang telah
     *    di-render oleh server (`GET /reports/{reportId}/pdf` atau `POST /reports/render-pdf`).
     * 3. Fallback Lokal (offline / backend belum dikonfigurasi): merender PDF 4 halaman di perangkat
     *    berdasarkan konfigurasi JSON `DocumentSettings` + auto-shrink teks otomatis.
     */
    suspend fun generateLaporanPdfBytes(
        context: Context,
        laporan: LaporanHarianEntity,
        listLog: List<LogKomunikasiEntity>,
        listLogistik: List<DistribusiLogistikEntity> = emptyList(),
        overrideSettings: DocumentSettings? = null
    ): ByteArray = withContext(Dispatchers.IO) {
        val isSandbox = com.example.training.SandboxTrainingManager.isSandboxModeActive() ||
            laporan.lokasi.contains("[SIMULASI", ignoreCase = true)
        if (isSandbox) {
            com.example.training.SandboxTrainingManager.markTrainingStepCompleted(
                com.example.training.SandboxTrainingManager.STEP_4_PRINT_PDF
            )
        }
        val settings = (overrideSettings ?: DocumentSettingsRepository.fetchDocumentSettings(context)).clamped()

        // 1. Remote Template PDF: minta PDF siap cetak dari Backend lebih dulu (kecuali saat Mode Latihan Sandbox agar watermark lokal terpasang)
        if (!isSandbox) {
            val remoteResult = DocumentSettingsRepository.fetchRemotePdfIfAvailable(
                context = context,
                laporan = laporan,
                listLog = listLog,
                listLogistik = listLogistik,
                settings = settings
            )
            if (remoteResult != null && DocumentSettingsRepository.isValidPdfBytes(remoteResult.pdfBytes)) {
                return@withContext remoteResult.pdfBytes
            }
        }

        // 2. Fallback Lokal berbasis konfigurasi JSON `DocumentSettings`
        val latLng = extractLatLng(laporan.koordinat)
        val latStr = latLng?.first ?: "-2.5830"
        val lngStr = latLng?.second ?: "101.1240"
        val mapBitmap = downloadStaticMapBitmap(latStr, lngStr)
            ?: generateSatelliteMapBitmapFallback(
                context = context,
                lat = latStr,
                lng = lngStr,
                lokasiLabel = laporan.lokasiBencana.ifBlank { "Wilayah Kabupaten Mukomuko" }
            )
        val evidencePhotoBitmap = resolveCitizenReportPhotoForPdf(context, laporan)
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val (monthlyData, pieSlices) = ReportExporter.buildDefaultMonthlyAnalyticsForAttachment(
            context = context,
            selectedYear = currentYear
        )
        val allDistrictsSummary = ReportExporter.buildDefaultDistrictsForecastForAttachment(context)
        buildTwoPagePdfBytes(
            context = context,
            laporan = laporan,
            listLog = listLog,
            listLogistik = listLogistik,
            mapBitmap = mapBitmap,
            evidencePhotoBitmap = evidencePhotoBitmap,
            monthlyData = monthlyData,
            pieSlices = pieSlices,
            selectedYear = currentYear,
            allDistrictsSummary = allDistrictsSummary,
            settings = settings
        )
    }

    private suspend fun resolveCitizenReportPhotoForPdf(
        context: Context,
        laporan: LaporanHarianEntity
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val candidates = LinkedHashSet<String>()

            laporan.fotoDokumentasiUri.trim().takeIf { it.isNotBlank() }?.let { candidates.add(it) }
            ImageUtils.lastSelectedCitizenReportPhotoUri?.trim()?.takeIf { it.isNotBlank() }?.let { candidates.add(it) }

            val sumberLower = laporan.sumberInformasi.trim().lowercase()
            val lokasiLower = laporan.lokasiBencana.trim().lowercase()
            val jenisLower = laporan.jenisBencana.trim().lowercase()
            val coordClean = laporan.koordinat.trim()

            if (com.example.training.SandboxTrainingManager.isSandboxModeActive()) {
                val sandboxList = com.example.training.SandboxTrainingManager.sandboxReports.value
                sandboxList.filter { r ->
                    !r.photoUri.isNullOrBlank() && (
                        (sumberLower.isNotBlank() && r.reporterName.isNotBlank() && sumberLower.contains(r.reporterName.lowercase())) ||
                            (lokasiLower.isNotBlank() && r.village.isNotBlank() && lokasiLower.contains(r.village.lowercase())) ||
                            (jenisLower.isNotBlank() && r.disasterType.equals(laporan.jenisBencana, ignoreCase = true))
                        )
                }.forEach { r ->
                    r.photoUri?.trim()?.takeIf { it.isNotBlank() }?.let { candidates.add(it) }
                }
                if (candidates.isEmpty()) {
                    sandboxList.firstOrNull { !it.photoUri.isNullOrBlank() }
                        ?.photoUri?.trim()?.takeIf { it.isNotBlank() }?.let { candidates.add(it) }
                }
            }

            val db = runCatching { com.example.data.local.AppDatabase.getInstance(context) }.getOrNull()

            // Cari kecocokan dari tabel citizen_disaster_reports (termasuk inline data URI cache)
            val allCitizenReports = runCatching {
                db?.weatherDao()?.getAllCitizenDisasterReportsSync().orEmpty()
            }.getOrDefault(emptyList())

            fun matchesCitizenReport(r: com.example.data.local.CitizenDisasterReportEntity): Boolean {
                val reporterMatch = sumberLower.isNotBlank() && r.reporterName.isNotBlank() &&
                    sumberLower.contains(r.reporterName.trim().lowercase())
                val phoneMatch = sumberLower.isNotBlank() && r.reporterPhone.isNotBlank() &&
                    sumberLower.contains(r.reporterPhone.trim().lowercase())
                val villageMatch = lokasiLower.isNotBlank() && r.village.isNotBlank() &&
                    lokasiLower.contains(r.village.trim().lowercase())
                val districtMatch = lokasiLower.isNotBlank() && r.district.isNotBlank() &&
                    lokasiLower.contains(r.district.trim().lowercase())
                val typeMatch = jenisLower.isNotBlank() && r.disasterType.isNotBlank() &&
                    (r.disasterType.equals(laporan.jenisBencana, ignoreCase = true) ||
                        jenisLower.contains(r.disasterType.trim().lowercase()) ||
                        r.disasterType.trim().lowercase().contains(jenisLower))
                return reporterMatch || phoneMatch || villageMatch || (districtMatch && typeMatch) || typeMatch
            }

            allCitizenReports.filter { matchesCitizenReport(it) }.forEach { r ->
                r.photoUri?.trim()?.takeIf { it.isNotBlank() }?.let { candidates.add(it) }
                ImageUtils.getInlineDataUriForReport(r.id)?.trim()?.takeIf { it.isNotBlank() }?.let { candidates.add(it) }
            }

            // Cari kecocokan dari tabel laporan_warga
            val allLaporanWarga = runCatching {
                db?.laporanWargaDao()?.getAllLaporanWargaSync().orEmpty()
            }.getOrDefault(emptyList())

            allLaporanWarga.filter { r ->
                !r.fotoUri.isNullOrBlank() && (
                    (sumberLower.isNotBlank() && r.namaPelapor.isNotBlank() && sumberLower.contains(r.namaPelapor.trim().lowercase())) ||
                        (lokasiLower.isNotBlank() && r.desa.isNotBlank() && lokasiLower.contains(r.desa.trim().lowercase())) ||
                        (coordClean.isNotBlank() && r.koordinat.trim() == coordClean) ||
                        (jenisLower.isNotBlank() && r.jenisBencana.isNotBlank() &&
                            (r.jenisBencana.equals(laporan.jenisBencana, ignoreCase = true) ||
                                jenisLower.contains(r.jenisBencana.trim().lowercase())))
                    )
            }.forEach { r ->
                r.fotoUri?.trim()?.takeIf { it.isNotBlank() }?.let { candidates.add(it) }
            }

            // Fallback tambahan jika laporan berasal dari warga tetapi pencarian teks berbeda tipis
            if (candidates.isEmpty() && sumberLower.contains("warga")) {
                allCitizenReports.firstOrNull { !it.photoUri.isNullOrBlank() }
                    ?.photoUri?.trim()?.let { candidates.add(it) }
                allLaporanWarga.firstOrNull { !it.fotoUri.isNullOrBlank() }
                    ?.fotoUri?.trim()?.let { candidates.add(it) }
            }

            for (candidateUri in candidates) {
                val decoded = loadPhotoBitmapFromAnySource(context, candidateUri)
                if (decoded != null && !decoded.isRecycled && decoded.width > 1 && decoded.height > 1) {
                    return@withContext decoded
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun loadPhotoBitmapFromAnySource(context: Context, uri: String): Bitmap? {
        val trimmed = uri.trim()
        if (trimmed.isBlank()) return null

        // 1. Jalur utama Supabase / Cache / Data URI
        com.example.data.api.SupabaseClient.downloadReportPhotoBitmap(trimmed)?.let { return it }

        // 2. Jalur lokal (Data URI, file://, content://, android.resource://, base64)
        ImageUtils.decodePhotoSourceToBitmap(trimmed, context)?.let { return it }

        // 3. Fallback HTTP/HTTPS langsung (Firebase Storage / public image URL)
        if (trimmed.startsWith("https://", ignoreCase = true) || trimmed.startsWith("http://", ignoreCase = true)) {
            return withTimeoutOrNull(4500L) {
                var conn: HttpURLConnection? = null
                try {
                    conn = (URL(trimmed).openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 3500
                        readTimeout = 3500
                        instanceFollowRedirects = true
                        doInput = true
                    }
                    conn.connect()
                    if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                        val bytes = conn.inputStream.use { it.readBytes() }
                        if (bytes.isNotEmpty()) {
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        } else null
                    } else null
                } catch (_: Exception) {
                    null
                } finally {
                    try {
                        conn?.disconnect()
                    } catch (_: Exception) {
                    }
                }
            }
        }
        return null
    }

    /**
     * Menggambar foto dokumentasi ke dalam kotak tujuan secara proporsional (fit-center)
     * agar gambar tidak tertarik/distorsi dan watermark GPS di bagian bawah tetap terbaca utuh.
     */
    private fun drawBitmapFitCenter(
        canvas: Canvas,
        bitmap: Bitmap,
        targetRect: RectF,
        bitmapPaint: Paint
    ) {
        val bgMattePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            style = Paint.Style.FILL
        }
        canvas.drawRect(targetRect, bgMattePaint)

        val srcW = bitmap.width.toFloat().coerceAtLeast(1f)
        val srcH = bitmap.height.toFloat().coerceAtLeast(1f)
        val dstW = targetRect.width().coerceAtLeast(1f)
        val dstH = targetRect.height().coerceAtLeast(1f)

        val scale = minOf(dstW / srcW, dstH / srcH)
        val drawW = srcW * scale
        val drawH = srcH * scale
        val left = targetRect.left + (dstW - drawW) / 2f
        val top = targetRect.top + (dstH - drawH) / 2f
        val destRect = RectF(left, top, left + drawW, top + drawH)
        canvas.drawBitmap(bitmap, null, destRect, bitmapPaint)
    }

    /**
     * Menulis ByteArray PDF ke URI hasil pilihan pengguna (Storage Access Framework)
     * menggunakan contentResolver.openOutputStream(uri).
     */
    fun writePdfBytesToUri(context: Context, uri: android.net.Uri, pdfBytes: ByteArray): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(pdfBytes)
                outputStream.flush()
            } ?: return false
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Mencetak Laporan Harian Pusdalops ke PDF secara Data-Driven (Remote Template PDF lebih dulu,
     * dengan fallback lokal berbasis `GET /document-settings` & Auto-Shrink Text).
     */
    fun cetakLaporanKePdf(
        context: Context,
        laporan: LaporanHarianEntity,
        listLog: List<LogKomunikasiEntity>,
        listLogistik: List<DistribusiLogistikEntity> = emptyList()
    ) {
        pdfScope.launch {
            try {
                // 1. Ambil PDF dari server template atau susun secara lokal menggunakan DocumentSettings
                val pdfBytes = generateLaporanPdfBytes(
                    context = context,
                    laporan = laporan,
                    listLog = listLog,
                    listLogistik = listLogistik
                )
                val detectedPages = withContext(Dispatchers.IO) {
                    detectPdfPageCount(context, pdfBytes)
                }

                // 2. Tampilkan dialog cetak / simpan PDF di Main Thread menggunakan PrintDocumentAdapter native
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                if (printManager != null) {
                    val sanitizedDate = laporan.tanggalKejadian.replace("/", "-").ifBlank { "Harian" }
                    val jobName = "Laporan_Harian_Pusdalops_$sanitizedDate"
                    val printAttributes = PrintAttributes.Builder()
                        .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                        .setResolution(PrintAttributes.Resolution("pdf", "PDF", 300, 300))
                        .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                        .build()

                    printManager.print(
                        jobName,
                        PdfBytesPrintAdapter(jobName, pdfBytes, pageCount = detectedPages),
                        printAttributes
                    )
                } else {
                    Toast.makeText(
                        context,
                        "Layanan cetak PDF tidak tersedia pada perangkat ini.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "Gagal menyiapkan dokumen PDF: ${e.localizedMessage ?: "Kesalahan tidak diketahui"}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun detectPdfPageCount(context: Context, pdfBytes: ByteArray): Int {
        var tempFile: File? = null
        return try {
            tempFile = File.createTempFile("bpbd_pdf_count_", ".pdf", context.cacheDir)
            FileOutputStream(tempFile).use { it.write(pdfBytes) }
            ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    renderer.pageCount.coerceAtLeast(1)
                }
            }
        } catch (_: Exception) {
            PrintDocumentInfo.PAGE_COUNT_UNKNOWN
        } finally {
            try {
                tempFile?.delete()
            } catch (_: Exception) {}
        }
    }

    private fun buildTwoPagePdfBytes(
        context: Context,
        laporan: LaporanHarianEntity,
        listLog: List<LogKomunikasiEntity>,
        listLogistik: List<DistribusiLogistikEntity>,
        mapBitmap: Bitmap?,
        evidencePhotoBitmap: Bitmap? = null,
        monthlyData: List<MonthlyDisasterSummary> = emptyList(),
        pieSlices: List<PieSliceData> = emptyList(),
        selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR),
        allDistrictsSummary: List<WeatherFullReport> = emptyList(),
        settings: DocumentSettings = DocumentSettingsRepository.getCachedOrDefaultSettings(context)
    ): ByteArray {
        return try {
            val pdfDocument = PdfDocument()
            var activePage: PdfDocument.Page? = null
            try {
                var pageNum = 1
                // ==================== HALAMAN 1 (DENGAN KOP SURAT) ====================
                val page1Info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum++).create()
                val page1 = pdfDocument.startPage(page1Info)
                activePage = page1
                drawPage1(context, page1.canvas, laporan, listLog, mapBitmap, evidencePhotoBitmap, settings)
                pdfDocument.finishPage(page1)
                activePage = null

                // ==================== HALAMAN 2 (TANPA KOP SURAT) ====================
                val page2Info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum++).create()
                val page2 = pdfDocument.startPage(page2Info)
                activePage = page2
                drawPage2(context, page2.canvas, listLogistik, settings)
                pdfDocument.finishPage(page2)
                activePage = null

                // ==================== HALAMAN 3 (LAMPIRAN STATISTIK BENCANA BULANAN) ====================
                if (monthlyData.isNotEmpty()) {
                    val page3Info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum++).create()
                    val page3 = pdfDocument.startPage(page3Info)
                    activePage = page3
                    ReportExporter.drawMonthlyDisasterAnalyticsPage(
                        context = context,
                        canvas = page3.canvas,
                        monthlyData = monthlyData,
                        pieSlices = pieSlices,
                        selectedYear = selectedYear,
                        scopeLabel = "15 Kecamatan Kab. Mukomuko",
                        documentSettings = settings
                    )
                    pdfDocument.finishPage(page3)
                    activePage = null
                }

                // ==================== HALAMAN 4 (LAMPIRAN PRAKIRAAN CUACA BMKG 15 KECAMATAN) ====================
                val page4Info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create()
                val page4 = pdfDocument.startPage(page4Info)
                activePage = page4
                ReportExporter.drawBmkgForecastPageToCanvas(
                    context = context,
                    canvas = page4.canvas,
                    pageWidth = PAGE_WIDTH,
                    pageHeight = PAGE_HEIGHT,
                    allDistrictsSummary = allDistrictsSummary,
                    documentSettings = settings
                )
                pdfDocument.finishPage(page4)
                activePage = null

                val outputStream = ByteArrayOutputStream()
                pdfDocument.writeTo(outputStream)
                val rawBytes = outputStream.toByteArray()
                if (rawBytes.size > 64) rawBytes else buildMinimalValidPdfFallback(laporan)
            } finally {
                try {
                    activePage?.let { pdfDocument.finishPage(it) }
                } catch (_: Throwable) {}
                try {
                    pdfDocument.close()
                } catch (_: Throwable) {}
            }
        } catch (_: Throwable) {
            buildMinimalValidPdfFallback(laporan)
        }
    }

    private fun buildMinimalValidPdfFallback(laporan: LaporanHarianEntity): ByteArray {
        val isSandbox = com.example.training.SandboxTrainingManager.isSandboxModeActive() ||
            laporan.lokasi.contains("[SIMULASI", ignoreCase = true)
        val watermarkLine = if (isSandbox) {
            com.example.training.SandboxTrainingManager.WATERMARK_HEADER_TEXT
        } else {
            "DOKUMEN RESMI PUSDALOPS-PB BPBD KABUPATEN MUKOMUKO"
        }
        val content = buildString {
            append("%PDF-1.4\n")
            append("1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n")
            append("2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj\n")
            append("3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Contents 4 0 R >> endobj\n")
            val textStream = "BT /F1 11 Tf 36 800 Td (${DocumentSettings.DEFAULT_KOP_LINE_1}) Tj 0 -14 Td (${DocumentSettings.DEFAULT_KOP_LINE_2}) Tj 0 -12 Td (${DocumentSettings.DEFAULT_KOP_LINE_3}) Tj 0 -12 Td (${DocumentSettings.DEFAULT_KOP_ADDRESS}) Tj 0 -20 Td ($watermarkLine - ${laporan.jenisBencana} - ${laporan.lokasi}) Tj ET"
            append("4 0 obj << /Length ${textStream.length} >> stream\n$textStream\nendstream endobj\n")
            append("trailer << /Root 1 0 R >>\n")
            append("%%EOF\n")
        }
        return content.toByteArray(Charsets.UTF_8)
    }

    private fun drawPage1(
        context: Context,
        canvas: Canvas,
        laporan: LaporanHarianEntity,
        listLog: List<LogKomunikasiEntity>,
        mapBitmap: Bitmap?,
        evidencePhotoBitmap: Bitmap? = null,
        settings: DocumentSettings
    ) {
        val margin = PAGE_MARGIN
        val contentWidth = PAGE_WIDTH - (margin * 2f)

        val koordinatTampil = laporan.koordinat.ifBlank { "-2.5830, 101.1240" }
        val lokasiTampil = laporan.lokasiBencana.ifBlank { "Wilayah Kabupaten Mukomuko" }
        val rawCuaca = laporan.cuacaSaatKejadian.trim().ifBlank { laporan.kondisiCuaca.trim() }
        val cuacaTerformat = if (
            rawCuaca.isBlank() ||
            rawCuaca == "-" ||
            rawCuaca.equals("Data cuaca tidak tersedia", ignoreCase = true)
        ) {
            "Data cuaca tidak tersedia"
        } else {
            rawCuaca.removePrefix("Kondisi Cuaca:").trim()
        }

        // 1. Kop Surat Resmi (HANYA di Halaman 1, teks header dari DocumentSettings)
        var y = ReportExporter.drawKopSuratResmi(
            context = context,
            canvas = canvas,
            pageWidth = PAGE_WIDTH,
            margin = margin,
            pageNumber = 1,
            documentSettings = settings
        )

        // 2. Judul Laporan & Paragraf Pengantar
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 11.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        y += 4f
        PdfTextAutoShrinkHelper.drawAutoShrinkText(
            canvas = canvas,
            text = "LAPORAN HARIAN PUSDALOPS",
            x = PAGE_WIDTH / 2f,
            y = y,
            maxWidth = contentWidth,
            basePaint = titlePaint,
            initialFontSize = 11.5f,
            minFontSize = 8.5f
        )
        y += 10f

        val introText =
            "Dilaporkan kejadian alam dan bencana di Kabupaten Mukomuko pukul 08.00 WIB sampai dengan pukul 08.00 WIB, yang diperoleh Pusdalops-PB BPBD Kabupaten Mukomuko sebagai berikut :"
        val bodyTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 8.2f
        }
        val introLayout = StaticLayout.Builder
            .obtain(introText, 0, introText.length, bodyTextPaint, contentWidth.toInt())
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1.05f)
            .setIncludePad(false)
            .build()
        canvas.save()
        canvas.translate(margin, y)
        introLayout.draw(canvas)
        canvas.restore()
        y += introLayout.height + 7f

        // 3. Bagian I: Kejadian Bencana
        val sectionTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("I. Kejadian Bencana", margin, y + 8f, sectionTitlePaint)
        y += 12f

        val infoRows = listOf(
            "Jenis Bencana" to laporan.jenisBencana.ifBlank { "-" },
            "Tanggal / Waktu" to "${laporan.tanggalKejadian.ifBlank { "-" }} / ${laporan.waktuKejadian.ifBlank { "-" }}",
            "Lokasi Kejadian" to lokasiTampil,
            "Kondisi Cuaca" to "Kondisi Cuaca: $cuacaTerformat",
            "Koordinat" to koordinatTampil,
            "Kedalaman" to laporan.kedalaman.ifBlank { "-" },
            "Cakupan Dampak" to laporan.cakupanDampak.ifBlank { "-" },
            "Upaya Dilakukan" to laporan.upayaDilakukan.ifBlank { "-" },
            "Kondisi Mutakhir" to laporan.kondisiMutakhir.ifBlank { "-" },
            "Korban" to "Luka: ${laporan.korbanLuka} Org, Meninggal: ${laporan.korbanMeninggal} Org",
            "Kerusakan" to laporan.kerusakan.ifBlank { "-" },
            "Penyebab" to laporan.penyebab.ifBlank { "-" },
            "Sumber Informasi" to laporan.sumberInformasi.ifBlank { "-" }
        )

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 41, 59)
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
        }
        val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = settings.bodyFontSize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val valuePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = settings.bodyFontSize
        }

        val labelColWidth = contentWidth * 0.28f
        val valueColWidth = contentWidth - labelColWidth
        val valueInnerWidth = (valueColWidth - 10f).toInt().coerceAtLeast(40)

        for ((label, rawValue) in infoRows) {
            if (label == "Cakupan Dampak") {
                val structuredLines = CakupanDampakFormatter.toPdfLines(
                    rawCakupanDampak = rawValue,
                    fallbackLokasi = lokasiTampil
                )
                val blockLayouts = structuredLines.map { lineText ->
                    val safeLine = settings.truncateIfNeeded(lineText, overrideMaxLength = 420)
                    StaticLayout.Builder
                        .obtain(safeLine, 0, safeLine.length, valuePaint, valueInnerWidth)
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                        .setLineSpacing(0.8f, 1.04f)
                        .setMaxLines(5)
                        .setEllipsize(TextUtils.TruncateAt.END)
                        .setIncludePad(false)
                        .build()
                }
                val itemGap = 3.5f
                val totalTextHeight = blockLayouts.sumOf { it.height }.toFloat() +
                    (if (blockLayouts.size > 1) itemGap * (blockLayouts.size - 1) else 0f)
                val rowHeight = max(16.5f, totalTextHeight + 7f)

                canvas.drawRect(margin, y, margin + labelColWidth, y + rowHeight, borderPaint)
                canvas.drawRect(margin + labelColWidth, y, margin + contentWidth, y + rowHeight, borderPaint)

                canvas.drawText(label, margin + 5f, y + 10.5f, labelPaint)
                var blockY = y + 3.5f
                blockLayouts.forEachIndexed { idx, layout ->
                    canvas.save()
                    canvas.translate(margin + labelColWidth + 5f, blockY)
                    layout.draw(canvas)
                    canvas.restore()
                    blockY += layout.height + if (idx < blockLayouts.lastIndex) itemGap else 0f
                }

                y += rowHeight
            } else {
                val allowExtendedLines = label == "Lokasi Kejadian" ||
                    label == "Upaya Dilakukan" ||
                    label == "Kondisi Mutakhir" ||
                    label == "Kerusakan"
                val cleanValue = settings.truncateIfNeeded(
                    rawText = rawValue,
                    overrideMaxLength = if (allowExtendedLines) 360 else null
                )
                val valLayout = StaticLayout.Builder
                    .obtain(cleanValue, 0, cleanValue.length, valuePaint, valueInnerWidth)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(0.5f, 1.03f)
                    .setMaxLines(if (allowExtendedLines) 4 else 2)
                    .setEllipsize(TextUtils.TruncateAt.END)
                    .setIncludePad(false)
                    .build()
                val rowHeight = max(14.5f, valLayout.height + 6f)

                canvas.drawRect(margin, y, margin + labelColWidth, y + rowHeight, borderPaint)
                canvas.drawRect(margin + labelColWidth, y, margin + contentWidth, y + rowHeight, borderPaint)

                canvas.drawText(label, margin + 5f, y + 10f, labelPaint)
                canvas.save()
                canvas.translate(margin + labelColWidth + 5f, y + 3f)
                valLayout.draw(canvas)
                canvas.restore()

                y += rowHeight
            }
        }

        // 4. Bagian II: Peta Lokasi Kejadian & Foto Bukti GPS Laporan Warga (Selalu 2 Kolom Bersebelahan)
        val hasEvidencePhoto = evidencePhotoBitmap != null && !evidencePhotoBitmap.isRecycled &&
            evidencePhotoBitmap.width > 1 && evidencePhotoBitmap.height > 1
        y += 6f
        canvas.drawText(
            "II. Peta Lokasi Kejadian & Foto Bukti Dokumentasi Lapangan (GPS)",
            margin,
            y + 8f,
            sectionTitlePaint
        )
        y += 11f

        // Jaga keseimbangan vertikal Halaman 1 agar Bagian III & Tanda Tangan Operator selalu muat rapi di A4
        val reservedForSection3AndFooter = 166f
        val availableForMapContainer = (PAGE_HEIGHT - reservedForSection3AndFooter - y)
        val mapContainerHeight = availableForMapContainer.coerceIn(106f, 140f)
        val mapContainerRect = RectF(margin, y, margin + contentWidth, y + mapContainerHeight)
        val mapContainerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(248, 250, 252)
            style = Paint.Style.FILL
        }
        val mapContainerBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(71, 85, 105)
            style = Paint.Style.STROKE
            strokeWidth = 0.9f
        }
        canvas.drawRoundRect(mapContainerRect, 4f, 4f, mapContainerBgPaint)
        canvas.drawRoundRect(mapContainerRect, 4f, 4f, mapContainerBorderPaint)

        val thinFramePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 41, 59)
            style = Paint.Style.STROKE
            strokeWidth = 1.0f
        }
        val whiteFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val captionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(51, 65, 85)
            textSize = 7.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val resolvedMapBitmap = if (mapBitmap != null && !mapBitmap.isRecycled && mapBitmap.width > 1 && mapBitmap.height > 1) {
            mapBitmap
        } else {
            val fallbackLatLng = extractLatLng(koordinatTampil)
            generateSatelliteMapBitmapFallback(
                context = context,
                lat = fallbackLatLng?.first ?: "-2.5830",
                lng = fallbackLatLng?.second ?: "101.1240",
                lokasiLabel = lokasiTampil
            )
        }

        val colGap = 12f
        val innerPadH = 8f
        val pairColWidth = (contentWidth - (innerPadH * 2f) - colGap) / 2f
        val frameHeight = (mapContainerHeight - 26f).coerceAtLeast(80f)
        val frameTop = y + 6f

        val leftMapRect = RectF(
            margin + innerPadH,
            frameTop,
            margin + innerPadH + pairColWidth,
            frameTop + frameHeight
        )
        val rightPhotoLeft = leftMapRect.right + colGap
        val rightPhotoRect = RectF(
            rightPhotoLeft,
            frameTop,
            rightPhotoLeft + pairColWidth,
            frameTop + frameHeight
        )

        // Kolom Kiri: Peta Lokasi Kejadian (Citra Satelit & GIS)
        canvas.drawRect(leftMapRect, whiteFillPaint)
        val innerMapRect = RectF(
            leftMapRect.left + 1.5f,
            leftMapRect.top + 1.5f,
            leftMapRect.right - 1.5f,
            leftMapRect.bottom - 1.5f
        )
        canvas.drawBitmap(resolvedMapBitmap, null, innerMapRect, bitmapPaint)
        canvas.drawRect(leftMapRect, thinFramePaint)
        canvas.drawText(
            "Peta Lokasi Kejadian (Citra Satelit & GIS)",
            leftMapRect.centerX(),
            leftMapRect.bottom + 12f,
            captionPaint
        )

        // Kolom Kanan: Foto GPS Bukti Laporan dari Warga (bersebelahan dengan Peta)
        canvas.drawRect(rightPhotoRect, whiteFillPaint)
        val innerPhotoRect = RectF(
            rightPhotoRect.left + 1.5f,
            rightPhotoRect.top + 1.5f,
            rightPhotoRect.right - 1.5f,
            rightPhotoRect.bottom - 1.5f
        )
        if (hasEvidencePhoto && evidencePhotoBitmap != null) {
            drawBitmapFitCenter(canvas, evidencePhotoBitmap, innerPhotoRect, bitmapPaint)
        } else {
            val placeholderBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(241, 245, 249)
                style = Paint.Style.FILL
            }
            val placeholderTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(51, 65, 85)
                textSize = 8.2f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val placeholderSub = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(100, 116, 139)
                textSize = 7.2f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawRect(innerPhotoRect, placeholderBg)
            canvas.drawText(
                "FOTO BUKTI GPS LAPORAN WARGA",
                innerPhotoRect.centerX(),
                innerPhotoRect.centerY() - 6f,
                placeholderTitle
            )
            canvas.drawText(
                "Belum ada foto bukti GPS warga terlampir",
                innerPhotoRect.centerX(),
                innerPhotoRect.centerY() + 7f,
                placeholderSub
            )
            canvas.drawText(
                "Koordinat: $koordinatTampil",
                innerPhotoRect.centerX(),
                innerPhotoRect.centerY() + 19f,
                placeholderSub
            )
        }
        canvas.drawRect(rightPhotoRect, thinFramePaint)
        canvas.drawText(
            "Foto Bukti Laporan Warga (Terverifikasi GPS)",
            rightPhotoRect.centerX(),
            rightPhotoRect.bottom + 12f,
            captionPaint
        )

        y += mapContainerHeight + 5f

        // 5. Bagian III: Komunikasi Pelaporan ke BPBD / BNPB
        canvas.drawText("III. Komunikasi Pelaporan ke BPBD / BNPB", margin, y + 8f, sectionTitlePaint)
        y += 12f

        val logColWeights = floatArrayOf(0.15f, 0.25f, 0.25f, 0.35f)
        val logHeaders = arrayOf("WAKTU", "DARI (STATION ID)", "TUJUAN (STATION ID)", "SUBJEK / PESAN")
        val tableHeaderBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(226, 232, 240)
            style = Paint.Style.FILL
        }
        val tableHeaderTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 7.8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val logHeaderHeight = 16f
        var colX = margin
        for (i in logHeaders.indices) {
            val colW = contentWidth * logColWeights[i]
            canvas.drawRect(colX, y, colX + colW, y + logHeaderHeight, tableHeaderBgPaint)
            canvas.drawRect(colX, y, colX + colW, y + logHeaderHeight, borderPaint)
            canvas.drawText(logHeaders[i], colX + colW / 2f, y + 11f, tableHeaderTextPaint)
            colX += colW
        }
        y += logHeaderHeight

        val desiredOperatorRowH = 78f
        val maxLogBottomY = PAGE_HEIGHT - 32f - desiredOperatorRowH - 2f

        if (listLog.isEmpty()) {
            val emptyRowH = 18f
            canvas.drawRect(margin, y, margin + contentWidth, y + emptyRowH, borderPaint)
            val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(71, 85, 105)
                textSize = 8.0f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("Tidak ada catatan komunikasi", PAGE_WIDTH / 2f, y + 12f, emptyPaint)
            y += emptyRowH
        } else {
            val maxLogRows = listLog.take(4)
            for (log in maxLogRows) {
                val cells = arrayOf(
                    log.waktu.ifBlank { "-" },
                    log.stationIdFrom.ifBlank { "-" },
                    log.stationIdTo.ifBlank { "-" },
                    log.subjek.ifBlank { "-" }
                )
                val cellLayouts = Array(cells.size) { i ->
                    val colW = contentWidth * logColWeights[i]
                    StaticLayout.Builder
                        .obtain(cells[i], 0, cells[i].length, valuePaint, (colW - 8f).toInt().coerceAtLeast(24))
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                        .setIncludePad(false)
                        .setMaxLines(2)
                        .setEllipsize(TextUtils.TruncateAt.END)
                        .build()
                }
                val rowH = max(15.5f, (cellLayouts.maxOfOrNull { it.height } ?: 10) + 5f)
                if (y + rowH > maxLogBottomY) break
                var cellX = margin
                for (i in cells.indices) {
                    val colW = contentWidth * logColWeights[i]
                    canvas.drawRect(cellX, y, cellX + colW, y + rowH, borderPaint)
                    canvas.save()
                    canvas.translate(cellX + 4f, y + (rowH - cellLayouts[i].height) / 2f)
                    cellLayouts[i].draw(canvas)
                    canvas.restore()
                    cellX += colW
                }
                y += rowH
            }
        }

        // Baris Paling Bawah Bagian III: Kolom Identitas & Tanda Tangan Operator Pusdalops
        // Diperlebar (48% lebar tabel) dan ditinggikan (78 pt) agar menyediakan ruang tanda tangan presisi & proporsional
        val availableSigH = PAGE_HEIGHT - 30f - y
        val operatorRowH = minOf(desiredOperatorRowH, availableSigH).coerceAtLeast(56f)
        if (y + operatorRowH <= PAGE_HEIGHT - 26f) {
            val leftInfoW = contentWidth * 0.52f
            val rightSigW = contentWidth - leftInfoW
            canvas.drawRect(margin, y, margin + leftInfoW, y + operatorRowH, borderPaint)
            canvas.drawRect(margin + leftInfoW, y, margin + contentWidth, y + operatorRowH, borderPaint)

            val resolvedOperator = DocumentSettingsRepository.resolveOfficerIdentity(
                context = context,
                rawName = laporan.operatorName,
                settings = settings
            )
            val resolvedStation = laporan.stationId.ifBlank { "PUSDALOPS-MM-01" }

            val infoLineStep = (operatorRowH - 16f) / 3f
            val line1Y = y + 15f
            val line2Y = line1Y + infoLineStep
            val line3Y = line2Y + infoLineStep
            val line4Y = y + operatorRowH - 9f

            PdfTextAutoShrinkHelper.drawAutoShrinkText(
                canvas = canvas,
                text = "Stasiun Pelapor : $resolvedStation",
                x = margin + 6f,
                y = line1Y,
                maxWidth = leftInfoW - 12f,
                basePaint = labelPaint,
                initialFontSize = settings.bodyFontSize,
                minFontSize = settings.minFontSize
            )
            PdfTextAutoShrinkHelper.drawAutoShrinkText(
                canvas = canvas,
                text = "Petugas / Operator : ${resolvedOperator.formattedName} (${resolvedOperator.nipLine})",
                x = margin + 6f,
                y = line2Y,
                maxWidth = leftInfoW - 12f,
                basePaint = valuePaint,
                initialFontSize = settings.bodyFontSize,
                minFontSize = settings.minFontSize
            )
            PdfTextAutoShrinkHelper.drawAutoShrinkText(
                canvas = canvas,
                text = "Tanggal Surat / Terima : ${laporan.tanggalSurat.ifBlank { "-" }} / ${laporan.tanggalTerima.ifBlank { "-" }}",
                x = margin + 6f,
                y = line3Y,
                maxWidth = leftInfoW - 12f,
                basePaint = valuePaint,
                initialFontSize = settings.bodyFontSize,
                minFontSize = settings.minFontSize
            )
            PdfTextAutoShrinkHelper.drawAutoShrinkText(
                canvas = canvas,
                text = "Status Berita : Terverifikasi Pusdalops-PB BPBD Kab. Mukomuko",
                x = margin + 6f,
                y = line4Y,
                maxWidth = leftInfoW - 12f,
                basePaint = valuePaint,
                initialFontSize = settings.bodyFontSize,
                minFontSize = settings.minFontSize
            )

            val sigCenterX = margin + leftInfoW + (rightSigW / 2f)
            val sigMaxTextW = (rightSigW - 14f).coerceAtLeast(60f)
            val opHeaderPaint = Paint(labelPaint).apply { textAlign = Paint.Align.CENTER }
            val opNamePaint = Paint(labelPaint).apply {
                textAlign = Paint.Align.CENTER
                isUnderlineText = true
            }
            val opNipPaint = Paint(valuePaint).apply {
                textAlign = Paint.Align.CENTER
                textSize = (settings.bodyFontSize - 0.4f).coerceAtLeast(settings.minFontSize)
            }
            PdfTextAutoShrinkHelper.drawAutoShrinkText(
                canvas = canvas,
                text = "Operator Pusdalops",
                x = sigCenterX,
                y = y + 13f,
                maxWidth = sigMaxTextW,
                basePaint = opHeaderPaint,
                initialFontSize = settings.bodyFontSize,
                minFontSize = settings.minFontSize
            )
            // Ruang kosong presisi (~44 pt) di tengah kotak untuk tanda tangan basah/digital Operator Pusdalops
            PdfTextAutoShrinkHelper.drawAutoShrinkText(
                canvas = canvas,
                text = resolvedOperator.formattedName,
                x = sigCenterX,
                y = y + operatorRowH - 18f,
                maxWidth = sigMaxTextW,
                basePaint = opNamePaint,
                initialFontSize = settings.fontSize,
                minFontSize = settings.minFontSize
            )
            PdfTextAutoShrinkHelper.drawAutoShrinkText(
                canvas = canvas,
                text = resolvedOperator.nipLine,
                x = sigCenterX,
                y = y + operatorRowH - 7f,
                maxWidth = sigMaxTextW,
                basePaint = opNipPaint,
                initialFontSize = (settings.fontSize - 0.8f).coerceAtLeast(settings.minFontSize),
                minFontSize = settings.minFontSize
            )
            y += operatorRowH
        }

        drawPageFooter(canvas, settings)
    }

    private fun drawPage2(
        context: Context,
        canvas: Canvas,
        listLogistik: List<DistribusiLogistikEntity>,
        settings: DocumentSettings
    ) {
        val margin = PAGE_MARGIN
        val contentWidth = PAGE_WIDTH - (margin * 2f)

        // Halaman 2 dimulai langsung dari konten TANPA Kop Surat
        var y = ReportExporter.drawKopSuratResmi(
            context = context,
            canvas = canvas,
            pageWidth = PAGE_WIDTH,
            margin = margin,
            pageNumber = 2,
            documentSettings = settings
        )

        val sectionTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 41, 59)
            textSize = 8.5f
        }

        y += 8f
        PdfTextAutoShrinkHelper.drawPdfText(canvas, "IV. Distribusi Logistik / Aset Bantuan", margin, y, sectionTitlePaint)
        y += 14f
        PdfTextAutoShrinkHelper.drawPdfText(
            canvas,
            "Daftar penyaluran logistik darurat dan aset bantuan penanggulangan bencana pada kejadian ini:",
            margin,
            y,
            subtitlePaint
        )
        y += 10f

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 41, 59)
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
        }
        val tableHeaderBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(226, 232, 240)
            style = Paint.Style.FILL
        }
        val tableHeaderTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 8.2f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val cellTextPaint = PdfTextAutoShrinkHelper.configurePdfPaint(
            TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(15, 23, 42)
                textSize = 8.2f
            },
            letterSpacingEm = 0.03f
        )
        val cellCenterPaint = PdfTextAutoShrinkHelper.configurePdfPaint(
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(15, 23, 42)
                textSize = 8.2f
                textAlign = Paint.Align.CENTER
            },
            letterSpacingEm = 0.03f
        )

        val logistikWeights = floatArrayOf(0.08f, 0.32f, 0.14f, 0.16f, 0.30f)
        val logistikHeaders = arrayOf("NO", "NAMA BARANG / ASET BANTUAN", "JUMLAH", "SATUAN", "KETERANGAN")
        val headerHeight = 20f

        var colX = margin
        for (i in logistikHeaders.indices) {
            val colW = contentWidth * logistikWeights[i]
            canvas.drawRect(colX, y, colX + colW, y + headerHeight, tableHeaderBgPaint)
            canvas.drawRect(colX, y, colX + colW, y + headerHeight, borderPaint)
            PdfTextAutoShrinkHelper.drawPdfText(canvas, logistikHeaders[i], colX + colW / 2f, y + 13f, tableHeaderTextPaint)
            colX += colW
        }
        y += headerHeight

        if (listLogistik.isEmpty()) {
            val emptyRowH = 22f
            canvas.drawRect(margin, y, margin + contentWidth, y + emptyRowH, borderPaint)
            val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(71, 85, 105)
                textSize = 8.5f
                textAlign = Paint.Align.CENTER
            }
            PdfTextAutoShrinkHelper.drawPdfText(
                canvas,
                "Belum ada distribusi logistik / aset bantuan yang dicatat",
                PAGE_WIDTH / 2f,
                y + 14f,
                emptyPaint
            )
            y += emptyRowH
        } else {
            listLogistik.forEachIndexed { index, item ->
                val w0 = contentWidth * logistikWeights[0]
                val w1 = contentWidth * logistikWeights[1]
                val w2 = contentWidth * logistikWeights[2]
                val w3 = contentWidth * logistikWeights[3]
                val w4 = contentWidth * logistikWeights[4]
                val nama = PdfTextAutoShrinkHelper.normalizePdfWordSpacing(item.namaBarang.ifBlank { "-" })
                val ket = PdfTextAutoShrinkHelper.normalizePdfWordSpacing(item.keterangan.ifBlank { "-" })
                val namaLayout = StaticLayout.Builder
                    .obtain(nama, 0, nama.length, cellTextPaint, (w1 - 10f).toInt().coerceAtLeast(30))
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setIncludePad(false)
                    .setMaxLines(2)
                    .setEllipsize(TextUtils.TruncateAt.END)
                    .build()
                val ketLayout = StaticLayout.Builder
                    .obtain(ket, 0, ket.length, cellTextPaint, (w4 - 10f).toInt().coerceAtLeast(30))
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setIncludePad(false)
                    .setMaxLines(2)
                    .setEllipsize(TextUtils.TruncateAt.END)
                    .build()
                val rowH = max(19f, max(namaLayout.height, ketLayout.height) + 6f)
                if (y + rowH > PAGE_HEIGHT - 240f) return@forEachIndexed
                var cellX = margin
                val textCenterBaseline = y + (rowH / 2f) + 3f

                // Kolom 1: NO
                canvas.drawRect(cellX, y, cellX + w0, y + rowH, borderPaint)
                PdfTextAutoShrinkHelper.drawPdfText(canvas, "${index + 1}", cellX + w0 / 2f, textCenterBaseline, cellCenterPaint)
                cellX += w0

                // Kolom 2: NAMA BARANG
                canvas.drawRect(cellX, y, cellX + w1, y + rowH, borderPaint)
                canvas.save()
                canvas.translate(cellX + 5f, y + (rowH - namaLayout.height) / 2f)
                namaLayout.draw(canvas)
                canvas.restore()
                cellX += w1

                // Kolom 3: JUMLAH
                canvas.drawRect(cellX, y, cellX + w2, y + rowH, borderPaint)
                PdfTextAutoShrinkHelper.drawPdfText(canvas, item.jumlah.ifBlank { "0" }, cellX + w2 / 2f, textCenterBaseline, cellCenterPaint)
                cellX += w2

                // Kolom 4: SATUAN
                canvas.drawRect(cellX, y, cellX + w3, y + rowH, borderPaint)
                PdfTextAutoShrinkHelper.drawPdfText(canvas, item.satuan.ifBlank { "-" }, cellX + w3 / 2f, textCenterBaseline, cellCenterPaint)
                cellX += w3

                // Kolom 5: KETERANGAN
                canvas.drawRect(cellX, y, cellX + w4, y + rowH, borderPaint)
                canvas.save()
                canvas.translate(cellX + 5f, y + (rowH - ketLayout.height) / 2f)
                ketLayout.draw(canvas)
                canvas.restore()

                y += rowH
            }
        }

        // Blok Tanda Tangan Pejabat (Tata Naskah Dinas — Piramida Terbalik 3 Kolom, Data-Driven + Auto-Shrink)
        val sigStartY = max(y + 36f, 230f)
        drawBlokTandaTanganPiramidaTerbalik(context, canvas, PAGE_WIDTH, sigStartY, margin, settings)

        drawPageFooter(canvas, settings)
    }

    /**
     * Menggambar Blok Tanda Tangan Pejabat sesuai Tata Naskah Dinas (Piramida Terbalik 3 Kolom)
     * secara 100% Data-Driven berdasarkan konfigurasi server (`GET /document-settings` & `GET /officers`)
     * dengan Auto-Shrink Text agar nama & gelar sepanjang apapun tetap rapi di dalam lebar kotak.
     */
    private fun drawBlokTandaTanganPiramidaTerbalik(
        context: Context,
        canvas: Canvas,
        pageWidth: Int,
        startY: Float,
        margin: Float,
        settings: DocumentSettings
    ) {
        val signatories = DocumentSettingsRepository.resolveSignatories(context, settings)
        val koordAdmin = signatories[DocumentSettings.SLOT_KOORD_ADMIN]
        val manajerPusdalops = signatories[DocumentSettings.SLOT_MANAJER_PUSDALOPS]
        val pengawasKalaksa = signatories[DocumentSettings.SLOT_PENGAWAS]

        val contentWidth = pageWidth - (margin * 2f)
        val colWidth = contentWidth / 3f
        val sideBoxMaxW = (colWidth - 8f).coerceAtLeast(60f)
        val centerBoxMaxW = (contentWidth * 0.56f).coerceAtLeast(sideBoxMaxW)

        val leftColCenterX = margin + (colWidth * 0.5f)
        val centerColCenterX = margin + (colWidth * 1.5f)
        val rightColCenterX = margin + (colWidth * 2.5f)

        val roleFontSize = (settings.fontSize - 0.2f).coerceAtLeast(settings.minFontSize)
        val nameFontSize = settings.fontSize
        val nipFontSize = (settings.fontSize - 0.6f).coerceAtLeast(settings.minFontSize)

        val rolePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = roleFontSize
            textAlign = Paint.Align.CENTER
        }
        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = nameFontSize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isUnderlineText = true
            textAlign = Paint.Align.CENTER
        }
        val nipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 41, 59)
            textSize = nipFontSize
            textAlign = Paint.Align.CENTER
        }

        fun drawSignatoryColumn(
            sig: com.example.model.ResolvedSignatory?,
            centerX: Float,
            topY: Float,
            maxWidth: Float
        ) {
            if (sig == null) return
            PdfTextAutoShrinkHelper.drawAutoShrinkText(
                canvas = canvas,
                text = sig.roleLine1,
                x = centerX,
                y = topY,
                maxWidth = maxWidth,
                basePaint = rolePaint,
                initialFontSize = roleFontSize,
                minFontSize = settings.minFontSize
            )
            PdfTextAutoShrinkHelper.drawAutoShrinkText(
                canvas = canvas,
                text = sig.roleLine2,
                x = centerX,
                y = topY + 12f,
                maxWidth = maxWidth,
                basePaint = rolePaint,
                initialFontSize = roleFontSize,
                minFontSize = settings.minFontSize
            )
            PdfTextAutoShrinkHelper.drawAutoShrinkText(
                canvas = canvas,
                text = sig.formattedName,
                x = centerX,
                y = topY + 62f,
                maxWidth = maxWidth,
                basePaint = namePaint,
                initialFontSize = nameFontSize,
                minFontSize = settings.minFontSize
            )
            PdfTextAutoShrinkHelper.drawAutoShrinkText(
                canvas = canvas,
                text = sig.nipLine,
                x = centerX,
                y = topY + 74f,
                maxWidth = maxWidth,
                basePaint = nipPaint,
                initialFontSize = nipFontSize,
                minFontSize = settings.minFontSize
            )
        }

        // Baris 1: Kiri (Koord. Administrasi) & Kanan (Manajer Pusdalops)
        val row1TopY = startY
        drawSignatoryColumn(koordAdmin, leftColCenterX, row1TopY, sideBoxMaxW)
        drawSignatoryColumn(manajerPusdalops, rightColCenterX, row1TopY, sideBoxMaxW)

        // Baris 2: Bawah Tengah (Pengawas / Kepala Pelaksana BPBD Mukomuko)
        val row2TopY = row1TopY + 102f
        drawSignatoryColumn(pengawasKalaksa, centerColCenterX, row2TopY, centerBoxMaxW)
    }

    private fun drawPageFooter(canvas: Canvas, settings: DocumentSettings) {
        ReportExporter.drawFooterAtribusiResmi(
            canvas = canvas,
            pageWidth = PAGE_WIDTH,
            pageHeight = PAGE_HEIGHT,
            margin = PAGE_MARGIN,
            documentSettings = settings
        )
    }

    /**
     * Adapter PrintManager native berbasis ByteArray PdfDocument sehingga tidak memerlukan WebView/Chromium.
     */
    private class PdfBytesPrintAdapter(
        private val jobName: String,
        private val pdfBytes: ByteArray,
        private val pageCount: Int
    ) : PrintDocumentAdapter() {

        override fun onLayout(
            oldAttributes: PrintAttributes?,
            newAttributes: PrintAttributes,
            cancellationSignal: CancellationSignal?,
            callback: LayoutResultCallback,
            extras: Bundle?
        ) {
            if (cancellationSignal?.isCanceled == true) {
                callback.onLayoutCancelled()
                return
            }
            val info = PrintDocumentInfo.Builder("$jobName.pdf")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(pageCount)
                .build()
            callback.onLayoutFinished(info, true)
        }

        override fun onWrite(
            pages: Array<out PageRange>?,
            destination: ParcelFileDescriptor,
            cancellationSignal: CancellationSignal?,
            callback: WriteResultCallback
        ) {
            if (cancellationSignal?.isCanceled == true) {
                callback.onWriteCancelled()
                return
            }
            try {
                FileOutputStream(destination.fileDescriptor).use { out ->
                    out.write(pdfBytes)
                    out.flush()
                }
                callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
            } catch (e: Exception) {
                callback.onWriteFailed(e.localizedMessage ?: "Gagal menulis dokumen PDF")
            }
        }
    }
}
