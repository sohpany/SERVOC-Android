package com.servoc.app.chemcore;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Auditable ChemCore analytical result payload shared by DB/report exporters. */
public final class ChemCoreResultEnvelope {
    public static final String SCHEMA="chemcore-result/1";

    public static final class CandidateSummary {
        public final long chemicalUid;public final String label;public final double posterior,bestPpm,residualRms;public final int contributors;public final ChemCoreDecision.EvidenceTier tier;
        public CandidateSummary(long chemicalUid,String label,double posterior,double bestPpm,double residualRms,int contributors,ChemCoreDecision.EvidenceTier tier){this.chemicalUid=chemicalUid;this.label=label==null?"":label;this.posterior=posterior;this.bestPpm=bestPpm;this.residualRms=residualRms;this.contributors=contributors;this.tier=tier;}
    }

    public final String sessionId;
    public final ChemCoreDecision.Outcome outcome;
    public final ChemCoreDecision.EvidenceTier evidenceTier;
    public final Long chemicalUid;
    public final String chemicalLabel;
    public final double ppm,predictionLow,predictionHigh,oodScore,topPosterior,normalizedEntropy;
    public final String samplingProfileId,featureSchema,modelVersion,note;
    public final List<CandidateSummary> candidates;

    public ChemCoreResultEnvelope(String sessionId,ChemCoreDecision.Outcome outcome,ChemCoreDecision.EvidenceTier evidenceTier,
                                  Long chemicalUid,String chemicalLabel,double ppm,double predictionLow,double predictionHigh,
                                  double oodScore,double topPosterior,double normalizedEntropy,String samplingProfileId,
                                  String featureSchema,String modelVersion,String note,List<CandidateSummary> candidates){
        this.sessionId=sessionId==null?"":sessionId;this.outcome=outcome==null?ChemCoreDecision.Outcome.INVALID_MEASUREMENT:outcome;
        this.evidenceTier=evidenceTier==null?ChemCoreDecision.EvidenceTier.M0_CATALOG_ONLY:evidenceTier;this.chemicalUid=chemicalUid;
        this.chemicalLabel=chemicalLabel==null?"":chemicalLabel;this.ppm=ppm;this.predictionLow=predictionLow;this.predictionHigh=predictionHigh;
        this.oodScore=oodScore;this.topPosterior=topPosterior;this.normalizedEntropy=normalizedEntropy;this.samplingProfileId=samplingProfileId==null?"":samplingProfileId;
        this.featureSchema=featureSchema==null?ChemCoreFeaturePipeline.SCHEMA_VERSION:featureSchema;this.modelVersion=modelVersion==null?"":modelVersion;this.note=note==null?"":note;
        this.candidates=candidates==null?Collections.emptyList():Collections.unmodifiableList(new ArrayList<>(candidates));
    }

    public JSONObject toJson(){
        JSONObject x=new JSONObject();x.put("schema",SCHEMA);x.put("session_id",sessionId);x.put("outcome",outcome.name());x.put("evidence_tier",evidenceTier.name());
        if(chemicalUid!=null)x.put("chemical_uid",chemicalUid);if(!chemicalLabel.isEmpty())x.put("chemical",chemicalLabel);
        putFinite(x,"ppm",ppm);putFinite(x,"prediction_low",predictionLow);putFinite(x,"prediction_high",predictionHigh);putFinite(x,"ood_score",oodScore);
        putFinite(x,"top_posterior",topPosterior);putFinite(x,"normalized_entropy",normalizedEntropy);x.put("sampling_profile_id",samplingProfileId);x.put("feature_schema",featureSchema);x.put("model_version",modelVersion);x.put("note",note);
        JSONArray a=new JSONArray();for(CandidateSummary c:candidates){JSONObject q=new JSONObject();q.put("chemical_uid",c.chemicalUid);q.put("label",c.label);putFinite(q,"posterior",c.posterior);putFinite(q,"best_ppm",c.bestPpm);putFinite(q,"residual_rms",c.residualRms);q.put("contributors",c.contributors);if(c.tier!=null)q.put("evidence_tier",c.tier.name());a.put(q);}x.put("candidates",a);
        x.put("quantitative_occupational_interpretation_permitted",ChemCoreDecision.permitsQuantitativeOccupationalInterpretation(outcome,evidenceTier));
        return x;
    }

    private static void putFinite(JSONObject x,String k,double v){if(Double.isFinite(v))x.put(k,v);else x.put(k,JSONObject.NULL);}
}
