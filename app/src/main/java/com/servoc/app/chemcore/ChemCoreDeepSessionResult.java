package com.servoc.app.chemcore;

import org.json.JSONObject;

/** Immutable final P9 session result: analytical decision plus primary/recovery dynamics. */
public final class ChemCoreDeepSessionResult {
    public static final String SCHEMA="chemcore-deep-session/1";
    public final ChemCoreResultEnvelope analyticalResult;
    public final ChemCoreSessionDynamics.Result dynamics;
    public final String primarySessionId,recoverySessionId;
    public final long analysisElapsedMs;

    public ChemCoreDeepSessionResult(ChemCoreResultEnvelope analyticalResult,ChemCoreSessionDynamics.Result dynamics,long analysisElapsedMs){
        if(analyticalResult==null)throw new IllegalArgumentException("analytical result required");
        if(dynamics==null)throw new IllegalArgumentException("session dynamics required");
        this.analyticalResult=analyticalResult;this.dynamics=dynamics;
        this.primarySessionId=analyticalResult.sessionId;this.recoverySessionId=dynamics.recoverySessionId;
        this.analysisElapsedMs=Math.max(0,analysisElapsedMs);
    }

    public JSONObject toJson(){
        try{
            JSONObject q=new JSONObject();q.put("schema",SCHEMA);q.put("primary_session_id",primarySessionId);q.put("recovery_session_id",recoverySessionId);
            q.put("analysis_elapsed_ms",analysisElapsedMs);q.put("performance_target_typical_ms",5000);q.put("performance_target_heavy_ms",10000);
            q.put("analytical_result",new JSONObject(analyticalResult.toJson().toString()));q.put("session_dynamics",dynamics.toJson());
            q.put("recovery_changes_identity_confidence",false);q.put("recovery_unlocks_reportable_ppm",false);
            return q;
        }catch(Exception e){throw new IllegalStateException("deep session serialization failed",e);}
    }
}
