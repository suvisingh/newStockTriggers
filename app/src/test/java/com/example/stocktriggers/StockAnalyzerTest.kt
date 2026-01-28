package com.example.stocktriggers

import org.junit.Test
import org.junit.Assert.*

class StockAnalyzerTest {

    @Test
    fun testBuySignal() {
        // Mean needs to be higher than current by > 1.5%
        // Let's say Mean = 100.
        // Current needs to be < 98.5 (e.g., 98.0)
        // Diff = -2.0
        // % = -2.0% (which is < -1.5%) -> BUY

        val data = listOf(
            DailyClose(1, 100.0),
            DailyClose(2, 100.0),
            DailyClose(3, 100.0),
            DailyClose(4, 100.0),
            DailyClose(5, 100.0), // Mean of these 5 is 100.0
            DailyClose(6, 98.0)   // Price drops to 98.0
        )

        val result = StockAnalyzer.analyze(data)
        assertNotNull(result)
        assertEquals(100.0, result!!.mean, 0.01)
        assertEquals(-2.0, result.difference, 0.01)
        assertEquals(-2.0, result.percentageChange, 0.01)
        assertEquals(Signal.BUY, result.signal)
    }

    @Test
    fun testSellSignal() {
        // Mean = 100.
        // Current needs to be > 103 (e.g. 104) -> +4% -> SELL
        
        val data = listOf(
            DailyClose(1, 100.0),
            DailyClose(2, 100.0),
            DailyClose(3, 100.0),
            DailyClose(4, 100.0),
            DailyClose(5, 100.0), // Mean of these 5 is 100.0
            DailyClose(6, 104.0)  // Price jumps to 104.0
        )

        val result = StockAnalyzer.analyze(data)
        assertNotNull(result)
        assertEquals(100.0, result!!.mean, 0.01)
        assertEquals(4.0, result.difference, 0.01)
        assertEquals(4.0, result.percentageChange, 0.01)
        assertEquals(Signal.SELL, result.signal)
    }

    @Test
    fun testNeutralSignal() {
        // Small change
         val data = listOf(
            DailyClose(1, 100.0),
            DailyClose(2, 100.0),
            DailyClose(3, 100.0),
            DailyClose(4, 100.0),
            DailyClose(5, 100.0), 
            DailyClose(6, 101.0) // +1% -> Neutral
        )

        val result = StockAnalyzer.analyze(data)
        assertEquals(Signal.NEUTRAL, result!!.signal)
    }

    @Test
    fun testInsufficientData() {
        val data = listOf(
            DailyClose(1, 100.0)
        )
        val result = StockAnalyzer.analyze(data)
        assertNull(result)
    }
}
