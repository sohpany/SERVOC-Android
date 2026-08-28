package com.servoc.app.analytics;

import com.servoc.app.chemical.ChemicalCatalog;
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

    public static String toJson(JSONObject diag){
        JSONArray a=new JSONArray();
        for(Map.Entry<Integer,ValidatedGasEngine.Result> e:analyze(diag).entrySet()){
            int chem=e.getKey();ValidatedGasEngine.Result r=e.getValue();
            JSONObject x=new JSONObject();
            try{
                x.put("chemical_id",chem);x.put("name",ChemicalCatalog.NAMES[chem]);x.put("formula",ChemicalCatalog.FORMULAS[chem]);
                x.put("ppm",r.ppm);x.put("agreement",r.agreement);x.put("confidence",r.confidence);x.put("risk",r.risk);
                x.put("quantitative",r.quantitative);x.put("contributors",r.totalContributors);x.put("primary",r.primaryContributors);
                x.put("manufacturer_cross",r.manufacturerCrossContributors);x.put("community_cross",r.communityCrossContributors);x.put("note",r.note);
            }catch(Exception ignored){}
            a.put(x);
        }
        JSONObject root=new JSONObject();
        try{
            root.put("results",a);
            root.put("precalibrated_installed_mq",ValidatedGasEngine.PRECALIBRATED_INSTALLED_MQ);
            root.put("safety_interpretation_enabled",ValidatedGasEngine.PRECALIBRATED_INSTALLED_MQ);
            root.put("mq7_mq9_heater_validation",false);
        }catch(Exception ignored){}
        return root.toString();
    }
}
