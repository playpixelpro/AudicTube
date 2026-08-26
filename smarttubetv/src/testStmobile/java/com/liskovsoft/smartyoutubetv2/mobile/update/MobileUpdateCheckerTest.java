package com.liskovsoft.smartyoutubetv2.mobile.update;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.liskovsoft.smartyoutubetv2.mobile.update.MobileUpdateChecker.Asset;
import com.liskovsoft.smartyoutubetv2.mobile.update.MobileUpdateChecker.ReleaseInfo;
import com.liskovsoft.smartyoutubetv2.mobile.update.MobileUpdateChecker.Result;
import com.liskovsoft.smartyoutubetv2.mobile.update.MobileUpdateChecker.Status;
import com.liskovsoft.smartyoutubetv2.mobile.update.SmarterTubeVersion.Channel;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Unit tests for the pure {@link MobileUpdateChecker#selectFrom} selection logic — release
 * discovery/ordering/channel/ABI rules from {@code docs/UPDATER_COMPATIBILITY.md}, exercised
 * without any network. Run with: gradlew :smarttubetv:testStmobileDebugUnitTest
 */
public class MobileUpdateCheckerTest {
    private static final String ABI = "arm64-v8a";

    @Test
    public void offersNewerBetaWithMatchingAbi() {
        List<ReleaseInfo> releases = Arrays.asList(
                release("v1.0.0", true),
                release("v1.1.0-beta.1", true));
        Result r = MobileUpdateChecker.selectFrom(releases, "v1.0.0", ABI, Channel.BETA);
        assertEquals(Status.UPDATE_AVAILABLE, r.status);
        assertEquals("v1.1.0-beta.1", r.latestTag);
        assertNotNull(r.assetUrl);
        assertTrue(r.assetUrl.contains("arm64-v8a"));
    }

    @Test
    public void reportsUpToDateWhenCurrentIsLatest() {
        List<ReleaseInfo> releases = Collections.singletonList(release("v1.0.0", true));
        Result r = MobileUpdateChecker.selectFrom(releases, "v1.0.0", ABI, Channel.BETA);
        assertEquals(Status.UP_TO_DATE, r.status);
    }

    @Test
    public void betaUserDoesNotSeeAlpha() {
        List<ReleaseInfo> releases = Arrays.asList(
                release("v1.0.0", true),
                release("v1.2.0-alpha.1", true));
        Result r = MobileUpdateChecker.selectFrom(releases, "v1.0.0", ABI, Channel.BETA);
        // The only newer release is an alpha, hidden from beta users -> nothing to offer.
        assertEquals(Status.UP_TO_DATE, r.status);
    }

    @Test
    public void alphaUserSeesAlpha() {
        List<ReleaseInfo> releases = Arrays.asList(
                release("v1.0.0", true),
                release("v1.2.0-alpha.1", true));
        Result r = MobileUpdateChecker.selectFrom(releases, "v1.0.0", ABI, Channel.ALPHA);
        assertEquals(Status.UPDATE_AVAILABLE, r.status);
        assertEquals("v1.2.0-alpha.1", r.latestTag);
    }

    @Test
    public void ignoresMalformedTags() {
        List<ReleaseInfo> releases = Arrays.asList(
                release("not-a-release", false),  // malformed
                release("v1.1.0-beta.1", true));
        Result r = MobileUpdateChecker.selectFrom(releases, "v1.0.0", ABI, Channel.BETA);
        assertEquals(Status.UPDATE_AVAILABLE, r.status);
        assertEquals("v1.1.0-beta.1", r.latestTag);
    }

    @Test
    public void noUpdateWhenProductVersionOlder() {
        List<ReleaseInfo> releases = Collections.singletonList(release("v1.0.0-beta.9", true));
        Result r = MobileUpdateChecker.selectFrom(releases, "v1.1.0-beta.1", ABI, Channel.BETA);
        assertEquals(Status.UP_TO_DATE, r.status);
    }

    @Test
    public void noCompatibleAssetWhenAbiMissing() {
        ReleaseInfo onlyX86 = new ReleaseInfo("v1.1.0-beta.1", true,
                "https://github.com/playpixelpro/AudicTube/releases/tag/x",
                Collections.singletonList(new Asset("AudicTube-v1.1.0-beta.1-x86.apk", "http://x/x86.apk")));
        Result r = MobileUpdateChecker.selectFrom(
                Collections.singletonList(onlyX86), "v1.0.0", ABI, Channel.BETA);
        assertEquals(Status.NO_COMPATIBLE_ASSET, r.status);
        assertNotNull(r.releaseUrl);
    }

    @Test
    public void parsesGitHubReleasesJson() {
        String json = "[{\"tag_name\":\"v1.1.0-beta.1\",\"prerelease\":true,"
                + "\"html_url\":\"https://github.com/playpixelpro/AudicTube/releases/tag/v1.1.0-beta.1\","
                + "\"assets\":[{\"name\":\"AudicTube-v1.1.0-beta.1-arm64-v8a.apk\","
                + "\"browser_download_url\":\"http://x/arm64.apk\"}]}]";
        List<ReleaseInfo> releases = MobileUpdateChecker.parseReleases(json);
        assertEquals(1, releases.size());
        assertEquals("v1.1.0-beta.1", releases.get(0).tag);
        assertEquals(1, releases.get(0).assets.size());
    }

    @Test
    public void parseReleasesToleratesGarbage() {
        // A GitHub error object (not an array) must yield no releases, not a crash.
        assertTrue(MobileUpdateChecker.parseReleases("{\"message\":\"Not Found\"}").isEmpty());
        assertTrue(MobileUpdateChecker.parseReleases(null).isEmpty());
        assertTrue(MobileUpdateChecker.parseReleases("garbage").isEmpty());
    }

    // ---- helpers ----

    /** A release carrying universal + arm64 + armeabi APK assets named per the doc convention. */
    private static ReleaseInfo release(String tag, boolean prerelease) {
        String base = "AudicTube-" + tag;
        List<Asset> assets = new ArrayList<>();
        assets.add(new Asset(base + "-universal.apk", "http://x/" + tag + "/universal.apk"));
        assets.add(new Asset(base + "-arm64-v8a.apk", "http://x/" + tag + "/arm64-v8a.apk"));
        assets.add(new Asset(base + "-armeabi-v7a.apk", "http://x/" + tag + "/armeabi-v7a.apk"));
        return new ReleaseInfo(tag, prerelease,
                "https://github.com/playpixelpro/AudicTube/releases/tag/" + tag, assets);
    }
}
