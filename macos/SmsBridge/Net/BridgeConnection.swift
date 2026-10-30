import CryptoKit
import Foundation
import Network
import Security

/// One TLS connection to the phone, with the certificate pin enforced in the TLS handshake.
///
/// Network.framework rather than URLSessionWebSocketTask: the verify block below is Apple's
/// documented hook for local-network TLS without a trusted root, connecting straight to an
/// `NWEndpoint.service` skips any hostname/SNI mismatch to suppress, there is no App Transport
/// Security exception to add, and `viabilityUpdateHandler` reports Wi-Fi roaming.
final class BridgeConnection {
    enum Event {
        /// TLS is up. `spkiHash` is what the phone actually presented.
        case connected(spkiHash: Data)
        case frame(Frame)
        case failed(String)
        /// The certificate did not match the pin — a different phone, a reinstalled app, or an
        /// attacker. Never resolved by silently re-pinning.
        case pinMismatch
        case closed
    }

    /// Written by the TLS verify block, which runs on `queue` before the connection is ready.
    private final class PinState {
        var observedSpkiHash: Data?
        var failed = false
    }

    private let connection: NWConnection
    private let queue = DispatchQueue(label: "dev.haxzie.smsbridge.connection")
    private let pinState = PinState()
    private var buffer = FrameBuffer()
    private var finished = false

    var onEvent: ((Event) -> Void)?

    /// - Parameter pinnedSpkiHash: nil during pairing, when the certificate is accepted in
    ///   memory and only committed once the phone proves it knows the QR secret.
    init(endpoint: NWEndpoint, pinnedSpkiHash: Data?) {
        let tls = NWProtocolTLS.Options()
        sec_protocol_options_set_min_tls_protocol_version(tls.securityProtocolOptions, .TLSv12)

        // This has to be installed before NWParameters is built: NWParameters snapshots the
        // protocol options, so a verify block added afterwards is silently ignored and the
        // default verification rejects the self-signed certificate with errSSLBadCert.
        let state = pinState
        sec_protocol_options_set_verify_block(
            tls.securityProtocolOptions,
            { _, trust, complete in
                guard let hash = Self.spkiHash(of: trust) else {
                    state.failed = true
                    return complete(false)
                }
                state.observedSpkiHash = hash
                if let pinnedSpkiHash, pinnedSpkiHash != hash {
                    // Equality over the exact public key is a strictly stronger statement than
                    // chain validation would be for a self-signed peer, so there is no
                    // SecTrustEvaluateWithError here: with no CA it would only add a failure
                    // mode.
                    state.failed = true
                    return complete(false)
                }
                complete(true)
            },
            queue
        )

        let tcp = NWProtocolTCP.Options()
        tcp.connectionTimeout = 10
        tcp.enableKeepalive = true
        tcp.keepaliveIdle = 30

        let parameters = NWParameters(tls: tls, tcp: tcp)
        parameters.includePeerToPeer = false
        connection = NWConnection(to: endpoint, using: parameters)
    }

    func start() {
        connection.stateUpdateHandler = { [weak self] state in
            guard let self else { return }
            switch state {
            case .ready:
                self.emit(.connected(spkiHash: self.pinState.observedSpkiHash ?? Data()))
                self.receive()
            case .failed(let error):
                self.emit(self.pinState.failed ? .pinMismatch : .failed(error.localizedDescription))
            case .cancelled:
                self.emit(.closed)
            case .waiting(let error):
                // Routinely transient (phone asleep, Wi-Fi settling). Reported so the UI can
                // explain itself rather than claiming to be connected.
                self.onEvent?(.failed(error.localizedDescription))
            default:
                break
            }
        }
        connection.viabilityUpdateHandler = { [weak self] viable in
            if !viable { self?.emit(.failed("the network path to the phone went away")) }
        }
        connection.start(queue: queue)
    }

    func send(_ frame: Frame) {
        guard let data = try? FrameBuffer.encode(frame) else { return }
        connection.send(content: data, completion: .contentProcessed { _ in })
    }

    func cancel() {
        finished = true
        connection.cancel()
    }

    private func receive() {
        connection.receive(minimumIncompleteLength: 1, maximumLength: 64 * 1024) {
            [weak self] data, _, isComplete, error in
            guard let self else { return }
            if let data, !data.isEmpty {
                self.buffer.append(data)
                do {
                    for frame in try self.buffer.drain() {
                        self.onEvent?(.frame(frame))
                    }
                } catch {
                    self.emit(.failed(error.localizedDescription))
                    self.connection.cancel()
                    return
                }
            }
            if isComplete {
                self.emit(.closed)
                self.connection.cancel()
                return
            }
            if let error {
                self.emit(.failed(error.localizedDescription))
                return
            }
            self.receive()
        }
    }

    /// Only one terminal event per connection, so a failure during teardown cannot restart the
    /// client twice.
    private func emit(_ event: Event) {
        switch event {
        case .connected, .frame:
            onEvent?(event)
        default:
            guard !finished else { return }
            finished = true
            onEvent?(event)
        }
    }

    /// SHA-256 over the leaf certificate's DER SubjectPublicKeyInfo — the same bytes Java's
    /// `PublicKey.getEncoded()` returns on the phone, which is what makes the pin comparable.
    private static func spkiHash(of trust: sec_trust_t) -> Data? {
        let secTrust = sec_trust_copy_ref(trust).takeRetainedValue()
        guard let chain = SecTrustCopyCertificateChain(secTrust) as? [SecCertificate],
              let leaf = chain.first,
              let publicKey = SecCertificateCopyKey(leaf),
              let representation = SecKeyCopyExternalRepresentation(publicKey, nil) as Data?,
              let parsed = try? P256.Signing.PublicKey(x963Representation: representation)
        else {
            return nil
        }
        return Data(SHA256.hash(data: parsed.derRepresentation))
    }
}
