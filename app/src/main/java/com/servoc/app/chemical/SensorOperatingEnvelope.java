package com.servoc.app.chemical;

/**
 * Operating-envelope policy for the eight MQ sensors physically used by HazRob.
 *
 * This class deliberately distinguishes manufacturer primary-target ranges from
 * datasheet cross-response graph fits. Cross-response is evidence that a sensor
 * reacts; it is not evidence of selectivity.
 */
public final class SensorOperatingEnvelope {
    private SensorOperatingEnvelope() {}

    public static final int UNKNOWN = 0;
    public static final int PRIMARY_TARGET = 3;
    public static final int DATASHEET_CROSS_RESPONSE = 2;
    public static final int OUT_OF_RANGE = -1;
    public static final int HEATER_PROTOCOL_REQUIRED = -2;

    public static final class Check {
        public final int status;
        public final double minPpm;
        public final double maxPpm;
        public final String note;

        Check(int status, double minPpm, double maxPpm, String note) {
            this.status = status;
            this.minPpm = minPpm;
            this.maxPpm = maxPpm;
            this.note = note;
        }

        public boolean usableForQuantitation() {
            return status == PRIMARY_TARGET || status == DATASHEET_CROSS_RESPONSE;
        }
    }

    /**
     * Validate an estimated ppm value against the best currently documented
     * operating envelope. heaterCycleValid must be true for MQ-7/MQ-9 data to
     * be accepted quantitatively because their results depend on the heater cycle.
     */
    public static Check check(int sensor, int chemical, double ppm, boolean heaterCycleValid) {
        if (!Double.isFinite(ppm) || ppm <= 0)
            return new Check(UNKNOWN, Double.NaN, Double.NaN, "Non-finite or non-positive concentration.");

        if ((sensor == GasEngine.MQ7 || sensor == GasEngine.MQ9) && !heaterCycleValid)
            return new Check(HEATER_PROTOCOL_REQUIRED, Double.NaN, Double.NaN,
                    "MQ-7/MQ-9 quantitative use requires a validated high/low heater cycle for the exact sensor revision/module.");

        Check primary = primaryEnvelope(sensor, chemical);
        if (primary != null) {
            if (ppm < primary.minPpm || ppm > primary.maxPpm)
                return new Check(OUT_OF_RANGE, primary.minPpm, primary.maxPpm,
                        "Outside manufacturer primary-target range; do not extrapolate ppm.");
            return primary;
        }

        for (ResearchCurveRegistry.Curve c : ResearchCurveRegistry.CURVES) {
            if (c.sensor != sensor || c.chemical != chemical) continue;
            if (ppm < c.minPpm || ppm > c.maxPpm)
                return new Check(OUT_OF_RANGE, c.minPpm, c.maxPpm,
                        "Outside the declared datasheet-derived cross-response graph range.");
            return new Check(DATASHEET_CROSS_RESPONSE, c.minPpm, c.maxPpm,
                    "Datasheet cross-response fit. Useful as ensemble evidence/fingerprint feature; not selective by itself.");
        }

        return new Check(UNKNOWN, Double.NaN, Double.NaN,
                "No documented quantitative envelope for this sensor/analyte pair.");
    }

    private static Check primaryEnvelope(int sensor, int chemical) {
        // Catalogue: CO=20, NH3=21, H2=75, CH4=76, propane=78,
        // ethanol=103, toluene=95, smoke=139, LPG=135.
        switch (sensor) {
            case GasEngine.MQ2:
                if (chemical == 135 || chemical == 78 || chemical == 76 || chemical == 75)
                    return primary(300, 10000, "MQ-2 flammable-gas operating envelope; exact gas sensitivity differs by analyte.");
                break;
            case GasEngine.MQ3:
                if (chemical == 103)
                    return primary(25, 500, "MQ-3B manufacturer alcohol range.");
                break;
            case GasEngine.MQ4:
                if (chemical == 76)
                    return primary(300, 10000, "MQ-4 manufacturer methane range.");
                break;
            case GasEngine.MQ5:
                if (chemical == 76 || chemical == 78 || chemical == 135)
                    return primary(300, 10000, "MQ-5 manufacturer methane/propane/LPG range.");
                break;
            case GasEngine.MQ7:
                if (chemical == 20)
                    return primary(10, 500, "MQ-7B manufacturer CO range; heater cycle is mandatory.");
                break;
            case GasEngine.MQ8:
                if (chemical == 75)
                    return primary(100, 1000, "MQ-8 manufacturer hydrogen range.");
                break;
            case GasEngine.MQ9:
                if (chemical == 20)
                    return primary(10, 500, "MQ-9B manufacturer CO range; heater cycle is mandatory.");
                if (chemical == 76)
                    return primary(300, 10000, "MQ-9B manufacturer methane range; heater cycle is mandatory.");
                break;
            case GasEngine.MQ135:
                if (chemical == 21 || chemical == 95 || chemical == 75 || chemical == 139)
                    return primary(10, 1000, "MQ-135 manufacturer-listed target range.");
                break;
            default:
                break;
        }
        return null;
    }

    private static Check primary(double min, double max, String note) {
        return new Check(PRIMARY_TARGET, min, max, note);
    }
}
