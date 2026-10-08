package com.servoc.app.chemcore.library;

import com.servoc.app.chemcore.SamplingProfile;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import static org.junit.Assert.*;

/** Offline scientific-function regression: Embedded Stable, not a references-only catalog. */
public final class HR069EmbeddedStableContractTest {
    private static final String DIR="build/generated/hazrobAssets/generated/chemcore/embedded_stable/";

    @Test public void bundledStableHasExactlyCompatibleVerifiedManifest()throws Exception{
        File payload=new File(DIR+"chemcore-stable-snapshot.json");
        JSONObject manifestJson=new JSONObject(Files.readString(Paths.get(DIR+"manifest.json"),StandardCharsets.UTF_8));
        ChemCoreLibraryManifest manifest=ChemCoreLibraryManifest.fromJson(manifestJson);
        assertEquals("1.0.0-hr069r1",manifest.libraryVersion);
        assertEquals(ChemCoreLibraryManifest.Channel.STABLE,manifest.channel);
        assertEquals("chemcore-stable-snapshot-v1",manifest.payloadSchema);
        assertEquals(652,manifest.chemicalCount);
        assertEquals(0,manifest.calibrationCount);
        assertTrue(ChemCoreLibraryVerifier.verifyManifestOnly(manifest,"0.10.2",
                ChemCoreLibraryManager.REQUIRED_SAMPLING_PROFILE).accepted);
        ChemCoreLibraryVerifier.VerificationResult valid=ChemCoreLibraryVerifier.verifyPayload(payload,manifest,"0.10.2");
        assertTrue(valid.reason,valid.accepted);
        byte[] tampered=Files.readAllBytes(payload.toPath());
        tampered[tampered.length/2]^=1;
        File bad=File.createTempFile("stable-tampered",".json");
        try{
            Files.write(bad.toPath(),tampered);
            assertFalse(ChemCoreLibraryVerifier.verifyPayload(bad,manifest,"0.10.2").accepted);
        }finally{bad.delete();}
    }

    @Test public void embeddedStableHas652IdentitiesAnd49NonM4UsablePriors()throws Exception{
        File file=new File(DIR+"chemcore-stable-snapshot.json");
        ChemCoreStableKnowledgeReader.Snapshot snapshot=ChemCoreStableKnowledgeReader.read(file);
        assertTrue(snapshot.note,snapshot.available);
        JSONObject root=snapshot.root;
        JSONObject counts=root.getJSONObject("content_counts");
        assertEquals(652,counts.getInt("chemical_identities"));
        assertEquals(49,counts.getInt("response_priors"));
        JSONObject k=root.getJSONObject("knowledge");
        JSONArray p=k.getJSONObject("response_priors").getJSONArray("records");
        assertEquals(49,p.length());
        for(int i=0;i<p.length();i++){
            JSONObject row=p.getJSONObject(i);
            assertFalse(row.optBoolean("device_validated",false));
            assertNotEquals("M4",row.optString("evidence_tier",""));
            assertFalse(row.optString("evidence_tier","").startsWith("M4"));
            assertTrue(row.optDouble("min_ppm",0)>0);
        }
        assertFalse(root.getJSONObject("policy").optBoolean("external_research_packaging_does_not_promote_m4",false)==false);
        assertTrue(ChemCoreStableKnowledgeReader.candidates(file,SamplingProfile.hazRobStandardChamberV2()).size()>0);
    }

    @Test public void installerDoesNotReplaceExistingRemoteStableOrLocalUserData()throws Exception{
        String installer=Files.readString(Paths.get("src/main/java/com/servoc/app/chemcore/library/ChemCoreEmbeddedStableInstaller.java"),StandardCharsets.UTF_8);
        assertTrue(installer.contains("store.activeVersion()"));
        assertTrue(installer.contains("if(!old.isEmpty())"));
        assertTrue(installer.contains("verifyPayload(stage,manifest,ReleaseAudit.VERSION)"));
        assertTrue(installer.contains("installVerifiedSnapshot(manifest,stage,ReleaseAudit.VERSION"));
        assertFalse(installer.contains("field_sessions"));
        assertFalse(installer.contains("calibration_curves"));
        assertFalse(installer.contains("DELETE FROM"));
        String seed=Files.readString(Paths.get("src/main/java/com/servoc/app/chemical/ChemicalSeedBootstrap.java"),StandardCharsets.UTF_8);
        assertTrue(seed.contains("ChemCoreEmbeddedStableInstaller.ensureInstalled(context)"));
    }
}
