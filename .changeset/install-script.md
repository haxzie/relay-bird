---
"@relaybird/web": minor
---

An install script at relaybird.haxzie.com/install.sh that sets up both halves: the Mac app
from the disk image, and the Android app over ADB, which is the only route Play Protect
allows for an app that reads SMS. It verifies the Mac app's signature before installing and
walks through enabling USB debugging when no phone is attached, re-checking until the
phone appears. With more than one device connected it lists them by name and asks which to
install on, rather than failing the way `adb install` does.

The home page now has one Download button pointing at /download rather than a platform
choice, since the install route needs more explanation than fits beside a button.
