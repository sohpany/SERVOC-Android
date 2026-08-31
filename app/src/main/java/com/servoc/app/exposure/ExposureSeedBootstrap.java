package com.servoc.app.exposure;

import android.content.Context;
import com.servoc.app.data.ExperimentsDb;
import java.io.InputStream;

/** Loads versioned authoritative exposure seeds bundled into HazRob builds. */
public final class ExposureSeedBootstrap {
    private ExposureSeedBootstrap(){}

    public static ExposureSeedImporter.Result ensureNioshIdlh(Context context,ExperimentsDb db){
        return ensure(context,db,"generated/exposure/niosh_idlh.json","exposure_niosh_idlh_sha256",true);
    }

    public static ExposureSeedImporter.Result ensureOshaZ1(Context context,ExperimentsDb db){
        return ensure(context,db,"generated/exposure/osha_z1/osha_z1.json","exposure_osha_z1_sha256",false);
    }

    private static ExposureSeedImporter.Result ensure(Context context,ExperimentsDb db,String asset,String metaKey,boolean niosh){
        try{
            String hash;try(InputStream h=context.getAssets().open(asset)){hash=ExposureSeedImporter.sha256(h);}
            if(!ExposureSeedImporter.needsImport(db.getReadableDatabase(),metaKey,hash))return null;
            try(InputStream in=context.getAssets().open(asset)){return niosh?ExposureSeedImporter.importNioshIdlh(db.getWritableDatabase(),in):ExposureSeedImporter.importOshaZ1(db.getWritableDatabase(),in);}
        }catch(Exception e){
            db.putMeta(metaKey+"_last_error",e.getClass().getSimpleName()+": "+String.valueOf(e.getMessage()));return null;
        }
    }
}
