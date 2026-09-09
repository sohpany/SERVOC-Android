package com.servoc.app.chemcore;

import android.content.Context;
import android.database.Cursor;
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
    public static final String ASSET="generated/chemcore/molecular_descriptors.json";
    public static final String EXPECTED_SCHEMA="chemcore-molecular-seed-v1";

    public static final class Result {
        public final boolean available;public final int records,resolved,matched,structures,properties;public final String message;
        Result(boolean available,int records,int resolved,int matched,int structures,int properties,String message){this.available=available;this.records=records;this.resolved=resolved;this.matched=matched;this.structures=structures;this.properties=properties;this.message=message;}
    }

    public static Result importIfPresent(Context context,ExperimentsDb db){
        if(context==null||db==null)return new Result(false,0,0,0,0,0,"context/database required");
        try(InputStream in=context.getAssets().open(ASSET)){
            String raw=read(in);JSONObject root=new JSONObject(raw);if(!EXPECTED_SCHEMA.equals(root.optString("schema")))return new Result(false,0,0,0,0,0,"unsupported molecular seed schema");
            MolecularIntelligenceRepository repo=new MolecularIntelligenceRepository(db);JSONArray rows=root.optJSONArray("records");if(rows==null)return new Result(true,0,0,0,0,0,"empty molecular seed");
            int resolved=0,matched=0,structures=0,properties=0;
            for(int i=0;i<rows.length();i++){
                JSONObject r=rows.optJSONObject(i);if(r==null||!"RESOLVED".equals(r.optString("status")))continue;resolved++;String cas=r.optString("cas","").trim();if(cas.isEmpty())continue;long uid=chemicalUidByCas(db,cas);if(uid<=0)continue;matched++;
                JSONObject p=r.optJSONObject("pubchem");if(p==null)continue;
                String canonical=text(p,"CanonicalSMILES"),isomeric=text(p,"IsomericSMILES"),inchi=text(p,"InChI"),inchikey=text(p,"InChIKey");
                if(nonempty(canonical)||nonempty(isomeric)||nonempty(inchi)||nonempty(inchikey))if(repo.saveStructure(uid,canonical,isomeric,inchi,inchikey,null,"PUBCHEM"))structures++;
                String[] numeric={"MolecularWeight","XLogP","TPSA","Complexity","HBondDonorCount","HBondAcceptorCount","RotatableBondCount","ExactMass","MonoisotopicMass","HeavyAtomCount","FormalCharge","CovalentUnitCount","DefinedAtomStereoCount","UndefinedAtomStereoCount","DefinedBondStereoCount","UndefinedBondStereoCount"};
                for(String key:numeric){JSONObject q=p.optJSONObject(key);if(q==null)continue;double v=q.optDouble("value",Double.NaN);if(!Double.isFinite(v))continue;String unit=q.optString("unit","");String method=q.optString("method","PubChem PUG REST property endpoint");if(repo.saveProperty(new MolecularProperty(uid,key,v,null,unit,Double.NaN,Double.NaN,method,"PUBCHEM",Double.NaN,"A_SOURCE"))>0)properties++;}
                String formula=text(p,"MolecularFormula");if(nonempty(formula))if(repo.saveProperty(new MolecularProperty(uid,"MolecularFormula",Double.NaN,formula,"",Double.NaN,Double.NaN,"PubChem PUG REST property endpoint","PUBCHEM",Double.NaN,"A_SOURCE"))>0)properties++;
                // Store the raw PubChem molecular record as a versioned descriptor artifact for reproducibility.
                String payload=p.toString();repo.saveDescriptorSet(uid,"pubchem-basic/1","PubChem PUG REST property endpoint","PUBCHEM",payload,sha256(payload),true);
            }
            return new Result(true,rows.length(),resolved,matched,structures,properties,"ChemCore molecular seed imported");
        }catch(java.io.FileNotFoundException e){return new Result(false,0,0,0,0,0,"molecular seed asset not installed");}
        catch(Exception e){return new Result(false,0,0,0,0,0,"molecular seed import failed: "+e.getClass().getSimpleName());}
    }

    private static long chemicalUidByCas(ExperimentsDb db,String cas){try(Cursor c=db.getReadableDatabase().rawQuery("SELECT uid FROM chemicals WHERE cas=? LIMIT 1",new String[]{cas})){return c.moveToFirst()?c.getLong(0):-1;}}
    private static String text(JSONObject p,String key){JSONObject q=p.optJSONObject(key);return q==null?null:q.optString("value",null);}
    private static boolean nonempty(String s){return s!=null&&!s.trim().isEmpty();}
    private static String read(InputStream in)throws Exception{ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] x=new byte[8192];for(int n;(n=in.read(x))>=0;)b.write(x,0,n);return b.toString(StandardCharsets.UTF_8.name());}
    private static String sha256(String s)throws Exception{byte[] h=MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));StringBuilder x=new StringBuilder();for(byte b:h)x.append(String.format(Locale.US,"%02x",b&255));return x.toString();}
}
