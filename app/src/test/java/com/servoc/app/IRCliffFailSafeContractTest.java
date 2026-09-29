package com.servoc.app;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/** Source-level guard for the HR069 downward-IR cliff fail-safe contract. */
public class IRCliffFailSafeContractTest {
    private static String source(String relative) throws Exception {
        Path p=Paths.get(relative);
        if(!Files.exists(p))p=Paths.get("app").resolve(relative);
        if(!Files.exists(p))p=Paths.get("..").resolve(relative);
        return new String(Files.readAllBytes(p),StandardCharsets.UTF_8);
    }

    @Test public void groundPresencePolarityAndDebounceAreExplicit() throws Exception {
        String cfg=source("firmware/HazRob/Config.h");
        assertTrue(cfg.contains("IR_GROUND_ACTIVE=LOW"));
        assertTrue(cfg.contains("IR_FAIL_SAFE_STARTUP=true"));
        assertTrue(cfg.contains("IR_CLIFF_ASSERT_SAMPLES=3"));
        assertTrue(cfg.contains("IR_CLIFF_CLEAR_SAMPLES=2"));
        assertTrue(cfg.contains("IR_PERIOD=50"));
    }

    @Test public void startupIsBlockedUntilGroundIsConfirmed() throws Exception {
        String hw=source("firmware/HazRob/Hardware.cpp");
        assertTrue(hw.contains("irWarning=Cfg::IR_FAIL_SAFE_STARTUP,irGroundDetected=false"));
        assertTrue(hw.contains("groundNow=irLevel==Cfg::IR_GROUND_ACTIVE"));
        assertTrue(hw.contains("irGroundDetected=true;irWarning=false"));
        assertTrue(hw.contains("irGroundDetected=false;irWarning=true"));
    }

    @Test public void cliffBlocksOnlyNetForwardComponent() throws Exception {
        String hw=source("firmware/HazRob/Hardware.cpp");
        assertTrue(hw.contains("forwardRequested=(requestedL+requestedR)>0.1f"));
        assertTrue(hw.contains("forwardBlocked=irWarning||ultraStop"));
        assertTrue(hw.contains("if(forwardBlocked&&forwardRequested){curL=curR=0;}else{curL=requestedL;curR=requestedR;}"));
        assertTrue(hw.contains("irWarning?\"cliff\":ultraStop?\"collision\""));
    }
}
