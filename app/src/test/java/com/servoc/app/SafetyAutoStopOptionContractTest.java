package com.servoc.app;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/** Source-level guard for MOD-03: warnings always active; automatic cliff/collision stop optional. */
public class SafetyAutoStopOptionContractTest {
    private static String source(String relative) throws Exception {
        Path p=Paths.get(relative);
        if(!Files.exists(p))p=Paths.get("app").resolve(relative);
        if(!Files.exists(p))p=Paths.get("..").resolve(relative);
        return new String(Files.readAllBytes(p),StandardCharsets.UTF_8);
    }

    @Test public void firmwareDefaultsAutoStopOffAndPersistsIt() throws Exception {
        String options=source("firmware/HazRob/SafetyOptions.cpp");
        assertTrue(options.contains("getBool(\"autoStop\",false)"));
        assertTrue(options.contains("putBool(\"autoStop\",enabled)"));
        assertTrue(options.contains("safetyNv.begin(\"hazsafe\",false)"));
    }

    @Test public void warningsRemainIndependentFromBlocking() throws Exception {
        String hw=source("firmware/HazRob/Hardware.cpp");
        assertTrue(hw.contains("Alerts::setIr(irWarning||ultraWarning)"));
        assertTrue(hw.contains("forwardBlocked=safetyAutoStopEnabled()&&(irWarning||ultraStop)"));
        assertTrue(hw.contains("forward_hazard"));
        assertTrue(hw.contains("stop_zone"));
    }

    @Test public void directAndRemoteCanReadAndSetTheSamePolicy() throws Exception {
        String ino=source("firmware/HazRob/HazRob.ino");
        String mqtt=source("firmware/HazRob/RemoteMqtt.cpp");
        String api=source("app/src/main/java/com/servoc/app/communication/RobotApi.java");
        assertTrue(ino.contains("/safety/auto_stop"));
        assertTrue(ino.contains("warnings_active"));
        assertTrue(mqtt.contains("safety_auto_stop_get"));
        assertTrue(mqtt.contains("safety_auto_stop_set"));
        assertTrue(api.contains("safetyAutoStopStatus()"));
        assertTrue(api.contains("setSafetyAutoStop(boolean enabled)"));
    }

    @Test public void settingsExposeTheOptionAndExplainWarningsStayActive() throws Exception {
        String settings=source("app/src/main/java/com/servoc/app/ui/SettingsTab.java");
        assertTrue(settings.contains("تفعيل التوقف التلقائي عند خطر السقوط أو الاصطدام"));
        assertTrue(settings.contains("التنبيهات تبقى فعالة حتى عند تعطيل التوقف التلقائي"));
        assertTrue(settings.contains("safetyAutoStopStatus()"));
        assertTrue(settings.contains("setSafetyAutoStop(autoStop)"));
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
