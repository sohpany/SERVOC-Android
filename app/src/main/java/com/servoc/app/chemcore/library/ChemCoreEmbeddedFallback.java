package com.servoc.app.chemcore.library;

import android.content.Context;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Describes the scientific knowledge already bundled inside the APK. This fallback remains
 * available when no remotely published ChemCore snapshot has been activated.
 */
public final class ChemCoreEmbeddedFallback {
    private ChemCoreEmbeddedFallback() {}

    public static final String MOLECULAR_MANIFEST_ASSET = "generated/chemcore/manifest.json";

    public static final class Info {
        public final boolean molecularSeedPresent;
        public final int recordCount;
        public final int resolvedCount;
        public final int unresolvedCount;
        public final String sha256;
        public final String label;

        Info(boolean present, int records, int resolved, int unresolved, String sha256, String label) {
            this.molecularSeedPresent = present;
            this.recordCount = records;
            this.resolvedCount = resolved;
            this.unresolvedCount = unresolved;
            this.sha256 = sha256 == null ? "" : sha256;
            this.label = label == null ? "" : label;
        }
    }

    public static Info inspect(Context context) {
        if (context == null) return new Info(false, 0, 0, 0, "", "APK bundled legacy knowledge");
        try (InputStream in = context.getAssets().open(MOLECULAR_MANIFEST_ASSET)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] b = new byte[4096]; int n;
            while ((n = in.read(b)) >= 0) if (n > 0) out.write(b, 0, n);
            JSONObject j = new JSONObject(out.toString(StandardCharsets.UTF_8.name()));
            return new Info(true,
                    j.optInt("record_count", 0),
                    j.optInt("resolved_count", 0),
                    j.optInt("unresolved_count", 0),
                    j.optString("sha256", ""),
                    "APK bundled ChemCore molecular seed");
        } catch (Exception ignored) {
            return new Info(false, 0, 0, 0, "", "APK bundled legacy chemical/exposure knowledge");
        }
    }
}
