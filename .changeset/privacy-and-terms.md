---
"@relaybird/web": minor
---

The site has a privacy policy and terms of use, linked from the footer and listed in the
sitemap. Google Play requires a hosted privacy policy for anything touching SMS, and there
was not one.

Both are written from what the code does rather than from a template, so the policy states
the things that are actually true and unusual here: the Android app sends no analytics at
all, its only outbound request being the release-version check; the Mac app's usage events
are a fixed server-enforced vocabulary that cannot carry a message, a number or a name; and
because those events are relayed through this site rather than posted to the analytics
vendor, the vendor never sees a user's address.
