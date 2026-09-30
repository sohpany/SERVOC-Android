package com.servoc.app.chemcore.library;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.*;

public class ChemCoreLibraryContractTest {
    @Test public void semanticVersionOrderingHandlesPrerelease() {
        assertTrue(ChemCoreLibraryVersion.parse("1.0.1").compareTo(ChemCoreLibraryVersion.parse("1.0.0")) > 0);
        assertTrue(ChemCoreLibraryVersion.parse("1.0.0").compareTo(ChemCoreLibraryVersion.parse("1.0.0-rc1")) > 0);
        assertTrue(ChemCoreLibraryVersion.parse("1.2.0").compareTo(ChemCoreLibraryVersion.parse("1.1.99")) > 0);
    }

    @Test public void stableManifestRequiresExplicitIntegrityAndRevisions() throws Exception {
        JSONObject j = baseManifest("stable");
        j.remove("data_revision"); j.put("integrity_mode", "");
        ChemCoreLibraryManifest m = ChemCoreLibraryManifest.fromJson(j);
        assertTrue(m.validate().stream().anyMatch(x -> x.contains("data_revision")));
        assertTrue(m.validate().stream().anyMatch(x -> x.contains("integrity_mode")));
    }

    @Test public void unsupportedManifestSchemaIsRejectedFailClosed() throws Exception {
        JSONObject j = baseManifest("research");
        j.put("schema_version", ChemCoreLibraryManifest.MANIFEST_SCHEMA + 1);
        ChemCoreLibraryManifest m = ChemCoreLibraryManifest.fromJson(j);
        assertTrue(m.validate().stream().anyMatch(x -> x.contains("schema_version")));
    }

    @Test public void stableManifestRejectsSqliteAndWrongPayloadSchema() throws Exception {
        JSONObject j = baseManifest("stable");
        j.put("payload_file", "chemcore_knowledge.sqlite"); j.put("payload_format", "sqlite");
        j.put("payload_schema", "wrong-schema");
        ChemCoreLibraryManifest m = ChemCoreLibraryManifest.fromJson(j);
        assertTrue(m.validate().stream().anyMatch(x -> x.contains("stable payload_format")));
        assertTrue(m.validate().stream().anyMatch(x -> x.contains("stable payload_schema")));
    }

    @Test public void researchManifestMayRemainUnsigned() throws Exception {
        ChemCoreLibraryManifest m = ChemCoreLibraryManifest.fromJson(baseManifest("research"));
        assertEquals(ChemCoreLibraryManifest.Channel.RESEARCH, m.channel);
        assertTrue(m.validate().isEmpty());
    }

    @Test public void manifestCompatibilityChecksSamplingProfileAndApp() throws Exception {
        ChemCoreLibraryManifest m=ChemCoreLibraryManifest.fromJson(baseManifest("stable"));
        assertTrue(ChemCoreLibraryVerifier.verifyManifestOnly(m,"0.10.2","hazrob-chemcore-chamber-v2").accepted);
        ChemCoreLibraryVerifier.VerificationResult badProfile=ChemCoreLibraryVerifier.verifyManifestOnly(m,"0.10.2","hazrob-chemcore-chamber-v1");
        assertFalse(badProfile.accepted);assertTrue(badProfile.reason.contains("sampling profile"));
        assertFalse(ChemCoreLibraryVerifier.verifyManifestOnly(m,"0.10.1","hazrob-chemcore-chamber-v2").accepted);
    }

    @Test public void verifierAcceptsMatchingSemanticPayloadAndRejectsHashOrRevisionMismatch() throws Exception {
        File f = File.createTempFile("chemcore-library-test", ".json");
        writePayload(f,"sha256:data1","hazrob-evidence-policy/1",652,4,76,0);
        JSONObject j = baseManifest("stable");
        j.put("payload_sha256", ChemCoreLibraryVerifier.sha256(f)); j.put("payload_size_bytes", f.length());
        ChemCoreLibraryManifest m=ChemCoreLibraryManifest.fromJson(j);
        ChemCoreLibraryVerifier.VerificationResult ok = ChemCoreLibraryVerifier.verifyPayload(f,m,"0.10.2");
        assertTrue(ok.reason,ok.accepted);

        writePayload(f,"sha256:other","hazrob-evidence-policy/1",652,4,76,0);
        j.put("payload_sha256",ChemCoreLibraryVerifier.sha256(f));j.put("payload_size_bytes",f.length());
        ChemCoreLibraryVerifier.VerificationResult mismatch=ChemCoreLibraryVerifier.verifyPayload(f,ChemCoreLibraryManifest.fromJson(j),"0.10.2");
        assertFalse(mismatch.accepted);assertTrue(mismatch.reason.contains("data revision"));

        Files.write(f.toPath(), "{\"tampered\":true}".getBytes(StandardCharsets.UTF_8));
        ChemCoreLibraryVerifier.VerificationResult bad = ChemCoreLibraryVerifier.verifyPayload(f,m,"0.10.2");
        assertFalse(bad.accepted);
    }

    private static void writePayload(File f,String dataRevision,String evidenceRevision,int chemicals,int sources,int models,int calibrations)throws Exception{
        JSONObject counts=new JSONObject();counts.put("chemical_identities",chemicals);counts.put("reference_artifacts",sources);counts.put("model_artifacts",models);counts.put("calibrations",calibrations);
        JSONObject policy=new JSONObject();policy.put("m4_requires_device_specific_validation",true);policy.put("external_research_packaging_does_not_promote_m4",true);
        JSONObject p=new JSONObject();p.put("schema",ChemCoreLibraryManifest.STABLE_PAYLOAD_SCHEMA);p.put("source_commit_sha","0123456789abcdef");p.put("data_revision",dataRevision);p.put("evidence_revision",evidenceRevision);p.put("content_counts",counts);p.put("policy",policy);p.put("knowledge",new JSONObject());
        Files.write(f.toPath(),p.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static JSONObject baseManifest(String channel) throws Exception {
        JSONObject j = new JSONObject();
        j.put("library_version", "1.0.0"); j.put("schema_version", ChemCoreLibraryManifest.MANIFEST_SCHEMA); j.put("channel", channel);
        j.put("created_utc", "2026-09-30T00:00:00Z"); j.put("source_commit_sha", "0123456789abcdef");
        j.put("payload_file", "chemcore-stable-snapshot.json"); j.put("payload_format", "json"); j.put("payload_schema",ChemCoreLibraryManifest.STABLE_PAYLOAD_SCHEMA);
        j.put("integrity_mode",ChemCoreLibraryManifest.INTEGRITY_MODE);
        j.put("payload_sha256", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        j.put("payload_size_bytes", 10); j.put("chemical_count", 652); j.put("source_count", 4);
        j.put("model_count", 76); j.put("calibration_count", 0); j.put("min_app_version", "0.10.2");
        j.put("feature_schema_version", "chemcore-features/1");j.put("data_revision","sha256:data1");j.put("evidence_revision","hazrob-evidence-policy/1");
        j.put("sampling_profile_compatibility", new JSONArray().put("hazrob-chemcore-chamber-v2"));
        return j;
    }
}
