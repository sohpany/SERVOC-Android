package com.servoc.app;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import static org.junit.Assert.*;

/** CR-20261008-007: diagnostic artifact identity is not release authority. */
public final class HR069R1ArtifactManifestContractTest {
    private static String workflow(String file)throws Exception{
        return new String(Files.readAllBytes(Paths.get("../.github/workflows/"+file)),StandardCharsets.UTF_8);
    }

    @Test public void diagnosticWorkflowKeepsExactSourceAndRevision()throws Exception{
        String s=workflow("build-rc2-test-apk.yml");
        assertTrue(s.contains("authority_status=WORKING_UNAPPROVED_NON_AUTHORITATIVE"));
        assertTrue(s.contains("engineering_revision=HR069-R1"));
        assertTrue(s.contains("change_record=CR-20261008-007"));
        assertTrue(s.contains("source_sha=${GITHUB_SHA}"));
        assertTrue(s.contains("HazRob-0.10.2-HR069-R1-RC2-WORKING-"));
        assertTrue(s.contains("apk_sha256="));
        assertTrue(s.contains("signing_cert_sha256="));
        assertTrue(s.contains("apksigner verify"));
    }

    @Test public void internalBuildCannotPublishOrPromoteRelease()throws Exception{
        String s=workflow("build-apk.yml");
        assertTrue(s.contains("engineering_revision=HR069-R1"));
        assertTrue(s.contains("release=HazRob-0.10.2"));
        assertFalse(s.contains("uses: actions/upload-artifact@v4"));
        assertFalse(s.contains("uses: softprops/action-gh-release"));
    }
}
