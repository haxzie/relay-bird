import ServiceManagement
import SwiftUI

struct SettingsView: View {
    @ObservedObject var client: BridgeClient
    let onBack: () -> Void

    @State private var notificationsEnabled = AppSettings.notificationsEnabled
    @State private var manualEndpoint = AppSettings.manualEndpoint ?? ""
    @State private var manualEndpointInvalid = false
    @State private var launchAtLogin = SMAppService.mainApp.status == .enabled

    var body: some View {
        ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    if let phone = client.phone {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(phone.deviceName).font(.subheadline.weight(.medium))
                            Text("Paired \(phone.pairedAt.formatted(date: .abbreviated, time: .shortened))")
                                .font(.caption2)
                                .foregroundStyle(.secondary)
                            Text("Shows on the phone as \"\(phone.macName)\"")
                                .font(.caption2)
                                .foregroundStyle(.secondary)
                        }
                    }

                    Toggle("Notify me about new messages", isOn: $notificationsEnabled)
                        .onChange(of: notificationsEnabled) { _, value in
                            AppSettings.notificationsEnabled = value
                        }

                    Toggle("Open at login", isOn: $launchAtLogin)
                        .onChange(of: launchAtLogin) { _, value in
                            try? value ? SMAppService.mainApp.register() : SMAppService.mainApp.unregister()
                            launchAtLogin = SMAppService.mainApp.status == .enabled
                        }

                    VStack(alignment: .leading, spacing: 4) {
                        Text("Phone address").font(.subheadline.weight(.medium))
                        Text("Leave empty to find the phone automatically. Set host:port when the network blocks mDNS, or to reach an emulator.")
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                        HStack {
                            TextField("192.168.1.42:45678", text: $manualEndpoint)
                                .textFieldStyle(.roundedBorder)
                                .font(.system(size: 12, design: .monospaced))
                            Button("Apply") { applyManualEndpoint() }
                                .controlSize(.small)
                        }
                        if manualEndpointInvalid {
                            Text("Expected host:port").font(.caption2).foregroundStyle(.red)
                        }
                    }

                    Divider()

                    Button("Reconnect now") { client.reconnectNow() }
                        .controlSize(.small)

                    Button("Forget this phone", role: .destructive) { client.unpair() }
                        .controlSize(.small)
                }
            .padding(14)
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
