package com.servoc.app.exposure;

import android.content.Context;

import com.servoc.app.data.ExperimentsDb;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Loads the versioned official NIOSH NPG LFL/UEL seed.
 *
 * A bundled asset is preferred. During HR-0101-047 transition the exact generated seed can also be
 * fetched from the controlled Render artifact endpoint, but only when its SHA-256 matches the pinned
 * value below. Once imported, SQLite remains the offline runtime source; no live web lookup is used
 * for hazard decisions.
 */
public final class GasFlammabilitySeedBootstrap {
    public static final String ASSET="generated/flammability/niosh_npg_flammability.json";
    public static final String REMOTE_URL="https://hazrob-0101-flammability-seed.onrender.com/flammability/niosh_npg_flammability.json";
    public static final String PINNED_SHA256="457b9a1d88b8c54d8863772ca3741f8d2afd152ebd0a9fc007ccb106f4d50434";
    private GasFlammabilitySeedBootstrap(){}

    public static GasFlammabilitySeedImporter.Result ensure(Context context,ExperimentsDb db){
        try{
            byte[] bytes=readBundled(context);String origin="bundled_asset";
            if(bytes==null){
                if(!GasFlammabilitySeedImporter.needsImport(db.getReadableDatabase(),PINNED_SHA256))return null;
                bytes=downloadPinned();origin="pinned_remote_fallback";
            }
            String hash=GasFlammabilitySeedImporter.sha256(new ByteArrayInputStream(bytes));
            if(!PINNED_SHA256.equalsIgnoreCase(hash))throw new IllegalStateException("flammability seed SHA-256 mismatch");
            if(!GasFlammabilitySeedImporter.needsImport(db.getReadableDatabase(),hash))return null;
            GasFlammabilitySeedImporter.Result result=GasFlammabilitySeedImporter.importSeed(db.getWritableDatabase(),new ByteArrayInputStream(bytes));
            db.putMeta("flammability_niosh_npg_bootstrap_origin",origin);return result;
        }catch(Exception e){db.putMeta("flammability_niosh_npg_last_error",e.getClass().getSimpleName()+": "+String.valueOf(e.getMessage()));return null;}
    }

    private static byte[] readBundled(Context context){try(InputStream in=context.getAssets().open(ASSET)){return readAll(in);}catch(Exception ignored){return null;}}
    private static byte[] downloadPinned()throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL(REMOTE_URL).openConnection();c.setConnectTimeout(12_000);c.setReadTimeout(20_000);c.setRequestProperty("Accept","application/json");c.setRequestProperty("User-Agent","HazRob-0.10.1-HR047");
        try{int code=c.getResponseCode();if(code<200||code>=300)throw new IllegalStateException("flammability seed HTTP "+code);try(InputStream in=c.getInputStream()){byte[] bytes=readAll(in);String hash=GasFlammabilitySeedImporter.sha256(new ByteArrayInputStream(bytes));if(!PINNED_SHA256.equalsIgnoreCase(hash))throw new IllegalStateException("remote flammability seed SHA-256 mismatch");return bytes;}}finally{c.disconnect();}
    }
    private static byte[] readAll(InputStream in)throws Exception{ByteArrayOutputStream out=new ByteArrayOutputStream(256*1024);byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);return out.toByteArray();}
}
