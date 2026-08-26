# Upstream merge workflow

This fork tracks [yuliskov/SmartTube](https://github.com/yuliskov/SmartTube). The YouTube client engine — `MediaServiceCore`, ExoPlayer, the InnerTube layer — is upstream's code. The phone UI under `smarttubetv/src/stmobile/` is the only fork-specific source set. Keeping the two in sync is mostly mechanical and mostly automated.

## The two-tier model

| Tier | What happens | Where |
|---|---|---|
| Detect & propose | Daily check for new upstream commits; opens a PR (clean merge) or issue (conflicts) | [`.github/workflows/upstream-sync.yml`](../.github/workflows/upstream-sync.yml) |
| Validate | Confirms the 3 integration points are intact and `stmobile` still builds | [`.github/workflows/stmobile-validate.yml`](../.github/workflows/stmobile-validate.yml) |
| Merge | **Clean PRs auto-merge once `stmobile-validate` is green.** Conflicts, or a failed build, hold the merge and open an issue for a human | `upstream-sync.yml` + branch protection |
| Ship | Human cuts a release after a batch of merges (version bump, signed APKs) | This document |

> **Auto-merge:** clean upstream changes that pass the phone-app build check now merge themselves —
> no action needed. A merge that touches an integration point or breaks the build fails the check,
> is **not** merged, and surfaces as an issue you're emailed about. So "hands-off until something
> actually needs me" is the default.

## Staying informed when you're not actively developing

You don't have to watch this repo to stay in sync:

- **Conflicts / failed builds** open an issue here → GitHub emails you (you own the repo).
- **Every upstream release:** click **Watch → Custom → Releases** on
  [yuliskov/SmartTube](https://github.com/yuliskov/SmartTube) to get an email whenever upstream
  ships a release — your cue to start a session if you want to cut a matching fork release.

Clean day-to-day upstream commits just auto-merge in the background; no notification needed.

## The 3 integration points

These are the only places where the fork touches upstream code. Every upstream merge has the potential to disturb them; `stmobile-validate.yml` greps for each one.

1. **`smarttubetv/build.gradle`** — `stmobile` product flavor block. Sets an explicit `applicationId "com.playpixelpro.audictube"` (NOT a suffix on upstream's `app.smarttube`) and `matchingFallbacks ['ststable']`. If a merge removes the flavor block or changes the id, the APK ships under the wrong package ID. (Renamed from the legacy `app.smarttube.mobile` in the 2026-06 beta — a one-time clean break; the old id does not upgrade in place.)

2. **`common/build.gradle`** — matching `stmobile` flavor block with `matchingFallbacks ['ststable']` so submodules that don't know about `stmobile` fall back to `ststable`. Without this the build fails resolving common-module dependencies.

3. **`MainApplication.setupViewManager()`** at `smarttubetv/src/main/java/com/liskovsoft/smartyoutubetv2/tv/ui/main/MainApplication.java` — must be `protected`, not `private`. `MobileApplication` overrides it to wire the phone activities into the view-manager. If upstream re-narrows it the override is silently inert.

## What clean merges look like

Most upstream changes touch `common/`, `MediaServiceCore/`, or the leanback UI under `smarttubetv/src/main/`. None of those overlap with our `stmobile` source set, so they merge clean. The bot opens a PR like:

```
Upstream sync 2026-06-15 — clean merge
```

with the upstream commit list in the body. Review:

1. Read the upstream commit list — anything that looks like a UI/UX or playback change is worth eyeballing.
2. Let CI run (`stmobile-validate` builds the APK).
3. Side-load the debug APK and smoke-test: home loads, search works, playback works, sign-in works.
4. Merge and tag a new fork release (see [When to bump the version](#when-to-bump-the-version)).

## What conflict merges look like

The bot opens an issue tagged `upstream-sync, conflict` listing the conflicting paths. Typical causes:

- Upstream re-shaped the `productFlavors` block → resolve in `smarttubetv/build.gradle`, keep our `stmobile { ... }`.
- Upstream added a new method to `BrowseView` / `SignInView` / `PlaybackView` → implement it (usually as a no-op) in the corresponding `Mobile*` class. See the rebase runbook for the existing implementations (`updateBadge()`, the 3-arg `showCode(...)`, etc.).
- Upstream renamed an activity referenced from the stmobile manifest → update the manifest.

Manual resolution:

```bash
git fetch upstream master
git checkout -b upstream-sync/manual-YYYY-MM-DD master
git merge upstream/master
# resolve conflicts
git commit
git push origin HEAD
gh pr create --base master --title "Upstream sync — manual merge"
```

Then let `stmobile-validate` run, smoke-test, merge.

## What to do when validate fails

If `stmobile-validate` flags a missing integration point on a PR:

- **Point 1 missing** → restore the `stmobile { ... }` block in `smarttubetv/build.gradle` with `applicationId "com.playpixelpro.audictube"`, `matchingFallbacks ['ststable']`, the `targetSdkVersion`, and the `versionCode`/`versionName` lines (increment the fork `versionCode` monotonically; see [When to bump the version](#when-to-bump-the-version)).
- **Point 2 missing** → restore the `stmobile { matchingFallbacks ['ststable'] }` block in `common/build.gradle`.
- **Point 3 missing** → widen `MainApplication.setupViewManager()` from `private` back to `protected`.

If the build step fails:

- Class-not-found / method-not-found in `Mobile*` files → upstream changed an interface; implement the new signature in the corresponding `Mobile*` class.
- Manifest-merger error → check if a TV-only activity referenced from the stmobile manifest got renamed or removed upstream.

## When to bump the version

AudicTube uses pure **Semantic Versioning** (semver, [conventionalcommits.org](https://www.conventionalcommits.org/en/v1.0.0/)) for the phone build. The upstream SmartTube version is **not** part of the product version anymore — it's recorded in release notes and the `Merge upstream SmartTube X.YY (sha)` git history instead.

Convention:

- `versionName` — follows semver, e.g. `v1.0.0`, `v1.1.0`, `v1.0.1`, or prereleases `v1.1.0-beta.1` / `v1.2.0-rc.1`. Bump:
  - **MAJOR** for breaking changes,
  - **MINOR** for user-visible features,
  - **PATCH** for bug fixes.
  - Defaults to `v1.0.0`; override with `-PversionName=2.0.0` (the `v` prefix is added automatically if missing).
- `versionCode` — must be **strictly increasing** across releases (Android requires this for in-place upgrades). Defaults to `1`; override with `-PversionCode=N`. Increment by 1 for each release.
- Bump in the `stmobile` flavor block of `smarttubetv/build.gradle` only — never in `defaultConfig` (that's upstream's).

After a clean merge and bump: `assembleStmobileRelease` (or push a `v*` tag to let the CI workflow build it), then publish the release. The GitHub Actions workflow (`build-mobile-release.yml`) builds the signed APKs and creates the GitHub Release automatically. Use `--prerelease` only for prerelease versions (`-beta`, `-rc`); drop it for stable `1.x` releases.
