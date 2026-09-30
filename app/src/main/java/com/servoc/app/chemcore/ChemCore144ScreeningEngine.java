package com.servoc.app.chemcore;

import com.servoc.app.analytics.LibraryStatus;
import com.servoc.app.chemical.ChemicalCatalog;
import com.servoc.app.chemical.ChemicalCoverage;
import com.servoc.app.chemical.MqSensorAffinity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Deterministic full-catalogue screening manifest.
 *
 * Two layers are intentionally kept separate:
 * 1) catalogue/array capability (status/evidence_level/method/sensor mask), and
 * 2) session-specific positive evidence from ChemCore candidate fusion.
 *
 * A session support index is never a calibrated detection probability. "detected" remains
 * reserved for an accepted named identity; PPM remains gated independently by M4 rules.
 */
public final class ChemCore144ScreeningEngine {
    private ChemCore144ScreeningEngine() {}
    public static final String PANEL_ID="hazrob-operational-144";
    public static final String SESSION_SUPPORT_SEMANTICS="analytical_confidence_positive_evidence_index_not_detection_probability";
    public static final String RELIABILITY_SEMANTICS="qualitative_band_of_session_support_index_not_probability_of_presence";

    public static JSONObject evaluate() { return evaluate(null); }

    public static JSONObject evaluate(ChemCoreResultEnvelope result) {
        try {
            JSONArray rows=new JSONArray();int candidateSupported=0,acceptedIdentities=0,quantified=0,arrayUnsupported=0;
            for(int i=0;i<ChemicalCatalog.COUNT;i++){
                ChemicalCoverage.Status coverage=ChemicalCoverage.forChemical(i);
                MqSensorAffinity.Plan plan=MqSensorAffinity.forChemical(i);
                ChemCoreResultEnvelope.CandidateSummary candidate=findCandidate(i,result);
                boolean accepted=matchesAcceptedChemical(i,result)&&ChemCoreDecision.permitsNamedIdentity(result.outcome);
                boolean ppm=accepted&&ChemCoreDecision.permitsQuantitativeOccupationalInterpretation(result.outcome,result.evidenceTier)&&Double.isFinite(result.ppm);
                JSONObject r=new JSONObject();
                r.put("index",i);
                r.put("name",ChemicalCatalog.NAMES[i]);
                r.put("formula",ChemicalCatalog.FORMULAS[i]);
                r.put("status",status(i,coverage,plan,result));
                r.put("evidence_level",coverage.level);
                r.put("method",coverage.method);
                r.put("sensor_mask",plan.mask);
                r.put("sensors",ChemicalCoverage.sensorNames(plan.mask));
                r.put("reason",coverage.note);
                r.put("detected",accepted);
                r.put("ppm_reportable",ppm);
                r.put("catalogue_array_supported",plan.mask!=0);
                r.put("session_candidate_evidence",candidate!=null);
                r.put("session_evidence_state",sessionEvidenceState(plan,accepted,candidate));
                r.put("session_support_semantics",SESSION_SUPPORT_SEMANTICS);
                r.put("appearance_reliability_semantics",RELIABILITY_SEMANTICS);
                if(candidate!=null){
                    r.put("candidate_rank",candidateRank(candidate,result));
                    putFinite(r,"ranking_weight",candidate.posterior);
                    putFinite(r,"session_support_index_pct",candidate.analyticalConfidencePct);
                    putFinite(r,"coverage_pct",candidate.coveragePct);
                    putFinite(r,"agreement_pct",candidate.agreementPct);
                    putFinite(r,"evidence_quality_pct",candidate.evidenceQualityPct);
                    putFinite(r,"residual_rms",candidate.residualRms);
                    r.put("evidence_group_count",candidate.evidenceGroupCount);
                    r.put("evidence_sensor_breadth",candidate.evidenceSensorBreadth);
                    r.put("contributors",candidate.contributors);
                    r.put("calibrated_contributors",candidate.calibratedContributors);
                    r.put("all_contributors_inside_domain",candidate.allContributorsInsideDomain);
                    r.put("candidate_evidence_tier",candidate.tier==null?"":candidate.tier.name());
                    r.put("appearance_reliability_class",reliabilityBand(candidate.analyticalConfidencePct,accepted));
                    candidateSupported++;
                }else{
                    r.put("candidate_rank",JSONObject.NULL);
                    r.put("ranking_weight",JSONObject.NULL);
                    r.put("session_support_index_pct",JSONObject.NULL);
                    r.put("coverage_pct",JSONObject.NULL);
                    r.put("agreement_pct",JSONObject.NULL);
                    r.put("evidence_quality_pct",JSONObject.NULL);
                    r.put("residual_rms",JSONObject.NULL);
                    r.put("evidence_group_count",0);
                    r.put("evidence_sensor_breadth",0);
                    r.put("contributors",0);
                    r.put("calibrated_contributors",0);
                    r.put("all_contributors_inside_domain",false);
                    r.put("candidate_evidence_tier","");
                    r.put("appearance_reliability_class",accepted?"ACCEPTED_IDENTITY_UNSCORED":plan.mask==0?"ARRAY_UNSUPPORTED":"NO_SESSION_CANDIDATE_EVIDENCE");
                }
                if(accepted)acceptedIdentities++;if(ppm)quantified++;if(plan.mask==0)arrayUnsupported++;
                rows.put(r);
            }
            JSONObject q=new JSONObject();
            q.put("panel_id",PANEL_ID);
            q.put("panel_version",LibraryStatus.CATALOG_VERSION);
            q.put("state","EVALUATED");
            q.put("target_count",ChemicalCatalog.COUNT);
            q.put("evaluated_count",rows.length());
            q.put("session_support_semantics",SESSION_SUPPORT_SEMANTICS);
            q.put("appearance_reliability_semantics",RELIABILITY_SEMANTICS);
            q.put("session_candidate_supported_count",candidateSupported);
            q.put("accepted_identity_count",acceptedIdentities);
            q.put("ppm_reportable_count",quantified);
            q.put("array_unsupported_count",arrayUnsupported);
            q.put("rows",rows);
            return q;
        } catch(JSONException e){throw new IllegalStateException("Unable to build 144 screening panel",e);}
    }

