package com.servoc.app.github;

import android.content.ContentValues;
import android.database.Cursor;
import com.servoc.app.data.ExperimentsDb;

/** Audit trail for developer reference-bank publication. No credential material is stored. */
public final class ReferencePublishAudit {
    private ReferencePublishAudit(){}

    public static long record(ExperimentsDb db,long experimentId,String status,String createdTs,String payloadHash,String publishedRef){
        if(db==null||experimentId<=0)throw new IllegalArgumentException("experiment");
        ContentValues v=new ContentValues();v.put("experiment_id",experimentId);v.put("status",status==null?"UNKNOWN":status);v.put("created_ts",createdTs==null?"":createdTs);if(payloadHash!=null)v.put("payload_hash",payloadHash);if(publishedRef!=null)v.put("published_ref",publishedRef);
        return db.getWritableDatabase().insertOrThrow("reference_queue",null,v);
    }

    public static String latestStatus(ExperimentsDb db,long experimentId){
        if(db==null||experimentId<=0)return"";
        try(Cursor c=db.getReadableDatabase().rawQuery("SELECT status,published_ref,created_ts FROM reference_queue WHERE experiment_id=? ORDER BY id DESC LIMIT 1",new String[]{String.valueOf(experimentId)})){
            if(!c.moveToFirst())return"";String s=c.getString(0),ref=c.isNull(1)?"":c.getString(1),ts=c.isNull(2)?"":c.getString(2);StringBuilder x=new StringBuilder(s==null?"":s);if(!ref.isEmpty())x.append(" • ").append(ref);if(!ts.isEmpty())x.append(" • ").append(ts);return x.toString();
        }catch(Exception e){return"";}
    }
}
