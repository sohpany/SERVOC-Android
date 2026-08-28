package com.servoc.app.chemical;

/**
 * Provenance vocabulary for every gas-response equation used by HazRob.
 * No equation should be promoted to an alarm-capable quantitative model merely
 * because it exists in a community library.
 */
public final class CurveProvenance {
    private CurveProvenance() {}

    public static final int MANUFACTURER_TABULATED = 4;
    public static final int MANUFACTURER_GRAPH_FIT = 3;
    public static final int INDEPENDENT_CALIBRATION = 3;
    public static final int COMMUNITY_DATASHEET_FIT = 2;
    public static final int CROSS_RESPONSE_RESEARCH = 1;

    public static final String MQDATASCIENCE_VERSION = "6.0.0";
    public static final String MQDATASCIENCE_COMMIT = "e88f876140587a708ee84a4d4d3acbd5720274ee";

    public static final class Evidence {
        public final int level;
        public final String source;
        public final String method;
        public final boolean alarmEligibleBySource;
        public final String note;

        public Evidence(int level, String source, String method,
                        boolean alarmEligibleBySource, String note) {
            this.level = level;
            this.source = source;
            this.method = method;
            this.alarmEligibleBySource = alarmEligibleBySource;
            this.note = note;
        }
    }

    public static Evidence inheritedGasEngine() {
        return new Evidence(
                COMMUNITY_DATASHEET_FIT,
                "MQSensorsLib / datasheet graph regressions inherited from SERVOC firmware",
                "power-law fit: ppm = a*(Rs/R0)^b",
                false,
                "Useful as a starting model only. Alarm eligibility additionally requires sensor-specific R0 calibration, correct heater phase, in-range operation and validation against known gas concentrations."
        );
    }

    public static Evidence mqDataScienceComparison() {
        return new Evidence(
                COMMUNITY_DATASHEET_FIT,
                "MQDataScience v"+MQDATASCIENCE_VERSION+" (MIT) @ "+MQDATASCIENCE_COMMIT,
                "digitized response-graph power law: ratio = a*ppm^b; inverted by HazRob for comparison",
                false,
                "Pinned research comparison source. It must not silently replace a validated HazRob calibration model."
        );
    }

    public static Evidence mqDataScienceEnvironmentalCorrection() {
        return new Evidence(
                CROSS_RESPONSE_RESEARCH,
                "MQDataScience v"+MQDATASCIENCE_VERSION+" (MIT) @ "+MQDATASCIENCE_COMMIT,
                "temperature/RH correction factor using RH-interpolated exponential coefficients",
                false,
                "Research-only environmental compensation. HazRob refuses out-of-domain clamping and will not apply this model to alarm decisions until validated against reference-gas measurements from the actual sensor array."
        );
    }

    public static Evidence manufacturerTarget(String model) {
        return new Evidence(
                MANUFACTURER_GRAPH_FIT,
                "Winsen " + model + " product manual / typical sensitivity graph",
                "manufacturer target/range evidence; fitted coefficients require graph digitization audit",
                false,
                "Manufacturer target status raises evidential weight, but a typical curve is not an individual-sensor calibration certificate."
        );
    }

    public static boolean canBecomeQuantitative(Evidence e,
                                                 boolean sensorSpecificCalibration,
                                                 boolean operatingEnvelopeValid,
                                                 boolean referenceGasValidated) {
        if (e == null) return false;
        return e.level >= COMMUNITY_DATASHEET_FIT
                && sensorSpecificCalibration
                && operatingEnvelopeValid
                && referenceGasValidated;
    }
}
