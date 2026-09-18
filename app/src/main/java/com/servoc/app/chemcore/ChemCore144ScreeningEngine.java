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

    public static JSONObject evaluate() { return evaluate(null); }

    public static JSONObject evaluate(ChemCoreResultEnvelope result) {
        try {
            JSONArray rows=new JSONArray();
            for(int i=0;i<ChemicalCatalog.COUNT;i++){
                ChemicalCoverage.Status coverage=ChemicalCoverage.forChemical(i);
                MqSensorAffinity.Plan plan=MqSensorAffinity.forChemical(i);
                JSONObject r=new JSONObject();
                r.put("index",i);
                r.put("name",ChemicalCatalog.NAMES[i]);
                r.put("formula",ChemicalCatalog.FORMULAS[i]);
                r.put("status",status(i,coverage,plan,result));
                r.put("evidence_level",coverage.level);
                r.put("method",coverage.method);
                r.put("sensor_mask",plan.mask);
                r.put("sensors",ChemicalCoverage.sensorNames(plan.mask));
                r.put("reason",coverage.note);
                boolean accepted=matchesAcceptedChemical(i,result)&&ChemCoreDecision.permitsNamedIdentity(result.outcome);
                r.put("detected",accepted);
                r.put("ppm_reportable",accepted&&ChemCoreDecision.permitsQuantitativeOccupationalInterpretation(result.outcome,result.evidenceTier)&&Double.isFinite(result.ppm));
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

    private static boolean matchesAcceptedChemical(int index,ChemCoreResultEnvelope result){\n        if(result==null||result.chemicalUid==null)return false;\n        // chemical_uid is a stable database UID, not a 0..143 catalogue index. Match the operational\n        // panel by canonical label; never equate UID numerically with the legacy catalogue position.\n        String label=result.chemicalLabel==null?"":result.chemicalLabel.trim();\n        return !label.isEmpty()&&label.equalsIgnoreCase(ChemicalCatalog.NAMES[index]);\n    }\n\n    private static String status(int index,ChemicalCoverage.Status c,MqSensorAffinity.Plan p,ChemCoreResultEnvelope result){
        if(matchesAcceptedChemical(index,result)){
            if(result.outcome==ChemCoreDecision.Outcome.OUT_OF_VALIDATED_DOMAIN)return "OUT_OF_DOMAIN";
            if(result.outcome==ChemCoreDecision.Outcome.UNKNOWN||result.outcome==ChemCoreDecision.Outcome.POSSIBLE_MIXTURE_OR_INTERFERENCE||result.outcome==ChemCoreDecision.Outcome.INSUFFICIENT_EVIDENCE)return "INSUFFICIENT_EVIDENCE";
            if(ChemCoreDecision.permitsNamedIdentity(result.outcome))return "SUPPORTED";
        }
        if(p.mask==0)return "UNSUPPORTED_BY_CURRENT_ARRAY";
        if(c.level==ChemicalCoverage.DIRECT_CURVE)return "SUPPORTED";
        if(c.level==ChemicalCoverage.FINGERPRINT_CANDIDATE)return "WEAK_MATCH";
        if(c.level==ChemicalCoverage.AGGREGATE)return "INSUFFICIENT_EVIDENCE";
        return "INSUFFICIENT_EVIDENCE";
    }
}
