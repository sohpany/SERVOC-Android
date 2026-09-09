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
import java.util.Locale;

/**
 * Imports the optional generated PubChem molecular seed into ChemCore provenance tables.
 * Absence of the asset is not fatal; the application remains operational with its legacy
 * chemical/exposure knowledge and reports molecular intelligence as unavailable.
 */
public final class ChemCoreMolecularSeedImporter {
    private ChemCoreMolecularSeedImporter() {}

    public static final String ASSET = "generated/chemcore/molecular_descriptors.json";
    public static final String EXPECTED_SCHEMA = "chemcore-molecular-seed-v1";
    private static final String META_SHA = "chemcore_molecular_seed_sha256";
    private static final String META_SUMMARY = "chemcore_molecular_seed_summary";
    private static final String SOURCE_ID = "PUBCHEM";

    public static final class Result {
        public final boolean available;
        public final int records, resolved, matched, structures, properties;
        public final String message;
        Result(boolean available,int records,int resolved,int matched,int structures,int properties,String message){
            this.available=available;this.records=records;this.resolved=resolved;this.matched=matched;
            this.structures=structures;this.properties=properties;this.message=message;
        }
    }

    public static Result importIfPresent(Context context, ExperimentsDb db) {
        if(context==null||db==null)return new Result(false,0,0,0,0,0,"context/database required");
        try(InputStream in=context.getAssets().open(ASSET)){
            String raw=read(in);
            String payloadSha=sha256(raw);
            JSONObject root=new JSONObject(raw);
            if(!EXPECTED_SCHEMA.equals(root.optString("schema")))
                return new Result(false,0,0,0,0,0,"unsupported molecular seed schema");

            JSONArray rows=root.optJSONArray("records");
            int recordCount=rows==null?0:rows.length();
            int declaredResolved=root.optInt("resolved_count",0);

            String installed=db.getMeta(META_SHA,"");
            if(payloadSha.equalsIgnoreCase(installed)){
                JSONObject s=parse(db.getMeta(META_SUMMARY,"{}"));
                return new Result(true,recordCount,declaredResolved,
                        s.optInt("matched",0),s.optInt("structures",0),s.optInt("properties",0),
                        "ChemCore molecular seed already imported");
            }

            ensurePubChemSource(db);
            MolecularIntelligenceRepository repo=new MolecularIntelligenceRepository(db);
            if(rows==null)return new Result(true,0,0,0,0,0,"empty molecular seed");

            SQLiteDatabase w=db.getWritableDatabase();
            w.beginTransaction();
            int resolved=0,matched=0,structures=0,properties=0;
            try{
                // Replace only previously generated PubChem evidence. Other sources and manually curated
                // properties are retained. This keeps a refreshed seed reproducible without row duplication.
                w.delete("chemcore_properties","source_id=?",new String[]{SOURCE_ID});
                w.delete("chemcore_descriptor_sets","source_id=? AND descriptor_version=?",
                        new String[]{SOURCE_ID,"pubchem-basic/2"});
                w.delete("chemcore_structures","source_id=?",new String[]{SOURCE_ID});

                for(int i=0;i<rows.length();i++){
                    JSONObject r=rows.optJSONObject(i);
                    if(r==null||!"RESOLVED".equals(r.optString("status")))continue;
                    resolved++;
                    String cas=r.optString("cas","").trim();
                    if(cas.isEmpty())continue;
                    long uid=chemicalUidByCas(db,cas);
                    if(uid<=0)continue;
                    matched++;
                    JSONObject p=r.optJSONObject("pubchem");
                    if(p==null)continue;

                    String connectivity=text(p,"ConnectivitySMILES"),smiles=text(p,"SMILES"),
                            inchi=text(p,"InChI"),inchikey=text(p,"InChIKey");
                    if(nonempty(connectivity)||nonempty(smiles)||nonempty(inchi)||nonempty(inchikey))
                        if(repo.saveStructure(uid,connectivity,smiles,inchi,inchikey,null,SOURCE_ID))structures++;

                    String[] numeric={"MolecularWeight","XLogP","TPSA","Complexity","HBondDonorCount",
                            "HBondAcceptorCount","RotatableBondCount","ExactMass","MonoisotopicMass",
                            "HeavyAtomCount","Charge","CovalentUnitCount","DefinedAtomStereoCount",
                            "UndefinedAtomStereoCount","DefinedBondStereoCount","UndefinedBondStereoCount"};
                    for(String key:numeric){
                        JSONObject q=p.optJSONObject(key);if(q==null)continue;
                        double v=q.optDouble("value",Double.NaN);if(!Double.isFinite(v))continue;
                        String unit=q.optString("unit","");
                        String method=q.optString("method","PubChem PUG REST property endpoint");
                        if(repo.saveProperty(new MolecularProperty(uid,key,v,null,unit,Double.NaN,Double.NaN,
                                method,SOURCE_ID,Double.NaN,"A_SOURCE"))>0)properties++;
                    }

                    String[] textProperties={"MolecularFormula","IUPACName","Title","SMILES","ConnectivitySMILES"};
                    for(String key:textProperties){
                        String value=text(p,key);
                        if(nonempty(value)&&repo.saveProperty(new MolecularProperty(uid,key,Double.NaN,value,"",
                                Double.NaN,Double.NaN,"PubChem PUG REST property endpoint",SOURCE_ID,
                                Double.NaN,"A_SOURCE"))>0)properties++;
                    }

                    String payload=p.toString();
                    repo.saveDescriptorSet(uid,"pubchem-basic/2","PubChem PUG REST property endpoint",
                            SOURCE_ID,payload,sha256(payload),true);
                }

                JSONObject summary=new JSONObject();
                summary.put("records",rows.length());summary.put("resolved",resolved);summary.put("matched",matched);
                summary.put("structures",structures);summary.put("properties",properties);
                db.putMeta(META_SHA,payloadSha);
                db.putMeta(META_SUMMARY,summary.toString());
                w.setTransactionSuccessful();
            } finally {
                w.endTransaction();
            }
            return new Result(true,rows.length(),resolved,matched,structures,properties,
                    "ChemCore molecular seed imported");
        }catch(java.io.FileNotFoundException e){
            return new Result(false,0,0,0,0,0,"molecular seed asset not installed");
        }catch(Exception e){
            db.putMeta("chemcore_molecular_seed_last_error",e.getClass().getSimpleName()+": "+String.valueOf(e.getMessage()));
            return new Result(false,0,0,0,0,0,"molecular seed import failed: "+e.getClass().getSimpleName());
        }
    }

