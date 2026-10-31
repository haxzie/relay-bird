import ServiceManagement
import SwiftUI
import UserNotifications

struct SettingsView: View {
    @ObservedObject var client: BridgeClient
    @ObservedObject var notifier: Notifier
    let onBack: () -> Void

    @State private var notificationsEnabled = AppSettings.notificationsEnabled
    @State private var manualEndpoint = AppSettings.manualEndpoint ?? ""
    @State private var manualEndpointInvalid = false
    @State private var launchAtLogin = SMAppService.mainApp.status == .enabled
    @State private var confirmingForget = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 18) {
                if let phone = client.phone {
                    VStack(alignment: .leading, spacing: 8) {
                        PhoneCard(phone: phone, connected: client.state == .connected)

                        if confirmingForget {
                            // Inline, like the mark-all prompt: a modal dismisses the popover
                            // it was launched from.
                            Callout(
                                symbol: "exclamationmark.triangle",
                                message: "Forgetting the phone deletes the pairing and the messages held on this Mac. You will need to scan a new code.",
                                action: "Forget",
                                destructive: true,
                                onAction: {
                                    client.unpair()
                                    confirmingForget = false
                                },
                                onCancel: { confirmingForget = false }
                            )
                        } else {
                            Button("Forget this phone", role: .destructive) {
                                confirmingForget = true
                            }
                            .controlSize(.small)
                        }
                    }
                }

                Section("Notifications") {
                    Toggle("Notify me about new messages", isOn: $notificationsEnabled)
                        .onChange(of: notificationsEnabled) { _, value in
                            AppSettings.notificationsEnabled = value
                        }
                    // Shown rather than left to be discovered: a denial is silent, and the
                    // only cure is a trip to System Settings.
                    if notifier.authorizationStatus == .denied {
                        Callout(
                            symbol: "bell.slash",
                            message: "macOS is blocking RelayBird's notifications, so nothing will appear however this is set.",
                            action: "Open System Settings",
                            onAction: notifier.openSystemSettings
                        )
                    }
                }

                Section("Startup") {
                    Toggle("Open at login", isOn: $launchAtLogin)
                        .onChange(of: launchAtLogin) { _, value in
                            try? value ? SMAppService.mainApp.register() : SMAppService.mainApp.unregister()
                            launchAtLogin = SMAppService.mainApp.status == .enabled
                        }
                }

                Section("Connection") {
                    Text("RelayBird finds your phone by itself. Set an address only when the network blocks discovery, or to reach an emulator.")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .fixedSize(horizontal: false, vertical: true)
                    HStack(spacing: 6) {
                        TextField("192.168.1.42:45678", text: $manualEndpoint)
                            .textFieldStyle(.roundedBorder)
                            .font(.system(size: 11, design: .monospaced))
                        Button("Apply", action: applyManualEndpoint)
                            .controlSize(.small)
                    }
                    if manualEndpointInvalid {
                        Text("Expected host:port").font(.caption2).foregroundStyle(.red)
                    }
                    Button("Reconnect now", action: client.reconnectNow)
                        .controlSize(.small)
                }

            }
            .padding(16)
        }
        .safeAreaInset(edge: .top, spacing: 0) {
            HStack(spacing: 8) {
                IconButton(systemName: "chevron.left", help: "Back to conversations", action: onBack)
                Text("Settings").font(.headline)
                Spacer()
            }
            .padding(.horizontal, Header.horizontalPadding)
            .frame(height: Header.height)
            .background(ProgressiveBlur())
        }
        .onAppear { notifier.refreshStatus() }
    }

    private func applyManualEndpoint() {
        let trimmed = manualEndpoint.trimmingCharacters(in: .whitespaces)
        if trimmed.isEmpty {
            AppSettings.manualEndpoint = nil
            manualEndpointInvalid = false
        } else if BridgeClient.parseEndpoint(trimmed) != nil {
            AppSettings.manualEndpoint = trimmed
            manualEndpointInvalid = false
        } else {
            manualEndpointInvalid = true
            return
        }
        client.reconnectNow()
    }
}

/// A labelled group, so the screen reads as a few short lists rather than one long column.
private struct Section<Content: View>: View {
    let title: String
    @ViewBuilder let content: Content

    init(_ title: String, @ViewBuilder content: () -> Content) {
        self.title = title
        self.content = content()
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title.uppercased())
                .font(.system(size: 10, weight: .semibold))
                .kerning(0.8)
                .foregroundStyle(.tertiary)
            content
        }
    }
}

private struct PhoneCard: View {
    let phone: PairedPhone
    let connected: Bool

    var body: some View {
        HStack(spacing: 10) {
            Image(systemName: "iphone")
                .font(.system(size: 18))
                .frame(width: 34, height: 34)
                .background(Color.primary.opacity(0.07), in: RoundedRectangle(cornerRadius: 9))
            VStack(alignment: .leading, spacing: 2) {
                Text(phone.deviceName).font(.subheadline.weight(.medium))
                HStack(spacing: 4) {
                    Circle()
                        .fill(connected ? Color.green : Color.secondary)
                        .frame(width: 6, height: 6)
                    Text(connected ? "Connected" : "Not connected")
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                }
                Text("Paired \(phone.pairedAt.formatted(date: .abbreviated, time: .omitted)) · shows as \"\(phone.macName)\"")
                    .font(.caption2)
                    .foregroundStyle(.tertiary)
                    .lineLimit(1)
            }
            Spacer(minLength: 0)
        }
        .padding(10)
        .background(Color.primary.opacity(0.05), in: RoundedRectangle(cornerRadius: 10))
    }
}

private struct Callout: View {
    let symbol: String
    let message: String
    let action: String
    var destructive: Bool = false
    let onAction: () -> Void
    var onCancel: (() -> Void)?

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(alignment: .top, spacing: 7) {
                Image(systemName: symbol)
                    .foregroundStyle(destructive ? Color.red : Color.orange)
                Text(message)
                    .font(.caption)
                    .fixedSize(horizontal: false, vertical: true)
            }
            HStack {
                Spacer()
                if let onCancel {
                    Button("Cancel", action: onCancel)
                }
                Button(action, role: destructive ? .destructive : nil, action: onAction)
            }
            .controlSize(.small)
        }
        .padding(10)
        .background(Color.primary.opacity(0.05), in: RoundedRectangle(cornerRadius: 10))
    }
}
