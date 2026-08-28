package com.servoc.app.chemical;

import com.servoc.app.exposure.ExposureLimits;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Evidence-weighted analysis layer for the physically installed MQ array.
 *
 * Primary-target contributions carry full weight. Datasheet cross-response
 * contributions can improve fingerprint/consensus evidence but cannot, by
 * themselves, promote a result to validated quantitative status.
 */
public final class ValidatedGasEngine {
    private ValidatedGasEngine() {}

    public static final class Result {
        public final int chemical;
        public final double ppm;
        public final double agreement;
        public final int totalContributors;
        public final int primaryContributors;
        public final int crossContributors;
        public final int rejectedContributors;
        public final int confidence;
        public final boolean quantitative;
        public final int risk;
        public final String note;

        Result(int chemical, double ppm, double agreement,
               int totalContributors, int primaryContributors,
               int crossContributors, int rejectedContributors,
               int confidence, boolean quantitative, int risk, String note) {
            this.chemical=chemical; this.ppm=ppm; this.agreement=agreement;
            this.totalContributors=totalContributors;
            this.primaryContributors=primaryContributors;
            this.crossContributors=crossContributors;
            this.rejectedContributors=rejectedContributors;
            this.confidence=confidence; this.quantitative=quantitative;
            this.risk=risk; this.note=note;
        }
    }

    private static final class WeightedValue {
        final double value;
        final double weight;
        WeightedValue(double value,double weight){this.value=value;this.weight=weight;}
    }

    public static List<Result> analyze(double[] ratios, boolean[] heaterCycleValid) {
        List<Result> out=new ArrayList<>();
        if(ratios==null) return out;
        for(int chem:GasEngine.SUPPORTED){
            Result r=analyzeOne(ratios,chem,heaterCycleValid);
            if(r!=null) out.add(r);
        }
        return out;
    }

    public static Result analyzeOne(double[] ratios, int chemical, boolean[] heaterCycleValid) {
        List<GasEngine.Estimate> raw=GasEngine.estimatesForChemical(ratios,chemical);
        if(raw.isEmpty()) return null;

        List<WeightedValue> accepted=new ArrayList<>();
        int primary=0, cross=0, rejected=0;

        for(GasEngine.Estimate e:raw){
            boolean heaterOk=heaterCycleValid==null || e.sensor>=heaterCycleValid.length || heaterCycleValid[e.sensor];
            SensorOperatingEnvelope.Check c=SensorOperatingEnvelope.check(e.sensor,chemical,e.ppm,heaterOk);
            if(c.status==SensorOperatingEnvelope.PRIMARY_TARGET){
                accepted.add(new WeightedValue(Math.log10(e.ppm),1.0));
                primary++;
            } else if(c.status==SensorOperatingEnvelope.DATASHEET_CROSS_RESPONSE){
                accepted.add(new WeightedValue(Math.log10(e.ppm),0.25));
                cross++;
            } else {
                rejected++;
            }
        }

        if(accepted.isEmpty()) return null;
        double center=weightedMedian(accepted);
        double ppm=Math.pow(10.0,center);

        List<WeightedValue> deviations=new ArrayList<>();
        for(WeightedValue v:accepted) deviations.add(new WeightedValue(Math.abs(v.value-center),v.weight));
        double mad=weightedMedian(deviations);
        double agreement=Math.max(0,Math.min(100,100*Math.exp(-1.8*mad)));

        boolean quantitative=primary>0;
        int confidence;
        if(primary>=2 && agreement>=65) confidence=GasEngine.HIGH;
        else if(primary>=1 && accepted.size()>=2 && agreement>=45) confidence=GasEngine.STRONG;
        else confidence=GasEngine.SUSPECTED;

        // Exposure screening is withheld from cross-response-only estimates.
        int risk=quantitative ? ExposureLimits.screeningRisk(chemical,ppm) : -1;
        String note=quantitative
                ? "At least one manufacturer primary-target sensor contributes; cross-response channels are down-weighted."
                : "Cross-response evidence only: use for fingerprint/research, not quantitative safety decisions.";

        return new Result(chemical,ppm,agreement,accepted.size(),primary,cross,rejected,
                confidence,quantitative,risk,note);
    }

    private static double weightedMedian(List<WeightedValue> values){
        if(values.isEmpty()) return Double.NaN;
        List<WeightedValue> s=new ArrayList<>(values);
        Collections.sort(s, Comparator.comparingDouble(v -> v.value));
        double total=0; for(WeightedValue v:s) total+=v.weight;
        double half=total*0.5, acc=0;
        for(WeightedValue v:s){
            acc+=v.weight;
            if(acc>=half) return v.value;
        }
        return s.get(s.size()-1).value;
    }
}
