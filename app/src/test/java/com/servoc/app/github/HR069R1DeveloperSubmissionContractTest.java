package com.servoc.app.github;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import static org.junit.Assert.*;

/** Guards the owner-ratified RC2 scientific source authority against legacy library reintroduction. */
public final class HR069R1DeveloperSubmissionContractTest {
    private static String src(String path)throws Exception{
        return new String(Files.readAllBytes(Paths.get(path)),StandardCharsets.UTF_8);
    }

    @Test public void onlyDeveloperApprovedLineageMayEnterReviewInbox()throws Exception{
        String p=src("src/main/java/com/servoc/app/github/GitHubReferencePublisher.java");
        assertTrue(p.contains("ReferenceLineageGuard.valid(db,record)"));
        assertTrue(p.contains("ExperimentQueries.isDeveloperReference(db,record.id)"));
        assertTrue(p.contains("ReferenceLineageGuard.artifactSourcesValid(db,artifact)"));
        assertTrue(p.contains("derived content SHA-256 mismatch"));
        assertTrue(p.contains("submit")||p.contains("Stage HR069"));
        assertFalse(p.contains("public static Result publish(String token,ExperimentQueries.Record record)"));
        assertFalse(p.contains("REPOSITORY=\"HazRob/build-harness\""));
        assertFalse(p.contains("BRANCH=\"main\""));
    }

    @Test public void submittedDeveloperReferenceDoesNotBecomeStableSnapshotOrM4()throws Exception{
        assertEquals("sohpany/SERVOC-Android",GitHubReferencePublisher.REPOSITORY);
        assertEquals("release/hazrob-0.10.2-rc2",GitHubReferencePublisher.BRANCH);
        assertTrue(GitHubReferencePublisher.DIRECTORY.startsWith("research_data/hr069_developer_submissions/"));
        assertFalse(GitHubReferencePublisher.DIRECTORY.contains("hr069_analytical_library"));
        String exporter=src("src/main/java/com/servoc/app/report/ReferenceBankExporter.java");
        assertTrue(exporter.contains("m4_promotion_by_upload"));
        assertTrue(exporter.contains("runtime_chemcore_update"));
        String dev=src("src/main/java/com/servoc/app/ui/DeveloperExperimentsTab.java");
        assertTrue(dev.contains("STAGED_FOR_REVIEW"));
        assertTrue(dev.contains("لا يُحدَّث ChemCore"));
        assertTrue(dev.contains("ReferenceLineageGuard.valid"));
    }

    @Test public void stableUpdateRemainsSeparateFromDeveloperPublication()throws Exception{
        String manager=src("src/main/java/com/servoc/app/chemcore/library/ChemCoreLibraryManager.java");
        assertTrue(manager.contains("ChemCoreLibraryVerifier.verifyManifestOnly"));
        assertTrue(manager.contains("MeasurementSessionRegistry.chemCoreCaptureRequested()"));
        String gradle=src("../app/build.gradle");
        assertTrue(gradle.contains("CHEMCORE_LIBRARY_MANIFEST_URL"));
    }
}
