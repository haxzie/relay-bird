import { DOCK_APPS } from './DockIcons'
import { Mark } from './Mark'
import { Wallpaper } from './Wallpaper'
import {
  BatteryGlyph,
  ChecksGlyph,
  ControlCentreGlyph,
  LaptopGlyph,
  PhoneGlyph,
  SearchGlyph,
  SignalGlyph,
  WifiGlyph,
} from './Glyphs'

/**
 * The product shot: a MacBook with the menu-bar panel open, and the phone it is mirroring
 * standing in front of it.
 *
 * Drawn rather than screenshotted. A screenshot would be two real devices, a staged inbox and
 * a re-shoot every time the UI moves; this stays legible at any width, weighs nothing, and
 * costs no asset hosting. Everything is sized in `em` over a `cqw` root (see .stage in
 * globals.css), so the whole composition scales with its container and nothing reflows.
 */

/** Deliberately mundane: the OTPs and delivery notices this exists to save you reaching for. */
const THREADS = [
  { from: 'HDFC Bank', time: '2m', body: 'OTP 492013 for your transaction of ₹4,200. Valid for 10 minutes.', unread: true },
  { from: 'Amma', time: '18m', body: 'Call me once you reach home', unread: true },
  { from: 'Swiggy', time: '1h', body: 'Your order from Empire Restaurant has been delivered.', unread: false },
  { from: 'Vodafone Idea', time: '3h', body: 'You have used 80% of your data for this cycle.', unread: false },
  { from: 'Ravi', time: 'Yesterday', body: 'sent over the invoice, have a look when free', unread: false },
]

export function HeroMock() {
  return (
    <div className="stage" aria-hidden>
      <div className="stage-inner">
        <MacBook />
        <Phone />
      </div>
    </div>
  )
}

export function MacBook({ screen }: { screen?: React.ReactNode }) {
  return (
    <div className="mb">
      <div className="mb-lid">
        <div className="mb-screen">
          <MenuBar />
          <div className="mb-desktop">
            <Wallpaper />
          </div>
          <Dock />
          {/* The menu-bar popover by default; callers can put something else on the screen,
              which is how the pairing shot is built without a second MacBook. */}
          {screen ?? <Panel />}
        </div>
        {/* Sits over the menu bar, where it does on every MacBook since 2021. */}
        <div className="mb-notch" />
      </div>
      {/* The hinge is wider than the lid, and the front edge dips for the opening. */}
      <div className="mb-base">
        <span className="mb-dip" />
      </div>
    </div>
  )
}

function MenuBar() {
  return (
    <div className="menubar">
      <span className="menu-app">Finder</span>
      <span className="menu-item">File</span>
      <span className="menu-item">Edit</span>
      <span className="menu-item">View</span>
      <span className="menu-item">Go</span>
      <span className="menu-item">Window</span>
      <span className="menu-item">Help</span>

      <div className="menu-right">
        {/* Lit the way macOS lights a menu-bar extra whose panel is showing. */}
        <span className="menu-active">
          <Mark className="menu-mark" />
        </span>
        <BatteryGlyph className="menu-glyph menu-glyph-battery" />
        <WifiGlyph className="menu-glyph" />
        <SearchGlyph className="menu-glyph" />
        <ControlCentreGlyph className="menu-glyph" />
        <span className="menu-clock">Fri 31 Oct 9:41</span>
      </div>
    </div>
  )
}

function Dock() {
  return (
    <div className="dock">
      {DOCK_APPS.map((app) => (
        <span key={app.name} className="dock-tile">
          {app.icon}
        </span>
      ))}
      <span className="dock-divider" />
      <span className="dock-tile dock-app">
        <Mark className="dock-mark" />
        <i className="dock-running" />
      </span>
    </div>
  )
}

function Panel() {
  return (
    <div className="panel">
      <div className="panel-arrow" />
      <div className="panel-header">
        <Mark className="panel-mark" />
        <span className="panel-title">Messages</span>
        <span className="panel-spacer" />
        <span className="panel-unread">2 unread</span>
        <ChecksGlyph className="panel-checks" />
      </div>

      <div className="panel-rows">
        {THREADS.map((thread) => (
          <div className="row" key={thread.from}>
            <span className={thread.unread ? 'row-dot row-dot-on' : 'row-dot'} />
            <div className="row-body">
              <div className="row-top">
                <span className={thread.unread ? 'row-from row-from-unread' : 'row-from'}>
                  {thread.from}
                </span>
                <span className="row-time">{thread.time}</span>
              </div>
              <p className="row-snippet">{thread.body}</p>
            </div>
          </div>
        ))}
      </div>

      <div className="panel-status">
        <span className="live-dot" />
        <span>Connected to Pixel 8</span>
      </div>
    </div>
  )
}

export function Phone() {
  return (
    <div className="phone">
      <div className="phone-screen">
        <div className="phone-lens" />

        {/* Android puts the clock on the left and the radios on the right — the single
            clearest tell that this is not an iPhone. */}
        <div className="phone-statusbar">
          <span className="phone-clock">9:41</span>
          <span className="phone-status-icons">
            <SignalGlyph className="phone-status-signal" />
            <WifiGlyph className="phone-status-glyph" />
            <BatteryGlyph className="phone-status-battery" />
          </span>
        </div>

        <div className="phone-brand">
          <Mark className="phone-mark" />
          <span>RelayBird</span>
        </div>

        <div className="phone-card">
          {/* The same phone-to-Mac illustration the Android home screen draws. */}
          <div className="link">
            <span className="link-tile">
              <PhoneGlyph className="link-icon" />
            </span>
            <span className="link-dots">
              {[0, 1, 2, 3, 4].map((i) => (
                <i key={i} />
              ))}
            </span>
            <span className="link-tile">
              <LaptopGlyph className="link-icon" />
            </span>
          </div>
          <p className="phone-card-title">Mirror to my Macs</p>
          <p className="phone-card-sub">Connected now</p>
          <div className="phone-button">Pair a Mac</div>
        </div>

        <p className="phone-label">PAIRED MACS</p>
        <div className="phone-device">
          <span className="phone-device-name">MacBook Pro</span>
          <span className="live-dot" />
        </div>
        <div className="phone-device">
          <span className="phone-device-name">Mac mini</span>
          <span className="phone-device-seen">2d</span>
        </div>

        <div className="phone-home" />
      </div>
    </div>
  )
}
