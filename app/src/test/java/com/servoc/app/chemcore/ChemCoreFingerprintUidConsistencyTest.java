package com.servoc.app.chemcore;

import com.servoc.app.fingerprint.ChemicalFingerprint;
import com.servoc.app.fingerprint.ResearchIdentificationFingerprintRegistry;
import com.servoc.app.sensors.RobotTelemetry;

import org.junit.Test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.*;

/** Regression guard: the fingerprint evidence row and promoted result must share one chemical_uid. */
public class ChemCoreFingerprintUidConsistencyTest {
    @Test public void acceptedFingerprintEvidenceAndPromotedIdentityUseSameCanonicalUid(){
        ChemicalFingerprint ethanol=null;
        for(ChemicalFingerprint f:ResearchIdentificationFingerprintRegistry.library()){
            if(f.chemicalId==103&&f.label.contains("|300ppm|")){ethanol=f;break;}
        }
        assertNotNull(ethanol);

        Map<String,Double> ratios=new LinkedHashMap<>();
        for(int i=0;i<Math.min(RobotTelemetry.MQ_KEYS.length,ethanol.meanLogRatio.length);i++){
            if(Double.isFinite(ethanol.meanLogRatio[i]))
                ratios.put(RobotTelemetry.MQ_KEYS[i],Math.pow(10.0,ethanol.meanLogRatio[i]));
        }

        AcceptedFingerprintIdentityEngine.Decision decision=AcceptedFingerprintIdentityEngine.evaluate(ratios);
        assertTrue(decision.accepted);
        assertNotNull(decision.evidence);

        ChemCoreResultEnvelope screening=new ChemCoreResultEnvelope(
                "UID-CONSISTENCY",ChemCoreDecision.Outcome.SCREENING,
                ChemCoreDecision.EvidenceTier.M2_LITERATURE_OR_MANUFACTURER_SUPPORTED,
                null,"",Double.NaN,Double.NaN,Double.NaN,Double.NaN,0.2,Double.NaN,
                "profile","features","base","screening only",Collections.emptyList());
        ChemCoreOpenSetGate.Result accepted=new ChemCoreOpenSetGate.Result(
                true,0.5,1.0,1.0,"inside trained feature domain");
        ChemCoreResultEnvelope fused=AcceptedFingerprintIdentityEngine.fuse(screening,ratios,accepted);

        assertNotNull(fused);
        assertEquals(ChemCoreDecision.Outcome.IDENTIFIED,fused.outcome);
        assertNotNull(fused.chemicalUid);
        assertEquals(decision.evidence.chemicalUid,fused.chemicalUid.longValue());
        assertEquals(ChemCoreLegacyCandidateRegistry.uidForLegacyChemical(103),fused.chemicalUid.longValue());
        assertEquals(ChemCoreDecision.EvidenceTier.M3_ARRAY_CONSISTENT,fused.evidenceTier);
        assertFalse(Double.isFinite(fused.ppm));
        assertFalse(fused.toJson().optBoolean("ppm_reportable",true));
    }
}
