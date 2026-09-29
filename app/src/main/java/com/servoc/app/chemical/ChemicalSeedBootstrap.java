package com.servoc.app.chemical;

import android.content.Context;

import com.servoc.app.chemcore.ChemCoreIdentityCurationRepository;
import com.servoc.app.chemcore.ChemCoreMolecularSeedImporter;
import com.servoc.app.data.ExperimentsDb;
import com.servoc.app.exposure.ExposureSeedBootstrap;
import com.servoc.app.exposure.GasFlammabilitySeedBootstrap;

import java.io.InputStream;

/** Loads versioned offline NIOSH scientific seeds, HR069 analytical research priors and the best available ChemCore molecular snapshot. */
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

        // Import exposure/flammability data only after the identity table has been populated so CAS matching is deterministic.
        ExposureSeedBootstrap.ensureNioshIdlh(context,db);
        GasFlammabilitySeedBootstrap.ensure(context,db);

        // Seed the complete HR069 research layer into the existing offline database. This is deliberately
        // non-M4: 43 MQ research curves, six preferred literature curves, external evidence descriptors and
        // the complete 76-model MQDataScience catalogue are stored for research/transfer work only.
        try{
            AnalyticalResearchBootstrap.ensure(db.getWritableDatabase());
            db.putMeta("analytical_research_seed_status","available:"+AnalyticalResearchBootstrap.SEED_VERSION);
        }catch(Exception e){
            db.putMeta("analytical_research_seed_status","error:"+e.getClass().getSimpleName()+":"+String.valueOf(e.getMessage()));
        }

        // Seed accepted external transfer fingerprints independently so an already-installed 0.10.2 database
        // receives newly derived assets without forcing a destructive schema migration or rewriting local M4 data.
        try{
            AnalyticalTransferBootstrap.ensure(db.getWritableDatabase());
            db.putMeta("analytical_transfer_seed_status","available:"+AnalyticalTransferBootstrap.SEED_VERSION);
        }catch(Exception e){
            db.putMeta("analytical_transfer_seed_status","error:"+e.getClass().getSimpleName()+":"+String.valueOf(e.getMessage()));
        }

        // Seed high-priority source descriptors discovered after the initial transfer package. These include
        // same-array methanol evidence, MQ-3 ethylene known-concentration evidence and the UCI drift dataset
        // for acetaldehyde/ethylene and corroborating VOCs. They remain transfer evidence only and never M4.
        try{
            AnalyticalPrioritySourceBootstrap.ensure(db.getWritableDatabase());
            db.putMeta("analytical_priority_sources_seed_status","available:"+AnalyticalPrioritySourceBootstrap.SEED_VERSION);
        }catch(Exception e){
            db.putMeta("analytical_priority_sources_seed_status","error:"+e.getClass().getSimpleName()+":"+String.valueOf(e.getMessage()));
        }

        // Seed operational hazard-pattern evidence separately from chemical identity. RoboFusion H1 is a
        // Normal-vs-gas-leak benchmark on six overlapping MQ channels; it must never promote a named analyte,
        // ppm result or M4 state merely by being present in the offline evidence database.
        try{
            OperationalHazardTransferBootstrap.ensure(db.getWritableDatabase());
            db.putMeta("operational_hazard_transfer_seed_status","available:"+OperationalHazardTransferBootstrap.SEED_VERSION);
        }catch(Exception e){
            db.putMeta("operational_hazard_transfer_seed_status","error:"+e.getClass().getSimpleName()+":"+String.valueOf(e.getMessage()));
        }

        // Molecular intelligence prefers an activated, verified Stable snapshot and falls back to the APK seed.
        // The importer is hash-idempotent, so startup remains cheap after the same content is installed once.
        ChemCoreMolecularSeedImporter.Result molecular=ChemCoreMolecularSeedImporter.importBestAvailable(context,db);
        db.putMeta("chemcore_molecular_seed_status",
                molecular.available?("available:"+molecular.records+":"+molecular.matched+":"+molecular.origin):molecular.message);

        // Keep the exact 37 non-single-molecule/unresolved identities in a separate negative-eligibility layer.
        // This import can only deny molecular-QSPR eligibility; it cannot promote any record to a molecular model.
        ChemCoreIdentityCurationRepository.ImportResult curation=ChemCoreIdentityCurationRepository.importIfPresent(context,db);
        db.putMeta("chemcore_identity_curation_status",
                curation.available?("available:"+curation.records+":"+curation.sha256):curation.message);
        return result;
    }
}