    private static ChemCoreResultEnvelope.CandidateSummary findCandidate(int index,ChemCoreResultEnvelope result){
        if(result==null||result.candidates==null||result.candidates.isEmpty())return null;String target=ChemicalCatalog.NAMES[index];
        for(ChemCoreResultEnvelope.CandidateSummary c:result.candidates)if(c!=null&&c.label!=null&&c.label.trim().equalsIgnoreCase(target))return c;
        return null;
    }

    private static int candidateRank(ChemCoreResultEnvelope.CandidateSummary candidate,ChemCoreResultEnvelope result){
        if(result==null||result.candidates==null)return -1;int rank=0;for(ChemCoreResultEnvelope.CandidateSummary c:result.candidates){if(c==null)continue;rank++;if(c==candidate)return rank;}return -1;
    }

    private static String sessionEvidenceState(MqSensorAffinity.Plan plan,boolean accepted,ChemCoreResultEnvelope.CandidateSummary candidate){
        if(accepted)return "ACCEPTED_NAMED_IDENTITY";
        if(candidate!=null)return "CANDIDATE_SUPPORT";
        if(plan.mask==0)return "NOT_EVALUABLE_BY_CURRENT_ARRAY";
        return "NO_POSITIVE_SESSION_CANDIDATE_EVIDENCE";
    }

    /** Reporting band only; the underlying index is not a calibrated probability. */
    private static String reliabilityBand(double support,boolean accepted){
        if(!Double.isFinite(support))return accepted?"ACCEPTED_IDENTITY_UNSCORED":"CANDIDATE_UNSCORED";
        if(support>=85)return accepted?"ACCEPTED_VERY_STRONG_SUPPORT":"VERY_STRONG_SUPPORT";
        if(support>=70)return accepted?"ACCEPTED_STRONG_SUPPORT":"STRONG_SUPPORT";
        if(support>=50)return accepted?"ACCEPTED_MODERATE_SUPPORT":"MODERATE_SUPPORT";
        if(support>=30)return accepted?"ACCEPTED_LIMITED_SUPPORT":"LIMITED_SUPPORT";
        return accepted?"ACCEPTED_WEAK_SUPPORT":"WEAK_SUPPORT";
    }

    private static void putFinite(JSONObject o,String key,double value)throws JSONException{if(Double.isFinite(value))o.put(key,value);else o.put(key,JSONObject.NULL);}

    private static boolean matchesAcceptedChemical(int index,ChemCoreResultEnvelope result){
        if(result==null||result.chemicalUid==null)return false;
        // chemical_uid is a stable database UID, not a 0..143 catalogue index. Match the operational
        // panel by canonical label; never equate UID numerically with the legacy catalogue position.
        String label=result.chemicalLabel==null?"":result.chemicalLabel.trim();
        return !label.isEmpty()&&label.equalsIgnoreCase(ChemicalCatalog.NAMES[index]);
    }

    private static String status(int index,ChemicalCoverage.Status c,MqSensorAffinity.Plan p,ChemCoreResultEnvelope result){
        if(matchesAcceptedChemical(index,result)){
            if(result.outcome==ChemCoreDecision.Outcome.OUT_OF_VALIDATED_DOMAIN)return "OUT_OF_DOMAIN";
            if(result.outcome==ChemCoreDecision.Outcome.UNKNOWN||result.outcome==ChemCoreDecision.Outcome.POSSIBLE_MIXTURE_OR_INTERFERENCE||result.outcome==ChemCoreDecision.Outcome.INSUFFICIENT_EVIDENCE)return "INSUFFICIENT_EVIDENCE";
            if(ChemCoreDecision.permitsNamedIdentity(result.outcome))return "SUPPORTED";
        }
        if(p.mask==0)return "UNSUPPORTED_BY_CURRENT_ARRAY";
        if(c.level==ChemicalCoverage.DIRECT_CURVE)return "SUPPORTED";
        if(c.level==ChemicalCoverage.FINGERPRINT_CANDIDATE)return "WEAK_MATCH";
        if(c.level==ChemicalCoverage.AGGREGATE)return "INSUFFICIENT_EVIDENCE";
        return "INSUFFICIENT_EVIDENCE";
    }
}
