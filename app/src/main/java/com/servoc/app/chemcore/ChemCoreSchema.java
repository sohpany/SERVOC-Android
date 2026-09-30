package com.servoc.app.chemcore;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/** Idempotent ChemCore schema bootstrap kept separate from legacy DB migrations. */
public final class ChemCoreSchema {
    private ChemCoreSchema() {}

    public static final int SCHEMA_VERSION = 5;

    public static void ensure(SQLiteDatabase db) {
        if (db == null) throw new IllegalArgumentException("database required");
        ensureSampling(db);
        ensureMolecularIntelligence(db);
        ensureModelGovernance(db);
        ensureReferenceLineageGuards(db);
    }

    private static void ensureSampling(SQLiteDatabase db) {
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
                "internal_displacement_m3 REAL NOT NULL DEFAULT 0," +
                "flow_status TEXT NOT NULL," +
                "flow_m3_s REAL," +
                "flow_uncertainty_m3_s REAL," +
                "notes TEXT," +
                "active INTEGER NOT NULL DEFAULT 0)");
        // Foundation builds before schema v4 created this table without displacement.
        // Add it idempotently so chamber-v2 retains its 23 cm3 sensor-cap correction after DB round trips.
        ensureColumn(db, "chemcore_sampling_profiles", "internal_displacement_m3",
                "REAL NOT NULL DEFAULT 0");
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

    private static void ensureMolecularIntelligence(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS chemcore_structures(" +
                "chemical_uid INTEGER PRIMARY KEY," +
                "canonical_smiles TEXT," +
                "isomeric_smiles TEXT," +
                "inchi TEXT," +
                "inchikey TEXT," +
                "functional_groups TEXT," +
                "source_id TEXT," +
                "updated_ts TEXT NOT NULL," +
                "FOREIGN KEY(chemical_uid) REFERENCES chemicals(uid) ON DELETE CASCADE," +
                "FOREIGN KEY(source_id) REFERENCES scientific_sources(source_id))");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_chemcore_structure_inchikey ON chemcore_structures(inchikey)");

        db.execSQL("CREATE TABLE IF NOT EXISTS chemcore_properties(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "chemical_uid INTEGER NOT NULL," +
                "property_key TEXT NOT NULL," +
                "value_numeric REAL," +
                "value_text TEXT," +
                "unit TEXT," +
                "temp_c REAL," +
                "pressure_kpa REAL," +
                "method TEXT," +
                "source_id TEXT," +
                "uncertainty REAL," +
                "quality_tier TEXT," +
                "created_ts TEXT NOT NULL," +
                "FOREIGN KEY(chemical_uid) REFERENCES chemicals(uid) ON DELETE CASCADE," +
                "FOREIGN KEY(source_id) REFERENCES scientific_sources(source_id))");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_chemcore_property_target ON chemcore_properties(chemical_uid,property_key,created_ts)");

        db.execSQL("CREATE TABLE IF NOT EXISTS chemcore_descriptor_sets(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "chemical_uid INTEGER NOT NULL," +
                "descriptor_version TEXT NOT NULL," +
                "created_ts TEXT NOT NULL," +
                "source_method TEXT NOT NULL," +
                "source_id TEXT," +
                "payload TEXT NOT NULL," +
                "sha256 TEXT NOT NULL," +
                "active INTEGER NOT NULL DEFAULT 0," +
                "FOREIGN KEY(chemical_uid) REFERENCES chemicals(uid) ON DELETE CASCADE," +
                "FOREIGN KEY(source_id) REFERENCES scientific_sources(source_id))");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_chemcore_descriptors_target ON chemcore_descriptor_sets(chemical_uid,active,id)");

        db.execSQL("CREATE TABLE IF NOT EXISTS chemcore_sensor_response_priors(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "sensor_key TEXT NOT NULL," +
                "chemical_uid INTEGER NOT NULL," +
                "model_kind TEXT NOT NULL," +
                "version TEXT NOT NULL," +
                "equation TEXT," +
                "coefficients TEXT," +
                "min_ppm REAL," +
                "max_ppm REAL," +
                "min_temp_c REAL," +
                "max_temp_c REAL," +
                "min_rh REAL," +
                "max_rh REAL," +
                "sampling_profile_id TEXT," +
                "uncertainty_payload TEXT," +
                "evidence_tier TEXT NOT NULL," +
                "source_id TEXT," +
                "created_ts TEXT NOT NULL," +
                "active INTEGER NOT NULL DEFAULT 0," +
                "FOREIGN KEY(chemical_uid) REFERENCES chemicals(uid) ON DELETE CASCADE," +
                "FOREIGN KEY(sampling_profile_id) REFERENCES chemcore_sampling_profiles(profile_id)," +
                "FOREIGN KEY(source_id) REFERENCES scientific_sources(source_id))");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_chemcore_prior_target ON chemcore_sensor_response_priors(sensor_key,chemical_uid,active)");
    }

