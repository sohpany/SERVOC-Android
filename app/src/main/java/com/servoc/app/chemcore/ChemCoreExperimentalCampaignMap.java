package com.servoc.app.chemcore;

/**
 * Stage-36 experiment-planning map for the fixed 144 panel.
 *
 * The campaign class tells the developer what kind of evidence should be collected next. It does
 * not block exploratory measurements and does not itself promote M3/M4.
 */
public final class ChemCoreExperimentalCampaignMap {
    public enum Campaign {
        TARGET_ANCHOR,
        BROAD_ARRAY_PURE,
        MIXTURE_OR_AGGREGATE,
        COMPATIBILITY_REVIEW,
        HARDWARE_GAP
    }

    private ChemCoreExperimentalCampaignMap() {}

    public static Campaign campaignForCatalogIndex(int index){
        ChemCoreAnalyticalObjectSemantics.Type object=ChemCoreAnalyticalObjectSemantics.typeForCatalogIndex(index);
        if(object==ChemCoreAnalyticalObjectSemantics.Type.MIXTURE || object==ChemCoreAnalyticalObjectSemantics.Type.AGGREGATE_METRIC)
            return Campaign.MIXTURE_OR_AGGREGATE;
        // Experiment planning follows the specialist map directly. This keeps CO in the target-
        // anchor campaign even though its current identity ceiling remains CANDIDATE because the
        // MQ7 heater/selectivity evidence is not yet hardware-complete.
        if(!ChemCoreSpecialistRoleRegistry.primarySensors(index).isEmpty())return Campaign.TARGET_ANCHOR;
        ChemCoreIdentityCeilingMap.Ceiling ceiling=ChemCoreIdentityCeilingMap.ceilingForCatalogIndex(index);
        switch(ceiling){
            case AFFINITY:return Campaign.COMPATIBILITY_REVIEW;
            case UNSUPPORTED:return Campaign.HARDWARE_GAP;
            default:return Campaign.BROAD_ARRAY_PURE;
        }
    }

    /** Minimal early-build guidance; formal M3/M4 promotion requirements remain in the validation protocol. */
    public static String exploratoryGuidance(Campaign c){
        switch(c){
            case TARGET_ANCHOR:return "collect clean-air baseline, target-sensor response, supporting-array response and repeatable recovery across several concentrations; preserve any heater-specific protocol required by the target relation";
            case BROAD_ARRAY_PURE:return "collect multi-sensor static and temporal fingerprints across representative concentrations and likely interferents";
            case MIXTURE_OR_AGGREGATE:return "collect source-pattern/headspace signatures and composition/context metadata; avoid assuming one pure-analyte ppm";
            case COMPATIBILITY_REVIEW:return "first establish safe material/sensor compatibility and whether the installed array has a useful reproducible response";
            default:return "record incidental array response only; prioritize a more appropriate sensor route before identity/ppm validation";
        }
    }
}
