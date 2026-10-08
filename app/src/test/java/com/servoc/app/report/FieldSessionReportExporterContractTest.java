package com.servoc.app.report;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import static org.junit.Assert.*;

/** Guards exact-ID binding and the compact multi-session Field Session report contract. */
public class FieldSessionReportExporterContractTest {
 private static String src(String path)throws Exception{return new String(Files.readAllBytes(Paths.get(path)),StandardCharsets.UTF_8);}

 @Test public void reportRootIsFieldSessionAndResolutionIsExact() throws Exception {
  String s=src("src/main/java/com/servoc/app/report/FieldSessionReportExporter.java");
  assertTrue(s.contains("measurementSessionIds(fs)"));assertTrue(s.contains("ExperimentQueries.bySessionId(db,sid)"));
  assertFalse(s.contains("SessionSeries.nearest"));assertFalse(s.contains("ORDER BY ABS"));
 }

 @Test public void excelRemainsAvailableBeforeDerivedChemCoreExperiment()throws Exception {
  String facade=src("src/main/java/com/servoc/app/report/FieldSessionReportExporter.java");
  String workbook=src("src/main/java/com/servoc/app/report/SmartFieldSessionXlsxWriter.java");
  assertFalse(facade.contains("if(b.records.isEmpty())throw"));
  assertTrue(facade.contains("SmartFieldSessionXlsxWriter.write(out,db,b,chemicals)"));
  assertTrue(workbook.contains("sheets.add(new SmartXlsxPackage.Sheet(\"بيانات الجلسة\""));
  assertTrue(workbook.contains("sheets.add(new SmartXlsxPackage.Sheet(\"السلسلة الزمنية\""));
  assertTrue(workbook.contains("g.row(\"Derived experiment records\",b.records.size())"));
 }

 @Test public void pdfIsCompactFourToFivePagesAndXlsxStaysDetailed() throws Exception {
  String f=src("src/main/java/com/servoc/app/report/FieldSessionReportExporter.java");
  String s=src("src/main/java/com/servoc/app/report/SmartFieldSessionPdfWriter.java");
  assertTrue(f.contains("MIN_PAGES=4"));assertTrue(f.contains("MAX_PAGES=5"));
  assertTrue(s.contains("MIN_PAGES=4"));assertTrue(s.contains("MAX_PAGES=5"));assertTrue(s.contains("CHARTS_PER_PAGE=6"));
  assertTrue(s.contains("int total=all.size()>1?5:4"));
  assertTrue(f.contains("SmartFieldSessionPdfWriter.writePdf(out,db,fieldSessionId,chemicals)"));
  assertTrue(f.contains("SmartFieldSessionXlsxWriter.write(out,db,b,chemicals)"));
 }

 @Test public void analyticalPagesUseSixChartThreeByTwoGrid() throws Exception {
  String s=src("src/main/java/com/servoc/app/report/SmartFieldSessionPdfWriter.java");
  assertTrue(s.contains("private static RectF[] grid6()"));
  assertTrue(s.contains("RectF[] r=new RectF[6]"));
  assertTrue(s.contains("for(int row=0;row<2;row++)for(int col=0;col<3;col++)"));
  assertTrue(s.contains("sensorSix"));assertTrue(s.contains("chemCoreSix"));assertTrue(s.contains("contextSix"));
 }

 @Test public void sensorPageContainsSixDistinctAnalyticalCharts() throws Exception {
  String s=src("src/main/java/com/servoc/app/report/SmartFieldSessionPdfWriter.java");
  assertTrue(s.contains("AnalyticalCharts.mqRsR0(c,r[0],s)"));assertTrue(s.contains("AnalyticalCharts.fingerprint(c,r[1],s)"));
  assertTrue(s.contains("SmartReportAdvancedCharts.mqCorrelationMatrix(c,r[2],s)"));assertTrue(s.contains("AnalyticalCharts.fingerprintRadar(c,r[3],s)"));
  assertTrue(s.contains("AnalyticalCharts.mqResponseKinetics(c,r[4],s)"));assertTrue(s.contains("AnalyticalCharts.mqPersistenceMap(c,r[5],s)"));
 }

