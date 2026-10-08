package com.servoc.app.data;

import android.database.Cursor;

import com.servoc.app.calibration.AcceptedR0Normalizer;
import com.servoc.app.hardware.HazRobHardwareProfile;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Read-only bridge from a saved experiment to its recorded HazRob MQ time-series session. */
public final class SessionSeries {
    private SessionSeries(){}

    public static final class Sample{
        public final long tMs;public final JSONObject env,mq,state;
        Sample(long tMs,String env,String mq,String state){this.tMs=tMs;this.env=json(env);this.mq=normalizeMq(json(mq));this.state=json(state);}
    }
    public static final class Series{
        public final String sessionId,startedTs,endedTs;public final List<Sample> samples;
        Series(String id,String start,String end,List<Sample> samples){this.sessionId=id;this.startedTs=start;this.endedTs=end;this.samples=Collections.unmodifiableList(samples);}
        public boolean usable(){return samples.size()>=2;}
        public long durationMs(){return samples.isEmpty()?0:samples.get(samples.size()-1).tMs;}
    }

    /**
     * Exact 0.10.2 / HR069 lineage path. Modern analysis, reference generation and reports must
     * never substitute a different measurement session based only on timestamp proximity.
     */
    public static Series forExperiment(ExperimentsDb db,ExperimentQueries.Record r){
        if(db==null||r==null||r.sessionId==null||r.sessionId.trim().isEmpty())return null;
        return byId(db,r.sessionId.trim());
    }

    public static Series byId(ExperimentsDb db,String sessionId){
        if(db==null||sessionId==null||sessionId.trim().isEmpty())return null;String start=null,end=null,id=sessionId.trim();
        try(Cursor c=db.getReadableDatabase().rawQuery("SELECT started_ts,ended_ts FROM measurement_sessions WHERE session_id=? LIMIT 1",new String[]{id})){if(!c.moveToFirst())return null;start=c.getString(0);end=c.getString(1);}catch(Exception ignored){return null;}
        return new Series(id,start,end,readSamples(db,id));
    }

    /**
     * Legacy recovery helper only. It exists so old records can still be inspected manually, but
     * it must not be used by HR069 reference generation, M4 qualification, exposure assessment or
     * new reports because proximity is not scientific lineage.
     */
    @Deprecated
    public static Series nearest(ExperimentsDb db,String experimentTs){
        if(db==null||experimentTs==null||experimentTs.trim().isEmpty())return null;
        try(Cursor c=db.getReadableDatabase().rawQuery("SELECT session_id FROM experiments WHERE ts=? AND session_id IS NOT NULL AND TRIM(session_id)<>'' ORDER BY id DESC LIMIT 1",new String[]{experimentTs})){if(c.moveToFirst()){Series exact=byId(db,c.getString(0));if(exact!=null)return exact;}}catch(Exception ignored){}
        String id=null,start=null,end=null;double delta=Double.POSITIVE_INFINITY;
        String sql="SELECT session_id,started_ts,ended_ts,ABS((julianday(COALESCE(ended_ts,started_ts))-julianday(?))*86400.0) d FROM measurement_sessions WHERE kind IN ('field_mq_cycle','developer_mq_cycle') AND ended_ts IS NOT NULL ORDER BY d ASC LIMIT 1";
        try(Cursor c=db.getReadableDatabase().rawQuery(sql,new String[]{experimentTs})){if(c.moveToFirst()){id=c.getString(0);start=c.getString(1);end=c.getString(2);delta=c.getDouble(3);}}catch(Exception ignored){return null;}
        if(id==null||!Double.isFinite(delta)||delta>3600.0)return null;return new Series(id,start,end,readSamples(db,id));
    }

    private static List<Sample> readSamples(ExperimentsDb db,String id){List<Sample> rows=new ArrayList<>();try(Cursor c=db.getReadableDatabase().rawQuery("SELECT t_ms,env_payload,mq_payload,motion_payload FROM telemetry_samples WHERE session_id=? ORDER BY t_ms,id",new String[]{id})){while(c.moveToNext())rows.add(new Sample(c.getLong(0),c.getString(1),c.getString(2),c.getString(3)));}catch(Exception ignored){}return rows;}

    /**
     * Report/analysis projection must never rewrite an archived sample's
     * device-specific R0 when the operator performs a NEW calibration.
     * Keep original R0, source, timestamp and lab provenance; only repair the
     * ratio against the R0 stored with that exact archived sample.
     */
    private static JSONObject normalizeMq(JSONObject source){
        if(source==null)return new JSONObject();
        try{
            JSONObject out=new JSONObject(source.toString());
            for(HazRobHardwareProfile.Channel hw:HazRobHardwareProfile.MQ){
                JSONObject q=out.optJSONObject(hw.key);if(q==null)continue;
                double frozen=AcceptedR0Normalizer.availableRecordedRatio(hw.key,q);
                if(Double.isFinite(frozen)&&frozen>0){
                    q.put("ratio",frozen);
                    q.put("historical_calibration_frozen",true);
                    double storedR0=q.optDouble("r0",Double.NaN),storedRs=q.optDouble("rs",Double.NaN);
                    q.put("historical_ratio_source",Double.isFinite(storedR0)&&storedR0>0
                        &&Double.isFinite(storedRs)&&storedRs>0?"persisted_rs_r0":"persisted_ratio");
                    String recordedSource=q.optString("r0_source","");
                    boolean deviceLocal=Double.isFinite(storedR0)&&storedR0>0
                        &&("manual_device_r0".equals(recordedSource)||"field_clean_air_r0".equals(recordedSource))
                        &&(q.optBoolean("r0_provenance_complete",false)
                          ||(!q.optString("r0_session","").isEmpty()&&!q.optString("r0_utc","").isEmpty()));
                    q.put("r0_device_local",deviceLocal);
                    q.put("r0_centrally_publishable",false);
                    // Do not claim manual R0, field provenance or M4 which were not actually captured.
                }
            }
            return out;
        }catch(Exception ignored){return source;}
    }

    public static double value(JSONObject o,String key){if(o==null||o.isNull(key))return Double.NaN;double v=o.optDouble(key,Double.NaN);return Double.isFinite(v)?v:Double.NaN;}
    public static JSONObject sensor(Sample s,String key){return s==null||s.mq==null?null:s.mq.optJSONObject(key);}
    private static JSONObject json(String s){try{return s==null||s.trim().isEmpty()?new JSONObject():new JSONObject(s);}catch(Exception ignored){return new JSONObject();}}
}
