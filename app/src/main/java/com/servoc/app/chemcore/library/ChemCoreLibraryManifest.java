package com.servoc.app.chemcore.library;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Immutable publication manifest for a remotely distributed ChemCore knowledge snapshot. */
public final class ChemCoreLibraryManifest {
    public enum Channel { STABLE, RESEARCH }

    public static final int MANIFEST_SCHEMA = 2;
    public static final String STABLE_PAYLOAD_SCHEMA = "chemcore-stable-snapshot-v1";
    public static final String INTEGRITY_MODE = "sha256";

    public final String libraryVersion;
    public final int schemaVersion;
    public final Channel channel;
    public final String createdUtc;
    public final String sourceCommitSha;
    public final String payloadFile;
    public final String payloadFormat;
    public final String payloadSchema;
    public final String integrityMode;
    public final String payloadSha256;
    public final long payloadSizeBytes;
    public final int chemicalCount;
    public final int sourceCount;
    public final int modelCount;
    public final int calibrationCount;
    public final String minAppVersion;
    public final String maxAppVersion;
    public final String featureSchemaVersion;
    public final String dataRevision;
    public final String evidenceRevision;
    public final List<String> samplingProfileCompatibility;

    public ChemCoreLibraryManifest(String libraryVersion, int schemaVersion, Channel channel, String createdUtc,
                                   String sourceCommitSha, String payloadFile, String payloadFormat, String payloadSchema,
                                   String integrityMode, String payloadSha256, long payloadSizeBytes, int chemicalCount,
                                   int sourceCount, int modelCount, int calibrationCount, String minAppVersion,
                                   String maxAppVersion, String featureSchemaVersion, String dataRevision,
                                   String evidenceRevision, List<String> samplingProfileCompatibility) {
        this.libraryVersion = n(libraryVersion);
        this.schemaVersion = schemaVersion;
        this.channel = channel == null ? Channel.STABLE : channel;
        this.createdUtc = n(createdUtc);
        this.sourceCommitSha = n(sourceCommitSha);
        this.payloadFile = n(payloadFile);
        this.payloadFormat = n(payloadFormat);
        this.payloadSchema = n(payloadSchema);
        this.integrityMode = n(integrityMode).toLowerCase(Locale.US);
        this.payloadSha256 = n(payloadSha256).toLowerCase(Locale.US);
        this.payloadSizeBytes = payloadSizeBytes;
        this.chemicalCount = chemicalCount;
        this.sourceCount = sourceCount;
        this.modelCount = modelCount;
        this.calibrationCount = calibrationCount;
        this.minAppVersion = n(minAppVersion);
        this.maxAppVersion = n(maxAppVersion);
        this.featureSchemaVersion = n(featureSchemaVersion);
        this.dataRevision = n(dataRevision);
        this.evidenceRevision = n(evidenceRevision);
        this.samplingProfileCompatibility = samplingProfileCompatibility == null
                ? List.of() : Collections.unmodifiableList(new ArrayList<>(samplingProfileCompatibility));
    }

    public static ChemCoreLibraryManifest fromJson(JSONObject j) {
        if (j == null) throw new IllegalArgumentException("manifest JSON required");
        List<String> sampling = new ArrayList<>();
        JSONArray a = j.optJSONArray("sampling_profile_compatibility");
        if (a != null) for (int i = 0; i < a.length(); i++) {
            String s = a.optString(i, "").trim();
            if (!s.isEmpty()) sampling.add(s);
        }
        String ch = j.optString("channel", "stable").trim().toUpperCase(Locale.US);
        Channel channel;
        try { channel = Channel.valueOf(ch); }
        catch (Exception e) { throw new IllegalArgumentException("unsupported library channel: " + ch, e); }
        return new ChemCoreLibraryManifest(
                j.optString("library_version", ""), j.optInt("schema_version", 0), channel,
                j.optString("created_utc", ""), j.optString("source_commit_sha", ""),
                j.optString("payload_file", ""), j.optString("payload_format", ""),
                j.optString("payload_schema", ""), j.optString("integrity_mode", ""),
                j.optString("payload_sha256", ""), j.optLong("payload_size_bytes", -1),
                j.optInt("chemical_count", -1), j.optInt("source_count", -1),
                j.optInt("model_count", -1), j.optInt("calibration_count", -1),
                j.optString("min_app_version", ""), j.optString("max_app_version", ""),
                j.optString("feature_schema_version", ""), j.optString("data_revision", ""),
                j.optString("evidence_revision", ""), sampling);
    }

