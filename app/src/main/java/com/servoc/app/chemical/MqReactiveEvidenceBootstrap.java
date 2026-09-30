package com.servoc.app.chemical;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.json.JSONArray;
import org.json.JSONObject;

/** Reactive/inorganic gas evidence for installed HR069 MQ sensors. */
public final class MqReactiveEvidenceBootstrap {
    private MqReactiveEvidenceBootstrap() {}

    public static final String META_KEY="mq_reactive_evidence_hr069_version";
    public static final String SEED_VERSION="2";
    public static final String SRC_NOX="ALVEAR_SENSORS_2022_MQ135_NOX";
    public static final String SRC_EMISSIONS="MARTILLANO_JCOMM_2017_MQ_EMISSIONS";
    public static final String SRC_CHLORINE="HADI_ECS_2022_MODIFIED_MQ135_CHLORINE";

    public static void ensure(SQLiteDatabase db){
        if(db==null||db.isReadOnly())return;
        if(SEED_VERSION.equals(meta(db,META_KEY)))return;
        db.beginTransaction();
        try{
            seedSources(db);
            db.delete("sensor_evidence","source_id IN (?,?,?)",new String[]{SRC_NOX,SRC_EMISSIONS,SRC_CHLORINE});
            seedNox(db);
            seedNo2Validation(db);
            seedSo2Validation(db);
            seedModifiedMq135Chlorine(db);
            putMeta(db,META_KEY,SEED_VERSION);
            putMeta(db,"bme280_role_hr069","temperature_humidity_pressure_context_and_compensation_only_not_chemical_identity_sensor");
            putMeta(db,"reactive_gas_mq135_caution","high H2S/SOx/Cl2/HCl exposure can corrode MQ135 and attenuate sensitivity; do not create quantitative identities without source-backed calibration");
            db.setTransactionSuccessful();
        }finally{db.endTransaction();}
    }

    private static void seedSources(SQLiteDatabase db){
        source(db,SRC_NOX,
                "Smart and Portable Air-Quality Monitoring IoT Low-Cost Devices in Ibarra City, Ecuador",
                "Alvear-Puertas et al. / Sensors","10.3390/s22187015","https://doi.org/10.3390/s22187015","B","2022 / peer reviewed",
                "MQ135 NOx reference equation under 20 C, 65% RH, 21% O2 and RL=20 kOhm. R0 is defined at 100 ppm NOx. Published equation: NOx(ppm)=156-133*(Rs/R0). BME280 temperature/humidity may be retained as environmental covariates; pressure is context only for this source.");
        source(db,SRC_EMISSIONS,
                "Android Based Real-Time Industrial Emission Monitoring System Using IoT Technology",
                "Martillano et al. / Journal of Communications","10.12720/jcm.12.11.623-629","https://doi.org/10.12720/jcm.12.11.623-629","B","2017 / published journal article",
                "Industrial emission validation used MQ135 for NO2 and MQ7 for SO2. Self-calibration checks reported 10 ppm NO2 -> 9.9 ppm and 50 ppm SO2 -> 49.4 ppm. Independent handheld-device comparison points were also reported around sub-ppm stack concentrations.");
        source(db,SRC_CHLORINE,
                "Development of Low-Cost and Multi-Material Sensing Approach for MQ 135 Sensor",
                "Hadi et al. / ECS Transactions","10.1149/10701.17309ecst","https://doi.org/10.1149/10701.17309ecst","C","2022 / experimental modified-sensor study",
                "MQ135 sensing/response hardware was modified to respond to hydrogen, carbon monoxide and chlorine. A chlorine response time of about 36 s was reported. The modification means this evidence is feasibility/protocol evidence only and must not be transferred quantitatively to stock HR069 MQ135.");
    }

    private static void seedNox(SQLiteDatabase db){
        Long uid=chemicalUid(db,141);if(uid==null)return;
        String eq="NOx_ppm = 156 - 133 * (Rs/R0)";
        JSONObject p=new JSONObject();
        try{
            p.put("analyte","NOx");p.put("sensor","MQ135");p.put("installed_hr069",true);
            p.put("temperature_c",20);p.put("relative_humidity_percent",65);p.put("oxygen_percent",21);p.put("load_resistance_kohm",20);
            p.put("r0_reference","MQ135 resistance at 100 ppm NOx");p.put("published_equation",eq);
            p.put("bme280_temperature_humidity_use","environmental compensation/context");
            p.put("bme280_pressure_use","context only; not part of published conversion");
            p.put("identity_fingerprint_accepted",true);p.put("ppm_reference_accepted",true);p.put("m4",false);
        }catch(Exception ignored){}
        evidence(db,"MQ135",uid,"PEER_REVIEWED_MQ135_NOX_RESPONSE_EQUATION",eq,p.toString(),null,null,SRC_NOX,
                "B_PEER_REVIEWED_EXACT_SENSOR_CURVE","ACCEPTED_IDENTITY_AND_PPM_REFERENCE");
    }

