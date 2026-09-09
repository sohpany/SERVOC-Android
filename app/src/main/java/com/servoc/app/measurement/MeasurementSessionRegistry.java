package com.servoc.app.measurement;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Process-local bridge exposing the single active TelemetryRecorder measurement-session ID.
 * It carries identifiers only; telemetry and scientific records remain persisted in SQLite.
 */
public final class MeasurementSessionRegistry {
    private static final AtomicReference<String> ACTIVE = new AtomicReference<>("");
    private static final AtomicReference<String> LAST_COMPLETED = new AtomicReference<>("");

    private MeasurementSessionRegistry() {}

    static void started(String sessionId) {
        ACTIVE.set(sessionId == null ? "" : sessionId);
    }

    static void completed(String sessionId) {
        String value = sessionId == null ? "" : sessionId;
        LAST_COMPLETED.set(value);
        ACTIVE.compareAndSet(value, "");
    }

    static void resetActive(String expected) {
        if (expected == null || expected.isEmpty()) ACTIVE.set("");
        else ACTIVE.compareAndSet(expected, "");
    }

    public static String activeSessionId() {
        String s = ACTIVE.get();
        return s == null ? "" : s;
    }

    public static String lastCompletedSessionId() {
        String s = LAST_COMPLETED.get();
        return s == null ? "" : s;
    }
}
