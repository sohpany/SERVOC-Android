package com.servoc.app.fingerprint;

import com.servoc.app.chemical.MqManufacturerOperatingContract;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Distance-based classifier for user-trained and explicitly separated research-prior fingerprints. */
public final class FingerprintClassifier {
    private FingerprintClassifier() {}

    public static final class Match {
        public final ChemicalFingerprint fingerprint;
        public final double score;
        public final double distance;
        public final int contributors;
        Match(ChemicalFingerprint f, double score, double distance, int contributors) {
            this.fingerprint = f; this.score = score; this.distance = distance; this.contributors = contributors;
        }
    }

    /** Existing empirical/user-trained fingerprint path. */
    public static List<Match> classify(double[] ratios, List<ChemicalFingerprint> library) {
        List<Match> out = new ArrayList<>();
        if (ratios == null || library == null) return out;
        for (ChemicalFingerprint f : library) {
            double sum = 0; int n = 0;
            int width = Math.min(ratios.length, f.meanLogRatio.length);
            for (int i = 0; i < width; i++) {
                double r = ratios[i], mu = f.meanLogRatio[i];
                if (!Double.isFinite(r) || r <= 0 || !Double.isFinite(mu)) continue;
                double sigma = (i < f.sigmaLogRatio.length && Double.isFinite(f.sigmaLogRatio[i])) ? f.sigmaLogRatio[i] : 0.15;
                sigma = Math.max(0.05, sigma);
                double z = (Math.log10(r) - mu) / sigma;
                sum += z * z; n++;
            }
            if (n == 0) continue;
            double d = Math.sqrt(sum / n);
            double coverage = Math.min(1.0, n / 4.0);
            double score = 100.0 * coverage * Math.exp(-0.5 * d);
            out.add(new Match(f, score, d, n));
        }
        out.sort(Comparator.comparingDouble((Match m) -> m.score).reversed());
        return out;
    }

    /**
     * Classifies against source-backed, model-derived HR069 research priors. Heater-cycled
     * channels are fail-closed when heater phase validity is unknown.
     */
    public static List<Match> classifyResearchPriors(double[] ratios) {
        return classifyResearchPriors(ratios,null);
    }

    /**
     * Returns at most one best concentration-template match per chemical. The result is an
     * identification/screening prior only; it never represents M4 device validation.
     */
    public static List<Match> classifyResearchPriors(double[] ratios, boolean[] heaterCycleValid) {
        List<Match> out=new ArrayList<>();
        if(ratios==null)return out;
        double[] usable=Arrays.copyOf(ratios,ratios.length);
        for(int i=0;i<usable.length;i++){
            if(!MqManufacturerOperatingContract.requiresHeaterCycle(i))continue;
            boolean valid=heaterCycleValid!=null&&i<heaterCycleValid.length&&heaterCycleValid[i];
            if(!valid)usable[i]=Double.NaN;
        }
        List<Match> all=classify(usable,ResearchIdentificationFingerprintRegistry.library());
        Map<Integer,Match> best=new LinkedHashMap<>();
        for(Match m:all){
            int chemical=m.fingerprint.chemicalId;
            if(!best.containsKey(chemical))best.put(chemical,m);
        }
        out.addAll(best.values());
        out.sort(Comparator.comparingDouble((Match m)->m.score).reversed());
        return out;
    }
}
