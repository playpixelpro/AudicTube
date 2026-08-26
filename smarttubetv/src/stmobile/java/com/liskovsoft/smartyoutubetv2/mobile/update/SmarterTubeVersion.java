package com.liskovsoft.smartyoutubetv2.mobile.update;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses and compares AudicTube release identifiers.
 *
 * <p>Format (pure semver with optional pre-release):
 * <pre>v&lt;major&gt;.&lt;minor&gt;.&lt;patch&gt;[-&lt;channel&gt;.&lt;n&gt;]</pre>
 * e.g. {@code v1.0.0}, {@code v1.1.0-beta.1}.
 *
 * <p>A {@code +st} upstream suffix may also appear for legacy compatibility but does not affect
 * ordering (treated as build metadata per semver).
 *
 * <p>{@link #parse(String)} returns {@code null} for anything that is not a recognised release
 * identifier or is malformed, so callers can safely ignore it rather than crash.
 *
 * <p>Pure Java, no Android dependencies — unit-testable in isolation.
 */
public final class SmarterTubeVersion implements Comparable<SmarterTubeVersion> {
    /** Release channel, ordered least to most stable: alpha &lt; beta &lt; rc &lt; stable. */
    public enum Channel {
        ALPHA(0, "alpha"),
        BETA(1, "beta"),
        RC(2, "rc"),
        STABLE(3, "stable");

        public final int rank;
        public final String id;

        Channel(int rank, String id) {
            this.rank = rank;
            this.id = id;
        }

        public static Channel fromId(String id) {
            if (id != null) {
                for (Channel c : values()) {
                    if (c.id.equalsIgnoreCase(id)) {
                        return c;
                    }
                }
            }
            return STABLE;
        }
    }

    // v1.0.0[-beta.1] — pure semver with optional pre-release channel; +st suffix is optional metadata.
    private static final Pattern NEW_SCHEME = Pattern.compile(
            "^v(\\d+)\\.(\\d+)\\.(\\d+)(?:-(alpha|beta|rc)\\.(\\d+))?(?:\\+st[\\w.-]+)?$");

    private final int major;
    private final int minor;
    private final int patch;
    private final Channel channel;
    private final int channelNumber; // N in "beta.N"; 0 for stable / legacy
    private final String upstreamBase; // e.g. "31.93" — metadata only, never compared
    private final boolean legacy;
    private final String raw;

    private SmarterTubeVersion(int major, int minor, int patch, Channel channel, int channelNumber,
                              String upstreamBase, boolean legacy, String raw) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.channel = channel;
        this.channelNumber = channelNumber;
        this.upstreamBase = upstreamBase;
        this.legacy = legacy;
        this.raw = raw;
    }

    /**
     * @return a parsed version, or {@code null} if {@code tag} is not a recognisable SmarterTube
     *         release (upstream-only or malformed tags return null and must be ignored).
     */
    public static SmarterTubeVersion parse(String tag) {
        if (tag == null) {
            return null;
        }
        String t = tag.trim();
        if (t.isEmpty()) {
            return null;
        }

        Matcher m = NEW_SCHEME.matcher(t);
        if (m.matches()) {
            int major = Integer.parseInt(m.group(1));
            int minor = Integer.parseInt(m.group(2));
            int patch = Integer.parseInt(m.group(3));
            String channelId = m.group(4); // null => stable
            Channel channel = channelId == null ? Channel.STABLE : Channel.fromId(channelId);
            int channelNumber = channelId == null ? 0 : Integer.parseInt(m.group(5));
            return new SmarterTubeVersion(major, minor, patch, channel, channelNumber, null, false, t);
        }

        return null; // not a recognised release / malformed -> ignore safely
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public int getPatch() {
        return patch;
    }

    public Channel getChannel() {
        return channel;
    }

    public int getChannelNumber() {
        return channelNumber;
    }

    /** Upstream SmartTube base, e.g. {@code "31.93"}. Metadata only — for display/diagnostics. */
    public String getUpstreamBase() {
        return upstreamBase;
    }

    public boolean isLegacy() {
        return legacy;
    }

    public String getRaw() {
        return raw;
    }

    /** Product version without channel/upstream, e.g. {@code "0.4.0"}. */
    public String getProductVersion() {
        return major + "." + minor + "." + patch;
    }

    /**
     * Channel visibility: a user on {@code userChannel} sees this release if it is at least as
     * stable as their channel allows.
     * <ul>
     *   <li>stable: stable only</li>
     *   <li>beta: beta, rc, stable</li>
     *   <li>rc: rc, stable</li>
     *   <li>alpha: everything</li>
     * </ul>
     */
    public boolean isVisibleTo(Channel userChannel) {
        if (userChannel == null) {
            return false;
        }
        switch (userChannel) {
            case STABLE:
                return channel == Channel.STABLE;
            case RC:
                return channel.rank >= Channel.RC.rank;
            case BETA:
                return channel.rank >= Channel.BETA.rank;
            case ALPHA:
            default:
                return true;
        }
    }

    @Override
    public int compareTo(SmarterTubeVersion o) {
        int c = Integer.compare(major, o.major);
        if (c != 0) {
            return c;
        }
        c = Integer.compare(minor, o.minor);
        if (c != 0) {
            return c;
        }
        c = Integer.compare(patch, o.patch);
        if (c != 0) {
            return c;
        }
        c = Integer.compare(channel.rank, o.channel.rank);
        if (c != 0) {
            return c;
        }
        return Integer.compare(channelNumber, o.channelNumber);
    }

    @Override
    public String toString() {
        return raw;
    }
}
