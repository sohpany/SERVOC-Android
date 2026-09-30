package com.servoc.app.chemcore;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Auditable ChemCore analytical result payload shared by DB/report exporters. */
public final class ChemCoreResultEnvelope {
    public static final String SCHEMA = "chemcore-result/4";
    public static final String LEGACY_SCHEMA_V3 = "chemcore-result/3";
    public static final String OPERATIONAL_PANEL_ID = "hazrob-operational-144";
    public static final String ANALYTICAL_CONFIDENCE_SEMANTICS = "operational_positive_evidence_index_not_calibrated_probability";

    public static final class CandidateSummary {
        public final long chemicalUid;
        public final String label;
        public final double posterior;
        /** Joint curve-fit concentration hypothesis. It is not reportable ppm unless the final M4 gate passes. */
        public final double bestPpm;
        public final double residualRms;
        public final int contributors;
        public final ChemCoreDecision.EvidenceTier tier;
        public final double coveragePct,agreementPct,evidenceQualityPct;
        public final int calibratedContributors;
        public final boolean allContributorsInsideDomain;
        public final Map<String,Double> residualLog10BySensor;
        /** P6 operational identification confidence. Never a calibrated probability. */
        public final double analyticalConfidencePct;
        public final int evidenceGroupCount,evidenceSensorBreadth;
        /** Full candidate evidence trail; confidence fusion decides which rows add positive support. */
        public final List<ChemCoreEvidenceObservation> evidenceObservations;

        /** Backward-compatible constructor. */
        public CandidateSummary(long chemicalUid,String label,double posterior,double bestPpm,double residualRms,int contributors,ChemCoreDecision.EvidenceTier tier) {
            this(chemicalUid,label,posterior,bestPpm,residualRms,contributors,tier,Double.NaN,Double.NaN,Double.NaN,0,false,
                    Collections.emptyMap(),Double.NaN,0,0,Collections.emptyList());
        }

        /** Backward-compatible P5 constructor. */
        public CandidateSummary(long chemicalUid,String label,double posterior,double bestPpm,double residualRms,int contributors,
                ChemCoreDecision.EvidenceTier tier,double coveragePct,double agreementPct,double evidenceQualityPct,
                int calibratedContributors,boolean allContributorsInsideDomain,Map<String,Double> residualLog10BySensor) {
            this(chemicalUid,label,posterior,bestPpm,residualRms,contributors,tier,coveragePct,agreementPct,evidenceQualityPct,
                    calibratedContributors,allContributorsInsideDomain,residualLog10BySensor,Double.NaN,0,0,Collections.emptyList());
        }

        public CandidateSummary(long chemicalUid,String label,double posterior,double bestPpm,double residualRms,int contributors,
                ChemCoreDecision.EvidenceTier tier,double coveragePct,double agreementPct,double evidenceQualityPct,
                int calibratedContributors,boolean allContributorsInsideDomain,Map<String,Double> residualLog10BySensor,
                double analyticalConfidencePct,int evidenceGroupCount,int evidenceSensorBreadth,
                List<ChemCoreEvidenceObservation> evidenceObservations) {
            this.chemicalUid=chemicalUid;this.label=label==null?"":label;this.posterior=posterior;this.bestPpm=bestPpm;
            this.residualRms=residualRms;this.contributors=contributors;this.tier=tier;
            this.coveragePct=coveragePct;this.agreementPct=agreementPct;this.evidenceQualityPct=evidenceQualityPct;
            this.calibratedContributors=Math.max(0,calibratedContributors);this.allContributorsInsideDomain=allContributorsInsideDomain;
            this.residualLog10BySensor=Collections.unmodifiableMap(new LinkedHashMap<>(residualLog10BySensor==null?Collections.emptyMap():residualLog10BySensor));
            this.analyticalConfidencePct=Double.isFinite(analyticalConfidencePct)?Math.max(0,Math.min(100,analyticalConfidencePct)):Double.NaN;
            this.evidenceGroupCount=Math.max(0,evidenceGroupCount);this.evidenceSensorBreadth=Math.max(0,evidenceSensorBreadth);
            this.evidenceObservations=evidenceObservations==null?Collections.emptyList():Collections.unmodifiableList(new ArrayList<>(evidenceObservations));
        }

