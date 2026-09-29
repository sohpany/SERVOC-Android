package com.servoc.app.chemical;

import com.servoc.app.calibration.CalibrationCurveProvider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Evidence-weighted MQ analysis. Exposure interpretation is intentionally handled by authority-aware repositories outside this engine. */
public final class ValidatedGasEngine {
    private ValidatedGasEngine() {}

    /** Compatibility flag: quantitative validity is now determined per contributor, never by a global default. */
    public static final boolean PRECALIBRATED_INSTALLED_MQ=false;
    private static final int INSTALLED_SENSOR_COUNT=8;

    public static final class Result {
        public final int chemical;
        public final double ppm,agreement,coverage,evidenceQuality,score;
        public final int totalContributors,primaryContributors,calibratedContributors,manufacturerCrossContributors,communityCrossContributors,rejectedContributors,agreementContributors,confidence,risk;
        public final boolean quantitative;
        public final String note,evidenceMode;

        Result(int chemical,double ppm,double agreement,int totalContributors,int primaryContributors,int calibratedContributors,int manufacturerCrossContributors,int communityCrossContributors,int rejectedContributors,int confidence,boolean quantitative,int risk,String note){
            this(chemical,ppm,agreement,defaultCoverage(totalContributors),defaultEvidence(primaryContributors,calibratedContributors,manufacturerCrossContributors,communityCrossContributors),defaultScore(agreement,totalContributors,primaryContributors,calibratedContributors,manufacturerCrossContributors,communityCrossContributors,quantitative),totalContributors,primaryContributors,calibratedContributors,manufacturerCrossContributors,communityCrossContributors,rejectedContributors,totalContributors,confidence,quantitative,risk,note);
        }

        Result(int chemical,double ppm,double agreement,double coverage,double evidenceQuality,double score,int totalContributors,int primaryContributors,int calibratedContributors,int manufacturerCrossContributors,int communityCrossContributors,int rejectedContributors,int agreementContributors,int confidence,boolean quantitative,int risk,String note){
            this.chemical=chemical;this.ppm=ppm;this.agreement=agreement;this.coverage=coverage;this.evidenceQuality=evidenceQuality;this.score=score;this.totalContributors=totalContributors;this.primaryContributors=primaryContributors;this.calibratedContributors=calibratedContributors;this.manufacturerCrossContributors=manufacturerCrossContributors;this.communityCrossContributors=communityCrossContributors;this.rejectedContributors=rejectedContributors;this.agreementContributors=agreementContributors;this.confidence=confidence;this.quantitative=quantitative;this.risk=risk;this.note=note;this.evidenceMode=totalContributors==1&&primaryContributors>0&&calibratedContributors>0?"SPECIALIST_SINGLE_SENSOR":(totalContributors>=2?"MULTI_SENSOR":"SINGLE_SENSOR_SCREENING");
        }
    }

    private static final class WeightedValue{final double value,weight;WeightedValue(double value,double weight){this.value=value;this.weight=weight;}}

    public static List<Result> analyze(double[] ratios,boolean[] heaterCycleValid){return analyze(ratios,heaterCycleValid,null,false);}
    public static List<Result> analyze(double[] ratios,boolean[] heaterCycleValid,CalibrationCurveProvider calibration,boolean identityConfirmed){
        List<Result> out=new ArrayList<>();if(ratios==null)return out;
        Set<Integer> chemicals=new LinkedHashSet<>();
        for(int chem:GasEngine.SUPPORTED)chemicals.add(chem);
        for(ResearchCurveRegistry.Curve c:ResearchCurveRegistry.CURVES)chemicals.add(c.chemical);
        for(LiteratureCurveRegistry.Curve c:LiteratureCurveRegistry.CURVES)chemicals.add(c.chemical);
        if(calibration!=null)for(int chem:calibration.chemicalIds())chemicals.add(chem);
        for(int chem:chemicals){Result r=analyzeOne(ratios,chem,heaterCycleValid,calibration,identityConfirmed);if(r!=null)out.add(r);}
        return out;
    }
    public static Result analyzeOne(double[] ratios,int chemical,boolean[] heaterCycleValid){return analyzeOne(ratios,chemical,heaterCycleValid,null,false);}

