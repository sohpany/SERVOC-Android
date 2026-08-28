package com.servoc.app.analytics;

import java.util.Arrays;

/**
 * Small on-device ridge regressor for concentration estimation from e-nose features.
 * Target is log1p(ppm), so zero-concentration mixture training samples are valid.
 * This is an empirical model and must be trained from controlled calibration data.
 */
public final class RidgeConcentrationModel {
    public final double[] mean;
    public final double[] scale;
    public final double[] beta; // intercept + standardized feature weights
    public final double lambda;
    public final int samples;

    private RidgeConcentrationModel(double[] mean, double[] scale, double[] beta, double lambda, int samples) {
        this.mean=mean; this.scale=scale; this.beta=beta; this.lambda=lambda; this.samples=samples;
    }

    public static RidgeConcentrationModel train(double[][] x, double[] ppm, double lambda) {
        if (x==null || ppm==null || x.length!=ppm.length || x.length<3)
            throw new IllegalArgumentException("Need >=3 aligned training samples");
        int d=0;
        for(double[] row:x) if(row!=null) d=Math.max(d,row.length);
        if(d==0) throw new IllegalArgumentException("No features");
        if(!Double.isFinite(lambda) || lambda<0) lambda=1e-3;

        double[] mean=new double[d], scale=new double[d];
        int[] count=new int[d];
        for(double[] row:x){
            if(row==null) continue;
            for(int j=0;j<Math.min(d,row.length);j++) if(Double.isFinite(row[j])){mean[j]+=row[j];count[j]++;}
        }
        for(int j=0;j<d;j++) if(count[j]>0) mean[j]/=count[j];
        for(double[] row:x){
            if(row==null) continue;
            for(int j=0;j<Math.min(d,row.length);j++) if(Double.isFinite(row[j])){
                double z=row[j]-mean[j]; scale[j]+=z*z;
            }
        }
        for(int j=0;j<d;j++){
            scale[j]=count[j]>1?Math.sqrt(scale[j]/(count[j]-1)):1.0;
            if(!Double.isFinite(scale[j]) || scale[j]<1e-9) scale[j]=1.0;
        }

        int p=d+1;
        double[][] a=new double[p][p];
        double[] b=new double[p];
        for(int i=0;i<x.length;i++){
            double y=Math.log1p(Math.max(0,Double.isFinite(ppm[i])?ppm[i]:0));
            double[] z=new double[p]; z[0]=1.0;
            for(int j=0;j<d;j++){
                double v=(x[i]!=null && j<x[i].length && Double.isFinite(x[i][j]))?x[i][j]:mean[j];
                z[j+1]=(v-mean[j])/scale[j];
            }
            for(int r=0;r<p;r++){
                b[r]+=z[r]*y;
                for(int c=0;c<p;c++) a[r][c]+=z[r]*z[c];
            }
        }
        for(int j=1;j<p;j++) a[j][j]+=lambda;
        double[] beta=solve(a,b);
        return new RidgeConcentrationModel(mean,scale,beta,lambda,x.length);
    }

    public double predict(double[] features) {
        if(features==null) return Double.NaN;
        double y=beta[0];
        for(int j=0;j<mean.length;j++){
            double v=j<features.length && Double.isFinite(features[j])?features[j]:mean[j];
            y+=beta[j+1]*(v-mean[j])/scale[j];
        }
        double ppm=Math.expm1(y);
        return Double.isFinite(ppm)?Math.max(0,ppm):Double.NaN;
    }

    private static double[] solve(double[][] a, double[] b) {
        int n=b.length;
        double[][] m=new double[n][n+1];
        for(int i=0;i<n;i++){
            System.arraycopy(a[i],0,m[i],0,n);
            m[i][n]=b[i];
        }
        for(int k=0;k<n;k++){
            int pivot=k;
            for(int i=k+1;i<n;i++) if(Math.abs(m[i][k])>Math.abs(m[pivot][k])) pivot=i;
            if(Math.abs(m[pivot][k])<1e-12){
                m[k][k]+=1e-9;
                pivot=k;
            }
            if(pivot!=k){ double[] t=m[k];m[k]=m[pivot];m[pivot]=t; }
            double div=m[k][k];
            if(Math.abs(div)<1e-18) div=div<0?-1e-18:1e-18;
            for(int j=k;j<=n;j++) m[k][j]/=div;
            for(int i=0;i<n;i++) if(i!=k){
                double f=m[i][k];
                if(f==0) continue;
                for(int j=k;j<=n;j++) m[i][j]-=f*m[k][j];
            }
        }
        double[] x=new double[n];
        for(int i=0;i<n;i++) x[i]=m[i][n];
        return x;
    }

    @Override public String toString() {
        return "RidgeConcentrationModel{samples="+samples+", features="+mean.length+", lambda="+lambda+", beta="+Arrays.toString(beta)+"}";
    }
}
