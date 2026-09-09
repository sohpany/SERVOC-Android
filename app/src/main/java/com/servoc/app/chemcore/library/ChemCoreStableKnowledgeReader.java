package com.servoc.app.chemcore.library;

import com.servoc.app.chemcore.ChemCoreDecision;
import com.servoc.app.chemcore.ChemCoreFeaturePipeline;
import com.servoc.app.chemcore.ChemCoreOpenSetGate;
import com.servoc.app.chemcore.ChemCoreResponseModel;
import com.servoc.app.chemcore.InverseMolecularSolver;
import com.servoc.app.chemcore.MahalanobisModel;
import com.servoc.app.chemcore.SamplingProfile;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Read-only adapter from a verified ChemCore Stable JSON payload into runtime analytical objects.
 *
 * Stable knowledge can update without an APK, but it cannot bypass evidence gates. In particular:
 * - a response-prior record is M1/M2 unless explicit device-validation metadata supports M4;
 * - an open-set model is ignored unless it declares independent known/unknown validation;
 * - sampling-profile compatibility is enforced at prediction/evaluation time.
 */
public final class ChemCoreStableKnowledgeReader {
    public static final String SNAPSHOT_SCHEMA="chemcore-stable-snapshot-v1";
    private ChemCoreStableKnowledgeReader(){}

    public static final class Snapshot {
        public final boolean available;
        public final String schema,sourceCommit,note;
        public final JSONObject root;
        Snapshot(boolean available,String schema,String sourceCommit,String note,JSONObject root){
            this.available=available;this.schema=schema==null?"":schema;this.sourceCommit=sourceCommit==null?"":sourceCommit;
            this.note=note==null?"":note;this.root=root;
        }
        public JSONObject knowledge(){return root==null?null:root.optJSONObject("knowledge");}
    }

    public static final class OpenSetArtifact {
        public final ChemCoreOpenSetGate gate;
        public final String modelVersion,validationProtocol;
        public OpenSetArtifact(ChemCoreOpenSetGate gate,String modelVersion,String validationProtocol){
            this.gate=gate;this.modelVersion=modelVersion==null?"":modelVersion;this.validationProtocol=validationProtocol==null?"":validationProtocol;
        }
    }

    public static Snapshot read(File payload){
        if(payload==null||!payload.isFile())return new Snapshot(false,"","","no active Stable payload",null);
        try(FileInputStream in=new FileInputStream(payload)){
            JSONObject root=new JSONObject(readAll(in));String schema=root.optString("schema","");
            if(!SNAPSHOT_SCHEMA.equals(schema))
                return new Snapshot(false,schema,root.optString("source_commit_sha",""),"active payload is not canonical Stable snapshot v1",root);
            return new Snapshot(true,schema,root.optString("source_commit_sha",""),"canonical Stable snapshot",root);
        }catch(Exception e){return new Snapshot(false,"","","Stable snapshot read failed: "+e.getClass().getSimpleName(),null);}
    }

    /** Returns forward response candidates published in the active Stable payload. */
    public static List<InverseMolecularSolver.Candidate> candidates(File payload,SamplingProfile profile){
        Snapshot s=read(payload);if(!s.available)return List.of();JSONObject k=s.knowledge();if(k==null)return List.of();
        JSONArray priors=asArray(k.opt("response_priors"));if(priors==null||priors.length()==0)return List.of();
        Map<Long,List<ChemCoreResponseModel>> models=new LinkedHashMap<>();Map<Long,String> labels=new LinkedHashMap<>();Map<Long,Double> priorWeights=new LinkedHashMap<>();Map<Long,Integer> minimums=new LinkedHashMap<>();
        for(int i=0;i<priors.length();i++){
            JSONObject r=priors.optJSONObject(i);if(r==null)continue;
            try{
                long uid=r.optLong("chemical_uid",-1);if(uid<=0)continue;
                StableLogPowerModel m=StableLogPowerModel.from(r,profile==null?"":profile.profileId);if(m==null)continue;
                models.computeIfAbsent(uid,x->new ArrayList<>()).add(m);
                labels.putIfAbsent(uid,r.optString("label","Chemical "+uid));
                double pw=r.optDouble("prior_log_weight",0);if(Double.isFinite(pw))priorWeights.put(uid,pw);
                minimums.put(uid,Math.max(1,r.optInt("minimum_contributors",2)));
            }catch(Exception ignored){}
        }
        List<InverseMolecularSolver.Candidate> out=new ArrayList<>();
        for(Map.Entry<Long,List<ChemCoreResponseModel>> e:models.entrySet()){
            long uid=e.getKey();out.add(new InverseMolecularSolver.Candidate(uid,labels.get(uid),e.getValue(),priorWeights.getOrDefault(uid,0.0),minimums.getOrDefault(uid,2)));
        }
        return out;
    }

