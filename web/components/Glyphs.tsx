/**
 * The menu-bar status glyphs, drawn rather than pulled from an icon font so they inherit
 * `currentColor` and scale with the em grid like everything else in the mock.
 *
 * Note there is no Apple logo at the left of the menu bar, where macOS puts one. Reproducing
 * it on a page marketing our own product is exactly what Apple's trademark guidelines
 * prohibit, so the bar starts at the active app's name instead.
 */

export function WifiGlyph({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 16 16" fill="none" className={className} aria-hidden>
      <path d="M1.2 5.4a10.5 10.5 0 0 1 13.6 0" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
      <path d="M3.6 8.2a7 7 0 0 1 8.8 0" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
      <path d="M6 11a3.3 3.3 0 0 1 4 0" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
      <circle cx="8" cy="13.4" r="1.05" fill="currentColor" />
    </svg>
  )
}

export function BatteryGlyph({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 26 14" fill="none" className={className} aria-hidden>
      <rect x="0.7" y="0.7" width="21" height="12.6" rx="3.4" stroke="currentColor" strokeOpacity="0.5" strokeWidth="1.3" />
      <rect x="2.6" y="2.6" width="15" height="8.8" rx="2" fill="currentColor" />
      <path d="M23.6 5.1v3.8a2.1 2.1 0 0 0 0-3.8Z" fill="currentColor" fillOpacity="0.5" />
    </svg>
  )
}

/** The two-slider Control Center tile. */
export function ControlCentreGlyph({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 16 16" fill="none" className={className} aria-hidden>
      <rect x="1" y="2.3" width="14" height="4.2" rx="2.1" stroke="currentColor" strokeWidth="1.3" />
      <circle cx="11.2" cy="4.4" r="1.15" fill="currentColor" />
      <rect x="1" y="9.5" width="14" height="4.2" rx="2.1" stroke="currentColor" strokeWidth="1.3" />
      <circle cx="4.8" cy="11.6" r="1.15" fill="currentColor" />
    </svg>
  )
}

export function SearchGlyph({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 16 16" fill="none" className={className} aria-hidden>
      <circle cx="7" cy="7" r="4.6" stroke="currentColor" strokeWidth="1.5" />
      <path d="m10.6 10.6 3.4 3.4" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
    </svg>
  )
}

