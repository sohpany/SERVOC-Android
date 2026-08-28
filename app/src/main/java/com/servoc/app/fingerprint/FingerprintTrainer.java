package com.servoc.app.fingerprint;

import java.util.ArrayList;
import java.util.List;

/** Builds a reusable fingerprint from repeated sensor-ratio captures. */
public final class FingerprintTrainer {
    private FingerprintTrainer() {}

    public static ChemicalFingerprint train(int chemicalId, String label, List<double[]> samples) {
        if (samples == null || samples.isEmpty()) return null;
        int width = 0;
        for (double[] s : samples) if (s != null) width = Math.max(width, s.length);
        if (width == 0) return null;

        double[] mean = new double[width];
        double[] m2 = new double[width];
        int[] n = new int[width];

        for (double[] s : samples) {
            if (s == null) continue;
            for (int i = 0; i < Math.min(width, s.length); i++) {
                double r = s[i];
                if (!Double.isFinite(r) || r <= 0) continue;
                double x = Math.log10(r);
                n[i]++;
                double d = x - mean[i];
                mean[i] += d / n[i];
                m2[i] += d * (x - mean[i]);
            }
        }

        double[] sigma = new double[width];
        for (int i = 0; i < width; i++) {
            if (n[i] == 0) { mean[i] = Double.NaN; sigma[i] = Double.NaN; }
            else sigma[i] = n[i] > 1 ? Math.sqrt(m2[i] / (n[i] - 1)) : 0.12;
            if (Double.isFinite(sigma[i])) sigma[i] = Math.max(0.05, sigma[i]);
        }
        return new ChemicalFingerprint(chemicalId, label, samples.size(), mean, sigma);
    }

    public static List<double[]> validSamples(List<double[]> input) {
        List<double[]> out = new ArrayList<>();
        if (input != null) for (double[] s : input) if (s != null) out.add(s);
        return out;
    }
}