 @Test public void chemCorePageContainsSixCandidateUncertaintyAndDynamicMolecularCharts() throws Exception {
  String s=src("src/main/java/com/servoc/app/report/SmartFieldSessionPdfWriter.java");
  assertTrue(s.contains("AnalyticalCharts.candidates(c,r[0],q)"));assertTrue(s.contains("AnalyticalCharts.candidateScatter(c,r[1],q)"));
  assertTrue(s.contains("SmartReportAdvancedCharts.curveConsistency(c,r[2],q)"));assertTrue(s.contains("AnalyticalCharts.candidateResiduals(c,r[3],q)"));
  assertTrue(s.contains("MolecularReportCharts.hasQsprEvidence(q)"));assertTrue(s.contains("MolecularReportCharts.qsprEvidence(c,r[4],q)"));
  assertTrue(s.contains("else AnalyticalCharts.evidenceTiers(c,r[4],q)"));assertTrue(s.contains("SmartReportCharts.decisionQuality(c,r[5],q)"));
 }

 @Test public void contextPageContains144EnvironmentAndConditionalSafety() throws Exception {
  String s=src("src/main/java/com/servoc/app/report/SmartFieldSessionPdfWriter.java");
  assertTrue(s.contains("SmartReportCharts.candidateEvidenceMatrix(c,r[0],q)"));
  assertTrue(s.contains("SmartReportCharts.screening144ReliabilityMatrix(c,r[1],q)"));
  assertTrue(s.contains("SmartReportCharts.top144SessionSupport(c,r[2],q)"));
  assertTrue(s.contains("AnalyticalCharts.screeningDonut(c,r[3],q)"));
  assertTrue(s.contains("AnalyticalCharts.mqEnvironmentMatrix(c,r[4],s)"));
  assertTrue(s.contains("if(plan.ppmReportable)AnalyticalCharts.exposureRatios(c,r[5],q)"));
 }

 @Test public void smartPdfUsesEveryIndependentMeasurementSessionButConsumesOneFacadeSelectedPrimary() throws Exception {
  String f=src("src/main/java/com/servoc/app/report/FieldSessionReportExporter.java");
  String s=src("src/main/java/com/servoc/app/report/SmartFieldSessionPdfWriter.java");
  assertTrue(s.contains("for(String sid:ids)"));assertTrue(s.contains("SessionSeries.byId(db,id)"));
  assertTrue(s.contains("decisions.latestPayloadForSession(id)"));assertTrue(s.contains("ExperimentQueries.bySessionId(db,id).size()"));
  assertTrue(f.contains("SmartReportPrimarySelector.score(candidateSeries,candidateDecision,candidateRecords)"));
  assertTrue(s.contains("SmartReportAnalyticalContract contract=b.analyticalContract"));
  assertTrue(s.contains("b.primaryMeasurementSessionId"));
  assertFalse("PDF must not re-rank the primary session",s.contains("e.score>=best.score"));
  assertFalse("PDF must not carry a duplicate private scoring policy",s.contains("private static int score("));
  assertTrue(s.contains("كل Measurement Session مستقلة"));
  assertTrue(s.contains("لا يتم دمج السلاسل الزمنية"));assertFalse(s.contains("SessionSeries.nearest"));
 }

 @Test public void multiSessionPageShowsConsistencyWithoutUpgradingIdentity() throws Exception {
  String s=src("src/main/java/com/servoc/app/report/SmartFieldSessionPdfWriter.java");
  assertTrue(s.contains("الثبات بين الجلسات"));assertTrue(s.contains("candidateStability"));assertTrue(s.contains("posteriorStability"));
  assertTrue(s.contains("ثبات المرشح يصف الاتساق ولا يثبت الهوية"));
 }

 @Test public void provenanceAndHazardContextRemainAvailable() throws Exception {
  String s=src("src/main/java/com/servoc/app/report/SmartFieldSessionPdfWriter.java");
  String h=src("src/main/java/com/servoc/app/report/CandidateHazardContext.java");
  assertTrue(s.contains("CandidateHazardContext.evaluate(db,q)"));assertTrue(s.contains("CandidateHazardCharts.executiveStrip"));
  assertTrue(s.contains("Library SHA-256"));assertTrue(s.contains("Sampling profile"));
  assertTrue(h.contains("SCREENING_FIT_ESTIMATE"));assertTrue(h.contains("M4_MEASURED_REPORTABLE"));
 }

 @Test public void xlsxStillPreservesIndependentMeasurementSessions() throws Exception {
  String x=src("src/main/java/com/servoc/app/report/SmartFieldSessionXlsxWriter.java");
  assertTrue(x.contains("السلاسل الزمنية — لا يتم دمج الجلسات الفنية في منحنى مصطنع"));
  assertTrue(x.contains("for(String sid:b.measurementSessionIds)"));assertTrue(x.contains("h.add(\"Measurement Session\")"));
  assertTrue(x.contains("CandidateHazardContext.evaluate(db,cc)"));assertTrue(x.contains("مصفوفة 144"));assertTrue(x.contains("حدود التعرض"));
 }
}