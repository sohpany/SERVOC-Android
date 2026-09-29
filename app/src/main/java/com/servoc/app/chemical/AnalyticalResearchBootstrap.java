package com.servoc.app.chemical;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.servoc.app.models.ModelRuntimeRegistry;
import com.servoc.app.models.MqDataScience76Catalog;

import org.json.JSONObject;

/**
 * Idempotent HR069 research-library bootstrap.
 *
 * This seed deliberately stores external/literature/community analytical material as
 * RESEARCH_EXTERNAL. It never creates an M4 validation marker and never activates a
 * generic MQDataScience model as a field quantitative model.
 */
public final class AnalyticalResearchBootstrap {
    private AnalyticalResearchBootstrap() {}

    public static final String META_KEY="analytical_research_seed_hr069_version";
    public static final String SEED_VERSION="1";
    public static final int GLOBAL_MODEL_CHEMICAL_ID=-1;

    private static final String[] MANAGED_SOURCES={
            "MQDATASCIENCE_6","UMBU_MQ135_2023","UCI_GSALC_2023","FRONTIERS_VOC_2026",
            "VISUAL_SMELL_2026","MULTIMODALGASDATA_2022","MQ3_MEOH_ETOH_2026",
            "MQ135_ENV_ETHANOL_2026","WINSEN_MQ5","FOOD_FRESHNESS_2025","ROBOFUSION_2026"
    };

    public static void ensure(SQLiteDatabase db){
        if(db==null||db.isReadOnly())return;
        String current=meta(db,META_KEY);
        if(SEED_VERSION.equals(current))return;
        db.beginTransaction();
        try{
            seedSources(db);
            clearManagedEvidence(db);
            seedResearchCurves(db);
            seedLiteratureCurves(db);
            seedExternalEvidence(db);
            seedDatasetDescriptors(db);
            seedMq76Models(db);
            putMeta(db,META_KEY,SEED_VERSION);
            putMeta(db,"analytical_research_seed_hr069_curves",String.valueOf(ResearchCurveRegistry.count()+LiteratureCurveRegistry.count()));
            putMeta(db,"analytical_research_seed_hr069_mq76_models",String.valueOf(MqDataScience76Catalog.COUNT));
            putMeta(db,"analytical_research_seed_hr069_policy","RESEARCH_EXTERNAL_NEVER_M4_WITHOUT_DEVICE_VALIDATION");
            db.setTransactionSuccessful();
        }finally{db.endTransaction();}
    }

