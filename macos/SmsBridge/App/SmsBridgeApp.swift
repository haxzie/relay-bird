import SwiftUI

@main
struct SmsBridgeApp: App {
    @NSApplicationDelegateAdaptor(AppDelegate.self) private var delegate

    var body: some Scene {
        // The entire UI lives in the menu bar popover the delegate owns; this scene exists
        // only because SwiftUI's App protocol requires one.
        Settings {
            EmptyView()
        }
    }
}
