package com.servoc.app.chemical;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Maintains the HazRob high-priority chemical catalogue as a SUBSET of the single master
 * chemicals table. The original 144 remain permanently prioritized, while any master-library
 * compound with accepted MQ identity/quantitative evidence is promoted automatically.
 *
 * Important design rule: priority rows contain no duplicate chemical facts. CAS, PubChem,
 * formula, names, exposure limits and sensor evidence remain attached to chemicals.uid and are
 * therefore identical whether the compound is viewed from the master library or priority view.
 */
public final class ChemicalPriorityBootstrap {
    private ChemicalPriorityBootstrap() {}

    public static final String META_KEY="chemical_priority_subset_version";
    public static final String VERSION="1";
    public static final String BASE_CLASS="BASE_144";
    public static final String PROMOTED_CLASS="PROMOTED_MQ_EVIDENCE";

    private static final class Aggregate {
        int count;
        final Set<String> sources=new LinkedHashSet<>();
        final Set<String> evidenceTypes=new LinkedHashSet<>();
    }

    public static void ensure(SQLiteDatabase db){
        if(db==null||db.isReadOnly())return;
        createSchema(db);
        db.beginTransaction();
        try{
            // The 144 catalogue is the permanent safety/occupational-priority base.
            for(int legacy=0;legacy<ChemicalCatalog.COUNT;legacy++){
                Long uid=uidByLegacy(db,legacy);if(uid==null)continue;
                ContentValues v=new ContentValues();
                v.put("chemical_uid",uid);v.put("priority_class",BASE_CLASS);
                v.put("promotion_reason","ORIGINAL_HAZROB_ENVIRONMENTAL_OHS_PRIORITY");
                v.put("qualifying_evidence_count",0);v.put("evidence_sources","");v.put("evidence_types","");
                db.insertWithOnConflict("chemical_priority",null,v,SQLiteDatabase.CONFLICT_REPLACE);
            }

            // Recalculate promoted rows from current accepted evidence so the subset cannot drift.
            db.delete("chemical_priority","priority_class=?",new String[]{PROMOTED_CLASS});
            Map<Long,Aggregate> qualified=collectQualifiedEvidence(db);
            for(Map.Entry<Long,Aggregate> e:qualified.entrySet()){
                long uid=e.getKey();Aggregate a=e.getValue();
                String existing=priorityClass(db,uid);
                if(BASE_CLASS.equals(existing)){
                    ContentValues v=new ContentValues();v.put("qualifying_evidence_count",a.count);
                    v.put("evidence_sources",join(a.sources));v.put("evidence_types",join(a.evidenceTypes));
                    db.update("chemical_priority",v,"chemical_uid=?",new String[]{String.valueOf(uid)});
                }else{
                    ContentValues v=new ContentValues();v.put("chemical_uid",uid);v.put("priority_class",PROMOTED_CLASS);
                    v.put("promotion_reason","ACCEPTED_MQ_IDENTITY_OR_QUANTITATIVE_EVIDENCE");v.put("qualifying_evidence_count",a.count);
                    v.put("evidence_sources",join(a.sources));v.put("evidence_types",join(a.evidenceTypes));
                    db.insertWithOnConflict("chemical_priority",null,v,SQLiteDatabase.CONFLICT_REPLACE);
                }
            }

            int total=count(db,"SELECT COUNT(*) FROM chemical_priority");
            int promoted=count(db,"SELECT COUNT(*) FROM chemical_priority WHERE priority_class='"+PROMOTED_CLASS+"'");
            int base=count(db,"SELECT COUNT(*) FROM chemical_priority WHERE priority_class='"+BASE_CLASS+"'");
            putMeta(db,META_KEY,VERSION);
            putMeta(db,"chemical_priority_policy","MASTER_SINGLE_RECORD_PRIORITY_SUBSET_SHARED_DETAILS");
            putMeta(db,"chemical_priority_base_count",String.valueOf(base));
            putMeta(db,"chemical_priority_promoted_count",String.valueOf(promoted));
            putMeta(db,"chemical_priority_total_count",String.valueOf(total));
            putMeta(db,"chemical_priority_promoted_names",promotedNames(db));
            db.setTransactionSuccessful();
        }finally{db.endTransaction();}
    }

