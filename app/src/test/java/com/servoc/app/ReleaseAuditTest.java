package com.servoc.app;

import org.junit.Test;
import static org.junit.Assert.*;

public class ReleaseAuditTest {
    @Test public void v092KeepsCriticalCameraAndEvidenceContracts(){
        assertEquals("0.9.2-rc1",ReleaseAudit.VERSION);
        assertEquals(8,ReleaseAudit.DATABASE_SCHEMA);
        assertTrue(ReleaseAudit.VIDEO_RECORDING);
        assertTrue(ReleaseAudit.DIRECT_ESP_CAM_MJPEG);
        assertTrue(ReleaseAudit.CAMERA_ENDPOINT_FALLBACK);
        assertTrue(ReleaseAudit.FLAME_EVIDENCE_EXPERIMENTAL);
        assertTrue(ReleaseAudit.GAS_DIRECTION_ASSIST);
        assertTrue(ReleaseAudit.VIDEO_STANDALONE_FROM_REPORTS);
        assertFalse(ReleaseAudit.VIDEO_LINKED_TO_MEASUREMENTS);
        assertTrue(ReleaseAudit.API4_SPLIT_SAFETY);
        assertTrue(ReleaseAudit.BACKGROUND_MQ_CONTINUITY);
        assertTrue(ReleaseAudit.DERIVED_REFERENCE_ARTIFACTS);
        assertTrue(ReleaseAudit.EXACT_SESSION_REPORT_LINEAGE);
        assertTrue(ReleaseAudit.THREE_SECOND_LINK_RECOVERY_HOLD);
    }

    @Test public void v092UsesSingleMqPowerRelayAndUnifiedRemoteBoundary(){
        assertTrue(ReleaseAudit.DUAL_ADS1115);
        assertTrue(ReleaseAudit.SINGLE_MQ_POWER_RELAY);
        assertFalse(ReleaseAudit.TWO_MQ_POWER_BANKS);
        assertFalse(ReleaseAudit.FOUR_MQ_RELAY_ROWS);
        assertTrue(ReleaseAudit.MANUAL_MQ_RELAY_CONTROL);
        assertTrue(ReleaseAudit.MANUAL_MQ_WARMUP_SETTING);
        assertFalse(ReleaseAudit.GAS_TRACKING_RELAY_BANK_CONTROL);
        assertTrue(ReleaseAudit.GAS_TRACKING_RELAY_RECOMMENDATION);
        assertTrue(ReleaseAudit.ADS_AUTO_REPROBE);
        assertTrue(ReleaseAudit.REMOTE_CONNECTIVITY);
        assertTrue(ReleaseAudit.REMOTE_MQTT_TLS);
        assertTrue(ReleaseAudit.REMOTE_WSS_VIDEO);
    }
}
