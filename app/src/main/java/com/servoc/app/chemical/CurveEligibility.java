package com.servoc.app.chemical;

/** Classifies inherited power-law records by current manufacturer evidence. */
public final class CurveEligibility {
    private CurveEligibility() {}

    public static boolean hasTargetBackedCurve(int chemical){
        for(int sensor=0;sensor<8;sensor++)
            if(GasEngine.hasCurve(sensor,chemical)&&ManufacturerEvidenceMatrix.isTarget(sensor,chemical)) return true;
        return false;
    }

    public static boolean hasManufacturerGraphCurve(int chemical){
        for(int sensor=0;sensor<8;sensor++)
            if(GasEngine.hasCurve(sensor,chemical)&&ManufacturerEvidenceMatrix.isGraphResponse(sensor,chemical)) return true;
        return false;
    }

    public static boolean hasAnyLibraryCurve(int chemical){return GasEngine.hasAnyCurve(chemical);}

    public static boolean hasCommunityOnlyCurve(int chemical){
        return hasAnyLibraryCurve(chemical)&&!hasTargetBackedCurve(chemical)&&!hasManufacturerGraphCurve(chemical);
    }

    public static int targetBackedChemicalCount(){
        int n=0;for(int c=0;c<ChemicalCatalog.COUNT;c++)if(hasTargetBackedCurve(c))n++;return n;
    }

    public static int graphSupportedChemicalCount(){
        int n=0;for(int c=0;c<ChemicalCatalog.COUNT;c++)if(hasManufacturerGraphCurve(c))n++;return n;
    }

    public static int communityOnlyChemicalCount(){
        int n=0;for(int c=0;c<ChemicalCatalog.COUNT;c++)if(hasCommunityOnlyCurve(c))n++;return n;
    }
}
