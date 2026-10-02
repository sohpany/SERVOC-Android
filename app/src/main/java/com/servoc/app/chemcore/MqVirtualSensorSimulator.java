package com.servoc.app.chemcore;

import com.servoc.app.chemical.GasEngine;

/**
 * Research-grade virtual MQ sensor / digital-twin pilot.
 *
 * The forward chemical response comes only from the accepted source-backed response layer. The
 * simulator then converts the predicted Rs/R0 into an idealized divider voltage and ADC count.
 * Optional drift and Gaussian-noise terms operate in log10(Rs/R0), which matches the analytical
 * geometry used by ChemCore. This is an empirical-electrical simulator, not a first-principles
 * SnO2 adsorption/kinetics model and never creates M4 calibration evidence.
 */
public final class MqVirtualSensorSimulator {
    public static final String VERSION="mq-digital-twin/1-mq3-pilot";
    public static final int PILOT_SENSOR=GasEngine.MQ3;

    private MqVirtualSensorSimulator() {}

    public static final class Request {
        public final int sensor,chemical;
        public final double ppm,temperatureC,rhPct;
        public final double r0Kohm,loadResistanceKohm,supplyV,adcReferenceV;
        public final int adcMaxCount;
        /** Additive systematic offset in log10(Rs/R0). */
        public final double driftLog10;
        /** 1-sigma random noise in log10(Rs/R0). */
        public final double noiseSigmaLog10;
        /** Deterministic standard-normal draw supplied by the caller/test harness. */
        public final double noiseZ;

        public Request(int sensor,int chemical,double ppm,double temperatureC,double rhPct,
                       double r0Kohm,double loadResistanceKohm,double supplyV,double adcReferenceV,
                       int adcMaxCount,double driftLog10,double noiseSigmaLog10,double noiseZ){
            this.sensor=sensor;this.chemical=chemical;this.ppm=ppm;this.temperatureC=temperatureC;this.rhPct=rhPct;
            this.r0Kohm=r0Kohm;this.loadResistanceKohm=loadResistanceKohm;this.supplyV=supplyV;
            this.adcReferenceV=adcReferenceV;this.adcMaxCount=adcMaxCount;this.driftLog10=driftLog10;
            this.noiseSigmaLog10=noiseSigmaLog10;this.noiseZ=noiseZ;
        }

        public static Request mq3(int chemical,double ppm){
            return new Request(PILOT_SENSOR,chemical,ppm,25.0,50.0,20.0,10.0,5.0,4.096,32767,0.0,0.0,0.0);
        }
    }

    public static final class Result {
        public final boolean available,insideDomain;
        public final int sensor,chemical;
        public final double ppm,meanLog10Ratio,simulatedLog10Ratio,ratio,rsKohm,vOut,adcInputV;
        public final int adcCount;
        public final double sourceSigmaLog10,appliedDriftLog10,appliedNoiseLog10;
        public final ChemCoreEvidenceObservation.Type evidenceType;
        public final String sourceId,note;

        Result(boolean available,boolean insideDomain,int sensor,int chemical,double ppm,
               double meanLog10Ratio,double simulatedLog10Ratio,double ratio,double rsKohm,double vOut,
               double adcInputV,int adcCount,double sourceSigmaLog10,double appliedDriftLog10,
               double appliedNoiseLog10,ChemCoreEvidenceObservation.Type evidenceType,String sourceId,String note){
            this.available=available;this.insideDomain=insideDomain;this.sensor=sensor;this.chemical=chemical;this.ppm=ppm;
            this.meanLog10Ratio=meanLog10Ratio;this.simulatedLog10Ratio=simulatedLog10Ratio;this.ratio=ratio;
            this.rsKohm=rsKohm;this.vOut=vOut;this.adcInputV=adcInputV;this.adcCount=adcCount;
            this.sourceSigmaLog10=sourceSigmaLog10;this.appliedDriftLog10=appliedDriftLog10;
            this.appliedNoiseLog10=appliedNoiseLog10;this.evidenceType=evidenceType;this.sourceId=sourceId==null?"":sourceId;
            this.note=note==null?"":note;
        }
    }

    public static Result simulate(Request r){
        if(r==null)return unavailable(null,"request required");
        if(r.sensor<0||r.sensor>7)return unavailable(r,"invalid MQ sensor index");
        if(!Double.isFinite(r.ppm)||r.ppm<=0)return unavailable(r,"positive ppm required");
        if(!AcceptedReferenceResponseModel.eligible(r.sensor,r.chemical))return unavailable(r,"no accepted source-backed response relation for sensor/analyte");

        AcceptedReferenceResponseModel model;
        try{model=new AcceptedReferenceResponseModel(r.sensor,r.chemical);}catch(IllegalArgumentException e){return unavailable(r,e.getMessage());}
        ChemCoreResponseModel.Estimate e=model.predict(r.ppm,r.temperatureC,r.rhPct,SamplingProfile.hazRobPrototypeV1());
        if(e==null||!e.available||!Double.isFinite(e.meanLog10Ratio))return unavailable(r,e==null?"reference prediction unavailable":e.note);

        double drift=Double.isFinite(r.driftLog10)?r.driftLog10:0.0;
        double sigma=Double.isFinite(r.noiseSigmaLog10)&&r.noiseSigmaLog10>0?r.noiseSigmaLog10:0.0;
        double z=Double.isFinite(r.noiseZ)?r.noiseZ:0.0;
        double noise=sigma*z;
        double logRatio=e.meanLog10Ratio+drift+noise;
        double ratio=Math.pow(10.0,logRatio);
        if(!Double.isFinite(ratio)||ratio<=0)return unavailable(r,"non-finite simulated Rs/R0");

        double rs=Double.isFinite(r.r0Kohm)&&r.r0Kohm>0?r.r0Kohm*ratio:Double.NaN;
        double vOut=Double.NaN,adcV=Double.NaN;int adc=-1;
        if(Double.isFinite(rs)&&rs>0&&Double.isFinite(r.loadResistanceKohm)&&r.loadResistanceKohm>0&&Double.isFinite(r.supplyV)&&r.supplyV>0){
            // Ideal divider: sensor resistance to Vc, load resistance to ground, Vout across RL.
            vOut=r.supplyV*r.loadResistanceKohm/(r.loadResistanceKohm+rs);
            if(Double.isFinite(r.adcReferenceV)&&r.adcReferenceV>0&&r.adcMaxCount>0){
                adcV=Math.max(0.0,Math.min(vOut,r.adcReferenceV));
                adc=(int)Math.round(adcV/r.adcReferenceV*r.adcMaxCount);
            }
        }
        String note=VERSION+"; empirical source-backed forward response + ideal resistor-divider conversion; not M4; "+e.note;
        return new Result(true,e.insideDomain,r.sensor,r.chemical,r.ppm,e.meanLog10Ratio,logRatio,ratio,rs,vOut,adcV,adc,
                e.sigmaLog10,drift,noise,e.evidenceType,e.sourceId,note);
    }

    private static Result unavailable(Request r,String why){
        return new Result(false,false,r==null?-1:r.sensor,r==null?-1:r.chemical,r==null?Double.NaN:r.ppm,
                Double.NaN,Double.NaN,Double.NaN,Double.NaN,Double.NaN,Double.NaN,-1,Double.NaN,0,0,null,"",why);
    }
}
