package dev.haxzie.relaybird

import android.app.Application
import dev.haxzie.relaybird.service.BridgeContainer
import dev.haxzie.relaybird.service.Notifications

class RelayBirdApp : Application() {
    lateinit var container: BridgeContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = BridgeContainer(this)
        // Here rather than only in the service: channel settings are fixed at creation, so a
        // build that republishes a channel has to get the chance to do it whenever the process
        // starts, not just when mirroring happens to be switched on.
        Notifications.ensureChannels(this)
    }
}
