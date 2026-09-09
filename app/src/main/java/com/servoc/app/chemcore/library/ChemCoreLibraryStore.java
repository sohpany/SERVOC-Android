package com.servoc.app.chemcore.library;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;

/**
 * Versioned reference-library store. It intentionally lives outside servoc.db so a remote
 * knowledge update can never overwrite device-local telemetry, experiments or operational state.
 */
public final class ChemCoreLibraryStore {
    private static final String PREF = "chemcore_library_store_v1";
    private static final String K_ACTIVE_VERSION = "active_version";
    private static final String K_ACTIVE_PATH = "active_path";
    private static final String K_ACTIVE_SHA = "active_sha256";
    private static final String K_ACTIVE_SOURCE = "active_source_commit";
    private static final String K_PREVIOUS_VERSION = "previous_version";
    private static final String K_PREVIOUS_PATH = "previous_path";
    private static final String K_PREVIOUS_SHA = "previous_sha256";

    public static final class InstallResult {
        public final boolean installed;
        public final String reason;
        public final String version;
        public InstallResult(boolean installed, String reason, String version) {
            this.installed = installed; this.reason = reason == null ? "" : reason; this.version = version == null ? "" : version;
        }
    }

    private final Context context;
    private final File root;
    private final SharedPreferences prefs;

    public ChemCoreLibraryStore(Context context) {
        if (context == null) throw new IllegalArgumentException("context required");
        this.context = context.getApplicationContext();
        this.root = new File(this.context.getFilesDir(), "chemcore-library");
        this.prefs = this.context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        if (!root.exists() && !root.mkdirs()) throw new IllegalStateException("cannot create ChemCore library directory");
    }

    public String activeVersion() { return prefs.getString(K_ACTIVE_VERSION, ""); }
    public String activeSha256() { return prefs.getString(K_ACTIVE_SHA, ""); }
    public String activeSourceCommit() { return prefs.getString(K_ACTIVE_SOURCE, ""); }

    public File activePayload() {
        String p = prefs.getString(K_ACTIVE_PATH, "");
        if (p == null || p.isEmpty()) return null;
        File f = new File(p);
        return f.isFile() ? f : null;
    }

    public InstallResult installVerifiedSnapshot(ChemCoreLibraryManifest manifest, File downloadedPayload,
                                                 String appVersion, PublicKey publicationKey,
                                                 boolean measurementActive, boolean allowDowngrade) {
        if (measurementActive) return new InstallResult(false, "update blocked while measurement/analysis is active", activeVersion());
        ChemCoreLibraryVerifier.VerificationResult v = ChemCoreLibraryVerifier.verifyPayload(downloadedPayload, manifest, appVersion, publicationKey);
        if (!v.accepted) return new InstallResult(false, v.reason, activeVersion());

        String current = activeVersion();
        if (!current.isEmpty() && !allowDowngrade) {
            try {
                ChemCoreLibraryVersion incoming = ChemCoreLibraryVersion.parse(manifest.libraryVersion);
                ChemCoreLibraryVersion installed = ChemCoreLibraryVersion.parse(current);
                if (incoming.compareTo(installed) < 0) return new InstallResult(false, "library downgrade refused", current);
                if (incoming.compareTo(installed) == 0 && manifest.payloadSha256.equalsIgnoreCase(activeSha256()))
                    return new InstallResult(false, "same verified library already active", current);
            } catch (Exception e) { return new InstallResult(false, "version comparison failed", current); }
        }

        File packages = new File(root, "packages");
        if (!packages.exists() && !packages.mkdirs()) return new InstallResult(false, "cannot create packages directory", current);
        String safeVersion = manifest.libraryVersion.replaceAll("[^A-Za-z0-9._-]", "_");
        File finalDir = new File(packages, safeVersion + "-" + manifest.payloadSha256.substring(0, 12));
        File tempDir = new File(packages, ".install-" + safeVersion + "-" + System.nanoTime());
        if (!tempDir.mkdirs()) return new InstallResult(false, "cannot create staging directory", current);
        File payloadOut = new File(tempDir, manifest.payloadFile);
        File manifestOut = new File(tempDir, "manifest.json");

        try {
            copyAndSync(downloadedPayload, payloadOut);
            writeAndSync(manifestOut, manifest.toJson().toString(2));
            String copiedSha = ChemCoreLibraryVerifier.sha256(payloadOut);
            if (!copiedSha.equalsIgnoreCase(manifest.payloadSha256)) throw new IllegalStateException("staged payload hash changed");
            if (finalDir.exists()) deleteTree(finalDir);
            if (!tempDir.renameTo(finalDir)) throw new IllegalStateException("atomic package activation rename failed");
            File active = new File(finalDir, manifest.payloadFile);
            if (!active.isFile()) throw new IllegalStateException("activated payload missing");

            SharedPreferences.Editor e = prefs.edit();
            String oldPath = prefs.getString(K_ACTIVE_PATH, "");
            if (oldPath != null && !oldPath.isEmpty()) {
                e.putString(K_PREVIOUS_VERSION, current);
                e.putString(K_PREVIOUS_PATH, oldPath);
                e.putString(K_PREVIOUS_SHA, activeSha256());
            }
            e.putString(K_ACTIVE_VERSION, manifest.libraryVersion);
            e.putString(K_ACTIVE_PATH, active.getAbsolutePath());
            e.putString(K_ACTIVE_SHA, manifest.payloadSha256);
            e.putString(K_ACTIVE_SOURCE, manifest.sourceCommitSha);
            if (!e.commit()) throw new IllegalStateException("active library pointer commit failed");
            return new InstallResult(true, "installed", manifest.libraryVersion);
        } catch (Exception ex) {
            deleteTree(tempDir);
            return new InstallResult(false, "installation failed: " + ex.getMessage(), current);
        }
    }

