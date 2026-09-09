package com.servoc.app.measurement;

import android.database.Cursor;
import com.servoc.app.data.ExperimentsDb;
import org.json.JSONArray;
import org.json.JSONObject;

/** Builds a portable session payload from telemetry_samples for reporting and deterministic re-analysis. */
public final class SessionSeries {
    private SessionSeries(){}

    public static JSONObject attachLatest(ExperimentsDb db,JSONObject snapshot){
        if(db==null)return cloneJson(snapshot);String session=latestSession(db);return attachSession(db,snapshot,session);
    }

    /** Attaches one exact persisted measurement session; preferred by ChemCore to prevent latest-row races. */
    public static JSONObject attachSession(ExperimentsDb db,JSONObject snapshot,String sessionId){
        JSONObject out=cloneJson(snapshot);if(db==null||sessionId==null||sessionId.trim().isEmpty())return out;String session=sessionId.trim();
        try{
            JSONObject meta=new JSONObject();meta.put("session_id",session);JSONArray samples=new JSONArray();
            try(Cursor c=db.getReadableDatabase().rawQuery("SELECT t_ms,env_payload,mq_payload,motion_payload FROM telemetry_samples WHERE session_id=? ORDER BY t_ms",new String[]{session})){
                while(c.moveToNext()){
                    JSONObject q=new JSONObject();q.put("t_ms",c.getLong(0));q.put("env",json(c.getString(1)));q.put("mq",json(c.getString(2)));q.put("state",json(c.getString(3)));samples.put(q);
                }
            }
            meta.put("sample_count",samples.length());meta.put("samples",samples);out.put("_session",meta);
        }catch(Exception ignored){}
        return out;
    }

    public static int sampleCount(ExperimentsDb db,String sessionId){
        if(db==null||sessionId==null||sessionId.trim().isEmpty())return 0;
        try(Cursor c=db.getReadableDatabase().rawQuery("SELECT COUNT(*) FROM telemetry_samples WHERE session_id=?",new String[]{sessionId.trim()})){return c.moveToFirst()?c.getInt(0):0;}catch(Exception e){return 0;}
    }

    private static String latestSession(ExperimentsDb db){
        try(Cursor c=db.getReadableDatabase().rawQuery("SELECT session_id FROM measurement_sessions ORDER BY started_ts DESC LIMIT 1",null)){return c.moveToFirst()?c.getString(0):null;}catch(Exception e){return null;}
    }
    private static JSONObject cloneJson(JSONObject q){try{return q==null?new JSONObject():new JSONObject(q.toString());}catch(Exception e){return new JSONObject();}}
    private static JSONObject json(String s){try{return s==null||s.isEmpty()?new JSONObject():new JSONObject(s);}catch(Exception e){return new JSONObject();}}
}
