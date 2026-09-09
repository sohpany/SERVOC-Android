package com.servoc.app.chemcore;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Auditable ChemCore analytical result payload shared by DB/report exporters. */
public final class ChemCoreResultEnvelope {
    public static final String SCHEMA = "chemcore-result/2";

    public static final class CandidateSummary {
        public final long chemicalUid;
        public final String label;
        public final double posterior;
        public final double bestPpm;
        public final double residualRms;
        public final int contributors;
        public final ChemCoreDecision.EvidenceTier tier;

        public CandidateSummary(long chemicalUid,String label,double posterior,double bestPpm,double residualRms,int contributors,ChemCoreDecision.EvidenceTier tier) {
            this.chemicalUid=chemicalUid;this.label=label==null?"":label;this.posterior=posterior;this.bestPpm=bestPpm;
            this.residualRms=residualRms;this.contributors=contributors;this.tier=tier;
        }
    }

    public final String sessionId;
    public final ChemCoreDecision.Outcome outcome;
    public final ChemCoreDecision.EvidenceTier evidenceTier;
    public final Long chemicalUid;
    public final String chemicalLabel;
    public final double ppm,predictionLow,predictionHigh,oodScore,topPosterior,normalizedEntropy;
    /** Semantic aliases retained for UI/research code: OOD is represented as a distance/score and top posterior as posterior. */
    public final double oodDistance,posterior;
    public final String samplingProfileId,featureSchema,modelVersion,note;
    /** Exact scientific library provenance used for this result. Empty means legacy/embedded provenance not yet bound. */
    public final String libraryVersion,librarySha256,librarySourceCommit;
    public final List<CandidateSummary> candidates;

    /** Backward-compatible constructor for callers that have not yet bound library provenance. */
    public ChemCoreResultEnvelope(String sessionId,ChemCoreDecision.Outcome outcome,ChemCoreDecision.EvidenceTier evidenceTier,
            Long chemicalUid,String chemicalLabel,double ppm,double predictionLow,double predictionHigh,double oodScore,
            double topPosterior,double normalizedEntropy,String samplingProfileId,String featureSchema,String modelVersion,
            String note,List<CandidateSummary> candidates) {
        this(sessionId,outcome,evidenceTier,chemicalUid,chemicalLabel,ppm,predictionLow,predictionHigh,oodScore,topPosterior,
                normalizedEntropy,samplingProfileId,featureSchema,modelVersion,note,candidates,"","","");
    }

    public ChemCoreResultEnvelope(String sessionId,ChemCoreDecision.Outcome outcome,ChemCoreDecision.EvidenceTier evidenceTier,
            Long chemicalUid,String chemicalLabel,double ppm,double predictionLow,double predictionHigh,double oodScore,
            double topPosterior,double normalizedEntropy,String samplingProfileId,String featureSchema,String modelVersion,
            String note,List<CandidateSummary> candidates,String libraryVersion,String librarySha256,String librarySourceCommit) {
        this.sessionId=sessionId==null?"":sessionId;
        this.outcome=outcome==null?ChemCoreDecision.Outcome.INVALID_MEASUREMENT:outcome;
        this.evidenceTier=evidenceTier==null?ChemCoreDecision.EvidenceTier.M0_CATALOG_ONLY:evidenceTier;
        this.chemicalUid=chemicalUid;this.chemicalLabel=chemicalLabel==null?"":chemicalLabel;this.ppm=ppm;
        this.predictionLow=predictionLow;this.predictionHigh=predictionHigh;this.oodScore=oodScore;this.topPosterior=topPosterior;
        this.oodDistance=oodScore;this.posterior=topPosterior;
        this.normalizedEntropy=normalizedEntropy;this.samplingProfileId=samplingProfileId==null?"":samplingProfileId;
        this.featureSchema=featureSchema==null?ChemCoreFeaturePipeline.SCHEMA_VERSION:featureSchema;
        this.modelVersion=modelVersion==null?"":modelVersion;this.note=note==null?"":note;
        this.libraryVersion=libraryVersion==null?"":libraryVersion;this.librarySha256=librarySha256==null?"":librarySha256;
        this.librarySourceCommit=librarySourceCommit==null?"":librarySourceCommit;
        this.candidates=candidates==null?Collections.emptyList():Collections.unmodifiableList(new ArrayList<>(candidates));
    }

    /** Returns an immutable copy bound to the exact reference-library snapshot used by the caller. */
    public ChemCoreResultEnvelope withLibraryProvenance(String version,String sha256,String sourceCommit) {
        return new ChemCoreResultEnvelope(sessionId,outcome,evidenceTier,chemicalUid,chemicalLabel,ppm,predictionLow,predictionHigh,
                oodScore,topPosterior,normalizedEntropy,samplingProfileId,featureSchema,modelVersion,note,candidates,
                version,sha256,sourceCommit);
    }

    public JSONObject toJson() {
        try {
            JSONObject out=new JSONObject();out.put("schema",SCHEMA);out.put("session_id",sessionId);out.put("outcome",outcome.name());out.put("evidence_tier",evidenceTier.name());
            if(chemicalUid!=null)out.put("chemical_uid",chemicalUid);if(!chemicalLabel.isEmpty())out.put("chemical",chemicalLabel);
            putFinite(out,"ppm",ppm);putFinite(out,"prediction_low",predictionLow);putFinite(out,"prediction_high",predictionHigh);
            putFinite(out,"ood_score",oodScore);putFinite(out,"top_posterior",topPosterior);putFinite(out,"normalized_entropy",normalizedEntropy);
            out.put("sampling_profile_id",samplingProfileId);out.put("feature_schema",featureSchema);out.put("model_version",modelVersion);out.put("note",note);
            JSONObject library=new JSONObject();library.put("version",libraryVersion);library.put("sha256",librarySha256);library.put("source_commit",librarySourceCommit);out.put("library",library);
            JSONArray candidateArray=new JSONArray();
            for(CandidateSummary candidate:candidates){if(candidate==null)continue;JSONObject item=new JSONObject();item.put("chemical_uid",candidate.chemicalUid);item.put("label",candidate.label);putFinite(item,"posterior",candidate.posterior);putFinite(item,"best_ppm",candidate.bestPpm);putFinite(item,"residual_rms",candidate.residualRms);item.put("contributors",candidate.contributors);if(candidate.tier!=null)item.put("evidence_tier",candidate.tier.name());candidateArray.put(item);}out.put("candidates",candidateArray);
            out.put("quantitative_occupational_interpretation_permitted",ChemCoreDecision.permitsQuantitativeOccupationalInterpretation(outcome,evidenceTier));
            return out;
        }catch(JSONException e){throw new IllegalStateException("Unable to serialize ChemCore result envelope",e);}
    }

    private static void putFinite(JSONObject target,String key,double value)throws JSONException{target.put(key,Double.isFinite(value)?value:JSONObject.NULL);}
}