    private static void seedNo2Validation(SQLiteDatabase db){
        Long uid=chemicalUid(db,14);if(uid==null)return;
        JSONObject p=new JSONObject();
        try{
            p.put("analyte","Nitrogen Dioxide");p.put("sensor","MQ135");
            p.put("self_calibration_expected_ppm",10.0);p.put("self_calibration_sensor_ppm",9.9);p.put("self_calibration_error_percent",1.0);
            p.put("third_party_reference_ppm",new JSONArray().put(0.62).put(0.66));
            p.put("mq135_sensor_ppm",new JSONArray().put(0.64).put(0.65));
            p.put("reported_error_percent",new JSONArray().put(3.2258).put(1.5152));
            p.put("reference_method","handheld industrial reference measurement under pollution-control expert supervision");
            p.put("identity_fingerprint_accepted",true);p.put("quantitative_validation_points",true);p.put("full_rs_r0_curve_available",false);p.put("m4",false);
        }catch(Exception ignored){}
        evidence(db,"MQ135",uid,"PUBLISHED_MQ135_NO2_THIRD_PARTY_PPM_VALIDATION",null,p.toString(),0.62,10.0,SRC_EMISSIONS,
                "B_PUBLISHED_EXACT_SENSOR_REFERENCE_COMPARISON","ACCEPTED_IDENTITY_AND_QUANTITATIVE_VALIDATION_POINTS");
    }

    private static void seedSo2Validation(SQLiteDatabase db){
        Long uid=chemicalUid(db,13);if(uid==null)return;
        JSONObject p=new JSONObject();
        try{
            p.put("analyte","Sulfur Dioxide");p.put("sensor","MQ7");
            p.put("self_calibration_expected_ppm",50.0);p.put("self_calibration_sensor_ppm",49.4);p.put("self_calibration_error_percent",1.2);
            p.put("third_party_reference_ppm",new JSONArray().put(0.82).put(0.89));
            p.put("mq7_sensor_ppm",new JSONArray().put(0.84).put(0.90));
            p.put("reported_error_percent",new JSONArray().put(2.439).put(1.1236));
            p.put("cross_sensitivity_warning","MQ7 is primarily a CO sensor; use as source-specific SO2 cross-response evidence, not selective SO2 metrology");
            p.put("heater_cycle_required",true);p.put("quantitative_validation_points",true);p.put("full_rs_r0_curve_available",false);p.put("m4",false);
        }catch(Exception ignored){}
        evidence(db,"MQ7",uid,"PUBLISHED_MQ7_SO2_THIRD_PARTY_PPM_VALIDATION",null,p.toString(),0.82,50.0,SRC_EMISSIONS,
                "B_PUBLISHED_EXACT_SENSOR_CROSS_RESPONSE_REFERENCE","ACCEPTED_SO2_CROSS_RESPONSE_VALIDATION_POINTS_HEATER_GATED");
    }

    private static void seedModifiedMq135Chlorine(SQLiteDatabase db){
        Long uid=chemicalUid(db,12);if(uid==null)return;
        JSONObject p=new JSONObject();
        try{
            p.put("analyte","Chlorine");p.put("sensor","MQ135");p.put("hardware_modified",true);p.put("reported_response_time_s",36.0);
            p.put("direct_hr069_transfer",false);p.put("reason","published experiment modified the MQ135 sensing/response unit; stock HR069 MQ135 must not inherit the chlorine response quantitatively");
            p.put("future_use","feasibility/protocol reference only");p.put("m4",false);
        }catch(Exception ignored){}
        evidence(db,"REFERENCE:MODIFIED_MQ135",uid,"MODIFIED_MQ135_CHLORINE_FEASIBILITY_EXPERIMENT",null,p.toString(),null,null,SRC_CHLORINE,
                "C_MODIFIED_SENSOR_FEASIBILITY","REFERENCE_ONLY_NO_DIRECT_HR069_ID_OR_PPM");
    }

    private static void evidence(SQLiteDatabase db,String sensorKey,Long chemicalUid,String type,String equation,String payload,Double minPpm,Double maxPpm,String sourceId,String tier,String status){
        ContentValues v=new ContentValues();v.put("sensor_key",sensorKey);if(chemicalUid!=null)v.put("chemical_uid",chemicalUid);v.put("evidence_type",type);
        if(equation!=null)v.put("equation",equation);if(payload!=null)v.put("coefficients",payload);if(minPpm!=null)v.put("min_ppm",minPpm);if(maxPpm!=null)v.put("max_ppm",maxPpm);
        v.put("source_id",sourceId);v.put("evidence_tier",tier);v.put("status",status);db.insertOrThrow("sensor_evidence",null,v);
    }
    private static void source(SQLiteDatabase db,String id,String title,String authority,String doi,String url,String tier,String version,String notes){
        ContentValues v=new ContentValues();v.put("source_id",id);v.put("title",title);v.put("authority",authority);v.put("doi",doi);v.put("url",url);v.put("evidence_tier",tier);v.put("version",version);v.put("notes",notes);
        db.insertWithOnConflict("scientific_sources",null,v,SQLiteDatabase.CONFLICT_REPLACE);
    }
    private static Long chemicalUid(SQLiteDatabase db,int legacyId){try(Cursor c=db.rawQuery("SELECT uid FROM chemicals WHERE legacy_id=? LIMIT 1",new String[]{String.valueOf(legacyId)})){return c.moveToFirst()?c.getLong(0):null;}}
    private static String meta(SQLiteDatabase db,String key){try(Cursor c=db.rawQuery("SELECT v FROM meta WHERE k=?",new String[]{key})){return c.moveToFirst()?c.getString(0):null;}}
    private static void putMeta(SQLiteDatabase db,String key,String value){ContentValues v=new ContentValues();v.put("k",key);v.put("v",value);db.insertWithOnConflict("meta",null,v,SQLiteDatabase.CONFLICT_REPLACE);}
}
