package com.servoc.app.chemcore;

import java.util.List;

/**
 * Conservative pure-compound consistency guard. It does not identify mixture components;
 * it only prevents a structurally poor single-compound fit from being over-reported.
 */
public final class MixtureResidualGuard {
    private MixtureResidualGuard() {}

    public enum Verdict { SINGLE_CANDIDATE_CONSISTENT, POSSIBLE_MIXTURE_OR_INTERFERENCE, AMBIGUOUS_IDENTITY, INSUFFICIENT_EVIDENCE }

    public static final class Result {
        public final Verdict verdict;
        public final double topPosterior;
        public final double secondPosterior;
        public final double standardizedResidualRms;
        public final String reason;
        Result(Verdict verdict,double topPosterior,double secondPosterior,double standardizedResidualRms,String reason){
            this.verdict=verdict;this.topPosterior=topPosterior;this.secondPosterior=secondPosterior;this.standardizedResidualRms=standardizedResidualRms;this.reason=reason==null?"":reason;
        }
    }

    public static Result evaluate(List<InverseMolecularSolver.Result> ranked,double maxResidualRms,double minIdentityPosterior,double minPosteriorMargin){
        if(ranked==null||ranked.isEmpty())return new Result(Verdict.INSUFFICIENT_EVIDENCE,Double.NaN,Double.NaN,Double.NaN,"no solvable chemical candidates");
        InverseMolecularSolver.Result top=ranked.get(0);if(top==null||!top.solvable)return new Result(Verdict.INSUFFICIENT_EVIDENCE,Double.NaN,Double.NaN,Double.NaN,"best candidate is not solvable");
        double second=ranked.size()>1&&ranked.get(1)!=null?ranked.get(1).posterior:0;
        double residualLimit=Double.isFinite(maxResidualRms)&&maxResidualRms>0?maxResidualRms:2.5;
        double posteriorLimit=Double.isFinite(minIdentityPosterior)?minIdentityPosterior:.80;
        double marginLimit=Double.isFinite(minPosteriorMargin)?minPosteriorMargin:.25;
        if(Double.isFinite(top.standardizedResidualRms)&&top.standardizedResidualRms>residualLimit)
            return new Result(Verdict.POSSIBLE_MIXTURE_OR_INTERFERENCE,top.posterior,second,top.standardizedResidualRms,"best pure-compound hypothesis leaves a large standardized multisensor residual");
        if(!(top.posterior>=posteriorLimit)||top.posterior-second<marginLimit)
            return new Result(Verdict.AMBIGUOUS_IDENTITY,top.posterior,second,top.standardizedResidualRms,"candidate posterior separation is insufficient");
        return new Result(Verdict.SINGLE_CANDIDATE_CONSISTENT,top.posterior,second,top.standardizedResidualRms,"pure-compound hypothesis is internally consistent within configured limits");
    }
}
