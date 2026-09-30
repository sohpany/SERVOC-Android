package com.servoc.app.exposure;

import android.database.Cursor;

import com.servoc.app.calibration.AcceptedR0Normalizer;
import com.servoc.app.chemcore.ChemCoreDecision;
import com.servoc.app.chemcore.ChemCoreResponseModel;
import com.servoc.app.chemcore.InverseMolecularSolver;
import com.servoc.app.chemcore.SamplingProfile;
import com.servoc.app.data.ExperimentsDb;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * P10 device-validated quantitative time series.
 *
 * Only M4 response models are admitted for the reportable quantitative series. Every accepted
 * quantitative point is retained with its session timestamp so reports and later re-analysis can
 * inspect the complete derived ppm history. Raw/normalized telemetry remains preserved separately
 * in telemetry_samples and is never discarded merely because a point is not quantitatively valid.
 */
public final class ChemCoreQuantitativeTimeSeries {
    public static final String SCHEMA="hazrob-quantitative-time-series/1";
    private static final long MAX_CONTIGUOUS_GAP_MS=2_000L;
    private ChemCoreQuantitativeTimeSeries(){}

    public static final class Point {
        public final long tMs;
        public final double ppm;
        public Point(long tMs,double ppm){this.tMs=Math.max(0,tMs);this.ppm=ppm;}
    }

    private static final class WindowSpec {
        final String role;final double durationMin;
        WindowSpec(String role,double durationMin){this.role=role;this.durationMin=durationMin;}
    }

    public static JSONObject extract(ExperimentsDb db,String sessionId,long chemicalUid,
                                     List<InverseMolecularSolver.Candidate> candidates,
                                     SamplingProfile profile,List<ExposureRepository.Limit> limits){
        if(db==null||sessionId==null||sessionId.trim().isEmpty())return withheld(sessionId,chemicalUid,"session_required");
        InverseMolecularSolver.Candidate source=findCandidate(candidates,chemicalUid);if(source==null)return withheld(sessionId,chemicalUid,"candidate_not_found");
        InverseMolecularSolver.Candidate m4=m4Only(source);if(m4==null)return withheld(sessionId,chemicalUid,"m4_models_required");
        List<Point> points=new ArrayList<>();int persisted=0,valid=0;double tSum=0,rhSum=0,pSum=0;int tn=0,rhn=0,pn=0;
        try(Cursor c=db.getReadableDatabase().rawQuery("SELECT t_ms,env_payload,mq_payload FROM telemetry_samples WHERE session_id=? ORDER BY t_ms,id",new String[]{sessionId.trim()})){
            while(c.moveToNext()){
                persisted++;long t=c.getLong(0);JSONObject env=json(c.getString(1)),mq=json(c.getString(2));Map<String,Double> ratios=new LinkedHashMap<>();
                for(ChemCoreResponseModel model:m4.models){String key=model.sensorKey();JSONObject q=mq.optJSONObject(key);double ratio=AcceptedR0Normalizer.ratio(key,q);if(Double.isFinite(ratio)&&ratio>0)ratios.put(key,ratio);}
                double tc=finite(env,"temperature_c","temperature","temp_c"),rh=finite(env,"humidity_rh","humidity","rh"),ph=finite(env,"pressure_hpa","pressure");if(Double.isFinite(ph)&&ph>2000)ph/=100.0;
                if(Double.isFinite(tc)){tSum+=tc;tn++;}if(Double.isFinite(rh)){rhSum+=rh;rhn++;}if(Double.isFinite(ph)){pSum+=ph;pn++;}
                InverseMolecularSolver.Result r=InverseMolecularSolver.solveOne(ratios,m4,tc,rh,profile);
                if(r!=null&&r.solvable&&r.hasDeviceValidatedQuantitativeSupport()&&Double.isFinite(r.bestPpm)&&r.bestPpm>=0){points.add(new Point(t,r.bestPpm));valid++;}
            }
        }catch(Exception e){return withheld(sessionId,chemicalUid,"telemetry_read_failure");}
        return summarize(sessionId,chemicalUid,points,persisted,valid,limits,tn>0?tSum/tn:Double.NaN,rhn>0?rhSum/rhn:Double.NaN,pn>0?pSum/pn:Double.NaN,m4);
    }

