package dev.haxzie.relaybird

import android.app.Application
import dev.haxzie.relaybird.service.BridgeContainer

class RelayBirdApp : Application() {
    lateinit var container: BridgeContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = BridgeContainer(this)
    }
}
