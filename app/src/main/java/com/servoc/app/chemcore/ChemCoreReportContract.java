package com.servoc.app.chemcore;

/**
 * Versioned scientific-output contract consumed by HazRob reporting.
 *
 * Bump VERSION whenever a material meaning, required key, evidence-tier rule or
 * reportability semantic exposed to SmartReport changes. A corresponding
 * SmartReportAnalyticalContract update and Change Record are then required.
 */
public final class ChemCoreReportContract {
    private ChemCoreReportContract(){}

    public static final String VERSION="HR069-CC-SR-1";
    public static final int SCHEMA_REVISION=1;

    /** Candidate posterior values are ranking weights, not calibrated presence probabilities. */
    public static final String RANKING_SEMANTICS="relative_softmax_model_weight_not_calibrated_probability";

    /** Only the explicit HR069 device/reference validated tier may unlock reportable ppm. */
    public static final String REPORTABLE_PPM_TIER="M4_HAZROB_VALIDATED";

    /** Top-level scientific surfaces that the report contract knows how to interpret. */
    public static final String[] KNOWN_SURFACES={
            "outcome","chemical","evidence_tier","candidates","screening_144",
            "top_posterior","posterior_separation_margin","ood_score","normalized_entropy",
            "analytical_confidence_pct","ppm_reportable","ppm","gas_exposure_ppm",
            "measurement_quality","lab_quality","library","sampling_profile_id",
            "feature_schema","model_version"
    };
}
