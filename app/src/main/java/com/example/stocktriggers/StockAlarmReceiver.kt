package com.example.stocktriggers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * StockAlarmReceiver triggers an expedited sync via WorkManager when an alarm fires.
 * This is used for time-critical syncs (11 AM, 2 PM) to bypass Doze limits.
 */
class StockAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("StockAlarmReceiver", "Alarm received! Triggering expedited sync.")

        val inputData = Data.Builder()
            .putBoolean("FORCE_NOTIFY", true)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<StockSyncWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setInputData(inputData)
            .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueue(syncRequest)
    }
}
