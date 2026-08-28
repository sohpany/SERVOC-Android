package com.servoc.app.data;

import android.database.Cursor;
import java.util.ArrayList;
import java.util.List;

public final class ExperimentQueries {
    private ExperimentQueries(){}

    public static final class Record {
        public final long id;public final String ts,payload,protocol,analytics;
        public Record(long id,String ts,String payload,String protocol,String analytics){this.id=id;this.ts=ts;this.payload=payload;this.protocol=protocol;this.analytics=analytics;}
    }

    public static List<Record> recent(ExperimentsDb db,int limit){
        List<Record> out=new ArrayList<>();
        int n=Math.max(1,Math.min(limit,1000));
        try(Cursor c=db.getReadableDatabase().rawQuery("SELECT id,ts,payload,protocol,analytics FROM experiments ORDER BY id DESC LIMIT "+n,null)){
            while(c.moveToNext())out.add(new Record(c.getLong(0),c.getString(1),c.getString(2),c.getString(3),c.getString(4)));
        }
        return out;
    }
}
