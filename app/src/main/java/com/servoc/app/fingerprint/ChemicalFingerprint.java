package com.servoc.app.fingerprint;

import java.util.Arrays;

/** Statistical fingerprint in log10(Rs/R0) space. */
public final class ChemicalFingerprint {
    public final int chemicalId;
    public final String label;
    public final int sampleCount;
    public final double[] meanLogRatio;
    public final double[] sigmaLogRatio;

    public ChemicalFingerprint(int chemicalId, String label, int sampleCount,
                               double[] meanLogRatio, double[] sigmaLogRatio) {
        this.chemicalId = chemicalId;
        this.label = label == null ? "" : label;
        this.sampleCount = Math.max(0, sampleCount);
        this.meanLogRatio = Arrays.copyOf(meanLogRatio, meanLogRatio.length);
        this.sigmaLogRatio = Arrays.copyOf(sigmaLogRatio, sigmaLogRatio.length);
    }
}
