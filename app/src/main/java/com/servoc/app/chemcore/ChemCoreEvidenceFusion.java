package com.servoc.app.chemcore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Positive-only, lineage-aware evidence fusion for the HazRob Analytical Confidence Index.
 *
 * Policy:
 *  - every chemically interpretable observation is retained;
 *  - full-quality independent evidence supplies the main confidence score;
 *  - immediate duplicates and upstream-lineage derivatives are de-duplicated separately;
 *  - QSPR/model-derived fingerprints that share the accepted reference-response library do not
 *    become additional independent proofs when direct source evidence is already present;
 *  - correlated derivatives may provide a small capped corroboration bonus;
 *  - degraded/out-of-domain evidence is support-only and capped;
 *  - posterior/model ranking remains separate from the Analytical Confidence Index.
 */
public final class ChemCoreEvidenceFusion {
    private ChemCoreEvidenceFusion(){}

    public static final double FULL_SUPPORT_STANDARDIZED_RESIDUAL=2.50;
    public static final double MAX_CORROBORATION_BONUS=8.0;
    public static final double MAX_CORRELATED_DERIVED_BONUS=3.0;

    public static final class Result {
        public final ChemCoreConfidenceMatrix.Score score;
        /** All chemically interpretable evidence, including degraded and correlated support. */
        public final List<ChemCoreEvidenceObservation> supportingEvidence;
        /** Independent confidence lineages that contribute points; correlated derivatives excluded. */
        public final int uniqueEvidenceGroups;
        public final int sensorBreadth;
        public final int qsprGroups,referenceGroups,hr069Groups,corroborativeGroups;
        /** Derived reference-library groups retained as corroboration but not independent proof. */
        public final int lineageCorrelatedGroups;

        Result(ChemCoreConfidenceMatrix.Score score,List<ChemCoreEvidenceObservation> supportingEvidence,
               int uniqueEvidenceGroups,int sensorBreadth,int qsprGroups,int referenceGroups,int hr069Groups,
               int corroborativeGroups,int lineageCorrelatedGroups){
            this.score=score;
            this.supportingEvidence=Collections.unmodifiableList(new ArrayList<>(supportingEvidence));
            this.uniqueEvidenceGroups=Math.max(0,uniqueEvidenceGroups);
            this.sensorBreadth=Math.max(0,sensorBreadth);
            this.qsprGroups=Math.max(0,qsprGroups);
            this.referenceGroups=Math.max(0,referenceGroups);
            this.hr069Groups=Math.max(0,hr069Groups);
            this.corroborativeGroups=Math.max(0,corroborativeGroups);
            this.lineageCorrelatedGroups=Math.max(0,lineageCorrelatedGroups);
        }
    }

    public static Result fuse(List<ChemCoreEvidenceObservation> rows){return fuseForChemical(Long.MIN_VALUE,rows,false);}
    public static Result fuseForChemical(long chemicalUid,List<ChemCoreEvidenceObservation> rows){return fuseForChemical(chemicalUid,rows,true);}

