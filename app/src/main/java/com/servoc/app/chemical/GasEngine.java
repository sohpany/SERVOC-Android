package com.servoc.app.chemical;

import com.servoc.app.exposure.ExposureLimits;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Direct Android port of GasProfiles.h + GasAnalyzer.cpp. */
public final class GasEngine {
    private GasEngine() {}

    public static final int MQ2=0, MQ3=1, MQ4=2, MQ5=3, MQ7=4, MQ8=5, MQ9=6, MQ135=7;
    public static final int NONE=0, SUSPECTED=1, STRONG=2, HIGH=3;

    private static final class Curve {
        final int sensor, chem; final double a,b;
        Curve(int sensor,int chem,double a,double b){this.sensor=sensor;this.chem=chem;this.a=a;this.b=b;}
    }

    private static final Curve[] CURVES={
        new Curve(MQ2,75,987.99,-2.162), new Curve(MQ2,135,574.25,-2.222), new Curve(MQ2,20,36974,-3.109), new Curve(MQ2,103,3616.1,-2.675), new Curve(MQ2,78,658.71,-2.168),
        new Curve(MQ3,135,44771,-3.245), new Curve(MQ3,20,521853,-3.821), new Curve(MQ3,103,.3934,-1.504), new Curve(MQ3,34,4.8387,-2.680), new Curve(MQ3,84,7585.3,-2.849),
        new Curve(MQ4,135,3811.9,-3.113), new Curve(MQ4,76,1012.7,-2.786), new Curve(MQ4,20,2.0e14,-19.05), new Curve(MQ4,103,6.0e10,-14.01), new Curve(MQ4,139,3.0e7,-8.308),
        new Curve(MQ5,75,1163.8,-3.874), new Curve(MQ5,135,80.897,-2.431), new Curve(MQ5,76,177.65,-2.560), new Curve(MQ5,20,491204,-5.826), new Curve(MQ5,103,97124,-4.918),
        new Curve(MQ7,75,69.014,-1.374), new Curve(MQ7,135,7.0e8,-7.703), new Curve(MQ7,76,6.0e13,-10.54), new Curve(MQ7,20,99.042,-1.518), new Curve(MQ7,103,4.0e16,-12.35),
        new Curve(MQ8,75,976.97,-.688), new Curve(MQ8,135,1.0e7,-3.123), new Curve(MQ8,76,8.0e13,-6.666), new Curve(MQ8,20,2.0e18,-8.074), new Curve(MQ8,103,76101,-1.860),
        new Curve(MQ9,135,1000.5,-2.186), new Curve(MQ9,76,4269.6,-2.648), new Curve(MQ9,20,599.65,-2.244),
        new Curve(MQ135,20,605.18,-3.937), new Curve(MQ135,103,77.255,-3.180), new Curve(MQ135,23,110.47,-2.862), new Curve(MQ135,95,44.947,-3.445), new Curve(MQ135,21,102.20,-2.473), new Curve(MQ135,110,34.668,-3.369)
    };

    public static final int[] SUPPORTED={75,135,20,103,78,76,139,34,84,23,95,21,110};

    public static int curveCount(){ return CURVES.length; }

    /** One raw per-sensor estimate before robust consensus or evidence filtering. */
    public static final class Estimate {
        public final int sensor;
        public final int chemical;
        public final double ppm;
        Estimate(int sensor, int chemical, double ppm) {
            this.sensor=sensor; this.chemical=chemical; this.ppm=ppm;
        }
    }

    /** Exposes the legacy curve estimates so the validated engine can audit each contributor. */
    public static List<Estimate> estimatesForChemical(double[] ratios, int chemical) {
        List<Estimate> out=new ArrayList<>();
        if(ratios==null) return out;
        for(Curve c:CURVES){
            if(c.chem!=chemical || c.sensor>=ratios.length) continue;
            double ratio=ratios[c.sensor];
            if(!Double.isFinite(ratio)||ratio<=0) continue;
            double ppm=c.a*Math.pow(ratio,c.b);
            if(Double.isFinite(ppm)&&ppm>0) out.add(new Estimate(c.sensor,c.chem,ppm));
        }
        return out;
    }

    /** Generates deterministic synthetic Rs/R0 ratios for offline UI/analysis testing only. */
    public static double[] simulateRatios(int chem,double ppm){
        double[] ratios=new double[8]; Arrays.fill(ratios,Double.NaN);
        if(!Double.isFinite(ppm)||ppm<=0) return ratios;
        for(Curve c:CURVES){
            if(c.chem!=chem || c.sensor>=ratios.length || c.a<=0 || c.b==0) continue;
            double ratio=Math.pow(ppm/c.a,1.0/c.b);
            if(Double.isFinite(ratio)&&ratio>0) ratios[c.sensor]=ratio;
        }
        return ratios;
    }

    public static final class Result {
        public final int chem, contributors, confidence, risk;
        public final double ppm, agreement;
        Result(int chem,double ppm,double agreement,int contributors,int confidence,int risk){
            this.chem=chem;this.ppm=ppm;this.agreement=agreement;this.contributors=contributors;this.confidence=confidence;this.risk=risk;
        }
    }

    public static List<Result> analyze(double[] ratios){
        List<Result> out=new ArrayList<>();
        if(ratios==null) return out;
        for(int chem:SUPPORTED){ Result r=analyzeOne(ratios,chem); if(r!=null) out.add(r); }
        return out;
    }

    public static Result analyzeOne(double[] ratios,int chem){
        double[] logs=new double[8]; int n=0;
        for(Curve c:CURVES){
            if(c.chem!=chem || c.sensor>=ratios.length) continue;
            double ratio=ratios[c.sensor];
            if(!Double.isFinite(ratio)||ratio<=0) continue;
            double ppm=c.a*Math.pow(ratio,c.b);
            if(!Double.isFinite(ppm)||ppm<=0) continue;
            logs[n++]=Math.log10(ppm); if(n>=logs.length) break;
        }
        if(n==0) return null;
        double center=median(Arrays.copyOf(logs,n));
        double ppm=Math.pow(10,center);
        double[] dev=new double[n]; for(int i=0;i<n;i++) dev[i]=Math.abs(logs[i]-center);
        double mad=median(dev);
        double base=n>=4?100.0:n*25.0;
        double agreement=Math.max(0,Math.min(100,base*Math.exp(-1.8*mad)));
        int confidence=(n>=3&&agreement>=70)?HIGH:(n>=2&&agreement>=45)?STRONG:SUSPECTED;
        int risk=ExposureLimits.screeningRisk(chem,ppm);
        return new Result(chem,ppm,agreement,n,confidence,risk);
    }

    private static double median(double[] v){
        if(v.length==0) return Double.NaN;
        Arrays.sort(v); int n=v.length;
        return (n&1)==1?v[n/2]:(v[n/2-1]+v[n/2])*.5;
    }
}
