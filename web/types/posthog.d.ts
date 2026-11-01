import type posthog from 'posthog-js'

declare global {
  interface Window {
    /** Set by PostHogAnalytics once init has run; absent when analytics is off. */
    posthog?: typeof posthog
  }
}

export {}
