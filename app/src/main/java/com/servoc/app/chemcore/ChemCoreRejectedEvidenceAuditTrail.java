package com.servoc.app.chemcore;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Auditable record of hypotheses/evidence that were observed but did not survive final ChemCore
 * promotion. Rejection is diagnostic; it is not negative proof that the chemical is absent.
 */
public final class ChemCoreRejectedEvidenceAuditTrail {
    public static final String VERSION="chemcore-rejected-evidence/1";

    public enum Reason {
        OUT_OF_VALIDATED_DOMAIN,
        HEATER_PROTOCOL_INVALID,
        INSUFFICIENT_CONTRIBUTORS,
        SPECIALIST_CONFLICT,
        OPEN_SET_OOD,
        UNSUPPORTED_HARDWARE,
        MODEL_UNAVAILABLE,
        INVALID_CONCENTRATION_DOMAIN,
        DISPLACED_BY_HIGHER_PRECEDENCE,
        SINGLE_SENSOR_SCREENING_ONLY,
        LIVE_HYPOTHESIS_NOT_CONFIRMED,
        UNKNOWN
    }

    public enum Stage {
        LIVE,
        RESPONSE_MODEL,
        JOINT_FIT,
        SPECIALIST_GATE,
        OPEN_SET,
        HARDWARE_GATE,
        SOURCE_PRECEDENCE,
        FINAL_DECISION
    }

    public static final class Row {
        public final Long chemicalUid;
        public final String chemicalLabel;
        public final String sensorKey;
        public final Stage stage;
        public final Reason reason;
        public final String sourceId,modelId,note;
        public final double diagnosticValue;

        public Row(Long chemicalUid,String chemicalLabel,String sensorKey,Stage stage,Reason reason,
                   String sourceId,String modelId,double diagnosticValue,String note){
            this.chemicalUid=chemicalUid;this.chemicalLabel=clean(chemicalLabel);this.sensorKey=clean(sensorKey);
            this.stage=stage==null?Stage.FINAL_DECISION:stage;this.reason=reason==null?Reason.UNKNOWN:reason;
            this.sourceId=clean(sourceId);this.modelId=clean(modelId);this.diagnosticValue=diagnosticValue;this.note=clean(note);
        }

        public JSONObject toJson(){
            try{
                JSONObject x=new JSONObject();x.put("schema",VERSION);x.put("stage",stage.name());x.put("reason",reason.name());
                if(chemicalUid!=null)x.put("chemical_uid",chemicalUid);if(!chemicalLabel.isEmpty())x.put("chemical",chemicalLabel);
                if(!sensorKey.isEmpty())x.put("sensor_key",sensorKey);if(!sourceId.isEmpty())x.put("source_id",sourceId);
                if(!modelId.isEmpty())x.put("model_id",modelId);if(Double.isFinite(diagnosticValue))x.put("diagnostic_value",diagnosticValue);
                if(!note.isEmpty())x.put("note",note);return x;
            }catch(Exception e){throw new IllegalStateException("Unable to serialize rejected evidence",e);}
        }
    }

    public static final class Result {
        public final List<Row> rows;
        Result(List<Row> rows){this.rows=Collections.unmodifiableList(new ArrayList<>(rows));}
        public boolean isEmpty(){return rows.isEmpty();}
        public JSONArray toJson(){JSONArray a=new JSONArray();for(Row r:rows)a.put(r.toJson());return a;}
    }

    private ChemCoreRejectedEvidenceAuditTrail(){}

    /**
     * Builds the analytical rejection trail from solver/gate outputs. Operational exclusions whose
     * channels never reached the solver can be supplied through explicitRows.
     */
    public static Result collect(List<InverseMolecularSolver.Result> ranked,
                                 ChemCoreSpecialistConflictEngine.Result specialist,
                                 ChemCoreOpenSetGate.Result openSet,
                                 List<Row> explicitRows){
        List<Row> out=new ArrayList<>();
        if(explicitRows!=null)out.addAll(explicitRows);
        if(ranked!=null)for(InverseMolecularSolver.Result r:ranked){
            if(r==null)continue;
            if(!r.solvable){
                Reason reason=reasonForUnsolved(r.note);
                out.add(new Row(r.chemicalUid,r.label,"",Stage.JOINT_FIT,reason,"","",Double.NaN,r.note));
                continue;
            }
            if(!r.allContributorsInsideDomain){
                out.add(new Row(r.chemicalUid,r.label,"",Stage.RESPONSE_MODEL,Reason.OUT_OF_VALIDATED_DOMAIN,
                        "","",r.standardizedResidualRms,"one or more contributing response models were outside their declared domain"));
            }
            if(r.referenceContributors<2&&r.calibratedContributors<1){
                out.add(new Row(r.chemicalUid,r.label,"",Stage.JOINT_FIT,Reason.SINGLE_SENSOR_SCREENING_ONLY,
                        "","",r.referenceContributors,"candidate lacks >=2 direct/reference contributors and has no M4 specialist support"));
            }
        }
        if(specialist!=null&&specialist.holdNamedIdentity){
            ChemCoreSpecialistConflictEngine.SpecialistSupport c=specialist.challengerSupport;
            out.add(new Row(c==null?null:c.chemicalUid,c==null?"":c.label,c==null?"":c.sensorKey,
                    Stage.SPECIALIST_GATE,Reason.SPECIALIST_CONFLICT,"",ChemCoreSpecialistConflictEngine.VERSION,
                    c==null?Double.NaN:c.standardizedResidual,specialist.reason));
        }
        if(openSet!=null&&!openSet.accepted){
            InverseMolecularSolver.Result top=firstSolvable(ranked);
            out.add(new Row(top==null?null:top.chemicalUid,top==null?"":top.label,"",Stage.OPEN_SET,
                    Reason.OPEN_SET_OOD,"","ChemCoreOpenSetGate",openSet.distance,openSet.reason));
        }
        return new Result(deduplicate(out));
    }

