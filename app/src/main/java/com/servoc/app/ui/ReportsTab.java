package com.servoc.app.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.RectF;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import com.servoc.app.chemcore.ChemCoreAnalysisPresenter;
import com.servoc.app.chemcore.ChemCoreDecisionRepository;
import com.servoc.app.data.ExperimentQueries;
import com.servoc.app.data.SessionSeries;
import com.servoc.app.field.FieldSessionRepository;
import com.servoc.app.report.AnalyticalCharts;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.Locale;

/** HazRob 0.10.2 / HR069 smart operator report workspace. */
public final class ReportsTab {
    private static final int BG=0xFF0E1116,CARD=0xFF151A20,CARD2=0xFF11171D,TEXT=0xFFECEFF1,MUTED=0xFFB0BEC5,GOOD=0xFF81C784,WARN=0xFFFFB74D,BAD=0xFFFF7043,INFO=0xFF90CAF9;
    private static final String[] CHARTS={
            "استجابة MQ / Rs-R0","البصمة الكيميائية","البصمة الرادارية Radar","المرشحون",
            "خريطة المرشحين Scatter","خريطة المرشحين متعددة المتغيرات","بواقي المرشحين Residual",
            "فحص 144","توزيع فحص 144 Donut","توزيع مستويات الدليل M0-M4","التعرض والاشتعال",
            "مصفوفة MQ × البيئة","جودة إشارة MQ / SNR","حركية الاستجابة t50-t90",
            "خريطة الاستجابة المستمرة AUC","ملف الجودة التحليلية Radar","الحرارة","الرطوبة","الضغط","الصوت"
    };

    private final HazRobHost host;
    private final ScrollView scroll;
    private final LinearLayout root,list;
    private final TextView archiveCard,sessionCard,decisionCard,qualityCard,safetyCard,candidateCard,chartStatus,technical;
    private final Button pdf,xlsx,technicalToggle;
    private final Spinner chartSelector;
    private final ImageView chartView;
    private final ChemCoreDecisionRepository decisions;
    private final FieldSessionRepository fieldSessions;
    private long selectedId=-1;
    private boolean technicalVisible=false;

