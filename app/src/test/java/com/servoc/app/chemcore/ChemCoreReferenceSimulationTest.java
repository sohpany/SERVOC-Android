package com.servoc.app.chemcore;

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
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import static org.junit.Assert.assertTrue;

/**
 * Informational, deterministic challenge simulation for the ChemCore analytical engine.
 *
 * Ground-truth samples are generated from ResearchCurveRegistry. The production inference path is
 * then run with ChemCoreLegacyCandidateRegistry.acceptedReferenceCandidates(). Where the accepted
 * inference model selects literature/manufacturer evidence instead of the research generator, the
 * sample is counted as cross-source. Where it falls back to the same research relation, the result
 * is explicitly labelled self-source and is NOT presented as independent accuracy evidence.
 *
 * This test has no minimum-accuracy assertion: poor recognition must remain visible rather than be
 * tuned away to make CI green. CI only requires that the simulation executes and writes an audit.
 */
public final class ChemCoreReferenceSimulationTest {
    private static final long RANDOM_SEED=20261002L;
    private static final double[] PPM_GRID={20,50,100,200,500,1000,3000,7000};
    private static final double[] NOISE_LOG10={0.0,0.05,0.10,0.15};
    private static final int NOISY_REPLICATES=12;
    private static final String[] SENSOR_KEYS={"mq2","mq3","mq4","mq5","mq7","mq8","mq9","mq135"};

    private static final class Agg {
        int samples,top1,top3,trueSolved,unknown;
        double absLogSum,absPctSum,biasLogSum,residualSum,rankSum;
        int concentrationN,residualN,rankN;
        void add(boolean hit1,boolean hit3,boolean isUnknown,int trueRank,Double estPpm,double truePpm,Double residual){
            samples++;if(hit1)top1++;if(hit3)top3++;if(isUnknown)unknown++;
            if(trueRank>0){rankSum+=trueRank;rankN++;trueSolved++;}
            if(estPpm!=null&&Double.isFinite(estPpm)&&estPpm>0){
                double e=Math.log10(estPpm/truePpm);absLogSum+=Math.abs(e);biasLogSum+=e;
                absPctSum+=Math.abs(estPpm-truePpm)/truePpm*100.0;concentrationN++;
            }
            if(residual!=null&&Double.isFinite(residual)){residualSum+=residual;residualN++;}
        }
        JSONObject json(){
            JSONObject x=new JSONObject();
            x.put("samples",samples);
            x.put("top1_correct",top1);x.put("top3_correct",top3);x.put("unknown_or_no_top",unknown);
            x.put("top1_accuracy_pct",pct(top1,samples));x.put("top3_accuracy_pct",pct(top3,samples));
            x.put("true_candidate_solved_pct",pct(trueSolved,samples));
            x.put("mean_true_rank",rankN>0?rankSum/rankN:JSONObject.NULL);
            x.put("mean_abs_log10_ppm_error",concentrationN>0?absLogSum/concentrationN:JSONObject.NULL);
            x.put("geometric_mean_fold_error",concentrationN>0?Math.pow(10.0,absLogSum/concentrationN):JSONObject.NULL);
            x.put("mean_abs_relative_ppm_error_pct",concentrationN>0?absPctSum/concentrationN:JSONObject.NULL);
            x.put("mean_log10_ppm_bias",concentrationN>0?biasLogSum/concentrationN:JSONObject.NULL);
            x.put("mean_true_candidate_standardized_residual_rms",residualN>0?residualSum/residualN:JSONObject.NULL);
            return x;
        }
    }

    private static final class MaterialAgg {
        final String label;final Agg all=new Agg(),multi=new Agg(),single=new Agg();
        int cross2,partialCross,selfSource;
        MaterialAgg(String label){this.label=label;}
        JSONObject json(){
            JSONObject x=new JSONObject();x.put("chemical",label);x.put("all",all.json());x.put("multi_sensor",multi.json());x.put("single_sensor",single.json());
            x.put("cross_source_2plus_samples",cross2);x.put("partial_cross_source_samples",partialCross);x.put("self_source_samples",selfSource);return x;
        }
    }

