import type { Metadata } from 'next'
import Link from 'next/link'

import { CONTACT_EMAIL, REPO } from '@/lib/site'

export const metadata: Metadata = {
  title: 'Privacy Policy — RelayBird',
  description:
    'What RelayBird touches, where it goes, and what is never collected. Your messages stay on your own network.',
  alternates: { canonical: '/privacy' },
}

/** The date the current wording took effect. Bump it whenever the substance changes. */
const EFFECTIVE = '3 November 2026'

export default function Privacy() {
  return (
    <main className="pt-16 sm:pt-24">
      <h1 className="text-4xl font-normal tracking-tight sm:text-5xl">
        Privacy Policy
        <span className="block text-muted">what we touch, and where it goes</span>
      </h1>
      <p className="mt-6 text-xs text-muted">In effect since {EFFECTIVE}</p>

      <div className="prose mt-12 max-w-2xl">
        <p>
          RelayBird is two apps: one on your Android phone, one on your Mac. The phone holds
          your messages and serves them over your own Wi-Fi; the Mac reads them. There is no
          RelayBird server in that path, and there is no account, so there is no database of
          anybody&rsquo;s text messages to be breached or subpoenaed.
        </p>
        <p>
          This policy covers the Android app, the Mac app and this website. It is written
          against what the code actually does — all of which is{' '}
          <a href={REPO} target="_blank" rel="noreferrer">
            public and auditable
          </a>
          .
        </p>

        <h2>The short version</h2>
        <ul>
          <li>
            <strong>Your messages never reach us.</strong> SMS content and contact names go
            from your phone to the Macs you have paired, directly, over your local network.
            They are not uploaded, relayed or backed up anywhere.
          </li>
          <li>
            <strong>The Android app sends no analytics at all.</strong> Not anonymised, not
            aggregated, none.
          </li>
          <li>
            <strong>The Mac app sends anonymous usage events</strong> — which screens get
            opened, not what is in them. It is a single switch in Settings to turn off.
          </li>
          <li>
            <strong>We cannot read your messages even if asked.</strong> Not a promise about
            our intentions; there is simply no copy on our side to hand over.
          </li>
        </ul>

        <h2>What the Android app accesses</h2>
        <p>
          The app asks for these permissions, and each one is used for exactly one thing:
        </p>
        <ul>
          <li>
            <strong>SMS (read and receive)</strong> — to list your conversations and to notice
            a new message as it arrives, so it can appear on a paired Mac. The app reads your
            inbox and sent box; it cannot send a message from your number.
          </li>
          <li>
            <strong>Contacts</strong> — to show a name instead of a bare phone number. Names
            are looked up on the phone and only ever sent to a Mac you paired.
          </li>
          <li>
            <strong>Camera</strong> — to scan the pairing QR code shown by the Mac. Nothing is
            recorded and no frame leaves the phone; the camera is on only while the scanner is
            open.
          </li>
          <li>
            <strong>Notifications</strong> — the ongoing notice Android requires while the
            bridge is running.
          </li>
          <li>
            <strong>Network, Wi-Fi state and multicast</strong> — to announce the phone on your
            local network so the Mac can find it, and to keep answering with the screen off.
          </li>
          <li>
            <strong>Battery optimisation exemption</strong> — optional, and only so Android
            stops killing the bridge while you are using it. Declining it does not disable the
            app.
          </li>
        </ul>
        <p>
          All of it stays on the phone or goes to a Mac you explicitly paired by scanning a
          code. Revoking a Mac under <strong>Paired Macs</strong> drops its session
          immediately.
        </p>

        <h2>Where your messages actually travel</h2>
        <p>
          Phone to Mac, over your own network, on a TLS connection that is mutually
          authenticated and pinned to the exact certificate established during pairing. The
          phone is the server and the Mac is always the client, so nothing is listening for
          your messages from outside. If the two devices are not on the same network, nothing
          syncs — we have deliberately not built the relay that would fix that.
        </p>

        <h2>What is stored, and where</h2>
        <ul>
          <li>
            <strong>On your phone</strong> — the pairing keys for each Mac you have approved.
            Your messages are not copied; they are read from Android&rsquo;s own SMS store.
          </li>
          <li>
            <strong>On your Mac</strong> — the messages it has fetched, so the menu bar opens
            instantly, plus the pairing key for your phone in the macOS Keychain.
          </li>
          <li>
            <strong>On our servers</strong> — none of the above. See below for the one
            exception, which carries no message data.
          </li>
        </ul>
        <p>
          Deleting either app removes its local data. Unpairing from the phone invalidates that
          Mac&rsquo;s access regardless of what it still holds on disk.
        </p>

        <h2>Analytics, stated precisely</h2>
        <p>
          The <strong>Mac app</strong> posts anonymous product events to an endpoint on this
          website. It is on by default and can be switched off under{' '}
          <strong>Send anonymous usage data</strong> in Settings, after which nothing is sent
          and nothing is queued.
        </p>
        <p>
          The event vocabulary is a fixed list — app launched, phone paired, phone connected,
          phone forgotten, messages opened, thread opened, notification clicked, update
          installed — accompanied only by your app version, OS version, platform, device model,
          a count of threads, and whether a connection was a reconnect. The server enforces
          that list and discards anything outside it, so the endpoint is structurally incapable
          of carrying a message, a phone number or a contact name.
        </p>
        <p>
          The identifier attached to these events is a random UUID generated on your Mac. It is
          not derived from your hardware, your identity or your phone, and resetting it is one
          click in Settings. Because the Mac app reports to our endpoint rather than to the
          analytics vendor directly, the vendor never sees your IP address.
        </p>
        <p>
          The <strong>Android app</strong> has no analytics of any kind. It does periodically
          fetch the published version number to tell you when a newer release exists — it
          installs nothing — and like any HTTP request that means our host briefly sees the
          request in its logs, including the originating IP address.
        </p>

        <h2>This website</h2>
        <p>
          relaybird.haxzie.com uses Google Analytics and PostHog to count visits and see which
          pages are read. That involves cookies and the usual page-view data: IP address,
          referrer, browser and operating system. It is ordinary website measurement and is
          entirely separate from the apps — nothing here can see anything on your phone.
        </p>

        <h2>Who else gets the data</h2>
        <p>
          We do not sell anything, and there is nothing in this system that could be sold. The
          only third parties involved are the analytics and hosting providers described above,
          acting as processors for the website and the anonymous Mac events. There is no
          advertising, no data broker, and no profile built about you.
        </p>

        <h2>Children</h2>
        <p>
          RelayBird is not directed at children under 13 and we do not knowingly collect their
          data. Given that the app collects no personal data to begin with, there is nothing
          age-specific to segregate.
        </p>

        <h2>Your rights</h2>
        <p>
          Depending on where you live you may have rights to access, correct, export or delete
          personal data held about you. In practice the honest answer is that we hold no
          account, no contact details and no message data about you — the only record is an
          anonymous random UUID attached to Mac usage events, which you can reset or stop
          generating at any time from Settings. If you would like the events associated with a
          given install id deleted, get in touch and we will remove them.
        </p>

        <h2>Changes</h2>
        <p>
          If this policy changes in substance we will update the date at the top and note it in
          the{' '}
          <Link href="/blog">changelog</Link>. The history of this page is in the public
          repository, so you can see exactly what changed and when.
        </p>

        <h2>Contact</h2>
        <p>
          Questions about this policy, or a request to delete the events tied to an install
          id: <a href={`mailto:${CONTACT_EMAIL}`}>{CONTACT_EMAIL}</a>. Anything that is better
          discussed in the open — a question about how the app behaves, say — can go in an{' '}
          <a href={`${REPO}/issues`} target="_blank" rel="noreferrer">
            issue on the repository
          </a>{' '}
          instead.
        </p>
      </div>
    </main>
  )
}
