package com.servoc.app.field;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.content.ContentValues;
import com.servoc.app.data.ExperimentsDb;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Portable source archive for one USER field session.
 * Exports only operational measurement data required to reconstruct analysis/reports.
 * Global scientific/developer calibration, fingerprints and model artifacts are excluded.
 */
public final class FieldSessionArchive {
    private FieldSessionArchive(){}

    public static JSONObject exportSession(ExperimentsDb db,String fieldSessionId) throws Exception {
        if(db==null||fieldSessionId==null||fieldSessionId.trim().isEmpty())throw new IllegalArgumentException("field session required");
        SQLiteDatabase r=db.getReadableDatabase();JSONObject root=new JSONObject();
        root.put("type","HazRob User Field Session Archive");root.put("schema_version",1);root.put("field_session_id",fieldSessionId);
        JSONArray measurements=new JSONArray();
        try(Cursor c=r.rawQuery("SELECT m.session_id,m.started_ts,m.ended_ts,m.kind,m.operator_role,m.payload,m.analytics FROM measurement_sessions m JOIN field_session_measurements f ON f.measurement_session_id=m.session_id WHERE f.field_session_id=? AND m.operator_role='USER' AND m.kind LIKE 'field_%' ORDER BY m.started_ts",new String[]{fieldSessionId})){
            while(c.moveToNext()){
                JSONObject m=new JSONObject();String sid=c.getString(0);m.put("session_id",sid);m.put("started_ts",c.getString(1));m.put("ended_ts",c.getString(2));m.put("kind",c.getString(3));m.put("operator_role",c.getString(4));m.put("payload",c.getString(5));m.put("analytics",c.getString(6));
                m.put("telemetry_samples",rows(r,"SELECT t_ms,env_payload,mq_payload,motion_payload FROM telemetry_samples WHERE session_id=? ORDER BY t_ms",sid,new String[]{"t_ms","env_payload","mq_payload","motion_payload"}));
                m.put("events",rows(r,"SELECT t_ms,event_type,confidence,payload FROM events WHERE session_id=? ORDER BY t_ms",sid,new String[]{"t_ms","event_type","confidence","payload"}));
                m.put("chemcore_decisions",rows(r,"SELECT t_ms,outcome,evidence_tier,chemical_uid,ppm,prediction_low,prediction_high,ood_score,payload FROM chemcore_decisions WHERE session_id=? ORDER BY t_ms",sid,new String[]{"t_ms","outcome","evidence_tier","chemical_uid","ppm","prediction_low","prediction_high","ood_score","payload"}));
                measurements.put(m);
            }
        }
        root.put("measurements",measurements);return root;
    }

    public static final class ImportSummary {
        public final int measurements,samples,events,decisions;
        ImportSummary(int measurements,int samples,int events,int decisions){this.measurements=measurements;this.samples=samples;this.events=events;this.decisions=decisions;}
    }

    /** Restores an exact USER field archive idempotently; never creates scientific reference data. */
    public static ImportSummary importSession(ExperimentsDb db,JSONObject root) throws Exception {
        if(db==null||root==null)throw new IllegalArgumentException("archive required");
        if(!"HazRob User Field Session Archive".equals(root.optString("type","")))throw new IllegalStateException("invalid field archive");
        if(root.optInt("schema_version",0)!=1)throw new IllegalStateException("unsupported field archive schema");
        String fieldId=root.optString("field_session_id","").trim();if(fieldId.isEmpty())throw new IllegalStateException("field_session_id required");
        SQLiteDatabase w=db.getWritableDatabase();if(!exists(w,"field_sessions","field_session_id",fieldId))throw new IllegalStateException("field session metadata must be restored first");
        JSONArray a=root.optJSONArray("measurements");int nm=0,ns=0,ne=0,nd=0;w.beginTransaction();
        try{
            if(a!=null)for(int i=0;i<a.length();i++){JSONObject m=a.optJSONObject(i);if(m==null)continue;String sid=req(m,"session_id"),kind=req(m,"kind"),role=req(m,"operator_role");if(!"USER".equals(role)||!kind.startsWith("field_"))throw new IllegalStateException("archive contains non-user measurement");
                ContentValues mv=new ContentValues();mv.put("session_id",sid);mv.put("started_ts",req(m,"started_ts"));putNullable(mv,"ended_ts",m);mv.put("kind",kind);mv.put("operator_role","USER");putNullable(mv,"payload",m);putNullable(mv,"analytics",m);w.insertWithOnConflict("measurement_sessions",null,mv,SQLiteDatabase.CONFLICT_IGNORE);nm++;
                ContentValues link=new ContentValues();link.put("field_session_id",fieldId);link.put("measurement_session_id",sid);link.put("linked_ts",req(m,"started_ts"));w.insertWithOnConflict("field_session_measurements",null,link,SQLiteDatabase.CONFLICT_IGNORE);
                // Child rows are replaced by exact session identity so repeated restore cannot duplicate AUTOINCREMENT rows.
                w.delete("telemetry_samples","session_id=?",new String[]{sid});w.delete("events","session_id=?",new String[]{sid});w.delete("chemcore_decisions","session_id=?",new String[]{sid});
                ns+=restoreSamples(w,sid,m.optJSONArray("telemetry_samples"));ne+=restoreEvents(w,sid,m.optJSONArray("events"));nd+=restoreDecisions(w,sid,m.optJSONArray("chemcore_decisions"));
            }
            w.setTransactionSuccessful();
        }finally{w.endTransaction();}
        return new ImportSummary(nm,ns,ne,nd);
    }

