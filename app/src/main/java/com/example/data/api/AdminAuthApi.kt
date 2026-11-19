package com.example.data.api

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit API Service khusus verifikasi otorisasi Administrator di sisi Server.
 *
 * - `POST /api/verify-admin-pass`: Memverifikasi kecocokan hash password master admin di database
 *   server dan mengembalikan Temporary Access Token (TTL 5–15 menit) bila valid.
 * - `POST /api/admin-audit-log`: Mengirimkan catatan audit log percobaan akses menu sensitif
 *   (Kelola Personil, Database Explorer, dll.) ke database server.
 */
interface AdminAuthApi {

    @POST("api/verify-admin-pass")
    suspend fun verifyAdminPassword(
        @Body payload: RequestBody
    ): Response<ResponseBody>

    @POST("api/admin-audit-log")
    suspend fun recordAdminAuditLog(
        @Body payload: RequestBody
    ): Response<ResponseBody>
}
