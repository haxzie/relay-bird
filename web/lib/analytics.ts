/**
 * Analytics configuration, in one place so every consumer agrees on whether it is on.
 *
 * Everything no-ops when a key is unset, so a fork or a local run sends nothing anywhere
 * without needing to know this exists.
 *
 * The PostHog project token is not a secret: it is write-only and is served to every browser
 * that loads the site, which is why it sits in `.env` rather than in a secret store. The GA
 * measurement ID is the same kind of value.
 */
export const GA_ID = process.env.NEXT_PUBLIC_GA_ID ?? ''
export const POSTHOG_TOKEN = process.env.NEXT_PUBLIC_POSTHOG_PROJECT_TOKEN ?? ''
export const POSTHOG_HOST = process.env.NEXT_PUBLIC_POSTHOG_HOST ?? 'https://us.i.posthog.com'

export const analyticsEnabled = Boolean(GA_ID || POSTHOG_TOKEN)
