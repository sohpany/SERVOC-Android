package com.servoc.app;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/** Source guard for landscape ECHO and operational gas warning overlays. */
public class DrivingHazardOverlayContractTest {
    private static String source(String relative) throws Exception {
        Path p=Paths.get(relative);
        if(!Files.exists(p))p=Paths.get("app").resolve(relative);
        if(!Files.exists(p))p=Paths.get("..").resolve(relative);
        return new String(Files.readAllBytes(p),StandardCharsets.UTF_8);
    }

    @Test public void landscapeInstallsBothDrivingBadges() throws Exception {
        String app=source("app/src/main/java/com/servoc/app/HazRobApplication.java");
        assertTrue(app.contains("installEchoDistanceBadge(activity,pageGroup)"));
        assertTrue(app.contains("installGasHazardBadge(activity,pageGroup)"));
        assertTrue(app.contains("hazrob_echo_distance_badge"));
        assertTrue(app.contains("hazrob_gas_hazard_badge"));
    }

    @Test public void echoDistanceUsesTenCentimeterWarningThreshold() throws Exception {
        String echo=source("app/src/main/java/com/servoc/app/ui/EchoDistanceBadge.java");
        assertTrue(echo.contains("WARNING_CM=10.0"));
        assertTrue(echo.contains("ECHO — cm"));
        assertTrue(echo.contains("cm<=WARNING_CM?0xFFFF5252:0xFF66BB6A"));
    }

    @Test public void gasBadgeUsesOperationalAlertLevelAndHidesWhenClear() throws Exception {
        String gas=source("app/src/main/java/com/servoc/app/ui/GasHazardBadge.java");
        assertTrue(gas.contains("optInt(\"gas\",0)"));
        assertTrue(gas.contains("if(gas<=0)"));
        assertTrue(gas.contains("⚠ تحذير غاز"));
        assertTrue(gas.contains("⚠ خطر غاز مرتفع"));
        assertTrue(gas.contains("does not imply calibrated ppm or chemical identity"));
    }
}
