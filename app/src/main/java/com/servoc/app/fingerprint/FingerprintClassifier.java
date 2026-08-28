package com.servoc.app.fingerprint;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Distance-based classifier for user-trained chemical fingerprints. */
public final class FingerprintClassifier {
    private FingerprintClassifier() {}

    public static final class Match {
        public final ChemicalFingerprint fingerprint;
        public final double score;
        public final double distance;
        public final int contributors;
        Match(ChemicalFingerprint f, double score, double distance, int contributors) {
            this.fingerprint = f; this.score = score; this.distance = distance; this.contributors = contributors;
        }
    }

    public static List<Match> classify(double[] ratios, List<ChemicalFingerprint> library) {
        List<Match> out = new ArrayList<>();
        if (ratios == null || library == null) return out;
        for (ChemicalFingerprint f : library) {
            double sum = 0; int n = 0;
            int width = Math.min(ratios.length, f.meanLogRatio.length);
            for (int i = 0; i < width; i++) {
                double r = ratios[i], mu = f.meanLogRatio[i];
                if (!Double.isFinite(r) || r <= 0 || !Double.isFinite(mu)) continue;
                double sigma = (i < f.sigmaLogRatio.length && Double.isFinite(f.sigmaLogRatio[i])) ? f.sigmaLogRatio[i] : 0.15;
                sigma = Math.max(0.05, sigma);
                double z = (Math.log10(r) - mu) / sigma;
                sum += z * z; n++;
            }
            if (n == 0) continue;
            double d = Math.sqrt(sum / n);
            double coverage = Math.min(1.0, n / 4.0);
            double score = 100.0 * coverage * Math.exp(-0.5 * d);
            out.add(new Match(f, score, d, n));
        }
        out.sort(Comparator.comparingDouble((Match m) -> m.score).reversed());
        return out;
    }
}
