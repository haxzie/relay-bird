package dev.haxzie.relaybird

import dev.haxzie.relaybird.net.Framing
import dev.haxzie.relaybird.net.Hello
import dev.haxzie.relaybird.net.Ping
import dev.haxzie.relaybird.net.ProtocolException
import dev.haxzie.relaybird.net.Sync
import dev.haxzie.relaybird.net.ThreadRequest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.EOFException

class FramingTest {
    @Test
    fun `round trips a frame`() {
        val frame = Hello(deviceId = "abc", deviceName = "Pixel", paired = true, challenge = "AAAA")
        val bytes = ByteArrayOutputStream().also { Framing.write(it, frame) }.toByteArray()
        assertEquals(frame, Framing.read(ByteArrayInputStream(bytes)))
    }

    @Test
    fun `round trips several frames over one stream`() {
        val out = ByteArrayOutputStream()
        Framing.write(out, Ping)
        Framing.write(out, Sync(limit = 7))
        Framing.write(out, ThreadRequest(threadId = 3, limit = 10, beforeId = 99))
        val input = ByteArrayInputStream(out.toByteArray())
        assertEquals(Ping, Framing.read(input))
        assertEquals(Sync(7), Framing.read(input))
        assertEquals(ThreadRequest(3, 10, 99), Framing.read(input))
    }

    @Test
    fun `length prefix is big endian`() {
        val bytes = ByteArrayOutputStream().also { Framing.write(it, Ping) }.toByteArray()
        val declared = ((bytes[0].toInt() and 0xff) shl 24) or
            ((bytes[1].toInt() and 0xff) shl 16) or
            ((bytes[2].toInt() and 0xff) shl 8) or
            (bytes[3].toInt() and 0xff)
        assertEquals(bytes.size - 4, declared)
    }

    @Test(expected = ProtocolException::class)
    fun `rejects an oversized declared length`() {
        val header = byteArrayOf(0x7f, 0xff.toByte(), 0xff.toByte(), 0xff.toByte())
        Framing.read(ByteArrayInputStream(header))
    }

    @Test(expected = ProtocolException::class)
    fun `rejects a zero length`() {
        Framing.read(ByteArrayInputStream(byteArrayOf(0, 0, 0, 0)))
    }

    @Test(expected = EOFException::class)
    fun `reports a truncated frame`() {
        val bytes = ByteArrayOutputStream().also { Framing.write(it, Ping) }.toByteArray()
        Framing.read(ByteArrayInputStream(bytes.copyOf(bytes.size - 1)))
    }
}
