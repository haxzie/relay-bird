# Changesets

A release here is one version covering both apps, because they ship as a pair: the protocol
version gates them against each other, so a phone and a Mac from different releases refuse to
talk. `fixed` in `config.json` enforces that — bumping either package bumps both.

Nothing is published to npm. `privatePackages` is set to version and tag these private packages
anyway, so `changeset tag` produces the `v*` tag that `release.yml` builds from.

## Recording a change

```sh
pnpm changeset
```

Pick the bump, describe the change in a sentence someone reading the release notes would want,
and commit the generated file with your work.

## Cutting a release

Push to `main` and the Version Packages pull request opens or updates itself. Merging it bumps
the versions, writes the changelogs, and tags the merge commit — which is what starts the build.
