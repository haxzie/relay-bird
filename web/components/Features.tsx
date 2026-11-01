import { LocalNetworkCrop, NotificationCrop, OngoingCrop, PinnedCrop } from './Crops'
import { MacBook, Phone } from './HeroMock'

/**
 * The bento grid.
 *
 * Every tile is copy plus a recreated piece of one of the apps. The crops are deliberately
 * partial views — a panel showing its first few threads, a thread showing two bubbles — but
 * each one ends on a whole row and a whole sentence. Nothing is sliced through the middle of
 * its own content; the tile ends, the interface inside it does not get chopped.
 *
 * The spans only apply from `lg` up; below that the grid collapses to two columns and then
 * one, so nothing depends on the masonry holding together at every width.
 */

export function Features() {
  return (
    <section className="mt-28">
      <p className="text-xs uppercase tracking-[0.18em] text-muted">Why RelayBird</p>
      <h2 className="mt-3 text-3xl font-normal tracking-tight sm:text-4xl">
        Built like a desktop app
        <span className="block text-muted">because that is what it is</span>
      </h2>

      <div className="mt-10 grid gap-4 sm:grid-cols-2 lg:grid-cols-6 lg:[grid-auto-rows:minmax(11.5rem,auto)]">
        <Tile className="pb-0 lg:col-span-4 lg:row-span-2">
          <Copy
            title="Native at both ends"
            body="A SwiftUI menu-bar app and a Jetpack Compose app, each written for its own platform. No Electron, no web view wrapped in a window."
          />
          {/* Both apps, overlapping, running off the bottom — the pair is the product. */}
          {/* The hero's own devices, reused. `mt-auto` with a fixed height is what makes the
              crop reliable: the row band is sized by the taller column beside this one, so any
              slack has to land above the devices. Letting the scene stretch instead left it
              taller than the devices and nothing got cut at all. */}
          <div className="pointer-events-none mt-auto h-[15rem] overflow-hidden sm:h-[17rem]">
            <div className="stage stage-tile">
              <div className="stage-inner">
                <MacBook />
                <Phone />
              </div>
            </div>
          </div>
        </Tile>

        <Tile className="lg:col-span-2">
          <Copy
            title="Encrypted, and pinned"
            body="TLS between the two, with the phone's certificate pinned on the Mac."
          />
          <BleedBottom>
            <PinnedCrop />
          </BleedBottom>
        </Tile>

        <Tile className="lg:col-span-2">
          <Copy
            title="Easy on the battery"
            body="No polling and no cloud push. The phone wakes only when a message arrives."
          />
          <BleedBottom>
            <OngoingCrop />
          </BleedBottom>
        </Tile>

        <Tile className="lg:col-span-3 lg:row-span-2">
          <Copy
            title="Never leaves your Wi-Fi"
            body="The Mac connects straight to your phone across the local network. Nothing is relayed through a server in the middle, so there is no account to create and no inbox of yours on someone else's disk."
          />
          <div className="pointer-events-none mt-6 flex flex-1 items-center justify-center">
            <LocalNetworkCrop />
          </div>
        </Tile>

        <Tile className="lg:col-span-3 lg:row-span-2">
          <Copy
            title="Notified the moment it lands"
            body="A banner for every new message, grouped by conversation. Click one and that thread opens in the menu bar."
          />
          <div className="pointer-events-none relative mt-6 min-h-[15rem] flex-1">
            <div className="absolute inset-0">
              <div className="stage stage-notify">
                <div className="stage-inner">
                  <MacBook />
                </div>
              </div>
            </div>
            {/* Inset from the right the way a real banner is, and narrower than the tile. */}
            <div className="absolute right-4 top-6 w-[70%]">
              <NotificationCrop />
            </div>
          </div>
        </Tile>

      </div>
    </section>
  )
}

function Tile({ className = '', children }: { className?: string; children: React.ReactNode }) {
  return (
    <div className={`relative flex flex-col overflow-hidden rounded-2xl bg-surface p-5 ${className}`}>
      {children}
    </div>
  )
}

function BleedBottom({ children }: { children: React.ReactNode }) {
  return <div className="pointer-events-none mt-5">{children}</div>
}

function Copy({ title, body }: { title: string; body: string }) {
  return (
    <div>
      <h3 className="text-sm font-semibold">{title}</h3>
      <p className="mt-2 text-sm leading-relaxed text-muted">{body}</p>
    </div>
  )
}