        /** Candidate-level quantitative support is complete only when every contributor is M4 and all remain inside domain. */
        public boolean hasDeviceQuantitativeSupport(){return contributors>0&&calibratedContributors==contributors&&allContributorsInsideDomain;}
    }

    public final String sessionId;
    public final ChemCoreDecision.Outcome outcome;
    public final ChemCoreDecision.EvidenceTier evidenceTier;
    public final Long chemicalUid;
    public final String chemicalLabel;
    public final double ppm,predictionLow,predictionHigh,oodScore,topPosterior,normalizedEntropy;
    /** Semantic aliases retained for UI/research code: OOD is represented as a distance/score and top posterior as posterior. */
    public final double oodDistance,posterior;
    /** Operational identification confidence; explicitly separate from posterior/model ranking. */
    public final double analyticalConfidencePct;
    public final int confidenceEvidenceGroups,confidenceSensorBreadth;
    public final String samplingProfileId,featureSchema,modelVersion,note;
    /** Exact scientific library provenance used for this result. Empty means legacy/embedded provenance not yet bound. */
    public final String libraryVersion,librarySha256,librarySourceCommit;
    public final List<CandidateSummary> candidates;
    /** Session-bound hardware/calibration/environment state. Empty for legacy results. */
    public final JSONObject labQuality;

    /** Backward-compatible constructor for callers that have not yet bound library provenance. */
    public ChemCoreResultEnvelope(String sessionId,ChemCoreDecision.Outcome outcome,ChemCoreDecision.EvidenceTier evidenceTier,
            Long chemicalUid,String chemicalLabel,double ppm,double predictionLow,double predictionHigh,double oodScore,
            double topPosterior,double normalizedEntropy,String samplingProfileId,String featureSchema,String modelVersion,
            String note,List<CandidateSummary> candidates) {
        this(sessionId,outcome,evidenceTier,chemicalUid,chemicalLabel,ppm,predictionLow,predictionHigh,oodScore,topPosterior,
                normalizedEntropy,samplingProfileId,featureSchema,modelVersion,note,candidates,"","","",null);
    }

    public ChemCoreResultEnvelope(String sessionId,ChemCoreDecision.Outcome outcome,ChemCoreDecision.EvidenceTier evidenceTier,
            Long chemicalUid,String chemicalLabel,double ppm,double predictionLow,double predictionHigh,double oodScore,
            double topPosterior,double normalizedEntropy,String samplingProfileId,String featureSchema,String modelVersion,
            String note,List<CandidateSummary> candidates,String libraryVersion,String librarySha256,String librarySourceCommit) {
        this(sessionId,outcome,evidenceTier,chemicalUid,chemicalLabel,ppm,predictionLow,predictionHigh,oodScore,topPosterior,
                normalizedEntropy,samplingProfileId,featureSchema,modelVersion,note,candidates,libraryVersion,librarySha256,librarySourceCommit,null);
    }

    private ChemCoreResultEnvelope(String sessionId,ChemCoreDecision.Outcome outcome,ChemCoreDecision.EvidenceTier evidenceTier,
            Long chemicalUid,String chemicalLabel,double ppm,double predictionLow,double predictionHigh,double oodScore,
            double topPosterior,double normalizedEntropy,String samplingProfileId,String featureSchema,String modelVersion,
            String note,List<CandidateSummary> candidates,String libraryVersion,String librarySha256,String librarySourceCommit,
            JSONObject labQuality) {
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
        CandidateSummary confidenceCandidate=findConfidenceCandidate(this.candidates,chemicalUid);
        if(confidenceCandidate!=null&&confidenceVisible(this.outcome,chemicalUid)){
            this.analyticalConfidencePct=confidenceCandidate.analyticalConfidencePct;
            this.confidenceEvidenceGroups=confidenceCandidate.evidenceGroupCount;
            this.confidenceSensorBreadth=confidenceCandidate.evidenceSensorBreadth;
        }else{
            this.analyticalConfidencePct=Double.NaN;this.confidenceEvidenceGroups=0;this.confidenceSensorBreadth=0;
        }
        this.labQuality=copy(labQuality);
    }

