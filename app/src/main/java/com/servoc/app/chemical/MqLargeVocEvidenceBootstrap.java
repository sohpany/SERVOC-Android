package com.servoc.app.chemical;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.json.JSONArray;
import org.json.JSONObject;

/** Same-array 12-VOC experiment covering MQ2-MQ9 with 10-100 ppm controlled series. */
public final class MqLargeVocEvidenceBootstrap {
    private MqLargeVocEvidenceBootstrap() {}

    public static final String META_KEY="mq_large_voc_evidence_hr069_version";
    public static final String SEED_VERSION="1";
    public static final String SRC="WANG_SNB_2022_MTL12";

    public static void ensure(SQLiteDatabase db){
        if(db==null||db.isReadOnly())return;
        if(SEED_VERSION.equals(meta(db,META_KEY)))return;
        db.beginTransaction();
        try{
            source(db);
            long benzaldehyde=ensureChemical(db,"100-52-7","Benzaldehyde","C7H6O");
            long ethyleneGlycol=ensureChemical(db,"107-21-1","Ethylene Glycol","C2H6O2");
            db.delete("sensor_evidence","source_id=?",new String[]{SRC});
            seedLegacy(db,102,"Methanol","methanol",true);
            seedLegacy(db,103,"Ethanol","ethanol",true);
            seedLegacy(db,104,"1-Propanol","n-propanol",true);
            seedLegacy(db,105,"Isopropanol","isopropanol",true);
            seedLegacy(db,110,"Acetone","acetone",true);
            seedLegacy(db,115,"Ethyl Acetate","ethyl acetate",true);
            seedLegacy(db,95,"Toluene","methylbenzene",true);
            seedLegacy(db,84,"n-Hexane","hexane",false);
            seedLegacy(db,118,"n-Butyl Acetate","butyl acetate",false);
            // Source reports dimethylbenzene without isomer resolution; retain family support on all catalogue isomers.
            seedLegacy(db,97,"o-Xylene","dimethylbenzene",false);
            seedLegacy(db,98,"m-Xylene","dimethylbenzene",false);
            seedLegacy(db,99,"p-Xylene","dimethylbenzene",false);
            if(benzaldehyde>0)seed(db,benzaldehyde,"Benzaldehyde","benzaldehyde",true);
            if(ethyleneGlycol>0)seed(db,ethyleneGlycol,"Ethylene Glycol","ethylene glycol",true);
            putMeta(db,META_KEY,SEED_VERSION);
            db.setTransactionSuccessful();
        }finally{db.endTransaction();}
    }

    private static void seedLegacy(SQLiteDatabase db,int legacy,String catalog,String sourceLabel,boolean exactLabel){
        Long uid=chemicalUid(db,legacy);if(uid!=null)seed(db,uid,catalog,sourceLabel,exactLabel);
    }

    private static void seed(SQLiteDatabase db,long uid,String catalog,String sourceLabel,boolean exactLabel){
        JSONObject p=new JSONObject();
        try{
            p.put("catalog_name",catalog);p.put("source_label",sourceLabel);p.put("source_label_exact_for_catalog",exactLabel);
            p.put("source_array",new JSONArray().put("MQ2").put("MQ3").put("MQ4").put("MQ5").put("MQ6").put("MQ7").put("MQ8").put("MQ9"));
            p.put("hr069_overlap",new JSONArray().put("MQ2").put("MQ3").put("MQ4").put("MQ5").put("MQ7").put("MQ8").put("MQ9"));
            p.put("hr069_overlap_count",7);p.put("source_only_sensor","MQ6");p.put("missing_hr069_sensor","MQ135");
            p.put("concentration_ppm",new JSONArray().put(10).put(20).put(30).put(40).put(50).put(60).put(70).put(80).put(90).put(100));
            p.put("response_definition","Ra/Rg");p.put("baseline_air_s",300);p.put("exposure_each_level_s",300);p.put("final_recovery_s",600);
            p.put("relative_humidity_percent_nominal",50);p.put("relative_humidity_tolerance_percent",10);
            p.put("temperature_c_nominal",26);p.put("temperature_tolerance_c",2);
            p.put("independent_repeats_more_than",10);p.put("raw_resistance_and_response_saved_to_csv_by_source",true);
            p.put("source_type_accuracy",0.952);p.put("source_concentration_accuracy",0.921);p.put("source_status_accuracy",0.973);
            p.put("identity_fingerprint_accepted",true);p.put("known_concentration_series_accepted",true);
            p.put("raw_numeric_csv_local",false);p.put("m4",false);
        }catch(Exception ignored){}
        String status=exactLabel?"ACCEPTED_SAME_ARRAY_IDENTITY_AND_PPM_SERIES":"ACCEPTED_FAMILY_LABEL_AND_PPM_SERIES";
        evidence(db,"ARRAY:MQ2,MQ3,MQ4,MQ5,MQ7,MQ8,MQ9",uid,"PEER_REVIEWED_7CHANNEL_VOC_10_100PPM_SERIES",p.toString(),10.0,100.0,SRC,"B_PEER_REVIEWED_SAME_ARRAY_KNOWN_CONCENTRATION",status);
    }

