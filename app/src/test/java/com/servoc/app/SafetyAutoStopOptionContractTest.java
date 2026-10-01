package com.servoc.app;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Source-level guard for RC2: IR/Echo warnings stay active while automatic motion stopping is disabled. */
public class SafetyAutoStopOptionContractTest {
    private static String source(String relative) throws Exception {
        Path p=Paths.get(relative);
        if(!Files.exists(p))p=Paths.get("app").resolve(relative);
        if(!Files.exists(p))p=Paths.get("..").resolve(relative);
        return new String(Files.readAllBytes(p),StandardCharsets.UTF_8);
    }

    @Test public void firmwareForcesAutoStopOffAndPersistsThatState() throws Exception {
        String options=source("firmware/HazRob/SafetyOptions.cpp");
        assertTrue(options.contains("safetyNv.begin(\"hazsafe\",false)"));
        assertTrue(options.contains("safetyNv.putBool(\"autoStop\",false)"));
        assertTrue(options.contains("bool safetyAutoStopEnabled()"));
        assertTrue(options.contains("return false;"));
        assertFalse(options.contains("putBool(\"autoStop\",enabled)"));
    }

    @Test public void warningsRemainIndependentFromBlocking() throws Exception {
        String hw=source("firmware/HazRob/Hardware.cpp");
        assertTrue(hw.contains("Alerts::setIr(irWarning||ultraWarning)"));
        assertTrue(hw.contains("forwardBlocked=safetyAutoStopEnabled()&&(irWarning||ultraStop)"));
        assertTrue(hw.contains("forward_hazard"));
        assertTrue(hw.contains("stop_zone"));
    }

    @Test public void compatibilityEndpointsRemainButCannotReEnableAutomaticStopping() throws Exception {
        String ino=source("firmware/HazRob/HazRob.ino");
        String mqtt=source("firmware/HazRob/RemoteMqtt.cpp");
        String api=source("app/src/main/java/com/servoc/app/communication/RobotApi.java");
        String options=source("firmware/HazRob/SafetyOptions.cpp");
        assertTrue(ino.contains("/safety/auto_stop"));
        assertTrue(ino.contains("warnings_active"));
        assertTrue(mqtt.contains("safety_auto_stop_get"));
        assertTrue(mqtt.contains("safety_auto_stop_set"));
        assertTrue(api.contains("safetyAutoStopStatus()"));
        assertTrue(api.contains("setSafetyAutoStop(boolean enabled)"));
        assertTrue(options.contains("(void)enabled"));
        assertTrue(options.contains("safetyNv.putBool(\"autoStop\",false)"));
    }

    @Test public void settingsShowWarningOnlyPolicyAndDoNotOfferEnablePath() throws Exception {
        String settings=source("app/src/main/java/com/servoc/app/ui/SettingsTab.java");
        assertTrue(settings.contains("التوقف التلقائي للحركة: ملغي — تنبيه فقط"));
        assertTrue(settings.contains("safetyAutoStop.setEnabled(false)"));
        assertFalse(settings.contains("تفعيل التوقف التلقائي عند خطر السقوط أو الاصطدام"));
        assertFalse(settings.contains("setSafetyAutoStop(autoStop)"));
    }

    @Test public void emergencyStopAndWatchdogRemainIndependent() throws Exception {
        String hw=source("firmware/HazRob/Hardware.cpp");
        String ino=source("firmware/HazRob/HazRob.ino");
        assertTrue(hw.contains("CONTROL_COMMAND_TIMEOUT_MS"));
        assertTrue(ino.contains("/estop"));
        assertTrue(ino.contains("/motion/stop"));
        assertTrue(ino.contains("watchdog_independent"));
        assertTrue(ino.contains("estop_independent"));
    }
}
