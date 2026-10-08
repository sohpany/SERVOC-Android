package com.servoc.app.chemical;

import android.content.Context;

import com.servoc.app.chemcore.ChemCoreIdentityCurationRepository;
import com.servoc.app.chemcore.ChemCoreMolecularSeedImporter;
import com.servoc.app.chemcore.ChemCoreQsprRuntimeRegistry;
import com.servoc.app.chemcore.library.ChemCoreEmbeddedStableInstaller;
import com.servoc.app.chemcore.library.ChemCoreLibraryStore;
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
        }catch(Exception e){db.putMeta("chemical_seed_niosh_last_error",e.getClass().getSimpleName()+": "+String.valueOf(e.getMessage()));}

        ExposureSeedBootstrap.ensureNioshIdlh(context,db);
        GasFlammabilitySeedBootstrap.ensure(context,db);

        try{AnalyticalResearchBootstrap.ensure(db.getWritableDatabase());db.putMeta("analytical_research_seed_status","available:"+AnalyticalResearchBootstrap.SEED_VERSION);}catch(Exception e){db.putMeta("analytical_research_seed_status","error:"+e.getClass().getSimpleName()+":"+String.valueOf(e.getMessage()));}
        try{AnalyticalTransferBootstrap.ensure(db.getWritableDatabase());db.putMeta("analytical_transfer_seed_status","available:"+AnalyticalTransferBootstrap.SEED_VERSION);}catch(Exception e){db.putMeta("analytical_transfer_seed_status","error:"+e.getClass().getSimpleName()+":"+String.valueOf(e.getMessage()));}
        try{AnalyticalPrioritySourceBootstrap.ensure(db.getWritableDatabase());db.putMeta("analytical_priority_sources_seed_status","available:"+AnalyticalPrioritySourceBootstrap.SEED_VERSION);}catch(Exception e){db.putMeta("analytical_priority_sources_seed_status","error:"+e.getClass().getSimpleName()+":"+String.valueOf(e.getMessage()));}
        try{MqUsefulEvidenceBootstrap.ensure(db.getWritableDatabase());db.putMeta("mq_useful_evidence_seed_status","available:"+MqUsefulEvidenceBootstrap.SEED_VERSION);}catch(Exception e){db.putMeta("mq_useful_evidence_seed_status","error:"+e.getClass().getSimpleName()+":"+String.valueOf(e.getMessage()));}
        try{MqSingleSensorEvidenceBootstrap.ensure(db.getWritableDatabase());db.putMeta("mq_single_sensor_evidence_seed_status","available:"+MqSingleSensorEvidenceBootstrap.SEED_VERSION);}catch(Exception e){db.putMeta("mq_single_sensor_evidence_seed_status","error:"+e.getClass().getSimpleName()+":"+String.valueOf(e.getMessage()));}
        try{MqCalibrationSeriesBootstrap.ensure(db.getWritableDatabase());db.putMeta("mq_calibration_series_seed_status","available:"+MqCalibrationSeriesBootstrap.SEED_VERSION);}catch(Exception e){db.putMeta("mq_calibration_series_seed_status","error:"+e.getClass().getSimpleName()+":"+String.valueOf(e.getMessage()));}
        try{MqVocFingerprintEvidenceBootstrap.ensure(db.getWritableDatabase());db.putMeta("mq_voc_fingerprint_evidence_seed_status","available:"+MqVocFingerprintEvidenceBootstrap.SEED_VERSION);}catch(Exception e){db.putMeta("mq_voc_fingerprint_evidence_seed_status","error:"+e.getClass().getSimpleName()+":"+String.valueOf(e.getMessage()));}
        try{MqLargeVocEvidenceBootstrap.ensure(db.getWritableDatabase());db.putMeta("mq_large_voc_evidence_seed_status","available:"+MqLargeVocEvidenceBootstrap.SEED_VERSION);}catch(Exception e){db.putMeta("mq_large_voc_evidence_seed_status","error:"+e.getClass().getSimpleName()+":"+String.valueOf(e.getMessage()));}
        try{MqReactiveEvidenceBootstrap.ensure(db.getWritableDatabase());db.putMeta("mq_reactive_evidence_seed_status","available:"+MqReactiveEvidenceBootstrap.SEED_VERSION);}catch(Exception e){db.putMeta("mq_reactive_evidence_seed_status","error:"+e.getClass().getSimpleName()+":"+String.valueOf(e.getMessage()));}
        try{OperationalHazardTransferBootstrap.ensure(db.getWritableDatabase());db.putMeta("operational_hazard_transfer_seed_status","available:"+OperationalHazardTransferBootstrap.SEED_VERSION);}catch(Exception e){db.putMeta("operational_hazard_transfer_seed_status","error:"+e.getClass().getSimpleName()+":"+String.valueOf(e.getMessage()));}

        // HR069-R1: initialize the genuine embedded Stable-channel snapshot before
        // molecular/QSPR imports. This does not write developer reference data or
        // override an installed remote Stable; failures retain existing APK fallback.
        ChemCoreLibraryStore.InstallResult embedded=ChemCoreEmbeddedStableInstaller.ensureInstalled(context);
        db.putMeta("chemcore_embedded_stable_bootstrap",
                embedded.installed?"installed:"+embedded.version:"unchanged_or_failed:"+embedded.reason+":"+embedded.version);

        ChemCoreMolecularSeedImporter.Result molecular=ChemCoreMolecularSeedImporter.importBestAvailable(context,db);
        db.putMeta("chemcore_molecular_seed_status",molecular.available?("available:"+molecular.records+":"+molecular.matched+":"+molecular.origin):molecular.message);
        ChemCoreIdentityCurationRepository.ImportResult curation=ChemCoreIdentityCurationRepository.importIfPresent(context,db);
        db.putMeta("chemcore_identity_curation_status",curation.available?("available:"+curation.records+":"+curation.sha256):curation.message);

        // PubChem/QSPR is rebuilt only after the molecular seed and the negative curation layer
        // are installed. It remains an M1 ranking/screening prior and cannot create M3/M4 alone.
        try{
            ChemCoreQsprRuntimeRegistry.Status qspr=ChemCoreQsprRuntimeRegistry.rebuild(db);
            db.putMeta("chemcore_qspr_runtime_status",qspr.summary());
        }catch(Exception e){
            db.putMeta("chemcore_qspr_runtime_status","error:"+e.getClass().getSimpleName()+":"+String.valueOf(e.getMessage()));
        }

        // Priority is a view/subset over the same master chemical records. Run it only after all
        // MQ evidence and molecular/identity enrichment have been loaded so newly supported
        // master-library compounds are promoted automatically without duplicating chemical facts.
        try{ChemicalPriorityBootstrap.ensure(db.getWritableDatabase());db.putMeta("chemical_priority_seed_status","available:"+ChemicalPriorityBootstrap.VERSION);}catch(Exception e){db.putMeta("chemical_priority_seed_status","error:"+e.getClass().getSimpleName()+":"+String.valueOf(e.getMessage()));}
        return result;
    }
}
