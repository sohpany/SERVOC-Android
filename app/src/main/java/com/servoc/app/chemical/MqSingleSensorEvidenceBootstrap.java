package com.servoc.app.chemical;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.json.JSONObject;

/** Useful single-channel MQ evidence from peer-reviewed calibration/characterization studies. */
public final class MqSingleSensorEvidenceBootstrap {
    private MqSingleSensorEvidenceBootstrap() {}

    public static final String META_KEY="mq_single_sensor_evidence_hr069_version";
    public static final String SEED_VERSION="2";

    public static final String SRC_MQ4_CH4="HONEYCUTT_SENSORS_2019_MQ4_CH4";
    public static final String SRC_MQ8_MULTI="SUNNY_SENSORS_2021_MQ8_CURVES";
    public static final String SRC_MQ7_CO="ALVEAR_SENSORS_2022_MQ7_CO";
    public static final String SRC_MQ5_CH4="PUTRA_TRANSIENT_2019_MQ5_CH4";
    public static final String SRC_MQ135_BENZENE="HIDAYANTI_IJITEE_2020_MQ135_BENZENE";
    public static final String SRC_MQ3_ETHANOL="TAZI_MSJ_2025_MQ3_ETHANOL";

    public static void ensure(SQLiteDatabase db){
        if(db==null||db.isReadOnly())return;
        if(SEED_VERSION.equals(meta(db,META_KEY)))return;
        db.beginTransaction();
        try{
            seedSources(db);
            db.delete("sensor_evidence","source_id IN (?,?,?,?,?,?)",new String[]{SRC_MQ4_CH4,SRC_MQ8_MULTI,SRC_MQ7_CO,SRC_MQ5_CH4,SRC_MQ135_BENZENE,SRC_MQ3_ETHANOL});
            seedMq4Methane(db);
            seedMq7CarbonMonoxide(db);
            seedMq8PublishedCurves(db);
            seedMq5Methane(db);
            seedMq135Benzene(db);
            seedMq3Ethanol(db);
            putMeta(db,META_KEY,SEED_VERSION);
            db.setTransactionSuccessful();
        }finally{db.endTransaction();}
    }

    private static void seedSources(SQLiteDatabase db){
        source(db,SRC_MQ4_CH4,
                "Precision and Limits of Detection for Selected Commercially Available, Low-Cost Carbon Dioxide and Methane Gas Sensors",
                "Honeycutt et al.","10.3390/s19143157","https://doi.org/10.3390/s19143157","B","2019 / peer reviewed",
                "MQ4 was experimentally exposed to five CH4 concentrations between 500 and 2200 ppm with reference-gas generation/verification. Stable response was reached in 78+/-10 s. The study fitted a nonlinear Langmuirian calibration and reported a corrected MQ4 methane detection limit of about 82 ppm. Temperature/humidity dependence and low-concentration calibration uncertainty are explicitly noted.");
        source(db,SRC_MQ8_MULTI,
                "Low-Cost IoT-Based Sensor System: A Case Study on Harsh Environmental Monitoring",
                "Sunny et al.","10.3390/s21010214","https://doi.org/10.3390/s21010214","B","2021 / peer reviewed",
                "MQ8 sensitivity-graph points were digitized and fitted to quantitative response relations. The paper reports log-form coefficients for H2 and MQ8 cross-responses to LPG, CH4, CO and alcohol, with H2 reference calibration around 1000 ppm. These are source/reference curves, not HR069 M4 calibration.");
        source(db,SRC_MQ7_CO,
                "Smart and Portable Air-Quality Monitoring IoT Low-Cost Devices in Ibarra City, Ecuador",
                "Alvear-Puertas et al.","10.3390/s22187015","https://doi.org/10.3390/s22187015","B","2022 / peer reviewed",
                "MQ7 CO conversion uses the manufacturer sensitivity curve under stated reference conditions (20 C, 65% RH, 21% O2, RL=10 kOhm). The paper reports a direct Rs/R0-to-CO equation and a 20-2000 ppm operating range.");
        source(db,SRC_MQ5_CH4,
                "Sistem Pendeteksi Kadar Gas Methana (CH4) Berbasis IoT Menggunakan NodeMCU ESP8266 dan Sensor Gas MQ-5",
                "Putra et al.","10.14710/transient.v8i2.181-186","https://doi.org/10.14710/transient.v8i2.181-186","B","2019 / peer reviewed",
                "MQ5 methane measurements were compared against gas chromatography. The reported 15-minute mean was 201.4 ppm CH4 with a delta error of 2.14% relative to the GC reference, providing a useful exact-sensor quantitative validation point.");
        source(db,SRC_MQ135_BENZENE,
                "Implementation of Blynk Internet of Things Platform for Benzene Gas Monitoring System on Density Laboratory",
                "Fitria Hidayanti","10.35940/ijitee.D1563.029420","https://doi.org/10.35940/ijitee.D1563.029420","B","2020 / published journal article",
                "MQ135 benzene monitoring was calibrated against standard measurements. Around the calibration test, the reference instrument reported approximately 0.604-0.614 ppm while the MQ135 device reported about 0.63-0.64 ppm; the paper reports average error 4.48%. This is low-concentration benzene evidence, not a complete HR069 curve.");
        source(db,SRC_MQ3_ETHANOL,
                "Development and prototyping of a concentrated ethanol gas monitoring system based on the MQ-3 sensor using the KAVi machine calibrator for environmental safety",
                "Tazi et al.","10.31893/multiscience.2025124","https://doi.org/10.31893/multiscience.2025124","B","2025 / peer reviewed",
                "MQ3 was experimentally calibrated against a KAVi ethanol-gas calibrator over 10%-70% generated ethanol concentration. The study reports good linearity and high measurement accuracy. The source unit is percent concentration, not ppm, so HazRob stores it as a quantitative/identity reference without unit conversion.");
    }

