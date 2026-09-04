package com.servoc.app.experiments;

import com.servoc.app.chemical.ChemicalRepository;
import com.servoc.app.data.ExperimentQueries;
import com.servoc.app.data.ExperimentsDb;
import com.servoc.app.data.SessionSeries;
import com.servoc.app.hardware.HazRobHardwareProfile;
import com.servoc.app.models.CalibrationRegressionModels;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Converts an operator-approved reference experiment into a traceable scientific artifact.
 * Fingerprints are derived from the exact recorded session. Quantitative calibration bundles
 * use only already-approved experiments plus the current candidate being approved.
 */
public final class ReferenceArtifactBuilder {
    private ReferenceArtifactBuilder(){}

    public static final class Result{
        public final String kind;
        public final JSONObject artifact;
        public final String summary;
        Result(String kind,JSONObject artifact,String summary){this.kind=kind;this.artifact=artifact;this.summary=summary;}
    }

    public static Result build(ExperimentsDb db,ExperimentQueries.Record current,ChemicalRepository.Chemical chemical,String approvedTs)throws Exception{
        if(db==null||current==null||chemical==null)throw new IllegalArgumentException("reference context");
        JSONObject p=json(current.payload);String mode=p.optString("reference_mode","fingerprint");
        if("quantitative".equals(mode))return buildCalibration(db,current,chemical,approvedTs);
        return buildFingerprint(db,current,chemical,approvedTs);
    }

    public static void persist(ExperimentsDb db,ChemicalRepository.Chemical chemical,Result result,String approvedTs)throws Exception{
        if(db==null||chemical==null||result==null)return;int legacy=chemical.legacyId==null?-1:chemical.legacyId;
        if("fingerprint".equals(result.kind)){
            String label="APPROVED • "+displayName(chemical)+" • "+approvedTs;
            db.saveFingerprint(legacy,label,approvedTs,result.artifact.toString());
            return;
        }
        JSONArray curves=result.artifact.optJSONArray("curves");if(curves==null)return;
        for(int i=0;i<curves.length();i++){
            JSONObject q=curves.optJSONObject(i);if(q==null)continue;String sensor=q.optString("sensor","");if(sensor.isEmpty())continue;
            JSONObject metrics=new JSONObject();metrics.put("r2_log",q.optDouble("r2_log",Double.NaN));metrics.put("log_rmse",q.optDouble("log_rmse",Double.NaN));metrics.put("cross_validated",q.optBoolean("cross_validated",false));metrics.put("concentration_levels",q.optInt("concentration_levels",0));metrics.put("operator_approved",true);
            db.saveModel(legacy,"CURVE_1D:"+sensor,"hazrob-approved-reference/1",approvedTs,q.optInt("samples",0),metrics.toString(),q.toString(),true);
        }
    }

    private static Result buildFingerprint(ExperimentsDb db,ExperimentQueries.Record r,ChemicalRepository.Chemical chemical,String approvedTs)throws Exception{
        JSONObject payload=json(r.payload);String sid=payload.optString("reference_session_id","").trim();
        SessionSeries.Series s=SessionSeries.byId(db,sid);if(s==null||!s.usable())throw new IllegalStateException("exact reference session unavailable");
        JSONObject sensors=new JSONObject();double max=0;double[] response=new double[HazRobHardwareProfile.MQ.length],base=new double[response.length],end=new double[response.length];
        for(int i=0;i<HazRobHardwareProfile.MQ.length;i++){
            String key=HazRobHardwareProfile.MQ[i].key;base[i]=medianWindow(s,key,0.00,0.25);end[i]=medianWindow(s,key,0.75,1.00);
            double v=Double.isFinite(base[i])&&Double.isFinite(end[i])&&base[i]>0&&end[i]>0?Math.abs(Math.log(end[i]/base[i])):Double.NaN;response[i]=v;if(Double.isFinite(v))max=Math.max(max,v);
        }
        int valid=0;for(int i=0;i<response.length;i++){
            String key=HazRobHardwareProfile.MQ[i].key;JSONObject q=new JSONObject();q.put("baseline_ratio",finiteOrNull(base[i]));q.put("exposed_ratio",finiteOrNull(end[i]));q.put("log_response",finiteOrNull(response[i]));
            double norm=Double.isFinite(response[i])&&max>0?response[i]/max:Double.NaN;q.put("normalized_response",finiteOrNull(norm));if(Double.isFinite(norm))valid++;sensors.put(key,q);
        }
        if(valid<2)throw new IllegalStateException("insufficient fingerprint channels");
        JSONObject a=baseArtifact("hazrob-approved-fingerprint/1","FINGERPRINT",r,chemical,approvedTs);a.put("session_samples",s.samples.size());a.put("session_duration_ms",s.durationMs());a.put("method","abs(ln(median(last25%)/median(first25%))) normalized to strongest valid channel");a.put("valid_channels",valid);a.put("sensors",sensors);
        return new Result("fingerprint",a,"بصمة معتمدة موثقة من "+valid+" قنوات MQ");
    }

