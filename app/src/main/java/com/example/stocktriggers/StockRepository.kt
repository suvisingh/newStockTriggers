package com.example.stocktriggers

import android.util.Log
import kotlinx.coroutines.delay
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * StockRepository handles the retrieval of stock market data from remote and mock sources.
 *
 * Intent:
 * This class abstracts the data sourcing logic, providing a clean interface for the ViewModel
 * to get historical stock price data. It handles network communication via Retrofit and
 * provides fallback mock data in case of failures.
 *
 * Exposed APIs:
 * - [getLastWorkingDaysData]: Fetches the most recent daily closing prices for a given symbol.
 */
class StockRepository {

    private val api: FinanceApi
    private val TAG = "StockRepository"

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://query1.finance.yahoo.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        api = retrofit.create(FinanceApi::class.java)
    }

    /**
     * Fetches a specified number of recent daily closing prices for a stock symbol.
     *
     * This function attempts to fetch real-time data from Yahoo Finance. If the network call fails
     * or the data is malformed, it falls back to providing a set of mock data points.
     *
     * @param symbol The stock ticker symbol to fetch data for (e.g., "^NSEI", "AAPL").
     * @param limit The maximum number of daily data points to return (default is 6).
     * @return A List of [DailyClose] objects containing timestamp and closing price.
     *
     * Example:
     * ```
     * val data = repository.getLastWorkingDaysData("AAPL", 5)
     * // returns a list of the last 5 closing prices for Apple.
     * ```
     */
    suspend fun getLastWorkingDaysData(symbol: String, limit: Int = 6): List<DailyClose> {
        Log.d(TAG, "getLastWorkingDaysData called for symbol: $symbol")
        return try {
            val response = api.getStockData(symbol)
            Log.d(TAG, "API Response received for $symbol")
            
            val result = response.chart?.result?.firstOrNull()
            if (result == null) {
                Log.w(TAG, "API response result is null, falling back to mock")
                return getMockData()
            }
            
            val timestamps = result.timestamp
            val closes = result.indicators.quote.firstOrNull()?.close
            
            if (closes == null) {
                Log.w(TAG, "API response closes is null, falling back to mock")
                return getMockData()
            }
            
            val validData = mutableListOf<DailyClose>()
            for (i in timestamps.indices) {
                if (i < closes.size && closes[i] != null) {
                    validData.add(DailyClose(timestamps[i], closes[i]!!))
                }
            }
            
            Log.d(TAG, "Parsed ${validData.size} valid data points")
            validData.takeLast(limit)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching data for $symbol: ${e.message}", e)
            getMockData().takeLast(limit)
        }
    }

    /**
     * Generates a list of mock stock data points for development and fallback purposes.
     *
     * @return A List of [DailyClose] objects with hardcoded prices and relative timestamps.
     */
    private suspend fun getMockData(): List<DailyClose> {
        Log.d(TAG, "Providing mock data")
        delay(500)
        val baseTime = System.currentTimeMillis() / 1000
        val daySeconds = 86400
        
        return listOf(
            DailyClose(baseTime - 5 * daySeconds, 21500.0),
            DailyClose(baseTime - 4 * daySeconds, 21600.0),
            DailyClose(baseTime - 3 * daySeconds, 21550.0),
            DailyClose(baseTime - 2 * daySeconds, 21700.0),
            DailyClose(baseTime - 1 * daySeconds, 21800.0),
            DailyClose(baseTime, 22200.0)
        )
    }
}
