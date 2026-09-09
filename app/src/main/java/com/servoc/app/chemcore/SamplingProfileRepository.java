package com.servoc.app.chemcore;

import android.content.ContentValues;
import android.database.Cursor;

import com.servoc.app.data.ExperimentsDb;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** SQLite persistence for versioned ChemCore sampling profiles. */
public final class SamplingProfileRepository {
    private static final String V1_ID = "hazrob-chemcore-chamber-v1";
    private static final String V2_ID = "hazrob-chemcore-chamber-v2";

    private static final String SELECT_COLUMNS =
            "profile_id,version,chamber_width_m,chamber_height_m,chamber_length_m," +
            "inlet_width_m,inlet_height_m,fan_width_m,fan_height_m,fan_voltage_v,fan_current_a," +
            "internal_displacement_m3,flow_status,flow_m3_s,flow_uncertainty_m3_s,notes";

    private final ExperimentsDb db;

    public SamplingProfileRepository(ExperimentsDb db) {
        if (db == null) throw new IllegalArgumentException("database required");
        this.db = db;
        ChemCoreSchema.ensure(db.getWritableDatabase());
        ensureCanonicalProfiles();
    }

    /**
     * Retains chamber-v1 for historical traceability and makes the adopted chamber-v2
     * the active profile for new ChemCore sessions.
     */
    private void ensureCanonicalProfiles() {
        if (byId(V1_ID) == null) save(SamplingProfile.hazRobPrototypeV1(), false);

        SamplingProfile standard = SamplingProfile.hazRobStandardChamberV2();
        SamplingProfile stored = byId(V2_ID);
        if (stored == null || !samePhysicalProfile(stored, standard)) {
            save(standard, true);
        } else {
            activate(V2_ID);
        }
    }

    /** Historical API retained for callers that explicitly require chamber-v1. */
    public SamplingProfile ensurePrototypeProfile() {
        SamplingProfile current = byId(V1_ID);
        if (current != null) return current;
        SamplingProfile p = SamplingProfile.hazRobPrototypeV1();
        save(p, false);
        return p;
    }

    /** Returns the canonical chamber adopted for current HazRob ChemCore analysis. */
    public SamplingProfile ensureStandardProfile() {
        SamplingProfile current = byId(V2_ID);
        if (current != null) {
            activate(V2_ID);
            return current;
        }
        SamplingProfile p = SamplingProfile.hazRobStandardChamberV2();
        save(p, true);
        return p;
    }

    public boolean save(SamplingProfile p, boolean active) {
        if (p == null) return false;
        if (active) db.getWritableDatabase().execSQL("UPDATE chemcore_sampling_profiles SET active=0");

        ContentValues v = new ContentValues();
        v.put("profile_id", p.profileId);
        v.put("version", p.version);
        v.put("created_ts", isoNow());
        v.put("chamber_width_m", p.chamberWidthM);
        v.put("chamber_height_m", p.chamberHeightM);
        v.put("chamber_length_m", p.chamberLengthM);
        v.put("inlet_width_m", p.inletWidthM);
        v.put("inlet_height_m", p.inletHeightM);
        v.put("fan_width_m", p.fanWidthM);
        v.put("fan_height_m", p.fanHeightM);
        v.put("fan_voltage_v", p.fanVoltageV);
        v.put("fan_current_a", p.fanCurrentA);
        v.put("internal_displacement_m3", p.internalDisplacementM3);
        v.put("flow_status", p.flowStatus.name());
        if (Double.isFinite(p.flowM3s)) v.put("flow_m3_s", p.flowM3s); else v.putNull("flow_m3_s");
        if (Double.isFinite(p.flowUncertaintyM3s)) v.put("flow_uncertainty_m3_s", p.flowUncertaintyM3s); else v.putNull("flow_uncertainty_m3_s");
        v.put("notes", p.notes);
        v.put("active", active ? 1 : 0);

        long row = db.getWritableDatabase().insertWithOnConflict(
                "chemcore_sampling_profiles",
                null,
                v,
                android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE);
        return row >= 0;
    }

    public boolean activate(String id) {
        if (id == null || id.trim().isEmpty() || byId(id) == null) return false;
        db.getWritableDatabase().beginTransaction();
        try {
            db.getWritableDatabase().execSQL("UPDATE chemcore_sampling_profiles SET active=0");
            ContentValues v = new ContentValues();
            v.put("active", 1);
            int changed = db.getWritableDatabase().update(
                    "chemcore_sampling_profiles", v, "profile_id=?", new String[]{id});
            db.getWritableDatabase().setTransactionSuccessful();
            return changed == 1;
        } finally {
            db.getWritableDatabase().endTransaction();
        }
    }

    public SamplingProfile active() {
        try (Cursor c = db.getReadableDatabase().rawQuery(
                "SELECT " + SELECT_COLUMNS +
                        " FROM chemcore_sampling_profiles WHERE active=1 ORDER BY created_ts DESC LIMIT 1",
                null)) {
            return c.moveToFirst() ? read(c) : null;
        }
    }

    public SamplingProfile byId(String id) {
        if (id == null || id.trim().isEmpty()) return null;
        try (Cursor c = db.getReadableDatabase().rawQuery(
                "SELECT " + SELECT_COLUMNS +
                        " FROM chemcore_sampling_profiles WHERE profile_id=? LIMIT 1",
                new String[]{id})) {
            return c.moveToFirst() ? read(c) : null;
        }
    }

    private static SamplingProfile read(Cursor c) {
        SamplingProfile.FlowStatus status;
        try {
            status = SamplingProfile.FlowStatus.valueOf(c.getString(12));
        } catch (Exception e) {
            status = SamplingProfile.FlowStatus.FLOW_UNCALIBRATED;
        }

        return new SamplingProfile(
                c.getString(0), c.getString(1),
                c.getDouble(2), c.getDouble(3), c.getDouble(4),
                c.getDouble(5), c.getDouble(6),
                nullable(c, 7), nullable(c, 8), nullable(c, 9), nullable(c, 10),
                nullable(c, 11),
                status, nullable(c, 13), nullable(c, 14), c.getString(15));
    }

    private static boolean samePhysicalProfile(SamplingProfile a, SamplingProfile b) {
        if (a == null || b == null) return false;
        return a.profileId.equals(b.profileId)
                && a.version.equals(b.version)
                && same(a.chamberWidthM, b.chamberWidthM)
                && same(a.chamberHeightM, b.chamberHeightM)
                && same(a.chamberLengthM, b.chamberLengthM)
                && same(a.inletWidthM, b.inletWidthM)
                && same(a.inletHeightM, b.inletHeightM)
                && same(a.fanWidthM, b.fanWidthM)
                && same(a.fanHeightM, b.fanHeightM)
                && same(a.fanVoltageV, b.fanVoltageV)
                && same(a.fanCurrentA, b.fanCurrentA)
                && same(a.internalDisplacementM3, b.internalDisplacementM3)
                && a.flowStatus == b.flowStatus
                && same(a.flowM3s, b.flowM3s)
                && same(a.flowUncertaintyM3s, b.flowUncertaintyM3s);
    }

    private static boolean same(double a, double b) {
        if (Double.isNaN(a) && Double.isNaN(b)) return true;
        if (!Double.isFinite(a) || !Double.isFinite(b)) return a == b;
        return Math.abs(a - b) <= 1e-12;
    }

    private static double nullable(Cursor c, int i) {
        return c.isNull(i) ? Double.NaN : c.getDouble(i);
    }

    private static String isoNow() {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        return f.format(new Date());
    }
}
