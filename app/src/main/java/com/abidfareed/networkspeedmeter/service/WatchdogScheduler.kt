package com.abidfareed.networkspeedmeter.service

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/** Schedules the two layers of the "service must not silently die" safety net. */
object WatchdogScheduler {

    private const val PERIODIC_WORK_NAME = "speed_meter_watchdog_periodic"
    private const val REVIVE_WORK_NAME = "speed_meter_watchdog_revive"

    /** Long-running periodic check, called once the service starts. WorkManager's floor is 15 min. */
    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<WatchdogWorker>(15, TimeUnit.MINUTES)
            .setBackoffCriteria(BackoffPolicy.LINEAR, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    /** Fast, short-delay one-shot revive attempt, called from the service's onDestroy(). */
    fun scheduleReviveIfEnabled(context: Context) {
        val request = OneTimeWorkRequestBuilder<WatchdogWorker>()
            .setInitialDelay(5, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            REVIVE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}
