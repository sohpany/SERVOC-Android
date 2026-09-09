package com.servoc.app.chemcore;

import android.content.ContentValues;
import android.database.Cursor;

import com.servoc.app.data.ExperimentsDb;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** Versioned persistence and activation for auditable ChemCore model artifacts. */
public final class ChemCoreModelArtifactRepository {
    private final ExperimentsDb db;

    public ChemCoreModelArtifactRepository(ExperimentsDb db){
        if(db==null)throw new IllegalArgumentException("database required");
        this.db=db;ChemCoreSchema.ensure(db.getWritableDatabase());
    }

    public long save(ChemCoreModelCard card,JSONObject payload,boolean active){
        if(card==null||payload==null||card.modelType.isEmpty()||card.version.isEmpty())return -1;
        String raw=payload.toString();String hash=sha256(raw);
        if(!card.sha256.isEmpty()&&!card.sha256.equalsIgnoreCase(hash))
            throw new IllegalArgumentException("model card SHA-256 does not match payload");
        if(active)db.getWritableDatabase().execSQL("UPDATE chemcore_model_artifacts SET active=0 WHERE model_type=?",new Object[]{card.modelType});
        ContentValues v=new ContentValues();
        v.put("model_type",card.modelType);v.put("version",card.version);v.put("created_ts",isoNow());
        v.put("feature_schema",card.featureSchema);v.put("training_scope",card.trainingScope);
        v.put("metrics",card.toJson().optJSONObject("metrics").toString());v.put("validity_domain",card.validityDomain);
        v.put("payload",raw);v.put("sha256",hash);v.put("active",active?1:0);
        return db.getWritableDatabase().insert("chemcore_model_artifacts",null,v);
    }

    public JSONObject activePayload(String modelType){
        if(modelType==null||modelType.trim().isEmpty())return null;
        try(Cursor c=db.getReadableDatabase().rawQuery(
                "SELECT payload,sha256 FROM chemcore_model_artifacts WHERE model_type=? AND active=1 ORDER BY id DESC LIMIT 1",
                new String[]{modelType.trim()})){
            if(!c.moveToFirst())return null;String raw=c.getString(0),expected=c.getString(1);
            if(raw==null||expected==null||!expected.equalsIgnoreCase(sha256(raw)))return null;
            try{return new JSONObject(raw);}catch(Exception e){return null;}
        }
    }

    public boolean activate(long id){
        if(id<=0)return false;
        String type=null;
        try(Cursor c=db.getReadableDatabase().rawQuery("SELECT model_type FROM chemcore_model_artifacts WHERE id=? LIMIT 1",new String[]{String.valueOf(id)})){
            if(c.moveToFirst())type=c.getString(0);
        }
        if(type==null)return false;
        db.getWritableDatabase().beginTransaction();
        try{
            db.getWritableDatabase().execSQL("UPDATE chemcore_model_artifacts SET active=0 WHERE model_type=?",new Object[]{type});
            ContentValues v=new ContentValues();v.put("active",1);
            int changed=db.getWritableDatabase().update("chemcore_model_artifacts",v,"id=?",new String[]{String.valueOf(id)});
            db.getWritableDatabase().setTransactionSuccessful();return changed==1;
        }finally{db.getWritableDatabase().endTransaction();}
    }

    public static String sha256(String raw){
        try{byte[] h=MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));StringBuilder s=new StringBuilder();for(byte b:h)s.append(String.format(Locale.US,"%02x",b&255));return s.toString();}
        catch(Exception e){throw new IllegalStateException("SHA-256 unavailable",e);}
    }

    private static String isoNow(){SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'",Locale.US);f.setTimeZone(TimeZone.getTimeZone("UTC"));return f.format(new Date());}
}
