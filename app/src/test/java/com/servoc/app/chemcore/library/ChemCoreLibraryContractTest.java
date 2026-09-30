package com.servoc.app.chemcore.library;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.Base64;

import static org.junit.Assert.*;

public class ChemCoreLibraryContractTest {
    @Test public void semanticVersionOrderingHandlesPrerelease() {
        assertTrue(ChemCoreLibraryVersion.parse("1.0.1").compareTo(ChemCoreLibraryVersion.parse("1.0.0")) > 0);
        assertTrue(ChemCoreLibraryVersion.parse("1.0.0").compareTo(ChemCoreLibraryVersion.parse("1.0.0-rc1")) > 0);
        assertTrue(ChemCoreLibraryVersion.parse("1.2.0").compareTo(ChemCoreLibraryVersion.parse("1.1.99")) > 0);
    }

    @Test public void stableManifestRequiresSignatureAndExplicitRevisions() throws Exception {
        JSONObject j = baseManifest("stable", "");
        j.remove("data_revision");
        ChemCoreLibraryManifest m = ChemCoreLibraryManifest.fromJson(j);
        assertTrue(m.validate().stream().anyMatch(x -> x.contains("signature")));
        assertTrue(m.validate().stream().anyMatch(x -> x.contains("data_revision")));
    }

    @Test public void unsupportedManifestSchemaIsRejectedFailClosed() throws Exception {
        JSONObject j = baseManifest("research", "");
        j.put("schema_version", ChemCoreLibraryManifest.MANIFEST_SCHEMA + 1);
        ChemCoreLibraryManifest m = ChemCoreLibraryManifest.fromJson(j);
        assertTrue(m.validate().stream().anyMatch(x -> x.contains("schema_version")));
    }

    @Test public void stableManifestRejectsSqliteAndWrongPayloadSchema() throws Exception {
        JSONObject j = baseManifest("stable", "placeholder");
        j.put("payload_file", "chemcore_knowledge.sqlite"); j.put("payload_format", "sqlite");
        j.put("payload_schema", "wrong-schema");
        ChemCoreLibraryManifest m = ChemCoreLibraryManifest.fromJson(j);
        assertTrue(m.validate().stream().anyMatch(x -> x.contains("stable payload_format")));
        assertTrue(m.validate().stream().anyMatch(x -> x.contains("stable payload_schema")));
    }

    @Test public void researchManifestMayBeUnsignedButNeverBecomesStableByParsing() throws Exception {
        ChemCoreLibraryManifest m = ChemCoreLibraryManifest.fromJson(baseManifest("research", ""));
        assertEquals(ChemCoreLibraryManifest.Channel.RESEARCH, m.channel);
        assertTrue(m.validate().isEmpty());
    }

    @Test public void manifestIsAuthenticatedBeforeRemoteMetadataIsTrusted() throws Exception {
        KeyPair kp=keyPair(); JSONObject j=baseManifest("stable", "placeholder");
        ChemCoreLibraryManifest unsigned=ChemCoreLibraryManifest.fromJson(j);j.put("signature",sign(unsigned,kp));
        ChemCoreLibraryManifest signed=ChemCoreLibraryManifest.fromJson(j);
        ChemCoreLibraryVerifier.VerificationResult ok=ChemCoreLibraryVerifier.verifyManifestOnly(signed,"0.10.2","hazrob-chemcore-chamber-v2",kp.getPublic());
        assertTrue(ok.reason,ok.accepted);
        ChemCoreLibraryVerifier.VerificationResult profile=ChemCoreLibraryVerifier.verifyManifestOnly(signed,"0.10.2","hazrob-chemcore-chamber-v1",kp.getPublic());
        assertFalse(profile.accepted);assertTrue(profile.reason.contains("sampling profile"));
        j.put("model_count",77);ChemCoreLibraryManifest tampered=ChemCoreLibraryManifest.fromJson(j);
        assertFalse(ChemCoreLibraryVerifier.verifyManifestOnly(tampered,"0.10.2","hazrob-chemcore-chamber-v2",kp.getPublic()).accepted);
    }

