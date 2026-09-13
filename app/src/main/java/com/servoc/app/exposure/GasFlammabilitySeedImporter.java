package com.servoc.app.exposure;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.servoc.app.chemical.ChemicalIdentityResolver;
import com.servoc.app.chemical.ChemicalIdentitySchema;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

/** Imports official NIOSH NPG LFL/UEL references without inventing missing values. */
public final class GasFlammabilitySeedImporter {
    public static final String SOURCE_ID="NIOSH_NPG_FLAMMABILITY";
    private GasFlammabilitySeedImporter(){}

    public static final class Result {
        public final int sourceRecords,lflInserted,uflInserted,unmatched;public final String hash;
        Result(int sourceRecords,int lflInserted,int uflInserted,int unmatched,String hash){this.sourceRecords=sourceRecords;this.lflInserted=lflInserted;this.uflInserted=uflInserted;this.unmatched=unmatched;this.hash=hash;}
    }

    public static Result importSeed(SQLiteDatabase db,InputStream input)throws Exception{
        ChemicalIdentitySchema.ensure(db);byte[] bytes=readAll(input);String hash=sha256(bytes);JSONObject root=new JSONObject(new String(bytes,StandardCharsets.UTF_8));
        if(!"hazrob.flammability-seed.v1".equals(root.optString("schema")))throw new IllegalArgumentException("unsupported flammability seed schema");
        if(!"NIOSH".equals(root.optString("authority"))||!"NPG_FLAMMABILITY".equals(root.optString("dataset"))||!root.optBoolean("authoritative",false))throw new IllegalArgumentException("authoritative NIOSH flammability provenance missing");
        JSONArray rows=root.getJSONArray("records");if(rows.length()<500||root.optInt("record_count",-1)!=rows.length())throw new IllegalArgumentException("incomplete flammability seed");
        int declaredLfl=root.optInt("numeric_lfl_count",0);if(declaredLfl<50)throw new IllegalArgumentException("insufficient numeric LFL coverage");
        int lfl=0,ufl=0,unmatched=0;db.beginTransaction();try{
            ensureSource(db,root.optString("generated_utc",""),hash);db.delete("exposure_limits","source_id=?",new String[]{SOURCE_ID});
            for(int i=0;i<rows.length();i++){
                JSONObject r=rows.getJSONObject(i);String cas=clean(r.optString("cas",null)),name=clean(r.optString("name_en",null)),url=clean(r.optString("source_url",null));
                if(name==null){unmatched++;continue;}Long uid;try{uid=ChemicalIdentityResolver.resolveOrCreate(db,SOURCE_ID,ChemicalIdentityResolver.stableSourceKey(cas,name),url==null?"https://www.cdc.gov/niosh/npg/":url,hash,cas,name);}catch(Exception identityError){unmatched++;continue;}
                Double low=finitePositive(r,"lfl_pct"),high=finitePositive(r,"ufl_pct");
                if(low!=null){insert(db,uid,"LFL",low,r,"lfl_text",url);lfl++;}
                if(high!=null){insert(db,uid,"UFL",high,r,"ufl_text",url);ufl++;}
            }
            putMeta(db,"flammability_niosh_npg_sha256",hash);putMeta(db,"flammability_niosh_npg_records",String.valueOf(rows.length()));putMeta(db,"flammability_niosh_npg_lfl_inserted",String.valueOf(lfl));putMeta(db,"flammability_niosh_npg_ufl_inserted",String.valueOf(ufl));putMeta(db,"flammability_niosh_npg_unmatched",String.valueOf(unmatched));putMeta(db,"flammability_niosh_npg_generated_utc",root.optString("generated_utc",""));db.setTransactionSuccessful();
        }finally{db.endTransaction();}
        if(lfl<50)throw new IllegalStateException("imported numeric LFL coverage unexpectedly low: "+lfl);
        return new Result(rows.length(),lfl,ufl,unmatched,hash);
    }

    public static boolean needsImport(SQLiteDatabase db,String hash){try(Cursor c=db.rawQuery("SELECT v FROM meta WHERE k='flammability_niosh_npg_sha256'",null)){return !c.moveToFirst()||!hash.equalsIgnoreCase(c.getString(0));}}
    public static String sha256(InputStream in)throws Exception{return sha256(readAll(in));}

    private static void insert(SQLiteDatabase db,long uid,String type,double value,JSONObject r,String rawKey,String url){ContentValues v=new ContentValues();v.put("chemical_uid",uid);v.put("authority","NIOSH");v.put("jurisdiction","US");v.put("limit_type",type);v.put("value",value);v.put("unit","%vol");String raw=clean(r.optString(rawKey,null));StringBuilder note=new StringBuilder("NIOSH NPG ").append(type).append("; gas/vapor flammability reference");if(raw!=null)note.append("; raw=").append(raw);if(url!=null)note.append("; source=").append(url);v.put("notation",note.toString());v.put("source_id",SOURCE_ID);v.put("revision","NIOSH NPG online entry");db.insertOrThrow("exposure_limits",null,v);}
    private static void ensureSource(SQLiteDatabase db,String generated,String hash){ContentValues v=new ContentValues();v.put("source_id",SOURCE_ID);v.put("title","NIOSH Pocket Guide — Lower/Upper Explosive (Flammable) Limits");v.put("authority","NIOSH/CDC");v.put("url","https://www.cdc.gov/niosh/npg/");v.put("evidence_tier","A");v.put("version",generated);v.put("notes","Official NIOSH NPG detail-page LFL/UEL seed. Missing or NA values are never inferred. Seed SHA-256="+hash);db.insertWithOnConflict("scientific_sources",null,v,SQLiteDatabase.CONFLICT_REPLACE);}
    private static Double finitePositive(JSONObject r,String key){if(r==null||r.isNull(key))return null;double x=r.optDouble(key,Double.NaN);return Double.isFinite(x)&&x>0?x:null;}
    private static void putMeta(SQLiteDatabase db,String k,String val){ContentValues v=new ContentValues();v.put("k",k);v.put("v",val==null?"":val);db.insertWithOnConflict("meta",null,v,SQLiteDatabase.CONFLICT_REPLACE);}
    private static String clean(String s){if(s==null)return null;s=s.trim().replaceAll("\\s+"," ");return s.isEmpty()?null:s;}
    private static byte[] readAll(InputStream in)throws Exception{try(InputStream x=in;ByteArrayOutputStream out=new ByteArrayOutputStream(256*1024)){byte[] b=new byte[8192];int n;while((n=x.read(b))!=-1)out.write(b,0,n);return out.toByteArray();}}
    private static String sha256(byte[] data)throws Exception{byte[] h=MessageDigest.getInstance("SHA-256").digest(data);StringBuilder s=new StringBuilder(64);for(byte b:h)s.append(String.format(Locale.US,"%02x",b&255));return s.toString();}
}
