package com.servoc.app.chemcore.library;

import android.content.Context;
import android.content.SharedPreferences;

import com.servoc.app.BuildConfig;
import com.servoc.app.chemcore.SamplingProfile;
import com.servoc.app.measurement.MeasurementSessionRegistry;

import org.json.JSONObject;

import java.io.File;
import java.net.URL;

/** High-level controller for discovery, verification, installation and rollback of ChemCore knowledge snapshots. */
public final class ChemCoreLibraryManager {
    /** Developer-only override. Normal users use BuildConfig.CHEMCORE_LIBRARY_MANIFEST_URL. */
    public static final String PREF_MANIFEST_URL = "chemcore_library_manifest_url";
    public static final String REQUIRED_SAMPLING_PROFILE = SamplingProfile.hazRobStandardChamberV2().profileId;

    public static final class Status {
        public final boolean remoteSnapshotActive;
        public final String version;
        public final String sha256;
        public final String sourceCommit;
        public final String dataRevision;
        public final String evidenceRevision;
        public final String sourceLabel;
        public final ChemCoreEmbeddedFallback.Info embedded;

        Status(boolean remoteSnapshotActive, String version, String sha256, String sourceCommit,
               String dataRevision, String evidenceRevision, String sourceLabel,
               ChemCoreEmbeddedFallback.Info embedded) {
            this.remoteSnapshotActive = remoteSnapshotActive;
            this.version = version == null ? "" : version;
            this.sha256 = sha256 == null ? "" : sha256;
            this.sourceCommit = sourceCommit == null ? "" : sourceCommit;
            this.dataRevision = dataRevision == null ? "" : dataRevision;
            this.evidenceRevision = evidenceRevision == null ? "" : evidenceRevision;
            this.sourceLabel = sourceLabel == null ? "" : sourceLabel;
            this.embedded = embedded;
        }
    }

    public static final class CheckSummary {
        public final ChemCoreLibraryUpdateClient.CheckResult remote;
        public final boolean updateAvailable;
        public final String installedVersion;
        public final String message;

        CheckSummary(ChemCoreLibraryUpdateClient.CheckResult remote, boolean updateAvailable,
                     String installedVersion, String message) {
            this.remote = remote;
            this.updateAvailable = updateAvailable;
            this.installedVersion = installedVersion == null ? "" : installedVersion;
            this.message = message == null ? "" : message;
        }
    }

    private final Context context;
    private final SharedPreferences prefs;
    private final ChemCoreLibraryStore store;
    private final ChemCoreLibraryUpdateClient client;

    public ChemCoreLibraryManager(Context context) {
        if (context == null) throw new IllegalArgumentException("context required");
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences("servoc", Context.MODE_PRIVATE);
        this.store = new ChemCoreLibraryStore(this.context);
        this.client = new ChemCoreLibraryUpdateClient();
    }

    public Status status() {
        File active = store.activePayload();
        boolean remote = active != null && active.isFile() && !store.activeVersion().isEmpty();
        ChemCoreEmbeddedFallback.Info embedded = ChemCoreEmbeddedFallback.inspect(context);
        return new Status(remote,
                remote ? store.activeVersion() : "embedded",
                remote ? store.activeSha256() : embedded.sha256,
                remote ? store.activeSourceCommit() : "apk",
                remote ? store.activeDataRevision() : "embedded/apk",
                remote ? store.activeEvidenceRevision() : "embedded/apk",
                remote ? "verified remote ChemCore snapshot" : embedded.label,
                embedded);
    }

    public String manifestUrl() {
        String override = manifestUrlOverride();
        return override.isEmpty() ? BuildConfig.CHEMCORE_LIBRARY_MANIFEST_URL : override;
    }

    public String manifestUrlOverride() {
        String value = prefs.getString(PREF_MANIFEST_URL, "");
        return value == null ? "" : value.trim();
    }

    public boolean usingDeveloperManifestOverride() { return !manifestUrlOverride().isEmpty(); }

