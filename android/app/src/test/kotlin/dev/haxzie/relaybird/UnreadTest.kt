package dev.haxzie.relaybird

import dev.haxzie.relaybird.sms.Unread
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The phone's own read flags cannot be updated by this app, so the Mac's read marks are what
 * actually keep a conversation from reappearing as unread.
 */
class UnreadTest {
    @Test
    fun `counts every unread message when the Mac has read nothing`() {
        val counts = Unread.countsByThread(mapOf(1L to listOf(10L, 11L, 12L)), emptyMap())
        assertEquals(mapOf(1L to 3), counts)
    }

    @Test
    fun `discounts messages the Mac has already read`() {
        val counts = Unread.countsByThread(mapOf(1L to listOf(10L, 11L, 12L)), mapOf(1L to 11L))
        assertEquals("only the message past the mark counts", mapOf(1L to 1), counts)
    }

    @Test
    fun `a thread read to the end disappears entirely`() {
        val counts = Unread.countsByThread(mapOf(1L to listOf(10L, 11L, 12L)), mapOf(1L to 12L))
        assertTrue("a fully read thread must not report zero-unread noise", counts.isEmpty())
    }

    @Test
    fun `a watermark ahead of every message still reads as none`() {
        val counts = Unread.countsByThread(mapOf(1L to listOf(10L)), mapOf(1L to 9_999L))
        assertTrue(counts.isEmpty())
    }

    @Test
    fun `watermarks apply per thread`() {
        val counts = Unread.countsByThread(
            mapOf(1L to listOf(10L, 11L), 2L to listOf(20L, 21L)),
            mapOf(1L to 11L),
        )
        assertEquals(mapOf(2L to 2), counts)
    }

    @Test
    fun `a watermark for an unknown thread is ignored`() {
        val counts = Unread.countsByThread(mapOf(1L to listOf(10L)), mapOf(99L to 500L))
        assertEquals(mapOf(1L to 1), counts)
    }

    @Test
    fun `no unread messages means no counts`() {
        assertTrue(Unread.countsByThread(emptyMap(), mapOf(1L to 5L)).isEmpty())
    }
}
