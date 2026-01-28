package com.example.stocktriggers

object StockAnalyzer {
    
    data class AnalysisResult(
        val mean: Double,
        val difference: Double,
        val percentageChange: Double,
        val signal: Signal
    )

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

        return AnalysisResult(mean, difference, percentageChange, signal)
    }
}