    private static Result buildCalibration(ExperimentsDb db,ExperimentQueries.Record current,ChemicalRepository.Chemical chemical,String approvedTs)throws Exception{
        List<ExperimentQueries.Record> rows=new ArrayList<>();for(ExperimentQueries.Record r:ExperimentQueries.recent(db,1000))if(r.approved)rows.add(r);rows.add(current);
        JSONArray curves=new JSONArray();int eligible=0;
        for(HazRobHardwareProfile.Channel hw:HazRobHardwareProfile.MQ){
            List<Double> x=new ArrayList<>(),y=new ArrayList<>();Set<String> levels=new HashSet<>();
            for(ExperimentQueries.Record r:rows){
                JSONObject p=json(r.payload);if(!"quantitative".equals(p.optString("reference_mode","")))continue;if(p.optLong("reference_chemical_uid",-1)!=chemical.uid)continue;if(!"ready_average".equals(p.optString("reference_capture_state","")))continue;
                double ppm=p.optDouble("reference_known_ppm",Double.NaN);JSONObject mq=p.optJSONObject("reference_sensor_average");JSONObject q=mq==null?null:mq.optJSONObject(hw.key);double ratio=q==null?Double.NaN:q.optDouble("ratio",Double.NaN);
                if(Double.isFinite(ppm)&&ppm>0&&Double.isFinite(ratio)&&ratio>0){x.add(ratio);y.add(ppm);levels.add(String.format(Locale.US,"%.9g",ppm));}
            }
            if(x.size()<3||levels.size()<3)continue;
            double[] xx=arr(x),yy=arr(y);List<CalibrationRegressionModels.Score> scores=CalibrationRegressionModels.rank(xx,yy);if(scores.isEmpty())continue;CalibrationRegressionModels.Score best=scores.get(0);eligible++;
            JSONObject q=new JSONObject();q.put("sensor",hw.key);q.put("samples",best.samples);q.put("concentration_levels",levels.size());q.put("catalog_index",best.fit.catalogIndex);q.put("model",best.fit.name);q.put("equation",best.fit.equation());q.put("r2_log",best.r2);q.put("log_rmse",best.logRmse);q.put("cross_validated",best.crossValidated);JSONArray coef=new JSONArray();for(double c:best.fit.coefficients)coef.put(c);q.put("coefficients",coef);q.put("validation_scope","operator-approved research reference; not a safety-grade certified calibration");curves.put(q);
        }
        JSONObject a=baseArtifact("hazrob-approved-calibration-bundle/1","CALIBRATION",current,chemical,approvedTs);a.put("curves",curves);a.put("eligible_sensors",eligible);a.put("status",eligible>0?"APPROVED_REFERENCE_CURVES":"APPROVED_POINTS_PENDING_MORE_LEVELS");a.put("selection_rule","lowest available log-RMSE among implemented CURVE_1D models for each sensor, using approved reference points plus current approval candidate");
        String summary=eligible>0?"تم تكوين "+eligible+" منحنيات مرجعية معتمدة":"تم اعتماد نقطة المعايرة؛ يلزم 3 مستويات تركيز صالحة على الأقل لتكوين منحنى";
        return new Result("calibration",a,summary);
    }

    private static JSONObject baseArtifact(String schema,String kind,ExperimentQueries.Record r,ChemicalRepository.Chemical chemical,String approvedTs)throws Exception{
        JSONObject a=new JSONObject();a.put("schema",schema);a.put("status","APPROVED");a.put("artifact_kind",kind);a.put("approved_at_utc",approvedTs);a.put("source_experiment_id",r.id);JSONObject p=json(r.payload);a.put("source_session_id",p.optString("reference_session_id",""));a.put("source_capture_id",p.optLong("reference_capture_id",-1));
        JSONObject c=new JSONObject();c.put("uid",chemical.uid);if(chemical.legacyId!=null)c.put("legacy_id",chemical.legacyId);c.put("name",chemical.name);c.put("name_ar",chemical.arabicName);c.put("formula",chemical.formula);c.put("cas",chemical.cas);a.put("chemical",c);return a;
    }

    private static double medianWindow(SessionSeries.Series s,String sensor,double f0,double f1){
        if(s==null||s.samples.isEmpty())return Double.NaN;long duration=Math.max(1,s.durationMs());List<Double> vals=new ArrayList<>();
        for(SessionSeries.Sample q:s.samples){double f=(double)q.tMs/duration;if(f<f0||f>f1)continue;JSONObject m=SessionSeries.sensor(q,sensor);double v=m==null?Double.NaN:m.optDouble("ratio",Double.NaN);if(Double.isFinite(v)&&v>0)vals.add(v);}
        if(vals.isEmpty())return Double.NaN;double[] a=arr(vals);Arrays.sort(a);int n=a.length;return(n&1)==1?a[n/2]:(a[n/2-1]+a[n/2])*0.5;
    }

    private static double[] arr(List<Double> v){double[] a=new double[v.size()];for(int i=0;i<a.length;i++)a[i]=v.get(i);return a;}
    private static Object finiteOrNull(double v){return Double.isFinite(v)?v:JSONObject.NULL;}
    private static JSONObject json(String s){try{return s==null||s.trim().isEmpty()?new JSONObject():new JSONObject(s);}catch(Exception e){return new JSONObject();}}
    private static String displayName(ChemicalRepository.Chemical c){if(c.arabicName!=null&&!c.arabicName.trim().isEmpty())return c.arabicName;return c.name==null?"chemical":c.name;}
}
