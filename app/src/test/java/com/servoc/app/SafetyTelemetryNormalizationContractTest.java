package com.servoc.app;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Cross-layer source guard: HR069 hazard/interlock telemetry must survive Android normalization. */
public class SafetyTelemetryNormalizationContractTest {
    private static String source(String relative) throws Exception {
        Path p=Paths.get(relative);
        if(!Files.exists(p))p=Paths.get("app").resolve(relative);
        if(!Files.exists(p))p=Paths.get("..").resolve(relative);
        return new String(Files.readAllBytes(p),StandardCharsets.UTF_8);
    }

    @Test public void firmwarePublishesHazardAndBlockingState() throws Exception {
        String hw=source("firmware/HazRob/Hardware.cpp");
        assertTrue(hw.contains("safety_auto_stop"));
        assertTrue(hw.contains("forward_hazard"));
        assertTrue(hw.contains("forward_blocked"));
        assertTrue(hw.contains("forward_block_reason"));
        assertTrue(hw.contains("stop_zone"));
        assertTrue(hw.contains("auto_stop_enabled"));
        assertTrue(hw.contains("cliff_warning"));
        assertTrue(hw.contains("ground_detected"));
    }

    @Test public void androidNormalizationPreservesSafetySemantics() throws Exception {
        String p=source("app/src/main/java/com/servoc/app/communication/RobotProtocolV4.java");
        assertTrue(p.contains("drive.put(\"safety_auto_stop\",safetyAutoStop)"));
        assertTrue(p.contains("drive.put(\"forward_hazard\",forwardHazard)"));
        assertTrue(p.contains("drive.put(\"forward_blocked\",forwardBlocked)"));
        assertTrue(p.contains("drive.put(\"forward_block_reason\",forwardBlockReason)"));
        assertTrue(p.contains("proximity.put(\"stop_zone\""));
        assertTrue(p.contains("proximity.put(\"auto_stop_enabled\""));
        assertTrue(p.contains("proximity.put(\"forward_blocked\""));
        assertTrue(p.contains("x.put(\"cliff_warning\",cliff)"));
        assertTrue(p.contains("x.put(\"ground_detected\""));
        assertTrue(p.contains("x.put(\"drive_interlock\",safetyAutoStop&&cliff)"));
        assertFalse(p.contains("x.put(\"drive_interlock\",false)"));
    }
}
