package com.servoc.app.chemcore;

import java.util.List;

/**
 * Positive-evidence-only confidence matrix for HazRob 0.10.2 / HR069.
 *
 * The score is an operational analytical confidence index (0..100), not a calibrated probability.
 * Full-strength independent evidence supplies the quality-first base score. Degraded but chemically
 * interpretable evidence may add a small capped corroboration bonus; it never subtracts points and
 * never upgrades quantitative reportability.
 */
public final class ChemCoreConfidenceMatrix {
    private ChemCoreConfidenceMatrix() {}

    public static final class Score {
        public final double totalPct;
        public final double qsprPct;
        public final double referencePct;
        public final double hr069Pct;
        public final double breadthPct;
        /** Small support-only bonus from degraded/weak corroborative evidence; capped by fusion. */
        public final double corroborationPct;
        /** Counts represent independent full-strength evidence groups, not MQ sensor channels. */
        public final int qsprCount;
        public final int referenceCount;
        public final int hr069Count;

        Score(double totalPct,double qsprPct,double referencePct,double hr069Pct,double breadthPct,double corroborationPct,
              int qsprCount,int referenceCount,int hr069Count){
            this.totalPct=clamp(totalPct,0,100);this.qsprPct=qsprPct;this.referencePct=referencePct;
            this.hr069Pct=hr069Pct;this.breadthPct=breadthPct;this.corroborationPct=Math.max(0,corroborationPct);
            this.qsprCount=Math.max(0,qsprCount);this.referenceCount=Math.max(0,referenceCount);this.hr069Count=Math.max(0,hr069Count);
        }
    }

    public static Score score(long chemicalUid,List<ChemCoreEvidenceObservation> evidence){
        return ChemCoreEvidenceFusion.fuseForChemical(chemicalUid,evidence).score;
    }

    /** Backward-compatible full-strength scoring entry point. */
    public static Score scoreIndependent(int qsprGroups,int referenceGroups,int hr069Groups,int sensorBreadth){
        return scoreIndependent(qsprGroups,referenceGroups,hr069Groups,sensorBreadth,0.0);
    }

    /**
     * Scores de-duplicated full-strength evidence groups, sensor breadth and capped weak support.
     * Corroboration is intentionally additive but small; fusion is responsible for capping it.
     */
    public static Score scoreIndependent(int qsprGroups,int referenceGroups,int hr069Groups,int sensorBreadth,double corroborationPct){
        int q=Math.max(0,qsprGroups),r=Math.max(0,referenceGroups),m4=Math.max(0,hr069Groups),n=Math.max(0,sensorBreadth);
        double qPts=repeatPoints(q,new double[]{20,10,6,4},1,45);
        double rPts=repeatPoints(r,new double[]{45,10,5},1,60);
        double mPts=repeatPoints(m4,new double[]{75,10,5},1,90);
        double breadth=n<=1?0:n==2?3:n==3?5:n==4?7:n<=6?9:10;
        double corroboration=clamp(corroborationPct,0,8.0);
        return new Score(qPts+rPts+mPts+breadth+corroboration,qPts,rPts,mPts,breadth,corroboration,q,r,m4);
    }

    @Deprecated
    public static Score score(int qsprCount,int referenceCount,int hr069Count,int totalContributors){
        int m4=Math.max(0,hr069Count);
        int referenceOnly=Math.max(0,Math.max(0,referenceCount)-m4);
        return scoreIndependent(Math.max(0,qsprCount),referenceOnly,m4,Math.max(0,totalContributors));
    }

    private static double repeatPoints(int count,double[] first,double tail,double cap){
        if(count<=0)return 0;double s=0;for(int i=0;i<count;i++)s+=i<first.length?first[i]:tail;return Math.min(cap,s);
    }
    private static double clamp(double x,double lo,double hi){return Math.max(lo,Math.min(hi,x));}
}