    private static void seedMq4Methane(SQLiteDatabase db){
        Long uid=chemicalUid(db,76);if(uid==null)return;
        JSONObject p=new JSONObject();
        try{
            p.put("analyte","Methane");p.put("sensor","MQ4");p.put("installed_hr069",true);
            p.put("experimental_concentration_min_ppm",500);p.put("experimental_concentration_max_ppm",2200);
            p.put("experimental_level_count",5);p.put("stable_response_time_s_mean",78);p.put("stable_response_time_s_sd",10);
            p.put("fit_model","nonlinear Langmuirian");p.put("power_law_acceptable_above_ppm",500);
            p.put("corrected_limit_of_detection_ppm",82);p.put("temperature_humidity_dependence",true);
            p.put("identity_fingerprint_accepted",true);p.put("ppm_reference_accepted",true);p.put("m4",false);
        }catch(Exception ignored){}
        evidence(db,"MQ4",uid,"PEER_REVIEWED_EXPERIMENTAL_MQ4_CH4_CALIBRATION",null,p.toString(),500.0,2200.0,
                SRC_MQ4_CH4,"B_PEER_REVIEWED_EXACT_SENSOR","ACCEPTED_IDENTITY_AND_PPM_REFERENCE");
    }

    private static void seedMq7CarbonMonoxide(SQLiteDatabase db){
        Long uid=chemicalUid(db,20);if(uid==null)return;
        JSONObject p=new JSONObject();
        String eq="CO_ppm = 10^((log10(17.5) - log10(Rs/R0)) / 0.63)";
        try{
            p.put("analyte","Carbon Monoxide");p.put("sensor","MQ7");p.put("installed_hr069",true);
            p.put("reference_conditions","20 C; 65% RH; 21% O2; RL=10 kOhm");
            p.put("r0_reference","sensor resistance at 100 ppm CO");p.put("published_equation",eq);
            p.put("identity_fingerprint_accepted",true);p.put("ppm_reference_accepted",true);p.put("m4",false);
        }catch(Exception ignored){}
        evidence(db,"MQ7",uid,"PEER_REVIEWED_MQ7_CO_RESPONSE_EQUATION",eq,p.toString(),20.0,2000.0,
                SRC_MQ7_CO,"B_PEER_REVIEWED_EXACT_SENSOR_CURVE","ACCEPTED_IDENTITY_AND_PPM_REFERENCE");
    }

    private static void seedMq8PublishedCurves(SQLiteDatabase db){
        mq8(db,75,"Hydrogen",-4.23,3.09,"primary target; source uses MQ8 graph fit and 1000 ppm H2 reference calibration");
        mq8(db,135,"LPG",-2.53,1.88,"MQ8 cross-response reference");
        mq8(db,76,"Methane",-2.55,2.26,"MQ8 cross-response reference");
        mq8(db,20,"Carbon Monoxide",-7.36,6.48,"MQ8 cross-response reference");
        mq8(db,103,"Ethanol / alcohol mapping",-4.50,4.82,"source labels alcohol; mapped to HazRob ethanol comparison entry");
    }

    private static void mq8(SQLiteDatabase db,int legacy,String analyte,double logm,double n,String note){
        Long uid=chemicalUid(db,legacy);if(uid==null)return;
        JSONObject p=new JSONObject();
        try{
            p.put("analyte",analyte);p.put("sensor","MQ8");p.put("installed_hr069",true);
            p.put("source_model","log10(ppm) = logm + n*log10(source resistance ratio)");
            p.put("logm",logm);p.put("n",n);p.put("note",note);
            p.put("identity_fingerprint_accepted",true);p.put("ppm_reference_accepted",true);p.put("runtime_conversion_requires_ratio_semantics_verification",true);p.put("m4",false);
        }catch(Exception ignored){}
        evidence(db,"MQ8",uid,"PEER_REVIEWED_MQ8_DIGITIZED_RESPONSE_RELATION",null,p.toString(),null,null,
                SRC_MQ8_MULTI,"B_PEER_REVIEWED_EXACT_SENSOR_CURVE","ACCEPTED_IDENTITY_AND_PPM_REFERENCE");
    }

