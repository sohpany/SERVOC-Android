package com.servoc.app.chemcore;

/** Source-backed ratio = a * ppm^b response prior with explicit validity domain. */
public final class PowerLawResponseModel implements ChemCoreResponseModel {
    private final String sensorKey,version,note;
    private final double a,b,minPpm,maxPpm,sigmaLog10,minTempC,maxTempC,minRh,maxRh;
    private final ChemCoreDecision.EvidenceTier tier;

    public PowerLawResponseModel(String sensorKey,String version,double a,double b,double minPpm,double maxPpm,
                                 double sigmaLog10,double minTempC,double maxTempC,double minRh,double maxRh,
                                 ChemCoreDecision.EvidenceTier tier,String note){
        if(sensorKey==null||sensorKey.trim().isEmpty())throw new IllegalArgumentException("sensorKey required");
        if(!(a>0)||!Double.isFinite(b)||!(minPpm>0)||!(maxPpm>minPpm))throw new IllegalArgumentException("invalid power-law model");
        this.sensorKey=sensorKey;this.version=version==null?"":version;this.a=a;this.b=b;this.minPpm=minPpm;this.maxPpm=maxPpm;
        this.sigmaLog10=Double.isFinite(sigmaLog10)&&sigmaLog10>0?sigmaLog10:0.15;
        this.minTempC=minTempC;this.maxTempC=maxTempC;this.minRh=minRh;this.maxRh=maxRh;
        this.tier=tier==null?ChemCoreDecision.EvidenceTier.M2_LITERATURE_OR_MANUFACTURER_SUPPORTED:tier;
        this.note=note==null?"":note;
    }

    @Override public String sensorKey(){return sensorKey;}
    @Override public String version(){return version;}
    @Override public ChemCoreDecision.EvidenceTier evidenceTier(){return tier;}
    @Override public double minPpm(){return minPpm;}
    @Override public double maxPpm(){return maxPpm;}

    @Override public Estimate predict(double ppm,double temperatureC,double rh,SamplingProfile samplingProfile){
        if(!(ppm>0)||ppm<minPpm||ppm>maxPpm)return new Estimate(false,Double.NaN,sigmaLog10,false,"ppm outside model range");
        boolean env=true;
        if(Double.isFinite(minTempC)&&Double.isFinite(maxTempC)&&Double.isFinite(temperatureC))env&=temperatureC>=minTempC&&temperatureC<=maxTempC;
        if(Double.isFinite(minRh)&&Double.isFinite(maxRh)&&Double.isFinite(rh))env&=rh>=minRh&&rh<=maxRh;
        double y=Math.log10(a)+b*Math.log10(ppm);
        return new Estimate(true,y,sigmaLog10,env,env?note:"environment outside documented model domain");
    }
}
