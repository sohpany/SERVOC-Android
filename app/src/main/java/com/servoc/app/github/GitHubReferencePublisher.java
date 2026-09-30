package com.servoc.app.github;

import com.servoc.app.data.ExperimentQueries;
import com.servoc.app.data.ExperimentsDb;
import com.servoc.app.report.ReferenceBankExporter;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Locale;

/** Publishes immutable approved raw experiments and derived references to the HazRob organization library. */
public final class GitHubReferencePublisher {
    /** Canonical scientific-library owner. Do not redirect approved laboratory evidence to a personal repository. */
    public static final String REPOSITORY="HazRob/build-harness";
    public static final String BRANCH="main";
    public static final String LIBRARY_ROOT="library";
    public static final String DIRECTORY=LIBRARY_ROOT+"/reference-bank/approved-experiments";
    public static final String DERIVED_DIRECTORY=LIBRARY_ROOT+"/reference-bank/derived-references";
    private static final String API_VERSION="2022-11-28";
    private GitHubReferencePublisher(){}

    public static final class Result {
        public final boolean success;public final int httpCode;public final String path,commitSha,message,payloadSha256;
        Result(boolean success,int httpCode,String path,String commitSha,String message,String payloadSha256){this.success=success;this.httpCode=httpCode;this.path=path;this.commitSha=commitSha;this.message=message;this.payloadSha256=payloadSha256;}
    }

    public static Result publish(String token,ExperimentQueries.Record record)throws Exception{
        if(record==null)throw new IllegalArgumentException("record required");
        if(!record.approved)throw new IllegalArgumentException("record must be APPROVED before publication");
        String secret=secret(token);String fileName=canonicalFileName(record);byte[] bytes=serialize(record);
        return put(secret,DIRECTORY+"/"+fileName,fileName,bytes,"Add HazRob approved reference experiment ");
    }

    public static Result publishArtifact(String token,ExperimentsDb.ReferenceArtifactRecord artifact)throws Exception{
        if(artifact==null)throw new IllegalArgumentException("artifact required");
        String secret=secret(token);String kind=safeName(artifact.kind);String hash=artifact.sha256==null?"":artifact.sha256;
        String prefix=hash.length()>=12?hash.substring(0,12):hash;
        String fileName=String.format(Locale.US,"HazRob_DERIVED_%s_C%06d_A%06d_%s.json",kind,artifact.chemicalUid,artifact.id,prefix);
        JSONObject root=new JSONObject();
        root.put("schema","hazrob-derived-reference-publication/1");
        root.put("status","APPROVED_DERIVED");
        root.put("canonical_repository",REPOSITORY);
        root.put("canonical_branch",BRANCH);
        root.put("artifact_id",artifact.id);root.put("chemical_uid",artifact.chemicalUid);root.put("kind",artifact.kind);
        root.put("version",artifact.version);root.put("created_at_utc",artifact.createdTs);root.put("source_experiment_ids",artifact.sourceIds);
        root.put("payload_sha256",artifact.sha256);root.put("metrics",json(artifact.metrics));root.put("reference",json(artifact.payload));
        byte[] bytes=root.toString(2).getBytes(StandardCharsets.UTF_8);
        return put(secret,DERIVED_DIRECTORY+"/"+fileName,fileName,bytes,"Add HazRob derived reference ");
    }

    private static Result put(String secret,String path,String fileName,byte[] bytes,String messagePrefix)throws Exception{
        String payloadHash=sha256(bytes);JSONObject request=buildRequest(fileName,bytes,messagePrefix);
        URL url=new URL("https://api.github.com/repos/"+REPOSITORY+"/contents/"+path);
        HttpURLConnection c=(HttpURLConnection)url.openConnection();c.setRequestMethod("PUT");c.setConnectTimeout(12_000);c.setReadTimeout(20_000);
        c.setDoOutput(true);c.setUseCaches(false);c.setRequestProperty("Accept","application/vnd.github+json");
        c.setRequestProperty("Authorization","Bearer "+secret);c.setRequestProperty("X-GitHub-Api-Version",API_VERSION);
        c.setRequestProperty("User-Agent","HazRob-Android/0.10.2");c.setRequestProperty("Content-Type","application/json; charset=utf-8");
        byte[] body=request.toString().getBytes(StandardCharsets.UTF_8);c.setFixedLengthStreamingMode(body.length);
        try(OutputStream out=c.getOutputStream()){out.write(body);out.flush();}
        int code=c.getResponseCode();String response=read(code>=200&&code<300?c.getInputStream():c.getErrorStream());c.disconnect();
        if(code==201){JSONObject j=json(response);String sha="";JSONObject commit=j.optJSONObject("commit");if(commit!=null)sha=commit.optString("sha","");return new Result(true,code,path,sha,"published",payloadHash);}
        // Canonical experiment/reference files are intentionally append-only. HTTP 422/409 therefore fails closed rather than overwriting evidence.
        return new Result(false,code,path,"",safeError(code,response),payloadHash);
    }

    public static String canonicalFileName(ExperimentQueries.Record record){if(record==null)throw new IllegalArgumentException("record required");return ReferenceBankExporter.fileName(record.id,record.ts);}
    public static JSONObject buildRequest(String fileName,byte[] recordBytes)throws Exception{return buildRequest(fileName,recordBytes,"Add HazRob approved reference experiment ");}
    private static JSONObject buildRequest(String fileName,byte[] bytes,String prefix)throws Exception{
        if(fileName==null||fileName.trim().isEmpty())throw new IllegalArgumentException("fileName");
        if(fileName.contains("/")||fileName.contains("\\")||fileName.contains(".."))throw new IllegalArgumentException("unsafe fileName");
        if(bytes==null||bytes.length==0)throw new IllegalArgumentException("recordBytes");
        JSONObject j=new JSONObject();j.put("message",prefix+fileName);j.put("content",Base64.getEncoder().encodeToString(bytes));j.put("branch",BRANCH);return j;
    }
    private static byte[] serialize(ExperimentQueries.Record r)throws Exception{ByteArrayOutputStream out=new ByteArrayOutputStream(32*1024);ReferenceBankExporter.writeJson(out,r);return out.toByteArray();}
    private static String secret(String token){String s=token==null?"":token.trim();if(s.length()<20)throw new IllegalArgumentException("GitHub token required");GitHubSessionAuth.set(s);return s;}
    private static String safeName(String s){String x=s==null?"reference":s.toLowerCase(Locale.US).replaceAll("[^a-z0-9_-]","_");return x.isEmpty()?"reference":x;}
    private static String sha256(byte[] data)throws Exception{byte[] h=MessageDigest.getInstance("SHA-256").digest(data);StringBuilder s=new StringBuilder(64);for(byte b:h)s.append(String.format(Locale.US,"%02x",b&255));return s.toString();}
    private static JSONObject json(String s){try{return s==null||s.isEmpty()?new JSONObject():new JSONObject(s);}catch(Exception e){return new JSONObject();}}
    private static String safeError(int code,String response){JSONObject j=json(response);String m=j.optString("message","");if(m.length()>180)m=m.substring(0,180);if(m.isEmpty())m="GitHub HTTP "+code;return m;}
    private static String read(InputStream in)throws Exception{if(in==null)return"";StringBuilder s=new StringBuilder();try(BufferedReader r=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8))){String line;while((line=r.readLine())!=null&&s.length()<16_384)s.append(line);}return s.toString();}
}
