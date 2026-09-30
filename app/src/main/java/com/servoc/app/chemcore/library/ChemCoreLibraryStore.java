package com.servoc.app.chemcore.library;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Versioned reference-library store. It intentionally lives outside servoc.db so a remote
 * knowledge update can never overwrite device-local telemetry, experiments or operational state.
 */
public final class ChemCoreLibraryStore {
    private static final String PREF = "chemcore_library_store_v2";
    private static final String K_ACTIVE_VERSION = "active_version";
    private static final String K_ACTIVE_PATH = "active_path";
    private static final String K_ACTIVE_MANIFEST_PATH = "active_manifest_path";
    private static final String K_ACTIVE_SHA = "active_sha256";
    private static final String K_ACTIVE_SOURCE = "active_source_commit";
    private static final String K_ACTIVE_DATA_REV = "active_data_revision";
    private static final String K_ACTIVE_EVIDENCE_REV = "active_evidence_revision";
    private static final String K_PREVIOUS_VERSION = "previous_version";
    private static final String K_PREVIOUS_PATH = "previous_path";
    private static final String K_PREVIOUS_MANIFEST_PATH = "previous_manifest_path";
    private static final String K_PREVIOUS_SHA = "previous_sha256";
    private static final String K_PREVIOUS_SOURCE = "previous_source_commit";
    private static final String K_PREVIOUS_DATA_REV = "previous_data_revision";
    private static final String K_PREVIOUS_EVIDENCE_REV = "previous_evidence_revision";

    public static final class InstallResult {
        public final boolean installed;
        public final String reason;
        public final String version;
        public InstallResult(boolean installed, String reason, String version) {
            this.installed = installed; this.reason = reason == null ? "" : reason; this.version = version == null ? "" : version;
        }
    }

    private final File root;
    private final SharedPreferences prefs;

    public ChemCoreLibraryStore(Context context) {
        if (context == null) throw new IllegalArgumentException("context required");
        Context app = context.getApplicationContext();
        this.root = new File(app.getFilesDir(), "chemcore-library");
        this.prefs = app.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        if (!root.exists() && !root.mkdirs()) throw new IllegalStateException("cannot create ChemCore library directory");
    }

    public String activeVersion() { return prefs.getString(K_ACTIVE_VERSION, ""); }
    public String activeSha256() { return prefs.getString(K_ACTIVE_SHA, ""); }
    public String activeSourceCommit() { return prefs.getString(K_ACTIVE_SOURCE, ""); }
    public String activeDataRevision() { return prefs.getString(K_ACTIVE_DATA_REV, ""); }
    public String activeEvidenceRevision() { return prefs.getString(K_ACTIVE_EVIDENCE_REV, ""); }

    public File activePayload() {
        String p = prefs.getString(K_ACTIVE_PATH, "");
        if (p == null || p.isEmpty()) return null;
        File f = new File(p);
        return f.isFile() ? f : null;
    }