    private static Result fuseForChemical(long chemicalUid,List<ChemCoreEvidenceObservation> rows,boolean filterChemical){
        List<ChemCoreEvidenceObservation> usable=new ArrayList<>(),full=new ArrayList<>();
        if(rows!=null)for(ChemCoreEvidenceObservation e:rows){
            if(e==null)continue;if(filterChemical&&e.chemicalUid!=chemicalUid)continue;
            if(isPositiveSupport(e)){usable.add(e);if(isFullStrengthSupport(e))full.add(e);}
        }

        // Preserve the immediate evidence keys as a second de-duplication layer. This prevents a
        // degraded copy of a strong observation from reappearing merely because its lineage class
        // is labelled CORROBORATIVE_COPY.
        Map<String,Boolean> fullImmediateGroups=new LinkedHashMap<>();
        for(ChemCoreEvidenceObservation e:full){
            if(e.independenceGroup!=null&&!e.independenceGroup.trim().isEmpty())fullImmediateGroups.put(e.independenceGroup,Boolean.TRUE);
        }

        // Stage 28: full-strength evidence is de-duplicated by scientific lineage, not merely by
        // immediate independenceGroup. Distinct direct sources remain distinct lineages.
        Map<String,ChemCoreEvidenceObservation> lineageRepresentative=new LinkedHashMap<>();
        for(ChemCoreEvidenceObservation e:full){
            String key=ChemCoreEvidenceLineagePolicy.confidenceLineageKey(e);
            ChemCoreEvidenceObservation current=lineageRepresentative.get(key);
            if(current==null||preferred(e,current))lineageRepresentative.put(key,e);
        }

        boolean hasDirectReference=false;
        for(ChemCoreEvidenceObservation e:lineageRepresentative.values()){
            if(ChemCoreEvidenceLineagePolicy.classify(e).kind==ChemCoreEvidenceLineagePolicy.Kind.DIRECT_EXTERNAL_REFERENCE){
                hasDirectReference=true;break;
            }
        }

        int q=0,r=0,m4=0;
        Map<String,ChemCoreEvidenceObservation> correlatedDerived=new LinkedHashMap<>();
        for(Map.Entry<String,ChemCoreEvidenceObservation> entry:lineageRepresentative.entrySet()){
            ChemCoreEvidenceObservation e=entry.getValue();
            ChemCoreEvidenceLineagePolicy.Kind kind=ChemCoreEvidenceLineagePolicy.classify(e).kind;
            switch(kind){
                case HR069_DEVICE_CALIBRATION:
                    m4++;break;
                case DIRECT_EXTERNAL_REFERENCE:
                case HR069_DEVICE_EXPERIMENTAL_FINGERPRINT:
                case EXTERNAL_REFERENCE_FINGERPRINT:
                    r++;break;
                case MODEL_DERIVED_REFERENCE_FINGERPRINT:
                    if(hasDirectReference)correlatedDerived.put(entry.getKey(),e);else r++;
                    break;
                case QSPR_MODEL_TRANSFER:
                    if(hasDirectReference)correlatedDerived.put(entry.getKey(),e);else q++;
                    break;
                default:
                    // Preserve safe compatibility for future typed evidence while refusing to
                    // manufacture independence from an unknown lineage.
                    if(isM4(e))m4++;
                    else if(isQspr(e))correlatedDerived.put(entry.getKey(),e);
                    else if(isReference(e))r++;
                    break;
            }
        }

        // Weak/degraded evidence contributes only when neither its immediate evidence group nor
        // its scientific lineage is already represented by full-strength evidence. Derived M1/M3
        // evidence sharing the reference library is routed into the correlated bucket whenever a
        // direct reference exists.
        Map<String,ChemCoreEvidenceObservation> weakRepresentative=new LinkedHashMap<>();
        for(ChemCoreEvidenceObservation e:usable){
            if(isFullStrengthSupport(e))continue;
            if(e.independenceGroup!=null&&fullImmediateGroups.containsKey(e.independenceGroup))continue;
            String key=ChemCoreEvidenceLineagePolicy.confidenceLineageKey(e);
            if(lineageRepresentative.containsKey(key))continue;
            if(hasDirectReference&&ChemCoreEvidenceLineagePolicy.correlatedDerivative(e)){
                ChemCoreEvidenceObservation current=correlatedDerived.get(key);
                if(current==null||supportWeight(e)>supportWeight(current))correlatedDerived.put(key,e);
                continue;
            }
            ChemCoreEvidenceObservation current=weakRepresentative.get(key);
            if(current==null||supportWeight(e)>supportWeight(current))weakRepresentative.put(key,e);
        }

        double weakCorroboration=0;int weakGroups=0;
        for(ChemCoreEvidenceObservation e:weakRepresentative.values()){
            double pts=corroborationPoints(e);if(pts>0){weakCorroboration+=pts;weakGroups++;}
        }

        double derivedCorroboration=0;int correlatedGroups=0;
        for(ChemCoreEvidenceObservation e:correlatedDerived.values()){
            double pts=corroborationPoints(e);if(pts>0){derivedCorroboration+=pts;correlatedGroups++;}
        }
        derivedCorroboration=Math.min(MAX_CORRELATED_DERIVED_BONUS,derivedCorroboration);
        double corroboration=Math.min(MAX_CORROBORATION_BONUS,weakCorroboration+derivedCorroboration);

        int breadth=ChemCoreEvidenceObservation.uniqueSensors(usable);
        ChemCoreConfidenceMatrix.Score score=ChemCoreConfidenceMatrix.scoreIndependent(q,r,m4,breadth,corroboration);
        int independentGroups=q+r+m4+weakGroups;
        return new Result(score,usable,independentGroups,breadth,q,r,m4,
                weakGroups+correlatedGroups,correlatedGroups);
    }

