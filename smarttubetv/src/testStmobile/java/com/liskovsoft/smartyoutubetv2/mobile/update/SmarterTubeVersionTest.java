package com.liskovsoft.smartyoutubetv2.mobile.update;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.liskovsoft.smartyoutubetv2.mobile.update.SmarterTubeVersion.Channel;

import org.junit.Test;

/**
 * Unit tests for {@link SmarterTubeVersion} — the parsing/comparison contract from
 * {@code docs/UPDATER_COMPATIBILITY.md}. Pure JUnit, no Android needed.
 *
 * Run with: gradlew :smarttubetv:testStmobileDebugUnitTest
 */
public class SmarterTubeVersionTest {

    // ---- Parsing ----

    @Test
    public void parsesNewSchemeBeta() {
        SmarterTubeVersion v = SmarterTubeVersion.parse("v1.0.0-beta.1");
        assertEquals(1, v.getMajor());
        assertEquals(0, v.getMinor());
        assertEquals(0, v.getPatch());
        assertEquals(Channel.BETA, v.getChannel());
        assertEquals(1, v.getChannelNumber());
        assertNull(v.getUpstreamBase());
        assertFalse(v.isLegacy());
    }

    @Test
    public void parsesNewSchemeBeta2() {
        SmarterTubeVersion v = SmarterTubeVersion.parse("v1.0.0-beta.2");
        assertEquals(2, v.getChannelNumber());
        assertEquals(Channel.BETA, v.getChannel());
    }

    @Test
    public void parsesDifferentVersion() {
        SmarterTubeVersion v = SmarterTubeVersion.parse("v1.1.0-beta.1");
        assertEquals("1.1.0", v.getProductVersion());
    }

    @Test
    public void parsesReleaseCandidate() {
        SmarterTubeVersion v = SmarterTubeVersion.parse("v1.0.0-rc.1");
        assertEquals(Channel.RC, v.getChannel());
        assertEquals(1, v.getChannelNumber());
    }

    @Test
    public void parsesStableWithNoChannelSuffix() {
        SmarterTubeVersion v = SmarterTubeVersion.parse("v1.0.0");
        assertEquals(Channel.STABLE, v.getChannel());
        assertEquals(0, v.getChannelNumber());
        assertFalse(v.isLegacy());
    }

    @Test
    public void parsesUpstreamBetaTail() {
        // +st suffix is optional metadata — should still parse cleanly.
        SmarterTubeVersion v = SmarterTubeVersion.parse("v1.0.0-beta.1+st31.94-beta");
        assertEquals("1.0.0", v.getProductVersion());
        assertEquals(Channel.BETA, v.getChannel());
    }

    // ---- Ordering ----

    @Test
    public void alphaIsOlderThanBeta() {
        assertTrue(lt("v1.1.0-alpha.1", "v1.1.0-beta.1"));
    }

    @Test
    public void betaIsOlderThanRc() {
        assertTrue(lt("v1.0.0-beta.1", "v1.0.0-rc.1"));
    }

    @Test
    public void rcIsOlderThanStable() {
        assertTrue(lt("v1.0.0-rc.1", "v1.0.0"));
    }

    @Test
    public void beta2IsNewerThanBeta1() {
        assertTrue(lt("v1.0.0-beta.1", "v1.0.0-beta.2"));
    }

    @Test
    public void patchBumpIsNewer() {
        assertTrue(lt("v1.0.0-beta.1", "v1.1.0-beta.1"));
    }

    @Test
    public void majorBumpIsNewer() {
        assertTrue(lt("v1.0.0", "v2.0.0"));
    }

    // ---- Safe handling of unrelated tags ----

    @Test
    public void upstreamTvTagIsIgnored() {
        // Plain upstream SmartTube version, not a SmarterTube release.
        assertNull(SmarterTubeVersion.parse("31.93"));
        assertNull(SmarterTubeVersion.parse("v31.93-beta"));
    }

    @Test
    public void malformedTagsAreIgnored() {
        assertNull(SmarterTubeVersion.parse(null));
        assertNull(SmarterTubeVersion.parse(""));
        assertNull(SmarterTubeVersion.parse("   "));
        assertNull(SmarterTubeVersion.parse("not-a-version"));
        assertNull(SmarterTubeVersion.parse("v0.4-beta.1")); // missing patch
        assertNull(SmarterTubeVersion.parse("0.4.0-beta.1+st31.93")); // missing leading v
    }

    // ---- Channel visibility ----

    @Test
    public void stableChannelSeesStableOnly() {
        assertTrue(visible("v1.0.0", Channel.STABLE));
        assertFalse(visible("v1.0.0-rc.1", Channel.STABLE));
        assertFalse(visible("v1.0.0-beta.1", Channel.STABLE));
        assertFalse(visible("v1.1.0-alpha.1", Channel.STABLE));
    }

    @Test
    public void betaChannelSeesBetaRcStable() {
        assertTrue(visible("v1.0.0-beta.1", Channel.BETA));
        assertTrue(visible("v1.0.0-rc.1", Channel.BETA));
        assertTrue(visible("v1.0.0", Channel.BETA));
        assertFalse(visible("v1.1.0-alpha.1", Channel.BETA));
    }

    @Test
    public void alphaChannelSeesEverything() {
        assertTrue(visible("v1.1.0-alpha.1", Channel.ALPHA));
        assertTrue(visible("v1.0.0-beta.1", Channel.ALPHA));
        assertTrue(visible("v1.0.0-rc.1", Channel.ALPHA));
        assertTrue(visible("v1.0.0", Channel.ALPHA));
    }

    // ---- helpers ----

    private static boolean lt(String older, String newer) {
        return SmarterTubeVersion.parse(older).compareTo(SmarterTubeVersion.parse(newer)) < 0;
    }

    private static boolean visible(String tag, Channel userChannel) {
        return SmarterTubeVersion.parse(tag).isVisibleTo(userChannel);
    }
}
