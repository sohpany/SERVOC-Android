package com.servoc.app.exposure;

import android.content.Context;
import com.servoc.app.data.ExperimentsDb;
import java.io.InputStream;

/** Loads versioned authoritative exposure seeds bundled into HazRob builds. */
public final class ExposureSeedBootstrap {
    private ExposureSeedBootstrap(){}

    public static ExposureSeedImporter.Result ensureNioshIdlh(Context context,ExperimentsDb db){
        final String asset="generated/exposure/niosh_idlh.json",metaKey="exposure_niosh_idlh_sha256";
        try{
            String hash;try(InputStream h=context.getAssets().open(asset)){hash=ExposureSeedImporter.sha256(h);}
            if(!ExposureSeedImporter.needsImport(db.getReadableDatabase(),metaKey,hash))return null;
            try(InputStream in=context.getAssets().open(asset)){return ExposureSeedImporter.importNioshIdlh(db.getWritableDatabase(),in);}
        }catch(Exception e){
            db.putMeta("exposure_niosh_idlh_last_error",e.getClass().getSimpleName()+": "+String.valueOf(e.getMessage()));return null;
        }
    }
}
