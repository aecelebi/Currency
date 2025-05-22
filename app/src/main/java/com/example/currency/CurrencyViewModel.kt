package com.example.currency

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log
import kotlinx.coroutines.flow.*
import java.io.IOException
import retrofit2.HttpException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val DUMMY_RATES_VS_USD = mapOf(
    "USD" to 1.0, "TRY" to 32.50, "EUR" to 0.92, "JPY" to 150.55, "GBP" to 0.79,
    "AUD" to 1.52, "CAD" to 1.37, "CHF" to 0.90, "CNY" to 7.24, "SEK" to 10.45, "NZD" to 1.63
)

private val DUMMY_CURRENCY_INFO_ORDERED = listOf(
    CurrencyInfo("USD", "US Dollar"), CurrencyInfo("TRY", "Turkish Lira"), CurrencyInfo("EUR", "Euro"),
    CurrencyInfo("JPY", "Japanese Yen"), CurrencyInfo("GBP", "British Pound"),
    CurrencyInfo("AUD", "Australian Dollar"), CurrencyInfo("CAD", "Canadian Dollar"),
    CurrencyInfo("CHF", "Swiss Franc"), CurrencyInfo("CNY", "Chinese Yuan"),
    CurrencyInfo("SEK", "Swedish Krona"), CurrencyInfo("NZD", "New Zealand Dollar")
)
private val currencyInfoMap: Map<String, CurrencyInfo> =
    DUMMY_CURRENCY_INFO_ORDERED.associateBy { it.code }


private val DEFAULT_ORDER = DUMMY_CURRENCY_INFO_ORDERED.map { it.code }
private const val DEFAULT_BASE_CURRENCY = "USD"
private const val DEFAULT_BASE_AMOUNT = "1.00"


class CurrencyViewModel : ViewModel() {

    private val _baseCurrencyCode = MutableStateFlow(DEFAULT_BASE_CURRENCY)
    val baseCurrencyCode: StateFlow<String> = _baseCurrencyCode.asStateFlow()

    private val _baseAmountInput = MutableStateFlow(DEFAULT_BASE_AMOUNT)
    val baseAmountInput: StateFlow<String> = _baseAmountInput.asStateFlow()

    private val _currencies = MutableStateFlow<List<DisplayCurrency>>(emptyList())
    val currencies: StateFlow<List<DisplayCurrency>> = _currencies.asStateFlow()

    private var currentCurrencyOrder: List<String> = DEFAULT_ORDER

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val apiService: FrankfurterApiService = RetrofitClient.frankfurterApiService

    private val targetSymbols = listOf("USD", "EUR", "TRY", "GBP", "JPY", "AUD", "CAD", "CHF", "CNY", "SEK", "NZD")
    private val allTrackedSymbols: List<String>
        get() = (targetSymbols + _baseCurrencyCode.value).distinct()


    private val _historicalData = MutableStateFlow<Map<String, Double>?>(null)
    val historicalData: StateFlow<Map<String, Double>?> = _historicalData.asStateFlow()

    fun fetchHistoricalData(baseCurrency: String, targetCurrency: String) {
        viewModelScope.launch {
            _historicalData.value = null // Yükleniyor durumu
            Log.d("CurrencyViewModel", "fetchHistoricalData called for $baseCurrency to $targetCurrency")

            try {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

                val calendar = Calendar.getInstance()
                calendar.add(Calendar.DAY_OF_YEAR, -29)
                val startDateString = dateFormat.format(calendar.time)

                val dateRangeString = "$startDateString.."


                Log.d("CurrencyViewModel", "Requesting historical data for range: $dateRangeString, from: $baseCurrency, symbols: $targetCurrency")

                val response = apiService.getHistoricalRates(
                    dateRange = dateRangeString,
                    from = baseCurrency,
                    symbols = targetCurrency
                )

                if (response.rates != null) {
                    val ratesFromApi = response.rates
                    val processedRates = mutableMapOf<String, Double>()

                    ratesFromApi.forEach { (date, currencyRateMap) ->
                        currencyRateMap[targetCurrency]?.let { rate ->
                            processedRates[date] = rate
                        }
                    }

                    if (processedRates.isEmpty() && ratesFromApi.isNotEmpty()) {
                        Log.w("CurrencyViewModel", "Historical data received, but no rates found for target currency: $targetCurrency in rates: $ratesFromApi")
                        _historicalData.value = emptyMap()
                    } else {
                        _historicalData.value = processedRates
                        Log.d("CurrencyViewModel", "Historical data processed: ${processedRates.size} entries")
                    }
                } else {
                    Log.w("CurrencyViewModel", "Historical rates response is null or rates map is null. Base: ${response.base}, Start: ${response.startDate}, End: ${response.endDate}")
                    _historicalData.value = emptyMap()
                }

            } catch (e: Exception) {
                Log.e("CurrencyViewModel", "Error fetching historical data for $baseCurrency to $targetCurrency: ${e.message}", e)
                _historicalData.value = emptyMap()
            }
        }
    }

