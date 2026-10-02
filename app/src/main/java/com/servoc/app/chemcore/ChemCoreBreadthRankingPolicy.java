package com.servoc.app.chemcore;

import java.util.Comparator;
import java.util.List;

/**
 * Early-build ranking regularizer for ChemCore.
 *
 * A one-channel hypothesis can tune its single concentration parameter to pass very close to one
 * observation, so normalized residual alone can give it an artificial advantage over a chemically
 * coherent multi-channel hypothesis. This policy keeps every candidate visible and does not change
 * its fitted concentration or residual; it only reweights the existing relative ranking weight by
 * sqrt(N), where N is the number of direct/reference M2+ channels that actually contributed to the
 * session fit. M1/QSPR channels do not increase N. The factor is deliberately mild and remains a
 * research-stage ranking heuristic pending HR069 empirical independence/correlation estimates.
 */
public final class ChemCoreBreadthRankingPolicy {
    public static final String VERSION="chemcore-breadth-ranking/1";
    public static final String SEMANTICS="sqrt_actual_m2plus_contributor_breadth_reweighting_not_probability";

    private ChemCoreBreadthRankingPolicy() {}

    public static void apply(List<InverseMolecularSolver.Result> rows) {
        if(rows==null||rows.isEmpty())return;
        double sum=0;
        for(InverseMolecularSolver.Result r:rows){
            if(r==null||!r.solvable||!Double.isFinite(r.posterior)||r.posterior<=0)continue;
            int breadth=Math.max(1,r.referenceContributors);
            r.posterior*=Math.sqrt(breadth);
            sum+=r.posterior;
        }
        if(sum>0){
            for(InverseMolecularSolver.Result r:rows)
                if(r!=null&&r.solvable&&Double.isFinite(r.posterior)&&r.posterior>0)r.posterior/=sum;
        }
        rows.sort(Comparator.comparingDouble((InverseMolecularSolver.Result r)->
                r!=null&&r.solvable&&Double.isFinite(r.posterior)?r.posterior:Double.NEGATIVE_INFINITY).reversed());
    }
}
