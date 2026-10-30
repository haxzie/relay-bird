# RelayBird wire protocol — v2

Single source of truth for both apps. Any change here needs a matching change in
`android/app/src/main/kotlin/dev/haxzie/relaybird/net/` and `macos/RelayBird/Net/`.

## Roles

- The **phone** is the server. It owns the SMS data and listens on a TLS socket.
- The **Mac** is always the client. It never listens.

## Discovery

The phone advertises Bonjour / mDNS service type `_relaybird._tcp` on the local domain.

TXT record:

| key    | value                                  |
|--------|----------------------------------------|
| `v`    | `1`                                    |
| `did`  | phone device id (random UUID, stable)  |
| `name` | human-readable phone name              |

The Mac also caches the last successful `host:port` and races a direct connect against the
Bonjour resolve, because mDNS is unreliable in practice. A manual `host:port` override exists
in Settings (and is the only way to reach an emulator).

## Transport

TCP, TLS 1.2+. The phone presents a self-signed P-256 certificate generated on first launch.
The Mac pins the SHA-256 of the certificate's DER-encoded SubjectPublicKeyInfo (**SPKI hash**).

The certificate must be a normal X.509v3 server certificate: basic constraints, key usage
(`digitalSignature` only — it is an EC key, and claiming `keyEncipherment` gets it rejected),
extended key usage `serverAuth`, a subject alternative name, and a sane validity window. Apple's
TLS stack rejects a bare certificate with `errSSLBadCert` *before* the client's custom
verification runs, so the pin never gets a chance to accept it.

Framing: `UInt32` big-endian byte length, then that many bytes of UTF-8 JSON. Max frame
1 MiB (1048576); a larger declared length is a protocol violation and closes the connection.

Every frame is a JSON object with a `t` (type) discriminator.

## Messages

| Direction  | `t`        | Fields                                                                        |
|------------|------------|-------------------------------------------------------------------------------|
| phone→mac  | `hello`    | `v`, `deviceId`, `deviceName`, `paired` (bool), `challenge` (b64, 32B)        |
| mac→phone  | `pair`     | `name` (Mac name), `proof` (b64 HMAC)                                         |
| phone→mac  | `paired`   | `confirm` (b64 HMAC), `deviceKey` (b64, 32B)                                  |
| mac→phone  | `auth`     | `proof` (b64 HMAC)                                                            |
| phone→mac  | `ready`    | —                                                                             |
| mac→phone  | `sync`     | `limit` (threads to return), `watermarks`: `[{threadId, lastReadId}]`         |
| phone→mac  | `threads`  | `threads`: `[{threadId, address, displayName?, snippet, date, unread, latestMessageId}]` |
| mac→phone  | `thread`   | `threadId`, `limit`, `beforeId?`                                              |
| phone→mac  | `messages` | `threadId`, `messages`: `[{id, threadId, address, body, date, outgoing, read}]` |
| phone→mac  | `message`  | `message`: a single message object, pushed live                               |
| mac→phone  | `markRead` | `threadId`, `upToId`                                                          |
| both       | `ping`     | —                                                                             |
| both       | `pong`     | —                                                                             |
| phone→mac  | `error`    | `code`, `msg`                                                                 |

`date` is epoch milliseconds. `outgoing` is true for `Telephony.Sms.MESSAGE_TYPE_SENT`.

Decoders must tolerate a missing field and fall back to a zero value rather than rejecting the
frame, so that adding one does not become a flag day.

## Read state

**The Mac owns it.** A non-default SMS app cannot write to the SMS provider: the phone's
`markRead` handler issues the update, Android drops it without an error, and the next `sync`
would report the same conversations as unread again — forever.

So the Mac records, per thread, the id of the newest message it has shown (`lastReadId`), keeps
it in its own database, and sends the set of them with every `sync`. The phone subtracts them
when counting:

    unread(thread) = count(messages where type = inbox and read = 0 and _id > lastReadId)

A thread therefore stays read once read, and goes unread again only when a message newer than
the mark arrives. `markRead` is still sent so the phone can attempt the provider write for the
benefit of its own Messages app; nothing depends on it succeeding. Read marks never move
backwards.

