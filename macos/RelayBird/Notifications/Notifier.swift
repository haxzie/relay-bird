import AppKit
import Foundation
import UserNotifications

/// Banner per incoming SMS. Clicking one opens that thread in the popover.
@MainActor
final class Notifier: NSObject, ObservableObject, UNUserNotificationCenterDelegate {
    nonisolated static let threadIdKey = "threadId"

    /// Published so the settings screen can say when notifications are blocked, instead of
    /// the app simply never notifying and never explaining why.
    @Published private(set) var authorizationStatus: UNAuthorizationStatus = .notDetermined

    var onOpenThread: ((Int64) -> Void)?

    func start() {
        UNUserNotificationCenter.current().delegate = self
        refreshStatus()
    }

    /// Asked for while the app is frontmost, not at launch.
    ///
    /// This app has no Dock icon and no window, so at launch it is never the active
    /// application — and macOS answers a permission request from an app in that state by
    /// denying it outright rather than prompting. The result was a permanent denial recorded
    /// before the user ever saw a prompt, which is why notifications silently never arrived.
    func requestAuthorizationIfNeeded() {
        let center = UNUserNotificationCenter.current()
        center.getNotificationSettings { [weak self] settings in
            Task { @MainActor in self?.authorizationStatus = settings.authorizationStatus }
            guard settings.authorizationStatus == .notDetermined else { return }
            center.requestAuthorization(options: [.alert, .sound]) { granted, error in
                if let error {
                    NSLog("RelayBird: notification authorisation failed: %@", error.localizedDescription)
                }
                NSLog("RelayBird: notification authorisation granted=%@", granted ? "yes" : "no")
                Task { @MainActor in self?.refreshStatus() }
            }
        }
    }

    func refreshStatus() {
        UNUserNotificationCenter.current().getNotificationSettings { [weak self] settings in
            Task { @MainActor in self?.authorizationStatus = settings.authorizationStatus }
        }
    }

    /// Opens the Notifications pane, since a denial can only be undone there.
    func openSystemSettings() {
        let url = URL(string: "x-apple.systempreferences:com.apple.preference.notifications")!
        NSWorkspace.shared.open(url)
    }

    func post(message: WireMessage, thread: WireThread?) {
        guard AppSettings.notificationsEnabled else { return }
        let content = UNMutableNotificationContent()
        content.title = thread?.title ?? message.address
        content.body = message.body
        content.sound = .default
        content.userInfo = [Self.threadIdKey: message.threadId]
        // Grouped per conversation, so a chatty thread collapses instead of stacking.
        content.threadIdentifier = "thread-\(message.threadId)"

        UNUserNotificationCenter.current().add(
            UNNotificationRequest(identifier: "sms-\(message.id)", content: content, trigger: nil)
        )
    }

    nonisolated func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        let threadId = (response.notification.request.content.userInfo[Self.threadIdKey] as? NSNumber)?.int64Value
        Task { @MainActor in
            if let threadId { self.onOpenThread?(threadId) }
            completionHandler()
        }
    }

    nonisolated func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        // The app has no windows, so "foreground" is always the right time to show a banner.
        completionHandler([.banner, .sound])
    }
}
