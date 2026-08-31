package com.servoc.app.chemical;

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

/** Imports provenance-preserving offline chemical identity seeds into the v6 knowledge database. */
public final class ChemicalSeedImporter {
    private ChemicalSeedImporter(){}

    public static final class Result {
        public final int sourceRecords,inserted,updated,skipped;public final String datasetHash;
        Result(int sourceRecords,int inserted,int updated,int skipped,String datasetHash){this.sourceRecords=sourceRecords;this.inserted=inserted;this.updated=updated;this.skipped=skipped;this.datasetHash=datasetHash;}
    }

    public static Result importNioshIdentity(SQLiteDatabase db,InputStream input)throws Exception{
        byte[] bytes=readAll(input);String hash=sha256(bytes);JSONObject root=new JSONObject(new String(bytes,StandardCharsets.UTF_8));
        if(!"hazrob.chemical-seed.v1".equals(root.optString("schema")))throw new IllegalArgumentException("unsupported chemical seed schema");
        JSONArray rows=root.getJSONArray("records");int declared=root.optInt("record_count",-1);if(declared!=rows.length()||rows.length()<500)throw new IllegalArgumentException("incomplete NIOSH seed");
        int inserted=0,updated=0,skipped=0;db.beginTransaction();try{
            for(int i=0;i<rows.length();i++){
                JSONObject r=rows.getJSONObject(i);String cas=clean(r.optString("cas",null)),name=clean(r.optString("name_en",null));if(cas==null||name==null){skipped++;continue;}
                Long uid=findByCas(db,cas);if(uid==null)uid=findByName(db,name);
                ContentValues v=new ContentValues();v.put("cas",cas);v.put("name_en",name);v.put("record_source","NIOSH_NPG");v.put("active",1);
                if(uid==null){long x=db.insert("chemicals",null,v);if(x>0)inserted++;else skipped++;}
                else{db.update("chemicals",v,"uid=?",new String[]{String.valueOf(uid)});updated++;}
            }
            putMeta(db,"chemical_seed_niosh_sha256",hash);putMeta(db,"chemical_seed_niosh_records",String.valueOf(rows.length()));putMeta(db,"chemical_seed_niosh_source_sha256",root.optString("source_sha256",""));putMeta(db,"chemical_seed_niosh_generated_utc",root.optString("generated_utc",""));db.setTransactionSuccessful();
        }finally{db.endTransaction();}
        return new Result(rows.length(),inserted,updated,skipped,hash);
    }

    public static boolean needsImport(SQLiteDatabase db,String hash){
        try(Cursor c=db.rawQuery("SELECT v FROM meta WHERE k='chemical_seed_niosh_sha256'",null)){return !c.moveToFirst()||!hash.equalsIgnoreCase(c.getString(0));}
    }

    public static String sha256(InputStream in)throws Exception{return sha256(readAll(in));}

    private static Long findByCas(SQLiteDatabase db,String cas){try(Cursor c=db.rawQuery("SELECT uid FROM chemicals WHERE cas=? LIMIT 1",new String[]{cas})){return c.moveToFirst()?c.getLong(0):null;}}
    private static Long findByName(SQLiteDatabase db,String name){try(Cursor c=db.rawQuery("SELECT uid FROM chemicals WHERE lower(name_en)=lower(?) LIMIT 1",new String[]{name})){return c.moveToFirst()?c.getLong(0):null;}}
    private static void putMeta(SQLiteDatabase db,String k,String val){ContentValues v=new ContentValues();v.put("k",k);v.put("v",val==null?"":val);db.insertWithOnConflict("meta",null,v,SQLiteDatabase.CONFLICT_REPLACE);}
    private static String clean(String s){if(s==null)return null;s=s.trim().replaceAll("\\s+"," ");return s.isEmpty()?null:s;}
    private static byte[] readAll(InputStream in)throws Exception{try(InputStream x=in;ByteArrayOutputStream out=new ByteArrayOutputStream(256*1024)){byte[] b=new byte[8192];int n;while((n=x.read(b))!=-1)out.write(b,0,n);return out.toByteArray();}}
    private static String sha256(byte[] data)throws Exception{byte[] h=MessageDigest.getInstance("SHA-256").digest(data);StringBuilder s=new StringBuilder(64);for(byte b:h)s.append(String.format(Locale.US,"%02x",b&255));return s.toString();}
}
