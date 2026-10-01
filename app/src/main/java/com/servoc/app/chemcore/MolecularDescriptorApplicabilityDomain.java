package com.servoc.app.chemcore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Conservative descriptor-space applicability domain for ChemCore molecular/QSPR models.
 *
 * This gate is deliberately independent of concentration, temperature and humidity. Those
 * variables remain governed by the response-model domain itself. The descriptor gate answers a
 * narrower question: is the target molecule represented by the descriptor space spanned by the
 * compounds that trained this sensor-specific QSPR model?
 *
 * The implementation uses two simultaneous research-grade guards:
 *  - every descriptor must remain inside the observed training min/max envelope;
 *  - the root-mean-square standardized descriptor distance must not exceed MAX_RMS_Z.
 *
 * This is an applicability-domain proxy, not evidence of identity and not a calibrated
 * probability. It may reject a QSPR extrapolation; it can never promote M1 evidence to M2/M3/M4.
 */
public final class MolecularDescriptorApplicabilityDomain {
    public static final String VERSION="chemcore-molecular-ad/1";
    public static final double MAX_RMS_Z=3.0;

    public static final class Result {
        public final boolean complete;
        public final boolean insideMinMaxEnvelope;
        public final double rmsZ;
        public final boolean insideDomain;
        public final String note;

        Result(boolean complete,boolean insideMinMaxEnvelope,double rmsZ,boolean insideDomain,String note){
            this.complete=complete;
            this.insideMinMaxEnvelope=insideMinMaxEnvelope;
            this.rmsZ=rmsZ;
            this.insideDomain=insideDomain;
            this.note=note==null?"":note;
        }
    }

    private final String[] descriptorNames;
    private final int trainingCompoundCount;
    private final double[] mean,sd,min,max;

    private MolecularDescriptorApplicabilityDomain(String[] descriptorNames,int trainingCompoundCount,
                                                    double[] mean,double[] sd,double[] min,double[] max){
        this.descriptorNames=Arrays.copyOf(descriptorNames,descriptorNames.length);
        this.trainingCompoundCount=trainingCompoundCount;
        this.mean=mean;this.sd=sd;this.min=min;this.max=max;
    }

    public static MolecularDescriptorApplicabilityDomain fit(String[] descriptorNames,
                                                              List<Map<String,Double>> distinctCompoundVectors){
        if(descriptorNames==null||descriptorNames.length==0)throw new IllegalArgumentException("descriptor names required");
        List<Map<String,Double>> rows=new ArrayList<>();
        if(distinctCompoundVectors!=null){
            for(Map<String,Double> v:distinctCompoundVectors)if(complete(v,descriptorNames))rows.add(v);
        }
        if(rows.size()<2)throw new IllegalArgumentException("at least two complete compound vectors required");

        int p=descriptorNames.length;
        double[] mean=new double[p],sd=new double[p],min=new double[p],max=new double[p];
        Arrays.fill(min,Double.POSITIVE_INFINITY);Arrays.fill(max,Double.NEGATIVE_INFINITY);
        for(Map<String,Double> row:rows){
            for(int j=0;j<p;j++){
                double x=row.get(descriptorNames[j]);
                mean[j]+=x;min[j]=Math.min(min[j],x);max[j]=Math.max(max[j],x);
            }
        }
        for(int j=0;j<p;j++)mean[j]/=rows.size();
        for(Map<String,Double> row:rows){
            for(int j=0;j<p;j++){double d=row.get(descriptorNames[j])-mean[j];sd[j]+=d*d;}
        }
        for(int j=0;j<p;j++){
            sd[j]=Math.sqrt(sd[j]/Math.max(1,rows.size()-1));
            if(!(sd[j]>1e-12))sd[j]=1.0;
        }
        return new MolecularDescriptorApplicabilityDomain(descriptorNames,rows.size(),mean,sd,min,max);
    }

    public Result evaluate(Map<String,Double> descriptors){
        if(!complete(descriptors,descriptorNames))
            return new Result(false,false,Double.NaN,false,"missing/non-finite core descriptor");
        boolean bounds=true;double z2=0;
        for(int j=0;j<descriptorNames.length;j++){
            double x=descriptors.get(descriptorNames[j]);
            if(x<min[j]||x>max[j])bounds=false;
            double z=(x-mean[j])/sd[j];z2+=z*z;
        }
        double rms=Math.sqrt(z2/descriptorNames.length);
        boolean inside=bounds&&Double.isFinite(rms)&&rms<=MAX_RMS_Z;
        String note=inside
                ?"inside sensor-specific molecular descriptor applicability domain"
                :!bounds?"outside at least one training descriptor min/max bound":"descriptor standardized distance exceeds research applicability threshold";
        return new Result(true,bounds,rms,inside,note);
    }

    public int trainingCompoundCount(){return trainingCompoundCount;}
    public String[] descriptorNames(){return Arrays.copyOf(descriptorNames,descriptorNames.length);}

    public Map<String,double[]> bounds(){
        Map<String,double[]> out=new LinkedHashMap<>();
        for(int i=0;i<descriptorNames.length;i++)out.put(descriptorNames[i],new double[]{min[i],max[i],mean[i],sd[i]});
        return out;
    }

    private static boolean complete(Map<String,Double> values,String[] names){
        if(values==null)return false;
        for(String n:names){Double x=values.get(n);if(x==null||!Double.isFinite(x))return false;}
        return true;
    }
}
