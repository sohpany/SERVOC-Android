package com.servoc.app.chemcore.library;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

/** Integrity, authenticity, semantic consistency and compatibility gates for ChemCore snapshots. */
public final class ChemCoreLibraryVerifier {
    private ChemCoreLibraryVerifier() {}

    public static final class VerificationResult {
        public final boolean accepted;
        public final String reason;
        public final String actualSha256;
        public VerificationResult(boolean accepted, String reason, String actualSha256) {
            this.accepted = accepted; this.reason = reason == null ? "" : reason;
            this.actualSha256 = actualSha256 == null ? "" : actualSha256;
        }
    }

    public static VerificationResult verifyPayload(File payload, ChemCoreLibraryManifest manifest,
                                                   String appVersion, PublicKey publicationKey) {
        if (payload == null || manifest == null) return new VerificationResult(false, "payload and manifest required", "");
        List<String> errors = manifest.validate();
        if (!errors.isEmpty()) return new VerificationResult(false, "manifest invalid: " + String.join("; ", errors), "");
        try {
            if (!manifest.isCompatibleWithApp(appVersion)) return new VerificationResult(false, "library incompatible with app version", "");
        } catch (Exception e) {
            return new VerificationResult(false, "app version compatibility check failed", "");
        }
        if (!payload.isFile()) return new VerificationResult(false, "payload file missing", "");
        if (payload.length() != manifest.payloadSizeBytes) return new VerificationResult(false, "payload size mismatch", "");
        String actual;
        try { actual = sha256(payload); }
        catch (Exception e) { return new VerificationResult(false, "payload hash failed: " + e.getMessage(), ""); }
        if (!constantTimeHexEquals(actual, manifest.payloadSha256)) return new VerificationResult(false, "payload SHA-256 mismatch", actual);

        if (manifest.channel == ChemCoreLibraryManifest.Channel.STABLE) {
            if (publicationKey == null) return new VerificationResult(false, "stable publication key not provisioned", actual);
            try {
                if (!verifyManifestSignature(manifest, publicationKey)) return new VerificationResult(false, "manifest publication signature invalid", actual);
            } catch (Exception e) {
                return new VerificationResult(false, "signature verification failed: " + e.getMessage(), actual);
            }
            String semantic = validateStablePayloadSemantics(payload, manifest);
            if (!semantic.isEmpty()) return new VerificationResult(false, semantic, actual);
        }
        return new VerificationResult(true, "verified", actual);
    }

    /** Verify manifest authenticity before trusting remote version, counts or payload path. */
    public static VerificationResult verifyManifestOnly(ChemCoreLibraryManifest manifest, String appVersion,
                                                        String requiredSamplingProfile, PublicKey publicationKey) {
        if (manifest == null) return new VerificationResult(false, "manifest required", "");
        List<String> errors = manifest.validate();
        if (!errors.isEmpty()) return new VerificationResult(false, "manifest invalid: " + String.join("; ", errors), "");
        if (manifest.channel != ChemCoreLibraryManifest.Channel.STABLE)
            return new VerificationResult(false, "normal updater accepts Stable channel only", "");
        if (publicationKey == null) return new VerificationResult(false, "stable publication key not provisioned", "");
        try {
            if (!manifest.isCompatibleWithApp(appVersion)) return new VerificationResult(false, "library incompatible with app version", "");
            if (!manifest.isCompatibleWithSamplingProfile(requiredSamplingProfile))
                return new VerificationResult(false, "library incompatible with active sampling profile", "");
            if (!verifyManifestSignature(manifest, publicationKey))
                return new VerificationResult(false, "manifest publication signature invalid", "");
        } catch (Exception e) {
            return new VerificationResult(false, "manifest verification failed: " + e.getMessage(), "");
        }
        return new VerificationResult(true, "verified manifest", "");
    }

