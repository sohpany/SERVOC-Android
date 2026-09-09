package com.servoc.app.chemcore;

/**
 * Versioned physical sampling profile for ChemCore. Geometry is SI internally.
 * Airflow-dependent outputs remain explicitly tagged by FlowStatus so estimated
 * engineering values cannot be mistaken for calibrated metrology.
 */
public final class SamplingProfile {
    public enum FlowStatus { FLOW_UNCALIBRATED, FLOW_ESTIMATED, FLOW_MEASURED, FLOW_CALIBRATED }

    public static final double STANDARD_V2_ESTIMATED_FLOW_M3S = 0.00110;      // 1.10 L/s
    public static final double STANDARD_V2_FLOW_UNCERTAINTY_M3S = 0.00040;    // engineering estimate
    public static final double STANDARD_V2_DOME_DISPLACEMENT_M3 = 0.000023;  // ~23 cm^3, eight exposed sensor caps

    public final String profileId;
    public final String version;
    public final double chamberWidthM, chamberHeightM, chamberLengthM;
    public final double inletWidthM, inletHeightM;
    public final double fanWidthM, fanHeightM, fanVoltageV, fanCurrentA;
    public final double internalDisplacementM3;
    public final FlowStatus flowStatus;
    public final double flowM3s;
    public final double flowUncertaintyM3s;
    public final String notes;

    /** Backward-compatible constructor: no internal displacement correction. */
    public SamplingProfile(String profileId, String version,
                           double chamberWidthM, double chamberHeightM, double chamberLengthM,
                           double inletWidthM, double inletHeightM,
                           double fanWidthM, double fanHeightM, double fanVoltageV, double fanCurrentA,
                           FlowStatus flowStatus, double flowM3s, double flowUncertaintyM3s, String notes) {
        this(profileId, version, chamberWidthM, chamberHeightM, chamberLengthM,
                inletWidthM, inletHeightM, fanWidthM, fanHeightM, fanVoltageV, fanCurrentA,
                0.0, flowStatus, flowM3s, flowUncertaintyM3s, notes);
    }

    public SamplingProfile(String profileId, String version,
                           double chamberWidthM, double chamberHeightM, double chamberLengthM,
                           double inletWidthM, double inletHeightM,
                           double fanWidthM, double fanHeightM, double fanVoltageV, double fanCurrentA,
                           double internalDisplacementM3,
                           FlowStatus flowStatus, double flowM3s, double flowUncertaintyM3s, String notes) {
        if (profileId == null || profileId.trim().isEmpty()) throw new IllegalArgumentException("profileId required");
        if (!(chamberWidthM > 0 && chamberHeightM > 0 && chamberLengthM > 0)) throw new IllegalArgumentException("invalid chamber geometry");
        if (!(inletWidthM > 0 && inletHeightM > 0)) throw new IllegalArgumentException("invalid inlet geometry");
        double gross = chamberWidthM * chamberHeightM * chamberLengthM;
        if (!(internalDisplacementM3 >= 0 && Double.isFinite(internalDisplacementM3) && internalDisplacementM3 < gross))
            throw new IllegalArgumentException("invalid internal displacement");
        this.profileId = profileId;
        this.version = version == null ? "" : version;
        this.chamberWidthM = chamberWidthM;
        this.chamberHeightM = chamberHeightM;
        this.chamberLengthM = chamberLengthM;
        this.inletWidthM = inletWidthM;
        this.inletHeightM = inletHeightM;
        this.fanWidthM = fanWidthM;
        this.fanHeightM = fanHeightM;
        this.fanVoltageV = fanVoltageV;
        this.fanCurrentA = fanCurrentA;
        this.internalDisplacementM3 = internalDisplacementM3;
        this.flowStatus = flowStatus == null ? FlowStatus.FLOW_UNCALIBRATED : flowStatus;
        this.flowM3s = flowM3s > 0 && Double.isFinite(flowM3s) ? flowM3s : Double.NaN;
        this.flowUncertaintyM3s = flowUncertaintyM3s >= 0 && Double.isFinite(flowUncertaintyM3s) ? flowUncertaintyM3s : Double.NaN;
        this.notes = notes == null ? "" : notes;
    }

