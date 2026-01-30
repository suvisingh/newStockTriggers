package com.example.stocktriggers

/**
 * StockAnalyzer is a utility object that performs technical analysis on stock price data.
 *
 * Intent:
 * This object encapsulates the logic for calculating moving averages, differences,
 * percentage changes, and generating trading signals based on historical closing prices.
 *
 * Exposed APIs:
 * - [analyze]: Takes a list of daily closing prices and returns an [AnalysisResult].
 */
object StockAnalyzer {
    
    /**
     * Data class containing the calculated metrics from a stock analysis.
     *
     * @property mean The calculated average of the previous 5 working days.
     * @property currentPrice The most recent closing price.
     * @property difference The absolute difference between current price and the mean.
     * @property percentageChange The percentage difference relative to the mean.
     * @property signal The trading [Signal] derived from the percentage change.
     */
    data class AnalysisResult(
        val mean: Double,
        val currentPrice: Double,
        val difference: Double,
        val percentageChange: Double,
        val signal: Signal
    )

    /**
     * Analyzes a list of stock data to calculate trading metrics and signals.
     *
     * Requires exactly 6 data points to perform a 5-day mean analysis against the current price.
     *
     * @param data A list of [DailyClose] objects. Should ideally contain at least 6 points.
     * @return An [AnalysisResult] if enough data is available, or null if the input list is too short.
     *
     * Example:
     * ```
     * val result = StockAnalyzer.analyze(stockDataList)
     * if (result != null) {
     *     println("Signal: ${result.signal}")
     * }
     * ```
     */
    fun analyze(data: List<DailyClose>): AnalysisResult? {
        // We need at least 6 data points: 5 for mean, 1 for current
        if (data.size < 6) return null

        val currentData = data.last()
        val previous5Days = data.takeLast(6).dropLast(1) 

        val mean = previous5Days.map { it.close }.average()
        val currentPrice = currentData.close
        val difference = currentPrice - mean
         // Avoid division by zero if mean is 0 (unlikely for stock, but good practice)
        val percentageChange = if (mean != 0.0) (difference / mean) * 100 else 0.0

        val signal = when {
            percentageChange < -1.5 -> Signal.BUY
            percentageChange > 3.0 -> Signal.SELL
            else -> Signal.NEUTRAL
        }

        return AnalysisResult(mean, currentPrice, difference, percentageChange, signal)
    }
}
