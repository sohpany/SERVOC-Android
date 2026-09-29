package com.servoc.app.analytics;

import com.servoc.app.calibration.CalibrationCurveProvider;
import com.servoc.app.chemical.CandidateRanker;
import com.servoc.app.chemical.ChemicalCatalog;
import com.servoc.app.chemical.ChemicalRepository;
import com.servoc.app.chemical.ValidatedGasEngine;
import com.servoc.app.hardware.HazRobHardwareProfile;
import com.servoc.app.sensors.RobotTelemetry;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Converts one ESP32 diagnostic/acquisition snapshot into evidence-weighted chemical results. */
public final class AnalyticsSnapshot {
    private AnalyticsSnapshot(){}
    public static final String ANALYSIS_VERSION="mq-fingerprint-v2-hr042";

    public static Map<Integer,ValidatedGasEngine.Result> analyze(JSONObject diag){return analyze(diag,null,false);}

    public static Map<Integer,ValidatedGasEngine.Result> analyze(JSONObject diag,CalibrationCurveProvider calibration,boolean identityConfirmed){
        Map<Integer,ValidatedGasEngine.Result> out=new HashMap<>();if(diag==null)return out;
        double[] ratios=RobotTelemetry.extractRatios(diag);
        boolean[] heater=manufacturerHeaterValidity();
        List<ValidatedGasEngine.Result> rows=ValidatedGasEngine.analyze(ratios,heater,calibration,identityConfirmed);
        for(ValidatedGasEngine.Result x:rows)out.put(x.chemical,x);return out;
    }

    public static String toJson(JSONObject diag){return toJson(diag,null,null,false);}
    public static String toJson(JSONObject diag,ChemicalRepository chemicals){return toJson(diag,chemicals,null,false);}

    public static String toJson(JSONObject diag,ChemicalRepository chemicals,CalibrationCurveProvider calibration,boolean identityConfirmed){
        Map<Integer,ValidatedGasEngine.Result> map=analyze(diag,calibration,identityConfirmed);
        List<ValidatedGasEngine.Result> rows=new ArrayList<>(map.values());rows.removeIf(r->r==null||r.totalContributors<=0);rows.sort(CandidateRanker.COMPARATOR);JSONArray a=new JSONArray();
        for(ValidatedGasEngine.Result r:rows){
            int chem=r.chemical;JSONObject x=new JSONObject();
            try{
                x.put("rank",a.length()+1);x.put("chemical_id",chem);ChemicalRepository.Chemical c=chemicals==null?null:chemicals.byLegacyId(chem);
                if(c!=null){x.put("chemical_uid",c.uid);if(c.legacyId!=null)x.put("legacy_id",c.legacyId);if(c.cas!=null)x.put("cas",c.cas);if(c.name!=null){x.put("name",c.name);x.put("name_en",c.name);}if(c.arabicName!=null)x.put("name_ar",c.arabicName);if(c.formula!=null)x.put("formula",c.formula);if(c.source!=null)x.put("record_source",c.source);}
                else if(chem>=0&&chem<ChemicalCatalog.COUNT){x.put("name",ChemicalCatalog.NAMES[chem]);x.put("formula",ChemicalCatalog.FORMULAS[chem]);}
                x.put("ppm",r.ppm);x.put("agreement",r.agreement);x.put("agreement_defined",r.agreementContributors>=2);x.put("agreement_contributors",r.agreementContributors);
                x.put("coverage_pct",r.coverage);x.put("evidence_quality_pct",r.evidenceQuality);x.put("candidate_score_pct",r.score);x.put("confidence",r.confidence);x.put("risk",r.risk);
                x.put("quantitative",r.quantitative);x.put("contributors",r.totalContributors);x.put("primary",r.primaryContributors);x.put("calibrated_contributors",r.calibratedContributors);x.put("manufacturer_cross",r.manufacturerCrossContributors);x.put("community_cross",r.communityCrossContributors);x.put("rejected_contributors",r.rejectedContributors);x.put("note",r.note);x.put("evidence_present",true);
            }catch(Exception ignored){}a.put(x);
        }
        AnalysisDecision.Summary decision=AnalysisDecision.evaluate(rows,validChannelCount(diag),calibratedR0Count(diag),environmentAvailable(diag));
        JSONObject root=new JSONObject();try{
            root.put("analysis_version",ANALYSIS_VERSION);root.put("results",a);root.put("result_count",a.length());root.put("result_scope","all evidence-bearing results; ranked deterministically");root.put("candidate_target_count",ChemicalCatalog.COUNT);
            root.put("device_calibration_provider",calibration==null?"none":"active");root.put("identity_confirmed",identityConfirmed);root.put("safety_interpretation_enabled",false);
            root.put("mq7_mq9_heater_validation",HazRobHardwareProfile.manufacturerHeaterProtocolValid("mq7")&&HazRobHardwareProfile.manufacturerHeaterProtocolValid("mq9"));
            root.put("manufacturer_heater_protocol",manufacturerHeaterProtocolJson());
            root.put("quality",qualityJson(decision));
        }catch(Exception ignored){}return root.toString();
    }

