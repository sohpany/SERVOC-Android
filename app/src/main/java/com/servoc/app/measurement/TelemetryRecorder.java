package com.servoc.app.measurement;

import android.content.ContentValues;
import android.os.SystemClock;
import com.servoc.app.data.ExperimentsDb;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** Automatically records normalized telemetry across each ESP32 MQ measurement cycle. */
public final class TelemetryRecorder {
    private final ExperimentsDb db;private String sessionId;private long startedElapsed,lastSampleElapsed;
    public TelemetryRecorder(ExperimentsDb db){this.db=db;}

    public synchronized void accept(JSONObject snapshot,int left,int right,boolean developer){
        if(snapshot==null)return;JSONObject m=snapshot.optJSONObject("measurement");String state=m==null?"idle":m.optString("state","idle");long now=SystemClock.elapsedRealtime();
        boolean activeState="warmup".equals(state)||"measuring".equals(state);
        if(sessionId==null&&activeState)start(developer,now);
        if(sessionId!=null&&(now-lastSampleElapsed>=500||"ready".equals(state)||"error".equals(state)||"idle".equals(state))){sample(snapshot,left,right,now);lastSampleElapsed=now;}
        if(sessionId!=null&&( "ready".equals(state)||"error".equals(state)||"idle".equals(state)))finish(snapshot,state);
    }

    public synchronized String activeSessionId(){return sessionId;}

    private void start(boolean developer,long now){sessionId=(developer?"DEV":"FIELD")+"-"+System.currentTimeMillis();startedElapsed=lastSampleElapsed=now;ContentValues v=new ContentValues();v.put("session_id",sessionId);v.put("started_ts",isoNow());v.put("kind",developer?"developer_mq_cycle":"field_mq_cycle");v.put("operator_role",developer?"DEVELOPER":"USER");db.getWritableDatabase().insertOrThrow("measurement_sessions",null,v);}

    private void sample(JSONObject snapshot,int left,int right,long now){ContentValues v=new ContentValues();v.put("session_id",sessionId);v.put("t_ms",Math.max(0,now-startedElapsed));JSONObject e=snapshot.optJSONObject("env"),mq=snapshot.optJSONObject("mq");v.put("env_payload",e==null?"{}":e.toString());v.put("mq_payload",mq==null?"{}":mq.toString());JSONObject motion=new JSONObject();try{motion.put("left",left);motion.put("right",right);JSONObject d=snapshot.optJSONObject("drive");if(d!=null)motion.put("failsafe",d.optBoolean("failsafe",false));}catch(Exception ignored){}v.put("motion_payload",motion.toString());db.getWritableDatabase().insert("telemetry_samples",null,v);}

    private void finish(JSONObject snapshot,String state){ContentValues v=new ContentValues();v.put("ended_ts",isoNow());v.put("payload",snapshot.toString());try{JSONObject meta=new JSONObject();meta.put("terminal_state",state);v.put("analytics",meta.toString());}catch(Exception ignored){}db.getWritableDatabase().update("measurement_sessions",v,"session_id=?",new String[]{sessionId});sessionId=null;startedElapsed=lastSampleElapsed=0;}

    private static String isoNow(){SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'",Locale.US);f.setTimeZone(TimeZone.getTimeZone("UTC"));return f.format(new Date());}
}
