package com.servoc.app.chemcore;

import java.util.List;

/**
 * Conservative pure-compound consistency guard. It does not identify mixture components;
 * it only prevents a structurally poor single-compound fit from being over-reported.
 *
 * The candidate weights consumed here are the solver's relative softmax model-ranking weights,
 * not calibrated probabilities of chemical presence. The default numeric limits are frozen
 * research-policy thresholds pending controlled HR069 pure-gas/mixture validation.
 */
public final class MixtureResidualGuard {
    public static final String POLICY_VERSION="mixture-residual-guard/2";
    public static final String THRESHOLD_SEMANTICS="research_policy_thresholds_not_calibrated_presence_probabilities";
    public static final double DEFAULT_MAX_STANDARDIZED_RESIDUAL_RMS=2.50;
    public static final double DEFAULT_MIN_TOP_RANKING_WEIGHT=0.80;
    public static final double DEFAULT_MIN_RANKING_WEIGHT_MARGIN=0.25;

    private MixtureResidualGuard() {}

    public enum Verdict { SINGLE_CANDIDATE_CONSISTENT, POSSIBLE_MIXTURE_OR_INTERFERENCE, AMBIGUOUS_IDENTITY, INSUFFICIENT_EVIDENCE }

    public static final class Result {
        public final Verdict verdict;
        /** Compatibility name: relative model-ranking weight, not calibrated probability. */
        public final double topPosterior;
        /** Compatibility name: relative model-ranking weight, not calibrated probability. */
        public final double secondPosterior;
        public final double standardizedResidualRms;
        public final String reason;
        Result(Verdict verdict,double topPosterior,double secondPosterior,double standardizedResidualRms,String reason){
            this.verdict=verdict;this.topPosterior=topPosterior;this.secondPosterior=secondPosterior;this.standardizedResidualRms=standardizedResidualRms;this.reason=reason==null?"":reason;
        }
        public double topRankingWeight(){return topPosterior;}
        public double secondRankingWeight(){return secondPosterior;}
    }

    /** Canonical HR069 research policy; numeric values are frozen until controlled validation. */
    public static Result evaluateDefault(List<InverseMolecularSolver.Result> ranked){
        return evaluate(ranked,DEFAULT_MAX_STANDARDIZED_RESIDUAL_RMS,DEFAULT_MIN_TOP_RANKING_WEIGHT,DEFAULT_MIN_RANKING_WEIGHT_MARGIN);
    }

    /**
     * Compatibility/custom-policy entry point. Parameter names are retained for API stability;
     * minIdentityPosterior and minPosteriorMargin operate on relative model-ranking weights.
     */
    public static Result evaluate(List<InverseMolecularSolver.Result> ranked,double maxResidualRms,double minIdentityPosterior,double minPosteriorMargin){
        if(ranked==null||ranked.isEmpty())return new Result(Verdict.INSUFFICIENT_EVIDENCE,Double.NaN,Double.NaN,Double.NaN,"no solvable chemical candidates");
        InverseMolecularSolver.Result top=ranked.get(0);if(top==null||!top.solvable)return new Result(Verdict.INSUFFICIENT_EVIDENCE,Double.NaN,Double.NaN,Double.NaN,"best candidate is not solvable");
        double second=ranked.size()>1&&ranked.get(1)!=null?ranked.get(1).posterior:0;
        double residualLimit=Double.isFinite(maxResidualRms)&&maxResidualRms>0?maxResidualRms:DEFAULT_MAX_STANDARDIZED_RESIDUAL_RMS;
        double rankingLimit=Double.isFinite(minIdentityPosterior)?minIdentityPosterior:DEFAULT_MIN_TOP_RANKING_WEIGHT;
        double marginLimit=Double.isFinite(minPosteriorMargin)?minPosteriorMargin:DEFAULT_MIN_RANKING_WEIGHT_MARGIN;
        if(Double.isFinite(top.standardizedResidualRms)&&top.standardizedResidualRms>residualLimit)
            return new Result(Verdict.POSSIBLE_MIXTURE_OR_INTERFERENCE,top.posterior,second,top.standardizedResidualRms,
                    "best pure-compound hypothesis leaves a large standardized multisensor residual; this is a mixture/interference warning, not component identification");
        if(!(top.posterior>=rankingLimit)||top.posterior-second<marginLimit)
            return new Result(Verdict.AMBIGUOUS_IDENTITY,top.posterior,second,top.standardizedResidualRms,
                    "relative candidate-ranking weight separation is insufficient; weights are not calibrated presence probabilities");
        return new Result(Verdict.SINGLE_CANDIDATE_CONSISTENT,top.posterior,second,top.standardizedResidualRms,
                "pure-compound hypothesis is internally consistent within frozen research-policy limits; "+THRESHOLD_SEMANTICS);
    }
}
