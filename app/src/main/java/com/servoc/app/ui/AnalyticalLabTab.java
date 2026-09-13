package com.servoc.app.ui;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.servoc.app.calibration.FieldR0Baseline;
import com.servoc.app.chemcore.ChemCoreAnalysisPresenter;
import com.servoc.app.chemcore.ChemCoreDecisionRepository;
import com.servoc.app.chemcore.ChemCoreLabQuality;
import com.servoc.app.measurement.MeasurementSessionRegistry;

import org.json.JSONObject;

import java.util.Locale;

/**
 * Operator-facing analytical laboratory shell around the controlled ChemCore state machine.
 * The session protocol remains in ChemCoreAnalysisTab; this surface adds pre-run readiness,
 * calibration gates, environmental diagnostics, mathematical interpretation and session-bound audit.
 */
public final class AnalyticalLabTab {
    private final Context context;private final HazRobHost host;private final ChemCoreAnalysisTab session;
    private final LinearLayout root,bar;private final FrameLayout page;private final View sessionView,dashboardView;
    private final Button sessionButton,dashboardButton;
    private final TextView readiness,calibration,environment,result,candidates,math;
    private final ChemCoreDecisionRepository decisions;
    private int selected=0;private String lastBoundSession="";

    public AnalyticalLabTab(Context c,HazRobHost host){
        context=c;this.host=host;this.session=new ChemCoreAnalysisTab(c,host);this.decisions=new ChemCoreDecisionRepository(host.database());
        root=UiKit.vbox(c);root.setBackgroundColor(0xFF0E1116);root.setPadding(UiKit.dp(c,4),UiKit.dp(c,4),UiKit.dp(c,4),UiKit.dp(c,8));
        bar=UiKit.hbox(c);sessionButton=UiKit.button(c,"الجلسة المضبوطة");dashboardButton=UiKit.button(c,"الجاهزية والنتائج");bar.addView(sessionButton,UiKit.weight(1));bar.addView(dashboardButton,UiKit.weight(1));root.addView(bar,UiKit.full(c));
        page=new FrameLayout(c);root.addView(page,new LinearLayout.LayoutParams(-1,0,1f));
        sessionView=session.view();

        ScrollView scroll=new ScrollView(c);scroll.setFillViewport(true);LinearLayout dash=UiKit.vbox(c);dash.setPadding(UiKit.dp(c,8),UiKit.dp(c,6),UiKit.dp(c,8),UiKit.dp(c,14));scroll.addView(dash);dashboardView=scroll;
        dash.addView(UiKit.title(c,"المختبر الكيميائي التحليلي الموسع • HazRob 0.10.1"),UiKit.full(c));
        dash.addView(UiKit.small(c,"المصدر التحليلي الوحيد هنا هو ChemCore v3. يفصل النظام بين R0 للهواء النظيف، التعرف متعدد الحساسات، ومعايرة M4 الكمية؛ ولا يحول screening-fit إلى PPM قابل للتقرير."),UiKit.full(c));

        LinearLayout readyCard=UiKit.card(c);readyCard.addView(UiKit.title(c,"جاهزية المختبر"),UiKit.full(c));readiness=box(c);readyCard.addView(readiness,UiKit.full(c));dash.addView(readyCard,UiKit.full(c));
        LinearLayout calCard=UiKit.card(c);calCard.addView(UiKit.title(c,"R0 وبوابات المعايرة"),UiKit.full(c));calibration=box(c);calCard.addView(calibration,UiKit.full(c));dash.addView(calCard,UiKit.full(c));
        LinearLayout envCard=UiKit.card(c);envCard.addView(UiKit.title(c,"البيئة والحالة I²C"),UiKit.full(c));environment=box(c);envCard.addView(environment,UiKit.full(c));dash.addView(envCard,UiKit.full(c));
        LinearLayout resultCard=UiKit.card(c);resultCard.addView(UiKit.title(c,"آخر قرار ChemCore مرتبط بجلسة"),UiKit.full(c));result=box(c);result.setTextIsSelectable(true);resultCard.addView(result,UiKit.full(c));candidates=box(c);candidates.setTextIsSelectable(true);resultCard.addView(candidates,UiKit.full(c));dash.addView(resultCard,UiKit.full(c));
        LinearLayout mathCard=UiKit.card(c);mathCard.addView(UiKit.title(c,"المنحنيات • اللوغاريتمات • البواقي"),UiKit.full(c));math=box(c);math.setTextIsSelectable(true);mathCard.addView(math,UiKit.full(c));dash.addView(mathCard,UiKit.full(c));

        sessionButton.setOnClickListener(v->show(0));dashboardButton.setOnClickListener(v->show(1));show(0);
    }

    private static TextView box(Context c){TextView v=UiKit.text(c,"—");v.setBackgroundColor(0xFF151A20);return v;}
    public View view(){return root;}

