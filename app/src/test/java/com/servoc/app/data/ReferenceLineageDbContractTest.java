package com.servoc.app.data;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Static source contracts for HazRob 0.10.2 / HR069 reference lineage only. */
public class ReferenceLineageDbContractTest {

    @Test public void chemCoreApprovalBoundaryIsHr069FailClosed() throws Exception {
        String s=new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/chemcore/ChemCoreSchema.java")), StandardCharsets.UTF_8);
        assertTrue(s.contains("SCHEMA_VERSION = 5"));
        assertTrue(s.contains("trg_hr069_reference_approval_update"));
        assertTrue(s.contains("trg_hr069_reference_approval_insert"));
        assertTrue(s.contains("c.session_id=NEW.session_id"));
        assertTrue(s.contains("c.chemical_uid=NEW.chemical_uid"));
        assertTrue(s.contains("c.phase='ready_average'"));
        assertTrue(s.contains("m.operator_role='DEVELOPER'"));
        assertTrue(s.contains("m.kind LIKE 'developer_%'"));
        assertTrue(s.contains("m.ended_ts IS NOT NULL"));
        assertTrue(s.contains("COUNT(*) FROM telemetry_samples"));
        assertTrue(s.contains(">=2"));
        assertTrue(s.contains("NEW.reference_kind<>'calibration' OR NEW.known_ppm>0"));
        assertTrue(s.contains("HR069_REFERENCE_LINEAGE_INVALID"));
    }

    @Test public void javaGuardRequiresCompletedSampledExactHr069Capture() throws Exception {
        String s=new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/data/ReferenceLineageGuard.java")), StandardCharsets.UTF_8);
        assertTrue(s.contains("MIN_TELEMETRY_SAMPLES = 2"));
        assertTrue(s.contains("JOIN measurement_sessions m ON m.session_id=c.session_id"));
        assertTrue(s.contains("endedTs.isEmpty()||sampleCount<MIN_TELEMETRY_SAMPLES"));
        assertTrue(s.contains("session.equals(captureSession)"));
        assertTrue(s.contains("captureChemical!=r.chemicalUid"));
        assertTrue(s.contains("hazrob-reference-capture-v0.10.2"));
        assertTrue(s.contains("HazRobHardwareProfile.ID"));
        assertTrue(s.contains("DEVICE_LOCAL_NOT_DISTRIBUTED"));
        assertFalse(s.contains("SessionSeries.nearest"));
    }

    @Test public void derivedReferencesUseExactLineageGuard() throws Exception {
        String s=new String(Files.readAllBytes(Paths.get("src/main/java/com/servoc/app/reference/ReferenceArtifactBuilder.java")), StandardCharsets.UTF_8);
        assertTrue(s.contains("ReferenceLineageGuard.validById"));
        assertTrue(s.contains("c.session_id=e.session_id"));
        assertTrue(s.contains("c.chemical_uid=e.chemical_uid"));
        assertFalse(s.contains("SessionSeries.nearest"));
    }
}
