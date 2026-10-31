import { ASSETS } from '@/lib/releases'

/**
 * The Sparkle feed, served from this origin as well as the bucket.
 *
 * The app points at the bucket directly, which is one less thing to be down; this exists so
 * the feed can be moved behind the site later without shipping a new build to do it.
 */
export async function GET() {
  const upstream = await fetch(`${ASSETS}/appcast.xml`, { next: { revalidate: 60 } })
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