`latestMessageId` lets the Mac reach the same conclusion on its own — it displays zero unread
whenever its mark has caught up — so a sync that predates the watermarks cannot resurrect a
badge either.

Heartbeat: each side sends `ping` every 20s of idleness and closes the connection if no frame
of any kind arrives for 60s.

`error` codes: `unpaired`, `bad_proof`, `protocol`, `no_permission`, `revoked`.

## Pairing handshake

All-symmetric, HMAC-SHA256 only. There are exactly two peers and no third party to verify a
signature against, so asymmetric identity keys would be pure overhead.

Notation: `‖` is byte concatenation, `H(k, m)` is HMAC-SHA256. All context strings are ASCII
and carry a version, so a future protocol change can never produce a colliding MAC.

1. Phone generates its self-signed cert on first launch. `spkiHash = SHA256(cert SPKI DER)`.
2. Mac generates `secret` = 32 bytes from a CSPRNG and renders the QR
   `relaybird://pair?v=1&name=<percent-encoded Mac name>&s=<base64url secret, no padding>`.
   Valid for 120 seconds, single use.
3. The Android app scans it and stores `(secret, macName)` as a *pending* pairing.
4. The Mac connects, accepts the phone's cert **in memory only**, and on `hello` with
   `paired: false` sends `pair` with

       proof = H(secret, "relaybird-pair-v1" ‖ spkiHash)

   This binds the QR secret to the exact certificate the Mac actually saw. A man-in-the-middle
   presents its own certificate, so its `proof` is over the wrong SPKI and the phone rejects it.
5. The phone verifies `proof` against its pending pairing, mints a random 32-byte `deviceKey`,
   and replies `paired` with

       confirm   = H(secret, "relaybird-confirm-v1" ‖ spkiHash ‖ utf8(macName))
       deviceKey = <32 random bytes>

6. The Mac verifies `confirm` and **only then** commits the SPKI pin and `deviceKey` to the
   Keychain. Without this step a MITM that cannot complete the handshake could still get the
   Mac to permanently pin a bogus certificate — an unrecoverable denial of service.
7. Both sides wipe `secret`.
8. On every later connect, `hello` carries `paired: true` and a fresh 32-byte `challenge`.
   The Mac replies `auth` with

       proof = H(deviceKey, "relaybird-auth-v1" ‖ challenge ‖ spkiHash)

   A pin mismatch is a hard abort surfaced as "device identity changed — re-pair", never a
   silent re-pin.

MAC comparison is constant-time on both sides: `HMAC.isValidAuthenticationCode` (CryptoKit),
`MessageDigest.isEqual` (Android).

## Test vectors

Asserted by both test suites — this is what catches silent interop drift.

```
secret    (b64) AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8=   # 0x00..0x1f
spkiHash  (b64) ICEiIyQlJicoKSorLC0uLzAxMjM0NTY3ODk6Ozw9Pj8=   # 0x20..0x3f
deviceKey (b64) QEFCQ0RFRkdISUpLTE1OT1BRUlNUVVZXWFlaW1xdXl8=   # 0x40..0x5f
challenge (b64) YGFiY2RlZmdoaWprbG1ub3BxcnN0dXZ3eHl6e3x9fn8=   # 0x60..0x7f
macName         Test Mac

pair    proof   ae5e1c9c5e34a70cdc313eba20ce9c2b4272ed81a8e381b0c187b85bb25f2255
confirm proof   688b0e4cbbd809ad9cc27d1b76c628622e3f825130c4ce3d6b9fce3f383f6de2
auth    proof   490195dbe69816d467535d13a730bd765eee83aeea6caaff2a4a7b1c4c708f51
```

## Known limits (v1)

- SMS only. MMS and group threads are out of scope — there is no public API to enumerate a
  thread's recipients, so a group thread would render under one participant's address.
- A non-default SMS app can only read the `inbox` and `sent` boxes. No drafts, outbox or failed.
- Read-only: the Mac cannot send.
