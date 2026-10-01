package com.servoc.app.chemcore;

import org.json.JSONObject;

/**
 * Canonical progressive analytical state projection for ChemCore.
 *
 * The state describes the strongest scientifically defensible result level reached by a session;
 * it is not a candidate posterior, confidence percentage or evidence tier. Rejection/OOD are
 * explicit terminal branches rather than low numerical scores.
 */
public final class ChemCoreProgressiveResultState {
    public static final String VERSION="chemcore-progressive-state/1";

    public enum State {
        INVALID,
        SIGNAL,
        AFFINITY,
        CANDIDATE,
        IDENTIFIED,
        QUANTIFIED,
        ANALYTICALLY_REJECTED,
        UNKNOWN_OOD
    }

    public static final class Result {
        public final State state;
        /** Furthest ordinary ladder level reached: 0 invalid, 1 signal ... 5 quantified. */
        public final int ladderLevel;
        public final boolean terminal;
        public final String reasonCode;
        public final String note;

        Result(State state,int ladderLevel,boolean terminal,String reasonCode,String note){
            this.state=state;this.ladderLevel=Math.max(0,Math.min(5,ladderLevel));this.terminal=terminal;
            this.reasonCode=reasonCode==null?"":reasonCode;this.note=note==null?"":note;
        }

        public JSONObject toJson(){
            try{
                JSONObject o=new JSONObject();o.put("schema",VERSION);o.put("state",state.name());
                o.put("ladder_level",ladderLevel);o.put("terminal",terminal);o.put("reason_code",reasonCode);o.put("note",note);return o;
            }catch(Exception e){throw new IllegalStateException("Unable to serialize progressive state",e);}
        }
    }

    private ChemCoreProgressiveResultState(){}

    public static Result project(ChemCoreResultEnvelope r){
        if(r==null)return new Result(State.INVALID,0,true,"NO_RESULT","result envelope unavailable");
        int reached=ordinaryLevel(r);
        switch(r.outcome){
            case INVALID_MEASUREMENT:
                return new Result(State.INVALID,0,true,"INVALID_MEASUREMENT",r.note);
            case UNKNOWN:
                return new Result(State.UNKNOWN_OOD,Math.max(1,reached),true,"UNKNOWN_OOD",r.note);
            case OUT_OF_VALIDATED_DOMAIN:
                return new Result(State.ANALYTICALLY_REJECTED,Math.max(1,reached),true,"OUT_OF_VALIDATED_DOMAIN",r.note);
            case IDENTIFIED:
                return new Result(State.IDENTIFIED,4,true,"IDENTIFIED_M3_OR_HIGHER",r.note);
            case QUANTIFIED:
                return new Result(State.QUANTIFIED,5,true,"QUANTIFIED_M4",r.note);
            case POSSIBLE_MIXTURE_OR_INTERFERENCE:
                return new Result(State.CANDIDATE,Math.max(3,reached),true,"MIXTURE_OR_INTERFERENCE_UNRESOLVED",r.note);
            case INSUFFICIENT_EVIDENCE:
                return ordinary(r,true,"INSUFFICIENT_EVIDENCE");
            case SCREENING:
            default:
                return ordinary(r,false,"SCREENING");
        }
    }

    private static Result ordinary(ChemCoreResultEnvelope r,boolean terminal,String reason){
        int level=ordinaryLevel(r);
        State state=level>=3?State.CANDIDATE:level==2?State.AFFINITY:State.SIGNAL;
        return new Result(state,level,terminal,reason,r.note);
    }

    private static int ordinaryLevel(ChemCoreResultEnvelope r){
        if(r==null)return 0;
        if(r.outcome==ChemCoreDecision.Outcome.QUANTIFIED)return 5;
        if(r.outcome==ChemCoreDecision.Outcome.IDENTIFIED)return 4;
        boolean hasCandidate=r.chemicalUid!=null||!r.candidates.isEmpty();
        if(hasCandidate&&r.evidenceTier!=null&&r.evidenceTier.ordinal()>=ChemCoreDecision.EvidenceTier.M2_LITERATURE_OR_MANUFACTURER_SUPPORTED.ordinal())return 3;
        if(hasCandidate&&r.evidenceTier==ChemCoreDecision.EvidenceTier.M1_MOLECULARLY_INFERRED)return 2;
        // A valid ChemCore result with no defensible named/affinity candidate still records signal.
        return r.outcome==ChemCoreDecision.Outcome.INVALID_MEASUREMENT?0:1;
    }
}
