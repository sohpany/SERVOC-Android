package com.servoc.app.data;

import android.database.Cursor;
import java.util.ArrayList;
import java.util.List;

public final class ExperimentQueries {
    private ExperimentQueries(){}
    public static final class Record {public final long id;public final String ts,payload,protocol,analytics,approvedTs,referenceName;public final boolean approved;public Record(long id,String ts,String payload,String protocol,String analytics,boolean approved,String approvedTs,String referenceName){this.id=id;this.ts=ts;this.payload=payload;this.protocol=protocol;this.analytics=analytics;this.approved=approved;this.approvedTs=approvedTs;this.referenceName=referenceName;}}
    public static List<Record> recent(ExperimentsDb db,int limit){List<Record> out=new ArrayList<>();int n=Math.max(1,Math.min(limit,1000));try(Cursor c=db.getReadableDatabase().rawQuery("SELECT id,ts,payload,protocol,analytics,approved,approved_ts,reference_name FROM experiments ORDER BY id DESC LIMIT "+n,null)){while(c.moveToNext())out.add(read(c));}return out;}public static Record byId(ExperimentsDb db,long id){try(Cursor c=db.getReadableDatabase().rawQuery("SELECT id,ts,payload,protocol,analytics,approved,approved_ts,reference_name FROM experiments WHERE id=?",new String[]{String.valueOf(id)})){return c.moveToFirst()?read(c):null;}}private static Record read(Cursor c){return new Record(c.getLong(0),c.getString(1),c.getString(2),c.getString(3),c.getString(4),c.getInt(5)!=0,c.getString(6),c.getString(7));}
}
