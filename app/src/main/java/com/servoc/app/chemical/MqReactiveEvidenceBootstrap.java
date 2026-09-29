package com.servoc.app.chemical;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.json.JSONObject;

/** Reactive-gas evidence for installed HR069 MQ sensors. */
public final class MqReactiveEvidenceBootstrap {
    private MqReactiveEvidenceBootstrap() {}

    public static final String META_KEY="mq_reactive_evidence_hr069_version";
    public static final String SEED_VERSION="1";
    public static final String SRC_NOX="ALVEAR_SENSORS_2022_MQ135_NOX";

    public static void ensure(SQLiteDatabase db){
        if(db==null||db.isReadOnly())return;
        if(SEED_VERSION.equals(meta(db,META_KEY)))return;
        db.beginTransaction();
        try{
            seedSource(db);
            db.delete("sensor_evidence","source_id=?",new String[]{SRC_NOX});
            seedNox(db);
            putMeta(db,META_KEY,SEED_VERSION);
            putMeta(db,"bme280_role_hr069","temperature_humidity_pressure_context_and_compensation_only_not_chemical_identity_sensor");
            putMeta(db,"reactive_gas_mq135_caution","high H2S/SOx/Cl2/HCl exposure can corrode MQ135 and attenuate sensitivity; do not create quantitative identities without source-backed calibration");
            db.setTransactionSuccessful();
        }finally{db.endTransaction();}
    }

    private static void seedSource(SQLiteDatabase db){
        ContentValues v=new ContentValues();
        v.put("source_id",SRC_NOX);
        v.put("title","Smart and Portable Air-Quality Monitoring IoT Low-Cost Devices in Ibarra City, Ecuador");
        v.put("authority","Alvear-Puertas et al. / Sensors");
        v.put("doi","10.3390/s22187015");
        v.put("url","https://doi.org/10.3390/s22187015");
        v.put("evidence_tier","B");
        v.put("version","2022 / peer reviewed");
        v.put("notes","MQ135 NOx reference equation under 20 C, 65% RH, 21% O2 and RL=20 kOhm. R0 is defined at 100 ppm NOx. Published equation: NOx(ppm)=156-133*(Rs/R0). BME280 temperature/humidity may be retained as environmental covariates; pressure is context only for this source.");
        db.insertWithOnConflict("scientific_sources",null,v,SQLiteDatabase.CONFLICT_REPLACE);
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
        ContentValues v=new ContentValues();v.put("sensor_key","MQ135");v.put("chemical_uid",uid);
        v.put("evidence_type","PEER_REVIEWED_MQ135_NOX_RESPONSE_EQUATION");v.put("equation",eq);v.put("coefficients",p.toString());
        v.put("source_id",SRC_NOX);v.put("evidence_tier","B_PEER_REVIEWED_EXACT_SENSOR_CURVE");
        v.put("status","ACCEPTED_IDENTITY_AND_PPM_REFERENCE");db.insertOrThrow("sensor_evidence",null,v);
    }

    private static Long chemicalUid(SQLiteDatabase db,int legacyId){try(Cursor c=db.rawQuery("SELECT uid FROM chemicals WHERE legacy_id=? LIMIT 1",new String[]{String.valueOf(legacyId)})){return c.moveToFirst()?c.getLong(0):null;}}
    private static String meta(SQLiteDatabase db,String key){try(Cursor c=db.rawQuery("SELECT v FROM meta WHERE k=?",new String[]{key})){return c.moveToFirst()?c.getString(0):null;}}
    private static void putMeta(SQLiteDatabase db,String key,String value){ContentValues v=new ContentValues();v.put("k",key);v.put("v",value);db.insertWithOnConflict("meta",null,v,SQLiteDatabase.CONFLICT_REPLACE);}
}
