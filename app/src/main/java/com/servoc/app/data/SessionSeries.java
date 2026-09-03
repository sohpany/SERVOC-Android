package com.servoc.app.data;

import android.database.Cursor;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Read-only bridge from a saved experiment to the nearest recorded HazRob MQ time-series session. */
public final class SessionSeries {
    private SessionSeries(){}

    public static final class Sample{
        public final long tMs;public final JSONObject env,mq,state;
        Sample(long tMs,String env,String mq,String state){this.tMs=tMs;this.env=json(env);this.mq=json(mq);this.state=json(state);}
    }

    public static final class Series{
        public final String sessionId,startedTs,endedTs;public final List<Sample> samples;
        Series(String id,String start,String end,List<Sample> samples){this.sessionId=id;this.startedTs=start;this.endedTs=end;this.samples=Collections.unmodifiableList(samples);}
        public boolean usable(){return samples.size()>=2;}
        public long durationMs(){return samples.isEmpty()?0:samples.get(samples.size()-1).tMs;}
    }

    /** Returns a measurement session whose end time is closest to the experiment timestamp, only within one hour. */
    public static Series nearest(ExperimentsDb db,String experimentTs){
        if(db==null||experimentTs==null||experimentTs.trim().isEmpty())return null;
        String id=null,start=null,end=null;double delta=Double.POSITIVE_INFINITY;
        String sql="SELECT session_id,started_ts,ended_ts,ABS((julianday(COALESCE(ended_ts,started_ts))-julianday(?))*86400.0) d FROM measurement_sessions WHERE kind IN ('field_mq_cycle','developer_mq_cycle') AND ended_ts IS NOT NULL ORDER BY d ASC LIMIT 1";
        try(Cursor c=db.getReadableDatabase().rawQuery(sql,new String[]{experimentTs})){
            if(c.moveToFirst()){id=c.getString(0);start=c.getString(1);end=c.getString(2);delta=c.getDouble(3);}
        }catch(Exception ignored){return null;}
        if(id==null||!Double.isFinite(delta)||delta>3600.0)return null;
        List<Sample> rows=new ArrayList<>();
        try(Cursor c=db.getReadableDatabase().rawQuery("SELECT t_ms,env_payload,mq_payload,motion_payload FROM telemetry_samples WHERE session_id=? ORDER BY t_ms,id",new String[]{id})){
            while(c.moveToNext())rows.add(new Sample(c.getLong(0),c.getString(1),c.getString(2),c.getString(3)));
        }catch(Exception ignored){return null;}
        return new Series(id,start,end,rows);
    }

    public static double value(JSONObject o,String key){if(o==null||o.isNull(key))return Double.NaN;double v=o.optDouble(key,Double.NaN);return Double.isFinite(v)?v:Double.NaN;}
    public static JSONObject sensor(Sample s,String key){return s==null||s.mq==null?null:s.mq.optJSONObject(key);}
    private static JSONObject json(String s){try{return s==null||s.trim().isEmpty()?new JSONObject():new JSONObject(s);}catch(Exception ignored){return new JSONObject();}}
}
