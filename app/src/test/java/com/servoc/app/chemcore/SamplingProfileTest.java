package com.servoc.app.chemcore;

import org.junit.Test;
import static org.junit.Assert.*;

public class SamplingProfileTest {
    @Test public void prototypeGeometryMatchesDeclaredChamber(){
        SamplingProfile p=SamplingProfile.hazRobPrototypeV1();
        assertEquals(0.54,p.grossVolumeLiters(),1e-12);
        assertEquals(0.0016,p.inletAreaM2(),1e-12);
        assertEquals(0.0036,p.chamberCrossSectionM2(),1e-12);
        assertEquals(1.4,p.fanElectricalPowerW(),1e-12);
        assertEquals(0.54,p.effectiveVolumeLiters(),1e-12);
        assertFalse(p.hasFlow());
        assertFalse(p.flowValidated());
        assertTrue(Double.isNaN(p.grossResidenceTimeS()));
    }

    @Test public void standardV2UsesFixedEstimatedSamplingFactor(){
        SamplingProfile p=SamplingProfile.hazRobStandardChamberV2();
        assertEquals("hazrob-chemcore-chamber-v2",p.profileId);
        assertEquals(SamplingProfile.FlowStatus.FLOW_ESTIMATED,p.flowStatus);
        assertEquals(0.54,p.grossVolumeLiters(),1e-12);
        assertEquals(0.517,p.effectiveVolumeLiters(),1e-12);
        assertEquals(0.0008,p.inletAreaM2(),1e-12);
        assertEquals(0.0036,p.chamberCrossSectionM2(),1e-12);
        assertEquals(0.00110,p.flowM3s,1e-12);
        assertEquals(0.00040,p.flowUncertaintyM3s,1e-12);
        assertEquals(1.375,p.inletVelocityMps(),1e-12);
        assertEquals(0.00110/0.0036,p.chamberVelocityMps(),1e-12);
        assertEquals(0.000517/0.00110,p.effectiveResidenceTimeS(),1e-12);
        assertTrue(p.hasFlow());
        assertFalse(p.flowValidated());
    }

    @Test public void flowDerivedQuantitiesAreConsistent(){
        SamplingProfile base=SamplingProfile.hazRobPrototypeV1();
        double q=SamplingProfile.cfmToM3s(8.0);
        SamplingProfile p=base.withFlow(SamplingProfile.FlowStatus.FLOW_ESTIMATED,q,q*0.5,"provisional estimate");
        assertTrue(p.hasFlow());
        assertFalse(p.flowValidated());
        assertEquals(q/0.0016,p.inletVelocityMps(),1e-12);
        assertEquals(q/0.0036,p.chamberVelocityMps(),1e-12);
        assertEquals(0.00054/q,p.grossResidenceTimeS(),1e-12);
        assertEquals(0.00054/q,p.effectiveResidenceTimeS(),1e-12);
        assertEquals(8.0,SamplingProfile.m3sToLitersPerMinute(q)/28.316846592,1e-6);
    }

    @Test public void calibratedFlagRequiresCalibratedStatusAndPositiveFlow(){
        SamplingProfile p=SamplingProfile.hazRobPrototypeV1().withFlow(
                SamplingProfile.FlowStatus.FLOW_CALIBRATED,
                SamplingProfile.litersPerMinuteToM3s(120),
                SamplingProfile.litersPerMinuteToM3s(5),"measured");
        assertTrue(p.flowValidated());
    }
}
