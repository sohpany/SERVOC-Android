package com.servoc.app.measurement;

/** Deterministic transient features for one normalized MOS/MQ response trace. */
public final class SignalFeatures {
    private SignalFeatures(){}
    public static final class Result {
        public final double baseline,peak,delta,initialSlope,timeTo50,timeTo90,auc,noiseStd;
        public final int validSamples;
        Result(double baseline,double peak,double delta,double initialSlope,double timeTo50,double timeTo90,double auc,double noiseStd,int validSamples){this.baseline=baseline;this.peak=peak;this.delta=delta;this.initialSlope=initialSlope;this.timeTo50=timeTo50;this.timeTo90=timeTo90;this.auc=auc;this.noiseStd=noiseStd;this.validSamples=validSamples;}
    }

    /** Time is seconds and response may be Rs/R0, conductance or another consistently oriented feature. */
    public static Result extract(double[] time,double[] response){
        if(time==null||response==null||time.length!=response.length||time.length<3)return new Result(Double.NaN,Double.NaN,Double.NaN,Double.NaN,Double.NaN,Double.NaN,Double.NaN,Double.NaN,0);
        int n=time.length,baseN=Math.max(1,Math.min(n/5,20)),valid=0;double base=0;
        for(int i=0;i<baseN;i++)if(Double.isFinite(response[i])){base+=response[i];valid++;}if(valid==0)return new Result(Double.NaN,Double.NaN,Double.NaN,Double.NaN,Double.NaN,Double.NaN,Double.NaN,Double.NaN,0);base/=valid;
        double min=base,max=base;int extrema=0;valid=0;for(int i=0;i<n;i++){double y=response[i];if(!Double.isFinite(y))continue;valid++;if(y<min){min=y;extrema=i;}if(y>max){max=y;extrema=i;}}
        double up=max-base,down=base-min,peak=up>=down?max:min,delta=peak-base,target50=base+.5*delta,target90=base+.9*delta,t50=crossing(time,response,target50,delta>=0),t90=crossing(time,response,target90,delta>=0);
        int slopeEnd=Math.max(1,Math.min(extrema,n-1));double slope=linearSlope(time,response,0,slopeEnd);double auc=0;for(int i=1;i<n;i++){if(!Double.isFinite(response[i-1])||!Double.isFinite(response[i])||!Double.isFinite(time[i-1])||!Double.isFinite(time[i]))continue;double dt=time[i]-time[i-1];if(dt>0)auc+=dt*((response[i-1]-base)+(response[i]-base))*.5;}
        double ss=0;int c=0;for(int i=0;i<baseN;i++)if(Double.isFinite(response[i])){double d=response[i]-base;ss+=d*d;c++;}double noise=c>1?Math.sqrt(ss/(c-1)):0;
        return new Result(base,peak,delta,slope,t50,t90,auc,noise,valid);
    }

    private static double crossing(double[] t,double[] y,double target,boolean rising){for(int i=1;i<y.length;i++){if(!Double.isFinite(y[i-1])||!Double.isFinite(y[i])||!Double.isFinite(t[i-1])||!Double.isFinite(t[i]))continue;boolean cross=rising?y[i]>=target:y[i]<=target;if(!cross)continue;double dy=y[i]-y[i-1];if(Math.abs(dy)<1e-12)return t[i];double f=(target-y[i-1])/dy;return t[i-1]+Math.max(0,Math.min(1,f))*(t[i]-t[i-1]);}return Double.NaN;}
    private static double linearSlope(double[] x,double[] y,int a,int b){double sx=0,sy=0,sxx=0,sxy=0;int n=0;for(int i=a;i<=b&&i<x.length;i++){if(!Double.isFinite(x[i])||!Double.isFinite(y[i]))continue;sx+=x[i];sy+=y[i];sxx+=x[i]*x[i];sxy+=x[i]*y[i];n++;}double d=n*sxx-sx*sx;return n>=2&&Math.abs(d)>1e-12?(n*sxy-sx*sy)/d:Double.NaN;}
}