    public InstallResult installVerifiedSnapshot(ChemCoreLibraryManifest manifest, File downloadedPayload,
                                                 String appVersion, boolean measurementActive,
                                                 boolean allowDowngrade) {
        if (measurementActive) return new InstallResult(false, "update blocked while measurement/analysis is active", activeVersion());
        ChemCoreLibraryVerifier.VerificationResult v = ChemCoreLibraryVerifier.verifyPayload(downloadedPayload, manifest, appVersion);
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
            ChemCoreLibraryVerifier.VerificationResult staged = ChemCoreLibraryVerifier.verifyPayload(payloadOut, manifest, appVersion);
            if (!staged.accepted) throw new IllegalStateException("staged package verification failed: " + staged.reason);
            if (finalDir.exists()) deleteTree(finalDir);
            if (!tempDir.renameTo(finalDir)) throw new IllegalStateException("atomic package activation rename failed");
            File active = new File(finalDir, manifest.payloadFile);
            File activeManifest = new File(finalDir, "manifest.json");
            if (!active.isFile() || !activeManifest.isFile()) throw new IllegalStateException("activated package incomplete");

            SharedPreferences.Editor e = prefs.edit();
            String oldPath = prefs.getString(K_ACTIVE_PATH, "");
            if (oldPath != null && !oldPath.isEmpty()) {
                e.putString(K_PREVIOUS_VERSION, current);
                e.putString(K_PREVIOUS_PATH, oldPath);
                e.putString(K_PREVIOUS_MANIFEST_PATH, prefs.getString(K_ACTIVE_MANIFEST_PATH, ""));
                e.putString(K_PREVIOUS_SHA, activeSha256());
                e.putString(K_PREVIOUS_SOURCE, activeSourceCommit());
                e.putString(K_PREVIOUS_DATA_REV, activeDataRevision());
                e.putString(K_PREVIOUS_EVIDENCE_REV, activeEvidenceRevision());
            }
            e.putString(K_ACTIVE_VERSION, manifest.libraryVersion);
            e.putString(K_ACTIVE_PATH, active.getAbsolutePath());
            e.putString(K_ACTIVE_MANIFEST_PATH, activeManifest.getAbsolutePath());
            e.putString(K_ACTIVE_SHA, manifest.payloadSha256);
            e.putString(K_ACTIVE_SOURCE, manifest.sourceCommitSha);
            e.putString(K_ACTIVE_DATA_REV, manifest.dataRevision);
            e.putString(K_ACTIVE_EVIDENCE_REV, manifest.evidenceRevision);
            if (!e.commit()) throw new IllegalStateException("active library pointer commit failed");
            return new InstallResult(true, "installed", manifest.libraryVersion);
        } catch (Exception ex) {
            deleteTree(tempDir);
            return new InstallResult(false, "installation failed: " + ex.getMessage(), current);
        }
    }

    /** Developer rollback. Previous payload and manifest are reverified before pointer swap. */
    public InstallResult rollbackToPrevious(boolean measurementActive, String appVersion) {
        if (measurementActive) return new InstallResult(false, "rollback blocked while measurement/analysis is active", activeVersion());
        String previousPath = prefs.getString(K_PREVIOUS_PATH, "");
        String previousManifestPath = prefs.getString(K_PREVIOUS_MANIFEST_PATH, "");
        if (previousPath == null || previousPath.isEmpty() || previousManifestPath == null || previousManifestPath.isEmpty())
            return new InstallResult(false, "no previous verified snapshot", activeVersion());
        File payload = new File(previousPath), manifestFile = new File(previousManifestPath);
        if (!payload.isFile() || !manifestFile.isFile()) return new InstallResult(false, "previous snapshot package missing", activeVersion());
        try {
            ChemCoreLibraryManifest previous = ChemCoreLibraryManifest.fromJson(new JSONObject(readUtf8(manifestFile)));
            ChemCoreLibraryVerifier.VerificationResult verified = ChemCoreLibraryVerifier.verifyPayload(payload, previous, appVersion);
            if (!verified.accepted) return new InstallResult(false, "previous snapshot verification failed: " + verified.reason, activeVersion());
            if (!previous.libraryVersion.equals(prefs.getString(K_PREVIOUS_VERSION, "")))
                return new InstallResult(false, "previous snapshot version pointer mismatch", activeVersion());
            if (!previous.payloadSha256.equalsIgnoreCase(prefs.getString(K_PREVIOUS_SHA, "")))
                return new InstallResult(false, "previous snapshot hash pointer mismatch", activeVersion());

            String curVersion = activeVersion(), curPath = prefs.getString(K_ACTIVE_PATH, ""), curManifest = prefs.getString(K_ACTIVE_MANIFEST_PATH, "");
            String curSha = activeSha256(), curSource = activeSourceCommit(), curData = activeDataRevision(), curEvidence = activeEvidenceRevision();
            SharedPreferences.Editor e = prefs.edit();
            e.putString(K_ACTIVE_VERSION, previous.libraryVersion); e.putString(K_ACTIVE_PATH, previousPath);
            e.putString(K_ACTIVE_MANIFEST_PATH, previousManifestPath); e.putString(K_ACTIVE_SHA, previous.payloadSha256);
            e.putString(K_ACTIVE_SOURCE, previous.sourceCommitSha); e.putString(K_ACTIVE_DATA_REV, previous.dataRevision);
            e.putString(K_ACTIVE_EVIDENCE_REV, previous.evidenceRevision);
            e.putString(K_PREVIOUS_VERSION, curVersion); e.putString(K_PREVIOUS_PATH, curPath);
            e.putString(K_PREVIOUS_MANIFEST_PATH, curManifest); e.putString(K_PREVIOUS_SHA, curSha);
            e.putString(K_PREVIOUS_SOURCE, curSource); e.putString(K_PREVIOUS_DATA_REV, curData);
            e.putString(K_PREVIOUS_EVIDENCE_REV, curEvidence);
            if (!e.commit()) return new InstallResult(false, "rollback pointer commit failed", activeVersion());
            return new InstallResult(true, "rolled back after verification", previous.libraryVersion);
        } catch (Exception ex) { return new InstallResult(false, "rollback failed: " + ex.getMessage(), activeVersion()); }
    }

    private static String readUtf8(File f) throws Exception {
        try (FileInputStream in = new FileInputStream(f); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] b = new byte[8192]; for (int n; (n = in.read(b)) >= 0;) if (n > 0) out.write(b, 0, n);
            return out.toString(StandardCharsets.UTF_8.name());
        }
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
