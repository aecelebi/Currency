package com.example.currency

data class DisplayCurrency(
    val info: CurrencyInfo,
    val relativeRate: Double?,
    val calculatedAmount: Double?
)
