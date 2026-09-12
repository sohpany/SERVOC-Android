package com.servoc.app.analytics;

import com.servoc.app.chemical.CandidateRanker;
import com.servoc.app.chemical.ValidatedGasEngine;
import java.util.ArrayList;
import java.util.List;

/**
 * Session/snapshot decision gate for the MQ electronic-nose layer.
 * It never upgrades fingerprint evidence into definitive chemical identification.
 */
public final class AnalysisDecision {
    private AnalysisDecision(){}

    public enum Outcome { UNKNOWN, AMBIGUOUS, CANDIDATE }

    public static final class Summary {
        public final Outcome outcome;
        public final int validChannels,calibratedR0Channels;
        public final boolean environmentAvailable,quantitativePermitted;
        public final double topScore,runnerUpScore,separationMargin;
        public final int topChemical,topContributors;
        public final String note;

        Summary(Outcome outcome,int validChannels,int calibratedR0Channels,boolean environmentAvailable,boolean quantitativePermitted,double topScore,double runnerUpScore,double separationMargin,int topChemical,int topContributors,String note){
            this.outcome=outcome;this.validChannels=validChannels;this.calibratedR0Channels=calibratedR0Channels;this.environmentAvailable=environmentAvailable;this.quantitativePermitted=quantitativePermitted;this.topScore=topScore;this.runnerUpScore=runnerUpScore;this.separationMargin=separationMargin;this.topChemical=topChemical;this.topContributors=topContributors;this.note=note;
        }
    }

    public static Summary evaluate(List<ValidatedGasEngine.Result> results,int validChannels,int calibratedR0Channels,boolean environmentAvailable){
        List<ValidatedGasEngine.Result> rows=new ArrayList<>();
        if(results!=null)for(ValidatedGasEngine.Result r:results)if(r!=null&&r.totalContributors>0)rows.add(r);
        rows.sort(CandidateRanker.COMPARATOR);
        validChannels=Math.max(0,Math.min(8,validChannels));
        calibratedR0Channels=Math.max(0,Math.min(8,calibratedR0Channels));
        if(rows.isEmpty())return new Summary(Outcome.UNKNOWN,validChannels,calibratedR0Channels,environmentAvailable,false,0,0,0,-1,0,"No evidence-bearing chemical candidate is available.");

        ValidatedGasEngine.Result top=rows.get(0);
        double topScore=finite(top.score),runner=rows.size()>1?finite(rows.get(1).score):0.0,margin=Math.max(0.0,topScore-runner);
        boolean quantitative=top.quantitative&&top.calibratedContributors>0;

        Outcome outcome;String note;
        if(validChannels<2||top.totalContributors<2||topScore<30.0){
            outcome=Outcome.UNKNOWN;
            note=validChannels<2?"Fewer than two valid MQ channels are available for a multi-sensor fingerprint.":top.totalContributors<2?"Top evidence is single-sensor only; inter-sensor identification is not supported.":"Top candidate score is below the screening decision threshold.";
        }else if(topScore<55.0||margin<10.0){
            outcome=Outcome.AMBIGUOUS;
            note=margin<10.0?"The leading candidates are insufficiently separated; report the result as ambiguous.":"Evidence is multi-sensor but not strong enough for a single leading candidate.";
        }else{
            outcome=Outcome.CANDIDATE;
            note="A leading screening candidate exists. This is an electronic-nose fingerprint result, not definitive laboratory identification.";
        }
        if(!environmentAvailable)note+=" Environmental T/RH/P compensation is unavailable for this snapshot.";
        if(!quantitative)note+=" Quantitative ppm interpretation is withheld until an accepted device/reference calibration gate passes.";
        return new Summary(outcome,validChannels,calibratedR0Channels,environmentAvailable,quantitative,topScore,runner,margin,top.chemical,top.totalContributors,note);
    }

    private static double finite(double v){return Double.isFinite(v)?Math.max(0.0,Math.min(100.0,v)):0.0;}
}
