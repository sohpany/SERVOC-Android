package com.servoc.app.chemcore;

import org.junit.Test;

import static org.junit.Assert.*;

public class ChemCoreRuntimeCoordinatorTest {

    private static final class MemoryPersistence implements ChemCoreRuntimeCoordinator.Persistence {
        String startedSession = "";
        ChemCoreSessionController.State lastState;
        ChemCoreDecision.Outcome outcome;
        ChemCoreDecision.EvidenceTier tier;

        @Override public boolean start(String sessionId, SamplingProfile profile,
                                       ChemCoreSessionController.Mode mode,
                                       ChemCoreSessionController.State state) {
            startedSession = sessionId;
            lastState = state;
            return true;
        }

        @Override public boolean update(String sessionId, ChemCoreSessionController.State state,
                                        String analytics) {
            lastState = state;
            return true;
        }

        @Override public boolean finish(String sessionId, ChemCoreSessionController.State state,
                                        ChemCoreDecision.Outcome outcome,
                                        ChemCoreDecision.EvidenceTier tier,
                                        String analytics) {
            lastState = state;
            this.outcome = outcome;
            this.tier = tier;
            return true;
        }
    }

    private static ChemCoreRuntimeCoordinator runtime(MemoryPersistence persistence) {
        return new ChemCoreRuntimeCoordinator(
                SamplingProfile.hazRobStandardChamberV2(),
                SamplingLayout.hazRobStandardArrayV2(),
                persistence);
    }

    @Test public void focusedAnalysisRefusesMotionAndUnpoweredArray() {
        MemoryPersistence p = new MemoryPersistence();
        ChemCoreRuntimeCoordinator r = runtime(p);

        assertEquals(ChemCoreSessionController.State.ERROR, r.begin(false, true).state);

        r = runtime(new MemoryPersistence());
        assertEquals(ChemCoreSessionController.State.ERROR, r.begin(true, false).state);
    }

    @Test public void sessionAttachmentUsesRealRecorderIdentifier() {
        MemoryPersistence p = new MemoryPersistence();
        ChemCoreRuntimeCoordinator r = runtime(p);
        ChemCoreRuntimeCoordinator.Snapshot start = r.begin(true, true);
        assertEquals(ChemCoreSessionController.State.CONDITION, start.state);
        assertTrue(start.armed);
        assertEquals("FLOW_ESTIMATED", start.flowStatus.name());
        assertFalse(start.flowValidated);

        assertTrue(r.attachMeasurementSession("DEV-123"));
        assertEquals("DEV-123", p.startedSession);
        assertEquals("DEV-123", r.snapshot().measurementSessionId);
        assertFalse(r.attachMeasurementSession("DEV-OTHER"));
    }

    @Test public void stateMachineDoesNotSkipScientificPhases() {
        MemoryPersistence p = new MemoryPersistence();
        ChemCoreRuntimeCoordinator r = runtime(p);
        r.begin(true, true);
        r.attachMeasurementSession("DEV-456");

        assertEquals(ChemCoreSessionController.State.BASELINE,
                r.advance(ChemCoreSessionController.Event.CONDITION_READY, true, null).state);
        // EXPOSURE_WINDOW_READY cannot skip the required explicit baseline-stable evidence.
        assertEquals(ChemCoreSessionController.State.BASELINE,
                r.advance(ChemCoreSessionController.Event.EXPOSURE_WINDOW_READY, true, null).state);
        assertEquals(ChemCoreSessionController.State.EXPOSURE,
                r.advance(ChemCoreSessionController.Event.BASELINE_STABLE, true, null).state);
        assertEquals(1, r.snapshot().exposurePass);
    }

    @Test public void motionDuringControlledStateFailsClosed() {
        MemoryPersistence p = new MemoryPersistence();
        ChemCoreRuntimeCoordinator r = runtime(p);
        r.begin(true, true);
        r.attachMeasurementSession("DEV-789");

        ChemCoreRuntimeCoordinator.Snapshot s = r.observeMotion(10, 0);
        assertEquals(ChemCoreSessionController.State.ERROR, s.state);
        assertFalse(s.armed);
        assertEquals(ChemCoreDecision.Outcome.INVALID_MEASUREMENT, p.outcome);
        assertEquals(ChemCoreDecision.EvidenceTier.M0_CATALOG_ONLY, p.tier);
    }
}
