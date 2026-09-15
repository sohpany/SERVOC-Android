package com.servoc.app.hardware;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/** Locks the Android hardware identity to the controlled 0.10.1 GPIO/firmware profile contract. */
public final class HazRobHardwareProfileTest {
    @Test public void profileIdentityMatchesHr050Contract(){
        assertEquals("hazrob-0.10.1-1r8mq-l298n6-batt-ultra-ldr-env2-v2",HazRobHardwareProfile.ID);
    }

    @Test public void driveAndMqAuthorityRemainPinned(){
        assertEquals(17,HazRobHardwareProfile.DRIVE_L_EN);
        assertEquals(19,HazRobHardwareProfile.DRIVE_L_IN1);
        assertEquals(32,HazRobHardwareProfile.DRIVE_L_IN2);
        assertEquals(16,HazRobHardwareProfile.DRIVE_R_EN);
        assertEquals(18,HazRobHardwareProfile.DRIVE_R_IN1);
        assertEquals(27,HazRobHardwareProfile.DRIVE_R_IN2);
        assertEquals(25,HazRobHardwareProfile.MQ_RELAY_GPIO);
        assertEquals(8,HazRobHardwareProfile.MQ.length);
    }

    @Test public void environmentalBusIsIndependentFromAdsBus(){
        assertEquals(21,HazRobHardwareProfile.I2C_SDA_GPIO);
        assertEquals(22,HazRobHardwareProfile.I2C_SCL_GPIO);
        assertEquals(13,HazRobHardwareProfile.ENV_I2C_SDA_GPIO);
        assertEquals(14,HazRobHardwareProfile.ENV_I2C_SCL_GPIO);
        assertFalse(HazRobHardwareProfile.I2C_SDA_GPIO==HazRobHardwareProfile.ENV_I2C_SDA_GPIO);
        assertFalse(HazRobHardwareProfile.I2C_SCL_GPIO==HazRobHardwareProfile.ENV_I2C_SCL_GPIO);
        assertEquals(35,HazRobHardwareProfile.IR_GPIO);
        assertEquals(0,HazRobHardwareProfile.IR_GROUND_ACTIVE_LEVEL);
    }
}
