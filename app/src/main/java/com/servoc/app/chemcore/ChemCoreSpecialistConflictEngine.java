package com.servoc.app.chemcore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Specialist-channel conflict gate for HR069 identity promotion.
 *
 * This engine never re-ranks candidates and never chooses an analyte. Its only authority is to
 * prevent a named-identity promotion when strong primary-anchor evidence points to a competing
 * specialist hypothesis. Cross-response on a non-specialist channel cannot trigger the gate.
 */
public final class ChemCoreSpecialistConflictEngine {
    public static final String VERSION="chemcore-specialist-conflict/1";
    /** Research guard: anchor fit must be within 1.5 standardized residuals. */
    public static final double MAX_ANCHOR_ABS_Z=1.50;
    /** Candidate-wide fit must remain at least broadly compatible; avoids one-channel exact-fit vetoes. */
    public static final double MAX_CANDIDATE_RMS=3.00;

    public enum Verdict {
        NO_SPECIALIST_EVIDENCE,
        TOP_SPECIALIST_SUPPORTED,
        SPECIALIST_CHALLENGER,
        CROSS_SPECIALIST_CONFLICT
    }

    public static final class SpecialistSupport {
        public final long chemicalUid;
        public final int catalogIndex;
        public final String label,sensorKey;
        public final double standardizedResidual;
        public final int referenceContributors,calibratedContributors;
        public final double candidateResidualRms;

        SpecialistSupport(InverseMolecularSolver.Result r,int catalogIndex,String sensorKey,double z){
            this.chemicalUid=r.chemicalUid;this.catalogIndex=catalogIndex;this.label=r.label;
            this.sensorKey=sensorKey;this.standardizedResidual=z;
            this.referenceContributors=r.referenceContributors;this.calibratedContributors=r.calibratedContributors;
            this.candidateResidualRms=r.standardizedResidualRms;
        }
    }

    public static final class Result {
        public final Verdict verdict;
        public final boolean holdNamedIdentity;
        public final SpecialistSupport topSupport,challengerSupport;
        public final List<SpecialistSupport> strongSpecialists;
        public final String reason;

        Result(Verdict verdict,boolean holdNamedIdentity,SpecialistSupport topSupport,
               SpecialistSupport challengerSupport,List<SpecialistSupport> strongSpecialists,String reason){
            this.verdict=verdict;this.holdNamedIdentity=holdNamedIdentity;
            this.topSupport=topSupport;this.challengerSupport=challengerSupport;
            this.strongSpecialists=Collections.unmodifiableList(new ArrayList<>(strongSpecialists));
            this.reason=reason==null?"":reason;
        }
    }

    private ChemCoreSpecialistConflictEngine(){}

    public static Result evaluate(List<InverseMolecularSolver.Result> ranked){
        if(ranked==null||ranked.isEmpty())return none("no ranked candidates");
        InverseMolecularSolver.Result top=null;
        for(InverseMolecularSolver.Result r:ranked)if(r!=null&&r.solvable){top=r;break;}
        if(top==null)return none("no solvable candidate");

        List<SpecialistSupport> strong=new ArrayList<>();
        for(InverseMolecularSolver.Result r:ranked){
            SpecialistSupport s=strongPrimaryAnchor(r);
            if(s!=null)strong.add(s);
        }
        if(strong.isEmpty())return none("no strong primary-anchor evidence after multisensor/specific-calibration gate");

        SpecialistSupport topSupport=null;
        for(SpecialistSupport s:strong)if(s.chemicalUid==top.chemicalUid){topSupport=s;break;}

        List<SpecialistSupport> challengers=new ArrayList<>();
        for(SpecialistSupport s:strong)if(s.chemicalUid!=top.chemicalUid)challengers.add(s);
        challengers.sort(Comparator.comparingDouble(s->Math.abs(s.standardizedResidual)));

        if(challengers.isEmpty()){
            if(topSupport!=null)return new Result(Verdict.TOP_SPECIALIST_SUPPORTED,false,topSupport,null,strong,
                    "top candidate has compatible primary-anchor support and no competing strong primary specialist");
            return noneWith(strong,"strong specialist evidence exists only outside resolvable top mapping");
        }

        SpecialistSupport challenger=challengers.get(0);
        if(topSupport==null){
            return new Result(Verdict.SPECIALIST_CHALLENGER,true,null,challenger,strong,
                    "best-ranked candidate lacks strong primary-anchor support while a competing candidate has a compatible primary specialist; named identity is held");
        }

        if(!topSupport.sensorKey.equalsIgnoreCase(challenger.sensorKey)){
            return new Result(Verdict.CROSS_SPECIALIST_CONFLICT,true,topSupport,challenger,strong,
                    "distinct primary specialist channels support competing analyte hypotheses; treat as unresolved identity / possible mixture or interference until independent discrimination resolves the conflict");
        }

        return new Result(Verdict.SPECIALIST_CHALLENGER,true,topSupport,challenger,strong,
                "the same specialist channel supports competing hypotheses; selectivity is insufficient for named identity");
    }

    private static SpecialistSupport strongPrimaryAnchor(InverseMolecularSolver.Result r){
        if(r==null||!r.solvable||!Double.isFinite(r.standardizedResidualRms)||r.standardizedResidualRms>MAX_CANDIDATE_RMS)return null;
        // Single M2 channel hypotheses are deliberately excluded because continuous inversion can
        // create an exact fit. A device-calibrated M4 specialist may qualify with one channel.
        if(r.referenceContributors<2&&r.calibratedContributors<1)return null;
        int catalog=ChemCoreSpecialistRoleRegistry.catalogIndex(r.chemicalUid,r.label);
        if(catalog<0)return null;

        SpecialistSupport best=null;
        for(ChemCoreEvidenceObservation e:r.evidenceObservations){
            if(e==null||!e.insideDomain||!"PRIMARY".equals(e.supportRole))continue;
            if(e.tier==null||e.tier.ordinal()<ChemCoreDecision.EvidenceTier.M2_LITERATURE_OR_MANUFACTURER_SUPPORTED.ordinal())continue;
            if(!Double.isFinite(e.standardizedResidual)||Math.abs(e.standardizedResidual)>MAX_ANCHOR_ABS_Z)continue;
            for(String sensor:e.sensorKeys){
                if(ChemCoreSpecialistRoleRegistry.role(catalog,sensor)!=ChemCoreSpecialistRoleRegistry.Role.PRIMARY_ANCHOR)continue;
                SpecialistSupport candidate=new SpecialistSupport(r,catalog,sensor.toLowerCase(Locale.US),e.standardizedResidual);
                if(best==null||Math.abs(candidate.standardizedResidual)<Math.abs(best.standardizedResidual))best=candidate;
            }
        }
        return best;
    }

    private static Result none(String reason){return new Result(Verdict.NO_SPECIALIST_EVIDENCE,false,null,null,Collections.emptyList(),reason);}
    private static Result noneWith(List<SpecialistSupport> strong,String reason){return new Result(Verdict.NO_SPECIALIST_EVIDENCE,false,null,null,strong,reason);}
}
