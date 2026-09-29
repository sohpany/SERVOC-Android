package com.servoc.app.chemical;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.json.JSONArray;
import org.json.JSONObject;

/** Useful single-channel MQ evidence from peer-reviewed calibration/characterization studies. */
public final class MqSingleSensorEvidenceBootstrap {
    private MqSingleSensorEvidenceBootstrap() {}

    public static final String META_KEY="mq_single_sensor_evidence_hr069_version";
    public static final String SEED_VERSION="3";

    public static final String SRC_MQ4_CH4="HONEYCUTT_SENSORS_2019_MQ4_CH4";
    public static final String SRC_MQ8_MULTI="SUNNY_SENSORS_2021_MQ8_CURVES";
    public static final String SRC_MQ7_CO="ALVEAR_SENSORS_2022_MQ7_CO";
    public static final String SRC_MQ5_CH4="PUTRA_TRANSIENT_2019_MQ5_CH4";
    public static final String SRC_MQ135_BENZENE="HIDAYANTI_IJITEE_2020_MQ135_BENZENE";
    public static final String SRC_MQ3_ETHANOL="TAZI_MSJ_2025_MQ3_ETHANOL";
    public static final String SRC_MQ_POWER="ASSAAD_BUILDINGS_2023_MQ_POWER";
    public static final String SRC_MQ135_NH3="IJRIAS_2025_MQ135_NH3";

    public static void ensure(SQLiteDatabase db){
        if(db==null||db.isReadOnly())return;
        if(SEED_VERSION.equals(meta(db,META_KEY)))return;
        db.beginTransaction();
        try{
            seedSources(db);
            db.delete("sensor_evidence","source_id IN (?,?,?,?,?,?,?,?)",new String[]{SRC_MQ4_CH4,SRC_MQ8_MULTI,SRC_MQ7_CO,SRC_MQ5_CH4,SRC_MQ135_BENZENE,SRC_MQ3_ETHANOL,SRC_MQ_POWER,SRC_MQ135_NH3});
            seedMq4Methane(db);
            seedMq7CarbonMonoxide(db);
            seedMq8PublishedCurves(db);
            seedMq5Methane(db);
            seedMq135Benzene(db);
            seedMq3Ethanol(db);
            seedPeerReviewedPowerCurves(db);
            seedMq135Ammonia(db);
            putMeta(db,META_KEY,SEED_VERSION);
            db.setTransactionSuccessful();
        }finally{db.endTransaction();}
    }

    private static void seedSources(SQLiteDatabase db){
        source(db,SRC_MQ4_CH4,"Precision and Limits of Detection for Selected Commercially Available, Low-Cost Carbon Dioxide and Methane Gas Sensors","Honeycutt et al.","10.3390/s19143157","https://doi.org/10.3390/s19143157","B","2019 / peer reviewed","Exact MQ4 methane calibration at five levels from 500-2200 ppm; nonlinear calibration; stable response 78+/-10 s; corrected detection limit about 82 ppm.");
        source(db,SRC_MQ8_MULTI,"Low-Cost IoT-Based Sensor System: A Case Study on Harsh Environmental Monitoring","Sunny et al.","10.3390/s21010214","https://doi.org/10.3390/s21010214","B","2021 / peer reviewed","MQ8 graph points digitized into quantitative H2/LPG/CH4/CO/alcohol response relations; H2 reference calibration around 1000 ppm.");
        source(db,SRC_MQ7_CO,"Smart and Portable Air-Quality Monitoring IoT Low-Cost Devices in Ibarra City, Ecuador","Alvear-Puertas et al.","10.3390/s22187015","https://doi.org/10.3390/s22187015","B","2022 / peer reviewed","Published MQ7 Rs/R0-to-CO equation under stated reference conditions; 20-2000 ppm range.");
        source(db,SRC_MQ5_CH4,"Sistem Pendeteksi Kadar Gas Methana (CH4) Berbasis IoT Menggunakan NodeMCU ESP8266 dan Sensor Gas MQ-5","Putra et al.","10.14710/transient.v8i2.181-186","https://doi.org/10.14710/transient.v8i2.181-186","B","2019 / peer reviewed","MQ5 methane measurement compared with gas chromatography; 201.4 ppm reported mean and 2.14% delta error.");
        source(db,SRC_MQ135_BENZENE,"Implementation of Blynk Internet of Things Platform for Benzene Gas Monitoring System on Density Laboratory","Fitria Hidayanti","10.35940/ijitee.D1563.029420","https://doi.org/10.35940/ijitee.D1563.029420","B","2020 / published journal article","MQ135 benzene measurements compared with a standard around 0.604-0.614 ppm; average error 4.48%.");
        source(db,SRC_MQ3_ETHANOL,"Development and prototyping of a concentrated ethanol gas monitoring system based on the MQ-3 sensor using the KAVi machine calibrator for environmental safety","Tazi et al.","10.31893/multiscience.2025124","https://doi.org/10.31893/multiscience.2025124","B","2025 / peer reviewed","MQ3 calibrated against a KAVi ethanol-gas calibrator over 10%-70%; good linearity; source unit is percent, not ppm.");
        source(db,SRC_MQ_POWER,"An IoT-Enabled Sensing Device to Quantify the Reliability of Shared Economy Systems Using Intelligent Sensor Fusion Building Technologies","Assaad et al.","10.3390/buildings13092182","https://doi.org/10.3390/buildings13092182","B","2023 / peer reviewed","Publishes PPM=a*(Rs/R0)^b calibration constants for MQ2 LPG, MQ4 natural gas, MQ5 H2, MQ7 CO, MQ9 combustible gas, and MQ135 ethanol/smoke/CO2.");
        source(db,SRC_MQ135_NH3,"Development and Validation of an Ammonia (NH3) Gas Concentration Measurement Device for Industrial and Health Application","IJRIAS","","https://rsisinternational.org/journals/ijrias/articles/development-and-validation-of-an-ammonia-nh3-gas-concentration-measurement-device-for-industrial-and-health-application/","B","2025 / published study","Exact MQ135 ammonia tests at 10,20,30,40,50 ppm; average error 2.11%; best reported accuracy in the 20-45 ppm region.");
    }

