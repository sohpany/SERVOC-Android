package com.servoc.app.exposure;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

/** Imports authority-specific exposure seeds without flattening or cross-authority merging. */
public final class ExposureSeedImporter {
    private ExposureSeedImporter(){}

    public static final class Result {
        public final int sourceRecords,inserted,unmatched;public final String hash;
        Result(int sourceRecords,int inserted,int unmatched,String hash){this.sourceRecords=sourceRecords;this.inserted=inserted;this.unmatched=unmatched;this.hash=hash;}
    }

    public static Result importNioshIdlh(SQLiteDatabase db,InputStream input)throws Exception{
        byte[] bytes=readAll(input);String hash=sha256(bytes);JSONObject root=new JSONObject(new String(bytes,StandardCharsets.UTF_8));
        if(!"hazrob.exposure-seed.v1".equals(root.optString("schema")))throw new IllegalArgumentException("unsupported exposure seed schema");
        if(!"NIOSH".equals(root.optString("authority"))||!"IDLH".equals(root.optString("dataset")))throw new IllegalArgumentException("not a NIOSH IDLH seed");
        JSONArray rows=root.getJSONArray("records");if(rows.length()<300||root.optInt("record_count",-1)!=rows.length())throw new IllegalArgumentException("incomplete IDLH seed");
        int inserted=0,unmatched=0;db.beginTransaction();try{
            ensureSource(db);db.delete("exposure_limits","source_id=?",new String[]{"NIOSH_IDLH"});
            for(int i=0;i<rows.length();i++){
                JSONObject r=rows.getJSONObject(i);String cas=clean(r.optString("cas",null)),name=clean(r.optString("name_en",null));Long uid=cas==null?null:findByCas(db,cas);if(uid==null&&name!=null)uid=findByName(db,name);if(uid==null){unmatched++;continue;}
                ContentValues v=new ContentValues();v.put("chemical_uid",uid);v.put("authority","NIOSH");v.put("jurisdiction","US");v.put("limit_type","IDLH");
                if(!r.isNull("value"))v.put("value",r.getDouble("value"));if(!r.isNull("unit"))v.put("unit",r.getString("unit"));if(!r.isNull("duration_min"))v.put("duration_min",r.getDouble("duration_min"));
                String notation=clean(r.optString("notation",null));if(r.optBoolean("current_policy",false))notation="CURRENT POLICY | "+(notation==null?"":notation);if(notation!=null)v.put("notation",notation);
                v.put("source_id","NIOSH_IDLH");v.put("revision",r.optString("revision",""));if(db.insertOrThrow("exposure_limits",null,v)>0)inserted++;
            }
            putMeta(db,"exposure_niosh_idlh_sha256",hash);putMeta(db,"exposure_niosh_idlh_source_sha256",root.optString("source_sha256",""));putMeta(db,"exposure_niosh_idlh_records",String.valueOf(rows.length()));putMeta(db,"exposure_niosh_idlh_inserted",String.valueOf(inserted));putMeta(db,"exposure_niosh_idlh_unmatched",String.valueOf(unmatched));putMeta(db,"exposure_niosh_idlh_generated_utc",root.optString("generated_utc",""));db.setTransactionSuccessful();
        }finally{db.endTransaction();}
        return new Result(rows.length(),inserted,unmatched,hash);
    }

    public static boolean needsImport(SQLiteDatabase db,String metaKey,String hash){try(Cursor c=db.rawQuery("SELECT v FROM meta WHERE k=?",new String[]{metaKey})){return !c.moveToFirst()||!hash.equalsIgnoreCase(c.getString(0));}}
    public static String sha256(InputStream in)throws Exception{return sha256(readAll(in));}

    private static void ensureSource(SQLiteDatabase db){ContentValues v=new ContentValues();v.put("source_id","NIOSH_IDLH");v.put("title","NIOSH Immediately Dangerous to Life or Health (IDLH) Values");v.put("authority","NIOSH/CDC");v.put("url","https://www.cdc.gov/niosh/idlh/intridl4.html");v.put("evidence_tier","A");v.put("version","current online table + historical 1994 values");v.put("notes","Where both values exist, CIB 66/newer value is NIOSH current policy; historical value remains retained separately.");db.insertWithOnConflict("scientific_sources",null,v,SQLiteDatabase.CONFLICT_REPLACE);}
    private static Long findByCas(SQLiteDatabase db,String cas){try(Cursor c=db.rawQuery("SELECT uid FROM chemicals WHERE cas=? AND active=1 LIMIT 1",new String[]{cas})){return c.moveToFirst()?c.getLong(0):null;}}
    private static Long findByName(SQLiteDatabase db,String name){try(Cursor c=db.rawQuery("SELECT uid FROM chemicals WHERE lower(name_en)=lower(?) AND active=1 LIMIT 1",new String[]{name})){return c.moveToFirst()?c.getLong(0):null;}}
    private static void putMeta(SQLiteDatabase db,String k,String val){ContentValues v=new ContentValues();v.put("k",k);v.put("v",val==null?"":val);db.insertWithOnConflict("meta",null,v,SQLiteDatabase.CONFLICT_REPLACE);}
    private static String clean(String s){if(s==null)return null;s=s.trim().replaceAll("\\s+"," ");return s.isEmpty()?null:s;}
    private static byte[] readAll(InputStream in)throws Exception{try(InputStream x=in;ByteArrayOutputStream out=new ByteArrayOutputStream(256*1024)){byte[] b=new byte[8192];int n;while((n=x.read(b))!=-1)out.write(b,0,n);return out.toByteArray();}}
    private static String sha256(byte[] data)throws Exception{byte[] h=MessageDigest.getInstance("SHA-256").digest(data);StringBuilder s=new StringBuilder(64);for(byte b:h)s.append(String.format(Locale.US,"%02x",b&255));return s.toString();}
}
