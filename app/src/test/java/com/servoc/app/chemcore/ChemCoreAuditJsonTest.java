package com.servoc.app.chemcore;

import org.json.JSONObject;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.*;

/** Regression coverage for the machine-readable scientific audit payloads. */
public class ChemCoreAuditJsonTest {

    @Test public void modelCardSerializesFiniteMetricsAndSources() {
        Map<String, Double> metrics = new LinkedHashMap<>();
        metrics.put("rmse", 0.42);
        metrics.put("invalid", Double.NaN);

        ChemCoreModelCard card = new ChemCoreModelCard(
                "QSPR_MOX",
                "1.0",
                ChemCoreFeaturePipeline.SCHEMA_VERSION,
                "training-set-v1",
                "leave-compound-out",
                "hazrob-chemcore-chamber-v2",
                "declared-domain",
                120,
                30,
                10,
                metrics,
                Arrays.asList("paper:1", "manufacturer:mq2", null, ""),
                "abc123");

        JSONObject json = card.toJson();
        assertEquals(ChemCoreModelCard.SCHEMA, json.optString("schema"));
        assertEquals("QSPR_MOX", json.optString("model_type"));
        assertEquals(0.42, json.optJSONObject("metrics").optDouble("rmse"), 1e-12);
        assertFalse(json.optJSONObject("metrics").has("invalid"));
        assertEquals(2, json.optJSONArray("source_ids").length());
        assertEquals("abc123", json.optString("sha256"));
    }

    @Test public void resultEnvelopePreservesNonFiniteAsJsonNullAndM4Gate() {
        ChemCoreResultEnvelope.CandidateSummary candidate =
                new ChemCoreResultEnvelope.CandidateSummary(
                        42L,
                        "Reference analyte",
                        0.91,
                        Double.NaN,
                        0.08,
                        6,
                        ChemCoreDecision.EvidenceTier.M4_HAZROB_VALIDATED);

        ChemCoreResultEnvelope envelope = new ChemCoreResultEnvelope(
                "session-001",
                ChemCoreDecision.Outcome.QUANTIFIED,
                ChemCoreDecision.EvidenceTier.M4_HAZROB_VALIDATED,
                42L,
                "Reference analyte",
                75.0,
                68.0,
                82.0,
                1.2,
                0.91,
                0.17,
                "hazrob-chemcore-chamber-v2",
                ChemCoreFeaturePipeline.SCHEMA_VERSION,
                "model-v1",
                "validated example",
                Collections.singletonList(candidate));

        JSONObject json = envelope.toJson();
        assertEquals(ChemCoreResultEnvelope.SCHEMA, json.optString("schema"));
        assertEquals("QUANTIFIED", json.optString("outcome"));
        assertTrue(json.optBoolean("quantitative_occupational_interpretation_permitted"));
        assertEquals(75.0, json.optDouble("ppm"), 1e-12);
        JSONObject candidateJson = json.optJSONArray("candidates").optJSONObject(0);
        assertTrue(candidateJson.isNull("best_ppm"));
        assertEquals(6, candidateJson.optInt("contributors"));
    }

    @Test public void nonM4QuantificationDoesNotPermitOccupationalInterpretation() {
        ChemCoreResultEnvelope envelope = new ChemCoreResultEnvelope(
                "session-002",
                ChemCoreDecision.Outcome.QUANTIFIED,
                ChemCoreDecision.EvidenceTier.M2_LITERATURE_OR_MANUFACTURER_SUPPORTED,
                null,
                "",
                12.0,
                Double.NaN,
                Double.NaN,
                Double.NaN,
                0.7,
                0.4,
                "hazrob-chemcore-chamber-v2",
                ChemCoreFeaturePipeline.SCHEMA_VERSION,
                "model-v1",
                "screening only",
                Collections.emptyList());

        assertFalse(envelope.toJson().optBoolean("quantitative_occupational_interpretation_permitted"));
    }
}
