package com.servoc.app.ui;

import android.content.ContentValues;
import android.content.Context;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.servoc.app.calibration.FieldR0Baseline;
import com.servoc.app.chemcore.ChemCoreAnalysisPresenter;
import com.servoc.app.chemcore.ChemCoreDecision;
import com.servoc.app.chemcore.ChemCoreDecisionRepository;
import com.servoc.app.chemcore.ChemCoreFeaturePipeline;
import com.servoc.app.chemcore.ChemCoreInferenceEngine;
import com.servoc.app.chemcore.ChemCoreLabQuality;
import com.servoc.app.chemcore.ChemCoreLiveStabilityMonitor;
import com.servoc.app.chemcore.ChemCoreOpenSetGate;
import com.servoc.app.chemcore.ChemCoreResultEnvelope;
import com.servoc.app.chemcore.ChemCoreRuntimeCoordinator;
import com.servoc.app.chemcore.ChemCoreSessionController;
import com.servoc.app.chemcore.ChemCoreSessionFeatureAdapter;
import com.servoc.app.chemcore.InverseMolecularSolver;
import com.servoc.app.chemcore.library.ChemCoreLibraryManager;
import com.servoc.app.chemcore.library.ChemCoreStableKnowledgeReader;
import com.servoc.app.measurement.MeasurementSessionRegistry;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/** Controlled baseline -> exposure -> analysis -> recovery ChemCore workflow. */
public final class ChemCoreAnalysisTab {
    private static final long MIN_EXPOSURE_MS=30_000L,EXTENSION_MS=15_000L,MAX_EXPOSURE_MS=120_000L;
    private final HazRobHost host;private final ScrollView scroll;private final ChemCoreRuntimeCoordinator runtime;private final ChemCoreLiveStabilityMonitor stability=new ChemCoreLiveStabilityMonitor();private final ChemCoreLibraryManager libraries;
    private final TextView phase,status,signal,resultText;private final Button start,action,abort;
    private boolean pendingPowerStart=false,active=false,awaitingSample=false,sampleConfirmed=false,awaitingRecoveryRemoval=false,recoveryConfirmed=false,pendingPrimaryFinalize=false;
    private long conditionStartedMs=0,exposureStartedMs=0,nextExposureCheckMs=0;private String attachedSessionId="";private ChemCoreResultEnvelope lastResult;

    public ChemCoreAnalysisTab(Context c,HazRobHost host){
        this.host=host;runtime=new ChemCoreRuntimeCoordinator(host.database());libraries=new ChemCoreLibraryManager(c);
        scroll=new ScrollView(c);scroll.setFillViewport(true);LinearLayout root=UiKit.vbox(c);root.setPadding(UiKit.dp(c,8),UiKit.dp(c,6),UiKit.dp(c,8),UiKit.dp(c,14));root.setBackgroundColor(0xFF0E1116);scroll.addView(root);
        root.addView(UiKit.title(c,"ChemCore • التحليل الكيميائي المضبوط"),UiKit.full(c));
        root.addView(UiKit.small(c,"CONDITION → BASELINE → EXPOSURE → FEATURE/DECISION → RECOVERY. يستخدم R0 المعتمد HR-0101-045 من Rs على مستوى التطبيق، ويمنع PPM القابل للتقرير دون M4 كامل."),UiKit.full(c));
        LinearLayout profile=UiKit.card(c);profile.addView(UiKit.title(c,"Sampling Profile"),UiKit.full(c));profile.addView(UiKit.small(c,"Chamber v2 • 0.517 L effective • Qest 1.10±0.40 L/s • v≈0.31 m/s • FLOW_ESTIMATED"),UiKit.full(c));root.addView(profile,UiKit.full(c));
        LinearLayout run=UiKit.card(c);phase=UiKit.title(c,"IDLE");run.addView(phase,UiKit.full(c));status=UiKit.small(c,"جاهز لبدء جلسة مضبوطة.");status.setBackgroundColor(0xFF151A20);run.addView(status,UiKit.full(c));signal=UiKit.small(c,"Stability: —");signal.setBackgroundColor(0xFF151A20);run.addView(signal,UiKit.full(c));
        LinearLayout buttons=UiKit.hbox(c);start=UiKit.button(c,"بدء ChemCore");start.setOnClickListener(v->startProtocol());buttons.addView(start,UiKit.weight(1));action=UiKit.button(c,"—");action.setEnabled(false);action.setOnClickListener(v->primaryAction());buttons.addView(action,UiKit.weight(1));abort=UiKit.button(c,"إلغاء آمن");abort.setEnabled(false);abort.setOnClickListener(v->abortProtocol("operator_abort"));buttons.addView(abort,UiKit.weight(.8f));run.addView(buttons,UiKit.full(c));root.addView(run,UiKit.full(c));
        LinearLayout r=UiKit.card(c);r.addView(UiKit.title(c,"النتيجة العلمية الموسعة"),UiKit.full(c));resultText=UiKit.text(c,"لا توجد نتيجة بعد.");resultText.setTextIsSelectable(true);resultText.setBackgroundColor(0xFF11171D);r.addView(resultText,UiKit.full(c));root.addView(r,UiKit.full(c));render();
    }

