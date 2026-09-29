package com.servoc.app.chemical;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.json.JSONArray;
import org.json.JSONObject;

/** Known-concentration MQ calibration series useful to HR069 identity and quantitative reference work. */
public final class MqCalibrationSeriesBootstrap {
    private MqCalibrationSeriesBootstrap() {}

    public static final String META_KEY="mq_calibration_series_hr069_version";
    public static final String SEED_VERSION="1";

    public static final String SRC_GHAZY_2017="GHAZY_MORSI_SHARKAS_2017_MQ2_MQ5_LPG_CO";
    public static final String SRC_KIRAN_2024="KIRAN_JES_2024_MQ5_MULTI_GAS";

    public static void ensure(SQLiteDatabase db){
        if(db==null||db.isReadOnly())return;
        if(SEED_VERSION.equals(meta(db,META_KEY)))return;
        db.beginTransaction();
        try{
            seedSources(db);
            db.delete("sensor_evidence","source_id IN (?,?)",new String[]{SRC_GHAZY_2017,SRC_KIRAN_2024});
            seedGhazySeries(db);
            seedKiranMq5(db);
            putMeta(db,META_KEY,SEED_VERSION);
            db.setTransactionSuccessful();
        }finally{db.endTransaction();}
    }

    private static void seedSources(SQLiteDatabase db){
        source(db,SRC_GHAZY_2017,
                "IoT and Artificial Intelligent Techniques to Improve the Accuracy of LPG and CO Gases",
                "Ahmed Ghazy; Maha Sharkas; Iman Morsi","",
                "https://www.researchgate.net/publication/364380893_IoT_and_Artificial_Intelligent_Techniques_to_Improve_the_Accuracy_of_LPG",
                "B","2017 / conference experiment",
                "Controlled MQ2 and MQ5 LPG/CO calibration at 25 C and 33% RH. LPG: 200/400/700/1000 ppm. CO: 50/100/200/500 ppm. Rs/R0 tables are published for multiple load resistances. ANN/fuzzy compensation was also evaluated for temperature/humidity effects.");
        source(db,SRC_KIRAN_2024,
                "Integration of MQ-5 Gas Sensors with Arduino Board",
                "Rakshitha Kiran et al.","10.52783/jes.5540",
                "https://doi.org/10.52783/jes.5540",
                "B","2024 / Journal of Electrical Systems",
                "MQ5 was exposed in a controlled environment to known methane, LPG and hydrogen concentrations at 200, 400 and 600 ppm. A clean-air baseline and calibration curves were established. Source gives the controlled concentration protocol but not complete tabulated raw values for every gas.");
    }

    private static void seedGhazySeries(SQLiteDatabase db){
        // LPG: exact published Rs/R0 series at 25 C / 33% RH.
        series(db,"MQ2",chemicalUid(db,135),"LPG",
                new double[]{200,400,700,1000},
                new double[]{1.6633,1.2109,0.9371,0.7959},
                new double[]{1.24213,1.0746,0.9560,0.8873},
                0.5,87.475,"standard_ratio vs best measured series",200.0,1000.0);
        series(db,"MQ5",chemicalUid(db,135),"LPG",
                new double[]{200,400,700,1000},
                new double[]{0.68749,0.52356,0.42019,0.36523},
                new double[]{0.6201,0.5120,0.4532,0.4205},
                2.0,91.94,"standard_ratio vs best measured series",200.0,1000.0);

        // CO: best-load measured series and source sensitivity-curve references.
        series(db,"MQ2",chemicalUid(db,20),"Carbon Monoxide",
                new double[]{50,100,200,500},
                new double[]{8.241,6.59,5.27,3.92},
                new double[]{7.71,5.90,4.30,3.15},
                1.5,86.55,"standard_ratio vs best measured series",50.0,500.0);
        series(db,"MQ5",chemicalUid(db,20),"Carbon Monoxide",
                new double[]{50,100,200,500},
                new double[]{4.275,3.92,3.70,3.20},
                new double[]{3.62,3.12,2.98,2.65},
                1.0,86.40,"standard_ratio vs best measured series",50.0,500.0);
    }

