package com.servoc.app.chemcore;

import com.servoc.app.chemical.ChemicalCatalog;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
 *
 * UID rule (P10 hardening): legacy_id and chemical_uid are different namespaces. The historical
 * legacy_id+1 value remains a compatibility fallback only when no database-backed QSPR candidate
 * exists for the same catalogue label. When QSPR has already been rebuilt from the live database,
 * its chemical_uid is authoritative and is reused by direct/reference candidates. This prevents
 * one compound from being represented by two different UIDs in the same ChemCore candidate set.
 */
public final class ChemCoreLegacyCandidateRegistry {
    private ChemCoreLegacyCandidateRegistry() {}

    public static final String UID_POLICY_VERSION="chemcore-legacy-uid-bridge/2";

    /** Preferred name for the broadened accepted reference candidate set. */
    public static List<InverseMolecularSolver.Candidate> acceptedReferenceCandidates(){
        List<InverseMolecularSolver.Candidate> qspr=ChemCoreQsprRuntimeRegistry.candidates();
        List<InverseMolecularSolver.Candidate> out=new ArrayList<>();
        for(int chem=0;chem<ChemicalCatalog.COUNT;chem++){
            List<ChemCoreResponseModel> models=new ArrayList<>();
            for(int sensor=0;sensor<8;sensor++)if(AcceptedReferenceResponseModel.eligible(sensor,chem)){
                try{models.add(new AcceptedReferenceResponseModel(sensor,chem));}catch(IllegalArgumentException ignored){}
            }
            if(models.isEmpty())continue;
            long uid=resolveUidForLegacyChemical(chem,qspr);
            if(uid<=0)continue; // ambiguous canonical mapping: fail closed rather than duplicate identity
            // One direct contributor may form a provisional screening/PPM hypothesis; named M3
            // still needs at least two direct/reference M2+ contributors in the inference gate.
            out.add(new InverseMolecularSolver.Candidate(uid,ChemicalCatalog.NAMES[chem],models,0.0,1));
        }
        return ChemCoreQsprRuntimeRegistry.supplement(out);
    }

    /** Compatibility API retained for existing callers. */
    public static List<InverseMolecularSolver.Candidate> manufacturerSupportedCandidates(){
        return acceptedReferenceCandidates();
    }

    /**
     * Resolves the ChemCore chemical_uid for one legacy catalogue row.
     *
     * If a live QSPR candidate exists for the same catalogue label, that candidate carries the
     * database-backed UID and is authoritative. If no such candidate exists, the historical
     * legacy_id+1 compatibility mapping is retained. If conflicting QSPR UIDs share the same
     * normalized label, -1 is returned and callers must fail closed.
     */
    public static long uidForLegacyChemical(int chemical){
        return resolveUidForLegacyChemical(chemical,ChemCoreQsprRuntimeRegistry.candidates());
    }

    static long resolveUidForLegacyChemical(int chemical,List<InverseMolecularSolver.Candidate> qsprCandidates){
        if(chemical<0||chemical>=ChemicalCatalog.COUNT)return -1;
        String expected=normalize(ChemicalCatalog.NAMES[chemical]);
        Long canonical=null;
        if(qsprCandidates!=null)for(InverseMolecularSolver.Candidate q:qsprCandidates){
            if(q==null||q.chemicalUid<=0||!expected.equals(normalize(q.label)))continue;
            if(canonical!=null&&canonical.longValue()!=q.chemicalUid)return -1;
            canonical=q.chemicalUid;
        }
        return canonical!=null?canonical:chemical+1L;
    }

    public static InverseMolecularSolver.Candidate forLegacyChemical(int chemical){
        if(chemical<0||chemical>=ChemicalCatalog.COUNT)return null;
        List<ChemCoreResponseModel> models=new ArrayList<>();
        for(int sensor=0;sensor<8;sensor++)if(AcceptedReferenceResponseModel.eligible(sensor,chemical)){
            try{models.add(new AcceptedReferenceResponseModel(sensor,chemical));}catch(IllegalArgumentException ignored){}
        }
        long uid=uidForLegacyChemical(chemical);
        if(models.isEmpty()){
            if(uid<=0)return null;
            for(InverseMolecularSolver.Candidate c:ChemCoreQsprRuntimeRegistry.candidates())if(c!=null&&c.chemicalUid==uid)return c;
            return null;
        }
        if(uid<=0)return null;
        List<InverseMolecularSolver.Candidate> merged=ChemCoreQsprRuntimeRegistry.supplement(List.of(
                new InverseMolecularSolver.Candidate(uid,ChemicalCatalog.NAMES[chemical],models,0.0,1)));
        return merged.isEmpty()?null:merged.get(0);
    }

    private static String normalize(String s){return s==null?"":s.trim().toLowerCase(Locale.US);}
}