    private static void seedMq4Methane(SQLiteDatabase db){
        Long uid=chemicalUid(db,76);if(uid==null)return;JSONObject p=new JSONObject();
        try{p.put("analyte","Methane");p.put("sensor","MQ4");p.put("experimental_concentration_min_ppm",500);p.put("experimental_concentration_max_ppm",2200);p.put("experimental_level_count",5);p.put("stable_response_time_s_mean",78);p.put("stable_response_time_s_sd",10);p.put("fit_model","nonlinear Langmuirian");p.put("corrected_limit_of_detection_ppm",82);p.put("identity_fingerprint_accepted",true);p.put("ppm_reference_accepted",true);p.put("m4",false);}catch(Exception ignored){}
        evidence(db,"MQ4",uid,"PEER_REVIEWED_EXPERIMENTAL_MQ4_CH4_CALIBRATION",null,p.toString(),500.0,2200.0,SRC_MQ4_CH4,"B_PEER_REVIEWED_EXACT_SENSOR","ACCEPTED_IDENTITY_AND_PPM_REFERENCE");
    }

    private static void seedMq7CarbonMonoxide(SQLiteDatabase db){
        Long uid=chemicalUid(db,20);if(uid==null)return;JSONObject p=new JSONObject();String eq="CO_ppm = 10^((log10(17.5) - log10(Rs/R0)) / 0.63)";
        try{p.put("analyte","Carbon Monoxide");p.put("sensor","MQ7");p.put("reference_conditions","20 C; 65% RH; 21% O2; RL=10 kOhm");p.put("r0_reference","sensor resistance at 100 ppm CO");p.put("published_equation",eq);p.put("m4",false);}catch(Exception ignored){}
        evidence(db,"MQ7",uid,"PEER_REVIEWED_MQ7_CO_RESPONSE_EQUATION",eq,p.toString(),20.0,2000.0,SRC_MQ7_CO,"B_PEER_REVIEWED_EXACT_SENSOR_CURVE","ACCEPTED_IDENTITY_AND_PPM_REFERENCE");
    }

    private static void seedMq8PublishedCurves(SQLiteDatabase db){
        mq8(db,75,"Hydrogen",-4.23,3.09,"primary target; 1000 ppm H2 reference calibration");
        mq8(db,135,"LPG",-2.53,1.88,"MQ8 cross-response reference");
        mq8(db,76,"Methane",-2.55,2.26,"MQ8 cross-response reference");
        mq8(db,20,"Carbon Monoxide",-7.36,6.48,"MQ8 cross-response reference");
        mq8(db,103,"Ethanol / alcohol mapping",-4.50,4.82,"source labels alcohol; mapped to HazRob ethanol comparison entry");
    }

