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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Runtime curation store for occupational identities that cannot safely be represented by one
 * PubChem molecule. This is a negative/eligibility evidence layer: it can forbid molecular QSPR,
 * but never promotes an entry into a molecular model by itself.
 */
public final class ChemCoreIdentityCurationRepository {
    public static final String ASSET = "generated/chemcore/unresolved_curation.json";
    public static final String SCHEMA = "chemcore-unresolved-curation-v1";
    private static final String META_SHA = "chemcore_unresolved_curation_sha256";
    private final ExperimentsDb db;

    public static final class Entry {
        public final String cas,nameEn,entityType,resolutionPolicy,note;
        public final boolean molecularQsprEligible;
        Entry(String cas,String nameEn,String entityType,boolean eligible,String resolutionPolicy,String note){
            this.cas=cas==null?"":cas;this.nameEn=nameEn==null?"":nameEn;this.entityType=entityType==null?"":entityType;
            this.molecularQsprEligible=eligible;this.resolutionPolicy=resolutionPolicy==null?"":resolutionPolicy;this.note=note==null?"":note;
        }
    }

    public static final class ImportResult {
        public final boolean available;
        public final int records;
        public final String sha256,message;
        ImportResult(boolean available,int records,String sha256,String message){this.available=available;this.records=records;this.sha256=sha256==null?"":sha256;this.message=message==null?"":message;}
    }

    public ChemCoreIdentityCurationRepository(ExperimentsDb db){
        if(db==null)throw new IllegalArgumentException("database required");this.db=db;ensureSchema(db.getWritableDatabase());
    }

    private static void ensureSchema(SQLiteDatabase w){
        w.execSQL("CREATE TABLE IF NOT EXISTS chemcore_identity_curation("+
                "cas TEXT NOT NULL,"+
                "name_en TEXT NOT NULL,"+
                "entity_type TEXT NOT NULL,"+
                "molecular_qspr_eligible INTEGER NOT NULL DEFAULT 0,"+
                "resolution_policy TEXT NOT NULL,"+
                "note TEXT,"+
                "source_seed_sha256 TEXT NOT NULL,"+
                "source_asset_sha256 TEXT NOT NULL,"+
                "imported_ts TEXT NOT NULL,"+
                "PRIMARY KEY(cas,name_en))");
        w.execSQL("CREATE INDEX IF NOT EXISTS idx_chemcore_curation_type ON chemcore_identity_curation(entity_type,molecular_qspr_eligible)");
    }

    public static ImportResult importIfPresent(Context context,ExperimentsDb db){
        if(context==null||db==null)return new ImportResult(false,0,"","context/database required");
        try(InputStream in=context.getAssets().open(ASSET)){
            String raw=read(in);String assetSha=sha256(raw);JSONObject root=new JSONObject(raw);
            if(!SCHEMA.equals(root.optString("schema")))return new ImportResult(false,0,assetSha,"unsupported curation schema");
            JSONArray rows=root.optJSONArray("records");int declared=root.optInt("record_count",-1);
            if(rows==null||declared!=rows.length()||declared!=37)return new ImportResult(false,0,assetSha,"curation coverage is not the expected 37 identities");
            String seedSha=root.optString("source_seed_sha256","").trim().toLowerCase(Locale.US);
            if(!seedSha.matches("[0-9a-f]{64}"))return new ImportResult(false,0,assetSha,"invalid source seed digest");
            for(int i=0;i<rows.length();i++){
                JSONObject r=rows.optJSONObject(i);
                if(r==null||r.optBoolean("molecular_qspr_eligible",true))return new ImportResult(false,0,assetSha,"curation attempted to promote molecular QSPR eligibility");
                String kind=r.optString("entity_type","");
                if(!allowed(kind))return new ImportResult(false,0,assetSha,"unsupported curated entity type: "+kind);
            }
            if(assetSha.equalsIgnoreCase(db.getMeta(META_SHA,"")))return new ImportResult(true,declared,assetSha,"curation already imported");
            ChemCoreIdentityCurationRepository repo=new ChemCoreIdentityCurationRepository(db);SQLiteDatabase w=db.getWritableDatabase();
            w.beginTransaction();try{
                w.delete("chemcore_identity_curation",null,null);
                String imported=isoNow();
                for(int i=0;i<rows.length();i++){
                    JSONObject r=rows.getJSONObject(i);ContentValues v=new ContentValues();
                    v.put("cas",r.optString("cas",""));v.put("name_en",r.optString("name_en",""));v.put("entity_type",r.optString("entity_type",""));
                    v.put("molecular_qspr_eligible",0);v.put("resolution_policy",r.optString("resolution_policy",""));v.put("note",r.optString("note",""));
                    v.put("source_seed_sha256",seedSha);v.put("source_asset_sha256",assetSha);v.put("imported_ts",imported);
                    w.insertOrThrow("chemcore_identity_curation",null,v);
                }
                db.putMeta(META_SHA,assetSha);db.putMeta("chemcore_unresolved_curation_count",String.valueOf(declared));w.setTransactionSuccessful();
            }finally{w.endTransaction();}
            return new ImportResult(true,declared,assetSha,"curation imported");
        }catch(java.io.FileNotFoundException e){return new ImportResult(false,0,"","curation asset not installed");}
        catch(Exception e){db.putMeta("chemcore_unresolved_curation_last_error",e.getClass().getSimpleName()+": "+String.valueOf(e.getMessage()));return new ImportResult(false,0,"","curation import failed: "+e.getClass().getSimpleName());}
    }

    public Entry find(String cas,String nameEn){
        try(Cursor c=db.getReadableDatabase().rawQuery("SELECT cas,name_en,entity_type,molecular_qspr_eligible,resolution_policy,note FROM chemcore_identity_curation WHERE cas=? AND name_en=? LIMIT 1",new String[]{nz(cas),nz(nameEn)})){
            return c.moveToFirst()?new Entry(c.getString(0),c.getString(1),c.getString(2),c.getInt(3)!=0,c.getString(4),c.getString(5)):null;
        }
    }
    public boolean molecularQsprEligible(String cas,String nameEn){Entry e=find(cas,nameEn);return e==null||e.molecularQsprEligible;}
    public int count(){try(Cursor c=db.getReadableDatabase().rawQuery("SELECT COUNT(*) FROM chemcore_identity_curation",null)){return c.moveToFirst()?c.getInt(0):0;}}

    private static boolean allowed(String x){return "PURE_COMPOUND".equals(x)||"MIXTURE".equals(x)||"MATERIAL".equals(x)||"ISOMER_GROUP".equals(x)||"OCCUPATIONAL_CATEGORY".equals(x);}
    private static String nz(String s){return s==null?"":s;}
    private static String read(InputStream in)throws Exception{ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] x=new byte[8192];for(int n;(n=in.read(x))>=0;)if(n>0)b.write(x,0,n);return b.toString(StandardCharsets.UTF_8.name());}
    private static String sha256(String s)throws Exception{byte[] h=MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));StringBuilder x=new StringBuilder();for(byte b:h)x.append(String.format(Locale.US,"%02x",b&255));return x.toString();}
    private static String isoNow(){SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'",Locale.US);f.setTimeZone(TimeZone.getTimeZone("UTC"));return f.format(new Date());}
}
