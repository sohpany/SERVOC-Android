package com.servoc.app.report;

import com.servoc.app.chemcore.ChemCoreReportContract;
import com.servoc.app.data.SessionSeries;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Versioned bridge between ChemCore scientific output and all SmartReport renderers.
 *
 * This class does not perform chemical inference. It validates report-facing semantics,
 * records which analytical surfaces are actually available, and prevents PDF/XLSX from
 * silently drifting away from the ChemCore contract.
 */
public final class SmartReportAnalyticalContract {
    public static final String VERSION="HR069-SMARTREPORT-1";
    public static final int SCHEMA_REVISION=1;
    public static final String EXPECTED_CHEMCORE_VERSION="HR069-CC-SR-1";

    public enum Capability {
        RECORDED_TIME_SERIES,
        CANDIDATES,
        SCREENING_144,
        QUANTITATIVE_PPM,
        GAS_SAFETY,
        QUALITY,
        UNCERTAINTY_DOMAIN,
        PROVENANCE,
        MOLECULAR_QSPR,
        MIXTURE_OPEN_SET
    }

    public static final class Availability {
        public final boolean available;
        public final String reason;
        Availability(boolean available,String reason){this.available=available;this.reason=reason;}
    }

    private final SessionSeries.Series series;
    private final JSONObject decision;
    private final Map<Capability,Availability> availability;

    private SmartReportAnalyticalContract(SessionSeries.Series series,JSONObject decision,Map<Capability,Availability> availability){
        this.series=series;
        this.decision=decision;
        this.availability=Collections.unmodifiableMap(new EnumMap<>(availability));
    }

    public static SmartReportAnalyticalContract bind(SessionSeries.Series series,JSONObject decision){
        if(!EXPECTED_CHEMCORE_VERSION.equals(ChemCoreReportContract.VERSION)){
            throw new IllegalStateException("ChemCore/SmartReport contract mismatch: expected "+EXPECTED_CHEMCORE_VERSION+" but ChemCore is "+ChemCoreReportContract.VERSION);
        }
        validateScientificSemantics(decision);
        EnumMap<Capability,Availability> a=new EnumMap<>(Capability.class);
        put(a,Capability.RECORDED_TIME_SERIES,series!=null&&series.usable(),"recorded_time_series_missing");
        put(a,Capability.CANDIDATES,arrayPresent(decision,"candidates"),"chemcore_candidates_missing");
        JSONObject panel=decision==null?null:decision.optJSONObject("screening_144");
        put(a,Capability.SCREENING_144,panel!=null&&panel.optInt("evaluated_count",0)>0,"screening_144_missing");
        boolean ppm=decision!=null&&decision.optBoolean("ppm_reportable",false)&&Double.isFinite(decision.optDouble("ppm",Double.NaN));
        put(a,Capability.QUANTITATIVE_PPM,ppm,"m4_quantitative_gate_not_passed");
        JSONObject gas=decision==null?null:decision.optJSONObject("gas_exposure_ppm");
        put(a,Capability.GAS_SAFETY,gas!=null&&gas.optBoolean("input_gate_passed",false),"gas_safety_gate_not_passed");
        put(a,Capability.QUALITY,decision!=null&&(decision.optJSONObject("measurement_quality")!=null||decision.optJSONObject("lab_quality")!=null),"quality_payload_missing");
        boolean uncertainty=decision!=null&&(decision.has("ood_score")||decision.has("normalized_entropy")||decision.has("prediction_low")||decision.has("prediction_high"));
        put(a,Capability.UNCERTAINTY_DOMAIN,uncertainty,"uncertainty_domain_payload_missing");
        boolean provenance=decision!=null&&(decision.optJSONObject("library")!=null||decision.has("model_version")||decision.has("feature_schema")||decision.has("sampling_profile_id"));
        put(a,Capability.PROVENANCE,provenance,"provenance_payload_missing");
        boolean molecular=decision!=null&&(decision.optJSONObject("molecular")!=null||decision.optJSONObject("qspr")!=null||decision.optJSONObject("molecular_qspr")!=null||decision.optJSONObject("pubchem")!=null);
        put(a,Capability.MOLECULAR_QSPR,molecular,"molecular_qspr_payload_missing");
        String outcome=decision==null?"":decision.optString("outcome","");
        boolean open="UNKNOWN".equals(outcome)||"POSSIBLE_MIXTURE_OR_INTERFERENCE".equals(outcome)||"OUT_OF_VALIDATED_DOMAIN".equals(outcome)||decisionHasMixtureSurface(decision);
        put(a,Capability.MIXTURE_OPEN_SET,open,"mixture_open_set_not_active");
        return new SmartReportAnalyticalContract(series,decision,a);
    }

    public SessionSeries.Series series(){return series;}
    public JSONObject decision(){return decision;}
    public Availability availability(Capability c){return availability.get(c);}
    public Map<Capability,Availability> availability(){return availability;}

    public JSONObject metadata(){
        JSONObject j=new JSONObject();
        try{
            j.put("smartreport_contract_version",VERSION);
            j.put("smartreport_contract_revision",SCHEMA_REVISION);
            j.put("chemcore_report_contract_version",ChemCoreReportContract.VERSION);
            j.put("chemcore_report_contract_revision",ChemCoreReportContract.SCHEMA_REVISION);
            j.put("ranking_semantics",ChemCoreReportContract.RANKING_SEMANTICS);
            JSONObject caps=new JSONObject();
            for(Capability c:Capability.values()){
                Availability q=availability.get(c);
                caps.put(c.name(),new JSONObject().put("available",q!=null&&q.available).put("reason",q==null?"not_evaluated":q.reason));
            }
            j.put("capabilities",caps);
        }catch(Exception e){throw new IllegalStateException(e);}
        return j;
    }

    private static void validateScientificSemantics(JSONObject d){
        if(d==null)return;
        boolean ppm=d.optBoolean("ppm_reportable",false);
        String tier=d.optString("evidence_tier","");
        if(ppm&&!ChemCoreReportContract.REPORTABLE_PPM_TIER.equals(tier)){
            throw new IllegalStateException("reportable ppm requires "+ChemCoreReportContract.REPORTABLE_PPM_TIER+"; found "+tier);
        }
        JSONObject gas=d.optJSONObject("gas_exposure_ppm");
        if(gas!=null&&gas.optBoolean("input_gate_passed",false)&&!ppm){
            throw new IllegalStateException("gas safety quantitative gate cannot pass when ppm_reportable=false");
        }
    }

    private static boolean arrayPresent(JSONObject d,String key){JSONArray a=d==null?null:d.optJSONArray(key);return a!=null&&a.length()>0;}
    private static boolean decisionHasMixtureSurface(JSONObject d){return d!=null&&(d.optJSONObject("mixture")!=null||d.optJSONObject("open_set")!=null||d.optJSONObject("mixture_guard")!=null);}
    private static void put(EnumMap<Capability,Availability> m,Capability c,boolean ok,String missing){m.put(c,new Availability(ok,ok?"available":missing));}
}