    @Test public void verifierAcceptsSignedSemanticPayloadAndRejectsTamperOrRevisionMismatch() throws Exception {
        File f = File.createTempFile("chemcore-library-test", ".json");
        writePayload(f,"sha256:data1","hazrob-evidence-policy/1",652,4,76,0);
        String hash = ChemCoreLibraryVerifier.sha256(f);KeyPair kp=keyPair();
        JSONObject j = baseManifest("stable", "placeholder");
        j.put("payload_sha256", hash); j.put("payload_size_bytes", f.length());
        ChemCoreLibraryManifest unsigned = ChemCoreLibraryManifest.fromJson(j);j.put("signature",sign(unsigned,kp));
        ChemCoreLibraryManifest signed = ChemCoreLibraryManifest.fromJson(j);

        ChemCoreLibraryVerifier.VerificationResult ok = ChemCoreLibraryVerifier.verifyPayload(f, signed, "0.10.2", kp.getPublic());
        assertTrue(ok.reason, ok.accepted);

        writePayload(f,"sha256:other","hazrob-evidence-policy/1",652,4,76,0);
        j.put("payload_sha256",ChemCoreLibraryVerifier.sha256(f));j.put("payload_size_bytes",f.length());
        ChemCoreLibraryManifest unsignedMismatch=ChemCoreLibraryManifest.fromJson(j);j.put("signature",sign(unsignedMismatch,kp));
        ChemCoreLibraryVerifier.VerificationResult mismatch=ChemCoreLibraryVerifier.verifyPayload(f,ChemCoreLibraryManifest.fromJson(j),"0.10.2",kp.getPublic());
        assertFalse(mismatch.accepted);assertTrue(mismatch.reason.contains("data revision"));

        Files.write(f.toPath(), "{\"tampered\":true}".getBytes(StandardCharsets.UTF_8));
        ChemCoreLibraryVerifier.VerificationResult bad = ChemCoreLibraryVerifier.verifyPayload(f, signed, "0.10.2", kp.getPublic());
        assertFalse(bad.accepted);
    }

    private static void writePayload(File f,String dataRevision,String evidenceRevision,int chemicals,int sources,int models,int calibrations)throws Exception{
        JSONObject counts=new JSONObject();counts.put("chemical_identities",chemicals);counts.put("reference_artifacts",sources);counts.put("model_artifacts",models);counts.put("calibrations",calibrations);
        JSONObject policy=new JSONObject();policy.put("m4_requires_device_specific_validation",true);policy.put("external_research_packaging_does_not_promote_m4",true);
        JSONObject p=new JSONObject();p.put("schema",ChemCoreLibraryManifest.STABLE_PAYLOAD_SCHEMA);p.put("source_commit_sha","0123456789abcdef");p.put("data_revision",dataRevision);p.put("evidence_revision",evidenceRevision);p.put("content_counts",counts);p.put("policy",policy);p.put("knowledge",new JSONObject());
        Files.write(f.toPath(),p.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static KeyPair keyPair()throws Exception{KeyPairGenerator g=KeyPairGenerator.getInstance("RSA");g.initialize(2048);return g.generateKeyPair();}
    private static String sign(ChemCoreLibraryManifest m,KeyPair kp)throws Exception{Signature s=Signature.getInstance("SHA256withRSA");s.initSign(kp.getPrivate());s.update(m.canonicalSigningBytes());return Base64.getEncoder().encodeToString(s.sign());}

    private static JSONObject baseManifest(String channel, String signature) throws Exception {
        JSONObject j = new JSONObject();
        j.put("library_version", "1.0.0"); j.put("schema_version", ChemCoreLibraryManifest.MANIFEST_SCHEMA); j.put("channel", channel);
        j.put("created_utc", "2026-09-30T00:00:00Z"); j.put("source_commit_sha", "0123456789abcdef");
        j.put("payload_file", "chemcore-stable-snapshot.json"); j.put("payload_format", "json"); j.put("payload_schema",ChemCoreLibraryManifest.STABLE_PAYLOAD_SCHEMA);
        j.put("payload_sha256", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        j.put("payload_size_bytes", 10); j.put("chemical_count", 652); j.put("source_count", 4);
        j.put("model_count", 76); j.put("calibration_count", 0); j.put("min_app_version", "0.10.2");
        j.put("feature_schema_version", "chemcore-features/1");j.put("data_revision","sha256:data1");j.put("evidence_revision","hazrob-evidence-policy/1");
        j.put("sampling_profile_compatibility", new JSONArray().put("hazrob-chemcore-chamber-v2"));
        j.put("signature_algorithm", "SHA256withRSA"); j.put("signature", signature);
        return j;
    }
}
