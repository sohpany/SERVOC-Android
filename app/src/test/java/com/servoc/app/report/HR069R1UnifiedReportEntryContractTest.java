package com.servoc.app.report;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import static org.junit.Assert.*;

/** Operator exports must never choose unrelated experiments or the old one-chart writer. */
public final class HR069R1UnifiedReportEntryContractTest {
    private static String src(String name)throws Exception{
        return new String(Files.readAllBytes(Paths.get(name)),StandardCharsets.UTF_8);
    }

    @Test public void genericOperatorExportRoutesOnlyExactLinkedFieldSession()throws Exception{
        String main=src("src/main/java/com/servoc/app/MainActivity.java");
        int start=main.indexOf("@Override public void requestExport(String format,long experimentId)");
        int next=main.indexOf("@Override public void requestFieldSessionExport",start);
        assertTrue(start>=0&&next>start);
        String method=main.substring(start,next);
        assertTrue(method.contains("fieldSessionIdForMeasurement(r.sessionId)"));
        assertTrue(method.contains("requestFieldSessionExport(format,fs)"));
        assertTrue(method.contains("selectedTab!=6"));
        assertFalse(method.contains("ExperimentQueries.recent(db,1)"));
    }

    @Test public void currentMeasurementExportUsesExactMeasurementSession()throws Exception{
        String measurements=src("src/main/java/com/servoc/app/ui/MeasurementsTab.java");
        assertTrue(measurements.contains("String sid=host.lastMeasurementSessionId()"));
        assertTrue(measurements.contains("ExperimentQueries.bySessionId(host.database(),sid)"));
        assertFalse(measurements.contains("ExperimentQueries.recent(host.database(),1)"));
    }

    @Test public void reportBundleIsConstructedAfterPickerOffMainThread()throws Exception{
        String main=src("src/main/java/com/servoc/app/MainActivity.java");
        int start=main.indexOf("@Override public void requestFieldSessionExport");
        int next=main.indexOf("@Override public void openReportsForSite",start);
        assertTrue(start>=0&&next>start);
        String ui=main.substring(start,next);
        assertTrue(ui.contains("fieldSessionById(fs)"));
        assertFalse(ui.contains("FieldSessionReportExporter.resolve("));
        assertTrue(main.contains("workers.execute(()->"));
        String site=src("src/main/java/com/servoc/app/SiteSummaryExportActivity.java");
        assertTrue(site.contains("new Thread(()->"));
        assertFalse(site.contains("try(ExperimentsDb db=new ExperimentsDb(this)){SiteSummaryReportExporter.resolve"));
    }
}
