package dev.haxzie.smsbridge.store

import android.content.Context
import android.util.Base64
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.UUID

@Serializable
data class PairedDevice(
    val name: String,
    /** base64 of the 32-byte shared device key. */
    val key: String,
    val pairedAt: Long,
    val lastSeen: Long,
) {
    val keyBytes: ByteArray get() = Base64.decode(key, Base64.NO_WRAP)
}

/**
 * Paired Macs and their device keys. Each key is a bearer credential for the whole SMS
 * database, so they live in a [SecureStore] rather than plain prefs.
 */
class PairedDevices(context: Context) {
    private val store = SecureStore(context, "paired_devices")
    private val json = Json { ignoreUnknownKeys = true }

    private val _devices = MutableStateFlow(read())
    val devices: StateFlow<List<PairedDevice>> = _devices.asStateFlow()

    /** Stable id advertised in the Bonjour TXT record so a Mac can recognise this phone. */
    val deviceId: String
        get() = store.getString(KEY_DEVICE_ID) ?: UUID.randomUUID().toString().also {
            store.putString(KEY_DEVICE_ID, it)
        }

    fun add(device: PairedDevice) = update { it.filterNot { d -> d.name == device.name } + device }

    fun remove(name: String) = update { it.filterNot { d -> d.name == name } }

    fun touch(name: String) = update { list ->
        list.map { if (it.name == name) it.copy(lastSeen = System.currentTimeMillis()) else it }
    }

    fun find(name: String): PairedDevice? = _devices.value.firstOrNull { it.name == name }

    /** Any paired key that validates [verify]. Reconnects do not announce which Mac they are. */
    fun findByProof(verify: (ByteArray) -> Boolean): PairedDevice? =
        _devices.value.firstOrNull { verify(it.keyBytes) }

    private fun update(transform: (List<PairedDevice>) -> List<PairedDevice>) {
        val next = transform(_devices.value)
        store.putString(KEY_DEVICES, json.encodeToString(next))
        _devices.value = next
    }

    private fun read(): List<PairedDevice> =
        store.getString(KEY_DEVICES)?.let {
            runCatching { json.decodeFromString<List<PairedDevice>>(it) }.getOrNull()
        } ?: emptyList()

    private companion object {
        const val KEY_DEVICES = "devices"
        const val KEY_DEVICE_ID = "device_id"
    }
}