    fun clearHistoricalData() {
        _historicalData.value = null
        Log.d("CurrencyViewModel", "Historical data cleared.")
    }
    //


    init {
        viewModelScope.launch {
            combine(baseCurrencyCode, baseAmountInput) { baseCode, amountStr ->
                Pair(baseCode, amountStr)
            }.collectLatest { (baseCode, amountStr) ->
                fetchExchangeRatesAndUpdateUI(baseCode, amountStr)
            }
        }
    }

    private fun fetchExchangeRatesAndUpdateUI(baseCode: String, amountStr: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val symbolsToQuery = allTrackedSymbols.filter { it != baseCode }.joinToString(",")

                Log.d("CurrencyViewModel", "Fetching rates for base: $baseCode, to: $symbolsToQuery")

                val response = apiService.getLatestRates(
                    baseCurrency = baseCode,
                    symbols = if (symbolsToQuery.isNotEmpty()) symbolsToQuery else null
                )

                if (response.isSuccessful) {
                    val frankfurterResponse = response.body()
                    if (frankfurterResponse?.rates != null) {
                        Log.d("CurrencyViewModel", "Rates received: ${frankfurterResponse.rates}")
                        updateCurrenciesFromApiResponse(baseCode, frankfurterResponse.rates, amountStr)
                    } else {
                        _errorMessage.value = "API response is empty or invalid."
                        Log.e("CurrencyViewModel", "API Error: Empty or invalid response body. Code: ${response.code()}, Message: ${response.message()}")
                        _currencies.value = emptyList()
                    }
                } else {
                    _errorMessage.value = "API Error: ${response.code()} - ${response.message()}"
                    Log.e("CurrencyViewModel", "API Error: Code: ${response.code()}, Message: ${response.message()}")
                    _currencies.value = emptyList()
                }
            } catch (e: IOException) {
                _errorMessage.value = "Network error. Please check your connection."
                Log.e("CurrencyViewModel", "Network error", e)
                _currencies.value = emptyList()
            } catch (e: HttpException) {
                _errorMessage.value = "Server error: ${e.code()} - ${e.message()}."
                Log.e("CurrencyViewModel", "HTTP error", e)
                _currencies.value = emptyList()
            } catch (e: Exception) {
                _errorMessage.value = "An unexpected error occurred: ${e.localizedMessage}"
                Log.e("CurrencyViewModel", "Unexpected error", e)
                _currencies.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun updateCurrenciesFromApiResponse(
        currentBaseCode: String,
        apiRates: Map<String, Double>,
        amountStr: String
    ) {
        val amount = amountStr.toDoubleOrNull() ?: 1.0
        val newDisplayCurrencies = mutableListOf<DisplayCurrency>()

        currencyInfoMap[currentBaseCode]?.let { info ->
            newDisplayCurrencies.add(
                DisplayCurrency(
                    info = info,
                    relativeRate = 1.0,
                    calculatedAmount = amount * 1.0
                )
            )
        }

        apiRates.forEach { (currencyCode, rate) ->
            currencyInfoMap[currencyCode]?.let { info ->
                newDisplayCurrencies.add(
                    DisplayCurrency(
                        info = info,
                        relativeRate = rate,
                        calculatedAmount = amount * rate
                    )
                )
            }
        }


        val sortedCurrencies = mutableListOf<DisplayCurrency>()
        val currentOrder = _currencies.value.map { it.info.code }.ifEmpty { allTrackedSymbols }

        for (codeInOrder in currentOrder) {
            newDisplayCurrencies.find { it.info.code == codeInOrder }?.let {
                sortedCurrencies.add(it)
            }
        }
        newDisplayCurrencies.forEach { dc ->
            if (sortedCurrencies.none { it.info.code == dc.info.code }) {
                sortedCurrencies.add(dc)
            }
        }

        _currencies.value = sortedCurrencies.distinctBy { it.info.code }
        Log.d("CurrencyViewModel", "Updated UI currencies: ${_currencies.value.map { it.info.code to it.relativeRate }}")
    }


    fun onBaseCurrencySelected(newBaseCode: String) {
        if (_baseCurrencyCode.value != newBaseCode) {
            _baseCurrencyCode.value = newBaseCode

        }
    }

    fun onBaseAmountChanged(newAmountStr: String) {
        _baseAmountInput.value = newAmountStr
    }

    fun onCurrencyOrderChanged(newOrder: List<String>) {
        val currentMap = _currencies.value.associateBy { it.info.code }
        val reorderedList = newOrder.mapNotNull { currentMap[it] }
        _currencies.value = reorderedList
    }
}