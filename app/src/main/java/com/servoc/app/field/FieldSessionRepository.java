package com.servoc.app.field;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.servoc.app.data.ExperimentsDb;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

/**
 * User-domain repository for sites and field sessions.
 *
 * This class intentionally has no API for experiments, calibration, fingerprints,
 * reference artifacts, models, or other developer/scientific-library records.
 */
public final class FieldSessionRepository {
    public static final String STATUS_ACTIVE="ACTIVE";
    public static final String STATUS_CLOSED="CLOSED";

    public static final class Site {
        public final String id,name,notes,createdTs,updatedTs;
        Site(String id,String name,String notes,String createdTs,String updatedTs){
            this.id=id;this.name=name;this.notes=notes;this.createdTs=createdTs;this.updatedTs=updatedTs;
        }
    }

    public static final class FieldSession {
        public final String id,siteId,name,startedTs,endedTs,status;
        FieldSession(String id,String siteId,String name,String startedTs,String endedTs,String status){
            this.id=id;this.siteId=siteId;this.name=name;this.startedTs=startedTs;this.endedTs=endedTs;this.status=status;
        }
        public boolean active(){return STATUS_ACTIVE.equals(status);}
    }

    private final ExperimentsDb db;
    public FieldSessionRepository(ExperimentsDb db){if(db==null)throw new IllegalArgumentException("db required");this.db=db;}

    public String createSite(String displayName,String notes){
        String name=requiredName(displayName,"site name");
        String id="SITE-"+UUID.randomUUID().toString();
        String now=isoNow();
        ContentValues v=new ContentValues();v.put("site_id",id);v.put("name",name);v.put("notes",notes==null?"":notes.trim());v.put("created_ts",now);v.put("updated_ts",now);
        db.getWritableDatabase().insertOrThrow("sites",null,v);return id;
    }

    public boolean renameSite(String siteId,String displayName){
        ContentValues v=new ContentValues();v.put("name",requiredName(displayName,"site name"));v.put("updated_ts",isoNow());
        return db.getWritableDatabase().update("sites",v,"site_id=?",new String[]{requiredId(siteId)})==1;
    }

    public List<Site> sites(){
        List<Site> out=new ArrayList<>();
        try(Cursor c=db.getReadableDatabase().rawQuery("SELECT site_id,name,notes,created_ts,updated_ts FROM sites ORDER BY updated_ts DESC,name COLLATE NOCASE",null)){
            while(c.moveToNext())out.add(new Site(c.getString(0),c.getString(1),nz(c.getString(2)),c.getString(3),c.getString(4)));
        }
        return out;
    }

    public String startFieldSession(String siteId,String displayName){
        String sid=requiredId(siteId);if(!siteExists(sid))throw new IllegalArgumentException("site not found");
        SQLiteDatabase w=db.getWritableDatabase();w.beginTransaction();
        try{
            try(Cursor c=w.rawQuery("SELECT 1 FROM field_sessions WHERE status=? LIMIT 1",new String[]{STATUS_ACTIVE})){
                if(c.moveToFirst())throw new IllegalStateException("another field session is already active");
            }
            String id="FS-"+UUID.randomUUID().toString();String now=isoNow();
            ContentValues v=new ContentValues();v.put("field_session_id",id);v.put("site_id",sid);v.put("name",requiredName(displayName,"field session name"));v.put("started_ts",now);v.put("status",STATUS_ACTIVE);v.put("created_ts",now);v.put("updated_ts",now);
            w.insertOrThrow("field_sessions",null,v);w.setTransactionSuccessful();return id;
        }finally{w.endTransaction();}
    }

    public boolean closeFieldSession(String fieldSessionId){
        ContentValues v=new ContentValues();String now=isoNow();v.put("ended_ts",now);v.put("status",STATUS_CLOSED);v.put("updated_ts",now);
        return db.getWritableDatabase().update("field_sessions",v,"field_session_id=? AND status=?",new String[]{requiredId(fieldSessionId),STATUS_ACTIVE})==1;
    }

    public FieldSession activeFieldSession(){
        try(Cursor c=db.getReadableDatabase().rawQuery("SELECT field_session_id,site_id,name,started_ts,ended_ts,status FROM field_sessions WHERE status=? ORDER BY started_ts DESC LIMIT 1",new String[]{STATUS_ACTIVE})){
            return c.moveToFirst()?fieldSession(c):null;
        }
    }

