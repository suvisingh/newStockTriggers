package com.example.stocktriggers

import java.util.concurrent.TimeUnit

/**
 * AppConfig holds the application-wide configuration constants.
 *
 * Intent:
 * Centralizes configuration values like sync schedules to make them easier to manage and modify.
 */
object AppConfig {
    /**
     * List of daily sync times (Hour, Minute) in 24-hour format.
     * Scheduled for every hour from 9:00 AM to 6:00 PM IST.
     */
    val SYNC_TIMES = (9..18).map { hour -> Pair(hour, 0) }

    /**
     * List of times when notifications are allowed to be shown.
     * Notifications only for:
     * - 11:00 AM
     * - 2:00 PM (14:00)
     */
    val NOTIFICATION_TIMES = listOf(
        Pair(11, 0),
        Pair(14, 0)
    )

    /**
     * The timezone ID for the sync schedule.
     * Default: "Asia/Kolkata" (IST)
     */
    const val SYNC_TIMEZONE = "Asia/Kolkata"
}

