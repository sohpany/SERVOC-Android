package com.servoc.app.reference;

import android.database.Cursor;
import com.servoc.app.calibration.EmpiricalMqCurve;
import com.servoc.app.data.ExperimentsDb;
import com.servoc.app.data.SessionSeries;
import com.servoc.app.hardware.HazRobHardwareProfile;
import com.servoc.app.models.CalibrationRegressionModels;
import org.json.JSONArray;
import org.json.JSONObject;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;

/** Derives versioned reference artifacts exclusively from APPROVED, averaged, session-linked experiments. */
public final class ReferenceArtifactBuilder {
    private ReferenceArtifactBuilder(){}
    public static final class Result{
        public final boolean built;public final String kind,message;public final long artifactId;public final String payload,sha256;
        Result(boolean built,String kind,long artifactId,String payload,String sha256,String message){this.built=built;this.kind=kind;this.artifactId=artifactId;this.payload=payload;this.sha256=sha256;this.message=message;}
    }
    public static Result rebuild(ExperimentsDb db,long chemicalUid,String kind)throws Exception{if(db==null||chemicalUid<=0)return new Result(false,n(kind),-1,"","","بيانات المرجع غير مكتملة");String k=n(kind).toLowerCase(Locale.US);if("fingerprint".equals(k))return fingerprint(db,chemicalUid);if("calibration".equals(k))return calibration(db,chemicalUid);return new Result(false,k,-1,"","","نوع المرجع غير معروف");}

    private static Result fingerprint(ExperimentsDb db,long chemicalUid)throws Exception{
        List<Long> ids=new ArrayList<>();List<String> sessions=new ArrayList<>();List<double[]> vectors=new ArrayList<>();
        String sql="SELECT e.id,e.session_id,e.capture_id FROM experiments e JOIN captures c ON c.id=e.capture_id WHERE e.approved=1 AND e.chemical_uid=? AND e.reference_kind='fingerprint' AND c.phase='ready_average' ORDER BY e.id";
        try(Cursor c=db.getReadableDatabase().rawQuery(sql,new String[]{String.valueOf(chemicalUid)})){while(c.moveToNext()){long id=c.getLong(0),captureId=c.getLong(2);String session=c.getString(1);double[] v=null;if(session!=null&&!session.isEmpty()){SessionSeries.Series s=SessionSeries.byId(db,session);if(s!=null&&s.usable())v=temporalFingerprint(s);}if(v==null)v=captureVector(db,captureId);if(v==null||valid(v)==0)continue;normalize(v);ids.add(id);sessions.add(session==null?"":session);vectors.add(v);}}
        if(vectors.isEmpty())return new Result(false,"fingerprint",-1,"","","لا توجد تجارب بصمة معتمدة وصالحة بعد");
        JSONObject root=new JSONObject();root.put("schema","hazrob-derived-fingerprint/1");root.put("kind","fingerprint");root.put("chemical_uid",chemicalUid);root.put("created_at_utc",isoNow());root.put("method","normalized temporal |ln(last-quarter median / first-quarter median)|; averaged-ratio fallback");root.put("hardware_profile",HazRobHardwareProfile.ID);root.put("source_experiment_ids",longs(ids));root.put("source_session_ids",strings(sessions));
        JSONArray exposures=new JSONArray();for(int i=0;i<vectors.size();i++){JSONObject x=new JSONObject();x.put("experiment_id",ids.get(i));x.put("session_id",sessions.get(i));x.put("vector",array(vectors.get(i)));exposures.put(x);}root.put("exposures",exposures);
        JSONObject aggregate=new JSONObject();int validChannels=0;for(int j=0;j<HazRobHardwareProfile.MQ.length;j++){List<Double> vals=new ArrayList<>();for(double[] v:vectors)if(j<v.length&&Double.isFinite(v[j]))vals.add(v[j]);if(vals.isEmpty())continue;validChannels++;double mean=mean(vals),sd=sd(vals,mean);JSONObject q=new JSONObject();q.put("mean",mean);q.put("sd",sd);q.put("n",vals.size());aggregate.put(HazRobHardwareProfile.MQ[j].key,q);}root.put("aggregate",aggregate);JSONObject metrics=new JSONObject();metrics.put("approved_exposures",vectors.size());metrics.put("valid_channels",validChannels);metrics.put("normalization","per-exposure max=1");root.put("metrics",metrics);
        String payload=root.toString(2),hash=sha256(payload),version="fingerprint-v1-n"+vectors.size()+"-"+stamp();long id=db.saveReferenceArtifact(chemicalUid,"fingerprint",version,isoNow(),csv(ids),metrics.toString(),payload,hash);return new Result(true,"fingerprint",id,payload,hash,"تم تحديث البصمة المعتمدة من "+vectors.size()+" تجربة");
    }

