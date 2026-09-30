package com.servoc.app.chemcore;

import com.servoc.app.fingerprint.ChemicalFingerprint;
import com.servoc.app.fingerprint.ResearchIdentificationFingerprintRegistry;
import com.servoc.app.sensors.RobotTelemetry;

import org.json.JSONObject;
import org.junit.Test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class ChemCoreEvidenceFusionTest {
    @Test public void threeQsprSensorChannelsFromOneModelAreOneEvidenceGroup(){
        List<ChemCoreEvidenceObservation> rows=List.of(
                ev(ChemCoreEvidenceObservation.Type.QSPR,ChemCoreDecision.EvidenceTier.M1_MOLECULARLY_INFERRED,"QSPR:runtime",List.of("mq2"),0.2,true),
                ev(ChemCoreEvidenceObservation.Type.QSPR,ChemCoreDecision.EvidenceTier.M1_MOLECULARLY_INFERRED,"QSPR:runtime",List.of("mq3"),0.3,true),
                ev(ChemCoreEvidenceObservation.Type.QSPR,ChemCoreDecision.EvidenceTier.M1_MOLECULARLY_INFERRED,"QSPR:runtime",List.of("mq135"),0.4,true));
        ChemCoreEvidenceFusion.Result r=ChemCoreEvidenceFusion.fuseForChemical(1,rows);
        assertEquals(1,r.qsprGroups);assertEquals(1,r.uniqueEvidenceGroups);assertEquals(3,r.sensorBreadth);
        assertEquals(20.0,r.score.qsprPct,1e-9);assertEquals(5.0,r.score.breadthPct,1e-9);assertEquals(25.0,r.score.totalPct,1e-9);
    }

    @Test public void threeIndependentQsprFamiliesRetainDiminishingPositiveSupport(){
        List<ChemCoreEvidenceObservation> rows=List.of(
                ev(ChemCoreEvidenceObservation.Type.QSPR,ChemCoreDecision.EvidenceTier.M1_MOLECULARLY_INFERRED,"QSPR:a",List.of("mq2"),0.2,true),
                ev(ChemCoreEvidenceObservation.Type.QSPR,ChemCoreDecision.EvidenceTier.M1_MOLECULARLY_INFERRED,"QSPR:b",List.of("mq3"),0.3,true),
                ev(ChemCoreEvidenceObservation.Type.QSPR,ChemCoreDecision.EvidenceTier.M1_MOLECULARLY_INFERRED,"QSPR:c",List.of("mq135"),0.4,true));
        ChemCoreEvidenceFusion.Result r=ChemCoreEvidenceFusion.fuseForChemical(1,rows);
        assertEquals(3,r.qsprGroups);assertEquals(3,r.uniqueEvidenceGroups);assertEquals(36.0,r.score.qsprPct,1e-9);assertEquals(41.0,r.score.totalPct,1e-9);
    }

    @Test public void singleReferenceAndSingleM4KeepQualityFirstBases(){
        ChemCoreEvidenceFusion.Result ref=ChemCoreEvidenceFusion.fuseForChemical(1,List.of(ev(ChemCoreEvidenceObservation.Type.LITERATURE_CURVE,ChemCoreDecision.EvidenceTier.M2_LITERATURE_OR_MANUFACTURER_SUPPORTED,"REF:paper-a",List.of("mq135"),0.3,true)));
        assertEquals(45.0,ref.score.totalPct,1e-9);assertEquals(1,ref.referenceGroups);assertEquals(0,ref.hr069Groups);assertEquals(1,ref.uniqueEvidenceGroups);
        ChemCoreEvidenceFusion.Result m4=ChemCoreEvidenceFusion.fuseForChemical(1,List.of(ev(ChemCoreEvidenceObservation.Type.HR069_CALIBRATION,ChemCoreDecision.EvidenceTier.M4_HAZROB_VALIDATED,"M4:run-a",List.of("mq3"),0.2,true)));
        assertEquals(75.0,m4.score.totalPct,1e-9);assertEquals(1,m4.hr069Groups);assertEquals(0,m4.referenceGroups);assertEquals(1,m4.uniqueEvidenceGroups);
    }

    @Test public void outOfDomainAndPoorFitAreRetainedAsSmallCorroborativeSupport(){
        ChemCoreEvidenceObservation good=ev(ChemCoreEvidenceObservation.Type.LITERATURE_CURVE,ChemCoreDecision.EvidenceTier.M2_LITERATURE_OR_MANUFACTURER_SUPPORTED,"REF:good",List.of("mq3"),0.4,true);
        ChemCoreEvidenceObservation out=ev(ChemCoreEvidenceObservation.Type.HR069_CALIBRATION,ChemCoreDecision.EvidenceTier.M4_HAZROB_VALIDATED,"M4:outside",List.of("mq2"),0.1,false);
        ChemCoreEvidenceObservation poor=ev(ChemCoreEvidenceObservation.Type.QSPR,ChemCoreDecision.EvidenceTier.M1_MOLECULARLY_INFERRED,"QSPR:poor",List.of("mq135"),4.0,true);
        ChemCoreEvidenceFusion.Result r=ChemCoreEvidenceFusion.fuseForChemical(1,List.of(good,out,poor));
        assertEquals(1,r.referenceGroups);assertEquals(0,r.hr069Groups);assertEquals(0,r.qsprGroups);assertEquals(2,r.corroborativeGroups);assertEquals(3,r.uniqueEvidenceGroups);assertEquals(3,r.sensorBreadth);assertEquals(3,r.supportingEvidence.size());
        assertEquals(2.5,r.score.corroborationPct,1e-9);assertEquals(52.5,r.score.totalPct,1e-9);
    }

    @Test public void degradedMeasurementCanSupportButNeverBecomesReportableQuantitative(){
        ChemCoreEvidenceObservation weak=new ChemCoreEvidenceObservation(
                ChemCoreEvidenceObservation.Type.SENSOR_PATTERN,1,"X",List.of("mq7"),"weak","m","s","weak-independent",
                ChemCoreDecision.EvidenceTier.M4_HAZROB_VALIDATED,true,false,true,40,1,0,0,0.5,"degraded",0.35,"CORROBORATIVE");
        assertEquals(0.35,weak.measurementQualityWeight,1e-9);assertEquals("CORROBORATIVE",weak.supportRole);assertFalse(weak.reportableQuantitative);
        ChemCoreEvidenceFusion.Result r=ChemCoreEvidenceFusion.fuseForChemical(1,List.of(weak));
        assertEquals(1,r.corroborativeGroups);assertEquals(1,r.uniqueEvidenceGroups);assertTrue(r.score.corroborationPct>0);assertTrue(r.score.totalPct<10);
    }

    @Test public void degradedCopyOfSameIndependenceGroupDoesNotDoubleCountStrongProof(){
        ChemCoreEvidenceObservation strong=ev(ChemCoreEvidenceObservation.Type.LITERATURE_CURVE,ChemCoreDecision.EvidenceTier.M2_LITERATURE_OR_MANUFACTURER_SUPPORTED,"REF:same",List.of("mq3"),0.2,true);
        ChemCoreEvidenceObservation weak=new ChemCoreEvidenceObservation(
                ChemCoreEvidenceObservation.Type.SENSOR_PATTERN,1,"X",List.of("mq135"),"weak","m","s","REF:same",
                ChemCoreDecision.EvidenceTier.M2_LITERATURE_OR_MANUFACTURER_SUPPORTED,true,false,false,40,1,0,0,0.3,"same source",0.40,"CORROBORATIVE");
        ChemCoreEvidenceFusion.Result r=ChemCoreEvidenceFusion.fuseForChemical(1,List.of(strong,weak));
        assertEquals(1,r.referenceGroups);assertEquals(0,r.corroborativeGroups);assertEquals(1,r.uniqueEvidenceGroups);assertEquals(2,r.sensorBreadth);assertEquals(48.0,r.score.totalPct,1e-9);
    }

    @Test public void derivedReferenceFingerprintDoesNotDoubleCountDirectReferenceCurve(){
        ChemCoreEvidenceObservation direct=ev(ChemCoreEvidenceObservation.Type.LITERATURE_CURVE,ChemCoreDecision.EvidenceTier.M2_LITERATURE_OR_MANUFACTURER_SUPPORTED,"REF:paper-a",List.of("mq135"),0.2,true);
        ChemCoreEvidenceObservation fp=ev(ChemCoreEvidenceObservation.Type.REFERENCE_FINGERPRINT,ChemCoreDecision.EvidenceTier.M3_ARRAY_CONSISTENT,"FP:derived",List.of("mq3","mq135"),0.4,true);
        ChemCoreEvidenceFusion.Result r=ChemCoreEvidenceFusion.fuseForChemical(1,List.of(direct,fp));
        assertEquals(1,r.referenceGroups);assertEquals(1,r.uniqueEvidenceGroups);assertEquals(2,r.sensorBreadth);assertEquals(2,r.supportingEvidence.size());assertEquals(48.0,r.score.totalPct,1e-9);
    }

    @Test public void resultEnvelopeKeepsPosteriorAndAnalyticalConfidenceSemanticallySeparate(){
        ChemCoreEvidenceObservation e=ev(ChemCoreEvidenceObservation.Type.LITERATURE_CURVE,ChemCoreDecision.EvidenceTier.M2_LITERATURE_OR_MANUFACTURER_SUPPORTED,"REF:a",List.of("mq135"),0.2,true);
        ChemCoreEvidenceFusion.Result f=ChemCoreEvidenceFusion.fuseForChemical(1,List.of(e));
        ChemCoreResultEnvelope.CandidateSummary c=new ChemCoreResultEnvelope.CandidateSummary(1,"X",0.93,40,0.2,1,ChemCoreDecision.EvidenceTier.M2_LITERATURE_OR_MANUFACTURER_SUPPORTED,50,90,60,0,true,Collections.emptyMap(),f.score.totalPct,f.uniqueEvidenceGroups,f.sensorBreadth,List.of(e));
        ChemCoreResultEnvelope result=new ChemCoreResultEnvelope("S",ChemCoreDecision.Outcome.SCREENING,ChemCoreDecision.EvidenceTier.M2_LITERATURE_OR_MANUFACTURER_SUPPORTED,1L,"X",Double.NaN,Double.NaN,Double.NaN,Double.NaN,0.93,Double.NaN,"p","f","m","screening",List.of(c));
        JSONObject json=result.toJson();assertEquals(0.93,json.optDouble("top_posterior"),1e-9);assertEquals(45.0,json.optDouble("analytical_confidence_pct"),1e-9);assertNotEquals(json.optDouble("top_posterior"),json.optDouble("analytical_confidence_pct"),1e-9);assertEquals(ChemCoreResultEnvelope.ANALYTICAL_CONFIDENCE_SEMANTICS,json.optString("analytical_confidence_semantics"));assertEquals(1,json.optInt("confidence_evidence_groups"));
        JSONObject ej=json.getJSONArray("candidates").getJSONObject(0).getJSONArray("evidence_observations").getJSONObject(0);assertEquals(1.0,ej.getDouble("measurement_quality_weight"),1e-9);assertEquals("PRIMARY",ej.getString("support_role"));
    }

    @Test public void acceptedFingerprintProducesStructuredConfidenceButNeverReportablePpm(){
        ChemicalFingerprint ethanol=template(103,"|300ppm|");assertNotNull(ethanol);Map<String,Double> ratios=ratios(ethanol);
        ChemCoreResultEnvelope base=new ChemCoreResultEnvelope("T",ChemCoreDecision.Outcome.SCREENING,ChemCoreDecision.EvidenceTier.M0_CATALOG_ONLY,null,"",Double.NaN,Double.NaN,Double.NaN,Double.NaN,0.2,Double.NaN,"profile","features","base","screening",Collections.emptyList());
        ChemCoreOpenSetGate.Result accepted=new ChemCoreOpenSetGate.Result(true,0.5,1.0,1.0,"inside trained feature domain");
        ChemCoreResultEnvelope fused=AcceptedFingerprintIdentityEngine.fuse(base,ratios,accepted);
        assertEquals(ChemCoreDecision.Outcome.IDENTIFIED,fused.outcome);assertTrue(Double.isFinite(fused.analyticalConfidencePct));assertTrue(fused.analyticalConfidencePct>=48.0);assertFalse(Double.isFinite(fused.ppm));assertFalse(fused.toJson().optBoolean("ppm_reportable",true));assertTrue(fused.confidenceEvidenceGroups>=1);assertTrue(fused.confidenceSensorBreadth>=2);
    }

    private static ChemCoreEvidenceObservation ev(ChemCoreEvidenceObservation.Type type,ChemCoreDecision.EvidenceTier tier,String group,List<String> sensors,double z,boolean inside){return new ChemCoreEvidenceObservation(type,1,"X",sensors,type.name(),group,"source:"+group,group,tier,inside,true,tier==ChemCoreDecision.EvidenceTier.M4_HAZROB_VALIDATED,40,1,0,0,z,"test");}
    private static ChemicalFingerprint template(int chemical,String labelPart){for(ChemicalFingerprint f:ResearchIdentificationFingerprintRegistry.library())if(f.chemicalId==chemical&&f.label.contains(labelPart))return f;return null;}
    private static Map<String,Double> ratios(ChemicalFingerprint f){Map<String,Double> out=new LinkedHashMap<>();for(int i=0;i<Math.min(RobotTelemetry.MQ_KEYS.length,f.meanLogRatio.length);i++)if(Double.isFinite(f.meanLogRatio[i]))out.put(RobotTelemetry.MQ_KEYS[i],Math.pow(10.0,f.meanLogRatio[i]));return out;}
}
