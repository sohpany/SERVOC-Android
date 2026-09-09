package com.servoc.app.chemcore;

import java.util.List;

/**
 * Adaptive Multisensor Chemical Interrogation policy. The first implementation is
 * deterministic and auditable; future learned policies can implement the same action contract.
 */
public final class AmciDecisionEngine {
    private AmciDecisionEngine() {}

    public enum Action {
        ACCEPT_RESULT,
        EXTEND_EXPOSURE,
        WAIT_FOR_STABILITY,
        OBSERVE_RECOVERY,
        REPEAT_EXPOSURE,
        REQUEST_SPATIAL_CONFIRMATION,
        RETURN_UNKNOWN,
        ABORT_INVALID
    }

    public static final class Policy {
        public final String version;
        public final double acceptPosterior;
        public final double maxNormalizedEntropy;
        public final double minFeatureCoverage;
        public final double maxAbsStabilitySlope;
        public final double extensionSeconds;
        public final double maxExposureSeconds;
        public final int maxRepeats;

        public Policy(String version,double acceptPosterior,double maxNormalizedEntropy,double minFeatureCoverage,
                      double maxAbsStabilitySlope,double extensionSeconds,double maxExposureSeconds,int maxRepeats){
            this.version=version==null?"":version;this.acceptPosterior=acceptPosterior;this.maxNormalizedEntropy=maxNormalizedEntropy;
            this.minFeatureCoverage=minFeatureCoverage;this.maxAbsStabilitySlope=maxAbsStabilitySlope;
            this.extensionSeconds=extensionSeconds;this.maxExposureSeconds=maxExposureSeconds;this.maxRepeats=Math.max(0,maxRepeats);
        }

        public static Policy researchV1(){return new Policy("amci-policy/1",0.90,0.45,0.80,0.0025,15,120,2);}
    }

    public static final class Observation {
        public final boolean baselineStable,measurementValid,openSetAccepted,recoveryObservable,spatialConfirmationAvailable;
        public final double topPosterior,normalizedEntropy,featureCoverage,absStabilitySlope,exposureSeconds;
        public final int repeatCount;

        public Observation(boolean baselineStable,boolean measurementValid,boolean openSetAccepted,boolean recoveryObservable,
                           boolean spatialConfirmationAvailable,double topPosterior,double normalizedEntropy,double featureCoverage,
                           double absStabilitySlope,double exposureSeconds,int repeatCount){
            this.baselineStable=baselineStable;this.measurementValid=measurementValid;this.openSetAccepted=openSetAccepted;
            this.recoveryObservable=recoveryObservable;this.spatialConfirmationAvailable=spatialConfirmationAvailable;
            this.topPosterior=topPosterior;this.normalizedEntropy=normalizedEntropy;this.featureCoverage=featureCoverage;
            this.absStabilitySlope=absStabilitySlope;this.exposureSeconds=exposureSeconds;this.repeatCount=repeatCount;
        }
    }

    public static final class Decision {
        public final Action action;
        public final double suggestedAdditionalSeconds;
        public final String reason;
        Decision(Action action,double suggestedAdditionalSeconds,String reason){this.action=action;this.suggestedAdditionalSeconds=suggestedAdditionalSeconds;this.reason=reason==null?"":reason;}
    }

    public static Decision decide(Observation o,Policy p){
        if(o==null)return new Decision(Action.ABORT_INVALID,0,"missing AMCI observation");
        if(p==null)p=Policy.researchV1();
        if(!o.measurementValid)return new Decision(Action.ABORT_INVALID,0,"measurement quality gate failed");
        if(!o.baselineStable)return new Decision(Action.WAIT_FOR_STABILITY,p.extensionSeconds,"baseline is not stable");
        if(!(o.featureCoverage>=p.minFeatureCoverage))return new Decision(Action.EXTEND_EXPOSURE,p.extensionSeconds,"insufficient valid feature coverage");
        if(Double.isFinite(o.absStabilitySlope)&&o.absStabilitySlope>p.maxAbsStabilitySlope&&o.exposureSeconds<p.maxExposureSeconds)
            return new Decision(Action.WAIT_FOR_STABILITY,Math.min(p.extensionSeconds,p.maxExposureSeconds-o.exposureSeconds),"response has not reached the configured stability envelope");
        if(o.openSetAccepted&&Double.isFinite(o.topPosterior)&&o.topPosterior>=p.acceptPosterior&&Double.isFinite(o.normalizedEntropy)&&o.normalizedEntropy<=p.maxNormalizedEntropy)
            return new Decision(Action.ACCEPT_RESULT,0,"identity evidence is decisive inside the trained domain");
        if(o.exposureSeconds<p.maxExposureSeconds)
            return new Decision(Action.EXTEND_EXPOSURE,Math.min(p.extensionSeconds,p.maxExposureSeconds-o.exposureSeconds),"identity uncertainty remains high");
        if(o.recoveryObservable&&o.repeatCount<p.maxRepeats)
            return new Decision(Action.OBSERVE_RECOVERY,0,"exposure limit reached; recovery kinetics may add discriminatory information");
        if(o.repeatCount<p.maxRepeats)
            return new Decision(Action.REPEAT_EXPOSURE,0,"repeat controlled exposure required for confirmation");
        if(o.spatialConfirmationAvailable)
            return new Decision(Action.REQUEST_SPATIAL_CONFIRMATION,0,"local analytical evidence remains ambiguous after allowed repeats");
        return new Decision(Action.RETURN_UNKNOWN,0,"evidence remains insufficient after the configured AMCI interrogation sequence");
    }

    /** Expected information gain in nats: H(current)-E[H(after action)]. */
    public static double expectedInformationGain(double[] currentPosterior,List<double[]> possiblePosteriors,double[] outcomeProbabilities){
        double h0=ChemCoreOpenSetGate.entropy(currentPosterior);if(!Double.isFinite(h0)||possiblePosteriors==null||outcomeProbabilities==null||possiblePosteriors.size()!=outcomeProbabilities.length)return Double.NaN;
        double sumP=0,expected=0;for(int i=0;i<outcomeProbabilities.length;i++){double p=outcomeProbabilities[i];double h=ChemCoreOpenSetGate.entropy(possiblePosteriors.get(i));if(!Double.isFinite(p)||p<0||!Double.isFinite(h))continue;sumP+=p;expected+=p*h;}if(!(sumP>0))return Double.NaN;expected/=sumP;return h0-expected;
    }
}
