package com.servoc.app.chemcore;

import com.servoc.app.chemical.ChemicalCatalog;
import com.servoc.app.chemical.GasEngine;
import com.servoc.app.chemical.ResearchCurveRegistry;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Pilot validation for the first HazRob virtual MQ sensor: MQ3.
 *
 * Reference challenge rows come from the independent ResearchCurveRegistry view. The simulator
 * itself uses AcceptedReferenceResponseModel source precedence. Therefore rows where AcceptedReference
 * resolves to manufacturer/direct evidence are cross-source comparisons; rows that fall back to the
 * same research relation are explicitly labeled SELF_SOURCE and count only as consistency checks.
 */
public final class Mq3VirtualSensorPilotTest {
    private static final double NOISE_SIGMA_LOG10=0.05;
    private static final double DRIFT_LOG10=0.02;

    private static final class Agg {
        int n,cross,self;
        double absLogRatio,absLogPpm,absPctPpm;
        void add(boolean isCross,double ratioErr,double ppmErrLog,double ppmErrPct){
            n++;if(isCross)cross++;else self++;absLogRatio+=Math.abs(ratioErr);absLogPpm+=Math.abs(ppmErrLog);absPctPpm+=Math.abs(ppmErrPct);
        }
        JSONObject json() throws Exception {
            JSONObject x=new JSONObject();x.put("samples",n);x.put("cross_source_samples",cross);x.put("self_source_samples",self);
            x.put("mean_abs_log10_ratio_error",n>0?absLogRatio/n:JSONObject.NULL);
            x.put("geometric_mean_ratio_fold_error",n>0?Math.pow(10.0,absLogRatio/n):JSONObject.NULL);
            x.put("mean_abs_log10_inverse_ppm_error",n>0?absLogPpm/n:JSONObject.NULL);
            x.put("geometric_mean_inverse_ppm_fold_error",n>0?Math.pow(10.0,absLogPpm/n):JSONObject.NULL);
            x.put("mean_abs_inverse_ppm_error_pct",n>0?absPctPpm/n:JSONObject.NULL);return x;
        }
    }

