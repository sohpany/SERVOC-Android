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
    public static final String SEED_VERSION="2";

    public static final String SRC_PAREDES="PAREDES_RCQ_2016_LINEAR_ALCOHOLS";
    public static final String SRC_KUMAR_CO="KUMAR_ELECTRONICS_2026_CO";
    public static final String SRC_MARQUES_H2="MARQUES_MQ8_H2_2026";
    public static final String SRC_ALVEAR_CO7="ALVEAR_MQ7_CO_2022";
    public static final String SRC_HONEYCUTT_CH4="HONEYCUTT_MQ4_CH4_2019";

    public static void ensure(SQLiteDatabase db){
        if(db==null||db.isReadOnly())return;
        if(SEED_VERSION.equals(meta(db,META_KEY)))return;
        db.beginTransaction();
        try{
            seedSources(db);
            db.delete("sensor_evidence","source_id IN (?,?,?,?,?)",
                    new String[]{SRC_PAREDES,SRC_KUMAR_CO,SRC_MARQUES_H2,SRC_ALVEAR_CO7,SRC_HONEYCUTT_CH4});
            seedButanol(db);
            seedCarbonMonoxideArray(db);
            seedHydrogenMq8Curve(db);
            seedCarbonMonoxideMq7Curve(db);
            seedMethaneMq4Experiment(db);
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
        source(db,SRC_MARQUES_H2,
                "Quantification of Hydrogen from Electrolysis by Combining a Resistive Electronic Sensor with the Standard Volumetric Method",
                "Marques et al.","10.3390/app16104863",
                "https://doi.org/10.3390/app16104863","B","2026 / peer reviewed",
                "Exact MQ8 H2 study. Published Eq. 16: C(ppm)=1000*(Rs/R0)^-0.68, with source-table points spanning 200-10000 ppm and experimental validation against a volumetric method. Reference curve only until HR069 M4 calibration.");
        source(db,SRC_ALVEAR_CO7,
                "Smart and Portable Air-Quality Monitoring IoT Low-Cost Devices in Ibarra City, Ecuador",
                "Alvear-Puertas et al.","10.3390/s22187015",
                "https://doi.org/10.3390/s22187015","B","2022 / peer reviewed",
                "Exact MQ7 CO conversion relation published for 20-2000 ppm. Equivalent response form Rs/R0=17.5*ppm^-0.63. MQ7 heater-cycle validity is still required on HR069.");
        source(db,SRC_HONEYCUTT_CH4,
                "Precision and Limits of Detection for Selected Commercially Available, Low-Cost Carbon Dioxide and Methane Gas Sensors",
                "Honeycutt et al.","10.3390/s19143157",
                "https://doi.org/10.3390/s19143157","B","2019 / peer reviewed",
                "Controlled MQ4 methane calibration using known gas mixtures and a precision NDIR analyzer. Calibration concentrations were generated from 1.85-995 ppm, additional responses included 500-2200 ppm, settling time was about 78+/-10 s, and the calibration-corrected MQ4 detection limit was 82 ppm.");
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

    private static void seedHydrogenMq8Curve(SQLiteDatabase db){
        Long uid=chemicalUid(db,75);if(uid==null)return;
        JSONObject p=new JSONObject();
        try{
            p.put("analyte","Hydrogen");p.put("sensor","MQ8");
            p.put("ppm_equation","C=1000*(Rs/R0)^-0.68");
            p.put("ratio_equation","Rs/R0=25808.615404180728*ppm^-1.4705882352941175");
            p.put("published_points_ppm",new JSONArray().put(200).put(300).put(500).put(700).put(1000).put(1500).put(2000).put(3000).put(5000).put(8000).put(10000));
            p.put("experimental_validation",true);p.put("identity_fingerprint_accepted",true);
            p.put("reference_ppm_accepted",true);p.put("m4",false);
        }catch(Exception ignored){}
        evidence(db,"MQ8",uid,"PEER_REVIEWED_RS_R0_PPM_CURVE",p.toString(),200.0,10000.0,
                SRC_MARQUES_H2,"B_PEER_REVIEWED_EXACT_MQ_CURVE","ACCEPTED_IDENTITY_AND_REFERENCE_PPM_CURVE");
    }

    private static void seedCarbonMonoxideMq7Curve(SQLiteDatabase db){
        Long uid=chemicalUid(db,20);if(uid==null)return;
        JSONObject p=new JSONObject();
        try{
            p.put("analyte","Carbon Monoxide");p.put("sensor","MQ7");
            p.put("ratio_equation","Rs/R0=17.5*ppm^-0.63");
            p.put("published_domain_ppm",new JSONArray().put(20).put(2000));
            p.put("heater_cycle_required",true);p.put("identity_fingerprint_accepted",true);
            p.put("reference_ppm_accepted",true);p.put("m4",false);
        }catch(Exception ignored){}
        evidence(db,"MQ7",uid,"PEER_REVIEWED_RS_R0_PPM_CURVE",p.toString(),20.0,2000.0,
                SRC_ALVEAR_CO7,"B_PEER_REVIEWED_EXACT_MQ_CURVE","ACCEPTED_IDENTITY_AND_REFERENCE_PPM_CURVE_HEATER_GATED");
    }

    private static void seedMethaneMq4Experiment(SQLiteDatabase db){
        Long uid=chemicalUid(db,76);if(uid==null)return;
        JSONObject p=new JSONObject();
        try{
            p.put("analyte","Methane");p.put("sensor","MQ4");
            p.put("controlled_known_gas",true);p.put("reference_analyzer","California Analytical Instruments ZRE NDIR");
            p.put("calibration_generation_min_ppm",1.85);p.put("calibration_generation_max_ppm",995.0);
            p.put("additional_exposure_range_ppm",new JSONArray().put(500).put(2200));
            p.put("settling_time_s_mean",78);p.put("settling_time_s_sd",10);
            p.put("calibration_corrected_lod_ppm",82.0);
            p.put("curve_model","Langmuir-like nonlinear fit; source coefficients not available in current text");
            p.put("identity_fingerprint_accepted",true);p.put("quantitative_reference",true);
            p.put("numeric_curve_coefficients_available",false);p.put("m4",false);
        }catch(Exception ignored){}
        evidence(db,"MQ4",uid,"PEER_REVIEWED_KNOWN_CONCENTRATION_CALIBRATION_EXPERIMENT",p.toString(),1.85,995.0,
                SRC_HONEYCUTT_CH4,"B_PEER_REVIEWED_EXACT_MQ_KNOWN_CONCENTRATION","ACCEPTED_IDENTITY_AND_QUANTITATIVE_REFERENCE_EXPERIMENT");
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
