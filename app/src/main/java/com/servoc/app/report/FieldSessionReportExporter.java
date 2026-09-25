package com.servoc.app.report;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.servoc.app.chemical.ChemicalRepository;
import com.servoc.app.data.ExperimentQueries;
import com.servoc.app.data.ExperimentsDb;
import com.servoc.app.field.FieldSessionRepository;

import java.io.OutputStream;
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
        SQLiteDatabase r=db.getReadableDatabase();
        for(String sid:sessions){
            try(Cursor c=r.rawQuery("SELECT id FROM experiments WHERE session_id=? ORDER BY id",new String[]{sid})){
                while(c.moveToNext()){
                    ExperimentQueries.Record q=ExperimentQueries.byId(db,c.getLong(0));
                    if(q!=null)out.add(q);
                }
            }
        }
        return new Bundle(fs,out);
    }

    public static void writePdf(OutputStream out,ExperimentsDb db,String fieldSessionId,ChemicalRepository chemicals)throws Exception{
        Bundle b=resolve(db,fieldSessionId);
        if(b.records.isEmpty())throw new IllegalStateException("field session has no reportable measurements");
        // Existing per-measurement report contract remains authoritative until the
        // multi-measurement renderer is introduced. Never silently choose another session.
        if(b.records.size()!=1)throw new IllegalStateException("field session aggregate PDF renderer required");
        ReportExporter.writePdf(out,b.records.get(0),chemicals);
    }

    public static void writeXlsx(OutputStream out,ExperimentsDb db,String fieldSessionId,ChemicalRepository chemicals)throws Exception{
        Bundle b=resolve(db,fieldSessionId);
        if(b.records.isEmpty())throw new IllegalStateException("field session has no reportable measurements");
        if(b.records.size()!=1)throw new IllegalStateException("field session aggregate XLSX renderer required");
        ReportExporter.writeXlsx(out,b.records.get(0),chemicals);
    }

    private static String required(String s){
        String v=s==null?"":s.trim();
        if(v.isEmpty())throw new IllegalArgumentException("field session required");
        return v;
    }
}
