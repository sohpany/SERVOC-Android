package com.servoc.app.chemcore;

/**
 * Positive-evidence-only confidence matrix for HazRob 0.10.2 / HR069.
 *
 * The score is an operational analytical confidence index (0..100), not a calibrated
 * probability. Only supporting evidence adds points. No contradiction penalty is applied.
 * Method quality dominates the base score; independent repetitions add diminishing support.
 */
public final class ChemCoreConfidenceMatrix {
    private ChemCoreConfidenceMatrix() {}

    public static final class Score {
        public final double totalPct;
        public final double qsprPct;
        public final double referencePct;
        public final double hr069Pct;
        public final double breadthPct;
        /** Counts represent independent evidence groups, not MQ sensor channels. */
        public final int qsprCount;
        public final int referenceCount;
        public final int hr069Count;

        Score(double totalPct,double qsprPct,double referencePct,double hr069Pct,double breadthPct,
              int qsprCount,int referenceCount,int hr069Count){
            this.totalPct=clamp(totalPct,0,100);this.qsprPct=qsprPct;this.referencePct=referencePct;
            this.hr069Pct=hr069Pct;this.breadthPct=breadthPct;this.qsprCount=Math.max(0,qsprCount);
            this.referenceCount=Math.max(0,referenceCount);this.hr069Count=Math.max(0,hr069Count);
        }
    }

    /**
     * Scores already de-duplicated independent evidence groups plus unique sensor breadth.
     *
     * Quality-first base values:
     *  - one HR069 device/reference validated group = 75 points
     *  - one direct literature/manufacturer/reference group = 45 points
     *  - one PubChem/QSPR molecular group = 20 points
     *
     * Independent repetitions add diminishing support:
     *  - QSPR:      +20, +10, +6, +4, then +1 each; cap 45
     *  - Reference: +45, +10, +5, then +1 each; cap 60
     *  - HR069 M4:  +75, +10, +5, then +1 each; cap 90
     *  - Multisensor breadth: 0..10 additional points
     */
    public static Score scoreIndependent(int qsprGroups,int referenceGroups,int hr069Groups,int sensorBreadth){
        int q=Math.max(0,qsprGroups),r=Math.max(0,referenceGroups),m4=Math.max(0,hr069Groups),n=Math.max(0,sensorBreadth);
        double qPts=repeatPoints(q,new double[]{20,10,6,4},1,45);
        double rPts=repeatPoints(r,new double[]{45,10,5},1,60);
        double mPts=repeatPoints(m4,new double[]{75,10,5},1,90);
        double breadth=n<=1?0:n==2?3:n==3?5:n==4?7:n<=6?9:10;
        return new Score(qPts+rPts+mPts+breadth,qPts,rPts,mPts,breadth,q,r,m4);
    }

    /**
     * Legacy compatibility API for pre-P6 callers that supplied raw tier counts. M4 entries are
     * removed from the M2+ count as before, but new code must use scoreIndependent after
     * de-duplicating by evidence independenceGroup.
     */
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
