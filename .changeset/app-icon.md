---
"@relaybird/macos": patch
---

The app icon is a proper macOS icon again. On macOS 26 it had been rendering as a
sharp-cornered black square sitting on a light plate, wherever icons are listed — the Dock,
Finder, Raycast.

macOS 26 does not mask a legacy icon to the system shape; it scales the artwork down and
composites it onto a plate of its own. Rounding the artwork first does not help, because the
system still insets it and the result is one squircle inside another. The icon is now an
Icon Composer asset, which the system draws natively at full bleed. macOS 14 and 15 keep a
correct icon from the `.icns` built alongside it.
