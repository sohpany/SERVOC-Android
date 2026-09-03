package com.servoc.app.analytics;

import com.servoc.app.calibration.CalibrationCurveProvider;
import com.servoc.app.chemical.ChemicalCatalog;
import com.servoc.app.chemical.ChemicalRepository;
import com.servoc.app.chemical.ValidatedGasEngine;
import com.servoc.app.sensors.RobotTelemetry;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Converts one ESP32 diagnostic/acquisition snapshot into evidence-weighted chemical results. */
public final class AnalyticsSnapshot {
    private AnalyticsSnapshot(){}

    public static Map<Integer,ValidatedGasEngine.Result> analyze(JSONObject diag){
        return analyze(diag,null,false);
    }

    /**
     * Analysis using the active device calibration when available. Generic manufacturer/research
     * curves remain secondary fingerprint evidence inside ValidatedGasEngine.
     */
    public static Map<Integer,ValidatedGasEngine.Result> analyze(JSONObject diag,CalibrationCurveProvider calibration,boolean identityConfirmed){
        Map<Integer,ValidatedGasEngine.Result> out=new HashMap<>();
        if(diag==null)return out;
        double[] ratios=RobotTelemetry.extractRatios(diag);
        // MQ-7/MQ-9 remain excluded from quantitative target use until firmware reports a
        // validated heater phase/cycle. Their responses may still contribute fingerprint evidence.
        boolean[] heater={true,true,true,true,false,true,false,true};
        List<ValidatedGasEngine.Result> r=ValidatedGasEngine.analyze(ratios,heater,calibration,identityConfirmed);
        for(ValidatedGasEngine.Result x:r)out.put(x.chemical,x);
        return out;
    }

    /** Compatibility overload for callers that do not have the database-backed catalogue/calibration provider. */
    public static String toJson(JSONObject diag){return toJson(diag,null,null,false);}

    public static String toJson(JSONObject diag,ChemicalRepository chemicals){return toJson(diag,chemicals,null,false);}

    /** Stores every actual analytical result together with database identity and calibration provenance. */
    public static String toJson(JSONObject diag,ChemicalRepository chemicals,CalibrationCurveProvider calibration,boolean identityConfirmed){
        JSONArray a=new JSONArray();
        for(Map.Entry<Integer,ValidatedGasEngine.Result> e:analyze(diag,calibration,identityConfirmed).entrySet()){
            int chem=e.getKey();ValidatedGasEngine.Result r=e.getValue();
            if(r==null||r.totalContributors<=0)continue;
            JSONObject x=new JSONObject();
            try{
                x.put("chemical_id",chem);
                ChemicalRepository.Chemical c=chemicals==null?null:chemicals.byLegacyId(chem);
                if(c!=null){
                    x.put("chemical_uid",c.uid);
                    if(c.legacyId!=null)x.put("legacy_id",c.legacyId);
                    if(c.cas!=null)x.put("cas",c.cas);
                    if(c.name!=null)x.put("name",c.name);
                    if(c.name!=null)x.put("name_en",c.name);
                    if(c.arabicName!=null)x.put("name_ar",c.arabicName);
                    if(c.formula!=null)x.put("formula",c.formula);
                    if(c.source!=null)x.put("record_source",c.source);
                }else if(chem>=0&&chem<ChemicalCatalog.COUNT){
                    x.put("name",ChemicalCatalog.NAMES[chem]);
                    x.put("formula",ChemicalCatalog.FORMULAS[chem]);
                }
                x.put("ppm",r.ppm);x.put("agreement",r.agreement);x.put("confidence",r.confidence);x.put("risk",r.risk);
                x.put("quantitative",r.quantitative);x.put("contributors",r.totalContributors);x.put("primary",r.primaryContributors);
                x.put("calibrated_contributors",r.calibratedContributors);
                x.put("manufacturer_cross",r.manufacturerCrossContributors);x.put("community_cross",r.communityCrossContributors);x.put("note",r.note);
                x.put("evidence_present",true);
            }catch(Exception ignored){}
            a.put(x);
        }
        JSONObject root=new JSONObject();
        try{
            root.put("results",a);
            root.put("result_count",a.length());
            root.put("result_scope","all evidence-bearing results; no fixed row cap");
            root.put("device_calibration_provider",calibration==null?"none":"active");
            root.put("identity_confirmed",identityConfirmed);
            root.put("safety_interpretation_enabled",false);
            root.put("mq7_mq9_heater_validation",false);
        }catch(Exception ignored){}
        return root.toString();
    }
}
