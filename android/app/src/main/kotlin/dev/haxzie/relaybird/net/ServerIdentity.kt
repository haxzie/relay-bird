package dev.haxzie.relaybird.net

import android.content.Context
import android.util.Base64
import android.util.Log
import dev.haxzie.relaybird.store.SecureStore
import org.bouncycastle.asn1.x509.BasicConstraints
import org.bouncycastle.asn1.x509.ExtendedKeyUsage
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.asn1.x509.GeneralName
import org.bouncycastle.asn1.x509.GeneralNames
import org.bouncycastle.asn1.x509.KeyPurposeId
import org.bouncycastle.asn1.x509.KeyUsage
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.security.spec.ECGenParameterSpec
import java.util.Date
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.security.auth.x500.X500Principal

/**
 * The phone's TLS identity: a self-signed P-256 certificate, held in a PKCS12 keystore that is
 * itself encrypted at rest by a non-exportable AndroidKeyStore key.
 *
 * AndroidKeyStore was the first choice — it generates a self-signed certificate for free and
 * keeps the private key in the TEE. It does not work for a TLS *server*: the key manager picks
 * the alias and hands over the chain correctly, but the handshake then dies in Conscrypt with
 * a bare connection close (verified on API 35, see BridgeHandshakeTest). Opaque keystore keys
 * are a well-trodden path for client certificates and not for server ones.
 *
 * So the TLS key is a software key, which Conscrypt can use, and the blob it lives in is sealed
 * with AndroidKeyStore AES-GCM so it is still not readable off a lost device.
 */
class ServerIdentity private constructor(
    val sslContext: SSLContext,
    val certificate: X509Certificate,
) {
    /** SHA-256 over the DER SubjectPublicKeyInfo. This is what the Mac pins. */
    val spkiHash: ByteArray = MessageDigest.getInstance("SHA-256")
        .digest(certificate.publicKey.encoded)

    companion object {
        private const val TAG = "ServerIdentity"
        private const val STORE_NAME = "tls_identity"
        private const val KEY_PKCS12 = "pkcs12"
        private const val KEY_PASSWORD = "password"
        private const val ALIAS = "relaybird-tls"
        private const val VALIDITY_MILLIS = 10L * 365 * 24 * 60 * 60 * 1000

        // Android pre-registers a crippled provider under the name "BC", so every BouncyCastle
        // call below has to be handed this instance rather than the string "BC".
        private val bouncyCastle = BouncyCastleProvider()

        fun load(context: Context): ServerIdentity {
            val store = SecureStore(context, STORE_NAME)
            val password = store.getString(KEY_PASSWORD) ?: newPassword().also {
                store.putString(KEY_PASSWORD, it)
            }
            val keyStore = store.getString(KEY_PKCS12)?.let { restore(it, password) }
                ?: generate().also { store.putString(KEY_PKCS12, export(it, password)) }

            val certificate = keyStore.getCertificate(ALIAS) as X509Certificate
            val kmf = KeyManagerFactory.getInstance("X509").apply {
                init(keyStore, password.toCharArray())
            }
            val sslContext = SSLContext.getInstance("TLS").apply {
                init(kmf.keyManagers, null, null)
            }
            Log.i(TAG, "TLS identity ready")
            return ServerIdentity(sslContext, certificate)
        }

        /** Wipes the identity, which forces every paired Mac to re-pair. */
        fun reset(context: Context) {
            SecureStore(context, STORE_NAME).clear()
        }

        private fun newPassword(): String =
            Base64.encodeToString(ByteArray(32).also { SecureRandom().nextBytes(it) }, Base64.NO_WRAP)

        /** JKS does not exist on Android; PKCS12 does. */
        private fun emptyKeyStore(): KeyStore = KeyStore.getInstance("PKCS12")

        private fun generate(): KeyStore {
            val keyPair = KeyPairGenerator.getInstance("EC").run {
                initialize(ECGenParameterSpec("secp256r1"))
                generateKeyPair()
            }
            val certificate = selfSign(keyPair)
            return emptyKeyStore().apply {
                load(null)
                setKeyEntry(ALIAS, keyPair.private, null, arrayOf(certificate))
            }
        }

        private fun selfSign(keyPair: KeyPair): X509Certificate {
            val now = System.currentTimeMillis()
            val subject = X500Principal("CN=RelayBird")
            val builder = JcaX509v3CertificateBuilder(
                subject,
                BigInteger.valueOf(now),
                Date(now - 24 * 60 * 60 * 1000L),
                // Ten years. The certificate is pinned by hash so expiry buys no security, but
                // absurd lifetimes trip platform sanity checks, and this is long enough that a
                // v1 user will never meet the renewal cliff.
                Date(now + VALIDITY_MILLIS),
                subject,
                keyPair.public,
            )

            // Apple's TLS stack rejects an extension-less certificate outright — with
            // errSSLBadCert, *before* it ever calls the client's custom verify block, so the
            // Mac cannot wave it through. Verified against the real Swift client; without
            // these five extensions the handshake fails with "bad certificate format".
            builder.addExtension(Extension.basicConstraints, true, BasicConstraints(false))
            // digitalSignature only: keyEncipherment is meaningless for an EC key (it cannot
            // do key transport) and Apple's TLS stack rejects the certificate for claiming it.
            builder.addExtension(Extension.keyUsage, true, KeyUsage(KeyUsage.digitalSignature))
            builder.addExtension(
                Extension.extendedKeyUsage,
                false,
                ExtendedKeyUsage(KeyPurposeId.id_kp_serverAuth),
            )
            // The Mac pins the public key and does not check names, but a SAN has to be present.
            builder.addExtension(
                Extension.subjectAlternativeName,
                false,
                GeneralNames(
                    arrayOf(
                        GeneralName(GeneralName.dNSName, "relaybird.local"),
                        GeneralName(GeneralName.dNSName, "localhost"),
                        GeneralName(GeneralName.iPAddress, "127.0.0.1"),
                    ),
                ),
            )
            builder.addExtension(
                Extension.subjectKeyIdentifier,
                false,
                JcaX509ExtensionUtils().createSubjectKeyIdentifier(keyPair.public),
            )

            val signer = JcaContentSignerBuilder("SHA256withECDSA")
                .setProvider(bouncyCastle)
                .build(keyPair.private)
            return JcaX509CertificateConverter()
                .setProvider(bouncyCastle)
                .getCertificate(builder.build(signer))
        }

        private fun export(keyStore: KeyStore, password: String): String {
            val bytes = ByteArrayOutputStream().also { keyStore.store(it, password.toCharArray()) }
            return Base64.encodeToString(bytes.toByteArray(), Base64.NO_WRAP)
        }

        private fun restore(encoded: String, password: String): KeyStore? = runCatching {
            emptyKeyStore().apply {
                load(ByteArrayInputStream(Base64.decode(encoded, Base64.NO_WRAP)), password.toCharArray())
            }
        }.onFailure { Log.w(TAG, "stored identity unreadable, regenerating: ${it.message}") }.getOrNull()
    }
}
