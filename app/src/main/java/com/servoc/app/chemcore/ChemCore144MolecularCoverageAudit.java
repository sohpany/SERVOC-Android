package com.servoc.app.chemcore;

import android.database.Cursor;

import com.servoc.app.chemical.ChemicalCatalog;
import com.servoc.app.data.ExperimentsDb;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Exact 144-entry molecular coverage audit for ChemCore Stage 24.
 *
 * Unlike MolecularCoverageReport, which summarizes the whole dynamic chemical database, this
 * audit is pinned to legacy IDs 0..143 and therefore answers the Stage-24 question directly:
 * which HazRob analytical-catalog entries have an authoritative molecular identity, structure
 * and complete CORE descriptor vector suitable for molecular/QSPR consideration?
 */
public final class ChemCore144MolecularCoverageAudit {
    public static final String SCHEMA="chemcore-144-molecular-coverage/1";

    public enum State {
        MISSING_LEGACY_RECORD,
        NON_MOLECULAR_CURATED,
        NO_CAS,
        NO_STRUCTURE,
        CORE_DESCRIPTOR_INCOMPLETE,
        QSPR_MOLECULAR_READY
    }

    public static final class Row {
        public final int legacyId;
        public final long chemicalUid;
        public final String catalogName,dbName,cas;
        public final State state;
        public final List<String> missingCore;

        Row(int legacyId,long chemicalUid,String catalogName,String dbName,String cas,State state,List<String> missingCore){
            this.legacyId=legacyId;this.chemicalUid=chemicalUid;this.catalogName=catalogName==null?"":catalogName;
            this.dbName=dbName==null?"":dbName;this.cas=cas==null?"":cas;this.state=state;
            this.missingCore=missingCore==null?Collections.emptyList():Collections.unmodifiableList(new ArrayList<>(missingCore));
        }
    }

    public static final class Result {
        public final int reviewed;
        public final List<Row> rows;
        public final Map<State,Integer> counts;

        Result(List<Row> rows,Map<State,Integer> counts){
            this.reviewed=rows.size();this.rows=Collections.unmodifiableList(rows);this.counts=Collections.unmodifiableMap(counts);
        }

        public int count(State state){Integer n=counts.get(state);return n==null?0:n;}
        public int molecularReady(){return count(State.QSPR_MOLECULAR_READY);}

        public JSONObject toJson(){
            try{
                JSONObject out=new JSONObject();out.put("schema",SCHEMA);out.put("reviewed",reviewed);
                JSONObject c=new JSONObject();for(State s:State.values())c.put(s.name(),count(s));out.put("counts",c);
                JSONArray a=new JSONArray();
                for(Row r:rows){
                    JSONObject x=new JSONObject();x.put("legacy_id",r.legacyId);x.put("catalog_name",r.catalogName);
                    x.put("chemical_uid",r.chemicalUid);x.put("db_name",r.dbName);x.put("cas",r.cas);x.put("state",r.state.name());
                    JSONArray m=new JSONArray();for(String q:r.missingCore)m.put(q);x.put("missing_core",m);a.put(x);
                }
                out.put("rows",a);return out;
            }catch(Exception e){throw new IllegalStateException("unable to serialize 144 molecular coverage audit",e);}
        }
    }

    private ChemCore144MolecularCoverageAudit(){}

    public static Result build(ExperimentsDb db){
        if(db==null)throw new IllegalArgumentException("database required");
        MolecularIntelligenceRepository molecular=new MolecularIntelligenceRepository(db);
        ChemCoreIdentityCurationRepository curation=new ChemCoreIdentityCurationRepository(db);
        List<Row> rows=new ArrayList<>(ChemicalCatalog.COUNT);
        Map<State,Integer> counts=new LinkedHashMap<>();for(State s:State.values())counts.put(s,0);

        for(int legacy=0;legacy<ChemicalCatalog.COUNT;legacy++){
            Row r=reviewOne(db,molecular,curation,legacy);rows.add(r);counts.put(r.state,counts.get(r.state)+1);
        }
        if(rows.size()!=144)throw new IllegalStateException("HazRob 144 catalogue size drifted: "+rows.size());
        return new Result(rows,counts);
    }

    private static Row reviewOne(ExperimentsDb db,MolecularIntelligenceRepository molecular,
                                 ChemCoreIdentityCurationRepository curation,int legacy){
        long uid=-1;String cas="",name="";
        try(Cursor c=db.getReadableDatabase().rawQuery(
                "SELECT uid,cas,name_en FROM chemicals WHERE legacy_id=? AND active=1 LIMIT 1",
                new String[]{String.valueOf(legacy)})){
            if(!c.moveToFirst())return row(legacy,-1,"","",State.MISSING_LEGACY_RECORD,Collections.emptyList());
            uid=c.getLong(0);cas=c.isNull(1)?"":c.getString(1);name=c.isNull(2)?"":c.getString(2);
        }

        ChemCoreIdentityCurationRepository.Entry curated=curation.find(cas,name);
        if(curated!=null&&!curated.molecularQsprEligible)
            return row(legacy,uid,name,cas,State.NON_MOLECULAR_CURATED,Collections.emptyList());
        if(cas==null||cas.trim().isEmpty())
            return row(legacy,uid,name,cas,State.NO_CAS,Collections.emptyList());
        if(!hasStructure(db,uid))
            return row(legacy,uid,name,cas,State.NO_STRUCTURE,Collections.emptyList());

        List<String> missing=new ArrayList<>();
        for(String key:MolecularDescriptorContract.CORE){
            Double v=molecular.latestNumeric(uid,key);if(v==null||!Double.isFinite(v))missing.add(key);
        }
        if(!missing.isEmpty())return row(legacy,uid,name,cas,State.CORE_DESCRIPTOR_INCOMPLETE,missing);
        return row(legacy,uid,name,cas,State.QSPR_MOLECULAR_READY,Collections.emptyList());
    }

    private static boolean hasStructure(ExperimentsDb db,long uid){
        try(Cursor c=db.getReadableDatabase().rawQuery(
                "SELECT 1 FROM chemcore_structures WHERE chemical_uid=? LIMIT 1",new String[]{String.valueOf(uid)})){
            return c.moveToFirst();
        }
    }

    private static Row row(int legacy,long uid,String dbName,String cas,State state,List<String> missing){
        String catalog=legacy>=0&&legacy<ChemicalCatalog.COUNT?ChemicalCatalog.NAMES[legacy]:"";
        return new Row(legacy,uid,catalog,dbName,cas,state,missing);
    }
}
