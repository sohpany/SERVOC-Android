package com.servoc.app.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.servoc.app.chemical.ChemicalCatalog;
import java.util.ArrayList;
import java.util.List;

/** Offline-first HazRob database for measurements, experiments, calibration and scientific reference data. */
public final class ExperimentsDb extends SQLiteOpenHelper {
    public static final int DB_VERSION=9;
    public ExperimentsDb(Context c){super(c,"servoc.db",null,DB_VERSION);}

    @Override public void onCreate(SQLiteDatabase db){
        db.execSQL("CREATE TABLE experiments(id INTEGER PRIMARY KEY AUTOINCREMENT, ts TEXT NOT NULL, payload TEXT NOT NULL, protocol TEXT, analytics TEXT, approved INTEGER NOT NULL DEFAULT 0, approved_ts TEXT, reference_name TEXT, session_id TEXT, capture_id INTEGER, chemical_uid INTEGER, reference_kind TEXT, known_ppm REAL)");
        db.execSQL("CREATE TABLE fingerprints(id INTEGER PRIMARY KEY AUTOINCREMENT, chemical_id INTEGER NOT NULL, label TEXT NOT NULL, created_ts TEXT NOT NULL, payload TEXT NOT NULL)");
        db.execSQL("CREATE TABLE captures(id INTEGER PRIMARY KEY AUTOINCREMENT, chemical_id INTEGER NOT NULL, chemical_uid INTEGER, known_ppm REAL, ts TEXT NOT NULL, session_id TEXT, temp_c REAL, rh REAL, phase TEXT, sensor_payload TEXT NOT NULL, feature_payload TEXT)");
        db.execSQL("CREATE TABLE models(id INTEGER PRIMARY KEY AUTOINCREMENT, chemical_id INTEGER NOT NULL, model_type TEXT NOT NULL, version TEXT NOT NULL, created_ts TEXT NOT NULL, training_count INTEGER, metrics TEXT, payload TEXT NOT NULL, active INTEGER NOT NULL DEFAULT 0)");
        createCalibrationTables(db);db.execSQL("CREATE TABLE meta(k TEXT PRIMARY KEY, v TEXT NOT NULL)");createV6Tables(db);createV8Tables(db);createV9UserFieldTables(db);seedLegacyChemicals(db);seedCoreSources(db);createCaptureUidIndex(db);
    }