    /** Returns an immutable copy bound to the exact reference-library snapshot used by the caller. */
    public ChemCoreResultEnvelope withLibraryProvenance(String version,String sha256,String sourceCommit) {
        return new ChemCoreResultEnvelope(sessionId,outcome,evidenceTier,chemicalUid,chemicalLabel,ppm,predictionLow,predictionHigh,
                oodScore,topPosterior,normalizedEntropy,samplingProfileId,featureSchema,modelVersion,note,candidates,
                version,sha256,sourceCommit,labQuality);
    }

    /** Returns an immutable copy with the exact lab readiness/calibration/environment snapshot used for this decision. */
    public ChemCoreResultEnvelope withLabQuality(JSONObject quality) {
        return new ChemCoreResultEnvelope(sessionId,outcome,evidenceTier,chemicalUid,chemicalLabel,ppm,predictionLow,predictionHigh,
                oodScore,topPosterior,normalizedEntropy,samplingProfileId,featureSchema,modelVersion,note,candidates,
                libraryVersion,librarySha256,librarySourceCommit,quality);
    }

    public JSONObject toJson() {
        try {
            JSONObject out=new JSONObject();out.put("schema",SCHEMA);out.put("session_id",sessionId);out.put("outcome",outcome.name());out.put("evidence_tier",evidenceTier.name());
            if(chemicalUid!=null)out.put("chemical_uid",chemicalUid);if(!chemicalLabel.isEmpty())out.put("chemical",chemicalLabel);
            putFinite(out,"ppm",ppm);putFinite(out,"prediction_low",predictionLow);putFinite(out,"prediction_high",predictionHigh);
            putFinite(out,"ood_score",oodScore);putFinite(out,"top_posterior",topPosterior);putFinite(out,"normalized_entropy",normalizedEntropy);
            putFinite(out,"analytical_confidence_pct",analyticalConfidencePct);out.put("analytical_confidence_semantics",ANALYTICAL_CONFIDENCE_SEMANTICS);
            out.put("confidence_evidence_groups",confidenceEvidenceGroups);out.put("confidence_sensor_breadth",confidenceSensorBreadth);
            out.put("sampling_profile_id",samplingProfileId);out.put("feature_schema",featureSchema);out.put("model_version",modelVersion);out.put("note",note);
            out.put("measurement_quality",measurementQualityJson());
            out.put("quality_gates",qualityGatesJson());
            out.put("methodology",methodologyJson());
            out.put("feature_summary",featureSummaryJson());
            out.put("screening_144",ChemCore144ScreeningEngine.evaluate(this));
            out.put("accepted_findings",new JSONArray());
            out.put("safety_findings",new JSONArray());
            out.put("source_refs",new JSONArray());
            JSONObject library=new JSONObject();library.put("version",libraryVersion);library.put("sha256",librarySha256);library.put("source_commit",librarySourceCommit);out.put("library",library);
            JSONArray candidateArray=new JSONArray();
            int rank=0;for(CandidateSummary candidate:candidates){if(candidate==null)continue;JSONObject item=new JSONObject();item.put("rank",++rank);item.put("chemical_uid",candidate.chemicalUid);item.put("label",candidate.label);putFinite(item,"posterior",candidate.posterior);
                putFinite(item,"screening_fit_ppm",candidate.bestPpm);putFinite(item,"best_ppm",candidate.bestPpm);putFinite(item,"residual_rms",candidate.residualRms);item.put("contributors",candidate.contributors);if(candidate.tier!=null)item.put("evidence_tier",candidate.tier.name());
                putFinite(item,"coverage_pct",candidate.coveragePct);putFinite(item,"agreement_pct",candidate.agreementPct);putFinite(item,"evidence_quality_pct",candidate.evidenceQualityPct);putFinite(item,"analytical_confidence_pct",candidate.analyticalConfidencePct);
                item.put("analytical_confidence_semantics",ANALYTICAL_CONFIDENCE_SEMANTICS);item.put("evidence_group_count",candidate.evidenceGroupCount);item.put("evidence_sensor_breadth",candidate.evidenceSensorBreadth);
                item.put("calibrated_contributors",candidate.calibratedContributors);item.put("all_contributors_inside_domain",candidate.allContributorsInsideDomain);item.put("device_quantitative_support",candidate.hasDeviceQuantitativeSupport());
                JSONObject residuals=new JSONObject();for(Map.Entry<String,Double> e:candidate.residualLog10BySensor.entrySet())if(e.getKey()!=null&&e.getValue()!=null&&Double.isFinite(e.getValue()))residuals.put(e.getKey(),e.getValue());item.put("residual_log10_by_sensor",residuals);
                JSONArray evidenceRows=new JSONArray();for(ChemCoreEvidenceObservation e:candidate.evidenceObservations)if(e!=null)evidenceRows.put(evidenceJson(e));item.put("evidence_observations",evidenceRows);candidateArray.put(item);}out.put("candidates",candidateArray);
            if(!candidates.isEmpty()){putFinite(out,"runner_up_posterior",candidates.size()>1?candidates.get(1).posterior:Double.NaN);double margin=candidates.size()>1?candidates.get(0).posterior-candidates.get(1).posterior:Double.NaN;putFinite(out,"posterior_separation_margin",margin);out.put("candidate_count",candidates.size());}
            boolean quantitative=ChemCoreDecision.permitsQuantitativeOccupationalInterpretation(outcome,evidenceTier);
            out.put("quantitative_occupational_interpretation_permitted",quantitative);out.put("ppm_reportable",quantitative&&Double.isFinite(ppm));
            if(!quantitative)out.put("ppm_withheld_reason",ppmWithheldReason());
            if(labQuality!=null&&labQuality.length()>0){JSONObject q=copy(labQuality);out.put("lab_quality",q);JSONObject gate=q.optJSONObject("calibration_gate");if(gate!=null)out.put("calibration_gate",new JSONObject(gate.toString()));}
            return out;
        }catch(JSONException e){throw new IllegalStateException("Unable to serialize ChemCore result envelope",e);}
    }