    private static void mq8(SQLiteDatabase db,int legacy,String analyte,double logm,double n,String note){
        Long uid=chemicalUid(db,legacy);if(uid==null)return;JSONObject p=new JSONObject();
        try{p.put("analyte",analyte);p.put("sensor","MQ8");p.put("source_model","log10(ppm) = logm + n*log10(source resistance ratio)");p.put("logm",logm);p.put("n",n);p.put("note",note);p.put("runtime_conversion_requires_ratio_semantics_verification",true);p.put("m4",false);}catch(Exception ignored){}
        evidence(db,"MQ8",uid,"PEER_REVIEWED_MQ8_DIGITIZED_RESPONSE_RELATION",null,p.toString(),null,null,SRC_MQ8_MULTI,"B_PEER_REVIEWED_EXACT_SENSOR_CURVE","ACCEPTED_IDENTITY_AND_PPM_REFERENCE");
    }

    private static void seedMq5Methane(SQLiteDatabase db){
        Long uid=chemicalUid(db,76);if(uid==null)return;JSONObject p=new JSONObject();
        try{p.put("analyte","Methane");p.put("sensor","MQ5");p.put("reported_mean_ppm",201.4);p.put("reference_method","gas chromatography");p.put("reported_delta_error_percent",2.14);p.put("single_point_validation",true);p.put("m4",false);}catch(Exception ignored){}
        evidence(db,"MQ5",uid,"PEER_REVIEWED_MQ5_CH4_GC_VALIDATION_POINT",null,p.toString(),null,null,SRC_MQ5_CH4,"B_PEER_REVIEWED_EXACT_SENSOR_REFERENCE","ACCEPTED_IDENTITY_AND_PPM_REFERENCE");
    }

    private static void seedMq135Benzene(SQLiteDatabase db){
        Long uid=chemicalUid(db,34);if(uid==null)return;JSONObject p=new JSONObject();
        try{p.put("analyte","Benzene");p.put("sensor","MQ135");p.put("reference_ppm_min",0.604);p.put("reference_ppm_max",0.614);p.put("device_ppm_min",0.63);p.put("device_ppm_max",0.64);p.put("reported_average_error_percent",4.48);p.put("field_mean_ppm",0.42);p.put("complete_curve_available",false);p.put("m4",false);}catch(Exception ignored){}
        evidence(db,"MQ135",uid,"PUBLISHED_MQ135_BENZENE_STANDARD_COMPARISON",null,p.toString(),0.604,0.614,SRC_MQ135_BENZENE,"B_PUBLISHED_EXACT_SENSOR_STANDARD_COMPARISON","ACCEPTED_IDENTITY_AND_LOW_PPM_REFERENCE");
    }

    private static void seedMq3Ethanol(SQLiteDatabase db){
        Long uid=chemicalUid(db,103);if(uid==null)return;JSONObject p=new JSONObject();
        try{p.put("analyte","Ethanol");p.put("sensor","MQ3");p.put("calibrator","KAVi machine");p.put("source_concentration_unit","percent");p.put("source_min_percent",10);p.put("source_max_percent",70);p.put("reported_behavior","linear response with high accuracy after calibration");p.put("ppm_conversion_registered",false);p.put("m4",false);}catch(Exception ignored){}
        evidence(db,"MQ3",uid,"PEER_REVIEWED_MQ3_ETHANOL_CALIBRATOR_SERIES",null,p.toString(),null,null,SRC_MQ3_ETHANOL,"B_PEER_REVIEWED_EXACT_SENSOR_CALIBRATION","ACCEPTED_IDENTITY_AND_NON_PPM_QUANTITATIVE_REFERENCE");
    }

