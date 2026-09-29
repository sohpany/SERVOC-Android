package com.servoc.app.chemical;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Seeds a source-preserving RoboFusion real-data operational gas-leak benchmark for HR069.
 *
 * This evidence is intentionally NOT attached to a chemical_uid. Hazard_1 is the source
 * operational gas-leak label and does not identify a pure gas. The six-channel prototype may
 * support future anomaly/gas-leak transfer validation, but it is never ppm calibration or M4.
 */
public final class OperationalHazardTransferBootstrap {
    private OperationalHazardTransferBootstrap() {}

    public static final String META_KEY="operational_hazard_transfer_hr069_version";
    public static final String SEED_VERSION="1";
    public static final String SOURCE_ID="ROBOFUSION_REAL_H1_2025";
    public static final String ASSET="research_data/hr069_analytical_library/robofusion_hr069_gasleak_operational_transfer_v1.json";
    public static final String SOURCE_COMMIT="d3907eda585cf202fc00c462b550d2512d5b9e6f";
    public static final String SOURCE_SHA256="ae4637a1fbffbb0d7e0750f6623b17bcfecc184043df58702cea07a158546528";
    public static final String DERIVED_SHA256="052a6669d22a56c3d7d3ee1119fd5619a8c6e301b74b22e37784dd06d99669e6";

    public static final int SOURCE_ROWS=69120;
    public static final int ORIGINAL_ROWS_USED=42704;
    public static final int INTERPOLATED_ROWS_EXCLUDED=26416;
    public static final int NORMAL_ROWS=41537;
    public static final int HAZARD1_ROWS=1167;
    public static final boolean PURE_ANALYTE_IDENTITY=false;
    public static final boolean QUANTITATIVE_PPM=false;
    public static final boolean M4=false;

    private static final String[] CHANNELS={"MQ2","MQ4","MQ5","MQ7","MQ8","MQ135"};
    private static final double[] HAZARD1_MEAN={
            -0.16218385913659927,0.6895123557707743,-0.4128213009720952,
            -0.6769106258203069,0.29794005163845416,-0.23639028919702562};
    private static final double[] HAZARD1_SIGMA={
            0.8886916640838581,0.5262006722158789,0.2560133917667277,
            0.41780831256948464,0.39548286634707874,0.5210604240378981};
    private static final double[] NORMAL_MEAN={
            0.8011661207651951,0.402437045582265,-0.39388039777911077,
            -0.4590498997274038,0.2211885748434197,-0.19371184833674884};
    private static final double[] NORMAL_SIGMA={
            1.0517744348091893,0.29324754503949085,0.2631175161079973,
            0.29760337032955236,0.15955579247931517,0.21871404741378692};

    public static int overlapCount(){return CHANNELS.length;}
    public static String[] channels(){return CHANNELS.clone();}
    public static double[] hazard1Prototype(){return HAZARD1_MEAN.clone();}
    public static double[] hazard1Sigma(){return HAZARD1_SIGMA.clone();}
    public static double[] normalPrototype(){return NORMAL_MEAN.clone();}
    public static double[] normalSigma(){return NORMAL_SIGMA.clone();}

    public static void ensure(SQLiteDatabase db){
        if(db==null||db.isReadOnly())return;
        if(SEED_VERSION.equals(meta(db,META_KEY)))return;
        db.beginTransaction();
        try{
            seedSource(db);
            db.delete("sensor_evidence","source_id=?",new String[]{SOURCE_ID});
            seedEvidence(db);
            putMeta(db,META_KEY,SEED_VERSION);
            putMeta(db,"operational_hazard_transfer_hr069_release","0.10.2/HR069");
            putMeta(db,"operational_hazard_transfer_hr069_asset_sha256",DERIVED_SHA256);
            putMeta(db,"operational_hazard_transfer_hr069_policy","OPERATIONAL_GAS_LEAK_BENCHMARK_ONLY_NO_CHEMICAL_ID_NO_PPM_NO_M4");
            db.setTransactionSuccessful();
        }finally{db.endTransaction();}
    }

