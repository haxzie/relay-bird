---
"@relaybird/android": minor
"@relaybird/macos": minor
---

Both apps now identify as `com.haxzie.relaybird` rather than `dev.haxzie.relaybird`, which
matches the domain the project actually lives on. The Android half needed a settled
application id before its first Google Play upload, because Play fixes it permanently at
that point; the Mac half moved with it so the two do not disagree.

**This release does not upgrade an existing install, on either platform.** Both systems
treat an identifier change as a different application:

- The new APK installs *alongside* an existing RelayBird rather than replacing it. Remove
  the old one with `adb uninstall dev.haxzie.relaybird`, or from the launcher.
- Sparkle cannot deliver an update across a bundle identifier change, so a Mac on 1.4.0 or
  earlier will not be offered this version and has to download it once by hand. Its paired
  phone and its settings do not carry over — the keychain item belongs to the old
  identifier's access group, and the preferences live in the old sandbox container — so
  pairing has to be done again.

Nothing about the wire protocol changed; a renamed phone and a renamed Mac pair exactly as
before.
