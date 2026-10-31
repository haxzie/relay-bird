import type { Metadata } from 'next'
import './globals.css'

export const metadata: Metadata = {
  title: 'RelayBird — your Android messages, on your Mac',
  description:
    'Read your phone’s text messages from the macOS menu bar, over your own Wi-Fi. No account, no cloud.',
}

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body className="min-h-screen bg-ink text-white antialiased">{children}</body>
    </html>
  )
}
