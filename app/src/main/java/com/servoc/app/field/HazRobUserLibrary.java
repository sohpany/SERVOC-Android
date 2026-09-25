package com.servoc.app.field;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.provider.DocumentsContract;

import org.json.JSONObject;

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
