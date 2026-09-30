package com.servoc.app.chemcore;

import com.servoc.app.fingerprint.ChemicalFingerprint;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public final class ChemCoreEvidenceObservationTest {
    @Test public void threeQsprSensorsRemainThreeSensorContributorsButOneEvidenceGroup(){
        QsprMoxModel m2=qspr("mq2");QsprMoxModel m3=qspr("mq3");QsprMoxModel m135=qspr("mq135");
        QsprResponseModelAdapter a2=new QsprResponseModelAdapter(m2,Collections.emptyMap());
        QsprResponseModelAdapter a3=new QsprResponseModelAdapter(m3,Collections.emptyMap());
        QsprResponseModelAdapter a135=new QsprResponseModelAdapter(m135,Collections.emptyMap());
        double ppm=40,temp=25,rh=50;
        Map<String,Double> observed=Map.of(
                "mq2",Math.pow(10,a2.predict(ppm,temp,rh,null).meanLog10Ratio),
                "mq3",Math.pow(10,a3.predict(ppm,temp,rh,null).meanLog10Ratio),
                "mq135",Math.pow(10,a135.predict(ppm,temp,rh,null).meanLog10Ratio));
        InverseMolecularSolver.Result r=InverseMolecularSolver.solveOne(observed,
                new InverseMolecularSolver.Candidate(77,"QSPR target",List.of(a2,a3,a135),0,2),temp,rh,SamplingProfile.hazRobPrototypeV1());
        assertTrue(r.solvable);assertEquals(3,r.molecularContributors);assertEquals(3,r.evidenceSensorBreadth);
        assertEquals(1,r.uniqueEvidenceGroups);assertEquals(3,r.evidenceObservations.size());
        for(ChemCoreEvidenceObservation e:r.evidenceObservations){
            assertEquals(ChemCoreEvidenceObservation.Type.QSPR,e.type);
            assertEquals("QSPR:"+ChemCoreQsprRuntimeRegistry.VERSION,e.independenceGroup);
            assertTrue(e.quantitativeEstimateSupported);assertFalse(e.reportableQuantitative);
            assertEquals(ChemCoreDecision.EvidenceTier.M1_MOLECULARLY_INFERRED,e.tier);
        }
    }

    @Test public void m4CurveCanBeReportableEvidenceButM2CurveCannot(){
        ChemCoreResponseModel m4=new PowerLawResponseModel("mq2","hr069-test",2,-.4,5,500,.05,10,40,10,90,ChemCoreDecision.EvidenceTier.M4_HAZROB_VALIDATED,"device calibration");
        ChemCoreResponseModel m2=new PowerLawResponseModel("mq3","literature-test",1.5,-.3,5,500,.08,10,40,10,90,ChemCoreDecision.EvidenceTier.M2_LITERATURE_OR_MANUFACTURER_SUPPORTED,"literature prior");
        double ppm=40;ChemCoreResponseModel.Estimate p4=m4.predict(ppm,25,50,null),p2=m2.predict(ppm,25,50,null);
        ChemCoreEvidenceObservation e4=ChemCoreEvidenceObservation.fromResponseModel(1,"A",m4,p4,ppm,Math.pow(10,p4.meanLog10Ratio),0,0);
        ChemCoreEvidenceObservation e2=ChemCoreEvidenceObservation.fromResponseModel(1,"A",m2,p2,ppm,Math.pow(10,p2.meanLog10Ratio),0,0);
        assertTrue(e4.reportableQuantitative);assertEquals(ChemCoreEvidenceObservation.Type.HR069_CALIBRATION,e4.type);
        assertFalse(e2.reportableQuantitative);assertEquals(ChemCoreDecision.EvidenceTier.M2_LITERATURE_OR_MANUFACTURER_SUPPORTED,e2.tier);
    }

    @Test public void outsideEnvironmentalDomainRemainsEvidenceButCannotBeReportable(){
        ChemCoreResponseModel m4=new PowerLawResponseModel("mq2","hr069-domain",2,-.4,5,500,.05,20,30,40,60,ChemCoreDecision.EvidenceTier.M4_HAZROB_VALIDATED,"device calibration");
        double ppm=40;ChemCoreResponseModel.Estimate p=m4.predict(ppm,45,80,null);
        assertTrue(p.available);assertFalse(p.insideDomain);
        ChemCoreEvidenceObservation e=ChemCoreEvidenceObservation.fromResponseModel(1,"A",m4,p,ppm,Math.pow(10,p.meanLog10Ratio),0,0);
        assertTrue(e.quantitativeEstimateSupported);assertFalse(e.reportableQuantitative);assertFalse(e.insideDomain);
    }

    @Test public void futureHr069ExperimentalFingerprintNeedsNoNewEngineAndRemainsIdentityOnly(){
        double[] mean={Math.log10(.60),Math.log10(.85),Double.NaN,Double.NaN,Double.NaN,Double.NaN,Double.NaN,Double.NaN};
        double[] sigma={.08,.08,Double.NaN,Double.NaN,Double.NaN,Double.NaN,Double.NaN,Double.NaN};
        ChemicalFingerprint fp=new ChemicalFingerprint(103,"HR069-EXP-ETHANOL",12,mean,sigma);
        ChemCoreFingerprintEvidenceEngine.SourceProfile source=ChemCoreFingerprintEvidenceEngine.SourceProfile.hr069Experimental("EXP-2026-001");
        ChemCoreFingerprintEvidenceEngine.Decision d=ChemCoreFingerprintEvidenceEngine.evaluate(
                Map.of("mq2",.60,"mq3",.85),List.of(fp),source,2,1.5,5,.20);
        assertTrue(d.accepted);assertNotNull(d.evidence);
        assertEquals(ChemCoreEvidenceObservation.Type.HR069_EXPERIMENTAL_FINGERPRINT,d.evidence.type);
        assertEquals(ChemCoreDecision.EvidenceTier.M3_ARRAY_CONSISTENT,d.evidence.tier);
        assertEquals(2,d.evidence.sensorKeys.size());assertFalse(d.evidence.quantitativeEstimateSupported);assertFalse(d.evidence.reportableQuantitative);
        assertEquals("EXP-2026-001",d.evidence.sourceId);
    }

    private static QsprMoxModel qspr(String sensor){
        List<QsprMoxModel.Observation> rows=new ArrayList<>();double[] ppm={5,8,12,20,30,40,60,90,140,220,340,500};
        for(int i=0;i<ppm.length;i++){double t=20+(i%3)*5,rh=40+(i%3)*10;double y=Math.log10(2.0)-0.40*Math.log10(ppm[i])+0.001*(t-25)-0.0005*(rh-50);rows.add(new QsprMoxModel.Observation(Collections.emptyMap(),ppm[i],t,rh,y,1));}
        return QsprMoxModel.fit(sensor,ChemCoreQsprRuntimeRegistry.VERSION+"/"+sensor,new String[0],rows,.10);
    }
}
