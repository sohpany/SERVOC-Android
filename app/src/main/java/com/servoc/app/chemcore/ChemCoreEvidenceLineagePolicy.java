package com.servoc.app.chemcore;

/**
 * Canonical evidence-lineage/correlation map for ChemCore.
 *
 * Independence groups answer whether two observations are duplicate copies of the same immediate
 * evidence source. Lineage answers a different question: whether apparently different evidence
 * types are derived from the same upstream scientific information. A model-derived fingerprint
 * and a PubChem/QSPR transfer model can therefore have different independenceGroup values while
 * still sharing the accepted-reference response library as an upstream dependency.
 */
public final class ChemCoreEvidenceLineagePolicy {
    public static final String VERSION="chemcore-evidence-lineage/1";
    public static final String ACCEPTED_REFERENCE_LIBRARY="UPSTREAM:ACCEPTED_REFERENCE_RESPONSE_LIBRARY";
    public static final String PUBCHEM_MOLECULAR_LIBRARY="UPSTREAM:PUBCHEM_MOLECULAR_DESCRIPTORS";

    public enum Kind {
        HR069_DEVICE_CALIBRATION,
        HR069_DEVICE_EXPERIMENTAL_FINGERPRINT,
        DIRECT_EXTERNAL_REFERENCE,
        MODEL_DERIVED_REFERENCE_FINGERPRINT,
        QSPR_MODEL_TRANSFER,
        EXTERNAL_REFERENCE_FINGERPRINT,
        CORROBORATIVE_COPY,
        UNKNOWN
    }

    public enum Relation {
        SAME_IMMEDIATE_EVIDENCE,
        SAME_DIRECT_SOURCE,
        COMMON_DERIVED_REFERENCE_LIBRARY,
        DERIVED_MODEL_VS_REFERENCE_SOURCE,
        SAME_DEVICE_CAMPAIGN,
        INDEPENDENT,
        INDETERMINATE
    }

    public static final class Lineage {
        public final Kind kind;
        public final String immediateGroup;
        public final String rootSourceId;
        public final boolean dependsOnAcceptedReferenceLibrary;
        public final boolean dependsOnPubChem;
        public final boolean deviceLocal;
        public final String note;

        Lineage(Kind kind,String immediateGroup,String rootSourceId,
                boolean referenceLibrary,boolean pubchem,boolean deviceLocal,String note){
            this.kind=kind==null?Kind.UNKNOWN:kind;
            this.immediateGroup=clean(immediateGroup);
            this.rootSourceId=clean(rootSourceId);
            this.dependsOnAcceptedReferenceLibrary=referenceLibrary;
            this.dependsOnPubChem=pubchem;
            this.deviceLocal=deviceLocal;
            this.note=note==null?"":note;
        }
    }

    private ChemCoreEvidenceLineagePolicy(){}

    public static Lineage classify(ChemCoreEvidenceObservation e){
        if(e==null)return new Lineage(Kind.UNKNOWN,"","",false,false,false,"null evidence");
        switch(e.type){
            case HR069_CALIBRATION:
                return new Lineage(Kind.HR069_DEVICE_CALIBRATION,e.independenceGroup,e.sourceId,false,false,true,
                        "device/reference calibration lineage");
            case HR069_EXPERIMENTAL_FINGERPRINT:
                return new Lineage(Kind.HR069_DEVICE_EXPERIMENTAL_FINGERPRINT,e.independenceGroup,e.sourceId,false,false,true,
                        "independent HR069 empirical fingerprint lineage when source campaign is independent");
            case QSPR:
                return new Lineage(Kind.QSPR_MODEL_TRANSFER,e.independenceGroup,e.sourceId,true,true,false,
                        "molecular model-transfer evidence trained from accepted reference response relations plus PubChem descriptors");
            case REFERENCE_FINGERPRINT:
                if(isModelDerivedReferenceFingerprint(e))
                    return new Lineage(Kind.MODEL_DERIVED_REFERENCE_FINGERPRINT,e.independenceGroup,e.sourceId,true,false,false,
                            "deterministic/model-derived fingerprint generated from accepted source-backed response relations");
                return new Lineage(Kind.EXTERNAL_REFERENCE_FINGERPRINT,e.independenceGroup,e.sourceId,false,false,false,
                        "reference fingerprint with no declared model-derived reference-library lineage");
            case LITERATURE_CURVE:
            case MANUFACTURER_CURVE:
            case RESEARCH_CURVE:
            case LEGACY_CURVE:
            case REFERENCE_CURVE:
                return new Lineage(Kind.DIRECT_EXTERNAL_REFERENCE,e.independenceGroup,e.sourceId,false,false,false,
                        "direct/reference response evidence; independence between references requires distinct source lineage");
            case SENSOR_PATTERN:
                return new Lineage(Kind.CORROBORATIVE_COPY,e.independenceGroup,e.sourceId,false,false,false,
                        "support-only sensor pattern retains the immediate independence group of its underlying model");
            default:
                return new Lineage(Kind.UNKNOWN,e.independenceGroup,e.sourceId,false,false,false,"unclassified evidence lineage");
        }
    }

