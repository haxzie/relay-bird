'use client'

import { usePathname } from 'next/navigation'
import { useEffect } from 'react'

import { POSTHOG_HOST, POSTHOG_TOKEN } from '@/lib/analytics'

/**
 * PostHog. Google Analytics is mounted separately in the layout via @next/third-parties.
 *
 * Loads only when the token is configured, so a local run or a fork sends nothing. Started
 * with `person_profiles: 'identified_only'` — this site has no accounts, so a person profile
 * per anonymous visitor would spend the quota on records nobody will ever look up.
 *
 * The instance is put on `window` once ready. Importing the module in two places does give
 * the same object, but nothing else can see it: not the console, and not a check for
 * whether analytics is actually live. One shared handle is easier to reason about and to
 * verify than module identity.
 */
export function PostHogAnalytics() {
  const pathname = usePathname()

  useEffect(() => {
    if (!POSTHOG_TOKEN) return
    let cancelled = false

    import('posthog-js').then(({ default: posthog }) => {
      if (cancelled) return
      if (!window.posthog) {
        posthog.init(POSTHOG_TOKEN, {
          api_host: POSTHOG_HOST,
          person_profiles: 'identified_only',
          // The App Router does not reload between routes, so PostHog's own listener would
          // miss every navigation after the first.
          capture_pageview: false,
          // Session replay is on by default and records what visitors do on the page. For a
          // product whose entire claim is that nothing of yours is recorded anywhere, that
          // is not a default to inherit silently. Counts and events, not recordings.
          disable_session_recording: true,
        })
        window.posthog = posthog
      }
      window.posthog?.capture('$pageview', { $current_url: window.location.href })
    })

    return () => {
      cancelled = true
    }
  }, [pathname])

  return null
}