    public static Result analyzeOne(double[] ratios,int chemical,boolean[] heaterCycleValid,CalibrationCurveProvider calibration,boolean identityConfirmed){
        List<GasEngine.Estimate> raw=GasEngine.estimatesForChemical(ratios,chemical);
        if(raw.isEmpty()&&calibration==null&&!hasResearchCurve(chemical)&&!LiteratureCurveRegistry.hasAnyCurve(chemical))return null;
        List<WeightedValue> accepted=new ArrayList<>();
        int primary=0,calibrated=0,calibratedPrimary=0,manufacturerCross=0,communityCross=0,rejected=0,literatureAccepted=0;
        double evidencePoints=0;
        boolean[] usedSensor=new boolean[Math.max(INSTALLED_SENSOR_COUNT,ratios==null?0:ratios.length)];

        // Tier 1: HR069 device-specific reference-gas calibration always has first priority.
        if(calibration!=null&&ratios!=null){
            for(int sensor=0;sensor<ratios.length&&sensor<usedSensor.length;sensor++){
                double ratio=ratios[sensor];if(!Double.isFinite(ratio)||ratio<=0)continue;
                CalibrationCurveProvider.Estimate ce=calibration.estimate(sensor,chemical,ratio);
                if(ce==null||!ce.valid||!ce.referenceValidated||!Double.isFinite(ce.ppm)||ce.ppm<=0)continue;
                boolean heaterOk=heaterCycleValid==null||sensor>=heaterCycleValid.length||heaterCycleValid[sensor];
                SensorOperatingEnvelope.Check envelope=SensorOperatingEnvelope.check(sensor,chemical,ce.ppm,heaterOk);
                if(envelope.status==SensorOperatingEnvelope.OUT_OF_RANGE||envelope.status==SensorOperatingEnvelope.HEATER_PROTOCOL_INVALID){rejected++;continue;}
                accepted.add(new WeightedValue(Math.log10(ce.ppm),2.0));usedSensor[sensor]=true;calibrated++;
                if(envelope.status==SensorOperatingEnvelope.PRIMARY_TARGET){primary++;calibratedPrimary++;evidencePoints+=100.0;}
                else if(envelope.status==SensorOperatingEnvelope.MANUFACTURER_GRAPH_RESPONSE){manufacturerCross++;evidencePoints+=80.0;}
                else{communityCross++;evidencePoints+=65.0;}
            }
        }

        // Tier 2: peer-reviewed Rs/R0 graph fits. Prefer these over duplicate community
        // coefficients for the same physical channel, but never treat them as M4 calibration.
        if(ratios!=null){
            for(LiteratureCurveRegistry.Curve lc:LiteratureCurveRegistry.CURVES){
                if(lc.chemical!=chemical||lc.sensor>=ratios.length)continue;
                if(lc.sensor<usedSensor.length&&usedSensor[lc.sensor])continue;
                double ratio=ratios[lc.sensor];if(!Double.isFinite(ratio)||ratio<=0)continue;
                boolean heaterOk=heaterCycleValid==null||lc.sensor>=heaterCycleValid.length||heaterCycleValid[lc.sensor];
                if((lc.sensor==GasEngine.MQ7||lc.sensor==GasEngine.MQ9)&&!heaterOk){rejected++;continue;}
                double ppm=lc.ppmFromRatio(ratio);
                if(!Double.isFinite(ppm)||ppm<=0||!lc.inDeclaredRange(ppm)){rejected++;continue;}
                SensorOperatingEnvelope.Check envelope=SensorOperatingEnvelope.check(lc.sensor,chemical,ppm,heaterOk);
                if(envelope.status==SensorOperatingEnvelope.OUT_OF_RANGE||envelope.status==SensorOperatingEnvelope.HEATER_PROTOCOL_INVALID){rejected++;continue;}
                if(envelope.status==SensorOperatingEnvelope.PRIMARY_TARGET){accepted.add(new WeightedValue(Math.log10(ppm),1.10));primary++;evidencePoints+=75.0;}
                else if(envelope.status==SensorOperatingEnvelope.MANUFACTURER_GRAPH_RESPONSE){accepted.add(new WeightedValue(Math.log10(ppm),0.35));manufacturerCross++;evidencePoints+=55.0;}
                else{accepted.add(new WeightedValue(Math.log10(ppm),0.15));communityCross++;evidencePoints+=35.0;}
                if(lc.sensor<usedSensor.length)usedSensor[lc.sensor]=true;
                literatureAccepted++;
            }
        }

        // Tier 3: inherited manufacturer/community power-law equations. A physical MQ channel
        // already represented by a device calibration or accepted peer-reviewed curve is skipped,
        // preventing duplicate source equations from masquerading as independent sensors.
        for(GasEngine.Estimate e:raw){
            if(e.sensor<usedSensor.length&&usedSensor[e.sensor])continue;
            boolean heaterOk=heaterCycleValid==null||e.sensor>=heaterCycleValid.length||heaterCycleValid[e.sensor];
            SensorOperatingEnvelope.Check c=SensorOperatingEnvelope.check(e.sensor,chemical,e.ppm,heaterOk);
            if(c.status==SensorOperatingEnvelope.PRIMARY_TARGET){accepted.add(new WeightedValue(Math.log10(e.ppm),1.0));primary++;evidencePoints+=70.0;}
            else if(c.status==SensorOperatingEnvelope.MANUFACTURER_GRAPH_RESPONSE){accepted.add(new WeightedValue(Math.log10(e.ppm),0.25));manufacturerCross++;evidencePoints+=45.0;}
            else if(c.status==SensorOperatingEnvelope.COMMUNITY_CROSS_RESPONSE){accepted.add(new WeightedValue(Math.log10(e.ppm),0.10));communityCross++;evidencePoints+=25.0;}
            else rejected++;
        }

        // Tier 4: additional MQDataScience/digitized graph fits are low-weight fingerprint evidence only.
        if(ratios!=null){
            for(ResearchCurveRegistry.Curve c:ResearchCurveRegistry.CURVES){
                if(c.chemical!=chemical||c.sensor>=ratios.length)continue;
                if(c.sensor<usedSensor.length&&usedSensor[c.sensor])continue;
                if(GasEngine.hasCurve(c.sensor,chemical))continue;
                double ratio=ratios[c.sensor];if(!Double.isFinite(ratio)||ratio<=0)continue;
                boolean heaterOk=heaterCycleValid==null||c.sensor>=heaterCycleValid.length||heaterCycleValid[c.sensor];
                if((c.sensor==GasEngine.MQ7||c.sensor==GasEngine.MQ9)&&!heaterOk){rejected++;continue;}
                double ppm=c.ppmFromRatio(ratio);if(!Double.isFinite(ppm)||ppm<=0||!c.inDeclaredRange(ppm)){rejected++;continue;}
                accepted.add(new WeightedValue(Math.log10(ppm),0.08));communityCross++;evidencePoints+=20.0;
            }
        }
        if(accepted.isEmpty())return null;

        // Never average regression coefficients from heterogeneous sources. Consensus is taken
        // over source predictions in log10(ppm) space, robustly using an evidence-weighted median.
        double center=weightedMedian(accepted),ppm=Math.pow(10.0,center);
        double agreement;
        if(accepted.size()<2){
            agreement=0.0;
        }else{
            List<WeightedValue> deviations=new ArrayList<>();
            for(WeightedValue v:accepted)deviations.add(new WeightedValue(Math.abs(v.value-center),v.weight));
            double mad=weightedMedian(deviations);
            agreement=clamp100(100.0*Math.exp(-1.8*mad));
        }

        double coverage=clamp100(100.0*Math.min(INSTALLED_SENSOR_COUNT,accepted.size())/INSTALLED_SENSOR_COUNT);
        double evidenceQuality=clamp100(evidencePoints/accepted.size());
        boolean quantitative=calibratedPrimary>0||(calibrated>0&&identityConfirmed);
        double score=clamp100(0.40*agreement+0.25*coverage+0.35*evidenceQuality+(quantitative?5.0:0.0));

        int confidence;
        boolean specialistSingle=accepted.size()==1&&calibratedPrimary==1;
        if(accepted.size()>=3&&score>=70.0)confidence=GasEngine.HIGH;
        else if(accepted.size()>=2&&score>=45.0)confidence=GasEngine.STRONG;
        else if(specialistSingle)confidence=GasEngine.STRONG;
        else confidence=GasEngine.SUSPECTED;

        int risk=-1;
        String note;
        if(calibratedPrimary>0)note="Device-specific reference-gas calibration is active on a manufacturer-target channel; generic equations are secondary evidence.";
        else if(calibrated>0&&identityConfirmed)note="Device-specific cross-response calibration is active and analyte identity is independently confirmed.";
        else if(calibrated>0)note="A calibrated cross-response exists, but analyte identity is not independently confirmed; quantitative safety interpretation is withheld.";
        else if(primary>0)note="Manufacturer-target response is present, but no accepted device-specific reference-gas calibration supports quantitative safety use.";
        else if(manufacturerCross>0)note="Manufacturer graph cross-response only: fingerprint/research evidence, not selective quantitative safety use.";
        else note="Community/MQDataScience graph-fit evidence only: fingerprint comparison; no quantitative safety interpretation.";
        if(literatureAccepted>0)note+=" Peer-reviewed datasheet-graph fit preferred over duplicate lower-tier coefficients on "+literatureAccepted+" channel(s) inside its published domain; this remains external prior evidence, not M4.";
        if(specialistSingle)note+=" Specialist single-sensor evidence: one device-specific reference-gas calibrated manufacturer-target channel is accepted without fabricating inter-sensor agreement.";
        else if(accepted.size()<2)note+=" Single-sensor screening evidence: inter-sensor agreement is not defined and is reported as 0%.";

        return new Result(chemical,ppm,agreement,coverage,evidenceQuality,score,accepted.size(),primary,calibrated,manufacturerCross,communityCross,rejected,accepted.size(),confidence,quantitative,risk,note);
    }

