# @relaybird/android

## 1.3.0

## 1.2.0

### Minor Changes

- a4cdb26: The Android app now says when a newer release exists, since a sideloaded app has nothing
  updating it and the Mac half updates itself into refusing to talk to an old phone. It only
  tells you — installing stays manual, so no install-packages permission.

  Releases mirror the APK to R2 alongside the disk image, and `latest.json` describes both
  halves, so the site links the real artefacts instead of the releases page.

  The site gains a download page, a blog, an about page, an OG image, a favicon, robots.txt
  and a sitemap.

- 8e74ab0: Show the paired devices with a tick once a Mac is connected, on their own panel at the top of
  the card, and use the same device outline as the notification icon.
- f58c3b2: In-app updates for the Mac app. A pill appears beside the settings gear when a newer
  version is on the feed; clicking it downloads, verifies and installs through Sparkle.

  Releases now mirror the notarised DMG to R2 under `assets.haxzie.com/relaybird`,
  alongside a signed Sparkle appcast and a `latest.json` for the download page. The new
  `web` package is the site and the API that serves both.

### Patch Changes

- 70f9a27: Ask for notification permission while the app is frontmost rather than at launch, say so in
  settings when macOS is blocking notifications, and rework the settings screen into labelled
  sections. The Mac app also reports its real version rather than 1.0.

## 1.1.1

### Patch Changes

- 4f8f47b: Stop the ongoing notification reappearing after it is swiped away. It tracked the connection
  and so was re-posted on every change, which on a busy link meant seconds after a dismissal.

## 1.1.0

### Minor Changes

- 02bfe70: Build, version and release both apps from CI. One version covers the pair, since the protocol
  gates them against each other, and a tag produces a signed APK alongside a signed, notarised
  Mac app in a single GitHub release.