    /**
     * Qualification is intentionally stricter than merely having an MQ mention.
     * Accepted feasibility-only, awaiting-raw, family-only, or no-direct-transfer rows do not
     * promote a compound. Accepted identity fingerprints, PPM/quantitative references and
     * calibrated/known-concentration relations do.
     */
    static boolean qualifies(String status,String evidenceType,Double minPpm,Double maxPpm){
        String s=up(status),t=up(evidenceType);
        if(!s.startsWith("ACCEPTED"))return false;
        if(s.contains("REFERENCE_ONLY")||s.contains("AWAITING")||s.contains("NO_DIRECT")||s.contains("FAMILY_LABEL"))return false;
        if(s.contains("IDENTITY")||s.contains("PPM")||s.contains("QUANTITATIVE"))return true;
        if(t.contains("FINGERPRINT")||t.contains("CALIBRATION")||t.contains("KNOWN_PPM")||t.contains("KNOWN_CONCENTRATION")||t.contains("REFERENCE_COMPARISON"))return true;
        return minPpm!=null&&maxPpm!=null&&Double.isFinite(minPpm)&&Double.isFinite(maxPpm)&&maxPpm>minPpm;
    }

    private static Map<Long,Aggregate> collectQualifiedEvidence(SQLiteDatabase db){
        Map<Long,Aggregate> out=new LinkedHashMap<>();
        try(Cursor c=db.rawQuery("SELECT chemical_uid,status,evidence_type,min_ppm,max_ppm,source_id FROM sensor_evidence WHERE chemical_uid IS NOT NULL",null)){
            while(c.moveToNext()){
                long uid=c.getLong(0);String status=c.getString(1),type=c.getString(2);
                Double min=c.isNull(3)?null:c.getDouble(3),max=c.isNull(4)?null:c.getDouble(4);
                if(!qualifies(status,type,min,max))continue;
                Aggregate a=out.get(uid);if(a==null){a=new Aggregate();out.put(uid,a);}a.count++;
                if(!c.isNull(5)&&!c.getString(5).trim().isEmpty())a.sources.add(c.getString(5).trim());
                if(type!=null&&!type.trim().isEmpty())a.evidenceTypes.add(type.trim());
            }
        }
        return out;
    }

    private static void createSchema(SQLiteDatabase db){
        db.execSQL("CREATE TABLE IF NOT EXISTS chemical_priority(chemical_uid INTEGER PRIMARY KEY, priority_class TEXT NOT NULL, promotion_reason TEXT NOT NULL, qualifying_evidence_count INTEGER NOT NULL DEFAULT 0, evidence_sources TEXT, evidence_types TEXT, FOREIGN KEY(chemical_uid) REFERENCES chemicals(uid) ON DELETE CASCADE)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_chemical_priority_class ON chemical_priority(priority_class,chemical_uid)");
        db.execSQL("DROP VIEW IF EXISTS priority_chemicals");
        db.execSQL("CREATE VIEW priority_chemicals AS SELECT c.uid,c.legacy_id,c.cas,c.pubchem_cid,c.name_en,c.name_ar,c.formula,c.molar_mass,c.chemical_class,c.record_source,c.active,p.priority_class,p.promotion_reason,p.qualifying_evidence_count,p.evidence_sources,p.evidence_types FROM chemicals c JOIN chemical_priority p ON p.chemical_uid=c.uid WHERE c.active=1");
    }

    private static String promotedNames(SQLiteDatabase db){
        List<String> names=new ArrayList<>();
        try(Cursor c=db.rawQuery("SELECT c.name_en FROM chemical_priority p JOIN chemicals c ON c.uid=p.chemical_uid WHERE p.priority_class=? ORDER BY lower(c.name_en)",new String[]{PROMOTED_CLASS})){
            while(c.moveToNext())names.add(c.getString(0));
        }
        return join(new LinkedHashSet<>(names));
    }
    private static Long uidByLegacy(SQLiteDatabase db,int legacy){try(Cursor c=db.rawQuery("SELECT uid FROM chemicals WHERE legacy_id=? LIMIT 1",new String[]{String.valueOf(legacy)})){return c.moveToFirst()?c.getLong(0):null;}}
    private static String priorityClass(SQLiteDatabase db,long uid){try(Cursor c=db.rawQuery("SELECT priority_class FROM chemical_priority WHERE chemical_uid=? LIMIT 1",new String[]{String.valueOf(uid)})){return c.moveToFirst()?c.getString(0):null;}}
    private static int count(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getInt(0):0;}}
    private static void putMeta(SQLiteDatabase db,String key,String value){ContentValues v=new ContentValues();v.put("k",key);v.put("v",value==null?"":value);db.insertWithOnConflict("meta",null,v,SQLiteDatabase.CONFLICT_REPLACE);}
    private static String join(Set<String> values){StringBuilder b=new StringBuilder();for(String x:values){if(b.length()>0)b.append(" | ");b.append(x);}return b.toString();}
    private static String up(String s){return s==null?"":s.trim().toUpperCase(Locale.US);}
}
