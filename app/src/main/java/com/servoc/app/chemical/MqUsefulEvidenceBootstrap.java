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
    public static final String SEED_VERSION="4";

    public static final String SRC_PAREDES="PAREDES_RCQ_2016_LINEAR_ALCOHOLS";
    public static final String SRC_KUMAR_CO="KUMAR_ELECTRONICS_2026_CO";
    public static final String SRC_MARQUES_H2="MARQUES_MQ8_H2_2026";
    public static final String SRC_ALVEAR_CO7="ALVEAR_MQ7_CO_2022";
    public static final String SRC_HONEYCUTT_CH4="HONEYCUTT_MQ4_CH4_2019";
    public static final String SRC_PUTRA_CH4_MQ5="PUTRA_MQ5_CH4_GC_2019";
    public static final String SRC_KHAKIM_LPG="KHAKIM_MQ_LPG_2026";
    public static final String SRC_WINSEN_MQ5_PROPANE="WINSEN_MQ5_PROPANE_CURRENT";
    public static final String SRC_WANG_MTL12="WANG_SNB_2022_MTL12";

    public static void ensure(SQLiteDatabase db){
        if(db==null||db.isReadOnly())return;
        if(SEED_VERSION.equals(meta(db,META_KEY)))return;
        db.beginTransaction();
        try{
            seedSources(db);
            db.delete("sensor_evidence","source_id IN (?,?,?,?,?,?,?,?,?)",
                    new String[]{SRC_PAREDES,SRC_KUMAR_CO,SRC_MARQUES_H2,SRC_ALVEAR_CO7,SRC_HONEYCUTT_CH4,
                            SRC_PUTRA_CH4_MQ5,SRC_KHAKIM_LPG,SRC_WINSEN_MQ5_PROPANE,SRC_WANG_MTL12});
            seedButanol(db);
            seedOctanolIfPresent(db);
            seedCarbonMonoxideArray(db);
            seedHydrogenMq8Curve(db);
            seedCarbonMonoxideMq7Curve(db);
            seedMethaneMq4Experiment(db);
            seedMethaneMq5GcValidation(db);
            seedLpgMultiMqValidation(db);
            seedPropaneMq5ManufacturerAnchor(db);
            seedMtl12VocEvidence(db);
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
        source(db,SRC_PUTRA_CH4_MQ5,
                "Sistem Pendeteksi Kadar Gas Methana (CH4) Berbasis IoT Menggunakan NodeMCU ESP8266 dan Sensor Gas MQ-5",
                "Putra et al.","10.14710/transient.v8i2.181-186",
                "https://doi.org/10.14710/transient.v8i2.181-186","B","2019 / peer reviewed",
                "Exact MQ5 methane validation. Fifteen-minute field measurement averaged 201.4 ppm and was compared with gas chromatography used as the calibration/reference method; reported delta error was 2.14 percent.");
        source(db,SRC_KHAKIM_LPG,
                "An Experimental Evaluation of Low-Cost MQ-Series LPG Sensors under Varying Temperature Conditions",
                "Khakim et al.","10.48084/etasr.15843",
                "https://doi.org/10.48084/etasr.15843","B","2026 / peer reviewed / CC BY 4.0",
                "Controlled LPG evaluation of MQ2/MQ4/MQ5/MQ6 from 15-31 C against a Bosean BTY-S100 reference instrument. HR069 overlaps MQ2/MQ4/MQ5. MQ2 was most stable overall; exact per-temperature ranges and fit metrics are preserved in sensor evidence.");
        source(db,SRC_WINSEN_MQ5_PROPANE,
                "MQ-5 Semiconductor Sensor for Flammable Gas",
                "Zhengzhou Winsen Electronics Technology Co., Ltd.","",
                "https://www.winsen-sensor.com/product/mq-5.html","A","current manufacturer documentation",
                "Exact installed MQ5 model. Target LPG/CH4, with CH4 and C3H8 detection range 300-10000 ppm. Sensitivity specification Rs(air)/Rs(2000 ppm C3H8)>=5 and concentration slope alpha<=0.6 for R3000/R1000 C3H8.");
        source(db,SRC_WANG_MTL12,
                "Target discrimination, concentration prediction, and status judgment of electronic nose system based on large-scale measurement and multi-task deep learning",
                "Tao Wang et al.","10.1016/j.snb.2021.130915",
                "https://doi.org/10.1016/j.snb.2021.130915","B","Sensors and Actuators B 351 (2022) 130915",
                "Large-scale same-MQ experiment with MQ2/MQ3/MQ4/MQ5/MQ6/MQ7/MQ8/MQ9. Twelve VOCs were measured from 10 to 100 ppm using thousands of gas-response processes and more than ten million sensing data points. Reported 4-s response-state task accuracy: 95.2% type recognition, 92.1% concentration prediction and 97.3% status judgment.");
    }

    private static void seedButanol(SQLiteDatabase db){
        Long uid=chemicalUid(db,106);if(uid==null)return;
        JSONObject p=new JSONObject();
        try{
            p.put("analyte","1-Butanol");p.put("source_name","n-butanol");
            p.put("same_hr069_sensors",new JSONArray().put("MQ2").put("MQ3"));
            p.put("experiment_analytes",new JSONArray().put("methanol").put("ethanol").put("n-butanol").put("1-octanol"));
            p.put("pattern_method","PCA");p.put("detection_order","methanol > ethanol > n-butanol > 1-octanol");
            p.put("explained_variance_before_standardization_percent",76);p.put("explained_variance_after_standardization_percent",85);
            p.put("identity_fingerprint_accepted",true);p.put("numeric_vector_available",false);
            p.put("quantitative_ppm",false);p.put("m4",false);
        }catch(Exception ignored){}
        evidence(db,"ARRAY:MQ2,MQ3",uid,"PEER_REVIEWED_SAME_MQ_IDENTITY_FINGERPRINT",p.toString(),null,null,
                SRC_PAREDES,"B_PEER_REVIEWED_SAME_MQ","ACCEPTED_IDENTITY_FINGERPRINT");
    }

    private static void seedOctanolIfPresent(SQLiteDatabase db){
        Long uid=chemicalUidByCas(db,"111-87-5");if(uid==null)return;
        JSONObject p=new JSONObject();
        try{
            p.put("analyte","1-Octanol");p.put("cas","111-87-5");
            p.put("same_hr069_sensors",new JSONArray().put("MQ2").put("MQ3"));
            p.put("pattern_method","PCA");p.put("identity_fingerprint_accepted",true);
            p.put("quantitative_ppm",false);p.put("m4",false);
        }catch(Exception ignored){}
        evidence(db,"ARRAY:MQ2,MQ3",uid,"PEER_REVIEWED_SAME_MQ_IDENTITY_FINGERPRINT",p.toString(),null,null,
                SRC_PAREDES,"B_PEER_REVIEWED_SAME_MQ","ACCEPTED_IDENTITY_FINGERPRINT");
    }

    private static void seedCarbonMonoxideArray(SQLiteDatabase db){
        Long uid=chemicalUid(db,20);if(uid==null)return;
        JSONObject p=new JSONObject();
        try{
            p.put("analyte","Carbon Monoxide");
            p.put("source_array",new JSONArray().put("MQ2").put("MQ3").put("MQ4").put("MQ5").put("MQ6").put("MQ7").put("MQ8").put("MQ135"));
            p.put("hr069_overlap",new JSONArray().put("MQ2").put("MQ3").put("MQ4").put("MQ5").put("MQ7").put("MQ8").put("MQ135"));
            p.put("hr069_overlap_count",7);p.put("source_only_sensor","MQ6");p.put("samples",2484);p.put("concentration_classes",6);
            p.put("concentration_span_description","fresh air through reported 1332++ ppm high class");p.put("steady_state_multichannel_samples",true);
            p.put("identity_fingerprint_accepted",true);p.put("ppm_classification_evidence",true);p.put("raw_csv_local",false);
            p.put("quantitative_hr069_ppm",false);p.put("m4",false);
        }catch(Exception ignored){}
        evidence(db,"ARRAY:MQ2,MQ3,MQ4,MQ5,MQ7,MQ8,MQ135",uid,"PEER_REVIEWED_SAME_MQ_KNOWN_CONCENTRATION_ARRAY_EVIDENCE",p.toString(),null,null,
                SRC_KUMAR_CO,"B_PEER_REVIEWED_SAME_MQ_KNOWN_CONCENTRATION","ACCEPTED_FINGERPRINT_AND_PPM_CLASS_REFERENCE");
    }

    private static void seedHydrogenMq8Curve(SQLiteDatabase db){
        Long uid=chemicalUid(db,75);if(uid==null)return;
        JSONObject p=new JSONObject();
        try{
            p.put("analyte","Hydrogen");p.put("sensor","MQ8");p.put("ppm_equation","C=1000*(Rs/R0)^-0.68");
            p.put("ratio_equation","Rs/R0=25808.615404180728*ppm^-1.4705882352941175");
            p.put("published_points_ppm",new JSONArray().put(200).put(300).put(500).put(700).put(1000).put(1500).put(2000).put(3000).put(5000).put(8000).put(10000));
            p.put("experimental_validation",true);p.put("identity_fingerprint_accepted",true);p.put("reference_ppm_accepted",true);p.put("m4",false);
        }catch(Exception ignored){}
        evidence(db,"MQ8",uid,"PEER_REVIEWED_RS_R0_PPM_CURVE",p.toString(),200.0,10000.0,SRC_MARQUES_H2,
                "B_PEER_REVIEWED_EXACT_MQ_CURVE","ACCEPTED_IDENTITY_AND_REFERENCE_PPM_CURVE");
    }

    private static void seedCarbonMonoxideMq7Curve(SQLiteDatabase db){
        Long uid=chemicalUid(db,20);if(uid==null)return;
        JSONObject p=new JSONObject();
        try{
            p.put("analyte","Carbon Monoxide");p.put("sensor","MQ7");p.put("ratio_equation","Rs/R0=17.5*ppm^-0.63");
            p.put("published_domain_ppm",new JSONArray().put(20).put(2000));p.put("heater_cycle_required",true);
            p.put("identity_fingerprint_accepted",true);p.put("reference_ppm_accepted",true);p.put("m4",false);
        }catch(Exception ignored){}
        evidence(db,"MQ7",uid,"PEER_REVIEWED_RS_R0_PPM_CURVE",p.toString(),20.0,2000.0,SRC_ALVEAR_CO7,
                "B_PEER_REVIEWED_EXACT_MQ_CURVE","ACCEPTED_IDENTITY_AND_REFERENCE_PPM_CURVE_HEATER_GATED");
    }

    private static void seedMethaneMq4Experiment(SQLiteDatabase db){
        Long uid=chemicalUid(db,76);if(uid==null)return;
        JSONObject p=new JSONObject();
        try{
            p.put("analyte","Methane");p.put("sensor","MQ4");p.put("controlled_known_gas",true);
            p.put("reference_analyzer","California Analytical Instruments ZRE NDIR");p.put("calibration_generation_min_ppm",1.85);p.put("calibration_generation_max_ppm",995.0);
            p.put("additional_exposure_range_ppm",new JSONArray().put(500).put(2200));p.put("settling_time_s_mean",78);p.put("settling_time_s_sd",10);
            p.put("calibration_corrected_lod_ppm",82.0);p.put("curve_model","Langmuir-like nonlinear fit; source coefficients not available in current text");
            p.put("identity_fingerprint_accepted",true);p.put("quantitative_reference",true);p.put("numeric_curve_coefficients_available",false);p.put("m4",false);
        }catch(Exception ignored){}
        evidence(db,"MQ4",uid,"PEER_REVIEWED_KNOWN_CONCENTRATION_CALIBRATION_EXPERIMENT",p.toString(),1.85,995.0,SRC_HONEYCUTT_CH4,
                "B_PEER_REVIEWED_EXACT_MQ_KNOWN_CONCENTRATION","ACCEPTED_IDENTITY_AND_QUANTITATIVE_REFERENCE_EXPERIMENT");
    }

    private static void seedMethaneMq5GcValidation(SQLiteDatabase db){
        Long uid=chemicalUid(db,76);if(uid==null)return;
        JSONObject p=new JSONObject();
        try{
            p.put("analyte","Methane");p.put("sensor","MQ5");p.put("measurement_duration_min",15);p.put("mean_sensor_ppm",201.4);
            p.put("reference_method","gas chromatography");p.put("reported_delta_error_percent",2.14);p.put("identity_fingerprint_accepted",true);
            p.put("quantitative_reference",true);p.put("full_curve_available",false);p.put("m4",false);
        }catch(Exception ignored){}
        evidence(db,"MQ5",uid,"PEER_REVIEWED_GC_VALIDATED_PPM_POINT",p.toString(),null,null,SRC_PUTRA_CH4_MQ5,
                "B_PEER_REVIEWED_EXACT_MQ_REFERENCE_COMPARISON","ACCEPTED_QUANTITATIVE_VALIDATION_POINT");
    }

    private static void seedLpgMultiMqValidation(SQLiteDatabase db){
        Long uid=chemicalUid(db,135);if(uid==null)return;
        JSONObject p=new JSONObject();
        try{
            p.put("analyte_class","LPG");p.put("sensors",new JSONArray().put("MQ2").put("MQ4").put("MQ5"));
            p.put("reference_instrument","Bosean BTY-S100");p.put("temperature_c_min",15);p.put("temperature_c_max",31);
            JSONArray schemes=new JSONArray();
            schemes.put(lpgScheme("15-20","MQ2",63,357,7.87,4.15,0.96,4.30));schemes.put(lpgScheme("15-20","MQ4",36,313,34.85,18.36,0.98,16.21));schemes.put(lpgScheme("15-20","MQ5",41,338,9.80,5.16,0.94,3.92));
            schemes.put(lpgScheme("21-25","MQ2",409,703,12.38,2.22,0.96,2.31));schemes.put(lpgScheme("21-25","MQ4",188,465,239.67,43.05,0.98,41.47));schemes.put(lpgScheme("21-25","MQ5",397,694,20.47,3.68,0.94,3.81));
            schemes.put(lpgScheme("26-31","MQ2",5,273,7.09,7.03,0.96,9.09));schemes.put(lpgScheme("26-31","MQ4",10,204,41.20,40.83,0.86,69.83));schemes.put(lpgScheme("26-31","MQ5",45,298,11.25,11.15,0.93,9.96));
            p.put("schemes",schemes);p.put("best_overall_sensor","MQ2");p.put("identity_fingerprint_accepted",true);p.put("quantitative_reference",true);p.put("m4",false);
        }catch(Exception ignored){}
        evidence(db,"ARRAY:MQ2,MQ4,MQ5",uid,"PEER_REVIEWED_MULTI_MQ_LPG_REFERENCE_VALIDATION",p.toString(),5.0,703.0,SRC_KHAKIM_LPG,
                "B_PEER_REVIEWED_MULTI_MQ_REFERENCE_COMPARISON","ACCEPTED_LPG_FINGERPRINT_AND_QUANTITATIVE_REFERENCE");
    }

    private static JSONObject lpgScheme(String tempBand,String sensor,double min,double max,double rmse,double rrmse,double r2,double deviation){
        JSONObject o=new JSONObject();try{o.put("temperature_c",tempBand);o.put("sensor",sensor);o.put("min_ppm",min);o.put("max_ppm",max);o.put("rmse",rmse);o.put("rrmse_percent",rrmse);o.put("r2",r2);o.put("relative_deviation_percent",deviation);}catch(Exception ignored){}return o;
    }

    private static void seedPropaneMq5ManufacturerAnchor(SQLiteDatabase db){
        Long uid=chemicalUid(db,78);if(uid==null)return;
        JSONObject p=new JSONObject();
        try{
            p.put("analyte","Propane");p.put("sensor","MQ5");p.put("formula","C3H8");p.put("detection_min_ppm",300);p.put("detection_max_ppm",10000);
            p.put("sensitivity_reference_ppm",2000);p.put("rs_air_over_rs_2000ppm_min",5.0);p.put("slope_low_ppm",1000);p.put("slope_high_ppm",3000);p.put("r3000_over_r1000_max",0.6);
            p.put("manufacturer_target",true);p.put("identity_fingerprint_accepted",true);p.put("full_numeric_curve_available",false);p.put("m4",false);
        }catch(Exception ignored){}
        evidence(db,"MQ5",uid,"MANUFACTURER_PROPANE_KNOWN_CONCENTRATION_ANCHORS",p.toString(),300.0,10000.0,SRC_WINSEN_MQ5_PROPANE,
                "A_MANUFACTURER_EXACT_MQ_KNOWN_CONCENTRATION","ACCEPTED_PROPANE_IDENTITY_AND_EXPERIMENT_ANCHORS");
    }

    private static void seedMtl12VocEvidence(SQLiteDatabase db){
        Object[][] rows={
                {102,"Methanol"},{103,"Ethanol"},{104,"1-Propanol"},{105,"Isopropanol"},
                {110,"Acetone"},{115,"Ethyl Acetate"},{95,"Toluene"}
        };
        for(Object[] row:rows){Long uid=chemicalUid(db,(Integer)row[0]);if(uid!=null)seedMtl12One(db,uid,(String)row[1],null);}
        Long benzaldehyde=chemicalUidByCas(db,"100-52-7");if(benzaldehyde!=null)seedMtl12One(db,benzaldehyde,"Benzaldehyde","100-52-7");
        Long ethyleneGlycol=chemicalUidByCas(db,"107-21-1");if(ethyleneGlycol!=null)seedMtl12One(db,ethyleneGlycol,"Ethylene Glycol","107-21-1");
    }

    private static void seedMtl12One(SQLiteDatabase db,long uid,String analyte,String cas){
        JSONObject p=new JSONObject();
        try{
            p.put("analyte",analyte);if(cas!=null)p.put("cas",cas);
            p.put("source_array",new JSONArray().put("MQ2").put("MQ3").put("MQ4").put("MQ5").put("MQ6").put("MQ7").put("MQ8").put("MQ9"));
            p.put("hr069_overlap",new JSONArray().put("MQ2").put("MQ3").put("MQ4").put("MQ5").put("MQ7").put("MQ8").put("MQ9"));
            p.put("hr069_overlap_count",7);p.put("source_only_sensor","MQ6");p.put("concentration_min_ppm",10);p.put("concentration_max_ppm",100);
            p.put("measurement_scale","thousands of response processes; >10 million sensing data points across the 12-VOC experiment");
            p.put("type_recognition_accuracy",0.952);p.put("concentration_prediction_accuracy",0.921);p.put("status_judgment_accuracy",0.973);
            p.put("identity_fingerprint_accepted",true);p.put("known_concentration_evidence",true);p.put("raw_numeric_local",false);p.put("m4",false);
        }catch(Exception ignored){}
        evidence(db,"ARRAY:MQ2,MQ3,MQ4,MQ5,MQ7,MQ8,MQ9",uid,"PEER_REVIEWED_SAME_MQ_VOC_FINGERPRINT_AND_CONCENTRATION_DATASET",p.toString(),10.0,100.0,
                SRC_WANG_MTL12,"B_PEER_REVIEWED_SAME_MQ_KNOWN_CONCENTRATION","ACCEPTED_IDENTITY_FINGERPRINT_AND_CONCENTRATION_REFERENCE");
    }

    private static void evidence(SQLiteDatabase db,String sensorKey,Long chemicalUid,String type,String payload,Double minPpm,Double maxPpm,String sourceId,String tier,String status){
        ContentValues v=new ContentValues();v.put("sensor_key",sensorKey);v.put("chemical_uid",chemicalUid);v.put("evidence_type",type);v.put("coefficients",payload);
        if(minPpm!=null)v.put("min_ppm",minPpm);if(maxPpm!=null)v.put("max_ppm",maxPpm);v.put("source_id",sourceId);v.put("evidence_tier",tier);v.put("status",status);db.insertOrThrow("sensor_evidence",null,v);
    }

    private static void source(SQLiteDatabase db,String id,String title,String authority,String doi,String url,String tier,String version,String notes){
        ContentValues v=new ContentValues();v.put("source_id",id);v.put("title",title);v.put("authority",authority);v.put("doi",doi);v.put("url",url);v.put("evidence_tier",tier);v.put("version",version);v.put("notes",notes);db.insertWithOnConflict("scientific_sources",null,v,SQLiteDatabase.CONFLICT_REPLACE);
    }
    private static Long chemicalUid(SQLiteDatabase db,int legacyId){try(Cursor c=db.rawQuery("SELECT uid FROM chemicals WHERE legacy_id=? LIMIT 1",new String[]{String.valueOf(legacyId)})){return c.moveToFirst()?c.getLong(0):null;}}
    private static Long chemicalUidByCas(SQLiteDatabase db,String cas){try(Cursor c=db.rawQuery("SELECT uid FROM chemicals WHERE cas=? LIMIT 1",new String[]{cas})){return c.moveToFirst()?c.getLong(0):null;}}
    private static String meta(SQLiteDatabase db,String key){try(Cursor c=db.rawQuery("SELECT v FROM meta WHERE k=?",new String[]{key})){return c.moveToFirst()?c.getString(0):null;}}
    private static void putMeta(SQLiteDatabase db,String key,String value){ContentValues v=new ContentValues();v.put("k",key);v.put("v",value);db.insertWithOnConflict("meta",null,v,SQLiteDatabase.CONFLICT_REPLACE);}
}
