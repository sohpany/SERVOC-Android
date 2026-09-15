package com.servoc.app.communication;

import org.junit.Test;
import static org.junit.Assert.*;

public final class RemoteDrivePolicyTest {
    @Test public void stopTransitionBypassesRateLimit(){
        assertTrue(RemoteDrivePolicy.due(1010,1000,true,0,0));
    }

    @Test public void movingChangesAreRateLimitedToTwentyFiveHz(){
        assertFalse(RemoteDrivePolicy.due(1030,1000,true,100,60));
        assertTrue(RemoteDrivePolicy.due(1040,1000,true,100,60));
    }

    @Test public void unchangedMotionGetsHoldRefresh(){
        assertFalse(RemoteDrivePolicy.due(1119,1000,false,100,100));
        assertTrue(RemoteDrivePolicy.due(1120,1000,false,100,100));
    }

    @Test public void unchangedZeroDoesNotGenerateTraffic(){
        assertFalse(RemoteDrivePolicy.due(5000,1000,false,0,0));
    }

    @Test public void policyTimingContractIsPinned(){
        assertEquals(20,RemoteDrivePolicy.PUMP_MS);
        assertEquals(40,RemoteDrivePolicy.MIN_PUBLISH_MS);
        assertEquals(120,RemoteDrivePolicy.HOLD_REFRESH_MS);
        assertEquals(900,RemoteDrivePolicy.COMMAND_TTL_MS);
    }
}