    private static JSONObject evidenceJson(ChemCoreEvidenceObservation e)throws JSONException{
        JSONObject x=new JSONObject();x.put("schema",ChemCoreEvidenceObservation.SCHEMA);x.put("type",e.type.name());x.put("family_id",e.familyId);x.put("model_id",e.modelId);x.put("source_id",e.sourceId);x.put("independence_group",e.independenceGroup);x.put("evidence_tier",e.tier.name());x.put("inside_domain",e.insideDomain);x.put("quantitative_estimate_supported",e.quantitativeEstimateSupported);x.put("reportable_quantitative",e.reportableQuantitative);
        JSONArray sensors=new JSONArray();for(String s:e.sensorKeys)sensors.put(s);x.put("sensor_keys",sensors);putFinite(x,"fitted_ppm",e.fittedPpm);putFinite(x,"residual_log10",e.residualLog10);putFinite(x,"standardized_residual",e.standardizedResidual);if(!e.note.isEmpty())x.put("note",e.note);return x;
    }

    private JSONObject measurementQualityJson() throws JSONException {
        JSONObject q=labQuality==null?new JSONObject():copy(labQuality);
        if(q==null)q=new JSONObject();
        q.put("state",q.length()==0?"NOT_RECORDED":q.optString("state","RECORDED"));
        return q;
    }