    public View view(){return scroll;}
    public void refresh(){
        JSONObject d=host.latestDiagnostics();long now=SystemClock.elapsedRealtime();if(pendingPowerStart&&mqPowered(d)){pendingPowerStart=false;beginAfterPowerConfirmed(now);}if(!active){render();return;}
        ChemCoreRuntimeCoordinator.Snapshot motion=runtime.observeMotion(host.driveLeft(),host.driveRight());if(motion.state==ChemCoreSessionController.State.ERROR){MeasurementSessionRegistry.requestChemCoreStop("motion_interlock");active=false;status.setText("تم إيقاف التحليل: اكتشفت حركة للروبوت.");render();return;}if(!mqPowered(d)){MeasurementSessionRegistry.requestChemCoreStop("mq_power_lost");runtime.fail("{\"reason\":\"mq_power_lost_or_telemetry_missing\"}");active=false;status.setText("تم إيقاف التحليل: طاقة مصفوفة MQ أو إثباتها التليمترى غير متاح.");render();return;}
        ChemCoreSessionController.State st=motion.state;
        if(st==ChemCoreSessionController.State.CONDITION){stability.accept(d,now);ChemCoreLiveStabilityMonitor.Assessment a=stability.assess();long required=Math.max(30,host.preferences().getInt("warm",60))*1000L;long localElapsed=Math.max(0,now-conditionStartedMs),firmwarePowerElapsed=relayPowerMs(d),conditioned=Math.max(localElapsed,firmwarePowerElapsed);signal.setText("Condition • "+a.summary()+" • heater "+Math.min(required,conditioned)/1000+"/"+required/1000+" s");if(conditioned>=required&&a.stable){runtime.advance(ChemCoreSessionController.Event.CONDITION_READY,true,"{\"gate\":\"relay_power_time_plus_stability\",\"relay_power_ms\":"+firmwarePowerElapsed+",\"local_elapsed_ms\":"+localElapsed+"}");if(!MeasurementSessionRegistry.requestChemCoreCapture(true)){runtime.abort("{\"reason\":\"telemetry_session_busy\"}");active=false;status.setText("تعذر بدء التسجيل: توجد جلسة Telemetry أخرى نشطة.");}else{stability.reset();status.setText("تم اجتياز conditioning. جارٍ فتح جلسة baseline محلية مستقلة...");}}}
        else if(st==ChemCoreSessionController.State.BASELINE){attachLiveSession();if(!attachedSessionId.isEmpty()){stability.accept(d,now);ChemCoreLiveStabilityMonitor.Assessment a=stability.assess();signal.setText("Baseline • "+a.summary());if(a.stable){runtime.advance(ChemCoreSessionController.Event.BASELINE_STABLE,true,"{\"gate\":\"multichannel_baseline_stable\"}");awaitingSample=true;sampleConfirmed=false;stability.reset();status.setText("Baseline صالح. أدخل العينة في مسار السحب ثم أكد بدء التعرض.");}}}
        else if(st==ChemCoreSessionController.State.EXPOSURE&&sampleConfirmed){stability.accept(d,now);ChemCoreLiveStabilityMonitor.Assessment a=stability.assess();signal.setText("Exposure • "+a.summary());long elapsed=now-exposureStartedMs;if(now>=nextExposureCheckMs&&elapsed>=MIN_EXPOSURE_MS){runtime.advance(ChemCoreSessionController.Event.EXPOSURE_WINDOW_READY,true,"{\"elapsed_ms\":"+elapsed+"}");if(a.stable){runtime.advance(ChemCoreSessionController.Event.RESPONSE_STABLE,true,"{\"gate\":\"multichannel_response_stable\"}");pendingPrimaryFinalize=true;MeasurementSessionRegistry.requestChemCoreStop("chemcore_primary_capture_complete");status.setText("الاستجابة مستقرة. جارٍ تثبيت السلسلة الزمنية واستخراج الخصائص.");}else if(elapsed<MAX_EXPOSURE_MS){runtime.advance(ChemCoreSessionController.Event.EXTEND_EXPOSURE,true,"{\"reason\":\"response_unstable\"}");nextExposureCheckMs=now+EXTENSION_MS;status.setText("الاستجابة غير مستقرة؛ AMCI يمدد التعرض تلقائياً.");}else{runtime.advance(ChemCoreSessionController.Event.EVIDENCE_INSUFFICIENT,true,"{\"reason\":\"stability_not_reached_at_max_exposure\"}");pendingPrimaryFinalize=true;MeasurementSessionRegistry.requestChemCoreStop("chemcore_insufficient_stability");status.setText("بلغ التعرض الحد البحثي دون استقرار كافٍ؛ النتيجة ستسجل كدليل غير كافٍ.");}}}
        else if(st==ChemCoreSessionController.State.FEATURE_EXTRACTION||pendingPrimaryFinalize)tryFinalizePrimary();
        else if(st==ChemCoreSessionController.State.RECOVERY&&recoveryConfirmed){stability.accept(d,now);ChemCoreLiveStabilityMonitor.Assessment a=stability.assess();signal.setText("Recovery • "+a.summary());if(a.stable){runtime.advance(ChemCoreSessionController.Event.RECOVERY_COMPLETE,true,"{\"gate\":\"recovery_stable\"}");active=false;status.setText("اكتملت جلسة ChemCore وحُفظت provenance.");}}
        render();
    }

