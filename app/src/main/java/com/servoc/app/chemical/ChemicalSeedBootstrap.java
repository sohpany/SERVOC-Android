package com.servoc.app.chemical;

import android.content.Context;
import com.servoc.app.data.ExperimentsDb;
import com.servoc.app.exposure.ExposureSeedBootstrap;
import java.io.InputStream;

/** Loads versioned offline NIOSH scientific seeds bundled into a HazRob build. */
public final class ChemicalSeedBootstrap {
    private ChemicalSeedBootstrap(){}

    public static ChemicalSeedImporter.Result ensureNiosh(Context context,ExperimentsDb db){
        final String asset="generated/niosh/niosh_chemicals.json";
        ChemicalSeedImporter.Result result=null;
        try{
            String hash;try(InputStream h=context.getAssets().open(asset)){hash=ChemicalSeedImporter.sha256(h);}
            if(ChemicalSeedImporter.needsImport(db.getReadableDatabase(),hash)){
                try(InputStream in=context.getAssets().open(asset)){result=ChemicalSeedImporter.importNioshIdentity(db.getWritableDatabase(),in);}
            }
        }catch(Exception e){
            db.putMeta("chemical_seed_niosh_last_error",e.getClass().getSimpleName()+": "+String.valueOf(e.getMessage()));
        }

        // Import exposure data only after the identity table has been populated so CAS matching is deterministic.
        ExposureSeedBootstrap.ensureNioshIdlh(context,db);
        return result;
    }
}
