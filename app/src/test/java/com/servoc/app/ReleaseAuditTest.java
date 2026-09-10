package com.servoc.app;

import org.junit.Test;
import static org.junit.Assert.*;

public class ReleaseAuditTest {
    @Test public void v0101KeepsCriticalCameraEvidenceAndSafetyContracts(){
        assertEquals("0.10.1",ReleaseAudit.VERSION);
        assertEquals("شهاب الصهباني، مجدي ذيبان",ReleaseAudit.PROJECT_PREPARERS);
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

    @Test public void v0101RetainsL298nAndSingleMqPowerRelay(){
        assertTrue(ReleaseAudit.L298N_4WD_DRIVE);
        assertFalse(ReleaseAudit.SERVO_DRIVE);
        assertFalse(ReleaseAudit.PRECALIBRATED_MQ);
        assertTrue(ReleaseAudit.DUAL_ADS1115);
        assertTrue(ReleaseAudit.ADS_FIXED_5V_SUPPLY);
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
        assertTrue(ReleaseAudit.REMOTE_PASSWORDLESS_OPERATOR_UI);
        assertTrue(ReleaseAudit.REMOTE_ROLE_SCOPED_BOOTSTRAP);
        assertTrue(ReleaseAudit.MQTT_RETAINED_LAST_WILL);
    }

    @Test public void v0101KeepsChemCoreAndEndUserUpdateContracts(){
        assertTrue(ReleaseAudit.CHEMCORE_FOUNDATION);
        assertTrue(ReleaseAudit.CHEMCORE_CONTROLLED_RUNTIME);
        assertTrue(ReleaseAudit.CHEMCORE_DYNAMIC_LIBRARY);
        assertTrue(ReleaseAudit.CHEMCORE_MOLECULAR_INTELLIGENCE);
        assertTrue(ReleaseAudit.CHEMCORE_OPEN_SET);
        assertTrue(ReleaseAudit.CHEMCORE_AMCI);
        assertTrue(ReleaseAudit.CHEMCORE_LIBRARY_SIGNATURE_VERIFY);
        assertTrue(ReleaseAudit.END_USER_APP_UPDATER);
        assertTrue(ReleaseAudit.LDR_DIGITAL_THRESHOLD);
        assertTrue(ReleaseAudit.LOCAL_ALERT_OUTPUT);
        assertTrue(ReleaseAudit.GAS_RESPONSE_AUDIBLE_ALERT);
        assertTrue(ReleaseAudit.ALERT_TTL_FAILSAFE);
        assertFalse(ReleaseAudit.AUDIO_STREAMING);
        assertFalse(ReleaseAudit.AUDIO_VIDEO_RECORDING);
    }
}