    public void refresh(){
        // The controlled session engine must continue to advance even while the dashboard page is visible.
        session.refresh();
        JSONObject d=host.latestDiagnostics();ChemCoreLabQuality q=ChemCoreLabQuality.evaluate(d,host.driveLeft(),host.driveRight(),host.database());
        renderQuality(q);bindCompletedSession(q);renderDecision();
    }

    private void show(int index){selected=index==1?1:0;page.removeAllViews();page.addView(selected==0?sessionView:dashboardView,new FrameLayout.LayoutParams(-1,-1));sessionButton.setTextColor(selected==0?Color.WHITE:Color.DKGRAY);dashboardButton.setTextColor(selected==1?Color.WHITE:Color.DKGRAY);sessionButton.setBackgroundColor(selected==0?0xFF263238:0xFFE0E0E0);dashboardButton.setBackgroundColor(selected==1?0xFF263238:0xFFE0E0E0);refresh();}

    private void renderQuality(ChemCoreLabQuality q){
        String state=!q.connected?"غير متصل":q.configurationReady?"جاهز لبروتوكول ChemCore":"غير جاهز";
        readiness.setText(state+"\n"+q.compact()+"\nConfiguration: "+yes(q.configurationReady)+" • Capture now: "+yes(q.captureReady)+" • Stationary: "+yes(q.stationary)+
                "\nBlockers: "+join(q.blockers)+"\nAdvisories: "+join(q.advisories));
        readiness.setTextColor(q.configurationReady?0xFF81C784:0xFFFFB74D);
        calibration.setText("R0 accepted campaign: "+FieldR0Baseline.SESSION+"\nR0 normalization: "+q.r0NormalizedChannels+"/8 • field clean-air: "+q.fieldR0Channels+"/8 • "+FieldR0Baseline.SAMPLE_COUNT+" samples / "+FieldR0Baseline.SESSION_COUNT+" sessions\nReference-gas/M4 sensors with ≥1 active accepted curve: "+q.referenceValidatedChannels+"/8\n"+
                (q.fingerprintNormalizationReady()?"البصمة Rs/R0 جاهزة؛ التطبيق يعيد التطبيع من Rs باستخدام HR-0101-045 حتى قبل مزامنة Firmware.":"البصمة غير جاهزة: R0 غير مكتمل.")+"\n"+
                (q.anyReferenceGasCalibrationAvailable()?"توجد معايرة مرجعية جزئية؛ السماح الكمي يظل خاصاً بكل مرشح وبجميع منحنياته المساهمة.":"PPM الكمي محجوب: لا توجد بعد منحنيات غاز مرجعي M4 نشطة ومقبولة."));
        String t=finite(q.temperatureC)?String.format(Locale.US,"%.2f °C",q.temperatureC):"—",h=finite(q.humidityRh)?String.format(Locale.US,"%.1f %%RH",q.humidityRh):"—",p=finite(q.pressureHpa)?String.format(Locale.US,"%.1f hPa",q.pressureHpa):"—";
        environment.setText("Sensor: "+blank(q.environmentSensor)+" • Status: "+blank(q.environmentStatus)+"\nAddress: "+blank(q.environmentAddress)+" • Chip ID: "+blank(q.environmentChipId)+"\nT "+t+" • RH "+h+" • P "+p+"\nEnvironmental correction: "+(q.environmentReady?"بيانات البيئة متاحة؛ يطبق التصحيح فقط إذا كان نموذج المركب يدعمه.":"WITHHELD — لا توجد قراءة بيئية صالحة."));
        environment.setTextColor(q.environmentReady?0xFF81C784:0xFFFFB74D);
    }

    private void bindCompletedSession(ChemCoreLabQuality q){
        String id=MeasurementSessionRegistry.lastCompletedSessionId();if(id==null||id.isEmpty()||id.equals(lastBoundSession))return;
        JSONObject payload=decisions.latestPayloadForSession(id);if(payload==null)return;
        if(payload.optJSONObject("lab_quality")==null)decisions.attachLabQuality(id,q.toJson(),System.currentTimeMillis());
        lastBoundSession=id;
    }

    private void renderDecision(){
        String id=MeasurementSessionRegistry.lastCompletedSessionId();JSONObject p=id==null||id.isEmpty()?null:decisions.latestPayloadForSession(id);
        result.setText(ChemCoreAnalysisPresenter.summary(p));
        candidates.setText(ChemCoreAnalysisPresenter.candidates(p,8));
        math.setText(ChemCoreAnalysisPresenter.equationsAndResiduals(p,4));
    }

    private static String yes(boolean v){return v?"PASS":"NO";}
    private static boolean finite(double v){return Double.isFinite(v);}
    private static String blank(String s){return s==null||s.isEmpty()?"—":s;}
    private static String join(java.util.List<String> x){if(x==null||x.isEmpty())return "none";StringBuilder s=new StringBuilder();for(String v:x){if(s.length()>0)s.append(", ");s.append(v);}return s.toString();}
}