package com.servoc.app.chemcore;

import org.junit.Test;
import static org.junit.Assert.*;

public class ChemCoreSessionControllerTest {
    @Test public void analysisRefusesMovingRobot(){
        ChemCoreSessionController c=new ChemCoreSessionController();
        ChemCoreSessionController.Snapshot s=c.start(ChemCoreSessionController.Mode.CHEMCORE_ANALYSIS,false,true,SamplingProfile.hazRobPrototypeV1());
        assertEquals(ChemCoreSessionController.State.ERROR,s.state);
        assertTrue(s.reason.contains("stationary"));
    }

    @Test public void controlledCycleAdvancesInOrder(){
        ChemCoreSessionController c=new ChemCoreSessionController();
        assertEquals(ChemCoreSessionController.State.CONDITION,c.start(ChemCoreSessionController.Mode.CHEMCORE_ANALYSIS,true,true,SamplingProfile.hazRobPrototypeV1()).state);
        assertEquals(ChemCoreSessionController.State.BASELINE,c.on(ChemCoreSessionController.Event.CONDITION_READY,true).state);
        assertEquals(ChemCoreSessionController.State.EXPOSURE,c.on(ChemCoreSessionController.Event.BASELINE_STABLE,true).state);
        assertEquals(1,c.snapshot().exposurePass);
        assertEquals(ChemCoreSessionController.State.STABILITY_CHECK,c.on(ChemCoreSessionController.Event.EXPOSURE_WINDOW_READY,true).state);
        assertEquals(ChemCoreSessionController.State.FEATURE_EXTRACTION,c.on(ChemCoreSessionController.Event.RESPONSE_STABLE,true).state);
        assertEquals(ChemCoreSessionController.State.IDENTITY,c.on(ChemCoreSessionController.Event.FEATURES_READY,true).state);
        assertEquals(ChemCoreSessionController.State.OPEN_SET_GATE,c.on(ChemCoreSessionController.Event.IDENTITY_READY,true).state);
        assertEquals(ChemCoreSessionController.State.QUANTIFICATION,c.on(ChemCoreSessionController.Event.OPEN_SET_ACCEPTED,true).state);
        assertEquals(ChemCoreSessionController.State.CONSISTENCY_CHECK,c.on(ChemCoreSessionController.Event.QUANTIFICATION_READY,true).state);
        assertEquals(ChemCoreSessionController.State.REPORT,c.on(ChemCoreSessionController.Event.CONSISTENCY_ACCEPTED,true).state);
        assertEquals(ChemCoreSessionController.State.RECOVERY,c.on(ChemCoreSessionController.Event.REPORT_SAVED,true).state);
        assertEquals(ChemCoreSessionController.State.COMPLETE,c.on(ChemCoreSessionController.Event.RECOVERY_COMPLETE,true).state);
    }

    @Test public void openSetRejectionSkipsQuantification(){
        ChemCoreSessionController c=new ChemCoreSessionController();
        c.start(ChemCoreSessionController.Mode.CHEMCORE_ANALYSIS,true,true,SamplingProfile.hazRobPrototypeV1());
        c.on(ChemCoreSessionController.Event.CONDITION_READY,true);
        c.on(ChemCoreSessionController.Event.BASELINE_STABLE,true);
        c.on(ChemCoreSessionController.Event.EXPOSURE_WINDOW_READY,true);
        c.on(ChemCoreSessionController.Event.RESPONSE_STABLE,true);
        c.on(ChemCoreSessionController.Event.FEATURES_READY,true);
        c.on(ChemCoreSessionController.Event.IDENTITY_READY,true);
        ChemCoreSessionController.Snapshot s=c.on(ChemCoreSessionController.Event.OPEN_SET_REJECTED,true);
        assertEquals(ChemCoreSessionController.State.REPORT,s.state);
        assertTrue(s.reason.contains("open-set"));
    }

    @Test public void motionDuringAnalysisFailsClosed(){
        ChemCoreSessionController c=new ChemCoreSessionController();
        c.start(ChemCoreSessionController.Mode.CHEMCORE_ANALYSIS,true,true,SamplingProfile.hazRobPrototypeV1());
        ChemCoreSessionController.Snapshot s=c.on(ChemCoreSessionController.Event.CONDITION_READY,false);
        assertEquals(ChemCoreSessionController.State.ERROR,s.state);
    }
}
