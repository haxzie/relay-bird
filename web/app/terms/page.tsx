import type { Metadata } from 'next'
import Link from 'next/link'

import { CONTACT_EMAIL, REPO } from '@/lib/site'

export const metadata: Metadata = {
  title: 'Terms of Use — RelayBird',
  description:
    'The terms you accept by using RelayBird: what it is, what it is not, and the limits of what it promises.',
  alternates: { canonical: '/terms' },
}

/** The date the current wording took effect. Bump it whenever the substance changes. */
const EFFECTIVE = '3 November 2026'

export default function Terms() {
  return (
    <main className="pt-16 sm:pt-24">
      <h1 className="text-4xl font-normal tracking-tight sm:text-5xl">
        Terms of Use
        <span className="block text-muted">short, because the product is small</span>
      </h1>
      <p className="mt-6 text-xs text-muted">In effect since {EFFECTIVE}</p>

      <div className="prose mt-12 max-w-2xl">
        <p>
          By installing or using RelayBird — the Android app, the Mac app, or this website —
          you agree to what follows. If you do not, do not use it; uninstalling is the whole of
          the exit process, because there is no account to close.
        </p>

        <h2>What you are getting</h2>
        <p>
          A licence to use RelayBird for free, for as long as you like, on devices you own or
          control. The source is public and the repository governs your rights to the code
          itself; these terms cover your use of the built apps and this site.
        </p>

        <h2>What you agree to use it for</h2>
        <ul>
          <li>
            <strong>Your own messages, on your own devices.</strong> Pairing a Mac grants that
            Mac the ability to read the text messages on the paired phone. Only pair devices
            you control, and only mirror a phone whose messages you are entitled to read.
          </li>
          <li>
            <strong>Not for surveillance.</strong> Installing RelayBird on someone else&rsquo;s
            phone, or pairing your Mac to a phone that is not yours, in order to read their
            messages without their knowledge is a misuse of this software and in many places a
            crime. It is the one use we would actively prefer you did not find a way to make
            work.
          </li>
          <li>
            <strong>Lawfully.</strong> You are responsible for complying with the laws that
            apply to you, including those on interception of communications and on handling
            other people&rsquo;s personal data.
          </li>
        </ul>

        <h2>What it does not promise</h2>
        <p>
          RelayBird is provided <strong>as is</strong>, without warranty of any kind, express or
          implied, including any warranty of merchantability, fitness for a particular purpose
          or non-infringement. Specifically, and not as fine print:
        </p>
        <ul>
          <li>
            <strong>It is not a reliable message delivery system.</strong> It mirrors what is
            already on your phone, on a best-effort basis, over a home network. Messages can be
            delayed or missed entirely — because the devices fell off Wi-Fi, because Android
            stopped the service, or because of a bug.
          </li>
          <li>
            <strong>Do not depend on it for anything urgent.</strong> Not for emergencies, not
            for medical or safety alerts, and not as your only sight of a two-factor code.
            Your phone remains the source of truth.
          </li>
          <li>
            <strong>It will not send messages.</strong> It reads. If you need to reply, that is
            still the phone&rsquo;s job.
          </li>
          <li>
            <strong>SMS only.</strong> MMS, group threads, drafts and outbox messages are out
            of scope and may never be supported.
          </li>
        </ul>

        <h2>Your data and your security</h2>
        <p>
          Your messages stay between your phone and the Macs you pair — see the{' '}
          <Link href="/privacy">Privacy Policy</Link> for exactly what moves where. That also
          means the security of what is mirrored depends on things only you control: who can
          physically reach your unlocked Mac, who is on your Wi-Fi, and which Macs you have
          left paired. Review <strong>Paired Macs</strong> on the phone and remove anything you
          no longer recognise.
        </p>
        <p>
          You are responsible for keeping backups of anything you care about. RelayBird is a
          window onto your phone, not a store of record.
        </p>

        <h2>Limits on liability</h2>
        <p>
          To the fullest extent permitted by law, the author is not liable for any indirect,
          incidental, special, consequential or exemplary damages, nor for lost profits, lost
          data, or any loss arising from a message you did not see in time. Nothing here limits
          liability that cannot be limited by law, and if your jurisdiction grants you
          consumer rights that override the above, those rights apply.
        </p>

        <h2>Changes and availability</h2>
        <p>
          These terms may change; the date at the top records when, and the public repository
          records exactly what. Releases, this website and the version-check endpoint are
          offered without any uptime commitment and may be modified or discontinued. The copy
          of the app already on your devices keeps working regardless.
        </p>

        <h2>Contact</h2>
        <p>
          Questions about these terms:{' '}
          <a href={`mailto:${CONTACT_EMAIL}`}>{CONTACT_EMAIL}</a>, or{' '}
          <a href={`${REPO}/issues`} target="_blank" rel="noreferrer">
            open an issue
          </a>{' '}
          on the repository.
        </p>
      </div>
    </main>
  )
}
