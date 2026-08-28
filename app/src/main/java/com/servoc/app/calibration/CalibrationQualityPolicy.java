package com.servoc.app.calibration;

/**
 * Configurable research-QC policy for HazRob MQ calibration sessions.
 * Defaults are project safeguards, not a regulatory standard. They are intentionally
 * conservative enough to reject one-point/one-shot models while remaining adjustable
 * for future laboratory protocols.
 */
public final class CalibrationQualityPolicy {
    public final int minDistinctConcentrations;
    public final int minReplicatesPerConcentration;
    public final double maxReplicateCvPercent;
    public final double minConcentrationSpanFactor;
    public final double minLogPowerR2;
    public final boolean requireBaseline;
    public final boolean requireRecovery;
    public final boolean requireHeaterProtocolMetadata;
    public final boolean disallowExtrapolation;

    public CalibrationQualityPolicy(int minDistinctConcentrations,
                                    int minReplicatesPerConcentration,
                                    double maxReplicateCvPercent,
                                    double minConcentrationSpanFactor,
                                    double minLogPowerR2,
                                    boolean requireBaseline,
                                    boolean requireRecovery,
                                    boolean requireHeaterProtocolMetadata,
                                    boolean disallowExtrapolation) {
        if (minDistinctConcentrations < 2) throw new IllegalArgumentException("At least two concentrations required");
        if (minReplicatesPerConcentration < 1) throw new IllegalArgumentException("At least one replicate required");
        if (!Double.isFinite(maxReplicateCvPercent) || maxReplicateCvPercent <= 0) throw new IllegalArgumentException("Invalid CV limit");
        if (!Double.isFinite(minConcentrationSpanFactor) || minConcentrationSpanFactor <= 1) throw new IllegalArgumentException("Invalid span factor");
        if (!Double.isFinite(minLogPowerR2) || minLogPowerR2 < 0 || minLogPowerR2 > 1) throw new IllegalArgumentException("Invalid R2 limit");
        this.minDistinctConcentrations=minDistinctConcentrations;
        this.minReplicatesPerConcentration=minReplicatesPerConcentration;
        this.maxReplicateCvPercent=maxReplicateCvPercent;
        this.minConcentrationSpanFactor=minConcentrationSpanFactor;
        this.minLogPowerR2=minLogPowerR2;
        this.requireBaseline=requireBaseline;
        this.requireRecovery=requireRecovery;
        this.requireHeaterProtocolMetadata=requireHeaterProtocolMetadata;
        this.disallowExtrapolation=disallowExtrapolation;
    }

    /** HazRob research default; intentionally configurable and not presented as a formal standard. */
    public static CalibrationQualityPolicy hazRobDefault() {
        return new CalibrationQualityPolicy(
                3,      // distinct known concentrations
                3,      // independent/stabilized captures per concentration
                15.0,   // maximum within-level CV of Rs/R0
                4.0,    // max ppm / min ppm must span at least 4x
                0.90,   // diagnostic global log-power R2; interpolation remains primary
                true,
                true,
                true,
                true
        );
    }
}
