package com.servoc.app.measurement;

import org.json.JSONObject;
import java.util.Locale;

/** Fast operational warning layer. It signals sensor-response elevation; chemical identification remains a later analysis step. */
public final class InstantMqWarningEngine {
    private InstantMqWarningEngine(){}
    public enum Level{UNAVAILABLE,NORMAL,ELEVATED,HIGH}
    public static final class Result{public final Level level;public final String label,alert;public final double delta;Result(Level l,String label,String alert,double d){this.level=l;this.label=label;this.alert=alert;this.delta=d;}}

    public static Result evaluate(String key,JSONObject reading,SensorHealthEngine.Snapshot health){
        String target=target(key);if(reading==null||!reading.optBoolean("valid",reading.optBoolean("ok",false)))return new Result(Level.UNAVAILABLE,target,"",Double.NaN);
        double ratio=reading.optDouble("ratio",Double.NaN),base=health==null?Double.NaN:health.baselineRatio;
        if(!Double.isFinite(ratio)||ratio<=0||!Double.isFinite(base)||base<=0)return new Result(Level.NORMAL,target,"",Double.NaN);
        double delta=Math.abs(ratio/base-1.0);
        if(delta>=0.40)return new Result(Level.HIGH,target,"استجابة عالية متوافقة مع "+target,delta);
        if(delta>=0.20)return new Result(Level.ELEVATED,target,"استجابة مرتفعة متوافقة مع "+target,delta);
        return new Result(Level.NORMAL,target,"",delta);
    }

    public static String target(String key){
        String k=key==null?"":key.toLowerCase(Locale.US);
        switch(k){
            case "mq2":return"الدخان / الغازات القابلة للاشتعال";
            case "mq3":return"الكحول";
            case "mq4":return"الميثان / الغاز الطبيعي";
            case "mq5":return"LPG / الغاز الطبيعي";
            case "mq7":return"أول أكسيد الكربون";
            case "mq8":return"الهيدروجين";
            case "mq9":return"أول أكسيد الكربون / الغازات القابلة للاشتعال";
            case "mq135":return"غازات ملوثة / جودة الهواء";
            default:return"استجابة غازية";
        }
    }
}
