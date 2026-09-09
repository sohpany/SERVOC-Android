package com.servoc.app.chemcore;

/**
 * Versioned physical sampling profile for ChemCore. Geometry is SI internally.
 * Airflow-dependent outputs are unavailable until a flow value is supplied.
 */
public final class SamplingProfile {
    public enum FlowStatus { FLOW_UNCALIBRATED, FLOW_ESTIMATED, FLOW_MEASURED, FLOW_CALIBRATED }

    public final String profileId;
    public final String version;
    public final double chamberWidthM, chamberHeightM, chamberLengthM;
    public final double inletWidthM, inletHeightM;
    public final double fanWidthM, fanHeightM, fanVoltageV, fanCurrentA;
    public final FlowStatus flowStatus;
    public final double flowM3s;
    public final double flowUncertaintyM3s;
    public final String notes;

    public SamplingProfile(String profileId, String version,
                           double chamberWidthM, double chamberHeightM, double chamberLengthM,
                           double inletWidthM, double inletHeightM,
                           double fanWidthM, double fanHeightM, double fanVoltageV, double fanCurrentA,
                           FlowStatus flowStatus, double flowM3s, double flowUncertaintyM3s, String notes) {
        if (profileId == null || profileId.trim().isEmpty()) throw new IllegalArgumentException("profileId required");
        if (!(chamberWidthM > 0 && chamberHeightM > 0 && chamberLengthM > 0)) throw new IllegalArgumentException("invalid chamber geometry");
        if (!(inletWidthM > 0 && inletHeightM > 0)) throw new IllegalArgumentException("invalid inlet geometry");
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
        this.flowStatus = flowStatus == null ? FlowStatus.FLOW_UNCALIBRATED : flowStatus;
        this.flowM3s = flowM3s > 0 && Double.isFinite(flowM3s) ? flowM3s : Double.NaN;
        this.flowUncertaintyM3s = flowUncertaintyM3s >= 0 && Double.isFinite(flowUncertaintyM3s) ? flowUncertaintyM3s : Double.NaN;
        this.notes = notes == null ? "" : notes;
    }

    public static SamplingProfile hazRobPrototypeV1() {
        return new SamplingProfile(
                "hazrob-chemcore-chamber-v1", "1",
                0.09, 0.04, 0.15,
                0.04, 0.04,
                0.04, 0.04, 5.0, 0.28,
                FlowStatus.FLOW_UNCALIBRATED, Double.NaN, Double.NaN,
                "Rear 40x40 mm extraction fan; actual in-chamber airflow must be measured before flow compensation is validated."
        );
    }

    public SamplingProfile withFlow(FlowStatus status, double flowM3s, double uncertaintyM3s, String note) {
        return new SamplingProfile(profileId, version,
                chamberWidthM, chamberHeightM, chamberLengthM,
                inletWidthM, inletHeightM,
                fanWidthM, fanHeightM, fanVoltageV, fanCurrentA,
                status, flowM3s, uncertaintyM3s,
                note == null || note.isEmpty() ? notes : note);
    }

    public double grossVolumeM3() { return chamberWidthM * chamberHeightM * chamberLengthM; }
    public double grossVolumeLiters() { return grossVolumeM3() * 1000.0; }
    public double chamberCrossSectionM2() { return chamberWidthM * chamberHeightM; }
    public double inletAreaM2() { return inletWidthM * inletHeightM; }
    public double fanElectricalPowerW() { return fanVoltageV * fanCurrentA; }

    public boolean hasFlow() { return Double.isFinite(flowM3s) && flowM3s > 0; }
    public boolean flowValidated() { return flowStatus == FlowStatus.FLOW_CALIBRATED && hasFlow(); }

    public double inletVelocityMps() { return hasFlow() ? flowM3s / inletAreaM2() : Double.NaN; }
    public double chamberVelocityMps() { return hasFlow() ? flowM3s / chamberCrossSectionM2() : Double.NaN; }
    public double grossResidenceTimeS() { return hasFlow() ? grossVolumeM3() / flowM3s : Double.NaN; }

    public static double cfmToM3s(double cfm) { return cfm * 0.00047194745; }
    public static double litersPerMinuteToM3s(double lpm) { return lpm / 60000.0; }
    public static double m3sToLitersPerMinute(double m3s) { return m3s * 60000.0; }
}
