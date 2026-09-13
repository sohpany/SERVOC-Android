package com.servoc.app.chemcore;

import android.database.Cursor;

import com.servoc.app.calibration.AcceptedR0Normalizer;
import com.servoc.app.calibration.FieldR0Baseline;
import com.servoc.app.data.ExperimentsDb;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Converts one exact persisted TelemetryRecorder session into the canonical ChemCore feature contract. */
public final class ChemCoreSessionFeatureAdapter {
    public static final String[] SENSOR_ORDER={"mq2","mq4","mq5","mq8","mq3","mq7","mq9","mq135"};
    private ChemCoreSessionFeatureAdapter(){}

    public static final class Extraction {
        public final String sessionId;
        public final ChemCoreFeaturePipeline.Result features;
        public final int persistedSamples;
        public final int samplesWithAnyMq;
        public final double durationSeconds;
        public final double meanTemperatureC,meanRh,meanPressureHpa;
        public final String featurePayloadSha256;
        /** Normalization provenance used to create the feature vector. */
        public final String r0Source,r0Campaign;
        public final int acceptedR0Samples,telemetryRatioFallbackSamples;

        Extraction(String sessionId,ChemCoreFeaturePipeline.Result features,int persistedSamples,int samplesWithAnyMq,
                   double durationSeconds,double meanTemperatureC,double meanRh,double meanPressureHpa,String sha,
                   String r0Source,String r0Campaign,int acceptedR0Samples,int telemetryRatioFallbackSamples){
            this.sessionId=sessionId;this.features=features;this.persistedSamples=persistedSamples;this.samplesWithAnyMq=samplesWithAnyMq;
            this.durationSeconds=durationSeconds;this.meanTemperatureC=meanTemperatureC;this.meanRh=meanRh;this.meanPressureHpa=meanPressureHpa;
            this.featurePayloadSha256=sha;this.r0Source=r0Source==null?"":r0Source;this.r0Campaign=r0Campaign==null?"":r0Campaign;
            this.acceptedR0Samples=Math.max(0,acceptedR0Samples);this.telemetryRatioFallbackSamples=Math.max(0,telemetryRatioFallbackSamples);
        }

        public boolean usable(){return features!=null&&features.sensorCount==SENSOR_ORDER.length&&features.sampleCount>=4&&samplesWithAnyMq>=4;}
        public boolean fullyAcceptedR0Normalized(){return acceptedR0Samples>0&&telemetryRatioFallbackSamples==0;}
    }

    public static Extraction extract(ExperimentsDb db,String sessionId){
        if(db==null||sessionId==null||sessionId.trim().isEmpty())throw new IllegalArgumentException("database and session required");
        String id=sessionId.trim();List<Double> times=new ArrayList<>();List<double[]> rows=new ArrayList<>();long first=-1,last=-1;int any=0,accepted=0,fallback=0;
        double tSum=0,rhSum=0,pSum=0;int tn=0,rhn=0,pn=0;
        try(Cursor c=db.getReadableDatabase().rawQuery("SELECT t_ms,env_payload,mq_payload FROM telemetry_samples WHERE session_id=? ORDER BY t_ms",new String[]{id})){
            while(c.moveToNext()){
                long t=c.getLong(0);if(first<0)first=t;last=t;times.add(t/1000.0);
                JSONObject env=json(c.getString(1)),mq=json(c.getString(2));double[] r=new double[SENSOR_ORDER.length];java.util.Arrays.fill(r,Double.NaN);boolean has=false;
                for(int i=0;i<SENSOR_ORDER.length;i++){
                    JSONObject q=mq.optJSONObject(SENSOR_ORDER[i]);double v=AcceptedR0Normalizer.ratio(SENSOR_ORDER[i],q);
                    if(Double.isFinite(v)&&v>0){r[i]=v;has=true;if(AcceptedR0Normalizer.usesAcceptedFieldR0(SENSOR_ORDER[i],q))accepted++;else fallback++;}
                }
                if(has)any++;rows.add(r);
                double tv=finite(env,"temperature_c","temperature","temp_c");if(Double.isFinite(tv)){tSum+=tv;tn++;}
                double hv=finite(env,"humidity_rh","humidity","rh");if(Double.isFinite(hv)){rhSum+=hv;rhn++;}
                double pv=finite(env,"pressure_hpa","pressure");if(Double.isFinite(pv)){if(pv>2000)pv/=100.0;pSum+=pv;pn++;}
            }
        }
        double[] t=new double[times.size()];double[][] x=new double[rows.size()][];for(int i=0;i<t.length;i++){t[i]=times.get(i);x[i]=rows.get(i);}
        ChemCoreFeaturePipeline.Result f=ChemCoreFeaturePipeline.extract(t,x);String sha=featureSha(f);
        return new Extraction(id,f,rows.size(),any,first>=0&&last>=first?(last-first)/1000.0:0,
                tn>0?tSum/tn:Double.NaN,rhn>0?rhSum/rhn:Double.NaN,pn>0?pSum/pn:Double.NaN,sha,
                AcceptedR0Normalizer.SOURCE,FieldR0Baseline.SESSION,accepted,fallback);
    }

    public static String featureSha(ChemCoreFeaturePipeline.Result result){
        if(result==null)return"";StringBuilder s=new StringBuilder();s.append(result.schemaVersion).append('\n').append(result.sampleCount).append('\n').append(result.sensorCount).append('\n');
        for(double v:result.vector)s.append(Double.isFinite(v)?String.format(Locale.US,"%.17g",v):"NaN").append('\n');
        try{MessageDigest d=MessageDigest.getInstance("SHA-256");byte[] h=d.digest(s.toString().getBytes(StandardCharsets.UTF_8));StringBuilder out=new StringBuilder(64);for(byte b:h)out.append(String.format(Locale.US,"%02x",b&255));return out.toString();}catch(Exception e){throw new IllegalStateException("SHA-256 unavailable",e);}
    }

    private static JSONObject json(String s){try{return s==null||s.isEmpty()?new JSONObject():new JSONObject(s);}catch(Exception e){return new JSONObject();}}
    private static double finite(JSONObject q,String...keys){if(q==null)return Double.NaN;for(String k:keys){if(q.isNull(k))continue;double v=q.optDouble(k,Double.NaN);if(Double.isFinite(v))return v;}return Double.NaN;}
}