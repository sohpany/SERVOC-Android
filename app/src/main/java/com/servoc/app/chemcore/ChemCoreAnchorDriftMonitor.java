package com.servoc.app.chemcore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Longitudinal clean-air/reference-anchor drift assessment for the HazRob MQ array.
 *
 * This class detects persistent multiplicative response shifts in log10 space. It never
 * rewrites R0, calibration curves, fingerprints or M4 evidence. Thresholds are supplied
 * by an explicit policy so literature/example values cannot silently become device limits.
 */
public final class ChemCoreAnchorDriftMonitor {
    private ChemCoreAnchorDriftMonitor(){}

    public static final String SCHEMA_VERSION="chemcore-anchor-drift/1";

    public enum Verdict { INSUFFICIENT_EVIDENCE, STABLE, WATCH, RECALIBRATION_REQUIRED }

    public static final class Policy {
        public final int minSessionsPerChannel;
        public final int minChannels;
        public final double watchAbsLog10Shift;
        public final double recalibrationAbsLog10Shift;
        public final int maxWatchChannelsBeforeRecalibration;
        public final String source;

        public Policy(int minSessionsPerChannel,int minChannels,double watchAbsLog10Shift,
                      double recalibrationAbsLog10Shift,int maxWatchChannelsBeforeRecalibration,
                      String source){
            if(minSessionsPerChannel<1)throw new IllegalArgumentException("minSessionsPerChannel must be >=1");
            if(minChannels<1)throw new IllegalArgumentException("minChannels must be >=1");
            if(!(watchAbsLog10Shift>0)||!Double.isFinite(watchAbsLog10Shift))throw new IllegalArgumentException("watch threshold must be finite and >0");
            if(!(recalibrationAbsLog10Shift>watchAbsLog10Shift)||!Double.isFinite(recalibrationAbsLog10Shift))throw new IllegalArgumentException("recalibration threshold must exceed watch threshold");
            if(maxWatchChannelsBeforeRecalibration<1)throw new IllegalArgumentException("maxWatchChannelsBeforeRecalibration must be >=1");
            this.minSessionsPerChannel=minSessionsPerChannel;this.minChannels=minChannels;
            this.watchAbsLog10Shift=watchAbsLog10Shift;this.recalibrationAbsLog10Shift=recalibrationAbsLog10Shift;
            this.maxWatchChannelsBeforeRecalibration=maxWatchChannelsBeforeRecalibration;
            this.source=source==null?"":source;
        }
    }

    public static final class ChannelResult {
        public final String sensorKey;
        public final int validSessions;
        public final double medianLog10Shift;
        public final double medianAbsDeviationLog10;
        public final double multiplicativeShift;
        public final boolean watch;
        public final boolean recalibration;

        ChannelResult(String sensorKey,int validSessions,double shift,double mad,Policy p){
            this.sensorKey=sensorKey;this.validSessions=validSessions;this.medianLog10Shift=shift;
            this.medianAbsDeviationLog10=mad;this.multiplicativeShift=Double.isFinite(shift)?Math.pow(10.0,shift):Double.NaN;
            double a=Math.abs(shift);this.recalibration=Double.isFinite(a)&&a>=p.recalibrationAbsLog10Shift;
            this.watch=!recalibration&&Double.isFinite(a)&&a>=p.watchAbsLog10Shift;
        }
    }

    public static final class Result {
        public final String schemaVersion=SCHEMA_VERSION;
        public final Verdict verdict;
        public final int eligibleChannels,watchChannels,recalibrationChannels;
        public final double medianAbsLog10Shift;
        public final List<ChannelResult> channels;
        public final String policySource;
        public final String note;

        Result(Verdict verdict,int eligible,int watch,int recal,double medianAbs,List<ChannelResult> channels,Policy p,String note){
            this.verdict=verdict;this.eligibleChannels=eligible;this.watchChannels=watch;this.recalibrationChannels=recal;
            this.medianAbsLog10Shift=medianAbs;this.channels=Collections.unmodifiableList(new ArrayList<>(channels));
            this.policySource=p.source;this.note=note;
        }

