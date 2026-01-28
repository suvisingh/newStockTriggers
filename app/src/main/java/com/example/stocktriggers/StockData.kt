package com.example.stocktriggers

import com.google.gson.annotations.SerializedName

data class StockData(
    @SerializedName("chart")
    val chart: ChartData? = null
)

data class ChartData(
    @SerializedName("result")
    val result: List<ChartResult>? = null,
    @SerializedName("error")
    val error: Any? = null
)

data class ChartResult(
    @SerializedName("meta")
    val meta: MetaData,
    @SerializedName("timestamp")
    val timestamp: List<Long>,
    @SerializedName("indicators")
    val indicators: Indicators
)

data class MetaData(
    @SerializedName("currency")
    val currency: String,
    @SerializedName("symbol")
    val symbol: String,
    @SerializedName("regularMarketPrice")
    val regularMarketPrice: Double
)

data class Indicators(
    @SerializedName("quote")
    val quote: List<Quote>
)

data class Quote(
    @SerializedName("close")
    val close: List<Double?>
)

// Simplified model for UI
data class DailyClose(
    val timestamp: Long,
    val close: Double
)
