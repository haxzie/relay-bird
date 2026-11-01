'use client'

/**
 * One call that reaches both destinations, so a new event does not have to remember two
 * APIs. Safe to call when neither is configured.
 */
export function track(event: string, properties: Record<string, unknown> = {}) {
  if (typeof window === 'undefined') return

  // `sendGAEvent` is @next/third-parties' own entry point; calling gtag directly would
  // depend on a load order the component does not promise.
  import('@next/third-parties/google')
    .then(({ sendGAEvent }) => sendGAEvent('event', event, properties))
    .catch(() => {})

  window.posthog?.capture(event, properties)
}
