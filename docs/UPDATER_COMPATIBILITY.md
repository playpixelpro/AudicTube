# SmarterTube Updater Compatibility

The `Check for updates` feature must support SmarterTube's versioning scheme before release naming is changed publicly.

Changing tag names without updating the updater risks breaking the most important recovery path users have.

## Goal

The updater must be able to:

```text
[ ] Discover SmarterTube releases from GitHub
[ ] Ignore upstream SmartTube TV releases
[ ] Parse new SmarterTube tags
[ ] Handle legacy SmarterTube tags during migration
[ ] Compare SmarterTube product versions correctly
[ ] Respect alpha/beta/stable channel rules
[ ] Select the correct APK asset
[ ] Open the correct release notes
```

## New Tag Format

Expected format:

```text
v<smartertube-version>+st<smarttube-base>
```

Examples:

```text
v0.4.0-beta.1+st31.93
v0.4.1-beta.2+st31.93
v0.5.0-alpha.1+st31.96
v1.0.0-rc.1+st32.10
v1.0.0+st32.10
```

## Values to Parse

The updater should parse these as separate values.

```text
SmarterTube version: 0.4.0-beta.1
Upstream base:       31.93
Channel:             beta
```

Do not compare the full tag as a single string.

## Version Comparison

The updater should compare SmarterTube product versions first.

Correct:

```text
0.4.1-beta.1 > 0.4.0-beta.1
```

Incorrect:

```text
st31.94 > st31.93, therefore release is newer
```

The upstream base is useful metadata, but it is not the product version.

## Channel Rules

### Stable Channel

Stable users should see stable releases only.

Visible:

```text
v1.0.0+st32.10
v1.0.1+st32.10
```

Hidden:

```text
v1.1.0-beta.1+st32.11
v1.1.0-alpha.1+st32.11
v1.0.0-rc.1+st32.10
```

### Beta Channel

Beta users should see beta, release candidate, and stable releases.

Visible:

```text
v0.4.1-beta.1+st31.93
v1.0.0-rc.1+st32.10
v1.0.0+st32.10
```

Hidden:

```text
v0.5.0-alpha.1+st31.96
```

### Alpha Channel

Alpha users may see alpha, beta, release candidate, and stable releases.

Visible:

```text
v0.5.0-alpha.1+st31.96
v0.4.1-beta.1+st31.93
v1.0.0-rc.1+st32.10
v1.0.0+st32.10
```

## Legacy Tags

The updater may encounter old tags such as:

```text
31.77-mobile-beta1
31.88-mobile-1.0
31.90-mobile-1.1
31.93-mobile-1.4
```

These should be treated as legacy SmarterTube releases.

During migration, the updater should either:

```text
Option A:
  Support legacy tags and allow users to update from them.

Option B:
  Detect legacy tags locally and direct users to a manual migration release.
```

Option A is preferred if it can be implemented cleanly.

## Legacy Mapping

If needed, hard-code a migration mapping for known old public releases.

Example:

```text
31.77-mobile-beta1 -> legacy beta
31.88-mobile-1.0   -> legacy beta-quality release, despite 1.0 label
31.90-mobile-1.1   -> legacy beta-quality release, despite 1.1 label
31.93-mobile-1.4   -> legacy beta-quality release, despite 1.4 label
```

A new release such as:

```text
v0.4.0-beta.1+st31.93
```

should be allowed to supersede old `31.xx-mobile-1.x` releases.

## Release Discovery

The updater should query SmarterTube releases, not upstream SmartTube releases.

Expected repository:

```text
CodeSculptor/SmarterTube
```

It should ignore releases from:

```text
yuliskov/SmartTube
```

unless intentionally using upstream data for display/debugging only.

## GitHub Prerelease Flag

GitHub releases have a prerelease flag.

The updater should not rely only on this flag.

It should check both:

```text
- parsed tag channel: alpha, beta, rc, stable
- GitHub prerelease flag
```

If they disagree, prefer the parsed SmarterTube tag and log or report the inconsistency.

Examples:

```text
v0.4.0-beta.1+st31.93 should be treated as beta even if GitHub prerelease is accidentally false.
v1.0.0+st32.10 should be treated as stable even if GitHub prerelease is accidentally true, but the release metadata should be fixed.
```

## APK Asset Selection

APK assets should be named consistently.

Recommended pattern:

```text
SmarterTube-v0.4.0-beta.1-st31.93-universal.apk
SmarterTube-v0.4.0-beta.1-st31.93-arm64-v8a.apk
SmarterTube-v0.4.0-beta.1-st31.93-armeabi-v7a.apk
SmarterTube-v0.4.0-beta.1-st31.93-x86.apk
```

The updater should select the best compatible asset.

Suggested priority:

```text
1. Exact ABI match
2. Universal APK
3. No update offered if no compatible asset exists
```

The updater must not select APKs for upstream SmartTube TV or another package.

## Release Metadata

If practical, include machine-readable metadata in each release.

This can be a JSON asset or a clearly delimited block in the release body.

Example:

