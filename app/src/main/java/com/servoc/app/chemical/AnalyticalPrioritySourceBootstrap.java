package com.servoc.app.chemical;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Adds high-priority external evidence discovered after the initial HR069 analytical seed.
 *
 * These records reduce discovery work for H2S, methanol, ethylene and acetaldehyde, but remain
 * transfer/research evidence. Nothing seeded here is device-specific M4 calibration.
 */
public final class AnalyticalPrioritySourceBootstrap {
    private AnalyticalPrioritySourceBootstrap() {}

    public static final String META_KEY="analytical_priority_sources_hr069_version";
    // v3 locks the source-backed methanol transfer contract and selected HR069 channels.
    public static final String SEED_VERSION="3";

    public static final String SRC_TOZLU="TOZLU_METHANOL_MQ_2024";
    public static final String SRC_KRIVEC="KRIVEC_ETHYLENE_MQ3_2015";
    public static final String SRC_UCI270="UCI_GAS_DRIFT_270";
    public static final String SRC_WINSEN_MQ135_H2S="WINSEN_MQ135_H2S";
    public static final String SRC_WINSEN_MQ136_H2S="WINSEN_MQ136_H2S";
    public static final String METHANOL_ASSET="research_data/hr069_analytical_library/methanol_mq_transfer_evidence_v1.json";

    public static void ensure(SQLiteDatabase db){
        if(db==null||db.isReadOnly())return;
        if(SEED_VERSION.equals(meta(db,META_KEY)))return;
        db.beginTransaction();
        try{
            seedSources(db);
            clearManagedEvidence(db);
            seedH2sEvidence(db);
            seedMethanolEvidence(db);
            seedEthyleneEvidence(db);
            seedUciDriftEvidence(db);
            putMeta(db,META_KEY,SEED_VERSION);
            putMeta(db,"analytical_priority_sources_hr069_release","0.10.2/HR069");
            putMeta(db,"analytical_priority_sources_hr069_policy","TRANSFER_EVIDENCE_ONLY_NEVER_M4");
            putMeta(db,"analytical_priority_sources_hr069_h2s_policy","H2S_PRIORITY_MQ135_QUALITATIVE_PLUS_MQ136_REFERENCE_NO_HR069_PPM");
            putMeta(db,"analytical_priority_sources_hr069_methanol_policy","METHANOL_SAME_ARRAY_TRANSFER_MQ3_MQ4_PRIORITY_NO_HR069_NUMERIC_VECTOR_NO_PPM_NO_M4");
            db.setTransactionSuccessful();
        }finally{db.endTransaction();}
    }

    private static void seedSources(SQLiteDatabase db){
        source(db,SRC_WINSEN_MQ135_H2S,
                "MQ135 Semiconductor Sensor for Air Quality",
                "Zhengzhou Winsen Electronics Technology Co., Ltd.","",
                "https://www.winsen-sensor.com/product/mq135.html","A","manufacturer current documentation",
                "Installed HR069 sensor. Manufacturer lists ammonia, sulfide and benzene-series vapours as target categories. The published 10-1000 ppm range is specified for ammonia/toluene/hydrogen/smoke, not an H2S-specific MQ135 curve. Manufacturer also warns that high-concentration H2S is corrosive and can attenuate sensor sensitivity.");

        source(db,SRC_WINSEN_MQ136_H2S,
                "MQ136 Gas Sensor for Hydrogen Sulfide",
                "Zhengzhou Winsen Electronics Technology Co., Ltd.","",
                "https://www.winsen-sensor.com/product/mq136.html","A","manufacturer current documentation",
                "Specialized H2S reference sensor, not installed on HR069. Manufacturer specifies H2S 1-200 ppm, Rs(air)/Rs(50 ppm H2S)>=3 and alpha=R200ppm/R50ppm H2S<=0.6. Use as H2S experiment/reference-domain evidence only; never transfer its ppm equation to MQ135 or the HR069 array.");

        source(db,SRC_TOZLU,
                "A Fast and Cost-Effective Electronic Nose Model for Methanol Detection Using Ensemble Learning",
                "Bilge Han Tozlu et al.","10.3390/chemosensors12110225",
                "https://doi.org/10.3390/chemosensors12110225","B","2024 / CC BY 4.0",
                "168 recordings; 11-sensor array includes MQ2/MQ3/MQ4/MQ5/MQ6/MQ7/MQ8/MQ9/MQ131/MQ135/MQ137, therefore all eight installed HR069 MQ models are present. Eight methanol/ethanol mixture classes were recorded for 40 s at 10 Hz. Published feature selection retained four features from MQ3, MQ4 and MQ137; only MQ3 and MQ4 of those selected sensors are installed on HR069. Raw time series are not public and remain requested as REQ-005. Mixture-ratio classification is not gas-phase ppm calibration.");

        source(db,SRC_KRIVEC,
                "Quantitative Ethylene Measurements with MOx Chemiresistive Sensors at Different Relative Air Humidities",
                "Matic Krivec et al.","10.3390/s151128088",
                "https://doi.org/10.3390/s151128088","B","2015 / open access",
                "MQ-3 ethylene measurements at 10/25/40/50 ppm and RH 0/33/64%. Raw/tabulated response values and fitted coefficients requested from author as REQ-006; no curve coefficients are invented meanwhile.");

        source(db,SRC_UCI270,
                "Gas Sensor Array Drift Dataset at Different Concentrations",
                "UCI Machine Learning Repository","10.24432/C5MK6M",
                "https://doi.org/10.24432/C5MK6M","B","UCI 270 / CC BY 4.0",
                "13,910 concentration-labelled measurements across 36 months using 16 Figaro TGS channels. Pure gases include ammonia, acetaldehyde, ethylene, ethanol, acetone and toluene. Cross-array transfer/drift evidence only; no HR069 MQ channels overlap.");
    }