    public List<String> validate() {
        List<String> e = new ArrayList<>();
        try { ChemCoreLibraryVersion.parse(libraryVersion); } catch (Exception x) { e.add("invalid library_version"); }
        if (schemaVersion != MANIFEST_SCHEMA) e.add("unsupported schema_version: " + schemaVersion);
        if (createdUtc.isEmpty()) e.add("created_utc required");
        if (!sourceCommitSha.matches("[0-9a-fA-F]{7,64}")) e.add("source_commit_sha required");
        if (payloadFile.isEmpty() || payloadFile.contains("..") || payloadFile.startsWith("/") || payloadFile.contains("\\")) e.add("unsafe payload_file");
        if (!("sqlite".equalsIgnoreCase(payloadFormat) || "json".equalsIgnoreCase(payloadFormat))) e.add("unsupported payload_format");
        if (payloadSchema.isEmpty()) e.add("payload_schema required");
        if (!INTEGRITY_MODE.equals(integrityMode)) e.add("integrity_mode must be sha256");
        if (!payloadSha256.matches("[0-9a-f]{64}")) e.add("payload_sha256 must be 64 hex chars");
        if (payloadSizeBytes <= 0) e.add("payload_size_bytes must be positive");
        if (chemicalCount < 0 || sourceCount < 0 || modelCount < 0 || calibrationCount < 0) e.add("record counts must be non-negative");
        if (minAppVersion.isEmpty()) e.add("min_app_version required");
        else try { ChemCoreLibraryVersion.parse(minAppVersion); } catch (Exception x) { e.add("invalid min_app_version"); }
        if (!maxAppVersion.isEmpty()) try { ChemCoreLibraryVersion.parse(maxAppVersion); } catch (Exception x) { e.add("invalid max_app_version"); }
        if (featureSchemaVersion.isEmpty()) e.add("feature_schema_version required");
        if (dataRevision.isEmpty()) e.add("data_revision required");
        if (evidenceRevision.isEmpty()) e.add("evidence_revision required");
        if (channel == Channel.STABLE) {
            if (!"json".equalsIgnoreCase(payloadFormat)) e.add("stable payload_format must be json");
            if (!STABLE_PAYLOAD_SCHEMA.equals(payloadSchema)) e.add("stable payload_schema must be " + STABLE_PAYLOAD_SCHEMA);
        }
        return e;
    }

    public boolean isCompatibleWithApp(String appVersion) {
        ChemCoreLibraryVersion app = ChemCoreLibraryVersion.parse(appVersion);
        if (!minAppVersion.isEmpty() && app.compareTo(ChemCoreLibraryVersion.parse(minAppVersion)) < 0) return false;
        return maxAppVersion.isEmpty() || app.compareTo(ChemCoreLibraryVersion.parse(maxAppVersion)) <= 0;
    }

    public boolean isCompatibleWithSamplingProfile(String profileId) {
        if (samplingProfileCompatibility.isEmpty()) return false;
        return profileId != null && samplingProfileCompatibility.contains(profileId.trim());
    }

    public JSONObject toJson() {
        JSONObject j = new JSONObject();
        try {
            j.put("library_version", libraryVersion); j.put("schema_version", schemaVersion);
            j.put("channel", channel.name().toLowerCase(Locale.US)); j.put("created_utc", createdUtc);
            j.put("source_commit_sha", sourceCommitSha); j.put("payload_file", payloadFile);
            j.put("payload_format", payloadFormat); j.put("payload_schema", payloadSchema);
            j.put("integrity_mode", integrityMode); j.put("payload_sha256", payloadSha256);
            j.put("payload_size_bytes", payloadSizeBytes); j.put("chemical_count", chemicalCount);
            j.put("source_count", sourceCount); j.put("model_count", modelCount); j.put("calibration_count", calibrationCount);
            j.put("min_app_version", minAppVersion); if (!maxAppVersion.isEmpty()) j.put("max_app_version", maxAppVersion);
            j.put("feature_schema_version", featureSchemaVersion); j.put("data_revision", dataRevision);
            j.put("evidence_revision", evidenceRevision);
            JSONArray a = new JSONArray(); for (String x : samplingProfileCompatibility) a.put(x); j.put("sampling_profile_compatibility", a);
        } catch (Exception impossible) { throw new IllegalStateException("manifest serialization failed", impossible); }
        return j;
    }

    private static String n(String s) { return s == null ? "" : s.trim(); }
}