        /** Drift evidence is advisory; it never authorizes automatic baseline replacement. */
        public boolean automaticBaselineUpdatePermitted(){return false;}
    }

    /**
     * @param referenceRatios per-channel reference anchor (>0), typically the accepted clean-air/reference state
     * @param sessions independent clean-air/reference session summaries using the same ratio definition
     */
    public static Result assess(Map<String,Double> referenceRatios,List<Map<String,Double>> sessions,Policy policy){
        if(policy==null)throw new IllegalArgumentException("policy required");
        if(referenceRatios==null||referenceRatios.isEmpty()||sessions==null||sessions.isEmpty())
            return new Result(Verdict.INSUFFICIENT_EVIDENCE,0,0,0,Double.NaN,Collections.emptyList(),policy,"missing reference or longitudinal anchor sessions");

        List<ChannelResult> rows=new ArrayList<>();
        for(Map.Entry<String,Double> e:referenceRatios.entrySet()){
            String key=normalize(e.getKey());Double ref=e.getValue();if(key.isEmpty()||ref==null||!(ref>0)||!Double.isFinite(ref))continue;
            List<Double> shifts=new ArrayList<>();
            for(Map<String,Double> session:sessions){
                Double v=lookup(session,key);if(v==null||!(v>0)||!Double.isFinite(v))continue;
                shifts.add(Math.log10(v/ref));
            }
            if(shifts.size()<policy.minSessionsPerChannel)continue;
            double med=median(shifts),mad=medianAbsDeviation(shifts,med);
            rows.add(new ChannelResult(key,shifts.size(),med,mad,policy));
        }

        int watch=0,recal=0;List<Double> abs=new ArrayList<>();
        for(ChannelResult r:rows){if(r.recalibration)recal++;else if(r.watch)watch++;if(Double.isFinite(r.medianLog10Shift))abs.add(Math.abs(r.medianLog10Shift));}
        Verdict verdict;String note;
        if(rows.size()<policy.minChannels){verdict=Verdict.INSUFFICIENT_EVIDENCE;note="too few longitudinal channels satisfy the policy";}
        else if(recal>0||watch>=policy.maxWatchChannelsBeforeRecalibration){verdict=Verdict.RECALIBRATION_REQUIRED;note="persistent anchor shift exceeds the supplied drift policy; preserve existing calibration and open a new calibration revision";}
        else if(watch>0){verdict=Verdict.WATCH;note="anchor shift is detectable but below the supplied recalibration rule; collect additional independent anchors";}
        else{verdict=Verdict.STABLE;note="eligible channels remain within the supplied longitudinal drift policy";}
        return new Result(verdict,rows.size(),watch,recal,median(abs),rows,policy,note);
    }

    private static Double lookup(Map<String,Double> m,String key){if(m==null)return null;Double v=m.get(key);if(v!=null)return v;for(Map.Entry<String,Double> e:m.entrySet())if(normalize(e.getKey()).equals(key))return e.getValue();return null;}
    private static String normalize(String s){return s==null?"":s.trim().toLowerCase(Locale.US);}
    private static double median(List<Double> x){if(x==null||x.isEmpty())return Double.NaN;List<Double> y=new ArrayList<>();for(Double v:x)if(v!=null&&Double.isFinite(v))y.add(v);if(y.isEmpty())return Double.NaN;Collections.sort(y);int n=y.size();return(n&1)==1?y.get(n/2):0.5*(y.get(n/2-1)+y.get(n/2));}
    private static double medianAbsDeviation(List<Double> x,double center){if(!Double.isFinite(center))return Double.NaN;List<Double>d=new ArrayList<>();for(Double v:x)if(v!=null&&Double.isFinite(v))d.add(Math.abs(v-center));return median(d);}
}
