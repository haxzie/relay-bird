package dev.haxzie.relaybird

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.haxzie.relaybird.ui.HomeScreen
import dev.haxzie.relaybird.ui.ScanScreen
import dev.haxzie.relaybird.ui.RelayBirdTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as RelayBirdApp).container
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
