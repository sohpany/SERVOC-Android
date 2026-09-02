package com.servoc.app.calibration;

import android.content.SharedPreferences;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Persistent device-specific R0 values entered by the developer. */
public final class ManualR0Calibration {
    private ManualR0Calibration(){}
    private static final String[] KEYS={"mq2","mq3","mq4","mq5","mq7","mq8","mq9","mq135"};
    private static final Map<String,Double> values=new HashMap<>();
    private static boolean loaded=false;

    public static synchronized void load(SharedPreferences p){
        values.clear();
        if(p!=null)for(String k:KEYS){String s=p.getString("manual_r0_"+k,"");try{double v=Double.parseDouble(s);if(Double.isFinite(v)&&v>0)values.put(k,v);}catch(Exception ignored){}}
        loaded=true;
    }

    public static synchronized boolean isLoaded(){return loaded;}
    public static synchronized double get(String key){Double v=values.get(norm(key));return v==null?Double.NaN:v;}
    public static synchronized boolean has(String key){double v=get(key);return Double.isFinite(v)&&v>0;}

    public static synchronized boolean save(SharedPreferences p,String key,double r0){
        key=norm(key);if(p==null||key.isEmpty()||!Double.isFinite(r0)||r0<=0)return false;
        values.put(key,r0);p.edit().putString("manual_r0_"+key,Double.toString(r0)).apply();return true;
    }

    public static synchronized void clear(SharedPreferences p,String key){
        key=norm(key);values.remove(key);if(p!=null)p.edit().remove("manual_r0_"+key).apply();
    }

    /** Recomputes Rs/R0 from the received Rs only when a developer-entered R0 exists. */
    public static synchronized void apply(String key,JSONObject q){
        if(q==null)return;double r0=get(key),rs=q.optDouble("rs",Double.NaN);
        try{
            if(Double.isFinite(r0)&&r0>0&&Double.isFinite(rs)&&rs>0){
                q.put("r0",r0);q.put("ratio",rs/r0);q.put("r0_calibrated",true);q.put("r0_source","manual_device_r0");
            }else{
                q.put("r0_calibrated",false);q.put("r0_source","firmware_placeholder_or_unknown");
            }
        }catch(Exception ignored){}
    }

    public static String summary(){
        StringBuilder s=new StringBuilder();for(String k:KEYS){if(s.length()>0)s.append('\n');double v=get(k);s.append(k.toUpperCase(Locale.US)).append(": ").append(Double.isFinite(v)?String.format(Locale.US,"%.5f",v):"غير معاير");}return s.toString();
    }

    public static String[] keys(){return KEYS.clone();}
    private static String norm(String key){return key==null?"":key.trim().toLowerCase(Locale.US);}
}
