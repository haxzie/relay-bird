package dev.haxzie.relaybird.net

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Wire protocol v1. Mirrored by hand in macos/RelayBird/Net/Protocol.swift — see
 * docs/PROTOCOL.md, which is the source of truth for both.
 */
const val PROTOCOL_VERSION = 2

val protocolJson: Json = Json {
    classDiscriminator = "t"
    ignoreUnknownKeys = true
    encodeDefaults = true
}

@Serializable
data class WireMessage(
    val id: Long,
    val threadId: Long,
    val address: String,
    val body: String,
    val date: Long,
    val outgoing: Boolean,
    val read: Boolean,
)

@Serializable
data class WireThread(
    val threadId: Long,
    val address: String,
    val displayName: String? = null,
    val snippet: String,
    val date: Long,
    /** Already excludes anything the Mac told us it had read — see [Watermark]. */
    val unread: Int,
    /** Newest message in the thread, so the Mac can tell when its read mark is current. */
    val latestMessageId: Long = 0,
)

/**
 * How far the Mac has read in a thread.
 *
 * The Mac has to track this itself: a non-default SMS app cannot write to the SMS provider, so
 * marking a conversation read here does not stick, and every resync would otherwise report the
 * same messages as unread forever.
 */
@Serializable
data class Watermark(val threadId: Long, val lastReadId: Long)

@Serializable
sealed interface Frame

// ---- phone -> mac ----

@Serializable
@SerialName("hello")
data class Hello(
    val v: Int = PROTOCOL_VERSION,
    val deviceId: String,
    val deviceName: String,
    val paired: Boolean,
    /** base64, 32 bytes, fresh per connection. */
    val challenge: String,
) : Frame

@Serializable
@SerialName("paired")
data class Paired(val confirm: String, val deviceKey: String) : Frame

@Serializable
@SerialName("ready")
data object Ready : Frame

@Serializable
@SerialName("threads")
data class Threads(val threads: List<WireThread>) : Frame

@Serializable
@SerialName("messages")
data class Messages(val threadId: Long, val messages: List<WireMessage>) : Frame

@Serializable
@SerialName("message")
data class NewMessage(val message: WireMessage) : Frame

@Serializable
@SerialName("error")
data class ErrorFrame(val code: String, val msg: String) : Frame

// ---- mac -> phone ----

@Serializable
@SerialName("pair")
data class Pair(val name: String, val proof: String) : Frame

@Serializable
@SerialName("auth")
data class Auth(val proof: String) : Frame

@Serializable
@SerialName("sync")
data class Sync(
    val limit: Int = 50,
    val watermarks: List<Watermark> = emptyList(),
) : Frame

@Serializable
@SerialName("thread")
data class ThreadRequest(val threadId: Long, val limit: Int = 100, val beforeId: Long? = null) : Frame

@Serializable
@SerialName("markRead")
data class MarkRead(val threadId: Long, val upToId: Long = 0) : Frame

// ---- both ----

@Serializable
@SerialName("ping")
data object Ping : Frame

@Serializable
@SerialName("pong")
data object Pong : Frame

object ErrorCode {
    const val UNPAIRED = "unpaired"
    const val BAD_PROOF = "bad_proof"
    const val PROTOCOL = "protocol"
    const val NO_PERMISSION = "no_permission"
    const val REVOKED = "revoked"
}
