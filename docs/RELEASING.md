# Releasing

Both apps carry one version and ship together. The protocol version gates them against each
other — a phone and a Mac from different releases refuse to talk — so `.changeset/config.json`
keeps the two packages in a `fixed` group and a release always produces both artefacts.

## Day to day

Record what changed alongside the work:

```sh
pnpm changeset
```

## Cutting a release

1. Merge your work into `main`. The **Version** workflow opens a *Version packages* pull
   request and keeps it current as more changesets land.
2. Merge that pull request. It bumps both packages, writes the changelogs, and regenerates
   `android/version.properties` and `macos/Config/Version.xcconfig` — the files Gradle and
   Xcode actually read, since neither can read a `package.json`.
3. The next **Version** run finds no pending changesets and tags the merge commit.
4. That tag starts **Release**, which builds a signed APK and a signed, notarised, stapled
   `.app`, then publishes both to a GitHub release.

Nothing is published to npm; the packages are private and exist to give changesets something
to version.

## Secrets

The build degrades rather than fails when these are missing, so a fork still produces working
artefacts — they are just marked `-unsigned`, and the Mac build is not notarised.

| Secret | What it is |
|---|---|
| `CSC_LINK` | base64 of the **Developer ID Application** `.p12` |
| `CSC_KEY_PASSWORD` | the password that `.p12` was exported with |
| `APPLE_API_KEY_BASE64` | base64 of the App Store Connect `.p8` used for notarisation |
| `APPLE_API_KEY_ID` | the key id, the `XXXXXXXXXX` in `AuthKey_XXXXXXXXXX.p8` |
| `APPLE_API_ISSUER` | the issuer UUID from App Store Connect → Users and Access → Integrations |
| `ANDROID_KEYSTORE_BASE64` | base64 of the release keystore |
| `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD` | its passwords and alias |

An **Apple Distribution** certificate is not a substitute for a Developer ID one: it signs
without complaint and Gatekeeper still refuses the download, so the failure lands on whoever
installs it rather than in the build log.

### The Android keystore

`~/Keys/relaybird.jks`, alias `relaybird`, RSA 4096, valid for 10000 days; the password is in
`~/Keys/relaybird-keystore-password.txt`, mode 600. Both are outside the repository and must
stay that way.

**Back it up.** An APK signed with a different key cannot upgrade one signed with this one —
losing it means every installed copy has to be uninstalled before a new build will go on.

### The two secrets that are not set

`CSC_KEY_PASSWORD` and `APPLE_API_ISSUER` cannot be read back out of another repository's
secrets, so they are the one thing left to do. Until they are set, the Mac build is ad-hoc
signed, published as `-unsigned`, and Gatekeeper will refuse it on any other machine.

```sh
# the password the Developer ID .p12 was exported with
gh secret set CSC_KEY_PASSWORD --repo haxzie/relay-bird

# App Store Connect -> Users and Access -> Integrations -> the Issuer ID above the key table
gh secret set APPLE_API_ISSUER --repo haxzie/relay-bird
```

## Building locally

`pnpm release` builds both. The Mac half signs with whatever `macos/Config/Signing.local.xcconfig`
names, and `pnpm macos notarize` submits it using a `notarytool` keychain profile.
