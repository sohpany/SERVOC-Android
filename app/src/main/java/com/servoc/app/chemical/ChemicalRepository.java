package com.servoc.app.chemical;

import android.database.Cursor;
import com.servoc.app.data.ExperimentsDb;
import java.util.ArrayList;
import java.util.List;

/** Database-backed chemical catalog. Legacy indices remain available only as migration identifiers. */
public final class ChemicalRepository {
    private final ExperimentsDb db;
    public ChemicalRepository(ExperimentsDb db){this.db=db;ChemicalIdentitySchema.ensure(db.getWritableDatabase());}
    public ExperimentsDb database(){return db;}

    public static final class Chemical {
        public final long uid;public final Integer legacyId,pubchemCid;public final String cas,name,arabicName,formula,chemicalClass,source;public final Double molarMass;
        Chemical(long uid,Integer legacyId,String cas,Integer pubchemCid,String name,String arabicName,String formula,Double molarMass,String chemicalClass,String source){this.uid=uid;this.legacyId=legacyId;this.cas=cas;this.pubchemCid=pubchemCid;this.name=name;this.arabicName=arabicName;this.formula=formula;this.molarMass=molarMass;this.chemicalClass=chemicalClass;this.source=source;}
        public String label(){StringBuilder s=new StringBuilder(name==null?"":name);if(cas!=null&&!cas.isEmpty())s.append(" • CAS ").append(cas);return s.toString();}
    }

    private static final String COLS="uid,legacy_id,cas,pubchem_cid,name_en,name_ar,formula,molar_mass,chemical_class,record_source";

    public int count(){return db.chemicalCount();}

    /** Current high-priority subset. It references the same master rows; no chemical facts are duplicated. */
    public List<Chemical> priority(int limit){
        ChemicalPriorityBootstrap.ensure(db.getWritableDatabase());
        int n=Math.max(1,Math.min(limit,5000));List<Chemical> out=new ArrayList<>();
        String sql="SELECT c."+COLS.replace(",",",c.")+" FROM chemicals c JOIN chemical_priority p ON p.chemical_uid=c.uid WHERE c.active=1 ORDER BY CASE p.priority_class WHEN 'BASE_144' THEN 0 ELSE 1 END, CASE WHEN c.legacy_id IS NULL THEN 999999 ELSE c.legacy_id END, lower(c.name_en) LIMIT "+n;
        try(Cursor c=db.getReadableDatabase().rawQuery(sql,null)){while(c.moveToNext())out.add(read(c));}
        return out;
    }

    public int priorityCount(){
        ChemicalPriorityBootstrap.ensure(db.getWritableDatabase());
        try(Cursor c=db.getReadableDatabase().rawQuery("SELECT COUNT(*) FROM chemical_priority p JOIN chemicals c ON c.uid=p.chemical_uid WHERE c.active=1",null)){return c.moveToFirst()?c.getInt(0):0;}
    }

    public boolean isPriority(long uid){
        ChemicalPriorityBootstrap.ensure(db.getWritableDatabase());
        try(Cursor c=db.getReadableDatabase().rawQuery("SELECT 1 FROM chemical_priority WHERE chemical_uid=? LIMIT 1",new String[]{String.valueOf(uid)})){return c.moveToFirst();}
    }

    public List<Chemical> search(String query,int limit){
        String q=query==null?"":query.trim();int n=Math.max(1,Math.min(limit,5000));List<Chemical> out=new ArrayList<>();
        String sql;String[] args;
        if(q.isEmpty()){sql="SELECT "+COLS+" FROM chemicals WHERE active=1 ORDER BY name_en LIMIT "+n;args=null;}
        else{sql="SELECT DISTINCT c.uid,c.legacy_id,c.cas,c.pubchem_cid,c.name_en,c.name_ar,c.formula,c.molar_mass,c.chemical_class,c.record_source FROM chemicals c LEFT JOIN chemical_synonyms s ON s.chemical_uid=c.uid LEFT JOIN chemical_identifiers i ON i.chemical_uid=c.uid WHERE c.active=1 AND (c.name_en LIKE ? OR c.name_ar LIKE ? OR c.cas LIKE ? OR c.formula LIKE ? OR s.synonym LIKE ? OR i.value LIKE ?) ORDER BY c.name_en LIMIT "+n;String x="%"+q+"%";args=new String[]{x,x,x,x,x,x};}
        try(Cursor c=db.getReadableDatabase().rawQuery(sql,args)){while(c.moveToNext())out.add(read(c));}
        return out;
    }

    /** Exact UID/CAS/name/synonym first; one unique fuzzy result is accepted as a convenience. */
    public Chemical resolve(String query){
        String q=query==null?"":query.trim();if(q.isEmpty())return null;
        if(q.matches("\\d+")){try{Chemical c=byUid(Long.parseLong(q));if(c!=null)return c;}catch(Exception ignored){}}
        Chemical c=exact(q);if(c!=null)return c;
        List<Chemical> rows=search(q,3);return rows.size()==1?rows.get(0):null;
    }

    public Chemical byLegacyId(int id){try(Cursor c=db.getReadableDatabase().rawQuery("SELECT "+COLS+" FROM chemicals WHERE legacy_id=? AND active=1",new String[]{String.valueOf(id)})){return c.moveToFirst()?read(c):null;}}
    public Chemical byUid(long uid){try(Cursor c=db.getReadableDatabase().rawQuery("SELECT "+COLS+" FROM chemicals WHERE uid=? AND active=1",new String[]{String.valueOf(uid)})){return c.moveToFirst()?read(c):null;}}
    public List<String> identifiers(long uid,String scheme){List<String> out=new ArrayList<>();try(Cursor c=db.getReadableDatabase().rawQuery("SELECT DISTINCT value FROM chemical_identifiers WHERE chemical_uid=? AND scheme=? ORDER BY value",new String[]{String.valueOf(uid),scheme})){while(c.moveToNext())out.add(c.getString(0));}return out;}

    private Chemical exact(String q){
        String sql="SELECT DISTINCT c.uid,c.legacy_id,c.cas,c.pubchem_cid,c.name_en,c.name_ar,c.formula,c.molar_mass,c.chemical_class,c.record_source FROM chemicals c LEFT JOIN chemical_synonyms s ON s.chemical_uid=c.uid LEFT JOIN chemical_identifiers i ON i.chemical_uid=c.uid WHERE c.active=1 AND (c.cas=? OR i.value=? OR lower(trim(c.name_en))=lower(trim(?)) OR lower(trim(c.name_ar))=lower(trim(?)) OR lower(trim(s.synonym))=lower(trim(?))) ORDER BY c.uid";
        try(Cursor x=db.getReadableDatabase().rawQuery(sql,new String[]{q,q,q,q,q})){Chemical one=null;int n=0;while(x.moveToNext()){one=read(x);n++;}return n==1?one:null;}
    }

    private static Chemical read(Cursor c){
        Integer legacy=c.isNull(1)?null:c.getInt(1);Integer pubchem=c.isNull(3)?null:c.getInt(3);Double mw=c.isNull(7)?null:c.getDouble(7);
        return new Chemical(c.getLong(0),legacy,c.getString(2),pubchem,c.getString(4),c.getString(5),c.getString(6),mw,c.getString(8),c.getString(9));
    }
}