    /** Any non-M0 observation with some interpretable quality remains in positive-support fusion. */
    static boolean isPositiveSupport(ChemCoreEvidenceObservation e){
        return e!=null&&e.tier!=null&&e.tier!=ChemCoreDecision.EvidenceTier.M0_CATALOG_ONLY&&supportWeight(e)>0;
    }

    /** Full-strength proof keeps domain/fit requirements and requires PRIMARY quality. */
    static boolean isFullStrengthSupport(ChemCoreEvidenceObservation e){
        if(e==null||!e.insideDomain||!"PRIMARY".equals(e.supportRole)||e.measurementQualityWeight<0.75)return false;
        return !Double.isFinite(e.standardizedResidual)||Math.abs(e.standardizedResidual)<=FULL_SUPPORT_STANDARDIZED_RESIDUAL;
    }

    static double supportWeight(ChemCoreEvidenceObservation e){
        if(e==null||e.tier==null||e.tier==ChemCoreDecision.EvidenceTier.M0_CATALOG_ONLY)return 0;
        double w=Math.max(0,Math.min(1,e.measurementQualityWeight));if(w<=0)return 0;
        if(!e.insideDomain)w*=0.40;
        if(Double.isFinite(e.standardizedResidual)){
            double z=Math.abs(e.standardizedResidual);
            if(z>6.0)w*=0.10;else if(z>4.0)w*=0.30;else if(z>FULL_SUPPORT_STANDARDIZED_RESIDUAL)w*=0.60;
        }
        if("CORROBORATIVE".equals(e.supportRole))w=Math.min(w,0.70);
        return Math.max(0,Math.min(1,w));
    }

    private static double corroborationPoints(ChemCoreEvidenceObservation e){
        double base;int tier=rank(e.tier);
        if(tier>=rank(ChemCoreDecision.EvidenceTier.M4_HAZROB_VALIDATED))base=4.0;
        else if(tier>=rank(ChemCoreDecision.EvidenceTier.M2_LITERATURE_OR_MANUFACTURER_SUPPORTED))base=3.0;
        else if(tier>=rank(ChemCoreDecision.EvidenceTier.M1_MOLECULARLY_INFERRED))base=1.5;
        else base=0.5;
        return base*supportWeight(e);
    }

    private static boolean preferred(ChemCoreEvidenceObservation incoming,ChemCoreEvidenceObservation current){
        int a=rank(incoming.tier),b=rank(current.tier);if(a!=b)return a>b;
        if(incoming.measurementQualityWeight!=current.measurementQualityWeight)return incoming.measurementQualityWeight>current.measurementQualityWeight;
        boolean af=Double.isFinite(incoming.standardizedResidual),bf=Double.isFinite(current.standardizedResidual);if(af!=bf)return af;
        if(af&&Math.abs(incoming.standardizedResidual)!=Math.abs(current.standardizedResidual))return Math.abs(incoming.standardizedResidual)<Math.abs(current.standardizedResidual);
        return incoming.sensorKeys.size()>current.sensorKeys.size();
    }

    private static boolean isQspr(ChemCoreEvidenceObservation e){return e.type==ChemCoreEvidenceObservation.Type.QSPR||e.tier==ChemCoreDecision.EvidenceTier.M1_MOLECULARLY_INFERRED;}
    private static boolean isM4(ChemCoreEvidenceObservation e){return e.type==ChemCoreEvidenceObservation.Type.HR069_CALIBRATION||e.tier==ChemCoreDecision.EvidenceTier.M4_HAZROB_VALIDATED;}
    private static boolean isReference(ChemCoreEvidenceObservation e){int x=rank(e.tier);return x>=rank(ChemCoreDecision.EvidenceTier.M2_LITERATURE_OR_MANUFACTURER_SUPPORTED)&&x<rank(ChemCoreDecision.EvidenceTier.M4_HAZROB_VALIDATED);}
    private static int rank(ChemCoreDecision.EvidenceTier t){return t==null?0:t.ordinal();}
}
