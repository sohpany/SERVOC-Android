package com.servoc.app.analytics;

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
        Map<Integer,ValidatedGasEngine.Result> out=new HashMap<>();
        if(diag==null)return out;
        double[] ratios=RobotTelemetry.extractRatios(diag);
        // Current ESP32 firmware does not expose a validated MQ-7/MQ-9 heater-cycle flag.
        // Those two channels therefore remain excluded from quantitative target use until
        // a firmware revision explicitly reports a valid heater phase/cycle.
        boolean[] heater={true,true,true,true,false,true,false,true};
        List<ValidatedGasEngine.Result> r=ValidatedGasEngine.analyze(ratios,heater);
        for(ValidatedGasEngine.Result x:r)out.put(x.chemical,x);
        return out;
    }

    /** Compatibility overload for callers that do not have the database-backed catalogue. */
    public static String toJson(JSONObject diag){return toJson(diag,null);}

    /** Stores every actual analytical result together with database identity metadata when available. */
    public static String toJson(JSONObject diag,ChemicalRepository chemicals){
        JSONArray a=new JSONArray();
        for(Map.Entry<Integer,ValidatedGasEngine.Result> e:analyze(diag).entrySet()){
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
                x.put("manufacturer_cross",r.manufacturerCrossContributors);x.put("community_cross",r.communityCrossContributors);x.put("note",r.note);
                x.put("evidence_present",true);
            }catch(Exception ignored){}
            a.put(x);
        }
        JSONObject root=new JSONObject();
        try{
            root.put("results",a);
            root.put("result_count",a.length());
            root.put("result_scope","all evidence-bearing results; no fixed 144-row report cap");
            root.put("precalibrated_installed_mq",ValidatedGasEngine.PRECALIBRATED_INSTALLED_MQ);
            root.put("safety_interpretation_enabled",ValidatedGasEngine.PRECALIBRATED_INSTALLED_MQ);
            root.put("mq7_mq9_heater_validation",false);
        }catch(Exception ignored){}
        return root.toString();
    }
}