    private static void seedMq5Methane(SQLiteDatabase db){
        Long uid=chemicalUid(db,76);if(uid==null)return;
        JSONObject p=new JSONObject();
        try{
            p.put("analyte","Methane");p.put("sensor","MQ5");p.put("installed_hr069",true);
            p.put("reported_mean_ppm",201.4);p.put("reference_method","gas chromatography");p.put("reported_delta_error_percent",2.14);
            p.put("identity_fingerprint_accepted",true);p.put("ppm_reference_accepted",true);p.put("single_point_validation",true);p.put("m4",false);
        }catch(Exception ignored){}
        evidence(db,"MQ5",uid,"PEER_REVIEWED_MQ5_CH4_GC_VALIDATION_POINT",null,p.toString(),null,null,
                SRC_MQ5_CH4,"B_PEER_REVIEWED_EXACT_SENSOR_REFERENCE","ACCEPTED_IDENTITY_AND_PPM_REFERENCE");
    }

    private static void seedMq135Benzene(SQLiteDatabase db){
        Long uid=chemicalUid(db,34);if(uid==null)return;
        JSONObject p=new JSONObject();
        try{
            p.put("analyte","Benzene");p.put("sensor","MQ135");p.put("installed_hr069",true);
            p.put("reference_ppm_min",0.604);p.put("reference_ppm_max",0.614);p.put("device_ppm_min",0.63);p.put("device_ppm_max",0.64);
            p.put("reported_average_error_percent",4.48);p.put("field_mean_ppm",0.42);
            p.put("identity_fingerprint_accepted",true);p.put("ppm_reference_accepted",true);p.put("complete_curve_available",false);p.put("m4",false);
        }catch(Exception ignored){}
        evidence(db,"MQ135",uid,"PUBLISHED_MQ135_BENZENE_STANDARD_COMPARISON",null,p.toString(),0.604,0.614,
                SRC_MQ135_BENZENE,"B_PUBLISHED_EXACT_SENSOR_STANDARD_COMPARISON","ACCEPTED_IDENTITY_AND_LOW_PPM_REFERENCE");
    }

    private static void seedMq3Ethanol(SQLiteDatabase db){
        Long uid=chemicalUid(db,103);if(uid==null)return;
        JSONObject p=new JSONObject();
        try{
            p.put("analyte","Ethanol");p.put("sensor","MQ3");p.put("installed_hr069",true);
            p.put("calibrator","KAVi machine");p.put("source_concentration_unit","percent");p.put("source_min_percent",10);p.put("source_max_percent",70);
            p.put("reported_behavior","linear response with high accuracy after calibration");p.put("identity_fingerprint_accepted",true);
            p.put("quantitative_reference_accepted",true);p.put("ppm_conversion_registered",false);p.put("m4",false);
        }catch(Exception ignored){}
        evidence(db,"MQ3",uid,"PEER_REVIEWED_MQ3_ETHANOL_CALIBRATOR_SERIES",null,p.toString(),null,null,
                SRC_MQ3_ETHANOL,"B_PEER_REVIEWED_EXACT_SENSOR_CALIBRATION","ACCEPTED_IDENTITY_AND_NON_PPM_QUANTITATIVE_REFERENCE");
    }

    private static void evidence(SQLiteDatabase db,String sensorKey,Long chemicalUid,String type,String equation,String payload,
                                 Double minPpm,Double maxPpm,String sourceId,String tier,String status){
        ContentValues v=new ContentValues();v.put("sensor_key",sensorKey);v.put("chemical_uid",chemicalUid);v.put("evidence_type",type);
        if(equation!=null)v.put("equation",equation);v.put("coefficients",payload);
        if(minPpm!=null)v.put("min_ppm",minPpm);if(maxPpm!=null)v.put("max_ppm",maxPpm);
        v.put("source_id",sourceId);v.put("evidence_tier",tier);v.put("status",status);db.insertOrThrow("sensor_evidence",null,v);
    }
    private static void source(SQLiteDatabase db,String id,String title,String authority,String doi,String url,String tier,String version,String notes){
        ContentValues v=new ContentValues();v.put("source_id",id);v.put("title",title);v.put("authority",authority);v.put("doi",doi);v.put("url",url);
        v.put("evidence_tier",tier);v.put("version",version);v.put("notes",notes);db.insertWithOnConflict("scientific_sources",null,v,SQLiteDatabase.CONFLICT_REPLACE);
    }
    private static Long chemicalUid(SQLiteDatabase db,int legacyId){try(Cursor c=db.rawQuery("SELECT uid FROM chemicals WHERE legacy_id=? LIMIT 1",new String[]{String.valueOf(legacyId)})){return c.moveToFirst()?c.getLong(0):null;}}
    private static String meta(SQLiteDatabase db,String key){try(Cursor c=db.rawQuery("SELECT v FROM meta WHERE k=?",new String[]{key})){return c.moveToFirst()?c.getString(0):null;}}
    private static void putMeta(SQLiteDatabase db,String key,String value){ContentValues v=new ContentValues();v.put("k",key);v.put("v",value);db.insertWithOnConflict("meta",null,v,SQLiteDatabase.CONFLICT_REPLACE);}
}
