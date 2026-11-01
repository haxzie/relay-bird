import type { Metadata } from 'next'
import { GoogleAnalytics } from '@next/third-parties/google'

import { PostHogAnalytics } from '@/components/Analytics'
import { DevTools } from '@/components/DevTools'
import { SiteFooter } from '@/components/SiteFooter'
import { SiteNav } from '@/components/SiteNav'
import { GA_ID } from '@/lib/analytics'
import { SITE_URL } from '@/lib/site'
import './globals.css'

export const metadata: Metadata = {
  // Without this, Next emits relative OG image URLs, which several crawlers ignore.
  metadataBase: new URL(SITE_URL),
  title: 'RelayBird — your Android messages, on your Mac',
  description:
    'Read your phone’s text messages from the macOS menu bar, over your own Wi-Fi. No account, no cloud.',
}

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body className="min-h-screen bg-ink text-white antialiased">
        {/* The container lives here so every route — home, blog, about — shares one set of
            margins, and the nav and footer are written once. */}
        <div className="mx-auto w-full max-w-6xl px-6">
          <SiteNav />
          {children}
          <SiteFooter />
        </div>
        <PostHogAnalytics />
        <DevTools />
        {GA_ID && <GoogleAnalytics gaId={GA_ID} />}
      </body>
    </html>
  )
}
