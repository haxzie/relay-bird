# @relaybird/web

## 1.5.0

### Minor Changes

- 06effac: The site has a privacy policy and terms of use, linked from the footer and listed in the
  sitemap. Google Play requires a hosted privacy policy for anything touching SMS, and there
  was not one.

  Both are written from what the code does rather than from a template, so the policy states
  the things that are actually true and unusual here: the Android app sends no analytics at
  all, its only outbound request being the release-version check; the Mac app's usage events
  are a fixed server-enforced vocabulary that cannot carry a message, a number or a name; and
  because those events are relayed through this site rather than posted to the analytics
  vendor, the vendor never sees a user's address.

## 1.4.0

### Minor Changes

- 950eae6: An install script at relaybird.haxzie.com/install.sh that sets up both halves: the Mac app
  from the disk image, and the Android app over ADB, which is the only route Play Protect
  allows for an app that reads SMS. It verifies the Mac app's signature before installing and
  walks through enabling USB debugging when no phone is attached, re-checking until the
  phone appears. With more than one device connected it lists them by name and asks which to
  install on, rather than failing the way `adb install` does.

  The home page now has one Download button pointing at /download rather than a platform
  choice, since the install route needs more explanation than fits beside a button.

## 1.3.0

### Minor Changes

- 8d74858: Product analytics. The site reports page views and download clicks to Google Analytics and
  PostHog. The Mac app reports a small set of anonymous events — launched, paired, connected,
  forgotten, notification clicked — to our own `/api/events`, which relays to PostHog.

  The endpoint validates rather than forwards: a fixed list of event names, a fixed list of
  scalar properties, and a `distinctId` that must be a bare UUID. Message content, phone
  numbers and contact names have no path through it. The app's identifier is random per
  install, and Settings has a toggle to stop sending and a button to reset the identifier.

  The dev server also gets its own build directory, so building the Worker no longer breaks a
  running `next dev`.

## 1.2.0

### Minor Changes

- a4cdb26: The Android app now says when a newer release exists, since a sideloaded app has nothing
  updating it and the Mac half updates itself into refusing to talk to an old phone. It only
  tells you — installing stays manual, so no install-packages permission.

  Releases mirror the APK to R2 alongside the disk image, and `latest.json` describes both
  halves, so the site links the real artefacts instead of the releases page.

  The site gains a download page, a blog, an about page, an OG image, a favicon, robots.txt
  and a sitemap.

- f58c3b2: In-app updates for the Mac app. A pill appears beside the settings gear when a newer
  version is on the feed; clicking it downloads, verifies and installs through Sparkle.

  Releases now mirror the notarised DMG to R2 under `assets.haxzie.com/relaybird`,
  alongside a signed Sparkle appcast and a `latest.json` for the download page. The new
  `web` package is the site and the API that serves both.
