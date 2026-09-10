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

    @Test public void stableManifestRequiresSignature() throws Exception {
        JSONObject j = baseManifest("stable", "");
        ChemCoreLibraryManifest m = ChemCoreLibraryManifest.fromJson(j);
        assertTrue(m.validate().stream().anyMatch(x -> x.contains("signature")));
    }

    @Test public void unsupportedManifestSchemaIsRejectedFailClosed() throws Exception {
        JSONObject j = baseManifest("research", "");
        j.put("schema_version", ChemCoreLibraryManifest.MANIFEST_SCHEMA + 1);
        ChemCoreLibraryManifest m = ChemCoreLibraryManifest.fromJson(j);
        assertTrue(m.validate().stream().anyMatch(x -> x.contains("schema_version")));
    }

    @Test public void stableManifestRejectsSqliteBecauseStableRuntimeConsumesCanonicalJson() throws Exception {
        JSONObject j = baseManifest("stable", "placeholder");
        j.put("payload_file", "chemcore_knowledge.sqlite");
        j.put("payload_format", "sqlite");
        ChemCoreLibraryManifest m = ChemCoreLibraryManifest.fromJson(j);
        assertTrue(m.validate().stream().anyMatch(x -> x.contains("stable payload_format")));
    }

    @Test public void researchManifestMayBeUnsignedButNeverBecomesStableByParsing() throws Exception {
        ChemCoreLibraryManifest m = ChemCoreLibraryManifest.fromJson(baseManifest("research", ""));
        assertEquals(ChemCoreLibraryManifest.Channel.RESEARCH, m.channel);
        assertTrue(m.validate().isEmpty());
    }

    @Test public void verifierAcceptsSignedMatchingPayloadAndRejectsTamper() throws Exception {
        File f = File.createTempFile("chemcore-library-test", ".json");
        Files.write(f.toPath(), "{\"schema\":\"chemcore-stable-snapshot-v1\"}".getBytes(StandardCharsets.UTF_8));
        String hash = ChemCoreLibraryVerifier.sha256(f);

        KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
        g.initialize(2048); KeyPair kp = g.generateKeyPair();

        JSONObject j = baseManifest("stable", "placeholder");
        j.put("payload_sha256", hash); j.put("payload_size_bytes", f.length());
        ChemCoreLibraryManifest unsigned = ChemCoreLibraryManifest.fromJson(j);
        Signature s = Signature.getInstance("SHA256withRSA");
        s.initSign(kp.getPrivate()); s.update(unsigned.canonicalSigningBytes());
        j.put("signature", Base64.getEncoder().encodeToString(s.sign()));
        ChemCoreLibraryManifest signed = ChemCoreLibraryManifest.fromJson(j);

        ChemCoreLibraryVerifier.VerificationResult ok = ChemCoreLibraryVerifier.verifyPayload(f, signed, "0.10.1", kp.getPublic());
        assertTrue(ok.reason, ok.accepted);

        Files.write(f.toPath(), "{\"tampered\":true}".getBytes(StandardCharsets.UTF_8));
        ChemCoreLibraryVerifier.VerificationResult bad = ChemCoreLibraryVerifier.verifyPayload(f, signed, "0.10.1", kp.getPublic());
        assertFalse(bad.accepted);
    }

    private static JSONObject baseManifest(String channel, String signature) throws Exception {
        JSONObject j = new JSONObject();
        j.put("library_version", "1.0.0"); j.put("schema_version", ChemCoreLibraryManifest.MANIFEST_SCHEMA); j.put("channel", channel);
        j.put("created_utc", "2026-09-10T00:00:00Z"); j.put("source_commit_sha", "0123456789abcdef");
        j.put("payload_file", "chemcore-stable-snapshot.json"); j.put("payload_format", "json");
        j.put("payload_sha256", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        j.put("payload_size_bytes", 10); j.put("chemical_count", 652); j.put("source_count", 2);
        j.put("model_count", 0); j.put("calibration_count", 0); j.put("min_app_version", "0.10.1");
        j.put("feature_schema_version", "chemcore-features/1");
        j.put("sampling_profile_compatibility", new JSONArray().put("hazrob-chemcore-chamber-v2"));
        j.put("signature_algorithm", "SHA256withRSA"); j.put("signature", signature);
        return j;
    }
}
