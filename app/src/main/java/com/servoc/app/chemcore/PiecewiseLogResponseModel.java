package com.servoc.app.chemcore;

import java.util.Arrays;

/**
 * Forward log-log interpolation through monotonic source/calibration points.
 * Extrapolation is deliberately disabled.
 */
public final class PiecewiseLogResponseModel implements ChemCoreResponseModel {
    private final String sensorKey,version,note;
    private final double[] ppm,ratio;
    private final double sigmaLog10,minTempC,maxTempC,minRh,maxRh;
    private final ChemCoreDecision.EvidenceTier tier;

    public PiecewiseLogResponseModel(String sensorKey,String version,double[] ppm,double[] ratio,double sigmaLog10,
                                     double minTempC,double maxTempC,double minRh,double maxRh,
                                     ChemCoreDecision.EvidenceTier tier,String note){
        if(sensorKey==null||sensorKey.trim().isEmpty())throw new IllegalArgumentException("sensorKey required");
        if(ppm==null||ratio==null||ppm.length!=ratio.length||ppm.length<2)throw new IllegalArgumentException("at least two paired points required");
        Integer[] order=new Integer[ppm.length];for(int i=0;i<order.length;i++)order[i]=i;
        Arrays.sort(order,(a,b)->Double.compare(ppm[a],ppm[b]));
        this.ppm=new double[ppm.length];this.ratio=new double[ratio.length];
        for(int i=0;i<order.length;i++){double p=ppm[order[i]],r=ratio[order[i]];if(!(p>0)||!(r>0)||!Double.isFinite(p)||!Double.isFinite(r))throw new IllegalArgumentException("positive finite points required");this.ppm[i]=p;this.ratio[i]=r;if(i>0&&this.ppm[i]<=this.ppm[i-1])throw new IllegalArgumentException("unique concentration levels required");}
        this.sensorKey=sensorKey;this.version=version==null?"":version;this.sigmaLog10=Double.isFinite(sigmaLog10)&&sigmaLog10>0?sigmaLog10:0.10;
        this.minTempC=minTempC;this.maxTempC=maxTempC;this.minRh=minRh;this.maxRh=maxRh;
        this.tier=tier==null?ChemCoreDecision.EvidenceTier.M4_HAZROB_VALIDATED:tier;this.note=note==null?"":note;
    }

    @Override public String sensorKey(){return sensorKey;}
    @Override public String version(){return version;}
    @Override public ChemCoreDecision.EvidenceTier evidenceTier(){return tier;}
    @Override public double minPpm(){return ppm[0];}
    @Override public double maxPpm(){return ppm[ppm.length-1];}

    @Override public Estimate predict(double concentration,double temperatureC,double rh,SamplingProfile samplingProfile){
        if(!(concentration>=minPpm()&&concentration<=maxPpm()))return new Estimate(false,Double.NaN,sigmaLog10,false,"no extrapolation outside calibrated/source range");
        int hi=Arrays.binarySearch(ppm,concentration);double y;
        if(hi>=0)y=Math.log10(ratio[hi]);
        else{hi=-hi-1;int lo=Math.max(0,hi-1);hi=Math.min(ppm.length-1,hi);if(lo==hi)y=Math.log10(ratio[lo]);else{double x=Math.log10(concentration),x0=Math.log10(ppm[lo]),x1=Math.log10(ppm[hi]);double y0=Math.log10(ratio[lo]),y1=Math.log10(ratio[hi]);double f=(x-x0)/(x1-x0);y=y0+f*(y1-y0);}}
        boolean env=true;if(Double.isFinite(minTempC)&&Double.isFinite(maxTempC)&&Double.isFinite(temperatureC))env&=temperatureC>=minTempC&&temperatureC<=maxTempC;
        if(Double.isFinite(minRh)&&Double.isFinite(maxRh)&&Double.isFinite(rh))env&=rh>=minRh&&rh<=maxRh;
        return new Estimate(true,y,sigmaLog10,env,env?note:"environment outside documented model domain");
    }
}