    private static void ensurePubChemSource(ExperimentsDb db){
        ContentValues v=new ContentValues();
        v.put("source_id",SOURCE_ID);v.put("title","PubChem");v.put("authority","NIH/NLM/NCBI");
        v.put("url","https://pubchem.ncbi.nlm.nih.gov/");v.put("evidence_tier","MOLECULAR_REFERENCE");
        v.put("version","PUG REST");v.put("notes","Molecular/structural reference; does not imply MQ detectability or calibration.");
        db.getWritableDatabase().insertWithOnConflict("scientific_sources",null,v,SQLiteDatabase.CONFLICT_IGNORE);
    }

    private static long chemicalUidByCas(ExperimentsDb db,String cas){
        try(Cursor c=db.getReadableDatabase().rawQuery("SELECT uid FROM chemicals WHERE cas=? LIMIT 1",new String[]{cas})){
            return c.moveToFirst()?c.getLong(0):-1;
        }
    }
    private static String text(JSONObject p,String key){JSONObject q=p.optJSONObject(key);return q==null?null:q.optString("value",null);}
    private static boolean nonempty(String s){return s!=null&&!s.trim().isEmpty();}
    private static String read(InputStream in)throws Exception{ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] x=new byte[8192];for(int n;(n=in.read(x))>=0;)b.write(x,0,n);return b.toString(StandardCharsets.UTF_8.name());}
    private static String sha256(String s)throws Exception{byte[] h=MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));StringBuilder x=new StringBuilder();for(byte b:h)x.append(String.format(Locale.US,"%02x",b&255));return x.toString();}
    private static JSONObject parse(String s){try{return new JSONObject(s==null?"{}":s);}catch(Exception e){return new JSONObject();}}
}
