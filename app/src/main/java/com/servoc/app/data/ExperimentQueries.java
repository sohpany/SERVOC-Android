package com.servoc.app.data;

import android.database.Cursor;
import java.util.ArrayList;
import java.util.List;

public final class ExperimentQueries {
    private ExperimentQueries(){}
    public static final class Record {
        public final long id,captureId,chemicalUid;public final String ts,payload,protocol,analytics,approvedTs,referenceName,sessionId,referenceKind;public final boolean approved;public final Double knownPpm;
        public Record(long id,String ts,String payload,String protocol,String analytics,boolean approved,String approvedTs,String referenceName,String sessionId,long captureId,long chemicalUid,String referenceKind,Double knownPpm){this.id=id;this.ts=ts;this.payload=payload;this.protocol=protocol;this.analytics=analytics;this.approved=approved;this.approvedTs=approvedTs;this.referenceName=referenceName;this.sessionId=sessionId==null?"":sessionId;this.captureId=captureId;this.chemicalUid=chemicalUid;this.referenceKind=referenceKind==null?"":referenceKind;this.knownPpm=knownPpm;}
        /** Compatibility constructor for legacy records/tests created before scientific lineage fields were added. */
        public Record(long id,String ts,String payload,String protocol,String analytics,boolean approved,String approvedTs,String referenceName){this(id,ts,payload,protocol,analytics,approved,approvedTs,referenceName,"",-1,-1,"",null);}
        public boolean isReference(){return captureId>0&&chemicalUid>0&&!referenceKind.isEmpty();}
    }
    private static final String COLS="id,ts,payload,protocol,analytics,approved,approved_ts,reference_name,session_id,capture_id,chemical_uid,reference_kind,known_ppm";
    public static List<Record> recent(ExperimentsDb db,int limit){List<Record> out=new ArrayList<>();int n=Math.max(1,Math.min(limit,1000));try(Cursor c=db.getReadableDatabase().rawQuery("SELECT "+COLS+" FROM experiments ORDER BY id DESC LIMIT "+n,null)){while(c.moveToNext())out.add(read(c));}return out;}
    /** Developer-only laboratory/reference records; never show USER field records as publishable. */
    public static List<Record> recentDeveloper(ExperimentsDb db,int limit){
        List<Record> out=new ArrayList<>();int n=Math.max(1,Math.min(limit,1000));
        String select="e."+COLS.replace(",",",e.");
        String sql="SELECT "+select+" FROM experiments e LEFT JOIN measurement_sessions m ON m.session_id=e.session_id "+
            "WHERE m.operator_role='DEVELOPER' ORDER BY e.id DESC LIMIT "+n;
        try(Cursor c=db.getReadableDatabase().rawQuery(sql,null)){while(c.moveToNext())out.add(read(c));}
        return out;
    }
    public static boolean isDeveloperReference(ExperimentsDb db,long id){
        if(db==null||id<=0)return false;
        String sql="SELECT 1 FROM experiments e JOIN measurement_sessions m ON m.session_id=e.session_id "+
            "WHERE e.id=? AND m.operator_role='DEVELOPER' AND e.capture_id IS NOT NULL "+
            "AND e.chemical_uid IS NOT NULL AND e.reference_kind IN ('fingerprint','calibration') LIMIT 1";
        try(Cursor c=db.getReadableDatabase().rawQuery(sql,new String[]{String.valueOf(id)})){return c.moveToFirst();}
    }

    public static Record byId(ExperimentsDb db,long id){try(Cursor c=db.getReadableDatabase().rawQuery("SELECT "+COLS+" FROM experiments WHERE id=?",new String[]{String.valueOf(id)})){return c.moveToFirst()?read(c):null;}}
    public static List<Record> bySessionId(ExperimentsDb db,String sessionId){List<Record> out=new ArrayList<>();if(db==null||sessionId==null||sessionId.trim().isEmpty())return out;try(Cursor c=db.getReadableDatabase().rawQuery("SELECT "+COLS+" FROM experiments WHERE session_id=? ORDER BY id",new String[]{sessionId.trim()})){while(c.moveToNext())out.add(read(c));}return out;}
    private static Record read(Cursor c){Double ppm=c.isNull(12)?null:c.getDouble(12);return new Record(c.getLong(0),c.getString(1),c.getString(2),c.getString(3),c.getString(4),c.getInt(5)!=0,c.getString(6),c.getString(7),c.getString(8),c.isNull(9)?-1:c.getLong(9),c.isNull(10)?-1:c.getLong(10),c.getString(11),ppm);}
}
