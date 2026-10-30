package dev.haxzie.relaybird.store

import android.util.Base64
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A scanned-but-not-yet-completed pairing. Deliberately in memory only and short lived: the QR
 * secret is a bearer token and must not outlive the pairing attempt.
 */
data class PendingPairing(
    val secret: ByteArray,
    val macName: String,
    val expiresAt: Long,
) {
    val expired: Boolean get() = System.currentTimeMillis() > expiresAt

    // Data class equals on a ByteArray compares identity, which would be a silent trap.
    override fun equals(other: Any?) = this === other
    override fun hashCode() = System.identityHashCode(this)
}

class PendingPairingStore {
    private val _pending = MutableStateFlow<PendingPairing?>(null)
    val pending: StateFlow<PendingPairing?> = _pending.asStateFlow()

    fun offer(secretBase64Url: String, macName: String, ttlMillis: Long = DEFAULT_TTL_MILLIS) {
        val secret = Base64.decode(secretBase64Url, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        _pending.value = PendingPairing(secret, macName, System.currentTimeMillis() + ttlMillis)
    }

    /** Returns the live pending pairing, clearing it if it has expired. */
    fun current(): PendingPairing? = _pending.value?.takeUnless { it.expired }.also {
        if (it == null) clear()
    }

    fun clear() {
        _pending.value?.secret?.fill(0)
        _pending.value = null
    }

    companion object {
        const val DEFAULT_TTL_MILLIS = 120_000L
    }
}
