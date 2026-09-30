package com.servoc.app.chemcore;

import android.database.Cursor;
import com.servoc.app.calibration.AcceptedR0Normalizer;
import com.servoc.app.data.ExperimentsDb;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Arrays;

/**
 * Secondary deep-session dynamics contract. It deliberately does not change
 * chemcore-features/1, whose 136-dimensional vector is frozen for current Stable/open-set models.
 * Recovery and cross-sensor dynamics are audit/interpretation evidence until separately validated;
 * they do not add identification-confidence points or unlock reportable PPM by themselves.
 */
public final class ChemCoreSessionDynamics {
    private ChemCoreSessionDynamics(){}
    public static final String SCHEMA="chemcore-session-dynamics/1";
    private static final String[] KEYS=ChemCoreSessionFeatureAdapter.SENSOR_ORDER;

    public static final class Result {
        public final String primarySessionId,recoverySessionId;
        public final int primarySamples,recoverySamples,primaryValidSensors,recoveryValidSensors;
        public final double primaryDurationS,recoveryDurationS;
        public final JSONObject payload;
        Result(String primary,String recovery,int pn,int rn,int pvs,int rvs,double pd,double rd,JSONObject payload){
            this.primarySessionId=primary==null?"":primary;this.recoverySessionId=recovery==null?"":recovery;
            this.primarySamples=pn;this.recoverySamples=rn;this.primaryValidSensors=pvs;this.recoveryValidSensors=rvs;
            this.primaryDurationS=pd;this.recoveryDurationS=rd;this.payload=copy(payload);
        }
        public boolean hasRecovery(){return recoverySamples>=4&&recoveryValidSensors>0;}
        public JSONObject toJson(){return copy(payload);}
    }

    public static Result extract(ExperimentsDb db,String primarySessionId,String recoverySessionId){
        if(db==null)throw new IllegalArgumentException("database required");
        Series p=read(db,primarySessionId),r=read(db,recoverySessionId);
        return compute(primarySessionId,recoverySessionId,p.t,p.x,p.env,r.t,r.x,r.env);
    }

    /** Pure computation entry point used by deterministic unit/performance tests. */
    static Result compute(String primaryId,String recoveryId,double[] pt,double[][] px,double[][] penv,double[] rt,double[][] rx,double[][] renv){
        ChemCoreFeaturePipeline.Result pf=ChemCoreFeaturePipeline.extract(pt,px);
        ChemCoreFeaturePipeline.Result rf=ChemCoreFeaturePipeline.extract(rt,rx);
        int pvs=validSensors(pf),rvs=validSensors(rf);
        JSONObject root=new JSONObject();
        try{
            root.put("schema",SCHEMA);root.put("primary_session_id",safe(primaryId));root.put("recovery_session_id",safe(recoveryId));
            root.put("primary_sample_count",px==null?0:px.length);root.put("recovery_sample_count",rx==null?0:rx.length);
            root.put("primary_duration_s",duration(pt));root.put("recovery_duration_s",duration(rt));
            root.put("primary_valid_sensors",pvs);root.put("recovery_valid_sensors",rvs);
            root.put("feature_schema_compatibility",ChemCoreFeaturePipeline.SCHEMA_VERSION);
            root.put("confidence_semantics","dynamics_audit_only_not_an_automatic_confidence_contributor");
            root.put("quantitation_semantics","dynamics_do_not_unlock_reportable_ppm");

            JSONObject sensors=new JSONObject();
            for(int s=0;s<KEYS.length;s++){
                JSONObject q=new JSONObject();
                put(q,"primary_valid_fraction",feature(pf,s,0));put(q,"baseline_log10_ratio",feature(pf,s,1));
                put(q,"primary_steady_log10_ratio",feature(pf,s,2));put(q,"primary_peak_log10_ratio",feature(pf,s,7));
                put(q,"primary_initial_slope_log10_per_s",feature(pf,s,8));put(q,"primary_t50_s",feature(pf,s,11));put(q,"primary_t90_s",feature(pf,s,12));
                put(q,"primary_abs_auc_log10_s",feature(pf,s,14));
                put(q,"recovery_valid_fraction",feature(rf,s,0));
                Recovery m=recovery(pt,px,rt,rx,s,feature(pf,s,1));
                put(q,"recovery_start_log10_ratio",m.start);put(q,"recovery_end_log10_ratio",m.end);put(q,"recovery_slope_log10_per_s",m.slope);
                put(q,"recovery_t50_s",m.t50);put(q,"recovery_t90_s",m.t90);put(q,"recovery_fraction_to_baseline",m.fraction);
                sensors.put(KEYS[s],q);
            }
            root.put("sensors",sensors);
            root.put("inter_sensor",crossSensor(pt,px,pf));
            root.put("environment",environment(penv,renv));
            JSONObject coverage=new JSONObject();coverage.put("primary_complete",pvs==KEYS.length);coverage.put("recovery_available",rx!=null&&rx.length>=4);
            coverage.put("recovery_complete",rvs==KEYS.length);coverage.put("minimum_primary_sensor_fraction",KEYS.length==0?0:(double)pvs/KEYS.length);
            coverage.put("minimum_recovery_sensor_fraction",KEYS.length==0?0:(double)rvs/KEYS.length);root.put("coverage",coverage);
        }catch(Exception e){throw new IllegalStateException("session dynamics serialization failed",e);}
        return new Result(primaryId,recoveryId,px==null?0:px.length,rx==null?0:rx.length,pvs,rvs,duration(pt),duration(rt),root);
    }

