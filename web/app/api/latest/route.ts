import { ASSETS } from '@/lib/releases'

export const dynamic = 'force-dynamic'

/**
 * What the app and any script can ask: the current release as JSON.
 *
 * A proxy rather than a redirect, so callers see one stable origin even if the bucket or its
 * domain changes underneath.
 */
export async function GET() {
  let upstream: Response
  try {
    upstream = await fetch(`${ASSETS}/latest.json`, { cache: 'no-store' })
  } catch {
    // An unreachable bucket *throws* rather than answering, so without this the route 500s
    // with an opaque "internal error" — which says nothing about what is actually wrong.
    return Response.json({ error: 'the release index is unreachable' }, { status: 503 })
  }
  if (!upstream.ok) {
    return Response.json({ error: 'no release published yet' }, { status: 503 })
  }
  return new Response(await upstream.text(), {
    headers: {
      'content-type': 'application/json',
      // The Worker renders per request; this is what stops it being asked per request.
      'cache-control': 'public, max-age=60',
    },
  })
}
