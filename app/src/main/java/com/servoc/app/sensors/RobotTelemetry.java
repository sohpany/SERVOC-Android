package com.servoc.app.sensors;

import org.json.JSONObject;
import java.util.Arrays;
import java.util.Locale;

/** Parser/formatter for the current SERVOC ESP32 /diag and /data contracts. */
public final class RobotTelemetry {
    private RobotTelemetry() {}

    public static final String[] MQ_KEYS={"mq2","mq3","mq4","mq5","mq7","mq8","mq9","mq135"};

    public static String formatDiagnostics(JSONObject root){
        if(root==null)return "No telemetry";
        StringBuilder s=new StringBuilder();

        JSONObject drive=root.optJSONObject("drive");
        if(drive!=null){
            s.append("DRIVE  L ").append(drive.optInt("left",0))
             .append("  R ").append(drive.optInt("right",0))
             .append("  failsafe ").append(drive.optBoolean("failsafe",false)?"ACTIVE":"OK").append('\n');
        }

        JSONObject bme=root.optJSONObject("bme");
        if(bme!=null){
            s.append("BME280  ");
            if(!bme.optBoolean("ok",false))s.append("N/A");
            else s.append(fmt(bme,"temperature",1)).append(" °C  ")
                  .append(fmt(bme,"humidity",1)).append(" %RH  ")
                  .append(fmt(bme,"pressure",1)).append(" hPa");
            s.append('\n');
        }

        JSONObject relay=root.optJSONObject("relay");
        if(relay!=null){
            s.append("RELAYS  G1:").append(onOff(relay.optBoolean("group1",false)))
             .append("  G2:").append(onOff(relay.optBoolean("group2",false)))
             .append("  G3:").append(onOff(relay.optBoolean("group3",false))).append('\n');
        }

        JSONObject m=root.optJSONObject("measurement");
        if(m!=null){
            s.append("MEASUREMENT  ").append(m.optString("state","unknown"));
            int g=m.optInt("group",0); if(g>0)s.append("  group ").append(g);
            double rem=m.optDouble("remaining",Double.NaN); if(Double.isFinite(rem)&&rem>0)s.append("  ").append(String.format(Locale.US,"%.1f s",rem));
            s.append("  samples ").append(m.optInt("samples1",0)).append('/')
             .append(m.optInt("samples2",0)).append('/').append(m.optInt("samples3",0)).append('\n');
        }

        JSONObject mq=root.optJSONObject("mq");
        if(mq!=null){
            s.append("MQ LIVE\n");
            for(String key:MQ_KEYS){
                JSONObject r=mq.optJSONObject(key); if(r==null)continue;
                s.append(key.toUpperCase(Locale.US)).append("  ").append(r.optString("status","?"));
                if(!r.isNull("raw"))s.append("  raw ").append(r.optInt("raw",0));
                appendNum(s,r,"adc"," Vadc");
                appendNum(s,r,"ao"," Vao");
                appendNum(s,r,"rs"," Rs");
                appendNum(s,r,"ratio"," Rs/R0");
                s.append('\n');
            }
        }

        if(m!=null && "ready".equalsIgnoreCase(m.optString("state"))){
            JSONObject avg=m.optJSONObject("mq");
            if(avg!=null){
                s.append("AVERAGED SESSION\n");
                for(String key:MQ_KEYS){
                    JSONObject r=avg.optJSONObject(key); if(r==null||!r.optBoolean("valid",false))continue;
                    s.append(key.toUpperCase(Locale.US));
                    appendNum(s,r,"raw"," raw");
                    appendNum(s,r,"ratio"," Rs/R0");
                    s.append('\n');
                }
            }
        }

        return s.toString().trim();
    }

    /** Prefer completed session averages; otherwise return currently valid live ratios. */
    public static double[] extractRatios(JSONObject root){
        double[] out=new double[MQ_KEYS.length]; Arrays.fill(out,Double.NaN);
        if(root==null)return out;
        JSONObject m=root.optJSONObject("measurement");
        JSONObject source=null;
        boolean averaged=false;
        if(m!=null && "ready".equalsIgnoreCase(m.optString("state"))){ source=m.optJSONObject("mq"); averaged=true; }
        if(source==null)source=root.optJSONObject("mq");
        if(source==null)return out;
        for(int i=0;i<MQ_KEYS.length;i++){
            JSONObject r=source.optJSONObject(MQ_KEYS[i]); if(r==null)continue;
            if(averaged && !r.optBoolean("valid",false))continue;
            double ratio=r.optDouble("ratio",Double.NaN);
            if(Double.isFinite(ratio)&&ratio>0)out[i]=ratio;
        }
        return out;
    }

    public static int availableMqCount(JSONObject root){
        if(root==null)return 0; JSONObject mq=root.optJSONObject("mq"); if(mq==null)return 0;
        int n=0; for(String k:MQ_KEYS)if(mq.optJSONObject(k)!=null)n++; return n;
    }

    public static boolean measurementReady(JSONObject root){
        JSONObject m=root==null?null:root.optJSONObject("measurement");
        return m!=null && "ready".equalsIgnoreCase(m.optString("state"));
    }

    private static String onOff(boolean b){return b?"ON":"OFF";}
    private static String fmt(JSONObject o,String key,int d){
        double v=o.optDouble(key,Double.NaN); return Double.isFinite(v)?String.format(Locale.US,"%."+d+"f",v):"NA";
    }
    private static void appendNum(StringBuilder s,JSONObject o,String key,String label){
        double v=o.optDouble(key,Double.NaN); if(Double.isFinite(v))s.append("  ").append(label).append(' ').append(String.format(Locale.US,"%.3f",v));
    }
}
