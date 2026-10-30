package dev.haxzie.smsbridge.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import dev.haxzie.smsbridge.service.BridgeContainer
import dev.haxzie.smsbridge.service.BridgeService
import java.text.DateFormat
import java.util.Date

private val REQUIRED_PERMISSIONS = buildList {
    add(Manifest.permission.READ_SMS)
    add(Manifest.permission.RECEIVE_SMS)
    add(Manifest.permission.READ_CONTACTS)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        add(Manifest.permission.POST_NOTIFICATIONS)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(container: BridgeContainer, onPairMac: () -> Unit) {
    val context = LocalContext.current
    val running by container.running.collectAsState()
    val devices by container.devices.devices.collectAsState()
    val connected by container.server.connected.collectAsState()
    val lastError by container.lastError.collectAsState()

    var missingPermissions by remember { mutableStateOf(missingPermissions(context)) }
    var batteryExempt by remember { mutableStateOf(isBatteryExempt(context)) }
    refreshOnResume {
        missingPermissions = missingPermissions(context)
        batteryExempt = isBatteryExempt(context)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { missingPermissions = missingPermissions(context) }

    Scaffold(topBar = { TopAppBar(title = { Text("SMS Bridge") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Mirror to my Macs", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = when {
                                !running -> "Off"
                                connected.isEmpty() -> "On · waiting for a Mac on this Wi-Fi"
                                else -> "On · ${connected.joinToString(", ")}"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = running,
                        enabled = missingPermissions.isEmpty(),
                        onCheckedChange = { on ->
                            if (on) BridgeService.start(context) else BridgeService.stop(context)
                        },
                    )
                }
            }

            lastError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            if (missingPermissions.isNotEmpty()) {
                SetupCard(
                    title = "Permissions needed",
                    body = "SMS Bridge needs to read your messages and contacts so it can mirror them to your Mac. Nothing leaves your Wi-Fi network.",
                    action = "Grant",
                ) { permissionLauncher.launch(missingPermissions.toTypedArray()) }
            }

            if (!batteryExempt) {
                SetupCard(
                    title = "Allow background running",
                    body = "Without a battery optimisation exemption Android may stop mirroring while the phone is idle.",
                    action = "Open settings",
                ) { context.requestBatteryExemption() }
            }

            Button(onClick = onPairMac, modifier = Modifier.fillMaxWidth()) {
                Text("Pair a Mac")
            }

            Text("Paired Macs", style = MaterialTheme.typography.titleMedium)
            if (devices.isEmpty()) {
                Text(
                    "None yet. Open SMS Bridge on your Mac and scan the QR code it shows.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(devices, key = { it.name }) { device ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(device.name, style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        text = if (device.name in connected) {
                                            "Connected now"
                                        } else {
                                            "Last seen ${formatTime(device.lastSeen)}"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                IconButton(onClick = {
                                    container.devices.remove(device.name)
                                    container.server.disconnect(device.name)
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove ${device.name}")
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            container.server.port.collectAsState().value?.let { port ->
                Text(
                    "Listening on port $port as \"${container.deviceName}\"",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SetupCard(title: String, body: String, action: String, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(body, style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(onClick = onClick) { Text(action) }
        }
    }
}

@Composable
private fun refreshOnResume(onResume: () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) onResume()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}

private fun missingPermissions(context: Context): List<String> = REQUIRED_PERMISSIONS.filter {
    ContextCompat.checkSelfPermission(context, it) != android.content.pm.PackageManager.PERMISSION_GRANTED
}

private fun isBatteryExempt(context: Context): Boolean =
    context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)

/**
 * The direct REQUEST_IGNORE_BATTERY_OPTIMIZATIONS dialog is restricted by Play policy to a
 * narrow allowlist. This app is sideload-only (SMS permissions rule out Play distribution), so
 * the direct intent is fine, with the settings list as a fallback.
 */
private fun Context.requestBatteryExemption() {
    val direct = Intent(
        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
        Uri.fromParts("package", packageName, null),
    )
    val intent = if (packageManager.resolveActivity(direct, 0) != null) {
        direct
    } else {
        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
    }
    runCatching { startActivity(intent) }
}

private fun formatTime(epochMillis: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(epochMillis))
