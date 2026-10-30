import Combine
import Foundation
import Network

/// Owns discovery, connection, pairing and the mirrored message state. The UI reads this and
/// nothing else.
@MainActor
final class BridgeClient: ObservableObject {
    enum State: Equatable {
        case unpaired
        case pairing
        case searching
        case connecting
        case connected
        case offline(String)
        /// The phone presented a different certificate than the one pinned at pairing.
        case identityChanged
    }

    @Published private(set) var state: State = .unpaired
    @Published private(set) var threads: [WireThread] = []
    @Published private(set) var messagesByThread: [Int64: [WireMessage]] = [:]
    @Published private(set) var pairingCode: PairingCode?
    @Published private(set) var phone: PairedPhone?
    @Published private(set) var phoneName: String?
    @Published private(set) var lastSyncedAt: Date?

    var onIncomingMessage: ((WireMessage, WireThread?) -> Void)?

    private let discovery = Discovery()
    private var discovered: [DiscoveredPhone] = []
    private var attempts: [BridgeConnection] = []
    private var spkiByAttempt: [ObjectIdentifier: Data] = [:]
    private var active: BridgeConnection?
    private var reconnectDelay: TimeInterval = 1
    private var reconnectTask: Task<Void, Never>?
    private var watchdog: Task<Void, Never>?
    private var pairingTimer: Task<Void, Never>?
    /// Suppresses notification banners for the initial backlog of a fresh sync.
    private var initialSyncDone = false

    init() {
        let cached = MessageCache.load()
        threads = cached.threads
        messagesByThread = cached.messagesByThread
        lastSyncedAt = cached.syncedAt
        phoneName = cached.phoneName
        phone = Keychain.load()
    }

    // MARK: - Lifecycle

    func start() {
        discovery.onResults = { [weak self] phones in
            Task { @MainActor in self?.handleDiscovery(phones) }
        }
        discovery.onPermissionProblem = { [weak self] message in
            Task { @MainActor in
                guard let self, self.state != .connected else { return }
                self.state = .offline(message)
            }
        }
        discovery.start()

        if phone == nil {
            state = .unpaired
        } else {
            state = .searching
            connectIfPossible()
        }
    }

    func stop() {
        discovery.stop()
        teardownAttempts()
        reconnectTask?.cancel()
        watchdog?.cancel()
    }

    // MARK: - Pairing

    func beginPairing() {
        let code = PairingCode.generate()
        pairingCode = code
        state = .pairing
        teardownAttempts()
        connectIfPossible()

        pairingTimer?.cancel()
        pairingTimer = Task { [weak self] in
            try? await Task.sleep(nanoseconds: UInt64(PairingCode.lifetime * 1_000_000_000))
            guard !Task.isCancelled else { return }
            await MainActor.run {
                guard let self, self.state == .pairing else { return }
                self.cancelPairing()
            }
        }
    }

    func cancelPairing() {
        pairingTimer?.cancel()
        pairingCode = nil
        teardownAttempts()
        state = phone == nil ? .unpaired : .searching
        connectIfPossible()
    }

    func unpair() {
        Keychain.clear()
        MessageCache.clear()
        phone = nil
        threads = []
        messagesByThread = [:]
        lastSyncedAt = nil
        teardownAttempts()
        state = .unpaired
    }

    // MARK: - Requests

    func openThread(_ threadId: Int64) {
        active?.send(.thread(threadId: threadId, limit: 100, beforeId: nil))
    }

    func markRead(_ threadId: Int64) {
        active?.send(.markRead(threadId: threadId))
        guard let index = threads.firstIndex(where: { $0.threadId == threadId }), threads[index].unread > 0 else {
            return
        }
        let thread = threads[index]
        threads[index] = WireThread(
            threadId: thread.threadId,
            address: thread.address,
            displayName: thread.displayName,
            snippet: thread.snippet,
            date: thread.date,
            unread: 0
        )
        persist()
    }

    func reconnectNow() {
        reconnectDelay = 1
        teardownAttempts()
        connectIfPossible()
    }

    var unreadCount: Int { threads.reduce(0) { $0 + $1.unread } }

    // MARK: - Connecting

    private func handleDiscovery(_ phones: [DiscoveredPhone]) {
        discovered = phones
        if active == nil { connectIfPossible() }
    }

