package com.servoc.app.chemical;

/**
 * Calibration-derived environmental compensation for MQ Rs/R0 ratios.
 *
 * No generic coefficients are hard-coded. The default identity model leaves the
 * measurement unchanged until HazRob has controlled calibration data for the
 * exact sensor/module. This avoids inventing temperature/humidity corrections.
 *
 * Model in log10 space:
 *   log10(ratio_corrected) = log10(ratio_measured)
 *                          - [bT*dT + bRH*dRH + bTR*dT*dRH
 *                             + bTT*dT^2 + bRR*dRH^2]
 * where dT=T-Tref and dRH=RH-RHref.
 */
public final class EnvironmentalCompensationModel {
    public final double tRefC;
    public final double rhRefPct;
    public final double bT;
    public final double bRH;
    public final double bTR;
    public final double bTT;
    public final double bRR;
    public final int calibrationPoints;
    public final String source;

    public EnvironmentalCompensationModel(double tRefC, double rhRefPct,
                                          double bT, double bRH, double bTR,
                                          double bTT, double bRR,
                                          int calibrationPoints, String source) {
        this.tRefC = tRefC;
        this.rhRefPct = rhRefPct;
        this.bT = bT;
        this.bRH = bRH;
        this.bTR = bTR;
        this.bTT = bTT;
        this.bRR = bRR;
        this.calibrationPoints = calibrationPoints;
        this.source = source == null ? "" : source;
    }

    public static EnvironmentalCompensationModel identity() {
        return new EnvironmentalCompensationModel(20.0, 55.0,
                0, 0, 0, 0, 0, 0,
                "Identity model: compensation disabled until sensor-specific calibration exists.");
    }

    public boolean isCalibrated() {
        return calibrationPoints >= 6 &&
                (bT != 0 || bRH != 0 || bTR != 0 || bTT != 0 || bRR != 0);
    }

    public double compensateRatio(double measuredRatio, double temperatureC, double humidityPct) {
        if (!Double.isFinite(measuredRatio) || measuredRatio <= 0) return Double.NaN;
        if (!Double.isFinite(temperatureC) || !Double.isFinite(humidityPct)) return measuredRatio;
        if (!isCalibrated()) return measuredRatio;

        double dT = temperatureC - tRefC;
        double dR = humidityPct - rhRefPct;
        double bias = bT*dT + bRH*dR + bTR*dT*dR + bTT*dT*dT + bRR*dR*dR;
        double correctedLog = Math.log10(measuredRatio) - bias;
        double corrected = Math.pow(10.0, correctedLog);
        return Double.isFinite(corrected) && corrected > 0 ? corrected : Double.NaN;
    }

    public double environmentalBiasLog10(double temperatureC, double humidityPct) {
        if (!isCalibrated() || !Double.isFinite(temperatureC) || !Double.isFinite(humidityPct)) return 0;
        double dT = temperatureC - tRefC;
        double dR = humidityPct - rhRefPct;
        return bT*dT + bRH*dR + bTR*dT*dR + bTT*dT*dT + bRR*dR*dR;
    }
}