    private JSONObject qualityGatesJson() throws JSONException {
        JSONObject q=new JSONObject();
        q.put("identity_named",ChemCoreDecision.permitsNamedIdentity(outcome));
        q.put("quantitative_occupational_interpretation",ChemCoreDecision.permitsQuantitativeOccupationalInterpretation(outcome,evidenceTier));
        q.put("m4_evidence",evidenceTier==ChemCoreDecision.EvidenceTier.M4_HAZROB_VALIDATED);
        q.put("outcome",outcome.name());
        if(!ChemCoreDecision.permitsQuantitativeOccupationalInterpretation(outcome,evidenceTier))q.put("ppm_withheld_reason",ppmWithheldReason());
        return q;
    }

    private JSONObject methodologyJson() throws JSONException {
        JSONObject q=new JSONObject();
        q.put("result_contract",SCHEMA);
        q.put("feature_contract",featureSchema);
        q.put("sampling_profile_id",samplingProfileId);
        q.put("model_version",modelVersion);
        q.put("candidate_concentration_semantics","joint_curve_fit_screening_hypothesis_unless_final_m4_gate_passes");
        q.put("posterior_semantics","relative_model_ranking_weight_not_calibrated_identity_probability");
        q.put("analytical_confidence_semantics",ANALYTICAL_CONFIDENCE_SEMANTICS);
        q.put("analytical_confidence_scale","0_to_100_positive_support_only_independence_deduplicated");
        return q;
    }

    private JSONObject featureSummaryJson() throws JSONException {
        JSONObject q=new JSONObject();
        q.put("schema",featureSchema);
        q.put("state","SESSION_BOUND_FEATURES_REFERENCED");
        return q;
    }

    /** Stage-1 fail-closed placeholder. Stage 4 replaces this with exactly 144 explicit rows. */
    private JSONObject pendingOperationalPanelJson() throws JSONException {
        JSONObject q=new JSONObject();
        q.put("panel_id",OPERATIONAL_PANEL_ID);
        q.put("panel_version","PENDING_STAGE_4");
        q.put("state","PENDING_ENGINE");
        q.put("target_count",144);
        q.put("evaluated_count",0);
        q.put("rows",new JSONArray());
        return q;
    }

    private String ppmWithheldReason(){
        if(outcome==ChemCoreDecision.Outcome.UNKNOWN)return "open_set_unknown";
        if(outcome==ChemCoreDecision.Outcome.POSSIBLE_MIXTURE_OR_INTERFERENCE)return "mixture_or_interference";
        if(outcome==ChemCoreDecision.Outcome.OUT_OF_VALIDATED_DOMAIN)return "outside_validated_domain";
        if(evidenceTier!=ChemCoreDecision.EvidenceTier.M4_HAZROB_VALIDATED)return "m4_device_reference_calibration_required";
        if(outcome!=ChemCoreDecision.Outcome.QUANTIFIED)return "identity_or_quantification_gate_not_passed";
        return "not_reportable";
    }

    private static CandidateSummary findConfidenceCandidate(List<CandidateSummary> rows,Long uid){
        if(rows==null||rows.isEmpty())return null;if(uid!=null)for(CandidateSummary c:rows)if(c!=null&&c.chemicalUid==uid)return c;return rows.get(0);
    }
    private static boolean confidenceVisible(ChemCoreDecision.Outcome outcome,Long uid){
        if(uid==null||outcome==null)return false;
        switch(outcome){
            case UNKNOWN:
            case POSSIBLE_MIXTURE_OR_INTERFERENCE:
            case INVALID_MEASUREMENT:
            case INSUFFICIENT_EVIDENCE:return false;
            default:return true;
        }
    }
    private static JSONObject copy(JSONObject x){if(x==null)return null;try{return new JSONObject(x.toString());}catch(Exception e){return null;}}
    private static void putFinite(JSONObject target,String key,double value)throws JSONException{target.put(key,Double.isFinite(value)?value:JSONObject.NULL);}
}
