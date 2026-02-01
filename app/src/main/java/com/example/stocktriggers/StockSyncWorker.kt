package com.example.stocktriggers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * StockSyncWorker is a WorkManager worker that performs background synchronization of stock data.
 *
 * Intent:
 * This worker runs periodically to fetch the latest stock data for all favorite symbols.
 * If significant market movements are detected (BUY/SELL signals), it triggers a system notification
 * to alert the user even if the app is closed.
 *
 * Exposed APIs:
 * - [doWork]: The entry point for the background work.
 */
class StockSyncWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    private val stockRepository = StockRepository()
    private val favoritesRepository = FavoritesRepository(appContext)
    private val stockAnalyzer = StockAnalyzer

    /**
     * Executes the background work.
     *
     * Iterates through all favorite stocks, fetches their latest data, and checks for trading signals.
     * If a signal is found (BUY or SELL), it sends a notification.
     *
     * @return [Result.success] if the work completed successfully (even if individual stocks failed),
     *         or [Result.retry] if a fatal error occurred preventing the entire batch.
     */
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d("StockSyncWorker", "Starting background sync")
            val favorites = favoritesRepository.getFavorites()
            if (favorites.isEmpty()) {
                Log.d("StockSyncWorker", "No favorites to sync")
                return@withContext Result.success()
            }

            for (symbol in favorites) {
                try {
                    val history = stockRepository.getLastWorkingDaysData(symbol)
                    val analysis = stockAnalyzer.analyze(history)
                    // Only notify if there is a BUY or SELL signal
                    if (analysis != null && analysis.signal != Signal.NEUTRAL) {
                        Log.d("StockSyncWorker", "Trigger detected for $symbol: ${analysis.signal}")
                        
                        if (shouldNotify()) {
                            sendNotification(symbol, analysis.signal, analysis.currentPrice)
                        } else {
                            Log.d("StockSyncWorker", "Skipping notification: Not in allowed window")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("StockSyncWorker", "Failed to sync $symbol", e)
                }
            }
            
            Result.success()
        } catch (e: Exception) {
            Log.e("StockSyncWorker", "Sync failed", e)
            Result.retry()
        }
    }

    /**
     * Helper function to build and trigger a system notification.
     *
     * @param symbol The stock symbol (e.g., "AAPL").
     * @param signal The detected trading signal (BUY/SELL).
     * @param price The current stock price.
     */
    private fun sendNotification(symbol: String, signal: Signal, price: Double) {
        val context = applicationContext
        val channelId = "stock_triggers_channel"
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Stock Triggers",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val title = "$symbol: $signal Signal!"
        val message = "Price is ${String.format("%.2f", price)}. Check the app!"
        
        Log.d("StockSyncWorker", "Building notification: $title")
        
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
            
        notificationManager.notify(symbol.hashCode(), notification)
    }

    /**
     * Checks if the current time coincides with a scheduled notification time.
     * Allowed window is [ScheduledTime, ScheduledTime + 30 minutes].
     */
    private fun shouldNotify(): Boolean {
        val calendar = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone(AppConfig.SYNC_TIMEZONE))
        
        // 1. Check for Weekend
        val dayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK)
        if (dayOfWeek == java.util.Calendar.SATURDAY || dayOfWeek == java.util.Calendar.SUNDAY) {
            return false
        }

        // 2. Check for Time Window
        val currentHour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(java.util.Calendar.MINUTE)

        // Convert current time to minutes from start of day for easier comparison
        val currentTotalMinutes = currentHour * 60 + currentMinute

        for ((hour, minute) in AppConfig.NOTIFICATION_TIMES) {
            val scheduledTotalMinutes = hour * 60 + minute
            // Check if we are within the 30-minute window AFTER the scheduled time
            // Example: 11:00 schedule, allowed 11:00 - 11:30
            if (currentTotalMinutes in scheduledTotalMinutes..(scheduledTotalMinutes + 30)) {
                return true
            }
        }
        return false
    }
}
