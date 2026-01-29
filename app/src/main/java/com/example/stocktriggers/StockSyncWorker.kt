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

class StockSyncWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    private val stockRepository = StockRepository()
    private val favoritesRepository = FavoritesRepository(appContext)
    private val stockAnalyzer = StockAnalyzer

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d("StockSyncWorker", "Starting background sync")
            val favorites = favoritesRepository.getFavorites()
            if (favorites.isEmpty()) {
                Log.d("StockSyncWorker", "No favorites to sync")
                return@withContext Result.success()
            }

            // In a real app, we might save this data to a database. 
            // Here we just notify if any trigger is hit to prove it works.
            for (symbol in favorites) {
                try {
                    val history = stockRepository.getLastWorkingDaysData(symbol)
                    val analysis = stockAnalyzer.analyze(history)
                    if (analysis != null && analysis.signal != Signal.NEUTRAL) {
                        sendNotification(symbol, analysis.signal, analysis.currentPrice)
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
        
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Using system icon for simplicity
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
            
        // Use unique ID per symbol (hashcode)
        notificationManager.notify(symbol.hashCode(), notification)
    }
}
