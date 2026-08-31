package com.servoc.app.data;

import android.content.ContentValues;
import android.database.Cursor;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** Persistence boundary for time-linked media and event evidence. */
public final class EvidenceStore {
    private EvidenceStore(){}

    public static String startStandaloneSession(ExperimentsDb db,String operatorRole,String kind){
        String id="MEDIA-"+System.currentTimeMillis();ContentValues v=new ContentValues();v.put("session_id",id);v.put("started_ts",isoNow());v.put("kind",kind==null?"media_capture":kind);v.put("operator_role",operatorRole==null?"OPERATOR":operatorRole);db.getWritableDatabase().insertOrThrow("measurement_sessions",null,v);return id;
    }

    /** Returns the currently open MQ acquisition session, if any. */
    public static String activeMeasurementSession(ExperimentsDb db){
        if(db==null)return null;
        try(Cursor c=db.getReadableDatabase().rawQuery("SELECT session_id FROM measurement_sessions WHERE ended_ts IS NULL AND kind IN ('field_mq_cycle','developer_mq_cycle') ORDER BY started_ts DESC LIMIT 1",null)){return c.moveToFirst()?c.getString(0):null;}
        catch(Exception ignored){return null;}
    }

    public static void finishStandaloneSession(ExperimentsDb db,String sessionId,String payload){
        if(sessionId==null)return;ContentValues v=new ContentValues();v.put("ended_ts",isoNow());if(payload!=null)v.put("payload",payload);db.getWritableDatabase().update("measurement_sessions",v,"session_id=?",new String[]{sessionId});
    }

    public static long saveMedia(ExperimentsDb db,String sessionId,String mediaType,String uri,String startedTs,String endedTs,String metadata){
        ContentValues v=new ContentValues();if(sessionId!=null)v.put("session_id",sessionId);v.put("media_type",mediaType);v.put("uri",uri);if(startedTs!=null)v.put("started_ts",startedTs);if(endedTs!=null)v.put("ended_ts",endedTs);if(metadata!=null)v.put("metadata",metadata);return db.getWritableDatabase().insertOrThrow("media_assets",null,v);
    }

    public static long saveEvent(ExperimentsDb db,String sessionId,long tMs,String eventType,Double confidence,String payload){
        ContentValues v=new ContentValues();if(sessionId!=null)v.put("session_id",sessionId);v.put("t_ms",Math.max(0,tMs));v.put("event_type",eventType);if(confidence!=null&&Double.isFinite(confidence))v.put("confidence",confidence);if(payload!=null)v.put("payload",payload);return db.getWritableDatabase().insertOrThrow("events",null,v);
    }

    public static String isoNow(){SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'",Locale.US);f.setTimeZone(TimeZone.getTimeZone("UTC"));return f.format(new Date());}
}
