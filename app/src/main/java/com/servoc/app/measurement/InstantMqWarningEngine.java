package com.servoc.app.measurement;

import org.json.JSONObject;
import java.util.Locale;

/**
 * Fast operational warning layer. It signals MQ response elevation only; chemical identification
 * and quantitative ppm remain later controlled analysis steps.
 *
 * HazRob firmware reports ratio = Rs/R0 using the accepted field clean-air R0 set. Therefore a
 * normalized ratio of 1.0 is a valid operational fallback when the Android clean-air baseline has
 * not yet been captured/persisted. A captured baseline still takes precedence when available.
 */
public final class InstantMqWarningEngine {
    private static final double R0_NORMALIZED_FALLBACK = 1.0;
    private static final double ELEVATED_DELTA = 0.20;
    private static final double HIGH_DELTA = 0.40;

    private InstantMqWarningEngine(){}
    public enum Level{UNAVAILABLE,NORMAL,ELEVATED,HIGH}
    public static final class Result{
        public final Level level;
        public final String label,alert;
        public final double delta;
        public final boolean usedFallbackBaseline;
        public Result(Level l,String label,String alert,double d,boolean fallback){
            this.level=l;this.label=label;this.alert=alert;this.delta=d;this.usedFallbackBaseline=fallback;
        }
        /** Compatibility constructor for states that do not perform baseline comparison (e.g. warm-up). */
        public Result(Level l,String label,String alert,double d){this(l,label,alert,d,false);}
    }

    public static Result evaluate(String key,JSONObject reading,SensorHealthEngine.Snapshot health){
        String target=target(key);
        if(reading==null||!reading.optBoolean("valid",reading.optBoolean("ok",false)))
            return new Result(Level.UNAVAILABLE,target,"",Double.NaN,false);

        double ratio=reading.optDouble("ratio",Double.NaN);
        if(!Double.isFinite(ratio)||ratio<=0)
            return new Result(Level.UNAVAILABLE,target,"",Double.NaN,false);

        double saved=health==null?Double.NaN:health.baselineRatio;
        boolean fallback=!Double.isFinite(saved)||saved<=0;
        double base=fallback?R0_NORMALIZED_FALLBACK:saved;
        double delta=Math.abs(ratio/base-1.0);

        if(delta>=HIGH_DELTA)
            return new Result(Level.HIGH,target,"استجابة عالية متوافقة مع "+target,delta,fallback);
        if(delta>=ELEVATED_DELTA)
            return new Result(Level.ELEVATED,target,"استجابة مرتفعة متوافقة مع "+target,delta,fallback);
        return new Result(Level.NORMAL,target,"",delta,fallback);
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
