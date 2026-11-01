'use client'

import { track } from '@/lib/track'

/**
 * A link that reports the click before the navigation starts.
 *
 * The download buttons are server-rendered, and a click handler needs a client component —
 * this is the smallest one that does the job, so the pages around it stay on the server.
 */
export function TrackedLink({
  href,
  event,
  properties,
  className,
  children,
}: {
  href: string
  event: string
  properties?: Record<string, unknown>
  className?: string
  children: React.ReactNode
}) {
  return (
    <a
      href={href}
      className={className}
      onClick={() => track(event, { ...properties, href })}
    >
      {children}
    </a>
  )
}
