import { latestRelease, megabytes } from '@/lib/releases'

// Rendered per request. Prerendering this page would pin it to whichever version was current
// when CI last built the site, so it would keep offering an old DMG after every release until
// something happened to rebuild it.
export const dynamic = 'force-dynamic'

export default async function Home() {
  const release = await latestRelease()

  return (
    <main className="mx-auto flex min-h-screen max-w-2xl flex-col justify-center gap-10 px-6 py-20">
      <header className="space-y-4">
        <h1 className="text-4xl font-bold tracking-tight">RelayBird</h1>
        <p className="text-lg text-muted">
          Your Android messages, in the macOS menu bar. Over your own Wi-Fi — no account, no
          cloud, nothing leaving the network.
        </p>
      </header>

      <section className="rounded-2xl border border-edge bg-surface p-6">
        {release ? (
          <div className="flex flex-wrap items-center justify-between gap-4">
            <div>
              <p className="text-sm text-muted">Latest release</p>
              <p className="text-xl font-semibold">{release.version}</p>
              <p className="text-xs text-muted">
                {megabytes(release.size)} · {new Date(release.publishedAt).toLocaleDateString()}
              </p>
            </div>
            <a
              href={release.url}
              className="rounded-xl bg-white px-5 py-3 font-semibold text-black transition hover:opacity-90"
            >
              Download for macOS
            </a>
          </div>
        ) : (
          <p className="text-muted">No release published yet.</p>
        )}
        <p className="mt-4 text-xs text-muted">
          The phone half is an APK on the{' '}
          <a className="underline" href="https://github.com/haxzie/relay-bird/releases">
            releases page
          </a>
          . Install both — they are gated against each other by protocol version, so a phone and
          a Mac from different releases will not pair.
        </p>
      </section>

      <section className="grid gap-4 sm:grid-cols-3">
        {[
          ['Stays on your network', 'The phone serves over TLS on the LAN and the Mac connects to it. Nothing is relayed through anyone else.'],
          ['Pairs like WhatsApp Web', 'The Mac shows a QR, the phone scans it once, and every reconnection after that is silent.'],
          ['Reads, never sends', 'Mirroring only. The Mac cannot send messages from your number.'],
        ].map(([title, body]) => (
          <div key={title} className="rounded-xl border border-edge bg-surface p-4">
            <h2 className="text-sm font-semibold">{title}</h2>
            <p className="mt-1 text-xs text-muted">{body}</p>
          </div>
        ))}
      </section>

      <footer className="text-xs text-muted">
        <a className="underline" href="https://github.com/haxzie/relay-bird">Source</a>
        {' · '}
        <a className="underline" href="/api/latest">latest.json</a>
      </footer>
    </main>
  )
}