    private static void seedPeerReviewedPowerCurves(SQLiteDatabase db){
        power(db,"MQ2",chemicalUid(db,135),"LPG",9.83,658.71,-2.168);
        power(db,"MQ4",chemicalUid(db,137),"Natural Gas",4.4,1012.70,-2.786);
        power(db,"MQ5",chemicalUid(db,75),"Hydrogen",6.5,1163.80,-3.874);
        power(db,"MQ7",chemicalUid(db,20),"Carbon Monoxide",27.5,99.04,-1.518);
        power(db,"MQ9",null,"Combustible Gas",9.6,4269.60,-2.648);
        power(db,"MQ135",chemicalUid(db,103),"Ethanol",3.6,77.26,-3.180);
        power(db,"MQ135",chemicalUid(db,139),"Smoke",3.6,44.95,-3.445);
        power(db,"MQ135",chemicalUid(db,23),"Carbon Dioxide",3.6,110.47,-2.862);
    }

    private static void power(SQLiteDatabase db,String sensor,Long uid,String analyte,double airRatio,double a,double b){
        JSONObject p=new JSONObject();String eq="PPM = "+a+" * (Rs/R0)^"+b;
        try{p.put("analyte",analyte);p.put("sensor",sensor);p.put("fresh_air_rs_r0",airRatio);p.put("a",a);p.put("b",b);p.put("published_equation",eq);p.put("domain_requires_source_or_manufacturer_bound",true);p.put("m4",false);}catch(Exception ignored){}
        evidence(db,sensor,uid,"PEER_REVIEWED_MQ_POWER_CALIBRATION_CONSTANTS",eq,p.toString(),null,null,SRC_MQ_POWER,"B_PEER_REVIEWED_CALIBRATION_CONSTANTS","ACCEPTED_CURVE_REFERENCE_DOMAIN_GATED");
    }

    private static void seedMq135Ammonia(SQLiteDatabase db){
        Long uid=chemicalUid(db,21);if(uid==null)return;JSONObject p=new JSONObject();
        try{p.put("analyte","Ammonia");p.put("sensor","MQ135");p.put("known_ppm",new JSONArray().put(10).put(20).put(30).put(40).put(50));p.put("reported_resistance_values",new JSONArray().put(25.88).put(24.24).put(23.57).put(21.75).put(18.65));p.put("reported_average_error_percent",2.11);p.put("best_accuracy_region_ppm","20-45");p.put("m4",false);}catch(Exception ignored){}
        evidence(db,"MQ135",uid,"PUBLISHED_MQ135_NH3_KNOWN_CONCENTRATION_SERIES",null,p.toString(),10.0,50.0,SRC_MQ135_NH3,"B_PUBLISHED_EXACT_SENSOR_SERIES","ACCEPTED_IDENTITY_AND_PPM_REFERENCE");
    }

    private static void evidence(SQLiteDatabase db,String sensorKey,Long chemicalUid,String type,String equation,String payload,Double minPpm,Double maxPpm,String sourceId,String tier,String status){
        ContentValues v=new ContentValues();v.put("sensor_key",sensorKey);if(chemicalUid!=null)v.put("chemical_uid",chemicalUid);v.put("evidence_type",type);if(equation!=null)v.put("equation",equation);v.put("coefficients",payload);if(minPpm!=null)v.put("min_ppm",minPpm);if(maxPpm!=null)v.put("max_ppm",maxPpm);v.put("source_id",sourceId);v.put("evidence_tier",tier);v.put("status",status);db.insertOrThrow("sensor_evidence",null,v);
    }
    private static void source(SQLiteDatabase db,String id,String title,String authority,String doi,String url,String tier,String version,String notes){ContentValues v=new ContentValues();v.put("source_id",id);v.put("title",title);v.put("authority",authority);v.put("doi",doi);v.put("url",url);v.put("evidence_tier",tier);v.put("version",version);v.put("notes",notes);db.insertWithOnConflict("scientific_sources",null,v,SQLiteDatabase.CONFLICT_REPLACE);}
    private static Long chemicalUid(SQLiteDatabase db,int legacyId){try(Cursor c=db.rawQuery("SELECT uid FROM chemicals WHERE legacy_id=? LIMIT 1",new String[]{String.valueOf(legacyId)})){return c.moveToFirst()?c.getLong(0):null;}}
    private static String meta(SQLiteDatabase db,String key){try(Cursor c=db.rawQuery("SELECT v FROM meta WHERE k=?",new String[]{key})){return c.moveToFirst()?c.getString(0):null;}}
    private static void putMeta(SQLiteDatabase db,String key,String value){ContentValues v=new ContentValues();v.put("k",key);v.put("v",value);db.insertWithOnConflict("meta",null,v,SQLiteDatabase.CONFLICT_REPLACE);}
}