    private static void source(SQLiteDatabase db){
        ContentValues v=new ContentValues();v.put("source_id",SRC);
        v.put("title","Target discrimination, concentration prediction, and status judgment of electronic nose system based on large-scale measurement and multi-task deep learning");
        v.put("authority","Tao Wang et al. / Sensors and Actuators B: Chemical");v.put("doi","10.1016/j.snb.2021.130915");v.put("url","https://doi.org/10.1016/j.snb.2021.130915");
        v.put("evidence_tier","B");v.put("version","2022 / peer reviewed");
        v.put("notes","Eight-sensor MQ2-MQ9 E-nose; 12 VOCs; controlled 10-100 ppm in 10 ppm steps; more than ten independent repeats per gas; RH 50+/-10%, 26+/-2 C; raw resistance and response curves recorded by source; >10 million sensing points; type/concentration/status accuracies about 95.2/92.1/97.3% using 4 s response windows.");
        db.insertWithOnConflict("scientific_sources",null,v,SQLiteDatabase.CONFLICT_REPLACE);
    }

    private static long ensureChemical(SQLiteDatabase db,String cas,String name,String formula){
        try(Cursor c=db.rawQuery("SELECT uid FROM chemicals WHERE cas=? LIMIT 1",new String[]{cas})){if(c.moveToFirst())return c.getLong(0);}
        ContentValues v=new ContentValues();v.put("cas",cas);v.put("name_en",name);v.put("formula",formula);v.put("record_source","HazRob MQ literature extension");v.put("active",1);
        return db.insert("chemicals",null,v);
    }
    private static Long chemicalUid(SQLiteDatabase db,int legacy){try(Cursor c=db.rawQuery("SELECT uid FROM chemicals WHERE legacy_id=? LIMIT 1",new String[]{String.valueOf(legacy)})){return c.moveToFirst()?c.getLong(0):null;}}
    private static void evidence(SQLiteDatabase db,String sensorKey,long uid,String type,String payload,Double min,Double max,String sourceId,String tier,String status){ContentValues v=new ContentValues();v.put("sensor_key",sensorKey);v.put("chemical_uid",uid);v.put("evidence_type",type);v.put("coefficients",payload);if(min!=null)v.put("min_ppm",min);if(max!=null)v.put("max_ppm",max);v.put("source_id",sourceId);v.put("evidence_tier",tier);v.put("status",status);db.insertOrThrow("sensor_evidence",null,v);}
    private static String meta(SQLiteDatabase db,String key){try(Cursor c=db.rawQuery("SELECT v FROM meta WHERE k=?",new String[]{key})){return c.moveToFirst()?c.getString(0):null;}}
    private static void putMeta(SQLiteDatabase db,String key,String value){ContentValues v=new ContentValues();v.put("k",key);v.put("v",value);db.insertWithOnConflict("meta",null,v,SQLiteDatabase.CONFLICT_REPLACE);}
}
