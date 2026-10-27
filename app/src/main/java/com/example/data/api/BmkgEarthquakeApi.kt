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
        val headerInterceptor = okhttp3.Interceptor { chain ->
            val original = chain.request()
            val request = original.newBuilder()
                .header("User-Agent", "SIPAGAR-KITA-Mukomuko/1.6.1 (Android; BPBD-PUSDALOPS-MMK; BMKG-Disaster-Client)")
                .header("Accept", "application/json, text/plain, */*")
                .header("Accept-Language", "id-ID,id;q=0.9,en-US;q=0.8,en;q=0.7")
                .header("Connection", "keep-alive")
                .method(original.method, original.body)
                .build()
            chain.proceed(request)
        }
        OkHttpClient.Builder()
            .addInterceptor(headerInterceptor)
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
