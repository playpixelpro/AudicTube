# SmarterTube Release Checklist

Run this before every public beta/stable release. Tick each box on a real device (and a tablet
where layout rows apply). Anything that cannot be ticked must be recorded in
[KNOWN_ISSUES.md](KNOWN_ISSUES.md) and reflected in [FEATURE_MATRIX.md](FEATURE_MATRIX.md).

Release under test: `__________________________`  (e.g. `v0.4.0-beta.1+st31.93`)
Upstream SmartTube base: `__________`  Channel: `__________`

## Install and upgrade

- [ ] Fresh install works (no prior version present)
- [ ] Upgrade from the previous SmarterTube release works (in-place, data preserved)
- [ ] App ID is correct (`com.playpixelpro.audictube`) and co-installs with the upstream TV build
- [ ] ABI-specific APK installs correctly (arm64-v8a / armeabi-v7a / x86)
- [ ] Universal APK installs correctly

## Update checking

- [ ] "Check for updates" opens without crashing
- [ ] Current release is **not** incorrectly reported as outdated
- [ ] A newer compatible release is detected and offered
- [ ] An older release is ignored (no downgrade offered)
- [ ] An incompatible upstream SmartTube TV release is ignored
- [ ] The correct APK asset is selected for the device ABI
- [ ] The GitHub release URL / release notes link opens correctly
- [ ] Network failure shows a clear message and does **not** claim "up to date"

## Signed-out browsing

- [ ] Home opens
- [ ] Search opens; suggestions work (if implemented)
- [ ] Video playback works
- [ ] Shorts playback works
- [ ] Channel page opens; channel uploads open
- [ ] Settings opens; About opens

## Signed-in browsing

- [ ] Sign-in works; sign-out works
- [ ] Account switcher works (if implemented)
- [ ] Subscriptions, History, Playlists open
- [ ] Notifications open or show a sane "unavailable" state
- [ ] Subscribe/unsubscribe works (if implemented)

## Player controls

- [ ] Play / pause
- [ ] Seek
- [ ] Back behaviour; resume behaviour
- [ ] Fullscreen / orientation handling
- [ ] Quality menu; captions; playback speed
- [ ] SponsorBlock controls; Return YouTube Dislike; DeArrow (where supported)

## Settings

- [ ] Settings screen opens and scrolls
- [ ] Each visible setting applies and persists across restart
- [ ] No TV-only setting is dumped into the phone UI without adaptation

## Channel pages & search

- [ ] Channel page header, tabs, and uploads render and scroll
- [ ] Search returns results; opening a result plays/loads correctly
- [ ] Shorts surface and play

## Layout and orientation

- [ ] Phone portrait
- [ ] Phone landscape
- [ ] Tablet portrait
- [ ] Tablet landscape
- [ ] No TV/landscape-only UI is dumped into the portrait UI without adaptation
- [ ] Important controls are reachable by touch; important text is readable

## Upstream merge smoke test

- [ ] App launches after the upstream merge used as this release's base
- [ ] Core playback + browse still work
- [ ] "Check for updates" still works after the merge

## Release notes

- [ ] Release notes include the **SmarterTube version** and the **upstream SmartTube base**
- [ ] Release channel stated; main purpose of the release stated
- [ ] Known issues updated; feature matrix updated
- [ ] APK asset names match the updater's expectations (`SmarterTube-<ver>-st<up>-<arch>.apk`)
