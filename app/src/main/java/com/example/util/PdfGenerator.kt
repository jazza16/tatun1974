package com.example.util

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.data.local.LaporanHarianEntity
import com.example.data.local.LogKomunikasiEntity

object PdfGenerator {

    private const val FOOTER_ATRIBUSI_TEXT =
        "Dicetak melalui Aplikasi SIPAGAR KITA - Pusdalops PB BPBD Kabupaten Mukomuko"

    /**
     * Menghasilkan blok HTML Kop Surat Resmi terstandarisasi:
     * - Sebelah KIRI  : Logo Kabupaten Mukomuko (transparan tanpa background, proporsional)
     * - Bagian TENGAH : Teks Instansi Pemerintah Kabupaten Mukomuko & BPBD
     * - Sebelah KANAN : Logo BPBD Kabupaten Mukomuko (transparan tanpa background, proporsional & sejajar)
     */
    private fun buildKopSuratResmiHtml(context: Context): String {
        val leftMukomukoLogoUri = ReportExporter.getCleanLogoDataUri(context, isMukomukoShield = true)
            .ifBlank { "file:///android_res/drawable/ic_mukomuko_logo.png" }
        val rightBpbdLogoUri = ReportExporter.getCleanLogoDataUri(context, isMukomukoShield = false)
            .ifBlank { "file:///android_res/drawable/ic_bpbd_mukomuko_circle.png" }

        return """
            <table class="kop-table">
                <tr>
                    <td class="kop-logo-cell" width="14%">
                        <img src="$leftMukomukoLogoUri" class="kop-logo" alt="Logo Kabupaten Mukomuko" />
                    </td>
                    <td class="kop-text-cell" width="72%">
                        <div class="kop-instansi">PEMERINTAH KABUPATEN MUKOMUKO</div>
                        <div class="kop-dinas">BADAN PENANGGULANGAN BENCANA DAERAH (BPBD)</div>
                        <div class="kop-unit">PUSAT PENGENDALIAN OPERASI PENANGGULANGAN BENCANA (PUSDALOPS-PB)</div>
                        <div class="kop-alamat">Jl. Danau Nibung, Kompleks Perkantoran Pemkab Mukomuko • Call Center Darurat 117 • Sistem SIPAGAR KITA</div>
                    </td>
                    <td class="kop-logo-cell" width="14%">
                        <img src="$rightBpbdLogoUri" class="kop-logo" alt="Logo BPBD Kabupaten Mukomuko" />
                    </td>
                </tr>
            </table>
            <div class="kop-divider-thick"></div>
            <div class="kop-divider-thin"></div>
        """.trimIndent()
    }

    /**
     * Menghasilkan blok HTML atribusi footer resmi di bagian paling bawah halaman.
     */
    private fun buildFooterAtribusiResmiHtml(): String = """
        <div class="pdf-footer">
            $FOOTER_ATRIBUSI_TEXT
        </div>
    """.trimIndent()

