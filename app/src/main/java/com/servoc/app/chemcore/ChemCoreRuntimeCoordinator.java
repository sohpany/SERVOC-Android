package com.servoc.app.chemcore;

import com.servoc.app.data.ExperimentsDb;

import org.json.JSONObject;

/**
 * Runtime bridge between the deterministic ChemCore state machine and persisted HazRob sessions.
 *
 * This class intentionally does not auto-assert BASELINE_STABLE, RESPONSE_STABLE, identity or
 * quantification. Those events must come from explicit evidence-producing code. It therefore
 * connects the new scientific state machine to the live application without silently collapsing
 * controlled analysis into the legacy single averaging phase.
 */
public final class ChemCoreRuntimeCoordinator {

    interface Persistence {
        boolean start(String sessionId, SamplingProfile profile, ChemCoreSessionController.Mode mode,
                      ChemCoreSessionController.State state);
        boolean update(String sessionId, ChemCoreSessionController.State state, String analytics);
        boolean finish(String sessionId, ChemCoreSessionController.State state,
                       ChemCoreDecision.Outcome outcome, ChemCoreDecision.EvidenceTier tier,
                       String analytics);
    }

    private static final class SqlitePersistence implements Persistence {
        private final ChemCoreSessionRepository repository;
        SqlitePersistence(ExperimentsDb db) { repository = new ChemCoreSessionRepository(db); }
        @Override public boolean start(String sessionId, SamplingProfile profile,
                                       ChemCoreSessionController.Mode mode,
                                       ChemCoreSessionController.State state) {
            return repository.start(sessionId, profile, mode, state);
        }
        @Override public boolean update(String sessionId, ChemCoreSessionController.State state,
                                        String analytics) {
            return repository.updateState(sessionId, state, analytics);
        }
        @Override public boolean finish(String sessionId, ChemCoreSessionController.State state,
                                        ChemCoreDecision.Outcome outcome,
                                        ChemCoreDecision.EvidenceTier tier,
                                        String analytics) {
            return repository.finish(sessionId, state, outcome, tier, analytics);
        }
    }

    public static final class Snapshot {
        public final ChemCoreSessionController.Mode mode;
        public final ChemCoreSessionController.State state;
        public final String reason;
        public final int exposurePass;
        public final String samplingProfileId;
        public final String layoutId;
        public final SamplingProfile.FlowStatus flowStatus;
        public final boolean flowValidated;
        public final boolean armed;
        public final String measurementSessionId;

        Snapshot(ChemCoreSessionController.Snapshot state,
                 SamplingProfile profile,
                 SamplingLayout layout,
                 boolean armed,
                 String measurementSessionId) {
            this.mode = state.mode;
            this.state = state.state;
            this.reason = state.reason;
            this.exposurePass = state.exposurePass;
            this.samplingProfileId = profile.profileId;
            this.layoutId = layout.layoutId;
            this.flowStatus = profile.flowStatus;
            this.flowValidated = profile.flowValidated();
            this.armed = armed;
            this.measurementSessionId = measurementSessionId == null ? "" : measurementSessionId;
        }

        public boolean controlled() {
            return mode != ChemCoreSessionController.Mode.SURVEY
                    && state != ChemCoreSessionController.State.IDLE;
        }

        public boolean terminal() {
            return state == ChemCoreSessionController.State.COMPLETE
                    || state == ChemCoreSessionController.State.ABORTED
                    || state == ChemCoreSessionController.State.ERROR;
        }
    }

    private final ChemCoreSessionController controller = new ChemCoreSessionController();
    private final SamplingProfile profile;
    private final SamplingLayout layout;
    private final Persistence persistence;
    private String measurementSessionId = "";
    private boolean armed = false;

    /** Creates the production runtime using the canonical persisted chamber-v2 profile. */
    public ChemCoreRuntimeCoordinator(ExperimentsDb db) {
        if (db == null) throw new IllegalArgumentException("database required");
        SamplingProfileRepository profiles = new SamplingProfileRepository(db);
        this.profile = profiles.ensureStandardProfile();
        this.layout = SamplingLayout.hazRobStandardArrayV2();
        if (!layout.samplingProfileId.equals(profile.profileId))
            throw new IllegalStateException("sampling layout/profile mismatch");
        this.persistence = new SqlitePersistence(db);
    }

    /** Test/research constructor without Android database dependencies. */
    ChemCoreRuntimeCoordinator(SamplingProfile profile, SamplingLayout layout, Persistence persistence) {
        if (profile == null || layout == null || persistence == null)
            throw new IllegalArgumentException("profile, layout and persistence required");
        if (!layout.samplingProfileId.equals(profile.profileId))
            throw new IllegalArgumentException("sampling layout/profile mismatch");
        this.profile = profile;
        this.layout = layout;
        this.persistence = persistence;
    }

    public synchronized Snapshot begin(boolean robotStationary, boolean mqPowered) {
        measurementSessionId = "";
        ChemCoreSessionController.Snapshot s = controller.start(
                ChemCoreSessionController.Mode.CHEMCORE_ANALYSIS,
                robotStationary,
                mqPowered,
                profile);
        armed = s.state == ChemCoreSessionController.State.CONDITION;
        return snapshot();
    }

