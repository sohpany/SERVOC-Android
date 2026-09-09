package com.servoc.app.chemcore;

/** Stable analytical decision vocabulary shared by ChemCore runtime, reports and model cards. */
public final class ChemCoreDecision {
    private ChemCoreDecision() {}

    public enum Outcome {
        SCREENING,
        IDENTIFIED,
        QUANTIFIED,
        UNKNOWN,
        INSUFFICIENT_EVIDENCE,
        OUT_OF_VALIDATED_DOMAIN,
        POSSIBLE_MIXTURE_OR_INTERFERENCE,
        INVALID_MEASUREMENT
    }

    public enum EvidenceTier {
        M0_CATALOG_ONLY,
        M1_MOLECULARLY_INFERRED,
        M2_LITERATURE_OR_MANUFACTURER_SUPPORTED,
        M3_ARRAY_CONSISTENT,
        M4_HAZROB_VALIDATED
    }

    public static boolean permitsNamedIdentity(Outcome outcome) {
        return outcome == Outcome.IDENTIFIED || outcome == Outcome.QUANTIFIED;
    }

    public static boolean permitsQuantitativeOccupationalInterpretation(Outcome outcome, EvidenceTier tier) {
        return outcome == Outcome.QUANTIFIED && tier == EvidenceTier.M4_HAZROB_VALIDATED;
    }
}
