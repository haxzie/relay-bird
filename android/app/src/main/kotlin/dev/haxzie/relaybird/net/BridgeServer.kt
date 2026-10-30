package dev.haxzie.relaybird.net

import android.util.Base64
import android.util.Log
import dev.haxzie.relaybird.sms.SmsRepository
import dev.haxzie.relaybird.store.PairedDevice
import dev.haxzie.relaybird.store.PairedDevices
import dev.haxzie.relaybird.store.PendingPairingStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.EOFException
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.Collections
import javax.net.ssl.SSLServerSocket
import javax.net.ssl.SSLSocket

/**
 * The phone side of the bridge: a TLS server with one [Session] per connected Mac.
 *
 * Plain SSLServerSocket rather than an HTTP/WebSocket stack. Ktor's CIO engine throws on any
 * HTTPS connector and its keystore builder cannot run on Android, and with length-prefixed JSON
 * there is nothing a WebSocket layer would add.
 */
class BridgeServer(
    private val identity: ServerIdentity,
    private val devices: PairedDevices,
    private val pending: PendingPairingStore,
    private val sms: SmsRepository,
    private val deviceName: String,
) {
    private val _port = MutableStateFlow<Int?>(null)
    val port: StateFlow<Int?> = _port.asStateFlow()

    private val _connected = MutableStateFlow<List<String>>(emptyList())
    val connected: StateFlow<List<String>> = _connected.asStateFlow()

    private val sessions = Collections.synchronizedList(mutableListOf<Session>())
    private var serverSocket: SSLServerSocket? = null
    private var acceptJob: Job? = null

    fun start(scope: CoroutineScope) {
        if (acceptJob != null) return
        acceptJob = scope.launch(Dispatchers.IO) {
            try {
                val socket = (identity.sslContext.serverSocketFactory.createServerSocket(0) as SSLServerSocket)
                    .apply { enabledProtocols = supportedProtocols.filter { it.startsWith("TLSv1.2") || it.startsWith("TLSv1.3") }.toTypedArray() }
                serverSocket = socket
                _port.value = socket.localPort
                Log.i(TAG, "listening on ${socket.localPort}")
                while (isActive) {
                    val client = socket.accept() as SSLSocket
                    val session = Session(client)
                    sessions += session
                    scope.launch(Dispatchers.IO) { session.run() }
                }
            } catch (e: IOException) {
                if (isActive) Log.e(TAG, "accept loop died", e)
            } finally {
                _port.value = null
            }
        }
    }

    fun stop() {
        acceptJob?.cancel()
        acceptJob = null
        runCatching { serverSocket?.close() }
        serverSocket = null
        synchronized(sessions) { sessions.toList() }.forEach { it.close() }
    }

    /** Push a newly arrived SMS to every authenticated Mac. */
    fun broadcast(message: WireMessage) {
        synchronized(sessions) { sessions.toList() }
            .filter { it.authenticatedAs != null }
            .forEach { it.send(NewMessage(message)) }
    }

    /** Drops a Mac's live session the moment it is revoked, rather than at the next reconnect. */
    fun disconnect(macName: String) {
        synchronized(sessions) { sessions.toList() }
            .filter { it.authenticatedAs == macName }
            .forEach {
                it.send(ErrorFrame(ErrorCode.REVOKED, "this Mac was removed from the phone"))
                it.close()
            }
    }

    private fun refreshConnected() {
        _connected.value = synchronized(sessions) { sessions.mapNotNull { it.authenticatedAs } }
    }

    private inner class Session(private val socket: SSLSocket) {
        private val challenge = Proofs.randomKey()
        var authenticatedAs: String? = null
            private set

        private val input by lazy { socket.inputStream.buffered() }
        private val output by lazy { socket.outputStream.buffered() }
        private var heartbeat: Job? = null

        fun run() {
            try {
                socket.soTimeout = READ_TIMEOUT_MILLIS
                send(
                    Hello(
                        deviceId = devices.deviceId,
                        deviceName = deviceName,
                        paired = devices.devices.value.isNotEmpty(),
                        challenge = challenge.base64(),
                    ),
                )
                while (true) {
                    val frame = Framing.read(input)
                    if (authenticatedAs == null) {
                        if (!handshake(frame)) return
                    } else {
                        handle(frame)
                    }
                }
            } catch (e: EOFException) {
                Log.i(TAG, "session closed by peer")
            } catch (e: SocketTimeoutException) {
                Log.i(TAG, "session timed out")
            } catch (e: Exception) {
                Log.w(TAG, "session error: ${e.message}")
            } finally {
                close()
            }
        }

        /** Returns false when the connection should be torn down. */
        private fun handshake(frame: Frame): Boolean = when (frame) {
            is Pair -> pair(frame)
            is Auth -> auth(frame)
            Ping -> { send(Pong); true }
            else -> {
                send(ErrorFrame(ErrorCode.UNPAIRED, "authenticate first"))
                false
            }
        }

        private fun pair(frame: Pair): Boolean {
            val offer = pending.current()
            if (offer == null) {
                send(ErrorFrame(ErrorCode.UNPAIRED, "no pairing in progress — scan the QR code again"))
                return false
            }
            val expected = Proofs.pair(offer.secret, identity.spkiHash)
            if (!Proofs.verify(expected, frame.proof.decodeBase64())) {
                // Either the wrong secret, or a man in the middle whose certificate differs from
                // the one the Mac hashed into its proof.
                send(ErrorFrame(ErrorCode.BAD_PROOF, "pairing proof rejected"))
                return false
            }

            val deviceKey = Proofs.randomKey()
            val now = System.currentTimeMillis()
            devices.add(PairedDevice(offer.macName, deviceKey.base64(), now, now))
            send(
                Paired(
                    confirm = Proofs.confirm(offer.secret, identity.spkiHash, offer.macName).base64(),
                    deviceKey = deviceKey.base64(),
                ),
            )
            pending.clear()
            authenticate(offer.macName)
            return true
        }

        private fun auth(frame: Auth): Boolean {
            val proof = frame.proof.decodeBase64()
            val device = devices.findByProof { key ->
                Proofs.verify(Proofs.auth(key, challenge, identity.spkiHash), proof)
            }
            if (device == null) {
                send(ErrorFrame(ErrorCode.UNPAIRED, "this Mac is not paired with this phone"))
                return false
            }
            devices.touch(device.name)
            authenticate(device.name)
            return true
        }

        private fun authenticate(macName: String) {
            authenticatedAs = macName
            refreshConnected()
            send(Ready)
            heartbeat = CoroutineScope(Dispatchers.IO).launch {
                while (socket.isConnected && !socket.isClosed) {
                    delay(HEARTBEAT_MILLIS)
                    send(Ping)
                }
            }
            Log.i(TAG, "authenticated $macName")
        }

        private fun handle(frame: Frame) {
            try {
                when (frame) {
                    is Sync -> send(
                        Threads(
                            sms.threads(
                                limit = frame.limit,
                                watermarks = frame.watermarks.associate { it.threadId to it.lastReadId },
                            ),
                        ),
                    )
                    is ThreadRequest -> send(
                        Messages(frame.threadId, sms.messages(frame.threadId, frame.limit, frame.beforeId)),
                    )
                    is MarkRead -> sms.markThreadRead(frame.threadId)
                    Ping -> send(Pong)
                    Pong -> Unit
                    else -> send(ErrorFrame(ErrorCode.PROTOCOL, "unexpected frame ${frame::class.simpleName}"))
                }
            } catch (e: SecurityException) {
                send(ErrorFrame(ErrorCode.NO_PERMISSION, "SMS permission not granted on the phone"))
            }
        }

        fun send(frame: Frame) {
            runCatching { Framing.write(output, frame) }
                .onFailure { Log.w(TAG, "write failed: ${it.message}") }
        }

        fun close() {
            heartbeat?.cancel()
            runCatching { socket.close() }
            sessions.remove(this)
            authenticatedAs = null
            refreshConnected()
        }
    }

    private companion object {
        const val TAG = "BridgeServer"
        const val HEARTBEAT_MILLIS = 20_000L
        const val READ_TIMEOUT_MILLIS = 60_000
    }
}

private fun ByteArray.base64(): String = Base64.encodeToString(this, Base64.NO_WRAP)
private fun String.decodeBase64(): ByteArray = Base64.decode(this, Base64.NO_WRAP)
