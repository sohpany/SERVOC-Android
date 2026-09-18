package com.servoc.app.chemcore;

import com.servoc.app.analytics.LibraryStatus;
import com.servoc.app.chemical.ChemicalCatalog;
import com.servoc.app.chemical.ChemicalCoverage;
import com.servoc.app.chemical.MqSensorAffinity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Deterministic full-catalogue screening manifest. It reports support/evidence; it never fabricates detection or ppm. */
public final class ChemCore144ScreeningEngine {
    private ChemCore144ScreeningEngine() {}
    public static final String PANEL_ID="hazrob-operational-144";

    public static JSONObject evaluate() {
        try {
            JSONArray rows=new JSONArray();
            for(int i=0;i<ChemicalCatalog.COUNT;i++){
                ChemicalCoverage.Status coverage=ChemicalCoverage.forChemical(i);
                MqSensorAffinity.Plan plan=MqSensorAffinity.forChemical(i);
                JSONObject r=new JSONObject();
                r.put("index",i);
                r.put("name",ChemicalCatalog.NAMES[i]);
                r.put("formula",ChemicalCatalog.FORMULAS[i]);
                r.put("status",status(coverage,plan));
                r.put("evidence_level",coverage.level);
                r.put("method",coverage.method);
                r.put("sensor_mask",plan.mask);
                r.put("sensors",ChemicalCoverage.sensorNames(plan.mask));
                r.put("reason",coverage.note);
                r.put("detected",false);
                r.put("ppm_reportable",false);
                rows.put(r);
            }
            JSONObject q=new JSONObject();
            q.put("panel_id",PANEL_ID);
            q.put("panel_version",LibraryStatus.CATALOG_VERSION);
            q.put("state","EVALUATED");
            q.put("target_count",ChemicalCatalog.COUNT);
            q.put("evaluated_count",rows.length());
            q.put("rows",rows);
            return q;
        } catch(JSONException e){throw new IllegalStateException("Unable to build 144 screening panel",e);}
    }

    private static String status(ChemicalCoverage.Status c,MqSensorAffinity.Plan p){
        if(p.mask==0)return "UNSUPPORTED_BY_CURRENT_ARRAY";
        if(c.level==ChemicalCoverage.DIRECT_CURVE)return "SUPPORTED";
        if(c.level==ChemicalCoverage.FINGERPRINT_CANDIDATE)return "WEAK_MATCH";
        if(c.level==ChemicalCoverage.AGGREGATE)return "INSUFFICIENT_EVIDENCE";
        return "INSUFFICIENT_EVIDENCE";
    }
}
