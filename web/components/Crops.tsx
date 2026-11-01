import { AndroidGlyph, BatteryGlyph, LockGlyph, SignalGlyph, WifiGlyph } from './Glyphs'
import { Mark } from './Mark'

/**
 * Recreations of the two apps' own interfaces, cut off by the tile they sit in.
 *
 * Drawn rather than screenshotted for the same reason as the hero: a real screenshot would
 * need re-taking every time the UI moves, and would carry a staged inbox with it. Each one is
 * positioned to run off the bottom or the right of its tile, so the tile reads as a window
 * onto the app rather than a box with a picture centred in it.
 */

/** The menu-bar popover: header, then threads running off the bottom edge. */
export function MacPanelCrop() {
  const rows = [
    { from: 'HDFC Bank', time: '2m', body: 'OTP 492013 for your transaction of ₹4,200.', unread: true },
    { from: 'Amma', time: '18m', body: 'Call me once you reach home', unread: true },
    { from: 'Swiggy', time: '1h', body: 'Your order has been delivered.', unread: false },
  ]
  return (
    <div className="w-[19rem] overflow-hidden rounded-xl bg-[#161618] shadow-2xl ring-1 ring-white/10">
      <div className="flex items-center gap-2 border-b border-white/10 px-3 py-2.5">
        <Mark className="h-3 w-3" />
        <span className="text-[11px] font-semibold">Messages</span>
        <span className="ml-auto text-[10px] text-muted">2 unread</span>
      </div>
      {rows.map((r) => (
        <div key={r.from} className="flex gap-2 border-b border-white/[0.06] px-3 py-2">
          <span
            className={`mt-1 h-1.5 w-1.5 flex-none rounded-full ${r.unread ? 'bg-accent' : 'bg-transparent'}`}
          />
          <div className="min-w-0 flex-1">
            <div className="flex items-baseline gap-2">
              <span className={`text-[11px] ${r.unread ? 'font-semibold' : ''}`}>{r.from}</span>
              <span className="ml-auto text-[9px] text-muted">{r.time}</span>
            </div>
            <p className="truncate text-[10px] text-muted">{r.body}</p>
          </div>
        </div>
      ))}
    </div>
  )
}

/** The Android home screen, cropped to the card that matters. */
export function AndroidCrop() {
  return (
    <div className="w-[12.5rem] overflow-hidden rounded-[1.4rem] bg-[#0a0a0b] p-2 shadow-2xl ring-1 ring-white/10">
      <div className="flex items-center justify-between px-1.5 pb-1.5 pt-0.5 text-[8px] text-white/80">
        <span className="font-semibold">9:41</span>
        <span className="flex items-center gap-1">
          <SignalGlyph className="h-1.5 w-2" />
          <WifiGlyph className="h-2 w-2" />
          <BatteryGlyph className="h-1.5 w-3" />
        </span>
      </div>
      <div className="flex items-center gap-1.5 px-1.5 pb-2">
        <Mark className="h-2.5 w-2.5" />
        <span className="text-[10px] font-semibold">RelayBird</span>
      </div>
      <div className="rounded-xl bg-[#141414] p-3 text-center">
        <p className="text-[10px] font-semibold">Mirror to my Macs</p>
        <p className="mt-0.5 text-[8px] text-live">Connected now</p>
        <div className="mt-2 rounded-full bg-white py-1 text-[9px] font-semibold text-black">
          Pair a Mac
        </div>
      </div>
      <p className="px-1.5 pb-1 pt-2 text-[7px] tracking-[0.1em] text-muted">PAIRED MACS</p>
      <div className="flex items-center rounded-lg bg-[#141414] px-2 py-1.5">
        <span className="flex-1 text-[9px]">MacBook Pro</span>
        <span className="h-1.5 w-1.5 rounded-full bg-live" />
      </div>
    </div>
  )
}