    private void startProtocol(){if(active||pendingPowerStart)return;if(!MeasurementSessionRegistry.activeSessionId().isEmpty()||MeasurementSessionRegistry.chemCoreCaptureRequested()){status.setText("لا يمكن البدء: توجد جلسة تسجيل نشطة.");return;}if(host.driveLeft()!=0||host.driveRight()!=0){host.setDriveCommand(0,0);status.setText("تم إرسال أمر توقف. أعد البدء بعد ثبوت السرعة صفر.");return;}JSONObject d=host.latestDiagnostics();if(!mqPowered(d)){pendingPowerStart=true;status.setText("جارٍ تشغيل مصفوفة MQ الثمانية ثم التحقق من الريلاي...");host.io().execute(()->{try{host.robotApi().mqPower(true);host.refreshAllTabs();}catch(Exception e){pendingPowerStart=false;host.showToast("تعذر تشغيل MQ: "+e.getMessage());}});render();return;}beginAfterPowerConfirmed(SystemClock.elapsedRealtime());}
    private void beginAfterPowerConfirmed(long now){ChemCoreRuntimeCoordinator.Snapshot s=runtime.begin(host.driveLeft()==0&&host.driveRight()==0,true);if(s.state==ChemCoreSessionController.State.ERROR){status.setText("رفض البدء: "+s.reason);return;}active=true;conditionStartedMs=now;attachedSessionId="";awaitingSample=false;sampleConfirmed=false;awaitingRecoveryRemoval=false;recoveryConfirmed=false;pendingPrimaryFinalize=false;lastResult=null;stability.reset();status.setText("CONDITION: انتظر زمن التسخين المثبت من relay.power_ms واستقرار 6/8 قنوات. لا تدخل العينة بعد.");render();}
    private void primaryAction(){ChemCoreSessionController.State st=runtime.snapshot().state;if(st==ChemCoreSessionController.State.EXPOSURE&&awaitingSample&&!sampleConfirmed){awaitingSample=false;sampleConfirmed=true;exposureStartedMs=SystemClock.elapsedRealtime();nextExposureCheckMs=exposureStartedMs+MIN_EXPOSURE_MS;stability.reset();status.setText("بدأ التعرض الموقّت. أبقِ العينة والظروف ثابتة.");}else if(st==ChemCoreSessionController.State.RECOVERY&&awaitingRecoveryRemoval&&!recoveryConfirmed){awaitingRecoveryRemoval=false;recoveryConfirmed=true;stability.reset();status.setText("بدأت مراقبة التعافي. أبقِ مدخل الحجرة على هواء مرجعي/نظيف قدر الإمكان.");}render();}
    private void attachLiveSession(){String id=MeasurementSessionRegistry.activeSessionId();if(id.isEmpty()||id.equals(attachedSessionId))return;if(runtime.attachMeasurementSession(id)){attachedSessionId=id;status.setText("Baseline recording active: "+id);}}

