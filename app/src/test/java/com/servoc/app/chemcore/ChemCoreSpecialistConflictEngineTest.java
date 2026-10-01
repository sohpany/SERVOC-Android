package com.servoc.app.chemcore;

import org.junit.Test;

import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.*;

public class ChemCoreSpecialistConflictEngineTest {
    @Test public void stage7RoleRegistryMatchesPrimaryAndSecondaryMap(){
        assertEquals(ChemCoreSpecialistRoleRegistry.Role.PRIMARY_ANCHOR,ChemCoreSpecialistRoleRegistry.role(20,"mq7"));
        assertEquals(ChemCoreSpecialistRoleRegistry.Role.SECONDARY_TARGET,ChemCoreSpecialistRoleRegistry.role(20,"mq9"));
        assertEquals(ChemCoreSpecialistRoleRegistry.Role.PRIMARY_ANCHOR,ChemCoreSpecialistRoleRegistry.role(75,"mq8"));
        assertEquals(ChemCoreSpecialistRoleRegistry.Role.PRIMARY_ANCHOR,ChemCoreSpecialistRoleRegistry.role(76,"mq4"));
        assertEquals(ChemCoreSpecialistRoleRegistry.Role.PRIMARY_ANCHOR,ChemCoreSpecialistRoleRegistry.role(103,"mq3"));
        assertEquals(ChemCoreSpecialistRoleRegistry.Role.SECONDARY_TARGET,ChemCoreSpecialistRoleRegistry.role(139,"mq135"));
        assertEquals(ChemCoreSpecialistRoleRegistry.Role.NONE,ChemCoreSpecialistRoleRegistry.role(103,"mq8"));
    }

    @Test public void hydrogenAndEthanolPrimaryAnchorsHoldNamedIdentity(){
        InverseMolecularSolver.Result hydrogen=result(76,"Hydrogen",0.70,1.16,4,0,"mq8",0.15);
        InverseMolecularSolver.Result ethanol=result(104,"Ethanol",0.04,2.66,4,0,"mq3",0.10);
        ChemCoreSpecialistConflictEngine.Result r=ChemCoreSpecialistConflictEngine.evaluate(List.of(hydrogen,ethanol));
        assertEquals(ChemCoreSpecialistConflictEngine.Verdict.CROSS_SPECIALIST_CONFLICT,r.verdict);
        assertTrue(r.holdNamedIdentity);
        assertNotNull(r.topSupport);assertEquals("mq8",r.topSupport.sensorKey);
        assertNotNull(r.challengerSupport);assertEquals("mq3",r.challengerSupport.sensorKey);
    }

    @Test public void singleChannelExactFitCannotCreateSpecialistVeto(){
        InverseMolecularSolver.Result hydrogen=result(76,"Hydrogen",0.70,1.16,4,0,"mq8",0.15);
        InverseMolecularSolver.Result ethanolSingle=result(104,"Ethanol",0.20,0.00,1,0,"mq3",0.00);
        ChemCoreSpecialistConflictEngine.Result r=ChemCoreSpecialistConflictEngine.evaluate(List.of(hydrogen,ethanolSingle));
        assertEquals(ChemCoreSpecialistConflictEngine.Verdict.TOP_SPECIALIST_SUPPORTED,r.verdict);
        assertFalse(r.holdNamedIdentity);
    }

    @Test public void strongSpecialistChallengerHoldsUnanchoredTop(){
        InverseMolecularSolver.Result lpg=result(136,"LPG",0.60,1.0,4,0,"mq2",0.2);
        InverseMolecularSolver.Result hydrogen=result(76,"Hydrogen",0.30,1.2,4,0,"mq8",0.3);
        ChemCoreSpecialistConflictEngine.Result r=ChemCoreSpecialistConflictEngine.evaluate(List.of(lpg,hydrogen));
        assertEquals(ChemCoreSpecialistConflictEngine.Verdict.SPECIALIST_CHALLENGER,r.verdict);
        assertTrue(r.holdNamedIdentity);assertNull(r.topSupport);assertEquals("Hydrogen",r.challengerSupport.label);
    }

    @Test public void compatibleTopAnchorWithoutChallengerDoesNotHold(){
        InverseMolecularSolver.Result methane=result(77,"Methane",0.80,1.0,4,0,"mq4",0.4);
        InverseMolecularSolver.Result propane=result(79,"Propane",0.20,1.1,2,0,"mq2",0.2);
        ChemCoreSpecialistConflictEngine.Result r=ChemCoreSpecialistConflictEngine.evaluate(List.of(methane,propane));
        assertEquals(ChemCoreSpecialistConflictEngine.Verdict.TOP_SPECIALIST_SUPPORTED,r.verdict);
        assertFalse(r.holdNamedIdentity);
    }

    @Test public void singleSensorM4SpecialistMayParticipate(){
        InverseMolecularSolver.Result ammonia=result(22,"Ammonia",0.55,0.5,1,1,"mq135",0.2);
        InverseMolecularSolver.Result hydrogen=result(76,"Hydrogen",0.45,0.8,4,0,"mq8",0.3);
        ChemCoreSpecialistConflictEngine.Result r=ChemCoreSpecialistConflictEngine.evaluate(List.of(ammonia,hydrogen));
        assertEquals(ChemCoreSpecialistConflictEngine.Verdict.CROSS_SPECIALIST_CONFLICT,r.verdict);
        assertTrue(r.holdNamedIdentity);
    }

    private static InverseMolecularSolver.Result result(long uid,String label,double posterior,double rms,
                                                         int reference,int calibrated,String sensor,double z){
        ChemCoreDecision.EvidenceTier tier=calibrated>0?ChemCoreDecision.EvidenceTier.M4_HAZROB_VALIDATED:
                ChemCoreDecision.EvidenceTier.M2_LITERATURE_OR_MANUFACTURER_SUPPORTED;
        ChemCoreEvidenceObservation e=new ChemCoreEvidenceObservation(
                calibrated>0?ChemCoreEvidenceObservation.Type.HR069_CALIBRATION:ChemCoreEvidenceObservation.Type.MANUFACTURER_CURVE,
                uid,label,List.of(sensor),"specialist","m","source:"+label,"spec:"+label,tier,true,true,calibrated>0,
                100,1,0,0,z,"specialist test");
        InverseMolecularSolver.Result r=new InverseMolecularSolver.Result(uid,label,true,100,rms,-0.5*rms*rms,
                Math.max(reference,calibrated),0,reference,calibrated,Double.NaN,Double.NaN,true,tier,
                new HashMap<>(),List.of(e),"test");
        r.posterior=posterior;return r;
    }
}
