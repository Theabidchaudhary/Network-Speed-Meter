package com.abidfareed.networkspeedmeter.service

import android.app.Notification
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.abidfareed.networkspeedmeter.network.NetworkMonitor
import com.abidfareed.networkspeedmeter.network.NetworkType
import com.abidfareed.networkspeedmeter.network.SpeedSample
import com.abidfareed.networkspeedmeter.network.SpeedTracker
import com.abidfareed.networkspeedmeter.notification.NotificationHelper
import com.abidfareed.networkspeedmeter.notification.SpeedIconRenderer
import com.abidfareed.networkspeedmeter.settings.AppSettings
import com.abidfareed.networkspeedmeter.settings.DisplayMode
import com.abidfareed.networkspeedmeter.settings.NetworkFilter
import com.abidfareed.networkspeedmeter.settings.SettingsRepository
import com.abidfareed.networkspeedmeter.util.SpeedFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Foreground service that owns the whole background lifecycle: it samples throughput, watches
 * network-type changes, reacts to settings, and keeps a single minimal notification (whose small
 * icon is the live speed) posted the whole time it runs. See TECHNICAL_FEASIBILITY.md for why
 * this notification-icon approach is the mechanism, and README.md for reliability engineering
 * (START_STICKY, boot receiver, watchdog alarm, battery-exemption prompt).
 */
class SpeedMeterService : LifecycleService() {

    companion object {
        const val ACTION_STOP = "com.abidfareed.networkspeedmeter.action.STOP"

        fun start(context: Context) {
            val intent = Intent(context, SpeedMeterService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, SpeedMeterService::class.java))
        }
    }

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var notificationHelper: NotificationHelper
    private val speedTracker = SpeedTracker()
    private lateinit var networkMonitor: NetworkMonitor

    private val _lastSample = MutableStateFlow(SpeedSample(0, 0))
    val lastSample get() = _lastSample.asStateFlow()

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(applicationContext)
        notificationHelper = NotificationHelper(applicationContext)
        networkMonitor = NetworkMonitor(applicationContext)
        notificationHelper.ensureChannel()

        // Must call startForeground within the OS grace period after startForegroundService().
        startForeground(NotificationHelper.NOTIFICATION_ID, placeholderNotification())

        observeAndPublish()
        WatchdogScheduler.schedule(applicationContext)
        ServiceStatus.setRunning(true)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    private fun placeholderNotification(): Notification =
        notificationHelper.buildNotification(
            downloadBytesPerSec = 0,
            uploadBytesPerSec = 0,
            contentText = getString(com.abidfareed.networkspeedmeter.R.string.notification_text_running),
            iconBitmap = SpeedIconRenderer.render(value = "0.0", unit = "KB")
        )

    private fun observeAndPublish() {
        val settingsFlow = settingsRepository.settingsFlow.distinctUntilChanged()
        val networkTypeFlow = networkMonitor.observe()

        lifecycleScope.launch {
            settingsFlow.collect { settings ->
                if (!settings.enabled) {
                    stopSelf()
                }
            }
        }

        val samplesFlow = settingsFlow
            .map { it.updateIntervalMillis }
            .distinctUntilChanged()
            .let { intervalFlow -> speedTracker.samples(intervalFlow) }

        lifecycleScope.launch {
            combine(samplesFlow, settingsFlow, networkTypeFlow) { sample, settings, networkType ->
                Triple(sample, settings, networkType)
            }.collect { (sample, settings, networkType) ->
                _lastSample.value = sample
                publish(sample, settings, networkType)
            }
        }
    }

    private fun publish(sample: SpeedSample, settings: AppSettings, networkType: NetworkType) {
        val filterMatches = when (settings.networkFilter) {
            NetworkFilter.BOTH -> true
            NetworkFilter.WIFI_ONLY -> networkType == NetworkType.WIFI
            NetworkFilter.MOBILE_ONLY -> networkType == NetworkType.MOBILE
        }

        val effectiveDown = if (filterMatches) sample.downloadBytesPerSec else 0L
        val effectiveUp = if (filterMatches) sample.uploadBytesPerSec else 0L

        val isIdle = effectiveDown == 0L && effectiveUp == 0L
        val hideForIdle = isIdle && !settings.showWhenZero

        val formattedDown = SpeedFormatter.formatCompact(effectiveDown, settings.unit, settings.decimalPlaces)
        val formattedUp = SpeedFormatter.formatCompact(effectiveUp, settings.unit, settings.decimalPlaces)

        val icon = if (hideForIdle) {
            SpeedIconRenderer.renderIdle()
        } else {
            notificationHelper.iconForSpeeds(
                effectiveDown, effectiveUp, settings.displayMode, formattedDown, formattedUp, settings.textSize
            )
        }

        val contentText = when (settings.displayMode) {
            DisplayMode.DOWNLOAD_ONLY -> "↓ ${SpeedFormatter.format(effectiveDown, settings.unit, settings.decimalPlaces)}"
            DisplayMode.UPLOAD_ONLY -> "↑ ${SpeedFormatter.format(effectiveUp, settings.unit, settings.decimalPlaces)}"
            DisplayMode.DOWNLOAD_AND_UPLOAD ->
                "↓ ${SpeedFormatter.format(effectiveDown, settings.unit, settings.decimalPlaces)}  " +
                    "↑ ${SpeedFormatter.format(effectiveUp, settings.unit, settings.decimalPlaces)}"
        } + when (networkType) {
            NetworkType.WIFI -> " · Wi-Fi"
            NetworkType.MOBILE -> " · Mobile"
            NetworkType.ETHERNET -> " · Ethernet"
            NetworkType.VPN -> " · VPN"
            else -> ""
        }

        val notification = notificationHelper.buildNotification(effectiveDown, effectiveUp, contentText, icon)
        val nm = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        nm.notify(NotificationHelper.NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        ServiceStatus.setRunning(false)
        WatchdogScheduler.scheduleReviveIfEnabled(applicationContext)
    }
}
