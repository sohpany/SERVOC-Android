package com.servoc.app.chemical;

import android.database.Cursor;
import com.servoc.app.data.ExperimentsDb;
import java.util.ArrayList;
import java.util.List;

/** Database-backed chemical catalog. Legacy indices remain available only as migration identifiers. */
public final class ChemicalRepository {
    private final ExperimentsDb db;
    public ChemicalRepository(ExperimentsDb db){this.db=db;ChemicalIdentitySchema.ensure(db.getWritableDatabase());}

    public static final class Chemical {
        public final long uid;public final Integer legacyId;public final String cas,name,arabicName,formula,source;
        Chemical(long uid,Integer legacyId,String cas,String name,String arabicName,String formula,String source){this.uid=uid;this.legacyId=legacyId;this.cas=cas;this.name=name;this.arabicName=arabicName;this.formula=formula;this.source=source;}
        public String label(){StringBuilder s=new StringBuilder(name==null?"":name);if(cas!=null&&!cas.isEmpty())s.append(" • CAS ").append(cas);return s.toString();}
    }

    public int count(){return db.chemicalCount();}

    public List<Chemical> search(String query,int limit){
        String q=query==null?"":query.trim();int n=Math.max(1,Math.min(limit,500));List<Chemical> out=new ArrayList<>();
        String sql;String[] args;
        if(q.isEmpty()){sql="SELECT uid,legacy_id,cas,name_en,name_ar,formula,record_source FROM chemicals WHERE active=1 ORDER BY name_en LIMIT "+n;args=null;}
        else{sql="SELECT DISTINCT c.uid,c.legacy_id,c.cas,c.name_en,c.name_ar,c.formula,c.record_source FROM chemicals c LEFT JOIN chemical_synonyms s ON s.chemical_uid=c.uid LEFT JOIN chemical_identifiers i ON i.chemical_uid=c.uid WHERE c.active=1 AND (c.name_en LIKE ? OR c.name_ar LIKE ? OR c.cas LIKE ? OR c.formula LIKE ? OR s.synonym LIKE ? OR i.value LIKE ?) ORDER BY c.name_en LIMIT "+n;String x="%"+q+"%";args=new String[]{x,x,x,x,x,x};}
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

    public Chemical byLegacyId(int id){try(Cursor c=db.getReadableDatabase().rawQuery("SELECT uid,legacy_id,cas,name_en,name_ar,formula,record_source FROM chemicals WHERE legacy_id=? AND active=1",new String[]{String.valueOf(id)})){return c.moveToFirst()?read(c):null;}}
    public Chemical byUid(long uid){try(Cursor c=db.getReadableDatabase().rawQuery("SELECT uid,legacy_id,cas,name_en,name_ar,formula,record_source FROM chemicals WHERE uid=? AND active=1",new String[]{String.valueOf(uid)})){return c.moveToFirst()?read(c):null;}}
    public List<String> identifiers(long uid,String scheme){List<String> out=new ArrayList<>();try(Cursor c=db.getReadableDatabase().rawQuery("SELECT DISTINCT value FROM chemical_identifiers WHERE chemical_uid=? AND scheme=? ORDER BY value",new String[]{String.valueOf(uid),scheme})){while(c.moveToNext())out.add(c.getString(0));}return out;}

    private Chemical exact(String q){
        String sql="SELECT DISTINCT c.uid,c.legacy_id,c.cas,c.name_en,c.name_ar,c.formula,c.record_source FROM chemicals c LEFT JOIN chemical_synonyms s ON s.chemical_uid=c.uid LEFT JOIN chemical_identifiers i ON i.chemical_uid=c.uid WHERE c.active=1 AND (c.cas=? OR i.value=? OR lower(trim(c.name_en))=lower(trim(?)) OR lower(trim(c.name_ar))=lower(trim(?)) OR lower(trim(s.synonym))=lower(trim(?))) ORDER BY c.uid";
        try(Cursor x=db.getReadableDatabase().rawQuery(sql,new String[]{q,q,q,q,q})){Chemical one=null;int n=0;while(x.moveToNext()){one=read(x);n++;}return n==1?one:null;}
    }

    private static Chemical read(Cursor c){Integer legacy=c.isNull(1)?null:c.getInt(1);return new Chemical(c.getLong(0),legacy,c.getString(2),c.getString(3),c.getString(4),c.getString(5),c.getString(6));}
}
