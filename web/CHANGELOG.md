# @relaybird/web

## 1.2.0

### Minor Changes

- f58c3b2: In-app updates for the Mac app. A pill appears beside the settings gear when a newer
  version is on the feed; clicking it downloads, verifies and installs through Sparkle.

  Releases now mirror the notarised DMG to R2 under `assets.haxzie.com/relaybird`,
  alongside a signed Sparkle appcast and a `latest.json` for the download page. The new
  `web` package is the site and the API that serves both.
