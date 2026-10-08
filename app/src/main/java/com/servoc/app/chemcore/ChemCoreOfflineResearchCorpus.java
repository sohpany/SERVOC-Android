package com.servoc.app.chemcore;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.servoc.app.data.ExperimentsDb;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Verified, reproducible offline mirror of the current HR069 scientific research corpus.
 * Each imported record is NON-M4 external reference evidence and never becomes a
 * calibration curve, accepted identity, Stable snapshot or user laboratory capture.
 * Import is atomic, idempotent, and isolated from all user Field Sessions.
 */
public final class ChemCoreOfflineResearchCorpus {
    public static final String MANIFEST_ASSET="generated/chemcore/hr069_offline/manifest.json";
    public static final String ASSET_DIR="generated/chemcore/hr069_offline/";
    public static final String SCHEMA="hazrob-chemcore-offline-research-corpus/1";
    public static final String META_SHA="chemcore_hr069_offline_corpus_sha256";
    private static final int SOURCE_COUNT=14;
    private static final int MAX_SOURCE_BYTES=512_000;
    private static final int MAX_MANIFEST_BYTES=30_000;

    public static final class Result {
        public final boolean available,changed;
        public final int imported;
        public final String sha256,reason;
        Result(boolean available,boolean changed,int imported,String sha,String reason){
            this.available=available;this.changed=changed;this.imported=imported;
            this.sha256=sha==null?"":sha;this.reason=reason==null?"":reason;
        }
    }
    private static final class Source {
        final String name,schema,sha,raw;
        Source(String name,String schema,String sha,String raw){this.name=name;this.schema=schema;this.sha=sha;this.raw=raw;}
    }
    private ChemCoreOfflineResearchCorpus(){}

    private static void ensureTable(SQLiteDatabase w){
        w.execSQL("CREATE TABLE IF NOT EXISTS chemcore_offline_research_sources("+
                "filename TEXT PRIMARY KEY,source_schema TEXT NOT NULL,"+
                "sha256 TEXT NOT NULL,raw_json TEXT NOT NULL,"+
                "source_release TEXT NOT NULL,evidence_scope TEXT NOT NULL)");
    }

    public static Result importIfPresent(Context context,ExperimentsDb db){
        if(context==null||db==null)return new Result(false,false,0,"","context/database required");
        try{
            byte[] manifestBytes=read(context,MANIFEST_ASSET,MAX_MANIFEST_BYTES);
            JSONObject manifest=new JSONObject(new String(manifestBytes,StandardCharsets.UTF_8));
            if(!SCHEMA.equals(manifest.optString("schema"))
                    ||!"0.10.2".equals(manifest.optString("release"))
                    ||!"HR069".equals(manifest.optString("hardware_revision"))
                    ||manifest.optBoolean("m4_promotion",true)
                    ||manifest.optBoolean("runtime_calibration_override",true)
                    ||!"sohpany/SERVOC-Android".equals(manifest.optString("authoritative_repository"))
                    ||!"release/hazrob-0.10.2-rc2".equals(manifest.optString("authoritative_branch"))
                    ||!"research_data/hr069_analytical_library".equals(manifest.optString("authoritative_root")))
                throw new IllegalArgumentException("HR069 corpus provenance or evidence policy mismatch");
            JSONArray entries=manifest.optJSONArray("sources");
            if(entries==null||entries.length()!=SOURCE_COUNT||manifest.optInt("source_count")!=SOURCE_COUNT)
                throw new IllegalArgumentException("HR069 scientific source count mismatch");
            List<Source> checked=new ArrayList<>();
            java.util.HashSet<String> seen=new java.util.HashSet<>();
            for(int i=0;i<entries.length();i++){
                JSONObject item=entries.optJSONObject(i);
                if(item==null)throw new IllegalArgumentException("missing source row");
                String name=item.optString("name","");
                if(!name.matches("[a-z0-9_]+\\.json")||"manifest.json".equals(name)||!seen.add(name))
                    throw new IllegalArgumentException("unsafe or duplicate scientific source filename");
                byte[] bytes=read(context,ASSET_DIR+name,MAX_SOURCE_BYTES);
                String actual=sha256(bytes);
                if(bytes.length!=item.optInt("bytes")||!actual.equalsIgnoreCase(item.optString("sha256","")))
                    throw new IllegalArgumentException("SHA/size mismatch: "+name);
                String raw=new String(bytes,StandardCharsets.UTF_8);
                JSONObject json=new JSONObject(raw);
                String schema=json.optString("schema","");
                if(schema.isEmpty()||!schema.equals(item.optString("source_schema","")))
                    throw new IllegalArgumentException("scientific source schema mismatch: "+name);
                checked.add(new Source(name,schema,actual,raw));
            }
            String manifestSha=sha256(manifestBytes);
            SQLiteDatabase w=db.getWritableDatabase();
            ensureTable(w);
            try(Cursor c=w.rawQuery("SELECT COUNT(*) FROM chemcore_offline_research_sources",null)){
                if(c.moveToFirst()&&c.getInt(0)==SOURCE_COUNT
                        &&manifestSha.equalsIgnoreCase(db.getMeta(META_SHA,"")))
                    return new Result(true,false,SOURCE_COUNT,manifestSha,"already current");
            }

            w.beginTransaction();
            try{
                w.delete("chemcore_offline_research_sources",null,null);
                for(Source item:checked){
                    ContentValues v=new ContentValues();
                    v.put("filename",item.name);v.put("source_schema",item.schema);
                    v.put("sha256",item.sha);v.put("raw_json",item.raw);
                    v.put("source_release","0.10.2/HR069");
                    v.put("evidence_scope","EXTERNAL_RESEARCH_NON_M4");
                    w.insertOrThrow("chemcore_offline_research_sources",null,v);
                }
                w.setTransactionSuccessful();
            }finally{w.endTransaction();}
            db.putMeta(META_SHA,manifestSha);
            return new Result(true,true,checked.size(),manifestSha,"HR069 research provenance imported");
        }catch(Exception e){
            return new Result(false,false,0,"",e.getClass().getSimpleName()+": "+String.valueOf(e.getMessage()));
        }
    }

    private static byte[] read(Context context,String asset,int limit)throws Exception{
        try(InputStream in=context.getAssets().open(asset)){
            ByteArrayOutputStream out=new ByteArrayOutputStream();
            byte[] buffer=new byte[8192];
            for(int n;(n=in.read(buffer))!=-1;){
                if(n==0)continue;
                if(out.size()+n>limit)throw new IllegalArgumentException("oversized HR069 asset");
                out.write(buffer,0,n);
            }
            return out.toByteArray();
        }
    }
    private static String sha256(byte[] bytes)throws Exception{
        byte[] digest=MessageDigest.getInstance("SHA-256").digest(bytes);
        StringBuilder b=new StringBuilder(64);
        for(byte x:digest)b.append(String.format(Locale.US,"%02x",x&255));
        return b.toString();
    }
}
