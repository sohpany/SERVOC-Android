package com.servoc.app.chemical;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Simple intake for useful MQ-family experimental evidence.
 *
 * Project rule: if a traceable/peer-reviewed experiment uses MQ models installed on HR069 and
 * contributes a chemical fingerprint, response curve, known-concentration range or PPM evidence,
 * preserve it for identification/reference use. Missing channels are never synthesized and M4
 * remains device/reference-calibration only.
 */
public final class MqUsefulEvidenceBootstrap {
    private MqUsefulEvidenceBootstrap() {}

    public static final String META_KEY="mq_useful_evidence_hr069_version";
    public static final String SEED_VERSION="1";

    public static final String SRC_PAREDES="PAREDES_RCQ_2016_LINEAR_ALCOHOLS";
    public static final String SRC_KUMAR_CO="KUMAR_ELECTRONICS_2026_CO";

    public static void ensure(SQLiteDatabase db){
        if(db==null||db.isReadOnly())return;
        if(SEED_VERSION.equals(meta(db,META_KEY)))return;
        db.beginTransaction();
        try{
            seedSources(db);
            db.delete("sensor_evidence","source_id IN (?,?)",new String[]{SRC_PAREDES,SRC_KUMAR_CO});
            seedButanol(db);
            seedCarbonMonoxideArray(db);
            putMeta(db,META_KEY,SEED_VERSION);
            putMeta(db,"mq_useful_evidence_hr069_policy","USE_ANY_TRACEABLE_SAME_MQ_FINGERPRINT_CURVE_OR_PPM_EVIDENCE_NO_SYNTHETIC_CHANNELS_M4_SEPARATE");
            db.setTransactionSuccessful();
        }finally{db.endTransaction();}
    }

    private static void seedSources(SQLiteDatabase db){
        source(db,SRC_PAREDES,
                "Implementation and evaluation of an electronic nose for the alcohols detection",
                "Paredes-Doig et al.","10.15446/rev.colomb.quim.v45n2.60393",
                "https://doi.org/10.15446/rev.colomb.quim.v45n2.60393","B","2016 / peer reviewed",
                "Four-SnO2-sensor e-nose including commercial MQ2 and MQ3. Vapor samples of methanol, ethanol, n-butanol and 1-octanol were experimentally differentiated using PCA. Reported detection order: methanol > ethanol > n-butanol > 1-octanol. Standardization increased explained sample variance from 76% to 85%.");
        source(db,SRC_KUMAR_CO,
                "A Novel E-Nose Architecture Based on Virtual Sensor-Augmented Embedded Intelligence for a Real-Time In-Vehicle Carbon Monoxide Concentration Estimation System",
                "Kumar et al.","10.3390/electronics15081671",
                "https://doi.org/10.3390/electronics15081671","B","2026 / peer reviewed",
                "Controlled CO experiment with MQ2/MQ3/MQ4/MQ5/MQ6/MQ7/MQ8/MQ135. 2484 eight-dimensional samples across six CO concentration classes from fresh air through a reported 1332++ ppm high class. Seven of those sensors are installed on HR069; MQ6 is source-only.");
    }

    private static void seedButanol(SQLiteDatabase db){
        Long uid=chemicalUid(db,106);if(uid==null)return;
        JSONObject p=new JSONObject();
        try{
            p.put("analyte","1-Butanol");p.put("source_name","n-butanol");
            p.put("same_hr069_sensors",new JSONArray().put("MQ2").put("MQ3"));
            p.put("experiment_analytes",new JSONArray().put("methanol").put("ethanol").put("n-butanol").put("1-octanol"));
            p.put("pattern_method","PCA");
            p.put("detection_order","methanol > ethanol > n-butanol > 1-octanol");
            p.put("explained_variance_before_standardization_percent",76);
            p.put("explained_variance_after_standardization_percent",85);
            p.put("identity_fingerprint_accepted",true);
            p.put("numeric_vector_available",false);
            p.put("quantitative_ppm",false);p.put("m4",false);
        }catch(Exception ignored){}
        evidence(db,"ARRAY:MQ2,MQ3",uid,"PEER_REVIEWED_SAME_MQ_IDENTITY_FINGERPRINT",p.toString(),
                null,null,SRC_PAREDES,"B_PEER_REVIEWED_SAME_MQ","ACCEPTED_IDENTITY_FINGERPRINT");
    }

    private static void seedCarbonMonoxideArray(SQLiteDatabase db){
        Long uid=chemicalUid(db,20);if(uid==null)return;
        JSONObject p=new JSONObject();
        try{
            p.put("analyte","Carbon Monoxide");
            p.put("source_array",new JSONArray().put("MQ2").put("MQ3").put("MQ4").put("MQ5").put("MQ6").put("MQ7").put("MQ8").put("MQ135"));
            p.put("hr069_overlap",new JSONArray().put("MQ2").put("MQ3").put("MQ4").put("MQ5").put("MQ7").put("MQ8").put("MQ135"));
            p.put("hr069_overlap_count",7);p.put("source_only_sensor","MQ6");
            p.put("samples",2484);p.put("concentration_classes",6);
            p.put("concentration_span_description","fresh air through reported 1332++ ppm high class");
            p.put("steady_state_multichannel_samples",true);
            p.put("identity_fingerprint_accepted",true);
            p.put("ppm_classification_evidence",true);
            p.put("raw_csv_local",false);
            p.put("quantitative_hr069_ppm",false);p.put("m4",false);
        }catch(Exception ignored){}
        evidence(db,"ARRAY:MQ2,MQ3,MQ4,MQ5,MQ7,MQ8,MQ135",uid,
                "PEER_REVIEWED_SAME_MQ_KNOWN_CONCENTRATION_ARRAY_EVIDENCE",p.toString(),
                null,null,SRC_KUMAR_CO,"B_PEER_REVIEWED_SAME_MQ_KNOWN_CONCENTRATION","ACCEPTED_FINGERPRINT_AND_PPM_CLASS_REFERENCE");
    }

    private static void evidence(SQLiteDatabase db,String sensorKey,Long chemicalUid,String type,String payload,
                                 Double minPpm,Double maxPpm,String sourceId,String tier,String status){
        ContentValues v=new ContentValues();v.put("sensor_key",sensorKey);v.put("chemical_uid",chemicalUid);
        v.put("evidence_type",type);v.put("coefficients",payload);
        if(minPpm!=null)v.put("min_ppm",minPpm);if(maxPpm!=null)v.put("max_ppm",maxPpm);
        v.put("source_id",sourceId);v.put("evidence_tier",tier);v.put("status",status);
        db.insertOrThrow("sensor_evidence",null,v);
    }

    private static void source(SQLiteDatabase db,String id,String title,String authority,String doi,String url,String tier,String version,String notes){
        ContentValues v=new ContentValues();v.put("source_id",id);v.put("title",title);v.put("authority",authority);
        v.put("doi",doi);v.put("url",url);v.put("evidence_tier",tier);v.put("version",version);v.put("notes",notes);
        db.insertWithOnConflict("scientific_sources",null,v,SQLiteDatabase.CONFLICT_REPLACE);
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
