/**
 * Everything the site knows about what has shipped comes from R2, written there by the
 * release-mirror workflow. The site holds no copy of its own, so it cannot drift from what
 * is actually downloadable.
 */
export const ASSETS = 'https://assets.haxzie.com/relaybird'

export type Release = {
  version: string
  tag: string
  publishedAt: string
  platform: string
  filename: string
  url: string
  size: number
}

/** Matches the 60s cache-control the workflow writes, rather than guessing at a number. */
const REVALIDATE = 60

export async function latestRelease(): Promise<Release | null> {
  try {
    const response = await fetch(`${ASSETS}/latest.json`, { next: { revalidate: REVALIDATE } })
    if (!response.ok) return null
    return (await response.json()) as Release
  } catch {
    // The page still renders without it — a download link pointing nowhere is worse than
    // one that honestly says it is not ready.
    return null
  }
}

export function megabytes(bytes: number): string {
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}
