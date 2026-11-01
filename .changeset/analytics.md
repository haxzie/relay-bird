---
"@relaybird/macos": minor
"@relaybird/web": minor
---

Product analytics. The site reports page views and download clicks to Google Analytics and
PostHog. The Mac app reports a small set of anonymous events — launched, paired, connected,
forgotten, notification clicked — to our own `/api/events`, which relays to PostHog.

The endpoint validates rather than forwards: a fixed list of event names, a fixed list of
scalar properties, and a `distinctId` that must be a bare UUID. Message content, phone
numbers and contact names have no path through it. The app's identifier is random per
install, and Settings has a toggle to stop sending and a button to reset the identifier.

The dev server also gets its own build directory, so building the Worker no longer breaks a
running `next dev`.
