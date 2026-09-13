package com.servoc.app.chemcore;

import android.content.ContentValues;
import android.database.Cursor;

import com.servoc.app.data.ExperimentsDb;
import com.servoc.app.exposure.GasPpmExposureAssessment;

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
