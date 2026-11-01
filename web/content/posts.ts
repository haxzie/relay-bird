/**
 * Posts, as data rather than files on disk.
 *
 * The site runs on Cloudflare Workers, where there is no filesystem at request time. Reading
 * markdown from `content/*.md` would work in `next dev` and then fail in production, or force
 * every post to be statically generated while the rest of the site renders per request.
 * Bundling the content sidesteps that entirely: it renders identically in both.
 */

export type Post = {
  slug: string
  title: string
  date: string
  summary: string
  body: string
}

export const POSTS: Post[] = [
  {
    slug: 'android-play-protect-blocked',
    title: 'Play Protect blocked the install: what to do',
    date: '2026-11-01',
    summary:
      'Android blocks any sideloaded app that reads SMS, by design and regardless of who made it. Here is why, and the three ways to install anyway.',
    body: `
Installing the RelayBird APK from a browser shows this:

> This app can request access to sensitive data. This can increase the risk of identity
> theft or financial fraud.

The install is blocked. Nothing is wrong with the download — this happens to every build,
on every device, for a documented reason.

## Why

Google Play Protect treats anything downloaded through a browser, messaging app or file
manager as an *internet-sideloading source*. If such an app also declares one of four
sensitive permissions — \`READ_SMS\`, \`RECEIVE_SMS\`, notification listener or accessibility —
Play Protect [blocks the installation
automatically](https://developers.google.com/android/play-protect/warning-dev-guidance).

The reasoning is sound. Reading someone's texts is how one-time passwords get stolen, and
that is the single most common shape of Android financial fraud. RelayBird reads your texts
for a different reason, but the permission it asks for is identical to the one malware asks
for, and Play Protect cannot tell the difference at install time.

This is not about the app being new, unsigned or unpopular. The APK is signed with a
4096-bit release key and notarised through the same pipeline as the Mac build. The block is
categorical: declare the permission, get blocked.

## The short way

The installer does both halves, and puts the Android app on over ADB:

\`\`\`
curl -fsSL https://relaybird.haxzie.com/install.sh | sh
\`\`\`

It fetches the current release, checks the Mac app's signature before installing it, and
walks you through enabling USB debugging if the phone is not already connected. If you would
rather do it by hand, the three routes below are what it is doing.

## Three ways to install by hand

### Install over ADB

The block applies to internet-sideloading sources. A computer using the Android debug bridge
is not one, so this route is unaffected:

1. On the phone, enable **Developer options** (tap **Build number** in Settings → About
   seven times), then turn on **USB debugging**.
2. Connect the phone to your computer.
3. Run \`adb install RelayBird-android.apk\`.

This is the cleanest option and the one we use ourselves.

### Install anyway, where the device offers it

Some Android builds show **More details** on the warning, with an **Install anyway**
underneath. Tap through it and the install proceeds. Whether this appears depends on your
Android version and your manufacturer — several OEMs have removed it.

### Turn Play Protect scanning off briefly

In the Play Store, tap your profile picture → **Play Protect** → the gear icon → turn off
**Scan apps with Play Protect**, install RelayBird, then **turn it back on**.

Leaving it off is a bad idea. It is the thing that catches actual SMS-stealing malware, and
that malware is common. Turn it off for the install and on again immediately.

## What we are doing about it

Google lists *"apps whose primary purpose is to handle SMS / MMS content"* among the allowed
uses of these permissions, which is a fair description of RelayBird. There is an appeal
route for apps blocked in error, and it has been submitted.

The other route is Google Play, which RelayBird cannot use either: Play restricts SMS
permissions to apps registered as your default SMS handler, and RelayBird deliberately
cannot send messages. Being the default SMS app would mean becoming a different product.

Until an appeal lands, ADB is the reliable path.
`.trim(),
  },
  {
    slug: 'how-to-view-android-sms-on-mac',
    title: 'How to view Android SMS on Mac',
    date: '2026-11-01',
    summary:
      'Read your Android text messages on macOS in about five minutes, over your own Wi-Fi, with no account and nothing passing through a server.',
    body: `
You can read your Android SMS on a Mac without a Google account, without a browser tab left
open, and without your messages passing through anyone's server. It takes two apps and one
QR code.

![RelayBird on a Mac and an Android phone](/blog/both-devices.webp)

## What you need

- A Mac on **macOS 14** or later
- An Android phone on **Android 8.0** or later
- Both on the **same Wi-Fi network**

That last one is the catch, and it is deliberate: the Mac talks straight to your phone across
the local network, so there is no server in the middle to reach you from elsewhere. If you
need your texts on a laptop in another country, see [the comparison of all four
options](/blog/how-to-see-android-messages-on-mac) instead — Google Messages for web is the
right tool for that.

## Step 1: Install RelayBird on the Mac

Download the disk image and drag the app to Applications. It is signed with a Developer ID
and notarised, so it opens with no Gatekeeper warning.

It lives in the menu bar — no Dock icon, no window. When macOS asks for permission to find
devices on your local network, **allow it**. The Mac cannot find your phone without it, and
macOS will not ask a second time.

## Step 2: Install RelayBird on the phone

The Android app is a signed APK from the releases page. Open it on the phone and allow
installation when prompted.

Grant the messages and contacts permission — contacts is what turns a bare number into a
name — and accept the battery optimisation exemption. Without that exemption Android stops
the service once the phone has been idle for a while, and your messages quietly stop
arriving on the Mac.

## Step 3: Scan the QR code

On the Mac, open the menu bar app and choose **Pair a phone**.

![The Mac showing a pairing QR code](/blog/mac-pairing.webp)

On the phone, tap **Pair a Mac** and scan it. That is the entire pairing: the two swap a key,
the Mac pins the phone's certificate, and they reconnect by themselves from then on.

## Step 4: Read your SMS

Click the feather in the menu bar.

![Android SMS shown in the macOS menu bar](/blog/mac-messages.webp)

Conversations are listed newest first with unread ones marked. New messages arrive as a
banner; click one to open that thread. Reading on the Mac marks the message read on the
phone too, so the unread count does not drift between them.

## Frequently asked

**Can I reply to SMS from the Mac?**
Not with RelayBird — it mirrors only. If replying matters more than keeping your messages
off a server, use Google Messages for web.

**Does this work without internet?**
Yes. Both devices need the same Wi-Fi, but neither needs an internet connection. The traffic
never leaves your router.

**Do I need a Google account?**
No. There is no account of any kind, which also means there is no inbox of yours stored
anywhere to be breached.

**Will it show one-time passwords?**
Yes, and that is largely the point. Google Messages for web [blocks OTPs entirely in
India](/blog/how-to-see-android-messages-on-mac) as an anti-fraud measure; a local bridge is
unaffected, because nothing passes through a third party that could withhold them.

**Something is not working.**
The [full setup guide](/blog/relaybird-setup-guide) has a troubleshooting section covering
discovery failures, router client isolation and certificate changes.
`.trim(),
  },
  {
    slug: 'relaybird-setup-guide',
    title: 'Setting up RelayBird: a step-by-step guide',
    date: '2026-11-01',
    summary:
      'Install both halves, pair them with a QR code, and read your Android texts from the Mac menu bar. Five minutes, no account, nothing leaving your Wi-Fi.',
    body: `
RelayBird puts your Android text messages in the macOS menu bar, over your own Wi-Fi. There
is no account and no server in the middle — the phone serves its messages and the Mac reads
them directly.

This is the detailed reference, including troubleshooting. If you just want the short
version, read [how to view Android SMS on Mac](/blog/how-to-view-android-sms-on-mac).

![The Mac menu-bar panel beside the Android app](/blog/both-devices.webp)

## Before you start

You need:

- A Mac running **macOS 14 or later**
- An Android phone running **Android 8.0 or later**
- Both on the **same Wi-Fi network** — this is the one hard requirement, and there is no way
  around it by design

## Step 1: Install the Mac app

Download the disk image, open it, and drag RelayBird to your Applications folder. The app is
signed with a Developer ID and notarised by Apple, so it opens without any Gatekeeper
warnings.

On first launch it appears in the menu bar — there is no Dock icon and no window. That is
deliberate: the whole app is the menu bar.

macOS will ask for permission to find devices on your local network. **Allow it.** Without
it the Mac cannot see your phone at all, and macOS gives no second prompt.

## Step 2: Install the Android app

The Android half is a signed APK from the releases page rather than a Play Store listing.
Open it on your phone and allow installation from your browser when prompted.

![The RelayBird Android app](/blog/android-home.webp)

On first run the app asks for two permissions:

- **Messages and contacts** — so it can read your inbox and show sender names instead of
  bare phone numbers
- **Notifications** — so it can show the ongoing notification Android requires while a
  background service is running

It will also offer to exempt itself from battery optimisation. Accept it. Without the
exemption Android will stop the mirroring service whenever the phone sits idle for a while,
and messages will quietly stop arriving.

## Step 3: Pair them

On the Mac, open the menu bar app and choose **Pair a phone**. It shows a QR code that is
valid for two minutes.

![The Mac showing a pairing QR code](/blog/mac-pairing.webp)

On the phone, tap **Pair a Mac** and scan the code.

That is the whole pairing. The two devices exchange a key, the Mac pins the phone's
certificate, and from then on they reconnect silently whenever both are on the network. You
will not be asked to pair again.

If the Mac cannot find the phone, check that both are on the same network and that you
allowed the local network prompt in step 1. On some routers, "client isolation" or "AP
isolation" stops devices seeing each other — turning that off fixes it.

## Step 4: Read your messages

Click the feather in the menu bar. Your conversations are there, newest first, with unread
ones marked.

![The RelayBird panel open in the macOS menu bar](/blog/mac-messages.webp)

Click a conversation to open it. New messages arrive as a banner; click one and it opens
that thread. Reading a message on the Mac marks it read, and the count stays in step across
both devices.

## What it will not do

- **Send messages.** RelayBird mirrors. It can show you a message and mark it read; it
  cannot send one from your number.
- **Work off your Wi-Fi.** Both devices have to be on the same network. Adding remote access
  would mean a relay server, which is the thing the project exists to avoid.
- **Carry MMS or group threads.** Android has no public API for enumerating group
  participants, so those are out of scope.

## Troubleshooting

**The Mac says "Connecting…" and never finishes.**
The phone has probably dropped off the network, or mirroring is off in the Android app. Open
the app and check it says "Connected now".

**Messages stop arriving after the phone has been idle.**
Battery optimisation is stopping the service. In Android settings, find RelayBird under apps
and set battery usage to unrestricted.

**Nothing arrives after a router change.**
The Mac caches the phone's last address. Give it a few seconds to fall back to discovery, or
toggle mirroring off and on in the Android app.

**It says "device identity changed".**
The phone regenerated its certificate — normally because the app's data was cleared or it
was reinstalled. Forget the phone in the Mac's settings and pair again. This warning is
working as intended: it is what stops something else on your network impersonating your
phone.
`.trim(),
  },
  {
    slug: 'how-to-see-android-messages-on-mac',
    title: 'How to see your Android messages on a Mac',
    date: '2026-11-01',
    summary:
      'Four ways to read Android texts on macOS \u2014 Google Messages for web, RelayBird, Pushbullet and KDE Connect \u2014 with the trade-off each one makes and steps for the two worth using.',
    body: `
macOS has no built-in way to read Android texts. Messages on a Mac is iMessage, and iMessage
does not carry SMS from an Android phone. So every one-time password, bank alert and delivery
code means picking the phone up again.

There are four realistic ways around it. This covers what each actually does, where each one
falls down, and how to set up the two worth using.

## The short answer

- **Google Messages for web** — free, works anywhere, but your texts route through Google and
  the browser tab has to stay open.
- **RelayBird** — a menu-bar app that reads your phone over your own Wi-Fi. No account, no
  server, but read-only and same-network.
- **Pushbullet** — mirrors notifications broadly; free tier is capped and it is a cloud relay.
- **KDE Connect** — open source and local, but the Mac build is rough.

## Option 1: Google Messages for web

The official route, and the one most people should try first.

1. On Android, open **Messages**, tap your profile picture, then **Device pairing**.
2. On the Mac, go to **messages.google.com/web** in any browser.
3. Scan the QR code the browser shows with your phone.
4. Tick **Remember this computer** so you are not re-pairing daily.

**What you get:** full two-way messaging, including sending, group chats and images. It is
the only option here that lets you reply.

**What it costs:** the phone must be online and reachable, your messages travel through
Google's infrastructure, and it lives in a browser tab — close the tab and the notifications
stop. If RCS is on, the web client sometimes lags behind the phone by a few seconds.

**The catch nobody mentions:** in India, and reportedly some other regions, Google blocks
one-time passwords from appearing in Messages for web at all. A Google product expert
confirms it was [turned off due to security issues](https://support.google.com/messages/thread/177630470/how-to-view-otp-one-time-password-on-messages-for-web?hl=en),
and the thread has hundreds of people asking the same question. The messages arrive on the
phone and simply never appear in the browser.

That matters, because reading one-time passwords is the single most common reason to want
your texts on a laptop. If you are in an affected region, the official option cannot do the
one job you wanted it for.

## Option 2: RelayBird

The reason this site exists, so take the recommendation with that in mind.

![RelayBird on a Mac and an Android phone](/blog/both-devices.webp)

RelayBird is two native apps — a Mac menu-bar app and an Android app — that talk directly to
each other over your local network. The phone serves its own messages; the Mac connects to
it. Nothing is relayed through a server, because there is no server to relay through.

1. Install the Mac app and the Android APK.
2. Open the Mac app. It shows a QR code.
3. Scan it from the Android app. The two pair once and reconnect silently after that.

![The RelayBird panel in the macOS menu bar](/blog/mac-messages.webp)

**What you get:** messages in the menu bar, a banner the moment one arrives, and no account
anywhere. Nothing leaves your Wi-Fi network, so there is no inbox of yours sitting on
somebody else's disk.

**What it costs:** both devices have to be on the same Wi-Fi, and it is read-only — it
mirrors your messages, it cannot send them. The Android half is a signed APK you install
directly rather than a Play Store listing: Google Play only permits SMS permissions for apps
actively registered as your default SMS, Phone or Assistant handler, and lists ["SMS or phone
notification enhancement and alerts"](https://support.google.com/googleplay/android-developer/answer/10208820) as an invalid use case for everything
else. RelayBird could qualify by becoming your default SMS app — but that would mean it has
to send messages too, which is a different product from a read-only mirror.

## Option 3: Pushbullet

Pushbullet mirrors Android notifications — including texts — to a desktop app or browser
extension. It handles more than SMS, which is the appeal.

The free tier caps how many messages you can send per month, and like Google Messages it is a
cloud relay: your notifications pass through Pushbullet's servers. The Mac experience is a
browser extension rather than a real app.

## Option 4: KDE Connect

Open source, local-network, and genuinely good on Linux. The macOS build exists but is
noticeably less polished than the Linux one, and setup involves more steps than most people
want for reading a text. Worth it if you already run KDE Connect elsewhere.

## Which to pick

| | Replies | Works off your Wi-Fi | Account needed | Your texts pass through |
|---|---|---|---|---|
| Google Messages for web | Yes | Yes | Google account | Google |
| ↳ OTPs, in India | — | — | — | blocked entirely |
| RelayBird | No | No | None | Nothing |
| Pushbullet | Limited on free | Yes | Pushbullet account | Pushbullet |
| KDE Connect | Yes | No | None | Nothing |

If you need to **reply** from the Mac, use Google Messages for web. Nothing else here does
that properly.

If you mostly need to **read** — codes, alerts, the message you glance at and act on — and
would rather those not pass through anyone's servers, a local option is a better fit.

## Common questions

**Can I see Android messages on a Mac without the internet?**
Yes, with a local option. RelayBird and KDE Connect work over your Wi-Fi with no internet at
all, because the two devices talk directly. Google Messages and Pushbullet both need the
phone online.

**Does any of this let me send texts from the Mac?**
Google Messages for web does, fully. KDE Connect does. RelayBird deliberately does not — it
mirrors only.

**Is it secure?**
It depends on where the messages go. Cloud options encrypt in transit but the provider
handles your messages. Local options keep them on your network; RelayBird uses TLS with the
phone's certificate pinned on the Mac, so a swapped certificate is refused rather than
trusted.

**Will it drain my phone battery?**
A well-built bridge should not. The cost is in polling and in waking the radio. RelayBird
holds one idle socket and wakes when a message actually arrives, rather than checking on a
timer.

**Why can I not see OTP messages on Google Messages for web?**
In India and some other regions Google disabled it deliberately, as an anti-fraud measure.
There is no setting to turn it back on. A local bridge is unaffected, because the messages
never leave your own network for anyone to withhold.

**Why can I not just use Messages on my Mac?**
Apple's Messages app handles iMessage and, for iPhone owners, SMS forwarded from an iPhone.
It has no mechanism for pairing an Android phone, and Apple has not opened one.
`.trim(),
  },
  {
    slug: 'why-there-is-no-cloud',
    title: 'Why RelayBird has no server',
    date: '2026-10-28',
    summary:
      'Every other phone-to-desktop bridge routes your texts through somebody’s backend. Here is what it costs to not do that, and why it was worth paying.',
    body: `
Every existing way to read your Android texts on a Mac has a server in the middle. Your
messages leave your phone, land on a machine you do not control, and are forwarded to your
laptop. That server sees every one-time password, every bank alert, every message from your
family.

It is not that these products are careless. A relay is genuinely the easy way to build this.
It gets you past NAT, past sleeping radios, past the two devices never being on the same
network. You get push notifications for free. You get an account system, which means you get
support tooling, analytics, and a business model.

RelayBird does none of that, and the phone is the server.

## What that buys

The Mac connects directly to your phone over the local network, with TLS, and the phone's
certificate pinned on the Mac after the first pairing. There is no account to create, no
password to leak, and no database of other people's text messages to be breached. If the
project disappears tomorrow, the two apps you already installed keep working, because there
is nothing to shut down.

## What it costs

Both devices have to be on the same Wi-Fi. That is a real limitation and not one I can
engineer around without adding the exact thing the product exists to avoid. If you want your
texts on a laptop in another country, this is the wrong tool.

Discovery is also harder than it looks. mDNS is genuinely flaky, so the Mac caches the last
address it saw and races a direct connection against the Bonjour lookup. The phone has to
hold a multicast lock or the Wi-Fi chip drops the discovery packets in hardware the moment
the screen goes off — that one took a while to find.

## The trade

A relay would make the product work in more places and know everything you receive. No relay
makes it work in one place and know nothing. For something whose entire job is handling
one-time passwords, that is not a close call.
`.trim(),
  },
  {
    slug: 'pairing-without-accounts',
    title: 'Pairing two devices without an account',
    date: '2026-10-30',
    summary:
      'A QR code, one shared secret, and four HMACs. How the Mac and the phone come to trust each other with nothing in between them.',
    body: `
Pairing is the part of a local-first product that people expect to be awkward, and it is
where most of them give up and add a login. RelayBird borrows the shape of WhatsApp Web: the
Mac shows a QR code, the phone scans it, and the two are bound together for good.

There is no asymmetric cryptography here, and that is deliberate. There are exactly two
peers and no third party who needs to verify a signature, so everything is HMAC-SHA256 over a
shared secret.

## The handshake

The Mac generates 32 random bytes and renders them as a QR code that expires in two minutes.
The phone scans it. The Mac then finds the phone over Bonjour, connects, and proves it knows
the secret by sending an HMAC over the phone's own certificate fingerprint.

That binding is the important part. An attacker on your network can present their own
certificate, but they cannot produce a MAC over it that the phone will accept, because they
never saw the QR code.

The phone verifies, generates a long-term device key, and sends it back alongside a second
HMAC. Only after checking *that* does the Mac commit the certificate pin to the Keychain.
Skipping that step would let someone who cannot complete the handshake still convince the Mac
to permanently trust a bogus certificate — an unrecoverable denial of service.

## Every reconnection after that

The phone sends a fresh random challenge; the Mac answers with an HMAC over the challenge and
the pinned fingerprint. Every MAC in the protocol is domain-separated and versioned, so a
proof minted for one step cannot be replayed into another, and both sides compare in constant
time.

The whole exchange is in [docs/PROTOCOL.md](https://github.com/haxzie/relay-bird), with fixed
test vectors that both the Swift and Kotlin suites assert against. That one test is what
catches the two implementations silently drifting apart.
`.trim(),
  },
]

export function allPosts(): Post[] {
  return [...POSTS].sort((a, b) => b.date.localeCompare(a.date))
}

export function findPost(slug: string): Post | undefined {
  return POSTS.find((p) => p.slug === slug)
}

export function formatDate(iso: string): string {
  return new Date(`${iso}T00:00:00Z`).toLocaleDateString('en-GB', {
    day: 'numeric',
    month: 'long',
    year: 'numeric',
    timeZone: 'UTC',
  })
}
