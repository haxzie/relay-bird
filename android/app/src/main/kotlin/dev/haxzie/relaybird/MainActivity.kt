package dev.haxzie.relaybird

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.haxzie.relaybird.service.BridgeService
import dev.haxzie.relaybird.ui.HomeScreen
import dev.haxzie.relaybird.ui.ScanScreen
import dev.haxzie.relaybird.ui.RelayBirdTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The app is dark-only, so the system bars are told to use light content rather
        // than following the device theme.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        val container = (application as RelayBirdApp).container
        // An activity is a foreground context, so this is a legal place to start the service
        // again after an update stopped it.
        if (container.mirroringWanted && !container.running.value) {
            BridgeService.start(this)
        }
        setContent {
            RelayBirdTheme {
                var scanning by remember { mutableStateOf(false) }
                if (scanning) {
                    ScanScreen(
                        container = container,
                        onDone = { scanning = false },
                    )
                } else {
                    HomeScreen(
                        container = container,
                        onPairMac = { scanning = true },
                    )
                }
            }
        }
    }
}