```json
{
  "app": "SmarterTube",
  "package": "com.playpixelpro.audictube",
  "version": "0.4.0-beta.1",
  "upstream": "31.93",
  "channel": "beta",
  "min_android": 26,
  "assets": [
    {
      "abi": "universal",
      "name": "SmarterTube-v0.4.0-beta.1-st31.93-universal.apk"
    },
    {
      "abi": "arm64-v8a",
      "name": "SmarterTube-v0.4.0-beta.1-st31.93-arm64-v8a.apk"
    }
  ]
}
```

## Parser Requirements

The tag parser should support:

```text
v0.4.0-beta.1+st31.93
v0.4.0-beta.2+st31.93
v0.5.0-alpha.1+st31.96
v1.0.0-rc.1+st32.10
v1.0.0+st32.10
```

It should reject or ignore unrelated tags.

It should not crash on unexpected tags.

## Suggested Regex

A starting point for new-style tags:

```regex
^v(?<version>\d+\.\d+\.\d+(?:-(?<channel>alpha|beta|rc)\.\d+)?)\+st(?<upstream>\d+\.\d+(?:-[A-Za-z0-9.-]+)?)$
```

Notes:

```text
- Stable releases have no channel suffix.
- If no channel is present, treat the release as stable.
- The upstream part may include a suffix such as -beta if needed.
```

## Comparison Examples

These examples should become tests.

```text
Current: v0.4.0-beta.1+st31.93
Latest:  v0.4.0-beta.2+st31.93
Result:  update available
```

```text
Current: v0.4.1-beta.1+st31.93
Latest:  v0.4.0-beta.9+st31.94
Result:  no update available
Reason:  SmarterTube product version is older despite newer upstream base
```

```text
Current: v1.0.0+st32.10
Latest:  v1.1.0-beta.1+st32.11
Channel: stable
Result:  no update available
Reason:  stable channel ignores beta releases
```

```text
Current: v1.0.0-rc.1+st32.10
Latest:  v1.0.0+st32.10
Channel: beta
Result:  update available
```

```text
Current: 31.93-mobile-1.4
Latest:  v0.4.0-beta.1+st31.93
Channel: beta
Result:  update available or migration offered
```

## Failure Behaviour

The updater should fail safely.

If release parsing fails:

```text
[ ] Do not crash
[ ] Show a clear message if user-facing
[ ] Log the unparseable tag if logging exists
[ ] Ignore that release and continue checking others where possible
```

If no compatible APK exists:

```text
[ ] Do not download a random asset
[ ] Do not offer an incompatible update
[ ] Show a clear message if user-facing
```

If GitHub cannot be reached:

```text
[ ] Show a network/update-check failure message
[ ] Do not claim the app is up to date unless the check actually succeeded
```

## Acceptance Criteria

Before adopting the new release naming publicly:

```text
[ ] Existing update-checking implementation has been found and documented
[ ] New tag parser implemented
[ ] Legacy tag handling implemented or explicit migration path added
[ ] Channel filtering implemented
[ ] ABI asset selection verified
[ ] Release notes URL verified
[ ] Updater does not confuse SmartTube upstream releases with SmarterTube releases
[ ] Manual check from an old 31.xx-mobile-1.x build succeeds or gives clear migration guidance
[ ] Manual check from v0.4.0-beta.1+st31.93 succeeds
```

## Claude Code Task Prompt

Use this prompt when assigning the updater work:

```text
Before changing release tag naming, find the existing update-checking implementation and document exactly how it discovers, parses, compares, and downloads releases.

Then update it to support:
- new tags like v0.4.0-beta.1+st31.93
- legacy tags like 31.93-mobile-1.4 during migration
- alpha/beta/rc/stable channel filtering
- ABI-specific APK asset selection
- release notes links
- safe failure on unexpected tags or missing assets

Do not change release naming until updater tests pass.

After coding, provide:
- files changed
- parser behaviour
- comparison rules
- manual test steps
- risks
- release checklist rows affected
```

---

# As-Built Updater Audit (Gate A)

This section records how update-checking *actually* works in the repo as of the Gate A beta
reset, and what changed. It supersedes assumptions in the sections above where they differ.

## There are two independent update paths

### 1. Phone updater (fork-owned, the one users see on the phone)

- **Entry point:** `MobileAboutActivity` ("Check for updates" button) →
  `com.liskovsoft.smartyoutubetv2.mobile.update.MobileUpdateChecker`
  (`smarttubetv/src/stmobile/...`).
- **Source of release info:** the **GitHub Releases API** for `CodeSculptor/SmarterTube`
  (`https://api.github.com/repos/CodeSculptor/SmarterTube/releases`). It is repo-scoped to the
  fork, so it never sees upstream SmartTube TV releases. It does **not** use a JSON manifest.
- This path is fork-owned and lives in the `stmobile` flavor source set, so it can be changed
  freely without touching upstream code.

#### Before Gate A

- Queried `/releases/latest`, which **excludes prereleases** — so beta/rc releases were
  invisible and a beta user would never be told about a newer beta.
- Compared `tag_name.equals(BuildConfig.VERSION_NAME)` — a **whole-string** comparison. It
  could only answer "identical or not", never "newer or older", and broke under the new
  `v0.4.0-beta.1+st31.93` tag shape.