    private void tryFinalizePrimary(){
        if(!pendingPrimaryFinalize||attachedSessionId.isEmpty())return;if(!MeasurementSessionRegistry.activeSessionId().isEmpty())return;if(!attachedSessionId.equals(MeasurementSessionRegistry.lastCompletedSessionId()))return;pendingPrimaryFinalize=false;
        try{
            ChemCoreSessionFeatureAdapter.Extraction x=ChemCoreSessionFeatureAdapter.extract(host.database(),attachedSessionId);
            if(runtime.snapshot().state==ChemCoreSessionController.State.REPORT)lastResult=new ChemCoreResultEnvelope(attachedSessionId,ChemCoreDecision.Outcome.INSUFFICIENT_EVIDENCE,ChemCoreDecision.EvidenceTier.M0_CATALOG_ONLY,null,"",Double.NaN,Double.NaN,Double.NaN,Double.NaN,Double.NaN,Double.NaN,runtime.profile().profileId,ChemCoreFeaturePipeline.SCHEMA_VERSION,"","response stability gate failed",java.util.Collections.emptyList());
            else if(!x.usable()){runtime.advance(ChemCoreSessionController.Event.EVIDENCE_INSUFFICIENT,true,"{\"reason\":\"feature_extraction_unusable\"}");lastResult=new ChemCoreResultEnvelope(attachedSessionId,ChemCoreDecision.Outcome.INVALID_MEASUREMENT,ChemCoreDecision.EvidenceTier.M0_CATALOG_ONLY,null,"",Double.NaN,Double.NaN,Double.NaN,Double.NaN,Double.NaN,Double.NaN,runtime.profile().profileId,ChemCoreFeaturePipeline.SCHEMA_VERSION,"","canonical feature extraction unusable",java.util.Collections.emptyList());}
            else{
                runtime.advance(ChemCoreSessionController.Event.FEATURES_READY,true,"{\"feature_sha256\":\""+x.featurePayloadSha256+"\"}");Map<String,Double> ratios=steadyRatios(x.features);ChemCoreLibraryManager.Status libStatus=libraries.status();List<InverseMolecularSolver.Candidate> stableCandidates=ChemCoreStableKnowledgeReader.candidates(libraries.activePayload(),runtime.profile());ChemCoreStableKnowledgeReader.OpenSetArtifact openArtifact=stableCandidates.isEmpty()?null:ChemCoreStableKnowledgeReader.openSetArtifact(libraries.activePayload(),runtime.profile().profileId);ChemCoreOpenSetGate.Result openResult=openArtifact==null?null:openArtifact.gate.evaluate(x.features.vector);String modelVersion=stableCandidates.isEmpty()?"legacy-manufacturer-priors":"chemcore-stable:"+libStatus.version;if(openArtifact!=null)modelVersion+="+"+openArtifact.modelVersion;ChemCoreInferenceEngine.Context ctx=new ChemCoreInferenceEngine.Context(attachedSessionId,runtime.profile().profileId,modelVersion,x.meanTemperatureC,x.meanRh,openResult);lastResult=stableCandidates.isEmpty()?ChemCoreInferenceEngine.analyzeLegacyManufacturerPriors(ratios,runtime.profile(),ctx):ChemCoreInferenceEngine.analyze(ratios,stableCandidates,runtime.profile(),ctx);advanceEvidenceState(lastResult,openArtifact,openResult);
            }
            ChemCoreLibraryManager.Status lib=libraries.status();lastResult=lastResult.withLibraryProvenance(lib.version,lib.sha256,lib.sourceCommit);
            ChemCoreLabQuality lab=ChemCoreLabQuality.evaluate(host.latestDiagnostics(),host.driveLeft(),host.driveRight(),host.database());lastResult=lastResult.withLabQuality(lab.toJson());
            new ChemCoreDecisionRepository(host.database()).save(Math.max(0,Math.round(x.durationSeconds*1000.0)),lastResult);persistAnalyticalExperiment(lastResult);
            runtime.finish(lastResult);if(runtime.snapshot().state==ChemCoreSessionController.State.REPORT)runtime.advance(ChemCoreSessionController.Event.REPORT_SAVED,true,"{\"saved\":true}");awaitingRecoveryRemoval=true;recoveryConfirmed=false;stability.reset();renderResult(x,lastResult);status.setText(lastResult.outcome==ChemCoreDecision.Outcome.UNKNOWN?"رفض Open-Set الهوية؛ أزل العينة وابدأ التعافي.":"تم حفظ القرار والتقرير المرتبط بالجلسة. أزل العينة ثم ابدأ مراقبة التعافي.");
        }catch(Exception e){runtime.fail("{\"reason\":\"feature_or_inference_failure\"}");lastResult=null;active=false;status.setText("فشل التحليل: "+e.getMessage());}
    }

