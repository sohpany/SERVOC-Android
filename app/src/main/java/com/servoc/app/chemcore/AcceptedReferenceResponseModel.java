package com.servoc.app.chemcore;

import com.servoc.app.chemical.GasEngine;
import com.servoc.app.chemical.LiteratureCurveRegistry;
import com.servoc.app.chemical.ManufacturerEvidenceMatrix;
import com.servoc.app.chemical.MqConditioningProfile;
import com.servoc.app.chemical.MqManufacturerOperatingContract;
import com.servoc.app.chemical.ResearchCurveRegistry;
import com.servoc.app.chemical.SensorOperatingEnvelope;

/**
 * Accepted provisional PPM reference model for HR069.
 *
 * Project decision 2026-09-29: available source-backed PPM libraries are usable now for
 * candidate fitting/ranking and provisional reference concentration estimates. They may be
 * recalibrated later. They remain external/reference evidence and never become M4 merely by
 * being enabled here.
 *
 * Source priority for the same physical sensor/analyte at a requested concentration:
 *  1) peer-reviewed literature curve inside its declared domain;
 *  2) inherited GasEngine curve when a finite manufacturer envelope exists;
 *  3) pinned ResearchCurveRegistry datasheet/community fit inside its declared domain.
 *
 * The model never extrapolates a source outside its declared concentration range and never
 * uses MQ-7/MQ-9 manufacturer/datasheet curves unless the required heater-cycle hardware is
 * valid. Their completed 48 h preheat still makes them available for raw/empirical captures.
 */
public final class AcceptedReferenceResponseModel implements ChemCoreResponseModel {
    public static final String VERSION="accepted-reference-ppm/1";
    public static final String ACCEPTANCE_STATE="ACCEPTED_PROVISIONAL_REFERENCE_PPM";
    public static final boolean M4=false;

    private final int sensor,chemical;
    private final String sensorKey;
    private final double minPpm,maxPpm;

    public AcceptedReferenceResponseModel(int sensor,int chemical){
        if(!eligible(sensor,chemical))throw new IllegalArgumentException("accepted source-backed response relation required");
        this.sensor=sensor;this.chemical=chemical;this.sensorKey=key(sensor);
        double[] d=domain(sensor,chemical);this.minPpm=d[0];this.maxPpm=d[1];
    }

    @Override public String sensorKey(){return sensorKey;}
    @Override public String version(){return VERSION;}
    @Override public ChemCoreDecision.EvidenceTier evidenceTier(){return ChemCoreDecision.EvidenceTier.M2_LITERATURE_OR_MANUFACTURER_SUPPORTED;}
    @Override public double minPpm(){return minPpm;}
    @Override public double maxPpm(){return maxPpm;}

    @Override public Estimate predict(double ppm,double temperatureC,double rh,SamplingProfile samplingProfile){
        if(!Double.isFinite(ppm)||ppm<=0)return unavailable("non-positive concentration");
        if(!MqConditioningProfile.initialPreheatSatisfied(sensor))return unavailable("48 h initial MQ preheat is not recorded as complete");
        if(MqManufacturerOperatingContract.requiresHeaterCycle(sensor)&&!MqConditioningProfile.manufacturerReferenceProtocolReady(sensor))
            return unavailable("manufacturer high/low heater-cycle protocol unavailable for this MQ channel");

        // Highest provenance: peer-reviewed fitted response relation.
        for(LiteratureCurveRegistry.Curve c:LiteratureCurveRegistry.CURVES){
            if(c.sensor!=sensor||c.chemical!=chemical||!c.inDeclaredRange(ppm))continue;
            double ratio=c.ratioFromPpm(ppm);
            if(valid(ratio))return estimate(ratio,0.20,"peer-reviewed "+c.sourceId+" • "+ACCEPTANCE_STATE+" • provisional reference PPM; not M4");
        }

        // Manufacturer-bounded inherited curve. No extrapolation beyond the evidence matrix.
        if(GasEngine.hasCurve(sensor,chemical)){
            SensorOperatingEnvelope.Check env=SensorOperatingEnvelope.check(sensor,chemical,ppm,
                    MqConditioningProfile.manufacturerReferenceProtocolReady(sensor));
            if(env.usableAsFingerprintEvidence()){
                double[] ratios=GasEngine.simulateRatios(chemical,ppm);
                double ratio=sensor<ratios.length?ratios[sensor]:Double.NaN;
                if(valid(ratio)){
                    double sigma=env.status==SensorOperatingEnvelope.PRIMARY_TARGET?0.22:
                            env.status==SensorOperatingEnvelope.MANUFACTURER_GRAPH_RESPONSE?0.27:0.32;
                    return estimate(ratio,sigma,"GasEngine + operating envelope • "+env.note+" • "+ACCEPTANCE_STATE+" • provisional reference PPM; not M4");
                }
            }
        }

        // Pinned datasheet/community graph fit, only inside the registry's declared range.
        for(ResearchCurveRegistry.Curve c:ResearchCurveRegistry.CURVES){
            if(c.sensor!=sensor||c.chemical!=chemical||!c.inDeclaredRange(ppm))continue;
            double ratio=c.ratioFromPpm(ppm);
            if(valid(ratio))return estimate(ratio,0.32,ResearchCurveRegistry.SOURCE+" • "+ACCEPTANCE_STATE+" • provisional reference PPM; not M4");
        }
        return unavailable("no accepted reference relation covers this concentration");
    }