/** macOS notification banners, as they actually stack up in the corner. */
const BANNERS = [
  { from: 'HDFC Bank', when: 'now', body: 'OTP 492013 for your transaction of ₹4,200. Valid for 10 minutes.' },
  { from: 'Khabib', when: '2m ago', body: 'Send me location' },
  { from: 'Swiggy', when: '5m ago', body: 'Your order from Empire Restaurant has been delivered.' },
]

export function NotificationCrop() {
  return (
    <div className="flex w-full flex-col gap-2">
      {BANNERS.map((n) => (
        <div
          key={n.from}
          className="w-full rounded-xl bg-[#1c1c20]/55 p-3 shadow-2xl ring-1 ring-white/15 backdrop-blur-xl backdrop-saturate-150"
        >
          <div className="flex gap-2.5">
            <span className="grid h-7 w-7 flex-none place-items-center rounded-lg bg-white shadow-sm">
              <Mark className="h-4 w-4 text-black" />
            </span>
            <div className="min-w-0 flex-1">
              <div className="flex items-baseline gap-2">
                <span className="text-[11px] font-semibold">{n.from}</span>
                <span className="ml-auto text-[9px] text-white/50">{n.when}</span>
              </div>
              <p className="text-[10px] leading-snug text-white/75">{n.body}</p>
            </div>
          </div>
        </div>
      ))}
    </div>
  )
}

/** The settings row that proves the pin, fingerprint and all. */
export function PinnedCrop() {
  return (
    <div className="w-full rounded-xl bg-[#161618] p-3 shadow-2xl ring-1 ring-white/10">
      <div className="flex items-center gap-2">
        <LockGlyph className="h-3.5 w-3.5 text-live" />
        <span className="text-[11px] font-semibold">Pixel 8</span>
        <span className="ml-auto rounded-full bg-live/15 px-1.5 py-0.5 text-[8px] text-live">
          pinned
        </span>
      </div>
      <p className="mt-2 font-mono text-[9px] leading-relaxed text-muted">
        a4:91:c0:7e:2b:d8:16:ff
        <br />
        3c:95:ab:40:dd:71:0e:62
      </p>
    </div>
  )
}

/** The Android ongoing notification — one line, no sound, no badge. */
export function OngoingCrop() {
  return (
    <div className="w-full rounded-xl bg-[#1d1d20] p-2.5 shadow-2xl ring-1 ring-white/10">
      <div className="flex items-center gap-2">
        <Mark className="h-2.5 w-2.5 text-muted" />
        <span className="text-[9px] text-muted">RelayBird</span>
        <span className="text-[9px] text-muted">·</span>
        <span className="text-[9px] text-muted">now</span>
        <AndroidGlyph className="ml-auto h-2 w-auto text-muted" />
      </div>
      <p className="mt-1 text-[11px] font-medium">Mirroring to MacBook Pro</p>
      <p className="text-[9px] text-muted">Tap to stop</p>
    </div>
  )
}

/** The permission sheet macOS puts up — the product's whole claim, in Apple's own words. */
export function LocalNetworkCrop() {
  return (
    <div className="w-[17rem] rounded-2xl bg-[#232326]/95 p-4 text-center shadow-2xl ring-1 ring-white/10 backdrop-blur">
      <span className="mx-auto grid h-10 w-10 place-items-center rounded-xl bg-white">
        <Mark className="h-6 w-6 text-black" />
      </span>
      <p className="mt-3 text-[11px] font-semibold leading-snug">
        &ldquo;RelayBird&rdquo; would like to find and connect to devices on your local network.
      </p>
      <div className="mt-3 flex gap-2">
        <span className="flex-1 rounded-md bg-white/10 py-1.5 text-[10px]">Don&rsquo;t Allow</span>
        <span className="flex-1 rounded-md bg-[#3b82f6] py-1.5 text-[10px] font-medium">Allow</span>
      </div>
    </div>
  )
}