    @Override public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion){
        if(oldVersion<2){try{db.execSQL("ALTER TABLE experiments ADD COLUMN protocol TEXT");}catch(Exception ignored){}try{db.execSQL("ALTER TABLE experiments ADD COLUMN analytics TEXT");}catch(Exception ignored){}db.execSQL("CREATE TABLE IF NOT EXISTS fingerprints(id INTEGER PRIMARY KEY AUTOINCREMENT, chemical_id INTEGER NOT NULL, label TEXT NOT NULL, created_ts TEXT NOT NULL, payload TEXT NOT NULL)");db.execSQL("CREATE TABLE IF NOT EXISTS meta(k TEXT PRIMARY KEY, v TEXT NOT NULL)");}
        if(oldVersion<3){db.execSQL("CREATE TABLE IF NOT EXISTS captures(id INTEGER PRIMARY KEY AUTOINCREMENT, chemical_id INTEGER NOT NULL, known_ppm REAL, ts TEXT NOT NULL, session_id TEXT, temp_c REAL, rh REAL, phase TEXT, sensor_payload TEXT NOT NULL, feature_payload TEXT)");db.execSQL("CREATE TABLE IF NOT EXISTS models(id INTEGER PRIMARY KEY AUTOINCREMENT, chemical_id INTEGER NOT NULL, model_type TEXT NOT NULL, version TEXT NOT NULL, created_ts TEXT NOT NULL, training_count INTEGER, metrics TEXT, payload TEXT NOT NULL, active INTEGER NOT NULL DEFAULT 0)");}
        if(oldVersion<4)createCalibrationTables(db);
        if(oldVersion<5){try{db.execSQL("ALTER TABLE experiments ADD COLUMN approved INTEGER NOT NULL DEFAULT 0");}catch(Exception ignored){}try{db.execSQL("ALTER TABLE experiments ADD COLUMN approved_ts TEXT");}catch(Exception ignored){}try{db.execSQL("ALTER TABLE experiments ADD COLUMN reference_name TEXT");}catch(Exception ignored){}}
        if(oldVersion<6){createV6Tables(db);seedLegacyChemicals(db);seedCoreSources(db);}
        if(oldVersion<7){try{db.execSQL("ALTER TABLE captures ADD COLUMN chemical_uid INTEGER");}catch(Exception ignored){}db.execSQL("UPDATE captures SET chemical_uid=(SELECT uid FROM chemicals WHERE legacy_id=captures.chemical_id) WHERE chemical_uid IS NULL AND chemical_id>=0");createCaptureUidIndex(db);}
        if(oldVersion<8){
            try{db.execSQL("ALTER TABLE experiments ADD COLUMN session_id TEXT");}catch(Exception ignored){}
            try{db.execSQL("ALTER TABLE experiments ADD COLUMN capture_id INTEGER");}catch(Exception ignored){}
            try{db.execSQL("ALTER TABLE experiments ADD COLUMN chemical_uid INTEGER");}catch(Exception ignored){}
            try{db.execSQL("ALTER TABLE experiments ADD COLUMN reference_kind TEXT");}catch(Exception ignored){}
            try{db.execSQL("ALTER TABLE experiments ADD COLUMN known_ppm REAL");}catch(Exception ignored){}
            createV8Tables(db);
        }
        if(oldVersion<9)createV9UserFieldTables(db);
    }

    private static void createCaptureUidIndex(SQLiteDatabase db){db.execSQL("CREATE INDEX IF NOT EXISTS idx_captures_chemical_uid ON captures(chemical_uid)");}
    private static void createCalibrationTables(SQLiteDatabase db){
        db.execSQL("CREATE TABLE IF NOT EXISTS calibration_curves(id INTEGER PRIMARY KEY AUTOINCREMENT, sensor_id INTEGER NOT NULL, chemical_id INTEGER NOT NULL, version TEXT NOT NULL, created_ts TEXT NOT NULL, reference_gas TEXT, source_note TEXT, r2 REAL, active INTEGER NOT NULL DEFAULT 0)");
        db.execSQL("CREATE TABLE IF NOT EXISTS calibration_points(id INTEGER PRIMARY KEY AUTOINCREMENT, curve_id INTEGER NOT NULL, seq INTEGER NOT NULL, ppm REAL NOT NULL, ratio REAL NOT NULL, temp_c REAL, rh REAL, FOREIGN KEY(curve_id) REFERENCES calibration_curves(id) ON DELETE CASCADE)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_cal_curve_target ON calibration_curves(sensor_id,chemical_id,active)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_cal_points_curve ON calibration_points(curve_id,seq)");
    }
    private static void createV8Tables(SQLiteDatabase db){
        db.execSQL("CREATE TABLE IF NOT EXISTS reference_artifacts(id INTEGER PRIMARY KEY AUTOINCREMENT, chemical_uid INTEGER NOT NULL, kind TEXT NOT NULL, version TEXT NOT NULL, created_ts TEXT NOT NULL, source_experiment_ids TEXT NOT NULL, metrics TEXT, payload TEXT NOT NULL, sha256 TEXT NOT NULL, active INTEGER NOT NULL DEFAULT 1)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_reference_artifact_target ON reference_artifacts(chemical_uid,kind,active,id)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_experiment_session ON experiments(session_id)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_experiment_reference ON experiments(chemical_uid,reference_kind,approved)");
    }

    /** User field-operation layer. Developer/lab records remain in their existing scientific tables. */
    private static void createV9UserFieldTables(SQLiteDatabase db){
        db.execSQL("CREATE TABLE IF NOT EXISTS sites(site_id TEXT PRIMARY KEY, name TEXT NOT NULL, notes TEXT, created_ts TEXT NOT NULL, updated_ts TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS field_sessions(field_session_id TEXT PRIMARY KEY, site_id TEXT NOT NULL, name TEXT NOT NULL, started_ts TEXT NOT NULL, ended_ts TEXT, status TEXT NOT NULL, created_ts TEXT NOT NULL, updated_ts TEXT NOT NULL, FOREIGN KEY(site_id) REFERENCES sites(site_id))");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_field_sessions_site_time ON field_sessions(site_id,started_ts)");
        db.execSQL("CREATE TABLE IF NOT EXISTS field_session_measurements(field_session_id TEXT NOT NULL, measurement_session_id TEXT NOT NULL UNIQUE, linked_ts TEXT NOT NULL, PRIMARY KEY(field_session_id,measurement_session_id), FOREIGN KEY(field_session_id) REFERENCES field_sessions(field_session_id), FOREIGN KEY(measurement_session_id) REFERENCES measurement_sessions(session_id))");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_field_measurements_field ON field_session_measurements(field_session_id,linked_ts)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_field_measurements_measurement ON field_session_measurements(measurement_session_id)");
    }

    private static void createV6Tables(SQLiteDatabase db){
        db.execSQL("CREATE TABLE IF NOT EXISTS chemicals(uid INTEGER PRIMARY KEY AUTOINCREMENT, legacy_id INTEGER UNIQUE, cas TEXT, pubchem_cid INTEGER, name_en TEXT NOT NULL, name_ar TEXT, formula TEXT, molar_mass REAL, chemical_class TEXT, record_source TEXT, active INTEGER NOT NULL DEFAULT 1)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_chem_cas ON chemicals(cas)");db.execSQL("CREATE INDEX IF NOT EXISTS idx_chem_name ON chemicals(name_en)");
        db.execSQL("CREATE TABLE IF NOT EXISTS chemical_synonyms(id INTEGER PRIMARY KEY AUTOINCREMENT, chemical_uid INTEGER NOT NULL, synonym TEXT NOT NULL, language TEXT, source_id TEXT, FOREIGN KEY(chemical_uid) REFERENCES chemicals(uid) ON DELETE CASCADE)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_synonym_text ON chemical_synonyms(synonym)");
        db.execSQL("CREATE TABLE IF NOT EXISTS scientific_sources(source_id TEXT PRIMARY KEY, title TEXT NOT NULL, authority TEXT, doi TEXT, url TEXT, evidence_tier TEXT NOT NULL, version TEXT, notes TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS exposure_limits(id INTEGER PRIMARY KEY AUTOINCREMENT, chemical_uid INTEGER NOT NULL, authority TEXT NOT NULL, jurisdiction TEXT, limit_type TEXT NOT NULL, value REAL, unit TEXT, duration_min REAL, notation TEXT, source_id TEXT, revision TEXT, FOREIGN KEY(chemical_uid) REFERENCES chemicals(uid), FOREIGN KEY(source_id) REFERENCES scientific_sources(source_id))");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_exposure_target ON exposure_limits(chemical_uid,authority,limit_type)");
        db.execSQL("CREATE TABLE IF NOT EXISTS sensor_evidence(id INTEGER PRIMARY KEY AUTOINCREMENT, sensor_key TEXT NOT NULL, chemical_uid INTEGER, evidence_type TEXT NOT NULL, equation TEXT, coefficients TEXT, min_ppm REAL, max_ppm REAL, min_temp_c REAL, max_temp_c REAL, min_rh REAL, max_rh REAL, heater_protocol TEXT, source_id TEXT, evidence_tier TEXT NOT NULL, status TEXT NOT NULL, FOREIGN KEY(chemical_uid) REFERENCES chemicals(uid), FOREIGN KEY(source_id) REFERENCES scientific_sources(source_id))");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_sensor_evidence ON sensor_evidence(sensor_key,chemical_uid,status)");
        db.execSQL("CREATE TABLE IF NOT EXISTS measurement_sessions(session_id TEXT PRIMARY KEY, started_ts TEXT NOT NULL, ended_ts TEXT, kind TEXT NOT NULL, operator_role TEXT NOT NULL, payload TEXT, analytics TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS telemetry_samples(id INTEGER PRIMARY KEY AUTOINCREMENT, session_id TEXT NOT NULL, t_ms INTEGER NOT NULL, env_payload TEXT, mq_payload TEXT, motion_payload TEXT, FOREIGN KEY(session_id) REFERENCES measurement_sessions(session_id) ON DELETE CASCADE)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_samples_session_time ON telemetry_samples(session_id,t_ms)");
        db.execSQL("CREATE TABLE IF NOT EXISTS media_assets(id INTEGER PRIMARY KEY AUTOINCREMENT, session_id TEXT, media_type TEXT NOT NULL, uri TEXT NOT NULL, started_ts TEXT, ended_ts TEXT, metadata TEXT, FOREIGN KEY(session_id) REFERENCES measurement_sessions(session_id))");
        db.execSQL("CREATE TABLE IF NOT EXISTS events(id INTEGER PRIMARY KEY AUTOINCREMENT, session_id TEXT, t_ms INTEGER NOT NULL, event_type TEXT NOT NULL, confidence REAL, payload TEXT, FOREIGN KEY(session_id) REFERENCES measurement_sessions(session_id))");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_events_session_time ON events(session_id,t_ms)");
        db.execSQL("CREATE TABLE IF NOT EXISTS reference_queue(id INTEGER PRIMARY KEY AUTOINCREMENT, experiment_id INTEGER NOT NULL, status TEXT NOT NULL, created_ts TEXT NOT NULL, payload_hash TEXT, published_ref TEXT, FOREIGN KEY(experiment_id) REFERENCES experiments(id))");
    }

    private static void seedLegacyChemicals(SQLiteDatabase db){try(Cursor c=db.rawQuery("SELECT COUNT(*) FROM chemicals",null)){if(c.moveToFirst()&&c.getInt(0)>0)return;}db.beginTransaction();try{for(int i=0;i<ChemicalCatalog.COUNT;i++){ContentValues v=new ContentValues();v.put("legacy_id",i);v.put("name_en",ChemicalCatalog.NAMES[i]);v.put("formula",ChemicalCatalog.FORMULAS[i]);v.put("record_source","HazRob legacy catalog v0.5");db.insertOrThrow("chemicals",null,v);}db.setTransactionSuccessful();}finally{db.endTransaction();}}
    private static void seedCoreSources(SQLiteDatabase db){
        source(db,"WINSEN_MQ","Winsen MQ-series manufacturer manuals","Zhengzhou Winsen Electronics Technology Co., Ltd.",null,"https://www.winsen-sensor.com/","A","manufacturer","Primary sensor specifications and sensitivity curves; individual manual revision is recorded by imported evidence rows.");
        source(db,"NIOSH_NPG","NIOSH Pocket Guide to Chemical Hazards","NIOSH/CDC",null,"https://www.cdc.gov/niosh/npg/","A","current online edition","Occupational-hygiene source for REL, IDLH and chemical hazard metadata.");
        source(db,"OSHA_PEL","29 CFR 1910.1000 Air Contaminants","OSHA",null,"https://www.osha.gov/laws-regs/regulations/standardnumber/1910/1910.1000","A","current eCFR/OSHA text","Federal permissible exposure limits and substance-specific pointers.");
        source(db,"PUBCHEM","PubChem","NIH/NLM/NCBI",null,"https://pubchem.ncbi.nlm.nih.gov/","A","live database","Chemical identity, identifiers, formulae and synonyms.");
        source(db,"NIST_WEBBOOK","NIST Chemistry WebBook","NIST",null,"https://webbook.nist.gov/chemistry/","A","SRD 69","Physical and thermochemical reference data where available.");
        source(db,"MOS_ML_REVIEW_2025","Advances in metal oxide semiconductor gas sensor arrays based on machine learning algorithms","Han et al.","10.1039/D4TC05220J","https://doi.org/10.1039/D4TC05220J","B","2025","Peer-reviewed review of MOS arrays and ML-assisted gas recognition.");
        source(db,"MOS_PREPROCESS_2025","Metal oxide semiconductor based electronic nose data pre-processing, review","Vajdi et al.","10.1016/j.engappai.2025.110540","https://doi.org/10.1016/j.engappai.2025.110540","B","2025","Peer-reviewed review of static/dynamic preprocessing and transient features.");
        source(db,"MOS_DRIFT_2026","A Comprehensive Review of Algorithms for Drift Compensation in Metal Oxide Semiconductor Gas Sensor Arrays","Chemosensors",null,"https://www.mdpi.com/2227-9040/14/6/143","B","2026","Peer-reviewed drift-compensation review.");
    }
    private static void source(SQLiteDatabase db,String id,String title,String authority,String doi,String url,String tier,String version,String notes){ContentValues v=new ContentValues();v.put("source_id",id);v.put("title",title);v.put("authority",authority);if(doi!=null)v.put("doi",doi);v.put("url",url);v.put("evidence_tier",tier);v.put("version",version);v.put("notes",notes);db.insertWithOnConflict("scientific_sources",null,v,SQLiteDatabase.CONFLICT_IGNORE);}

    public long save(String ts,String payload){return save(ts,payload,"","");}
    public long save(String ts,String payload,String protocol,String analytics){ContentValues v=new ContentValues();v.put("ts",ts);v.put("payload",payload);v.put("protocol",protocol);v.put("analytics",analytics);return getWritableDatabase().insert("experiments",null,v);}
    public long saveReferenceExperiment(String ts,String payload,String protocol,String analytics,String sessionId,long captureId,long chemicalUid,String kind,Double knownPpm){ContentValues v=new ContentValues();v.put("ts",ts);v.put("payload",payload);v.put("protocol",protocol);v.put("analytics",analytics);v.put("session_id",sessionId);v.put("capture_id",captureId);v.put("chemical_uid",chemicalUid);v.put("reference_kind",kind);if(knownPpm!=null)v.put("known_ppm",knownPpm);return getWritableDatabase().insert("experiments",null,v);}
    public boolean approveExperiment(long id,String approvedTs,String referenceName){ContentValues v=new ContentValues();v.put("approved",1);v.put("approved_ts",approvedTs);v.put("reference_name",referenceName);return getWritableDatabase().update("experiments",v,"id=?",new String[]{String.valueOf(id)})==1;}
    public int count(){try(Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM experiments",null)){return c.moveToFirst()?c.getInt(0):0;}}
    public int approvedCount(){try(Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM experiments WHERE approved=1",null)){return c.moveToFirst()?c.getInt(0):0;}}
    public boolean hasApprovedReference(long chemicalUid,String kind){try(Cursor c=getReadableDatabase().rawQuery("SELECT 1 FROM experiments WHERE chemical_uid=? AND reference_kind=? AND approved=1 LIMIT 1",new String[]{String.valueOf(chemicalUid),kind})){return c.moveToFirst();}}
    public int chemicalCount(){try(Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM chemicals WHERE active=1",null)){return c.moveToFirst()?c.getInt(0):0;}}

    public long saveFingerprint(int chemicalId,String label,String createdTs,String payload){ContentValues v=new ContentValues();v.put("chemical_id",chemicalId);v.put("label",label);v.put("created_ts",createdTs);v.put("payload",payload);return getWritableDatabase().insert("fingerprints",null,v);}
    public int fingerprintCount(){try(Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM fingerprints",null)){return c.moveToFirst()?c.getInt(0):0;}}
    public List<String> fingerprintPayloads(){List<String> out=new ArrayList<>();try(Cursor c=getReadableDatabase().rawQuery("SELECT payload FROM fingerprints ORDER BY id",null)){while(c.moveToNext())out.add(c.getString(0));}return out;}

    public long saveCapture(int chemicalId,Double knownPpm,String ts,String sessionId,Double tempC,Double rh,String phase,String sensorPayload,String featurePayload){return saveCaptureForChemical(null,chemicalId,knownPpm,ts,sessionId,tempC,rh,phase,sensorPayload,featurePayload);}
    public long saveCaptureForChemical(Long chemicalUid,Integer legacyId,Double knownPpm,String ts,String sessionId,Double tempC,Double rh,String phase,String sensorPayload,String featurePayload){ContentValues v=new ContentValues();v.put("chemical_id",legacyId==null?-1:legacyId);if(chemicalUid!=null)v.put("chemical_uid",chemicalUid);if(knownPpm!=null)v.put("known_ppm",knownPpm);v.put("ts",ts);v.put("session_id",sessionId);if(tempC!=null)v.put("temp_c",tempC);if(rh!=null)v.put("rh",rh);v.put("phase",phase);v.put("sensor_payload",sensorPayload);v.put("feature_payload",featurePayload);return getWritableDatabase().insert("captures",null,v);}
    public int captureCount(){try(Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM captures",null)){return c.moveToFirst()?c.getInt(0):0;}}
    public String capturePhase(long id){try(Cursor c=getReadableDatabase().rawQuery("SELECT phase FROM captures WHERE id=?",new String[]{String.valueOf(id)})){return c.moveToFirst()?c.getString(0):"";}}
    public boolean sessionExists(String id){if(id==null||id.trim().isEmpty())return false;try(Cursor c=getReadableDatabase().rawQuery("SELECT 1 FROM measurement_sessions WHERE session_id=? LIMIT 1",new String[]{id})){return c.moveToFirst();}}

    public long saveModel(int chemicalId,String modelType,String version,String createdTs,int trainingCount,String metrics,String payload,boolean active){if(active)getWritableDatabase().execSQL("UPDATE models SET active=0 WHERE chemical_id=? AND model_type=?",new Object[]{chemicalId,modelType});ContentValues v=new ContentValues();v.put("chemical_id",chemicalId);v.put("model_type",modelType);v.put("version",version);v.put("created_ts",createdTs);v.put("training_count",trainingCount);v.put("metrics",metrics);v.put("payload",payload);v.put("active",active?1:0);return getWritableDatabase().insert("models",null,v);}
    public int modelCount(){try(Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM models",null)){return c.moveToFirst()?c.getInt(0):0;}}

    public long saveCalibrationCurve(int sensorId,int chemicalId,String version,String createdTs,String referenceGas,String sourceNote,double r2,double[] ppm,double[] ratio,Double tempC,Double rh,boolean active){if(ppm==null||ratio==null||ppm.length!=ratio.length||ppm.length<2)throw new IllegalArgumentException("paired calibration points required");SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{if(active)db.execSQL("UPDATE calibration_curves SET active=0 WHERE sensor_id=? AND chemical_id=?",new Object[]{sensorId,chemicalId});ContentValues cv=new ContentValues();cv.put("sensor_id",sensorId);cv.put("chemical_id",chemicalId);cv.put("version",version);cv.put("created_ts",createdTs);cv.put("reference_gas",referenceGas);cv.put("source_note",sourceNote);if(Double.isFinite(r2))cv.put("r2",r2);cv.put("active",active?1:0);long curveId=db.insertOrThrow("calibration_curves",null,cv);for(int i=0;i<ppm.length;i++){ContentValues p=new ContentValues();p.put("curve_id",curveId);p.put("seq",i);p.put("ppm",ppm[i]);p.put("ratio",ratio[i]);if(tempC!=null)p.put("temp_c",tempC);if(rh!=null)p.put("rh",rh);db.insertOrThrow("calibration_points",null,p);}db.setTransactionSuccessful();return curveId;}finally{db.endTransaction();}}
    public int calibrationCurveCount(){try(Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM calibration_curves",null)){return c.moveToFirst()?c.getInt(0):0;}}

    public static final class ReferenceArtifactRecord{public final long id,chemicalUid;public final String kind,version,createdTs,sourceIds,metrics,payload,sha256;public ReferenceArtifactRecord(long id,long chemicalUid,String kind,String version,String createdTs,String sourceIds,String metrics,String payload,String sha256){this.id=id;this.chemicalUid=chemicalUid;this.kind=kind;this.version=version;this.createdTs=createdTs;this.sourceIds=sourceIds;this.metrics=metrics;this.payload=payload;this.sha256=sha256;}}
    public long saveReferenceArtifact(long chemicalUid,String kind,String version,String createdTs,String sourceIds,String metrics,String payload,String sha256){SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{db.execSQL("UPDATE reference_artifacts SET active=0 WHERE chemical_uid=? AND kind=?",new Object[]{chemicalUid,kind});ContentValues v=new ContentValues();v.put("chemical_uid",chemicalUid);v.put("kind",kind);v.put("version",version);v.put("created_ts",createdTs);v.put("source_experiment_ids",sourceIds);v.put("metrics",metrics);v.put("payload",payload);v.put("sha256",sha256);v.put("active",1);long id=db.insertOrThrow("reference_artifacts",null,v);db.setTransactionSuccessful();return id;}finally{db.endTransaction();}}
    public ReferenceArtifactRecord latestReferenceArtifact(long chemicalUid,String kind){try(Cursor c=getReadableDatabase().rawQuery("SELECT id,chemical_uid,kind,version,created_ts,source_experiment_ids,metrics,payload,sha256 FROM reference_artifacts WHERE chemical_uid=? AND kind=? AND active=1 ORDER BY id DESC LIMIT 1",new String[]{String.valueOf(chemicalUid),kind})){if(!c.moveToFirst())return null;return new ReferenceArtifactRecord(c.getLong(0),c.getLong(1),c.getString(2),c.getString(3),c.getString(4),c.getString(5),c.getString(6),c.getString(7),c.getString(8));}}
    public int referenceArtifactCount(){try(Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM reference_artifacts",null)){return c.moveToFirst()?c.getInt(0):0;}}

    public void putMeta(String key,String value){ContentValues v=new ContentValues();v.put("k",key);v.put("v",value==null?"":value);getWritableDatabase().insertWithOnConflict("meta",null,v,SQLiteDatabase.CONFLICT_REPLACE);}
    public String getMeta(String key,String fallback){try(Cursor c=getReadableDatabase().rawQuery("SELECT v FROM meta WHERE k=?",new String[]{key})){return c.moveToFirst()?c.getString(0):fallback;}}
}