    /// Candidate endpoints, best first. The cached endpoint is raced against Bonjour because
    /// mDNS is genuinely unreliable — it lives in an updatable system module, so behaviour
    /// varies by module version and not just by OS version.
    private func candidateEndpoints() -> [NWEndpoint] {
        if let manual = AppSettings.manualEndpoint, let endpoint = Self.parseEndpoint(manual) {
            return [endpoint]
        }

        var endpoints: [NWEndpoint] = []
        if let cached = AppSettings.cachedEndpoint {
            endpoints.append(
                .hostPort(host: NWEndpoint.Host(cached.host), port: NWEndpoint.Port(rawValue: cached.port)!)
            )
        }
        // Prefer the phone we are paired with, but during pairing anything advertising counts.
        let matching = discovered.filter { phone == nil || $0.deviceId == nil || $0.deviceId == phone?.deviceId }
        endpoints.append(contentsOf: matching.map(\.endpoint))
        return Array(endpoints.prefix(3))
    }

    static func parseEndpoint(_ text: String) -> NWEndpoint? {
        let parts = text.split(separator: ":")
        guard parts.count == 2, let port = UInt16(parts[1]), let nwPort = NWEndpoint.Port(rawValue: port) else {
            return nil
        }
        return .hostPort(host: NWEndpoint.Host(String(parts[0])), port: nwPort)
    }

    private func connectIfPossible() {
        guard active == nil, attempts.isEmpty else { return }
        guard phone != nil || pairingCode != nil else { return }

        let endpoints = candidateEndpoints()
        guard !endpoints.isEmpty else {
            if state == .connecting { state = .searching }
            return
        }
        if state != .pairing { state = .connecting }

        for endpoint in endpoints {
            // nil pin during pairing: the certificate is accepted in memory and only committed
            // once the phone has proved it knows the QR secret.
            let connection = BridgeConnection(endpoint: endpoint, pinnedSpkiHash: phone?.spkiHash)
            connection.onEvent = { [weak self, weak connection] event in
                guard let self, let connection else { return }
                Task { @MainActor in self.handle(event, from: connection, endpoint: endpoint) }
            }
            attempts.append(connection)
            connection.start()
        }
    }

    private func handle(_ event: BridgeConnection.Event, from connection: BridgeConnection, endpoint: NWEndpoint) {
        // Once one attempt has won, the others are noise.
        if let active, active !== connection {
            connection.cancel()
            return
        }

        switch event {
        case .connected(let spkiHash):
            spkiByAttempt[ObjectIdentifier(connection)] = spkiHash
            cacheEndpoint(endpoint)

        case .frame(let frame):
            handle(frame, from: connection)

        case .pinMismatch:
            drop(connection)
            state = .identityChanged

        case .failed(let message):
            drop(connection)
            if attempts.isEmpty && active == nil {
                state = .offline(message)
                scheduleReconnect()
            }

        case .closed:
            drop(connection)
            if attempts.isEmpty && active == nil {
                if state != .identityChanged && state != .unpaired {
                    state = .offline("Disconnected")
                }
                scheduleReconnect()
            }
        }
    }

    private func handle(_ frame: Frame, from connection: BridgeConnection) {
        switch frame {
        case .hello(let version, let deviceId, let deviceName, _, let challenge):
            guard version == Protocol.version else {
                state = .offline("The phone app speaks protocol v\(version); update both apps.")
                connection.cancel()
                return
            }
            phoneName = deviceName
            guard let spkiHash = spkiByAttempt[ObjectIdentifier(connection)] else {
                connection.cancel()
                return
            }
            if let phone {
                connection.send(
                    .auth(proof: Proofs.auth(deviceKey: phone.deviceKey, challenge: challenge, spkiHash: spkiHash))
                )
            } else if let code = pairingCode, !code.expired {
                connection.send(
                    .pair(name: code.macName, proof: Proofs.pair(secret: code.secret, spkiHash: spkiHash))
                )
                pendingPairDeviceId = deviceId
            } else {
                connection.cancel()
            }

        case .paired(let confirm, let deviceKey):
            guard let code = pairingCode,
                  let spkiHash = spkiByAttempt[ObjectIdentifier(connection)],
                  Proofs.verifyConfirm(
                      secret: code.secret,
                      spkiHash: spkiHash,
                      macName: code.macName,
                      candidate: confirm
                  )
            else {
                // Without a valid confirmation the pin is never committed: otherwise an
                // attacker who cannot finish the handshake could still get this Mac to pin a
                // bogus certificate permanently.
                state = .offline("Pairing failed: the phone's reply could not be verified.")
                drop(connection)
                return
            }
            let paired = PairedPhone(
                deviceId: pendingPairDeviceId ?? "",
                deviceName: phoneName ?? "Android phone",
                spkiHash: spkiHash,
                deviceKey: deviceKey,
                macName: code.macName,
                pairedAt: Date()
            )
            Keychain.save(paired)
            phone = paired
            pairingTimer?.cancel()
            pairingCode = nil
            promote(connection)

        case .ready:
            promote(connection)

        case .threads(let incoming):
            threads = incoming
            lastSyncedAt = Date()
            initialSyncDone = true
            persist()

        case .messages(let threadId, let messages):
            messagesByThread[threadId] = messages
            persist()

        case .message(let message):
            apply(message)

        case .ping:
            connection.send(.pong)

        case .pong:
            break

        case .error(let code, let message):
            if code == ErrorCode.unpaired || code == ErrorCode.revoked {
                // The phone no longer knows this Mac, so the stored key is worthless.
                unpair()
                state = .offline("This Mac was removed from the phone. Pair again to reconnect.")
            } else {
                state = .offline(message)
            }
            drop(connection)

        default:
            break
        }
        resetWatchdog()
    }

