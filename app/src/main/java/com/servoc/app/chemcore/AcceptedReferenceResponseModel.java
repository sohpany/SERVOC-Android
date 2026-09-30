package com.servoc.app.chemcore;

import com.servoc.app.chemical.GasEngine;
import com.servoc.app.chemical.LinearLiteratureResponseRegistry;
import com.servoc.app.chemical.LiteratureCurveRegistry;
import com.servoc.app.chemical.ManufacturerEvidenceMatrix;
import com.servoc.app.chemical.MqConditioningProfile;
import com.servoc.app.chemical.MqManufacturerOperatingContract;
import com.servoc.app.chemical.ResearchCurveRegistry;
import com.servoc.app.chemical.SensorOperatingEnvelope;

/**
 * Accepted provisional PPM reference model for HR069.
 *
 * Available source-backed PPM libraries are usable for bounded candidate fitting/ranking and
 * provisional reference concentration estimates. They remain M2/reference evidence and never
 * become M4 merely by being enabled here.
 */
public final class AcceptedReferenceResponseModel implements ChemCoreResponseModel {
    public static final String VERSION="accepted-reference-ppm/2";
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
    @Override public String modelId(){return VERSION+":"+sensorKey+":chem-"+chemical;}
    @Override public String evidenceFamilyId(){return "ACCEPTED_REFERENCE_RESPONSE";}
    @Override public boolean reportableQuantitative(){return false;}

    @Override public Estimate predict(double ppm,double temperatureC,double rh,SamplingProfile samplingProfile){
        if(!Double.isFinite(ppm)||ppm<=0)return unavailable("non-positive concentration");
        if(!MqConditioningProfile.initialPreheatSatisfied(sensor))return unavailable("48 h initial MQ preheat is not recorded as complete");
        if(MqManufacturerOperatingContract.requiresHeaterCycle(sensor)&&!MqConditioningProfile.manufacturerReferenceProtocolReady(sensor))
            return unavailable("manufacturer high/low heater-cycle protocol unavailable for this MQ channel");

        for(LiteratureCurveRegistry.Curve c:LiteratureCurveRegistry.CURVES){
            if(c.sensor!=sensor||c.chemical!=chemical||!c.inDeclaredRange(ppm))continue;
            double ratio=c.ratioFromPpm(ppm);
            if(valid(ratio))return estimate(ratio,0.20,ChemCoreEvidenceObservation.Type.LITERATURE_CURVE,
                    c.sourceId,"peer-reviewed "+c.sourceId+" • "+ACCEPTANCE_STATE+" • provisional reference PPM; not M4");
        }

        for(LinearLiteratureResponseRegistry.Curve c:LinearLiteratureResponseRegistry.CURVES){
            if(c.sensor!=sensor||c.chemical!=chemical||!c.inDeclaredRange(ppm))continue;
            double ratio=c.ratioFromPpm(ppm);
            if(valid(ratio)){
                String env="reference "+c.refTempC+" C/"+c.refRhPercent+"%RH/"+c.refO2Percent+"%O2; RL="+c.loadResistanceKohm+" kOhm";
                return estimate(ratio,0.25,ChemCoreEvidenceObservation.Type.LITERATURE_CURVE,c.sourceId,
                        "peer-reviewed "+c.sourceId+" • "+env+" • BME280 T/RH/P retained for transfer context • "+ACCEPTANCE_STATE+" • provisional reference PPM; not M4");
            }
        }

        if(GasEngine.hasCurve(sensor,chemical)){
            ManufacturerEvidenceMatrix.Evidence manufacturer=ManufacturerEvidenceMatrix.get(sensor,chemical);
            if(manufacturer!=null&&manufacturer.level!=ManufacturerEvidenceMatrix.NONE){
                SensorOperatingEnvelope.Check env=SensorOperatingEnvelope.check(sensor,chemical,ppm,
                        MqConditioningProfile.manufacturerReferenceProtocolReady(sensor));
                if(env.usableAsFingerprintEvidence()){
                    double[] ratios=GasEngine.simulateRatios(chemical,ppm);
                    double ratio=sensor<ratios.length?ratios[sensor]:Double.NaN;
                    if(valid(ratio)){
                        double sigma=env.status==SensorOperatingEnvelope.PRIMARY_TARGET?0.22:0.27;
                        String source=manufacturer.source==null||manufacturer.source.trim().isEmpty()?"manufacturer-evidence-matrix":manufacturer.source;
                        return estimate(ratio,sigma,ChemCoreEvidenceObservation.Type.MANUFACTURER_CURVE,source,
                                "GasEngine + manufacturer operating envelope • "+env.note+" • "+ACCEPTANCE_STATE+" • provisional reference PPM; not M4");
                    }
                }
            }
        }

        for(ResearchCurveRegistry.Curve c:ResearchCurveRegistry.CURVES){
            if(c.sensor!=sensor||c.chemical!=chemical||!c.inDeclaredRange(ppm))continue;
            double ratio=c.ratioFromPpm(ppm);
            if(valid(ratio))return estimate(ratio,0.32,ChemCoreEvidenceObservation.Type.RESEARCH_CURVE,
                    ResearchCurveRegistry.SOURCE,ResearchCurveRegistry.SOURCE+" • "+ACCEPTANCE_STATE+" • provisional reference PPM; not M4");
        }
        return unavailable("no accepted reference relation covers this concentration");
    }

