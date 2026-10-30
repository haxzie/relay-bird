package dev.haxzie.relaybird.service

import android.content.Context
import android.os.Build
import dev.haxzie.relaybird.net.BridgeServer
import dev.haxzie.relaybird.net.ServerIdentity
import dev.haxzie.relaybird.sms.ContactNames
import dev.haxzie.relaybird.sms.SmsRepository
import dev.haxzie.relaybird.store.PairedDevices
import dev.haxzie.relaybird.store.PendingPairingStore
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Hand-rolled container, shared by the service and the UI. The app is small enough that a DI
 * framework would be more moving parts than it removes.
 */
class BridgeContainer(private val context: Context) {
    val devices = PairedDevices(context)
    val pending = PendingPairingStore()
    val contacts = ContactNames(context)
    val sms = SmsRepository(context, contacts)

    val deviceName: String = listOf(Build.MANUFACTURER, Build.MODEL)
        .filter { it.isNotBlank() }
        .joinToString(" ")
        .ifBlank { "Android phone" }
        .replaceFirstChar { it.uppercase() }

    val identity: ServerIdentity by lazy { ServerIdentity.load(context) }

    val server: BridgeServer by lazy {
        BridgeServer(identity, devices, pending, sms, deviceName)
    }

    /** True while [BridgeService] is running, so the UI can show the real state. */
    val running = MutableStateFlow(false)

    private val prefs = context.getSharedPreferences("relaybird_state", Context.MODE_PRIVATE)

    /**
     * Whether the user wants mirroring on, as distinct from whether it is on right now.
     *
     * Installing an APK force-stops the app, and Android does not restart the services of a
     * force-stopped app, so without remembering this an update would quietly leave mirroring
     * off until someone noticed and toggled it again.
     */
    var mirroringWanted: Boolean
        get() = prefs.getBoolean(KEY_MIRRORING_WANTED, false)
        set(value) = prefs.edit().putBoolean(KEY_MIRRORING_WANTED, value).apply()

    private companion object {
        const val KEY_MIRRORING_WANTED = "mirroring_wanted"
    }

    /** Last error surfaced by the service, shown on the home screen. */
    val lastError = MutableStateFlow<String?>(null)
}
