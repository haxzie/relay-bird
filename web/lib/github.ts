/**
 * The repository's star count, for the header.
 *
 * Cached in the isolate for ten minutes. The page renders per request, and GitHub allows an
 * unauthenticated client sixty calls an hour — on a Worker that quota is shared across every
 * visitor behind the same egress, so calling it on each render would spend it in minutes and
 * then start failing. Stars move slowly; ten minutes of staleness costs nothing.
 *
 * Isolate memory is not a real cache: it is dropped whenever the isolate recycles. That is
 * fine, because the only consequence of a miss is one extra API call, and the only
 * consequence of a failure is that the header shows no number.
 */
const REPO = 'haxzie/relay-bird'
const TTL_MS = 10 * 60 * 1000

let cached: { stars: number; at: number } | null = null

export async function repoStars(): Promise<number | null> {
  if (cached && Date.now() - cached.at < TTL_MS) return cached.stars

  try {
    const response = await fetch(`https://api.github.com/repos/${REPO}`, {
      // GitHub rejects requests without one.
      headers: { 'user-agent': 'relaybird-site', accept: 'application/vnd.github+json' },
      cache: 'no-store',
    })
    if (!response.ok) return cached?.stars ?? null
    const body = (await response.json()) as { stargazers_count?: number }
    if (typeof body.stargazers_count !== 'number') return cached?.stars ?? null

    cached = { stars: body.stargazers_count, at: Date.now() }
    return cached.stars
  } catch {
    // Serve the last known figure if there is one; otherwise the header just omits it.
    return cached?.stars ?? null
  }
}

export function formatStars(count: number): string {
  if (count < 1000) return String(count)
  return `${(count / 1000).toFixed(1).replace(/\.0$/, '')}k`
}
