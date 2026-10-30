import SwiftUI

/// Popover content: the thread list, a thread, pairing, or settings.
struct RootView: View {
    @ObservedObject var client: BridgeClient
    @State private var route: Route = .threads
    @Binding var pendingThreadId: Int64?

    enum Route: Equatable {
        case threads
        case thread(Int64)
        case settings
    }

    var body: some View {
        VStack(spacing: 0) {
            content
            Divider()
            StatusBar(client: client, route: $route)
        }
        .frame(width: 360, height: 460)
        .onChange(of: pendingThreadId) { _, threadId in
            guard let threadId else { return }
            open(threadId)
            pendingThreadId = nil
        }
    }

    @ViewBuilder
    private var content: some View {
        if client.pairingCode != nil {
            PairingView(client: client)
        } else if client.state == .unpaired {
            PairingPromptView(client: client)
        } else if client.state == .identityChanged {
            IdentityChangedView(client: client)
        } else {
            switch route {
            case .threads:
                ThreadListView(client: client, onOpen: open)
            case .thread(let threadId):
                ThreadDetailView(
                    client: client,
                    threadId: threadId,
                    onBack: {
                        client.closeThread()
                        route = .threads
                    }
                )
            case .settings:
                SettingsView(client: client, onBack: { route = .threads })
            }
        }
    }

    private func open(_ threadId: Int64) {
        route = .thread(threadId)
        client.openThread(threadId)
    }
}

private struct StatusBar: View {
    @ObservedObject var client: BridgeClient
    @Binding var route: RootView.Route

    var body: some View {
        HStack(spacing: 6) {
            Circle()
                .fill(indicatorColor)
                .frame(width: 7, height: 7)
            Text(statusText)
                .font(.caption)
                .foregroundStyle(.secondary)
                .lineLimit(1)
                .truncationMode(.tail)
            Spacer()
            IconButton(
                systemName: "gearshape",
                help: "Settings",
                action: { route = route == .settings ? .threads : .settings }
            )
        }
        .padding(.horizontal, Header.horizontalPadding)
        .padding(.vertical, 6)
    }

    private var indicatorColor: Color {
        switch client.state {
        case .connected: return .green
        case .connecting, .searching, .pairing: return .orange
        case .offline, .unpaired, .identityChanged: return .secondary
        }
    }

    private var statusText: String {
        switch client.state {
        case .connected:
            return client.phoneName ?? "Connected"
        case .connecting:
            return "Connecting…"
        case .searching:
            return "Looking for \(client.phone?.deviceName ?? "your phone")…"
        case .pairing:
            return "Waiting for the phone to scan…"
        case .unpaired:
            return "Not paired"
        case .identityChanged:
            return "Phone identity changed"
        case .offline(let message):
            if let syncedAt = client.lastSyncedAt {
                return "Offline · synced \(syncedAt.formatted(.relative(presentation: .numeric)))"
            }
            return message
        }
    }
}
