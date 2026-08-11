package com.abidfareed.networkspeedmeter.settings

enum class DisplayMode { DOWNLOAD_ONLY, UPLOAD_ONLY, DOWNLOAD_AND_UPLOAD }

enum class SpeedUnit { AUTO, KB, MB, GB }

enum class NetworkFilter { BOTH, WIFI_ONLY, MOBILE_ONLY }

enum class AppTheme { SYSTEM, LIGHT, DARK }

enum class TextSize { SMALL, MEDIUM, LARGE }

data class AppSettings(
    val enabled: Boolean = true,
    val displayMode: DisplayMode = DisplayMode.DOWNLOAD_ONLY,
    val unit: SpeedUnit = SpeedUnit.AUTO,
    val updateIntervalMillis: Long = 1000L,
    val decimalPlaces: Int = 0,
    val showWhenZero: Boolean = true,
    val networkFilter: NetworkFilter = NetworkFilter.BOTH,
    val theme: AppTheme = AppTheme.SYSTEM,
    val textSize: TextSize = TextSize.MEDIUM,
    val startOnBoot: Boolean = true
)
