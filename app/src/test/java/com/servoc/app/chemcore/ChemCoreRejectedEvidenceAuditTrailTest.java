package com.servoc.app.chemcore;

import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.*;

public class ChemCoreRejectedEvidenceAuditTrailTest {
    @Test public void recordsUnsolvedSingleSensorAndOutOfDomainReasons(){
        InverseMolecularSolver.Result unsolved=new InverseMolecularSolver.Result(1,"A",false,Double.NaN,Double.NaN,
                Double.NEGATIVE_INFINITY,0,0,0,0,Double.NaN,Double.NaN,false,
                ChemCoreDecision.EvidenceTier.M0_CATALOG_ONLY,new HashMap<>(),Collections.emptyList(),
                "insufficient overlapping sensor evidence");
        InverseMolecularSolver.Result single=new InverseMolecularSolver.Result(2,"B",true,100,0.5,-0.1,
                1,0,1,0,Double.NaN,Double.NaN,true,
                ChemCoreDecision.EvidenceTier.M2_LITERATURE_OR_MANUFACTURER_SUPPORTED,new HashMap<>(),Collections.emptyList(),"ok");
        InverseMolecularSolver.Result outside=new InverseMolecularSolver.Result(3,"C",true,100,1.2,-0.7,
                2,0,2,0,Double.NaN,Double.NaN,false,
                ChemCoreDecision.EvidenceTier.M2_LITERATURE_OR_MANUFACTURER_SUPPORTED,new HashMap<>(),Collections.emptyList(),"outside");
        ChemCoreRejectedEvidenceAuditTrail.Result r=ChemCoreRejectedEvidenceAuditTrail.collect(
                List.of(unsolved,single,outside),null,null,Collections.emptyList());
        assertTrue(has(r,ChemCoreRejectedEvidenceAuditTrail.Reason.INSUFFICIENT_CONTRIBUTORS));
        assertTrue(has(r,ChemCoreRejectedEvidenceAuditTrail.Reason.SINGLE_SENSOR_SCREENING_ONLY));
        assertTrue(has(r,ChemCoreRejectedEvidenceAuditTrail.Reason.OUT_OF_VALIDATED_DOMAIN));
    }

    @Test public void operationalPreSolverRejectionsArePreserved(){
        List<ChemCoreRejectedEvidenceAuditTrail.Row> explicit=List.of(
                ChemCoreRejectedEvidenceAuditTrail.heaterInvalid(20L,"CO","mq7","heater phase unavailable"),
                ChemCoreRejectedEvidenceAuditTrail.unsupportedHardware(4L,"H2S","mq136","sensor not installed"));
        ChemCoreRejectedEvidenceAuditTrail.Result r=ChemCoreRejectedEvidenceAuditTrail.collect(
                Collections.emptyList(),null,null,explicit);
        assertTrue(has(r,ChemCoreRejectedEvidenceAuditTrail.Reason.HEATER_PROTOCOL_INVALID));
        assertTrue(has(r,ChemCoreRejectedEvidenceAuditTrail.Reason.UNSUPPORTED_HARDWARE));
    }

    @Test public void openSetRejectionRetainsTopHypothesisForAudit(){
        InverseMolecularSolver.Result top=result(76,"Hydrogen",4,4,0,true,Collections.emptyList());
        top.posterior=0.8;
        ChemCoreOpenSetGate.Result gate=new ChemCoreOpenSetGate.Result(false,4.2,3.0,1.0,"outside trained feature domain");
        ChemCoreRejectedEvidenceAuditTrail.Result r=ChemCoreRejectedEvidenceAuditTrail.collect(List.of(top),null,gate,Collections.emptyList());
        ChemCoreRejectedEvidenceAuditTrail.Row row=find(r,ChemCoreRejectedEvidenceAuditTrail.Reason.OPEN_SET_OOD);
        assertNotNull(row);assertEquals(Long.valueOf(76),row.chemicalUid);assertEquals("Hydrogen",row.chemicalLabel);
    }

