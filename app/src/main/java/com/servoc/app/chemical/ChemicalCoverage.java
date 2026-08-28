package com.servoc.app.chemical;

/**
 * Scientific coverage planner for the 144-entry chemical catalogue.
 * It intentionally separates direct quantitative curves from empirical fingerprint work.
 */
public final class ChemicalCoverage {
    private ChemicalCoverage() {}

    public static final int DIRECT_CURVE = 3;
    public static final int FINGERPRINT_CANDIDATE = 2;
    public static final int UNVALIDATED_OR_WEAK = 1;
    public static final int AGGREGATE = 0;

    public static final class Status {
        public final int level;
        public final String method;
        public final String note;
        Status(int level, String method, String note) {
            this.level=level; this.method=method; this.note=note;
        }
    }

    private static final int[] CORROSIVE_OR_OXIDIZING = {
            4,10,11,12,13,14,15,16,17,18,19,42,43,44,45,46,47,60,61
    };
    private static final int[] LOW_RESPONSE_OR_UNPROVEN = {
            22,24,49,123,124,125,126,127,128,129,130,131,132,133,134
    };

    public static Status forChemical(int chem) {
        if (chem < 0 || chem >= ChemicalCatalog.COUNT)
            return new Status(UNVALIDATED_OR_WEAK, "UNKNOWN", "Invalid catalogue index.");

        if (isDirectCurve(chem)) {
            return new Status(DIRECT_CURVE,
                    "Rs/R0 log-power curve + multi-sensor robust consensus",
                    "A quantitative curve exists in the current HazRob library. Coefficients remain calibration-dependent and are not a substitute for sensor-specific R0 validation.");
        }

        if (chem >= 135) {
            return new Status(AGGREGATE,
                    "Composite / operational metric",
                    "This entry represents a mixture or aggregate metric rather than one pure analyte. It should be estimated from trained mixture fingerprints or derived from constituent gases, not assigned a single universal MQ curve.");
        }

        if (contains(CORROSIVE_OR_OXIDIZING, chem)) {
            return new Status(UNVALIDATED_OR_WEAK,
                    "Research only; fingerprinting requires compatibility review",
                    "The purchased MQ array has no validated quantitative model for this analyte, and corrosive/oxidizing gases can bias, poison or damage semiconductor sensors. Do not infer ppm from cross-sensitivity alone.");
        }

        if (contains(LOW_RESPONSE_OR_UNPROVEN, chem)) {
            return new Status(UNVALIDATED_OR_WEAK,
                    "No evidence-backed MQ quantitative model",
                    "Current MQ-array response is expected to be weak, indirect or insufficiently documented. Retain in the catalogue but report as unsupported until controlled calibration demonstrates a usable signal.");
        }

        return new Status(FINGERPRINT_CANDIDATE,
                "Eight-sensor electronic-nose fingerprint + supervised concentration regression",
                "This compound is a candidate for empirical training using the existing MQ-2/3/4/5/7/8/9/135 array. Identification must be learned from controlled exposures; concentration regression must be trained separately from identity classification.");
    }

    public static int directCurveCount() {
        return GasEngine.SUPPORTED.length;
    }

    public static int fingerprintCandidateCount() {
        int n=0;
        for(int i=0;i<ChemicalCatalog.COUNT;i++) if(forChemical(i).level==FINGERPRINT_CANDIDATE) n++;
        return n;
    }

    public static int weakOrUnvalidatedCount() {
        int n=0;
        for(int i=0;i<ChemicalCatalog.COUNT;i++) if(forChemical(i).level==UNVALIDATED_OR_WEAK) n++;
        return n;
    }

    private static boolean isDirectCurve(int chem) {
        for (int c : GasEngine.SUPPORTED) if (c == chem) return true;
        return false;
    }

    private static boolean contains(int[] a, int value) {
        for (int x : a) if (x == value) return true;
        return false;
    }
}