/** The double tick the panel header uses for mark-all-as-read. */
export function ChecksGlyph({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 20 12" fill="none" className={className} aria-hidden>
      <path d="m1 6.6 3.1 3.2L10.8 2.4" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" />
      <path d="m8.6 9.8 6.7-7.4" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}

/**
 * The macOS download mark. A laptop rather than the Apple logo, for the trademark reason
 * above — say the word and it can be swapped.
 */
export function MacGlyph({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 20 16" fill="none" className={className} aria-hidden>
      <rect x="2.6" y="1.2" width="14.8" height="10" rx="1.5" stroke="currentColor" strokeWidth="1.5" />
      <path d="M0.8 13.4h18.4a1.4 1.4 0 0 1-1.4 1.4H2.2a1.4 1.4 0 0 1-1.4-1.4Z" fill="currentColor" />
    </svg>
  )
}

/**
 * The Android robot head, from Google's mark, which they publish for reuse under CC BY.
 *
 * The viewBox is cropped to the head rather than padded out to a square, so a given height
 * draws a head of that height. Its aspect is ~1.75:1, so size it with `w-auto`.
 */
export function AndroidGlyph({ className }: { className?: string }) {
  return (
    <svg viewBox="1 5.2 22 12.9" fill="currentColor" className={className} aria-hidden>
      <path d="M17.6 9.48l1.84-3.18a.38.38 0 0 0-.14-.52.38.38 0 0 0-.52.14l-1.86 3.22a11.43 11.43 0 0 0-8.84 0L6.22 5.92a.38.38 0 0 0-.52-.14.38.38 0 0 0-.14.52L7.4 9.48A10.78 10.78 0 0 0 2 18h20a10.78 10.78 0 0 0-4.4-8.52ZM7.8 14.87a1.1 1.1 0 1 1 0-2.2 1.1 1.1 0 0 1 0 2.2Zm8.4 0a1.1 1.1 0 1 1 0-2.2 1.1 1.1 0 0 1 0 2.2Z" />
    </svg>
  )
}

/** Cellular bars, for the Android status bar. */
export function SignalGlyph({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 16 12" fill="currentColor" className={className} aria-hidden>
      <rect x="0" y="8.4" width="2.6" height="3.6" rx="0.6" />
      <rect x="4.4" y="6" width="2.6" height="6" rx="0.6" />
      <rect x="8.8" y="3.2" width="2.6" height="8.8" rx="0.6" />
      <rect x="13.2" y="0" width="2.6" height="12" rx="0.6" />
    </svg>
  )
}

/** The two endpoints the app's pairing illustration draws. */
export function PhoneGlyph({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 16 16" fill="none" className={className} aria-hidden>
      <rect x="4" y="1" width="8" height="14" rx="1.6" stroke="currentColor" strokeWidth="1.3" />
      <path d="M6.9 12.6h2.2" stroke="currentColor" strokeWidth="1.3" strokeLinecap="round" />
    </svg>
  )
}

export function LaptopGlyph({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 18 16" fill="none" className={className} aria-hidden>
      <rect x="3" y="2.5" width="12" height="8.5" rx="1.3" stroke="currentColor" strokeWidth="1.3" />
      <path d="M1.2 13.2h15.6" stroke="currentColor" strokeWidth="1.3" strokeLinecap="round" />
    </svg>
  )
}

export function LockGlyph({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 16 16" fill="none" className={className} aria-hidden>
      <rect x="2.8" y="6.8" width="10.4" height="7.6" rx="2" stroke="currentColor" strokeWidth="1.4" />
      <path d="M5.4 6.8V5a2.6 2.6 0 0 1 5.2 0v1.8" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round" />
      <circle cx="8" cy="10.6" r="1.15" fill="currentColor" />
    </svg>
  )
}

/** A cloud with a line through it: the thing this product deliberately does not use. */
export function CloudOffGlyph({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 18 16" fill="none" className={className} aria-hidden>
      <path
        d="M5 12.4h7.3a3 3 0 0 0 .5-6 4.4 4.4 0 0 0-8-1.5A3.4 3.4 0 0 0 5 12.4Z"
        stroke="currentColor"
        strokeWidth="1.4"
        strokeLinejoin="round"
      />
      <path d="M2.2 2.2 15.8 14.2" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round" />
    </svg>
  )
}

export function BellGlyph({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 16 16" fill="none" className={className} aria-hidden>
      <path
        d="M8 2.2a4.3 4.3 0 0 0-4.3 4.3c0 3-1.1 4-1.1 4h10.8s-1.1-1-1.1-4A4.3 4.3 0 0 0 8 2.2Z"
        stroke="currentColor"
        strokeWidth="1.4"
        strokeLinejoin="round"
      />
      <path d="M6.6 13a1.6 1.6 0 0 0 2.8 0" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round" />
    </svg>
  )
}

/** One-way: the arrow only points at the Mac. */
export function OneWayGlyph({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 18 16" fill="none" className={className} aria-hidden>
      <path d="M2.4 8h12" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round" />
      <path d="m10.6 4.2 3.8 3.8-3.8 3.8" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}

export function BoltGlyph({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 16 16" fill="currentColor" className={className} aria-hidden>
      <path d="M9.4 1.4 3.6 9.1h3.3l-.6 5.6 5.9-7.8H8.9l.5-5.5Z" />
    </svg>
  )
}

/**
 * The Apple logo. Added on request — noting once that Apple's trademark guidelines ask that
 * it not be used in marketing for a product that is not theirs; the word "Mac" alone is the
 * safe form. Easy to swap back for [[MacGlyph]].
 */
export function AppleGlyph({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 814 1000" fill="currentColor" className={className} aria-hidden>
      <path d="M788.1 340.9c-5.8 4.5-108.2 62.2-108.2 190.5 0 148.4 130.3 200.9 134.2 202.2-.6 3.2-20.7 71.9-68.7 141.9-42.8 61.6-87.5 123.1-155.5 123.1s-85.5-39.5-164-39.5c-76.5 0-103.7 40.8-165.9 40.8s-105.6-57-155.5-127C46.7 790.7 0 663 0 541.8c0-194.4 126.4-297.5 250.8-297.5 66.1 0 121.2 43.4 162.7 43.4 39.5 0 101.1-46 176.3-46 28.5 0 130.9 2.6 198.3 99.2zm-234-181.5c31.1-36.9 53.1-88.1 53.1-139.3 0-7.1-.6-14.3-1.9-20.1-50.6 1.9-110.8 33.7-147.1 75.8-28.5 32.4-55.1 83.6-55.1 135.5 0 7.8 1.3 15.6 1.9 18.1 3.2.6 8.4 1.3 13.6 1.3 45.4 0 102.5-30.4 135.5-71.3z" />
    </svg>
  )
}

/** GitHub's mark, used to link to the repository. */
export function GitHubGlyph({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 16 16" fill="currentColor" className={className} aria-hidden>
      <path d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27s1.36.09 2 .27c1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.01 8.01 0 0 0 16 8c0-4.42-3.58-8-8-8Z" />
    </svg>
  )
}

