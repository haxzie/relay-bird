import type { Metadata } from 'next'
import Link from 'next/link'

import { InstallCommand } from '@/components/InstallCommand'
import { latestRelease, megabytes, type Release } from '@/lib/releases'
import { REPO } from '@/lib/site'

export const metadata: Metadata = {
  title: 'Download RelayBird — Mac and Android',
  description:
    'Install RelayBird on your Mac and Android phone with one command. Read your phone’s text messages from the Mac menu bar, over your own Wi-Fi.',
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
          Install RelayBird
          <span className="block text-muted">one command, both halves</span>
        </h1>
        {release && (
          <p className="mt-6 inline-flex items-center gap-2 rounded-full border border-edge px-3 py-1 text-xs text-muted">
            <span className="h-1.5 w-1.5 rounded-full bg-live" />
            Version {release.version} · released {releaseDate(release.publishedAt)}
          </p>
        )}
      </div>

      <div className="mx-auto mt-12 max-w-2xl rounded-2xl bg-surface p-6 sm:p-8">
        <InstallCommand />
        <p className="mt-4 text-sm leading-relaxed text-muted">
          Run this on your Mac. It installs the menu-bar app, then installs the Android app
          onto your phone over a USB cable. Both halves come from the same release, which
          matters: they refuse to pair across versions.
        </p>

        <div className="mt-8 border-t border-edge pt-8">
          <p className="text-xs uppercase tracking-[0.14em] text-muted">Before you run it</p>

          <ol className="mt-6 space-y-7">
            <Step n={1} title="Install ADB on your Mac">
              <p>
                The Android half has to go on over the Android debug bridge. This is not a
                preference: Play Protect blocks any app that reads SMS when it is installed
                from a browser, so tapping the APK on the phone does not work.
              </p>
              <Code>brew install --cask android-platform-tools</Code>
              <p className="text-xs">
                No Homebrew?{' '}
                <a
                  className="underline underline-offset-4"
                  href="https://developer.android.com/tools/releases/platform-tools"
                  target="_blank"
                  rel="noreferrer"
                >
                  Download platform-tools from Google
                </a>{' '}
                and add it to your PATH.
              </p>
            </Step>

            <Step n={2} title="Turn on Developer options">
              <p>
                On the phone: <B>Settings</B> → <B>About phone</B> → tap <B>Build number</B>{' '}
                seven times. It will tell you when you are a developer.
              </p>
            </Step>

            <Step n={3} title="Turn on USB debugging">
              <p>
                <B>Settings</B> → <B>System</B> → <B>Developer options</B> → turn on{' '}
                <B>USB debugging</B>. On some phones Developer options sits directly in
                Settings rather than under System.
              </p>
            </Step>

            <Step n={4} title="Plug the phone into the Mac">
              <p>
                Use a cable that carries data — plenty of charging cables do not. The phone
                will ask whether to allow USB debugging from this computer; tap <B>Allow</B>,
                and tick <B>Always allow</B> so it stops asking.
              </p>
            </Step>
          </ol>
        </div>

        <div className="mt-8 flex flex-wrap gap-x-6 gap-y-1 border-t border-edge pt-6 text-xs text-muted">
          <span>macOS 14 or later</span>
          <span>Android 8.0 or later</span>
          <span>Both on the same Wi-Fi</span>
          {release && (
            <span className="ml-auto">
              {megabytes(release.mac.size)} + {megabytes(release.android.size)}
            </span>
          )}
        </div>
      </div>

      <div className="mx-auto mt-8 max-w-2xl space-y-2 text-center text-xs text-muted">
        <p>
          No cable to hand?{' '}
          <Link className="underline underline-offset-4" href="/blog/android-play-protect-blocked">
            The other ways to install, and why Android blocks this one
          </Link>
          .
        </p>
        <p>
          Prefer the files directly?{' '}
          <DirectLinks release={release} />
        </p>
      </div>
    </main>
  )
}

function Step({ n, title, children }: { n: number; title: string; children: React.ReactNode }) {
  return (
    <li className="flex gap-4">
      <span className="grid h-6 w-6 flex-none place-items-center rounded-full bg-raised text-xs font-semibold">
        {n}
      </span>
      <div className="min-w-0 flex-1">
        <p className="text-sm font-semibold">{title}</p>
        <div className="mt-2 space-y-3 text-sm leading-relaxed text-muted">{children}</div>
      </div>
    </li>
  )
}

function B({ children }: { children: React.ReactNode }) {
  return <strong className="font-semibold text-white">{children}</strong>
}

function Code({ children }: { children: React.ReactNode }) {
  return (
    <code className="block overflow-x-auto rounded-lg bg-raised px-3 py-2 font-mono text-[13px] text-white">
      {children}
    </code>
  )
}

/** The raw artefacts, for anyone who would rather not run a script. */
function DirectLinks({ release }: { release: Release | null }) {
  if (!release) {
    return (
      <a className="underline underline-offset-4" href={`${REPO}/releases`}>
        GitHub releases
      </a>
    )
  }
  return (
    <>
      <a className="underline underline-offset-4" href={release.mac.url}>
        the disk image
      </a>{' '}
      and{' '}
      <a className="underline underline-offset-4" href={release.android.url}>
        the APK
      </a>
    </>
  )
}
