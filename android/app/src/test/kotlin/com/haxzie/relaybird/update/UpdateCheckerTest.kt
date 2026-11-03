package com.haxzie.relaybird.update

import com.haxzie.relaybird.update.UpdateChecker.Companion.isNewer
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {

    @Test
    fun `offers a newer patch, minor and major`() {
        assertTrue(isNewer("1.2.1", "1.2.0"))
        assertTrue(isNewer("1.3.0", "1.2.9"))
        assertTrue(isNewer("2.0.0", "1.99.99"))
    }

    @Test
    fun `does not offer the same or an older version`() {
        assertFalse(isNewer("1.2.0", "1.2.0"))
        assertFalse(isNewer("1.1.9", "1.2.0"))
        assertFalse(isNewer("1.2.0", "2.0.0"))
    }

    /**
     * The reason this is compared numerically. Lexically "1.10.0" < "1.9.0", so a string
     * compare would stop offering updates after the ninth minor and nobody would notice
     * until the releases stopped landing.
     */
    @Test
    fun `compares components numerically, not lexically`() {
        assertTrue(isNewer("1.10.0", "1.9.0"))
        assertTrue(isNewer("1.2.10", "1.2.9"))
        assertFalse(isNewer("1.9.0", "1.10.0"))
    }

    @Test
    fun `tolerates a v prefix and release suffixes`() {
        assertTrue(isNewer("v1.3.0", "1.2.0"))
        assertFalse(isNewer("1.2.0-beta.1", "1.2.0"))
        assertTrue(isNewer("1.3.0+build7", "1.2.0"))
    }

    @Test
    fun `treats unparseable input as not newer rather than nagging`() {
        assertFalse(isNewer("", "1.2.0"))
        assertFalse(isNewer("latest", "1.2.0"))
        assertFalse(isNewer("1.2.0", "unknown"))
    }

    @Test
    fun `handles versions with different component counts`() {
        assertTrue(isNewer("1.3", "1.2.9"))
        assertFalse(isNewer("1.2", "1.2.0"))
        assertTrue(isNewer("1.2.0.1", "1.2.0"))
    }
}
