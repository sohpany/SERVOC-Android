package com.servoc.app.chemcore;

/**
 * Central uncertainty policy for ChemCore reference-response fitting.
 *
 * These values are research-scale proxies in log10(Rs/R0) space. They preserve the existing
 * 0.10.2/HR069 numerical behavior while making the provenance/semantics explicit and auditable.
 * They are NOT calibrated prediction intervals, confidence intervals, or device-specific M4
 * uncertainty estimates. A source-specific value may replace a proxy only after replicated
 * evidence supports that sensor/analyte/domain uncertainty.
 */
public final class ChemCoreUncertaintyPolicy {
    public static final String VERSION="chemcore-uncertainty-policy/1";
    public static final String SEMANTICS="research_log10_response_sigma_proxy_not_calibrated_prediction_interval";

    /** Peer-reviewed power-law/graph response relation proxy. */
    public static final double LITERATURE_POWER_SIGMA_LOG10=0.20;
    /** Peer-reviewed non-power/linear transfer relation proxy. */
    public static final double LITERATURE_LINEAR_SIGMA_LOG10=0.25;
    /** Manufacturer relation inside its primary target envelope. */
    public static final double MANUFACTURER_PRIMARY_SIGMA_LOG10=0.22;
    /** Manufacturer relation used in a secondary/graph-supported envelope. */
    public static final double MANUFACTURER_SECONDARY_SIGMA_LOG10=0.27;
    /** Research/external curve without device-specific replicated HR069 uncertainty. */
    public static final double RESEARCH_CURVE_SIGMA_LOG10=0.32;
    /** Conservative placeholder carried by unavailable reference estimates. */
    public static final double UNAVAILABLE_REFERENCE_SIGMA_LOG10=0.32;

    /** Solver fallback when a response model does not expose a positive finite sigma. */
    public static final double SOLVER_FALLBACK_SIGMA_LOG10=0.20;
    /** Numerical floor only; it is not a claim that any model has this physical precision. */
    public static final double MIN_SIGMA_LOG10=0.02;
    /** Existing conservative widening for a model prediction outside its declared domain. */
    public static final double OUT_OF_DOMAIN_SIGMA_MULTIPLIER=1.80;

    private ChemCoreUncertaintyPolicy() {}

    public static double solverSigma(double modelSigmaLog10,boolean insideDomain){
        double sigma=Double.isFinite(modelSigmaLog10)&&modelSigmaLog10>0
                ?Math.max(MIN_SIGMA_LOG10,modelSigmaLog10)
                :SOLVER_FALLBACK_SIGMA_LOG10;
        if(!insideDomain)sigma*=OUT_OF_DOMAIN_SIGMA_MULTIPLIER;
        return sigma;
    }

    /** R² alone is deliberately insufficient to derive a sigma estimate. */
    public static boolean acceptsR2AsSigmaEvidence(){return false;}
}
