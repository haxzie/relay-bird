import Foundation

/// On-disk copy of the last sync, so the popover shows real content the instant it opens —
/// including while the phone is asleep or off the network.
struct CachedState: Codable {
    var threads: [WireThread] = []
    var messagesByThread: [Int64: [WireMessage]] = [:]
    var syncedAt: Date?
    var phoneName: String?
}

enum MessageCache {
    private static let fileName = "cache.json"
    private static let maxThreads = 100
    private static let maxMessagesPerThread = 200

    private static var url: URL? {
        guard let base = FileManager.default.urls(
            for: .applicationSupportDirectory, in: .userDomainMask
        ).first else { return nil }
        let directory = base.appendingPathComponent("RelayBird", isDirectory: true)
        try? FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        return directory.appendingPathComponent(fileName)
    }

    static func load() -> CachedState {
        guard let url, let data = try? Data(contentsOf: url),
              let state = try? JSONDecoder().decode(CachedState.self, from: data)
        else {
            return CachedState()
        }
        return state
    }

    static func save(_ state: CachedState) {
        guard let url else { return }
        var trimmed = state
        trimmed.threads = Array(state.threads.prefix(maxThreads))
        let liveThreadIds = Set(trimmed.threads.map(\.threadId))
        trimmed.messagesByThread = state.messagesByThread
            .filter { liveThreadIds.contains($0.key) }
            .mapValues { Array($0.suffix(maxMessagesPerThread)) }
        guard let data = try? JSONEncoder().encode(trimmed) else { return }
        try? data.write(to: url, options: .atomic)
    }

    static func clear() {
        guard let url else { return }
        try? FileManager.default.removeItem(at: url)
    }
}
