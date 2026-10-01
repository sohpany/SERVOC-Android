package com.servoc.app.chemcore;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Leakage-resistant validation helpers for QSPR-MOX research models.
 * Rows from the same compound are never split between training and validation in the
 * leave-compound-out protocol.
 *
 * The source-backed runtime bench contains several deterministic concentration points per
 * compound. Those points are not independent laboratory sessions. Validation is therefore
 * grouped by compound and reports both pooled error and the worst held-out-compound fold.
 */
public final class QsprValidationProtocol {
    private QsprValidationProtocol() {}

    public static final String LEAVE_COMPOUND_OUT = "leave-compound-out/2-compound-balanced";

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
        public final double meanCompoundRmseLog10,worstCompoundRmseLog10,worstCompoundAbsBiasLog10;
        Result(int attempted,int evaluated,int predictions,double rmse,double mae,double bias,double inside,
               double meanCompoundRmse,double worstCompoundRmse,double worstCompoundAbsBias){
            this.protocol=LEAVE_COMPOUND_OUT;this.compoundsAttempted=attempted;this.compoundsEvaluated=evaluated;
            this.predictions=predictions;this.rmseLog10=rmse;this.maeLog10=mae;this.biasLog10=bias;
            this.insideEnvelopeFraction=inside;this.meanCompoundRmseLog10=meanCompoundRmse;
            this.worstCompoundRmseLog10=worstCompoundRmse;this.worstCompoundAbsBiasLog10=worstCompoundAbsBias;
        }
    }

    public static Result leaveCompoundOut(String sensorKey,String version,String[] descriptorNames,
                                          List<LabeledObservation> rows,double ridgeLambda){
        if(rows==null||rows.isEmpty())throw new IllegalArgumentException("observations required");
        Set<Long> compounds=new LinkedHashSet<>();for(LabeledObservation r:rows)if(valid(r))compounds.add(r.chemicalUid);
        if(compounds.size()<2)throw new IllegalArgumentException("at least two compounds required");
        double sse=0,sae=0,se=0;int predictions=0,inside=0,evaluated=0;
        double sumFoldRmse=0,worstFoldRmse=Double.NEGATIVE_INFINITY,worstFoldAbsBias=Double.NEGATIVE_INFINITY;
        for(long holdout:compounds){
            List<QsprMoxModel.Observation> train=new ArrayList<>(),test=new ArrayList<>();
            for(LabeledObservation r:rows){if(!valid(r))continue;if(r.chemicalUid==holdout)test.add(r.observation);else train.add(r.observation);}
            if(test.isEmpty())continue;
            QsprMoxModel model;
            try{model=QsprMoxModel.fit(sensorKey,version+"-lco-"+holdout,descriptorNames,train,ridgeLambda);}
            catch(IllegalArgumentException e){continue;}
            int foldPredictions=0;double foldSse=0,foldSe=0;
            for(QsprMoxModel.Observation o:test){
                QsprMoxModel.Prediction p=model.predict(o.descriptors,o.ppm,o.tempC,o.rh);
                if(!p.available||!Double.isFinite(p.meanLog10Ratio))continue;
                double err=o.log10Ratio-p.meanLog10Ratio;sse+=err*err;sae+=Math.abs(err);se+=err;
                foldSse+=err*err;foldSe+=err;
                predictions++;foldPredictions++;if(p.insideTrainingEnvelope)inside++;
            }
            if(foldPredictions>0){
                evaluated++;
                double foldRmse=Math.sqrt(foldSse/foldPredictions);
                double foldAbsBias=Math.abs(foldSe/foldPredictions);
                sumFoldRmse+=foldRmse;
                worstFoldRmse=Math.max(worstFoldRmse,foldRmse);
                worstFoldAbsBias=Math.max(worstFoldAbsBias,foldAbsBias);
            }
        }
        if(predictions==0)return new Result(compounds.size(),evaluated,0,Double.NaN,Double.NaN,Double.NaN,Double.NaN,
                Double.NaN,Double.NaN,Double.NaN);
        return new Result(compounds.size(),evaluated,predictions,Math.sqrt(sse/predictions),sae/predictions,se/predictions,
                (double)inside/predictions,evaluated>0?sumFoldRmse/evaluated:Double.NaN,
                evaluated>0?worstFoldRmse:Double.NaN,evaluated>0?worstFoldAbsBias:Double.NaN);
    }

    private static boolean valid(LabeledObservation r){return r!=null&&r.chemicalUid>0&&r.observation!=null;}
}
