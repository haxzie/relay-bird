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

    /** Last error surfaced by the service, shown on the home screen. */
    val lastError = MutableStateFlow<String?>(null)
}
