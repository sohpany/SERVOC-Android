package com.servoc.app.control;

import static org.junit.Assert.*;
import org.junit.Test;

public final class EmergencyStopPressPolicyTest {
    @Test public void latchRequiresFullTwoSecondHold(){
        assertFalse(EmergencyStopPressPolicy.shouldLatch(false,0));
        assertFalse(EmergencyStopPressPolicy.shouldLatch(false,1999));
        assertTrue(EmergencyStopPressPolicy.shouldLatch(false,2000));
        assertTrue(EmergencyStopPressPolicy.shouldLatch(false,2500));
    }

    @Test public void latchedStateUsesTapForResetNotRelatch(){
        assertFalse(EmergencyStopPressPolicy.shouldLatch(true,5000));
        assertTrue(EmergencyStopPressPolicy.shouldResetOnTap(true));
        assertFalse(EmergencyStopPressPolicy.shouldResetOnTap(false));
    }
}
