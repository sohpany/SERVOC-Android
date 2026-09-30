package com.servoc.app.chemcore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Positive-only evidence fusion for the HazRob Analytical Confidence Index.
 *
 * Rules:
 *  - evidence outside its declared model/environment domain contributes no confidence points;
 *  - a poor fit is not punished, it simply fails to add positive support;
 *  - confidence is de-duplicated by independenceGroup, never by sensor channel count;
 *  - sensor breadth is retained as a small, separate bonus;
 *  - posterior/model ranking is intentionally not used here.
 */
public final class ChemCoreEvidenceFusion {
    private ChemCoreEvidenceFusion(){}

    /** Conservative positive-support gate; values above this do not subtract points. */
    public static final double MAX_SUPPORT_STANDARDIZED_RESIDUAL=2.50;

    public static final class Result {
        public final ChemCoreConfidenceMatrix.Score score;
        public final List<ChemCoreEvidenceObservation> supportingEvidence;
        /** Number of independent groups that actually contribute confidence points after de-duplication guards. */
        public final int uniqueEvidenceGroups;
        public final int sensorBreadth;
        public final int qsprGroups,referenceGroups,hr069Groups;

        Result(ChemCoreConfidenceMatrix.Score score,List<ChemCoreEvidenceObservation> supportingEvidence,
               int uniqueEvidenceGroups,int sensorBreadth,int qsprGroups,int referenceGroups,int hr069Groups){
            this.score=score;
            this.supportingEvidence=Collections.unmodifiableList(new ArrayList<>(supportingEvidence));
            this.uniqueEvidenceGroups=Math.max(0,uniqueEvidenceGroups);
            this.sensorBreadth=Math.max(0,sensorBreadth);
            this.qsprGroups=Math.max(0,qsprGroups);
            this.referenceGroups=Math.max(0,referenceGroups);
            this.hr069Groups=Math.max(0,hr069Groups);
        }
    }

    public static Result fuse(List<ChemCoreEvidenceObservation> rows){
        return fuseForChemical(Long.MIN_VALUE,rows,false);
    }

    public static Result fuseForChemical(long chemicalUid,List<ChemCoreEvidenceObservation> rows){
        return fuseForChemical(chemicalUid,rows,true);
    }

    private static Result fuseForChemical(long chemicalUid,List<ChemCoreEvidenceObservation> rows,boolean filterChemical){
        List<ChemCoreEvidenceObservation> supporting=new ArrayList<>();
        if(rows!=null)for(ChemCoreEvidenceObservation e:rows){
            if(e==null)continue;
            if(filterChemical&&e.chemicalUid!=chemicalUid)continue;
            if(isPositiveSupport(e))supporting.add(e);
        }

        Map<String,ChemCoreEvidenceObservation> representative=new LinkedHashMap<>();
        for(ChemCoreEvidenceObservation e:supporting){
            ChemCoreEvidenceObservation current=representative.get(e.independenceGroup);
            if(current==null||preferred(e,current))representative.put(e.independenceGroup,e);
        }

        boolean hasDirectReference=false;
        for(ChemCoreEvidenceObservation e:representative.values())if(isDirectReference(e)){hasDirectReference=true;break;}

        int q=0,r=0,m4=0;
        for(ChemCoreEvidenceObservation e:representative.values()){
            if(isM4(e)){m4++;continue;}
            if(isQspr(e)){q++;continue;}
            if(isReference(e)){
                // ResearchIdentificationFingerprintRegistry templates are synthesized from
                // source-backed response priors. If direct reference curves already exist for
                // this candidate, the derived fingerprint contributes pattern/sensor breadth
                // but is not counted as another independent reference proof.
                if(e.type==ChemCoreEvidenceObservation.Type.REFERENCE_FINGERPRINT&&hasDirectReference)continue;
                r++;
            }
        }
        int breadth=ChemCoreEvidenceObservation.uniqueSensors(supporting);
        ChemCoreConfidenceMatrix.Score score=ChemCoreConfidenceMatrix.scoreIndependent(q,r,m4,breadth);
        // This count is deliberately the number of groups that contributed points. The complete
        // supportingEvidence trail is retained separately for audit, including derived patterns
        // that may add sensor breadth but are blocked from adding another independent proof.
        int contributingGroups=q+r+m4;
        return new Result(score,supporting,contributingGroups,breadth,q,r,m4);
    }

    static boolean isPositiveSupport(ChemCoreEvidenceObservation e){
        if(e==null||!e.insideDomain)return false;
        if(Double.isFinite(e.standardizedResidual)&&Math.abs(e.standardizedResidual)>MAX_SUPPORT_STANDARDIZED_RESIDUAL)return false;
        return e.tier!=null&&e.tier!=ChemCoreDecision.EvidenceTier.M0_CATALOG_ONLY;
    }

    private static boolean preferred(ChemCoreEvidenceObservation incoming,ChemCoreEvidenceObservation current){
        int a=rank(incoming.tier),b=rank(current.tier);if(a!=b)return a>b;
        boolean af=Double.isFinite(incoming.standardizedResidual),bf=Double.isFinite(current.standardizedResidual);
        if(af!=bf)return af;
        if(af&&Math.abs(incoming.standardizedResidual)!=Math.abs(current.standardizedResidual))
            return Math.abs(incoming.standardizedResidual)<Math.abs(current.standardizedResidual);
        return incoming.sensorKeys.size()>current.sensorKeys.size();
    }

    private static boolean isQspr(ChemCoreEvidenceObservation e){
        return e.type==ChemCoreEvidenceObservation.Type.QSPR||e.tier==ChemCoreDecision.EvidenceTier.M1_MOLECULARLY_INFERRED;
    }
    private static boolean isM4(ChemCoreEvidenceObservation e){
        return e.type==ChemCoreEvidenceObservation.Type.HR069_CALIBRATION||e.tier==ChemCoreDecision.EvidenceTier.M4_HAZROB_VALIDATED;
    }
    private static boolean isReference(ChemCoreEvidenceObservation e){
        int x=rank(e.tier);return x>=rank(ChemCoreDecision.EvidenceTier.M2_LITERATURE_OR_MANUFACTURER_SUPPORTED)&&x<rank(ChemCoreDecision.EvidenceTier.M4_HAZROB_VALIDATED);
    }
    private static boolean isDirectReference(ChemCoreEvidenceObservation e){
        if(e==null||!isReference(e))return false;
        switch(e.type){
            case LITERATURE_CURVE:
            case MANUFACTURER_CURVE:
            case RESEARCH_CURVE:
            case LEGACY_CURVE:
            case REFERENCE_CURVE:return true;
            default:return false;
        }
    }
    private static int rank(ChemCoreDecision.EvidenceTier t){return t==null?0:t.ordinal();}
}
