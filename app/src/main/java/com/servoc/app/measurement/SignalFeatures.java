package com.servoc.app.measurement;

import com.servoc.app.chemcore.ChemCoreFeaturePipeline;

/**
 * Legacy single-trace compatibility adapter.
 * Scientific feature extraction is owned by ChemCoreFeaturePipeline
 * (chemcore-features/1). Input response must be a positive Rs/R0 trace.
 */
@Deprecated
public final class SignalFeatures {
    private SignalFeatures(){}
    public static final class Result {
        public final double baseline,peak,delta,initialSlope,timeTo50,timeTo90,auc,noiseStd;
        public final int validSamples;
        Result(double baseline,double peak,double delta,double initialSlope,double timeTo50,double timeTo90,double auc,double noiseStd,int validSamples){this.baseline=baseline;this.peak=peak;this.delta=delta;this.initialSlope=initialSlope;this.timeTo50=timeTo50;this.timeTo90=timeTo90;this.auc=auc;this.noiseStd=noiseStd;this.validSamples=validSamples;}
    }

    /** Compatibility projection of the canonical log10(Rs/R0) feature contract. */
    public static Result extract(double[] time,double[] response){
        if(time==null||response==null||time.length!=response.length||time.length<3)
            return missing();
        double[][] ratios=new double[response.length][1];
        for(int i=0;i<response.length;i++)ratios[i][0]=response[i];
        ChemCoreFeaturePipeline.Result c=ChemCoreFeaturePipeline.extract(time,ratios);
        if(c.sensorCount==0)return missing();
        int valid=(int)Math.round(c.feature(0,0)*response.length);
        return new Result(
                c.feature(0,1), c.feature(0,7), c.feature(0,3), c.feature(0,8),
                c.feature(0,11), c.feature(0,12), c.feature(0,13), c.feature(0,15), valid);
    }

    private static Result missing(){return new Result(Double.NaN,Double.NaN,Double.NaN,Double.NaN,Double.NaN,Double.NaN,Double.NaN,Double.NaN,0);}
}
