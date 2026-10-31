import { ASSETS } from '@/lib/releases'

/**
 * What the app and any script can ask: the current release as JSON.
 *
 * A proxy rather than a redirect, so callers see one stable origin even if the bucket or its
 * domain changes underneath.
 */
export async function GET() {
  const upstream = await fetch(`${ASSETS}/latest.json`, { next: { revalidate: 60 } })
  if (!upstream.ok) {
    return Response.json({ error: 'no release published yet' }, { status: 503 })
  }
  return new Response(await upstream.text(), {
    headers: {
      'content-type': 'application/json',
      'cache-control': 'public, max-age=60',
    },
  })
}
