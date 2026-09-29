package com.servoc.app.chemical;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Seeds source-preserving external fingerprint assets that have been accepted for transfer/
 * identification work in HR069. This layer is deliberately separate from local empirical/M4
 * reference artifacts: third-party fingerprints may guide identity and provisional research
 * interpretation, but they never become device/reference calibration merely by being seeded.
 */
public final class AnalyticalTransferBootstrap {
    private AnalyticalTransferBootstrap() {}

    public static final String META_KEY="analytical_transfer_seed_hr069_version";
    public static final String SEED_VERSION="1";
    public static final String RELEASE="0.10.2";
    public static final String HARDWARE_REVISION="HR069";

    public static final String MQDS001_ASSET="research_data/hr069_analytical_library/multimodal_mq_transfer_fingerprints_v1.json";
    public static final String MQDS001_SOURCE_SHA256="13c2c85cfbb23357a71555b9bc2cbe7dd68e931ffc94df30389c1a4631203745";
    public static final String MQDS001_DERIVED_SHA256="6e41294da4fbaed3e390dc2d5daff576dff07d1c67887d1d794012dea602ff8f";

    public static final String UCI_ASSET="research_data/hr069_analytical_library/uci_mq135_lowconcentration_transfer_fingerprints_v1.json";
    public static final String UCI_SOURCE_SHA256="813ebb7d00ed9172114b68d27ab3213170d6c220b8b23907dce91e9838cd48ab";
    public static final String UCI_DERIVED_SHA256="93434b1a3eb84d13d736ca066fb68501374d58d6ca9e845c6111de5cc8e52372";

    private static final int[] UCI_ANALYTES={110,103,115,84,105,95};
    private static final String[] UCI_NAMES={"Acetone","Ethanol","Ethyl Acetate","n-Hexane","Isopropanol","Toluene"};

    public static void ensure(SQLiteDatabase db){
        if(db==null||db.isReadOnly())return;
        if(SEED_VERSION.equals(meta(db,META_KEY)))return;
        db.beginTransaction();
        try{
            seedSource(db);
            clearManagedRows(db);
            seedMultimodalClassAsset(db);
            seedUciMq135Assets(db);
            putMeta(db,META_KEY,SEED_VERSION);
            putMeta(db,"analytical_transfer_seed_hr069_release",RELEASE+"/"+HARDWARE_REVISION);
            putMeta(db,"analytical_transfer_seed_hr069_assets","2");
            putMeta(db,"analytical_transfer_seed_hr069_uci_records","18");
            putMeta(db,"analytical_transfer_seed_hr069_multimodal_samples","6400");
            putMeta(db,"analytical_transfer_seed_hr069_policy","ACCEPTED_TRANSFER_REFERENCE_NEVER_SELF_PROMOTES_TO_M4");
            db.setTransactionSuccessful();
        }finally{db.endTransaction();}
    }

    private static void seedSource(SQLiteDatabase db){
        ContentValues v=new ContentValues();
        v.put("source_id","MQDS-001");
        v.put("title","Gas Sensor Measurement / Gas-Sensors-Measurements-Dataset");
        v.put("authority","TakMashhido / Zenodo mirror; probable derivative of MultimodalGasData");
        v.put("doi","10.5281/zenodo.13283720");
        v.put("url","https://github.com/TakMashhido/Gas-Sensors-Measurements-Dataset");
        v.put("evidence_tier","C");
        v.put("version","1.0.0 @ a4f36dbcdcc650b9f7c7d105846ea1b6d69f76d4");
        v.put("notes","6400 MQ-array samples: NoGas/Smoke/Perfume/Mixture. Six HR069 channels overlap. Treated as non-independent/transfer evidence until provenance equivalence is resolved; never M4.");
        db.insertWithOnConflict("scientific_sources",null,v,SQLiteDatabase.CONFLICT_REPLACE);
    }

    private static void clearManagedRows(SQLiteDatabase db){
        db.delete("sensor_evidence","source_id=? AND evidence_type LIKE ?",new String[]{"MQDS-001","EXTERNAL_TRANSFER_%"});
        db.delete("sensor_evidence","source_id=? AND evidence_type LIKE ?",new String[]{"UCI_GSALC_2023","EXTERNAL_TRANSFER_%"});
    }

