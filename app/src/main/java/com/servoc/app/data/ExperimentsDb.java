package com.servoc.app.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

/** Offline-first HazRob database for raw captures, experiments, fingerprints, calibration and trained models. */
public final class ExperimentsDb extends SQLiteOpenHelper {
    public static final int DB_VERSION=4;
    public ExperimentsDb(Context c){super(c,"servoc.db",null,DB_VERSION);}

    @Override public void onCreate(SQLiteDatabase db){
        db.execSQL("CREATE TABLE experiments(id INTEGER PRIMARY KEY AUTOINCREMENT, ts TEXT NOT NULL, payload TEXT NOT NULL, protocol TEXT, analytics TEXT)");
        db.execSQL("CREATE TABLE fingerprints(id INTEGER PRIMARY KEY AUTOINCREMENT, chemical_id INTEGER NOT NULL, label TEXT NOT NULL, created_ts TEXT NOT NULL, payload TEXT NOT NULL)");
        db.execSQL("CREATE TABLE captures(id INTEGER PRIMARY KEY AUTOINCREMENT, chemical_id INTEGER NOT NULL, known_ppm REAL, ts TEXT NOT NULL, session_id TEXT, temp_c REAL, rh REAL, phase TEXT, sensor_payload TEXT NOT NULL, feature_payload TEXT)");
        db.execSQL("CREATE TABLE models(id INTEGER PRIMARY KEY AUTOINCREMENT, chemical_id INTEGER NOT NULL, model_type TEXT NOT NULL, version TEXT NOT NULL, created_ts TEXT NOT NULL, training_count INTEGER, metrics TEXT, payload TEXT NOT NULL, active INTEGER NOT NULL DEFAULT 0)");
        createCalibrationTables(db);
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
        if(oldVersion<4){
            createCalibrationTables(db);
        }
    }

    private static void createCalibrationTables(SQLiteDatabase db){
        db.execSQL("CREATE TABLE IF NOT EXISTS calibration_curves(id INTEGER PRIMARY KEY AUTOINCREMENT, sensor_id INTEGER NOT NULL, chemical_id INTEGER NOT NULL, version TEXT NOT NULL, created_ts TEXT NOT NULL, reference_gas TEXT, source_note TEXT, r2 REAL, active INTEGER NOT NULL DEFAULT 0)");
        db.execSQL("CREATE TABLE IF NOT EXISTS calibration_points(id INTEGER PRIMARY KEY AUTOINCREMENT, curve_id INTEGER NOT NULL, seq INTEGER NOT NULL, ppm REAL NOT NULL, ratio REAL NOT NULL, temp_c REAL, rh REAL, FOREIGN KEY(curve_id) REFERENCES calibration_curves(id) ON DELETE CASCADE)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_cal_curve_target ON calibration_curves(sensor_id,chemical_id,active)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_cal_points_curve ON calibration_points(curve_id,seq)");
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

    /** Saves one calibration curve atomically. Existing active curve for the same sensor/analyte is retained but deactivated. */
    public long saveCalibrationCurve(int sensorId,int chemicalId,String version,String createdTs,
                                     String referenceGas,String sourceNote,double r2,
                                     double[] ppm,double[] ratio,Double tempC,Double rh,
                                     boolean active){
        if(ppm==null||ratio==null||ppm.length!=ratio.length||ppm.length<2)
            throw new IllegalArgumentException("paired calibration points required");
        SQLiteDatabase db=getWritableDatabase();
        db.beginTransaction();
        try{
            if(active)db.execSQL("UPDATE calibration_curves SET active=0 WHERE sensor_id=? AND chemical_id=?",new Object[]{sensorId,chemicalId});
            ContentValues cv=new ContentValues();
            cv.put("sensor_id",sensorId);cv.put("chemical_id",chemicalId);cv.put("version",version);cv.put("created_ts",createdTs);
            cv.put("reference_gas",referenceGas);cv.put("source_note",sourceNote);if(Double.isFinite(r2))cv.put("r2",r2);cv.put("active",active?1:0);
            long curveId=db.insertOrThrow("calibration_curves",null,cv);
            for(int i=0;i<ppm.length;i++){
                ContentValues p=new ContentValues();p.put("curve_id",curveId);p.put("seq",i);p.put("ppm",ppm[i]);p.put("ratio",ratio[i]);
                if(tempC!=null)p.put("temp_c",tempC);if(rh!=null)p.put("rh",rh);
                db.insertOrThrow("calibration_points",null,p);
            }
            db.setTransactionSuccessful();
            return curveId;
        }finally{db.endTransaction();}
    }

    public int calibrationCurveCount(){try(Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM calibration_curves",null)){return c.moveToFirst()?c.getInt(0):0;}}

    public void putMeta(String key,String value){
        ContentValues v=new ContentValues();v.put("k",key);v.put("v",value==null?"":value);
        getWritableDatabase().insertWithOnConflict("meta",null,v,SQLiteDatabase.CONFLICT_REPLACE);
    }
    public String getMeta(String key,String fallback){
        try(Cursor c=getReadableDatabase().rawQuery("SELECT v FROM meta WHERE k=?",new String[]{key})){return c.moveToFirst()?c.getString(0):fallback;}
    }
}
