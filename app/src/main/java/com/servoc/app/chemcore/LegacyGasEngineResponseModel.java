package com.servoc.app.chemcore;

import com.servoc.app.chemical.GasEngine;
import com.servoc.app.chemical.ManufacturerEvidenceMatrix;

/**
 * Audited forward adapter for inherited GasEngine power-law curves, but only when the
 * current manufacturer evidence matrix supplies a finite analyte/sensor concentration range.
 * The inherited equation remains M2 evidence, never device calibration.
 */
public final class LegacyGasEngineResponseModel implements ChemCoreResponseModel {
    private final int sensor,chemical;
    private final String sensorKey,version,note;
    private final double minPpm,maxPpm;

    public LegacyGasEngineResponseModel(int sensor,int chemical){
        if(sensor<0||sensor>7||!GasEngine.hasCurve(sensor,chemical))throw new IllegalArgumentException("registered GasEngine curve required");
        ManufacturerEvidenceMatrix.Evidence e=ManufacturerEvidenceMatrix.get(sensor,chemical);
        if(e==null||e.level==ManufacturerEvidenceMatrix.NONE||!(e.minPpm>0)||!(e.maxPpm>e.minPpm))
            throw new IllegalArgumentException("finite manufacturer evidence domain required");
        this.sensor=sensor;this.chemical=chemical;this.sensorKey=key(sensor);this.version="legacy-gasengine-forward/1";
        this.minPpm=e.minPpm;this.maxPpm=e.maxPpm;
        this.note=e.source+" • "+e.note+" • inherited curve used as M2 prior; not device calibration";
    }

    @Override public String sensorKey(){return sensorKey;}
    @Override public String version(){return version;}
    @Override public ChemCoreDecision.EvidenceTier evidenceTier(){return ChemCoreDecision.EvidenceTier.M2_LITERATURE_OR_MANUFACTURER_SUPPORTED;}
    @Override public double minPpm(){return minPpm;}
    @Override public double maxPpm(){return maxPpm;}

    @Override public Estimate predict(double ppm,double temperatureC,double rh,SamplingProfile samplingProfile){
        if(!(ppm>=minPpm&&ppm<=maxPpm))return new Estimate(false,Double.NaN,.22,false,"ppm outside manufacturer-supported domain");
        double[] ratios=GasEngine.simulateRatios(chemical,ppm);double r=sensor<ratios.length?ratios[sensor]:Double.NaN;
        if(!(r>0)||!Double.isFinite(r))return new Estimate(false,Double.NaN,.22,false,"legacy curve did not produce finite response");
        // No source-backed T/RH compensation is asserted by this adapter; uncertainty remains deliberately broad.
        return new Estimate(true,Math.log10(r),.22,true,note);
    }

    public static boolean eligible(int sensor,int chemical){
        if(!GasEngine.hasCurve(sensor,chemical))return false;ManufacturerEvidenceMatrix.Evidence e=ManufacturerEvidenceMatrix.get(sensor,chemical);
        return e!=null&&e.level!=ManufacturerEvidenceMatrix.NONE&&e.minPpm>0&&e.maxPpm>e.minPpm;
    }

    private static String key(int sensor){String[] k={"mq2","mq3","mq4","mq5","mq7","mq8","mq9","mq135"};return sensor>=0&&sensor<k.length?k[sensor]:"mq?";}
}
