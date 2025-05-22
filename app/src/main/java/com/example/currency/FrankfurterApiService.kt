package com.example.currency

import com.example.currency.FrankfurterLatestResponse
import com.example.currency.HistoricalRatesResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Path

interface FrankfurterApiService {

    @GET("latest")
    suspend fun getLatestRates(
        @Query("from") baseCurrency: String,
        @Query("to") symbols: String? = null
    ): Response<FrankfurterLatestResponse>

    @GET("{dateRange}")
    suspend fun getHistoricalRates(
        @retrofit2.http.Path("dateRange", encoded = true) dateRange: String,
        @Query("from") from: String,
        @Query("symbols") symbols: String
    ): HistoricalRatesResponse
}