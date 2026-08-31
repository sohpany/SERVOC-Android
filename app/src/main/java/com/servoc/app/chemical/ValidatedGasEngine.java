package com.servoc.app.chemical;

import com.servoc.app.calibration.CalibrationCurveProvider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Evidence-weighted MQ analysis. Exposure interpretation is intentionally handled by authority-aware repositories outside this engine. */
public final class ValidatedGasEngine {
    private ValidatedGasEngine() {}

    /** v0.6: default R0 placeholders never imply reference-gas calibration. */
    public static final boolean PRECALIBRATED_INSTALLED_MQ=false;

    public static final class Result {
        public final int chemical;public final double ppm,agreement;public final int totalContributors,primaryContributors,calibratedContributors,manufacturerCrossContributors,communityCrossContributors,rejectedContributors,confidence,risk;public final boolean quantitative;public final String note;
        Result(int chemical,double ppm,double agreement,int totalContributors,int primaryContributors,int calibratedContributors,int manufacturerCrossContributors,int communityCrossContributors,int rejectedContributors,int confidence,boolean quantitative,int risk,String note){this.chemical=chemical;this.ppm=ppm;this.agreement=agreement;this.totalContributors=totalContributors;this.primaryContributors=primaryContributors;this.calibratedContributors=calibratedContributors;this.manufacturerCrossContributors=manufacturerCrossContributors;this.communityCrossContributors=communityCrossContributors;this.rejectedContributors=rejectedContributors;this.confidence=confidence;this.quantitative=quantitative;this.risk=risk;this.note=note;}
    }
    private static final class WeightedValue{final double value,weight;WeightedValue(double value,double weight){this.value=value;this.weight=weight;}}

    public static List<Result> analyze(double[] ratios,boolean[] heaterCycleValid){return analyze(ratios,heaterCycleValid,null,false);}
    public static List<Result> analyze(double[] ratios,boolean[] heaterCycleValid,CalibrationCurveProvider calibration,boolean identityConfirmed){List<Result> out=new ArrayList<>();if(ratios==null)return out;for(int chem:GasEngine.SUPPORTED){Result r=analyzeOne(ratios,chem,heaterCycleValid,calibration,identityConfirmed);if(r!=null)out.add(r);}return out;}
    public static Result analyzeOne(double[] ratios,int chemical,boolean[] heaterCycleValid){return analyzeOne(ratios,chemical,heaterCycleValid,null,false);}