    private static int restoreSamples(SQLiteDatabase w,String sid,JSONArray a){int n=0;if(a==null)return 0;for(int i=0;i<a.length();i++){JSONObject x=a.optJSONObject(i);if(x==null)continue;ContentValues v=new ContentValues();v.put("session_id",sid);v.put("t_ms",x.optLong("t_ms"));putNullable(v,"env_payload",x);putNullable(v,"mq_payload",x);putNullable(v,"motion_payload",x);w.insertOrThrow("telemetry_samples",null,v);n++;}return n;}
    private static int restoreEvents(SQLiteDatabase w,String sid,JSONArray a){int n=0;if(a==null)return 0;for(int i=0;i<a.length();i++){JSONObject x=a.optJSONObject(i);if(x==null)continue;ContentValues v=new ContentValues();v.put("session_id",sid);v.put("t_ms",x.optLong("t_ms"));v.put("event_type",x.optString("event_type",""));if(!x.isNull("confidence"))v.put("confidence",x.optDouble("confidence"));putNullable(v,"payload",x);w.insertOrThrow("events",null,v);n++;}return n;}
    private static int restoreDecisions(SQLiteDatabase w,String sid,JSONArray a){int n=0;if(a==null)return 0;for(int i=0;i<a.length();i++){JSONObject x=a.optJSONObject(i);if(x==null)continue;ContentValues v=new ContentValues();v.put("session_id",sid);v.put("t_ms",x.optLong("t_ms"));v.put("outcome",x.optString("outcome",""));v.put("evidence_tier",x.optString("evidence_tier",""));for(String k:new String[]{"chemical_uid","ppm","prediction_low","prediction_high","ood_score"})if(!x.isNull(k)){if("chemical_uid".equals(k))v.put(k,x.optLong(k));else v.put(k,x.optDouble(k));}putNullable(v,"payload",x);w.insertOrThrow("chemcore_decisions",null,v);n++;}return n;}
    private static void putNullable(ContentValues v,String key,JSONObject x){if(x.has(key)&&!x.isNull(key))v.put(key,x.optString(key,""));}
    private static boolean exists(SQLiteDatabase db,String table,String key,String value){try(Cursor c=db.rawQuery("SELECT 1 FROM "+table+" WHERE "+key+"=? LIMIT 1",new String[]{value})){return c.moveToFirst();}}
    private static String req(JSONObject x,String k){String v=x.optString(k,"").trim();if(v.isEmpty())throw new IllegalStateException("missing "+k);return v;}

    private static JSONArray rows(SQLiteDatabase db,String sql,String id,String[] names)throws Exception{
        JSONArray a=new JSONArray();try(Cursor c=db.rawQuery(sql,new String[]{id})){while(c.moveToNext()){JSONObject x=new JSONObject();for(int i=0;i<names.length;i++){if(c.isNull(i))x.put(names[i],JSONObject.NULL);else switch(c.getType(i)){case Cursor.FIELD_TYPE_INTEGER:x.put(names[i],c.getLong(i));break;case Cursor.FIELD_TYPE_FLOAT:x.put(names[i],c.getDouble(i));break;default:x.put(names[i],c.getString(i));}}a.put(x);}}return a;
    }
}
