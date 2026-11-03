import Link from 'next/link'

import { Mark } from './Mark'
import { AUTHOR_URL } from '@/lib/site'

export function SiteFooter() {
  return (
    <>
      <footer className="mt-24 flex flex-wrap items-center gap-x-6 gap-y-2 border-t border-edge py-8 text-xs text-muted">
        <span>Open source, and only ever on your Wi-Fi.</span>
        {/* Grouped so the two never split across a wrap — at narrow widths the footer breaks
            into two lines, and a lone "Terms" under the tagline reads like a heading. */}
        <span className="flex items-center gap-x-6">
          <Link className="transition hover:text-white" href="/privacy">
            Privacy
          </Link>
          <Link className="transition hover:text-white" href="/terms">
            Terms
          </Link>
        </span>
        <a
          className="ml-auto transition hover:text-white"
          href={AUTHOR_URL}
          target="_blank"
          rel="noreferrer"
        >
          Made by Haxzie
        </a>
      </footer>

      {/* The wordmark, with both the feather and the type cut off by the bottom of the page.
          Decorative, so hidden from assistive tech — the same name is already in the nav.

          `items-baseline` puts the feather's bottom edge on the text baseline, so sizing it
          to the cap height makes it span exactly the capitals' band. Both then sit in the
          same vertical range and the wrapper's crop takes the same slice off each. */}
      <div className="-mx-6 h-[0.62em] overflow-hidden text-[15vw]" aria-hidden>
        <div className="flex items-baseline justify-center gap-[0.05em] font-semibold leading-[0.78] tracking-tighter text-white/[0.12]">
          <Mark tight className="h-[0.7em] w-[0.7em]" />
          <span>RelayBird</span>
        </div>
      </div>
    </>
  )
}