    public static Result analyzeOne(double[] ratios,int chemical,boolean[] heaterCycleValid,CalibrationCurveProvider calibration,boolean identityConfirmed){
        List<GasEngine.Estimate> raw=GasEngine.estimatesForChemical(ratios,chemical);if(raw.isEmpty()&&calibration==null)return null;
        List<WeightedValue> accepted=new ArrayList<>();int primary=0,calibrated=0,calibratedPrimary=0,manufacturerCross=0,communityCross=0,rejected=0;boolean[] usedSensor=new boolean[Math.max(8,ratios==null?0:ratios.length)];

        if(calibration!=null&&ratios!=null){for(int sensor=0;sensor<ratios.length&&sensor<usedSensor.length;sensor++){double ratio=ratios[sensor];if(!Double.isFinite(ratio)||ratio<=0)continue;CalibrationCurveProvider.Estimate ce=calibration.estimate(sensor,chemical,ratio);if(ce==null||!ce.valid||!ce.referenceValidated||!Double.isFinite(ce.ppm)||ce.ppm<=0)continue;boolean heaterOk=heaterCycleValid==null||sensor>=heaterCycleValid.length||heaterCycleValid[sensor];SensorOperatingEnvelope.Check envelope=SensorOperatingEnvelope.check(sensor,chemical,ce.ppm,heaterOk);if(envelope.status==SensorOperatingEnvelope.OUT_OF_RANGE||envelope.status==SensorOperatingEnvelope.HEATER_PROTOCOL_INVALID){rejected++;continue;}accepted.add(new WeightedValue(Math.log10(ce.ppm),2.0));usedSensor[sensor]=true;calibrated++;if(envelope.status==SensorOperatingEnvelope.PRIMARY_TARGET){primary++;calibratedPrimary++;}else if(envelope.status==SensorOperatingEnvelope.MANUFACTURER_GRAPH_RESPONSE)manufacturerCross++;else communityCross++;}}

        // Generic manufacturer/research equations remain evidence for identity/fingerprint comparison.
        for(GasEngine.Estimate e:raw){if(e.sensor<usedSensor.length&&usedSensor[e.sensor])continue;boolean heaterOk=heaterCycleValid==null||e.sensor>=heaterCycleValid.length||heaterCycleValid[e.sensor];SensorOperatingEnvelope.Check c=SensorOperatingEnvelope.check(e.sensor,chemical,e.ppm,heaterOk);if(c.status==SensorOperatingEnvelope.PRIMARY_TARGET){accepted.add(new WeightedValue(Math.log10(e.ppm),1.0));primary++;}else if(c.status==SensorOperatingEnvelope.MANUFACTURER_GRAPH_RESPONSE){accepted.add(new WeightedValue(Math.log10(e.ppm),0.25));manufacturerCross++;}else if(c.status==SensorOperatingEnvelope.COMMUNITY_CROSS_RESPONSE){accepted.add(new WeightedValue(Math.log10(e.ppm),0.10));communityCross++;}else rejected++;}
        if(accepted.isEmpty())return null;

        double center=weightedMedian(accepted),ppm=Math.pow(10.0,center);List<WeightedValue> deviations=new ArrayList<>();for(WeightedValue v:accepted)deviations.add(new WeightedValue(Math.abs(v.value-center),v.weight));double mad=weightedMedian(deviations);double agreement=Math.max(0,Math.min(100,100*Math.exp(-1.8*mad)));
        boolean quantitative=calibratedPrimary>0||(calibrated>0&&identityConfirmed);
        int confidence;if((calibrated>=2||primary>=2)&&agreement>=65)confidence=GasEngine.HIGH;else if((calibrated>=1||primary>=1)&&accepted.size()>=2&&agreement>=45)confidence=GasEngine.STRONG;else confidence=GasEngine.SUSPECTED;
        // Retained for binary/source compatibility with v0.5 callers. Risk is no longer computed here.
        int risk=-1;
        String note;if(calibratedPrimary>0)note="Device-specific reference-gas calibration is active on a manufacturer-target channel; generic equations are secondary evidence.";else if(calibrated>0&&identityConfirmed)note="Device-specific cross-response calibration is active and analyte identity is independently confirmed.";else if(calibrated>0)note="A calibrated cross-response exists, but analyte identity is not independently confirmed; quantitative safety interpretation is withheld.";else if(primary>0)note="Manufacturer-target response is present, but this physical sensor has no validated reference-gas calibration; use for identification/fingerprint research only.";else if(manufacturerCross>0)note="Manufacturer graph cross-response only: fingerprint/research evidence, not selective quantitative safety use.";else note="Community/research curve evidence only: fingerprint comparison; no quantitative safety interpretation.";
        return new Result(chemical,ppm,agreement,accepted.size(),primary,calibrated,manufacturerCross,communityCross,rejected,confidence,quantitative,risk,note);
    }

    private static double weightedMedian(List<WeightedValue> values){if(values.isEmpty())return Double.NaN;List<WeightedValue>s=new ArrayList<>(values);Collections.sort(s,Comparator.comparingDouble(v->v.value));double total=0;for(WeightedValue v:s)total+=v.weight;double half=total*.5,acc=0;for(WeightedValue v:s){acc+=v.weight;if(acc>=half)return v.value;}return s.get(s.size()-1).value;}
}
