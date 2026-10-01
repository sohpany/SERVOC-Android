package com.servoc.app;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import static org.junit.Assert.*;

/** Guards the high-contrast contract for every HazRob workspace except the driving/video workspace. */
public final class NonDrivingUiContrastContractTest {
    private static String src(String path)throws Exception{
        return new String(Files.readAllBytes(Paths.get(path)),StandardCharsets.UTF_8);
    }

    @Test public void sharedUiKitHasFailSafeContrastGuard() throws Exception {
        String s=src("src/main/java/com/servoc/app/ui/UiKit.java");
        assertTrue(s.contains("MIN_TEXT_CONTRAST=4.5"));
        assertTrue(s.contains("guardOnAttach(t)"));
        assertTrue(s.contains("guardOnAttach(b)"));
        assertTrue(s.contains("guardOnAttach(e)"));
        assertTrue(s.contains("effectiveBackground"));
        assertTrue(s.contains("contrastRatio(current,bg)<MIN_TEXT_CONTRAST"));
        assertTrue(s.contains("foregroundFor(bg)"));
        assertTrue(s.contains("applyNonDrivingContrast"));
    }

    @Test public void allPrimaryNonDrivingTabsDeclareDarkWorkspaceSurface() throws Exception {
        String[] files={
                "FieldSessionsTab.java","MeasurementsTab.java","ReportsTab.java",
                "StatusTab.java","SettingsTab.java","DeveloperTab.java"
        };
        for(String name:files){
            String s=src("src/main/java/com/servoc/app/ui/"+name);
            assertTrue(name+" must declare the HR069 dark workspace surface",s.contains("0xFF0E1116"));
        }
    }

    @Test public void developerSubWorkspacesAreAlsoDarkAndReadable() throws Exception {
        String[] files={
                "DeveloperExperimentsTab.java","AnalyticalLabTab.java",
                "DeveloperConnectionDiagnosticsTab.java","CentralLibraryDashboardTab.java"
        };
        for(String name:files){
            String s=src("src/main/java/com/servoc/app/ui/"+name);
            assertTrue(name+" must declare the HR069 dark workspace surface",s.contains("0xFF0E1116"));
            assertTrue(name+" must use shared readable UI primitives",s.contains("UiKit."));
        }
    }

    @Test public void reportsAndMeasurementsUseExplicitDarkCardsForDynamicText() throws Exception {
        String reports=src("src/main/java/com/servoc/app/ui/ReportsTab.java");
        String measurements=src("src/main/java/com/servoc/app/ui/MeasurementsTab.java");
        assertTrue(reports.contains("CARD=0xFF151A20"));
        assertTrue(reports.contains("TEXT=0xFFECEFF1"));
        assertTrue(reports.contains("MUTED=0xFFB0BEC5"));
        assertTrue(measurements.contains("CARD=0xFF171D24"));
        assertTrue(measurements.contains("TEXT=0xFFECEFF1"));
        assertTrue(measurements.contains("MUTED=0xFFB0BEC5"));
    }

    @Test public void drivingWorkspaceIsNotRepaintedByNonDrivingAudit() throws Exception {
        String drive=src("src/main/java/com/servoc/app/ui/ControlTab.java");
        assertFalse(drive.contains("applyNonDrivingContrast"));
        String kit=src("src/main/java/com/servoc/app/ui/UiKit.java");
        assertTrue(kit.contains("driving/video overlay"));
    }
}