    private static void seedSources(SQLiteDatabase db){
        source(db,"MQDATASCIENCE_6","MQDataScience v6.0.0 pinned analytical library","MQDataScience community project",null,
                "https://github.com/abcdaaaaaaaaa/MQDataScience","C","6.0.0 @ "+MqDataScience76Catalog.PINNED_COMMIT,
                "43 graph-derived MQ response priors plus the adopted 76-model research catalogue; never M4 by source alone.");
        source(db,"UMBU_MQ135_2023",LiteratureCurveRegistry.UMBU_2023_TITLE,"Umbu et al.",LiteratureCurveRegistry.UMBU_2023_DOI,
                "https://doi.org/"+LiteratureCurveRegistry.UMBU_2023_DOI,"B","2023","Peer-reviewed MQ135 Rs/R0 graph regressions; published domain 10-100 ppm.");
        source(db,"UCI_GSALC_2023","Gas sensor array low-concentration","UCI Machine Learning Repository","10.24432/C5CK6F",
                "https://doi.org/10.24432/C5CK6F","B","2023","Known-concentration time series; 50/100/200 ppb; includes MQ135. CC BY 4.0.");
        source(db,"FRONTIERS_VOC_2026","A lightweight intelligent model for VOC mixture analysis","Frontiers in Bioengineering and Biotechnology","10.3389/fbioe.2026.1821312",
                "https://doi.org/10.3389/fbioe.2026.1821312","B","2026","Concentration-labelled ethanol/isopropanol/acetone series, 1-15 ppm; includes MQ135 and MQ3.");
        source(db,"VISUAL_SMELL_2026","Visual smell fingerprinting with sensor arrays","Peer-reviewed study","10.1016/j.foohum.2026.101079",
                "https://doi.org/10.1016/j.foohum.2026.101079","B","2026","Exact/superset MQ2/3/4/5/6/7/8/9/135 array; pure ethanol and acetone fingerprints.");
        source(db,"MULTIMODALGASDATA_2022","MultimodalGasData: Multimodal Dataset for Gas Detection and Classification","Dataset/article authors","10.17632/zkwgkjkjn9.2",
                "https://doi.org/10.17632/zkwgkjkjn9.2","B","2022","6400 samples; NoGas/Smoke/Perfume/Mixture; CC BY 4.0; six HR069 channels overlap.");
        source(db,"MQ3_MEOH_ETOH_2026","Methanol/ethanol mixture identification with humidity suppression","Peer-reviewed study","10.1016/j.snb.2026.139758",
                "https://doi.org/10.1016/j.snb.2026.139758","B","2026","MQ3 dynamic-temperature-modulation evidence; HR069 fixed-heater transfer required.");
        source(db,"MQ135_ENV_ETHANOL_2026","MQ135 ethanol environmental-response study","Peer-reviewed study",null,
                "https://www.mdpi.com/","B","2026","0-500 ppm ethanol across temperature/RH states; setup-specific voltage model.");
        source(db,"WINSEN_MQ5","Winsen MQ-5 Semiconductor Sensor for Flammable Gas","Zhengzhou Winsen Electronics Technology Co., Ltd.",null,
                "https://www.winsen-sensor.com/","A","manufacturer","Butane/propane/methane qualitative/target-family evidence; no fabricated n-butane curve.");
        source(db,"FOOD_FRESHNESS_2025","Food Freshness Electronic Nose Dataset","Mehrab Mahdian","10.5281/zenodo.17285312",
                "https://doi.org/10.5281/zenodo.17285312","C","v1 2025-10-07","MQ2/3/4/5/6/7/8/9/135 time-series archive; all eight HR069 MQ models represented. Permission/license clarification pending; research reference only.");
        source(db,"ROBOFUSION_2026","RoboSense / RoboFusion real-world industrial MQ-array dataset","Amr Khamis et al.","10.1038/s41598-025-33603-5",
                "https://github.com/Amr-Khamis-84/RoboFusion-Dataset","B","2026","CC0 repository data; MQ2/3/5/7/8/135 overlap. Canonical-file relationship under author clarification.");
    }

    private static void clearManagedEvidence(SQLiteDatabase db){
        StringBuilder q=new StringBuilder("source_id IN (");
        String[] args=new String[MANAGED_SOURCES.length];
        for(int i=0;i<MANAGED_SOURCES.length;i++){if(i>0)q.append(',');q.append('?');args[i]=MANAGED_SOURCES[i];}
        q.append(')');
        db.delete("sensor_evidence",q.toString(),args);
        db.delete("models","chemical_id=? AND version LIKE ?",new String[]{String.valueOf(GLOBAL_MODEL_CHEMICAL_ID),"MQDataScience 6.0.0%"});
    }

    private static void seedResearchCurves(SQLiteDatabase db){
        for(ResearchCurveRegistry.Curve c:ResearchCurveRegistry.CURVES){
            Long uid=chemicalUid(db,c.chemical);if(uid==null)continue;
            JSONObject coefficients=new JSONObject();
            try{
                coefficients.put("ratioA",c.ratioA);coefficients.put("ratioB",c.ratioB);
                coefficients.put("outputScale",c.outputScale);coefficients.put("gasLabel",c.gasLabel);
                coefficients.put("note",c.note);coefficients.put("source_model","ratio = a * ppm^b");
            }catch(Exception ignored){}
            evidence(db,sensorName(c.sensor),uid,"RS_R0_CURVE_PRIOR","Rs/R0 = a * ppm^b",coefficients.toString(),
                    c.minPpm,c.maxPpm,null,null,null,"MQDATASCIENCE_6","C_PINNED_COMMUNITY_DATASHEET_FIT","RESEARCH_EXTERNAL");
        }
    }

