package com.servoc.app.chemcore;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Leakage-resistant validation helpers for QSPR-MOX research models.
 * Rows from the same compound are never split between training and validation in the
 * leave-compound-out protocol.
 */
public final class QsprValidationProtocol {
    private QsprValidationProtocol() {}

    public static final String LEAVE_COMPOUND_OUT = "leave-compound-out/1";

    public static final class LabeledObservation {
        public final long chemicalUid;
        public final String independentSessionId;
        public final QsprMoxModel.Observation observation;

        public LabeledObservation(long chemicalUid,String independentSessionId,QsprMoxModel.Observation observation){
            this.chemicalUid=chemicalUid;this.independentSessionId=independentSessionId==null?"":independentSessionId;
            this.observation=observation;
        }
    }

    public static final class Result {
        public final String protocol;
        public final int compoundsAttempted,compoundsEvaluated,predictions;
        public final double rmseLog10,maeLog10,biasLog10,insideEnvelopeFraction;
        Result(int attempted,int evaluated,int predictions,double rmse,double mae,double bias,double inside){
            this.protocol=LEAVE_COMPOUND_OUT;this.compoundsAttempted=attempted;this.compoundsEvaluated=evaluated;
            this.predictions=predictions;this.rmseLog10=rmse;this.maeLog10=mae;this.biasLog10=bias;
            this.insideEnvelopeFraction=inside;
        }
    }

    public static Result leaveCompoundOut(String sensorKey,String version,String[] descriptorNames,
                                          List<LabeledObservation> rows,double ridgeLambda){
        if(rows==null||rows.isEmpty())throw new IllegalArgumentException("observations required");
        Set<Long> compounds=new LinkedHashSet<>();for(LabeledObservation r:rows)if(valid(r))compounds.add(r.chemicalUid);
        if(compounds.size()<2)throw new IllegalArgumentException("at least two compounds required");
        double sse=0,sae=0,se=0;int predictions=0,inside=0,evaluated=0;
        for(long holdout:compounds){
            List<QsprMoxModel.Observation> train=new ArrayList<>(),test=new ArrayList<>();
            for(LabeledObservation r:rows){if(!valid(r))continue;if(r.chemicalUid==holdout)test.add(r.observation);else train.add(r.observation);}
            if(test.isEmpty())continue;
            QsprMoxModel model;
            try{model=QsprMoxModel.fit(sensorKey,version+"-lco-"+holdout,descriptorNames,train,ridgeLambda);}
            catch(IllegalArgumentException e){continue;}
            int foldPredictions=0;
            for(QsprMoxModel.Observation o:test){
                QsprMoxModel.Prediction p=model.predict(o.descriptors,o.ppm,o.tempC,o.rh);
                if(!p.available||!Double.isFinite(p.meanLog10Ratio))continue;
                double err=o.log10Ratio-p.meanLog10Ratio;sse+=err*err;sae+=Math.abs(err);se+=err;
                predictions++;foldPredictions++;if(p.insideTrainingEnvelope)inside++;
            }
            if(foldPredictions>0)evaluated++;
        }
        if(predictions==0)return new Result(compounds.size(),evaluated,0,Double.NaN,Double.NaN,Double.NaN,Double.NaN);
        return new Result(compounds.size(),evaluated,predictions,Math.sqrt(sse/predictions),sae/predictions,se/predictions,(double)inside/predictions);
    }

    private static boolean valid(LabeledObservation r){return r!=null&&r.chemicalUid>0&&r.observation!=null;}
}