    public static AnalysisDecision.Summary decision(JSONObject diag,CalibrationCurveProvider calibration,boolean identityConfirmed){
        Map<Integer,ValidatedGasEngine.Result> map=analyze(diag,calibration,identityConfirmed);
        return AnalysisDecision.evaluate(new ArrayList<>(map.values()),validChannelCount(diag),calibratedR0Count(diag),environmentAvailable(diag));
    }

    static boolean[] manufacturerHeaterValidity(){
        boolean[] out=new boolean[RobotTelemetry.MQ_KEYS.length];
        for(int i=0;i<out.length;i++)out[i]=HazRobHardwareProfile.manufacturerHeaterProtocolValid(RobotTelemetry.MQ_KEYS[i]);
        return out;
    }

    private static JSONObject manufacturerHeaterProtocolJson(){
        JSONObject o=new JSONObject();JSONArray valid=new JSONArray(),invalid=new JSONArray();
        try{
            o.put("authority",HazRobHardwareProfile.ID);
            o.put("cyclic_driver_installed",HazRobHardwareProfile.CYCLIC_MQ_HEATER_DRIVER_INSTALLED);
            for(String key:RobotTelemetry.MQ_KEYS){
                if(HazRobHardwareProfile.manufacturerHeaterProtocolValid(key))valid.put(key);else invalid.put(key);
            }
            o.put("valid_channels",valid);o.put("invalid_channels",invalid);
            o.put("note",HazRobHardwareProfile.CYCLIC_MQ_HEATER_DRIVER_INSTALLED
                    ?"Installed hardware declares manufacturer cyclic-heater capability; physical qualification still governs use."
                    :"HR069 common 5 V MQ rail cannot execute the MQ-7/MQ-9 5.0 V/1.5 V manufacturer heater cycle; those channels are withheld from heater-qualified inference.");
        }catch(Exception ignored){}
        return o;
    }

    private static JSONObject qualityJson(AnalysisDecision.Summary d){JSONObject q=new JSONObject();try{q.put("outcome",d.outcome.name());q.put("valid_channels",d.validChannels);q.put("calibrated_r0_channels",d.calibratedR0Channels);q.put("environment_available",d.environmentAvailable);q.put("quantitative_permitted",d.quantitativePermitted);q.put("top_score_pct",d.topScore);q.put("runner_up_score_pct",d.runnerUpScore);q.put("separation_margin_pct",d.separationMargin);q.put("top_chemical_id",d.topChemical>=0?d.topChemical:JSONObject.NULL);q.put("top_contributors",d.topContributors);q.put("note",d.note);}catch(Exception ignored){}return q;}
    private static int validChannelCount(JSONObject d){double[] r=RobotTelemetry.extractRatios(d);int n=0;for(double v:r)if(Double.isFinite(v)&&v>0)n++;return n;}
    private static int calibratedR0Count(JSONObject d){JSONObject mq=d==null?null:d.optJSONObject("mq");if(mq==null)return 0;int n=0;for(String k:RobotTelemetry.MQ_KEYS){JSONObject q=mq.optJSONObject(k);if(q!=null&&q.optBoolean("r0_calibrated",false))n++;}return n;}
    private static boolean environmentAvailable(JSONObject d){JSONObject e=d==null?null:d.optJSONObject("env");return e!=null&&e.optBoolean("ok",false);}
}