    /**
     * Attaches the measurement session created by TelemetryRecorder after the ESP32 cycle starts.
     * No synthetic session identifier is created here; provenance always points to the real row.
     */
    public synchronized boolean attachMeasurementSession(String sessionId) {
        if (!armed || sessionId == null || sessionId.trim().isEmpty()) return false;
        String normalized = sessionId.trim();
        if (normalized.equals(measurementSessionId)) return true;
        if (!measurementSessionId.isEmpty()) return false;
        ChemCoreSessionController.Snapshot s = controller.snapshot();
        if (!persistence.start(normalized, profile, s.mode, s.state)) return false;
        measurementSessionId = normalized;
        return true;
    }

    public synchronized Snapshot advance(ChemCoreSessionController.Event event,
                                         boolean robotStationary,
                                         String analytics) {
        ChemCoreSessionController.Snapshot s = controller.on(event, robotStationary);
        persistState(s, analytics);
        if (isTerminal(s.state)) armed = false;
        return snapshot();
    }

    public synchronized Snapshot abort(String analytics) {
        ChemCoreSessionController.Snapshot s = controller.on(
                ChemCoreSessionController.Event.ABORT, true);
        armed = false;
        finishContext(s, ChemCoreDecision.Outcome.INVALID_MEASUREMENT,
                ChemCoreDecision.EvidenceTier.M0_CATALOG_ONLY, analytics);
        return snapshot();
    }

    public synchronized Snapshot fail(String analytics) {
        ChemCoreSessionController.Snapshot s = controller.on(
                ChemCoreSessionController.Event.FAIL, true);
        armed = false;
        finishContext(s, ChemCoreDecision.Outcome.INVALID_MEASUREMENT,
                ChemCoreDecision.EvidenceTier.M0_CATALOG_ONLY, analytics);
        return snapshot();
    }

    /** Persists the final analytical envelope without changing its evidence semantics. */
    public synchronized boolean finish(ChemCoreResultEnvelope result) {
        if (result == null || measurementSessionId.isEmpty()) return false;
        ChemCoreSessionController.Snapshot s = controller.snapshot();
        boolean ok = persistence.finish(
                measurementSessionId,
                s.state,
                result.outcome,
                result.evidenceTier,
                result.toJson().toString());
        if (ok && isTerminal(s.state)) armed = false;
        return ok;
    }

    /** Enforces the stationary interlock continuously while a controlled state is active. */
    public synchronized Snapshot observeMotion(int left, int right) {
        ChemCoreSessionController.Snapshot s = controller.snapshot();
        if (ChemCoreSessionController.requiresStationary(s.state) && (left != 0 || right != 0)) {
            return fail("{\"reason\":\"motion_interlock\"}");
        }
        return snapshot();
    }

    public synchronized Snapshot snapshot() {
        return new Snapshot(controller.snapshot(), profile, layout, armed, measurementSessionId);
    }

    public SamplingProfile profile() { return profile; }
    public SamplingLayout layout() { return layout; }

    public synchronized JSONObject statusJson() {
        Snapshot s = snapshot();
        JSONObject out = new JSONObject();
        try {
            out.put("mode", s.mode.name());
            out.put("state", s.state.name());
            out.put("reason", s.reason);
            out.put("exposure_pass", s.exposurePass);
            out.put("sampling_profile_id", s.samplingProfileId);
            out.put("layout_id", s.layoutId);
            out.put("layout_position_status", layout.positionStatus.name());
            out.put("flow_status", s.flowStatus.name());
            out.put("flow_validated", s.flowValidated);
            out.put("flow_l_s", Double.isFinite(profile.flowM3s) ? profile.flowM3s * 1000.0 : JSONObject.NULL);
            out.put("flow_uncertainty_l_s", Double.isFinite(profile.flowUncertaintyM3s)
                    ? profile.flowUncertaintyM3s * 1000.0 : JSONObject.NULL);
            out.put("effective_volume_l", profile.effectiveVolumeM3() * 1000.0);
            out.put("armed", s.armed);
            out.put("measurement_session_id", s.measurementSessionId);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to serialize ChemCore runtime status", e);
        }
        return out;
    }

    private void persistState(ChemCoreSessionController.Snapshot s, String analytics) {
        if (measurementSessionId.isEmpty()) return;
        if (isTerminal(s.state)) {
            finishContext(s,
                    s.state == ChemCoreSessionController.State.COMPLETE
                            ? ChemCoreDecision.Outcome.SCREENING
                            : ChemCoreDecision.Outcome.INVALID_MEASUREMENT,
                    ChemCoreDecision.EvidenceTier.M0_CATALOG_ONLY,
                    analytics);
        } else {
            persistence.update(measurementSessionId, s.state, analytics);
        }
    }

    private void finishContext(ChemCoreSessionController.Snapshot s,
                               ChemCoreDecision.Outcome outcome,
                               ChemCoreDecision.EvidenceTier tier,
                               String analytics) {
        if (measurementSessionId.isEmpty()) return;
        persistence.finish(measurementSessionId, s.state, outcome, tier, analytics);
    }

    private static boolean isTerminal(ChemCoreSessionController.State state) {
        return state == ChemCoreSessionController.State.COMPLETE
                || state == ChemCoreSessionController.State.ABORTED
                || state == ChemCoreSessionController.State.ERROR;
    }
}
