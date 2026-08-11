package com.abidfareed.networkspeedmeter.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.abidfareed.networkspeedmeter.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

/**
 * Relaunches the foreground service after a reboot (or an app update, which also kills the
 * process) - but only if the user has left "Start after boot" enabled in Settings.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_LOCKED_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return

        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val settings = SettingsRepository(appContext).settingsFlow.first()
                if (settings.enabled && settings.startOnBoot) {
                    ContextCompat.startForegroundService(
                        appContext,
                        Intent(appContext, SpeedMeterService::class.java)
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
