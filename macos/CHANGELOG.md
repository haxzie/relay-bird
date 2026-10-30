# @relaybird/macos

## 1.1.1

### Patch Changes

- 4f8f47b: Stop the ongoing notification reappearing after it is swiped away. It tracked the connection
  and so was re-posted on every change, which on a busy link meant seconds after a dismissal.

## 1.1.0

### Minor Changes

- 02bfe70: Build, version and release both apps from CI. One version covers the pair, since the protocol
  gates them against each other, and a tag produces a signed APK alongside a signed, notarised
  Mac app in a single GitHub release.
