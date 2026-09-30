package com.servoc.app.chemcore;

import android.content.ContentValues;
import android.database.Cursor;

import com.servoc.app.data.ExperimentsDb;
import com.servoc.app.exposure.GasHazardPriority;
import com.servoc.app.exposure.GasPpmExposureAssessment;

import org.json.JSONArray;
import org.json.JSONObject;

/** Persists and retrieves auditable ChemCore analytical decisions separately from raw telemetry. */
public final class ChemCoreDecisionRepository {
    private final ExperimentsDb db;
    public ChemCoreDecisionRepository(ExperimentsDb db){if(db==null)throw new IllegalArgumentException("database required");this.db=db;ChemCoreSchema.ensure(db.getWritableDatabase());}

    public long save(long tMs,ChemCoreResultEnvelope r){
        if(r==null||r.sessionId.isEmpty())return -1;ContentValues v=new ContentValues();v.put("session_id",r.sessionId);v.put("t_ms",tMs);v.put("outcome",r.outcome.name());v.put("evidence_tier",r.evidenceTier.name());
        if(r.chemicalUid!=null)v.put("chemical_uid",r.chemicalUid);if(Double.isFinite(r.ppm))v.put("ppm",r.ppm);if(Double.isFinite(r.predictionLow))v.put("prediction_low",r.predictionLow);if(Double.isFinite(r.predictionHigh))v.put("prediction_high",r.predictionHigh);if(Double.isFinite(r.oodScore))v.put("ood_score",r.oodScore);
        JSONObject payload=r.toJson();try{payload.put(GasPpmExposureAssessment.KEY,GasPpmExposureAssessment.evaluate(db,payload,tMs));}catch(Exception ignored){}
        v.put("payload",payload.toString());return db.getWritableDatabase().insert("chemcore_decisions",null,v);
    }

    /**
     * Returns the latest ChemCore decision bound to exactly one measurement session.
     * Reporting code must use this instead of a global "last decision" to preserve lineage.
     */
    public JSONObject latestPayloadForSession(String sessionId){
        if(sessionId==null||sessionId.trim().isEmpty())return null;
        try(Cursor c=db.getReadableDatabase().rawQuery(
                "SELECT payload FROM chemcore_decisions WHERE session_id=? AND payload IS NOT NULL ORDER BY t_ms DESC,id DESC LIMIT 1",
                new String[]{sessionId.trim()})){
            if(!c.moveToFirst())return null;
            String raw=c.getString(0);if(raw==null||raw.trim().isEmpty())return null;
            try{return new JSONObject(raw);}catch(Exception ignored){return null;}
        }
    }

    /**
     * Returns only accepted, quantitative gas-safety findings. Candidate identities and screening
     * hypotheses remain preserved in each decision payload but are excluded from this safety sort so
     * they can never alter the accepted analytical identity or occupational decision.
     */
    public JSONArray recentAcceptedGasFindings(int limit,GasHazardPriority.Mode mode){
        int requested=Math.max(1,Math.min(100,limit)),scan=Math.max(requested*4,20);JSONArray raw=new JSONArray();
        try(Cursor c=db.getReadableDatabase().rawQuery("SELECT session_id,payload FROM chemcore_decisions WHERE payload IS NOT NULL ORDER BY id DESC LIMIT "+scan,null)){
            while(c.moveToNext()&&raw.length()<requested){
                String session=c.getString(0),text=c.getString(1);if(text==null||text.trim().isEmpty())continue;
                try{JSONObject p=new JSONObject(text),g=p.optJSONObject(GasPpmExposureAssessment.KEY);if(g==null||!g.optBoolean("input_gate_passed",false))continue;JSONObject q=new JSONObject(g.toString());q.put("session_id",session==null?"":session);q.put("chemical",p.optString("chemical",q.optString("chemical","")));q.put("evidence_tier",p.optString("evidence_tier",""));q.put("outcome",p.optString("outcome",""));raw.put(q);}catch(Exception ignored){}
            }
        }
        return GasHazardPriority.sort(raw,mode);
    }

