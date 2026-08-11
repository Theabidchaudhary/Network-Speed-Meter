package com.abidfareed.networkspeedmeter

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.abidfareed.networkspeedmeter.network.NetworkMonitor
import com.abidfareed.networkspeedmeter.network.NetworkType
import com.abidfareed.networkspeedmeter.network.SpeedSample
import com.abidfareed.networkspeedmeter.network.SpeedTracker
import com.abidfareed.networkspeedmeter.service.ServiceStatus
import com.abidfareed.networkspeedmeter.service.SpeedMeterService
import com.abidfareed.networkspeedmeter.settings.AppSettings
import com.abidfareed.networkspeedmeter.settings.AppTheme
import com.abidfareed.networkspeedmeter.settings.DisplayMode
import com.abidfareed.networkspeedmeter.settings.NetworkFilter
import com.abidfareed.networkspeedmeter.settings.SettingsRepository
import com.abidfareed.networkspeedmeter.settings.SpeedUnit
import com.abidfareed.networkspeedmeter.settings.TextSize
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SettingsRepository(application)
    private val networkMonitor = NetworkMonitor(application)

    val settings: StateFlow<AppSettings> = repository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    val networkType: StateFlow<NetworkType> = networkMonitor.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NetworkType.NONE)

    val isServiceRunning: StateFlow<Boolean> = ServiceStatus.isRunning

    private val speedTracker = SpeedTracker()

    /** Independent, lightweight UI-only sampler so the home screen shows live numbers whether
     * or not the foreground service happens to be running (e.g. while the user is deciding
     * whether to turn it on). Uses the same cheap TrafficStats read the service uses. */
    val liveSample: StateFlow<SpeedSample> = speedTracker.samples(
        settings.map { it.updateIntervalMillis }.distinctUntilChanged()
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SpeedSample(0, 0))

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setEnabled(enabled)
            if (enabled) {
                SpeedMeterService.start(getApplication<Application>())
            } else {
                SpeedMeterService.stop(getApplication<Application>())
            }
        }
    }

    fun restartService() {
        val app = getApplication<Application>()
        SpeedMeterService.stop(app)
        SpeedMeterService.start(app)
    }

    fun setDisplayMode(mode: DisplayMode) = viewModelScope.launch { repository.setDisplayMode(mode) }
    fun setUnit(unit: SpeedUnit) = viewModelScope.launch { repository.setUnit(unit) }
    fun setUpdateInterval(ms: Long) = viewModelScope.launch { repository.setUpdateIntervalMillis(ms) }
    fun setDecimalPlaces(count: Int) = viewModelScope.launch { repository.setDecimalPlaces(count) }
    fun setShowWhenZero(show: Boolean) = viewModelScope.launch { repository.setShowWhenZero(show) }
    fun setNetworkFilter(filter: NetworkFilter) = viewModelScope.launch { repository.setNetworkFilter(filter) }
    fun setTheme(theme: AppTheme) = viewModelScope.launch { repository.setTheme(theme) }
    fun setTextSize(size: TextSize) = viewModelScope.launch { repository.setTextSize(size) }
    fun setStartOnBoot(value: Boolean) = viewModelScope.launch { repository.setStartOnBoot(value) }
}
