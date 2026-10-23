package com.servoc.app;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import static org.junit.Assert.*;

/**
 * Developer workflow smoke contracts checked at compile/test time.
 * This is source-based coverage: it does not simulate Android touch, a real phone,
 * GitHub credentials, live Wi-Fi, a reference chemical or physical sensors.
 */
public final class HR069R1DeveloperWorkflowSmokeContractTest {
    private static String source(String path)throws Exception{
        return new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/"+path)),StandardCharsets.UTF_8);
    }

    @Test public void allFiveDeveloperPagesExistAndOnlyUnlockAfterCorrectPin()throws Exception{
        String s=source("ui/DeveloperTab.java");
        assertTrue(s.contains("RoleGate.DEVELOPER_PIN.equals(pin.getText().toString().trim())"));
        assertTrue(s.contains("experiments=new DeveloperExperimentsTab("));
        assertTrue(s.contains("chemCore=new AnalyticalLabTab("));
        assertTrue(s.contains("r0=new DeveloperR0Tab("));
        assertTrue(s.contains("connectionSetup=new DeveloperConnectionDiagnosticsTab("));
        assertTrue(s.contains("library=new CentralLibraryDashboardTab("));
        assertTrue(s.contains("\"التجارب\",\"المختبر\",\"معايرة R0\",\"الاتصال\",\"المكتبة\""));
        assertTrue(s.contains("if(!unlocked||views==null)return"));
        assertTrue(s.contains("if(selected==0&&experiments!=null)"));
        assertTrue(s.contains("else if(selected==4&&library!=null)"));
    }

    @Test public void experimentsCannotPromoteUnreviewedOrUserLocalData()throws Exception{
        String s=source("ui/DeveloperExperimentsTab.java");
        assertTrue(s.contains("referenceKindSpinner"));
        assertTrue(s.contains("knownPpm"));
        assertTrue(s.contains("MeasurementSessionRegistry.markDeveloperMqCycle()"));
        assertTrue(s.contains("host.robotApi().startMeasurement()"));
        assertTrue(s.contains("host.robotApi().cancelMeasurement()"));
        assertTrue(s.contains("ReferenceLineageGuard.valid("));
        assertTrue(s.contains("ExperimentQueries.isDeveloperReference("));
        assertTrue(s.contains("recentDeveloper(host.database(),100)"));
        assertTrue(s.contains("STAGED_FOR_REVIEW"));
        assertTrue(s.contains("لا يُحدَّث ChemCore"));
        assertFalse(s.contains("r0.setOnClickListener(v->openR0Maintenance())"));
    }

    @Test public void localR0SaveAndResetInterlockedDuringActiveCapture()throws Exception{
        String r0=source("ui/DeveloperR0Tab.java");
        assertTrue(r0.contains("calibrationChangeBlocked()"));
        assertTrue(r0.contains("ChemCoreLibraryManager.measurementOrAnalysisActive(host.latestDiagnostics())"));
        assertTrue(r0.contains("ManualR0Calibration.save("));
        assertTrue(r0.contains("ManualR0Calibration.reset("));
        assertTrue(r0.contains("ManualR0Calibration.saveAll("));
        assertTrue(r0.contains("kΩ"));
        assertTrue(r0.contains("1000 Ω = 1 kΩ"));
    }

    @Test public void labReportsCannotCallResearchScreeningQuantifiedPpmWithoutM4()throws Exception{
        String lab=source("ui/AnalyticalLabTab.java");
        assertTrue(lab.contains("M4"));
        assertTrue(lab.contains("ChemCoreLabQuality.evaluate("));
        assertTrue(lab.contains("MeasurementSessionRegistry.lastCompletedSessionId()"));
        assertTrue(lab.contains("decisions.latestPayloadForSession(id)"));
        assertTrue(lab.contains("ChemCoreAnalysisPresenter"));
        String reports=source("report/SmartFieldSessionPdfWriter.java");
        assertTrue(reports.contains("M4 وحده يسمح بالقياس الكمي المهني"));
        assertTrue(reports.contains("Tracking غير مدرج في التقرير"));
    }

    @Test public void remoteCommandsAreDeveloperScopedAndObservable()throws Exception{
        String s=source("ui/DeveloperConnectionDiagnosticsTab.java");
        assertTrue(s.contains("host.robotApi().remoteStatus()"));
        assertTrue(s.contains("host.robotApi().remoteAuthorize(enabled)"));
        assertTrue(s.contains("host.robotApi().remoteDisconnect()"));
        assertTrue(s.contains("host.robotApi().remoteReconnect()"));
        assertTrue(s.contains("host.robotApi().remoteClear()"));
        assertTrue(s.contains("First failure"));
        assertTrue(s.contains("host.io().execute("));
    }

    @Test public void libraryUpdatesAreVersionedAndBlockedDuringCapture()throws Exception{
        String s=source("ui/CentralLibraryDashboardTab.java");
        assertTrue(s.contains("ChemCoreLibraryManager"));
        assertTrue(s.contains("measurementOrAnalysisActive(host.latestDiagnostics())"));
        assertTrue(s.contains("manager.rollback("));
        assertTrue(s.contains("getMeta(\"chemcore_update_telemetry_json\""));
        String mgr=source("chemcore/library/ChemCoreLibraryManager.java");
        assertTrue(mgr.contains("ChemCoreLibraryVerifier.verifyManifestOnly"));
    }

    @Test public void reportPairExistsWithPersistedScientificProvenance()throws Exception{
        String pdf=source("report/SmartFieldSessionPdfWriter.java");
        String xlsx=source("report/SmartFieldSessionXlsxWriter.java");
        assertTrue(pdf.contains("FieldSessionReportExporter.resolve("));
        assertTrue(pdf.contains("ReportIntelligence.plan("));
        assertTrue(pdf.contains("AnalyticalCharts"));
        assertTrue(xlsx.contains("SmartXlsxPackage.Sheet("));
        assertTrue(xlsx.contains("SmartReportAnalyticalContract"));
        assertTrue(xlsx.contains("السلسلة الزمنية"));
        assertTrue(xlsx.contains("عقد التحليل"));
    }

    @Test public void rawEchoDiagnosticsAndDirectBmeTelemetryPropagateToUi()throws Exception{
        String protocol=source("communication/RobotProtocolV4.java");
        String status=source("ui/StatusTab.java");
        String measurements=source("ui/MeasurementsTab.java");
        for(String field:new String[]{"no_rise","no_fall","stuck_high","invalid_pulse","echo_level","bus_recoveries","total_read_errors"})
            assertTrue("Missing telemetry field "+field,protocol.contains(field));
        assertTrue(status.contains("echoReading.status"));
        assertTrue(measurements.contains("BME direct GPIO13/14"));
        assertFalse(measurements.contains("lastValidEnv"));
        assertFalse(measurements.contains("staleEnv"));
    }
}