    public ReportsTab(Context c,HazRobHost host){
        this.host=host;decisions=new ChemCoreDecisionRepository(host.database());fieldSessions=new FieldSessionRepository(host.database());
        scroll=new ScrollView(c);scroll.setFillViewport(true);root=UiKit.vbox(c);root.setPadding(UiKit.dp(c,8),UiKit.dp(c,6),UiKit.dp(c,8),UiKit.dp(c,12));root.setBackgroundColor(BG);scroll.addView(root);
        root.addView(UiKit.title(c,"التقارير الذكية • HR069"),UiKit.full(c));

        archiveCard=card(c,"السجل • لا توجد قياسات محفوظة");root.addView(archiveCard,UiKit.full(c));
        LinearLayout r1=UiKit.hbox(c);sessionCard=kpi(c,"الجلسة\n—");decisionCard=kpi(c,"القرار\n—");r1.addView(sessionCard,UiKit.weight(1));r1.addView(decisionCard,UiKit.weight(1));root.addView(r1,UiKit.full(c));
        LinearLayout r2=UiKit.hbox(c);qualityCard=kpi(c,"الجودة\n—");safetyCard=kpi(c,"السلامة\n—");r2.addView(qualityCard,UiKit.weight(1));r2.addView(safetyCard,UiKit.weight(1));root.addView(r2,UiKit.full(c));
        candidateCard=card(c,"المرشحون • —");candidateCard.setTextIsSelectable(true);root.addView(candidateCard,UiKit.full(c));

        LinearLayout actions=UiKit.hbox(c);pdf=UiKit.button(c,"تصدير PDF");pdf.setOnClickListener(v->export("pdf"));actions.addView(pdf,UiKit.weight(1));xlsx=UiKit.button(c,"تصدير Excel");xlsx.setOnClickListener(v->export("xlsx"));actions.addView(xlsx,UiKit.weight(1));root.addView(actions,UiKit.full(c));

        TextView chartTitle=UiKit.small(c,"الرسوم التحليلية • يعرض التطبيق فقط الرسوم التي تتوفر لها بيانات مسجلة فعلية");chartTitle.setTextColor(MUTED);root.addView(chartTitle,UiKit.full(c));
        chartSelector=new Spinner(c);chartSelector.setAdapter(new ArrayAdapter<String>(c,android.R.layout.simple_spinner_dropdown_item,CHARTS));chartSelector.setBackgroundColor(0xFFF5F5F5);root.addView(chartSelector,UiKit.full(c));
        chartStatus=card(c,"المخطط • اختر جلسة محفوظة");root.addView(chartStatus,UiKit.full(c));
        chartView=new ImageView(c);chartView.setAdjustViewBounds(true);chartView.setMinimumHeight(UiKit.dp(c,260));chartView.setBackgroundColor(Color.WHITE);root.addView(chartView,UiKit.full(c));
        chartSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> p,View v,int pos,long id){renderSelectedChart(pos);}public void onNothingSelected(AdapterView<?> p){}});

        technical=card(c,"—");technical.setTextIsSelectable(true);technical.setVisibility(View.GONE);
        technicalToggle=UiKit.button(c,"إظهار الأدلة التقنية");technicalToggle.setOnClickListener(v->{technicalVisible=!technicalVisible;technical.setVisibility(technicalVisible?View.VISIBLE:View.GONE);technicalToggle.setText(technicalVisible?"إخفاء الأدلة التقنية":"إظهار الأدلة التقنية");});root.addView(technicalToggle,UiKit.full(c));
        root.addView(technical,UiKit.full(c));

        TextView history=UiKit.small(c,"القياسات المحفوظة");history.setTextColor(MUTED);root.addView(history,UiKit.full(c));list=UiKit.vbox(c);root.addView(list,UiKit.full(c));setEnabled(false);
    }

    public View view(){return scroll;}

    public void refresh(){
        list.removeAllViews();List<ExperimentQueries.Record> rows=ExperimentQueries.recent(host.database(),100);archiveCard.setText("السجل • "+host.database().count()+" قياس محفوظ • المعروض "+rows.size());
        if(rows.isEmpty()){selectedId=-1;setEnabled(false);sessionCard.setText("الجلسة\n—");decisionCard.setText("القرار\n—");qualityCard.setText("الجودة\n—");safetyCard.setText("السلامة\n—");candidateCard.setText("المرشحون • —");technical.setText("لا توجد بيانات تقرير.");chartView.setImageDrawable(null);chartStatus.setText("المخطط • لا توجد جلسة محفوظة");return;}
        if(selectedId<0)selectedId=rows.get(0).id;boolean found=false;
        for(ExperimentQueries.Record r:rows){if(r.id==selectedId)found=true;Button b=UiKit.button(list.getContext(),(r.id==selectedId?"● ":"")+"#"+r.id+"   "+safe(r.ts)+"   "+safe(r.protocol));b.setOnClickListener(v->select(r.id));list.addView(b,UiKit.full(list.getContext()));}
        if(!found)selectedId=rows.get(0).id;showSelected();
    }

    private void select(long id){selectedId=id;showSelected();refreshHistorySelection();}
    private void refreshHistorySelection(){refresh();}

    private void showSelected(){
        ExperimentQueries.Record r=ExperimentQueries.byId(host.database(),selectedId);if(r==null){setEnabled(false);return;}setEnabled(true);
        JSONObject d=json(r.payload),m=d.optJSONObject("measurement"),e=d.optJSONObject("env"),relay=d.optJSONObject("relay"),a=json(r.analytics);String sid=r.sessionId==null?"":r.sessionId.trim();JSONObject cc=sid.isEmpty()?null:decisions.latestPayloadForSession(sid);SessionSeries.Series ss=SessionSeries.forExperiment(host.database(),r);
        String fieldSessionId=sid.isEmpty()?"":fieldSessions.fieldSessionIdForMeasurement(sid);boolean fieldReport=!fieldSessionId.isEmpty();pdf.setText(fieldReport?"تصدير PDF الذكي للجلسة":"تصدير PDF للقياس");xlsx.setText(fieldReport?"Excel ذكي للجلسة":"Excel للقياس");
        boolean seriesOk=ss!=null&&ss.usable(),mqOn=relay!=null&&relay.optBoolean("on",relay.optInt("mask",0)!=0);String state=m==null?"—":m.optString("state","—");
        sessionCard.setText("الجلسة #"+r.id+"\n"+(sid.isEmpty()?"بدون Session ID":sid)+"\n"+state+(seriesOk?" • "+ss.samples.size()+" عينة":" • لا سلسلة زمنية"));sessionCard.setTextColor(seriesOk?GOOD:WARN);

        if(cc==null){decisionCard.setText("القرار\nلا توجد نتيجة ChemCore");decisionCard.setTextColor(WARN);qualityCard.setText("الجودة\n—");safetyCard.setText("السلامة\nمحجوبة");candidateCard.setText("المرشحون • لا توجد نتيجة مرتبطة");}
        else{
            String outcome=cc.optString("outcome","—"),tier=cc.optString("evidence_tier","—"),chemical=cc.optString("chemical","—");double post=cc.optDouble("top_posterior",Double.NaN),margin=cc.optDouble("posterior_separation_margin",Double.NaN),ood=cc.optDouble("ood_score",Double.NaN),entropy=cc.optDouble("normalized_entropy",Double.NaN),ppm=cc.optDouble("ppm",Double.NaN);boolean ppmOk=cc.optBoolean("ppm_reportable",false);
            decisionCard.setText("القرار • "+outcome+"\n"+chemical+" • "+pct(post)+"\n"+tier+(ppmOk&&Double.isFinite(ppm)?" • "+fmt(ppm)+" ppm":" • PPM محجوب"));decisionCard.setTextColor("M4_HAZROB_VALIDATED".equals(tier)?GOOD:INFO);
            JSONObject q=first(cc.optJSONObject("measurement_quality"),cc.optJSONObject("lab_quality"));int valid=q==null?-1:q.optInt("valid_mq_channels",q.optInt("valid_channels",-1));String qstate=q==null?"—":q.optString("state",q.optString("overall_state",q.optBoolean("capture_ready",false)?"READY":"—"));qualityCard.setText("الجودة • "+qstate+"\nMQ "+(valid>=0?valid+"/8":"—")+" • OOD "+fmt(ood)+"\nEntropy "+fmt(entropy)+" • Δ "+pct(margin));qualityCard.setTextColor(valid>=6?GOOD:WARN);
            JSONObject gx=cc.optJSONObject("gas_exposure_ppm");if(gx==null||!gx.optBoolean("input_gate_passed",false)){safetyCard.setText("السلامة\nالتفسير الكمي محجوب\n"+(gx==null?"لا توجد نتيجة M4":"بوابة القياس لم تجتز"));safetyCard.setTextColor(WARN);}else{double hp=gx.optDouble("hazard_priority_percent",Double.NaN),lfl=gx.optDouble("percent_lfl",Double.NaN);safetyCard.setText("السلامة • "+gx.optString("hazard_symbols","")+gx.optString("toxicity_state",gx.optString("state","—"))+"\nأولوية "+pct100(hp)+" • LFL "+pct100(lfl));safetyCard.setTextColor((Double.isFinite(hp)&&hp>=100)||(Double.isFinite(lfl)&&lfl>=100)?BAD:WARN);}
            candidateCard.setText(candidateSummary(cc));
        }

        StringBuilder t=new StringBuilder("المرجع والتتبّع\n");t.append("Experiment #").append(r.id).append(" • UTC ").append(safe(r.ts)).append("\nProtocol: ").append(safe(r.protocol)).append("\nSession: ").append(sid.isEmpty()?"—":sid).append("\nField Session: ").append(fieldReport?fieldSessionId:"—").append("\nMQ Power: ").append(mqOn?"ON":"OFF");if(relay!=null&&mqOn)t.append(" • ").append(relay.optLong("power_ms",0)).append(" ms");if(e!=null&&e.optBoolean("ok",false))t.append("\nENV: ").append(fmt(e.optDouble("temperature",Double.NaN))).append(" °C • ").append(fmt(e.optDouble("humidity",Double.NaN))).append(" %RH • ").append(fmt(e.optDouble("pressure",Double.NaN))).append(" hPa");t.append("\nStored analytical rows: ").append(a.optJSONArray("results")==null?0:a.optJSONArray("results").length());if(cc!=null){t.append("\n\n").append(ChemCoreAnalysisPresenter.summary(cc));t.append("\n\n").append(ChemCoreAnalysisPresenter.equationsAndResiduals(cc,6));}technical.setText(t.toString());renderSelectedChart(chartSelector.getSelectedItemPosition());
    }

    private void renderSelectedChart(int pos){
        ExperimentQueries.Record r=ExperimentQueries.byId(host.database(),selectedId);if(r==null){chartView.setImageDrawable(null);chartStatus.setText("المخطط • لا توجد جلسة");return;}
        SessionSeries.Series ss=SessionSeries.forExperiment(host.database(),r);String sid=r.sessionId==null?"":r.sessionId.trim();JSONObject cc=sid.isEmpty()?null:decisions.latestPayloadForSession(sid);boolean seriesOk=ss!=null&&ss.usable(),chemOk=cc!=null;boolean needsChem=pos>=3&&pos<=10,needsSeries=pos<=2||pos>=11;
        if((needsChem&&!chemOk)||(needsSeries&&!seriesOk)){chartView.setImageDrawable(null);chartStatus.setText("غير متاح • "+CHARTS[Math.max(0,Math.min(pos,CHARTS.length-1))]+" • يحتاج "+(needsChem?"نتيجة ChemCore محفوظة":"سلسلة زمنية مسجلة"));chartStatus.setTextColor(WARN);return;}
        String source=needsChem?"ChemCore payload":"السلسلة الزمنية المسجلة";chartStatus.setText("متاح ● • "+CHARTS[pos]+" • المصدر: "+source+" • لا توجد نقاط مصطنعة");chartStatus.setTextColor(GOOD);
        Bitmap b=Bitmap.createBitmap(1200,650,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(b);canvas.drawColor(Color.WHITE);RectF box=new RectF(12,12,1188,638);
        switch(pos){
            case 0:AnalyticalCharts.mqRsR0(canvas,box,ss);break;case 1:AnalyticalCharts.fingerprint(canvas,box,ss);break;case 2:AnalyticalCharts.fingerprintRadar(canvas,box,ss);break;
            case 3:AnalyticalCharts.candidates(canvas,box,cc);break;case 4:AnalyticalCharts.candidateScatter(canvas,box,cc);break;case 5:AnalyticalCharts.candidateBubbleMap(canvas,box,cc);break;case 6:AnalyticalCharts.candidateResiduals(canvas,box,cc);break;case 7:AnalyticalCharts.screening144(canvas,box,cc);break;case 8:AnalyticalCharts.screeningDonut(canvas,box,cc);break;case 9:AnalyticalCharts.evidenceTiers(canvas,box,cc);break;case 10:AnalyticalCharts.exposureRatios(canvas,box,cc);break;
            case 11:AnalyticalCharts.mqEnvironmentMatrix(canvas,box,ss);break;case 12:AnalyticalCharts.mqSignalQuality(canvas,box,ss);break;case 13:AnalyticalCharts.mqResponseKinetics(canvas,box,ss);break;case 14:AnalyticalCharts.mqPersistenceMap(canvas,box,ss);break;case 15:AnalyticalCharts.analyticalQualityRadar(canvas,box,ss);break;case 16:AnalyticalCharts.temperature(canvas,box,ss);break;case 17:AnalyticalCharts.humidity(canvas,box,ss);break;case 18:AnalyticalCharts.pressure(canvas,box,ss);break;case 19:AnalyticalCharts.sound(canvas,box,ss);break;
            default:AnalyticalCharts.candidates(canvas,box,cc);
        }
        chartView.setImageBitmap(b);
    }

    private TextView card(Context c,String text){TextView t=UiKit.text(c,text);t.setTextColor(TEXT);t.setBackgroundColor(CARD);t.setPadding(UiKit.dp(c,10),UiKit.dp(c,9),UiKit.dp(c,10),UiKit.dp(c,9));return t;}
    private TextView kpi(Context c,String text){TextView t=card(c,text);t.setTextSize(14);t.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);return t;}
    private static JSONObject first(JSONObject a,JSONObject b){return a!=null?a:b;}
    private static String candidateSummary(JSONObject p){JSONArray a=p==null?null:p.optJSONArray("candidates");if(a==null||a.length()==0)return "المرشحون • لا توجد فرضيات قابلة للحل";StringBuilder s=new StringBuilder("المرشحون الأعلى • ").append(a.length());int n=Math.min(6,a.length());for(int i=0;i<n;i++){JSONObject q=a.optJSONObject(i);if(q==null)continue;s.append("\n#").append(q.optInt("rank",i+1)).append(" ").append(q.optString("label","—")).append(" • ").append(pct(q.optDouble("posterior",Double.NaN))).append(" • residual ").append(fmt(q.optDouble("residual_rms",Double.NaN))).append(" • ").append(q.optString("evidence_tier","—"));}if(a.length()>n)s.append("\n+").append(a.length()-n).append(" مرشح إضافي في نتائج التحليل");return s.toString();}
    private void setEnabled(boolean v){pdf.setEnabled(v);xlsx.setEnabled(v);chartSelector.setEnabled(v);}
    private void export(String f){
        if(selectedId<0){host.showToast("اختر قياسًا أولًا");return;}
        ExperimentQueries.Record r=ExperimentQueries.byId(host.database(),selectedId);if(r==null){host.showToast("السجل غير موجود");return;}
        String sid=r.sessionId==null?"":r.sessionId.trim();String fieldSessionId=sid.isEmpty()?"":fieldSessions.fieldSessionIdForMeasurement(sid);
        if(!fieldSessionId.isEmpty()){host.requestFieldSessionExport(f,fieldSessionId);return;}
        host.requestExport(f,selectedId);
    }
    private static JSONObject json(String s){try{return s==null?new JSONObject():new JSONObject(s);}catch(Exception e){return new JSONObject();}}
    private static String safe(String s){return s==null?"":s;}
    private static String fmt(double v){return Double.isFinite(v)?String.format(Locale.US,"%.2f",v):"—";}
    private static String pct(double v){if(!Double.isFinite(v))return"—";return String.format(Locale.US,"%.1f%%",v<=1.000001?v*100:v);}
    private static String pct100(double v){return Double.isFinite(v)?String.format(Locale.US,"%.1f%%",v):"—";}
}
