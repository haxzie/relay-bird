package dev.haxzie.smsbridge

import android.app.Application
import dev.haxzie.smsbridge.service.BridgeContainer

class SmsBridgeApp : Application() {
    lateinit var container: BridgeContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = BridgeContainer(this)
    }
}