    /**
     * Restores a validated open-set gate only when the artifact explicitly records independent
     * known/unknown threshold selection and matches the current feature/sampling contracts.
     */
    public static OpenSetArtifact openSetArtifact(File payload,String samplingProfileId){
        Snapshot s=read(payload);if(!s.available)return null;JSONObject k=s.knowledge();if(k==null)return null;
        JSONArray artifacts=asArray(k.opt("model_artifacts"));if(artifacts==null)return null;
        for(int i=0;i<artifacts.length();i++){
            JSONObject a=artifacts.optJSONObject(i);if(a==null)continue;
            if(!"MAHALANOBIS_OPEN_SET_V1".equalsIgnoreCase(a.optString("type","")))continue;
            if(!a.optBoolean("validated",false))continue;
            if(!ChemCoreFeaturePipeline.SCHEMA_VERSION.equals(a.optString("feature_schema_version","")))continue;
            if(!profileCompatible(a.optJSONArray("sampling_profiles"),samplingProfileId))continue;
            JSONObject v=a.optJSONObject("validation");if(v==null)continue;
            if(!"INDEPENDENT_KNOWN_UNKNOWN".equalsIgnoreCase(v.optString("threshold_source","")))continue;
            String protocol=v.optString("validation_protocol","").trim();if(protocol.isEmpty())continue;
            try{
                double[] mean=array1(a.getJSONArray("mean"));double[][] inv=array2(a.getJSONArray("inverse_covariance"),mean.length);
                double threshold=a.optDouble("threshold",Double.NaN),coverage=a.optDouble("minimum_coverage",0.75),ridge=a.optDouble("ridge",0);
                if(!(threshold>0)||!Double.isFinite(threshold))continue;
                MahalanobisModel model=MahalanobisModel.fromFrozenParameters(mean,inv,ridge,a.optInt("training_count",0));
                ChemCoreOpenSetGate gate=ChemCoreOpenSetGate.fromValidatedThreshold(model,threshold,coverage);
                return new OpenSetArtifact(gate,a.optString("model_version","stable-open-set"),protocol);
            }catch(Exception ignored){}
        }
        return null;
    }

    private static final class StableLogPowerModel implements ChemCoreResponseModel {
        final String sensor,version,profileId,calibrationId;
        final ChemCoreDecision.EvidenceTier tier;
        final double intercept,slope,sigma,minPpm,maxPpm,tempCoeff,rhCoeff,refTemp,refRh,minTemp,maxTemp,minRh,maxRh;
        final List<String> profiles;

        StableLogPowerModel(String sensor,String version,String profileId,String calibrationId,ChemCoreDecision.EvidenceTier tier,
                            double intercept,double slope,double sigma,double minPpm,double maxPpm,double tempCoeff,double rhCoeff,
                            double refTemp,double refRh,double minTemp,double maxTemp,double minRh,double maxRh,List<String> profiles){
            this.sensor=sensor;this.version=version;this.profileId=profileId;this.calibrationId=calibrationId;this.tier=tier;
            this.intercept=intercept;this.slope=slope;this.sigma=sigma;this.minPpm=minPpm;this.maxPpm=maxPpm;
            this.tempCoeff=tempCoeff;this.rhCoeff=rhCoeff;this.refTemp=refTemp;this.refRh=refRh;this.minTemp=minTemp;this.maxTemp=maxTemp;this.minRh=minRh;this.maxRh=maxRh;this.profiles=profiles;
        }

        static StableLogPowerModel from(JSONObject r,String activeProfile){
            String form=r.optString("form","LOG10_LINEAR");if(!"LOG10_LINEAR".equalsIgnoreCase(form))return null;
            String sensor=r.optString("sensor_key","").trim().toLowerCase(Locale.US);if(!sensor.matches("mq(2|3|4|5|7|8|9|135)"))return null;
            double a=r.optDouble("intercept",Double.NaN),b=r.optDouble("slope",Double.NaN),sig=r.optDouble("sigma_log10",Double.NaN),lo=r.optDouble("min_ppm",Double.NaN),hi=r.optDouble("max_ppm",Double.NaN);
            if(!Double.isFinite(a)||!Double.isFinite(b)||Math.abs(b)<1e-12||!(lo>0)||!(hi>lo))return null;
            if(!Double.isFinite(sig)||sig<=0)sig=.20;sig=Math.max(.02,sig);
            ChemCoreDecision.EvidenceTier tier=parseTier(r.optString("evidence_tier","M1"));
            String cal=r.optString("calibration_id","").trim();boolean device=r.optBoolean("device_validated",false);String validation=r.optString("validation_protocol","").trim();
            if(tier==ChemCoreDecision.EvidenceTier.M4_HAZROB_VALIDATED&&(!device||cal.isEmpty()||validation.isEmpty()))tier=ChemCoreDecision.EvidenceTier.M2_LITERATURE_OR_MANUFACTURER_SUPPORTED;
            List<String> profiles=strings(r.optJSONArray("sampling_profiles"));
            if(tier==ChemCoreDecision.EvidenceTier.M4_HAZROB_VALIDATED&&(activeProfile==null||activeProfile.isEmpty()||!profiles.contains(activeProfile)))return null;
            return new StableLogPowerModel(sensor,r.optString("model_version","stable-prior"),activeProfile,cal,tier,a,b,sig,lo,hi,
                    finiteOr(r,"temperature_coefficient",0),finiteOr(r,"rh_coefficient",0),finiteOr(r,"reference_temperature_c",25),finiteOr(r,"reference_rh",50),
                    finiteOr(r,"min_temperature_c",Double.NEGATIVE_INFINITY),finiteOr(r,"max_temperature_c",Double.POSITIVE_INFINITY),
                    finiteOr(r,"min_rh",Double.NEGATIVE_INFINITY),finiteOr(r,"max_rh",Double.POSITIVE_INFINITY),profiles);
        }

