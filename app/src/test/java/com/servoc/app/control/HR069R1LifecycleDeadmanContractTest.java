package com.servoc.app.control;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import static org.junit.Assert.*;

/** Robot motors must stop when Android UI loses focus, regardless of auto-stop preference. */
public final class HR069R1LifecycleDeadmanContractTest {
    @Test public void appPauseForcesZeroAndIndependentStop()throws Exception{
        String s=new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/MainActivity.java")),StandardCharsets.UTF_8);
        int pause=s.indexOf("protected void onPause(){");
        int destroy=s.indexOf("protected void onDestroy()",pause);
        assertTrue(pause>=0&&destroy>pause);
        String p=s.substring(pause,destroy);
        assertTrue(p.contains("left=right=0;"));
        assertTrue(p.contains("motionHistory.record(0,0,SystemClock.elapsedRealtime())"));
        assertTrue(p.contains("controlTab.resetJoystick()"));
        assertTrue(p.contains("queueDriveSend()"));
        assertTrue(p.contains("robotApi.motionStop()"));
    }

    @Test public void firmwareWatchdogStillIndependentlyEnforcesStop()throws Exception{
        String hw=new String(Files.readAllBytes(Paths.get("../firmware/HazRob/Hardware.cpp")),StandardCharsets.UTF_8);
        assertTrue(hw.contains("CONTROL_COMMAND_TIMEOUT_MS"));
        String safety=new String(Files.readAllBytes(Paths.get("../firmware/HazRob/SafetyOptions.cpp")),StandardCharsets.UTF_8);
        assertTrue(safety.contains("return autoStop;"));
        assertTrue(safety.contains("getBool(\"autoStop\",false)"));
    }
}
