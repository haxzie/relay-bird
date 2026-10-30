package dev.haxzie.relaybird.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LaptopMac
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.haxzie.relaybird.R
import dev.haxzie.relaybird.service.BridgeContainer
import dev.haxzie.relaybird.service.BridgeService
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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(R.drawable.ic_relaybird),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(modifier = Modifier.size(10.dp))
                        Text("RelayBird", style = MaterialTheme.typography.titleLarge)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Dimens.screenPadding),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            MirrorCard(
                running = running,
                connected = connected,
                enabled = missingPermissions.isEmpty(),
                onToggle = { on ->
                    if (on) BridgeService.start(context) else BridgeService.stop(context)
                },
                onPairMac = onPairMac,
            )

            lastError?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            if (missingPermissions.isNotEmpty()) {
                SetupCard(
                    title = "Permissions needed",
                    body = "RelayBird reads your messages and contacts so it can mirror them to your Mac. Nothing leaves your Wi-Fi network.",
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

            Text(
                text = "PAIRED MACS",
                style = SectionLabelStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )

            if (devices.isEmpty()) {
                Text(
                    text = "None yet. Open RelayBird on your Mac and scan the code it shows.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    modifier = Modifier.weight(1f, fill = false),
                ) {
                    items(devices, key = { it.name }) { device ->
                        DeviceRow(
                            name = device.name,
                            detail = if (device.name in connected) {
                                "Connected now"
                            } else {
                                "Last seen ${formatTime(device.lastSeen)}"
                            },
                            online = device.name in connected,
                            onRemove = {
                                container.devices.remove(device.name)
                                container.server.disconnect(device.name)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MirrorCard(
    running: Boolean,
    connected: List<String>,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onPairMac: () -> Unit,
) {
    val linked = running && connected.isNotEmpty()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.cardRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column {
            LinkIllustration(
                linked = linked,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 26.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 18.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Mirror to my Macs", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = when {
                            !running -> "Off"
                            connected.isEmpty() -> "Waiting for a Mac on this Wi-Fi"
                            else -> connected.joinToString(", ")
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (linked) RelayBirdColors.Green else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = running,
                    enabled = enabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = RelayBirdColors.Black,
                        checkedTrackColor = RelayBirdColors.White,
                        checkedBorderColor = RelayBirdColors.White,
                        uncheckedThumbColor = RelayBirdColors.Muted,
                        uncheckedTrackColor = RelayBirdColors.SurfaceRaised,
                        uncheckedBorderColor = RelayBirdColors.Border,
                    ),
                )
            }

            Button(
                onClick = onPairMac,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 18.dp, bottom = 18.dp)
                    .height(48.dp),
                shape = RoundedCornerShape(Dimens.buttonRadius),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RelayBirdColors.White,
                    contentColor = RelayBirdColors.Black,
                ),
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text("Pair a Mac", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/**
 * Phone and Mac with a run of dots between them: green once a Mac is actually connected, grey
 * otherwise. The dots travel while connected, which is the clearest way to say "live" without
 * adding any words.
 */
@Composable
private fun LinkIllustration(linked: Boolean, modifier: Modifier = Modifier) {
    val tint by animateColorAsState(
        targetValue = if (linked) RelayBirdColors.Green else RelayBirdColors.Border,
        animationSpec = tween(durationMillis = 400),
        label = "linkTint",
    )
    val endpointTint by animateColorAsState(
        targetValue = if (linked) RelayBirdColors.White else RelayBirdColors.Muted,
        animationSpec = tween(durationMillis = 400),
        label = "endpointTint",
    )
    val transition = rememberInfiniteTransition(label = "linkFlow")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = DOT_COUNT.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
        ),
        label = "linkPhase",
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Endpoint(icon = Icons.Default.Smartphone, tint = endpointTint, description = "Phone")
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(DOT_COUNT) { index ->
                // Each dot brightens as the phase passes it, so the run reads left to right.
                val distance = ((phase - index + DOT_COUNT) % DOT_COUNT).coerceAtMost(
                    (index - phase + DOT_COUNT) % DOT_COUNT,
                )
                val alpha = if (linked) {
                    lerp(0.25f, 1f, (1f - (distance / 1.6f)).coerceIn(0f, 1f))
                } else {
                    1f
                }
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(tint.copy(alpha = tint.alpha * alpha), CircleShape),
                )
            }
        }
        Endpoint(icon = Icons.Default.LaptopMac, tint = endpointTint, description = "Mac")
    }
}

@Composable
private fun Endpoint(icon: ImageVector, tint: Color, description: String) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = tint,
            modifier = Modifier.size(24.dp),
        )
    }
}

private const val DOT_COUNT = 5

@Composable
private fun DeviceRow(name: String, detail: String, online: Boolean, onRemove: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.cardRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.LaptopMac,
                    contentDescription = null,
                    tint = if (online) RelayBirdColors.Green else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(modifier = Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (online) RelayBirdColors.Green else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove $name",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun PrimaryButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(Dimens.buttonHeight),
        shape = RoundedCornerShape(Dimens.buttonRadius),
        colors = ButtonDefaults.buttonColors(
            containerColor = RelayBirdColors.White,
            contentColor = RelayBirdColors.Black,
        ),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun SetupCard(title: String, body: String, action: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.cardRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
                onClick = onClick,
                shape = RoundedCornerShape(Dimens.buttonRadius),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
            ) {
                Text(action, style = MaterialTheme.typography.labelLarge)
            }
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