    /**
     * Canonical key for confidence de-duplication. Unlike independenceGroup, this key collapses
     * model-derived fingerprint and QSPR evidence onto their common upstream response library.
     * Direct references remain source-specific so distinct papers/manufacturer sources can retain
     * independence when provenance supports it.
     */
    public static String confidenceLineageKey(ChemCoreEvidenceObservation e){
        Lineage x=classify(e);
        switch(x.kind){
            case MODEL_DERIVED_REFERENCE_FINGERPRINT:
            case QSPR_MODEL_TRANSFER:
                return ACCEPTED_REFERENCE_LIBRARY;
            case DIRECT_EXTERNAL_REFERENCE:
                return "DIRECT:"+fallback(x.rootSourceId,x.immediateGroup,"unknown-reference-source");
            case HR069_DEVICE_CALIBRATION:
                return "HR069_CAL:"+fallback(x.rootSourceId,x.immediateGroup,"unknown-device-calibration");
            case HR069_DEVICE_EXPERIMENTAL_FINGERPRINT:
                return "HR069_FP:"+fallback(x.rootSourceId,x.immediateGroup,"unknown-device-fingerprint");
            case EXTERNAL_REFERENCE_FINGERPRINT:
                return "EXT_FP:"+fallback(x.rootSourceId,x.immediateGroup,"unknown-reference-fingerprint");
            case CORROBORATIVE_COPY:
                return "CORR:"+fallback(x.immediateGroup,x.rootSourceId,"unknown-corroborative");
            default:
                return "UNKNOWN:"+fallback(x.immediateGroup,x.rootSourceId,"unresolved");
        }
    }

    /**
     * Correlation relation used by Stage 28. It is intentionally conservative: unknown lineage is
     * not automatically declared independent.
     */
    public static Relation relation(ChemCoreEvidenceObservation a,ChemCoreEvidenceObservation b){
        if(a==null||b==null)return Relation.INDETERMINATE;
        Lineage x=classify(a),y=classify(b);
        if(nonblank(x.immediateGroup)&&x.immediateGroup.equals(y.immediateGroup))return Relation.SAME_IMMEDIATE_EVIDENCE;

        if(x.kind==Kind.DIRECT_EXTERNAL_REFERENCE&&y.kind==Kind.DIRECT_EXTERNAL_REFERENCE){
            if(nonblank(x.rootSourceId)&&x.rootSourceId.equals(y.rootSourceId))return Relation.SAME_DIRECT_SOURCE;
            return nonblank(x.rootSourceId)&&nonblank(y.rootSourceId)?Relation.INDEPENDENT:Relation.INDETERMINATE;
        }

        if(isDerivedReferenceModel(x.kind)&&isDerivedReferenceModel(y.kind))
            return Relation.COMMON_DERIVED_REFERENCE_LIBRARY;
        if((isDerivedReferenceModel(x.kind)&&y.kind==Kind.DIRECT_EXTERNAL_REFERENCE)||
                (isDerivedReferenceModel(y.kind)&&x.kind==Kind.DIRECT_EXTERNAL_REFERENCE))
            return Relation.DERIVED_MODEL_VS_REFERENCE_SOURCE;

        if(x.deviceLocal&&y.deviceLocal){
            if(nonblank(x.rootSourceId)&&x.rootSourceId.equals(y.rootSourceId))return Relation.SAME_DEVICE_CAMPAIGN;
            return nonblank(x.rootSourceId)&&nonblank(y.rootSourceId)?Relation.INDEPENDENT:Relation.INDETERMINATE;
        }

        if(x.kind==Kind.CORROBORATIVE_COPY||y.kind==Kind.CORROBORATIVE_COPY){
            if(nonblank(x.rootSourceId)&&x.rootSourceId.equals(y.rootSourceId))return Relation.SAME_DIRECT_SOURCE;
            return Relation.INDETERMINATE;
        }

        if(x.kind==Kind.UNKNOWN||y.kind==Kind.UNKNOWN)return Relation.INDETERMINATE;
        if(nonblank(x.rootSourceId)&&x.rootSourceId.equals(y.rootSourceId))return Relation.SAME_DIRECT_SOURCE;
        return Relation.INDEPENDENT;
    }

    public static boolean independentConfirmation(ChemCoreEvidenceObservation a,ChemCoreEvidenceObservation b){
        return relation(a,b)==Relation.INDEPENDENT;
    }

    public static boolean correlatedDerivative(ChemCoreEvidenceObservation e){
        Kind k=classify(e).kind;return isDerivedReferenceModel(k);
    }

    private static boolean isDerivedReferenceModel(Kind k){
        return k==Kind.MODEL_DERIVED_REFERENCE_FINGERPRINT||k==Kind.QSPR_MODEL_TRANSFER;
    }

    private static boolean isModelDerivedReferenceFingerprint(ChemCoreEvidenceObservation e){
        String family=clean(e.familyId).toUpperCase();String source=clean(e.sourceId).toUpperCase();
        return family.contains("SOURCE_BACKED_REFERENCE_FINGERPRINT")||
                source.contains("RESEARCHIDENTIFICATIONFINGERPRINTREGISTRY")||
                source.contains("ACCEPTED_REFERENCE_RESPONSE_LIBRARY");
    }

    private static String fallback(String a,String b,String c){return nonblank(a)?a:nonblank(b)?b:c;}
    private static String clean(String s){return s==null?"":s.trim();}
    private static boolean nonblank(String s){return s!=null&&!s.trim().isEmpty();}
}
