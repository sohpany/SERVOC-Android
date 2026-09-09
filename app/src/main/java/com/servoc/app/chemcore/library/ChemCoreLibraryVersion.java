package com.servoc.app.chemcore.library;

import java.util.Locale;

/** Small dependency-free semantic-version helper for independently versioned ChemCore libraries. */
public final class ChemCoreLibraryVersion implements Comparable<ChemCoreLibraryVersion> {
    public final int major;
    public final int minor;
    public final int patch;
    public final String prerelease;

    public ChemCoreLibraryVersion(int major, int minor, int patch, String prerelease) {
        if (major < 0 || minor < 0 || patch < 0) throw new IllegalArgumentException("negative version component");
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.prerelease = prerelease == null ? "" : prerelease.trim();
    }

    public static ChemCoreLibraryVersion parse(String raw) {
        if (raw == null) throw new IllegalArgumentException("version required");
        String s = raw.trim();
        if (s.startsWith("v") || s.startsWith("V")) s = s.substring(1);
        int plus = s.indexOf('+');
        if (plus >= 0) s = s.substring(0, plus); // build metadata does not affect ordering
        String pre = "";
        int dash = s.indexOf('-');
        if (dash >= 0) {
            pre = s.substring(dash + 1);
            s = s.substring(0, dash);
        }
        String[] p = s.split("\\.");
        if (p.length < 1 || p.length > 3) throw new IllegalArgumentException("invalid semantic version: " + raw);
        int a = integer(p[0], raw), b = p.length > 1 ? integer(p[1], raw) : 0, c = p.length > 2 ? integer(p[2], raw) : 0;
        return new ChemCoreLibraryVersion(a, b, c, pre);
    }

    private static int integer(String s, String raw) {
        try { return Integer.parseInt(s); }
        catch (Exception e) { throw new IllegalArgumentException("invalid semantic version: " + raw, e); }
    }

    @Override public int compareTo(ChemCoreLibraryVersion o) {
        if (o == null) return 1;
        int x = Integer.compare(major, o.major); if (x != 0) return x;
        x = Integer.compare(minor, o.minor); if (x != 0) return x;
        x = Integer.compare(patch, o.patch); if (x != 0) return x;
        if (prerelease.isEmpty() && o.prerelease.isEmpty()) return 0;
        if (prerelease.isEmpty()) return 1; // final > prerelease
        if (o.prerelease.isEmpty()) return -1;
        return comparePrerelease(prerelease, o.prerelease);
    }

    private static int comparePrerelease(String a, String b) {
        String[] x = a.split("\\."), y = b.split("\\.");
        int n = Math.max(x.length, y.length);
        for (int i = 0; i < n; i++) {
            if (i >= x.length) return -1;
            if (i >= y.length) return 1;
            boolean xn = x[i].matches("\\d+"), yn = y[i].matches("\\d+");
            int c;
            if (xn && yn) c = Integer.compare(Integer.parseInt(x[i]), Integer.parseInt(y[i]));
            else if (xn) c = -1;
            else if (yn) c = 1;
            else c = x[i].compareToIgnoreCase(y[i]);
            if (c != 0) return c;
        }
        return 0;
    }

    public boolean isNewerThan(String other) { return compareTo(parse(other)) > 0; }

    @Override public String toString() {
        return String.format(Locale.US, "%d.%d.%d%s", major, minor, patch, prerelease.isEmpty() ? "" : "-" + prerelease);
    }
}
