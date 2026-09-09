package com.servoc.app.chemcore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Selects a Mahalanobis open-set threshold from independent known-vs-unknown validation vectors.
 * Training vectors are used only to estimate the feature-domain model; threshold metrics are
 * calculated exclusively from the supplied validation sets.
 */
public final class OpenSetThresholdCalibrator {
    private OpenSetThresholdCalibrator() {}

    public static final String PROTOCOL = "independent-known-vs-heldout-unknown/1";

    public static final class Result {
        public final MahalanobisModel model;
        public final double threshold;
        public final double knownRecall;
        public final double unknownRecall;
        public final double balancedAccuracy;
        public final double falseRejectRate;
        public final double falseAcceptRate;
        public final int knownCount;
        public final int unknownCount;
        public final String protocol;

        Result(MahalanobisModel model,double threshold,double knownRecall,double unknownRecall,
               int knownCount,int unknownCount){
            this.model=model;this.threshold=threshold;this.knownRecall=knownRecall;this.unknownRecall=unknownRecall;
            this.balancedAccuracy=(knownRecall+unknownRecall)/2.0;
            this.falseRejectRate=1.0-knownRecall;this.falseAcceptRate=1.0-unknownRecall;
            this.knownCount=knownCount;this.unknownCount=unknownCount;this.protocol=PROTOCOL;
        }

        public ChemCoreOpenSetGate gate(double minimumCoverage){
            return ChemCoreOpenSetGate.fromValidatedThreshold(model,threshold,minimumCoverage);
        }
    }

    /**
     * Chooses the threshold maximizing balanced accuracy while enforcing a requested minimum
     * known-class recall where feasible. Ties prefer the lower threshold to reduce false accepts.
     */
    public static Result calibrate(double[][] trainingKnown,
                                   double[][] validationKnown,
                                   double[][] validationUnknown,
                                   double ridgeFraction,
                                   double minimumKnownRecall){
        MahalanobisModel model=MahalanobisModel.fit(trainingKnown,ridgeFraction);
        double[] kd=distances(model,validationKnown),ud=distances(model,validationUnknown);
        if(kd.length<2||ud.length<2)throw new IllegalArgumentException("at least two finite validation vectors per class required");
        double minRecall=Double.isFinite(minimumKnownRecall)?Math.max(0,Math.min(1,minimumKnownRecall)):0.90;

        double[] candidates=candidateThresholds(kd,ud);
        Result best=null,bestFallback=null;
        for(double t:candidates){
            double kr=knownRecall(kd,t),ur=unknownRecall(ud,t);
            Result r=new Result(model,t,kr,ur,kd.length,ud.length);
            if(better(r,bestFallback))bestFallback=r;
            if(kr+1e-12>=minRecall&&better(r,best))best=r;
        }
        return best!=null?best:bestFallback;
    }

    private static boolean better(Result a,Result b){
        if(a==null)return false;if(b==null)return true;
        if(a.balancedAccuracy>b.balancedAccuracy+1e-12)return true;
        if(Math.abs(a.balancedAccuracy-b.balancedAccuracy)<=1e-12){
            if(a.unknownRecall>b.unknownRecall+1e-12)return true;
            if(Math.abs(a.unknownRecall-b.unknownRecall)<=1e-12)return a.threshold<b.threshold;
        }
        return false;
    }

    private static double[] distances(MahalanobisModel model,double[][] rows){
        List<Double> out=new ArrayList<>();
        if(rows!=null)for(double[] row:rows){double d=model.distance(row);if(Double.isFinite(d))out.add(d);}
        double[] x=new double[out.size()];for(int i=0;i<x.length;i++)x[i]=out.get(i);return x;
    }

    private static double[] candidateThresholds(double[] a,double[] b){
        double[] x=new double[a.length+b.length];System.arraycopy(a,0,x,0,a.length);System.arraycopy(b,0,x,a.length,b.length);Arrays.sort(x);
        List<Double> c=new ArrayList<>();
        if(x.length>0)c.add(Math.max(1e-12,x[0]*0.999999));
        for(int i=0;i<x.length;i++){
            c.add(Math.max(1e-12,x[i]));
            if(i+1<x.length&&x[i+1]>x[i])c.add((x[i]+x[i+1])/2.0);
        }
        if(x.length>0)c.add(x[x.length-1]*1.000001+1e-12);
        double[] z=new double[c.size()];for(int i=0;i<z.length;i++)z[i]=c.get(i);return z;
    }

    private static double knownRecall(double[] d,double threshold){int ok=0;for(double x:d)if(x<=threshold)ok++;return (double)ok/d.length;}
    private static double unknownRecall(double[] d,double threshold){int ok=0;for(double x:d)if(x>threshold)ok++;return (double)ok/d.length;}
}
