package com.servoc.app.chemcore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Positive-only evidence fusion for the HazRob Analytical Confidence Index.
 *
 * Policy:
 *  - every chemically interpretable observation is retained and considered;
 *  - full-quality independent evidence supplies the main confidence score;
 *  - degraded/out-of-domain/poor-fit evidence may still corroborate with a small capped bonus;
 *  - evidence from the same independenceGroup never becomes a second proof merely because another
 *    MQ channel or lower-quality copy exists;
 *  - missing power/ADC/source information is diagnostic-only, not fabricated chemical evidence;
 *  - posterior/model ranking remains separate from the Analytical Confidence Index.
 */
public final class ChemCoreEvidenceFusion {
    private ChemCoreEvidenceFusion(){}

    public static final double FULL_SUPPORT_STANDARDIZED_RESIDUAL=2.50;
    public static final double MAX_CORROBORATION_BONUS=8.0;

    public static final class Result {
        public final ChemCoreConfidenceMatrix.Score score;
        /** All chemically interpretable evidence, including degraded support-only observations. */
        public final List<ChemCoreEvidenceObservation> supportingEvidence;
        /** Number of independent groups that actually add positive confidence points. */
        public final int uniqueEvidenceGroups;
        public final int sensorBreadth;
        public final int qsprGroups,referenceGroups,hr069Groups,corroborativeGroups;

        Result(ChemCoreConfidenceMatrix.Score score,List<ChemCoreEvidenceObservation> supportingEvidence,
               int uniqueEvidenceGroups,int sensorBreadth,int qsprGroups,int referenceGroups,int hr069Groups,int corroborativeGroups){
            this.score=score;
            this.supportingEvidence=Collections.unmodifiableList(new ArrayList<>(supportingEvidence));
            this.uniqueEvidenceGroups=Math.max(0,uniqueEvidenceGroups);
            this.sensorBreadth=Math.max(0,sensorBreadth);
            this.qsprGroups=Math.max(0,qsprGroups);
            this.referenceGroups=Math.max(0,referenceGroups);
            this.hr069Groups=Math.max(0,hr069Groups);
            this.corroborativeGroups=Math.max(0,corroborativeGroups);
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

        Map<String,ChemCoreEvidenceObservation> representative=new LinkedHashMap<>();
        for(ChemCoreEvidenceObservation e:full){ChemCoreEvidenceObservation current=representative.get(e.independenceGroup);if(current==null||preferred(e,current))representative.put(e.independenceGroup,e);}

        boolean hasDirectReference=false;for(ChemCoreEvidenceObservation e:representative.values())if(isDirectReference(e)){hasDirectReference=true;break;}
        int q=0,r=0,m4=0;
        for(ChemCoreEvidenceObservation e:representative.values()){
            if(isM4(e)){m4++;continue;}if(isQspr(e)){q++;continue;}
            if(isReference(e)){if(e.type==ChemCoreEvidenceObservation.Type.REFERENCE_FINGERPRINT&&hasDirectReference)continue;r++;}
        }

        // Weak/degraded evidence contributes only when its independence group is not already
        // represented by full-strength evidence. It is de-duplicated and capped so it can support
        // a decision without overpowering strong reference/M4 evidence.
        Map<String,ChemCoreEvidenceObservation> weakRepresentative=new LinkedHashMap<>();
        for(ChemCoreEvidenceObservation e:usable){
            if(isFullStrengthSupport(e)||representative.containsKey(e.independenceGroup))continue;
            ChemCoreEvidenceObservation current=weakRepresentative.get(e.independenceGroup);
            if(current==null||supportWeight(e)>supportWeight(current))weakRepresentative.put(e.independenceGroup,e);
        }
        double corroboration=0;int weakGroups=0;
        for(ChemCoreEvidenceObservation e:weakRepresentative.values()){
            double pts=corroborationPoints(e);if(pts>0){corroboration+=pts;weakGroups++;}
        }
        corroboration=Math.min(MAX_CORROBORATION_BONUS,corroboration);

        int breadth=ChemCoreEvidenceObservation.uniqueSensors(usable);
        ChemCoreConfidenceMatrix.Score score=ChemCoreConfidenceMatrix.scoreIndependent(q,r,m4,breadth,corroboration);
        int contributingGroups=q+r+m4+weakGroups;
        return new Result(score,usable,contributingGroups,breadth,q,r,m4,weakGroups);
    }

    /** Any non-M0 observation with some interpretable quality remains in positive-support fusion. */
    static boolean isPositiveSupport(ChemCoreEvidenceObservation e){
        return e!=null&&e.tier!=null&&e.tier!=ChemCoreDecision.EvidenceTier.M0_CATALOG_ONLY&&supportWeight(e)>0;
    }

    /** Full-strength proof keeps the historical domain/fit requirements and requires PRIMARY quality. */
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
    private static boolean isDirectReference(ChemCoreEvidenceObservation e){
        if(e==null||!isReference(e))return false;switch(e.type){case LITERATURE_CURVE:case MANUFACTURER_CURVE:case RESEARCH_CURVE:case LEGACY_CURVE:case REFERENCE_CURVE:return true;default:return false;}
    }
    private static int rank(ChemCoreDecision.EvidenceTier t){return t==null?0:t.ordinal();}
}