    public void setManifestUrl(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (!value.isEmpty()) {
            try {
                URL u = new URL(value);
                if (!"https".equalsIgnoreCase(u.getProtocol()) || u.getUserInfo() != null)
                    throw new IllegalArgumentException("ChemCore update source must be HTTPS without embedded credentials");
            } catch (IllegalArgumentException e) { throw e; }
            catch (Exception e) { throw new IllegalArgumentException("invalid ChemCore manifest URL", e); }
        }
        prefs.edit().putString(PREF_MANIFEST_URL, value).apply();
    }

    public CheckSummary check(String appVersion) throws Exception {
        String raw = manifestUrl();
        if (raw == null || raw.trim().isEmpty()) throw new IllegalStateException("ChemCore update source is not configured");
        ChemCoreLibraryUpdateClient.CheckResult remote = client.check(new URL(raw.trim()));
        ChemCoreLibraryManifest m = remote.manifest;
        ChemCoreLibraryVerifier.VerificationResult validated =
                ChemCoreLibraryVerifier.verifyManifestOnly(m, appVersion, REQUIRED_SAMPLING_PROFILE);
        if (!validated.accepted) throw new IllegalStateException(validated.reason);

        String installed = store.activeVersion();
        boolean newer = installed == null || installed.isEmpty();
        if (!newer) newer = ChemCoreLibraryVersion.parse(m.libraryVersion).compareTo(ChemCoreLibraryVersion.parse(installed)) > 0;
        String message = newer ? "new compatible stable library available" : "installed library is current or newer";
        return new CheckSummary(remote, newer, installed, message);
    }

    public ChemCoreLibraryStore.InstallResult install(CheckSummary checked, String appVersion, boolean measurementActive) throws Exception {
        if (checked == null || checked.remote == null) throw new IllegalArgumentException("checked update required");
        if (!checked.updateAvailable) return new ChemCoreLibraryStore.InstallResult(false, "no newer compatible library", store.activeVersion());
        ChemCoreLibraryVerifier.VerificationResult validated =
                ChemCoreLibraryVerifier.verifyManifestOnly(checked.remote.manifest, appVersion, REQUIRED_SAMPLING_PROFILE);
        if (!validated.accepted) return new ChemCoreLibraryStore.InstallResult(false, validated.reason, store.activeVersion());

        File staging = new File(context.getCacheDir(), "chemcore-library-download");
        if (!staging.exists() && !staging.mkdirs()) throw new IllegalStateException("cannot create ChemCore download staging directory");
        File downloaded = client.downloadPayload(checked.remote, staging);
        try {
            return store.installVerifiedSnapshot(checked.remote.manifest, downloaded, appVersion, measurementActive, false);
        } finally {
            //noinspection ResultOfMethodCallIgnored
            downloaded.delete();
        }
    }

    public File activePayload() { return store.activePayload(); }
    public ChemCoreLibraryStore.InstallResult rollback(boolean measurementActive, String appVersion) {
        return store.rollbackToPrevious(measurementActive, appVersion);
    }

    /** Fail closed for both firmware numeric states and Android/ChemCore named states. */
    public static boolean measurementOrAnalysisActive(JSONObject snapshot) {
        // HR069-R1: a developer ChemCore controlled capture can be active while ESP32 state is idle.
        // Never swap the stable reference snapshot during any active local recording/capture.
        if (MeasurementSessionRegistry.chemCoreCaptureRequested()
                || !MeasurementSessionRegistry.activeSessionId().isEmpty()) return true;
        if (snapshot == null) return false;
        JSONObject m = snapshot.optJSONObject("measurement");
        if (m == null) return false;
        Object raw = m.opt("state");
        if (raw instanceof Number) {
            int s=((Number)raw).intValue();
            if (s==1 || s==2) return true;
        }
        String state=String.valueOf(raw==null?"":raw).trim().toLowerCase();
        return "warm".equals(state)||"warmup".equals(state)||"measure".equals(state)||"measuring".equals(state)
                ||"analysis".equals(state)||"analyzing".equals(state)||"recovering".equals(state);
    }
}
