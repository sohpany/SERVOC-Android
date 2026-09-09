package com.servoc.app.chemcore.library;

import android.content.Context;
import android.content.SharedPreferences;

import com.servoc.app.BuildConfig;

import org.json.JSONObject;

import java.io.File;
import java.net.URL;
import java.security.PublicKey;

/** High-level controller for discovery, verification, installation and rollback of ChemCore knowledge snapshots. */
public final class ChemCoreLibraryManager {
    /** Developer-only override. Normal users use BuildConfig.CHEMCORE_LIBRARY_MANIFEST_URL. */
    public static final String PREF_MANIFEST_URL = "chemcore_library_manifest_url";

    public static final class Status {
        public final boolean remoteSnapshotActive;
        public final String version;
        public final String sha256;
        public final String sourceCommit;
        public final String sourceLabel;
        public final ChemCoreEmbeddedFallback.Info embedded;
        public final boolean publicationKeyProvisioned;

        Status(boolean remoteSnapshotActive, String version, String sha256, String sourceCommit,
               String sourceLabel, ChemCoreEmbeddedFallback.Info embedded, boolean publicationKeyProvisioned) {
            this.remoteSnapshotActive = remoteSnapshotActive;
            this.version = version == null ? "" : version;
            this.sha256 = sha256 == null ? "" : sha256;
            this.sourceCommit = sourceCommit == null ? "" : sourceCommit;
            this.sourceLabel = sourceLabel == null ? "" : sourceLabel;
            this.embedded = embedded;
            this.publicationKeyProvisioned = publicationKeyProvisioned;
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
                remote ? "verified remote ChemCore snapshot" : embedded.label,
                embedded,
                ChemCorePublicationKeyProvider.isProvisioned(context));
    }

    /** Effective Stable manifest URL: developer override when present, otherwise the build-pinned public endpoint. */
    public String manifestUrl() {
        String override = manifestUrlOverride();
        return override.isEmpty() ? BuildConfig.CHEMCORE_LIBRARY_MANIFEST_URL : override;
    }

    /** Returns only the developer override, not the build-pinned default. */
    public String manifestUrlOverride() {
        String value = prefs.getString(PREF_MANIFEST_URL, "");
        return value == null ? "" : value.trim();
    }

    public boolean usingDeveloperManifestOverride() { return !manifestUrlOverride().isEmpty(); }

    /** Developer-only override; empty restores the build-pinned Stable endpoint. */
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
        if (m.channel != ChemCoreLibraryManifest.Channel.STABLE)
            throw new IllegalStateException("normal updater accepts Stable channel only");
        if (!m.isCompatibleWithApp(appVersion))
            return new CheckSummary(remote, false, store.activeVersion(), "remote library is not compatible with this app version");

        String installed = store.activeVersion();
        boolean newer = installed == null || installed.isEmpty();
        if (!newer) newer = ChemCoreLibraryVersion.parse(m.libraryVersion).compareTo(ChemCoreLibraryVersion.parse(installed)) > 0;
        String message = newer ? "new compatible stable library available" : "installed library is current or newer";
        return new CheckSummary(remote, newer, installed, message);
    }

    public ChemCoreLibraryStore.InstallResult install(CheckSummary checked, String appVersion, boolean measurementActive) throws Exception {
        if (checked == null || checked.remote == null) throw new IllegalArgumentException("checked update required");
        if (!checked.updateAvailable) return new ChemCoreLibraryStore.InstallResult(false, "no newer compatible library", store.activeVersion());
        if (checked.remote.manifest.channel != ChemCoreLibraryManifest.Channel.STABLE)
            return new ChemCoreLibraryStore.InstallResult(false, "research snapshots cannot be installed by normal updater", store.activeVersion());
        PublicKey key = ChemCorePublicationKeyProvider.load(context);
        if (key == null) return new ChemCoreLibraryStore.InstallResult(false, "stable publication key not provisioned", store.activeVersion());

        File staging = new File(context.getCacheDir(), "chemcore-library-download");
        if (!staging.exists() && !staging.mkdirs()) throw new IllegalStateException("cannot create ChemCore download staging directory");
        File downloaded = client.downloadPayload(checked.remote, staging);
        try {
            return store.installVerifiedSnapshot(checked.remote.manifest, downloaded, appVersion, key, measurementActive, false);
        } finally {
            //noinspection ResultOfMethodCallIgnored
            downloaded.delete();
        }
    }

    public File activePayload() { return store.activePayload(); }
    public ChemCoreLibraryStore.InstallResult rollback(boolean measurementActive) { return store.rollbackToPrevious(measurementActive); }

    /** Fail closed for both firmware numeric states and Android/ChemCore named states. */
    public static boolean measurementOrAnalysisActive(JSONObject snapshot) {
        if (snapshot == null) return false;
        JSONObject m = snapshot.optJSONObject("measurement");
        if (m == null) return false;
        Object raw = m.opt("state");
        if (raw instanceof Number) {
            int s=((Number)raw).intValue();
            if (s==1 || s==2) return true; // firmware WARM / MEASURE
        }
        String state=String.valueOf(raw==null?"":raw).trim().toLowerCase();
        return "warm".equals(state)||"warmup".equals(state)||"measure".equals(state)||"measuring".equals(state)
                ||"analysis".equals(state)||"analyzing".equals(state)||"recovering".equals(state);
    }
}