    private var pendingPairDeviceId: String?

    private func promote(_ connection: BridgeConnection) {
        guard active !== connection else { return }
        active = connection
        attempts.removeAll { $0 !== connection }
        reconnectDelay = 1
        state = .connected
        initialSyncDone = false
        connection.send(.sync(limit: 50))
        resetWatchdog()
    }

    private func drop(_ connection: BridgeConnection) {
        connection.cancel()
        attempts.removeAll { $0 === connection }
        spkiByAttempt.removeValue(forKey: ObjectIdentifier(connection))
        if active === connection {
            active = nil
            watchdog?.cancel()
        }
    }

    private func teardownAttempts() {
        attempts.forEach { $0.cancel() }
        active?.cancel()
        attempts = []
        active = nil
        spkiByAttempt = [:]
        watchdog?.cancel()
    }

    private func apply(_ message: WireMessage) {
        var thread = threads.first { $0.threadId == message.threadId }
        let updated = WireThread(
            threadId: message.threadId,
            address: thread?.address ?? message.address,
            displayName: thread?.displayName,
            snippet: String(message.body.prefix(140)),
            date: message.date,
            unread: (thread?.unread ?? 0) + (message.outgoing || message.read ? 0 : 1)
        )
        threads.removeAll { $0.threadId == message.threadId }
        threads.insert(updated, at: 0)
        thread = updated

        var existing = messagesByThread[message.threadId] ?? []
        if !existing.contains(where: { $0.id == message.id }) {
            existing.append(message)
            messagesByThread[message.threadId] = existing
        }
        lastSyncedAt = Date()
        persist()

        if !message.outgoing && initialSyncDone {
            onIncomingMessage?(message, updated)
        }
    }

    private func cacheEndpoint(_ endpoint: NWEndpoint) {
        guard case .hostPort(let host, let port) = endpoint else { return }
        let text: String
        switch host {
        case .ipv4(let address): text = "\(address)"
        case .ipv6(let address): text = "\(address)"
        case .name(let name, _): text = name
        @unknown default: return
        }
        AppSettings.cachedEndpoint = (text, port.rawValue)
    }

    private func persist() {
        MessageCache.save(
            CachedState(
                threads: threads,
                messagesByThread: messagesByThread,
                syncedAt: lastSyncedAt,
                phoneName: phoneName
            )
        )
    }

    private func scheduleReconnect() {
        guard phone != nil || pairingCode != nil else { return }
        reconnectTask?.cancel()
        let delay = reconnectDelay
        reconnectDelay = min(reconnectDelay * 2, 30)
        reconnectTask = Task { [weak self] in
            try? await Task.sleep(nanoseconds: UInt64(delay * 1_000_000_000))
            guard !Task.isCancelled else { return }
            await MainActor.run { self?.connectIfPossible() }
        }
    }

    /// The phone pings every 20s; 60s of total silence means the connection is gone even if
    /// TCP has not noticed yet (common after the Mac sleeps).
    private func resetWatchdog() {
        watchdog?.cancel()
        watchdog = Task { [weak self] in
            try? await Task.sleep(nanoseconds: 60_000_000_000)
            guard !Task.isCancelled else { return }
            await MainActor.run {
                guard let self, let active = self.active else { return }
                self.drop(active)
                self.state = .offline("The phone stopped responding")
                self.scheduleReconnect()
            }
        }
    }
}
