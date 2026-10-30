package dev.haxzie.smsbridge

import android.util.Base64
import androidx.test.platform.app.InstrumentationRegistry
import dev.haxzie.smsbridge.net.Auth
import dev.haxzie.smsbridge.net.BridgeServer
import dev.haxzie.smsbridge.net.ErrorCode
import dev.haxzie.smsbridge.net.ErrorFrame
import dev.haxzie.smsbridge.net.Frame
import dev.haxzie.smsbridge.net.Framing
import dev.haxzie.smsbridge.net.Hello
import dev.haxzie.smsbridge.net.Pair as PairFrame
import dev.haxzie.smsbridge.net.Paired
import dev.haxzie.smsbridge.net.Proofs
import dev.haxzie.smsbridge.net.Ready
import dev.haxzie.smsbridge.net.ServerIdentity
import dev.haxzie.smsbridge.net.Sync
import dev.haxzie.smsbridge.net.Threads
import dev.haxzie.smsbridge.sms.ContactNames
import dev.haxzie.smsbridge.sms.SmsRepository
import dev.haxzie.smsbridge.store.PairedDevices
import dev.haxzie.smsbridge.store.PendingPairingStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.security.MessageDigest
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.X509TrustManager

/**
 * Drives the real [BridgeServer] over a real TLS socket, playing the Mac's side of the
 * handshake from docs/PROTOCOL.md.
 *
 * Runs on a device because the two things most likely to break only exist there: the
 * AndroidKeyStore-generated certificate, and whether its opaque key actually works in a
 * server-side KeyManagerFactory.
 */
class BridgeHandshakeTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var scope: CoroutineScope
    private lateinit var identity: ServerIdentity
    private lateinit var devices: PairedDevices
    private lateinit var pending: PendingPairingStore
    private lateinit var server: BridgeServer

    @Before
    fun setUp() {
        // The thread-list assertion needs real provider access; the SMS group is one grant.
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        listOf(
            "android.permission.READ_SMS",
            "android.permission.RECEIVE_SMS",
            "android.permission.READ_CONTACTS",
        ).forEach { automation.grantRuntimePermission(context.packageName, it) }

        ServerIdentity.reset(context)
        identity = ServerIdentity.load(context)
        devices = PairedDevices(context)
        devices.devices.value.forEach { devices.remove(it.name) }
        pending = PendingPairingStore()
        val sms = SmsRepository(context, ContactNames(context))
        server = BridgeServer(identity, devices, pending, sms, "Test Phone")
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        server.start(scope)
    }

    @After
    fun tearDown() {
        server.stop()
        scope.cancel()
        ServerIdentity.reset(context)
    }

    @Test
    fun identityIsSelfSignedP256AndStable() {
        val certificate = identity.certificate
        assertEquals("EC", certificate.publicKey.algorithm)
        assertEquals(certificate.subjectX500Principal, certificate.issuerX500Principal)
        // 32 bytes of SHA-256 over the DER SubjectPublicKeyInfo — the bytes the Mac pins.
        assertEquals(32, identity.spkiHash.size)
        assertEquals(
            identity.spkiHash.toList(),
            MessageDigest.getInstance("SHA-256").digest(certificate.publicKey.encoded).toList(),
        )
        // Reloading must not rotate the key, or every paired Mac would break on restart.
        assertEquals(identity.spkiHash.toList(), ServerIdentity.load(context).spkiHash.toList())
    }

    @Test
    fun pairsThenReconnectsWithTheStoredKey() = runBlocking {
        val secret = Proofs.randomKey()
        pending.offer(secret.base64Url(), "Test Mac")

        val deviceKey = client().use { client ->
            val hello = client.read() as Hello
            assertEquals("Test Phone", hello.deviceName)
            assertEquals(32, hello.challenge.decodeBase64().size)

            // The proof covers the certificate this client actually saw, which is what makes a
            // man in the middle unable to complete pairing.
            client.write(
                PairFrame(
                    name = "Test Mac",
                    proof = Proofs.pair(secret, client.spkiHash).base64(),
                ),
            )

            val paired = client.read() as Paired
            assertTrue(
                "the phone must prove it knows the secret before the Mac commits the pin",
                Proofs.verify(
                    Proofs.confirm(secret, client.spkiHash, "Test Mac"),
                    paired.confirm.decodeBase64(),
                ),
            )
            assertEquals(Ready, client.read())

            client.write(Sync(limit = 5))
            assertNotNull("a paired Mac can read the thread list", client.read() as Threads)
            paired.deviceKey.decodeBase64()
        }

        assertEquals(listOf("Test Mac"), devices.devices.value.map { it.name })

        // Reconnect: no QR, no pending pairing, just the stored device key.
        client().use { client ->
            val hello = client.read() as Hello
            assertTrue(hello.paired)
            client.write(
                Auth(proof = Proofs.auth(deviceKey, hello.challenge.decodeBase64(), client.spkiHash).base64()),
            )
            assertEquals(Ready, client.read())
        }
    }

    @Test
    fun rejectsAPairingProofOverTheWrongCertificate() = runBlocking {
        val secret = Proofs.randomKey()
        pending.offer(secret.base64Url(), "Test Mac")

        client().use { client ->
            client.read() as Hello
            // What a man in the middle would send: a proof over its own certificate.
            val otherSpki = client.spkiHash.copyOf().also { it[0] = (it[0] + 1).toByte() }
            client.write(PairFrame(name = "Test Mac", proof = Proofs.pair(secret, otherSpki).base64()))

            val error = client.read() as ErrorFrame
            assertEquals(ErrorCode.BAD_PROOF, error.code)
        }
        assertTrue("a rejected pairing must not be stored", devices.devices.value.isEmpty())
    }

    @Test
    fun rejectsAnUnknownMac() = runBlocking {
        client().use { client ->
            val hello = client.read() as Hello
            client.write(
                Auth(proof = Proofs.auth(Proofs.randomKey(), hello.challenge.decodeBase64(), client.spkiHash).base64()),
            )
            assertEquals(ErrorCode.UNPAIRED, (client.read() as ErrorFrame).code)
        }
    }

    @Test
    fun rejectsPairingWithNoCodeOnScreen() = runBlocking {
        client().use { client ->
            client.read() as Hello
            client.write(PairFrame(name = "Test Mac", proof = Proofs.randomKey().base64()))
            assertEquals(ErrorCode.UNPAIRED, (client.read() as ErrorFrame).code)
        }
    }

    @Test
    fun rejectsAnExpiredPairingCode() = runBlocking {
        pending.offer(Proofs.randomKey().base64Url(), "Test Mac", ttlMillis = -1)
        client().use { client ->
            client.read() as Hello
            client.write(PairFrame(name = "Test Mac", proof = Proofs.randomKey().base64()))
            assertEquals(ErrorCode.UNPAIRED, (client.read() as ErrorFrame).code)
        }
    }

    private suspend fun client(): TestClient = withTimeout(10_000) {
        var port: Int? = null
        while (port == null) {
            port = server.port.value
            if (port == null) kotlinx.coroutines.delay(50)
        }
        TestClient(port)
    }

    /** The Mac's side: TLS with the certificate captured rather than validated against a CA. */
    private inner class TestClient(port: Int) : AutoCloseable {
        private var certificate: X509Certificate? = null
        private val socket: SSLSocket
        private val input: BufferedInputStream
        private val output: BufferedOutputStream

        val spkiHash: ByteArray
            get() = MessageDigest.getInstance("SHA-256").digest(certificate!!.publicKey.encoded)

        init {
            val trustManager = object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) = Unit
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
                    certificate = chain.first()
                }
                override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
            }
            val sslContext = SSLContext.getInstance("TLS").apply {
                init(null, arrayOf(trustManager), null)
            }
            socket = (sslContext.socketFactory.createSocket("127.0.0.1", port) as SSLSocket).apply {
                soTimeout = 10_000
                startHandshake()
            }
            input = BufferedInputStream(socket.inputStream)
            output = BufferedOutputStream(socket.outputStream)
        }

        fun read(): Frame = Framing.read(input)
        fun write(frame: Frame) = Framing.write(output, frame)
        override fun close() = socket.close()
    }
}

private fun ByteArray.base64(): String = Base64.encodeToString(this, Base64.NO_WRAP)
private fun ByteArray.base64Url(): String =
    Base64.encodeToString(this, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
private fun String.decodeBase64(): ByteArray = Base64.decode(this, Base64.NO_WRAP)
