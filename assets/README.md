# Brand assets

`relaybird-mark.svg` is the source of truth — a single white path on an 842×842 viewBox.

**Android** vector drawables in `android/app/src/main/res/drawable/` embed that path directly
(`ic_relaybird`, `ic_notification`, `ic_launcher_foreground`). If the mark changes, copy the new
`pathData` into all three. The launcher foreground additionally scales the art to 0.509 so it
clears the adaptive icon's safe zone — see the comment in that file before changing it.

**macOS** icons are generated into `macos/RelayBird/Resources/Assets.xcassets`:

```sh
pnpm macos icons
```

That runs `tools/generate-macos-icons.swift` over `relaybird-mark-2048.png`, which produces the
rounded app icon tile at every required size plus the alpha-only menu bar and in-app marks.
