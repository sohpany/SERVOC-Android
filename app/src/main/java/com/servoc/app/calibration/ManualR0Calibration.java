package com.servoc.app.calibration;

import android.content.SharedPreferences;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Persistent device-specific R0 values, with the accepted field clean-air baseline as fallback. */
public final class ManualR0Calibration {
    private ManualR0Calibration(){}
    private static final String[] KEYS={"mq2","mq3","mq4","mq5","mq7","mq8","mq9","mq135"};
    private static final Map<String,Double> values=new HashMap<>();
    private static final Map<String,String> sources=new HashMap<>();
    private static boolean loaded=false;

    public static synchronized void load(SharedPreferences p){
        values.clear();sources.clear();
        for(String k:KEYS){
            boolean accepted=false;
            if(p!=null){
                String s=p.getString("manual_r0_"+k,"");
                try{
                    double v=Double.parseDouble(s);
                    if(Double.isFinite(v)&&v>0){
                        values.put(k,v);
                        sources.put(k,p.getString("manual_r0_source_"+k,"manual_device_r0"));
                        accepted=true;
                    }
                }catch(Exception ignored){}
            }
            if(!accepted&&FieldR0Baseline.has(k)){
                values.put(k,FieldR0Baseline.value(k));
                sources.put(k,FieldR0Baseline.SOURCE);
            }
        }
        loaded=true;
    }

    public static synchronized boolean isLoaded(){return loaded;}
    public static synchronized double get(String key){
        key=norm(key);Double v=values.get(key);
        if(v!=null)return v;
        return FieldR0Baseline.value(key);
    }
    public static synchronized String getSource(String key){
        key=norm(key);String s=sources.get(key);
        if(s!=null&&!s.isEmpty())return s;
        return FieldR0Baseline.has(key)?FieldR0Baseline.SOURCE:"unavailable";
    }
    public static synchronized boolean has(String key){double v=get(key);return Double.isFinite(v)&&v>0;}

    /** A manual R0 override is device normalization only; it does not by itself validate a ppm curve. */
    public static synchronized boolean save(SharedPreferences p,String key,double r0){
        key=norm(key);if(p==null||key.isEmpty()||!Double.isFinite(r0)||r0<=0)return false;
        values.put(key,r0);sources.put(key,"manual_device_r0");
        p.edit().putString("manual_r0_"+key,Double.toString(r0)).putString("manual_r0_source_"+key,"manual_device_r0").apply();
        return true;
    }

    /** Removes a manual override and reverts the channel to the accepted field clean-air baseline. */
    public static synchronized void clear(SharedPreferences p,String key){
        key=norm(key);values.remove(key);sources.remove(key);
        if(p!=null)p.edit().remove("manual_r0_"+key).remove("manual_r0_source_"+key).apply();
        if(FieldR0Baseline.has(key)){values.put(key,FieldR0Baseline.value(key));sources.put(key,FieldR0Baseline.SOURCE);}
    }

    /** Recomputes Rs/R0 from received Rs using the active device-specific normalization baseline. */
    public static synchronized void apply(String key,JSONObject q){
        if(q==null)return;key=norm(key);double r0=get(key),rs=q.optDouble("rs",Double.NaN);String source=getSource(key);
        try{
            if(Double.isFinite(r0)&&r0>0&&Double.isFinite(rs)&&rs>0){
                q.put("r0",r0);
                q.put("ratio",rs/r0);
                q.put("r0_calibrated",true);
                q.put("r0_source",source);
                q.put("r0_reference_validated",false);
                if(FieldR0Baseline.SOURCE.equals(source)){
                    q.put("r0_session",FieldR0Baseline.SESSION);
                    q.put("r0_utc",FieldR0Baseline.UTC);
                    q.put("r0_samples",FieldR0Baseline.SAMPLE_COUNT);
                    q.put("r0_window_s",FieldR0Baseline.WINDOW_SECONDS);
                    q.put("r0_environment_compensated",FieldR0Baseline.ENVIRONMENT_COMPENSATED);
                }
            }else{
                q.put("r0_calibrated",false);
                q.put("r0_source","unavailable");
                q.put("r0_reference_validated",false);
            }
        }catch(Exception ignored){}
    }

    public static String summary(){
        StringBuilder s=new StringBuilder();
        for(String k:KEYS){
            if(s.length()>0)s.append('\n');double v=get(k);
            s.append(k.toUpperCase(Locale.US)).append(": ").append(Double.isFinite(v)?String.format(Locale.US,"%.5f",v):"غير معاير");
            if(Double.isFinite(v))s.append(" • ").append(getSource(k));
        }
        return s.toString();
    }

    public static String[] keys(){return KEYS.clone();}
    private static String norm(String key){return key==null?"":key.trim().toLowerCase(Locale.US);}
}
