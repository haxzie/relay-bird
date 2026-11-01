import Link from 'next/link'

import { GitHubGlyph } from './Glyphs'
import { Mark } from './Mark'
import { formatStars, repoStars } from '@/lib/github'
import { latestRelease } from '@/lib/releases'
import { REPO } from '@/lib/site'

/**
 * Shared across every page, so the version pill and star count are fetched once here rather
 * than being threaded through each route.
 */
export async function SiteNav() {
  const [release, stars] = await Promise.all([latestRelease(), repoStars()])

  return (
    <nav className="flex items-center gap-3 py-6">
      {/* Grouped so the mark sits close to the name without tightening the nav's other gaps.
          The mark's artwork carries ~10% padding inside its box, so the optical gap runs a
          couple of pixels wider than the value set here. */}
      <Link href="/" className="flex items-center gap-1.5">
        <Mark className="h-5 w-5" />
        <span className="font-semibold">RelayBird</span>
      </Link>
      {release && (
        <span className="rounded-full border border-edge px-2 py-0.5 text-[11px] text-muted">
          {release.version}
        </span>
      )}

      <span className="ml-4 flex items-center gap-5 text-sm">
        <Link className="text-muted transition hover:text-white" href="/blog">
          Blog
        </Link>
        <Link className="text-muted transition hover:text-white" href="/about">
          About
        </Link>
      </span>

      <span className="ml-auto flex items-center gap-3 text-sm">
        <a
          href={REPO}
          className="flex items-center gap-2 rounded-full border border-edge px-3 py-1.5 text-muted transition hover:border-white/40 hover:text-white"
        >
          <GitHubGlyph className="h-4 w-4" />
          GitHub
          {/* Omitted rather than shown as a dash when GitHub cannot be reached. */}
          {stars !== null && (
            <span className="flex items-center gap-1 border-l border-edge pl-2 text-xs">
              <StarGlyph className="h-3 w-3" />
              {formatStars(stars)}
            </span>
          )}
        </a>

        <Link
          href="/download"
          className="rounded-full bg-white px-4 py-1.5 font-semibold text-black transition hover:opacity-90"
        >
          Download
        </Link>
      </span>
    </nav>
  )
}

function StarGlyph({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 16 16" fill="currentColor" className={className} aria-hidden>
      <path d="m8 1.4 2.06 4.18 4.61.67-3.33 3.25.78 4.59L8 11.92l-4.12 2.17.78-4.59L1.33 6.25l4.61-.67L8 1.4Z" />
    </svg>
  )
}
