package com.servoc.app.chemcore;

import android.content.ContentValues;
import android.database.Cursor;
import com.servoc.app.data.ExperimentsDb;
import com.servoc.app.data.SessionSeries;
import com.servoc.app.sensors.RobotTelemetry;
import org.json.JSONArray;
import org.json.JSONObject;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Builds canonical ChemCore fingerprint references only from approved session-linked exposures
 * that share the requested sampling profile. This prevents silently mixing chamber/flow domains.
 */
public final class ChemCoreFeatureReferenceBuilder {
    private ChemCoreFeatureReferenceBuilder() {}
    public static final String KIND="chemcore_fingerprint";

    public static final class Result {
        public final boolean built;public final long referenceId;public final int exposures;public final String version,sha256,message;
        Result(boolean built,long referenceId,int exposures,String version,String sha256,String message){this.built=built;this.referenceId=referenceId;this.exposures=exposures;this.version=version;this.sha256=sha256;this.message=message;}
    }

    private static final class Exposure {
        final long experimentId;final String sessionId;final double[] vector;final double t,rh,p;
        Exposure(long experimentId,String sessionId,double[] vector,double t,double rh,double p){this.experimentId=experimentId;this.sessionId=sessionId;this.vector=vector;this.t=t;this.rh=rh;this.p=p;}
    }

    public static Result rebuild(ExperimentsDb db,long chemicalUid,String samplingProfileId)throws Exception{
        if(db==null||chemicalUid<=0)return new Result(false,-1,0,"","","database/chemical required");
        ChemCoreSchema.ensure(db.getWritableDatabase());
        if(samplingProfileId==null||samplingProfileId.trim().isEmpty())return new Result(false,-1,0,"","","explicit sampling profile required for ChemCore reference");
        String profile=samplingProfileId.trim();List<Exposure> exposures=new ArrayList<>();
        String sql="SELECT e.id,e.session_id FROM experiments e JOIN chemcore_session_context cc ON cc.session_id=e.session_id WHERE e.approved=1 AND e.chemical_uid=? AND e.reference_kind='fingerprint' AND e.session_id IS NOT NULL AND TRIM(e.session_id)<>'' AND cc.sampling_profile_id=? ORDER BY e.id";
        try(Cursor c=db.getReadableDatabase().rawQuery(sql,new String[]{String.valueOf(chemicalUid),profile})){
            while(c.moveToNext()){
                long experimentId=c.getLong(0);String sessionId=c.getString(1);SessionSeries.Series s=SessionSeries.byId(db,sessionId);if(s==null||!s.usable())continue;
                ChemCoreSessionFeatures.Result f=ChemCoreSessionFeatures.fromSeries(s);if(f.features.sensorCount!=RobotTelemetry.MQ_KEYS.length||f.features.vector.length==0)continue;
                if(validFraction(f.features.vector)<0.50)continue;
                exposures.add(new Exposure(experimentId,sessionId,Arrays.copyOf(f.features.vector,f.features.vector.length),f.meanTemperatureC,f.meanRh,f.meanPressureHpa));
            }
        }
        if(exposures.size()<2)return new Result(false,-1,exposures.size(),"","","at least two approved ChemCore exposures with the same sampling profile are required");

        int width=exposures.get(0).vector.length;double[] mean=new double[width],sd=new double[width];int[] count=new int[width];
        Arrays.fill(mean,Double.NaN);Arrays.fill(sd,Double.NaN);
        for(int j=0;j<width;j++){
            double sum=0;int n=0;for(Exposure e:exposures){double v=e.vector[j];if(Double.isFinite(v)){sum+=v;n++;}}count[j]=n;if(n==0)continue;double m=sum/n;mean[j]=m;if(n>1){double ss=0;for(Exposure e:exposures){double v=e.vector[j];if(Double.isFinite(v)){double d=v-m;ss+=d*d;}}sd[j]=Math.sqrt(ss/(n-1));}else sd[j]=Double.NaN;
        }

        JSONObject root=new JSONObject();root.put("schema","chemcore-feature-reference/1");root.put("kind",KIND);root.put("chemical_uid",chemicalUid);root.put("created_at_utc",isoNow());root.put("feature_schema",ChemCoreFeaturePipeline.SCHEMA_VERSION);root.put("sampling_profile_id",profile);root.put("sensor_order",new JSONArray(Arrays.asList(RobotTelemetry.MQ_KEYS)));root.put("features_per_sensor",ChemCoreFeaturePipeline.FEATURES_PER_SENSOR);root.put("feature_names",new JSONArray(Arrays.asList(ChemCoreFeaturePipeline.FEATURE_NAMES)));
        JSONArray raw=new JSONArray(),experimentIds=new JSONArray(),sessionIds=new JSONArray();for(Exposure e:exposures){JSONObject q=new JSONObject();q.put("experiment_id",e.experimentId);q.put("session_id",e.sessionId);q.put("vector",array(e.vector));putFinite(q,"temperature_c",e.t);putFinite(q,"rh_pct",e.rh);putFinite(q,"pressure_hpa",e.p);raw.put(q);experimentIds.put(e.experimentId);sessionIds.put(e.sessionId);}root.put("source_experiment_ids",experimentIds);root.put("source_session_ids",sessionIds);root.put("exposures",raw);
        JSONObject aggregate=new JSONObject();aggregate.put("mean",array(mean));aggregate.put("sd",array(sd));JSONArray counts=new JSONArray();for(int n:count)counts.put(n);aggregate.put("n",counts);root.put("aggregate",aggregate);
        JSONObject metrics=new JSONObject();metrics.put("approved_exposures",exposures.size());metrics.put("finite_feature_fraction",finiteFraction(mean));metrics.put("sampling_profile_mixed",false);root.put("metrics",metrics);
        String payload=root.toString(2),hash=sha256(payload),version="chemcore-fingerprint-v1-n"+exposures.size()+"-"+stamp();
        db.getWritableDatabase().execSQL("UPDATE chemcore_feature_references SET active=0 WHERE chemical_uid=? AND kind=? AND sampling_profile_id=? AND feature_schema=?",new Object[]{chemicalUid,KIND,profile,ChemCoreFeaturePipeline.SCHEMA_VERSION});
        ContentValues v=new ContentValues();v.put("chemical_uid",chemicalUid);v.put("kind",KIND);v.put("version",version);v.put("created_ts",isoNow());v.put("feature_schema",ChemCoreFeaturePipeline.SCHEMA_VERSION);v.put("sampling_profile_id",profile);v.put("source_experiment_ids",joinIds(exposures,true));v.put("source_session_ids",joinIds(exposures,false));v.put("exposure_count",exposures.size());v.put("payload",payload);v.put("sha256",hash);v.put("active",1);
        long id=db.getWritableDatabase().insert("chemcore_feature_references",null,v);if(id<0)return new Result(false,-1,exposures.size(),version,hash,"failed to persist ChemCore feature reference");
        return new Result(true,id,exposures.size(),version,hash,"canonical ChemCore fingerprint reference rebuilt");
    }

