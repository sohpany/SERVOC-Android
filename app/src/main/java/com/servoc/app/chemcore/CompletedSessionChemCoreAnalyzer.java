package com.servoc.app.chemcore;

import com.servoc.app.calibration.AcceptedR0Normalizer;
import com.servoc.app.data.ExperimentsDb;
import com.servoc.app.hardware.HazRobHardwareProfile;

import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Converts one completed ordinary HR069 measurement into a persisted ChemCore screening decision.
 *
 * This bridge deliberately keeps candidate ranking separate from identity acceptance. A normal
 * field measurement may produce ranked screening hypotheses, but it has no explicit open-set/OOD
 * acceptance and no M4 reference-gas validation. Therefore this class never manufactures a named
 * identity or reportable ppm merely because the MQ array responded strongly.
 */
public final class CompletedSessionChemCoreAnalyzer {
    public static final String MODEL_VERSION="chemcore-completed-session-screening/1";
    private CompletedSessionChemCoreAnalyzer(){}

    public static final class Outcome {
        public final long decisionRowId;
        public final int observedChannels,candidateCount;
        public final ChemCoreResultEnvelope result;
        Outcome(long row,int observed,int candidates,ChemCoreResultEnvelope result){
            this.decisionRowId=row;this.observedChannels=observed;this.candidateCount=candidates;this.result=result;
        }
        public boolean persisted(){return decisionRowId>=0;}
    }

    /** Analyze and persist a completed firmware measurement session. */
    public static Outcome analyzeAndPersist(ExperimentsDb db,String sessionId,JSONObject terminalSnapshot,long tMs){
        if(db==null||sessionId==null||sessionId.trim().isEmpty()||terminalSnapshot==null)
            return new Outcome(-1,0,0,null);
        String id=sessionId.trim();
        ChemCoreDecisionRepository decisions=new ChemCoreDecisionRepository(db);
        JSONObject existing=decisions.latestPayloadForSession(id);
        if(existing!=null){
            int count=existing.optInt("candidate_count",existing.optJSONArray("candidates")==null?0:existing.optJSONArray("candidates").length());
            return new Outcome(-2,0,count,null);
        }

        Map<String,Double> ratios=observedRatios(terminalSnapshot);
        if(ratios.isEmpty())return new Outcome(-1,0,0,null);

        ChemCoreSessionFeatureAdapter.Extraction extraction=null;
        try{extraction=ChemCoreSessionFeatureAdapter.extract(db,id);}catch(Exception ignored){}

        JSONObject env=terminalSnapshot.optJSONObject("env");
        double temperature=env==null?Double.NaN:finite(env,"temperature","temperature_c","temp_c");
        double rh=env==null?Double.NaN:finite(env,"humidity","humidity_rh","rh");
        if(extraction!=null){
            if(!Double.isFinite(temperature)&&Double.isFinite(extraction.meanTemperatureC))temperature=extraction.meanTemperatureC;
            if(!Double.isFinite(rh)&&Double.isFinite(extraction.meanRh))rh=extraction.meanRh;
        }

        SamplingProfile profile=SamplingProfile.hazRobStandardChamberV2();
        ChemCoreInferenceEngine.Context context=new ChemCoreInferenceEngine.Context(
                id,profile.profileId,MODEL_VERSION,temperature,rh,null,false);
        List<InverseMolecularSolver.Candidate> library=ChemCoreLegacyCandidateRegistry.acceptedReferenceCandidates();
        ChemCoreResultEnvelope result=ChemCoreInferenceEngine.analyze(ratios,library,profile,context);

        try{
            JSONObject quality=new JSONObject();quality.put("state","FIELD_SCREENING");
            quality.put("valid_mq_channels",ratios.size());quality.put("r0_normalized_channels",ratios.size());
            quality.put("reference_validated_channels",0);quality.put("m4_quantitative_gate",false);
            quality.put("candidate_ranking_permitted",true);quality.put("named_identity_requires_open_set_acceptance",true);
            result=result.withLabQuality(quality);
        }catch(Exception ignored){}

        long row=decisions.save(Math.max(0,tMs),result);
        return new Outcome(row,ratios.size(),result==null?0:result.candidates.size(),result);
    }

    /**
     * Prefer the completed firmware average; fall back channel-by-channel to the current live array.
     * Only accepted-R0 normalized, valid ratios enter the primary solver.
     */
    public static Map<String,Double> observedRatios(JSONObject snapshot){
        Map<String,Double> out=new LinkedHashMap<>();if(snapshot==null)return out;
        JSONObject measurement=snapshot.optJSONObject("measurement");
        JSONObject averaged=measurement==null?null:measurement.optJSONObject("mq");
        JSONObject live=snapshot.optJSONObject("mq");
        for(HazRobHardwareProfile.Channel hw:HazRobHardwareProfile.MQ){
            JSONObject q=averaged==null?null:averaged.optJSONObject(hw.key);
            double ratio=AcceptedR0Normalizer.ratio(hw.key,q);
            if(!finitePositive(ratio)){
                q=live==null?null:live.optJSONObject(hw.key);ratio=AcceptedR0Normalizer.ratio(hw.key,q);
            }
            if(finitePositive(ratio))out.put(hw.key,ratio);
        }
        return out;
    }

    private static double finite(JSONObject o,String...keys){
        if(o==null)return Double.NaN;for(String key:keys){if(o.isNull(key))continue;double v=o.optDouble(key,Double.NaN);if(Double.isFinite(v))return v;}return Double.NaN;
    }
    private static boolean finitePositive(double v){return Double.isFinite(v)&&v>0;}
}
