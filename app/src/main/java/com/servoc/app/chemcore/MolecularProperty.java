package com.servoc.app.chemcore;

/** Provenance-rich scalar/text molecular or physicochemical property record. */
public final class MolecularProperty {
    public final long chemicalUid;
    public final String key;
    public final double numericValue;
    public final String textValue;
    public final String unit;
    public final double temperatureC;
    public final double pressureKPa;
    public final String method;
    public final String sourceId;
    public final double uncertainty;
    public final String qualityTier;

    public MolecularProperty(long chemicalUid, String key, double numericValue, String textValue, String unit,
                             double temperatureC, double pressureKPa, String method, String sourceId,
                             double uncertainty, String qualityTier) {
        this.chemicalUid = chemicalUid;
        this.key = key == null ? "" : key;
        this.numericValue = numericValue;
        this.textValue = textValue;
        this.unit = unit == null ? "" : unit;
        this.temperatureC = temperatureC;
        this.pressureKPa = pressureKPa;
        this.method = method == null ? "" : method;
        this.sourceId = sourceId == null ? "" : sourceId;
        this.uncertainty = uncertainty;
        this.qualityTier = qualityTier == null ? "" : qualityTier;
    }

    public boolean hasNumeric() { return Double.isFinite(numericValue); }
    public boolean hasTemperature() { return Double.isFinite(temperatureC); }
    public boolean hasPressure() { return Double.isFinite(pressureKPa); }
}