    private static void seedSource(SQLiteDatabase db){
        ContentValues v=new ContentValues();
        v.put("source_id",SOURCE_ID);
        v.put("title","RoboFusion real H1 operational gas-leak dataset");
        v.put("authority","Amr Khamis / RoboFusion-Dataset");
        v.put("doi","");
        v.put("url","https://github.com/Amr-Khamis-84/RoboFusion-Dataset");
        v.put("evidence_tier","C");
        v.put("version",SOURCE_COMMIT+" / CC0-1.0");
        v.put("notes","Pinned Real_Dataset_12_Days_5_H1.csv. Six HR069 MQ channels overlap. 69,120 source rows; 42,704 Status=Original rows used after excluding 26,416 Interpolation rows. Source classes are Normal and Hazard_1 only. Hazard_1 is an operational gas-leak class, not a chemical identity. REQ-002 remains open for provenance/calibration clarification.");
        db.insertWithOnConflict("scientific_sources",null,v,SQLiteDatabase.CONFLICT_REPLACE);
    }

    private static void seedEvidence(SQLiteDatabase db){
        JSONObject p=new JSONObject();
        try{
            p.put("asset",ASSET);
            p.put("source_commit",SOURCE_COMMIT);
            p.put("source_sha256",SOURCE_SHA256);
            p.put("derived_sha256",DERIVED_SHA256);
            p.put("license","CC0-1.0");
            p.put("channels",array(CHANNELS));
            p.put("hr069_overlap_count",overlapCount());
            p.put("missing_hr069_channels",new JSONArray().put("MQ3").put("MQ9"));
            p.put("source_rows",SOURCE_ROWS);
            p.put("original_rows_used",ORIGINAL_ROWS_USED);
            p.put("interpolated_rows_excluded",INTERPOLATED_ROWS_EXCLUDED);
            p.put("quality_column","Status");
            p.put("quality_policy","Original only; Interpolation excluded");
            p.put("classes",new JSONArray().put("Normal").put("Hazard_1"));
            p.put("normal_rows",NORMAL_ROWS);
            p.put("hazard1_rows",HAZARD1_ROWS);
            p.put("transform","CLR of log10(1+x) across six overlapping MQ channels");
            p.put("normal_prototype",array(NORMAL_MEAN));
            p.put("normal_sigma",array(NORMAL_SIGMA));
            p.put("hazard1_prototype",array(HAZARD1_MEAN));
            p.put("hazard1_sigma",array(HAZARD1_SIGMA));
            p.put("operational_gas_leak_benchmark",true);
            p.put("pure_analyte_identity",PURE_ANALYTE_IDENTITY);
            p.put("quantitative_ppm",QUANTITATIVE_PPM);
            p.put("m4",M4);
        }catch(Exception ignored){}

        ContentValues v=new ContentValues();
        v.put("sensor_key","ARRAY:MQ2,MQ4,MQ5,MQ7,MQ8,MQ135");
        v.put("evidence_type","EXTERNAL_OPERATIONAL_GAS_LEAK_FINGERPRINT_ASSET");
        v.put("coefficients",p.toString());
        v.put("source_id",SOURCE_ID);
        v.put("evidence_tier","C_EXTERNAL_OPERATIONAL");
        v.put("status","ACCEPTED_OPERATIONAL_BENCHMARK_TRANSFER_REQUIRED");
        db.insertOrThrow("sensor_evidence",null,v);
    }

    private static JSONArray array(String[] a){JSONArray out=new JSONArray();for(String v:a)out.put(v);return out;}
    private static JSONArray array(double[] a){JSONArray out=new JSONArray();for(double v:a)out.put(Double.valueOf(v));return out;}
    private static String meta(SQLiteDatabase db,String key){
        try(Cursor c=db.rawQuery("SELECT v FROM meta WHERE k=?",new String[]{key})){return c.moveToFirst()?c.getString(0):null;}
    }
    private static void putMeta(SQLiteDatabase db,String key,String value){
        ContentValues v=new ContentValues();v.put("k",key);v.put("v",value);
        db.insertWithOnConflict("meta",null,v,SQLiteDatabase.CONFLICT_REPLACE);
    }
}