    @Test public void generateMq3DigitalTwinPilotReport() throws Exception {
        List<ResearchCurveRegistry.Curve> mq3=new ArrayList<>();
        for(ResearchCurveRegistry.Curve c:ResearchCurveRegistry.CURVES)if(c.sensor==GasEngine.MQ3)mq3.add(c);
        assertTrue("MQ3 research reference set required",mq3.size()>=5);

        JSONArray rows=new JSONArray();Map<Integer,Agg> byChemical=new LinkedHashMap<>();Agg overall=new Agg();
        int electricalChecks=0,noiseChecks=0;

        for(ResearchCurveRegistry.Curve ref:mq3){
            if(!AcceptedReferenceResponseModel.eligible(GasEngine.MQ3,ref.chemical))continue;
            AcceptedReferenceResponseModel accepted=new AcceptedReferenceResponseModel(GasEngine.MQ3,ref.chemical);
            double lo=Math.max(ref.minPpm,accepted.minPpm()),hi=Math.min(ref.maxPpm,accepted.maxPpm());
            if(!(lo>0)||!(hi>lo))continue;
            double[] points={logPoint(lo,hi,0.35),logPoint(lo,hi,0.70)};
            for(int j=0;j<points.length;j++){
                double ppm=points[j];
                MqVirtualSensorSimulator.Request clean=new MqVirtualSensorSimulator.Request(
                        GasEngine.MQ3,ref.chemical,ppm,25.0,50.0,20.0,10.0,5.0,4.096,32767,0.0,0.0,0.0);
                MqVirtualSensorSimulator.Result sim=MqVirtualSensorSimulator.simulate(clean);
                assertTrue("MQ3 simulator must return an accepted reference response for "+ref.gasLabel,sim.available);

                double referenceRatio=ref.ratioFromPpm(ppm);
                double ratioErr=Math.log10(sim.ratio/referenceRatio);
                double inversePpm=ref.ppmFromRatio(sim.ratio);
                double ppmErrLog=Math.log10(inversePpm/ppm);
                double ppmErrPct=(inversePpm-ppm)/ppm*100.0;
                boolean cross=sim.evidenceType!=ChemCoreEvidenceObservation.Type.RESEARCH_CURVE;
                overall.add(cross,ratioErr,ppmErrLog,ppmErrPct);
                byChemical.computeIfAbsent(ref.chemical,k->new Agg()).add(cross,ratioErr,ppmErrLog,ppmErrPct);

                // Electrical forward/inverse consistency of the ideal divider model.
                double reconstructedRs=clean.loadResistanceKohm*(clean.supplyV/sim.vOut-1.0);
                double reconstructedRatio=reconstructedRs/clean.r0Kohm;
                assertEquals(sim.ratio,reconstructedRatio,Math.max(1e-9,sim.ratio*1e-9));electricalChecks++;

                // Deterministic perturbation: +1 sigma noise plus a small systematic drift.
                MqVirtualSensorSimulator.Request perturbed=new MqVirtualSensorSimulator.Request(
                        GasEngine.MQ3,ref.chemical,ppm,25.0,50.0,20.0,10.0,5.0,4.096,32767,
                        DRIFT_LOG10,NOISE_SIGMA_LOG10,1.0);
                MqVirtualSensorSimulator.Result noisy=MqVirtualSensorSimulator.simulate(perturbed);
                assertTrue(noisy.available);
                assertEquals(DRIFT_LOG10+NOISE_SIGMA_LOG10,
                        noisy.simulatedLog10Ratio-noisy.meanLog10Ratio,1e-10);noiseChecks++;

                JSONObject r=new JSONObject();r.put("chemical_id",ref.chemical);r.put("chemical",ChemicalCatalog.NAMES[ref.chemical]);
                r.put("reference_label",ref.gasLabel);r.put("ppm",ppm);r.put("source_class",cross?"CROSS_SOURCE":"SELF_SOURCE");
                r.put("simulator_evidence_type",sim.evidenceType==null?JSONObject.NULL:sim.evidenceType.name());r.put("simulator_source_id",sim.sourceId);
                r.put("reference_ratio",referenceRatio);r.put("simulated_ratio",sim.ratio);r.put("ratio_log10_error",ratioErr);
                r.put("reference_inverse_ppm_from_simulated_ratio",inversePpm);r.put("inverse_ppm_log10_error",ppmErrLog);r.put("inverse_ppm_error_pct",ppmErrPct);
                r.put("source_sigma_log10",sim.sourceSigmaLog10);r.put("rs_kohm",sim.rsKohm);r.put("vout_v",sim.vOut);r.put("adc_input_v",sim.adcInputV);r.put("adc_count",sim.adcCount);
                r.put("perturbed_ratio_plus_1sigma_and_drift",noisy.ratio);rows.put(r);
            }
        }

        JSONObject by=new JSONObject();for(Map.Entry<Integer,Agg> e:byChemical.entrySet())by.put(ChemicalCatalog.NAMES[e.getKey()],e.getValue().json());
        JSONObject root=new JSONObject();root.put("schema","hazrob-mq3-digital-twin-pilot/1");root.put("simulator_version",MqVirtualSensorSimulator.VERSION);
        root.put("sensor","MQ3");root.put("scientific_scope","empirical-electrical research simulator; source-backed Rs/R0 forward model; not first-principles SnO2 kinetics and not M4");
        root.put("reference","ResearchCurveRegistry MQ3 relations");root.put("source_policy","cross-source rows are validation-like comparisons; self-source rows are consistency only");
        root.put("noise_test_sigma_log10",NOISE_SIGMA_LOG10);root.put("drift_test_log10",DRIFT_LOG10);root.put("electrical_consistency_checks",electricalChecks);root.put("noise_injection_checks",noiseChecks);
        root.put("overall",overall.json());root.put("by_chemical",by);root.put("samples",rows);

        File build=new File("build");if(!build.isDirectory()&&!build.mkdirs())throw new IllegalStateException("cannot create build directory");
        File report=new File(build,"mq3-digital-twin-pilot.json");Files.write(report.toPath(),root.toString(2).getBytes(StandardCharsets.UTF_8));
        System.out.println("MQ3_DIGITAL_TWIN_PILOT_REPORT="+report.getAbsolutePath());System.out.println("MQ3_DIGITAL_TWIN_PILOT_SUMMARY="+root.getJSONObject("overall").toString());
        assertTrue("pilot must exercise several chemicals",byChemical.size()>=5);assertTrue("pilot must perform electrical checks",electricalChecks>=10);
    }

    private static double logPoint(double lo,double hi,double f){return Math.pow(10.0,Math.log10(lo)+f*(Math.log10(hi)-Math.log10(lo)));}
}
