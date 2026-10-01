package com.servoc.app.chemcore;

/** Empirical open-set gate for a trained chemical/family feature domain. */
public final class ChemCoreOpenSetGate {
    public static final String VERSION="chemcore-open-set/1";

    public static final class Result {
        public final boolean accepted;
        public final double distance;
        public final double threshold;
        public final double coverage;
        public final String reason;

        Result(boolean accepted, double distance, double threshold, double coverage, String reason) {
            this.accepted = accepted;
            this.distance = distance;
            this.threshold = threshold;
            this.coverage = coverage;
            this.reason = reason == null ? "" : reason;
        }
    }

    public final MahalanobisModel model;
    public final double threshold;
    public final double minimumCoverage;

    private ChemCoreOpenSetGate(MahalanobisModel model, double threshold, double minimumCoverage) {
        if (model == null) throw new IllegalArgumentException("model required");
        if (!Double.isFinite(threshold) || threshold <= 0) throw new IllegalArgumentException("positive finite threshold required");
        this.model = model;
        this.threshold = threshold;
        this.minimumCoverage = clampCoverage(minimumCoverage);
    }

    public static ChemCoreOpenSetGate fit(double[][] training, double ridgeFraction,
                                           double empiricalQuantile, double thresholdExpansion,
                                           double minimumCoverage) {
        MahalanobisModel model = MahalanobisModel.fit(training, ridgeFraction);
        double base = MahalanobisModel.empiricalQuantile(model.trainingDistances(training), empiricalQuantile);
        if (!Double.isFinite(base) || base <= 0) base = Math.sqrt(Math.max(1, model.mean.length));
        double expansion = Double.isFinite(thresholdExpansion) && thresholdExpansion >= 1.0 ? thresholdExpansion : 1.25;
        return new ChemCoreOpenSetGate(model, base * expansion, minimumCoverage);
    }

    /**
     * Builds a gate from a threshold selected on independent known/unknown validation sessions.
     * The caller must retain the calibration metrics/model card; this factory does not itself
     * assert that the threshold is scientifically validated.
     */
    public static ChemCoreOpenSetGate fromValidatedThreshold(MahalanobisModel model,
                                                              double threshold,
                                                              double minimumCoverage) {
        return new ChemCoreOpenSetGate(model, threshold, minimumCoverage);
    }

    public Result evaluate(double[] featureVector) {
        if (featureVector == null || featureVector.length != model.mean.length)
            return new Result(false, Double.NaN, threshold, 0, "dimension mismatch");
        int valid = 0;
        for (double v : featureVector) if (Double.isFinite(v)) valid++;
        double coverage = featureVector.length == 0 ? 0 : (double) valid / featureVector.length;
        if (coverage < minimumCoverage)
            return new Result(false, Double.NaN, threshold, coverage, "insufficient feature coverage");
        double d = model.distance(featureVector);
        if (!Double.isFinite(d)) return new Result(false, d, threshold, coverage, "invalid distance");
        boolean ok = d <= threshold;
        return new Result(ok, d, threshold, coverage,
                ok ? "inside trained feature domain" : "outside trained feature domain");
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
