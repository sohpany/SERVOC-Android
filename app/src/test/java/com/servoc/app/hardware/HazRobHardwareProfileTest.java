package com.servoc.app.hardware;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** Locks the Android hardware identity to the 0.10.1 GPIO/firmware profile contract. */
public final class HazRobHardwareProfileTest {
    @Test public void profileIdentityMatches0101Contract(){
        assertEquals("hazrob-0.10.1-1r8mq-l298n6-batt-ultra-ldr-v1",HazRobHardwareProfile.ID);
    }

    @Test public void driveAndMqAuthorityRemainPinned(){
        // HR-0101-048: logical LEFT is physical L298N channel B; RIGHT is channel A.
        assertEquals(17,HazRobHardwareProfile.DRIVE_L_EN);
        assertEquals(19,HazRobHardwareProfile.DRIVE_L_IN1);
        assertEquals(32,HazRobHardwareProfile.DRIVE_L_IN2);
        assertEquals(16,HazRobHardwareProfile.DRIVE_R_EN);
        assertEquals(18,HazRobHardwareProfile.DRIVE_R_IN1);
        assertEquals(27,HazRobHardwareProfile.DRIVE_R_IN2);
        assertEquals(25,HazRobHardwareProfile.MQ_RELAY_GPIO);
        assertEquals(8,HazRobHardwareProfile.MQ.length);
    }
}
