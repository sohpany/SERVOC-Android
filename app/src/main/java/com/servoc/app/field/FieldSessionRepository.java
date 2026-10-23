package com.servoc.app.field;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.servoc.app.data.ExperimentsDb;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
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

    /**
     * Closes only an ACTIVE field session whose explicitly linked measurement sessions have all
     * been finalized. A field container must never close around a still-running acquisition.
     */
    public boolean closeFieldSession(String fieldSessionId){
        String id=requiredId(fieldSessionId);SQLiteDatabase w=db.getWritableDatabase();w.beginTransaction();
        try{
            try(Cursor c=w.rawQuery("SELECT 1 FROM field_sessions WHERE field_session_id=? AND status=? LIMIT 1",new String[]{id,STATUS_ACTIVE})){if(!c.moveToFirst())return false;}
            String openSql="SELECT 1 FROM field_session_measurements fsm JOIN measurement_sessions ms ON ms.session_id=fsm.measurement_session_id WHERE fsm.field_session_id=? AND (ms.ended_ts IS NULL OR TRIM(ms.ended_ts)='') LIMIT 1";
            try(Cursor c=w.rawQuery(openSql,new String[]{id})){if(c.moveToFirst())return false;}
            ContentValues v=new ContentValues();String now=isoNow();v.put("ended_ts",now);v.put("status",STATUS_CLOSED);v.put("updated_ts",now);
            int n=w.update("field_sessions",v,"field_session_id=? AND status=?",new String[]{id,STATUS_ACTIVE});w.setTransactionSuccessful();return n==1;
        }finally{w.endTransaction();}
    }

    public FieldSession activeFieldSession(){
        try(Cursor c=db.getReadableDatabase().rawQuery("SELECT field_session_id,site_id,name,started_ts,ended_ts,status FROM field_sessions WHERE status=? ORDER BY started_ts DESC LIMIT 1",new String[]{STATUS_ACTIVE})){
            return c.moveToFirst()?fieldSession(c):null;
        }
    }

    /** Lightweight identity lookup; report bundles must be resolved OFF the UI thread. */
    public FieldSession fieldSessionById(String fieldSessionId){
        String id=requiredId(fieldSessionId);
        try(Cursor c=db.getReadableDatabase().rawQuery(
            "SELECT field_session_id,site_id,name,started_ts,ended_ts,status FROM field_sessions WHERE field_session_id=? LIMIT 1",
            new String[]{id})){return c.moveToFirst()?fieldSession(c):null;}
    }

    public List<FieldSession> sessionsForSite(String siteId){
        List<FieldSession> out=new ArrayList<>();
        try(Cursor c=db.getReadableDatabase().rawQuery("SELECT field_session_id,site_id,name,started_ts,ended_ts,status FROM field_sessions WHERE site_id=? ORDER BY started_ts DESC,field_session_id DESC",new String[]{requiredId(siteId)})){
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
            long row=w.insertWithOnConflict("field_session_measurements",null,v,SQLiteDatabase.CONFLICT_IGNORE);
            w.setTransactionSuccessful();return row!=-1;
        }finally{w.endTransaction();}
    }

    /** Single grouped query for session counts: avoids N+1 DB reads during tab rendering. */
    public Map<String,Integer> measurementCountsForSite(String siteId){
        Map<String,Integer> out=new HashMap<>();
        String sql="SELECT fsm.field_session_id,COUNT(*) FROM field_session_measurements fsm " +
            "JOIN field_sessions fs ON fs.field_session_id=fsm.field_session_id " +
            "WHERE fs.site_id=? GROUP BY fsm.field_session_id";
        try(Cursor c=db.getReadableDatabase().rawQuery(sql,new String[]{requiredId(siteId)})){
            while(c.moveToNext())out.put(c.getString(0),c.getInt(1));
        }
        return out;
    }

    /** Explicit user approval, independent of old ACTIVE/CLOSED storage state. */
    public boolean isApproved(String fieldSessionId){
        try(Cursor c=db.getReadableDatabase().rawQuery(
            "SELECT 1 FROM field_session_approvals a JOIN field_sessions f ON f.field_session_id=a.field_session_id WHERE a.field_session_id=? AND f.status=? LIMIT 1",
            new String[]{requiredId(fieldSessionId),STATUS_CLOSED})){return c.moveToFirst();}
    }
    public List<FieldSession> approvedSessionsForSite(String siteId){
        List<FieldSession> out=new ArrayList<>();
        String sql="SELECT f.field_session_id,f.site_id,f.name,f.started_ts,f.ended_ts,f.status "+
            "FROM field_sessions f JOIN field_session_approvals a ON a.field_session_id=f.field_session_id "+
            "WHERE f.site_id=? AND f.status=? ORDER BY f.started_ts DESC,f.field_session_id DESC";
        try(Cursor c=db.getReadableDatabase().rawQuery(sql,new String[]{requiredId(siteId),STATUS_CLOSED})){
            while(c.moveToNext())out.add(fieldSession(c));
        }
        return out;
    }
    /** Approval requires exactly one completed USER/FIELD acquisition. */
    public boolean canApprove(String fieldSessionId){
        if(isApproved(fieldSessionId))return false;
        String sql="SELECT COUNT(*),MIN(CASE WHEN ms.ended_ts IS NOT NULL AND TRIM(ms.ended_ts)<>'' AND "+
            "ms.operator_role='USER' AND ms.kind LIKE 'field_%' "+
            "AND EXISTS(SELECT 1 FROM telemetry_samples t WHERE t.session_id=ms.session_id) THEN 1 ELSE 0 END) "+
            "FROM field_session_measurements fsm JOIN measurement_sessions ms ON ms.session_id=fsm.measurement_session_id "+
            "JOIN field_sessions fs ON fs.field_session_id=fsm.field_session_id "+
            "WHERE fsm.field_session_id=? AND fs.status=?";
        try(Cursor c=db.getReadableDatabase().rawQuery(sql,new String[]{requiredId(fieldSessionId),STATUS_CLOSED})){
            return c.moveToFirst()&&c.getInt(0)==1&&c.getInt(1)==1;
        }
    }
    /** Transactional, no legacy record is rewritten or silently promoted. */
    public boolean approve(String fieldSessionId){
        String id=requiredId(fieldSessionId);
        SQLiteDatabase w=db.getWritableDatabase();w.beginTransaction();
        try{
            String sql="SELECT fsm.measurement_session_id FROM field_session_measurements fsm "+
                "JOIN field_sessions fs ON fs.field_session_id=fsm.field_session_id "+
                "JOIN measurement_sessions ms ON ms.session_id=fsm.measurement_session_id "+
                "WHERE fsm.field_session_id=? AND fs.status=? AND ms.operator_role='USER' "+
                "AND ms.kind LIKE 'field_%' AND ms.ended_ts IS NOT NULL AND TRIM(ms.ended_ts)<>'' "+
                "AND EXISTS(SELECT 1 FROM telemetry_samples t WHERE t.session_id=ms.session_id)";
            String measurement="";
            try(Cursor c=w.rawQuery(sql,new String[]{id,STATUS_CLOSED})){
                if(!c.moveToFirst())return false;
                measurement=c.getString(0);if(c.moveToNext())return false;
            }
            try(Cursor c=w.rawQuery("SELECT COUNT(*) FROM field_session_measurements WHERE field_session_id=?",new String[]{id})){
                if(!c.moveToFirst()||c.getInt(0)!=1)return false;
            }
            ContentValues v=new ContentValues();v.put("field_session_id",id);
            v.put("measurement_session_id",measurement);v.put("approved_ts",isoNow());
            long inserted=w.insertWithOnConflict("field_session_approvals",null,v,SQLiteDatabase.CONFLICT_IGNORE);
            w.setTransactionSuccessful();return inserted!=-1;
        }finally{w.endTransaction();}
    }

    public List<String> measurementSessionIds(String fieldSessionId){
        List<String> out=new ArrayList<>();
        try(Cursor c=db.getReadableDatabase().rawQuery("SELECT measurement_session_id FROM field_session_measurements WHERE field_session_id=? ORDER BY linked_ts,measurement_session_id",new String[]{requiredId(fieldSessionId)})){
            while(c.moveToNext())out.add(c.getString(0));
        }
        return out;
    }

    /** Exact reverse lookup used by SmartReport UI routing; empty means the measurement is not in a user Field Session. */
    public String fieldSessionIdForMeasurement(String measurementSessionId){
        String ms=measurementSessionId==null?"":measurementSessionId.trim();if(ms.isEmpty())return "";
        try(Cursor c=db.getReadableDatabase().rawQuery("SELECT field_session_id FROM field_session_measurements WHERE measurement_session_id=? ORDER BY linked_ts DESC,field_session_id DESC LIMIT 1",new String[]{ms})){
            return c.moveToFirst()?nz(c.getString(0)):"";
        }
    }

    /**
     * Permanently deletes a CLOSED user's Field Session and its own measurement data.
     * Developer/reference/scientific libraries, calibrations, and other sessions are excluded.
     * This is atomic in SQLite; caller separately purges portable SAF archives.
     */
    public boolean deleteFieldSession(String fieldSessionId){
        String id=requiredId(fieldSessionId);SQLiteDatabase w=db.getWritableDatabase();w.beginTransaction();
        try{
            try(Cursor c=w.rawQuery("SELECT status FROM field_sessions WHERE field_session_id=? LIMIT 1",new String[]{id})){
                if(!c.moveToFirst()||!STATUS_CLOSED.equals(c.getString(0)))return false;
            }
            java.util.List<String> ids=new java.util.ArrayList<>();
            try(Cursor c=w.rawQuery("SELECT m.session_id,m.kind,m.operator_role FROM measurement_sessions m JOIN field_session_measurements f ON f.measurement_session_id=m.session_id WHERE f.field_session_id=?",new String[]{id})){
                while(c.moveToNext()){
                    String ms=c.getString(0),kind=c.getString(1),role=c.getString(2);
                    if(!"USER".equals(role)||kind==null||!kind.startsWith("field_"))
                        throw new IllegalStateException("Refusing deletion: non-user/reference measurement linked to Field Session");
                    ids.add(ms);
                }
            }
            // Do not permit any referenced/approved capture to be removed through a user UI action.
            for(String ms:ids){
                try(Cursor c=w.rawQuery("SELECT 1 FROM experiments e LEFT JOIN reference_queue q ON q.experiment_id=e.id WHERE e.session_id=? AND (e.approved<>0 OR e.approved_ts IS NOT NULL OR e.reference_kind IS NOT NULL OR q.experiment_id IS NOT NULL) LIMIT 1",new String[]{ms})){
                    if(c.moveToFirst())throw new IllegalStateException("Refusing deletion: published or controlled reference");
                }
                try(Cursor c=w.rawQuery("SELECT 1 FROM captures WHERE session_id=? LIMIT 1",new String[]{ms})){
                    if(c.moveToFirst())throw new IllegalStateException("Refusing deletion: scientific capture uses this identity");
                }
            }
            w.delete("field_session_approvals","field_session_id=?",new String[]{id});
            w.delete("field_session_measurements","field_session_id=?",new String[]{id});
            for(String ms:ids){
                String[] arg={ms};
                w.delete("experiments","session_id=?",arg);
                w.delete("events","session_id=?",arg);
                w.delete("chemcore_decisions","session_id=?",arg);
                w.delete("telemetry_samples","session_id=?",arg);
                w.delete("media_assets","session_id=?",arg);
                w.delete("measurement_sessions","session_id=?",arg);
            }
            int n=w.delete("field_sessions","field_session_id=?",new String[]{id});
            if(n!=1)throw new IllegalStateException("Field Session disappeared during deletion");
            ContentValues tombstone=new ContentValues();tombstone.put("k","deleted_field_session_"+id);tombstone.put("v",isoNow());
            w.insertWithOnConflict("meta",null,tombstone,SQLiteDatabase.CONFLICT_REPLACE);
            w.setTransactionSuccessful();return true;
        }finally{w.endTransaction();}
    }

    /** Explicit retry: resets only unapproved USER/FIELD measurements; keeps scientific records untouched. */
    public boolean restartUnapproved(String fieldSessionId){
        String id=requiredId(fieldSessionId);SQLiteDatabase w=db.getWritableDatabase();w.beginTransaction();
        try{
            try(Cursor c=w.rawQuery("SELECT 1 FROM field_session_approvals WHERE field_session_id=? LIMIT 1",new String[]{id})){
                if(c.moveToFirst())return false;
            }
            try(Cursor c=w.rawQuery("SELECT 1 FROM field_sessions WHERE field_session_id=? AND status=? LIMIT 1",
                new String[]{id,STATUS_CLOSED})){if(!c.moveToFirst())return false;}
            try(Cursor c=w.rawQuery("SELECT 1 FROM field_sessions WHERE status=? LIMIT 1",new String[]{STATUS_ACTIVE})){
                if(c.moveToFirst())return false;
            }
            List<String> ids=new ArrayList<>();
            try(Cursor c=w.rawQuery(
                "SELECT ms.session_id,ms.kind,ms.operator_role FROM measurement_sessions ms JOIN field_session_measurements fsm "+
                "ON fsm.measurement_session_id=ms.session_id WHERE fsm.field_session_id=?",new String[]{id})){
                while(c.moveToNext()){
                    String measurementId=c.getString(0);
                    if(!FieldSessionPolicy.mayAttachMeasurement(c.getString(1),c.getString(2)))
                        throw new IllegalStateException("Reference or developer capture must not be reset");
                    ids.add(measurementId);
                }
            }
            for(String ms:ids){
                try(Cursor c=w.rawQuery(
                    "SELECT 1 FROM experiments e LEFT JOIN reference_queue q ON q.experiment_id=e.id "+
                    "WHERE e.session_id=? AND (e.approved<>0 OR e.approved_ts IS NOT NULL OR e.reference_kind IS NOT NULL OR q.experiment_id IS NOT NULL) LIMIT 1",
                    new String[]{ms})){if(c.moveToFirst())throw new IllegalStateException("Approved scientific artifact cannot be reset");}
                try(Cursor c=w.rawQuery("SELECT 1 FROM captures WHERE session_id=? LIMIT 1",new String[]{ms})){
                    if(c.moveToFirst())throw new IllegalStateException("Scientific capture cannot be reset");
                }
            }
            w.delete("field_session_measurements","field_session_id=?",new String[]{id});
            for(String ms:ids){
                String[] arg={ms};
                w.delete("experiments","session_id=?",arg);w.delete("events","session_id=?",arg);
                w.delete("chemcore_decisions","session_id=?",arg);w.delete("telemetry_samples","session_id=?",arg);
                w.delete("media_assets","session_id=?",arg);w.delete("measurement_sessions","session_id=?",arg);
            }
            ContentValues updated=new ContentValues();
            updated.put("status",STATUS_ACTIVE);updated.putNull("ended_ts");updated.put("updated_ts",isoNow());
            int changed=w.update("field_sessions",updated,"field_session_id=? AND status=?",
                new String[]{id,STATUS_CLOSED});
            if(changed!=1)throw new IllegalStateException("Concurrent field session state change");
            w.setTransactionSuccessful();return true;
        }finally{w.endTransaction();}
    }

    /** Delete a site only after all of its field sessions have been removed. */
    public boolean deleteEmptySite(String siteId){
        String id=requiredId(siteId);SQLiteDatabase w=db.getWritableDatabase();w.beginTransaction();
        try{
            try(Cursor c=w.rawQuery("SELECT 1 FROM field_sessions WHERE site_id=? LIMIT 1",new String[]{id})){
                if(c.moveToFirst())return false;
            }
            int n=w.delete("sites","site_id=?",new String[]{id});
            if(n!=1)return false;
            ContentValues tombstone=new ContentValues();tombstone.put("k","deleted_site_"+id);tombstone.put("v",isoNow());
            w.insertWithOnConflict("meta",null,tombstone,SQLiteDatabase.CONFLICT_REPLACE);
            w.setTransactionSuccessful();return true;
        }finally{w.endTransaction();}
    }

    private boolean siteExists(String id){try(Cursor c=db.getReadableDatabase().rawQuery("SELECT 1 FROM sites WHERE site_id=? LIMIT 1",new String[]{id})){return c.moveToFirst();}}
    private static FieldSession fieldSession(Cursor c){return new FieldSession(c.getString(0),c.getString(1),c.getString(2),c.getString(3),nz(c.getString(4)),c.getString(5));}
    private static String requiredId(String s){String v=s==null?"":s.trim();if(v.isEmpty())throw new IllegalArgumentException("id required");return v;}
    private static String requiredName(String s,String what){String v=s==null?"":s.trim();if(v.isEmpty())throw new IllegalArgumentException(what+" required");return v;}
    private static String nz(String s){return s==null?"":s;}
    private static String isoNow(){SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",Locale.US);f.setTimeZone(TimeZone.getTimeZone("UTC"));return f.format(new Date());}
}
