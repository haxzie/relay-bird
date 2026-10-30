package dev.haxzie.relaybird

import dev.haxzie.relaybird.net.Proofs
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The vectors in docs/PROTOCOL.md, asserted here and in macos/RelayBirdTests/ProofsTests.swift.
 * This is the test that catches silent interop drift between the two implementations.
 */
class ProofsTest {
    private val secret = ByteArray(32) { it.toByte() }
    private val spkiHash = ByteArray(32) { (it + 0x20).toByte() }
    private val deviceKey = ByteArray(32) { (it + 0x40).toByte() }
    private val challenge = ByteArray(32) { (it + 0x60).toByte() }
    private val macName = "Test Mac"

    @Test
    fun `pair proof matches the shared vector`() {
        assertEquals(
            "ae5e1c9c5e34a70cdc313eba20ce9c2b4272ed81a8e381b0c187b85bb25f2255",
            Proofs.pair(secret, spkiHash).hex(),
        )
    }

    @Test
    fun `confirm proof matches the shared vector`() {
        assertEquals(
            "688b0e4cbbd809ad9cc27d1b76c628622e3f825130c4ce3d6b9fce3f383f6de2",
            Proofs.confirm(secret, spkiHash, macName).hex(),
        )
    }

    @Test
    fun `auth proof matches the shared vector`() {
        assertEquals(
            "490195dbe69816d467535d13a730bd765eee83aeea6caaff2a4a7b1c4c708f51",
            Proofs.auth(deviceKey, challenge, spkiHash).hex(),
        )
    }

    @Test
    fun `pair proof is bound to the certificate`() {
        // A man in the middle presents a different certificate, so its proof cannot match.
        val otherSpki = spkiHash.copyOf().also { it[0] = 0xff.toByte() }
        assertFalse(
            Proofs.verify(
                Proofs.pair(secret, spkiHash),
                Proofs.pair(secret, otherSpki),
            ),
        )
    }

    @Test
    fun `auth proof is bound to the challenge`() {
        val replayed = Proofs.auth(deviceKey, challenge, spkiHash)
        val fresh = Proofs.auth(deviceKey, ByteArray(32) { 0x11 }, spkiHash)
        assertFalse(Proofs.verify(fresh, replayed))
    }

    @Test
    fun `contexts are domain separated`() {
        // Same key and same trailing bytes, different purpose: must never collide.
        assertFalse(Proofs.verify(Proofs.pair(secret, spkiHash), Proofs.auth(secret, ByteArray(0), spkiHash)))
    }

    @Test
    fun `random keys are 32 bytes and distinct`() {
        val a = Proofs.randomKey()
        val b = Proofs.randomKey()
        assertEquals(32, a.size)
        assertFalse(Proofs.verify(a, b))
        assertTrue(a.any { it != 0.toByte() })
    }

    @Test
    fun `verify is reflexive`() {
        val proof = Proofs.pair(secret, spkiHash)
        assertArrayEquals(proof, Proofs.pair(secret, spkiHash))
        assertTrue(Proofs.verify(proof, Proofs.pair(secret, spkiHash)))
    }

    private fun ByteArray.hex() = joinToString("") { "%02x".format(it) }
}
