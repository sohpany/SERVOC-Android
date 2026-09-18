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

    /** Attaches operator-selected tracking lineage without changing the analytical decision. */
    public boolean attachOperatorTracking(String sessionId,int chemicalId,String name,long capturedAtMs){if(sessionId==null||sessionId.trim().isEmpty())return false;String id=sessionId.trim();try(Cursor c=db.getReadableDatabase().rawQuery("SELECT id,payload FROM chemcore_decisions WHERE session_id=? AND payload IS NOT NULL ORDER BY t_ms DESC,id DESC LIMIT 1",new String[]{id})){if(!c.moveToFirst())return false;long rowId=c.getLong(0);JSONObject payload=new JSONObject(c.getString(1)==null?"{}":c.getString(1));JSONObject t=new JSONObject();t.put("kind","operator_selected_hypothesis");t.put("active",chemicalId>=0);if(chemicalId>=0){t.put("chemical_uid",chemicalId);t.put("name",name==null?"":name);}t.put("captured_at_ms",Math.max(0,capturedAtMs));t.put("does_not_modify_analytical_decision",true);payload.put("operator_tracking",t);ContentValues v=new ContentValues();v.put("payload",payload.toString());return db.getWritableDatabase().update("chemcore_decisions",v,"id=?",new String[]{String.valueOf(rowId)})==1;}catch(Exception ignored){return false;}}

    /**
     * Returns only accepted, quantitative gas-safety findings. Candidate identities and screening
     * hypotheses are deliberately excluded so safety sorting can never alter analytical identity.
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
}
