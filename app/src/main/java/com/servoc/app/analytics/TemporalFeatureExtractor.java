package com.servoc.app.analytics;

import java.util.Arrays;

/**
 * Extracts time-domain e-nose features from an Rs/R0 time series.
 * Input shape: samples x sensors. Designed for the eight-sensor MQ array.
 * Features per sensor: validity, baseline, steady-state, delta, min, max, slope and AUC.
 */
public final class TemporalFeatureExtractor {
    private TemporalFeatureExtractor() {}

    public static final int FEATURES_PER_SENSOR = 8;

    public static double[] extract(double[][] ratios) {
        if (ratios == null || ratios.length == 0) return new double[0];
        int sensors = 0;
        for (double[] row : ratios) if (row != null) sensors = Math.max(sensors, row.length);
        if (sensors == 0) return new double[0];

        double[] out = new double[sensors * FEATURES_PER_SENSOR];
        for (int s=0; s<sensors; s++) {
            double[] x = new double[ratios.length];
            int valid=0;
            for (int i=0; i<ratios.length; i++) {
                double v = ratios[i] != null && s < ratios[i].length ? ratios[i][s] : Double.NaN;
                if (Double.isFinite(v) && v > 0) {
                    x[i] = Math.log10(v);
                    valid++;
                } else x[i] = Double.NaN;
            }
            int o=s*FEATURES_PER_SENSOR;
            out[o] = ratios.length == 0 ? 0 : (double)valid / ratios.length;
            if (valid < 3) continue;

            double baseline = robustWindowMedian(x, 0.0, 0.15);
            double steady = robustWindowMedian(x, 0.75, 1.0);
            double min=Double.POSITIVE_INFINITY, max=Double.NEGATIVE_INFINITY;
            double auc=0, sx=0, sy=0, sxx=0, sxy=0;
            int n=0;
            for (int i=0; i<x.length; i++) {
                if (!Double.isFinite(x[i])) continue;
                min=Math.min(min,x[i]); max=Math.max(max,x[i]);
                auc += Math.abs(x[i]-baseline);
                double t = x.length <= 1 ? 0 : (double)i/(x.length-1);
                sx+=t; sy+=x[i]; sxx+=t*t; sxy+=t*x[i]; n++;
            }
            double den=n*sxx-sx*sx;
            double slope=Math.abs(den)<1e-12?0:(n*sxy-sx*sy)/den;

            out[o+1]=finiteOrZero(baseline);
            out[o+2]=finiteOrZero(steady);
            out[o+3]=finiteOrZero(steady-baseline);
            out[o+4]=finiteOrZero(min);
            out[o+5]=finiteOrZero(max);
            out[o+6]=finiteOrZero(slope);
            out[o+7]=n==0?0:auc/n;
        }
        return out;
    }

    private static double robustWindowMedian(double[] x, double fromFraction, double toFraction) {
        int start=Math.max(0,Math.min(x.length-1,(int)Math.floor(fromFraction*x.length)));
        int end=Math.max(start+1,Math.min(x.length,(int)Math.ceil(toFraction*x.length)));
        double[] tmp=new double[end-start]; int n=0;
        for(int i=start;i<end;i++) if(Double.isFinite(x[i])) tmp[n++]=x[i];
        if(n==0) return Double.NaN;
        tmp=Arrays.copyOf(tmp,n); Arrays.sort(tmp);
        return (n&1)==1?tmp[n/2]:(tmp[n/2-1]+tmp[n/2])*0.5;
    }

    private static double finiteOrZero(double x) { return Double.isFinite(x)?x:0; }
}
