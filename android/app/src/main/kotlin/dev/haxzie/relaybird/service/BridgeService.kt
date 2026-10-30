package dev.haxzie.relaybird.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import dev.haxzie.relaybird.RelayBirdApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

/**
 * Holds the TLS server, the mDNS advertisement and the Wi-Fi locks open for as long as
 * mirroring is on.
 *
 * The service type is `remoteMessaging`, which Android documents for exactly this ("transfer
 * text messages from one device to another") and which has no runtime prerequisites and no
 * timeout. `dataSync` would be killed after 6 hours per day on Android 15+ and then refuse to
 * restart, and `specialUse` is for cases the other types do not cover.
 */
class BridgeService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var container: BridgeContainer
    private lateinit var advertiser: NsdAdvertiser
    private var watcher: SmsWatcher? = null
    private var multicastLock: WifiManager.MulticastLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    override fun onCreate() {
        super.onCreate()
        container = (application as RelayBirdApp).container
        advertiser = NsdAdvertiser(this)
        Notifications.ensureChannels(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            container.mirroringWanted = false
            stopSelf()
            return START_NOT_STICKY
        }
        container.mirroringWanted = true
        startForegroundNotification()

        if (container.running.value) return START_STICKY
        container.running.value = true
        container.lastError.value = null

        acquireWifiLocks()
        container.contacts.startWatching()

        try {
            container.server.start(scope)
        } catch (e: Exception) {
            Log.e(TAG, "server failed to start", e)
            container.lastError.value = "Could not start the bridge: ${e.message}"
            stopSelf()
            return START_NOT_STICKY
        }

        watcher = SmsWatcher(this, container.sms) { messages ->
            Log.i(TAG, "pushing ${messages.size} new message(s) to ${container.server.connected.value.size} Mac(s)")
            messages.forEach { container.server.broadcast(it) }
        }.also { it.start() }

        scope.launch {
            container.server.port.filterNotNull().collect { port ->
                advertiser.register(port, container.devices.deviceId, container.deviceName)
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        advertiser.unregister()
        watcher?.stop()
        container.server.stop()
        container.contacts.stopWatching()
        releaseWifiLocks()
        container.running.value = false
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Posted once and never updated.
     *
     * It used to track the connection, which meant re-posting on every change — and the Mac
     * races several endpoints per reconnect, so that was constant. Re-posting a notification
     * the user has dismissed brings it straight back, which made the swipe Android 14 added
     * useless here. The live status belongs in the app and on the Mac; this notification only
     * exists because a foreground service must have one.
     */
    private fun startForegroundNotification() {
        val notification = Notifications.foreground(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                Notifications.ID_FOREGROUND,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING,
            )
        } else {
            startForeground(Notifications.ID_FOREGROUND, notification)
        }
    }

    /**
     * Without the multicast lock the Wi-Fi chip drops multicast frames in hardware once the
     * screen goes off, so inbound mDNS queries never reach the system responder and the phone
     * becomes invisible to the Mac while asleep.
     */
    private fun acquireWifiLocks() {
        val wifi = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        multicastLock = wifi.createMulticastLock("relaybird").apply {
            setReferenceCounted(true)
            acquire()
        }
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            WifiManager.WIFI_MODE_FULL_LOW_LATENCY
        } else {
            @Suppress("DEPRECATION")
            WifiManager.WIFI_MODE_FULL_HIGH_PERF
        }
        wifiLock = wifi.createWifiLock(mode, "relaybird").apply {
            setReferenceCounted(true)
            acquire()
        }
    }

    private fun releaseWifiLocks() {
        multicastLock?.takeIf { it.isHeld }?.release()
        wifiLock?.takeIf { it.isHeld }?.release()
        multicastLock = null
        wifiLock = null
    }

    companion object {
        private const val TAG = "BridgeService"
        const val ACTION_STOP = "dev.haxzie.relaybird.STOP"

        fun start(context: Context) {
            context.startForegroundService(Intent(context, BridgeService::class.java))
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, BridgeService::class.java).setAction(ACTION_STOP),
            )
        }
    }
}
