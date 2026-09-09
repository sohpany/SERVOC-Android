package com.servoc.app.chemcore.library;

import android.content.Context;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;

/** Loads the ChemCore stable-library publication public key bundled with the APK. */
public final class ChemCorePublicationKeyProvider {
    public static final String ASSET_PATH = "chemcore/publication_key.pem";
    private ChemCorePublicationKeyProvider() {}

    /**
     * Returns null until a real publication key is provisioned. The private signing key must never
     * be bundled in the APK or committed to the repository.
     */
    public static PublicKey load(Context context) {
        if (context == null) return null;
        try (InputStream in = context.getAssets().open(ASSET_PATH)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] b = new byte[4096];
            int n;
            while ((n = in.read(b)) >= 0) if (n > 0) out.write(b, 0, n);
            String pem = out.toString(StandardCharsets.US_ASCII.name()).trim();
            if (pem.isEmpty() || pem.contains("NOT_PROVISIONED")) return null;
            return ChemCoreLibraryVerifier.parseRsaPublicKeyPem(pem);
        } catch (Exception ignored) {
            return null;
        }
    }

    public static boolean isProvisioned(Context context) { return load(context) != null; }
}
