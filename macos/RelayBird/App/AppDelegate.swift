import AppKit
import Combine
import SwiftUI

/// Owns the status item and its popover.
///
/// NSStatusItem + NSPopover rather than SwiftUI's MenuBarExtra: the unread badge needs a custom
/// status-item title, and the popover has to be openable programmatically when a notification
/// is clicked.
@MainActor
final class AppDelegate: NSObject, NSApplicationDelegate {
    private let client = BridgeClient()
    private let notifier = Notifier()
    private var statusItem: NSStatusItem!
    private var popover: NSPopover!
    private var pendingThreadId: Int64?
    private var cancellables: Set<AnyCancellable> = []

    func applicationDidFinishLaunching(_ notification: Notification) {
        statusItem = NSStatusBar.system.statusItem(withLength: NSStatusItem.variableLength)
        statusItem.button?.image = NSImage(
            systemSymbolName: "message.badge.filled.fill",
            accessibilityDescription: "RelayBird"
        )
        statusItem.button?.imagePosition = .imageLeading
        statusItem.button?.target = self
        statusItem.button?.action = #selector(togglePopover)

        popover = NSPopover()
        popover.behavior = .transient
        popover.contentSize = NSSize(width: 360, height: 460)
        popover.contentViewController = NSHostingController(
            rootView: RootView(
                client: client,
                pendingThreadId: Binding(
                    get: { [weak self] in self?.pendingThreadId },
                    set: { [weak self] in self?.pendingThreadId = $0 }
                )
            )
        )

        notifier.onOpenThread = { [weak self] threadId in
            self?.pendingThreadId = threadId
            self?.showPopover()
        }
        notifier.requestAuthorization()

        client.onIncomingMessage = { [weak self] message, thread in
            self?.notifier.post(message: message, thread: thread)
        }
        client.$threads
            .map { threads in threads.reduce(0) { $0 + $1.unread } }
            .removeDuplicates()
            .sink { [weak self] unread in
                self?.statusItem.button?.title = unread > 0 ? " \(unread)" : ""
            }
            .store(in: &cancellables)

        client.start()
    }

    func applicationWillTerminate(_ notification: Notification) {
        client.stop()
    }

    @objc private func togglePopover() {
        if popover.isShown {
            popover.performClose(nil)
        } else {
            showPopover()
        }
    }

    private func showPopover() {
        guard let button = statusItem.button else { return }
        popover.show(relativeTo: button.bounds, of: button, preferredEdge: .minY)
        popover.contentViewController?.view.window?.makeKey()
    }
}
