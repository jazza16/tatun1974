package com.example.data.api

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.POST
import retrofit2.http.Streaming

/**
 * Retrofit API Service untuk arsitektur Dokumen PDF Data-Driven BPBD Kabupaten Mukomuko:
 * 1. `GET /document-settings` -> Mengambil konfigurasi layout & format gelar JSON dari Dashboard Web Admin.
 * 2. `GET /officers` -> Mengambil daftar Nama, Gelar, Jabatan, dan NIP Petugas langsung dari server.
 * 3. `GET /reports/{reportId}/pdf` -> Mengunduh file PDF yang telah di-render oleh Backend berdasarkan ID laporan.
 * 4. `POST /reports/render-pdf` -> Meminta Backend merender PDF untuk laporan baru/draft dengan template server terbaru.
 */
interface DocumentTemplateApi {

    @GET("document-settings")
    suspend fun getDocumentSettings(): Response<ResponseBody>

    @GET("officers")
    suspend fun getOfficers(): Response<ResponseBody>

    @Streaming
    @GET("reports/{reportId}/pdf")
    suspend fun downloadReportPdf(
        @Path("reportId") reportId: String
    ): Response<ResponseBody>

    @Streaming
    @POST("reports/render-pdf")
    suspend fun renderReportPdf(
        @Body payload: RequestBody
    ): Response<ResponseBody>
}