    private static JSONObject crossSensor(double[] t,double[][] x,ChemCoreFeaturePipeline.Result f)throws Exception{
        JSONObject out=new JSONObject(),relative=new JSONObject(),corr=new JSONObject();
        double[] steady=new double[KEYS.length];Arrays.fill(steady,Double.NaN);double[] valid=new double[KEYS.length];int n=0;
        for(int s=0;s<KEYS.length;s++){double y=feature(f,s,2);if(Double.isFinite(y)){steady[s]=Math.pow(10,y);valid[n++]=y;}}
        double center=n==0?Double.NaN:median(Arrays.copyOf(valid,n));
        for(int s=0;s<KEYS.length;s++)if(Double.isFinite(steady[s])&&Double.isFinite(center))put(relative,KEYS[s],Math.log10(steady[s])-center);
        for(int a=0;a<KEYS.length;a++)for(int b=a+1;b<KEYS.length;b++){double c=correlation(x,a,b);if(Double.isFinite(c))put(corr,KEYS[a]+"__"+KEYS[b],c);}
        out.put("steady_log10_relative_to_array_median",relative);out.put("primary_pairwise_log10_correlation",corr);out.put("correlation_min_paired_samples",4);return out;
    }

    private static JSONObject environment(double[][] p,double[][] r)throws Exception{
        JSONObject out=new JSONObject(),primary=envMean(p),recovery=envMean(r),delta=new JSONObject();
        out.put("primary_mean",primary);out.put("recovery_mean",recovery);
        for(String k:new String[]{"temperature_c","rh","pressure_hpa"}){double a=primary.optDouble(k,Double.NaN),b=recovery.optDouble(k,Double.NaN);if(Double.isFinite(a)&&Double.isFinite(b))put(delta,k,b-a);}
        out.put("recovery_minus_primary",delta);return out;
    }
    private static JSONObject envMean(double[][] e)throws Exception{JSONObject q=new JSONObject();if(e==null||e.length==0)return q;for(int j=0;j<3;j++){double sum=0;int n=0;for(double[] row:e)if(row!=null&&j<row.length&&Double.isFinite(row[j])){sum+=row[j];n++;}if(n>0)put(q,j==0?"temperature_c":j==1?"rh":"pressure_hpa",sum/n);}return q;}

