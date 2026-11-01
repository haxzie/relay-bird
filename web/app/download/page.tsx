import type { Metadata } from 'next'
import Link from 'next/link'

import { AndroidGlyph, AppleGlyph } from '@/components/Glyphs'
import { InstallCommand } from '@/components/InstallCommand'
import { TrackedLink } from '@/components/TrackedLink'
import { latestRelease, megabytes, type Release } from '@/lib/releases'
import { REPO } from '@/lib/site'

export const metadata: Metadata = {
  title: 'Download RelayBird — Mac and Android',
  description:
    'Download RelayBird for macOS and Android. Read your phone’s text messages from the Mac menu bar, over your own Wi-Fi.',
  alternates: { canonical: '/download' },
}

// The release comes from R2 at request time, so the version shown is whatever is actually
// downloadable rather than whatever was true when the site was last built.
export const dynamic = 'force-dynamic'

function releaseDate(iso: string): string {
  return new Date(iso).toLocaleDateString('en-GB', {
    day: 'numeric',
    month: 'long',
    year: 'numeric',
  })
}

export default async function Download() {
  const release = await latestRelease()

  return (
    <main className="pt-16 sm:pt-24">
      <div className="text-center">
        <h1 className="text-4xl font-normal tracking-tight sm:text-5xl">
          Download RelayBird
          <span className="block text-muted">both halves, one version</span>
        </h1>
        <p className="mx-auto mt-6 max-w-xl text-sm leading-relaxed text-muted">
          Install both. They are gated against each other by protocol version, so a phone and
          a Mac from different releases will refuse to pair rather than misbehave.
        </p>
        {release && (
          <p className="mt-6 inline-flex items-center gap-2 rounded-full border border-edge px-3 py-1 text-xs text-muted">
            <span className="h-1.5 w-1.5 rounded-full bg-live" />
            Version {release.version} · released {releaseDate(release.publishedAt)}
          </p>
        )}
      </div>

      <div className="mt-12">
        <InstallCommand />
      </div>

      <p className="mt-14 text-center text-xs uppercase tracking-[0.14em] text-muted">
        or install each half yourself
      </p>

      <div className="mt-6 grid gap-4 sm:grid-cols-2">
        <Platform
          name="macOS"
          glyph={<AppleGlyph className="h-6 w-auto" />}
          requirement="macOS 14 Sonoma or later · Apple silicon and Intel"
          detail={
            release
              ? `${release.mac.filename} · ${megabytes(release.mac.size)}`
              : 'No release published yet'
          }
          href={release?.mac.url ?? `${REPO}/releases`}
          cta="Download for Mac"
          tone="mac"
          notes={[
            'Signed and notarised, so it opens without a Gatekeeper warning',
            'Lives in the menu bar — no Dock icon, no window',
            'Updates itself from here on',
          ]}
        />

        <Platform
          name="Android"
          glyph={<AndroidGlyph className="h-6 w-auto text-[#3DDC84]" />}
          requirement="Android 8.0 Oreo or later"
          detail={
            release
              ? `${release.android.filename} · ${megabytes(release.android.size)}`
              : 'No release published yet'
          }
          href={release?.android.url ?? `${REPO}/releases`}
          cta="Download for Android"
          tone="android"
          notes={[
            'Not on Google Play: Play restricts SMS permissions to default SMS apps',
            'Play Protect blocks SMS apps installed from a browser — install over ADB',
            'The app tells you when a new version is out',
          ]}
        />
      </div>

      <p className="mt-6 text-center text-xs text-muted">
        Android blocks sideloaded apps that read SMS.{' '}
        <Link className="underline underline-offset-4" href="''' + GUIDE + '''">
          Here is why, and how to install anyway
        </Link>
        .
      </p>

      <p className="mt-3 text-center text-xs text-muted">
        Both devices must be on the same Wi-Fi network.{' '}
        <Link className="underline underline-offset-4" href="/blog/how-to-view-android-sms-on-mac">
          Setup takes about five minutes
        </Link>
        .
      </p>
    </main>
  )
}

function Platform({
  name,
  glyph,
  requirement,
  detail,
  href,
  cta,
  notes,
  tone,
}: {
  name: string
  glyph: React.ReactNode
  requirement: string
  detail: string
  href: string
  cta: string
  notes: string[]
  tone: 'mac' | 'android'
}) {
  // Each platform's own colour. Both take black or white text at a comfortable contrast,
  // so the label is set per tone rather than assumed.
  const button =
    tone === 'mac'
      ? 'bg-[#0A84FF] text-white hover:bg-[#0A84FF]/90'
      : 'bg-[#3DDC84] text-black hover:bg-[#3DDC84]/90'
  return (
    <div className="flex flex-col rounded-2xl bg-surface p-7">
      <div className="flex items-center gap-3">
        {glyph}
        <span className="text-lg font-semibold">{name}</span>
      </div>
      <p className="mt-2 text-sm text-muted">{requirement}</p>

      <ul className="mt-6 space-y-2.5">
        {notes.map((note) => (
          <li key={note} className="flex gap-2.5 text-sm leading-snug text-muted">
            <span className="mt-1.5 h-1 w-1 flex-none rounded-full bg-muted/60" />
            {note}
          </li>
        ))}
      </ul>

      {/* Pushed to the bottom so the two cards' buttons line up whatever the copy length. */}
      <div className="mt-auto pt-7">
        <TrackedLink
          href={href}
          event="download_clicked"
          properties={{ platform: tone, placement: 'download_page' }}
          className={`flex items-center justify-center gap-2.5 rounded-full px-6 py-3 text-sm font-semibold transition ${button}`}
        >
          {cta}
        </TrackedLink>
        <p className="mt-3 text-center text-xs text-muted">{detail}</p>
      </div>
    </div>
  )
}