    private static void clearManagedEvidence(SQLiteDatabase db){
        db.delete("sensor_evidence","source_id IN (?,?,?,?,?)",
                new String[]{SRC_TOZLU,SRC_KRIVEC,SRC_UCI270,SRC_WINSEN_MQ135_H2S,SRC_WINSEN_MQ136_H2S});
    }

    private static void seedH2sEvidence(SQLiteDatabase db){
        Long h2s=chemicalUid(db,4);if(h2s==null)return;

        JSONObject installed=new JSONObject();
        try{
            installed.put("analyte","Hydrogen Sulfide");installed.put("formula","H2S");installed.put("cas","7783-06-4");
            installed.put("sensor","MQ135");installed.put("installed_hr069",true);
            installed.put("manufacturer_target_category","sulfide");
            installed.put("h2s_specific_rs_r0_curve_registered",false);
            installed.put("h2s_specific_ppm_domain_registered",false);
            installed.put("high_concentration_h2s_corrosion_warning",true);
            installed.put("role","qualitative installed-channel affinity and compatibility guard");
            installed.put("direct_hr069_ppm",false);installed.put("m4",false);
        }catch(Exception ignored){}
        evidence(db,"MQ135",h2s,"MANUFACTURER_H2S_SULFIDE_AFFINITY_DESCRIPTOR",installed.toString(),null,null,
                SRC_WINSEN_MQ135_H2S,"A_MANUFACTURER_QUALITATIVE_INSTALLED","ACCEPTED_H2S_PRIORITY_COMPATIBILITY_GUARD");

        JSONObject reference=new JSONObject();
        try{
            reference.put("analyte","Hydrogen Sulfide");reference.put("formula","H2S");reference.put("cas","7783-06-4");
            reference.put("sensor","MQ136");reference.put("installed_hr069",false);
            reference.put("detection_min_ppm",1.0);reference.put("detection_max_ppm",200.0);
            reference.put("sensitivity_reference_ppm",50.0);reference.put("rs_air_over_rs_50ppm_min",3.0);
            reference.put("slope_reference_low_ppm",50.0);reference.put("slope_reference_high_ppm",200.0);
            reference.put("r200_over_r50_max",0.6);
            reference.put("role","quantitative H2S reference-domain and experiment-planning evidence; noninstalled sensor");
            reference.put("curve_coefficients_registered",false);
            reference.put("transfer_to_mq135_forbidden",true);
            reference.put("direct_hr069_ppm",false);reference.put("m4",false);
        }catch(Exception ignored){}
        evidence(db,"REFERENCE:MQ136",h2s,"MANUFACTURER_H2S_KNOWN_CONCENTRATION_REFERENCE",reference.toString(),1.0,200.0,
                SRC_WINSEN_MQ136_H2S,"A_MANUFACTURER_QUANTITATIVE_REFERENCE_NONINSTALLED","ACCEPTED_REFERENCE_DOMAIN_HR069_TRANSFER_REQUIRED");
    }