    @Test public void generateReferenceChallengeReport() throws Exception {
        SamplingProfile profile=SamplingProfile.hazRobPrototypeV1();
        List<InverseMolecularSolver.Candidate> candidates=ChemCoreLegacyCandidateRegistry.acceptedReferenceCandidates();
        assertTrue("accepted candidate set must not be empty",!candidates.isEmpty());

        Map<Integer,List<ResearchCurveRegistry.Curve>> generatorByChemical=new LinkedHashMap<>();
        for(ResearchCurveRegistry.Curve c:ResearchCurveRegistry.CURVES)
            generatorByChemical.computeIfAbsent(c.chemical,k->new ArrayList<>()).add(c);

        Random random=new Random(RANDOM_SEED);
        Agg multiOverall=new Agg(),singleOverall=new Agg(),allOverall=new Agg();
        Map<String,Agg> byNoise=new LinkedHashMap<>();
        Map<Integer,MaterialAgg> byMaterial=new LinkedHashMap<>();
        JSONArray deterministic=new JSONArray(),failures=new JSONArray();
        int generatedMaterials=0;

        for(Map.Entry<Integer,List<ResearchCurveRegistry.Curve>> entry:generatorByChemical.entrySet()){
            int chemical=entry.getKey();
            InverseMolecularSolver.Candidate truth=findCandidate(candidates,chemical+1L);
            if(truth==null)continue;
            MaterialAgg material=new MaterialAgg(truth.label);byMaterial.put(chemical,material);generatedMaterials++;

            for(double ppm:PPM_GRID){
                List<ResearchCurveRegistry.Curve> active=new ArrayList<>();
                for(ResearchCurveRegistry.Curve c:entry.getValue())if(c.inDeclaredRange(ppm))active.add(c);
                if(active.isEmpty())continue;
                boolean multi=active.size()>=2;
                for(double noise:NOISE_LOG10){
                    int reps=noise==0?1:NOISY_REPLICATES;
                    String noiseKey=String.format(Locale.US,"sigma_log10_%.2f",noise);
                    Agg noiseAgg=byNoise.computeIfAbsent(noiseKey,k->new Agg());
                    for(int rep=0;rep<reps;rep++){
                        Map<String,Double> observed=new LinkedHashMap<>();
                        for(ResearchCurveRegistry.Curve c:active){
                            double ratio=c.ratioFromPpm(ppm);if(!(ratio>0)||!Double.isFinite(ratio))continue;
                            if(noise>0)ratio*=Math.pow(10.0,random.nextGaussian()*noise);
                            observed.put(key(c.sensor),ratio);
                        }
                        if(observed.isEmpty())continue;

                        int crossChannels=crossSourceChannels(truth,active,ppm,profile);
                        String sourceClass=crossChannels>=2?"CROSS_SOURCE_2PLUS":crossChannels==1?"PARTIAL_CROSS_SOURCE":"SELF_SOURCE";
                        if(crossChannels>=2)material.cross2++;else if(crossChannels==1)material.partialCross++;else material.selfSource++;

                        ChemCoreInferenceEngine.Context ctx=new ChemCoreInferenceEngine.Context(
                                "sim-"+chemical+"-"+(long)ppm+"-"+noiseKey+"-"+rep,
                                profile.profileId,"reference-simulation/1",25.0,50.0,null,true);
                        ChemCoreResultEnvelope result=ChemCoreInferenceEngine.analyze(observed,candidates,profile,ctx);

                        long trueUid=chemical+1L;
                        boolean hit1=result.chemicalUid!=null&&result.chemicalUid==trueUid;
                        int rank=rankOf(result.candidates,trueUid);
                        boolean hit3=rank>0&&rank<=3;
                        ChemCoreResultEnvelope.CandidateSummary trueSummary=findSummary(result.candidates,trueUid);
                        Double est=trueSummary==null?null:trueSummary.bestPpm;
                        Double residual=trueSummary==null?null:trueSummary.residualRms;
                        boolean noTop=result.chemicalUid==null;

                        allOverall.add(hit1,hit3,noTop,rank,est,ppm,residual);
                        material.all.add(hit1,hit3,noTop,rank,est,ppm,residual);
                        noiseAgg.add(hit1,hit3,noTop,rank,est,ppm,residual);
                        if(multi){multiOverall.add(hit1,hit3,noTop,rank,est,ppm,residual);material.multi.add(hit1,hit3,noTop,rank,est,ppm,residual);}
                        else{singleOverall.add(hit1,hit3,noTop,rank,est,ppm,residual);material.single.add(hit1,hit3,noTop,rank,est,ppm,residual);}

                        JSONObject row=sampleJson(truth.label,ppm,noise,rep,observed.size(),sourceClass,crossChannels,result,rank,trueSummary);
                        if(noise==0)deterministic.put(row);
                        if(!hit1&&failures.length()<80)failures.put(row);
                    }
                }
            }
        }

        JSONObject root=new JSONObject();
        root.put("schema","chemcore-reference-challenge/1");root.put("random_seed",RANDOM_SEED);
        root.put("generator","ResearchCurveRegistry datasheet-derived/community comparison relations");
        root.put("inference","ChemCoreInferenceEngine + accepted-reference candidate registry");
        root.put("temperature_c",25.0);root.put("rh_pct",50.0);
        root.put("ranking_semantics",InverseMolecularSolver.RANKING_SEMANTICS);
        root.put("scientific_scope","simulation/model-challenge only; not real-world HR069 accuracy and not M4 calibration");
        root.put("headline_accuracy_scope","multi-sensor generated samples only; single-sensor cases reported separately");
        root.put("generated_material_count",generatedMaterials);
        root.put("overall_multi_sensor",multiOverall.json());root.put("overall_single_sensor",singleOverall.json());root.put("overall_all",allOverall.json());
        JSONObject noiseJson=new JSONObject();for(Map.Entry<String,Agg> e:byNoise.entrySet())noiseJson.put(e.getKey(),e.getValue().json());root.put("by_noise",noiseJson);
        JSONArray mats=new JSONArray();for(MaterialAgg a:byMaterial.values())mats.put(a.json());root.put("by_material",mats);
        root.put("deterministic_examples",deterministic);root.put("first_top1_failures",failures);

        File build=new File("build");if(!build.isDirectory()&&!build.mkdirs())throw new IllegalStateException("cannot create build directory");
        File report=new File(build,"chemcore-reference-simulation.json");
        Files.write(report.toPath(),root.toString(2).getBytes(StandardCharsets.UTF_8));
        System.out.println("CHEMCORE_REFERENCE_SIMULATION_REPORT="+report.getAbsolutePath());
        System.out.println("CHEMCORE_REFERENCE_SIMULATION_SUMMARY="+root.getJSONObject("overall_multi_sensor").toString());

        assertTrue("simulation must generate multi-sensor cases",multiOverall.samples>0);
        assertTrue("simulation must generate multiple materials",generatedMaterials>=5);
    }

