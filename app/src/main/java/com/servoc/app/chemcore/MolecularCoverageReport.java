package com.servoc.app.chemcore;

import android.database.Cursor;

import com.servoc.app.data.ExperimentsDb;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Database-derived completeness audit for the ChemCore molecular intelligence layer. */
public final class MolecularCoverageReport {
    private MolecularCoverageReport() {}

    public static final String SCHEMA = "chemcore-molecular-coverage/1";

    public static final class PropertyCoverage {
        public final String propertyKey;
        public final int populatedChemicals;
        public final int totalChemicals;
        public final double fraction;

        PropertyCoverage(String propertyKey, int populatedChemicals, int totalChemicals) {
            this.propertyKey = propertyKey;
            this.populatedChemicals = populatedChemicals;
            this.totalChemicals = totalChemicals;
            this.fraction = totalChemicals > 0 ? (double) populatedChemicals / totalChemicals : 0.0;
        }
    }

    public static final class Result {
        public final int totalChemicals;
        public final int structures;
        public final int activeDescriptorSets;
        public final List<PropertyCoverage> properties;

        Result(int totalChemicals, int structures, int activeDescriptorSets,
               List<PropertyCoverage> properties) {
            this.totalChemicals = totalChemicals;
            this.structures = structures;
            this.activeDescriptorSets = activeDescriptorSets;
            this.properties = Collections.unmodifiableList(properties);
        }

        public JSONObject toJson() {
            try {
                JSONObject out = new JSONObject();
                out.put("schema", SCHEMA);
                out.put("total_chemicals", totalChemicals);
                out.put("structures", structures);
                out.put("structure_coverage", totalChemicals > 0 ? (double) structures / totalChemicals : 0.0);
                out.put("active_descriptor_sets", activeDescriptorSets);
                out.put("active_descriptor_coverage", totalChemicals > 0 ? (double) activeDescriptorSets / totalChemicals : 0.0);
                JSONArray items = new JSONArray();
                for (PropertyCoverage p : properties) {
                    JSONObject q = new JSONObject();
                    q.put("property", p.propertyKey);
                    q.put("populated_chemicals", p.populatedChemicals);
                    q.put("total_chemicals", p.totalChemicals);
                    q.put("coverage", p.fraction);
                    items.put(q);
                }
                out.put("properties", items);
                return out;
            } catch (Exception e) {
                throw new IllegalStateException("Unable to serialize molecular coverage report", e);
            }
        }
    }

    public static Result build(ExperimentsDb db) {
        if (db == null) throw new IllegalArgumentException("database required");
        ChemCoreSchema.ensure(db.getWritableDatabase());
        int total = scalar(db, "SELECT COUNT(*) FROM chemicals");
        int structures = scalar(db, "SELECT COUNT(*) FROM chemcore_structures");
        int descriptorSets = scalar(db,
                "SELECT COUNT(DISTINCT chemical_uid) FROM chemcore_descriptor_sets WHERE active=1");
        List<PropertyCoverage> properties = new ArrayList<>();
        for (String key : MolecularDescriptorContract.CORE) {
            properties.add(new PropertyCoverage(key, propertyCount(db, key), total));
        }
        for (String key : MolecularDescriptorContract.OPTIONAL) {
            properties.add(new PropertyCoverage(key, propertyCount(db, key), total));
        }
        return new Result(total, structures, descriptorSets, properties);
    }

    private static int propertyCount(ExperimentsDb db, String key) {
        try (Cursor c = db.getReadableDatabase().rawQuery(
                "SELECT COUNT(DISTINCT chemical_uid) FROM chemcore_properties " +
                        "WHERE property_key=? AND value_numeric IS NOT NULL",
                new String[]{key})) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    private static int scalar(ExperimentsDb db, String sql) {
        try (Cursor c = db.getReadableDatabase().rawQuery(sql, null)) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }
}
