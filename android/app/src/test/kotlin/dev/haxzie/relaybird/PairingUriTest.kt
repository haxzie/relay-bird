package dev.haxzie.relaybird

import dev.haxzie.relaybird.net.PairingUri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Base64

class PairingUriTest {
    @Test
    fun `parses a well formed code`() {
        val parsed = PairingUri.parse("relaybird://pair?v=1&name=Haxzie%27s%20MacBook&s=AAECAw")
        assertEquals("Haxzie's MacBook", parsed?.macName)
        assertEquals("AAECAw", parsed?.secretBase64Url)
    }

    @Test
    fun `keeps a base64url secret intact`() {
        // '-' and '_' are significant in base64url and must survive decoding.
        val parsed = PairingUri.parse("relaybird://pair?v=1&name=Mac&s=a-b_c0")
        assertEquals("a-b_c0", parsed?.secretBase64Url)
    }

    @Test
    fun `parses a code produced by the real Mac generator`() {
        // Captured verbatim from macos PairingCode.generate(), so a change to either side's
        // encoding breaks this rather than breaking pairing on a user's desk.
        val parsed = PairingUri.parse(
            "relaybird://pair?v=1&name=Haxzie%27s%20MacBook%20Pro&s=QlXF4_ODcYx4VJWk6y_Kqs5T86ECI6-nvBPk7bnAq_8",
        )
        assertEquals("Haxzie's MacBook Pro", parsed?.macName)
        assertEquals(
            "the Mac sends 32 bytes as unpadded base64url",
            32,
            Base64.getUrlDecoder().decode(parsed?.secretBase64Url).size,
        )
    }

    @Test
    fun `rejects other schemes, hosts and versions`() {
        assertNull(PairingUri.parse("https://pair?v=1&name=Mac&s=AAECAw"))
        assertNull(PairingUri.parse("relaybird://connect?v=1&name=Mac&s=AAECAw"))
        assertNull(PairingUri.parse("relaybird://pair?v=2&name=Mac&s=AAECAw"))
    }

    @Test
    fun `rejects missing fields`() {
        assertNull(PairingUri.parse("relaybird://pair?v=1&name=Mac"))
        assertNull(PairingUri.parse("relaybird://pair?v=1&s=AAECAw"))
        assertNull(PairingUri.parse("not a uri at all"))
    }
}
