package com.example.util

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.data.local.LaporanHarianEntity
import com.example.data.local.LogKomunikasiEntity

object PdfGenerator {

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
                    body { font-family: Arial, sans-serif; font-size: 12px; margin: 20px; }
                    h2 { text-align: center; font-size: 16px; margin-bottom: 5px; }
                    p { text-align: justify; margin-bottom: 15px; }
                    h3 { font-size: 14px; margin-top: 20px; }
                    table { width: 100%; border-collapse: collapse; margin-top: 10px; margin-bottom: 20px; }
                    th, td { border: 1px solid black; padding: 8px; text-align: left; vertical-align: top; }
                    .no-border { border: none; }
                    .signature-table td { border: none; text-align: center; padding-top: 30px; }
                    .log-table th { background-color: #f2f2f2; text-align: center; }
                </style>
            </head>
            <body>
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
        webView.loadDataWithBaseURL(null, htmlContent, "text/HTML", "UTF-8", null)
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
