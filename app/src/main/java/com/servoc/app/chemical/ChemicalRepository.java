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

    public Chemical byLegacyId(int id){try(Cursor c=db.getReadableDatabase().rawQuery("SELECT uid,legacy_id,cas,name_en,name_ar,formula,record_source FROM chemicals WHERE legacy_id=? AND active=1",new String[]{String.valueOf(id)})){return c.moveToFirst()?read(c):null;}}
    public Chemical byUid(long uid){try(Cursor c=db.getReadableDatabase().rawQuery("SELECT uid,legacy_id,cas,name_en,name_ar,formula,record_source FROM chemicals WHERE uid=? AND active=1",new String[]{String.valueOf(uid)})){return c.moveToFirst()?read(c):null;}}
    public List<String> identifiers(long uid,String scheme){List<String> out=new ArrayList<>();try(Cursor c=db.getReadableDatabase().rawQuery("SELECT DISTINCT value FROM chemical_identifiers WHERE chemical_uid=? AND scheme=? ORDER BY value",new String[]{String.valueOf(uid),scheme})){while(c.moveToNext())out.add(c.getString(0));}return out;}

    private static Chemical read(Cursor c){Integer legacy=c.isNull(1)?null:c.getInt(1);return new Chemical(c.getLong(0),legacy,c.getString(2),c.getString(3),c.getString(4),c.getString(5),c.getString(6));}
}
