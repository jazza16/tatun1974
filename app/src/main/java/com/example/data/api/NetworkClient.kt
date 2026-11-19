package com.example.data.api

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object NetworkClient {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
            }
        )
        .build()

    val weatherApi: OpenMeteoApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenMeteoApi::class.java)
    }

    val airQualityApi: AirQualityApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://air-quality-api.open-meteo.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(AirQualityApi::class.java)
    }

    val archiveApi: ArchiveWeatherApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://archive-api.open-meteo.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ArchiveWeatherApi::class.java)
    }

    /**
     * Membuat instance Retrofit untuk layanan Remote PDF Template & `GET /document-settings`.
     * URL dasar diambil dari konfigurasi BuildConfig/Secrets tanpa hardcode di kode sumber.
     */
    fun createDocumentTemplateApi(
        baseUrl: String,
        timeoutMs: Long = 8_000L
    ): DocumentTemplateApi {
        val normalizedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val clampedTimeoutMs = timeoutMs.coerceIn(1_500L, 30_000L)
        val client = okHttpClient.newBuilder()
            .connectTimeout(clampedTimeoutMs, TimeUnit.MILLISECONDS)
            .readTimeout(clampedTimeoutMs, TimeUnit.MILLISECONDS)
            .writeTimeout(clampedTimeoutMs, TimeUnit.MILLISECONDS)
            .build()
        return Retrofit.Builder()
            .baseUrl(normalizedBaseUrl)
            .client(client)
            .build()
            .create(DocumentTemplateApi::class.java)
    }

    /**
     * Membuat instance Retrofit untuk layanan dinamis `GET /emergency-contacts`.
     */
    fun createEmergencyContactsApi(
        baseUrl: String,
        timeoutMs: Long = 5_000L
    ): EmergencyContactsApi {
        val normalizedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val clampedTimeoutMs = timeoutMs.coerceIn(1_500L, 20_000L)
        val client = okHttpClient.newBuilder()
            .connectTimeout(clampedTimeoutMs, TimeUnit.MILLISECONDS)
            .readTimeout(clampedTimeoutMs, TimeUnit.MILLISECONDS)
            .build()
        return Retrofit.Builder()
            .baseUrl(normalizedBaseUrl)
            .client(client)
            .build()
            .create(EmergencyContactsApi::class.java)
    }

    /**
     * Membuat instance Retrofit untuk verifikasi server-side `POST /api/verify-admin-pass`
     * dan pencatatan `POST /api/admin-audit-log`.
     */
    fun createAdminAuthApi(
        baseUrl: String,
        timeoutMs: Long = 8_000L
    ): AdminAuthApi {
        val normalizedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val clampedTimeoutMs = timeoutMs.coerceIn(1_500L, 25_000L)
        val client = okHttpClient.newBuilder()
            .connectTimeout(clampedTimeoutMs, TimeUnit.MILLISECONDS)
            .readTimeout(clampedTimeoutMs, TimeUnit.MILLISECONDS)
            .writeTimeout(clampedTimeoutMs, TimeUnit.MILLISECONDS)
            .build()
        return Retrofit.Builder()
            .baseUrl(normalizedBaseUrl)
            .client(client)
            .build()
            .create(AdminAuthApi::class.java)
    }
}



