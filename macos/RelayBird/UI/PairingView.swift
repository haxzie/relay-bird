import SwiftUI

struct PairingPromptView: View {
    @ObservedObject var client: BridgeClient

    var body: some View {
        VStack(spacing: 12) {
            Image(systemName: "iphone.gen3.radiowaves.left.and.right")
                .font(.system(size: 32))
                .foregroundStyle(.tertiary)
            Text("Pair your phone").font(.headline)
            Text("Install RelayBird on your Android phone, tap \"Pair a Mac\", then scan the code this Mac shows.")
                .font(.caption)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
            Button("Show pairing code") { client.beginPairing() }
                .buttonStyle(.borderedProminent)
        }
        .padding(24)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

struct PairingView: View {
    @ObservedObject var client: BridgeClient
    @State private var secondsRemaining = Int(PairingCode.lifetime)

    private let tick = Timer.publish(every: 1, on: .main, in: .common).autoconnect()

    var body: some View {
        VStack(spacing: 10) {
            Text("Scan with the phone")
                .font(.headline)

            if let image = client.pairingCode?.qrImage() {
                Image(nsImage: image)
                    .interpolation(.none)
                    .padding(8)
                    .background(Color.white, in: RoundedRectangle(cornerRadius: 8))
            }

            Text("Open RelayBird on your phone and tap \"Pair a Mac\".")
                .font(.caption)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)

            Text(secondsRemaining > 0 ? "Expires in \(secondsRemaining)s" : "Expired")
                .font(.caption2)
                .monospacedDigit()
                .foregroundStyle(secondsRemaining > 15 ? .secondary : Color.orange)

            HStack {
                Button("Cancel") { client.cancelPairing() }
                Button("New code") { client.beginPairing() }
            }
            .controlSize(.small)
        }
        .padding(16)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .onReceive(tick) { _ in
            secondsRemaining = client.pairingCode?.secondsRemaining ?? 0
        }
    }
}

struct IdentityChangedView: View {
    @ObservedObject var client: BridgeClient

    var body: some View {
        VStack(spacing: 12) {
            Image(systemName: "exclamationmark.shield")
                .font(.system(size: 32))
                .foregroundStyle(.orange)
            Text("Phone identity changed").font(.headline)
            Text("""
            A device on this network claims to be your phone but presented a different \
            certificate. That happens after reinstalling the phone app or clearing its data — \
            and it is also what an impostor looks like.

            Nothing was synced. Pair again only if you reinstalled the app yourself.
            """)
            .font(.caption)
            .foregroundStyle(.secondary)
            .multilineTextAlignment(.leading)

            Button("Forget this phone and pair again") {
                client.unpair()
                client.beginPairing()
            }
            .buttonStyle(.borderedProminent)
        }
        .padding(20)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}
