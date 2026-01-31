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
     * The hour of the day (24-hour format) when the daily sync cycle should start.
     * Default: 9 (9 AM)
     */
    const val SYNC_START_HOUR = 9

    /**
     * The minute of the hour when the daily sync cycle should start.
     * Default: 30 (30 minutes past the hour)
     */
    const val SYNC_START_MINUTE = 30

    /**
     * The timezone ID for the sync schedule.
     * Default: "Asia/Kolkata" (IST)
     */
    const val SYNC_TIMEZONE = "Asia/Kolkata"

    /**
     * The interval at which the background sync should repeat.
     * Default: 1
     */
    const val SYNC_INTERVAL_VALUE = 1L

    /**
     * The unit of time for the sync interval.
     * Default: HOURS
     */
    val SYNC_INTERVAL_UNIT = TimeUnit.HOURS
}
