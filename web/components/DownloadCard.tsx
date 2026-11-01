import { AppleGlyph } from './Glyphs'
import { TrackedLink } from './TrackedLink'
import { MacBook } from './HeroMock'
import { latestRelease, megabytes } from '@/lib/releases'
import { REPO } from '@/lib/site'

const POINTS = [
  'Over your own Wi-Fi, never a server',
  'A banner the moment a message lands',
  'No account, nothing to sign up for',
]

/**
 * The download callout that rides alongside a post.
 *
 * Fetches the release itself rather than taking it as a prop, so a page can drop it in
 * anywhere without threading data through. The hero's Mac is reused for the thumbnail — at
 * this size the panel text is illegible, which is exactly how a product screenshot reads in
 * a card this wide.
 */
export async function DownloadCard() {
  const release = await latestRelease()

  return (
    <div className="overflow-hidden rounded-2xl border border-edge bg-surface">
      <div className="h-[8.5rem] overflow-hidden">
        <div className="stage stage-card">
          <div className="stage-inner">
            <MacBook />
          </div>
        </div>
      </div>

      <div className="p-5">
        <p className="text-base font-semibold leading-snug">
          Your phone&rsquo;s messages, in the Mac menu bar
        </p>

        <ul className="mt-4 space-y-3">
          {POINTS.map((point) => (
            <li key={point} className="flex gap-2.5 text-sm leading-snug text-muted">
              <TickGlyph className="mt-0.5 h-4 w-4 flex-none text-live" />
              {point}
            </li>
          ))}
        </ul>

        <TrackedLink
          href={release?.mac.url ?? `${REPO}/releases`}
          event="download_clicked"
          properties={{ platform: 'mac', placement: 'article_card' }}
          className="mt-5 flex items-center justify-center gap-2.5 rounded-full bg-[#0A84FF] px-5 py-3 text-sm font-semibold text-white transition hover:bg-[#0A84FF]/90"
        >
          <AppleGlyph className="h-4 w-auto" />
          Download for Mac
        </TrackedLink>

        <p className="mt-3 text-center text-xs text-muted">
          {release ? `Free and open source · ${megabytes(release.mac.size)}` : 'Free and open source'}
        </p>
      </div>
    </div>
  )
}

function TickGlyph({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 16 16" fill="none" className={className} aria-hidden>
      <path
        d="m3 8.6 3.3 3.3L13 5.2"
        stroke="currentColor"
        strokeWidth="1.9"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  )
}
