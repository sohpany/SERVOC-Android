package com.servoc.app.chemcore;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Exact replay of HazRob Experiment 4 (2026-10-01) using the eight reported Rs/R0 values. */
public final class ChemCoreExperiment4ReplayTest {
    @Test public void replayExactRobotRatiosThroughCurrentEngine() throws Exception {
        Map<String,Double> observed=new LinkedHashMap<>();
        observed.put("mq2",0.5503083626889448);
        observed.put("mq4",0.4049359098145372);
        observed.put("mq5",0.7425364888978386);
        observed.put("mq8",0.14234264178542308);
        observed.put("mq3",0.2491536317839980);
        observed.put("mq7",0.27015339198389815);
        observed.put("mq9",0.4790678565399657);
        observed.put("mq135",0.27699898696320846);
        assertEquals(8,observed.size());

        SamplingProfile profile=SamplingProfile.hazRobPrototypeV1();
        ChemCoreInferenceEngine.Context context=new ChemCoreInferenceEngine.Context(
                "experiment-4-20261001-replay",
                profile.profileId,
                "chemcore-current-experiment4-replay/1",
                Double.NaN,Double.NaN,
                null,
                true);

        ChemCoreResultEnvelope result=ChemCoreInferenceEngine.analyzeLegacyManufacturerPriors(observed,profile,context);
        assertTrue("replay must return analytical candidates",!result.candidates.isEmpty());

        JSONObject root=new JSONObject();
        root.put("schema","chemcore-experiment4-replay/1");
        root.put("ground_truth_revealed_after_original_blind_test","Ethanol 96% liquid challenge with tobacco-smoke interference");
        root.put("important_ground_truth_note","96% is liquid concentration, not traceable gas-phase ppm");
        root.put("temperature_c",JSONObject.NULL);
        root.put("rh_pct",JSONObject.NULL);
        root.put("open_set_state","UNAVAILABLE_IN_ORIGINAL_REPORT");
        root.put("time_series_state","UNAVAILABLE_IN_ORIGINAL_REPORT");
        JSONObject ratios=new JSONObject();for(Map.Entry<String,Double> e:observed.entrySet())ratios.put(e.getKey(),e.getValue());root.put("observed_rs_r0",ratios);

        JSONObject old=new JSONObject();
        old.put("engine","mq-fingerprint-v2-hr042");
        old.put("top_candidate","Methane");
        old.put("ethanol_rank",5);
        old.put("old_candidate_order",new JSONArray().put("Methane").put("Hydrogen").put("Propane").put("LPG").put("Ethanol").put("Benzene"));
        root.put("old_report_baseline",old);

        root.put("current_outcome",result.outcome.name());
        root.put("current_evidence_tier",result.evidenceTier.name());
        root.put("current_top_candidate",result.chemicalLabel);
        root.put("current_top_relative_weight",finiteOrNull(result.topPosterior));
        root.put("current_note",result.note);
        root.put("ranking_semantics",InverseMolecularSolver.RANKING_SEMANTICS);
        root.put("reportable_ppm",result.toJson().optBoolean("ppm_reportable",false));

        JSONArray rows=new JSONArray();int ethanolRank=-1,hydrogenRank=-1,methaneRank=-1;
        for(int i=0;i<result.candidates.size();i++){
            ChemCoreResultEnvelope.CandidateSummary c=result.candidates.get(i);
            JSONObject x=new JSONObject();x.put("rank",i+1);x.put("chemical_uid",c.chemicalUid);x.put("label",c.label);
            x.put("relative_weight",finiteOrNull(c.posterior));x.put("fit_ppm_screening",finiteOrNull(c.bestPpm));
            x.put("residual_rms",finiteOrNull(c.residualRms));x.put("contributors",c.contributors);
            x.put("evidence_tier",c.tier==null?JSONObject.NULL:c.tier.name());x.put("coverage_pct",finiteOrNull(c.coveragePct));
            x.put("agreement_pct",finiteOrNull(c.agreementPct));x.put("analytical_confidence_pct",finiteOrNull(c.analyticalConfidencePct));
            JSONObject residuals=new JSONObject();for(Map.Entry<String,Double> r:c.residualLog10BySensor.entrySet())if(Double.isFinite(r.getValue()))residuals.put(r.getKey(),r.getValue());x.put("residual_log10_by_sensor",residuals);
            rows.put(x);
            if("Ethanol".equalsIgnoreCase(c.label))ethanolRank=i+1;
            if("Hydrogen".equalsIgnoreCase(c.label))hydrogenRank=i+1;
            if("Methane".equalsIgnoreCase(c.label))methaneRank=i+1;
        }
        root.put("candidates",rows);
        root.put("ethanol_rank_current",ethanolRank>0?ethanolRank:JSONObject.NULL);
        root.put("hydrogen_rank_current",hydrogenRank>0?hydrogenRank:JSONObject.NULL);
        root.put("methane_rank_current",methaneRank>0?methaneRank:JSONObject.NULL);
        root.put("ethanol_rank_improvement_vs_old",ethanolRank>0?5-ethanolRank:JSONObject.NULL);
        root.put("identity_accuracy_interpretation","This single challenge can show ranking improvement/degradation against revealed ground truth, but cannot establish population accuracy.");

        File build=new File("build");if(!build.isDirectory()&&!build.mkdirs())throw new IllegalStateException("cannot create build directory");
        File report=new File(build,"chemcore-experiment4-replay.json");
        Files.write(report.toPath(),root.toString(2).getBytes(StandardCharsets.UTF_8));
        System.out.println("CHEMCORE_EXPERIMENT4_REPLAY_REPORT="+report.getAbsolutePath());
        System.out.println("CHEMCORE_EXPERIMENT4_REPLAY_SUMMARY="+new JSONObject()
                .put("outcome",result.outcome.name()).put("top",result.chemicalLabel)
                .put("ethanol_rank",ethanolRank>0?ethanolRank:JSONObject.NULL)
                .put("hydrogen_rank",hydrogenRank>0?hydrogenRank:JSONObject.NULL)
                .put("methane_rank",methaneRank>0?methaneRank:JSONObject.NULL).toString());
    }

    private static Object finiteOrNull(double v){return Double.isFinite(v)?v:JSONObject.NULL;}
}
