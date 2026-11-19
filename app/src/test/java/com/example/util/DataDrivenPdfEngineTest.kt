package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.test.core.app.ApplicationProvider
import com.example.data.api.DocumentTemplateApi
import com.example.data.local.LaporanHarianEntity
import com.example.data.repository.DocumentSettingsRepository
import com.example.model.DocumentSettings
import com.example.model.GelarFormat
import com.example.model.RemoteOfficerRecord
import com.example.model.RemoteSignatoryConfig
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Response

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DataDrivenPdfEngineTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        DocumentSettingsRepository.resetForTesting()
        DocumentSettingsRepository.disableFirestoreForTesting = true
        context.getSharedPreferences("bpbd_document_settings_cache", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @After
    fun tearDown() {
        DocumentSettingsRepository.resetForTesting()
    }

    @Test
    fun `parse GET document-settings JSON and clamp unsafe values`() {
        val serverJson = """
            {
              "fontSize": 10.5,
              "minFontSize": 6.0,
              "bodyFontSize": 8.5,
              "maxLengthBeforeTruncate": 95,
              "gelarFormat": "SHORT_DOT",
              "nipPrefix": "NIP.",
              "useRemotePdfTemplate": true,
              "footerText": "Dokumen Resmi BPBD Mukomuko - Versi Web Dashboard",
              "signatories": [
                {
                  "slotKey": "pengawas",
                  "roleLine1": "Kepala Pelaksana",
                  "roleLine2": "BPBD Kab. Mukomuko",
                  "nama": "Ruri Irwandi",
                  "gelarDepan": "Dr. Ir.",
                  "gelarBelakang": "S.T., M.T.",
                  "nip": "197906072006041011"
                }
              ],
              "officers": [
                {
                  "id": "off-01",
                  "nama": "Hitatun Arazak",
                  "gelarDepan": "H.",
                  "gelarBelakang": "S.Sos., M.Si.",
                  "nip": "197401282006041006",
                  "role": "Manajer Pusdalops-PB"
                }
              ]
            }
        """.trimIndent()

        val settings = DocumentSettings.fromJson(serverJson, source = DocumentSettings.SOURCE_API)
        assertNotNull(settings)
        settings!!

        assertEquals(10.5f, settings.fontSize, 0.01f)
        assertEquals(6.0f, settings.minFontSize, 0.01f)
        assertEquals(8.5f, settings.bodyFontSize, 0.01f)
        assertEquals(95, settings.maxLengthBeforeTruncate)
        assertEquals(GelarFormat.SHORT_DOT, settings.gelarFormat)
        assertEquals("Dokumen Resmi BPBD Mukomuko - Versi Web Dashboard", settings.footerText)
        assertEquals(1, settings.signatories.size)
        assertEquals(1, settings.officers.size)

        // Pastikan nilai ekstrem dari server di-clamp agar layout tetap aman
        val unsafeJson = """{"fontSize": 99.0, "minFontSize": 1.0, "maxLengthBeforeTruncate": 5}"""
        val clamped = DocumentSettings.fromJson(unsafeJson)!!
        assertEquals(DocumentSettings.MAX_ALLOWED_FONT_SIZE, clamped.fontSize, 0.01f)
        assertEquals(4.5f, clamped.minFontSize, 0.01f)
        assertEquals(32, clamped.maxLengthBeforeTruncate)
    }

    @Test
    fun `formatNameWithGelar respects all dynamic gelarFormat options from backend`() {
        val baseSettings = DocumentSettings()

        assertEquals(
            "Dr. Ir. Ruri Irwandi, S.T., M.T.",
            baseSettings.formatNameWithGelar("Ruri Irwandi", "Dr. Ir.", "S.T., M.T.", GelarFormat.FULL)
        )
        assertEquals(
            "Ruri Irwandi",
            baseSettings.formatNameWithGelar("Ruri Irwandi", "Dr. Ir.", "S.T., M.T.", GelarFormat.NO_TITLE)
        )
        assertEquals(
            "DR. IR. RURI IRWANDI, S.T., M.T.",
            baseSettings.formatNameWithGelar("Ruri Irwandi", "Dr. Ir.", "S.T., M.T.", GelarFormat.UPPERCASE)
        )
        assertEquals(
            "Dr Ir Ruri Irwandi, ST, MT",
            baseSettings.formatNameWithGelar("Ruri Irwandi", "Dr. Ir.", "S.T., M.T.", GelarFormat.SHORT_DOT)
        )
        assertEquals(
            "RURI IRWANDI",
            baseSettings.formatRawFullName("Dr. RURI IRWANDI, S.T., M.T.", GelarFormat.NO_TITLE)
        )
    }

    @Test
    fun `resolveSignatories and resolveOfficerIdentity pull dynamically from API data without hardcode`() = runBlocking {
        val fakeApi = FakeDocumentTemplateApi(
            settingsJson = """
                {
                  "fontSize": 9.5,
                  "minFontSize": 5.5,
                  "gelarFormat": "FULL",
                  "signatories": [
                    {
                      "slotKey": "koord_admin",
                      "roleLine1": "Koord. Admin Baru",
                      "roleLine2": "Pusdalops-PB",
                      "nama": "Budi Santoso",
                      "gelarBelakang": "S.E., M.M.",
                      "nip": "198501012010011001"
                    }
                  ]
                }
            """.trimIndent(),
            officersJson = """
                {
                  "officers": [
                    {
                      "id": "manajer-baru",
                      "nama": "Siti Rahma",
                      "gelarDepan": "Hj.",
                      "gelarBelakang": "S.KM., M.Kes.",
                      "nip": "198002022008012002",
                      "role": "Manajer Pusdalops-PB"
                    }
                  ]
                }
            """.trimIndent()
        )
        DocumentSettingsRepository.overrideApiForTesting = fakeApi

        val fetched = DocumentSettingsRepository.fetchDocumentSettings(context)
        assertEquals(DocumentSettings.SOURCE_API, fetched.source)
        assertEquals(1, fetched.officers.size)

        val signatories = DocumentSettingsRepository.resolveSignatories(context, fetched)
        assertEquals("Budi Santoso, S.E., M.M.", signatories[DocumentSettings.SLOT_KOORD_ADMIN]?.formattedName)
        assertEquals("NIP. 198501012010011001", signatories[DocumentSettings.SLOT_KOORD_ADMIN]?.nipLine)
        assertEquals("Hj. Siti Rahma, S.KM., M.Kes.", signatories[DocumentSettings.SLOT_MANAJER_PUSDALOPS]?.formattedName)

        val officer = DocumentSettingsRepository.resolveOfficerIdentity(
            context = context,
            rawNip = "198002022008012002",
            settings = fetched
        )
        assertEquals("Hj. Siti Rahma, S.KM., M.Kes.", officer.formattedName)
        assertEquals("NIP. 198002022008012002", officer.nipLine)
    }

    @Test
    fun `remote PDF template is returned first when backend responds with valid PDF bytes`() = runBlocking {
        val fakeRemotePdf = "%PDF-1.7\n1 0 obj\n<< /Type /Catalog >>\nendobj\n%%EOF".toByteArray()
        val fakeApi = FakeDocumentTemplateApi(
            settingsJson = """{"useRemotePdfTemplate": true, "fontSize": 9.0}""",
            remotePdfById = fakeRemotePdf
        )
        DocumentSettingsRepository.overrideApiForTesting = fakeApi

        val laporan = LaporanHarianEntity(
            id = 42,
            jenisBencana = "Banjir Bandang",
            tanggalKejadian = "18/11/2026",
            waktuKejadian = "08:30 WIB",
            lokasiBencana = "Kec. Kota Mukomuko"
        )

        val generated = PdfGenerator.generateLaporanPdfBytes(
            context = context,
            laporan = laporan,
            listLog = emptyList()
        )

        assertArrayEquals(fakeRemotePdf, generated)
        assertTrue(fakeApi.getReportPdfCalled)
    }

    @Test
    fun `offline or invalid remote PDF falls back cleanly to local PDF using cached GET document-settings`() = runBlocking {
        // Simpan cache konfigurasi terakhir saat online
        val cachedSettings = DocumentSettings(
            fontSize = 8.8f,
            minFontSize = 5.5f,
            maxLengthBeforeTruncate = 80,
            gelarFormat = GelarFormat.SHORT_DOT,
            useRemotePdfTemplate = true,
            footerText = "Footer Offline Cache BPBD",
            signatories = listOf(
                RemoteSignatoryConfig(
                    slotKey = DocumentSettings.SLOT_PENGAWAS,
                    roleLine1 = "Pengawas",
                    roleLine2 = "BPBD Mukomuko",
                    nama = "Pejabat Cache Offline",
                    gelarBelakang = "S.T., M.T.",
                    nip = "197906072006041011"
                )
            ),
            officers = listOf(
                RemoteOfficerRecord(
                    id = "off-cache",
                    nama = "Operator Cache",
                    gelarBelakang = "S.Kom.",
                    nip = "199001012015011005",
                    role = "Operator Pusdalops"
                )
            )
        )
        DocumentSettingsRepository.saveCachedSettings(context, cachedSettings)

        // Simulasikan API gagal / respons bukan PDF valid
        val failingApi = FakeDocumentTemplateApi(
            shouldFailSettings = true,
            remotePdfById = "<html>502 Bad Gateway</html>".toByteArray()
        )
        DocumentSettingsRepository.overrideApiForTesting = failingApi

        val fetched = DocumentSettingsRepository.fetchDocumentSettings(context)
        assertEquals(DocumentSettings.SOURCE_CACHE, fetched.source)
        assertEquals(80, fetched.maxLengthBeforeTruncate)
        assertEquals(GelarFormat.SHORT_DOT, fetched.gelarFormat)

        assertFalse(DocumentSettingsRepository.isValidPdfBytes("<html>502</html>".toByteArray()))

        val laporan = LaporanHarianEntity(
            id = 7,
            jenisBencana = "Tanah Longsor",
            tanggalKejadian = "18/11/2026",
            waktuKejadian = "10:00 WIB",
            lokasi = "Kec. V Koto",
            operatorName = "Operator Cache"
        )
        val remoteAttempt = DocumentSettingsRepository.fetchRemotePdfIfAvailable(
            context = context,
            laporan = laporan,
            listLog = emptyList(),
            listLogistik = emptyList(),
            settings = fetched
        )
        assertNull(remoteAttempt)

        // Fallback lokal merender Kop Surat, Blok Tanda Tangan Piramida Terbalik & Footer berbasis cached DocumentSettings
        val fallbackBmp = Bitmap.createBitmap(595, 842, Bitmap.Config.ARGB_8888)
        val fallbackCanvas = Canvas(fallbackBmp)
        val afterKopY = ReportExporter.drawKopSuratResmi(
            context = context,
            canvas = fallbackCanvas,
            pageWidth = 595,
            margin = 36f,
            pageNumber = 1,
            documentSettings = fetched
        )
        val afterSigY = ReportExporter.drawBlokTandaTanganPiramidaTerbalik(
            canvas = fallbackCanvas,
            pageWidth = 595,
            startY = afterKopY + 40f,
            margin = 36f,
            context = context,
            documentSettings = fetched
        )
        ReportExporter.drawFooterAtribusiResmi(
            canvas = fallbackCanvas,
            pageWidth = 595,
            pageHeight = 842,
            margin = 36f,
            documentSettings = fetched
        )
        assertTrue("Kop surat Y ($afterKopY) harus > 100", afterKopY > 100f)
        assertTrue("Blok tanda tangan Y ($afterSigY) harus melebihi posisi awal", afterSigY > afterKopY + 120f)
    }

    @Test
    fun `auto-shrink helper reduces font size for long names with academic titles without going below minFontSize`() {
        val shortName = "HITATUN ARAZAK"
        val mediumTitleName = "Dr. RURI IRWANDI, S.T., M.T., M.A.P."
        val veryLongTitleName = "Prof. Dr. Ir. H. Muhammad Abdurrahman Al-Mukomuko, S.T., M.T., M.A.P., IPU., ASEAN Eng."
        val signatureBoxWidth = 160f

        // 1. Uji deterministik dengan metrik proporsional huruf Latin (~0.58 * fontSize per karakter)
        val proportionalMeasurer: (String, Float) -> Float = { text, size -> text.length * size * 0.58f }

        val shortSize = PdfTextAutoShrinkHelper.computeAutoShrinkFontSize(
            text = shortName,
            maxWidth = signatureBoxWidth,
            initialFontSize = 10.0f,
            minFontSize = 5.5f,
            widthMeasurer = proportionalMeasurer
        )
        val mediumSize = PdfTextAutoShrinkHelper.computeAutoShrinkFontSize(
            text = mediumTitleName,
            maxWidth = signatureBoxWidth,
            initialFontSize = 10.0f,
            minFontSize = 5.5f,
            widthMeasurer = proportionalMeasurer
        )
        val longSize = PdfTextAutoShrinkHelper.computeAutoShrinkFontSize(
            text = veryLongTitleName,
            maxWidth = signatureBoxWidth,
            initialFontSize = 10.0f,
            minFontSize = 5.5f,
            widthMeasurer = proportionalMeasurer
        )

        assertEquals(10.0f, shortSize, 0.01f)
        assertTrue("Expected medium title font size ($mediumSize) to shrink between 5.5f and 10.0f", mediumSize in 5.5f..9.75f)
        assertTrue(
            "Expected medium title width at $mediumSize pt to fit inside $signatureBoxWidth pt",
            proportionalMeasurer(mediumTitleName, mediumSize) <= signatureBoxWidth
        )
        assertEquals(5.5f, longSize, 0.01f)

        // 2. Uji render langsung ke Android Canvas/Paint: tidak memutasi basePaint dan tetap dalam rentang aman
        val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 10.0f
            textAlign = Paint.Align.CENTER
        }
        val bmp = Bitmap.createBitmap(240, 60, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val tightBoxWidth = (basePaint.measureText(veryLongTitleName) * 0.6f).coerceAtLeast(10f)
        val drawnSize = PdfTextAutoShrinkHelper.drawAutoShrinkText(
            canvas = canvas,
            text = veryLongTitleName,
            x = 120f,
            y = 30f,
            maxWidth = tightBoxWidth,
            basePaint = basePaint,
            initialFontSize = 10.0f,
            minFontSize = 5.5f
        )
        assertTrue("Expected drawnSize ($drawnSize) in [5.5, 10.0]", drawnSize in 5.5f..10.0f)
        assertEquals(10.0f, basePaint.textSize, 0.01f)
    }

    private class FakeDocumentTemplateApi(
        private val settingsJson: String = "{}",
        private val officersJson: String = "[]",
        private val remotePdfById: ByteArray? = null,
        private val remotePdfByPost: ByteArray? = null,
        private val shouldFailSettings: Boolean = false
    ) : DocumentTemplateApi {

        var getReportPdfCalled: Boolean = false
            private set

        override suspend fun getDocumentSettings(): Response<ResponseBody> {
            if (shouldFailSettings) {
                return Response.error(503, "Offline".toResponseBody("text/plain".toMediaType()))
            }
            return Response.success(settingsJson.toResponseBody("application/json".toMediaType()))
        }

        override suspend fun getOfficers(): Response<ResponseBody> {
            return Response.success(officersJson.toResponseBody("application/json".toMediaType()))
        }

        override suspend fun downloadReportPdf(reportId: String): Response<ResponseBody> {
            getReportPdfCalled = true
            val bytes = remotePdfById
                ?: return Response.error(404, "Not Found".toResponseBody("text/plain".toMediaType()))
            return Response.success(bytes.toResponseBody("application/pdf".toMediaType()))
        }

        override suspend fun renderReportPdf(payload: RequestBody): Response<ResponseBody> {
            val bytes = remotePdfByPost
                ?: return Response.error(503, "Unavailable".toResponseBody("text/plain".toMediaType()))
            return Response.success(bytes.toResponseBody("application/pdf".toMediaType()))
        }
    }
}
