package com.servoc.app.exposure;

import android.database.Cursor;
import com.servoc.app.data.ExperimentsDb;
import java.util.ArrayList;
import java.util.List;

/** Authority-preserving occupational exposure knowledge for HazRob 0.10.2. */
public final class ExposureRepository {
    public static final class Limit {
        public final long id,chemicalUid;
        public final String authority,jurisdiction,type,unit,notation,sourceId,revision;
        public final Double value,durationMin;
        Limit(long id,long chemicalUid,String authority,String jurisdiction,String type,Double value,String unit,Double durationMin,String notation,String sourceId,String revision){
            this.id=id;this.chemicalUid=chemicalUid;this.authority=authority;this.jurisdiction=jurisdiction;this.type=type;this.value=value;this.unit=unit;this.durationMin=durationMin;this.notation=notation;this.sourceId=sourceId;this.revision=revision;
        }
        public boolean numeric(){return value!=null&&Double.isFinite(value);}
    }

    private final ExperimentsDb db;
    public ExposureRepository(ExperimentsDb db){this.db=db;}

    public List<Limit> forChemical(long chemicalUid){
        return query("WHERE e.chemical_uid=?",new String[]{String.valueOf(chemicalUid)});
    }

    public List<Limit> forChemical(long chemicalUid,String authority){
        return query("WHERE e.chemical_uid=? AND e.authority=?",new String[]{String.valueOf(chemicalUid),authority});
    }

    public List<Limit> byCas(String cas){
        if(cas==null||cas.trim().isEmpty())return new ArrayList<>();
        return query("JOIN chemicals c ON c.uid=e.chemical_uid WHERE c.cas=?",new String[]{cas.trim()});
    }

    /** Canonical molecular mass (g/mol) from the same chemical master used by exposure limits. */
    public double molarMass(long chemicalUid){
        if(chemicalUid<=0)return Double.NaN;
        try(Cursor c=db.getReadableDatabase().rawQuery("SELECT molar_mass FROM chemicals WHERE uid=? LIMIT 1",new String[]{String.valueOf(chemicalUid)})){
            if(!c.moveToFirst()||c.isNull(0))return Double.NaN;double v=c.getDouble(0);return Double.isFinite(v)&&v>0?v:Double.NaN;
        }catch(Exception e){return Double.NaN;}
    }

    public int count(){try(Cursor c=db.getReadableDatabase().rawQuery("SELECT COUNT(*) FROM exposure_limits",null)){return c.moveToFirst()?c.getInt(0):0;}}

    private List<Limit> query(String clause,String[] args){
        List<Limit> out=new ArrayList<>();
        String sql="SELECT e.id,e.chemical_uid,e.authority,e.jurisdiction,e.limit_type,e.value,e.unit,e.duration_min,e.notation,e.source_id,e.revision FROM exposure_limits e "+clause+" ORDER BY e.authority COLLATE NOCASE,e.limit_type COLLATE NOCASE,e.revision DESC";
        try(Cursor c=db.getReadableDatabase().rawQuery(sql,args)){while(c.moveToNext())out.add(read(c));}
        return out;
    }

    private static Limit read(Cursor c){
        Double value=c.isNull(5)?null:c.getDouble(5),duration=c.isNull(7)?null:c.getDouble(7);
        return new Limit(c.getLong(0),c.getLong(1),c.getString(2),c.getString(3),c.getString(4),value,c.getString(6),duration,c.getString(8),c.getString(9),c.getString(10));
    }
}
