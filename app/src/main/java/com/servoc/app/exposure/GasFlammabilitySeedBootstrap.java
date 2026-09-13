package com.servoc.app.exposure;

import android.content.Context;

import com.servoc.app.data.ExperimentsDb;

import java.io.InputStream;

/** Loads the versioned official NIOSH NPG LFL/UEL seed bundled with HazRob. */
public final class GasFlammabilitySeedBootstrap {
    private GasFlammabilitySeedBootstrap(){}

    public static GasFlammabilitySeedImporter.Result ensure(Context context,ExperimentsDb db){
        final String asset="generated/flammability/niosh_npg_flammability.json";
        try{
            String hash;try(InputStream h=context.getAssets().open(asset)){hash=GasFlammabilitySeedImporter.sha256(h);}
            if(!GasFlammabilitySeedImporter.needsImport(db.getReadableDatabase(),hash))return null;
            try(InputStream in=context.getAssets().open(asset)){return GasFlammabilitySeedImporter.importSeed(db.getWritableDatabase(),in);}
        }catch(Exception e){db.putMeta("flammability_niosh_npg_last_error",e.getClass().getSimpleName()+": "+String.valueOf(e.getMessage()));return null;}
    }
}
