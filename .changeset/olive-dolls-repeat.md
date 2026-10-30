---
"@relaybird/android": patch
"@relaybird/macos": patch
---

Stop the ongoing notification reappearing after it is swiped away. It tracked the connection
and so was re-posted on every change, which on a busy link meant seconds after a dismissal.
