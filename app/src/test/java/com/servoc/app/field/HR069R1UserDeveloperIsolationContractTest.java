package com.servoc.app.field;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import static org.junit.Assert.*;

/** Locks separate USER-local and developer-only scientific central publication domains. */
public final class HR069R1UserDeveloperIsolationContractTest {
    private static String code(String path)throws Exception{
        return new String(Files.readAllBytes(Paths.get(path)),StandardCharsets.UTF_8);
    }

    @Test public void normalUserWorkspaceContainsNoPublishOrSync()throws Exception{
        String s=code("src/main/java/com/servoc/app/ui/FieldSessionsTab.java");
        assertFalse(s.contains("host.syncUserLibrary()"));
        assertFalse(s.contains("requestUserLibraryFolder()"));
        assertFalse(s.contains("requestReferenceBankExport"));
        assertTrue(s.contains("تخزين محلي"));
    }

    @Test public void measurementRecorderLinksOnlyUserFieldAndSeparatesDeveloper()throws Exception{
        String recorder=code("src/main/java/com/servoc/app/measurement/TelemetryRecorder.java");
        String reg=code("src/main/java/com/servoc/app/measurement/MeasurementSessionRegistry.java");
        assertTrue(recorder.contains("MeasurementSessionRegistry.consumeDeveloperMqCycle()"));
        assertTrue(recorder.contains("if(!developer&&!chemCore)"));
        assertTrue(recorder.contains("developer?\"DEVELOPER\":\"USER\""));
        assertTrue(reg.contains("markDeveloperMqCycle()"));
    }

    @Test public void developerCanPublishOnlyItsOwnVettedReferenceRecords()throws Exception{
        String queries=code("src/main/java/com/servoc/app/data/ExperimentQueries.java");
        String dev=code("src/main/java/com/servoc/app/ui/DeveloperExperimentsTab.java");
        assertTrue(queries.contains("m.operator_role='DEVELOPER'"));
        assertTrue(queries.contains("m.operator_role='USER' AND m.kind LIKE 'field_%'"));
        assertTrue(dev.contains("ExperimentQueries.recentDeveloper(host.database(),100)"));
        assertTrue(dev.contains("ExperimentQueries.isDeveloperReference(host.database(),r.id)"));
        assertTrue(dev.contains("ExperimentQueries.isDeveloperMeasurementSession(host.database(),session)"));
        assertTrue(dev.contains("!r.approved"));
    }
}
