package com.servoc.app;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import static org.junit.Assert.*;

/** Source-level UX guardrails. Passing these is NOT a device field acceptance test. */
public final class HR069R1FieldUXRegressionsTest {
    private static String source(String relativePath)throws Exception {
        return new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/"+relativePath)),
                StandardCharsets.UTF_8);
    }
    @Test public void bmeEnvironmentAlwaysUsesCurrentLiveTelemetry()throws Exception {
        String code=source("ui/MeasurementsTab.java");
        assertTrue(code.contains("if(e!=null&&e.optBoolean(\"ok\",false)){"));
        assertTrue(code.contains("envDiagnostic.setText(envDiagnosticText(e))"));
        assertFalse(code.contains("lastValidEnv"));
        assertFalse(code.contains("staleEnv"));
        assertFalse(code.contains("آخر قراءة صالحة (قديمة)"));
    }
    @Test public void fastRefreshDoesNotRebuildHiddenCandidateViews()throws Exception {
        String code=source("ui/MeasurementsTab.java");
        assertTrue(code.contains("switch(selected){case 0:physical.refresh()"));
        assertTrue(code.contains("candidateList.removeAllViews();"));
        assertTrue(code.contains("lastRenderedAt<750L"));
    }
    @Test public void driveAxesRemainIndependentAndSendPairedCommands()throws Exception {
        String sliders=source("control/DualTankDriveControl.java");
        String control=source("ui/ControlTab.java");
        assertTrue(sliders.contains("update(e.getY(ix))"));
        assertTrue(sliders.contains("setValue(Math.round((mid-y)*100f/half),true)"));
        assertTrue(control.contains("tankLeftRaw=v;if(driveMode==DriveMode.TANK)applyTankDrive()"));
        assertTrue(control.contains("tankRightRaw=v;if(driveMode==DriveMode.TANK)applyTankDrive()"));
        assertTrue(control.contains("host.setDriveCommand(tankLeftRaw,tankRightRaw)"));
    }
    @Test public void siteAndSessionScopesAreHierarchical()throws Exception {
        String code=source("ui/FieldSessionsTab.java");
        assertTrue(code.contains("siteDropdown=new Spinner(c)"));
        assertTrue(code.contains("sessionDropdown=new Spinner(c)"));
        assertTrue(code.contains("repo.sessionsForSite(selectedSiteId)"));
        assertTrue(code.contains("selectedSessionId=all.get(pos).id"));
    }
    @Test public void reportsDoNotAllocateBlankChartsForMissingEvidence()throws Exception {
        String code=source("ui/ReportsTab.java");
        assertTrue(code.contains("availableCharts.clear()"));
        assertTrue(code.contains("chartView.setVisibility(View.GONE)"));
        assertTrue(code.contains("host.io().execute(()->{"));
        assertTrue(code.contains("if((needsChem&&chemOk)||(!needsChem&&seriesOk))"));
    }
    @Test public void echoAndDeveloperCalibrationRemainsClear()throws Exception {
        String control=source("ui/ControlTab.java");
        String status=source("ui/StatusTab.java");
        String experiments=source("ui/DeveloperExperimentsTab.java");
        assertTrue(control.contains("ECHO لا قياس • timeout"));
        assertTrue(status.contains("HIGH=أرض"));
        assertFalse(experiments.contains("r0.setOnClickListener(v->openR0Maintenance())"));
        assertTrue(experiments.contains("تبويب المطور «معايرة R0»"));
    }
}