    /** Preserves a named live hypothesis that the completed deep analysis did not confirm. */
    public static Row liveHypothesisNotConfirmed(ChemCoreLiveSensingEngine.Snapshot live,ChemCoreResultEnvelope completed){
        if(live==null||live.chemicalUid==null||live.chemicalLabel.isEmpty())return null;
        boolean same=completed!=null&&completed.chemicalUid!=null&&completed.chemicalUid.equals(live.chemicalUid)&&
                ChemCoreDecision.permitsNamedIdentity(completed.outcome);
        if(same)return null;
        Reason reason=completed==null?Reason.LIVE_HYPOTHESIS_NOT_CONFIRMED:
                completed.outcome==ChemCoreDecision.Outcome.UNKNOWN?Reason.OPEN_SET_OOD:
                completed.outcome==ChemCoreDecision.Outcome.OUT_OF_VALIDATED_DOMAIN?Reason.OUT_OF_VALIDATED_DOMAIN:
                completed.outcome==ChemCoreDecision.Outcome.POSSIBLE_MIXTURE_OR_INTERFERENCE?Reason.SPECIALIST_CONFLICT:
                Reason.LIVE_HYPOTHESIS_NOT_CONFIRMED;
        String note="live hypothesis was retained for audit but was not confirmed by completed-session analysis";
        if(completed!=null&&!completed.note.isEmpty())note+="; final="+completed.outcome.name()+": "+completed.note;
        return new Row(live.chemicalUid,live.chemicalLabel,"",Stage.LIVE,reason,"chemcore-live/1","",
                live.posterior,note);
    }

    public static Row heaterInvalid(Long uid,String label,String sensor,String note){
        return new Row(uid,label,sensor,Stage.HARDWARE_GATE,Reason.HEATER_PROTOCOL_INVALID,"","",Double.NaN,note);
    }
    public static Row unsupportedHardware(Long uid,String label,String sensor,String note){
        return new Row(uid,label,sensor,Stage.HARDWARE_GATE,Reason.UNSUPPORTED_HARDWARE,"","",Double.NaN,note);
    }
    public static Row displacedByPrecedence(Long uid,String label,String sensor,String sourceId,String modelId,String note){
        return new Row(uid,label,sensor,Stage.SOURCE_PRECEDENCE,Reason.DISPLACED_BY_HIGHER_PRECEDENCE,sourceId,modelId,Double.NaN,note);
    }

    private static Reason reasonForUnsolved(String note){
        String s=clean(note).toLowerCase();
        if(s.contains("no response models"))return Reason.MODEL_UNAVAILABLE;
        if(s.contains("invalid candidate concentration domain"))return Reason.INVALID_CONCENTRATION_DOMAIN;
        if(s.contains("insufficient overlapping sensor evidence"))return Reason.INSUFFICIENT_CONTRIBUTORS;
        return Reason.UNKNOWN;
    }

    private static InverseMolecularSolver.Result firstSolvable(List<InverseMolecularSolver.Result> rows){
        if(rows!=null)for(InverseMolecularSolver.Result r:rows)if(r!=null&&r.solvable)return r;return null;
    }

    private static List<Row> deduplicate(List<Row> rows){
        List<Row> out=new ArrayList<>();java.util.LinkedHashSet<String> seen=new java.util.LinkedHashSet<>();
        for(Row r:rows){if(r==null)continue;String key=r.stage+"|"+r.reason+"|"+r.chemicalUid+"|"+r.sensorKey+"|"+r.sourceId+"|"+r.modelId;
            if(seen.add(key))out.add(r);}
        return out;
    }
    private static String clean(String s){return s==null?"":s.trim();}
}
