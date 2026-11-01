import type { Metadata } from 'next'

import { AndroidGlyph, AppleGlyph } from '@/components/Glyphs'
import { MacBook, Phone } from '@/components/HeroMock'
import { Mark } from '@/components/Mark'
import { QR_BITS, QR_SIZE } from '@/components/qr-code'

/**
 * The source for the images used in blog posts.
 *
 * Kept as a page rather than a one-off script so the artwork stays the same components the
 * site renders — regenerating after a UI change is one screenshot run, not a redraw. Not
 * linked anywhere and excluded from indexing; `tools/shots.js` captures each `data-shot`.
 */
export const metadata: Metadata = { robots: { index: false, follow: false } }

const QUIET = 4

function PairingScreen() {
  const rects: Array<{ x: number; y: number; w: number }> = []
  for (let r = 0; r < QR_SIZE; r++) {
    let run = 0
    for (let c = 0; c <= QR_SIZE; c++) {
      const on = c < QR_SIZE && QR_BITS[r * QR_SIZE + c] === '1'
      if (on) run++
      else if (run) {
        rects.push({ x: c - run + QUIET, y: r + QUIET, w: run })
        run = 0
      }
    }
  }
  const span = QR_SIZE + QUIET * 2

  return (
    <div className="pair-window">
      <Mark className="pair-mark" />
      <p className="pair-title">Pair a phone</p>
      <p className="pair-sub">Scan this from the RelayBird app on your Android</p>
      <svg className="pair-qr" viewBox={`0 0 ${span} ${span}`} shapeRendering="crispEdges">
        <rect width={span} height={span} fill="#fff" rx="1.5" />
        {rects.map((m) => (
          <rect key={`${m.x}-${m.y}`} x={m.x} y={m.y} width={m.w} height="1" fill="#0a0a0b" />
        ))}
      </svg>
      <p className="pair-foot">Expires in 1:58</p>
    </div>
  )
}

export default function Shots() {
  return (
    <main className="space-y-20 py-20">
      {/* The social card, at exactly the 1200x630 every platform crops to. */}
      <div
        data-scene="og"
        className="flex h-[630px] w-[1200px] flex-col items-center justify-center bg-ink"
      >
        <h1 className="text-center text-[64px] font-normal leading-[1.12] tracking-tight">
          Your <AndroidGlyph className="inline-block h-[0.62em] w-auto align-baseline" />{' '}
          phone&rsquo;s messages
          <span className="block text-muted">
            on your <AppleGlyph className="inline-block h-[0.78em] w-auto align-[-0.06em]" />{' '}
            Mac
          </span>
        </h1>
        <div className="mt-14 flex items-center gap-4">
          <span className="flex items-center gap-2.5 rounded-full bg-[#0A84FF] px-7 py-3.5 text-lg font-semibold text-white">
            <AppleGlyph className="h-5 w-auto" />
            Download for Mac
          </span>
          <span className="flex items-center gap-2.5 rounded-full border border-white/30 px-7 py-3.5 text-lg font-semibold text-white">
            <AndroidGlyph className="h-5 w-auto text-[#3DDC84]" />
            Download for Android
          </span>
        </div>
      </div>

      {/* The app icon: a rounded black tile with the mark, squircle-ish corner radius. */}
      <div
        data-scene="icon"
        className="grid h-[512px] w-[512px] place-items-center rounded-[112px] bg-black"
      >
        <Mark tight className="h-[260px] w-[260px] text-white" />
      </div>

      <div className="w-[1100px] bg-ink p-6" data-scene="messages">
        <div className="stage">
          <div className="stage-inner">
            <MacBook />
            <Phone />
          </div>
        </div>
      </div>

      {/* The phone on its own, so the capture does not pick up the Mac's wallpaper behind it. */}
      <div className="w-[1100px] bg-ink p-6" data-scene="phone">
        <div className="stage">
          <div className="stage-inner">
            <Phone />
          </div>
        </div>
      </div>

      <div className="w-[1100px] bg-ink p-6" data-scene="pairing">
        <div className="stage">
          <div className="stage-inner">
            <MacBook screen={<PairingScreen />} />
          </div>
        </div>
      </div>
    </main>
  )
}
