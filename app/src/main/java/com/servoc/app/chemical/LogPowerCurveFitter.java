package com.servoc.app.chemical;

/**
 * Fits datasheet/calibration points to the common MQ power-law model:
 * ratio = a * ppm^b, using ordinary least squares in log10 space.
 * The inverse form is ppm = A * ratio^B.
 */
public final class LogPowerCurveFitter {
    private LogPowerCurveFitter() {}

    public static final class Fit {
        public final double ratioA, ratioB;
        public final double ppmA, ppmB;
        public final double r2, rmseLog10;
        public final int points;
        Fit(double ratioA,double ratioB,double ppmA,double ppmB,double r2,double rmseLog10,int points){
            this.ratioA=ratioA;this.ratioB=ratioB;this.ppmA=ppmA;this.ppmB=ppmB;
            this.r2=r2;this.rmseLog10=rmseLog10;this.points=points;
        }
        public double ppm(double ratio){return ratio>0?ppmA*Math.pow(ratio,ppmB):Double.NaN;}
        public double ratio(double ppm){return ppm>0?ratioA*Math.pow(ppm,ratioB):Double.NaN;}
    }

    public static Fit fit(double[] ppm,double[] ratio){
        if(ppm==null||ratio==null||ppm.length!=ratio.length)throw new IllegalArgumentException("Aligned ppm and ratio arrays required");
        int n=0; double sx=0,sy=0,sxx=0,sxy=0;
        for(int i=0;i<ppm.length;i++){
            if(!Double.isFinite(ppm[i])||!Double.isFinite(ratio[i])||ppm[i]<=0||ratio[i]<=0)continue;
            double x=Math.log10(ppm[i]), y=Math.log10(ratio[i]);
            sx+=x;sy+=y;sxx+=x*x;sxy+=x*y;n++;
        }
        if(n<2)throw new IllegalArgumentException("Need at least two positive finite points");
        double den=n*sxx-sx*sx;
        if(Math.abs(den)<1e-15)throw new IllegalArgumentException("PPM points do not span a usable range");
        double b=(n*sxy-sx*sy)/den;
        double intercept=(sy-b*sx)/n;
        if(Math.abs(b)<1e-12)throw new IllegalArgumentException("Curve slope is too small for inversion");
        double a=Math.pow(10,intercept);

        double yMean=sy/n, ssRes=0, ssTot=0;
        for(int i=0;i<ppm.length;i++){
            if(!Double.isFinite(ppm[i])||!Double.isFinite(ratio[i])||ppm[i]<=0||ratio[i]<=0)continue;
            double x=Math.log10(ppm[i]), y=Math.log10(ratio[i]);
            double pred=intercept+b*x;
            double e=y-pred; ssRes+=e*e;
            double d=y-yMean; ssTot+=d*d;
        }
        double r2=ssTot>0?1.0-ssRes/ssTot:1.0;
        double rmse=Math.sqrt(ssRes/n);
        double invB=1.0/b;
        double invA=Math.pow(1.0/a,invB);
        return new Fit(a,b,invA,invB,r2,rmse,n);
    }
}
