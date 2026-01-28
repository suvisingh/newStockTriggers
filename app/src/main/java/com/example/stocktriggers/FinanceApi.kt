package com.example.stocktriggers

import retrofit2.http.GET
import retrofit2.http.Query

interface FinanceApi {
    // Example endpoint structure (using Yahoo Finance style as a reference for structure)
    // https://query1.finance.yahoo.com/v8/finance/chart/^NSEI?interval=1d&range=1mo
    @GET("v8/finance/chart/{symbol}")
    suspend fun getStockData(
        @retrofit2.http.Path("symbol") symbol: String,
        @Query("interval") interval: String = "1d",
        @Query("range") range: String = "10d" // Fetching more than 6 to be safe
    ): StockData
}
