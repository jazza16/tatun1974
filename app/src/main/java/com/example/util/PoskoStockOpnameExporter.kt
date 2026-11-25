package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
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
import com.example.data.repository.NikAidDistributionRecord
import com.example.data.repository.PoskoLogisticCommodity
import com.example.data.repository.PoskoLogisticsRepository
import com.example.data.repository.PoskoRefugeeUnit
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.max

/**
 * Generator & Eksportir Resmi Berita Acara Stock Opname Logistik, Aset Kebencanaan & Pertanggungjawaban
 * Tata Kelola Barang Milik Negara/Daerah (BMN/BMD) serta Rekapitulasi Pengungsi & Distribusi Bantuan NIK 16 Digit
 * Pusdalops-PB BPBD Kabupaten Mukomuko.
 *
 * Mendukung 2 Format Resmi:
 * 1. Dokumen PDF Resmi (A4, Kop Surat Pemerintah Kabupaten Mukomuko / BPBD, Tabel Stock Opname FEFO & Status Kritis,
 *    Rekap Pengungsi Terpilah, Audit Distribusi Bantuan NIK 16 Digit, dan Blok Tanda Tangan Pertanggungjawaban BMN/BMD).
 * 2. Berkas CSV / Excel (UTF-8 BOM) untuk rekonsiliasi neraca persediaan aset BMN/BMD & inspektorat.
 */
object PoskoStockOpnameExporter {

    private const val A4_WIDTH = 595
    private const val A4_HEIGHT = 842
    private const val MARGIN = 32f

    val OFFICIAL_REPORT_PURPOSES = listOf(
        "Berita Acara Stock Opname Logistik & Tertib Administrasi BMN/BMD",
        "Laporan Pertanggungjawaban Mutasi Persediaan & Distribusi Bantuan (NIK 16 Digit)",
        "Rekonsiliasi Aset Kebencanaan & Audit Inspektorat / BPKP / BNPB"
    )

