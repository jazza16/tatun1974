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

    private const val A4_WIDTH = ReportExporter.A4_PAGE_WIDTH
    private const val A4_HEIGHT = ReportExporter.A4_PAGE_HEIGHT
    private const val MARGIN = ReportExporter.STANDARD_MARGIN

    val OFFICIAL_REPORT_PURPOSES = listOf(
        "Berita Acara Stock Opname Logistik & Tertib Administrasi BMN/BMD",
        "Laporan Pertanggungjawaban Mutasi Persediaan & Distribusi Bantuan (NIK 16 Digit)",
        "Rekonsiliasi Aset Kebencanaan & Audit Inspektorat / BPKP / BNPB"
    )

    private data class OfficialDocumentSpec(
        val title: String,
        val numberPrefix: String,
        val closingDocumentName: String,
        val executorTitle: String,
        val fileNamePrefix: String
    )

    private fun resolveDocumentSpec(documentPurpose: String): OfficialDocumentSpec {
        return when {
            documentPurpose.contains("Mutasi", ignoreCase = true) -> OfficialDocumentSpec(
                title = "LAPORAN PERTANGGUNGJAWABAN MUTASI PERSEDIAAN & DISTRIBUSI BANTUAN (BMN/BMD)",
                numberPrefix = "LPJ-MUT/BPBD-MM",
                closingDocumentName = "Laporan Pertanggungjawaban Mutasi Persediaan Logistik & Distribusi Bantuan (NIK 16 Digit)",
                executorTitle = "Petugas Pelaksana Pertanggungjawaban Mutasi",
                fileNamePrefix = "LPJ_Mutasi_Persediaan_BMN_Logistik_BPBD_Mukomuko"
            )
            documentPurpose.contains("Rekonsiliasi", ignoreCase = true) -> OfficialDocumentSpec(
                title = "BERITA ACARA REKONSILIASI ASET KEBENCANAAN & TATA KELOLA BMN / BMD",
                numberPrefix = "BA-REKON/BPBD-MM",
                closingDocumentName = "Berita Acara Rekonsiliasi Aset Kebencanaan & Persediaan Logistik (BMN/BMD)",
                executorTitle = "Petugas Pelaksana Rekonsiliasi Aset BMN/BMD",
                fileNamePrefix = "BA_Rekonsiliasi_Aset_BMN_Logistik_BPBD_Mukomuko"
            )
            else -> OfficialDocumentSpec(
                title = "BERITA ACARA STOCK OPNAME LOGISTIK & TATA KELOLA BARANG MILIK NEGARA / DAERAH (BMN/BMD)",
                numberPrefix = "BA-SO/BPBD-MM",
                closingDocumentName = "Berita Acara Stock Opname Persediaan Logistik & Aset Kebencanaan",
                executorTitle = "Petugas Pelaksana Stock Opname",
                fileNamePrefix = "BA_Stock_Opname_BMN_Logistik_BPBD_Mukomuko"
            )
        }
    }

    /**
     * Menghitung posisi Y awal blok pernyataan penutup & tanda tangan pejabat tepat 16 pt
     * di bawah batas akhir tabel terakhir (tanpa ruang kosong berlebih), dengan batas aman
     * agar tidak pernah menumpuk di atas Footer Resmi (`A4_HEIGHT - 28f`).
     */
    internal fun computeSignatureBlockTopY(tableEndY: Float): Float {
        return (tableEndY + 16f).coerceAtMost(A4_HEIGHT - 132f)
    }

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
     * Mengekspor langsung dari [PoskoLogisticsRepository] ke dokumen PDF Resmi Berita Acara Stock Opname / LPJ Mutasi BMN/BMD.
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
        val docSpec = resolveDocumentSpec(documentPurpose)

        val idLocale = Locale.forLanguageTag("id-ID")
        val timestampTag = SimpleDateFormat("yyyyMMdd_HHmm", idLocale).apply {
            timeZone = TimeZone.getTimeZone("Asia/Jakarta")
        }.format(Date())

        val downloadName = "${docSpec.fileNamePrefix}_$timestampTag.pdf"
        val outFile = File(getExportDir(context), downloadName)

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
                    "PDF ${docSpec.closingDocumentName} tersimpan di Downloads/SIPAGAR_KITA/$downloadName"
                } else {
                    "PDF ${docSpec.closingDocumentName} siap diunduh & dibagikan"
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
                    "${docSpec.closingDocumentName} BPBD Kab. Mukomuko (${stocks.size} Komoditas)"
                )
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Terlampir Dokumen Resmi ${docSpec.closingDocumentName} Pusdalops-PB BPBD Kabupaten Mukomuko untuk keperluan: $documentPurpose."
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(shareIntent, "Unduh / Bagikan PDF Resmi BMN/BMD").apply {
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
        val docSpec = resolveDocumentSpec(documentPurpose)

        val idLocale = Locale.forLanguageTag("id-ID")
        val timestampTag = SimpleDateFormat("yyyyMMdd_HHmm", idLocale).apply {
            timeZone = TimeZone.getTimeZone("Asia/Jakarta")
        }.format(Date())

        val downloadName = "${docSpec.fileNamePrefix}_$timestampTag.csv"
        val outFile = File(getExportDir(context), downloadName)

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
                    "CSV/Excel ${docSpec.closingDocumentName} tersimpan di Downloads/SIPAGAR_KITA/$downloadName"
                } else {
                    "CSV/Excel ${docSpec.closingDocumentName} siap diunduh & dibagikan"
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
                    "CSV ${docSpec.closingDocumentName} BPBD Kab. Mukomuko (${stocks.size} Komoditas)"
                )
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Terlampir Kertas Kerja CSV/Excel ${docSpec.closingDocumentName} BPBD Kabupaten Mukomuko untuk keperluan: $documentPurpose."
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(shareIntent, "Unduh / Bagikan CSV Stock Opname & Mutasi BMN/BMD").apply {
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
        val docSpec = resolveDocumentSpec(documentPurpose)

        val sb = StringBuilder()
        // Standar Kop Surat Resmi 4 Baris sesuai Laporan Harian Pusdalops
        sb.appendLine(com.example.model.DocumentSettings.DEFAULT_KOP_LINE_1)
        sb.appendLine(com.example.model.DocumentSettings.DEFAULT_KOP_LINE_2)
        sb.appendLine(com.example.model.DocumentSettings.DEFAULT_KOP_LINE_3)
        sb.appendLine(com.example.model.DocumentSettings.DEFAULT_KOP_ADDRESS)
        sb.appendLine(docSpec.title)
        sb.appendLine("Tujuan Dokumen,${sanitizeCsvCell(documentPurpose)}")
        sb.appendLine("Waktu Cetak Dokumen,${sanitizeCsvCell(printedAt)}")
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
        sb.appendLine()
        sb.appendLine(com.example.model.DocumentSettings.DEFAULT_FOOTER_TEXT)

        return sb.toString()
    }

    fun buildStockOpnamePdfBytes(
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
            DocumentSettingsRepository.getCachedOrDefaultSettings(context).clamped()
        }.getOrDefault(com.example.model.DocumentSettings().clamped())

        val resolvedSignatories = runCatching {
            DocumentSettingsRepository.resolveSignatories(context, docSettings)
        }.getOrDefault(emptyMap())

        val resolvedOfficer = runCatching {
            DocumentSettingsRepository.resolveOfficerIdentity(
                context = context,
                rawName = officerName,
                rawNip = officerNip,
                settings = docSettings
            )
        }.getOrNull()

        val docSpec = resolveDocumentSpec(documentPurpose)
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
        canvas.drawColor(Color.WHITE)

        val contentWidth = A4_WIDTH - (MARGIN * 2f)
        val centerX = A4_WIDTH / 2f

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val subHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 41, 59)
            textSize = 8.2f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        val sectionTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 9.0f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 41, 59)
            textSize = 7.8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val boldBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 7.8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val tableHeaderBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            style = Paint.Style.FILL
        }
        val tableHeaderTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 7.4f
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

        // 1. KOP SURAT RESMI STANDAR (SAMA DENGAN LAPORAN HARIAN PUSDALOPS & DOKUMEN RESMI LAINNYA)
        var y = ReportExporter.drawKopSuratResmi(
            context = context,
            canvas = canvas,
            pageWidth = A4_WIDTH,
            margin = MARGIN,
            pageNumber = 1,
            documentSettings = docSettings
        )

        // 2. JUDUL DOKUMEN RESMI (BA STOCK OPNAME / LPJ MUTASI / BA REKONSILIASI ASET)
        y += 2f
        PdfTextAutoShrinkHelper.drawAutoShrinkText(
            canvas = canvas,
            text = docSpec.title,
            x = centerX,
            y = y,
            maxWidth = contentWidth,
            basePaint = titlePaint,
            initialFontSize = 10.2f,
            minFontSize = 7.2f
        )
        y += 11.5f
        PdfTextAutoShrinkHelper.drawAutoShrinkText(
            canvas = canvas,
            text = "Nomor Dokumen: ${docSpec.numberPrefix}/$docNumberDate • Dicetak: $fullDateStr",
            x = centerX,
            y = y,
            maxWidth = contentWidth,
            basePaint = subHeaderPaint,
            initialFontSize = 8.0f,
            minFontSize = 6.5f
        )
        y += 12f

        // 3. KOTAK RINGKASAN METADATA PERTANGGUNGJAWABAN PETUGAS (AUTO-SHRINK AGAR TIDAK MELEBAR KELUAR KOTAK)
        val criticalCount = stocks.count { it.isCritical }
        val fefoWarnCount = stocks.count { it.isExpired || it.isExpiringSoonFefo }
        val totalRefugees = poskoUnits.sumOf { it.totalJiwa }
        val totalVulnerable = poskoUnits.sumOf { it.totalKelompokRentan }

        val metaBoxHeight = 52f
        val metaRect = RectF(MARGIN, y, A4_WIDTH - MARGIN, y + metaBoxHeight)
        canvas.drawRoundRect(metaRect, 6f, 6f, zebraRowPaint)
        canvas.drawRoundRect(metaRect, 6f, 6f, borderPaint)

        val formattedOfficerName = resolvedOfficer?.formattedName?.takeIf { it.isNotBlank() && it != "-" }
            ?: officerName.ifBlank { "Petugas Posko & Logistik" }
        val formattedNipLine = resolvedOfficer?.nipLine?.takeIf { it.isNotBlank() && it != "NIP. -" }
            ?: officerNip.takeIf { it.isNotBlank() && it != "-" }?.let { "NIP. $it" }
            ?: "NIP. -"
        val safeRole = officerRole.ifBlank {
            resolvedOfficer?.role?.takeIf { it.isNotBlank() } ?: "Manajer Pusdalops-PB"
        }

        val innerMetaX = MARGIN + 10f
        val innerMetaMaxW = contentWidth - 20f

        PdfTextAutoShrinkHelper.drawAutoShrinkText(
            canvas = canvas,
            text = "Tujuan Pelaporan: $documentPurpose",
            x = innerMetaX,
            y = y + 12.5f,
            maxWidth = innerMetaMaxW,
            basePaint = boldBodyPaint,
            initialFontSize = 7.8f,
            minFontSize = 6.2f
        )
        PdfTextAutoShrinkHelper.drawAutoShrinkText(
            canvas = canvas,
            text = "Pejabat / Petugas Pelapor: $formattedOfficerName ($formattedNipLine • Jabatan: $safeRole)",
            x = innerMetaX,
            y = y + 24.5f,
            maxWidth = innerMetaMaxW,
            basePaint = bodyPaint,
            initialFontSize = 7.6f,
            minFontSize = 6.0f
        )
        PdfTextAutoShrinkHelper.drawAutoShrinkText(
            canvas = canvas,
            text = "Ringkasan Aset & Posko: ${stocks.size} Komoditas Logistik ($criticalCount Status Kritis <10, $fefoWarnCount Peringatan FEFO) • ${poskoUnits.size} Posko Aktif ($totalRefugees Jiwa / $totalVulnerable Rentan)",
            x = innerMetaX,
            y = y + 36.5f,
            maxWidth = innerMetaMaxW,
            basePaint = bodyPaint,
            initialFontSize = 7.6f,
            minFontSize = 6.0f
        )
        PdfTextAutoShrinkHelper.drawAutoShrinkText(
            canvas = canvas,
            text = "Standar Kepatuhan: PP No. 27/2014 & Permendagri No. 19/2016 tentang Pedoman Pengelolaan Barang Milik Negara/Daerah (BMN/BMD) & Perka BNPB",
            x = innerMetaX,
            y = y + 47.5f,
            maxWidth = innerMetaMaxW,
            basePaint = bodyPaint,
            initialFontSize = 7.5f,
            minFontSize = 5.8f
        )
        y += metaBoxHeight + 13f

        // 4. TABEL I: STOCK OPNAME PERSEDIAAN LOGISTIK & FEFO (Total Lebar = 523pt)
        PdfTextAutoShrinkHelper.drawAutoShrinkText(
            canvas = canvas,
            text = "I. TABEL PEMERIKSAAN FISIK (STOCK OPNAME) PERSEDIAAN LOGISTIK & STATUS FEFO (BMN/BMD)",
            x = MARGIN,
            y = y,
            maxWidth = contentWidth,
            basePaint = sectionTitlePaint,
            initialFontSize = 8.8f,
            minFontSize = 6.8f
        )
        y += 5f

        val colWidths1 = floatArrayOf(24f, 66f, 151f, 64f, 60f, 76f, 82f) // Total = 523pt
        val headers1 = arrayOf("No", "Kode BMN", "Nama Komoditas Logistik", "Posko", "Saldo Fisik", "Tgl Exp (FEFO)", "Status Audit")
        val colCentered1 = booleanArrayOf(true, false, false, true, true, true, true)
        val rowHeight1 = 16.5f
        val gridLinePaint = Paint(borderPaint).apply { strokeWidth = 0.7f }
        val tableHeaderCenterPaint = Paint(tableHeaderTextPaint).apply { textAlign = Paint.Align.CENTER }
        val bodyCenterPaint = Paint(bodyPaint).apply { textAlign = Paint.Align.CENTER }
        val boldCenterPaint = Paint(boldBodyPaint).apply { textAlign = Paint.Align.CENTER }

        // Header Tabel I
        canvas.drawRect(MARGIN, y, A4_WIDTH - MARGIN, y + rowHeight1, tableHeaderBgPaint)
        var xCursor = MARGIN
        headers1.forEachIndexed { i, h ->
            val centered = colCentered1[i]
            val drawX = if (centered) xCursor + (colWidths1[i] / 2f) else xCursor + 4f
            val baseP = if (centered) tableHeaderCenterPaint else tableHeaderTextPaint
            PdfTextAutoShrinkHelper.drawAutoShrinkText(
                canvas = canvas,
                text = h,
                x = drawX,
                y = y + 11.5f,
                maxWidth = colWidths1[i] - 8f,
                basePaint = baseP,
                initialFontSize = 7.3f,
                minFontSize = 5.6f
            )
            if (i > 0) {
                canvas.drawLine(xCursor, y, xCursor, y + rowHeight1, gridLinePaint)
            }
            xCursor += colWidths1[i]
        }
        canvas.drawRect(MARGIN, y, A4_WIDTH - MARGIN, y + rowHeight1, gridLinePaint)
        y += rowHeight1

        if (stocks.isEmpty()) {
            canvas.drawRect(MARGIN, y, A4_WIDTH - MARGIN, y + rowHeight1, gridLinePaint)
            PdfTextAutoShrinkHelper.drawAutoShrinkText(
                canvas = canvas,
                text = "Belum ada data komoditas logistik tercatat.",
                x = centerX,
                y = y + 11.5f,
                maxWidth = contentWidth - 16f,
                basePaint = subHeaderPaint,
                initialFontSize = 7.5f,
                minFontSize = 6.0f
            )
            y += rowHeight1
        } else {
            stocks.take(10).forEachIndexed { idx, item ->
                val rowTop = y
                val rowBottom = y + rowHeight1
                if (item.isCritical) {
                    canvas.drawRect(MARGIN, rowTop, A4_WIDTH - MARGIN, rowBottom, criticalRowPaint)
                } else if (idx % 2 == 1) {
                    canvas.drawRect(MARGIN, rowTop, A4_WIDTH - MARGIN, rowBottom, zebraRowPaint)
                }
                canvas.drawRect(MARGIN, rowTop, A4_WIDTH - MARGIN, rowBottom, gridLinePaint)

                val auditLabel = when {
                    item.isExpired -> "EXP / TARIK"
                    item.isCritical -> "KRITIS (<10)"
                    item.isExpiringSoonFefo -> "FEFO <=30Hr"
                    else -> "AMAN / LAYAK"
                }

                val cells = arrayOf(
                    (idx + 1).toString(),
                    item.code,
                    item.name,
                    item.poskoCode,
                    "${item.currentStock} ${item.unit}",
                    item.expiryDateIso ?: "Non-Exp",
                    auditLabel
                )

                var cellX = MARGIN
                cells.forEachIndexed { cIdx, text ->
                    if (cIdx > 0) {
                        canvas.drawLine(cellX, rowTop, cellX, rowBottom, gridLinePaint)
                    }
                    val centered = colCentered1[cIdx]
                    val p = when {
                        centered && (cIdx == 4 || cIdx == 6) -> boldCenterPaint
                        centered -> bodyCenterPaint
                        cIdx == 4 || cIdx == 6 -> boldBodyPaint
                        else -> bodyPaint
                    }
                    val drawX = if (centered) cellX + (colWidths1[cIdx] / 2f) else cellX + 4f
                    PdfTextAutoShrinkHelper.drawAutoShrinkText(
                        canvas = canvas,
                        text = text,
                        x = drawX,
                        y = rowTop + 11.5f,
                        maxWidth = colWidths1[cIdx] - 8f,
                        basePaint = p,
                        initialFontSize = 7.5f,
                        minFontSize = 5.6f
                    )
                    cellX += colWidths1[cIdx]
                }
                y += rowHeight1
            }
        }
        y += 13f

        // 5. TABEL II: REKAPITULASI PENGUNGSI POSKO AKTIF (Total Lebar = 523pt)
        PdfTextAutoShrinkHelper.drawAutoShrinkText(
            canvas = canvas,
            text = "II. REKAPITULASI DATA PENGUNGSI TERPILAH PER POSKO AKTIF (DASAR DISTRIBUSI LOGISTIK)",
            x = MARGIN,
            y = y,
            maxWidth = contentWidth,
            basePaint = sectionTitlePaint,
            initialFontSize = 8.8f,
            minFontSize = 6.8f
        )
        y += 5f

        val colWidths2 = floatArrayOf(74f, 165f, 68f, 54f, 54f, 54f, 54f) // Total = 523pt
        val headers2 = arrayOf("Kode Posko", "Nama Posko Pengungsian", "Total Jiwa", "Lansia", "Ibu Hamil", "Balita", "Disabilitas")
        val colCentered2 = booleanArrayOf(true, false, true, true, true, true, true)

        canvas.drawRect(MARGIN, y, A4_WIDTH - MARGIN, y + rowHeight1, tableHeaderBgPaint)
        xCursor = MARGIN
        headers2.forEachIndexed { i, h ->
            val centered = colCentered2[i]
            val drawX = if (centered) xCursor + (colWidths2[i] / 2f) else xCursor + 4f
            val baseP = if (centered) tableHeaderCenterPaint else tableHeaderTextPaint
            PdfTextAutoShrinkHelper.drawAutoShrinkText(
                canvas = canvas,
                text = h,
                x = drawX,
                y = y + 11.5f,
                maxWidth = colWidths2[i] - 8f,
                basePaint = baseP,
                initialFontSize = 7.3f,
                minFontSize = 5.6f
            )
            if (i > 0) {
                canvas.drawLine(xCursor, y, xCursor, y + rowHeight1, gridLinePaint)
            }
            xCursor += colWidths2[i]
        }
        canvas.drawRect(MARGIN, y, A4_WIDTH - MARGIN, y + rowHeight1, gridLinePaint)
        y += rowHeight1

        if (poskoUnits.isEmpty()) {
            canvas.drawRect(MARGIN, y, A4_WIDTH - MARGIN, y + rowHeight1, gridLinePaint)
            PdfTextAutoShrinkHelper.drawAutoShrinkText(
                canvas = canvas,
                text = "Belum ada data posko pengungsian aktif tercatat.",
                x = centerX,
                y = y + 11.5f,
                maxWidth = contentWidth - 16f,
                basePaint = subHeaderPaint,
                initialFontSize = 7.5f,
                minFontSize = 6.0f
            )
            y += rowHeight1
        } else {
            poskoUnits.take(5).forEachIndexed { idx, posko ->
                val rowTop = y
                val rowBottom = y + rowHeight1
                if (idx % 2 == 1) {
                    canvas.drawRect(MARGIN, rowTop, A4_WIDTH - MARGIN, rowBottom, zebraRowPaint)
                }
                canvas.drawRect(MARGIN, rowTop, A4_WIDTH - MARGIN, rowBottom, gridLinePaint)

                val cells = arrayOf(
                    posko.poskoCode,
                    posko.poskoName,
                    "${posko.totalJiwa}/${posko.maxCapacity}",
                    posko.lansia.toString(),
                    posko.ibuHamil.toString(),
                    posko.balita.toString(),
                    posko.disabilitas.toString()
                )
                var cellX = MARGIN
                cells.forEachIndexed { cIdx, text ->
                    if (cIdx > 0) {
                        canvas.drawLine(cellX, rowTop, cellX, rowBottom, gridLinePaint)
                    }
                    val centered = colCentered2[cIdx]
                    val drawX = if (centered) cellX + (colWidths2[cIdx] / 2f) else cellX + 4f
                    val p = if (centered) bodyCenterPaint else bodyPaint
                    PdfTextAutoShrinkHelper.drawAutoShrinkText(
                        canvas = canvas,
                        text = text,
                        x = drawX,
                        y = rowTop + 11.5f,
                        maxWidth = colWidths2[cIdx] - 8f,
                        basePaint = p,
                        initialFontSize = 7.5f,
                        minFontSize = 5.6f
                    )
                    cellX += colWidths2[cIdx]
                }
                y += rowHeight1
            }
        }
        y += 13f

        // 6. TABEL III: LOG AUDIT MUTASI KELUAR / DISTRIBUSI BANTUAN BERBASIS NIK 16 DIGIT (Total Lebar = 523pt)
        PdfTextAutoShrinkHelper.drawAutoShrinkText(
            canvas = canvas,
            text = "III. BUKU BESAR MUTASI KELUAR & DISTRIBUSI BANTUAN WARGA (ANTI-DATA GANDA NIK 16 DIGIT)",
            x = MARGIN,
            y = y,
            maxWidth = contentWidth,
            basePaint = sectionTitlePaint,
            initialFontSize = 8.8f,
            minFontSize = 6.8f
        )
        y += 5f

        val colWidths3 = floatArrayOf(62f, 96f, 108f, 125f, 54f, 78f) // Total = 523pt
        val headers3 = arrayOf("ID Mutasi", "NIK Penerima (16 Digit)", "Nama Penerima", "Komoditas Disalurkan", "Jumlah", "Waktu Salur")
        val colCentered3 = booleanArrayOf(true, true, false, false, true, true)

        canvas.drawRect(MARGIN, y, A4_WIDTH - MARGIN, y + rowHeight1, tableHeaderBgPaint)
        xCursor = MARGIN
        headers3.forEachIndexed { i, h ->
            val centered = colCentered3[i]
            val drawX = if (centered) xCursor + (colWidths3[i] / 2f) else xCursor + 4f
            val baseP = if (centered) tableHeaderCenterPaint else tableHeaderTextPaint
            PdfTextAutoShrinkHelper.drawAutoShrinkText(
                canvas = canvas,
                text = h,
                x = drawX,
                y = y + 11.5f,
                maxWidth = colWidths3[i] - 8f,
                basePaint = baseP,
                initialFontSize = 7.3f,
                minFontSize = 5.6f
            )
            if (i > 0) {
                canvas.drawLine(xCursor, y, xCursor, y + rowHeight1, gridLinePaint)
            }
            xCursor += colWidths3[i]
        }
        canvas.drawRect(MARGIN, y, A4_WIDTH - MARGIN, y + rowHeight1, gridLinePaint)
        y += rowHeight1

        if (distributionLedger.isEmpty()) {
            canvas.drawRect(MARGIN, y, A4_WIDTH - MARGIN, y + rowHeight1, gridLinePaint)
            PdfTextAutoShrinkHelper.drawAutoShrinkText(
                canvas = canvas,
                text = "Belum ada mutasi distribusi bantuan warga yang tercatat.",
                x = centerX,
                y = y + 11.5f,
                maxWidth = contentWidth - 16f,
                basePaint = subHeaderPaint,
                initialFontSize = 7.5f,
                minFontSize = 6.0f
            )
            y += rowHeight1
        } else {
            distributionLedger.take(6).forEachIndexed { idx, log ->
                val rowTop = y
                val rowBottom = y + rowHeight1
                if (idx % 2 == 1) {
                    canvas.drawRect(MARGIN, rowTop, A4_WIDTH - MARGIN, rowBottom, zebraRowPaint)
                }
                canvas.drawRect(MARGIN, rowTop, A4_WIDTH - MARGIN, rowBottom, gridLinePaint)

                val cells = arrayOf(
                    log.distributionId,
                    log.recipientNik,
                    "${log.recipientName} (${log.vulnerabilityGroup})",
                    log.commodityName,
                    "${log.quantity} ${log.unit}",
                    log.timestampIso.takeLast(11)
                )
                var cellX = MARGIN
                cells.forEachIndexed { cIdx, text ->
                    if (cIdx > 0) {
                        canvas.drawLine(cellX, rowTop, cellX, rowBottom, gridLinePaint)
                    }
                    val centered = colCentered3[cIdx]
                    val drawX = if (centered) cellX + (colWidths3[cIdx] / 2f) else cellX + 4f
                    val p = if (centered) bodyCenterPaint else bodyPaint
                    PdfTextAutoShrinkHelper.drawAutoShrinkText(
                        canvas = canvas,
                        text = text,
                        x = drawX,
                        y = rowTop + 11.5f,
                        maxWidth = colWidths3[cIdx] - 8f,
                        basePaint = p,
                        initialFontSize = 7.5f,
                        minFontSize = 5.6f
                    )
                    cellX += colWidths3[cIdx]
                }
                y += rowHeight1
            }
        }

        // 7. BLOK PENGESAHAN & PERTANGGUNGJAWABAN BMN/BMD (PRESISI TEPAT DI BAWAH TABEL TERAKHIR)
        // Diposisikan proporsional tepat 16 pt di bawah Tabel III agar tidak menyisakan ruang kosong berlebih,
        // dengan batas bawah aman agar tidak pernah bertabrakan dengan Footer Resmi.
        val signTopY = computeSignatureBlockTopY(y)

        val statementText = PdfTextAutoShrinkHelper.normalizePdfWordSpacing(
            "Demikian ${docSpec.closingDocumentName} ini dibuat dengan sebenarnya sebagai bukti pertanggungjawaban " +
                "tertib administrasi Pengelolaan Barang Milik Negara/Daerah (BMN/BMD) pada Pusdalops-PB BPBD Kabupaten Mukomuko."
        )
        val textPaint = PdfTextAutoShrinkHelper.configurePdfPaint(
            TextPaint(bodyPaint).apply { textSize = 7.6f },
            letterSpacingEm = 0.025f
        )
        val staticLayout = StaticLayout.Builder
            .obtain(statementText, 0, statementText.length, textPaint, contentWidth.toInt())
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(1.5f, 1.15f)
            .setIncludePad(false)
            .build()
        canvas.save()
        canvas.translate(MARGIN, signTopY)
        staticLayout.draw(canvas)
        canvas.restore()

        val sigBlockY = signTopY + staticLayout.height + 16f
        val leftColCenterX = MARGIN + (contentWidth * 0.25f)
        val rightColCenterX = A4_WIDTH - MARGIN - (contentWidth * 0.25f)
        val sigColMaxW = (contentWidth * 0.46f).coerceAtLeast(160f)

        val sigRolePaint = Paint(bodyPaint).apply {
            textAlign = Paint.Align.CENTER
            textSize = 7.8f
            color = Color.rgb(30, 41, 59)
        }
        val sigRoleBoldPaint = Paint(boldBodyPaint).apply {
            textAlign = Paint.Align.CENTER
            textSize = 8.0f
            color = Color.rgb(15, 23, 42)
        }
        val sigNamePaint = Paint(boldBodyPaint).apply {
            textAlign = Paint.Align.CENTER
            textSize = 8.2f
            isUnderlineText = true
            color = Color.rgb(15, 23, 42)
        }
        val sigNipPaint = Paint(bodyPaint).apply {
            textAlign = Paint.Align.CENTER
            textSize = 7.6f
            color = Color.rgb(51, 65, 85)
        }

        // Kolom Kiri Atas: Mengetahui / Mengesahkan Pejabat Penatausahaan BMN/BMD / Kalak BPBD
        PdfTextAutoShrinkHelper.drawAutoShrinkText(
            canvas = canvas,
            text = "Mengetahui / Mengesahkan,",
            x = leftColCenterX,
            y = sigBlockY,
            maxWidth = sigColMaxW,
            basePaint = sigRolePaint,
            initialFontSize = 7.8f,
            minFontSize = 6.2f
        )
        PdfTextAutoShrinkHelper.drawAutoShrinkText(
            canvas = canvas,
            text = "Kepala Pelaksana BPBD Kab. Mukomuko",
            x = leftColCenterX,
            y = sigBlockY + 10.5f,
            maxWidth = sigColMaxW,
            basePaint = sigRoleBoldPaint,
            initialFontSize = 8.0f,
            minFontSize = 6.2f
        )
        PdfTextAutoShrinkHelper.drawAutoShrinkText(
            canvas = canvas,
            text = "Selaku Pejabat Penatausahaan BMN/BMD",
            x = leftColCenterX,
            y = sigBlockY + 20.5f,
            maxWidth = sigColMaxW,
            basePaint = sigRolePaint,
            initialFontSize = 7.6f,
            minFontSize = 6.0f
        )

        // Kolom Kanan Atas: Tanggal, Jabatan Resmi (mis. Manajer Pusdalops-PB) & Peran Pelaksana
        val datePlaceStr = SimpleDateFormat("dd MMMM yyyy", idLocale).format(now)
        PdfTextAutoShrinkHelper.drawAutoShrinkText(
            canvas = canvas,
            text = "Mukomuko, $datePlaceStr",
            x = rightColCenterX,
            y = sigBlockY,
            maxWidth = sigColMaxW,
            basePaint = sigRolePaint,
            initialFontSize = 7.8f,
            minFontSize = 6.2f
        )
        PdfTextAutoShrinkHelper.drawAutoShrinkText(
            canvas = canvas,
            text = safeRole,
            x = rightColCenterX,
            y = sigBlockY + 10.5f,
            maxWidth = sigColMaxW,
            basePaint = sigRoleBoldPaint,
            initialFontSize = 8.0f,
            minFontSize = 6.2f
        )
        PdfTextAutoShrinkHelper.drawAutoShrinkText(
            canvas = canvas,
            text = "Selaku ${docSpec.executorTitle}",
            x = rightColCenterX,
            y = sigBlockY + 20.5f,
            maxWidth = sigColMaxW,
            basePaint = sigRolePaint,
            initialFontSize = 7.6f,
            minFontSize = 6.0f
        )

        // Kolom Kiri Bawah: Nama & NIP Kepala Pelaksana BPBD
        val pengawasSig = resolvedSignatories[com.example.model.DocumentSettings.SLOT_PENGAWAS]
        val kalakName = pengawasSig?.formattedName?.takeIf { it.isNotBlank() && it != "-" }
            ?: "RURI IRWANDI, ST, MT"
        val kalakNip = pengawasSig?.nipLine?.takeIf { it.isNotBlank() && it != "NIP. -" }
            ?: "NIP. 197906072006041011"

        PdfTextAutoShrinkHelper.drawAutoShrinkText(
            canvas = canvas,
            text = kalakName,
            x = leftColCenterX,
            y = sigBlockY + 54.5f,
            maxWidth = sigColMaxW,
            basePaint = sigNamePaint,
            initialFontSize = 8.2f,
            minFontSize = 6.5f
        )
        PdfTextAutoShrinkHelper.drawAutoShrinkText(
            canvas = canvas,
            text = kalakNip,
            x = leftColCenterX,
            y = sigBlockY + 65f,
            maxWidth = sigColMaxW,
            basePaint = sigNipPaint,
            initialFontSize = 7.6f,
            minFontSize = 6.2f
        )

        // Kolom Kanan Bawah: Nama & NIP Petugas (Tanpa menempelkan jabatan di samping NIP agar tidak melebar)
        PdfTextAutoShrinkHelper.drawAutoShrinkText(
            canvas = canvas,
            text = formattedOfficerName,
            x = rightColCenterX,
            y = sigBlockY + 54.5f,
            maxWidth = sigColMaxW,
            basePaint = sigNamePaint,
            initialFontSize = 8.2f,
            minFontSize = 6.5f
        )
        PdfTextAutoShrinkHelper.drawAutoShrinkText(
            canvas = canvas,
            text = formattedNipLine,
            x = rightColCenterX,
            y = sigBlockY + 65f,
            maxWidth = sigColMaxW,
            basePaint = sigNipPaint,
            initialFontSize = 7.6f,
            minFontSize = 6.2f
        )

        // 8. FOOTER ATRIBUSI RESMI (DISAMAKAN DENGAN LAPORAN HARIAN PUSDALOPS & DOKUMEN RESMI LAINNYA)
        ReportExporter.drawFooterAtribusiResmi(
            canvas = canvas,
            pageWidth = A4_WIDTH,
            pageHeight = A4_HEIGHT,
            margin = MARGIN,
            documentSettings = docSettings
        )

        pdfDocument.finishPage(page)

        val bos = ByteArrayOutputStream()
        pdfDocument.writeTo(bos)
        pdfDocument.close()
        return bos.toByteArray()
    }
}