    public static boolean eligible(int sensor,int chemical){
        if(sensor<0||sensor>7||!MqConditioningProfile.acquisitionReady(sensor))return false;
        if(MqManufacturerOperatingContract.requiresHeaterCycle(sensor)&&!MqConditioningProfile.manufacturerReferenceProtocolReady(sensor))return false;
        double[] d=domain(sensor,chemical);return d[0]>0&&d[1]>d[0];
    }

    public static boolean hasAnyAcceptedReference(int chemical){for(int sensor=0;sensor<8;sensor++)if(eligible(sensor,chemical))return true;return false;}
    public static int acceptedContributorCount(int chemical){int n=0;for(int sensor=0;sensor<8;sensor++)if(eligible(sensor,chemical))n++;return n;}

    private static double[] domain(int sensor,int chemical){
        double min=Double.POSITIVE_INFINITY,max=Double.NEGATIVE_INFINITY;
        if(sensor<0||sensor>7)return new double[]{Double.NaN,Double.NaN};
        if(!MqConditioningProfile.acquisitionReady(sensor))return new double[]{Double.NaN,Double.NaN};
        if(MqManufacturerOperatingContract.requiresHeaterCycle(sensor)&&!MqConditioningProfile.manufacturerReferenceProtocolReady(sensor))return new double[]{Double.NaN,Double.NaN};
        for(LiteratureCurveRegistry.Curve c:LiteratureCurveRegistry.CURVES)if(c.sensor==sensor&&c.chemical==chemical){min=Math.min(min,c.minPpm);max=Math.max(max,c.maxPpm);}
        for(LinearLiteratureResponseRegistry.Curve c:LinearLiteratureResponseRegistry.CURVES)if(c.sensor==sensor&&c.chemical==chemical){min=Math.min(min,c.minPpm);max=Math.max(max,c.maxPpm);}
        if(GasEngine.hasCurve(sensor,chemical)){
            ManufacturerEvidenceMatrix.Evidence e=ManufacturerEvidenceMatrix.get(sensor,chemical);
            if(e!=null&&e.level!=ManufacturerEvidenceMatrix.NONE&&e.minPpm>0&&e.maxPpm>e.minPpm){min=Math.min(min,e.minPpm);max=Math.max(max,e.maxPpm);}
        }
        for(ResearchCurveRegistry.Curve c:ResearchCurveRegistry.CURVES)if(c.sensor==sensor&&c.chemical==chemical){min=Math.min(min,c.minPpm);max=Math.max(max,c.maxPpm);}
        return Double.isFinite(min)&&Double.isFinite(max)&&max>min?new double[]{min,max}:new double[]{Double.NaN,Double.NaN};
    }

    private Estimate estimate(double ratio,double sigma,ChemCoreEvidenceObservation.Type type,String source,String note){
        String src=source==null?"":source.trim();String family=type.name();String independence=family+":"+src;
        return new Estimate(true,Math.log10(ratio),sigma,true,note,type,family,modelId(),src,independence);
    }
    private Estimate unavailable(String note){return new Estimate(false,Double.NaN,0.32,false,note);}
    private static boolean valid(double x){return Double.isFinite(x)&&x>0;}
    private static String key(int sensor){String[] k={"mq2","mq3","mq4","mq5","mq7","mq8","mq9","mq135"};return sensor>=0&&sensor<k.length?k[sensor]:"mq?";}
}
