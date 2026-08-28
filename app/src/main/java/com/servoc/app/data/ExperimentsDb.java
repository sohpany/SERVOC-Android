package com.servoc.app.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

/** Offline-first HazRob database for raw captures, experiments, fingerprints and trained models. */
public final class ExperimentsDb extends SQLiteOpenHelper {
    public static final int DB_VERSION=3;
    public ExperimentsDb(Context c){super(c,"servoc.db",null,DB_VERSION);}

    @Override public void onCreate(SQLiteDatabase db){
        db.execSQL("CREATE TABLE experiments(id INTEGER PRIMARY KEY AUTOINCREMENT, ts TEXT NOT NULL, payload TEXT NOT NULL, protocol TEXT, analytics TEXT)");
        db.execSQL("CREATE TABLE fingerprints(id INTEGER PRIMARY KEY AUTOINCREMENT, chemical_id INTEGER NOT NULL, label TEXT NOT NULL, created_ts TEXT NOT NULL, payload TEXT NOT NULL)");
        db.execSQL("CREATE TABLE captures(id INTEGER PRIMARY KEY AUTOINCREMENT, chemical_id INTEGER NOT NULL, known_ppm REAL, ts TEXT NOT NULL, session_id TEXT, temp_c REAL, rh REAL, phase TEXT, sensor_payload TEXT NOT NULL, feature_payload TEXT)");
        db.execSQL("CREATE TABLE models(id INTEGER PRIMARY KEY AUTOINCREMENT, chemical_id INTEGER NOT NULL, model_type TEXT NOT NULL, version TEXT NOT NULL, created_ts TEXT NOT NULL, training_count INTEGER, metrics TEXT, payload TEXT NOT NULL, active INTEGER NOT NULL DEFAULT 0)");
        db.execSQL("CREATE TABLE meta(k TEXT PRIMARY KEY, v TEXT NOT NULL)");
    }

    @Override public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion){
        if(oldVersion<2){
            try{db.execSQL("ALTER TABLE experiments ADD COLUMN protocol TEXT");}catch(Exception ignored){}
            try{db.execSQL("ALTER TABLE experiments ADD COLUMN analytics TEXT");}catch(Exception ignored){}
            db.execSQL("CREATE TABLE IF NOT EXISTS fingerprints(id INTEGER PRIMARY KEY AUTOINCREMENT, chemical_id INTEGER NOT NULL, label TEXT NOT NULL, created_ts TEXT NOT NULL, payload TEXT NOT NULL)");
            db.execSQL("CREATE TABLE IF NOT EXISTS meta(k TEXT PRIMARY KEY, v TEXT NOT NULL)");
        }
        if(oldVersion<3){
            db.execSQL("CREATE TABLE IF NOT EXISTS captures(id INTEGER PRIMARY KEY AUTOINCREMENT, chemical_id INTEGER NOT NULL, known_ppm REAL, ts TEXT NOT NULL, session_id TEXT, temp_c REAL, rh REAL, phase TEXT, sensor_payload TEXT NOT NULL, feature_payload TEXT)");
            db.execSQL("CREATE TABLE IF NOT EXISTS models(id INTEGER PRIMARY KEY AUTOINCREMENT, chemical_id INTEGER NOT NULL, model_type TEXT NOT NULL, version TEXT NOT NULL, created_ts TEXT NOT NULL, training_count INTEGER, metrics TEXT, payload TEXT NOT NULL, active INTEGER NOT NULL DEFAULT 0)");
        }
    }

    public long save(String ts,String payload){return save(ts,payload,"","");}
    public long save(String ts,String payload,String protocol,String analytics){
        ContentValues v=new ContentValues();v.put("ts",ts);v.put("payload",payload);v.put("protocol",protocol);v.put("analytics",analytics);
        return getWritableDatabase().insert("experiments",null,v);
    }
    public int count(){try(Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM experiments",null)){return c.moveToFirst()?c.getInt(0):0;}}

    public long saveFingerprint(int chemicalId,String label,String createdTs,String payload){
        ContentValues v=new ContentValues();v.put("chemical_id",chemicalId);v.put("label",label);v.put("created_ts",createdTs);v.put("payload",payload);
        return getWritableDatabase().insert("fingerprints",null,v);
    }
    public int fingerprintCount(){try(Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM fingerprints",null)){return c.moveToFirst()?c.getInt(0):0;}}
    public List<String> fingerprintPayloads(){
        List<String> out=new ArrayList<>();
        try(Cursor c=getReadableDatabase().rawQuery("SELECT payload FROM fingerprints ORDER BY id",null)){while(c.moveToNext())out.add(c.getString(0));}
        return out;
    }

    public long saveCapture(int chemicalId,Double knownPpm,String ts,String sessionId,Double tempC,Double rh,String phase,String sensorPayload,String featurePayload){
        ContentValues v=new ContentValues();v.put("chemical_id",chemicalId);if(knownPpm!=null)v.put("known_ppm",knownPpm);v.put("ts",ts);
        v.put("session_id",sessionId);if(tempC!=null)v.put("temp_c",tempC);if(rh!=null)v.put("rh",rh);v.put("phase",phase);
        v.put("sensor_payload",sensorPayload);v.put("feature_payload",featurePayload);
        return getWritableDatabase().insert("captures",null,v);
    }
    public int captureCount(){try(Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM captures",null)){return c.moveToFirst()?c.getInt(0):0;}}

    public long saveModel(int chemicalId,String modelType,String version,String createdTs,int trainingCount,String metrics,String payload,boolean active){
        if(active)getWritableDatabase().execSQL("UPDATE models SET active=0 WHERE chemical_id=? AND model_type=?",new Object[]{chemicalId,modelType});
        ContentValues v=new ContentValues();v.put("chemical_id",chemicalId);v.put("model_type",modelType);v.put("version",version);v.put("created_ts",createdTs);
        v.put("training_count",trainingCount);v.put("metrics",metrics);v.put("payload",payload);v.put("active",active?1:0);
        return getWritableDatabase().insert("models",null,v);
    }
    public int modelCount(){try(Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM models",null)){return c.moveToFirst()?c.getInt(0):0;}}

    public void putMeta(String key,String value){
        ContentValues v=new ContentValues();v.put("k",key);v.put("v",value==null?"":value);
        getWritableDatabase().insertWithOnConflict("meta",null,v,SQLiteDatabase.CONFLICT_REPLACE);
    }
    public String getMeta(String key,String fallback){
        try(Cursor c=getReadableDatabase().rawQuery("SELECT v FROM meta WHERE k=?",new String[]{key})){return c.moveToFirst()?c.getString(0):fallback;}
    }
}