    private fun getExportDir(context: Context): File {
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

    private fun saveToPublicDownloads(
        context: Context,
        sourceFile: File,
        downloadFileName: String,
        mimeType: String
    ): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, downloadFileName)
                    put(MediaStore.Downloads.MIME_TYPE, mimeType)
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/SIPAGAR_KITA")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { out ->
                        sourceFile.inputStream().use { input -> input.copyTo(out) }
                    }
                    values.clear()
                    values.put(MediaStore.Downloads.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                    true
                } else {
                    false
                }
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Mengekspor langsung dari [PoskoLogisticsRepository] ke dokumen PDF Resmi Berita Acara Stock Opname BMN/BMD.
     */
    fun exportStockOpnameToPdf(
        context: Context,
        officerName: String,
        officerNip: String,
        officerRole: String,
        documentPurpose: String = OFFICIAL_REPORT_PURPOSES.first()
    ): File {
        val repo = PoskoLogisticsRepository.getInstance(context)
        val stocks = repo.logisticStocks.value
        val poskoUnits = repo.poskoUnits.value
        val ledger = repo.distributionLedger.value

        val idLocale = Locale.forLanguageTag("id-ID")
        val timestampTag = SimpleDateFormat("yyyyMMdd_HHmm", idLocale).apply {
            timeZone = TimeZone.getTimeZone("Asia/Jakarta")
        }.format(Date())

        val outFile = File(
            getExportDir(context),
            "BA_Stock_Opname_BMN_Logistik_BPBD_Mukomuko_$timestampTag.pdf"
        )

        val pdfBytes = buildStockOpnamePdfBytes(
            context = context,
            stocks = stocks,
            poskoUnits = poskoUnits,
            distributionLedger = ledger,
            officerName = officerName,
            officerNip = officerNip,
            officerRole = officerRole,
            documentPurpose = documentPurpose
        )

        FileOutputStream(outFile).use { fos ->
            fos.write(pdfBytes)
            fos.flush()
        }

        val downloadName = "BA_Stock_Opname_BMN_Logistik_BPBD_Mukomuko_$timestampTag.pdf"
        val savedToDownloads = saveToPublicDownloads(
            context = context,
            sourceFile = outFile,
            downloadFileName = downloadName,
            mimeType = "application/pdf"
        )

        runCatching {
            Toast.makeText(
                context,
                if (savedToDownloads) {
                    "PDF Berita Acara Stock Opname BMN/BMD tersimpan di Downloads/SIPAGAR_KITA/$downloadName"
                } else {
                    "PDF Berita Acara Stock Opname BMN/BMD siap diunduh & dibagikan"
                },
                Toast.LENGTH_LONG
            ).show()
        }

        runCatching {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(
                    Intent.EXTRA_SUBJECT,
                    "Berita Acara Stock Opname Logistik & BMN/BMD BPBD Kab. Mukomuko (${stocks.size} Komoditas)"
                )
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Terlampir Dokumen Resmi Berita Acara Stock Opname Logistik, Aset Kebencanaan & Pertanggungjawaban Tata Kelola Barang Milik Negara/Daerah (BMN/BMD) BPBD Kabupaten Mukomuko untuk keperluan: $documentPurpose."
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(shareIntent, "Unduh / Bagikan PDF Stock Opname BMN/BMD").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        }

        return outFile
    }

    /**
     * Mengekspor langsung dari [PoskoLogisticsRepository] ke berkas CSV (Excel) Stock Opname & Mutasi BMN/BMD.
     */
    fun exportStockOpnameToCsv(
        context: Context,
        officerName: String,
        officerNip: String,
        officerRole: String,
        documentPurpose: String = OFFICIAL_REPORT_PURPOSES.first()
    ): File {
        val repo = PoskoLogisticsRepository.getInstance(context)
        val stocks = repo.logisticStocks.value
        val poskoUnits = repo.poskoUnits.value
        val ledger = repo.distributionLedger.value

        val idLocale = Locale.forLanguageTag("id-ID")
        val timestampTag = SimpleDateFormat("yyyyMMdd_HHmm", idLocale).apply {
            timeZone = TimeZone.getTimeZone("Asia/Jakarta")
        }.format(Date())

        val outFile = File(
            getExportDir(context),
            "Rekap_Stock_Opname_BMN_Logistik_BPBD_Mukomuko_$timestampTag.csv"
        )

        val csvContent = buildStockOpnameCsvString(
            stocks = stocks,
            poskoUnits = poskoUnits,
            distributionLedger = ledger,
            officerName = officerName,
            officerNip = officerNip,
            officerRole = officerRole,
            documentPurpose = documentPurpose
        )

        FileOutputStream(outFile).use { fos ->
            // UTF-8 BOM agar Microsoft Excel langsung mengenali karakter dan pemisah kolom dengan rapi
            fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
            fos.write(csvContent.toByteArray(Charsets.UTF_8))
            fos.flush()
        }

        val downloadName = "Rekap_Stock_Opname_BMN_Logistik_BPBD_Mukomuko_$timestampTag.csv"
        val savedToDownloads = saveToPublicDownloads(
            context = context,
            sourceFile = outFile,
            downloadFileName = downloadName,
            mimeType = "text/csv"
        )

        runCatching {
            Toast.makeText(
                context,
                if (savedToDownloads) {
                    "CSV/Excel Stock Opname BMN/BMD tersimpan di Downloads/SIPAGAR_KITA/$downloadName"
                } else {
                    "CSV/Excel Stock Opname BMN/BMD siap diunduh & dibagikan"
                },
                Toast.LENGTH_LONG
            ).show()
        }

        runCatching {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(
                    Intent.EXTRA_SUBJECT,
                    "CSV Stock Opname Logistik & BMN/BMD BPBD Kab. Mukomuko (${stocks.size} Komoditas)"
                )
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Terlampir Kertas Kerja CSV/Excel Stock Opname Logistik & Pertanggungjawaban Tata Kelola Barang Milik Negara/Daerah (BMN/BMD) BPBD Kabupaten Mukomuko untuk keperluan: $documentPurpose."
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(shareIntent, "Unduh / Bagikan CSV Stock Opname BMN/BMD").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        }

        return outFile
    }

    /**
     * Menghasilkan string CSV untuk Stock Opname BMN/BMD, Rekap Pengungsi Posko, dan Ledger Distribusi NIK.
     * Dapat diuji langsung di JVM Unit Test tanpa ketergantungan Android UI.
     */
    fun buildStockOpnameCsvString(
        stocks: List<PoskoLogisticCommodity>,
        poskoUnits: List<PoskoRefugeeUnit>,
        distributionLedger: List<NikAidDistributionRecord>,
        officerName: String,
        officerNip: String,
        officerRole: String,
        documentPurpose: String
    ): String {
        val idLocale = Locale.forLanguageTag("id-ID")
        val printedAt = SimpleDateFormat("dd MMMM yyyy HH:mm 'WIB'", idLocale).apply {
            timeZone = TimeZone.getTimeZone("Asia/Jakarta")
        }.format(Date())

        val sb = StringBuilder()
        sb.appendLine("PEMERINTAH KABUPATEN MUKOMUKO - BADAN PENANGGULANGAN BENCANA DAERAH (BPBD)")
        sb.appendLine("BERITA ACARA STOCK OPNAME PERSEDIAAN LOGISTIK & PERTANGGUNGJAWABAN TATA KELOLA BMN/BMD")
        sb.appendLine("Tujuan Dokumen,${sanitizeCsvCell(documentPurpose)}")
        sb.appendLine("Waktu Cetak Stock Opname,${sanitizeCsvCell(printedAt)}")
        sb.appendLine("Petugas Penanggung Jawab,${sanitizeCsvCell(officerName.ifBlank { "Petugas Posko / Logistik" })}")
        sb.appendLine("NIP / ID Registrasi,${sanitizeCsvCell(officerNip.ifBlank { "-" })}")
        sb.appendLine("Jabatan / Role RBAC,${sanitizeCsvCell(officerRole.ifBlank { "Petugas Posko" })}")
        sb.appendLine()

        // BAGIAN I: TABEL STOCK OPNAME LOGISTIK & ASET PERSEDIAAN BMN/BMD (FEFO & STATUS KRITIS)
        sb.appendLine("BAGIAN I: DAFTAR INVENTARIS & STOCK OPNAME PERSEDIAAN LOGISTIK DARURAT (BMN/BMD)")
        sb.appendLine("No,Kode Barang / BMN,Nama Komoditas Logistik,Kategori Persediaan,Lokasi Posko / Gudang,Saldo Fisik (Stok),Satuan,Ambang Kritis,Status Ketersediaan,Tgl Kedaluwarsa (FEFO),Status Kelayakan FEFO,Terakhir Diperbarui")
        stocks.forEachIndexed { idx, item ->
            val stockStatus = if (item.isCritical) "KRITIS (< 10) - WAJIB RESTOCK" else "AMAN (>= 10)"
            val fefoStatus = when {
                item.expiryDateIso.isNullOrBlank() -> "NON-KEDALUWARSA / ASET TETAP"
                item.isExpired -> "KEDALUWARSA - TARIK DARI DISTRIBUSI"
                item.isExpiringSoonFefo -> "PRIORITAS KELUAR FEFO (<= 30 HARI)"
                else -> "LAYAK SALUR (FEFO AMAN)"
            }
            sb.appendLine(
                listOf(
                    (idx + 1).toString(),
                    sanitizeCsvCell(item.code),
                    sanitizeCsvCell(item.name),
                    sanitizeCsvCell(item.category),
                    sanitizeCsvCell(item.poskoCode),
                    item.currentStock.toString(),
                    sanitizeCsvCell(item.unit),
                    item.minThreshold.toString(),
                    sanitizeCsvCell(stockStatus),
                    sanitizeCsvCell(item.expiryDateIso ?: "-"),
                    sanitizeCsvCell(fefoStatus),
                    sanitizeCsvCell(item.updatedAtIso)
                ).joinToString(",")
            )
        }
        sb.appendLine()

        // BAGIAN II: REKAPITULASI PENGUNGSI TERPILAH PER POSKO AKTIF
        sb.appendLine("BAGIAN II: REKAPITULASI DATA PENGUNGSI TERPILAH PER POSKO AKTIF")
        sb.appendLine("No,Kode Posko,Nama Posko Pengungsian,Kecamatan,ID Kejadian (Incident ID),Total Jiwa,Kapasitas Maks,Lansia (>=60 Th),Ibu Hamil,Balita (0-5 Th),Disabilitas,Total Kelompok Rentan,Petugas Verifikator")
        poskoUnits.forEachIndexed { idx, posko ->
            sb.appendLine(
                listOf(
                    (idx + 1).toString(),
                    sanitizeCsvCell(posko.poskoCode),
                    sanitizeCsvCell(posko.poskoName),
                    sanitizeCsvCell(posko.district),
                    sanitizeCsvCell(posko.incidentId),
                    posko.totalJiwa.toString(),
                    posko.maxCapacity.toString(),
                    posko.lansia.toString(),
                    posko.ibuHamil.toString(),
                    posko.balita.toString(),
                    posko.disabilitas.toString(),
                    posko.totalKelompokRentan.toString(),
                    sanitizeCsvCell(posko.updatedByOfficer)
                ).joinToString(",")
            )
        }
        sb.appendLine()

        // BAGIAN III: BUKU BESAR (LEDGER) MUTASI KELUAR & DISTRIBUSI BANTUAN BERBASIS NIK 16 DIGIT
        sb.appendLine("BAGIAN III: BUKU BESAR PERTANGGUNGJAWABAN DISTRIBUSI BANTUAN WARGA (TERVERIFIKASI NIK 16 DIGIT)")
        sb.appendLine("No,ID Transaksi,NIK Penerima (16 Digit PK),Nama Penerima Bantuan,Kelompok Rentan,Kode Barang,Nama Komoditas,Jumlah Disalurkan,Satuan,Posko Penyalur,Petugas Penyalur,Waktu Penyerahan")
        distributionLedger.forEachIndexed { idx, log ->
            sb.appendLine(
                listOf(
                    (idx + 1).toString(),
                    sanitizeCsvCell(log.distributionId),
                    sanitizeCsvCell("'${log.recipientNik}"),
                    sanitizeCsvCell(log.recipientName),
                    sanitizeCsvCell(log.vulnerabilityGroup),
                    sanitizeCsvCell(log.commodityCode),
                    sanitizeCsvCell(log.commodityName),
                    log.quantity.toString(),
                    sanitizeCsvCell(log.unit),
                    sanitizeCsvCell(log.poskoCode),
                    sanitizeCsvCell(log.distributedByOfficer),
                    sanitizeCsvCell(log.timestampIso)
                ).joinToString(",")
            )
        }

        return sb.toString()
    }

    private fun buildStockOpnamePdfBytes(
        context: Context,
        stocks: List<PoskoLogisticCommodity>,
        poskoUnits: List<PoskoRefugeeUnit>,
        distributionLedger: List<NikAidDistributionRecord>,
        officerName: String,
        officerNip: String,
        officerRole: String,
        documentPurpose: String
    ): ByteArray {
        val docSettings = runCatching {
            DocumentSettingsRepository.getCachedOrDefaultSettings(context)
        }.getOrNull()
        val resolvedSignatories = runCatching {
            if (docSettings != null) {
                DocumentSettingsRepository.resolveSignatories(context, docSettings)
            } else {
                emptyMap()
            }
        }.getOrDefault(emptyMap())

        val idLocale = Locale.forLanguageTag("id-ID")
        val now = Date()
        val fullDateStr = SimpleDateFormat("EEEE, dd MMMM yyyy • HH:mm 'WIB'", idLocale).apply {
            timeZone = TimeZone.getTimeZone("Asia/Jakarta")
        }.format(now)
        val docNumberDate = SimpleDateFormat("yyyy/MM/dd", idLocale).format(now)

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(A4_WIDTH, A4_HEIGHT, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 11.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val subHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 41, 59)
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        val sectionTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 41, 59)
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val boldBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val tableHeaderBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            style = Paint.Style.FILL
        }
        val tableHeaderTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 7.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(203, 213, 225)
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
        }
        val criticalRowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(254, 242, 242)
            style = Paint.Style.FILL
        }
        val zebraRowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(248, 250, 252)
            style = Paint.Style.FILL
        }

        var y = MARGIN

        // 1. KOP SURAT RESMI PEMERINTAH KABUPATEN MUKOMUKO - BPBD
        val centerX = A4_WIDTH / 2f
        canvas.drawText("PEMERINTAH KABUPATEN MUKOMUKO", centerX, y + 10f, titlePaint)
        titlePaint.textSize = 13f
        canvas.drawText("BADAN PENANGGULANGAN BENCANA DAERAH (BPBD)", centerX, y + 25f, titlePaint)
        canvas.drawText(
            "PUSAT PENGENDALIAN OPERASI PENANGGULANGAN BENCANA (PUSDALOPS-PB)",
            centerX,
            y + 37f,
            subHeaderPaint
        )
        val addressLine = "Jl. Imam Bonjol, Komplek Perkantoran Pemda Kabupaten Mukomuko, Provinsi Bengkulu"
        canvas.drawText(addressLine, centerX, y + 48f, subHeaderPaint)

        y += 56f
        val doubleLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            strokeWidth = 1.8f
        }
        canvas.drawLine(MARGIN, y, A4_WIDTH - MARGIN, y, doubleLinePaint)
        doubleLinePaint.strokeWidth = 0.7f
        canvas.drawLine(MARGIN, y + 3f, A4_WIDTH - MARGIN, y + 3f, doubleLinePaint)
        y += 18f

        // 2. JUDUL BERITA ACARA STOCK OPNAME BMN/BMD
        titlePaint.textSize = 10.5f
        canvas.drawText(
            "BERITA ACARA STOCK OPNAME LOGISTIK & TATA KELOLA BARANG MILIK NEGARA / DAERAH (BMN/BMD)",
            centerX,
            y,
            titlePaint
        )
        y += 12f
        canvas.drawText(
            "Nomor Dokumen: BA-SO/BPBD-MM/$docNumberDate • Dicetak: $fullDateStr",
            centerX,
            y,
            subHeaderPaint
        )
        y += 16f

        // 3. KOTAK RINGKASAN METADATA PERTANGGUNGJAWABAN PETUGAS
        val criticalCount = stocks.count { it.isCritical }
        val fefoWarnCount = stocks.count { it.isExpired || it.isExpiringSoonFefo }
        val totalRefugees = poskoUnits.sumOf { it.totalJiwa }
        val totalVulnerable = poskoUnits.sumOf { it.totalKelompokRentan }

        val metaRect = RectF(MARGIN, y, A4_WIDTH - MARGIN, y + 54f)
        canvas.drawRoundRect(metaRect, 6f, 6f, zebraRowPaint)
        canvas.drawRoundRect(metaRect, 6f, 6f, borderPaint)

        val safeOfficer = officerName.ifBlank { "Petugas Posko & Logistik" }
        val safeNip = officerNip.ifBlank { "-" }
        val safeRole = officerRole.ifBlank { "Petugas Posko" }

        canvas.drawText("Tujuan Pelaporan: $documentPurpose", MARGIN + 10f, y + 14f, boldBodyPaint)
        canvas.drawText(
            "Pejabat / Petugas Pelapor: $safeOfficer (NIP/ID: $safeNip • Role: $safeRole)",
            MARGIN + 10f,
            y + 27f,
            bodyPaint
        )
        canvas.drawText(
            "Ringkasan Aset & Posko: ${stocks.size} Komoditas Logistik ($criticalCount Status Kritis <10, $fefoWarnCount Peringatan FEFO) • ${poskoUnits.size} Posko Aktif ($totalRefugees Jiwa / $totalVulnerable Rentan)",
            MARGIN + 10f,
            y + 40f,
            bodyPaint
        )
        canvas.drawText(
            "Standar Kepatuhan: PP No. 27/2014 & Permendagri No. 19/2016 tentang Pedoman Pengelolaan Barang Milik Negara/Daerah (BMN/BMD) & Perka BNPB",
            MARGIN + 10f,
            y + 50f,
            bodyPaint
        )
        y += 68f

        // 4. TABEL I: STOCK OPNAME PERSEDIAAN LOGISTIK & FEFO
        canvas.drawText(
            "I. TABEL PEMERIKSAAN FISIK (STOCK OPNAME) PERSEDIAAN LOGISTIK & STATUS FEFO (BMN/BMD)",
            MARGIN,
            y,
            sectionTitlePaint
        )
        y += 6f

        val colWidths1 = floatArrayOf(24f, 68f, 155f, 62f, 58f, 82f, 82f)
        val headers1 = arrayOf("No", "Kode BMN", "Nama Komoditas Logistik", "Posko", "Saldo Fisik", "Tgl Exp (FEFO)", "Status Audit")
        val rowHeight1 = 18f

        // Draw Table 1 Header
        canvas.drawRect(MARGIN, y, A4_WIDTH - MARGIN, y + rowHeight1, tableHeaderBgPaint)
        var xCursor = MARGIN
        headers1.forEachIndexed { i, h ->
            canvas.drawText(h, xCursor + 4f, y + 12f, tableHeaderTextPaint)
            xCursor += colWidths1[i]
        }
        y += rowHeight1

        stocks.take(12).forEachIndexed { idx, item ->
            val rowTop = y
            val rowBottom = y + rowHeight1
            if (item.isCritical) {
                canvas.drawRect(MARGIN, rowTop, A4_WIDTH - MARGIN, rowBottom, criticalRowPaint)
            } else if (idx % 2 == 1) {
                canvas.drawRect(MARGIN, rowTop, A4_WIDTH - MARGIN, rowBottom, zebraRowPaint)
            }
            canvas.drawRect(MARGIN, rowTop, A4_WIDTH - MARGIN, rowBottom, borderPaint)

            val auditLabel = when {
                item.isExpired -> "EXP / TARIK"
                item.isCritical -> "KRITIS (<10)"
                item.isExpiringSoonFefo -> "FEFO <=30Hr"
                else -> "AMAN / LAYAK"
            }

            val cells = arrayOf(
                (idx + 1).toString(),
                item.code,
                item.name.take(28),
                item.poskoCode,
                "${item.currentStock} ${item.unit}",
                item.expiryDateIso ?: "Non-Exp",
                auditLabel
            )

            var cellX = MARGIN
            cells.forEachIndexed { cIdx, text ->
                val p = if (cIdx == 4 || cIdx == 6) boldBodyPaint else bodyPaint
                canvas.drawText(text, cellX + 4f, rowTop + 12f, p)
                cellX += colWidths1[cIdx]
            }
            y += rowHeight1
        }
        y += 16f

        // 5. TABEL II: REKAPITULASI PENGUNGSI POSKO AKTIF
        canvas.drawText(
            "II. REKAPITULASI DATA PENGUNGSI TERPILAH PER POSKO AKTIF (DASAR DISTRIBUSI LOGISTIK)",
            MARGIN,
            y,
            sectionTitlePaint
        )
        y += 6f

        val colWidths2 = floatArrayOf(75f, 165f, 70f, 55f, 55f, 55f, 56f)
        val headers2 = arrayOf("Kode Posko", "Nama Posko Pengungsian", "Total Jiwa", "Lansia", "Ibu Hamil", "Balita", "Disabilitas")

        canvas.drawRect(MARGIN, y, A4_WIDTH - MARGIN, y + rowHeight1, tableHeaderBgPaint)
        xCursor = MARGIN
        headers2.forEachIndexed { i, h ->
            canvas.drawText(h, xCursor + 4f, y + 12f, tableHeaderTextPaint)
            xCursor += colWidths2[i]
        }
        y += rowHeight1

        poskoUnits.take(6).forEachIndexed { idx, posko ->
            val rowTop = y
            val rowBottom = y + rowHeight1
            if (idx % 2 == 1) {
                canvas.drawRect(MARGIN, rowTop, A4_WIDTH - MARGIN, rowBottom, zebraRowPaint)
            }
            canvas.drawRect(MARGIN, rowTop, A4_WIDTH - MARGIN, rowBottom, borderPaint)

            val cells = arrayOf(
                posko.poskoCode,
                posko.poskoName.take(29),
                "${posko.totalJiwa}/${posko.maxCapacity}",
                posko.lansia.toString(),
                posko.ibuHamil.toString(),
                posko.balita.toString(),
                posko.disabilitas.toString()
            )
            var cellX = MARGIN
            cells.forEachIndexed { cIdx, text ->
                canvas.drawText(text, cellX + 4f, rowTop + 12f, bodyPaint)
                cellX += colWidths2[cIdx]
            }
            y += rowHeight1
        }
        y += 16f

        // 6. TABEL III: LOG AUDIT MUTASI KELUAR / DISTRIBUSI BANTUAN BERBASIS NIK 16 DIGIT
        canvas.drawText(
            "III. BUKU BESAR MUTASI KELUAR & DISTRIBUSI BANTUAN WARGA (ANTI-DATA GANDA NIK 16 DIGIT)",
            MARGIN,
            y,
            sectionTitlePaint
        )
        y += 6f

        val colWidths3 = floatArrayOf(62f, 98f, 105f, 130f, 56f, 80f)
        val headers3 = arrayOf("ID Mutasi", "NIK Penerima (16 Digit)", "Nama Penerima", "Komoditas Disalurkan", "Jumlah", "Waktu Salur")

        canvas.drawRect(MARGIN, y, A4_WIDTH - MARGIN, y + rowHeight1, tableHeaderBgPaint)
        xCursor = MARGIN
        headers3.forEachIndexed { i, h ->
            canvas.drawText(h, xCursor + 4f, y + 12f, tableHeaderTextPaint)
            xCursor += colWidths3[i]
        }
        y += rowHeight1

        distributionLedger.take(8).forEachIndexed { idx, log ->
            val rowTop = y
            val rowBottom = y + rowHeight1
            if (idx % 2 == 1) {
                canvas.drawRect(MARGIN, rowTop, A4_WIDTH - MARGIN, rowBottom, zebraRowPaint)
            }
            canvas.drawRect(MARGIN, rowTop, A4_WIDTH - MARGIN, rowBottom, borderPaint)

            val cells = arrayOf(
                log.distributionId,
                log.recipientNik,
                "${log.recipientName.take(14)} (${log.vulnerabilityGroup.take(6)})",
                log.commodityName.take(22),
                "${log.quantity} ${log.unit}",
                log.timestampIso.takeLast(11)
            )
            var cellX = MARGIN
            cells.forEachIndexed { cIdx, text ->
                canvas.drawText(text, cellX + 4f, rowTop + 12f, bodyPaint)
                cellX += colWidths3[cIdx]
            }
            y += rowHeight1
        }

        // 7. BLOK PENGESAHAN & PERTANGGUNGJAWABAN BMN/BMD DI BAGIAN BAWAH HALAMAN
        val signTopY = max(y + 22f, A4_HEIGHT - 135f)
        val leftSignX = MARGIN + 20f
        val rightSignX = A4_WIDTH - MARGIN - 185f

        val statementText =
            "Demikian Berita Acara Stock Opname Persediaan Logistik & Aset Kebencanaan ini dibuat dengan sebenarnya sebagai bukti pertanggungjawaban " +
                "tertib administrasi Pengelolaan Barang Milik Negara/Daerah (BMN/BMD) pada Pusdalops-PB BPBD Kabupaten Mukomuko."
        val textPaint = TextPaint(bodyPaint)
        val staticLayout = StaticLayout.Builder
            .obtain(statementText, 0, statementText.length, textPaint, (A4_WIDTH - 2 * MARGIN).toInt())
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .build()
        canvas.save()
        canvas.translate(MARGIN, signTopY - 14f)
        staticLayout.draw(canvas)
        canvas.restore()

        val sigBlockY = signTopY + 18f
        canvas.drawText("Mengetahui / Mengesahkan,", leftSignX, sigBlockY, bodyPaint)
        canvas.drawText("Pejabat Penatausahaan BMN/BMD / Kalak BPBD", leftSignX, sigBlockY + 11f, boldBodyPaint)

        val datePlaceStr = SimpleDateFormat("dd MMMM yyyy", idLocale).format(now)
        canvas.drawText("Mukomuko, $datePlaceStr", rightSignX, sigBlockY, bodyPaint)
        canvas.drawText("Petugas Pelaksana Stock Opname,", rightSignX, sigBlockY + 11f, boldBodyPaint)

        val pengawasSig = resolvedSignatories[com.example.model.DocumentSettings.SLOT_PENGAWAS]
        val kalakName = pengawasSig?.formattedName?.takeIf { it.isNotBlank() && it != "-" }
            ?: "Kepala Pelaksana BPBD Kab. Mukomuko"
        val kalakNip = pengawasSig?.nipLine?.takeIf { it.isNotBlank() && it != "NIP. -" }
            ?: "NIP. 197805122005011004"

        canvas.drawText(kalakName, leftSignX, sigBlockY + 54f, boldBodyPaint)
        canvas.drawText(kalakNip, leftSignX, sigBlockY + 64f, bodyPaint)

        canvas.drawText(safeOfficer, rightSignX, sigBlockY + 54f, boldBodyPaint)
        canvas.drawText("NIP/ID: $safeNip • $safeRole", rightSignX, sigBlockY + 64f, bodyPaint)

        pdfDocument.finishPage(page)

        val bos = ByteArrayOutputStream()
        pdfDocument.writeTo(bos)
        pdfDocument.close()
        return bos.toByteArray()
    }
}
