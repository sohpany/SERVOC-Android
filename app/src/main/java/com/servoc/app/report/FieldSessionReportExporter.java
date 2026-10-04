package com.servoc.app.report;

import com.servoc.app.chemical.ChemicalRepository;
import com.servoc.app.chemcore.ChemCoreDecisionRepository;
import com.servoc.app.data.ExperimentQueries;
import com.servoc.app.data.ExperimentsDb;
import com.servoc.app.data.SessionSeries;
import com.servoc.app.field.FieldSessionRepository;

import org.json.JSONObject;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/** Stable Field Session report facade. PDF is compact; XLSX remains the detailed auditable workbook. */
public final class FieldSessionReportExporter {
    private FieldSessionReportExporter(){}
    public static final int PAGE_WIDTH=842,PAGE_HEIGHT=595,MIN_PAGES=4,MAX_PAGES=5;

    public static final class Bundle {
        public final String fieldSessionId;
        public final FieldSessionRepository.Site site;
        public final FieldSessionRepository.FieldSession fieldSession;
        public final List<String> measurementSessionIds;
        public final List<ExperimentQueries.Record> records;
        public final String primaryMeasurementSessionId;
        public final SessionSeries.Series primarySeries;
        public final JSONObject primaryDecision;
        /** Shared ChemCore -> SmartReport contract consumed by both PDF and XLSX paths. */
        public final SmartReportAnalyticalContract analyticalContract;
        Bundle(String fieldSessionId,FieldSessionRepository.Site site,FieldSessionRepository.FieldSession fieldSession,
               List<String> measurementSessionIds,List<ExperimentQueries.Record> records,String primaryMeasurementSessionId,
               SessionSeries.Series primarySeries,JSONObject primaryDecision,SmartReportAnalyticalContract analyticalContract){
            this.fieldSessionId=fieldSessionId;this.site=site;this.fieldSession=fieldSession;this.measurementSessionIds=measurementSessionIds;
            this.records=records;this.primaryMeasurementSessionId=primaryMeasurementSessionId;this.primarySeries=primarySeries;this.primaryDecision=primaryDecision;
            this.analyticalContract=analyticalContract;
        }
    }

    public static Bundle resolve(ExperimentsDb db,String fieldSessionId){
        if(db==null)throw new IllegalArgumentException("db required");String fs=required(fieldSessionId);
        FieldSessionRepository repo=new FieldSessionRepository(db);FieldSessionRepository.Site site=null;FieldSessionRepository.FieldSession field=null;
        for(FieldSessionRepository.Site s:repo.sites()){for(FieldSessionRepository.FieldSession q:repo.sessionsForSite(s.id))if(fs.equals(q.id)){site=s;field=q;break;}if(field!=null)break;}
        if(field==null||site==null)throw new IllegalArgumentException("field session not found");
        List<String> sessions=repo.measurementSessionIds(fs);List<ExperimentQueries.Record> records=new ArrayList<>();for(String sid:sessions)records.addAll(ExperimentQueries.bySessionId(db,sid));
        ChemCoreDecisionRepository decisions=new ChemCoreDecisionRepository(db);String primary="";JSONObject decision=null;
        for(int i=sessions.size()-1;i>=0;i--){String sid=sessions.get(i);JSONObject p=decisions.latestPayloadForSession(sid);if(p!=null){primary=sid;decision=p;break;}}
        if(primary.isEmpty()&&!sessions.isEmpty())primary=sessions.get(sessions.size()-1);
        SessionSeries.Series series=primary.isEmpty()?null:SessionSeries.byId(db,primary);
        SmartReportAnalyticalContract contract=SmartReportAnalyticalContract.bind(series,decision);
        return new Bundle(fs,site,field,new ArrayList<>(sessions),records,primary,series,decision,contract);
    }

    public static void writePdf(OutputStream out,ExperimentsDb db,String fieldSessionId,ChemicalRepository chemicals)throws Exception{
        // SmartFieldSessionPdfWriter resolves the bundle and therefore cannot bypass the shared contract guard.
        SmartFieldSessionPdfWriter.writePdf(out,db,fieldSessionId,chemicals);
    }
    public static void writeXlsx(OutputStream out,ExperimentsDb db,String fieldSessionId,ChemicalRepository chemicals)throws Exception{
        Bundle b=resolve(db,fieldSessionId);
        if(b.records.isEmpty())throw new IllegalStateException("field session has no derived experiment workbook");
        // XLSX receives the same already-validated Bundle/contract used by the field-report system.
        SmartFieldSessionXlsxWriter.write(out,db,b,chemicals);
    }
    private static String required(String s){String v=s==null?"":s.trim();if(v.isEmpty())throw new IllegalArgumentException("field session required");return v;}
}
