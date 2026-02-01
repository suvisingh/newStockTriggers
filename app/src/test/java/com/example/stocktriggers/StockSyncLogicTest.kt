package com.example.stocktriggers

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class StockSyncLogicTest {

    // Helper to simulate the shouldNotify logic from StockSyncWorker
    // Since shouldNotify is private and depends on system time, we replicate the logic here for testing
    // In a real scenario, we would inject a Clock or TimeProvider dependency.
    // Helper to simulate the shouldNotify logic from StockSyncWorker
    // Modified to accept dayOfWeek for testing weekend logic
    private fun shouldNotifySimulated(currentHour: Int, currentMinute: Int, dayOfWeek: Int = Calendar.MONDAY): Boolean {
        val NOTIFICATION_TIMES = listOf(
            Pair(11, 0),
            Pair(14, 0)
        )

        // 1. Check for Weekend
        if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
            return false
        }
        
        val currentTotalMinutes = currentHour * 60 + currentMinute

        for ((hour, minute) in NOTIFICATION_TIMES) {
            val scheduledTotalMinutes = hour * 60 + minute
            if (currentTotalMinutes in scheduledTotalMinutes..(scheduledTotalMinutes + 30)) {
                return true
            }
        }
        return false
    }

    @Test
    fun `test notification allowed at exact scheduled time 11 AM`() {
        assertTrue(shouldNotifySimulated(11, 0))
    }

    @Test
    fun `test notification allowed within 30 mins after 11 AM`() {
        assertTrue(shouldNotifySimulated(11, 15))
        assertTrue(shouldNotifySimulated(11, 30))
    }

    @Test
    fun `test notification allowed at exact scheduled time 2 PM`() {
        assertTrue(shouldNotifySimulated(14, 0))
    }

    @Test
    fun `test notification blocked before 11 AM`() {
        assertFalse(shouldNotifySimulated(10, 59))
    }

    @Test
    fun `test notification blocked after 30 mins window`() {
        assertFalse(shouldNotifySimulated(11, 31))
        assertFalse(shouldNotifySimulated(14, 31))
    }

    @Test
    fun `test notification blocked at random times`() {
        assertFalse(shouldNotifySimulated(9, 0))
        assertFalse(shouldNotifySimulated(12, 0))
        assertFalse(shouldNotifySimulated(18, 0))
    }

    @Test
    fun `test notification blocked on Saturday even at correct time`() {
        assertFalse(shouldNotifySimulated(11, 0, Calendar.SATURDAY))
        assertFalse(shouldNotifySimulated(14, 0, Calendar.SATURDAY))
    }

    @Test
    fun `test notification blocked on Sunday even at correct time`() {
        assertFalse(shouldNotifySimulated(11, 0, Calendar.SUNDAY))
        assertFalse(shouldNotifySimulated(14, 0, Calendar.SUNDAY))
    }
}