    private static void ensureModelGovernance(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS chemcore_model_artifacts(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "model_type TEXT NOT NULL," +
                "version TEXT NOT NULL," +
                "created_ts TEXT NOT NULL," +
                "feature_schema TEXT NOT NULL," +
                "training_scope TEXT," +
                "metrics TEXT," +
                "validity_domain TEXT," +
                "payload TEXT NOT NULL," +
                "sha256 TEXT NOT NULL," +
                "active INTEGER NOT NULL DEFAULT 0)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_chemcore_models_active ON chemcore_model_artifacts(model_type,active,id)");

        db.execSQL("CREATE TABLE IF NOT EXISTS chemcore_feature_references(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "chemical_uid INTEGER NOT NULL," +
                "kind TEXT NOT NULL," +
                "version TEXT NOT NULL," +
                "created_ts TEXT NOT NULL," +
                "feature_schema TEXT NOT NULL," +
                "sampling_profile_id TEXT," +
                "source_experiment_ids TEXT NOT NULL," +
                "source_session_ids TEXT NOT NULL," +
                "exposure_count INTEGER NOT NULL," +
                "payload TEXT NOT NULL," +
                "sha256 TEXT NOT NULL," +
                "active INTEGER NOT NULL DEFAULT 1," +
                "FOREIGN KEY(chemical_uid) REFERENCES chemicals(uid) ON DELETE CASCADE," +
                "FOREIGN KEY(sampling_profile_id) REFERENCES chemcore_sampling_profiles(profile_id))");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_chemcore_feature_ref_target ON chemcore_feature_references(chemical_uid,kind,active,id)");

        db.execSQL("CREATE TABLE IF NOT EXISTS chemcore_decisions(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "session_id TEXT NOT NULL," +
                "t_ms INTEGER NOT NULL," +
                "outcome TEXT NOT NULL," +
                "evidence_tier TEXT NOT NULL," +
                "chemical_uid INTEGER," +
                "ppm REAL," +
                "prediction_low REAL," +
                "prediction_high REAL," +
                "ood_score REAL," +
                "payload TEXT," +
                "FOREIGN KEY(session_id) REFERENCES measurement_sessions(session_id) ON DELETE CASCADE," +
                "FOREIGN KEY(chemical_uid) REFERENCES chemicals(uid))");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_chemcore_decision_session ON chemcore_decisions(session_id,t_ms)");
    }

    /**
     * Database-level fail-closed boundary for 0.10.2 / HR069 reference approval.
     * It prevents a reference row from becoming APPROVED unless its averaged capture and completed
     * DEVELOPER measurement session resolve to the same session/chemical/known concentration and
     * the originating session contains at least two persisted telemetry samples.
     */
    private static void ensureReferenceLineageGuards(SQLiteDatabase db) {
        String valid="EXISTS(SELECT 1 FROM captures c JOIN measurement_sessions m ON m.session_id=c.session_id " +
                "WHERE c.id=NEW.capture_id AND c.session_id=NEW.session_id AND c.chemical_uid=NEW.chemical_uid " +
                "AND c.phase='ready_average' AND m.operator_role='DEVELOPER' AND m.kind LIKE 'developer_%' " +
                "AND m.ended_ts IS NOT NULL AND TRIM(m.ended_ts)<>'' " +
                "AND (SELECT COUNT(*) FROM telemetry_samples t WHERE t.session_id=m.session_id)>=2 " +
                "AND ((NEW.known_ppm IS NULL AND c.known_ppm IS NULL) OR " +
                "(NEW.known_ppm IS NOT NULL AND c.known_ppm IS NOT NULL AND ABS(NEW.known_ppm-c.known_ppm)<=MAX(1.0,ABS(NEW.known_ppm),ABS(c.known_ppm))*0.000001)) " +
                "AND (NEW.reference_kind<>'calibration' OR NEW.known_ppm>0))";
        String ref="NEW.reference_kind IN ('fingerprint','calibration')";
        db.execSQL("CREATE TRIGGER IF NOT EXISTS trg_hr069_reference_approval_update " +
                "BEFORE UPDATE OF approved ON experiments WHEN NEW.approved=1 AND COALESCE(OLD.approved,0)<>1 AND "+ref+" BEGIN " +
                "SELECT CASE WHEN NOT ("+valid+") THEN RAISE(ABORT,'HR069_REFERENCE_LINEAGE_INVALID') END; END");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS trg_hr069_reference_approval_insert " +
                "BEFORE INSERT ON experiments WHEN NEW.approved=1 AND "+ref+" BEGIN " +
                "SELECT CASE WHEN NOT ("+valid+") THEN RAISE(ABORT,'HR069_REFERENCE_LINEAGE_INVALID') END; END");
    }

    private static void ensureColumn(SQLiteDatabase db, String table, String column, String definition) {
        if (hasColumn(db, table, column)) return;
        db.execSQL("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
    }

    private static boolean hasColumn(SQLiteDatabase db, String table, String column) {
        try (Cursor c = db.rawQuery("PRAGMA table_info(" + table + ")", null)) {
            int name = c.getColumnIndex("name");
            while (c.moveToNext()) {
                if (name >= 0 && column.equalsIgnoreCase(c.getString(name))) return true;
            }
        }
        return false;
    }
}
