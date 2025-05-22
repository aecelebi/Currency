package com.example.currency

import com.google.gson.annotations.SerializedName

data class FrankfurterLatestResponse(
    @SerializedName("amount")
    val amount: Double?,
    @SerializedName("base")
    val base: String?,
    @SerializedName("date")
    val date: String?,
    @SerializedName("rates")
    val rates: Map<String, Double>?
)