    private static void seedMethanolEvidence(SQLiteDatabase db){
        Long methanol=chemicalUid(db,102);if(methanol==null)return;
        JSONObject p=new JSONObject();
        try{
            p.put("asset",METHANOL_ASSET);
            p.put("array",new JSONArray().put("MQ2").put("MQ3").put("MQ4").put("MQ5").put("MQ6").put("MQ7").put("MQ8").put("MQ9").put("MQ131").put("MQ135").put("MQ137"));
            p.put("hr069_overlap",new JSONArray().put("MQ2").put("MQ3").put("MQ4").put("MQ5").put("MQ7").put("MQ8").put("MQ9").put("MQ135"));
            p.put("hr069_overlap_count",8);p.put("recordings",168);p.put("duration_s",40);p.put("sampling_hz",10);
            p.put("reported_raw_matrix_shape",new JSONArray().put(168).put(11).put(401));
            p.put("mixture_classes",new JSONArray().put("0% MeOH / 100% EtOH").put("10/90").put("20/80").put("30/70").put("40/60").put("50/50").put("60/40").put("100% MeOH / 0% EtOH"));
            p.put("selected_sensor_models",new JSONArray().put("MQ3").put("MQ4").put("MQ137"));
            p.put("selected_hr069_sensor_models",new JSONArray().put("MQ3").put("MQ4"));
            p.put("selected_feature_count",4);
            p.put("mq3_energy_usage_percent",16.86);
            p.put("mq4_energy_usage_percent",41.55);
            p.put("mq137_energy_usage_percent",21.87);
            p.put("mq137_snr_usage_percent",19.72);
            p.put("accuracy_all_features",0.8588);
            p.put("accuracy_selected_features",0.8185);
            p.put("gas_phase_ppm_defined",false);
            p.put("raw_data_request","REQ-005");
            p.put("raw_data_public",false);
            p.put("direct_hr069_named_identity",false);
            p.put("direct_gas_ppm",false);p.put("m4",false);
        }catch(Exception ignored){}
        evidence(db,"ARRAY:MQ2,MQ3,MQ4,MQ5,MQ7,MQ8,MQ9,MQ135",methanol,
                "EXTERNAL_SAME_ARRAY_METHANOL_FINGERPRINT_DESCRIPTOR",p.toString(),null,null,
                SRC_TOZLU,"B_PEER_REVIEWED_SAME_ARRAY","ACCEPTED_SOURCE_DESCRIPTOR_AWAITING_RAW");
    }

    private static void seedEthyleneEvidence(SQLiteDatabase db){
        Long ethylene=chemicalUid(db,89);if(ethylene==null)return;
        JSONObject p=new JSONObject();
        try{
            p.put("sensor","MQ3");
            p.put("concentrations_ppm",new JSONArray().put(10).put(25).put(40).put(50));
            p.put("relative_humidity_percent",new JSONArray().put(0).put(33).put(64));
            p.put("flow_ml_min",500);p.put("exposure_s",600);
            p.put("reported_mq3_correlation","R2 > 0.97 over 10-40 ppm");
            p.put("reported_prediction_accuracy_ppm_all","+/-9.2");
            p.put("reported_prediction_accuracy_ppm_below_50","+/-6.7");
            p.put("raw_data_request","REQ-006");p.put("curve_coefficients_registered",false);p.put("m4",false);
        }catch(Exception ignored){}
        evidence(db,"MQ3",ethylene,"EXTERNAL_KNOWN_CONCENTRATION_RESPONSE_DESCRIPTOR",p.toString(),10.0,50.0,
                SRC_KRIVEC,"B_PEER_REVIEWED_KNOWN_CONCENTRATION","ACCEPTED_SOURCE_DESCRIPTOR_AWAITING_RAW");
    }

    private static void seedUciDriftEvidence(SQLiteDatabase db){
        Object[][] rows={
                {21,"Ammonia",50.0,1000.0},
                {54,"Acetaldehyde",5.0,500.0},
                {89,"Ethylene",10.0,300.0},
                {103,"Ethanol",10.0,600.0},
                {110,"Acetone",12.0,1000.0},
                {95,"Toluene",10.0,100.0}
        };
        for(Object[] row:rows){
            int legacy=(Integer)row[0];Long uid=chemicalUid(db,legacy);if(uid==null)continue;
            double min=(Double)row[2],max=(Double)row[3];
            JSONObject p=new JSONObject();
            try{
                p.put("analyte",row[1]);p.put("dataset_measurements",13910);p.put("duration_months",36);
                p.put("source_array","16 Figaro TGS MOX channels");p.put("hr069_mq_overlap",0);
                p.put("role","cross-array concentration/identity/drift transfer evidence");p.put("m4",false);
            }catch(Exception ignored){}
            evidence(db,"ARRAY:TGS2600,TGS2602,TGS2610,TGS2620",uid,
                    "EXTERNAL_CROSS_ARRAY_KNOWN_CONCENTRATION_DRIFT",p.toString(),min,max,
                    SRC_UCI270,"B_EXTERNAL_KNOWN_CONCENTRATION","ACCEPTED_TRANSFER_REFERENCE");
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
