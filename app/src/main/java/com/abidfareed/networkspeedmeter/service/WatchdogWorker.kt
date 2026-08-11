package com.abidfareed.networkspeedmeter.service

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.abidfareed.networkspeedmeter.settings.SettingsRepository
import kotlinx.coroutines.flow.first

/**
 * Periodic (WorkManager, minimum 15-minute cadence) safety net: if the user still wants the
 * meter running, make sure the foreground service is actually alive. Re-issuing
 * startForegroundService() on an already-running service is a harmless no-op (just triggers
 * onStartCommand again), so no "is it already running" check is needed.
 */
class WatchdogWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val settings = SettingsRepository(applicationContext).settingsFlow.first()
        if (settings.enabled) {
            ContextCompat.startForegroundService(
                applicationContext,
                android.content.Intent(applicationContext, SpeedMeterService::class.java)
            )
        }
        return Result.success()
    }
}
