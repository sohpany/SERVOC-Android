package com.servoc.app.chemcore;

/**
 * Runtime semantics for the Stage-4 analytical object taxonomy of the fixed 144-item panel.
 *
 * This class deliberately stays small: it only prevents operational mixtures/aggregate metrics
 * from inheriting pure-molecule identity and ppm semantics. It does not attempt mixture
 * decomposition or composition estimation.
 */
public final class ChemCoreAnalyticalObjectSemantics {
    public enum Type { PURE_ANALYTE, MIXTURE, AGGREGATE_METRIC, SOURCE_MATERIAL_HEADSPACE }

    private ChemCoreAnalyticalObjectSemantics() {}

    public static Type typeForCatalogIndex(int index) {
        if (index >= 135 && index <= 139) return Type.MIXTURE;
        if (index >= 140 && index <= 142) return Type.AGGREGATE_METRIC;
        if (index == 143) return Type.MIXTURE;
        return Type.PURE_ANALYTE;
    }

    public static boolean permitsPureAnalyteIdentity(int index) {
        return typeForCatalogIndex(index) == Type.PURE_ANALYTE;
    }

    /** Pure-analyte ppm semantics are not assigned to mixtures or operational aggregate metrics. */
    public static boolean permitsPureAnalytePpm(int index) {
        return typeForCatalogIndex(index) == Type.PURE_ANALYTE;
    }

    public static String interpretation(int index) {
        switch (typeForCatalogIndex(index)) {
            case MIXTURE:
                return "composition-dependent mixture/source class; report pattern/class evidence, not one pure-molecule ppm";
            case AGGREGATE_METRIC:
                return "operational aggregate/index; do not interpret as one molecular species";
            case SOURCE_MATERIAL_HEADSPACE:
                return "source material/headspace class; report source-pattern evidence separately from vapor-component identity";
            default:
                return "single chemical/elemental analyte entry";
        }
    }
}
