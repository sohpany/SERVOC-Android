package com.servoc.app;

import org.junit.Test;

import static org.junit.Assert.*;

public class ReleaseAuditTest {

    @Test public void v08KeepsCriticalCameraAndEvidenceContracts(){
        assertEquals("0.8.0",ReleaseAudit.VERSION);
        assertTrue(ReleaseAudit.VIDEO_RECORDING);
        assertTrue(ReleaseAudit.DIRECT_ESP_CAM_MJPEG);
        assertTrue(ReleaseAudit.CAMERA_ENDPOINT_FALLBACK);
        assertTrue(ReleaseAudit.FLAME_EVIDENCE_EXPERIMENTAL);
        assertTrue(ReleaseAudit.GAS_DIRECTION_ASSIST);
        assertTrue(ReleaseAudit.VIDEO_STANDALONE_FROM_REPORTS);
        assertFalse(ReleaseAudit.VIDEO_LINKED_TO_MEASUREMENTS);
    }

    @Test public void v08KeepsMqPowerAndTrackingManual(){
        assertTrue(ReleaseAudit.DUAL_ADS1115);
        assertTrue(ReleaseAudit.TWO_MQ_POWER_BANKS);
        assertFalse(ReleaseAudit.FOUR_MQ_RELAY_ROWS);
        assertTrue(ReleaseAudit.MANUAL_MQ_RELAY_CONTROL);
        assertTrue(ReleaseAudit.MANUAL_MQ_WARMUP_SETTING);
        assertFalse(ReleaseAudit.GAS_TRACKING_RELAY_BANK_CONTROL);
        assertTrue(ReleaseAudit.GAS_TRACKING_RELAY_RECOMMENDATION);
    }
}
