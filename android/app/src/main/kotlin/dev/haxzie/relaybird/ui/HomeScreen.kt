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
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
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
            // The illustration sits on its own panel so it reads as a header rather than
            // floating in the card's padding.
            LinkIllustration(
                linked = linked,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(
                            topStart = Dimens.cardRadius,
                            topEnd = Dimens.cardRadius,
                        ),
                    )
                    .padding(vertical = 34.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
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
 * What the card leads with.
 *
 * Connected shows the paired devices with a tick over them — one mark that says it is working.
 * Otherwise a phone and a Mac with a gap of dots between them, which is the same picture
 * waiting to be completed.
 */
@Composable
private fun LinkIllustration(linked: Boolean, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Crossfade(targetState = linked, label = "linkState") { connected ->
            if (connected) ConnectedMark() else WaitingLink()
        }
    }
}

@Composable
private fun ConnectedMark() {
    Box(contentAlignment = Alignment.Center) {
        Ripples()
        // The same tile the phone and Mac sit in when waiting, so the two states are the same
        // object rather than two different drawings. The badge overhangs it, hence the padding.
        Box(modifier = Modifier.padding(6.dp)) {
            Endpoint(
                painter = painterResource(R.drawable.ic_devices),
                description = "Phone and Mac connected",
                tint = RelayBirdColors.White,
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 7.dp, y = (-7).dp)
                    .size(22.dp)
                    .background(RelayBirdColors.Green, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = RelayBirdColors.Black,
                    modifier = Modifier.size(15.dp),
                )
            }
        }
    }
}

/**
 * Circles travelling outwards from the devices while the link is live.
 *
 * Staggered by even fractions of one period so a ring leaves as the last is fading, which
 * reads as continuous rather than as a pulse with a gap in it. Each fades as it grows, so the
 * outermost is nearly gone by the time it reaches its limit and there is no hard edge.
 */
@Composable
private fun Ripples(count: Int = 3) {
    val transition = rememberInfiniteTransition(label = "ripples")
    repeat(count) { index ->
        val progress by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = RIPPLE_MILLIS, easing = LinearEasing),
                initialStartOffset = StartOffset(index * RIPPLE_MILLIS / count),
            ),
            label = "ripple$index",
        )
        Box(
            modifier = Modifier
                .size(lerp(RIPPLE_FROM, RIPPLE_TO, progress))
                .border(
                    width = 1.5.dp,
                    color = RelayBirdColors.Green.copy(alpha = (1f - progress) * 0.45f),
                    shape = CircleShape,
                ),
        )
    }
}

@Composable
private fun WaitingLink() {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Endpoint(icon = Icons.Default.Smartphone, description = "Phone")
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(DOT_COUNT) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(RelayBirdColors.Border, CircleShape),
                )
            }
        }
        Endpoint(icon = Icons.Default.LaptopMac, description = "Mac")
    }
}

@Composable
private fun Endpoint(
    description: String,
    tint: Color = RelayBirdColors.Muted,
    icon: ImageVector? = null,
    painter: Painter? = null,
) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center,
    ) {
        val iconModifier = Modifier.size(24.dp)
        if (painter != null) {
            Icon(painter = painter, contentDescription = description, tint = tint, modifier = iconModifier)
        } else if (icon != null) {
            Icon(imageVector = icon, contentDescription = description, tint = tint, modifier = iconModifier)
        }
    }
}

private const val DOT_COUNT = 5
private const val RIPPLE_MILLIS = 2600
private val RIPPLE_FROM = 54.dp
private val RIPPLE_TO = 112.dp

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
