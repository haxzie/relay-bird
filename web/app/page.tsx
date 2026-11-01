import { AndroidGlyph, AppleGlyph } from '@/components/Glyphs'
import { TrackedLink } from '@/components/TrackedLink'
import { Features } from '@/components/Features'
import { HeroMock } from '@/components/HeroMock'
import { HowItWorks } from '@/components/HowItWorks'
import { UseCases } from '@/components/UseCases'
import { latestRelease, megabytes, type Release } from '@/lib/releases'
import { REPO } from '@/lib/site'

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
 * Both downloads. The Mac button always goes somewhere: with nothing published yet it points
 * at the releases page rather than sitting there dead.
 */
function DownloadButtons({
  release,
  className = '',
}: {
  release: Release | null
  className?: string
}) {
  return (
    <div className={`flex flex-wrap items-center gap-3 ${className}`}>
      <TrackedLink
        href={release?.mac.url ?? `${REPO}/releases`}
        event="download_clicked"
        properties={{ platform: 'mac', version: release?.version ?? 'none', placement: 'cta' }}
        className="flex items-center gap-2.5 rounded-full bg-[#0A84FF] px-6 py-3 text-sm font-semibold text-white transition hover:bg-[#0A84FF]/90"
      >
        <AppleGlyph className="h-4 w-auto" />
        Download for Mac
        {release && <span className="font-normal text-white/60">{megabytes(release.mac.size)}</span>}
      </TrackedLink>
      <TrackedLink
        href={release?.android.url ?? `${REPO}/releases`}
        event="download_clicked"
        properties={{ platform: 'android', version: release?.version ?? 'none', placement: 'cta' }}
        className="flex items-center gap-2.5 rounded-full border border-white/30 px-6 py-3 text-sm font-semibold text-white transition hover:border-white/60 hover:bg-white/5"
      >
        {/* The robot in Android green, not the Play badge: the APK is not a Play listing,
            and Google's badge terms expect that badge to link to one. */}
        <AndroidGlyph className="h-4 w-auto text-[#3DDC84]" />
        Download for Android
      </TrackedLink>
    </div>
  )
}
