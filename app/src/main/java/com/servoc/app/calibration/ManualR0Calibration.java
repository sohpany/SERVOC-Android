package com.servoc.app.calibration;

import android.content.SharedPreferences;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/** Persistent device-specific R0 values, with the accepted field clean-air baseline as fallback. */
public final class ManualR0Calibration {
    private ManualR0Calibration(){}
    private static final String[] KEYS={"mq2","mq3","mq4","mq5","mq7","mq8","mq9","mq135"};
    private static final Map<String,Double> values=new HashMap<>();
    private static final Map<String,String> sources=new HashMap<>(),sessions=new HashMap<>(),utcs=new HashMap<>();
    private static boolean loaded=false;

    public static synchronized void load(SharedPreferences p){
        values.clear();sources.clear();sessions.clear();utcs.clear();
        for(String k:KEYS){
            boolean accepted=false;
            if(p!=null){
                String s=p.getString("manual_r0_"+k,"");
                try{
                    double v=Double.parseDouble(s);
                    if(Double.isFinite(v)&&v>0){
                        values.put(k,v);sources.put(k,p.getString("manual_r0_source_"+k,"manual_device_r0"));
                        String session=p.getString("manual_r0_session_"+k,"");if(!session.isEmpty())sessions.put(k,session);
                        String utc=p.getString("manual_r0_utc_"+k,"");if(!utc.isEmpty())utcs.put(k,utc);
                        accepted=true;
                    }
                }catch(Exception ignored){}
            }
            if(!accepted&&FieldR0Baseline.has(k))setFieldFallback(k);
        }
        loaded=true;
    }

    public static synchronized boolean isLoaded(){return loaded;}
    public static synchronized double get(String key){key=norm(key);Double v=values.get(key);if(v!=null)return v;return FieldR0Baseline.value(key);}
    public static synchronized String getSource(String key){key=norm(key);String s=sources.get(key);if(s!=null&&!s.isEmpty())return s;return FieldR0Baseline.has(key)?FieldR0Baseline.SOURCE:"unavailable";}
    public static synchronized String getSession(String key){key=norm(key);String s=sessions.get(key);if(s!=null)return s;return FieldR0Baseline.has(key)&&FieldR0Baseline.SOURCE.equals(getSource(key))?FieldR0Baseline.SESSION:"";}
    public static synchronized String getUtc(String key){key=norm(key);String s=utcs.get(key);if(s!=null)return s;return FieldR0Baseline.has(key)&&FieldR0Baseline.SOURCE.equals(getSource(key))?FieldR0Baseline.UTC:"";}
    public static synchronized boolean has(String key){double v=get(key);return Double.isFinite(v)&&v>0;}

    /** A manual R0 override is device normalization only; it does not by itself validate a ppm curve. */
    public static synchronized boolean save(SharedPreferences p,String key,double r0){
        key=norm(key);if(p==null||key.isEmpty()||!Double.isFinite(r0)||r0<=0)return false;
        long now=System.currentTimeMillis();String utc=iso(now),session="MANUAL-R0-"+now;
        values.put(key,r0);sources.put(key,"manual_device_r0");sessions.put(key,session);utcs.put(key,utc);
        p.edit().putString("manual_r0_"+key,Double.toString(r0)).putString("manual_r0_source_"+key,"manual_device_r0")
                .putString("manual_r0_session_"+key,session).putString("manual_r0_utc_"+key,utc).apply();
        return true;
    }

    /** Removes a manual override and reverts the channel to the accepted field clean-air baseline. */
    public static synchronized void clear(SharedPreferences p,String key){
        key=norm(key);values.remove(key);sources.remove(key);sessions.remove(key);utcs.remove(key);
        if(p!=null)p.edit().remove("manual_r0_"+key).remove("manual_r0_source_"+key).remove("manual_r0_session_"+key).remove("manual_r0_utc_"+key).apply();
        if(FieldR0Baseline.has(key))setFieldFallback(key);
    }

    /** Recomputes Rs/R0 from received Rs using the active device-specific normalization baseline. */
    public static synchronized void apply(String key,JSONObject q){
        if(q==null)return;key=norm(key);double r0=get(key),rs=q.optDouble("rs",Double.NaN);String source=getSource(key),session=getSession(key),utc=getUtc(key);
        try{
            if(Double.isFinite(r0)&&r0>0&&Double.isFinite(rs)&&rs>0){
                q.put("r0",r0);q.put("ratio",rs/r0);q.put("r0_calibrated",true);q.put("r0_source",source);q.put("r0_reference_validated",false);
                if(!session.isEmpty())q.put("r0_session",session);if(!utc.isEmpty())q.put("r0_utc",utc);
                long age=ageMs(utc);q.put("r0_age_known",age>=0);if(age>=0)q.put("r0_age_ms",age);
                q.put("r0_provenance_complete",!source.isEmpty()&&!"unavailable".equals(source)&&!session.isEmpty()&&!utc.isEmpty());
                if(FieldR0Baseline.SOURCE.equals(source)){
                    q.put("r0_samples",FieldR0Baseline.SAMPLE_COUNT);q.put("r0_window_s",FieldR0Baseline.WINDOW_SECONDS);q.put("r0_environment_compensated",FieldR0Baseline.ENVIRONMENT_COMPENSATED);
                }
            }else{
                q.put("r0_calibrated",false);q.put("r0_source","unavailable");q.put("r0_reference_validated",false);q.put("r0_age_known",false);q.put("r0_provenance_complete",false);
            }
        }catch(Exception ignored){}
    }

    public static String summary(){
        StringBuilder s=new StringBuilder();
        for(String k:KEYS){if(s.length()>0)s.append('\n');double v=get(k);s.append(k.toUpperCase(Locale.US)).append(": ").append(Double.isFinite(v)?String.format(Locale.US,"%.5f",v):"غير معاير");if(Double.isFinite(v))s.append(" • ").append(getSource(k));}
        return s.toString();
    }

    public static String[] keys(){return KEYS.clone();}
    private static void setFieldFallback(String k){values.put(k,FieldR0Baseline.value(k));sources.put(k,FieldR0Baseline.SOURCE);sessions.put(k,FieldR0Baseline.SESSION);utcs.put(k,FieldR0Baseline.UTC);}
    private static String norm(String key){return key==null?"":key.trim().toLowerCase(Locale.US);}
    private static String iso(long ms){SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'",Locale.US);f.setTimeZone(TimeZone.getTimeZone("UTC"));return f.format(new Date(ms));}
    private static long ageMs(String utc){if(utc==null||utc.isEmpty())return -1;try{SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'",Locale.US);f.setTimeZone(TimeZone.getTimeZone("UTC"));Date d=f.parse(utc);return d==null?-1:Math.max(0,System.currentTimeMillis()-d.getTime());}catch(Exception e){return -1;}}
}
