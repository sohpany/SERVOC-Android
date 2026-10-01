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

    /** Accept the shared palette or an explicit equivalent dark surface; do not lock implementation spelling. */
    private static boolean darkWorkspace(String s){
        return s.contains("UiKit.NON_DRIVING_BG")||s.contains("0xFF0E1116")||
                s.contains("UiKit.HAZ_BLACK")||s.contains("UiKit.HAZ_SURFACE")||
                s.contains("0xFF11171D")||s.contains("0xFF151A20")||s.contains("0xFF171D24");
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
            assertTrue(name+" must declare/use a HazRob dark workspace surface",darkWorkspace(s));
            assertTrue(name+" must use shared readable UI primitives",s.contains("UiKit."));
        }
    }

    @Test public void developerSubWorkspacesAreAlsoDarkAndReadable() throws Exception {
        String[] files={
                "DeveloperExperimentsTab.java","AnalyticalLabTab.java","ChemCoreAnalysisTab.java",
                "DeveloperConnectionDiagnosticsTab.java","CentralLibraryDashboardTab.java","ChemCoreLibraryTab.java"
        };
        for(String name:files){
            String s=src("src/main/java/com/servoc/app/ui/"+name);
            assertTrue(name+" must use shared readable UI primitives",s.contains("UiKit."));
            assertTrue(name+" must inherit or declare a dark HazRob surface",darkWorkspace(s)||s.contains("UiKit.card("));
        }
    }

    @Test public void developerChemicalChooserHasExplicitDarkSurface() throws Exception {
        String s=src("src/main/java/com/servoc/app/ui/ChemicalExperimentMatrixDialog.java");
        assertTrue(s.contains("root.setBackgroundColor(UiKit.NON_DRIVING_BG)"));
        assertTrue(s.contains("UiKit.input"));
        assertTrue(s.contains("UiKit.small"));
    }

    @Test public void reportsAndMeasurementsUseReadableDarkCardsForDynamicText() throws Exception {
        String reports=src("src/main/java/com/servoc/app/ui/ReportsTab.java");
        String measurements=src("src/main/java/com/servoc/app/ui/MeasurementsTab.java");
        assertTrue(reports.contains("0xFF151A20"));
        assertTrue(reports.contains("0xFFECEFF1"));
        assertTrue(reports.contains("0xFFB0BEC5"));
        assertTrue(measurements.contains("0xFF171D24"));
        assertTrue(measurements.contains("0xFFECEFF1"));
        assertTrue(measurements.contains("0xFFB0BEC5"));
        assertTrue(darkWorkspace(reports));
        assertTrue(darkWorkspace(measurements));
    }

    @Test public void safetyPanelUsesDarkSharedCardAndReadableText() throws Exception {
        String s=src("src/main/java/com/servoc/app/ui/GasSafetyFindingsPanel.java");
        assertTrue(s.contains("root=UiKit.card(c)"));
        assertTrue(s.contains("list=UiKit.text"));
        assertTrue(s.contains("list.setBackgroundColor(0xFF11171D)"));
    }

    @Test public void drivingWorkspaceIsNotRepaintedByNonDrivingAudit() throws Exception {
        String drive=src("src/main/java/com/servoc/app/ui/ControlTab.java");
        assertFalse(drive.contains("applyNonDrivingContrast"));
        String kit=src("src/main/java/com/servoc/app/ui/UiKit.java");
        assertTrue(kit.contains("driving/video overlay"));
    }
}
