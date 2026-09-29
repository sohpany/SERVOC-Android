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
 * These candidates remain non-M4. A fitted concentration can be displayed as a provisional
 * reference hypothesis, but occupational/safety quantitative interpretation is still gated by M4.
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
            // One contributor may form a provisional screening/PPM hypothesis; named M3 identity still needs multisensor evidence.
            out.add(new InverseMolecularSolver.Candidate(chem+1L,ChemicalCatalog.NAMES[chem],models,0.0,1));
        }
        return out;
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
        return models.isEmpty()?null:new InverseMolecularSolver.Candidate(chemical+1L,ChemicalCatalog.NAMES[chemical],models,0.0,1);
    }
}
