package com.example.stocktriggers

import com.google.gson.annotations.SerializedName

/**
 * Serializable data class for the Yahoo Finance API response.
 *
 * Intent:
 * Root object for parsing the JSON response from the chart API.
 */
data class StockData(
    @SerializedName("chart")
    val chart: ChartData? = null
)

/**
 * Data class representing the chart result container.
 */
data class ChartData(
    @SerializedName("result")
    val result: List<ChartResult>? = null,
    @SerializedName("error")
    val error: Any? = null
)

/**
 * Data class containing the timestamp and indicators for the stock.
 */
data class ChartResult(
    @SerializedName("meta")
    val meta: MetaData,
    @SerializedName("timestamp")
    val timestamp: List<Long>,
    @SerializedName("indicators")
    val indicators: Indicators
)

/**
 * Metadata about the stock query response.
 */
data class MetaData(
    @SerializedName("currency")
    val currency: String,
    @SerializedName("symbol")
    val symbol: String,
    @SerializedName("regularMarketPrice")
    val regularMarketPrice: Double
)

/**
 * Wrapper for technical indicators in the response.
 */
data class Indicators(
    @SerializedName("quote")
    val quote: List<Quote>
)

/**
 * Wrapper for the quote values (OHLC). We only use 'close' prices.
 */
data class Quote(
    @SerializedName("close")
    val close: List<Double?>
)

/**
 * Simplified domain model representing a single day's closing price.
 *
 * Intent:
 * Provides a clean, non-nullable structure for the UI and business logic code to use,
 * decoupling it from the raw API response format.
 *
 * @property timestamp The time of the data point in seconds.
 * @property close The closing price.
 */
data class DailyClose(
    val timestamp: Long,
    val close: Double
)

/**
 * Enum representing the calculated trading signal.
 */
enum class Signal {
    BUY, SELL, NEUTRAL
}

/**
 * Sealed interface representing the UI state of the stock data screen.
 *
 * Intent:
 * Follows the UDF (Unidirectional Data Flow) pattern to represent the view's state.
 */
sealed class StockUiState {
    /** State for when data is being fetched. */
    object Loading : StockUiState()
    
    /** 
     * State for when data has been successfully fetched and analyzed.
     * Contains all necessary metrics for the UI.
     */
    data class Success(
        val symbol: String,
        val data: List<DailyClose>,
        val currentPrice: Double,
        val meanPrice: Double,
        val difference: Double,
        val percentageChange: Double,
        val signal: Signal
    ) : StockUiState()
    
    /** State for when an error occurs during fetch or analysis. */
    data class Error(val message: String) : StockUiState()
}
