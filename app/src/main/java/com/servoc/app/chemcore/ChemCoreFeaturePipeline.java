package com.servoc.app.chemcore;

import java.util.Arrays;

/**
 * Canonical ChemCore time-domain feature contract for cross-reactive MQ traces.
 *
 * Input shape is samples x sensors and each value is Rs/R0 (>0). The pipeline converts
 * valid samples to log10(Rs/R0), preserves the sign of the chemical response and emits
 * a stable, versioned feature vector. New ChemCore reference/training/runtime paths must
 * use this contract instead of inventing local fingerprint definitions.
 */
public final class ChemCoreFeaturePipeline {
    private ChemCoreFeaturePipeline() {}

    public static final String SCHEMA_VERSION = "chemcore-features/1";
    public static final int FEATURES_PER_SENSOR = 17;

    public static final String[] FEATURE_NAMES = {
            "valid_fraction",
            "baseline_log10_ratio",
            "steady_log10_ratio",
            "signed_delta_log10",
            "abs_delta_log10",
            "min_log10_ratio",
            "max_log10_ratio",
            "peak_log10_ratio",
            "initial_slope_log10_per_s",
            "stability_slope_log10_per_s",
            "t10_s",
            "t50_s",
            "t90_s",
            "signed_auc_log10_s",
            "abs_auc_log10_s",
            "baseline_noise_sd_log10",
            "snr_abs_delta_over_noise"
    };

    public static final class Result {
        public final String schemaVersion;
        public final int sampleCount;
        public final int sensorCount;
        public final boolean physicalTime;
        public final double[] vector;

        Result(int sampleCount, int sensorCount, boolean physicalTime, double[] vector) {
            this.schemaVersion = SCHEMA_VERSION;
            this.sampleCount = sampleCount;
            this.sensorCount = sensorCount;
            this.physicalTime = physicalTime;
            this.vector = vector;
        }

        public double feature(int sensorIndex, int featureIndex) {
            if (sensorIndex < 0 || sensorIndex >= sensorCount || featureIndex < 0 || featureIndex >= FEATURES_PER_SENSOR)
                return Double.NaN;
            return vector[sensorIndex * FEATURES_PER_SENSOR + featureIndex];
        }
    }

    /** Uses real time in seconds when supplied; otherwise falls back to sample index time. */
    public static Result extract(double[] timeSeconds, double[][] ratios) {
        if (ratios == null || ratios.length == 0) return new Result(0, 0, false, new double[0]);
        int sensors = 0;
        for (double[] row : ratios) if (row != null) sensors = Math.max(sensors, row.length);
        if (sensors == 0) return new Result(ratios.length, 0, false, new double[0]);

        boolean physicalTime = validTime(timeSeconds, ratios.length);
        double[] t = new double[ratios.length];
        if (physicalTime) System.arraycopy(timeSeconds, 0, t, 0, t.length);
        else for (int i = 0; i < t.length; i++) t[i] = i;

        double[] out = new double[sensors * FEATURES_PER_SENSOR];
        Arrays.fill(out, Double.NaN);

        for (int s = 0; s < sensors; s++) {
            double[] y = new double[ratios.length];
            Arrays.fill(y, Double.NaN);
            int valid = 0;
            for (int i = 0; i < ratios.length; i++) {
                double r = ratios[i] != null && s < ratios[i].length ? ratios[i][s] : Double.NaN;
                if (Double.isFinite(r) && r > 0) {
                    y[i] = Math.log10(r);
                    valid++;
                }
            }

            int o = s * FEATURES_PER_SENSOR;
            out[o] = (double) valid / ratios.length;
            if (valid < 4) continue;

            int baselineEnd = Math.max(2, Math.min(y.length, (int) Math.ceil(y.length * 0.20)));
            int steadyStart = Math.max(0, Math.min(y.length - 1, (int) Math.floor(y.length * 0.75)));
            double baseline = median(y, 0, baselineEnd);
            double steady = median(y, steadyStart, y.length);
            if (!Double.isFinite(baseline) || !Double.isFinite(steady)) continue;

            double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
            for (double v : y) if (Double.isFinite(v)) { min = Math.min(min, v); max = Math.max(max, v); }
            double down = baseline - min, up = max - baseline;
            double peak = up >= down ? max : min;
            double delta = steady - baseline;
            if (Math.abs(delta) < 1e-12) delta = peak - baseline;

            int initialEnd = Math.max(baselineEnd + 1, Math.min(y.length - 1, (int) Math.ceil(y.length * 0.55)));
            double initialSlope = slope(t, y, Math.max(0, baselineEnd - 1), initialEnd);
            double stabilitySlope = slope(t, y, steadyStart, y.length - 1);

            double target10 = baseline + 0.10 * (peak - baseline);
            double target50 = baseline + 0.50 * (peak - baseline);
            double target90 = baseline + 0.90 * (peak - baseline);
            boolean rising = peak >= baseline;
            double t0 = firstFiniteTime(t, y, Math.max(0, baselineEnd - 1));
            double t10 = relativeCrossing(t, y, target10, rising, Math.max(1, baselineEnd), t0);
            double t50 = relativeCrossing(t, y, target50, rising, Math.max(1, baselineEnd), t0);
            double t90 = relativeCrossing(t, y, target90, rising, Math.max(1, baselineEnd), t0);

            double signedAuc = 0, absAuc = 0;
            for (int i = 1; i < y.length; i++) {
                if (!Double.isFinite(y[i - 1]) || !Double.isFinite(y[i])) continue;
                double dt = t[i] - t[i - 1];
                if (!Double.isFinite(dt) || dt <= 0) continue;
                double a = y[i - 1] - baseline, b = y[i] - baseline;
                signedAuc += dt * (a + b) * 0.5;
                absAuc += dt * (Math.abs(a) + Math.abs(b)) * 0.5;
            }

            double noise = sampleSd(y, 0, baselineEnd, baseline);
            double snr = Double.isFinite(noise) && noise > 1e-12 ? Math.abs(delta) / noise : Double.POSITIVE_INFINITY;

            out[o + 1] = baseline;
            out[o + 2] = steady;
            out[o + 3] = delta;
            out[o + 4] = Math.abs(delta);
            out[o + 5] = min;
            out[o + 6] = max;
            out[o + 7] = peak;
            out[o + 8] = initialSlope;
            out[o + 9] = stabilitySlope;
            out[o + 10] = t10;
            out[o + 11] = t50;
            out[o + 12] = t90;
            out[o + 13] = signedAuc;
            out[o + 14] = absAuc;
            out[o + 15] = noise;
            out[o + 16] = snr;
        }
        return new Result(ratios.length, sensors, physicalTime, out);
    }