    private static Result calibration(ExperimentsDb db,long chemicalUid)throws Exception{
        int legacy=-1;String label="Chemical "+chemicalUid;try(Cursor c=db.getReadableDatabase().rawQuery("SELECT legacy_id,COALESCE(NULLIF(name_ar,''),name_en) FROM chemicals WHERE uid=?",new String[]{String.valueOf(chemicalUid)})){if(c.moveToFirst()){legacy=c.isNull(0)?-1:c.getInt(0);label=c.getString(1);}}if(legacy<0)return new Result(false,"calibration",-1,"","","المادة لا تملك معرفاً تشغيلياً صالحاً للمعايرة");
        List<Long> sourceIds=new ArrayList<>();List<Double>[] ratios=new List[HazRobHardwareProfile.MQ.length];List<Double>[] ppms=new List[HazRobHardwareProfile.MQ.length];List<Double>[] temps=new List[HazRobHardwareProfile.MQ.length];List<Double>[] rhs=new List[HazRobHardwareProfile.MQ.length];for(int i=0;i<ratios.length;i++){ratios[i]=new ArrayList<>();ppms[i]=new ArrayList<>();temps[i]=new ArrayList<>();rhs[i]=new ArrayList<>();}
        String sql="SELECT e.id,e.known_ppm,c.sensor_payload,c.temp_c,c.rh FROM experiments e JOIN captures c ON c.id=e.capture_id WHERE e.approved=1 AND e.chemical_uid=? AND e.reference_kind='calibration' AND c.phase='ready_average' AND e.known_ppm IS NOT NULL AND e.known_ppm>0 ORDER BY e.id";
        try(Cursor c=db.getReadableDatabase().rawQuery(sql,new String[]{String.valueOf(chemicalUid)})){while(c.moveToNext()){long id=c.getLong(0);double ppm=c.getDouble(1);JSONObject sensors=json(c.getString(2));boolean any=false;for(int i=0;i<HazRobHardwareProfile.MQ.length;i++){JSONObject q=sensors.optJSONObject(HazRobHardwareProfile.MQ[i].key);double r=q==null?Double.NaN:q.optDouble("ratio",Double.NaN);if(!Double.isFinite(r)||r<=0)continue;ratios[i].add(r);ppms[i].add(ppm);temps[i].add(c.isNull(3)?Double.NaN:c.getDouble(3));rhs[i].add(c.isNull(4)?Double.NaN:c.getDouble(4));any=true;}if(any&&!sourceIds.contains(id))sourceIds.add(id);}}
        if(sourceIds.size()<2)return new Result(false,"calibration",-1,"","","يلزم تعرضان معتمدان على الأقل؛ ويفضل عدة مستويات وتكرارات");
        JSONObject root=new JSONObject();root.put("schema","hazrob-derived-calibration/1");root.put("kind","calibration");root.put("chemical_uid",chemicalUid);root.put("chemical_legacy_id",legacy);root.put("chemical",label);root.put("created_at_utc",isoNow());root.put("source_experiment_ids",longs(sourceIds));root.put("operational_estimator","log-log piecewise interpolation over replicate-aggregated, monotonic approved concentration levels; no extrapolation");
        JSONObject sensorsOut=new JSONObject();int documented=0,active=0;String version="calibration-v1-n"+sourceIds.size()+"-"+stamp();
        for(int i=0;i<HazRobHardwareProfile.MQ.length;i++){
            if(ratios[i].size()<2||levels(ppms[i])<2)continue;double[] x=arr(ratios[i]),y=arr(ppms[i]);List<CalibrationRegressionModels.Score> scores=CalibrationRegressionModels.rank(x,y);if(scores.isEmpty())continue;documented++;CalibrationRegressionModels.Score best=scores.get(0);LevelPoints lv=aggregateLevels(ppms[i],ratios[i]);boolean operational=false;String operationalNote="";
            if(lv.ppm.length>=2){try{new EmpiricalMqCurve(HazRobHardwareProfile.MQ[i].sensorId,legacy,lv.ppm,lv.ratio,System.currentTimeMillis(),label,"validation");db.saveCalibrationCurve(HazRobHardwareProfile.MQ[i].sensorId,legacy,version,isoNow(),label,"Derived only from APPROVED HazRob experiments: "+csv(sourceIds),best.r2,lv.ppm,lv.ratio,finiteMean(temps[i]),finiteMean(rhs[i]),true);operational=true;active++;}catch(IllegalArgumentException ex){operationalNote=ex.getMessage()==null?"non-monotonic approved response":ex.getMessage();}}
            JSONObject q=new JSONObject();q.put("samples",x.length);q.put("concentration_levels",levels(ppms[i]));q.put("ppm_min",min(y));q.put("ppm_max",max(y));q.put("all_ratio_samples",array(x));q.put("all_ppm_samples",array(y));q.put("aggregated_ppm_levels",array(lv.ppm));q.put("aggregated_ratio_medians",array(lv.ratio));q.put("best_model",best.fit.name);q.put("equation",best.fit.equation());q.put("coefficients",array(best.fit.coefficients));q.put("r2_log",best.r2);q.put("log_rmse",best.logRmse);q.put("cross_validated",best.crossValidated);q.put("operational_curve_active",operational);if(!operationalNote.isEmpty())q.put("operational_rejection",operationalNote);sensorsOut.put(HazRobHardwareProfile.MQ[i].key,q);
        }
        if(documented==0)return new Result(false,"calibration",-1,"","","لا يوجد حساس يملك مستويين صالحين على الأقل لتوثيق منحنى");
        root.put("sensors",sensorsOut);JSONObject metrics=new JSONObject();metrics.put("approved_experiments",sourceIds.size());metrics.put("documented_sensor_models",documented);metrics.put("active_monotonic_curves",active);metrics.put("minimum_levels",2);root.put("metrics",metrics);String payload=root.toString(2),hash=sha256(payload);long id=db.saveReferenceArtifact(chemicalUid,"calibration",version,isoNow(),csv(sourceIds),metrics.toString(),payload,hash);String msg=active>0?"تم تحديث المعايرة؛ منحنيات كمية فعالة لـ "+active+" حساسات":"تم توثيق بيانات المعايرة، لكن الاستجابة المعتمدة ليست رتيبة بما يكفي لتفعيل منحنى كمي بعد";return new Result(true,"calibration",id,payload,hash,msg);
    }

