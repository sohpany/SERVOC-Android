package com.servoc.app.chemcore.library;

import android.content.Context;

import com.servoc.app.ReleaseAudit;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** One-time offline bootstrap for the verified, pre-release Stable library in HR069-R1 APK.
 * External/previously installed verified Stable always takes precedence and is not replaced.
 * The bundled Stable has research response-priors but no fabricated M4 calibrations.
 */
public final class ChemCoreEmbeddedStableInstaller {
    public static final String ASSET_ROOT="generated/chemcore/embedded_stable/";
    public static final String LIBRARY_VERSION="1.0.0-hr069r1";
    private ChemCoreEmbeddedStableInstaller(){}

    public static ChemCoreLibraryStore.InstallResult ensureInstalled(Context context) {
        if(context==null)return new ChemCoreLibraryStore.InstallResult(false,"context required","");
        Context app=context.getApplicationContext();
        ChemCoreLibraryStore store=new ChemCoreLibraryStore(app);
        String old=store.activeVersion();
        // APK installations must never downgrade a user-activated Stable update, even if
        // an APK is reinstalled or the remote delivery channel is temporarily unavailable.
        if(!old.isEmpty()){
            if(store.activePayload()!=null)return new ChemCoreLibraryStore.InstallResult(false,"existing verified library retains precedence",old);
            return new ChemCoreLibraryStore.InstallResult(false,"installed library pointer invalid; do not silently downgrade",old);
        }
        if(ChemCoreLibraryManager.measurementOrAnalysisActive(null))
            return new ChemCoreLibraryStore.InstallResult(false,"capture active; defer bundled Stable activation","");
        File stage=null;
        try{
            JSONObject j=new JSONObject(new String(readAsset(app,ASSET_ROOT+"manifest.json",32768),StandardCharsets.UTF_8));
            ChemCoreLibraryManifest manifest=ChemCoreLibraryManifest.fromJson(j);
            if(!LIBRARY_VERSION.equals(manifest.libraryVersion))
                throw new IllegalStateException("unexpected embedded Stable library version");
            ChemCoreLibraryVerifier.VerificationResult v=ChemCoreLibraryVerifier.verifyManifestOnly(
                    manifest,ReleaseAudit.VERSION,ChemCoreLibraryManager.REQUIRED_SAMPLING_PROFILE);
            if(!v.accepted)throw new IllegalStateException(v.reason);
            if(!"chemcore-stable-snapshot.json".equals(manifest.payloadFile))
                throw new IllegalStateException("unexpected embedded Stable payload name");
            stage=File.createTempFile("hr069-embedded-stable-", ".json",app.getCacheDir());
            try(InputStream in=app.getAssets().open(ASSET_ROOT+manifest.payloadFile);
                FileOutputStream out=new FileOutputStream(stage)){
                byte[] b=new byte[32768];
                long copied=0;
                for(int n;(n=in.read(b))!=-1;){
                    if(n==0)continue;copied+=n;
                    if(copied>manifest.payloadSizeBytes)throw new IllegalStateException("embedded Stable payload size limit");
                    out.write(b,0,n);
                }
                out.flush();out.getFD().sync();
                if(copied!=manifest.payloadSizeBytes)throw new IllegalStateException("embedded Stable size mismatch");
            }
            ChemCoreLibraryVerifier.VerificationResult verified=ChemCoreLibraryVerifier.verifyPayload(stage,manifest,ReleaseAudit.VERSION);
            if(!verified.accepted)throw new IllegalStateException(verified.reason);
            return store.installVerifiedSnapshot(manifest,stage,ReleaseAudit.VERSION,
                    ChemCoreLibraryManager.measurementOrAnalysisActive(null),false);
        }catch(Exception e){
            return new ChemCoreLibraryStore.InstallResult(false,"embedded Stable bootstrap failed: "+
                    e.getClass().getSimpleName()+":"+String.valueOf(e.getMessage()),old);
        }finally{if(stage!=null)stage.delete();}
    }

    private static byte[] readAsset(Context context,String path,int limit)throws Exception{
        try(InputStream in=context.getAssets().open(path);ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] b=new byte[8192];for(int n;(n=in.read(b))!=-1;){
                if(n==0)continue;
                if(out.size()+n>limit)throw new IllegalArgumentException("oversized embedded Stable manifest");
                out.write(b,0,n);
            }
            return out.toByteArray();
        }
    }
}