    public static String featureName(int sensorIndex, int featureIndex, String sensorKey) {
        if (featureIndex < 0 || featureIndex >= FEATURE_NAMES.length) return "unknown";
        String prefix = sensorKey == null || sensorKey.isEmpty() ? "sensor" + sensorIndex : sensorKey.toLowerCase();
        return prefix + "." + FEATURE_NAMES[featureIndex];
    }

    private static boolean validTime(double[] t, int n) {
        if (t == null || t.length != n || n < 2) return false;
        double previous = Double.NEGATIVE_INFINITY;
        for (double x : t) {
            if (!Double.isFinite(x) || x <= previous) return false;
            previous = x;
        }
        return true;
    }

    private static double median(double[] a, int from, int to) {
        from = Math.max(0, from); to = Math.min(a.length, Math.max(from, to));
        double[] x = new double[to - from]; int n = 0;
        for (int i = from; i < to; i++) if (Double.isFinite(a[i])) x[n++] = a[i];
        if (n == 0) return Double.NaN;
        x = Arrays.copyOf(x, n); Arrays.sort(x);
        return (n & 1) == 1 ? x[n / 2] : 0.5 * (x[n / 2 - 1] + x[n / 2]);
    }

    private static double sampleSd(double[] a, int from, int to, double center) {
        if (!Double.isFinite(center)) return Double.NaN;
        double ss = 0; int n = 0;
        for (int i = Math.max(0, from); i < Math.min(a.length, to); i++) if (Double.isFinite(a[i])) {
            double d = a[i] - center; ss += d * d; n++;
        }
        return n > 1 ? Math.sqrt(ss / (n - 1)) : Double.NaN;
    }

    private static double slope(double[] x, double[] y, int from, int toInclusive) {
        double sx = 0, sy = 0, sxx = 0, sxy = 0; int n = 0;
        for (int i = Math.max(0, from); i <= Math.min(y.length - 1, toInclusive); i++) {
            if (!Double.isFinite(x[i]) || !Double.isFinite(y[i])) continue;
            sx += x[i]; sy += y[i]; sxx += x[i] * x[i]; sxy += x[i] * y[i]; n++;
        }
        double den = n * sxx - sx * sx;
        return n >= 2 && Math.abs(den) > 1e-15 ? (n * sxy - sx * sy) / den : Double.NaN;
    }

    private static double firstFiniteTime(double[] t, double[] y, int start) {
        for (int i = Math.max(0, start); i < y.length; i++) if (Double.isFinite(y[i]) && Double.isFinite(t[i])) return t[i];
        return Double.NaN;
    }

    private static double relativeCrossing(double[] t, double[] y, double target, boolean rising, int start, double t0) {
        if (!Double.isFinite(t0)) return Double.NaN;
        for (int i = Math.max(1, start); i < y.length; i++) {
            if (!Double.isFinite(y[i - 1]) || !Double.isFinite(y[i])) continue;
            boolean crossed = rising ? y[i] >= target : y[i] <= target;
            if (!crossed) continue;
            double dy = y[i] - y[i - 1];
            double tc;
            if (Math.abs(dy) < 1e-15) tc = t[i];
            else {
                double f = (target - y[i - 1]) / dy;
                f = Math.max(0, Math.min(1, f));
                tc = t[i - 1] + f * (t[i] - t[i - 1]);
            }
            return Math.max(0, tc - t0);
        }
        return Double.NaN;
    }
}
