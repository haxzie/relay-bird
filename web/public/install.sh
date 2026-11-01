#!/bin/sh
#
# RelayBird installer.
#
#   curl -fsSL https://relaybird.haxzie.com/install.sh | sh
#
# Installs the Mac app and, if a phone is attached, the Android app over ADB. Both halves
# come from the same release, which matters: they are gated against each other by protocol
# version and refuse to pair across versions.
#
# ADB is not an incidental choice. Google Play Protect blocks any app declaring SMS
# permissions when it is installed from a browser or file manager, so downloading the APK
# and tapping it does not work. A computer using the debug bridge is not one of those
# sources, so this route installs cleanly.
#
# Nothing here needs root, and every file is fetched over HTTPS from assets.haxzie.com.

set -eu

FEED="https://assets.haxzie.com/relaybird/latest.json"
APP_DIR="/Applications"

if [ -t 1 ] && [ -z "${NO_COLOR:-}" ]; then
  B=$(printf '\033[1m'); DIM=$(printf '\033[2m'); G=$(printf '\033[32m')
  Y=$(printf '\033[33m'); R=$(printf '\033[31m'); N=$(printf '\033[0m')
else
  B=''; DIM=''; G=''; Y=''; R=''; N=''
fi

say()  { printf '%s\n' "$*"; }
step() { printf '\n%s==>%s %s%s%s\n' "$G" "$N" "$B" "$*" "$N"; }
warn() { printf '%s!%s  %s\n' "$Y" "$N" "$*"; }
die()  { printf '%s✗%s  %s\n' "$R" "$N" "$*" >&2; exit 1; }
ask()  { printf '%s?%s  %s ' "$Y" "$N" "$*"; }

WORK=''
cleanup() {
  [ -n "$WORK" ] && [ -d "$WORK" ] && rm -rf "$WORK"
  # A failed run must not leave the disk image mounted.
  [ -n "${MOUNT:-}" ] && [ -d "${MOUNT:-}" ] && hdiutil detach "$MOUNT" -quiet 2>/dev/null || true
}
trap cleanup EXIT INT TERM

# --- the release -------------------------------------------------------------

# Pulls one string field out of a section of the feed. awk rather than jq or python: neither
# is guaranteed on a clean macOS, and awk is.
field() {
  printf '%s' "$FEED_JSON" | tr '\n' ' ' | awk -v sec="$1" -v key="$2" '{
    if (sec != "") {
      i = index($0, "\"" sec "\"")
      if (i == 0) exit 1
      rest = substr($0, i)
      $0 = substr(rest, 1, index(rest, "}"))
    }
    k = index($0, "\"" key "\"")
    if (k == 0) exit 1
    s = substr($0, k + length(key) + 2)
    s = substr(s, index(s, ":") + 1)
    s = substr(s, index(s, "\"") + 1)
    print substr(s, 1, index(s, "\"") - 1)
  }'
}

command -v curl >/dev/null 2>&1 || die "curl is required."

step "Looking up the latest release"
FEED_JSON=$(curl -fsSL --max-time 30 "$FEED") || die "Could not reach $FEED"
VERSION=$(field "" version) || die "The release feed looks malformed."
[ -n "$VERSION" ] || die "The release feed did not name a version."
say "   RelayBird ${B}${VERSION}${N}"

# --- the Mac app -------------------------------------------------------------

install_mac() {
  DMG_URL=$(field mac url); DMG_NAME=$(field mac filename)
  [ -n "$DMG_URL" ] || die "The feed has no macOS download."

  step "Installing the Mac app"
  say "   ${DIM}${DMG_NAME}${N}"
  curl -fsSL --max-time 300 -o "$WORK/app.dmg" "$DMG_URL" || die "Download failed."

  MOUNT="$WORK/mnt"
  mkdir -p "$MOUNT"
  hdiutil attach "$WORK/app.dmg" -nobrowse -quiet -mountpoint "$MOUNT" \
    || die "Could not open the disk image."

  SRC="$MOUNT/RelayBird.app"
  [ -d "$SRC" ] || die "The disk image did not contain RelayBird.app."

  # The build is signed and notarised, so this should always pass. If it does not, something
  # replaced the download in transit and installing it would be the wrong move.
  if ! spctl -a -t exec "$SRC" >/dev/null 2>&1; then
    die "The downloaded app failed Gatekeeper's signature check. Not installing it."
  fi
  say "   ${DIM}signature verified${N}"

  # Copy beside the existing app and swap, rather than deleting first. A failed or
  # interrupted copy must never leave someone with no app at all.
  STAGE="$APP_DIR/.RelayBird.app.incoming"
  rm -rf "$STAGE"
  if ! cp -R "$SRC" "$STAGE" 2>/dev/null; then
    rm -rf "$STAGE"
    die "Could not write to $APP_DIR. Re-run with sudo, or drag the app across yourself."
  fi

  if [ -d "$APP_DIR/RelayBird.app" ]; then
    # Replacing a running app leaves the old process running against deleted files.
    pkill -x RelayBird 2>/dev/null && sleep 1 || true
    rm -rf "$APP_DIR/RelayBird.app"
  fi
  mv "$STAGE" "$APP_DIR/RelayBird.app"

  hdiutil detach "$MOUNT" -quiet 2>/dev/null || true
  MOUNT=''
  say "   ${G}installed${N} to $APP_DIR/RelayBird.app"
}

