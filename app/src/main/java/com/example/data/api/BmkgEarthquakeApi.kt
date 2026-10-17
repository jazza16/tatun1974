package com.example.data.api

import com.example.model.BmkgAutoGempaResponse
import com.example.model.BmkgRecentGempaResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import java.util.concurrent.TimeUnit

interface BmkgEarthquakeApi {
    @GET("autogempa.json")
    suspend fun getAutoGempa(): BmkgAutoGempaResponse

    @GET("gempaterkini.json")
    suspend fun getRecentEarthquakes(): BmkgRecentGempaResponse

    @GET("gempadirasakan.json")
    suspend fun getFeltEarthquakes(): BmkgRecentGempaResponse
}

object BmkgNetworkClient {
    private const val BASE_URL = "https://data.bmkg.go.id/DataMKG/TEWS/"

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    val api: BmkgEarthquakeApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(BmkgEarthquakeApi::class.java)
    }
}
