package com.servoc.app.chemcore;

import com.servoc.app.chemical.ChemicalCatalog;
import java.util.ArrayList;
import java.util.List;

/**
 * Conservative bridge from the existing 144-compound legacy catalogue into ChemCore.
 * Only sensor/compound pairs that have BOTH an inherited curve and a finite current
 * manufacturer evidence domain are admitted. Other catalogue entries remain available to
 * molecular/QSPR/fingerprint paths but are not silently assigned an arbitrary curve.
 */
public final class ChemCoreLegacyCandidateRegistry {
    private ChemCoreLegacyCandidateRegistry() {}

    public static List<InverseMolecularSolver.Candidate> manufacturerSupportedCandidates(){
        List<InverseMolecularSolver.Candidate> out=new ArrayList<>();
        for(int chem=0;chem<ChemicalCatalog.COUNT;chem++){
            List<ChemCoreResponseModel> models=new ArrayList<>();
            for(int sensor=0;sensor<8;sensor++)if(LegacyGasEngineResponseModel.eligible(sensor,chem)){
                try{models.add(new LegacyGasEngineResponseModel(sensor,chem));}catch(IllegalArgumentException ignored){}
            }
            if(models.isEmpty())continue;
            // One contributor may generate a screening candidate; M3 array consistency requires >=2 later.
            out.add(new InverseMolecularSolver.Candidate(chem+1L,ChemicalCatalog.NAMES[chem],models,0.0,1));
        }
        return out;
    }

    public static InverseMolecularSolver.Candidate forLegacyChemical(int chemical){
        if(chemical<0||chemical>=ChemicalCatalog.COUNT)return null;
        List<ChemCoreResponseModel> models=new ArrayList<>();
        for(int sensor=0;sensor<8;sensor++)if(LegacyGasEngineResponseModel.eligible(sensor,chemical)){
            try{models.add(new LegacyGasEngineResponseModel(sensor,chemical));}catch(IllegalArgumentException ignored){}
        }
        return models.isEmpty()?null:new InverseMolecularSolver.Candidate(chemical+1L,ChemicalCatalog.NAMES[chemical],models,0.0,1);
    }
}
