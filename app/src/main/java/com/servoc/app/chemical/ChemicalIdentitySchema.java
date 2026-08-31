package com.servoc.app.chemical;

import android.database.sqlite.SQLiteDatabase;

/** Additive v6 identity schema: a chemical may have many identifiers and many provenance records. */
public final class ChemicalIdentitySchema {
    private ChemicalIdentitySchema(){}

    public static void ensure(SQLiteDatabase db){
        db.execSQL("CREATE TABLE IF NOT EXISTS chemical_sources(id INTEGER PRIMARY KEY AUTOINCREMENT, chemical_uid INTEGER NOT NULL, source_id TEXT NOT NULL, source_key TEXT NOT NULL, record_url TEXT, dataset_hash TEXT, FOREIGN KEY(chemical_uid) REFERENCES chemicals(uid) ON DELETE CASCADE)");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_chemical_source_key ON chemical_sources(source_id,source_key)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_chemical_sources_uid ON chemical_sources(chemical_uid)");
        db.execSQL("CREATE TABLE IF NOT EXISTS chemical_identifiers(id INTEGER PRIMARY KEY AUTOINCREMENT, chemical_uid INTEGER NOT NULL, scheme TEXT NOT NULL, value TEXT NOT NULL, source_id TEXT NOT NULL DEFAULT '', FOREIGN KEY(chemical_uid) REFERENCES chemicals(uid) ON DELETE CASCADE)");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_chemical_identifier_unique ON chemical_identifiers(chemical_uid,scheme,value,source_id)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_chemical_identifier_lookup ON chemical_identifiers(scheme,value)");
    }
}
