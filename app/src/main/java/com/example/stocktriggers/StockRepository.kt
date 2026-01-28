package com.example.stocktriggers

import android.util.Log
import kotlinx.coroutines.delay
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

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
