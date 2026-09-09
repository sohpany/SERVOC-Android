package com.servoc.app.chemcore;

/**
 * Conservative physics gate. It can reject physically inconsistent hypotheses but never
 * establishes chemical identity by itself.
 */
public final class MolecularFeasibilityGate {
    private MolecularFeasibilityGate() {}

    public enum Verdict { SUPPORTED, PHYSICALLY_IMPLAUSIBLE, INSUFFICIENT_DATA }

    public static final class Result {
        public final Verdict verdict;
        public final double saturationPpm;
        public final double idealGasDensityKgM3;
        public final double relativeDensityToAir;
        public final String reason;

        Result(Verdict verdict,double saturationPpm,double idealGasDensityKgM3,double relativeDensityToAir,String reason){
            this.verdict=verdict;this.saturationPpm=saturationPpm;this.idealGasDensityKgM3=idealGasDensityKgM3;
            this.relativeDensityToAir=relativeDensityToAir;this.reason=reason==null?"":reason;
        }
    }

    /**
     * vaporPressureKPa should represent saturation vapor pressure near the supplied temperature.
     * toleranceFactor >=1 allows uncertainty/nonequilibrium margin. This check is only applicable
     * when a meaningful equilibrium vapor-pressure ceiling exists for the candidate.
     */
    public static Result evaluate(double predictedPpm,double molarMassGPerMol,double vaporPressureKPa,
                                  double temperatureC,double totalPressureKPa,double toleranceFactor){
        double density=ChemCorePhysics.idealGasDensityKgM3(molarMassGPerMol,temperatureC,totalPressureKPa);
        double relative=ChemCorePhysics.relativeVaporDensityToAir(molarMassGPerMol);
        double sat=ChemCorePhysics.saturationPpm(vaporPressureKPa,totalPressureKPa);
        if(!(predictedPpm>=0)||!Double.isFinite(predictedPpm))
            return new Result(Verdict.INSUFFICIENT_DATA,sat,density,relative,"predicted concentration unavailable");
        if(!Double.isFinite(sat))
            return new Result(Verdict.INSUFFICIENT_DATA,sat,density,relative,"no source-backed vapor-pressure ceiling for this condition");
        double tol=Double.isFinite(toleranceFactor)&&toleranceFactor>=1?toleranceFactor:1.25;
        if(predictedPpm>sat*tol)
            return new Result(Verdict.PHYSICALLY_IMPLAUSIBLE,sat,density,relative,"candidate concentration exceeds vapor-pressure feasibility envelope");
        return new Result(Verdict.SUPPORTED,sat,density,relative,"candidate concentration is not rejected by the available physical envelope");
    }
}
