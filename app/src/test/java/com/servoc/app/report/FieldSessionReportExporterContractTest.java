package com.servoc.app.report;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import static org.junit.Assert.*;

/** Guards exact-ID binding and the multi-session evidence-driven Field Session report contract. */
public class FieldSessionReportExporterContractTest {
 private static String src(String path)throws Exception{return new String(Files.readAllBytes(Paths.get(path)),StandardCharsets.UTF_8);}

 @Test public void reportRootIsFieldSessionAndResolutionIsExact() throws Exception {
  String s=src("src/main/java/com/servoc/app/report/FieldSessionReportExporter.java");
  assertTrue(s.contains("measurementSessionIds(fs)"));
  assertTrue(s.contains("ExperimentQueries.bySessionId(db,sid)"));
  assertFalse(s.contains("SessionSeries.nearest"));
  assertFalse(s.contains("ORDER BY ABS"));
 }

 @Test public void pdfRoutesToNewMultiSessionWriterAndKeepsXlsxCompanion() throws Exception {
  String s=src("src/main/java/com/servoc/app/report/FieldSessionReportExporter.java");
  assertTrue(s.contains("MIN_PAGES=7"));assertTrue(s.contains("MAX_PAGES=10"));
  assertTrue(s.contains("SmartFieldSessionPdfWriter.writePdf(out,db,fieldSessionId,chemicals)"));
  assertTrue(s.contains("SmartFieldSessionXlsxWriter.write(out,db,b,chemicals)"));
 }

 @Test public void smartPdfUsesEveryIndependentMeasurementSession() throws Exception {
  String s=src("src/main/java/com/servoc/app/report/SmartFieldSessionPdfWriter.java");
  assertTrue(s.contains("for(String sid:ids)"));
  assertTrue(s.contains("SessionSeries.byId(db,id)"));
  assertTrue(s.contains("decisions.latestPayloadForSession(id)"));
  assertTrue(s.contains("ExperimentQueries.bySessionId(db,id).size()"));
  assertTrue(s.contains("كل Measurement Session تبقى مستقلة زمنيًا"));
  assertTrue(s.contains("لا يتم دمجها في منحنى مصطنع"));
  assertFalse(s.contains("SessionSeries.nearest"));
 }

 @Test public void primaryIsEvidenceCompletenessChoiceNotBlindLastSession() throws Exception {
  String s=src("src/main/java/com/servoc/app/report/SmartFieldSessionPdfWriter.java");
  assertTrue(s.contains("e.score>=best.score"));
  assertTrue(s.contains("if(s!=null&&s.usable())"));
  assertTrue(s.contains("if(d!=null)"));
  assertTrue(s.contains("ppm_reportable"));
  assertTrue(s.contains("Primary selection: evidence completeness + ChemCore + usable series"));
 }

 @Test public void pdfIsDynamicLandscapeSevenToTenPagesAndRichInCharts() throws Exception {
  String s=src("src/main/java/com/servoc/app/report/SmartFieldSessionPdfWriter.java");
  assertTrue(s.contains("PAGE_WIDTH=842"));assertTrue(s.contains("PAGE_HEIGHT=595"));
  assertTrue(s.contains("MIN_PAGES=7"));assertTrue(s.contains("MAX_PAGES=10"));
  assertTrue(s.contains("int total=7+(anySeries?1:0)+(anyChem?1:0)+(multi?1:0)"));
  assertTrue(s.contains("AnalyticalCharts.mqRsR0"));assertTrue(s.contains("AnalyticalCharts.fingerprint"));
  assertTrue(s.contains("AnalyticalCharts.mqSignalQuality"));assertTrue(s.contains("AnalyticalCharts.fingerprintRadar"));
  assertTrue(s.contains("AnalyticalCharts.mqResponseKinetics"));assertTrue(s.contains("AnalyticalCharts.mqPersistenceMap"));
  assertTrue(s.contains("AnalyticalCharts.candidates"));assertTrue(s.contains("AnalyticalCharts.candidateScatter"));
  assertTrue(s.contains("AnalyticalCharts.candidateBubbleMap"));assertTrue(s.contains("AnalyticalCharts.candidateResiduals"));
  assertTrue(s.contains("AnalyticalCharts.screeningDonut"));assertTrue(s.contains("SmartReportCharts.screening144ReliabilityMatrix"));
 }

 @Test public void reportShowsCrossSessionConsistencyWithoutUpgradingIdentity() throws Exception {
  String s=src("src/main/java/com/servoc/app/report/SmartFieldSessionPdfWriter.java");
  assertTrue(s.contains("الثبات والاختلاف بين جلسات القياس"));
  assertTrue(s.contains("candidateStability"));assertTrue(s.contains("posteriorStability"));
  assertTrue(s.contains("ثبات المرشح بين الجلسات يصف الاتساق ولا يثبت الهوية"));
  assertTrue(s.contains("Screening-fit concentration لا يساوي PPM مهني قابل للتقرير"));
 }

 @Test public void siteIdentityHazardContextAndProvenanceRemainVisible() throws Exception {
  String s=src("src/main/java/com/servoc/app/report/SmartFieldSessionPdfWriter.java");
  String h=src("src/main/java/com/servoc/app/report/CandidateHazardContext.java");
  assertTrue(s.contains("b.site.name+\" [\"+shorten(b.site.id,18)+\"]"));
  assertTrue(s.contains("CandidateHazardContext.evaluate(db,primaryDecision)"));
  assertTrue(s.contains("CandidateHazardCharts.table"));
  assertTrue(s.contains("Library SHA-256"));assertTrue(s.contains("Sampling profile"));
  assertTrue(h.contains("SCREENING_FIT_ESTIMATE"));assertTrue(h.contains("M4_MEASURED_REPORTABLE"));
 }

 @Test public void trackingRemainsOperationalAndExcludedFromReportContent() throws Exception {
  String s=src("src/main/java/com/servoc/app/report/SmartFieldSessionPdfWriter.java");
  String x=src("src/main/java/com/servoc/app/report/SmartFieldSessionXlsxWriter.java");
  assertTrue(s.contains("وظيفة Tracking التشغيلية غير مدرجة في التقرير"));
  assertTrue(x.contains("Tracking وظيفة تشغيلية وغير مدرجة في التقرير"));
 }

 @Test public void xlsxStillPreservesIndependentMeasurementSessions() throws Exception {
  String x=src("src/main/java/com/servoc/app/report/SmartFieldSessionXlsxWriter.java");
  assertTrue(x.contains("السلاسل الزمنية — لا يتم دمج الجلسات الفنية في منحنى مصطنع"));
  assertTrue(x.contains("for(String sid:b.measurementSessionIds)"));
  assertTrue(x.contains("h.add(\"Measurement Session\")"));
  assertTrue(x.contains("CandidateHazardContext.evaluate(db,cc)"));
  assertTrue(x.contains("مصفوفة 144"));assertTrue(x.contains("حدود التعرض"));assertTrue(x.contains("الجودة وعدم اليقين"));
 }
}
