import { ASSETS } from '@/lib/releases'

export const dynamic = 'force-dynamic'

/**
 * The Sparkle feed, served from this origin as well as the bucket.
 *
 * The app points at the bucket directly, which is one less thing to be down; this exists so
 * the feed can be moved behind the site later without shipping a new build to do it.
 */
export async function GET() {
  let upstream: Response
  try {
    upstream = await fetch(`${ASSETS}/appcast.xml`, { cache: 'no-store' })
  } catch {
    return new Response('the appcast is unreachable', { status: 503 })
  }
  if (!upstream.ok) {
    return new Response('no appcast published yet', { status: 503 })
  }
  return new Response(await upstream.text(), {
    headers: {
      'content-type': 'application/xml',
      'cache-control': 'public, max-age=60',
    },
  })
}
