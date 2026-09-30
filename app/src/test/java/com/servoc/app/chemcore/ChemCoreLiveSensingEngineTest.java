package com.servoc.app.chemcore;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public final class ChemCoreLiveSensingEngineTest {
    @Test public void broadClassesAreOperationalNotChemicalIdentity(){
        Map<String,Double> alcohol=new HashMap<>();alcohol.put("mq3",.13);alcohol.put("mq135",.09);alcohol.put("mq2",.05);
        assertEquals(ChemCoreLiveSensingEngine.BroadClass.ALCOHOL_VOC,ChemCoreLiveSensingEngine.classifyBroad(alcohol).broadClass);
        Map<String,Double> flammable=new HashMap<>();flammable.put("mq2",.11);flammable.put("mq4",.10);flammable.put("mq5",.085);
        assertEquals(ChemCoreLiveSensingEngine.BroadClass.FLAMMABLE_VAPORS,ChemCoreLiveSensingEngine.classifyBroad(flammable).broadClass);
        Map<String,Double> smoke=new HashMap<>();smoke.put("mq7",.13);smoke.put("mq135",.10);
        assertEquals(ChemCoreLiveSensingEngine.BroadClass.COMBUSTION_SMOKE,ChemCoreLiveSensingEngine.classifyBroad(smoke).broadClass);
    }

    @Test public void liveFitGateRequiresStrongSeparatedInsideDomainFit(){
        InverseMolecularSolver.Result top=result(1,.76,1.0,true,"mq2","mq5"),runner=result(2,.24,1.2,true,"mq3","mq135");
        assertTrue(ChemCoreLiveSensingEngine.liveFitGate(List.of(top,runner)).accepted);
        assertFalse(ChemCoreLiveSensingEngine.liveFitGate(List.of(result(1,.54,1.0,true,"mq2","mq5"),result(2,.46,1.0,true,"mq3","mq135"))).accepted);
        assertFalse(ChemCoreLiveSensingEngine.liveFitGate(List.of(result(1,.80,1.0,false,"mq2","mq5"),result(2,.20,1.0,true,"mq3","mq135"))).accepted);
    }

    @Test public void rejectedGateNeverShowsName(){
        ChemCoreLiveSensingEngine.Snapshot out=ChemCoreLiveSensingEngine.applyInferenceResult(broad(),envelope(ChemCoreDecision.Outcome.IDENTIFIED,ChemCoreDecision.EvidenceTier.M3_ARRAY_CONSISTENT,Double.NaN,72),new ChemCoreOpenSetGate.Result(false,2.8,2,.75,"live-fit: rejected"),2000);
        assertEquals(ChemCoreLiveSensingEngine.Phase.UNKNOWN_OR_AMBIGUOUS,out.phase);assertNull(out.chemicalUid);assertEquals("",out.chemicalLabel);assertFalse(out.ppmReportable);
    }

    @Test public void m3ShowsNameAndConfidenceButNotPpm(){
        ChemCoreLiveSensingEngine.Snapshot out=ChemCoreLiveSensingEngine.applyInferenceResult(broad(),envelope(ChemCoreDecision.Outcome.IDENTIFIED,ChemCoreDecision.EvidenceTier.M3_ARRAY_CONSISTENT,Double.NaN,78),accepted(),2000);
        assertEquals(ChemCoreLiveSensingEngine.Phase.IDENTIFIED,out.phase);assertEquals("Ethanol",out.chemicalLabel);assertEquals(78,out.analyticalConfidencePct,1e-9);assertFalse(out.ppmReportable);assertTrue(Double.isNaN(out.ppm));
    }

    @Test public void onlyM4QuantifiedExposesPpm(){
        ChemCoreLiveSensingEngine.Snapshot out=ChemCoreLiveSensingEngine.applyInferenceResult(broad(),envelope(ChemCoreDecision.Outcome.QUANTIFIED,ChemCoreDecision.EvidenceTier.M4_HAZROB_VALIDATED,123.4,91),accepted(),2000);
        assertTrue(out.ppmReportable);assertEquals(123.4,out.ppm,1e-9);assertEquals(91,out.analyticalConfidencePct,1e-9);assertEquals(.70,out.toJson().optDouble("posterior"),1e-9);
    }

    private static ChemCoreOpenSetGate.Result accepted(){return new ChemCoreOpenSetGate.Result(true,.9,2,.75,"live-fit: accepted");}
    private static ChemCoreLiveSensingEngine.Snapshot broad(){return new ChemCoreLiveSensingEngine.Snapshot(ChemCoreLiveSensingEngine.Phase.BROAD_HAZARD_ALERT,ChemCoreLiveSensingEngine.Severity.WARNING,ChemCoreLiveSensingEngine.BroadClass.ALCOHOL_VOC,80,3,1000,1000,0,null,"",Double.NaN,Double.NaN,Double.NaN,false,false,"","broad");}
    private static ChemCoreResultEnvelope envelope(ChemCoreDecision.Outcome o,ChemCoreDecision.EvidenceTier t,double ppm,double confidence){
        ChemCoreResultEnvelope.CandidateSummary c=new ChemCoreResultEnvelope.CandidateSummary(1,"Ethanol",.70,100,.8,2,t,75,85,85,t==ChemCoreDecision.EvidenceTier.M4_HAZROB_VALIDATED?2:0,true,new HashMap<>(),confidence,2,2,new ArrayList<>());
        return new ChemCoreResultEnvelope("LIVE",o,t,1L,"Ethanol",ppm,Double.NaN,Double.NaN,.8,.70,.3,"hazrob-chemcore-chamber-v2",ChemCoreFeaturePipeline.SCHEMA_VERSION,"test","",List.of(c));
    }
    private static InverseMolecularSolver.Result result(long uid,double posterior,double rms,boolean inside,String... sensors){
        List<ChemCoreEvidenceObservation> e=new ArrayList<>();int i=0;for(String s:sensors)e.add(new ChemCoreEvidenceObservation(ChemCoreEvidenceObservation.Type.LITERATURE_CURVE,uid,"X",List.of(s),"ref","m"+(++i),"src"+i,"g"+i,ChemCoreDecision.EvidenceTier.M2_LITERATURE_OR_MANUFACTURER_SUPPORTED,inside,true,false,100,1,0,0,0,""));
        InverseMolecularSolver.Result r=new InverseMolecularSolver.Result(uid,"X",true,100,rms,0,sensors.length,0,sensors.length,0,Double.NaN,Double.NaN,inside,ChemCoreDecision.EvidenceTier.M2_LITERATURE_OR_MANUFACTURER_SUPPORTED,new HashMap<>(),e,"");r.posterior=posterior;return r;
    }
}