    private static void seedMultimodalClassAsset(SQLiteDatabase db){
        JSONObject p=new JSONObject();
        try{
            p.put("asset",MQDS001_ASSET);
            p.put("source_sha256",MQDS001_SOURCE_SHA256);
            p.put("derived_sha256",MQDS001_DERIVED_SHA256);
            p.put("sample_count",6400);p.put("samples_per_class",1600);
            p.put("hr069_overlap_channels",6);
            p.put("missing_hr069_channels",new JSONArray().put("MQ4").put("MQ9"));
            p.put("classes",new JSONArray().put("NoGas").put("Smoke").put("Perfume").put("Mixture"));
            p.put("vector_semantics","source ADC means normalized by cross-sensor geomean and NoGas class mean; not Rs/R0");
            p.put("identity_reference_accepted",true);p.put("direct_hr069_named_identity",false);
            p.put("m4",false);
        }catch(Exception ignored){}
        String array="ARRAY:MQ2,MQ3,MQ5,MQ7,MQ8,MQ135";
        evidence(db,array,null,"EXTERNAL_TRANSFER_CLASS_FINGERPRINT_ASSET",p.toString(),null,null,
                "MQDS-001","C_EXTERNAL_FINGERPRINT","ACCEPTED_TRANSFER_CLASS_FINGERPRINT");

        Long smoke=chemicalUid(db,139);
        if(smoke!=null)evidence(db,array,smoke,"EXTERNAL_TRANSFER_SMOKE_FINGERPRINT_ASSET",p.toString(),null,null,
                "MQDS-001","C_EXTERNAL_FINGERPRINT","ACCEPTED_TRANSFER_CLASS_FINGERPRINT");
    }

    private static void seedUciMq135Assets(SQLiteDatabase db){
        for(int i=0;i<UCI_ANALYTES.length;i++){
            Long uid=chemicalUid(db,UCI_ANALYTES[i]);if(uid==null)continue;
            JSONObject p=new JSONObject();
            try{
                p.put("asset",UCI_ASSET);p.put("source_sha256",UCI_SOURCE_SHA256);p.put("derived_sha256",UCI_DERIVED_SHA256);
                p.put("legacy_id",UCI_ANALYTES[i]);p.put("analyte",UCI_NAMES[i]);
                p.put("concentrations_ppm",new JSONArray().put(0.05).put(0.10).put(0.20));
                p.put("replicates_per_concentration",5);p.put("records_per_analyte",3);
                p.put("features","baseline, early response, exposure mean, endpoint, delta, peak, endpoint slope");
                p.put("vector_semantics","source MQ135 response dynamics normalized to each sample baseline; not HR069 Rs/R0");
                p.put("identity_reference_accepted",true);p.put("direct_hr069_named_identity",false);
                p.put("m4",false);
            }catch(Exception ignored){}
            evidence(db,"MQ135",uid,"EXTERNAL_TRANSFER_KNOWN_CONCENTRATION_FINGERPRINT",p.toString(),0.05,0.20,
                    "UCI_GSALC_2023","B_EXTERNAL_KNOWN_CONCENTRATION","ACCEPTED_TRANSFER_REFERENCE");
        }
    }

    private static void evidence(SQLiteDatabase db,String sensorKey,Long chemicalUid,String type,String payload,
                                 Double minPpm,Double maxPpm,String sourceId,String tier,String status){
        ContentValues v=new ContentValues();v.put("sensor_key",sensorKey);if(chemicalUid!=null)v.put("chemical_uid",chemicalUid);
        v.put("evidence_type",type);v.put("coefficients",payload);
        if(minPpm!=null&&Double.isFinite(minPpm))v.put("min_ppm",minPpm);
        if(maxPpm!=null&&Double.isFinite(maxPpm))v.put("max_ppm",maxPpm);
        v.put("source_id",sourceId);v.put("evidence_tier",tier);v.put("status",status);
        db.insertOrThrow("sensor_evidence",null,v);
    }

    private static Long chemicalUid(SQLiteDatabase db,int legacyId){
        try(Cursor c=db.rawQuery("SELECT uid FROM chemicals WHERE legacy_id=? LIMIT 1",new String[]{String.valueOf(legacyId)})){
            return c.moveToFirst()?c.getLong(0):null;
        }
    }
    private static String meta(SQLiteDatabase db,String key){
        try(Cursor c=db.rawQuery("SELECT v FROM meta WHERE k=?",new String[]{key})){return c.moveToFirst()?c.getString(0):null;}
    }
    private static void putMeta(SQLiteDatabase db,String key,String value){
        ContentValues v=new ContentValues();v.put("k",key);v.put("v",value);db.insertWithOnConflict("meta",null,v,SQLiteDatabase.CONFLICT_REPLACE);
    }
}
