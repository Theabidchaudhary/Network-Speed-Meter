package com.abidfareed.networkspeedmeter.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.abidfareed.networkspeedmeter.network.NetworkType
import com.abidfareed.networkspeedmeter.util.PowerUtils
import com.abidfareed.networkspeedmeter.util.SpeedFormatter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.abidfareed.networkspeedmeter.R

@Composable
fun MainScreen(viewModel: MainViewModel, onOpenSettings: () -> Unit) {
    val settings by viewModel.settings.collectAsState()
    val networkType by viewModel.networkType.collectAsState()
    val isRunning by viewModel.isServiceRunning.collectAsState()
    val sample by viewModel.liveSample.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
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
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatusCard(
                isRunning = isRunning,
                enabled = settings.enabled,
                networkType = networkType
            )

            SpeedCard(
                downloadBytesPerSec = sample.downloadBytesPerSec,
                uploadBytesPerSec = sample.uploadBytesPerSec,
                unit = settings.unit,
                decimalPlaces = settings.decimalPlaces
            )

            Card(colors = CardDefaults.cardColors()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Network Speed Meter ON", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = settings.enabled,
                        onCheckedChange = { viewModel.setEnabled(it) }
                    )
                }
            }

            if (settings.enabled && !PowerUtils.isIgnoringBatteryOptimizations(context)) {
                BatteryWarningCard(onFix = {
                    context.startActivity(PowerUtils.requestIgnoreBatteryOptimizationsIntent(context))
                })
            }

            if (settings.enabled && !isRunning) {
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "The service isn't currently active. Samsung may have stopped it in the background.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { viewModel.restartService() }) {
                            Text("Restart service")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusCard(isRunning: Boolean, enabled: Boolean, networkType: NetworkType) {
    Card {
        Column(modifier = Modifier.padding(20.dp)) {
            val statusText = when {
                !enabled -> "○ Speed meter inactive"
                isRunning -> "● Speed meter active"
                else -> "○ Speed meter inactive"
            }
            Text(statusText, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            val networkLabel = when (networkType) {
                NetworkType.WIFI -> "Monitoring Wi-Fi"
                NetworkType.MOBILE -> "Monitoring mobile data"
                NetworkType.ETHERNET -> "Monitoring Ethernet"
                NetworkType.VPN -> "Monitoring via VPN"
                NetworkType.OTHER -> "Monitoring network"
                NetworkType.NONE -> "No active connection"
            }
            Text(networkLabel, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun SpeedCard(downloadBytesPerSec: Long, uploadBytesPerSec: Long, unit: com.abidfareed.networkspeedmeter.settings.SpeedUnit, decimalPlaces: Int) {
    Card {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(
                    SpeedFormatter.format(downloadBytesPerSec, unit, decimalPlaces),
                    style = MaterialTheme.typography.headlineMedium
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.width(8.dp))
                Text(
                    SpeedFormatter.format(uploadBytesPerSec, unit, decimalPlaces),
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        }
    }
}

@Composable
private fun BatteryWarningCard(onFix: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Samsung may put this app to sleep in the background, which stops the speed meter.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = onFix) {
                Text("Exempt from battery optimization")
            }
        }
    }
}

