package com.example.currency

import android.util.Log
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.FloatEntry
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable
import java.text.SimpleDateFormat
import kotlin.math.roundToInt
import com.patrykandpatrick.vico.compose.style.currentChartStyle
import com.patrykandpatrick.vico.core.chart.line.LineChart
import com.patrykandpatrick.vico.core.chart.values.AxisValuesOverrider
import com.patrykandpatrick.vico.core.component.text.textComponent
import java.util.Date
import java.util.Locale
import kotlin.math.min

fun getFlagEmojiForCurrency(currencyCode: String): String {
    return when (currencyCode) {
        "USD" -> "🇺🇸"
        "TRY" -> "🇹🇷"
        "EUR" -> "🇪🇺"
        "JPY" -> "🇯🇵"
        "GBP" -> "🇬🇧"
        "AUD" -> "🇦🇺"
        "CAD" -> "🇨🇦"
        "CHF" -> "🇨🇭"
        "CNY" -> "🇨🇳"
        "SEK" -> "🇸🇪"
        "NZD" -> "🇳🇿"
        else -> "🏳️"
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CurrencyListScreen(currencyViewModel: CurrencyViewModel = viewModel()) {
    val baseCurrencyCode by currencyViewModel.baseCurrencyCode.collectAsState()
    val baseAmountInput by currencyViewModel.baseAmountInput.collectAsState()
    val currencies by currencyViewModel.currencies.collectAsState()
    val historicalData by currencyViewModel.historicalData.collectAsState()

    val reorderableState = rememberReorderableLazyListState(
        onMove = { from, to ->
            val currentCodes = currencies.map { it.info.code }.toMutableList()
            val movedItem = currentCodes.removeAt(from.index)
            currentCodes.add(to.index, movedItem)
            currencyViewModel.onCurrencyOrderChanged(currentCodes)
        }
    )

    val isLoading by currencyViewModel.isLoading.collectAsState()
    val errorMessage by currencyViewModel.errorMessage.collectAsState()

    var showHistoryPopupForCurrency by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Currency Converter") }
                  )
                Text(
                    text = "Rates updated daily around 16:00 CET.",
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(vertical = 4.dp, horizontal = 16.dp)
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            BaseCurrencySection(
                baseCurrencyCode = baseCurrencyCode,
                baseAmountInput = baseAmountInput,
                onAmountChange = { newAmount ->
                    currencyViewModel.onBaseAmountChanged(newAmount)
                },
                onBaseCurrencyClick = {
                    Log.d("CurrencyListScreen", "Base currency section clicked")
                }
            )
            Spacer(modifier = Modifier.height(16.dp))

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    isLoading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    errorMessage != null -> {
                        Text(
                            text = "Error: $errorMessage \n\nPull down to refresh.",
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp)
                        )
                    }
                    currencies.isEmpty() && !isLoading -> {
                        Text(
                            text = "No currency data available. \nPull down to refresh.",
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp)
                        )
                    }
                    else -> {
                        LazyColumn(
                            state = reorderableState.listState,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(Modifier.reorderable(reorderableState))
                        ) {
                            items(
                                items = currencies,
                                key = { it.info.code }
                            ) { displayCurrency ->
                                ReorderableItem(
                                    reorderableState = reorderableState,
                                    key = displayCurrency.info.code
                                ) { isDragging ->
                                    CurrencyRowItem(
                                        displayCurrency = displayCurrency,
                                        isBaseCurrency = displayCurrency.info.code == baseCurrencyCode,
                                        onItemClick = {
                                            currencyViewModel.onBaseCurrencySelected(displayCurrency.info.code)
                                        },
                                        onHistoryClick = { currencyCode ->
                                            currencyViewModel.fetchHistoricalData(baseCurrencyCode, currencyCode)
                                            showHistoryPopupForCurrency = currencyCode
                                            Log.d("CurrencyListScreen", "History icon clicked for: $currencyCode, base: $baseCurrencyCode")
                                        },
                                        isDragging = isDragging,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .detectReorderAfterLongPress(reorderableState)
                                            .animateItemPlacement()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    showHistoryPopupForCurrency?.let { targetCurrencyCode ->
        HistoricalChartPopup(
            historicalData = historicalData,
            baseCurrency = baseCurrencyCode,
            targetCurrency = targetCurrencyCode,
            onDismissRequest = {
                showHistoryPopupForCurrency = null
                currencyViewModel.clearHistoricalData()
            }
        )
    }
}

@Composable
fun BaseCurrencySection(
    baseCurrencyCode: String,
    baseAmountInput: String,
    onAmountChange: (String) -> Unit,
    onBaseCurrencyClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onBaseCurrencyClick)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = getFlagEmojiForCurrency(baseCurrencyCode),
                    fontSize = 28.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Column {
                    Text(
                        text = "Base Currency",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = baseCurrencyCode,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            OutlinedTextField(
                value = baseAmountInput,
                onValueChange = onAmountChange,
                label = { Text("Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(130.dp),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(fontSize = 18.sp)
            )
        }
    }
}


@Composable
fun HistoricalChartPopup(
    historicalData: Map<String, Double>?,
    baseCurrency: String,
    targetCurrency: String,
    onDismissRequest: () -> Unit
) {
    val chartEntryModelProducer = remember { ChartEntryModelProducer() }
    val serverDateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val displayDateFormat = remember { SimpleDateFormat("dd/MM", Locale.getDefault()) }

    var processedChartData by remember { mutableStateOf<Pair<List<FloatEntry>, List<Date>>?>(null) }
    var yAxisMin by remember { mutableStateOf<Float?>(null) }
    var yAxisMax by remember { mutableStateOf<Float?>(null) }

    LaunchedEffect(historicalData) {
        Log.d("ChartPopup", "Historical data changed: ${historicalData?.size} entries")
        if (historicalData != null && historicalData.isNotEmpty()) {
            val parsedEntries = historicalData.entries
                .mapNotNull { entry ->
                    try {
                        val date = serverDateFormat.parse(entry.key)
                        val rateValue = entry.value
                        date?.let { d -> Triple(d, 0f, rateValue.toFloat()) }
                    } catch (e: Exception) {
                        Log.e("ChartPopup", "Error parsing entry: ${entry.key} -> ${entry.value}", e)
                        null
                    }
                }
                .sortedBy { it.first } // Tarihe göre sırala

            val floatEntries = parsedEntries.mapIndexed { index, triple ->
                FloatEntry(index.toFloat(), triple.third)
            }
            val dates = parsedEntries.map { it.first }

            if (floatEntries.isNotEmpty()) {
                Log.d("ChartPopup", "Processed ${floatEntries.size} entries for chart.")
                chartEntryModelProducer.setEntries(floatEntries)
                processedChartData = Pair(floatEntries, dates)

                // Y ekseni için dinamik min/max hesapla
                val rates = floatEntries.map { it.y }
                val dataMin = rates.minOrNull()
                val dataMax = rates.maxOrNull()

                if (dataMin != null && dataMax != null) {
                    val difference = dataMax - dataMin
                    val padding = if (difference < (dataMax * 0.02f)) {
                        (dataMax * 0.02f).coerceAtLeast(0.001f)
                    } else {
                        difference * 0.1f // %10 pay (daha büyük farklar için)
                    }

                    yAxisMin = (dataMin - padding).coerceAtLeast(0f)
                    yAxisMax = dataMax + padding

                    if ((yAxisMax!! - yAxisMin!!) < (dataMax * 0.01f)) {
                        yAxisMin = (dataMin * 0.98f).coerceAtLeast(0f)
                        yAxisMax = dataMax * 1.02f
                    }
                    if (yAxisMin == yAxisMax) {
                        yAxisMin = yAxisMin!! * 0.98f // %2 aşağı
                        yAxisMax = yAxisMax!! * 1.02f // %2 yukarı
                        if (yAxisMin == 0f && yAxisMax == 0f && dataMax > 0f) {
                            yAxisMax = dataMax * 2f
                        }
                    }


                    Log.d("ChartPopup", "Y-Axis calculated: Min=$yAxisMin, Max=$yAxisMax (DataMin=$dataMin, DataMax=$dataMax, Padding=$padding)")
                } else {
                    yAxisMin = null
                    yAxisMax = null
                }
            } else {
                Log.d("ChartPopup", "No valid entries after processing. Clearing chart model.")
                chartEntryModelProducer.setEntries(emptyList<FloatEntry>())
                processedChartData = null
                yAxisMin = null
                yAxisMax = null
            }
        } else {
            Log.d("ChartPopup", "Historical data is null or empty. Clearing chart model.")
            chartEntryModelProducer.setEntries(emptyList<FloatEntry>())
            processedChartData = null
            yAxisMin = null
            yAxisMax = null
        }
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.65f),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$baseCurrency / $targetCurrency Graph (Last 30 Days)",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when {
                        historicalData == null && processedChartData == null -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(modifier = Modifier.padding(bottom = 8.dp))
                                Text("Loading historical data...")
                            }
                        }
                        (historicalData != null && historicalData.isEmpty()) || (processedChartData != null && processedChartData!!.first.isEmpty()) -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No historical data found for this parity",
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 32.dp)
                                )
                            }
                        }
                        processedChartData != null && processedChartData!!.first.isNotEmpty() -> {
                            val currentChartEntries = processedChartData!!.first
                            val currentDates = processedChartData!!.second

                            val bottomAxisValueFormatter = AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
                                val entryIndex = value.roundToInt()
                                if (entryIndex >= 0 && entryIndex < currentDates.size) {
                                    val maxLabels = 5
                                    val step = (currentDates.size / maxLabels).coerceAtLeast(1)
                                    if (entryIndex % step == 0 || entryIndex == 0 || entryIndex == currentDates.size -1) {
                                        displayDateFormat.format(currentDates[entryIndex])
                                    } else {
                                        ""
                                    }
                                } else {
                                    ""
                                }
                            }

                            val startAxis = rememberStartAxis(
                                label = textComponent {
                                    color = currentChartStyle.axis.axisLabelColor.toArgb()
                                    textSizeSp = 10f
                                },
                                valueFormatter = { value, _ -> "%.4f".format(value) },
                            )

                            Chart(
                                chart = lineChart(
                                    lines = listOf(
                                        LineChart.LineSpec(
                                            lineColor = MaterialTheme.colorScheme.primary.toArgb(),
                                            lineThicknessDp = 2f
                                        )
                                    ),
                                    axisValuesOverrider = AxisValuesOverrider.adaptiveYValues(
                                        yFraction = 1.15f,
                                    )
                                ),
                                chartModelProducer = chartEntryModelProducer,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(vertical = 8.dp),
                                startAxis = startAxis,
                                bottomAxis = rememberBottomAxis(
                                    valueFormatter = bottomAxisValueFormatter,
                                    guideline = null
                                )
                            )
                        }
                        else -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(modifier = Modifier.padding(bottom = 8.dp))
                                Text("Loading data...")
                            }
                        }
                    }
                } // Box sonu

                // Min/Max değerlerini gösteren Text
                if (processedChartData != null && processedChartData!!.first.isNotEmpty()) {
                    val rates = processedChartData!!.first.map { it.y.toDouble() }
                    val minRate = rates.minOrNull()
                    val maxRate = rates.maxOrNull()
                    Spacer(modifier = Modifier.height(8.dp))
                    if (minRate != null && maxRate != null) {
                        Text(
                            "Min: ${"%.4f".format(minRate)} - Max: ${"%.4f".format(maxRate)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onDismissRequest) {
                    Text("Close")
                }
            }
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CurrencyRowItem(
    displayCurrency: DisplayCurrency,
    isBaseCurrency: Boolean,
    onItemClick: () -> Unit,
    onHistoryClick: (currencyCode: String) -> Unit,
    modifier: Modifier = Modifier,
    isDragging: Boolean = false
) {
    val elevation by animateDpAsState(if (isDragging) 8.dp else (if (isBaseCurrency) 4.dp else 2.dp), label = "elevation_animation")

    Card(
        modifier = modifier.shadow(elevation = elevation, shape = MaterialTheme.shapes.medium),
        colors = CardDefaults.cardColors(
            containerColor = if (isBaseCurrency) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.DragIndicator,
                contentDescription = "Reorder item",
                modifier = Modifier
                    .padding(start = 16.dp, end = 8.dp)
                    .size(24.dp)
                    .align(Alignment.CenterVertically),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onItemClick() }
                    .padding(start = 8.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = getFlagEmojiForCurrency(displayCurrency.info.code),
                    fontSize = 28.sp,
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .align(Alignment.CenterVertically)
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = displayCurrency.info.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = displayCurrency.info.code,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = displayCurrency.relativeRate?.let { "%.4f".format(Locale.US, it) } ?: "---",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = displayCurrency.calculatedAmount?.let { "%.2f".format(Locale.US, it) } ?: "---",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (!isBaseCurrency) {
                IconButton(
                    onClick = { onHistoryClick(displayCurrency.info.code) },
                    modifier = Modifier.padding(start = 4.dp, end = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.History,
                        contentDescription = "View history for ${displayCurrency.info.code}",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                Spacer(modifier = Modifier
                    .padding(start = 4.dp, end = 8.dp)
                    .width(48.dp))
            }
        }
    }
}
@Preview(showBackground = true, name = "Currency List Screen Preview")
@Composable
fun CurrencyListScreenPreview() {
    MaterialTheme {
        CurrencyListScreen(currencyViewModel = viewModel())
    }
}

@Preview(showBackground = true)
@Composable
fun CurrencyRowItemPreview() {
    MaterialTheme {
        Column {
            CurrencyRowItem(
                displayCurrency = DisplayCurrency(
                    info = CurrencyInfo("USD", "US Dollar"),
                    relativeRate = 1.0,
                    calculatedAmount = 100.00
                ),
                isBaseCurrency = true,
                onHistoryClick = {},
                onItemClick = {}
            )
            Spacer(Modifier.height(8.dp))
            CurrencyRowItem(
                displayCurrency = DisplayCurrency(
                    info = CurrencyInfo("EUR", "Euro"),
                    relativeRate = 0.92,
                    calculatedAmount = 92.00
                ),
                isBaseCurrency = false,
                onHistoryClick = {},
                onItemClick = {}
            )
        }
    }
}