    private static void series(SQLiteDatabase db,String sensor,Long uid,String analyte,double[] ppm,double[] standard,double[] measured,
                               double bestRlKohm,double accuracy,String semantics,Double min,Double max){
        if(uid==null)return;
        JSONObject p=new JSONObject();
        try{
            p.put("analyte",analyte);p.put("sensor",sensor);
            p.put("temperature_c",25);p.put("relative_humidity_percent",33);
            p.put("ppm",array(ppm));p.put("source_standard_rs_r0",array(standard));p.put("measured_rs_r0_best_rl",array(measured));
            p.put("best_load_resistance_kohm",bestRlKohm);p.put("reported_accuracy_percent",accuracy);
            p.put("series_semantics",semantics);p.put("identity_fingerprint_accepted",true);p.put("ppm_reference_accepted",true);p.put("m4",false);
        }catch(Exception ignored){}
        evidence(db,sensor,uid,"CONTROLLED_MQ_RS_R0_KNOWN_PPM_SERIES",p.toString(),min,max,SRC_GHAZY_2017,
                "B_CONTROLLED_EXPERIMENT_EXACT_SENSOR","ACCEPTED_IDENTITY_AND_PPM_REFERENCE");
    }

    private static void seedKiranMq5(SQLiteDatabase db){
        knownLevels(db,76,"Methane");
        knownLevels(db,135,"LPG");
        knownLevels(db,75,"Hydrogen");
    }

    private static void knownLevels(SQLiteDatabase db,int legacy,String analyte){
        Long uid=chemicalUid(db,legacy);if(uid==null)return;
        JSONObject p=new JSONObject();
        try{
            p.put("analyte",analyte);p.put("sensor","MQ5");
            p.put("controlled_known_ppm",new JSONArray().put(200).put(400).put(600));
            p.put("clean_air_baseline_reported",true);p.put("calibration_curve_established",true);
            p.put("raw_series_tabulated_in_current_source_extract",false);
            p.put("identity_fingerprint_accepted",true);p.put("ppm_reference_protocol_accepted",true);p.put("m4",false);
        }catch(Exception ignored){}
        evidence(db,"MQ5",uid,"CONTROLLED_MQ5_MULTI_GAS_KNOWN_PPM_LEVELS",p.toString(),200.0,600.0,SRC_KIRAN_2024,
                "B_PEER_REVIEWED_EXACT_SENSOR_KNOWN_CONCENTRATION","ACCEPTED_KNOWN_CONCENTRATION_REFERENCE");
    }

    private static JSONArray array(double[] x){JSONArray a=new JSONArray();for(double v:x)a.put(v);return a;}
    private static void evidence(SQLiteDatabase db,String sensorKey,Long chemicalUid,String type,String payload,Double minPpm,Double maxPpm,String sourceId,String tier,String status){
        ContentValues v=new ContentValues();v.put("sensor_key",sensorKey);v.put("chemical_uid",chemicalUid);v.put("evidence_type",type);v.put("coefficients",payload);
        if(minPpm!=null)v.put("min_ppm",minPpm);if(maxPpm!=null)v.put("max_ppm",maxPpm);v.put("source_id",sourceId);v.put("evidence_tier",tier);v.put("status",status);
        db.insertOrThrow("sensor_evidence",null,v);
    }
    private static void source(SQLiteDatabase db,String id,String title,String authority,String doi,String url,String tier,String version,String notes){
        ContentValues v=new ContentValues();v.put("source_id",id);v.put("title",title);v.put("authority",authority);v.put("doi",doi);v.put("url",url);v.put("evidence_tier",tier);v.put("version",version);v.put("notes",notes);
        db.insertWithOnConflict("scientific_sources",null,v,SQLiteDatabase.CONFLICT_REPLACE);
    }
    private static Long chemicalUid(SQLiteDatabase db,int legacyId){try(Cursor c=db.rawQuery("SELECT uid FROM chemicals WHERE legacy_id=? LIMIT 1",new String[]{String.valueOf(legacyId)})){return c.moveToFirst()?c.getLong(0):null;}}
    private static String meta(SQLiteDatabase db,String key){try(Cursor c=db.rawQuery("SELECT v FROM meta WHERE k=?",new String[]{key})){return c.moveToFirst()?c.getString(0):null;}}
    private static void putMeta(SQLiteDatabase db,String key,String value){ContentValues v=new ContentValues();v.put("k",key);v.put("v",value);db.insertWithOnConflict("meta",null,v,SQLiteDatabase.CONFLICT_REPLACE);}
}
