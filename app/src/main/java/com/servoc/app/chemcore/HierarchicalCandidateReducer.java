package com.servoc.app.chemcore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Hierarchical family -> compound reducer for ChemCore.
 *
 * Family labels are supplied by a provenance-bearing taxonomy layer; this class deliberately
 * does not infer chemical families from names or ad-hoc string heuristics. It only aggregates
 * already-computed compound posterior mass and selects candidate families deterministically.
 */
public final class HierarchicalCandidateReducer {
    private HierarchicalCandidateReducer() {}

    public static final String PROTOCOL = "family-posterior-mass/1";

    public static final class FamilyAssignment {
        public final long chemicalUid;
        public final String family;
        public final String sourceId;
        public FamilyAssignment(long chemicalUid,String family,String sourceId){
            this.chemicalUid=chemicalUid;this.family=normalizeFamily(family);this.sourceId=sourceId==null?"":sourceId;
        }
    }

    public static final class FamilyScore {
        public final String family;
        public final double posteriorMass;
        public final int compounds;
        FamilyScore(String family,double posteriorMass,int compounds){this.family=family;this.posteriorMass=posteriorMass;this.compounds=compounds;}
    }

    public static final class Result {
        public final String protocol;
        public final List<FamilyScore> families;
        public final Set<String> retainedFamilies;
        public final List<InverseMolecularSolver.Result> retainedCompounds;
        Result(List<FamilyScore> families,Set<String> retainedFamilies,List<InverseMolecularSolver.Result> retainedCompounds){
            this.protocol=PROTOCOL;this.families=Collections.unmodifiableList(families);
            this.retainedFamilies=Collections.unmodifiableSet(retainedFamilies);
            this.retainedCompounds=Collections.unmodifiableList(retainedCompounds);
        }
    }

    /**
     * Retains families until cumulative posterior mass reaches targetMass, while respecting
     * maxFamilies. Unassigned compounds are grouped as UNCLASSIFIED rather than silently lost.
     */
    public static Result reduce(List<InverseMolecularSolver.Result> compounds,
                                List<FamilyAssignment> assignments,
                                double targetMass,
                                int maxFamilies){
        double target=Double.isFinite(targetMass)?Math.max(0.0,Math.min(1.0,targetMass)):0.95;
        int cap=Math.max(1,maxFamilies);
        Map<Long,String> byUid=new LinkedHashMap<>();
        if(assignments!=null)for(FamilyAssignment a:assignments)if(a!=null&&a.chemicalUid>0&&!a.family.isEmpty())byUid.put(a.chemicalUid,a.family);

        Map<String,Double> mass=new LinkedHashMap<>();Map<String,Integer> count=new LinkedHashMap<>();
        if(compounds!=null)for(InverseMolecularSolver.Result r:compounds){
            if(r==null||!r.solvable||!Double.isFinite(r.posterior)||r.posterior<0)continue;
            String family=byUid.get(r.chemicalUid);if(family==null||family.isEmpty())family="UNCLASSIFIED";
            mass.put(family,mass.getOrDefault(family,0.0)+r.posterior);
            count.put(family,count.getOrDefault(family,0)+1);
        }
        List<FamilyScore> ranked=new ArrayList<>();for(Map.Entry<String,Double> e:mass.entrySet())ranked.add(new FamilyScore(e.getKey(),e.getValue(),count.getOrDefault(e.getKey(),0)));
        ranked.sort(Comparator.comparingDouble((FamilyScore x)->x.posteriorMass).reversed().thenComparing(x->x.family));

        Set<String> retained=new LinkedHashSet<>();double cumulative=0;
        for(FamilyScore f:ranked){
            if(retained.size()>=cap)break;
            retained.add(f.family);cumulative+=f.posteriorMass;
            if(cumulative+1e-12>=target)break;
        }
        List<InverseMolecularSolver.Result> kept=new ArrayList<>();
        if(compounds!=null)for(InverseMolecularSolver.Result r:compounds){
            if(r==null)continue;String family=byUid.get(r.chemicalUid);if(family==null||family.isEmpty())family="UNCLASSIFIED";
            if(retained.contains(family))kept.add(r);
        }
        kept.sort(Comparator.comparingDouble((InverseMolecularSolver.Result r)->r.posterior).reversed());
        return new Result(ranked,retained,kept);
    }

    private static String normalizeFamily(String s){return s==null?"":s.trim().toUpperCase(Locale.US);}
}
