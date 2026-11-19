package com.example.data.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET

/**
 * Retrofit API Service untuk Direktori Kontak Darurat Publik (Tab "Kontak Penting").
 *
 * Endpoint: `GET /emergency-contacts`
 * Mengembalikan daftar JSON instansi layanan darurat publik (BPBD/117, Kepolisian/110,
 * Damkar/113, Ambulans/RSUD, Basarnas/115, dst.) agar admin dapat menambah atau mengubah
 * nomor penting dari server tanpa perlu merilis pembaruan aplikasi.
 */
interface EmergencyContactsApi {

    @GET("emergency-contacts")
    suspend fun getEmergencyContacts(): Response<ResponseBody>
}
