package com.servoc.app.field;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.provider.DocumentsContract;

import org.json.JSONObject;
import org.json.JSONArray;

import com.servoc.app.data.ExperimentsDb;
import android.database.Cursor;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Persistent user-selected HazRob library root.
 * The root URI is obtained through ACTION_OPEN_DOCUMENT_TREE and persistable SAF permission,
 * so it is not tied to app-private storage.
 */
public final class HazRobUserLibrary {
    public static final String PREF_ROOT_URI="hazrob_user_library_root_uri";
    public static final int SCHEMA_VERSION=1;
    private static final String MANIFEST="hazrob-library.json";

    private final Context context;
    private final SharedPreferences prefs;

    public HazRobUserLibrary(Context context){
        this.context=context.getApplicationContext();
        this.prefs=this.context.getSharedPreferences("servoc",Context.MODE_PRIVATE);
    }

    public static Intent createPickerIntent(){
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION|Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        return i;
    }

    public synchronized void attach(Uri treeUri) throws Exception {
        if(treeUri==null)throw new IllegalArgumentException("library folder required");
        ContentResolver r=context.getContentResolver();
        r.takePersistableUriPermission(treeUri,Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        Uri doc=DocumentsContract.buildDocumentUriUsingTree(treeUri,DocumentsContract.getTreeDocumentId(treeUri));
        if(!canOpen(r,doc))throw new IllegalStateException("selected HazRob library folder is not readable");
        prefs.edit().putString(PREF_ROOT_URI,treeUri.toString()).apply();
        ensureManifest();
    }

    public synchronized void detach(){prefs.edit().remove(PREF_ROOT_URI).apply();}
    public synchronized boolean attached(){return root()!=null;}
    public synchronized Uri root(){String raw=prefs.getString(PREF_ROOT_URI,"");if(raw==null||raw.trim().isEmpty())return null;try{return Uri.parse(raw);}catch(Exception e){return null;}}

    public synchronized JSONObject manifest() throws Exception {
        Uri file=find(MANIFEST);
        if(file==null)return null;
        try(InputStream in=context.getContentResolver().openInputStream(file)){
            if(in==null)return null;
            byte[] buf=new byte[4096];StringBuilder s=new StringBuilder();int n;
            while((n=in.read(buf))!=-1)s.append(new String(buf,0,n,StandardCharsets.UTF_8));
            return new JSONObject(s.toString());
        }
    }

    public synchronized void ensureManifest() throws Exception {
        JSONObject existing=manifest();
        if(existing!=null){
            int schema=existing.optInt("schema_version",0);
            if(schema>SCHEMA_VERSION)throw new IllegalStateException("HazRob library was created by a newer app");
            if(schema<=0)throw new IllegalStateException("invalid HazRob library manifest");
            return;
        }
        JSONObject m=new JSONObject();
        m.put("type","HazRob User Library");
        m.put("schema_version",SCHEMA_VERSION);
        m.put("storage_contract","SAF-persistable-tree");
        m.put("reports_are_derived",true);
        writeText(MANIFEST,m.toString(2));
    }

    /**
     * Writes a portable user-domain snapshot. It contains sites, field sessions and
     * mappings only; developer experiments/calibration/fingerprints/reference tables
     * are intentionally excluded.
     */
    public synchronized void syncUserIndex(ExperimentsDb db) throws Exception {
        if(db==null)throw new IllegalArgumentException("db required");
        JSONObject root=new JSONObject();root.put("schema_version",SCHEMA_VERSION);root.put("type","HazRob User Field Index");
        JSONArray sites=new JSONArray();
        try(Cursor c=db.getReadableDatabase().rawQuery("SELECT site_id,name,notes,created_ts,updated_ts FROM sites ORDER BY created_ts",null)){
            while(c.moveToNext()){JSONObject x=new JSONObject();x.put("site_id",c.getString(0));x.put("name",c.getString(1));x.put("notes",c.getString(2));x.put("created_ts",c.getString(3));x.put("updated_ts",c.getString(4));sites.put(x);}
        }
        JSONArray sessions=new JSONArray();
        try(Cursor c=db.getReadableDatabase().rawQuery("SELECT field_session_id,site_id,name,started_ts,ended_ts,status,created_ts,updated_ts FROM field_sessions ORDER BY started_ts",null)){
            while(c.moveToNext()){JSONObject x=new JSONObject();x.put("field_session_id",c.getString(0));x.put("site_id",c.getString(1));x.put("name",c.getString(2));x.put("started_ts",c.getString(3));x.put("ended_ts",c.getString(4));x.put("status",c.getString(5));x.put("created_ts",c.getString(6));x.put("updated_ts",c.getString(7));sessions.put(x);}
        }
        JSONArray links=new JSONArray();
        try(Cursor c=db.getReadableDatabase().rawQuery("SELECT field_session_id,measurement_session_id,linked_ts FROM field_session_measurements ORDER BY linked_ts",null)){
            while(c.moveToNext()){JSONObject x=new JSONObject();x.put("field_session_id",c.getString(0));x.put("measurement_session_id",c.getString(1));x.put("linked_ts",c.getString(2));links.put(x);}
        }
        root.put("sites",sites);root.put("field_sessions",sessions);root.put("measurement_links",links);
        writeText("field-index.json",root.toString(2));
    }

    public synchronized void writeText(String displayName,String text) throws Exception {
        Uri root=root();if(root==null)throw new IllegalStateException("HazRob user library is not attached");
        ContentResolver r=context.getContentResolver();
        String treeId=DocumentsContract.getTreeDocumentId(root);
        Uri parent=DocumentsContract.buildDocumentUriUsingTree(root,treeId);
        Uri file=find(displayName);
        if(file==null)file=DocumentsContract.createDocument(r,parent,"application/json",displayName);
        if(file==null)throw new IllegalStateException("cannot create HazRob library file");
        try(OutputStream out=r.openOutputStream(file,"wt")){
            if(out==null)throw new IllegalStateException("cannot write HazRob library file");
            out.write((text==null?"":text).getBytes(StandardCharsets.UTF_8));out.flush();
        }
    }

    private Uri find(String displayName) throws Exception {
        Uri root=root();if(root==null)return null;
        ContentResolver r=context.getContentResolver();String treeId=DocumentsContract.getTreeDocumentId(root);
        Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(root,treeId);
        try(android.database.Cursor c=r.query(children,new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_DISPLAY_NAME},null,null,null)){
            if(c==null)return null;while(c.moveToNext())if(displayName.equals(c.getString(1)))return DocumentsContract.buildDocumentUriUsingTree(root,c.getString(0));
        }
        return null;
    }

    private static boolean canOpen(ContentResolver r,Uri doc){
        try(android.database.Cursor c=r.query(doc,new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID},null,null,null)){return c!=null&&c.moveToFirst();}catch(Exception e){return false;}
    }
}
