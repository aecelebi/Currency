package com.example.currency

import com.google.gson.annotations.SerializedName


data class HistoricalRatesResponse(
    @SerializedName("base")
    val base: String?,
    @SerializedName("start_date")
    val startDate: String?,
    @SerializedName("end_date")
    val endDate: String?,
    @SerializedName("rates")
    val rates: Map<String, Map<String, Double>>?
)
