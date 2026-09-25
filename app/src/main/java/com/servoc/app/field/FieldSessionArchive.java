package com.servoc.app.field;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
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

    private static JSONArray rows(SQLiteDatabase db,String sql,String id,String[] names)throws Exception{
        JSONArray a=new JSONArray();try(Cursor c=db.rawQuery(sql,new String[]{id})){while(c.moveToNext()){JSONObject x=new JSONObject();for(int i=0;i<names.length;i++){if(c.isNull(i))x.put(names[i],JSONObject.NULL);else switch(c.getType(i)){case Cursor.FIELD_TYPE_INTEGER:x.put(names[i],c.getLong(i));break;case Cursor.FIELD_TYPE_FLOAT:x.put(names[i],c.getDouble(i));break;default:x.put(names[i],c.getString(i));}}a.put(x);}}return a;
    }
}
