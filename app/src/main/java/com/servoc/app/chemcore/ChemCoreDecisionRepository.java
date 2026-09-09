package com.servoc.app.chemcore;

import android.content.ContentValues;
import android.database.Cursor;

import com.servoc.app.data.ExperimentsDb;

import org.json.JSONObject;

/** Persists and retrieves auditable ChemCore analytical decisions separately from raw telemetry. */
public final class ChemCoreDecisionRepository {
    private final ExperimentsDb db;
    public ChemCoreDecisionRepository(ExperimentsDb db){if(db==null)throw new IllegalArgumentException("database required");this.db=db;ChemCoreSchema.ensure(db.getWritableDatabase());}

    public long save(long tMs,ChemCoreResultEnvelope r){
        if(r==null||r.sessionId.isEmpty())return -1;ContentValues v=new ContentValues();v.put("session_id",r.sessionId);v.put("t_ms",tMs);v.put("outcome",r.outcome.name());v.put("evidence_tier",r.evidenceTier.name());
        if(r.chemicalUid!=null)v.put("chemical_uid",r.chemicalUid);if(Double.isFinite(r.ppm))v.put("ppm",r.ppm);if(Double.isFinite(r.predictionLow))v.put("prediction_low",r.predictionLow);if(Double.isFinite(r.predictionHigh))v.put("prediction_high",r.predictionHigh);if(Double.isFinite(r.oodScore))v.put("ood_score",r.oodScore);v.put("payload",r.toJson().toString());
        return db.getWritableDatabase().insert("chemcore_decisions",null,v);
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
}