    /** Pure-Java summary used by unit tests and report replay. */
    public static JSONObject summarize(String sessionId,long chemicalUid,List<Point> points,int persistedSamples,int validSamples,
                                       List<ExposureRepository.Limit> limits,double meanTemperatureC,double meanRh,double meanPressureHpa,
                                       InverseMolecularSolver.Candidate m4Candidate){
        try{
            List<Point> clean=clean(points);JSONObject out=new JSONObject();out.put("schema",SCHEMA);out.put("session_id",sessionId==null?"":sessionId);out.put("chemical_uid",chemicalUid);
            out.put("persisted_samples",Math.max(0,persistedSamples));out.put("valid_ppm_samples",Math.max(0,validSamples));out.put("m4_only",true);out.put("max_contiguous_gap_ms",MAX_CONTIGUOUS_GAP_MS);
            out.put("raw_telemetry_preserved_elsewhere",true);out.put("raw_telemetry_source","telemetry_samples");out.put("all_valid_quantitative_points_preserved",true);
            out.put("model_count",m4Candidate==null?0:m4Candidate.models.size());JSONArray models=new JSONArray();if(m4Candidate!=null)for(ChemCoreResponseModel m:m4Candidate.models)models.put(new JSONObject().put("sensor",m.sensorKey()).put("model_id",m.modelId()).put("source_id",m.sourceId()).put("evidence_tier",m.evidenceTier().name()));out.put("models",models);
            JSONObject env=new JSONObject();putFinite(env,"mean_temperature_c",meanTemperatureC);putFinite(env,"mean_rh",meanRh);putFinite(env,"mean_pressure_hpa",meanPressureHpa);env.put("temperature_available",Double.isFinite(meanTemperatureC));env.put("pressure_available",Double.isFinite(meanPressureHpa));out.put("environment_basis",env);
            JSONArray pointRows=new JSONArray();for(Point p:clean)pointRows.put(new JSONObject().put("t_ms",p.tMs).put("ppm",p.ppm));out.put("points",pointRows);
            if(clean.isEmpty()){out.put("valid",false);out.put("reason","no_valid_m4_ppm_samples");out.put("windows",new JSONArray());return out;}
            out.put("valid",true);out.put("first_t_ms",clean.get(0).tMs);out.put("last_t_ms",clean.get(clean.size()-1).tMs);out.put("latest_ppm",clean.get(clean.size()-1).ppm);
            double min=Double.POSITIVE_INFINITY,max=Double.NEGATIVE_INFINITY;for(Point p:clean){min=Math.min(min,p.ppm);max=Math.max(max,p.ppm);}out.put("min_ppm",min);out.put("max_ppm",max);
            Integration full=integrate(clean,clean.get(0).tMs,clean.get(clean.size()-1).tMs);putFinite(out,"covered_minutes",full.coveredMs/60_000.0);putFinite(out,"time_weighted_mean_ppm",full.valid?full.averagePpm:Double.NaN);
            JSONArray windows=new JSONArray();for(WindowSpec e:windowDurations(limits)){String role=e.role;double minReq=e.durationMin;long end=clean.get(clean.size()-1).tMs,start=end-Math.round(minReq*60_000.0);Integration w=integrate(clean,start,end);JSONObject j=new JSONObject();j.put("role",role);j.put("duration_min",minReq);j.put("covered_minutes",w.coveredMs/60_000.0);j.put("average_ppm",w.valid?w.averagePpm:JSONObject.NULL);j.put("valid",w.valid&&w.coveredMs+1000>=Math.round(minReq*60_000.0));j.put("coverage_fraction",minReq>0?Math.min(1.0,w.coveredMs/(minReq*60_000.0)):0);windows.put(j);}out.put("windows",windows);
            return out;
        }catch(Exception e){return withheld(sessionId,chemicalUid,"temporal_summary_failure");}
    }

