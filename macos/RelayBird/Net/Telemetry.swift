import Foundation
import os

/// Anonymous product analytics, posted to our own endpoint rather than to a vendor.
///
/// This is the one thing in RelayBird that leaves your network, so it is worth being precise
/// about what it is and is not:
///
/// - It never carries message content, phone numbers, contact names, or thread text. The
///   server's allow-list rejects anything outside a fixed vocabulary, and this type has no
///   code path that could reach a message in the first place.
/// - The identifier is a random UUID generated on this Mac. It is not derived from the
///   hardware, the user, or the paired phone, and clearing it is one click in Settings.
/// - It can be turned off, and when it is, nothing is sent and nothing is queued.
/// - Failures are silent and never retried. An analytics call must not be able to affect
///   whether the app works.
@MainActor
final class Telemetry {
    static let shared = Telemetry()

    private let endpoint = URL(string: "https://relaybird.haxzie.com/api/events")!
    private let log = Logger(subsystem: "com.haxzie.relaybird", category: "telemetry")
    private let session: URLSession

    private init() {
        let configuration = URLSessionConfiguration.ephemeral
        configuration.timeoutIntervalForRequest = 8
        // No cookies and no credential storage: there is no session here to persist, and a
        // cookie jar would be a way to be recognised across installs.
        configuration.httpCookieAcceptPolicy = .never
        configuration.httpShouldSetCookies = false
        session = URLSession(configuration: configuration)
    }

    enum Event: String {
        case appLaunched = "app_launched"
        case phonePaired = "phone_paired"
        case phoneConnected = "phone_connected"
        case phoneForgotten = "phone_forgotten"
        case messagesOpened = "messages_opened"
        case threadOpened = "thread_opened"
        case notificationClicked = "notification_clicked"
        case updateInstalled = "update_installed"
    }

    func send(_ event: Event, properties: [String: Any] = [:]) {
        guard AppSettings.analyticsEnabled else { return }

        var payload: [String: Any] = [
            "event": event.rawValue,
            "distinctId": AppSettings.installId,
        ]

        var merged = properties
        merged["app_version"] = Bundle.main.appVersion
        merged["os_version"] = ProcessInfo.processInfo.operatingSystemVersionString
        merged["platform"] = "mac"
        payload["properties"] = merged

        guard let body = try? JSONSerialization.data(withJSONObject: payload) else { return }

        var request = URLRequest(url: endpoint)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = body

        session.dataTask(with: request) { [log] _, response, error in
            if let error {
                log.debug("telemetry \(event.rawValue, privacy: .public) failed: \(error.localizedDescription, privacy: .public)")
            } else if let http = response as? HTTPURLResponse, http.statusCode >= 400 {
                log.debug("telemetry \(event.rawValue, privacy: .public) rejected: \(http.statusCode)")
            }
        }.resume()
    }
}

extension Bundle {
    var appVersion: String {
        object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "unknown"
    }
}
