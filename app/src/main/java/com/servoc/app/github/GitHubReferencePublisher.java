package com.servoc.app.github;

import android.util.Base64;
import com.servoc.app.data.ExperimentQueries;
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

/**
 * Publishes an already APPROVED immutable experiment record to the dedicated
 * reference-bank branch. Authentication is supplied by the developer for the current
 * app session and is never persisted or included in payload/error text.
 */
public final class GitHubReferencePublisher {
    public static final String REPOSITORY="sohpany/SERVOC-Android";
    public static final String BRANCH="reference-bank";
    public static final String DIRECTORY="reference_bank/approved_experiments";
    private static final String API_VERSION="2022-11-28";

    private GitHubReferencePublisher(){}

    public static final class Result {
        public final boolean success;
        public final int httpCode;
        public final String path,commitSha,message;
        Result(boolean success,int httpCode,String path,String commitSha,String message){this.success=success;this.httpCode=httpCode;this.path=path;this.commitSha=commitSha;this.message=message;}
    }

    public static Result publish(String token,ExperimentQueries.Record record)throws Exception{
        if(record==null)throw new IllegalArgumentException("record required");
        if(!record.approved)throw new IllegalArgumentException("record must be APPROVED before publication");
        String secret=token==null?"":token.trim();if(secret.length()<20)throw new IllegalArgumentException("GitHub token required");
        String fileName=record.referenceName==null||record.referenceName.trim().isEmpty()?ReferenceBankExporter.fileName(record.id,record.ts):record.referenceName.trim();
        String path=DIRECTORY+"/"+fileName;
        byte[] recordBytes=serialize(record);
        JSONObject request=buildRequest(fileName,recordBytes);

        URL url=new URL("https://api.github.com/repos/"+REPOSITORY+"/contents/"+path);
        HttpURLConnection c=(HttpURLConnection)url.openConnection();
        c.setRequestMethod("PUT");c.setConnectTimeout(12_000);c.setReadTimeout(20_000);c.setDoOutput(true);c.setUseCaches(false);
        c.setRequestProperty("Accept","application/vnd.github+json");
        c.setRequestProperty("Authorization","Bearer "+secret);
        c.setRequestProperty("X-GitHub-Api-Version",API_VERSION);
        c.setRequestProperty("User-Agent","HazRob-Android/0.6");
        c.setRequestProperty("Content-Type","application/json; charset=utf-8");
        byte[] body=request.toString().getBytes(StandardCharsets.UTF_8);c.setFixedLengthStreamingMode(body.length);
        try(OutputStream out=c.getOutputStream()){out.write(body);out.flush();}
        int code=c.getResponseCode();String response=read(code>=200&&code<300?c.getInputStream():c.getErrorStream());c.disconnect();
        if(code==201){JSONObject j=json(response);String sha="";JSONObject commit=j.optJSONObject("commit");if(commit!=null)sha=commit.optString("sha","");return new Result(true,code,path,sha,"published");}
        String safe=safeError(code,response);return new Result(false,code,path,"",safe);
    }

    /** Request builder is public for deterministic unit tests; it never receives credentials. */
    public static JSONObject buildRequest(String fileName,byte[] recordBytes)throws Exception{
        if(fileName==null||fileName.trim().isEmpty())throw new IllegalArgumentException("fileName");if(recordBytes==null||recordBytes.length==0)throw new IllegalArgumentException("recordBytes");
        JSONObject j=new JSONObject();j.put("message","Add HazRob approved reference experiment "+fileName);j.put("content",Base64.encodeToString(recordBytes,Base64.NO_WRAP));j.put("branch",BRANCH);return j;
    }

    private static byte[] serialize(ExperimentQueries.Record r)throws Exception{ByteArrayOutputStream out=new ByteArrayOutputStream(32*1024);ReferenceBankExporter.writeJson(out,r);return out.toByteArray();}
    private static JSONObject json(String s){try{return s==null||s.isEmpty()?new JSONObject():new JSONObject(s);}catch(Exception e){return new JSONObject();}}
    private static String safeError(int code,String response){JSONObject j=json(response);String m=j.optString("message","");if(m.length()>180)m=m.substring(0,180);if(m.isEmpty())m="GitHub HTTP "+code;return m;}
    private static String read(InputStream in)throws Exception{if(in==null)return"";StringBuilder s=new StringBuilder();try(BufferedReader r=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8))){String line;while((line=r.readLine())!=null&&s.length()<16_384)s.append(line);}return s.toString();}
}
