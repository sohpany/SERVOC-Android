package com.servoc.app.calibration;

import java.util.Locale;

/**
 * Device-specific clean-air R0 baseline derived from HazRob measurement session 10.
 *
 * The values are geometric means of Rs across all 74 time-series samples from
 * FIELD-1789255293867 (2026-09-12T23:22:12Z). They are valid as a field
 * clean-air normalization baseline for this physical eight-MQ assembly.
 *
 * They are NOT reference-gas validation for manufacturer ppm curves. Quantitative
 * ppm remains gated by CalibrationCurveProvider.Estimate.referenceValidated.
 */
public final class FieldR0Baseline {
    private FieldR0Baseline() {}

    public static final String SOURCE="field_clean_air_r0";
    public static final String SESSION="FIELD-1789255293867";
    public static final String UTC="2026-09-12T23:22:12Z";
    public static final int SAMPLE_COUNT=74;
    public static final double WINDOW_SECONDS=30.1;
    public static final boolean REFERENCE_GAS_VALIDATED=false;
    public static final boolean ENVIRONMENT_COMPENSATED=false;

    public static double value(String key){
        if(key==null)return Double.NaN;
        switch(key.trim().toLowerCase(Locale.US)){
            case "mq2": return 79.3176036586;
            case "mq4": return 36.1454126550;
            case "mq5": return 214.5398913808;
            case "mq8": return 35.7836813368;
            case "mq3": return 8.7149649220;
            case "mq7": return 119.2408579279;
            case "mq9": return 32.3349994303;
            case "mq135": return 198.8868482045;
            default: return Double.NaN;
        }
    }

    public static boolean has(String key){double v=value(key);return Double.isFinite(v)&&v>0;}
}
