package com.servoc.app.chemcore.library;

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

/** Integrity, authenticity and compatibility gates for ChemCore library snapshots. */
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
        }
        return new VerificationResult(true, "verified", actual);
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
