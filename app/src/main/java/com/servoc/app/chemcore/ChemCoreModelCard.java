package com.servoc.app.chemcore;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

/** Minimal machine-readable model card for scientific audit and release freeze. */
public final class ChemCoreModelCard {
    public static final String SCHEMA = "chemcore-model-card/1";

    public final String modelType;
    public final String version;
    public final String featureSchema;
    public final String trainingScope;
    public final String validationProtocol;
    public final String samplingProfileId;
    public final String validityDomain;
    public final String sha256;
    public final int trainingExperiments;
    public final int validationExperiments;
    public final int blindTestExperiments;
    public final Map<String, Double> metrics;
    public final List<String> sourceIds;

    public ChemCoreModelCard(
            String modelType,
            String version,
            String featureSchema,
            String trainingScope,
            String validationProtocol,
            String samplingProfileId,
            String validityDomain,
            int trainingExperiments,
            int validationExperiments,
            int blindTestExperiments,
            Map<String, Double> metrics,
            List<String> sourceIds,
            String sha256) {
        this.modelType = modelType == null ? "" : modelType;
        this.version = version == null ? "" : version;
        this.featureSchema = featureSchema == null ? "" : featureSchema;
        this.trainingScope = trainingScope == null ? "" : trainingScope;
        this.validationProtocol = validationProtocol == null ? "" : validationProtocol;
        this.samplingProfileId = samplingProfileId == null ? "" : samplingProfileId;
        this.validityDomain = validityDomain == null ? "" : validityDomain;
        this.trainingExperiments = trainingExperiments;
        this.validationExperiments = validationExperiments;
        this.blindTestExperiments = blindTestExperiments;
        this.metrics = metrics;
        this.sourceIds = sourceIds;
        this.sha256 = sha256 == null ? "" : sha256;
    }

    /**
     * Serializes this immutable audit record without leaking Android's checked JSONException
     * into every reporting caller. The values are already constrained to JSON-compatible
     * primitives/containers; a serialization failure therefore indicates a programming error.
     */
    public JSONObject toJson() {
        try {
            JSONObject out = new JSONObject();
            out.put("schema", SCHEMA);
            out.put("model_type", modelType);
            out.put("version", version);
            out.put("feature_schema", featureSchema);
            out.put("training_scope", trainingScope);
            out.put("validation_protocol", validationProtocol);
            out.put("sampling_profile_id", samplingProfileId);
            out.put("validity_domain", validityDomain);
            out.put("training_experiments", trainingExperiments);
            out.put("validation_experiments", validationExperiments);
            out.put("blind_test_experiments", blindTestExperiments);

            JSONObject metricJson = new JSONObject();
            if (metrics != null) {
                for (Map.Entry<String, Double> entry : metrics.entrySet()) {
                    String key = entry.getKey();
                    Double value = entry.getValue();
                    if (key != null && !key.isEmpty() && value != null && Double.isFinite(value)) {
                        metricJson.put(key, value);
                    }
                }
            }
            out.put("metrics", metricJson);

            JSONArray sourceJson = new JSONArray();
            if (sourceIds != null) {
                for (String sourceId : sourceIds) {
                    if (sourceId != null && !sourceId.isEmpty()) {
                        sourceJson.put(sourceId);
                    }
                }
            }
            out.put("source_ids", sourceJson);
            out.put("sha256", sha256);
            return out;
        } catch (JSONException e) {
            throw new IllegalStateException("Unable to serialize ChemCore model card", e);
        }
    }
}