    private static void seedLiteratureCurves(SQLiteDatabase db){
        for(LiteratureCurveRegistry.Curve c:LiteratureCurveRegistry.CURVES){
            Long uid=chemicalUid(db,c.chemical);if(uid==null)continue;
            JSONObject coefficients=new JSONObject();
            try{
                coefficients.put("ratioA",c.ratioA);coefficients.put("ratioB",c.ratioB);coefficients.put("r2",c.r2);
                coefficients.put("gasLabel",c.gasLabel);coefficients.put("note",c.note);coefficients.put("doi",c.doi);
            }catch(Exception ignored){}
            evidence(db,sensorName(c.sensor),uid,"PEER_REVIEWED_RS_R0_CURVE","Rs/R0 = a * ppm^b",coefficients.toString(),
                    c.minPpm,c.maxPpm,null,null,null,c.sourceId,"B_PEER_REVIEWED_GRAPH_FIT","RESEARCH_EXTERNAL");
        }
    }

    private static void seedExternalEvidence(SQLiteDatabase db){
        for(ExternalEvidenceCoverage.Evidence e:ExternalEvidenceCoverage.RECORDS){
            Long uid=chemicalUid(db,e.chemical);if(uid==null)continue;
            JSONObject payload=new JSONObject();
            try{
                payload.put("sourceType",e.sourceType);payload.put("sensorScope",e.sensorScope);
                payload.put("domain",e.domain);payload.put("note",e.note);payload.put("m4",false);
                payload.put("evidenceLevel",ExternalEvidenceCoverage.levelName(e.level));
            }catch(Exception ignored){}
            double[] ppm=ppmDomain(e.sourceId);
            evidence(db,e.sensorScope,uid,"EXTERNAL_"+ExternalEvidenceCoverage.levelName(e.level).toUpperCase().replace(' ','_').replace('/','_'),
                    null,payload.toString(),ppm[0],ppm[1],null,null,null,e.sourceId,
                    e.level>=ExternalEvidenceCoverage.KNOWN_CONCENTRATION_RESPONSE?"B_EXTERNAL_KNOWN_CONCENTRATION":"C_EXTERNAL_FINGERPRINT","RESEARCH_EXTERNAL");
        }
    }

    private static void seedDatasetDescriptors(SQLiteDatabase db){
        JSONObject food=new JSONObject();
        try{
            food.put("archive","Food-Freshness-E-Nose-Dataset.zip");
            food.put("published_md5","008b8ccb62d7a7d51ff9ffcfd698a1e5");
            food.put("sensors","MQ2,MQ3,MQ4,MQ5,MQ6,MQ7,MQ8,MQ9,MQ135");
            food.put("hr069_overlap","8/8 plus MQ6");
            food.put("permission_status","PENDING_AUTHOR_CONFIRMATION");
            food.put("m4",false);
        }catch(Exception ignored){}
        evidence(db,"ARRAY:MQ2,MQ3,MQ4,MQ5,MQ6,MQ7,MQ8,MQ9,MQ135",null,"EXTERNAL_FINGERPRINT_DATASET",null,food.toString(),null,null,null,null,null,"FOOD_FRESHNESS_2025","C_EXTERNAL_DATASET","RESEARCH_EXTERNAL");

        JSONObject robo=new JSONObject();
        try{
            robo.put("repository","Amr-Khamis-84/RoboFusion-Dataset");
            robo.put("sensors","MQ2,MQ3,MQ5,MQ7,MQ8,MQ135");
            robo.put("license","CC0-1.0");robo.put("m4",false);
        }catch(Exception ignored){}
        evidence(db,"ARRAY:MQ2,MQ3,MQ5,MQ7,MQ8,MQ135",null,"EXTERNAL_TEMPORAL_DATASET",null,robo.toString(),null,null,null,null,null,"ROBOFUSION_2026","B_EXTERNAL_DATASET","RESEARCH_EXTERNAL");
    }

