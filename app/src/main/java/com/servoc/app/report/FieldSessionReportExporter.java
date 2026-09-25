package com.servoc.app.report;

import com.servoc.app.chemical.ChemicalRepository;
import com.servoc.app.data.ExperimentQueries;
import com.servoc.app.data.ExperimentsDb;
import com.servoc.app.field.FieldSessionRepository;

import java.io.OutputStream;\nimport java.io.ByteArrayOutputStream;\nimport java.nio.charset.StandardCharsets;\nimport java.util.zip.ZipEntry;\nimport java.util.zip.ZipOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Field-session report bridge.
 *
 * The immutable field_session_id is the report root. Measurement records are resolved
 * only through field_session_measurements -> measurement_sessions -> experiments.session_id.
 * No timestamp/nearest-session fallback is permitted.
 */
public final class FieldSessionReportExporter {
    private FieldSessionReportExporter(){}

    public static final class Bundle {
        public final String fieldSessionId;
        public final List<ExperimentQueries.Record> records;
        Bundle(String fieldSessionId,List<ExperimentQueries.Record> records){
            this.fieldSessionId=fieldSessionId;this.records=records;
        }
    }

    public static Bundle resolve(ExperimentsDb db,String fieldSessionId){
        if(db==null)throw new IllegalArgumentException("db required");
        String fs=required(fieldSessionId);
        FieldSessionRepository repo=new FieldSessionRepository(db);
        List<String> sessions=repo.measurementSessionIds(fs);
        List<ExperimentQueries.Record> out=new ArrayList<>();
        for(String sid:sessions)out.addAll(ExperimentQueries.bySessionId(db,sid));
        return new Bundle(fs,out);
    }

    public static void writePdf(OutputStream out,ExperimentsDb db,String fieldSessionId,ChemicalRepository chemicals)throws Exception{
        Bundle b=resolve(db,fieldSessionId);
        if(b.records.isEmpty())throw new IllegalStateException("field session has no derived experiment report; raw measurement remains preserved");
        if(b.records.size()==1){ReportExporter.writePdf(out,b.records.get(0),chemicals);return;}
        // A multi-measurement field session is exported as one lossless report package.
        // Each child PDF remains bound to its exact immutable measurement session.
        try(ZipOutputStream zip=new ZipOutputStream(out)){
            putManifest(zip,b);
            for(ExperimentQueries.Record r:b.records){
                ByteArrayOutputStream q=new ByteArrayOutputStream();
                ReportExporter.writePdf(q,r,chemicals);
                put(zip,"measurements/measurement-"+r.id+"-"+safe(r.sessionId)+".pdf",q.toByteArray());
            }
            zip.finish();
        }
    }

    public static void writeXlsx(OutputStream out,ExperimentsDb db,String fieldSessionId,ChemicalRepository chemicals)throws Exception{
        Bundle b=resolve(db,fieldSessionId);
        if(b.records.isEmpty())throw new IllegalStateException("field session has no derived experiment report; raw measurement remains preserved");
        if(b.records.size()==1){ReportExporter.writeXlsx(out,b.records.get(0),chemicals);return;}
        try(ZipOutputStream zip=new ZipOutputStream(out)){
            putManifest(zip,b);
            for(ExperimentQueries.Record r:b.records){
                ByteArrayOutputStream q=new ByteArrayOutputStream();
                ReportExporter.writeXlsx(q,r,chemicals);
                put(zip,"measurements/measurement-"+r.id+"-"+safe(r.sessionId)+".xlsx",q.toByteArray());
            }
            zip.finish();
        }
    }

    private static void putManifest(ZipOutputStream z,Bundle b)throws Exception{
        StringBuilder j=new StringBuilder("{\n  \"schema\": \"hazrob-field-report/1\",\n  \"field_session_id\": \"")
            .append(json(b.fieldSessionId)).append("\",\n  \"measurement_count\": ").append(b.records.size()).append(",\n  \"measurements\": [");
        for(int i=0;i<b.records.size();i++){ExperimentQueries.Record r=b.records.get(i);if(i>0)j.append(",");
            j.append("\n    {\"experiment_id\":").append(r.id).append(",\"session_id\":\"").append(json(r.sessionId)).append("\",\"utc\":\"").append(json(r.ts)).append("\"}");}
        j.append("\n  ]\n}\n");put(z,"manifest.json",j.toString().getBytes(StandardCharsets.UTF_8));
    }
    private static void put(ZipOutputStream z,String name,byte[] bytes)throws Exception{ZipEntry e=new ZipEntry(name);z.putNextEntry(e);z.write(bytes);z.closeEntry();}
    private static String safe(String s){return (s==null?"unknown":s).replaceAll("[^A-Za-z0-9._-]","_");}
    private static String json(String s){if(s==null)return "";return s.replace("\\\\","\\\\\\\\").replace("\"","\\\\\"").replace("\n","\\
").replace("\r","\\\\r");}

    private static String required(String s){
        String v=s==null?"":s.trim();
        if(v.isEmpty())throw new IllegalArgumentException("field session required");
        return v;
    }
}
