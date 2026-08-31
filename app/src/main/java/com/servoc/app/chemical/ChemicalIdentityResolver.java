package com.servoc.app.chemical;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.Locale;

/** Resolves authoritative chemical identities without guessing ambiguous matches.
 *  CAS is preferred, unique name matching is a fallback, and unresolved authoritative
 *  rows are preserved as new identities with source provenance instead of being dropped.
 */
public final class ChemicalIdentityResolver {
    private ChemicalIdentityResolver(){}

    public static long resolveOrCreate(SQLiteDatabase db,String sourceId,String sourceKey,String recordUrl,String datasetHash,String cas,String name){
        ChemicalIdentitySchema.ensure(db);
        sourceId=clean(sourceId);sourceKey=clean(sourceKey);cas=clean(cas);name=clean(name);
        if(sourceId==null||sourceKey==null||name==null)throw new IllegalArgumentException("source identity and name required");

        Long uid=findBySource(db,sourceId,sourceKey);
        if(uid==null&&cas!=null)uid=uniqueByIdentifier(db,"CAS",cas,name);
        if(uid==null&&cas!=null)uid=uniqueByPrimaryCas(db,cas,name);
        if(uid==null)uid=uniqueByName(db,name);
        if(uid==null)uid=create(db,sourceId,cas,name);
        else enrich(db,uid,sourceId,cas,name);

        linkSource(db,uid,sourceId,sourceKey,recordUrl,datasetHash);
        if(cas!=null)addIdentifier(db,uid,"CAS",cas,sourceId);
        addSynonymIfDifferent(db,uid,name,sourceId);
        return uid;
    }

    private static Long findBySource(SQLiteDatabase db,String sourceId,String sourceKey){
        try(Cursor c=db.rawQuery("SELECT chemical_uid FROM chemical_sources WHERE source_id=? AND source_key=? LIMIT 1",new String[]{sourceId,sourceKey})){return c.moveToFirst()?c.getLong(0):null;}
    }

    private static Long uniqueByIdentifier(SQLiteDatabase db,String scheme,String value,String name){
        try(Cursor c=db.rawQuery("SELECT c.uid,c.name_en FROM chemical_identifiers i JOIN chemicals c ON c.uid=i.chemical_uid WHERE i.scheme=? AND i.value=? AND c.active=1 ORDER BY c.uid",new String[]{scheme,value})){return choose(c,name);}
    }

    private static Long uniqueByPrimaryCas(SQLiteDatabase db,String cas,String name){
        try(Cursor c=db.rawQuery("SELECT uid,name_en FROM chemicals WHERE cas=? AND active=1 ORDER BY uid",new String[]{cas})){return choose(c,name);}
    }

    private static Long uniqueByName(SQLiteDatabase db,String name){
        try(Cursor c=db.rawQuery("SELECT uid,name_en FROM chemicals WHERE lower(trim(name_en))=lower(trim(?)) AND active=1 ORDER BY uid",new String[]{name})){return choose(c,name);}
    }

    private static Long choose(Cursor c,String name){
        Long only=null,exact=null;int count=0,exactCount=0;
        while(c.moveToNext()){
            long uid=c.getLong(0);String n=clean(c.getString(1));count++;only=uid;
            if(name!=null&&n!=null&&n.equalsIgnoreCase(name)){exact=uid;exactCount++;}
        }
        if(count==1)return only;
        return exactCount==1?exact:null;
    }

    private static long create(SQLiteDatabase db,String sourceId,String cas,String name){
        ContentValues v=new ContentValues();if(cas!=null)v.put("cas",cas);v.put("name_en",name);v.put("record_source",sourceId);v.put("active",1);return db.insertOrThrow("chemicals",null,v);
    }

    private static void enrich(SQLiteDatabase db,long uid,String sourceId,String cas,String name){
        ContentValues v=new ContentValues();
        try(Cursor c=db.rawQuery("SELECT cas,record_source,name_en FROM chemicals WHERE uid=?",new String[]{String.valueOf(uid)})){
            if(c.moveToFirst()){
                String oldCas=c.isNull(0)?null:clean(c.getString(0)),sources=c.isNull(1)?null:clean(c.getString(1));
                if(oldCas==null&&cas!=null)v.put("cas",cas);
                if(sources==null)v.put("record_source",sourceId);else if(!containsSource(sources,sourceId))v.put("record_source",sources+" + "+sourceId);
            }
        }
        if(v.size()>0)db.update("chemicals",v,"uid=?",new String[]{String.valueOf(uid)});
    }

    private static void addSynonymIfDifferent(SQLiteDatabase db,long uid,String name,String sourceId){
        String canonical=null;try(Cursor c=db.rawQuery("SELECT name_en FROM chemicals WHERE uid=?",new String[]{String.valueOf(uid)})){if(c.moveToFirst())canonical=clean(c.getString(0));}
        if(canonical==null||canonical.equalsIgnoreCase(name))return;
        ContentValues v=new ContentValues();v.put("chemical_uid",uid);v.put("synonym",name);v.put("language","en");v.put("source_id",sourceId);
        try{db.insert("chemical_synonyms",null,v);}catch(Exception ignored){}
    }

    private static void linkSource(SQLiteDatabase db,long uid,String sourceId,String sourceKey,String recordUrl,String datasetHash){
        ContentValues v=new ContentValues();v.put("chemical_uid",uid);v.put("source_id",sourceId);v.put("source_key",sourceKey);if(recordUrl!=null)v.put("record_url",recordUrl);if(datasetHash!=null)v.put("dataset_hash",datasetHash);
        db.insertWithOnConflict("chemical_sources",null,v,SQLiteDatabase.CONFLICT_REPLACE);
    }

    private static void addIdentifier(SQLiteDatabase db,long uid,String scheme,String value,String sourceId){
        ContentValues v=new ContentValues();v.put("chemical_uid",uid);v.put("scheme",scheme);v.put("value",value);v.put("source_id",sourceId);
        db.insertWithOnConflict("chemical_identifiers",null,v,SQLiteDatabase.CONFLICT_IGNORE);
    }

    private static boolean containsSource(String list,String source){for(String x:list.split("\\s*\\+\\s*"))if(source.equals(x.trim()))return true;return false;}
    public static String stableSourceKey(String cas,String name){String c=clean(cas);if(c!=null)return c;String n=clean(name);return n==null?"unknown":n.toLowerCase(Locale.US).replaceAll("[^a-z0-9]+","-").replaceAll("(^-|-$)","");}
    private static String clean(String s){if(s==null)return null;s=s.trim().replaceAll("\\s+"," ");return s.isEmpty()?null:s;}
}
