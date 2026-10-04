package com.servoc.app.report;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.*;

/** Prevents the operator SmartReport buttons from silently falling back to the legacy single-measurement path. */
public final class SmartReportUiRoutingContractTest {
    private static String read(String path)throws Exception{
        return new String(Files.readAllBytes(Paths.get(path)),StandardCharsets.UTF_8);
    }

    @Test public void reportsTabRoutesMappedMeasurementsToFieldSessionSmartReport()throws Exception{
        String s=read("src/main/java/com/servoc/app/ui/ReportsTab.java");
        assertTrue(s.contains("import com.servoc.app.field.FieldSessionRepository;"));
        assertTrue(s.contains("fieldSessions.fieldSessionIdForMeasurement(sid)"));
        assertTrue(s.contains("if(!fieldSessionId.isEmpty()){host.requestFieldSessionExport(f,fieldSessionId);return;}"));
        assertTrue("legacy export must remain only as a fallback for non-field measurements",s.contains("host.requestExport(f,selectedId);"));
        assertTrue(s.contains("تصدير PDF الذكي للجلسة"));
        assertTrue(s.contains("Excel ذكي للجلسة"));
    }

    @Test public void reverseLookupUsesOnlyExactMeasurementSessionMapping()throws Exception{
        String s=read("src/main/java/com/servoc/app/field/FieldSessionRepository.java");
        assertTrue(s.contains("fieldSessionIdForMeasurement(String measurementSessionId)"));
        assertTrue(s.contains("SELECT field_session_id FROM field_session_measurements WHERE measurement_session_id=?"));
        assertTrue(s.contains("new String[]{ms}"));
        assertTrue(s.contains("LIMIT 1"));
    }

    @Test public void fieldSessionsTabStillUsesSameSmartExporterEntryPoint()throws Exception{
        String s=read("src/main/java/com/servoc/app/ui/FieldSessionsTab.java");
        assertTrue(s.contains("host.requestFieldSessionExport(\"pdf\",q.id)"));
        assertTrue(s.contains("host.requestFieldSessionExport(\"xlsx\",q.id)"));
    }
}
