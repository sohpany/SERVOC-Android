package com.servoc.app.calibration;

/**
 * Supplies device-specific analyte calibration estimates to the analytical engine.
 * Implementations may load active curves from SQLite, files or a future cloud backup.
 */
public interface CalibrationCurveProvider {
    final class Estimate {
        public final double ppm;
        public final boolean valid;
        public final boolean referenceValidated;
        public final String modelVersion;
        public final String note;

        public Estimate(double ppm, boolean valid, boolean referenceValidated,
                        String modelVersion, String note) {
            this.ppm=ppm;
            this.valid=valid;
            this.referenceValidated=referenceValidated;
            this.modelVersion=modelVersion==null?"":modelVersion;
            this.note=note==null?"":note;
        }
    }

    /** Returns null when no active calibration exists for this sensor/analyte pair. */
    Estimate estimate(int sensorId, int chemicalId, double rsOverR0);
}
