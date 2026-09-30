package com.servoc.app.exposure;

import com.servoc.app.chemcore.ChemCoreDecisionRepository;
import com.servoc.app.chemcore.ChemCoreResultEnvelope;
import com.servoc.app.chemcore.InverseMolecularSolver;
import com.servoc.app.chemcore.SamplingProfile;
import com.servoc.app.chemcore.library.ChemCoreStableKnowledgeReader;
import com.servoc.app.data.ExperimentsDb;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.Collections;
import java.util.List;

/**
 * P10 finalizer for one already-persisted primary ChemCore session.
 *
 * Identification/evidence fusion is intentionally left untouched. This class only creates the
 * device-validated quantitative safety layer: M4-only temporal ppm, current exposure references,
 * TWA/STEL windows when actually covered, and immediate Ceiling/IDLH/LFL comparisons. The result is
 * attached to the SAME chemcore_decisions row through ChemCoreDecisionRepository.
 */
public final class ChemCoreP10SessionFinalizer {
    private ChemCoreP10SessionFinalizer(){}

    public static JSONObject finalizeAndAttach(ExperimentsDb db,String sessionId,ChemCoreResultEnvelope result,
                                               File activeLibraryPayload,SamplingProfile profile){
        if(db==null||sessionId==null||sessionId.trim().isEmpty())return invalid(sessionId,"session_required");
        try{
            long uid=result!=null&&result.chemicalUid!=null?result.chemicalUid.longValue():-1L;
            ExposureRepository exposureRepo=new ExposureRepository(db);
            List<ExposureRepository.Limit> limits=uid>0?exposureRepo.forChemical(uid):Collections.emptyList();
            List<InverseMolecularSolver.Candidate> candidates=activeLibraryPayload==null?Collections.emptyList():ChemCoreStableKnowledgeReader.candidates(activeLibraryPayload,profile);
            JSONObject series=uid>0
                    ?ChemCoreQuantitativeTimeSeries.extract(db,sessionId,uid,candidates,profile,limits)
                    :invalid(sessionId,"accepted_chemical_identity_required");
            ChemCoreDecisionRepository decisions=new ChemCoreDecisionRepository(db);
            if(!decisions.attachQuantitativeExposure(sessionId,series)){
                JSONObject out=new JSONObject(series.toString());out.put("p10_attachment_succeeded",false);out.put("p10_attachment_reason","decision_row_not_found_or_update_failed");return out;
            }
            JSONObject out=new JSONObject(series.toString());out.put("p10_attachment_succeeded",true);return out;
        }catch(Exception e){return invalid(sessionId,"p10_finalization_failure");}
    }

    private static JSONObject invalid(String sessionId,String reason){
        try{return new JSONObject().put("schema",ChemCoreQuantitativeTimeSeries.SCHEMA).put("session_id",sessionId==null?"":sessionId).put("chemical_uid",JSONObject.NULL).put("valid",false).put("m4_only",true).put("reason",reason).put("points",new JSONArray()).put("windows",new JSONArray()).put("raw_telemetry_preserved_elsewhere",true).put("raw_telemetry_source","telemetry_samples");}
        catch(Exception e){return new JSONObject();}
    }
}
