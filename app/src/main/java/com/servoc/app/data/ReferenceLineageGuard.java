package com.servoc.app.data;

import android.database.Cursor;

import com.servoc.app.hardware.HazRobHardwareProfile;

import org.json.JSONObject;

import java.util.Locale;

/**
 * Fail-closed lineage guard for 0.10.2 / HR069 developer reference evidence.
 *
 * A reference experiment is scientifically usable only when its experiment row, averaged capture
 * and originating completed measurement session all point to the same physical acquisition.
 * Timestamp proximity is never accepted as lineage. R0 is deliberately outside this contract
 * because it is device-local normalization state, not centrally publishable chemical evidence.
 */
public final class ReferenceLineageGuard {
    private static final double PPM_REL_TOL = 1e-6;
    private static final int MIN_TELEMETRY_SAMPLES = 2;
    private ReferenceLineageGuard(){}

    public static boolean valid(ExperimentsDb db, ExperimentQueries.Record r){
        if(db==null||r==null||!r.isReference())return false;
        String session=n(r.sessionId),kind=n(r.referenceKind).toLowerCase(Locale.US);
        if(session.isEmpty()||r.captureId<=0||r.chemicalUid<=0)return false;
        if(!"fingerprint".equals(kind)&&!"calibration".equals(kind))return false;

        String captureSession="",phase="",measurementKind="",role="",endedTs="",featurePayload="";
        long captureChemical=-1,sampleCount=0;
        Double capturePpm=null;
        String sql="SELECT c.session_id,c.chemical_uid,c.known_ppm,c.phase,m.kind,m.operator_role,m.ended_ts,c.feature_payload," +
                "(SELECT COUNT(*) FROM telemetry_samples t WHERE t.session_id=m.session_id) " +
                "FROM captures c JOIN measurement_sessions m ON m.session_id=c.session_id WHERE c.id=? LIMIT 1";
        try(Cursor c=db.getReadableDatabase().rawQuery(sql,new String[]{String.valueOf(r.captureId)})){
            if(!c.moveToFirst())return false;
            captureSession=n(c.getString(0));captureChemical=c.isNull(1)?-1:c.getLong(1);
            capturePpm=c.isNull(2)?null:c.getDouble(2);phase=n(c.getString(3));measurementKind=n(c.getString(4));role=n(c.getString(5));
            endedTs=n(c.getString(6));featurePayload=n(c.getString(7));sampleCount=c.getLong(8);
        }catch(Exception ignored){return false;}

        if(!session.equals(captureSession)||captureChemical!=r.chemicalUid)return false;
        if(!"ready_average".equalsIgnoreCase(phase))return false;
        if(!"DEVELOPER".equals(role)||!measurementKind.startsWith("developer_"))return false;
        if(endedTs.isEmpty()||sampleCount<MIN_TELEMETRY_SAMPLES)return false;
        if(!ppmEquivalent(r.knownPpm,capturePpm))return false;
        if("calibration".equals(kind)&&(r.knownPpm==null||!Double.isFinite(r.knownPpm)||r.knownPpm<=0))return false;
        if(!hr069Capture(featurePayload,session,kind))return false;
        return true;
    }

    public static boolean validById(ExperimentsDb db,long experimentId){
        return valid(db,ExperimentQueries.byId(db,experimentId));
    }

    /** Every source of a derived artifact must be approved, exact-lineage evidence for the same chemical and kind. */
    public static boolean artifactSourcesValid(ExperimentsDb db,ExperimentsDb.ReferenceArtifactRecord artifact){
        if(db==null||artifact==null||artifact.chemicalUid<=0)return false;
        String kind=n(artifact.kind).toLowerCase(Locale.US),src=n(artifact.sourceIds);
        if(src.isEmpty())return false;
        int count=0;
        for(String token:src.split(",")){
            String x=token.trim();if(x.isEmpty())continue;
            long id;try{id=Long.parseLong(x);}catch(Exception ignored){return false;}
            ExperimentQueries.Record r=ExperimentQueries.byId(db,id);
            if(r==null||!r.approved||r.chemicalUid!=artifact.chemicalUid||!kind.equals(n(r.referenceKind).toLowerCase(Locale.US))||!valid(db,r))return false;
            count++;
        }
        return count>0;
    }

    private static boolean hr069Capture(String payload,String session,String kind){
        try{
            JSONObject x=new JSONObject(payload);
            if(!"hazrob-reference-capture-v0.10.2".equals(x.optString("schema","")))return false;
            if(!HazRobHardwareProfile.ID.equals(x.optString("hardware_profile","")))return false;
            if(!session.equals(n(x.optString("session_id",""))))return false;
            if(!kind.equals(n(x.optString("reference_kind","")).toLowerCase(Locale.US)))return false;
            if(!"ready_average".equalsIgnoreCase(x.optString("capture_state","")))return false;
            return "DEVICE_LOCAL_NOT_DISTRIBUTED".equals(x.optString("r0_scope",""));
        }catch(Exception ignored){return false;}
    }

    private static boolean ppmEquivalent(Double a,Double b){
        if(a==null||b==null)return a==null&&b==null;
        if(!Double.isFinite(a)||!Double.isFinite(b))return false;
        double scale=Math.max(1.0,Math.max(Math.abs(a),Math.abs(b)));
        return Math.abs(a-b)<=scale*PPM_REL_TOL;
    }
    private static String n(String s){return s==null?"":s.trim();}
}