    @Test public void specialistConflictIsPreservedWithoutCallingItNegativeProof(){
        ChemCoreEvidenceObservation h2Anchor=specialistEvidence(76,"Hydrogen","mq8",0.2);
        ChemCoreEvidenceObservation ethanolAnchor=specialistEvidence(104,"Ethanol","mq3",0.1);
        InverseMolecularSolver.Result hydrogen=result(76,"Hydrogen",4,4,0,true,List.of(h2Anchor));hydrogen.posterior=0.7;
        InverseMolecularSolver.Result ethanol=result(104,"Ethanol",4,4,0,true,List.of(ethanolAnchor));ethanol.posterior=0.3;
        ChemCoreSpecialistConflictEngine.Result specialist=ChemCoreSpecialistConflictEngine.evaluate(List.of(hydrogen,ethanol));
        assertTrue(specialist.holdNamedIdentity);
        ChemCoreRejectedEvidenceAuditTrail.Result trail=ChemCoreRejectedEvidenceAuditTrail.collect(
                List.of(hydrogen,ethanol),specialist,null,Collections.emptyList());
        ChemCoreRejectedEvidenceAuditTrail.Row row=find(trail,ChemCoreRejectedEvidenceAuditTrail.Reason.SPECIALIST_CONFLICT);
        assertNotNull(row);assertEquals(ChemCoreRejectedEvidenceAuditTrail.Stage.SPECIALIST_GATE,row.stage);
        assertTrue(row.note.toLowerCase().contains("specialist"));
    }

    @Test public void liveNamedHypothesisThatDeepAnalysisWithholdsIsRetained(){
        ChemCoreLiveSensingEngine.Snapshot live=new ChemCoreLiveSensingEngine.Snapshot(
                ChemCoreLiveSensingEngine.Phase.IDENTIFIED,ChemCoreLiveSensingEngine.Severity.WARNING,
                ChemCoreLiveSensingEngine.BroadClass.REDUCING_GAS,80,3,1,2,1,76L,"Hydrogen",
                55,0.70,Double.NaN,false,true,"live gate","live identity");
        ChemCoreResultEnvelope completed=new ChemCoreResultEnvelope("S",ChemCoreDecision.Outcome.UNKNOWN,
                ChemCoreDecision.EvidenceTier.M0_CATALOG_ONLY,null,"",Double.NaN,Double.NaN,Double.NaN,4.2,
                0.70,Double.NaN,"p","f","m","open-set rejection",Collections.emptyList());
        ChemCoreRejectedEvidenceAuditTrail.Row row=ChemCoreRejectedEvidenceAuditTrail.liveHypothesisNotConfirmed(live,completed);
        assertNotNull(row);assertEquals(ChemCoreRejectedEvidenceAuditTrail.Reason.OPEN_SET_OOD,row.reason);
        assertEquals(Long.valueOf(76),row.chemicalUid);assertTrue(row.note.contains("not confirmed"));
    }

    private static ChemCoreEvidenceObservation specialistEvidence(long uid,String label,String sensor,double z){
        return new ChemCoreEvidenceObservation(ChemCoreEvidenceObservation.Type.MANUFACTURER_CURVE,uid,label,List.of(sensor),
                "specialist","m","source:"+label,"spec:"+label,
                ChemCoreDecision.EvidenceTier.M2_LITERATURE_OR_MANUFACTURER_SUPPORTED,true,true,false,
                100,1,0,0,z,"specialist evidence");
    }

    private static InverseMolecularSolver.Result result(long uid,String label,int contributors,int refs,int cal,boolean inside,
                                                         List<ChemCoreEvidenceObservation> evidence){
        return new InverseMolecularSolver.Result(uid,label,true,100,0.8,-0.32,contributors,0,refs,cal,
                Double.NaN,Double.NaN,inside,ChemCoreDecision.EvidenceTier.M2_LITERATURE_OR_MANUFACTURER_SUPPORTED,
                new HashMap<>(),evidence,"test");
    }
    private static boolean has(ChemCoreRejectedEvidenceAuditTrail.Result r,ChemCoreRejectedEvidenceAuditTrail.Reason reason){return find(r,reason)!=null;}
    private static ChemCoreRejectedEvidenceAuditTrail.Row find(ChemCoreRejectedEvidenceAuditTrail.Result r,ChemCoreRejectedEvidenceAuditTrail.Reason reason){
        for(ChemCoreRejectedEvidenceAuditTrail.Row x:r.rows)if(x.reason==reason)return x;return null;
    }
}
