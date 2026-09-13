package com.servoc.app.calibration;

import java.util.Locale;

/**
 * Device-specific field clean-air R0 baseline derived from three independent HazRob
 * clean-air sessions on 2026-09-13.
 *
 * For each session, Rs is aggregated as a geometric mean over the complete 30.0 s
 * time series. The three session geometric means are then combined with equal session
 * weight in log space. The campaign contains 216 samples total (73 + 70 + 73).
 *
 * Wind suitability is controlled field-protocol context. The onboard ENV channel did
 * not provide T/RH/P in these reports, so no environmental compensation is claimed.
 * These values are NOT reference-gas validation for manufacturer ppm curves.
 * Quantitative ppm remains gated by CalibrationCurveProvider.Estimate.referenceValidated.
 */
public final class FieldR0Baseline {
    private FieldR0Baseline() {}

    public static final String SOURCE="field_clean_air_r0";
    public static final String SESSION="FIELD-R0-3SESSION-20260913";
    public static final String UTC="2026-09-13T01:00:47Z";
    public static final int SESSION_COUNT=3;
    public static final int SAMPLE_COUNT=216;
    public static final double WINDOW_SECONDS=30.0;
    public static final boolean REFERENCE_GAS_VALIDATED=false;
    public static final boolean ENVIRONMENT_COMPENSATED=false;

    public static double value(String key){
        if(key==null)return Double.NaN;
        switch(key.trim().toLowerCase(Locale.US)){
            case "mq2": return 91.7340228444;
            case "mq4": return 65.2695876771;
            case "mq5": return 251.9647223909;
            case "mq8": return 54.8886824742;
            case "mq3": return 20.6860315768;
            case "mq7": return 160.6013515297;
            case "mq9": return 69.9900854903;
            case "mq135": return 213.2390474819;
            default: return Double.NaN;
        }
    }

    public static boolean has(String key){double v=value(key);return Double.isFinite(v)&&v>0;}
}