    private static final class Integration{final boolean valid;final long coveredMs;final double averagePpm;Integration(boolean v,long c,double a){valid=v;coveredMs=c;averagePpm=a;}}
    private static Integration integrate(List<Point> p,long start,long end){
        if(p==null||p.size()<2||end<=start)return new Integration(false,0,Double.NaN);double area=0;long covered=0;
        for(int i=1;i<p.size();i++){Point a=p.get(i-1),b=p.get(i);if(b.tMs<=a.tMs)continue;long segStart=Math.max(start,a.tMs),segEnd=Math.min(end,b.tMs);long fullGap=b.tMs-a.tMs;if(segEnd<=segStart||fullGap>MAX_CONTIGUOUS_GAP_MS)continue;
            double fa=(segStart-a.tMs)/(double)fullGap,fb=(segEnd-a.tMs)/(double)fullGap;double ya=a.ppm+(b.ppm-a.ppm)*fa,yb=a.ppm+(b.ppm-a.ppm)*fb;long dt=segEnd-segStart;area+=(ya+yb)*0.5*dt;covered+=dt;
        }
        return new Integration(covered>0,covered,covered>0?area/covered:Double.NaN);
    }

    private static List<WindowSpec> windowDurations(List<ExposureRepository.Limit> limits){
        Map<String,WindowSpec> unique=new LinkedHashMap<>();if(limits==null)return new ArrayList<>();
        for(ExposureRepository.Limit l:limits){
            if(l==null||l.durationMin==null||!Double.isFinite(l.durationMin)||l.durationMin<=0)continue;
            String r=role(l.type);if(!"STEL".equals(r)&&!"TWA".equals(r))continue;
            String key=r+"@"+Long.toString(Math.round(l.durationMin*60_000.0));unique.putIfAbsent(key,new WindowSpec(r,l.durationMin));
        }
        return new ArrayList<>(unique.values());
    }
    private static String role(String type){String t=type==null?"":type.trim().toUpperCase(Locale.US).replace('-','_').replace(' ','_');if(t.contains("STEL")||t.contains("SHORT_TERM"))return"STEL";if(t.contains("TWA")||t.contains("PEL")||t.contains("REL"))return"TWA";return"OTHER";}
    private static List<Point> clean(List<Point> in){List<Point> out=new ArrayList<>();if(in!=null)for(Point p:in)if(p!=null&&Double.isFinite(p.ppm)&&p.ppm>=0){if(out.isEmpty()||p.tMs>out.get(out.size()-1).tMs)out.add(p);}return out;}
    private static InverseMolecularSolver.Candidate findCandidate(List<InverseMolecularSolver.Candidate> rows,long uid){if(rows!=null)for(InverseMolecularSolver.Candidate c:rows)if(c!=null&&c.chemicalUid==uid)return c;return null;}
    private static InverseMolecularSolver.Candidate m4Only(InverseMolecularSolver.Candidate c){List<ChemCoreResponseModel> models=new ArrayList<>();for(ChemCoreResponseModel m:c.models)if(m!=null&&m.evidenceTier()==ChemCoreDecision.EvidenceTier.M4_HAZROB_VALIDATED&&m.reportableQuantitative())models.add(m);if(models.size()<c.minimumContributors)return null;return new InverseMolecularSolver.Candidate(c.chemicalUid,c.label,models,c.priorLogWeight,c.minimumContributors);}
    private static JSONObject withheld(String session,long uid,String reason){try{return new JSONObject().put("schema",SCHEMA).put("session_id",session==null?"":session).put("chemical_uid",uid).put("valid",false).put("m4_only",true).put("raw_telemetry_preserved_elsewhere",true).put("raw_telemetry_source","telemetry_samples").put("all_valid_quantitative_points_preserved",true).put("reason",reason).put("points",new JSONArray()).put("windows",new JSONArray());}catch(Exception e){return new JSONObject();}}
    private static JSONObject json(String s){try{return s==null||s.isEmpty()?new JSONObject():new JSONObject(s);}catch(Exception e){return new JSONObject();}}
    private static double finite(JSONObject q,String...keys){if(q==null||keys==null)return Double.NaN;for(String k:keys){if(q.isNull(k))continue;double v=q.optDouble(k,Double.NaN);if(Double.isFinite(v))return v;}return Double.NaN;}
    private static void putFinite(JSONObject o,String key,double v)throws Exception{o.put(key,Double.isFinite(v)?v:JSONObject.NULL);}
}
