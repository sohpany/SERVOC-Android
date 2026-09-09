package com.servoc.app.chemcore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Interpretable research baseline for molecularly informed MOX response prediction.
 *
 * Fits log10(Rs/R0) from log10(ppm), T, RH, molecular descriptors and descriptor×logC
 * interactions. The model is intentionally classified as an M1 prior unless an external
 * validation process promotes it; it must never be treated as a device calibration curve.
 */
public final class QsprMoxModel {
    public static final String MODEL_KIND="QSPR_MOX_RIDGE_LOG_RESPONSE";

    public static final class Observation {
        public final Map<String,Double> descriptors;
        public final double ppm,tempC,rh,log10Ratio,weight;
        public Observation(Map<String,Double> descriptors,double ppm,double tempC,double rh,double log10Ratio,double weight){
            this.descriptors=descriptors;this.ppm=ppm;this.tempC=tempC;this.rh=rh;this.log10Ratio=log10Ratio;
            this.weight=Double.isFinite(weight)&&weight>0?weight:1.0;
        }
    }

    public static final class Prediction {
        public final boolean available;
        public final double meanLog10Ratio;
        public final double responseRatio;
        public final double residualRmseLog10;
        public final double uncertaintyProxyLog10;
        public final double domainDistance;
        public final boolean insideTrainingEnvelope;
        public final ChemCoreDecision.EvidenceTier evidenceTier;
        public final String note;
        Prediction(boolean available,double meanLog10Ratio,double residualRmseLog10,double uncertaintyProxyLog10,
                   double domainDistance,boolean insideTrainingEnvelope,String note){
            this.available=available;this.meanLog10Ratio=meanLog10Ratio;
            this.responseRatio=available&&Double.isFinite(meanLog10Ratio)?Math.pow(10.0,meanLog10Ratio):Double.NaN;
            this.residualRmseLog10=residualRmseLog10;this.uncertaintyProxyLog10=uncertaintyProxyLog10;
            this.domainDistance=domainDistance;this.insideTrainingEnvelope=insideTrainingEnvelope;
            this.evidenceTier=ChemCoreDecision.EvidenceTier.M1_MOLECULARLY_INFERRED;this.note=note==null?"":note;
        }
    }

    public final String sensorKey;
    public final String version;
    public final String[] descriptorNames;
    public final int trainingCount;
    public final double ridgeLambda;
    public final double residualRmseLog10;
    public final double minPpm,maxPpm,minTempC,maxTempC,minRh,maxRh;
    private final double[] featureMean,featureSd,beta;

    private QsprMoxModel(String sensorKey,String version,String[] descriptorNames,int trainingCount,double ridgeLambda,
                         double residualRmseLog10,double minPpm,double maxPpm,double minTempC,double maxTempC,
                         double minRh,double maxRh,double[] featureMean,double[] featureSd,double[] beta){
        this.sensorKey=sensorKey;this.version=version;this.descriptorNames=descriptorNames;this.trainingCount=trainingCount;
        this.ridgeLambda=ridgeLambda;this.residualRmseLog10=residualRmseLog10;this.minPpm=minPpm;this.maxPpm=maxPpm;
        this.minTempC=minTempC;this.maxTempC=maxTempC;this.minRh=minRh;this.maxRh=maxRh;
        this.featureMean=featureMean;this.featureSd=featureSd;this.beta=beta;
    }

