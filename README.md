# SMS Bridge

Read your Android phone's text messages from the macOS menu bar. Over your Wi-Fi, directly —
no account, no server, nothing leaves the network.

- **`android/`** — Kotlin + Compose. Reads SMS, serves them over TLS, advertises itself by mDNS.
- **`macos/`** — Swift + SwiftUI. Menu bar popover: conversations, then the thread.

Pairing works like WhatsApp Web: the Mac shows a QR code, the phone scans it, and every later
reconnection is silent.

## Requirements

Xcode 16+, JDK 17, the Android SDK (`ANDROID_HOME`), `xcodegen` (`brew install xcodegen`),
Node 20+ and pnpm.

## Getting started

```sh
pnpm install          # wires the workspaces (there are no JS dependencies)
pnpm test             # Android unit tests + macOS test suite
pnpm build            # debug builds of both apps

pnpm dev:android      # install the debug APK on a connected device
pnpm dev:macos        # build and launch the Mac app
pnpm device-test      # on-device handshake tests (needs a device or emulator)
pnpm logs             # tail the phone's bridge logs
```

The workspace is a task runner over Gradle and Xcode — the repo contains no JavaScript.
Per-package scripts live in `android/package.json` and `macos/package.json`; run one directly
with `pnpm android <script>` or `pnpm macos <script>`.

## Pairing

1. Run the Android app, grant the SMS and contacts permissions, and turn on **Mirror to my Macs**.
2. Launch the Mac app, click the menu bar icon, and choose **Show pairing code**.
3. On the phone tap **Pair a Mac** and scan the QR. The code is valid for two minutes.

After that the Mac finds the phone by itself whenever both are on the same network.

To revoke a Mac, remove it from **Paired Macs** on the phone; its session drops immediately.

## How it works

The phone is the server: it owns the messages, listens on a TLS socket and advertises
`_smsbridge._tcp`. The Mac is always the client. Frames are length-prefixed JSON — no HTTP, no
WebSocket.

Pairing is mutually authenticated with HMAC-SHA256 and binds the QR secret to the exact
certificate the Mac saw, so a man in the middle cannot complete it, and the Mac does not commit
the certificate pin until the phone has proved it knows the secret. `docs/PROTOCOL.md` is the
source of truth, and its test vectors are asserted by both test suites.

A few decisions worth knowing about, because they are not obvious and they were all learned the
hard way:

- The TLS key is a software key sealed at rest with AndroidKeyStore AES-GCM, not an
  AndroidKeyStore key directly. Opaque keystore keys are a well-trodden path for *client*
  certificates; as a TLS *server* key the handshake dies inside Conscrypt.
- The certificate needs real X.509v3 extensions and must not claim `keyEncipherment` — it is an
  EC key, and Apple's TLS stack rejects the certificate outright, before any custom verification
  gets a say.
- A `sec_protocol_options_set_verify_block` must be installed *before* `NWParameters` is built.
  `NWParameters` snapshots the options, so a block added afterwards is silently ignored.
- The foreground service type is `remoteMessaging`. `dataSync` is capped at six hours a day on
  Android 15+ and then refuses to restart.
- The service holds a Wi-Fi `MulticastLock`. Without it the radio drops multicast in hardware
  once the screen is off and the phone simply vanishes from the network.

## Limits

- SMS only. MMS and group threads are out of scope: there is no public API to enumerate a
  thread's recipients.
- A non-default SMS app can only read the inbox and sent boxes — no drafts or outbox.
- Read-only: you cannot send from the Mac.
- Not distributable on Google Play, which restricts SMS permissions to default SMS/Phone/
  Assistant handlers. Sideload the signed APK.

## Signing

Both are gitignored local files, with templates next to them:

- `android/keystore.properties` — falls back to the debug key when absent.
- `macos/Config/Signing.local.xcconfig` — falls back to ad-hoc signing when absent.

Then `pnpm release` builds both, and `pnpm macos notarize` submits and staples the Mac app.