    private void persistAnalyticalExperiment(ChemCoreResultEnvelope r){
        try{JSONObject d=host.latestDiagnostics(),a=new JSONObject();a.put("analysis_version","chemcore-v3-session-bound");a.put("chemcore_schema",ChemCoreResultEnvelope.SCHEMA);a.put("r0_campaign",FieldR0Baseline.SESSION);ContentValues v=new ContentValues();v.put("ts",isoNow());v.put("payload",d==null?"{}":d.toString());v.put("protocol","HazRob 0.10.1 • controlled ChemCore analytical session • R0 "+FieldR0Baseline.SESSION);v.put("analytics",a.toString());v.put("session_id",attachedSessionId);v.put("reference_kind","chemcore_controlled_analysis");if(r.chemicalUid!=null)v.put("chemical_uid",r.chemicalUid);host.database().getWritableDatabase().insert("experiments",null,v);}catch(Exception ignored){}
    }

    private void advanceEvidenceState(ChemCoreResultEnvelope result,ChemCoreStableKnowledgeReader.OpenSetArtifact artifact,ChemCoreOpenSetGate.Result openResult){if(result==null){runtime.advance(ChemCoreSessionController.Event.EVIDENCE_INSUFFICIENT,true,"{\"reason\":\"missing_result\"}");return;}if(artifact==null||openResult==null){runtime.advance(ChemCoreSessionController.Event.EVIDENCE_INSUFFICIENT,true,"{\"reason\":\"validated_open_set_model_not_bound\"}");return;}if(result.outcome==ChemCoreDecision.Outcome.INVALID_MEASUREMENT||result.outcome==ChemCoreDecision.Outcome.INSUFFICIENT_EVIDENCE){runtime.advance(ChemCoreSessionController.Event.EVIDENCE_INSUFFICIENT,true,"{\"reason\":\"inference_evidence_insufficient\"}");return;}runtime.advance(ChemCoreSessionController.Event.IDENTITY_READY,true,"{\"identity_model\":\"stable\"}");if(!openResult.accepted||result.outcome==ChemCoreDecision.Outcome.UNKNOWN){runtime.advance(ChemCoreSessionController.Event.OPEN_SET_REJECTED,true,"{\"distance\":"+openResult.distance+",\"threshold\":"+openResult.threshold+"}");return;}runtime.advance(ChemCoreSessionController.Event.OPEN_SET_ACCEPTED,true,"{\"distance\":"+openResult.distance+",\"threshold\":"+openResult.threshold+"}");runtime.advance(ChemCoreSessionController.Event.QUANTIFICATION_READY,true,"{\"quantitative_outcome\":\""+result.outcome.name()+"\"}");if(result.outcome==ChemCoreDecision.Outcome.POSSIBLE_MIXTURE_OR_INTERFERENCE)runtime.advance(ChemCoreSessionController.Event.MIXTURE_SUSPECTED,true,"{\"guard\":\"residual\"}");else runtime.advance(ChemCoreSessionController.Event.CONSISTENCY_ACCEPTED,true,"{\"guard\":\"passed_or_nonquantitative\"}");}

