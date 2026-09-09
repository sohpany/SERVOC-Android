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
    private final ExperimentsDb db;

    public SamplingProfileRepository(ExperimentsDb db) {
        if (db == null) throw new IllegalArgumentException("database required");
        this.db = db;
        ChemCoreSchema.ensure(db.getWritableDatabase());
        ensurePrototypeProfile();
    }

    public SamplingProfile ensurePrototypeProfile() {
        SamplingProfile current = byId("hazrob-chemcore-chamber-v1");
        if (current != null) return current;
        SamplingProfile p = SamplingProfile.hazRobPrototypeV1();
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
        v.put("flow_status", p.flowStatus.name());
        if (Double.isFinite(p.flowM3s)) v.put("flow_m3_s", p.flowM3s); else v.putNull("flow_m3_s");
        if (Double.isFinite(p.flowUncertaintyM3s)) v.put("flow_uncertainty_m3_s", p.flowUncertaintyM3s); else v.putNull("flow_uncertainty_m3_s");
        v.put("notes", p.notes);
        v.put("active", active ? 1 : 0);
        long row = db.getWritableDatabase().insertWithOnConflict("chemcore_sampling_profiles", null, v, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE);
        return row >= 0;
    }

    public SamplingProfile active() {
        try (Cursor c = db.getReadableDatabase().rawQuery(
                "SELECT profile_id,version,chamber_width_m,chamber_height_m,chamber_length_m,inlet_width_m,inlet_height_m,fan_width_m,fan_height_m,fan_voltage_v,fan_current_a,flow_status,flow_m3_s,flow_uncertainty_m3_s,notes FROM chemcore_sampling_profiles WHERE active=1 ORDER BY created_ts DESC LIMIT 1", null)) {
            return c.moveToFirst() ? read(c) : null;
        }
    }

    public SamplingProfile byId(String id) {
        if (id == null || id.trim().isEmpty()) return null;
        try (Cursor c = db.getReadableDatabase().rawQuery(
                "SELECT profile_id,version,chamber_width_m,chamber_height_m,chamber_length_m,inlet_width_m,inlet_height_m,fan_width_m,fan_height_m,fan_voltage_v,fan_current_a,flow_status,flow_m3_s,flow_uncertainty_m3_s,notes FROM chemcore_sampling_profiles WHERE profile_id=? LIMIT 1",
                new String[]{id})) {
            return c.moveToFirst() ? read(c) : null;
        }
    }

    private static SamplingProfile read(Cursor c) {
        SamplingProfile.FlowStatus status;
        try { status = SamplingProfile.FlowStatus.valueOf(c.getString(11)); }
        catch (Exception e) { status = SamplingProfile.FlowStatus.FLOW_UNCALIBRATED; }
        return new SamplingProfile(
                c.getString(0), c.getString(1),
                c.getDouble(2), c.getDouble(3), c.getDouble(4),
                c.getDouble(5), c.getDouble(6),
                nullable(c,7), nullable(c,8), nullable(c,9), nullable(c,10),
                status, nullable(c,12), nullable(c,13), c.getString(14));
    }

    private static double nullable(Cursor c, int i) { return c.isNull(i) ? Double.NaN : c.getDouble(i); }

    private static String isoNow() {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        return f.format(new Date());
    }
}