    /** Developer-only rollback target. Caller remains responsible for role gating and measurement interlock. */
    public InstallResult rollbackToPrevious(boolean measurementActive) {
        if (measurementActive) return new InstallResult(false, "rollback blocked while measurement/analysis is active", activeVersion());
        String previousPath = prefs.getString(K_PREVIOUS_PATH, "");
        String previousVersion = prefs.getString(K_PREVIOUS_VERSION, "");
        String previousSha = prefs.getString(K_PREVIOUS_SHA, "");
        if (previousPath == null || previousPath.isEmpty()) return new InstallResult(false, "no previous validated snapshot", activeVersion());
        File f = new File(previousPath);
        if (!f.isFile()) return new InstallResult(false, "previous snapshot payload missing", activeVersion());
        try {
            String actual = ChemCoreLibraryVerifier.sha256(f);
            if (previousSha == null || previousSha.isEmpty() || !actual.equalsIgnoreCase(previousSha))
                return new InstallResult(false, "previous snapshot integrity check failed", activeVersion());
            String curVersion = activeVersion(), curPath = prefs.getString(K_ACTIVE_PATH, ""), curSha = activeSha256();
            SharedPreferences.Editor e = prefs.edit();
            e.putString(K_ACTIVE_VERSION, previousVersion); e.putString(K_ACTIVE_PATH, previousPath); e.putString(K_ACTIVE_SHA, previousSha);
            e.putString(K_PREVIOUS_VERSION, curVersion); e.putString(K_PREVIOUS_PATH, curPath); e.putString(K_PREVIOUS_SHA, curSha);
            if (!e.commit()) return new InstallResult(false, "rollback pointer commit failed", activeVersion());
            return new InstallResult(true, "rolled back", previousVersion);
        } catch (Exception ex) { return new InstallResult(false, "rollback failed: " + ex.getMessage(), activeVersion()); }
    }

    private static void copyAndSync(File in, File out) throws Exception {
        File parent = out.getParentFile(); if (parent != null && !parent.exists() && !parent.mkdirs()) throw new IllegalStateException("cannot create output directory");
        byte[] b = new byte[32768];
        try (FileInputStream src = new FileInputStream(in); FileOutputStream dst = new FileOutputStream(out)) {
            int n; while ((n = src.read(b)) >= 0) if (n > 0) dst.write(b, 0, n);
            dst.flush(); dst.getFD().sync();
        }
    }

    private static void writeAndSync(File out, String text) throws Exception {
        try (FileOutputStream f = new FileOutputStream(out)) {
            f.write(text.getBytes(StandardCharsets.UTF_8)); f.flush(); f.getFD().sync();
        }
    }

    private static void deleteTree(File f) {
        if (f == null || !f.exists()) return;
        if (f.isDirectory()) { File[] c = f.listFiles(); if (c != null) for (File x : c) deleteTree(x); }
        //noinspection ResultOfMethodCallIgnored
        f.delete();
    }
}
