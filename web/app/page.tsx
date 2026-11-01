import { AndroidGlyph, AppleGlyph } from '@/components/Glyphs'
import { TrackedLink } from '@/components/TrackedLink'
import { Features } from '@/components/Features'
import { HeroMock } from '@/components/HeroMock'
import { HowItWorks } from '@/components/HowItWorks'
import { UseCases } from '@/components/UseCases'
import { latestRelease, type Release } from '@/lib/releases'

// Rendered per request. Prerendering this page would pin it to whichever version was current
// when CI last built the site, so it would keep offering an old DMG after every release until
// something happened to rebuild it.
export const dynamic = 'force-dynamic'

export default async function Home() {
  const release = await latestRelease()

  return (
    <main>
      <section className="pt-16 sm:pt-24">
        <h1 className="text-center text-4xl font-normal leading-[1.1] tracking-tight sm:text-6xl">
          Your{' '}
          <AndroidGlyph className="inline-block h-[0.62em] w-auto align-baseline" />{' '}
          phone&rsquo;s messages
          <span className="block text-muted">
            on your <AppleGlyph className="inline-block h-[0.78em] w-auto align-[-0.06em]" />{' '}
            Mac
          </span>
        </h1>

        <DownloadButtons release={release} className="mt-14 justify-center sm:mt-16" />

        {/* The picture does the explaining: one menu bar, one phone, one network. */}
        <div className="mt-12 overflow-hidden rounded-2xl border border-edge bg-gradient-to-b from-white/[0.055] to-transparent p-4 sm:mt-16 sm:p-10">
          <HeroMock />
        </div>
      </section>

      <UseCases />

      <HowItWorks />

      <Features />

      {/* The closing ask, after the case has been made. */}
      <section className="mt-28 rounded-3xl bg-surface px-6 py-16 text-center sm:px-16 sm:py-20">
        <h2 className="mx-auto max-w-2xl text-3xl font-normal leading-tight tracking-tight sm:text-5xl">
          Stop reaching for your phone
          <span className="block text-muted">to read one line of text</span>
        </h2>
        <p className="mx-auto mt-6 max-w-xl text-sm leading-relaxed text-muted">
          Every code, every delivery notice, every message worth a glance — on the Mac that is
          already in front of you, over the Wi-Fi you are already on.
        </p>
        <DownloadButtons release={release} className="mt-10 justify-center" />
      </section>
    </main>
  )
}

/**
 * One button to the download page, rather than a platform choice in the hero.
 *
 * The page it leads to can explain the install script, the ADB requirement and the Play
 * Protect block — none of which fits beside a button, and all of which someone needs before
 * the Android half will actually install.
 */
function DownloadButtons({
  release,
  className = '',
}: {
  release: Release | null
  className?: string
}) {
  return (
    <div className={`flex flex-col items-center gap-3 ${className}`}>
      <TrackedLink
        href="/download"
        event="download_cta_clicked"
        properties={{ version: release?.version ?? 'none' }}
        className="rounded-full bg-white px-7 py-3.5 text-sm font-semibold text-black transition hover:opacity-90"
      >
        Download RelayBird
      </TrackedLink>
      <p className="text-xs text-muted">
        {release ? `Version ${release.version} · ` : ''}Mac and Android
      </p>
    </div>
  )
}