    /** Historical full-opening prototype retained so old sampling records remain interpretable. */
    public static SamplingProfile hazRobPrototypeV1() {
        return new SamplingProfile(
                "hazrob-chemcore-chamber-v1", "1",
                0.09, 0.04, 0.15,
                0.04, 0.04,
                0.04, 0.04, 5.0, 0.28,
                FlowStatus.FLOW_UNCALIBRATED, Double.NaN, Double.NaN,
                "Rear 40x40 mm extraction fan; original 40x40 mm inlet; actual in-chamber airflow was not assigned."
        );
    }

    /**
     * Canonical HazRob ChemCore chamber geometry adopted 2026-09-09.
     *
     * Mechanical standard:
     * - chamber 90 x 40 x 150 mm;
     * - centered front inlet 40 x 20 mm (50% of the original inlet area);
     * - rear 40 x 40 mm, 5 V / 0.28 A extractor mounted behind a centered ~28 x 28 mm
     *   outlet aperture, exposing about half of the fan face while preserving symmetry;
     * - only the eight MQ sensor caps project into the chamber.
     *
     * The fixed 1.10 L/s value is an engineering sampling factor, not a calibrated fan
     * measurement. It is intentionally tagged FLOW_ESTIMATED and carries a broad uncertainty.
     */
    public static SamplingProfile hazRobStandardChamberV2() {
        return new SamplingProfile(
                "hazrob-chemcore-chamber-v2", "2",
                0.09, 0.04, 0.15,
                0.04, 0.02,
                0.04, 0.04, 5.0, 0.28,
                STANDARD_V2_DOME_DISPLACEMENT_M3,
                FlowStatus.FLOW_ESTIMATED,
                STANDARD_V2_ESTIMATED_FLOW_M3S,
                STANDARD_V2_FLOW_UNCERTAINTY_M3S,
                "Standard fixed sampling geometry. Front inlet 40x20 mm centered. Rear outlet approximately 28x28 mm centered over the 40x40 mm extractor (~49% face area). Eight sensor caps only inside chamber. Estimated sensor-cap displacement ~23 cm3. Nominal Q=1.10 L/s is a conservative engineering estimate derived from a comparable 40x40x10 mm 5 V/0.28 A fan rated 6.66 CFM free-air, then derated for the reduced outlet/inlet and system losses. Do not promote to FLOW_MEASURED or FLOW_CALIBRATED without physical metrology."
        );
    }

    public SamplingProfile withFlow(FlowStatus status, double flowM3s, double uncertaintyM3s, String note) {
        return new SamplingProfile(profileId, version,
                chamberWidthM, chamberHeightM, chamberLengthM,
                inletWidthM, inletHeightM,
                fanWidthM, fanHeightM, fanVoltageV, fanCurrentA,
                internalDisplacementM3,
                status, flowM3s, uncertaintyM3s,
                note == null || note.isEmpty() ? notes : note);
    }

    public double grossVolumeM3() { return chamberWidthM * chamberHeightM * chamberLengthM; }
    public double grossVolumeLiters() { return grossVolumeM3() * 1000.0; }
    public double effectiveVolumeM3() { return grossVolumeM3() - internalDisplacementM3; }
    public double effectiveVolumeLiters() { return effectiveVolumeM3() * 1000.0; }
    public double chamberCrossSectionM2() { return chamberWidthM * chamberHeightM; }
    public double inletAreaM2() { return inletWidthM * inletHeightM; }
    public double fanElectricalPowerW() { return fanVoltageV * fanCurrentA; }

    public boolean hasFlow() { return Double.isFinite(flowM3s) && flowM3s > 0; }
    public boolean flowValidated() { return flowStatus == FlowStatus.FLOW_CALIBRATED && hasFlow(); }

    /** Superficial mean velocity based on the gross chamber cross-section. */
    public double inletVelocityMps() { return hasFlow() ? flowM3s / inletAreaM2() : Double.NaN; }
    public double chamberVelocityMps() { return hasFlow() ? flowM3s / chamberCrossSectionM2() : Double.NaN; }
    public double grossResidenceTimeS() { return hasFlow() ? grossVolumeM3() / flowM3s : Double.NaN; }
    public double effectiveResidenceTimeS() { return hasFlow() ? effectiveVolumeM3() / flowM3s : Double.NaN; }

    public static double cfmToM3s(double cfm) { return cfm * 0.00047194745; }
    public static double litersPerMinuteToM3s(double lpm) { return lpm / 60000.0; }
    public static double m3sToLitersPerMinute(double m3s) { return m3s * 60000.0; }
}