    private static JSONObject sampleJson(String truth,double ppm,double noise,int rep,int observedChannels,String sourceClass,int crossChannels,
                                         ChemCoreResultEnvelope result,int trueRank,ChemCoreResultEnvelope.CandidateSummary trueSummary){
        JSONObject x=new JSONObject();x.put("truth",truth);x.put("true_ppm",ppm);x.put("noise_sigma_log10",noise);x.put("replicate",rep);
        x.put("observed_channels",observedChannels);x.put("source_class",sourceClass);x.put("cross_source_channels",crossChannels);
        x.put("outcome",result.outcome.name());x.put("predicted_top",result.chemicalLabel);x.put("top_relative_weight",finiteOrNull(result.topPosterior));x.put("true_rank",trueRank>0?trueRank:JSONObject.NULL);
        if(trueSummary!=null){x.put("true_candidate_fit_ppm",finiteOrNull(trueSummary.bestPpm));x.put("true_candidate_residual_rms",finiteOrNull(trueSummary.residualRms));x.put("true_candidate_relative_weight",finiteOrNull(trueSummary.posterior));}
        JSONArray top=new JSONArray();for(int i=0;i<Math.min(3,result.candidates.size());i++){ChemCoreResultEnvelope.CandidateSummary c=result.candidates.get(i);JSONObject q=new JSONObject();q.put("rank",i+1);q.put("label",c.label);q.put("relative_weight",finiteOrNull(c.posterior));q.put("fit_ppm",finiteOrNull(c.bestPpm));q.put("residual_rms",finiteOrNull(c.residualRms));q.put("contributors",c.contributors);top.put(q);}x.put("top3",top);return x;
    }

    private static int crossSourceChannels(InverseMolecularSolver.Candidate truth,List<ResearchCurveRegistry.Curve> active,double ppm,SamplingProfile profile){
        int n=0;for(ResearchCurveRegistry.Curve c:active){String sensor=key(c.sensor);ChemCoreResponseModel m=findModel(truth.models,sensor);if(m==null)continue;
            ChemCoreResponseModel.Estimate e=m.predict(ppm,25.0,50.0,profile);if(e==null||!e.available)continue;
            if(e.evidenceType!=ChemCoreEvidenceObservation.Type.RESEARCH_CURVE)n++;}
        return n;
    }

    private static ChemCoreResponseModel findModel(List<ChemCoreResponseModel> models,String key){for(ChemCoreResponseModel m:models)if(m!=null&&key.equalsIgnoreCase(m.sensorKey()))return m;return null;}
    private static InverseMolecularSolver.Candidate findCandidate(List<InverseMolecularSolver.Candidate> rows,long uid){for(InverseMolecularSolver.Candidate c:rows)if(c.chemicalUid==uid)return c;return null;}
    private static ChemCoreResultEnvelope.CandidateSummary findSummary(List<ChemCoreResultEnvelope.CandidateSummary> rows,long uid){for(ChemCoreResultEnvelope.CandidateSummary c:rows)if(c.chemicalUid==uid)return c;return null;}
    private static int rankOf(List<ChemCoreResultEnvelope.CandidateSummary> rows,long uid){for(int i=0;i<rows.size();i++)if(rows.get(i).chemicalUid==uid)return i+1;return -1;}
    private static Object finiteOrNull(double v){return Double.isFinite(v)?v:JSONObject.NULL;}
    private static double pct(int n,int d){return d>0?100.0*n/d:Double.NaN;}
    private static String key(int sensor){return sensor>=0&&sensor<SENSOR_KEYS.length?SENSOR_KEYS[sensor]:"mq?";}
}
