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
    private static final String IMPORTER_VERSION="2";
    private ChemicalSeedImporter(){}

    public static final class Result {
        public final int sourceRecords,inserted,updated,skipped;public final String datasetHash;
        Result(int sourceRecords,int inserted,int updated,int skipped,String datasetHash){this.sourceRecords=sourceRecords;this.inserted=inserted;this.updated=updated;this.skipped=skipped;this.datasetHash=datasetHash;}
    }

    public static Result importNioshIdentity(SQLiteDatabase db,InputStream input)throws Exception{
        ChemicalIdentitySchema.ensure(db);
        byte[] bytes=readAll(input);String hash=sha256(bytes);JSONObject root=new JSONObject(new String(bytes,StandardCharsets.UTF_8));
        if(!"hazrob.chemical-seed.v1".equals(root.optString("schema")))throw new IllegalArgumentException("unsupported chemical seed schema");
        JSONObject provenance=root.optJSONObject("provenance");
        if(provenance==null||!provenance.optBoolean("authoritative",false)||!"NIOSH".equals(provenance.optString("authority")))throw new IllegalArgumentException("NIOSH provenance missing");
        JSONArray rows=root.getJSONArray("records");int declared=root.optInt("record_count",-1);if(declared!=rows.length()||rows.length()<500)throw new IllegalArgumentException("incomplete NIOSH seed");
        int inserted=0,updated=0,skipped=0;db.beginTransaction();try{
            for(int i=0;i<rows.length();i++){
                JSONObject r=rows.getJSONObject(i);String cas=clean(r.optString("cas",null)),name=clean(r.optString("name_en",null)),sourceId=clean(r.optString("source_id","NIOSH_NPG")),sourceUrl=clean(r.optString("source_url",null));
                if(cas==null||name==null||sourceId==null||sourceUrl==null||!r.optBoolean("authoritative",false)){skipped++;continue;}
                String sourceKey=sourceKey(sourceUrl);Long uid=findBySource(db,sourceId,sourceKey);boolean created=false;
                if(uid==null)uid=findByName(db,name);
                if(uid==null){ContentValues v=new ContentValues();v.put("cas",cas);v.put("name_en",name);v.put("record_source",sourceId);v.put("active",1);long x=db.insertOrThrow("chemicals",null,v);uid=x;inserted++;created=true;}
                else{
                    ContentValues v=new ContentValues();v.put("active",1);
                    String existingCas=stringValue(db,"SELECT cas FROM chemicals WHERE uid=?",uid);if(existingCas==null)v.put("cas",cas);
                    String oldSource=stringValue(db,"SELECT record_source FROM chemicals WHERE uid=?",uid);if(oldSource==null||oldSource.isEmpty())v.put("record_source",sourceId);else if(!containsSource(oldSource,sourceId))v.put("record_source",oldSource+" + "+sourceId);
                    if(findBySource(db,sourceId,sourceKey)!=null)v.put("name_en",name);
                    db.update("chemicals",v,"uid=?",new String[]{String.valueOf(uid)});updated++;
                }
                linkSource(db,uid,sourceId,sourceKey,sourceUrl,hash);addIdentifier(db,uid,"CAS",cas,sourceId);
                if(created){} // keeps inserted/updated accounting explicit for audit output
            }
            putMeta(db,"chemical_seed_niosh_sha256",hash);putMeta(db,"chemical_seed_niosh_records",String.valueOf(rows.length()));putMeta(db,"chemical_seed_niosh_source_sha256",root.optString("source_sha256",""));putMeta(db,"chemical_seed_niosh_generated_utc",root.optString("generated_utc",""));putMeta(db,"chemical_seed_niosh_importer_version",IMPORTER_VERSION);db.setTransactionSuccessful();
        }finally{db.endTransaction();}
        return new Result(rows.length(),inserted,updated,skipped,hash);
    }

    public static boolean needsImport(SQLiteDatabase db,String hash){
        ChemicalIdentitySchema.ensure(db);String saved=null,version=null;
        try(Cursor c=db.rawQuery("SELECT k,v FROM meta WHERE k IN ('chemical_seed_niosh_sha256','chemical_seed_niosh_importer_version')",null)){while(c.moveToNext()){if("chemical_seed_niosh_sha256".equals(c.getString(0)))saved=c.getString(1);else version=c.getString(1);}}
        return !hash.equalsIgnoreCase(saved==null?"":saved)||!IMPORTER_VERSION.equals(version);
    }

    public static String sha256(InputStream in)throws Exception{return sha256(readAll(in));}

    private static Long findBySource(SQLiteDatabase db,String sourceId,String sourceKey){try(Cursor c=db.rawQuery("SELECT chemical_uid FROM chemical_sources WHERE source_id=? AND source_key=? LIMIT 1",new String[]{sourceId,sourceKey})){return c.moveToFirst()?c.getLong(0):null;}}
    private static Long findByName(SQLiteDatabase db,String name){try(Cursor c=db.rawQuery("SELECT uid FROM chemicals WHERE lower(trim(name_en))=lower(trim(?)) AND active=1 LIMIT 1",new String[]{name})){return c.moveToFirst()?c.getLong(0):null;}}
    private static String stringValue(SQLiteDatabase db,String sql,long uid){try(Cursor c=db.rawQuery(sql,new String[]{String.valueOf(uid)})){if(!c.moveToFirst()||c.isNull(0))return null;String s=clean(c.getString(0));return s;}}
    private static boolean containsSource(String list,String source){for(String x:list.split("\\s*\\+\\s*"))if(source.equals(x.trim()))return true;return false;}
    private static void linkSource(SQLiteDatabase db,long uid,String sourceId,String sourceKey,String url,String hash){ContentValues v=new ContentValues();v.put("chemical_uid",uid);v.put("source_id",sourceId);v.put("source_key",sourceKey);v.put("record_url",url);v.put("dataset_hash",hash);db.insertWithOnConflict("chemical_sources",null,v,SQLiteDatabase.CONFLICT_REPLACE);}
    private static void addIdentifier(SQLiteDatabase db,long uid,String scheme,String value,String sourceId){ContentValues v=new ContentValues();v.put("chemical_uid",uid);v.put("scheme",scheme);v.put("value",value);v.put("source_id",sourceId);db.insertWithOnConflict("chemical_identifiers",null,v,SQLiteDatabase.CONFLICT_IGNORE);}
    private static String sourceKey(String url){String s=url.trim();int q=s.indexOf('?');if(q>=0)s=s.substring(0,q);int slash=s.lastIndexOf('/');String key=slash>=0?s.substring(slash+1):s;return key.toLowerCase(Locale.US);}
    private static void putMeta(SQLiteDatabase db,String k,String val){ContentValues v=new ContentValues();v.put("k",k);v.put("v",val==null?"":val);db.insertWithOnConflict("meta",null,v,SQLiteDatabase.CONFLICT_REPLACE);}
    private static String clean(String s){if(s==null)return null;s=s.trim().replaceAll("\\s+"," ");return s.isEmpty()?null:s;}
    private static byte[] readAll(InputStream in)throws Exception{try(InputStream x=in;ByteArrayOutputStream out=new ByteArrayOutputStream(256*1024)){byte[] b=new byte[8192];int n;while((n=x.read(b))!=-1)out.write(b,0,n);return out.toByteArray();}}
    private static String sha256(byte[] data)throws Exception{byte[] h=MessageDigest.getInstance("SHA-256").digest(data);StringBuilder s=new StringBuilder(64);for(byte b:h)s.append(String.format(Locale.US,"%02x",b&255));return s.toString();}
}
