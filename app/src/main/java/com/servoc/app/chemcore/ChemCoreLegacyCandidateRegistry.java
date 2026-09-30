package com.servoc.app.chemcore;

import com.servoc.app.chemical.ChemicalCatalog;
import java.util.ArrayList;
import java.util.List;

/**
 * Bridge from the 144-compound legacy catalogue into the current accepted HR069 reference layer.
 *
 * The project decision dated 2026-09-29 accepts available source-backed PPM libraries for current
 * research/reference fitting, with later recalibration allowed. Candidate models therefore use
 * AcceptedReferenceResponseModel, which enforces source domains, source priority, completed 48 h
 * initial MQ conditioning, and the separate MQ-7/MQ-9 heater-cycle constraint.
 *
 * Validated PubChem/QSPR M1 models may supplement missing sensor channels for ranking. A direct
 * M2/M4 model always wins on the same channel, and QSPR cannot independently create M3/M4.
 */
public final class ChemCoreLegacyCandidateRegistry {
    private ChemCoreLegacyCandidateRegistry() {}

    /** Preferred name for the broadened accepted reference candidate set. */
    public static List<InverseMolecularSolver.Candidate> acceptedReferenceCandidates(){
        List<InverseMolecularSolver.Candidate> out=new ArrayList<>();
        for(int chem=0;chem<ChemicalCatalog.COUNT;chem++){
            List<ChemCoreResponseModel> models=new ArrayList<>();
            for(int sensor=0;sensor<8;sensor++)if(AcceptedReferenceResponseModel.eligible(sensor,chem)){
                try{models.add(new AcceptedReferenceResponseModel(sensor,chem));}catch(IllegalArgumentException ignored){}
            }
            if(models.isEmpty())continue;
            // One direct contributor may form a provisional screening/PPM hypothesis; named M3
            // still needs at least two direct/reference M2+ contributors in the inference gate.
            out.add(new InverseMolecularSolver.Candidate(chem+1L,ChemicalCatalog.NAMES[chem],models,0.0,1));
        }
        return ChemCoreQsprRuntimeRegistry.supplement(out);
    }

    /** Compatibility API retained for existing callers. */
    public static List<InverseMolecularSolver.Candidate> manufacturerSupportedCandidates(){
        return acceptedReferenceCandidates();
    }

    public static InverseMolecularSolver.Candidate forLegacyChemical(int chemical){
        if(chemical<0||chemical>=ChemicalCatalog.COUNT)return null;
        List<ChemCoreResponseModel> models=new ArrayList<>();
        for(int sensor=0;sensor<8;sensor++)if(AcceptedReferenceResponseModel.eligible(sensor,chemical)){
            try{models.add(new AcceptedReferenceResponseModel(sensor,chemical));}catch(IllegalArgumentException ignored){}
        }
        if(models.isEmpty()){
            for(InverseMolecularSolver.Candidate c:ChemCoreQsprRuntimeRegistry.candidates())if(c.chemicalUid==chemical+1L)return c;
            return null;
        }
        List<InverseMolecularSolver.Candidate> merged=ChemCoreQsprRuntimeRegistry.supplement(List.of(
                new InverseMolecularSolver.Candidate(chemical+1L,ChemicalCatalog.NAMES[chemical],models,0.0,1)));
        return merged.isEmpty()?null:merged.get(0);
    }
}
