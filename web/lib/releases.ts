/**
 * Everything the site knows about what has shipped comes from R2, written there by the
 * release-mirror workflow. The site holds no copy of its own, so it cannot drift from what
 * is actually downloadable.
 */
export const ASSETS = 'https://assets.haxzie.com/relaybird'

export type Asset = {
  platform: string
  filename: string
  url: string
  size: number
}

export type Release = {
  version: string
  tag: string
  publishedAt: string
  mac: Asset
  android: Asset
}

export async function latestRelease(): Promise<Release | null> {
  try {
    // Uncached on purpose. Next's data cache needs OpenNext's incremental cache behind it,
    // which on Workers means an R2 bucket plus a Durable Object queue — a lot of moving parts
    // for one page. Rendering per request is always correct instead of correct-for-60s, and
    // the bucket serves this file with its own 60s cache-control, so Cloudflare's edge absorbs
    // the repeat traffic anyway.
    const response = await fetch(`${ASSETS}/latest.json`, { cache: 'no-store' })
    if (!response.ok) return null

    const body = (await response.json()) as Partial<Release>
    // A feed missing either half is not a release this site can describe, and rendering a
    // download button over `undefined` would produce a link to nowhere.
    if (!body.version || !body.mac?.url || !body.android?.url) return null
    return body as Release
  } catch {
    // The page still renders without it — a download link pointing nowhere is worse than
    // one that honestly says it is not ready.
    return null
  }
}

export function megabytes(bytes: number): string {
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}
