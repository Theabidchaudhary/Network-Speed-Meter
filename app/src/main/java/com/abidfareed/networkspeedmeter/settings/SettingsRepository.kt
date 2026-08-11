package com.abidfareed.networkspeedmeter.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "speed_meter_settings")

/** Thin, dependency-free wrapper over Preferences DataStore for all user-configurable options. */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val ENABLED = booleanPreferencesKey("enabled")
        val DISPLAY_MODE = stringPreferencesKey("display_mode")
        val UNIT = stringPreferencesKey("unit")
        val INTERVAL_MS = longPreferencesKey("interval_ms")
        val DECIMALS = intPreferencesKey("decimals")
        val SHOW_WHEN_ZERO = booleanPreferencesKey("show_when_zero")
        val NETWORK_FILTER = stringPreferencesKey("network_filter")
        val THEME = stringPreferencesKey("theme")
        val TEXT_SIZE = stringPreferencesKey("text_size")
        val START_ON_BOOT = booleanPreferencesKey("start_on_boot")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            enabled = prefs[Keys.ENABLED] ?: true,
            displayMode = prefs[Keys.DISPLAY_MODE]?.let { runCatching { DisplayMode.valueOf(it) }.getOrNull() }
                ?: DisplayMode.DOWNLOAD_ONLY,
            unit = prefs[Keys.UNIT]?.let { runCatching { SpeedUnit.valueOf(it) }.getOrNull() } ?: SpeedUnit.AUTO,
            updateIntervalMillis = prefs[Keys.INTERVAL_MS] ?: 1000L,
            decimalPlaces = prefs[Keys.DECIMALS] ?: 1,
            showWhenZero = prefs[Keys.SHOW_WHEN_ZERO] ?: true,
            networkFilter = prefs[Keys.NETWORK_FILTER]?.let { runCatching { NetworkFilter.valueOf(it) }.getOrNull() }
                ?: NetworkFilter.BOTH,
            theme = prefs[Keys.THEME]?.let { runCatching { AppTheme.valueOf(it) }.getOrNull() } ?: AppTheme.SYSTEM,
            textSize = prefs[Keys.TEXT_SIZE]?.let { runCatching { TextSize.valueOf(it) }.getOrNull() }
                ?: TextSize.MEDIUM,
            startOnBoot = prefs[Keys.START_ON_BOOT] ?: true
        )
    }

    suspend fun setEnabled(value: Boolean) = context.dataStore.edit { it[Keys.ENABLED] = value }

    suspend fun setDisplayMode(value: DisplayMode) =
        context.dataStore.edit { it[Keys.DISPLAY_MODE] = value.name }

    suspend fun setUnit(value: SpeedUnit) = context.dataStore.edit { it[Keys.UNIT] = value.name }

    suspend fun setUpdateIntervalMillis(value: Long) =
        context.dataStore.edit { it[Keys.INTERVAL_MS] = value }

    suspend fun setDecimalPlaces(value: Int) = context.dataStore.edit { it[Keys.DECIMALS] = value }

    suspend fun setShowWhenZero(value: Boolean) =
        context.dataStore.edit { it[Keys.SHOW_WHEN_ZERO] = value }

    suspend fun setNetworkFilter(value: NetworkFilter) =
        context.dataStore.edit { it[Keys.NETWORK_FILTER] = value.name }

    suspend fun setTheme(value: AppTheme) = context.dataStore.edit { it[Keys.THEME] = value.name }

    suspend fun setTextSize(value: TextSize) = context.dataStore.edit { it[Keys.TEXT_SIZE] = value.name }

    suspend fun setStartOnBoot(value: Boolean) =
        context.dataStore.edit { it[Keys.START_ON_BOOT] = value }
}
