package com.servoc.app.vision;

/**
 * Temporal evidence layer for visual flame inference.
 * A single frame never promotes persistent evidence. Input values are normalized
 * heuristic evidence scores, NOT calibrated probabilities of fire.
 */
public final class FlameEvidenceTracker {
    public enum State { CLEAR, CANDIDATE, PERSISTENT }

    public static final class Snapshot {
        public final State state;
        /** Current normalized heuristic evidence score in [0,1]; not a probability. */
        public final double evidenceScore;
        /** Compatibility alias retained for 0.6 source stability. Do not present as probability. */
        @Deprecated public final double probability;
        public final double ema,peak;
        public final int positiveFrames,totalFrames;
        public final long firstPositiveMs,lastFrameMs,persistentSinceMs;
        Snapshot(State state,double evidenceScore,double ema,double peak,int positiveFrames,int totalFrames,long firstPositiveMs,long lastFrameMs,long persistentSinceMs){
            this.state=state;this.evidenceScore=evidenceScore;this.probability=evidenceScore;this.ema=ema;this.peak=peak;this.positiveFrames=positiveFrames;this.totalFrames=totalFrames;this.firstPositiveMs=firstPositiveMs;this.lastFrameMs=lastFrameMs;this.persistentSinceMs=persistentSinceMs;
        }
    }

    private final double enterThreshold,exitThreshold,alpha;
    private final int requiredPositiveFrames;
    private final long requiredDurationMs,clearAfterMs;
    private State state=State.CLEAR;
    private double ema=0,peak=0,lastEvidenceScore=0;
    private int positives=0,total=0;
    private long firstPositive=-1,lastFrame=-1,persistentSince=-1;

    public FlameEvidenceTracker(){this(.72,.45,.30,4,900,1200);}
    public FlameEvidenceTracker(double enterThreshold,double exitThreshold,double alpha,int requiredPositiveFrames,long requiredDurationMs,long clearAfterMs){
        if(!(enterThreshold>exitThreshold&&exitThreshold>=0&&enterThreshold<=1))throw new IllegalArgumentException("thresholds");
        if(alpha<=0||alpha>1||requiredPositiveFrames<1||requiredDurationMs<0||clearAfterMs<0)throw new IllegalArgumentException("timing");
        this.enterThreshold=enterThreshold;this.exitThreshold=exitThreshold;this.alpha=alpha;this.requiredPositiveFrames=requiredPositiveFrames;this.requiredDurationMs=requiredDurationMs;this.clearAfterMs=clearAfterMs;
    }

    public synchronized Snapshot update(double evidenceScore,long elapsedMs){
        if(!Double.isFinite(evidenceScore))evidenceScore=0;evidenceScore=Math.max(0,Math.min(1,evidenceScore));lastEvidenceScore=evidenceScore;lastFrame=elapsedMs;total++;ema=total==1?evidenceScore:(alpha*evidenceScore+(1-alpha)*ema);peak=Math.max(peak,evidenceScore);
        if(evidenceScore>=enterThreshold){if(firstPositive<0)firstPositive=elapsedMs;positives++;if(state==State.CLEAR)state=State.CANDIDATE;if(positives>=requiredPositiveFrames&&elapsedMs-firstPositive>=requiredDurationMs&&ema>=exitThreshold){state=State.PERSISTENT;if(persistentSince<0)persistentSince=elapsedMs;}}
        else if(evidenceScore<exitThreshold){if(state==State.PERSISTENT){if(elapsedMs-persistentSince>=clearAfterMs)resetEvidence();}else resetEvidence();}
        return snapshot();
    }

    public synchronized Snapshot missing(long elapsedMs){if(lastFrame>=0&&elapsedMs-lastFrame>clearAfterMs)resetEvidence();return snapshot();}
    public synchronized void reset(){state=State.CLEAR;ema=peak=lastEvidenceScore=0;positives=total=0;firstPositive=lastFrame=persistentSince=-1;}
    public synchronized Snapshot snapshot(){return new Snapshot(state,lastEvidenceScore,ema,peak,positives,total,firstPositive,lastFrame,persistentSince);}

    private void resetEvidence(){state=State.CLEAR;positives=0;firstPositive=-1;persistentSince=-1;peak=lastEvidenceScore;}
}
