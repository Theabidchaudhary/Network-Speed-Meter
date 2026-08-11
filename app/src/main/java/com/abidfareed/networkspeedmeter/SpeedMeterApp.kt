package com.abidfareed.networkspeedmeter

import android.app.Application
import com.abidfareed.networkspeedmeter.notification.NotificationHelper

class SpeedMeterApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Safe to call unconditionally: creating a channel that already exists is a no-op.
        NotificationHelper(this).ensureChannel()
    }
}
