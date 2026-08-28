package com.servoc.app.calibration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One controlled calibration session for one MQ sensor and one catalogue analyte.
 * Raw captures are retained. Exposure replicates are aggregated in log(Rs/R0) space
 * using a median, which is robust to occasional transient spikes.
 */
public final class CalibrationSession {
    public static final String BASELINE="baseline";
    public static final String EXPOSURE="exposure";
    public static final String RECOVERY="recovery";

    public static final class Capture {
        public final String phase;
        public final double knownPpm;
        public final double ratio;
        public final double tempC;
        public final double rh;
        public final boolean heaterProtocolValid;
        public final long timestampMs;

        public Capture(String phase,double knownPpm,double ratio,double tempC,double rh,
                       boolean heaterProtocolValid,long timestampMs){
            this.phase=phase==null?"":phase;
            this.knownPpm=knownPpm;
            this.ratio=ratio;
            this.tempC=tempC;
            this.rh=rh;
            this.heaterProtocolValid=heaterProtocolValid;
            this.timestampMs=timestampMs;
        }
    }

    public static final class Level {
        public final double ppm;
        public final double medianRatio;
        public final int replicates;
        public final double cvPercent;
        public final double medianTempC;
        public final double medianRh;

        Level(double ppm,double medianRatio,int replicates,double cvPercent,double medianTempC,double medianRh){
            this.ppm=ppm;this.medianRatio=medianRatio;this.replicates=replicates;
            this.cvPercent=cvPercent;this.medianTempC=medianTempC;this.medianRh=medianRh;
        }
    }

    public final String sessionId;
    public final int sensorId;
    public final int chemicalId;
    public final String referenceGas;
    public final long startedAtMs;
    private final List<Capture> captures=new ArrayList<>();

    public CalibrationSession(String sessionId,int sensorId,int chemicalId,String referenceGas,long startedAtMs){
        if(sensorId<0||sensorId>7)throw new IllegalArgumentException("Invalid MQ sensor id");
        if(chemicalId<0)throw new IllegalArgumentException("Invalid chemical id");
        this.sessionId=sessionId==null?"":sessionId;
        this.sensorId=sensorId;this.chemicalId=chemicalId;
        this.referenceGas=referenceGas==null?"":referenceGas;
        this.startedAtMs=startedAtMs;
    }

    public void add(Capture c){
        if(c==null)throw new IllegalArgumentException("capture required");
        if(!Double.isFinite(c.ratio)||c.ratio<=0)throw new IllegalArgumentException("Rs/R0 must be positive");
        if(EXPOSURE.equals(c.phase)&&(!Double.isFinite(c.knownPpm)||c.knownPpm<=0))
            throw new IllegalArgumentException("Exposure requires known ppm");
        captures.add(c);
    }

    public List<Capture> captures(){return Collections.unmodifiableList(captures);}
    public boolean hasPhase(String phase){for(Capture c:captures)if(phase.equals(c.phase))return true;return false;}

    public boolean allHeaterMetadataValid(){
        for(Capture c:captures) if(EXPOSURE.equals(c.phase)&&!c.heaterProtocolValid) return false;
        return true;
    }

    public List<Level> exposureLevels(){
        List<Double> ppmValues=new ArrayList<>();
        for(Capture c:captures){
            if(!EXPOSURE.equals(c.phase))continue;
            boolean exists=false;
            for(double p:ppmValues)if(nearlyEqual(p,c.knownPpm)){exists=true;break;}
            if(!exists)ppmValues.add(c.knownPpm);
        }
        Collections.sort(ppmValues);
        List<Level> out=new ArrayList<>();
        for(double p:ppmValues){
            List<Double> r=new ArrayList<>(),t=new ArrayList<>(),h=new ArrayList<>();
            for(Capture c:captures){
                if(EXPOSURE.equals(c.phase)&&nearlyEqual(p,c.knownPpm)){
                    r.add(c.ratio);
                    if(Double.isFinite(c.tempC))t.add(c.tempC);
                    if(Double.isFinite(c.rh))h.add(c.rh);
                }
            }
            out.add(new Level(p,geometricMedian(r),r.size(),cvPercent(r),median(t),median(h)));
        }
        return out;
    }

    public EmpiricalMqCurve buildCurve(String sourceNote){
        List<Level> levels=exposureLevels();
        double[] p=new double[levels.size()],r=new double[levels.size()];
        for(int i=0;i<levels.size();i++){p[i]=levels.get(i).ppm;r[i]=levels.get(i).medianRatio;}
        return new EmpiricalMqCurve(sensorId,chemicalId,p,r,System.currentTimeMillis(),referenceGas,sourceNote);
    }

    private static boolean nearlyEqual(double a,double b){
        double scale=Math.max(1.0,Math.max(Math.abs(a),Math.abs(b)));
        return Math.abs(a-b)<=1e-9*scale;
    }

    private static double geometricMedian(List<Double> v){
        if(v.isEmpty())return Double.NaN;
        List<Double> logs=new ArrayList<>();
        for(double x:v)if(Double.isFinite(x)&&x>0)logs.add(Math.log(x));
        if(logs.isEmpty())return Double.NaN;
        return Math.exp(median(logs));
    }

    private static double cvPercent(List<Double> v){
        if(v.isEmpty())return Double.NaN;
        double mean=0;int n=0;
        for(double x:v)if(Double.isFinite(x)){mean+=x;n++;}
        if(n==0||mean==0)return Double.NaN;
        mean/=n;
        if(n==1)return 0;
        double ss=0;
        for(double x:v)if(Double.isFinite(x)){double d=x-mean;ss+=d*d;}
        return 100.0*Math.sqrt(ss/(n-1))/Math.abs(mean);
    }

    private static double median(List<Double> v){
        if(v.isEmpty())return Double.NaN;
        List<Double> a=new ArrayList<>(v);Collections.sort(a);int n=a.size();
        return (n&1)==1?a.get(n/2):(a.get(n/2-1)+a.get(n/2))*0.5;
    }
}
