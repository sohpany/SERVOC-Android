package com.servoc.app.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

/** Offline-first SERVOC database. Raw measurements and derived analytics are stored together. */
public final class ExperimentsDb extends SQLiteOpenHelper {
    public static final int DB_VERSION=2;
    public ExperimentsDb(Context c){super(c,"servoc.db",null,DB_VERSION);}

    @Override public void onCreate(SQLiteDatabase db){
        db.execSQL("CREATE TABLE experiments(id INTEGER PRIMARY KEY AUTOINCREMENT, ts TEXT NOT NULL, payload TEXT NOT NULL, protocol TEXT, analytics TEXT)");
        db.execSQL("CREATE TABLE fingerprints(id INTEGER PRIMARY KEY AUTOINCREMENT, chemical_id INTEGER NOT NULL, label TEXT NOT NULL, created_ts TEXT NOT NULL, payload TEXT NOT NULL)");
        db.execSQL("CREATE TABLE meta(k TEXT PRIMARY KEY, v TEXT NOT NULL)");
    }

    @Override public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion){
        if(oldVersion<2){
            try{db.execSQL("ALTER TABLE experiments ADD COLUMN protocol TEXT");}catch(Exception ignored){}
            try{db.execSQL("ALTER TABLE experiments ADD COLUMN analytics TEXT");}catch(Exception ignored){}
            db.execSQL("CREATE TABLE IF NOT EXISTS fingerprints(id INTEGER PRIMARY KEY AUTOINCREMENT, chemical_id INTEGER NOT NULL, label TEXT NOT NULL, created_ts TEXT NOT NULL, payload TEXT NOT NULL)");
            db.execSQL("CREATE TABLE IF NOT EXISTS meta(k TEXT PRIMARY KEY, v TEXT NOT NULL)");
        }
    }

    public long save(String ts,String payload){return save(ts,payload,"","");}
    public long save(String ts,String payload,String protocol,String analytics){
        ContentValues v=new ContentValues();v.put("ts",ts);v.put("payload",payload);v.put("protocol",protocol);v.put("analytics",analytics);
        return getWritableDatabase().insert("experiments",null,v);
    }
    public int count(){try(Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM experiments",null)){return c.moveToFirst()?c.getInt(0):0;}}

    public long saveFingerprint(int chemicalId,String label,String createdTs,String payload){
        ContentValues v=new ContentValues();v.put("chemical_id",chemicalId);v.put("label",label);v.put("created_ts",createdTs);v.put("payload",payload);
        return getWritableDatabase().insert("fingerprints",null,v);
    }
    public int fingerprintCount(){try(Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM fingerprints",null)){return c.moveToFirst()?c.getInt(0):0;}}
    public List<String> fingerprintPayloads(){
        List<String> out=new ArrayList<>();
        try(Cursor c=getReadableDatabase().rawQuery("SELECT payload FROM fingerprints ORDER BY id",null)){while(c.moveToNext())out.add(c.getString(0));}
        return out;
    }
    public void putMeta(String key,String value){
        ContentValues v=new ContentValues();v.put("k",key);v.put("v",value==null?"":value);
        getWritableDatabase().insertWithOnConflict("meta",null,v,SQLiteDatabase.CONFLICT_REPLACE);
    }
    public String getMeta(String key,String fallback){
        try(Cursor c=getReadableDatabase().rawQuery("SELECT v FROM meta WHERE k=?",new String[]{key})){return c.moveToFirst()?c.getString(0):fallback;}
    }
}
