package com.servoc.app.chemcore;

import android.content.ContentValues;
import android.database.Cursor;
import com.servoc.app.data.ExperimentsDb;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/** Persistence facade for ChemCore molecular structures, properties and descriptor sets. */
public final class MolecularIntelligenceRepository {
    private final ExperimentsDb db;

    public MolecularIntelligenceRepository(ExperimentsDb db) {
        if (db == null) throw new IllegalArgumentException("database required");
        this.db = db;
        ChemCoreSchema.ensure(db.getWritableDatabase());
    }

    public boolean saveStructure(long chemicalUid, String canonicalSmiles, String isomericSmiles,
                                 String inchi, String inchikey, String functionalGroups, String sourceId) {
        if (chemicalUid <= 0) return false;
        ContentValues v = new ContentValues();
        v.put("chemical_uid", chemicalUid);
        put(v,"canonical_smiles",canonicalSmiles); put(v,"isomeric_smiles",isomericSmiles);
        put(v,"inchi",inchi); put(v,"inchikey",inchikey); put(v,"functional_groups",functionalGroups);
        put(v,"source_id",sourceId); v.put("updated_ts", isoNow());
        long row = db.getWritableDatabase().insertWithOnConflict("chemcore_structures", null, v,
                android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE);
        return row >= 0;
    }

    public long saveProperty(MolecularProperty p) {
        if (p == null || p.chemicalUid <= 0 || p.key.trim().isEmpty()) return -1;
        ContentValues v = new ContentValues();
        v.put("chemical_uid", p.chemicalUid); v.put("property_key", p.key);
        if (Double.isFinite(p.numericValue)) v.put("value_numeric", p.numericValue); else v.putNull("value_numeric");
        put(v,"value_text",p.textValue); put(v,"unit",p.unit);
        if (Double.isFinite(p.temperatureC)) v.put("temp_c",p.temperatureC); else v.putNull("temp_c");
        if (Double.isFinite(p.pressureKPa)) v.put("pressure_kpa",p.pressureKPa); else v.putNull("pressure_kpa");
        put(v,"method",p.method); put(v,"source_id",p.sourceId);
        if (Double.isFinite(p.uncertainty)) v.put("uncertainty",p.uncertainty); else v.putNull("uncertainty");
        put(v,"quality_tier",p.qualityTier); v.put("created_ts",isoNow());
        return db.getWritableDatabase().insert("chemcore_properties",null,v);
    }

    public List<MolecularProperty> properties(long chemicalUid, String key) {
        List<MolecularProperty> out = new ArrayList<>();
        String sql="SELECT property_key,value_numeric,value_text,unit,temp_c,pressure_kpa,method,source_id,uncertainty,quality_tier FROM chemcore_properties WHERE chemical_uid=?";
        List<String> args=new ArrayList<>();args.add(String.valueOf(chemicalUid));
        if(key!=null&&!key.trim().isEmpty()){sql+=" AND property_key=?";args.add(key.trim());}
        sql+=" ORDER BY id DESC";
        try(Cursor c=db.getReadableDatabase().rawQuery(sql,args.toArray(new String[0]))){while(c.moveToNext())out.add(new MolecularProperty(
                chemicalUid,c.getString(0),nullable(c,1),c.getString(2),c.getString(3),nullable(c,4),nullable(c,5),
                c.getString(6),c.getString(7),nullable(c,8),c.getString(9)));}
        return out;
    }

    public Double latestNumeric(long chemicalUid, String key) {
        try(Cursor c=db.getReadableDatabase().rawQuery(
                "SELECT value_numeric FROM chemcore_properties WHERE chemical_uid=? AND property_key=? AND value_numeric IS NOT NULL ORDER BY id DESC LIMIT 1",
                new String[]{String.valueOf(chemicalUid),key})){
            return c.moveToFirst()&&!c.isNull(0)?c.getDouble(0):null;
        }
    }

    public boolean saveDescriptorSet(long chemicalUid, String descriptorVersion, String sourceMethod,
                                     String sourceId, String payload, String sha256, boolean active) {
        if(chemicalUid<=0||descriptorVersion==null||payload==null||sha256==null)return false;
        if(active)db.getWritableDatabase().execSQL("UPDATE chemcore_descriptor_sets SET active=0 WHERE chemical_uid=?",new Object[]{chemicalUid});
        ContentValues v=new ContentValues();v.put("chemical_uid",chemicalUid);v.put("descriptor_version",descriptorVersion);
        v.put("created_ts",isoNow());v.put("source_method",sourceMethod==null?"":sourceMethod);put(v,"source_id",sourceId);
        v.put("payload",payload);v.put("sha256",sha256);v.put("active",active?1:0);
        return db.getWritableDatabase().insert("chemcore_descriptor_sets",null,v)>=0;
    }

    private static void put(ContentValues v,String k,String s){if(s==null||s.isEmpty())v.putNull(k);else v.put(k,s);}
    private static double nullable(Cursor c,int i){return c.isNull(i)?Double.NaN:c.getDouble(i);}
    private static String isoNow(){SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'",Locale.US);f.setTimeZone(TimeZone.getTimeZone("UTC"));return f.format(new Date());}
}