    private static void seedMq76Models(SQLiteDatabase db){
        String version="MQDataScience 6.0.0 @ "+MqDataScience76Catalog.PINNED_COMMIT;
        String now="2026-09-29T00:00:00Z";
        for(int i=0;i<MqDataScience76Catalog.COUNT;i++){
            JSONObject metrics=new JSONObject(),payload=new JSONObject();
            try{
                metrics.put("runtime_status",ModelRuntimeRegistry.runtimeStatus(i));
                metrics.put("system_integration_status",ModelRuntimeRegistry.systemIntegrationStatus(i));
                metrics.put("android_native",ModelRuntimeRegistry.isAndroidNative(i));
                metrics.put("upstream_verified",ModelRuntimeRegistry.isUpstreamVerified(i));
                metrics.put("approximation",ModelRuntimeRegistry.isApproximation(i));
                payload.put("index",i);payload.put("name",MqDataScience76Catalog.NAMES[i]);
                payload.put("source",MqDataScience76Catalog.SOURCE);payload.put("pinned_commit",MqDataScience76Catalog.PINNED_COMMIT);
                payload.put("adopted",true);payload.put("research_integrated",ModelRuntimeRegistry.isResearchIntegrated(i));
                payload.put("evidence_class","RESEARCH_MODEL");payload.put("m4",false);
            }catch(Exception ignored){}
            ContentValues v=new ContentValues();v.put("chemical_id",GLOBAL_MODEL_CHEMICAL_ID);v.put("model_type","MQ76:"+MqDataScience76Catalog.NAMES[i]);
            v.put("version",version);v.put("created_ts",now);v.put("training_count",0);v.put("metrics",metrics.toString());v.put("payload",payload.toString());v.put("active",0);
            db.insertOrThrow("models",null,v);
        }
    }

    private static double[] ppmDomain(String sourceId){
        if("UCI_GSALC_2023".equals(sourceId))return new double[]{0.05,0.2};
        if("FRONTIERS_VOC_2026".equals(sourceId))return new double[]{1.0,15.0};
        if("MQ135_ENV_ETHANOL_2026".equals(sourceId))return new double[]{0.0,500.0};
        if("UMBU_MQ135_2023".equals(sourceId))return new double[]{10.0,100.0};
        return new double[]{Double.NaN,Double.NaN};
    }

    private static Long chemicalUid(SQLiteDatabase db,int legacyId){
        try(Cursor c=db.rawQuery("SELECT uid FROM chemicals WHERE legacy_id=? LIMIT 1",new String[]{String.valueOf(legacyId)})){return c.moveToFirst()?c.getLong(0):null;}
    }

    private static String sensorName(int sensor){
        switch(sensor){
            case GasEngine.MQ2:return "MQ2";case GasEngine.MQ3:return "MQ3";case GasEngine.MQ4:return "MQ4";case GasEngine.MQ5:return "MQ5";
            case GasEngine.MQ7:return "MQ7";case GasEngine.MQ8:return "MQ8";case GasEngine.MQ9:return "MQ9";case GasEngine.MQ135:return "MQ135";
            default:return "MQ?";
        }
    }

    private static void evidence(SQLiteDatabase db,String sensorKey,Long chemicalUid,String type,String equation,String coefficients,
                                 Double minPpm,Double maxPpm,Double minTemp,Double maxTemp,Double minRh,String sourceId,String tier,String status){
        ContentValues v=new ContentValues();v.put("sensor_key",sensorKey);if(chemicalUid!=null)v.put("chemical_uid",chemicalUid);v.put("evidence_type",type);
        if(equation!=null)v.put("equation",equation);if(coefficients!=null)v.put("coefficients",coefficients);
        if(minPpm!=null&&Double.isFinite(minPpm))v.put("min_ppm",minPpm);if(maxPpm!=null&&Double.isFinite(maxPpm))v.put("max_ppm",maxPpm);
        if(minTemp!=null)v.put("min_temp_c",minTemp);if(maxTemp!=null)v.put("max_temp_c",maxTemp);if(minRh!=null)v.put("min_rh",minRh);
        v.put("source_id",sourceId);v.put("evidence_tier",tier);v.put("status",status);db.insertOrThrow("sensor_evidence",null,v);
    }

    private static void source(SQLiteDatabase db,String id,String title,String authority,String doi,String url,String tier,String version,String notes){
        ContentValues v=new ContentValues();v.put("source_id",id);v.put("title",title);v.put("authority",authority);if(doi!=null)v.put("doi",doi);v.put("url",url);
        v.put("evidence_tier",tier);v.put("version",version);v.put("notes",notes);db.insertWithOnConflict("scientific_sources",null,v,SQLiteDatabase.CONFLICT_REPLACE);
    }

    private static String meta(SQLiteDatabase db,String key){try(Cursor c=db.rawQuery("SELECT v FROM meta WHERE k=?",new String[]{key})){return c.moveToFirst()?c.getString(0):null;}}
    private static void putMeta(SQLiteDatabase db,String key,String value){ContentValues v=new ContentValues();v.put("k",key);v.put("v",value);db.insertWithOnConflict("meta",null,v,SQLiteDatabase.CONFLICT_REPLACE);}
}