    fun cetakLaporanKePdf(
        context: Context,
        laporan: LaporanHarianEntity,
        listLog: List<LogKomunikasiEntity>
    ) {
        // 1. Susun teks HTML berdasarkan data dari Database
        val htmlContent = """
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; font-size: 12px; margin: 20px; padding-bottom: 40px; }
                    h2 { text-align: center; font-size: 15px; margin-top: 14px; margin-bottom: 5px; }
                    p { text-align: justify; margin-bottom: 15px; }
                    h3 { font-size: 14px; margin-top: 20px; }
                    table { width: 100%; border-collapse: collapse; margin-top: 10px; margin-bottom: 20px; table-layout: fixed; word-wrap: break-word; }
                    th, td { border: 1px solid black; padding: 8px; text-align: left; vertical-align: top; word-break: break-word; overflow-wrap: break-word; }
                    .no-border { border: none; }
                    .signature-table td { border: none; text-align: center; padding-top: 30px; }
                    .log-table th { background-color: #f2f2f2; text-align: center; }
                    .kop-table { width: 100%; border: none; margin-top: 0; margin-bottom: 6px; background: transparent; }
                    .kop-table td { border: none; padding: 2px 4px; vertical-align: middle; background: transparent; }
                    .kop-logo-cell { text-align: center; vertical-align: middle; background: transparent; }
                    .kop-logo { height: 60px; width: auto; max-width: 64px; object-fit: contain; background: transparent; display: inline-block; vertical-align: middle; }
                    .kop-text-cell { text-align: center; line-height: 1.35; vertical-align: middle; }
                    .kop-instansi { font-size: 14px; font-weight: bold; color: #0F172A; }
                    .kop-dinas { font-size: 14px; font-weight: bold; color: #0F172A; }
                    .kop-unit { font-size: 11px; font-weight: bold; color: #C2410C; margin-top: 2px; }
                    .kop-alamat { font-size: 9.5px; color: #334155; margin-top: 2px; }
                    .kop-divider-thick { border-top: 2.5px solid #0F172A; margin-top: 4px; }
                    .kop-divider-thin { border-top: 1px solid #64748B; margin-top: 2px; margin-bottom: 12px; }
                    .pdf-footer {
                        position: fixed;
                        bottom: 0;
                        left: 0;
                        right: 0;
                        border-top: 1px solid #CBD5E1;
                        padding-top: 5px;
                        font-size: 9.5px;
                        color: #64748B;
                        text-align: center;
                    }
                </style>
            </head>
            <body>
                ${buildKopSuratResmiHtml(context)}
                <h2>LAPORAN HARIAN PUSDALOPS</h2>
                <p>Dilaporkan kejadian alam dan bencana di Kabupaten Mukomuko publik 08.00 WIB, sampai dengan pukul 08.00 WIB, yang diperoleh Pusdalops-PB BPBD Kabupaten Mukomuko sebagai berikut :</p>
                
                <h3>I. Kejadian Bencana</h3>
                <table>
                    <tr><td width="30%"><b>Jenis Bencana</b></td><td>${laporan.jenisBencana}</td></tr>
                    <tr><td><b>Tanggal / Waktu</b></td><td>${laporan.tanggalKejadian} / ${laporan.waktuKejadian}</td></tr>
                    <tr><td><b>Kedalaman</b></td><td>${laporan.kedalaman}</td></tr>
                    <tr><td><b>Lokasi Bencana</b></td><td>${laporan.lokasiBencana}</td></tr>
                    <tr><td><b>Koordinat</b></td><td>${laporan.koordinat}</td></tr>
                    <tr><td><b>Cakupan Dampak</b></td><td>${laporan.cakupanDampak}</td></tr>
                    <tr><td><b>Kondisi Cuaca</b></td><td>${laporan.kondisiCuaca}</td></tr>
                    <tr><td><b>Upaya Dilakukan</b></td><td>${laporan.upayaDilakukan}</td></tr>
                    <tr><td><b>Kondisi Mutakhir</b></td><td>${laporan.kondisiMutakhir}</td></tr>
                    <tr><td><b>Korban</b></td><td>Luka: ${laporan.korbanLuka} Org, Meninggal: ${laporan.korbanMeninggal} Org</td></tr>
                    <tr><td><b>Kerusakan</b></td><td>${laporan.kerusakan}</td></tr>
                    <tr><td><b>Penyebab</b></td><td>${laporan.penyebab}</td></tr>
                    <tr><td><b>Sumber Informasi</b></td><td>${laporan.sumberInformasi}</td></tr>
                </table>

                <h3>II. Komunikasi Pelaporan ke BPBD / BNPB</h3>
                <table class="log-table">
                    <tr>
                        <th width="15%">WAKTU</th>
                        <th width="25%">DARI (STATION ID)</th>
                        <th width="25%">TUJUAN (STATION ID)</th>
                        <th width="35%">SUBJEK / PESAN</th>
                    </tr>
                    <!-- Looping data log di sini -->
                    ${generateLogRows(listLog)}
                </table>

                <!-- Bagian Tanda Tangan -->
                <br><br>
                <table class="signature-table" width="100%">
                    <tr>
                        <td width="33%">
                            Manager Pusdalops-PB<br>BPBD Mukomuko<br><br><br><br>
                            <b><u>HITATUN</u></b><br>
                            NIP. 197401282006041006
                        </td>
                        <td width="33%">
                            Koordinator Administrasi<br>Pusdalops-PB<br><br><br><br>
                            <b><u>AAN FITRADINATA, SE</u></b><br>
                            NIP. 197903122006041007
                        </td>
                        <td width="33%">
                            Kepala Pelaksana<br>BPBD Mukomuko<br><br><br><br>
                            <b><u>RURI IRWANDI, ST.MT</u></b><br>
                            NIP. 197906072006041011
                        </td>
                    </tr>
                </table>
                ${buildFooterAtribusiResmiHtml()}
            </body>
            </html>
        """.trimIndent()

        // 2. Proses Mencetak ke PDF
        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                val printAdapter = view?.createPrintDocumentAdapter("Laporan_Harian_Pusdalops") ?: return

                val jobName = "Laporan_Harian_Pusdalops_${laporan.tanggalKejadian.replace("/", "-")}"
                printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
            }
        }
        webView.loadDataWithBaseURL("file:///android_res/drawable/", htmlContent, "text/HTML", "UTF-8", null)
    }

    // Fungsi bantuan untuk membuat baris tabel log secara dinamis
    private fun generateLogRows(logs: List<LogKomunikasiEntity>): String {
        if (logs.isEmpty()) return "<tr><td colspan='4' style='text-align:center;'>Tidak ada catatan komunikasi</td></tr>"

        val sb = StringBuilder()
        for (log in logs) {
            sb.append("""
                <tr>
                    <td>${log.waktu}</td>
                    <td>${log.stationIdFrom}</td>
                    <td>${log.stationIdTo}</td>
                    <td>${log.subjek}</td>
                </tr>
            """)
        }
        return sb.toString()
    }
}