# --- the Android app ---------------------------------------------------------

find_adb() {
  if command -v adb >/dev/null 2>&1; then printf 'adb'; return 0; fi
  for candidate in \
    "$HOME/Library/Android/sdk/platform-tools/adb" \
    "/opt/homebrew/bin/adb" \
    "/usr/local/bin/adb"
  do
    [ -x "$candidate" ] && { printf '%s' "$candidate"; return 0; }
  done
  return 1
}

install_android() {
  APK_URL=$(field android url); APK_NAME=$(field android filename)
  [ -n "$APK_URL" ] || { warn "The feed has no Android download; skipping the phone."; return 0; }

  step "Installing the Android app"

  if ! ADB=$(find_adb); then
    warn "ADB is not installed, so the phone half cannot be installed here."
    say ""
    say "   Install it with:  ${B}brew install --cask android-platform-tools${N}"
    say "   then run this script again."
    say ""
    say "   ${DIM}Without ADB the APK has to be installed from the phone, and Android"
    say "   blocks that for apps that read SMS. See relaybird.haxzie.com/blog.${N}"
    return 0
  fi

  say "   ${DIM}using $ADB${N}"

  if [ -z "$("$ADB" devices | sed -n '2,$p' | grep -w device || true)" ]; then
    say ""
    say "   ${B}Connect your phone${N}"
    say "   1. Settings ${DIM}→${N} About phone ${DIM}→${N} tap ${B}Build number${N} seven times"
    say "   2. Settings ${DIM}→${N} System ${DIM}→${N} Developer options ${DIM}→${N} turn on ${B}USB debugging${N}"
    say "   3. Plug the phone into this Mac with a cable"
    say "   4. On the phone, tap ${B}Allow${N} when it asks about USB debugging"
    say ""
    ask "Press enter once the phone is connected, or Ctrl-C to skip:"
    # Reading from the terminal, not stdin: stdin is the script itself under `curl | sh`.
    if [ -r /dev/tty ]; then read -r _ < /dev/tty; else warn "No terminal to prompt on; skipping."; return 0; fi

    say "   waiting for the phone…"
    if ! "$ADB" wait-for-device 2>/dev/null; then
      warn "No phone appeared. Run the script again once it is connected."
      return 0
    fi
  fi

  UNAUTH=$("$ADB" devices | sed -n '2,$p' | grep -w unauthorized || true)
  if [ -n "$UNAUTH" ]; then
    warn "The phone is connected but has not authorised this Mac."
    say "   Unlock it and tap ${B}Allow USB debugging${N}, then run this script again."
    return 0
  fi

  MODEL=$("$ADB" shell getprop ro.product.model 2>/dev/null | tr -d '\r' || true)
  [ -n "$MODEL" ] && say "   ${DIM}found $MODEL${N}"

  say "   ${DIM}${APK_NAME}${N}"
  curl -fsSL --max-time 300 -o "$WORK/app.apk" "$APK_URL" || die "APK download failed."

  # -r keeps existing data, so pairings survive an upgrade.
  if "$ADB" install -r "$WORK/app.apk" >/dev/null 2>&1; then
    say "   ${G}installed${N} on $([ -n "$MODEL" ] && printf '%s' "$MODEL" || printf 'your phone')"
  else
    OUT=$("$ADB" install -r "$WORK/app.apk" 2>&1 || true)
    case "$OUT" in
      *INSTALL_FAILED_UPDATE_INCOMPATIBLE*|*signatures+do+not+match*|*INSTALL_FAILED_VERSION_DOWNGRADE*)
        warn "A different build of RelayBird is already installed."
        say "   Remove it first:  ${B}$ADB uninstall dev.haxzie.relaybird${N}"
        ;;
      *)
        warn "The install did not complete:"
        printf '%s\n' "$OUT" | sed 's/^/     /'
        ;;
    esac
    return 0
  fi
}

# --- run ---------------------------------------------------------------------

[ "$(uname -s)" = "Darwin" ] || die "This installer is for macOS. RelayBird's other half is an Android app."

WORK=$(mktemp -d)
install_mac
install_android

step "Done"
say "   Open RelayBird from Applications. It lives in the menu bar."
say "   Then open it on your phone and scan the code to pair."
say ""
say "   ${DIM}Both devices need to be on the same Wi-Fi network.${N}"
say ""