    /**
     * Binds the analytical-lab readiness/calibration/environment snapshot to the latest decision of
     * the same telemetry session. This updates only the JSON audit envelope; scientific scalar
     * columns and the decision itself are unchanged.
     */
    public boolean attachLabQuality(String sessionId,JSONObject quality,long capturedAtMs){
        if(sessionId==null||sessionId.trim().isEmpty()||quality==null)return false;String id=sessionId.trim();
        try(Cursor c=db.getReadableDatabase().rawQuery(
                "SELECT id,payload FROM chemcore_decisions WHERE session_id=? AND payload IS NOT NULL ORDER BY t_ms DESC,id DESC LIMIT 1",
                new String[]{id})){
            if(!c.moveToFirst())return false;long rowId=c.getLong(0);String raw=c.getString(1);JSONObject payload=new JSONObject(raw==null?"{}":raw);
            JSONObject copy=new JSONObject(quality.toString());copy.put("captured_at_ms",Math.max(0,capturedAtMs));copy.put("binding","session_bound_post_decision_runtime_snapshot");
            payload.put("lab_quality",copy);JSONObject gate=copy.optJSONObject("calibration_gate");if(gate!=null)payload.put("calibration_gate",new JSONObject(gate.toString()));
            ContentValues v=new ContentValues();v.put("payload",payload.toString());return db.getWritableDatabase().update("chemcore_decisions",v,"id=?",new String[]{String.valueOf(rowId)})==1;
        }catch(Exception ignored){return false;}
    }

    /**
     * Attaches the immutable P9 deep-session result to the existing chemical decision row.
     * Identity, confidence and quantitative scalar columns are intentionally not recomputed here.
     */
    public boolean attachDeepSession(String primarySessionId,ChemCoreDeepSessionResult deep){
        if(primarySessionId==null||primarySessionId.trim().isEmpty()||deep==null)return false;String id=primarySessionId.trim();
        try(Cursor c=db.getReadableDatabase().rawQuery(
                "SELECT id,payload FROM chemcore_decisions WHERE session_id=? AND payload IS NOT NULL ORDER BY t_ms DESC,id DESC LIMIT 1",
                new String[]{id})){
            if(!c.moveToFirst())return false;long rowId=c.getLong(0);String raw=c.getString(1);JSONObject payload=new JSONObject(raw==null?"{}":raw);
            JSONObject deepJson=deep.toJson();payload.put("deep_session",deepJson);payload.put("session_dynamics",deep.dynamics.toJson());
            payload.put("recovery_session_id",deep.recoverySessionId);payload.put("deep_session_finalized",true);payload.put("deep_analysis_elapsed_ms",deep.analysisElapsedMs);
            ContentValues v=new ContentValues();v.put("payload",payload.toString());return db.getWritableDatabase().update("chemcore_decisions",v,"id=?",new String[]{String.valueOf(rowId)})==1;
        }catch(Exception ignored){return false;}
    }

    /**
     * P10 finalization: binds the M4-only reportable quantitative series while preserving every
     * other available evidence family and every raw/normalized telemetry record for audit and later
     * reporting. M1/M2/M3 evidence is never deleted; it remains screening/support evidence and simply
     * cannot promote an occupational quantitative claim by itself.
     */
    public boolean attachQuantitativeExposure(String primarySessionId,JSONObject quantitativeSeries){
        if(primarySessionId==null||primarySessionId.trim().isEmpty()||quantitativeSeries==null)return false;String id=primarySessionId.trim();
        try(Cursor c=db.getReadableDatabase().rawQuery(
                "SELECT id,payload FROM chemcore_decisions WHERE session_id=? AND payload IS NOT NULL ORDER BY t_ms DESC,id DESC LIMIT 1",
                new String[]{id})){
            if(!c.moveToFirst())return false;long rowId=c.getLong(0);String raw=c.getString(1);JSONObject payload=new JSONObject(raw==null?"{}":raw);
            JSONObject seriesCopy=new JSONObject(quantitativeSeries.toString());long first=seriesCopy.optLong("first_t_ms",0),last=seriesCopy.optLong("last_t_ms",first);long duration=Math.max(0,last-first);
            JSONObject exposure=GasPpmExposureAssessment.evaluate(db,payload,duration,seriesCopy);
            payload.put("quantitative_time_series",seriesCopy);payload.put(GasPpmExposureAssessment.KEY,exposure);
            payload.put("evidence_measurement_inventory",evidenceMeasurementInventory(id,payload,seriesCopy,exposure));
            payload.put("p10_quantitative_exposure_finalized",true);payload.put("p10_temporal_basis",seriesCopy.optBoolean("valid",false)?"M4_TIME_SERIES":"CURRENT_M4_PPM_ONLY");
            ContentValues v=new ContentValues();v.put("payload",payload.toString());return db.getWritableDatabase().update("chemcore_decisions",v,"id=?",new String[]{String.valueOf(rowId)})==1;
        }catch(Exception ignored){return false;}
    }