    private static final class LevelPoints{final double[] ppm,ratio;LevelPoints(double[] p,double[] r){ppm=p;ratio=r;}}
    private static LevelPoints aggregateLevels(List<Double> ppm,List<Double> ratio){TreeMap<Double,List<Double>> m=new TreeMap<>();for(int i=0;i<ppm.size()&&i<ratio.size();i++){Double p=ppm.get(i),r=ratio.get(i);if(p==null||r==null||!Double.isFinite(p)||!Double.isFinite(r)||p<=0||r<=0)continue;m.computeIfAbsent(p,k->new ArrayList<>()).add(r);}double[] p=new double[m.size()],r=new double[m.size()];int i=0;for(Map.Entry<Double,List<Double>> e:m.entrySet()){p[i]=e.getKey();r[i]=median(e.getValue());i++;}return new LevelPoints(p,r);}
    private static double median(List<Double> v){double[] a=arr(v);Arrays.sort(a);int n=a.length;return n==0?Double.NaN:n%2==1?a[n/2]:(a[n/2-1]+a[n/2])/2.0;}
    private static double[] captureVector(ExperimentsDb db,long captureId){try(Cursor c=db.getReadableDatabase().rawQuery("SELECT sensor_payload FROM captures WHERE id=?",new String[]{String.valueOf(captureId)})){if(!c.moveToFirst())return null;JSONObject root=json(c.getString(0));double[] v=new double[HazRobHardwareProfile.MQ.length];Arrays.fill(v,Double.NaN);for(int i=0;i<v.length;i++){JSONObject q=root.optJSONObject(HazRobHardwareProfile.MQ[i].key);double r=q==null?Double.NaN:q.optDouble("ratio",Double.NaN);if(Double.isFinite(r)&&r>0)v[i]=r;}return v;}}
    private static double[] temporalFingerprint(SessionSeries.Series s){double[] v=new double[HazRobHardwareProfile.MQ.length];Arrays.fill(v,Double.NaN);for(int i=0;i<v.length;i++){String key=HazRobHardwareProfile.MQ[i].key;double a=medianWindow(s,key,0,.25),b=medianWindow(s,key,.75,1);if(Double.isFinite(a)&&Double.isFinite(b)&&a>0&&b>0)v[i]=Math.abs(Math.log(b/a));}return valid(v)==0?null:v;}
    private static double medianWindow(SessionSeries.Series s,String sensor,double f0,double f1){long duration=Math.max(1,s.durationMs());List<Double> a=new ArrayList<>();for(SessionSeries.Sample q:s.samples){double f=(double)q.tMs/duration;if(f<f0||f>f1)continue;JSONObject m=SessionSeries.sensor(q,sensor);double r=m==null?Double.NaN:m.optDouble("ratio",Double.NaN);if(Double.isFinite(r)&&r>0)a.add(r);}return a.isEmpty()?Double.NaN:median(a);}
    private static void normalize(double[] v){double max=0;for(double x:v)if(Double.isFinite(x))max=Math.max(max,Math.abs(x));if(max>0)for(int i=0;i<v.length;i++)if(Double.isFinite(v[i]))v[i]/=max;}
    private static int valid(double[] v){int n=0;for(double x:v)if(Double.isFinite(x))n++;return n;}
    private static int levels(List<Double> v){Set<String>s=new HashSet<>();for(Double x:v)if(x!=null&&Double.isFinite(x))s.add(String.format(Locale.US,"%.9g",x));return s.size();}
    private static double mean(List<Double> v){double s=0;for(double x:v)s+=x;return s/v.size();}
    private static double sd(List<Double> v,double m){if(v.size()<2)return 0;double s=0;for(double x:v)s+=(x-m)*(x-m);return Math.sqrt(s/(v.size()-1));}
    private static Double finiteMean(List<Double> v){double s=0;int n=0;for(Double x:v)if(x!=null&&Double.isFinite(x)){s+=x;n++;}return n==0?null:s/n;}
    private static double[] arr(List<Double> v){double[] x=new double[v.size()];for(int i=0;i<x.length;i++)x[i]=v.get(i);return x;}
    private static double min(double[] a){double v=Double.POSITIVE_INFINITY;for(double x:a)v=Math.min(v,x);return v;}
    private static double max(double[] a){double v=Double.NEGATIVE_INFINITY;for(double x:a)v=Math.max(v,x);return v;}
    private static JSONArray array(double[] a){JSONArray j=new JSONArray();for(double x:a)j.put(Double.isFinite(x)?x:JSONObject.NULL);return j;}
    private static JSONArray longs(List<Long> a){JSONArray j=new JSONArray();for(Long x:a)j.put(x);return j;}
    private static JSONArray strings(List<String> a){JSONArray j=new JSONArray();for(String x:a)j.put(x);return j;}
    private static String csv(List<Long> a){StringBuilder s=new StringBuilder();for(Long x:a){if(s.length()>0)s.append(',');s.append(x);}return s.toString();}
    private static JSONObject json(String s){try{return s==null?new JSONObject():new JSONObject(s);}catch(Exception e){return new JSONObject();}}
    private static String sha256(String s)throws Exception{byte[] h=MessageDigest.getInstance("SHA-256").digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));StringBuilder x=new StringBuilder();for(byte b:h)x.append(String.format(Locale.US,"%02x",b&255));return x.toString();}
    private static String n(String s){return s==null?"":s;}
    private static String isoNow(){SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'",Locale.US);f.setTimeZone(TimeZone.getTimeZone("UTC"));return f.format(new Date());}
    private static String stamp(){SimpleDateFormat f=new SimpleDateFormat("yyyyMMddHHmmss",Locale.US);f.setTimeZone(TimeZone.getTimeZone("UTC"));return f.format(new Date());}
}
