package com.example.stocktriggers

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit interface definition for the Yahoo Finance API.
 *
 * Intent:
 * Defines the HTTP endpoints and request structure for fetching stock market data.
 */
interface FinanceApi {
    
    /**
     * Fetches historical chart data for a specific stock symbol.
     *
     * @param symbol The stock ticker symbol (e.g., "^NSEI" for NIFTY 50).
     * @param interval The data granularity (default: "1d" for daily).
     * @param range The historical range to fetch (default: "10d" to ensure we have enough working days).
     * @return [StockData] The parsed API response.
     */
    @GET("v8/finance/chart/{symbol}")
    suspend fun getStockData(
        @retrofit2.http.Path("symbol") symbol: String,
        @Query("interval") interval: String = "1d",
        @Query("range") range: String = "10d" // Fetching more than 6 to be safe
    ): StockData
}
