package com.servoc.app.chemcore;

/** Empirical/validated open-set gate for a trained chemical/family feature domain. */
public final class ChemCoreOpenSetGate {
    public static final String VERSION="chemcore-open-set/2-provenance";

    /**
     * Provenance of the threshold used by the OOD gate. This is deliberately separate from the
     * geometric accepted/rejected result: an empirical training quantile is useful operationally,
     * but it must not be documented as independently validated unless that validation happened.
     */
    public enum GateKind {
        EMPIRICAL_TRAINING_QUANTILE,
        INDEPENDENT_VALIDATION_THRESHOLD,
        EXTERNAL_RUNTIME_POLICY
    }

    public static final class Result {
        public final boolean accepted;
        public final double distance;
        public final double threshold;
        public final double coverage;
        public final String reason;
        public final GateKind gateKind;
        public final boolean independentlyValidatedThreshold;

        /** Compatibility constructor for explicit runtime policies such as the live-fit gate. */
        Result(boolean accepted, double distance, double threshold, double coverage, String reason) {
            this(accepted,distance,threshold,coverage,reason,GateKind.EXTERNAL_RUNTIME_POLICY,false);
        }

        Result(boolean accepted,double distance,double threshold,double coverage,String reason,
               GateKind gateKind,boolean independentlyValidatedThreshold) {
            this.accepted = accepted;
            this.distance = distance;
            this.threshold = threshold;
            this.coverage = coverage;
            this.gateKind = gateKind == null ? GateKind.EXTERNAL_RUNTIME_POLICY : gateKind;
            this.independentlyValidatedThreshold = independentlyValidatedThreshold;
            String base=reason == null ? "" : reason;
            this.reason = base + (base.isEmpty()?"":"; ") + "gate_kind=" + this.gateKind.name()
                    + "; independently_validated_threshold=" + this.independentlyValidatedThreshold;
        }
    }

    public final MahalanobisModel model;
    public final double threshold;
    public final double minimumCoverage;
    public final GateKind gateKind;
    public final boolean independentlyValidatedThreshold;

    private ChemCoreOpenSetGate(MahalanobisModel model,double threshold,double minimumCoverage,
                                GateKind gateKind,boolean independentlyValidatedThreshold) {
        if (model == null) throw new IllegalArgumentException("model required");
        if (!Double.isFinite(threshold) || threshold <= 0) throw new IllegalArgumentException("positive finite threshold required");
        this.model = model;
        this.threshold = threshold;
        this.minimumCoverage = clampCoverage(minimumCoverage);
        this.gateKind = gateKind == null ? GateKind.EMPIRICAL_TRAINING_QUANTILE : gateKind;
        this.independentlyValidatedThreshold = independentlyValidatedThreshold;
    }

    public static ChemCoreOpenSetGate fit(double[][] training, double ridgeFraction,
                                           double empiricalQuantile, double thresholdExpansion,
                                           double minimumCoverage) {
        MahalanobisModel model = MahalanobisModel.fit(training, ridgeFraction);
        double base = MahalanobisModel.empiricalQuantile(model.trainingDistances(training), empiricalQuantile);
        if (!Double.isFinite(base) || base <= 0) base = Math.sqrt(Math.max(1, model.mean.length));
        double expansion = Double.isFinite(thresholdExpansion) && thresholdExpansion >= 1.0 ? thresholdExpansion : 1.25;
        return new ChemCoreOpenSetGate(model, base * expansion, minimumCoverage,
                GateKind.EMPIRICAL_TRAINING_QUANTILE,false);
    }

    /**
     * Builds a gate from a threshold selected on independent known/unknown validation sessions.
     * The caller must retain the calibration metrics/model card supporting this assertion.
     */
    public static ChemCoreOpenSetGate fromValidatedThreshold(MahalanobisModel model,
                                                              double threshold,
                                                              double minimumCoverage) {
        return new ChemCoreOpenSetGate(model, threshold, minimumCoverage,
                GateKind.INDEPENDENT_VALIDATION_THRESHOLD,true);
    }

    public Result evaluate(double[] featureVector) {
        if (featureVector == null || featureVector.length != model.mean.length)
            return result(false, Double.NaN, 0, "dimension mismatch");
        int valid = 0;
        for (double v : featureVector) if (Double.isFinite(v)) valid++;
        double coverage = featureVector.length == 0 ? 0 : (double) valid / featureVector.length;
        if (coverage < minimumCoverage)
            return result(false, Double.NaN, coverage, "insufficient feature coverage");
        double d = model.distance(featureVector);
        if (!Double.isFinite(d)) return result(false, d, coverage, "invalid distance");
        boolean ok = d <= threshold;
        return result(ok, d, coverage,
                ok ? "inside trained feature domain" : "outside trained feature domain");
    }

    private Result result(boolean accepted,double distance,double coverage,String reason){
        return new Result(accepted,distance,threshold,coverage,reason,gateKind,independentlyValidatedThreshold);
    }

    public static double entropy(double[] probabilities) {
        if (probabilities == null || probabilities.length == 0) return Double.NaN;
        double sum = 0;
        for (double p : probabilities) if (Double.isFinite(p) && p > 0) sum += p;
        if (!(sum > 0)) return Double.NaN;
        double h = 0;
        for (double p : probabilities) if (Double.isFinite(p) && p > 0) {
            double q = p / sum;
            h -= q * Math.log(q);
        }
        return h;
    }

    public static double normalizedEntropy(double[] probabilities) {
        double h = entropy(probabilities);
        int positive = 0;
        if (probabilities != null)
            for (double p : probabilities) if (Double.isFinite(p) && p > 0) positive++;
        return Double.isFinite(h) && positive > 1 ? h / Math.log(positive)
                : (positive == 1 ? 0 : Double.NaN);
    }

    private static double clampCoverage(double value) {
        return Double.isFinite(value) ? Math.max(0, Math.min(1, value)) : 0.75;
    }
}
