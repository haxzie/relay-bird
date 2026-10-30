package dev.haxzie.relaybird.net

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * The pairing and reconnect proofs from docs/PROTOCOL.md. HMAC-SHA256 throughout: there are
 * exactly two peers and no third party to verify a signature against, so asymmetric identity
 * keys would be pure overhead.
 *
 * Every context string is versioned so a future protocol change can never produce a colliding
 * MAC for a different purpose.
 */
object Proofs {
    private const val PAIR_CONTEXT = "relaybird-pair-v1"
    private const val CONFIRM_CONTEXT = "relaybird-confirm-v1"
    private const val AUTH_CONTEXT = "relaybird-auth-v1"

    const val KEY_BYTES = 32

    /** Proves the Mac holds the QR secret *and* saw this exact certificate. */
    fun pair(secret: ByteArray, spkiHash: ByteArray): ByteArray =
        hmac(secret, PAIR_CONTEXT.toByteArray() + spkiHash)

    /** Proves back to the Mac that the phone holds the secret, so it can safely commit the pin. */
    fun confirm(secret: ByteArray, spkiHash: ByteArray, macName: String): ByteArray =
        hmac(secret, CONFIRM_CONTEXT.toByteArray() + spkiHash + macName.toByteArray())

    fun auth(deviceKey: ByteArray, challenge: ByteArray, spkiHash: ByteArray): ByteArray =
        hmac(deviceKey, AUTH_CONTEXT.toByteArray() + challenge + spkiHash)

    fun randomKey(bytes: Int = KEY_BYTES): ByteArray =
        ByteArray(bytes).also { SecureRandom().nextBytes(it) }

    /** Constant time. */
    fun verify(expected: ByteArray, actual: ByteArray): Boolean = MessageDigest.isEqual(expected, actual)

    private fun hmac(key: ByteArray, message: ByteArray): ByteArray =
        Mac.getInstance("HmacSHA256").run {
            init(SecretKeySpec(key, "HmacSHA256"))
            doFinal(message)
        }
}
