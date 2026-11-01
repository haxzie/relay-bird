import type { Metadata } from 'next'

import { AUTHOR_URL, REPO } from '@/lib/site'

export const metadata: Metadata = {
  title: 'About — RelayBird',
  description: 'What RelayBird is, what it refuses to do, and who built it.',
}

export default function About() {
  return (
    <main className="pt-16 sm:pt-24">
      <h1 className="text-4xl font-normal tracking-tight sm:text-5xl">
        About
        <span className="block text-muted">what this is, and what it refuses to be</span>
      </h1>

      <div className="prose mt-12 max-w-2xl">
        <p>
          There is no good way to read your phone&rsquo;s text messages on a Mac. iMessage
          covers iPhone; if you carry an Android, every one-time password, bank alert and
          delivery code means picking the phone up again.
        </p>
        <p>
          RelayBird fixes that in the narrowest way possible. The Android app holds your
          messages and serves them over your own Wi-Fi. The Mac app sits in the menu bar and
          reads them. Nothing is relayed through a server, because there is no server.
        </p>

        <h2>What it will not do</h2>
        <ul>
          <li>
            <strong>Work off your network.</strong> Both devices have to be on the same Wi-Fi.
            Fixing that would mean adding the relay the project exists to avoid.
          </li>
          <li>
            <strong>Send messages.</strong> It mirrors. The Mac can show a message and mark it
            read; it cannot send one from your number.
          </li>
          <li>
            <strong>Ask you to sign in.</strong> There is no account, so there is no database
            of other people&rsquo;s text messages to be breached.
          </li>
        </ul>

        <h2>How it is built</h2>
        <p>
          Two native apps: SwiftUI on the Mac, Jetpack Compose on Android. They speak a small
          length-prefixed JSON protocol over TLS, authenticated with HMAC and a pinned
          certificate. The protocol is documented with fixed test vectors that both test
          suites assert against, which is what stops the two halves drifting apart.
        </p>
        <p>
          It is{' '}
          <a href={REPO} target="_blank" rel="noreferrer">
            open source
          </a>
          . The Android half is distributed as a signed APK rather than through Google Play.
          Play only permits SMS permissions for apps actively registered as your default SMS,
          Phone or Assistant handler, and lists &ldquo;SMS or phone notification enhancement
          and alerts&rdquo; among its{' '}
          <a href="https://support.google.com/googleplay/android-developer/answer/10208820" target="_blank" rel="noreferrer">
            invalid use cases
          </a>{' '}
          for anything else. RelayBird could qualify by becoming your default SMS app, but
          that would require it to send messages — which is a different product.
        </p>

        <h2>Who</h2>
        <p>
          Built by{' '}
          <a href={AUTHOR_URL} target="_blank" rel="noreferrer">
            Haxzie
          </a>
          , mostly because the alternative was picking up a phone forty times a day.
        </p>
      </div>
    </main>
  )
}
