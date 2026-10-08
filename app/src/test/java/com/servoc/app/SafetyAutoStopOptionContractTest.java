package com.servoc.app;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static org.junit.Assert.*;

/** HR069-R1: E-STOP/watchdog independent, IR/Echo auto-stop is explicit/persistent. */
public class SafetyAutoStopOptionContractTest {
    private static String source(String path)throws Exception{
        Path p=Paths.get(path);if(!Files.exists(p))p=Paths.get("app").resolve(path);
        if(!Files.exists(p))p=Paths.get("..").resolve(path);
        return new String(Files.readAllBytes(p),StandardCharsets.UTF_8);
    }

    @Test public void firmwarePersistsOperatorSettingWithDefaultOff()throws Exception{
        String s=source("firmware/HazRob/SafetyOptions.cpp");
        assertTrue(s.contains("safetyNv.begin(\"hazsafe\",false)"));
        assertTrue(s.contains("safetyNv.getBool(\"autoStop\",false)"));
        assertTrue(s.contains("safetyNv.putBool(\"autoStop\",enabled)"));
        assertTrue(s.contains("return autoStop;"));
        assertFalse(s.contains("(void)enabled"));
    }

    @Test public void firmwareHighMeansGroundAndLowMeansNoGround()throws Exception{
        String cfg=source("firmware/HazRob/Config.h");
        String hw=source("firmware/HazRob/Hardware.cpp");
        assertTrue(cfg.contains("IR_GROUND_ACTIVE=HIGH"));
        assertTrue(hw.contains("bool groundNow=irLevel==Cfg::IR_GROUND_ACTIVE"));
        assertTrue(hw.contains("irGroundDetected=false;irWarning=true"));
        assertFalse(hw.contains("irVerified"));
        assertFalse(hw.contains("irSeenHigh"));
    }

    @Test public void directAndRemoteSafetyControlsRemainAvailable()throws Exception{
        String web=source("firmware/HazRob/HazRob.ino");
        String mqtt=source("firmware/HazRob/RemoteMqtt.cpp");
        String api=source("app/src/main/java/com/servoc/app/communication/RobotApi.java");
        assertTrue(web.contains("web.on(\"/safety/auto_stop\""));
        assertTrue(web.contains("safetyAutoStopSet(v==\"1\",true)"));
        assertTrue(mqtt.contains("safety_auto_stop_get"));
        assertTrue(mqtt.contains("safety_auto_stop_set"));
        assertTrue(api.contains("safetyAutoStopStatus()"));
        assertTrue(api.contains("setSafetyAutoStop(boolean enabled)"));
    }

    @Test public void settingsAllowOperatorEnableOrDisable()throws Exception{
        String settings=source("app/src/main/java/com/servoc/app/ui/SettingsTab.java");
        assertTrue(settings.contains("private void applySafetyAutoStop(boolean enabled)"));
        assertTrue(settings.contains("host.robotApi().setSafetyAutoStop(enabled)"));
        assertTrue(settings.contains("showSafetyAutoStop(status.optBoolean(\"enabled\",false))"));
        assertFalse(settings.contains("host.robotApi().setSafetyAutoStop(false)"));
        String hw=source("firmware/HazRob/Hardware.cpp");
        assertTrue(hw.contains("forwardBlocked=safetyAutoStopEnabled()&&(irWarning||ultraStop)"));
    }

    @Test public void emergencyStopAndWatchdogStayIndependent()throws Exception{
        String hw=source("firmware/HazRob/Hardware.cpp");
        String web=source("firmware/HazRob/HazRob.ino");
        assertTrue(hw.contains("CONTROL_COMMAND_TIMEOUT_MS"));
        assertTrue(web.contains("/estop"));
        assertTrue(web.contains("/motion/stop"));
        assertTrue(web.contains("watchdog_independent"));
        assertTrue(web.contains("estop_independent"));
    }
}