    private static double validFraction(double[] v){if(v==null||v.length==0)return 0;int n=0;for(double x:v)if(Double.isFinite(x))n++;return (double)n/v.length;}
    private static double finiteFraction(double[] v){return validFraction(v);}
    private static JSONArray array(double[] a){JSONArray j=new JSONArray();for(double x:a)j.put(Double.isFinite(x)?x:JSONObject.NULL);return j;}
    private static void putFinite(JSONObject o,String k,double x)throws Exception{if(Double.isFinite(x))o.put(k,x);else o.put(k,JSONObject.NULL);}
    private static String joinIds(List<Exposure> x,boolean experiments){StringBuilder s=new StringBuilder();for(Exposure e:x){if(s.length()>0)s.append(',');s.append(experiments?String.valueOf(e.experimentId):e.sessionId);}return s.toString();}
    private static String sha256(String s)throws Exception{byte[] h=MessageDigest.getInstance("SHA-256").digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));StringBuilder x=new StringBuilder();for(byte b:h)x.append(String.format(Locale.US,"%02x",b&255));return x.toString();}
    private static String stamp(){SimpleDateFormat f=new SimpleDateFormat("yyyyMMddHHmmss",Locale.US);f.setTimeZone(TimeZone.getTimeZone("UTC"));return f.format(new Date());}
    private static String isoNow(){SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'",Locale.US);f.setTimeZone(TimeZone.getTimeZone("UTC"));return f.format(new Date());}
}
