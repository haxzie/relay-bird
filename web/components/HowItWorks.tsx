import { AndroidGlyph, LaptopGlyph, MacGlyph, PhoneGlyph } from './Glyphs'
import { Mark } from './Mark'
import { QR_BITS, QR_SIZE } from './qr-code'

/**
 * The four steps, each with a looping illustration above it.
 *
 * The animation is pure CSS: no JavaScript ships for this, so it costs nothing on a page that
 * is otherwise entirely server-rendered, and it survives with the illustration simply sitting
 * still when someone has asked for reduced motion (see the media query in globals.css).
 */

const STEPS = [
  {
    n: '01',
    title: 'Get RelayBird for your Mac',
    body: 'A signed, notarised app that lives in the menu bar. No Dock icon, no window.',
    art: <MacDownload />,
  },
  {
    n: '02',
    title: 'Get RelayBird for your Android',
    body: 'The phone half holds your messages and never sends them anywhere but your Mac.',
    art: <AndroidDownload />,
  },
  {
    n: '03',
    title: 'Scan the QR from your phone',
    body: 'The Mac shows a code, the phone scans it once, and the two are paired for good.',
    art: <ScanPairing />,
  },
  {
    n: '04',
    title: 'Read, and get notified',
    body: 'Every conversation in the menu bar, and a banner the moment a new message lands.',
    art: <Inbox />,
  },
]

export function HowItWorks() {
  return (
    <section className="mt-28">
      <p className="text-xs uppercase tracking-[0.18em] text-muted">How it works</p>
      <h2 className="mt-3 text-3xl font-normal tracking-tight sm:text-4xl">
        Paired in under a minute
        <span className="block text-muted">and silent from then on</span>
      </h2>

      {/* One card, hairline-divided. The gap is the divider: a 1px grid gap over the edge
          colour, with each cell painting the page background back over it. */}
      <div className="mt-10 grid gap-px overflow-hidden rounded-2xl border border-edge bg-edge sm:grid-cols-2 lg:grid-cols-4">
        {STEPS.map((step) => (
          <div key={step.n} className="bg-ink p-5">
            <div className="art">
              <div className="art-inner">{step.art}</div>
            </div>
            <p className="mt-5 text-xs text-muted">{step.n}</p>
            <h3 className="mt-1 text-sm font-semibold">{step.title}</h3>
            <p className="mt-2 text-sm leading-relaxed text-muted">{step.body}</p>
          </div>
        ))}
      </div>
    </section>
  )
}

/** A download dropping into a MacBook, with the progress bar filling to match. */
function MacDownload() {
  return (
    <>
      <span className="drop">
        <MacGlyph className="drop-glyph" />
      </span>
      <span className="dev-mac">
        <span className="dev-mac-screen">
          <span className="bar">
            <i />
          </span>
        </span>
        <span className="dev-mac-base" />
      </span>
    </>
  )
}

function AndroidDownload() {
  return (
    <>
      <span className="drop">
        <AndroidGlyph className="drop-glyph" />
      </span>
      <span className="dev-phone">
        <span className="dev-phone-screen">
          <span className="bar">
            <i />
          </span>
        </span>
      </span>
    </>
  )
}

/**
 * Drawn as SVG on integer module coordinates. As a CSS grid of `1fr` columns the modules
 * landed on fractional pixels, and the blurred edges meant the code would not decode — it
 * only looked like a QR. Horizontal runs are merged into single rects to keep the node
 * count down.
 */
const QUIET = 4

function QrCode() {
  const rects: Array<{ x: number; y: number; w: number }> = []
  for (let r = 0; r < QR_SIZE; r++) {
    let run = 0
    for (let c = 0; c <= QR_SIZE; c++) {
      const on = c < QR_SIZE && QR_BITS[r * QR_SIZE + c] === '1'
      if (on) {
        run++
      } else if (run) {
        rects.push({ x: c - run + QUIET, y: r + QUIET, w: run })
        run = 0
      }
    }
  }

  const span = QR_SIZE + QUIET * 2
  return (
    <svg className="qr" viewBox={`0 0 ${span} ${span}`} shapeRendering="crispEdges">
      <rect width={span} height={span} fill="#fff" rx="1.5" />
      {rects.map((m) => (
        <rect key={`${m.x}-${m.y}`} x={m.x} y={m.y} width={m.w} height="1" fill="#0a0a0b" />
      ))}
    </svg>
  )
}

/** A viewfinder over the Mac's code, with the scan line sweeping it. */
function ScanPairing() {
  return (
    <>
      <QrCode />
      <span className="scanline" />
      <span className="bracket bracket-tl" />
      <span className="bracket bracket-tr" />
      <span className="bracket bracket-bl" />
      <span className="bracket bracket-br" />
      <span className="paired">
        <PhoneGlyph className="paired-glyph" />
        <i className="paired-link" />
        <LaptopGlyph className="paired-glyph" />
      </span>
    </>
  )
}

/** Threads sitting in the panel, with a banner arriving over them. */
function Inbox() {
  return (
    <>
      <span className="notif">
        <Mark className="notif-mark" />
        <span className="notif-lines">
          <i />
          <i />
        </span>
      </span>
      <span className="threads">
        {[0, 1, 2].map((i) => (
          <span className="thread" key={i}>
            <i className="thread-dot" style={{ animationDelay: `${i * 0.25}s` }} />
            <i className="thread-line" />
          </span>
        ))}
      </span>
    </>
  )
}
