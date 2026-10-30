package dev.haxzie.relaybird.net

import java.io.EOFException
import java.io.InputStream
import java.io.OutputStream

/** UInt32 big-endian length prefix, then that many bytes of UTF-8 JSON. */
object Framing {
    const val MAX_FRAME_BYTES = 1 shl 20

    fun write(out: OutputStream, frame: Frame) {
        val body = protocolJson.encodeToString(Frame.serializer(), frame).toByteArray(Charsets.UTF_8)
        require(body.size <= MAX_FRAME_BYTES) { "frame too large: ${body.size}" }
        val header = byteArrayOf(
            (body.size ushr 24).toByte(),
            (body.size ushr 16).toByte(),
            (body.size ushr 8).toByte(),
            body.size.toByte(),
        )
        synchronized(out) {
            out.write(header)
            out.write(body)
            out.flush()
        }
    }

    /** Blocks until a whole frame arrives. Throws [EOFException] on a clean peer close. */
    fun read(input: InputStream): Frame {
        val header = readFully(input, 4)
        val length = ((header[0].toInt() and 0xff) shl 24) or
            ((header[1].toInt() and 0xff) shl 16) or
            ((header[2].toInt() and 0xff) shl 8) or
            (header[3].toInt() and 0xff)
        if (length <= 0 || length > MAX_FRAME_BYTES) {
            throw ProtocolException("declared frame length $length out of range")
        }
        val body = readFully(input, length)
        return protocolJson.decodeFromString(Frame.serializer(), body.toString(Charsets.UTF_8))
    }

    private fun readFully(input: InputStream, n: Int): ByteArray {
        val buf = ByteArray(n)
        var read = 0
        while (read < n) {
            val r = input.read(buf, read, n - read)
            if (r < 0) throw EOFException("peer closed after $read of $n bytes")
            read += r
        }
        return buf
    }
}

class ProtocolException(message: String) : Exception(message)
