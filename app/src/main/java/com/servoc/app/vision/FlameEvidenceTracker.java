package com.servoc.app.vision;

/**
 * Temporal evidence layer for visual flame inference.
 * A single frame never promotes a flame alarm. Model probability is treated as evidence only.
 */
public final class FlameEvidenceTracker {
    public enum State { CLEAR, CANDIDATE, PERSISTENT }

    public static final class Snapshot {
        public final State state;
        public final double probability,ema,peak;
        public final int positiveFrames,totalFrames;
        public final long firstPositiveMs,lastFrameMs,persistentSinceMs;
        Snapshot(State state,double probability,double ema,double peak,int positiveFrames,int totalFrames,long firstPositiveMs,long lastFrameMs,long persistentSinceMs){
            this.state=state;this.probability=probability;this.ema=ema;this.peak=peak;this.positiveFrames=positiveFrames;this.totalFrames=totalFrames;this.firstPositiveMs=firstPositiveMs;this.lastFrameMs=lastFrameMs;this.persistentSinceMs=persistentSinceMs;
        }
    }

    private final double enterThreshold,exitThreshold,alpha;
    private final int requiredPositiveFrames;
    private final long requiredDurationMs,clearAfterMs;
    private State state=State.CLEAR;
    private double ema=0,peak=0,lastProbability=0;
    private int positives=0,total=0;
    private long firstPositive=-1,lastFrame=-1,persistentSince=-1;

    public FlameEvidenceTracker(){this(.72,.45,.30,4,900,1200);}
    public FlameEvidenceTracker(double enterThreshold,double exitThreshold,double alpha,int requiredPositiveFrames,long requiredDurationMs,long clearAfterMs){
        if(!(enterThreshold>exitThreshold&&exitThreshold>=0&&enterThreshold<=1))throw new IllegalArgumentException("thresholds");
        if(alpha<=0||alpha>1||requiredPositiveFrames<1||requiredDurationMs<0||clearAfterMs<0)throw new IllegalArgumentException("timing");
        this.enterThreshold=enterThreshold;this.exitThreshold=exitThreshold;this.alpha=alpha;this.requiredPositiveFrames=requiredPositiveFrames;this.requiredDurationMs=requiredDurationMs;this.clearAfterMs=clearAfterMs;
    }

    public synchronized Snapshot update(double probability,long elapsedMs){
        if(!Double.isFinite(probability))probability=0;probability=Math.max(0,Math.min(1,probability));lastProbability=probability;lastFrame=elapsedMs;total++;ema=total==1?probability:(alpha*probability+(1-alpha)*ema);peak=Math.max(peak,probability);
        if(probability>=enterThreshold){if(firstPositive<0)firstPositive=elapsedMs;positives++;if(state==State.CLEAR)state=State.CANDIDATE;if(positives>=requiredPositiveFrames&&elapsedMs-firstPositive>=requiredDurationMs&&ema>=exitThreshold){state=State.PERSISTENT;if(persistentSince<0)persistentSince=elapsedMs;}}
        else if(probability<exitThreshold){if(state==State.PERSISTENT){if(elapsedMs-persistentSince>=clearAfterMs)resetEvidence();}else resetEvidence();}
        return snapshot();
    }

    public synchronized Snapshot missing(long elapsedMs){if(lastFrame>=0&&elapsedMs-lastFrame>clearAfterMs)resetEvidence();return snapshot();}
    public synchronized void reset(){state=State.CLEAR;ema=peak=lastProbability=0;positives=total=0;firstPositive=lastFrame=persistentSince=-1;}
    public synchronized Snapshot snapshot(){return new Snapshot(state,lastProbability,ema,peak,positives,total,firstPositive,lastFrame,persistentSince);}

    private void resetEvidence(){state=State.CLEAR;positives=0;firstPositive=-1;persistentSince=-1;peak=lastProbability;}
}
