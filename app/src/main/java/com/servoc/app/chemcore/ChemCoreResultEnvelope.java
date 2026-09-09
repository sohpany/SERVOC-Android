package com.servoc.app.chemcore;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Auditable ChemCore analytical result payload shared by DB/report exporters. */
public final class ChemCoreResultEnvelope {
    public static final String SCHEMA = "chemcore-result/1";

    public static final class CandidateSummary {
        public final long chemicalUid;
        public final String label;
        public final double posterior;
        public final double bestPpm;
        public final double residualRms;
        public final int contributors;
        public final ChemCoreDecision.EvidenceTier tier;

        public CandidateSummary(
                long chemicalUid,
                String label,
                double posterior,
                double bestPpm,
                double residualRms,
                int contributors,
                ChemCoreDecision.EvidenceTier tier) {
            this.chemicalUid = chemicalUid;
            this.label = label == null ? "" : label;
            this.posterior = posterior;
            this.bestPpm = bestPpm;
            this.residualRms = residualRms;
            this.contributors = contributors;
            this.tier = tier;
        }
    }

    public final String sessionId;
    public final ChemCoreDecision.Outcome outcome;
    public final ChemCoreDecision.EvidenceTier evidenceTier;
    public final Long chemicalUid;
    public final String chemicalLabel;
    public final double ppm;
    public final double predictionLow;
    public final double predictionHigh;
    public final double oodScore;
    public final double topPosterior;
    public final double normalizedEntropy;
    public final String samplingProfileId;
    public final String featureSchema;
    public final String modelVersion;
    public final String note;
    public final List<CandidateSummary> candidates;

    public ChemCoreResultEnvelope(
            String sessionId,
            ChemCoreDecision.Outcome outcome,
            ChemCoreDecision.EvidenceTier evidenceTier,
            Long chemicalUid,
            String chemicalLabel,
            double ppm,
            double predictionLow,
            double predictionHigh,
            double oodScore,
            double topPosterior,
            double normalizedEntropy,
            String samplingProfileId,
            String featureSchema,
            String modelVersion,
            String note,
            List<CandidateSummary> candidates) {
        this.sessionId = sessionId == null ? "" : sessionId;
        this.outcome = outcome == null
                ? ChemCoreDecision.Outcome.INVALID_MEASUREMENT
                : outcome;
        this.evidenceTier = evidenceTier == null
                ? ChemCoreDecision.EvidenceTier.M0_CATALOG_ONLY
                : evidenceTier;
        this.chemicalUid = chemicalUid;
        this.chemicalLabel = chemicalLabel == null ? "" : chemicalLabel;
        this.ppm = ppm;
        this.predictionLow = predictionLow;
        this.predictionHigh = predictionHigh;
        this.oodScore = oodScore;
        this.topPosterior = topPosterior;
        this.normalizedEntropy = normalizedEntropy;
        this.samplingProfileId = samplingProfileId == null ? "" : samplingProfileId;
        this.featureSchema = featureSchema == null
                ? ChemCoreFeaturePipeline.SCHEMA_VERSION
                : featureSchema;
        this.modelVersion = modelVersion == null ? "" : modelVersion;
        this.note = note == null ? "" : note;
        this.candidates = candidates == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(candidates));
    }

    /**
     * Serializes the scientific result envelope. JSONException is treated as an internal
     * programming error so report/database callers are not forced to propagate a checked
     * Android JSON exception for a structure made only from JSON-safe values.
     */
    public JSONObject toJson() {
        try {
            JSONObject out = new JSONObject();
            out.put("schema", SCHEMA);
            out.put("session_id", sessionId);
            out.put("outcome", outcome.name());
            out.put("evidence_tier", evidenceTier.name());

            if (chemicalUid != null) {
                out.put("chemical_uid", chemicalUid);
            }
            if (!chemicalLabel.isEmpty()) {
                out.put("chemical", chemicalLabel);
            }

            putFinite(out, "ppm", ppm);
            putFinite(out, "prediction_low", predictionLow);
            putFinite(out, "prediction_high", predictionHigh);
            putFinite(out, "ood_score", oodScore);
            putFinite(out, "top_posterior", topPosterior);
            putFinite(out, "normalized_entropy", normalizedEntropy);
            out.put("sampling_profile_id", samplingProfileId);
            out.put("feature_schema", featureSchema);
            out.put("model_version", modelVersion);
            out.put("note", note);

            JSONArray candidateArray = new JSONArray();
            for (CandidateSummary candidate : candidates) {
                if (candidate == null) {
                    continue;
                }
                JSONObject item = new JSONObject();
                item.put("chemical_uid", candidate.chemicalUid);
                item.put("label", candidate.label);
                putFinite(item, "posterior", candidate.posterior);
                putFinite(item, "best_ppm", candidate.bestPpm);
                putFinite(item, "residual_rms", candidate.residualRms);
                item.put("contributors", candidate.contributors);
                if (candidate.tier != null) {
                    item.put("evidence_tier", candidate.tier.name());
                }
                candidateArray.put(item);
            }
            out.put("candidates", candidateArray);
            out.put(
                    "quantitative_occupational_interpretation_permitted",
                    ChemCoreDecision.permitsQuantitativeOccupationalInterpretation(
                            outcome, evidenceTier));
            return out;
        } catch (JSONException e) {
            throw new IllegalStateException("Unable to serialize ChemCore result envelope", e);
        }
    }

    private static void putFinite(JSONObject target, String key, double value) throws JSONException {
        target.put(key, Double.isFinite(value) ? value : JSONObject.NULL);
    }
}
