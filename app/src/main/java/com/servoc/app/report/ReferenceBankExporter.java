package com.servoc.app.report;

import com.servoc.app.ReleaseAudit;
import com.servoc.app.data.ExperimentQueries;
import org.json.JSONObject;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

/** GitHub-ready immutable approved-experiment record. No credentials are stored in the APK. */
public final class ReferenceBankExporter {
    private ReferenceBankExporter(){}
    public static String fileName(long id,String ts){String stamp=ts==null?"unknown":ts.replace("-","").replace(":","").replace("T","_").replace("Z","");return String.format(Locale.US,"HazRob_APPROVED_%06d_%s.json",id,stamp);}
    public static void writeJson(OutputStream out,ExperimentQueries.Record r)throws Exception{
        if(r==null)throw new IllegalArgumentException("record");JSONObject root=new JSONObject();root.put("schema","hazrob-approved-experiment/2");root.put("status","APPROVED");root.put("app","HazRob");root.put("app_version",ReleaseAudit.VERSION);root.put("experiment_id",r.id);root.put("accepted_at_utc",n(r.ts));root.put("approved_at_utc",n(r.approvedTs));root.put("protocol",n(r.protocol));root.put("repository_target","reference_bank/approved_experiments/");
        JSONObject lineage=new JSONObject();lineage.put("session_id",n(r.sessionId));lineage.put("capture_id",r.captureId);lineage.put("chemical_uid",r.chemicalUid);lineage.put("reference_kind",n(r.referenceKind));if(r.knownPpm!=null)lineage.put("known_ppm",r.knownPpm);root.put("lineage",lineage);
        JSONObject telemetry=json(r.payload),analytics=json(r.analytics);root.put("telemetry",telemetry);root.put("analytics",analytics);JSONObject integrity=new JSONObject();integrity.put("telemetry_sha256",sha256(n(r.payload)));integrity.put("analytics_sha256",sha256(n(r.analytics)));integrity.put("lineage_sha256",sha256(lineage.toString()));integrity.put("record_rule","Approved raw experiments are immutable. Derived fingerprints/calibration artifacts cite these experiment IDs and are versioned independently.");root.put("integrity",integrity);out.write(root.toString(2).getBytes(StandardCharsets.UTF_8));out.flush();
    }
    private static JSONObject json(String s){try{return s==null||s.trim().isEmpty()?new JSONObject():new JSONObject(s);}catch(Exception e){return new JSONObject();}}
    private static String sha256(String s)throws Exception{byte[] d=MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));StringBuilder x=new StringBuilder();for(byte b:d)x.append(String.format(Locale.US,"%02x",b&255));return x.toString();}
    private static String n(String s){return s==null?"":s;}
}
