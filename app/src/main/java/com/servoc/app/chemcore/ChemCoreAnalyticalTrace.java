package com.servoc.app.chemcore;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Stage-38 end-to-end analytical trace for reports, developer review and regression fixtures.
 * It composes existing results; it does not create a new classifier or modify ranking.
 */
public final class ChemCoreAnalyticalTrace {
    public static final String VERSION="chemcore-analytical-trace/1";
    public static final String DEVELOPMENT_MODE="EARLY_BUILD_EXPLORATORY";

    private ChemCoreAnalyticalTrace() {}

    public static JSONObject build(ChemCoreResultEnvelope result,
                                   ChemCoreRejectedEvidenceAuditTrail.Result rejected){
        try{
            JSONObject out=new JSONObject();
            out.put("schema",VERSION);
            out.put("development_mode",DEVELOPMENT_MODE);
            out.put("analysis_path",analysisPath());
            out.put("progressive_state",ChemCoreProgressiveResultState.project(result).toJson());
            out.put("result",result==null?JSONObject.NULL:result.toJson());
            out.put("rejected_evidence",rejected==null?new JSONArray():rejected.toJson());

            int index=catalogIndex(result);
            if(index>=0){
                ChemCoreAnalyticalObjectSemantics.Type objectType=ChemCoreAnalyticalObjectSemantics.typeForCatalogIndex(index);
                JSONObject knowledge=new JSONObject();
                knowledge.put("catalog_index",index);
                knowledge.put("object_type",objectType.name());
                knowledge.put("object_interpretation",ChemCoreAnalyticalObjectSemantics.interpretation(index));
                knowledge.put("pure_analyte_ppm_semantics",ChemCoreAnalyticalObjectSemantics.permitsPureAnalytePpm(index));
                knowledge.put("identity_ceiling",ChemCoreIdentityCeilingMap.ceilingForCatalogIndex(index).name());
                knowledge.put("identity_ceiling_is_runtime_gate",ChemCoreIdentityCeilingMap.isRuntimeGate());
                ChemCoreExperimentalCampaignMap.Campaign campaign=ChemCoreExperimentalCampaignMap.campaignForCatalogIndex(index);
                knowledge.put("experimental_campaign",campaign.name());
                knowledge.put("experimental_guidance",ChemCoreExperimentalCampaignMap.exploratoryGuidance(campaign));
                out.put("knowledge_map",knowledge);
            }
            return out;
        }catch(Exception e){throw new IllegalStateException("Unable to build ChemCore analytical trace",e);}
    }

    private static JSONArray analysisPath(){
        JSONArray a=new JSONArray();
        a.put("QUALITY");a.put("SIGNAL");a.put("FAMILY");a.put("SENSOR_ROLES");a.put("CURVES_COMMON_C");
        a.put("FINGERPRINT");a.put("TEMPORAL_QSPR");a.put("OOD_DIAGNOSTIC");a.put("INTERFERENCE_MIXTURE");
        a.put("IDENTITY");a.put("PPM");return a;
    }

    private static int catalogIndex(ChemCoreResultEnvelope r){
        if(r==null||r.chemicalUid==null)return -1;
        long x=r.chemicalUid-1L;return x>=0&&x<144?(int)x:-1;
    }
}
