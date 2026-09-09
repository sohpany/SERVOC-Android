package com.servoc.app.chemcore;

import android.database.sqlite.SQLiteDatabase;

/** Idempotent ChemCore schema bootstrap kept separate from legacy DB migrations. */
public final class ChemCoreSchema {
    private ChemCoreSchema() {}

    public static final int SCHEMA_VERSION = 1;

    public static void ensure(SQLiteDatabase db) {
        if (db == null) throw new IllegalArgumentException("database required");
        db.execSQL("CREATE TABLE IF NOT EXISTS chemcore_sampling_profiles(" +
                "profile_id TEXT PRIMARY KEY," +
                "version TEXT NOT NULL," +
                "created_ts TEXT NOT NULL," +
                "chamber_width_m REAL NOT NULL," +
                "chamber_height_m REAL NOT NULL," +
                "chamber_length_m REAL NOT NULL," +
                "inlet_width_m REAL NOT NULL," +
                "inlet_height_m REAL NOT NULL," +
                "fan_width_m REAL," +
                "fan_height_m REAL," +
                "fan_voltage_v REAL," +
                "fan_current_a REAL," +
                "flow_status TEXT NOT NULL," +
                "flow_m3_s REAL," +
                "flow_uncertainty_m3_s REAL," +
                "notes TEXT," +
                "active INTEGER NOT NULL DEFAULT 0)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_chemcore_sampling_active ON chemcore_sampling_profiles(active,created_ts)");

        db.execSQL("CREATE TABLE IF NOT EXISTS chemcore_session_context(" +
                "session_id TEXT PRIMARY KEY," +
                "sampling_profile_id TEXT NOT NULL," +
                "mode TEXT NOT NULL," +
                "started_ts TEXT NOT NULL," +
                "ended_ts TEXT," +
                "analysis_state TEXT," +
                "feature_schema TEXT," +
                "outcome TEXT," +
                "evidence_tier TEXT," +
                "analytics TEXT," +
                "FOREIGN KEY(session_id) REFERENCES measurement_sessions(session_id) ON DELETE CASCADE," +
                "FOREIGN KEY(sampling_profile_id) REFERENCES chemcore_sampling_profiles(profile_id))");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_chemcore_session_profile ON chemcore_session_context(sampling_profile_id,started_ts)");

        db.execSQL("CREATE TABLE IF NOT EXISTS chemcore_channel_timing(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "session_id TEXT NOT NULL," +
                "sample_t_ms INTEGER NOT NULL," +
                "scan_cycle_id INTEGER," +
                "sensor_key TEXT NOT NULL," +
                "channel_offset_us INTEGER," +
                "FOREIGN KEY(session_id) REFERENCES measurement_sessions(session_id) ON DELETE CASCADE)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_chemcore_timing_session ON chemcore_channel_timing(session_id,sample_t_ms,sensor_key)");
    }
}