        @Override public String sensorKey(){return sensor;}@Override public String version(){return version;}@Override public ChemCoreDecision.EvidenceTier evidenceTier(){return tier;}@Override public double minPpm(){return minPpm;}@Override public double maxPpm(){return maxPpm;}
        @Override public Estimate predict(double ppm,double temperatureC,double rh,SamplingProfile samplingProfile){
            if(!(ppm>0)||ppm<minPpm||ppm>maxPpm)return new Estimate(false,Double.NaN,sigma,false,"ppm outside published model domain");
            boolean inside=true;if(!profiles.isEmpty()){String p=samplingProfile==null?"":samplingProfile.profileId;inside=profiles.contains(p);}if(Double.isFinite(temperatureC)&&(temperatureC<minTemp||temperatureC>maxTemp))inside=false;if(Double.isFinite(rh)&&(rh<minRh||rh>maxRh))inside=false;
            double y=intercept+slope*Math.log10(ppm);if(Double.isFinite(temperatureC))y+=tempCoeff*(temperatureC-refTemp);if(Double.isFinite(rh))y+=rhCoeff*(rh-refRh);
            return new Estimate(true,y,sigma,inside,inside?"inside Stable response-model domain":"outside Stable response-model environmental/sampling domain");
        }
    }

    private static ChemCoreDecision.EvidenceTier parseTier(String raw){
        String s=raw==null?"":raw.trim().toUpperCase(Locale.US);
        if(s.startsWith("M4"))return ChemCoreDecision.EvidenceTier.M4_HAZROB_VALIDATED;
        if(s.startsWith("M3"))return ChemCoreDecision.EvidenceTier.M3_ARRAY_CONSISTENT;
        if(s.startsWith("M2"))return ChemCoreDecision.EvidenceTier.M2_LITERATURE_OR_MANUFACTURER_SUPPORTED;
        if(s.startsWith("M1"))return ChemCoreDecision.EvidenceTier.M1_MOLECULARLY_INFERRED;
        return ChemCoreDecision.EvidenceTier.M0_CATALOG_ONLY;
    }
    private static JSONArray asArray(Object value){if(value instanceof JSONArray)return (JSONArray)value;if(value instanceof JSONObject){JSONObject o=(JSONObject)value;for(String k:new String[]{"records","items","priors","artifacts","models"}){JSONArray a=o.optJSONArray(k);if(a!=null)return a;}}return null;}
    private static List<String> strings(JSONArray a){List<String> out=new ArrayList<>();if(a!=null)for(int i=0;i<a.length();i++){String s=a.optString(i,"").trim();if(!s.isEmpty()&&!out.contains(s))out.add(s);}return out;}
    private static boolean profileCompatible(JSONArray a,String profile){List<String> x=strings(a);return x.isEmpty()||(profile!=null&&x.contains(profile));}
    private static double finiteOr(JSONObject r,String key,double fallback){double v=r.optDouble(key,Double.NaN);return Double.isFinite(v)?v:fallback;}
    private static double[] array1(JSONArray a){double[] x=new double[a.length()];for(int i=0;i<x.length;i++){x[i]=a.getDouble(i);if(!Double.isFinite(x[i]))throw new IllegalArgumentException("non-finite vector");}return x;}
    private static double[][] array2(JSONArray a,int n){if(a.length()!=n)throw new IllegalArgumentException("matrix dimension");double[][] x=new double[n][n];for(int i=0;i<n;i++){JSONArray r=a.getJSONArray(i);if(r.length()!=n)throw new IllegalArgumentException("matrix row dimension");for(int j=0;j<n;j++){x[i][j]=r.getDouble(j);if(!Double.isFinite(x[i][j]))throw new IllegalArgumentException("non-finite matrix");}}return x;}
    private static String readAll(FileInputStream in)throws Exception{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[16384];for(int n;(n=in.read(b))>=0;)if(n>0)out.write(b,0,n);return out.toString(StandardCharsets.UTF_8.name());}
}
