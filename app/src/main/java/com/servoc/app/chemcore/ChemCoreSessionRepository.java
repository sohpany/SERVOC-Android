package com.servoc.app.chemcore;

import android.content.ContentValues;
import android.database.Cursor;
import com.servoc.app.data.ExperimentsDb;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** Persists ChemCore context without modifying the legacy measurement session payload. */
public final class ChemCoreSessionRepository {
    private final ExperimentsDb db;

    public ChemCoreSessionRepository(ExperimentsDb db) {
        if (db == null) throw new IllegalArgumentException("database required");
        this.db = db;
        ChemCoreSchema.ensure(db.getWritableDatabase());
    }

    public boolean start(String sessionId, SamplingProfile profile, ChemCoreSessionController.Mode mode,
                         ChemCoreSessionController.State state) {
        if (sessionId == null || sessionId.trim().isEmpty() || profile == null) return false;
        ContentValues v = new ContentValues();
        v.put("session_id", sessionId);
        v.put("sampling_profile_id", profile.profileId);
        v.put("mode", mode == null ? ChemCoreSessionController.Mode.CHEMCORE_ANALYSIS.name() : mode.name());
        v.put("started_ts", isoNow());
        v.put("analysis_state", state == null ? ChemCoreSessionController.State.CONDITION.name() : state.name());
        v.put("feature_schema", ChemCoreFeaturePipeline.SCHEMA_VERSION);
        long row = db.getWritableDatabase().insertWithOnConflict("chemcore_session_context", null, v,
                android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE);
        return row >= 0;
    }

    public boolean updateState(String sessionId, ChemCoreSessionController.State state, String analytics) {
        if (sessionId == null || sessionId.trim().isEmpty() || state == null) return false;
        ContentValues v = new ContentValues();
        v.put("analysis_state", state.name());
        if (analytics != null) v.put("analytics", analytics);
        return db.getWritableDatabase().update("chemcore_session_context", v, "session_id=?", new String[]{sessionId}) == 1;
    }

    public boolean finish(String sessionId, ChemCoreSessionController.State state,
                          ChemCoreDecision.Outcome outcome, ChemCoreDecision.EvidenceTier tier, String analytics) {
        if (sessionId == null || sessionId.trim().isEmpty()) return false;
        ContentValues v = new ContentValues();
        v.put("ended_ts", isoNow());
        if (state != null) v.put("analysis_state", state.name());
        if (outcome != null) v.put("outcome", outcome.name());
        if (tier != null) v.put("evidence_tier", tier.name());
        if (analytics != null) v.put("analytics", analytics);
        return db.getWritableDatabase().update("chemcore_session_context", v, "session_id=?", new String[]{sessionId}) == 1;
    }

    public String samplingProfileId(String sessionId) {
        if (sessionId == null) return null;
        try (Cursor c = db.getReadableDatabase().rawQuery(
                "SELECT sampling_profile_id FROM chemcore_session_context WHERE session_id=?", new String[]{sessionId})) {
            return c.moveToFirst() ? c.getString(0) : null;
        }
    }

    private static String isoNow() {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        return f.format(new Date());
    }
}
