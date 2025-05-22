package com.example.currency

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.example.currency.FrankfurterApiService // Servis arayüzünü import et

object RetrofitClient {

    private const val BASE_URL = "https://api.frankfurter.app/"

    // Lazy initialization ile sadece gerektiğinde oluşturulur
    val frankfurterApiService: FrankfurterApiService by lazy {
        // Logging Interceptor (Geliştirme sırasında ağ isteklerini görmek için)
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY // BODY: Request ve response detaylarını gösterir
        }

        // OkHttpClient
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor) // Geliştirme için loglamayı ekle
            // .connectTimeout(30, TimeUnit.SECONDS) // Opsiyonel: Bağlantı zaman aşımı
            // .readTimeout(30, TimeUnit.SECONDS)    // Opsiyonel: Okuma zaman aşımı
            .build()

        // Retrofit Builder
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient) // Özel OkHttpClient'ı kullan
            .addConverterFactory(GsonConverterFactory.create()) // JSON'ı Kotlin objelerine dönüştür
            .build()
            .create(FrankfurterApiService::class.java) // Servis arayüzünü implemente et
    }
}