    private static Recovery recovery(double[] pt,double[][] px,double[] rt,double[][] rx,int s,double primaryBaseline){
        if(rx==null||rt==null||rx.length<4||rt.length!=rx.length||!Double.isFinite(primaryBaseline))return Recovery.none();
        double[] y=new double[rx.length];Arrays.fill(y,Double.NaN);for(int i=0;i<rx.length;i++){double v=rx[i]!=null&&s<rx[i].length?rx[i][s]:Double.NaN;if(Double.isFinite(v)&&v>0)y[i]=Math.log10(v);}
        int edge=Math.max(2,(int)Math.ceil(y.length*.20));double start=median(y,0,edge),end=median(y,Math.max(0,y.length-edge),y.length);if(!Double.isFinite(start)||!Double.isFinite(end))return Recovery.none();
        double slope=slope(rt,y);double initialDistance=Math.abs(start-primaryBaseline);double finalDistance=Math.abs(end-primaryBaseline);
        double fraction=initialDistance>1e-12?Math.max(0,Math.min(1,1-finalDistance/initialDistance)):Double.NaN;
        double t50=crossRecovery(rt,y,start,primaryBaseline,.50),t90=crossRecovery(rt,y,start,primaryBaseline,.90);
        return new Recovery(start,end,slope,t50,t90,fraction);
    }
    private static double crossRecovery(double[] t,double[] y,double start,double baseline,double fraction){if(!Double.isFinite(start)||!Double.isFinite(baseline)||Math.abs(start-baseline)<1e-12)return Double.NaN;double target=start+(baseline-start)*fraction,t0=t[0];boolean rising=baseline>start;for(int i=1;i<y.length;i++){if(!Double.isFinite(y[i]))continue;if((rising&&y[i]>=target)||(!rising&&y[i]<=target))return Math.max(0,t[i]-t0);}return Double.NaN;}
    private static double slope(double[] t,double[] y){if(t==null||y==null||t.length!=y.length)return Double.NaN;double sx=0,sy=0,sxx=0,sxy=0;int n=0;for(int i=0;i<y.length;i++)if(Double.isFinite(t[i])&&Double.isFinite(y[i])){sx+=t[i];sy+=y[i];sxx+=t[i]*t[i];sxy+=t[i]*y[i];n++;}double d=n*sxx-sx*sx;return n>=2&&Math.abs(d)>1e-15?(n*sxy-sx*sy)/d:Double.NaN;}
    private static double correlation(double[][] x,int a,int b){if(x==null)return Double.NaN;double sa=0,sb=0,saa=0,sbb=0,sab=0;int n=0;for(double[] r:x){if(r==null||a>=r.length||b>=r.length)continue;double va=r[a],vb=r[b];if(!(va>0)||!(vb>0)||!Double.isFinite(va)||!Double.isFinite(vb))continue;va=Math.log10(va);vb=Math.log10(vb);sa+=va;sb+=vb;saa+=va*va;sbb+=vb*vb;sab+=va*vb;n++;}if(n<4)return Double.NaN;double da=n*saa-sa*sa,db=n*sbb-sb*sb;if(da<=1e-15||db<=1e-15)return Double.NaN;return (n*sab-sa*sb)/Math.sqrt(da*db);}
    private static int validSensors(ChemCoreFeaturePipeline.Result f){if(f==null)return 0;int n=0;for(int s=0;s<Math.min(KEYS.length,f.sensorCount);s++)if(Double.isFinite(f.feature(s,0))&&f.feature(s,0)>=.5)n++;return n;}
    private static double feature(ChemCoreFeaturePipeline.Result f,int s,int k){return f==null?Double.NaN:f.feature(s,k);}
    private static double duration(double[] t){return t!=null&&t.length>1&&Double.isFinite(t[0])&&Double.isFinite(t[t.length-1])?Math.max(0,t[t.length-1]-t[0]):0;}
    private static double median(double[] a){return median(a,0,a.length);}private static double median(double[] a,int from,int to){double[] z=new double[Math.max(0,to-from)];int n=0;for(int i=Math.max(0,from);i<Math.min(a.length,to);i++)if(Double.isFinite(a[i]))z[n++]=a[i];if(n==0)return Double.NaN;z=Arrays.copyOf(z,n);Arrays.sort(z);return(n&1)==1?z[n/2]:(z[n/2-1]+z[n/2])/2;}
    private static void put(JSONObject q,String k,double v)throws Exception{if(Double.isFinite(v))q.put(k,v);else q.put(k,JSONObject.NULL);}private static String safe(String s){return s==null?"":s;}
    private static JSONObject copy(JSONObject q){try{return q==null?new JSONObject():new JSONObject(q.toString());}catch(Exception e){return new JSONObject();}}

    private static Series read(ExperimentsDb db,String sessionId){if(sessionId==null||sessionId.trim().isEmpty())return Series.empty();String id=sessionId.trim();java.util.ArrayList<Double> ts=new java.util.ArrayList<>();java.util.ArrayList<double[]> xs=new java.util.ArrayList<>();java.util.ArrayList<double[]> es=new java.util.ArrayList<>();try(Cursor c=db.getReadableDatabase().rawQuery("SELECT t_ms,env_payload,mq_payload FROM telemetry_samples WHERE session_id=? ORDER BY t_ms,id",new String[]{id})){while(c.moveToNext()){ts.add(c.getLong(0)/1000.0);JSONObject env=json(c.getString(1)),mq=json(c.getString(2));double[] x=new double[KEYS.length];Arrays.fill(x,Double.NaN);for(int i=0;i<KEYS.length;i++)x[i]=AcceptedR0Normalizer.ratio(KEYS[i],mq.optJSONObject(KEYS[i]));xs.add(x);double p=finite(env,"pressure_hpa","pressure");if(Double.isFinite(p)&&p>2000)p/=100.0;es.add(new double[]{finite(env,"temperature_c","temperature","temp_c"),finite(env,"humidity_rh","humidity","rh"),p});}}catch(Exception e){throw new IllegalStateException("session dynamics read failed",e);}return Series.of(ts,xs,es);}
    private static JSONObject json(String s){try{return s==null||s.isEmpty()?new JSONObject():new JSONObject(s);}catch(Exception e){return new JSONObject();}}
    private static double finite(JSONObject q,String...keys){if(q==null)return Double.NaN;for(String k:keys){double v=q.optDouble(k,Double.NaN);if(Double.isFinite(v))return v;}return Double.NaN;}
    private static final class Recovery{final double start,end,slope,t50,t90,fraction;Recovery(double a,double b,double c,double d,double e,double f){start=a;end=b;slope=c;t50=d;t90=e;fraction=f;}static Recovery none(){return new Recovery(Double.NaN,Double.NaN,Double.NaN,Double.NaN,Double.NaN,Double.NaN);}}
    private static final class Series{final double[] t;final double[][] x,env;Series(double[] t,double[][] x,double[][] e){this.t=t;this.x=x;this.env=e;}static Series empty(){return new Series(new double[0],new double[0][],new double[0][]);}static Series of(java.util.List<Double> t,java.util.List<double[]> x,java.util.List<double[]> e){double[] a=new double[t.size()];for(int i=0;i<a.length;i++)a[i]=t.get(i);return new Series(a,x.toArray(new double[0][]),e.toArray(new double[0][]));}}
}
