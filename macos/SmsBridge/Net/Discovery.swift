import Foundation
import Network

struct DiscoveredPhone: Equatable {
    let endpoint: NWEndpoint
    let deviceId: String?
    let name: String?
}

/// Bonjour browser for `_smsbridge._tcp`.
///
/// On macOS 15+ browsing requires local network permission *and* the NSBonjourServices /
/// NSLocalNetworkUsageDescription Info.plist keys; without them the browser sits in `.waiting`
/// with a policy error and no prompt is ever shown, which is why that case is surfaced as a
/// real error rather than ignored.
final class Discovery {
    private var browser: NWBrowser?
    private let queue = DispatchQueue(label: "dev.haxzie.smsbridge.discovery")

    var onResults: (([DiscoveredPhone]) -> Void)?
    var onPermissionProblem: ((String) -> Void)?

    func start() {
        guard browser == nil else { return }
        let parameters = NWParameters()
        parameters.includePeerToPeer = false
        let browser = NWBrowser(
            for: .bonjour(type: Protocol.serviceType, domain: nil),
            using: parameters
        )
        browser.browseResultsChangedHandler = { [weak self] results, _ in
            let phones = results.map { result -> DiscoveredPhone in
                var deviceId: String?
                var name: String?
                if case .bonjour(let txt) = result.metadata {
                    deviceId = txt["did"]
                    name = txt["name"]
                }
                return DiscoveredPhone(endpoint: result.endpoint, deviceId: deviceId, name: name)
            }
            self?.onResults?(phones)
        }
        browser.stateUpdateHandler = { [weak self] state in
            if case .waiting(let error) = state {
                self?.onPermissionProblem?(
                    "Cannot browse the local network (\(error)). Check System Settings › Privacy & Security › Local Network."
                )
            }
        }
        self.browser = browser
        browser.start(queue: queue)
    }

    func stop() {
        browser?.cancel()
        browser = nil
    }
}
