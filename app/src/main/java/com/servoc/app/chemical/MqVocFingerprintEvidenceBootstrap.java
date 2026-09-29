package com.servoc.app.chemical;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.json.JSONArray;
import org.json.JSONObject;

/** Peer-reviewed VOC fingerprint evidence using the same MQ models installed on HR069. */
public final class MqVocFingerprintEvidenceBootstrap {
    private MqVocFingerprintEvidenceBootstrap() {}

    public static final String META_KEY="mq_voc_fingerprint_evidence_hr069_version";
    public static final String SEED_VERSION="1";
    public static final String SRC_OSHOVSKY_2026="OSHOVSKY_FOODHUM_2026_VISUAL_FP";

    public static void ensure(SQLiteDatabase db){
        if(db==null||db.isReadOnly())return;
        if(SEED_VERSION.equals(meta(db,META_KEY)))return;
        db.beginTransaction();
        try{
            source(db,SRC_OSHOVSKY_2026,
                    "Visual smell fingerprinting with sensor arrays",
                    "Gennady V. Oshovsky et al.","10.1016/j.foohum.2026.101079",
                    "https://doi.org/10.1016/j.foohum.2026.101079","B","2026 / peer reviewed open access",
                    "Nine-MQ array: MQ2/MQ3/MQ4/MQ5/MQ6/MQ7/MQ8/MQ9/MQ135. Pure acetone and ethanol were experimentally differentiated using response magnitude, response rate, and pairwise ratios. Acetone single- and multi-sensor tests include repeated additions reported as 1.0 uL / about 1.5 ppm per addition. All eight HR069 models are present; MQ6 is additional.");
            db.delete("sensor_evidence","source_id=?",new String[]{SRC_OSHOVSKY_2026});
            seedSolvent(db,110,"Acetone",true);
            seedSolvent(db,103,"Ethanol",false);
            putMeta(db,META_KEY,SEED_VERSION);
            db.setTransactionSuccessful();
        }finally{db.endTransaction();}
    }

    private static void seedSolvent(SQLiteDatabase db,int legacy,String analyte,boolean acetone){
        Long uid=chemicalUid(db,legacy);if(uid==null)return;
        JSONObject p=new JSONObject();
        try{
            p.put("analyte",analyte);
            p.put("source_array",new JSONArray().put("MQ2").put("MQ3").put("MQ4").put("MQ5").put("MQ6").put("MQ7").put("MQ8").put("MQ9").put("MQ135"));
            p.put("hr069_overlap",new JSONArray().put("MQ2").put("MQ3").put("MQ4").put("MQ5").put("MQ7").put("MQ8").put("MQ9").put("MQ135"));
            p.put("hr069_overlap_count",8);
            p.put("fingerprint_features",new JSONArray().put("end signal").put("maximum response slope").put("pairwise end-signal ratios").put("pairwise response-rate ratios"));
            p.put("independent_measurements",acetone?6:3);
            p.put("identity_fingerprint_accepted",true);
            if(acetone){
                p.put("single_sensor_example","MQ135");
                p.put("reported_increment_ppm",1.5);
                p.put("reported_increment_volume_ul",1.0);
                p.put("repeated_additions",4);
                p.put("nonlinear_accumulation_warning",true);
            }
            p.put("runtime_numeric_vector_available_in_text",false);
            p.put("m4",false);
        }catch(Exception ignored){}
        evidence(db,"ARRAY:MQ2,MQ3,MQ4,MQ5,MQ7,MQ8,MQ9,MQ135",uid,
                "PEER_REVIEWED_EXACT_ARRAY_SOLVENT_FINGERPRINT",p.toString(),
                acetone?1.5:null,null,SRC_OSHOVSKY_2026,"B_PEER_REVIEWED_EXACT_ARRAY","ACCEPTED_IDENTITY_FINGERPRINT");
    }

    private static void evidence(SQLiteDatabase db,String sensorKey,Long uid,String type,String payload,Double minPpm,Double maxPpm,String sourceId,String tier,String status){
        ContentValues v=new ContentValues();v.put("sensor_key",sensorKey);v.put("chemical_uid",uid);v.put("evidence_type",type);v.put("coefficients",payload);
        if(minPpm!=null)v.put("min_ppm",minPpm);if(maxPpm!=null)v.put("max_ppm",maxPpm);v.put("source_id",sourceId);v.put("evidence_tier",tier);v.put("status",status);db.insertOrThrow("sensor_evidence",null,v);
    }
    private static void source(SQLiteDatabase db,String id,String title,String authority,String doi,String url,String tier,String version,String notes){ContentValues v=new ContentValues();v.put("source_id",id);v.put("title",title);v.put("authority",authority);v.put("doi",doi);v.put("url",url);v.put("evidence_tier",tier);v.put("version",version);v.put("notes",notes);db.insertWithOnConflict("scientific_sources",null,v,SQLiteDatabase.CONFLICT_REPLACE);}
    private static Long chemicalUid(SQLiteDatabase db,int legacyId){try(Cursor c=db.rawQuery("SELECT uid FROM chemicals WHERE legacy_id=? LIMIT 1",new String[]{String.valueOf(legacyId)})){return c.moveToFirst()?c.getLong(0):null;}}
    private static String meta(SQLiteDatabase db,String key){try(Cursor c=db.rawQuery("SELECT v FROM meta WHERE k=?",new String[]{key})){return c.moveToFirst()?c.getString(0):null;}}
    private static void putMeta(SQLiteDatabase db,String key,String value){ContentValues v=new ContentValues();v.put("k",key);v.put("v",value);db.insertWithOnConflict("meta",null,v,SQLiteDatabase.CONFLICT_REPLACE);}
}