    public static QsprMoxModel fit(String sensorKey,String version,String[] descriptorNames,List<Observation> observations,double ridgeLambda){
        if(sensorKey==null||sensorKey.trim().isEmpty())throw new IllegalArgumentException("sensorKey required");
        if(descriptorNames==null)descriptorNames=new String[0];
        List<Observation> rows=new ArrayList<>();
        if(observations!=null)for(Observation o:observations)if(valid(o,descriptorNames))rows.add(o);
        int rawDim=3+2*descriptorNames.length; // logC,T,RH,descriptor values, descriptor×logC
        if(rows.size()<Math.max(6,rawDim+2))throw new IllegalArgumentException("insufficient complete QSPR observations");

        double[][] raw=new double[rows.size()][rawDim];double[] y=new double[rows.size()],w=new double[rows.size()];
        double minP=Double.POSITIVE_INFINITY,maxP=Double.NEGATIVE_INFINITY,minT=Double.POSITIVE_INFINITY,maxT=Double.NEGATIVE_INFINITY,minH=Double.POSITIVE_INFINITY,maxH=Double.NEGATIVE_INFINITY;
        for(int i=0;i<rows.size();i++){
            Observation o=rows.get(i);double lc=Math.log10(o.ppm);int k=0;raw[i][k++]=lc;raw[i][k++]=o.tempC;raw[i][k++]=o.rh;
            for(String n:descriptorNames)raw[i][k++]=o.descriptors.get(n);
            for(String n:descriptorNames)raw[i][k++]=o.descriptors.get(n)*lc;
            y[i]=o.log10Ratio;w[i]=o.weight;
            minP=Math.min(minP,o.ppm);maxP=Math.max(maxP,o.ppm);minT=Math.min(minT,o.tempC);maxT=Math.max(maxT,o.tempC);minH=Math.min(minH,o.rh);maxH=Math.max(maxH,o.rh);
        }
        double[] mean=new double[rawDim],sd=new double[rawDim];
        for(int j=0;j<rawDim;j++){for(double[] r:raw)mean[j]+=r[j];mean[j]/=raw.length;double ss=0;for(double[] r:raw){double d=r[j]-mean[j];ss+=d*d;}sd[j]=Math.sqrt(ss/Math.max(1,raw.length-1));if(!(sd[j]>1e-12))sd[j]=1.0;}

        int p=rawDim+1;double[][] a=new double[p][p];double[] b=new double[p];
        for(int i=0;i<raw.length;i++){
            double[] x=new double[p];x[0]=1.0;for(int j=0;j<rawDim;j++)x[j+1]=(raw[i][j]-mean[j])/sd[j];
            for(int r=0;r<p;r++){b[r]+=w[i]*x[r]*y[i];for(int c=0;c<p;c++)a[r][c]+=w[i]*x[r]*x[c];}
        }
        double lambda=Double.isFinite(ridgeLambda)&&ridgeLambda>=0?ridgeLambda:1e-3;
        for(int j=1;j<p;j++)a[j][j]+=lambda;
        double[] beta=solve(a,b);
        double sse=0,sw=0;for(int i=0;i<raw.length;i++){double pred=beta[0];for(int j=0;j<rawDim;j++)pred+=beta[j+1]*(raw[i][j]-mean[j])/sd[j];double e=y[i]-pred;sse+=w[i]*e*e;sw+=w[i];}
        double rmse=Math.sqrt(sse/Math.max(1.0,sw));
        return new QsprMoxModel(sensorKey,version==null?"":version,Arrays.copyOf(descriptorNames,descriptorNames.length),rows.size(),lambda,rmse,minP,maxP,minT,maxT,minH,maxH,mean,sd,beta);
    }

    public Prediction predict(Map<String,Double> descriptors,double ppm,double tempC,double rh){
        if(!(ppm>0)||!Double.isFinite(tempC)||!Double.isFinite(rh))return unavailable("invalid concentration/environment");
        for(String n:descriptorNames){Double v=descriptors==null?null:descriptors.get(n);if(v==null||!Double.isFinite(v))return unavailable("missing descriptor: "+n);}
        double lc=Math.log10(ppm);double[] raw=new double[featureMean.length];int k=0;raw[k++]=lc;raw[k++]=tempC;raw[k++]=rh;
        for(String n:descriptorNames)raw[k++]=descriptors.get(n);
        for(String n:descriptorNames)raw[k++]=descriptors.get(n)*lc;
        double pred=beta[0],d2=0;for(int j=0;j<raw.length;j++){double z=(raw[j]-featureMean[j])/featureSd[j];pred+=beta[j+1]*z;d2+=z*z;}
        double dd=Math.sqrt(d2/Math.max(1,raw.length));
        boolean envelope=ppm>=minPpm&&ppm<=maxPpm&&tempC>=minTempC&&tempC<=maxTempC&&rh>=minRh&&rh<=maxRh&&dd<=4.0;
        double u=residualRmseLog10*(1.0+0.15*Math.min(10.0,dd));
        return new Prediction(true,pred,residualRmseLog10,u,dd,envelope,
                envelope?"M1 molecular/QSPR prior inside training envelope":"M1 molecular/QSPR prior outside at least one training-domain limit");
    }

    private Prediction unavailable(String note){return new Prediction(false,Double.NaN,residualRmseLog10,Double.NaN,Double.NaN,false,note);}

    private static boolean valid(Observation o,String[] names){
        if(o==null||o.descriptors==null||!(o.ppm>0)||!Double.isFinite(o.tempC)||!Double.isFinite(o.rh)||!Double.isFinite(o.log10Ratio))return false;
        for(String n:names){Double v=o.descriptors.get(n);if(v==null||!Double.isFinite(v))return false;}return true;
    }

    private static double[] solve(double[][] a,double[] b){
        int n=b.length;double[][] m=new double[n][n+1];for(int i=0;i<n;i++){if(a[i].length!=n)throw new IllegalArgumentException("matrix");System.arraycopy(a[i],0,m[i],0,n);m[i][n]=b[i];}
        for(int c=0;c<n;c++){int p=c;for(int r=c+1;r<n;r++)if(Math.abs(m[r][c])>Math.abs(m[p][c]))p=r;if(Math.abs(m[p][c])<1e-14)throw new IllegalArgumentException("singular QSPR design");if(p!=c){double[] q=m[p];m[p]=m[c];m[c]=q;}double div=m[c][c];for(int j=c;j<=n;j++)m[c][j]/=div;for(int r=0;r<n;r++)if(r!=c){double f=m[r][c];for(int j=c;j<=n;j++)m[r][j]-=f*m[c][j];}}
        double[] x=new double[n];for(int i=0;i<n;i++)x[i]=m[i][n];return x;
    }
}