- No channel filtering, no ABI asset selection, no legacy handling.

#### After Gate A

- Queries the **list** endpoint (`/releases?per_page=30`) so prereleases are visible.
- Parses each `tag_name` with `SmarterTubeVersion`, which splits the tag into product version,
  channel, and upstream base, and **ignores** anything that isn't a SmarterTube release
  (upstream-only or malformed tags) instead of crashing.
- Orders by SmarterTube **product version** (channel-aware); upstream base (`+st…`) is metadata
  and never changes precedence. Legacy `31.xx-mobile-*` tags sort older than any `v0.x` release.
- Filters by the current build's **channel** (a beta build is not offered alpha releases).
- Selects the APK asset for the device ABI via `ApkAssetSelector`
  (exact ABI → universal → "no compatible asset", never a foreign/random APK).
- Cross-checks the parsed channel against GitHub's `prerelease` flag and logs a mismatch,
  trusting the parsed tag (per "GitHub Prerelease Flag" above).
- Fails safe: network/parse errors surface a clear message and offer the releases page; they
  never claim "up to date".

### 2. Upstream auto-checker (NOT fork-editable)

- **Code:** `SharedModules/appupdatechecker2` (`AppUpdateChecker` / `AppVersionChecker`), driven
  by `common`'s `AppUpdatePresenter`, kicked off at boot from `SplashPresenter` →
  `BootDialogPresenter`. `SharedModules` is an upstream **git submodule the fork must not edit**.
- **Source of release info:** a hardcoded JSON manifest from `R.array.update_urls`. The
  `stmobile` flavor declares no `update_urls.xml`, so via `matchingFallbacks=['ststable']` it
  reads the **upstream** manifest (`yuliskov/SmartTubeNext`'s `smarttube_stable2.json`).
- **Comparison:** integer `versionCode` only — picks the manifest's highest `versionCode` and
  compares to the installed `versionCode`.

#### Why it is inert on the phone (and why we did not repoint it)

The phone-port `versionCode` is deliberately in the 23xxx range (23711 for the beta reset),
far above upstream SmartTube's `versionCode` (~2383 for 31.93). The upstream checker therefore
always finds `installed (23711) > manifest-latest (~2383)` and reports "latest version" — it
**never offers an upstream TV APK** on the phone. Because the real phone path is the fork-owned
GitHub-API updater above, repointing `update_urls` to a fork manifest would add a second,
redundant release-info source (in a different format) to maintain at every release, for no user
benefit. So Gate A intentionally does **not** add a fork manifest or `common/src/stmobile/res/
values/update_urls.xml`.

If this ever needs neutralising (e.g. a future upstream `versionCode` climbs past the phone's),
the lever is to add `common/src/stmobile/res/values/update_urls.xml` pointing at a fork manifest
in the appupdatechecker2 format, or to gate the boot check off for `stmobile`.

## Migration behaviour (legacy tags)

`SmarterTubeVersion` recognises legacy `<upstream>-mobile-<suffix>` tags
(`31.77-mobile-beta1`, `31.88-mobile-1.0`, `31.90-mobile-1.1`, `31.93-mobile-1.4`) as **legacy
SmarterTube** releases, all treated as beta-channel and **older than any** `v0.x` beta-reset
release regardless of their old `1.x` label. A user still on a legacy build is offered the beta
reset; legacy tags never parse as upstream SmartTube and never crash the updater. This implements
**Option A** ("support legacy tags and allow users to update from them").

## APK asset selection & naming

Release APKs are named (build.gradle `applicationVariants`, `stmobile` only):

```text
SmarterTube-v0.4.0-beta.1-st31.93-universal.apk
SmarterTube-v0.4.0-beta.1-st31.93-arm64-v8a.apk
SmarterTube-v0.4.0-beta.1-st31.93-armeabi-v7a.apk
SmarterTube-v0.4.0-beta.1-st31.93-x86.apk
```

(`+st` becomes `-st` because `+` is not filename-safe.) `ApkAssetSelector` matches the `<arch>`
token in the GitHub asset name: exact ABI first, then universal, else a clear
`NO_COMPATIBLE_ASSET` state. The ABI splits produced are `armeabi-v7a`, `arm64-v8a`, `x86`, plus
`universal` (see the `splits { abi { … } }` block).

## Channel configuration

There is no channel-selection UI. The current build's channel is parsed from its own
`versionName` (e.g. `v0.4.0-beta.1+st31.93` → beta), defaulting to **beta** while the app is
pre-1.0. A future channel toggle would live in the phone settings screen and feed the
`userChannel` argument of `MobileUpdateChecker.selectFrom` / `channelOf`.

## Tests

`smarttubetv/src/testStmobile/.../update/`:
`SmarterTubeVersionTest`, `ApkAssetSelectorTest`, `MobileUpdateCheckerTest` (the pure
`selectFrom` selection logic, including legacy migration, channel filtering, ABI selection,
prerelease handling, and tolerant JSON parsing). Run with
`gradlew :smarttubetv:testStmobileDebugUnitTest`.
