package com.servoc.app.chemcore;

/**
 * Stage-35 knowledge-map ceiling for the fixed 144-item HazRob panel.
 *
 * This is an early-build planning status, not a runtime veto. Controlled HR069 experiments may
 * promote or demote any entry without changing the catalogue identity.
 */
public final class ChemCoreIdentityCeilingMap {
    public enum Ceiling {
        PROVISIONAL_IDENTITY_CAPABLE,
        CANDIDATE,
        AFFINITY,
        UNSUPPORTED
    }

    private ChemCoreIdentityCeilingMap() {}

    private static final int[] ADEQUATE = {21,75,76,103};
    private static final int[] COMPATIBILITY_REVIEW = {0,10,11,12,13,14,15,16,17,18,19,42,43,44,45,46,47,60,61};
    private static final int[] UNSUPPORTED = {22,24,49,123,124,125,126,127,128,129,130,131,132,133,134};

    public static Ceiling ceilingForCatalogIndex(int index) {
        if(index < 0 || index >= 144) throw new IllegalArgumentException("catalog index must be 0..143");
        if(contains(ADEQUATE,index)) return Ceiling.PROVISIONAL_IDENTITY_CAPABLE;
        if(contains(COMPATIBILITY_REVIEW,index)) return Ceiling.AFFINITY;
        if(contains(UNSUPPORTED,index)) return Ceiling.UNSUPPORTED;
        return Ceiling.CANDIDATE;
    }

    public static boolean isRuntimeGate(){ return false; }

    public static String interpretation(int index){
        Ceiling c=ceilingForCatalogIndex(index);
        switch(c){
            case PROVISIONAL_IDENTITY_CAPABLE:
                return "current HR069 hardware/evidence route can support provisional identity development; validation can revise this status";
            case CANDIDATE:
                return "retain as analytical candidate/family candidate; exact identity remains experimental";
            case AFFINITY:
                return "retain affinity/family-level evidence while hardware compatibility or sensing route remains under review";
            default:
                return "current installed MQ array lacks a defensible primary selective route; keep only incidental/diagnostic response evidence";
        }
    }

    private static boolean contains(int[] a,int v){for(int x:a)if(x==v)return true;return false;}
}