    public List<FieldSession> sessionsForSite(String siteId){
        List<FieldSession> out=new ArrayList<>();
        try(Cursor c=db.getReadableDatabase().rawQuery("SELECT field_session_id,site_id,name,started_ts,ended_ts,status FROM field_sessions WHERE site_id=? ORDER BY started_ts DESC",new String[]{requiredId(siteId)})){
            while(c.moveToNext())out.add(fieldSession(c));
        }
        return out;
    }

    /**
     * Links only USER/FIELD technical acquisitions. Developer sessions are rejected
     * even if a caller accidentally passes their ID.
     */
    public boolean linkMeasurement(String fieldSessionId,String measurementSessionId){
        String fs=requiredId(fieldSessionId),ms=requiredId(measurementSessionId);
        SQLiteDatabase w=db.getWritableDatabase();w.beginTransaction();
        try{
            try(Cursor c=w.rawQuery("SELECT status FROM field_sessions WHERE field_session_id=?",new String[]{fs})){
                if(!c.moveToFirst()||!STATUS_ACTIVE.equals(c.getString(0)))return false;
            }
            try(Cursor c=w.rawQuery("SELECT kind,operator_role FROM measurement_sessions WHERE session_id=?",new String[]{ms})){
                if(!c.moveToFirst())return false;
                String kind=nz(c.getString(0)),role=nz(c.getString(1));
                if(!FieldSessionPolicy.mayAttachMeasurement(kind,role))return false;
            }
            ContentValues v=new ContentValues();v.put("field_session_id",fs);v.put("measurement_session_id",ms);v.put("linked_ts",isoNow());
            long id=w.insertWithOnConflict("field_session_measurements",null,v,SQLiteDatabase.CONFLICT_IGNORE);
            w.setTransactionSuccessful();return id!=-1;
        }finally{w.endTransaction();}
    }

    public List<String> measurementSessionIds(String fieldSessionId){
        List<String> out=new ArrayList<>();
        try(Cursor c=db.getReadableDatabase().rawQuery("SELECT measurement_session_id FROM field_session_measurements WHERE field_session_id=? ORDER BY linked_ts",new String[]{requiredId(fieldSessionId)})){
            while(c.moveToNext())out.add(c.getString(0));
        }
        return out;
    }

    /**
     * Deletes user field metadata/mappings only. It deliberately does not delete
     * measurement_sessions or any developer/scientific-library table.
     */
    public boolean deleteFieldSession(String fieldSessionId){
        String id=requiredId(fieldSessionId);SQLiteDatabase w=db.getWritableDatabase();w.beginTransaction();
        try{
            w.delete("field_session_measurements","field_session_id=?",new String[]{id});
            int n=w.delete("field_sessions","field_session_id=?",new String[]{id});
            w.setTransactionSuccessful();return n==1;
        }finally{w.endTransaction();}
    }

    /** A site can be deleted only after its user field sessions have been removed. */
    public boolean deleteEmptySite(String siteId){
        String id=requiredId(siteId);
        try(Cursor c=db.getReadableDatabase().rawQuery("SELECT 1 FROM field_sessions WHERE site_id=? LIMIT 1",new String[]{id})){
            if(c.moveToFirst())return false;
        }
        return db.getWritableDatabase().delete("sites","site_id=?",new String[]{id})==1;
    }

    private boolean siteExists(String id){try(Cursor c=db.getReadableDatabase().rawQuery("SELECT 1 FROM sites WHERE site_id=? LIMIT 1",new String[]{id})){return c.moveToFirst();}}
    private static FieldSession fieldSession(Cursor c){return new FieldSession(c.getString(0),c.getString(1),c.getString(2),c.getString(3),nz(c.getString(4)),c.getString(5));}
    private static String requiredId(String s){String v=s==null?"":s.trim();if(v.isEmpty())throw new IllegalArgumentException("id required");return v;}
    private static String requiredName(String s,String what){String v=s==null?"":s.trim();if(v.isEmpty())throw new IllegalArgumentException(what+" required");return v;}
    private static String nz(String s){return s==null?"":s;}
    private static String isoNow(){SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'",Locale.US);f.setTimeZone(TimeZone.getTimeZone("UTC"));return f.format(new Date());}
}
