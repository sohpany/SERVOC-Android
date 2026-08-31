package com.servoc.app.vision;

import org.junit.Test;
import static org.junit.Assert.*;

public class FlameEvidenceTrackerTest {
    @Test public void singleHotFrameNeverBecomesPersistent(){
        FlameEvidenceTracker t=new FlameEvidenceTracker(.7,.4,.5,3,500,1000);
        assertEquals(FlameEvidenceTracker.State.CANDIDATE,t.update(.95,0).state);
        assertNotEquals(FlameEvidenceTracker.State.PERSISTENT,t.update(.1,100).state);
    }

    @Test public void sustainedEvidenceBecomesPersistent(){
        FlameEvidenceTracker t=new FlameEvidenceTracker(.7,.4,.5,3,500,1000);
        t.update(.85,0);t.update(.88,250);
        FlameEvidenceTracker.Snapshot s=t.update(.90,550);
        assertEquals(FlameEvidenceTracker.State.PERSISTENT,s.state);
        assertTrue(s.ema>.4);assertEquals(3,s.positiveFrames);
    }

    @Test public void hysteresisDoesNotClearPersistentEvidenceOnOneModerateFrame(){
        FlameEvidenceTracker t=new FlameEvidenceTracker(.7,.4,.5,2,200,1000);
        t.update(.9,0);FlameEvidenceTracker.Snapshot p=t.update(.9,250);
        assertEquals(FlameEvidenceTracker.State.PERSISTENT,p.state);
        assertEquals(FlameEvidenceTracker.State.PERSISTENT,t.update(.55,400).state);
    }

    @Test public void missingFramesEventuallyClearEvidence(){
        FlameEvidenceTracker t=new FlameEvidenceTracker(.7,.4,.5,2,200,500);
        t.update(.9,0);t.update(.9,250);
        assertEquals(FlameEvidenceTracker.State.CLEAR,t.missing(900).state);
    }
}
