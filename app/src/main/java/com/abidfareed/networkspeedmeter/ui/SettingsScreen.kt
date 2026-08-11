package com.abidfareed.networkspeedmeter.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.abidfareed.networkspeedmeter.MainViewModel
import com.abidfareed.networkspeedmeter.settings.AppTheme
import com.abidfareed.networkspeedmeter.settings.DisplayMode
import com.abidfareed.networkspeedmeter.settings.NetworkFilter
import com.abidfareed.networkspeedmeter.settings.SpeedUnit
import com.abidfareed.networkspeedmeter.settings.TextSize

@Composable
fun SettingsScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding: PaddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            SettingsSection(title = "Display") {
                LabeledChipRow(
                    label = "Show",
                    options = DisplayMode.entries.map { it to it.label() },
                    selected = settings.displayMode,
                    onSelect = viewModel::setDisplayMode
                )
                LabeledChipRow(
                    label = "Units",
                    options = SpeedUnit.entries.map { it to it.label() },
                    selected = settings.unit,
                    onSelect = viewModel::setUnit
                )
                LabeledChipRow(
                    label = "Decimal places",
                    options = listOf(0, 1, 2).map { it to it.toString() },
                    selected = settings.decimalPlaces,
                    onSelect = viewModel::setDecimalPlaces
                )
                LabeledChipRow(
                    label = "Update interval",
                    options = listOf(500L to "500 ms", 1000L to "1 s", 2000L to "2 s"),
                    selected = settings.updateIntervalMillis,
                    onSelect = viewModel::setUpdateInterval
                )
                SettingsSwitchRow(
                    label = "Show when speed is zero",
                    checked = settings.showWhenZero,
                    onCheckedChange = viewModel::setShowWhenZero
                )
            }

            SettingsSection(title = "Network") {
                LabeledChipRow(
                    label = "Show for",
                    options = NetworkFilter.entries.map { it to it.label() },
                    selected = settings.networkFilter,
                    onSelect = viewModel::setNetworkFilter
                )
            }

            SettingsSection(title = "Appearance") {
                LabeledChipRow(
                    label = "Theme",
                    options = AppTheme.entries.map { it to it.label() },
                    selected = settings.theme,
                    onSelect = viewModel::setTheme
                )
                LabeledChipRow(
                    label = "Text size",
                    options = TextSize.entries.map { it to it.label() },
                    selected = settings.textSize,
                    onSelect = viewModel::setTextSize
                )
            }

            SettingsSection(title = "Startup") {
                SettingsSwitchRow(
                    label = "Start speed meter after device boot",
                    checked = settings.startOnBoot,
                    onCheckedChange = viewModel::setStartOnBoot
                )
            }

            Text(
                "Everything runs entirely on-device. No data ever leaves your phone: no server, " +
                    "no account, no analytics, no ads.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Card {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun SettingsSwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun <T> LabeledChipRow(
    label: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (value, text) ->
                FilterChip(
                    selected = value == selected,
                    onClick = { onSelect(value) },
                    label = { Text(text) }
                )
            }
        }
    }
}

private fun DisplayMode.label() = when (this) {
    DisplayMode.DOWNLOAD_ONLY -> "Download only"
    DisplayMode.UPLOAD_ONLY -> "Upload only"
    DisplayMode.DOWNLOAD_AND_UPLOAD -> "Download + Upload"
}

private fun SpeedUnit.label() = when (this) {
    SpeedUnit.AUTO -> "Auto"
    SpeedUnit.B -> "B/s"
    SpeedUnit.KB -> "KB/s"
    SpeedUnit.MB -> "MB/s"
    SpeedUnit.GB -> "GB/s"
}

private fun NetworkFilter.label() = when (this) {
    NetworkFilter.BOTH -> "Wi-Fi & Mobile"
    NetworkFilter.WIFI_ONLY -> "Wi-Fi only"
    NetworkFilter.MOBILE_ONLY -> "Mobile only"
}

private fun AppTheme.label() = when (this) {
    AppTheme.SYSTEM -> "Follow system"
    AppTheme.LIGHT -> "Light"
    AppTheme.DARK -> "Dark"
}

private fun TextSize.label() = when (this) {
    TextSize.SMALL -> "Small"
    TextSize.MEDIUM -> "Medium"
    TextSize.LARGE -> "Large"
}
