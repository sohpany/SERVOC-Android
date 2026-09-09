package com.servoc.app.chemcore;

import java.util.EnumSet;

/**
 * Deterministic ChemCore analysis state machine. It deliberately separates controlled
 * analysis from free-moving SURVEY operation and refuses to start focused analysis while
 * the robot is moving.
 */
public final class ChemCoreSessionController {
    public enum Mode { SURVEY, CHEMCORE_ANALYSIS, AMCI_CONFIRMATION }
    public enum State {
        IDLE,
        CONDITION,
        BASELINE,
        EXPOSURE,
        STABILITY_CHECK,
        FEATURE_EXTRACTION,
        IDENTITY,
        OPEN_SET_GATE,
        QUANTIFICATION,
        CONSISTENCY_CHECK,
        REPORT,
        RECOVERY,
        COMPLETE,
        ABORTED,
        ERROR
    }

    public enum Event {
        START,
        CONDITION_READY,
        BASELINE_STABLE,
        EXPOSURE_WINDOW_READY,
        RESPONSE_STABLE,
        FEATURES_READY,
        IDENTITY_READY,
        OPEN_SET_ACCEPTED,
        OPEN_SET_REJECTED,
        QUANTIFICATION_READY,
        CONSISTENCY_ACCEPTED,
        MIXTURE_SUSPECTED,
        REPORT_SAVED,
        RECOVERY_COMPLETE,
        EXTEND_EXPOSURE,
        REPEAT_EXPOSURE,
        ABORT,
        FAIL
    }

    public static final class Snapshot {
        public final Mode mode;
        public final State state;
        public final String reason;
        public final int exposurePass;

        Snapshot(Mode mode, State state, String reason, int exposurePass) {
            this.mode = mode;
            this.state = state;
            this.reason = reason == null ? "" : reason;
            this.exposurePass = exposurePass;
        }
    }

    private Mode mode = Mode.SURVEY;
    private State state = State.IDLE;
    private String reason = "";
    private int exposurePass = 0;

    public Snapshot snapshot() { return new Snapshot(mode, state, reason, exposurePass); }

    public Snapshot start(Mode requestedMode, boolean robotStationary, boolean mqPowered, SamplingProfile samplingProfile) {
        if (requestedMode == null) requestedMode = Mode.CHEMCORE_ANALYSIS;
        if (requestedMode == Mode.SURVEY) {
            mode = Mode.SURVEY; state = State.IDLE; reason = "survey mode"; exposurePass = 0; return snapshot();
        }
        if (!robotStationary) return error("focused ChemCore analysis requires stationary robot");
        if (!mqPowered) return error("MQ array must be powered before ChemCore analysis");
        if (samplingProfile == null) return error("sampling profile required");
        mode = requestedMode;
        state = State.CONDITION;
        reason = samplingProfile.flowValidated() ? "validated sampling profile" : "sampling profile active; airflow not yet validated";
        exposurePass = 0;
        return snapshot();
    }

    public Snapshot on(Event event, boolean robotStationary) {
        if (event == null) return snapshot();
        if (event == Event.ABORT) { state = State.ABORTED; reason = "operator/system abort"; return snapshot(); }
        if (event == Event.FAIL) return error("analysis failure");
        if (requiresStationary(state) && !robotStationary) return error("robot motion detected during controlled analysis");

        switch (state) {
            case CONDITION:
                if (event == Event.CONDITION_READY) state = State.BASELINE;
                break;
            case BASELINE:
                if (event == Event.BASELINE_STABLE) { state = State.EXPOSURE; exposurePass++; }
                break;
            case EXPOSURE:
                if (event == Event.EXPOSURE_WINDOW_READY) state = State.STABILITY_CHECK;
                break;
            case STABILITY_CHECK:
                if (event == Event.EXTEND_EXPOSURE) state = State.EXPOSURE;
                else if (event == Event.RESPONSE_STABLE) state = State.FEATURE_EXTRACTION;
                break;
            case FEATURE_EXTRACTION:
                if (event == Event.FEATURES_READY) state = State.IDENTITY;
                break;
            case IDENTITY:
                if (event == Event.IDENTITY_READY) state = State.OPEN_SET_GATE;
                break;
            case OPEN_SET_GATE:
                if (event == Event.OPEN_SET_REJECTED) { state = State.REPORT; reason = "open-set rejection"; }
                else if (event == Event.OPEN_SET_ACCEPTED) state = State.QUANTIFICATION;
                break;
            case QUANTIFICATION:
                if (event == Event.QUANTIFICATION_READY) state = State.CONSISTENCY_CHECK;
                break;
            case CONSISTENCY_CHECK:
                if (event == Event.CONSISTENCY_ACCEPTED) state = State.REPORT;
                else if (event == Event.MIXTURE_SUSPECTED) { state = State.REPORT; reason = "possible mixture or interference"; }
                else if (event == Event.REPEAT_EXPOSURE) state = State.RECOVERY;
                break;
            case REPORT:
                if (event == Event.REPORT_SAVED) state = State.RECOVERY;
                break;
            case RECOVERY:
                if (event == Event.RECOVERY_COMPLETE) state = State.COMPLETE;
                else if (event == Event.REPEAT_EXPOSURE) { state = State.BASELINE; reason = "AMCI repeat requested"; }
                break;
            default:
                break;
        }
        return snapshot();
    }

    public static boolean requiresStationary(State s) {
        return s != null && EnumSet.of(State.CONDITION, State.BASELINE, State.EXPOSURE, State.STABILITY_CHECK,
                State.FEATURE_EXTRACTION, State.IDENTITY, State.OPEN_SET_GATE, State.QUANTIFICATION,
                State.CONSISTENCY_CHECK, State.REPORT, State.RECOVERY).contains(s);
    }

    private Snapshot error(String why) {
        state = State.ERROR; reason = why == null ? "error" : why; return snapshot();
    }
}
