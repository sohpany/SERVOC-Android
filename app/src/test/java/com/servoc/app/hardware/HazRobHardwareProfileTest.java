package com.servoc.app.hardware;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/** Locks the Android hardware identity to the controlled HazRob 0.10.1 GPIO/firmware profile. */
public final class HazRobHardwareProfileTest {
    @Test public void profileIdentityMatchesCanonicalContract(){
        assertEquals("hazrob-0.10.1-1r8mq-l298n6-batt-ultra-ldr-env2-v2",HazRobHardwareProfile.ID);
    }

    @Test public void driveAndMqAuthorityRemainPinned(){
        assertEquals("L298N",HazRobHardwareProfile.DRIVE_DRIVER);
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
    }

    @Test public void installedSafetyAndSensorPinsRemainPinned(){
        assertEquals(26,HazRobHardwareProfile.ALERT_GPIO);
        assertEquals(34,HazRobHardwareProfile.LDR_GPIO);
        assertEquals(35,HazRobHardwareProfile.IR_GPIO);
        assertEquals(0,HazRobHardwareProfile.IR_GROUND_ACTIVE_LEVEL);
        assertEquals(36,HazRobHardwareProfile.SOUND_GPIO);
        assertEquals(33,HazRobHardwareProfile.BATTERY_ADC_GPIO);
        assertEquals(23,HazRobHardwareProfile.ULTRASONIC_TRIG_GPIO);
        assertEquals(39,HazRobHardwareProfile.ULTRASONIC_ECHO_GPIO);
    }
}
