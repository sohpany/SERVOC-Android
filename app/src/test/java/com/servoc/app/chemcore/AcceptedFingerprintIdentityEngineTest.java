package com.servoc.app.chemcore;

import com.servoc.app.fingerprint.ChemicalFingerprint;
import com.servoc.app.fingerprint.ResearchIdentificationFingerprintRegistry;
import com.servoc.app.sensors.RobotTelemetry;

import org.json.JSONObject;
import org.junit.Test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class AcceptedFingerprintIdentityEngineTest {
    @Test public void exactMultichannelEthanolReferencePromotesScreeningToIdentityButNeverM4(){
        ChemicalFingerprint ethanol=template(103,"|300ppm|");
        assertNotNull(ethanol);
        Map<String,Double> ratios=ratios(ethanol);
        ChemCoreResultEnvelope base=screening();
        ChemCoreResultEnvelope fused=AcceptedFingerprintIdentityEngine.fuse(base,ratios);
        assertNotNull(fused);
        assertTrue(AcceptedFingerprintIdentityEngine.isAcceptedIdentity(fused));
        assertEquals(ChemCoreDecision.Outcome.IDENTIFIED,fused.outcome);
        assertEquals(ChemCoreDecision.EvidenceTier.M3_ARRAY_CONSISTENT,fused.evidenceTier);
        assertEquals(Long.valueOf(104L),fused.chemicalUid);
        assertEquals("Ethanol",fused.chemicalLabel);
        assertFalse(Double.isFinite(fused.ppm));
        JSONObject json=fused.toJson();
        assertFalse(json.optBoolean("ppm_reportable",true));
        assertFalse(json.optBoolean("quantitative_occupational_interpretation_permitted",true));
        assertTrue(fused.note.contains(ResearchIdentificationFingerprintRegistry.IDENTITY_ACCEPTANCE_STATE));
    }

    @Test public void singleChannelAcetonePriorDoesNotBecomeNamedIdentity(){
        ChemicalFingerprint acetone=template(110,"|50ppm|");
        assertNotNull(acetone);
        ChemCoreResultEnvelope fused=AcceptedFingerprintIdentityEngine.fuse(screening(),ratios(acetone));
        assertEquals(ChemCoreDecision.Outcome.SCREENING,fused.outcome);
        assertFalse(AcceptedFingerprintIdentityEngine.isAcceptedIdentity(fused));
    }

    @Test public void acceptedFingerprintLayerCannotOverrideUnknown(){
        ChemicalFingerprint ethanol=template(103,"|300ppm|");
        assertNotNull(ethanol);
        ChemCoreResultEnvelope unknown=new ChemCoreResultEnvelope("T",ChemCoreDecision.Outcome.UNKNOWN,
                ChemCoreDecision.EvidenceTier.M2_LITERATURE_OR_MANUFACTURER_SUPPORTED,null,"",
                Double.NaN,Double.NaN,Double.NaN,2.0,Double.NaN,Double.NaN,"profile","features","model",
                "validated open-set rejection",Collections.emptyList());
        ChemCoreResultEnvelope fused=AcceptedFingerprintIdentityEngine.fuse(unknown,ratios(ethanol));
        assertSame(unknown,fused);
        assertEquals(ChemCoreDecision.Outcome.UNKNOWN,fused.outcome);
    }

    @Test public void registryAcceptanceIsIdentityOnly(){
        assertTrue(ResearchIdentificationFingerprintRegistry.IDENTITY_REFERENCE_ACCEPTED);
        assertEquals("ACCEPTED_FOR_IDENTIFICATION",ResearchIdentificationFingerprintRegistry.IDENTITY_ACCEPTANCE_STATE);
        assertFalse(ResearchIdentificationFingerprintRegistry.M4);
    }

    private static ChemCoreResultEnvelope screening(){
        return new ChemCoreResultEnvelope("T",ChemCoreDecision.Outcome.SCREENING,
                ChemCoreDecision.EvidenceTier.M2_LITERATURE_OR_MANUFACTURER_SUPPORTED,null,"",
                Double.NaN,Double.NaN,Double.NaN,Double.NaN,0.2,Double.NaN,"profile","features","base",
                "screening only",Collections.emptyList());
    }

    private static ChemicalFingerprint template(int chemical,String labelPart){
        for(ChemicalFingerprint f:ResearchIdentificationFingerprintRegistry.library())
            if(f.chemicalId==chemical&&f.label.contains(labelPart))return f;
        return null;
    }

    private static Map<String,Double> ratios(ChemicalFingerprint f){
        Map<String,Double> out=new LinkedHashMap<>();
        for(int i=0;i<Math.min(RobotTelemetry.MQ_KEYS.length,f.meanLogRatio.length);i++)
            if(Double.isFinite(f.meanLogRatio[i]))out.put(RobotTelemetry.MQ_KEYS[i],Math.pow(10.0,f.meanLogRatio[i]));
        return out;
    }
}