    public static boolean eligible(int sensor,int chemical){
        if(sensor<0||sensor>7||!MqConditioningProfile.acquisitionReady(sensor))return false;
        if(MqManufacturerOperatingContract.requiresHeaterCycle(sensor)&&!MqConditioningProfile.manufacturerReferenceProtocolReady(sensor))return false;
        double[] d=domain(sensor,chemical);return d[0]>0&&d[1]>d[0];
    }

    public static boolean hasAnyAcceptedReference(int chemical){
        for(int sensor=0;sensor<8;sensor++)if(eligible(sensor,chemical))return true;
        return false;
    }

    public static int acceptedContributorCount(int chemical){
        int n=0;for(int sensor=0;sensor<8;sensor++)if(eligible(sensor,chemical))n++;return n;
    }

    private static double[] domain(int sensor,int chemical){
        double min=Double.POSITIVE_INFINITY,max=Double.NEGATIVE_INFINITY;
        if(sensor<0||sensor>7)return new double[]{Double.NaN,Double.NaN};
        if(!MqConditioningProfile.acquisitionReady(sensor))return new double[]{Double.NaN,Double.NaN};
        if(MqManufacturerOperatingContract.requiresHeaterCycle(sensor)&&!MqConditioningProfile.manufacturerReferenceProtocolReady(sensor))
            return new double[]{Double.NaN,Double.NaN};

        for(LiteratureCurveRegistry.Curve c:LiteratureCurveRegistry.CURVES)if(c.sensor==sensor&&c.chemical==chemical){
            min=Math.min(min,c.minPpm);max=Math.max(max,c.maxPpm);
        }
        if(GasEngine.hasCurve(sensor,chemical)){
            ManufacturerEvidenceMatrix.Evidence e=ManufacturerEvidenceMatrix.get(sensor,chemical);
            if(e!=null&&e.level!=ManufacturerEvidenceMatrix.NONE&&e.minPpm>0&&e.maxPpm>e.minPpm){
                min=Math.min(min,e.minPpm);max=Math.max(max,e.maxPpm);
            }
        }
        for(ResearchCurveRegistry.Curve c:ResearchCurveRegistry.CURVES)if(c.sensor==sensor&&c.chemical==chemical){
            min=Math.min(min,c.minPpm);max=Math.max(max,c.maxPpm);
        }
        return Double.isFinite(min)&&Double.isFinite(max)&&max>min?new double[]{min,max}:new double[]{Double.NaN,Double.NaN};
    }

    private Estimate estimate(double ratio,double sigma,String note){
        return new Estimate(true,Math.log10(ratio),sigma,true,note);
    }
    private Estimate unavailable(String note){return new Estimate(false,Double.NaN,0.32,false,note);}
    private static boolean valid(double x){return Double.isFinite(x)&&x>0;}
    private static String key(int sensor){String[] k={"mq2","mq3","mq4","mq5","mq7","mq8","mq9","mq135"};return sensor>=0&&sensor<k.length?k[sensor]:"mq?";}
}
