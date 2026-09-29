package com.servoc.app;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/** Source-level guard for MOD-04 ultrasonic distance overlay. */
public class EchoDistanceOverlayContractTest {
    private static String source(String relative) throws Exception {
        Path p=Paths.get(relative);
        if(!Files.exists(p))p=Paths.get("app").resolve(relative);
        if(!Files.exists(p))p=Paths.get("..").resolve(relative);
        return new String(Files.readAllBytes(p),StandardCharsets.UTF_8);
    }

    @Test public void landscapeDrivingShellInstallsEchoBadge() throws Exception {
        String app=source("app/src/main/java/com/servoc/app/HazRobApplication.java");
        assertTrue(app.contains("EchoDistanceBadge"));
        assertTrue(app.contains("hazrob_echo_distance_badge"));
        assertTrue(app.contains("Gravity.CENTER_HORIZONTAL|Gravity.TOP"));
    }

    @Test public void distanceSemanticsAreGreenAboveTenRedAtOrBelowTen() throws Exception {
        String badge=source("app/src/main/java/com/servoc/app/ui/EchoDistanceBadge.java");
        assertTrue(badge.contains("WARNING_CM=10.0"));
        assertTrue(badge.contains("cm<=WARNING_CM?0xFFFF5252:0xFF66BB6A"));
        assertTrue(badge.contains("ECHO — cm"));
        assertTrue(badge.contains("proximity.optDouble(\"cm\""));
    }
}
