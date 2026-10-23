package com.servoc.app.report;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import static org.junit.Assert.*;

/** HR069-R1: reports are either one user's Field Session or its complete parent site. */
public final class SmartReportUiRoutingContractTest {
    private static String read(String path)throws Exception{
        return new String(Files.readAllBytes(Paths.get(path)),StandardCharsets.UTF_8);
    }

    @Test public void centralReportSelectorIsOneSessionOrFullSite()throws Exception{
        String s=read("src/main/java/com/servoc/app/ui/ReportsTab.java");
        assertTrue(s.contains("fieldSessions.approvedSessionsForSite(chosenSiteId)"));
        assertTrue(s.contains("SiteSummaryExportActivity.start(scroll.getContext(),chosenSiteId,format)"));
        assertTrue(s.contains("host.requestFieldSessionExport(format,chosenSessionId)"));
        assertTrue(s.contains("كل الجلسات المعتمدة"));
        assertFalse(s.contains("host.requestExport(f,selectedId);"));
        assertTrue(s.contains("ExperimentQueries.recentUser(host.database(),100)"));
    }

    @Test public void userSessionTabRoutesThroughSameReportScope()throws Exception{
        String s=read("src/main/java/com/servoc/app/ui/FieldSessionsTab.java");
        assertTrue(s.contains("host.requestFieldSessionExport(format,reportApprovedId)"));
        assertTrue(s.contains("host.openReportsForSite(selectedSiteId,\"\")"));
        assertFalse(s.contains("host.syncUserLibrary()"));
        assertFalse(s.contains("requestUserLibraryFolder()"));
    }

    @Test public void siteExporterIncludesEverySiteSessionWithoutSubsetFilter()throws Exception{
        String exporter=read("src/main/java/com/servoc/app/report/SiteSummaryReportExporter.java");
        assertTrue(exporter.contains("repo.approvedSessionsForSite(id)"));
        String field=read("src/main/java/com/servoc/app/field/FieldSessionRepository.java");
        assertTrue(field.contains("fieldSessionIdForMeasurement(String measurementSessionId)"));
        assertTrue(field.contains("SELECT field_session_id FROM field_session_measurements WHERE measurement_session_id=?"));
    }

    @Test public void heavySiteExportPreservesSafActivityAndRunsOffMainThread()throws Exception{
        String manifest=read("src/main/AndroidManifest.xml");
        assertFalse(manifest.contains("android:noHistory=\"true\""));
        String writer=read("src/main/java/com/servoc/app/SiteSummaryExportActivity.java");
        assertTrue(writer.contains("new Thread(()->"));
        assertTrue(writer.contains("SmartSiteXlsxWriter.write"));
        assertTrue(writer.contains("SiteSummaryReportExporter.writePdf"));
    }
}
