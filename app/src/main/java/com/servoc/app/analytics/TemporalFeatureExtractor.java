package com.servoc.app.analytics;

import com.servoc.app.chemcore.ChemCoreFeaturePipeline;

/**
 * Legacy compatibility adapter.
 *
 * Scientific feature extraction is canonical in ChemCoreFeaturePipeline
 * (chemcore-features/1). This class intentionally delegates to that contract
 * and exposes a reduced eight-feature view only for legacy consumers.
 */
@Deprecated
public final class TemporalFeatureExtractor {
    private TemporalFeatureExtractor() {}

    public static final int FEATURES_PER_SENSOR = 8;

    public static double[] extract(double[][] ratios) {
        ChemCoreFeaturePipeline.Result canonical = ChemCoreFeaturePipeline.extract(null, ratios);
        if (canonical.sensorCount == 0) return new double[0];
        double[] out = new double[canonical.sensorCount * FEATURES_PER_SENSOR];
        for (int s = 0; s < canonical.sensorCount; s++) {
            int o = s * FEATURES_PER_SENSOR;
            out[o]   = finiteOrZero(canonical.feature(s, 0));  // valid fraction
            out[o+1] = finiteOrZero(canonical.feature(s, 1));  // baseline log10 ratio
            out[o+2] = finiteOrZero(canonical.feature(s, 2));  // steady log10 ratio
            out[o+3] = finiteOrZero(canonical.feature(s, 3));  // signed delta
            out[o+4] = finiteOrZero(canonical.feature(s, 5));  // min
            out[o+5] = finiteOrZero(canonical.feature(s, 6));  // max
            out[o+6] = finiteOrZero(canonical.feature(s, 8));  // initial slope
            out[o+7] = finiteOrZero(canonical.feature(s, 14)); // absolute AUC
        }
        return out;
    }

    private static double finiteOrZero(double x) { return Double.isFinite(x) ? x : 0; }
}