    private static boolean hasResearchCurve(int chemical){for(ResearchCurveRegistry.Curve c:ResearchCurveRegistry.CURVES)if(c.chemical==chemical)return true;return false;}
    private static double weightedMedian(List<WeightedValue> values){if(values.isEmpty())return Double.NaN;List<WeightedValue>s=new ArrayList<>(values);Collections.sort(s,Comparator.comparingDouble(v->v.value));double total=0;for(WeightedValue v:s)total+=v.weight;double half=total*.5,acc=0;for(WeightedValue v:s){acc+=v.weight;if(acc>=half)return v.value;}return s.get(s.size()-1).value;}
    private static double clamp100(double v){return Math.max(0.0,Math.min(100.0,v));}
    private static double defaultCoverage(int total){return clamp100(100.0*Math.min(INSTALLED_SENSOR_COUNT,Math.max(0,total))/INSTALLED_SENSOR_COUNT);}
    private static double defaultEvidence(int primary,int calibrated,int manufacturerCross,int communityCross){if(calibrated>0)return 90.0;if(primary>0)return 70.0;if(manufacturerCross>0)return 45.0;if(communityCross>0)return 25.0;return 0.0;}
    private static double defaultScore(double agreement,int total,int primary,int calibrated,int manufacturerCross,int communityCross,boolean quantitative){return clamp100(0.40*(total>=2?clamp100(agreement):0.0)+0.25*defaultCoverage(total)+0.35*defaultEvidence(primary,calibrated,manufacturerCross,communityCross)+(quantitative?5.0:0.0));}
}