    private void renderResult(ChemCoreSessionFeatureAdapter.Extraction x,ChemCoreResultEnvelope r){StringBuilder s=new StringBuilder(ChemCoreAnalysisPresenter.summary(r.toJson()));s.append("\n\nCapture: ").append(x.persistedSamples).append(" samples • ").append(String.format(Locale.US,"%.1f s",x.durationSeconds));s.append("\nR0 normalization: ").append(x.r0Source).append(" • ").append(x.r0Campaign).append(" • accepted points ").append(x.acceptedR0Samples).append(" • telemetry fallback ").append(x.telemetryRatioFallbackSamples);s.append("\nFeature SHA: ").append(shortId(x.featurePayloadSha256));s.append("\n\n").append(ChemCoreAnalysisPresenter.candidates(r.toJson(),8));s.append("\n\n").append(ChemCoreAnalysisPresenter.equationsAndResiduals(r.toJson(),4));resultText.setText(s.toString());}
    private static Map<String,Double> steadyRatios(ChemCoreFeaturePipeline.Result f){Map<String,Double> out=new LinkedHashMap<>();if(f==null)return out;for(int i=0;i<ChemCoreSessionFeatureAdapter.SENSOR_ORDER.length;i++){double y=f.feature(i,2);if(Double.isFinite(y))out.put(ChemCoreSessionFeatureAdapter.SENSOR_ORDER[i],Math.pow(10.0,y));}return out;}
    private void abortProtocol(String reason){if(MeasurementSessionRegistry.chemCoreCaptureRequested())MeasurementSessionRegistry.requestChemCoreStop(reason);if(active)runtime.abort("{\"reason\":\""+reason+"\"}");active=false;pendingPowerStart=false;awaitingSample=false;sampleConfirmed=false;awaitingRecoveryRemoval=false;recoveryConfirmed=false;status.setText("تم إلغاء الجلسة بأمان.");render();}
    private void render(){ChemCoreRuntimeCoordinator.Snapshot s=runtime.snapshot();phase.setText(s.state.name()+" • "+s.samplingProfileId);boolean idle=!active&&!pendingPowerStart;start.setEnabled(idle);abort.setEnabled(active||pendingPowerStart);action.setEnabled(false);action.setText("—");if(active&&s.state==ChemCoreSessionController.State.EXPOSURE&&awaitingSample&&!sampleConfirmed){action.setText("تم إدخال العينة • ابدأ التعرض");action.setEnabled(true);}else if(active&&s.state==ChemCoreSessionController.State.RECOVERY&&awaitingRecoveryRemoval&&!recoveryConfirmed){action.setText("تمت إزالة العينة • ابدأ التعافي");action.setEnabled(true);}}
    private static boolean mqPowered(JSONObject d){if(d==null)return false;JSONObject r=d.optJSONObject("relay");if(r==null)return false;return (r.optInt("mask",r.optBoolean("on",false)?1:0)&1)!=0;}
    private static long relayPowerMs(JSONObject d){if(d==null)return 0;JSONObject r=d.optJSONObject("relay");return r==null?0:Math.max(0,r.optLong("power_ms",0));}
    private static String shortId(String s){if(s==null||s.isEmpty())return"—";return s.length()<=14?s:s.substring(0,14)+"…";}
    private static String isoNow(){SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'",Locale.US);f.setTimeZone(TimeZone.getTimeZone("UTC"));return f.format(new Date());}
}