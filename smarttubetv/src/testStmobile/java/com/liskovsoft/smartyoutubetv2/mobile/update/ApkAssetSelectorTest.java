package com.liskovsoft.smartyoutubetv2.mobile.update;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Unit tests for {@link ApkAssetSelector}. Run with:
 * gradlew :smarttubetv:testStmobileDebugUnitTest
 */
public class ApkAssetSelectorTest {

    private static final List<String> FULL_SET = Arrays.asList(
            "AudicTube-v0.4.0-beta.1-st31.93-universal.apk",
            "AudicTube-v0.4.0-beta.1-st31.93-arm64-v8a.apk",
            "AudicTube-v0.4.0-beta.1-st31.93-armeabi-v7a.apk",
            "AudicTube-v0.4.0-beta.1-st31.93-x86.apk");

    @Test
    public void picksExactAbiWhenPresent() {
        assertEquals("SmarterTube-v0.4.0-beta.1-st31.93-arm64-v8a.apk",
                ApkAssetSelector.select(FULL_SET, "arm64-v8a"));
        assertEquals("SmarterTube-v0.4.0-beta.1-st31.93-armeabi-v7a.apk",
                ApkAssetSelector.select(FULL_SET, "armeabi-v7a"));
        assertEquals("SmarterTube-v0.4.0-beta.1-st31.93-x86.apk",
                ApkAssetSelector.select(FULL_SET, "x86"));
    }

    @Test
    public void fallsBackToUniversalWhenNoExactAbi() {
        List<String> noArm64 = Arrays.asList(
                "SmarterTube-v0.4.0-beta.1-st31.93-universal.apk",
                "SmarterTube-v0.4.0-beta.1-st31.93-armeabi-v7a.apk");
        assertEquals("SmarterTube-v0.4.0-beta.1-st31.93-universal.apk",
                ApkAssetSelector.select(noArm64, "arm64-v8a"));
    }

    @Test
    public void prefersExactAbiOverUniversal() {
        // Universal is listed first, but the exact ABI must still win.
        assertEquals("SmarterTube-v0.4.0-beta.1-st31.93-arm64-v8a.apk",
                ApkAssetSelector.select(FULL_SET, "arm64-v8a"));
    }

    @Test
    public void returnsNullWhenNoCompatibleAsset() {
        List<String> onlyX86 = Collections.singletonList(
                "SmarterTube-v0.4.0-beta.1-st31.93-x86.apk");
        assertNull(ApkAssetSelector.select(onlyX86, "arm64-v8a"));
    }

    @Test
    public void returnsNullForEmptyOrNullInput() {
        assertNull(ApkAssetSelector.select(null, "arm64-v8a"));
        assertNull(ApkAssetSelector.select(Collections.emptyList(), "arm64-v8a"));
    }

    @Test
    public void ignoresNonApkEntries() {
        List<String> withNoise = Arrays.asList(
                "SmarterTube-v0.4.0-beta.1-st31.93.json",
                "checksums.txt",
                "SmarterTube-v0.4.0-beta.1-st31.93-arm64-v8a.apk");
        assertEquals("SmarterTube-v0.4.0-beta.1-st31.93-arm64-v8a.apk",
                ApkAssetSelector.select(withNoise, "arm64-v8a"));
    }

    @Test
    public void x86_64DoesNotMatchX86() {
        assertEquals("x86_64", ApkAssetSelector.abiOf("SmarterTube-v0.4.0-beta.1-st31.93-x86_64.apk"));
        assertEquals("x86", ApkAssetSelector.abiOf("SmarterTube-v0.4.0-beta.1-st31.93-x86.apk"));
    }
}
