package com.servoc.app.field;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import static org.junit.Assert.*;

/** Static contract guards for portable field-library restore semantics. */
public class HazRobUserLibraryContractTest {
    @Test public void restoreIsIdBasedAndNeverUsesNearestTimestamp() throws Exception {
        String s=new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/field/HazRobUserLibrary.java")),StandardCharsets.UTF_8);
        assertTrue(s.contains("CONFLICT_REPLACE"));
        assertTrue(s.contains("measurementExists(w,ms)"));
        assertTrue(s.contains("field_session_measurements"));
        assertFalse(s.contains("ORDER BY ABS"));
        assertFalse(s.contains("experiments WHERE"));
        assertFalse(s.contains("calibration_curves WHERE"));
        assertFalse(s.contains("reference_artifacts WHERE"));
    }
    @Test public void restoreSummaryCarriesDerivedExperimentCount() throws Exception {
        String s=new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/field/HazRobUserLibrary.java")),StandardCharsets.UTF_8);
        assertTrue(s.contains("measurements,experiments,samples,events,decisions"));
        assertTrue(s.contains("ax+=q.experiments"));
        assertTrue(s.contains("new RestoreSummary(ns,nf,nl,skip,am,ax,as,ae,ad)"));
    }
    @Test public void portableIndexContainsOnlyUserFieldCollections() throws Exception {
        String s=new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/field/HazRobUserLibrary.java")),StandardCharsets.UTF_8);
        assertTrue(s.contains("\"sites\""));
        assertTrue(s.contains("\"field_sessions\""));
        assertTrue(s.contains("\"measurement_links\""));
        assertFalse(s.contains("put(\"calibration"));
        assertFalse(s.contains("put(\"fingerprint"));
    }
}