    private static String validateStablePayloadSemantics(File payload, ChemCoreLibraryManifest manifest) {
        if (!"json".equalsIgnoreCase(manifest.payloadFormat)) return "stable payload must be JSON";
        try (FileInputStream in = new FileInputStream(payload)) {
            JSONObject root = new JSONObject(readUtf8(in, Math.min(manifest.payloadSizeBytes + 1L, 256L * 1024L * 1024L)));
            if (!manifest.payloadSchema.equals(root.optString("schema", ""))) return "payload schema does not match signed manifest";
            if (!manifest.sourceCommitSha.equalsIgnoreCase(root.optString("source_commit_sha", ""))) return "payload source commit does not match signed manifest";
            if (!manifest.dataRevision.equals(root.optString("data_revision", ""))) return "payload data revision does not match signed manifest";
            if (!manifest.evidenceRevision.equals(root.optString("evidence_revision", ""))) return "payload evidence revision does not match signed manifest";
            JSONObject counts = root.optJSONObject("content_counts");
            if (counts == null) return "payload content_counts missing";
            if (counts.optInt("chemical_identities", -1) != manifest.chemicalCount) return "payload chemical count does not match signed manifest";
            if (counts.optInt("reference_artifacts", -1) != manifest.sourceCount) return "payload source count does not match signed manifest";
            if (counts.optInt("model_artifacts", -1) != manifest.modelCount) return "payload model count does not match signed manifest";
            if (counts.optInt("calibrations", -1) != manifest.calibrationCount) return "payload calibration count does not match signed manifest";
            JSONObject policy = root.optJSONObject("policy");
            if (policy == null || !policy.optBoolean("m4_requires_device_specific_validation", false))
                return "payload evidence policy missing M4 device-validation gate";
            if (!policy.optBoolean("external_research_packaging_does_not_promote_m4", false))
                return "payload evidence policy permits unsafe external M4 promotion";
            return "";
        } catch (Exception e) {
            return "stable payload semantic validation failed: " + e.getClass().getSimpleName();
        }
    }

    public static String sha256(File f) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] b = new byte[32768];
        try (InputStream in = new FileInputStream(f)) {
            int n; while ((n = in.read(b)) >= 0) if (n > 0) md.update(b, 0, n);
        }
        return hex(md.digest());
    }

    public static boolean verifyManifestSignature(ChemCoreLibraryManifest manifest, PublicKey key) throws Exception {
        if (manifest == null || key == null) return false;
        String algorithm = manifest.signatureAlgorithm.trim();
        if (!"SHA256withRSA".equalsIgnoreCase(algorithm)) throw new IllegalArgumentException("unsupported signature algorithm: " + algorithm);
        Signature s = Signature.getInstance("SHA256withRSA");
        s.initVerify(key);
        s.update(manifest.canonicalSigningBytes());
        byte[] signature = Base64.getDecoder().decode(manifest.signatureBase64);
        return s.verify(signature);
    }

    public static PublicKey parseRsaPublicKeyPem(String pem) throws Exception {
        if (pem == null) throw new IllegalArgumentException("public key required");
        String body = pem.replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] der = Base64.getDecoder().decode(body.getBytes(StandardCharsets.US_ASCII));
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
    }

    private static String readUtf8(InputStream in, long maxBytes) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream(); byte[] b = new byte[16384]; long total = 0;
        for (int n; (n = in.read(b)) >= 0;) {
            if (n <= 0) continue; total += n; if (total > maxBytes) throw new IllegalStateException("payload exceeds declared size"); out.write(b, 0, n);
        }
        return out.toString(StandardCharsets.UTF_8.name());
    }

    private static boolean constantTimeHexEquals(String a, String b) {
        if (a == null || b == null) return false;
        byte[] x = a.toLowerCase(Locale.US).getBytes(StandardCharsets.US_ASCII);
        byte[] y = b.toLowerCase(Locale.US).getBytes(StandardCharsets.US_ASCII);
        return MessageDigest.isEqual(x, y);
    }

    private static String hex(byte[] b) {
        StringBuilder s = new StringBuilder(b.length * 2);
        for (byte x : b) s.append(String.format(Locale.US, "%02x", x & 0xff));
        return s.toString();
    }
}