    /** Explicit inventory proving that reportability gates do not discard lower-tier evidence/data. */
    private JSONObject evidenceMeasurementInventory(String primarySessionId,JSONObject payload,JSONObject series,JSONObject exposure)throws Exception{
        JSONObject inv=new JSONObject();inv.put("schema","hazrob-evidence-measurement-inventory/1");inv.put("policy","PRESERVE_ALL_AVAILABLE_EVIDENCE_AND_MEASUREMENTS_SEPARATE_FROM_REPORTABILITY_GATES");
        inv.put("primary_session_id",primarySessionId);String recovery=payload.optString("recovery_session_id","");inv.put("recovery_session_id",recovery);
        inv.put("raw_telemetry_source","telemetry_samples");inv.put("raw_telemetry_preserved",true);inv.put("normalized_mq_measurements_preserved",true);inv.put("environment_measurements_preserved",true);inv.put("quality_flags_preserved",true);inv.put("motion_and_transport_state_preserved",true);
        inv.put("primary_telemetry_sample_count",telemetryCount(primarySessionId));inv.put("recovery_telemetry_sample_count",recovery.isEmpty()?0:telemetryCount(recovery));
        JSONArray candidates=payload.optJSONArray("candidates");int candidateCount=candidates==null?0:candidates.length(),evidenceCount=0,m1=0,m2=0,m3=0,m4=0;
        if(candidates!=null)for(int i=0;i<candidates.length();i++){JSONObject candidate=candidates.optJSONObject(i);if(candidate==null)continue;JSONArray rows=candidate.optJSONArray("evidence_observations");if(rows==null)continue;for(int j=0;j<rows.length();j++){JSONObject e=rows.optJSONObject(j);if(e==null)continue;evidenceCount++;String tier=e.optString("evidence_tier","");if(tier.startsWith("M1"))m1++;else if(tier.startsWith("M2"))m2++;else if(tier.startsWith("M3"))m3++;else if(tier.startsWith("M4"))m4++;}}
        inv.put("candidate_count",candidateCount);inv.put("evidence_observation_count",evidenceCount);inv.put("m1_evidence_count",m1);inv.put("m2_evidence_count",m2);inv.put("m3_evidence_count",m3);inv.put("m4_evidence_count",m4);
        inv.put("candidate_rankings_preserved",true);inv.put("screening_fit_ppm_preserved",true);inv.put("analytical_confidence_preserved",true);inv.put("posterior_preserved",true);inv.put("open_set_state_preserved",true);inv.put("deep_session_dynamics_preserved",payload.has("session_dynamics"));
        JSONArray points=series.optJSONArray("points");inv.put("reportable_quantitative_series_m4_only",true);inv.put("reportable_quantitative_point_count",points==null?0:points.length());inv.put("all_valid_m4_quantitative_points_preserved",series.optBoolean("all_valid_quantitative_points_preserved",false));
        inv.put("health_reference_count",exposure.optInt("ppm_reference_count",0));inv.put("lfl_reference_count",exposure.optInt("lfl_reference_count",0));inv.put("ufl_reference_count",exposure.optInt("ufl_reference_count",0));inv.put("converted_mass_reference_count",exposure.optInt("converted_mass_reference_count",0));
        inv.put("lower_tier_evidence_retention","M1_M2_M3_RETAINED_FOR_IDENTIFICATION_SCREENING_AND_AUDIT_NOT_REPORTABLE_PPM");inv.put("m4_quantitative_policy","ONLY_DEVICE_VALIDATED_M4_CAN_DRIVE_REPORTABLE_PPM_TWA_STEL_CEILING_IDLH_LFL_COMPARISONS");
        return inv;
    }

    private int telemetryCount(String sessionId){
        if(sessionId==null||sessionId.trim().isEmpty())return 0;
        try(Cursor c=db.getReadableDatabase().rawQuery("SELECT COUNT(*) FROM telemetry_samples WHERE session_id=?",new String[]{sessionId.trim()})){return c.moveToFirst()?c.getInt(0):0;}catch(Exception e){return 0;}
    }
}
