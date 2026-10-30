import Foundation

/// Small, non-secret preferences. Named AppSettings because SwiftUI already owns `Settings`. The cached endpoint is what makes reconnection feel instant
/// — and keeps working when mDNS does not.
enum AppSettings {
    private enum Key {
        static let cachedHost = "cachedHost"
        static let cachedPort = "cachedPort"
        static let manualEndpoint = "manualEndpoint"
        static let notificationsEnabled = "notificationsEnabled"
    }

    private static let defaults = UserDefaults.standard

    static var cachedEndpoint: (host: String, port: UInt16)? {
        get {
            guard let host = defaults.string(forKey: Key.cachedHost) else { return nil }
            let port = UInt16(defaults.integer(forKey: Key.cachedPort))
            guard port > 0 else { return nil }
            return (host, port)
        }
        set {
            defaults.set(newValue?.host, forKey: Key.cachedHost)
            defaults.set(Int(newValue?.port ?? 0), forKey: Key.cachedPort)
        }
    }

    /// `host:port`, used when the network blocks mDNS and when talking to an emulator.
    static var manualEndpoint: String? {
        get { defaults.string(forKey: Key.manualEndpoint)?.nilIfBlank }
        set { defaults.set(newValue?.nilIfBlank, forKey: Key.manualEndpoint) }
    }

    static var notificationsEnabled: Bool {
        get { defaults.object(forKey: Key.notificationsEnabled) as? Bool ?? true }
        set { defaults.set(newValue, forKey: Key.notificationsEnabled) }
    }

    static var macName: String {
        Host.current().localizedName ?? ProcessInfo.processInfo.hostName
    }
}

private extension String {
    var nilIfBlank: String? {
        let trimmed = trimmingCharacters(in: .whitespaces)
        return trimmed.isEmpty ? nil : trimmed
    }
}
