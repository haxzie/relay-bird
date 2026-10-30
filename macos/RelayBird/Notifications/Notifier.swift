import Foundation
import UserNotifications

/// Banner per incoming SMS. Clicking one opens that thread in the popover.
@MainActor
final class Notifier: NSObject, UNUserNotificationCenterDelegate {
    nonisolated static let threadIdKey = "threadId"

    var onOpenThread: ((Int64) -> Void)?

    func requestAuthorization() {
        UNUserNotificationCenter.current().delegate = self
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound]) { _, _ in }
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
