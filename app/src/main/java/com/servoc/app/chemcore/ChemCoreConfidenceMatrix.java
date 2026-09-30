package com.servoc.app.chemcore;

/**
 * Positive-evidence-only confidence matrix for HazRob 0.10.2 / HR069.
 *
 * The score is an operational analytical confidence index (0..100), not a calibrated
 * probability. Only supporting evidence adds points. No contradiction penalty is applied.
 * Repeated support within the same methodology has diminishing value and a hard cap so that
 * one evidence family cannot dominate the complete result by repetition alone.
 */
public final class ChemCoreConfidenceMatrix {
    private ChemCoreConfidenceMatrix() {}

    public static final class Score {
        public final double totalPct;
        public final double qsprPct;
        public final double referencePct;
        public final double hr069Pct;
        public final double breadthPct;
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
     * Weight budget:
     *  - PubChem/QSPR independent supporting channels/models: max 40
     *  - literature/manufacturer/direct reference support (M2+): max 30
     *  - HR069 device/reference validated support (M4): max 25
     *  - multisensor breadth bonus: max 5
     * Total = 100.
     *
     * Repetition schedules intentionally diminish:
     * QSPR       : +16, +10, +7, +4, then +1 each (cap 40)
     * Reference  : +14, +8,  +5, +2, then +1 each (cap 30)
     * HR069 M4   : +14, +7,  +3, then +1 each (cap 25)
     */
    public static Score score(int qsprCount,int referenceCount,int hr069Count,int totalContributors){
        int q=Math.max(0,qsprCount),r=Math.max(0,referenceCount),m4=Math.max(0,hr069Count),n=Math.max(0,totalContributors);
        double qPts=repeatPoints(q,new double[]{16,10,7,4},1,40);
        double rPts=repeatPoints(r,new double[]{14,8,5,2},1,30);
        double mPts=repeatPoints(m4,new double[]{14,7,3},1,25);
        double breadth=n<=1?0:n==2?2:n==3?3:n==4?4:5;
        return new Score(qPts+rPts+mPts+breadth,qPts,rPts,mPts,breadth,q,r,m4);
    }

    private static double repeatPoints(int count,double[] first,double tail,double cap){
        if(count<=0)return 0;double s=0;for(int i=0;i<count;i++)s+=i<first.length?first[i]:tail;return Math.min(cap,s);
    }
    private static double clamp(double x,double lo,double hi){return Math.max(lo,Math.min(hi,x));}
}
